package com.meshai.worker.model

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class NodeInfo(
    @SerialName("node_id")
    val nodeId: String,

    @SerialName("device_name")
    val deviceName: String,

    @SerialName("device_type")
    @EncodeDefault
    val deviceType: String = "android",

    @SerialName("operating_system")
    val operatingSystem: String,

    @SerialName("ram_mb")
    val ramMb: Long,

    @SerialName("cpu_cores")
    val cpuCores: Int,

    @SerialName("battery_percent")
    val batteryPercent: Int,

    @SerialName("capabilities")
    @EncodeDefault
    val capabilities: List<String> = listOf("worker"),

    @SerialName("port")
    @EncodeDefault
    val port: Int = 8080,
    
    // Phase 6: Hardware-Adaptive AI Extensions
    @SerialName("available_ram_mb")
    val availableRamMb: Long? = null,
    
    @SerialName("cpu_architecture")
    val cpuArchitecture: String? = null,
    
    @SerialName("ai_runtime")
    val aiRuntime: String? = null,
    
    @SerialName("llm_available")
    val llmAvailable: Boolean? = null,
    
    @SerialName("model_name")
    val modelName: String? = null,
    
    @SerialName("model_size_mb")
    val modelSizeMb: Int? = null,
    
    @SerialName("max_context_tokens")
    val maxContextTokens: Int? = null,
    
    @SerialName("max_output_tokens")
    val maxOutputTokens: Int? = null,
    
    @SerialName("max_concurrent_inference")
    val maxConcurrentInference: Int? = null
)

object Capabilities {
    const val WORKER = "worker"
    const val CALCULATE = "calculate"
    const val LLM = "llm"
}
