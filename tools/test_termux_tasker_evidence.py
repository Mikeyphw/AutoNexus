"""TT-F0 current evidence-review contract, no legacy/handoff-only gates."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
BASE = 'app/src/main/java/io/github/mikeyphw/autonexus/termuxtasker/'

def read(path): return (ROOT / path).read_text(encoding='utf-8')

class TTFOEvidenceSourceTests(unittest.TestCase):
    def test_identity_gate_and_inert_original_transport_unchanged(self):
        h = read(BASE + 'ReadOnlyTraceHooks.kt')
        assert 'TrustedTarget.inspect(context).supported' in h
        assert 'installPinnedHooks(load.classLoader, process)' in h
        assert h.index('TrustedTarget.inspect(context).supported') < h.index('installPinnedHooks(load.classLoader, process)')
        for forbidden in ('param.setResult(', 'param.setThrowable(', '.sendBroadcast(', '.startService(',
                          'ProcessBuilder(', 'Runtime.getRuntime', 'getBundleExtra('):
            self.assertNotIn(forbidden, h)
        g = read('app/src/main/java/io/github/mikeyphw/autonexus/core/RuntimeGate.kt')
        self.assertIn('const val BEHAVIOR_HOOKS_ENABLED = false', g)
        self.assertIn('const val ROOT_ENABLED = false', g)

    def test_import_requires_explicit_document_selection_with_small_bounded_input(self):
        panel = read(BASE + 'TermuxTaskerPanel.kt')
        self.assertIn('rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument())', panel)
        self.assertIn('importer.launch(arrayOf(', panel)
        self.assertIn('imported = result', panel)
        self.assertIn('withContext(Dispatchers.IO)', panel)
        self.assertIn('TraceEvidence.MAX_IMPORT_BYTES - captured.size()', panel)
        self.assertIn('TraceEvidence.analyze(', panel)
        for forbidden in ('ClipboardManager', 'getPrimaryClip(', 'su -c', 'Runtime.getRuntime',
                          'exec(', 'startService(', 'sendBroadcast(', 'getExternalStorageDirectory('):
            self.assertNotIn(forbidden, panel)

    def test_export_optin_and_enum_only_no_raw_log_persistence(self):
        panel = read(BASE + 'TermuxTaskerPanel.kt')
        model = read(BASE + 'TraceEvidence.kt')
        self.assertIn('ActivityResultContracts.CreateDocument("text/plain")', panel)
        self.assertIn('exporter.launch("autonexus-tt-f0-evidence.txt")', panel)
        self.assertIn('report.sanitizedReport()', panel)
        self.assertIn('const val MAX_INSPECTED_LINES = 12_000', model)
        self.assertIn('const val MAX_LINE_CHARS = 1024', model)
        self.assertIn('const val MAX_IMPORT_BYTES = 512 * 1024', model)
        self.assertIn('correlation=PROCESS_LOCAL_ONLY', model)
        for forbidden in ('logLine.toString()', 'writeText(line', 'putString("logs"',
                          'getSharedPreferences(', 'XposedBridge.log(', 'getStringExtra('):
            self.assertNotIn(forbidden, model)

    def test_absent_phase_never_claims_tasker_non_delivery(self):
        model = read(BASE + 'TraceEvidence.kt')
        self.assertIn('Missing events do not prove Tasker failed to dispatch', model)
        self.assertIn('host acceptance=UNKNOWN', model)
        self.assertIn('Execution and callback remain UNKNOWN', model)
        self.assertIn('if (inspected >= MAX_INSPECTED_LINES || accepted >= MAX_ACCEPTED_EVENTS)', model)

if __name__ == '__main__': unittest.main()
