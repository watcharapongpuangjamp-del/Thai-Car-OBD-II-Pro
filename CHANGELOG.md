# Changelog

## v2.5.0 Production Pro
### Added
- Complete DCC Document Control framework (`docs/DCC/`).
- Strict `AppOperationMode` architecture (`REAL_HARDWARE` vs `SIMULATOR`).
- USB OTG OBD-II Driver (`UsbObdDriver`) for ELM327 / FTDI / CH340 / PL2303 chips with multi-stage connection state machine.
- Virtual CAN Simulator (`Obd2EmulatorService`) for testing Idle, Cruise, Boost, Overheat, and Fault scenarios.
- Room Database persistence (`ObdDatabase`) for Scan History, Vehicle Profiles, and Maintenance Logs.
- Gemini AI Mechanic with Thai/English diagnostic advice, severity evaluation, and strict provenance labeling.
- Material 3 Jetpack Compose UI with Thai localization, gauge displays, multi-module DTC scanner, predictive maintenance RUL, and vehicle profile manager.

### Fixed
- Fixed build and manifest metadata matching `com.aistudio.thaiautoobd.diagnostics`.
- Eliminated mock data fallbacks in Real Hardware mode.
