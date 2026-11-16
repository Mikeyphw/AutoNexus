package io.github.mikeyphw.autonexus.termuxtasker

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
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

    private class Session(val activity: Activity, val original: EditorDraft,
                          var draft: EditorDraft, val unknownKeys: Boolean,
                          val stdinWasNull: Boolean) {
        var enhanced = true
        var finishApproved = false
        var dialog: Dialog? = null
        var inputs: Array<EditText>? = null
        var terminal: CheckBox? = null
        var waitForResult: CheckBox? = null
        var validation: TextView? = null
        fun current(): EditorDraft {
            val v = inputs ?: return draft
            return EditorDraft(
                v[0].text.toString(), v[1].text.toString(), v[2].text.toString(),
                v[3].text.toString(), v[4].text.toString(), v[5].text.toString(),
                terminal?.isChecked == true, waitForResult?.isChecked == true,
            )
        }
        fun dismiss() { try { dialog?.dismiss() } catch (_: Throwable) { }
            dialog = null; inputs = null }
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
        val state = Session(activity, original, active, unknown, stdinWasNull)
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
            setOnClickListener { state.enhanced = true; show(state) }
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

    private fun field(activity: Activity, label: String, value: String,
                      multiline: Boolean = false): Pair<LinearLayout, EditText> {
        val container = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, px(activity, 8), 0, px(activity, 8))
        }
        container.addView(TextView(activity).apply { text = label; textSize = 15f })
        val edit = EditText(activity).apply {
            setText(value)
            setSingleLine(!multiline)
            if (multiline) { minLines = 2; maxLines = 5 }
            contentDescription = label
        }
        container.addView(edit, LinearLayout.LayoutParams(-1, -2))
        return container to edit
    }
    private fun px(activity: Activity, amount: Int): Int =
        (amount * activity.resources.displayMetrics.density).toInt()

    private fun show(state: Session) {
        val activity = state.activity
        if (activity.isFinishing || activity.isDestroyed) return
        if (state.dialog?.isShowing == true) return
        val draft = state.draft
        val root = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(px(activity, 16), px(activity, 12), px(activity, 16), px(activity, 16))
        }
        fun header(title: String, description: String) {
            root.addView(TextView(activity).apply {
                text = title; textSize = 19f
                setPadding(0, px(activity, 12), 0, px(activity, 4))
            })
            root.addView(TextView(activity).apply { text = description; textSize = 12f })
        }
        header("Command", "Fields are staged locally until Save to Tasker. No command is executed.")
        val pairs = arrayOf(
            field(activity, "Executable", draft.executable),
            field(activity, "Arguments · preserved literally", draft.arguments, true),
            field(activity, "Working directory", draft.workingDirectory),
            field(activity, "stdin · not logged", draft.stdin, true),
        )
        pairs.forEach { root.addView(it.first) }
        header("Execution", "Switching mode never discards mode-specific fields.")
        val terminal = CheckBox(activity).apply { text = "Execute in terminal session"; isChecked = draft.inTerminal }
        root.addView(terminal)
        val action = field(activity, "Terminal session action · raw value", draft.sessionAction)
        val level = field(activity, "Background log level · raw value", draft.logLevel)
        root.addView(action.first); root.addView(level.first)
        header("Results", "Tasker controls the host timeout. Waiting never means the command is safe to retry.")
        val wait = CheckBox(activity).apply { text = "Wait for result"; isChecked = draft.waitForResult }
        root.addView(wait)
        root.addView(TextView(activity).apply {
            text = "Background: %stdout, %stderr, %result, %err, %errmsg. " +
                   "Terminal: combined transcript; no independent stderr."
            textSize = 12f
        })
        header("Diagnostics", "LSPosed TT_TRACE reports local stages only, not Tasker host acceptance.")
        val feedback = TextView(activity).apply { textSize = 13f; text = if (state.unknownKeys)
            "Unknown legacy Tasker fields detected. Enhanced Save is disabled; use Original."
            else "No script execution, permission changes, or new Tasker bundle keys." }
        root.addView(feedback)
        val controls = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
        root.addView(controls)
        val fields = arrayOf(pairs[0].second, pairs[1].second, pairs[2].second,
            pairs[3].second, action.second, level.second)
        state.inputs = fields; state.terminal = terminal; state.waitForResult = wait; state.validation = feedback
        val dialog = Dialog(activity).apply { setCancelable(false) }
        fun button(caption: String, action: () -> Unit) {
            controls.addView(Button(activity).apply { text = caption; setOnClickListener { action() } },
                LinearLayout.LayoutParams(-1, -2))
        }
        button("Validate & Save to Tasker") { save(state) }
        button("Original editor (discard enhanced draft)") { originalEditor(state) }
        button("Discard Tasker changes") { discard(state) }
        button("Keep editing") { feedback.text = "Your draft is still open and has not been saved." }
        val scroll = ScrollView(activity).apply {
            isFillViewport = true
            addView(root)
        }
        dialog.setContentView(scroll)
        dialog.window?.apply {
            setLayout((activity.resources.displayMetrics.widthPixels * .96).toInt(),
                (activity.resources.displayMetrics.heightPixels * .88).toInt())
            setGravity(Gravity.CENTER)
            setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        }
        state.dialog = dialog
        dialog.show()
        // Platform Dialog doesn't support layout sizing before show() on all ROMs.
        dialog.window?.setLayout((activity.resources.displayMetrics.widthPixels * .96).toInt(),
            (activity.resources.displayMetrics.heightPixels * .88).toInt())
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
        state.draft = draft
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
        state.draft = state.current()
        if (state.draft != state.original) {
            AlertDialog.Builder(state.activity)
                .setTitle("Discard enhanced draft?")
                .setMessage("The original editor will retain its existing fields. Enhanced draft edits will not be saved.")
                .setNegativeButton("Keep editing", null)
                .setPositiveButton("Discard draft & open Original") { _, _ ->
                    state.enhanced = false
                    state.dismiss()
                }.show()
        } else { state.enhanced = false; state.dismiss() }
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
