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
