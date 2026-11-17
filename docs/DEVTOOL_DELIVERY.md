# TT-E2 native Devtool v3.1+ delivery

Current milestone uses the single consolidated reviewed native overlay and the
Python entry `AutoNexus_TTE2_native_entry.pyz` from the handoff; this entry
validates reviewed bytes and calls Devtool’s own native Python launcher, not a shell wrapper.

- The target repository, named feature branch, initial HEAD/tree and changed-file
  Git blobs remain immutable. Never silently repin the AutoNexus target.
- Devtool runtime comes dynamically from the latest verifiable clean `main` of
  `Mikeyphw/devtool` and **is not hardcoded to a Devtool commit**. The native
  launcher checks its own v3.1+ same-major compatibility and records actual
  runtime HEAD/tree/blobs in its receipts.
- The entrypoint independently verifies the reviewed outer-package SHA-256,
  complete 19-file source inventory, per-file SHA-256, changed-file preimages,
  exact target Git tree and branch before native building. Tampering refuses.
- Keep all four `DEVTOOL_*` plain terminal selectors. No Textual/full-screen
  hosting, clipboard writes, manual worktree patch/apply, non-native commits,
  resets, rebases, force-pushes, merges, Gradle-on-device or release claims.
- Native launcher `build`, `check`, then first-time `apply --approve-remote
  --watch-ci` own the transaction. After commit, resume native durable
  `status`/`deliver`/`ci`, **never** reapply. The entrypoint refuses if target
  HEAD has advanced; inspect the durable session first.

TT-E2 includes only Termux:Tasker safe editor enhancements. Android hosted CI,
LSPosed device behavior and release qualification are separate acceptance steps.
Prior version-/handoff-specific launcher instructions are historical and **not**
current execution gates.
