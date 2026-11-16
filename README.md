# AutoNexus

**One LSPosed companion APK, four independently bounded Tasker-plugin domains.** AutoNexus does not rebuild, patch, re-sign, replace, or bypass licenses for the original applications.

## Current implementation

| Domain | Reference package/version | Implemented behavior |
|---|---|---|
| AutoNotification | `com.joaomgcd.autonotification` 4.3.14 / 182 | Package status and inert scope only; no notification changes |
| AutoInput | `com.joaomgcd.autoinput` 3.0.12 / 104 | Package status and inert scope only; no UI/gesture changes |
| AutoTools | `com.joaomgcd.autotools` 2.3.19 / 197 | Package status and inert scope only; no editor/JSON changes |
| Termux:Tasker | `com.termux.tasker` 0.9.0 / 1002, exact F-Droid signing certificate | **TT-F0:** read-only IPC-stage traces and companion readiness diagnostics. **TT-E1:** in-process, signer/version-gated enhanced editor covering all eight original fields |

The AutoNexus Material 3 companion shows independent private, versioned **staged requests**, installed-package comparisons and local-only diagnostics. Companion switches do not grant privileges or enable shell/IPC changes. LSPosed scope activation is manual and cannot be inferred from the companion UI.

**TT-E1 enhanced editor** appears inside the original Termux:Tasker configuration activity after it loads, not as a new Tasker plugin or replacement APK. It offers Command, Execution, Results and Diagnostics; edits are staged locally. **Validate & Save** delegates Locale/Tasker bundle creation to the original activity. **Discard**, **Keep Editing**, and **Original editor** fallback make the save intent explicit. Unknown action keys and unverified numeric ABI refuse enhanced Save. Back/Home cannot silently save while enhanced mode is active. Rotation restoration is transient and bounded. The original UI remains the fallback if hooks or compatibility checks fail.

**TT-F0 trace** records only fixed receiver/dispatch/result/callback stages, local outcomes and durations in the LSPosed manager's logs. Neither the editor nor the trace logs command paths, arguments, stdin/stdout, Tasker variables or secret data. Tracing does not establish Tasker host acknowledgment.

**Not implemented:** the known Tasker “plugin did not respond” reliability fix; Tasker host-side / Termux process trace; browser and exact argv studio; completed multi-screen original editor replacement; root helpers; SystemUI hooks; behavior changes for the other three domains; release signing or physical-device qualification of TT-E1. No unknown-state script is automatically retried. See [TT implementation](docs/TERMUX_TASKER_IMPLEMENTATION.md) and the [four full domain roadmaps](docs/ROADMAP_INDEX.md).

## Build and hosted checks

Current debug version: **0.4.0-tt-e1 (versionCode 4)**. Requirements: JDK 17, Android SDK 36 (`compileSdk` and `targetSdk` 36; `minSdk` 26), Gradle 8.13, Android Gradle Plugin 8.13.2, Kotlin/Compose compiler plugin 2.3.21, Compose BOM 2026.06.01 and activity-compose 1.13.0. The historical Xposed API `de.robv.android.xposed:api:82` is **compileOnly**, never shipped as framework implementation classes.

```sh
python3 tools/verify_bootstrap.py --repo .
python3 -m unittest discover -s tools -p 'test_*.py' -v
gradle :app:assembleDebug :app:testDebugUnitTest :app:lintDebug --no-daemon --stacktrace
python3 tools/verify_bootstrap.py --repo . --apk app/build/outputs/apk/debug/app-debug.apk
```

The GitHub Actions `AutoNexus Android source gate` checks out the **exact PR HEAD**, runs these mandatory checks, and uploads the debug APK plus SHA-256/source identity for **seven days** when green. CI's bounded Maven 429 retry never converts compile, test, lint or exhausted-network errors into successes. Hosted build, physical LSPosed loading, native Devtool CI qualification and release signing are distinct evidence levels; previous HEAD success does not prove a new overlay passed.

## Native delivery and security

All repository changes must use exact-pinned native **Mikeyphw/devtool Launcher v3.1+**: verify the intended named feature branch, origin, HEAD/tree, every modified preimage blob, launcher HEAD/tree/schema and payload; native validation, atomic commit, durable session, authorized push and PR/CI observation. Never replace native application with manual file copying, force-push, rebase, reset or discarded receipts. After `LAUNCHER_V3_COMMITTED`, resume the session rather than reapplying the package. See [Devtool delivery](docs/DEVTOOL_DELIVERY.md) and [validation](docs/VALIDATION.md).

No new exported components, root operations, permission grants, SystemUI/system_server scopes, Tasker bundle keys or automatic command replays are introduced. Runtime command/notification modification gates remain off for all domains. Termux:Tasker TT-E1 is a separately scoped **UI-only** enhancement, not an IPC or shell behavior change. See [security](docs/SECURITY.md).
