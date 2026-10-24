package com.jantursky.kapow.samples.gallery

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.BlindsOrientation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class CallTest {
    @Test
    fun floatsAreSourceLiterals() {
        assertEquals("45f", 45f.kt())
        assertEquals("0.5f", 0.5f.kt())
        assertEquals("0.35f", 0.35f.kt())
        assertEquals("-12.25f", (-12.25f).kt())
        assertEquals("-0.5f", (-0.5f).kt())
        assertEquals("0f", 0.001f.kt())
        assertEquals("1.1f", 1.1f.kt())
    }

    @Test
    fun valuesFormatLikeTheLibraryTakesThem() {
        assertEquals("16.dp", 16.dp.kt())
        assertEquals("2.5.dp", 2.5.dp.kt())
        assertEquals("2.seconds", 2.seconds.kt())
        assertEquals("1500.milliseconds", 1500.milliseconds.kt())
        assertEquals("450.milliseconds", 450.milliseconds.kt())
        assertEquals("Color(0xFF6B62F2)", Color(0xFF6B62F2).kt())
        assertEquals("Color(0x66FFFFFF)", Color(0x66FFFFFF).kt())
    }

    @Test
    fun aShortCallStaysOnOneLine() {
        val effect = call("ChangeEffect.shine", "angle" to 45f.kt(), "duration" to 1.seconds.kt())
        assertEquals("ChangeEffect.shine(angle = 45f, duration = 1.seconds)", effect.render())
        assertEquals("ChangeEffect.flash()", call("ChangeEffect.flash").render())
    }

    @Test
    fun aLongCallBreaksOneArgumentPerLine() {
        val effect =
            call(
                "ChangeEffect.ripple",
                "color" to Color(0x66FFFFFF).kt(),
                "shape" to "RoundedCornerShape(20.dp)",
                "origin" to "TransformOrigin.Center",
                "duration" to 600.milliseconds.kt(),
            )
        assertEquals(
            """
            ChangeEffect.ripple(
                color = Color(0x66FFFFFF),
                shape = RoundedCornerShape(20.dp),
                origin = TransformOrigin.Center,
                duration = 600.milliseconds,
            )
            """.trimIndent(),
            effect.render(),
        )
    }

    @Test
    fun aNestedCallIsIndentedUnderItsParent() {
        val effect =
            call(
                "ChangeEffect.ripple",
                "color" to Color(0x66FFFFFF).kt(),
                "shape" to "RoundedCornerShape(20.dp)",
                "origin" to "TransformOrigin.Center",
                "duration" to 600.milliseconds.kt(),
            )
        assertEquals(
            """
            Modifier.changeEffect(
                count,
                ChangeEffect.ripple(
                    color = Color(0x66FFFFFF),
                    shape = RoundedCornerShape(20.dp),
                    origin = TransformOrigin.Center,
                    duration = 600.milliseconds,
                ),
            )
            """.trimIndent(),
            changeEffectCode(effect),
        )
    }

    @Test
    fun theUsageHelpersWrapTheEffect() {
        assertEquals(
            "Modifier.conditionalEffect(isActive, ConditionalEffect.glow())",
            conditionalEffectCode(call("ConditionalEffect.glow")),
        )
        assertEquals(
            "Modifier.progressEffect(ProgressEffect.shine()) { progress }",
            progressEffectCode(call("ProgressEffect.shine")),
        )
        assertEquals(
            "KapowAnimatedVisibility(\n    visible,\n    enter = KapowEnter.iris(),\n" +
                "    exit = KapowExit.poof(),\n) {\n    // your content\n}",
            transitionCode(call("KapowEnter.iris"), call("KapowExit.poof")),
        )
        assertEquals(
            "KapowAnimatedVisibility(visible, enter = KapowEnter.pop()) {\n    // your content\n}",
            transitionCode(enter = call("KapowEnter.pop")),
        )
    }

    @Test
    fun originAndOrientationPrintAsSource() {
        assertEquals("TransformOrigin.Center", Origin.Center.kt())
        assertEquals("TransformOrigin(0f, 0f)", Origin.TopStart.kt())
        assertEquals("TransformOrigin(1f, 1f)", Origin.BottomEnd.kt())
        assertEquals("BlindsOrientation.Vertical", BlindsOrientation.Vertical.kt())
    }
}
