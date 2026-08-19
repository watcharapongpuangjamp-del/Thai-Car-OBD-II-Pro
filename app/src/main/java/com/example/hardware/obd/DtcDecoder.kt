package com.example.hardware.obd

import com.example.model.AppOperationMode
import com.example.model.DtcCode
import com.example.model.DtcSeverity
import com.example.model.DtcStatus
import java.util.Locale

object DtcDecoder {

    /**
     * Decodes raw Mode 03 / 07 / 0A response from ELM327 into a list of DtcCode objects.
     * Response format can be:
     * - "43 01 33 00 00 00 00" -> P0133
     * - "43 02 01 33 03 00 00" -> P0133, P0300
     * - "43 00" -> No codes
     * - "NO DATA" -> No codes
     *
     * SAE J2019 / ISO 15031-6 DTC 2-Byte Encoding:
     * Byte 1:
     *   Bits 7-6: System (00 = P (Powertrain), 01 = C (Chassis), 10 = B (Body), 11 = U (Network))
     *   Bits 5-4: First digit (00 = 0, 01 = 1, 10 = 2, 11 = 3)
     *   Bits 3-0: Second digit (Hex 0-F)
     * Byte 2:
     *   Bits 7-4: Third digit (Hex 0-F)
     *   Bits 3-0: Fourth digit (Hex 0-F)
     */
    fun decodeDtcResponse(
        rawResponse: String,
        defaultModule: String = "ECM",
        modeProvenance: AppOperationMode = AppOperationMode.REAL_HARDWARE,
        status: DtcStatus = DtcStatus.UNKNOWN
    ): List<DtcCode> {
        val normalized = ObdResponseNormalizer.normalize(rawResponse)
        if (normalized.isEmpty() ||
            normalized.contains("NO DATA", ignoreCase = true) ||
            normalized.contains("UNABLE", ignoreCase = true) ||
            normalized.contains("ERROR", ignoreCase = true)
        ) {
            return emptyList()
        }

        val tokens = normalized.split("\\s+".toRegex()).filter { it.isNotBlank() }
        if (tokens.isEmpty()) return emptyList()

        val dtcBytes = mutableListOf<Byte>()
        var startIndex = 0

        // Check for Service response ID (0x43 for Mode 03, 0x47 for Mode 07, 0x4A for Mode 0A)
        if (tokens[0].equals("43", ignoreCase = true) ||
            tokens[0].equals("47", ignoreCase = true) ||
            tokens[0].equals("4A", ignoreCase = true)
        ) {
            startIndex = 1
            // Sometimes followed by DTC count byte (e.g. 43 02 ...)
            if (tokens.size > 1 && tokens[1].length <= 2 && tokens.size % 2 == 0) {
                // If odd number of payload bytes remaining, the second token is the count byte
                startIndex = 2
            }
        }

        for (i in startIndex until tokens.size) {
            val token = tokens[i]
            if (token.length == 2) {
                val b = token.toIntOrNull(16)
                if (b != null) {
                    dtcBytes.add(b.toByte())
                }
            } else if (token.length == 4) {
                val b1 = token.substring(0, 2).toIntOrNull(16)
                val b2 = token.substring(2, 4).toIntOrNull(16)
                if (b1 != null && b2 != null) {
                    dtcBytes.add(b1.toByte())
                    dtcBytes.add(b2.toByte())
                }
            }
        }

        val resultList = mutableListOf<DtcCode>()
        for (i in 0 until dtcBytes.size - 1 step 2) {
            val b1 = dtcBytes[i].toInt() and 0xFF
            val b2 = dtcBytes[i + 1].toInt() and 0xFF

            // Both zero bytes mean end of DTC list / padding
            if (b1 == 0 && b2 == 0) continue

            val formattedCode = formatDtcCode(b1, b2)
            if (formattedCode.isNotBlank()) {
                val metadata = lookupDtcMetadata(formattedCode, defaultModule, modeProvenance, status)
                resultList.add(metadata)
            }
        }

        return resultList
    }

    private fun formatDtcCode(b1: Int, b2: Int): String {
        val systemPrefix = when ((b1 and 0xC0) shr 6) {
            0 -> "P"
            1 -> "C"
            2 -> "B"
            3 -> "U"
            else -> "P"
        }
        val firstDigit = (b1 and 0x30) shr 4
        val secondDigit = String.format("%X", b1 and 0x0F)
        val thirdDigit = String.format("%X", (b2 and 0xF0) shr 4)
        val fourthDigit = String.format("%X", b2 and 0x0F)

        return "$systemPrefix$firstDigit$secondDigit$thirdDigit$fourthDigit"
    }

    fun lookupDtcMetadata(
        code: String,
        module: String = "ECM",
        modeProvenance: AppOperationMode = AppOperationMode.REAL_HARDWARE,
        status: DtcStatus = DtcStatus.UNKNOWN
    ): DtcCode {
        val upper = code.uppercase(Locale.ROOT).trim()
        return when (upper) {
            "P0300" -> DtcCode(
                code = "P0300",
                module = module,
                descriptionEn = "Random/Multiple Cylinder Misfire Detected",
                descriptionTh = "ตรวจพบการจุดระเบิดผิดพลาดแบบสุ่ม / หลายสูบ (หัวเทียน/คอยล์จุดระเบิด)",
                severity = DtcSeverity.CRITICAL,
                modeProvenance = modeProvenance
            )
            "P0301" -> DtcCode(
                code = "P0301",
                module = module,
                descriptionEn = "Cylinder 1 Misfire Detected",
                descriptionTh = "ตรวจพบการจุดระเบิดผิดพลาดที่สูบ 1 (คอยล์/หัวเทียน)",
                severity = DtcSeverity.CRITICAL,
                modeProvenance = modeProvenance
            )
            "P0171" -> DtcCode(
                code = "P0171",
                module = module,
                descriptionEn = "System Too Lean (Bank 1)",
                descriptionTh = "ส่วนผสมน้ำมันบางเกินไป Bank 1 (อาจมีอากาศรั่วเข้าระบบหรือแรงดันน้ำมันตก)",
                severity = DtcSeverity.WARNING,
                modeProvenance = modeProvenance
            )
            "P0420" -> DtcCode(
                code = "P0420",
                module = module,
                descriptionEn = "Catalyst System Efficiency Below Threshold (Bank 1)",
                descriptionTh = "ประสิทธิภาพระบบกรองไอเสียแคทาไลติกต่ำกว่าเกณฑ์ Bank 1",
                severity = DtcSeverity.WARNING,
                modeProvenance = modeProvenance
            )
            "P0118" -> DtcCode(
                code = "P0118",
                module = module,
                descriptionEn = "Engine Coolant Temperature Circuit High",
                descriptionTh = "วงจรเซนเซอร์อุณหภูมิน้ำหล่อเย็นเครื่องยนต์แรงดันสูงผิดปกติ",
                severity = DtcSeverity.WARNING,
                modeProvenance = modeProvenance
            )
            "P0562" -> DtcCode(
                code = "P0562",
                module = module,
                descriptionEn = "System Voltage Low",
                descriptionTh = "แรงดันไฟในระบบต่ำผิดปกติ (ตรวจสอบแบตเตอรี่และไดชาร์จ)",
                severity = DtcSeverity.WARNING,
                modeProvenance = modeProvenance
            )
            "C0035" -> DtcCode(
                code = "C0035",
                module = "ABS",
                descriptionEn = "Left Front Wheel Speed Sensor Circuit Fault",
                descriptionTh = "วงจรเซนเซอร์วัดความเร็วล้อหน้าซ้ายขัดข้อง (ระบบ ABS)",
                severity = DtcSeverity.WARNING,
                modeProvenance = modeProvenance
            )
            "U0100" -> DtcCode(
                code = "U0100",
                module = "CAN Gateway",
                descriptionEn = "Lost Communication With ECM/PCM",
                descriptionTh = "ขาดการสื่อสารกับกล่องควบคุมเครื่องยนต์ ECM/PCM",
                severity = DtcSeverity.CRITICAL,
                modeProvenance = modeProvenance
            )
            "B0001" -> DtcCode(
                code = "B0001",
                module = "SRS",
                descriptionEn = "Driver Frontal Stage 1 Deployment Control",
                descriptionTh = "ระบบควบคุมการทำงานของถุงลมนิรภัยคนขับฝั่งหน้า Stage 1 ขัดข้อง",
                severity = DtcSeverity.CRITICAL,
                modeProvenance = modeProvenance
            )
            else -> DtcCode(
                code = upper,
                module = module,
                descriptionEn = "Standard OBD-II Diagnostic Fault Code",
                descriptionTh = "รหัสวิเคราะห์ปัญหามาตรฐาน OBD-II ($upper)",
                severity = if (upper.startsWith("P03") || upper.startsWith("U")) DtcSeverity.CRITICAL else DtcSeverity.WARNING,
                modeProvenance = modeProvenance
            )
        }
    }
}
