package com.meshai.worker.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import androidx.test.core.app.ApplicationProvider
import android.content.Context
import java.io.File

@RunWith(RobolectricTestRunner::class)
class AndroidModelRegistryTest {

    @Test
    fun testModelMissing() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val registry = AndroidModelRegistry(context)
        
        // Ensure file does not exist
        val file = registry.getModelFile()
        if (file.exists()) file.delete()

        assertFalse(registry.isModelInstalled)
        assertEquals(null, registry.currentModel)
    }

    @Test
    fun testModelExists() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val registry = AndroidModelRegistry(context)
        
        // Create dummy file
        val file = registry.getModelFile()
        file.parentFile?.mkdirs()
        file.writeText("dummy data")

        assertTrue(registry.isModelInstalled)
        assertEquals("MediaPipe Quantized LLM", registry.currentModel?.name)
        
        file.delete()
    }
}
