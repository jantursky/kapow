package com.jantursky.kapow

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.unit.dp
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Compares additive sparks with a reference recorded before the blend layer was bounded. */
@OptIn(ExperimentalTestApi::class)
class AdditiveSparksGoldenTest {
    private var value by mutableIntStateOf(0)

    private fun sparks() =
        ChangeEffect.spray(
            particles = ParticleContent.shape(ParticleShape.Circle, listOf(Color(0xFFFF8A00)), 16.dp),
            count = 30,
            physics = ParticlePhysics.of(blendMode = BlendMode.Plus),
        )

    @Test
    fun theBlendedPixelsAreWhatTheyWereWithAnUnboundedLayer() =
        runEffectTest {
            setEffectContent {
                Box(Modifier.fillMaxSize().background(Color.Black)) {
                    SolidElement(Modifier.offset(150.dp, 150.dp).changeEffect(value, sparks()))
                }
            }
            value++
            mainClock.advanceTimeBy(250)
            val image = capture()
            // Additive orange over black: some pixels are brighter than black, and the corners stay black.
            var lit = 0
            for (y in 0 until 400) for (x in 0 until 400) if (image.colorAt(x, y) != Color.Black) lit++
            assertTrue(lit > 200, "sparks are drawn: $lit")
            assertEquals(Color.Black, image.colorAt(2, 2))
            // The reference was recorded with the unbounded layer of 1.1.0. Bounding the layer moves its origin,
            // which shifts anti-aliased edges by a few levels on well under 1 % of the pixels, so the
            // comparison allows that much and nothing more.
            val reference = ImageIO.read(File("src/jvmTest/screenshots/additive_sparks_250.png"))
            var changed = 0
            var worst = 0
            for (y in 0 until 400) {
                for (x in 0 until 400) {
                    val now = image.colorAt(x, y)
                    val was = Color(reference.getRGB(x, y))
                    val gap =
                        maxOf(
                            abs(now.red - was.red),
                            abs(now.green - was.green),
                            abs(now.blue - was.blue),
                        )
                    val levels = (gap * 255).roundToInt()
                    if (levels > 0) changed++
                    worst = maxOf(worst, levels)
                }
            }
            assertTrue(changed < 400 * 400 * 0.005, "pixels that differ from 1.1.0: $changed")
            assertTrue(worst <= 80, "the largest difference in one channel: $worst")
        }
}
