package com.example.hardware.usb

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.util.Log
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class UsbDeviceEvent {
    data class Attached(val device: UsbDevice) : UsbDeviceEvent()
    data class Detached(val device: UsbDevice) : UsbDeviceEvent()
    data class PermissionResult(val device: UsbDevice, val isGranted: Boolean) : UsbDeviceEvent()
}

class UsbPermissionManager(private val context: Context) {

    companion object {
        const val ACTION_USB_PERMISSION = "com.example.USB_PERMISSION"
        private const val TAG = "UsbPermissionManager"
    }

    private val usbManager = context.getSystemService(Context.USB_SERVICE) as? UsbManager

    private val _deviceEvents = MutableSharedFlow<UsbDeviceEvent>(extraBufferCapacity = 16)
    val deviceEvents: SharedFlow<UsbDeviceEvent> = _deviceEvents.asSharedFlow()

    private val _attachedDevice = MutableStateFlow<UsbDevice?>(null)
    val attachedDevice: StateFlow<UsbDevice?> = _attachedDevice.asStateFlow()

    private var isReceiverRegistered = false

    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(receiverContext: Context?, intent: Intent?) {
            val action = intent?.action ?: return
            val device: UsbDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
            }

            when (action) {
                ACTION_USB_PERMISSION -> {
                    val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                    device?.let {
                        Log.i(TAG, "USB Permission result for ${it.deviceName}: granted=$granted")
                        _deviceEvents.tryEmit(UsbDeviceEvent.PermissionResult(it, granted))
                    }
                }
                UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                    device?.let {
                        Log.i(TAG, "USB Device Attached: ${it.deviceName} (VID: ${it.vendorId}, PID: ${it.productId})")
                        _attachedDevice.value = it
                        _deviceEvents.tryEmit(UsbDeviceEvent.Attached(it))
                    }
                }
                UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                    device?.let {
                        Log.i(TAG, "USB Device Detached: ${it.deviceName}")
                        if (_attachedDevice.value?.deviceId == it.deviceId) {
                            _attachedDevice.value = null
                        }
                        _deviceEvents.tryEmit(UsbDeviceEvent.Detached(it))
                    }
                }
            }
        }
    }

    fun startListening() {
        if (!isReceiverRegistered) {
            val filter = IntentFilter().apply {
                addAction(ACTION_USB_PERMISSION)
                addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
                addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(usbReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                context.registerReceiver(usbReceiver, filter)
            }
            isReceiverRegistered = true
            scanForAttachedDevices()
        }
    }

    fun stopListening() {
        if (isReceiverRegistered) {
            try {
                context.unregisterReceiver(usbReceiver)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to unregister USB receiver: ${e.message}")
            }
            isReceiverRegistered = false
        }
    }

    fun scanForAttachedDevices(): UsbDevice? {
        val manager = usbManager ?: return null
        val device = manager.deviceList.values.firstOrNull()
        _attachedDevice.value = device
        return device
    }

    fun hasPermission(device: UsbDevice): Boolean {
        return usbManager?.hasPermission(device) == true
    }

    fun requestPermission(device: UsbDevice) {
        val manager = usbManager ?: return
        val flags = PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        val permissionIntent = PendingIntent.getBroadcast(
            context,
            0,
            Intent(ACTION_USB_PERMISSION).setPackage(context.packageName),
            flags
        )
        manager.requestPermission(device, permissionIntent)
    }
}
