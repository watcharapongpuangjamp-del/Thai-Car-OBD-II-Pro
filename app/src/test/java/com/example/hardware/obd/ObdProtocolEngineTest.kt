package com.example.hardware.obd

import com.example.model.DiagnosticError
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ObdProtocolEngineTest {

    // Fake implementation of IElm327Driver
    private class FakeElmDriver : IElm327Driver {
        var commandResponses = mutableMapOf<String, String>()
        
        override suspend fun performHandshake(): Result<ObdProtocol> = Result.success(ObdProtocol.UNKNOWN)
        
        override suspend fun queryObd(command: String, timeoutMs: Int): Result<ObdFrame> = Result.failure(Exception("Not implemented"))
        
        override suspend fun sendRawCommand(command: String, timeoutMs: Int): String {
            return commandResponses[command] ?: "NO DATA"
        }
        
        override fun getDetectedProtocol(): ObdProtocol = ObdProtocol.UNKNOWN
        override fun getElmVersion(): String = "Fake"
    }

    private val fakeElmDriver = FakeElmDriver()
    private val engine = ObdProtocolEngine(fakeElmDriver)

    @Test
    fun `test protocol auto-detection`() = runBlocking {
        // Setup responses
        fakeElmDriver.commandResponses["ATSP6"] = "OK"
        fakeElmDriver.commandResponses["0100"] = "41 00 ..."
        
        val result = engine.initializeProtocolEngine(ObdProtocol.AUTO)
        
        assertEquals(ObdProtocol.ISO_15765_4_CAN_11BIT_500K, result.getOrNull())
    }
}
