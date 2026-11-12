package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.AppTab
import com.example.model.EdiAreaColorResult
import com.example.model.EdiDevArea
import com.example.model.EdiGlobalTrafficLight
import com.example.model.EdiGroupChangeResolutionAction
import com.example.model.EdiItemResponse
import com.example.model.EdiOfficialCatalog
import com.example.util.EdiPdfExporter
import com.example.viewmodel.CredEdiViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class EdiPhase4BPdfExporterTest {

    private fun context(): Context = ApplicationProvider.getApplicationContext()

    private fun copyPdfForVisualValidation(srcPath: String, scenarioFileName: String): File {
        val outDir = File("build/edi_pdf_validation")
        if (!outDir.exists()) outDir.mkdirs()
        val dest = File(outDir, scenarioFileName)
        File(srcPath).copyTo(dest, overwrite = true)
        return dest
    }

    private fun prepareCleanGroup(vm: CredEdiViewModel, groupNumber: Int) {
        vm.selectTab(AppTab.EDI)
        vm.selectEdiGroupFromBank(groupNumber)
        if (vm.uiState.value.ediEngineState.pendingGroupChangeConfirmation != null) {
            vm.resolveEdiPendingGroupChange(
                EdiGroupChangeResolutionAction.DISCARD_PREVIOUS_CONFIRMED_AND_SWITCH_TO_CLEAN_TARGET
            )
        }
    }

    private fun completeAllMandatoryAreasAsGreen(vm: CredEdiViewModel, groupNumber: Int) {
        val spec = EdiOfficialCatalog.getGroup(groupNumber)
        spec.areaItems.values.flatten().forEach { item ->
            if (item.uniqueId == "G13_LE_13_3" || item.uniqueId == "G14_LE_14_3") {
                vm.recordEdiDualEvidence(item.uniqueId, true, true, null, "Evaluación consistente")
            } else {
                vm.recordEdiCurrentItemResponse(item.uniqueId, EdiItemResponse.YES)
            }
        }
    }

    private fun completeAllEnAsNormal(vm: CredEdiViewModel, groupNumber: Int) {
        EdiOfficialCatalog.getApplicableEnItems(groupNumber).forEach {
            vm.recordEdiCurrentItemResponse(it.uniqueId, EdiItemResponse.NO)
        }
    }

    private fun completeAllAlaAsNegative(vm: CredEdiViewModel, groupNumber: Int) {
        EdiOfficialCatalog.getGroup(groupNumber).alaItems.forEach {
            vm.recordEdiCurrentItemResponse(it.uniqueId, EdiItemResponse.NO)
        }
    }

    private fun completeAllFrbAsNegative(vm: CredEdiViewModel) {
        EdiOfficialCatalog.frbItems.forEach {
            vm.recordEdiCurrentItemResponse(it.uniqueId, EdiItemResponse.NO)
        }
    }

    private fun completeAllAleAsNegative(vm: CredEdiViewModel, groupNumber: Int) {
        EdiOfficialCatalog.getGroup(groupNumber).aleItems.forEach { item ->
            if (item.uniqueId == "G12_ALE_12_3") {
                vm.recordEdiDualEvidence(item.uniqueId, false, false, null, "Sin alerta")
            } else {
                vm.recordEdiCurrentItemResponse(item.uniqueId, EdiItemResponse.NO)
            }
        }
    }

    @Test
    fun `01 scenario Grupo 1 sin ALA y sin CO genera ficha oficial definitiva VERDE`() {
        val vm = CredEdiViewModel()
        vm.updateFechaNacimiento("05/01/2025")
        vm.updateFechaEvaluacion("20/02/2025") // 1m 15d -> Grupo 1
        vm.updatePerimetroCefalicoInput("38.0")
        vm.updatePesoInput("4.80")
        vm.updateLongitudInput("56.0")

        prepareCleanGroup(vm, 1)
        completeAllMandatoryAreasAsGreen(vm, 1)
        completeAllEnAsNormal(vm, 1)
        completeAllFrbAsNegative(vm)
        completeAllAleAsNegative(vm, 1)
        // En Grupo 1: 1 FRB positivo sin otros criterios mantiene VERDE
        vm.recordEdiCurrentItemResponse("FRB_2", EdiItemResponse.YES)

        val state = vm.uiState.value
        assertEquals(1, state.ediEffectiveGroupNumber)
        assertEquals(EdiGlobalTrafficLight.VERDE, state.ediGlobalEvaluation.trafficLight)
        assertFalse(state.ediGlobalEvaluation.alaAppliesToGroup)
        assertEquals(EdiAreaColorResult.NOT_APPLICABLE, state.ediAxis3AreasSummary.areaResults[EdiDevArea.CO]?.status)

        val result = EdiPdfExporter.exportarFichaOficialEdiPdf(context(), state)
        assertTrue(result.message, result.success)
        assertFalse(result.isDraft)
        assertNotNull(result.filePath)
        assertTrue(result.fileName!!.startsWith("Ficha_Oficial_EDI_G1_"))

        val saved = File(result.filePath!!)
        assertTrue(saved.exists() && saved.length() > 10_000L)
        copyPdfForVisualValidation(result.filePath!!, "scenario_01_grupo_1.pdf")
    }

    @Test
    fun `02 scenario Grupo 3 con retroceso de 2 reactivos a Grupo 2 marca Grupo anterior y AMARILLO`() {
        val vm = CredEdiViewModel()
        vm.updateFechaNacimiento("10/11/2024")
        vm.updateFechaEvaluacion("25/02/2025") // 3m 15d -> Grupo 3
        vm.updatePerimetroCefalicoInput("40.5")

        prepareCleanGroup(vm, 3)
        completeAllMandatoryAreasAsGreen(vm, 3)
        // MG 0/2 SÍ -> activa retroceso a 2 reactivos de G2
        vm.recordEdiCurrentItemResponse("G3_MG_3_1", EdiItemResponse.NO)
        vm.recordEdiCurrentItemResponse("G3_MG_3_2", EdiItemResponse.NO)
        vm.recordEdiFallbackResponse("G3_RETRO_G2_MG_1", EdiItemResponse.YES)
        vm.recordEdiFallbackResponse("G3_RETRO_G2_MG_2", EdiItemResponse.YES)

        completeAllEnAsNormal(vm, 3)
        completeAllAlaAsNegative(vm, 3)
        completeAllFrbAsNegative(vm)
        completeAllAleAsNegative(vm, 3)

        val state = vm.uiState.value
        assertEquals(3, state.ediEffectiveGroupNumber)
        assertEquals(EdiAreaColorResult.AMARILLO, state.ediAxis3AreasSummary.areaResults[EdiDevArea.MG]?.status)
        assertTrue(state.ediAxis3AreasSummary.areaResults[EdiDevArea.MG]?.fallbackRequired == true)
        assertEquals(EdiGlobalTrafficLight.AMARILLO, state.ediGlobalEvaluation.trafficLight)

        val result = EdiPdfExporter.exportarFichaOficialEdiPdf(context(), state)
        assertTrue(result.success)
        assertFalse(result.isDraft)
        assertTrue(result.fileName!!.startsWith("Ficha_Oficial_EDI_G3_"))
        copyPdfForVisualValidation(result.filePath!!, "scenario_02_grupo_3_retroceso.pdf")
    }

    @Test
    fun `03 scenario Grupo 8 con retroceso excepcional a 2 reactivos del Grupo 7`() {
        val vm = CredEdiViewModel()
        vm.updateFechaNacimiento("10/12/2023")
        vm.updateFechaEvaluacion("20/02/2025") // 14m 10d -> Grupo 8
        vm.updatePerimetroCefalicoInput("46.2")

        prepareCleanGroup(vm, 8)
        completeAllMandatoryAreasAsGreen(vm, 8)
        // MF 0/3 SÍ -> retroceso excepcional a 2 reactivos de G7
        vm.recordEdiCurrentItemResponse("G8_MF_8_1", EdiItemResponse.NO)
        vm.recordEdiCurrentItemResponse("G8_MF_8_2", EdiItemResponse.NO)
        vm.recordEdiCurrentItemResponse("G8_MF_8_3", EdiItemResponse.NO)
        vm.recordEdiFallbackResponse("G8_RETRO_G7_MF_1", EdiItemResponse.YES)
        vm.recordEdiFallbackResponse("G8_RETRO_G7_MF_2", EdiItemResponse.YES)

        completeAllEnAsNormal(vm, 8)
        completeAllAlaAsNegative(vm, 8)

        val state = vm.uiState.value
        val mfEval = state.ediAxis3AreasSummary.areaResults[EdiDevArea.MF]!!
        assertEquals(8, state.ediEffectiveGroupNumber)
        assertEquals(2, mfEval.fallbackItems.size)
        assertEquals(7, mfEval.fallbackSourceGroupNumber)
        assertEquals(EdiAreaColorResult.AMARILLO, mfEval.status)
        assertEquals(EdiGlobalTrafficLight.AMARILLO, state.ediGlobalEvaluation.trafficLight)

        val result = EdiPdfExporter.exportarFichaOficialEdiPdf(context(), state)
        assertTrue(result.success)
        assertFalse(result.isDraft)
        assertTrue(result.fileName!!.startsWith("Ficha_Oficial_EDI_G8_"))
        copyPdfForVisualValidation(result.filePath!!, "scenario_03_grupo_8_retro_g7.pdf")
    }

    @Test
    fun `04 scenario Grupo 13 con retroceso especial CO-R y sin dibujar retroceso historico ignorado`() {
        val vm = CredEdiViewModel()
        vm.updateFechaNacimiento("01/09/2021")
        vm.updateFechaEvaluacion("15/02/2025") // 41m 14d -> Grupo 13
        vm.updatePerimetroCefalicoInput("49.8")

        prepareCleanGroup(vm, 13)
        completeAllMandatoryAreasAsGreen(vm, 13)

        // Simular retroceso histórico previo en MG que ya no aplica tras corrección a SÍ
        vm.recordEdiCurrentItemResponse("G13_MG_13_1", EdiItemResponse.NO)
        vm.recordEdiCurrentItemResponse("G13_MG_13_2", EdiItemResponse.NO)
        vm.recordEdiCurrentItemResponse("G13_MG_13_3", EdiItemResponse.NO)
        vm.recordEdiFallbackResponse("G13_RETRO_G12_MG_1", EdiItemResponse.NO)
        vm.recordEdiCurrentItemResponse("G13_MG_13_1", EdiItemResponse.YES)

        // Activar retroceso especial CO-R 13.1..13.3 con 2/3 SÍ = AMARILLO
        vm.recordEdiCurrentItemResponse("G13_CO_13_1", EdiItemResponse.NO)
        vm.recordEdiCurrentItemResponse("G13_CO_13_2", EdiItemResponse.NO)
        vm.recordEdiCurrentItemResponse("G13_CO_13_3", EdiItemResponse.NO)
        vm.recordEdiFallbackResponse("G13_RETRO_COR_13_1", EdiItemResponse.YES)
        vm.recordEdiFallbackResponse("G13_RETRO_COR_13_2", EdiItemResponse.YES)
        vm.recordEdiFallbackResponse("G13_RETRO_COR_13_3", EdiItemResponse.NO)

        completeAllEnAsNormal(vm, 13)
        completeAllAlaAsNegative(vm, 13)

        val state = vm.uiState.value
        val coEval = state.ediAxis3AreasSummary.areaResults[EdiDevArea.CO]!!
        assertTrue(coEval.fallbackRequired)
        assertTrue(coEval.usesSpecialCoRFallback)
        assertEquals(EdiAreaColorResult.AMARILLO, coEval.status)
        assertFalse(state.ediAxis3AreasSummary.areaResults[EdiDevArea.MG]!!.fallbackRequired)
        assertEquals(EdiGlobalTrafficLight.AMARILLO, state.ediGlobalEvaluation.trafficLight)

        val result = EdiPdfExporter.exportarFichaOficialEdiPdf(context(), state)
        assertTrue(result.success)
        assertFalse(result.isDraft)
        assertTrue(result.fileName!!.startsWith("Ficha_Oficial_EDI_G13_"))
        copyPdfForVisualValidation(result.filePath!!, "scenario_04_grupo_13_cor.pdf")
    }

    @Test
    fun `05 scenario Grupo 14 con los 8 reactivos ALE completos y protocolo dual LE 14_3`() {
        val vm = CredEdiViewModel()
        vm.updateFechaNacimiento("01/06/2020")
        vm.updateFechaEvaluacion("15/02/2025") // 56m 14d -> Grupo 14
        vm.updatePerimetroCefalicoInput("50.6")

        prepareCleanGroup(vm, 14)
        completeAllMandatoryAreasAsGreen(vm, 14)
        // LE 14.3 con conflicto resuelto manualmente como SÍ y nota clínica
        vm.recordEdiDualEvidence(
            itemUniqueId = "G14_LE_14_3",
            observationEvidence = false,
            interviewEvidence = true,
            humanResolutionIfConflict = true,
            note = "Madre refiere lenguaje fluido en casa"
        )
        completeAllEnAsNormal(vm, 14)
        completeAllAlaAsNegative(vm, 14)
        completeAllFrbAsNegative(vm)

        // Registrar las 8 ALE del Grupo 14 (mix SÍ/NO)
        val aleItems = EdiOfficialCatalog.getGroup(14).aleItems
        assertEquals(8, aleItems.size)
        aleItems.forEachIndexed { idx, item ->
            val resp = if (idx in setOf(0, 2, 5, 7)) EdiItemResponse.YES else EdiItemResponse.NO
            vm.recordEdiCurrentItemResponse(item.uniqueId, resp)
        }
        vm.updateEdiItemClinicalNote("G14_ALE_14_1", "Revisado con Láminas B, C y D")

        val state = vm.uiState.value
        assertEquals(14, state.ediEffectiveGroupNumber)
        // En G14 (>G4), FRB/ALE positivos no cambian el semáforo global (VERDE)
        assertEquals(EdiGlobalTrafficLight.VERDE, state.ediGlobalEvaluation.trafficLight)

        val result = EdiPdfExporter.exportarFichaOficialEdiPdf(context(), state)
        assertTrue(result.success)
        assertFalse(result.isDraft)
        assertTrue(result.fileName!!.startsWith("Ficha_Oficial_EDI_G14_"))
        copyPdfForVisualValidation(result.filePath!!, "scenario_05_grupo_14_8ale.pdf")
    }

    @Test
    fun `06 scenario Grupo 15 con EN 1_3 no aplicable genera ficha oficial VERDE`() {
        val vm = CredEdiViewModel()
        vm.updateFechaNacimiento("01/10/2019")
        vm.updateFechaEvaluacion("15/02/2025") // 64m 14d -> Grupo 15
        vm.updatePerimetroCefalicoInput("51.2")

        prepareCleanGroup(vm, 15)
        completeAllMandatoryAreasAsGreen(vm, 15)
        completeAllEnAsNormal(vm, 15)
        completeAllAlaAsNegative(vm, 15)

        val state = vm.uiState.value
        assertEquals(15, state.ediEffectiveGroupNumber)
        assertFalse(state.ediGlobalEvaluation.en13AppliesToGroup)
        assertEquals(EdiGlobalTrafficLight.VERDE, state.ediGlobalEvaluation.trafficLight)

        val result = EdiPdfExporter.exportarFichaOficialEdiPdf(context(), state)
        assertTrue(result.success)
        assertFalse(result.isDraft)
        assertTrue(result.fileName!!.startsWith("Ficha_Oficial_EDI_G15_"))
        copyPdfForVisualValidation(result.filePath!!, "scenario_06_grupo_15_no_en13.pdf")
    }

    @Test
    fun `07 scenario evaluacion incompleta con advertencia clinica prioritaria exporta BORRADOR sin semaforo`() {
        val vm = CredEdiViewModel()
        vm.updateFechaNacimiento("15/02/2025")
        vm.updateFechaEvaluacion("09/05/2025")
        vm.togglePrematurez(true)
        vm.updateEgSemanas(36)
        vm.updateEgDias(5) // Discrepancia etaria activa

        vm.selectTab(AppTab.EDI)
        vm.selectEdiGroupFromBank(4)
        // Registrar hallazgos críticos mientras faltan reactivos obligatorios y material
        vm.recordEdiCurrentItemResponse("G4_ALA_4_1", EdiItemResponse.YES)
        vm.recordEdiCurrentItemResponse("EN_1_1", EdiItemResponse.YES)
        vm.recordEdiCurrentItemResponse("G4_MF_4_2", EdiItemResponse.NOT_EVALUATED_MISSING_MATERIAL)

        val state = vm.uiState.value
        assertEquals(EdiGlobalTrafficLight.INCOMPLETE, state.ediGlobalEvaluation.trafficLight)
        assertFalse(state.ediGlobalEvaluation.isDefinitive)
        assertTrue(state.ediGlobalEvaluation.hasPriorityCriticalWarning)

        val result = EdiPdfExporter.exportarFichaOficialEdiPdf(context(), state)
        assertTrue(result.success)
        assertTrue("Debe marcarse estrictamente como borrador", result.isDraft)
        assertTrue(result.fileName!!.startsWith("Borrador_EDI_Incompleta_"))
        assertTrue(result.message.contains("Borrador", ignoreCase = true))
        copyPdfForVisualValidation(result.filePath!!, "scenario_07_borrador_alerta_prioritaria.pdf")
    }

    @Test
    fun `08 scenario Grupo 10 completo con FRB y ALE opcionales sin responder genera ficha oficial definitiva`() {
        val vm = CredEdiViewModel()
        vm.updateFechaNacimiento("15/05/2023")
        vm.updateFechaEvaluacion("20/02/2025") // 21m 5d -> Grupo 10
        vm.updatePerimetroCefalicoInput("47.8")

        prepareCleanGroup(vm, 10)
        // Responder SÓLO ejes obligatorios (Áreas, EN, ALA); dejar FRB y ALE sin responder (y uno falta material)
        completeAllMandatoryAreasAsGreen(vm, 10)
        completeAllEnAsNormal(vm, 10)
        completeAllAlaAsNegative(vm, 10)
        vm.recordEdiCurrentItemResponse("G10_ALE_10_1", EdiItemResponse.NOT_EVALUATED_MISSING_MATERIAL)

        val state = vm.uiState.value
        assertEquals(10, state.ediEffectiveGroupNumber)
        assertEquals(EdiGlobalTrafficLight.VERDE, state.ediGlobalEvaluation.trafficLight)
        assertTrue(state.ediGlobalEvaluation.isDefinitive)
        assertTrue(state.ediGlobalEvaluation.nonBlockingClinicalWarnings.isNotEmpty())

        val result = EdiPdfExporter.exportarFichaOficialEdiPdf(context(), state)
        assertTrue(result.success)
        assertFalse("FRB/ALE opcionales pendientes en G10 no deben degradar a borrador", result.isDraft)
        assertTrue(result.fileName!!.startsWith("Ficha_Oficial_EDI_G10_"))
        copyPdfForVisualValidation(result.filePath!!, "scenario_08_grupo_10_opcionales_pendientes.pdf")
    }

    @Test
    fun `09 exportador no sobrescribe archivos previos e ignora estado heredado en conflicto`() {
        val vm = CredEdiViewModel()
        vm.updateFechaNacimiento("05/01/2025")
        vm.updateFechaEvaluacion("20/02/2025")
        prepareCleanGroup(vm, 1)
        completeAllMandatoryAreasAsGreen(vm, 1)
        completeAllEnAsNormal(vm, 1)
        completeAllFrbAsNegative(vm)
        completeAllAleAsNegative(vm, 1)

        // Corromper intencionalmente campos heredados antiguos (deben ser ignorados totalmente)
        vm.setSaItemPresent(0, true)
        vm.toggleEnTono() // fuerza semaforoCalculado heredado != VERDE
        val state = vm.uiState.value
        assertEquals(EdiGlobalTrafficLight.VERDE, state.ediGlobalEvaluation.trafficLight)

        val first = EdiPdfExporter.exportarFichaOficialEdiPdf(context(), state)
        val second = EdiPdfExporter.exportarFichaOficialEdiPdf(context(), state)
        assertTrue(first.success && second.success)
        assertNotEquals("No debe sobrescribir exportaciones sucesivas", first.filePath, second.filePath)
        assertTrue(File(first.filePath!!).exists())
        assertTrue(File(second.filePath!!).exists())
        assertEquals(EdiGlobalTrafficLight.VERDE.displayLabel, first.trafficLightLabel)
    }
}
