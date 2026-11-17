# AutoNexus current feature inventory

This table distinguishes **source implemented** from **hosted, physical and release acceptance**. A passed earlier hosted build does not accept unbuilt changes.

| Component | Current source behavior | Still outstanding |
|---|---|---|
| Shared companion | Material 3 dashboard, package visibility, private versioned per-domain staged intent, diagnostics | Authenticated process-crossing settings IPC (only when needed) |
| AutoNotification | Exact package scope, no notification-modifying hooks | HTML restoration, complete editors, optional verified SystemUI adapter |
| AutoInput | Exact package scope, no UI/gesture hooks | Actions v2, UI query, gestures, screenshot, explicit root fallback |
| AutoTools | Exact package scope, no JSON/secure settings behavior changes | Full 33 editors, JSON/visual Web Screen/Dialog studios, permitted secure settings |
| Termux:Tasker TT-F0 | Verified-package read-only receiver/dispatch/result/callback timing and redacted LSPosed log; companion readiness | Physical traces, host acceptance proof, proven non-response repair |
| Termux:Tasker TT-E1 | Eight-field in-plugin editor; explicit save/discard/stock fallback; bounded activity-state recovery; conservative validation using original numeric constants | Physical save/restore equivalence; browser, argument studio, complete rich UI; specific IPC repair after evidence |
| Hosted CI | Exact source SHA checkout, static/JVM/Android lint/DEX gates and temporary debug artifact | New HEAD hosted checks, physical device acceptance and production signing |

**Tasker IPC reliability status:** no modifying IPC fix is implemented. The reported “plugin did not respond” symptom does not by itself prove the command never executed. Automatic command replay stays prohibited. No Termux:Tasker feature requires SystemUI, a root daemon or altered Tasker/Termux APKs.

### TT-E2 implementation note

The Termux:Tasker enhanced editor gains an opt-in exact original tokenizer preview, bounded read-only script/directory navigator with UID-based fail-closed fallback, and mode/result guidance. This does **not** constitute the completed IPC reliability repair or full roadmap acceptance; see `docs/TERMUX_TASKER_IMPLEMENTATION.md`.

TT-E2 regression hardening: current-original-field reentry, throwaway enhanced-only drafts, bounded inspected directory entries and denial of symlink traversal. No new execution or IPC behavior.


### Termux:Tasker TT-F0 evidence import (prepared, awaiting CI/device acceptance)

- Operator-selected, read-only import of an existing LSPosed `TT_TRACE` log
  using Android's document picker; bounded to 512 KiB / 12,000 lines / 320
  accepted process-local enum events.
- Redacted phase/outcome counts and evidence-incomplete notices; an explicit
  sanitized report export through the Android document picker, without raw log
  content or new permissions. Missing trace events are not interpreted as proof
  that Tasker failed to dispatch or that a command was not executed.
- No IPC behavior repair, Tasker host hook, script replay, root command, Termux
  original APK modification, SystemUI scope, or acceptance claim.


### TT-F1 editor and boot-safety candidate (native validation pending)
- In-process native themed section navigator with fixed footer, adaptive palette
- Original path autocomplete adapter suggestions preserved via independent copy
- Bounded per-query suggestions from accessible script tree and a manual path
  fallback when Android UID cannot enumerate the folder
- Opt-in Tasker boot Wait setup and read-only unlock status (service-ready is
  explicitly UNKNOWN); no startup command delay/replay or extra app component
- Existing v0.9.0 serializer, version+signer gate, privacy and no-root/no-SystemUI
  scope unchanged; hosted/physical tests remain separately required
