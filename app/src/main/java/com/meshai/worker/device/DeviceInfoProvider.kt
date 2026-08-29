package com.meshai.worker.device

import android.app.ActivityManager
import android.content.Context
import android.os.BatteryManager
import android.os.Build
import com.meshai.worker.model.NodeInfo
import java.util.UUID

class DeviceInfoProvider(private val context: Context) {
    private val sharedPreferences = context.getSharedPreferences("meshai_worker_prefs", Context.MODE_PRIVATE)

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

        val cpuCores = Runtime.getRuntime().availableProcessors()

        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val batteryPercent = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)

        return NodeInfo(
            nodeId = getOrCreateNodeId(),
            deviceName = "${Build.MANUFACTURER} ${Build.MODEL}",
            operatingSystem = "Android ${Build.VERSION.RELEASE}",
            ramMb = ramMb,
            cpuCores = cpuCores,
            batteryPercent = batteryPercent,
            capabilities = listOf("worker")
        )
    }

    fun getBatteryPercent(): Int {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        return batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }
}
