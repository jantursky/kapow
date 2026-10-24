import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Compiles the Kotlin samples from the docs and READMEs (copied in by scripts/doc-samples.py).
// They live in jvmTest so samples showing tests compile with the test libraries.
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
}

kotlin {
    jvm { compilerOptions { jvmTarget.set(JvmTarget.JVM_17) } }

    sourceSets {
        jvmTest.dependencies {
            implementation(project(":kapow"))
            implementation(project(":kapow-crease"))
            implementation(project(":kapow-multihaptic"))
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.ui)
            implementation(libs.compose.animation)
            implementation(libs.compose.ui.test)
            implementation(compose.desktop.currentOs)
            implementation(kotlin("test"))
        }
    }
}

// The samples only need to compile; there are no tests to run.
tasks.withType<Test>().configureEach { failOnNoDiscoveredTests = false }
