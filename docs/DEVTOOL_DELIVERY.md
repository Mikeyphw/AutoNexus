# Native Devtool v3.1+ handoff — NO APPLY PERFORMED

Verified source: `Mikeyphw/devtool` `main` commit `af8b0936550016dc72f3facc7d2eabeef8216ddc`; `main` tree `1326e99521acf53a134a1baa2c9d862c8cb68f36`; `docs/DEVTOOL_LAUNCHER_CONTRACT_V3.md` blob `9962901d77143d0ed9837b0f8acf7854ba3a1dc2`; launcher module `devtool/launcher_contract_v3.py` blob `8f7ab603bb458068a3b4d800f3b90403fbee1d55`. Reverify main HEAD, tree and blob at delivery time—do not silently repin immutable artifacts.

**Blocker at packaging:** `Mikeyphw/AutoNexus` did not exist on GitHub, and there was no target named-branch HEAD/tree. The v3.1 native builder cannot produce a valid source-pinned `.pyz` without that. This archive is *a reviewed data-only payload for native delivery*, NOT a signed overlay, `.pyz`, committed change or hosted result.

The payload manifest is `delivery/plan.json`, with S7 schema v1: `README.md` is **modify**, every other path is **add**. It assumes a clean named branch created with GitHub’s initial README only. Devtool will pin and verify the actual committed README preimage at build time. If any other payload path already exists or the branch/origin differs, the builder must refuse; review and regenerate the plan against the actual target HEAD. Never copy these files over an existing worktree to bypass Devtool.

### One-time clean repository anchor (not an overlay or feature mutation)

GitHub CLI provides a non-manual-patch repository genesis with a named default branch and existing committed README:

```sh
mkdir -p "$HOME/Code" && cd "$HOME/Code" &&   gh repo create Mikeyphw/AutoNexus --private --add-readme --clone
```

If it already exists, **do not recreate it**; confirm the exact remote, HEAD and worktree status instead. The default branch must be `main` for the planned PR base; otherwise regenerate the approved delivery base after inspection. Repository initialization does not validate, apply or deliver any AutoNexus code.

### Native launcher build command (after approved creation of the target repository and clean named branch)

From the extracted bundle root (where `delivery/plan.json` and `payload/` are present), run:

```sh
export DEVTOOL_EXECUTION_PRESENTATION=plain DEVTOOL_JOB_UI=plain \
  DEVTOOL_TEXTUAL_APPLY_HOST=0 DEVTOOL_TEXTUAL_EXECUTION_HOST=0
python3 -B -m devtool.launcher_contract_v3 build \
  --repo "$HOME/Code/AutoNexus" \
  --devtool-repo "$HOME/Code/devtool" \
  --plan "$HOME/storage/downloads/AutoNexus_Bootstrap/delivery/plan.json" \
  --payload "$HOME/storage/downloads/AutoNexus_Bootstrap/payload" \
  --output "$HOME/storage/downloads/AUTONEXUS_BOOTSTRAP_V31.pyz" \
  --name autonexus-bootstrap \
  --commit-message 'feat: bootstrap shared AutoNexus LSPosed module' \
  --pr-title 'Bootstrap unified AutoNexus LSPosed module'
```

Builder must verify exact target origin, branch, HEAD, tree, changed files/preimages, Devtool CLI schema and native launcher blob. Then from inside the target worktree:

```sh
python3 -B "$HOME/storage/downloads/AUTONEXUS_BOOTSTRAP_V31.pyz" check
python3 -B "$HOME/storage/downloads/AUTONEXUS_BOOTSTRAP_V31.pyz" apply --approve-remote
```

**Do not use `--watch-ci` on this first source commit if the target starting HEAD lacks a committed `config/delivery-required-ci.json`.** Devtool v3.1 rejects it before apply. Onboard target CI only with real workflow IDs after the workflow exists; create a new source-bound native session and use native `ci` to qualify the actual PR HEAD. No workflow IDs were invented in this bundle.

After a committed apply, use `status`, `resume`, `deliver`, or `ci` with exact session ID. Never replay `apply` or drift a previously built immutable package. Failure logs from launcher/native run must be preserved; on failure only, the launcher suggests an opt-in `termux-clipboard-set < PRIVATE_LOG_PATH` after reviewing secrets. No auto-copy, no Textual hosts.

**Evidence status:** local static tests and payload hashes only. Native Devtool apply, compile, emulator, device, signing, PR and hosted CI are not performed.
