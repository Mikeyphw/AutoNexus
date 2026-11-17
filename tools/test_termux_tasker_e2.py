"""TT-E2: bounded, read-only UI additions; original serializer and IPC unchanged."""
from pathlib import Path
import unittest

R = Path(__file__).resolve().parents[1]
P = 'app/src/main/java/io/github/mikeyphw/autonexus/termuxtasker/'
def read(p): return (R/p).read_text(encoding='utf-8')

class EditorE2ContractTests(unittest.TestCase):
    def test_exact_original_argument_tokenizer_only(self):
        s = read(P+'EnhancedEditorHooks.kt')
        self.assertIn('Class.forName("com.termux.tasker.ArgumentTokenizer"', s)
        self.assertIn('XposedHelpers.callStaticMethod(parser, "tokenize", text)', s)
        self.assertIn('ArgumentStudio.inspect(pairs[1].second.text.toString())', s)
        model=read(P+'ArgumentStudio.kt')
        self.assertNotIn('Runtime.getRuntime',model)
        self.assertNotIn('ProcessBuilder',model)
        self.assertNotIn('split(" ")',model)
    def test_explicit_preview_not_automatic(self):
        s=read(P+'EnhancedEditorHooks.kt')
        self.assertIn('Preview arguments with original tokenizer',s)
        self.assertIn('setOnClickListener {',s)
        self.assertIn('Argument token preview hidden.',s)
        self.assertIn('MAX_PREVIEW_CHARS = 8192',read(P+'ArgumentStudio.kt'))
    def test_browse_is_optin_and_readonly_and_bounded(self):
        s=read(P+'EnhancedEditorHooks.kt')
        explorer=read(P+'ScriptExplorer.kt')
        self.assertIn('Browse Termux scripts (read-only, if accessible)',s)
        self.assertIn('browseScripts(state, File(ScriptExplorer.TERMUX_SCRIPTS), false)',s)
        self.assertIn('val browseGeneration = AtomicInteger(0)',s)
        self.assertIn('MAX_ENTRIES = 60',explorer)
        self.assertIn('MAX_DEPTH = 5',explorer)
        self.assertIn('MAX_INSPECTED = 256',explorer)
        self.assertIn('if (inspected >= MAX_INSPECTED) { more = true; break }', explorer)
        self.assertIn('inspected++', explorer)
        self.assertIn('if (Files.isSymbolicLink(segment))',explorer)
        self.assertIn('Files.isSymbolicLink(entry)',explorer)
        self.assertIn('Files.newDirectoryStream',explorer)
        self.assertIn('if (!inside(canonicalRoot, canonicalFolder))',explorer)
        for forbidden in ('su -c', 'chmod', 'FileInputStream', 'readBytes(', 'readText(',
                          'Runtime.getRuntime', 'ProcessBuilder', 'walkTopDown', 'listFiles(',
                          'Files.write', 'Files.copy', 'Files.move'):
            self.assertNotIn(forbidden,explorer)
    def test_editor_reentry_re_reads_stock_and_discards_stale_draft(self):
        s=read(P+'EnhancedEditorHooks.kt')
        bridge=read(P+'EditorDraftBridge.kt')
        self.assertIn('val currentStock = stockDraft(activity) ?: return@setOnClickListener',s)
        self.assertIn('state.mode.reopenEnhanced(currentStock)',s)
        self.assertIn('state.mode.returnToOriginal(currentStock)',s)
        self.assertIn('state.mode.needsDiscardConfirmation(pending, currentStock)',s)
        self.assertIn('state.stdinWasNull = stockStdinWasNull(activity)',s)
        self.assertIn('draft = stockNow',bridge)
        self.assertNotIn('state.enhanced =',s)
        self.assertNotIn('state.draft =',s)
    def test_switching_modes_does_not_erase_hidden_controls(self):
        s=read(P+'EnhancedEditorHooks.kt')
        self.assertIn('action.first.visibility',s)
        self.assertIn('level.first.visibility',s)
        self.assertIn('terminal.setOnClickListener { updateModeVisibility() }',s)
        self.assertNotIn('action.second.setText("")',s)
        self.assertNotIn('level.second.setText("")',s)
    def test_original_save_and_cancel_still_own_output(self):
        s=read(P+'EnhancedEditorHooks.kt')
        self.assertIn('writeStock(state.activity, draft, state.stdinWasNull)',s)
        self.assertIn('state.activity.finish()',s)
        self.assertIn('mIsCancelled',s)
        self.assertIn('Unknown legacy Tasker fields detected',s)
        for forbidden in ('generateBundle(', 'sendBroadcast(', '.startService(',
                          'XposedBridge.log(', 'startForegroundService(', 'putExtra(',
                          'termux-wake-lock', 'getExternalFilesDir(', 'ClipboardManager'):
            self.assertNotIn(forbidden,s)
    def test_four_domain_gate_and_no_root_unchanged(self):
        self.assertIn('const val BEHAVIOR_HOOKS_ENABLED = false',
                      read('app/src/main/java/io/github/mikeyphw/autonexus/core/RuntimeGate.kt'))
        self.assertIn('const val ROOT_ENABLED = false',
                      read('app/src/main/java/io/github/mikeyphw/autonexus/core/RuntimeGate.kt'))
        self.assertIn('versionCode = 5',read('app/build.gradle.kts'))
        self.assertIn('versionName = "0.5.0-tt-e2"',read('app/build.gradle.kts'))
    def test_jvm_tests_use_android_supported_file_writes(self):
        source = read('app/src/test/java/io/github/mikeyphw/autonexus/termuxtasker/ScriptExplorerTest.kt')
        self.assertNotIn('Files.writeString(', source)
        self.assertIn('Files.write(', source)
        self.assertIn('toByteArray(Charsets.UTF_8)', source)
        self.assertIn('ByteArray(0)', source)

    def test_docs_do_not_claim_ipc_fix(self):
        s=read('docs/TERMUX_TASKER_IMPLEMENTATION.md')
        self.assertIn('NO IPC FIX CLAIM',s)
        self.assertIn('NO FULL EDITOR CLAIM',s)
        self.assertIn('NO PHYSICAL DEVICE TRACE CLAIM',s)

if __name__=='__main__': unittest.main()
