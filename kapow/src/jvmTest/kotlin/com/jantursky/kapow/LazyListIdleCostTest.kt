package com.jantursky.kapow

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.internal.layerRecords
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class LazyListIdleCostTest {
    private var tick by mutableIntStateOf(0)
    private var value by mutableIntStateOf(0)

    /** 100 rows of 4 px each, so all of them are on screen, every one with a change effect at rest. */
    private fun ComposeUiTest.rows(effect: () -> ChangeEffect) {
        setEffectContent {
            LazyColumn(Modifier.height(400.dp)) {
                items((0 until ROWS).toList()) { row ->
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .background(Cobalt)
                            .drawWithContent {
                                tick
                                drawContent()
                            }.changeEffect(value, effect()),
                    )
                }
            }
        }
    }

    @Test
    fun aHundredRowsWithEffectsAtRestRecordNoLayers() =
        runEffectTest {
            rows { ChangeEffect.shake() }
            mainClock.advanceTimeByFrame()
            val before = layerRecords
            repeat(FRAMES) {
                tick++
                mainClock.advanceTimeByFrame()
            }
            val records = layerRecords - before
            println("LAYER-RECORDS rows=$ROWS frames=$FRAMES records=$records")
            assertEquals(0, records, "layers recorded for $ROWS idle rows over $FRAMES frames")
        }

    @Test
    fun theSameRowsRecordALayerAfterTheyPlay() =
        runEffectTest {
            rows { ChangeEffect.shake() }
            mainClock.advanceTimeByFrame()
            value++
            repeat(3) { mainClock.advanceTimeByFrame() }
            val before = layerRecords
            repeat(5) {
                tick++
                mainClock.advanceTimeByFrame()
            }
            assertTrue(
                layerRecords - before >= ROWS / 2,
                "rows that play are drawn through layers: ${layerRecords - before}",
            )
        }

    private companion object {
        const val ROWS = 100
        const val FRAMES = 60
    }
}
