with open('./app/src/main/java/com/example/repository/VehicleRepository.kt', 'r') as f:
    content = f.read()

import re

# Remove OkHttp and JSONObject imports if they exist, add Firebase imports
content = content.replace('import okhttp3.MediaType.Companion.toMediaType', '')
content = content.replace('import okhttp3.OkHttpClient', '')
content = content.replace('import okhttp3.Request', '')
content = content.replace('import okhttp3.RequestBody.Companion.toRequestBody', '')
content = content.replace('import org.json.JSONArray', '')
content = content.replace('import org.json.JSONObject', '')

if 'import com.google.firebase.Firebase' not in content:
    content = content.replace('import com.example.model.AiAnalysisResult', 'import com.example.model.AiAnalysisResult\nimport com.google.firebase.Firebase\nimport com.google.firebase.vertexai.vertexAI')

# Replace the analyzeWithAiMechanic function body
old_func_pattern = r'val apiKey = com\.example\.BuildConfig\.GEMINI_API_KEY.*?catch \(e: Exception\) \{.*?\}'
new_func_body = '''try {
            // Using Firebase Vertex AI (Secure AI Gateway)
            // No direct API keys in source code
            val generativeModel = Firebase.vertexAI.generativeModel("gemini-1.5-flash")
            val response = generativeModel.generateContent(promptText)
            val text = response.text ?: "ไม่สามารถดึงคำตอบจาก AI ได้"

            AiAnalysisResult(
                summaryTh = text,
                severityLevel = if (ruleReport.overallSeverity == com.example.rules.EvaluationSeverity.CRITICAL || dtcCodes.any { it.severity == com.example.model.DtcSeverity.CRITICAL }) "วิกฤต (Critical)" else if (ruleReport.overallSeverity == com.example.rules.EvaluationSeverity.WARNING) "เตือน (Warning)" else "ทั่วไป (Normal)",
                possibleRootCausesTh = if (ruleReport.anomalies.isNotEmpty()) ruleReport.anomalies.map { it.titleTh } else listOf("วิเคราะห์รวมโดย Gemini AI Mechanic"),
                recommendedActionsTh = if (ruleReport.anomalies.isNotEmpty()) ruleReport.anomalies.map { it.recommendedActionTh } else listOf("ปฏิบัติตามคำแนะนำของ AI หรือนำรถเข้าศูนย์บริการช่างใกล้บ้าน"),
                provenanceLabel = modeTag,
                rawPromptUsed = promptText,
                ruleReport = ruleReport
            )
        } catch (e: Exception) {
            val severityLabel = when (ruleReport.overallSeverity) {
                com.example.rules.EvaluationSeverity.CRITICAL -> "วิกฤต (Critical)"
                com.example.rules.EvaluationSeverity.FAULT -> "เซนเซอร์ชำรุด (Fault)"
                com.example.rules.EvaluationSeverity.WARNING -> "เตือน (Warning)"
                com.example.rules.EvaluationSeverity.INFO -> "ข้อมูล (Info)"
                com.example.rules.EvaluationSeverity.NORMAL -> "ปกติ (Normal)"
            }
            AiAnalysisResult(
                summaryTh = "วิเคราะห์ระบบผ่าน Diagnostic Rule Engine เรียบร้อยแล้ว (${modeTag}) (AI Unavailable): ${ruleReport.summaryTh}",
                severityLevel = severityLabel,
                possibleRootCausesTh = if (ruleReport.anomalies.isNotEmpty()) {
                    ruleReport.anomalies.flatMap { it.potentialCausesTh }
                } else if (dtcCodes.isNotEmpty()) {
                    listOf("รหัส DTC ${dtcCodes.first().code}: ${dtcCodes.first().descriptionTh}", "ความผิดปกติในระบบเซนเซอร์วัดค่า")
                } else {
                    listOf("ไม่พบสาเหตุผิดปกติร้ายแรง")
                },
                recommendedActionsTh = if (ruleReport.anomalies.isNotEmpty()) {
                    ruleReport.anomalies.map { it.recommendedActionTh }
                } else {
                    listOf("ตรวจสอบขั้วปลั๊กและสายไฟที่เกี่ยวข้อง", "ทำความสะอาดเซนเซอร์และทดสอบลบลบโค้ด DTC")
                },
                provenanceLabel = modeTag,
                rawPromptUsed = promptText,
                ruleReport = ruleReport
            )
        }'''

content = re.sub(old_func_pattern, new_func_body, content, flags=re.DOTALL)

with open('./app/src/main/java/com/example/repository/VehicleRepository.kt', 'w') as f:
    f.write(content)
