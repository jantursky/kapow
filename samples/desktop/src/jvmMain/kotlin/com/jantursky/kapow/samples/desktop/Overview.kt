package com.jantursky.kapow.samples.desktop

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.samples.design.KapowShapes
import com.jantursky.kapow.samples.design.KapowText
import com.jantursky.kapow.samples.design.KapowTheme
import com.jantursky.kapow.samples.gallery.EffectTile
import com.jantursky.kapow.samples.gallery.Entry
import com.jantursky.kapow.samples.gallery.Group
import com.jantursky.kapow.samples.gallery.entries

/**
 * The start screen: every catalog entry as a live tile under its group. The grid composes only the tiles in
 * view, so only those play. A click opens the entry's page.
 */
@Composable
internal fun Overview(
    autoplay: Boolean,
    onOpen: (Entry) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = KapowTheme.colors
    val state = rememberLazyGridState()
    Box(modifier) {
        LazyVerticalGrid(
            GridCells.Adaptive(240.dp),
            state = state,
            contentPadding = PaddingValues(horizontal = 32.dp, vertical = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Group.entries.forEach { group ->
                val members = entries.filter { it.group == group }
                if (members.isEmpty()) return@forEach
                item(key = group.name, span = { GridItemSpan(maxLineSpan) }) {
                    KapowText(
                        group.title,
                        Modifier.padding(top = if (group == Group.entries.first()) 0.dp else 16.dp),
                        style = KapowTheme.type.subheading,
                        color = colors.title,
                    )
                }
                items(members, key = { it.id }) { entry ->
                    EffectTile(
                        entry,
                        autoplay,
                        Modifier
                            .clip(KapowShapes.card)
                            .clickable(role = Role.Button, onClickLabel = "Open ${entry.title}") { onOpen(entry) },
                    )
                }
            }
        }
        VerticalScrollbar(rememberScrollbarAdapter(state), Modifier.align(Alignment.CenterEnd).fillMaxHeight())
    }
}
