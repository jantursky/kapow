package com.jantursky.kapow

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** The built-in particle shapes. */
public enum class ParticleShape { Circle, Square, Star, Heart, Sparkle }

/**
 * What a particle effect draws for each particle: a built-in [shape][ParticleContent.shape], a
 * [painter][ParticleContent.painter] such as an icon, or a short [text][ParticleContent.text] like "+1".
 */
@Immutable
public sealed class ParticleContent {
    internal data class Shape(
        val shape: ParticleShape,
        val colors: List<Color>,
        val size: Dp,
    ) : ParticleContent()

    internal data class Image(
        val painter: Painter,
        val size: Dp,
        val tint: Color?,
    ) : ParticleContent()

    internal data class Text(
        val text: String,
        val style: TextStyle,
    ) : ParticleContent()

    public companion object {
        /** Warm amber, coral, violet and cobalt. */
        public val DefaultColors: List<Color> =
            listOf(Color(0xFFFFB547), Color(0xFFFF6B5A), Color(0xFF6B62F2), Color(0xFF3D7BFF))

        /** A [ParticleShape], colored from [colors] in turn. */
        public fun shape(
            shape: ParticleShape = ParticleShape.Sparkle,
            colors: List<Color> = DefaultColors,
            size: Dp = 10.dp,
        ): ParticleContent {
            require(colors.isNotEmpty()) { "colors must not be empty" }
            return Shape(shape, colors, size)
        }

        /** A [painter] drawn at [size], optionally tinted. Remember the painter so it isn't recreated. */
        public fun painter(
            painter: Painter,
            size: Dp = 18.dp,
            tint: Color? = null,
        ): ParticleContent = Image(painter, size, tint)

        /** A short label such as "+1". */
        public fun text(
            text: String,
            style: TextStyle = TextStyle(color = Color(0xFFFFB547), fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
        ): ParticleContent = Text(text, style)
    }
}
