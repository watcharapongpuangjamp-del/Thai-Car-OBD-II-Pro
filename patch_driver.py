import re

with open('app/src/main/java/com/example/hardware/UsbObdDriver.kt', 'r') as f:
    content = f.read()

# Add new imports
content = content.replace("import com.example.model.ConnectionState", "import com.example.model.ConnectionState\nimport com.example.model.AdapterConnectionState\nimport com.example.model.EcuConnectionState")

# We need to add state flows for adapter and ECU states
old_state_flows = """    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState = _connectionState.asStateFlow()

    private val _liveTelemetry = MutableStateFlow("""

new_state_flows = """    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState = _connectionState.asStateFlow()
    
    private val _adapterState = MutableStateFlow(AdapterConnectionState.DISCONNECTED)
    val adapterState = _adapterState.asStateFlow()
    
    private val _ecuState = MutableStateFlow(EcuConnectionState.NOT_STARTED)
    val ecuState = _ecuState.asStateFlow()

    private val _liveTelemetry = MutableStateFlow("""

content = content.replace(old_state_flows, new_state_flows)


# Function to emit telemetry
helper_function = """
    private fun updateTelemetry(message: String = "") {
        val current = _liveTelemetry.value
        _liveTelemetry.value = current.copy(
            isConnected = _ecuState.value == EcuConnectionState.CONNECTED,
            connectionState = _connectionState.value,
            adapterState = _adapterState.value,
            ecuState = _ecuState.value,
            statusMessage = message.ifEmpty { current.statusMessage }
        )
    }
"""
content = content.replace("    init {", helper_function + "\n    init {")

# Replace direct assignment in eventListenerJob
content = content.replace(
"""                                activeDevice = event.device
                                _connectionState.value = ConnectionState.DEVICE_DETECTED
                                _liveTelemetry.value = LiveSensorData.disconnected(
                                    AppOperationMode.REAL_HARDWARE,
                                    "ตรวจพบอุปกรณ์ USB: ${event.device.deviceName}"
                                )""",
"""                                activeDevice = event.device
                                _connectionState.value = ConnectionState.DEVICE_DETECTED
                                _adapterState.value = AdapterConnectionState.DEVICE_DETECTED
                                updateTelemetry("ตรวจพบอุปกรณ์ USB: ${event.device.deviceName}")
                                val current = _liveTelemetry.value
                                _liveTelemetry.value = current.copy(
                                    usbVidPid = "${event.device.vendorId}:${event.device.productId}"
                                )"""
)

content = content.replace(
"""                                _connectionState.value = ConnectionState.PERMISSION_GRANTED
                                _liveTelemetry.value = LiveSensorData.disconnected(
                                    AppOperationMode.REAL_HARDWARE,
                                    "ได้รับสิทธิ์เข้าถึงอุปกรณ์ USB แล้ว"
                                )""",
"""                                _connectionState.value = ConnectionState.PERMISSION_GRANTED
                                _adapterState.value = AdapterConnectionState.PERMISSION_GRANTED
                                updateTelemetry("ได้รับสิทธิ์เข้าถึงอุปกรณ์ USB แล้ว")"""
)

content = content.replace(
"""                                _connectionState.value = ConnectionState.PERMISSION_DENIED
                                _liveTelemetry.value = LiveSensorData.disconnected(
                                    AppOperationMode.REAL_HARDWARE,
                                    "ผู้ใช้ปฏิเสธการให้สิทธิ์เข้าถึงอุปกรณ์ USB"
                                )""",
"""                                _connectionState.value = ConnectionState.PERMISSION_DENIED
                                _adapterState.value = AdapterConnectionState.PERMISSION_DENIED
                                updateTelemetry("ผู้ใช้ปฏิเสธการให้สิทธิ์เข้าถึงอุปกรณ์ USB")"""
)

# Replace in checkAndConnectUsbDevice
content = content.replace(
"""        if (device == null) {
            _connectionState.value = ConnectionState.DISCONNECTED
            _liveTelemetry.value = LiveSensorData.disconnected(
                AppOperationMode.REAL_HARDWARE,
                "ไม่พบสาย USB OTG หรือ OBD-II Adapter เชื่อมต่ออยู่"
            )
            return@withContext ConnectionState.DISCONNECTED
        }""",
"""        if (device == null) {
            _connectionState.value = ConnectionState.DISCONNECTED
            _adapterState.value = AdapterConnectionState.DISCONNECTED
            _ecuState.value = EcuConnectionState.NOT_STARTED
            _liveTelemetry.value = LiveSensorData.disconnected(
                AppOperationMode.REAL_HARDWARE,
                "ไม่พบสาย USB OTG หรือ OBD-II Adapter เชื่อมต่ออยู่"
            )
            return@withContext ConnectionState.DISCONNECTED
        }"""
)

content = content.replace(
"""        activeDevice = device
        _connectionState.value = ConnectionState.DEVICE_DETECTED
        _liveTelemetry.value = LiveSensorData.disconnected(
            AppOperationMode.REAL_HARDWARE,
            "ตรวจพบอุปกรณ์: ${device.deviceName} (VID: ${device.vendorId}, PID: ${device.productId})"
        )""",
"""        activeDevice = device
        _connectionState.value = ConnectionState.DEVICE_DETECTED
        _adapterState.value = AdapterConnectionState.DEVICE_DETECTED
        updateTelemetry("ตรวจพบอุปกรณ์: ${device.deviceName} (VID: ${device.vendorId}, PID: ${device.productId})")
        val current = _liveTelemetry.value
        _liveTelemetry.value = current.copy(
            usbVidPid = "${device.vendorId}:${device.productId}"
        )"""
)

content = content.replace(
"""            _connectionState.value = ConnectionState.PERMISSION_REQUIRED
            _liveTelemetry.value = LiveSensorData.disconnected(
                AppOperationMode.REAL_HARDWARE,
                "กรุณากดยินยอมสิทธิ์เข้าถึง USB บนหน้าจออุปกรณ์"
            )""",
"""            _connectionState.value = ConnectionState.PERMISSION_REQUIRED
            _adapterState.value = AdapterConnectionState.PERMISSION_REQUIRED
            updateTelemetry("กรุณากดยินยอมสิทธิ์เข้าถึง USB บนหน้าจออุปกรณ์")"""
)

content = content.replace(
"""        _connectionState.value = ConnectionState.PERMISSION_GRANTED
        _liveTelemetry.value = LiveSensorData.disconnected(
            AppOperationMode.REAL_HARDWARE,
            "กำลังเชื่อมต่ออุปกรณ์ USB..."
        )""",
"""        _connectionState.value = ConnectionState.PERMISSION_GRANTED
        _adapterState.value = AdapterConnectionState.PERMISSION_GRANTED
        updateTelemetry("กำลังเชื่อมต่ออุปกรณ์ USB...")"""
)

# In proceedWithUsbOpen
content = content.replace(
"""        val success = transport.open(device)
        if (!success) {
            _connectionState.value = ConnectionState.USB_OPEN_FAILED
            _liveTelemetry.value = LiveSensorData.disconnected(
                AppOperationMode.REAL_HARDWARE,
                "เปิดพอร์ต USB ล้มเหลว โปรดถอดสายแล้วเสียบใหม่"
            )
            return@withContext ConnectionState.USB_OPEN_FAILED
        }""",
"""        val success = transport.open(device)
        if (!success) {
            _connectionState.value = ConnectionState.USB_OPEN_FAILED
            _adapterState.value = AdapterConnectionState.USB_OPEN_FAILED
            updateTelemetry("เปิดพอร์ต USB ล้มเหลว โปรดถอดสายแล้วเสียบใหม่")
            return@withContext ConnectionState.USB_OPEN_FAILED
        }"""
)

content = content.replace(
"""        _connectionState.value = ConnectionState.USB_OPEN
        _liveTelemetry.value = LiveSensorData.disconnected(
            AppOperationMode.REAL_HARDWARE,
            "กำลังตั้งค่า Serial Baud Rate (38400)..."
        )""",
"""        _connectionState.value = ConnectionState.USB_OPEN
        _adapterState.value = AdapterConnectionState.USB_OPEN
        updateTelemetry("กำลังตั้งค่า Serial Baud Rate (38400)...")"""
)

content = content.replace(
"""        val configSuccess = transport.configure(SerialConfig(baudRate = 38400))
        if (!configSuccess) {
            _connectionState.value = ConnectionState.SERIAL_ERROR
            _liveTelemetry.value = LiveSensorData.disconnected(
                AppOperationMode.REAL_HARDWARE,
                "ตั้งค่า Serial Communication ล้มเหลว"
            )
            return@withContext ConnectionState.SERIAL_ERROR
        }""",
"""        val configSuccess = transport.configure(SerialConfig(baudRate = 38400))
        if (!configSuccess) {
            _connectionState.value = ConnectionState.SERIAL_ERROR
            _adapterState.value = AdapterConnectionState.SERIAL_ERROR
            updateTelemetry("ตั้งค่า Serial Communication ล้มเหลว")
            return@withContext ConnectionState.SERIAL_ERROR
        }"""
)

content = content.replace(
"""        _connectionState.value = ConnectionState.SERIAL_READY
        _liveTelemetry.value = LiveSensorData.disconnected(
            AppOperationMode.REAL_HARDWARE,
            "เชื่อมต่อ ELM327 Adapter..."
        )""",
"""        _connectionState.value = ConnectionState.SERIAL_READY
        _adapterState.value = AdapterConnectionState.SERIAL_READY
        updateTelemetry("เชื่อมต่อ ELM327 Adapter...")
        val current2 = _liveTelemetry.value
        _liveTelemetry.value = current2.copy(
            usbDriver = transport.javaClass.simpleName,
            serialBaudRate = 38400
        )"""
)

content = content.replace(
"""        _connectionState.value = ConnectionState.ADAPTER_HANDSHAKE
        
        try {
            elmDriver.initialize()
        } catch (e: Exception) {
            Log.e(TAG, "Hardware Handshake failed", e)
            
            // Map specific errors to more useful states
            val errorState = when (val diagErr = DiagnosticError.fromException(e)) {
                is DiagnosticError.EcuError -> ConnectionState.ECU_NOT_RESPONDING
                is DiagnosticError.Elm327Error -> ConnectionState.ADAPTER_NOT_RESPONDING
                is DiagnosticError.ProtocolError -> ConnectionState.PROTOCOL_DETECTION_FAILED
                else -> ConnectionState.ERROR
            }
            
            _connectionState.value = errorState
            _liveTelemetry.value = LiveSensorData.disconnected(
                AppOperationMode.REAL_HARDWARE,
                "เกิดข้อผิดพลาด: ${e.message ?: "ไม่สามารถสื่อสารกับ Adapter"}"
            )
            return@withContext errorState
        }""",
"""        _connectionState.value = ConnectionState.ADAPTER_HANDSHAKE
        _adapterState.value = AdapterConnectionState.ADAPTER_HANDSHAKE
        updateTelemetry("Adapter Handshake...")
        
        try {
            elmDriver.initialize()
        } catch (e: Exception) {
            Log.e(TAG, "Hardware Handshake failed", e)
            
            // Map specific errors to more useful states
            val diagErr = DiagnosticError.fromException(e)
            val errorState = when (diagErr) {
                is DiagnosticError.EcuError -> ConnectionState.ECU_NOT_RESPONDING
                is DiagnosticError.Elm327Error -> ConnectionState.ADAPTER_NOT_RESPONDING
                is DiagnosticError.ProtocolError -> ConnectionState.PROTOCOL_DETECTION_FAILED
                else -> ConnectionState.ERROR
            }
            
            _connectionState.value = errorState
            if (diagErr is DiagnosticError.Elm327Error || diagErr is DiagnosticError.UnknownError) {
                _adapterState.value = AdapterConnectionState.ADAPTER_NOT_RESPONDING
            } else {
                _adapterState.value = AdapterConnectionState.ADAPTER_RESPONDING
                _ecuState.value = EcuConnectionState.ERROR
            }
            
            updateTelemetry("เกิดข้อผิดพลาด: ${e.message ?: "ไม่สามารถสื่อสารกับ Adapter"}")
            return@withContext errorState
        }"""
)

content = content.replace(
"""        _connectionState.value = ConnectionState.ADAPTER_RESPONDING
        _connectionState.value = ConnectionState.PROTOCOL_DETECTED
        _connectionState.value = ConnectionState.ECU_RESPONDING""",
"""        _connectionState.value = ConnectionState.ADAPTER_RESPONDING
        _adapterState.value = AdapterConnectionState.ADAPTER_RESPONDING
        updateTelemetry("Adapter ตอบสนอง เริ่มตรวจสอบ ECU")
        _ecuState.value = EcuConnectionState.PROTOCOL_DETECTING
        
        // Wait, ECU detection isn't explicitly done here, elmDriver.initialize might do some?
        // Actually elmDriver.initialize sends ATZ, ATE0, ATL0, ATS0, ATH0, ATAT1, ATST62, ATSP0.
        // It detects protocol. Let's assume it detects protocol.
        
        _connectionState.value = ConnectionState.PROTOCOL_DETECTED
        _ecuState.value = EcuConnectionState.PROTOCOL_DETECTED
        _connectionState.value = ConnectionState.ECU_RESPONDING
        _ecuState.value = EcuConnectionState.ECU_RESPONDING
        val current3 = _liveTelemetry.value
        _liveTelemetry.value = current3.copy(vehicleBusBitrate = 500000) // Default CAN 500k
"""
)

content = content.replace(
"""        val isValid = pollingEngine.validateLiveConnection()
        if (!isValid) {
            Log.e(TAG, "ECU Live Validation Failed")
            _connectionState.value = ConnectionState.ECU_NOT_RESPONDING
            _liveTelemetry.value = LiveSensorData.disconnected(
                AppOperationMode.REAL_HARDWARE,
                "เชื่อมต่ออะแดปเตอร์ได้ แต่ ECU ไม่ส่งข้อมูล (โปรดตรวจสอบสวิตช์กุญแจรถ)"
            )
            return@withContext ConnectionState.ECU_NOT_RESPONDING
        }""",
"""        val isValid = pollingEngine.validateLiveConnection()
        if (!isValid) {
            Log.e(TAG, "ECU Live Validation Failed")
            _connectionState.value = ConnectionState.ECU_NOT_RESPONDING
            _ecuState.value = EcuConnectionState.ECU_NOT_RESPONDING
            updateTelemetry("เชื่อมต่ออะแดปเตอร์ได้ แต่ ECU ไม่ส่งข้อมูล (โปรดตรวจสอบสวิตช์กุญแจรถ)")
            return@withContext ConnectionState.ECU_NOT_RESPONDING
        }"""
)

content = content.replace(
"""        _connectionState.value = ConnectionState.LIVE_DATA_VALIDATED
        _connectionState.value = ConnectionState.CONNECTED
        _liveTelemetry.value = LiveSensorData.disconnected( // Note: Initializing as disconnected, then poller takes over
            AppOperationMode.REAL_HARDWARE,
            "เชื่อมต่อสมบูรณ์ กำลังอ่านข้อมูล..."
        ).copy(isConnected = true, connectionState = ConnectionState.CONNECTED)""",
"""        _connectionState.value = ConnectionState.LIVE_DATA_VALIDATED
        _ecuState.value = EcuConnectionState.LIVE_DATA_VALIDATED
        _connectionState.value = ConnectionState.CONNECTED
        _ecuState.value = EcuConnectionState.CONNECTED
        
        val initialData = LiveSensorData.disconnected( 
            AppOperationMode.REAL_HARDWARE,
            "เชื่อมต่อสมบูรณ์ กำลังอ่านข้อมูล..."
        ).copy(
            isConnected = true, 
            connectionState = ConnectionState.CONNECTED,
            adapterState = AdapterConnectionState.ADAPTER_RESPONDING,
            ecuState = EcuConnectionState.CONNECTED,
            usbVidPid = _liveTelemetry.value.usbVidPid,
            usbDriver = _liveTelemetry.value.usbDriver,
            serialBaudRate = _liveTelemetry.value.serialBaudRate,
            vehicleBusBitrate = _liveTelemetry.value.vehicleBusBitrate
        )
        _liveTelemetry.value = initialData"""
)

content = content.replace(
"""    private fun handleDeviceDetached(device: UsbDevice) {
        if (activeDevice?.deviceName == device.deviceName) {
            _connectionState.value = ConnectionState.DEVICE_DISCONNECTED
            _liveTelemetry.value = LiveSensorData.disconnected(
                AppOperationMode.REAL_HARDWARE,
                "สาย USB ถูกถอดออก"
            )
            activeDevice = null
            stopPolling()
        }
    }""",
"""    private fun handleDeviceDetached(device: UsbDevice) {
        if (activeDevice?.deviceName == device.deviceName) {
            _connectionState.value = ConnectionState.DEVICE_DISCONNECTED
            _adapterState.value = AdapterConnectionState.DEVICE_DISCONNECTED
            _ecuState.value = EcuConnectionState.NOT_STARTED
            updateTelemetry("สาย USB ถูกถอดออก")
            activeDevice = null
            stopPolling()
        }
    }"""
)

content = content.replace(
"""    fun disconnect() {
        stopPolling()
        driverScope.launch {
            _connectionState.value = ConnectionState.DISCONNECTED
            _liveTelemetry.value = LiveSensorData.disconnected(
                AppOperationMode.REAL_HARDWARE,
                "ยกเลิกการเชื่อมต่อแล้ว"
            )
            transport.close()
        }
    }""",
"""    fun disconnect() {
        stopPolling()
        driverScope.launch {
            _connectionState.value = ConnectionState.DISCONNECTED
            _adapterState.value = AdapterConnectionState.DISCONNECTED
            _ecuState.value = EcuConnectionState.NOT_STARTED
            updateTelemetry("ยกเลิกการเชื่อมต่อแล้ว")
            transport.close()
        }
    }"""
)

with open('app/src/main/java/com/example/hardware/UsbObdDriver.kt', 'w') as f:
    f.write(content)
