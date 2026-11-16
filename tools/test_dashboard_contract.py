"""Dashboard stage source safety assertions; existing bootstrap tests remain active."""
import pathlib
import re
import unittest
import xml.etree.ElementTree as ET

ROOT = pathlib.Path(__file__).resolve().parents[1]


def read(rel):
    return (ROOT / rel).read_text(encoding="utf-8")


def verified_plugin_build(root_gradle, app_gradle):
    assert 'id("org.jetbrains.kotlin.plugin.compose") version "2.3.21" apply false' in root_gradle
    assert 'id("org.jetbrains.kotlin.plugin.compose")' in app_gradle
    assert 'compose = true' in app_gradle
    assert 'androidx.compose:compose-bom:2026.09.00' in app_gradle
    assert 'androidx.compose.material3:material3' in app_gradle
    assert 'androidx.activity:activity-compose:1.13.0' in app_gradle
    assert 'jvmTarget.set(JvmTarget.JVM_17)' in app_gradle
    assert 'kotlinOptions' not in app_gradle
    assert re.search(r'versionCode\s*=\s*2\b', app_gradle)


def settings_are_private(source):
    assert 'Context.MODE_PRIVATE' in source
    assert 'preferences.edit()' in source
    assert '.commit()' in source
    assert 'if (!read(domain).readable) return false' in source
    for bad in ('MODE_WORLD_READABLE', 'MODE_WORLD_WRITEABLE', 'createDeviceProtectedStorageContext',
                'getExternalFilesDir', 'FileProvider', 'android.os.ServiceManager', 'su -c'):
        assert bad not in source, bad


def workflow_is_exact_source_and_bounded(workflow):
    assert 'ref: ${{ github.event.pull_request.head.sha || github.sha }}' in workflow
    assert 'persist-credentials: false' in workflow
    assert 'EXPECTED_HEAD: ${{ github.event.pull_request.head.sha || github.sha }}' in workflow
    assert 'test "$(git rev-parse HEAD)" = "$EXPECTED_HEAD"' in workflow
    assert 'ea165f8d65b6e75b540449e92b4886f43607fa02' in workflow
    assert 'retention-days: 7' in workflow
    assert 'app-debug.apk.sha256' in workflow
    assert 'source-commit.txt' in workflow
    assert 'sha256sum app-debug.apk' in workflow
    assert 'python3 -m unittest discover' in workflow
    assert ':app:assembleDebug :app:testDebugUnitTest :app:lintDebug' in workflow
    assert 'verify_bootstrap.py --repo . --apk' in workflow


class AndroidStageContractTests(unittest.TestCase):
    def test_material3_and_typed_kotlin(self):
        verified_plugin_build(read('build.gradle.kts'), read('app/build.gradle.kts'))

    def test_legacy_kotlin_dsl_rejected(self):
        with self.assertRaises(AssertionError):
            verified_plugin_build(read('build.gradle.kts'), read('app/build.gradle.kts').replace(
                'jvmTarget.set(JvmTarget.JVM_17)', 'kotlinOptions { jvmTarget = "17" }'))

    def test_compose_mismatched_bom_rejected(self):
        with self.assertRaises(AssertionError):
            verified_plugin_build(read('build.gradle.kts'), read('app/build.gradle.kts').replace('2026.09.00', '2024.01.00'))

    def test_settings_private_and_fail_closed(self):
        settings_are_private(read('app/src/main/java/io/github/mikeyphw/autonexus/settings/DomainSettingsStore.kt'))

    def test_world_readable_downgrade_rejected(self):
        with self.assertRaises(AssertionError):
            settings_are_private(read('app/src/main/java/io/github/mikeyphw/autonexus/settings/DomainSettingsStore.kt').replace('Context.MODE_PRIVATE', 'Context.MODE_WORLD_READABLE'))

    def test_runtime_hooks_remain_off(self):
        gate = read('app/src/main/java/io/github/mikeyphw/autonexus/core/RuntimeGate.kt')
        for flag in ('BEHAVIOR_HOOKS_ENABLED', 'SYSTEM_UI_ENABLED', 'SYSTEM_SERVER_ENABLED', 'ROOT_ENABLED'):
            self.assertRegex(gate, rf'const val {flag} = false\b')
        self.assertIn('return false', read('app/src/main/java/io/github/mikeyphw/autonexus/core/RootPolicy.kt'))
        ui = read('app/src/main/java/io/github/mikeyphw/autonexus/MainActivity.kt')
        self.assertIn('Safe-off', ui)
        self.assertIn('LSPosed enabled/scope status: UNKNOWN', ui)
        self.assertIn('Requested future enhancements', ui.replace('Request future enhancements', 'Requested future enhancements'))
        self.assertNotIn('findAndHookMethod(', ui)
        self.assertNotIn('ProcessBuilder(', ui)

    def test_exact_scope_without_exported_provider(self):
        root = ET.parse(ROOT/'app/src/main/AndroidManifest.xml').getroot()
        app = root.find('application')
        self.assertEqual([], list(root.findall('uses-permission')))
        self.assertEqual([], list(app.findall('provider')))
        self.assertEqual([], list(app.findall('service')))
        self.assertEqual([], list(app.findall('receiver')))
        self.assertEqual(1, len(list(app.findall('activity'))))
        scope_xml = ET.parse(ROOT/'app/src/main/res/values/arrays.xml').getroot()
        scopes = [i.text for i in scope_xml.find("string-array[@name='xposed_scope']").findall('item')]
        self.assertEqual(['com.joaomgcd.autonotification', 'com.joaomgcd.autoinput',
                          'com.joaomgcd.autotools', 'com.termux.tasker'], scopes)

    def test_exact_sha_ci_source_and_debug_artifact(self):
        workflow_is_exact_source_and_bounded(read('.github/workflows/android-ci.yml'))

    def test_ci_must_reject_checkout_merge_ref(self):
        with self.assertRaises(AssertionError):
            workflow_is_exact_source_and_bounded(read('.github/workflows/android-ci.yml').replace(
                'ref: ${{ github.event.pull_request.head.sha || github.sha }}', 'ref: main'))

    def test_diagnostic_data_remains_local_only(self):
        store = read('app/src/main/java/io/github/mikeyphw/autonexus/settings/DomainSettingsStore.kt')
        activity = read('app/src/main/java/io/github/mikeyphw/autonexus/MainActivity.kt')
        for bad in ('ClipboardManager', 'startService(', 'sendBroadcast(', 'getContentResolver()',
                    'XposedBridge.log(', 'Thread.sleep(', 'HttpURLConnection', 'WebView'):
            self.assertNotIn(bad, store + activity)


if __name__ == '__main__':
    unittest.main()
