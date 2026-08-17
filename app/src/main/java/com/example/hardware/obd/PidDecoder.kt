package com.example.hardware.obd

object PidDecoder {

    /**
     * Engine RPM (PID 0x0C)
     * Formula: ((A * 256) + B) / 4
     * Unit: RPM
     */
    fun decodeRpm(data: ByteArray): Int {
        if (data.size < 2) return 0
        val a = data[0].toInt() and 0xFF
        val b = data[1].toInt() and 0xFF
        return ((a * 256) + b) / 4
    }

    /**
     * Vehicle Speed (PID 0x0D)
     * Formula: A
     * Unit: km/h
     */
    fun decodeSpeed(data: ByteArray): Int {
        if (data.isEmpty()) return 0
        return data[0].toInt() and 0xFF
    }

    /**
     * Engine Coolant Temperature (PID 0x05)
     * Formula: A - 40
     * Unit: °C
     */
    fun decodeCoolantTemp(data: ByteArray): Int {
        if (data.isEmpty()) return 0
        val a = data[0].toInt() and 0xFF
        return a - 40
    }

    /**
     * Throttle Position (PID 0x11)
     * Formula: (A * 100) / 255
     * Unit: %
     */
    fun decodeThrottlePosition(data: ByteArray): Int {
        if (data.isEmpty()) return 0
        val a = data[0].toInt() and 0xFF
        return (a * 100) / 255
    }

    /**
     * Calculated Engine Load (PID 0x04)
     * Formula: (A * 100) / 255
     * Unit: %
     */
    fun decodeEngineLoad(data: ByteArray): Int {
        if (data.isEmpty()) return 0
        val a = data[0].toInt() and 0xFF
        return (a * 100) / 255
    }

    /**
     * Intake Air Temperature (PID 0x0F)
     * Formula: A - 40
     * Unit: °C
     */
    fun decodeIntakeAirTemp(data: ByteArray): Int {
        if (data.isEmpty()) return 0
        val a = data[0].toInt() and 0xFF
        return a - 40
    }

    /**
     * Intake Manifold Absolute Pressure (MAP) (PID 0x0B)
     * Formula: A
     * Unit: kPa
     * Converted to Boost (bar relative to 101.3 kPa atm): (A - 101.3) / 100
     */
    fun decodeMapPressureKpa(data: ByteArray): Int {
        if (data.isEmpty()) return 101
        return data[0].toInt() and 0xFF
    }

    fun decodeBoostPressureBar(data: ByteArray): Float {
        val mapKpa = decodeMapPressureKpa(data)
        val boostKpa = (mapKpa - 101.3f).coerceAtLeast(0f)
        return (boostKpa / 100f * 100).toInt() / 100f
    }

    /**
     * Mass Air Flow Sensor (MAF) (PID 0x10)
     * Formula: ((A * 256) + B) / 100
     * Unit: g/s
     * Can estimate Fuel Flow (L/h) ~ MAF * 3600 / (14.7 * 745) / 10
     */
    fun decodeMafAirFlow(data: ByteArray): Float {
        if (data.size < 2) return 0f
        val a = data[0].toInt() and 0xFF
        val b = data[1].toInt() and 0xFF
        return ((a * 256) + b) / 100f
    }

    fun estimateFuelRateLph(mafGps: Float, speedKmh: Int): Float {
        if (mafGps <= 0f) return 0f
        // Gasoline air-fuel ratio ~14.7, density ~745 g/L
        val lph = (mafGps * 3600f) / (14.7f * 745f)
        return (lph * 10).toInt() / 10f
    }

    /**
     * Control Module Voltage / Battery Voltage (PID 0x42)
     * Formula: ((A * 256) + B) / 1000
     * Unit: V
     */
    fun decodeControlModuleVoltage(data: ByteArray): Float {
        if (data.size < 2) return 0f
        val a = data[0].toInt() and 0xFF
        val b = data[1].toInt() and 0xFF
        return ((a * 256) + b) / 1000f
    }

    /**
     * Parses Battery Voltage from ELM327 'ATRV' response (e.g. "12.6V", "13.8V")
     */
    fun parseAtRvVoltage(raw: String): Float {
        val clean = raw.replace("V", "", ignoreCase = true)
            .replace(">", "")
            .replace("\r", "")
            .replace("\n", "")
            .trim()
        return clean.toFloatOrNull() ?: 12.6f
    }
}
