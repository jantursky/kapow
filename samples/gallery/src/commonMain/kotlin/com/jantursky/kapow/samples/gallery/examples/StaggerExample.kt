package com.jantursky.kapow.samples.gallery.examples

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.ChangeEffect
import com.jantursky.kapow.KapowAnimatedVisibility
import com.jantursky.kapow.KapowEnter
import com.jantursky.kapow.KapowExit
import com.jantursky.kapow.KapowStagger
import com.jantursky.kapow.changeEffect
import com.jantursky.kapow.samples.design.KapowEffectColors
import com.jantursky.kapow.samples.design.KapowShapes
import com.jantursky.kapow.samples.design.KapowText
import kotlin.time.Duration

private val Rows = listOf("Inbox", "Drafts", "Sent", "Archive", "Trash")

/**
 * A list whose rows wipe in and out one after another, and whose shine runs down the rows when [shine] changes. Each
 * row waits [KapowStagger.delay] of its index, both for its transition and for its shine.
 */
@Composable
fun StaggerExample(
    visible: Boolean,
    shine: Int,
    step: Duration,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Rows.forEachIndexed { index, title ->
            val delay = KapowStagger.delay(index, step)
            KapowAnimatedVisibility(
                visible,
                enter = KapowEnter.wipe().delay(delay),
                exit = KapowExit.wipe().delay(delay),
            ) {
                Box(
                    Modifier
                        .size(width = 260.dp, height = 40.dp)
                        .changeEffect(shine, ChangeEffect.shine().delay(delay))
                        .clip(KapowShapes.card)
                        .background(KapowEffectColors.violet)
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    KapowText(title, color = Color.White)
                }
            }
        }
    }
}
