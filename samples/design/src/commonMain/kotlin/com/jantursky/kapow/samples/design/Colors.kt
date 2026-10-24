package com.jantursky.kapow.samples.design

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/** The palette of design/tokens.css. Dark is the primary theme; light mirrors it. */
@Immutable
class KapowColors(
    val canvas: Color,
    val panel: Color,
    val glass: Color,
    val stage: Color,
    val hairline: Color,
    val title: Color,
    val ink: Color,
    val ash: Color,
    val slate: Color,
    val action: Color,
    val onAction: Color,
    val success: Color,
    val warning: Color,
    val isDark: Boolean,
)

/** The violet is light only: glows, washes and focus. Never a fill for text or buttons. */
val KapowViolet = Color(0xFF6B62F2)

/** Default colors for the effects themselves, the four stops of the hero horizon gradient. */
object KapowEffectColors {
    val amber = Color(0xFFFFB547)
    val coral = Color(0xFFFF6B5A)
    val violet = KapowViolet
    val cobalt = Color(0xFF3D7BFF)
}

val DarkColors =
    KapowColors(
        canvas = Color(0xFF0A0A0A),
        panel = Color(0xFF161616),
        glass = Color(0x1AD4D4D4),
        stage = Color(0xFF111111),
        hairline = Color(0x29E5E5E5),
        title = Color(0xFFFFFFFF),
        ink = Color(0xFFEDEDED),
        ash = Color(0xFFC2C2C2),
        slate = Color(0xFF8A8A8A),
        action = Color(0xFFFFFFFF),
        onAction = Color(0xFF161616),
        success = Color(0xFF7EE2A8),
        warning = Color(0xFFFFCF5C),
        isDark = true,
    )

val LightColors =
    KapowColors(
        canvas = Color(0xFFFAFAFA),
        panel = Color(0xFFFFFFFF),
        glass = Color(0x0A161616),
        stage = Color(0xFFF2F2F2),
        hairline = Color(0x1F161616),
        title = Color(0xFF0A0A0A),
        ink = Color(0xFF161616),
        ash = Color(0xFF3D3D3D),
        slate = Color(0xFF6B6B6B),
        action = Color(0xFF161616),
        onAction = Color(0xFFFFFFFF),
        success = Color(0xFF14804A),
        warning = Color(0xFFA15C00),
        isDark = false,
    )
