package com.meshai.worker.ai

import org.junit.Assert.assertEquals
import org.junit.Test

class HardwareProfileTest {

    @Test
    fun testResourceClassification_4gb() {
        val klass = HardwareProfile.classify(3700) // 4GB devices often show ~3700MB total
        assertEquals(ResourceClass.ENTRY, klass)
    }

    @Test
    fun testResourceClassification_6gb() {
        val klass = HardwareProfile.classify(5600)
        assertEquals(ResourceClass.STANDARD, klass)
    }

    @Test
    fun testResourceClassification_8gb() {
        val klass = HardwareProfile.classify(7500)
        assertEquals(ResourceClass.HIGH, klass)
    }
}
