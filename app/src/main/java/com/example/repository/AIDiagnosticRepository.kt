package com.example.repository

import com.example.model.VehicleInfo
import com.google.firebase.vertexai.GenerativeModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository responsible for generating diagnostic analysis using Gemini AI.
 * It combines real-time OBD-II data, vehicle profile, and cloud-sourced repair manuals
 * to provide context-aware, human-readable repair advice.
 */
class AIDiagnosticRepository(
    private val generativeModel: GenerativeModel
) {

    /**
     * Analyzes a DTC based on vehicle profile and cloud-sourced manual context.
     * 
     * @param dtc The OBD-II code scanned from the vehicle (e.g., P0301)
     * @param vehicle The profile of the car (brand, model, year)
     * @param repairManualContext The content retrieved from Google Drive/Cloud knowledge base
     * @return Result containing the AI-generated Thai repair guide
     */
    suspend fun analyzeDtc(
        dtc: String,
        vehicle: VehicleInfo,
        repairManualContext: String?
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val prompt = buildPrompt(dtc, vehicle, repairManualContext)
            
            // Generate content using the Firebase Vertex AI SDK
            val response = generativeModel.generateContent(prompt)
            
            Result.success(response.text ?: "ไม่สามารถสร้างคำแนะนำการซ่อมได้ในขณะนี้")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun buildPrompt(
        dtc: String,
        vehicle: VehicleInfo,
        repairManualContext: String?
    ): String {
        return """
            คุณคือช่างยนต์ผู้เชี่ยวชาญในประเทศไทยที่มีประสบการณ์สูง
            โปรดวิเคราะห์รหัสปัญหา DTC: $dtc 
            สำหรับรถยนต์: ${vehicle.brand} ${vehicle.model} ปี ${vehicle.year}
            
            แหล่งข้อมูล: ระบบวิเคราะห์ Real-time จากรถยนต์จริงโดย OBD-II Adapter
            
            [ข้อมูลอ้างอิงจากคู่มือซ่อมเฉพาะรุ่น (Cloud Knowledge Base)]:
            ${repairManualContext ?: "ไม่มีข้อมูลคู่มือซ่อมเฉพาะรุ่นนี้ในระบบ โปรดใช้หลักการวิเคราะห์มาตรฐานตามประสบการณ์ช่างผู้เชี่ยวชาญ"}
            
            โปรดตอบกลับเป็นภาษาไทยโดยละเอียดตามโครงสร้างต่อไปนี้:
            1. อธิบายสาเหตุของรหัสปัญหา $dtc ในบริบทของรถรุ่นนี้
            2. ขั้นตอนการตรวจสอบและแก้ไขปัญหาตามคู่มือซ่อมที่ให้มา (หรือตามหลักสากลหากไม่มีคู่มือ)
            3. ข้อควรระวังในการซ่อมแซมสำหรับรถรุ่นนี้
            4. ประเมินราคาค่าแรงช่างโดยประมาณในประเทศไทย (ระบุช่วงราคาเป็นบาทไทย)
            
            หากข้อมูลจากคู่มือซ่อมไม่เพียงพอ หรือรหัสนี้มีความเสี่ยงสูง ให้ระบุคำเตือนที่ชัดเจนว่าควรนำรถเข้าตรวจที่ศูนย์บริการ
            """.trimIndent()
    }
}
