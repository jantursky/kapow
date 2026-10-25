package com.jantursky.kapow

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.NonSkippableComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.internal.ChangeRunner
import com.jantursky.kapow.internal.ConditionalRunner
import com.jantursky.kapow.internal.EffectContext
import com.jantursky.kapow.internal.ProgressRunner
import kotlin.time.Duration

/** The element color in drawing tests. */
val Cobalt = Color(0xFF3D7BFF)

/** Runs [block] in a 400 × 400 test window with a paused clock, so tests step frames themselves. */
@OptIn(ExperimentalTestApi::class)
fun runEffectTest(block: ComposeUiTest.() -> Unit) =
    runDesktopComposeUiTest(400, 400) {
        mainClock.autoAdvance = false
        block()
    }

/** Sets [content] at density 1, with full motion unless [motion] says otherwise. */
@OptIn(ExperimentalTestApi::class)
fun ComposeUiTest.setEffectContent(
    motion: KapowMotion = KapowMotion.Full,
    content: @Composable () -> Unit,
) {
    setContent {
        CompositionLocalProvider(LocalDensity provides Density(1f), LocalKapowMotion provides motion) { content() }
    }
}

/** A 100 × 100 box carrying [modifier]. */
@Composable
fun Element(modifier: Modifier) {
    Box(modifier.size(100.dp))
}

/** A solid 100 × 100 box in [color] carrying [modifier], tagged "element". */
@Composable
fun SolidElement(
    modifier: Modifier,
    color: Color = Cobalt,
) {
    Box(modifier.testTag("element").size(100.dp).background(color))
}

/** The whole test window as pixels. */
@OptIn(ExperimentalTestApi::class)
fun ComposeUiTest.capture(): ImageBitmap = onRoot().captureToImage()

/** The color at ([x], [y]) of an image. */
fun ImageBitmap.colorAt(
    x: Int,
    y: Int,
): Color = toPixelMap()[x, y]

/** Brightness from 0 to 1, enough to tell a highlight from the base color. */
val Color.luminance: Float get() = 0.2126f * red + 0.7152f * green + 0.0722f * blue

/** Counts how often the composable that owns an effect composes. */
class CompositionCounter {
    var count = 0
        private set

    // Not skippable, so it runs whenever the caller recomposes.
    @NonSkippableComposable
    @Composable
    fun Count() {
        SideEffect { count++ }
    }
}

/** A change effect that records when it fires and whether motion was reduced, and draws nothing. */
data class RecordingEffect(
    val log: MutableList<Boolean> = mutableListOf(),
    override val cooldown: Duration = Duration.ZERO,
) : ChangeEffect() {
    override fun createRunner(): ChangeRunner =
        object : ChangeRunner() {
            override fun fire(context: EffectContext) {
                log += context.isReducedMotion()
            }
        }
}

/** A change effect that records the speed and reduced-motion mode its runner is given. */
data class ContextProbe(
    val seen: MutableList<Pair<Float, ReducedMotionMode>>,
) : ChangeEffect() {
    override val cooldown: Duration get() = Duration.ZERO

    override fun createRunner(): ChangeRunner =
        object : ChangeRunner() {
            override fun fire(context: EffectContext) {
                seen += context.speed to context.reducedMotionMode
            }
        }
}

/** A conditional effect that records each call of `setActive`. */
data class ActiveProbe(
    val log: MutableList<Boolean>,
) : ConditionalEffect() {
    override fun createRunner(): ConditionalRunner =
        object : ConditionalRunner() {
            override fun setActive(
                context: EffectContext,
                active: Boolean,
            ) {
                log += active
            }
        }
}

/** A conditional effect that records the speed and fades its runner is given. */
data class ContextProbeConditional(
    val seen: MutableList<Triple<Float, Any?, Any?>>,
) : ConditionalEffect() {
    override fun createRunner(): ConditionalRunner =
        object : ConditionalRunner() {
            override fun setActive(
                context: EffectContext,
                active: Boolean,
            ) {
                seen += Triple(context.speed, context.fadeIn, context.fadeOut)
            }
        }
}

/** A progress effect that records the progress its runner would draw. */
data class ProgressProbe(
    val seen: MutableList<Float>,
) : ProgressEffect() {
    override fun createRunner(): ProgressRunner =
        object : ProgressRunner() {
            override fun ContentDrawScope.drawEffect() {
                seen += progress()
                drawContent()
            }
        }
}
