package com.jantursky.kapow.platform

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

// Runs in the headless Chrome that wasmJsTest starts. The media query is replaced by a stub so the test controls
// what the browser reports.
private fun stubReducedMotion(reduced: Boolean): Unit =
    js(
        """{
        if (!window.__kapowMatchMedia) window.__kapowMatchMedia = window.matchMedia;
        window.matchMedia = function (query) { return { matches: reduced, media: query }; };
    }""",
    )

private fun restoreMatchMedia(): Unit =
    js("{ if (window.__kapowMatchMedia) { window.matchMedia = window.__kapowMatchMedia; } }")

class PlatformReadersTest {
    @AfterTest
    fun restore() = restoreMatchMedia()

    @Test
    fun theBrowserSettingIsRead() {
        stubReducedMotion(true)
        assertTrue(platformReducedMotion())
        stubReducedMotion(false)
        assertFalse(platformReducedMotion())
    }

    @Test
    fun blurIsSupported() {
        assertTrue(blurSupported())
    }
}
