import re

with open('./app/src/main/java/com/example/hardware/obd/RealDtcScanner.kt', 'r') as f:
    content = f.read()

# Replace resolveModuleFromHeader
new_content = re.sub(
    r'private fun resolveModuleFromHeader.*?\}',
    'private fun resolveModuleFromHeader(header: String): String {\n        return "ECU_${header.uppercase()}"\n    }',
    content,
    flags=re.DOTALL
)

with open('./app/src/main/java/com/example/hardware/obd/RealDtcScanner.kt', 'w') as f:
    f.write(new_content)
