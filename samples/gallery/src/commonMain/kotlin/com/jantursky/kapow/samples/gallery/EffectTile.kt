package com.jantursky.kapow.samples.gallery

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.KapowParticleLayer
import com.jantursky.kapow.samples.design.KapowCaption
import com.jantursky.kapow.samples.design.KapowShapes
import com.jantursky.kapow.samples.design.KapowText
import com.jantursky.kapow.samples.design.KapowTheme
import kotlin.math.roundToInt

/**
 * Set by [EffectTile]: [ExampleScaffold] then shows its stage only, without the card and the controls, scaled
 * down to fit the tile, and plays it on its own while the host's autoplay is on.
 */
internal val LocalExamplePreview = staticCompositionLocalOf { false }

/**
 * The room an example gets in a tile before it is scaled down to the tile's stage. Every example fits a full
 * page stage, and most are drawn for about 300 × 160 dp, so this keeps them large in the tile.
 */
private val PreviewSize = DpSize(340.dp, 255.dp)

/**
 * The parts of a tile around its stage, in dp. The web page lays its tile links out with the same numbers in
 * site.css (`.tile`), so the links sit exactly over the tiles the canvas draws.
 */
object EffectTileMetrics {
    /** Padding around the stage and under the caption. */
    const val PADDING = 12

    /** Between the stage and the title. */
    const val GAP = 10

    /** One caption line: the title, then the group. */
    const val LINE = 20

    /** The stage's width over its height. */
    const val STAGE_ASPECT = 4f / 3f
}

/**
 * A compact, live preview of [entry] for an overview grid: its stage with the example playing over and over
 * (while [autoplay] is on), its title and its group, on a `glass` card. The tile takes no input of its own, so a
 * host can make it a link or a button; scrolling over it scrolls the grid it sits in.
 */
@Composable
fun EffectTile(
    entry: Entry,
    autoplay: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = KapowTheme.colors
    val padding = EffectTileMetrics.PADDING.dp
    Column(
        modifier
            .clip(KapowShapes.card)
            .background(colors.glass)
            .padding(padding),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(EffectTileMetrics.STAGE_ASPECT)
                .clip(KapowShapes.stage)
                .background(colors.stage)
                .border(1.dp, colors.hairline, KapowShapes.stage),
        ) {
            CompositionLocalProvider(
                LocalGalleryAutoplay provides autoplay,
                LocalExamplePreview provides true,
            ) {
                entry.example.content()
            }
            // Over the example, so its own scrollers and buttons never see the pointer.
            Box(Modifier.matchParentSize().pointerInput(Unit) {})
        }
        Box(Modifier.height(EffectTileMetrics.GAP.dp))
        KapowText(
            entry.title,
            Modifier.height(EffectTileMetrics.LINE.dp),
            style = KapowTheme.type.label,
            color = colors.title,
            maxLines = 1,
        )
        KapowCaption(entry.group.title, Modifier.height(EffectTileMetrics.LINE.dp), color = colors.slate)
    }
}

/** What [ExampleScaffold] shows inside an [EffectTile]: the stage alone, scaled to fit, with its own particle layer. */
@Composable
internal fun PreviewStage(stage: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize().scaleToFit(PreviewSize)) {
        // The tile clips, and its particles stay with it while the grid scrolls.
        KapowParticleLayer(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { stage() }
        }
    }
}

/**
 * Lays the content out at [size] and scales it down, never up, to fit the space it gets, centered. Drawing,
 * particles included, scales with it.
 */
private fun Modifier.scaleToFit(size: DpSize): Modifier =
    layout { measurable, constraints ->
        val width = size.width.roundToPx()
        val height = size.height.roundToPx()
        val placeable = measurable.measure(Constraints.fixed(width, height))
        val outerWidth = if (constraints.hasBoundedWidth) constraints.maxWidth else width
        val outerHeight = if (constraints.hasBoundedHeight) constraints.maxHeight else height
        val scale = minOf(1f, outerWidth / width.toFloat(), outerHeight / height.toFloat())
        layout(outerWidth, outerHeight) {
            placeable.placeWithLayer(
                ((outerWidth - width * scale) / 2).roundToInt(),
                ((outerHeight - height * scale) / 2).roundToInt(),
            ) {
                scaleX = scale
                scaleY = scale
                transformOrigin = TransformOrigin(0f, 0f)
            }
        }
    }
