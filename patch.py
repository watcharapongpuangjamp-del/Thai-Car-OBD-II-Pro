import re

with open('app/src/main/java/com/example/model/Models.kt', 'r') as f:
    content = f.read()

replacement = """
enum class AdapterConnectionState(val labelEn: String, val isError: Boolean = false) {
    DISCONNECTED("Disconnected"),
    DEVICE_DETECTED("USB Device Detected"),
    PERMISSION_REQUIRED("USB Permission Required"),
    PERMISSION_GRANTED("USB Permission Granted"),
    USB_OPEN("USB Port Opened"),
    SERIAL_READY("Serial Communication Ready"),
    ADAPTER_HANDSHAKE("Adapter Handshake"),
    ADAPTER_RESPONDING("Adapter Responding"),
    
    PERMISSION_DENIED("USB Permission Denied", isError = true),
    USB_OPEN_FAILED("Failed to Open USB Port", isError = true),
    SERIAL_ERROR("Serial Configuration Error", isError = true),
    ADAPTER_NOT_RESPONDING("ELM327 Adapter Not Responding", isError = true),
    DEVICE_DISCONNECTED("USB Device Detached", isError = true),
    ERROR("Connection Error", isError = true)
}

enum class EcuConnectionState(val labelEn: String, val isError: Boolean = false) {
    NOT_STARTED("Not Started"),
    PROTOCOL_DETECTING("Detecting Protocol"),
    PROTOCOL_DETECTED("OBD Protocol Detected"),
    ECU_RESPONDING("ECU Responding"),
    LIVE_DATA_VALIDATED("Live Telemetry Validated"),
    CONNECTED("Fully Connected"),
    
    PROTOCOL_DETECTION_FAILED("OBD Protocol Detection Failed", isError = true),
    ECU_NOT_RESPONDING("ECU Not Responding (Check Ignition)", isError = true),
    TIMEOUT("Communication Timeout", isError = true),
    ERROR("Connection Error", isError = true)
}

enum class ConnectionState(val labelTh: String, val labelEn: String, val isError: Boolean = false) {
"""

content = content.replace("enum class ConnectionState(val labelTh: String, val labelEn: String, val isError: Boolean = false) {", replacement)

# Add adapter state and ECU state to LiveSensorData
old_live_sensor_data = """data class LiveSensorData(
    val isConnected: Boolean,
    val connectionState: ConnectionState,"""

new_live_sensor_data = """data class LiveSensorData(
    val isConnected: Boolean,
    val connectionState: ConnectionState,
    val adapterState: AdapterConnectionState = AdapterConnectionState.DISCONNECTED,
    val ecuState: EcuConnectionState = EcuConnectionState.NOT_STARTED,
    
    // Hardware diagnostics
    val usbVidPid: String = "",
    val usbDriver: String = "",
    val serialBaudRate: Int = 0,
    val vehicleBusBitrate: Int = 0,"""
content = content.replace(old_live_sensor_data, new_live_sensor_data)

old_disconnected = """fun disconnected(mode: AppOperationMode, message: String = "ยังไม่ได้เชื่อมต่ออุปกรณ์") = LiveSensorData(
            isConnected = false,
            connectionState = ConnectionState.DISCONNECTED,"""
new_disconnected = """fun disconnected(mode: AppOperationMode, message: String = "ยังไม่ได้เชื่อมต่ออุปกรณ์") = LiveSensorData(
            isConnected = false,
            connectionState = ConnectionState.DISCONNECTED,
            adapterState = AdapterConnectionState.DISCONNECTED,
            ecuState = EcuConnectionState.NOT_STARTED,"""
content = content.replace(old_disconnected, new_disconnected)

old_simulated = """fun simulated(
            rpm: Int,
            speed: Int,
            coolant: Int,
            voltage: Float,
            map: Int,
            boost: Float,
            fuelRate: Float,
            throttle: Int,
            intakeTemp: Int,
            load: Int
        ) = LiveSensorData(
            isConnected = true,
            connectionState = ConnectionState.CONNECTED,"""
new_simulated = """fun simulated(
            rpm: Int,
            speed: Int,
            coolant: Int,
            voltage: Float,
            map: Int,
            boost: Float,
            fuelRate: Float,
            throttle: Int,
            intakeTemp: Int,
            load: Int
        ) = LiveSensorData(
            isConnected = true,
            connectionState = ConnectionState.CONNECTED,
            adapterState = AdapterConnectionState.ADAPTER_RESPONDING,
            ecuState = EcuConnectionState.CONNECTED,"""
content = content.replace(old_simulated, new_simulated)

with open('app/src/main/java/com/example/model/Models.kt', 'w') as f:
    f.write(content)
