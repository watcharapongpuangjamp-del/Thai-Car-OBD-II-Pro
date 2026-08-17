package com.example.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.screens.*
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.PurpleGlow
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.MainViewModel

sealed class Screen(val route: String, val titleTh: String, val icon: ImageVector) {
    object Dashboard : Screen("dashboard", "หน้าวัดค่า", Icons.Default.Speed)
    object DtcScan : Screen("dtc_scan", "สแกนโค้ด", Icons.Default.QrCodeScanner)
    object AiMechanic : Screen("ai_mechanic", "AI ช่างยนต์", Icons.Default.AutoAwesome)
    object Predictive : Screen("predictive", "คาดการณ์", Icons.Default.HourglassTop)
    object Profile : Screen("profile", "ข้อมูลรถ", Icons.Default.DirectionsCar)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThaiObdApp(viewModel: MainViewModel) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Dashboard) }

    val activeMode by viewModel.activeMode.collectAsStateWithLifecycle()
    val telemetry by viewModel.liveTelemetry.collectAsStateWithLifecycle()
    val dtcCodes by viewModel.dtcCodes.collectAsStateWithLifecycle()
    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()
    val aiResult by viewModel.aiResult.collectAsStateWithLifecycle()
    val isAiAnalyzing by viewModel.isAiAnalyzing.collectAsStateWithLifecycle()
    val profiles by viewModel.vehicleProfiles.collectAsStateWithLifecycle()
    val selectedProfile by viewModel.selectedProfile.collectAsStateWithLifecycle()
    val currentScenario by viewModel.currentScenario.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Thai Car OBD-II Pro",
                            fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            color = TextPrimary
                        )
                        Text(
                            text = "v2.5.0 Production Pro",
                            fontSize = 11.sp,
                            color = CyanPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = SurfaceDark,
                contentColor = TextPrimary
            ) {
                listOf(
                    Screen.Dashboard,
                    Screen.DtcScan,
                    Screen.AiMechanic,
                    Screen.Predictive,
                    Screen.Profile
                ).forEach { screen ->
                    val isSelected = currentScreen.route == screen.route
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentScreen = screen },
                        icon = {
                            Icon(
                                imageVector = screen.icon,
                                contentDescription = screen.titleTh,
                                tint = if (isSelected) CyanPrimary else TextSecondary
                            )
                        },
                        label = {
                            Text(
                                text = screen.titleTh,
                                color = if (isSelected) CyanPrimary else TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal
                            )
                        },
                        modifier = Modifier.testTag("nav_tab_${screen.route}")
                    )
                }
            }
        },
        containerColor = DarkBackground
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (currentScreen) {
                Screen.Dashboard -> DashboardScreen(
                    telemetry = telemetry,
                    activeMode = activeMode,
                    currentScenario = currentScenario,
                    onModeSelected = { viewModel.setMode(it) },
                    onConnectUsb = { viewModel.connectUsbHardware() },
                    onDisconnectUsb = { viewModel.disconnectUsbHardware() },
                    onScenarioSelected = { viewModel.setSimulatorScenario(it) }
                )
                Screen.DtcScan -> DtcScannerScreen(
                    dtcCodes = dtcCodes,
                    isScanning = isScanning,
                    activeMode = activeMode,
                    onStartScan = { viewModel.scanDtcs() },
                    onClearDtcs = { viewModel.clearDtcs() },
                    onNavigateToAiMechanic = { currentScreen = Screen.AiMechanic }
                )
                Screen.AiMechanic -> AiMechanicScreen(
                    aiResult = aiResult,
                    isAiAnalyzing = isAiAnalyzing,
                    telemetry = telemetry,
                    dtcCodes = dtcCodes,
                    onRequestAiAnalysis = { viewModel.requestAiMechanicAnalysis() }
                )
                Screen.Predictive -> PredictiveMaintenanceScreen(
                    items = viewModel.getPredictiveMaintenanceItems()
                )
                Screen.Profile -> VehicleProfileScreen(
                    profiles = profiles,
                    selectedProfile = selectedProfile,
                    onSelectProfile = { viewModel.selectVehicleProfile(it) },
                    onAddProfile = { name, make, model, year, engine, plate, mileage ->
                        viewModel.addVehicleProfile(name, make, model, year, engine, plate, mileage)
                    },
                    onAddMaintenanceLog = { title, cost, mileage, category ->
                        viewModel.addMaintenanceLog(title, cost, mileage, category)
                    }
                )
            }
        }
    }
}
