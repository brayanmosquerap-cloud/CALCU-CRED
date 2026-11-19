package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.example.model.AppTab
import com.example.model.CurvaIndicador
import com.example.model.EdiBirthMaturityCondition
import com.example.model.EdiClassificationCauseType
import com.example.model.EdiGlobalTrafficLight
import com.example.model.EdiGroupChangeResolutionAction
import com.example.model.EdiInternalView
import com.example.model.EdiItemResponse
import com.example.model.EdiOfficialCatalog
import com.example.util.EdiPdfExporter
import com.example.viewmodel.CredEdiViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Suite de regresión dedicada para OPCIÓN 2:
 * Confirmación obligatoria de condición al nacer en menores de 24 meses (< 24m)
 * cuando la condición al nacer es UNSPECIFIED y no existe EG registrada.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class EdiBirthMaturityUnder24mOption2Test {

    @get:Rule
    val composeRule = createComposeRule()

    private fun context(): Context = ApplicationProvider.getApplicationContext()

    private fun completeGroupAsGreen(vm: CredEdiViewModel, groupNumber: Int) {
        val spec = EdiOfficialCatalog.getGroup(groupNumber)
        spec.areaItems.values.flatten().forEach { item ->
            if (item.uniqueId == "G13_LE_13_3" || item.uniqueId == "G14_LE_14_3") {
                vm.recordEdiDualEvidence(item.uniqueId, true, true, null, "Consistente")
            } else {
                vm.recordEdiCurrentItemResponse(item.uniqueId, EdiItemResponse.YES)
            }
        }
        EdiOfficialCatalog.getApplicableEnItems(groupNumber).forEach {
            vm.recordEdiCurrentItemResponse(it.uniqueId, EdiItemResponse.NO)
        }
        spec.alaItems.forEach {
            vm.recordEdiCurrentItemResponse(it.uniqueId, EdiItemResponse.NO)
        }
        EdiOfficialCatalog.frbItems.forEach {
            vm.recordEdiCurrentItemResponse(it.uniqueId, EdiItemResponse.NO)
        }
        spec.aleItems.forEach { item ->
            if (item.uniqueId == "G12_ALE_12_3") {
                vm.recordEdiDualEvidence(item.uniqueId, false, false, null, "Sin alerta")
            } else {
                vm.recordEdiCurrentItemResponse(item.uniqueId, EdiItemResponse.NO)
            }
        }
    }

    @Test
    fun `01 menor de 24 meses con UNSPECIFIED y EG no registrada activa isBirthMaturityConfirmationPendingInUnder24m`() {
        val vm = CredEdiViewModel()
        vm.updateFechaNacimiento("15/10/2024")
        vm.updateFechaEvaluacion("15/02/2025") // 0a 4m 0d (< 24m)

        val state = vm.uiState.value
        assertEquals("0a 4m 0d", state.edadCronologicaAMD)
        assertEquals(0, state.edadCronologicaAnios)
        assertFalse(state.esPrematuro)
        assertFalse(state.ediEgRegistrada)
        assertFalse(state.ediHasRegisteredEg)
        assertEquals(EdiBirthMaturityCondition.UNSPECIFIED, state.ediEffectiveMaturityCondition)
        assertTrue(state.isBirthMaturityConfirmationPendingInUnder24m)
        assertTrue(state.isAgeDependentEvaluationBlocked)
        assertFalse("No debe activar isCorrectedAgePending sino isBirthMaturityConfirmationPendingInUnder24m", state.isCorrectedAgePending)
        assertEquals("Confirmar condición", state.ediGestationalAgeHeadlineForSummary)
        assertEquals("Pendiente de confirmar condición al nacer", state.proximoControlFecha)

        composeRule.setContent { CredEdiApp(viewModel = vm) }
        composeRule.onNodeWithTag("calc_birth_maturity_summary_pill")
            .performScrollTo()
            .assertTextContains("Pendiente confirmar", substring = true)
        composeRule.onNodeWithTag("calc_pending_birth_maturity_banner")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag("calc_confirm_term_no_weeks_button")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag("calc_activate_preterm_button")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun `02 menor de 24 meses con UNSPECIFIED no asigna automaticamente grupo EDI por edad`() {
        val vm = CredEdiViewModel()
        // El grupo inicial por defecto es G1; al ingresar una edad de 6m 15d (que correspondería a G5),
        // NO debe cambiar automáticamente a G5 mientras la condición al nacer sea UNSPECIFIED.
        assertEquals(1, vm.uiState.value.ediEffectiveGroupNumber)

        vm.updateFechaEvaluacion("16/02/2025")
        vm.updateFechaNacimiento("01/08/2024") // 0a 6m 15d -> correspondería a G5 si fuera a término

        val state = vm.uiState.value
        assertTrue(state.isBirthMaturityConfirmationPendingInUnder24m)
        assertEquals(
            "No debe asignar automáticamente el Grupo 5 sin confirmar condición al nacer",
            1,
            state.ediEffectiveGroupNumber
        )
        assertNull(state.ediEngineState.pendingGroupChangeConfirmation)
    }

    @Test
    fun `03 menor de 24 meses con UNSPECIFIED y todos los reactivos contestados mantiene semaforo INCOMPLETE y no definitivo`() {
        val vm = CredEdiViewModel()
        vm.updateFechaNacimiento("05/01/2025")
        vm.updateFechaEvaluacion("20/02/2025") // 1m 15d (< 24m)
        completeGroupAsGreen(vm, 1)

        val state = vm.uiState.value
        assertTrue(state.isBirthMaturityConfirmationPendingInUnder24m)
        val eval = state.ediGlobalEvaluation
        assertEquals(EdiGlobalTrafficLight.INCOMPLETE, eval.trafficLight)
        assertFalse(eval.isDefinitive)
        assertTrue(eval.blockedByAgeDiscrepancy)
        assertTrue(eval.blockingReasons.any { it.contains("BLOQUEO CLÍNICO DE CONDICIÓN AL NACER") })
        assertTrue(eval.causes.any { it.type == EdiClassificationCauseType.BLOCKED_AGE_DISCREPANCY })

        // Todas las respuestas registradas se conservan intactas
        assertTrue(state.ediEngineState.activeSession.currentGroupResponses.isNotEmpty())
        assertTrue(state.ediEngineState.activeSession.currentGroupResponses.values.all { it.isAnsweredDefinitively })
    }

    @Test
    fun `04 menor de 24 meses con UNSPECIFIED exporta PDF como BORRADOR sin dictamen definitivo ni diagnosticos por edad en pie`() {
        val vm = CredEdiViewModel()
        vm.updateFechaNacimiento("05/01/2025")
        vm.updateFechaEvaluacion("20/02/2025") // 1m 15d (< 24m)
        vm.updatePesoInput("4.80")
        vm.updateLongitudInput("56.0")
        vm.updatePerimetroCefalicoInput("38.0")
        completeGroupAsGreen(vm, 1)

        val state = vm.uiState.value
        assertTrue(state.isBirthMaturityConfirmationPendingInUnder24m)

        val pdfRes = EdiPdfExporter.exportarFichaOficialEdiPdf(context(), state)
        assertTrue(pdfRes.message, pdfRes.success)
        assertTrue("El PDF debe exportarse estrictamente como BORRADOR", pdfRes.isDraft)
        assertTrue(pdfRes.fileName!!.startsWith("Borrador_EDI_Incompleta_"))
        assertTrue(File(pdfRes.filePath!!).exists())

        // Pie de Página 1 conserva mediciones registradas pero NO imprime clasificaciones OMS dependientes de edad
        val footer = EdiPdfExporter.buildCredFooterSummary(state)
        assertTrue(footer.contains("Peso: 4.80 kg"))
        assertTrue(footer.contains("Talla: 56.0 cm"))
        assertTrue(footer.contains("PC: 38.0 cm"))
        assertFalse("No debe imprimir clasificación P/E sin confirmar condición al nacer", footer.contains(state.evalPE.shortClassification))
        assertFalse("No debe imprimir clasificación T/E sin confirmar condición al nacer", footer.contains(state.evalTE.shortClassification))

        val p1Lines = EdiPdfExporter.buildPage1GestationalAgeLines(state)
        assertEquals(listOf("EG no registrada", "(Sin confirmación)"), p1Lines)
        val p2Detail = EdiPdfExporter.buildPage2GestationalAgeDetail(state)
        assertTrue(p2Detail.contains("EG no registrada (condición al nacer no especificada)"))
    }

    @Test
    fun `05 menor de 24 meses con UNSPECIFIED en Ventana 2 muestra aviso pendiente en indicadores por edad y mantiene PT`() {
        val vm = CredEdiViewModel()
        vm.updateFechaNacimiento("15/10/2024")
        vm.updateFechaEvaluacion("15/02/2025") // 4m 0d (< 24m)
        vm.updatePesoInput("6.50")
        vm.updateLongitudInput("63.0")
        vm.updatePerimetroCefalicoInput("41.0")

        composeRule.setContent { CredEdiApp(viewModel = vm) }
        composeRule.onNodeWithTag("tab_curvas").performClick()
        composeRule.waitForIdle()

        // Banner de confirmación pendiente visible en Ventana 2
        composeRule.onNodeWithTag("curvas_pending_corrected_age_banner")
            .performScrollTo()
            .assertIsDisplayed()

        // P/E depende de la edad -> muestra clasificación pendiente de confirmar condición al nacer
        vm.selectCurvaIndicador(CurvaIndicador.PE)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("curva_summary_headline")
            .performScrollTo()
            .assertTextContains("6.50 kg · Clasificación pendiente de confirmar condición al nacer", substring = true)

        // P/T (Peso para la Longitud) NO depende de la edad en < 24m -> se mantiene disponible y con su clasificación real
        vm.selectCurvaIndicador(CurvaIndicador.PT)
        composeRule.waitForIdle()
        val expectedPtClassification = vm.uiState.value.evalPT.shortClassification
        composeRule.onNodeWithTag("curva_summary_headline")
            .performScrollTo()
            .assertTextContains("6.50 kg", substring = true)
            .assertTextContains("· $expectedPtClassification", substring = true)
    }

    @Test
    fun `06 menor de 24 meses con UNSPECIFIED en Ventana 3 muestra diagnostico y proximo control pendientes manteniendo PT y mediciones`() {
        val vm = CredEdiViewModel()
        vm.updateFechaNacimiento("15/10/2024")
        vm.updateFechaEvaluacion("15/02/2025") // 4m 0d (< 24m)
        vm.updatePesoInput("6.50")
        vm.updateLongitudInput("63.0")
        vm.updatePerimetroCefalicoInput("41.0")

        composeRule.setContent { CredEdiApp(viewModel = vm) }
        composeRule.onNodeWithTag("tab_diagnostico").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("diagnostico_edad_corregida_text")
            .performScrollTo()
            .assertTextContains("PENDIENTE DE CONFIRMACIÓN DE CONDICIÓN AL NACER", substring = true)
        composeRule.onNodeWithTag("diagnostico_global_title")
            .performScrollTo()
            .assertTextContains("PENDIENTE DE CONFIRMACIÓN DE CONDICIÓN AL NACER", substring = true)

        assertEquals("Pendiente de confirmar condición al nacer", vm.uiState.value.proximoControlFecha)
        assertEquals(6.50, vm.uiState.value.pesoKg, 0.001)
        assertEquals(63.0, vm.uiState.value.longitudCm, 0.01)
        assertEquals(41.0, vm.uiState.value.perimetroCefalicoCm, 0.01)
        assertTrue(vm.uiState.value.evalPT.zScore.isFinite())
    }

    @Test
    fun `07 menor de 24 meses al confirmar A termino sin semanas exactas desbloquea evaluacion usa edad cronologica asigna grupo y no inventa semanas`() {
        val vm = CredEdiViewModel()
        vm.updateFechaEvaluacion("16/02/2025")
        vm.updateFechaNacimiento("01/08/2024") // 6m 15d -> Grupo 5

        // Antes de confirmar: pendiente y en G1
        assertTrue(vm.uiState.value.isBirthMaturityConfirmationPendingInUnder24m)
        assertEquals(1, vm.uiState.value.ediEffectiveGroupNumber)

        // Confirmar A término sin semanas exactas
        vm.confirmEdiTermWithoutWeeks()

        val state = vm.uiState.value
        assertFalse(state.isBirthMaturityConfirmationPendingInUnder24m)
        assertFalse(state.isAgeDependentEvaluationBlocked)
        assertFalse(state.esPrematuro)
        assertFalse(state.ediHasRegisteredEg)
        assertEquals(EdiBirthMaturityCondition.CONFIRMED_TERM, state.ediEffectiveMaturityCondition)
        assertEquals("A término — semanas exactas no registradas", state.ediGestationalAgeDisplay)
        assertEquals("No aplica edad corregida", state.edadCorregidaAMD)
        assertEquals(5, state.ediEffectiveGroupNumber) // 6m 15d -> Grupo 5 asignado automáticamente

        completeGroupAsGreen(vm, 5)
        assertEquals(EdiGlobalTrafficLight.VERDE, vm.uiState.value.ediGlobalEvaluation.trafficLight)
        assertTrue(vm.uiState.value.ediGlobalEvaluation.isDefinitive)

        val p1Joined = EdiPdfExporter.buildPage1GestationalAgeLines(vm.uiState.value).joinToString(" ")
        val p2Detail = EdiPdfExporter.buildPage2GestationalAgeDetail(vm.uiState.value)
        listOf("32", "37", "38", "39", "40", "≥37").forEach { forbidden ->
            assertFalse("No debe inventar $forbidden en pág 1", p1Joined.contains(forbidden))
            assertFalse("No debe inventar $forbidden en pág 2", p2Detail.contains(forbidden))
        }
    }

    @Test
    fun `08 menor de 24 meses al confirmar A termino con EG 38s 2d desbloquea evaluacion usa edad cronologica y muestra A termino 38s 2d`() {
        val vm = CredEdiViewModel()
        vm.updateFechaNacimiento("01/08/2024")
        vm.updateFechaEvaluacion("16/02/2025") // 6m 15d -> Grupo 5
        assertTrue(vm.uiState.value.isBirthMaturityConfirmationPendingInUnder24m)

        vm.updateEgSemanas(38)
        vm.updateEgDias(2)

        val state = vm.uiState.value
        assertFalse(state.isBirthMaturityConfirmationPendingInUnder24m)
        assertFalse(state.isAgeDependentEvaluationBlocked)
        assertFalse(state.esPrematuro)
        assertTrue(state.ediHasRegisteredEg)
        assertEquals(EdiBirthMaturityCondition.CONFIRMED_TERM, state.ediEffectiveMaturityCondition)
        assertEquals("A término · 38s 2d", state.ediGestationalAgeDisplay)
        assertEquals("0a 6m 15d", state.edadEfectivaAMD)
        assertEquals(5, state.ediEffectiveGroupNumber)

        completeGroupAsGreen(vm, 5)
        assertEquals(EdiGlobalTrafficLight.VERDE, vm.uiState.value.ediGlobalEvaluation.trafficLight)
        assertTrue(vm.uiState.value.ediGlobalEvaluation.isDefinitive)
    }

    @Test
    fun `09 menor de 24 meses al activar prematuridad sin EG exacta pasa al flujo EDAD CORREGIDA PENDIENTE`() {
        val vm = CredEdiViewModel()
        vm.updateFechaNacimiento("15/10/2024")
        vm.updateFechaEvaluacion("15/02/2025") // 4m 0d (< 24m)

        // Inicialmente está pendiente de confirmar condición al nacer (UNSPECIFIED)
        assertTrue(vm.uiState.value.isBirthMaturityConfirmationPendingInUnder24m)
        assertFalse(vm.uiState.value.isCorrectedAgePending)

        // Activar prematuridad sin EG exacta -> pasa a CONFIRMED_PRETERM con isCorrectedAgePending
        vm.togglePrematurez(true)

        val state = vm.uiState.value
        assertFalse(state.isBirthMaturityConfirmationPendingInUnder24m)
        assertEquals(EdiBirthMaturityCondition.CONFIRMED_PRETERM, state.ediEffectiveMaturityCondition)
        assertTrue(state.isCorrectedAgePending)
        assertTrue(state.isAgeDependentEvaluationBlocked)
        assertEquals("EDAD CORREGIDA PENDIENTE", state.edadCorregidaAMD)
        assertEquals("Pretérmino confirmado — EG exacta no registrada", state.ediGestationalAgeDisplay)
    }

    @Test
    fun `10 menor de 24 meses al confirmar prematuridad con EG 34s 5d calcula 37 dias de prematuridad aplica edad corregida y desbloquea evaluacion`() {
        val vm = CredEdiViewModel()
        vm.updateFechaNacimiento("15/10/2024")
        vm.updateFechaEvaluacion("15/02/2025") // 4m 0d cronológicos (Grupo 4)
        assertTrue(vm.uiState.value.isBirthMaturityConfirmationPendingInUnder24m)

        vm.updateEgSemanas(34)
        vm.updateEgDias(5) // 280 - (34*7 + 5) = 37 días de prematuridad -> edad corregida 2m 24d (Grupo 2)

        val state = vm.uiState.value
        assertFalse(state.isBirthMaturityConfirmationPendingInUnder24m)
        assertFalse(state.isCorrectedAgePending)
        assertFalse(state.isAgeDependentEvaluationBlocked)
        assertTrue(state.esPrematuro)
        assertTrue(state.ediHasRegisteredEg)
        assertEquals(37, state.diasPrematuridad)
        assertTrue(state.aplicaEdadCorregida)
        assertEquals("Pretérmino · 34s 5d", state.ediGestationalAgeDisplay)
        assertEquals(2, state.ediEffectiveGroupNumber) // Asignado por edad corregida (2m 24d -> Grupo 2)

        completeGroupAsGreen(vm, 2)
        assertEquals(EdiGlobalTrafficLight.VERDE, vm.uiState.value.ediGlobalEvaluation.trafficLight)
        assertTrue(vm.uiState.value.ediGlobalEvaluation.isDefinitive)
    }

    @Test
    fun `11 menor de 24 meses al confirmar 32s 0d explicitamente calcula 56 dias de prematuridad aplica edad corregida y desbloquea evaluacion`() {
        val vm = CredEdiViewModel()
        vm.updateFechaNacimiento("15/10/2024")
        vm.updateFechaEvaluacion("15/02/2025") // 4m 0d cronológicos
        assertTrue(vm.uiState.value.isBirthMaturityConfirmationPendingInUnder24m)

        vm.togglePrematurez(true)
        assertTrue(vm.uiState.value.isCorrectedAgePending)

        // Confirmar explícitamente 32s 0d sin mover el slider
        vm.confirmCurrentEgSemanas()

        val state = vm.uiState.value
        assertFalse(state.isBirthMaturityConfirmationPendingInUnder24m)
        assertFalse(state.isCorrectedAgePending)
        assertFalse(state.isAgeDependentEvaluationBlocked)
        assertTrue(state.ediHasRegisteredEg)
        assertEquals(56, state.diasPrematuridad)
        assertTrue(state.aplicaEdadCorregida)
        assertEquals("0a 2m 5d", state.edadCorregidaAMD)
        assertEquals("Pretérmino · 32s 0d", state.ediGestationalAgeDisplay)
        assertEquals(2, state.ediEffectiveGroupNumber) // 2m 5d -> Grupo 2
    }

    @Test
    fun `12 frontera 23m 29d 1a 11m 29d con UNSPECIFIED exige confirmacion de condicion al nacer`() {
        val vm = CredEdiViewModel()
        vm.updateFechaNacimiento("15/05/2024")
        vm.updateFechaEvaluacion("14/05/2026") // Exactamente 1a 11m 29d (23m 29d)

        val state = vm.uiState.value
        assertEquals("1a 11m 29d", state.edadCronologicaAMD)
        assertEquals(1, state.edadCronologicaAnios)
        assertEquals(EdiBirthMaturityCondition.UNSPECIFIED, state.ediEffectiveMaturityCondition)
        assertTrue(
            "En 1a 11m 29d (< 24 meses cumplidos) debe exigir confirmación de condición al nacer",
            state.isBirthMaturityConfirmationPendingInUnder24m
        )
        assertTrue(state.isAgeDependentEvaluationBlocked)
    }

    @Test
    fun `13 frontera 24m 0d 2a 0m 0d con UNSPECIFIED NO exige confirmacion y usa edad cronologica`() {
        val vm = CredEdiViewModel()
        vm.updateFechaNacimiento("15/05/2024")
        vm.updateFechaEvaluacion("15/05/2026") // Exactamente 2a 0m 0d (24m 0d)

        val state = vm.uiState.value
        assertEquals("2a 0m 0d", state.edadCronologicaAMD)
        assertEquals(2, state.edadCronologicaAnios)
        assertEquals(24, state.edadCronologicaMeses)
        assertEquals(EdiBirthMaturityCondition.UNSPECIFIED, state.ediEffectiveMaturityCondition)
        assertFalse(
            "En 2a 0m 0d (>= 24 meses cumplidos) no aplica edad corregida y no exige confirmación",
            state.isBirthMaturityConfirmationPendingInUnder24m
        )
        assertFalse(state.isAgeDependentEvaluationBlocked)
        assertEquals(10, state.ediEffectiveGroupNumber) // 24m 0d -> Grupo 10 (19m a 24m 29d)
    }

    @Test
    fun `14 limpiar o revertir confirmacion en menor de 24 meses despues de tener semaforo definitivo vuelve a UNSPECIFIED e invalida semaforo y PDF`() {
        val vm = CredEdiViewModel()
        vm.updateFechaNacimiento("05/01/2025")
        vm.updateFechaEvaluacion("20/02/2025") // 1m 15d -> Grupo 1
        vm.confirmEdiTermWithoutWeeks()
        completeGroupAsGreen(vm, 1)

        // 1. Con confirmación a término: semáforo definitivo VERDE y PDF oficial
        assertEquals(EdiGlobalTrafficLight.VERDE, vm.uiState.value.ediGlobalEvaluation.trafficLight)
        assertTrue(vm.uiState.value.ediGlobalEvaluation.isDefinitive)
        val pdfOfficial = EdiPdfExporter.exportarFichaOficialEdiPdf(context(), vm.uiState.value)
        assertFalse(pdfOfficial.isDraft)

        // 2. Limpiar/revertir confirmación -> vuelve a UNSPECIFIED
        vm.clearEdiRegisteredEg()

        val stateAfter = vm.uiState.value
        assertEquals(EdiBirthMaturityCondition.UNSPECIFIED, stateAfter.ediEffectiveMaturityCondition)
        assertTrue(stateAfter.isBirthMaturityConfirmationPendingInUnder24m)
        assertTrue(stateAfter.isAgeDependentEvaluationBlocked)
        assertEquals(EdiGlobalTrafficLight.INCOMPLETE, stateAfter.ediGlobalEvaluation.trafficLight)
        assertFalse(stateAfter.ediGlobalEvaluation.isDefinitive)

        val pdfDraft = EdiPdfExporter.exportarFichaOficialEdiPdf(context(), stateAfter)
        assertTrue("Al revertir a UNSPECIFIED en < 24m, el PDF debe volver a ser BORRADOR", pdfDraft.isDraft)
        assertTrue(pdfDraft.fileName!!.startsWith("Borrador_EDI_Incompleta_"))
    }

    @Test
    fun `15 alertas clinicas prioritarias ALA positiva EN positiva y area ROJO en menor de 24 meses con UNSPECIFIED siguen visibles mientras semaforo es INCOMPLETE`() {
        val vm = CredEdiViewModel()
        vm.updateFechaNacimiento("05/01/2025")
        vm.updateFechaEvaluacion("10/03/2025") // 2m 5d (< 24m), UNSPECIFIED
        vm.selectEdiGroupFromBank(2)

        val specG2 = EdiOfficialCatalog.getGroup(2)
        // Registrar ALA positiva, EN positiva y área MG en ROJO (0/2 en G2 con retroceso G1 en NO)
        vm.recordEdiCurrentItemResponse(specG2.alaItems.first().uniqueId, EdiItemResponse.YES)
        vm.recordEdiCurrentItemResponse(EdiOfficialCatalog.getApplicableEnItems(2).first().uniqueId, EdiItemResponse.YES)
        vm.recordEdiCurrentItemResponse("G2_MG_2_1", EdiItemResponse.NO)
        vm.recordEdiCurrentItemResponse("G2_MG_2_2", EdiItemResponse.NO)
        vm.recordEdiFallbackResponse("G2_RETRO_G1_MG_1", EdiItemResponse.NO)
        vm.recordEdiFallbackResponse("G2_RETRO_G1_MG_2", EdiItemResponse.NO)

        val state = vm.uiState.value
        assertTrue(state.isBirthMaturityConfirmationPendingInUnder24m)
        val eval = state.ediGlobalEvaluation
        assertEquals(EdiGlobalTrafficLight.INCOMPLETE, eval.trafficLight)
        assertFalse(eval.isDefinitive)
        assertTrue(eval.hasPriorityCriticalWarning)
        assertTrue(eval.requiresImmediateClinicalAttentionDespiteIncomplete)
        assertTrue(eval.priorityCriticalAlerts.size >= 3)
    }

    @Test
    fun `16 cambio de condicion al nacer en menor de 24 meses cuando el grupo activo tiene respuestas y la edad confirmada corresponde a otro grupo solicita confirmacion y no mezcla respuestas`() {
        val vm = CredEdiViewModel()
        vm.updateFechaEvaluacion("16/02/2025")
        vm.updateFechaNacimiento("01/08/2024") // 6m 15d (G5 si es a término), inicia en G1 por UNSPECIFIED

        assertEquals(1, vm.uiState.value.ediEffectiveGroupNumber)
        // Registrar respuestas en G1 antes de confirmar condición al nacer
        vm.recordEdiCurrentItemResponse("G1_MG_1_1", EdiItemResponse.YES)
        vm.recordEdiCurrentItemResponse("G1_MF_1_1", EdiItemResponse.NO)
        assertTrue(vm.uiState.value.ediEngineState.activeSession.hasRecordedClinicalResponses)

        // Confirmar A término sin semanas exactas -> la edad cronológica (6m 15d) sugiere Grupo 5 != Grupo 1
        vm.confirmEdiTermWithoutWeeks()

        val stateWithDialog = vm.uiState.value
        assertFalse(stateWithDialog.isBirthMaturityConfirmationPendingInUnder24m)
        assertEquals("Permanece en G1 hasta que el profesional confirme el cambio de grupo", 1, stateWithDialog.ediEffectiveGroupNumber)
        val pending = stateWithDialog.ediEngineState.pendingGroupChangeConfirmation
        assertNotNull("Debe solicitar confirmación antes de cambiar de G1 a G5", pending)
        assertEquals(1, pending!!.previousGroupNumber)
        assertEquals(5, pending.targetGroupNumber)

        // Confirmar archivado de G1 y cambio limpio a G5 -> no mezcla respuestas de G1 en G5
        vm.resolveEdiPendingGroupChange(
            EdiGroupChangeResolutionAction.ARCHIVE_PREVIOUS_AND_SWITCH_TO_CLEAN_TARGET
        )

        val stateSwitched = vm.uiState.value
        assertEquals(5, stateSwitched.ediEffectiveGroupNumber)
        assertNull(stateSwitched.ediEngineState.pendingGroupChangeConfirmation)
        assertTrue(
            "La nueva sesión en G5 debe iniciar limpia sin mezclar respuestas de G1",
            stateSwitched.ediEngineState.activeSession.currentGroupResponses.values.all { it == EdiItemResponse.UNANSWERED }
        )
        // Y la sesión previa de G1 quedó archivada intacta
        val archivedG1 = stateSwitched.ediEngineState.archivedSessionsByGroup[1]
        assertNotNull(archivedG1)
        assertEquals(EdiItemResponse.YES, archivedG1!!.currentGroupResponses["G1_MG_1_1"])
    }
}
