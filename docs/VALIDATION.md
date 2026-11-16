# AutoNexus bootstrap validation and evidence

The current module is intentionally inert. These gates preserve that design while qualifying its build configuration. No skipped test is recorded as a pass.

## Local source gates

1. `python3 tools/verify_bootstrap.py --repo .`: verifies manifest, exact four packages, Xposed registration, fail-closed hook/root policy, typed Kotlin/JVM 17 configuration and committed-required-CI policy *contents*.
2. `python3 -m unittest discover -s tools -p "test_*.py" -v`: negative and positive tests for legacy Kotlin DSL rejection, policy identity, DEX class-definition parsing and malformed DEX refusal. This is distinct from compiling an APK.
3. `gradle :app:assembleDebug :app:testDebugUnitTest :app:lintDebug --no-daemon --stacktrace`: native Android compilation, JVM tests and lint. Requires Gradle 8.13, JDK 17 and Android SDK 36.
4. `python3 tools/verify_bootstrap.py --repo . --apk app/build/outputs/apk/debug/app-debug.apk`: validates actual embedded DEX class definitions (including multidex), rejects bundled `de.robv.android.xposed` implementation classes and requires the module entry class, rather than trusting ZIP entry names.

## Hosted and native delivery

- `.github/workflows/android-ci.yml` runs all four gates on `pull_request` and on pushes to `main`. GitHub Actions tooling is pinned to verified full commit SHAs.
- `config/delivery-required-ci.json` registers the actual `AutoNexus Android source gate` workflow ID `419982126` (GitHub API observed 2026-11-16), base `main`, event `pull_request`, release separate. Policy is *introduced in this overlay*. The v3.1 launcher checks required-CI policy **before** applying; therefore the initial apply cannot use this newly added policy to qualify the current session. After the new commit, start a **new native source-bound session** for the new HEAD and observe CI with Devtool's `delivery session ci`. Do not reapply the overlay.
- `devtool.launcher_contract_v3` must build an immutable source-bound package on a clean named **feature** checkout, perform native scaffold and overlay validation, then native atomic apply/commit, durable session, explicitly approved push and PR reconciliation. Never patch repository files outside Devtool, replay an applied overlay, reset/rebase/force push or delete native evidence.
- CI success remains distinct from Android 16 physical LSPosed loading and signing/release acceptance. Release qualification is not performed by this gate.

## Obsolete gates / assurance

There are no old HFxx/Pxx/Rxx workflows to delete in this new repository. The existing bootstrap static/negative suite and JVM tests remain in place; the Kotlin DSL and actual DEX checks are additive modern coverage, not weakened tests.
