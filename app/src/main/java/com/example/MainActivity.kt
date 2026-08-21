package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.example.ui.components.ExitConfirmationDialog
import com.example.ui.ThaiObdApp
import com.example.ui.theme.ThaiCarOBDTheme
import com.example.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ThaiCarOBDTheme {
                var showSplash by remember { mutableStateOf(true) }
                var showExitDialog by remember { mutableStateOf(false) }

                if (showSplash) {
                    com.example.ui.screens.SplashScreen(
                        logoResId = android.R.drawable.ic_menu_compass,
                        onSplashFinished = { showSplash = false }
                    )
                } else {
                    BackHandler {
                        showExitDialog = true
                    }
                    
                    ThaiObdApp(viewModel = viewModel)

                    if (showExitDialog) {
                        ExitConfirmationDialog(
                            onConfirm = { finish() },
                            onDismiss = { showExitDialog = false }
                        )
                    }
                }
            }
        }
        
        // Handle USB attach intent
        if (android.hardware.usb.UsbManager.ACTION_USB_DEVICE_ATTACHED == intent.action) {
            viewModel.connectUsbHardware()
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        if (android.hardware.usb.UsbManager.ACTION_USB_DEVICE_ATTACHED == intent.action) {
            viewModel.connectUsbHardware()
        }
    }
}
