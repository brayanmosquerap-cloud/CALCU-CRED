package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.example.model.AppTab
import com.example.model.EdiAxis
import com.example.model.EdiDevArea
import com.example.model.EdiGlobalTrafficLight
import com.example.model.EdiGroupAutoSelectionStatus
import com.example.model.EdiGroupChangeResolutionAction
import com.example.model.EdiGroupFilterCategory
import com.example.model.EdiInternalView
import com.example.model.EdiItemResponse
import com.example.model.EdiOfficialCatalog
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class EdiPhase4AIntegrationTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun fillAllMandatoryForGroupAsGreen(
        vm: CredEdiViewModel,
        groupNumber: Int,
        skipOptionalFrbAndAleInG5To15: Boolean = false
    ) {
        val spec = EdiOfficialCatalog.getGroup(groupNumber)
        if (!skipOptionalFrbAndAleInG5To15 || groupNumber in 1..4) {
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
        spec.areaItems.values.flatten().forEach { item ->
            if (item.uniqueId == "G13_LE_13_3" || item.uniqueId == "G14_LE_14_3") {
                vm.recordEdiDualEvidence(item.uniqueId, true, true, null, "Logrado")
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
    }

    @Test
    fun `01 navigation across all 4 internal edi views in Compose UI preserving 4 tabs`() {
        val vm = CredEdiViewModel()
        composeRule.setContent {
            CredEdiApp(viewModel = vm)
        }

        // Navigate to Tab 4 (EDI)
        composeRule.onNodeWithTag("tab_edi").performClick()
        composeRule.onNodeWithTag("edi_view_summary").assertIsDisplayed()

        // Summary -> Bank of 15 Groups
        composeRule.onNodeWithTag("edi_btn_change_group").performClick()
        composeRule.onNodeWithTag("edi_view_group_bank").assertIsDisplayed()

        // Bank -> Back to Summary
        composeRule.onNodeWithTag("edi_bank_back").performClick()
        composeRule.onNodeWithTag("edi_view_summary").assertIsDisplayed()

        // Summary -> Questionnaire & Fallback
        composeRule.onNodeWithTag("edi_btn_continue_evaluation").performScrollTo().performClick()
        composeRule.onNodeWithTag("edi_view_questionnaire").assertIsDisplayed()

        // Questionnaire -> Results & Traceability
        composeRule.onNodeWithTag("edi_q_to_results").performClick()
        composeRule.onNodeWithTag("edi_view_results").assertIsDisplayed()

        // Results -> Back to Summary
        composeRule.onNodeWithTag("edi_res_back").performClick()
        composeRule.onNodeWithTag("edi_view_summary").assertIsDisplayed()
    }

    @Test
    fun `02 selection of all 15 official groups and bank search filters`() {
        val vm = CredEdiViewModel()

        for (g in 1..15) {
            vm.selectEdiGroupFromBank(g)
            assertEquals(g, vm.uiState.value.ediEffectiveGroupNumber)
            assertEquals(g, vm.uiState.value.ediActiveGroupSpec.groupNumber)
        }

        // Verify filter and search state updates cleanly
        vm.selectEdiGroupFilter(EdiGroupFilterCategory.PREESCOLARES)
        assertEquals(EdiGroupFilterCategory.PREESCOLARES, vm.uiState.value.ediGroupFilterCategory)
        vm.updateEdiGroupSearchQuery("Grupo 13")
        assertEquals("Grupo 13", vm.uiState.value.ediGroupSearchQuery)
    }

    @Test
    fun `03 individual responses missing material gating dual modality conflict and separate notes`() {
        val vm = CredEdiViewModel()
        vm.selectTab(AppTab.EDI)
        // Group 9 to verify ALE 9.1 does NOT have missing material button while LE 9.3 (Lamina A) DOES
        vm.selectEdiGroupFromBank(9)
        vm.openEdiAxisQuestionnaire(EdiAxis.ALE)

        composeRule.setContent {
            CredEdiApp(viewModel = vm)
        }

        // ALE 9.1 should not expose missing material button (header-only mention, no physical plate in protocol)
        composeRule.onNodeWithTag("edi_item_card_G9_ALE_9_1").assertIsDisplayed()
        composeRule.onNodeWithTag("edi_resp_missing_mat_G9_ALE_9_1").assertDoesNotExist()
        // Even if called programmatically, engine rejects NOT_EVALUATED_MISSING_MATERIAL for ALE 9.1
        vm.recordEdiCurrentItemResponse("G9_ALE_9_1", EdiItemResponse.NOT_EVALUATED_MISSING_MATERIAL)
        assertEquals(
            EdiItemResponse.UNANSWERED,
            vm.uiState.value.ediGlobalEvaluation.aleResponsesResolved["G9_ALE_9_1"]
        )

        // LE 9.3 uses Lamina A in protocol -> has missing material button
        vm.selectEdiAxis(EdiAxis.AREAS)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("edi_resp_missing_mat_G9_LE_9_3")
            .performScrollTo()
            .performClick()
        assertEquals(
            EdiItemResponse.NOT_EVALUATED_MISSING_MATERIAL,
            vm.uiState.value.ediEngineState.activeSession.currentGroupResponses["G9_LE_9_3"]
        )

        // Reset G9 clean, test Group 12 ALE 12.3 dual modality conflict + human resolution + notes
        vm.selectEdiGroupFromBank(12)
        vm.resolveEdiPendingGroupChange(EdiGroupChangeResolutionAction.DISCARD_PREVIOUS_CONFIRMED_AND_SWITCH_TO_CLEAN_TARGET)
        vm.openEdiAxisQuestionnaire(EdiAxis.ALE)
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("edi_dual_obs_yes_G12_ALE_12_3").performScrollTo().performClick()
        composeRule.onNodeWithTag("edi_dual_int_no_G12_ALE_12_3").performScrollTo().performClick()
        composeRule.onNodeWithTag("edi_dual_conflict_G12_ALE_12_3").assertIsDisplayed()
        assertEquals(
            EdiItemResponse.DISCREPANCY_REQUIRES_HUMAN_RESOLUTION,
            vm.uiState.value.ediGlobalEvaluation.aleResponsesResolved["G12_ALE_12_3"]
        )

        // Human resolution overrides discrepancy
        composeRule.onNodeWithTag("edi_dual_resolve_no_G12_ALE_12_3").performScrollTo().performClick()
        assertEquals(
            EdiItemResponse.NO,
            vm.uiState.value.ediGlobalEvaluation.aleResponsesResolved["G12_ALE_12_3"]
        )

        // Clinical note stored separately without modifying responses
        composeRule.onNodeWithTag("edi_note_toggle_G12_ALE_12_3").performScrollTo().performClick()
        composeRule.onNodeWithTag("edi_note_input_G12_ALE_12_3").performScrollTo().performTextInput("Observado en consultorio")
        assertEquals(
            "Observado en consultorio",
            vm.uiState.value.ediEngineState.activeSession.itemClinicalNotes["G12_ALE_12_3"]
        )
    }

    @Test
    fun `04 fallback activates on all NO and deactivates preserving historical answers when changed to YES`() {
        val vm = CredEdiViewModel()
        vm.selectTab(AppTab.EDI)
        vm.selectEdiGroupFromBank(5)
        vm.openEdiAxisQuestionnaire(EdiAxis.AREAS)
        vm.selectEdiDevAreaFilter(EdiDevArea.MG)

        composeRule.setContent {
            CredEdiApp(viewModel = vm)
        }

        // Answer 0/2 YES (both NO) on G5 MG -> fallback section activates (Group 4 MG)
        composeRule.onNodeWithTag("edi_resp_no_G5_MG_5_1").performScrollTo().performClick()
        composeRule.onNodeWithTag("edi_resp_no_G5_MG_5_2").performScrollTo().performClick()
        composeRule.onNodeWithTag("edi_fallback_section_MG").performScrollTo().assertIsDisplayed()

        // Record fallback answers -> MG reaches AMARILLO via fallback
        composeRule.onNodeWithTag("edi_resp_yes_G5_RETRO_G4_MG_1").performScrollTo().performClick()
        composeRule.onNodeWithTag("edi_resp_yes_G5_RETRO_G4_MG_2").performScrollTo().performClick()
        assertEquals(
            com.example.model.EdiAreaColorResult.AMARILLO,
            vm.uiState.value.ediAxis3AreasSummary.areaResults[EdiDevArea.MG]?.status
        )

        // Change G5_MG_5_1 back to YES -> fallback section disappears, ignored historical preserved
        composeRule.onNodeWithTag("edi_resp_yes_G5_MG_5_1").performScrollTo().performClick()
        composeRule.onNodeWithTag("edi_fallback_section_MG").assertDoesNotExist()
        composeRule.onNodeWithTag("edi_fallback_ignored_note_MG").performScrollTo().assertIsDisplayed()
        assertTrue(
            vm.uiState.value.ediAxis3AreasSummary.areaResults[EdiDevArea.MG]
                ?.preservedHistoricalFallbackResponses?.isNotEmpty() == true
        )
    }

    @Test
    fun `05 official exceptions G8 to G7 2 items, G13 CO-R, G15 no EN13, G1-12 no CO, G1 no ALA`() {
        val vm = CredEdiViewModel()
        vm.selectTab(AppTab.EDI)

        // Exception A: Group 8 -> fallback to 2 items of Group 7
        vm.selectEdiGroupFromBank(8)
        vm.recordEdiCurrentItemResponse("G8_MG_8_1", EdiItemResponse.NO)
        vm.recordEdiCurrentItemResponse("G8_MG_8_2", EdiItemResponse.NO)
        vm.recordEdiCurrentItemResponse("G8_MG_8_3", EdiItemResponse.NO)
        val g8Mg = vm.uiState.value.ediAxis3AreasSummary.areaResults[EdiDevArea.MG]!!
        assertTrue(g8Mg.fallbackRequired)
        assertEquals(7, g8Mg.fallbackSourceGroupNumber)
        assertEquals(2, g8Mg.fallbackItems.size)
        // CO does not apply in G1-G12
        assertEquals(
            com.example.model.EdiAreaColorResult.NOT_APPLICABLE,
            vm.uiState.value.ediAxis3AreasSummary.areaResults[EdiDevArea.CO]?.status
        )

        // Exception B: Group 13 CO -> special CO-R 13.1..13.3 in UI
        vm.selectEdiGroupFromBank(13)
        vm.resolveEdiPendingGroupChange(EdiGroupChangeResolutionAction.DISCARD_PREVIOUS_CONFIRMED_AND_SWITCH_TO_CLEAN_TARGET)
        vm.recordEdiCurrentItemResponse("G13_CO_13_1", EdiItemResponse.NO)
        vm.recordEdiCurrentItemResponse("G13_CO_13_2", EdiItemResponse.NO)
        vm.recordEdiCurrentItemResponse("G13_CO_13_3", EdiItemResponse.NO)
        val g13Co = vm.uiState.value.ediAxis3AreasSummary.areaResults[EdiDevArea.CO]!!
        assertTrue(g13Co.fallbackRequired)
        assertTrue(g13Co.usesSpecialCoRFallback)
        assertEquals(listOf("CO-R 13.1", "CO-R 13.2", "CO-R 13.3"), g13Co.fallbackItems.map { it.sourceItem.itemCode })

        // Exception C: Group 15 EN 1.3 does not apply + Group 1 ALA does not apply in UI
        vm.selectEdiGroupFromBank(15)
        vm.resolveEdiPendingGroupChange(EdiGroupChangeResolutionAction.DISCARD_PREVIOUS_CONFIRMED_AND_SWITCH_TO_CLEAN_TARGET)
        vm.openEdiAxisQuestionnaire(EdiAxis.EN)
        composeRule.setContent { CredEdiApp(viewModel = vm) }
        composeRule.onNodeWithTag("edi_en13_not_applicable_g15").assertIsDisplayed()
        composeRule.onNodeWithTag("edi_item_card_EN_1_3").assertDoesNotExist()

        vm.selectEdiGroupFromBank(1)
        vm.openEdiAxisQuestionnaire(EdiAxis.ALA)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("edi_ala_not_applicable_g1").assertIsDisplayed()
    }

    @Test
    fun `06 global semaphore VERDE AMARILLO ROJO INCOMPLETE rendered exclusively from EdiGlobalSemaphoreEngine`() {
        val vm = CredEdiViewModel()
        vm.selectTab(AppTab.EDI)
        vm.selectEdiGroupFromBank(3)
        vm.navigateEdiView(EdiInternalView.RESULTS_TRACEABILITY)

        composeRule.setContent { CredEdiApp(viewModel = vm) }

        // Incomplete initially -> INCOMPLETE
        assertEquals(EdiGlobalTrafficLight.INCOMPLETE, vm.uiState.value.ediGlobalEvaluation.trafficLight)
        composeRule.onNodeWithTag("edi_global_semaphore_label").assertIsDisplayed()
        composeRule.onNodeWithText(EdiGlobalTrafficLight.INCOMPLETE.displayLabel).assertIsDisplayed()

        // Complete G3 all normal -> VERDE
        fillAllMandatoryForGroupAsGreen(vm, 3)
        composeRule.waitForIdle()
        assertEquals(EdiGlobalTrafficLight.VERDE, vm.uiState.value.ediGlobalEvaluation.trafficLight)
        composeRule.onNodeWithText(EdiGlobalTrafficLight.VERDE.displayLabel).assertIsDisplayed()

        // G3: 1 FRB + 1 ALE positive -> AMARILLO
        vm.recordEdiCurrentItemResponse("FRB_1", EdiItemResponse.YES)
        vm.recordEdiCurrentItemResponse("G3_ALE_3_1", EdiItemResponse.YES)
        composeRule.waitForIdle()
        assertEquals(EdiGlobalTrafficLight.AMARILLO, vm.uiState.value.ediGlobalEvaluation.trafficLight)
        composeRule.onNodeWithText(EdiGlobalTrafficLight.AMARILLO.displayLabel).assertIsDisplayed()

        // G3: Positive ALA -> ROJO
        vm.recordEdiCurrentItemResponse("G3_ALA_3_1", EdiItemResponse.YES)
        composeRule.waitForIdle()
        assertEquals(EdiGlobalTrafficLight.ROJO, vm.uiState.value.ediGlobalEvaluation.trafficLight)
        composeRule.onNodeWithText(EdiGlobalTrafficLight.ROJO.displayLabel).assertIsDisplayed()
    }

    @Test
    fun `07 priority clinical warnings stay visible in UI even while evaluation is INCOMPLETE`() {
        val vm = CredEdiViewModel()
        vm.selectTab(AppTab.EDI)
        vm.selectEdiGroupFromBank(4)
        vm.navigateEdiView(EdiInternalView.RESULTS_TRACEABILITY)

        // Only mark ALA 4.1 = YES while leaving all areas and EN unanswered -> INCOMPLETE + priority alert
        vm.recordEdiCurrentItemResponse("G4_ALA_4_1", EdiItemResponse.YES)
        assertEquals(EdiGlobalTrafficLight.INCOMPLETE, vm.uiState.value.ediGlobalEvaluation.trafficLight)
        assertTrue(vm.uiState.value.ediGlobalEvaluation.hasPriorityCriticalWarning)

        composeRule.setContent { CredEdiApp(viewModel = vm) }
        composeRule.onNodeWithTag("edi_priority_warning_banner").assertIsDisplayed()
        composeRule.onNodeWithText(EdiGlobalTrafficLight.INCOMPLETE.displayLabel).assertIsDisplayed()
    }

    @Test
    fun `08 optional FRB and ALE in groups 5 to 15 never block global semaphore`() {
        val vm = CredEdiViewModel()
        vm.selectTab(AppTab.EDI)
        vm.selectEdiGroupFromBank(10)

        // Complete mandatory Eje 3, Eje 4 (EN), Eje 5 (ALA) for G10, leave FRB & ALE UNANSWERED + missing material
        fillAllMandatoryForGroupAsGreen(vm, 10, skipOptionalFrbAndAleInG5To15 = true)
        vm.recordEdiCurrentItemResponse("G10_ALE_10_1", EdiItemResponse.NOT_EVALUATED_MISSING_MATERIAL)

        val eval = vm.uiState.value.ediGlobalEvaluation
        assertEquals(EdiGlobalTrafficLight.VERDE, eval.trafficLight)
        assertTrue(eval.isDefinitive)
        assertTrue(eval.nonBlockingClinicalWarnings.isNotEmpty())

        vm.navigateEdiView(EdiInternalView.RESULTS_TRACEABILITY)
        composeRule.setContent { CredEdiApp(viewModel = vm) }
        composeRule.onNodeWithText(EdiGlobalTrafficLight.VERDE.displayLabel).assertIsDisplayed()
        composeRule.onNodeWithTag("edi_non_blocking_warnings_card").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `09 corrected age weeks and days discrepancy blocks auto-selection and unlocks after documented resolution`() {
        val vm = CredEdiViewModel()
        vm.selectTab(AppTab.EDI)

        // 1. Out of range (< 1m): Birth 01/03/2025 -> Eval 20/03/2025 (19 days)
        vm.updateFechaNacimiento("01/03/2025")
        vm.updateFechaEvaluacion("20/03/2025")
        assertTrue(vm.uiState.value.ediIsOutOfOfficialAgeRange)
        assertTrue(vm.uiState.value.ediGlobalEvaluation.blockedByOutOfRangeAge)
        assertEquals(EdiGlobalTrafficLight.INCOMPLETE, vm.uiState.value.ediGlobalEvaluation.trafficLight)

        // 2. Consistent example from manual: 14/07/2024 -> 20/03/2025 with 34s 0d -> 6m 24d vs 6m 23d (both G5)
        vm.togglePrematurez(true)
        vm.updateEgSemanas(34)
        vm.updateEgDias(0)
        vm.updateFechaNacimiento("14/07/2024")
        vm.updateFechaEvaluacion("20/03/2025")
        assertEquals(EdiGroupAutoSelectionStatus.AUTO_SELECTED_CONSISTENT, vm.uiState.value.ediCorrectedAgeComparison.autoSelectionStatus)
        assertEquals(5, vm.uiState.value.ediEffectiveGroupNumber)

        // 3. Exact discrepancy boundary across Manual EDI (30d/12m) vs Calendar CRED:
        //    Term child born 31/01/2025 evaluated on 31/03/2025 -> Calendar=2m 0d (G2), Manual EDI=2m 0d;
        //    Let's test 31/01/2025 -> 01/03/2025: Calendar CRED = 1m 1d (G1); Manual EDI: d(1)<31 -> borrow 30d: d=31-31=0d, m=2-1=1m -> 1m 0d (both G1).
        //    Find exact boundary where Calendar=1m 29d vs 2m 0d or 3m 29d vs 4m 0d:
        //    Birth 30/01/2025 -> Eval 29/03/2025: Calendar = 1m 29d (G1); Manual = (29<30 -> d=59-30=29, m=2-1=1 -> 1m 29d).
        //    Birth 31/01/2025 -> Eval 30/03/2025: Calendar = 1m 30d -> actually Period.between(2025-01-31, 2025-03-30) = 1m 27d, wait 2024 leap or 30d->31d month!
        //    Birth 01/03/2025 -> Eval 01/05/2025 preterm 35w 4d (prematurity = 31d):
        //    Let's search dynamically in test or use known date pair:
        //    Birth 15/02/2025 -> Eval 15/05/2025 preterm 38w? No, 36w 5d (prematurity=23d):
        //    Calendar: 15/02 + 23d = 10/03/2025 -> Eval 10/05/2025 = 2m 0d (G2); Manual: crono=2m 25d - 23d = 2m 2d.
        //    What about Eval 09/05/2025 with 15/02/2025 & 36w 5d (23d)? Calendar: 10/03 -> 09/05 = 1m 29d (G1)! Manual: crono 15/02->09/05 (9<15 -> d=39-15=24d, m=4-2=2m) = 2m 24d - 23d = 2m 1d (G2)!!
        vm.updateFechaNacimiento("15/02/2025")
        vm.updateFechaEvaluacion("09/05/2025")
        vm.togglePrematurez(true)
        vm.updateEgSemanas(36)
        vm.updateEgDias(5)

        val comp = vm.uiState.value.ediCorrectedAgeComparison
        assertEquals("36s 5d", vm.uiState.value.ediGestationalAgeInput.originalLabel)
        assertEquals(
            EdiGroupAutoSelectionStatus.BLOCKED_GROUP_DISCREPANCY_REQUIRES_PROFESSIONAL_REVIEW,
            comp.autoSelectionStatus
        )
        assertTrue(vm.uiState.value.ediHasUnresolvedAgeDiscrepancy)
        assertTrue(vm.uiState.value.ediGlobalEvaluation.blockedByAgeDiscrepancy)
        assertEquals(EdiGlobalTrafficLight.INCOMPLETE, vm.uiState.value.ediGlobalEvaluation.trafficLight)

        // Resolve via UI in Bank of 15 Groups
        vm.selectTab(AppTab.EDI)
        vm.navigateEdiView(EdiInternalView.GROUP_BANK_15)
        composeRule.setContent { CredEdiApp(viewModel = vm) }

        composeRule.onNodeWithTag("edi_discrepancy_panel").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("edi_discrepancy_candidate_G2").performScrollTo().performClick()
        composeRule.onNodeWithTag("edi_discrepancy_note_input")
            .performScrollTo()
            .performTextInput("Paciente acorde a Grupo 2 por criterio clínico")
        composeRule.onNodeWithTag("edi_discrepancy_confirm_btn").performScrollTo().performClick()

        assertFalse(vm.uiState.value.ediHasUnresolvedAgeDiscrepancy)
        assertEquals(2, vm.uiState.value.ediEffectiveGroupNumber)
        assertFalse(vm.uiState.value.ediGlobalEvaluation.blockedByAgeDiscrepancy)
    }

    @Test
    fun `10 group change confirmation preserves or archives recorded answers without reusing across groups`() {
        val vm = CredEdiViewModel()
        vm.selectTab(AppTab.EDI)
        vm.selectEdiGroupFromBank(3)
        vm.recordEdiCurrentItemResponse("G3_MG_3_1", EdiItemResponse.YES)

        // Attempt switch to Group 4 while G3 has recorded response -> triggers confirmation dialog
        vm.navigateEdiView(EdiInternalView.GROUP_BANK_15)
        composeRule.setContent { CredEdiApp(viewModel = vm) }
        composeRule.onNodeWithTag("edi_bank_group_4").performScrollTo().performClick()

        assertNotNull(vm.uiState.value.ediEngineState.pendingGroupChangeConfirmation)
        composeRule.onNodeWithTag("edi_group_change_dialog").assertIsDisplayed()

        // Cancel first -> stays on G3 with response intact
        composeRule.onNodeWithTag("edi_confirm_cancel_switch").performClick()
        assertNull(vm.uiState.value.ediEngineState.pendingGroupChangeConfirmation)
        assertEquals(3, vm.uiState.value.ediEffectiveGroupNumber)
        assertEquals(
            EdiItemResponse.YES,
            vm.uiState.value.ediEngineState.activeSession.currentGroupResponses["G3_MG_3_1"]
        )

        // Switch again and archive G3 -> switches cleanly to G4 with ZERO reused answers
        composeRule.onNodeWithTag("edi_bank_group_4").performScrollTo().performClick()
        composeRule.onNodeWithTag("edi_confirm_archive_switch").performClick()
        assertEquals(4, vm.uiState.value.ediEffectiveGroupNumber)
        assertTrue(vm.uiState.value.ediEngineState.archivedSessionsByGroup.containsKey(3))
        assertEquals(
            EdiItemResponse.UNANSWERED,
            vm.uiState.value.ediEngineState.activeSession.currentGroupResponses["G4_MG_4_1"]
        )
        assertFalse(vm.uiState.value.ediEngineState.activeSession.currentGroupResponses.containsKey("G3_MG_3_1"))
    }

    @Test
    fun `11 pdf export in Window 4 UI exports official sheet when complete and draft when incomplete`() {
        val vm = CredEdiViewModel()
        vm.selectTab(AppTab.EDI)
        vm.selectEdiGroupFromBank(2)
        fillAllMandatoryForGroupAsGreen(vm, 2)
        vm.navigateEdiView(EdiInternalView.RESULTS_TRACEABILITY)

        composeRule.setContent { CredEdiApp(viewModel = vm) }
        composeRule.onNodeWithTag("edi_pdf_export_button")
            .performScrollTo()
            .assertIsDisplayed()
            .assertIsEnabled()
        composeRule.onNodeWithText("Exportar Ficha Oficial EDI (PDF)")
            .assertIsDisplayed()

        // Ejecutar exportación real desde la UI
        composeRule.onNodeWithTag("edi_pdf_export_button").performClick()
        composeRule.onNodeWithTag("edi_pdf_export_result")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("FICHA OFICIAL DEFINITIVA EXPORTADA", substring = true)
            .assertIsDisplayed()

        // Cambiar a estado INCOMPLETE -> botón cambia a Borrador
        vm.recordEdiCurrentItemResponse("G2_MG_2_1", EdiItemResponse.UNANSWERED)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Exportar Borrador (Evaluación Incompleta) PDF")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag("edi_pdf_export_button").performClick()
        composeRule.onNodeWithText("BORRADOR INCOMPLETO EXPORTADO", substring = true)
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun `12 windows 1 2 and 3 continue working without regressions`() {
        val vm = CredEdiViewModel()
        composeRule.setContent { CredEdiApp(viewModel = vm) }

        // Tab 1: Calculadora CRED
        vm.updateFechaNacimiento("01/10/2024")
        vm.updateFechaEvaluacion("16/02/2025")
        vm.updatePesoInput("7.00")
        vm.updateLongitudInput("64.0")
        vm.updatePerimetroCefalicoInput("41.5")
        composeRule.waitForIdle()
        assertEquals("0a 4m 15d", vm.uiState.value.edadCronologicaAMD)
        assertTrue(vm.uiState.value.evalPE.zScore.isFinite())

        // Tab 2: Curvas OMS
        composeRule.onNodeWithTag("tab_curvas").performClick()
        assertEquals(AppTab.CURVAS_OMS, vm.uiState.value.currentTab)

        // Tab 3: Diagnóstico
        composeRule.onNodeWithTag("tab_diagnostico").performClick()
        assertEquals(AppTab.DIAGNOSTICO, vm.uiState.value.currentTab)
        assertTrue(vm.uiState.value.globalDiagnosis.title.isNotBlank())

        // Tab 4: EDI works independently without altering Tabs 1-3 data
        composeRule.onNodeWithTag("tab_edi").performClick()
        assertEquals(AppTab.EDI, vm.uiState.value.currentTab)
        assertEquals("0a 4m 15d", vm.uiState.value.edadCronologicaAMD)
        assertEquals(4, vm.uiState.value.ediEffectiveGroupNumber) // 4m 15d -> Grupo 4
    }
}
