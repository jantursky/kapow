import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// The Compose part of the effect gallery: the examples, their controls and the catalog. The web page
// (:samples:web) and the desktop window (:samples:desktop) each put their own chrome around it; the phone and
// tablet apps (:samples:app with :samples:androidApp and samples/iosApp) use MobileGallery.
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.android.kmp.library)
}

kotlin {
    android {
        namespace = "com.jantursky.kapow.samples.gallery"
        compileSdk =
            libs.versions.android.compileSdk
                .get()
                .toInt()
        minSdk =
            libs.versions.android.minSdk
                .get()
                .toInt()
        compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
    }

    jvm { compilerOptions { jvmTarget.set(JvmTarget.JVM_17) } }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs { browser() }

    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            api(project(":samples:design"))
            api(project(":kapow"))
            implementation(libs.compose.animation)
            // System back (Android) and the edge swipe (iOS) in MobileGallery.
            implementation(libs.navigationevent.compose)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
