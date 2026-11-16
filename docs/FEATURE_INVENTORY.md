# Feature inventory — preflight versus future behavior adapters

| Component | Existing baseline | Preflight milestone proposed | Still not implemented |
|---|---|---|---|
| Shared companion UI | Legacy read-only Android view cards | Compose Material 3 dashboard, dark/light dynamic color, package details and diagnostics tab | Runtime injected plugin editors |
| Four independent domains | Package scope and inert entrypoint | Private per-domain requested intent and local-details controls with safe-off labels | Authenticated settings IPC and behavior hooks |
| AutoNotification | Scope only | Version visibility and inert LSPosed checklist | HTML restoration, Notify/Table/Intercept/Query/Channels/Rules/Tiles/Toasts UI, conditional SystemUI |
| AutoInput | Scope only | Version visibility and inert LSPosed checklist | Actions v2, UI Query, gestures, capture, events and root input |
| AutoTools | Scope only | Version visibility and inert LSPosed checklist | 33 editors, JSON/Web Screen/Dialog studios, Secure Settings |
| Termux:Tasker | Scope only | Version visibility and inert LSPosed checklist | Two-sided IPC tracing/repair, post-reboot readiness and safe Tasker editor, no SystemUI |
| Hosted build | GitHub Android compile/JVM/lint+DEX gate | Exact PR HEAD checkout and short-lived debug APK with source/SHA-256 | Device/test/release qualification |

The four `roadmaps/*.md` remain agreed per-domain feature specifications. A staged request switch deliberately does **not** activate the LSPosed module's modifying hooks. Physical app version, ROM and LSPosed compatibility must be verified before implementing those adapters.

## Termux:Tasker TT-F0 shipped source scope

The Termux:Tasker domain now has exact-signer/version-gated **read-only**
receiver/dispatch/result/callback observations, a bounded, redacted LSPosed
trace and a companion readiness panel. No functional IPC repair or enhanced
editor is asserted. Read `docs/TERMUX_TASKER_IMPLEMENTATION.md`.
