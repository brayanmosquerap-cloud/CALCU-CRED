package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.example.model.AppTab
import com.example.model.EdiAxis
import com.example.model.EdiBirthMaturityCondition
import com.example.model.EdiDevArea
import com.example.model.EdiGlobalTrafficLight
import com.example.model.EdiGroupChangeResolutionAction
import com.example.model.EdiInternalView
import com.example.model.EdiItemResponse
import com.example.model.EdiOfficialCatalog
import com.example.util.EdiPdfExporter
import com.example.viewmodel.CredEdiViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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
class EdiUnverifiableBirthDateSafetyTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun context(): Context = ApplicationProvider.getApplicationContext()

    private fun fillAllMandatoryForGroupAsGreen(
        vm: CredEdiViewModel,
        groupNumber: Int,
        skipOptionalFrbAndAleInG5To15: Boolean = true
    ) {
        val spec = EdiOfficialCatalog.getGroup(groupNumber)
        if (!skipOptionalFrbAndAleInG5To15 || groupNumber in 1..4) {
            EdiOfficialCatalog.frbItems.forEach { item ->
                vm.recordEdiCurrentItemResponse(item.uniqueId, EdiItemResponse.NO)
            }
            spec.aleItems.forEach { item ->
                if (item.uniqueId == "G12_ALE_12_3") {
                    vm.recordEdiDualEvidence(item.uniqueId, false, false, null, "Sin alerta")
                } else {
                    vm.recordEdiCurrentItemResponse(item.uniqueId, EdiItemResponse.NO)
                }
            }
        }
        spec.areaItems.values.flatten().forEach { item ->
            if (item.uniqueId == "G13_LE_13_3" || item.uniqueId == "G14_LE_14_3") {
                vm.recordEdiDualEvidence(item.uniqueId, true, true, null, "Logrado")
            } else {
                vm.recordEdiCurrentItemResponse(item.uniqueId, EdiItemResponse.YES)
            }
        }
        EdiOfficialCatalog.getApplicableEnItems(groupNumber).forEach { item ->
            vm.recordEdiCurrentItemResponse(item.uniqueId, EdiItemResponse.NO)
        }
        spec.alaItems.forEach { item ->
            vm.recordEdiCurrentItemResponse(item.uniqueId, EdiItemResponse.NO)
        }
    }

    @Test
    fun `01 manual consultation of G1 without birth date stays INCOMPLETE and non-definitive`() {
        val vm = CredEdiViewModel()
        assertEquals("", vm.uiState.value.fechaNacimiento)
        assertFalse(vm.uiState.value.isChronologicalAgeVerifiable)
        assertTrue(vm.uiState.value.isEdiManualConsultationMode)

        fillAllMandatoryForGroupAsGreen(vm, 1)

        val state = vm.uiState.value
        assertEquals(1, state.ediEffectiveGroupNumber)
        assertEquals(EdiGlobalTrafficLight.INCOMPLETE, state.ediGlobalEvaluation.trafficLight)
        assertFalse(state.ediGlobalEvaluation.isDefinitive)
        assertTrue(state.ediGlobalEvaluation.blockedByOutOfRangeAge)
        assertTrue(state.ediUnverifiableAgeReason.contains("Edad no verificable: registrar fechas válidas"))
        assertTrue(
            state.ediGlobalEvaluation.blockingReasons.any {
                it.contains("INCOMPLETO / EDAD NO VERIFICABLE") &&
                    it.contains("Edad no verificable: registrar fechas válidas")
            }
        )
    }

    @Test
    fun `02 manual consultation of G8 without birth date allows area fallback rehearsal while staying INCOMPLETE`() {
        val vm = CredEdiViewModel()
        vm.selectEdiGroupFromBank(8)
        assertEquals(8, vm.uiState.value.ediEffectiveGroupNumber)
        assertFalse(vm.uiState.value.isChronologicalAgeVerifiable)

        val spec8 = EdiOfficialCatalog.getGroup(8)
        spec8.areaItems.getValue(EdiDevArea.MG).forEach { item ->
            vm.recordEdiCurrentItemResponse(item.uniqueId, EdiItemResponse.NO)
        }
        vm.recordEdiFallbackResponse("G8_RETRO_G7_MG_1", EdiItemResponse.YES)
        vm.recordEdiFallbackResponse("G8_RETRO_G7_MG_2", EdiItemResponse.YES)

        listOf(EdiDevArea.MF, EdiDevArea.LE, EdiDevArea.SO, EdiDevArea.CO).forEach { area ->
            spec8.areaItems.getValue(area).forEach { item ->
                vm.recordEdiCurrentItemResponse(item.uniqueId, EdiItemResponse.YES)
            }
        }
        EdiOfficialCatalog.getApplicableEnItems(8).forEach { item ->
            vm.recordEdiCurrentItemResponse(item.uniqueId, EdiItemResponse.NO)
        }
        spec8.alaItems.forEach { item ->
            vm.recordEdiCurrentItemResponse(item.uniqueId, EdiItemResponse.NO)
        }

        val state = vm.uiState.value
        assertTrue(state.ediGlobalEvaluation.yellowAreas.contains(EdiDevArea.MG))
        assertEquals(EdiGlobalTrafficLight.INCOMPLETE, state.ediGlobalEvaluation.trafficLight)
        assertFalse(state.ediGlobalEvaluation.isDefinitive)
    }

    @Test
    fun `03 manual consultation of G10 without birth date stays INCOMPLETE and non-definitive`() {
        val vm = CredEdiViewModel()
        vm.selectEdiGroupFromBank(10)
        fillAllMandatoryForGroupAsGreen(vm, 10)

        val state = vm.uiState.value
        assertEquals(10, state.ediEffectiveGroupNumber)
        assertFalse(state.isChronologicalAgeVerifiable)
        assertEquals(EdiGlobalTrafficLight.INCOMPLETE, state.ediGlobalEvaluation.trafficLight)
        assertFalse(state.ediGlobalEvaluation.isDefinitive)
        assertTrue(state.ediUnverifiableAgeReason.contains("Edad no verificable: registrar fechas válidas"))
    }

    @Test
    fun `04 manual consultation of G11 and G15 without birth date also blocks definitive semaphore`() {
        val vm = CredEdiViewModel()

        // Test G11 (25 to 30m catalog group) without birth date
        vm.selectEdiGroupFromBank(11)
        fillAllMandatoryForGroupAsGreen(vm, 11)
        var state = vm.uiState.value
        assertEquals(11, state.ediEffectiveGroupNumber)
        assertFalse(state.isChronologicalAgeVerifiable)
        assertEquals(EdiGlobalTrafficLight.INCOMPLETE, state.ediGlobalEvaluation.trafficLight)
        assertFalse(state.ediGlobalEvaluation.isDefinitive)

        // Switch to G15 (49 to 59m catalog group) without birth date
        vm.selectEdiGroupFromBank(15)
        vm.resolveEdiPendingGroupChange(
            EdiGroupChangeResolutionAction.ARCHIVE_PREVIOUS_AND_SWITCH_TO_CLEAN_TARGET
        )
        fillAllMandatoryForGroupAsGreen(vm, 15)
        state = vm.uiState.value
        assertEquals(15, state.ediEffectiveGroupNumber)
        assertFalse(state.isChronologicalAgeVerifiable)
        assertEquals(EdiGlobalTrafficLight.INCOMPLETE, state.ediGlobalEvaluation.trafficLight)
        assertFalse(state.ediGlobalEvaluation.isDefinitive)
        assertTrue(
            state.ediGlobalEvaluation.blockingReasons.any {
                it.contains("Edad no verificable: registrar fechas válidas")
            }
        )
    }

    @Test
    fun `05 invalid birth date blocks definitive evaluation and explains unverifiable age`() {
        val vm = CredEdiViewModel()
        vm.selectEdiGroupFromBank(12)
        fillAllMandatoryForGroupAsGreen(vm, 12)

        vm.updateFechaEvaluacion("20/05/2025")
        vm.updateFechaNacimiento("31/02/2023") // Impossible calendar date

        val state = vm.uiState.value
        assertFalse(state.isChronologicalAgeVerifiable)
        assertTrue(state.isEdiManualConsultationMode)
        assertTrue(state.ediUnverifiableAgeReason.contains("Edad no verificable: registrar fechas válidas"))
        assertEquals(EdiGlobalTrafficLight.INCOMPLETE, state.ediGlobalEvaluation.trafficLight)
        assertFalse(state.ediGlobalEvaluation.isDefinitive)
    }

    @Test
    fun `06 empty or invalid evaluation date blocks definitive evaluation`() {
        val vm = CredEdiViewModel()
        vm.updateFechaNacimiento("15/01/2022") // Would be >= 24m if eval date were valid
        vm.updateFechaEvaluacion("") // Empty eval date

        fillAllMandatoryForGroupAsGreen(vm, vm.uiState.value.ediEffectiveGroupNumber)
        var state = vm.uiState.value
        assertFalse(state.isChronologicalAgeVerifiable)
        assertTrue(state.ediUnverifiableAgeReason.contains("Edad no verificable: registrar fechas válidas"))
        assertEquals(EdiGlobalTrafficLight.INCOMPLETE, state.ediGlobalEvaluation.trafficLight)
        assertFalse(state.ediGlobalEvaluation.isDefinitive)

        // Invalid eval date
        vm.updateFechaEvaluacion("99/99/2025")
        state = vm.uiState.value
        assertFalse(state.isChronologicalAgeVerifiable)
        assertTrue(state.ediUnverifiableAgeReason.contains("Edad no verificable: registrar fechas válidas"))
        assertEquals(EdiGlobalTrafficLight.INCOMPLETE, state.ediGlobalEvaluation.trafficLight)
        assertFalse(state.ediGlobalEvaluation.isDefinitive)
    }

    @Test
    fun `07 evaluation date earlier than birth date blocks definitive evaluation`() {
        val vm = CredEdiViewModel()
        vm.updateFechaEvaluacion("10/05/2025")
        vm.updateFechaNacimiento("20/05/2025") // Inverted dates

        fillAllMandatoryForGroupAsGreen(vm, vm.uiState.value.ediEffectiveGroupNumber)
        val state = vm.uiState.value
        assertFalse(state.isChronologicalAgeVerifiable)
        assertTrue(state.isEdiManualConsultationMode)
        assertTrue(state.ediGlobalEvaluation.blockedByOutOfRangeAge)
        assertTrue(state.ediUnverifiableAgeReason.contains("Edad no verificable: registrar fechas válidas"))
        assertTrue(state.ediUnverifiableAgeReason.contains("anterior a la fecha de nacimiento"))
        assertEquals(EdiGlobalTrafficLight.INCOMPLETE, state.ediGlobalEvaluation.trafficLight)
        assertFalse(state.ediGlobalEvaluation.isDefinitive)
    }

    @Test
    fun `08 valid birth and evaluation dates make chronological age verifiable`() {
        val vm = CredEdiViewModel()
        vm.updateFechaEvaluacion("20/05/2025")
        vm.updateFechaNacimiento("10/01/2022") // 40 months -> G13

        val state = vm.uiState.value
        assertTrue(state.isChronologicalAgeVerifiable)
        assertFalse(state.isEdiManualConsultationMode)
        assertEquals("", state.ediUnverifiableAgeReason)
        assertEquals(13, state.ediEffectiveGroupNumber)
    }

    @Test
    fun `09 registering valid dates after manual consultation with recorded answers requests group change confirmation and archives without mixing`() {
        val vm = CredEdiViewModel()
        // Professional manually selects G10 and records responses without birth date
        vm.selectEdiGroupFromBank(10)
        vm.recordEdiCurrentItemResponse("G10_MG_10_1", EdiItemResponse.YES)
        vm.updateEdiItemClinicalNote("G10_MG_10_1", "Ensayo en consulta manual G10")
        assertTrue(vm.uiState.value.ediEngineState.activeSession.hasRecordedClinicalResponses)

        // Now professional enters valid dates corresponding to G13 (e.g. 38 months old)
        vm.updateFechaEvaluacion("20/05/2025")
        vm.updateFechaNacimiento("10/03/2022")

        val stateAfterDates = vm.uiState.value
        assertTrue(stateAfterDates.isChronologicalAgeVerifiable)
        // Must NOT silently overwrite or mix G10 answers into G13: confirmation is requested
        val pending = stateAfterDates.ediEngineState.pendingGroupChangeConfirmation
        assertNotNull(pending)
        assertEquals(10, pending?.previousGroupNumber)
        assertEquals(13, pending?.targetGroupNumber)
        assertEquals(10, stateAfterDates.ediEffectiveGroupNumber)

        // Confirm archiving G10 and switching cleanly to G13
        vm.resolveEdiPendingGroupChange(
            EdiGroupChangeResolutionAction.ARCHIVE_PREVIOUS_AND_SWITCH_TO_CLEAN_TARGET
        )

        val stateAfterConfirm = vm.uiState.value
        assertNull(stateAfterConfirm.ediEngineState.pendingGroupChangeConfirmation)
        assertEquals(13, stateAfterConfirm.ediEffectiveGroupNumber)
        assertFalse(stateAfterConfirm.ediEngineState.activeSession.hasRecordedClinicalResponses)
        assertTrue(stateAfterConfirm.ediEngineState.archivedSessionsByGroup.containsKey(10))
        assertEquals(
            EdiItemResponse.YES,
            stateAfterConfirm.ediEngineState.archivedSessionsByGroup[10]?.currentGroupResponses?.get("G10_MG_10_1")
        )
    }

    @Test
    fun `10 priority clinical alerts ALA EN and ROJO area remain visible during unverifiable age block`() {
        val vm = CredEdiViewModel()
        vm.selectEdiGroupFromBank(6)
        assertFalse(vm.uiState.value.isChronologicalAgeVerifiable)

        val spec6 = EdiOfficialCatalog.getGroup(6)
        // Record ALA positive, EN positive, and MG ROJO (0 YES in G6 and G5 fallback)
        vm.recordEdiCurrentItemResponse(spec6.alaItems.first().uniqueId, EdiItemResponse.YES)
        vm.recordEdiCurrentItemResponse(EdiOfficialCatalog.getApplicableEnItems(6).first().uniqueId, EdiItemResponse.YES)
        spec6.areaItems.getValue(EdiDevArea.MG).forEach {
            vm.recordEdiCurrentItemResponse(it.uniqueId, EdiItemResponse.NO)
        }
        vm.recordEdiFallbackResponse("G6_RETRO_G5_MG_1", EdiItemResponse.NO)
        vm.recordEdiFallbackResponse("G6_RETRO_G5_MG_2", EdiItemResponse.NO)
        vm.updateEdiItemClinicalNote("G6_ALA_6_1", "Alerta prioritaria detectada en consulta")

        val state = vm.uiState.value
        val eval = state.ediGlobalEvaluation
        assertEquals(EdiGlobalTrafficLight.INCOMPLETE, eval.trafficLight)
        assertFalse(eval.isDefinitive)
        assertTrue(eval.hasPriorityCriticalWarning)
        assertTrue(eval.priorityCriticalAlerts.any { it.axis == EdiAxis.ALA })
        assertTrue(eval.priorityCriticalAlerts.any { it.axis == EdiAxis.EN })
        assertTrue(eval.priorityCriticalAlerts.any { it.axis == EdiAxis.AREAS })
        assertEquals(
            "Alerta prioritaria detectada en consulta",
            state.ediEngineState.activeSession.itemClinicalNotes["G6_ALA_6_1"]
        )
    }

    @Test
    fun `11 draft PDF export without verifiable age marks BORRADOR includes unverifiable age text and preserves only real measurements`() {
        val vm = CredEdiViewModel()
        vm.selectEdiGroupFromBank(11)
        fillAllMandatoryForGroupAsGreen(vm, 11)
        // Enter only weight (no length, no head circumference)
        vm.updatePesoInput("12.40")

        val state = vm.uiState.value
        assertFalse(state.isChronologicalAgeVerifiable)
        assertEquals(EdiGlobalTrafficLight.INCOMPLETE, state.ediGlobalEvaluation.trafficLight)
        assertFalse(state.ediGlobalEvaluation.isDefinitive)

        val ageSummary = EdiPdfExporter.buildPage2AgeVerificationSummary(state)
        assertTrue(ageSummary.contains("BORRADOR / EVALUACIÓN INCOMPLETA"))
        assertTrue(ageSummary.contains("Edad no verificable: registrar fechas válidas"))

        val footerSummary = EdiPdfExporter.buildCredFooterSummary(state)
        assertTrue(footerSummary.contains("Peso: 12.40 kg"))
        assertFalse(footerSummary.contains("Talla:"))
        assertFalse(footerSummary.contains("PC:"))
        assertFalse(footerSummary.contains(state.evalPE.shortClassification)) // No age-dependent diagnosis
        assertTrue(footerSummary.contains("Edad no verificable: registrar fechas válidas"))

        val exportResult = EdiPdfExporter.exportarFichaOficialEdiPdf(context(), state)
        assertTrue(exportResult.message, exportResult.success)
        assertTrue(exportResult.isDraft)
        assertTrue(exportResult.fileName!!.startsWith("Borrador_EDI_Incompleta_"))

        val outDir = File("build/unverifiable_age_pdfs").apply { mkdirs() }
        val sourceFile = File(exportResult.filePath!!)
        assertTrue(sourceFile.exists() && sourceFile.length() > 10_000L)
        sourceFile.copyTo(File(outDir, exportResult.fileName!!), overwrite = true)
    }

    @Test
    fun `12 impossible to generate definitive official PDF without valid dates across G1 G8 G10 G11 and G15`() {
        val groupsToVerify = listOf(1, 8, 10, 11, 15)
        for (groupNum in groupsToVerify) {
            val vm = CredEdiViewModel()
            vm.selectEdiGroupFromBank(groupNum)
            fillAllMandatoryForGroupAsGreen(vm, groupNum, skipOptionalFrbAndAleInG5To15 = false)

            val state = vm.uiState.value
            assertFalse("Group G$groupNum must not have verifiable age", state.isChronologicalAgeVerifiable)
            assertEquals(
                "Group G$groupNum must remain INCOMPLETE without valid dates",
                EdiGlobalTrafficLight.INCOMPLETE,
                state.ediGlobalEvaluation.trafficLight
            )
            assertFalse("Group G$groupNum must not be definitive", state.ediGlobalEvaluation.isDefinitive)

            val pdf = EdiPdfExporter.exportarFichaOficialEdiPdf(context(), state)
            assertTrue("Group G$groupNum PDF must be forced to draft", pdf.isDraft)
            assertTrue(pdf.fileName!!.startsWith("Borrador_EDI_Incompleta_"))
        }
    }

    @Test
    fun `13 clean unlock when valid dates and valid gestational condition are registered`() {
        val vm = CredEdiViewModel()
        vm.selectTab(AppTab.EDI)
        vm.selectEdiGroupFromBank(11)
        fillAllMandatoryForGroupAsGreen(vm, 11)

        // Initially unverifiable -> INCOMPLETE
        assertFalse(vm.uiState.value.isChronologicalAgeVerifiable)
        assertEquals(EdiGlobalTrafficLight.INCOMPLETE, vm.uiState.value.ediGlobalEvaluation.trafficLight)

        // UI banner visible in Window 4
        vm.navigateEdiView(EdiInternalView.RESULTS_TRACEABILITY)
        composeRule.setContent { CredEdiApp(viewModel = vm) }
        composeRule.onNodeWithTag("edi_res_unverifiable_age_banner")
            .performScrollTo()
            .assertIsDisplayed()

        // Now register valid dates matching G11 (26 months old)
        vm.updateFechaEvaluacion("20/05/2025")
        vm.updateFechaNacimiento("10/03/2023")
        composeRule.waitForIdle()

        val unlockedState = vm.uiState.value
        assertTrue(unlockedState.isChronologicalAgeVerifiable)
        assertEquals(11, unlockedState.ediEffectiveGroupNumber)
        assertEquals(EdiGlobalTrafficLight.VERDE, unlockedState.ediGlobalEvaluation.trafficLight)
        assertTrue(unlockedState.ediGlobalEvaluation.isDefinitive)

        val officialPdf = EdiPdfExporter.exportarFichaOficialEdiPdf(context(), unlockedState)
        assertTrue(officialPdf.message, officialPdf.success)
        assertFalse(officialPdf.isDraft)
        assertTrue(officialPdf.fileName!!.startsWith("Ficha_Oficial_EDI_G11_"))
    }

    @Test
    fun `14 non-regression of ALTO-01 MEDIO-01 and Option 2 under 24m birth maturity confirmation`() {
        val vm = CredEdiViewModel()
        // 1) Under 24m with valid dates but UNSPECIFIED maturity -> Option 2 blocks until maturity confirmed
        vm.updateFechaEvaluacion("20/05/2025")
        vm.updateFechaNacimiento("10/01/2025") // 4m 10d -> G4
        assertTrue(vm.uiState.value.isChronologicalAgeVerifiable)
        assertEquals(EdiBirthMaturityCondition.UNSPECIFIED, vm.uiState.value.ediEffectiveMaturityCondition)
        assertTrue(vm.uiState.value.isBirthMaturityConfirmationPendingInUnder24m)
        vm.selectEdiGroupFromBank(4)
        fillAllMandatoryForGroupAsGreen(vm, 4)
        assertEquals(EdiGlobalTrafficLight.INCOMPLETE, vm.uiState.value.ediGlobalEvaluation.trafficLight)

        // Confirm term without weeks (Option 2 + MEDIO-01) -> unlocks cleanly without using default 32w
        vm.confirmEdiTermWithoutWeeks()
        assertFalse(vm.uiState.value.isBirthMaturityConfirmationPendingInUnder24m)
        assertFalse(vm.uiState.value.usaEdadCorregida)
        assertEquals(EdiGlobalTrafficLight.VERDE, vm.uiState.value.ediGlobalEvaluation.trafficLight)
        assertTrue(vm.uiState.value.ediGlobalEvaluation.isDefinitive)

        // ALTO-01: Only head circumference entered -> footer prints only PC
        vm.updatePerimetroCefalicoInput("41.2")
        val footer = EdiPdfExporter.buildCredFooterSummary(vm.uiState.value)
        assertTrue(footer.contains("PC: 41.2 cm"))
        assertFalse(footer.contains("Peso:"))
        assertFalse(footer.contains("Talla:"))
    }

    @Test
    fun `15 integrity of Windows 1 2 and 3 remains intact`() {
        val vm = CredEdiViewModel()
        vm.updateFechaEvaluacion("20/05/2025")
        vm.updateFechaNacimiento("15/05/2024") // 12 months
        vm.confirmEdiTermWithoutWeeks()
        vm.updatePesoInput("9.60")
        vm.updateLongitudInput("75.5")
        vm.updatePerimetroCefalicoInput("46.0")

        val state = vm.uiState.value
        assertFalse(state.isAgeDependentEvaluationBlocked)
        assertEquals(12, state.edadCronologicaMeses)
        assertTrue(state.evalPE.shortClassification.isNotBlank())
        assertTrue(state.evalTE.shortClassification.isNotBlank())
        assertTrue(state.evalPT.shortClassification.isNotBlank())
        assertTrue(state.evalPCE.shortClassification.isNotBlank())

        vm.selectTab(AppTab.CALCULADORA)
        assertEquals(AppTab.CALCULADORA, vm.uiState.value.currentTab)
        vm.selectTab(AppTab.CURVAS_OMS)
        assertEquals(AppTab.CURVAS_OMS, vm.uiState.value.currentTab)
        vm.selectTab(AppTab.DIAGNOSTICO)
        assertEquals(AppTab.DIAGNOSTICO, vm.uiState.value.currentTab)
    }
}
