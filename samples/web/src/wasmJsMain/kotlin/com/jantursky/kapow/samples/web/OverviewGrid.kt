package com.jantursky.kapow.samples.web

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.structuralEqualityPolicy
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.LocalKapowMotion
import com.jantursky.kapow.samples.design.KapowText
import com.jantursky.kapow.samples.design.KapowTheme
import com.jantursky.kapow.samples.gallery.EffectTile
import com.jantursky.kapow.samples.gallery.Entry
import com.jantursky.kapow.samples.gallery.Group
import com.jantursky.kapow.samples.gallery.entryById
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.Element
import org.w3c.dom.asList

/**
 * A heading or a tile of the overview grid where the page laid out its element, in CSS px (which are dp in the
 * canvas) from the grid's top left corner.
 */
internal data class GridItem(
    val key: String,
    val group: Group?,
    val entry: Entry?,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
)

/** The part of the grid the visitor sees, in grid px. Empty while the grid is out of the window or hidden. */
internal data class GridView(
    val top: Float,
    val bottom: Float,
)

/** Every heading and tile of the grid, measured again whenever the grid changes size. */
private val gridItems = mutableStateOf<List<GridItem>>(emptyList())

/** How far below the grid's top the canvas sits: 0 until the grid reaches the window's top, then it follows the scroll. */
private val gridCanvasTop = mutableFloatStateOf(0f)

private val gridView = mutableStateOf(GridView(0f, 0f))

/** Calls [callback] whenever [element] changes size, and once right away. */
private fun onResize(
    element: Element,
    callback: () -> Unit,
): Unit = js("new ResizeObserver(() => callback()).observe(element)")

private fun measureGrid() {
    val grid = document.getElementById("effect-grid") ?: return
    val origin = grid.getBoundingClientRect()
    gridItems.value =
        grid
            .querySelectorAll("[data-tile], [data-heading]")
            .asList()
            .filterIsInstance<Element>()
            .mapNotNull { element ->
                val entry = element.getAttribute("data-tile")?.let(::entryById)
                val group =
                    element.getAttribute("data-heading")?.let { name ->
                        Group.entries.firstOrNull {
                            it.name ==
                                name
                        }
                    }
                val rect = element.getBoundingClientRect()
                if (entry == null && group == null) return@mapNotNull null
                GridItem(
                    key = entry?.id ?: "group-${group?.name}",
                    group = group,
                    entry = entry,
                    x = (rect.left - origin.left).toFloat(),
                    y = (rect.top - origin.top).toFloat(),
                    width = rect.width.toFloat(),
                    height = rect.height.toFloat(),
                )
            }
    followScroll()
}

/** Where the sticky canvas is over the grid, and which part of the grid is in the window. Cheap: two rects. */
private fun followScroll() {
    val grid = document.getElementById("effect-grid") ?: return
    val canvas = document.getElementById("grid-canvas") ?: return
    val g = grid.getBoundingClientRect()
    val c = canvas.getBoundingClientRect()
    gridCanvasTop.value = (c.top - g.top).toFloat()
    val top = (maxOf(c.top, 0.0) - g.top).toFloat()
    val bottom = (minOf(c.bottom, window.innerHeight.toDouble()) - g.top).toFloat()
    gridView.value = if (c.height > 0 && bottom > top) GridView(top, bottom) else GridView(0f, 0f)
}

/**
 * Keeps the grid's measurements up to date: when it changes size (the window, the overview showing again),
 * when anything above it moves it (fonts, the folding sidebar on a phone) and on every scroll.
 */
internal fun watchGrid() {
    document.getElementById("effect-grid")?.let { onResize(it) { measureGrid() } }
    document.body?.let { onResize(it) { followScroll() } }
    window.addEventListener("scroll", { followScroll() })
    window.addEventListener("resize", { followScroll() })
}

/**
 * The overview grid's canvas: the headings and the live tiles that are in view, each where the page laid out
 * its link. Tiles out of view are not composed at all, so they cost nothing and their autoplay rests; a tile
 * that scrolls back in starts over. Nothing shows while an example page is open.
 */
@Composable
internal fun OverviewGrid() {
    KapowTheme(darkTheme = pageIsDark.value) {
        // No haptics here: a screenful of examples would buzz all the time.
        CompositionLocalProvider(LocalKapowMotion provides pageMotion.value) {
            Box(Modifier.fillMaxSize().background(KapowTheme.colors.canvas)) {
                if (pageEntry.value == null) GridItems()
            }
        }
    }
}

@Composable
private fun GridItems() {
    val inView by remember {
        derivedStateOf(structuralEqualityPolicy()) {
            val view = gridView.value
            gridItems.value.filter { it.height > 0 && it.y < view.bottom && it.y + it.height > view.top }
        }
    }
    val autoplay = pageAutoplay.value
    val colors = KapowTheme.colors
    inView.forEach { item ->
        key(item.key) {
            // The scroll moves the items at placement, without composing them again.
            val place =
                Modifier
                    .offset { IntOffset(item.x.dp.roundToPx(), (item.y - gridCanvasTop.value).dp.roundToPx()) }
                    .requiredSize(item.width.dp, item.height.dp)
            when {
                item.entry != null -> {
                    EffectTile(item.entry, autoplay, place)
                }

                item.group != null -> {
                    KapowText(
                        item.group.title,
                        place,
                        KapowTheme.type.subheading,
                        colors.title,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}
