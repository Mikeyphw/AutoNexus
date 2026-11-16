# Termux:Tasker current implementation — TT-F0 + TT-E1

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

**NO FULL EDITOR CLAIM:** This is a functioning source-level editor bridge covering all eight original fields; it is *not* yet the complete roadmap's file browser, exact original argument-token preview, dynamic icon/theme system, safe preview/test studio, or visually redesigned multiple-screen configuration. Those require further compatibility and device acceptance.

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
