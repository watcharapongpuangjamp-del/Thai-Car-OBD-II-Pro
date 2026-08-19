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

        val telemetry = LiveSensorData.fromReal(
            rpm = 850,
            speedKmh = 0,
            coolantTempC = 90,
            batteryVoltage = 12.5,
            intakeAirTempC = 35,
            manifoldPressureKpa = 30,
            throttlePosPercent = 15.0,
            engineLoadPercent = 25.0,
            timingAdvance = 10.0,
            fuelRateLph = 1.2,
            boostPressureBar = 0.0
        )

        val dtcCodes = listOf(
            DtcCode(
                code = "P0171",
                module = "ECM",
                descriptionTh = "ระบบเชื้อเพลิงบางเกินไป (System Too Lean)",
                descriptionEn = "System Too Lean (Bank 1)",
                severity = DtcSeverity.CRITICAL,
                status = "Confirmed",
                provenance = DataProvenance.REAL_HARDWARE,
                timestamp = System.currentTimeMillis()
            )
        )

        // This should throw an exception in Firebase/OkHttp since Firebase isn't initialized or no network
        // It will trigger the fallback logic returning the rule engine's results.
        val result = repository.analyzeWithAiMechanic(
            vehicleInfo = "Toyota Hilux Revo 2018",
            dtcCodes = dtcCodes,
            telemetry = telemetry
        )

        // Verify fallback contains Rule Engine summary and the correct DTC
        assertTrue(result.summaryTh.contains("วิเคราะห์ระบบผ่าน Diagnostic Rule Engine เรียบร้อยแล้ว") || result.summaryTh.contains("AI Unavailable"))
        assertTrue(result.possibleRootCausesTh.any { it.contains("P0171") || it.contains("ระบบเชื้อเพลิง") || it.contains("รั่ว") })
        assertEquals("วิกฤต (Critical)", result.severityLevel)
        assertEquals("REAL VEHICLE HARDWARE", result.provenanceLabel)
    }
}
