# Security and privacy invariants

1. Original Tasker plugin APKs, their licenses, preferences and task bundles are never modified, re-signed or bypassed.
2. Default **behavior OFF, root OFF, SystemUI OFF and system_server OFF** remain compile-time invariants. A staged UI request cannot override them.
3. Only the four allowed app scopes exist in `xposedscope`. No SystemUI/Android system/Termux/Tasker host scope or permission escalation.
4. Two per-domain preferences are module-app-private (`MODE_PRIVATE`), schema-versioned and committed atomically. Unknown/newer/malformed data disables controls; never overwrite incompatible data to force a state.
5. No exported settings provider, cross-process preference share, external settings file, network telemetry, background service or periodic collection. Injected processes do not consume the staged settings.
6. Local diagnostics include only package visibility, installed version and basic OS API/release. LSPosed activation is explicitly UNKNOWN in the companion app; don't misstate a requested switch as an effective hook.
7. No logging or sharing notification contents, accessibility trees, Tasker variables, script arguments, tokens or other sensitive payloads. Device screenshots and LSPosed logs must be reviewed/redacted before sharing.
8. No default clipboard copying or synthetic/log-masking native terminal UI. Devtool uses native plain/Rich stdout and `--no-clipboard`, respects NO_COLOR, CLICOLOR=0 and TERM=dumb.
9. No retry of an unknown-state Tasker command or AutoInput gesture without explicit user review; no permission, accessibility, MediaProjection or Direct Boot bypass.
10. Any future root or behavior adapter requires narrow privilege grants, authenticated IPC, version/ROM verification, opt-in, independent fault isolation, rollback and actual physical acceptance.
11. Debug APK artifacts are non-release test binaries, short-lived (7 days), bound to source SHA and packaged with SHA-256. No claim of release signing or device validity.
12. Native Devtool is the sole overlay mutation/delivery authority. No manual patches to the user's worktree, resets, rebases, force pushes, silent repins or destruction of native receipts.

## TT-F0 trace privacy

Only fixed stage/outcome enums, process category, local sequence and elapsed
time are emitted. No Tasker action content is read from intents or bundles.
Exact installed signing certificate, version name and code are attested before
method hooks install. The module never starts services, repeats commands,
grants `RUN_COMMAND` or claims that a local `signalFinish` return is accepted
by the Tasker host. See `docs/TERMUX_TASKER_IMPLEMENTATION.md`.

## TT-E1 configuration safety

- Enhanced editor is version and F-Droid signer gated to the original Termux:Tasker 0.9.0 main process. It never runs scripts, grants permissions, changes Termux policy or adds Tasker bundle fields.
- All inputs remain in-process, with bounded versioned Activity saved-state restoration; none enter read-only `TT_TRACE` logs.
- Back/Home cannot invoke the implicit stock Save while the enhanced editor is active. Discard marks the legacy cancellation flag. Original fallback preserves untouched native controls. Unknown serialized keys and unavailable numeric constants refuse enhanced Save.
- No root, SystemUI, automatic command replay, or Tasker host IPC rewriting is introduced.

## TT-E2 read-only editor affordances

- The argument preview uses the exact original v0.9.0 `ArgumentTokenizer` in the attested process, only after the operator taps Preview; the token strings remain solely on that screen. A failure to load or parse is not treated as successful validation and never affects Tasker save semantics. No clipboard or log export.
- The opt-in local script explorer enumerates at most 60 entries and five folder levels under the canonical Termux scripts directory; it rejects symlinks and escapes, performs no reads of file bodies and never requests root or Android permissions. It must report inaccessible paths rather than bypassing the plugin UID.
- Neither preview nor browsing transmits, executes, rewrites or automatically saves a Tasker action. All runtime `RUN_COMMAND` permissions, original serialization and fail-closed compatibility gates remain authoritative.

TT-E2 hardening: script navigation bounds total inspection to 256 entries, including rejected/symlink entries; it also refuses symlinked path components even when their destination stays inside the approved root. Enhanced-to-Original discards only its transient draft and does not write stock fields. Reopening reads latest stock fields to avoid stale draft overwrite. The handoff wrapper independently verifies package SHA-256 plus all changed-file hashes and preimages before calling native Devtool.
