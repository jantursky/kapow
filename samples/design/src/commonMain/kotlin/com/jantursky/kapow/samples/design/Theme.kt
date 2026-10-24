package com.jantursky.kapow.samples.design

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp

/** Corner radii from design/DESIGN.md. */
object KapowShapes {
    val control = RoundedCornerShape(10.dp)
    val stage = RoundedCornerShape(16.dp)
    val card = RoundedCornerShape(24.dp)
    val pill = CircleShape
}

private val LocalColors = staticCompositionLocalOf { DarkColors }
private val LocalTypography = staticCompositionLocalOf<KapowTypography> { error("KapowTheme missing") }

object KapowTheme {
    val colors: KapowColors
        @Composable @ReadOnlyComposable
        get() = LocalColors.current
    val type: KapowTypography
        @Composable @ReadOnlyComposable
        get() = LocalTypography.current
}

@Composable
fun KapowTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalColors provides if (darkTheme) DarkColors else LightColors,
        LocalTypography provides rememberKapowTypography(),
        content = content,
    )
}
