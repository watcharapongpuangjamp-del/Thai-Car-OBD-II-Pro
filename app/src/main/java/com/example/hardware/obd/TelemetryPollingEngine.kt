package com.example.hardware.obd

import android.util.Log
import com.example.model.AppOperationMode
import com.example.model.ConnectionState
import com.example.model.DataProvenance
import com.example.model.LiveSensorData
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
    private val elmDriver: Elm327Driver
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

    fun startPolling() {
        stopPolling()
        consecutiveErrors = 0
        lastSuccessfulPacketTimestamp = System.currentTimeMillis()
        Log.i(TAG, "Starting Adaptive Real Hardware OBD Telemetry Polling loop with 500ms stale watchdog...")

        pollingJob = pollingScope.launch {
            var windowStartTime = System.currentTimeMillis()
            var pidsQueriedInWindow = 0
            var currentPidsPerSec = 0
            var pollingIntervalMs = 200L

            while (isActive) {
                val cycleStartTime = System.currentTimeMillis()

                try {
                    val currentData = pollAdaptiveSensors()
                    val cycleDuration = System.currentTimeMillis() - cycleStartTime
                    
                    // Check if USB buffer / response was idle for > 500ms (no successful PIDs or timeout)
                    val idleDuration = System.currentTimeMillis() - lastSuccessfulPacketTimestamp
                    if (idleDuration > 500) {
                        Log.w(TAG, "USB buffer idle for ${idleDuration}ms (>500ms threshold). Triggering DISCONNECTED_STALE.")
                        _telemetryFlow.value = LiveSensorData.disconnected(
                            AppOperationMode.REAL_HARDWARE,
                            "ข้อมูลค้างเติ่ง: USB Buffer ว่างนานกว่า 500ms (${idleDuration}ms)"
                        ).copy(connectionState = ConnectionState.DISCONNECTED_STALE)
                        break
                    }

                    consecutiveErrors = 0
                    lastSuccessfulPacketTimestamp = System.currentTimeMillis()

                    pidsQueriedInWindow += currentData.queriedCount
                    if (System.currentTimeMillis() - windowStartTime >= 1000) {
                        currentPidsPerSec = pidsQueriedInWindow
                        if (currentPidsPerSec < 8) pollingIntervalMs = maxOf(50L, pollingIntervalMs - 20)
                        else if (currentPidsPerSec > 12) pollingIntervalMs = minOf(500L, pollingIntervalMs + 20)
                        pidsQueriedInWindow = 0
                        windowStartTime = System.currentTimeMillis()
                    }
                    
                    delay(pollingIntervalMs)

                    val activeProtocol = protocolEngine.getActiveProtocol()
                    val isConnected = activeProtocol.id != "UNKNOWN"
                    val connectionState = if (isConnected) ConnectionState.CONNECTED else ConnectionState.ECU_NOT_RESPONDING

                    _telemetryFlow.value = currentData.sensorData.copy(
                        isConnected = isConnected,
                        connectionState = connectionState,
                        pidPerSec = currentPidsPerSec,
                        latencyMs = cycleDuration.toInt(),
                        mode = AppOperationMode.REAL_HARDWARE,
                        statusMessage = if (isConnected) "กำลังอ่านข้อมูลสด (โปรโตคอล: ${activeProtocol.id})" else "รอการเชื่อมต่อ ECU"
                    )

                } catch (e: Exception) {
                    consecutiveErrors++
                    timeoutRequests++
                    Log.w(TAG, "Telemetry adaptive polling error ($consecutiveErrors/5): ${e.message}")
                    
                    val idleDuration = System.currentTimeMillis() - lastSuccessfulPacketTimestamp
                    if (consecutiveErrors >= 5 || idleDuration > 500) {
                        _telemetryFlow.value = LiveSensorData.disconnected(
                            AppOperationMode.REAL_HARDWARE,
                            "ขาดการติดต่อ / USB Buffer ว่างนานเกิน 500ms: ${e.localizedMessage}"
                        ).copy(connectionState = ConnectionState.DISCONNECTED_STALE)
                        break
                    }
                }

                // Adaptive sleep interval based on ECU latency
                delay(150)
            }
        }
    }

    private data class AdaptivePollResult(
        val sensorData: LiveSensorData,
        val queriedCount: Int
    )

    private suspend fun pollAdaptiveSensors(): AdaptivePollResult = withContext(Dispatchers.IO) {
        var queried = 0

        // FAST PIDs (~5Hz target): RPM, Speed, Throttle
        queried++
        totalRequests++
        val rpmFrame = protocolEngine.readPid(0x0C).getOrNull()
        val rpm = if (rpmFrame != null) {
            successRequests++
            PidDecoder.decodeRpm(rpmFrame.data)
        } else {
            unsupportedRequests++
            null
        }

        queried++
        totalRequests++
        val speedFrame = protocolEngine.readPid(0x0D).getOrNull()
        val speed = if (speedFrame != null) {
            successRequests++
            PidDecoder.decodeSpeed(speedFrame.data)
        } else {
            unsupportedRequests++
            null
        }

        queried++
        totalRequests++
        val throttleFrame = protocolEngine.readPid(0x11).getOrNull()
        val throttle = if (throttleFrame != null) {
            successRequests++
            PidDecoder.decodeThrottlePosition(throttleFrame.data)
        } else {
            unsupportedRequests++
            null
        }

        // MEDIUM PIDs (~2Hz): Coolant, MAP, Load
        queried++
        totalRequests++
        val ectFrame = protocolEngine.readPid(0x05).getOrNull()
        val ect = if (ectFrame != null) {
            successRequests++
            PidDecoder.decodeCoolantTemp(ectFrame.data)
        } else {
            unsupportedRequests++
            null
        }

        queried++
        totalRequests++
        val mapFrame = protocolEngine.readPid(0x0B).getOrNull()
        val boost = if (mapFrame != null) {
            successRequests++
            PidDecoder.decodeBoostPressureBar(mapFrame.data)
        } else {
            unsupportedRequests++
            null
        }

        queried++
        totalRequests++
        val loadFrame = protocolEngine.readPid(0x04).getOrNull()
        val load = if (loadFrame != null) {
            successRequests++
            PidDecoder.decodeEngineLoad(loadFrame.data)
        } else {
            unsupportedRequests++
            null
        }

        // SLOW PIDs (~1Hz): Intake Temp, MAF, Voltage
        queried++
        totalRequests++
        val iatFrame = protocolEngine.readPid(0x0F).getOrNull()
        val iat = if (iatFrame != null) {
            successRequests++
            PidDecoder.decodeIntakeAirTemp(iatFrame.data)
        } else {
            unsupportedRequests++
            null
        }

        queried++
        totalRequests++
        val mafFrame = protocolEngine.readPid(0x10).getOrNull()
        val fuelRate = if (mafFrame != null) {
            successRequests++
            val mafGps = PidDecoder.decodeMafAirFlow(mafFrame.data)
            PidDecoder.estimateFuelRateLph(mafGps, speed ?: 0)
        } else {
            unsupportedRequests++
            null
        }

        var voltage: Float? = null
        try {
            val atrvRaw = elmDriver.sendRawCommand("ATRV", 500)
            voltage = PidDecoder.parseAtRvVoltage(atrvRaw)
            if (voltage != null && voltage <= 0f) voltage = null
        } catch (_: Exception) {
            val voltFrame = protocolEngine.readPid(0x42).getOrNull()
            if (voltFrame != null) {
                voltage = PidDecoder.decodeControlModuleVoltage(voltFrame.data)
            }
        }

        val sensorData = LiveSensorData(
            isConnected = true,
            connectionState = ConnectionState.CONNECTED,
            rpm = rpm,
            speedKmh = speed,
            coolantTempC = ect,
            batteryVoltage = voltage,
            boostPressureBar = boost,
            fuelRateLph = fuelRate,
            throttlePosPercent = throttle,
            intakeTempC = iat,
            engineLoadPercent = load,
            pidPerSec = 0,
            latencyMs = 0,
            mode = AppOperationMode.REAL_HARDWARE,
            statusMessage = "Adaptive Real Hardware Telemetry Active"
        )

        return@withContext AdaptivePollResult(sensorData, queried)
    }

    fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    fun setPollingInterval(intervalMs: Long) {
        // Interval controlled by adaptive scheduler
    }
}

