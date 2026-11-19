package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.AppTab
import com.example.model.ClinicalAgeCalculator
import com.example.model.CredEdiUiState
import com.example.model.EdiGlobalTrafficLight
import com.example.model.EdiGroupChangeResolutionAction
import com.example.model.EdiItemResponse
import com.example.model.EdiOfficialCatalog
import com.example.util.EdiPdfExporter
import com.example.viewmodel.CredEdiViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Regresión dedicada para ALTO-01 (Auditoría Fase 5):
 * Garantiza que EdiPdfExporter nunca imprima valores antropométricos predeterminados
 * (5.40 kg, 61.5 cm, 39.5 cm) ni diagnósticos nutricionales correspondientes a mediciones
 * ausentes, eliminadas o sin datos suficientes, manteniendo intacto el semáforo EDI y CRED.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class EdiAlto01FooterAntropometriaTest {

    private fun context(): Context = ApplicationProvider.getApplicationContext()

    private fun prepareCleanGroup(vm: CredEdiViewModel, groupNumber: Int) {
        vm.selectTab(AppTab.EDI)
        vm.selectEdiGroupFromBank(groupNumber)
        if (vm.uiState.value.ediEngineState.pendingGroupChangeConfirmation != null) {
            vm.resolveEdiPendingGroupChange(
                EdiGroupChangeResolutionAction.DISCARD_PREVIOUS_CONFIRMED_AND_SWITCH_TO_CLEAN_TARGET
            )
        }
    }

    private fun completeAllGreenEdi(vm: CredEdiViewModel, groupNumber: Int) {
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

    private fun exportAndVerifyPdf(vm: CredEdiViewModel, afterName: String): File {
        val state = vm.uiState.value
        val result = EdiPdfExporter.exportarFichaOficialEdiPdf(context(), state)
        assertTrue("Falló exportación PDF ($afterName): ${result.message}", result.success)
        assertNotNull(result.filePath)
        val file = File(result.filePath!!)
        assertTrue(file.exists() && file.length() > 10_000L)
        val header = file.readBytes().copyOfRange(0, 5).toString(Charsets.US_ASCII)
        assertEquals("%PDF-", header)

        val outDir = File("build/alto01_before_after/after")
        if (!outDir.exists()) outDir.mkdirs()
        val copied = File(outDir, afterName)
        file.copyTo(copied, overwrite = true)
        return copied
    }

    @Test
    fun `01 ninguna medicion ingresada no imprime peso talla pc ni diagnosticos`() {
        val vm = CredEdiViewModel().apply {
            updateFechaNacimiento("10/12/2023")
            updateFechaEvaluacion("20/02/2025") // 14m 10d -> Grupo 8
            confirmEdiTermWithoutWeeks()
        }
        prepareCleanGroup(vm, 8)
        completeAllGreenEdi(vm, 8)

        val state = vm.uiState.value
        assertEquals(EdiGlobalTrafficLight.VERDE, state.ediGlobalEvaluation.trafficLight)

        val footer = EdiPdfExporter.buildCredFooterSummary(state)
        assertFalse(footer.contains("Peso:"))
        assertFalse(footer.contains("Talla:"))
        assertFalse(footer.contains("PC:"))
        assertFalse(footer.contains("5.40"))
        assertFalse(footer.contains("61.5"))
        assertFalse(footer.contains("39.5"))
        assertFalse(footer.contains(state.evalPE.shortClassification))
        assertFalse(footer.contains(state.evalTE.shortClassification))
        assertTrue(footer.contains("Ver Anexo de Trazabilidad en Página 2"))

        exportAndVerifyPdf(vm, "after_01_ninguna_medicion.pdf")
    }

    @Test
    fun `02 unicamente perimetro cefalico muestra solo PC sin peso talla ni diagnosticos falsos`() {
        val vm = CredEdiViewModel().apply {
            updateFechaNacimiento("01/06/2020")
            updateFechaEvaluacion("15/02/2025") // 56m 14d -> Grupo 14
            updatePerimetroCefalicoInput("50.6")
        }
        prepareCleanGroup(vm, 14)
        completeAllGreenEdi(vm, 14)

        val state = vm.uiState.value
        assertEquals(EdiGlobalTrafficLight.VERDE, state.ediGlobalEvaluation.trafficLight)

        val footer = EdiPdfExporter.buildCredFooterSummary(state)
        assertTrue("Debe incluir PC ingresado", footer.contains("PC: 50.6 cm"))
        assertFalse("Nunca debe imprimir peso default", footer.contains("Peso:"))
        assertFalse("Nunca debe imprimir 5.40", footer.contains("5.40"))
        assertFalse("Nunca debe imprimir talla default", footer.contains("Talla:"))
        assertFalse("Nunca debe imprimir 61.5", footer.contains("61.5"))
        assertFalse("Nunca debe imprimir diagnóstico P/E", footer.contains(state.evalPE.shortClassification))
        assertFalse("Nunca debe imprimir diagnóstico T/E", footer.contains(state.evalTE.shortClassification))

        exportAndVerifyPdf(vm, "after_02_solo_pc_g14.pdf")
    }

    @Test
    fun `03 unicamente peso muestra solo peso y su diagnostico PE sin talla ni PC`() {
        val vm = CredEdiViewModel().apply {
            updateFechaNacimiento("10/12/2023")
            updateFechaEvaluacion("20/02/2025") // 14m 10d -> Grupo 8
            confirmEdiTermWithoutWeeks()
            updatePesoInput("9.80")
        }
        prepareCleanGroup(vm, 8)
        completeAllGreenEdi(vm, 8)

        val state = vm.uiState.value
        val footer = EdiPdfExporter.buildCredFooterSummary(state)
        assertTrue(footer.contains("Peso: 9.80 kg (${state.evalPE.shortClassification})"))
        assertFalse(footer.contains("Talla:"))
        assertFalse(footer.contains("61.5"))
        assertFalse(footer.contains("PC:"))
        assertFalse(footer.contains("39.5"))
        assertFalse(footer.contains(state.evalTE.shortClassification))

        exportAndVerifyPdf(vm, "after_03_solo_peso.pdf")
    }

    @Test
    fun `04 unicamente talla muestra solo talla y su diagnostico TE sin peso ni PC`() {
        val vm = CredEdiViewModel().apply {
            updateFechaNacimiento("10/12/2023")
            updateFechaEvaluacion("20/02/2025") // 14m 10d -> Grupo 8
            confirmEdiTermWithoutWeeks()
            updateLongitudInput("77.5")
        }
        prepareCleanGroup(vm, 8)
        completeAllGreenEdi(vm, 8)

        val state = vm.uiState.value
        val footer = EdiPdfExporter.buildCredFooterSummary(state)
        assertTrue(footer.contains("Talla: 77.5 cm (${state.evalTE.shortClassification})"))
        assertFalse(footer.contains("Peso:"))
        assertFalse(footer.contains("5.40"))
        assertFalse(footer.contains("PC:"))
        assertFalse(footer.contains("39.5"))
        assertFalse(footer.contains(state.evalPE.shortClassification))

        exportAndVerifyPdf(vm, "after_04_solo_talla.pdf")
    }

    @Test
    fun `05 peso y talla sin PC omite PC y mantiene ambos diagnosticos y semaforo EDI`() {
        val vm = CredEdiViewModel().apply {
            updateFechaNacimiento("10/12/2023")
            updateFechaEvaluacion("20/02/2025") // Grupo 8
            confirmEdiTermWithoutWeeks()
            updatePesoInput("10.10")
            updateLongitudInput("78.0")
        }
        prepareCleanGroup(vm, 8)
        completeAllGreenEdi(vm, 8)
        // Forzar un amarillo en MG para confirmar preservación del semáforo
        vm.recordEdiCurrentItemResponse("G8_MG_8_1", EdiItemResponse.NO)
        vm.recordEdiCurrentItemResponse("G8_MG_8_2", EdiItemResponse.NO)

        val state = vm.uiState.value
        assertEquals(EdiGlobalTrafficLight.AMARILLO, state.ediGlobalEvaluation.trafficLight)

        val footer = EdiPdfExporter.buildCredFooterSummary(state)
        assertTrue(footer.contains("Peso: 10.10 kg (${state.evalPE.shortClassification})"))
        assertTrue(footer.contains("Talla: 78.0 cm (${state.evalTE.shortClassification})"))
        assertFalse(footer.contains("PC:"))
        assertFalse(footer.contains("39.5"))

        exportAndVerifyPdf(vm, "after_05_peso_y_talla_sin_pc.pdf")
    }

    @Test
    fun `06 PC y peso sin talla omite talla y su diagnostico TE`() {
        val vm = CredEdiViewModel().apply {
            updateFechaNacimiento("10/12/2023")
            updateFechaEvaluacion("20/02/2025") // Grupo 8
            confirmEdiTermWithoutWeeks()
            updatePerimetroCefalicoInput("46.2")
            updatePesoInput("9.90")
        }
        prepareCleanGroup(vm, 8)
        completeAllGreenEdi(vm, 8)

        val state = vm.uiState.value
        val footer = EdiPdfExporter.buildCredFooterSummary(state)
        assertTrue(footer.contains("Peso: 9.90 kg (${state.evalPE.shortClassification})"))
        assertTrue(footer.contains("PC: 46.2 cm"))
        assertFalse(footer.contains("Talla:"))
        assertFalse(footer.contains("61.5"))
        assertFalse(footer.contains(state.evalTE.shortClassification))

        exportAndVerifyPdf(vm, "after_06_pc_y_peso_sin_talla.pdf")
    }

    @Test
    fun `07 tres mediciones registradas imprime las tres e incluye ingreso explicito igual a 5_40`() {
        val vm = CredEdiViewModel().apply {
            updateFechaNacimiento("05/01/2025")
            updateFechaEvaluacion("20/02/2025") // 1m 15d -> Grupo 1
            confirmEdiTermWithoutWeeks()
            updatePesoInput("5.40")
            updateLongitudInput("56.5")
            updatePerimetroCefalicoInput("38.2")
        }
        prepareCleanGroup(vm, 1)
        completeAllGreenEdi(vm, 1)

        val state = vm.uiState.value
        assertEquals(EdiGlobalTrafficLight.VERDE, state.ediGlobalEvaluation.trafficLight)

        val footer = EdiPdfExporter.buildCredFooterSummary(state)
        assertTrue(footer.contains("Peso: 5.40 kg (${state.evalPE.shortClassification})"))
        assertTrue(footer.contains("Talla: 56.5 cm (${state.evalTE.shortClassification})"))
        assertTrue(footer.contains("PC: 38.2 cm"))

        exportAndVerifyPdf(vm, "after_07_todas_las_mediciones.pdf")
    }

    @Test
    fun `08 registro seguido de eliminacion remueve mediciones y diagnosticos del PDF`() {
        val vm = CredEdiViewModel().apply {
            updateFechaNacimiento("10/12/2023")
            updateFechaEvaluacion("20/02/2025") // Grupo 8
            confirmEdiTermWithoutWeeks()
            updatePesoInput("10.50")
            updateLongitudInput("79.0")
            updatePerimetroCefalicoInput("46.8")
        }
        prepareCleanGroup(vm, 8)
        completeAllGreenEdi(vm, 8)

        val withAll = EdiPdfExporter.buildCredFooterSummary(vm.uiState.value)
        assertTrue(withAll.contains("Peso: 10.50 kg"))
        assertTrue(withAll.contains("Talla: 79.0 cm"))
        assertTrue(withAll.contains("PC: 46.8 cm"))

        // Eliminar peso y talla dejando únicamente PC
        vm.updatePesoInput("")
        vm.updateLongitudInput("")
        val afterPartialDelete = vm.uiState.value
        val footerOnlyPc = EdiPdfExporter.buildCredFooterSummary(afterPartialDelete)
        assertFalse("No debe persistir peso eliminado 10.50", footerOnlyPc.contains("10.50"))
        assertFalse("No debe persistir etiqueta Peso:", footerOnlyPc.contains("Peso:"))
        assertFalse("No debe persistir talla eliminada 79.0", footerOnlyPc.contains("79.0"))
        assertFalse("No debe persistir etiqueta Talla:", footerOnlyPc.contains("Talla:"))
        assertTrue("Debe conservar PC vigente", footerOnlyPc.contains("PC: 46.8 cm"))
        exportAndVerifyPdf(vm, "after_08a_tras_eliminar_peso_talla.pdf")

        // Eliminar también PC -> vuelve al estado sin mediciones
        vm.updatePerimetroCefalicoInput("")
        val afterFullDelete = vm.uiState.value
        val footerEmpty = EdiPdfExporter.buildCredFooterSummary(afterFullDelete)
        assertFalse(footerEmpty.contains("Peso:"))
        assertFalse(footerEmpty.contains("Talla:"))
        assertFalse(footerEmpty.contains("PC:"))
        assertFalse(footerEmpty.contains("46.8"))
        assertTrue(footerEmpty.contains("Ver Anexo de Trazabilidad en Página 2"))
        exportAndVerifyPdf(vm, "after_08b_tras_eliminar_todo.pdf")
    }

    @Test
    fun `09 valores vacios invalidos o sin fecha de nacimiento no generan diagnosticos falsos y CRED queda intacto`() {
        // Caso A: inputs inválidos / cero / negativos no se imprimen
        val invalidState = CredEdiUiState(
            fechaNacimiento = "10/12/2023",
            fechaEvaluacion = "20/02/2025",
            pesoInput = "0.0",
            longitudInput = "-12",
            perimetroCefalicoInput = "abc"
        )
        val invalidFooter = EdiPdfExporter.buildCredFooterSummary(invalidState)
        assertFalse(invalidFooter.contains("Peso:"))
        assertFalse(invalidFooter.contains("Talla:"))
        assertFalse(invalidFooter.contains("PC:"))
        assertFalse(invalidFooter.contains("5.40"))
        assertFalse(invalidFooter.contains("61.5"))
        assertFalse(invalidFooter.contains("39.5"))

        // Caso B: peso y talla reales pero SIN fecha de nacimiento -> imprime mediciones, NO diagnóstico OMS de 0m 0d
        val vmNoBirth = CredEdiViewModel().apply {
            updateFechaEvaluacion("20/02/2025")
            updatePesoInput("12.40")
            updateLongitudInput("86.0")
        }
        prepareCleanGroup(vmNoBirth, 10)
        completeAllGreenEdi(vmNoBirth, 10)
        val noBirthState = vmNoBirth.uiState.value
        val noBirthFooter = EdiPdfExporter.buildCredFooterSummary(noBirthState)
        assertTrue(noBirthFooter.contains("Peso: 12.40 kg"))
        assertTrue(noBirthFooter.contains("Talla: 86.0 cm"))
        assertFalse("Sin fecha de nacimiento no debe imprimir clasificación OMS PE", noBirthFooter.contains("("))
        assertFalse(noBirthFooter.contains("PC:"))
        exportAndVerifyPdf(vmNoBirth, "after_09_sin_fecha_nacimiento.pdf")

        // Caso C: verificar que ClinicalAgeCalculator y cálculos CRED no sufrieron alteración alguna
        val credAges = ClinicalAgeCalculator.calculateAges("12/03/2025", "10/11/2026", true, 34, 0)
        assertEquals("1a 7m 29d", credAges.edadCronologicaAMD)
        assertEquals("1a 6m 18d", credAges.edadCorregidaAMD)
        assertEquals("23/04/2025", credAges.fechaAjustada)
    }
}
