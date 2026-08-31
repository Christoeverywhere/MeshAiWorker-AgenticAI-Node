package com.meshai.worker.ai

import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import android.util.Log

class MediaPipeLLMEngine(private val context: Context, private val modelPath: String) : LocalLLMEngine {

    private var llmInference: LlmInference? = null
    override var isAvailable: Boolean = false
        private set

    override val aiRuntimeName: String = "MediaPipe GenAI API"

    fun initialize() {
        if (!File(modelPath).exists()) {
            Log.e("MeshAI", "Model file not found at: $modelPath")
            isAvailable = false
            return
        }
        try {
            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(modelPath)
                .setMaxTokens(512)
                .build()
            
            llmInference = LlmInference.createFromOptions(context, options)
            isAvailable = true
            Log.i("MeshAI", "MediaPipe LLM Engine initialized successfully")
        } catch (e: Exception) {
            Log.e("MeshAI", "Failed to initialize MediaPipe LLM: ${e.message}")
            isAvailable = false
        }
    }

    override suspend fun generate(prompt: String, config: GenerationConfig): GenerationResult = withContext(Dispatchers.IO) {
        if (!isAvailable || llmInference == null) {
            return@withContext GenerationResult(text = "", error = "Local LLM model unavailable or not initialized")
        }
        
        try {
            Log.d("MeshAI", "Generating response for prompt length: ${prompt.length}")
            val response = llmInference!!.generateResponse(prompt)
            GenerationResult(text = response)
        } catch (e: Exception) {
            Log.e("MeshAI", "Inference failed: ${e.message}")
            GenerationResult(text = "", error = "Inference failed: ${e.message}")
        }
    }
    
    fun close() {
        llmInference?.close()
        llmInference = null
        isAvailable = false
    }
}
