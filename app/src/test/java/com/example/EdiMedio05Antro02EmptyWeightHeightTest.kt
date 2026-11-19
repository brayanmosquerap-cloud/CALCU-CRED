package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.example.model.ClassificationSeverity
import com.example.model.CurvaIndicador
import com.example.model.CutoffBand
import com.example.model.GlobalDiagnosisCompleteness
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

/**
 * MEDIO-05 — LOTE ANTRO-02:
 * Pruebas exhaustivas de regresión para la corrección de diagnósticos nutricionales falsos
 * con peso y talla/longitud vacíos, parciales o borrados (Casos obligatorios A a M).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class EdiMedio05Antro02EmptyWeightHeightTest {

    @get:Rule
    val composeRule = createComposeRule()

    /**
     * Configura el escenario clínico de un niño de 14 meses nacido a término:
     * - FN: 15/04/2025
     * - Evaluación: 15/06/2026 (14 meses 0 días)
     * - Nacido a término confirmado (< 24 meses)
     */
    private fun create14MonthTermChildViewModel(): CredEdiViewModel {
        val viewModel = CredEdiViewModel()
        viewModel.updateFechaNacimiento("15/04/2025")
        viewModel.updateFechaEvaluacion("15/06/2026")
        viewModel.togglePrematurez(false)
        return viewModel
    }

    // =========================================================================
    // CASO A: Inicio en frío con los tres campos vacíos
    // =========================================================================
    @Test
    fun `caso A - inicio en frio tiene peso talla y PC vacios con NaN y 0 datos`() {
        val viewModel = CredEdiViewModel()
        val state = viewModel.uiState.value

        assertEquals("", state.pesoInput)
        assertEquals("", state.longitudInput)
        assertEquals("", state.perimetroCefalicoInput)

        assertTrue("pesoKg inicial debe ser NaN", state.pesoKg.isNaN())
        assertTrue("longitudCm inicial debe ser NaN", state.longitudCm.isNaN())
        assertTrue("perimetroCefalicoCm inicial debe ser NaN", state.perimetroCefalicoCm.isNaN())

        assertFalse(state.hasPesoIngresado)
        assertFalse(state.hasLongitudIngresada)
        assertFalse(state.hasPerimetroCefalicoIngresado)

        assertTrue(state.effectivePesoKg.isNaN())
        assertTrue(state.effectiveLongitudCm.isNaN())
        assertTrue(state.effectivePerimetroCefalicoCm.isNaN())
        assertTrue("imcCalculado sin peso ni talla debe ser NaN", state.imcCalculado.isNaN())

        assertEquals(0, state.datosAntropometricosCount)
    }

    // =========================================================================
    // CASO B: Reproducción exacta ANTRO-02 (Niño de 14 meses, término, sin mediciones)
    // =========================================================================
    @Test
    fun `caso B - reproduccion exacta ANTRO-02 nino 14 meses termino con 3 campos vacios nunca diagnostica desnutricion ni talla baja`() {
        val viewModel = create14MonthTermChildViewModel()
        val state = viewModel.uiState.value

        assertEquals("1a 2m 0d", state.edadCronologicaAMD)
        assertFalse(state.isAgeDependentEvaluationBlocked)
        assertFalse(state.hasPesoIngresado)
        assertFalse(state.hasLongitudIngresada)
        assertFalse(state.hasPerimetroCefalicoIngresado)

        // Los 5 indicadores deben estar en "Sin medición"
        listOf(state.evalPE, state.evalTE, state.evalPT, state.evalIMCE, state.evalPCE).forEach { eval ->
            assertFalse("${eval.indicador} no debe tener referencia activa sin medicion", eval.isReferenceAvailable)
            assertEquals("Sin medición", eval.shortClassification)
            assertEquals("--", eval.patientValueFormatted)
            assertTrue(eval.zScore.isNaN())
        }

        assertEquals("Peso no registrado", state.evalPE.fullClassification)
        assertEquals("Talla/longitud no registrada", state.evalTE.fullClassification)
        assertEquals("Peso y talla/longitud no registrados", state.evalPT.fullClassification)
        assertEquals("Peso y talla/longitud no registrados", state.evalIMCE.fullClassification)
        assertEquals("Perímetro cefálico no registrado", state.evalPCE.fullClassification)

        // Dictamen global sin mediciones
        val global = state.globalDiagnosis
        assertEquals(GlobalDiagnosisCompleteness.UNAVAILABLE, global.completeness)
        assertEquals("Sin mediciones antropométricas registradas", global.title)
        assertFalse(global.title.contains("Desnutrición", ignoreCase = true))
        assertFalse(global.title.contains("Déficit", ignoreCase = true))
        assertFalse(global.title.contains("Talla baja", ignoreCase = true))

        // Verificación visual en DiagnosticoScreen
        composeRule.setContent {
            CredEdiApp(viewModel = viewModel)
        }
        composeRule.onNodeWithTag("tab_diagnostico").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("diagnostico_global_unavailable_badge", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithTag("diagnostico_global_z_badge", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithTag("diagnostico_global_title", useUnmergedTree = true).assertExists()
        composeRule.onAllNodesWithText("Sin medición", substring = false, useUnmergedTree = true)[0].assertExists()
        composeRule.onAllNodesWithText("Sin mediciones antropométricas registradas", substring = true, useUnmergedTree = true)[0].assertExists()

        // Jamás deben aparecer los falsos positivos de ANTRO-02 ni los valores precargados 5.40 / 61.5
        assertEquals(0, composeRule.onAllNodesWithText("Desnutrición severa", substring = true, useUnmergedTree = true).fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithText("Déficit ponderal", substring = true, useUnmergedTree = true).fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithText("Talla baja severa", substring = true, useUnmergedTree = true).fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithText("5.40", substring = true, useUnmergedTree = true).fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithText("61.5", substring = true, useUnmergedTree = true).fetchSemanticsNodes().size)
    }

    // =========================================================================
    // CASO C: Solo peso registrado (14 meses, término)
    // =========================================================================
    @Test
    fun `caso C - solo peso registrado evalua PE e invalida TE PT IMCE y emite dictamen parcial`() {
        val viewModel = create14MonthTermChildViewModel()
        viewModel.updatePesoInput("10.20")
        val stateNormal = viewModel.uiState.value

        assertTrue(stateNormal.hasPesoIngresado)
        assertFalse(stateNormal.hasLongitudIngresada)
        assertFalse(stateNormal.hasPerimetroCefalicoIngresado)
        assertEquals(1, stateNormal.datosAntropometricosCount)
        assertTrue(stateNormal.imcCalculado.isNaN())

        assertTrue(stateNormal.evalPE.isReferenceAvailable)
        assertEquals(CutoffBand.NORMAL_RANGE, stateNormal.evalPE.activeBand)
        assertEquals("Eutrófico", stateNormal.evalPE.shortClassification)

        assertFalse(stateNormal.evalTE.isReferenceAvailable)
        assertEquals("Sin medición", stateNormal.evalTE.shortClassification)
        assertFalse(stateNormal.evalPT.isReferenceAvailable)
        assertEquals("Sin medición", stateNormal.evalPT.shortClassification)
        assertEquals("Talla/longitud no registrada para calcular P/T", stateNormal.evalPT.fullClassification)
        assertFalse(stateNormal.evalIMCE.isReferenceAvailable)
        assertEquals("Sin medición", stateNormal.evalIMCE.shortClassification)
        assertEquals("Talla/longitud no registrada para calcular IMC/E", stateNormal.evalIMCE.fullClassification)
        assertFalse(stateNormal.evalPCE.isReferenceAvailable)
        assertEquals("Sin medición", stateNormal.evalPCE.shortClassification)

        assertEquals(GlobalDiagnosisCompleteness.PARTIAL, stateNormal.globalDiagnosis.completeness)
        assertTrue(stateNormal.globalDiagnosis.title.contains("Parcial", ignoreCase = true))
        assertEquals(ClassificationSeverity.WARNING, stateNormal.globalDiagnosis.severity)

        // Subcaso C2: Peso bajo severo (6.50 kg a los 14m) sin talla -> no debe ocultarse la alteración de P/E
        viewModel.updatePesoInput("6.50")
        val stateLowWeight = viewModel.uiState.value
        assertEquals(CutoffBand.BELOW_MINUS_3, stateLowWeight.evalPE.activeBand)
        assertEquals(GlobalDiagnosisCompleteness.PARTIAL, stateLowWeight.globalDiagnosis.completeness)
        assertEquals(ClassificationSeverity.SEVERE, stateLowWeight.globalDiagnosis.severity)
        assertTrue(stateLowWeight.globalDiagnosis.title.contains("Valoración Parcial", ignoreCase = true))
    }

    // =========================================================================
    // CASO D: Solo talla registrada (14 meses, término)
    // =========================================================================
    @Test
    fun `caso D - solo talla registrada evalua TE e invalida PE PT IMCE y emite dictamen parcial`() {
        val viewModel = create14MonthTermChildViewModel()
        viewModel.updateLongitudInput("78.0")
        val stateNormal = viewModel.uiState.value

        assertFalse(stateNormal.hasPesoIngresado)
        assertTrue(stateNormal.hasLongitudIngresada)
        assertFalse(stateNormal.hasPerimetroCefalicoIngresado)
        assertEquals(1, stateNormal.datosAntropometricosCount)
        assertTrue(stateNormal.imcCalculado.isNaN())

        assertTrue(stateNormal.evalTE.isReferenceAvailable)
        assertEquals(CutoffBand.NORMAL_RANGE, stateNormal.evalTE.activeBand)
        assertEquals("Adecuada", stateNormal.evalTE.shortClassification)

        assertFalse(stateNormal.evalPE.isReferenceAvailable)
        assertEquals("Sin medición", stateNormal.evalPE.shortClassification)
        assertFalse(stateNormal.evalPT.isReferenceAvailable)
        assertEquals("Sin medición", stateNormal.evalPT.shortClassification)
        assertEquals("Peso corporal no registrado para calcular P/T", stateNormal.evalPT.fullClassification)
        assertFalse(stateNormal.evalIMCE.isReferenceAvailable)
        assertEquals("Sin medición", stateNormal.evalIMCE.shortClassification)
        assertEquals("Peso corporal no registrado para calcular IMC/E", stateNormal.evalIMCE.fullClassification)
        assertFalse(stateNormal.evalPCE.isReferenceAvailable)
        assertEquals("Sin medición", stateNormal.evalPCE.shortClassification)

        assertEquals(GlobalDiagnosisCompleteness.PARTIAL, stateNormal.globalDiagnosis.completeness)
        assertTrue(stateNormal.globalDiagnosis.title.contains("Parcial", ignoreCase = true))
        assertEquals(ClassificationSeverity.WARNING, stateNormal.globalDiagnosis.severity)

        // Subcaso D2: Talla baja severa (68.0 cm a los 14m) sin peso -> no debe ocultarse la alteración de T/E
        viewModel.updateLongitudInput("68.0")
        val stateLowHeight = viewModel.uiState.value
        assertEquals(CutoffBand.BELOW_MINUS_3, stateLowHeight.evalTE.activeBand)
        assertEquals(GlobalDiagnosisCompleteness.PARTIAL, stateLowHeight.globalDiagnosis.completeness)
        assertEquals(ClassificationSeverity.SEVERE, stateLowHeight.globalDiagnosis.severity)
        assertTrue(stateLowHeight.globalDiagnosis.title.contains("Valoración Parcial", ignoreCase = true))
    }

    // =========================================================================
    // CASO E: Peso y talla registrados, PC vacío
    // =========================================================================
    @Test
    fun `caso E - peso y talla registrados con PC vacio evalua PE TE PT IMCE completo y PCE sin medicion`() {
        val viewModel = create14MonthTermChildViewModel()
        viewModel.updatePesoInput("10.20")
        viewModel.updateLongitudInput("78.0")
        val state = viewModel.uiState.value

        assertTrue(state.hasPesoIngresado)
        assertTrue(state.hasLongitudIngresada)
        assertFalse(state.hasPerimetroCefalicoIngresado)
        assertEquals(2, state.datosAntropometricosCount)
        assertTrue(state.imcCalculado.isFinite())

        assertTrue(state.evalPE.isReferenceAvailable)
        assertTrue(state.evalTE.isReferenceAvailable)
        assertTrue(state.evalPT.isReferenceAvailable)
        assertTrue(state.evalIMCE.isReferenceAvailable)
        assertFalse(state.evalPCE.isReferenceAvailable)
        assertEquals("Sin medición", state.evalPCE.shortClassification)

        assertEquals(GlobalDiagnosisCompleteness.COMPLETE, state.globalDiagnosis.completeness)
        assertEquals(ClassificationSeverity.NORMAL, state.globalDiagnosis.severity)
    }

    // =========================================================================
    // CASO F: Peso, talla y PC registrados
    // =========================================================================
    @Test
    fun `caso F - peso talla y PC registrados evalua los 5 indicadores y muestra 3 datos en CalculadoraScreen`() {
        val viewModel = create14MonthTermChildViewModel()
        viewModel.updatePesoInput("10.20")
        viewModel.updateLongitudInput("78.0")
        viewModel.updatePerimetroCefalicoInput("46.5")
        val state = viewModel.uiState.value

        assertEquals(3, state.datosAntropometricosCount)
        assertTrue(state.evalPE.isReferenceAvailable)
        assertTrue(state.evalTE.isReferenceAvailable)
        assertTrue(state.evalPT.isReferenceAvailable)
        assertTrue(state.evalIMCE.isReferenceAvailable)
        assertTrue(state.evalPCE.isReferenceAvailable)
        assertEquals("Normocéfalo", state.evalPCE.shortClassification)
        assertEquals(GlobalDiagnosisCompleteness.COMPLETE, state.globalDiagnosis.completeness)

        composeRule.setContent {
            CredEdiApp(viewModel = viewModel)
        }

        composeRule.onNodeWithTag("calculadora_datos_count_badge", useUnmergedTree = true)
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("3 datos", useUnmergedTree = true).assertIsDisplayed()
    }

    // =========================================================================
    // CASO G: Peso y talla registrados -> borrar peso
    // =========================================================================
    @Test
    fun `caso G - registrar peso y talla y luego borrar peso invalida PE PT e IMCE inmediatamente`() {
        val viewModel = create14MonthTermChildViewModel()
        viewModel.updatePesoInput("10.20")
        viewModel.updateLongitudInput("78.0")
        assertTrue(viewModel.uiState.value.evalPE.isReferenceAvailable)
        assertTrue(viewModel.uiState.value.evalPT.isReferenceAvailable)
        assertTrue(viewModel.uiState.value.evalIMCE.isReferenceAvailable)

        // Borrar peso
        viewModel.updatePesoInput("")
        val state = viewModel.uiState.value

        assertFalse(state.hasPesoIngresado)
        assertTrue("pesoKg debe invalidarse a NaN al vaciar el campo", state.pesoKg.isNaN())
        assertTrue(state.effectivePesoKg.isNaN())
        assertTrue(state.imcCalculado.isNaN())

        assertEquals("Sin medición", state.evalPE.shortClassification)
        assertEquals("Sin medición", state.evalPT.shortClassification)
        assertEquals("Sin medición", state.evalIMCE.shortClassification)

        // T/E sigue activo porque la talla continúa registrada
        assertTrue(state.evalTE.isReferenceAvailable)
        assertEquals("Adecuada", state.evalTE.shortClassification)
        assertEquals(GlobalDiagnosisCompleteness.PARTIAL, state.globalDiagnosis.completeness)
    }

    // =========================================================================
    // CASO H: Peso y talla registrados -> borrar talla
    // =========================================================================
    @Test
    fun `caso H - registrar peso y talla y luego borrar talla invalida TE PT e IMCE sin quedar en 30 cm`() {
        val viewModel = create14MonthTermChildViewModel()
        viewModel.updatePesoInput("10.20")
        viewModel.updateLongitudInput("78.0")
        assertTrue(viewModel.uiState.value.evalTE.isReferenceAvailable)

        // Borrar talla
        viewModel.updateLongitudInput("")
        val state = viewModel.uiState.value

        assertFalse(state.hasLongitudIngresada)
        assertTrue("longitudCm debe invalidarse a NaN y nunca quedar en 30.0", state.longitudCm.isNaN())
        assertTrue(state.effectiveLongitudCm.isNaN())
        assertTrue(state.imcCalculado.isNaN())

        assertEquals("Sin medición", state.evalTE.shortClassification)
        assertEquals("Sin medición", state.evalPT.shortClassification)
        assertEquals("Sin medición", state.evalIMCE.shortClassification)

        // P/E sigue activo porque el peso continúa registrado
        assertTrue(state.evalPE.isReferenceAvailable)
        assertEquals("Eutrófico", state.evalPE.shortClassification)
        assertEquals(GlobalDiagnosisCompleteness.PARTIAL, state.globalDiagnosis.completeness)
    }

    // =========================================================================
    // CASO I: Registrar los tres -> borrar los tres
    // =========================================================================
    @Test
    fun `caso I - registrar peso talla y PC y luego borrar los tres retorna integramente a Sin medicion`() {
        val viewModel = create14MonthTermChildViewModel()
        viewModel.updatePesoInput("10.20")
        viewModel.updateLongitudInput("78.0")
        viewModel.updatePerimetroCefalicoInput("46.5")
        assertEquals(3, viewModel.uiState.value.datosAntropometricosCount)

        viewModel.updatePesoInput("")
        viewModel.updateLongitudInput("")
        viewModel.updatePerimetroCefalicoInput("")

        val state = viewModel.uiState.value
        assertEquals(0, state.datosAntropometricosCount)
        assertTrue(state.pesoKg.isNaN())
        assertTrue(state.longitudCm.isNaN())
        assertTrue(state.perimetroCefalicoCm.isNaN())
        assertTrue(state.imcCalculado.isNaN())

        listOf(state.evalPE, state.evalTE, state.evalPT, state.evalIMCE, state.evalPCE).forEach { eval ->
            assertFalse(eval.isReferenceAvailable)
            assertEquals("Sin medición", eval.shortClassification)
            assertEquals("--", eval.patientValueFormatted)
        }
        assertEquals(GlobalDiagnosisCompleteness.UNAVAILABLE, state.globalDiagnosis.completeness)
        assertEquals("Sin mediciones antropométricas registradas", state.globalDiagnosis.title)
    }

    // =========================================================================
    // CASO J: Entrada parcial y borrado carácter por carácter ("100" -> "10" -> "1" -> "")
    // =========================================================================
    @Test
    fun `caso J - borrado caracter por caracter en talla y peso nunca fabrica 30 cm ni 0_50 kg ni falsos severos`() {
        val viewModel = create14MonthTermChildViewModel()
        viewModel.updatePesoInput("10.20")
        viewModel.updateLongitudInput("100")
        assertTrue(viewModel.uiState.value.hasLongitudIngresada)
        assertEquals(100.0, viewModel.uiState.value.longitudCm, 0.001)

        // Paso 1: "100" -> "10" (parcial < 30.0 cm)
        viewModel.updateLongitudInput("10")
        val state10 = viewModel.uiState.value
        assertEquals("10", state10.longitudInput)
        assertTrue(state10.isLongitudTemporalOrInvalid)
        assertFalse(state10.hasLongitudIngresada)
        assertTrue("Con '10' en longitudInput, longitudCm debe ser NaN y NO 30.0", state10.longitudCm.isNaN())
        assertEquals("Sin medición", state10.evalTE.shortClassification)
        assertFalse(state10.evalTE.fullClassification.contains("Talla Baja", ignoreCase = true))

        // Paso 2: "10" -> "1" (parcial < 30.0 cm)
        viewModel.updateLongitudInput("1")
        val state1 = viewModel.uiState.value
        assertEquals("1", state1.longitudInput)
        assertTrue(state1.isLongitudTemporalOrInvalid)
        assertFalse(state1.hasLongitudIngresada)
        assertTrue("Con '1' en longitudInput, longitudCm debe ser NaN y NO 30.0", state1.longitudCm.isNaN())
        assertEquals("Sin medición", state1.evalTE.shortClassification)
        assertFalse(state1.evalTE.fullClassification.contains("Talla Baja", ignoreCase = true))

        // Paso 3: "1" -> "" (vacío)
        viewModel.updateLongitudInput("")
        val stateEmpty = viewModel.uiState.value
        assertEquals("", stateEmpty.longitudInput)
        assertFalse(stateEmpty.isLongitudTemporalOrInvalid)
        assertFalse(stateEmpty.hasLongitudIngresada)
        assertTrue(stateEmpty.longitudCm.isNaN())
        assertEquals("Sin medición", stateEmpty.evalTE.shortClassification)

        // También verificar entrada parcial en peso: "0." y "0.2" (< 0.50 kg)
        viewModel.updatePesoInput("0.")
        assertTrue(viewModel.uiState.value.isPesoTemporalOrInvalid)
        assertFalse(viewModel.uiState.value.hasPesoIngresado)
        assertTrue(viewModel.uiState.value.pesoKg.isNaN())
        assertEquals("Sin medición", viewModel.uiState.value.evalPE.shortClassification)

        viewModel.updatePesoInput("0.2")
        assertTrue(viewModel.uiState.value.isPesoTemporalOrInvalid)
        assertFalse(viewModel.uiState.value.hasPesoIngresado)
        assertTrue(viewModel.uiState.value.pesoKg.isNaN())
        assertEquals("Sin medición", viewModel.uiState.value.evalPE.shortClassification)

        // Verificar que valores altos reales no se truncan silenciosamente a 40 kg ni 130 cm
        viewModel.updatePesoInput("45.0")
        viewModel.updateLongitudInput("135.0")
        assertEquals(45.0, viewModel.uiState.value.pesoKg, 0.001)
        assertEquals(135.0, viewModel.uiState.value.longitudCm, 0.001)
        assertTrue(viewModel.uiState.value.requiresPesoClinicalReview)
        assertTrue(viewModel.uiState.value.requiresLongitudClinicalReview)
    }

    // =========================================================================
    // CASO K: Talla > 110 cm con mediciones válidas (P/T fuera de dominio vs Sin medición)
    // =========================================================================
    @Test
    fun `caso K - talla mayor a 110 cm con peso y talla validos reporta PT No disponible por rango y no Sin medicion`() {
        val viewModel = CredEdiViewModel()
        viewModel.updateFechaNacimiento("15/06/2022")
        viewModel.updateFechaEvaluacion("15/06/2026") // 48 meses
        viewModel.updatePesoInput("17.5")
        viewModel.updateLongitudInput("110.5")
        val state = viewModel.uiState.value

        assertTrue(state.hasPesoIngresado)
        assertTrue(state.hasLongitudIngresada)
        assertTrue(state.evalPE.isReferenceAvailable)
        assertTrue(state.evalTE.isReferenceAvailable)
        assertTrue(state.evalIMCE.isReferenceAvailable)

        // P/T fuera de dominio OMS (45.0–110.0 cm), distinto de "Sin medición"
        assertFalse(state.evalPT.isReferenceAvailable)
        assertEquals("No disponible", state.evalPT.shortClassification)
        assertEquals("NO DISPONIBLE POR RANGO DE REFERENCIA", state.evalPT.unavailabilityTitle)
        assertEquals(GlobalDiagnosisCompleteness.PARTIAL, state.globalDiagnosis.completeness)
        assertEquals(listOf("P/T"), state.globalDiagnosis.outOfRangeIndicators)
    }

    // =========================================================================
    // CASO L: Edad > 60 meses (Fuera de rango OMS vs Sin medición)
    // =========================================================================
    @Test
    fun `caso L - edad mayor a 60 meses reporta fuera de rango de referencia OMS y no Sin medicion`() {
        val viewModel = CredEdiViewModel()
        viewModel.updateFechaNacimiento("01/01/2020")
        viewModel.updateFechaEvaluacion("15/05/2025") // 64 meses 14 días (> 60m)
        val state = viewModel.uiState.value

        assertFalse(state.isOmsAgeRangeAvailable)
        listOf(state.evalPE, state.evalTE, state.evalPT, state.evalIMCE, state.evalPCE).forEach { eval ->
            assertFalse(eval.isReferenceAvailable)
            assertEquals("No disponible", eval.shortClassification)
            assertEquals("NO DISPONIBLE POR RANGO DE REFERENCIA", eval.unavailabilityTitle)
        }
        assertEquals(GlobalDiagnosisCompleteness.UNAVAILABLE, state.globalDiagnosis.completeness)
        assertEquals("NO DISPONIBLE POR RANGO DE REFERENCIA", state.globalDiagnosis.title)
    }

    // =========================================================================
    // CASO M: Prematuridad < 24 meses con EG confirmada vs pendiente
    // =========================================================================
    @Test
    fun `caso M - prematuridad menor de 24 meses con EG pendiente bloquea por edad y con EG confirmada usa edad corregida`() {
        val viewModel = CredEdiViewModel()
        viewModel.updateFechaNacimiento("15/10/2024")
        viewModel.updateFechaEvaluacion("15/02/2025") // 4m cronológicos
        viewModel.updatePesoInput("5.40")
        viewModel.updateLongitudInput("61.5")

        // Al activar prematurez sin confirmar EG, la edad corregida queda pendiente
        viewModel.togglePrematurez(true)
        assertTrue(viewModel.uiState.value.isCorrectedAgePending)
        assertTrue(viewModel.uiState.value.isAgeDependentEvaluationBlocked)

        // Al confirmar EG (32 sem -> 2m corregidos), se desbloquea y evalúa con edad corregida
        viewModel.confirmCurrentEgSemanas()
        val stateConfirmed = viewModel.uiState.value
        assertFalse(stateConfirmed.isCorrectedAgePending)
        assertFalse(stateConfirmed.isAgeDependentEvaluationBlocked)
        assertTrue(stateConfirmed.aplicaEdadCorregida)
        assertEquals("Eutrófico", stateConfirmed.evalPE.shortClassification)
        assertEquals("Adecuada", stateConfirmed.evalTE.shortClassification)
    }

    // =========================================================================
    // VERIFICACIÓN ADICIONAL: CurvasOmsScreen sin mediciones y PDF Exporter
    // =========================================================================
    @Test
    fun `CurvasOmsScreen sin mediciones muestra tarjeta de Sin medicion en todas las pestanas y exportador PDF muestra No registrado`() {
        val viewModel = create14MonthTermChildViewModel()

        composeRule.setContent {
            CredEdiApp(viewModel = viewModel)
        }
        composeRule.onNodeWithTag("tab_curvas").performClick()
        composeRule.waitForIdle()

        // En P/E por defecto con peso vacío: tarjeta de no disponible y texto "Sin medición"
        composeRule.onNodeWithTag("curvas_chart_unavailable_card", useUnmergedTree = true)
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag("curvas_cutoff_unavailable_card", useUnmergedTree = true)
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("Sin clasificación de Peso/Edad (Sin medición)", substring = true, useUnmergedTree = true)
            .assertIsDisplayed()

        // Verificar resumen de exportación PDF con los 3 campos vacíos: no debe incluir 5.40, 61.5 ni diagnósticos falsos
        val footerEmpty = EdiPdfExporter.buildCredFooterSummary(viewModel.uiState.value)
        assertFalse(footerEmpty.contains("Peso:"))
        assertFalse(footerEmpty.contains("Talla:"))
        assertFalse(footerEmpty.contains("PC:"))
        assertFalse(footerEmpty.contains("5.40"))
        assertFalse(footerEmpty.contains("61.5"))
        assertFalse(footerEmpty.contains("Desnutrición", ignoreCase = true))
        assertFalse(footerEmpty.contains("Déficit", ignoreCase = true))
        assertFalse(footerEmpty.contains("Talla baja", ignoreCase = true))
    }
}
