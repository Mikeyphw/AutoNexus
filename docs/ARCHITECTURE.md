# Architecture / bootstrap boundaries

- Exactly one Android application and one LSPosed legacy entry point.
- `DomainRegistry` is the canonical four-package allowlist. Unknown packages (including SystemUI, Android, Tasker host and Termux itself) are ignored.
- `RuntimeGate` is a constant false gate: no modification even when user selects all four LSPosed scopes.
- `RootPolicy` rejects every privileged operation; no root executable/service provided. Future implementation must use a restricted, authenticated channel with allowlisted operations, no arbitrary shell from apps/Web Screens and explicit consent for state changes.
- Process data and preferences: no cross-process settings IPC yet. Future settings must be transactional, versioned, authenticated, isolated by domain, and fail closed if unavailable. Never expose world-readable preference files.
- No Tasker bundle translation or rewrite in bootstrap. Future enhanced editors must round-trip original serialized actions, preserving unknown keys and output variables.
- Optional SystemUI will be specifically version pinned and opt-in for AutoNotification HTML and AutoInput shade controls; Termux:Tasker must never require SystemUI.
- No system_server registration by default. A future framework tracing milestone requires separately reviewed risk and security gates.
- Crash containment: each future domain must have a feature-level disable strategy and safe boot recovery tested before behavior changes ship.
