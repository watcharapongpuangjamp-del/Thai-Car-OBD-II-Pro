# Test Baseline Register

## Mandatory Validation Matrix (Change CHG-2026-001)

| Test Case ID | Description | Mode | Expected Result | Pass / Fail |
| :--- | :--- | :--- | :--- | :--- |
| **TC-01** | App launch with no USB device attached | REAL_HARDWARE | State: `DISCONNECTED`, Sensors: `N/A`, No fake values shown | Pass |
| **TC-02** | USB OTG ELM327 Connected & Permission Granted | REAL_HARDWARE | State Machine progresses through handshake to `CONNECTED` | Pass |
| **TC-03** | Switch to Simulator Mode | SIMULATOR | Synthetic Virtual CAN stream active with prominent `SIMULATOR` tag | Pass |
| **TC-04** | DTC Multi-Module Scan | REAL vs SIM | Real ECU codes in Real Mode, Test scenario DTCs in Sim Mode | Pass |
| **TC-05** | Gemini AI Mechanic Analysis | BOTH | Diagnostic summary rendered with explicit data provenance | Pass |
| **TC-06** | Local Persistence (Room DB) | BOTH | Scan history logs saved and retrievable offline | Pass |
