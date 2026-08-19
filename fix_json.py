with open('./app/src/main/java/com/example/repository/VehicleRepository.kt', 'r') as f:
    content = f.read()

if 'import org.json.JSONArray' not in content:
    content = content.replace('import android.content.Context', 'import android.content.Context\nimport org.json.JSONArray\nimport org.json.JSONObject')

with open('./app/src/main/java/com/example/repository/VehicleRepository.kt', 'w') as f:
    f.write(content)
