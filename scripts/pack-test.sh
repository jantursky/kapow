#!/usr/bin/env bash
# Publishes the three modules to Maven Local and compiles a fresh consumer project against them for the JVM and
# the browser, so a broken POM, a missing variant or a leaked internal type fails here, not for users.
set -euo pipefail

root=$(cd "$(dirname "$0")/.." && pwd)
version=$(sed -n 's/^VERSION_NAME=//p' "$root/gradle.properties")
kotlin=$(sed -n 's/^kotlin = "\(.*\)"/\1/p' "$root/gradle/libs.versions.toml")
compose=$(sed -n 's/^compose = "\(.*\)"/\1/p' "$root/gradle/libs.versions.toml")
consumer=$(mktemp -d)
trap 'rm -rf "$consumer"' EXIT

"$root/gradlew" -p "$root" :kapow:publishToMavenLocal :kapow-crease:publishToMavenLocal \
  :kapow-multihaptic:publishToMavenLocal --quiet

mkdir -p "$consumer/src/commonMain/kotlin"
cat > "$consumer/settings.gradle.kts" <<'EOF'
pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenLocal { content { includeGroup("io.github.jantursky") } }
        google()
        mavenCentral()
    }
}

rootProject.name = "consumer"
EOF
cat > "$consumer/build.gradle.kts" <<EOF
@file:OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)

plugins {
    kotlin("multiplatform") version "$kotlin"
    id("org.jetbrains.kotlin.plugin.compose") version "$kotlin"
    id("org.jetbrains.compose") version "$compose"
}

kotlin {
    jvm()
    wasmJs { browser() }
    sourceSets {
        commonMain.dependencies {
            implementation("io.github.jantursky:kapow:$version")
            implementation("io.github.jantursky:kapow-crease:$version")
            implementation("io.github.jantursky:kapow-multihaptic:$version")
        }
    }
}
EOF
cat > "$consumer/src/commonMain/kotlin/Consumer.kt" <<'EOF'
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import com.jantursky.kapow.ChangeEffect
import com.jantursky.kapow.ConditionalEffect
import com.jantursky.kapow.KapowAnimatedVisibility
import com.jantursky.kapow.KapowEnter
import com.jantursky.kapow.KapowExit
import com.jantursky.kapow.KapowParticleLayer
import com.jantursky.kapow.LocalKapowFeedback
import com.jantursky.kapow.ParticleContent
import com.jantursky.kapow.ProgressEffect
import com.jantursky.kapow.changeEffect
import com.jantursky.kapow.conditionalEffect
import com.jantursky.kapow.crease.hingeEffect
import com.jantursky.kapow.kapowTransition
import com.jantursky.kapow.multihaptic.rememberMultihapticFeedback
import com.jantursky.kapow.progressEffect
import kotlin.time.Duration.Companion.seconds

@Composable
fun Screen(likes: Int, unread: Boolean, visible: Boolean, progress: Float) {
    CompositionLocalProvider(LocalKapowFeedback provides rememberMultihapticFeedback()) {
        KapowParticleLayer {
            Box(
                Modifier
                    .changeEffect(likes, ChangeEffect.spray(ParticleContent.shape()))
                    .conditionalEffect(unread, ConditionalEffect.repeat(ChangeEffect.pulse(), every = 2.seconds))
                    .progressEffect(ProgressEffect.iris()) { progress }
                    .hingeEffect(ProgressEffect.shine()),
            )
            KapowAnimatedVisibility(visible, enter = KapowEnter.pop(), exit = KapowExit.poof()) {
                Card(this)
            }
        }
    }
}

@Composable
fun Card(scope: AnimatedVisibilityScope) {
    Box(Modifier.kapowTransition(scope, KapowEnter.iris(), KapowExit.blinds()))
}
EOF

"$root/gradlew" -p "$consumer" compileKotlinJvm compileKotlinWasmJs --quiet
echo "Consumer compiled for jvm and wasmJs against io.github.jantursky:kapow, kapow-crease and kapow-multihaptic $version"
