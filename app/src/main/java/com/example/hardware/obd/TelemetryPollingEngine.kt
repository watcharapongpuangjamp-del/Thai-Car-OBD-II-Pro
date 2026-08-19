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
    private var cachedRpm: Int? = null
    private var cachedSpeed: Int? = null
    private var cachedThrottle: Int? = null
    private var cachedCoolant: Int? = null
    private var cachedBoost: Float? = null
    private var cachedLoad: Int? = null
    private var cachedIat: Int? = null
    private var cachedFuelRate: Float? = null
    private var cachedVoltage: Float? = null

    private var cycleCounter = 0

    fun startPolling() {
        stopPolling()
        consecutiveErrors = 0
        lastSuccessfulPacketTimestamp = System.currentTimeMillis()
        cycleCounter = 0
        Log.i(TAG, "Starting Adaptive Real Hardware OBD Telemetry Polling (FAST ~5Hz, MEDIUM ~2Hz, SLOW ~1Hz)...")

        pollingJob = pollingScope.launch {
            var windowStartTime = System.currentTimeMillis()
            var pidsQueriedInWindow = 0
            var currentPidsPerSec = 0
            var pollingIntervalMs = 200L

            while (isActive) {
                val cycleStartTime = System.currentTimeMillis()
                cycleCounter++

                try {
                    val pollResult = pollAdaptiveSensorsBySchedule(cycleCounter)
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

    private suspend fun pollAdaptiveSensorsBySchedule(cycle: Int): AdaptivePollResult = withContext(Dispatchers.IO) {
        var queriedCount = 0

        // 1. FAST PIDs (~5Hz / Every cycle): RPM (0x0C), Speed (0x0D)
        totalRequests++
        queriedCount++
        val rpmFrame = protocolEngine.readPid(0x0C)
        if (rpmFrame.isSuccess) {
            successRequests++
            cachedRpm = PidDecoder.decodeRpm(rpmFrame.getOrThrow().data)
        } else {
            timeoutRequests++
        }

        totalRequests++
        queriedCount++
        val speedFrame = protocolEngine.readPid(0x0D)
        if (speedFrame.isSuccess) {
            successRequests++
            cachedSpeed = PidDecoder.decodeSpeed(speedFrame.getOrThrow().data)
        } else {
            timeoutRequests++
        }

        // 2. MEDIUM PIDs (~2Hz / Every 2 cycles): Coolant (0x05), Throttle (0x11), Engine Load (0x04)
        if (cycle % 2 == 0) {
            totalRequests++
            queriedCount++
            val ectFrame = protocolEngine.readPid(0x05)
            if (ectFrame.isSuccess) {
                successRequests++
                cachedCoolant = PidDecoder.decodeCoolantTemp(ectFrame.getOrThrow().data)
            } else {
                timeoutRequests++
            }

            totalRequests++
            queriedCount++
            val throttleFrame = protocolEngine.readPid(0x11)
            if (throttleFrame.isSuccess) {
                successRequests++
                cachedThrottle = PidDecoder.decodeThrottlePosition(throttleFrame.getOrThrow().data)
            } else {
                timeoutRequests++
            }

            totalRequests++
            queriedCount++
            val loadFrame = protocolEngine.readPid(0x04)
            if (loadFrame.isSuccess) {
                successRequests++
                cachedLoad = PidDecoder.decodeEngineLoad(loadFrame.getOrThrow().data)
            } else {
                timeoutRequests++
            }
        }

        // 3. SLOW PIDs (~1Hz / Every 5 cycles): Intake Temp (0x0F), MAF (0x10), Boost/MAP (0x0B), Voltage (ATRV)
        if (cycle % 5 == 0) {
            totalRequests++
            queriedCount++
            val iatFrame = protocolEngine.readPid(0x0F)
            if (iatFrame.isSuccess) {
                successRequests++
                cachedIat = PidDecoder.decodeIntakeAirTemp(iatFrame.getOrThrow().data)
            } else {
                timeoutRequests++
            }

            totalRequests++
            queriedCount++
            val mafFrame = protocolEngine.readPid(0x10)
            if (mafFrame.isSuccess) {
                successRequests++
                val mafGps = PidDecoder.decodeMafAirFlow(mafFrame.getOrThrow().data)
                cachedFuelRate = PidDecoder.estimateFuelRateLph(mafGps, cachedSpeed ?: 0)
            } else {
                timeoutRequests++
            }

            totalRequests++
            queriedCount++
            val mapFrame = protocolEngine.readPid(0x0B)
            if (mapFrame.isSuccess) {
                successRequests++
                cachedBoost = PidDecoder.decodeBoostPressureBar(mapFrame.getOrThrow().data)
            } else {
                timeoutRequests++
            }

            try {
                val atrvRaw = elmDriver.sendRawCommand("ATRV", 400)
                val volt = PidDecoder.parseAtRvVoltage(atrvRaw)
                if (volt != null && volt > 0f) {
                    cachedVoltage = volt
                }
            } catch (_: Exception) {}
        }

        val sensorData = LiveSensorData(
            isConnected = true,
            connectionState = ConnectionState.CONNECTED,
            rpm = cachedRpm,
            speedKmh = cachedSpeed,
            coolantTempC = cachedCoolant,
            batteryVoltage = cachedVoltage,
            boostPressureBar = cachedBoost,
            fuelRateLph = cachedFuelRate,
            throttlePosPercent = cachedThrottle,
            intakeTempC = cachedIat,
            engineLoadPercent = cachedLoad,
            pidPerSec = 0,
            latencyMs = 0,
            mode = AppOperationMode.REAL_HARDWARE,
            statusMessage = "Adaptive Polling Active (FAST/MED/SLOW separated)"
        )

        return@withContext AdaptivePollResult(sensorData, queriedCount)
    }

    fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    fun setPollingInterval(intervalMs: Long) {}
}


