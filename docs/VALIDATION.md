# AutoNexus preflight validation and evidence

No skipped check counts as a pass. All validation stages keep the module behavior-inert and root-disabled.

## Source and JVM

1. `python3 tools/verify_bootstrap.py --repo .`: existing scope, Xposed, fail-closed runtime/root, Kotlin DSL and CI identity checks.
2. `python3 -m unittest discover -s tools -p 'test_*.py' -v`: the existing 15 bootstrap security, DEX and CI tests plus 10 dashboard/security/checkout negatives.
3. `gradle :app:assembleDebug :app:testDebugUnitTest :app:lintDebug --no-daemon --stacktrace`: hosted/local Android, Compose compilation, JVM unit and lint gates, including `DomainSettingsTest` and `DomainSettingsStoreTest` with a deterministic, in-memory SharedPreferences double.
4. `python3 tools/verify_bootstrap.py --repo . --apk app/build/outputs/apk/debug/app-debug.apk`: actual DEX class-definition safety (Xposed API not bundled; the entry class present).

## Exact PR HEAD and downloadable test APK

CI checks out `${{ github.event.pull_request.head.sha || github.sha }}` directly, disables checkout credential persistence, and independently asserts `git rev-parse HEAD`. A signed-for-debug (NOT release-signed) `app-debug.apk`, `app-debug.apk.sha256` and `source-commit.txt` are uploaded to an Actions artifact labeled by the exact source HEAD, retained for 7 days. Artifact identity and integrity are only meaningful when the on-device APK hash matches the accompanying SHA file and the PR HEAD is independently verified. GitHub's older PR-merge checkout behavior is not accepted as exact-source evidence.

The existing `config/delivery-required-ci.json` pins repository `Mikeyphw/AutoNexus`, base `main`, PR event and workflow `AutoNexus Android source gate` ID `419982126`. Devtool must use the latest exact v3.1+ runtime, validate/commit through its native overlay engine, create a durable delivery session, and observe the actual PR HEAD. Hosted green does not imply native required-CI qualification if branch/ruleset/registrar/evidence requirements cannot be proven.

## Physical Android / LSPosed acceptance (NOT performed by CI)

1. Verify artifact SHA-256 after downloading it, ensure `source-commit.txt` equals the PR HEAD and install the debug APK as an update only after explicit operator approval. Keep original plugins untouched.
2. Launch AutoNexus on Android 16. Confirm Material 3 dashboard renders, app/package versions distinguish reference matches from mismatches, dark/light and rotation/large-font layouts remain usable.
3. Toggle one domain's staged request and local diagnostics; relaunch the app. Confirm that only the selected domain's values persist and **no behavior hook becomes active**. Repeat a failed-write or corrupt-schema scenario in a controlled test, not by modifying live preferences.
4. In the LSPosed manager, enable only the four explicitly named plugin packages. Relaunch each installed plugin; check its LSPosed log for the original scoped inert marker. Absence of a marker is NOT proof the plugin is absent; inspect scope and logs. No SystemUI/system_server scopes.
5. Run non-destructive smoke interactions for the installed original plugins; observe crashes and permission behavior. **Do not replay unknown-state Termux:Tasker scripts** to obtain an artificial pass.
6. Report device API/ROM, LSPosed implementation/version, installed plugin versions, screenshot evidence, crash or hook logs with sensitive content redacted, and pass/fail per step. Device qualification and release signing require separate review.

## Cleanup and security

Existing legacy bootstrap tests are retained, not bypassed. New checks cover private data, staged-only toggles, exact CI checkout, and bounded artifact retention. There are no obsolete historical HFxx/Pxx/Rxx test chains in this repository. Native RUN-V3 and LIFE receipts must remain untouched. Do not rerun an immutable launcher after it already committed; resume the durable session instead.

## Hosted dependency-rate-limit recovery (CI-only)

On the exact PR HEAD `6fe71361ce711501346416fa12ddb9c56b1a6d5e`, Actions run
`43933740068` failed during the Gradle root project's plugin classpath resolution.
Maven Central returned **HTTP 429 Too Many Requests** across transitive plugin
artifacts. Source validation and all 25 Python regression checks passed; this
run **did not** compile Android, run JVM tests or lint, or produce the debug APK.

`tools/ci_gradle_rate_limit.py` runs the **same mandatory Gradle tasks** and
forwards each attempt's real output. The only retry classification is a genuine
Gradle dependency-resolution failure with a Maven GET HTTP 429. It runs at most
three attempts with backoff (45s and 120s). Compilation, JVM-test, lint,
missing artifact, HTTP 403/404, and generic failures are **never** treated as
rate-limit successes. Persistent 429 failures remain red. Existing source,
DEX, runtime-safety, APK and artifact checks are retained; tests for retry
logic are included in `tools/test_ci_gradle_rate_limit.py` and run in the
existing Python unittest gate.

Rerunning the failed workflow was attempted through a GitHub integration and
refused with HTTP 403 (Actions write permission missing). No hosted rerun or
qualification should be claimed from this source-only hardening. The retry
wrapper does not bypass Devtool's native required-CI policy, change device
behavior, or promote a debug APK to a signed release.

## Compose AAR metadata incompatibility on latest source (2026-11-16)

Exact PR HEAD `dce8ff47fa73a9034f76317d2b895902c97b5e96` produced Actions run
`43937398061`. The CI wrapper correctly **did not retry**: `:app:checkDebugAarMetadata`
failed with 18 incompatibilities because Compose core 1.12.1 (from BOM 2026.09.00)
requires `compileSdk >= 37` and AGP 9.1+, while this Android 16 project is
using SDK 36 and AGP 8.13.2. This is a dependency/API compatibility failure,
not a Maven HTTP 429. Python security/regression checks passed; Android JVM,
lint and APK checks did not finish and must not be described as passed.

The next source-pinned compatibility repair selects stable Compose BOM
`2026.06.01` (core 1.11.4, Material3 1.4.0) without changing Android SDK,
Gradle, Kotlin, Xposed scope, default-off gate, or required-CI policy. The
existing `tools/test_dashboard_contract.py` test is updated to reject later
BOM versions and unreviewed SDK changes; all retained tests still run. The
actual AAR metadata task stays enabled, including negative and packaging checks.

References: https://developer.android.com/develop/ui/compose/setup-compose-dependencies-and-compiler
and the published Compose BOM `2026.06.01` POM/version mapping at
https://mvnrepository.com/artifact/androidx.compose/compose-bom/2026.06.01/dependencies.
This repair is not accepted until the exact new PR HEAD finishes Android
compile, JVM tests, lint and DEX/APK verification. Existing native Devtool
session `85c6107867a4408e8043f27af8392c63` is already committed; never
reapply that old package. A **new** native source-bound overlay/session is
required for this compatibility change.
