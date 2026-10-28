package com.jantursky.kapow

import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.HoverInteraction
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.ui.geometry.Offset
import com.jantursky.kapow.internal.InteractionTracker
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class InteractionTrackerTest {
    @Test
    fun theTrackerFollowsEachKindOfInteraction() {
        val press = PressInteraction.Press(Offset.Zero)
        val pressed = InteractionTracker(KapowInteraction.Pressed)
        assertTrue(pressed.onInteraction(press))
        assertTrue(pressed.isActive)
        assertFalse(pressed.onInteraction(HoverInteraction.Enter()), "other kinds are ignored")
        assertTrue(pressed.onInteraction(PressInteraction.Cancel(press)))
        assertFalse(pressed.isActive)

        val enter = HoverInteraction.Enter()
        val hovered = InteractionTracker(KapowInteraction.Hovered)
        assertTrue(hovered.onInteraction(enter))
        assertTrue(hovered.onInteraction(HoverInteraction.Exit(enter)))

        val focus = FocusInteraction.Focus()
        val focused = InteractionTracker(KapowInteraction.Focused)
        assertTrue(focused.onInteraction(focus))
        assertTrue(focused.onInteraction(FocusInteraction.Unfocus(focus)))

        val start = DragInteraction.Start()
        val dragged = InteractionTracker(KapowInteraction.Dragged)
        assertTrue(dragged.onInteraction(start))
        assertTrue(dragged.onInteraction(DragInteraction.Cancel(start)))
        assertFalse(dragged.isActive)
    }

    @Test
    fun twoOverlappingPressesStayActiveUntilBothEnd() {
        val tracker = InteractionTracker(KapowInteraction.Pressed)
        val a = PressInteraction.Press(Offset.Zero)
        val b = PressInteraction.Press(Offset.Zero)
        tracker.onInteraction(a)
        assertFalse(tracker.onInteraction(b))
        assertFalse(tracker.onInteraction(PressInteraction.Release(a)))
        assertTrue(tracker.onInteraction(PressInteraction.Release(b)))
    }
}
