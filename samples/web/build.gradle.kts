import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
}

// The gallery shows the example files it runs, so the source on the page is always the code that runs.
// They live in :samples:gallery and are read at build time into sources.js, next to the fonts, the brand
// mark and the design tokens.
val siteAssets = layout.buildDirectory.dir("generated/site")
val generateSiteAssets by tasks.registering {
    val examples =
        project(":samples:gallery").layout.projectDirectory.dir(
            "src/commonMain/kotlin/com/jantursky/kapow/samples/gallery/examples",
        )
    val design = rootProject.layout.projectDirectory.dir("design")
    val fonts = project(":samples:design").layout.projectDirectory.dir("src/commonMain/composeResources/font")
    val version = providers.gradleProperty("VERSION_NAME")
    val output = siteAssets
    inputs.property("version", version)
    inputs.dir(examples)
    inputs.dir(design)
    inputs.dir(fonts)
    outputs.dir(output)
    doLast {
        val out = output.get().asFile.apply { deleteRecursively() }
        out.resolve("fonts").mkdirs()

        fun json(text: String): String =
            buildString {
                append('"')
                text.forEach { c ->
                    when (c) {
                        '"' -> append("\\\"")
                        '\\' -> append("\\\\")
                        '\n' -> append("\\n")
                        '\t' -> append("\\t")
                        '<' -> append("\\u003c")
                        else -> append(c)
                    }
                }
                append('"')
            }
        val files =
            examples.asFile
                .listFiles()
                .orEmpty()
                .filter { it.extension == "kt" }
                .sortedBy { it.name }
        val entries = files.joinToString(",\n") { "  ${json(it.name)}: ${json(it.readText())}" }
        out.resolve("sources.js").writeText(
            "window.KAPOW_VERSION = ${json(version.get())};\nwindow.KAPOW_SOURCES = {\n$entries\n};\n",
        )
        design.file("tokens.css").asFile.copyTo(out.resolve("tokens.css"))
        design.file("brand/mark.svg").asFile.copyTo(out.resolve("mark.svg"))
        design.file("brand/icon.svg").asFile.copyTo(out.resolve("favicon.svg"))
        fonts.asFile
            .listFiles()
            .orEmpty()
            .forEach { it.copyTo(out.resolve("fonts/${it.name}")) }
    }
}

kotlin {
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        outputModuleName = "web"
        browser {
            commonWebpackConfig { outputFileName = "app.js" }
        }
        binaries.executable()
    }

    sourceSets {
        wasmJsMain {
            resources.srcDir(generateSiteAssets)
            dependencies {
                implementation(project(":samples:gallery"))
                implementation(project(":kapow-multihaptic"))
            }
        }
    }
}
