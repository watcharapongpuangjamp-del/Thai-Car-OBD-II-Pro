package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cable
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hardware.SimulatorScenario
import com.example.model.AppOperationMode
import com.example.model.ConnectionState
import com.example.model.LiveSensorData
import com.example.ui.theme.*

@Composable
fun ModeHeader(
    activeMode: AppOperationMode,
    telemetry: LiveSensorData,
    currentScenario: SimulatorScenario,
    onModeSelected: (AppOperationMode) -> Unit,
    onConnectUsb: () -> Unit,
    onDisconnectUsb: () -> Unit,
    onScenarioSelected: (SimulatorScenario) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("mode_header_card"),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            // Mode Segmented Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkBackground)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                AppOperationMode.values().forEach { mode ->
                    val isSelected = mode == activeMode
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) CyanPrimary else Color.Transparent)
                            .clickable { onModeSelected(mode) }
                            .padding(vertical = 10.dp)
                            .testTag("mode_tab_${mode.name}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (mode == AppOperationMode.REAL_HARDWARE) Icons.Default.Usb else Icons.Default.Computer,
                                contentDescription = mode.displayName,
                                tint = if (isSelected) DarkBackground else TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (mode == AppOperationMode.REAL_HARDWARE) "Real Hardware (OTG)" else "Virtual CAN Simulator",
                                color = if (isSelected) DarkBackground else TextSecondary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (activeMode == AppOperationMode.REAL_HARDWARE) {
                // Real Hardware Connection Controls & State Machine Status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(
                                        when (telemetry.connectionState) {
                                            ConnectionState.CONNECTED -> EmeraldConnected
                                            ConnectionState.DISCONNECTED -> RedCritical
                                            else -> AmberWarning
                                        }
                                    )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = telemetry.connectionState.labelTh,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                        Text(
                            text = telemetry.statusMessage,
                            color = TextSecondary,
                            fontSize = 11.sp,
                            maxLines = 1
                        )
                    }

                    if (telemetry.connectionState == ConnectionState.CONNECTED) {
                        Button(
                            onClick = onDisconnectUsb,
                            colors = ButtonDefaults.buttonColors(containerColor = RedCritical.copy(alpha = 0.2f)),
                            modifier = Modifier.testTag("btn_disconnect_usb")
                        ) {
                            Text("ตัดการเชื่อมต่อ", color = RedCritical, fontSize = 12.sp)
                        }
                    } else {
                        Button(
                            onClick = onConnectUsb,
                            colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
                            modifier = Modifier.testTag("btn_connect_usb")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Cable, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("เชื่อมต่อ USB OTG", color = DarkBackground, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            } else {
                // Simulator Scenario Selector
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(PurpleAi.copy(alpha = 0.2f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("VIRTUAL CAN", color = PurpleGlow, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("เลือกจำลองสถานการณ์:", color = TextSecondary, fontSize = 12.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    ScrollableTabRow(
                        selectedTabIndex = SimulatorScenario.values().indexOf(currentScenario),
                        edgePadding = 0.dp,
                        containerColor = Color.Transparent,
                        divider = {}
                    ) {
                        SimulatorScenario.values().forEach { scenario ->
                            Tab(
                                selected = scenario == currentScenario,
                                onClick = { onScenarioSelected(scenario) },
                                modifier = Modifier
                                    .padding(end = 6.dp)
                                    .testTag("sim_tab_${scenario.name}")
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (scenario == currentScenario) PurpleAi else SurfaceCard)
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = scenario.labelTh,
                                        color = if (scenario == currentScenario) TextPrimary else TextSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
