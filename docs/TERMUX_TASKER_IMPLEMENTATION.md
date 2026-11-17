# Termux:Tasker current implementation — TT-F0 + TT-E1 + TT-E2

## Verified source and compatibility boundary

The original user-supplied Termux:Tasker v0.9.0 APK SHA-256 is `f8cd889f9987759bcaeb68b8a8ba337085b5938b97a5cd9f05a4e610d411ef5e`. Its F-Droid signing certificate SHA-256 is `228fb2cfe90831c1499ec3ccaf61e96e8e1ce70766b9474672ce427334d41c42`. TT-E1 only hooks `com.termux.tasker` in its **normal editor process**, after TT-F0's versionCode `1002`, versionName `0.9.0`, package and signature attestation. An incompatible build retains the original editor.

## TT-E1 implemented: original editor bridge

The source implementation adds an *in-process Android dialog* to the original `EditConfigurationActivity`, not a replacement Termux:Tasker APK or an exported companion service. A button on the original configuration form reopens it. The modal opens by default after its original activity completes `onCreate` and binds to the original 0.9.0 field IDs:

- Command: executable, arguments (literal string), working directory, stdin.
- Execution: in-terminal, session action, custom background log level.
- Results: wait-for-result and an explanation of Tasker's host timeout / output variables.
- Diagnostics: safe explanations of plugin-side TT-F0 stages and a clear admission that host acceptance is unverified.

Draft editing is local only. **Validate & Save** checks nonempty executable, conservative size limits, numeric values against installed Termux constants, unknown serialized Tasker keys, and defers `%variables`. If validation succeeds, the editor updates the *original* v0.9.0 widgets and `mStdin`, then invokes the original activity's `finish()`, allowing the original plugin to create the Locale/Tasker bundle, string blurb, replacement-key list and result-variable metadata. It does not serialize the plugin action itself or inject a new key.

**Discard** explicitly sets the original activity's `mIsCancelled`, returns `Activity.RESULT_CANCELED` and finishes without a modified action. **Original** discards any uncommitted enhanced draft (with confirmation) and returns the original activity. **Keep Editing** leaves the draft. While enhanced mode is active, intercepted Back/Home cannot silently invoke the original `finish()` and save. When enhanced mode is disabled, stock editor behavior is left unchanged.

The injected Activity captures transient draft state using a versioned, bounded Android `onSaveInstanceState` Bundle, with no cross-package preference or file storage. The bridge fails closed if the stock field IDs or original methods are unavailable. Unknown action keys and unavailable numeric bounds prevent enhanced Save, with a visible Original fallback. Original Tasker/Termux permissions and script path policies are never bypassed.

**NO FULL EDITOR CLAIM:** TT-E1 provided a source-level bridge for the eight original fields, before TT-E2's opt-in read-only browser and original-tokenizer preview. Even with TT-E2, there is no complete file manager, dynamic icon/theme system, runnable safe-test studio or visually redesigned multiple-screen configuration. All new TT-E2 behavior remains pending hosted and device acceptance.

## TT-E2 implemented: optional Argument, Script and Result Studio

- An **explicit Preview arguments** action calls the installed, pinned `com.termux.tasker.ArgumentTokenizer.tokenize(String)` via classloader reflection. Nothing is parsed or normalized by AutoNexus. The preview shows at most 64 tokens and 8,192 input characters; oversized/malformed content or unavailable original parsing fails closed. Preview data stays in the foreground editor UI, appears only after a tap, is never logged/exported, and does not alter the raw arguments. `%variables` remain unresolved and are identified as such.
- **Browse Termux scripts** and **Choose working directory** explicitly enumerate only the standard `~/.termux/tasker` path using the original process's existing filesystem rights. Bounded listing (60 entries, five levels), symlink exclusion, canonical parent containment and asynchronous UI updates prevent privilege escalation and directory traversal. Selection stages a text value; the operator must still press Save. If the plugin cannot read Termux private files, the browser reports the limitation, does not attempt root, and preserves manual/Original entry. No files are opened or executed.
- The two mode-specific numeric controls are hidden/shown as whole field groups without clearing their staged data. Result help distinguishes `%stdout`, `%stdout_original_length`, `%stderr`, `%stderr_original_length`, `%result`, `%err`, `%errmsg`, and pending vs observed callback boundaries.
- The original stock `finish()` remains the **only serialization** mechanism. No extra keys are added to Tasker bundles, no pending intent or service dispatch is modified, no script is retried.

**NO FULL EDITOR CLAIM:** TT-E2 adds capability without the complete roadmap's semantic test studio, safe external path chooser, sampled result inspector, script favorites or root-assisted diagnostics. A script browser in the plugin UID cannot promise access to Termux-private folders.

## TT-F0 read-only IPC trace preserved

Receiver entry/exit (`:background`), dispatch helper return, result-service/return-helper events, and local `signalFinish` outcomes remain the TT-F0 closed-enum, process-local, bounded and redacted observations. No scripts, paths, arguments, stdin/stdout/stderr, Tasker variables, Bundle contents or exception messages are logged. Only stage and local duration are observed.

**NO IPC FIX CLAIM:** TT-E1 does not modify ordered broadcasts, PendingIntent, service execution, host callbacks or the Tasker host. `signalFinish()` returning true does not prove Tasker accepted the completion. Shell commands are **never** automatically replayed.

**NO PHYSICAL DEVICE TRACE CLAIM:** No source-level check, hosted build, stub compile or APK artifact proves the injected Activity or original Tasker task can save/load in the real LSPosed environment. This milestone still requires installation and physical acceptance.

## Device acceptance checklist

1. From the exact PR HEAD, verify `source-commit.txt` and debug APK hash before installing. Confirm plugin v0.9.0, versionCode 1002, and the specified F-Droid signer. In LSPosed, scope **only** `com.termux.tasker` for this domain; restart target processes.
2. Open an existing Tasker → Plugin → Termux:Tasker configuration. Verify all eight fields match original values. Unchanged Save must preserve action behavior and result variables. Compare original vs enhanced save where possible; do not infer equality merely from a successful dialog close.
3. Edit a harmless test action. Verify literal arguments, stdin including null/empty, both mode-specific numeric options, wait-for-result, `%variables`, out-of-range values, unknown keys, and excess-size guards. Verify rollback, Discard, Back/Home, Original fallback, orientation change and large font/keyboard usability.
4. Remove the LSPosed scope and confirm the original editor remains operational. Confirm no extra app permissions, provider, receiver, service, root helper, SystemUI scope or changed runtime command behavior.
5. To investigate original "plugin did not respond" symptoms, use one explicitly requested harmless Tasker run **once**. Collect redacted TT_TRACE events, their process/stage/elapsed time, the Tasker host error time and user-unlock state. Missing callbacks are not proof the command never executed.

Physical acceptance is separate from hosted build success, native Devtool receipt, and production release qualification. No specific IPC repair may be enabled until the missing stage has been verified on the device.

### TT-E2 editor transition and browse hardening

Selecting Original abandons staged enhanced-only values and keeps the current stock widgets unchanged. Reopening Enhanced first re-reads current stock fields (including stdin, terminal/result toggles and hidden-mode values) rather than recovering its previous enhanced draft. Original-vs-enhanced discard decisions compare against current stock fields. The editor-state bridge has JVM regression tests for stale-draft and reopened-stock behavior. Read-only script scanning examines at most 256 directory entries total, including skipped symlinks, displays at most 60, caps depth at 5, and refuses symlinked intermediate directories. Native command execution, IPC completion and Tasker result bundle serialization are unchanged.
