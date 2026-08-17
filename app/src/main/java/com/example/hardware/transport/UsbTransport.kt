package com.example.hardware.transport

import android.content.Context
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import com.example.model.DiagnosticError
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

data class SerialConfig(
    val baudRate: Int = 38400,
    val dataBits: Int = 8,
    val stopBits: Int = UsbSerialPort.STOPBITS_1,
    val parity: Int = UsbSerialPort.PARITY_NONE,
    val dtrRts: Boolean = true
)

interface UsbTransport {
    suspend fun open(device: UsbDevice, config: SerialConfig = SerialConfig()): Result<Unit>
    suspend fun close()
    suspend fun write(data: ByteArray, timeoutMs: Int = 1000): Result<Int>
    suspend fun read(buffer: ByteArray, timeoutMs: Int = 1000): Result<Int>
    fun isOpen(): Boolean
    fun configure(config: SerialConfig): Result<Unit>
    fun getDiagnostics(): String
}

class AndroidUsbTransport(private val context: Context) : UsbTransport {

    private var usbSerialPort: UsbSerialPort? = null
    private var activeDevice: UsbDevice? = null
    private var activeConfig: SerialConfig = SerialConfig()
    private var bytesTransmitted: Long = 0
    private var bytesReceived: Long = 0

    override suspend fun open(device: UsbDevice, config: SerialConfig): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val usbManager = context.getSystemService(Context.USB_SERVICE) as? UsbManager
                ?: return@withContext Result.failure(
                    DiagnosticError.UsbError("ERR_USB_NO_SERVICE", "ไม่พบบริการ USB Manager บนอุปกรณ์", "USB service not available")
                )

            val prober = UsbSerialProber.getDefaultProber()
            val driver = prober.probeDevice(device)
                ?: return@withContext Result.failure(
                    DiagnosticError.UsbError(
                        "ERR_USB_DRIVER_NOT_FOUND",
                        "ไม่พบไดรเวอร์ Serial สำหรับอุปกรณ์ USB: ${device.deviceName} (VID: ${device.vendorId}, PID: ${device.productId})",
                        "No compatible serial driver found for USB device"
                    )
                )

            if (driver.ports.isEmpty()) {
                return@withContext Result.failure(
                    DiagnosticError.UsbError("ERR_USB_NO_PORTS", "อุปกรณ์ USB ไม่มี Serial Port ใช้งานได้", "No serial ports on driver")
                )
            }

            val connection = usbManager.openDevice(device)
                ?: return@withContext Result.failure(
                    DiagnosticError.UsbError("ERR_USB_OPEN_FAILED", "ไม่สามารถเปิดการเชื่อมต่อ USB ได้ (อาจติด permission หรือถูกใช้งานอยู่)", "Failed to open USB device connection")
                )

            val port = driver.ports[0]
            port.open(connection)
            port.setParameters(config.baudRate, config.dataBits, config.stopBits, config.parity)
            if (config.dtrRts) {
                try {
                    port.dtr = true
                    port.rts = true
                } catch (_: Exception) {}
            }

            usbSerialPort = port
            activeDevice = device
            activeConfig = config
            Result.success(Unit)
        } catch (e: Exception) {
            close()
            Result.failure(
                DiagnosticError.SerialError(
                    "ERR_SERIAL_OPEN_EXCEPTION",
                    "เกิดข้อผิดพลาดในการเปิด Serial Port: ${e.localizedMessage}",
                    "Exception during serial port opening",
                    e
                )
            )
        }
    }

    override suspend fun close(): Unit = withContext(Dispatchers.IO) {
        try {
            usbSerialPort?.close()
        } catch (_: Exception) {
        } finally {
            usbSerialPort = null
            activeDevice = null
        }
    }

    override suspend fun write(data: ByteArray, timeoutMs: Int): Result<Int> = withContext(Dispatchers.IO) {
        val port = usbSerialPort ?: return@withContext Result.failure(
            DiagnosticError.UsbError("ERR_PORT_NOT_OPEN", "พอร์ต USB ยังไม่เปิดใช้งาน", "USB port is not open")
        )
        try {
            port.write(data, timeoutMs)
            bytesTransmitted += data.size
            Result.success(data.size)
        } catch (e: Exception) {
            Result.failure(
                DiagnosticError.SerialError(
                    "ERR_SERIAL_WRITE",
                    "ไม่สามารถส่งข้อมูลไปยัง USB Adapter ได้: ${e.localizedMessage}",
                    "Failed to write to serial port",
                    e
                )
            )
        }
    }

    override suspend fun read(buffer: ByteArray, timeoutMs: Int): Result<Int> = withContext(Dispatchers.IO) {
        val port = usbSerialPort ?: return@withContext Result.failure(
            DiagnosticError.UsbError("ERR_PORT_NOT_OPEN", "พอร์ต USB ยังไม่เปิดใช้งาน", "USB port is not open")
        )
        try {
            val bytesRead = port.read(buffer, timeoutMs)
            bytesReceived += bytesRead
            Result.success(bytesRead)
        } catch (e: Exception) {
            Result.failure(
                DiagnosticError.SerialError(
                    "ERR_SERIAL_READ",
                    "ไม่สามารถอ่านข้อมูลจาก USB Adapter ได้: ${e.localizedMessage}",
                    "Failed to read from serial port",
                    e
                )
            )
        }
    }

    override fun isOpen(): Boolean {
        return usbSerialPort?.isOpen == true
    }

    override fun configure(config: SerialConfig): Result<Unit> {
        val port = usbSerialPort ?: return Result.failure(
            DiagnosticError.UsbError("ERR_PORT_NOT_OPEN", "พอร์ต USB ยังไม่เปิดใช้งาน", "USB port is not open")
        )
        return try {
            port.setParameters(config.baudRate, config.dataBits, config.stopBits, config.parity)
            activeConfig = config
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(
                DiagnosticError.SerialError(
                    "ERR_SERIAL_RECONFIG",
                    "ไม่สามารถเปลี่ยนค่า Serial Parameter ได้: ${e.localizedMessage}",
                    "Failed to reconfigure serial parameters",
                    e
                )
            )
        }
    }

    override fun getDiagnostics(): String {
        return buildString {
            appendLine("USB Transport Diagnostics:")
            appendLine("  Is Open: ${isOpen()}")
            appendLine("  Device: ${activeDevice?.deviceName ?: "None"} (VID: ${activeDevice?.vendorId}, PID: ${activeDevice?.productId})")
            appendLine("  Baud Rate: ${activeConfig.baudRate}")
            appendLine("  Data Bits: ${activeConfig.dataBits}, Stop Bits: ${activeConfig.stopBits}, Parity: ${activeConfig.parity}")
            appendLine("  TX Bytes: $bytesTransmitted, RX Bytes: $bytesReceived")
        }
    }
}
