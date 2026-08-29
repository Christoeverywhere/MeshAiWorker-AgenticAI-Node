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
    val port: Int = 8080
)
