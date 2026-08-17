package com.example.analytics

import com.example.model.AppOperationMode
import com.example.model.LiveSensorData
import com.example.model.TripStatus
import com.example.model.TripSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class TripAnalyticsEngine {

    private val _activeTripStatus = MutableStateFlow(TripStatus.PAUSED)
    val activeTripStatus: Flow<TripStatus> = _activeTripStatus.asStateFlow()

    private var currentTripId: String = ""
    private var startTimeMs: Long = 0
    private var lastSampleTimeMs: Long = 0

    // Accumulators
    private var totalDistanceMeters: Double = 0.0
    private var totalFuelUsedMilliliters: Double = 0.0
    private var speedSamplesCount: Long = 0
    private var speedSum: Double = 0.0
    private var maxSpeed: Int = 0
    private var maxRpm: Int = 0
    private var maxCoolant: Int = 0
    private var idleTimeSeconds: Long = 0
    private var currentMode: AppOperationMode = AppOperationMode.REAL_HARDWARE

    private val _currentTripSummary = MutableStateFlow<TripSummary?>(null)
    val currentTripSummary: Flow<TripSummary?> = _currentTripSummary.asStateFlow()

    fun startOrResumeTrip(mode: AppOperationMode) {
        if (_activeTripStatus.value == TripStatus.RECORDING) return

        if (_activeTripStatus.value == TripStatus.PAUSED && currentTripId.isEmpty()) {
            // New trip
            currentTripId = "TRIP-${UUID.randomUUID().toString().take(8).uppercase()}"
            startTimeMs = System.currentTimeMillis()
            lastSampleTimeMs = startTimeMs
            totalDistanceMeters = 0.0
            totalFuelUsedMilliliters = 0.0
            speedSamplesCount = 0
            speedSum = 0.0
            maxSpeed = 0
            maxRpm = 0
            maxCoolant = 0
            idleTimeSeconds = 0
            currentMode = mode
        } else {
            lastSampleTimeMs = System.currentTimeMillis()
        }

        _activeTripStatus.value = TripStatus.RECORDING
    }

    fun pauseTrip() {
        if (_activeTripStatus.value == TripStatus.RECORDING) {
            _activeTripStatus.value = TripStatus.PAUSED
        }
    }

    fun resetAndEndTrip(): TripSummary? {
        val finalSummary = calculateCurrentSummary()
        _activeTripStatus.value = TripStatus.COMPLETED
        currentTripId = ""
        _activeTripStatus.value = TripStatus.PAUSED
        _currentTripSummary.value = null
        return finalSummary
    }

    fun ingestTelemetrySample(sample: LiveSensorData) {
        if (_activeTripStatus.value != TripStatus.RECORDING) return
        if (!sample.isConnected) return

        val now = System.currentTimeMillis()
        val deltaSeconds = if (lastSampleTimeMs > 0) ((now - lastSampleTimeMs) / 1000.0).coerceAtLeast(0.0) else 0.0
        lastSampleTimeMs = now

        val speed = sample.speedKmh ?: 0
        val rpm = sample.rpm ?: 0
        val ect = sample.coolantTempC ?: 0
        val fuelRateLph = sample.fuelRateLph ?: 0f

        // 1. Distance accumulation: speed (km/h) * (1000/3600) m/s * deltaSeconds
        val distanceDeltaMeters = (speed * (1000.0 / 3600.0)) * deltaSeconds
        totalDistanceMeters += distanceDeltaMeters

        // 2. Fuel consumption accumulation: L/h -> mL/s * deltaSeconds
        val fuelMlDelta = (fuelRateLph / 3.6) * deltaSeconds
        totalFuelUsedMilliliters += fuelMlDelta

        // 3. Speed metrics
        speedSamplesCount++
        speedSum += speed
        if (speed > maxSpeed) maxSpeed = speed

        // 4. Engine metrics
        if (rpm > maxRpm) maxRpm = rpm
        if (ect > maxCoolant) maxCoolant = ect

        // 5. Idle time calculation (speed == 0 and rpm > 400)
        if (speed == 0 && rpm > 400) {
            idleTimeSeconds += deltaSeconds.toLong()
        }

        currentMode = sample.mode
        _currentTripSummary.value = calculateCurrentSummary()
    }

    private fun calculateCurrentSummary(): TripSummary? {
        if (currentTripId.isEmpty()) return null

        val now = System.currentTimeMillis()
        val durationSec = ((now - startTimeMs) / 1000).coerceAtLeast(1)
        val distanceKm = (totalDistanceMeters / 1000.0).toFloat()
        val avgSpeed = if (speedSamplesCount > 0) (speedSum / speedSamplesCount).toFloat() else 0f
        val fuelUsedLiters = (totalFuelUsedMilliliters / 1000.0).toFloat()

        // L / 100km = (FuelUsed / DistanceKm) * 100
        val avgL100km = if (distanceKm > 0.1f) {
            ((fuelUsedLiters / distanceKm) * 100f * 10).toInt() / 10f
        } else {
            0.0f
        }

        return TripSummary(
            tripId = currentTripId,
            startTimeMs = startTimeMs,
            endTimeMs = now,
            durationSeconds = durationSec,
            distanceKm = (distanceKm * 100).toInt() / 100f,
            avgSpeedKmh = (avgSpeed * 10).toInt() / 10f,
            maxSpeedKmh = maxSpeed,
            maxRpm = maxRpm,
            avgFuelConsumptionL100km = avgL100km,
            totalFuelUsedLiters = (fuelUsedLiters * 100).toInt() / 100f,
            maxCoolantTempC = maxCoolant,
            idleTimeSeconds = idleTimeSeconds,
            mode = currentMode
        )
    }
}
