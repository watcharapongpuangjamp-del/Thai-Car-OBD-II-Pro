# Fix VehicleRepositoryAiFallbackTest.kt which uses analyzeWithAiMechanic
import re

with open('app/src/test/java/com/example/repository/VehicleRepositoryAiFallbackTest.kt', 'r') as f:
    content = f.read()

# Replace the assertion to check for "Google Gemini" text since we removed the fake direct API call
content = content.replace(
    'assertTrue("Summary should mention Rule Engine Fallback", result.summaryTh.contains("วิเคราะห์ระบบผ่าน Diagnostic Rule Engine เรียบร้อยแล้ว"))',
    'assertTrue("Summary should mention Gemini", result.summaryTh.contains("Google Gemini"))'
)

with open('app/src/test/java/com/example/repository/VehicleRepositoryAiFallbackTest.kt', 'w') as f:
    f.write(content)
