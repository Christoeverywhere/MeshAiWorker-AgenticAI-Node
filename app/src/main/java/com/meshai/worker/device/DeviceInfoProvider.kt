package com.meshai.worker.device

import android.app.ActivityManager
import android.content.Context
import android.os.BatteryManager
import android.os.Build
import com.meshai.worker.model.NodeInfo
import java.util.UUID

import com.meshai.worker.ai.HardwareProfile
import com.meshai.worker.ai.LocalLLMEngine
import com.meshai.worker.ai.ModelRegistry

import com.meshai.worker.ai.AndroidModelRegistry
import com.meshai.worker.ai.MediaPipeLLMEngine

class DeviceInfoProvider(private val context: Context) {
    private val sharedPreferences = context.getSharedPreferences("meshai_worker_prefs", Context.MODE_PRIVATE)

    val modelRegistry = AndroidModelRegistry(context)
    val llmEngine = MediaPipeLLMEngine(context, modelRegistry.getModelFile().absolutePath)
    
    init {
        // Attempt to initialize if the model is already present at startup
        if (modelRegistry.isModelInstalled) {
            llmEngine.initialize()
        }
    }

    fun getOrCreateNodeId(): String {
        var nodeId = sharedPreferences.getString("node_id", null)
        if (nodeId == null) {
            nodeId = "phone_${UUID.randomUUID().toString().substring(0, 8)}"
            sharedPreferences.edit().putString("node_id", nodeId).apply()
        }
        return nodeId
    }

    fun getDeviceInfo(): NodeInfo {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        val ramMb = memoryInfo.totalMem / (1024 * 1024)
        val availableRamMb = memoryInfo.availMem / (1024 * 1024)

        val cpuCores = Runtime.getRuntime().availableProcessors()
        val cpuArch = System.getProperty("os.arch") ?: "unknown"

        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val batteryPercent = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        
        val hwProfile = HardwareProfile(
            totalRamMb = ramMb,
            availableRamMb = availableRamMb,
            cpuCores = cpuCores,
            cpuArchitecture = cpuArch,
            resourceClass = HardwareProfile.classify(ramMb)
        )
        
        val baseCapabilities = mutableListOf(
            com.meshai.worker.model.Capabilities.WORKER, 
            com.meshai.worker.model.Capabilities.CALCULATE
        )
        
        val isLlmReady = llmEngine.isAvailable && modelRegistry.isModelInstalled && hwProfile.resourceClass != com.meshai.worker.ai.ResourceClass.ENTRY
        
        if (isLlmReady) {
            baseCapabilities.add(com.meshai.worker.model.Capabilities.LLM)
        }

        return NodeInfo(
            nodeId = getOrCreateNodeId(),
            deviceName = "${Build.MANUFACTURER} ${Build.MODEL}",
            operatingSystem = "Android ${Build.VERSION.RELEASE}",
            ramMb = ramMb,
            cpuCores = cpuCores,
            batteryPercent = batteryPercent,
            capabilities = baseCapabilities,
            availableRamMb = availableRamMb,
            cpuArchitecture = cpuArch,
            aiRuntime = llmEngine.aiRuntimeName,
            llmAvailable = isLlmReady,
            modelName = modelRegistry.currentModel?.name,
            modelSizeMb = modelRegistry.currentModel?.sizeMb,
            maxContextTokens = modelRegistry.currentModel?.maxContextTokens,
            maxOutputTokens = modelRegistry.currentModel?.maxOutputTokens,
            maxConcurrentInference = if (isLlmReady) 1 else 0
        )
    }

    fun getBatteryPercent(): Int {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        return batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }
}
