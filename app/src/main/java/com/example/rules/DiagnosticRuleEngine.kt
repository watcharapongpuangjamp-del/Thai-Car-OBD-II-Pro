package com.example.rules

import com.example.model.DtcCode
import com.example.model.DtcSeverity
import com.example.model.LiveSensorData

class DiagnosticRuleEngine {

    fun evaluate(
        telemetry: LiveSensorData,
        dtcCodes: List<DtcCode> = emptyList()
    ): RuleEngineReport {
        val anomalies = mutableListOf<DiagnosticAnomaly>()
        var isTelemetryPlausible = true

        // 1. Engine Temperature Evaluation
        val tempEval = EngineTemperatureThresholds.evaluate(telemetry.coolantTempC)
        tempEval.anomaly?.let { anomalies.add(it) }

        // 2. Voltage & Electrical Evaluation
        val voltEval = VoltageThresholds.evaluate(telemetry.batteryVoltage, telemetry.rpm)
        voltEval.anomaly?.let { anomalies.add(it) }

        // 3. Fuel & Load Evaluation
        val fuelEval = FuelAndLoadThresholds.evaluate(
            throttlePct = telemetry.throttlePosPercent,
            loadPct = telemetry.engineLoadPercent,
            boostBar = telemetry.boostPressureBar,
            fuelRateLph = telemetry.fuelRateLph,
            speedKmh = telemetry.speedKmh,
            rpm = telemetry.rpm
        )
        anomalies.addAll(fuelEval.anomalies)

        // 4. Physical Plausibility Rules (Sensor Cross-Validation)
        val speed = telemetry.speedKmh ?: 0
        val rpm = telemetry.rpm ?: 0

        // Plausibility Rule A: Moving Vehicle with 0 Engine RPM (while connected)
        if (speed > 25 && rpm == 0 && telemetry.isConnected) {
            isTelemetryPlausible = false
            anomalies.add(
                DiagnosticAnomaly(
                    category = DiagnosticCategory.SENSOR_PLAUSIBILITY,
                    severity = EvaluationSeverity.WARNING,
                    parameterName = "Speed vs RPM Plausibility",
                    measuredValue = "Speed: $speed km/h, RPM: 0",
                    expectedRange = "RPM > 500 when Speed > 25 km/h",
                    titleTh = "ความสอดคล้องของสัญญาณความเร็วและรอบเครื่องยนต์ผิดปกติ",
                    descriptionTh = "รถกำลังเคลื่อนที่ด้วยความเร็ว $speed km/h แต่ค่ารอบเครื่องยนต์เป็น 0 RPM บ่งชี้ว่าเซนเซอร์ Crankshaft หรือการดึงข้อมูล PID มีปัญหา",
                    potentialCausesTh = listOf("เซนเซอร์ตำแหน่งเพลาข้อเหวี่ยง (CKP) ส่งสัญญาณขาดหาย", "โปรโตคอล OBD ตอบสนองล่าช้าในบาง PID"),
                    recommendedActionTh = "ตรวจสอบสายสัญญาณเซนเซอร์รอบเครื่องยนต์ CKP Sensor"
                )
            )
        }

        // Plausibility Rule B: Severe High RPM at Standstill (> 5500 RPM at 0 km/h)
        if (speed == 0 && rpm > 5500) {
            anomalies.add(
                DiagnosticAnomaly(
                    category = DiagnosticCategory.SENSOR_PLAUSIBILITY,
                    severity = EvaluationSeverity.WARNING,
                    parameterName = "Over-rev at Neutral",
                    measuredValue = "RPM: $rpm, Speed: 0 km/h",
                    expectedRange = "RPM < 5000 at Standstill",
                    titleTh = "เครื่องยนต์หมุนรอบสูงมากขณะรถจอดนิ่ง (High RPM Over-rev)",
                    descriptionTh = "เครื่องยนต์มีรอบสูงถึง $rpm RPM ในขณะรถจอด เสี่ยงต่อความเสียหายของวาล์วและลูกสูบ",
                    potentialCausesTh = listOf("การเบิ้ลคันเร่งเกินขีดจำกัดขณะเกียร์ว่าง", "สายคันเร่งหรือลิ้นเร่งค้าง"),
                    recommendedActionTh = "ปล่อยคันเร่งทันทีและตรวจสอบกลไกลิ้นปีกผีเสื้อ"
                )
            )
        }

        // 5. Correlate with DTCs
        if (dtcCodes.isNotEmpty()) {
            for (dtc in dtcCodes) {
                if (dtc.severity == DtcSeverity.CRITICAL) {
                    anomalies.add(
                        DiagnosticAnomaly(
                            category = DiagnosticCategory.CORRELATED_DTC,
                            severity = EvaluationSeverity.CRITICAL,
                            parameterName = "DTC ${dtc.code}",
                            measuredValue = "${dtc.module}: ${dtc.descriptionEn}",
                            expectedRange = "No Active DTCs",
                            titleTh = "รหัสข้อผิดพลาดระดับวิกฤต: ${dtc.code}",
                            descriptionTh = "ตรวจพบรหัสความผิดปกติ ${dtc.code} ในกล่อง ${dtc.module} (${dtc.descriptionTh})",
                            potentialCausesTh = listOf("ความผิดปกติในระบบชิ้นส่วนตามนิยาม SAE J2019 ของโค้ด ${dtc.code}"),
                            recommendedActionTh = "สแกนรหัสและแก้ไขจุดบกพร่องตามคู่มือซ่อมก่อนทำการลบโค้ด"
                        )
                    )
                }
            }
        }

        // Determine Overall Severity
        val overallSeverity = when {
            anomalies.any { it.severity == EvaluationSeverity.FAULT } -> EvaluationSeverity.FAULT
            anomalies.any { it.severity == EvaluationSeverity.CRITICAL } -> EvaluationSeverity.CRITICAL
            anomalies.any { it.severity == EvaluationSeverity.WARNING } -> EvaluationSeverity.WARNING
            anomalies.any { it.severity == EvaluationSeverity.INFO } -> EvaluationSeverity.INFO
            else -> EvaluationSeverity.NORMAL
        }

        // Generate Thai Summary
        val summaryTh = when (overallSeverity) {
            EvaluationSeverity.FAULT -> "ตรวจพบความผิดปกติของฮาร์ดแวร์เซนเซอร์ (${anomalies.size} รายการ) กรุณาตรวจสอบวงจรเซนเซอร์"
            EvaluationSeverity.CRITICAL -> "ตรวจพบสภาวะวิกฤต (${anomalies.size} รายการ) ที่ต้องได้รับการแก้ไขทันทีเพื่อป้องกันความเสียหายต่อเครื่องยนต์"
            EvaluationSeverity.WARNING -> "ตรวจพบค่าเซนเซอร์ที่เริ่มผิดปกติ (${anomalies.size} รายการ) ควรสังเกตอาการและตรวจสอบเพิ่มเติม"
            EvaluationSeverity.INFO -> "ข้อมูลเซนเซอร์อยู่ในสภาวะเริ่มต้นอุ่นเครื่อง หรือมีข้อมูลเชิงสังเกต"
            EvaluationSeverity.NORMAL -> "ข้อมูลเซนเซอร์ทั้งหมดผ่านการตรวจสอบกฎทางวิศวกรรม (Rule Engine Validation: PASS) ทำงานอยู่ในเกณฑ์ปกติ"
        }

        // Build AI Enrichment Context
        val aiContextBuilder = StringBuilder()
        aiContextBuilder.append("=== กฎการวินิจฉัยทางวิศวกรรม (DETERMINISTIC DIAGNOSTIC RULE ENGINE) ===\n")
        aiContextBuilder.append("สถานะการประเมินโดยรวม: $overallSeverity\n")
        aiContextBuilder.append("ความสอดคล้องของเซนเซอร์: ${if (isTelemetryPlausible) "ถูกต้องตามหลักฟิสิกส์ (Plausible)" else "พบความขัดแย้งของสัญญาณ (Implausible)"}\n")
        aiContextBuilder.append("สถานะอุณหภูมิน้ำหล่อเย็น: ${tempEval.statusLabelTh}\n")
        aiContextBuilder.append("สถานะระบบไฟฟ้า/แรงดัน: ${voltEval.statusLabelTh}\n")
        aiContextBuilder.append("สถานะระบบเชื้อเพลิง/บูสต์: ${fuelEval.statusLabelTh}\n")

        if (anomalies.isNotEmpty()) {
            aiContextBuilder.append("\n[รายการความผิดปกติที่ตรวจพบโดย Rule Engine]:\n")
            anomalies.forEachIndexed { idx, anomaly ->
                aiContextBuilder.append("${idx + 1}. [${anomaly.severity}] ${anomaly.titleTh}\n")
                aiContextBuilder.append("   - ตัวแปร: ${anomaly.parameterName} = ${anomaly.measuredValue} (เกณฑ์ปกติ: ${anomaly.expectedRange})\n")
                aiContextBuilder.append("   - รายละเอียด: ${anomaly.descriptionTh}\n")
                aiContextBuilder.append("   - คำแนะนำช่าง: ${anomaly.recommendedActionTh}\n")
            }
        } else {
            aiContextBuilder.append("\n[ผลลัพธ์]: ไม่พบความผิดปกติของตัวแปรเซนเซอร์หลัก (All Engine Safety Thresholds OK)\n")
        }

        return RuleEngineReport(
            overallSeverity = overallSeverity,
            isTelemetryPlausible = isTelemetryPlausible,
            anomalies = anomalies,
            summaryTh = summaryTh,
            aiEnrichmentContext = aiContextBuilder.toString()
        )
    }
}
