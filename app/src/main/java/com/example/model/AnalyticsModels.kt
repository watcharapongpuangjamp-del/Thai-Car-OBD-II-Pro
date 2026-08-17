package com.example.model

enum class TripStatus {
    RECORDING,
    PAUSED,
    COMPLETED
}

data class TripSummary(
    val tripId: String,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val durationSeconds: Long,
    val distanceKm: Float,
    val avgSpeedKmh: Float,
    val maxSpeedKmh: Int,
    val maxRpm: Int,
    val avgFuelConsumptionL100km: Float,
    val totalFuelUsedLiters: Float,
    val maxCoolantTempC: Int,
    val idleTimeSeconds: Long,
    val mode: AppOperationMode,
    val dataProvenance: DataProvenance = if (mode == AppOperationMode.REAL_HARDWARE) DataProvenance.REAL_HARDWARE else DataProvenance.SIMULATOR
)

data class DataExportPayload(
    val exportTimestamp: Long = System.currentTimeMillis(),
    val appVersion: String,
    val operationMode: AppOperationMode,
    val vehicleVin: String?,
    val dtcs: List<DtcCode>,
    val latestTelemetry: LiveSensorData?,
    val tripSummaries: List<TripSummary>,
    val maintenanceItems: List<PredictiveMaintenanceItem>,
    val checksumSha256: String
)
