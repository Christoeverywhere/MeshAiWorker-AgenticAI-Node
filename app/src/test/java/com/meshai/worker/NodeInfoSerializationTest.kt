package com.meshai.worker

import com.meshai.worker.model.NodeInfo
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class NodeInfoSerializationTest {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Test
    fun testDefaultCapabilitiesIsWorker() {
        val nodeInfo = NodeInfo(
            nodeId = "phone_5a06e8e5",
            deviceName = "samsung SM-M055F",
            operatingSystem = "Android 16",
            ramMb = 3653,
            cpuCores = 8,
            batteryPercent = 30
        )
        assertEquals(listOf("worker"), nodeInfo.capabilities)
        assertEquals("android", nodeInfo.deviceType)
        assertEquals(8080, nodeInfo.port)
    }

    @Test
    fun testSerializationContainsWorkerCapability() {
        val nodeInfo = NodeInfo(
            nodeId = "phone_5a06e8e5",
            deviceName = "samsung SM-M055F",
            operatingSystem = "Android 16",
            ramMb = 3653,
            cpuCores = 8,
            batteryPercent = 30
        )

        val jsonString = json.encodeToString(nodeInfo)
        val jsonElement = json.parseToJsonElement(jsonString).jsonObject

        // Verify capabilities
        assertNotNull(jsonElement["capabilities"])
        val capabilitiesArray = jsonElement["capabilities"]!!.jsonArray
        assertEquals(1, capabilitiesArray.size)
        assertEquals("worker", capabilitiesArray[0].jsonPrimitive.content)

        // Verify all fields required by FastAPI orchestrator
        assertEquals("phone_5a06e8e5", jsonElement["node_id"]!!.jsonPrimitive.content)
        assertEquals("samsung SM-M055F", jsonElement["device_name"]!!.jsonPrimitive.content)
        assertEquals("android", jsonElement["device_type"]!!.jsonPrimitive.content)
        assertEquals("Android 16", jsonElement["operating_system"]!!.jsonPrimitive.content)
        assertEquals(3653L, jsonElement["ram_mb"]!!.jsonPrimitive.content.toLong())
        assertEquals(8, jsonElement["cpu_cores"]!!.jsonPrimitive.content.toInt())
        assertEquals(30, jsonElement["battery_percent"]!!.jsonPrimitive.content.toInt())
        assertEquals(8080, jsonElement["port"]!!.jsonPrimitive.content.toInt())
    }

    @Test
    fun testSerializationWithDefaultJsonIncludesEncodeDefault() {
        val nodeInfo = NodeInfo(
            nodeId = "phone_3d8135f5",
            deviceName = "samsung SM-M176B",
            operatingSystem = "Android 16",
            ramMb = 3450,
            cpuCores = 8,
            batteryPercent = 85
        )

        val jsonString = Json.Default.encodeToString(nodeInfo)
        val jsonElement = Json.Default.parseToJsonElement(jsonString).jsonObject

        // Even with Json.Default, @EncodeDefault ensures capabilities is present
        assertNotNull(jsonElement["capabilities"])
        val capabilitiesArray = jsonElement["capabilities"]!!.jsonArray
        assertEquals(1, capabilitiesArray.size)
        assertEquals("worker", capabilitiesArray[0].jsonPrimitive.content)
    }

    @Test
    fun testDeserializationDefaultsToWorker() {
        val rawJson = """
            {
                "node_id": "phone_3d8135f5",
                "device_name": "samsung SM-M176B",
                "device_type": "android",
                "operating_system": "Android 16",
                "ram_mb": 3450,
                "cpu_cores": 8,
                "battery_percent": 85,
                "port": 8080
            }
        """.trimIndent()

        val decoded = json.decodeFromString<NodeInfo>(rawJson)
        assertEquals(listOf("worker"), decoded.capabilities)
    }
}
