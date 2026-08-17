package com.example.hardware.usb

import android.util.Log
import com.example.model.ConnectionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class UsbConnectionWatchdog(
    private val idleTimeoutMs: Long = 500L
) {
    companion object {
        private const val TAG = "UsbConnectionWatchdog"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var watchdogJob: Job? = null

    private var lastActivityTimestamp = System.currentTimeMillis()

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.CONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    fun startMonitoring() {
        stopMonitoring()
        lastActivityTimestamp = System.currentTimeMillis()
        Log.i(TAG, "Starting USB Connection Watchdog (idle timeout: ${idleTimeoutMs}ms)...")

        watchdogJob = scope.launch {
            while (isActive) {
                val currentElapsed = System.currentTimeMillis() - lastActivityTimestamp
                if (currentElapsed > idleTimeoutMs) {
                    if (_connectionState.value != ConnectionState.DISCONNECTED_STALE) {
                        Log.w(TAG, "Serial buffer idle for ${currentElapsed}ms (> ${idleTimeoutMs}ms). Triggering DISCONNECTED_STALE.")
                        _connectionState.value = ConnectionState.DISCONNECTED_STALE
                    }
                }
                delay(100L)
            }
        }
    }

    fun recordActivity() {
        lastActivityTimestamp = System.currentTimeMillis()
        if (_connectionState.value == ConnectionState.DISCONNECTED_STALE) {
            Log.i(TAG, "USB serial buffer activity resumed. Restoring CONNECTED state.")
            _connectionState.value = ConnectionState.CONNECTED
        }
    }

    fun stopMonitoring() {
        watchdogJob?.cancel()
        watchdogJob = null
        Log.i(TAG, "Stopped USB Connection Watchdog.")
    }
}
