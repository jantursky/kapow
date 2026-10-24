package com.jantursky.kapow.samples.desktop

import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import com.jantursky.kapow.samples.gallery.entries
import com.jantursky.kapow.samples.gallery.entryById
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class DesktopGalleryTest {
    @Test
    fun everyEntryRendersInBothThemes() {
        for (dark in listOf(true, false)) {
            for (entry in entries) {
                runDesktopComposeUiTest(width = 1200, height = 820) {
                    // Autoplay keeps the frame clock busy, so time moves by hand.
                    mainClock.autoAdvance = false
                    setContent { DesktopGallery(initialEntry = entry, initialDark = dark) }
                    mainClock.advanceTimeBy(1500)
                    onNodeWithText(entry.summary).assertExists()
                }
            }
        }
    }

    @Test
    fun topBarControlsWork() =
        runDesktopComposeUiTest(width = 1200, height = 820) {
            mainClock.autoAdvance = false
            setContent { DesktopGallery() }
            mainClock.advanceTimeBy(500)
            onNodeWithText("Pause").performClick()
            mainClock.advanceTimeBy(100)
            onNodeWithText("Play").assertExists()
            onNodeWithText("Light").performClick()
            onNodeWithText("On").performClick()
            mainClock.advanceTimeBy(100)
            onNodeWithText("Play").performClick()
            mainClock.advanceTimeBy(100)
            onNodeWithText("Pause").assertExists()
        }

    /** With `KAPOW_SCREENSHOTS` set to a directory, saves a dark and a light screenshot of one example page there. */
    @Test
    fun screenshots() {
        val dir = System.getenv("KAPOW_SCREENSHOTS")?.let(::File) ?: return
        dir.mkdirs()
        val entry = entryById("change-spray")!!
        for (dark in listOf(true, false)) {
            runDesktopComposeUiTest(width = 1200, height = 820) {
                mainClock.autoAdvance = false
                setContent { DesktopGallery(initialEntry = entry, initialDark = dark) }
                // Autoplay starts 600 ms in; the burst is at its widest a little after.
                mainClock.advanceTimeBy(1000)
                val image = onRoot().captureToImage().toAwtImage()
                ImageIO.write(image, "png", dir.resolve("${entry.id}-${if (dark) "dark" else "light"}.png"))
            }
        }
    }
}
