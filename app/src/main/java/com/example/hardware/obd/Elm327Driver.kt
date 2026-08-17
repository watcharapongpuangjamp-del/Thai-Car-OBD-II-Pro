package com.example.hardware.obd

import android.util.Log
import com.example.hardware.transport.UsbTransport
import com.example.model.DiagnosticError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class Elm327Driver(
    private val transport: UsbTransport
) {

    companion object {
        private const val TAG = "Elm327Driver"
    }

    private var detectedProtocol: ObdProtocol = ObdProtocol.UNKNOWN
    private var elmVersion: String = "Unknown"

    /**
     * Executes strict initialization sequence according to automotive spec:
     * 1. ATZ (Reset ELM327)
     * 2. ATE0 (Echo off)
     * 3. ATL0 (Linefeeds off)
     * 4. ATS0 (Spaces off or on - normalized)
     * 5. ATH0 (Headers off initially or configured)
     * 6. ATSP0 (Automatic protocol detection)
     * 7. 0100 (ECU ping and PID discovery)
     */
    suspend fun performHandshake(): Result<ObdProtocol> = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "Starting ELM327 handshake sequence...")

            // 1. Reset ELM327
            val atzResp = sendRawCommand("ATZ", timeoutMs = 2500)
            Log.i(TAG, "ATZ Response: $atzResp")
            delay(1000) // Allow ELM327 internal bootup

            // 2. Echo Off
            val ate0Resp = sendRawCommand("ATE0", timeoutMs = 1500)
            Log.i(TAG, "ATE0 Response: $ate0Resp")

            // 3. Linefeed Off
            val atl0Resp = sendRawCommand("ATL0", timeoutMs = 1000)
            Log.i(TAG, "ATL0 Response: $atl0Resp")

            // 4. Spaces Off (or keep predictable)
            val ats0Resp = sendRawCommand("ATS0", timeoutMs = 1000)
            Log.i(TAG, "ATS0 Response: $ats0Resp")

            // 5. Headers Off
            val ath0Resp = sendRawCommand("ATH0", timeoutMs = 1000)
            Log.i(TAG, "ATH0 Response: $ath0Resp")

            // Identify Version (ATI)
            val atiResp = sendRawCommand("ATI", timeoutMs = 1000)
            elmVersion = atiResp.replace(">", "").trim()
            Log.i(TAG, "ELM327 Version: $elmVersion")

            // 6. Set Protocol to Auto
            val atsp0Resp = sendRawCommand("ATSP0", timeoutMs = 1500)
            Log.i(TAG, "ATSP0 Response: $atsp0Resp")

            // 7. Verify ECU Connection with Mode 01 PID 00
            Log.i(TAG, "Sending 0100 to detect ECU protocol...")
            val pid00Resp = sendRawCommand("0100", timeoutMs = 4000)
            Log.i(TAG, "0100 Response: $pid00Resp")

            val normalized = ObdResponseNormalizer.normalize(pid00Resp, "0100")
            if (normalized.contains("NO DATA", ignoreCase = true) ||
                normalized.contains("UNABLE TO CONNECT", ignoreCase = true) ||
                normalized.contains("BUS INIT: ERROR", ignoreCase = true) ||
                normalized.contains("CAN ERROR", ignoreCase = true)
            ) {
                return@withContext Result.failure(
                    DiagnosticError.EcuError(
                        "ERR_ECU_NO_CONNECT",
                        "ECU ไม่ตอบสนองต่อคำสั่ง 0100 (กรุณาบิดกุญแจรถยนต์ไปที่ตำแหน่ง ON หรือสตาร์ทเครื่องยนต์)",
                        "ECU failed to respond to 0100 ping. Ensure ignition is ON"
                    )
                )
            }

            // Query active protocol via ATDP / ATDPN
            val atdpResp = sendRawCommand("ATDP", timeoutMs = 1000)
            Log.i(TAG, "Active OBD Protocol (ATDP): $atdpResp")
            detectedProtocol = ObdProtocol.fromElmCode(atdpResp)

            Log.i(TAG, "ELM327 Handshake completed successfully. Protocol: ${detectedProtocol.description}")
            Result.success(detectedProtocol)
        } catch (e: Exception) {
            Log.e(TAG, "ELM327 Handshake failed with exception", e)
            Result.failure(
                DiagnosticError.Elm327Error(
                    "ERR_HANDSHAKE_FAILED",
                    "การทำ Handshake กับอะแดปเตอร์ ELM327 ล้มเหลว: ${e.localizedMessage}",
                    "Handshake failed: ${e.message}",
                    e
                )
            )
        }
    }

    suspend fun queryObd(command: String, timeoutMs: Int = 2000): Result<ObdFrame> = withContext(Dispatchers.IO) {
        try {
            val raw = sendRawCommand(command, timeoutMs)
            val frame = ObdResponseNormalizer.parseFrame(raw, command)
                ?: return@withContext Result.failure(
                    DiagnosticError.ParserError(
                        "ERR_FRAME_EMPTY",
                        "ไม่สามารถแปลงข้อมูลตอบกลับจากคำสั่ง $command เป็น OBD Frame ได้ (Raw: $raw)",
                        "Malformed or empty OBD response for $command"
                    )
                )
            Result.success(frame)
        } catch (e: Exception) {
            Result.failure(
                DiagnosticError.ProtocolError(
                    "ERR_QUERY_OBD",
                    "เกิดข้อผิดพลาดในการส่งคำสั่ง $command: ${e.localizedMessage}",
                    "Failed to query OBD command $command",
                    e
                )
            )
        }
    }

    suspend fun sendRawCommand(command: String, timeoutMs: Int = 1500): String = withContext(Dispatchers.IO) {
        if (!transport.isOpen()) {
            throw DiagnosticError.UsbError("ERR_TRANSPORT_CLOSED", "พอร์ต USB ปิดอยู่ ไม่สามารถส่งคำสั่งได้", "Transport is closed")
        }
        val cmdBytes = (command.trim() + "\r").toByteArray()
        val writeResult = transport.write(cmdBytes, timeoutMs)
        if (writeResult.isFailure) {
            throw writeResult.exceptionOrNull() ?: DiagnosticError.SerialError()
        }

        val resultBuilder = StringBuilder()
        val buffer = ByteArray(512)
        val startTime = System.currentTimeMillis()

        while (System.currentTimeMillis() - startTime < timeoutMs) {
            val readResult = transport.read(buffer, 300)
            if (readResult.isSuccess) {
                val count = readResult.getOrDefault(0)
                if (count > 0) {
                    val chunk = String(buffer, 0, count)
                    resultBuilder.append(chunk)
                    if (chunk.contains(">")) {
                        break
                    }
                }
            } else {
                break
            }
        }

        val response = resultBuilder.toString().trim()
        if (response.isEmpty()) {
            throw DiagnosticError.Elm327Error(
                "ERR_ELM_TIMEOUT",
                "อะแดปเตอร์ ELM327 ไม่ตอบสนองภายใน ${timeoutMs}ms (คำสั่ง: $command)",
                "ELM327 timeout on command $command"
            )
        }
        return@withContext response
    }

    fun getDetectedProtocol(): ObdProtocol = detectedProtocol
    fun getElmVersion(): String = elmVersion
}
