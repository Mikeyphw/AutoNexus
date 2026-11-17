"""TT-F0 regressions: real v0.9.0 hook boundaries, fail-closed identity and privacy."""
from pathlib import Path
import re
import unittest
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
JAVA = 'app/src/main/java/io/github/mikeyphw/autonexus/'

def read(path):
    return (ROOT/path).read_text(encoding='utf-8')

class TermuxTaskerContractTests(unittest.TestCase):
    def test_pinned_expected_plugin_version_and_signing_certificate(self):
        contract=read(JAVA+'termuxtasker/TraceProtocol.kt')
        self.assertIn('const val PLUGIN = "com.termux.tasker"',contract)
        self.assertIn('const val VERSION_NAME = "0.9.0"',contract)
        self.assertIn('const val VERSION_CODE = 1002L',contract)
        self.assertIn('228fb2cfe90831c1499ec3ccaf61e96e8e1ce70766b9474672ce427334d41c42',contract)
        self.assertIn('certificateSha256 == CERTIFICATE_SHA256',contract)

    def test_hooks_are_installed_only_after_identity_attestation(self):
        s=read(JAVA+'termuxtasker/ReadOnlyTraceHooks.kt')
        self.assertIn('Application::class.java, "attach"',s)
        self.assertLess(s.index('TrustedTarget.inspect(context).supported'),s.index('installPinnedHooks(load.classLoader, process)'))
        self.assertIn('if (!installed.compareAndSet(false, true)) return',s)
        self.assertIn('TraceProtocol.Phase.INCOMPATIBLE',s)
        self.assertIn('getPackageInfo(',read(JAVA+'termuxtasker/TrustedTarget.kt'))

    def test_hook_abi_matches_verified_v090_methods(self):
        s=read(JAVA+'termuxtasker/ReadOnlyTraceHooks.kt')
        for marker in ('"com.termux.tasker.FireReceiver"', '"onReceive"',
                       '"com.termux.tasker.utils.PluginUtils"',
                       '"sendExecuteIntentToExecuteService"', '"com.termux.tasker.PluginResultsService"',
                       '"onHandleIntent"', '"sendPendingResultToPluginHostApp"',
                       'TaskerPlugin\\$Setting', '"signalFinish"'):
            self.assertIn(marker,s)

    def test_trace_does_not_modify_execution_or_result(self):
        s=read(JAVA+'termuxtasker/ReadOnlyTraceHooks.kt')
        for forbidden in (r'param\.setResult\(',r'param\.setThrowable\(',
                          r'\.sendBroadcast\(',r'\.startService\(',r'\.startForegroundService\(',
                          r'PendingIntent\.getService',r'Runtime\.getRuntime',r'ProcessBuilder\(',
                          r'\bsu\s+-c\b'):
            self.assertNotRegex(s,forbidden)

    def test_trace_does_not_inspect_secret_payloads(self):
        s=read(JAVA+'termuxtasker/ReadOnlyTraceHooks.kt')
        for forbidden in ('getBundleExtra(', 'getStringExtra(', 'getExtras()',
                          '.getIntent()', 'arguments_string', '.stdin', 'stdout', 'stderr',
                          '.getMessage()', 'Throwable.printStackTrace'):
            self.assertNotIn(forbidden,s)
        self.assertIn('MAX_EVENTS_PER_PROCESS',s)

    def test_readiness_is_read_only_and_admits_unknowns(self):
        s=read(JAVA+'termuxtasker/TermuxTaskerPanel.kt')
        self.assertIn('net.dinglisch.android.taskerm',s)
        self.assertIn('com.termux.permission.RUN_COMMAND',s)
        self.assertIn('isUserUnlocked',s)
        self.assertIn('UNKNOWN',s)
        self.assertIn('No automatic script replay',s)
        self.assertNotRegex(s, r'\b(?:startService|sendBroadcast|grantRuntimePermission|exec)\(')

    def test_companion_permission_and_scope_remain_constrained(self):
        root=ET.parse(ROOT/'app/src/main/AndroidManifest.xml').getroot()
        app=root.find('application')
        self.assertFalse(root.findall('uses-permission'))
        self.assertFalse(app.findall('provider'))
        self.assertFalse(app.findall('receiver'))
        self.assertFalse(app.findall('service'))
        queries=[e.get('{http://schemas.android.com/apk/res/android}name') for e in root.find('queries')]
        self.assertIn('com.termux',queries)
        self.assertIn('net.dinglisch.android.taskerm',queries)
        scope=ET.parse(ROOT/'app/src/main/res/values/arrays.xml').getroot()
        items=[i.text for i in scope.find("string-array[@name='xposed_scope']").findall('item')]
        self.assertEqual(['com.joaomgcd.autonotification','com.joaomgcd.autoinput',
                          'com.joaomgcd.autotools','com.termux.tasker'],items)

    def test_no_other_domain_hook_enabled(self):
        s=read(JAVA+'core/RuntimeGate.kt')
        self.assertIn('const val BEHAVIOR_HOOKS_ENABLED = false',s)
        self.assertIn('const val ROOT_ENABLED = false',s)
        entry=read(JAVA+'hooks/AutoNexusEntry.kt')
        self.assertIn('if (domain == Domain.TERMUX_TASKER) ReadOnlyTraceHooks.arm(lpparam)',entry)
        self.assertIn('if (!RuntimeGate.mayInstallBehaviorHook(lpparam.packageName)) return',entry)

    def test_no_behavior_fixes_misrepresented_as_done(self):
        s=read('docs/TERMUX_TASKER_IMPLEMENTATION.md')
        for marker in ('NO IPC FIX CLAIM', 'NO FULL EDITOR CLAIM', 'NO PHYSICAL DEVICE TRACE CLAIM'):
            self.assertIn(marker,s)

if __name__=='__main__':
    unittest.main()
