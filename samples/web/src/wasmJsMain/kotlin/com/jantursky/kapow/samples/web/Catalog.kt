package com.jantursky.kapow.samples.web

import androidx.compose.runtime.Composable

internal enum class Group(
    val title: String,
) {
    Change("Change effects"),
    Conditional("Conditional effects"),
    Progress("Progress effects"),
    Transitions("Transitions"),
    More("More"),
}

/** A live example: [content] is the stage with its controls, [sourceFile] the file in `examples/` shown next to it. */
internal class Example(
    val sourceFile: String,
    val content: @Composable () -> Unit,
)

/** One sidebar entry. Without an [example] its page says "Coming next". */
internal class Entry(
    val id: String,
    val group: Group,
    val title: String,
    val summary: String,
    val example: Example? = null,
)

/** Every page of the gallery, in sidebar order. To make a page live, pass its [Example]. */
internal val entries: List<Entry> =
    listOf(
        Entry(
            "change-shine",
            Group.Change,
            "Shine",
            "A band of light sweeps across the element.",
            Example(
                "ShineExample.kt",
            ) {
                ShineDemo()
            },
        ),
        Entry("change-pulse", Group.Change, "Pulse", "Rings or fills grow outward behind the element and fade."),
        Entry("change-glow", Group.Change, "Glow", "A halo flares around the element and settles."),
        Entry("change-spray", Group.Change, "Spray", "Particles burst out of the element."),
        Entry("change-rise", Group.Change, "Rise", "A label or particle floats up from the element."),
        Entry("change-spin", Group.Change, "Spin", "The element turns around an axis."),
        Entry("change-delay", Group.Change, "Delay", "Starts any change effect after a pause."),
        Entry(
            "conditional-repeat",
            Group.Conditional,
            "Repeat",
            "Plays a change effect again and again while a condition holds.",
        ),
        Entry(
            "conditional-glow",
            Group.Conditional,
            "Glow",
            "Keeps a halo around the element while a condition holds.",
        ),
        Entry(
            "conditional-push-down",
            Group.Conditional,
            "Push down",
            "Presses the element in while a condition holds.",
        ),
        Entry(
            "conditional-smoke",
            Group.Conditional,
            "Smoke",
            "Lets smoke drift off the element while a condition holds.",
        ),
        Entry("progress-shine", Group.Progress, "Shine", "A band of light that follows a progress value."),
        Entry("progress-glow", Group.Progress, "Glow", "A halo that grows with a progress value."),
        Entry("progress-iris", Group.Progress, "Iris", "A circular mask that opens with a progress value."),
        Entry("transition-iris", Group.Transitions, "Iris", "A circle opens or closes over the content."),
        Entry("transition-clock", Group.Transitions, "Clock", "A clock hand sweeps the content in or out."),
        Entry("transition-wipe", Group.Transitions, "Wipe", "A straight edge reveals or hides the content."),
        Entry("transition-glare", Group.Transitions, "Glare", "A band of light uncovers the content."),
        Entry("transition-blinds", Group.Transitions, "Blinds", "Slats open or close over the content."),
        Entry("transition-poof", Group.Transitions, "Poof", "The content disappears in a puff of smoke."),
        Entry("transition-anvil", Group.Transitions, "Anvil", "The content drops in and lands with dust."),
        Entry("transition-pop", Group.Transitions, "Pop", "The content pops in with a burst."),
        Entry("more-particle-layer", Group.More, "Particle layer", "Draws particles above clipping parents."),
        Entry(
            "more-hinge",
            Group.More,
            "Hinge",
            "Shine, glow or iris driven by the device hinge angle (kapow-crease).",
        ),
        Entry("more-feedback", Group.More, "Feedback", "Haptics or sound fired together with an effect."),
    )

internal fun entryById(id: String): Entry? = entries.firstOrNull { it.id == id }
