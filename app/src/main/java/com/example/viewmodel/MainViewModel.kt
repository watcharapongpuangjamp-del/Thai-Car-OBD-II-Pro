package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.db.MaintenanceLogEntity
import com.example.db.VehicleProfileEntity
import com.example.hardware.SimulatorScenario
import com.example.model.AiAnalysisResult
import com.example.model.AppOperationMode
import com.example.model.ConnectionState
import com.example.model.DtcCode
import com.example.model.LiveSensorData
import com.example.model.PredictiveMaintenanceItem
import com.example.repository.VehicleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val repository = VehicleRepository(application)

    val activeMode: StateFlow<AppOperationMode> = repository.activeMode
    val liveTelemetry: StateFlow<LiveSensorData> = repository.liveTelemetry.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = LiveSensorData.disconnected(AppOperationMode.REAL_HARDWARE, "เริ่มต้นระบบ")
    )

    private val _dtcCodes = MutableStateFlow<List<DtcCode>>(emptyList())
    val dtcCodes: StateFlow<List<DtcCode>> = _dtcCodes.asStateFlow()

    val diagnosticRuleReport: StateFlow<com.example.rules.RuleEngineReport> = combine(liveTelemetry, dtcCodes) { telemetry, dtcs ->
        repository.diagnosticRuleEngine.evaluate(telemetry, dtcs)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = repository.diagnosticRuleEngine.evaluate(
            LiveSensorData.disconnected(AppOperationMode.REAL_HARDWARE, "เริ่มต้นระบบ"),
            emptyList()
        )
    )

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _aiResult = MutableStateFlow<AiAnalysisResult?>(null)
    val aiResult: StateFlow<AiAnalysisResult?> = _aiResult.asStateFlow()

    private val _isAiAnalyzing = MutableStateFlow(false)
    val isAiAnalyzing: StateFlow<Boolean> = _isAiAnalyzing.asStateFlow()

    val vehicleProfiles: StateFlow<List<VehicleProfileEntity>> = repository.allProfiles.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _selectedProfile = MutableStateFlow<VehicleProfileEntity?>(null)
    val selectedProfile: StateFlow<VehicleProfileEntity?> = _selectedProfile.asStateFlow()

    val currentScenario = repository.emulatorService.currentScenario

    val activeTripStatus = repository.tripAnalytics.activeTripStatus
    val currentTripSummary = repository.tripAnalytics.currentTripSummary

    init {
        // Collect telemetry for Trip Analytics
        viewModelScope.launch {
            liveTelemetry.collect { sample ->
                repository.tripAnalytics.ingestTelemetrySample(sample)
            }
        }
        // Initialize default vehicle profile if database is empty
        viewModelScope.launch {
            repository.allProfiles.collect { profiles ->
                if (profiles.isNotEmpty() && _selectedProfile.value == null) {
                    _selectedProfile.value = profiles.first()
                }
            }
        }
    }

    fun setMode(mode: AppOperationMode) {
        repository.setOperationMode(mode)
    }

    fun connectUsbHardware() {
        viewModelScope.launch {
            repository.connectRealHardware()
        }
    }

    fun disconnectUsbHardware() {
        repository.disconnectRealHardware()
    }

    fun setSimulatorScenario(scenario: SimulatorScenario) {
        repository.setSimulatorScenario(scenario)
    }

    fun scanDtcs() {
        viewModelScope.launch {
            _isScanning.value = true
            val codes = repository.scanDtcs()
            _dtcCodes.value = codes
            _isScanning.value = false

            _selectedProfile.value?.let { profile ->
                repository.saveScanRecord(profile.id, codes)
            }
        }
    }

    fun clearDtcs() {
        viewModelScope.launch {
            _isScanning.value = true
            repository.clearDtcs()
            _dtcCodes.value = emptyList()
            _aiResult.value = null
            _isScanning.value = false
        }
    }

    fun requestAiMechanicAnalysis() {
        viewModelScope.launch {
            _isAiAnalyzing.value = true
            val profileInfo = _selectedProfile.value?.let {
                "${it.make} ${it.model} ปี ${it.year} เครื่องยนต์ ${it.engineType} เลขกิโลเมตร ${it.odometerKm} กม."
            } ?: "รถยนต์ทั่วไป"

            val result = repository.analyzeWithAiMechanic(
                vehicleInfo = profileInfo,
                dtcCodes = _dtcCodes.value,
                telemetry = liveTelemetry.value
            )
            _aiResult.value = result
            _isAiAnalyzing.value = false
        }
    }

    fun getPredictiveMaintenanceItems(): List<PredictiveMaintenanceItem> {
        return repository.getPredictiveMaintenanceList(liveTelemetry.value)
    }

    fun selectVehicleProfile(profile: VehicleProfileEntity) {
        _selectedProfile.value = profile
    }

    fun addVehicleProfile(name: String, make: String, model: String, year: Int, engine: String, plate: String, mileage: Int) {
        viewModelScope.launch {
            val newProfile = VehicleProfileEntity(
                name = name,
                make = make,
                model = model,
                year = year,
                engineType = engine,
                licensePlate = plate,
                odometerKm = mileage
            )
            val id = repository.saveProfile(newProfile)
            _selectedProfile.value = newProfile.copy(id = id)
        }
    }

    fun addMaintenanceLog(title: String, cost: Double, mileage: Int, category: String) {
        val currentVehicleId = _selectedProfile.value?.id ?: 1L
        viewModelScope.launch {
            val log = MaintenanceLogEntity(
                vehicleId = currentVehicleId,
                titleTh = title,
                dateTimestamp = System.currentTimeMillis(),
                costBaht = cost,
                mileageKm = mileage,
                category = category
            )
            repository.saveMaintenanceLog(log)
        }
    }

    fun startTrip() {
        repository.tripAnalytics.startOrResumeTrip(activeMode.value)
    }

    fun pauseTrip() {
        repository.tripAnalytics.pauseTrip()
    }

    fun stopTrip(): com.example.model.TripSummary? {
        return repository.tripAnalytics.resetAndEndTrip()
    }

    private val _lastExportedPdfPath = MutableStateFlow<String?>(null)
    val lastExportedPdfPath: StateFlow<String?> = _lastExportedPdfPath.asStateFlow()

    fun exportDiagnosticPdfReport(onComplete: ((java.io.File?) -> Unit)? = null) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            val profile = _selectedProfile.value
            val currentTelemetry = liveTelemetry.value
            val currentDtcs = _dtcCodes.value
            val currentRuleReport = diagnosticRuleReport.value
            val currentAi = _aiResult.value

            val session = com.example.model.DiagnosticSession(
                sessionId = "DS-${System.currentTimeMillis() % 1000000}",
                timestamp = System.currentTimeMillis(),
                vehicleName = profile?.name ?: "รถยนต์ทดสอบ",
                vehicleMake = profile?.make ?: "Toyota",
                vehicleModel = profile?.model ?: "Hilux Revo",
                vehicleYear = profile?.year ?: 2022,
                vehicleVin = "MHFAB22G0K${100000 + (System.currentTimeMillis() % 900000)}",
                licensePlate = profile?.licensePlate ?: "1กข-9999 กทม.",
                odometerKm = profile?.odometerKm ?: 125400,
                dtcCodes = currentDtcs,
                telemetrySnapshot = currentTelemetry,
                ruleReport = currentRuleReport,
                aiAnalysis = currentAi,
                technicianName = "ช่างผู้ตรวจสอบระบบ Thai OBD-II Pro",
                mode = activeMode.value
            )

            val exportDir = java.io.File(context.cacheDir, "reports")
            exportDir.mkdirs()
            val targetFile = java.io.File(exportDir, "Diagnostic_Report_${session.sessionId}.pdf")

            val result = repository.pdfExporter.exportToFile(session, targetFile)
            if (result.isSuccess) {
                _lastExportedPdfPath.value = targetFile.absolutePath
                onComplete?.invoke(targetFile)
            } else {
                onComplete?.invoke(null)
            }
        }
    }

    override fun onCleared() {

        super.onCleared()
        repository.release()
    }
}
