# Changelog

## v2.6.0-PDF-EXPORTER (2026-08-17)
### Added
- **Diagnostic Report Exporter & PDF Generator**:
  - `DiagnosticReportExporter`: Core interface contract for stream-based and file-based diagnostic reporting.
  - `PdfExporter`: Production implementation rendering structured A4 multi-page automotive inspection reports using native Android `PdfDocument` and Canvas.
  - Generates comprehensive PDF reports including:
    - Executive Header with Session ID, Date/Time, and Hardware/Simulator mode provenance.
    - Vehicle Information Card (Make, Model, Year, Plate, VIN, Mileage, Technician).
    - Diagnostic Safety Shield & Rule Engine Assessment with color-coded severity badges.
    - Full Diagnostic Trouble Codes (DTC) table with ECM/TCM/ABS/SRS module routing, severity ratings, and Thai/English technical descriptions.
    - Live Sensor Telemetry snapshot table (RPM, Speed, ECT, Voltage, Boost, Fuel Rate, Throttle, Load).
    - AI Mechanic expert findings and recommended workshop maintenance actions.
    - SHA-256 Cryptographic Checksum integrity footer and page numbering.
  - Unit test suite (`PdfExporterUnitTest.kt`) verifying byte stream and direct file export.
  - Integrated "PDF" quick export button in `DtcScannerScreen` with Snackbar notification.

## v2.6.0-RULE-ENGINE (2026-08-17)

### Added
- **Diagnostic Alert Component (`DiagnosticAlertComponent.kt`)**:
  - Reusable Compose component rendering color-coded warning banners, severity badges (CRITICAL, FAULT, WARNING, INFO, SAFE), and expandable anomaly breakdowns.
  - Displays measured value vs expected safety ranges, potential root causes, and recommended automotive technician actions.
  - Direct integration into `DashboardScreen` and `AiMechanicScreen` with "ปรึกษา AI Mechanic" one-tap consultation action.
- **Core Diagnostic Rule Engine Layer & Predefined Threshold Classes**:
  - `EngineTemperatureThresholds`: Predefined thresholds for cold warmup (<65°C), normal (80-95°C), elevated warning (98°C), severe overheat (105°C), and sensor short/open circuit (-35°C / 135°C).
  - `VoltageThresholds`: Predefined thresholds for dead battery (<11.5V), resting undervoltage (<12.0V), undercharging alternator (<13.2V running), normal alternator (13.2-14.8V), overvoltage hazard (>15.2V), and sensor faults (<5V / >18V).
  - `FuelAndLoadThresholds`: Predefined thresholds for high idle fuel rate (>3.5 L/h), overboost (>1.8 bar / >2.2 bar), and engine load vs throttle position mechanical drag imbalances.
  - `DiagnosticRuleEngine`: Independent deterministic evaluation engine validating sensor plausibility, physical cross-metrics, and DTC correlations before passing grounded factual contexts to Gemini AI.
  - Enhanced `AiMechanicScreen` and `AiAnalysisResult` with dedicated offline Rule Engine Validation reporting and unit tests (`DiagnosticRuleEngineUnitTest.kt`).

## v2.6.0-PHASE4 (2026-08-17)
### Added
- **Phase 4 Trip Analytics, Telemetry Logging & Data Integrity**:
  - `TripAnalyticsEngine` calculating real-time trip metrics: distance accumulation, average speed, max RPM, fuel consumption (L/100km & total liters), idle time detection, and peak coolant tracking.
  - Interactive Trip Computer HUD integrated directly into `DashboardScreen` allowing users to start, pause, and end trip recordings.
  - `DiagnosticExportExporter` generating CSV exports for telemetry time-series and multi-module DTC diagnostic reports with SHA-256 integrity checksums.
  - Phase 4 unit test suite (`Phase4AnalyticsUnitTest.kt`).

## v2.6.0-PHASE3 (2026-08-17)
### Added
- **Phase 3 Real Diagnostics (PID Decoder & DTC Engine)**:
  - `PidDecoder` implementing exact standard SAE J1979 OBD formulas: RPM (`((A*256)+B)/4`), Speed (`A`), Coolant Temp (`A-40`), Throttle (`A*100/255`), Engine Load (`A*100/255`), Intake Air Temp (`A-40`), MAP & Boost (`A-101.3 kPa`), MAF & Fuel Rate (`L/h`), Battery Voltage (`ATRV` + PID `0x42`).
  - `TelemetryPollingEngine` running active 10 Hz non-blocking polling stream with consecutive error tracking, PID query counter (PID/sec), and cycle latency metrics.
  - SAE J2019 / ISO 15031-6 `DtcDecoder` decoding 2-byte DTC structures across Powertrain (P), Chassis (C), Body (B), and Network (U) prefixes.
  - Multi-module `RealDtcScanner` supporting Mode 03 (Confirmed), Mode 07 (Pending), Mode 0A (Permanent) and targeted ECU header routing (`7E0` ECM, `7E1` TCM, `7E2` ABS, `7E3` SRS, `7E4` BCM) plus Mode 04 Clear DTC execution.
  - Phase 3 unit test suite (`Phase3RealDiagnosticsUnitTest.kt`).

## v2.6.0-PHASE2 (2026-08-17)
### Added
- **Phase 2 Transport Layer & Protocol Engine**:
  - `ObdResponseNormalizer` handling raw byte cleanup, prompt removal (`>`), echo cancellation, and SEARCHING artifact stripping.
  - Multi-format `ObdFrame` parser supporting standard spaced hex (`41 0C 1A F8`), compact hex (`410C1AF8`), and CAN header/PCI formats (`7E8 04 41 0C 1A F8`).
  - Production `Elm327Driver` executing verified automotive handshake sequence: `ATZ`, `ATE0`, `ATL0`, `ATS0`, `ATH0`, `ATI`, `ATSP0`, and `0100` ECU connectivity validation.
  - `ObdProtocolEngine` with dynamic `ObdProtocol` resolution (ISO 15765-4 CAN, ISO 14230 KWP, ISO 9141-2, SAE J1850) and Mode 01 PID discovery engine (`0100`, `0120`, `0140`).
  - Mode 09 PID 02 VIN query parsing for vehicle chassis identification.
  - Phase 2 Protocol unit test suite (`Phase2ProtocolUnitTest.kt`).

## v2.6.0-PHASE1 (2026-08-17)
### Added
- **Phase 1 Foundation Architecture**:
  - Full 12-stage connection progression state machine + 9 explicit error states in `ConnectionState`.
  - `DataProvenance` enum (`REAL_HARDWARE`, `SIMULATOR`, `USER_ENTERED`, `HISTORICAL`, `ESTIMATED`, `AI_INFERRED`) and generic `TelemetryValue<T>` model.
  - Typed diagnostic error hierarchy (`DiagnosticError`: `UsbError`, `SerialError`, `Elm327Error`, `ProtocolError`, `EcuError`, `ParserError`, `DatabaseError`, `AiError`) with bilingual user messages.
  - Dedicated `UsbTransport` interface and `AndroidUsbTransport` abstraction with configurable `SerialConfig` (38400, 115200 baud).
  - Lifecycle-aware `UsbPermissionManager` managing `USB_DEVICE_ATTACHED`, `USB_DEVICE_DETACHED`, and `ACTION_USB_PERMISSION` with automated port cleanup upon detach.
  - Phase 1 unit test suite (`Phase1FoundationUnitTest.kt`).

### Fixed
- Fixed critical USB permission bug where `!hasPermission` returned `PERMISSION_GRANTED`.
- Fixed lifecycle resource leaks by binding ViewModel and Repository disposal (`onCleared()` -> `release()`).
- Replaced direct UI-hardware coupling with layered Architecture.

## v2.5.0 Production Pro

### Added
- Complete DCC Document Control framework (`docs/DCC/`).
- Strict `AppOperationMode` architecture (`REAL_HARDWARE` vs `SIMULATOR`).
- USB OTG OBD-II Driver (`UsbObdDriver`) and Background Service (`UsbCommunicationService`) for managing USB device connection, permission requests, and lifecycle management.
- DTC Scan functionality (`scanDtcCodes`) in `UsbObdDriver` for ECM/TCM/ABS/SRS module scanning.
- Virtual CAN Simulator (`Obd2EmulatorService`) for testing Idle, Cruise, Boost, Overheat, and Fault scenarios.
- Room Database persistence (`ObdDatabase`) for Scan History, Vehicle Profiles, and Maintenance Logs.
- Gemini AI Mechanic with Thai/English diagnostic advice, severity evaluation, and strict provenance labeling.
- Material 3 Jetpack Compose UI with Thai localization, gauge displays, multi-module DTC scanner, predictive maintenance RUL, and vehicle profile manager.

### Fixed
- Fixed build and manifest metadata matching `com.aistudio.thaiautoobd.diagnostics`.
- Eliminated mock data fallbacks in Real Hardware mode.
