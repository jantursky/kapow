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

/** Fills the sidebar and the overview with the entries of the catalog. */
internal fun renderEntryLists() {
    val groups = Group.entries.flatMap { groupList(it) }
    document.getElementById("sidebar")?.append(*groups.toTypedArray())
    Group.entries.forEach { group ->
        val section = element("section", "class" to "group", children = groupList(group))
        document.getElementById("overview-groups")?.appendChild(section)
    }
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

/** Shows the Play/Pause pill in the state autoplay is in: its label names what a press does. */
internal fun showAutoplay(playing: Boolean) {
    val button = document.getElementById("autoplay") ?: return
    button.setAttribute("aria-label", if (playing) "Pause autoplay" else "Play autoplay")
    button.setAttribute("data-playing", playing.toString())
    button.querySelector(".autoplay-label")?.textContent = if (playing) "Pause" else "Play"
}
