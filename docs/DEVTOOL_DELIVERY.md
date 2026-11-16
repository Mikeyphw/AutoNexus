# AutoNexus native Devtool v3.1+ — consolidated CI repair

## Pinned starting source

- Repository: `Mikeyphw/AutoNexus`, branch `feature/autonexus-bootstrap`, exact HEAD `765371659edcc8439e395ccddaccb0956e68b358`, tree `0320996f41ffbefda3098fa63e1f0ee13a2d7cad`. PR #1 is open and draft.
- Devtool: `Mikeyphw/devtool` `main` exact HEAD `c3f766e9252391779a854bb4bcfd47cef077aefe`, tree `08d8a07a8ee7cd8bb5dc1a19880cb718a2b08689`, launcher module blob `74361e67f930c591955ff9acf699b81098df8702`; contract doc blob `65bd5e0e44dcb6b2e42acebcef4c6a746bebff68`. No automatic immutable package repinning.
- Failure evidence: hosted AutoNexus source gate run `43847118183`, job `133229970189` failed during Gradle build-script compilation at `app/build.gradle.kts:27` (`kotlinOptions { jvmTarget = "17" }`), before Android compilation, tests, lint or APK inspection.

## Consolidated contents

One bounded Devtool S7 plan (`delivery/plan.json`) and matching separate payload: Kotlin compiler DSL fix, current static security/DEX parser tests, CI source gate, actual required-CI policy, and current docs. No deletions, security bypasses, edited original plugin APKs, SystemUI hooks or root executors. The payload ZIP is **not** a native Devtool overlay or a launcher itself; only `devtool.launcher_contract_v3 build` can create the immutable `.pyz` after checking the exact real target checkout.

## Native handoff

Run the included `AutoNexus_CI_Repair_Deliver.zsh` from Termux after verifying its artifact SHA-256. It uses a **fresh, separately located clean clone of the same feature branch**, because the existing user's working tree contains preserved Devtool RUN-V3 and cleanup receipts. It must never remove or hide those receipts or overwrite user files. The immutable launcher is built against this clean clone at the exact pinned feature SHA/tree, then runs `check` and a single `apply --approve-remote` (no reapply). The new native push/PR is reconciled with existing PR #1; no force push, rebase, reset, or automatic merge.

This overlay introduces `config/delivery-required-ci.json` for the first time. The new file is not yet committed at the **source** HEAD, so CI qualification cannot be performed by the precommit policy check. Following the committed apply, the delivery wrapper establishes a **new** native source-bound session on the resulting commit and runs native `delivery session ci` with the committed policy. Missing/failed registration remains non-green; prior green checks do not qualify a new SHA. The native launcher keeps its own original durable session.

The script prints all native terminal output and never makes clipboard calls, launches Textual or claims a hosted/device pass that was not observed. On an unsuccessful postcommit stage, use the printed session ID with native `status`/`resume`/`report`; **never rerun apply**.

## Qualification

This package is validated locally for source syntax, static and negative Python tests, payload/hash provenance, and Zsh-compatible stage structure. Native Devtool check, atomic apply/commit, hosted CI, Android Gradle build, LSPosed device and release qualification are unperformed by the package creator.
