package com.example.hardware.obd

interface IElm327Driver {
    suspend fun performHandshake(): Result<ObdProtocol>
    suspend fun queryObd(command: String, timeoutMs: Int = 2000): Result<ObdFrame>
    suspend fun sendRawCommand(command: String, timeoutMs: Int = 1500): String
    fun getDetectedProtocol(): ObdProtocol
    fun getElmVersion(): String
}
