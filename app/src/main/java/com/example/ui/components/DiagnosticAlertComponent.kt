package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rules.DiagnosticAnomaly
import com.example.rules.DiagnosticCategory
import com.example.rules.EvaluationSeverity
import com.example.rules.RuleEngineReport
import com.example.ui.theme.*

/**
 * Reusable Jetpack Compose component that visualizes telemetry and diagnostic rule evaluations
 * from [com.example.rules.DiagnosticRuleEngine].
 *
 * Displays color-coded warning banners, severity badges, physical threshold validations,
 * and expandable anomaly root cause details with optional AI Mechanic consultation trigger.
 */
@Composable
fun DiagnosticAlertComponent(
    report: RuleEngineReport,
    modifier: Modifier = Modifier,
    onAnomalyClick: ((DiagnosticAnomaly) -> Unit)? = null,
    onConsultAiClick: (() -> Unit)? = null,
    initiallyExpanded: Boolean = true
) {
    var isExpanded by remember { mutableStateOf(initiallyExpanded) }

    val accentColor = when (report.overallSeverity) {
        EvaluationSeverity.CRITICAL, EvaluationSeverity.FAULT -> RedCritical
        EvaluationSeverity.WARNING -> AmberWarning
        EvaluationSeverity.INFO -> CyanPrimary
        EvaluationSeverity.NORMAL -> EmeraldConnected
    }

    val badgeIcon: ImageVector = when (report.overallSeverity) {
        EvaluationSeverity.CRITICAL, EvaluationSeverity.FAULT -> Icons.Default.Error
        EvaluationSeverity.WARNING -> Icons.Default.Warning
        EvaluationSeverity.INFO -> Icons.Default.Info
        EvaluationSeverity.NORMAL -> Icons.Default.CheckCircle
    }

    val badgeLabel = when (report.overallSeverity) {
        EvaluationSeverity.FAULT -> "เซนเซอร์ชำรุด (FAULT)"
        EvaluationSeverity.CRITICAL -> "ระดับวิกฤต (CRITICAL)"
        EvaluationSeverity.WARNING -> "แจ้งเตือนความปลอดภัย (WARNING)"
        EvaluationSeverity.INFO -> "ข้อมูลสถานะ (INFO)"
        EvaluationSeverity.NORMAL -> "ปลอดภัยตามเกณฑ์ (SAFE)"
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize()
            .border(
                width = if (report.overallSeverity == EvaluationSeverity.NORMAL) 1.dp else 1.5.dp,
                color = accentColor.copy(alpha = if (report.overallSeverity == EvaluationSeverity.NORMAL) 0.3f else 0.8f),
                shape = RoundedCornerShape(14.dp)
            )
            .testTag("diagnostic_alert_component"),
        colors = CardDefaults.cardColors(
            containerColor = SurfaceDark
        ),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = badgeIcon,
                            contentDescription = badgeLabel,
                            tint = accentColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Rule Engine Safety Shield",
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            // Status Badge
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(accentColor.copy(alpha = 0.2f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = badgeLabel,
                                    color = accentColor,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = if (report.anomalies.isEmpty()) {
                                "พิกัดความร้อน, แรงดันไฟ และบูสต์ผ่านเกณฑ์มาตรฐาน"
                            } else {
                                "พบ ${report.anomalies.size} รายการที่ต้องตรวจสอบ"
                            },
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                IconButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("toggle_alert_expansion")
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Toggle Details",
                        tint = TextSecondary
                    )
                }
            }

            // Summary description
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = report.summaryTh,
                color = if (report.overallSeverity == EvaluationSeverity.NORMAL) TextSecondary else TextPrimary,
                fontSize = 11.sp,
                lineHeight = 16.sp
            )

            // Expandable Anomalies Section
            AnimatedVisibility(
                visible = isExpanded && report.anomalies.isNotEmpty(),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                ) {
                    Divider(
                        color = SurfaceCard,
                        thickness = 1.dp,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )

                    Text(
                        text = "รายการความผิดปกติจากเกณฑ์พิกัดทางกายภาพ:",
                        color = CyanPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    report.anomalies.forEachIndexed { index, anomaly ->
                        AnomalyItemCard(
                            anomaly = anomaly,
                            index = index,
                            onClick = { onAnomalyClick?.invoke(anomaly) }
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    if (onConsultAiClick != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Button(
                            onClick = onConsultAiClick,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp)
                                .testTag("consult_ai_from_alert_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = PurpleAi),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "ส่งข้อมูลให้ AI Mechanic วิเคราะห์เชิงลึก",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AnomalyItemCard(
    anomaly: DiagnosticAnomaly,
    index: Int,
    onClick: () -> Unit
) {
    var showDetails by remember { mutableStateOf(false) }

    val itemColor = when (anomaly.severity) {
        EvaluationSeverity.CRITICAL, EvaluationSeverity.FAULT -> RedCritical
        EvaluationSeverity.WARNING -> AmberWarning
        else -> CyanPrimary
    }

    val categoryLabel = when (anomaly.category) {
        DiagnosticCategory.ENGINE_TEMPERATURE -> "อุณหภูมิเครื่องยนต์ (ECT)"
        DiagnosticCategory.ELECTRICAL_VOLTAGE -> "ระบบไฟฟ้า / ไดชาร์จ"
        DiagnosticCategory.FUEL_AND_LOAD -> "ระบบเชื้อเพลิง / บูสต์"
        DiagnosticCategory.SENSOR_PLAUSIBILITY -> "ความสอดคล้องของสัญญาณ"
        DiagnosticCategory.CORRELATED_DTC -> "รหัสข้อผิดพลาด DTC"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(DarkBackground.copy(alpha = 0.7f))
            .border(0.5.dp, itemColor.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            .clickable {
                showDetails = !showDetails
                onClick()
            }
            .padding(10.dp)
            .testTag("anomaly_item_card_$index")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(3.dp))
                    .background(itemColor.copy(alpha = 0.15f))
                    .padding(horizontal = 5.dp, vertical = 1.dp)
            ) {
                Text(
                    text = categoryLabel,
                    color = itemColor,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Text(
                text = anomaly.measuredValue,
                color = itemColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = anomaly.titleTh,
            color = TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "เกณฑ์ปกติ: ${anomaly.expectedRange}",
            color = TextMuted,
            fontSize = 10.sp
        )

        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = anomaly.descriptionTh,
            color = TextSecondary,
            fontSize = 10.sp,
            lineHeight = 14.sp
        )

        AnimatedVisibility(
            visible = showDetails,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(modifier = Modifier.padding(top = 6.dp)) {
                if (anomaly.potentialCausesTh.isNotEmpty()) {
                    Text(
                        text = "สาเหตุที่เป็นไปได้:",
                        color = AmberWarning,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    anomaly.potentialCausesTh.forEach { cause ->
                        Text(
                            text = "• $cause",
                            color = TextSecondary,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(start = 4.dp, top = 1.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "คำแนะนำช่าง: ${anomaly.recommendedActionTh}",
                    color = EmeraldConnected,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
