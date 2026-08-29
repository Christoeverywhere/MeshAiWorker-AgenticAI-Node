package com.meshai.worker.ui

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.meshai.worker.device.DeviceInfoProvider
import com.meshai.worker.model.NodeInfo
import com.meshai.worker.network.NetworkManager
import com.meshai.worker.repository.WorkerRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.*

enum class WorkerState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR
}

class WorkerViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: WorkerRepository
    private val prefs = application.getSharedPreferences("meshai_worker_prefs", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(WorkerUiState())
    val uiState: StateFlow<WorkerUiState> = _uiState.asStateFlow()

    private var heartbeatJob: Job? = null
    private var reconnectJob: Job? = null
    private var retryDelayMs = 2000L
    private val maxRetryDelayMs = 10000L

    init {
        val deviceInfoProvider = DeviceInfoProvider(application)
        val networkManager = NetworkManager()
        repository = WorkerRepository(deviceInfoProvider, networkManager)

        val savedUrl = prefs.getString("orchestrator_url", "http://192.168.0.107:8000") ?: "http://192.168.0.107:8000"
        val deviceInfo = repository.getDeviceInfo()

        _uiState.value = _uiState.value.copy(
            orchestratorUrl = savedUrl,
            nodeInfo = deviceInfo
        )
    }

    fun updateOrchestratorUrl(url: String) {
        _uiState.value = _uiState.value.copy(orchestratorUrl = url)
        prefs.edit().putString("orchestrator_url", url).apply()
    }

    fun connect() {
        if (_uiState.value.workerState == WorkerState.CONNECTED || _uiState.value.workerState == WorkerState.CONNECTING) return

        stopHeartbeat()
        stopReconnection()
        
        _uiState.value = _uiState.value.copy(
            workerState = WorkerState.CONNECTING,
            errorMessage = ""
        )

        viewModelScope.launch(Dispatchers.IO) {
            val nodeInfo = repository.getDeviceInfo()
            val result = repository.register(_uiState.value.orchestratorUrl, nodeInfo)

            withContext(Dispatchers.Main) {
                if (result.isSuccess) {
                    _uiState.value = _uiState.value.copy(
                        workerState = WorkerState.CONNECTED,
                        nodeInfo = nodeInfo,
                        errorMessage = "Connected to MeshAI Orchestrator"
                    )
                    retryDelayMs = 2000L
                    startHeartbeat()
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Unknown error"
                    _uiState.value = _uiState.value.copy(
                        workerState = WorkerState.ERROR,
                        errorMessage = "Unable to reach orchestrator: $error"
                    )
                    scheduleReconnection()
                }
            }
        }
    }

    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                val battery = repository.getBatteryPercent()
                val result = repository.heartbeat(
                    _uiState.value.orchestratorUrl,
                    _uiState.value.nodeInfo?.nodeId ?: "",
                    battery
                )

                withContext(Dispatchers.Main) {
                    if (result.isSuccess) {
                        val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                        _uiState.value = _uiState.value.copy(
                            lastHeartbeat = "$time (Success)",
                            nodeInfo = _uiState.value.nodeInfo?.copy(batteryPercent = battery)
                        )
                    } else {
                        val error = result.exceptionOrNull()?.message ?: ""
                        if (error.contains("404")) {
                            Log.w("MeshAI", "Node not recognized by server. Re-registering...")
                            stopHeartbeat()
                            connect()
                        } else {
                            _uiState.value = _uiState.value.copy(
                                workerState = WorkerState.ERROR,
                                errorMessage = "Heartbeat failed: $error"
                            )
                            stopHeartbeat()
                            scheduleReconnection()
                        }
                    }
                }
                delay(3000)
            }
        }
    }

    private fun stopHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = null
    }

    private fun scheduleReconnection() {
        stopReconnection()
        reconnectJob = viewModelScope.launch {
            delay(retryDelayMs)
            Log.d("MeshAI", "Attempting automatic reconnection...")
            retryDelayMs = (retryDelayMs * 2).coerceAtMost(maxRetryDelayMs)
            connect()
        }
    }

    private fun stopReconnection() {
        reconnectJob?.cancel()
        reconnectJob = null
    }

    fun disconnect() {
        stopHeartbeat()
        stopReconnection()
        _uiState.value = _uiState.value.copy(
            workerState = WorkerState.DISCONNECTED,
            errorMessage = "Disconnected by user"
        )
    }

    override fun onCleared() {
        super.onCleared()
        stopHeartbeat()
        stopReconnection()
    }
}

data class WorkerUiState(
    val orchestratorUrl: String = "",
    val workerState: WorkerState = WorkerState.DISCONNECTED,
    val nodeInfo: NodeInfo? = null,
    val lastHeartbeat: String = "Never",
    val errorMessage: String = ""
)
