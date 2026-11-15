import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.roborazzi) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.kmp.library) apply false
    alias(libs.plugins.kover) apply false
    alias(libs.plugins.dokka)
    alias(libs.plugins.maven.publish) apply false
    alias(libs.plugins.binary.compatibility.validator) apply false
    alias(libs.plugins.spotless)
}

// One API reference for the four published modules, built with ./gradlew dokkaGenerate. A public symbol without
// KDoc is a warning, and warnings fail the build.
dependencies {
    dokka(project(":kapow"))
    dokka(project(":kapow-crease"))
    dokka(project(":kapow-multihaptic"))
    dokka(project(":kapow-test"))
}

dokka {
    moduleName.set("Kapow")
    dokkaPublications.configureEach { failOnWarning.set(true) }
}

subprojects {
    plugins.withId("org.jetbrains.dokka") {
        extensions.configure<org.jetbrains.dokka.gradle.DokkaExtension> {
            dokkaSourceSets.configureEach { reportUndocumented.set(true) }
            dokkaPublications.configureEach { failOnWarning.set(true) }
        }
    }
}

spotless {
    kotlin {
        target("kapow*/src/**/*.kt", "samples/*/src/**/*.kt")
        targetExclude("**/build/**", "samples/docs-check/src/**")
        ktlint(libs.versions.ktlint.get())
    }
    kotlinGradle {
        target("*.gradle.kts", "kapow*/*.gradle.kts", "samples/*/*.gradle.kts")
        targetExclude("**/build/**")
        ktlint(libs.versions.ktlint.get())
    }
}

// Repositories are declared in settings only, so the Wasm toolchains resolve from the ivy
// repositories there instead of ones the Kotlin plugin would add to each project.
allprojects {
    plugins.withType<org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsPlugin> {
        the<org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsEnvSpec>().downloadBaseUrl.set(null as String?)
    }
    plugins.withType<org.jetbrains.kotlin.gradle.targets.wasm.yarn.WasmYarnPlugin> {
        the<org.jetbrains.kotlin.gradle.targets.wasm.yarn.WasmYarnRootEnvSpec>().downloadBaseUrl.set(null as String?)
    }
    @OptIn(ExperimentalWasmDsl::class)
    plugins.withType<org.jetbrains.kotlin.gradle.targets.wasm.binaryen.BinaryenPlugin> {
        the<org.jetbrains.kotlin.gradle.targets.wasm.binaryen.BinaryenEnvSpec>().downloadBaseUrl.set(null as String?)
    }
}
