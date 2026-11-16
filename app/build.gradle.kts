import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "io.github.mikeyphw.autonexus"
    compileSdk = 36
    defaultConfig {
        applicationId = "io.github.mikeyphw.autonexus"
        minSdk = 26
        targetSdk = 36
        versionCode = 3
        versionName = "0.3.0-tt-f0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildTypes {
        release {
            isMinifyEnabled = false // Separate reviewed R8/keep-rules gate before release.
            isDebuggable = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures { compose = true }
    testOptions { unitTests.isReturnDefaultValues = true }
    packaging { resources.excludes += setOf("META-INF/AL2.0", "META-INF/LGPL2.1") }
}

// Kotlin Gradle Plugin 2.3+ requires the typed compilerOptions DSL.
// Keep Java and Kotlin bytecode at the same JVM 17 target.
kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    // The newer 2026.08+/09 BOM resolves Compose 1.12.x which requires SDK 37 and AGP 9.1+.
    // 2026.06.01 pins Compose core 1.11.4 / Material3 1.4.0 for SDK 36 / AGP 8.13.2.
    implementation(platform("androidx.compose:compose-bom:2026.06.01"))
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.13.0")
    compileOnly("de.robv.android.xposed:api:82")
    testImplementation("junit:junit:4.13.2")
}
