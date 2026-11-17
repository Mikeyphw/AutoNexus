#!/usr/bin/env python3
"""Read-only current-source bootstrap contract tests (no git, network or device)."""
import argparse, json, pathlib, re, struct, sys, zipfile, xml.etree.ElementTree as ET

PACKAGES = (
    'com.joaomgcd.autonotification',
    'com.joaomgcd.autoinput',
    'com.joaomgcd.autotools',
    'com.termux.tasker',
)
ANDROID = '{http://schemas.android.com/apk/res/android}'


# DEX class_def inspection is necessary because ZIP member-name checks cannot
# distinguish compile-only Xposed API references from accidentally shipped
# class implementations. Parse only bounded, checked DEX structures; never
# interpret references in the string/type pools as declarations.
def _u32(blob: bytes, offset: int) -> int:
    if offset < 0 or offset + 4 > len(blob):
        raise ValueError('DEX u32 out of range')
    return struct.unpack_from('<I', blob, offset)[0]


def _section(blob: bytes, size_offset: int, location_offset: int, item_size: int) -> tuple[int, int]:
    count = _u32(blob, size_offset)
    start = _u32(blob, location_offset)
    if count > len(blob) // item_size or (count and (start < 0x70 or start > len(blob) - count * item_size)):
        raise ValueError('DEX section offsets or sizes invalid')
    return count, start


def _string(blob: bytes, offset: int) -> str:
    if offset < 0x70 or offset >= len(blob):
        raise ValueError('DEX string offset invalid')
    cursor = offset
    for _ in range(5):
        if cursor >= len(blob):
            raise ValueError('Truncated DEX ULEB128')
        byte = blob[cursor]
        cursor += 1
        if byte < 128:
            break
    else:
        raise ValueError('DEX ULEB128 length invalid')
    try:
        end = blob.index(0, cursor)
    except ValueError as exc:
        raise ValueError('Unterminated DEX string') from exc
    # Class descriptors are ASCII, even though DEX permits MUTF-8 string data.
    return blob[cursor:end].decode('ascii', errors='strict')


def defined_dex_classes(blob: bytes) -> set[str]:
    if len(blob) < 0x70 or not blob.startswith(b'dex\n') or blob[7] != 0:
        raise ValueError('Not a regular DEX file')
    if _u32(blob, 0x20) != len(blob):
        raise ValueError('DEX declared file size mismatch')
    strings, string_table = _section(blob, 0x38, 0x3c, 4)
    types, type_table = _section(blob, 0x40, 0x44, 4)
    classes, class_table = _section(blob, 0x60, 0x64, 32)
    result: set[str] = set()
    for i in range(classes):
        class_type_idx = _u32(blob, class_table + i * 32)
        if class_type_idx >= types:
            raise ValueError('DEX class refers to absent type')
        descriptor_idx = _u32(blob, type_table + class_type_idx * 4)
        if descriptor_idx >= strings:
            raise ValueError('DEX type refers to absent string')
        value = _string(blob, _u32(blob, string_table + descriptor_idx * 4))
        if not (value.startswith('L') and value.endswith(';')):
            raise ValueError('Invalid DEX class descriptor')
        if value in result:
            raise ValueError('Duplicate DEX class definition')
        result.add(value)
    return result


def check_kotlin_compiler_contract(script: str) -> None:
    # This form was rejected by Kotlin 2.3.21 on the actual PR #1 hosted run.
    if re.search(r'\bkotlinOptions\s*\{', script):
        raise AssertionError('Legacy kotlinOptions DSL must not return')
    if not re.search(r'\bkotlin\s*\{\s*compilerOptions\s*\{\s*'
                     r'jvmTarget\.set\(JvmTarget\.JVM_17\)\s*\}\s*\}', script):
        raise AssertionError('Typed Kotlin JVM 17 compilerOptions missing')
    if 'import org.jetbrains.kotlin.gradle.dsl.JvmTarget' not in script:
        raise AssertionError('Typed Kotlin JvmTarget import missing')
    if ('sourceCompatibility = JavaVersion.VERSION_17' not in script or
            'targetCompatibility = JavaVersion.VERSION_17' not in script):
        raise AssertionError('Java 17 and Kotlin JVM 17 must match')
    if 'compileOnly("de.robv.android.xposed:api:82")' not in script:
        raise AssertionError('Xposed API must remain compileOnly')
    if 'implementation("de.robv.android.xposed:api:82")' in script:
        raise AssertionError('Xposed API must not become a runtime dependency')


def check_ci_policy(policy: dict) -> None:
    required = {
        'schema_version': 1, 'repository': 'Mikeyphw/AutoNexus',
        'base_branch': 'main', 'event': 'pull_request',
        'release_qualification': 'separate',
        'required_workflows': [
            {'name': 'AutoNexus Android source gate', 'workflow_id': 419982126}
        ],
    }
    if policy != required:
        raise AssertionError('CI policy does not match verified repository/workflow identity')


def check_apk(apk: pathlib.Path) -> None:
    entry = 'Lio/github/mikeyphw/autonexus/hooks/AutoNexusEntry;'
    with zipfile.ZipFile(apk) as archive:
        members = set(archive.namelist())
        if 'assets/xposed_init' not in members:
            raise AssertionError('Xposed entry asset absent from APK')
        if archive.read('assets/xposed_init').decode('utf-8').strip() != entry[1:-1].replace('/', '.'):
            raise AssertionError('Xposed entry asset differs from scoped implementation')
        dex_members = sorted(x for x in members if re.fullmatch(r'classes(?:[0-9]+)?\.dex', x))
        if 'classes.dex' not in dex_members:
            raise AssertionError('No primary DEX in built APK')
        defined: set[str] = set()
        for member in dex_members:
            info = archive.getinfo(member)
            if info.file_size > 64 * 1024 * 1024:
                raise AssertionError('Unexpectedly large DEX')
            classes = defined_dex_classes(archive.read(member))
            if defined.intersection(classes):
                raise AssertionError('Duplicate class definitions across DEX files')
            defined.update(classes)
        if entry not in defined:
            raise AssertionError('Compiled LSPosed entry class is missing')
        shipped = sorted(value for value in defined if value.startswith('Lde/robv/android/xposed/'))
        if shipped:
            raise AssertionError('Compile-only Xposed API class shipped: ' + shipped[0])


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
    check_kotlin_compiler_contract(b)
    check_ci_policy(json.loads((root/'config/delivery-required-ci.json').read_text()))
    workflow = (root/'.github/workflows/android-ci.yml').read_text()
    assert 'name: AutoNexus Android source gate' in workflow
    assert 'python3 -m unittest discover -s tools' in workflow
    assert ':app:assembleDebug :app:testDebugUnitTest :app:lintDebug' in workflow
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
        check_apk(apk)
    print('AUTONEXUS_BOOTSTRAP_STATIC_NEGATIVE_PASS', 'APK_INSPECTED' if apk else 'APK_NOT_BUILT')

if __name__ == '__main__':
    parser=argparse.ArgumentParser()
    parser.add_argument('--repo', type=pathlib.Path,default=pathlib.Path(__file__).resolve().parents[1])
    parser.add_argument('--apk', type=pathlib.Path)
    args=parser.parse_args()
    check(args.repo.resolve(), args.apk)
