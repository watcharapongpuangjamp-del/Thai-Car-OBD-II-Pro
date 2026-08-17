package com.example.hardware.obd

data class ObdFrame(
    val header: String? = null,
    val length: Int? = null,
    val mode: Int,
    val pid: Int? = null,
    val data: ByteArray,
    val raw: String,
    val timestamp: Long = System.currentTimeMillis()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ObdFrame

        if (header != other.header) return false
        if (length != other.length) return false
        if (mode != other.mode) return false
        if (pid != other.pid) return false
        if (!data.contentEquals(other.data)) return false
        if (raw != other.raw) return false
        if (timestamp != other.timestamp) return false

        return true
    }

    override fun hashCode(): Int {
        var result = header?.hashCode() ?: 0
        result = 31 * result + (length ?: 0)
        result = 31 * result + mode
        result = 31 * result + (pid ?: 0)
        result = 31 * result + data.contentHashCode()
        result = 31 * result + raw.hashCode()
        result = 31 * result + timestamp.hashCode()
        return result
    }

    fun toHexDataString(): String {
        return data.joinToString(" ") { String.format("%02X", it) }
    }
}

enum class ObdProtocol(val id: String, val protocolNumber: Int, val description: String) {
    AUTO("AUTO", 0, "Automatic Protocol Search"),
    SAE_J1850_PWM("SAE J1850 PWM", 1, "SAE J1850 PWM (41.6 kbaud)"),
    SAE_J1850_VPW("SAE J1850 VPW", 2, "SAE J1850 VPW (10.4 kbaud)"),
    ISO_9141_2("ISO 9141-2", 3, "ISO 9141-2 (5 baud init, 10.4 kbaud)"),
    ISO_14230_4_KWP_5BAUD("ISO 14230-4 KWP", 4, "ISO 14230-4 KWP (5 baud init, 10.4 kbaud)"),
    ISO_14230_4_KWP_FAST("ISO 14230-4 KWP FAST", 5, "ISO 14230-4 KWP (fast init, 10.4 kbaud)"),
    ISO_15765_4_CAN_11BIT_500K("ISO 15765-4 CAN 11/500", 6, "ISO 15765-4 CAN (11 bit ID, 500 kbaud)"),
    ISO_15765_4_CAN_29BIT_500K("ISO 15765-4 CAN 29/500", 7, "ISO 15765-4 CAN (29 bit ID, 500 kbaud)"),
    ISO_15765_4_CAN_11BIT_250K("ISO 15765-4 CAN 11/250", 8, "ISO 15765-4 CAN (11 bit ID, 250 kbaud)"),
    ISO_15765_4_CAN_29BIT_250K("ISO 15765-4 CAN 29/250", 9, "ISO 15765-4 CAN (29 bit ID, 250 kbaud)"),
    SAE_J1939_CAN("SAE J1939 CAN", 10, "SAE J1939 CAN (29 bit ID, 250* kbaud)"),
    USER1_CAN("USER1 CAN", 11, "USER1 CAN (11* bit ID, 125* kbaud)"),
    USER2_CAN("USER2 CAN", 12, "USER2 CAN (11* bit ID, 50* kbaud)"),
    UNKNOWN("UNKNOWN", -1, "Unknown / Undetermined Protocol");

    companion object {
        fun fromElmCode(code: String): ObdProtocol {
            val upper = code.uppercase().trim()
            return when {
                upper.contains("AUTO") -> AUTO
                upper.contains("ISO 15765-4") || upper.contains("CAN") -> {
                    when {
                        upper.contains("29") && upper.contains("250") -> ISO_15765_4_CAN_29BIT_250K
                        upper.contains("29") && upper.contains("500") -> ISO_15765_4_CAN_29BIT_500K
                        upper.contains("11") && upper.contains("250") -> ISO_15765_4_CAN_11BIT_250K
                        else -> ISO_15765_4_CAN_11BIT_500K
                    }
                }
                upper.contains("ISO 14230") || upper.contains("KWP") -> {
                    if (upper.contains("FAST")) ISO_14230_4_KWP_FAST else ISO_14230_4_KWP_5BAUD
                }
                upper.contains("ISO 9141") -> ISO_9141_2
                upper.contains("J1850 PWM") -> SAE_J1850_PWM
                upper.contains("J1850 VPW") -> SAE_J1850_VPW
                upper.contains("J1939") -> SAE_J1939_CAN
                else -> UNKNOWN
            }
        }
    }
}
