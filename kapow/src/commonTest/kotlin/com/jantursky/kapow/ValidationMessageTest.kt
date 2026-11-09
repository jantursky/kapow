package com.jantursky.kapow

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class ValidationMessageTest {
    /** One bad call: the argument it breaks, the value it passes (as text) and the call itself. */
    private class Bad(
        val name: String,
        val value: String,
        val call: () -> Unit,
    )

    private val table =
        listOf(
            // Change effects and their wrappers.
            Bad("count", "0") { ChangeEffect.shine().times(0) },
            Bad("delay", "-1s") { ChangeEffect.shine().delay((-1).seconds) },
            Bad("cooldown", "-2s") { ChangeEffect.shine().cooldown((-2).seconds) },
            Bad("multiplier", "0.0") { ChangeEffect.shine().speed(0f) },
            Bad("count", "0") { ChangeEffect.spray(count = 0) },
            Bad("swings", "21") { ChangeEffect.shake(swings = 21) },
            Bad("hops", "6") { ChangeEffect.jump(hops = 6) },
            Bad("shading", "2.0") { ChangeEffect.spin(shading = 2f) },
            Bad("overshoot", "1.0") { ChangeEffect.spin(overshoot = 1f) },
            // Conditional effects and their wrappers.
            Bad("rings", "0") { ConditionalEffect.ping(rings = 0) },
            Bad("duration", "-1s") { ConditionalEffect.ping().fadeIn((-1).seconds) },
            Bad("multiplier", "-1.0") { ConditionalEffect.ping().speed(-1f) },
            // Progress effects.
            Bad("bandWidth", "0.0") { ProgressEffect.shine(bandWidth = 0f) },
            // Transitions.
            Bad("ringCount", "9") { KapowEnter.pop(ringCount = 9) },
            Bad("squash", "2.0") { KapowEnter.boing(squash = 2f) },
            Bad("degrees", "0.0") { KapowExit.rotate3D(degrees = 0f) },
            Bad("delay", "-1s") { KapowEnter.iris().delay((-1).seconds) },
            Bad("lean", "90.0") { KapowEnter.skid(lean = 90f) },
            // Shapes, physics and stagger.
            Bad("index", "-1") { KapowStagger.delay(-1) },
            Bad("points", "2") { StarShape(points = 2) },
            Bad("drag", "0.0") { ParticlePhysics.of(drag = 0f) },
        )

    @Test
    fun everyRejectedArgumentNamesItselfAndItsValue() {
        for (bad in table) {
            val error = assertFailsWith<IllegalArgumentException>("${bad.name} = ${bad.value}") { bad.call() }
            val message = error.message.orEmpty()
            assertTrue(message.startsWith("${bad.name} must "), "starts with the name and the rule: $message")
            assertTrue(message.endsWith(", was ${bad.value}"), "ends with the value ${bad.value}: $message")
        }
    }

    @Test
    fun theMessageSaysWhatWasRequired() {
        val message = assertFailsWith<IllegalArgumentException> { ChangeEffect.shine().times(0) }.message
        assertEquals("count must be positive, was 0", message)
    }

    @Test
    fun theSmallestAcceptedValuesAreStillAccepted() {
        ChangeEffect.shine().times(1)
        ChangeEffect.shine().delay(0.seconds)
        ChangeEffect.spray(count = 1)
        ChangeEffect.shake(swings = 1)
        ChangeEffect.jump(hops = 5)
        ChangeEffect.spin(overshoot = 0.95f)
        ConditionalEffect.ping(rings = 1)
        KapowEnter.pop(ringCount = 5)
        KapowEnter.boing(squash = 1f)
        KapowStagger.delay(0)
        StarShape(points = 3)
        ParticlePhysics.of(drag = 1f)
    }
}
