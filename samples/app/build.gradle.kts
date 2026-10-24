import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// The gallery as a phone and tablet app: MobileGallery from :samples:gallery with multihaptic haptics. The
// Android app (:samples:androidApp) depends on it; on iOS it is the static GalleryKit framework that
// samples/iosApp links, with MainViewController() as its entry.
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.android.kmp.library)
}

kotlin {
    android {
        namespace = "com.jantursky.kapow.samples.app"
        // multihaptic's Android artifact asks for API 37.2 at compile time.
        compileSdk {
            version = release(37) { minorApiLevel = 2 }
        }
        minSdk =
            libs.versions.android.minSdk
                .get()
                .toInt()
        compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
    }

    listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "GalleryKit"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":samples:gallery"))
            implementation(project(":kapow-multihaptic"))
        }
    }
}
