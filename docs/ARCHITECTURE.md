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

The four detailed `docs/roadmaps/` remain specifications, not implemented behavior. Runtime adapters, editor injection, IPC fixes, optional root and conditional SystemUI hooks remain blocked on real-device baselines and separately reviewed native overlay changes. Do not auto-retry unknown-state Tasker commands. Physical LSPosed acceptance is independent of hosted JVM/Android build success.
