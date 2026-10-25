package com.jantursky.kapow.samples.gallery

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.samples.design.LabeledSlider
import com.jantursky.kapow.samples.design.PrimaryPill
import com.jantursky.kapow.samples.gallery.examples.ConfettiPopExample
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.seconds

/** The controls around [ConfettiPopExample]. */
@Composable
internal fun ConfettiPopDemo() {
    var payments by remember { mutableIntStateOf(0) }
    var pieces by remember { mutableFloatStateOf(28f) }
    var ribbons by remember { mutableFloatStateOf(0.33f) }
    var size by remember { mutableFloatStateOf(7f) }
    ExampleScaffold(
        autoplay = Autoplay(2.2.seconds) { payments++ },
        stage = { ConfettiPopExample(payments, pieces.roundToInt(), ribbons, size.dp) },
        code = {
            changeEffectCode(
                call(
                    "ChangeEffect.confettiPop",
                    "count" to pieces.roundToInt().toString(),
                    "ribbonRatio" to "${ribbons.oneDecimal()}f",
                    "pieceSize" to "${size.roundToInt()}.dp",
                ),
                value = "payments",
            )
        },
        controls = {
            PrimaryPill("Pay", onClick = { payments++ })
            LabeledSlider("Pieces", pieces.roundToInt().toString(), pieces, { pieces = it }, 4f..80f)
            LabeledSlider("Ribbons", "${(ribbons * 100).roundToInt()} %", ribbons, { ribbons = it }, 0f..1f)
            LabeledSlider("Piece size", "${size.roundToInt()} dp", size, { size = it }, 4f..14f)
        },
    )
}
