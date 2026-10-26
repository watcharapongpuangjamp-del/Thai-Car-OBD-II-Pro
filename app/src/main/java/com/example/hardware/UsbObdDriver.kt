package com.example.hardware

import android.content.Context
import android.hardware.usb.UsbDevice
import android.util.Log
import com.example.hardware.obd.Elm327Driver
import com.example.hardware.obd.ObdProtocol
import com.example.hardware.obd.ObdProtocolEngine
import com.example.hardware.obd.RealDtcScanner
import com.example.hardware.obd.TelemetryPollingEngine
import com.example.hardware.transport.AndroidUsbTransport
import com.example.hardware.transport.SerialConfig
import com.example.hardware.transport.UsbTransport
import com.example.hardware.usb.UsbDeviceEvent
import com.example.hardware.usb.UsbPermissionManager
import com.example.model.AppOperationMode
import com.example.model.ConnectionState
import com.example.model.AdapterConnectionState
import com.example.model.EcuConnectionState
import com.example.model.DiagnosticError
import com.example.model.DtcCode
import com.example.model.DtcScanResult
import com.example.model.DtcScanStatus
import com.example.model.LiveSensorData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class UsbObdDriver(
    private val context: Context,
    private val transport: UsbTransport = AndroidUsbTransport(context),
    val permissionManager: UsbPermissionManager = UsbPermissionManager(context)
) {

    companion object {
        private const val TAG = "UsbObdDriver"
    }

    private val driverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var eventListenerJob: Job? = null
    private var telemetryCollectionJob: Job? = null

    val elmDriver = Elm327Driver(transport)
    val protocolEngine = ObdProtocolEngine(elmDriver)
    val pollingEngine = TelemetryPollingEngine(protocolEngine, elmDriver)
    val dtcScanner = RealDtcScanner(elmDriver)

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState = _connectionState.asStateFlow()
    
    private val _adapterState = MutableStateFlow(AdapterConnectionState.DISCONNECTED)
    val adapterState = _adapterState.asStateFlow()
    
    private val _ecuState = MutableStateFlow(EcuConnectionState.NOT_STARTED)
    val ecuState = _ecuState.asStateFlow()

    private val _liveTelemetry = MutableStateFlow(
        LiveSensorData.disconnected(AppOperationMode.REAL_HARDWARE, "รอเชื่อมต่ออุปกรณ์ USB OTG")
    )
    val liveTelemetry: Flow<LiveSensorData> = _liveTelemetry

    private var activeDevice: UsbDevice? = null


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

    init {
        startLifecycleMonitoring()
    }

    fun startLifecycleMonitoring() {
        permissionManager.startListening()
        if (eventListenerJob == null || eventListenerJob?.isActive == false) {
            eventListenerJob = driverScope.launch {
                permissionManager.deviceEvents.collect { event ->
                    when (event) {
                        is UsbDeviceEvent.Attached -> {
                            Log.i(TAG, "Hardware Event: Device Attached -> ${event.device.deviceName}")
                            if (_connectionState.value == ConnectionState.DISCONNECTED || _connectionState.value.isError) {
                                activeDevice = event.device
                                _connectionState.value = ConnectionState.DEVICE_DETECTED
                                _adapterState.value = AdapterConnectionState.DEVICE_DETECTED
                                updateTelemetry("ตรวจพบอุปกรณ์ USB: ${event.device.deviceName}")
                                val current = _liveTelemetry.value
                                _liveTelemetry.value = current.copy(
                                    usbVidPid = "${event.device.vendorId}:${event.device.productId}"
                                )
                            }
                        }
                        is UsbDeviceEvent.Detached -> {
                            Log.i(TAG, "Hardware Event: Device Detached -> ${event.device.deviceName}")
                            handleDeviceDetached(event.device)
                        }
                        is UsbDeviceEvent.PermissionResult -> {
                            Log.i(TAG, "Hardware Event: Permission Result for ${event.device.deviceName} = ${event.isGranted}")
                            if (event.isGranted) {
                                _connectionState.value = ConnectionState.PERMISSION_GRANTED
                                _adapterState.value = AdapterConnectionState.PERMISSION_GRANTED
                                updateTelemetry("ได้รับสิทธิ์เข้าถึงอุปกรณ์ USB แล้ว")
                                driverScope.launch {
                                    proceedWithUsbOpen(event.device)
                                }
                            } else {
                                _connectionState.value = ConnectionState.PERMISSION_DENIED
                                _adapterState.value = AdapterConnectionState.PERMISSION_DENIED
                                updateTelemetry("ผู้ใช้ปฏิเสธการให้สิทธิ์เข้าถึงอุปกรณ์ USB")
                            }
                        }
                    }
                }
            }
        }
    }

    suspend fun checkAndConnectUsbDevice(): ConnectionState = withContext(Dispatchers.IO) {
        val device = permissionManager.scanForAttachedDevices()
        if (device == null) {
            _connectionState.value = ConnectionState.DISCONNECTED
            _adapterState.value = AdapterConnectionState.DISCONNECTED
            _ecuState.value = EcuConnectionState.NOT_STARTED
            _liveTelemetry.value = LiveSensorData.disconnected(
                AppOperationMode.REAL_HARDWARE,
                "ไม่พบสาย USB OTG หรือ OBD-II Adapter เชื่อมต่ออยู่"
            )
            return@withContext ConnectionState.DISCONNECTED
        }

        activeDevice = device
        _connectionState.value = ConnectionState.DEVICE_DETECTED
        _adapterState.value = AdapterConnectionState.DEVICE_DETECTED
        updateTelemetry("ตรวจพบอุปกรณ์: ${device.deviceName} (VID: ${device.vendorId}, PID: ${device.productId})")
        val current = _liveTelemetry.value
        _liveTelemetry.value = current.copy(
            usbVidPid = "${device.vendorId}:${device.productId}"
        )

        if (!permissionManager.hasPermission(device)) {
            Log.i(TAG, "Permission required for device ${device.deviceName}. Requesting from user...")
            _connectionState.value = ConnectionState.PERMISSION_REQUIRED
            _adapterState.value = AdapterConnectionState.PERMISSION_REQUIRED
            updateTelemetry("กรุณากดยินยอมสิทธิ์เข้าถึง USB บนหน้าจออุปกรณ์")
            permissionManager.requestPermission(device)
            return@withContext ConnectionState.PERMISSION_REQUIRED
        }

        _connectionState.value = ConnectionState.PERMISSION_GRANTED
        return@withContext proceedWithUsbOpen(device)
    }

    private suspend fun proceedWithUsbOpen(device: UsbDevice): ConnectionState = withContext(Dispatchers.IO) {
        // 1. USB_OPEN
        val openResult = transport.open(device, SerialConfig(baudRate = 38400))
        if (openResult.isFailure) {
            val exception = openResult.exceptionOrNull()
            Log.e(TAG, "Failed to open USB transport", exception)
            _connectionState.value = ConnectionState.USB_OPEN_FAILED
            _adapterState.value = AdapterConnectionState.USB_OPEN_FAILED
            _ecuState.value = EcuConnectionState.NOT_STARTED
            _liveTelemetry.value = LiveSensorData.disconnected(
                AppOperationMode.REAL_HARDWARE,
                "เปิดพอร์ต USB ล้มเหลว: ${exception?.localizedMessage ?: "Unknown"}"
            ).copy(
                connectionState = ConnectionState.USB_OPEN_FAILED,
                adapterState = _adapterState.value,
                ecuState = _ecuState.value,
                usbVidPid = "${device.vendorId}:${device.productId}"
            )
            return@withContext ConnectionState.USB_OPEN_FAILED
        }

        _connectionState.value = ConnectionState.USB_OPEN
        _adapterState.value = AdapterConnectionState.USB_OPEN
        _ecuState.value = EcuConnectionState.NOT_STARTED
        _liveTelemetry.value = LiveSensorData.disconnected(
            AppOperationMode.REAL_HARDWARE,
            "เปิดพอร์ต USB สำเร็จ กำลังตรวจสอบการสื่อสาร Serial"
        ).copy(
            connectionState = _connectionState.value,
            adapterState = _adapterState.value,
            ecuState = _ecuState.value,
            usbVidPid = "${device.vendorId}:${device.productId}"
        )

        // 2. SERIAL_READY
        if (!transport.isOpen()) {
            _connectionState.value = ConnectionState.SERIAL_ERROR
            _adapterState.value = AdapterConnectionState.SERIAL_ERROR
            _ecuState.value = EcuConnectionState.NOT_STARTED
            _liveTelemetry.value = LiveSensorData.disconnected(
                AppOperationMode.REAL_HARDWARE,
                "พอร์ต Serial ไม่พร้อมใช้งาน"
            ).copy(
                connectionState = _connectionState.value,
                adapterState = _adapterState.value,
                ecuState = _ecuState.value,
                usbVidPid = "${device.vendorId}:${device.productId}"
            )
            return@withContext ConnectionState.SERIAL_ERROR
        }

        _connectionState.value = ConnectionState.SERIAL_READY
        _adapterState.value = AdapterConnectionState.SERIAL_READY
        _liveTelemetry.value = LiveSensorData.disconnected(
            AppOperationMode.REAL_HARDWARE,
            "Serial Communication พร้อมใช้งาน (38400 baud, 8N1)"
        ).copy(
            connectionState = _connectionState.value,
            adapterState = _adapterState.value,
            ecuState = _ecuState.value,
            usbVidPid = "${device.vendorId}:${device.productId}",
            serialBaudRate = 38400
        )

        // 3. ADAPTER_HANDSHAKE
        _connectionState.value = ConnectionState.ADAPTER_HANDSHAKE
        _adapterState.value = AdapterConnectionState.ADAPTER_HANDSHAKE
        _ecuState.value = EcuConnectionState.NOT_STARTED
        _liveTelemetry.value = LiveSensorData.disconnected(
            AppOperationMode.REAL_HARDWARE,
            "กำลังส่งคำสั่ง Handshake และตรวจสอบ OBD Protocol..."
        ).copy(
            connectionState = _connectionState.value,
            adapterState = _adapterState.value,
            ecuState = _ecuState.value,
            usbVidPid = "${device.vendorId}:${device.productId}",
            serialBaudRate = 38400
        )

        val protocolResult = protocolEngine.initializeProtocolEngine()
        if (protocolResult.isFailure) {
            val error = protocolResult.exceptionOrNull()
            Log.e(TAG, "Handshake failed", error)
            val nextState = when (error) {
                is DiagnosticError.EcuError -> ConnectionState.ECU_NOT_RESPONDING
                is DiagnosticError.Elm327Error -> ConnectionState.ADAPTER_NOT_RESPONDING
                is DiagnosticError.ProtocolError -> ConnectionState.PROTOCOL_DETECTION_FAILED
                else -> ConnectionState.ERROR
            }
            _connectionState.value = nextState
            _adapterState.value = when (nextState) {
                ConnectionState.ADAPTER_NOT_RESPONDING -> AdapterConnectionState.ADAPTER_NOT_RESPONDING
                ConnectionState.PROTOCOL_DETECTION_FAILED -> AdapterConnectionState.ADAPTER_RESPONDING
                else -> _adapterState.value
            }
            _ecuState.value = when (nextState) {
                ConnectionState.ECU_NOT_RESPONDING -> EcuConnectionState.ECU_NOT_RESPONDING
                ConnectionState.PROTOCOL_DETECTION_FAILED -> EcuConnectionState.PROTOCOL_DETECTION_FAILED
                else -> EcuConnectionState.NOT_STARTED
            }
            _liveTelemetry.value = LiveSensorData.disconnected(
                AppOperationMode.REAL_HARDWARE,
                error?.localizedMessage ?: "การเชื่อมต่อกับ ELM327/ECU ล้มเหลว"
            ).copy(
                connectionState = nextState,
                adapterState = _adapterState.value,
                ecuState = _ecuState.value,
                usbVidPid = "${device.vendorId}:${device.productId}",
                serialBaudRate = 38400
            )
            return@withContext nextState
        }

        val protocol = protocolResult.getOrDefault(ObdProtocol.UNKNOWN)
        _connectionState.value = ConnectionState.ADAPTER_RESPONDING
        _adapterState.value = AdapterConnectionState.ADAPTER_RESPONDING
        updateTelemetry("Adapter ตอบสนอง เริ่มตรวจสอบ ECU")
        _ecuState.value = EcuConnectionState.PROTOCOL_DETECTED
        _connectionState.value = ConnectionState.PROTOCOL_DETECTED
        updateTelemetry("ตรวจพบ OBD Protocol ${protocol.description}; กำลังตรวจสอบการตอบสนองของ ECU")
        _connectionState.value = ConnectionState.ECU_RESPONDING
        _ecuState.value = EcuConnectionState.ECU_RESPONDING
        updateTelemetry("Adapter ตอบสนอง กำลังตรวจสอบ PID จาก ECU")
        // Do not report a CAN bitrate unless it was actually detected.


        // Explicitly validate reading live PID 0x0C (RPM) before LIVE_DATA_VALIDATED and CONNECTED
        Log.i(TAG, "Validating live data by querying PID 0x0C (RPM)...")
        val pid0cResult = protocolEngine.readPid(0x0C)
        if (pid0cResult.isFailure) {
            val err = pid0cResult.exceptionOrNull()
            Log.e(TAG, "Live data validation failed on PID 0x0C", err)
            _connectionState.value = ConnectionState.ECU_NOT_RESPONDING
            _ecuState.value = EcuConnectionState.ECU_NOT_RESPONDING
            _liveTelemetry.value = LiveSensorData.disconnected(
                AppOperationMode.REAL_HARDWARE,
                "ECU ไม่ส่งข้อมูล PID 0x0C (RPM) สำหรับการตรวจสอบความถูกต้อง"
            ).copy(
                connectionState = _connectionState.value,
                adapterState = _adapterState.value,
                ecuState = _ecuState.value,
                usbVidPid = "${device.vendorId}:${device.productId}",
                serialBaudRate = 38400
            )
            return@withContext ConnectionState.ECU_NOT_RESPONDING
        }

        _connectionState.value = ConnectionState.LIVE_DATA_VALIDATED
        _ecuState.value = EcuConnectionState.LIVE_DATA_VALIDATED
        updateTelemetry("ตรวจสอบ PID 0x0C สำเร็จ กำลังเริ่มอ่านข้อมูลสด")
        _connectionState.value = ConnectionState.CONNECTED
        _ecuState.value = EcuConnectionState.CONNECTED
        updateTelemetry("เชื่อมต่อ ELM327 และ ECU สำเร็จ")

        // Start Real Telemetry Polling Loop
        startPollingStream()

        return@withContext ConnectionState.CONNECTED
    }


    private fun startPollingStream() {
        pollingEngine.startPolling()
        telemetryCollectionJob?.cancel()
        telemetryCollectionJob = driverScope.launch {
            pollingEngine.telemetryFlow.collect { telemetry ->
                val current = _liveTelemetry.value
                if (!telemetry.isConnected) {
                    _connectionState.value = telemetry.connectionState
                    _ecuState.value = when (telemetry.connectionState) {
                        ConnectionState.DISCONNECTED_STALE,
                        ConnectionState.ECU_NOT_RESPONDING -> EcuConnectionState.TIMEOUT
                        else -> EcuConnectionState.ERROR
                    }
                }
                _liveTelemetry.value = telemetry.copy(
                    adapterState = current.adapterState,
                    ecuState = if (telemetry.isConnected) current.ecuState else _ecuState.value,
                    usbVidPid = current.usbVidPid,
                    usbDriver = current.usbDriver,
                    serialBaudRate = current.serialBaudRate,
                    vehicleBusBitrate = current.vehicleBusBitrate
                )
            }
        }
    }

    private fun handleDeviceDetached(device: UsbDevice) {
        driverScope.launch {
            // Ignore detach notifications for a device that is not the active adapter.
            if (activeDevice?.deviceId != device.deviceId) return@launch
            pollingEngine.stopPolling()
            telemetryCollectionJob?.cancel()
            transport.close()
            activeDevice = null
            _connectionState.value = ConnectionState.DEVICE_DISCONNECTED
            _adapterState.value = AdapterConnectionState.DEVICE_DISCONNECTED
            _ecuState.value = EcuConnectionState.NOT_STARTED
            _liveTelemetry.value = LiveSensorData.disconnected(
                AppOperationMode.REAL_HARDWARE,
                "อุปกรณ์ USB ถูกถอดออก (Device Detached)"
            ).copy(
                connectionState = _connectionState.value,
                adapterState = _adapterState.value,
                ecuState = _ecuState.value
            )
        }
    }

    suspend fun sendCommand(command: String, timeoutMs: Int = 1000): String = withContext(Dispatchers.IO) {
        return@withContext elmDriver.sendRawCommand(command, timeoutMs)
    }

    fun disconnect() {
        driverScope.launch {
            pollingEngine.stopPolling()
            telemetryCollectionJob?.cancel()
            transport.close()
            activeDevice = null
            _connectionState.value = ConnectionState.DISCONNECTED
            _adapterState.value = AdapterConnectionState.DISCONNECTED
            _ecuState.value = EcuConnectionState.NOT_STARTED
            _liveTelemetry.value = LiveSensorData.disconnected(
                AppOperationMode.REAL_HARDWARE,
                "ตัดการเชื่อมต่ออุปกรณ์ USB เรียบร้อย"
            ).copy(
                connectionState = _connectionState.value,
                adapterState = _adapterState.value,
                ecuState = _ecuState.value
            )
        }
    }

    suspend fun scanRealHardwareDtcs(): DtcScanResult = withContext(Dispatchers.IO) {
        if (_connectionState.value != ConnectionState.CONNECTED) {
            return@withContext DtcScanResult(emptyList(), DtcScanStatus.FAILED("Not connected"))
        }
        // Temporarily pause polling during DTC bus sweep
        pollingEngine.stopPolling()
        return@withContext try {
            dtcScanner.performCompleteDtcScan()
        } finally {
            // Always resume polling after the DTC sweep, including exceptional exits.
            if (_connectionState.value == ConnectionState.CONNECTED && transport.isOpen()) {
                startPollingStream()
            }
        }
    }

    suspend fun clearRealHardwareDtcs(): Result<Boolean> = withContext(Dispatchers.IO) {
        if (_connectionState.value != ConnectionState.CONNECTED) {
            return@withContext Result.failure(IllegalStateException("Not connected to ECU"))
        }
        pollingEngine.stopPolling()
        return@withContext try {
            dtcScanner.clearDtcCodes()
        } finally {
            // Always resume polling after clear attempts when the ECU session remains active.
            if (_connectionState.value == ConnectionState.CONNECTED && transport.isOpen()) {
                startPollingStream()
            }
        }
    }

    fun release() {
        permissionManager.stopListening()
        eventListenerJob?.cancel()
        pollingEngine.stopPolling()
        telemetryCollectionJob?.cancel()
        disconnect()
    }
}
