"""TT-E1: source invariants for the original Termux:Tasker editor bridge."""
from pathlib import Path
import unittest
import re

ROOT=Path(__file__).resolve().parents[1]
P='app/src/main/java/io/github/mikeyphw/autonexus/termuxtasker/'
def read(path):return (ROOT/path).read_text(encoding='utf-8')
class EditorBridgeTests(unittest.TestCase):
    def setUp(self):
        self.bridge=read(P+'EnhancedEditorHooks.kt')
        self.draft=read(P+'EditorContract.kt')
        self.trace=read(P+'ReadOnlyTraceHooks.kt')
        self.entry=read('app/src/main/java/io/github/mikeyphw/autonexus/hooks/AutoNexusEntry.kt')
    def test_version_signer_gate_protects_editor(self):
        self.assertIn('TrustedTarget.inspect(context).supported',self.trace)
        self.assertLess(self.trace.index('TrustedTarget.inspect(context).supported'),
                        self.trace.index('installPinnedHooks(load.classLoader, process)'))
        self.assertIn('safeInstall(process) { EnhancedEditorHooks.install(loader) }',self.trace)
        self.assertIn('if (process == TraceProtocol.Process.BACKGROUND)',self.trace)
        self.assertIn('domain == Domain.TERMUX_TASKER',self.entry)
    def test_hook_exact_stock_activity_and_original_lifecycle(self):
        for marker in ('com.termux.tasker.EditConfigurationActivity', '"onCreate"',
                       '"finish"','"onSaveInstanceState"','"onDestroy"'):
            self.assertIn(marker,self.bridge)
        self.assertIn('Activity::class.java, "onSaveInstanceState"',self.bridge)
        self.assertIn('Activity::class.java, "onDestroy"',self.bridge)
        self.assertLess(self.bridge.index('Activity::class.java, "onSaveInstanceState"'),
                        self.bridge.index('TARGET, loader, "onCreate"'))
        self.assertLess(self.bridge.index('TARGET, loader, "finish"'),
                        self.bridge.index('TARGET, loader, "onCreate"'))
    def test_all_eight_original_fields_retained(self):
        for value in ('"executable_path"','"arguments"','"working_directory_path"',
                      '"session_action"','"background_custom_log_level"',
                      '"in_terminal"','"wait_for_result"','"mStdin"'):
            self.assertIn(value,self.bridge)
        self.assertIn('original app\'s existing widgets',self.bridge)
    def test_save_uses_original_finish_and_no_locale_bundle_rewrite(self):
        self.assertIn('writeStock(state.activity, draft, state.stdinWasNull)',self.bridge)
        self.assertIn('state.activity.finish()',self.bridge)
        for banned in ('generateBundle(', 'putExtra(', 'sendBroadcast(', 'startService(',
                       'startForegroundService(', 'PendingIntent.get', 'ProcessBuilder(', 'Runtime.getRuntime'):
            self.assertNotIn(banned,self.bridge)
    def test_cancel_sets_original_cancel_flag_and_result(self):
        self.assertIn('setBooleanField(state.activity, "mIsCancelled", true)',self.bridge)
        self.assertIn('state.activity.setResult(Activity.RESULT_CANCELED)',self.bridge)
        self.assertIn('state.finishApproved = true',self.bridge)
    def test_back_while_enhanced_cannot_save_implicitly(self):
        self.assertIn('if (!state.enhanced || state.finishApproved || activity.isFinishing) return',self.bridge)
        self.assertIn('param.result = null',self.bridge)
        self.assertIn('confirmExit(state)',self.bridge)
    def test_unknown_fields_do_not_silently_convert(self):
        self.assertIn('EditorDraft.hasUnknownKeys(old.keySet())',self.bridge)
        self.assertIn('if (state.unknownKeys)',self.bridge)
        self.assertIn('Enhanced Save is disabled',self.bridge)
        self.assertIn('"net.dinglisch.android.tasker.extras.VARIABLE_REPLACE_KEYS"',self.draft)
    def test_restore_schema_is_versioned_and_private(self):
        self.assertIn('const val RESTORE_SCHEMA = 1',self.draft)
        self.assertIn('putBundle(STATE, toBundle(draft, state.stdinWasNull))',self.bridge)
        self.assertIn('bundle?.getInt("schema", -1)',self.bridge)
        self.assertNotIn('MODE_WORLD_READABLE',self.bridge)
    def test_validation_defers_tasker_variables(self):
        self.assertIn("value.contains('%')",self.draft)
        self.assertIn('MAX_STDIN_CHARS = 45000',self.draft)
        self.assertIn('range == null',self.draft)
    def test_original_fallback_discards_only_draft(self):
        self.assertIn('footerButton("Original", false) { originalEditor(state) }',self.bridge)
        self.assertIn('state.mode.returnToOriginal(currentStock)',self.bridge)
        self.assertIn('state.mode.reopenEnhanced(currentStock)',self.bridge)
        self.assertIn('state.mode.needsDiscardConfirmation(pending, currentStock)',self.bridge)
        self.assertIn('Discard draft & open Original',self.bridge)
    def test_no_secret_logging_in_bridge(self):
        for banned in ('XposedBridge.log(', 'Log.d(', 'Log.i(', 'Log.e(',
                       'printStackTrace(', 'getStringExtra(', 'getExternalFilesDir('):
            self.assertNotIn(banned,self.bridge)
    def test_no_other_module_domains_or_scope_added(self):
        self.assertNotIn('system_server',self.bridge)
        self.assertNotIn('com.android.systemui',self.bridge)
        self.assertNotIn('net.dinglisch.android.taskerm"',self.entry)
        self.assertIn('const val BEHAVIOR_HOOKS_ENABLED = false',
                      read('app/src/main/java/io/github/mikeyphw/autonexus/core/RuntimeGate.kt'))
    def test_stock_hooks_cannot_replay_commands(self):
        for source in (self.bridge,self.draft):
            for banned in ('su -c','sendBroadcast(','.startService(','.startForegroundService(',
                           'java.lang.ProcessBuilder','exec(', 'setThrowable('):
                self.assertNotIn(banned,source)

if __name__=='__main__':unittest.main()
