package com.example.export

import android.util.Log
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.util.zip.GZIPOutputStream

@Serializable
data class TelemetrySample(
    val timestamp: Long,
    val rpm: Int?,
    val speed: Int?,
    val coolantTemp: Float?,
    val throttlePos: Float?,
    val engineLoad: Float?,
    val intakeTemp: Float?,
    val fuelRateLph: Float?,
    val voltage: Float?
)

@Serializable
data class RawCommunicationLogEntry(
    val timestamp: Long,
    val direction: String, // "TX" or "RX"
    val rawHexOrText: String,
    val protocolId: String
)

@Serializable
data class DiagnosticExportPackage(
    val exportTimestamp: Long = System.currentTimeMillis(),
    val appVersion: String = "1.0.0",
    val telemetryHistory: List<TelemetrySample>,
    val communicationLogs: List<RawCommunicationLogEntry>,
    val metadata: Map<String, String> = emptyMap()
)

class DiagnosticJsonExporter {
    companion object {
        private const val TAG = "DiagnosticJsonExporter"
        private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }
    }

    /**
     * Packages TelemetryHistory and RawCommunicationLog into a compressed GZIP JSON format
     * and writes it to the provided output stream.
     */
    fun exportCompressedJson(
        telemetryHistory: List<TelemetrySample>,
        communicationLogs: List<RawCommunicationLogEntry>,
        metadata: Map<String, String> = emptyMap(),
        outputStream: OutputStream
    ): Result<Unit> {
        return try {
            val exportPackage = DiagnosticExportPackage(
                telemetryHistory = telemetryHistory,
                communicationLogs = communicationLogs,
                metadata = metadata
            )
            val jsonString = json.encodeToString(exportPackage)
            
            GZIPOutputStream(outputStream).use { gzipOut ->
                gzipOut.write(jsonString.toByteArray(Charsets.UTF_8))
                gzipOut.flush()
            }
            Log.i(TAG, "Successfully exported ${telemetryHistory.size} telemetry samples and ${communicationLogs.size} log entries to compressed JSON.")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to export compressed diagnostic package: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Helper to export directly to a target File.
     */
    fun exportCompressedJsonToFile(
        telemetryHistory: List<TelemetrySample>,
        communicationLogs: List<RawCommunicationLogEntry>,
        metadata: Map<String, String> = emptyMap(),
        destinationFile: File
    ): Result<File> {
        return try {
            FileOutputStream(destinationFile).use { fos ->
                val result = exportCompressedJson(telemetryHistory, communicationLogs, metadata, fos)
                if (result.isSuccess) {
                    Result.success(destinationFile)
                } else {
                    Result.failure(result.exceptionOrNull() ?: RuntimeException("Unknown export error"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write compressed JSON to file ${destinationFile.absolutePath}: ${e.message}", e)
            Result.failure(e)
        }
    }
}
