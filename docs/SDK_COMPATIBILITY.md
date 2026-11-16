# SDK / LSPosed compatibility decision

- Android application: `compileSdk=36`, `targetSdk=36`, `minSdk=26`; Java/JVM 17.
- Gradle 8.13; Android Gradle Plugin 8.13.2; Kotlin/Compose Compiler Gradle plugin **both pinned to 2.3.21**.
- Compose BOM **`2026.06.01`**: Compose foundation/runtime/material 1.11.4 and Material 3 1.4.0; activity-compose remains 1.13.0. This is an intentional SDK 36 / AGP 8.13 compatibility pin. The later BOM `2026.08.00` (Compose 1.12+) requires SDK 37 / AGP 9.1+ and is deliberately not used. Do not upgrade the BOM in isolation. Google Maven/repository settings already exist. The exact-HEAD hosted Android build remains the acceptance authority; a source-only pin is not proof of successful compilation.
- Android 12+ dynamic system theme when supported, static Material3 light/dark fallback on older devices.
- Legacy Xposed API `82` remains compileOnly. LSPosed/Vector loading on a real Android 16 device has **not** been proven; no modern libxposed double-entry fallback is automatically enabled.
- Only four ordinary app package scopes; no SystemUI/`system_server`, no native root executor. Installed APK reference versions are expectations from earlier supplied static analysis, not observed facts about the device.
- Preflight version `0.2.0-preflight` (versionCode 2) is for debug device acceptance, not a signed production release. A debug APK artifact is not evidence of working LSPosed scope loading.
