import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// The Compose part of the effect gallery: the examples, their controls and the catalog. The web page
// (:samples:web) and the desktop window (:samples:desktop) each put their own chrome around it.
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
}

kotlin {
    jvm { compilerOptions { jvmTarget.set(JvmTarget.JVM_17) } }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs { browser() }

    sourceSets {
        commonMain.dependencies {
            api(project(":samples:design"))
            api(project(":kapow"))
            implementation(libs.compose.animation)
        }
    }
}
