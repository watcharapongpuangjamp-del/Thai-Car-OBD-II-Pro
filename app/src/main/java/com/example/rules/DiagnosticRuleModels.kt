package com.example.rules

import com.example.model.DtcSeverity

enum class EvaluationSeverity {
    NORMAL,
    INFO,
    WARNING,
    CRITICAL,
    FAULT
}

enum class DiagnosticCategory {
    ENGINE_TEMPERATURE,
    FUEL_AND_LOAD,
    ELECTRICAL_VOLTAGE,
    SENSOR_PLAUSIBILITY,
    CORRELATED_DTC
}

data class DiagnosticAnomaly(
    val category: DiagnosticCategory,
    val severity: EvaluationSeverity,
    val parameterName: String,
    val measuredValue: String,
    val expectedRange: String,
    val titleTh: String,
    val descriptionTh: String,
    val potentialCausesTh: List<String>,
    val recommendedActionTh: String
)

data class TemperatureEvaluation(
    val severity: EvaluationSeverity,
    val statusLabelTh: String,
    val anomaly: DiagnosticAnomaly? = null
)

data class VoltageEvaluation(
    val severity: EvaluationSeverity,
    val statusLabelTh: String,
    val anomaly: DiagnosticAnomaly? = null
)

data class FuelAndLoadEvaluation(
    val severity: EvaluationSeverity,
    val statusLabelTh: String,
    val anomalies: List<DiagnosticAnomaly> = emptyList()
)

data class RuleEngineReport(
    val overallSeverity: EvaluationSeverity,
    val isTelemetryPlausible: Boolean,
    val anomalies: List<DiagnosticAnomaly>,
    val summaryTh: String,
    val aiEnrichmentContext: String
)
