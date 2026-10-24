package com.jantursky.kapow.samples.gallery

import androidx.compose.runtime.Composable

/** The sidebar groups, in order. */
enum class Group(
    val title: String,
) {
    Change("Change effects"),
    Conditional("Conditional effects"),
    Progress("Progress effects"),
    Transitions("Transitions"),
    More("More"),
}

/** A live example: [content] is the stage with its controls, [sourceFile] the file in `examples/` shown next to it. */
class Example(
    val sourceFile: String,
    val content: @Composable () -> Unit,
)

/** One sidebar entry and its page. */
class Entry(
    val id: String,
    val group: Group,
    val title: String,
    val summary: String,
    val example: Example,
)

/** Every page of the gallery, in sidebar order. */
val entries: List<Entry> =
    listOf(
        Entry(
            "change-shine",
            Group.Change,
            "Shine",
            "A band of light sweeps across the element.",
            Example("ShineExample.kt") { ShineDemo() },
        ),
        Entry(
            "change-pulse",
            Group.Change,
            "Pulse",
            "Rings or fills grow outward behind the element and fade.",
            Example("PulseExample.kt") { PulseDemo() },
        ),
        Entry(
            "change-glow",
            Group.Change,
            "Glow",
            "A halo flares around the element and settles.",
            Example("GlowExample.kt") { GlowDemo() },
        ),
        Entry(
            "change-spray",
            Group.Change,
            "Spray",
            "Particles burst out of the element.",
            Example("SprayExample.kt") { SprayDemo() },
        ),
        Entry(
            "change-rise",
            Group.Change,
            "Rise",
            "A label or particle floats up from the element.",
            Example("RiseExample.kt") { RiseDemo() },
        ),
        Entry(
            "change-spin",
            Group.Change,
            "Spin",
            "The element turns around an axis.",
            Example("SpinExample.kt") { SpinDemo() },
        ),
        Entry(
            "change-delay",
            Group.Change,
            "Delay",
            "Starts any change effect after a pause.",
            Example("DelayExample.kt") { DelayDemo() },
        ),
        Entry(
            "conditional-repeat",
            Group.Conditional,
            "Repeat",
            "Plays a change effect again and again while a condition holds.",
            Example("RepeatExample.kt") { RepeatDemo() },
        ),
        Entry(
            "conditional-glow",
            Group.Conditional,
            "Glow",
            "Keeps a halo around the element while a condition holds.",
            Example("ConditionalGlowExample.kt") { ConditionalGlowDemo() },
        ),
        Entry(
            "conditional-push-down",
            Group.Conditional,
            "Push down",
            "Presses the element in while a condition holds.",
            Example("PushDownExample.kt") { PushDownDemo() },
        ),
        Entry(
            "conditional-smoke",
            Group.Conditional,
            "Smoke",
            "Lets smoke drift off the element while a condition holds.",
            Example("SmokeExample.kt") { SmokeDemo() },
        ),
        Entry(
            "progress-shine",
            Group.Progress,
            "Shine",
            "A band of light that follows a progress value.",
            Example("ProgressShineExample.kt") { ProgressShineDemo() },
        ),
        Entry(
            "progress-glow",
            Group.Progress,
            "Glow",
            "A halo that grows with a progress value.",
            Example("ProgressGlowExample.kt") { ProgressGlowDemo() },
        ),
        Entry(
            "progress-scroll",
            Group.Progress,
            "Scroll",
            "An effect that follows how far a list has scrolled.",
            Example("ScrollProgressExample.kt") { ScrollProgressDemo() },
        ),
        Entry(
            "progress-iris",
            Group.Progress,
            "Iris",
            "A circular mask that opens with a progress value.",
            Example("ProgressIrisExample.kt") { ProgressIrisDemo() },
        ),
        Entry(
            "transition-iris",
            Group.Transitions,
            "Iris",
            "A circle opens or closes over the content.",
            Example("IrisExample.kt") { IrisDemo() },
        ),
        Entry(
            "transition-clock",
            Group.Transitions,
            "Clock",
            "A clock hand sweeps the content in or out.",
            Example("ClockExample.kt") { ClockDemo() },
        ),
        Entry(
            "transition-wipe",
            Group.Transitions,
            "Wipe",
            "A straight edge reveals or hides the content.",
            Example("WipeExample.kt") { WipeDemo() },
        ),
        Entry(
            "transition-glare",
            Group.Transitions,
            "Glare",
            "A band of light uncovers the content.",
            Example("GlareExample.kt") { GlareDemo() },
        ),
        Entry(
            "transition-blinds",
            Group.Transitions,
            "Blinds",
            "Slats open or close over the content.",
            Example("BlindsExample.kt") { BlindsDemo() },
        ),
        Entry(
            "transition-poof",
            Group.Transitions,
            "Poof",
            "The content disappears in a puff of smoke.",
            Example("PoofExample.kt") { PoofDemo() },
        ),
        Entry(
            "transition-anvil",
            Group.Transitions,
            "Anvil",
            "The content drops in and lands with dust.",
            Example("AnvilExample.kt") { AnvilDemo() },
        ),
        Entry(
            "transition-pop",
            Group.Transitions,
            "Pop",
            "The content pops in with a burst.",
            Example("PopExample.kt") { PopDemo() },
        ),
        Entry(
            "transition-dissolve",
            Group.Transitions,
            "Dissolve",
            "The content appears or vanishes square by square.",
            Example("DissolveExample.kt") { DissolveDemo() },
        ),
        Entry(
            "transition-curtain",
            Group.Transitions,
            "Curtain",
            "Two halves part to reveal the content.",
            Example("CurtainExample.kt") { CurtainDemo() },
        ),
        Entry(
            "transition-shape-iris",
            Group.Transitions,
            "Shape iris",
            "A star or any shape opens over the content.",
            Example("ShapeIrisExample.kt") { ShapeIrisDemo() },
        ),
        Entry(
            "transition-checker",
            Group.Transitions,
            "Checker",
            "A checkerboard opens over the content in two waves.",
            Example("CheckerExample.kt") { CheckerDemo() },
        ),
        Entry(
            "transition-tear",
            Group.Transitions,
            "Tear",
            "A torn paper edge reveals or hides the content.",
            Example("TearExample.kt") { TearDemo() },
        ),
        Entry(
            "transition-blob",
            Group.Transitions,
            "Blob",
            "A wobbly blob grows over the content.",
            Example("BlobExample.kt") { BlobDemo() },
        ),
        Entry(
            "transition-shatter",
            Group.Transitions,
            "Shatter",
            "The content breaks into falling shards.",
            Example("ShatterExample.kt") { ShatterDemo() },
        ),
        Entry(
            "transition-disintegrate",
            Group.Transitions,
            "Disintegrate",
            "The content turns to drifting dust.",
            Example("DisintegrateExample.kt") { DisintegrateDemo() },
        ),
        Entry(
            "more-particle-layer",
            Group.More,
            "Particle layer",
            "Draws particles above clipping parents.",
            Example("ClippedRowExample.kt") { ClippedRowDemo() },
        ),
        Entry(
            "more-hinge",
            Group.More,
            "Hinge",
            "Shine, glow or iris driven by the device hinge angle (kapow-crease).",
            Example("HingeExample.kt") { HingeDemo() },
        ),
        Entry(
            "more-feedback",
            Group.More,
            "Feedback",
            "Haptics or sound fired together with an effect.",
            Example("FeedbackExample.kt") { FeedbackDemo() },
        ),
    )

/** The entry with [id], the part of a link after `#`, or null. */
fun entryById(id: String): Entry? = entries.firstOrNull { it.id == id }
