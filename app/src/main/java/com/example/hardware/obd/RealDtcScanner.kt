package com.example.hardware.obd

import android.util.Log
import com.example.model.DtcCode
import com.example.model.DtcScanResult
import com.example.model.DtcScanStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class EcuModule(
    val id: String,
    val name: String,
    val requestHeader: String,
    val responseHeader: String,
    val txCommand: String = "03"
)

class RealDtcScanner(
    private val elmDriver: IElm327Driver
) {

    companion object {
        private const val TAG = "RealDtcScanner"
    }

    private val supportedModules = listOf(
        EcuModule("ECM", "Engine Control Module", "7E0", "7E8", "03"),
        EcuModule("TCM", "Transmission Control Module", "7E1", "7E9", "03"),
        EcuModule("ABS", "Anti-Lock Braking System", "7E2", "7EA", "03"),
        EcuModule("SRS", "Supplemental Restraint System (Airbag)", "7E3", "7EB", "03"),
        EcuModule("BCM", "Body Control Module", "7E4", "7EC", "03")
    )

    /**
     * Performs a comprehensive DTC Scan and returns DtcScanResult.
     */
    suspend fun performCompleteDtcScan(): DtcScanResult = withContext(Dispatchers.IO) {
        val dtcResults = mutableListOf<DtcCode>()
        val seenCodes = mutableSetOf<String>()
        var errorsEncountered = false

        try {
            Log.i(TAG, "Starting Global Mode 03 DTC Query...")
            // 1. Standard Mode 03 (Broadcast)
            val mode03Raw = elmDriver.sendRawCommand("03", timeoutMs = 3000)
            if (mode03Raw == "TIMEOUT") {
                errorsEncountered = true
            } else {
                val mode03Codes = DtcDecoder.decodeDtcResponse(mode03Raw, defaultModule = "ECM")
                mode03Codes.forEach {
                    if (seenCodes.add(it.code)) {
                        dtcResults.add(it)
                    }
                }
            }

            // (Simplified pending/permanent logic)
            // ... (Add similar error handling for 07, 0A queries)

            // 4. Targeted CAN Header queries for TCM, ABS, SRS modules
            for (module in supportedModules) {
                if (module.id == "ECM") continue

                try {
                    elmDriver.sendRawCommand("ATSH ${module.requestHeader}", timeoutMs = 1000)
                    val modResponse = elmDriver.sendRawCommand(module.txCommand, timeoutMs = 2000)
                    
                    if (modResponse != "TIMEOUT" && !modResponse.contains("ERROR")) {
                        val moduleCodes = DtcDecoder.decodeDtcResponse(modResponse, defaultModule = module.id)
                        moduleCodes.forEach {
                            if (seenCodes.add(it.code)) {
                                dtcResults.add(it)
                            }
                        }
                    } else {
                        errorsEncountered = true
                    }
                } catch (e: Exception) {
                    errorsEncountered = true
                    Log.d(TAG, "Module ${module.name} (${module.id}) not responding: ${e.message}")
                } finally {
                    try {
                        elmDriver.sendRawCommand("ATSH 7DF", timeoutMs = 500)
                    } catch (_: Exception) {}
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Exception during comprehensive DTC scan: ${e.message}", e)
            return@withContext DtcScanResult(emptyList(), DtcScanStatus.FAILED(e.message ?: "Unknown error"))
        }

        val status = when {
            errorsEncountered && dtcResults.isNotEmpty() -> DtcScanStatus.PARTIAL
            errorsEncountered -> DtcScanStatus.FAILED("Scan failed or timed out in some modules")
            dtcResults.isEmpty() -> DtcScanStatus.NO_CODES
            else -> DtcScanStatus.SUCCESS
        }

        Log.i(TAG, "DTC Scan completed. Total DTCs: ${dtcResults.size}, Status: $status")
        return@withContext DtcScanResult(dtcResults, status)
    }

    /**
     * Clears Diagnostic Trouble Codes (Mode 04 - Clear Codes & Reset MIL)
     */
    suspend fun clearDtcCodes(): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "Sending Mode 04 (Clear DTC Codes)...")
            val resp = elmDriver.sendRawCommand("04", timeoutMs = 4000)
            
            // Fix: Check for TIMEOUT or ERROR explicitly
            if (resp == "TIMEOUT" || resp.contains("ERROR")) {
                Log.e(TAG, "Mode 04 failed: $resp")
                Result.failure(Exception("ECU failed to clear DTCs: $resp"))
            } else {
                Log.i(TAG, "DTC Clear command 04 confirmed")
                Result.success(true)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear DTC codes", e)
            Result.failure(e)
        }
    }
}
