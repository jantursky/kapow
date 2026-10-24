// The gallery as an Android app: MainActivity hosts GalleryApp from :samples:app.
// Run it with ./gradlew :samples:androidApp:installDebug, or the "Gallery (android)" run configuration.
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.jantursky.kapow.samples.android"
    // multihaptic's Android artifact asks for API 37.2 at compile time.
    compileSdk {
        version = release(37) { minorApiLevel = 2 }
    }

    defaultConfig {
        applicationId = "com.jantursky.kapow.samples.gallery"
        minSdk =
            libs.versions.android.minSdk
                .get()
                .toInt()
        targetSdk =
            libs.versions.android.targetSdk
                .get()
                .toInt()
        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(project(":samples:app"))
    implementation(libs.androidx.activity.compose)
}
