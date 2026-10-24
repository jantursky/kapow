package com.jantursky.kapow.samples.web

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.samples.design.KapowCaption
import com.jantursky.kapow.samples.design.KapowTheme

/** Average frame time and frames over budget in a trailing window, from frame timestamps. */
internal class FrameWindow(
    private val windowNanos: Long = 1_000_000_000L,
    private val budgetNanos: Long = 25_000_000L,
) {
    private val times = ArrayDeque<Long>()

    class Stats(
        val averageMillis: Float,
        val dropped: Int,
    )

    fun add(frameTimeNanos: Long): Stats {
        times.addLast(frameTimeNanos)
        while (times.size > 2 && frameTimeNanos - times.first() > windowNanos) times.removeFirst()
        val gaps = times.zipWithNext { a, b -> b - a }
        if (gaps.isEmpty()) return Stats(0f, 0)
        return Stats(gaps.average().toFloat() / 1_000_000f, gaps.count { it > budgetNanos })
    }
}

/** A dot and "16.7 ms · 0 dropped" over the last second. Green while no frame went over 25 ms, amber otherwise. */
@Composable
internal fun FrameMeter(modifier: Modifier = Modifier) {
    var stats by remember { mutableStateOf(FrameWindow.Stats(0f, 0)) }
    LaunchedEffect(Unit) {
        val window = FrameWindow()
        var shownAt = 0L
        while (true) {
            withFrameNanos { now ->
                val latest = window.add(now)
                // The text only needs to change a few times a second.
                if (now - shownAt > 250_000_000L) {
                    stats = latest
                    shownAt = now
                }
            }
        }
    }
    val colors = KapowTheme.colors
    val tone = if (stats.dropped == 0) colors.success else colors.warning
    Row(modifier.fillMaxSize().background(colors.panel), verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.size(8.dp)) { drawCircle(tone) }
        val millis = (stats.averageMillis * 10).toInt() / 10f
        KapowCaption("$millis ms · ${stats.dropped} dropped", Modifier.padding(start = 8.dp), color = tone)
    }
}
