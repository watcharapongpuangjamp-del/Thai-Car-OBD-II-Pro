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

    private val _liveTelemetry = MutableStateFlow(
        LiveSensorData.disconnected(AppOperationMode.REAL_HARDWARE, "รอเชื่อมต่ออุปกรณ์ USB OTG")
    )
    val liveTelemetry: Flow<LiveSensorData> = _liveTelemetry

    private var activeDevice: UsbDevice? = null

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
                                _liveTelemetry.value = LiveSensorData.disconnected(
                                    AppOperationMode.REAL_HARDWARE,
                                    "ตรวจพบอุปกรณ์ USB: ${event.device.deviceName}"
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
                                _liveTelemetry.value = LiveSensorData.disconnected(
                                    AppOperationMode.REAL_HARDWARE,
                                    "ได้รับสิทธิ์เข้าถึงอุปกรณ์ USB แล้ว"
                                )
                                driverScope.launch {
                                    proceedWithUsbOpen(event.device)
                                }
                            } else {
                                _connectionState.value = ConnectionState.PERMISSION_DENIED
                                _liveTelemetry.value = LiveSensorData.disconnected(
                                    AppOperationMode.REAL_HARDWARE,
                                    "ผู้ใช้ปฏิเสธการให้สิทธิ์เข้าถึงอุปกรณ์ USB"
                                )
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
            _liveTelemetry.value = LiveSensorData.disconnected(
                AppOperationMode.REAL_HARDWARE,
                "ไม่พบสาย USB OTG หรือ OBD-II Adapter เชื่อมต่ออยู่"
            )
            return@withContext ConnectionState.DISCONNECTED
        }

        activeDevice = device
        _connectionState.value = ConnectionState.DEVICE_DETECTED
        _liveTelemetry.value = LiveSensorData.disconnected(
            AppOperationMode.REAL_HARDWARE,
            "ตรวจพบอุปกรณ์: ${device.deviceName} (VID: ${device.vendorId}, PID: ${device.productId})"
        )

        if (!permissionManager.hasPermission(device)) {
            Log.i(TAG, "Permission required for device ${device.deviceName}. Requesting from user...")
            _connectionState.value = ConnectionState.PERMISSION_REQUIRED
            _liveTelemetry.value = LiveSensorData.disconnected(
                AppOperationMode.REAL_HARDWARE,
                "กรุณากดยินยอมสิทธิ์เข้าถึง USB บนหน้าจออุปกรณ์"
            )
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
            _liveTelemetry.value = LiveSensorData.disconnected(
                AppOperationMode.REAL_HARDWARE,
                "เปิดพอร์ต USB ล้มเหลว: ${exception?.localizedMessage ?: "Unknown"}"
            )
            return@withContext ConnectionState.USB_OPEN_FAILED
        }

        _connectionState.value = ConnectionState.USB_OPEN
        _liveTelemetry.value = LiveSensorData.disconnected(
            AppOperationMode.REAL_HARDWARE,
            "เปิดพอร์ต USB สำเร็จ กำลังตรวจสอบการสื่อสาร Serial"
        )

        // 2. SERIAL_READY
        if (!transport.isOpen()) {
            _connectionState.value = ConnectionState.SERIAL_ERROR
            _liveTelemetry.value = LiveSensorData.disconnected(
                AppOperationMode.REAL_HARDWARE,
                "พอร์ต Serial ไม่พร้อมใช้งาน"
            )
            return@withContext ConnectionState.SERIAL_ERROR
        }

        _connectionState.value = ConnectionState.SERIAL_READY
        _liveTelemetry.value = LiveSensorData.disconnected(
            AppOperationMode.REAL_HARDWARE,
            "Serial Communication พร้อมใช้งาน (38400 baud, 8N1)"
        )

        // 3. ADAPTER_HANDSHAKE
        _connectionState.value = ConnectionState.ADAPTER_HANDSHAKE
        _liveTelemetry.value = LiveSensorData.disconnected(
            AppOperationMode.REAL_HARDWARE,
            "กำลังส่งคำสั่ง Handshake (ATZ, ATE0, ATL0, ATSP0)..."
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
            _liveTelemetry.value = LiveSensorData.disconnected(
                AppOperationMode.REAL_HARDWARE,
                error?.localizedMessage ?: "การเชื่อมต่อกับ ELM327/ECU ล้มเหลว"
            )
            return@withContext nextState
        }

        val protocol = protocolResult.getOrDefault(ObdProtocol.UNKNOWN)
        _connectionState.value = ConnectionState.ADAPTER_RESPONDING
        _connectionState.value = ConnectionState.PROTOCOL_DETECTED
        _connectionState.value = ConnectionState.ECU_RESPONDING
        _connectionState.value = ConnectionState.LIVE_DATA_VALIDATED
        _connectionState.value = ConnectionState.CONNECTED

        // Start Real Telemetry Polling Loop
        startPollingStream()

        return@withContext ConnectionState.CONNECTED
    }

    private fun startPollingStream() {
        pollingEngine.startPolling()
        telemetryCollectionJob?.cancel()
        telemetryCollectionJob = driverScope.launch {
            pollingEngine.telemetryFlow.collect { telemetry ->
                _liveTelemetry.value = telemetry
            }
        }
    }

    private fun handleDeviceDetached(device: UsbDevice) {
        driverScope.launch {
            pollingEngine.stopPolling()
            telemetryCollectionJob?.cancel()
            transport.close()
            activeDevice = null
            _connectionState.value = ConnectionState.DEVICE_DISCONNECTED
            _liveTelemetry.value = LiveSensorData.disconnected(
                AppOperationMode.REAL_HARDWARE,
                "อุปกรณ์ USB ถูกถอดออก (Device Detached)"
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
            _liveTelemetry.value = LiveSensorData.disconnected(
                AppOperationMode.REAL_HARDWARE,
                "ตัดการเชื่อมต่ออุปกรณ์ USB เรียบร้อย"
            )
        }
    }

    suspend fun scanRealHardwareDtcs(): DtcScanResult = withContext(Dispatchers.IO) {
        if (_connectionState.value != ConnectionState.CONNECTED) {
            return@withContext DtcScanResult(emptyList(), DtcScanStatus.FAILED("Not connected"))
        }
        // Temporarily pause polling during DTC bus sweep
        pollingEngine.stopPolling()
        val dtcResult = dtcScanner.performCompleteDtcScan()
        // Resume polling
        startPollingStream()
        return@withContext dtcResult
    }

    suspend fun clearRealHardwareDtcs(): Result<Boolean> = withContext(Dispatchers.IO) {
        if (_connectionState.value != ConnectionState.CONNECTED) {
            return@withContext Result.failure(IllegalStateException("Not connected to ECU"))
        }
        pollingEngine.stopPolling()
        val result = dtcScanner.clearDtcCodes()
        startPollingStream()
        return@withContext result
    }

    fun release() {
        permissionManager.stopListening()
        eventListenerJob?.cancel()
        pollingEngine.stopPolling()
        telemetryCollectionJob?.cancel()
        disconnect()
    }
}
