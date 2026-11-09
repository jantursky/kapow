package com.jantursky.kapow.samples.gallery.examples

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.ChangeEffect
import com.jantursky.kapow.KapowParticleLayer
import com.jantursky.kapow.ParticleContent
import com.jantursky.kapow.ParticleShape
import com.jantursky.kapow.changeEffect
import com.jantursky.kapow.samples.design.KapowEffectColors
import com.jantursky.kapow.samples.design.KapowShapes
import com.jantursky.kapow.samples.design.KapowText
import com.jantursky.kapow.samples.design.KapowTheme

/** A scrolling list where each row has a like button. The hearts scroll along with their row. */
@Composable
fun ScrollingHeartsExample(
    state: LazyListState,
    likes: Int,
    likedRow: Int,
) {
    val spray = ChangeEffect.spray(ParticleContent.shape(ParticleShape.Heart), count = 11)
    KapowParticleLayer {
        LazyColumn(
            state = state,
            modifier =
                Modifier
                    .width(300.dp)
                    .height(240.dp)
                    .clip(KapowShapes.stage)
                    .background(KapowTheme.colors.panel),
        ) {
            items(ROWS) { row ->
                Row(
                    Modifier.fillMaxWidth().height(60.dp).padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    KapowText("Run ${row + 1}", color = KapowTheme.colors.title)
                    Box(
                        Modifier
                            .changeEffect(if (row == likedRow) likes else 0, spray)
                            .clip(CircleShape)
                            .background(KapowEffectColors.coral)
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    ) {
                        KapowText("Like", color = Color.Black)
                    }
                }
            }
        }
    }
}

private const val ROWS = 30
