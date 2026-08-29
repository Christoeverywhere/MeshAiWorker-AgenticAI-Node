package com.meshai.worker.model

import kotlinx.serialization.Serializable

@Serializable
data class RegistrationResponse(
    val status: String,
    val message: String? = null
)

@Serializable
data class HeartbeatResponse(
    val status: String
)
