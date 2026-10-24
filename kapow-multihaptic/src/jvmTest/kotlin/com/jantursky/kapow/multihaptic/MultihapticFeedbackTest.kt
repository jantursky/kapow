package com.jantursky.kapow.multihaptic

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import com.jantursky.kapow.KapowFeedback
import com.jantursky.kapow.KapowFeedbackEvent
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertSame

@OptIn(ExperimentalTestApi::class)
class MultihapticFeedbackTest {
    @Test
    fun theHandlerIsKeptAcrossRecompositionsAndAcceptsEveryEvent() =
        runDesktopComposeUiTest {
            val seen = mutableListOf<KapowFeedback>()
            var round by mutableIntStateOf(0)
            setContent {
                round
                seen += rememberMultihapticFeedback()
            }
            round++
            waitForIdle()
            assertSame(seen.first(), seen.last())
            val handler = assertNotNull(seen.first())
            // Desktop has no vibrator; the stub must take every event without failing.
            listOf(
                KapowFeedbackEvent.Selection,
                KapowFeedbackEvent.ImpactHeavy,
                KapowFeedbackEvent.Error,
                KapowFeedbackEvent.Custom("x"),
            ).forEach(handler::perform)
        }
}
