# AutoNexus

**One LSPosed module. Four independent Tasker plugin domains.**

AutoNexus is an Android/LSPosed shared companion for AutoNotification, AutoInput, AutoTools and Termux:Tasker. It does not rebuild, patch, re-sign, replace, or bypass licensing for those apps.

The preflight stage intentionally contains **zero behavior-changing method hooks**, no root execution, no SystemUI or `system_server` scope, and no privileged permissions. The legacy LSPosed load-package entry point registers in the four application processes and remains fail-closed. No release/device compatibility is asserted until accepted on a physical device.

| Domain | Package | Reference APK version | Bootstrap |
|---|---|---|---|
| AutoNotification | `com.joaomgcd.autonotification` | 4.3.14 / 182 | scoped, inert |
| AutoInput | `com.joaomgcd.autoinput` | 3.0.12 / 104 | scoped, inert |
| AutoTools | `com.joaomgcd.autotools` | 2.3.19 / 197 | scoped, inert |
| Termux:Tasker | `com.termux.tasker` | 0.9.0 / 1002 | scoped, inert |

## What is implemented

- Material 3 Compose dashboard with four independently stored **staged requests**, local-only diagnostics switches and a read-only on-device acceptance checklist.
- Versioned, private, atomic per-domain preferences. Settings cannot be read from LSPosed target processes; no authenticated settings IPC or behavior hooks exist yet.
- Live package-manager version/reference comparison, with explicit "not installed or not visible" uncertainty; LSPosed activation remains unknown from the companion UI.
- Debug APK hosting workflow pinned to exact PR HEAD with SHA-256 and seven-day retention for physical acceptance. This is **not release signing**.
- Preflight version `0.3.0-tt-f0` / versionCode `3` (Termux:Tasker diagnostics).
- Single Android application ID `io.github.mikeyphw.autonexus` and legacy LSPosed registration (`xposed_init`, manifest scope).
- Material 3 dashboard with four status cards and installed-package visibility checks. No mocked privilege status.
- Pure domain registry, versioned compatibility reference data, scope isolation, and fail-closed runtime gate.
- Explicit root-operation policy with **no executor or root escalation**; all root operations are rejected in bootstrap.
- JVM unit tests, static artifact integrity/security tests, and an Android GitHub Actions source gate.
- Final consolidated roadmaps stored under `docs/roadmaps/`, plus delivery and security contracts.

## Termux:Tasker TT-F0 observability

Termux:Tasker **v0.9.0 / code 1002 / exact supplied F-Droid certificate** now has
version-gated, read-only LSPosed observations for receiver, service dispatch,
results service and Tasker callback API. A companion diagnostics tab shows
Tasker/Termux package, Tasker RUN_COMMAND grant and user-unlock state.
Observation does not establish host acceptance, and logs are available only
through the LSPosed manager (no cross-process log service). This feature is
active only when the user explicitly enables the Termux:Tasker LSPosed scope;
the companion `Request future enhancements` switch remains staged. See
`docs/TERMUX_TASKER_IMPLEMENTATION.md` for supported versions and physical tests.

## What is NOT implemented

No behavior-changing enhancement hooks, HTML fixes, UI editor overlays, Tasker IPC fixes, JSON engine replacements, gesture retries, injection into SystemUI, root daemon, release signing, or real-device tests. These are planned features, not bootstrap acceptance claims.

## Build

Build with Android SDK 36, JDK 17 and Gradle 8.13. The AGP pin is 8.13.2 and Kotlin/Compose compiler plugin pin is 2.3.21; Compose BOM is **2026.06.01** (core 1.11.4 and Material 3 1.4.0, compatible with the pinned SDK/AGP); activity-compose is 1.13.0. Use an externally installed Gradle 8.13 (`gradle :app:assembleDebug :app:testDebugUnitTest :app:lintDebug`) or the native Devtool Android Gradle runner. The ZIP deliberately does **not** include an unverified Gradle wrapper JAR.

The Xposed legacy API (`de.robv.android.xposed:api:82`) is a **compileOnly** dependency; only the framework supplies it at runtime. The restricted Appodeal artifact repository in settings is used for that historical API coordinate; confirm the artifact source and checksum before production release. Dependency/artifact resolution was not tested in the offline authoring environment.

## Delivery and CI

The initial bootstrap was committed at `765371659edcc8439e395ccddaccb0956e68b358` on `feature/autonexus-bootstrap`, with open draft PR #1. The first hosted Android source gate failed on the legacy Kotlin `kotlinOptions { jvmTarget = "17" }` syntax; the consolidated CI-repair overlay migrates to the typed `compilerOptions` DSL, tests DEX class definitions (not only APK ZIP names), and adds the committed Devtool required-CI policy for the actual GitHub workflow ID.

All source changes are delivered exclusively with the native Devtool Launcher v3.1+ immutable package: source/branch/HEAD/tree/preimage verification, native overlay validation, atomic commit, durable session, approved push/PR and exact-head CI observation. This repository's `.devtool` private receipts are never deleted to satisfy a clean-source check; the repair package is built and applied in a separate clean, verified checkout of the same feature branch.

CI does not prove Android 16 physical LSPosed loading, device behavior or signed release readiness. See `docs/DEVTOOL_DELIVERY.md` and `docs/VALIDATION.md`.

## Default behavior and privacy

All feature behavior hooks and privileged operations default disabled. Hooks never log notification contents, accessibility tree text, Tasker variables, executable arguments or secrets. Existing apps work exactly as before. No implicit uploads or automatic clipboard use.

## Preflight device acceptance (proposed follow-up)

The consolidated preflight overlay is based on source HEAD `779a76ee1126de52fc09adb759997b03f48c29d6`, with a **hosted CI success for the prior CI repair only**. Do not claim the new Material 3/Compose UI compiles or runs until its own exact-head checks pass. After native delivery, obtain the short-lived `autonexus-preflight-SHA` debug APK artifact from the PR Actions run, check `app-debug.apk.sha256` and `source-commit.txt`, then explicitly install/test it. LSPosed target scopes remain manual, exactly four plugin packages. Do not add system packages. See `docs/VALIDATION.md` for the acceptance checklist.

## CI dependency rate limits

The hosted Material 3 preflight run on the feature branch encountered Maven
Central HTTP 429 while resolving Gradle classpath artifacts, before Android
compilation. The CI-only wrapper retries the *same* Android build, JVM tests,
and lint tasks at most three times **only** for confirmed dependency-resolution
429 responses. Other failures remain failures; the wrapper cannot guarantee
recovery from persistent upstream throttling. See `docs/VALIDATION.md` for
failure evidence, tests, and acceptance boundaries.

## CompileSdk 36 / Compose compatibility repair

The source-bound hosted run on `dce8ff47fa73a9034f76317d2b895902c97b5e96`
failed in Gradle's **mandatory AAR metadata gate**: BOM 2026.09.00 resolved
Compose 1.12.1 requiring compileSdk 37 and AGP 9.1+. The Maven 429 retry
wrapper correctly treated that as a non-retryable source/dependency error.
The next consolidated change selects stable BOM 2026.06.01 (Compose core
1.11.4, Material3 1.4.0) and updates the existing build contract tests. It
retains compileSdk/targetSdk 36, AGP 8.13.2, and all no-hook/root safeguards.
The new hosted Android build, APK and device acceptance are still pending.

## Kotlin Compose scope correction (source-only until hosted CI)

The SDK36 Compose dependency correction allowed the hosted preflight build to
reach Kotlin compilation, which exposed an invalid top-level `weight` import
in `MainActivity.kt`. The consolidated follow-up removes that import while
preserving the two Row-scoped `Modifier.weight(1f)` calls and adds regression
tests. It does not change app behavior, root/LSPosed scope, device permissions,
or dependencies. A new exact-HEAD Android build is required before accepting
the UI as compiled; device acceptance is still outstanding.
