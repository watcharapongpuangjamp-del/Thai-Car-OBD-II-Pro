package com.example.hardware.obd

import android.util.Log
import com.example.model.AppOperationMode
import com.example.model.ConnectionState
import com.example.model.DataProvenance
import com.example.model.LiveSensorData
import com.example.model.TelemetryValue
import com.example.model.SensorQuality
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TelemetryPollingEngine(
    private val protocolEngine: ObdProtocolEngine,
    private val elmDriver: IElm327Driver
) {

    companion object {
        private const val TAG = "TelemetryPollingEngine"
    }

    private val pollingScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var pollingJob: Job? = null

    private val _telemetryFlow = MutableStateFlow(
        LiveSensorData.disconnected(AppOperationMode.REAL_HARDWARE, "รอเริ่มต้นระบบ Adaptive Telemetry Polling")
    )
    val telemetryFlow: Flow<LiveSensorData> = _telemetryFlow.asStateFlow()

    private var consecutiveErrors = 0
    private var totalRequests = 0
    private var successRequests = 0
    private var timeoutRequests = 0
    private var unsupportedRequests = 0
    private var lastSuccessfulPacketTimestamp = System.currentTimeMillis()

    // Cached sensor values across adaptive cycles
    private var cachedRpm: TelemetryValue<Int>? = null
    private var cachedSpeed: TelemetryValue<Int>? = null
    private var cachedThrottle: TelemetryValue<Int>? = null
    private var cachedCoolant: TelemetryValue<Int>? = null
    private var cachedMap: TelemetryValue<Int>? = null
    private var cachedBoost: TelemetryValue<Float>? = null
    private var cachedLoad: TelemetryValue<Int>? = null
    private var cachedIat: TelemetryValue<Int>? = null
    private var cachedFuelRate: TelemetryValue<Float>? = null
    private var cachedVoltage: TelemetryValue<Float>? = null

    private var lastFastPoll = 0L
    private var lastMedPoll = 0L
    private var lastSlowPoll = 0L

    fun startPolling() {
        stopPolling()
        consecutiveErrors = 0
        totalRequests = 0
        successRequests = 0
        timeoutRequests = 0
        unsupportedRequests = 0
        lastSuccessfulPacketTimestamp = System.currentTimeMillis()

        Log.i(TAG, "Starting Adaptive Real Hardware OBD Telemetry Polling (Time-based: FAST ~5Hz, MEDIUM ~2Hz, SLOW ~1Hz)...")

        pollingJob = pollingScope.launch {
            var windowStartTime = System.currentTimeMillis()
            var pidsQueriedInWindow = 0
            var currentPidsPerSec = 0
            var pollingIntervalMs = 50L

            while (isActive) {
                val cycleStartTime = System.currentTimeMillis()

                try {
                    val pollResult = pollAdaptiveSensorsTimeBased()
                    val cycleDuration = System.currentTimeMillis() - cycleStartTime
                    
                    val idleDuration = System.currentTimeMillis() - lastSuccessfulPacketTimestamp
                    if (idleDuration > 1500) {
                        Log.w(TAG, "USB buffer idle for ${idleDuration}ms (>1500ms threshold). Triggering DISCONNECTED_STALE.")
                        _telemetryFlow.value = LiveSensorData.disconnected(
                            AppOperationMode.REAL_HARDWARE,
                            "ข้อมูลขาดหาย: USB Buffer ว่างนานกว่า 1.5s (${idleDuration}ms)"
                        ).copy(connectionState = ConnectionState.DISCONNECTED_STALE)
                        break
                    }

                    consecutiveErrors = 0
                    lastSuccessfulPacketTimestamp = System.currentTimeMillis()

                    pidsQueriedInWindow += pollResult.queriedCount
                    if (System.currentTimeMillis() - windowStartTime >= 1000) {
                        currentPidsPerSec = pidsQueriedInWindow
                        pidsQueriedInWindow = 0
                        windowStartTime = System.currentTimeMillis()
                    }
                    
                    val activeProtocol = protocolEngine.getActiveProtocol()
                    val isConnected = activeProtocol.id != "UNKNOWN"
                    val connectionState = if (isConnected) ConnectionState.CONNECTED else ConnectionState.ECU_NOT_RESPONDING

                    _telemetryFlow.value = pollResult.sensorData.copy(
                        isConnected = isConnected,
                        connectionState = connectionState,
                        pidPerSec = currentPidsPerSec,
                        latencyMs = cycleDuration.toInt(),
                        mode = AppOperationMode.REAL_HARDWARE,
                        statusMessage = if (isConnected) "กำลังอ่านข้อมูลสด [Req: $totalRequests, Success: $successRequests, Timeout: $timeoutRequests]" else "รอการเชื่อมต่อ ECU"
                    )

                    delay(pollingIntervalMs)

                } catch (e: Exception) {
                    consecutiveErrors++
                    timeoutRequests++
                    Log.w(TAG, "Telemetry adaptive polling error ($consecutiveErrors/5): ${e.message}")
                    
                    val idleDuration = System.currentTimeMillis() - lastSuccessfulPacketTimestamp
                    if (consecutiveErrors >= 5 || idleDuration > 2000) {
                        _telemetryFlow.value = LiveSensorData.disconnected(
                            AppOperationMode.REAL_HARDWARE,
                            "ขาดการติดต่อ / USB Buffer ว่างนานเกินกำหนด: ${e.localizedMessage}"
                        ).copy(connectionState = ConnectionState.DISCONNECTED_STALE)
                        break
                    }
                    delay(300)
                }
            }
        }
    }

    private data class AdaptivePollResult(
        val sensorData: LiveSensorData,
        val queriedCount: Int
    )

    private suspend fun pollAdaptiveSensorsTimeBased(): AdaptivePollResult = withContext(Dispatchers.IO) {
        var queriedCount = 0
        val now = System.currentTimeMillis()

        // 1. FAST PIDs (~5Hz -> every 200ms)
        if (now - lastFastPoll >= 200) {
            lastFastPoll = now
            
            totalRequests++
            queriedCount++
            val rpmFrame = protocolEngine.readPid(0x0C)
            if (rpmFrame.isSuccess) {
                successRequests++
                val value = PidDecoder.decodeRpm(rpmFrame.getOrThrow().data)
                cachedRpm = TelemetryValue(value, "RPM", DataProvenance.REAL_HARDWARE, SensorQuality.GOOD)
            } else {
                timeoutRequests++
            }

            totalRequests++
            queriedCount++
            val speedFrame = protocolEngine.readPid(0x0D)
            if (speedFrame.isSuccess) {
                successRequests++
                val value = PidDecoder.decodeSpeed(speedFrame.getOrThrow().data)
                cachedSpeed = TelemetryValue(value, "km/h", DataProvenance.REAL_HARDWARE, SensorQuality.GOOD)
            } else {
                timeoutRequests++
            }
        }

        // 2. MEDIUM PIDs (~2Hz -> every 500ms)
        if (now - lastMedPoll >= 500) {
            lastMedPoll = now
            
            totalRequests++
            queriedCount++
            val ectFrame = protocolEngine.readPid(0x05)
            if (ectFrame.isSuccess) {
                successRequests++
                val value = PidDecoder.decodeCoolantTemp(ectFrame.getOrThrow().data)
                cachedCoolant = TelemetryValue(value, "°C", DataProvenance.REAL_HARDWARE, SensorQuality.GOOD)
            } else {
                timeoutRequests++
            }

            totalRequests++
            queriedCount++
            val throttleFrame = protocolEngine.readPid(0x11)
            if (throttleFrame.isSuccess) {
                successRequests++
                val value = PidDecoder.decodeThrottlePosition(throttleFrame.getOrThrow().data)
                cachedThrottle = TelemetryValue(value, "%", DataProvenance.REAL_HARDWARE, SensorQuality.GOOD)
            } else {
                timeoutRequests++
            }

            totalRequests++
            queriedCount++
            val loadFrame = protocolEngine.readPid(0x04)
            if (loadFrame.isSuccess) {
                successRequests++
                val value = PidDecoder.decodeEngineLoad(loadFrame.getOrThrow().data)
                cachedLoad = TelemetryValue(value, "%", DataProvenance.REAL_HARDWARE, SensorQuality.GOOD)
            } else {
                timeoutRequests++
            }
        }

        // 3. SLOW PIDs (~1Hz -> every 1000ms)
        if (now - lastSlowPoll >= 1000) {
            lastSlowPoll = now
            
            totalRequests++
            queriedCount++
            val iatFrame = protocolEngine.readPid(0x0F)
            if (iatFrame.isSuccess) {
                successRequests++
                val value = PidDecoder.decodeIntakeAirTemp(iatFrame.getOrThrow().data)
                cachedIat = TelemetryValue(value, "°C", DataProvenance.REAL_HARDWARE, SensorQuality.GOOD)
            } else {
                timeoutRequests++
            }

            totalRequests++
            queriedCount++
            val mafFrame = protocolEngine.readPid(0x10)
            if (mafFrame.isSuccess) {
                successRequests++
                val mafGps = PidDecoder.decodeMafAirFlow(mafFrame.getOrThrow().data)
                val speed = cachedSpeed?.value ?: 0
                val fuel = PidDecoder.estimateFuelRateLph(mafGps, speed)
                cachedFuelRate = TelemetryValue(fuel, "L/h", DataProvenance.REAL_HARDWARE, SensorQuality.GOOD)
            } else {
                timeoutRequests++
            }

            totalRequests++
            queriedCount++
            val mapFrame = protocolEngine.readPid(0x0B)
            if (mapFrame.isSuccess) {
                successRequests++
                val data = mapFrame.getOrThrow().data
                val mapVal = PidDecoder.decodeMapPressureKpa(data)
                val boostVal = PidDecoder.decodeBoostPressureBar(data)
                cachedMap = TelemetryValue(mapVal, "kPa", DataProvenance.REAL_HARDWARE, SensorQuality.GOOD)
                cachedBoost = TelemetryValue(boostVal, "Bar", DataProvenance.REAL_HARDWARE, SensorQuality.GOOD)
            } else {
                timeoutRequests++
            }

            try {
                val atrvRaw = elmDriver.sendRawCommand("ATRV", 400)
                val volt = PidDecoder.parseAtRvVoltage(atrvRaw)
                if (volt != null && volt > 0f) {
                    cachedVoltage = TelemetryValue(volt, "V", DataProvenance.REAL_HARDWARE, SensorQuality.GOOD)
                }
            } catch (_: Exception) {}
        }

        val sensorData = LiveSensorData(
            isConnected = true,
            connectionState = ConnectionState.CONNECTED,
            rpmData = cachedRpm ?: TelemetryValue(null, "RPM", DataProvenance.REAL_HARDWARE),
            speedData = cachedSpeed ?: TelemetryValue(null, "km/h", DataProvenance.REAL_HARDWARE),
            coolantData = cachedCoolant ?: TelemetryValue(null, "°C", DataProvenance.REAL_HARDWARE),
            voltageData = cachedVoltage ?: TelemetryValue(null, "V", DataProvenance.REAL_HARDWARE),
            mapData = cachedMap ?: TelemetryValue(null, "kPa", DataProvenance.REAL_HARDWARE),
            boostData = cachedBoost ?: TelemetryValue(null, "Bar", DataProvenance.REAL_HARDWARE),
            fuelRateData = cachedFuelRate ?: TelemetryValue(null, "L/h", DataProvenance.REAL_HARDWARE),
            throttleData = cachedThrottle ?: TelemetryValue(null, "%", DataProvenance.REAL_HARDWARE),
            intakeTempData = cachedIat ?: TelemetryValue(null, "°C", DataProvenance.REAL_HARDWARE),
            engineLoadData = cachedLoad ?: TelemetryValue(null, "%", DataProvenance.REAL_HARDWARE),
            pidPerSec = 0,
            latencyMs = 0,
            mode = AppOperationMode.REAL_HARDWARE,
            statusMessage = "Adaptive Polling Active (FAST/MED/SLOW Time-Based)"
        )

        return@withContext AdaptivePollResult(sensorData, queriedCount)
    }

    fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    fun setPollingInterval(intervalMs: Long) {}
}
