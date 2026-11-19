package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.example.model.AppTab
import com.example.model.ClassificationSeverity
import com.example.model.CurvaIndicador
import com.example.model.EdiAreaColorResult
import com.example.model.EdiAreaFallbackEngine
import com.example.model.EdiAxis
import com.example.model.EdiClinicalModality
import com.example.model.EdiDevArea
import com.example.model.EdiGlobalSemaphoreEngine
import com.example.model.EdiGlobalTrafficLight
import com.example.model.EdiGroupSessionSnapshot
import com.example.model.EdiItemResponse
import com.example.model.EdiOfficialCatalog
import com.example.model.EdiOfficialPlate
import com.example.model.EdiPhase1SessionManager
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
 * MEDIO-04 — LOTE 4:
 * Corrección controlada de materiales e interfaz EDI:
 * - #5 MG 4.2 (retirar "Juguete atractivo" de protocolRequiredMaterials)
 * - #6 ALE 5.5 (retirar "Juguete atractivo" de protocolRequiredMaterials)
 * - #7 Badge de materiales solo en encabezado general en EdiScreen.kt
 * - #8 MG 8.2 (conservar "Tapete o silla pequeña", retirar "y juguete llamativo")
 * - #9 SO 8.3 (retirar "Pelota o juguete llamativo" de protocolRequiredMaterials, conservar headerMats)
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class EdiMedio04Lote4MaterialsAndBadgeTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun context(): Context = ApplicationProvider.getApplicationContext()

    private fun completeAllGreenForGroup(vm: CredEdiViewModel, groupNumber: Int) {
        val spec = EdiOfficialCatalog.getGroup(groupNumber)
        spec.areaItems.values.flatten().forEach {
            vm.recordEdiCurrentItemResponse(it.uniqueId, EdiItemResponse.YES)
        }
        EdiOfficialCatalog.getApplicableEnItems(groupNumber).forEach {
            vm.recordEdiCurrentItemResponse(it.uniqueId, EdiItemResponse.NO)
        }
        spec.alaItems.forEach {
            vm.recordEdiCurrentItemResponse(it.uniqueId, EdiItemResponse.NO)
        }
        if (groupNumber in 1..4) {
            EdiOfficialCatalog.frbItems.forEach {
                vm.recordEdiCurrentItemResponse(it.uniqueId, EdiItemResponse.NO)
            }
            spec.aleItems.forEach {
                vm.recordEdiCurrentItemResponse(it.uniqueId, EdiItemResponse.NO)
            }
        }
    }

    private fun configureConsistentGroupContext(vm: CredEdiViewModel, groupNumber: Int) {
        vm.updateFechaEvaluacion("15/06/2026")
        val birthDate = when (groupNumber) {
            4 -> "15/02/2026" // 4m 0d -> G4
            5 -> "15/12/2025" // 6m 0d -> G5
            6 -> "15/10/2025" // 8m 0d -> G6
            8 -> "15/04/2025" // 14m 0d -> G8
            9 -> "15/12/2024" // 18m 0d -> G9
            13 -> "15/06/2022" // 48m 0d -> G13
            else -> error("Unsupported test helper group: $groupNumber")
        }
        vm.updateFechaNacimiento(birthDate)
        if (groupNumber <= 10) {
            vm.confirmEdiTermWithoutWeeks()
        }
        vm.selectEdiGroupFromBank(groupNumber)
    }

    private fun assertHeaderMaterialBadgeDisplayed(uniqueId: String) {
        val tag = "edi_header_material_only_badge_$uniqueId"
        composeRule.onNodeWithTag(tag)
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNode(
            hasAnyAncestor(hasTestTag(tag)) and
                hasText("Material mencionado en el encabezado general; no indispensable para este reactivo")
        ).assertIsDisplayed()
    }

    @Test
    fun `caso A MG 4_2 sin juguete obligatorio permite estimulo verbal y oculta falta material`() {
        val mg42 = EdiOfficialCatalog.findItemByUniqueId("G4_MG_4_2")!!
        assertEquals("MG 4.2", mg42.itemCode)
        assertTrue(mg42.protocolRequiredMaterials.isEmpty())
        assertTrue(mg42.protocolRequiredPlates.isEmpty())
        assertFalse(mg42.requiresMaterialInProtocol)
        assertFalse(mg42.isListedWithHeaderMaterialOnly())
        assertEquals(EdiClinicalModality.DIRECT_OBSERVATION, mg42.modality)
        assertTrue(mg42.yesIsNormalGreen)
        assertTrue(mg42.clinicalInstruction.contains("hablándole, cantándole o mostrándole algún juguete atractivo"))

        val vm = CredEdiViewModel()
        vm.selectTab(AppTab.EDI)
        configureConsistentGroupContext(vm, 4)
        vm.openEdiAxisQuestionnaire(EdiAxis.AREAS)

        composeRule.setContent { CredEdiApp(viewModel = vm) }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("edi_item_card_G4_MG_4_2").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("edi_resp_missing_mat_G4_MG_4_2").assertDoesNotExist()
        composeRule.onNodeWithTag("edi_header_material_only_badge_G4_MG_4_2").assertDoesNotExist()

        // Evaluación completa en G4 con MG 4.2 mediante estímulo verbal (YES/NO), sin bloqueo falso
        completeAllGreenForGroup(vm, 4)
        val globalEval = vm.uiState.value.ediGlobalEvaluation
        assertEquals(EdiGlobalTrafficLight.VERDE, globalEval.trafficLight)
        assertTrue(globalEval.isDefinitive)
        assertTrue(globalEval.missingMaterialItemCodes.isEmpty())
    }

    @Test
    fun `caso B ALE 5_5 sin juguete obligatorio conserva modalidad polaridad y caracter opcional en G5`() {
        val ale55 = EdiOfficialCatalog.findItemByUniqueId("G5_ALE_5_5")!!
        assertEquals("ALE 5.5", ale55.itemCode)
        assertTrue(ale55.protocolRequiredMaterials.isEmpty())
        assertTrue(ale55.protocolRequiredPlates.isEmpty())
        assertFalse(ale55.requiresMaterialInProtocol)
        assertEquals(EdiClinicalModality.DIRECT_OBSERVATION, ale55.modality)
        assertTrue(ale55.hasObservationAsterisks)
        assertFalse(ale55.yesIsNormalGreen)
        assertTrue(ale55.clinicalInstruction.contains("hablándole, cantándole o mostrándole algún juguete atractivo"))
        assertTrue(ale55.clinicalInstruction.contains("Repetición del ítem: hasta 2 veces más de cada lado."))

        val vm = CredEdiViewModel()
        vm.selectTab(AppTab.EDI)
        configureConsistentGroupContext(vm, 5)
        vm.openEdiAxisQuestionnaire(EdiAxis.ALE)

        composeRule.setContent { CredEdiApp(viewModel = vm) }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("edi_item_card_G5_ALE_5_5").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("edi_resp_missing_mat_G5_ALE_5_5").assertDoesNotExist()

        // Aunque no se responda ALE 5.5 (ni FRB), G5 emite VERDE definitivo si áreas/EN/ALA son normales
        completeAllGreenForGroup(vm, 5)
        val eval = vm.uiState.value.ediGlobalEvaluation
        assertFalse(eval.aleMandatoryForGroup)
        assertEquals(EdiGlobalTrafficLight.VERDE, eval.trafficLight)
        assertTrue(eval.isDefinitive)
    }

    @Test
    fun `caso C MG 8_2 conserva alternativa silla o tapete sin exigir juguete ni alterar MG 8_3`() {
        val mg82 = EdiOfficialCatalog.findItemByUniqueId("G8_MG_8_2")!!
        val mg83 = EdiOfficialCatalog.findItemByUniqueId("G8_MG_8_3")!!

        assertEquals(listOf("Tapete o silla pequeña"), mg82.protocolRequiredMaterials)
        assertTrue(mg82.requiresMaterialInProtocol)
        assertFalse(mg82.protocolRequiredMaterials.any { it.contains("juguete", ignoreCase = true) })
        assertTrue(mg82.clinicalInstruction.contains("Siente al niño en una silla pequeña o en un tapete"))
        assertTrue(mg82.yesIsNormalGreen)

        // MG 8.3 sigue intacto con juguete
        assertEquals(listOf("Sonaja o juguete llamativo"), mg83.protocolRequiredMaterials)

        val vm = CredEdiViewModel()
        vm.selectTab(AppTab.EDI)
        vm.selectEdiGroupFromBank(8)
        vm.openEdiAxisQuestionnaire(EdiAxis.AREAS)

        composeRule.setContent { CredEdiApp(viewModel = vm) }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("edi_item_card_G8_MG_8_2").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("edi_header_material_only_badge_G8_MG_8_2").assertDoesNotExist()
        composeRule.onNodeWithTag("edi_resp_missing_mat_G8_MG_8_2")
            .performScrollTo()
            .performClick()
        composeRule.waitForIdle()

        val eval = vm.uiState.value.ediGlobalEvaluation
        assertEquals(EdiGlobalTrafficLight.INCOMPLETE, eval.trafficLight)
        assertTrue(eval.missingMaterialItemCodes.contains("MG 8.2"))
    }

    @Test
    fun `caso D SO 8_3 sin pelota ni juguete obligatorio conserva orden ven y no bloquea por material`() {
        val so83 = EdiOfficialCatalog.findItemByUniqueId("G8_SO_8_3")!!
        assertEquals("SO 8.3", so83.itemCode)
        assertTrue(so83.protocolRequiredMaterials.isEmpty())
        assertFalse(so83.requiresMaterialInProtocol)
        assertEquals(listOf("Pelota o juguete llamativo"), so83.generalHeaderMaterials)
        assertTrue(so83.isListedWithHeaderMaterialOnly())
        assertTrue(so83.yesIsNormalGreen)
        assertEquals(EdiClinicalModality.DIRECT_OBSERVATION, so83.modality)
        assertTrue(so83.clinicalInstruction.contains("\"ven\""))
        assertTrue(so83.clinicalInstruction.contains("\"trae la pelota\""))

        val vm = CredEdiViewModel()
        vm.selectTab(AppTab.EDI)
        configureConsistentGroupContext(vm, 8)
        vm.openEdiAxisQuestionnaire(EdiAxis.AREAS)

        composeRule.setContent { CredEdiApp(viewModel = vm) }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("edi_item_card_G8_SO_8_3").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("edi_resp_missing_mat_G8_SO_8_3").assertDoesNotExist()

        completeAllGreenForGroup(vm, 8)
        val eval = vm.uiState.value.ediGlobalEvaluation
        assertEquals(EdiGlobalTrafficLight.VERDE, eval.trafficLight)
        assertTrue(eval.isDefinitive)
    }

    @Test
    fun `caso E badge de ALE 6_4 en interfaz refleja encabezado sin afirmar modalidad incorrecta`() {
        val ale64 = EdiOfficialCatalog.findItemByUniqueId("G6_ALE_6_4")!!
        assertEquals(EdiClinicalModality.DIRECTED_INTERVIEW, ale64.modality)
        assertTrue(ale64.isListedWithHeaderMaterialOnly())

        val vm = CredEdiViewModel()
        vm.selectTab(AppTab.EDI)
        vm.selectEdiGroupFromBank(6)
        vm.openEdiAxisQuestionnaire(EdiAxis.ALE)

        composeRule.setContent { CredEdiApp(viewModel = vm) }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("edi_item_card_G6_ALE_6_4").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("edi_resp_missing_mat_G6_ALE_6_4").assertDoesNotExist()
        assertHeaderMaterialBadgeDisplayed("G6_ALE_6_4")
        assertEquals(
            0,
            composeRule.onAllNodesWithText("Protocolo clínico sin lámina física (observación del cuidador)")
                .fetchSemanticsNodes().size
        )
    }

    @Test
    fun `caso F badge de ALE 9_1 no afirma observacion del cuidador cuando observa al nino`() {
        val ale91 = EdiOfficialCatalog.findItemByUniqueId("G9_ALE_9_1")!!
        assertEquals(EdiClinicalModality.DIRECT_OBSERVATION, ale91.modality)
        assertTrue(ale91.isListedWithHeaderMaterialOnly())

        val vm = CredEdiViewModel()
        vm.selectTab(AppTab.EDI)
        vm.selectEdiGroupFromBank(9)
        vm.openEdiAxisQuestionnaire(EdiAxis.ALE)

        composeRule.setContent { CredEdiApp(viewModel = vm) }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("edi_item_card_G9_ALE_9_1").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("edi_resp_missing_mat_G9_ALE_9_1").assertDoesNotExist()
        assertHeaderMaterialBadgeDisplayed("G9_ALE_9_1")
        assertEquals(
            0,
            composeRule.onAllNodesWithText("Protocolo clínico sin lámina física (observación del cuidador)")
                .fetchSemanticsNodes().size
        )
    }

    @Test
    fun `caso G badge de SO 8_3 en interfaz tras retirar requisito de juguete`() {
        val vm = CredEdiViewModel()
        vm.selectTab(AppTab.EDI)
        vm.selectEdiGroupFromBank(8)
        vm.openEdiAxisQuestionnaire(EdiAxis.AREAS)

        composeRule.setContent { CredEdiApp(viewModel = vm) }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("edi_item_card_G8_SO_8_3").performScrollTo().assertIsDisplayed()
        assertHeaderMaterialBadgeDisplayed("G8_SO_8_3")
        composeRule.onNodeWithTag("edi_resp_missing_mat_G8_SO_8_3").assertDoesNotExist()
        assertEquals(
            0,
            composeRule.onAllNodesWithText("Protocolo clínico sin lámina física (observación del cuidador)")
                .fetchSemanticsNodes().size
        )
    }

    @Test
    fun `caso H retroceso G5 hacia G4 hereda MG 4_2 sin juguete obligatorio`() {
        val mgFallbacks = EdiAreaFallbackEngine.getFallbackItemsForArea(5, EdiDevArea.MG)
        val retroMg42 = mgFallbacks.firstOrNull { it.sourceItem.uniqueId == "G4_MG_4_2" }
        assertNotNull(retroMg42)
        assertEquals("G5_RETRO_G4_MG_2", retroMg42!!.fallbackUniqueId)
        assertFalse(retroMg42.sourceItem.requiresMaterialInProtocol)
        assertTrue(retroMg42.sourceItem.protocolRequiredMaterials.isEmpty())

        val vm = CredEdiViewModel()
        vm.selectTab(AppTab.EDI)
        vm.selectEdiGroupFromBank(5)
        vm.openEdiAxisQuestionnaire(EdiAxis.AREAS)
        // Activar retroceso de MG respondiendo NO a MG 5.1 y MG 5.2
        vm.recordEdiCurrentItemResponse("G5_MG_5_1", EdiItemResponse.NO)
        vm.recordEdiCurrentItemResponse("G5_MG_5_2", EdiItemResponse.NO)

        composeRule.setContent { CredEdiApp(viewModel = vm) }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("edi_item_card_G5_RETRO_G4_MG_2").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("edi_resp_missing_mat_G5_RETRO_G4_MG_2").assertDoesNotExist()

        vm.recordEdiFallbackResponse("G5_RETRO_G4_MG_1", EdiItemResponse.YES)
        vm.recordEdiFallbackResponse("G5_RETRO_G4_MG_2", EdiItemResponse.YES)
        val mgResult = vm.uiState.value.ediGlobalEvaluation.axis3AreasSummary.areaResults[EdiDevArea.MG]!!
        assertEquals(EdiAreaColorResult.AMARILLO, mgResult.status)
    }

    @Test
    fun `caso I retroceso G9 hacia G8 hereda MG 8_2 con silla o tapete y SO 8_3 sin pelota`() {
        val g9MgFallback = EdiAreaFallbackEngine.getFallbackItemsForArea(9, EdiDevArea.MG)
        val retroMg82 = g9MgFallback.first { it.sourceItem.uniqueId == "G8_MG_8_2" }
        assertEquals("G9_RETRO_G8_MG_2", retroMg82.fallbackUniqueId)
        assertEquals(listOf("Tapete o silla pequeña"), retroMg82.sourceItem.protocolRequiredMaterials)
        assertTrue(retroMg82.sourceItem.requiresMaterialInProtocol)

        val g9SoFallback = EdiAreaFallbackEngine.getFallbackItemsForArea(9, EdiDevArea.SO)
        val retroSo83 = g9SoFallback.first { it.sourceItem.uniqueId == "G8_SO_8_3" }
        assertEquals("G9_RETRO_G8_SO_3", retroSo83.fallbackUniqueId)
        assertTrue(retroSo83.sourceItem.protocolRequiredMaterials.isEmpty())
        assertFalse(retroSo83.sourceItem.requiresMaterialInProtocol)
        assertTrue(retroSo83.sourceItem.isListedWithHeaderMaterialOnly())

        val vm = CredEdiViewModel()
        vm.selectTab(AppTab.EDI)
        vm.selectEdiGroupFromBank(9)
        vm.openEdiAxisQuestionnaire(EdiAxis.AREAS)
        // Activar retroceso de MG y SO en G9
        listOf("G9_MG_9_1", "G9_MG_9_2", "G9_MG_9_3", "G9_SO_9_1", "G9_SO_9_2", "G9_SO_9_3").forEach {
            vm.recordEdiCurrentItemResponse(it, EdiItemResponse.NO)
        }

        composeRule.setContent { CredEdiApp(viewModel = vm) }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("edi_item_card_G9_RETRO_G8_MG_2").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("edi_resp_missing_mat_G9_RETRO_G8_MG_2").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("edi_item_card_G9_RETRO_G8_SO_3").performScrollTo().assertIsDisplayed()
        assertHeaderMaterialBadgeDisplayed("G9_RETRO_G8_SO_3")
        composeRule.onNodeWithTag("edi_resp_missing_mat_G9_RETRO_G8_SO_3").assertDoesNotExist()
    }

    @Test
    fun `caso J preservacion de materiales y laminas realmente obligatorios`() {
        val mf41 = EdiOfficialCatalog.findItemByCode("MF 4.1")!!
        val mf42 = EdiOfficialCatalog.findItemByCode("MF 4.2")!!
        val mg83 = EdiOfficialCatalog.findItemByCode("MG 8.3")!!
        val ale53 = EdiOfficialCatalog.findItemByCode("ALE 5.3")!!
        val le93 = EdiOfficialCatalog.findItemByCode("LE 9.3")!!
        val ale93 = EdiOfficialCatalog.findItemByCode("ALE 9.3")!!
        val ale123 = EdiOfficialCatalog.findItemByCode("ALE 12.3")!!
        val ale141 = EdiOfficialCatalog.findItemByCode("ALE 14.1")!!
        val co141 = EdiOfficialCatalog.findItemByCode("CO 14.1")!!
        val co142 = EdiOfficialCatalog.findItemByCode("CO 14.2")!!
        val co151 = EdiOfficialCatalog.findItemByCode("CO 15.1")!!
        val co153 = EdiOfficialCatalog.findItemByCode("CO 15.3")!!

        assertEquals(listOf("Pelota roja sostenida por una cuerda"), mf41.protocolRequiredMaterials)
        assertEquals(listOf("Sonaja, cubo o juguete llamativo"), mf42.protocolRequiredMaterials)
        assertEquals(listOf("Sonaja o juguete llamativo"), mg83.protocolRequiredMaterials)
        assertEquals(listOf("Sonaja, cubo u otro objeto atractivo"), ale53.protocolRequiredMaterials)
        assertEquals(setOf(EdiOfficialPlate.LAMINA_A), le93.protocolRequiredPlates)
        assertFalse(ale93.requiresMaterialInProtocol)
        assertEquals(EdiClinicalModality.DUAL_OBSERVATION_AND_INTERVIEW_ALE_12_3, ale123.modality)
        assertEquals(
            setOf(EdiOfficialPlate.LAMINA_B, EdiOfficialPlate.LAMINA_C, EdiOfficialPlate.LAMINA_D),
            ale141.protocolRequiredPlates
        )
        assertTrue(ale141.requiresMaterialInProtocol)
        assertEquals(listOf("Discos de 6 cm de diámetro de diez colores diferentes"), co141.protocolRequiredMaterials)
        assertEquals(listOf("Diez cubos de 3 x 3 cm del mismo color"), co142.protocolRequiredMaterials)
        assertEquals(listOf("Papel y lápiz"), co151.protocolRequiredMaterials)
        assertEquals(listOf("Monedas o billetes de diferente denominación"), co153.protocolRequiredMaterials)
    }

    @Test
    fun `caso K catalogo mantiene 270 identificadores unicos y 15 grupos etarios`() {
        assertEquals(15, EdiOfficialCatalog.groups.size)
        val allIds = EdiOfficialCatalog.groups.flatMap { g ->
            g.areaItems.values.flatten().map { it.uniqueId } +
                g.aleItems.map { it.uniqueId } +
                g.alaItems.map { it.uniqueId }
        } + EdiOfficialCatalog.frbItems.map { it.uniqueId } +
            EdiOfficialCatalog.allEnItems.map { it.uniqueId } +
            EdiOfficialCatalog.specialGroup13CoRFallbackItems.map { it.uniqueId }
        assertEquals(270, allIds.size)
        assertEquals(270, allIds.toSet().size)
    }

    @Test
    fun `caso L semaforos y alertas clinicas prioritarias permanecen intactos`() {
        val vm = CredEdiViewModel()
        vm.selectEdiGroupFromBank(8)
        vm.recordEdiCurrentItemResponse("G8_ALA_8_1", EdiItemResponse.YES)
        val eval = vm.uiState.value.ediGlobalEvaluation
        assertEquals(EdiGlobalTrafficLight.INCOMPLETE, eval.trafficLight)
        assertTrue(eval.requiresImmediateClinicalAttentionDespiteIncomplete)
        assertTrue(eval.priorityCriticalAlerts.any { it.code == "ALA 8.1" })
    }

    @Test
    fun `caso M regresion de lotes 1, 2, 2B, 3 y exportacion PDF`() {
        // Lote 2 & 2B
        val peOut = WhoGrowthStandards.evaluateIndicator(CurvaIndicador.PE, SexoBiologico.NINA, 60, 20.0, 112.0, 15.9, edadDias = 1)
        assertFalse(peOut.isReferenceAvailable)
        assertTrue(peOut.zScore.isNaN())

        val vm = CredEdiViewModel()
        vm.selectSexo(SexoBiologico.NINO)
        vm.updateFechaNacimiento("15/06/2022")
        vm.updateFechaEvaluacion("15/06/2026") // 48m 0d -> G13
        vm.updatePesoInput("17.50")
        vm.updateLongitudInput("110.5") // >110cm => P/T no disponible
        vm.updatePerimetroCefalicoInput("50.2")
        assertEquals(GlobalDiagnosisCompleteness.PARTIAL, vm.uiState.value.globalDiagnosis.completeness)
        assertEquals(ClassificationSeverity.WARNING, vm.uiState.value.globalDiagnosis.severity)

        // Lote 3
        assertTrue(
            EdiOfficialCatalog.findItemByUniqueId("EN_1_2")!!.clinicalInstruction
                .contains("el párpado queda entreabierto mientras se cierra en lado sano.")
        )
        assertTrue(
            EdiOfficialCatalog.findItemByUniqueId("G4_SO_4_2")!!.clinicalInstruction
                .contains("¿Recurre el niño a estrategias para impedir que el adulto se aleje?")
        )
        assertTrue(
            EdiOfficialCatalog.findItemByUniqueId("G11_LE_11_2")!!.clinicalInstruction
                .contains("Asegúrese que no se refiera a sí mismo en tercera persona (hablar de sí mismo como si fuera otra persona).")
        )

        // PDF
        completeAllGreenForGroup(vm, 13)
        val pdfResult = EdiPdfExporter.exportarFichaOficialEdiPdf(context(), vm.uiState.value)
        assertTrue(pdfResult.message, pdfResult.success)
        assertNotNull(pdfResult.filePath)
        assertTrue(File(pdfResult.filePath!!).length() > 10_000L)
    }

    @Test
    fun `caso N senales ALE opcionales en G5 y G6 no bloquean semaforo`() {
        // G5 con ALE 5.3 marcada como falta de material (opcional) y ALE 5.5 sin responder -> VERDE
        val vm5 = CredEdiViewModel()
        configureConsistentGroupContext(vm5, 5)
        completeAllGreenForGroup(vm5, 5)
        vm5.recordEdiCurrentItemResponse("G5_ALE_5_3", EdiItemResponse.NOT_EVALUATED_MISSING_MATERIAL)
        val eval5 = vm5.uiState.value.ediGlobalEvaluation
        assertEquals(EdiGlobalTrafficLight.VERDE, eval5.trafficLight)
        assertTrue(eval5.isDefinitive)
        assertTrue(eval5.optionalMissingMaterialItemCodes.contains("ALE 5.3"))

        // G6 con ALE 6.4 sin responder y áreas/EN/ALA normales -> VERDE
        val vm6 = CredEdiViewModel()
        configureConsistentGroupContext(vm6, 6)
        completeAllGreenForGroup(vm6, 6)
        val eval6 = vm6.uiState.value.ediGlobalEvaluation
        assertEquals(EdiGlobalTrafficLight.VERDE, eval6.trafficLight)
        assertTrue(eval6.isDefinitive)
    }

    @Test
    fun `caso O respuestas Falta material en items sin material obligatorio no generan bloqueos ni se convierten en SI o NO`() {
        // 1) A través del gestor de sesiones y motor de retroceso, intentar registrar NOT_EVALUATED_MISSING_MATERIAL
        //    para MG 4.2, ALE 5.5, SO 8.3 y retrocesos G5->G4 MG 4.2 / G9->G8 SO 8.3 se normaliza a UNANSWERED (jamás YES/NO).
        var s4 = EdiPhase1SessionManager.createInitialState(4)
        s4 = EdiPhase1SessionManager.recordCurrentGroupItemResponse(s4, "G4_MG_4_2", EdiItemResponse.NOT_EVALUATED_MISSING_MATERIAL)
        assertEquals(EdiItemResponse.UNANSWERED, s4.activeSession.currentGroupResponses["G4_MG_4_2"])
        val eval4 = EdiGlobalSemaphoreEngine.evaluateGlobalSemaphore(s4)
        assertFalse(eval4.missingMaterialItemCodes.contains("MG 4.2"))
        assertTrue(eval4.pendingMandatoryItemCodes.contains("MG 4.2"))

        var s5 = EdiPhase1SessionManager.createInitialState(5)
        s5 = EdiPhase1SessionManager.recordCurrentGroupItemResponse(s5, "G5_ALE_5_5", EdiItemResponse.NOT_EVALUATED_MISSING_MATERIAL)
        assertEquals(EdiItemResponse.UNANSWERED, s5.activeSession.currentGroupResponses["G5_ALE_5_5"])
        s5 = EdiAreaFallbackEngine.recordValidatedFallbackResponse(s5, "G5_RETRO_G4_MG_2", EdiItemResponse.NOT_EVALUATED_MISSING_MATERIAL)
        assertEquals(EdiItemResponse.UNANSWERED, s5.activeSession.fallbackGroupResponses["G5_RETRO_G4_MG_2"])

        var s8 = EdiPhase1SessionManager.createInitialState(8)
        s8 = EdiPhase1SessionManager.recordCurrentGroupItemResponse(s8, "G8_SO_8_3", EdiItemResponse.NOT_EVALUATED_MISSING_MATERIAL)
        assertEquals(EdiItemResponse.UNANSWERED, s8.activeSession.currentGroupResponses["G8_SO_8_3"])
        val eval8 = EdiGlobalSemaphoreEngine.evaluateGlobalSemaphore(s8)
        assertFalse(eval8.missingMaterialItemCodes.contains("SO 8.3"))
        assertTrue(eval8.pendingMandatoryItemCodes.contains("SO 8.3"))

        var s9 = EdiPhase1SessionManager.createInitialState(9)
        s9 = EdiAreaFallbackEngine.recordValidatedFallbackResponse(s9, "G9_RETRO_G8_SO_3", EdiItemResponse.NOT_EVALUATED_MISSING_MATERIAL)
        assertEquals(EdiItemResponse.UNANSWERED, s9.activeSession.fallbackGroupResponses["G9_RETRO_G8_SO_3"])

        // 2) Inspección de snapshot pre-Lote 4 con NOT_EVALUATED_MISSING_MATERIAL ya almacenado:
        //    Nunca se convierte silenciosamente en YES/NO ni permite emitir VERDE.
        val g4Spec = EdiOfficialCatalog.getGroup(4)
        val preLote4SnapshotG4 = EdiGroupSessionSnapshot(
            groupNumber = 4,
            currentGroupResponses = (g4Spec.areaItems.values.flatten().associate { it.uniqueId to EdiItemResponse.YES } +
                g4Spec.aleItems.associate { it.uniqueId to EdiItemResponse.NO } +
                g4Spec.alaItems.associate { it.uniqueId to EdiItemResponse.NO } +
                mapOf("G4_MG_4_2" to EdiItemResponse.NOT_EVALUATED_MISSING_MATERIAL)),
            fallbackGroupResponses = emptyMap(),
            frbResponses = EdiOfficialCatalog.frbItems.associate { it.uniqueId to EdiItemResponse.NO },
            enResponses = EdiOfficialCatalog.getApplicableEnItems(4).associate { it.uniqueId to EdiItemResponse.NO }
        )
        val evalLegacyG4 = EdiGlobalSemaphoreEngine.evaluateGlobalSemaphore(4, preLote4SnapshotG4)
        assertEquals(EdiGlobalTrafficLight.INCOMPLETE, evalLegacyG4.trafficLight)
        assertFalse(evalLegacyG4.isDefinitive)
        val mgResultLegacy = evalLegacyG4.axis3AreasSummary.areaResults[EdiDevArea.MG]!!
        assertEquals(EdiAreaColorResult.INCOMPLETE, mgResultLegacy.status)
        assertEquals(EdiItemResponse.NOT_EVALUATED_MISSING_MATERIAL, mgResultLegacy.currentItemResponses["G4_MG_4_2"])
    }
}
