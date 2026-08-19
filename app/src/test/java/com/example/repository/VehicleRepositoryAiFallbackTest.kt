package com.example.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class VehicleRepositoryAiFallbackTest {

    @Test
    fun testAiMechanicFallback_WhenNoCredentialsOrNetworkFail() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = VehicleRepository(context)

        val telemetry = LiveSensorData(
            isConnected = false,
            connectionState = ConnectionState.DISCONNECTED,
            pidPerSec = 0,
            latencyMs = 0,
            mode = AppOperationMode.REAL_HARDWARE
        )

        val dtcCodes = listOf(
            DtcCode(
                code = "P0171",
                module = "ECM",
                descriptionEn = "System Too Lean (Bank 1)",
                descriptionTh = "ระบบเชื้อเพลิงบางเกินไป (System Too Lean)",
                severity = DtcSeverity.CRITICAL,
                modeProvenance = AppOperationMode.REAL_HARDWARE,
                timestamp = System.currentTimeMillis()
            )
        )

        val result = repository.analyzeWithAiMechanic(
            vehicleInfo = "Toyota Hilux Revo 2018",
            dtcCodes = dtcCodes,
            telemetry = telemetry
        )

        assertNotNull(result)
        assertEquals("วิกฤต (Critical)", result.severityLevel)
        assertEquals("REAL VEHICLE HARDWARE", result.provenanceLabel)
    }
}
