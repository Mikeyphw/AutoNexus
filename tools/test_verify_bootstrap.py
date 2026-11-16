"""Current bootstrap build/DEX security regression checks; no network or Android SDK."""
from __future__ import annotations

import json
from pathlib import Path
import struct
import sys
import tempfile
import unittest
from zipfile import ZipFile

sys.path.insert(0, str(Path(__file__).resolve().parent))
from verify_bootstrap import (check_apk, check_ci_policy,
                              check_kotlin_compiler_contract, defined_dex_classes)


ENTRY = 'io.github.mikeyphw.autonexus.hooks.AutoNexusEntry'
KOTLIN_SOURCE = '''import org.jetbrains.kotlin.gradle.dsl.JvmTarget
android { compileOptions {
  sourceCompatibility = JavaVersion.VERSION_17
  targetCompatibility = JavaVersion.VERSION_17
} }
kotlin { compilerOptions { jvmTarget.set(JvmTarget.JVM_17) } }
dependencies { compileOnly("de.robv.android.xposed:api:82") }
'''
POLICY = {
    'schema_version': 1, 'repository': 'Mikeyphw/AutoNexus',
    'base_branch': 'main', 'event': 'pull_request',
    'release_qualification': 'separate',
    'required_workflows': [
        {'name': 'AutoNexus Android source gate', 'workflow_id': 419982126}
    ],
}


def structural_dex_fixture(descriptor: str, *, definition: bool = True) -> bytes:
    """Small structural test fixture (not a signed, checksum-complete DEX)."""
    encoded = descriptor.encode('ascii')
    assert len(encoded) < 128
    strings_start, types_start = 0x70, 0x74
    classes_start = 0x78 if definition else 0
    string_start = 0x98 if definition else 0x78
    size = string_start + 1 + len(encoded) + 1
    buf = bytearray(size)
    buf[:8] = b'dex\n035\x00'
    struct.pack_into('<I', buf, 0x20, size)
    struct.pack_into('<II', buf, 0x38, 1, strings_start)
    struct.pack_into('<II', buf, 0x40, 1, types_start)
    struct.pack_into('<II', buf, 0x60, 1 if definition else 0, classes_start)
    struct.pack_into('<I', buf, strings_start, string_start)
    struct.pack_into('<I', buf, types_start, 0)  # type -> first descriptor
    if definition:
        struct.pack_into('<I', buf, classes_start, 0)  # class_def -> type 0
    buf[string_start] = len(encoded)  # ULEB128 UTF-16 length
    buf[string_start + 1 : string_start + 1 + len(encoded)] = encoded
    return bytes(buf)


class KotlinCompilerContractTests(unittest.TestCase):
    def test_typed_jvm17_accepted(self):
        check_kotlin_compiler_contract(KOTLIN_SOURCE)

    def test_reject_legacy_dsl(self):
        with self.assertRaisesRegex(AssertionError, 'Legacy'):
            check_kotlin_compiler_contract(KOTLIN_SOURCE + '\nkotlinOptions { jvmTarget = "17" }')

    def test_reject_jvm_target_mismatch(self):
        with self.assertRaisesRegex(AssertionError, 'JVM 17'):
            check_kotlin_compiler_contract(KOTLIN_SOURCE.replace('JvmTarget.JVM_17', 'JvmTarget.JVM_11'))

    def test_xposed_must_not_be_implementation_dependency(self):
        with self.assertRaisesRegex(AssertionError, 'runtime dependency'):
            check_kotlin_compiler_contract(KOTLIN_SOURCE + '\nimplementation("de.robv.android.xposed:api:82")')


class PolicyContractTests(unittest.TestCase):
    def test_actual_github_workflow_identity(self):
        check_ci_policy(POLICY)

    def test_reject_invented_workflow_id(self):
        changed = json.loads(json.dumps(POLICY))
        changed['required_workflows'][0]['workflow_id'] = 1
        with self.assertRaises(AssertionError):
            check_ci_policy(changed)

    def test_reject_cross_repo_policy(self):
        changed = {**POLICY, 'repository': 'other/not-autonexus'}
        with self.assertRaises(AssertionError):
            check_ci_policy(changed)


class DexDefinitionTests(unittest.TestCase):
    def test_compiled_entry_class_definition(self):
        self.assertEqual({f'L{ENTRY.replace(".", "/")};'},
                         defined_dex_classes(structural_dex_fixture(f'L{ENTRY.replace(".", "/")};')))

    def test_interface_reference_is_not_a_definition(self):
        # A compile-only reference inside type_ids alone is allowed.
        self.assertEqual(set(), defined_dex_classes(structural_dex_fixture(
            'Lde/robv/android/xposed/IXposedHookLoadPackage;', definition=False)))

    def test_reject_truncated_dex(self):
        with self.assertRaises(ValueError):
            defined_dex_classes(structural_dex_fixture('Ltest/Example;')[:-8])

    def test_reject_corrupt_class_index(self):
        buf = bytearray(structural_dex_fixture('Ltest/Example;'))
        struct.pack_into('<I', buf, 0x78, 4)
        with self.assertRaisesRegex(ValueError, 'absent type'):
            defined_dex_classes(bytes(buf))

    def test_inspect_actual_apk_members_and_class_definitions(self):
        with tempfile.TemporaryDirectory() as temp:
            name = Path(temp) / 'positive.apk'
            with ZipFile(name, 'w') as z:
                z.writestr('assets/xposed_init', ENTRY + '\n')
                z.writestr('classes.dex', structural_dex_fixture(f'L{ENTRY.replace(".", "/")};'))
            check_apk(name)

    def test_reject_shipped_xposed_api_in_secondary_dex(self):
        with tempfile.TemporaryDirectory() as temp:
            name = Path(temp) / 'negative.apk'
            with ZipFile(name, 'w') as z:
                z.writestr('assets/xposed_init', ENTRY + '\n')
                z.writestr('classes.dex', structural_dex_fixture(f'L{ENTRY.replace(".", "/")};'))
                z.writestr('classes2.dex', structural_dex_fixture('Lde/robv/android/xposed/XposedBridge;'))
            with self.assertRaisesRegex(AssertionError, 'Compile-only Xposed API class shipped'):
                check_apk(name)

    def test_reject_shipped_xposed_api_in_tenth_dex(self):
        with tempfile.TemporaryDirectory() as temp:
            name = Path(temp) / 'negative-multidex.apk'
            with ZipFile(name, 'w') as z:
                z.writestr('assets/xposed_init', ENTRY + '\n')
                z.writestr('classes.dex', structural_dex_fixture(f'L{ENTRY.replace(".", "/")};'))
                z.writestr('classes10.dex', structural_dex_fixture('Lde/robv/android/xposed/XposedBridge;'))
            with self.assertRaisesRegex(AssertionError, 'Compile-only Xposed API class shipped'):
                check_apk(name)

    def test_reject_apk_without_module_entry_class(self):
        with tempfile.TemporaryDirectory() as temp:
            name = Path(temp) / 'negative.apk'
            with ZipFile(name, 'w') as z:
                z.writestr('assets/xposed_init', ENTRY + '\n')
                z.writestr('classes.dex', structural_dex_fixture('Lunrelated/Class;'))
            with self.assertRaisesRegex(AssertionError, 'entry class is missing'):
                check_apk(name)


if __name__ == '__main__':
    unittest.main()
