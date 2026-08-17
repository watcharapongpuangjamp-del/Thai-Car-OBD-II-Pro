package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hardware.SimulatorScenario
import com.example.model.AppOperationMode
import com.example.model.LiveSensorData
import com.example.rules.RuleEngineReport
import com.example.ui.components.DiagnosticAlertComponent
import com.example.ui.components.LiveGaugeCard
import com.example.ui.components.ModeHeader
import com.example.ui.theme.*

@Composable
fun DashboardScreen(
    telemetry: LiveSensorData,
    activeMode: AppOperationMode,
    currentScenario: SimulatorScenario,
    diagnosticReport: RuleEngineReport? = null,
    tripSummary: com.example.model.TripSummary?,
    tripStatus: com.example.model.TripStatus,
    onStartTrip: () -> Unit,
    onPauseTrip: () -> Unit,
    onStopTrip: () -> Unit,
    onModeSelected: (AppOperationMode) -> Unit,
    onConnectUsb: () -> Unit,
    onDisconnectUsb: () -> Unit,
    onScenarioSelected: (SimulatorScenario) -> Unit,
    onNavigateToAiMechanic: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
            .testTag("dashboard_screen")
    ) {
        // Mode Header & Connection Status
        ModeHeader(
            activeMode = activeMode,
            telemetry = telemetry,
            currentScenario = currentScenario,
            onModeSelected = onModeSelected,
            onConnectUsb = onConnectUsb,
            onDisconnectUsb = onDisconnectUsb,
            onScenarioSelected = onScenarioSelected
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Diagnostic Alert Component (Rule Engine Safety Shield)
        diagnosticReport?.let { report ->
            DiagnosticAlertComponent(
                report = report,
                onConsultAiClick = onNavigateToAiMechanic,
                initiallyExpanded = report.anomalies.isNotEmpty()
            )
            Spacer(modifier = Modifier.height(14.dp))
        }

        // ECU Communication Stats Bar
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("pid_stats_card"),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = "PID Rate",
                        tint = CyanPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "PID Stream: ${telemetry.pidPerSec} PID/s",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Text(
                    text = "Latency: ${telemetry.latencyMs} ms",
                    color = TextSecondary,
                    fontSize = 12.sp
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            if (activeMode == AppOperationMode.REAL_HARDWARE) EmeraldConnected.copy(alpha = 0.2f)
                            else PurpleAi.copy(alpha = 0.2f)
                        )
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (activeMode == AppOperationMode.REAL_HARDWARE) "REAL ECU" else "SIMULATOR",
                        color = if (activeMode == AppOperationMode.REAL_HARDWARE) EmeraldConnected else PurpleGlow,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Primary Live Gauges Grid (2 Columns)
        val rpmVal = telemetry.rpm?.toString() ?: "N/A"
        val rpmFrac = (telemetry.rpm ?: 0) / 6000f

        val speedVal = telemetry.speedKmh?.toString() ?: "N/A"
        val speedFrac = (telemetry.speedKmh ?: 0) / 220f

        val coolantVal = telemetry.coolantTempC?.let { "$it°C" } ?: "N/A"
        val coolantFrac = (telemetry.coolantTempC ?: 0) / 130f
        val coolantAccent = if ((telemetry.coolantTempC ?: 0) > 105) RedCritical else if ((telemetry.coolantTempC ?: 0) > 98) AmberWarning else CyanPrimary
        val coolantStatus = if ((telemetry.coolantTempC ?: 0) > 105) "ความร้อนสูง!" else if ((telemetry.coolantTempC ?: 0) > 98) "เตือนความร้อน" else "อุณหภูมิปกติ"

        val voltVal = telemetry.batteryVoltage?.let { String.format("%.1fV", it) } ?: "N/A"
        val voltFrac = ((telemetry.batteryVoltage ?: 12f) - 10f) / 6f
        val voltAccent = if ((telemetry.batteryVoltage ?: 12f) < 12.0f) AmberWarning else EmeraldConnected

        val boostVal = telemetry.boostPressureBar?.let { String.format("%.2f bar", it) } ?: "N/A"
        val boostFrac = (telemetry.boostPressureBar ?: 0f) / 2.5f

        val fuelVal = telemetry.fuelRateLph?.let { String.format("%.1f L/h", it) } ?: "N/A"
        val fuelFrac = (telemetry.fuelRateLph ?: 0f) / 20f

        Row(modifier = Modifier.fillMaxWidth()) {
            LiveGaugeCard(
                titleTh = "รอบเครื่องยนต์ (RPM)",
                titleEn = "Engine Speed",
                valueString = rpmVal,
                unit = "rpm",
                progressFraction = rpmFrac,
                colorAccent = CyanPrimary,
                statusNoticeTh = "ปกติ",
                testTag = "gauge_rpm",
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(12.dp))
            LiveGaugeCard(
                titleTh = "ความเร็วรถ (Speed)",
                titleEn = "Vehicle Speed",
                valueString = speedVal,
                unit = "km/h",
                progressFraction = speedFrac,
                colorAccent = EmeraldConnected,
                statusNoticeTh = "ปกติ",
                testTag = "gauge_speed",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            LiveGaugeCard(
                titleTh = "ความร้อนหม้อน้ำ",
                titleEn = "Coolant Temp",
                valueString = coolantVal,
                unit = "",
                progressFraction = coolantFrac,
                colorAccent = coolantAccent,
                statusNoticeTh = coolantStatus,
                testTag = "gauge_coolant",
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(12.dp))
            LiveGaugeCard(
                titleTh = "แรงดันแบตเตอรี่",
                titleEn = "Battery Voltage",
                valueString = voltVal,
                unit = "",
                progressFraction = voltFrac,
                colorAccent = voltAccent,
                statusNoticeTh = if ((telemetry.batteryVoltage ?: 12f) < 12.0f) "แบตต่ำ" else "ชาร์จปกติ",
                testTag = "gauge_voltage",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            LiveGaugeCard(
                titleTh = "แรงดันเทอร์โบ (Boost)",
                titleEn = "Manifold Pressure",
                valueString = boostVal,
                unit = "",
                progressFraction = boostFrac,
                colorAccent = AmberWarning,
                statusNoticeTh = "Turbo Active",
                testTag = "gauge_boost",
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(12.dp))
            LiveGaugeCard(
                titleTh = "อัตราสิ้นเปลืองน้ำมัน",
                titleEn = "Fuel Flow Rate",
                valueString = fuelVal,
                unit = "",
                progressFraction = fuelFrac,
                colorAccent = PurpleGlow,
                statusNoticeTh = "Realtime",
                testTag = "gauge_fuel_rate",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Trip Computer HUD Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("trip_computer_card"),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ระบบบันทึกทริป (Trip Computer)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Row {
                        if (tripStatus == com.example.model.TripStatus.RECORDING) {
                            Button(
                                onClick = onPauseTrip,
                                colors = ButtonDefaults.buttonColors(containerColor = AmberWarning),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("พักทริป", fontSize = 11.sp, color = DarkBackground)
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Button(
                                onClick = onStopTrip,
                                colors = ButtonDefaults.buttonColors(containerColor = RedCritical),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("จบทริป", fontSize = 11.sp, color = androidx.compose.ui.graphics.Color.White)
                            }
                        } else {
                            Button(
                                onClick = onStartTrip,
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldConnected),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text(if (tripSummary != null) "บันทึกต่อ" else "เริ่มทริปใหม่", fontSize = 11.sp, color = DarkBackground)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("ระยะทาง", fontSize = 11.sp, color = TextSecondary)
                        Text(
                            text = tripSummary?.let { "${it.distanceKm} km" } ?: "0.0 km",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyanPrimary
                        )
                    }
                    Column {
                        Text("ความเร็วเฉลี่ย", fontSize = 11.sp, color = TextSecondary)
                        Text(
                            text = tripSummary?.let { "${it.avgSpeedKmh} km/h" } ?: "0.0 km/h",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldConnected
                        )
                    }
                    Column {
                        Text("อัตราสิ้นเปลือง", fontSize = 11.sp, color = TextSecondary)
                        Text(
                            text = tripSummary?.let { "${it.avgFuelConsumptionL100km} L/100km" } ?: "0.0 L/100km",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = AmberWarning
                        )
                    }
                    Column {
                        Text("น้ำมันที่ใช้", fontSize = 11.sp, color = TextSecondary)
                        Text(
                            text = tripSummary?.let { "${it.totalFuelUsedLiters} L" } ?: "0.0 L",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = PurpleGlow
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
