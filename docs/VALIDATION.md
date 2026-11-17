# AutoNexus preflight validation and evidence

No skipped check counts as a pass. Runtime command/notification changes and root remain disabled. TT-F0 observational hooks and the TT-E1 configuration UI are explicitly scoped, signer-verified enhancements and must not be described as no hooks at all.

## Source and JVM

1. `python3 tools/verify_bootstrap.py --repo .`: existing scope, Xposed, fail-closed runtime/root, Kotlin DSL and CI identity checks.
2. `python3 -m unittest discover -s tools -p 'test_*.py' -v`: the existing bootstrap, DEX, CI, dashboard, Kotlin-DSL, Compose SDK-compatibility and Maven-429 negative tests, plus Compose weight-scope import regressions.
3. `gradle :app:assembleDebug :app:testDebugUnitTest :app:lintDebug --no-daemon --stacktrace`: hosted/local Android, Compose compilation, JVM unit and lint gates, including `DomainSettingsTest` and `DomainSettingsStoreTest` with a deterministic, in-memory SharedPreferences double.
4. `python3 tools/verify_bootstrap.py --repo . --apk app/build/outputs/apk/debug/app-debug.apk`: actual DEX class-definition safety (Xposed API not bundled; the entry class present).

## Exact PR HEAD and downloadable test APK

CI checks out `${{ github.event.pull_request.head.sha || github.sha }}` directly, disables checkout credential persistence, and independently asserts `git rev-parse HEAD`. A signed-for-debug (NOT release-signed) `app-debug.apk`, `app-debug.apk.sha256` and `source-commit.txt` are uploaded to an Actions artifact labeled by the exact source HEAD, retained for 7 days. Artifact identity and integrity are only meaningful when the on-device APK hash matches the accompanying SHA file and the PR HEAD is independently verified. GitHub's older PR-merge checkout behavior is not accepted as exact-source evidence.

The existing `config/delivery-required-ci.json` pins repository `Mikeyphw/AutoNexus`, base `main`, PR event and workflow `AutoNexus Android source gate` ID `419982126`. Devtool must dynamically resolve and attest the latest compatible v3.1+ current-main runtime (never pin a historical Devtool SHA), validate/commit through its native overlay engine, create a durable delivery session, and observe the actual PR HEAD. Hosted green does not imply native required-CI qualification if branch/ruleset/registrar/evidence requirements cannot be proven.

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

## Compose Kotlin compile correction (preflight v0.2.0)

On 2026-11-16, the source-bound GitHub Actions run
[`43940397810`](https://github.com/Mikeyphw/AutoNexus/actions/runs/43940397810)
for commit `e87daca16858cdf3df27c873d889432f8f51b69f`
**passed** AAR metadata, reached `:app:compileDebugKotlin`, and failed because
`MainActivity.kt:18` imported `androidx.compose.foundation.layout.weight`
as if it were a top-level public function. The resolved Compose API supplies
`Modifier.weight()` as a member extension of `RowScope` or `ColumnScope`;
`Column(Modifier.weight(1f))` within an existing `Row` lambda resolves without
that import. The repair removes **only** the invalid import and adds positive
and negative regression checks.

The previous Android SDK 36 / AGP 8.13.2 / Compose BOM 2026.06.01 pins and
all existing security checks stay in effect. This correction has source-level
local tests only until a new exact-HEAD Android compile, JVM tests, lint,
APK DEX check and artifact upload complete. Physical LSPosed/device and
production signing remain separately unverified.

## Termux:Tasker TT-F0 (read-only, not an IPC fix)

- The exact user APK SHA-256 and certificate are pinned in
  `TraceProtocol.kt` / `TrustedTarget.kt`. Unknown installed identities are
  not hooked. Read-only observations are installed in the existing LSPosed
  package scope after `Application.attach`, without any extra permissions.
- `tools/test_termux_tasker_contract.py` checks method signatures derived
  from upstream v0.9.0, exact identity gating, non-mutating/no-payload hooks,
  root/scope invariants and privacy. `TraceProtocolTest.kt` exercises signer
  mismatch, ordered result classification, exception categorization and safe
  bounded logs. Existing gates remain active and source-bound.
- The hosted Gradle check still must run assemble, JVM tests, lint and DEX
  inspection. A green run qualifies build only; device traces, installed
  signer, Tasker IPC acceptance and editor integration require physical tests.
- On-device operating instructions and exclusions are in
  `docs/TERMUX_TASKER_IMPLEMENTATION.md`. No old historical test chain was
  deleted; existing security gates are preserved.

## TT-E1 enhanced in-plugin editor

- `tools/test_termux_tasker_editor.py` tests current hooked Activity identity, eight fields, explicit Save/Cancel, transient restoration, unknown keys, fail-closed ranges and privacy. All existing checks are retained.
- `EditorContractTest` is part of hosted `:app:testDebugUnitTest`, checking exact literal arguments/stdin, empty executable, numeric bounds, Tasker `%variables`, size caps and unknown fields.
- Hosted Android build and `:app:lintDebug` must compile the Android/Xposed editor bridge; local stub compilation, if performed, is not hosted qualification.
- Physical acceptance: install the exact-HEAD debug APK, scope LSPosed only to `com.termux.tasker`, verify pinned F-Droid 0.9.0 signature; open an existing Tasker action and compare all eight saved fields. Verify no-op Save, Discard, Original fallback, Back/Home, rotation, large stdin, null/default values, incompatible signature and malformed values. Check original Tasker result bundle, not only dialog success.
- Run only one harmless action on explicit user initiation. TT-F0 trace stages must identify the actual IPC failure before any behavioral retry/repair. No release signing or hardware acceptance claim from CI alone.

## TT-E2 editor studio validation (proposed, not hosted yet)

- Python `test_termux_tasker_e2.py` covers exact original tokenizer binding, opt-in preview, bounded canonical script browsing, safe read-only behavior, preserved raw fields, and no new execution or root paths.
- JVM `ArgumentStudioTest` and `ScriptExplorerTest` cover token preview refusal, unchanged raw arguments, unresolved Tasker variables, symlink traversal, non-allowlisted directories, bounded enumeration and deep directory refusal.
- Hosted build must still compile against Android SDK 36, AGP 8.13.2, Kotlin 2.3.21, and compile-only legacy Xposed API; all existing source, JVM, lint and APK checks remain mandatory.
- Device acceptance: check the optional browser under the plugin UID; if inaccessible, verify fallback, not a guessed success. Save/Discard/Original plus activity recreation, mode toggles and `%variables` require physical confirmation. No IPC reliability or release claim is made.

TT-E2 revised acceptance: test editor transition `EditorDraftBridgeTest` (discard/current Original/re-entry), test `ScriptExplorerTest` (bounded scanning, internal/escaped symlinks, depth, limits), run Python contracts and security gates. Wrapper source/package SHA verification is independently tested with altered bytes. Native Devtool `check`, hosted Android compile/test/lint, real LSPosed/Tasker and release remain separate qualifications; do not mark unperformed tests passed.

## TT-E2 hosted Kotlin unit-test compilation repair (2026-11-17)

The exact feature-branch PR HEAD `f3fd62d00966db0538b1b4c9273556436f444f5e`
failed GitHub Actions run `43999132101` at `:app:compileDebugUnitTestKotlin`.
`ScriptExplorerTest.kt` used five `Files.writeString(...)` calls, which are
missing from the Android SDK 36 Java API stubs despite local JDK availability.
The fix uses `Files.write(Path, ByteArray)` and explicit UTF-8 byte arrays or
zero-length arrays; test assertions, production `ScriptExplorer.kt`, scope,
permissions and Gradle version remain unchanged. `tools/test_termux_tasker_e2.py`
now rejects reinstating the unsupported method. No failing or obsolete checks
were removed or skipped. The repaired source requires a **new exact-HEAD**
Gradle assemble/JVM/lint/DEX workflow, uploaded APK evidence and native Devtool
CI qualification. A previous green test on a host JDK is not an Android build pass.
The original TT-E2 overlay has already committed; use its existing durable
Devtool session for its status, and the new source-bound correction package only
once. Never attempt to reapply TT-E2 itself or rewrite the existing commit.
