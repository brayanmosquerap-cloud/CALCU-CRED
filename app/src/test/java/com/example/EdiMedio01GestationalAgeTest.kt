package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.example.model.ClinicalAgeCalculator
import com.example.model.CredEdiUiState
import com.example.model.EdiBirthMaturityCondition
import com.example.model.EdiClassificationCauseType
import com.example.model.EdiGlobalTrafficLight
import com.example.model.EdiInternalView
import com.example.model.EdiItemResponse
import com.example.model.EdiOfficialCatalog
import com.example.util.EdiPdfExporter
import com.example.viewmodel.CredEdiViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class EdiMedio01GestationalAgeTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun context(): Context = ApplicationProvider.getApplicationContext()

    private fun completeGroup1Green(vm: CredEdiViewModel) {
        val spec = EdiOfficialCatalog.getGroup(1)
        spec.areaItems.values.flatten().forEach { item ->
            vm.recordEdiCurrentItemResponse(item.uniqueId, EdiItemResponse.YES)
        }
        EdiOfficialCatalog.getApplicableEnItems(1).forEach {
            vm.recordEdiCurrentItemResponse(it.uniqueId, EdiItemResponse.NO)
        }
        EdiOfficialCatalog.frbItems.forEach {
            vm.recordEdiCurrentItemResponse(it.uniqueId, EdiItemResponse.NO)
        }
        spec.aleItems.forEach {
            vm.recordEdiCurrentItemResponse(it.uniqueId, EdiItemResponse.NO)
        }
    }

    private fun exportAfterPdf(vm: CredEdiViewModel, fileName: String) {
        val result = EdiPdfExporter.exportarFichaOficialEdiPdf(context(), vm.uiState.value)
        assertTrue("Falló exportación PDF $fileName: ${result.message}", result.message.isNotBlank() && result.success)
        val outDir = File("build/medio01_before_after/after")
        if (!outDir.exists()) outDir.mkdirs()
        File(result.filePath!!).copyTo(File(outDir, fileName), overwrite = true)
    }

    @Test
    fun `01 EG no registrada con egSemanas 32 y egDias 0 no calcula edad corregida ni usa 32 semanas`() {
        val vm = CredEdiViewModel()
        vm.updateFechaNacimiento("05/01/2025")
        vm.updateFechaEvaluacion("20/02/2025")
        completeGroup1Green(vm)

        val state = vm.uiState.value
        assertEquals(32, state.egSemanas)
        assertEquals(0, state.egDias)
        assertFalse(state.esPrematuro)
        assertFalse(state.ediEgRegistrada)
        assertFalse(state.ediHasRegisteredEg)
        assertFalse(state.aplicaEdadCorregida)
        assertFalse(state.usaEdadCorregida)
        assertEquals(0, state.diasPrematuridad)
        assertEquals(0, state.deficitSemanas)
        assertEquals("No aplica edad corregida", state.edadCorregidaAMD)
        assertEquals(EdiBirthMaturityCondition.UNSPECIFIED, state.ediEffectiveMaturityCondition)
        assertEquals("EG no registrada", state.ediGestationalAgeDisplay)

        val p1Lines = EdiPdfExporter.buildPage1GestationalAgeLines(state)
        assertEquals(listOf("EG no registrada", "(Sin confirmación)"), p1Lines)
        val joinedP1 = p1Lines.joinToString(" ")
        assertFalse(joinedP1.contains("32s"))
        assertFalse(joinedP1.contains("A término"))
        assertFalse(joinedP1.contains("≥37"))

        val footer = EdiPdfExporter.buildCredFooterSummary(state)
        assertTrue(footer.contains("EG no registrada"))
        assertFalse(footer.contains("32s"))
        assertFalse(footer.contains("A término"))
        assertFalse(footer.contains("≥37"))

        val p2Detail = EdiPdfExporter.buildPage2GestationalAgeDetail(state)
        assertTrue(p2Detail.contains("EG no registrada"))
        assertFalse(p2Detail.contains("32s"))
        assertFalse(p2Detail.contains("≥37"))

        exportAfterPdf(vm, "after_01_unregistered.pdf")
    }

    @Test
    fun `02 prematuridad activada en menor de 24 meses sin EG exacta muestra EDAD CORREGIDA PENDIENTE y bloquea grupo y semaforo`() {
        val vm = CredEdiViewModel()
        // Niño de 4 meses cronológicos (15/10/2024 -> 15/02/2025)
        vm.updateFechaNacimiento("15/10/2024")
        vm.updateFechaEvaluacion("15/02/2025")
        vm.updatePesoInput("6.20")
        vm.updateLongitudInput("62.0")
        vm.updatePerimetroCefalicoInput("40.5")

        // Forzar grupo inicial en G1 antes de activar prematuridad sin semanas exactas
        vm.selectEdiGroupFromBank(1)
        completeGroup1Green(vm)

        // Activar prematuridad sin registrar semanas exactas
        vm.togglePrematurez(true)

        val state = vm.uiState.value
        assertTrue(state.esPrematuro)
        assertFalse(state.ediEgRegistrada)
        assertFalse(state.ediHasRegisteredEg)
        assertFalse("No debe calcularse edad corregida con las 32 semanas por defecto", state.aplicaEdadCorregida)
        assertEquals(0, state.diasPrematuridad)
        assertEquals(0, state.deficitSemanas)
        assertTrue(state.isCorrectedAgePending)
        assertTrue(state.isAgeDependentEvaluationBlocked)
        assertEquals("EDAD CORREGIDA PENDIENTE", state.edadCorregidaAMD)
        assertEquals("EDAD CORREGIDA PENDIENTE", state.edadEfectivaAMD)
        assertEquals("Pretérmino confirmado — EG exacta no registrada", state.ediGestationalAgeDisplay)
        assertEquals("Pendiente de Edad Corregida", state.proximoControlCorregido.fechaFormateada)
        assertEquals("Pendiente de Edad Corregida", state.proximoControlFecha)

        // Bloquea la selección automática de grupo EDI (permanece en G1, no cambia a G3 ni a G4)
        assertEquals(1, state.ediEffectiveGroupNumber)

        // Bloquea el semáforo definitivo aunque todos los reactivos de G1 estén en VERDE
        val eval = state.ediGlobalEvaluation
        assertEquals(EdiGlobalTrafficLight.INCOMPLETE, eval.trafficLight)
        assertFalse(eval.isDefinitive)
        assertTrue(eval.blockedByAgeDiscrepancy)
        assertTrue(eval.blockingReasons.any { it.contains("EDAD CORREGIDA PENDIENTE") })
        assertTrue(eval.causes.any { it.type == EdiClassificationCauseType.BLOCKED_AGE_DISCREPANCY })

        // Conserva todas las mediciones registradas y respuestas del cuestionario
        assertEquals("6.20", state.pesoInput)
        assertEquals("62.0", state.longitudInput)
        assertEquals("40.5", state.perimetroCefalicoInput)
        assertTrue(state.ediEngineState.activeSession.currentGroupResponses.isNotEmpty())

        // PDF se exporta como BORRADOR sin diagnósticos nutricionales falsos por edad
        val pdfRes = EdiPdfExporter.exportarFichaOficialEdiPdf(context(), state)
        assertTrue(pdfRes.success)
        assertTrue("El PDF debe ser BORRADOR cuando la edad corregida está pendiente", pdfRes.isDraft)
        val footer = EdiPdfExporter.buildCredFooterSummary(state)
        assertTrue(footer.contains("Peso: 6.20 kg"))
        assertTrue(footer.contains("Talla: 62.0 cm"))
        assertTrue(footer.contains("PC: 40.5 cm"))
        assertFalse("No debe imprimir clasificación P/E con edad corregida pendiente", footer.contains("Eutrófico"))

        exportAfterPdf(vm, "after_06_preterm_under24m_pending_eg.pdf")
    }

    @Test
    fun `03 prematuridad activada en nino de 24 meses o mas sin EG exacta no aplica edad corregida y usa edad cronologica sin inventar semanas`() {
        val vm = CredEdiViewModel()
        // Niño de 30 meses cronológicos (15/08/2022 -> 15/02/2025 -> 2a 6m 0d)
        vm.updateFechaNacimiento("15/08/2022")
        vm.updateFechaEvaluacion("15/02/2025")
        vm.togglePrematurez(true)

        val state = vm.uiState.value
        assertTrue(state.esPrematuro)
        assertFalse(state.ediHasRegisteredEg)
        assertEquals(2, state.edadCronologicaAnios)
        assertEquals(30, state.edadCronologicaMeses)
        assertFalse("En >= 24 meses no aplica edad corregida", state.aplicaEdadCorregida)
        assertFalse("En >= 24 meses la edad corregida no está pendiente", state.isCorrectedAgePending)
        assertFalse(state.isAgeDependentEvaluationBlocked)
        assertEquals("No aplica edad corregida", state.edadCorregidaAMD)
        assertEquals("Pretérmino confirmado — EG exacta no registrada", state.ediGestationalAgeDisplay)

        // Usa edad cronológica (30m -> Grupo 11: 25m a 30m 29d) sin inventar 32, 37 ni 40 semanas
        assertEquals(11, state.ediEffectiveGroupNumber)
        val p2Detail = EdiPdfExporter.buildPage2GestationalAgeDetail(state)
        assertTrue(p2Detail.contains("Pretérmino confirmado — EG exacta no registrada"))
        listOf("32", "37", "38", "39", "40").forEach { forbidden ->
            assertFalse("No debe inventar $forbidden semanas en >= 24m", p2Detail.contains("${forbidden}s"))
        }
    }

    @Test
    fun `04 confirmacion explicita de 32 semanas 0 dias sin mover slider calcula 56 dias de prematuridad y aplica edad corregida`() {
        val vm = CredEdiViewModel()
        vm.updateFechaNacimiento("05/01/2025")
        vm.updateFechaEvaluacion("20/03/2025")
        vm.togglePrematurez(true)

        // Antes de confirmar: pendiente
        assertTrue(vm.uiState.value.isCorrectedAgePending)
        assertEquals(0, vm.uiState.value.diasPrematuridad)

        // Confirmar explícitamente las 32 semanas actuales sin mover el slider
        vm.confirmCurrentEgSemanas()

        val state = vm.uiState.value
        assertTrue(state.ediEgRegistrada)
        assertTrue(state.ediHasRegisteredEg)
        assertTrue(state.esPrematuro)
        assertFalse(state.isCorrectedAgePending)
        assertFalse(state.ediHasGestationalContradiction)
        assertEquals(EdiBirthMaturityCondition.CONFIRMED_PRETERM, state.ediEffectiveMaturityCondition)
        assertEquals("Pretérmino · 32s 0d", state.ediGestationalAgeDisplay)
        assertEquals(56, state.diasPrematuridad)
        assertEquals(8, state.deficitSemanas)
        assertTrue(state.aplicaEdadCorregida)
        assertEquals(listOf("32s 0d", "(Pretérmino)"), EdiPdfExporter.buildPage1GestationalAgeLines(state))
        assertTrue(EdiPdfExporter.buildPage2GestationalAgeDetail(state).contains("Pretérmino · 32s 0d"))
    }

    @Test
    fun `05 EG 34s 5d conserva semanas y dias exactos sin redondeo`() {
        val vm = CredEdiViewModel()
        vm.updateFechaNacimiento("14/07/2024")
        vm.updateFechaEvaluacion("20/03/2025")
        vm.updateEgSemanas(34)
        vm.updateEgDias(5)

        val state = vm.uiState.value
        assertTrue(state.ediHasRegisteredEg)
        assertTrue(state.esPrematuro)
        assertFalse(state.ediHasGestationalContradiction)
        assertEquals("Pretérmino · 34s 5d", state.ediGestationalAgeDisplay)
        assertEquals(37, state.diasPrematuridad) // 280 - (34*7 + 5) = 37 días
        assertEquals(listOf("34s 5d", "(Pretérmino)"), EdiPdfExporter.buildPage1GestationalAgeLines(state))
        assertTrue(EdiPdfExporter.buildPage2GestationalAgeDetail(state).contains("Pretérmino · 34s 5d"))
    }

    @Test
    fun `06 frontera 36s 6d calcula exactamente 22 dias de prematuridad sin redondear a 37s`() {
        val vm = CredEdiViewModel()
        vm.updateFechaNacimiento("15/02/2025")
        vm.updateFechaEvaluacion("09/05/2025")
        vm.updateEgSemanas(36)
        vm.updateEgDias(6)

        val state = vm.uiState.value
        assertTrue(state.ediHasRegisteredEg)
        assertTrue("36s 6d debe ser pretérmino", state.esPrematuro)
        assertEquals(EdiBirthMaturityCondition.CONFIRMED_PRETERM, state.ediEffectiveMaturityCondition)
        assertFalse(state.ediHasGestationalContradiction)
        assertEquals("Pretérmino · 36s 6d", state.ediGestationalAgeDisplay)
        assertFalse(state.ediGestationalAgeDisplay.contains("A término"))
        assertEquals(22, state.diasPrematuridad) // 280 - (36*7 + 6) = 22 días
        assertTrue(state.aplicaEdadCorregida)
        assertEquals(listOf("36s 6d", "(Pretérmino)"), EdiPdfExporter.buildPage1GestationalAgeLines(state))

        exportAfterPdf(vm, "after_03_36s6d_preterm.pdf")
    }

    @Test
    fun `07 frontera 37s 0d se trata como nacimiento a termino sin correccion por prematuridad`() {
        val vm = CredEdiViewModel()
        vm.updateFechaNacimiento("15/02/2025")
        vm.updateFechaEvaluacion("09/05/2025")
        vm.updateEgSemanas(37)
        vm.updateEgDias(0)

        val state = vm.uiState.value
        assertTrue(state.ediHasRegisteredEg)
        assertFalse("37s 0d no es pretérmino", state.esPrematuro)
        assertEquals(EdiBirthMaturityCondition.CONFIRMED_TERM, state.ediEffectiveMaturityCondition)
        assertFalse(state.ediHasGestationalContradiction)
        assertEquals("A término · 37s 0d", state.ediGestationalAgeDisplay)
        assertEquals(0, state.diasPrematuridad)
        assertFalse(state.aplicaEdadCorregida)
        assertEquals(listOf("37s 0d", "(A término)"), EdiPdfExporter.buildPage1GestationalAgeLines(state))

        exportAfterPdf(vm, "after_04_37s0d_term.pdf")
    }

    @Test
    fun `08 nacimiento a termino confirmado sin semanas exactas muestra texto explicito sin inventar 37 a 40`() {
        val vm = CredEdiViewModel()
        vm.updateFechaNacimiento("05/01/2025")
        vm.updateFechaEvaluacion("20/02/2025")
        vm.confirmEdiTermWithoutWeeks()
        completeGroup1Green(vm)

        val state = vm.uiState.value
        assertFalse(state.esPrematuro)
        assertFalse(state.ediHasRegisteredEg)
        assertFalse(state.ediHasGestationalContradiction)
        assertFalse(state.aplicaEdadCorregida)
        assertEquals(EdiBirthMaturityCondition.CONFIRMED_TERM, state.ediEffectiveMaturityCondition)
        assertEquals("A término — semanas exactas no registradas", state.ediGestationalAgeDisplay)

        val p1Joined = EdiPdfExporter.buildPage1GestationalAgeLines(state).joinToString(" ")
        assertTrue(p1Joined.contains("A término"))
        assertTrue(p1Joined.contains("semanas exactas"))
        assertTrue(p1Joined.contains("no registradas"))
        listOf("32", "37", "38", "39", "40", "≥37").forEach { forbidden ->
            assertFalse("No debe inventar $forbidden en pág 1", p1Joined.contains(forbidden))
        }

        val p2Detail = EdiPdfExporter.buildPage2GestationalAgeDetail(state)
        assertTrue(p2Detail.contains("A término — semanas exactas no registradas"))
        listOf("32", "37", "38", "39", "40", "≥37").forEach { forbidden ->
            assertFalse("No debe inventar $forbidden en pág 2", p2Detail.contains(forbidden))
        }

        exportAfterPdf(vm, "after_02_term_no_weeks.pdf")
    }

    @Test
    fun `09 contradiccion esPrematuro false con EG menor a 37 bloquea grupo automatico clasificaciones y semaforo definitivo`() {
        val vm = CredEdiViewModel()
        vm.updateFechaNacimiento("05/01/2025")
        vm.updateFechaEvaluacion("20/03/2025")
        vm.selectEdiGroupFromBank(1)
        completeGroup1Green(vm)

        // Registrar 34s 5d y luego desactivar prematuridad (contradicción: esPrematuro == false con EG 34s 5d)
        vm.updateEgSemanas(34)
        vm.updateEgDias(5)
        vm.selectEdiGroupFromBank(1)
        completeGroup1Green(vm)
        vm.togglePrematurez(false)

        val state = vm.uiState.value
        assertFalse(state.esPrematuro)
        assertEquals(34, state.egSemanas)
        assertEquals(5, state.egDias)
        assertTrue(state.ediHasGestationalContradiction)
        assertTrue(state.isCorrectedAgePending)
        assertTrue(state.isAgeDependentEvaluationBlocked)
        assertEquals("EDAD CORREGIDA PENDIENTE", state.edadCorregidaAMD)
        assertEquals("Pendiente de Edad Corregida", state.proximoControlFecha)

        // Cambiar fecha de evaluación mientras persiste la contradicción no debe cambiar el grupo automáticamente
        vm.updateFechaEvaluacion("20/06/2025")
        assertEquals("El grupo automático debe estar bloqueado durante la contradicción", 1, vm.uiState.value.ediEffectiveGroupNumber)

        // El semáforo definitivo está bloqueado
        val eval = vm.uiState.value.ediGlobalEvaluation
        assertEquals(EdiGlobalTrafficLight.INCOMPLETE, eval.trafficLight)
        assertFalse(eval.isDefinitive)
        assertTrue(eval.blockedByAgeDiscrepancy)

        exportAfterPdf(vm, "after_05_contradiction_34s5d_vs_term.pdf")
    }

    @Test
    fun `10 contradiccion esPrematuro true con EG mayor o igual a 37 bloquea grupo automatico clasificaciones y semaforo definitivo`() {
        val vm = CredEdiViewModel()
        vm.updateFechaNacimiento("05/01/2025")
        vm.updateFechaEvaluacion("20/02/2025")
        vm.updateEgSemanas(39)
        vm.updateEgDias(2)
        completeGroup1Green(vm)

        // Antes de contradicción: semáforo definitivo VERDE
        assertEquals(EdiGlobalTrafficLight.VERDE, vm.uiState.value.ediGlobalEvaluation.trafficLight)

        // Activar prematuridad manteniendo 39s 2d (contradicción: esPrematuro == true con EG >= 37)
        vm.togglePrematurez(true)

        val state = vm.uiState.value
        assertTrue(state.esPrematuro)
        assertEquals(39, state.egSemanas)
        assertEquals(2, state.egDias)
        assertTrue(state.ediHasGestationalContradiction)
        assertTrue(state.isCorrectedAgePending)
        assertTrue(state.isAgeDependentEvaluationBlocked)
        assertEquals("EDAD CORREGIDA PENDIENTE", state.edadCorregidaAMD)

        // Cambiar fecha de evaluación no cambia automáticamente de grupo mientras exista contradicción
        vm.updateFechaEvaluacion("20/08/2025")
        assertEquals(1, vm.uiState.value.ediEffectiveGroupNumber)

        // Semáforo definitivo bloqueado
        val eval = vm.uiState.value.ediGlobalEvaluation
        assertEquals(EdiGlobalTrafficLight.INCOMPLETE, eval.trafficLight)
        assertFalse(eval.isDefinitive)
    }

    @Test
    fun `11 introduccion de contradiccion despues de calcular semaforo definitivo lo invalida en UI y PDF`() {
        val vm = CredEdiViewModel()
        vm.updateFechaNacimiento("05/01/2025")
        vm.updateFechaEvaluacion("20/02/2025")
        vm.updateEgSemanas(38)
        vm.updateEgDias(0)
        completeGroup1Green(vm)

        // 1. Semáforo definitivo VERDE y PDF oficial (no borrador)
        assertEquals(EdiGlobalTrafficLight.VERDE, vm.uiState.value.ediGlobalEvaluation.trafficLight)
        assertTrue(vm.uiState.value.ediGlobalEvaluation.isDefinitive)
        val pdfBefore = EdiPdfExporter.exportarFichaOficialEdiPdf(context(), vm.uiState.value)
        assertFalse("Antes de la contradicción el PDF es oficial definitivo", pdfBefore.isDraft)

        // 2. Introducir contradicción (activar prematuridad con 38s 0d registradas)
        vm.togglePrematurez(true)

        // 3. El semáforo definitivo queda invalidado inmediatamente en modelo y PDF
        val evalAfter = vm.uiState.value.ediGlobalEvaluation
        assertEquals(EdiGlobalTrafficLight.INCOMPLETE, evalAfter.trafficLight)
        assertFalse(evalAfter.isDefinitive)
        val pdfAfter = EdiPdfExporter.exportarFichaOficialEdiPdf(context(), vm.uiState.value)
        assertTrue("Al existir contradicción el PDF debe invalidar el dictamen y exportarse como BORRADOR", pdfAfter.isDraft)
        assertTrue(pdfAfter.fileName!!.startsWith("Borrador_EDI_Incompleta_"))

        // 4. En UI (EdiResultsTraceabilityView), el label del semáforo muestra evaluación incompleta
        composeRule.setContent { CredEdiApp(viewModel = vm) }
        composeRule.onNodeWithTag("tab_edi").performClick()
        vm.navigateEdiView(EdiInternalView.RESULTS_TRACEABILITY)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("edi_global_semaphore_label")
            .performScrollTo()
            .assertTextContains("Evaluación incompleta", substring = true)
    }

    @Test
    fun `12 ningun FRB se marca automaticamente con 32 semanas predeterminadas no confirmadas`() {
        val vm = CredEdiViewModel()
        // Estado inicial
        assertTrue(vm.uiState.value.frbItems.none { it.checked })
        assertFalse(vm.uiState.value.frbItems.first { it.id == 1 }.title.contains("32 sem EG"))
        assertTrue(vm.uiState.value.ediEngineState.activeSession.frbResponses.values.all { it == EdiItemResponse.UNANSWERED })

        // Activar prematuridad sin confirmar semanas
        vm.togglePrematurez(true)
        assertFalse("No debe marcarse frbItems[1] con 32 semanas no confirmadas", vm.uiState.value.frbItems.first { it.id == 1 }.checked)
        assertFalse(vm.uiState.value.frbItems.first { it.id == 1 }.title.contains("32 sem EG"))
        assertTrue(vm.uiState.value.ediEngineState.activeSession.frbResponses.values.all { it == EdiItemResponse.UNANSWERED })

        // Resolver contradicción como pretérmino sin semanas exactas tampoco marca 32 semanas ficticias
        vm.updateEgSemanas(39)
        vm.togglePrematurez(true)
        vm.confirmEdiContradictionAsPretermWithoutWeeks()
        assertFalse(vm.uiState.value.frbItems.first { it.id == 1 }.checked)
        assertFalse(vm.uiState.value.frbItems.first { it.id == 1 }.title.contains("32 sem EG"))
        assertEquals("Pretérmino confirmado — EG exacta no registrada", vm.uiState.value.ediGestationalAgeDisplay)
        assertTrue(vm.uiState.value.ediEngineState.activeSession.frbResponses.values.all { it == EdiItemResponse.UNANSWERED })
    }

    @Test
    fun `13 alertas clinicas criticas ALA EN y area ROJO permanecen visibles durante los bloqueos`() {
        val vm = CredEdiViewModel()
        vm.updateFechaNacimiento("05/01/2025")
        vm.updateFechaEvaluacion("10/03/2025")
        // Seleccionar Grupo 2 (donde aplica ALA) y activar bloqueo por prematuridad sin EG exacta
        vm.selectEdiGroupFromBank(2)
        vm.togglePrematurez(true)

        val specG2 = EdiOfficialCatalog.getGroup(2)
        // Registrar una señal de alarma positiva (ALA) y una alteración neurológica (EN)
        val firstAla = specG2.alaItems.first()
        val firstEn = EdiOfficialCatalog.getApplicableEnItems(2).first()
        vm.recordEdiCurrentItemResponse(firstAla.uniqueId, EdiItemResponse.YES)
        vm.recordEdiCurrentItemResponse(firstEn.uniqueId, EdiItemResponse.YES)

        val eval = vm.uiState.value.ediGlobalEvaluation
        // El semáforo definitivo está bloqueado (INCOMPLETE), pero las alertas prioritarias siguen activas y visibles
        assertEquals(EdiGlobalTrafficLight.INCOMPLETE, eval.trafficLight)
        assertFalse(eval.isDefinitive)
        assertTrue("Las alertas críticas prioritarias deben permanecer activas durante el bloqueo", eval.hasPriorityCriticalWarning)
        assertTrue(eval.requiresImmediateClinicalAttentionDespiteIncomplete)
        assertTrue(eval.priorityCriticalAlerts.size >= 2)
    }

    @Test
    fun `14 coherencia completa entre Ventana 1 Ventana 2 Ventana 3 Ventana 4 y el PDF exportado`() {
        val vm = CredEdiViewModel()
        vm.updateFechaNacimiento("15/10/2024")
        vm.updateFechaEvaluacion("15/02/2025")
        vm.updatePesoInput("6.10")
        vm.updateLongitudInput("61.5")
        vm.updatePerimetroCefalicoInput("40.0")

        composeRule.setContent { CredEdiApp(viewModel = vm) }

        // 1. En Ventana 1 (Calculadora): activar prematuridad sin confirmar semanas
        composeRule.onNodeWithTag("prematurez_switch").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("edad_corregida_text").performScrollTo()
            .assertTextContains("EDAD CORREGIDA PENDIENTE", substring = true)

        // 2. En Ventana 2 (Curvas OMS): banner de edad corregida pendiente visible y medición conservada
        composeRule.onNodeWithTag("tab_curvas").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("curvas_pending_corrected_age_banner").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("curva_summary_headline").performScrollTo()
            .assertTextContains("6.10 kg · Clasificación pendiente de Edad Corregida", substring = true)

        // 3. En Ventana 3 (Diagnóstico): diagnóstico y próximo control pendientes de Edad Corregida
        composeRule.onNodeWithTag("tab_diagnostico").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("diagnostico_edad_corregida_text").performScrollTo()
            .assertTextContains("EDAD CORREGIDA PENDIENTE", substring = true)
        composeRule.onNodeWithTag("diagnostico_global_title").performScrollTo()
            .assertTextContains("DIAGNÓSTICO PENDIENTE DE EDAD CORREGIDA", substring = true)
        assertEquals("Pendiente de Edad Corregida", vm.uiState.value.proximoControlFecha)

        // 4. En Ventana 4 (EDI): muestra «Pretérmino confirmado — EG exacta no registrada» y «EDAD CORREGIDA PENDIENTE»
        composeRule.onNodeWithTag("tab_edi").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("edi_summary_corrected_age_value").performScrollTo()
            .assertTextContains("EDAD CORREGIDA PENDIENTE", substring = true)
        composeRule.onNodeWithTag("edi_summary_ga_status_text").performScrollTo()
            .assertTextContains("Pretérmino confirmado — EG exacta no registrada", substring = true)

        // 5. Volver a Ventana 1 y confirmar explícitamente 32 semanas con el botón sin mover el slider
        composeRule.onNodeWithTag("tab_calculadora").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("calc_confirm_eg_button").performScrollTo().performClick()
        composeRule.waitForIdle()

        assertTrue(vm.uiState.value.ediHasRegisteredEg)
        assertFalse(vm.uiState.value.isCorrectedAgePending)
        assertEquals("0a 2m 5d", vm.uiState.value.edadCorregidaAMD)
        composeRule.onNodeWithTag("edad_corregida_text").performScrollTo()
            .assertTextContains("0a 2m 5d", substring = true)
    }

    @Test
    fun `15 corte exacto a los 2 anos para prematuro con EG confirmada y preservacion de ClinicalAgeCalculator`() {
        val calc = ClinicalAgeCalculator.calculateAges("14/07/2024", "20/03/2025", true, 34, 5)
        assertEquals(37, calc.diasRestarPrematuridad)
        assertTrue(calc.aplicaEdadCorregida)

        val vm = CredEdiViewModel()
        vm.updateEgSemanas(34)
        vm.updateEgDias(0)
        vm.updateFechaNacimiento("12/03/2025")

        // 1a 11m 27d (< 2 años) -> Aplica edad corregida
        vm.updateFechaEvaluacion("11/03/2027")
        assertTrue(vm.uiState.value.aplicaEdadCorregida)
        assertEquals("1a 10m 16d", vm.uiState.value.edadCorregidaAMD)

        // 2a 0m 0d (>= 2 años) -> No aplica edad corregida
        vm.updateFechaEvaluacion("12/03/2027")
        assertFalse(vm.uiState.value.aplicaEdadCorregida)
        assertEquals("No aplica edad corregida", vm.uiState.value.edadCorregidaAMD)
    }

    @Test
    fun `16 coincidencia entre las 4 vistas internas de Ventana 4 EDI y la resolucion de contradicciones`() {
        val vm = CredEdiViewModel()
        vm.updateFechaNacimiento("14/07/2024")
        vm.updateFechaEvaluacion("20/03/2025")

        composeRule.setContent { CredEdiApp(viewModel = vm) }

        composeRule.onNodeWithTag("tab_edi").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("edi_summary_ga_status_text").assertTextContains("EG no registrada", substring = true)

        composeRule.onNodeWithTag("edi_btn_change_group").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("edi_btn_confirm_term_no_weeks").performScrollTo().performClick()
        composeRule.waitForIdle()

        val expectedDisplay = "A término — semanas exactas no registradas"
        composeRule.onNodeWithTag("edi_bank_ga_status_text").performScrollTo().assertTextContains(expectedDisplay, substring = true)

        vm.navigateEdiView(EdiInternalView.SUMMARY_5_AXES)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("edi_summary_ga_status_text").performScrollTo().assertTextContains(expectedDisplay, substring = true)

        vm.navigateEdiView(EdiInternalView.QUESTIONNAIRE_AND_FALLBACK)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("edi_q_ga_status_text").assertTextContains(expectedDisplay, substring = true)

        vm.navigateEdiView(EdiInternalView.RESULTS_TRACEABILITY)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("edi_res_ga_status_text").performScrollTo().assertTextContains(expectedDisplay, substring = true)

        // Introducir contradicción (34s 5d + switch prematuridad OFF) y verificar las 4 vistas
        vm.updateEgSemanas(34)
        vm.updateEgDias(5)
        vm.navigateEdiView(EdiInternalView.GROUP_BANK_15)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("edi_prematurity_switch").performScrollTo().performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("edi_ga_contradiction_card").performScrollTo().assertIsDisplayed()
        val expectedContradiction = vm.uiState.value.ediGestationalAgeDisplay
        assertTrue(expectedContradiction.contains("34s 5d"))

        vm.navigateEdiView(EdiInternalView.SUMMARY_5_AXES)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("edi_summary_ga_contradiction_banner").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("edi_summary_ga_status_text").performScrollTo().assertTextContains(expectedContradiction, substring = true)

        vm.navigateEdiView(EdiInternalView.QUESTIONNAIRE_AND_FALLBACK)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("edi_q_ga_contradiction_banner").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("edi_q_ga_status_text").assertTextContains(expectedContradiction, substring = true)

        vm.navigateEdiView(EdiInternalView.RESULTS_TRACEABILITY)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("edi_res_ga_contradiction_banner").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("edi_res_ga_status_text").performScrollTo().assertTextContains(expectedContradiction, substring = true)

        // Resolver desde la tarjeta de confirmación en Banco EDI -> Pretérmino 34s 5d
        vm.navigateEdiView(EdiInternalView.GROUP_BANK_15)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("edi_ga_confirm_keep_eg").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertFalse(vm.uiState.value.ediHasGestationalContradiction)
        composeRule.onNodeWithTag("edi_bank_ga_status_text").performScrollTo().assertTextContains("Pretérmino · 34s 5d", substring = true)
    }
}
