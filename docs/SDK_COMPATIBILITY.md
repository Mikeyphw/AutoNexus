# SDK / LSPosed compatibility decision

This bootstrap targets Android 16 / SDK 36, Gradle 8.13, Android Gradle Plugin 8.13.2, Kotlin Gradle Plugin 2.3.21 and JDK 17. The Kotlin compiler is configured through the supported `kotlin { compilerOptions { jvmTarget.set(JvmTarget.JVM_17) } }` DSL to match Java source/target compatibility 17. The deprecated `android.kotlinOptions { jvmTarget = "17" }` was rejected by the hosted Gradle Kotlin DSL compiler on AutoNexus PR #1, run 43847118183.

The legacy Xposed API 82 remains compile-only pending framework/device verification. The built APK gate reads actual DEX class definitions to ensure Xposed runtime classes are not packaged, while allowing expected interface references. A modern libxposed entry must not be introduced without runtime probing and duplicate-entry protection.

LSPosed scopes remain exactly the four supplied plugin packages: AutoNotification, AutoInput, AutoTools, and Termux:Tasker. No SystemUI, Tasker host or `system_server` scope is enabled. Root operations and behavior hooks are fail-closed. These source/toolchain decisions are not proof of Android 16 device compatibility; build, unit tests, lint and device acceptance are separate.
