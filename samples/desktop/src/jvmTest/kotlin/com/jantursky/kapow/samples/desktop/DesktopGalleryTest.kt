package com.jantursky.kapow.samples.desktop

import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import com.jantursky.kapow.samples.design.KapowTheme
import com.jantursky.kapow.samples.gallery.GalleryStage
import com.jantursky.kapow.samples.gallery.entries
import com.jantursky.kapow.samples.gallery.entryById
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals

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
    fun everyEntryPublishesItsUsageCode() {
        val missing = mutableListOf<String>()
        for (entry in entries) {
            var code = ""
            runDesktopComposeUiTest(width = 1200, height = 820) {
                mainClock.autoAdvance = false
                setContent {
                    KapowTheme(
                        darkTheme = true,
                    ) { GalleryStage(entry, autoplay = false, onCode = { code = it }) }
                }
                mainClock.advanceTimeBy(500)
            }
            // A usage snippet calls something and has no leftover template markers.
            if (!code.contains("(") || code.contains("\${")) missing += entry.id
        }
        assertEquals(emptyList(), missing, "entries without a usage snippet")
    }

    @Test
    fun theUsageFollowsTheControls() =
        runDesktopComposeUiTest(width = 1200, height = 820) {
            mainClock.autoAdvance = false
            setContent { DesktopGallery(initialEntry = entryById("change-shine")) }
            mainClock.advanceTimeBy(500)
            onNodeWithText("Copy").assertExists()
            onNode(hasText("angle = 45f", substring = true)).assertExists()
        }

    @Test
    fun overviewShowsEveryEntryAndOpensOne() =
        runDesktopComposeUiTest(width = 1200, height = 820) {
            mainClock.autoAdvance = false
            setContent { DesktopGallery() }
            mainClock.advanceTimeBy(1500)
            // The first tiles are in view; a tile carries its title and its group.
            val spray = entryById("change-spray")!!
            onNode(hasText(spray.title) and hasText(spray.group.title)).performClick()
            mainClock.advanceTimeBy(500)
            onNodeWithText(spray.summary).assertExists()
            onNodeWithText("Overview").performClick()
            mainClock.advanceTimeBy(500)
            onNodeWithText(spray.summary).assertDoesNotExist()
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

    /** With `KAPOW_SCREENSHOTS` set to a directory, saves dark and light screenshots of the overview and one example page there. */
    @Test
    fun screenshots() {
        val dir = System.getenv("KAPOW_SCREENSHOTS")?.let(::File) ?: return
        dir.mkdirs()
        val entry = entryById("change-spray")!!
        for (dark in listOf(true, false)) {
            runDesktopComposeUiTest(width = 1200, height = 820) {
                mainClock.autoAdvance = false
                setContent { DesktopGallery(initialDark = dark) }
                mainClock.advanceTimeBy(1500)
                val image = onRoot().captureToImage().toAwtImage()
                ImageIO.write(image, "png", dir.resolve("overview-${if (dark) "dark" else "light"}.png"))
            }
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
