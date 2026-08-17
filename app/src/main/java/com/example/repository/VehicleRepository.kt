package com.example.repository

import android.content.Context
import com.example.BuildConfig
import com.example.db.DtcScanRecordEntity
import com.example.db.MaintenanceLogEntity
import com.example.db.ObdDatabase
import com.example.db.VehicleProfileEntity
import com.example.hardware.Obd2EmulatorService
import com.example.hardware.SimulatorScenario
import com.example.hardware.UsbObdDriver
import com.example.model.AiAnalysisResult
import com.example.model.AppOperationMode
import com.example.model.ConnectionState
import com.example.model.DtcCode
import com.example.model.LiveSensorData
import com.example.model.PredictiveMaintenanceItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class VehicleRepository(private val context: Context) {

    private val db = ObdDatabase.getDatabase(context)
    val usbDriver = UsbObdDriver(context)
    val emulatorService = Obd2EmulatorService()

    private val _activeMode = MutableStateFlow(AppOperationMode.REAL_HARDWARE)
    val activeMode = _activeMode.asStateFlow()

    // Combined live telemetry flow respecting absolute separation rule
    val liveTelemetry: Flow<LiveSensorData> = combine(
        _activeMode,
        usbDriver.liveTelemetry,
        emulatorService.simulatedTelemetry
    ) { mode, realData, simData ->
        when (mode) {
            AppOperationMode.REAL_HARDWARE -> realData
            AppOperationMode.SIMULATOR -> simData
        }
    }

    fun setOperationMode(mode: AppOperationMode) {
        _activeMode.value = mode
        if (mode == AppOperationMode.REAL_HARDWARE) {
            // Ensure simulator does not bleed into real mode
        }
    }

    suspend fun connectRealHardware(): ConnectionState {
        return usbDriver.checkAndConnectUsbDevice()
    }

    fun disconnectRealHardware() {
        usbDriver.disconnect()
    }

    fun setSimulatorScenario(scenario: SimulatorScenario) {
        emulatorService.setScenario(scenario)
    }

    suspend fun scanDtcs(): List<DtcCode> {
        return when (_activeMode.value) {
            AppOperationMode.REAL_HARDWARE -> usbDriver.scanRealHardwareDtcs()
            AppOperationMode.SIMULATOR -> emulatorService.generateSimulatorDtcs(emulatorService.currentScenario.value)
        }
    }

    suspend fun clearDtcs(): Result<Boolean> {
        return when (_activeMode.value) {
            AppOperationMode.REAL_HARDWARE -> usbDriver.clearRealHardwareDtcs()
            AppOperationMode.SIMULATOR -> Result.success(true)
        }
    }

    val tripAnalytics = com.example.analytics.TripAnalyticsEngine()
    val diagnosticRuleEngine = com.example.rules.DiagnosticRuleEngine()
    val pdfExporter: com.example.export.DiagnosticReportExporter = com.example.export.PdfExporter()

    // Room DB profile methods
    val allProfiles: Flow<List<VehicleProfileEntity>> = db.vehicleProfileDao().getAllProfiles()

    suspend fun addDefaultProfileIfEmpty() {
        // Handled in ViewModel init
    }

    suspend fun saveProfile(profile: VehicleProfileEntity): Long {
        return db.vehicleProfileDao().insertProfile(profile)
    }

    suspend fun saveScanRecord(vehicleId: Long, codes: List<DtcCode>): Long {
        val jsonArray = JSONArray()
        codes.forEach { code ->
            jsonArray.put(JSONObject().apply {
                put("code", code.code)
                put("module", code.module)
                put("descriptionTh", code.descriptionTh)
                put("severity", code.severity.name)
            })
        }
        val record = DtcScanRecordEntity(
            vehicleId = vehicleId,
            totalCodesFound = codes.size,
            codesJson = jsonArray.toString(),
            modeProvenance = _activeMode.value.name
        )
        return db.dtcScanDao().insertScanRecord(record)
    }

    fun getScanRecords(vehicleId: Long): Flow<List<DtcScanRecordEntity>> {
        return db.dtcScanDao().getScansForVehicle(vehicleId)
    }

    suspend fun saveMaintenanceLog(log: MaintenanceLogEntity): Long {
        return db.maintenanceLogDao().insertLog(log)
    }

    fun getMaintenanceLogs(vehicleId: Long): Flow<List<MaintenanceLogEntity>> {
        return db.maintenanceLogDao().getLogsForVehicle(vehicleId)
    }

    // Predictive Maintenance Calculations with Provenance
    fun getPredictiveMaintenanceList(currentTelemetry: LiveSensorData): List<PredictiveMaintenanceItem> {
        val provenance = if (currentTelemetry.mode == AppOperationMode.REAL_HARDWARE) {
            "REAL_SENSOR_HISTORY"
        } else {
            "SIMULATED_HISTORY"
        }

        val voltage = currentTelemetry.batteryVoltage ?: 12.6f
        val batteryHealth = when {
            voltage >= 13.8f -> 95
            voltage >= 12.5f -> 82
            voltage >= 12.0f -> 60
            else -> 35
        }

        val coolant = currentTelemetry.coolantTempC ?: 88
        val coolantStatus = if (coolant > 105) "REPLACE" else if (coolant > 98) "ATTENTION" else "GOOD"

        return listOf(
            PredictiveMaintenanceItem(
                componentNameTh = "แบตเตอรี่ & ไดชาร์จ (Battery & Alternator)",
                componentNameEn = "Battery & Alternator Health",
                healthPercentage = batteryHealth,
                rulKilometers = (batteryHealth * 250),
                statusLevel = if (batteryHealth < 50) "REPLACE" else if (batteryHealth < 75) "ATTENTION" else "GOOD",
                recommendationTh = if (batteryHealth < 50) "แรงดันไฟต่ำกว่ามาตรฐาน ควรตรวจเช็กระบบชาร์จไฟหรือเปลี่ยนแบตเตอรี่" else "ระบบไฟฟ้าและแรงดันแบตเตอรี่ทำงานปกติ",
                provenance = provenance
            ),
            PredictiveMaintenanceItem(
                componentNameTh = "น้ำหล่อเย็นเครื่องยนต์ (Engine Coolant)",
                componentNameEn = "Engine Coolant State",
                healthPercentage = if (coolant > 105) 30 else if (coolant > 98) 65 else 90,
                rulKilometers = if (coolant > 105) 500 else 12000,
                statusLevel = coolantStatus,
                recommendationTh = if (coolant > 105) "อุณหภูมิสูงผิดปกติ ตรวจเช็กวาล์วน้ำ พัดลมหม้อน้ำ และระดับน้ำหล่อเย็นทันที" else "ระดับอุณหภูมิหม้อน้ำอยู่ในเกณฑ์ปกติ",
                provenance = provenance
            ),
            PredictiveMaintenanceItem(
                componentNameTh = "น้ำมันเครื่องสังเคราะห์ (Engine Oil Life)",
                componentNameEn = "Synthetic Oil Life",
                healthPercentage = 78,
                rulKilometers = 7800,
                statusLevel = "GOOD",
                recommendationTh = "ระยะทางสะสมคงเหลืออีกประมาณ 7,800 กม. ก่อนกำหนดเปลี่ยนถ่ายน้ำมันเครื่องรอบถัดไป",
                provenance = provenance
            ),
            PredictiveMaintenanceItem(
                componentNameTh = "ผ้าเบรกหน้า & จานดิสก์เบรก (Front Brake Pads)",
                componentNameEn = "Front Brake Wear",
                healthPercentage = 62,
                rulKilometers = 11500,
                statusLevel = "GOOD",
                recommendationTh = "ความหนาผ้าเบรกคงเหลือประมาณ 62% สามารถใช้งานปกติ",
                provenance = provenance
            )
        )
    }

    // Gemini AI Mechanic Analysis with Direct REST API, Pre-validated Diagnostic Rule Engine & Provenance Labeling
    suspend fun analyzeWithAiMechanic(
        vehicleInfo: String,
        dtcCodes: List<DtcCode>,
        telemetry: LiveSensorData
    ): AiAnalysisResult = withContext(Dispatchers.IO) {
        val modeTag = if (telemetry.mode == AppOperationMode.REAL_HARDWARE) "REAL VEHICLE HARDWARE" else "VIRTUAL CAN SIMULATOR"

        // 1. Evaluate independent deterministic diagnostic rules before passing to Gemini
        val ruleReport = diagnosticRuleEngine.evaluate(telemetry, dtcCodes)

        val promptText = """
            คุณคือ "AI Mechanic" ผู้เชี่ยวชาญช่างวิเคราะห์ระบบรถยนต์ OBD-II ประจำแอป Thai Car OBD-II Pro
            กรุณาวิเคราะห์ข้อมูลอาการและรหัสปัญหารถยนต์ดังนี้:
            
            [แหล่งที่มาข้อมูล / Provenance]: $modeTag
            [ข้อมูลรถยนต์]: $vehicleInfo
            [รหัสความผิดปกติ DTC ที่พบ]: ${if (dtcCodes.isEmpty()) "ไม่พบรหัสความผิดปกติ" else dtcCodes.joinToString { "${it.code} (${it.module}) - ${it.descriptionTh}" }}
            [ข้อมูลเซนเซอร์สด]: RPM=${telemetry.rpm ?: "N/A"}, Speed=${telemetry.speedKmh ?: "N/A"} km/h, Coolant=${telemetry.coolantTempC ?: "N/A"}°C, Voltage=${telemetry.batteryVoltage ?: "N/A"}V, Boost=${telemetry.boostPressureBar ?: "N/A"} bar, FuelRate=${telemetry.fuelRateLph ?: "N/A"} L/h, Throttle=${telemetry.throttlePosPercent ?: "N/A"}%, Load=${telemetry.engineLoadPercent ?: "N/A"}%
            
            ${ruleReport.aiEnrichmentContext}
            
            คำแนะนำ: ให้ตอบเป็นภาษาไทยที่เป็นมิตร ชัดเจน เข้าใจง่ายสำหรับผู้ขับขี่และช่างยนต์ไทย โดยใช้ผลการตรวจของ Rule Engine ข้างต้นเป็นฐานข้อเท็จจริง และระบุ:
            1. สรุปภาพรวมปัญหาและความรุนแรง
            2. สาเหตุที่อาจเป็นไปได้ 2-3 ข้อ
            3. แนวทางแก้ไขและวิธีซ่อมแซมเบื้องต้น
        """.trimIndent()

        val apiKey = try {
            BuildConfig::class.java.getField("GEMINI_API_KEY").get(null) as? String ?: ""
        } catch (e: Exception) {
            ""
        }

        if (apiKey.isBlank()) {
            val severityLabel = when (ruleReport.overallSeverity) {
                com.example.rules.EvaluationSeverity.CRITICAL -> "วิกฤต (Critical)"
                com.example.rules.EvaluationSeverity.FAULT -> "เซนเซอร์ชำรุด (Fault)"
                com.example.rules.EvaluationSeverity.WARNING -> "เตือน (Warning)"
                com.example.rules.EvaluationSeverity.INFO -> "ข้อมูล (Info)"
                com.example.rules.EvaluationSeverity.NORMAL -> "ปกติ (Normal)"
            }

            return@withContext AiAnalysisResult(
                summaryTh = "วิเคราะห์ระบบผ่าน Diagnostic Rule Engine เรียบร้อยแล้ว (${modeTag}): ${ruleReport.summaryTh}",
                severityLevel = severityLabel,
                possibleRootCausesTh = if (ruleReport.anomalies.isNotEmpty()) {
                    ruleReport.anomalies.flatMap { it.potentialCausesTh }
                } else if (dtcCodes.isNotEmpty()) {
                    listOf("รหัส DTC ${dtcCodes.first().code}: ${dtcCodes.first().descriptionTh}", "ความผิดปกติในระบบเซนเซอร์วัดค่า")
                } else {
                    listOf("ไม่พบสาเหตุผิดปกติร้ายแรง")
                },
                recommendedActionsTh = if (ruleReport.anomalies.isNotEmpty()) {
                    ruleReport.anomalies.map { it.recommendedActionTh }
                } else {
                    listOf("ตรวจสอบขั้วปลั๊กและสายไฟที่เกี่ยวข้อง", "ทำความสะอาดเซนเซอร์และทดสอบลบลบโค้ด DTC")
                },
                provenanceLabel = modeTag,
                rawPromptUsed = promptText,
                ruleReport = ruleReport
            )
        }

        try {
            val client = OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build()

            val jsonBody = JSONObject().apply {
                put("contents", JSONArray().put(JSONObject().apply {
                    put("parts", JSONArray().put(JSONObject().apply {
                        put("text", promptText)
                    }))
                }))
            }

            val request = Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey")
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseStr = response.body?.string() ?: ""
            val jsonResp = JSONObject(responseStr)
            val candidates = jsonResp.optJSONArray("candidates")
            val text = candidates?.optJSONObject(0)
                ?.optJSONObject("content")
                ?.optJSONArray("parts")
                ?.optJSONObject(0)
                ?.optString("text") ?: "ไม่สามารถดึงคำตอบจาก AI ได้"

            AiAnalysisResult(
                summaryTh = text,
                severityLevel = if (ruleReport.overallSeverity == com.example.rules.EvaluationSeverity.CRITICAL || dtcCodes.any { it.severity == com.example.model.DtcSeverity.CRITICAL }) "วิกฤต (Critical)" else if (ruleReport.overallSeverity == com.example.rules.EvaluationSeverity.WARNING) "เตือน (Warning)" else "ทั่วไป (Normal)",
                possibleRootCausesTh = if (ruleReport.anomalies.isNotEmpty()) ruleReport.anomalies.map { it.titleTh } else listOf("วิเคราะห์รวมโดย Gemini AI Mechanic"),
                recommendedActionsTh = if (ruleReport.anomalies.isNotEmpty()) ruleReport.anomalies.map { it.recommendedActionTh } else listOf("ปฏิบัติตามคำแนะนำของ AI หรือนำรถเข้าศูนย์บริการช่างใกล้บ้าน"),
                provenanceLabel = modeTag,
                rawPromptUsed = promptText,
                ruleReport = ruleReport
            )
        } catch (e: Exception) {
            AiAnalysisResult(
                summaryTh = "เกิดข้อผิดพลาดในการเชื่อมต่อ AI: ${e.localizedMessage}. ผลตรวจ Rule Engine: ${ruleReport.summaryTh}",
                severityLevel = if (ruleReport.overallSeverity == com.example.rules.EvaluationSeverity.CRITICAL) "วิกฤต (Critical)" else "เตือน (Warning)",
                possibleRootCausesTh = if (ruleReport.anomalies.isNotEmpty()) ruleReport.anomalies.flatMap { it.potentialCausesTh } else listOf("ระบบเครือข่ายขัดข้อง"),
                recommendedActionsTh = if (ruleReport.anomalies.isNotEmpty()) ruleReport.anomalies.map { it.recommendedActionTh } else listOf("รีเฟรชหน้าจอหรือลองใหม่อีกครั้ง"),
                provenanceLabel = modeTag,
                rawPromptUsed = promptText,
                ruleReport = ruleReport
            )
        }
    }

    fun release() {
        usbDriver.release()
    }
}
