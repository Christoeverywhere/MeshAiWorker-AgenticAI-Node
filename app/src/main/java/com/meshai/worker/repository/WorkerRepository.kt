package com.meshai.worker.repository

import com.meshai.worker.device.DeviceInfoProvider
import com.meshai.worker.model.NodeInfo
import com.meshai.worker.network.NetworkManager

class WorkerRepository(
    private val deviceInfoProvider: DeviceInfoProvider,
    private val networkManager: NetworkManager
) {
    fun getDeviceInfo(): NodeInfo = deviceInfoProvider.getDeviceInfo()
    
    fun getBatteryPercent(): Int = deviceInfoProvider.getBatteryPercent()

    suspend fun register(baseUrl: String, nodeInfo: NodeInfo): Result<Unit> {
        return networkManager.registerNode(baseUrl, nodeInfo)
    }

    suspend fun heartbeat(baseUrl: String, nodeId: String, batteryPercent: Int): Result<Unit> {
        return networkManager.sendHeartbeat(baseUrl, nodeId, batteryPercent)
    }

    suspend fun pollTasks(baseUrl: String, nodeId: String): Result<com.meshai.worker.model.TaskResponse?> {
        return networkManager.pollTasks(baseUrl, nodeId)
    }

    suspend fun submitTaskResult(baseUrl: String, taskId: String, request: com.meshai.worker.model.TaskResultRequest): Result<Unit> {
        return networkManager.submitTaskResult(baseUrl, taskId, request)
    }
}
