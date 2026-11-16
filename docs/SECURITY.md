# Safety and privacy invariants

1. Original installed plugin APKs and licenses are never modified or bypassed.
2. No feature hooks, su, persistent service, Device Admin, accessibility permissions or privileged permission requests in bootstrap.
3. No log content from notifications, UI trees, script arguments, SSH credentials, Tasker variables or JSON payloads.
4. No broad SystemUI or system_server scope in initial `xposedscope`; re-verify target package ID, APK version and ROM fingerprint before future privileged adapter registration.
5. Never retry unknown-state Termux:Tasker commands or destructive AutoInput gestures automatically.
6. No permission/notification-listener/accessibility consent bypass or MediaProjection consent bypass.
7. Review root helper operations separately, with verified caller identity, replay protection, explicit consent and bounded timeouts.
8. All future feature toggles default off and fail closed; recovery and module disable must be possible without SystemUI.
9. Logs are private, opt-in exported and user-reviewable; never copy into clipboard automatically.
10. Native Devtool is sole mutation authority for overlays; no manual patch, reset, rebase, force push or source repinning.
