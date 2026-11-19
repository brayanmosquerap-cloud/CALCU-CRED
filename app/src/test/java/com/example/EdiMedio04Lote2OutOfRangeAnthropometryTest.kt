package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.example.model.ClassificationSeverity
import com.example.model.ClinicalAgeCalculator
import com.example.model.CredEdiUiState
import com.example.model.CurvaIndicador
import com.example.model.EdiAreaColorResult
import com.example.model.EdiAreaFallbackEngine
import com.example.model.EdiDevArea
import com.example.model.EdiGlobalTrafficLight
import com.example.model.EdiItemResponse
import com.example.model.EdiOfficialCatalog
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
 * MEDIO-04 — LOTE 2:
 * Protección contra clasificaciones antropométricas producidas mediante recorte
 * silencioso a 60 meses o 45..110 cm cuando no existe referencia OMS implementada.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class EdiMedio04Lote2OutOfRangeAnthropometryTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun context(): Context = ApplicationProvider.getApplicationContext()

    private fun completeGroup15AsGreen(vm: CredEdiViewModel) {
        val spec = EdiOfficialCatalog.getGroup(15)
        val g15SpecificCount = spec.areaItems.values.sumOf { it.size } +
            EdiOfficialCatalog.getApplicableEnItems(15).size +
            spec.alaItems.size +
            spec.aleItems.size
        assertEquals(24, g15SpecificCount)
        spec.areaItems.values.flatten().forEach { item ->
            vm.recordEdiCurrentItemResponse(item.uniqueId, EdiItemResponse.YES)
        }
        EdiOfficialCatalog.getApplicableEnItems(15).forEach { item ->
            vm.recordEdiCurrentItemResponse(item.uniqueId, EdiItemResponse.NO)
        }
        spec.alaItems.forEach { item ->
            vm.recordEdiCurrentItemResponse(item.uniqueId, EdiItemResponse.NO)
        }
        EdiOfficialCatalog.frbItems.forEach { item ->
            vm.recordEdiCurrentItemResponse(item.uniqueId, EdiItemResponse.NO)
        }
        spec.aleItems.forEach { item ->
            vm.recordEdiCurrentItemResponse(item.uniqueId, EdiItemResponse.NO)
        }
    }

    @Test
    fun `01 frontera exacta 59m29d y 60m0d disponibles vs 60m1d 60m29d 61m0d 71m29d y 72m0d no disponibles en ambos sexos`() {
        val sexos = listOf(SexoBiologico.NINO, SexoBiologico.NINA)
        val inRangeAges = listOf(0 to 0, 24 to 0, 59 to 29, 60 to 0)
        val outOfRangeAges = listOf(60 to 1, 60 to 29, 61 to 0, 64 to 14, 71 to 29, 72 to 0)

        for (sexo in sexos) {
            for ((meses, dias) in inRangeAges) {
                for (ind in CurvaIndicador.entries) {
                    val eval = WhoGrowthStandards.evaluateIndicator(
                        indicador = ind,
                        sexo = sexo,
                        edadMeses = meses,
                        edadDias = dias,
                        pesoKg = if (meses < 12) 6.0 else 18.2,
                        longitudCm = if (meses < 12) 62.0 else 108.0,
                        imcCalculado = 15.5,
                        perimetroCefalicoCm = if (meses < 12) 41.0 else 50.5,
                        hasVerifiableAge = true
                    )
                    assertTrue(
                        "Edad ${meses}m ${dias}d debe estar disponible para $ind (${sexo.label})",
                        eval.isReferenceAvailable
                    )
                    assertTrue(eval.zScore.isFinite())
                    assertTrue(eval.cutoffRows.isNotEmpty())
                    assertTrue(eval.curves.isNotEmpty())
                }
            }

            for ((meses, dias) in outOfRangeAges) {
                val expectedAmd = "${meses / 12}a ${meses % 12}m ${dias}d"
                val evals = CurvaIndicador.entries.map { ind ->
                    ind to WhoGrowthStandards.evaluateIndicator(
                        indicador = ind,
                        sexo = sexo,
                        edadMeses = meses,
                        edadDias = dias,
                        pesoKg = 21.50,
                        longitudCm = 114.0,
                        imcCalculado = 16.54,
                        perimetroCefalicoCm = 51.2,
                        hasVerifiableAge = true
                    )
                }
                evals.forEach { (ind, eval) ->
                    assertFalse(
                        "Edad ${meses}m ${dias}d NO debe generar clasificación OMS para $ind (${sexo.label})",
                        eval.isReferenceAvailable
                    )
                    assertEquals("Edad real no debe recortarse a 5a 0m", expectedAmd, eval.edadAMD)
                    assertTrue(eval.zScore.isNaN())
                    assertTrue(eval.medianValue.isNaN())
                    assertTrue(eval.cutoffRows.isEmpty())
                    assertTrue(eval.curves.isEmpty())
                    assertEquals("No disponible", eval.shortClassification)
                    assertEquals("NO DISPONIBLE POR RANGO DE REFERENCIA", eval.unavailabilityTitle)
                }

                val global = WhoGrowthStandards.evaluateGlobalDiagnosis(
                    sexo = sexo,
                    edadMeses = meses,
                    edadDias = dias,
                    pesoKg = 21.50,
                    longitudCm = 114.0,
                    perimetroCefalicoCm = 51.2,
                    peEval = evals[0].second,
                    teEval = evals[1].second,
                    ptEval = evals[2].second,
                    imcEval = evals[3].second,
                    hasVerifiableAge = true
                )
                assertFalse(global.isAvailable)
                assertFalse(global.pcAvailable)
                assertEquals("NO DISPONIBLE POR RANGO DE REFERENCIA", global.title)
                assertTrue(global.zGlobal.isNaN())
                assertTrue(global.pcZScore.isNaN())
            }
        }
    }

    @Test
    fun `02 rango de talla para P-T 45 a 110 cm exacto y sin recorte en 44_9 y 110_1 cm`() {
        val validLengths = listOf(45.0, 75.0, 110.0)
        val invalidLengths = listOf(44.9, 110.1, 125.0)

        for (len in validLengths) {
            val eval = WhoGrowthStandards.evaluateIndicator(
                indicador = CurvaIndicador.PT,
                sexo = SexoBiologico.NINO,
                edadMeses = 48,
                edadDias = 0,
                pesoKg = 17.2,
                longitudCm = len,
                imcCalculado = 15.0,
                hasVerifiableAge = true
            )
            assertTrue("Longitud $len cm en 48m debe estar disponible en P/T", eval.isReferenceAvailable)
            assertTrue(eval.zScore.isFinite())
        }

        for (len in invalidLengths) {
            val ptEval = WhoGrowthStandards.evaluateIndicator(
                indicador = CurvaIndicador.PT,
                sexo = SexoBiologico.NINO,
                edadMeses = 48,
                edadDias = 0,
                pesoKg = 19.5,
                longitudCm = len,
                imcCalculado = 15.6,
                hasVerifiableAge = true
            )
            assertFalse("Longitud $len cm fuera de 45..110 debe marcar P/T no disponible", ptEval.isReferenceAvailable)
            assertTrue(ptEval.zScore.isNaN())
            assertTrue(ptEval.unavailabilityReason.contains("45.0 a 110.0 cm"))

            // P/E, T/E, IMC/E siguen disponibles a los 48m y el dictamen global excluye P/T sin leer NaN
            val pe = WhoGrowthStandards.evaluateIndicator(CurvaIndicador.PE, SexoBiologico.NINO, 48, 19.5, len, 15.6)
            val te = WhoGrowthStandards.evaluateIndicator(CurvaIndicador.TE, SexoBiologico.NINO, 48, 19.5, len, 15.6)
            val imc = WhoGrowthStandards.evaluateIndicator(CurvaIndicador.IMCE, SexoBiologico.NINO, 48, 19.5, len, 15.6)
            assertTrue(pe.isReferenceAvailable && te.isReferenceAvailable && imc.isReferenceAvailable)

            val global = WhoGrowthStandards.evaluateGlobalDiagnosis(
                sexo = SexoBiologico.NINO,
                edadMeses = 48,
                pesoKg = 19.5,
                longitudCm = len,
                perimetroCefalicoCm = 50.2,
                peEval = pe,
                teEval = te,
                ptEval = ptEval,
                imcEval = imc,
                hasVerifiableAge = true
            )
            assertTrue(global.isAvailable)
            assertTrue(global.zGlobal.isFinite())
            assertEquals(listOf("P/T"), global.outOfRangeIndicators)
        }
    }

    @Test
    fun `03 fechas vacias o invertidas impiden diagnostico OMS y reportan edad no verificable`() {
        val blankState = CredEdiUiState(
            fechaNacimiento = "",
            fechaEvaluacion = "15/06/2026",
            pesoInput = "14.0",
            longitudInput = "95.0",
            perimetroCefalicoInput = "48.0"
        )
        assertFalse(blankState.isChronologicalAgeVerifiable)
        assertFalse(blankState.isOmsAgeRangeAvailable)
        assertEquals("Edad no verificable", blankState.grupoEtarioOmsClasificado)
        assertFalse(blankState.evalPE.isReferenceAvailable)
        assertFalse(blankState.evalPT.isReferenceAvailable)
        assertFalse(blankState.globalDiagnosis.isAvailable)

        val invertedState = CredEdiUiState(
            fechaNacimiento = "20/06/2026",
            fechaEvaluacion = "15/06/2026",
            pesoInput = "14.0",
            longitudInput = "95.0"
        )
        assertFalse(invertedState.isChronologicalAgeVerifiable)
        assertFalse(invertedState.evalTE.isReferenceAvailable)
        assertFalse(invertedState.globalDiagnosis.isAvailable)
    }

    @Test
    fun `04 grupo 15 de 64m 14d conserva edad cronologica real y mediciones en Ventana 1 2 y 3 sin clasificacion falsa`() {
        val vm = CredEdiViewModel()
        vm.selectSexo(SexoBiologico.NINA)
        vm.updateFechaNacimiento("01/02/2021")
        vm.updateFechaEvaluacion("15/06/2026") // 64m 14d = 5a 4m 14d (G15)
        vm.updatePesoInput("21.50")
        vm.updateLongitudInput("114.0")
        vm.updatePerimetroCefalicoInput("51.2")

        val state = vm.uiState.value
        assertEquals("5a 4m 14d", state.edadCronologicaAMD)
        assertEquals(64, state.edadEfectivaMeses)
        assertEquals(14, state.edadEfectivaDias)
        assertEquals(15, state.ediEffectiveGroupNumber)
        assertFalse(state.isOmsAgeRangeAvailable)
        assertEquals("Fuera de rango OMS implementado (> 60m)", state.grupoEtarioOmsClasificado)
        assertEquals(16.54, state.imcCalculado, 0.02)
        assertFalse(state.evalPE.isReferenceAvailable)
        assertFalse(state.evalTE.isReferenceAvailable)
        assertFalse(state.evalPT.isReferenceAvailable)
        assertFalse(state.evalIMCE.isReferenceAvailable)
        assertFalse(state.evalPCE.isReferenceAvailable)
        assertFalse(state.globalDiagnosis.isAvailable)
        // pcEnRangoNormalOms no debe degradar falsamente por NaN
        assertTrue(state.pcEnRangoNormalOms)

        composeRule.setContent { CredEdiApp(viewModel = vm) }

        // Ventana 1: Calculadora conserva edad real, IMC matemático real, badge neutro sin P/T falso y sin Z-score falso
        composeRule.onNodeWithTag("edad_cronologica_text")
            .performScrollTo()
            .assertTextContains("5a 4m 14d", substring = true)
        composeRule.onNodeWithTag("calculadora_imc_value")
            .performScrollTo()
            .assertTextContains("16.5", substring = true)
        composeRule.onNodeWithTag("calculadora_pt_badge")
            .performScrollTo()
            .assertTextContains("Fuera de rango P/T", substring = true)
        composeRule.onNodeWithTag("calculadora_imc_zscore_text")
            .performScrollTo()
            .assertTextContains("IMC/E: No disp. (ref. 0-60m)", substring = true)

        // Ventana 2: Curvas OMS muestra banner y placeholders neutros sin crash en cutoffRows ni puntos recortados
        composeRule.onNodeWithTag("tab_curvas").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("curva_edad_calculada_text")
            .performScrollTo()
            .assertTextContains("5a 4m 14d", substring = true)
        composeRule.onNodeWithTag("curvas_out_of_range_banner")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag("curva_summary_headline")
            .performScrollTo()
            .assertTextContains("21.50 kg · NO DISPONIBLE POR RANGO DE REFERENCIA", substring = true)
        composeRule.onNodeWithTag("curvas_chart_unavailable_card")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag("curvas_cutoff_unavailable_card")
            .performScrollTo()
            .assertIsDisplayed()

        // Probar cambio de pestañas T/E, P/T, IMC/E, PC/E en >60m sin excepciones
        for (ind in listOf(CurvaIndicador.TE, CurvaIndicador.PT, CurvaIndicador.IMCE, CurvaIndicador.PCE)) {
            vm.selectCurvaIndicador(ind)
            composeRule.waitForIdle()
            composeRule.onNodeWithTag("curvas_chart_unavailable_card").assertIsDisplayed()
        }

        // Ventana 3: Diagnóstico muestra dictamen y tarjetas neutras conservando edad real y mediciones
        composeRule.onNodeWithTag("tab_diagnostico").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("diagnostico_global_title")
            .performScrollTo()
            .assertTextContains("NO DISPONIBLE POR RANGO DE REFERENCIA", substring = true)
    }

    @Test
    fun `05 diagnostico con 48m y talla 115cm muestra aviso parcial para P-T manteniendo P-E T-E y PC-E`() {
        val vm = CredEdiViewModel()
        vm.updateFechaNacimiento("15/06/2022")
        vm.updateFechaEvaluacion("15/06/2026") // 48m 0d exactos
        vm.updatePesoInput("19.50")
        vm.updateLongitudInput("115.0") // > 110.0 cm
        vm.updatePerimetroCefalicoInput("50.2")

        composeRule.setContent { CredEdiApp(viewModel = vm) }
        composeRule.onNodeWithTag("tab_diagnostico").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("diagnostico_partial_out_of_range_notice")
            .performScrollTo()
            .assertTextContains("P/T", substring = true)
    }

    @Test
    fun `06 exportacion PDF de Grupo 15 mayor de 60m y de 72m conserva mediciones reales y omite clasificaciones OMS recortadas`() {
        val vmG15 = CredEdiViewModel()
        vmG15.selectSexo(SexoBiologico.NINO)
        vmG15.updateFechaNacimiento("01/02/2021")
        vmG15.updateFechaEvaluacion("15/06/2026") // 64m 14d (G15)
        vmG15.updatePesoInput("21.50")
        vmG15.updateLongitudInput("114.0")
        vmG15.updatePerimetroCefalicoInput("51.2")
        completeGroup15AsGreen(vmG15)

        val stateG15 = vmG15.uiState.value
        assertEquals(EdiGlobalTrafficLight.VERDE, stateG15.ediGlobalEvaluation.trafficLight)
        assertTrue(stateG15.ediGlobalEvaluation.isDefinitive)

        val footerG15 = EdiPdfExporter.buildCredFooterSummary(stateG15)
        assertTrue(footerG15.contains("Peso: 21.50 kg"))
        assertTrue(footerG15.contains("Talla: 114.0 cm"))
        assertTrue(footerG15.contains("PC: 51.2 cm"))
        assertTrue(footerG15.contains("Clasif. OMS: No disponible · ref. OMS 0-60m"))
        assertFalse("No debe emitir clasificación de 60m para Peso", footerG15.contains("21.50 kg ("))
        assertFalse("No debe emitir clasificación de 60m para Talla", footerG15.contains("114.0 cm ("))
        assertFalse(footerG15.contains("Eutrófico"))
        assertFalse(footerG15.contains("Adecuada"))
        assertFalse(footerG15.contains("Sobrepeso"))

        val pdfResultG15 = EdiPdfExporter.exportarFichaOficialEdiPdf(context(), stateG15)
        assertTrue(pdfResultG15.message, pdfResultG15.success)
        assertFalse(pdfResultG15.isDraft)
        assertNotNull(pdfResultG15.filePath)
        assertTrue(File(pdfResultG15.filePath!!).length() > 10_000L)

        // También verificar edad >= 72 meses (72m 0d)
        val vm72 = CredEdiViewModel()
        vm72.updateFechaNacimiento("15/06/2020")
        vm72.updateFechaEvaluacion("15/06/2026") // 6a 0m 0d = 72m 0d
        vm72.updatePesoInput("23.40")
        vm72.updateLongitudInput("118.5")
        val state72 = vm72.uiState.value
        assertEquals("6a 0m 0d", state72.edadCronologicaAMD)
        assertFalse(state72.evalPE.isReferenceAvailable)
        val footer72 = EdiPdfExporter.buildCredFooterSummary(state72)
        assertTrue(footer72.contains("Peso: 23.40 kg"))
        assertTrue(footer72.contains("Talla: 118.5 cm"))
        assertTrue(footer72.contains("Clasif. OMS: No disponible · ref. OMS 0-60m"))
        assertFalse(footer72.contains("23.40 kg ("))
        assertFalse(footer72.contains("118.5 cm ("))
    }

    @Test
    fun `07 independencia total de EDI en Grupo 15 mayor de 60m incluyendo EN 1_3 NO APLICA retroceso G15 a G14 y dictamenes definitivos`() {
        // Caso VERDE completo en G15 (71m 29d, extremo superior oficial EDI pero fuera de OMS 0-60m)
        val vmGreen = CredEdiViewModel()
        vmGreen.updateFechaNacimiento("17/06/2020")
        vmGreen.updateFechaEvaluacion("15/06/2026") // 71m 29d
        vmGreen.updatePesoInput("22.10")
        vmGreen.updateLongitudInput("116.0")
        completeGroup15AsGreen(vmGreen)

        val stGreen = vmGreen.uiState.value
        assertEquals(15, stGreen.ediEffectiveGroupNumber)
        assertFalse(stGreen.isOmsAgeRangeAvailable)
        assertFalse(stGreen.ediGlobalEvaluation.en13AppliesToGroup)
        assertEquals(EdiGlobalTrafficLight.VERDE, stGreen.ediGlobalEvaluation.trafficLight)
        assertTrue(stGreen.ediGlobalEvaluation.isDefinitive)

        // Caso AMARILLO por 1 área no lograda + retroceso G15 -> G14 aprobado
        val vmYellow = CredEdiViewModel()
        vmYellow.updateFechaNacimiento("17/06/2020")
        vmYellow.updateFechaEvaluacion("15/06/2026")
        completeGroup15AsGreen(vmYellow)
        val coItemsG15 = EdiOfficialCatalog.getGroup(15).areaItems.getValue(EdiDevArea.CO)
        coItemsG15.forEach { item ->
            vmYellow.recordEdiCurrentItemResponse(item.uniqueId, EdiItemResponse.NO)
        }
        EdiAreaFallbackEngine.getFallbackItemsForArea(15, EdiDevArea.CO).forEach { fb ->
            vmYellow.recordEdiFallbackResponse(fb.fallbackUniqueId, EdiItemResponse.YES)
        }
        val evalYellow = vmYellow.uiState.value.ediGlobalEvaluation
        assertEquals(EdiGlobalTrafficLight.AMARILLO, evalYellow.trafficLight)
        assertTrue(evalYellow.isDefinitive)
        assertEquals(EdiAreaColorResult.AMARILLO, evalYellow.axis3AreasSummary.areaResults.getValue(EdiDevArea.CO).status)

        // Caso ROJO por EN 1.2 alterada (EN 1.3 no aplica en G15)
        val vmRed = CredEdiViewModel()
        vmRed.updateFechaNacimiento("17/06/2020")
        vmRed.updateFechaEvaluacion("15/06/2026")
        completeGroup15AsGreen(vmRed)
        vmRed.recordEdiCurrentItemResponse("EN_1_2", EdiItemResponse.YES)
        val evalRed = vmRed.uiState.value.ediGlobalEvaluation
        assertEquals(EdiGlobalTrafficLight.ROJO, evalRed.trafficLight)
        assertTrue(evalRed.isDefinitive)
        assertFalse(evalRed.en13AppliesToGroup)

        // Verificar que ClinicalAgeCalculator sigue intacto
        val calc = ClinicalAgeCalculator.calculateAges("17/06/2020", "15/06/2026", false, 40, 0)
        assertEquals(71, calc.cronologicaMeses)
        assertEquals(29, calc.cronologicaDias)
    }
}
