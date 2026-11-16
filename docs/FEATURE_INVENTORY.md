# AutoNexus current feature inventory

This table distinguishes **source implemented** from **hosted, physical and release acceptance**. A passed earlier hosted build does not accept unbuilt changes.

| Component | Current source behavior | Still outstanding |
|---|---|---|
| Shared companion | Material 3 dashboard, package visibility, private versioned per-domain staged intent, diagnostics | Authenticated process-crossing settings IPC (only when needed) |
| AutoNotification | Exact package scope, no notification-modifying hooks | HTML restoration, complete editors, optional verified SystemUI adapter |
| AutoInput | Exact package scope, no UI/gesture hooks | Actions v2, UI query, gestures, screenshot, explicit root fallback |
| AutoTools | Exact package scope, no JSON/secure settings behavior changes | Full 33 editors, JSON/visual Web Screen/Dialog studios, permitted secure settings |
| Termux:Tasker TT-F0 | Verified-package read-only receiver/dispatch/result/callback timing and redacted LSPosed log; companion readiness | Physical traces, host acceptance proof, proven non-response repair |
| Termux:Tasker TT-E1 | Eight-field in-plugin editor; explicit save/discard/stock fallback; bounded activity-state recovery; conservative validation using original numeric constants | Physical save/restore equivalence; browser, argument studio, complete rich UI; specific IPC repair after evidence |
| Hosted CI | Exact source SHA checkout, static/JVM/Android lint/DEX gates and temporary debug artifact | New HEAD hosted checks, physical device acceptance and production signing |

**Tasker IPC reliability status:** no modifying IPC fix is implemented. The reported “plugin did not respond” symptom does not by itself prove the command never executed. Automatic command replay stays prohibited. No Termux:Tasker feature requires SystemUI, a root daemon or altered Tasker/Termux APKs.
