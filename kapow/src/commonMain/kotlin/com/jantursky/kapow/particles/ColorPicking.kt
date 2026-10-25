package com.jantursky.kapow.particles

import androidx.compose.ui.graphics.Color
import com.jantursky.kapow.ColorPick
import com.jantursky.kapow.ParticleContent
import kotlin.random.Random

/**
 * One color for each of [count] particles, in particle order. [ColorPick.Sequential] (or null) takes [colors] in
 * turn; [ColorPick.Random] draws each from the seeded generator, so the same seed gives the same colors.
 */
internal fun pickColors(
    colors: List<Color>,
    pick: ColorPick?,
    count: Int,
    seed: Int,
): List<Color> {
    if (pick != ColorPick.Random) return colors
    val random = Random(seed xor COLOR_SALT)
    return List(count) { colors[random.nextInt(colors.size)] }
}

/** This content with the colors of its built-in shape picked for [count] particles. Other content has none. */
internal fun ParticleContent.picked(
    pick: ColorPick?,
    count: Int,
    seed: Int,
): ParticleContent =
    if (this is ParticleContent.Shape && pick == ColorPick.Random) {
        copy(colors = pickColors(colors, pick, count, seed))
    } else {
        this
    }

private const val COLOR_SALT = 0x5EED
