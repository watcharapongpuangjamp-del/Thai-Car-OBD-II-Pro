package com.example.model

enum class AppOperationMode(val displayName: String) {
    REAL_HARDWARE("Real Hardware (USB OTG)"),
    SIMULATOR("Virtual CAN Simulator")
}

enum class ConnectionState(val labelTh: String, val labelEn: String) {
    DISCONNECTED("ไม่ได้เชื่อมต่อ", "Disconnected"),
    DEVICE_DETECTED("ตรวจพบอุปกรณ์ USB", "USB Device Detected"),
    PERMISSION_GRANTED("ได้รับสิทธิ์ USB", "USB Permission Granted"),
    USB_OPEN("เปิดพอร์ต USB แล้ว", "USB Port Opened"),
    SERIAL_READY("สื่อสาร Serial พร้อม", "Serial Communication Ready"),
    ADAPTER_HANDSHAKE("กำลังเชื่อมต่อ ELM327", "Adapter Handshake"),
    ADAPTER_RESPONDING("ELM327 ตอบรับแล้ว", "Adapter Responding"),
    PROTOCOL_DETECTED("ตรวจพบโปรโตคอล OBD2", "OBD Protocol Detected"),
    ECU_RESPONDING("ECU รถยนต์ตอบรับแล้ว", "ECU Responding"),
    LIVE_DATA_VALIDATED("ตรวจสอบข้อมูลเซนเซอร์เรียบร้อย", "Live Telemetry Validated"),
    CONNECTED("เชื่อมต่อสมบูรณ์", "Fully Connected"),
    ERROR("เกิดข้อผิดพลาดในการเชื่อมต่อ", "Connection Error")
}

data class LiveSensorData(
    val isConnected: Boolean,
    val connectionState: ConnectionState,
    val rpm: Int?,
    val speedKmh: Int?,
    val coolantTempC: Int?,
    val batteryVoltage: Float?,
    val boostPressureBar: Float?,
    val fuelRateLph: Float?,
    val throttlePosPercent: Int?,
    val intakeTempC: Int?,
    val engineLoadPercent: Int?,
    val pidPerSec: Int,
    val latencyMs: Int,
    val mode: AppOperationMode,
    val statusMessage: String = ""
) {
    companion object {
        fun disconnected(mode: AppOperationMode, message: String = "ยังไม่ได้เชื่อมต่ออุปกรณ์") = LiveSensorData(
            isConnected = false,
            connectionState = ConnectionState.DISCONNECTED,
            rpm = null,
            speedKmh = null,
            coolantTempC = null,
            batteryVoltage = null,
            boostPressureBar = null,
            fuelRateLph = null,
            throttlePosPercent = null,
            intakeTempC = null,
            engineLoadPercent = null,
            pidPerSec = 0,
            latencyMs = 0,
            mode = mode,
            statusMessage = message
        )

        fun simulated(
            rpm: Int,
            speed: Int,
            coolant: Int,
            voltage: Float,
            boost: Float,
            fuelRate: Float,
            throttle: Int,
            intakeTemp: Int,
            load: Int
        ) = LiveSensorData(
            isConnected = true,
            connectionState = ConnectionState.CONNECTED,
            rpm = rpm,
            speedKmh = speed,
            coolantTempC = coolant,
            batteryVoltage = voltage,
            boostPressureBar = boost,
            fuelRateLph = fuelRate,
            throttlePosPercent = throttle,
            intakeTempC = intakeTemp,
            engineLoadPercent = load,
            pidPerSec = 32,
            latencyMs = 15,
            mode = AppOperationMode.SIMULATOR,
            statusMessage = "Virtual CAN Stream Active"
        )
    }
}

enum class DtcSeverity(val labelTh: String, val colorHex: String) {
    CRITICAL("วิกฤต - ควรหยุดรถทันที", "#E53935"),
    WARNING("เตือน - ตรวจเช็กโดยเร็ว", "#FB8C00"),
    INFO("ข้อมูล - บันทึกประวัติ", "#1E88E5")
}

data class DtcCode(
    val code: String,
    val module: String, // ECM, TCM, ABS, SRS, BCM
    val descriptionEn: String,
    val descriptionTh: String,
    val severity: DtcSeverity,
    val modeProvenance: AppOperationMode,
    val timestamp: Long = System.currentTimeMillis()
)

data class PredictiveMaintenanceItem(
    val componentNameTh: String,
    val componentNameEn: String,
    val healthPercentage: Int,
    val rulKilometers: Int,
    val statusLevel: String, // GOOD, ATTENTION, REPLACE
    val recommendationTh: String,
    val provenance: String // REAL_SENSOR_HISTORY, SIMULATED_HISTORY, ESTIMATED
)

data class AiAnalysisResult(
    val summaryTh: String,
    val severityLevel: String,
    val possibleRootCausesTh: List<String>,
    val recommendedActionsTh: List<String>,
    val provenanceLabel: String,
    val rawPromptUsed: String = ""
)
