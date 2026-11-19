package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.AppTab
import com.example.model.ClassificationSeverity
import com.example.model.CurvaIndicador
import com.example.model.CutoffBand
import com.example.model.SexoBiologico
import com.example.viewmodel.CredEdiViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("CRED & EDI", appName)
    }

    @Test
    fun `verify tabs and datos edi modal flow`() {
        val viewModel = CredEdiViewModel()
        assertEquals(AppTab.CALCULADORA, viewModel.uiState.value.currentTab)

        viewModel.selectTab(AppTab.CURVAS_OMS)
        assertEquals(AppTab.CURVAS_OMS, viewModel.uiState.value.currentTab)

        viewModel.selectTab(AppTab.DIAGNOSTICO)
        assertEquals(AppTab.DIAGNOSTICO, viewModel.uiState.value.currentTab)

        viewModel.selectTab(AppTab.EDI)
        assertEquals(AppTab.EDI, viewModel.uiState.value.currentTab)
        assertFalse(viewModel.uiState.value.showDatosEdiModal)

        viewModel.openDatosEdiModal()
        assertTrue(viewModel.uiState.value.showDatosEdiModal)

        viewModel.guardarYCalcularSemaforoEdi()
        assertFalse(viewModel.uiState.value.showDatosEdiModal)
    }

    @Test
    fun `verify manual date calculation and gender switch`() {
        val viewModel = CredEdiViewModel()

        // Default: F. NACIMIENTO is empty (""), F. EVALUACIÓN is today, and Prematurez is disabled (false)
        assertEquals("", viewModel.uiState.value.fechaNacimiento)
        assertEquals(com.example.model.ClinicalAgeCalculator.todayFormatted(), viewModel.uiState.value.fechaEvaluacion)
        assertEquals(com.example.model.ClinicalAgeCalculator.todayFormatted(), viewModel.uiState.value.ediFechaEval)
        assertFalse(viewModel.uiState.value.esPrematuro)
        assertFalse(viewModel.uiState.value.frbItems.first { it.id == 1 }.checked)
        assertEquals(0, viewModel.uiState.value.edadCronologicaMeses)
        assertEquals(0, viewModel.uiState.value.edadCronologicaDias)
        assertEquals("0a 0m 0d", viewModel.uiState.value.edadCronologicaAMD)
        assertEquals("No aplica edad corregida", viewModel.uiState.value.edadCorregidaAMD)

        // Setting 15/10/2024 -> 15/02/2025 with prematurez disabled by default -> chronological is 0a 4m 0d, corrected does not apply
        viewModel.updateFechaNacimiento("15/10/2024")
        viewModel.updateFechaEvaluacion("15/02/2025")
        assertEquals("0a 4m 0d", viewModel.uiState.value.edadCronologicaAMD)
        assertFalse(viewModel.uiState.value.aplicaEdadCorregida)
        assertEquals("No aplica edad corregida", viewModel.uiState.value.edadCorregidaAMD)

        // Manually activating Prematurez: before confirming weeks, corrected age is pending and FRB is not auto-checked
        viewModel.togglePrematurez(true)
        assertTrue(viewModel.uiState.value.esPrematuro)
        assertFalse(viewModel.uiState.value.aplicaEdadCorregida)
        assertTrue(viewModel.uiState.value.isCorrectedAgePending)
        assertEquals("EDAD CORREGIDA PENDIENTE", viewModel.uiState.value.edadCorregidaAMD)
        assertFalse(viewModel.uiState.value.frbItems.first { it.id == 1 }.checked)

        // Explicitly confirming 32w EG (without moving slider) -> +56 days -> fecha ajustada 10/12/2024
        // Eval 15/02/2025 -> 0a 4m 0d chronological, 0a 2m 5d corrected on real calendar
        viewModel.confirmCurrentEgSemanas()
        assertTrue(viewModel.uiState.value.aplicaEdadCorregida)
        assertTrue(viewModel.uiState.value.frbItems.first { it.id == 1 }.checked)
        assertEquals(0, viewModel.uiState.value.edadCronologicaAnios)
        assertEquals(4, viewModel.uiState.value.edadCronologicaMesesRestantes)
        assertEquals(4, viewModel.uiState.value.edadCronologicaMeses)
        assertEquals(0, viewModel.uiState.value.edadCronologicaDias)
        assertEquals("0a 4m 0d", viewModel.uiState.value.edadCronologicaAMD)
        assertEquals("10/12/2024", viewModel.uiState.value.fechaAjustada)
        assertEquals(0, viewModel.uiState.value.edadCorregidaAnios)
        assertEquals(2, viewModel.uiState.value.edadCorregidaMesesRestantes)
        assertEquals(2, viewModel.uiState.value.edadCorregidaMeses)
        assertEquals(5, viewModel.uiState.value.edadCorregidaDias)
        assertEquals("0a 2m 5d", viewModel.uiState.value.edadCorregidaAMD)

        // Birth: 10/01/2025, Eval: 20/05/2025, 32w EG (+56 days -> fecha ajustada 07/03/2025)
        // Chronological: 0a 4m 10d, Corrected: 0a 2m 13d
        viewModel.updateFechaNacimiento("10/01/2025")
        viewModel.updateFechaEvaluacion("20/05/2025")
        assertEquals("0a 4m 10d", viewModel.uiState.value.edadCronologicaAMD)
        assertEquals("07/03/2025", viewModel.uiState.value.fechaAjustada)
        assertEquals("0a 2m 13d", viewModel.uiState.value.edadCorregidaAMD)

        // Over 1 year old example: Birth 10/01/2025, Eval 20/03/2026 (1a 2m 10d chronological, 1a 0m 13d corrected at 32w EG)
        viewModel.updateFechaEvaluacion("20/03/2026")
        assertEquals(1, viewModel.uiState.value.edadCronologicaAnios)
        assertEquals(2, viewModel.uiState.value.edadCronologicaMesesRestantes)
        assertEquals(10, viewModel.uiState.value.edadCronologicaDias)
        assertEquals("1a 2m 10d", viewModel.uiState.value.edadCronologicaAMD)
        assertEquals(1, viewModel.uiState.value.edadCorregidaAnios)
        assertEquals(0, viewModel.uiState.value.edadCorregidaMesesRestantes)
        assertEquals(13, viewModel.uiState.value.edadCorregidaDias)
        assertEquals("1a 0m 13d", viewModel.uiState.value.edadCorregidaAMD)

        // Switch gender between Niño (Pastel Blue) and Niña (Pastel Pink)
        assertEquals(SexoBiologico.NINO, viewModel.uiState.value.sexo)
        viewModel.selectSexo(SexoBiologico.NINA)
        assertEquals(SexoBiologico.NINA, viewModel.uiState.value.sexo)
    }

    @Test
    fun `verify nts 238 age calculation regression and edge cases`() {
        val viewModel = CredEdiViewModel()

        // 1. MANDATORY REGRESSION TEST CASE:
        // Nacimiento: 12/03/2025
        // Evaluación: 10/11/2026
        // Edad gestacional: 34 semanas
        // Edad cronológica esperada: 1 año, 7 meses y 29 días (1a 7m 29d)
        // Fecha ajustada: 23/04/2025
        // Edad corregida esperada: 1 año, 6 meses y 18 días (1a 6m 18d)
        viewModel.updateFechaNacimiento("12/03/2025")
        viewModel.updateFechaEvaluacion("10/11/2026")
        viewModel.togglePrematurez(true)
        viewModel.updateEgSemanas(34)

        val reg = viewModel.uiState.value
        assertEquals(1, reg.edadCronologicaAnios)
        assertEquals(7, reg.edadCronologicaMesesRestantes)
        assertEquals(29, reg.edadCronologicaDias)
        assertEquals("1a 7m 29d", reg.edadCronologicaAMD)
        assertEquals("1 año, 7 meses y 29 días", reg.edadCronologicaCompleta)
        assertTrue(reg.aplicaEdadCorregida)
        assertEquals(6, reg.deficitSemanas)
        assertEquals(42, reg.diasPrematuridad)
        assertEquals("23/04/2025", reg.fechaAjustada)
        assertEquals(1, reg.edadCorregidaAnios)
        assertEquals(6, reg.edadCorregidaMesesRestantes)
        assertEquals(18, reg.edadCorregidaDias)
        assertEquals("1a 6m 18d", reg.edadCorregidaAMD)
        assertEquals("1 año, 6 meses y 18 días", reg.edadCorregidaCompleta)

        // 2. Day of birth as Day Zero (0a 0m 0d)
        viewModel.updateFechaNacimiento("12/03/2025")
        viewModel.updateFechaEvaluacion("12/03/2025")
        assertEquals("0a 0m 0d", viewModel.uiState.value.edadCronologicaAMD)
        assertEquals("0 años, 0 meses y 0 días", viewModel.uiState.value.edadCronologicaCompleta)

        // 3. Preterm infants of 28, 32, 34, and 36 weeks (Birth: 10/01/2025, Eval: 10/06/2025 -> Crono: 0a 5m 0d)
        viewModel.updateFechaNacimiento("10/01/2025")
        viewModel.updateFechaEvaluacion("10/06/2025")
        assertEquals("0a 5m 0d", viewModel.uiState.value.edadCronologicaAMD)

        // 28 weeks: 12 weeks missing = 84 days -> 10/01/2025 + 84d = 04/04/2025 -> to 10/06/2025 = 0a 2m 6d
        viewModel.updateEgSemanas(28)
        assertEquals(84, viewModel.uiState.value.diasPrematuridad)
        assertEquals("04/04/2025", viewModel.uiState.value.fechaAjustada)
        assertEquals("0a 2m 6d", viewModel.uiState.value.edadCorregidaAMD)

        // 32 weeks: 8 weeks missing = 56 days -> 10/01/2025 + 56d = 07/03/2025 -> to 10/06/2025 = 0a 3m 3d
        viewModel.updateEgSemanas(32)
        assertEquals(56, viewModel.uiState.value.diasPrematuridad)
        assertEquals("07/03/2025", viewModel.uiState.value.fechaAjustada)
        assertEquals("0a 3m 3d", viewModel.uiState.value.edadCorregidaAMD)

        // 34 weeks: 6 weeks missing = 42 days -> 10/01/2025 + 42d = 21/02/2025 -> to 10/06/2025 = 0a 3m 20d
        viewModel.updateEgSemanas(34)
        assertEquals(42, viewModel.uiState.value.diasPrematuridad)
        assertEquals("21/02/2025", viewModel.uiState.value.fechaAjustada)
        assertEquals("0a 3m 20d", viewModel.uiState.value.edadCorregidaAMD)

        // 36 weeks: 4 weeks missing = 28 days -> 10/01/2025 + 28d = 07/02/2025 -> to 10/06/2025 = 0a 4m 3d
        viewModel.updateEgSemanas(36)
        assertEquals(28, viewModel.uiState.value.diasPrematuridad)
        assertEquals("07/02/2025", viewModel.uiState.value.fechaAjustada)
        assertEquals("0a 4m 3d", viewModel.uiState.value.edadCorregidaAMD)

        // 4. Full-term infants (>= 37 weeks: 37, 38, 40 weeks) -> "No aplica edad corregida"
        for (termWeeks in listOf(37, 38, 40)) {
            viewModel.updateEgSemanas(termWeeks)
            assertFalse(viewModel.uiState.value.esPrematuro)
            assertFalse(viewModel.uiState.value.aplicaEdadCorregida)
            assertEquals("No aplica edad corregida", viewModel.uiState.value.edadCorregidaAMD)
            assertEquals("No aplica edad corregida", viewModel.uiState.value.edadCorregidaCompleta)
        }

        // 5. Boundary at 2 years of chronological age (< 2 years vs == 2 years vs > 2 years)
        viewModel.updateEgSemanas(32) // Preterm 32 weeks
        viewModel.updateFechaNacimiento("15/05/2024")

        // Under 2 years (1 day before turning 2: 14/05/2026 -> 1a 11m 29d) -> Corrected age applies
        viewModel.updateFechaEvaluacion("14/05/2026")
        assertEquals("1a 11m 29d", viewModel.uiState.value.edadCronologicaAMD)
        assertTrue(viewModel.uiState.value.aplicaEdadCorregida)
        assertEquals("10/07/2024", viewModel.uiState.value.fechaAjustada)
        assertEquals("1a 10m 4d", viewModel.uiState.value.edadCorregidaAMD)

        // Exactly 2 years (15/05/2026 -> 2a 0m 0d) -> "No aplica edad corregida"
        viewModel.updateFechaEvaluacion("15/05/2026")
        assertEquals("2a 0m 0d", viewModel.uiState.value.edadCronologicaAMD)
        assertEquals("2 años, 0 meses y 0 días", viewModel.uiState.value.edadCronologicaCompleta)
        assertFalse(viewModel.uiState.value.aplicaEdadCorregida)
        assertEquals("No aplica edad corregida", viewModel.uiState.value.edadCorregidaAMD)
        assertEquals("No aplica edad corregida", viewModel.uiState.value.edadCorregidaCompleta)

        // Over 2 years (20/08/2026 -> 2a 3m 5d) -> "No aplica edad corregida"
        viewModel.updateFechaEvaluacion("20/08/2026")
        assertEquals("2a 3m 5d", viewModel.uiState.value.edadCronologicaAMD)
        assertFalse(viewModel.uiState.value.aplicaEdadCorregida)
        assertEquals("No aplica edad corregida", viewModel.uiState.value.edadCorregidaAMD)

        // 6. End-of-month dates (31, 30, 28, 29 days)
        viewModel.togglePrematurez(false)
        // Born 31/01/2025, evaluated 28/02/2025 (non-leap year end of Feb) -> 28 calendar days (0a 0m 28d)
        viewModel.updateFechaNacimiento("31/01/2025")
        viewModel.updateFechaEvaluacion("28/02/2025")
        assertEquals("0a 0m 28d", viewModel.uiState.value.edadCronologicaAMD)

        // Born 31/01/2025, evaluated 02/03/2025 -> 1 month (to 28/02/2025) + 2 days = 0a 1m 2d
        viewModel.updateFechaEvaluacion("02/03/2025")
        assertEquals("0a 1m 2d", viewModel.uiState.value.edadCronologicaAMD)

        // Born 31/10/2025, evaluated 30/11/2025 -> 0a 0m 30d; evaluated 01/12/2025 -> 0a 1m 1d
        viewModel.updateFechaNacimiento("31/10/2025")
        viewModel.updateFechaEvaluacion("30/11/2025")
        assertEquals("0a 0m 30d", viewModel.uiState.value.edadCronologicaAMD)
        viewModel.updateFechaEvaluacion("01/12/2025")
        assertEquals("0a 1m 1d", viewModel.uiState.value.edadCronologicaAMD)

        // 7. Leap year vs Non-leap year February
        // Leap year 2024: 15/02/2024 to 10/03/2024 -> Feb has 29 days -> (29 - 15) + 10 = 24 days (0a 0m 24d)
        viewModel.updateFechaNacimiento("15/02/2024")
        viewModel.updateFechaEvaluacion("10/03/2024")
        assertEquals("0a 0m 24d", viewModel.uiState.value.edadCronologicaAMD)

        // Non-leap year 2025: 15/02/2025 to 10/03/2025 -> Feb has 28 days -> (28 - 15) + 10 = 23 days (0a 0m 23d)
        viewModel.updateFechaNacimiento("15/02/2025")
        viewModel.updateFechaEvaluacion("10/03/2025")
        assertEquals("0a 0m 23d", viewModel.uiState.value.edadCronologicaAMD)

        // Born on leap day 29/02/2024:
        // Evaluated 28/02/2025 -> 0a 11m 30d; evaluated 01/03/2025 -> 1a 0m 1d; evaluated 29/02/2028 -> 4a 0m 0d
        viewModel.updateFechaNacimiento("29/02/2024")
        viewModel.updateFechaEvaluacion("28/02/2025")
        assertEquals("0a 11m 30d", viewModel.uiState.value.edadCronologicaAMD)
        viewModel.updateFechaEvaluacion("01/03/2025")
        assertEquals("1a 0m 1d", viewModel.uiState.value.edadCronologicaAMD)
        viewModel.updateFechaEvaluacion("29/02/2028")
        assertEquals("4a 0m 0d", viewModel.uiState.value.edadCronologicaAMD)
    }

    @Test
    fun `verify who growth curves and nutritional classifications across all indicators`() {
        val viewModel = CredEdiViewModel()
        viewModel.updateFechaNacimiento("15/10/2024")
        viewModel.updateFechaEvaluacion("15/02/2025")
        viewModel.togglePrematurez(true)
        viewModel.confirmCurrentEgSemanas()
        // ANTRO-02: Explicitly enter weight and height now that cold start defaults to Double.NaN ("Sin medición")
        viewModel.updatePesoInput("5.40")
        viewModel.updateLongitudInput("61.5")
        viewModel.updatePerimetroCefalicoInput("39.5")

        // 1. 2m corrected Boy (5.40 kg, 61.5 cm): Normal / Eutrófico across P/E, T/E, P/T, IMC
        val state0 = viewModel.uiState.value
        assertEquals(CutoffBand.NORMAL_RANGE, state0.evalPE.activeBand)
        assertEquals("Eutrófico", state0.evalPE.shortClassification)
        assertEquals(CutoffBand.NORMAL_RANGE, state0.evalTE.activeBand)
        assertEquals("Adecuada", state0.evalTE.shortClassification)
        assertEquals(CutoffBand.NORMAL_RANGE, state0.evalPT.activeBand)
        assertEquals("Normal", state0.evalPT.shortClassification)
        assertEquals(CutoffBand.NORMAL_RANGE, state0.evalIMCE.activeBand)
        assertEquals("Normal", state0.evalIMCE.shortClassification)
        assertEquals(ClassificationSeverity.NORMAL, state0.globalDiagnosis.severity)

        // 2. Switch indicator tabs in Curvas OMS (P/E -> T/E -> P/T -> IMC)
        viewModel.selectCurvaIndicador(CurvaIndicador.TE)
        assertEquals(CurvaIndicador.TE, viewModel.uiState.value.currentCurvaEvaluation.indicador)
        assertEquals("cm", viewModel.uiState.value.currentCurvaEvaluation.unit)

        viewModel.selectCurvaIndicador(CurvaIndicador.PT)
        assertEquals(CurvaIndicador.PT, viewModel.uiState.value.currentCurvaEvaluation.indicador)
        assertEquals(61.5f, viewModel.uiState.value.currentCurvaEvaluation.patientX, 0.01f)

        viewModel.selectCurvaIndicador(CurvaIndicador.IMCE)
        assertEquals(CurvaIndicador.IMCE, viewModel.uiState.value.currentCurvaEvaluation.indicador)
        assertEquals("kg/m²", viewModel.uiState.value.currentCurvaEvaluation.unit)

        viewModel.selectCurvaIndicador(CurvaIndicador.PCE)
        assertEquals(CurvaIndicador.PCE, viewModel.uiState.value.currentCurvaEvaluation.indicador)
        assertEquals("cm", viewModel.uiState.value.currentCurvaEvaluation.unit)
        assertEquals("Normocéfalo", viewModel.uiState.value.currentCurvaEvaluation.shortClassification)
        assertEquals(39.5, viewModel.uiState.value.currentCurvaEvaluation.patientValue, 0.01)

        // 3. Adjust weight to trigger Overweight/Obesity (> 6.30 kg at 2m Boy)
        viewModel.adjustPeso(1.20) // 5.40 + 1.20 = 6.60 kg
        val overweightState = viewModel.uiState.value
        assertEquals(CutoffBand.PLUS_2_TO_3, overweightState.evalPE.activeBand)
        assertEquals("Sobrepeso", overweightState.evalPE.shortClassification)
        assertEquals(ClassificationSeverity.WARNING, overweightState.globalDiagnosis.severity)
    }

    @Test
    fun `verify next cred control schedule per nts 238 minsa`() {
        val viewModel = CredEdiViewModel()
        viewModel.updateFechaNacimiento("15/10/2024")
        viewModel.updateFechaEvaluacion("15/02/2025")
        viewModel.togglePrematurez(true)
        assertEquals("Pendiente de Edad Corregida", viewModel.uiState.value.proximoControlCorregido.fechaFormateada)
        viewModel.confirmCurrentEgSemanas()

        // F.Nac 15/10/2024, F.Eval 15/02/2025
        // Chronological age = 4m 0d -> 4° Control (4m) is Bimestral (En 60 días) -> 15/04/2025 (15 de Abril, 2025)
        // Corrected age (32w EG) = 2m 0d -> 2° Control (2m) is Mensual (En 30 días) -> 15/03/2025 (15 de Marzo, 2025)
        val defaultState = viewModel.uiState.value
        assertEquals("15 de Abril, 2025", defaultState.proximoControlCronologico.fechaFormateada)
        assertEquals("15/04/2025", defaultState.proximoControlCronologico.fechaCorta)
        assertEquals("En 60 días", defaultState.proximoControlCronologico.intervaloBadge)

        assertEquals("15 de Marzo, 2025", defaultState.proximoControlCorregido.fechaFormateada)
        assertEquals("15/03/2025", defaultState.proximoControlCorregido.fechaCorta)
        assertEquals("En 30 días", defaultState.proximoControlCorregido.intervaloBadge)

        // Newborn (10 days old: 05/02/2025 -> 15/02/2025): Semanal (En 7 días) -> 22/02/2025
        viewModel.clearEdiRegisteredEg()
        viewModel.togglePrematurez(false)
        viewModel.updateFechaNacimiento("05/02/2025")
        viewModel.updateFechaEvaluacion("15/02/2025")
        assertEquals("22 de Febrero, 2025", viewModel.uiState.value.proximoControlInfo.fechaFormateada)
        assertEquals("En 7 días", viewModel.uiState.value.proximoControlInfo.intervaloBadge)

        // 6 months old (15/08/2024 -> 15/02/2025): 5° Control (6m) is Mensual (En 30 días) -> 15/03/2025
        viewModel.updateFechaNacimiento("15/08/2024")
        assertEquals("15 de Marzo, 2025", viewModel.uiState.value.proximoControlInfo.fechaFormateada)
        assertEquals("En 30 días", viewModel.uiState.value.proximoControlInfo.intervaloBadge)

        // 7 months old (15/07/2024 -> 15/02/2025): 6° Control (7m) is Bimestral (En 60 días) -> 15/04/2025
        viewModel.updateFechaNacimiento("15/07/2024")
        assertEquals("15 de Abril, 2025", viewModel.uiState.value.proximoControlInfo.fechaFormateada)
        assertEquals("En 60 días", viewModel.uiState.value.proximoControlInfo.intervaloBadge)

        // 9 months old (15/05/2024 -> 15/02/2025): 7° Control (9m) -> 12m is Trimestral (En 90 días) -> 15/05/2025
        viewModel.updateFechaNacimiento("15/05/2024")
        assertEquals("15 de Mayo, 2025", viewModel.uiState.value.proximoControlInfo.fechaFormateada)
        assertEquals("En 90 días", viewModel.uiState.value.proximoControlInfo.intervaloBadge)

        // 2 years old / 24 months (15/02/2023 -> 15/02/2025): Semestral (En 180 días) -> 15/08/2025
        viewModel.updateFechaNacimiento("15/02/2023")
        assertEquals("15 de Agosto, 2025", viewModel.uiState.value.proximoControlInfo.fechaFormateada)
        assertEquals("En 180 días", viewModel.uiState.value.proximoControlInfo.intervaloBadge)

        // 5 years old / 60 months (15/02/2020 -> 15/02/2025): Anual (En 360 días) -> 15/02/2026
        viewModel.updateFechaNacimiento("15/02/2020")
        assertEquals("15 de Febrero, 2026", viewModel.uiState.value.proximoControlInfo.fechaFormateada)
        assertEquals("En 360 días", viewModel.uiState.value.proximoControlInfo.intervaloBadge)
    }

    @Test
    fun `verify manual edi 2026 evaluation across all 4 axes and semaforo`() {
        val viewModel = CredEdiViewModel()

        // 0. By default, ALL hitos and items across the 4 Ejes are disabled (false) and free to mark manually
        val defaultEdi = viewModel.uiState.value
        assertEquals(0, defaultEdi.frbDetectadosCount)
        assertTrue(defaultEdi.frbItems.all { !it.checked })
        assertEquals(0, defaultEdi.saPresentesCount)
        assertTrue(defaultEdi.saItems.all { !it.isPresent })
        assertEquals(0, defaultEdi.hitosLogradosCount)
        assertTrue(defaultEdi.areaItems.all { !it.isAchieved })
        assertEquals(0, defaultEdi.enItemsMarcadosCount)
        assertFalse(defaultEdi.enTonoSimetrico)
        assertFalse(defaultEdi.enMoroPresente)
        assertFalse(defaultEdi.enSuccionRitmica)
        assertFalse(defaultEdi.enPrensionPresente)
        assertFalse(defaultEdi.enMovilidadOcularNormal)

        viewModel.updateFechaNacimiento("15/10/2024")
        viewModel.updateFechaEvaluacion("15/02/2025")
        viewModel.togglePrematurez(true)
        viewModel.confirmCurrentEgSemanas()

        // 1. Manually mark all 5 hitos of Eje 3 and all 5 neurological items of Eje 4 -> VERDE
        for (i in 0..4) {
            viewModel.setAreaItemAchieved(i, true)
        }
        viewModel.toggleEnTono()
        viewModel.toggleEnMoro()
        viewModel.toggleEnSuccion()
        viewModel.toggleEnPrension()
        viewModel.toggleEnMovilidadOcular()

        val s0 = viewModel.uiState.value
        assertEquals("Grupo 3 (1m a 2m 29d)", s0.ediGrupoEtario)
        assertEquals(5, s0.hitosLogradosCount)
        assertEquals(0, s0.saPresentesCount)
        assertEquals(5, s0.enItemsMarcadosCount)
        assertTrue(s0.exploracionNeurologicaNormal)
        assertEquals(com.example.model.SemaforoColor.VERDE, s0.semaforoCalculado)

        // 2. Failing 1 developmental area (4/5 hitos, e.g. MG = No) -> AMARILLO (Rezago)
        viewModel.setAreaItemAchieved(0, false)
        assertEquals(4, viewModel.uiState.value.hitosLogradosCount)
        assertEquals(com.example.model.SemaforoColor.AMARILLO, viewModel.uiState.value.semaforoCalculado)

        // 3. Failing >2 developmental areas (2/5 hitos) -> ROJO (Riesgo de retraso)
        viewModel.setAreaItemAchieved(1, false)
        viewModel.setAreaItemAchieved(2, false)
        assertEquals(2, viewModel.uiState.value.hitosLogradosCount)
        assertEquals(com.example.model.SemaforoColor.ROJO, viewModel.uiState.value.semaforoCalculado)

        // Restore all 5/5 developmental areas -> back to VERDE
        viewModel.setAreaItemAchieved(0, true)
        viewModel.setAreaItemAchieved(1, true)
        viewModel.setAreaItemAchieved(2, true)
        assertEquals(com.example.model.SemaforoColor.VERDE, viewModel.uiState.value.semaforoCalculado)

        // 4. Neurological exploration abnormality (EN 1.1 Tono alterado) -> ROJO
        viewModel.toggleEnTono()
        assertFalse(viewModel.uiState.value.exploracionNeurologicaNormal)
        assertEquals(com.example.model.SemaforoColor.ROJO, viewModel.uiState.value.semaforoCalculado)
        viewModel.toggleEnTono()
        // 5. Verification of registered evaluation state
        assertTrue(viewModel.uiState.value.exploracionNeurologicaNormal)
    }

    @Test
    fun `verify annual date adjustment and gestational age week input`() {
        val viewModel = CredEdiViewModel()

        // 1. Default evaluation date is today, and can be modified + stepped annually
        assertEquals(com.example.model.ClinicalAgeCalculator.todayFormatted(), viewModel.uiState.value.fechaEvaluacion)
        viewModel.updateFechaEvaluacion("15/02/2025")
        assertEquals("15/02/2025", viewModel.uiState.value.fechaEvaluacion)
        viewModel.adjustEdiFechaEvalYear(1)
        assertEquals("15/02/2026", viewModel.uiState.value.fechaEvaluacion)
        assertEquals("15/02/2026", viewModel.uiState.value.ediFechaEval)

        viewModel.adjustEdiFechaEvalYear(-1)
        assertEquals("15/02/2025", viewModel.uiState.value.fechaEvaluacion)
        assertEquals("15/02/2025", viewModel.uiState.value.ediFechaEval)

        // 2. Typing intermediate date without premature revert
        viewModel.updateEdiFechaEval("20/08/202")
        assertEquals("20/08/202", viewModel.uiState.value.ediFechaEval)
        // Completing valid date
        viewModel.updateEdiFechaEval("20/08/2025")
        assertEquals("20/08/2025", viewModel.uiState.value.fechaEvaluacion)
        assertEquals("20/08/2025", viewModel.uiState.value.ediFechaEval)

        // 3. Gestational age week adjustment and input
        // Prematurity is disabled by default, and can be manually enabled
        assertEquals(32, viewModel.uiState.value.egSemanas)
        assertFalse(viewModel.uiState.value.esPrematuro)
        assertFalse(viewModel.uiState.value.frbItems.first { it.id == 1 }.checked)
        viewModel.togglePrematurez(true)
        assertTrue(viewModel.uiState.value.esPrematuro)
        assertFalse(viewModel.uiState.value.frbItems.first { it.id == 1 }.checked)
        viewModel.confirmCurrentEgSemanas()
        assertTrue(viewModel.uiState.value.frbItems.first { it.id == 1 }.checked)
        assertTrue(viewModel.uiState.value.frbItems.first { it.id == 1 }.title.contains("32 sem EG"))

        // Adjust +1 week to 33
        viewModel.adjustEdiEgSemanas(1)
        assertEquals(33, viewModel.uiState.value.egSemanas)
        assertEquals("33", viewModel.uiState.value.ediEgSemanasInput)
        assertTrue(viewModel.uiState.value.esPrematuro)
        assertTrue(viewModel.uiState.value.frbItems.first { it.id == 1 }.title.contains("33 sem EG"))

        // Input full-term (e.g. 39 weeks)
        viewModel.updateEdiEgSemanasInput("39")
        assertEquals(39, viewModel.uiState.value.egSemanas)
        assertEquals("39", viewModel.uiState.value.ediEgSemanasInput)
        assertFalse(viewModel.uiState.value.esPrematuro)
        assertFalse(viewModel.uiState.value.frbItems.first { it.id == 1 }.checked)
        assertTrue(viewModel.uiState.value.frbItems.first { it.id == 1 }.title.contains("A término (39 sem EG)"))
        // At term, corrected age equals chronological age
        assertEquals(viewModel.uiState.value.edadCronologicaMeses, viewModel.uiState.value.edadCorregidaMeses)
        assertEquals(viewModel.uiState.value.edadCronologicaDias, viewModel.uiState.value.edadCorregidaDias)

        // Input preterm again (e.g. 30 weeks)
        viewModel.updateEdiEgSemanasInput("30")
        assertEquals(30, viewModel.uiState.value.egSemanas)
        assertTrue(viewModel.uiState.value.esPrematuro)
        assertTrue(viewModel.uiState.value.frbItems.first { it.id == 1 }.checked)
        assertEquals(10, viewModel.uiState.value.deficitSemanas) // (40 - 30) = 10 weeks
    }

    @Test
    fun `verify export official edi pdf generation with prefilled clinical evaluation`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val viewModel = CredEdiViewModel()
        val state = viewModel.uiState.value

        val exportResult = com.example.util.EdiPdfExporter.exportarFichaOficialEdiPdf(context, state)
        assertTrue(exportResult.message, exportResult.success)
        org.junit.Assert.assertNotNull(exportResult.filePath)
        val file = java.io.File(exportResult.filePath!!)
        assertTrue(file.exists())
        assertTrue("PDF file size should be > 10KB", file.length() > 10000)
    }

    @Test
    fun `verify semaforo is computed outcome from registered evaluation data and not manual selection`() {
        val viewModel = CredEdiViewModel()

        // Initially all hitos across axes are disabled (false) -> Resultado Rojo until evaluated
        assertEquals(0, viewModel.uiState.value.hitosLogradosCount)
        assertEquals(0, viewModel.uiState.value.enItemsMarcadosCount)
        assertEquals(com.example.model.SemaforoColor.ROJO, viewModel.uiState.value.semaforoCalculado)

        // Enter evaluation modal, manually mark all 5 hitos of Eje 3 and 5 items of Eje 4 -> Resultado Verde
        viewModel.openDatosEdiModal()
        assertTrue(viewModel.uiState.value.showDatosEdiModal)
        for (i in 0..4) {
            viewModel.setAreaItemAchieved(i, true)
        }
        viewModel.toggleEnTono()
        viewModel.toggleEnMoro()
        viewModel.toggleEnSuccion()
        viewModel.toggleEnPrension()
        viewModel.toggleEnMovilidadOcular()
        viewModel.guardarYCalcularSemaforoEdi()
        assertFalse(viewModel.uiState.value.showDatosEdiModal)
        assertEquals(com.example.model.SemaforoColor.VERDE, viewModel.uiState.value.semaforoCalculado)

        // Enter evaluation modal, register a warning sign (SA): Resultado becomes Amarillo (Rezago)
        viewModel.openDatosEdiModal()
        assertTrue(viewModel.uiState.value.showDatosEdiModal)
        viewModel.setSaItemPresent(0, true)
        viewModel.guardarYCalcularSemaforoEdi()
        assertFalse(viewModel.uiState.value.showDatosEdiModal)
        assertEquals(com.example.model.SemaforoColor.AMARILLO, viewModel.uiState.value.semaforoCalculado)

        // Enter evaluation modal, register neurological alteration: Resultado becomes Rojo (Riesgo)
        viewModel.openDatosEdiModal()
        viewModel.toggleEnTono()
        viewModel.guardarYCalcularSemaforoEdi()
        assertEquals(com.example.model.SemaforoColor.ROJO, viewModel.uiState.value.semaforoCalculado)

        // Reset neurological alteration and SA: Resultado returns strictly to Verde
        viewModel.toggleEnTono()
        viewModel.setSaItemPresent(0, false)
        viewModel.guardarYCalcularSemaforoEdi()
        assertEquals(com.example.model.SemaforoColor.VERDE, viewModel.uiState.value.semaforoCalculado)
    }

    @Test
    fun `verify anthropometric measurement fields are empty by default and update dynamically`() {
        val viewModel = CredEdiViewModel()

        // 1. Empty by default so user can freely fill them in
        val initial = viewModel.uiState.value
        assertEquals("", initial.pesoInput)
        assertEquals("", initial.longitudInput)
        assertEquals("", initial.perimetroCefalicoInput)
        assertFalse(initial.hasPesoIngresado)
        assertFalse(initial.hasLongitudIngresada)
        assertFalse(initial.hasPerimetroCefalicoIngresado)
        assertEquals(0, initial.datosAntropometricosCount)

        // 2. Typing values updates both raw input string and numeric clinical measurements
        viewModel.updatePesoInput("6.20")
        viewModel.updateLongitudInput("63.5")
        viewModel.updatePerimetroCefalicoInput("40.2")

        val updated = viewModel.uiState.value
        assertEquals("6.20", updated.pesoInput)
        assertEquals("63.5", updated.longitudInput)
        assertEquals("40.2", updated.perimetroCefalicoInput)
        assertEquals(6.20, updated.pesoKg, 0.001)
        assertEquals(63.5, updated.longitudCm, 0.001)
        assertEquals(40.2, updated.perimetroCefalicoCm, 0.001)
        assertTrue(updated.hasPesoIngresado)
        assertTrue(updated.hasLongitudIngresada)
        assertTrue(updated.hasPerimetroCefalicoIngresado)
        assertEquals(3, updated.datosAntropometricosCount)
    }

    @Test
    fun `verify curvas oms pulls exact amd age and updates chart by classified group`() {
        val viewModel = CredEdiViewModel()

        // 1. Chronological age with days: Birth 01/10/2024, Eval 16/02/2025 -> 0a 4m 15d (prematurity off)
        viewModel.updateFechaNacimiento("01/10/2024")
        viewModel.updateFechaEvaluacion("16/02/2025")
        viewModel.updatePesoInput("7.00")
        viewModel.updateLongitudInput("64.0")
        viewModel.updatePerimetroCefalicoInput("41.5")

        val stateCrono = viewModel.uiState.value
        assertFalse(stateCrono.usaEdadCorregida)
        assertEquals("0a 4m 15d", stateCrono.edadEfectivaAMD)
        assertEquals("Edad Cronológica", stateCrono.edadEfectivaTipoLabel)
        assertEquals("0 a 2 años (0-24m)", stateCrono.grupoEtarioOmsClasificado)
        assertEquals("0a 4m 15d", stateCrono.evalPE.edadAMD)
        // 4 months + 15 days = 4.5f on the chart X-axis
        assertEquals(4.5f, stateCrono.evalPE.patientX, 0.02f)
        assertEquals(0f, stateCrono.evalPE.xMin, 0.01f)
        assertEquals(24f, stateCrono.evalPE.xMax, 0.01f)
        assertTrue(stateCrono.evalPE.grupoEtarioTitle.contains("0 a 2 años (0-24m)"))

        // 2. Enable prematurity and confirm 32w EG (-56 days): Corrected age becomes 0a 2m 19d
        viewModel.togglePrematurez(true)
        viewModel.confirmCurrentEgSemanas()
        val stateCorr = viewModel.uiState.value
        assertTrue(stateCorr.usaEdadCorregida)
        assertEquals(stateCorr.edadCorregidaAMD, stateCorr.edadEfectivaAMD)
        assertEquals("Edad Corregida", stateCorr.edadEfectivaTipoLabel)
        assertEquals(stateCorr.edadCorregidaAMD, stateCorr.evalPE.edadAMD)
        val expectedXCorr = stateCorr.edadCorregidaMeses + (stateCorr.edadCorregidaDias / 30f)
        assertEquals(expectedXCorr, stateCorr.evalPE.patientX, 0.02f)

        // 3. Classified group 2 a 5 años (24-60m): Birth 01/01/2022, Eval 16/02/2025 -> 3a 1m 15d (37m 15d)
        // Prematurity correction no longer applies at >= 24 months per NTS 238
        viewModel.updateFechaNacimiento("01/01/2022")
        viewModel.updatePesoInput("14.60")
        viewModel.updateLongitudInput("96.5")
        val state2to5 = viewModel.uiState.value
        assertFalse(state2to5.usaEdadCorregida)
        assertEquals("3a 1m 15d", state2to5.edadEfectivaAMD)
        assertEquals("2 a 5 años (24-60m)", state2to5.grupoEtarioOmsClasificado)
        assertEquals("3a 1m 15d", state2to5.evalPE.edadAMD)
        assertEquals(37.5f, state2to5.evalPE.patientX, 0.02f)
        assertEquals(24f, state2to5.evalPE.xMin, 0.01f)
        assertEquals(60f, state2to5.evalPE.xMax, 0.01f)
        assertTrue(state2to5.evalPE.grupoEtarioTitle.contains("2 a 5 años (24-60m)"))
    }
}


