"""TT-F1 current UI and boot-workflow regression gates; no historic handoff chains."""
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
P = ROOT / 'app/src/main/java/io/github/mikeyphw/autonexus/termuxtasker'

def read(name): return (P / name).read_text(encoding='utf-8')

class TermuxTaskerF1Tests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.editor = read('EnhancedEditorHooks.kt')
        cls.style = read('EditorUiKit.kt')
        cls.readiness = read('BootReadinessPolicy.kt')

    def test_stock_autocomplete_adapters_restored_in_both_path_fields(self):
        for marker in ('AutoCompleteTextView(activity)', 'bindAutocomplete(activity, this, stockName',
                       'source.adapter as? ArrayAdapter<*>', 'view.setOnFocusChangeListener',
                       'stockName = "executable_path"', 'stockName = "working_directory_path"'):
            self.assertIn(marker, self.editor)
        self.assertIn('showDropDown()', self.editor)
        self.assertIn('ScriptExplorer.browse(root, folder)', self.editor)
        self.assertIn('debounce.postDelayed(job, 180L)', self.editor)
        self.assertIn('debounce.removeCallbacks(it)', self.editor)
        self.assertIn('EditorPathSuggestions.displayOptions(root, folder, query, result, directoryOnly)', self.editor)
        self.assertIn('adapter = ArrayAdapter(activity, android.R.layout.simple_dropdown_item_1line, values)', self.editor)
        self.assertIn('InputType.TYPE_CLASS_TEXT else', self.editor)

    def test_both_select_buttons_have_real_fallback_when_inaccessible(self):
        for marker in ('Select executable', 'Select working directory',
                       'choosePath(state, false, pairs[0].second)',
                       'choosePath(state, true, pairs[2].second)',
                       '.setPositiveButton("Browse scripts")', '.setNeutralButton("Enter path")',
                       'manualPath(state, directoryOnly, targetInput)',
                       'state.validation = feedbackTop',
                       'browseScripts(state, root, directoryOnly)'):
            self.assertIn(marker, self.editor)
        self.assertNotIn('ACTION_OPEN_DOCUMENT', self.editor)  # URI is not a shell executable

    def test_modern_editor_has_fixed_footer_sections_and_adaptive_palette(self):
        for marker in ('"Command", "Execution", "Results", "Startup", "Diagnostics"',
                       'LinearLayout.LayoutParams(-1, 0, 1f)', 'shell.addView(footer)',
                       'footerButton("Save", true)', 'footerButton("Original", false)',
                       'footerButton("Discard", false)', 'activateTab = { i ->',
                       'setBackgroundColor(kit.canvas)'):
            self.assertIn(marker, self.editor)
        for marker in ('Configuration.UI_MODE_NIGHT_MASK', 'Color.parseColor',
                       'GradientDrawable', 'cornerRadius', 'contentDescription',
                       'textSize = 13f', 'TextView(activity)', 'setOnClickListener { onClick() }'):
            self.assertIn(marker, self.style)

    def test_boot_policy_requires_external_tasker_guard_never_delays_in_flight(self):
        for marker in ('DEFAULT_GRACE_SECONDS = 180L', 'Phase.LOCKED',
                       'Phase.GRACE_PERIOD', 'Phase.DEPENDENCY_UNVERIFIED',
                       'isExecutionConfirmed(e: Evidence): Boolean = false',
                       'No command or callback is queued or replayed'):
            self.assertIn(marker, self.readiness + self.editor)
        for forbidden in ('Thread.sleep(', 'startService(', 'sendBroadcast(', 'PendingIntent',
                          'su -c', 'chmod 777', 'ProcessBuilder(', 'Runtime.getRuntime('):
            self.assertNotIn(forbidden,self.editor+self.readiness)

    def test_original_tasker_serializer_still_exclusive(self):
        self.assertIn('state.activity.finish()', self.editor)
        self.assertIn('writeStock(state.activity, draft, state.stdinWasNull)', self.editor)
        self.assertIn('setBooleanField(state.activity, "mIsCancelled", true)', self.editor)
        self.assertIn('state.mode.reopenEnhanced(currentStock)', self.editor)
        self.assertNotIn('generateBundle(', self.editor)

    def test_past_editor_regressions_still_discovered(self):
        pytests = [p.name for p in (ROOT / 'tools').glob('test_termux_tasker*.py')]
        for name in ('test_termux_tasker_contract.py', 'test_termux_tasker_editor.py',
                     'test_termux_tasker_e2.py', 'test_termux_tasker_evidence.py'):
            self.assertIn(name,pytests)

if __name__ == '__main__':unittest.main()
