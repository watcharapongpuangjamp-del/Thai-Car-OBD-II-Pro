package com.example.hardware.obd

import com.example.model.DtcScanResult
import com.example.model.DtcScanStatus
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RealDtcScannerTest {

    private class FakeElmDriver(val responseToReturn: String) : IElm327Driver {
        override suspend fun performHandshake(): Result<ObdProtocol> = Result.success(ObdProtocol.AUTO)
        override suspend fun queryObd(command: String, timeoutMs: Int): Result<ObdFrame> = Result.success(ObdFrame(mode = 3, data = byteArrayOf(), raw = responseToReturn))
        override suspend fun sendRawCommand(command: String, timeoutMs: Int): String = responseToReturn
        override fun getDetectedProtocol(): ObdProtocol = ObdProtocol.AUTO
        override fun getElmVersion(): String = "ELM327 v1.5"
    }

    @Test
    fun `scan_timeout_returns_failed_status`() = runBlocking {
        val fakeDriver = FakeElmDriver("TIMEOUT")
        val scanner = RealDtcScanner(fakeDriver)

        val result = scanner.performCompleteDtcScan()

        assertTrue("Should return FAILED status on timeout", result.status is DtcScanStatus.FAILED)
    }

    @Test
    fun `scan_malformed_response_handled`() = runBlocking {
        val fakeDriver = FakeElmDriver("INVALID_RESPONSE_DATA")
        val scanner = RealDtcScanner(fakeDriver)

        val result = scanner.performCompleteDtcScan()

        assertTrue(result.status == DtcScanStatus.NO_CODES)
    }
}
