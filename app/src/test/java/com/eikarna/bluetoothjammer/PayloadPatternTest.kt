package com.eikarna.bluetoothjammer

import api.PayloadPattern
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class PayloadPatternTest {

    @Test
    fun fixedPattern_sizeIsExact() {
        assertEquals(600, PayloadPattern.FIXED.buffer(600).size)
    }

    @Test
    fun sawtooth_cyclesThroughBytes() {
        val b = PayloadPattern.SAWTOOTH.buffer(512)
        assertEquals(0, b[0].toInt() and 0xFF)
        assertEquals(255, b[255].toInt() and 0xFF)
        assertEquals(0, b[256].toInt() and 0xFF)
    }

    @Test
    fun random_variesBetweenCalls() {
        val a = PayloadPattern.RANDOM.buffer(64)
        val b = PayloadPattern.RANDOM.buffer(64)
        assertNotEquals(a.contentToString(), b.contentToString())
    }
}
