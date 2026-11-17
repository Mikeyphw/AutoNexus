# Termux:Tasker TT-F1 — native Devtool delivery (current)

Use `AutoNexus_TTF1_NativeV31.pyz` for the current TT-F1 consolidated
editor and startup-safety candidate. It embeds a reviewed immutable overlay,
**not** a handwritten overlay engine. The entry verifies target Git origin,
named branch, exact source HEAD/tree, changed-file preimage blobs and payload
SHA-256; independently verifies the reviewed ZIP before calling the *official*
Devtool Python builder and native `check`/`apply`/durable delivery/CI.

At execution, Devtool comes dynamically from the **latest compatible clean
`Mikeyphw/devtool` main**. Do not hardcode a historical Devtool commit or
silently repin the immutable AutoNexus source. The selected runtime's actual
HEAD/tree/schema/module blobs are re-attested and recorded by the official
launcher. Its native API determines compatibility; if incompatible, refuse.

Use a genuine attached terminal and supported environment selectors:

```shell
export DEVTOOL_EXECUTION_PRESENTATION=plain
export DEVTOOL_JOB_UI=plain
export DEVTOOL_TEXTUAL_APPLY_HOST=0
export DEVTOOL_TEXTUAL_EXECUTION_HOST=0
python3 -u -B "$HOME/storage/downloads/AutoNexus_TTF1_NativeV31.pyz" apply --approve-remote --watch-ci
```

The entry passes child native stdout/stderr directly and never uses Textual,
clipboard APIs, manual patch/apply or Git history rewrites. Preserve user
worktrees. A committed apply creates a durable native session: if later CI
fails, use `status`, `deliver`, `ci` or `resume` with the actual session ID,
**not** the same immutable `apply` again.

Local stub/API compilation is not hosted Android/LSPosed qualification.
Android 16 plugin autocomplete, picker, first-unlock behavior and exact
Tasker saved bundle still require on-device tests. This package does not
silently modify Tasker's startup profile or claim an automatic IPC fix.
