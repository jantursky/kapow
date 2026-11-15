#!/usr/bin/env bash
# Publishes the four modules to Maven Local, compiles a fresh consumer project against them for the JVM and the
# browser, and builds a minified Android consumer (R8, no Kapow rules) from the same screen, so a broken POM, a missing variant or a leaked internal type fails here, not for users.
set -euo pipefail

root=$(cd "$(dirname "$0")/.." && pwd)
version=$(sed -n 's/^VERSION_NAME=//p' "$root/gradle.properties")
kotlin=$(sed -n 's/^kotlin = "\(.*\)"/\1/p' "$root/gradle/libs.versions.toml")
compose=$(sed -n 's/^compose = "\(.*\)"/\1/p' "$root/gradle/libs.versions.toml")
consumer=$(mktemp -d)
android_consumer=$(mktemp -d)
trap 'rm -rf "$consumer" "$android_consumer"' EXIT

"$root/gradlew" -p "$root" :kapow:publishToMavenLocal :kapow-crease:publishToMavenLocal \
  :kapow-multihaptic:publishToMavenLocal :kapow-test:publishToMavenLocal --quiet

mkdir -p "$consumer/src/commonMain/kotlin" "$consumer/src/jvmTest/kotlin"
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
        jvmTest.dependencies {
            implementation("io.github.jantursky:kapow-test:$version")
            implementation(kotlin("test"))
            implementation(compose.desktop.currentOs)
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

cat > "$consumer/src/jvmTest/kotlin/ConsumerTest.kt" <<'EOF'
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import com.jantursky.kapow.test.KapowTestMode
import com.jantursky.kapow.test.advanceKapowEffects
import com.jantursky.kapow.test.setKapowContent
import kotlin.test.Test
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalTestApi::class)
class ConsumerTest {
    @Test
    fun screenRunsWithEffectsOff() =
        runDesktopComposeUiTest {
            setKapowContent(KapowTestMode.Disabled) { Screen(likes = 1, unread = true, visible = true, progress = 0.5f) }
            advanceKapowEffects(100.milliseconds)
        }
}
EOF

"$root/gradlew" -p "$consumer" compileKotlinJvm compileKotlinWasmJs compileTestKotlinJvm --quiet
echo "Consumer compiled for jvm and wasmJs against io.github.jantursky:kapow, kapow-crease, kapow-multihaptic and kapow-test $version"

# An Android application that uses all four artifacts, minified by R8 in full mode with no rules of Kapow's own.
# "Builds minified and the entry classes survive" is what this proves; it does not launch the app.
agp=$(sed -n 's/^agp = "\(.*\)"/\1/p' "$root/gradle/libs.versions.toml")
mkdir -p "$android_consumer/app/src/main/kotlin"
cat > "$android_consumer/settings.gradle.kts" <<EOF
pluginManagement {
    repositories {
        google()
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

rootProject.name = "android-consumer"
include(":app")
EOF
cat > "$android_consumer/build.gradle.kts" <<EOF
plugins {
    id("com.android.application") version "$agp" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "$kotlin" apply false
}
EOF
cat > "$android_consumer/gradle.properties" <<EOF
org.gradle.jvmargs=-Xmx3g
android.useAndroidX=true
EOF
if [ -f "$root/local.properties" ]; then cp "$root/local.properties" "$android_consumer/local.properties"; fi
cat > "$android_consumer/app/build.gradle.kts" <<EOF
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "consumer.app"
    // multihaptic's Android artifact asks for API 37.2 at compile time.
    compileSdk { version = release(37) { minorApiLevel = 2 } }
    defaultConfig {
        applicationId = "consumer.app"
        minSdk = 23
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            // The stock Android rules only; nothing of Kapow's.
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation("io.github.jantursky:kapow:$version")
    implementation("io.github.jantursky:kapow-crease:$version")
    implementation("io.github.jantursky:kapow-multihaptic:$version")
    implementation("io.github.jantursky:kapow-test:$version")
    implementation("androidx.activity:activity-compose:1.13.0")
}
EOF
cat > "$android_consumer/app/src/main/AndroidManifest.xml" <<EOF
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <application android:label="consumer">
        <activity android:name="MainActivity" android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
EOF
# The activity named in the manifest must sit in the app's package, or R8 finds no entry point and removes the lot.
{ echo "package consumer.app"; echo; cat "$consumer/src/commonMain/kotlin/Consumer.kt"; } \
  > "$android_consumer/app/src/main/kotlin/Consumer.kt"
cat > "$android_consumer/app/src/main/kotlin/MainActivity.kt" <<EOF
package consumer.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.jantursky.kapow.test.KapowTestMode
import com.jantursky.kapow.test.KapowTest

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { KapowTest(KapowTestMode.Full) { Screen(likes = 1, unread = true, visible = true, progress = 0.5f) } }
    }
}
EOF

"$root/gradlew" -p "$android_consumer" :app:assembleRelease --quiet

# R8 writes every class it saw into the mapping file; a removed one is marked R8$$REMOVED. R8 may merge or inline
# small classes, so the check names classes that cannot go away while the screen draws: each effect node, the
# multihaptic handler, the test mode and the crease reader the hinge effect calls.
mapping="$android_consumer/app/build/outputs/mapping/release/mapping.txt"
for entry in com.jantursky.kapow.ChangeEffectNode com.jantursky.kapow.ConditionalEffectNode \
  com.jantursky.kapow.TransitionNode com.jantursky.kapow.multihaptic.MultihapticFeedbackKt \
  com.jantursky.kapow.test.KapowTestMode com.jantursky.crease.Hinge; do
  if ! grep "^$entry -> " "$mapping" | grep -vq 'R8\$\$REMOVED'; then
    echo "R8 removed or lost $entry" >&2
    exit 1
  fi
done
echo "Minified Android consumer built with R8 and no Kapow rules, entry classes kept"
