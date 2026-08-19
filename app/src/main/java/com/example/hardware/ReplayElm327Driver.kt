package com.example.hardware

import com.example.hardware.obd.IElm327Driver
import com.example.hardware.obd.ObdFrame
import com.example.hardware.obd.ObdProtocol

class ReplayElm327Driver(
    private val simulatedProtocol: ObdProtocol = ObdProtocol.ISO_15765_4_CAN_11BIT_500K
) : IElm327Driver {

    override suspend fun performHandshake(): Result<ObdProtocol> {
        return Result.success(simulatedProtocol)
    }

    override suspend fun queryObd(command: String, timeoutMs: Int): Result<ObdFrame> {
        return Result.failure(Exception("Replay driver simple mode"))
    }

    override suspend fun sendRawCommand(command: String, timeoutMs: Int): String {
        return "NO DATA"
    }

    override fun getDetectedProtocol(): ObdProtocol {
        return simulatedProtocol
    }

    override fun getElmVersion(): String {
        return "ELM327 v1.5 (Replay)"
    }
}
