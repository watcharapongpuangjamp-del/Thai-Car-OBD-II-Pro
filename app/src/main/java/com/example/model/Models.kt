package com.example.model

enum class AppOperationMode(val displayName: String) {
    REAL_HARDWARE("Real Hardware (USB OTG)"),
    SIMULATOR("Virtual CAN Simulator")
}

enum class DataProvenance {
    REAL_HARDWARE,
    SIMULATOR,
    USER_ENTERED,
    HISTORICAL,
    ESTIMATED,
    AI_INFERRED,
    UNKNOWN
}

enum class SensorQuality {
    GOOD,
    STALE,
    ERROR,
    UNKNOWN
}

data class TelemetryValue<T>(
    val value: T?,
    val unit: String,
    val provenance: DataProvenance,
    val quality: SensorQuality = SensorQuality.UNKNOWN,
    val timestamp: Long = System.currentTimeMillis()
) {
    val isStale: Boolean
        get() = (System.currentTimeMillis() - timestamp) > 2500L
}

enum class ConnectionState(val labelTh: String, val labelEn: String, val isError: Boolean = false) {
    // Normal connection progression sequence
    DISCONNECTED("ไม่ได้เชื่อมต่อ", "Disconnected"),
    DEVICE_DETECTED("ตรวจพบอุปกรณ์ USB", "USB Device Detected"),
    PERMISSION_REQUIRED("รอการอนุญาตสิทธิ์ USB", "USB Permission Required"),
    PERMISSION_GRANTED("ได้รับสิทธิ์ USB แล้ว", "USB Permission Granted"),
    USB_OPEN("เปิดพอร์ต USB แล้ว", "USB Port Opened"),
    SERIAL_READY("สื่อสาร Serial พร้อม", "Serial Communication Ready"),
    ADAPTER_HANDSHAKE("กำลังเชื่อมต่อ ELM327", "Adapter Handshake"),
    ADAPTER_RESPONDING("ELM327 ตอบรับแล้ว", "Adapter Responding"),
    PROTOCOL_DETECTED("ตรวจพบโปรโตคอล OBD2", "OBD Protocol Detected"),
    ECU_RESPONDING("ECU รถยนต์ตอบรับแล้ว", "ECU Responding"),
    LIVE_DATA_VALIDATED("ตรวจสอบข้อมูลเซนเซอร์เรียบร้อย", "Live Telemetry Validated"),
    CONNECTED("เชื่อมต่อสมบูรณ์", "Fully Connected"),

    // Explicit error states
    PERMISSION_DENIED("ผู้ใช้ปฏิเสธสิทธิ์ USB", "USB Permission Denied", isError = true),
    USB_OPEN_FAILED("ไม่สามารถเปิดพอร์ต USB ได้", "Failed to Open USB Port", isError = true),
    SERIAL_ERROR("เกิดข้อผิดพลาดในการตั้งค่า Serial", "Serial Configuration Error", isError = true),
    ADAPTER_NOT_RESPONDING("อะแดปเตอร์ ELM327 ไม่ตอบสนอง", "ELM327 Adapter Not Responding", isError = true),
    PROTOCOL_DETECTION_FAILED("ไม่สามารถตรวจจับโปรโตคอล OBD2 ได้", "OBD Protocol Detection Failed", isError = true),
    ECU_NOT_RESPONDING("กล่อง ECU ไม่ตอบสนอง (ตรวจสอบสวิตช์กุญแจ)", "ECU Not Responding (Check Ignition)", isError = true),
    TIMEOUT("หมดเวลาการรอข้อมูลจาก ECU/Adapter", "Communication Timeout", isError = true),
    DEVICE_DISCONNECTED("อุปกรณ์ USB ถูกถอดออก", "USB Device Detached", isError = true),
    DISCONNECTED_STALE("ข้อมูลค้างเติ่งเนื่องจาก USB Buffer ว่าง >500มก.", "Disconnected - Buffer Idle >500ms", isError = true),
    ERROR("เกิดข้อผิดพลาดในการเชื่อมต่อ", "Connection Error", isError = true)
}

sealed class UsbConnectionState {
    object Disconnected : UsbConnectionState()
    data class PermissionRequested(val deviceName: String) : UsbConnectionState()
    data class PermissionGranted(val deviceName: String) : UsbConnectionState()
    object Opening : UsbConnectionState()
    data class Connected(val deviceName: String) : UsbConnectionState()
    data class Error(val message: String) : UsbConnectionState()
}

data class LiveSensorData(
    val isConnected: Boolean,
    val connectionState: ConnectionState,
    
    val rpmData: TelemetryValue<Int> = TelemetryValue(null, "RPM", DataProvenance.UNKNOWN),
    val speedData: TelemetryValue<Int> = TelemetryValue(null, "km/h", DataProvenance.UNKNOWN),
    val coolantData: TelemetryValue<Int> = TelemetryValue(null, "°C", DataProvenance.UNKNOWN),
    val voltageData: TelemetryValue<Float> = TelemetryValue(null, "V", DataProvenance.UNKNOWN),
    val mapData: TelemetryValue<Int> = TelemetryValue(null, "kPa", DataProvenance.UNKNOWN),
    val boostData: TelemetryValue<Float> = TelemetryValue(null, "Bar", DataProvenance.UNKNOWN),
    val fuelRateData: TelemetryValue<Float> = TelemetryValue(null, "L/h", DataProvenance.UNKNOWN),
    val throttleData: TelemetryValue<Int> = TelemetryValue(null, "%", DataProvenance.UNKNOWN),
    val intakeTempData: TelemetryValue<Int> = TelemetryValue(null, "°C", DataProvenance.UNKNOWN),
    val engineLoadData: TelemetryValue<Int> = TelemetryValue(null, "%", DataProvenance.UNKNOWN),

    val pidPerSec: Int,
    val latencyMs: Int,
    val mode: AppOperationMode,
    val statusMessage: String = ""
) {
    val rpm: Int? get() = rpmData.value
    val speedKmh: Int? get() = speedData.value
    val coolantTempC: Int? get() = coolantData.value
    val batteryVoltage: Float? get() = voltageData.value
    val mapPressureKpa: Int? get() = mapData.value
    val boostPressureBar: Float? get() = boostData.value
    val fuelRateLph: Float? get() = fuelRateData.value
    val throttlePosPercent: Int? get() = throttleData.value
    val intakeTempC: Int? get() = intakeTempData.value
    val engineLoadPercent: Int? get() = engineLoadData.value

    companion object {
        fun disconnected(mode: AppOperationMode, message: String = "ยังไม่ได้เชื่อมต่ออุปกรณ์") = LiveSensorData(
            isConnected = false,
            connectionState = ConnectionState.DISCONNECTED,
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
            map: Int,
            boost: Float,
            fuelRate: Float,
            throttle: Int,
            intakeTemp: Int,
            load: Int
        ) = LiveSensorData(
            isConnected = true,
            connectionState = ConnectionState.CONNECTED,
            rpmData = TelemetryValue(rpm, "RPM", DataProvenance.SIMULATOR),
            speedData = TelemetryValue(speed, "km/h", DataProvenance.SIMULATOR),
            coolantData = TelemetryValue(coolant, "°C", DataProvenance.SIMULATOR),
            voltageData = TelemetryValue(voltage, "V", DataProvenance.SIMULATOR),
            mapData = TelemetryValue(map, "kPa", DataProvenance.SIMULATOR),
            boostData = TelemetryValue(boost, "Bar", DataProvenance.SIMULATOR),
            fuelRateData = TelemetryValue(fuelRate, "L/h", DataProvenance.SIMULATOR),
            throttleData = TelemetryValue(throttle, "%", DataProvenance.SIMULATOR),
            intakeTempData = TelemetryValue(intakeTemp, "°C", DataProvenance.SIMULATOR),
            engineLoadData = TelemetryValue(load, "%", DataProvenance.SIMULATOR),
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
    val rawPromptUsed: String = "",
    val ruleReport: com.example.rules.RuleEngineReport? = null
)

sealed class DtcScanStatus {
    object SUCCESS : DtcScanStatus()
    object NO_CODES : DtcScanStatus()
    object PARTIAL : DtcScanStatus() // พบแค่บางโมดูล
    data class FAILED(val reason: String) : DtcScanStatus()
}

data class DtcScanResult(
    val codes: List<DtcCode>,
    val status: DtcScanStatus,
    val timestamp: Long = System.currentTimeMillis()
)

data class DiagnosticSession(
    val sessionId: String = "DS-${System.currentTimeMillis() % 1000000}",
    val timestamp: Long = System.currentTimeMillis(),
    val vehicleName: String = "รถยนต์ทดสอบ",
    val vehicleMake: String = "Toyota",
    val vehicleModel: String = "Hilux Revo",
    val vehicleYear: Int = 2022,
    val vehicleVin: String = "MHFAB22G0K1234567",
    val licensePlate: String = "1กข-9999 กทม.",
    val odometerKm: Int = 125400,
    val dtcCodes: List<DtcCode> = emptyList(),
    val telemetrySnapshot: LiveSensorData? = null,
    val ruleReport: com.example.rules.RuleEngineReport? = null,
    val aiAnalysis: AiAnalysisResult? = null,
    val technicianName: String = "ช่างผู้ตรวจสอบระบบ Thai OBD-II Pro",
    val notes: String = "",
    val mode: AppOperationMode = AppOperationMode.REAL_HARDWARE
)


data class ValidatedDiagnosticSnapshot(
    val id: String = java.util.UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val isRealHardware: Boolean,
    val liveData: LiveSensorData?,
    val dtcResult: DtcScanResult?,
    val rawEvidence: List<com.example.hardware.obd.ObdFrame> = emptyList()
)
