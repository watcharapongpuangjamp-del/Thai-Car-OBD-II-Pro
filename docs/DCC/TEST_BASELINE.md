# Test Baseline Register

## Mandatory Validation Matrix (Change CHG-2026-008 - Diagnostic Rule Engine Layer)

| Test Case ID | Description | Mode | Expected Result | Pass / Fail |
| :--- | :--- | :--- | :--- | :--- |
| **TC-RE-01** | Engine Coolant Temperature Threshold Rules | OFFLINE/BOTH | Correctly identifies Cold, Normal, Warning, Overheating, and Sensor Open/Short faults | Pass |
| **TC-RE-02** | Electrical Voltage & Alternator Rules | OFFLINE/BOTH | Accurately identifies Resting Undervoltage, Running Undercharging, and Overvoltage Hazard | Pass |
| **TC-RE-03** | Fuel Delivery, Overboost & Load Imbalance Rules | OFFLINE/BOTH | Correctly flags Idle Over-fueling (>3.5 L/h), Overboost (>1.8 bar), and Load/Throttle mismatch | Pass |
| **TC-RE-04** | Physical Sensor Cross-Validation Plausibility | OFFLINE/BOTH | Detects implausible states (e.g. speed > 25 km/h with 0 RPM, or neutral over-revving > 5500 RPM) | Pass |
| **TC-RE-05** | Gemini Prompt Factual Pre-validation Grounding | OFFLINE/AI | Injects deterministic rule facts into prompt, attaches RuleEngineReport to AiAnalysisResult | Pass |

## Mandatory Validation Matrix (Change CHG-2026-007 - Phase 4 Trip Analytics & Integrity)

| Test Case ID | Description | Mode | Expected Result | Pass / Fail |
| :--- | :--- | :--- | :--- | :--- |
| **TC-P4-01** | Trip Distance & Fuel Accumulator Precision | REAL & SIM | Accurately calculates distance delta (m) and fuel volume (mL) per interval | Pass |
| **TC-P4-02** | Idle Time Discrimination | REAL & SIM | Correctly classifies idle duration when vehicle speed is 0 and RPM > 400 | Pass |
| **TC-P4-03** | Average Fuel Economy Calculation (L/100km) | REAL & SIM | Evaluates ratio of fuel consumed to distance traveled without divide-by-zero | Pass |
| **TC-P4-04** | Diagnostic CSV Exporter Schema & Content | BOTH | Generates compliant CSV format for telemetry and multi-module DTC codes | Pass |
| **TC-P4-05** | Cryptographic SHA-256 Checksum Integrity | BOTH | Produces 64-character hex digest verifying log data provenance and authenticity | Pass |

## Mandatory Validation Matrix (Change CHG-2026-006 - Phase 3 Real Diagnostics)

| Test Case ID | Description | Mode | Expected Result | Pass / Fail |
| :--- | :--- | :--- | :--- | :--- |
| **TC-P3-01** | SAE J1979 Formula Exactness (RPM, Speed, ECT, MAP) | REAL_HARDWARE | Evaluates exact standard scaling factors without truncation or rounding error | Pass |
| **TC-P3-02** | Battery Voltage Acquisition (ATRV & PID 0x42) | REAL_HARDWARE | Correctly parses ELM327 voltage string format and returns valid float in Volts | Pass |
| **TC-P3-03** | 10Hz Non-blocking Polling Loop & Stream Health | REAL_HARDWARE | Telemetry stream maintains error thresholds and calculates dynamic PID/sec | Pass |
| **TC-P3-04** | SAE J2019 DTC Multi-Prefix Decoder (P, C, B, U) | REAL_HARDWARE | Decodes 2-byte hexadecimal pairs into standard 5-character alphanumeric DTCs | Pass |
| **TC-P3-05** | Multi-Module Header Switching & Mode 04 Clear | REAL_HARDWARE | ATSH targeted requests to ECM/TCM/ABS/SRS and transmits Mode 04 clear command | Pass |

## Mandatory Validation Matrix (Change CHG-2026-005 - Phase 2 Protocol Engine)

| Test Case ID | Description | Mode | Expected Result | Pass / Fail |
| :--- | :--- | :--- | :--- | :--- |
| **TC-P2-01** | Response Normalizer Artifact Stripping | REAL_HARDWARE | Strips prompt (`>`), echoes, `SEARCHING...`, and trailing CRLF correctly | Pass |
| **TC-P2-02** | OBD Frame Multi-format Parsing | REAL_HARDWARE | Parses standard spaced, compact hex, and CAN header frames into `ObdFrame` | Pass |
| **TC-P2-03** | Error Response Detection | REAL_HARDWARE | Detects `NO DATA`, `UNABLE TO CONNECT`, `CAN ERROR`, and rejects corrupted frames | Pass |
| **TC-P2-04** | Protocol Classification from ELM code | REAL_HARDWARE | Maps standard ELM protocol responses to typed `ObdProtocol` enum entries | Pass |
| **TC-P2-05** | Mode 01 PID Discovery Bitmask Decoding | REAL_HARDWARE | Decodes 32-bit bitmasks from `0100`, `0120`, `0140` into supported PID set | Pass |

## Mandatory Validation Matrix (Change CHG-2026-004 - Phase 1 Foundation)

| Test Case ID | Description | Mode | Expected Result | Pass / Fail |
| :--- | :--- | :--- | :--- | :--- |
| **TC-P1-01** | USB Permission State Flow Verification | REAL_HARDWARE | Returns `PERMISSION_REQUIRED` on missing permission, never fake `PERMISSION_GRANTED` | Pass |
| **TC-P1-02** | Connection State Machine Coverage | BOTH | All 12 progression states and 9 error states correctly indexed with bilingual labels | Pass |
| **TC-P1-03** | USB Transport Layer Diagnostics & Reconfig | REAL_HARDWARE | `UsbTransport` interface properly abstracts `usb-serial-for-android` with baud rate support (38400, 115200) | Pass |
| **TC-P1-04** | USB Detach Lifecycle Clean-up | REAL_HARDWARE | Detach event triggers `close()`, clears active device, and sets `DEVICE_DISCONNECTED` state | Pass |
| **TC-P1-05** | Diagnostic Error Model Integrity | BOTH | Typed exceptions provide structured error codes and bilingual user-facing messages | Pass |

## Mandatory Validation Matrix (Change CHG-2026-001)

| Test Case ID | Description | Mode | Expected Result | Pass / Fail |
| :--- | :--- | :--- | :--- | :--- |
| **TC-01** | App launch with no USB device attached | REAL_HARDWARE | State: `DISCONNECTED`, Sensors: `N/A`, No fake values shown | Pass |
| **TC-02** | USB OTG ELM327 Connected & Permission Granted | REAL_HARDWARE | State Machine progresses through handshake to `CONNECTED` | Pass |
| **TC-03** | Switch to Simulator Mode | SIMULATOR | Synthetic Virtual CAN stream active with prominent `SIMULATOR` tag | Pass |
| **TC-04** | DTC Multi-Module Scan | REAL vs SIM | Real ECU codes in Real Mode, Test scenario DTCs in Sim Mode | Pass |
| **TC-05** | Gemini AI Mechanic Analysis | BOTH | Diagnostic summary rendered with explicit data provenance | Pass |
| **TC-06** | Local Persistence (Room DB) | BOTH | Scan history logs saved and retrievable offline | Pass |
