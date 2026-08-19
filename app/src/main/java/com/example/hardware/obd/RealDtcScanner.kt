package com.example.hardware.obd

import android.util.Log
import com.example.model.DtcCode
import com.example.model.DtcScanResult
import com.example.model.DtcScanStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RealDtcScanner(
    private val elmDriver: IElm327Driver
) {

    companion object {
        private const val TAG = "RealDtcScanner"
    }

    /**
     * Performs a comprehensive DTC Scan with Dynamic ECU Discovery and Mode 03 / 07 / 0A support.
     */
    suspend fun performCompleteDtcScan(): DtcScanResult = withContext(Dispatchers.IO) {
        val dtcResults = mutableListOf<DtcCode>()
        val seenCodes = mutableSetOf<String>()
        var successfulQueries = 0
        var totalQueriesAttempted = 0

        try {
            Log.i(TAG, "Enabling headers (AT H1) for dynamic ECU discovery...")
            elmDriver.sendRawCommand("ATH1", 1000)

            // Discover active ECU response headers dynamically (e.g. 7E8, 7E9, 7EA, etc.)
            val discoveredHeaders = mutableSetOf<String>()
            try {
                val pingResp = elmDriver.sendRawCommand("0100", 2000)
                val lines = pingResp.split("\n", "\r").map { it.trim() }.filter { it.isNotEmpty() }
                for (line in lines) {
                    // Typical CAN response header format: e.g. "7E8 06 41 00 BE 3F B8 11"
                    val parts = line.split("\\s+".toRegex())
                    if (parts.size >= 2 && parts[0].length == 3 && parts[0].all { it.isLetterOrDigit() }) {
                        discoveredHeaders.add(parts[0].uppercase())
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Dynamic header discovery ping failed: ${e.message}")
            }

            // If no headers discovered dynamically, fallback to standard ECM header 7E8 or general broadcast
            val targetHeaders = if (discoveredHeaders.isNotEmpty()) {
                discoveredHeaders.toList()
            } else {
                listOf("7E8") // Default ECU header
            }

            Log.i(TAG, "Discovered ECU Response Headers: $targetHeaders")

            val modes = listOf("03", "07", "0A")

            for (header in targetHeaders) {
                val assignedModule = resolveModuleFromHeader(header)
                
                // Set target request header corresponding to response header (e.g., 7E8 -> 7E0)
                val reqHeader = computeRequestHeader(header)
                try {
                    elmDriver.sendRawCommand("ATSH $reqHeader", 1000)
                } catch (_: Exception) {}

                for (mode in modes) {
                    totalQueriesAttempted++
                    try {
                        val resp = elmDriver.sendRawCommand(mode, 3000)
                        if (resp.isBlank() || resp.contains("TIMEOUT", true) || resp.contains("ERROR", true) || resp.contains("NO DATA", true)) {
                            Log.w(TAG, "Mode $mode query on header $header returned invalid or timeout: $resp")
                        } else {
                            successfulQueries++
                            val codes = DtcDecoder.decodeDtcResponse(resp, defaultModule = assignedModule)
                            codes.forEach { code ->
                                if (seenCodes.add(code.code)) {
                                    dtcResults.add(code)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Exception querying Mode $mode on header $header: ${e.message}")
                    }
                }
            }

            // Reset headers back to default broadcast 7DF
            try {
                elmDriver.sendRawCommand("ATSH 7DF", 500)
                elmDriver.sendRawCommand("ATH0", 500)
            } catch (_: Exception) {}

            val status = when {
                successfulQueries == 0 && totalQueriesAttempted > 0 -> DtcScanStatus.FAILED("All ECU DTC queries failed or timed out")
                successfulQueries < totalQueriesAttempted && dtcResults.isNotEmpty() -> DtcScanStatus.PARTIAL
                successfulQueries < totalQueriesAttempted && dtcResults.isEmpty() -> DtcScanStatus.NO_CODES
                dtcResults.isEmpty() -> DtcScanStatus.NO_CODES
                else -> DtcScanStatus.SUCCESS
            }

            Log.i(TAG, "DTC Scan completed. Total DTCs: ${dtcResults.size}, Status: $status")
            return@withContext DtcScanResult(dtcResults, status)

        } catch (e: Exception) {
            Log.e(TAG, "Exception during comprehensive dynamic DTC scan: ${e.message}", e)
            return@withContext DtcScanResult(emptyList(), DtcScanStatus.FAILED(e.message ?: "Unknown error"))
        }
    }

    private fun resolveModuleFromHeader(header: String): String {
        return when (header.uppercase()) {
            "7E8" -> "ECM"
            "7E9" -> "TCM"
            "7EA" -> "ABS"
            "7EB" -> "SRS"
            "7EC" -> "BCM"
            else -> "UNKNOWN_ECU"
        }
    }

    private fun computeRequestHeader(responseHeader: String): String {
        return when (responseHeader.uppercase()) {
            "7E8" -> "7E0"
            "7E9" -> "7E1"
            "7EA" -> "7E2"
            "7EB" -> "7E3"
            "7EC" -> "7E4"
            else -> {
                // Generic conversion: subtract 8 from last hex char if possible, e.g. 7E8 -> 7E0
                try {
                    val base = responseHeader.toInt(16)
                    val req = base - 8
                    String.format("%03X", req)
                } catch (_: Exception) {
                    "7DF"
                }
            }
        }
    }

    /**
     * Clears Diagnostic Trouble Codes (Mode 04 - Clear Codes & Reset MIL)
     */
    suspend fun clearDtcCodes(): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "Sending Mode 04 (Clear DTC Codes & Reset MIL)...")
            val resp = elmDriver.sendRawCommand("04", timeoutMs = 4000)
            val normalized = ObdResponseNormalizer.normalize(resp, "04")
            
            // Strict check: must not be TIMEOUT, ERROR, NO DATA, and must contain positive confirmation (44 or OK)
            if (resp == "TIMEOUT" || resp.contains("ERROR", true) || resp.contains("NO DATA", true) ||
                (!normalized.contains("44") && !normalized.contains("OK"))) {
                Log.e(TAG, "Mode 04 clear failed: $resp")
                Result.failure(Exception("ECU failed to clear DTCs: $resp"))
            } else {
                Log.i(TAG, "DTC Clear command 04 confirmed successfully")
                Result.success(true)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear DTC codes", e)
            Result.failure(e)
        }
    }
}

