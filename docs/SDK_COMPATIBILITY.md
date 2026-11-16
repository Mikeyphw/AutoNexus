# SDK / LSPosed compatibility decision

Bootstrapped against Android 16 target API 36, Gradle 8.13, Android Gradle Plugin 8.13.2, Kotlin 2.3.21 and JDK 17. See Android AGP compatibility documentation and Kotlin Gradle plugin compatibility matrix for accepted version bounds.

The legacy Xposed API 82 is selected because Vector/LSPosed legacy compatibility is a plausible target and user runtime support is not yet inspected. The API MUST remain compile-only. A modern libxposed entry may be added in a future milestone only after real framework probing and without accidentally loading both entries twice.

`xposedscope` lists four ordinary app packages only; no SystemUI or Tasker host before version-pinned device trials. Native RootOperation is permanently deny by default in this source revision. Package version references are based on supplied APKs, not installed device evidence.
