package com.example.hardware.obd

import android.util.Log
import com.example.model.DiagnosticError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class SupportedPids(
    val supportedPidsMap: Set<Int> = emptySet()
) {
    fun isSupported(pid: Int): Boolean = supportedPidsMap.contains(pid)
}

class ObdProtocolEngine(
    private val elmDriver: IElm327Driver
) {

    companion object {
        private const val TAG = "ObdProtocolEngine"
        // Ordered scan list for Auto detection
        val AUTO_SCAN_ORDER = listOf(
            ObdProtocol.ISO_15765_4_CAN_11BIT_500K,
            ObdProtocol.ISO_15765_4_CAN_29BIT_500K,
            ObdProtocol.ISO_14230_4_KWP_FAST,
            ObdProtocol.ISO_9141_2
        )
    }

    private var activeProtocol: ObdProtocol = ObdProtocol.UNKNOWN
    private var supportedPids: SupportedPids = SupportedPids()

    suspend fun initializeProtocolEngine(targetProtocol: ObdProtocol = ObdProtocol.AUTO): Result<ObdProtocol> = withContext(Dispatchers.IO) {
        ObdDiagnosticLogger.log("PROTOCOL_ENGINE", "Initializing with protocol: ${targetProtocol.id}")
        
        val protocolToUse = if (targetProtocol == ObdProtocol.AUTO) {
            detectProtocol()
        } else {
            setProtocol(targetProtocol)
        }
        
        if (protocolToUse == ObdProtocol.UNKNOWN) {
            return@withContext Result.failure(DiagnosticError.ProtocolError("ERR_PROTOCOL_DETECT", "ไม่สามารถตรวจจับหรือตั้งค่าโปรโตคอลได้", "Failed to detect or set protocol"))
        }
        
        activeProtocol = protocolToUse
        ObdDiagnosticLogger.log("PROTOCOL_ENGINE", "Protocol Ready: ${activeProtocol.id}")
        
        // Discover supported PIDs for Mode 01
        discoverSupportedPids()
        
        Result.success(activeProtocol)
    }

    private suspend fun detectProtocol(): ObdProtocol {
        for (protocol in AUTO_SCAN_ORDER) {
            ObdDiagnosticLogger.log("AUTO_DETECT", "Trying: ${protocol.id}")
            if (setProtocol(protocol) != ObdProtocol.UNKNOWN) {
                // Verify with a simple ping
                val ping = elmDriver.sendRawCommand("0100", timeoutMs = 2000)
                if (ping.contains("41", ignoreCase = true)) {
                    ObdDiagnosticLogger.log("AUTO_DETECT", "Detected: ${protocol.id}")
                    return protocol
                }
            }
        }
        return ObdProtocol.UNKNOWN
    }

    private suspend fun setProtocol(protocol: ObdProtocol): ObdProtocol {
        val command = "ATSP${protocol.protocolNumber}"
        val response = elmDriver.sendRawCommand(command, timeoutMs = 1000)
        return if (response.contains("OK", ignoreCase = true)) {
            protocol
        } else {
            ObdDiagnosticLogger.log("SET_PROTOCOL", "Failed: ${protocol.id} | Response: $response")
            ObdProtocol.UNKNOWN
        }
    }

    suspend fun discoverSupportedPids(): SupportedPids = withContext(Dispatchers.IO) {
        val supported = mutableSetOf<Int>()
        try {
            // Check 0100 (PIDs 01-20)
            val pid00Frame = elmDriver.queryObd("0100").getOrNull()
            if (pid00Frame != null && pid00Frame.data.size >= 4) {
                parsePidBitmask(0x00, pid00Frame.data, supported)

                // If PID 0120 is supported, query 0120 (PIDs 21-40)
                if (supported.contains(0x20)) {
                    val pid20Frame = elmDriver.queryObd("0120").getOrNull()
                    if (pid20Frame != null && pid20Frame.data.size >= 4) {
                        parsePidBitmask(0x20, pid20Frame.data, supported)

                        // If PID 0140 is supported, query 0140 (PIDs 41-60)
                        if (supported.contains(0x40)) {
                            val pid40Frame = elmDriver.queryObd("0140").getOrNull()
                            if (pid40Frame != null && pid40Frame.data.size >= 4) {
                                parsePidBitmask(0x40, pid40Frame.data, supported)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "PID discovery partially failed: ${e.message}")
        }
        supportedPids = SupportedPids(supported)
        Log.i(TAG, "Supported PIDs discovered (${supported.size} PIDs): ${supported.map { String.format("0x%02X", it) }}")
        return@withContext supportedPids
    }

    private fun parsePidBitmask(basePid: Int, data: ByteArray, targetSet: MutableSet<Int>) {
        for (byteIdx in 0 until minOf(4, data.size)) {
            val byteVal = data[byteIdx].toInt() and 0xFF
            for (bit in 7 downTo 0) {
                if ((byteVal and (1 shl bit)) != 0) {
                    val pidNumber = basePid + (byteIdx * 8) + (7 - bit) + 1
                    targetSet.add(pidNumber)
                }
            }
        }
    }

    suspend fun readPid(pid: Int): Result<ObdFrame> = withContext(Dispatchers.IO) {
        val pidHex = String.format("01%02X", pid)
        val frameResult = elmDriver.queryObd(pidHex)
        if (frameResult.isFailure) {
            return@withContext frameResult
        }
        val frame = frameResult.getOrThrow()
        if (frame.mode != 0x41) {
            return@withContext Result.failure(
                DiagnosticError.ParserError(
                    "ERR_INVALID_MODE",
                    "ได้รับ Mode ตอบกลับไม่ถูกต้อง (คาดหวัง 0x41 ได้รับ 0x${Integer.toHexString(frame.mode)})",
                    "Expected response mode 0x41, got 0x${Integer.toHexString(frame.mode)}"
                )
            )
        }
        Result.success(frame)
    }

    suspend fun queryVin(): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Mode 09 PID 02 - VIN
            val raw = elmDriver.sendRawCommand("0902", timeoutMs = 3000)
            val normalized = ObdResponseNormalizer.normalize(raw, "0902")
            if (normalized.contains("NO DATA", ignoreCase = true)) {
                return@withContext Result.failure(DiagnosticError.EcuError("ERR_VIN_NO_DATA", "ECU ไม่ส่งข้อมูลเลขตัวถัง VIN", "VIN query returned NO DATA"))
            }

            // Extract alphanumeric characters for VIN (17 chars)
            val cleaned = normalized.replace(" ", "")
            // Remove common mode/PID prefixes
            val hexRegex = Regex("4902[0-9A-Fa-f]+")
            val match = hexRegex.find(cleaned)
            val hexPayload = if (match != null) match.value.substring(4) else cleaned

            val asciiVin = StringBuilder()
            for (i in 0 until hexPayload.length - 1 step 2) {
                val byteVal = hexPayload.substring(i, i + 2).toIntOrNull(16)
                if (byteVal != null && byteVal in 32..126) {
                    asciiVin.append(byteVal.toChar())
                }
            }

            val finalVin = asciiVin.toString().filter { it.isLetterOrDigit() }
            if (finalVin.length >= 11) {
                Result.success(finalVin.take(17))
            } else {
                Result.failure(DiagnosticError.ParserError("ERR_VIN_FORMAT", "ไม่สามารถแปลงข้อมูลเป็น VIN มาตรฐานได้", "Failed to parse standard VIN"))
            }
        } catch (e: Exception) {
            Result.failure(DiagnosticError.ProtocolError("ERR_VIN_EXCEPTION", "เกิดข้อผิดพลาดในการดึง VIN: ${e.message}", "Exception during VIN read", e))
        }
    }

    fun getActiveProtocol(): ObdProtocol = activeProtocol
    fun getSupportedPids(): SupportedPids = supportedPids
}
