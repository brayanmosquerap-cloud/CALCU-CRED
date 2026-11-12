package com.example

import com.example.model.ClinicalAgeCalculator
import com.example.viewmodel.CredEdiViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ClinicalAgeCalculatorTest {

    @Test
    fun `regression test case nts 238 - 12_03_2025 to 10_11_2026 at 34 weeks`() {
        // Caso de regresión obligatorio:
        // Nacimiento: 12/03/2025
        // Evaluación: 10/11/2026
        // Edad gestacional: 34 semanas
        // Edad cronológica esperada: 1 año, 7 meses y 29 días
        // Fecha ajustada: 23/04/2025
        // Edad corregida esperada: 1 año, 6 meses y 18 días
        val result = ClinicalAgeCalculator.calculateAges(
            fechaNacimiento = "12/03/2025",
            fechaEvaluacion = "10/11/2026",
            esPrematuro = true,
            egSemanas = 34
        )

        assertEquals(1, result.cronologicaAnios)
        assertEquals(7, result.cronologicaMesesDelAnio)
        assertEquals(29, result.cronologicaDias)
        assertEquals("1a 7m 29d", result.edadCronologicaAMD)
        assertEquals("1 año, 7 meses y 29 días", result.edadCronologicaCompleta)

        assertTrue(result.aplicaEdadCorregida)
        assertEquals(6, result.semanasPrematuridad)
        assertEquals(42, result.diasRestarPrematuridad)
        assertEquals("23/04/2025", result.fechaAjustada)

        assertEquals(1, result.corregidaAnios)
        assertEquals(6, result.corregidaMesesDelAnio)
        assertEquals(18, result.corregidaDias)
        assertEquals("1a 6m 18d", result.edadCorregidaAMD)
        assertEquals("1 año, 6 meses y 18 días", result.edadCorregidaCompleta)
    }

    @Test
    fun `preterm gestational ages 28, 32, 34, and 36 weeks`() {
        val viewModel = CredEdiViewModel()
        viewModel.updateFechaNacimiento("10/01/2025")
        viewModel.updateFechaEvaluacion("10/06/2025")
        viewModel.togglePrematurez(true)

        // Edad cronológica común: 0 años, 5 meses y 0 días
        assertEquals("0a 5m 0d", viewModel.uiState.value.edadCronologicaAMD)
        assertEquals("0 años, 5 meses y 0 días", viewModel.uiState.value.edadCronologicaCompleta)

        // 28 semanas: faltan 12 semanas = 84 días -> 10/01/2025 + 84d = 04/04/2025 -> al 10/06/2025 = 0a 2m 6d
        viewModel.updateEgSemanas(28)
        assertEquals(84, viewModel.uiState.value.diasPrematuridad)
        assertEquals("04/04/2025", viewModel.uiState.value.fechaAjustada)
        assertEquals("0a 2m 6d", viewModel.uiState.value.edadCorregidaAMD)
        assertEquals("0 años, 2 meses y 6 días", viewModel.uiState.value.edadCorregidaCompleta)

        // 32 semanas: faltan 8 semanas = 56 días -> 10/01/2025 + 56d = 07/03/2025 -> al 10/06/2025 = 0a 3m 3d
        viewModel.updateEgSemanas(32)
        assertEquals(56, viewModel.uiState.value.diasPrematuridad)
        assertEquals("07/03/2025", viewModel.uiState.value.fechaAjustada)
        assertEquals("0a 3m 3d", viewModel.uiState.value.edadCorregidaAMD)
        assertEquals("0 años, 3 meses y 3 días", viewModel.uiState.value.edadCorregidaCompleta)

        // 34 semanas: faltan 6 semanas = 42 días -> 10/01/2025 + 42d = 21/02/2025 -> al 10/06/2025 = 0a 3m 20d
        viewModel.updateEgSemanas(34)
        assertEquals(42, viewModel.uiState.value.diasPrematuridad)
        assertEquals("21/02/2025", viewModel.uiState.value.fechaAjustada)
        assertEquals("0a 3m 20d", viewModel.uiState.value.edadCorregidaAMD)
        assertEquals("0 años, 3 meses y 20 días", viewModel.uiState.value.edadCorregidaCompleta)

        // 36 semanas: faltan 4 semanas = 28 días -> 10/01/2025 + 28d = 07/02/2025 -> al 10/06/2025 = 0a 4m 3d
        viewModel.updateEgSemanas(36)
        assertEquals(28, viewModel.uiState.value.diasPrematuridad)
        assertEquals("07/02/2025", viewModel.uiState.value.fechaAjustada)
        assertEquals("0a 4m 3d", viewModel.uiState.value.edadCorregidaAMD)
        assertEquals("0 años, 4 meses y 3 días", viewModel.uiState.value.edadCorregidaCompleta)
    }

    @Test
    fun `full term infants do not apply corrected age`() {
        val viewModel = CredEdiViewModel()
        viewModel.updateFechaNacimiento("12/03/2025")
        viewModel.updateFechaEvaluacion("10/11/2026")

        for (eg in listOf(37, 38, 39, 40, 41)) {
            viewModel.updateEgSemanas(eg)
            val s = viewModel.uiState.value
            assertFalse("EG $eg should not apply corrected age", s.aplicaEdadCorregida)
            assertEquals("No aplica edad corregida", s.edadCorregidaAMD)
            assertEquals("No aplica edad corregida", s.edadCorregidaCompleta)
            assertEquals("1a 7m 29d", s.edadEfectivaAMD)
        }
    }

    @Test
    fun `children under 2 years vs exactly 2 years vs over 2 years`() {
        val viewModel = CredEdiViewModel()
        viewModel.togglePrematurez(true)
        viewModel.updateEgSemanas(34) // 42 días de prematuridad
        viewModel.updateFechaNacimiento("12/03/2025")

        // Menor de 2 años (11/03/2027 -> 1 año, 11 meses y 27 días) -> Sí aplica edad corregida
        viewModel.updateFechaEvaluacion("11/03/2027")
        assertEquals("1a 11m 27d", viewModel.uiState.value.edadCronologicaAMD)
        assertTrue(viewModel.uiState.value.aplicaEdadCorregida)
        assertEquals("23/04/2025", viewModel.uiState.value.fechaAjustada)
        assertEquals("1a 10m 16d", viewModel.uiState.value.edadCorregidaAMD)

        // Cumple exactamente 2 años (12/03/2027 -> 2 años, 0 meses y 0 días) -> No aplica edad corregida
        viewModel.updateFechaEvaluacion("12/03/2027")
        assertEquals("2a 0m 0d", viewModel.uiState.value.edadCronologicaAMD)
        assertEquals("2 años, 0 meses y 0 días", viewModel.uiState.value.edadCronologicaCompleta)
        assertFalse(viewModel.uiState.value.aplicaEdadCorregida)
        assertEquals("No aplica edad corregida", viewModel.uiState.value.edadCorregidaAMD)
        assertEquals("No aplica edad corregida", viewModel.uiState.value.edadCorregidaCompleta)

        // Mayor de 2 años (15/06/2027 -> 2 años, 3 meses y 3 días) -> No aplica edad corregida
        viewModel.updateFechaEvaluacion("15/06/2027")
        assertEquals("2a 3m 3d", viewModel.uiState.value.edadCronologicaAMD)
        assertFalse(viewModel.uiState.value.aplicaEdadCorregida)
        assertEquals("No aplica edad corregida", viewModel.uiState.value.edadCorregidaAMD)
    }

    @Test
    fun `end of month dates and leap years accuracy`() {
        // Día de nacimiento como día cero
        val dayZero = ClinicalAgeCalculator.calculateAges("28/02/2025", "28/02/2025", false, 40)
        assertEquals("0a 0m 0d", dayZero.edadCronologicaAMD)

        // Fin de mes enero (31 días) a febrero no bisiesto (28 días) y marzo
        // Del 31/01/2025 al 28/02/2025 transcurren exactamente 28 días calendario (0a 0m 28d)
        assertEquals(
            "0a 0m 28d",
            ClinicalAgeCalculator.calculateAges("31/01/2025", "28/02/2025", false, 40).edadCronologicaAMD
        )
        // Del 31/01/2025 al 01/03/2025: 1 mes calendario (al 28/02/2025) + 1 día = 0a 1m 1d
        assertEquals(
            "0a 1m 1d",
            ClinicalAgeCalculator.calculateAges("31/01/2025", "01/03/2025", false, 40).edadCronologicaAMD
        )
        assertEquals(
            "0a 1m 3d",
            ClinicalAgeCalculator.calculateAges("30/01/2025", "03/03/2025", false, 40).edadCronologicaAMD
        )
        // Fin de mes octubre (31 días) a noviembre (30 días) y diciembre
        assertEquals(
            "0a 0m 30d",
            ClinicalAgeCalculator.calculateAges("31/10/2025", "30/11/2025", false, 40).edadCronologicaAMD
        )
        assertEquals(
            "0a 1m 1d",
            ClinicalAgeCalculator.calculateAges("31/10/2025", "01/12/2025", false, 40).edadCronologicaAMD
        )

        // Año bisiesto 2024 vs año no bisiesto 2025 en febrero
        assertEquals(
            "0a 0m 24d",
            ClinicalAgeCalculator.calculateAges("15/02/2024", "10/03/2024", false, 40).edadCronologicaAMD
        )
        assertEquals(
            "0a 0m 23d",
            ClinicalAgeCalculator.calculateAges("15/02/2025", "10/03/2025", false, 40).edadCronologicaAMD
        )

        // Nacido en 29 de febrero (año bisiesto 2024)
        assertEquals(
            "0a 11m 29d",
            ClinicalAgeCalculator.calculateAges("29/02/2024", "27/02/2025", false, 40).edadCronologicaAMD
        )
        assertEquals(
            "0a 11m 30d",
            ClinicalAgeCalculator.calculateAges("29/02/2024", "28/02/2025", false, 40).edadCronologicaAMD
        )
        assertEquals(
            "1a 0m 1d",
            ClinicalAgeCalculator.calculateAges("29/02/2024", "01/03/2025", false, 40).edadCronologicaAMD
        )
        assertEquals(
            "4a 0m 0d",
            ClinicalAgeCalculator.calculateAges("29/02/2024", "29/02/2028", false, 40).edadCronologicaAMD
        )
    }
}
