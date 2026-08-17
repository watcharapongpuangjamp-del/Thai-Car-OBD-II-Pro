package com.example.hardware.obd

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class Mode04DtcClearVerificationTest {

    @Test
    fun `verify mode 04 response interpretation logic`() {
        // Test normalizer and expected ECU acknowledgement for Mode 04 (Clear DTC / Reset MIL)
        // Standard positive response from ECU for Mode 04 is "44"
        val positiveResponse = "44"
        val normalizedPositive = ObdResponseNormalizer.normalize(positiveResponse, "04")
        val isSuccessPositive = normalizedPositive.contains("44") || normalizedPositive.contains("OK")
        assertTrue("Mode 04 response '44' must be correctly interpreted as success", isSuccessPositive)

        // Test NO DATA response (should not be interpreted as success)
        val noDataResponse = "NO DATA"
        val normalizedNoData = ObdResponseNormalizer.normalize(noDataResponse, "04")
        val isSuccessNoData = (normalizedNoData.contains("44") || normalizedNoData.contains("OK")) && normalizedNoData.isNotEmpty()
        assertFalse("Mode 04 response 'NO DATA' must NOT be interpreted as success", isSuccessNoData)

        // Test error response
        val errorResponse = "ERROR"
        val normalizedError = ObdResponseNormalizer.normalize(errorResponse, "04")
        val isSuccessError = (normalizedError.contains("44") || normalizedError.contains("OK")) && normalizedError.isNotEmpty()
        assertFalse("Mode 04 response 'ERROR' must NOT be interpreted as success", isSuccessError)
    }
}
