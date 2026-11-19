package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.example.model.ClassificationSeverity
import com.example.model.CurvaIndicador
import com.example.model.EdiGlobalTrafficLight
import com.example.model.EdiItemResponse
import com.example.model.EdiOfficialCatalog
import com.example.model.GlobalDiagnosisCompleteness
import com.example.model.SexoBiologico
import com.example.model.WhoGrowthStandards
import com.example.util.EdiPdfExporter
import com.example.viewmodel.CredEdiViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
 * MEDIO-04 — LOTE 2B:
 * Corrección complementaria de seguridad del diagnóstico antropométrico:
 * - Distinción entre diagnóstico global COMPLETE, PARTIAL y UNAVAILABLE cuando falta P/T.
 * - Eliminación de sustitución silenciosa de ptZScore por 0.0 en velocidad ponderal.
 * - Rechazo explícito de entradas no finitas (NaN, +Infinity, -Infinity) en computeZScore y evaluateIndicator.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class EdiMedio04Lote2BAnthropometrySafetyTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun context(): Context = ApplicationProvider.getApplicationContext()

    private fun completeGroup15AsGreen(vm: CredEdiViewModel) {
        val spec = EdiOfficialCatalog.getGroup(15)
        spec.areaItems.values.flatten().forEach { vm.recordEdiCurrentItemResponse(it.uniqueId, EdiItemResponse.YES) }
        EdiOfficialCatalog.getApplicableEnItems(15).forEach { vm.recordEdiCurrentItemResponse(it.uniqueId, EdiItemResponse.NO) }
        spec.alaItems.forEach { vm.recordEdiCurrentItemResponse(it.uniqueId, EdiItemResponse.NO) }
        EdiOfficialCatalog.frbItems.forEach { vm.recordEdiCurrentItemResponse(it.uniqueId, EdiItemResponse.NO) }
        spec.aleItems.forEach { vm.recordEdiCurrentItemResponse(it.uniqueId, EdiItemResponse.NO) }
    }

    @Test
    fun `caso A normalidad completa con todas las referencias OMS disponibles`() {
        val vm = CredEdiViewModel()
        vm.selectSexo(SexoBiologico.NINO)
        vm.updateFechaNacimiento("15/06/2022")
        vm.updateFechaEvaluacion("15/06/2026") // 48m 0d
        vm.updatePesoInput("16.30")
        vm.updateLongitudInput("103.3")
        vm.updatePerimetroCefalicoInput("50.2")

        val state = vm.uiState.value
        assertTrue(state.evalPE.isReferenceAvailable)
        assertTrue(state.evalTE.isReferenceAvailable)
        assertTrue(state.evalPT.isReferenceAvailable)
        assertTrue(state.evalIMCE.isReferenceAvailable)
        assertTrue(state.evalPCE.isReferenceAvailable)

        val global = state.globalDiagnosis
        assertTrue(global.isAvailable)
        assertEquals(GlobalDiagnosisCompleteness.COMPLETE, global.completeness)
        assertFalse(global.isPartial)
        assertTrue(global.outOfRangeIndicators.isEmpty())
        assertEquals("Eutrófico / Crecimiento Adecuado", global.title)
        assertEquals("Sin desnutrición aguda ni crónica · Canal de crecimiento armónico", global.description)
        assertEquals(ClassificationSeverity.NORMAL, global.severity)
        assertEquals("Ganancia Ponderal Óptima", global.gananciaStatusTitle)
        assertEquals(ClassificationSeverity.NORMAL, global.gananciaSeverity)
        assertEquals("Catch-Up\nÓptimo", global.catchUpBadgeText)

        composeRule.setContent { CredEdiApp(viewModel = vm) }
        composeRule.onNodeWithTag("tab_diagnostico").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("diagnostico_global_title")
            .performScrollTo()
            .assertTextContains("Eutrófico / Crecimiento Adecuado")
        composeRule.onNodeWithTag("diagnostico_global_description")
            .performScrollTo()
            .assertTextContains("Canal de desarrollo armónico y proporcional")
        composeRule.onNodeWithTag("diagnostico_global_check_icon")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag("diagnostico_velocity_badge")
            .performScrollTo()
            .assertTextContains("Catch-Up\nÓptimo")
    }

    @Test
    fun `caso B P-T no disponible con indicadores restantes normales no emite normalidad global ni check verde`() {
        val vm = CredEdiViewModel()
        vm.selectSexo(SexoBiologico.NINO)
        vm.updateFechaNacimiento("15/06/2022")
        vm.updateFechaEvaluacion("15/06/2026") // 48m 0d
        vm.updatePesoInput("17.50")
        vm.updateLongitudInput("110.5") // > 110.0 cm => P/T no disponible
        vm.updatePerimetroCefalicoInput("50.2")

        val state = vm.uiState.value
        assertFalse(state.evalPT.isReferenceAvailable)
        assertTrue(state.evalPT.zScore.isNaN())
        assertTrue(state.evalPE.isReferenceAvailable)
        assertEquals("Eutrófico", state.evalPE.shortClassification)
        assertTrue(state.evalTE.isReferenceAvailable)
        assertEquals("Adecuada", state.evalTE.shortClassification)
        assertTrue(state.evalIMCE.isReferenceAvailable)
        assertEquals("Normal", state.evalIMCE.shortClassification)
        assertTrue(state.evalPCE.isReferenceAvailable)

        val global = state.globalDiagnosis
        assertTrue(global.isAvailable)
        assertEquals(GlobalDiagnosisCompleteness.PARTIAL, global.completeness)
        assertTrue(global.isPartial)
        assertEquals(listOf("P/T"), global.outOfRangeIndicators)
        assertEquals(ClassificationSeverity.WARNING, global.severity)
        assertFalse(global.title.contains("Eutrófico / Crecimiento Adecuado"))
        assertTrue(global.title.contains("Parcial", ignoreCase = true))
        assertTrue(global.title.contains("P/T"))
        assertFalse(global.description.contains("Sin desnutrición aguda"))
        assertFalse(global.description.contains("Canal de crecimiento armónico"))
        assertTrue(global.description.contains("P/T no disponible"))
        assertEquals("Normocéfalo", global.pcClassification)
        assertEquals(110.5, state.longitudCm, 0.01)
        assertEquals(48, state.edadEfectivaMeses)

        composeRule.setContent { CredEdiApp(viewModel = vm) }
        composeRule.onNodeWithTag("tab_diagnostico").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("diagnostico_global_title")
            .performScrollTo()
            .assertTextContains("Valoración Nutricional Parcial (sin P/T)")
        composeRule.onNodeWithTag("diagnostico_global_description")
            .performScrollTo()
            .assertTextContains("no puede descartarse desnutrición aguda", substring = true)
        composeRule.onNodeWithTag("diagnostico_global_warning_badge")
            .assertIsDisplayed()
        assertEquals(0, composeRule.onAllNodesWithTag("diagnostico_global_check_icon").fetchSemanticsNodes().size)
        composeRule.onNodeWithTag("diagnostico_partial_out_of_range_notice")
            .performScrollTo()
            .assertTextContains("P/T", substring = true)
    }

    @Test
    fun `caso C P-T no disponible con otro indicador alterado conserva alerta patologica y valoracion parcial`() {
        // C1: T/E Severa (< 45.0 cm -> P/T no disponible, T/E < -3 DE), P/E normal
        val vmStunting = CredEdiViewModel()
        vmStunting.selectSexo(SexoBiologico.NINO)
        vmStunting.updateFechaNacimiento("15/06/2022")
        vmStunting.updateFechaEvaluacion("15/06/2026") // 48m 0d
        vmStunting.updatePesoInput("15.50")
        vmStunting.updateLongitudInput("44.5") // < 45.0 cm
        vmStunting.updatePerimetroCefalicoInput("50.2")

        val stC1 = vmStunting.uiState.value
        assertFalse(stC1.evalPT.isReferenceAvailable)
        assertTrue(stC1.evalTE.isReferenceAvailable)
        assertEquals(ClassificationSeverity.SEVERE, stC1.evalTE.severity)
        assertEquals(GlobalDiagnosisCompleteness.PARTIAL, stC1.globalDiagnosis.completeness)
        assertEquals(ClassificationSeverity.SEVERE, stC1.globalDiagnosis.severity)
        assertTrue(stC1.globalDiagnosis.title.contains("Retraso Longitudinal"))
        assertTrue(stC1.globalDiagnosis.title.contains("parcial", ignoreCase = true))
        assertTrue(stC1.globalDiagnosis.description.contains("P/T no disponible"))

        // C2: P/E severo (< -3 DE) + talla 112.5 cm (> 110.0 cm -> P/T no disponible)
        val vmSeverePe = CredEdiViewModel()
        vmSeverePe.selectSexo(SexoBiologico.NINO)
        vmSeverePe.updateFechaNacimiento("15/06/2022")
        vmSeverePe.updateFechaEvaluacion("15/06/2026") // 48m 0d
        vmSeverePe.updatePesoInput("11.00") // < 11.2 kg (-3 DE a los 48m)
        vmSeverePe.updateLongitudInput("112.5") // > 110.0 cm
        vmSeverePe.updatePerimetroCefalicoInput("50.2")

        val stC2 = vmSeverePe.uiState.value
        assertFalse(stC2.evalPT.isReferenceAvailable)
        assertEquals(ClassificationSeverity.SEVERE, stC2.evalPE.severity)
        assertEquals(ClassificationSeverity.WARNING, stC2.evalTE.severity)
        assertEquals(GlobalDiagnosisCompleteness.PARTIAL, stC2.globalDiagnosis.completeness)
        assertEquals(ClassificationSeverity.SEVERE, stC2.globalDiagnosis.severity)
        assertTrue(stC2.globalDiagnosis.title.contains("Desnutrición Severa Detectada"))
        assertTrue(stC2.globalDiagnosis.description.contains("P/T no disponible"))

        // C3: T/E alta (> +2 DE) con P/E e IMC normales y P/T no disponible (no debe ocultarse ni decir Eutrófico)
        val vmHighTe = CredEdiViewModel()
        vmHighTe.selectSexo(SexoBiologico.NINO)
        vmHighTe.updateFechaNacimiento("15/06/2022")
        vmHighTe.updateFechaEvaluacion("15/06/2026") // 48m 0d
        vmHighTe.updatePesoInput("17.50")
        vmHighTe.updateLongitudInput("112.5") // T/E > +2 DE (111.7..115.9)
        vmHighTe.updatePerimetroCefalicoInput("50.2")
        val stC3 = vmHighTe.uiState.value
        assertEquals(GlobalDiagnosisCompleteness.PARTIAL, stC3.globalDiagnosis.completeness)
        assertEquals(ClassificationSeverity.WARNING, stC3.globalDiagnosis.severity)
        assertTrue(stC3.globalDiagnosis.title.contains("Talla Alta"))
        assertFalse(stC3.globalDiagnosis.title.contains("Eutrófico"))

        composeRule.setContent { CredEdiApp(viewModel = vmSeverePe) }
        composeRule.onNodeWithTag("tab_diagnostico").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("diagnostico_global_title")
            .performScrollTo()
            .assertTextContains("Desnutrición Severa Detectada", substring = true)
        composeRule.onNodeWithTag("diagnostico_global_warning_badge")
            .performScrollTo()
            .assertIsDisplayed()
        assertEquals(0, composeRule.onAllNodesWithTag("diagnostico_global_check_icon").fetchSemanticsNodes().size)
    }

    @Test
    fun `caso D frontera inferior de talla 44_9 vs 45_0 cm`() {
        val pe0 = WhoGrowthStandards.evaluateIndicator(CurvaIndicador.PE, SexoBiologico.NINO, 0, 3.30, 44.9, 16.4)
        val te0 = WhoGrowthStandards.evaluateIndicator(CurvaIndicador.TE, SexoBiologico.NINO, 0, 3.30, 44.9, 16.4)
        val imc0 = WhoGrowthStandards.evaluateIndicator(CurvaIndicador.IMCE, SexoBiologico.NINO, 0, 3.30, 44.9, 16.4)

        val ptBelow = WhoGrowthStandards.evaluateIndicator(CurvaIndicador.PT, SexoBiologico.NINO, 0, 3.30, 44.9, 16.4)
        assertFalse(ptBelow.isReferenceAvailable)
        assertTrue(ptBelow.zScore.isNaN())

        val globalBelow = WhoGrowthStandards.evaluateGlobalDiagnosis(
            sexo = SexoBiologico.NINO,
            edadMeses = 0,
            pesoKg = 3.30,
            longitudCm = 44.9,
            perimetroCefalicoCm = 34.5,
            peEval = pe0,
            teEval = te0,
            ptEval = ptBelow,
            imcEval = imc0
        )
        assertEquals(GlobalDiagnosisCompleteness.PARTIAL, globalBelow.completeness)

        val ptExact = WhoGrowthStandards.evaluateIndicator(CurvaIndicador.PT, SexoBiologico.NINO, 0, 2.40, 45.0, 11.85)
        assertTrue(ptExact.isReferenceAvailable)
        assertTrue(ptExact.zScore.isFinite())
    }

    @Test
    fun `caso E frontera superior de talla 110_0 vs 110_1 110_5 y 115_0 cm`() {
        val peso = 17.50
        val pc = 50.2
        val pe = WhoGrowthStandards.evaluateIndicator(CurvaIndicador.PE, SexoBiologico.NINO, 48, peso, 110.0, 14.46)
        val te110 = WhoGrowthStandards.evaluateIndicator(CurvaIndicador.TE, SexoBiologico.NINO, 48, peso, 110.0, 14.46)
        val imc110 = WhoGrowthStandards.evaluateIndicator(CurvaIndicador.IMCE, SexoBiologico.NINO, 48, peso, 110.0, 14.46)
        val pt110 = WhoGrowthStandards.evaluateIndicator(CurvaIndicador.PT, SexoBiologico.NINO, 48, peso, 110.0, 14.46)

        assertTrue(pt110.isReferenceAvailable)
        assertTrue(pt110.zScore.isFinite())
        val global110 = WhoGrowthStandards.evaluateGlobalDiagnosis(
            sexo = SexoBiologico.NINO,
            edadMeses = 48,
            pesoKg = peso,
            longitudCm = 110.0,
            perimetroCefalicoCm = pc,
            peEval = pe,
            teEval = te110,
            ptEval = pt110,
            imcEval = imc110
        )
        assertEquals(GlobalDiagnosisCompleteness.COMPLETE, global110.completeness)
        assertEquals("Eutrófico / Crecimiento Adecuado", global110.title)

        for (outLen in listOf(110.1, 110.5, 115.0)) {
            val imc = peso / ((outLen / 100.0) * (outLen / 100.0))
            val teOut = WhoGrowthStandards.evaluateIndicator(CurvaIndicador.TE, SexoBiologico.NINO, 48, peso, outLen, imc)
            val imcOut = WhoGrowthStandards.evaluateIndicator(CurvaIndicador.IMCE, SexoBiologico.NINO, 48, peso, outLen, imc)
            val ptOut = WhoGrowthStandards.evaluateIndicator(CurvaIndicador.PT, SexoBiologico.NINO, 48, peso, outLen, imc)

            assertFalse("Longitud $outLen cm debe invalidar P/T", ptOut.isReferenceAvailable)
            assertTrue(ptOut.zScore.isNaN())

            val globalOut = WhoGrowthStandards.evaluateGlobalDiagnosis(
                sexo = SexoBiologico.NINO,
                edadMeses = 48,
                pesoKg = peso,
                longitudCm = outLen,
                perimetroCefalicoCm = pc,
                peEval = pe,
                teEval = teOut,
                ptEval = ptOut,
                imcEval = imcOut
            )
            assertEquals(GlobalDiagnosisCompleteness.PARTIAL, globalOut.completeness)
            assertFalse(globalOut.title.contains("Eutrófico / Crecimiento Adecuado"))
            assertFalse(globalOut.description.contains("Sin desnutrición aguda"))
        }
    }

    @Test
    fun `caso F edad fuera de referencia implementada sin recorte silencioso a 60m`() {
        val exact60 = WhoGrowthStandards.evaluateIndicator(CurvaIndicador.PE, SexoBiologico.NINA, 60, 18.2, 108.0, 15.6, edadDias = 0)
        assertTrue(exact60.isReferenceAvailable)
        assertEquals("5a 0m 0d", exact60.edadAMD)

        for ((meses, dias) in listOf(60 to 1, 61 to 0, 71 to 29)) {
            val pe = WhoGrowthStandards.evaluateIndicator(CurvaIndicador.PE, SexoBiologico.NINA, meses, 21.0, 115.0, 15.9, edadDias = dias)
            val te = WhoGrowthStandards.evaluateIndicator(CurvaIndicador.TE, SexoBiologico.NINA, meses, 21.0, 115.0, 15.9, edadDias = dias)
            val pt = WhoGrowthStandards.evaluateIndicator(CurvaIndicador.PT, SexoBiologico.NINA, meses, 21.0, 115.0, 15.9, edadDias = dias)
            val imc = WhoGrowthStandards.evaluateIndicator(CurvaIndicador.IMCE, SexoBiologico.NINA, meses, 21.0, 115.0, 15.9, edadDias = dias)

            assertFalse(pe.isReferenceAvailable)
            assertFalse(pt.isReferenceAvailable)
            assertEquals("${meses / 12}a ${meses % 12}m ${dias}d", pe.edadAMD)
            val global = WhoGrowthStandards.evaluateGlobalDiagnosis(
                sexo = SexoBiologico.NINA,
                edadMeses = meses,
                edadDias = dias,
                pesoKg = 21.0,
                longitudCm = 115.0,
                perimetroCefalicoCm = 51.0,
                peEval = pe,
                teEval = te,
                ptEval = pt,
                imcEval = imc
            )
            assertFalse(global.isAvailable)
            assertEquals(GlobalDiagnosisCompleteness.UNAVAILABLE, global.completeness)
            assertTrue(global.zGlobal.isNaN())
        }
    }

    @Test
    fun `caso G entradas no finitas NaN e Infinity rechazadas explicitamente y nunca devuelven 0_0`() {
        val sampleRef = WhoGrowthStandards.RefPoint(
            x = 48f,
            m3Neg = 11.2f,
            m2Neg = 12.7f,
            m1Neg = 14.4f,
            median = 16.3f,
            p1Pos = 18.6f,
            p2Pos = 21.2f,
            p3Pos = 24.2f
        )
        // Invocación directa de computeZScore
        assertEquals(0.0, WhoGrowthStandards.computeZScore(16.3, sampleRef), 0.0001)
        val nonFiniteInputs = listOf(Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY)
        for (bad in nonFiniteInputs) {
            val z = WhoGrowthStandards.computeZScore(bad, sampleRef)
            assertTrue("computeZScore($bad) debe ser NaN y nunca 0.0", z.isNaN())
            assertFalse(z == 0.0)
        }

        // Invocación vía evaluateIndicator para los 5 indicadores en edad válida 48m
        for (bad in nonFiniteInputs) {
            val peBad = WhoGrowthStandards.evaluateIndicator(CurvaIndicador.PE, SexoBiologico.NINO, 48, bad, 103.3, 15.0, 50.2)
            val teBad = WhoGrowthStandards.evaluateIndicator(CurvaIndicador.TE, SexoBiologico.NINO, 48, 16.3, bad, 15.0, 50.2)
            val ptBadW = WhoGrowthStandards.evaluateIndicator(CurvaIndicador.PT, SexoBiologico.NINO, 48, bad, 103.3, 15.0, 50.2)
            val ptBadH = WhoGrowthStandards.evaluateIndicator(CurvaIndicador.PT, SexoBiologico.NINO, 48, 16.3, bad, 15.0, 50.2)
            val imcBad = WhoGrowthStandards.evaluateIndicator(CurvaIndicador.IMCE, SexoBiologico.NINO, 48, 16.3, 103.3, bad, 50.2)
            val pcBad = WhoGrowthStandards.evaluateIndicator(CurvaIndicador.PCE, SexoBiologico.NINO, 48, 16.3, 103.3, 15.0, bad)

            listOf(peBad, teBad, ptBadW, ptBadH, imcBad, pcBad).forEach { eval ->
                assertFalse("${eval.indicador} con $bad no debe estar disponible", eval.isReferenceAvailable)
                assertTrue("${eval.indicador} con $bad debe tener zScore NaN", eval.zScore.isNaN())
            }
        }

        // evaluateGlobalDiagnosis con PC no finito debe marcar PC/E no disponible sin devolver 0.0
        val peOk = WhoGrowthStandards.evaluateIndicator(CurvaIndicador.PE, SexoBiologico.NINO, 48, 16.3, 103.3, 15.3)
        val teOk = WhoGrowthStandards.evaluateIndicator(CurvaIndicador.TE, SexoBiologico.NINO, 48, 16.3, 103.3, 15.3)
        val ptOk = WhoGrowthStandards.evaluateIndicator(CurvaIndicador.PT, SexoBiologico.NINO, 48, 16.3, 103.3, 15.3)
        val imcOk = WhoGrowthStandards.evaluateIndicator(CurvaIndicador.IMCE, SexoBiologico.NINO, 48, 16.3, 103.3, 15.3)
        val globalBadPc = WhoGrowthStandards.evaluateGlobalDiagnosis(
            sexo = SexoBiologico.NINO,
            edadMeses = 48,
            pesoKg = 16.3,
            longitudCm = 103.3,
            perimetroCefalicoCm = Double.NaN,
            peEval = peOk,
            teEval = teOk,
            ptEval = ptOk,
            imcEval = imcOk
        )
        assertFalse(globalBadPc.pcAvailable)
        assertTrue(globalBadPc.pcZScore.isNaN())
        assertTrue(globalBadPc.outOfRangeIndicators.contains("PC/E"))
    }

    @Test
    fun `caso H velocidad ponderal con P-T indisponible no concluye Ganancia Ponderal Optima ni Catch-Up Optimo`() {
        val vm = CredEdiViewModel()
        vm.selectSexo(SexoBiologico.NINO)
        vm.updateFechaNacimiento("15/06/2022")
        vm.updateFechaEvaluacion("15/06/2026") // 48m 0d
        vm.updatePesoInput("17.50")
        vm.updateLongitudInput("110.5") // > 110.0 cm => P/T no disponible
        vm.updatePerimetroCefalicoInput("50.2")

        val state = vm.uiState.value
        val global = state.globalDiagnosis
        assertFalse(state.evalPT.isReferenceAvailable)
        assertTrue(global.gananciaGramosMes >= global.minimoGramosMesEsperado)
        assertFalse(global.gananciaStatusTitle == "Ganancia Ponderal Óptima")
        assertFalse(global.catchUpBadgeText == "Catch-Up\nÓptimo")
        assertEquals(ClassificationSeverity.WARNING, global.gananciaSeverity)
        assertEquals("Velocidad Estimada Parcial (sin P/T)", global.gananciaStatusTitle)
        assertEquals("Parcial\nsin P/T", global.catchUpBadgeText)

        composeRule.setContent { CredEdiApp(viewModel = vm) }
        composeRule.onNodeWithTag("tab_diagnostico").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("diagnostico_velocity_badge")
            .performScrollTo()
            .assertTextContains("Parcial\nsin P/T")
    }

    @Test
    fun `caso I regresion EDI completa sin cambios en motores catalogo ni alertas Lote 1`() {
        assertEquals(15, EdiOfficialCatalog.groups.size)
        val allIds = EdiOfficialCatalog.groups.flatMap { spec ->
            spec.areaItems.values.flatten().map { it.uniqueId } +
                spec.alaItems.map { it.uniqueId } +
                spec.aleItems.map { it.uniqueId }
        } + EdiOfficialCatalog.frbItems.map { it.uniqueId } +
            EdiOfficialCatalog.allEnItems.map { it.uniqueId } +
            EdiOfficialCatalog.specialGroup13CoRFallbackItems.map { it.uniqueId }
        assertEquals(270, allIds.toSet().size)

        // G15 operativo con EN 1.3 NO APLICA e independencia de P/T fuera de rango (48m, 110.5cm) y edad >60m
        val vm = CredEdiViewModel()
        vm.updateFechaNacimiento("17/06/2020")
        vm.updateFechaEvaluacion("15/06/2026") // 71m 29d (G15)
        vm.updatePesoInput("22.00")
        vm.updateLongitudInput("115.0")
        completeGroup15AsGreen(vm)

        val greenState = vm.uiState.value
        assertEquals(15, greenState.ediEffectiveGroupNumber)
        assertFalse(greenState.ediGlobalEvaluation.en13AppliesToGroup)
        assertEquals(EdiGlobalTrafficLight.VERDE, greenState.ediGlobalEvaluation.trafficLight)
        assertTrue(greenState.ediGlobalEvaluation.isDefinitive)

        val pdfResult = EdiPdfExporter.exportarFichaOficialEdiPdf(context(), greenState)
        assertTrue(pdfResult.message, pdfResult.success)
        assertNotNull(pdfResult.filePath)
        assertTrue(File(pdfResult.filePath!!).length() > 10_000L)

        // Regresión Lote 1: evaluación INCOMPLETE conserva requiresImmediateClinicalAttentionDespiteIncomplete = true
        val vmIncompleteRed = CredEdiViewModel()
        vmIncompleteRed.updateFechaNacimiento("15/06/2022")
        vmIncompleteRed.updateFechaEvaluacion("15/06/2026") // 48m 0d
        vmIncompleteRed.updatePesoInput("17.50")
        vmIncompleteRed.updateLongitudInput("110.5") // P/T no disponible
        val alaFirst = EdiOfficialCatalog.getGroup(vmIncompleteRed.uiState.value.ediEffectiveGroupNumber).alaItems.first()
        vmIncompleteRed.recordEdiCurrentItemResponse(alaFirst.uniqueId, EdiItemResponse.YES)
        val incompleteEval = vmIncompleteRed.uiState.value.ediGlobalEvaluation
        assertEquals(EdiGlobalTrafficLight.INCOMPLETE, incompleteEval.trafficLight)
        assertTrue(incompleteEval.requiresImmediateClinicalAttentionDespiteIncomplete)
    }
}
