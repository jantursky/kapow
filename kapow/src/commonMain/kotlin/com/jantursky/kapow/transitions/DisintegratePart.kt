package com.jantursky.kapow.transitions

import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.ParticleShape
import com.jantursky.kapow.TransitionKind
import com.jantursky.kapow.internal.EffectContext
import com.jantursky.kapow.particles.Emission
import com.jantursky.kapow.particles.ParticleBatch
import com.jantursky.kapow.particles.ParticleSystem
import com.jantursky.kapow.particles.Spread
import com.jantursky.kapow.particles.shapePainter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * The moment, from 0 to 1 over the exit, when the grain at [column], [row] of a grid [columns] wide turns to dust.
 * A band sweeps from left to right over the first [DUST_SWEEP] of the exit; inside the band the grains go in a
 * seeded random order, so the band's edge is ragged.
 */
internal fun dustMoment(
    column: Int,
    row: Int,
    columns: Int,
): Float {
    val u = (column + 0.5f) / columns
    return (u * (1f - DUST_BAND) + DUST_BAND * cellHash(column, row, DUST_SEED)) * DUST_SWEEP
}

/** The moment the dust of emission column [index] of [count] rises: when the band's middle passes it. */
internal fun dustRise(
    index: Int,
    count: Int,
): Float {
    val u = (index + 0.5f) / count
    return (u * (1f - DUST_BAND) + DUST_BAND / 2) * DUST_SWEEP
}

/**
 * What has turned to dust at [t], 0 to 1 over the exit, of [size] in grains of about [cell] px: whole columns left
 * of the band as one block, and the grains inside it one by one, neighbours in a row merged.
 */
internal fun dustHidden(
    size: Size,
    cell: Float,
    t: Float,
): Path {
    val grid = cellGrid(size, cell)
    // Columns whose latest grain is already gone are gone whole.
    var gone = 0
    while (gone < grid.width && ((gone + 0.5f) / grid.width * (1f - DUST_BAND) + DUST_BAND) * DUST_SWEEP <= t) gone++
    return Path().apply {
        if (gone > 0) addRect(Rect(0f, 0f, cellEdge(gone, grid.width, size.width), size.height))
        for (row in 0 until grid.height) {
            val top = cellEdge(row, grid.height, size.height)
            val bottom = cellEdge(row + 1, grid.height, size.height)
            var start = -1
            for (column in gone..grid.width) {
                val hidden = column < grid.width && dustMoment(column, row, grid.width) <= t
                if (hidden && start < 0) start = column
                if (!hidden && start >= 0) {
                    addRect(
                        Rect(
                            cellEdge(start, grid.width, size.width),
                            top,
                            cellEdge(column, grid.width, size.width),
                            bottom,
                        ),
                    )
                    start = -1
                }
                // Past the band nothing has gone yet.
                if (!hidden && (column + 0.5f) / grid.width * (1f - DUST_BAND) * DUST_SWEEP > t) break
            }
        }
    }
}

/**
 * The colors to give the dust of each of [columns] columns, read from [image] at a few heights. Pixels that are
 * mostly transparent are skipped; a column with nothing opaque gets [fallback].
 */
internal fun dustColors(
    image: ImageBitmap,
    columns: Int,
    fallback: Color,
): List<List<Color>> =
    List(columns) { k ->
        val x = ((k + 0.5f) / columns * image.width).roundToInt().coerceIn(0, image.width - 1)
        val picked =
            (0 until DUST_SAMPLES).mapNotNull { r ->
                val y = ((r + 0.5f) / DUST_SAMPLES * image.height).roundToInt().coerceIn(0, image.height - 1)
                image.toPixelMap(x, y, 1, 1)[0, 0].takeIf { it.alpha >= 0.5f }?.copy(alpha = 1f)
            }
        picked.ifEmpty { listOf(fallback) }
    }

internal const val DUST_SWEEP = 0.55f
internal const val DUST_BAND = 0.3f
internal const val DUST_COLUMNS = 10
private const val DUST_SAMPLES = 4
private const val DUST_SEED = 71
private val DustFallback = Color(0xFFC2C2C2)

/**
 * Turns the content to dust: a ragged band sweeps across it, grain by grain, and as it passes each part, small
 * particles in that part's colors drift up and away.
 */
internal class DisintegratePart(
    private val kind: TransitionKind.Disintegrate,
) : TransitionPart() {
    private var watcher: Job? = null
    private var sampler: Job? = null
    private var colors: List<List<Color>>? = null

    override fun ContentDrawScope.draw(progress: Float) {
        drawContent()
        drawPath(dustHidden(size, 3.dp.toPx(), 1f - progress), Color.Black, blendMode = BlendMode.DstOut)
    }

    override fun attach(
        context: EffectContext,
        entering: () -> Boolean,
        progress: () -> Float,
    ) {
        watcher?.cancel()
        watcher =
            context.scope.launch {
                var exiting = false
                var risen = 0
                snapshotFlow { entering() to progress() }.collect { (entering, p) ->
                    if (entering) {
                        exiting = false
                        return@collect
                    }
                    if (!exiting) {
                        if (p >= 1f) return@collect
                        exiting = true
                        risen = 0
                        sample(context)
                    }
                    while (risen < DUST_COLUMNS && 1f - p >= dustRise(risen, DUST_COLUMNS)) {
                        context.emit(dust(context, risen++))
                    }
                }
            }
    }

    private fun sample(context: EffectContext) {
        colors = null
        sampler?.cancel()
        if (kind.color.isSpecified) return
        sampler =
            context.scope.launch {
                colors =
                    try {
                        context.snapshot()?.let { dustColors(it, DUST_COLUMNS, DustFallback) }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        // A target that can't read the layer back keeps the fallback color.
                        null
                    }
            }
    }

    private fun dust(
        context: EffectContext,
        column: Int,
    ): ParticleBatch =
        with(context.density) {
            val width = context.size.width.toFloat()
            val height = context.size.height.toFloat()
            val half = width / DUST_COLUMNS / 2
            val life = max(0.2f, kind.duration.inWholeMilliseconds / 1000f * (1f - DUST_SWEEP) * 0.9f)
            val palette =
                if (kind.color.isSpecified) listOf(kind.color) else colors?.get(column) ?: listOf(DustFallback)
            ParticleBatch(
                ParticleSystem(
                    Emission(
                        count = min(40, max(8, (height / 2.5.dp.toPx()).roundToInt())),
                        angle = Spread(-85f, -15f),
                        speed = Spread(30.dp.toPx(), 110.dp.toPx()),
                        lifetime = Spread(life * 0.5f, life),
                        gravity = -40.dp.toPx(),
                        drag = 0.4f,
                        spin = Spread(-180f, 180f),
                        scale = Spread(0.5f, 1.2f),
                        offsetX = Spread(-half, half),
                        offsetY = Spread(-height / 2, height / 2),
                        wobble = 3.dp.toPx(),
                        wobbleRate = 1.5f,
                    ),
                    seed = DUST_SEED + column,
                ),
                shapePainter(ParticleShape.Square, palette, 3.dp.toPx()),
                opacity = { 1f - it * it },
                growth = { 1f - 0.4f * it },
            ).apply { origin = Offset((column + 0.5f) / DUST_COLUMNS * width, height / 2) }
        }

    override fun stop() {
        watcher?.cancel()
        sampler?.cancel()
    }
}
