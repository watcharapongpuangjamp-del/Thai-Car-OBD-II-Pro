# Simulator Specification

## Operation Mode: AppOperationMode.SIMULATOR

### Virtual CAN Simulation Architecture
```
Obd2EmulatorService (Virtual CAN Engine)
       ↓
Simulator Scenarios: Idle, Cruise, High Boost, Overheat, DTC Fault Injection
       ↓
Simulator Data Source
       ↓
Domain Model (Labeled: SIMULATOR / VIRTUAL CAN)
       ↓
UI Display with Prominent "SIMULATOR MODE" Indicator
```

### Simulator Guidelines
1. Synthetic data must always be tagged with `dataSource = AppOperationMode.SIMULATOR`.
2. All UI components, charts, DTC scan logs, and AI Mechanic prompts MUST display explicit simulator provenance badges.
3. Simulator mode must never contaminate real vehicle hardware databases.
