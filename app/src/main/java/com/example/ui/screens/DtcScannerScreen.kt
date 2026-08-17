package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Warning
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
import com.example.model.AppOperationMode
import com.example.model.DtcCode
import com.example.model.DtcSeverity
import com.example.ui.theme.*

@Composable
fun DtcScannerScreen(
    dtcCodes: List<DtcCode>,
    isScanning: Boolean,
    activeMode: AppOperationMode,
    onStartScan: () -> Unit,
    onClearDtcs: () -> Unit,
    onNavigateToAiMechanic: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp)
            .testTag("dtc_scanner_screen")
    ) {
        // Module Status Banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ระบบวินิจฉัยรถยนต์ (Multi-Module Scan)",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (activeMode == AppOperationMode.REAL_HARDWARE) EmeraldConnected.copy(0.2f) else PurpleAi.copy(0.2f))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (activeMode == AppOperationMode.REAL_HARDWARE) "REAL ECU SCAN" else "SIMULATOR SCAN",
                            color = if (activeMode == AppOperationMode.REAL_HARDWARE) EmeraldConnected else PurpleGlow,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf("ECM เครื่อง", "TCM เกียร์", "ABS เบรก", "SRS ถุงลม", "BCM ตัวถัง").forEach { moduleName ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(SurfaceCard)
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = moduleName,
                                color = TextSecondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Scan Controls Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onStartScan,
                enabled = !isScanning,
                colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
                modifier = Modifier
                    .weight(1f)
                    .testTag("btn_start_dtc_scan")
            ) {
                if (isScanning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = DarkBackground,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("กำลังสแกน ECU...", color = DarkBackground, fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan DTC", modifier = Modifier.size(18.dp), tint = DarkBackground)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("สแกนรหัส DTC ทั้งหมด", color = DarkBackground, fontWeight = FontWeight.Bold)
                }
            }

            if (dtcCodes.isNotEmpty()) {
                Spacer(modifier = Modifier.width(12.dp))
                OutlinedButton(
                    onClick = onClearDtcs,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = RedCritical),
                    modifier = Modifier.testTag("btn_clear_dtc_codes")
                ) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = "Clear DTCs", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("ลบโค้ด")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // DTC Results List
        if (dtcCodes.isEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "No DTCs",
                        tint = EmeraldConnected,
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "ไม่พบรหัสความผิดปกติ (No DTC Codes Found)",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "ระบบกล่องควบคุม ECU เครื่องยนต์, เกียร์, ABS และระบบไฟฟ้าทำงานในเกณฑ์ปกติ",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            // DTC Codes List View
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "พบข้อผิดปกติทั้งหมด ${dtcCodes.size} รหัส:",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )

                    Button(
                        onClick = onNavigateToAiMechanic,
                        colors = ButtonDefaults.buttonColors(containerColor = PurpleAi),
                        modifier = Modifier.testTag("btn_ask_ai_mechanic")
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("ส่งให้ AI Mechanic วิเคราะห์", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(dtcCodes) { dtc ->
                        val severityColor = when (dtc.severity) {
                            DtcSeverity.CRITICAL -> RedCritical
                            DtcSeverity.WARNING -> AmberWarning
                            DtcSeverity.INFO -> CyanPrimary
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("dtc_item_${dtc.code}"),
                            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(severityColor.copy(0.2f))
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = dtc.code,
                                                color = severityColor,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 16.sp
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = dtc.module,
                                            color = TextSecondary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(SurfaceCard)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = dtc.modeProvenance.name,
                                            color = TextMuted,
                                            fontSize = 10.sp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = dtc.descriptionTh,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = dtc.descriptionEn,
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
