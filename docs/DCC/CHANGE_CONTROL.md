# Change Control Register

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
