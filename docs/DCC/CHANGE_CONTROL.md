# Change Control Register

## Change Record: CHG-2026-009
- **Title:** Diagnostic Report Exporter Interface & Native PDF Exporter Implementation
- **Date:** 2026-08-17
- **Author/Agent:** Senior Android Automotive Diagnostic Engineer / Agent
- **Previous Version:** 2.6.0-RULE-ENGINE
- **Target Version:** 2.6.0-PDF-EXPORTER
- **Application ID:** `com.aistudio.thaiautoobd.diagnostics`
- **Reason:** Implement `DiagnosticReportExporter` interface and `PdfExporter` native Android PDF engine using `DiagnosticSession` and `DtcCode` models to generate formatted, multi-page vehicle diagnostic inspection reports with telemetry snapshots, DTC tables, Rule Engine Safety Shield evaluations, and SHA-256 integrity signatures.
- **Affected Components:**
  - `com.example.export.DiagnosticReportExporter.kt`
  - `com.example.export.PdfExporter.kt`
  - `com.example.model.Models.kt` (DiagnosticSession)
  - `com.example.repository.VehicleRepository.kt`
  - `com.example.viewmodel.MainViewModel.kt`
  - `com.example.ui.screens.DtcScannerScreen.kt`
  - `com.example.ui.ThaiObdApp.kt`
  - `com.example.PdfExporterUnitTest.kt`
- **Risk:** Low (Export Layer & UI Action)
- **Test Result:** Passed Build Verification (`compile_applet`) & Unit Tests
- **Rollback Point:** v2.6.0-RULE-ENGINE

## Change Record: CHG-2026-008

- **Title:** Core Diagnostic Rule Engine Layer, Predefined Thresholds & Deterministic AI Telemetry Pre-validation
- **Date:** 2026-08-17
- **Author/Agent:** Senior Android Automotive Diagnostic Engineer / Agent
- **Previous Version:** 2.6.0-PHASE4
- **Target Version:** 2.6.0-RULE-ENGINE
- **Application ID:** `com.aistudio.thaiautoobd.diagnostics`
- **Reason:** Implement core deterministic diagnostic rule engine layer with predefined physical threshold classes (EngineTemperatureThresholds, VoltageThresholds, FuelAndLoadThresholds) operating independently of the AI to validate telemetry and sensor plausibility before enriching Gemini AI prompts.
- **Affected Components:**
  - `com.example.rules.DiagnosticRuleModels.kt`
  - `com.example.rules.DiagnosticThresholds.kt`
  - `com.example.rules.DiagnosticRuleEngine.kt`
  - `com.example.ui.components.DiagnosticAlertComponent.kt`
  - `com.example.model.Models.kt`
  - `com.example.repository.VehicleRepository.kt`
  - `com.example.ui.screens.DashboardScreen.kt`
  - `com.example.ui.screens.AiMechanicScreen.kt`
  - `com.example.DiagnosticRuleEngineUnitTest.kt`
- **Risk:** Low (Pre-processing & Rule Layer)
- **Test Result:** Passed Build Verification (`compile_applet`) & Unit Tests
- **Rollback Point:** v2.6.0-PHASE4

## Change Record: CHG-2026-007
- **Title:** Phase 4: Trip Analytics Engine, Telemetry CSV Export & Data Provenance Integrity
- **Date:** 2026-08-17
- **Author/Agent:** Senior Android Automotive Diagnostic Engineer / Agent
- **Previous Version:** 2.6.0-PHASE3
- **Target Version:** 2.6.0-PHASE4
- **Application ID:** `com.aistudio.thaiautoobd.diagnostics`
- **Reason:** Implement Phase 4 of Master Prompt: Continuous trip calculation (distance, avg speed, fuel economy L/100km, total fuel consumed, idle time tracking), Trip Computer HUD in Dashboard UI, and cryptographic SHA-256 verified CSV export formatting for DTCs and telemetry time series.
- **Affected Components:**
  - `com.example.model.AnalyticsModels.kt`
  - `com.example.analytics.TripAnalyticsEngine.kt`
  - `com.example.analytics.DiagnosticExportExporter.kt`
  - `com.example.ui.screens.DashboardScreen.kt`
  - `com.example.ui.ThaiObdApp.kt`
  - `com.example.repository.VehicleRepository.kt`
  - `com.example.viewmodel.MainViewModel.kt`
  - `com.example.Phase4AnalyticsUnitTest.kt`
- **Risk:** Low (Analytics Layer)
- **Test Result:** Passed Build Verification (`compile_applet`) & Unit Tests
- **Rollback Point:** v2.6.0-PHASE3

## Change Record: CHG-2026-006
- **Title:** Phase 3: Real Diagnostics (SAE J1979 PID Decoder, Telemetry Polling Engine & Multi-Module DTC Scanner)
- **Date:** 2026-08-17
- **Author/Agent:** Senior Android Automotive Diagnostic Engineer / Agent
- **Previous Version:** 2.6.0-PHASE2
- **Target Version:** 2.6.0-PHASE3
- **Application ID:** `com.aistudio.thaiautoobd.diagnostics`
- **Reason:** Implement Phase 3 of Master Prompt: Strict mathematical PID decoding formulas (RPM, Speed, ECT, Throttle, Load, IAT, MAP/Boost, MAF/Fuel Rate, Battery Voltage), active 10 Hz non-blocking telemetry stream, SAE J2019 DTC decoder across P/C/B/U prefixes, multi-ECU header routing (ECM, TCM, ABS, SRS, BCM), and Mode 04 DTC clearance.
- **Affected Components:**
  - `com.example.hardware.obd.PidDecoder.kt`
  - `com.example.hardware.obd.DtcDecoder.kt`
  - `com.example.hardware.obd.TelemetryPollingEngine.kt`
  - `com.example.hardware.obd.RealDtcScanner.kt`
  - `com.example.hardware.UsbObdDriver.kt`
  - `com.example.repository.VehicleRepository.kt`
  - `com.example.viewmodel.MainViewModel.kt`
  - `com.example.Phase3RealDiagnosticsUnitTest.kt`
- **Risk:** Low (Isolated Diagnostic Processing)
- **Test Result:** Passed Build Verification (`compile_applet`) & Unit Tests
- **Rollback Point:** v2.6.0-PHASE2

## Change Record: CHG-2026-005
- **Title:** Phase 2: ELM327 Handshake, Response Normalizer, OBD Frame Parser & Protocol Engine
- **Date:** 2026-08-17
- **Author/Agent:** Senior Android Automotive Diagnostic Engineer / Agent
- **Previous Version:** 2.6.0-PHASE1
- **Target Version:** 2.6.0-PHASE2
- **Application ID:** `com.aistudio.thaiautoobd.diagnostics`
- **Reason:** Implement Phase 2 of Master Prompt: ELM327 strict handshake sequence (ATZ, ATE0, ATL0, ATS0, ATH0, ATSP0, 0100 validation), multi-format response normalizer, OBD frame model, dynamic protocol detection, and PID discovery bitmask parser.
- **Affected Components:**
  - `com.example.hardware.obd.ObdModels.kt`
  - `com.example.hardware.obd.ObdResponseNormalizer.kt`
  - `com.example.hardware.obd.Elm327Driver.kt`
  - `com.example.hardware.obd.ObdProtocolEngine.kt`
  - `com.example.hardware.UsbObdDriver.kt`
  - `com.example.Phase2ProtocolUnitTest.kt`
- **Risk:** Low (Layered Protocol Isolation)
- **Test Result:** Passed Build Verification (`compile_applet`) & Unit Tests
- **Rollback Point:** v2.6.0-PHASE1

## Change Record: CHG-2026-004
- **Title:** Phase 1 Foundation: USB Permission Lifecycle, State Machine & Transport Layer
- **Date:** 2026-08-17
- **Author/Agent:** Senior Android Automotive Diagnostic Engineer / Agent
- **Previous Version:** 2.5.0
- **Target Version:** 2.6.0-PHASE1
- **Application ID:** `com.aistudio.thaiautoobd.diagnostics`
- **Reason:** Implement Phase 1 Foundation of Master Prompt: Fix USB permission logic, formalize connection state machine, build dedicated UsbTransport layer, handle USB attach/detach lifecycle events, and create typed DiagnosticError model.
- **Affected Components:**
  - `com.example.model.Models.kt`
  - `com.example.model.DiagnosticErrors.kt`
  - `com.example.hardware.transport.UsbTransport.kt`
  - `com.example.hardware.usb.UsbPermissionManager.kt`
  - `com.example.hardware.UsbObdDriver.kt`
  - `com.example.service.UsbCommunicationService.kt`
  - `com.example.repository.VehicleRepository.kt`
  - `com.example.viewmodel.MainViewModel.kt`
  - `com.example.ui.components.ModeHeader.kt`
  - `com.example.Phase1FoundationUnitTest.kt`
- **Risk:** Medium (Architecture Refactoring)
- **Test Result:** Passed Build Verification (`compile_applet`) & Unit Tests
- **Rollback Point:** v2.5.0

## Change Record: CHG-2026-001
- **Title:** Baseline Initialization, Real Hardware USB OTG Driver & Simulator Isolation
- **Date:** 2026-08-15
- **Author/Agent:** Senior Android Architect / Agent
- **Previous Version:** 1.0 (Template)
- **Target Version:** 2.5.0 (versionCode = 250)
- **Application ID:** `com.aistudio.thaiautoobd.diagnostics`
- **Reason:** Establish Production Pro baseline, create strict USB OTG OBD-II real hardware pipeline, enforce simulator synthetic data isolation, and provide Room DB + Gemini AI Mechanic.
- **Affected Components:**
  - `app/build.gradle.kts`
  - `metadata.json`
  - `strings.xml` / `settings.gradle.kts`
  - `UsbObdDriver` / `RealHardwareSource`
  - `Obd2EmulatorService` / `SimulatorDataSource`
  - `ObdDatabase` / Room Entities & DAOs
  - `MainViewModel` / Diagnostic Screens
- **Risk:** High (Core System Foundation)
- **Test Result:** Passed Build Verification (`compile_applet`)
- **Rollback Point:** Initial Template Commit
