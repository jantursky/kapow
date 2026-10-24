package com.jantursky.kapow.samples.web

import androidx.compose.runtime.mutableStateOf
import com.jantursky.kapow.KapowMotion
import com.jantursky.kapow.samples.gallery.Entry
import com.jantursky.kapow.samples.gallery.entryById
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLSelectElement
import org.w3c.dom.MutationObserver
import org.w3c.dom.MutationObserverInit
import org.w3c.dom.asList

/**
 * Whether the page is dark: `data-theme` on the root element, which site.js sets from the saved choice or the
 * system. Compose reads it here; the switch lives in the page.
 */
internal val pageIsDark = mutableStateOf(readPageIsDark())

/** The reduced-motion select in the nav, as `LocalKapowMotion` wants it. */
internal val pageMotion = mutableStateOf(readPageMotion())

/** The "Haptics" checkbox in the nav: whether effects ask the browser to vibrate. */
internal val pageHaptics = mutableStateOf(readPageHaptics())

/**
 * Whether the examples play on their own. On by default; the Play/Pause pills over an example and over the
 * overview grid flip it, and `?autoplay=0` in the URL starts the page paused (the README GIF recorder uses it to
 * press the controls itself).
 */
internal val pageAutoplay = mutableStateOf(!Regex("[?&]autoplay=0(&|$)").containsMatchIn(window.location.search))

/** The entry in the URL hash (`#change-shine`), or null for the overview. */
internal val pageEntry = mutableStateOf(readPageEntry())

private fun readPageIsDark(): Boolean = document.documentElement?.getAttribute("data-theme") == "dark"

private fun readPageMotion(): KapowMotion =
    when ((document.getElementById("motion") as? HTMLSelectElement)?.value) {
        "full" -> KapowMotion.Full
        "reduced" -> KapowMotion.Reduced
        else -> KapowMotion.System
    }

private fun readPageHaptics(): Boolean = (document.getElementById("haptics") as? HTMLInputElement)?.checked == true

private fun readPageEntry(): Entry? = entryById(window.location.hash.removePrefix("#"))

/** Keeps the states above in step with the page. */
internal fun watchPage() {
    document.documentElement?.let { root ->
        MutationObserver { _, _ -> pageIsDark.value = readPageIsDark() }
            .observe(
                root,
                MutationObserverInit(
                    attributes = true,
                    attributeFilter = arrayOf("data-theme".toJsString()).toJsArray(),
                ),
            )
    }
    document.getElementById("motion")?.addEventListener("change", { pageMotion.value = readPageMotion() })
    document.getElementById("haptics")?.addEventListener("change", { pageHaptics.value = readPageHaptics() })
    document.querySelectorAll(".autoplay").asList().forEach { button ->
        button.addEventListener(
            "click",
            {
                pageAutoplay.value = !pageAutoplay.value
                showAutoplay(pageAutoplay.value)
            },
        )
    }
    window.addEventListener(
        "hashchange",
        {
            pageEntry.value = readPageEntry()
            showPage(pageEntry.value)
        },
    )
}
