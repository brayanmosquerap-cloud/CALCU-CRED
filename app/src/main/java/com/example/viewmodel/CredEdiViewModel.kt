package com.example.viewmodel

import androidx.lifecycle.ViewModel
import com.example.model.AppTab
import com.example.model.ClinicalAgeCalculator
import com.example.model.CredEdiUiState
import com.example.model.CurvaIndicador
import com.example.model.EdiAgeDiscrepancyResolution
import com.example.model.EdiAreaFallbackEngine
import com.example.model.EdiAxis
import com.example.model.EdiDevArea
import com.example.model.EdiGroupAutoSelectionStatus
import com.example.model.EdiGroupChangeResolutionAction
import com.example.model.EdiGroupFilterCategory
import com.example.model.EdiInternalView
import com.example.model.EdiItemResponse
import com.example.model.EdiOfficialCatalog
import com.example.model.EdiPhase1SessionManager
import com.example.model.SemaforoColor
import com.example.model.SexoBiologico
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.Locale
import kotlin.math.roundToInt

class CredEdiViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(CredEdiUiState())
    val uiState: StateFlow<CredEdiUiState> = _uiState.asStateFlow()

    private fun syncEdi2026StateForAgeChange(state: CredEdiUiState): CredEdiUiState {
        val validResolution = state.ediAgeDiscrepancyResolution?.takeIf {
            it.matchesInputs(
                fn = state.fechaNacimiento,
                fe = state.fechaEvaluacion,
                prematuro = state.esPrematuro,
                sem = state.egSemanas,
                dias = state.egDias
            )
        }
        val comparison = EdiOfficialCatalog.compareCorrectedAgeMethods(
            fechaNacimiento = state.fechaNacimiento,
            fechaEvaluacion = state.fechaEvaluacion,
            esPrematuro = state.esPrematuro,
            egSemanas = state.egSemanas,
            egDias = state.egDias
        )

        var updatedEngine = state.ediEngineState
        var updatedOverride = state.ediManualGroupOverride

        if (comparison.autoSelectionStatus == EdiGroupAutoSelectionStatus.AUTO_SELECTED_CONSISTENT) {
            val targetGroup = comparison.automaticallyAssignedGroup?.groupNumber
            if (targetGroup != null && !updatedOverride && targetGroup != updatedEngine.activeGroupNumber) {
                updatedEngine = EdiPhase1SessionManager.requestGroupChange(
                    state = updatedEngine,
                    targetGroupNumber = targetGroup,
                    reason = "Actualización automática por edad EDI: corresponde al Grupo $targetGroup"
                )
            }
        } else if (
            comparison.autoSelectionStatus == EdiGroupAutoSelectionStatus.BLOCKED_GROUP_DISCREPANCY_REQUIRES_PROFESSIONAL_REVIEW &&
            validResolution == null
        ) {
            updatedOverride = false
        }

        return state.copy(
            ediEngineState = updatedEngine,
            ediAgeDiscrepancyResolution = validResolution,
            ediManualGroupOverride = updatedOverride
        )
    }

    private fun syncEdiTemplateQuestions(state: CredEdiUiState): CredEdiUiState {
        val template = state.ediTemplate
        val updatedSa = state.saItems.mapIndexed { index, item ->
            val newQuestion = template.saQuestions.getOrElse(index) { item.title }
            item.copy(title = newQuestion)
        }
        val updatedAreas = state.areaItems.mapIndexed { index, item ->
            val pair = template.areaQuestions.getOrNull(index)
            if (pair != null) {
                item.copy(title = pair.first, question = pair.second)
            } else {
                item
            }
        }
        return syncEdi2026StateForAgeChange(
            state.copy(
                saItems = updatedSa,
                areaItems = updatedAreas
            )
        )
    }

    fun selectTab(tab: AppTab) {
        _uiState.update {
            it.copy(
                currentTab = tab,
                showDatosEdiModal = false
            )
        }
    }

    fun selectSexo(sexo: SexoBiologico) {
        _uiState.update { it.copy(sexo = sexo) }
    }

    private fun normalizeDateInput(raw: String): String {
        val trimmed = raw.take(10)
        return if (trimmed.length == 8 && trimmed.all { it.isDigit() }) {
            "${trimmed.substring(0, 2)}/${trimmed.substring(2, 4)}/${trimmed.substring(4, 8)}"
        } else {
            trimmed
        }
    }

    fun updateFechaNacimiento(fecha: String) {
        val normalized = normalizeDateInput(fecha)
        _uiState.update { state ->
            syncEdiTemplateQuestions(state.copy(fechaNacimiento = normalized))
        }
    }

    fun updateFechaEvaluacion(fecha: String) {
        val normalized = normalizeDateInput(fecha)
        _uiState.update { state ->
            val parsed = ClinicalAgeCalculator.parseDate(normalized)
            val syncedEdiDate = if (parsed != null) {
                String.format(Locale.US, "%02d/%02d/%04d", parsed.day, parsed.month, parsed.year)
            } else {
                normalized
            }
            syncEdiTemplateQuestions(
                state.copy(
                    fechaEvaluacion = normalized,
                    ediFechaEval = syncedEdiDate
                )
            )
        }
    }

    fun togglePrematurez(enabled: Boolean) {
        _uiState.update { state ->
            val effectiveEg = if (enabled && state.egSemanas >= 37) 32 else state.egSemanas
            val updatedFrb = state.frbItems.map { item ->
                if (item.id == 1) {
                    if (enabled) {
                        item.copy(title = "Prematurez < 37 semanas ($effectiveEg sem EG)", checked = true)
                    } else {
                        item.copy(checked = false)
                    }
                } else {
                    item
                }
            }
            syncEdiTemplateQuestions(
                state.copy(
                    esPrematuro = enabled,
                    egSemanas = effectiveEg,
                    ediEgSemanasInput = effectiveEg.toString(),
                    frbItems = updatedFrb
                )
            )
        }
    }

    fun updateEgSemanas(semanas: Int) {
        val clamped = semanas.coerceIn(20, 44)
        val isPrematuro = clamped < 37
        _uiState.update { state ->
            val updatedFrb = state.frbItems.map { item ->
                if (item.id == 1) {
                    if (isPrematuro) {
                        item.copy(title = "Prematurez < 37 semanas ($clamped sem EG)", checked = true)
                    } else {
                        item.copy(title = "A término ($clamped sem EG)", checked = false)
                    }
                } else {
                    item
                }
            }
            syncEdiTemplateQuestions(
                state.copy(
                    egSemanas = clamped,
                    ediEgSemanasInput = clamped.toString(),
                    esPrematuro = isPrematuro,
                    frbItems = updatedFrb
                )
            )
        }
    }

    private fun sanitizeDecimal(raw: String): String {
        val normalized = raw.replace(',', '.')
        val sb = StringBuilder()
        var hasDot = false
        for (ch in normalized) {
            if (ch.isDigit()) {
                sb.append(ch)
            } else if (ch == '.' && !hasDot) {
                hasDot = true
                sb.append(ch)
            }
        }
        return sb.toString().take(6)
    }

    fun updatePesoInput(raw: String) {
        val cleaned = sanitizeDecimal(raw)
        val parsed = cleaned.toDoubleOrNull()
        _uiState.update { state ->
            if (parsed != null && parsed > 0.0) {
                state.copy(
                    pesoInput = cleaned,
                    pesoKg = parsed.coerceIn(0.50, 40.00)
                )
            } else {
                state.copy(pesoInput = cleaned)
            }
        }
    }

    fun adjustPeso(delta: Double) {
        _uiState.update { state ->
            val next = (((state.pesoKg + delta) * 100.0).roundToInt() / 100.0).coerceIn(1.50, 25.00)
            state.copy(
                pesoKg = next,
                pesoInput = next.formatDecimal(2)
            )
        }
    }

    fun setPesoKg(peso: Double) {
        _uiState.update { state ->
            val clamped = peso.coerceIn(0.50, 40.00)
            state.copy(
                pesoKg = clamped,
                pesoInput = clamped.formatDecimal(2)
            )
        }
    }

    fun updateLongitudInput(raw: String) {
        val cleaned = sanitizeDecimal(raw)
        val parsed = cleaned.toDoubleOrNull()
        _uiState.update { state ->
            if (parsed != null && parsed > 0.0) {
                state.copy(
                    longitudInput = cleaned,
                    longitudCm = parsed.coerceIn(30.0, 130.0)
                )
            } else {
                state.copy(longitudInput = cleaned)
            }
        }
    }

    fun adjustLongitud(delta: Double) {
        _uiState.update { state ->
            val next = (((state.longitudCm + delta) * 10.0).roundToInt() / 10.0).coerceIn(35.0, 110.0)
            state.copy(
                longitudCm = next,
                longitudInput = next.formatDecimal(1)
            )
        }
    }

    fun setLongitudCm(longitud: Double) {
        _uiState.update { state ->
            val clamped = longitud.coerceIn(30.0, 130.0)
            state.copy(
                longitudCm = clamped,
                longitudInput = clamped.formatDecimal(1)
            )
        }
    }

    fun updatePerimetroCefalicoInput(raw: String) {
        val cleaned = sanitizeDecimal(raw)
        val parsed = cleaned.toDoubleOrNull()
        _uiState.update { state ->
            if (parsed != null && parsed > 0.0) {
                state.copy(
                    perimetroCefalicoInput = cleaned,
                    perimetroCefalicoCm = parsed.coerceIn(20.0, 65.0)
                )
            } else {
                state.copy(perimetroCefalicoInput = cleaned)
            }
        }
    }

    fun adjustPerimetroCefalico(delta: Double) {
        _uiState.update { state ->
            val next = (((state.perimetroCefalicoCm + delta) * 10.0).roundToInt() / 10.0).coerceIn(25.0, 60.0)
            state.copy(
                perimetroCefalicoCm = next,
                perimetroCefalicoInput = next.formatDecimal(1)
            )
        }
    }

    fun setPerimetroCefalicoCm(pc: Double) {
        _uiState.update { state ->
            val clamped = pc.coerceIn(20.0, 65.0)
            state.copy(
                perimetroCefalicoCm = clamped,
                perimetroCefalicoInput = clamped.formatDecimal(1)
            )
        }
    }

    fun selectCurvaIndicador(indicador: CurvaIndicador) {
        _uiState.update { it.copy(selectedIndicador = indicador) }
    }

    fun openDatosEdiModal() {
        _uiState.update { it.copy(showDatosEdiModal = true) }
    }

    fun closeDatosEdiModal() {
        _uiState.update { it.copy(showDatosEdiModal = false) }
    }

    fun adjustEdiFechaEvalYear(deltaYears: Int) {
        _uiState.update { state ->
            val base = if (state.ediFechaEval.isNotBlank()) state.ediFechaEval else state.fechaEvaluacion
            val newDateStr = ClinicalAgeCalculator.adjustYear(base, deltaYears)
            syncEdiTemplateQuestions(
                state.copy(
                    ediFechaEval = newDateStr,
                    fechaEvaluacion = newDateStr
                )
            )
        }
    }

    fun updateEdiFechaEval(value: String) {
        _uiState.update { state ->
            val parsed = ClinicalAgeCalculator.parseDate(value)
            val updatedState = if (parsed != null) {
                val formatted = String.format(Locale.US, "%02d/%02d/%04d", parsed.day, parsed.month, parsed.year)
                state.copy(
                    ediFechaEval = value,
                    fechaEvaluacion = formatted
                )
            } else {
                state.copy(
                    ediFechaEval = value
                )
            }
            syncEdiTemplateQuestions(updatedState)
        }
    }

    fun updateEdiEgSemanasInput(value: String) {
        val digits = value.filter { it.isDigit() }.take(2)
        val parsed = digits.toIntOrNull()
        _uiState.update { state ->
            if (parsed != null && parsed in 20..44) {
                val isPrematuro = parsed < 37
                val updatedFrb = state.frbItems.map { item ->
                    if (item.id == 1) {
                        if (isPrematuro) {
                            item.copy(title = "Prematurez < 37 semanas ($parsed sem EG)", checked = true)
                        } else {
                            item.copy(title = "A término ($parsed sem EG)", checked = false)
                        }
                    } else {
                        item
                    }
                }
                syncEdiTemplateQuestions(
                    state.copy(
                        ediEgSemanasInput = digits,
                        egSemanas = parsed,
                        esPrematuro = isPrematuro,
                        frbItems = updatedFrb
                    )
                )
            } else {
                state.copy(
                    ediEgSemanasInput = digits
                )
            }
        }
    }

    fun adjustEdiEgSemanas(delta: Int) {
        _uiState.update { state ->
            val next = (state.egSemanas + delta).coerceIn(20, 44)
            val isPrematuro = next < 37
            val updatedFrb = state.frbItems.map { item ->
                if (item.id == 1) {
                    if (isPrematuro) {
                        item.copy(title = "Prematurez < 37 semanas ($next sem EG)", checked = true)
                    } else {
                        item.copy(title = "A término ($next sem EG)", checked = false)
                    }
                } else {
                    item
                }
            }
            syncEdiTemplateQuestions(
                state.copy(
                    egSemanas = next,
                    ediEgSemanasInput = next.toString(),
                    esPrematuro = isPrematuro,
                    frbItems = updatedFrb
                )
            )
        }
    }

    fun toggleFrbItem(id: Int) {
        _uiState.update { state ->
            val updated = state.frbItems.map { item ->
                if (item.id == id) item.copy(checked = !item.checked) else item
            }
            state.copy(
                frbItems = updated
            )
        }
    }

    fun setSaItemPresent(id: Int, isPresent: Boolean) {
        _uiState.update { state ->
            val updated = state.saItems.map { item ->
                if (item.id == id) item.copy(isPresent = isPresent) else item
            }
            state.copy(
                saItems = updated
            )
        }
    }

    fun setAreaItemAchieved(id: Int, isAchieved: Boolean) {
        _uiState.update { state ->
            val updated = state.areaItems.map { item ->
                if (item.id == id) item.copy(isAchieved = isAchieved) else item
            }
            state.copy(
                areaItems = updated
            )
        }
    }

    fun toggleEnTono() {
        _uiState.update { state ->
            state.copy(
                enTonoSimetrico = !state.enTonoSimetrico
            )
        }
    }

    fun toggleEnMoro() {
        _uiState.update { state ->
            state.copy(
                enMoroPresente = !state.enMoroPresente
            )
        }
    }

    fun toggleEnSuccion() {
        _uiState.update { state ->
            state.copy(
                enSuccionRitmica = !state.enSuccionRitmica
            )
        }
    }

    fun toggleEnPrension() {
        _uiState.update { state ->
            state.copy(
                enPrensionPresente = !state.enPrensionPresente
            )
        }
    }

    fun toggleEnMovilidadOcular() {
        _uiState.update { state ->
            state.copy(
                enMovilidadOcularNormal = !state.enMovilidadOcularNormal
            )
        }
    }

    fun guardarYCalcularSemaforoEdi() {
        _uiState.update { state ->
            state.copy(
                showDatosEdiModal = false
            )
        }
    }

    // --- Controladores Fase 4A: Motor Clínico EDI Peruano 2026 ---

    fun navigateEdiView(view: EdiInternalView) {
        _uiState.update { it.copy(ediInternalView = view) }
    }

    fun navigateEdiBack(): Boolean {
        val current = _uiState.value.ediInternalView
        if (current == EdiInternalView.SUMMARY_5_AXES) return false
        _uiState.update { it.copy(ediInternalView = EdiInternalView.SUMMARY_5_AXES) }
        return true
    }

    fun selectEdiAxis(axis: EdiAxis) {
        _uiState.update { it.copy(ediSelectedAxis = axis) }
    }

    fun openEdiAxisQuestionnaire(axis: EdiAxis) {
        _uiState.update {
            it.copy(
                ediSelectedAxis = axis,
                ediInternalView = EdiInternalView.QUESTIONNAIRE_AND_FALLBACK
            )
        }
    }

    fun selectEdiDevAreaFilter(area: EdiDevArea?) {
        _uiState.update { it.copy(ediSelectedDevAreaFilter = area) }
    }

    fun updateEdiGroupSearchQuery(query: String) {
        _uiState.update { it.copy(ediGroupSearchQuery = query) }
    }

    fun selectEdiGroupFilter(filter: EdiGroupFilterCategory) {
        _uiState.update { it.copy(ediGroupFilterCategory = filter) }
    }

    fun updateEgDias(dias: Int) {
        val clamped = dias.coerceIn(0, 6)
        _uiState.update { state ->
            syncEdi2026StateForAgeChange(state.copy(egDias = clamped))
        }
    }

    fun adjustEgDias(delta: Int) {
        _uiState.update { state ->
            val next = (state.egDias + delta).coerceIn(0, 6)
            syncEdi2026StateForAgeChange(state.copy(egDias = next))
        }
    }

    fun selectEdiGroupFromBank(groupNumber: Int) {
        val safeGroup = groupNumber.coerceIn(1, 15)
        _uiState.update { state ->
            val autoGroup = state.ediCorrectedAgeComparison.automaticallyAssignedGroup?.groupNumber
            val updatedEngine = EdiPhase1SessionManager.requestGroupChange(
                state = state.ediEngineState,
                targetGroupNumber = safeGroup,
                reason = "Selección manual desde el Banco de los 15 Grupos EDI"
            )
            val switchedImmediately = updatedEngine.pendingGroupChangeConfirmation == null
            val isManualOverride = if (switchedImmediately && autoGroup != null) {
                safeGroup != autoGroup
            } else {
                state.ediManualGroupOverride
            }
            state.copy(
                ediEngineState = updatedEngine,
                ediManualGroupOverride = isManualOverride
            )
        }
    }

    fun useAgeSuggestedEdiGroup() {
        _uiState.update { state ->
            val autoGroup = state.ediCorrectedAgeComparison.automaticallyAssignedGroup?.groupNumber
                ?: return@update state
            val updatedEngine = EdiPhase1SessionManager.requestGroupChange(
                state = state.ediEngineState,
                targetGroupNumber = autoGroup,
                reason = "Restaurar grupo asignado por edad ($autoGroup)"
            )
            val switchedImmediately = updatedEngine.pendingGroupChangeConfirmation == null
            state.copy(
                ediEngineState = updatedEngine,
                ediManualGroupOverride = if (switchedImmediately) false else state.ediManualGroupOverride
            )
        }
    }

    fun resolveEdiPendingGroupChange(action: EdiGroupChangeResolutionAction) {
        _uiState.update { state ->
            val resolvedEngine = EdiPhase1SessionManager.confirmPendingGroupChange(
                state = state.ediEngineState,
                action = action
            )
            val autoGroup = state.ediCorrectedAgeComparison.automaticallyAssignedGroup?.groupNumber
            val isOverride = if (autoGroup != null) {
                resolvedEngine.activeGroupNumber != autoGroup
            } else {
                false
            }
            state.copy(
                ediEngineState = resolvedEngine,
                ediManualGroupOverride = isOverride
            )
        }
    }

    fun confirmEdiAgeDiscrepancyResolution(
        selectedGroupNumber: Int,
        justificationNote: String
    ) {
        val trimmedNote = justificationNote.trim()
        if (trimmedNote.isEmpty()) return
        _uiState.update { state ->
            val comparison = state.ediCorrectedAgeComparison
            val validCandidate = comparison.candidateGroupsForProfessionalReview
                .any { it.groupNumber == selectedGroupNumber }
            if (!validCandidate) return@update state

            val resolution = EdiAgeDiscrepancyResolution(
                fechaNacimiento = state.fechaNacimiento,
                fechaEvaluacion = state.fechaEvaluacion,
                esPrematuro = state.esPrematuro,
                egSemanas = state.egSemanas,
                egDias = state.egDias,
                selectedGroupNumber = selectedGroupNumber,
                clinicalJustificationNote = trimmedNote
            )
            val updatedEngine = EdiPhase1SessionManager.requestGroupChange(
                state = state.ediEngineState,
                targetGroupNumber = selectedGroupNumber,
                reason = "Resolución profesional de discrepancia etaria: $trimmedNote"
            )
            state.copy(
                ediAgeDiscrepancyResolution = resolution,
                ediEngineState = updatedEngine,
                ediManualGroupOverride = false
            )
        }
    }

    fun recordEdiCurrentItemResponse(
        itemUniqueId: String,
        response: EdiItemResponse
    ) {
        _uiState.update { state ->
            val cleanedDual = if (response != EdiItemResponse.DISCREPANCY_REQUIRES_HUMAN_RESOLUTION) {
                state.ediEngineState.copy(
                    activeSession = state.ediEngineState.activeSession.copy(
                        dualEvidenceRecords = state.ediEngineState.activeSession.dualEvidenceRecords - itemUniqueId
                    )
                )
            } else {
                state.ediEngineState
            }
            val updated = EdiPhase1SessionManager.recordCurrentGroupItemResponse(
                state = cleanedDual,
                itemUniqueId = itemUniqueId,
                response = response
            )
            state.copy(ediEngineState = updated)
        }
    }

    fun recordEdiDualEvidence(
        itemUniqueId: String,
        observationEvidence: Boolean?,
        interviewEvidence: Boolean?,
        humanResolutionIfConflict: Boolean? = null,
        note: String = ""
    ) {
        _uiState.update { state ->
            val updated = EdiPhase1SessionManager.recordDualModalityEvidence(
                state = state.ediEngineState,
                itemUniqueId = itemUniqueId,
                observationEvidence = observationEvidence,
                interviewEvidence = interviewEvidence,
                humanResolutionIfConflict = humanResolutionIfConflict,
                note = note
            )
            state.copy(ediEngineState = updated)
        }
    }

    fun recordEdiFallbackResponse(
        fallbackUniqueId: String,
        response: EdiItemResponse
    ) {
        _uiState.update { state ->
            val cleanedDual = if (response != EdiItemResponse.DISCREPANCY_REQUIRES_HUMAN_RESOLUTION) {
                state.ediEngineState.copy(
                    activeSession = state.ediEngineState.activeSession.copy(
                        dualEvidenceRecords = state.ediEngineState.activeSession.dualEvidenceRecords - fallbackUniqueId
                    )
                )
            } else {
                state.ediEngineState
            }
            val updated = EdiAreaFallbackEngine.recordValidatedFallbackResponse(
                state = cleanedDual,
                fallbackUniqueId = fallbackUniqueId,
                response = response
            )
            state.copy(ediEngineState = updated)
        }
    }

    fun recordEdiFallbackDualEvidence(
        fallbackUniqueId: String,
        observationEvidence: Boolean?,
        interviewEvidence: Boolean?,
        humanResolutionIfConflict: Boolean? = null,
        note: String = ""
    ) {
        _uiState.update { state ->
            val updated = EdiAreaFallbackEngine.recordDualModalityFallbackEvidence(
                state = state.ediEngineState,
                fallbackUniqueId = fallbackUniqueId,
                observationEvidence = observationEvidence,
                interviewEvidence = interviewEvidence,
                humanResolutionIfConflict = humanResolutionIfConflict,
                note = note
            )
            state.copy(ediEngineState = updated)
        }
    }

    fun updateEdiItemClinicalNote(
        itemUniqueId: String,
        note: String
    ) {
        _uiState.update { state ->
            val session = state.ediEngineState.activeSession
            val updatedNotes = if (note.isBlank()) {
                session.itemClinicalNotes - itemUniqueId
            } else {
                session.itemClinicalNotes + (itemUniqueId to note)
            }
            state.copy(
                ediEngineState = state.ediEngineState.copy(
                    activeSession = session.copy(itemClinicalNotes = updatedNotes)
                )
            )
        }
    }
}

fun Double.formatDecimal(decimals: Int): String =
    String.format(Locale.US, "%.${decimals}f", this)

fun Double.formatSignedDecimal(decimals: Int): String {
    val formatted = String.format(Locale.US, "%.${decimals}f", this)
    return if (this > 0 && !formatted.startsWith("+")) "+$formatted" else formatted
}
