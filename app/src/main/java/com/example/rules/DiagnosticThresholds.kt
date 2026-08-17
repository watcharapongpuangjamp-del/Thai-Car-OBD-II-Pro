package com.example.rules

object EngineTemperatureThresholds {
    const val SENSOR_SHORT_LOW_C = -35
    const val SENSOR_SHORT_HIGH_C = 135
    const val COLD_ENGINE_MAX_C = 65
    const val NORMAL_MIN_C = 80
    const val NORMAL_MAX_C = 95
    const val ELEVATED_WARNING_C = 98
    const val SEVERE_OVERHEAT_C = 105

    fun evaluate(coolantTempC: Int?): TemperatureEvaluation {
        if (coolantTempC == null) {
            return TemperatureEvaluation(
                severity = EvaluationSeverity.NORMAL,
                statusLabelTh = "ไม่มีข้อมูลอุณหภูมิน้ำหล่อเย็น"
            )
        }

        // 1. Sensor Hardware Short/Open Circuit
        if (coolantTempC <= SENSOR_SHORT_LOW_C || coolantTempC >= SENSOR_SHORT_HIGH_C) {
            val anomaly = DiagnosticAnomaly(
                category = DiagnosticCategory.ENGINE_TEMPERATURE,
                severity = EvaluationSeverity.FAULT,
                parameterName = "Coolant Temperature (ECT)",
                measuredValue = "$coolantTempC °C",
                expectedRange = "-30°C ถึง 130°C",
                titleTh = "เซนเซอร์อุณหภูมิน้ำหล่อเย็นชำรุด (ECT Sensor Open/Short)",
                descriptionTh = "ค่าอุณหภูมิที่วัดได้ ($coolantTempC °C) อยู่นอกช่วงพิกัดทางกายภาพ บ่งชี้ว่าวงจรเซนเซอร์ขาดหรือลัดวงจร",
                potentialCausesTh = listOf("ปลั๊ก ECT หลุดหรือสายไฟขาด", "เซนเซอร์ ECT เสื่อมสภาพ/ช็อตลงกราวด์", "วงจรกล่อง ECU มีปัญหา"),
                recommendedActionTh = "ตรวจสอบความต้านทานของเซนเซอร์ ECT และสายไฟก่อนสตาร์ทรถ"
            )
            return TemperatureEvaluation(EvaluationSeverity.FAULT, "เซนเซอร์ ECT ชำรุด", anomaly)
        }

        // 2. Severe Overheating
        if (coolantTempC >= SEVERE_OVERHEAT_C) {
            val anomaly = DiagnosticAnomaly(
                category = DiagnosticCategory.ENGINE_TEMPERATURE,
                severity = EvaluationSeverity.CRITICAL,
                parameterName = "Coolant Temperature (ECT)",
                measuredValue = "$coolantTempC °C",
                expectedRange = "$NORMAL_MIN_C°C - $NORMAL_MAX_C°C",
                titleTh = "เครื่องยนต์มีความร้อนสูงวิกฤต (Severe Overheating)",
                descriptionTh = "อุณหภูมิน้ำหล่อเย็นสูงเกิน $SEVERE_OVERHEAT_C °C เสี่ยงต่อฝาสูบโก่งและปะเก็นแตก",
                potentialCausesTh = listOf("วาล์วน้ำ (Thermostat) ไม่เปิด", "พัดลมระบายความร้อนหม้อน้ำไม่ทำงาน", "น้ำหล่อเย็นแห้ง/หม้อน้ำรั่ว", "ปั๊มน้ำชำรุด"),
                recommendedActionTh = "ดับเครื่องยนต์ทันที รอให้เครื่องเย็นลง และตรวจเช็กระดับน้ำหล่อเย็นและพัดลมหม้อน้ำ"
            )
            return TemperatureEvaluation(EvaluationSeverity.CRITICAL, "ความร้อนสูงระดับวิกฤต ($coolantTempC°C)", anomaly)
        }

        // 3. Elevated Warning
        if (coolantTempC >= ELEVATED_WARNING_C) {
            val anomaly = DiagnosticAnomaly(
                category = DiagnosticCategory.ENGINE_TEMPERATURE,
                severity = EvaluationSeverity.WARNING,
                parameterName = "Coolant Temperature (ECT)",
                measuredValue = "$coolantTempC °C",
                expectedRange = "$NORMAL_MIN_C°C - $NORMAL_MAX_C°C",
                titleTh = "อุณหภูมิเครื่องยนต์เริ่มสูงผิดปกติ (Elevated Temperature)",
                descriptionTh = "อุณหภูมิสูงกว่าพิกัดควบคุมปกติ ($coolantTempC °C) อาจเกิดจากภาระโหลดหนักหรือระบบระบายความร้อนเริ่มเสื่อม",
                potentialCausesTh = listOf("ครีบหม้อน้ำอุดตัน/สกปรก", "พัดลมหม้อน้ำหมุนรอบต่ำ", "ขับขี่ขึ้นทางลาดชันหรือการจราจรติดขัดจัด"),
                recommendedActionTh = "หลีกเลี่ยงการเร่งรอบสูง สังเกตพัดลมระบายความร้อน และเช็กระดับน้ำพักหม้อน้ำ"
            )
            return TemperatureEvaluation(EvaluationSeverity.WARNING, "อุณหภูมิเริ่มสูง ($coolantTempC°C)", anomaly)
        }

        // 4. Cold Engine State
        if (coolantTempC < COLD_ENGINE_MAX_C) {
            return TemperatureEvaluation(
                severity = EvaluationSeverity.INFO,
                statusLabelTh = "เครื่องยนต์ยังไม่ถึงอุณหภูมิทำงานปกติ (Cold / Warming Up)"
            )
        }

        // 5. Normal
        return TemperatureEvaluation(
            severity = EvaluationSeverity.NORMAL,
            statusLabelTh = "อุณหภูมิทำงานปกติ ($coolantTempC°C)"
        )
    }
}

object VoltageThresholds {
    const val SENSOR_FAULT_LOW_V = 5.0f
    const val SENSOR_FAULT_HIGH_V = 18.0f
    const val DEAD_BATTERY_V = 11.5f
    const val UNDERVOLTAGE_WARNING_V = 12.0f
    const val RUNNING_CHARGING_MIN_V = 13.2f
    const val RUNNING_CHARGING_MAX_V = 14.8f
    const val OVERVOLTAGE_HAZARD_V = 15.2f

    fun evaluate(voltage: Float?, rpm: Int?): VoltageEvaluation {
        if (voltage == null) {
            return VoltageEvaluation(
                severity = EvaluationSeverity.NORMAL,
                statusLabelTh = "ไม่มีข้อมูลแรงดันไฟฟ้า"
            )
        }

        val isEngineRunning = (rpm ?: 0) > 400

        // 1. Hardware Sensor Plausibility Fault
        if (voltage <= SENSOR_FAULT_LOW_V || voltage >= SENSOR_FAULT_HIGH_V) {
            val anomaly = DiagnosticAnomaly(
                category = DiagnosticCategory.ELECTRICAL_VOLTAGE,
                severity = EvaluationSeverity.FAULT,
                parameterName = "System Voltage",
                measuredValue = "$voltage V",
                expectedRange = "9.0V - 16.0V",
                titleTh = "แรงดันไฟฟ้าอยู่นอกพิกัดทางกายภาพ (Voltage Sensor Fault)",
                descriptionTh = "ระบบตรวจพบแรงดันไฟ $voltage V ซึ่งผิดปกติอย่างร้ายแรงหรือวงจรวัดแรงดันเสียหาย",
                potentialCausesTh = listOf("ขั้วแบตเตอรี่หลวมจัดหรือมีขี้เกลือ", "สายกราวด์ตัวถังหลวม", "วงจรแปลงสัญญาณ ELM327 คลาดเคลื่อน"),
                recommendedActionTh = "ตรวจสอบขั้วแบตเตอรี่และสายดินหลักทันที"
            )
            return VoltageEvaluation(EvaluationSeverity.FAULT, "เซนเซอร์แรงดันไฟชำรุด", anomaly)
        }

        // 2. Overvoltage Hazard
        if (voltage >= OVERVOLTAGE_HAZARD_V) {
            val anomaly = DiagnosticAnomaly(
                category = DiagnosticCategory.ELECTRICAL_VOLTAGE,
                severity = EvaluationSeverity.CRITICAL,
                parameterName = "Alternator Voltage",
                measuredValue = "$voltage V",
                expectedRange = "$RUNNING_CHARGING_MIN_V V - $RUNNING_CHARGING_MAX_V V",
                titleTh = "ไดชาร์จจ่ายไฟเกินพิกัดวิกฤต (Overvoltage Hazard)",
                descriptionTh = "แรงดันไฟสูงถึง $voltage V เสี่ยงทำให้กล่อง ECU, กล่องฟิวส์ และอุปกรณ์อิเล็กทรอนิกส์เสียหาย",
                potentialCausesTh = listOf("ไอซีเรกูเลเตอร์ในไดชาร์จ (Voltage Regulator) ชำรุด", "ไดชาร์จทำงานผิดปกติ"),
                recommendedActionTh = "หยุดใช้งานรถและนำไดชาร์จเข้าตรวจสอบทันที"
            )
            return VoltageEvaluation(EvaluationSeverity.CRITICAL, "ไดชาร์จจ่ายไฟเกิน ($voltage V)", anomaly)
        }

        // 3. Engine Running but Voltage Low (Alternator not charging)
        if (isEngineRunning && voltage < RUNNING_CHARGING_MIN_V) {
            val severity = if (voltage < DEAD_BATTERY_V) EvaluationSeverity.CRITICAL else EvaluationSeverity.WARNING
            val anomaly = DiagnosticAnomaly(
                category = DiagnosticCategory.ELECTRICAL_VOLTAGE,
                severity = severity,
                parameterName = "Charging Voltage",
                measuredValue = "$voltage V",
                expectedRange = "$RUNNING_CHARGING_MIN_V V - $RUNNING_CHARGING_MAX_V V",
                titleTh = if (severity == EvaluationSeverity.CRITICAL) "ไดชาร์จไม่ประจุไฟ / แบตเตอรี่วิกฤต" else "ไดชาร์จจ่ายไฟต่ำกว่าเกณฑ์ (Undercharging)",
                descriptionTh = "ขณะเครื่องยนต์ทำงานรอบ ${(rpm ?: 0)} RPM แรงดันไฟเพียง $voltage V ซึ่งต่ำกว่าเกณฑ์ชาร์จ ($RUNNING_CHARGING_MIN_V V)",
                potentialCausesTh = listOf("สายพานไดชาร์จหย่อนหรือขาด", "ไดชาร์จ (Alternator) เสื่อมสภาพ", "แบตเตอรี่เสื่อมเก็บไฟไม่อยู่"),
                recommendedActionTh = "ตรวจเช็กสายพานหน้าเครื่องและนำไดชาร์จไปตรวจวัดกระแสแอมป์"
            )
            return VoltageEvaluation(severity, "ระบบชาร์จไฟต่ำ ($voltage V)", anomaly)
        }

        // 4. Engine Off but Battery Weak
        if (!isEngineRunning && voltage < UNDERVOLTAGE_WARNING_V) {
            val anomaly = DiagnosticAnomaly(
                category = DiagnosticCategory.ELECTRICAL_VOLTAGE,
                severity = EvaluationSeverity.WARNING,
                parameterName = "Resting Battery Voltage",
                measuredValue = "$voltage V",
                expectedRange = "12.4V - 12.8V",
                titleTh = "แรงดันแบตเตอรี่ตอนดับเครื่องต่ำ (Weak Battery)",
                descriptionTh = "แรงดันแบตเตอรี่ $voltage V ต่ำกว่ามาตรฐาน เสี่ยงสตาร์ทติดยาก",
                potentialCausesTh = listOf("แบตเตอรี่มีอายุการใช้งานนาน", "มีกระแสไฟฟ้ารั่วไหลในระบบ (Parasitic Draw)", "จอดรถทิ้งไว้นาน"),
                recommendedActionTh = "นำแบตเตอรี่ไปชาร์จไฟหรือตรวจวัดค่า CCA"
            )
            return VoltageEvaluation(EvaluationSeverity.WARNING, "แบตเตอรี่อ่อน ($voltage V)", anomaly)
        }

        return VoltageEvaluation(EvaluationSeverity.NORMAL, "แรงดันไฟปกติ ($voltage V)")
    }
}

object FuelAndLoadThresholds {
    const val IDLE_FUEL_RATE_HIGH_LPH = 3.5f
    const val MAX_SAFE_BOOST_BAR = 1.8f
    const val SEVERE_BOOST_HAZARD_BAR = 2.2f
    const val HIGH_ENGINE_LOAD_PCT = 85
    const val LOW_THROTTLE_PCT = 15

    fun evaluate(
        throttlePct: Int?,
        loadPct: Int?,
        boostBar: Float?,
        fuelRateLph: Float?,
        speedKmh: Int?,
        rpm: Int?
    ): FuelAndLoadEvaluation {
        val anomalies = mutableListOf<DiagnosticAnomaly>()
        val isIdle = (speedKmh ?: 0) == 0 && (rpm ?: 0) in 500..1000

        // 1. Idle Over-fueling / Rich Idle
        if (isIdle && fuelRateLph != null && fuelRateLph > IDLE_FUEL_RATE_HIGH_LPH) {
            anomalies.add(
                DiagnosticAnomaly(
                    category = DiagnosticCategory.FUEL_AND_LOAD,
                    severity = EvaluationSeverity.WARNING,
                    parameterName = "Idle Fuel Rate",
                    measuredValue = "$fuelRateLph L/h",
                    expectedRange = "0.6 - 2.0 L/h",
                    titleTh = "อัตราสิ้นเปลืองน้ำมันขณะเดินเบาสูงผิดปกติ (High Idle Fuel Rate)",
                    descriptionTh = "เครื่องยนต์ใช้น้ำมันขณะจอดเดินเบาสูงถึง $fuelRateLph L/h บ่งชี้ว่าส่วนผสมหนาเกินไป",
                    potentialCausesTh = listOf("หัวฉีดน้ำมันรั่ว/ปิดไม่สนิท", "เซนเซอร์ O2 อ่านค่าเพี้ยน", "เรกูเลเตอร์แรงดันน้ำมันเชื้อเพลิงเสีย"),
                    recommendedActionTh = "ตรวจเช็กค่า Long Term Fuel Trim (LTFT) และล้างหัวฉีดน้ำมัน"
                )
            )
        }

        // 2. Overboost Hazard
        if (boostBar != null && boostBar >= MAX_SAFE_BOOST_BAR) {
            val isCritical = boostBar >= SEVERE_BOOST_HAZARD_BAR
            anomalies.add(
                DiagnosticAnomaly(
                    category = DiagnosticCategory.FUEL_AND_LOAD,
                    severity = if (isCritical) EvaluationSeverity.CRITICAL else EvaluationSeverity.WARNING,
                    parameterName = "Boost Pressure",
                    measuredValue = "$boostBar bar",
                    expectedRange = "0.0 - 1.5 bar",
                    titleTh = if (isCritical) "แรงดันบูสต์เทอร์โบสูงเกินเกณฑ์วิกฤต (Overboost Hazard)" else "แรงดันบูสต์สูงกว่าพิกัดมาตรฐาน",
                    descriptionTh = "แรงดันบูสต์ $boostBar bar อาจทำให้กำลังอัดในห้องเผาไหม้สูงเกินขีดจำกัดของลูกสูบและท่อไอดี",
                    potentialCausesTh = listOf("เวสต์เกต (Wastegate) ค้างหรือสายลมเวสต์เกตหลุด", "โซลินอยด์คุมบูสต์เสีย", "แมปจูนนิ่งบูสต์สูงเกินพิกัด"),
                    recommendedActionTh = "ตรวจสอบการทำงานของก้านเวสต์เกตและวาล์วคุมบูสต์ทันที"
                )
            )
        }

        // 3. Engine Load vs Throttle Plausibility Mismatch
        if (loadPct != null && throttlePct != null) {
            if (loadPct >= HIGH_ENGINE_LOAD_PCT && throttlePct <= LOW_THROTTLE_PCT && (rpm ?: 0) > 600) {
                anomalies.add(
                    DiagnosticAnomaly(
                        category = DiagnosticCategory.FUEL_AND_LOAD,
                        severity = EvaluationSeverity.WARNING,
                        parameterName = "Load vs Throttle",
                        measuredValue = "Load: $loadPct%, Throttle: $throttlePct%",
                        expectedRange = "Load ควรแปรผันตรงตาม Throttle",
                        titleTh = "ภาระโหลดเครื่องยนต์สูงผิดปกติเทียบกับตำแหน่งลิ้นปีกผีเสื้อ (High Load Imbalance)",
                        descriptionTh = "เครื่องยนต์มีภาระโหลดสูงถึง $loadPct% ในขณะที่ลิ้นเร่งเปิดเพียง $throttlePct%",
                        potentialCausesTh = listOf("ระบบเกียร์ลื่น/ทอร์กคอนเวอร์เตอร์หน่วงผิดปกติ", "ระบบเบรกติดขัด (Brake Drag)", "เซนเซอร์ MAP/MAF ส่งสัญญาณเพี้ยน"),
                        recommendedActionTh = "ตรวจสอบระบบเบรกว่ามีล้อติดหรือไม่ และเช็กแรงดันน้ำมันเกียร์"
                    )
                )
            }
        }

        val worstSeverity = anomalies.maxByOrNull { it.severity.ordinal }?.severity ?: EvaluationSeverity.NORMAL
        val statusLabel = when (worstSeverity) {
            EvaluationSeverity.CRITICAL -> "ระบบเชื้อเพลิง/บูสต์มีภาวะวิกฤต"
            EvaluationSeverity.WARNING -> "พบความผิดปกติในระบบเชื้อเพลิงหรือโหลด (${anomalies.size} รายการ)"
            EvaluationSeverity.FAULT -> "เซนเซอร์ระบบเชื้อเพลิงชำรุด"
            else -> "ระบบเชื้อเพลิงและภาระเครื่องยนต์ปกติ"
        }

        return FuelAndLoadEvaluation(worstSeverity, statusLabel, anomalies)
    }
}
