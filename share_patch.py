import re

with open('app/src/main/java/com/example/repository/VehicleRepository.kt', 'r') as f:
    content = f.read()

# Replace analyzeWithAiMechanic with generateMarkdownDiagnosticContext
old_analyze = """    suspend fun analyzeWithAiMechanic(
        vehicleInfo: String,
        dtcCodes: List<DtcCode>,
        telemetry: LiveSensorData
    ): AiAnalysisResult = withContext(Dispatchers.IO) {"""

new_analyze = """    suspend fun analyzeWithAiMechanic(
        vehicleInfo: String,
        dtcCodes: List<DtcCode>,
        telemetry: LiveSensorData
    ): AiAnalysisResult = withContext(Dispatchers.IO) {
        val modeTag = if (telemetry.mode == AppOperationMode.REAL_HARDWARE) "REAL VEHICLE HARDWARE" else "VIRTUAL CAN SIMULATOR"
        val ruleReport = diagnosticRuleEngine.evaluate(telemetry, dtcCodes)
        val promptText = \"\"\"
            กรุณาวิเคราะห์ข้อมูลอาการและรหัสปัญหารถยนต์ดังนี้:
            
            [แหล่งที่มาข้อมูล / Provenance]: $modeTag
            [ข้อมูลรถยนต์]: $vehicleInfo
            [รหัสความผิดปกติ DTC ที่พบ]: ${if (dtcCodes.isEmpty()) "ไม่พบรหัสความผิดปกติ" else dtcCodes.joinToString { "${it.code} (${it.module}) - ${it.descriptionTh}" }}
            [ข้อมูลเซนเซอร์สด]: RPM=${telemetry.rpm ?: "N/A"}, Speed=${telemetry.speedKmh ?: "N/A"} km/h, Coolant=${telemetry.coolantTempC ?: "N/A"}°C, Voltage=${telemetry.batteryVoltage ?: "N/A"}V, Boost=${telemetry.boostPressureBar ?: "N/A"} bar, FuelRate=${telemetry.fuelRateLph ?: "N/A"} L/h, Throttle=${telemetry.throttlePosPercent ?: "N/A"}%, Load=${telemetry.engineLoadPercent ?: "N/A"}%
            
            ${ruleReport.aiEnrichmentContext}
        \"\"\".trimIndent()

        val severityLabel = when (ruleReport.overallSeverity) {
            com.example.rules.EvaluationSeverity.CRITICAL -> "วิกฤต (Critical)"
            com.example.rules.EvaluationSeverity.FAULT -> "เซนเซอร์ชำรุด (Fault)"
            com.example.rules.EvaluationSeverity.WARNING -> "เตือน (Warning)"
            com.example.rules.EvaluationSeverity.INFO -> "ข้อมูล (Info)"
            com.example.rules.EvaluationSeverity.NORMAL -> "ปกติ (Normal)"
        }
        
        AiAnalysisResult(
            summaryTh = "กรุณากดปุ่มด้านล่างเพื่อส่งข้อมูลนี้ไปยังแอป Google Gemini สำหรับวิเคราะห์อย่างละเอียด",
            severityLevel = severityLabel,
            possibleRootCausesTh = emptyList(),
            recommendedActionsTh = emptyList(),
            provenanceLabel = modeTag,
            rawPromptUsed = promptText,
            ruleReport = ruleReport
        )
    }"""

content = re.sub(r'    suspend fun analyzeWithAiMechanic\([\s\S]*?fun release\(\)', new_analyze + "\n\n    fun release()", content)

with open('app/src/main/java/com/example/repository/VehicleRepository.kt', 'w') as f:
    f.write(content)
