package com.example.service

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import com.example.hardware.usb.UsbPermissionManager

class UsbCommunicationService : Service() {

    companion object {
        private const val TAG = "UsbCommService"
    }

    private val binder = LocalBinder()
    lateinit var permissionManager: UsbPermissionManager
        private set

    inner class LocalBinder : Binder() {
        fun getService(): UsbCommunicationService = this@UsbCommunicationService
    }

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "UsbCommunicationService created")
        permissionManager = UsbPermissionManager(this)
        permissionManager.startListening()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "UsbCommunicationService destroyed")
        permissionManager.stopListening()
    }

    override fun onBind(intent: Intent?): IBinder = binder
}

