# Real Hardware Specification

## Operation Mode: AppOperationMode.REAL_HARDWARE

### Hardware Pipeline Architecture
```
Physical USB Device (OTG Adapter: ELM327 / FTDI / CH340 / PL2303)
       ↓
Android USB Host Permission
       ↓
USB Serial Interface Claim & Baud Setup (38400 / 115200 bps)
       ↓
ELM327 Protocol Handshake (ATZ, ATE0, ATL0, ATS0, ATH0, ATSP0)
       ↓
ECU Communication Validation (PID 0100)
       ↓
Live Sensor Telemetry Parsing
       ↓
Validated Real Telemetry → UI
```

### Strict Real Mode Rules
1. **NO Mock Fallbacks:** In `REAL_HARDWARE` mode, never inject synthetic default values (e.g., 85°C, 14.1V, 1850 RPM) when connection or ECU response fails.
2. **Explicit Connection State Machine:**
   `DISCONNECTED` → `DEVICE_DETECTED` → `PERMISSION_GRANTED` → `USB_OPEN` → `SERIAL_READY` → `ADAPTER_HANDSHAKE` → `ADAPTER_RESPONDING` → `PROTOCOL_DETECTED` → `ECU_RESPONDING` → `LIVE_DATA_VALIDATED` → `CONNECTED`.
3. **Data Integrity:** Missing or unsupported PIDs are explicitly reported as `N/A`, `NO DATA`, or `UNSUPPORTED`.
