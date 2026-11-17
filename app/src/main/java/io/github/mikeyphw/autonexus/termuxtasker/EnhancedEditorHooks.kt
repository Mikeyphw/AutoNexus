package io.github.mikeyphw.autonexus.termuxtasker

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.text.InputType
import android.text.Editable
import android.text.TextWatcher
import android.content.Context
import android.os.UserManager
import android.os.Handler
import android.os.Looper
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import java.util.WeakHashMap

/**
 * TT-E1: an in-process enhanced form for the ORIGINAL Termux:Tasker editor.
 * All fields are copied into the original app's existing widgets only after
 * explicit Save; original finish() still owns Locale bundle serialization.
 * No Tasker/Termux broadcast, shell execution or private-data logging occurs.
 * Only installed after TT-F0 has verified the pinned app version and signer.
 */
object EnhancedEditorHooks {
    private const val TARGET = "com.termux.tasker.EditConfigurationActivity"
    private const val STATE = "io.github.mikeyphw.autonexus.tt.editor.state.v1"
    private val sessions = WeakHashMap<Activity, Session>()
    private val fieldNames = arrayOf(
        "executable_path", "arguments", "working_directory_path",
        "session_action", "background_custom_log_level",
    )

    private class Session(val activity: Activity, initialDraft: EditorDraft,
                          val unknownKeys: Boolean, var stdinWasNull: Boolean) {
        val mode = EditorDraftBridge(initialDraft)
        val enhanced: Boolean get() = mode.enhanced
        val draft: EditorDraft get() = mode.draft
        var finishApproved = false
        var dialog: Dialog? = null
        var inputs: Array<EditText>? = null
        var terminal: CheckBox? = null
        var waitForResult: CheckBox? = null
        var validation: TextView? = null
        val browseGeneration = AtomicInteger(0)
        fun current(): EditorDraft {
            val v = inputs ?: return draft
            return EditorDraft(
                v[0].text.toString(), v[1].text.toString(), v[2].text.toString(),
                v[3].text.toString(), v[4].text.toString(), v[5].text.toString(),
                terminal?.isChecked == true, waitForResult?.isChecked == true,
            )
        }
        fun dismiss() {
            browseGeneration.incrementAndGet()
            try { dialog?.dismiss() } catch (_: Throwable) { }
            dialog = null; inputs = null
        }
    }

    /** Installed only in the exact com.termux.tasker main process. */
    fun install(loader: ClassLoader) {
        XposedHelpers.findAndHookMethod(Activity::class.java, "onSaveInstanceState", Bundle::class.java,
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val activity = param.thisObject as? Activity ?: return
                    val state = synchronized(sessions) { sessions[activity] } ?: return
                    if (!state.enhanced) return
                    try {
                        val draft = state.current()
                        // No unbounded Android saved-state data, no Tasker result extras.
                        if (draft.validationProblems(Int.MIN_VALUE..Int.MAX_VALUE,
                                Int.MIN_VALUE..Int.MAX_VALUE).any { it.contains("too large") }) return
                        (param.args[0] as? Bundle)?.putBundle(STATE, toBundle(draft, state.stdinWasNull))
                    } catch (_: Throwable) { /* No changes to stock save semantics. */ }
                }
            })
        XposedHelpers.findAndHookMethod(TARGET, loader, "finish",
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val activity = param.thisObject as? Activity ?: return
                    val state = synchronized(sessions) { sessions[activity] } ?: return
                    if (!state.enhanced || state.finishApproved || activity.isFinishing) return
                    // Back/Home must not implicitly invoke stock finish() and save.
                    param.result = null
                    if (state.dialog?.isShowing != true) {
                        try { confirmExit(state) } catch (_: Throwable) { /* fail closed */ }
                    }
                }
            })
        XposedHelpers.findAndHookMethod(Activity::class.java, "onDestroy", object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                val activity = param.thisObject as? Activity ?: return
                val state = synchronized(sessions) { sessions.remove(activity) }
                state?.dismiss()
            }
        })
        // Activate the UI LAST: a lifecycle/finish hook failure must never
        // install a partially guarded configuration editor.
        XposedHelpers.findAndHookMethod(TARGET, loader, "onCreate", Bundle::class.java,
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val activity = param.thisObject as? Activity ?: return
                    try { attach(activity, param.args.getOrNull(0) as? Bundle) }
                    catch (_: Throwable) { /* Stock editor remains the only UI. */ }
                }
            })
    }

    private fun id(activity: Activity, name: String): Int =
        activity.resources.getIdentifier(name, "id", activity.packageName)

    private fun textField(activity: Activity, name: String): EditText? {
        val identifier = id(activity, name)
        return if (identifier == 0) null else activity.findViewById<View>(identifier) as? EditText
    }

    private fun check(activity: Activity, name: String): CheckBox? {
        val identifier = id(activity, name)
        return if (identifier == 0) null else activity.findViewById<View>(identifier) as? CheckBox
    }

    private fun stockDraft(activity: Activity): EditorDraft? {
        val fields = fieldNames.map { textField(activity, it) ?: return null }
        val terminal = check(activity, "in_terminal") ?: return null
        val wait = check(activity, "wait_for_result") ?: return null
        val stdin = try { XposedHelpers.getObjectField(activity, "mStdin") as? String ?: "" }
                    catch (_: Throwable) { return null }
        return EditorDraft(
            fields[0].text.toString(), fields[1].text.toString(), fields[2].text.toString(),
            stdin, fields[3].text.toString(), fields[4].text.toString(),
            terminal.isChecked, wait.isChecked,
        )
    }

    private fun stockStdinWasNull(activity: Activity): Boolean = try {
        XposedHelpers.getObjectField(activity, "mStdin") == null
    } catch (_: Throwable) { false }

    private fun attach(activity: Activity, saved: Bundle?) {
        val original = stockDraft(activity) ?: return
        // When the original activity recreated, its onCreate(saved != null)
        // skipped loading parts of the action. Restore OUR independent draft.
        val restored = try { fromBundle(saved?.getBundle(STATE)) } catch (_: Throwable) { null }
        val active = restored ?: original
        val unknown = try {
            val old = activity.intent?.getBundleExtra("com.twofortyfouram.locale.intent.extra.BUNDLE")
            old != null && EditorDraft.hasUnknownKeys(old.keySet())
        } catch (_: Throwable) { true }
        val stdinWasNull = if (restored != null) try {
            saved?.getBundle(STATE)?.getBoolean("n") == true
        } catch (_: Throwable) { false } else try {
            XposedHelpers.getObjectField(activity, "mStdin") == null
        } catch (_: Throwable) { false }
        val state = Session(activity, active, unknown, stdinWasNull)
        synchronized(sessions) { sessions[activity] = state }
        if (restored != null) {
            // Restoration must only happen for the exact injected schema.
            // Never serialize a new Locale result during Activity recreation.
            try { writeStock(activity, restored, stdinWasNull) } catch (_: Throwable) {
                synchronized(sessions) { sessions.remove(activity) }
                return
            }
        }
        val rootId = id(activity, "activity_main")
        val root = if (rootId != 0) activity.findViewById<View>(rootId) as? ViewGroup else null
        if (root == null) {
            synchronized(sessions) { sessions.remove(activity) }
            return
        }
        // Native fallback is always visible; using this button reopens the
        // enhanced editor after the operator has chosen Original.
        val entry = Button(activity).apply {
            text = "AutoNexus · Enhanced editor"
            contentDescription = "Open enhanced Termux Tasker configuration editor"
            setOnClickListener {
                // Stock fields may have been edited while Original was visible.
                // Never resurrect an earlier enhanced draft over those changes.
                val currentStock = stockDraft(activity) ?: return@setOnClickListener
                state.stdinWasNull = stockStdinWasNull(activity)
                state.mode.reopenEnhanced(currentStock)
                show(state)
            }
        }
        root.addView(entry, 0, ViewGroup.LayoutParams(-1, -2))
        entry.post { if (!activity.isFinishing && !activity.isDestroyed) show(state) }
    }

    private fun toBundle(draft: EditorDraft, stdinWasNull: Boolean) = Bundle().apply {
        putInt("schema", EditorDraft.RESTORE_SCHEMA)
        putString("e", draft.executable); putString("a", draft.arguments)
        putString("w", draft.workingDirectory); putString("i", draft.stdin)
        putString("s", draft.sessionAction); putString("l", draft.logLevel)
        putBoolean("t", draft.inTerminal); putBoolean("r", draft.waitForResult)
        putBoolean("n", stdinWasNull)
    }

    private fun fromBundle(bundle: Bundle?): EditorDraft? {
        if (bundle?.getInt("schema", -1) != EditorDraft.RESTORE_SCHEMA) return null
        return EditorDraft(
            bundle.getString("e") ?: return null, bundle.getString("a") ?: return null,
            bundle.getString("w") ?: return null, bundle.getString("i") ?: return null,
            bundle.getString("s") ?: return null, bundle.getString("l") ?: return null,
            bundle.getBoolean("t"), bundle.getBoolean("r"),
        )
    }

    private fun px(activity: Activity, amount: Int): Int =
        (amount * activity.resources.displayMetrics.density).toInt()

    /**
     * The stock 0.9.0 Activity uses AutoCompleteTextView and ArrayAdapter<String>
     * for executable and working-directory suggestions. Keep the same live
     * adapters (no new filesystem permissions or speculative shell history).
     * Staging still happens ONLY in this enhanced draft until explicit Save.
     */
    private fun stockSuggestions(activity: Activity, stockName: String): List<String> {
        val source = textField(activity, stockName) as? AutoCompleteTextView ?: return emptyList()
        val adapter = source.adapter as? ArrayAdapter<*> ?: return emptyList()
        return (0 until minOf(adapter.count, EditorPathSuggestions.MAX_OPTIONS)).mapNotNull {
            adapter.getItem(it) as? String
        }
    }

    /** Local adapter is separate from the stock editor's live adapter.
     * Typeahead can refresh from a restricted, read-only directory listing;
     * no stock input is modified while typing. If filesystem access fails,
     * the original adapter snapshot and manual input still remain available. */
    private fun bindAutocomplete(activity: Activity, view: AutoCompleteTextView,
                                 stockName: String, initial: List<String>) {
        val values = ArrayList(initial)
        val adapter = ArrayAdapter(activity, android.R.layout.simple_dropdown_item_1line, values)
        view.setAdapter(adapter)
        view.threshold = 0
        val request = AtomicInteger(0)
        val debounce = Handler(Looper.getMainLooper())
        var scheduled: Runnable? = null
        view.setOnFocusChangeListener { _, focused ->
            if (focused && adapter.count > 0) view.showDropDown()
        }
        view.setOnClickListener { if (adapter.count > 0) view.showDropDown() }
        view.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString() ?: return
                val serial = request.incrementAndGet()
                val root = File(ScriptExplorer.TERMUX_SCRIPTS)
                scheduled?.let { debounce.removeCallbacks(it) }
                val folder = EditorPathSuggestions.folderForQuery(root, query) ?: return
                val directoryOnly = stockName == "working_directory_path"
                val job = Runnable {
                    if (serial != request.get()) return@Runnable
                    Thread {
                        val result = ScriptExplorer.browse(root, folder)
                        val updated = EditorPathSuggestions.displayOptions(root, folder, query, result, directoryOnly)
                        activity.runOnUiThread {
                            if (serial != request.get() || activity.isFinishing || activity.isDestroyed ||
                                !view.isAttachedToWindow || updated.isEmpty()) return@runOnUiThread
                            // Never clear the stock app's adapter or change its fields.
                            adapter.clear()
                            adapter.addAll(updated)
                            adapter.notifyDataSetChanged()
                        }
                    }.apply { isDaemon = true; start() }
                }
                scheduled = job
                debounce.postDelayed(job, 180L)
            }
        })
    }

    private fun field(activity: Activity, label: String, value: String,
                      kit: EditorUiKit, stockName: String? = null,
                      multiline: Boolean = false): Pair<LinearLayout, EditText> {
        val container = kit.column()
        container.setPadding(0, px(activity, 8), 0, px(activity, 5))
        container.addView(kit.label(label))
        val edit: EditText = if (stockName != null) {
            AutoCompleteTextView(activity).apply {
                bindAutocomplete(activity, this, stockName, stockSuggestions(activity, stockName))
            }
        } else EditText(activity)
        edit.apply {
            setText(value)
            setSingleLine(!multiline)
            if (multiline) {
                minLines = 2; maxLines = 5
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            } else inputType = if (stockName != null) InputType.TYPE_CLASS_TEXT else
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            contentDescription = label
            kit.styleInput(this)
        }
        container.addView(edit, LinearLayout.LayoutParams(-1, -2))
        return container to edit
    }

    /** A path chooser must ALWAYS provide a working manual fallback: the
     * original plugin process may be denied Termux private-files access. */
    private fun choosePath(state: Session, directoryOnly: Boolean, current: EditText) {
        val activity = state.activity
        val kind = if (directoryOnly) "Working directory" else "Executable"
        val root = File(ScriptExplorer.TERMUX_SCRIPTS)
        val suggestion = if (directoryOnly) "Use the script folder or enter another directory" else
            "Select an accessible script or type an absolute/relative path"
        AlertDialog.Builder(activity)
            .setTitle("Choose $kind")
            .setMessage("$suggestion. Browsing needs the original plugin's filesystem access; no root is used.")
            .setPositiveButton("Browse scripts") { _, _ -> browseScripts(state, root, directoryOnly) }
            .setNeutralButton("Enter path") { _, _ -> manualPath(state, directoryOnly, current) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun manualPath(state: Session, directoryOnly: Boolean, current: EditText) {
        val activity = state.activity
        val input = EditText(activity).apply {
            setSingleLine(true)
            setText(current.text.toString())
            setSelection(text.length)
            contentDescription = if (directoryOnly) "Working directory path" else "Executable path"
            setPadding(px(activity, 14), px(activity, 12), px(activity, 14), px(activity, 12))
        }
        val wrapper = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(px(activity, 16), px(activity, 8), px(activity, 16), 0)
            addView(input, LinearLayout.LayoutParams(-1, -2))
        }
        AlertDialog.Builder(activity)
            .setTitle(if (directoryOnly) "Enter working directory" else "Enter executable path")
            .setMessage("The path is staged only. Original Termux:Tasker validates it on Save. " +
                "Tasker variables are left unchanged.")
            .setView(wrapper)
            .setPositiveButton("Use path") { _, _ ->
                if (!state.enhanced || state.dialog?.isShowing != true) return@setPositiveButton
                current.setText(input.text.toString())
                state.validation?.text = "Path staged. No file opened or command executed."
            }
            .setNegativeButton("Cancel", null).show()
    }

    private fun show(state: Session) {
        val activity = state.activity
        if (activity.isFinishing || activity.isDestroyed) return
        if (state.dialog?.isShowing == true) return
        val draft = state.draft
        val kit = EditorUiKit(activity)
        val shell = kit.column().apply { setBackgroundColor(kit.canvas) }
        val heading = kit.column().apply {
            setPadding(px(activity, 18), px(activity, 14), px(activity, 18), px(activity, 6))
            addView(kit.heading("Termux:Tasker"))
            addView(kit.secondary("Enhanced configuration • Original plugin handles execution and Save"))
        }
        shell.addView(heading)
        val tabs = arrayOf("Command", "Execution", "Results", "Startup", "Diagnostics")
        val nav = LinearLayout(activity).apply { orientation = LinearLayout.HORIZONTAL }
        val navScroll = android.widget.HorizontalScrollView(activity).apply {
            isHorizontalScrollBarEnabled = false
            addView(nav)
        }
        shell.addView(navScroll)
        val sections = ArrayList<LinearLayout>()
        fun section(title: String, help: String): LinearLayout {
            val group = kit.column().apply {
                setPadding(px(activity, 16), px(activity, 10), px(activity, 16), px(activity, 10))
            }
            group.addView(kit.heading(title))
            group.addView(kit.secondary(help))
            sections.add(group)
            return group
        }
        val command = section("Command", "Edit the original fields without executing any script.")
        val pairs = arrayOf(
            field(activity, "Executable", draft.executable, kit, stockName = "executable_path"),
            field(activity, "Arguments · preserved literally", draft.arguments, kit, multiline = true),
            field(activity, "Working directory", draft.workingDirectory, kit, stockName = "working_directory_path"),
            field(activity, "stdin · not logged", draft.stdin, kit, multiline = true),
        )
        command.addView(kit.card().apply {
            addView(pairs[0].first)
            addView(kit.secondary("Executable suggestions come from the original editor when available."))
            addView(kit.action("Select executable", primary = false) {
                choosePath(state, false, pairs[0].second)
            })
            addView(pairs[1].first)
            addView(pairs[2].first)
            addView(kit.action("Select working directory", primary = false) {
                choosePath(state, true, pairs[2].second)
            })
            addView(pairs[3].first)
        })
        // Keep exact original tokenizer and opt-in preview, never normalize raw argv.
        val tokenFeedback = kit.secondary("Argument token preview hidden. Original tokenizer; no execution.")
        command.addView(kit.card().apply {
            addView(kit.action("Preview arguments with original tokenizer", primary = false) {
                val preview = ArgumentStudio.inspect(pairs[1].second.text.toString()) { text ->
                    val parser = Class.forName("com.termux.tasker.ArgumentTokenizer", false, activity.classLoader)
                    @Suppress("UNCHECKED_CAST")
                    (XposedHelpers.callStaticMethod(parser, "tokenize", text) as? List<*>)
                        ?.map { it as? String ?: error("Unexpected tokenizer output") }
                }
                tokenFeedback.text = preview.display()
            })
            addView(tokenFeedback)
        })
        val execution = section("Execution", "Background is usually preferable for boot-time tasks.")
        val terminal = CheckBox(activity).apply { text = "Execute in terminal session"; isChecked = draft.inTerminal }
        val action = field(activity, "Terminal session action · raw value", draft.sessionAction, kit)
        val level = field(activity, "Background log level · raw value", draft.logLevel, kit)
        execution.addView(kit.card().apply {
            addView(terminal)
            addView(action.first)
            addView(level.first)
            addView(kit.secondary("Hidden values remain staged and unchanged. Android may restrict terminal launches in the background."))
        })
        fun updateModeVisibility() {
            action.first.visibility = if (terminal.isChecked) View.VISIBLE else View.GONE
            level.first.visibility = if (terminal.isChecked) View.GONE else View.VISIBLE
        }
        terminal.setOnClickListener { updateModeVisibility() }
        updateModeVisibility()
        val results = section("Results", "Tasker controls the timeout; never retry a possibly executed script.")
        val wait = CheckBox(activity).apply { text = "Wait for result"; isChecked = draft.waitForResult }
        results.addView(kit.card().apply {
            addView(wait)
            addView(kit.secondary("Background: %stdout, %stdout_original_length, %stderr, %stderr_original_length, " +
                "%result, %err, %errmsg. Terminal: combined transcript. Local callback return is not proof of Tasker acceptance."))
        })
        val boot = section("Startup readiness", "The failed boot action worked manually after startup.")
        val bootObservation = kit.secondary("Startup observation not yet refreshed.")
        fun refreshBootObservation() {
            val unlocked = try {
                (activity.getSystemService(Context.USER_SERVICE) as? UserManager)?.isUserUnlocked == true
            } catch (_: Throwable) { false }
            val stateNow = BootReadinessPolicy.evaluate(
                BootReadinessPolicy.Evidence(unlocked, null, null))
            bootObservation.text = if (unlocked)
                "User unlocked · Termux service readiness UNVERIFIED · ${stateNow.name}"
            else "Device locked or unlock state unavailable · ${stateNow.name}"
        }
        boot.addView(kit.card().apply {
            addView(bootObservation)
            addView(kit.action("Refresh startup state", primary = false) { refreshBootObservation() })
            addView(kit.label("Boot-time action policy"))
            addView(kit.secondary(BootReadinessPolicy.explanation))
            addView(kit.action("Show safe Tasker boot setup", primary = false) {
                AlertDialog.Builder(activity).setTitle("Tasker: safe boot setup")
                    .setMessage(BootReadinessPolicy.taskerInstructions)
                    .setPositiveButton("OK", null).show()
            })
            addView(kit.secondary("This editor cannot safely delay an existing Tasker action. " +
                "No command or callback is queued or replayed by AutoNexus."))
        })
        refreshBootObservation()
        val diagnostic = section("Diagnostics", "Privacy-safe local phases, not Tasker host acknowledgment.")
        val feedback = kit.secondary(if (state.unknownKeys)
            "Unknown legacy Tasker fields detected. Enhanced Save is disabled; use Original."
            else "No new Bundle keys, permission changes, root calls, or script execution.")
        diagnostic.addView(kit.card().apply {
            addView(feedback)
            addView(kit.secondary("LSPosed TT_TRACE is process-local. Consult TT-F0 boot evidence before behavior changes."))
        })
        val footer = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(px(activity, 12), px(activity, 9), px(activity, 12), px(activity, 12))
            setBackgroundColor(kit.canvas)
        }
        val fields = arrayOf(pairs[0].second, pairs[1].second, pairs[2].second,
            pairs[3].second, action.second, level.second)
        state.inputs = fields; state.terminal = terminal; state.waitForResult = wait
        val dialog = Dialog(activity).apply { setCancelable(false) }
        val sectionHost = kit.column()
        sections.forEach { sectionHost.addView(it) }
        val scroll = ScrollView(activity).apply { isFillViewport = true; addView(sectionHost) }
        shell.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        val feedbackTop = kit.secondary(if (state.unknownKeys)
            "Original action contains unsupported fields — use Original." else "Changes are staged until Save.")
        feedbackTop.setPadding(px(activity, 16), 0, px(activity, 16), 0)
        shell.addView(feedbackTop)
        state.validation = feedbackTop  // fixed, always visible; picker errors cannot disappear below the fold
        fun footerButton(caption: String, primary: Boolean, action: () -> Unit) {
            footer.addView(kit.action(caption, primary, action), LinearLayout.LayoutParams(0, -2, 1f).apply {
                marginEnd = px(activity, 5)
            })
        }
        footerButton("Save", true) { save(state) }
        footerButton("Original", false) { originalEditor(state) }
        footerButton("Discard", false) { discard(state) }
        shell.addView(footer)
        lateinit var activateTab: (Int) -> Unit
        activateTab = { i ->
            sections.forEachIndexed { index, container -> container.visibility = if (i == index) View.VISIBLE else View.GONE }
            nav.removeAllViews()
            tabs.forEachIndexed { index, name ->
                nav.addView(kit.nav(name, index == i) { activateTab(index) }, LinearLayout.LayoutParams(-2, -2).apply {
                    marginStart = px(activity, 6)
                })
            }
            scroll.scrollTo(0, 0)
        }
        activateTab(0)
        dialog.setContentView(shell)
        state.dialog = dialog
        dialog.show()
        dialog.window?.apply {
            setLayout((activity.resources.displayMetrics.widthPixels * .96).toInt(),
                (activity.resources.displayMetrics.heightPixels * .87).toInt())
            setGravity(Gravity.CENTER)
            setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        }
    }

    /**
     * Explicit opt-in read-only explorer, restricted to the original Termux
     * scripts tree. Missing Android UID permissions are shown, never bypassed.
     * Results are returned only while this editor is still alive and active.
     */
    private fun browseScripts(state: Session, folder: File, directoryOnly: Boolean) {
        val activity = state.activity
        if (!state.enhanced || state.dialog?.isShowing != true) return
        val generation = state.browseGeneration.incrementAndGet()
        state.validation?.text = "Inspecting scripts with existing app permissions; no root or execution."
        Thread {
            val listing = ScriptExplorer.browse(File(ScriptExplorer.TERMUX_SCRIPTS), folder)
            activity.runOnUiThread {
                if (activity.isFinishing || activity.isDestroyed || !state.enhanced ||
                    state.dialog?.isShowing != true || generation != state.browseGeneration.get()) return@runOnUiThread
                if (!listing.available) {
                    state.validation?.text = listing.message
                    val targetInput = state.inputs?.getOrNull(if (directoryOnly) 2 else 0)
                    if (targetInput != null) {
                        AlertDialog.Builder(activity)
                            .setTitle("Script browser unavailable")
                            .setMessage(listing.message + "\nYou can still enter a path manually, " +
                                "or use the original editor's suggestions.")
                            .setPositiveButton("Enter path") { _, _ -> manualPath(state, directoryOnly, targetInput) }
                            .setNegativeButton("Cancel", null).show()
                    }
                    return@runOnUiThread
                }
                val names = ArrayList<String>()
                val choices = ArrayList<ScriptExplorer.Entry?>()
                val root = File(ScriptExplorer.TERMUX_SCRIPTS).canonicalFile
                val current = folder.canonicalFile
                if (current != root) {
                    names.add("↑ Parent directory")
                    choices.add(ScriptExplorer.Entry("..", current.parentFile.absolutePath,
                        ScriptExplorer.Kind.DIRECTORY))
                }
                if (directoryOnly) {
                    names.add("Use this directory as working directory (staged)")
                    choices.add(null)
                }
                listing.entries.forEach { item ->
                    names.add(if (item.kind == ScriptExplorer.Kind.DIRECTORY)
                        "📁 ${item.label}" else "📄 ${item.label}")
                    choices.add(item)
                }
                val title = if (listing.truncated) "Browse scripts · first 60 entries" else "Browse scripts · read-only"
                AlertDialog.Builder(activity).setTitle(title)
                    .setItems(names.toTypedArray()) { _, index ->
                        val selected = choices[index]
                        if (selected == null && directoryOnly) {
                            state.inputs?.getOrNull(2)?.setText(current.absolutePath)
                            state.validation?.text = "Working directory staged; Save is still required."
                        } else if (selected?.kind == ScriptExplorer.Kind.DIRECTORY) {
                            browseScripts(state, File(selected.path), directoryOnly)
                        } else if (selected?.kind == ScriptExplorer.Kind.REGULAR_FILE && !directoryOnly) {
                            state.inputs?.getOrNull(0)?.setText(selected.path)
                            state.validation?.text = "Executable path staged; Save is still required."
                        }
                    }
                    .setNeutralButton("Enter path") { _, _ ->
                        state.inputs?.getOrNull(if (directoryOnly) 2 else 0)?.let {
                            manualPath(state, directoryOnly, it)
                        }
                    }
                    .setNegativeButton("Cancel", null).show()
            }
        }.apply { isDaemon = true; start() }
    }

    private fun ranges(state: Session): Pair<IntRange?, IntRange?> {
        val loader = state.activity.classLoader
        return try {
            val service = Class.forName(
                "com.termux.shared.termux.TermuxConstants\$TERMUX_APP\$TERMUX_SERVICE", false, loader)
            val logger = Class.forName("com.termux.shared.logger.Logger", false, loader)
            val session = XposedHelpers.getStaticIntField(service, "MIN_VALUE_EXTRA_SESSION_ACTION")..
                XposedHelpers.getStaticIntField(service, "MAX_VALUE_EXTRA_SESSION_ACTION")
            val log = XposedHelpers.getStaticIntField(logger, "LOG_LEVEL_OFF")..
                XposedHelpers.getStaticIntField(logger, "MAX_LOG_LEVEL")
            session to log
        } catch (_: Throwable) { null to null }
    }

    private fun writeStock(activity: Activity, draft: EditorDraft, preserveNullStdin: Boolean = false) {
        val values = arrayOf(draft.executable, draft.arguments, draft.workingDirectory,
            draft.sessionAction, draft.logLevel)
        fieldNames.forEachIndexed { index, key ->
            val target = textField(activity, key) ?: error("Missing stock input")
            target.setText(values[index])
        }
        (check(activity, "in_terminal") ?: error("Missing terminal toggle")).isChecked = draft.inTerminal
        (check(activity, "wait_for_result") ?: error("Missing result toggle")).isChecked = draft.waitForResult
        XposedHelpers.setObjectField(activity, "mStdin", if (preserveNullStdin && draft.stdin.isEmpty()) null else draft.stdin)
        XposedHelpers.callMethod(activity, "updateStdinViewText")
    }

    private fun save(state: Session) {
        val draft = state.current()
        state.mode.stage(draft)
        val (session, log) = ranges(state)
        val errors = draft.validationProblems(session, log)
        if (state.unknownKeys) {
            state.validation?.text = "Unrecognized Tasker action fields; use Original without converting the bundle."
            return
        }
        if (errors.isNotEmpty()) {
            state.validation?.text = errors.joinToString("\n")
            return
        }
        val old = stockDraft(state.activity) ?: run {
            state.validation?.text = "Stock editor unavailable; no changes saved."
            return
        }
        try {
            writeStock(state.activity, draft, state.stdinWasNull)
        } catch (_: Throwable) {
            try { writeStock(state.activity, old, state.stdinWasNull) } catch (_: Throwable) { /* never fabricate success */ }
            state.validation?.text = "Cannot update original fields; no save attempted. Use Original."
            return
        }
        state.finishApproved = true
        state.dismiss()
        // The ORIGINAL app generates the Locale result and returned variables.
        state.activity.finish()
    }

    private fun originalEditor(state: Session) {
        val currentStock = stockDraft(state.activity) ?: run {
            state.validation?.text = "Cannot snapshot Original safely. No fields changed."
            return
        }
        val pending = state.current()
        fun openOriginal() {
            // Discard staged-only values and keep the current stock widgets untouched.
            state.mode.returnToOriginal(currentStock)
            state.stdinWasNull = stockStdinWasNull(state.activity)
            state.dismiss()
        }
        if (state.mode.needsDiscardConfirmation(pending, currentStock)) {
            AlertDialog.Builder(state.activity)
                .setTitle("Discard enhanced draft?")
                .setMessage("Original keeps its current fields. Uncommitted enhanced edits are discarded.")
                .setNegativeButton("Keep editing", null)
                .setPositiveButton("Discard draft & open Original") { _, _ -> openOriginal() }
                .show()
        } else openOriginal()
    }

    private fun discard(state: Session) {
        AlertDialog.Builder(state.activity)
            .setTitle("Discard changes to Tasker action?")
            .setMessage("No modified Tasker bundle will be returned. This does not stop or retry any script.")
            .setNegativeButton("Keep editing", null)
            .setPositiveButton("Discard") { _, _ ->
                try {
                    XposedHelpers.setBooleanField(state.activity, "mIsCancelled", true)
                    state.finishApproved = true
                    state.dismiss()
                    state.activity.setResult(Activity.RESULT_CANCELED)
                    state.activity.finish()
                } catch (_: Throwable) { /* do not accidentally save */ }
            }.show()
    }

    private fun confirmExit(state: Session) {
        AlertDialog.Builder(state.activity)
            .setTitle("Leave enhanced editor?")
            .setMessage("Back/Home does not save implicitly. Return to your draft or discard changes.")
            .setNegativeButton("Keep editing") { _, _ -> show(state) }
            .setPositiveButton("Discard action edits") { _, _ -> discard(state) }
            .show()
    }
}
