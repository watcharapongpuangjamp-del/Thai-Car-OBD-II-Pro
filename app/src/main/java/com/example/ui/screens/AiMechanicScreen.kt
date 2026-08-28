package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Psychology
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
import android.content.Intent
import androidx.compose.ui.platform.LocalContext
import com.example.model.AiAnalysisResult
import com.example.model.AppOperationMode
import com.example.model.DtcCode
import com.example.model.LiveSensorData
import com.example.ui.theme.*

@Composable
fun AiMechanicScreen(
    aiResult: AiAnalysisResult?,
    isAiAnalyzing: Boolean,
    telemetry: LiveSensorData,
    dtcCodes: List<DtcCode>,
    onRequestAiAnalysis: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
            .testTag("ai_mechanic_screen")
    ) {
        // AI Hero Banner
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(PurpleAi.copy(0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "AI Mechanic",
                                tint = PurpleGlow,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Gemini AI Mechanic",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "ผู้ช่วยช่างยนต์อัจฉริยะภาษาไทย",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (telemetry.mode == AppOperationMode.REAL_HARDWARE) EmeraldConnected.copy(0.2f) else PurpleAi.copy(0.2f)
                            )
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = if (telemetry.mode == AppOperationMode.REAL_HARDWARE) "REAL HARDWARE" else "SIMULATOR",
                            color = if (telemetry.mode == AppOperationMode.REAL_HARDWARE) EmeraldConnected else PurpleGlow,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "วิเคราะห์ข้อมูล ECU, ค่าความร้อน, แรงดันไฟ และโค้ดปัญหารถยนต์ด้วยระบบประมวลผล Gemini AI อัจฉริยะ",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = onRequestAiAnalysis,
                    enabled = !isAiAnalyzing,
                    colors = ButtonDefaults.buttonColors(containerColor = PurpleAi),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_request_ai_analysis")
                ) {
                    if (isAiAnalyzing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = TextPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("กำลังประมวลผลการวิเคราะห์...", color = TextPrimary, fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Default.Psychology, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("เริ่มการวิเคราะห์ปัญหาด้วย Gemini AI", color = TextPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // AI Results Card Display
        if (aiResult != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ai_result_card"),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Severity",
                                tint = AmberWarning,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "ระดับการประเมิน: ${aiResult.severityLevel}",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(SurfaceCard)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Data Source: ${aiResult.provenanceLabel}",
                                color = TextMuted,
                                fontSize = 10.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = SurfaceCard)
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "สรุปผลการวิเคราะห์ (AI Mechanic Overview):",
                        color = CyanPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = aiResult.summaryTh,
                        color = TextPrimary,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Independent Diagnostic Rule Engine Validation Box
                    aiResult.ruleReport?.let { report ->
                        com.example.ui.components.DiagnosticAlertComponent(
                            report = report,
                            initiallyExpanded = report.anomalies.isNotEmpty()
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    val context = LocalContext.current
                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, aiResult.rawPromptUsed)
                                setPackage("com.google.android.apps.bard") // Intent to official Gemini app
                            }
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                // Fallback to generic share if Gemini app not installed
                                val chooser = Intent.createChooser(intent.apply { setPackage(null) }, "ส่งข้อมูลให้ AI Mechanic (Gemini)")
                                context.startActivity(chooser)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PurpleAi),
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                    ) {
                        Icon(Icons.Default.Psychology, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("ส่งวิเคราะห์ด้วยแอป Google Gemini", color = TextPrimary, fontWeight = FontWeight.Bold)
                    }

                    if (aiResult.possibleRootCausesTh.isNotEmpty()) {
                        Text(
                            text = "สาเหตุที่เป็นไปได้ (Possible Causes):",
                            color = AmberWarning,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        aiResult.possibleRootCausesTh.forEach { cause ->
                            Row(modifier = Modifier.padding(vertical = 2.dp)) {
                                Text("• ", color = AmberWarning, fontWeight = FontWeight.Bold)
                                Text(cause, color = TextSecondary, fontSize = 12.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (aiResult.recommendedActionsTh.isNotEmpty()) {
                        Text(
                            text = "แนวทางแก้ไขและวิธีซ่อมแซม (Recommended Actions):",
                            color = EmeraldConnected,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        aiResult.recommendedActionsTh.forEach { action ->
                            Row(modifier = Modifier.padding(vertical = 2.dp)) {
                                Icon(
                                    Icons.Default.Build,
                                    contentDescription = null,
                                    tint = EmeraldConnected,
                                    modifier = Modifier
                                        .size(14.dp)
                                        .padding(top = 2.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(action, color = TextPrimary, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape = RoundedCornerShape(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "กดปุ่ม 'เริ่มการวิเคราะห์ปัญหาด้วย Gemini AI' ด้านบน เพื่อเริ่มต้นวิเคราะห์สุขภาพรถยนต์ของคุณ",
                        color = TextMuted,
                        fontSize = 12.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
    }
}
