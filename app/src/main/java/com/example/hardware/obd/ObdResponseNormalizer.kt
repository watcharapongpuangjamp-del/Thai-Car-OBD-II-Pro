package com.example.hardware.obd

import java.util.Locale

object ObdResponseNormalizer {

    /**
     * Normalizes raw response from ELM327 adapter:
     * - Strips prompt character '>'
     * - Strips carriage returns and newlines
     * - Strips echo of command
     * - Strips 'SEARCHING...' indicator
     * - Strips spaces and unneeded control characters
     */
    fun normalize(rawResponse: String, sentCommand: String? = null): String {
        var clean = rawResponse
            .replace("\r", " ")
            .replace("\n", " ")
            .replace(">", "")
            .trim()

        // Remove prompt / searching artifacts
        clean = clean.replace("SEARCHING...", "", ignoreCase = true).trim()
        clean = clean.replace("SEARCHING", "", ignoreCase = true).trim()
        clean = clean.replace("STOPPED", "", ignoreCase = true).trim()

        // Remove echo of sent command if present at start
        sentCommand?.let { cmd ->
            val trimmedCmd = cmd.trim().uppercase(Locale.ROOT)
            val cleanUpper = clean.uppercase(Locale.ROOT)
            if (cleanUpper.startsWith(trimmedCmd)) {
                clean = clean.substring(trimmedCmd.length).trim()
            }
        }

        return clean.trim()
    }

    /**
     * Extracts hex data segments from normalized response.
     * Handles both space-separated responses (e.g. "41 0C 1A F8")
     * and compact responses (e.g. "410C1AF8") and CAN header responses (e.g. "7E8 04 41 0C 1A F8").
     */
    fun parseFrame(rawResponse: String, sentCommand: String? = null): ObdFrame? {
        val normalized = normalize(rawResponse, sentCommand)
        if (normalized.isEmpty()) return null

        val tokens = normalized.split("\\s+".toRegex()).filter { it.isNotBlank() }
        if (tokens.isEmpty()) return null

        // Check for special ELM error responses
        val upperFirst = tokens.first().uppercase(Locale.ROOT)
        if (upperFirst in listOf("NO", "UNABLE", "BUS", "CAN", "ERROR", "FB", "LV", "?")) {
            return null
        }

        var header: String? = null
        var length: Int? = null
        val hexBytes = mutableListOf<Byte>()

        var index = 0

        // Check if first token is a 3 or 8 digit CAN Header (e.g. 7E8 or 18DAF110)
        if (tokens.isNotEmpty() && isHex(tokens[0]) && (tokens[0].length == 3 || tokens[0].length == 8)) {
            header = tokens[0]
            index++

            // Check if second token is PCI Length byte in CAN (e.g. 02, 03, 04, 05, 06, 07)
            if (index < tokens.size && tokens[index].length <= 2 && isHex(tokens[index])) {
                length = tokens[index].toIntOrNull(16)
                index++
            }
        }

        // If tokens are formatted as single continuous hex string (e.g. "410C1AF8")
        if (index < tokens.size && tokens[index].length > 2) {
            val compactHex = tokens.subList(index, tokens.size).joinToString("")
            val parsedBytes = parseCompactHex(compactHex)
            if (parsedBytes.isEmpty()) return null
            val mode = parsedBytes[0].toInt() and 0xFF
            val pid = if (parsedBytes.size > 1) parsedBytes[1].toInt() and 0xFF else null
            val payload = if (parsedBytes.size > 2) parsedBytes.copyOfRange(2, parsedBytes.size) else ByteArray(0)
            return ObdFrame(
                header = header,
                length = length ?: parsedBytes.size,
                mode = mode,
                pid = pid,
                data = payload,
                raw = rawResponse
            )
        }

        // Parse remaining space-separated hex bytes
        for (i in index until tokens.size) {
            val token = tokens[i]
            if (isHex(token)) {
                val byteVal = token.toIntOrNull(16)
                if (byteVal != null) {
                    hexBytes.add(byteVal.toByte())
                }
            }
        }

        if (hexBytes.isEmpty()) return null

        val mode = hexBytes[0].toInt() and 0xFF
        val pid = if (hexBytes.size > 1) hexBytes[1].toInt() and 0xFF else null
        val payload = if (hexBytes.size > 2) hexBytes.subList(2, hexBytes.size).toByteArray() else ByteArray(0)

        return ObdFrame(
            header = header,
            length = length ?: hexBytes.size,
            mode = mode,
            pid = pid,
            data = payload,
            raw = rawResponse
        )
    }

    private fun isHex(s: String): Boolean {
        if (s.isEmpty()) return false
        return s.all { c -> (c in '0'..'9') || (c in 'a'..'f') || (c in 'A'..'F') }
    }

    private fun parseCompactHex(hex: String): ByteArray {
        val clean = if (hex.length % 2 != 0) hex.substring(0, hex.length - 1) else hex
        val bytes = ByteArray(clean.length / 2)
        for (i in clean.indices step 2) {
            val byteVal = clean.substring(i, i + 2).toIntOrNull(16) ?: return ByteArray(0)
            bytes[i / 2] = byteVal.toByte()
        }
        return bytes
    }
}
