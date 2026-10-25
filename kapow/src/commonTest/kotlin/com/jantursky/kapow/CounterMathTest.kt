package com.jantursky.kapow

import com.jantursky.kapow.text.RollSlot
import com.jantursky.kapow.text.SlotKind
import com.jantursky.kapow.text.countValue
import com.jantursky.kapow.text.rollDirection
import com.jantursky.kapow.text.rollSlots
import com.jantursky.kapow.text.slotProgress
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CounterMathTest {
    @Test
    fun theEndsAreExact() {
        assertEquals(0, countValue(0, 1000, 0f))
        assertEquals(1000, countValue(0, 1000, 1f))
        assertEquals(Long.MAX_VALUE, countValue(0, Long.MAX_VALUE, 1f))
        assertEquals(-7, countValue(50, -7, 1f))
    }

    @Test
    fun theValueMovesMonotonicallyUp() {
        var last = 0L
        for (i in 0..200) {
            val v = countValue(0, 1000, i / 200f)
            assertTrue(v >= last)
            last = v
        }
        assertEquals(1000, last)
    }

    @Test
    fun theValueMovesMonotonicallyDown() {
        var last = 1000L
        for (i in 0..200) {
            val v = countValue(1000, -250, i / 200f)
            assertTrue(v <= last)
            last = v
        }
        assertEquals(-250, last)
    }

    @Test
    fun theMiddleIsHalfWay() {
        assertEquals(500, countValue(0, 1000, 0.5f))
        assertEquals(150, countValue(100, 200, 0.5f))
    }

    @Test
    fun everyDigitRollsFrom199To200() {
        val slots = rollSlots("199", "200")
        assertEquals(listOf(RollSlot('1', '2'), RollSlot('9', '0'), RollSlot('9', '0')), slots)
        assertEquals(listOf(SlotKind.Roll, SlotKind.Roll, SlotKind.Roll), slots.map { it.kind })
    }

    @Test
    fun aNewLeadingDigitFadesIn() {
        val slots = rollSlots("9", "10")
        assertEquals(listOf(RollSlot(null, '1'), RollSlot('9', '0')), slots)
        assertEquals(listOf(SlotKind.Fade, SlotKind.Roll), slots.map { it.kind })
    }

    @Test
    fun aLostLeadingDigitFadesOut() {
        val slots = rollSlots("10", "9")
        assertEquals(listOf(RollSlot('1', null), RollSlot('0', '9')), slots)
        assertEquals(listOf(SlotKind.Fade, SlotKind.Roll), slots.map { it.kind })
    }

    @Test
    fun unchangedSlotsStayAndSeparatorsFade() {
        val slots = rollSlots("1,999", "2,000")
        assertEquals(
            listOf(SlotKind.Roll, SlotKind.Static, SlotKind.Roll, SlotKind.Roll, SlotKind.Roll),
            slots.map { it.kind },
        )
        assertEquals(SlotKind.Static, RollSlot('3', '3').kind)
        assertEquals(SlotKind.Fade, RollSlot('9', ',').kind)
        assertEquals(SlotKind.Fade, RollSlot('-', '5').kind)
        assertEquals(listOf(RollSlot('4', '4'), RollSlot('2', '2')), rollSlots("42", "42"))
    }

    @Test
    fun theSignOfTheChangePicksTheDirection() {
        assertEquals(1, rollDirection(199, 200))
        assertEquals(-1, rollDirection(10, 9))
        assertEquals(-1, rollDirection(0, -5))
    }

    @Test
    fun slotsStartFromTheRightAndAllEndTogetherWithTheMove() {
        // Four slots, the last character is number 0.
        assertEquals(0f, slotProgress(0f, 0, 4, 0.4f))
        assertEquals(0f, slotProgress(0.3f, 3, 4, 0.4f))
        assertTrue(slotProgress(0.3f, 0, 4, 0.4f) > slotProgress(0.3f, 1, 4, 0.4f))
        assertTrue(slotProgress(0.5f, 1, 4, 0.4f) > slotProgress(0.5f, 2, 4, 0.4f))
        for (slot in 0 until 4) assertEquals(1f, slotProgress(1f, slot, 4, 0.4f))
        assertEquals(1f, slotProgress(0.6f, 0, 4, 0.4f))
        assertEquals(1f, slotProgress(1f, 0, 1, 0.4f))
        assertEquals(0f, slotProgress(0f, 0, 1, 0.4f))
    }
}
