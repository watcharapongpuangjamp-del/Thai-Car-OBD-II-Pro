with open('app/src/main/java/com/example/hardware/UsbObdDriver.kt', 'r') as f:
    content = f.read()

old_code = """    private fun startPollingStream() {
        pollingEngine.startPolling()
        telemetryCollectionJob?.cancel()
        telemetryCollectionJob = driverScope.launch {
            pollingEngine.telemetryFlow.collect { telemetry ->
                _liveTelemetry.value = telemetry
            }
        }
    }"""

new_code = """    private fun startPollingStream() {
        pollingEngine.startPolling()
        telemetryCollectionJob?.cancel()
        telemetryCollectionJob = driverScope.launch {
            pollingEngine.telemetryFlow.collect { telemetry ->
                val current = _liveTelemetry.value
                _liveTelemetry.value = telemetry.copy(
                    adapterState = current.adapterState,
                    ecuState = current.ecuState,
                    usbVidPid = current.usbVidPid,
                    usbDriver = current.usbDriver,
                    serialBaudRate = current.serialBaudRate,
                    vehicleBusBitrate = current.vehicleBusBitrate
                )
            }
        }
    }"""

content = content.replace(old_code, new_code)
with open('app/src/main/java/com/example/hardware/UsbObdDriver.kt', 'w') as f:
    f.write(content)
