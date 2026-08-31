package com.meshai.worker.ai

import android.content.Context
import java.io.File
import android.util.Log

class AndroidModelRegistry(private val context: Context) : ModelRegistry {

    // Predictable local model directory: /data/user/0/com.meshai.worker/files/models
    private val modelDir = File(context.filesDir, "models")
    
    // Hardcoded for Step 7: We expect a Gemma 2B INT4 or Falcon 1B INT4 model formatted for MediaPipe.
    // For example, "gemma-2b-it-cpu-int4.task" or "falcon-1b-int4.task"
    private val expectedModelName = "model.task"
    
    override val isModelInstalled: Boolean
        get() = getModelFile().exists()

    override val currentModel: ModelInfo?
        get() {
            val file = getModelFile()
            if (!file.exists()) return null
            
            // In a real production app, we would parse model metadata.
            // For Step 7, we infer metadata based on the presence of the mediapipe model file.
            val sizeMb = (file.length() / (1024 * 1024)).toInt()
            
            return ModelInfo(
                name = "MediaPipe Quantized LLM",
                sizeMb = sizeMb,
                maxContextTokens = 1024,
                maxOutputTokens = 512,
                requiredRamMb = sizeMb.toLong() + 500L // model size + 500MB overhead
            )
        }

    fun getModelFile(): File {
        if (!modelDir.exists()) {
            modelDir.mkdirs()
        }
        return File(modelDir, expectedModelName)
    }
}
