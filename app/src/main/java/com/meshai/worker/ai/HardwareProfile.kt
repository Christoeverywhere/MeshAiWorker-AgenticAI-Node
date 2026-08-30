package com.meshai.worker.ai

enum class ResourceClass {
    ENTRY,      // < 4GB
    STANDARD,   // 4GB - 6GB
    HIGH        // >= 6GB
}

data class HardwareProfile(
    val totalRamMb: Long,
    val availableRamMb: Long,
    val cpuCores: Int,
    val cpuArchitecture: String,
    val resourceClass: ResourceClass
) {
    companion object {
        fun classify(totalRamMb: Long): ResourceClass {
            return when {
                totalRamMb < 3800 -> ResourceClass.ENTRY // Using 3800 to safely catch 4GB devices which report slightly less
                totalRamMb < 5800 -> ResourceClass.STANDARD
                else -> ResourceClass.HIGH
            }
        }
    }
}
