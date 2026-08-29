package com.meshai.worker.network

import android.util.Log
import com.meshai.worker.model.NodeInfo
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import kotlin.coroutines.resume

class NetworkManager {
    private val client = OkHttpClient()
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    private val mediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun registerNode(baseUrl: String, nodeInfo: NodeInfo): Result<Unit> = suspendCancellableCoroutine { continuation ->
        val url = "$baseUrl/api/v1/nodes/register"
        val jsonBody = json.encodeToString(nodeInfo)
        val body = jsonBody.toRequestBody(mediaType)
        
        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        val call = client.newCall(request)
        continuation.invokeOnCancellation { call.cancel() }

        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("MeshAI", "Registration failed: ${e.message}")
                if (continuation.isActive) continuation.resume(Result.failure(e))
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (it.isSuccessful) {
                        Log.d("MeshAI", "Registration successful")
                        if (continuation.isActive) continuation.resume(Result.success(Unit))
                    } else {
                        Log.e("MeshAI", "Registration error: ${it.code}")
                        if (continuation.isActive) continuation.resume(Result.failure(IOException("Unexpected code $it")))
                    }
                }
            }
        })
    }

    suspend fun sendHeartbeat(baseUrl: String, nodeId: String, batteryPercent: Int): Result<Unit> = suspendCancellableCoroutine { continuation ->
        val url = "$baseUrl/api/v1/nodes/$nodeId/heartbeat"
        val jsonBody = buildJsonObject {
            put("battery_percent", batteryPercent)
        }.toString()
        val body = jsonBody.toRequestBody(mediaType)

        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        val call = client.newCall(request)
        continuation.invokeOnCancellation { call.cancel() }

        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("MeshAI", "Heartbeat failed: ${e.message}")
                if (continuation.isActive) continuation.resume(Result.failure(e))
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (it.isSuccessful) {
                        Log.d("MeshAI", "Heartbeat successful")
                        if (continuation.isActive) continuation.resume(Result.success(Unit))
                    } else if (it.code == 404) {
                        Log.w("MeshAI", "Node not found during heartbeat (404)")
                        if (continuation.isActive) continuation.resume(Result.failure(IOException("Node not found (404)")))
                    } else {
                        Log.e("MeshAI", "Heartbeat error: ${it.code}")
                        if (continuation.isActive) continuation.resume(Result.failure(IOException("Unexpected code $it")))
                    }
                }
            }
        })
    }
}
