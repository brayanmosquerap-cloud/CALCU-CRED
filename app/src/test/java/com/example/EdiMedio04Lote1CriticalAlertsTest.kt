package com.example

import com.example.model.EdiAreaColorResult
import com.example.model.EdiAreaFallbackEngine
import com.example.model.EdiArchivedClinicalContext
import com.example.model.EdiArchivedOriginValidationState
import com.example.model.EdiBirthMaturityCondition
import com.example.model.EdiDevArea
import com.example.model.EdiGlobalEvaluationResult
import com.example.model.EdiGlobalSemaphoreEngine
import com.example.model.EdiGlobalTrafficLight
import com.example.model.EdiGroupChangeResolutionAction
import com.example.model.EdiItemResponse
import com.example.model.EdiOfficialCatalog
import com.example.model.EdiPriorityCriticalAlertType
import com.example.viewmodel.CredEdiViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * MEDIO-04 — LOTE 1:
 * Verificación exhaustiva de la propagación de
 * [EdiGlobalEvaluationResult.requiresImmediateClinicalAttentionDespiteIncomplete]
 * en las tres ramas de degradación a INCOMPLETE de [com.example.model.CredEdiUiState.ediGlobalEvaluation].
 */
class EdiMedio04Lote1CriticalAlertsTest {

    private fun fillAllMandatoryForGroupAsNormal(
        vm: CredEdiViewModel,
        groupNumber: Int,
        includeOptionalFrbAndAleInG5To15: Boolean = false
    ) {
        val spec = EdiOfficialCatalog.getGroup(groupNumber)
        if (groupNumber in 1..4 || includeOptionalFrbAndAleInG5To15) {
            EdiOfficialCatalog.frbItems.forEach { item ->
                vm.recordEdiCurrentItemResponse(item.uniqueId, EdiItemResponse.NO)
            }
            spec.aleItems.forEach { item ->
                if (item.uniqueId == "G12_ALE_12_3") {
                    vm.recordEdiDualEvidence(item.uniqueId, false, false, null, "Sin alerta")
                } else {
                    vm.recordEdiCurrentItemResponse(item.uniqueId, EdiItemResponse.NO)
                }
            }
        }
        spec.areaItems.values.flatten().forEach { item ->
            if (item.uniqueId == "G13_LE_13_3" || item.uniqueId == "G14_LE_14_3") {
                vm.recordEdiDualEvidence(item.uniqueId, true, true, null, "Logrado")
            } else {
                vm.recordEdiCurrentItemResponse(item.uniqueId, EdiItemResponse.YES)
            }
        }
        EdiOfficialCatalog.getApplicableEnItems(groupNumber).forEach { item ->
            vm.recordEdiCurrentItemResponse(item.uniqueId, EdiItemResponse.NO)
        }
        spec.alaItems.forEach { item ->
            vm.recordEdiCurrentItemResponse(item.uniqueId, EdiItemResponse.NO)
        }
    }

    private fun forceAreaRedWithCompletedFallback(
        vm: CredEdiViewModel,
        groupNumber: Int,
        area: EdiDevArea
    ) {
        val spec = EdiOfficialCatalog.getGroup(groupNumber)
        spec.areaItems.getValue(area).forEach { item ->
            vm.recordEdiCurrentItemResponse(item.uniqueId, EdiItemResponse.NO)
        }
        val fallbackItems = EdiAreaFallbackEngine.getFallbackItemsForArea(groupNumber, area)
        fallbackItems.forEach { fb ->
            vm.recordEdiFallbackResponse(fb.fallbackUniqueId, EdiItemResponse.NO)
        }
    }

    private fun assertDegradedCriticalIncomplete(
        eval: EdiGlobalEvaluationResult,
        expectedAlertType: EdiPriorityCriticalAlertType
    ) {
        assertEquals(EdiGlobalTrafficLight.INCOMPLETE, eval.trafficLight)
        assertFalse(eval.isDefinitive)
        assertTrue(eval.blockingReasons.isNotEmpty())
        assertTrue(eval.hasPriorityCriticalWarning)
        assertTrue(eval.priorityCriticalAlerts.isNotEmpty())
        assertTrue(eval.priorityCriticalAlerts.any { it.type == expectedAlertType })
        assertTrue(
            "requiresImmediateClinicalAttentionDespiteIncomplete debe ser true al degradarse a INCOMPLETE con alerta crítica",
            eval.requiresImmediateClinicalAttentionDespiteIncomplete
        )
        assertTrue(
            eval.clinicalSummaryText.contains("ADVERTENCIA CLÍNICA PRIORITARIA", ignoreCase = true) ||
                eval.clinicalSummaryText.contains("atención inmediata", ignoreCase = true)
        )
    }

    private fun assertDegradedNonCriticalIncomplete(eval: EdiGlobalEvaluationResult) {
        assertEquals(EdiGlobalTrafficLight.INCOMPLETE, eval.trafficLight)
        assertFalse(eval.isDefinitive)
        assertTrue(eval.blockingReasons.isNotEmpty())
        assertFalse(eval.hasPriorityCriticalWarning)
        assertTrue(eval.priorityCriticalAlerts.isEmpty())
        assertFalse(eval.requiresImmediateClinicalAttentionDespiteIncomplete)
    }

    // =========================================================================
    // RAMA A: Edad cronológica no verificable (!isChronologicalAgeVerifiable)
    // =========================================================================

    @Test
    fun `rama A - caso A B C D y E en edad cronologica no verificable`() {
        // Caso A: baseResult definitivo ROJO con ALA positiva -> INCOMPLETE con flag inmediato en true
        run {
            val vm = CredEdiViewModel()
            vm.selectEdiGroupFromBank(4)
            fillAllMandatoryForGroupAsNormal(vm, 4)
            vm.recordEdiCurrentItemResponse("G4_ALA_4_1", EdiItemResponse.YES)

            val rawBase = EdiGlobalSemaphoreEngine.evaluateGlobalSemaphore(vm.uiState.value.ediEngineState, null)
            assertEquals(EdiGlobalTrafficLight.ROJO, rawBase.trafficLight)
            assertTrue(rawBase.isDefinitive)
            assertFalse(rawBase.requiresImmediateClinicalAttentionDespiteIncomplete)

            val eval = vm.uiState.value.ediGlobalEvaluation
            assertFalse(vm.uiState.value.isChronologicalAgeVerifiable)
            assertTrue(eval.blockedByOutOfRangeAge)
            assertDegradedCriticalIncomplete(eval, EdiPriorityCriticalAlertType.POSITIVE_ALARM_SIGNAL_ALA)
        }

        // Caso B: baseResult definitivo ROJO con EN positiva -> INCOMPLETE con flag inmediato en true
        run {
            val vm = CredEdiViewModel()
            vm.selectEdiGroupFromBank(4)
            fillAllMandatoryForGroupAsNormal(vm, 4)
            vm.recordEdiCurrentItemResponse("EN_1_1", EdiItemResponse.YES)

            val rawBase = EdiGlobalSemaphoreEngine.evaluateGlobalSemaphore(vm.uiState.value.ediEngineState, null)
            assertEquals(EdiGlobalTrafficLight.ROJO, rawBase.trafficLight)
            assertTrue(rawBase.isDefinitive)
            assertFalse(rawBase.requiresImmediateClinicalAttentionDespiteIncomplete)

            val eval = vm.uiState.value.ediGlobalEvaluation
            assertTrue(eval.blockedByOutOfRangeAge)
            assertDegradedCriticalIncomplete(eval, EdiPriorityCriticalAlertType.ALTERED_NEUROLOGICAL_EXAM_EN)
        }

        // Caso C: baseResult definitivo ROJO con Área de desarrollo ROJA -> INCOMPLETE con flag inmediato en true
        run {
            val vm = CredEdiViewModel()
            vm.selectEdiGroupFromBank(4)
            fillAllMandatoryForGroupAsNormal(vm, 4)
            forceAreaRedWithCompletedFallback(vm, 4, EdiDevArea.MG)

            val rawBase = EdiGlobalSemaphoreEngine.evaluateGlobalSemaphore(vm.uiState.value.ediEngineState, null)
            assertEquals(EdiGlobalTrafficLight.ROJO, rawBase.trafficLight)
            assertTrue(rawBase.isDefinitive)
            assertFalse(rawBase.requiresImmediateClinicalAttentionDespiteIncomplete)

            val eval = vm.uiState.value.ediGlobalEvaluation
            assertTrue(eval.blockedByOutOfRangeAge)
            assertDegradedCriticalIncomplete(eval, EdiPriorityCriticalAlertType.RED_DEVELOPMENT_AREA)
        }

        // Caso D: baseResult sin alertas prioritarias (VERDE) -> degradado a INCOMPLETE con flag en false
        run {
            val vm = CredEdiViewModel()
            vm.selectEdiGroupFromBank(4)
            fillAllMandatoryForGroupAsNormal(vm, 4)

            val rawBase = EdiGlobalSemaphoreEngine.evaluateGlobalSemaphore(vm.uiState.value.ediEngineState, null)
            assertEquals(EdiGlobalTrafficLight.VERDE, rawBase.trafficLight)
            assertFalse(rawBase.hasPriorityCriticalWarning)

            val eval = vm.uiState.value.ediGlobalEvaluation
            assertTrue(eval.blockedByOutOfRangeAge)
            assertDegradedNonCriticalIncomplete(eval)
        }

        // Caso E: baseResult ya INCOMPLETE con alerta crítica -> conserva flag en true
        run {
            val vm = CredEdiViewModel()
            vm.selectEdiGroupFromBank(4)
            fillAllMandatoryForGroupAsNormal(vm, 4)
            vm.recordEdiCurrentItemResponse("G4_MF_4_1", EdiItemResponse.UNANSWERED)
            vm.recordEdiCurrentItemResponse("G4_ALA_4_1", EdiItemResponse.YES)

            val rawBase = EdiGlobalSemaphoreEngine.evaluateGlobalSemaphore(vm.uiState.value.ediEngineState, null)
            assertEquals(EdiGlobalTrafficLight.INCOMPLETE, rawBase.trafficLight)
            assertTrue(rawBase.requiresImmediateClinicalAttentionDespiteIncomplete)

            val eval = vm.uiState.value.ediGlobalEvaluation
            assertTrue(eval.blockedByOutOfRangeAge)
            assertDegradedCriticalIncomplete(eval, EdiPriorityCriticalAlertType.POSITIVE_ALARM_SIGNAL_ALA)
        }
    }

    // =========================================================================
    // RAMA B: Contradicción gestacional, edad corregida pendiente o
    //         confirmación de madurez al nacer pendiente (< 24m)
    // =========================================================================

    @Test
    fun `rama B - caso A B C D y E en condicion al nacer pendiente menor de 24m`() {
        // Fecha cronológica: 2m 5d -> Grupo 2 (donde aplican Áreas, EN, FRB, ALE y ALA)
        fun setupUnder24mPending(): CredEdiViewModel = CredEdiViewModel().apply {
            updateFechaNacimiento("05/01/2025")
            updateFechaEvaluacion("10/03/2025")
            selectEdiGroupFromBank(2)
        }

        // Caso A: ROJO definitivo por ALA positiva degradado por confirmación al nacer pendiente
        run {
            val vm = setupUnder24mPending()
            fillAllMandatoryForGroupAsNormal(vm, 2)
            vm.recordEdiCurrentItemResponse("G2_ALA_2_1", EdiItemResponse.YES)

            val state = vm.uiState.value
            assertTrue(state.isBirthMaturityConfirmationPendingInUnder24m)
            val rawBase = EdiGlobalSemaphoreEngine.evaluateGlobalSemaphore(state.ediEngineState, state.ediCorrectedAgeComparison)
            assertEquals(EdiGlobalTrafficLight.ROJO, rawBase.trafficLight)
            assertTrue(rawBase.isDefinitive)
            assertFalse(rawBase.requiresImmediateClinicalAttentionDespiteIncomplete)

            val eval = state.ediGlobalEvaluation
            assertTrue(eval.blockedByAgeDiscrepancy)
            assertDegradedCriticalIncomplete(eval, EdiPriorityCriticalAlertType.POSITIVE_ALARM_SIGNAL_ALA)
        }

        // Caso B: ROJO definitivo por EN positiva degradado por confirmación al nacer pendiente
        run {
            val vm = setupUnder24mPending()
            fillAllMandatoryForGroupAsNormal(vm, 2)
            vm.recordEdiCurrentItemResponse("EN_1_2", EdiItemResponse.YES)

            val state = vm.uiState.value
            val rawBase = EdiGlobalSemaphoreEngine.evaluateGlobalSemaphore(state.ediEngineState, state.ediCorrectedAgeComparison)
            assertEquals(EdiGlobalTrafficLight.ROJO, rawBase.trafficLight)
            assertTrue(rawBase.isDefinitive)

            val eval = state.ediGlobalEvaluation
            assertTrue(eval.blockedByAgeDiscrepancy)
            assertDegradedCriticalIncomplete(eval, EdiPriorityCriticalAlertType.ALTERED_NEUROLOGICAL_EXAM_EN)
        }

        // Caso C: ROJO definitivo por Área de desarrollo ROJA degradado por confirmación al nacer pendiente
        run {
            val vm = setupUnder24mPending()
            fillAllMandatoryForGroupAsNormal(vm, 2)
            forceAreaRedWithCompletedFallback(vm, 2, EdiDevArea.MG)

            val state = vm.uiState.value
            val rawBase = EdiGlobalSemaphoreEngine.evaluateGlobalSemaphore(state.ediEngineState, state.ediCorrectedAgeComparison)
            assertEquals(EdiGlobalTrafficLight.ROJO, rawBase.trafficLight)
            assertTrue(rawBase.isDefinitive)

            val eval = state.ediGlobalEvaluation
            assertTrue(eval.blockedByAgeDiscrepancy)
            assertDegradedCriticalIncomplete(eval, EdiPriorityCriticalAlertType.RED_DEVELOPMENT_AREA)
        }

        // Caso D: VERDE sin alertas prioritarias -> degradado a INCOMPLETE con flag en false
        run {
            val vm = setupUnder24mPending()
            fillAllMandatoryForGroupAsNormal(vm, 2)

            val eval = vm.uiState.value.ediGlobalEvaluation
            assertTrue(eval.blockedByAgeDiscrepancy)
            assertDegradedNonCriticalIncomplete(eval)
        }

        // Caso E: Ya INCOMPLETE con alerta crítica -> conserva flag en true
        run {
            val vm = setupUnder24mPending()
            fillAllMandatoryForGroupAsNormal(vm, 2)
            vm.recordEdiCurrentItemResponse("G2_LE_2_1", EdiItemResponse.UNANSWERED)
            vm.recordEdiCurrentItemResponse("G2_ALA_2_1", EdiItemResponse.YES)

            val state = vm.uiState.value
            val rawBase = EdiGlobalSemaphoreEngine.evaluateGlobalSemaphore(state.ediEngineState, state.ediCorrectedAgeComparison)
            assertEquals(EdiGlobalTrafficLight.INCOMPLETE, rawBase.trafficLight)
            assertTrue(rawBase.requiresImmediateClinicalAttentionDespiteIncomplete)

            val eval = state.ediGlobalEvaluation
            assertDegradedCriticalIncomplete(eval, EdiPriorityCriticalAlertType.POSITIVE_ALARM_SIGNAL_ALA)
        }
    }

    @Test
    fun `rama B - edad corregida pendiente y contradiccion gestacional propagan alerta critica inmediata`() {
        // B2: isCorrectedAgePending (prematuro < 24m sin EG exacta) con Caso A, B, C y D
        run {
            val vm = CredEdiViewModel().apply {
                updateFechaNacimiento("05/01/2025")
                updateFechaEvaluacion("10/03/2025")
                selectEdiGroupFromBank(2)
                togglePrematurez(true)
            }
            assertTrue(vm.uiState.value.isCorrectedAgePending)
            fillAllMandatoryForGroupAsNormal(vm, 2)

            // Caso D
            assertDegradedNonCriticalIncomplete(vm.uiState.value.ediGlobalEvaluation)

            // Caso A (ALA)
            vm.recordEdiCurrentItemResponse("G2_ALA_2_1", EdiItemResponse.YES)
            assertDegradedCriticalIncomplete(
                vm.uiState.value.ediGlobalEvaluation,
                EdiPriorityCriticalAlertType.POSITIVE_ALARM_SIGNAL_ALA
            )

            // Caso B (EN)
            vm.recordEdiCurrentItemResponse("G2_ALA_2_1", EdiItemResponse.NO)
            vm.recordEdiCurrentItemResponse("EN_1_1", EdiItemResponse.YES)
            assertDegradedCriticalIncomplete(
                vm.uiState.value.ediGlobalEvaluation,
                EdiPriorityCriticalAlertType.ALTERED_NEUROLOGICAL_EXAM_EN
            )

            // Caso C (Área ROJA)
            vm.recordEdiCurrentItemResponse("EN_1_1", EdiItemResponse.NO)
            forceAreaRedWithCompletedFallback(vm, 2, EdiDevArea.MF)
            assertDegradedCriticalIncomplete(
                vm.uiState.value.ediGlobalEvaluation,
                EdiPriorityCriticalAlertType.RED_DEVELOPMENT_AREA
            )
        }

        // B3: ediHasGestationalContradiction (EG 39s con switch prematuridad activo) con Caso A y D
        run {
            val vm = CredEdiViewModel().apply {
                updateFechaNacimiento("05/01/2025")
                updateFechaEvaluacion("10/03/2025")
                selectEdiGroupFromBank(2)
                updateEgSemanas(39)
                togglePrematurez(true)
            }
            assertTrue(vm.uiState.value.ediHasGestationalContradiction)
            fillAllMandatoryForGroupAsNormal(vm, 2)

            // Caso D
            assertDegradedNonCriticalIncomplete(vm.uiState.value.ediGlobalEvaluation)

            // Caso A
            vm.recordEdiCurrentItemResponse("G2_ALA_2_1", EdiItemResponse.YES)
            assertDegradedCriticalIncomplete(
                vm.uiState.value.ediGlobalEvaluation,
                EdiPriorityCriticalAlertType.POSITIVE_ALARM_SIGNAL_ALA
            )
        }
    }

    // =========================================================================
    // RAMA C: Sesión restaurada incompatible, origen no verificable o
    //         discrepancia de grupo/edad
    // =========================================================================

    private fun restoreUnverifiedG10IntoVerifiedG10Context(
        configureAnswers: (CredEdiViewModel) -> Unit
    ): CredEdiViewModel {
        val vm = CredEdiViewModel()
        // 1. Captura en consulta manual sin fecha de nacimiento -> origen NO VERIFICADO
        vm.selectEdiGroupFromBank(10)
        configureAnswers(vm)

        // 2. Archivar G10 cambiando a G11
        vm.selectEdiGroupFromBank(11)
        vm.resolveEdiPendingGroupChange(
            EdiGroupChangeResolutionAction.ARCHIVE_PREVIOUS_AND_SWITCH_TO_CLEAN_TARGET
        )

        // 3. Registrar fechas verificables que corresponden al Grupo 10 (22m 10d, a término)
        vm.updateFechaEvaluacion("20/05/2025")
        vm.updateFechaNacimiento("10/07/2023")
        vm.confirmEdiTermWithoutWeeks()
        assertEquals(10, vm.uiState.value.ediOfficialApplicableGroupNumber)

        // 4. Restaurar ARCH_001_G10 en el mismo episodio: grupo activo (10) == grupo oficial (10),
        //    por lo que baseResult es definitivo, pero Rama C degrada por isRestoredSessionOriginUnverified.
        vm.requestRestoreEdiArchivedSession("ARCH_001_G10")
        vm.confirmRestoreEdiArchivedSession(confirmSamePatientContinuity = true)
        assertTrue(vm.uiState.value.isRestoredSessionOriginUnverified)
        assertFalse(vm.uiState.value.ediHasGroupAgeMismatch)
        return vm
    }

    @Test
    fun `rama C - caso A B C D y E en sesion restaurada con origen no verificado`() {
        // Caso A: baseResult definitivo ROJO con ALA positiva -> INCOMPLETE con flag inmediato en true
        run {
            val vm = restoreUnverifiedG10IntoVerifiedG10Context {
                fillAllMandatoryForGroupAsNormal(it, 10)
                it.recordEdiCurrentItemResponse("G10_ALA_10_1", EdiItemResponse.YES)
            }
            val state = vm.uiState.value
            val rawBase = EdiGlobalSemaphoreEngine.evaluateGlobalSemaphore(state.ediEngineState, state.ediCorrectedAgeComparison)
            assertEquals(EdiGlobalTrafficLight.ROJO, rawBase.trafficLight)
            assertTrue(rawBase.isDefinitive)
            assertFalse(rawBase.requiresImmediateClinicalAttentionDespiteIncomplete)

            val eval = state.ediGlobalEvaluation
            assertTrue(eval.blockedByAgeDiscrepancy)
            assertDegradedCriticalIncomplete(eval, EdiPriorityCriticalAlertType.POSITIVE_ALARM_SIGNAL_ALA)
        }

        // Caso B: baseResult definitivo ROJO con EN positiva -> INCOMPLETE con flag inmediato en true
        run {
            val vm = restoreUnverifiedG10IntoVerifiedG10Context {
                fillAllMandatoryForGroupAsNormal(it, 10)
                it.recordEdiCurrentItemResponse("EN_1_3", EdiItemResponse.YES)
            }
            val state = vm.uiState.value
            val rawBase = EdiGlobalSemaphoreEngine.evaluateGlobalSemaphore(state.ediEngineState, state.ediCorrectedAgeComparison)
            assertEquals(EdiGlobalTrafficLight.ROJO, rawBase.trafficLight)
            assertTrue(rawBase.isDefinitive)

            val eval = state.ediGlobalEvaluation
            assertTrue(eval.blockedByAgeDiscrepancy)
            assertDegradedCriticalIncomplete(eval, EdiPriorityCriticalAlertType.ALTERED_NEUROLOGICAL_EXAM_EN)
        }

        // Caso C: baseResult definitivo ROJO con Área ROJA -> INCOMPLETE con flag inmediato en true
        run {
            val vm = restoreUnverifiedG10IntoVerifiedG10Context {
                fillAllMandatoryForGroupAsNormal(it, 10)
                forceAreaRedWithCompletedFallback(it, 10, EdiDevArea.MG)
            }
            val state = vm.uiState.value
            val rawBase = EdiGlobalSemaphoreEngine.evaluateGlobalSemaphore(state.ediEngineState, state.ediCorrectedAgeComparison)
            assertEquals(EdiGlobalTrafficLight.ROJO, rawBase.trafficLight)
            assertTrue(rawBase.isDefinitive)

            val eval = state.ediGlobalEvaluation
            assertTrue(eval.blockedByAgeDiscrepancy)
            assertDegradedCriticalIncomplete(eval, EdiPriorityCriticalAlertType.RED_DEVELOPMENT_AREA)
        }

        // Caso D: baseResult VERDE sin alertas prioritarias -> INCOMPLETE con flag en false
        run {
            val vm = restoreUnverifiedG10IntoVerifiedG10Context {
                fillAllMandatoryForGroupAsNormal(it, 10)
            }
            val eval = vm.uiState.value.ediGlobalEvaluation
            assertTrue(eval.blockedByAgeDiscrepancy)
            assertDegradedNonCriticalIncomplete(eval)
        }

        // Caso E: baseResult ya INCOMPLETE con alerta crítica -> conserva flag en true
        run {
            val vm = restoreUnverifiedG10IntoVerifiedG10Context {
                fillAllMandatoryForGroupAsNormal(it, 10)
                it.recordEdiCurrentItemResponse("G10_MF_10_1", EdiItemResponse.UNANSWERED)
                it.recordEdiCurrentItemResponse("G10_ALA_10_1", EdiItemResponse.YES)
            }
            val state = vm.uiState.value
            val rawBase = EdiGlobalSemaphoreEngine.evaluateGlobalSemaphore(state.ediEngineState, state.ediCorrectedAgeComparison)
            assertEquals(EdiGlobalTrafficLight.INCOMPLETE, rawBase.trafficLight)
            assertTrue(rawBase.requiresImmediateClinicalAttentionDespiteIncomplete)

            val eval = state.ediGlobalEvaluation
            assertDegradedCriticalIncomplete(eval, EdiPriorityCriticalAlertType.POSITIVE_ALARM_SIGNAL_ALA)
        }
    }

    @Test
    fun `rama C - subcondicion C1 defensa en profundidad de modelo y subcondicion C3 mismatch de grupo`() {
        // Nota de alcance técnico:
        // En el flujo ViewModel/SessionManager, R3 impide restaurar sesiones de otro episodio/sexo
        // (startNewWorkEpisode limpia restoredFromArchiveId). Si un estado de modelo llegara a portar
        // restoredFromArchiveId con episodio distinto, la guarda defensiva C1 en ediGlobalEvaluation
        // propaga requiresImmediateClinicalAttentionDespiteIncomplete correctamente.
        val vm = CredEdiViewModel().apply {
            updateFechaEvaluacion("20/05/2025")
            updateFechaNacimiento("10/07/2023") // 22m 10d -> G10
            confirmEdiTermWithoutWeeks()
            selectEdiGroupFromBank(10)
        }
        fillAllMandatoryForGroupAsNormal(vm, 10)
        vm.recordEdiCurrentItemResponse("G10_ALA_10_1", EdiItemResponse.YES)
        val baseState = vm.uiState.value
        val mismatchedState = baseState.copy(
            ediEngineState = baseState.ediEngineState.copy(
                restoredFromArchiveId = "ARCH_EXT_G10",
                restoredSessionOriginContext = EdiArchivedClinicalContext(
                    workEpisodeId = "EP_OTRO",
                    originValidationState = EdiArchivedOriginValidationState.VERIFIED,
                    sexoLabel = baseState.sexo.label,
                    fechaNacimiento = baseState.fechaNacimiento,
                    fechaEvaluacion = baseState.fechaEvaluacion,
                    esPrematuro = false,
                    egSemanas = 40,
                    egDias = 0,
                    ediEgRegistrada = false,
                    ediBirthMaturityCondition = EdiBirthMaturityCondition.CONFIRMED_TERM,
                    effectiveAgeLabel = baseState.edadCronologicaAMD,
                    gestationalAgeDisplayLabel = baseState.ediGestationalAgeDisplay,
                    applicableGroupNumberAtCapture = 10,
                    validationSummaryAtCapture = "VERIFICADO"
                )
            )
        )
        assertTrue(mismatchedState.isRestoredSessionFromDifferentPatientOrEpisode)
        assertDegradedCriticalIncomplete(
            mismatchedState.ediGlobalEvaluation,
            EdiPriorityCriticalAlertType.POSITIVE_ALARM_SIGNAL_ALA
        )

        // Subcondición C3 (ediHasGroupAgeMismatch): en el flujo real, ageComparisonForEngine detecta
        // que grupo oficial (7) != grupo activo (6), de modo que baseResult ya nace INCOMPLETE (Caso E)
        // y Rama C conserva requiresImmediateClinicalAttentionDespiteIncomplete == true con alerta crítica.
        val vmMismatch = CredEdiViewModel().apply {
            updateFechaEvaluacion("20/07/2025")
            updateFechaNacimiento("10/08/2024") // 11m 10d -> G7
            confirmEdiTermWithoutWeeks()
            selectEdiGroupFromBank(6)
        }
        fillAllMandatoryForGroupAsNormal(vmMismatch, 6)
        vmMismatch.recordEdiCurrentItemResponse("G6_ALA_6_1", EdiItemResponse.YES)
        assertTrue(vmMismatch.uiState.value.ediHasGroupAgeMismatch)
        assertDegradedCriticalIncomplete(
            vmMismatch.uiState.value.ediGlobalEvaluation,
            EdiPriorityCriticalAlertType.POSITIVE_ALARM_SIGNAL_ALA
        )
    }

    // =========================================================================
    // CASO F: Resultado definitivo ROJO con alerta crítica sin bloqueo posterior
    // =========================================================================

    @Test
    fun `caso F - resultado definitivo ROJO con alerta critica sin bloqueo mantiene contrato del motor`() {
        // F1: ALA positiva en G4 definitivo
        run {
            val vm = CredEdiViewModel().apply {
                updateFechaEvaluacion("20/05/2025")
                updateFechaNacimiento("10/01/2025") // 4m 10d -> G4
                confirmEdiTermWithoutWeeks()
            }
            assertEquals(4, vm.uiState.value.ediEffectiveGroupNumber)
            fillAllMandatoryForGroupAsNormal(vm, 4)
            vm.recordEdiCurrentItemResponse("G4_ALA_4_1", EdiItemResponse.YES)

            val eval = vm.uiState.value.ediGlobalEvaluation
            assertEquals(EdiGlobalTrafficLight.ROJO, eval.trafficLight)
            assertTrue(eval.isDefinitive)
            assertTrue(eval.hasPriorityCriticalWarning)
            assertTrue(eval.priorityCriticalAlerts.any { it.type == EdiPriorityCriticalAlertType.POSITIVE_ALARM_SIGNAL_ALA })
            assertFalse(
                "En un dictamen definitivo ROJO sin bloqueo posterior, requiresImmediateClinicalAttentionDespiteIncomplete debe permanecer en false",
                eval.requiresImmediateClinicalAttentionDespiteIncomplete
            )
        }

        // F2: EN positiva en G6 definitivo
        run {
            val vm = CredEdiViewModel().apply {
                updateFechaEvaluacion("20/05/2025")
                updateFechaNacimiento("10/08/2024") // 9m 10d -> G6
                confirmEdiTermWithoutWeeks()
            }
            assertEquals(6, vm.uiState.value.ediEffectiveGroupNumber)
            fillAllMandatoryForGroupAsNormal(vm, 6)
            vm.recordEdiCurrentItemResponse("EN_1_2", EdiItemResponse.YES)

            val eval = vm.uiState.value.ediGlobalEvaluation
            assertEquals(EdiGlobalTrafficLight.ROJO, eval.trafficLight)
            assertTrue(eval.isDefinitive)
            assertTrue(eval.hasPriorityCriticalWarning)
            assertTrue(eval.priorityCriticalAlerts.any { it.type == EdiPriorityCriticalAlertType.ALTERED_NEUROLOGICAL_EXAM_EN })
            assertFalse(eval.requiresImmediateClinicalAttentionDespiteIncomplete)
        }

        // F3: Área ROJA en G10 definitivo
        run {
            val vm = CredEdiViewModel().apply {
                updateFechaEvaluacion("20/05/2025")
                updateFechaNacimiento("10/07/2023") // 22m 10d -> G10
                confirmEdiTermWithoutWeeks()
            }
            assertEquals(10, vm.uiState.value.ediEffectiveGroupNumber)
            fillAllMandatoryForGroupAsNormal(vm, 10)
            forceAreaRedWithCompletedFallback(vm, 10, EdiDevArea.MG)

            val eval = vm.uiState.value.ediGlobalEvaluation
            assertEquals(EdiGlobalTrafficLight.ROJO, eval.trafficLight)
            assertTrue(eval.isDefinitive)
            assertEquals(listOf(EdiDevArea.MG), eval.redAreas)
            assertTrue(eval.hasPriorityCriticalWarning)
            assertTrue(eval.priorityCriticalAlerts.any { it.type == EdiPriorityCriticalAlertType.RED_DEVELOPMENT_AREA })
            assertFalse(eval.requiresImmediateClinicalAttentionDespiteIncomplete)
        }
    }

    // =========================================================================
    // Invariantes de no regresión del catálogo
    // =========================================================================

    @Test
    fun `no regresion de invariantes del catalogo oficial y G15`() {
        assertEquals(15, EdiOfficialCatalog.groups.size)
        val totalUniqueIds = EdiOfficialCatalog.groups.sumOf { spec ->
            spec.areaItems.values.flatten().size + spec.aleItems.size + spec.alaItems.size
        } + EdiOfficialCatalog.frbItems.size + EdiOfficialCatalog.allEnItems.size + EdiOfficialCatalog.specialGroup13CoRFallbackItems.size
        assertEquals(270, totalUniqueIds)
        assertFalse(EdiOfficialCatalog.getGroup(15).neurologicalEn13Applies)
        assertEquals(2, EdiOfficialCatalog.getApplicableEnItems(15).size)
        assertEquals(3, EdiOfficialCatalog.getApplicableEnItems(1).size)
    }
}
