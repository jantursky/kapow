package com.jantursky.kapow.samples.gallery

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.jantursky.kapow.KapowFeedback
import com.jantursky.kapow.KapowMotion
import com.jantursky.kapow.LocalKapowFeedback
import com.jantursky.kapow.LocalKapowMotion
import com.jantursky.kapow.samples.design.GhostPill
import com.jantursky.kapow.samples.design.KapowCaption
import com.jantursky.kapow.samples.design.KapowShapes
import com.jantursky.kapow.samples.design.KapowText
import com.jantursky.kapow.samples.design.KapowTheme
import com.jantursky.kapow.samples.design.SegmentedControl

/** The reduced-motion choice as the web gallery words it: System, Off (full motion) or On (reduced). */
private enum class MobileMotion(
    val label: String,
    val motion: KapowMotion,
) {
    System("System", KapowMotion.System),
    Off("Off", KapowMotion.Full),
    On("On", KapowMotion.Reduced),
}

/**
 * The gallery for phones and tablets, all in Compose: a top bar with autoplay and a settings sheet (theme,
 * reduced motion, haptics), the overview grid of live tiles, and an entry's page with its live example and the
 * collapsible usage code. Back (the system gesture, or the Back pill) returns to the overview.
 *
 * [initialEntry] opens that page first; [initialDark] picks the theme, null follows the system. [haptics] is the
 * platform's [KapowFeedback], used while the Haptics switch is on; [onDarkChange] reports the theme so the host
 * can match its system bars.
 */
@Composable
fun MobileGallery(
    initialEntry: Entry? = null,
    initialDark: Boolean? = null,
    haptics: KapowFeedback? = null,
    onDarkChange: (Boolean) -> Unit = {},
) {
    val systemDark = isSystemInDarkTheme()
    var entryId by rememberSaveable { mutableStateOf(initialEntry?.id) }
    var dark by rememberSaveable { mutableStateOf(initialDark ?: systemDark) }
    var motion by rememberSaveable { mutableStateOf(KapowMotion.System) }
    var autoplay by rememberSaveable { mutableStateOf(true) }
    var hapticsOn by rememberSaveable { mutableStateOf(false) }
    var settingsOpen by rememberSaveable { mutableStateOf(false) }
    val entry = entryId?.let(::entryById)
    LaunchedEffect(dark) { onDarkChange(dark) }
    NavigationBackHandler(
        state = rememberNavigationEventState(NavigationEventInfo.None),
        isBackEnabled = entry != null || settingsOpen,
        onBackCompleted = { if (settingsOpen) settingsOpen = false else entryId = null },
    )
    KapowTheme(darkTheme = dark) {
        CompositionLocalProvider(
            LocalKapowMotion provides motion,
            LocalKapowFeedback provides if (hapticsOn) haptics else null,
        ) {
            Column(Modifier.fillMaxSize().background(KapowTheme.colors.canvas)) {
                MobileTopBar(
                    entry = entry,
                    onBack = { entryId = null },
                    autoplay = autoplay,
                    onAutoplayChange = { autoplay = it },
                    settingsOpen = settingsOpen,
                    onSettingsOpenChange = { settingsOpen = it },
                )
                if (settingsOpen) {
                    MobileSettings(
                        dark = dark,
                        onDarkChange = { dark = it },
                        motion = motion,
                        onMotionChange = { motion = it },
                        haptics = if (haptics != null) hapticsOn else null,
                        onHapticsChange = { hapticsOn = it },
                    )
                }
                val page = Modifier.weight(1f).fillMaxWidth()
                when (entry) {
                    null -> MobileOverview(autoplay, onOpen = { entryId = it.id }, page)
                    else -> MobileEntryPage(entry, autoplay, page)
                }
            }
        }
    }
}

/** The name, or Back and the open entry's title; autoplay and the settings switch on the right. */
@Composable
private fun MobileTopBar(
    entry: Entry?,
    onBack: () -> Unit,
    autoplay: Boolean,
    onAutoplayChange: (Boolean) -> Unit,
    settingsOpen: Boolean,
    onSettingsOpenChange: (Boolean) -> Unit,
) {
    val colors = KapowTheme.colors
    Column(
        Modifier
            .background(colors.panel)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)),
    ) {
        Row(
            Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (entry == null) {
                Spacer(Modifier.width(4.dp))
                KapowText("Kapow", style = KapowTheme.type.subheading, color = colors.title)
                KapowCaption("gallery", color = colors.slate)
                Spacer(Modifier.weight(1f))
            } else {
                GhostPill("Back", onClick = onBack)
                KapowText(
                    entry.title,
                    Modifier.weight(1f),
                    style = KapowTheme.type.subheading,
                    color = colors.title,
                    maxLines = 1,
                )
            }
            GhostPill(if (autoplay) "Pause" else "Play", onClick = { onAutoplayChange(!autoplay) })
            GhostPill(if (settingsOpen) "Done" else "Settings", onClick = { onSettingsOpenChange(!settingsOpen) })
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(colors.hairline))
    }
}

/** Theme, reduced motion and, where the platform has a vibrator, haptics; [haptics] is null without one. */
@Composable
private fun MobileSettings(
    dark: Boolean,
    onDarkChange: (Boolean) -> Unit,
    motion: KapowMotion,
    onMotionChange: (KapowMotion) -> Unit,
    haptics: Boolean?,
    onHapticsChange: (Boolean) -> Unit,
) {
    val colors = KapowTheme.colors
    Column(
        Modifier
            .fillMaxWidth()
            .background(colors.panel)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SettingRow("Theme") {
                SegmentedControl(listOf(true, false), dark, onDarkChange, it, label = { if (it) "Dark" else "Light" })
            }
            SettingRow("Reduced motion") {
                SegmentedControl(
                    MobileMotion.entries,
                    MobileMotion.entries.first { it.motion == motion },
                    { onMotionChange(it.motion) },
                    it,
                    label = { it.label },
                )
            }
            if (haptics != null) {
                SettingRow("Haptics") {
                    SegmentedControl(
                        listOf(false, true),
                        haptics,
                        onHapticsChange,
                        it,
                        label = { if (it) "On" else "Off" },
                    )
                }
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(colors.hairline))
    }
}

@Composable
private fun SettingRow(
    label: String,
    control: @Composable (Modifier) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        KapowCaption(label, Modifier.width(120.dp))
        control(Modifier.weight(1f))
    }
}

/**
 * Every catalog entry as a live tile under its group, two columns on a phone and more on a tablet. The grid
 * composes only the tiles in view, so only those play. A tap opens the entry's page.
 */
@Composable
private fun MobileOverview(
    autoplay: Boolean,
    onOpen: (Entry) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = KapowTheme.colors
    // The tiles scroll under the home indicator or navigation bar and stop above it at the end.
    val bottom =
        WindowInsets.safeDrawing
            .only(WindowInsetsSides.Bottom)
            .asPaddingValues()
            .calculateBottomPadding()
    LazyVerticalGrid(
        GridCells.Adaptive(160.dp),
        modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 16.dp, bottom = 16.dp + bottom),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Group.entries.forEach { group ->
            val members = entries.filter { it.group == group }
            if (members.isEmpty()) return@forEach
            item(key = group.name, span = { GridItemSpan(maxLineSpan) }) {
                KapowText(
                    group.title,
                    Modifier.padding(start = 4.dp, top = if (group == Group.entries.first()) 0.dp else 12.dp),
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
}

/** The group and summary of [entry] over its live example, and the usage code under it, which folds away. */
@Composable
private fun MobileEntryPage(
    entry: Entry,
    autoplay: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = KapowTheme.colors
    var code by remember(entry) { mutableStateOf("") }
    var codeOpen by rememberSaveable { mutableStateOf(false) }
    Column(
        modifier
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal))
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(Modifier.padding(horizontal = 4.dp)) {
            KapowCaption(entry.group.title, color = colors.slate)
            KapowText(entry.summary, color = colors.ash)
        }
        GalleryStage(entry, autoplay, Modifier.weight(1f), onCode = { code = it })
        if (code.isNotEmpty()) {
            if (codeOpen) {
                CodePanel(code, Modifier.fillMaxWidth().heightIn(max = 220.dp))
            }
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                KapowCaption("Usage code", Modifier.weight(1f), color = colors.slate)
                GhostPill(if (codeOpen) "Hide code" else "Show code", onClick = { codeOpen = !codeOpen })
            }
        }
    }
}
