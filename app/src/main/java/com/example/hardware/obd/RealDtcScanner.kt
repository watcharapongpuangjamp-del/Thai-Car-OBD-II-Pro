package com.example.hardware.obd

import android.util.Log
import com.example.model.DtcCode
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
    private val elmDriver: Elm327Driver
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
     * Performs a comprehensive DTC Scan:
     * 1. Mode 03 (Confirmed Stored DTCs)
     * 2. Mode 07 (Pending DTCs)
     * 3. Mode 0A (Permanent DTCs)
     * 4. Multi-ECU header addressing for TCM/ABS/SRS modules
     */
    suspend fun performCompleteDtcScan(): List<DtcCode> = withContext(Dispatchers.IO) {
        val dtcResults = mutableListOf<DtcCode>()
        val seenCodes = mutableSetOf<String>()

        try {
            Log.i(TAG, "Starting Global Mode 03 DTC Query...")
            // 1. Standard Mode 03 (Broadcast)
            val mode03Raw = elmDriver.sendRawCommand("03", timeoutMs = 3000)
            val mode03Codes = DtcDecoder.decodeDtcResponse(mode03Raw, defaultModule = "ECM")
            mode03Codes.forEach {
                if (seenCodes.add(it.code)) {
                    dtcResults.add(it)
                }
            }

            // 2. Pending DTCs (Mode 07)
            val mode07Raw = elmDriver.sendRawCommand("07", timeoutMs = 2500)
            val mode07Codes = DtcDecoder.decodeDtcResponse(mode07Raw, defaultModule = "ECM")
            mode07Codes.forEach {
                if (seenCodes.add(it.code)) {
                    dtcResults.add(it.copy(descriptionTh = "${it.descriptionTh} [Pending]"))
                }
            }

            // 3. Permanent DTCs (Mode 0A)
            val mode0aRaw = elmDriver.sendRawCommand("0A", timeoutMs = 2500)
            val mode0aCodes = DtcDecoder.decodeDtcResponse(mode0aRaw, defaultModule = "ECM")
            mode0aCodes.forEach {
                if (seenCodes.add(it.code)) {
                    dtcResults.add(it.copy(descriptionTh = "${it.descriptionTh} [Permanent]"))
                }
            }

            // 4. Targeted CAN Header queries for TCM, ABS, SRS modules
            for (module in supportedModules) {
                if (module.id == "ECM") continue // Already queried globally

                try {
                    // Set CAN Header (ATSH)
                    elmDriver.sendRawCommand("ATSH ${module.requestHeader}", timeoutMs = 1000)
                    val modResponse = elmDriver.sendRawCommand(module.txCommand, timeoutMs = 2000)
                    val moduleCodes = DtcDecoder.decodeDtcResponse(modResponse, defaultModule = module.id)
                    moduleCodes.forEach {
                        if (seenCodes.add(it.code)) {
                            dtcResults.add(it)
                        }
                    }
                } catch (e: Exception) {
                    Log.d(TAG, "Module ${module.name} (${module.id}) not responding: ${e.message}")
                } finally {
                    // Reset Header to default (ATSH 7DF / Auto)
                    try {
                        elmDriver.sendRawCommand("ATSH 7DF", timeoutMs = 500)
                    } catch (_: Exception) {}
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Exception during comprehensive DTC scan: ${e.message}", e)
        }

        Log.i(TAG, "DTC Scan completed. Total DTCs discovered: ${dtcResults.size}")
        return@withContext dtcResults
    }

    /**
     * Clears Diagnostic Trouble Codes (Mode 04 - Clear Codes & Reset MIL)
     */
    suspend fun clearDtcCodes(): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "Sending Mode 04 (Clear DTC Codes and Reset Check Engine MIL)...")
            val resp = elmDriver.sendRawCommand("04", timeoutMs = 4000)
            val clean = ObdResponseNormalizer.normalize(resp, "04")

            if (clean.contains("44") || clean.contains("OK") || clean.isEmpty()) {
                Log.i(TAG, "DTC Clear command 04 confirmed by ECU")
                Result.success(true)
            } else {
                Result.failure(Exception("ECU response unexpected for Mode 04: $clean"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear DTC codes", e)
            Result.failure(e)
        }
    }
}
