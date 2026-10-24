package com.jantursky.kapow.samples.web

import com.jantursky.kapow.samples.gallery.Entry
import com.jantursky.kapow.samples.gallery.Group
import com.jantursky.kapow.samples.gallery.entries
import kotlinx.browser.document
import org.w3c.dom.Element
import org.w3c.dom.asList

private fun element(
    tag: String,
    vararg attributes: Pair<String, String>,
    text: String? = null,
    children: List<Element> = emptyList(),
): Element =
    document.createElement(tag).apply {
        attributes.forEach { (name, value) -> setAttribute(name, value) }
        text?.let { textContent = it }
        children.forEach { appendChild(it) }
    }

private fun link(entry: Entry): Element =
    element("a", "href" to "#${entry.id}", "data-entry" to entry.id, text = entry.title)

private fun groupList(group: Group): List<Element> =
    listOf(
        element("h2", text = group.title),
        element(
            "ul",
            children = entries.filter { it.group == group }.map { element("li", children = listOf(link(it))) },
        ),
    )

/** A link over one tile of the overview grid. The canvas draws the tile; the link carries its name and focus. */
private fun tile(entry: Entry): Element =
    element(
        "li",
        children =
            listOf(
                element(
                    "a",
                    "href" to "#${entry.id}",
                    "class" to "tile",
                    "data-tile" to entry.id,
                    children =
                        listOf(
                            element("span", "class" to "sr-only", text = "${entry.title}, ${entry.group.title}"),
                        ),
                ),
            ),
    )

/** A group of the overview grid: its heading (drawn by the canvas too) and a link per entry. */
private fun gridGroup(group: Group): Element =
    element(
        "section",
        "class" to "grid-group",
        "aria-labelledby" to "grid-${group.name}",
        children =
            listOf(
                element(
                    "h3",
                    "class" to "grid-heading",
                    "id" to "grid-${group.name}",
                    "data-heading" to group.name,
                    children = listOf(element("span", "class" to "sr-only", text = group.title)),
                ),
                element("ul", "class" to "tiles", children = entries.filter { it.group == group }.map { tile(it) }),
            ),
    )

/** Fills the sidebar and the overview grid with the entries of the catalog. */
internal fun renderEntryLists() {
    val groups = Group.entries.flatMap { groupList(it) }
    document.getElementById("sidebar")?.append(*groups.toTypedArray())
    val grid = document.getElementById("effect-grid")
    Group.entries.filter { group -> entries.any { it.group == group } }.forEach { grid?.appendChild(gridGroup(it)) }
}

private fun setText(
    id: String,
    text: String,
) {
    document.getElementById(id)?.textContent = text
}

private fun setHidden(
    id: String,
    hidden: Boolean,
) {
    document.getElementById(id)?.let { if (hidden) it.setAttribute("hidden", "") else it.removeAttribute("hidden") }
}

/** Shows the overview, or the page of [entry] with its title, summary, source and the current mark in the sidebar. */
internal fun showPage(entry: Entry?) {
    setHidden("overview", entry != null)
    setHidden("example", entry == null)
    document.title = if (entry == null) "Kapow" else "${entry.title} · ${entry.group.title} · Kapow"
    if (entry != null) {
        setText("example-group", entry.group.title)
        setText("example-title", entry.title)
        setText("example-summary", entry.summary)
        showSource(entry.example.sourceFile)
    }
    document.querySelectorAll("#sidebar a").asList().filterIsInstance<Element>().forEach {
        if (it.getAttribute("data-entry") ==
            entry?.id
        ) {
            it.setAttribute("aria-current", "page")
        } else {
            it.removeAttribute("aria-current")
        }
    }
}

/** site.js renders the generated source of an example file; an empty name hides the panel. */
private fun showSource(file: String): Unit = js("window.kapowShowSource(file)")

/**
 * Shows the Play/Pause pills (the example's and the overview grid's) in the state autoplay is in: the label
 * names what a press does.
 */
internal fun showAutoplay(playing: Boolean) {
    document.querySelectorAll(".autoplay").asList().filterIsInstance<Element>().forEach { button ->
        button.setAttribute("aria-label", if (playing) "Pause autoplay" else "Play autoplay")
        button.setAttribute("data-playing", playing.toString())
        button.querySelector(".autoplay-label")?.textContent = if (playing) "Pause" else "Play"
    }
}
