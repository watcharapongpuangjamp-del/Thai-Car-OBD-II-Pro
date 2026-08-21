package com.example.hardware.obd

import android.util.Log

object ObdDiagnosticLogger {
    private val TAG = "ObdDiagnosticLogger"
    private val logs = mutableListOf<String>()

    fun log(command: String, response: String) {
        val entry = "TX: $command | RX: $response"
        logs.add(entry)
        Log.d(TAG, entry)
    }

    fun getLogs(): List<String> = logs.toList()
    
    fun clear() {
        logs.clear()
    }
}
