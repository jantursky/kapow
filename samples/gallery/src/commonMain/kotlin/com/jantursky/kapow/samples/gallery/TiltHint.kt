package com.jantursky.kapow.samples.gallery

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput

/** The hint under the tilt example: the gesture that works for the pointer type the visitor used last. */
internal fun tiltHint(touch: Boolean): String =
    if (touch) "Press and drag on the card to tilt it." else "Move the pointer over the card to tilt it."

/** Whether a pointer of [type] is a finger, which tilts by dragging rather than by hovering. */
internal fun isTouch(type: PointerType): Boolean = type == PointerType.Touch

/**
 * Reports, on every pointer event over the stage, whether the last pointer was a touch. It only watches the
 * events: nothing is consumed, so the card still tilts and takes its taps.
 */
internal fun Modifier.watchPointerKind(onTouch: (Boolean) -> Unit): Modifier =
    pointerInput(onTouch) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val type = event.changes.firstOrNull()?.type ?: continue
                onTouch(isTouch(type))
            }
        }
    }
