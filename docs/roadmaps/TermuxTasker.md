# Termux:Tasker — Final Consolidated Roadmap (One Shared LSPosed Module)

**Target:** User-supplied `Termux꞉Tasker_0.9.0.apk` (`com.termux.tasker`, version 0.9.0 / versionCode 1002) on Android 16, within the **single shared LSPosed module** supporting AutoNotification, AutoInput, AutoTools and Termux:Tasker.  
**Permitted:** Runtime LSPosed hooks, injected configuration screens, module-owned settings and diagnostics, a restricted **optional root helper** using the device's existing root provider, and optional *Tasker-side* / *Termux-side* hooks subject to installed-version verification.  
**Excluded:** Any SystemUI hooks for this domain; rebuilding, modifying, re-signing, replacing or changing the target APKs; adding a second LSPosed or Magisk module; bypassing the Termux permission model, removing Tasker authorization, or blindly replaying commands.  
**Status:** Roadmap and design contract only. No running-device hook, root operation, implementation, Devtool overlay, hosted CI or physical acceptance is represented as completed.

---

## 0. Provenance, evidence definitions and non-negotiable constraints

| Evidence | Verified information | Limitation |
|---|---|---|
| Uploaded APK | SHA-256 `f8cd889f9987759bcaeb68b8a8ba337085b5938b97a5cd9f05a4e610d411ef5e`; 2 DEX files; APK zip inventory verified | Uploaded file may differ from installed package/runtime |
| v0.9.0 upstream source | `EditConfigurationActivity.java` blob `e0fbdd8116d4dff31e3b73e29d229c799c1aadb7` | Upstream tag may differ from a modified or repackaged installed artifact |
| v0.9.0 upstream source | `FireReceiver.java` blob `dd0e3c5505bbf619dc7c1d94e4301c3b77aaaed0` | Hook success depends on installed class loader and runtime |
| v0.9.0 upstream source | `PluginUtils.java` blob `03860798cd294f455491ffc07445f72febfe1c35` | Candidate method hooks require ABI/signature verification |
| v0.9.0 upstream source | `PluginResultsService.java` blob `087261c8861a7fefbe955a82abed037f233defb2` | Does not prove results arrive during the observed failure |
| v0.9.0 upstream source | `PluginBundleManager.java` blob `6be4103d61d162620d82f2200e6797c123a82679` | Original bundle format must remain untouched |
| v0.9.0 upstream source | `activity_edit_configuration.xml` blob `22bff83e456969ce9c28f8036d30e6cd1e17f368` | Source XML is not an observed screenshot of the user's device |
| v0.9.0 upstream source | `AndroidManifest.xml` blob `c5c7255001da9f8c53f2261643e8858b75b80ef2` | Actual package/permission/install state must be read from device |
| Earlier user-directed audit | `Termux_Tasker_0.9.0_Config_UIUX_LSPosed_Audit.md` | UX improvements are proposed, not proven effective |
| Observed symptom | User reports that *Tasker says the plugin itself didn't respond* while executing an **already-configured** Termux:Tasker action, worse after reboot; direct interaction with the plugin works | No Tasker log, stack trace or runtime trace yet confirms where communication breaks |

**Evidence notation:** **V** verified code/APK fact; **P** planned implementation; **C** conditional on device traces or compatibility. Every test report must identify **local**, **native Devtool**, **hosted CI**, **physical device**, and **release** evidence separately.

**Core contract:** One installed module package/LSPosed registration, one shared dashboard, one shared restricted-root boundary, one shared versioned logging and settings system, independent domain switches, and per-domain failure containment. Termux:Tasker is a domain, **not a second module**. It must continue working when AutoNotification, AutoInput and AutoTools are disabled, and those domains must work when Termux:Tasker is disabled.

## 1. Verified current architecture and actual failure scope

### 1.1 Existing Tasker execution handshake (**V**)

```text
Tasker existing configured action
    │ plugin host prepares Locale/Tasker action intent
    ▼
Android ordered broadcast delivery  (can fail before plugin Java executes)
    ▼
com.termux.tasker:background
    FireReceiver.onReceive(Context, Intent)
    ├─ action/bundle parse and validation
    ├─ permission / Termux availability / executable preflight
    └─ PluginUtils.sendExecuteIntentToExecuteService(...)
         ├─ if waiting: ordered-broadcast RESULT_CODE_PENDING
         │             + PendingIntent to PluginResultsService
         ├─ if not waiting: immediate ordered-broadcast result
         └─ startForegroundService/startService for Termux execution
                            │
                            ▼
              com.termux TermuxService
                            │ command execution and result
                            ▼
              PluginResultsService.onHandleIntent(...)
                            │
                            ▼
              PluginUtils.sendPendingResultToPluginHostApp(...)
                            │
                            ▼
              TaskerPlugin.Setting.signalFinish(...)
                            │ service start OR broadcast
                            ▼
              Tasker completion handler
```

`FireReceiver` explicitly runs in `:background` from the manifest. Successful launch of `EditConfigurationActivity` only demonstrates the editor path, **not** successful delivery to this receiver. Initial ordered-broadcast acknowledgement and final completion are distinct; app-side hooks cannot see a broadcast that never enters the app process.

### 1.2 Real code-supported hazards to investigate (**V facts, C actual impact**)

1. **Receiver not reached:** Tasker dispatch, Android broadcast/package state, delivery restriction, process crash or cold-start are separate from Termux shell execution. Plugin-only hooks cannot prove non-delivery; root read-only diagnostics and optional Tasker-side tracing are required.
2. **Initialization before action processing:** `FireReceiver.onReceive()` configures crash handling/logging and handles bundles/Termux availability before the service dispatch. Exceptions or process death in these areas could prevent response. Check precise exception and process history.
3. **Pending-result setup:** `sendExecuteIntentToExecuteService()` uses `PendingIntent.getService(...)` and `setResultCode(PENDING)` in the waiting path. Creation of the PendingIntent happens outside its later service-start `try/catch`. A runtime error here warrants a *recorded failure and safe error response* when legally possible, not pretending the command ran.
4. **Service dispatch:** It uses `startForegroundService()` on Android O+; Android can reject service starts. It catches exceptions around the service start, but the receiver path must be measured as a whole, including when an immediate OK has already been signaled.
5. **Final completion delivery:** `TaskerPlugin.Setting.signalFinish()` reconstructs a completion Intent and uses `startService`, `startForegroundService` or `sendBroadcast`, depending on host-provided extras. Its shown source catches `URISyntaxException` but not every runtime service-start/security failure; it can also return `false` if required completion information is absent. Observe the exact call and whether the host receives it.
6. **Host response acceptance:** Even if the plugin attempts a callback, only Tasker-side evidence or a verifiable Android delivery receipt establishes Tasker actually received and accepted it. "Called signalFinish" is not equivalent to completion acknowledged.
7. **Concurrent callbacks:** Unique PendingIntent request codes are tracked in plugin preferences. Test creation, callback matching, process restart, and concurrent actions rather than assuming a collision.
8. **Boot timing:** Determine whether failures happen before the first unlock, just after unlock, or long after boot. Direct Boot credential-encrypted storage and component registration matter; ordinary Java hooks cannot change an APK manifest's `directBootAware` registration.
9. **Permission and package state:** `com.termux.permission.RUN_COMMAND` is required for Tasker, and Termux bootstrap/accessible files are prerequisites; confirm them at the time of failure, without silently granting anything.
10. **Tasker error wording:** "Plugin didn't respond" is treated as a host communication symptom; it is **not** automatically diagnosed as a command timeout. Capture the exact host error code, broadcast/callback states, and timeline.

### 1.3 Failure classification (**P**) — do not collapse these

| Code | Meaning | Evidence needed | Recovery eligibility |
|---|---|---|---|
| TT-H01 | Tasker never dispatched action | Tasker-side trace | Only host-level notification; never replay |
| TT-H02 | Android denied/failed broadcast delivery | Host dispatch + system logs; receiver absent | Diagnose permission, package or system state; no automatic replay |
| TT-H03 | Receiver cold start or initialization failed | Process-start and crash evidence | Local hook only if receiver process reaches safe code |
| TT-H04 | Receiver rejected configuration or policy | Matched request and immediate error | Preserve original meaningful failure and Tasker variables |
| TT-H05 | Receiver accepted but Termux service start failed | Dispatch exception, no execution confirmation | Return failure when possible; no duplicate execution |
| TT-H06 | Immediate acknowledgment missing/invalid | Ordered-broadcast outcome absent | Protocol correctness investigation; possible narrow hook |
| TT-H07 | Pending result never reached result service | Matched PendingIntent and Termux result trace | Diagnose Termux process/service, no replay |
| TT-H08 | Completion callback failed delivery | `signalFinish` false/exception, Android denial | Controlled *callback-only* recovery if host intent is still valid, no script rerun |
| TT-H09 | Tasker did not accept callback | Completion sent but not acknowledged | Optional Tasker-side host instrumentation |
| TT-H10 | Action request arrived before user unlock | `UserManager.isUserUnlocked=false` with boot timeline | Report unsupported pre-unlock execution; no manifest bypass |
| TT-H11 | Command executed but host action timed out | Shell timing and host timeout evidence | Report separately; not a substitute diagnosis for H01–H09 |
| TT-H12 | Root-helper unavailable or rejected | Privilege handoff errors | Degrade to stock plugin; root must never be mandatory |

## 2. One-module architecture and execution boundaries

| Layer | Target process | Purpose | Default |
|---|---|---|---|
| Termux:Tasker editor adapter | `com.termux.tasker` normal process | Full runtime-injected editor, validation, save/restore | Enhanced when pinned compatible; original fallback |
| Termux:Tasker runtime adapter | `com.termux.tasker:background` | Receiver entry/exit, ordered-result status, PendingIntent creation, dispatch milestones | Read-only traces first |
| Result callback adapter | `com.termux.tasker` results service process | Observe completion metadata and safe failure classification | Read-only initially |
| Optional Tasker host adapter | Installed Tasker host package, version-pinned | Observe request dispatch and delivery acceptance if plugin-only evidence insufficient | Off by default; no guessed hook symbols |
| Optional Termux adapter | Installed `com.termux` version-pinned | Observe service acceptance and result handoff without changing shell behavior | Off by default |
| Shared module UI/settings | One installed LSPosed APK | Domain toggles, compatibility diagnostics, request timeline, recovery panel | Accessible even if plugin process fails |
| Shared restricted root helper | Existing user-approved `su` provider, **not** a separate module | Read-only Android/package/process/user-state diagnostics, optional approved repair | Privileged actions off by default |
| SystemUI | **None** | Not involved with Termux:Tasker runtime or editor reliability | Never installed for this domain |

Use a versioned diagnostic event schema with: monotonic time, wall-time for correlation, boot ID if available, action correlation token, scope/process, event stage, result/error class, exception family, elapsed time and safe redacted command metadata. A correlation ID does **not** become a Tasker protocol extra; carry it through a module-owned, bounded side channel keyed only to authorized session data. When a definitive link is impossible, label correlation *probable/time-based*, never certain.

No file-path, stdin, full argument string, script body, stdout/stderr, secrets or tokens in logs by default. Module log buffers are bounded, TTL-managed, permission-restricted and exportable only after review. An unavailable root helper must not block IPC or user automation.

## 3. FULL Termux:Tasker configuration UI/UX revamp

### 3.1 Navigation, overall visual and behavioral contract (**P**)

Directly opening an existing **Tasker → Plugin → Termux:Tasker → Edit** action must enter the **enhanced editor** without modifying the saved action. Navigation:

- **Command:** executable selection, script explorer, argument token preview, working directory, stdin.
- **Execution:** Background/Terminal segmented mode, mode-specific controls, session action, log level, boot/runtime preflight.
- **Results:** wait-for-result, returned-variable mapping, Tasker timeout explanation, empty/truncated result handling.
- **Test & Diagnostics:** opt-in safe tests, non-executing validation, last matching request/response trace, failure explanations.
- **Advanced:** raw original fields, Tasker variables, plugin compatibility and original-editor switch.

Use a compact top summary: `Executable` (resolved/deferred/invalid) · `Mode` · `Result mode` · `Permissions` · `Plugin handshake` (observed/not observed, never an invented "online" state). Provide persistent **Save**, **Discard** and **Keep Editing** actions; label actual save versus navigating Back. No auto-execute on form opening or while typing. Present a clear *Enhanced / Original* switch and a global disable option.

Provide theme-aware material controls, dynamic spacing, readable contrast, font scaling, landscape/tablet layouts, edge-to-edge/status/keyboard insets, touch accessibility, TalkBack and keyboard navigation. A companion app navigation route is supplemental; Tasker's embedded config entrypoint is primary.

#### Proposed visual layout

```text
Termux:Tasker — Configure Action                   [Original]
────────────────────────────────────────────────────────────
[Command] [Execution] [Results] [Test & Logs] [Advanced]

COMMAND
Executable    [ ~/.termux/tasker/sync.sh           ] [Browse]
              ✓ File found · readable/executable
Arguments     [ --mode "two words" --count 3       ]
              Preview: argv[0]=--mode, argv[1]=two words, ...
Working dir   [ ~/Code/project                      ] [Browse]
stdin         [ Edit stdin... ]  120 characters · bundle size

EXECUTION
Run in        (●) Background     ( ) Terminal session
Custom log    [ Normal ▾ ]          [Advanced raw value]

RESULTS
Wait for result [✓]   Host timeout configured in Tasker
%stdout  %stderr  %result   %err   %errmsg

HEALTH / DIAGNOSTICS
Last host attempt: [No local evidence yet / stage + elapsed]
[Inspect communication] [Validate configuration]

────────────────────────────────────────────────────────────
                               [Discard] [Save to Tasker]
```

**A concept, not a real screenshot or implemented app UI.** Display explicit markers: *Example*, *Local validation*, *Observed Android state*, *Actual Tasker response*.

### 3.2 Every current configuration field — complete coverage (**V names, P upgrades**)

| Existing v0.9.0 control | Enhanced design | Rule for saving/behavior |
|---|---|---|
| **Executable** `executable_path` | Searchable file explorer, favorites/recents, quick paths, file info, exact resulting path, manual mode | Write back to original `AutoCompleteTextView`; preserve original path syntax/Tasker variables |
| **Arguments** `arguments` | Multiline or optional token chips, quote-aware syntax diagnostics, read-only `argv` preview, examples | Preserve original argument string byte-for-byte unless explicitly edited; exact `ArgumentTokenizer` semantics |
| **Working directory** `working_directory_path` | Searchable directory picker, preview of canonical path, writable/existence status, optional/default explanation | Keep null/empty distinctions and Tasker variables; do not create directories in preview |
| **stdin** `view_stdin` | Dedicated multiline editor with monospace view, search, length meter, estimated Bundle size, revert/draft recovery | Preserve `mStdin`, original TextIO contract and Tasker size limits; no silent truncation or script execution |
| **Terminal Session Action** `session_action` | Labeled selector for values supported by installed Termux constants; contextual help | Map to exact legacy numeric string; expose raw/variable entry for advanced users |
| **Custom Log Level** `background_custom_log_level` | Readable enum selector with source/meaning and advanced raw entry | Preserve null/default and exact allowed integer bounds |
| **Execute in terminal session** `in_terminal` | Clear Background/Terminal segmented control and runtime explanation | Update original checkbox; preserve both mode-specific values when switching |
| **Wait for result** `wait_for_result` | Toggle with precise distinction between command output and plugin acknowledgment | Persist original boolean; do not invent plugin-local Tasker timeout |
| **Executable absolute path label** `executable_absolute_path` | Inline canonical path/variable deferred-state indicator | Suppress false errors for unresolved `%variables` |
| **Working directory absolute path label** `working_directory_absolute_path` | Inline path resolution and permissions | Respect Termux user and storage boundaries |
| **Termux files inaccessible warning** | Troubleshooting card, details and safe shortcuts | Do not claim `su` bypasses missing/bootstrap inaccessible files |
| **Tasker RUN_COMMAND permission warning** | Explicit "Permission missing" card with Android Settings navigation where available | Never auto-grant permission; Tasker must remain authorized |
| **allow-external-apps policy warning** | Clear policy explanation and risk warning | Never silently enable policy or weaken path restrictions |
| **Help URL / log level menu** | Contextual help, advanced logs, version/compatibility, privacy-safe export | Preserve original help and diagnostic fallback |
| **Toolbar Cancel / Done / Back** | Explicit Save / Discard / Keep Editing with validation and dirty-state confirmation | Preserve Locale/Tasker `RESULT_OK`, `EXTRA_BUNDLE` and blurb contract |

### 3.3 Script and directory explorer (**P**)

- Browse only Termux paths actually accessible to the plugin's UID, respect root boundaries, do not silently escalate. Include `~/.termux/tasker`, `~/`, `$PREFIX/` shortcuts when valid; no inaccessible-path promises.
- Search directories and executable candidates; clearly separate executable files, directories, links, permission errors and unknown variable-derived paths. Keep original manual autocomplete available.
- Canonicalize for display and validation without altering user-selected textual representation unless chosen. Reject path traversal only where the original plugin would reject it; do not loosen upstream path restrictions.
- Browse operations are local and read-only. Avoid synchronous large-directory work on the UI thread; use bounded asynchronous enumeration with cancellation and stale-generation guards. Never call `su` to enumerate sensitive unrelated folders by default.
- Expose "Open directory" or "Open in Termux" only if a supported component and user confirmation exists; no execution side effects in browsing.

### 3.4 Argument Studio (**P**)

- Parse the user's arguments using the **original** `com.termux.tasker.ArgumentTokenizer`, not shell parsing or an invented tokenizer. Show token count, quoting and escapes; highlight ambiguous or incomplete literals.
- Distinguish **Tasker variable substitution at runtime** from a preview using sample input. A variable-containing path/argument must not be flagged as a nonexistent literal file.
- Preserve whitespace and raw input exactly when user has not edited it. Preview must not normalize or silently reorder arguments.
- Offer examples for `--flag value`, quoted spaces, escaped quotes and local `%variables`, with explicit warnings that the command is not implicitly a shell command line.
- Never autocomplete script output or query shell history in the background.

### 3.5 Execution Mode Studio (**P**)

- Show **Background** versus **Terminal Session** semantics and field visibility. Hide *whole* irrelevant cards, not just inner EditText fields; restore previously entered values when the mode is toggled back.
- Session Action options derive from pinned Termux constants or an explicitly verified mapping; unknown values are retained in a raw field rather than changed.
- Background custom log levels use labels derived from the installed logger constants; optional null/default remains distinct from an explicit level.
- Explain terminal session limitations when Android cannot start a foreground activity from the background; do not label them as plugin communication failures.
- Show actual user-unlock/readiness and permission state without implying root is required to run commands.

### 3.6 Results and Tasker Variable Studio (**P**)

- Display available result variables and behavior of `%stdout`, `%stdout_original_length`, `%stderr`, `%stderr_original_length`, `%result`, `%err` and `%errmsg`, according to output mode and host support.
- Explain **ordered broadcast received**, **pending accepted**, **command launched**, **result received**, and **Tasker completed** as separate stages.
- Show host timeout location in Tasker and what `Wait for result` actually changes. **Do not present a fake plugin-side timeout slider.** The user's confirmed problem is not automatically reclassified as command timeout.
- Show safe output-length indicators and truncation/original-length differences when returned. Do not claim that changing Intent limits restores Termux output already truncated upstream.
- Provide explicit optional output capture or sample result preview; no real execution without direct approval. Keep stdout/stderr content out of diagnostics by default.

### 3.7 Validation, Save/Cancel, life-cycle and recovery (**P**, release blockers)

- Fix the current `savedInstanceState != null` early-return behavior *without assuming* Android already preserves private `mStdin` or that the original Tasker bundle is safe to reload over user edits. Save/restore all visible inputs, `mStdin`, mode-specific fields, touched/dirty flags and pending picker state through module-local ephemeral state.
- Preserve the original `Activity` result and `PluginBundleManager.generateBundle()` contract. **Do not add keys**: `parseBundle()` validates expected key inventory/count (3–10 accepted). Keep host variable-replacement extras, version code, blurb and relevant-variable metadata.
- Validate before actual Save: executable nonempty; verify concrete paths/permissions without rejecting variables; session/log-level values in range; stdin and entire serialized Bundle under limits; path policy and permission warnings accurately differentiated. Do not silently tighten original rules for legitimate dynamic configurations.
- Existing activity routes Home/Back through `finish()`. Intercept user intention at navigation before the original method can save by accident; reentrancy-proof cancellation and Save/Discard state machine. Test callback order, no duplicate `finish()` and no lost results.
- Save unchanged action → no semantic change. Unknown older/newer fields must be retained in original mode; module-only preferences must stay in module storage, never the Tasker bundle.
- Actionable errors: field focus, concise reason, Android/system settings links when appropriate, and privacy-safe technical details. No automatic granting/modifying `RUN_COMMAND`, external-app policy or package configuration.

### 3.8 Diagnostics UX (**P**)

- Module dashboard: **Overview** (installed identities, grants, availability), **Communication Timeline** (Tasker dispatch → receiver → Termux → callback), **Editor Validation**, **Root Readiness**, **Safe Mode**, **Export Logs**.
- Show last observed event with timestamps, phase, elapsed time, receiver PID/process, and confidence. If Tasker-side tracing is not enabled and the plugin never started, show **"No receiver invocation observed"**, **not** "Tasker failed to dispatch".
- Provide "Test plugin communication" as an **explicitly initiated**, non-destructive probe only when authorized and technically supported. Separate *test broadcast/acknowledgment* from *test shell command*; do not synthesize a Tasker host result or claim a probe proves the real configured action worked.
- Test execution (if ever offered) requires an explicit review of executable and arguments, with irreversible actions blocked from auto-repeat; no surprise command execution from a UI inspector.

## 4. Reliability implementation: Tasker → plugin communication

### 4.1 Stage A — Read-only, dual-sided diagnostic capture (**P**, mandatory first)

Instrument **without changing behavior**:

1. Tasker host-side (only after discovering exact runtime methods/signatures): action dispatch ID/type, ordered broadcast sent/rejected/finished, host callback acceptance, error class. Do not read unrelated Tasker actions.
2. Plugin `FireReceiver.onReceive()`: entry/exit, PID/process cold start, safe bundle key/schema checks, return/result code, exception family and timing.
3. `PluginUtils.sendExecuteIntentToExecuteService()`: isOrderedBroadcast, pending vs immediate branch, PendingIntent creation, service start attempt and outcome, without logging executable content.
4. `PluginResultsService.onHandleIntent()`: result callback arrival and original-intent matching; possible null/bad data.
5. `PluginUtils.sendPendingResultToPluginHostApp()` + `TaskerPlugin.Setting.signalFinish()`: completion channel chosen, returned boolean, exception and Android delivery attempt.
6. Optional `com.termux` service hook: execution request accepted and result constructed; only with an exactly compatible Termux binary, no speculative hooks.
7. Shared root helper: inspect `cmd package`/package state, process lifecycle, `dumpsys activity` broadcast/service evidence where available, boot ID and `UserManager.isUserUnlocked`, targeted logcat markers. Require explicit user approval for log capture.

An observation chain must identify phases accurately; a missing late event may be log loss, host process death, or callback rejection and is not proof the command never executed.

### 4.2 Stage B — Narrow fixes only for proven failure paths (**P/C**)

| Proven issue | Conditional repair | Must not do |
|---|---|---|
| Receiver crashes in early initialization | Isolate optional nonessential work; preserve original crash/reporting semantics; return structured failure when feasible | Swallow fatal bugs or fabricate success |
| PendingIntent generation fails | Wrap carefully selected setup boundary and provide clear ordered result where supported | Bypass underlying Android security or assume result already existed |
| Foreground service start rejected | Surface exact denial and preserve a consistent failure result | Repeat command via root to defeat background rules |
| `signalFinish` throws or returns false | Record and, *when provably safe and callback still valid*, retry **only callback delivery** with bounded attempts | Re-execute script or send duplicate callbacks after confirmed host acceptance |
| Receiver not invoked, host says plugin unresponsive | Diagnose host intent/package permission/Android delivery; optional host-side compatibility hook if verified | Pretend an app-side `onReceive()` hook fixes non-delivery |
| Termux service process dies after command acceptance | Return a truthful unknown/failed state if result is lost; allow user-controlled recovery workflow | Assume shell work did not happen and automatically rerun |
| User storage locked | Report/defers unsupported pre-unlock action at host workflow level where possible | Force manifest component Direct Boot support, execute scripts before CE is accessible |
| Concurrent result requests intermingle | Fix verified identity/correlation collision with narrowly scoped handling and tests | Rely on process-local guessed IDs or risk leaking results to wrong task |

**Idempotency rule:** Every execution has a local semantic state: **not accepted / accepted, execution unknown / executed / completed**. Automatic command replay is off by default. A Tasker command performing an irreversible side effect may never be rerun solely because its acknowledgment was lost. Do not re-send any pending completion when the host's acceptance state is unknown without explicit duplicate-safe evidence.

### 4.3 Boot and cold-start reliability (**P/C**)

Track **pre-first-unlock**, **immediately after first unlock**, **device idle**, **after app update**, **after force-stop/kill**, **battery restriction**, **concurrent Tasker startup tasks**, and **normal running** as distinct environments.

- Measure package-enabled state, permission grants, background process cold-start timeline and Termux files accessibility. A cold start is not inherently a bug; report actual delay and errors.
- Root helper may support a user-approved **read-only readiness probe**, but not blind prewarming by launching unrelated activities or forcing service starts. Do not change system boot settings/permissions without separate authorization.
- Since the plugin receiver is not declared `directBootAware`, and Termux relies on normal user app data, **pre-unlock command execution is not guaranteed and is explicitly outside first-release acceptance**. Detect and explain rather than silently bypass.
- If a verified Tasker host-side hook can postpone a not-yet-dispatched **idempotent** action until user-unlock/valid readiness under the existing Tasker task semantics, it can be considered opt-in; Tasker's original task order and cancellation behavior must be proven unchanged. Otherwise leave startup tasks untouched and expose an actionable warning.
- After the first unlock, the acceptance target is reliable delivery of **existing configured Tasker actions** without requiring the user to open Termux:Tasker manually.

## 5. Root-assisted diagnostics and security boundary

Root is **optional and supplemental**, not an alternate shell-execution backend. Use the same root-provider integration as the other three domains, with strictly enumerated operations: package/version/UID/process diagnostics, user-unlock and permission readback, service/broadcast logs and optional explicitly approved recovery probes. No unrestricted shell input from Tasker script, editor, IPC parcel or Web Screen can enter the helper.

- Authenticated caller identity, command allowlist, argument validation, minimum data exposure, durable audit of privileged operations and failure codes. No privilege grants just for convenience.
- Root operation invoked asynchronously; normal plugin communication never waits for a root command and never depends on root being present.
- Do not mutate Termux's app data, scripts, shared UID, owner/group/permissions, policy file or Tasker grants automatically. `allow-external-apps=true` is explicitly security-sensitive; a display warning is not permission to enable it.
- No `force-stop`, process killing, clearing app data, AppOps overrides, keyguard bypass, Direct Boot manifest rewriting, battery restriction hacking or global broadcast hacks as an automatic recovery strategy.
- If Android already denies an operation, report and preserve restriction; root must not silently reimplement the Tasker plugin protocol outside the trusted caller.
- Root diagnostics should never be bundled in an independent Magisk module or require replacing the original APK. The same installed LSPosed companion owns settings and the restricted helper interface.

## 6. Compatibility and cross-domain guarantees

1. **Single shared APK:** one companion dashboard with AutoNotification, AutoInput, AutoTools, Termux:Tasker. Separate domain feature flag, version matrix, hook guard, diagnostics buffer and disable switch.
2. **No SystemUI for Termux:Tasker:** the shared SystemUI adapter may exist for other domains but installs no Termux:Tasker hooks or dependencies.
3. **Termux UID and signing:** since Termux:Tasker uses `sharedUserId`, preserve existing install/signature family and user data; *never* recommend replacing it just to work around IPC.
4. **Tasker version:** installed host version and plugin protocol must be verified before any optional host-side injection. Unsupported host behavior falls back to read-only OS tracing or original behavior.
5. **Termux version:** optional execution-service hooks require exact class/method mapping and verified package identity; otherwise observe via plugin/root logs only.
6. **Source pinning:** the v0.9.0 Git source tag verifies the implementation reference; pin installed APK binary signature and hashes again at implementation. Never assume git source SHA proves byte-for-byte identity with supplied APK.
7. **No handshake modification unless necessary:** original `Bundle` schema, Locale extras, ordered result codes, PendingIntent, relevant-variable declaration and `signalFinish()` contract remain stable.
8. **No cross-domain privilege escalation:** HTML editor, JSON Studio or AutoInput visual inspector may not invoke Termux commands through this domain without the user's authorized Tasker action and proper permission.
9. **No hidden incompatibility:** unknown/unsupported method class means feature disables with a visible reason and original UI/IPC remains usable; no crash on obfuscation or class-loader mismatch.
10. **Version upgrades:** compatibility policy differentiates known pinned, detected compatible, unknown and incompatible; unknown versions run only safe read-only diagnostics when feasible, not brittle behavior hooks.

## 7. Verified hooks and binding inventory

| Scope | Candidate entry / method | Intended use | Evidence and gate |
|---|---|---|---|
| Editor | `EditConfigurationActivity.onCreate(Bundle)` | Inject enhanced editor after original view inflation | **V**, verify installed class and view IDs |
| Editor | `EditConfigurationActivity.onResume()` | Refresh safe readiness checks | **V**; no filesystem UI blocking |
| Editor | `EditConfigurationActivity.finish()` | Save/Cancel/Back validation and result preservation | **V**, reentrancy and Tasker return tests |
| Editor | `processExecutablePath(String)` | Native validation hints and canonicalization status | **V**, no app behavior changes before tests |
| Editor | `processWorkingDirectoryPath(String)` | Native path diagnostics | **V** |
| Editor | `processIntFieldValue(...)` | Existing numeric bounds | **V**, preserve `%variable` passthrough |
| Editor | `updateStdinViewVisibility(boolean)` | Hide whole mode-dependent group | **V**, after original method |
| Editor | `updateSessionActionViewVisibility(boolean)` | Terminal-only section | **V**, after original method |
| Editor | `updateBackgroundCustomLogLevelViewVisibility(boolean)` | Background-only section | **V**, after original method |
| Bundle | `PluginBundleManager.parseBundle(...)` | Validate original config schema and warning conditions | **V**, read-only first |
| Bundle | `PluginBundleManager.generateBundle(...)` | Keep original saved result construction | **V**, no extra keys |
| Args | `ArgumentTokenizer.tokenize(...)` | Exact argv preview | **V**, no interpreter substitution |
| Receiver | `FireReceiver.onReceive(Context, Intent)` | Ordered broadcast/cold-start events | **V**, main diagnosis surface |
| Plugin execution | `PluginUtils.sendExecuteIntentToExecuteService(...)` | Pending/immediate ack and service dispatch | **V**, behavior fix conditional |
| Result | `PluginResultsService.onHandleIntent(Intent)` | Result reception | **V** |
| Result | `PluginUtils.sendPendingResultToPluginHostApp(...)` | Pending result mapping | **V** |
| Completion | `TaskerPlugin.Setting.signalFinish(...)` | Host callback success/failure | **V**, catch/error handling conditional |
| Host | Actual Tasker broadcast/plugin service methods | Pre-receiver trace/ack acceptance | **C**, no method names guessed |
| Termux | Actual Termux service methods | Execution receipt/result generation | **C**, exact installed package required |
| Root helper | Android package/process/broadcast diagnostics | Pre-receiver and boot state | **P**, read-only and opt-in |

These are **hook candidates**, not claims that every method is safe to modify. Default installation is instrumentation-only and feature-by-feature enabled after gates pass.

## 8. Consolidated milestones — no serial micro-patches

| Milestone | Complete deliverable | Gate before advancing |
|---|---|---|
| **TT-F0 — Exact runtime baseline** | Pin APK SHA/signature, Tasker/Termux/Android versions, root provider and LSPosed identity. Stock-config edit round trips. Reproduce existing-action "plugin didn't respond" and build read-only communication correlation across host/receiver/result. | Evidence distinguishes at least initial delivery vs callback paths, or reports precise missing observation and next capture |
| **TT-F1 — Full Termux:Tasker domain delivery (single consolidated overlay)** | All **UI/UX controls**, editor persistence and save fixes, file picker, argv preview, result studio, root diagnostics interface, dual-sided IPC telemetry, failure classification, bounded and proven communication fixes, boot-state handling, settings, privacy, tests and docs. No deferred UI subfeatures. | Native Devtool overlay validation and negative/compatibility tests; existing actions unaffected; recovery cannot double-execute |
| **TT-F2 — Conditional host/Termux compatibility expansion** | Only if TT-F0/F1 evidence proves plugin-only hooks cannot fix a specific failure, add exact-pinned Tasker or Termux runtime adapter, matching tests and explicit safety guard in **one consolidated milestone**, not a handoff-specific patch chain. | Demonstrated benefit with no protocol regression, no duplicate script execution, original fallback verified |
| **TT-F3 — Shared module hardening and release** | Integrate fully with the **single** four-domain module, prune obsolete tests/gates, run hosted CI and physical Android 16 acceptance, verify module disable/uninstall recovery, sign the **one** release APK, document source/pr/CI/device status. | All mandatory acceptance evidence satisfied or explicitly marked outstanding; no release-ready claim otherwise |

Milestone numbers are organizational, not an excuse for a serial chain of corrective P01/P02/HF patches. If a consolidated artifact fails, correct the cause and revise the same milestone deliverable; resume native durable sessions after a committed apply rather than reapplying.

## 9. Full acceptance and regression matrix

### 9.1 Existing Tasker actions and plugin handshakes

- **Normal:** 20 sequential actions, at least two representative scripts (short success and nonzero exit), ordered response, pending response, stdout/stderr/result variables and `%err/%errmsg` semantics.
- **Reboot:** 10 boot-after-first-unlock trials with existing Tasker startup actions, without opening the plugin first; record OS/Tasker/module states and each phase. No false "successful" classification when receiver not called.
- **Pre-unlock:** 3 observational trials with a clear, truthful unsupported/locked-state report where that restriction applies. These are **not expected successful script executions**.
- **Cold start/process death:** Target killed when safe to reproduce, recreated background process, Termux service absent, host restarts, permission revoked and restored. Distinguish force-stopped packages (platform limitation) from recoverable cold starts.
- **Concurrency:** 50 mixed sequential/concurrent requests; correlate each output to its initiating Tasker action, no cross-task mixups or duplicate callback artifacts.
- **Protocol:** Tasker task with `Wait for result` on/off, host timeout 0 and >0, foreground and background modes, immediate and pending errors, host cancellation, missing/completed callback, intent corruption and binder limits.
- **Failure injection:** Deliberately trigger receiver validation failure, denied service start where reproducible, missing executable, invalid permission, broken path, invalid bundle, missing/cancelled PendingIntent, result-service crash and completion delivery denial. No successful statuses on failed operations.
- **No-replay guarantee:** A script incrementing a counter must never increment more than once for a single Tasker execution when a result/callback is delayed or lost. Host-only callback retry must not relaunch shell execution.

### 9.2 Complete editor and UX

- Open and save unchanged legacy actions; compare bundle field values, count/keys, version, result blurb and variable substitution settings before/after.
- Create new action; edit every field; Save, Cancel, Back/Home, gesture back, rotation, OS process recreation, activity returning from TextIO stdin editor, multiwindow and app/task switching.
- Run all modes (background/terminal), checking whole-card visibility and preserved mode-specific values on toggles.
- File picker: empty folders, unreadable directories, symlinks, large directories, non-executable files, `%dynamic` paths, `$PREFIX` and `~/` forms, directory picker and manual autocomplete fallback.
- Arguments: Unicode, quotes/escapes, trailing escapes, empty argument and multiple spaces; preview matches actual `ArgumentTokenizer` for valid cases and does not change original stored input.
- Stdin: empty, long, multibyte, special characters, near-bundle-limit, over-limit, rotation and returning from nested editor; no silent data loss.
- Numeric inputs: defaults, min/max, out-of-range, nonnumeric strings, legitimate `%variables` and legacy unknown values.
- Screens: dark/light, Android font scaling, landscape, narrow window, IME insets, TalkBack labels and focus order, permissions denied, module toggles, original editor fallback.
- Test/diagnostics UI: no execution on typing; explicit opt-in for any shell test; data redaction is the default.

### 9.3 Security and negative tests

- Protect `com.termux.permission.RUN_COMMAND`: ungranted callers cannot invoke plugin commands; no bypass through root helper or a new exported endpoint.
- Preserve external-path policy and original permissions, no auto-enable `allow-external-apps`, no direct execution of unauthorized files.
- No command/stdin/argv/stdout/stderr/secrets included in diagnostic exports by default; optional sensitive capture requires explicit consent, scope and TTL.
- Root unavailable/denied/malformed input/timeout must leave existing Tasker operation functional or fail with the original meaningful result, never hang the response path.
- Untrusted Intent extras, corrupted Bundle, missing pending intent and invalid Tasker completion URI must not become arbitrary privileged operations.
- Unknown APK/Tasker/Termux versions, class loader failure, concurrent hook initialization and double-install attempts must fail closed for the enhancement while retaining stock behavior.
- No SystemUI changes for this domain; other three LSPosed domains remain operational when Termux:Tasker domain is disabled or crashes.

### 9.4 Device, hosted, and release evidence

For every mandatory test, report **executed/passed/failed/skipped** and environment: JVM/unit, Android integration/emulator, native Devtool overlay validation, hosted CI and physical Android 16 device. A successful CI check does not establish physical-boot reliability. Include signed one-module APK identity and verification of installed host/process compatibility only after actual installation/testing. Use screenshots/recordings as supplemental UX evidence only, redacted.

## 10. Native Devtool Launcher v3.1+ delivery contract (future implementation)

Before **each overlay**, inspect `Mikeyphw/devtool` `main`, record exact origin, HEAD, tree/blob identities and verified latest **compatible native Devtool Launcher v3.1+**. Autodiscover the named target branch/worktree; detached HEAD may serve as a verified runtime checkout but **not** the target branch. Pin exact source and payload hashes; verify the overlay file inventory and package integrity before use.

Use the native launcher for preflight → package verification → native overlay validation → atomic apply/commit → durable session status/recovery → user-approved push/PR → optional hosted CI watch. **Do not** reimplement the overlay engine, copy patches manually, do ad hoc `git` replacement or repin immutable artifacts silently. If launcher is incompatible, repair launcher compatibility before delivery.

Native plain-terminal execution only:

```sh
DEVTOOL_EXECUTION_PRESENTATION=plain \
DEVTOOL_JOB_UI=plain \
DEVTOOL_TEXTUAL_APPLY_HOST=0 \
DEVTOOL_TEXTUAL_EXECUTION_HOST=0 \
python3 -B VERIFIED_NATIVE_LAUNCHER.pyz  # replace with verified package + supported documented arguments
```

The example establishes the required environment, **not** a valid final execution command; first inspect the current launcher CLI and arguments. Use real stdout/stderr and obey `NO_COLOR`, `CLICOLOR=0` and `TERM=dumb`; do not auto-copy terminal data to clipboard. Do not silently suppress diagnostics or errors. No reset/rebase/force push/auto-merge/overwriting unrelated user work.

Audit impacted tests, scripts, docs, workflow discovery and CI before changes. Update/consolidate modern checks; remove genuinely obsolete historic HFxx/Pxx/Rxx handoff-only gates without reducing security, compatibility or regression coverage; never delete a failing meaningful check to get green CI.

One **complete overlay per milestone**. When native validation fails, preserve and request the **full launcher + Devtool logs** if needed, fix the cause and revise the consolidated artifact. After a committed apply, resume the durable session using the current supported `status`/`deliver`/`ci` subcommands, rather than reapplying.

Mandatory delivered report for any future overlay: repository, named branch, starting HEAD, resulting commit, launcher version/runtime, exact changed files and scope, changed/consolidated/deleted tests with reasons, preflight/native/local test outcomes, root/system/device evidence separately, PR and CI status, **one actual ready-to-run Termux command** (using verified CLI), limitations and remaining acceptance. Only report `PUSH_VERIFIED`, `PR_VERIFIED` or release readiness with corresponding proof.

**No repository was selected and no overlay executed in preparation of this roadmap.** The exact launcher command, branch/HEAD, commit and PR therefore cannot truthfully be supplied yet.

## 11. Reconciliation with earlier Termux:Tasker suggestions — explicit disposition

This fills the remaining differences between `AutoInput_TermuxTasker_LSPosed_Audit.md`, `Termux_Tasker_0.9.0_Config_UIUX_LSPosed_Audit.md`, and this runtime-communication-first roadmap. **No SystemUI hooks** belong to this domain.

| Earlier suggestion | Disposition | Acceptance boundary |
|---|---|---|
| Configured Tasker action says plugin itself did not respond, particularly after boot | **Required TT-F0 tracing and TT-F1 evidence-driven fix** | Tasker host dispatch → Android receiver entry/ack → Termux handoff → result callback; distinguish plugin non-response from script completion timeout |
| Complete executable/arguments/workdir/stdin/mode/results/Save/Cancel editor overhaul | **Required TT-F1** | Existing Locale/Tasker Bundle unchanged unless explicitly edited; original fallback |
| Preflight for shebang, executable permissions, policy, path, version and Termux availability | **Required TT-F1 non-executing preflight**: show shebang/interpreter availability where the file is readable; defer checks on Tasker variables | Do not execute script, chmod unrelated files or silently enable `allow-external-apps` |
| Missing, late and truncated results | **Required TT-F1 diagnostics**: show original vs delivered stdout/stderr lengths and callback stage | Preserve Tasker result schema and do not claim missing bytes are recoverable |
| **Optional large-result transport (previous TT-04)** | **Conditional future feature within the same module**, not an invisible fix: an explicitly selected, authenticated **file-backed output workflow** for cooperating scripts, with safe Termux-accessible path, unique run ID, atomic handoff, file size/TTL caps, cleanup, conflict safety and explicit output import | The plugin cannot recover already-truncated Intent output. Must test >100 KB, concurrent runs, aborted writes, Unicode/binary and no root-based bypass of Termux path policy. May be activated only after an end-to-end test; otherwise mark unsupported |
| **Optional structured results (previous TT-07)** | **Conditional opt-in feature**: module-local JSON summary of stage/exit/length/error data or a script-side JSON result produced by a cooperating workflow; keep `%stdout`, `%stderr`, `%result`, `%err`, `%errmsg` untouched | Validate exact Tasker variable semantics, null/empty distinction, quoting, nonzero exit, Unicode and payload limits; never promise arbitrary new legacy plugin keys |
| Cancellation and idempotency | **Required TT-F1 safety controls**, behavioral replay off by default | Never rerun scripts solely due to lost acknowledgment/callback |
| Upstream source-level fixes / APK rebuild | **Intentionally excluded from this LSPosed-only project**; separate proposal only if the constraint is explicitly changed | No original APK changes, resigning, `sharedUserId` disturbance or bypass of `RUN_COMMAND` |
| Root-as-shell execution backend; SystemUI fixes; automatic Direct Boot permission bypass | **Intentionally excluded** | Root is for restricted diagnostics and approved recovery, not replacing trusted command execution |

**Important scope distinction:** Only **required** items block TT-F1 acceptance. Conditional large-output and structured-result facilities must not be misrepresented as already implemented or guaranteed on Android 16, but their design and negative tests remain tracked within this **one shared LSPosed module** (not a separate Termux plugin APK). Existing Tasker actions must keep working without adopting either option.

## 12. Risks, dependencies, decisions and explicit non-goals

| Risk/unknown | Required resolution | Decision rule |
|---|---|---|
| Installed Tasker version unknown | Read installed package version and actual host dispatch behavior | No proprietary Tasker hook before version/signature mapping |
| Installed Termux release/signature unknown | Read installed package/service identity and Termux bootstrap availability | Optional Termux-side hook stays disabled until pinned |
| Error phrase alone does not identify root cause | Gather host + receiver + result-service logs near actual failure | Diagnose before changing handshake semantics |
| Boot-before-first-unlock limitations | Record Direct Boot state and first-unlock events | No promise of pre-unlock script execution |
| LSPosed delivery cannot intercept an unstarted process | Optional Tasker/OS/root observations | No fake receiver-side "fix" for absent receiver |
| Running scripts can have side effects | Classify command execution states and host acknowledgment | No automatic command replay under uncertainty |
| Runtime UI injection may break lifecycle | Keep original view inputs and a safe Original editor toggle | Fail back to stock editor, not crash |
| Existing v0.9.0 bundle schema | Differential roundtrip and versioned tests | No unknown keys added; no incompatible serializer |
| Root introduces new attack surface | Restrict operations/callers, audit outputs | No unrestricted root command receiver or permission bypass |
| Shared-module size/complexity | Common infrastructure + isolated adapters | One APK, no hidden separate Termux module |

**Excluded from promised acceptance:** guaranteed pre-unlock execution; bypassing Tasker permissions or Termux external-app policy; permission or battery-management hacks; automatically rerunning commands; full arbitrary shell output beyond Termux/Intent limits without a separate explicitly chosen file-based workflow; altering Tasker or Termux APKs; SystemUI hooks; bypass of Android's signed/shared-UID installation model; claiming boot reliability before device testing.

## 13. References and source boundaries

- Uploaded exact APK: `Termux꞉Tasker_0.9.0.apk`, SHA-256 listed above.
- Earlier APK + editor audit: `Termux_Tasker_0.9.0_Config_UIUX_LSPosed_Audit.md`.
- Earlier dual-app APK audit: `AutoInput_TermuxTasker_LSPosed_Audit.md`.
- Previous final shared-module roadmaps: `AutoNotification_Final_Consolidated_Roadmap_Shared_LSPosed.md`, `AutoInput_Final_Consolidated_Roadmap_Shared_LSPosed.md`, `AutoTools_Final_Consolidated_Roadmap_Shared_LSPosed.md`.
- Termux:Tasker source (verified v0.9.0): https://github.com/termux/termux-tasker/tree/v0.9.0
- Receiver implementation: https://github.com/termux/termux-tasker/blob/v0.9.0/app/src/main/java/com/termux/tasker/FireReceiver.java
- Plugin response plumbing: https://github.com/termux/termux-tasker/blob/v0.9.0/app/src/main/java/com/termux/tasker/utils/PluginUtils.java
- Tasker host communication implementation: https://github.com/termux/termux-tasker/blob/v0.9.0/app/src/main/java/com/termux/tasker/utils/TaskerPlugin.java
- Editor: https://github.com/termux/termux-tasker/blob/v0.9.0/app/src/main/java/com/termux/tasker/EditConfigurationActivity.java
- Bundle schema: https://github.com/termux/termux-tasker/blob/v0.9.0/app/src/main/java/com/termux/tasker/PluginBundleManager.java
- Layout: https://github.com/termux/termux-tasker/blob/v0.9.0/app/src/main/res/layout/activity_edit_configuration.xml
- Official plugin documentation: https://github.com/termux/termux-tasker/blob/master/README.md
- Android Direct Boot documentation: https://developer.android.com/privacy-and-security/direct-boot

**FINAL OUTCOME:** Termux:Tasker is fully specified as the **fourth independently controlled domain in one shared LSPosed module**, with a complete editor revamp and a communication-first reliability program. The user's reported Tasker↔plugin unresponsiveness is the top validation target, not assumed script timeout. No SystemUI work is required. The document is a plan, not a claim of a successful fix or release.
