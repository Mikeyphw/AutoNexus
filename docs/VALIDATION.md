# Validation contract

- **Static local**: `python3 tools/verify_bootstrap.py --repo .` (pure XML, source, negative-scope assertions, no Android toolchain).
- **JVM**: `gradle :app:testDebugUnitTest` tests exact four-package scope, default-off runtime gate and root-deny policy.
- **Android**: `gradle :app:assembleDebug :app:lintDebug` using JDK17, Gradle8.13, AGP8.13.2, SDK36.
- **Built APK**: `python3 tools/verify_bootstrap.py --repo . --apk app/build/outputs/apk/debug/app-debug.apk` must see xposed_init and reject any bundled Xposed classes.
- **Native Devtool**: Launcher v3.1+ `check` must call native delivery adoption scaffold and validate overlay; `apply` native atomic, source-pinned commit/session.
- **Hosted**: workflow must run on exact PR HEAD and be qualified with a genuine committed required-CI policy using real GitHub workflow IDs.
- **Device**: enable four application scopes on actual Android16 device, verify app launches and class loading is inert, no target app regressions, no SystemUI / root influence; root denial test.
- **Release**: signing and device verification separately required. None of these later stages were run during source packaging.

No check may be deleted to conceal failures. Obsolete gate cleanup: fresh project contains no historical HFxx/Pxx/Rxx files, legacy CI aliases or carryover test chains; one current smoke/security suite.
