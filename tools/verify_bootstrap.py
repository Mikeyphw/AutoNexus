#!/usr/bin/env python3
"""Read-only current-source bootstrap contract tests (no git, network or device)."""
import argparse, json, pathlib, re, sys, zipfile, xml.etree.ElementTree as ET

PACKAGES = (
    'com.joaomgcd.autonotification',
    'com.joaomgcd.autoinput',
    'com.joaomgcd.autotools',
    'com.termux.tasker',
)
ANDROID = '{http://schemas.android.com/apk/res/android}'

def check(root: pathlib.Path, apk: pathlib.Path | None) -> None:
    manifest = ET.parse(root/'app/src/main/AndroidManifest.xml').getroot()
    app = manifest.find('application')
    assert app is not None
    assert not any(x.tag == 'uses-permission' for x in manifest), 'no privilege requests at bootstrap'
    meta = {n.attrib.get(ANDROID+'name'): n for n in app.findall('meta-data')}
    assert meta['xposedmodule'].get(ANDROID+'value') == 'true'
    assert meta['xposedminversion'].get(ANDROID+'value') == '82'
    assert meta['xposedscope'].get(ANDROID+'resource') == '@array/xposed_scope'
    arrays = ET.parse(root/'app/src/main/res/values/arrays.xml').getroot()
    scope = arrays.find("string-array[@name='xposed_scope']")
    assert scope is not None
    assert tuple((n.text or '').strip() for n in scope.findall('item')) == PACKAGES
    assert (root/'app/src/main/assets/xposed_init').read_text().strip() == 'io.github.mikeyphw.autonexus.hooks.AutoNexusEntry'
    assert len(list((root/'app/src/main').rglob('*.kt'))) >= 4
    b = (root/'app/build.gradle.kts').read_text()
    assert 'compileOnly("de.robv.android.xposed:api:82")' in b
    assert 'implementation("de.robv.android.xposed:api:82")' not in b
    gate = (root/'app/src/main/java/io/github/mikeyphw/autonexus/core/RuntimeGate.kt').read_text()
    for k in ('BEHAVIOR_HOOKS_ENABLED', 'SYSTEM_UI_ENABLED', 'SYSTEM_SERVER_ENABLED', 'ROOT_ENABLED'):
        assert re.search(rf'const val {k} = false\b', gate), f'{k} must be false'
    root_policy = (root/'app/src/main/java/io/github/mikeyphw/autonexus/core/RootPolicy.kt').read_text()
    assert 'return false' in root_policy
    assert 'ProcessBuilder' not in root_policy and 'Runtime.getRuntime' not in root_policy
    entry = (root/'app/src/main/java/io/github/mikeyphw/autonexus/hooks/AutoNexusEntry.kt').read_text()
    assert 'RuntimeGate.mayInstallBehaviorHook' in entry
    assert 'findAndHookMethod' not in entry and 'hookAllMethods' not in entry
    assert all('com.android.systemui' not in x.text for x in scope.findall('item'))
    if apk is not None:
        with zipfile.ZipFile(apk) as z:
            names=set(z.namelist())
            assert 'assets/xposed_init' in names
            assert z.read('assets/xposed_init').decode().strip() == 'io.github.mikeyphw.autonexus.hooks.AutoNexusEntry'
            assert 'classes.dex' in names
            assert not any(n.startswith('de/robv/android/xposed/') for n in names), 'Xposed API classes must not ship'
    print('AUTONEXUS_BOOTSTRAP_STATIC_NEGATIVE_PASS', 'APK_INSPECTED' if apk else 'APK_NOT_BUILT')

if __name__ == '__main__':
    parser=argparse.ArgumentParser()
    parser.add_argument('--repo', type=pathlib.Path,default=pathlib.Path(__file__).resolve().parents[1])
    parser.add_argument('--apk', type=pathlib.Path)
    args=parser.parse_args()
    check(args.repo.resolve(), args.apk)
