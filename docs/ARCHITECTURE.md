# AutoNexus architecture and preflight boundaries

## Module boundary

- Exactly one Android APK, one legacy LSPosed entrypoint and four allowed plugin package scopes.
- `DomainRegistry` is the exact allowlist. Unknown packages, including SystemUI, Tasker host, system_server and Termux itself, are ignored.
- `RuntimeGate` is still source-level constant false, for all behavior, SystemUI, system_server and root paths. **The Material 3 toggles are staged user intent, not runtime activation.**
- `RootPolicy` rejects every privileged operation; no root service or shell execution is provided.

## App-only configuration (preflight milestone)

`DomainSettingsContract` gives each package two stable, isolated boolean keys:
`requested` (future enhancement request) and `local_diagnostics` (display local package details). Both default false. The store is module-UID-only Android `SharedPreferences` in `MODE_PRIVATE`, schema version 1. Save is an atomic, synchronous `commit`; the UI updates only on success. Unknown/newer schema, orphaned old keys, invalid types and unreadable data all fail closed; the app does **not** silently discard newer preferences.

This storage belongs exclusively to the AutoNexus Activity. Injected target processes **cannot** read it, and no provider, exported service, external file or world-readable preference bridge is registered. The UI never claims that an LSPosed scope is active. A future adapter must establish authenticated versioned cross-process IPC with isolated per-domain authorization and independently verified compatibility before enabling any hook. Existing original Tasker plugin bundles/preferences remain unchanged.

## Material 3 app UI

`MainActivity` uses Compose Material 3, a system-based dynamic color scheme when supported, scrollable cards, two accessible navigation destinations and live package-manager inspection. `queries` remains the exact four known packages. The UI distinguishes installed/reference match, mismatch and Android's indistinguishable "not installed or not visible" case. Local diagnostics reveal only package metadata. No notifications, Tasker variables, UI trees, scripts or secret-bearing strings are collected.

## Future domain work

The four detailed `docs/roadmaps/` remain specifications, not implemented behavior. TT-F0 read-only tracing and TT-E1's version-gated editor are implemented in source but not physically qualified. **Modifying IPC fixes**, other-domain adapters, optional root and conditional SystemUI hooks remain blocked on on-device evidence and separately reviewed native overlays. Do not auto-retry unknown-state Tasker commands. Physical LSPosed acceptance is independent of hosted JVM/Android build success.

## TT-F0 observational boundary

The Termux:Tasker scope may load version/signature-pinned Xposed read-only
method hooks, which never set hook results or change execution state.
Observations are bounded to 160 closed-enum events per process and only
visible in LSPosed logs; the companion app uses PackageManager/UserManager
read-only checks. No cross-app IPC, storage provider, command execution or
automated recovery channel is added. This does not enable the false
`RuntimeGate` behavior/privilege flags.

## Termux:Tasker TT-E1 configuration boundary

The version/signer-gated main plugin process has a UI-only hook into its existing `EditConfigurationActivity`. Its dialog stages eight stock fields. Validate & Save copies the values into the original widgets and invokes the stock `finish()` to generate the original Locale bundle; Discard marks the original cancellation flag and returns `RESULT_CANCELED`. Original fallback discards only the enhanced draft. Back/Home while enhanced mode is active cannot call the original implicit Save. A versioned, bounded Activity saved-state bundle restores transient drafts during recreation; it is not added to Tasker configuration, cross-package preferences or root services. Unknown Tasker fields and unavailable numeric ABI refuse enhanced Save.

## TT-E2 local editor explorers

The plugin's existing signer/version attestation remains the only gate to the injected editor. The new pure `ArgumentStudio` accepts the output of the installed plugin's `ArgumentTokenizer` and performs no independent tokenization. `ScriptExplorer` uses NIO read-only bounded enumeration on a user-triggered worker; stale Activity requests are dropped using a generation counter. No FileProvider, exported component, service or IPC channel is added. Selecting a path only writes a staged field, and the original plugin must still validate and serialize it through `EditConfigurationActivity.finish()`.

TT-E2 uses `EditorDraftBridge` to isolate enhanced/Original draft transitions, while the original app alone owns bundle serialization. The script browser has independent inspected-entry/display/depth bounds and symlink-component refusal.
