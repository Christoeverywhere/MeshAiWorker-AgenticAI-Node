package com.meshai.worker.ai

data class ModelInfo(
    val name: String,
    val sizeMb: Int,
    val maxContextTokens: Int,
    val maxOutputTokens: Int,
    val requiredRamMb: Long
)

interface ModelRegistry {
    val isModelInstalled: Boolean
    val currentModel: ModelInfo?
}
