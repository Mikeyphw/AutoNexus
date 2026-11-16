# Native Devtool v3.1+ consolidated preflight delivery

AutoNexus is delivered only by Devtool's immutable native launcher: exact source/origin/branch/HEAD/tree/blob/payload verification, native adoption scaffold and validation, atomic apply/commit, durable session, explicitly authorized push/PR and required-CI observation. This document accompanies the **next** proposed preflight milestone, and is not proof that it has been applied.

**Target:** `Mikeyphw/AutoNexus` `feature/autonexus-bootstrap` starting HEAD `779a76ee1126de52fc09adb759997b03f48c29d6`, tree `75c923e6cafca5c473db2814292f69b0088afd66`; PR #1, base `main`. The source must remain exactly at these pins when the package is built; refuse drift rather than repin this immutable artifact.

**Devtool source at authoring:** `Mikeyphw/devtool` `main` `c3f766e9252391779a854bb4bcfd47cef077aefe`, tree `08d8a07a8ee7cd8bb5dc1a19880cb718a2b08689`, `devtool/launcher_contract_v3.py` Git blob `74361e67f930c591955ff9acf699b81098df8702`, committed CLI schema blob `fdb4d7ad690a824359b0bd1f236946c0368cc67a`. Do not use a changed Devtool main without rebuilding/reattesting the immutable package; the builder must use the currently verified compatible exact pinned implementation.

The payload ZIP has `delivery/plan.json` with S7 schema v1, a `payload/` subtree and exact path/preimage/payload inventory. Extract into a NEW outside-repository directory and build with the native `devtool.launcher_contract_v3 build` command. Because native RUN-V3 and cleanup receipts already exist in the operator's original checkout, use a separate clean checkout of the *same* feature branch for the exact source match; do not delete these receipts or force `git clean`. Do not treat checkout as manual overlay application.

Native package flow from the clean checkout:

```zsh
export DEVTOOL_EXECUTION_PRESENTATION=plain DEVTOOL_JOB_UI=plain
export DEVTOOL_TEXTUAL_APPLY_HOST=0 DEVTOOL_TEXTUAL_EXECUTION_HOST=0
python3 -B -m devtool.launcher_contract_v3 build --repo TARGET --devtool-repo DEVTOOL --plan PLAN --payload PAYLOAD --output NEW_PACKAGE.pyz --name autonexus-preflight --commit-message 'feat: add safe-off Material 3 preflight dashboard' --pr-title 'Bootstrap unified AutoNexus LSPosed module'
python3 -B NEW_PACKAGE.pyz check --repo TARGET
python3 -B NEW_PACKAGE.pyz apply --repo TARGET --approve-remote --watch-ci
```

The actual one-command Termux invocation accompanies the ZIP handoff. Running `check` uses native scaffold+validator but does not apply. `apply` performs commit, durable session, push/PR and optional native required-CI observation. The committed policy exists before apply and is required. If any native operation reports a committed HEAD or a session ID, preserve both and **resume `status`/`deliver`/`ci` by that ID instead of reapplying**.

**Acceptance levels:** authoring source static/JVM-limited tests; native validation; new hosted Android/Compose build and debug APK; physical Android/LSPosed loading; release signing. These levels are not interchangeable. Package ZIP hash proves integrity, not authenticity or release qualification. Never claim device acceptance from CI alone.
