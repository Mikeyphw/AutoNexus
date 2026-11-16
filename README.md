# AutoNexus

**One LSPosed module. Four independent Tasker plugin domains.**

AutoNexus is an Android/LSPosed bootstrap for AutoNotification, AutoInput, AutoTools and Termux:Tasker. It does not rebuild, patch, re-sign, replace, or bypass licensing for those apps.

This foundation intentionally contains **zero behavior-changing method hooks**, no root execution, no SystemUI or `system_server` scope, and no privileged permissions. The legacy LSPosed load-package entry point registers in the four application processes and remains fail-closed. No release/device compatibility is asserted until accepted on a physical device.

| Domain | Package | Reference APK version | Bootstrap |
|---|---|---|---|
| AutoNotification | `com.joaomgcd.autonotification` | 4.3.14 / 182 | scoped, inert |
| AutoInput | `com.joaomgcd.autoinput` | 3.0.12 / 104 | scoped, inert |
| AutoTools | `com.joaomgcd.autotools` | 2.3.19 / 197 | scoped, inert |
| Termux:Tasker | `com.termux.tasker` | 0.9.0 / 1002 | scoped, inert |

## What is implemented

- Single Android application ID `io.github.mikeyphw.autonexus` and legacy LSPosed registration (`xposed_init`, manifest scope).
- Module dashboard with four status cards and installed-package visibility checks. No mocked privilege status.
- Pure domain registry, versioned compatibility reference data, scope isolation, and fail-closed runtime gate.
- Explicit root-operation policy with **no executor or root escalation**; all root operations are rejected in bootstrap.
- JVM unit tests, static artifact integrity/security tests, and an Android GitHub Actions source gate.
- Final consolidated roadmaps stored under `docs/roadmaps/`, plus delivery and security contracts.

## What is NOT implemented

No HTML fixes, UI editor overlays, Tasker IPC fixes, JSON engine replacements, gesture retries, injection into SystemUI, root daemon, release signing, or real-device tests. These are planned features, not bootstrap acceptance claims.

## Build

Build with Android SDK 36, JDK 17 and Gradle 8.13. The AGP pin is 8.13.2 and Kotlin plugin pin is 2.3.21. Use an externally installed Gradle 8.13 (`gradle :app:assembleDebug :app:testDebugUnitTest :app:lintDebug`) or the native Devtool Android Gradle runner. The ZIP deliberately does **not** include an unverified Gradle wrapper JAR.

The Xposed legacy API (`de.robv.android.xposed:api:82`) is a **compileOnly** dependency; only the framework supplies it at runtime. The restricted Appodeal artifact repository in settings is used for that historical API coordinate; confirm the artifact source and checksum before production release. Dependency/artifact resolution was not tested in the offline authoring environment.

## Delivery and CI

The initial bootstrap was committed at `765371659edcc8439e395ccddaccb0956e68b358` on `feature/autonexus-bootstrap`, with open draft PR #1. The first hosted Android source gate failed on the legacy Kotlin `kotlinOptions { jvmTarget = "17" }` syntax; the consolidated CI-repair overlay migrates to the typed `compilerOptions` DSL, tests DEX class definitions (not only APK ZIP names), and adds the committed Devtool required-CI policy for the actual GitHub workflow ID.

All source changes are delivered exclusively with the native Devtool Launcher v3.1+ immutable package: source/branch/HEAD/tree/preimage verification, native overlay validation, atomic commit, durable session, approved push/PR and exact-head CI observation. This repository's `.devtool` private receipts are never deleted to satisfy a clean-source check; the repair package is built and applied in a separate clean, verified checkout of the same feature branch.

CI does not prove Android 16 physical LSPosed loading, device behavior or signed release readiness. See `docs/DEVTOOL_DELIVERY.md` and `docs/VALIDATION.md`.

## Default behavior and privacy

All feature behavior hooks and privileged operations default disabled. Hooks never log notification contents, accessibility tree text, Tasker variables, executable arguments or secrets. Existing apps work exactly as before. No implicit uploads or automatic clipboard use.
