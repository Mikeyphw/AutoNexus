# AutoTools — Final Consolidated Roadmap (One Shared LSPosed Module)

**Specification date:** 2026-11-16  
**Domain:** AutoTools 2.3.19 (`com.joaomgcd.autotools`, versionCode **197**, target SDK **36**)  
**Target environment:** Android 16 / supplied AxionOS SystemUI reference; LSPosed or compatible hooking runtime **must be verified on the actual device**  
**Destination:** **ONE installed LSPosed module APK**, with four independently switchable domains: **AutoNotification, AutoInput, AutoTools and Termux:Tasker**. This document is the complete AutoTools-domain specification, not a proposal for a second module.  
**Permitted mechanisms:** In-process LSPosed hooks, module-owned activity/UI, companion-local preview engines, an **optional shared restricted root helper** invoked via an existing approved root manager, and **conditional** shared AxionOS SystemUI hooks only if evidence requires them.  
**Prohibited:** Rebuilding, repacking, patching, resigning, replacing or license-bypassing the original AutoTools APK; silently mutating saved Tasker actions, settings, system permissions or browser security; a separate LSPosed/Magisk feature module; unrestricted root API for JavaScript or JSON.

**Document status:** A deliverable roadmap and acceptance contract. **No module implementation, live LSPosed hook, root/device test, Devtool overlay, commit, PR or CI validation is claimed.**

---

## 0. Source provenance, verified observations and evidence levels

| Input | SHA-256 / evidence | Supported conclusions |
|---|---|---|
| `AutoTools_2.3.19.apk` | `2890b63ed6fe24307716fc26d14d126bfbaa58fe49562295b58c9fef91c091e7` | Two DEX files; 33 `ActivityConfig*` class definitions; 23 named compiled `config_*` preference XML definitions; plugin receivers, JSON, Web Screens, Secure Settings, OCR, SSH and other class families |
| `System UI_16.apk` | `8e2d9452ca0e2473ff1574deefb610d40b94b1ecf7810a1efcb05745df720ee9` | AxionOS Android 16 reference with Quick Settings/shade classes; no assumption that the uploaded APK exactly matches installed SystemUI |
| Prior AutoTools deep audit | `AutoTools_2.3.19_Deep_LSPosed_Audit.md` | APK-based component, preference and method inventory |
| Prior JSON/UX audit | `AutoTools_2.3.19_JSON_UX_LSPosed_Audit.md` | Existing JSON Read/Write preference keys and proposed UI/semantic safeguards |
| Prior LSPosed + Root audit | `AutoTools_2.3.19_LSPosed_Root_SystemUI_Deep_Audit.md` | Root/privilege boundaries, optional SystemUI use, verified method candidates |

**Evidence key:** **V** = present in uploaded binary, previous APK-derived inventory or directly checked source; **P** = proposed feature or test; **C** = requires method-body research and physical-device evidence. Compiled classes do **not** prove the features currently work, nor that a named method has appropriate execution semantics for a hook.

Baseline technical findings (**V**):

- JSON Read and JSON Write have **33** and **37** compiled preference nodes respectively; these are *resource nodes*, not an exact simultaneous-visible-control count.
- Text editor has 55 preference nodes, Date 54, Sort Arrays 38, SSH 14, Regex 11, HTML Read 9; Web Screen and Dialog have especially important **runtime-generated** options.
- AutoTools declares Tasker action and condition receivers, services for execution, Web Screen overlay service and Secure Settings changers. Some exported Tasker components have no manifest-level permission, but this alone is **not evidence of a vulnerability**; caller validation and plugin interoperability require audit.
- The APK bundles third-party RootShell classes and references to the separate **AutoTools Root Add-On**. Their presence does **not** establish that the add-on is installed, needed, authorized or functional on Android 16.
- The installed root provider, LSPosed/Vector runtime, actual phone fingerprint, grants/AppOps, current crash history, license and **on-device UI rendering** are **unverified**. All critical hooks require installed-binary checks at implementation time.

## 1. One-module product contract and responsibility boundaries

1. **One package and module ID.** AutoTools is a feature domain in the same installed LSPosed module as AutoNotification, AutoInput and Termux:Tasker. Exactly one shared dashboard, shared root helper, common logging/schema/compatibility layer, crash recovery and signing/release process.
2. **Domain isolation.** Failure, upgrade or disabling of AutoTools must not disable the other three domains. Runtime hook installation and exception budgets are scoped to the AutoTools process and separately authorized optional layers.
3. **No APK mutation.** Preserve the legitimate licensed app, signature, component definitions, existing plugin actions, result variables, Web Screen presets and stored preferences. Saving a pre-existing Tasker action without user edits must preserve its execution semantics.
4. **Default original behavior.** Missing target class, unknown package version, broken view resource or runtime exception results in the original AutoTools editor/execution path remaining usable. Never interrupt Tasker because a preview/editor enhancement failed.
5. **UI-only changes are not execution changes.** Read-only preview engines must not call `Intent*.fire()`, write secure settings, launch SSH requests, fetch URLs, send commands or start overlays automatically while the user types.
6. **Root is not a JSON feature.** JSON, text and array processors run unprivileged within the AutoTools adapter/companion. Root is for narrowly approved device-state checks, privileges and diagnostics.
7. **SystemUI is NOT a baseline dependency.** Optional shared Quick Settings/shade adapter only when a concrete ROM-specific task needs it. No SystemUI hooks are needed for JSON, dialogs, data tools, OCR, SSH or ordinary Web Screen editing.
8. **Actual outcomes must be distinguished.** Request accepted, action dispatched, callback received, result returned and *system state actually verified* are distinct statuses. Neither a successful API call nor shell exit alone proves the requested effect.
9. **Shared storage schema is versioned.** Module-only settings/drafts use a versioned local store with backup/migration, foreign-key-safe action association and a no-op path when disabled. Never inject unrecognized keys into a legacy Tasker action bundle without explicit protocol proof.
10. **No credential or private content collection by default.** Event timelines log state/elapsed time/operation category, not JSON bodies, URLs with auth tokens, secrets, SSH responses, OCR text or Web Screen JavaScript content.

### 1.1 Execution layers

| Layer | Process / owner | Function | Default |
|---|---|---|---|
| AutoTools LSPosed adapter | `com.joaomgcd.autotools` | Configuration UI interception, Action/Condition tracing, narrowly scoped runtime improvements | Diagnostics first; no semantic changes |
| Module UI and preview libraries | Same installed module APK; injected where needed | Visual editors, local typed preview, read-only change summary, settings/diagnostics | Enabled only on matched binary |
| Shared restricted root helper | Module-owned interface to existing root provider | Approved read-only package/AppOps/window/status diagnostics and specifically approved state changes | Off/read-only until permitted |
| Shared AxionOS SystemUI adapter | `com.android.systemui` only if independently enabled | Quick Settings visible-state verification and narrowly scoped shade interactions | Off |
| `system_server` | Not required for baseline | No planned hook; only consider with independently documented necessity and approval | Off |

## 2. Full UI/UX redesign — navigation, behavior and visual language

### 2.1 Unified entry points and navigation (**P**)

**Shared module dashboard → AutoTools:** Overview · JSON Studio · Data Lab · Web Screen Studio · Dialog Studio · Secure Settings Studio · Networking/SSH · OCR/Media · System Controls · Tasker Diagnostics · Compatibility · Advanced.

**From Tasker:** The actual AutoTools configuration action/condition should open **directly into its corresponding enhanced editor**, not a disconnected companion home screen. Present a clearly visible **Enhanced / Original editor** switch and keep the original activity lifecycle/save path accessible.

**Edit flows:** New / existing action → inspect original keys and dynamic schema → enhance UI → validate → show preview & warnings → Save through original serialization, or Discard / Continue. No surprise auto-save on Back, predictive back, activity destruction, dialog dismissal, rotation or process death.

**Shared UI standards:**

- Responsive phones/tablets, portrait/landscape, dark/light theme, dynamic contrast, RTL, large font, TalkBack, touch targets, keyboard navigation, display cutouts, edge-to-edge and IME/keyboard insets.
- Search across **every nested preference**, anchored breadcrumbs, collapsible groups, quick navigation, starred/recent options, contextual explanations and searchable action types.
- Each field has a visible *source* (literal, Tasker runtime `%variable`, user sample, AutoTools default), type (string/number/boolean/array/object/secret), validation status, reset/undo, and original raw-field escape hatch.
- Sensitive fields show reveal/hide and avoid clipboard/logging. Diagnostics never silently disclose private data. Optional copy/export only on an explicit tap.
- Sticky Save and Cancel controls respect the existing Tasker result contract. Dirty-state checks, per-field changes, unsaved-state recovery, rotation-safe drafts and error focus are release-blocking.
- Show **Simulated / Sample / Actual execution / Verified observed** labels to prevent a preview being mistaken for a successful Tasker action or root operation.
- Strict read-only previews by default. Network or root tests require a separate explicit user action and security preflight.
- Unknown, obfuscated, deprecated or future dynamic fields must be preserved and remain editable in Original/Advanced mode; the revamp must not hide them permanently.

### 2.2 Full 33-editor inventory and required enhanced surfaces (**V** classes, **P** improvements)

All of the following **33** `com.joaomgcd.autotools.activity.ActivityConfig*` classes were verified directly from uploaded APK DEX. The classes establish the coverage inventory, not proof of every proposed behavior.

| Existing configuration class (suffix after `ActivityConfig`) | Required enhanced UI outcome |
|---|---|
| `AVRCP` | Media command selector, currently supported control list, capability preview and callback diagnostics |
| `AppShortcuts` | App/shortcut picker, resolved target preview, missing-shortcut warnings |
| `AssistantCondition` | State/condition explanation, event/condition preview, supported-input validation |
| `Badge` | Badge/launcher context, value preview and supported-launcher limitations |
| `BadgeCondition` | Condition matcher, actual vs requested badge status explanation |
| `ChromeCustomTabs` | URL/options validation, explicit browser preview/test, callback/fallback clarity |
| `Connectivity` | Guided connectivity action settings, effective-state inspection and permission diagnostics |
| `ConnectivityCondition` | Human-readable conditions, sample values, state-transition diagnostics |
| `Date` | Date/time format builder, timezone/locale preview, offset/date arithmetic validation |
| `Dialog` | Type-aware visual form builder, modal preview and result-state debugger |
| `Flashlight` | Capability/status readback, permission/device support explanation |
| `GesturesScreen` | Coordinate/gesture bounds visualization and dispatch result diagnostics, not a duplicate of AutoInput's input engine |
| `HTMLRead` | HTML source/selector inspector, existing Easy Setup augmentation, extraction preview |
| `Json` | **JSON Read Studio:** tree/path/filter/sort/aggregate/output designer |
| `JsonWrite` | **JSON Write Studio:** typed mutation rows and side-by-side diff |
| `Launcher` | Launcher target/state picker, supported-API warnings |
| `LogCat` | Log-filter builder, output privacy and version/permission diagnostics |
| `Math` | Expression editor, syntax/type/precision preview and clear errors |
| `MediaStateCondition` | State transition/condition preview and reporting |
| `Muzei` | Availability/status and supported integration editor; avoid claiming installed-provider support without test |
| `OCR` | Visual crop/rotation inspector and recognized-output preview with data protection |
| `Regex` | Pattern playground, flags, capture groups and match explanation |
| `Report` | Readable report configuration, output formatting and variable preview |
| `SSH` | Connection/profile editor, argument quoting, safe test and credential protections |
| `Sensors` | Sensor selector, availability/range and change/trigger preview |
| `SortArrays` | Typed array inspector, sort order/key/locale/number semantics and before/after preview |
| `SystemStates` | System-state/condition explorer, supported observation guidance |
| `Text` | Unified text transformation pipeline, nested-operation navigation and sample preview |
| `Toast` | Preview/settings/appearance and platform compatibility diagnostics |
| `Vibration` | Pattern editor with units, duration validation and explicit test control |
| `Wait` | Time/condition parameter validator and pending/finish state explanations |
| `WebScreen` | **Web Screen Studio:** source/presets/dynamic variables/overlay placement/debugging |
| `settings` | **Secure Settings Studio:** human-readable current/desired/effective state and root capability checker |

**Additional action-only adapters:** DEX contains `IntentHttp`, `IntentXmlRead`, `IntentShortenURL`, `IntentFingerprint`, `IntentFingerprintCondition` and other `Intent*` classes without a matching class in this **33 `ActivityConfig*` inventory**. The shared UI should expose relevant tracing/diagnostic affordances **only after locating their actual configuration entry points**; do not invent an activity hook for them.

### 2.3 Cross-editor Save/Cancel and legacy mapping (**P**)

1. Snapshot current preference values, nested screens, dynamic variables and original Tasker bundle before injecting controls. Use stable class/resource key identities and exact original serializer.
2. Legacy fields must round-trip; when editing one property, unrelated bytes/values/unknown future fields must be retained (where source serialization format permits).
3. Validate only supported fields; Tasker `%variable` placeholders are *runtime dependent* rather than automatically erroneous; never eagerly resolve them using personal Tasker data.
4. Reopen a saved action in enhanced editor, Cancel and verify no changes. Save with no edits and verify semantically identical action result. Save with one edit and compare field-level diff, preserving existing reserved variables and dynamic metadata.
5. Provide safe Original Editor fallback for method mismatch, unsupported dynamic schemas, invalid stored configs or module unavailability.
6. Module-only presets/drafts remain local and explicit; never auto-rewrite old Tasker profiles to new storage.

## 3. JSON Studio — COMPLETE Read + Write overhaul (**P**, highest UX priority)

### 3.1 JSON Read — Source → Explore → Transform → Output

- **Source:** Paste/test JSON with syntax highlighting, formatting and precise line/column errors; distinguish literal sample from runtime `%variable`; show object/array roots and bounded size/depth budgets.
- **Sources:** Existing JSON text and supported URL/auth inputs stay supported. Never fetch a URL during typing; explicit test button with network origin, timeout and sanitized error display.
- **Tree explorer:** Expand/collapse, search keys/values with privacy guards, type badges, array index/length, node count, path breadcrumb, copy/select node with user action.
- **Path builder:** Generate **AutoTools-supported path dialect**, not generic unvalidated JSONPath; suggestions driven by sample and actual parser, highlighting ambiguous fields in Simple Mode.
- **Filter studio:** Existing exact/regex/case-insensitive/contains rules mapped to visual operator chips; show filter order, matching nodes, regex syntax errors and empty-result explanations.
- **Transform studio:** Sort order/type, aggregate sum/average/min/max where supported, separator and array flattening/shape diagnostics; show numeric type or invalid-value warnings.
- **Output mapping:** Preview exact Tasker output variable names, scalar vs array shape, expected `%atjsonpaths()` (when supported), missing/null/empty distinctions; show actual sample-derived result **and whether any runtime placeholder remains unresolved**.
- **Advanced tab:** Preserve all original raw preferences and parser behavior; toggles can revert UI enhancements without changing result semantics.

### 3.2 JSON Write — Input → Operations → Validate → Compare → Output

- Parse the starting JSON into an editable **tree preview**, leaving the actual original input unchanged until Save.
- Support visual rows for **simple values, append to array, add object to array, update, delete**, with a clearly defined order matching the existing writer.
- Pair values and keys visually instead of making users maintain parallel text fields. Validate mismatched `JsonKeys`/`JsonValues`, `UpdatePaths`/`UpdateValues` and other paired fields, accounting for runtime-variable unknowns.
- Type-aware inputs for string, number, boolean, JSON null, arrays, objects; offer explicit conversion but never silently change legacy coercion.
- Show before/after tree and diff: added/changed/removed nodes, affected-match counts, proposed output, JSON pretty mode and output size.
- Validate path syntax, filter matching and array indices; distinguish error, no match, unsupported operation and runtime-dependent expression.
- Support undo/redo/reorder in preview only if serialization can represent it exactly. Otherwise label the affordance unsupported and retain advanced original entry.
- Confirm compatibility with the **exact legacy key spelling `FilterValye`**; do NOT rename or migrate it silently.
- Avoid invoking `IntentJsonWrite.fire()` as a preview: it can use application data and trigger Tasker output effects. Use a verified isolated parser/test engine with side-effect-free controlled fixtures.

### 3.3 Correctness improvements (conditional **C**, not assumed defects)

- Missing key vs `null` vs `""` vs `[]` vs `false` vs `0`, numeric precision, Unicode/escapes, deeply nested inputs, duplicate-name ambiguity, filter precedence, regex errors, sort and aggregate semantics.
- Differential-test new engine against actual AutoTools methods with deterministic fixtures; retain source behavior by default even where surprising.
- Any true engine fix must be independently toggled and narrowly hook the proven parsing/mutation boundary; old actions remain unchanged until explicitly opting into corrected semantics.
- Detect Binder/Tasker output size limits, huge input/cancellation and memory exhaustion. Never claim unbounded JSON results.

## 4. Web Screen Studio — complete UX and reliability (**P/C**)

**Existing features to extend, not recreate:** preset import/save/delete/refresh, auto-generated variable inputs from `<meta name="autotoolswebscreen" ...>`, injected JavaScript/HTML, direct commands, local/remote sources, overlay/dialog/window modes, and `autoToolsUpdateValues(values)` updates.

**Complete Studio UI:**

1. **Source & trust:** local path vs HTTPS/HTTP URL, resolved source, read-only fingerprint, remote trust/cleartext warning, explicit remote fetch test, preset origin/version, missing-file diagnostics.
2. **Preset manager:** search, select, compare and inspect existing presets; import/export only on user action; safe duplicate/rename flows; external resources never given root authority.
3. **Dynamic variable form:** read the actual Web Screen variable schema; group by metadata `group`, show title/description/hint/type/default, handle switches/options/icons/images/colors/files, and retain unsupported new metadata via Original Editor.
4. **Visual layout designer:** actual supported overlay/window/dialog/normal configuration, size, gravity, margins, offsets, orientation, focus/touch/keyboard/inset modes, visibility and screen transitions. Show which settings are subject to Android security/window ordering restrictions.
5. **HTML/JavaScript editor:** source preview, format/syntax linting and diagnostics. Keep JavaScript execution within existing WebView security context; do not expose root APIs or blindly enable additional WebView bridges.
6. **Preview modes:** *simulated* static preview, *explicit real WebView test* (side effects and network disclosed), and *actual Tasker execution trace*. Never treat a preview as proof that an overlay will appear after boot.
7. **Update testing:** check whether `autoToolsUpdateValues` exists, preview variable payload, show update callbacks and compare in-place update vs full refresh. Do not fabricate support for pages without callback.
8. **Lifecycle inspector:** source resolved → WebView loaded/error → JS initialized/exception → overlay requested/attached/visible/updated/dismissed; include timestamps and sanitized failure reason.
9. **Recovery controls:** user-approved refresh/re-create only when safe; no infinite auto-retry, hidden persistent overlay or background-activity restriction bypass.

**Key limitation:** App overlay windows remain below critical system windows. SystemUI is not a general Web Screen overlay rendering engine; no unconditional global shade or keyboard hooks.

## 5. Dialog Studio — complete dynamic configuration overhaul (**P**)

- Discover the selected **actual** dialog type and its runtime-generated settings; do not assume the short `config_dialog` XML is the complete form.
- Provide a guided layout for title/body, choices, icons, colors, actions, buttons, list/grid, form fields and timers **only where the active dialog type supports them**.
- Type-dependent visibility (no empty irrelevant cards); preview readable size/scrolling/keyboard, accessibility focus and light/dark/landscape states.
- Distinguish completed, positive, negative, cancelled, dismissed, timed out and missing callback using **the original result contract**, not invented Tasker variables.
- Validate matching button labels/options, list indices, regex patterns, number/date formats and runtime placeholders as supported; error messages cite the actual offending field.
- Preview has no implicit external side effects; user can explicitly test a dialog with a rollback/dismiss control.

## 6. Data Lab — all transformations and parsers (**P**)

| Family | Design | Correctness and diagnostics |
|---|---|---|
| **Text** | Pipeline cards for actual supported operations; search across 55-node nested preferences; input/output side-by-side | Unicode, whitespace, separators, empty/null, Tasker variable behavior, large text |
| **Regex** | Live pattern field, flags, matches highlighted, group names/indexes and test fixtures | Invalid patterns, zero matches, overbroad capture, pathological regex runtime budgets |
| **HTML Read** | Source viewer, select element/path, retain built-in Easy Setup, preview extracts | CSS/selector dialect verification, URLs and encoding; never assume browser-like DOM support beyond tested engine |
| **XML Read** | Inspect actual configuration and parser entry point before new editor; tree/query/result preview if present | Namespace, attributes, repeated nodes, entities, malformed XML; no external entity expansion in companion preview |
| **Sort Arrays** | Array grid, sort key/direction/type/locale preview, before/after comparison | Stable ordering, nulls, mixed types, empty arrays, repeatability |
| **Date** | Locale/timezone preview, supported token palette, date math and formatting | DST, ambiguous times, leap days, epoch units, invalid formats |
| **Math** | Expression editor, operator/function guidance from actual engine | Precision, divide-by-zero, overflow, invalid expressions |
| **OCR** | Image/crop/rotation visualization, test-on-sample, result structure display | Empty/partial reads, permissions, large image, multilingual uncertainty; redact private image content |
| **Sensors** | Device-supported sensors and measurement-unit cards | Availability, no data, update cadence, sensor permission/state |
| **Report** | Preview human-readable summary based on actual configuration | Output variable size, formatting, blank fields |

**Explicitly scoped extras:** HTTP/URL shortening and action-only XML/HTTP classes can receive diagnostics and a UI adapter if their true Tasker configuration surface is identified. Do not fabricate supported options from class names alone.

## 7. System Controls and Secure Settings Studio — root-aware (**P/C**)

### 7.1 Secure Settings Inspector

- **Capability inventory:** `WRITE_SECURE_SETTINGS` actual grant, overlay permission, notification permission, relevant AppOps, root availability, requested action and OS capability.
- **Change preview:** old value → requested value → observed value; identify which service or state is authoritative; explain unsupported/normalized/rejected requests.
- **Guided controls:** secure/global/system settings, animation scales, orientation, immersive/fullscreen, low power, accessibility services, notification listeners, Quick Settings tiles **where already implemented in AutoTools**.
- **Transactional testing:** explicit consent, minimal reversible operation, read-back verification, show whether restore can be safely performed. Do not treat `settings get` success alone as system behavior confirmed.
- **Safety:** user-revoked permissions/accessibility remain revoked until user reauthorizes; no silent grants, device security relaxation or cross-user modifications.
- **Root Add-On:** detect official `com.joaomgcd.autotoolsroot` if installed, then compare functional overlap before adding any root-facing UI. It is not an assumed dependency.

### 7.2 Other system and integration editors

- Connectivity actions/conditions: requested vs observed Wi-Fi/Bluetooth/mobile-network and app permissions, within supported Android APIs.
- Flashlight: device camera/torch availability, state and failures.
- App shortcuts/launcher/badges: package/shortcut target preview, missing app/launcher restrictions and callback delivery.
- System states/media-state/assistant conditions: event/source visibility, actual vs requested state, privacy-preserving event inspector.
- Chrome Custom Tabs: URL validation, browser availability and explicit test only.
- Vibration/toast/wait: timing controls and clear state feedback, with explicit tests that won't accidentally alter active Tasker tasks.
- LogCat and diagnostics: Android permission restrictions made visible; redact logs; root collection explicitly authorized and bounded.
- AVRCP/Muzei: editor support and environment detection conditional on the real installed target/media provider.

## 8. SSH, remote data and privacy-sensitive operations (**P**)

- **SSH:** host/port/profile, existing auth method controls, display/hide secrets, host-key identity warnings, quoted argument preview and staged connection diagnostics (DNS → TCP → SSH → host auth → user auth → execution/result). No password/keys in logs, no automatic trust-on-first-use without disclosure, no root tunneling to bypass remote identity checks.
- **HTTP/network:** when existing action is present, show URL scheme, redirects, status and timeout category, output limits and explicit test control. No silent requests from preview, no widening of cleartext/certificate policy.
- **External file access:** respect Android scoped storage, file permissions and per-user grants; root is not a substitute for ordinary consent unless user explicitly initiates a suitable approved read.

## 9. Tasker IPC, action/condition reliability and boot readiness (**P/C**)

### 9.1 Trace protocol

`Tasker invoked → Android delivery attempted → receiver entered → valid bundle decoded → selected action/condition → service/worker started → result or pending acknowledgement → completion delivered → Tasker host observed`.

Hook **both** `FIRE_SETTING` and `QUERY_CONDITION`; they have different semantics, especially polling and synchronous returns. Track IDs, process state, boot/unlock state, elapsed times and failure classification, never record payloads by default.

**Important:** The previously reported intermittent *Termux:Tasker* message **“plugin itself didn't respond”** is not proof that AutoTools has the same failure. Shared infrastructure can correlate issues across domains, but AutoTools fixes require independent reproduction.

### 9.2 Recovery safeguards

- Separate **non-delivery**, receiver cold-start, failed decoding, execution refusal, pending result lost and host completion delivery failure; only recover confirmed stage failures.
- Never retry an action if it may already have executed with side effects (SSH command, Secure Settings change, remote HTTP request, dialog submission). Use idempotency-safe replay only with explicit proof and opt-in.
- Before first unlock: classify credential-encrypted state and queue/defer only when request and host contract safely support it; never promise Direct Boot operation by Java hooking alone.
- Root may read bounded package/process/service state, never force-stop/restart Tasker/AutoTools or wipe app data without explicit authorization.

## 10. Root helper and optional SystemUI scope (**P/C**)

### 10.1 Root within the single module

The root helper is **shared, packaged/controlled by the same module and not an independently installed LSPosed/Magisk module**. It may use an existing authorized `su` provider. It is not assumed enabled merely because the phone is rooted. Separate permission grants for the helper must be opt-in.

Allowed categories: bounded package/AppOps state, specified settings readback, WindowManager/service summary, user-approved reversible privileged settings operation, sanitized log snapshots. Each operation is allowlisted with argument validation, caller identity/authentication, timeout, rate limit, audit receipt and cancellation.

**Forbidden:** generic root command endpoint, arbitrary shell strings from WebView/Tasker JSON, reading private keys or credential stores, unbounded screenshots, access to protected fields, silent permissions changes or escalating external HTTP content into root commands.

### 10.2 SystemUI (shared adapter; default OFF)

Only conditionally for **Quick Settings visual-state verification**, shade context, and a verified AxionOS-specific mismatch that cannot be observed with ordinary APIs. Must exact-pin the installed SystemUI fingerprint and hook method signature, measure stability and support safe disable without a functioning shade.

**No SystemUI dependence** for JSON, Text, Dates, Regex, OCR, SSH, Web Screen source/editor, Dialog UI or Tasker plugin IPC. No default `system_server` scope.

## 11. Verified LSPosed hook candidate inventory (**V** method names; **C** working semantics)

| Domain | Binary classes/methods to investigate | Hook mode |
|---|---|---|
| JSON Read | `ActivityConfigJson.onCreate(Bundle)`; `IntentJson.fire()` | UI after-layout injection; read-only entry/exit timing |
| JSON Write | `ActivityConfigJsonWrite.onCreate(Bundle)`; `.isResultValid(IntentTaskerPlugin)`; `IntentJsonWrite.fire()` | UI/validation observation; optional evidence-driven exact fix |
| JSON parser | `JsonReaderFactory.get(...)`, `JsonReaderSimple.read(...)`, `JsonReaderComplex.getPathList(String)`, `JsonReaderBase.onNullValue(...)` | Research and differential tests before any semantic hook |
| Web Screens | `ActivityConfigWebScreen.onCreate(Bundle)`, `.isResultValid(IntentTaskerPlugin)`; `ActivityWebScreen.onCreate/onResume/onPause/onDestroy`; `ServiceOverlay.onStartCommand(Intent,int,int)`, `.onDestroy()` | UI injection and lifecycle tracing |
| Dialogs | `ActivityConfigDialog.onCreate(Bundle)`; `IntentDialog.fire()` | UI injection; outcome diagnostics |
| Secure Settings | `ActivityConfigsettings.onCreate(Bundle)`, `.isResultValid(IntentTaskerPlugin)`; `OutputProvidersettings.execute(Inputsettings)` | UI and effective-result diagnostics |
| IPC action | `com.joaomgcd.common.tasker.BroadcastReceiverFire.onReceive(Context,Intent)`; `com.joaomgcd.autotools.broadcastreceiver.BroadcastReceiverFire.getIntentFactory(Context)` | Preserve parent/child behavior; low-overhead trace |
| IPC condition | `com.joaomgcd.common.tasker.BroadcastReceiverQuery.onReceive(Context,Intent)`; AutoTools subclass `getIntentFactory(Context)` | Distinct query semantics/trace |
| Service dispatch | `com.joaomgcd.common.tasker.IntentServiceFire.onHandleIntent(Intent)` and AutoTools long-running service class family | Observe only until actual response path mapped |
| OCR / Secure Settings | `OutputProviderOCR.execute(InputOCR)`, `OutputProvidersettings.execute(Inputsettings)` | Diagnostics, not silent rerun |
| Data actions | `IntentRegex.fire()`, `IntentSSH.fire()`, `IntentOCR.fire()`, `IntentJson.fire()`, `IntentJsonWrite.fire()` | Timing/error metadata, original return untouched |

**Hook verification gate:** fully qualify and confirm method overload/parameter/return types from *installed* APK and classloader; check obfuscation/version drift; no broad catch-and-swallow wrappers that conceal original crashes; fail back to original function. No speculative method names or arguments accepted for implementation.

## 12. Consolidated milestone strategy (no micro-overlay chain)

These milestones are **consolidated acceptance boundaries**, not an invitation to split the editor into dozens of sequential fix packages. The **complete AutoTools domain including all 33 editor families** belongs in its full-feature milestone.

| Milestone | Single consolidated deliverable | Mandatory exit gate |
|---|---|---|
| **AT-F0 — Preflight / evidence** | Identify actual shared-module repository/branch, installed binaries and runtime, exact metadata and DEX fingerprints, root provider/add-on status; read-only IPC/overlay/setting probes, full editor screenshots and legacy bundle fixtures | Exact provenance and working hook probes; source and device evidence separated |
| **AT-F1 — FULL AutoTools domain** | One complete integrated feature change set: **all configuration UX families**, JSON Read/Write Studio, Web Screen Studio, Dialog Studio, Data Lab, Secure Settings Studio, SSH/OCR/system-control editors, per-action diagnostics, safety controls and app-side reliability fixes demonstrated by tests; native tests/docs together | 33-editor coverage matrix complete, original Save/Cancel and Tasker bundles round-trip, no unexpected side effects, goldens/differential tests pass |
| **AT-F2 — Conditional root/SystemUI adapters** | Only additional necessary, approved privileged readback/recovery and/or AxionOS Quick Settings hooks that could not be handled in app; independently switchable and covered by negative/recovery tests | Permission isolation, actual effective state, safe uninstall/disable; SystemUI crash-free physical testing if enabled |
| **AT-F3 — Shared release qualification** | Security, accessibility, performance, boot, compatibility and crash-loop testing; signed single-module APK; reproducible hashes; hosted CI on exact PR HEAD; runtime acceptance and rollback docs | No regressions in any of four domains, CI and real-device passes separately evidenced, source/provenance/receipts preserved |

If a stage fails, repair the underlying implementation and revise the **same consolidated artifact**, not an expanding sequence of emergency/obsolete version-specific patches. Read-only probes are the only separately justified preparatory artifact if required to locate source-specific hook boundaries.

## 13. Comprehensive test and negative coverage

### 13.1 UI, accessibility and configuration

- All **33 verified** configuration activity families: new/edit/reopen/Save/Cancel/Back, nested-screen navigation, dynamic options, raw legacy fallback, landscape/portrait, font 200%, dark/light, TalkBack, RTL, IME inset and process recreation.
- Persist an untouched old Tasker action and compare serialized output semantics before/after enablement, and on editing one field. Unknown keys retained; especially `FilterValye` and generated Web Screen variables.
- No silent network test, root action, data capture, screenshot, overlay display, dialog confirmation or Secure Settings write during editor preview.

### 13.2 Data and output correctness

- JSON: null/missing/empty, mixed types, big numbers, Unicode, escape sequences, arrays/indexing, nested objects, filters/regex, sorting and aggregate edge cases, stale placeholders, malformed input, duplicate field names and huge payloads.
- JSON Write: mismatched parallel fields, multiple operations/order, unintended deletion, reversible preview, post-save result, variable names and pretty output.
- Text/Regex/HTML/XML/Date/Math/Arrays: documented original semantics, locale/DST, malformed expression, special characters, excessive regex time and deterministic results.
- Preview vs original action on controlled fixtures: exact result shape/variable names and error classes, with no new side effects.

### 13.3 Web Screens, dialogs and external integrations

- Preset source local/remote, no network/404/SSL error, JavaScript exception, missing update callback, in-place update vs rebuild, overlay permission absent, focus/IME, rotation, status bar/QS overlap, background activity denied and process kill/restart.
- Dialog: each supported dynamic type, back/dismiss/cancel/positive/negative, timeout/late callback, focus and keyboard accessibility.
- SSH: wrong host/port, DNS offline, host-key mismatch, auth refused, timeout, cancellation, output/truncation and no secret leakage.
- OCR: image absent, cropped/rotated, memory pressure, blank result, user content privacy and permission revoked.

### 13.4 IPC, root, boot, safety, shared-module behavior

- Action and condition requests traced separately through immediate/pending results; two simultaneous tasks, polling storms, service cold start, process eviction, before unlock/after unlock, host/plugin restarts, no duplicate execution.
- Root unavailable, root denied, stale grant, malformed IPC, untrusted Web Screen JS, evil URL/file path, wrong caller UID, oversized logs, Android OS permission changed; helper must reject safely.
- Optional SystemUI adapter: wrong ROM/DEX, hook absent/renamed, QS toggles, screen unlock, recovery without UI, no crash loop, no global behavior change.
- Four-domain isolation: disabling AutoTools leaves AutoInput, AutoNotification and Termux:Tasker running; AutoTools failure cannot break the common module or Tasker integration.
- Stress and operational cases: repeated screen openings, ~500 mixed action/condition events, slow helper, storage full, low-memory kills, hook invocation timing bound and safe disable mid-session.

### 13.5 Release-blocking acceptance checklist

1. One installed LSPosed module and exact plugin/ROM compatibility; **original AutoTools APK + license unchanged**.
2. **Complete 33-editor UX coverage**, including unsupported-original-field fallback and original legacy navigation.
3. JSON Read/Write available as visual studios with type-aware path/mutation controls, sample previews, before/after diff and proven preservation of old Tasker output semantics.
4. Web Screen, Dialog, Secure Settings, Data Lab, SSH/OCR and system-control UX changes work or declare a **specific, evidenced limitation** instead of silently hiding an editor.
5. No unintentional root or network calls, no secrets in logs, original permissions/consent respected, no deceptive result statuses or double-executed side effects.
6. No SystemUI dependency for normal AutoTools operations; optional adapter separately tested and reversible.
7. Native unit/regression/negative/overlay validation, hosted PR CI and physical Android 16 acceptance all reported **separately**, not conflated; skipped tests never called passes.
8. Safe recovery/uninstall and feature-level disable documented; no unnecessary obsolete gates or brittle handoff-specific scripts.

## 14. Native Devtool v3.1+ delivery contract (for future implementation)

Before any implementation overlay:

1. Identify the real shared module repository, **named target branch** and worktree. Inspect `Mikeyphw/devtool` **`main`** for its latest verified compatible native launcher **v3.1+**, including real runtime/head/tree/blob/payload identity and launcher contract. Detached HEAD is allowed only for a verified runtime checkout, not an unnamed target branch.
2. Validate source/payload hash inventory, preflight and native Devtool overlay validation before applying. Deliver through the native launcher for **atomic apply/commit**, durable session/status/resume, and separately approved push/PR/CI watching. Never replace it with manual patching, file copying, ad-hoc Git application or a homemade overlay engine.
3. Native terminal output only:

   ```sh
   export DEVTOOL_EXECUTION_PRESENTATION=plain
   export DEVTOOL_JOB_UI=plain
   export DEVTOOL_TEXTUAL_APPLY_HOST=0
   export DEVTOOL_TEXTUAL_EXECUTION_HOST=0
   ```

   Respect `NO_COLOR`, `CLICOLOR=0`, `TERM=dumb`; do not auto-copy commands/logs to the clipboard. Do not suppress real diagnostics. Use only verified launcher commands/flags; do **not** invent a launcher or a generic direct command before the actual package and arguments are known.
4. Audit affected unit tests, version-specific or obsolete gates, CI workflows, discovery and docs; consolidate redundant coverage, remove genuinely obsolete scaffolding **only while preserving real security/regression assurances**.
5. No reset, rebase, force push, auto merge, dirty-worktree overwrite or unrelated work modifications. Native session resume after committed apply (status/deliver/ci), **never reapply** an already-applied overlay.
6. Per-overlay report must give repository, branch, starting HEAD/resulting commit, launcher version/runtime identity, changed-file list, checks updated/consolidated/deleted and reasons, local/native validation, actual PR HEAD/hosted CI state, **one verified ready-to-run Termux command**, and limitations/device acceptance steps.
7. If validation, the native launcher or contract fails, **retain and ask for the full launcher + Devtool run logs** rather than disguising the failure or weakening validation.

This roadmap did **not** access or mutate the repository and therefore has **no genuine launcher/branch/commit/PR/CI state to report**.

## 15. Reconciliation with earlier AutoTools suggestions — explicit disposition

Cross-checked with `AutoTools_2.3.19_Deep_LSPosed_Audit.md`, `AutoTools_2.3.19_JSON_UX_LSPosed_Audit.md`, and `AutoTools_2.3.19_LSPosed_Root_SystemUI_Deep_Audit.md`:

| Earlier suggestion | Disposition | Acceptance boundary |
|---|---|---|
| Full 33-editor UI/UX, JSON Studio, Data Lab, Web Screens, Dialogs, SSH/OCR, Secure Settings and action/condition diagnostics | **Required AT-F1** | Existing Tasker keys/outputs unchanged without explicit user edits; legacy editor remains accessible |
| JSON types/null/missing, indexed arrays, complex filters, sorting, aggregation and Write operations/diff | **Required AT-F1** | Differential fixtures against actual AutoTools behavior, no side effects during preview |
| Web Screen source trust, JS errors, overlay lifecycle and dynamic source variables | **Required AT-F1** | Local/HTTP/HTTPS origin labels, explicit remote test, JS error classification, original `autoToolsUpdateValues()` preserved |
| Android 16 UI insets, background execution errors and **JobScheduler/WorkManager quota visibility** | **Required AT-F1 diagnostics**, root metadata optional | Actual launch/defer/error evidence; no generic background bypass |
| **Exported Tasker component caller-contract audit** | **Required AT-F1/AT-F3 negative tests** of harmless malformed/wrong-caller intents, action names, bundles and oversized extras for `BroadcastReceiverFire/Query` / applicable services | An exported component without manifest-level permission is **not automatically vulnerable**; never break authorized Tasker hosts or invent security enforcement |
| Authenticated root readback, appops, selected reversible Secure Settings verification | **Conditional privileged capability**, included only after observed need/approval | Effective-state readback, caller/operation allowlist, no untrusted WebView-to-root bridge |
| Shared AxionOS Quick Settings and shade state readback | **Conditional AT-F2**, default off | No SystemUI dependence for JSON, Web Screens, dialogs, OCR, SSH or normal Tasker IPC |
| SSH key or secret access through root, implicit network previews, permission bypass or arbitrary JSON-to-shell execution | **Intentionally excluded** | Preserve consent, isolation and host-key verification |

This reconciliation makes **exported-IPC and scheduler quota coverage explicit**; they were previously described only indirectly. Items outside AutoTools' **verified configuration entry points** remain candidates requiring discovery, not fabricated screens.

## 16. Open prerequisites, feasibility limits and explicit non-goals

**Need before implementation:** Shared-module repository/branch identity; actual installed AutoTools and SystemUI SHA; LSPosed/Vector fork/runtime version and classloader behavior on Android 16; root provider and active grants; whether the official AutoTools Root Add-On is installed; sample export of a **non-sensitive** saved JSON/Web Screen/Dialog Tasker action; physical UI and reproducible failing workflows.

**Still uncertain:** Full method-body call graph; all runtime-generated preference schemas; how Tasker serializes each dynamic field; exact cross-process result callback behavior; feasibility of a reliable zero-modification visual designer for *every* dynamically generated editor; Android 16 root and SystemUI compatibility. Each must be established through concrete tests rather than claimed complete from class names.

**Not part of this release:** Rebuilding any original APK, license bypass, hidden Accessibility/notification permissions changes, unrestricted WebView-root bridge, bypassing secure display/input restrictions, direct JSON preview network execution, universal fixes for arbitrary ROMs, blanket SystemUI/system_server hooks, invented parser/SSH/Web Screen capabilities, or changing old Tasker actions to a proprietary format without explicit migration approval.

## 17. Source and platform references

**Source artifacts**: original user-uploaded `AutoTools_2.3.19.apk`; `System UI_16.apk`; AutoTools deep audit, JSON/UX audit, and Root/SystemUI deep audit noted above. SHA-256 values verified directly in this session.

**Official/public technical references:**

- AutoTools Web Screen guide: https://joaoapps.com/AutoApps/Help/Info/com.joaomgcd.autotools/com.joaomgcd.autotools.activity.ActivityConfigWebScreen.html
- Developer Web Screen variables, callbacks and JS API: https://github.com/joaomgcd/AutoToolsWebScreens
- AutoTools Secure Settings guide: https://joaoapps.com/AutoApps/Help/Info/com.joaomgcd.autotools/com.joaomgcd.autotools.activity.ActivityConfigsettings.html
- AutoTools Root Add-On (separate, optional external package): https://play.google.com/store/apps/details?id=com.joaomgcd.autotoolsroot
- Android 16 foreground-service / job quotas: https://developer.android.com/develop/background-work/services/fgs/changes
- Android background-activity restrictions: https://developer.android.com/guide/components/activities/secure-bal
- Window overlay ordering: https://developer.android.com/reference/android/view/WindowManager.LayoutParams
- LSPosed module scopes: https://github.com/LSPosed/LSPosed/wiki/Module-Scope

---

## Final outcome and delivery status

**Required product outcome:** The **same one installed LSPosed module** supplies a complete AutoTools domain: 33-editor UX parity and a significant visual upgrade, especially **JSON Read/Write, Web Screens, Dialogs, data transformations, Secure Settings, SSH and OCR**, with plugin execution diagnostics, restricted optional root capabilities, carefully scoped SystemUI integration only where proven necessary, and no change to the original licensed APK or existing Tasker action semantics.

**Today:** The design and acceptance specification is complete. **Implementation, installed runtime compatibility, root approval, native overlay validation, hosted CI, actual PR and device acceptance are NOT PERFORMED.**
