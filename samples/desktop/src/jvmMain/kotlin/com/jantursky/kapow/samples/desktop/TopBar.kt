package com.jantursky.kapow.samples.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jantursky.kapow.KapowMotion
import com.jantursky.kapow.samples.design.GhostPill
import com.jantursky.kapow.samples.design.KapowCaption
import com.jantursky.kapow.samples.design.KapowText
import com.jantursky.kapow.samples.design.KapowTheme
import com.jantursky.kapow.samples.design.SegmentedControl

/** The reduced-motion choice as the web gallery words it: System, Off (full motion) or On (reduced). */
private enum class ReducedMotion(
    val label: String,
    val motion: KapowMotion,
) {
    System("System", KapowMotion.System),
    Off("Off", KapowMotion.Full),
    On("On", KapowMotion.Reduced),
}

/** The name on the left; autoplay, reduced motion and the theme on the right. */
@Composable
internal fun TopBar(
    dark: Boolean,
    onDarkChange: (Boolean) -> Unit,
    motion: KapowMotion,
    onMotionChange: (KapowMotion) -> Unit,
    autoplay: Boolean,
    onAutoplayChange: (Boolean) -> Unit,
) {
    val colors = KapowTheme.colors
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(colors.panel)
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            KapowText("Kapow", style = KapowTheme.type.subheading, color = colors.title)
            KapowCaption("gallery", color = colors.slate)
            Box(Modifier.weight(1f))
            GhostPill(if (autoplay) "Pause" else "Play", onClick = { onAutoplayChange(!autoplay) })
            Box(Modifier.width(8.dp))
            KapowCaption("Reduced motion")
            SegmentedControl(
                ReducedMotion.entries,
                ReducedMotion.entries.first { it.motion == motion },
                { onMotionChange(it.motion) },
                Modifier.width(260.dp),
                label = { it.label },
            )
            Box(Modifier.width(8.dp))
            SegmentedControl(
                listOf(true, false),
                dark,
                onDarkChange,
                Modifier.width(150.dp),
                label = { if (it) "Dark" else "Light" },
            )
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(colors.hairline))
    }
}
