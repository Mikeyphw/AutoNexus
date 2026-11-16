# AutoInput — Final Consolidated Roadmap (One Shared LSPosed Module)

**Baseline:** 2026-11-16 · `com.joaomgcd.autoinput` 3.0.12 (`versionCode=104`, target SDK 36) · Android 16 / supplied AxionOS SystemUI.  
**Destination:** **one installed LSPosed companion APK** with four independently configurable feature domains: **AutoNotification**, **AutoInput**, **AutoTools** and **Termux:Tasker**. This document specifies **the AutoInput domain and its shared-core contracts**, not four separate modules.  
**Allowed implementation:** LSPosed runtime hooks, shared module-owned UI, an optional restricted root helper, and *selective* AxionOS SystemUI integration.  
**Forbidden:** rebuilding, patching, resigning, replacing or bypassing the license of the existing AutoInput APK; modifying the original SystemUI APK; silently changing saved Tasker actions; replacing Android accessibility consent or MediaProjection consent with hidden workarounds.  
**Evidence status:** Static APK and resource inspection only. This is a roadmap, **not** a built, installed or device-tested module.

## 0. Provenance and evidence grades

| Artifact | Verified SHA-256 | Verified observations |
|---|---|---|
| `AutoInput_3.0.12.apk` | `4f33f46c2a8b4ff7b4e7c54a93b0ae63fdfcc09f5b1654b185e2f6d2e4295b05` | `com.joaomgcd.autoinput` 3.0.12 (104); API 36 target; two DEX files; Actions v2, legacy actions, UI Query, gestures, screen capture, accessibility and root-related resources |
| `System UI_16.apk` | `8e2d9452ca0e2473ff1574deefb610d40b94b1ecf7810a1efcb05745df720ee9` | `com.android.systemui`; AxionOS Android 16 compiled QS, shade and keyguard class families |

Other inspected materials: `AutoInput_3.0.12_Deep_LSPosed_Audit.md`, `AutoInput_3.0.12_LSPosed_Root_SystemUI_Audit.md`, and their compiled DEX/resource inventories. The original APK packages native libraries, including `libautoinput.so`. Their native implementation and effects have **not** been reverse-engineered or validated; native hooks are **not** a default requirement.

**V = directly present in APK metadata, compiled preference resources, or DEX declarations. P = proposed implementation. C = conditional on physical-device proof.** Installed APK identity, Android build fingerprint, installed root provider, LSPosed/Vector compatibility, original license state, method bodies and actual runtime failures remain unverified.

## 1. Product requirements and shared-module contract

1. **Exactly one LSPosed module install** with a single dashboard and independent feature domains. Domains have separate class-loader resolution, hook activation, validation, settings, telemetry and safe-disable switches.
2. **AutoInput is the feature domain, not a separate APK.** Its app-process hooks target `com.joaomgcd.autoinput`. The root helper is one shared restricted service, never four per-app helpers. SystemUI is a shared adapter used only by features with demonstrated system-UI requirements. Termux:Tasker has **no SystemUI dependency**.
3. **No original APK rebuild or update.** Preserve installed AutoInput, licensing, Tasker plug-in host contract, existing input models, exact serialization, reserved variables, IDs, action order, permissions and application preferences.
4. **Fail-open at the hook boundary:** an unrecognized class, version, native callback or failed auxiliary component should fall back to original AutoInput behavior, not break Tasker action execution. For unsafe automation conditions, **fail-closed for the optional added action**; never emit a speculative tap.
5. **Privacy/security by design:** local-only data by default, no capture of passwords, PINs, OTPs or arbitrary typed text in logs; root narrowly authenticated, disabled for unverified binaries, and explicitly opt-in for actions that alter device state.
6. **No misleading result states:** separate action *requested*, *dispatch accepted*, *gesture completed*, and *application postcondition verified*. A successful shell exit or gesture callback alone is **not** proof that the intended UI change happened.
7. **Preserve all existing capabilities.** Current Actions v2 already supports multi-step sequences and conditions, UI Query already supports wait-for-text/regex/visibility/clickability, and AutoInput already exposes accessibility management, retry controls and historical root input features. Enhance instead of duplicating.
8. **No forced OTA compatibility:** match exact installed APK and critical method hashes. Reject unsafe hooks after unknown AutoInput or AxionOS updates; continue safe app-only functionality where compatible.

### Responsibility boundaries

| Execution layer | Owned by | Purpose | Default |
|---|---|---|---|
| AutoInput runtime adapter | AutoInput domain | Instrument actions/queries/gestures and inject compatible editors | Read-only diagnostics first |
| Domain configuration/editor | Same shared module APK | Comprehensive UI and presets while preserving original plug-in serialization | Enabled after identity checks |
| Restricted root helper | Shared core | Explicit `input`, `screencap`, window/service-state checks and approved recovery | Diagnostic-only or off |
| AxionOS SystemUI adapter | Shared core, feature-gated | Observe shade/QS/keyguard state for AutoInput; rendering for AutoNotification separately | Off until proven necessary |
| `system_server` observations | Shared core, conditional | Narrow framework diagnostics only if simpler layers cannot explain a bug | Off; not part of baseline acceptance |

## 2. FULL configuration and companion UI/UX redesign

### 2.1 Current editor baseline (V)

- **Actions v2 Tasker screen:** `ActivityConfigActionv2`, compiled `config_action_v2` preferences, and separate visual/multi-action helper `ActivityInputActions`.
- **Legacy Action:** `ActivityConfigPerformAction`, `config_perform_action` sections **Easy Setup / Manual Setup / Stored Actions / Advanced / Repeat**.
- **UI Query:** `ActivityConfigUIQuery`, `config_ui_query`; package, field extraction, visibility/clickability, regex, wait-for-text, accessibility/screen options.
- **Gestures:** `config_gestures`, gesture types and dynamic input options.
- **Screen Capture:** `ActivityConfigScreenCapture`, `config_screen_capture`; screenshot/video, crop, pixel color/palette, image comparison, microphone audio, resolution and duration.
- **Input/Key/UI-update events:** individual configuration activities including `ActivityConfigInputActionEvent`, `ActivityConfigKeyEvent`, `ActivityConfigUIUpdateEvent`.
- **Main preferences:** already contain *Accessibility In Foreground*, *Enable Just When Needed*, retry interval, logs and action-error notifications.
- **Legacy root resources:** Root Settings, Select Input Device, Record Input, playback controls. Their continued runtime reachability must be demonstrated before enhancement.

**The new UI must be available from the existing Tasker configuration flows, not only the module launcher.** Prefer runtime-injected views in the original Activity and original save/cancel code paths. If a module-owned activity is used, its result bridge and permissions must be proven to round-trip Tasker plug-in data exactly.

### 2.2 Domain home and navigation (P)

The shared module dashboard opens **AutoInput → Overview / Actions v2 / Legacy Actions / UI Query / Gestures / Screen Capture / Events / Root & Recording / Accessibility Health / Diagnostics / Advanced**. Context-sensitive Tasker editors open directly to the relevant item, not the entire dashboard. Offer **Enhanced Editor** / **Original Editor** switching; a disabled module always leaves original screens functional.

Global design rules:

- Modern Android 16-compatible layout, light/dark modes, edge-to-edge/inset handling, landscape/compact-phone support, IME/keyboard-aware action bar, responsive large fonts, TalkBack labels and sensible focus ordering.
- Searchable setting names, categorized help, breadcrumbs, favorites/recent fields, contextual explanations and an always-available read-only source/settings summary.
- **Save / Discard / Continue editing** with unsaved-changes dialog, correct Tasker `RESULT_OK`/cancel semantics, drafts on rotation/activity recreation, and no silent auto-save on Back or system gestures.
- Distinguish literal values, Tasker `%variables` and dynamic expressions; show unresolved variables as *runtime-dependent*, not syntactically invalid. No automatic expansion in an offline preview.
- All new module-only state must be in a versioned companion store with migration/rollback. Original plugin bundles and preference keys are untouched unless edited and saved through the verified original serializer.
- Preview labels: **simulated**, **accessibility snapshot**, **test execution**, **verified observed effect**. Never mark a mock screen as an actual device result.
- Support field-level reset/undo, keyboard navigation, long-press for explanations, and clear error locations; no surprise accessibility actions while editing.

### 2.3 Actions v2 — complete multi-step Action Studio (P)

**Goal:** replace fragmented editing with a visual, step-by-step workflow while retaining the underlying `InputActionv2` model.

- Scrollable **step cards** with order, action kind, target, timing, preconditions, postcondition, estimated matched nodes and last-test result. Collapsible detail prevents an unmanageable long page.
- **Add / Duplicate / Reorder / Remove** with undo, drag handles plus accessibility alternative; serialize through validated original action order. Explicit warning for any action type the enhanced editor does not yet understand; preserve unknown fields byte-for-byte, open original editor rather than deleting them.
- **Action kind selector:** click/long click, text entry, element/action matching, scroll/swipe, gesture and other **currently supported** original types, determined from actual runtime descriptors. Do not invent an action type the original APK cannot serialize.
- **Target mode:** resource ID, visible text, content description, accessibility match, nearby text, coordinates and stored selectors as supported by that action; clearly show resolution fallback order and selector ambiguity.
- **When to run:** show current-app restriction, exclude AutoInput/Tasker, text must be present, wait/pre-delay and condition timing already supported. Visual boolean editor only where translatable to the existing model.
- **Advanced controls:** existing `InputAdditionalOptions` items such as `checkMs`, `preActionDelay`, `separator` and `withCoordinates`; group them under **Timing**, **Matching**, **Output** with effective-value explanations.
- **Execution test:** user-triggered dry inspection (no side effect) versus live test (explicitly confirmed), with progress and per-step status. Display before/after accessible state only with user choice and redaction.
- **Debug timeline:** request → service/window ready → target resolved → dispatch → callback → postcondition → result. If a step fails, show *the first actual failing boundary*, not a generic “AutoInput failed”.
- **Safety:** annotate non-idempotent operations; no automatic repeat of submit, purchase, delete, send or other irreversible actions.

### 2.4 Legacy Action — complete redesign with original-format compatibility (P)

- Replace Easy/Manual/Stored/Advanced/Repeat nesting with **Target → Action → Timing & repetition → Preview → Advanced** navigation.
- Preserve legacy modes and original saved keys, including field type, field ID, write text, nearby text, stored action, screen check, accessibility option, repeat count/interval.
- Offer a **live locator assistant** with app/window context, selector matches, and a clearly labeled coordinate pick option.
- **Action summary** shows the exact action that the original format will execute; validate negative coordinates, absent target, bad numeric timing and impossible repeat count without rejecting legitimate Tasker runtime variables.
- **Repeat builder:** display count, interval, stop condition and potential side effects; bounded/safe diagnostics, no silent semantic rewriting.
- **Stored actions:** searchable entries and explicit import/reference behavior based on existing saved-action support. Never move or delete original entries automatically.
- Any unsupported historical option stays reachable through **Original Editor**.

### 2.5 UI Query — interactive element inspector and query builder (P)

- **Window explorer:** foreground app, package, window title/type where provided, display ID, rotation, bounds and accessibility availability. Show the difference between accessible nodes and root window metadata.
- **Hierarchy tree:** expandable nodes with text/content descriptions *redacted by default*, resource IDs, class, clickable/enabled/visible, bounds and match count. User may temporarily reveal content in a local session; no default persistence.
- **Tap-to-select** node, searchable attributes and path suggestion. Provide one-click field transfer to the original query preferences, not a new incompatible selector dialect.
- **Visual condition builder:** package restriction, exact/sub-string/regex tests, only visible, only clickable, wait-for-text, plus existing filtering semantics. Show zero/one/multiple matches and why an item was excluded.
- **Regex tester** on a user-provided sample with regex error location, case flags and exact match/group preview. Avoid silently normalizing regex.
- **Tasker output mapper:** show expected output variable keys, arrays/order, empty/missing/redacted fields and limits, using the actual current action schema.
- **Live refresh** with bounded polling, pause/freeze snapshot, displayed data-source time and UI-thread throttling. Never run unbounded tree traversals during Tasker execution.
- Accurate diagnostics such as `NO_ACTIVE_WINDOW`, `TARGET_APP_NOT_FOREGROUND`, `NO_MATCH`, `MULTIPLE_MATCHES`, `STALE_NODE`, `WRONG_DISPLAY`, `ACCESSIBILITY_LIMITED` only when evidence supports them.

### 2.6 Gesture Studio — visual gestures, validation and replay (P)

- Canvas preview of intended tap, long press, swipe, drag, pinch and supported multipointer gestures; timeline with touch points and durations.
- Target display/viewport, rotation, density, magnification and system inset guides; real device coordinates distinguished from normalized percentages.
- Dedicated numeric editors for start/end positions, paths, durations, hold time and up to the original supported pointer count; constraints validated against actual accessible display metrics.
- **Collision warnings** for overlapping gestures, pending user touches, off-screen coordinates and system transitions. Track `dispatchGesture()` false, callback completed and callback cancelled distinctly.
- **Test playback** must be explicit, interruptible, and never automatically retry a potentially destructive interaction.
- Optional root fallback only for supported **simple** taps/swipes/key events after eligibility and postcondition checks; don't promise root shell `input` reproduces Android's full multi-touch gesture API.

### 2.7 Screen Capture Studio — screenshots, video and visual operations (P)

- Mode cards for **Screenshot / Pixel color / Palette / Image comparison / Video** where supported by original action; only show fields pertinent to selected mode.
- **Screenshot editor:** source display, path/destination, image size, crop rectangle with visual handles, aspect/rotation, screen-coordinate preview and file/permission checks.
- **Pixel/palette tools:** sampled pixel/palette preview, tolerance/format controls only where supported, color format clarity (HEX/RGB/alpha), clear out-of-bounds warnings.
- **Image comparison:** choose sample and source, preview crop and coordinate alignment, distinguish visually similar from asserted match.
- **Video editor:** duration, resolution, optional microphone audio, storage estimate, permission and lifecycle indicators, stop/release and output inspection.
- **Capture backend:** original MediaProjection, plus a separately named optional root `screencap` workflow. Do not deceive users about which backend runs or silently intercept/reuse MediaProjection consent tokens.
- **Explicit privacy controls:** capture only on demand or when a user-configured action runs; no screenshots/recordings in diagnostic logs by default; bounded retention, export and secure deletion options.
- Secure windows are not assumed capturable; display honest `content unavailable` rather than trying to bypass `FLAG_SECURE`.

### 2.8 Event configuration studio (P)

Unify the *presentation* but preserve distinct original Tasker event schemas:

| Event editor | New UX | Strict compatibility requirement |
|---|---|---|
| Input Action Event | Visual event type/target filter, debounce inspector, sample event preview | Original conditions and returned fields retained |
| UI Update Event | App/window restrictions, state-change preview, event frequency and false-positive explanation | No silently dropped callbacks |
| Key Event | Key picker, supported key-name/code preview, press/release differentiation, event exclusivity warning | Never intercept system/emergency keys without explicit supported action |
| Accessibility-triggered events | Service health and scope banner, event-time correlation, log preview | Don't invent extra privileges or bypass redaction |

Event throttling/deduplication must be opt-in and keyed on actual semantic identity. Never coalesce distinct user input events solely because they occur close together.

### 2.9 Root input, recording and advanced input-device editor (P/C)

- First validate the existing **Root Settings / Record Input / Playback** code paths on the installed 3.0.12 build. Historical compiled resources are not evidence they still work.
- If functional, expose input-device list, capabilities, selected device, coordinate calibration, playback speed/duration and **read-only event preview** with a redacted/safe event model.
- Allow user-selected recording only for a clearly scoped session; explicit Start/Stop, recording indicator and retention policy; never enable device-wide keylogging or invisible background capture.
- Low-level `/dev/input` operations, if ever needed, require independent consent, explicit device selection and real-device proof; do not blanket-enable them in the root helper.
- Offer root behavior choices: **Off / Diagnostics / Explicit eligible fallback**, with per-action eligibility gates and explicit fallback display in the timeline.

### 2.10 Accessibility health, settings and recovery center (P)

- State panel: **disabled by user**, **not bound**, **starting**, **connected**, **interrupted**, **shutting down intentionally**, **unknown**; source and last updated time for each state.
- Surface the app's existing **Accessibility in Foreground**, **Enable Just When Needed**, retry interval and action-error notifications with plain-language behavior descriptions, not duplicate settings.
- Boot timeline: locked boot, first unlock, app cold start, service bind, first reliable query/gesture. Explain why a post-boot action was delayed or refused.
- One-tap link to the real Android Accessibility settings and a user-approved recovery action where supported. Do not re-enable an intentionally revoked service in the background.
- Service/process permission dashboard, root availability, LSPosed scope/compatibility, SystemUI feature status, firmware fingerprint and precise failure category.
- **Troubleshooting assistant:** facts first, suggestions second. Avoid generic “restart” recommendations if a measurable failure boundary exists.

### 2.11 Shared diagnostics and UX quality bar (P)

- Filterable cross-domain timeline with AutoInput-specific request IDs, Tasker action/editor labels and stage timings; redacted by default.
- Distinguish **editor invalid**, **accessibility disconnected**, **target not found**, **gesture cancelled**, **root fallback not eligible**, **capture consent missing**, **SystemUI state not ready**, and **Tasker callback not observed**.
- Export only with explicit user action; diagnostic archive includes hashes/runtime fingerprint, hook map, severity/event codes and sanitized traces, not personal screen text.
- First-run explainers; accessible navigation; clear error recovery; inline help; undo; predictable back gestures; no clipboard copying without explicit user activation.
- Module-wide safe-mode banner and auto-disabled hook reason; preserve each domain's settings if another domain is disabled.

## 3. Action execution reliability and outcome verification

### 3.1 Cross-layer state machine

`RECEIVED → COMPATIBILITY_CHECK → SERVICE_READY → WINDOW_STABLE → LOCATOR_RESOLVED → INPUT_DISPATCHED → INPUT_CALLBACK → POSTCONDITION_EVALUATED → DONE / CANCELLED / UNCERTAIN / FAILED`

- Instrument existing `InputActionv2.performActions(Integer)` and legacy `IntentPerformAction.fire()` without replacing their core semantics by default.
- Correlate by action execution/request ID, not only timestamp or process PID; cap pending sessions and release references on cancellation/process death.
- For each step, record elapsed time, app/window ID, target match count, input method, callback result and explicitly observed postcondition.
- Native Tasker results remain unmodified by default. New diagnostic output variables can be considered only under an opt-in, schema-tested extension.

### 3.2 Smart waiting and safe recovery

- Prefer **event-driven wait for a matching window/element**; retain existing time-based delays and controls. If the app already provides a wait feature, instrument and clarify it rather than duplicating.
- Distinguish a temporary stale node from a permanent no-match; bounded re-resolution of safe read-only queries is allowed.
- **Never retry by default** after any input that might have produced a side effect but lacks a confirmed result. No automatic repeat of purchases, deletions, sends, submissions, security-related actions or password entry.
- Serialize *automation-owned* gestures only when explicitly enabled and safe; never capture or block global user input to make them pass.
- Handle Tasker cancellation and session lifecycle explicitly; a failure in the helper or diagnostics path must not silently execute an alternative action.

## 4. Root-assisted capabilities and constraints

| Feature | Root role | Safety boundaries |
|---|---|---|
| Input fallback | Narrowly approved shell `input tap/swipe/keyevent` for supported simple cases | Eligibility proof; no retries on uncertain previous dispatch; original accessibility preferred |
| UI Query diagnostics | Window/focus/display geometry and read-only service state | Root metadata ≠ full accessibility tree; no hidden text bypass |
| Screenshot alternative | Explicit shell `screencap` backend | User selection, no secure-window bypass, short-lived files |
| Boot/service diagnostics | Inspect process/service/user-unlocked state | No stealth permission regrant or Direct Boot promise |
| Input-device inspection | Read capabilities of user-selected device | No background key capture or generic event injection API |

**Root helper design:** authenticate the requesting UID/signature/module, allowlist operations and parameters, block arbitrary command strings, run off UI threads with timeouts, bound stdout/stderr, keep per-operation audit records, and preserve explicit permission revocation. Never expose a generic elevated shell endpoint to a Web Screen, exported app receiver or Tasker variables.

Root being present is **not** evidence that it is granted to the module, that input commands succeed under current SELinux policy, or that protected/locked surfaces can be accessed. Distinguish denied, unsupported and unavailable.

## 5. Conditional AxionOS SystemUI integration

**Purpose:** observe and optionally coordinate system-owned UI states for AutoInput. It is **not** a general input engine or a prerequisite for the editor revamp.

- Confirm live signatures and real behavior of the supplied QS/shade/keyguard class families; compiled static anchors include `com.android.systemui.qs.QSPanel`, `com.android.systemui.shade.NotificationShadeWindowView` and `NotificationShadeWindowViewController`.
- Read-only state signals: shade collapsed/transitioning/expanded, QS expansion status, current tile state where observable, keyguard visibility and input-ready transitions. Define actual state transitions from probes; do not guess them from class names.
- In AutoInput gestures, use state-aware waiting and correct geometry to reduce blind taps on Quick Settings/notification shade elements.
- Optional state-aware user-requested expand/collapse only if platform API/command is confirmed on the live ROM and accepted by tests.
- No SystemUI hook for ordinary JSON processing, Termux:Tasker communication or unrelated app element clicking; no global event interception by default.
- SystemUI hooks must be exact-version matched, individually disableable and recoverable if the system interface crashes. OTA change = automatically suspend incompatible SystemUI adapter while retaining app-only AutoInput operation.
- `system_server` hooks require a separate explicit approval and a proven unmet requirement. **No baseline scope in `system_server`.**

## 6. Verified candidate hook inventory

| Area | Confirmed AutoInput class / method | Planned role |
|---|---|---|
| Accessibility | `com.joaomgcd.autoinput.service.ServiceAccessibilityV2.onCreate()` | Lifecycle telemetry |
| Accessibility | `...ServiceAccessibilityV2.onServiceConnected()` | Readiness |
| Accessibility | `...ServiceAccessibilityV2.onAccessibilityEvent(AccessibilityEvent)` | Event observation with privacy guards |
| Accessibility | `...ServiceAccessibilityV2.onInterrupt()` / `onDestroy()` | State transitions |
| Boot | `com.joaomgcd.autoinput.service.BroadcastReceiverBootCompleted.onReceive(Context,Intent)` | Boot lifecycle correlation |
| Actions v2 | `com.joaomgcd.autoinput.actionv2.json.InputActionv2.performActions(Integer)` | Execution stages |
| Actions v2 | `...InputActionv2.getActions()` / `getAdditionalOptions()` / `getWhenToPerformAction()` | Read-only configuration insights |
| Legacy action | `com.joaomgcd.autoinput.intent.IntentPerformAction.fire()` | Execution correlation |
| UI Query | `com.joaomgcd.autoinput.intent.IntentUIQuery.fire()` | Query outcome and match diagnostics |
| Gestures | `com.joaomgcd.autoinput.gestures.OutputProviderGestures.execute(InputGestures)` | Dispatch trace |
| Gestures | `...OutputProviderGestures.getGestureDescription(InputGestures)` | Gesture layout/coordinate preview |
| Screen capture | `com.joaomgcd.autoinput.service.ServiceScreenCapture.onStartCommand(Intent,int,int)` | Capture lifecycle |
| Screen capture | `...ServiceScreenCapture.onImageAvailable(ImageReader)` / `onDestroy()` | Completion/cleanup telemetry |
| Editor | `com.joaomgcd.autoinput.activity.ActivityConfigActionv2.onCreate(Bundle)` | Enhanced configuration entry |
| Editor | `com.joaomgcd.autoinput.activity.ActivityInputActions.onCreate(Bundle)` | Action Studio |
| Editor | `com.joaomgcd.autoinput.activity.ActivityConfigPerformAction.onCreate(Bundle)` | Legacy editor |
| Editor | `com.joaomgcd.autoinput.activity.ActivityConfigUIQuery.onCreate(Bundle)` | Query inspector |
| Editor | `com.joaomgcd.autoinput.activity.ActivityConfigScreenCapture.onCreate(Bundle)` | Capture Studio |

**Important:** These are candidate hooks identified from compiled declarations. They must be verified against installed classloaders/overloads and actual method behavior before implementation. Avoid unversioned obfuscated helpers and broad `hookAllMethods` across security-sensitive pathways.

## 7. Consolidated milestones and delivery plan

**Not a chain of tiny overlays.** Every implementation milestone is one complete, cohesive change set with functionality, tests, docs, negative coverage and verified file inventory. Fix a failed artifact by correcting and revising that consolidated artifact before apply; never pile up P01/P02/HFxx compatibility patches.

| Milestone | Consolidated deliverable | Acceptance gate |
|---|---|---|
| **AI-F0 — Runtime provenance & diagnostic baseline** | Exact installed AutoInput/SystemUI identity, LSPosed/root runtime, hook compatibility probes, action/gesture/query and post-boot read-only trace, Tasker plugin round-trip fixtures | Confidently locate observed failure stages; no target app mutation; privacy-safe baseline |
| **AI-F1 — Complete AutoInput domain** | **All enhanced editors and UI/UX**, Action Studio, legacy editor, UI Query inspector, Gesture Studio, Screen Capture Studio, Event Studio, root/recording settings, service-health dashboard, action timelines, safe waiting, conditional root fallback, versioned settings, tests and docs | Every original option accessible; old Tasker tasks unchanged unless saved; no unsafe repeats; original editor fallback; static and native validations pass |
| **AI-F2 — Conditional AxionOS integration** | Only proven necessary SystemUI QS/shade state observations, optional supported root screenshot integration adjustments, exact ROM gating and crash recovery | Physical device proves improvement; no SystemUI instability; module-off/original behavior restored; no mandatory SystemUI dependency |
| **AI-F3 — Final hardening & release** | Performance, privacy, regression/security/negative tests, boot and lockscreen checks, UI accessibility acceptance, source inventory audit, signed shared-module release and recovery instructions | Verified hosted CI, distinct physical-device evidence and rollback acceptance. No skipped test counted as passed |

**Milestone rules:** If no SystemUI requirement is demonstrated, **AI-F2 is omitted** rather than filled with speculative hooks. Root features remain independent and off unless explicitly opted in. An individually incomplete UI screen does not satisfy AI-F1's **full UI/UX** scope.

### Native Devtool overlay contract (for implementation, not this roadmap)

When a repository/branch is selected, use `Mikeyphw/devtool` `main` and the **latest verified compatible native Launcher v3.1+**. Record exact launcher source/origin/branch/HEAD/tree/blob and overlay payload hashes, preflight, native overlay validation, atomic apply/commit and durable session/CI tracking. Discover the trusted target worktree; accept verified detached-HEAD launcher runtime but **named branch for target**. Preserve unrelated work and never reset/rebase/force-push/auto-merge.

Launcher execution presentation must use supported environment settings:

```sh
DEVTOOL_EXECUTION_PRESENTATION=plain
DEVTOOL_JOB_UI=plain
DEVTOOL_TEXTUAL_APPLY_HOST=0
DEVTOOL_TEXTUAL_EXECUTION_HOST=0
```

Disable automatic terminal clipboard copying; use real stdout/stderr, respect `NO_COLOR`, `CLICOLOR=0`, `TERM=dumb`. Do not fabricate flags or rewrite the native Devtool mechanism. Audit target and Devtool tests/gates/CI for obsolete version-specific scaffolding; replace obsolete checks with current, meaningful coverage. Deliver one native consolidated overlay per milestone with actual package integrity validation. After commit, resume the durable session rather than reapply.

Each *actual overlay* report must include repository/branch, starting HEAD/resulting commit, launcher/runtime identity, exact changed-file scope and checks updated/deleted with reasons, local/native results, PR/CI state, one verified runnable Termux command, limitations and remaining device/release acceptance. **This roadmap does not create an overlay or establish a repository/branch, so none of those delivery results are claimed.**

## 8. Acceptance and regression matrix

| Test domain | Required positive & negative coverage | Failure to avoid |
|---|---|---|
| Licensing | Original licensed AutoInput unchanged, module enabled/disabled, Tasker imports/exports | License bypass, broken authorization |
| Existing configuration | Old Actions v2 and legacy tasks open/save/cancel/round-trip without loss; Tasker dynamic variables retained | Changed serialization, unknown fields discarded |
| Full editor coverage | All screens and settings reachable in enhanced and original editors; search, keyboard, rotation, Back, 200% font, dark mode, TalkBack | Inaccessible controls, silent save |
| Actions v2 | Single/3/10 steps; missing, stale, duplicated targets; conditional wait and cancellation | Non-idempotent replays, wrong target |
| Legacy actions | Stored actions, repeat times/interval, nearby-text and coordinate modes | Legacy regression |
| UI Query | Regex/exact, 0/1/many matches, dynamic lists, redacted secure nodes, app background, focused wrong window | Leaked private content, fabricated matches |
| Accessibility | User disabled, service starting, intentionally just-when-needed, bind interrupted, process death, first unlock | Unapproved permission regrant or false readiness |
| Gestures | Completed vs cancelled, multipointer, user interruption, rotation, split screen, IME, magnification and other displays | Gesture overlap, false success |
| Root fallback | `su` granted/denied/revoked, broken helper, invalid display, ambiguous prior dispatch, SELinux denial | Duplicate tap, hidden privilege change |
| Input recording | Features absent/present; supported device enumeration; explicit start/stop and no key logging | Sensitive input capture |
| Screen capture | New MediaProjection consent/session, end-session cleanup, root backend, secure window, crop out of bounds, video abort | Token reuse, screenshot leakage |
| Events | Key, UI Update, Input Action under bursty real events and screen changes | Incorrect event deduplication |
| SystemUI | Exact AxionOS fingerprint; shade/QS lifecycle, keyguard, OTA mismatch and process crash/rollback | Crash loop or affecting unrelated UI |
| Boot | Before first unlock (capability check only), after first unlock, delayed service bind, cold process, Tasker actions | Running credential-dependent actions before unlock |
| Robustness | 100+ repeated test cycles, no sustained CPU/memory growth, bounded logs and payloads, module-off fallback | ANR, hot-loop, unbounded telemetry |
| Cross-domain isolation | AutoNotification/AutoTools/Termux:Tasker unaffected when AutoInput hooks fail | One domain disables all |
| Supply-chain & CI | Pin source/blob/payload, native Devtool validation, PR HEAD checks, negative tests and documented device evidence | Claiming CI == device acceptance |

### Required product acceptance (release-blocking)

1. Users can configure every supported AutoInput Tasker action/event through the new UI or a clearly accessible original-editor fallback without losing values.
2. Actions v2 and legacy Action errors are pinpointed to the actual observed failure stage, including cancelled gestures and ambiguous matching, rather than generic timeout claims.
3. Optional root fallback is independently controllable; no automatic repeated side-effecting actions or credential/secure-window bypass.
4. UI Query shows correct available nodes and match explanations; sensitive text is redacted unless explicitly, temporarily revealed.
5. Safe-mode/module-off returns the original AutoInput behavior; unexpected OTA mismatches disable incompatible modifying hooks.
6. SystemUI, if included, survives restart and invalid/unknown hooks safely; unrelated apps and the other three plugin domains remain functional.
7. Hosted CI, native overlay tests, and rooted physical Android 16 acceptance are separately evidenced; missing tests are not passes.

## 9. Reconciliation with earlier AutoInput suggestions — explicit disposition

Earlier `AutoInput_3.0.12_Deep_LSPosed_Audit.md`, `AutoInput_3.0.12_LSPosed_Root_SystemUI_Audit.md`, and the cross-plugin audit also proposed these edge cases. They are now explicit acceptance items:

| Earlier suggestion | Disposition | Acceptance boundary |
|---|---|---|
| Actions v2 / legacy editor, UI Query inspector, gesture designer, capture, event studio, service health, root-device UX | **Required AI-F1** | Full old Tasker action parity and original-editor fallback |
| Dynamic-window waiting, outcome verification, stale-node resolution, bounded recovery, event debouncing | **Required AI-F1 instrumentation**, behavioral changes opt-in and safe | Distinguish dispatched from completed action; never blind-repeat side effects |
| **Key-suppression empty-selection hazard** | **Required AI-F1 UI warning and recovery tests:** if the installed app's existing key filter treats *no selected keys* as *match all*, display a prominent warning before enabling suppression; include an escape/recovery procedure | Check actual installed semantics; do not intercept system/emergency keys or change saved filter values implicitly |
| **Burst classifier for accessibility events** | **Required AI-F1 diagnostics**, optional semantic coalescing | Count, classify and time event bursts first; retain ordering and distinct click/text/key updates; no silent global throttling |
| **Native `libautoinput.so` compatibility** | **Required AI-F0 compatibility test and AI-F3 regression**; direct native hooks conditional only if proven essential | Check ABI/library loading/version/classloader boundaries and observe crashes; never assume Java hooks cover native behavior |
| Root input/record-playback and selected input-device inspection | **Conditional opt-in AI-F1** based on device proof of existing root implementation | Explicit sessions, no hidden keylogging, no high-privilege generic input endpoint |
| Root screenshots; Quick Settings/shade state-aware controls | **Conditional AI-F2**, if measured necessary | Secure-window protections and ROM compatibility preserved; no blanket SystemUI hooks |
| Suppressing user consent, guaranteed unlock/secure screenshot, silent service permission grants | **Intentionally excluded** | Android security/consent boundaries |

The **key-suppression** and **native-library** rows are newly explicit relative to the previous final roadmap. These are *not* claims that either behavior is broken on the device. New tests should be consolidated into existing current behavior checks rather than creating obsolete handoff-specific gate chains.

## 10. Explicit exclusions and unresolved prerequisites

- No rebuilding AutoInput or AxionOS SystemUI, APK modification, licensing bypass or unsupported hidden API promises.
- No guaranteed automatic accessibility regrant, pre-first-unlock automation, password/PIN unlock, restricted-screen capture, global keylogging or unconsented device input recording.
- No assumption that every historical AutoInput Root resource is active or that a root shell command implements complex Android gesture semantics.
- No direct `system_server` modifications for the baseline; require proof and separate approval.
- **Before implementation:** establish the actual shared-module repository, named branch, existing worktree status, current Devtool launcher compatibility, user device's root/LSPosed runtime, AutoInput installed SHA and AxionOS SystemUI fingerprint. The provided APK may differ from what is installed.
- **Not yet performed:** runtime hook proof, on-device root tests, full source/decompilation verification, native Devtool overlay validation, PR/CI, signed build, device compatibility acceptance.

## 11. Reference materials

- Local uploaded audits: `AutoInput_3.0.12_Deep_LSPosed_Audit.md`; `AutoInput_3.0.12_LSPosed_Root_SystemUI_Audit.md`.
- Android AccessibilityService / dispatchGesture: https://developer.android.com/reference/android/accessibilityservice/AccessibilityService
- Android GestureResultCallback: https://developer.android.com/reference/android/accessibilityservice/AccessibilityService.GestureResultCallback
- Android MediaProjection: https://developer.android.com/media/grow/media-projection
- Android 14 target behavior (fresh per-session MediaProjection consent): https://developer.android.com/about/versions/14/behavior-changes-14
- Android 16 target behavior (edge-to-edge and predictive back): https://developer.android.com/about/versions/16/behavior-changes-16
- Android Direct Boot: https://developer.android.com/privacy-and-security/direct-boot
- Original AutoInput changelog / root feature history: https://joaoapps.com/autoinput/changelog/

## Final implementation recommendation

**Deliver AI-F0 for precise runtime evidence, then one complete AI-F1 feature-domain implementation containing the full AutoInput UI/UX redesign and reliable action/query/gesture execution layer, with controlled root opt-in. Add AI-F2 SystemUI adapters only when physically demonstrated to be necessary; finish with AI-F3 acceptance and release. All of this ships inside the ONE shared LSPosed module, never as a second AutoInput LSPosed APK.**
