import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// The effect gallery in a desktop window: the shared examples of :samples:gallery with a sidebar and a top bar.
// Run it with ./gradlew :samples:desktop:run, or :samples:desktop:hotRunJvm to edit the examples live.
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
}

kotlin {
    jvm { compilerOptions { jvmTarget.set(JvmTarget.JVM_17) } }

    sourceSets {
        jvmMain.dependencies {
            implementation(project(":samples:gallery"))
            implementation(compose.desktop.currentOs)
        }
        jvmTest.dependencies {
            implementation(libs.compose.ui.test)
            implementation(kotlin("test"))
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.jantursky.kapow.samples.desktop.MainKt"
    }
}
