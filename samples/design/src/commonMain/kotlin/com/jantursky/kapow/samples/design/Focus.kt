package com.jantursky.kapow.samples.design

import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/** The focus treatment of design/DESIGN.md: a 2 dp violet ring at 60 % over a soft violet glow at 25 %. */
@Composable
internal fun Modifier.kapowFocusRing(
    source: MutableInteractionSource,
    shape: Shape,
): Modifier {
    val focused by source.collectIsFocusedAsState()
    if (!focused) return this
    return drawBehind {
        drawOutline(
            shape.createOutline(size, layoutDirection, this),
            KapowViolet.copy(alpha = 0.25f),
            style = Stroke(8.dp.toPx()),
        )
    }.border(2.dp, KapowViolet.copy(alpha = 0.6f), shape)
}
