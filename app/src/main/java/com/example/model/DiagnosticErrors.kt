package com.example.model

sealed class DiagnosticError(
    val errorCode: String,
    val userMessageTh: String,
    val userMessageEn: String,
    cause: Throwable? = null
) : Exception("[$errorCode] $userMessageEn: $userMessageTh", cause) {

    class UsbError(
        errorCode: String = "ERR_USB_GENERAL",
        userMessageTh: String = "เกิดข้อผิดพลาดในการเชื่อมต่อพอร์ต USB",
        userMessageEn: String = "USB port communication error",
        cause: Throwable? = null
    ) : DiagnosticError(errorCode, userMessageTh, userMessageEn, cause)

    class SerialError(
        errorCode: String = "ERR_SERIAL_CONFIG",
        userMessageTh: String = "ไม่สามารถตั้งค่า Baud Rate หรือสื่อสารผ่าน Serial Port ได้",
        userMessageEn: String = "Failed to configure serial baud rate or serial communication",
        cause: Throwable? = null
    ) : DiagnosticError(errorCode, userMessageTh, userMessageEn, cause)

    class Elm327Error(
        errorCode: String = "ERR_ELM327_HANDSHAKE",
        userMessageTh: String = "อะแดปเตอร์ ELM327 ไม่ตอบสนองต่อคำสั่ง Handshake",
        userMessageEn: String = "ELM327 adapter failed to respond to handshake command",
        cause: Throwable? = null
    ) : DiagnosticError(errorCode, userMessageTh, userMessageEn, cause)

    class ProtocolError(
        errorCode: String = "ERR_OBD_PROTOCOL",
        userMessageTh: String = "ไม่พบโปรโตคอล OBD-II ที่รถยนต์รองรับ (ISO 15765-4 / KWP / etc.)",
        userMessageEn: String = "Unable to auto-detect vehicle OBD-II protocol",
        cause: Throwable? = null
    ) : DiagnosticError(errorCode, userMessageTh, userMessageEn, cause)

    class EcuError(
        errorCode: String = "ERR_ECU_NO_RESPONSE",
        userMessageTh: String = "กล่อง ECU รถยนต์ไม่ตอบสนอง กรุณาตรวจสอบว่าบิดกุญแจ ON หรือยัง",
        userMessageEn: String = "ECU is not responding. Ensure ignition key is in ON position",
        cause: Throwable? = null
    ) : DiagnosticError(errorCode, userMessageTh, userMessageEn, cause)

    class ParserError(
        errorCode: String = "ERR_FRAME_PARSE",
        userMessageTh: String = "โครงสร้างข้อมูลตอบกลับจาก ECU ผิดรูปแบบหรือไม่ถูกต้อง",
        userMessageEn: String = "Malformed response frame received from ECU",
        cause: Throwable? = null
    ) : DiagnosticError(errorCode, userMessageTh, userMessageEn, cause)

    class DatabaseError(
        errorCode: String = "ERR_DB_PERSIST",
        userMessageTh: String = "ไม่สามารถบันทึกประวัติการสแกนหรือข้อมูลยานพาหนะลงฐานข้อมูลได้",
        userMessageEn: String = "Failed to persist diagnostic record in local database",
        cause: Throwable? = null
    ) : DiagnosticError(errorCode, userMessageTh, userMessageEn, cause)

    class AiError(
        errorCode: String = "ERR_AI_ANALYSIS",
        userMessageTh: String = "ไม่สามารถประมวลผลคำแนะนำจาก AI Mechanic ได้",
        userMessageEn: String = "AI Mechanic analysis processing failed",
        cause: Throwable? = null
    ) : DiagnosticError(errorCode, userMessageTh, userMessageEn, cause)
}
