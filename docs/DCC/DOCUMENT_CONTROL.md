# DCC Document Control Policy

**Project:** Thai Car OBD-II Pro  
**Application ID:** `com.aistudio.thaiautoobd.diagnostics`  
**Current Version:** `2.5.0`  
**Build Code:** `250`  

---

## Document Control Objectives
1. Maintain system continuity and architectural integrity across all software revisions.
2. Prevent accidental mixing of incompatible revisions or unauthorized mock data injection into `REAL_HARDWARE` mode pipelines.
3. Ensure single source of truth across Gradle (`versionName`), Android Manifest, Application UI, and Release Documentation.

## Authoritative Specifications
- **Real Hardware Specification:** `docs/DCC/REAL_HARDWARE_SPEC.md`
- **Simulator Specification:** `docs/DCC/SIMULATOR_SPEC.md`
- **Architecture Revision Register:** `docs/DCC/ARCHITECTURE_REVISION.md`
- **Change Control Log:** `docs/DCC/CHANGE_CONTROL.md`
- **Test Baseline:** `docs/DCC/TEST_BASELINE.md`
