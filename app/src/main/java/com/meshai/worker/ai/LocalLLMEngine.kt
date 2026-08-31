package com.meshai.worker.ai

data class GenerationConfig(
    val maxTokens: Int = 512,
    val temperature: Float = 0.7f
)

data class GenerationResult(
    val text: String,
    val error: String? = null
)

interface LocalLLMEngine {
    val isAvailable: Boolean
    val aiRuntimeName: String
    
    suspend fun generate(prompt: String, config: GenerationConfig): GenerationResult
}
