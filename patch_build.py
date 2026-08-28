with open('app/build.gradle.kts', 'r') as f:
    content = f.read()

content = content.replace('  buildFeatures {\n    compose = true\n    buildConfig = true\n  }', '  buildFeatures {\n    compose = true\n  }')
content = content.replace('  implementation(libs.google.ai.generativeai)\n', '')

with open('app/build.gradle.kts', 'w') as f:
    f.write(content)

import os
if os.path.exists('.env.example'):
    os.remove('.env.example')
if os.path.exists('.env'):
    os.remove('.env')
