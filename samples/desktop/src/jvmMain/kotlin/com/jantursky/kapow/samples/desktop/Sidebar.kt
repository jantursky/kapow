package com.jantursky.kapow.samples.desktop

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.samples.design.KapowCaption
import com.jantursky.kapow.samples.design.KapowShapes
import com.jantursky.kapow.samples.design.KapowText
import com.jantursky.kapow.samples.design.KapowTheme
import com.jantursky.kapow.samples.gallery.Entry
import com.jantursky.kapow.samples.gallery.Group
import com.jantursky.kapow.samples.gallery.entries

/** Every catalog entry under its group, as in the web sidebar. The [selected] one sits on a `glass` pill. */
@Composable
internal fun Sidebar(
    selected: Entry,
    onSelect: (Entry) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = KapowTheme.colors
    val scroll = rememberScrollState()
    Row(modifier.width(240.dp).background(colors.panel)) {
        Box(Modifier.weight(1f).fillMaxHeight()) {
            Column(Modifier.verticalScroll(scroll).padding(horizontal = 12.dp, vertical = 16.dp)) {
                Group.entries.forEachIndexed { index, group ->
                    KapowCaption(
                        group.title,
                        Modifier.padding(start = 14.dp, top = if (index == 0) 0.dp else 20.dp, bottom = 6.dp),
                        color = colors.slate,
                    )
                    entries.filter { it.group == group }.forEach { entry ->
                        SidebarLink(entry, entry == selected, onClick = { onSelect(entry) })
                    }
                }
            }
            VerticalScrollbar(
                rememberScrollbarAdapter(scroll),
                Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
            )
        }
        Box(Modifier.width(1.dp).fillMaxHeight().background(colors.hairline))
    }
}

@Composable
private fun SidebarLink(
    entry: Entry,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val colors = KapowTheme.colors
    val source = remember { MutableInteractionSource() }
    val hovered by source.collectIsHoveredAsState()
    KapowText(
        entry.title,
        Modifier
            .fillMaxWidth()
            .clip(KapowShapes.pill)
            .then(if (isSelected) Modifier.background(colors.glass) else Modifier)
            .hoverable(source)
            .clickable(interactionSource = source, indication = null, role = Role.Tab, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        style = KapowTheme.type.body,
        color = if (isSelected || hovered) colors.title else colors.ash,
        maxLines = 1,
    )
}
