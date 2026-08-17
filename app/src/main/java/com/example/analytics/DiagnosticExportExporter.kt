package com.example.analytics

import com.example.model.DataExportPayload
import com.example.model.DtcCode
import com.example.model.LiveSensorData
import com.example.model.TripSummary
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DiagnosticExportExporter {

    fun generateCsvLog(
        samples: List<LiveSensorData>,
        vehicleVin: String?
    ): String {
        val sb = StringBuilder()
        sb.append("# Thai OBD-II Pro - Diagnostic Telemetry Log\n")
        sb.append("# Vehicle VIN: ${vehicleVin ?: "N/A"}\n")
        sb.append("# Export Time: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}\n")
        sb.append("Timestamp,Mode,RPM,Speed_kmh,CoolantTemp_C,Battery_V,Boost_bar,FuelRate_Lph,Throttle_pct,Load_pct,Latency_ms,Status\n")

        for (sample in samples) {
            sb.append("${System.currentTimeMillis()},")
            sb.append("${sample.mode.name},")
            sb.append("${sample.rpm ?: ""},")
            sb.append("${sample.speedKmh ?: ""},")
            sb.append("${sample.coolantTempC ?: ""},")
            sb.append("${sample.batteryVoltage ?: ""},")
            sb.append("${sample.boostPressureBar ?: ""},")
            sb.append("${sample.fuelRateLph ?: ""},")
            sb.append("${sample.throttlePosPercent ?: ""},")
            sb.append("${sample.engineLoadPercent ?: ""},")
            sb.append("${sample.latencyMs},")
            sb.append("\"${sample.statusMessage}\"\n")
        }
        return sb.toString()
    }

    fun generateDtcReportCsv(
        dtcs: List<DtcCode>,
        vehicleVin: String?
    ): String {
        val sb = StringBuilder()
        sb.append("# Thai OBD-II Pro - Diagnostic Trouble Codes (DTC) Report\n")
        sb.append("# Vehicle VIN: ${vehicleVin ?: "N/A"}\n")
        sb.append("# Total Codes: ${dtcs.size}\n")
        sb.append("DTC_Code,Module,Severity,Description_TH,Description_EN,Data_Provenance,Timestamp\n")

        for (dtc in dtcs) {
            sb.append("${dtc.code},")
            sb.append("${dtc.module},")
            sb.append("${dtc.severity.name},")
            sb.append("\"${dtc.descriptionTh}\",")
            sb.append("\"${dtc.descriptionEn}\",")
            sb.append("${dtc.modeProvenance.name},")
            sb.append("${dtc.timestamp}\n")
        }
        return sb.toString()
    }

    fun calculateSha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(input.toByteArray(Charsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }
}
