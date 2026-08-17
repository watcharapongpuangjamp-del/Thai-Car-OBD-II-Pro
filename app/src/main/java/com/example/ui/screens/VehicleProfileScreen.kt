package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.db.VehicleProfileEntity
import com.example.ui.theme.*

@Composable
fun VehicleProfileScreen(
    profiles: List<VehicleProfileEntity>,
    selectedProfile: VehicleProfileEntity?,
    onSelectProfile: (VehicleProfileEntity) -> Unit,
    onAddProfile: (name: String, make: String, model: String, year: Int, engine: String, plate: String, mileage: Int) -> Unit,
    onAddMaintenanceLog: (title: String, cost: Double, mileage: Int, category: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddVehicleDialog by remember { mutableStateOf(false) }
    var showAddLogDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
            .testTag("vehicle_profile_screen")
    ) {
        // Vehicle Header
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
                        Icon(
                            imageVector = Icons.Default.DirectionsCar,
                            contentDescription = null,
                            tint = CyanPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "โปรไฟล์รถยนต์ (Vehicle Profile)",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    Button(
                        onClick = { showAddVehicleDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
                        modifier = Modifier.testTag("btn_add_new_vehicle")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("เพิ่มรถยนต์", fontSize = 11.sp, color = DarkBackground, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (selectedProfile != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = selectedProfile.name,
                                color = CyanPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${selectedProfile.make} ${selectedProfile.model} (ปี ${selectedProfile.year})",
                                color = TextPrimary,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "เครื่องยนต์: ${selectedProfile.engineType} | ทะเบียน: ${selectedProfile.licensePlate}",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "เลขกิโลเมตรสะสม: ${selectedProfile.odometerKm} กม.",
                                color = TextMuted,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Vehicle List Selectors
        Text(
            text = "รายการรถยนต์ที่ลงทะเบียนในระบบ Room DB (${profiles.size} คัน):",
            color = TextSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        profiles.forEach { profile ->
            val isSelected = profile.id == selectedProfile?.id
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable { onSelectProfile(profile) }
                    .testTag("vehicle_card_${profile.id}"),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) SurfaceCard else SurfaceDark
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = profile.name,
                            color = if (isSelected) CyanPrimary else TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "${profile.make} ${profile.model} - ${profile.licensePlate}",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }

                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(CyanPrimary)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("กำลังเลือก", color = DarkBackground, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Maintenance Action Button
        Button(
            onClick = { showAddLogDialog = true },
            colors = ButtonDefaults.buttonColors(containerColor = SurfaceCard),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("btn_add_maintenance_log")
        ) {
            Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = CyanPrimary)
            Spacer(modifier = Modifier.width(8.dp))
            Text("บันทึกประวัติการซ่อมบำรุง / เข้าศูนย์", color = TextPrimary, fontWeight = FontWeight.Bold)
        }
    }

    // Add Vehicle Dialog
    if (showAddVehicleDialog) {
        var name by remember { mutableStateOf("") }
        var make by remember { mutableStateOf("") }
        var model by remember { mutableStateOf("") }
        var year by remember { mutableStateOf("2024") }
        var engine by remember { mutableStateOf("") }
        var plate by remember { mutableStateOf("") }
        var odometer by remember { mutableStateOf("25000") }

        AlertDialog(
            onDismissRequest = { showAddVehicleDialog = false },
            title = { Text("เพิ่มรถยนต์คันใหม่ (Room DB)", color = TextPrimary) },
            text = {
                Column {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("ชื่อเรียกคันนี้") })
                    OutlinedTextField(value = make, onValueChange = { make = it }, label = { Text("ยี่ห้อ (เช่น Toyota, Honda)") })
                    OutlinedTextField(value = model, onValueChange = { model = it }, label = { Text("รุ่น (เช่น Civic, Hilux)") })
                    OutlinedTextField(value = year, onValueChange = { year = it }, label = { Text("ปี ค.ศ.") })
                    OutlinedTextField(value = engine, onValueChange = { engine = it }, label = { Text("ขนาด/รหัสเครื่องยนต์") })
                    OutlinedTextField(value = plate, onValueChange = { plate = it }, label = { Text("ทะเบียนรถ") })
                    OutlinedTextField(value = odometer, onValueChange = { odometer = it }, label = { Text("เลขไมล์ปัจจุบัน (กม.)") })
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank() && make.isNotBlank()) {
                            onAddProfile(
                                name,
                                make,
                                model,
                                year.toIntOrNull() ?: 2024,
                                engine,
                                plate,
                                odometer.toIntOrNull() ?: 0
                            )
                            showAddVehicleDialog = false
                        }
                    }
                ) {
                    Text("บันทึก")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddVehicleDialog = false }) {
                    Text("ยกเลิก")
                }
            }
        )
    }

    // Add Maintenance Log Dialog
    if (showAddLogDialog) {
        var title by remember { mutableStateOf("เปลี่ยนถ่ายน้ำมันเครื่องสังเคราะห์") }
        var cost by remember { mutableStateOf("2400") }
        var mileage by remember { mutableStateOf("48000") }
        var category by remember { mutableStateOf("Oil Change") }

        AlertDialog(
            onDismissRequest = { showAddLogDialog = false },
            title = { Text("บันทึกการซ่อมบำรุง (Maintenance Log)", color = TextPrimary) },
            text = {
                Column {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("รายการซ่อม/เช็กระยะ") })
                    OutlinedTextField(value = cost, onValueChange = { cost = it }, label = { Text("ค่าใช้จ่าย (บาท)") })
                    OutlinedTextField(value = mileage, onValueChange = { mileage = it }, label = { Text("เลขไมล์ตอนเปลี่ยน (กม.)") })
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            onAddMaintenanceLog(
                                title,
                                cost.toDoubleOrNull() ?: 0.0,
                                mileage.toIntOrNull() ?: 0,
                                category
                            )
                            showAddLogDialog = false
                        }
                    }
                ) {
                    Text("บันทึก")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddLogDialog = false }) {
                    Text("ยกเลิก")
                }
            }
        )
    }
}
