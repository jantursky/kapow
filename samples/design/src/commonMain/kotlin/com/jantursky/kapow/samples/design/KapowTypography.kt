package com.jantursky.kapow.samples.design

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.jantursky.kapow.samples.design.resources.Res
import com.jantursky.kapow.samples.design.resources.dm_sans
import com.jantursky.kapow.samples.design.resources.geist
import com.jantursky.kapow.samples.design.resources.geist_mono
import org.jetbrains.compose.resources.Font
import org.jetbrains.compose.resources.FontResource

/** The compact part of the type scale in design/DESIGN.md that the Compose widgets use. Weights stay at 500 or below. */
@Immutable
class KapowTypography(
    val headingSm: TextStyle,
    val subheading: TextStyle,
    val body: TextStyle,
    val label: TextStyle,
    val caption: TextStyle,
    val code: TextStyle,
    /** Tabular figures for timings and frame times. */
    val numeric: TextStyle,
)

/** The fonts are variable files, so one resource per family serves every weight. */
@OptIn(ExperimentalTextApi::class)
@Composable
private fun variableFamily(
    font: FontResource,
    vararg weights: FontWeight,
): FontFamily =
    FontFamily(
        weights.map { Font(font, it, variationSettings = FontVariation.Settings(FontVariation.weight(it.weight))) },
    )

@Composable
internal fun rememberKapowTypography(): KapowTypography {
    val sans = variableFamily(Res.font.dm_sans, FontWeight.Normal, FontWeight.Medium)
    val heading = variableFamily(Res.font.geist, FontWeight.Medium)
    val mono = variableFamily(Res.font.geist_mono, FontWeight.Normal)
    return remember(sans, heading, mono) { kapowTypography(sans, heading, mono) }
}

private fun kapowTypography(
    sans: FontFamily,
    heading: FontFamily,
    mono: FontFamily,
): KapowTypography {
    fun style(
        family: FontFamily,
        size: Int,
        line: Int,
        weight: FontWeight,
        tracking: Float = 0f,
    ) = TextStyle(
        fontFamily = family,
        fontSize = size.sp,
        lineHeight = line.sp,
        fontWeight = weight,
        letterSpacing = tracking.sp,
    )

    val body = style(sans, 16, 24, FontWeight.Normal)
    return KapowTypography(
        headingSm = style(heading, 24, 32, FontWeight.Medium),
        subheading = style(sans, 18, 27, FontWeight.Medium),
        body = body,
        label = style(sans, 14, 20, FontWeight.Medium),
        caption = style(sans, 13, 20, FontWeight.Medium, 0.33f),
        code = style(mono, 13, 20, FontWeight.Normal),
        numeric = body.copy(fontFeatureSettings = "tnum"),
    )
}
