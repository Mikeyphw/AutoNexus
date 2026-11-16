# AutoNotification — Final Consolidated Roadmap (Shared LSPosed Module)

**Baseline:** 2026-11-16 · AutoNotification `com.joaomgcd.autonotification` 4.3.14 (versionCode 182), target API 36 · Android 16 / user-supplied AxionOS SystemUI.  
**Target delivery:** **one shared LSPosed companion APK** serving AutoNotification, AutoInput, AutoTools and Termux:Tasker, with independent domains and a restricted optional root helper. **This document specifies the AutoNotification domain only.**  
**Status:** technical plan grounded in the two supplied APKs and previous static audits; **not implemented, not installed or tested on a physical device**.  
**Non-negotiable:** no rebuild, patch, re-sign, replacement or licensing bypass of any original app or SystemUI APK. Existing Tasker tasks, original preference keys, saved bundles, permissions, notification identity, callbacks and behavior must remain valid.

## 0. Source provenance and evidence boundaries

| Artifact | SHA-256 | Static observations |
|---|---|---|
| `AutoNotification_4.3.14.apk` | `bc72e994c8783498555a8601604b8c2aacfe51d544298ed2914dd98f12ee0783` | HTML conversion, notification listener, Notify/Intercept/Query/Table configuration, channels, toast accessibility, 40 tiles |
| `System UI_16.apk` | `8e2d9452ca0e2473ff1574deefb610d40b94b1ecf7810a1efcb05745df720ee9` | Android 16 AxionOS notification wrappers, content view and binder classes |

Prior detailed audits: `AutoNotification_4.3.14_Deep_LSPosed_Audit.md`, `AutoNotification_HTML_Restoration_LSPosed_Roadmap.md`. Actual installed APKs, ROM fingerprint, LSPosed/Vector version, root implementation and live framework flags remain unverified. All hook resolutions must be verified against the live installed environment before enabling modifying behavior.

**Evidence levels:** **V** static APK/DEX resource verified; **P** proposed behavior backed by a plausible hook; **C** contingent on on-device proving of a failure boundary. A class name or method signature existing does not prove a fix works.

## 1. Product-level requirements

1. **Single module, four independent domains.** One installed LSPosed APK; AutoNotification, AutoInput, AutoTools and Termux:Tasker are feature packages, not separate modules. Independent toggles, scope validation, process-isolated fault handling and per-domain diagnostics. Do not require SystemUI in Termux:Tasker.
2. **No target APK modifications.** Runtime injected views, hooks and a companion Activity hosted by the shared module. Settings saved to original AutoNotification configuration where representable, plus tightly namespaced sidecar policies only for genuinely new features.
3. **Root is additive, not default.** Ordinary UI, HTML conversion, intercept/query inspection and preview work without root. A narrow authenticated helper performs only approved privileged diagnostics and targeted recovery. Never expose generic `su` execution to plugin or Web/Tasker input.
4. **SystemUI is opt-in and version-pinned.** Support a per-build AxionOS rendering adapter for HTML only after app-side diagnostics demonstrate it is needed. No global notification rewriting or cross-package text alteration; no default `system_server` hook.
5. **Existing licenses, user permissions, notification privacy and Tasker semantics are honored.** No bypass of device lock or sensitive/OTP redaction. No automatic enabling of revoked notification-access permissions.
6. **Truthful operation.** Distinguish a command dispatched from completion, a matched notification from an action performed, a preview from the actual shade, and a test pass from a skipped or unperformed check.

### Major capability map

| Domain | Primary scope | SystemUI | Root |
|---|---|---|---|
| AutoNotification | HTML, visual editors, Intercept, Query, reply/actions, listener, channel/table/tile diagnostics | Conditional HTML rendering and optional verified QS state | Read-only system state; narrowly authorized recovery |
| AutoInput | Separate roadmap | Optional QS/shade adapter | Separate root input policy |
| AutoTools | Separate roadmap | Usually unnecessary | Separate Secure Settings policy |
| Termux:Tasker | Separate roadmap | **None** | Separate IPC/boot diagnostics policy |

## 2. AutoNotification full UI/UX redesign

### 2.1 Existing implementation baseline (V)

The original `res/xml/config_notify` comprises **78 `EditTextPreference` fields, 21 checkboxes, 6 list selectors, 6 categories, and 22 nested `PreferenceScreen`s**. `config_notify_buttons` contains **another 21 text fields and 3 selectors**. Additional independent configuration activities cover Intercept, Query, Notification Table, Actions/Reply, Channels, Block/Snooze and tile configuration. Current flow is a legacy, heavily nested preference hierarchy, not a visual composer.

### 2.2 New entry points and navigation (P)

Within AutoNotification's existing Tasker configuration activities, inject an **Enhanced Editor** entry and a non-destructive switch to **Original Editor**. The enhanced editor is a hosted runtime-injected view (or module-owned modal with a verified Activity-result bridge), not a target APK rebuild.

Primary navigation: **Overview**, **Compose**, **Intercept**, **Query**, **Actions & Replies**, **Tables**, **Channels**, **Rules**, **Quick Settings**, **Toasts**, **Diagnostics**, **Advanced**. For a specific Tasker action, show only its relevant editor; the domain overview links to other tools without rewriting unrelated Tasker actions.

Global UX requirements:
- Search across settings, controls, help and nested fields; breadcrumbs; contextual tips; recent/favorite fields.
- Android 16 Material-style layout respecting edge-to-edge insets, IME/keyboard, large fonts, screen rotation, accessibility semantics, dynamic light/dark mode and compact phones.
- **Save / Discard / Continue Editing**, draft preservation on recreation, unsaved-change warning, and no accidental save on Back. Respect Tasker's original plugin result contract.
- Non-destructive imports, diagnostics export and optional editor preference presets. Do not silently migrate existing tasks.
- Live validation with exact affected-field highlighting, Tasker `%variable` placeholders marked **unresolved**, not incorrectly rejected.
- Preview outcomes with visible source: **simulated editor preview** versus **actual test notification on device**.
- Module-created new settings stored in versioned companion configuration with bounded retention, stable keys and a visible compatibility notice; never insert unknown keys into legacy Tasker bundles that cannot parse them.

### 2.3 Compose: complete Notify action wizard (P)

Replace 22 nested screens in the enhanced UI with seven logical sections, each preserving original serialized fields:

1. **Content** — notification ID/tag, title, expanded title, body, expanded body, subtext, info, ticker, append/update, persistent and timeout. Inline HTML editor, tag assistance, plain/styled toggle, live parsing diagnostics, Tasker variable chips.
2. **Appearance** — small/large icon, images, picture sources/cache, tint, backgrounds, colorization, visibility, progress, LED, sound and vibration, with contrast and dimension warnings; report ROM/channel overrides rather than presenting false guarantees.
3. **Actions** — tap, dismiss, create, URL, buttons, reply and command mappings; a visual **button/action row editor** pairs index + label + icon + action and validates length mismatches and placeholders.
4. **Delivery & behavior** — channel, importance and existing channel state; alert constraints, lockscreen/public version, priority, grouping, persistence, updates; no silent mutation of user-controlled channel behavior.
5. **Conversation & media** — participants, messages, media metadata (track/album/artist, controls/state, position/duration), grouping and summaries, editing repeatable entries as structured rows.
6. **Preview & test** — collapsed, expanded, heads-up, light/dark, private/public representations with exact scope labels. Generate **explicitly requested** test notification to compare simulated vs actual rendering; allow cleaning up created fixture only.
7. **Advanced/original** — every remaining preference accessible with the same raw keys, original values and a one-tap return to legacy preference UI.

**Critical validation:** preserve IDs/tags, channel ID, timeouts, all `PendingIntent`s, `RemoteInput`s, actions order, notification extras, tasker replacement syntax and index-sensitive separators. A user-created template must never rewrite running Tasker tasks without explicit save.

### 2.4 Rich HTML authoring/preview (P + C)

- Syntax-aware editor for supported Android `Html.fromHtml` subset; paste/escape entities, nested `<b>`, `<i>`, `<u>`, `<s>`, `<font color>`, line breaks and paragraphs, optional background/text sizes where validated.
- Side-by-side **source**, **Android Spannable preview**, **actual posted notification**; show exactly which span type survives: input -> `Html.fromHtml` -> `Notification.Builder` -> extras -> SystemUI rendered `TextView`.
- Explicit per-field support matrix: collapsed title, collapsed body, big title, big body, subtext, Table/custom view. Distinguish “unsupported by template,” “span stripped,” and “source contained no styling.”
- Color picker with hex/alpha validation and light/dark contrast warnings. Never silently darken colors if user intended exact colors; an optional contrast adjustment must be disclosed and reversible.
- Raw HTML sanitization and bounded input; no arbitrary web HTML/JS, external fonts or full CSS engine.

### 2.5 Intercept visual rule builder (P)

- Read existing filters for package/app, title, text, category, regex, case-sensitive/case-insensitive, inverted conditions, group summary, ID, media/reply state and priority.
- Create visual AND/OR filter composition **only when exactly expressible in existing AutoNotification settings**. Any richer semantics require opt-in module-side evaluation and separate tests; do not silently change legacy matching.
- **Why matched / why not** inspector, filter-by-filter trace, regex syntax and sample testing, counts/false positive warning, duplicate event explanations.
- Real notification selection only for authorized listener-visible notifications; default logs redact contents and never claim access to Android-redacted fields.
- Distinguish initial active-notification snapshot from new posts; never replay events merely to populate the debugger.

### 2.6 Notification Query and JSON output inspector (P)

- Field explorer with source label (notification extra, RemoteViews extraction when available, not present, redacted); filters, regex, result counts, index mapping.
- Show returned Tasker variable names/arrays; **Get As JSON** and **Get All Fields** already exist and must be preserved rather than duplicated.
- Structured JSON preview, expandable tree, selector-to-output mapping, safe truncation and performance warnings for large queries.
- Query sandbox preview must not execute notification actions or change notification state.

### 2.7 Actions, replies, buttons and cancellation (P)

- Typed action builder for tap, notification button, inline reply and dismiss; inspect chosen notification key, IDs, native action index and `RemoteInput` availability.
- Human-readable status: matched / intent dispatch attempted / dispatch accepted / downstream confirmation unknown or observed. Never infer app-level completion merely from `PendingIntent.send()` success.
- Reply composer preview and missing-field warnings; redact contents in default logs. Do **not** automatically retry replies or actions with side effects.
- Per-action safety notices for uncertain repeat execution; explicit manual retry only.

### 2.8 Notification Table designer (P)

- Dedicated grid/row editor for table cells, icons, text, colors, sizes, separators, actions and update rules; show row and column counts, array index alignment, supported custom RemoteViews limits.
- Render mock preview and an opt-in actual Table notification. Side-by-side standard Notify vs Table for HTML compatibility testing.
- Validate nonempty rows, excessive length, malformed color, missing icon, inaccessible image and channel behavior. Preserve the original Table payload format.
- Do not automatically convert every notification to a Table; Android 12+ custom layouts use system templates and may not match browser HTML.

### 2.9 Channels, blocking and snoozing (P)

- Channel list with effective state, importance, sound, vibration, badges and existing user overrides. Compare **requested** versus **observed** behavior.
- Channel create/edit guide that marks immutable attributes of previously created channels. Link to Android's native channel settings for user-controlled changes, not unauthorized override.
- Visual Block and Snooze rules; conflict detection, affected-app preview, scope and duration, reversibility, explanation when a rule matches.
- Audit log distinguishing rule applied, service confirmation, and effect observed.

### 2.10 Quick Settings tile editor (P, optional SystemUI)

- Inventory of 40 existing tile services; setup status, label/icon, intended vs observed active/inactive/unavailable state, click mapping, last update, lifecycle.
- Test tile activation and toggle reconciliation; follow `onStartListening` / `onStopListening` boundaries rather than running a permanent loop.
- Optional AxionOS integration to inspect shade/tile state, **only** when a verified SystemUI method supports it and standard TileService state is insufficient.
- Never force tile additions, bypass lockscreen gates or assume a tile stays bound after leaving the shade.

### 2.11 Toast and service-health editors (P/C)

- Toast intercept view with Accessibility service status, allowed event type, package, matching filters, event arrival stats, and “text unavailable” explanations.
- Service health dashboard for notification listener (authorized/connected/disconnected/rebinding), toast accessibility, process alive, Tasker integration, recent hook errors, root availability.
- Note that SystemUI or root observations can help diagnose lost toast events but cannot guarantee recovery of source text never made available.

## 3. HTML restoration engine: confirmed hooks and conditional fixes

**Verified AutoNotification DEX integration targets:**
- `com.joaomgcd.common8.NotificationInfo.html(boolean, String): CharSequence` and `html(String)`; `Html.fromHtml` invocation.
- `com.joaomgcd.autonotification.intent.IntentNotification.G1(): NotificationInfo` (Use HTML setting).
- `NotificationInfo.Q(boolean): Notification` (expanded `BigTextStyle.bigText` creation).
- `NotificationInfo.getNotificationBuilderAction(Bitmap): Notification.Builder` (title/body builder fields).
- `NotificationInfo.K(int, Notification, boolean): Notification` (posting boundary).
- `NotificationInfoTable.getForTable(...)` (custom `RemoteViews` text).

**Verified SystemUI targets:**
- `com.android.systemui.statusbar.notification.row.wrapper.NotificationBigTextTemplateViewWrapper.onContentUpdated(ExpandableNotificationRow)`;
- `com.android.systemui.statusbar.notification.row.wrapper.NotificationTemplateViewWrapper.onContentUpdated(ExpandableNotificationRow)`;
- `NotificationRowContentBinderImpl.inflateNotificationViews(...)` **only if essential**.

**Decision ladder (all changes version-guarded):**
1. Read-only trace the user-enabled HTML flag and the resulting actual `Spanned` types/counts. No raw text in logs.
2. If conversion fails before posting, fix only AutoNotification's conversion/field mapping.
3. If builder, expanded template or `RemoteViews` strips styling, intercept the proven boundary and preserve original supported spans. AOSP Android 16 conditionally strips spans in `BigTextStyle.makeExpandedContentView()`: prove the relevant flag and path are active on AxionOS rather than assuming they are.
4. Preserve only compact, versioned, allowlisted styles in notification extras if required by a downstream adapter, and verify the extra survives actual posting and updates. Guard metadata integrity and parcel size; do not use untrusted extras as proof of authenticity.
5. If the notification data contains styles but the rendered view does not, use opt-in exact-version SystemUI wrapper hooks to reapply styles after inflation. Match the AutoNotification-originated SBN and validated notification identity; never change unrelated packages or privacy-redacted content.
6. Re-test rebind/reinflation, collapse/expand, heads-up, grouping, theme and update. Fall back to original rendering when anything is ambiguous.

**P0 must-haves:** supported inline bold/italic/underline/strike, foreground colors, newline/paragraph/entities, original Tasker variables, unchanged actions, and clear per-surface reporting. **Stretch:** background colors, font-size spans, super/subscript, conditional custom view fallback when Android allows. **Out of scope:** full HTML/CSS, JavaScript inside notifications, global system formatting flag changes, private content bypass.

## 4. Interception, listener and action reliability (LSPosed first, root only as needed)

**Existing verified behavior:** `ServiceNotificationIntercept.onListenerConnected()` already invokes active-notification retrieval, and the app implements connected/disconnected, posted and removed callbacks. The implementation is not missing basic bootstrap behavior.

- Trace listener connection and first usable event, startup snapshot versus new event, remove/update reason and duration, and correlation key + generation. Hash identifiers, not text, for local diagnosis.
- Use the official `requestRebind(ComponentName)` only when the listener is in a state where it is permitted, access is granted, and retries are bounded. No perpetual rebind loop, auto-force-stop, auto-regrant or duplicate initial notifications.
- Explain filter match, action dispatch, reply availability, query field provenance, group-summary interpretation and media controls with evidence levels.
- Record backpressure/drop metrics in bounded queues; never block listener callbacks to write files or run root commands.
- Root helper may check `dumpsys notification`, service state, package enabled state and appops when authorized and available; it does not fabricate notifications missed or redacted by Android.
- If a notification was posted and later filtered by the OS, document why metadata differs rather than bypassing security-sensitive filters.

## 5. Restricted root helper and SystemUI contract

### 5.1 Root helper (C)

Approved operations only: read-only compatibility/provenance checks, permission-state diagnostics, listener status and relevant notification service state; carefully authorized one-shot recovery when a confirmed failure is repairable. Maintain an explicit capability allowlist, restricted caller identity, authenticated IPC, timeouts, bounded output and read-only default. No generic shell exposed to Tasker/plugin inputs. Never persist root shell access tokens or plaintext notification content.

### 5.2 SystemUI adapter (C)

- Disabled until exact AxionOS fingerprint/class signature validation and device-specific read-only trace are complete; off by default in new installations if compatibility unknown.
- Targets only AutoNotification-owned notifications verified with a collision-resistant correlator. Respect multiple users/profiles and notification update generations.
- Hook after original `onContentUpdated`, operate on allowed text nodes, avoid retaining stale View/row references, and handle async inflation/reuse/heads-up.
- Never touch other apps' notifications, Android's notification privacy/redaction, lockscreen security, channel permissions or arbitrary QS tile behavior.
- **Recovery before rollout:** external safe-disable marker, compatibility sentinel, limited hook error thresholds, no root calls in the rendering path, boot-loop rollback instructions tested before enabling.
- `system_server` hooks are explicitly excluded from initial delivery and require a separately demonstrated, essential gap and independent risk approval.

## 6. Companion UX, settings, privacy and compatibility

Companion main dashboard:

- Device compatibility: original APK hashes, installed versions, ROM fingerprint, framework/API, root helper and each hook family status.
- Tabs for AutoNotification/AutoInput/AutoTools/Termux:Tasker; one set of global privacy/crash settings and distinct per-app controls.
- AutoNotification cards for HTML, UI Editor, Listener, Intercept/Query, Toast, Channels, Tables, Tiles and optional SystemUI.
- Tests: named preset library, simulate preview, create/remove an explicit test notification, trace one action, listener health self-check.
- Diagnostics: timeline with stage, thread/process, start/finish, failure code and affected hook; filtering, export/redaction, retention and manual clear.
- Safety: kill switches for root/SystemUI/each feature, restore defaults, compatibility mismatch alerts, a fail-open “legacy editor” switch and offline rollback instructions.

Config architecture:

- Original preference keys/bundles remain canonical for existing Tasker actions. Changes round-trip through original encoder/`finish` contract.
- Any sidecar policy uses namespaced, schema-versioned storage and stable action identity, with clearly visible unsupported/missing-policy behavior. Migration must be opt-in and reversible.
- No license hooks and no data flows to external servers. Logs omit raw messages/OTP, reply text, images, URLs and credentials unless the user explicitly enables a narrow diagnostic capture, and never by default.
- UI accessible by touch and keyboard/screen reader; preserve rotation/drafts, IME visibility, long content, RTL, no forced clipboard operations.

## 7. Consolidated milestone and delivery plan

**These are acceptance gates, not permission to fragment into serial micro-overlays.** Prefer one integrated AutoNotification feature overlay after the read-only evidence baseline; if the HTML fix needs data before precise hook selection, a read-only probe is the only intentionally separate preparatory artifact. Fix underlying failures by revising that consolidated feature artifact, not piling on hotfix ZIPs.

| Milestone | Scope consolidated together | Exit criteria |
|---|---|---|
| **Foundation / AN-F0** | Verify installed APKs, LSPosed/Vector compatibility, root implementation, AxionOS fingerprint, raw preference mapping; reproducible fixture & read-only HTML/listener/UI tracing | Target identity verified, exact hook resolution, first actual formatting-loss boundary and UI serialization baseline established |
| **Full AutoNotification / AN-F1** | **One integrated feature change set**: complete UI revamp for all editor families; HTML restoration at proven boundary; preview engine, Intercept/Query/Action/Table/Channel/Rule/Tile/Toast UX; listener diagnostics and safe recovery; companion UI; narrow root service | Unit/negative/contracts pass; old saved tasks unchanged; new editor round-trips; HTML must-haves work on tested surfaces or explicit device limitation is demonstrated |
| **Optional AxionOS adapter / AN-F2** | Only if AN-F1's app-side restoration cannot restore styling: exact-version SystemUI rendering hooks and QS state fixes required by evidence; crash safeguards included in same change set | Confirm rendered style across lifecycle, no other app affected, no SystemUI crash or privacy regression; rollback tested |
| **Release / AN-F3** | Consolidated hardening, performance, accessibility, boot, root/security, regression suite, CI workflows and docs; signed shared-module APK and reproducible provenance | Physical-device evidence, hosted checks on exact PR HEAD, license unchanged, safe disable/recovery verified, honest known limitations |

**Integration with shared module:** The AutoNotification domain ships as part of the **same single artifact** as the other plugin domains, not a separate LSPosed module. Work can be developed/tested per domain, but overall release/test/rollback controls are shared. The final shared release should not claim full multi-app completion until all independently approved domains pass their own acceptance gates.

### Development/overlay contract (when implementation is requested)

- Repository/target branch **not yet selected**. Do not invent commit/PR/CI identifiers.
- Before the first overlay, inspect `Mikeyphw/devtool` `main`, pin verified Devtool Launcher v3.1+ native launcher and source/tree/blob identity, verify target named branch/worktree and package integrity; use native preflight/validate/atomic apply/durable sessions/approved push and PR, not manual patching.
- Native plain terminal settings: `DEVTOOL_EXECUTION_PRESENTATION=plain`, `DEVTOOL_JOB_UI=plain`, `DEVTOOL_TEXTUAL_APPLY_HOST=0`, `DEVTOOL_TEXTUAL_EXECUTION_HOST=0`. No clipboard-copying from the tool. Preserve user work and never reset/rebase/force-push or auto-merge.
- Audit obsolete/overlapping gates, retain meaningful security and regression coverage, consolidate tests, update CI/docs. Verify local/native/hosted/device evidence separately.
- Every actual overlay handoff must report: repo, target branch, starting HEAD/resulting commit, launcher/runtime identity, exact file inventory, updated/deleted checks and reasons, tests, PR/CI state, one ready-to-run Termux command, limitations and acceptance.

## 8. Test matrix and negative coverage

| Focus | Non-negotiable cases | Acceptance |
|---|---|---|
| Source/provenance | Wrong AutoNotification version, wrong SystemUI, absent method, changed loader, disabled root | Graceful no-op, specific reason; no target APK mutation |
| HTML | Use HTML on/off, tags, nested style, colors, paragraphs, malformed markup, Tasker placeholders, empty/huge text | Correct supported spans on tested surface; unsupported paths explained |
| Surfaces | Collapsed/expanded/heads-up/group/lockscreen/private/public, standard vs Table, dark/light, RTL | No leaks; unchanged action/interaction and legible supported formatting |
| Lifecycle | 200+ create/update/cancel/repost cycles, rapid re-inflation, shade toggles, same ID, multiple users, group summary | No stale spans, cross-key style mix-up, duplicate Tasker firings |
| Editor | Full old preferences round trip; edit/new action; Cancel vs Save; back; rotation; dynamic variables; process restart | Legacy Tasker bundle identical when untouched; changed value serialized exactly as original |
| Intercept/Query | Match/no match, regex invalid, inverted filter, JSON arrays, missing/redacted fields, active snapshot | Explanations match actual engine; no made-up fields or event replay |
| Actions/replies | Notification tap, button, cancel, inline reply, unavailable actions, duplicate prevention | No unintended repeated command or false completion claims |
| Channels/rules | Existing immutable channel, permission denied, snooze cancel, overlapping block rules | User settings preserved; observed state explained |
| Quick Settings/toasts | Tile lifecycle bind/rebind, removed tile, screen state, service disconnected, toast event unavailable | No polling loops or fabricated text; graceful degraded state |
| Root/privacy/security | Unauthorized caller, malicious extras, invalid IPC, revoked permission, missing root, redacted messages | Operations refused safely; no plaintext content in default diagnostic export |
| Stability/performance | Cold boot/first unlock, 200+ cycles, SystemUI kill/restart, LSPosed disable/re-enable, crash loop recovery | No SystemUI boot loop; measurable bounded processing overhead; recovery proven |
| Hosted/device | Unit, integration, negative, hook contract, emulator if supported, physical Android 16 | Each layer reported independently; skipped ≠ passed |

Additional stress coverage: 500-event mixed load, slow root helper, storage full, low-memory app kill, notification extras approaching Binder size constraints, concurrent updates and two similarly named notifications. All tests should be deterministic where possible and protect user data.

### Required product acceptance

1. **Same licensed AutoNotification 4.3.14 installed and functional.** No changes to its APK/signature, license or Tasker action records.
2. **Complete enhanced UI accessible**, legacy escape hatch working, reliable Save/Cancel/drafts, previews labeled simulated vs actual.
3. **HTML formatting problem demonstrably fixed on specified tested layouts** or a narrowly documented Android 16/AxionOS limitation supported by recorded evidence; never claim full browser HTML.
4. **No duplicate notification tasks, actions or replies** introduced by listener recovery or inspection.
5. **Root optional**, SystemUI optional with tested safe-disable; feature scope restricted to verified packages.
6. **Every assertion supported by actual test evidence** including exact artifact hashes, version fingerprints, native Devtool validation and exact hosted PR HEAD.

## 9. Reconciliation with earlier AutoNotification suggestions — explicit disposition

The recommendations from `AutoNotification_HTML_Restoration_LSPosed_Roadmap.md`, `AutoNotification_4.3.14_Deep_LSPosed_Audit.md`, and the later LSPosed + root discussion are explicitly classified here. This section **does not** authorize new APKs, unrelated SystemUI hooks or premature privilege changes.

| Earlier suggestion | Disposition in the one shared module | Acceptance boundary |
|---|---|---|
| HTML repair, supported rich text, diagnostic span tracing, editor preview | **Required — AN-F1**, conditional AxionOS adapter in AN-F2 | Actual collapsed/expanded/updated layout behavior confirmed; not full HTML/CSS |
| Intercept/Query, action/reply failure reasons, listener rebind safeguards, channels, block/snooze, Table, toast, 40 tiles | **Required — AN-F1** | Match explanations and observed results; no duplicate Tasker firings |
| Full Notify / buttons / Table / query / rule / channel / tile / toast UI/UX | **Required — AN-F1** | Legacy-field parity, exact serialization, Save/Cancel/draft checks |
| Enhanced media-session and Android Auto-related diagnostics | **Conditional feature in AN-F1**: map `IntentMediaInfo`/Android Auto-related code paths on installed APK, expose supported media state/command and session lifecycle diagnostics only when verified | No unsupported Android Auto protocol changes; no duplicate media commands |
| Optional local notification history | **Optional opt-in AN-F1 capability**: bounded **metadata-only** ring buffer (event kind, hashed notification identity, channel, timestamps, status), search/filter/export and TTL/purge UI | Disabled by default; never archive notification body, OTP, reply text or images by default; history cannot reconstruct undelivered notifications |
| Framework event tracing in `system_server` | **Conditional / separately approved** only if ordinary listener plus app/SystemUI diagnostics cannot identify a demonstrated missing event. Read-only metadata first | Version-pinned, bounded overhead, privacy/security review, tested boot-loop recovery; not part of default scopes |
| Exported Tasker/AutoApps receiver authorization audit | **Required negative/security tests in AN-F1/AN-F3**, without assuming a vulnerability from exported components | Harmless malformed intents, wrong caller/permission, payload-size edge cases; preserve legitimate Tasker callers |
| Full HTML/CSS/JavaScript, unrestricted hidden/private notification capture, silent permission regrant, automatic reply replay | **Intentionally excluded** | Platform/privacy/safety constraints; cannot be promised by root access |

**Completeness rule:** An earlier item classified *conditional* must be traced to a device-observed requirement and explicit compatibility decision; it is not silently considered delivered. An *optional* feature must have a working independent toggle, negative tests and a documented no-op state before being called implemented. Any feature added here is still delivered within the **one AutoNotification domain and one shared LSPosed module**.

## 10. Explicit non-goals and pending information

- No modification of original APKs, no bypass of AutoApps/Tasker licenses or privilege prompts.
- No universal support for arbitrary CSS/JavaScript, background HTML or custom fonts in Android notifications.
- No automatic attempts to unredact OTPs/private notification text or bypass lockscreen protection.
- No global `system_server` or SystemUI behavior changes without a separately proven requirement.
- No hard-coded assumptions about LSPosed fork compatibility, installed root state or platform feature flags.
- Pending before implementation: select shared-module repository and branch; verify installed source/runtime; capture a reproducible HTML failure and controlled Tasker fixtures; inspect live SystemUI class loader; verify root policy.

## 11. Verified public platform references

1. Android 16 AOSP `Notification.BigTextStyle.makeExpandedContentView()` and `stripStyling` conditional — https://android.googlesource.com/platform/frameworks/base/+/android16-qpr2-release/core/java/android/app/Notification.java
2. Android `NotificationListenerService`/`requestRebind` lifecycle — https://developer.android.com/reference/android/service/notification/NotificationListenerService
3. Notification channels and user-controlled immutable behavior — https://developer.android.com/develop/ui/compose/notifications/channels
4. Android 12+ custom notification restrictions — https://developer.android.com/develop/ui/views/notifications/custom-notification
5. Quick Settings TileService lifecycle — https://developer.android.com/reference/android/service/quicksettings/TileService
6. AutoNotification HTML regression developer discussion — https://tasker.helprace.com/i1871-html-formatting-broken-in-autonotification-with-android-15

**Final status:** Roadmap and technical source review complete; no LSPosed APK, root helper, Git overlay, PR, hosted CI or real-device implementation delivered by this document.
