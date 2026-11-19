package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.example.model.AppTab
import com.example.model.ClassificationSeverity
import com.example.model.CurvaIndicador
import com.example.model.CutoffBand
import com.example.model.EdiAreaColorResult
import com.example.model.EdiDevArea
import com.example.model.EdiGlobalTrafficLight
import com.example.model.EdiItemResponse
import com.example.model.EdiOfficialCatalog
import com.example.model.GlobalDiagnosisCompleteness
import com.example.model.PerimetroCefalicoCaptureState
import com.example.model.PerimetroCefalicoOmsStatus
import com.example.model.SexoBiologico
import com.example.model.WhoGrowthStandards
import com.example.util.EdiPdfExporter
import com.example.viewmodel.CredEdiViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * MEDIO-05 — LOTE PC/E-01
 * Pruebas unitarias y Robolectric de corrección de clasificación cefálica sin medición.
 * Cubre obligatoriamente los Casos A a M de la sección 8 del lote PC/E-01.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class EdiMedio05Pce01HeadCircumferenceEmptyTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun context(): Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `caso A estado inicial con PC vacio muestra Sin medicion y nunca Microcefalia`() {
        val vm = CredEdiViewModel()
        vm.selectSexo(SexoBiologico.NINO)
        vm.updateFechaNacimiento("15/06/2022")
        vm.updateFechaEvaluacion("15/06/2026") // 48m 0d
        vm.updatePesoInput("17.5")
        vm.updateLongitudInput("110.5")

        val state = vm.uiState.value
        assertEquals("", state.perimetroCefalicoInput)
        assertTrue(state.perimetroCefalicoCm.isNaN())
        assertTrue(state.effectivePerimetroCefalicoCm.isNaN())
        assertFalse(state.hasPerimetroCefalicoIngresado)
        assertFalse(state.isPerimetroCefalicoTemporalOrInvalid)
        assertEquals(PerimetroCefalicoCaptureState.EMPTY_UNREGISTERED, state.perimetroCefalicoCaptureState)
        assertEquals(PerimetroCefalicoOmsStatus.UNREGISTERED, state.pcOmsEvaluationStatus)

        // Evaluación PC/E sin medición
        val pce = state.evalPCE
        assertFalse(pce.isReferenceAvailable)
        assertTrue(pce.zScore.isNaN())
        assertEquals("--", pce.patientValueFormatted)
        assertEquals("Sin medición", pce.shortClassification)
        assertEquals("Perímetro cefálico no registrado", pce.fullClassification)
        assertEquals("Perímetro cefálico no registrado", pce.unavailabilityTitle)
        assertNotEquals("Microcefalia", pce.shortClassification)
        assertFalse(pce.fullClassification.contains("Microcefalia", ignoreCase = true))

        // Diagnóstico global no reporta Microcefalia
        val global = state.globalDiagnosis
        assertFalse(global.pcAvailable)
        assertEquals("Sin medición", global.pcClassification)
        assertNotEquals("Microcefalia", global.pcClassification)

        // Verificación UI en DiagnosticoScreen
        composeRule.setContent { CredEdiApp(viewModel = vm) }
        composeRule.onNodeWithTag("tab_diagnostico").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("diagnostico_mini_pce_card")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNode(
            hasAnyAncestor(hasTestTag("diagnostico_mini_pce_card")) and
                hasText("Cráneo (PC/E)", substring = true),
            useUnmergedTree = true
        ).assertExists()
        composeRule.onNode(
            hasAnyAncestor(hasTestTag("diagnostico_mini_pce_card")) and
                hasText("Sin medición", substring = true),
            useUnmergedTree = true
        ).assertExists()

        composeRule.onNodeWithTag("diagnostico_grid_pce_card")
            .performScrollTo()
        composeRule.onAllNodes(
            hasAnyAncestor(hasTestTag("diagnostico_grid_pce_card")) and
                hasText("Sin medición", substring = true),
            useUnmergedTree = true
        ).onFirst().assertExists()
        composeRule.onAllNodes(
            hasAnyAncestor(hasTestTag("diagnostico_grid_pce_card")) and
                hasText("--", substring = true),
            useUnmergedTree = true
        ).onFirst().assertExists()
        composeRule.onAllNodes(
            hasAnyAncestor(hasTestTag("diagnostico_grid_pce_card")) and
                hasText("Perímetro cefálico no registrado", substring = true),
            useUnmergedTree = true
        ).onFirst().assertExists()

        assertEquals(0, composeRule.onAllNodesWithText("Microcefalia", substring = true).fetchSemanticsNodes().size)
    }

    @Test
    fun `caso B ingreso de PC 50 cm a los 48 meses evalua Normocefalo con Z valido`() {
        val vm = CredEdiViewModel()
        vm.selectSexo(SexoBiologico.NINO)
        vm.updateFechaNacimiento("15/06/2022")
        vm.updateFechaEvaluacion("15/06/2026") // 48m 0d
        vm.updatePesoInput("17.5")
        vm.updateLongitudInput("110.5")
        vm.updatePerimetroCefalicoInput("50")

        val state = vm.uiState.value
        assertEquals("50", state.perimetroCefalicoInput)
        assertEquals(50.0, state.perimetroCefalicoCm, 0.001)
        assertEquals(50.0, state.effectivePerimetroCefalicoCm, 0.001)
        assertTrue(state.hasPerimetroCefalicoIngresado)
        assertEquals(PerimetroCefalicoCaptureState.VALIDATED_MEASUREMENT, state.perimetroCefalicoCaptureState)
        assertEquals(PerimetroCefalicoOmsStatus.NORMAL_IN_RANGE, state.pcOmsEvaluationStatus)
        assertTrue(state.pcEnRangoNormalOms)

        val pce = state.evalPCE
        assertTrue(pce.isReferenceAvailable)
        assertTrue(pce.zScore.isFinite())
        assertEquals("Normocéfalo", pce.shortClassification)
        assertEquals(CutoffBand.NORMAL_RANGE, pce.activeBand)
        assertEquals(ClassificationSeverity.NORMAL, pce.severity)

        val global = state.globalDiagnosis
        assertTrue(global.pcAvailable)
        assertEquals("Normocéfalo", global.pcClassification)

        composeRule.setContent { CredEdiApp(viewModel = vm) }
        composeRule.onNodeWithTag("tab_diagnostico").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("diagnostico_mini_pce_card")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNode(
            hasAnyAncestor(hasTestTag("diagnostico_mini_pce_card")) and
                hasText("Cráneo (PC/E)", substring = true),
            useUnmergedTree = true
        ).assertExists()
        composeRule.onNode(
            hasAnyAncestor(hasTestTag("diagnostico_mini_pce_card")) and
                hasText("Normocéfalo", substring = true),
            useUnmergedTree = true
        ).assertExists()

        composeRule.onNodeWithTag("diagnostico_grid_pce_card")
            .performScrollTo()
        composeRule.onAllNodes(
            hasAnyAncestor(hasTestTag("diagnostico_grid_pce_card")) and
                hasText("Normocéfalo", substring = true),
            useUnmergedTree = true
        ).onFirst().assertExists()
        composeRule.onAllNodes(
            hasAnyAncestor(hasTestTag("diagnostico_grid_pce_card")) and
                hasText("50.0", substring = true),
            useUnmergedTree = true
        ).onFirst().assertExists()
        composeRule.onAllNodes(
            hasAnyAncestor(hasTestTag("diagnostico_grid_pce_card")) and
                hasText("cm", substring = true),
            useUnmergedTree = true
        ).onFirst().assertExists()
    }

    @Test
    fun `caso C borrado secuencial 50 a 5 a vacio termina en Sin medicion y nunca Microcefalia ficticia`() {
        val vm = CredEdiViewModel()
        vm.selectSexo(SexoBiologico.NINO)
        vm.updateFechaNacimiento("15/06/2022")
        vm.updateFechaEvaluacion("15/06/2026")
        vm.updatePesoInput("17.5")
        vm.updateLongitudInput("110.5")

        // Paso 1: "50" -> Normocéfalo
        vm.updatePerimetroCefalicoInput("50")
        assertEquals("Normocéfalo", vm.uiState.value.evalPCE.shortClassification)

        // Paso 2: "5" (transitorio durante borrado) -> NO se convierte en 20.0 cm ni en Microcefalia
        vm.updatePerimetroCefalicoInput("5")
        val step5 = vm.uiState.value
        assertEquals("5", step5.perimetroCefalicoInput)
        assertTrue(step5.perimetroCefalicoCm.isNaN())
        assertFalse(step5.hasPerimetroCefalicoIngresado)
        assertTrue(step5.isPerimetroCefalicoTemporalOrInvalid)
        assertNotEquals("Microcefalia", step5.evalPCE.shortClassification)
        assertNotEquals("Microcefalia", step5.globalDiagnosis.pcClassification)

        // Paso 3: "" -> Sin medición
        vm.updatePerimetroCefalicoInput("")
        val finalState = vm.uiState.value
        assertEquals("", finalState.perimetroCefalicoInput)
        assertTrue(finalState.perimetroCefalicoCm.isNaN())
        assertFalse(finalState.hasPerimetroCefalicoIngresado)
        assertFalse(finalState.isPerimetroCefalicoTemporalOrInvalid)
        assertEquals("Sin medición", finalState.evalPCE.shortClassification)
        assertTrue(finalState.evalPCE.zScore.isNaN())
        assertEquals("--", finalState.evalPCE.patientValueFormatted)
        assertEquals("Sin medición", finalState.globalDiagnosis.pcClassification)
        assertNotEquals("Microcefalia", finalState.evalPCE.shortClassification)
    }

    @Test
    fun `caso D borrado directo 50 a vacio limpia valor anterior y no conserva Normocefalo ni 50 cm`() {
        val vm = CredEdiViewModel()
        vm.selectSexo(SexoBiologico.NINO)
        vm.updateFechaNacimiento("15/06/2022")
        vm.updateFechaEvaluacion("15/06/2026")
        vm.updatePesoInput("17.5")
        vm.updateLongitudInput("110.5")

        vm.updatePerimetroCefalicoInput("50")
        assertEquals(50.0, vm.uiState.value.perimetroCefalicoCm, 0.001)
        assertEquals("Normocéfalo", vm.uiState.value.evalPCE.shortClassification)

        vm.updatePerimetroCefalicoInput("")
        val cleared = vm.uiState.value
        assertEquals("", cleared.perimetroCefalicoInput)
        assertTrue(cleared.perimetroCefalicoCm.isNaN())
        assertTrue(cleared.effectivePerimetroCefalicoCm.isNaN())
        assertFalse(cleared.hasPerimetroCefalicoIngresado)
        assertFalse(cleared.evalPCE.isReferenceAvailable)
        assertEquals("Sin medición", cleared.evalPCE.shortClassification)
        assertEquals("--", cleared.evalPCE.patientValueFormatted)
        assertEquals("Sin medición", cleared.globalDiagnosis.pcClassification)
    }

    @Test
    fun `caso E entrada temporal 5 no se convierte silenciosamente en 20 cm ni genera diagnostico OMS`() {
        val vm = CredEdiViewModel()
        vm.selectSexo(SexoBiologico.NINO)
        vm.updateFechaNacimiento("15/06/2022")
        vm.updateFechaEvaluacion("15/06/2026")
        vm.updatePesoInput("16.3")
        vm.updateLongitudInput("103.3")

        vm.updatePerimetroCefalicoInput("5")
        val state = vm.uiState.value
        assertEquals("5", state.perimetroCefalicoInput)
        assertNotEquals(20.0, state.perimetroCefalicoCm, 0.001)
        assertTrue(state.perimetroCefalicoCm.isNaN())
        assertFalse(state.hasPerimetroCefalicoIngresado)
        assertTrue(state.isPerimetroCefalicoTemporalOrInvalid)
        assertEquals(PerimetroCefalicoCaptureState.TEMPORARY_OR_INVALID, state.perimetroCefalicoCaptureState)
        assertEquals(PerimetroCefalicoOmsStatus.PENDING_VALIDATION, state.pcOmsEvaluationStatus)
        assertFalse(state.evalPCE.isReferenceAvailable)
        assertTrue(state.evalPCE.zScore.isNaN())
        assertEquals("Sin medición", state.evalPCE.shortClassification)
    }

    @Test
    fun `caso F entrada fuera de intervalo 70 no se convierte silenciosamente en 65 cm y marca revision clinica`() {
        val vm = CredEdiViewModel()
        vm.selectSexo(SexoBiologico.NINO)
        vm.updateFechaNacimiento("15/06/2022")
        vm.updateFechaEvaluacion("15/06/2026")
        vm.updatePesoInput("16.3")
        vm.updateLongitudInput("103.3")

        vm.updatePerimetroCefalicoInput("70")
        val state = vm.uiState.value
        assertEquals("70", state.perimetroCefalicoInput)
        assertNotEquals(65.0, state.perimetroCefalicoCm, 0.001)
        assertEquals(70.0, state.perimetroCefalicoCm, 0.001)
        assertTrue(state.hasPerimetroCefalicoIngresado)
        assertTrue(state.requiresPerimetroCefalicoClinicalReview)
        assertEquals(PerimetroCefalicoCaptureState.VALIDATED_MEASUREMENT, state.perimetroCefalicoCaptureState)
        assertEquals(PerimetroCefalicoOmsStatus.ALTERED_OUT_OF_RANGE, state.pcOmsEvaluationStatus)
        assertFalse(state.pcEnRangoNormalOms)
        assertTrue(state.evalPCE.isReferenceAvailable)
        assertEquals(70.0, state.evalPCE.patientValue, 0.001)
        assertTrue(state.evalPCE.shortClassification.contains("Macrocefalia"))
    }

    @Test
    fun `caso G diagnostico nutricional completo con PC vacio mantiene COMPLETE y no incluye PC-E en outOfRangeIndicators`() {
        val vm = CredEdiViewModel()
        vm.selectSexo(SexoBiologico.NINO)
        vm.updateFechaNacimiento("15/06/2022")
        vm.updateFechaEvaluacion("15/06/2026") // 48m 0d
        vm.updatePesoInput("16.3")
        vm.updateLongitudInput("103.3")
        vm.updatePerimetroCefalicoInput("")

        val state = vm.uiState.value
        assertTrue(state.evalPE.isReferenceAvailable)
        assertTrue(state.evalTE.isReferenceAvailable)
        assertTrue(state.evalPT.isReferenceAvailable)
        assertTrue(state.evalIMCE.isReferenceAvailable)
        assertFalse(state.evalPCE.isReferenceAvailable)

        val global = state.globalDiagnosis
        assertTrue(global.isAvailable)
        assertEquals(GlobalDiagnosisCompleteness.COMPLETE, global.completeness)
        assertFalse(global.isPartial)
        assertTrue(global.outOfRangeIndicators.isEmpty())
        assertFalse(global.outOfRangeIndicators.contains("PC/E"))
        assertEquals("Eutrófico / Crecimiento Adecuado", global.title)
        assertEquals(ClassificationSeverity.NORMAL, global.severity)
        assertFalse(global.pcAvailable)
        assertEquals("Sin medición", global.pcClassification)
    }

    @Test
    fun `caso H diagnostico nutricional parcial con PC vacio reporta solo P-T en outOfRangeIndicators`() {
        val vm = CredEdiViewModel()
        vm.selectSexo(SexoBiologico.NINO)
        vm.updateFechaNacimiento("15/06/2022")
        vm.updateFechaEvaluacion("15/06/2026") // 48m 0d
        vm.updatePesoInput("17.5")
        vm.updateLongitudInput("110.5") // > 110.0 cm -> P/T fuera de rango
        vm.updatePerimetroCefalicoInput("")

        val state = vm.uiState.value
        assertFalse(state.evalPT.isReferenceAvailable)
        assertFalse(state.evalPCE.isReferenceAvailable)

        val global = state.globalDiagnosis
        assertTrue(global.isAvailable)
        assertEquals(GlobalDiagnosisCompleteness.PARTIAL, global.completeness)
        assertTrue(global.isPartial)
        assertEquals(listOf("P/T"), global.outOfRangeIndicators)
        assertFalse(global.outOfRangeIndicators.contains("PC/E"))
        assertEquals("Sin medición", global.pcClassification)
    }

    @Test
    fun `caso I CurvasOmsScreen en pestana PC-Edad con PC vacio muestra Sin medicion y no dibuja punto ni Fuera de rango`() {
        val vm = CredEdiViewModel()
        vm.selectSexo(SexoBiologico.NINO)
        vm.updateFechaNacimiento("15/06/2022")
        vm.updateFechaEvaluacion("15/06/2026") // 48m 0d
        vm.updatePesoInput("17.5")
        vm.updateLongitudInput("110.5")
        vm.updatePerimetroCefalicoInput("")
        vm.selectCurvaIndicador(CurvaIndicador.PCE)
        vm.selectTab(AppTab.CURVAS_OMS)

        composeRule.setContent { CredEdiApp(viewModel = vm) }
        composeRule.onNodeWithTag("tab_curvas").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("curvas_chart_unavailable_card")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNode(
            hasAnyAncestor(hasTestTag("curvas_chart_unavailable_card")) and
                hasText("Perímetro cefálico no registrado"),
            useUnmergedTree = true
        ).assertExists()
        composeRule.onNode(
            hasAnyAncestor(hasTestTag("curvas_chart_unavailable_card")) and
                hasText("Medición actual: -- (Sin medición)", substring = true),
            useUnmergedTree = true
        ).assertExists()

        composeRule.onNodeWithTag("curvas_cutoff_unavailable_card")
            .performScrollTo()
        composeRule.onNode(
            hasAnyAncestor(hasTestTag("curvas_cutoff_unavailable_card")) and
                hasText("Sin clasificación cefálica (Sin medición)", substring = true),
            useUnmergedTree = true
        ).assertExists()

        // No debe mostrar "Fuera de rango" ni "Microcefalia"
        assertEquals(0, composeRule.onAllNodesWithText("Fuera de rango", substring = true).fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithText("Microcefalia", substring = true).fetchSemanticsNodes().size)
    }

    @Test
    fun `caso J referencia OMS fuera de rango por edad mayor a 60m se distingue de ausencia de medicion`() {
        val vm = CredEdiViewModel()
        vm.selectSexo(SexoBiologico.NINO)
        vm.updateFechaNacimiento("01/02/2021")
        vm.updateFechaEvaluacion("15/06/2026") // 64m 14d (> 60m)
        vm.updatePesoInput("19.2")
        vm.updateLongitudInput("111.0")
        vm.updatePerimetroCefalicoInput("51.2")

        val stateOutAge = vm.uiState.value
        assertEquals(64, stateOutAge.edadEfectivaMeses)
        assertEquals(PerimetroCefalicoCaptureState.REFERENCE_UNAVAILABLE_AGE, stateOutAge.perimetroCefalicoCaptureState)
        assertEquals(PerimetroCefalicoOmsStatus.REFERENCE_UNAVAILABLE, stateOutAge.pcOmsEvaluationStatus)
        assertFalse(stateOutAge.evalPCE.isReferenceAvailable)
        assertEquals("PC/E no disponible por rango de referencia", stateOutAge.evalPCE.fullClassification)
        assertEquals("NO DISPONIBLE POR RANGO DE REFERENCIA", stateOutAge.evalPCE.unavailabilityTitle)
        assertEquals("No disponible", stateOutAge.evalPCE.shortClassification)
        assertEquals("51.2", stateOutAge.evalPCE.patientValueFormatted)

        // Comparado con edad dentro de rango (48m) pero sin medición
        val vmInAgeNoMeas = CredEdiViewModel()
        vmInAgeNoMeas.selectSexo(SexoBiologico.NINO)
        vmInAgeNoMeas.updateFechaNacimiento("15/06/2022")
        vmInAgeNoMeas.updateFechaEvaluacion("15/06/2026") // 48m 0d
        vmInAgeNoMeas.updatePerimetroCefalicoInput("")

        val stateNoMeas = vmInAgeNoMeas.uiState.value
        assertEquals(PerimetroCefalicoCaptureState.EMPTY_UNREGISTERED, stateNoMeas.perimetroCefalicoCaptureState)
        assertEquals(PerimetroCefalicoOmsStatus.UNREGISTERED, stateNoMeas.pcOmsEvaluationStatus)
        assertEquals("Perímetro cefálico no registrado", stateNoMeas.evalPCE.fullClassification)
        assertEquals("Perímetro cefálico no registrado", stateNoMeas.evalPCE.unavailabilityTitle)
        assertEquals("Sin medición", stateNoMeas.evalPCE.shortClassification)
        assertEquals("--", stateNoMeas.evalPCE.patientValueFormatted)
    }

    @Test
    fun `caso K regresion de alteracion real de PC detecta microcefalia y macrocefalia sinteticas reales`() {
        val vm = CredEdiViewModel()
        vm.selectSexo(SexoBiologico.NINO)
        vm.updateFechaNacimiento("15/06/2022")
        vm.updateFechaEvaluacion("15/06/2026") // 48m 0d
        vm.updatePesoInput("16.3")
        vm.updateLongitudInput("103.3")

        // Microcefalia sintética real a los 48 meses (44.0 cm < -2 DE)
        vm.updatePerimetroCefalicoInput("44.0")
        val microState = vm.uiState.value
        assertTrue(microState.hasPerimetroCefalicoIngresado)
        assertTrue(microState.evalPCE.isReferenceAvailable)
        assertTrue(microState.evalPCE.zScore < -2.0)
        assertTrue(microState.evalPCE.shortClassification.contains("Microcefalia"))
        assertEquals(PerimetroCefalicoOmsStatus.ALTERED_OUT_OF_RANGE, microState.pcOmsEvaluationStatus)
        assertFalse(microState.pcEnRangoNormalOms)
        assertTrue(microState.globalDiagnosis.pcClassification.contains("Microcefalia"))

        // Macrocefalia sintética real a los 48 meses (55.5 cm > +2 DE)
        vm.updatePerimetroCefalicoInput("55.5")
        val macroState = vm.uiState.value
        assertTrue(macroState.hasPerimetroCefalicoIngresado)
        assertTrue(macroState.evalPCE.isReferenceAvailable)
        assertTrue(macroState.evalPCE.zScore > 2.0)
        assertTrue(macroState.evalPCE.shortClassification.contains("Macrocefalia"))
        assertEquals(PerimetroCefalicoOmsStatus.ALTERED_OUT_OF_RANGE, macroState.pcOmsEvaluationStatus)
        assertFalse(macroState.pcEnRangoNormalOms)
        assertTrue(macroState.globalDiagnosis.pcClassification.contains("Macrocefalia"))
    }

    @Test
    fun `caso L regresion EDI 15 grupos 270 IDs semaforos fallbacks alertas criticas y exportacion PDF`() {
        assertEquals(15, EdiOfficialCatalog.groups.size)
        val allItems = EdiOfficialCatalog.groups.flatMap { spec ->
            spec.areaItems.values.flatten() + spec.alaItems + spec.aleItems
        } + EdiOfficialCatalog.frbItems +
            EdiOfficialCatalog.allEnItems +
            EdiOfficialCatalog.specialGroup13CoRFallbackItems

        val uniqueIds = allItems.map { it.uniqueId }
        assertEquals(270, uniqueIds.size)
        assertEquals(270, uniqueIds.toSet().size)

        // Verificar semáforo global y exportación PDF en grupo 15 con PC vacío
        val vm = CredEdiViewModel()
        vm.selectSexo(SexoBiologico.NINO)
        vm.updateFechaNacimiento("15/06/2021")
        vm.updateFechaEvaluacion("15/06/2026") // 60m -> G15
        vm.selectEdiGroupFromBank(15)
        vm.updatePesoInput("18.5")
        vm.updateLongitudInput("110.0")
        vm.updatePerimetroCefalicoInput("")

        val spec15 = EdiOfficialCatalog.getGroup(15)
        spec15.areaItems.values.flatten().forEach { vm.recordEdiCurrentItemResponse(it.uniqueId, EdiItemResponse.YES) }
        EdiOfficialCatalog.getApplicableEnItems(15).forEach { vm.recordEdiCurrentItemResponse(it.uniqueId, EdiItemResponse.NO) }
        spec15.alaItems.forEach { vm.recordEdiCurrentItemResponse(it.uniqueId, EdiItemResponse.NO) }
        EdiOfficialCatalog.frbItems.forEach { vm.recordEdiCurrentItemResponse(it.uniqueId, EdiItemResponse.NO) }
        spec15.aleItems.forEach { vm.recordEdiCurrentItemResponse(it.uniqueId, EdiItemResponse.NO) }

        val evalEdi = vm.uiState.value.ediGlobalEvaluation
        assertEquals(EdiGlobalTrafficLight.VERDE, evalEdi.trafficLight)
        assertTrue(
            EdiDevArea.entries.all { area ->
                evalEdi.axis3AreasSummary.areaResults[area]?.status == EdiAreaColorResult.VERDE
            }
        )

        val footer = EdiPdfExporter.buildCredFooterSummary(vm.uiState.value)
        assertFalse(footer.contains("PC:"))
        assertFalse(footer.contains("39.5"))
        assertFalse(footer.contains("Microcefalia"))
        assertFalse(footer.contains("NaN"))

        val pdfResult = EdiPdfExporter.exportarFichaOficialEdiPdf(context(), vm.uiState.value)
        assertTrue(pdfResult.message, pdfResult.success)
        assertNotNull(pdfResult.filePath)
        val pdfFile = File(pdfResult.filePath!!)
        assertTrue(pdfFile.exists())
        assertTrue(pdfFile.length() > 10_000L)
        pdfFile.delete()
    }

    @Test
    fun `caso M regresion de los lotes 1 2 2B 3 y 4 de MEDIO-04`() {
        // Lote 1: Hallazgo #4 propagación de alerta crítica prioritaria en estado INCOMPLETE
        val vmLote1 = CredEdiViewModel()
        vmLote1.updateFechaNacimiento("15/02/2026")
        vmLote1.updateFechaEvaluacion("15/06/2026") // 4m -> G4
        vmLote1.confirmEdiTermWithoutWeeks()
        vmLote1.selectEdiGroupFromBank(4)
        vmLote1.recordEdiCurrentItemResponse("EN_1_1", EdiItemResponse.YES) // EN alterado pero faltan áreas
        val evalIncompleteCrit = vmLote1.uiState.value.ediGlobalEvaluation
        assertEquals(EdiGlobalTrafficLight.INCOMPLETE, evalIncompleteCrit.trafficLight)
        assertTrue(evalIncompleteCrit.hasPriorityCriticalWarning)
        assertTrue(evalIncompleteCrit.requiresImmediateClinicalAttentionDespiteIncomplete)

        // Lote 2 & 2B: Protección de rangos OMS y diagnóstico parcial
        val ptOut = WhoGrowthStandards.evaluateIndicator(
            indicador = CurvaIndicador.PT,
            sexo = SexoBiologico.NINO,
            edadMeses = 48,
            pesoKg = 17.5,
            longitudCm = 110.5,
            perimetroCefalicoCm = 50.0
        )
        assertFalse(ptOut.isReferenceAvailable)
        assertTrue(ptOut.zScore.isNaN())

        // Lote 3: Hallazgos #1 (EN_1_2), #2 (G4_SO_4_2), #3 (G11_LE_11_2)
        val en12 = EdiOfficialCatalog.allEnItems.firstOrNull { it.uniqueId == "EN_1_2" }
        assertNotNull(en12)
        assertTrue(en12!!.clinicalInstruction.contains("mientras se cierra en lado sano."))
        val so42 = EdiOfficialCatalog.getGroup(4).areaItems[EdiDevArea.SO]?.firstOrNull { it.uniqueId == "G4_SO_4_2" }
        assertNotNull(so42)
        assertTrue(so42!!.clinicalInstruction.contains("¿Recurre el niño a estrategias para impedir que el adulto se aleje?"))
        val le112 = EdiOfficialCatalog.getGroup(11).areaItems[EdiDevArea.LE]?.firstOrNull { it.uniqueId == "G11_LE_11_2" }
        assertNotNull(le112)
        assertTrue(le112!!.clinicalInstruction.contains("Asegúrese que no se refiera a sí mismo en tercera persona"))

        // Lote 4: Hallazgos #5 (G4_MG_4_2), #6 (G5_ALE_5_5), #8 (G8_MG_8_2), #9 (G8_SO_8_3)
        val mg42 = EdiOfficialCatalog.findItemByUniqueId("G4_MG_4_2")!!
        assertTrue(mg42.protocolRequiredMaterials.isEmpty())
        val ale55 = EdiOfficialCatalog.findItemByUniqueId("G5_ALE_5_5")!!
        assertTrue(ale55.protocolRequiredMaterials.isEmpty())
        val mg82 = EdiOfficialCatalog.findItemByUniqueId("G8_MG_8_2")!!
        assertEquals(listOf("Tapete o silla pequeña"), mg82.protocolRequiredMaterials)
        val so83 = EdiOfficialCatalog.findItemByUniqueId("G8_SO_8_3")!!
        assertTrue(so83.protocolRequiredMaterials.isEmpty())
    }
}
