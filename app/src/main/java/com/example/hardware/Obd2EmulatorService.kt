package com.example.hardware

import com.example.model.AppOperationMode
import com.example.model.ConnectionState
import com.example.model.DtcCode
import com.example.model.DtcSeverity
import com.example.model.DtcStatus
import com.example.model.LiveSensorData
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.random.Random

enum class SimulatorScenario(val labelTh: String) {
    IDLE("เดินเบา (Idle)"),
    CRUISE("ขับขี่ทั่วไป (City Cruise)"),
    HIGH_BOOST("เร่งแซง / เทอร์โบ (High Boost)"),
    OVERHEAT_WARNING("ความร้อนสูง (Overheat Test)"),
    SENSOR_FAULT("ระบบเซนเซอร์ขัดข้อง (Dtc Fault)")
}

class Obd2EmulatorService {

    private val _currentScenario = MutableStateFlow(SimulatorScenario.IDLE)
    val currentScenario = _currentScenario.asStateFlow()

    private val _simulatedTelemetry = MutableStateFlow(
        LiveSensorData.simulated(
            rpm = 750,
            speed = 0,
            coolant = 86,
            voltage = 14.1f,
            map = 101,
            boost = 0.02f,
            fuelRate = 1.1f,
            throttle = 12,
            intakeTemp = 32,
            load = 15
        )
    )
    val simulatedTelemetry: Flow<LiveSensorData> = _simulatedTelemetry

    fun setScenario(scenario: SimulatorScenario) {
        _currentScenario.value = scenario
        updateTelemetryForScenario(scenario)
    }

    private fun updateTelemetryForScenario(scenario: SimulatorScenario) {
        when (scenario) {
            SimulatorScenario.IDLE -> {
                _simulatedTelemetry.value = LiveSensorData.simulated(
                    rpm = 760 + Random.nextInt(-20, 20),
                    speed = 0,
                    coolant = 88,
                    voltage = 14.2f,
                    map = 101,
                    boost = 0.03f,
                    fuelRate = 1.1f,
                    throttle = 12,
                    intakeTemp = 35,
                    load = 16
                )
            }
            SimulatorScenario.CRUISE -> {
                _simulatedTelemetry.value = LiveSensorData.simulated(
                    rpm = 2150 + Random.nextInt(-50, 50),
                    speed = 85 + Random.nextInt(-3, 3),
                    coolant = 90,
                    voltage = 13.9f,
                    map = 140,
                    boost = 0.45f,
                    fuelRate = 6.4f,
                    throttle = 38,
                    intakeTemp = 38,
                    load = 42
                )
            }
            SimulatorScenario.HIGH_BOOST -> {
                _simulatedTelemetry.value = LiveSensorData.simulated(
                    rpm = 3800 + Random.nextInt(-100, 100),
                    speed = 124 + Random.nextInt(-5, 5),
                    coolant = 95,
                    voltage = 13.8f,
                    map = 280,
                    boost = 1.85f,
                    fuelRate = 14.2f,
                    throttle = 85,
                    intakeTemp = 48,
                    load = 92
                )
            }
            SimulatorScenario.OVERHEAT_WARNING -> {
                _simulatedTelemetry.value = LiveSensorData.simulated(
                    rpm = 2400 + Random.nextInt(-40, 40),
                    speed = 90,
                    coolant = 112, // Danger High
                    voltage = 13.5f,
                    map = 160,
                    boost = 0.60f,
                    fuelRate = 8.5f,
                    throttle = 45,
                    intakeTemp = 58,
                    load = 65
                )
            }
            SimulatorScenario.SENSOR_FAULT -> {
                _simulatedTelemetry.value = LiveSensorData.simulated(
                    rpm = 1850 + Random.nextInt(-30, 30),
                    speed = 65,
                    coolant = 92,
                    voltage = 14.0f,
                    map = 120,
                    boost = 0.20f,
                    fuelRate = 4.8f,
                    throttle = 25,
                    intakeTemp = 42,
                    load = 32
                )
            }
        }
    }

    suspend fun runSimulatorLoop() {
        while (true) {
            updateTelemetryForScenario(_currentScenario.value)
            delay(100) // 10Hz
        }
    }

    suspend fun performSimulatedDtcScan(): List<DtcCode> {
        delay(2000) // Simulate network/ECU wait
        if (_currentScenario.value == SimulatorScenario.SENSOR_FAULT) {
            return listOf(
                DtcCode("P0171", "ECM", "System Too Lean (Bank 1)", "ส่วนผสมเชื้อเพลิงบางเกินไป", DtcSeverity.WARNING, DtcStatus.CONFIRMED, AppOperationMode.SIMULATOR),
                DtcCode("P0087", "ECM", "Fuel Rail/System Pressure - Too Low", "แรงดันในรางหัวฉีดต่ำเกินไป", DtcSeverity.CRITICAL, DtcStatus.CONFIRMED, AppOperationMode.SIMULATOR)
            )
        }
        if (_currentScenario.value == SimulatorScenario.OVERHEAT_WARNING) {
             return listOf(
                DtcCode("P0217", "ECM", "Engine Coolant Over Temperature Condition", "อุณหภูมิน้ำหล่อเย็นสูงเกินกำหนด", DtcSeverity.CRITICAL, DtcStatus.CONFIRMED, AppOperationMode.SIMULATOR)
            )
        }
        return emptyList()
    }
}
