with open('app/src/main/java/com/example/ui/screens/AiMechanicScreen.kt', 'r') as f:
    content = f.read()

import_statement = "import android.content.Intent\nimport androidx.compose.ui.platform.LocalContext\nimport com.example.model.AiAnalysisResult\n"
content = content.replace("import com.example.model.AiAnalysisResult\n", import_statement)

button_code = """
                    if (aiResult.possibleRootCausesTh.isNotEmpty()) {
"""
replacement = """
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
"""
content = content.replace(button_code, replacement)

with open('app/src/main/java/com/example/ui/screens/AiMechanicScreen.kt', 'w') as f:
    f.write(content)
