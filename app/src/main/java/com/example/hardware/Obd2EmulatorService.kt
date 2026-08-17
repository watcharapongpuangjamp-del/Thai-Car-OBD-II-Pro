package com.example.hardware

import com.example.model.AppOperationMode
import com.example.model.ConnectionState
import com.example.model.DtcCode
import com.example.model.DtcSeverity
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
                    boost = 0.60f,
                    fuelRate = 8.5f,
                    throttle = 45,
                    intakeTemp = 58,
                    load = 65
                )
            }
            SimulatorScenario.SENSOR_FAULT -> {
                _simulatedTelemetry.value = LiveSensorData.simulated(
                    rpm = 1850 + Random.nextInt(-80, 80),
                    speed = 60,
                    coolant = 104,
                    voltage = 12.1f, // Low Battery / Alternator issue
                    boost = 0.20f,
                    fuelRate = 9.8f,
                    throttle = 30,
                    intakeTemp = 42,
                    load = 58
                )
            }
        }
    }

    suspend fun generateSimulatorDtcs(scenario: SimulatorScenario): List<DtcCode> {
        delay(600) // Simulate ECU scan delay
        return when (scenario) {
            SimulatorScenario.IDLE, SimulatorScenario.CRUISE -> listOf()
            SimulatorScenario.HIGH_BOOST -> listOf(
                DtcCode(
                    code = "P0234",
                    module = "ECM (กล่องควบคุมเครื่องยนต์)",
                    descriptionEn = "Turbocharger/Supercharger Overboost Condition",
                    descriptionTh = "แรงดันเทอร์โบชาร์จเจอร์เกินค่ามาตรฐาน (Overboost)",
                    severity = DtcSeverity.WARNING,
                    modeProvenance = AppOperationMode.SIMULATOR
                )
            )
            SimulatorScenario.OVERHEAT_WARNING -> listOf(
                DtcCode(
                    code = "P0217",
                    module = "ECM (กล่องควบคุมเครื่องยนต์)",
                    descriptionEn = "Engine Coolant Over Temperature Condition",
                    descriptionTh = "อุณหภูมิความร้อนน้ำ cooling เครื่องยนต์สูงเกินระดับปลอดภัย",
                    severity = DtcSeverity.CRITICAL,
                    modeProvenance = AppOperationMode.SIMULATOR
                ),
                DtcCode(
                    code = "P1256",
                    module = "ECM (กล่องควบคุมเครื่องยนต์)",
                    descriptionEn = "Coolant Temperature Sensor Signal Circuit Fault",
                    descriptionTh = "สัญญาณวงจรเซนเซอร์อุณหภูมิน้ำขัดข้อง",
                    severity = DtcSeverity.WARNING,
                    modeProvenance = AppOperationMode.SIMULATOR
                )
            )
            SimulatorScenario.SENSOR_FAULT -> listOf(
                DtcCode(
                    code = "P0171",
                    module = "ECM (กล่องควบคุมเครื่องยนต์)",
                    descriptionEn = "System Too Lean (Bank 1)",
                    descriptionTh = "ส่วนผสมไอดีบางเกินไป (System Too Lean)",
                    severity = DtcSeverity.WARNING,
                    modeProvenance = AppOperationMode.SIMULATOR
                ),
                DtcCode(
                    code = "C0035",
                    module = "ABS / ESC (ระบบเบรกกันล็อก)",
                    descriptionEn = "Left Front Wheel Speed Sensor Circuit Fault",
                    descriptionTh = "วงจรเซนเซอร์ความเร็วล้อหน้าซ้ายขัดข้อง",
                    severity = DtcSeverity.WARNING,
                    modeProvenance = AppOperationMode.SIMULATOR
                ),
                DtcCode(
                    code = "P0741",
                    module = "TCM (เกียร์อัตโนมัติ)",
                    descriptionEn = "Torque Converter Clutch Circuit Performance or Stuck Off",
                    descriptionTh = "ระบบทอร์กคอนเวอร์เตอร์คลัตช์ลื่นหรือค้างเปิด",
                    severity = DtcSeverity.CRITICAL,
                    modeProvenance = AppOperationMode.SIMULATOR
                )
            )
        }
    }
}
