package com.meshai.worker.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class RegistrationResponse(
    val status: String,
    val message: String? = null
)

@Serializable
data class HeartbeatResponse(
    val status: String
)

@Serializable
data class TaskResponse(
    val task_id: String,
    val task_type: String,
    val status: String,
    val payload: JsonObject? = null
)

@Serializable
data class TaskResultRequest(
    val node_id: String,
    val status: String,
    val result: String? = null,
    val error: String? = null
)
