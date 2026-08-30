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
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.double
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
    private var taskPollingJob: Job? = null
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
        stopTaskPolling()
        
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
                    startTaskPolling()
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
                            stopTaskPolling()
                            connect()
                        } else {
                            _uiState.value = _uiState.value.copy(
                                workerState = WorkerState.ERROR,
                                errorMessage = "Heartbeat failed: $error"
                            )
                            stopHeartbeat()
                            stopTaskPolling()
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

    private fun startTaskPolling() {
        taskPollingJob?.cancel()
        taskPollingJob = viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                val nodeId = _uiState.value.nodeInfo?.nodeId ?: ""
                val baseUrl = _uiState.value.orchestratorUrl
                if (nodeId.isNotEmpty()) {
                    val result = repository.pollTasks(baseUrl, nodeId)
                    val task = result.getOrNull()
                    if (task != null) {
                        withContext(Dispatchers.Main) {
                            _uiState.value = _uiState.value.copy(currentTask = task.task_id)
                        }

                        var resultStatus = "FAILED"
                        var resultData: String? = null
                        var error: String? = null

                        if (task.task_type == "PING") {
                            delay(500) // Simulate work
                            resultStatus = "COMPLETED"
                            resultData = "PONG"
                            withContext(Dispatchers.Main) {
                                _uiState.value = _uiState.value.copy(tasksCompleted = _uiState.value.tasksCompleted + 1)
                            }
                        } else if (task.task_type == "CALCULATE") {
                            Log.d("MeshAI", "[WORKER] Executing CALCULATE")
                            try {
                                val operation = task.payload?.get("operation")?.jsonPrimitive?.content
                                val valuesArray = task.payload?.get("values")?.jsonArray
                                val testDelayMsStr = task.payload?.get("test_delay_ms")?.jsonPrimitive?.content
                                
                                if (operation == null || valuesArray == null || valuesArray.isEmpty()) {
                                    throw IllegalArgumentException("Malformed payload or empty values")
                                }

                                if (testDelayMsStr != null) {
                                    val delayMs = testDelayMsStr.toLongOrNull()
                                    if (delayMs != null && delayMs in 100L..10000L) {
                                        Log.d("MeshAI", "[WORKER] Test delay active: $delayMs ms")
                                        delay(delayMs)
                                    } else {
                                        throw IllegalArgumentException("Invalid test_delay_ms: $testDelayMsStr")
                                    }
                                }

                                val numbers = valuesArray.map { it.jsonPrimitive.double }
                                Log.d("MeshAI", "[WORKER] Operation: $operation")
                                
                                var calcResult = 0.0
                                when (operation) {
                                    "SUM" -> {
                                        calcResult = numbers.sum()
                                    }
                                    "SUBTRACT" -> {
                                        calcResult = numbers.first() - numbers.drop(1).sum()
                                    }
                                    "MULTIPLY" -> {
                                        calcResult = numbers.fold(1.0) { acc, d -> acc * d }
                                    }
                                    else -> {
                                        throw IllegalArgumentException("Invalid operation: $operation")
                                    }
                                }
                                
                                val formattedResult = if (calcResult % 1.0 == 0.0) {
                                    calcResult.toLong().toString()
                                } else {
                                    calcResult.toString()
                                }

                                Log.d("MeshAI", "[WORKER] Result: $formattedResult")
                                resultStatus = "COMPLETED"
                                resultData = formattedResult
                                withContext(Dispatchers.Main) {
                                    _uiState.value = _uiState.value.copy(tasksCompleted = _uiState.value.tasksCompleted + 1)
                                }
                            } catch (e: Exception) {
                                Log.e("MeshAI", "[WORKER] Calculate error: ${e.message}")
                                error = e.message ?: "Calculation error"
                                withContext(Dispatchers.Main) {
                                    _uiState.value = _uiState.value.copy(tasksFailed = _uiState.value.tasksFailed + 1)
                                }
                            }
                        } else if (task.task_type == "LLM_GENERATE") {
                            Log.d("MeshAI", "[WORKER] Executing LLM_GENERATE")
                            
                            val deviceInfoProvider = DeviceInfoProvider(getApplication())
                            val llmEngine = deviceInfoProvider.llmEngine
                            if (!llmEngine.isAvailable) {
                                error = "Local LLM model unavailable or not initialized"
                                Log.w("MeshAI", "[WORKER] $error")
                                withContext(Dispatchers.Main) {
                                    _uiState.value = _uiState.value.copy(tasksFailed = _uiState.value.tasksFailed + 1)
                                }
                            } else {
                                val promptElement = task.payload["prompt"]
                                val promptStr = if (promptElement != null && promptElement.toString().isNotBlank()) promptElement.jsonPrimitive.content else null
                                val maxTokensElement = task.payload["max_tokens"]
                                val maxTokens = if (maxTokensElement != null) maxTokensElement.jsonPrimitive.content.toIntOrNull() ?: 512 else 512
                                
                                if (promptStr == null || promptStr.isBlank()) {
                                    error = "Missing or empty prompt"
                                    withContext(Dispatchers.Main) {
                                        _uiState.value = _uiState.value.copy(tasksFailed = _uiState.value.tasksFailed + 1)
                                    }
                                } else {
                                    // Memory Safety check
                                    val memInfo = android.app.ActivityManager.MemoryInfo()
                                    val am = getApplication<Application>().getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
                                    am.getMemoryInfo(memInfo)
                                    val availRamMb = memInfo.availMem / (1024 * 1024)
                                    val requiredRam = deviceInfoProvider.modelRegistry.currentModel?.requiredRamMb ?: 1500L
                                    
                                    if (availRamMb < (requiredRam * 0.8)) { // Buffer safety
                                        error = "Insufficient memory for local LLM inference (Available: $availRamMb MB)"
                                        Log.e("MeshAI", "[WORKER] $error")
                                        withContext(Dispatchers.Main) {
                                            _uiState.value = _uiState.value.copy(tasksFailed = _uiState.value.tasksFailed + 1)
                                        }
                                    } else {
                                        try {
                                            val genResult = llmEngine.generate(
                                                promptStr, 
                                                com.meshai.worker.ai.GenerationConfig(maxTokens = maxTokens)
                                            )
                                            if (genResult.error != null) {
                                                error = genResult.error
                                                withContext(Dispatchers.Main) {
                                                    _uiState.value = _uiState.value.copy(tasksFailed = _uiState.value.tasksFailed + 1)
                                                }
                                            } else {
                                                resultStatus = "COMPLETED"
                                                resultData = genResult.text
                                                withContext(Dispatchers.Main) {
                                                    _uiState.value = _uiState.value.copy(tasksCompleted = _uiState.value.tasksCompleted + 1)
                                                }
                                            }
                                        } catch(e: Exception) {
                                            error = "LLM Engine Crash: ${e.message}"
                                            withContext(Dispatchers.Main) {
                                                _uiState.value = _uiState.value.copy(tasksFailed = _uiState.value.tasksFailed + 1)
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            error = "Unknown task type"
                            withContext(Dispatchers.Main) {
                                _uiState.value = _uiState.value.copy(tasksFailed = _uiState.value.tasksFailed + 1)
                            }
                        }

                        repository.submitTaskResult(
                            baseUrl,
                            task.task_id,
                            com.meshai.worker.model.TaskResultRequest(nodeId, resultStatus, resultData, error)
                        )

                        withContext(Dispatchers.Main) {
                            _uiState.value = _uiState.value.copy(currentTask = "NONE")
                        }
                    }
                }
                delay(3000) // Poll every 3 seconds
            }
        }
    }

    private fun stopTaskPolling() {
        taskPollingJob?.cancel()
        taskPollingJob = null
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
        stopTaskPolling()
        _uiState.value = _uiState.value.copy(
            workerState = WorkerState.DISCONNECTED,
            errorMessage = "Disconnected by user"
        )
    }

    override fun onCleared() {
        super.onCleared()
        stopHeartbeat()
        stopReconnection()
        stopTaskPolling()
    }
}

data class WorkerUiState(
    val orchestratorUrl: String = "",
    val workerState: WorkerState = WorkerState.DISCONNECTED,
    val nodeInfo: NodeInfo? = null,
    val lastHeartbeat: String = "Never",
    val errorMessage: String = "",
    val currentTask: String = "NONE",
    val tasksCompleted: Int = 0,
    val tasksFailed: Int = 0
)
