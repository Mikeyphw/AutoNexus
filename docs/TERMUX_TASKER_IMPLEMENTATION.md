# Termux:Tasker TT-F0 — read-only IPC trace and readiness (one shared module)

## Verified implementation and deliberate boundary

**Source contract:** The original user-supplied `Termux꞉Tasker_0.9.0.apk` has
SHA-256 `f8cd889f9987759bcaeb68b8a8ba337085b5938b97a5cd9f05a4e610d411ef5e`.
Its F-Droid signing certificate SHA-256 is
`228fb2cfe90831c1499ec3ccaf61e96e8e1ce70766b9474672ce427334d41c42`.
Signatures of upstream tagged v0.9.0 Java methods were inspected against the
verified `termux/termux-tasker` source. The installed package and physical
LSPosed runtime have **not** been independently attested at authoring time.

TT-F0 adds **observational, not behavior-changing** method hooks to the same
AutoNexus APK, conditional on the user enabling its LSPosed scope for
`com.termux.tasker` and an exact `0.9.0` / `1002` / signer match.
Incompatible or unreadable installations are left untouched.

- `com.termux.tasker:background`: `FireReceiver.onReceive(Context, Intent)`
  entry/return, and `PluginUtils.sendExecuteIntentToExecuteService(...)`
  entry/return.
- Main plugin process: `PluginResultsService.onHandleIntent(Intent)`,
  `PluginUtils.sendPendingResultToPluginHostApp(Context, Intent)`, and
  `TaskerPlugin.Setting.signalFinish(Context, Intent, int, Bundle)`
  entry/return.
- Process-local diagnostic sequences distinguish status and duration;
  each process logs at most 160 events per load. Stage names and outcomes
  are fixed enums. No scripts, command paths, arguments, stdin, stdout,
  stderr, intents, bundle contents, exception messages, or Tasker variables
  are read or logged. No cross-process correlation or host acknowledgment
  is invented. LSPosed's own log access/retention governs the records.
- The companion app adds a Termux:Tasker diagnostics page with installed
  identity verification, Tasker/Termux visibility, Tasker's `RUN_COMMAND`
  permission check, user-unlock state and a safe reproduction protocol.
  It cannot inspect the plugin's private script directory or read the
  LSPosed log from its own UID.

**NO IPC FIX CLAIM:** TT-F0 does not repair missing Tasker replies, change
PendingIntent/ordered broadcast semantics, start Termux services, or retry a
script. `signalFinish()` returning true is a *local delivery attempt*, not
Tasker host acceptance. Missing receiver events do not prove non-dispatch.

**NO FULL EDITOR CLAIM:** The original Termux:Tasker editor and action bundle
remain untouched. The new companion screen is not a replacement Tasker editor.
Full editor restoration, Save/Discard and bundle roundtrip controls belong
to the next version-gated TT-F1 stage, after obtaining physical traces.

**NO PHYSICAL DEVICE TRACE CLAIM:** Source and hosted CI qualification do not
prove this hook chain has loaded on Android 16; the operator must enable the
LSPosed scope, restart target processes and collect one harmless Tasker run.

## Execution and privacy boundaries

- Exactly four ordinary app scopes; no Tasker host, Termux app, SystemUI or
  system_server scope added. RootPolicy and `RuntimeGate` remain fail-closed.
- Neither app nor module obtains `RUN_COMMAND`, root, storage or log-reading
  permissions. A denied Tasker grant is reported, never silently granted.
- No new exported receiver, service, provider, or cross-app settings channel.
- Trace activation is via the existing user-managed LSPosed scope, **not**
  the companion app's "Request future enhancements" switch (still staged).
  Revoke the scope to deactivate tracing; changes take effect on process restart.
- `isUserUnlocked=false` is diagnostic information, not permission to bypass
  Direct Boot/credential-encrypted storage or launch scripts before unlock.
- All production Tasker actions retain stock execution; no automatic replay.

## Physical acceptance (required, not yet performed)

1. On Android 16, install the new debug APK only after confirming source SHA and
   build identity. Scope LSPosed to `com.termux.tasker`; do not scope Tasker host
   or `com.termux` for this milestone. Restart Termux:Tasker processes.
2. Confirm the dashboard identity status is exactly matched; if not, hooks must
   refuse. Confirm four other app behaviors and the stock Termux:Tasker editor
   remain unchanged with module active and disabled.
3. Choose a **single harmless** Tasker action, and run it once. Collect logs
   matching `AutoNexus TT_TRACE` with timestamps and redact unrelated entries.
   Compare `BACKGROUND` receive/dispatch and `EDITOR` result/callback stages.
4. Test normal operation, after restart, immediately after unlock, and after
   long idle; do not run potentially destructive commands more than once.
5. Verify plugin nonresponse is classified by where events stop, and verify
   a missing event is not reported as host dispatch failure. Tasker-host
   observation requires separate exact-version, explicit scope authority.
6. Check root off, no SystemUI scope, no automatic command retries, and no
   changes to original Locale/Tasker bundle keys and variables.

The next TT-F1 implementation is gated on this evidence. The full required
editor and the identified specific IPC repair will be delivered together as
one independently reviewed milestone, rather than speculative method patches.
