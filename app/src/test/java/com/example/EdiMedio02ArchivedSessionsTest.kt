package com.example

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ApplicationProvider
import com.example.model.AppTab
import com.example.model.EdiArchivedCompatibilityStatus
import com.example.model.EdiArchivedOriginValidationState
import com.example.model.EdiAxis
import com.example.model.EdiDevArea
import com.example.model.EdiGlobalTrafficLight
import com.example.model.EdiGroupChangeResolutionAction
import com.example.model.EdiInternalView
import com.example.model.EdiItemResponse
import com.example.model.EdiOfficialCatalog
import com.example.model.EdiPendingRestoreRequest
import com.example.model.EdiPhase1SessionManager
import com.example.model.SexoBiologico
import com.example.util.EdiPdfExporter
import com.example.viewmodel.CredEdiViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * Suite de pruebas obligatoria para MEDIO-02 y su Corrección de Seguridad (R1, R2, R3):
 * Recuperación segura de evaluaciones EDI archivadas sin pérdida de respuestas,
 * notas clínicas, retrocesos ni evidencias duales, con protección estricta de episodios
 * y prevención de mezcla de evaluaciones entre pacientes.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class EdiMedio02ArchivedSessionsTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun fillAllMandatoryForGroupAsGreen(vm: CredEdiViewModel, groupNumber: Int) {
        val spec = EdiOfficialCatalog.getGroup(groupNumber)
        EdiOfficialCatalog.frbItems.forEach { item ->
            vm.recordEdiCurrentItemResponse(item.uniqueId, EdiItemResponse.NO)
        }
        spec.aleItems.forEach { item ->
            vm.recordEdiCurrentItemResponse(item.uniqueId, EdiItemResponse.NO)
        }
        spec.areaItems.values.flatten().forEach { item ->
            vm.recordEdiCurrentItemResponse(item.uniqueId, EdiItemResponse.YES)
        }
        EdiOfficialCatalog.getApplicableEnItems(groupNumber).forEach { item ->
            vm.recordEdiCurrentItemResponse(item.uniqueId, EdiItemResponse.NO)
        }
        spec.alaItems.forEach { item ->
            vm.recordEdiCurrentItemResponse(item.uniqueId, EdiItemResponse.NO)
        }
    }

    @Test
    fun `01 archive single evaluation of a group and preserve all its responses`() {
        val vm = CredEdiViewModel()
        vm.updateFechaEvaluacion("20/05/2025")
        vm.updateFechaNacimiento("10/08/2024") // 9m 10d -> Grupo 6
        vm.confirmEdiTermWithoutWeeks()
        vm.selectTab(AppTab.EDI)

        assertEquals(6, vm.uiState.value.ediEffectiveGroupNumber)
        vm.recordEdiCurrentItemResponse("G6_MG_6_1", EdiItemResponse.YES)
        vm.recordEdiCurrentItemResponse("G6_MF_6_2", EdiItemResponse.NO)
        vm.updateEdiItemClinicalNote("G6_MG_6_1", "Sostén sentado firme")

        // Request group change to G7 -> confirmation required
        vm.selectEdiGroupFromBank(7)
        assertNotNull(vm.uiState.value.ediEngineState.pendingGroupChangeConfirmation)

        vm.resolveEdiPendingGroupChange(
            EdiGroupChangeResolutionAction.ARCHIVE_PREVIOUS_AND_SWITCH_TO_CLEAN_TARGET
        )

        val engine = vm.uiState.value.ediEngineState
        assertEquals(7, engine.activeGroupNumber)
        assertFalse(engine.activeSession.hasRecordedClinicalResponses)
        assertTrue(engine.hasAnyDataInCurrentEpisode)
        assertEquals(1, engine.archivedSessionHistory.size)

        val entry = engine.archivedSessionHistory.first()
        assertEquals("ARCH_001_G6", entry.archiveId)
        assertEquals(1, entry.sequenceNumber)
        assertEquals(6, entry.groupNumber)
        assertEquals(EdiItemResponse.YES, entry.snapshot.currentGroupResponses["G6_MG_6_1"])
        assertEquals(EdiItemResponse.NO, entry.snapshot.currentGroupResponses["G6_MF_6_2"])
        assertEquals("Sostén sentado firme", entry.snapshot.itemClinicalNotes["G6_MG_6_1"])
        assertEquals(entry.snapshot, engine.archivedSessionsByGroup[6])
    }

    @Test
    fun `02 archive two distinct evaluations of the same group without overwriting the first`() {
        val vm = CredEdiViewModel()
        vm.updateFechaEvaluacion("20/05/2025")
        vm.updateFechaNacimiento("10/08/2024") // Grupo 6
        vm.confirmEdiTermWithoutWeeks()

        // First evaluation in G6
        vm.recordEdiCurrentItemResponse("G6_MG_6_1", EdiItemResponse.YES)
        vm.updateEdiItemClinicalNote("G6_MG_6_1", "Primera evaluación G6")
        vm.selectEdiGroupFromBank(7)
        vm.resolveEdiPendingGroupChange(
            EdiGroupChangeResolutionAction.ARCHIVE_PREVIOUS_AND_SWITCH_TO_CLEAN_TARGET
        )

        // Return to G6 (G7 is clean so switches directly) and record a second evaluation in G6
        vm.selectEdiGroupFromBank(6)
        assertEquals(6, vm.uiState.value.ediEffectiveGroupNumber)
        vm.recordEdiCurrentItemResponse("G6_MG_6_1", EdiItemResponse.NO)
        vm.recordEdiCurrentItemResponse("G6_LE_6_1", EdiItemResponse.YES)
        vm.updateEdiItemClinicalNote("G6_MG_6_1", "Segunda evaluación G6")

        vm.selectEdiGroupFromBank(8)
        vm.resolveEdiPendingGroupChange(
            EdiGroupChangeResolutionAction.ARCHIVE_PREVIOUS_AND_SWITCH_TO_CLEAN_TARGET
        )

        val g6History = vm.uiState.value.ediEngineState.getArchivedEntriesForGroup(6)
        assertEquals(2, g6History.size)

        val first = g6History[0]
        val second = g6History[1]
        assertNotEquals(first.archiveId, second.archiveId)
        assertEquals("ARCH_001_G6", first.archiveId)
        assertEquals("ARCH_002_G6", second.archiveId)
        assertEquals(1, first.sequenceNumber)
        assertEquals(2, second.sequenceNumber)

        // First evaluation remains completely intact and was NOT overwritten
        assertEquals(EdiItemResponse.YES, first.snapshot.currentGroupResponses["G6_MG_6_1"])
        assertEquals("Primera evaluación G6", first.snapshot.itemClinicalNotes["G6_MG_6_1"])

        // Second evaluation has its own distinct responses
        assertEquals(EdiItemResponse.NO, second.snapshot.currentGroupResponses["G6_MG_6_1"])
        assertEquals(EdiItemResponse.YES, second.snapshot.currentGroupResponses["G6_LE_6_1"])
        assertEquals("Segunda evaluación G6", second.snapshot.itemClinicalNotes["G6_MG_6_1"])
    }

    @Test
    fun `03 discarding active session does not delete historical archived sessions of the same group`() {
        val vm = CredEdiViewModel()
        vm.updateFechaEvaluacion("20/05/2025")
        vm.updateFechaNacimiento("10/08/2024") // G6
        vm.confirmEdiTermWithoutWeeks()

        // Archive first G6 evaluation
        vm.recordEdiCurrentItemResponse("G6_MG_6_1", EdiItemResponse.YES)
        vm.updateEdiItemClinicalNote("G6_MG_6_1", "Sesión histórica protegida")
        vm.selectEdiGroupFromBank(7)
        vm.resolveEdiPendingGroupChange(
            EdiGroupChangeResolutionAction.ARCHIVE_PREVIOUS_AND_SWITCH_TO_CLEAN_TARGET
        )
        assertEquals(1, vm.uiState.value.ediEngineState.archivedSessionHistory.size)

        // Switch back to G6, record temporary responses, and then DISCARD active G6 when switching to G8
        vm.selectEdiGroupFromBank(6)
        vm.recordEdiCurrentItemResponse("G6_MG_6_1", EdiItemResponse.NO)
        vm.selectEdiGroupFromBank(8)
        vm.resolveEdiPendingGroupChange(
            EdiGroupChangeResolutionAction.DISCARD_PREVIOUS_CONFIRMED_AND_SWITCH_TO_CLEAN_TARGET
        )

        val engine = vm.uiState.value.ediEngineState
        assertEquals(8, engine.activeGroupNumber)
        // Historical archived entry of G6 must still exist intact!
        assertEquals(1, engine.archivedSessionHistory.size)
        val preservedEntry = engine.archivedSessionHistory.first()
        assertEquals("ARCH_001_G6", preservedEntry.archiveId)
        assertEquals(EdiItemResponse.YES, preservedEntry.snapshot.currentGroupResponses["G6_MG_6_1"])
        assertEquals("Sesión histórica protegida", preservedEntry.snapshot.itemClinicalNotes["G6_MG_6_1"])
        assertTrue(engine.archivedSessionsByGroup.containsKey(6))
    }

    @Test
    fun `04 opening preview of an archived session does not mutate the active session`() {
        val vm = CredEdiViewModel()
        vm.updateFechaEvaluacion("20/05/2025")
        vm.updateFechaNacimiento("10/08/2024") // G6
        vm.confirmEdiTermWithoutWeeks()

        vm.recordEdiCurrentItemResponse("G6_MG_6_1", EdiItemResponse.YES)
        vm.selectEdiGroupFromBank(7)
        vm.resolveEdiPendingGroupChange(
            EdiGroupChangeResolutionAction.ARCHIVE_PREVIOUS_AND_SWITCH_TO_CLEAN_TARGET
        )

        // Record active responses in G7
        vm.recordEdiCurrentItemResponse("G7_MG_7_1", EdiItemResponse.NO)
        val activeSnapshotBeforePreview = vm.uiState.value.ediEngineState.activeSession

        // Open preview of ARCH_001_G6
        vm.openEdiArchivedSessionPreview("ARCH_001_G6")
        val stateDuringPreview = vm.uiState.value.ediEngineState
        assertNotNull(stateDuringPreview.previewArchivedEntry)
        assertEquals("ARCH_001_G6", stateDuringPreview.previewArchivedEntry?.archiveId)
        assertEquals(7, stateDuringPreview.activeGroupNumber)
        assertEquals(activeSnapshotBeforePreview, stateDuringPreview.activeSession)

        // Close preview -> active session still untouched
        vm.closeEdiArchivedSessionPreview()
        assertNull(vm.uiState.value.ediEngineState.previewArchivedEntry)
        assertEquals(7, vm.uiState.value.ediEffectiveGroupNumber)
        assertEquals(activeSnapshotBeforePreview, vm.uiState.value.ediEngineState.activeSession)
    }

    @Test
    fun `05 restore archived session when active session is empty requires and respects explicit continuity confirmation`() {
        val vm = CredEdiViewModel()
        vm.updateFechaEvaluacion("20/05/2025")
        vm.updateFechaNacimiento("10/08/2024") // G6
        vm.confirmEdiTermWithoutWeeks()

        vm.recordEdiCurrentItemResponse("G6_MG_6_1", EdiItemResponse.YES)
        vm.recordEdiCurrentItemResponse("G6_SO_6_1", EdiItemResponse.YES)
        vm.selectEdiGroupFromBank(7)
        vm.resolveEdiPendingGroupChange(
            EdiGroupChangeResolutionAction.ARCHIVE_PREVIOUS_AND_SWITCH_TO_CLEAN_TARGET
        )

        assertFalse(vm.uiState.value.ediEngineState.activeSession.hasRecordedClinicalResponses)

        vm.requestRestoreEdiArchivedSession("ARCH_001_G6")
        val pendingRestore = vm.uiState.value.ediEngineState.pendingRestoreRequest
        assertNotNull(pendingRestore)
        assertFalse(pendingRestore!!.activeSessionWillBeArchivedFirst)
        assertFalse(pendingRestore.samePatientAndEpisodeConfirmed)

        vm.setRestoreSamePatientContinuityConfirmed(true)
        vm.confirmRestoreEdiArchivedSession()

        val engine = vm.uiState.value.ediEngineState
        assertNull(engine.pendingRestoreRequest)
        assertEquals(6, engine.activeGroupNumber)
        assertEquals("ARCH_001_G6", engine.restoredFromArchiveId)
        assertEquals(EdiItemResponse.YES, engine.activeSession.currentGroupResponses["G6_MG_6_1"])
        assertEquals(EdiItemResponse.YES, engine.activeSession.currentGroupResponses["G6_SO_6_1"])
        // No extra auto-archive was created because active session was empty
        assertEquals(1, engine.archivedSessionHistory.size)
    }

    @Test
    fun `06 restore archived session when active session has responses archives active session first`() {
        val vm = CredEdiViewModel()
        vm.updateFechaEvaluacion("20/05/2025")
        vm.updateFechaNacimiento("10/08/2024") // G6
        vm.confirmEdiTermWithoutWeeks()

        // Record & archive G6
        vm.recordEdiCurrentItemResponse("G6_MG_6_1", EdiItemResponse.YES)
        vm.updateEdiItemClinicalNote("G6_MG_6_1", "Nota original G6")
        vm.selectEdiGroupFromBank(7)
        vm.resolveEdiPendingGroupChange(
            EdiGroupChangeResolutionAction.ARCHIVE_PREVIOUS_AND_SWITCH_TO_CLEAN_TARGET
        )

        // Record responses in active G7
        vm.recordEdiCurrentItemResponse("G7_MG_7_1", EdiItemResponse.NO)
        vm.updateEdiItemClinicalNote("G7_MG_7_1", "Nota activa en G7 que no debe perderse")

        vm.requestRestoreEdiArchivedSession("ARCH_001_G6")
        val pendingRestore = vm.uiState.value.ediEngineState.pendingRestoreRequest
        assertNotNull(pendingRestore)
        assertTrue(pendingRestore!!.activeSessionWillBeArchivedFirst)

        vm.setRestoreSamePatientContinuityConfirmed(true)
        vm.confirmRestoreEdiArchivedSession()
        val engine = vm.uiState.value.ediEngineState
        assertEquals(6, engine.activeGroupNumber)
        assertEquals("ARCH_001_G6", engine.restoredFromArchiveId)
        assertEquals(EdiItemResponse.YES, engine.activeSession.currentGroupResponses["G6_MG_6_1"])
        assertEquals("Nota original G6", engine.activeSession.itemClinicalNotes["G6_MG_6_1"])
        assertFalse(engine.activeSession.currentGroupResponses.containsKey("G7_MG_7_1"))

        // Active G7 session was automatically archived as ARCH_002_G7 before restoring G6
        assertEquals(2, engine.archivedSessionHistory.size)
        val autoArchivedG7 = engine.archivedSessionHistory[1]
        assertEquals("ARCH_002_G7", autoArchivedG7.archiveId)
        assertEquals(7, autoArchivedG7.groupNumber)
        assertEquals(EdiItemResponse.NO, autoArchivedG7.snapshot.currentGroupResponses["G7_MG_7_1"])
        assertEquals(
            "Nota activa en G7 que no debe perderse",
            autoArchivedG7.snapshot.itemClinicalNotes["G7_MG_7_1"]
        )
    }

    @Test
    fun `07 canceling restoration keeps active session completely intact`() {
        val vm = CredEdiViewModel()
        vm.updateFechaEvaluacion("20/05/2025")
        vm.updateFechaNacimiento("10/08/2024") // G6
        vm.confirmEdiTermWithoutWeeks()

        vm.recordEdiCurrentItemResponse("G6_MG_6_1", EdiItemResponse.YES)
        vm.selectEdiGroupFromBank(7)
        vm.resolveEdiPendingGroupChange(
            EdiGroupChangeResolutionAction.ARCHIVE_PREVIOUS_AND_SWITCH_TO_CLEAN_TARGET
        )

        vm.recordEdiCurrentItemResponse("G7_MG_7_1", EdiItemResponse.YES)
        val snapshotBefore = vm.uiState.value.ediEngineState.activeSession

        vm.requestRestoreEdiArchivedSession("ARCH_001_G6")
        assertNotNull(vm.uiState.value.ediEngineState.pendingRestoreRequest)

        vm.cancelRestoreEdiArchivedSession()
        val engine = vm.uiState.value.ediEngineState
        assertNull(engine.pendingRestoreRequest)
        assertEquals(7, engine.activeGroupNumber)
        assertEquals(snapshotBefore, engine.activeSession)
        assertEquals(1, engine.archivedSessionHistory.size)
    }

    @Test
    fun `08 restore responses across all five official EDI axes`() {
        val vm = CredEdiViewModel()
        vm.updateFechaEvaluacion("20/05/2025")
        vm.updateFechaNacimiento("10/01/2022") // 40m -> Grupo 13 (includes all 5 axes including CO)
        vm.selectTab(AppTab.EDI)
        assertEquals(13, vm.uiState.value.ediEffectiveGroupNumber)

        // Axis 1: FRB
        vm.recordEdiCurrentItemResponse("FRB_1", EdiItemResponse.YES)
        vm.recordEdiCurrentItemResponse("FRB_2", EdiItemResponse.NO)
        // Axis 2: ALE
        vm.recordEdiCurrentItemResponse("G13_ALE_13_1", EdiItemResponse.YES)
        // Axis 3: AREAS (MG, MF, LE, SO, CO)
        vm.recordEdiCurrentItemResponse("G13_MG_13_1", EdiItemResponse.YES)
        vm.recordEdiCurrentItemResponse("G13_MF_13_1", EdiItemResponse.NO)
        vm.recordEdiCurrentItemResponse("G13_LE_13_1", EdiItemResponse.YES)
        vm.recordEdiCurrentItemResponse("G13_SO_13_1", EdiItemResponse.YES)
        vm.recordEdiCurrentItemResponse("G13_CO_13_1", EdiItemResponse.YES)
        // Axis 4: EN
        vm.recordEdiCurrentItemResponse("EN_1_1", EdiItemResponse.NO)
        vm.recordEdiCurrentItemResponse("EN_1_2", EdiItemResponse.YES)
        // Axis 5: ALA
        vm.recordEdiCurrentItemResponse("G13_ALA_13_1", EdiItemResponse.YES)

        vm.selectEdiGroupFromBank(14)
        vm.resolveEdiPendingGroupChange(
            EdiGroupChangeResolutionAction.ARCHIVE_PREVIOUS_AND_SWITCH_TO_CLEAN_TARGET
        )
        assertEquals(14, vm.uiState.value.ediEffectiveGroupNumber)

        vm.requestRestoreEdiArchivedSession("ARCH_001_G13")
        vm.confirmRestoreEdiArchivedSession(confirmSamePatientContinuity = true)

        val restored = vm.uiState.value.ediEngineState.activeSession
        assertEquals(13, vm.uiState.value.ediEffectiveGroupNumber)
        assertEquals(EdiItemResponse.YES, restored.frbResponses["FRB_1"])
        assertEquals(EdiItemResponse.NO, restored.frbResponses["FRB_2"])
        assertEquals(EdiItemResponse.YES, restored.currentGroupResponses["G13_ALE_13_1"])
        assertEquals(EdiItemResponse.YES, restored.currentGroupResponses["G13_MG_13_1"])
        assertEquals(EdiItemResponse.NO, restored.currentGroupResponses["G13_MF_13_1"])
        assertEquals(EdiItemResponse.YES, restored.currentGroupResponses["G13_LE_13_1"])
        assertEquals(EdiItemResponse.YES, restored.currentGroupResponses["G13_SO_13_1"])
        assertEquals(EdiItemResponse.YES, restored.currentGroupResponses["G13_CO_13_1"])
        assertEquals(EdiItemResponse.NO, restored.enResponses["EN_1_1"])
        assertEquals(EdiItemResponse.YES, restored.enResponses["EN_1_2"])
        assertEquals(EdiItemResponse.YES, restored.currentGroupResponses["G13_ALA_13_1"])
    }

    @Test
    fun `09 restore fallback responses including special CO-R fallback of Group 13`() {
        val vm = CredEdiViewModel()
        vm.updateFechaEvaluacion("20/05/2025")
        vm.updateFechaNacimiento("10/01/2022") // 40m -> Grupo 13
        vm.selectTab(AppTab.EDI)

        // Trigger MG fallback (G12) and CO special fallback (CO-R 13.1..13.3)
        vm.recordEdiCurrentItemResponse("G13_MG_13_1", EdiItemResponse.NO)
        vm.recordEdiCurrentItemResponse("G13_MG_13_2", EdiItemResponse.NO)
        vm.recordEdiCurrentItemResponse("G13_MG_13_3", EdiItemResponse.NO)
        vm.recordEdiFallbackResponse("G13_RETRO_G12_MG_1", EdiItemResponse.YES)
        vm.recordEdiFallbackResponse("G13_RETRO_G12_MG_2", EdiItemResponse.YES)
        vm.recordEdiFallbackResponse("G13_RETRO_G12_MG_3", EdiItemResponse.YES)

        val corIds = EdiOfficialCatalog.specialGroup13CoRFallbackItems.map { it.uniqueId }
        assertEquals(3, corIds.size)

        vm.recordEdiCurrentItemResponse("G13_CO_13_1", EdiItemResponse.NO)
        vm.recordEdiCurrentItemResponse("G13_CO_13_2", EdiItemResponse.NO)
        vm.recordEdiCurrentItemResponse("G13_CO_13_3", EdiItemResponse.NO)
        vm.recordEdiFallbackResponse(corIds[0], EdiItemResponse.YES)
        vm.recordEdiFallbackResponse(corIds[1], EdiItemResponse.NO)
        vm.recordEdiFallbackResponse(corIds[2], EdiItemResponse.YES)

        vm.selectEdiGroupFromBank(14)
        vm.resolveEdiPendingGroupChange(
            EdiGroupChangeResolutionAction.ARCHIVE_PREVIOUS_AND_SWITCH_TO_CLEAN_TARGET
        )

        vm.requestRestoreEdiArchivedSession("ARCH_001_G13")
        vm.confirmRestoreEdiArchivedSession(confirmSamePatientContinuity = true)

        val restored = vm.uiState.value.ediEngineState.activeSession
        assertEquals(EdiItemResponse.YES, restored.fallbackGroupResponses["G13_RETRO_G12_MG_1"])
        assertEquals(EdiItemResponse.YES, restored.fallbackGroupResponses[corIds[0]])
        assertEquals(EdiItemResponse.NO, restored.fallbackGroupResponses[corIds[1]])
        assertEquals(EdiItemResponse.YES, restored.fallbackGroupResponses[corIds[2]])

        val coResult = vm.uiState.value.ediAxis3AreasSummary.areaResults[EdiDevArea.CO]!!
        assertTrue(coResult.fallbackRequired)
        assertTrue(coResult.usesSpecialCoRFallback)
    }

    @Test
    fun `10 restore per-item clinical notes intact`() {
        val vm = CredEdiViewModel()
        vm.updateFechaEvaluacion("20/05/2025")
        vm.updateFechaNacimiento("10/08/2024") // G6
        vm.confirmEdiTermWithoutWeeks()

        vm.recordEdiCurrentItemResponse("G6_MG_6_1", EdiItemResponse.YES)
        vm.updateEdiItemClinicalNote("G6_MG_6_1", "Nota MG 6.1: apoyo palmar estable")
        vm.updateEdiItemClinicalNote("G6_ALE_6_1", "Nota ALE 6.1: seguimiento ocular completo")

        vm.selectEdiGroupFromBank(7)
        vm.resolveEdiPendingGroupChange(
            EdiGroupChangeResolutionAction.ARCHIVE_PREVIOUS_AND_SWITCH_TO_CLEAN_TARGET
        )

        vm.requestRestoreEdiArchivedSession("ARCH_001_G6")
        vm.confirmRestoreEdiArchivedSession(confirmSamePatientContinuity = true)

        val notes = vm.uiState.value.ediEngineState.activeSession.itemClinicalNotes
        assertEquals(2, notes.size)
        assertEquals("Nota MG 6.1: apoyo palmar estable", notes["G6_MG_6_1"])
        assertEquals("Nota ALE 6.1: seguimiento ocular completo", notes["G6_ALE_6_1"])
    }

    @Test
    fun `11 restore dual modality evidence records for ALE 12_3, LE 13_3 and LE 14_3`() {
        val vm = CredEdiViewModel()
        vm.updateFechaEvaluacion("20/05/2025")
        vm.updateFechaNacimiento("10/03/2023") // 26m -> G12
        vm.selectTab(AppTab.EDI)

        // 1) G12_ALE_12_3 with conflict + human resolution
        vm.selectEdiGroupFromBank(12)
        vm.recordEdiDualEvidence(
            itemUniqueId = "G12_ALE_12_3",
            observationEvidence = true,
            interviewEvidence = false,
            humanResolutionIfConflict = false,
            note = "Resolución clínica directa en consultorio G12"
        )
        vm.selectEdiGroupFromBank(13)
        vm.resolveEdiPendingGroupChange(
            EdiGroupChangeResolutionAction.ARCHIVE_PREVIOUS_AND_SWITCH_TO_CLEAN_TARGET
        )

        // 2) G13_LE_13_3 dual evidence
        vm.recordEdiDualEvidence(
            itemUniqueId = "G13_LE_13_3",
            observationEvidence = true,
            interviewEvidence = true,
            humanResolutionIfConflict = null,
            note = "Coincidencia observación e interrogatorio G13"
        )
        vm.selectEdiGroupFromBank(14)
        vm.resolveEdiPendingGroupChange(
            EdiGroupChangeResolutionAction.ARCHIVE_PREVIOUS_AND_SWITCH_TO_CLEAN_TARGET
        )

        // 3) G14_LE_14_3 dual evidence + fallback dual evidence on G14_RETRO_G13_LE_3
        vm.recordEdiCurrentItemResponse("G14_LE_14_1", EdiItemResponse.NO)
        vm.recordEdiCurrentItemResponse("G14_LE_14_2", EdiItemResponse.NO)
        vm.recordEdiDualEvidence(
            itemUniqueId = "G14_LE_14_3",
            observationEvidence = false,
            interviewEvidence = false,
            humanResolutionIfConflict = null,
            note = "No logrado en G14 LE 14.3"
        )
        vm.recordEdiFallbackDualEvidence(
            fallbackUniqueId = "G14_RETRO_G13_LE_3",
            observationEvidence = true,
            interviewEvidence = false,
            humanResolutionIfConflict = true,
            note = "Retroceso LE 13.3 resuelto a favor"
        )
        vm.selectEdiGroupFromBank(15)
        vm.resolveEdiPendingGroupChange(
            EdiGroupChangeResolutionAction.ARCHIVE_PREVIOUS_AND_SWITCH_TO_CLEAN_TARGET
        )

        // Restore G12 and verify ALE 12.3 dual evidence
        vm.requestRestoreEdiArchivedSession("ARCH_001_G12")
        vm.confirmRestoreEdiArchivedSession(confirmSamePatientContinuity = true)
        val rec12 = vm.uiState.value.ediEngineState.activeSession.dualEvidenceRecords["G12_ALE_12_3"]
        assertNotNull(rec12)
        assertEquals(true, rec12?.directObservationEvidence)
        assertEquals(false, rec12?.directedInterviewEvidence)
        assertEquals(false, rec12?.professionalHumanResolution)
        assertEquals("Resolución clínica directa en consultorio G12", rec12?.clinicalJustificationNote)
        assertEquals(
            EdiItemResponse.NO,
            vm.uiState.value.ediEngineState.activeSession.currentGroupResponses["G12_ALE_12_3"]
        )

        // Restore G13 and verify LE 13.3 dual evidence
        vm.requestRestoreEdiArchivedSession("ARCH_002_G13")
        vm.confirmRestoreEdiArchivedSession(confirmSamePatientContinuity = true)
        val rec13 = vm.uiState.value.ediEngineState.activeSession.dualEvidenceRecords["G13_LE_13_3"]
        assertNotNull(rec13)
        assertEquals(true, rec13?.directObservationEvidence)
        assertEquals(true, rec13?.directedInterviewEvidence)
        assertEquals(
            EdiItemResponse.YES,
            vm.uiState.value.ediEngineState.activeSession.currentGroupResponses["G13_LE_13_3"]
        )

        // Restore G14 and verify LE 14.3 + fallback LE 13.3 dual evidence
        vm.requestRestoreEdiArchivedSession("ARCH_003_G14")
        vm.confirmRestoreEdiArchivedSession(confirmSamePatientContinuity = true)
        val rec14 = vm.uiState.value.ediEngineState.activeSession.dualEvidenceRecords["G14_LE_14_3"]
        val rec14Retro = vm.uiState.value.ediEngineState.activeSession.dualEvidenceRecords["G14_RETRO_G13_LE_3"]
        assertNotNull(rec14)
        assertNotNull(rec14Retro)
        assertEquals(true, rec14Retro?.professionalHumanResolution)
        assertEquals(
            EdiItemResponse.YES,
            vm.uiState.value.ediEngineState.activeSession.fallbackGroupResponses["G14_RETRO_G13_LE_3"]
        )
    }

    @Test
    fun `12 restore items with NOT_EVALUATED_MISSING_MATERIAL status`() {
        val vm = CredEdiViewModel()
        vm.updateFechaEvaluacion("20/05/2025")
        vm.updateFechaNacimiento("10/11/2023") // 18m 10d -> Grupo 9
        vm.confirmEdiTermWithoutWeeks()
        vm.selectTab(AppTab.EDI)

        assertEquals(9, vm.uiState.value.ediEffectiveGroupNumber)
        // G9_LE_9_3 requires material in protocol (Lámina A)
        vm.recordEdiCurrentItemResponse(
            "G9_LE_9_3",
            EdiItemResponse.NOT_EVALUATED_MISSING_MATERIAL
        )
        assertEquals(
            EdiItemResponse.NOT_EVALUATED_MISSING_MATERIAL,
            vm.uiState.value.ediEngineState.activeSession.currentGroupResponses["G9_LE_9_3"]
        )

        vm.selectEdiGroupFromBank(10)
        vm.resolveEdiPendingGroupChange(
            EdiGroupChangeResolutionAction.ARCHIVE_PREVIOUS_AND_SWITCH_TO_CLEAN_TARGET
        )

        vm.requestRestoreEdiArchivedSession("ARCH_001_G9")
        vm.confirmRestoreEdiArchivedSession(confirmSamePatientContinuity = true)

        assertEquals(9, vm.uiState.value.ediEffectiveGroupNumber)
        assertEquals(
            EdiItemResponse.NOT_EVALUATED_MISSING_MATERIAL,
            vm.uiState.value.ediEngineState.activeSession.currentGroupResponses["G9_LE_9_3"]
        )
    }

    @Test
    fun `13 restore a specific evaluation among multiple of the same group using its archiveId`() {
        val vm = CredEdiViewModel()
        vm.updateFechaEvaluacion("20/05/2025")
        vm.updateFechaNacimiento("10/08/2024") // G6
        vm.confirmEdiTermWithoutWeeks()

        // Evaluation 1 of G6 -> ARCH_001_G6
        vm.recordEdiCurrentItemResponse("G6_MG_6_1", EdiItemResponse.YES)
        vm.updateEdiItemClinicalNote("G6_MG_6_1", "Primera sesión G6")
        vm.selectEdiGroupFromBank(7)
        vm.resolveEdiPendingGroupChange(
            EdiGroupChangeResolutionAction.ARCHIVE_PREVIOUS_AND_SWITCH_TO_CLEAN_TARGET
        )

        // Evaluation 2 of G6 -> ARCH_002_G6
        vm.selectEdiGroupFromBank(6)
        vm.recordEdiCurrentItemResponse("G6_MG_6_1", EdiItemResponse.NO)
        vm.updateEdiItemClinicalNote("G6_MG_6_1", "Segunda sesión G6")
        vm.selectEdiGroupFromBank(7)
        vm.resolveEdiPendingGroupChange(
            EdiGroupChangeResolutionAction.ARCHIVE_PREVIOUS_AND_SWITCH_TO_CLEAN_TARGET
        )

        // Evaluation 3 of G6 -> ARCH_003_G6
        vm.selectEdiGroupFromBank(6)
        vm.recordEdiCurrentItemResponse("G6_MF_6_1", EdiItemResponse.YES)
        vm.updateEdiItemClinicalNote("G6_MF_6_1", "Tercera sesión G6")
        vm.selectEdiGroupFromBank(7)
        vm.resolveEdiPendingGroupChange(
            EdiGroupChangeResolutionAction.ARCHIVE_PREVIOUS_AND_SWITCH_TO_CLEAN_TARGET
        )

        assertEquals(3, vm.uiState.value.ediEngineState.getArchivedEntriesForGroup(6).size)

        // Specifically restore the FIRST evaluation (ARCH_001_G6) even though 2 newer G6 entries exist
        vm.requestRestoreEdiArchivedSession("ARCH_001_G6")
        vm.confirmRestoreEdiArchivedSession(confirmSamePatientContinuity = true)

        val active = vm.uiState.value.ediEngineState.activeSession
        assertEquals("ARCH_001_G6", vm.uiState.value.ediEngineState.restoredFromArchiveId)
        assertEquals(EdiItemResponse.YES, active.currentGroupResponses["G6_MG_6_1"])
        assertEquals(EdiItemResponse.UNANSWERED, active.currentGroupResponses["G6_MF_6_1"])
        assertEquals("Primera sesión G6", active.itemClinicalNotes["G6_MG_6_1"])
    }

    @Test
    fun `14 restore session with unverifiable original context marks it NO VERIFICADO and blocks automatic definitive semaphore`() {
        val vm = CredEdiViewModel()
        // Birth date is blank -> unverifiable age (manual consultation mode)
        vm.selectEdiGroupFromBank(10)
        fillAllMandatoryForGroupAsGreen(vm, 10)
        assertFalse(vm.uiState.value.isChronologicalAgeVerifiable)

        // Switch to G11 -> archives G10 with UNVERIFIED origin context
        vm.selectEdiGroupFromBank(11)
        vm.resolveEdiPendingGroupChange(
            EdiGroupChangeResolutionAction.ARCHIVE_PREVIOUS_AND_SWITCH_TO_CLEAN_TARGET
        )

        val archivedEntry = vm.uiState.value.ediEngineState.archivedSessionHistory.first()
        assertEquals(
            EdiArchivedOriginValidationState.UNVERIFIED,
            archivedEntry.originContext.originValidationState
        )
        assertEquals("NO VERIFICADO", archivedEntry.originContext.originValidationState.displayLabel)

        // Even if the clinician now enters valid dates for G10 (e.g. 22 months old) in the same episode,
        // the archived session's origin context was NO VERIFICADO and must not automatically emit a definitive semaphore
        vm.updateFechaEvaluacion("20/05/2025")
        vm.updateFechaNacimiento("10/07/2023") // 22m 10d -> G10
        vm.confirmEdiTermWithoutWeeks()

        val compatibility = vm.uiState.value.evaluateArchivedEntryCompatibility(archivedEntry)
        assertEquals(
            EdiArchivedCompatibilityStatus.INCOMPATIBLE_ORIGIN_UNVERIFIED,
            compatibility.status
        )
        assertFalse(compatibility.isFullyCompatibleForDefinitiveEvaluation)

        vm.requestRestoreEdiArchivedSession("ARCH_001_G10")
        assertTrue(
            vm.uiState.value.ediEngineState.pendingRestoreRequest!!.warnings.any {
                it.contains("NO VERIFICADO")
            }
        )
        vm.confirmRestoreEdiArchivedSession(confirmSamePatientContinuity = true)

        assertTrue(vm.uiState.value.isRestoredSessionOriginUnverified)
        assertEquals(EdiGlobalTrafficLight.INCOMPLETE, vm.uiState.value.ediGlobalEvaluation.trafficLight)
        assertFalse(vm.uiState.value.ediGlobalEvaluation.isDefinitive)
    }

    @Test
    fun `15 restore session after changing evaluation date or gestational condition shows incompatibility warning and does not retroactively alter origin context`() {
        val vm = CredEdiViewModel()
        vm.updateFechaEvaluacion("20/05/2025")
        vm.updateFechaNacimiento("10/08/2024") // 9m 10d cronológica -> A término = G6
        vm.confirmEdiTermWithoutWeeks()

        vm.recordEdiCurrentItemResponse("G6_MG_6_1", EdiItemResponse.YES)

        // Archive G6 while term (9m 10d, G6)
        vm.selectEdiGroupFromBank(5)
        vm.resolveEdiPendingGroupChange(
            EdiGroupChangeResolutionAction.ARCHIVE_PREVIOUS_AND_SWITCH_TO_CLEAN_TARGET
        )

        val archivedG6 = vm.uiState.value.ediEngineState.archivedSessionHistory.first()
        assertFalse(archivedG6.originContext.esPrematuro)
        assertEquals(6, archivedG6.originContext.applicableGroupNumberAtCapture)

        // Now change gestational condition to preterm 32 weeks and evaluation date to 15/03/2025 -> G5
        vm.togglePrematurez(true)
        vm.updateEgSemanas(32)
        vm.updateEgDias(0)
        vm.updateFechaEvaluacion("15/03/2025") // Crono 7m 5d, Corregida ~5m 10d -> G5

        // Verify original context of ARCH_001_G6 was NOT retroactively modified
        val archivedAfterChange = vm.uiState.value.ediEngineState.archivedSessionHistory.first()
        assertFalse(archivedAfterChange.originContext.esPrematuro)
        assertEquals("20/05/2025", archivedAfterChange.originContext.fechaEvaluacion)
        assertEquals(6, archivedAfterChange.originContext.applicableGroupNumberAtCapture)

        // Request restore of ARCH_001_G6 -> must show incompatibility warning
        vm.requestRestoreEdiArchivedSession("ARCH_001_G6")
        val pendingRestore = vm.uiState.value.ediEngineState.pendingRestoreRequest
        assertNotNull(pendingRestore)
        assertEquals(
            EdiArchivedCompatibilityStatus.INCOMPATIBLE_GROUP_OR_GESTATIONAL_STATE,
            pendingRestore!!.compatibility.status
        )
        assertTrue(pendingRestore.warnings.isNotEmpty())
    }

    @Test
    fun `16 block definitive semaphore when restored or manual group does not match current official age group`() {
        val vm = CredEdiViewModel()
        vm.updateFechaEvaluacion("20/05/2025")
        vm.updateFechaNacimiento("10/08/2024") // 9m 10d -> Grupo 6
        vm.confirmEdiTermWithoutWeeks()

        // Complete all mandatory items of G6 as VERDE
        fillAllMandatoryForGroupAsGreen(vm, 6)
        assertEquals(EdiGlobalTrafficLight.VERDE, vm.uiState.value.ediGlobalEvaluation.trafficLight)
        assertTrue(vm.uiState.value.ediGlobalEvaluation.isDefinitive)

        // Archive G6 and switch to G7
        vm.selectEdiGroupFromBank(7)
        vm.resolveEdiPendingGroupChange(
            EdiGroupChangeResolutionAction.ARCHIVE_PREVIOUS_AND_SWITCH_TO_CLEAN_TARGET
        )

        // In G7 (which does NOT match the 9m 10d age -> official G6), even if all G7 items are answered:
        fillAllMandatoryForGroupAsGreen(vm, 7)
        assertTrue(vm.uiState.value.ediHasGroupAgeMismatch)
        assertEquals(EdiGlobalTrafficLight.INCOMPLETE, vm.uiState.value.ediGlobalEvaluation.trafficLight)
        assertFalse(vm.uiState.value.ediGlobalEvaluation.isDefinitive)

        // Now change evaluation date so child is 11m 10d (official G7), and restore ARCH_001_G6 (G6 != G7)
        vm.updateFechaEvaluacion("20/07/2025")
        assertEquals(7, vm.uiState.value.ediOfficialApplicableGroupNumber)

        vm.requestRestoreEdiArchivedSession("ARCH_001_G6")
        vm.confirmRestoreEdiArchivedSession(confirmSamePatientContinuity = true)

        assertEquals(6, vm.uiState.value.ediEffectiveGroupNumber)
        assertTrue(vm.uiState.value.ediHasGroupAgeMismatch)
        assertEquals(EdiGlobalTrafficLight.INCOMPLETE, vm.uiState.value.ediGlobalEvaluation.trafficLight)
        assertFalse(vm.uiState.value.ediGlobalEvaluation.isDefinitive)
    }

    @Test
    fun `17 allow definitive semaphore when restored session is compatible and complete with explicit continuity confirmation`() {
        val vm = CredEdiViewModel()
        vm.updateFechaEvaluacion("20/05/2025")
        vm.updateFechaNacimiento("10/08/2024") // 9m 10d -> Grupo 6
        vm.confirmEdiTermWithoutWeeks()

        fillAllMandatoryForGroupAsGreen(vm, 6)
        assertEquals(EdiGlobalTrafficLight.VERDE, vm.uiState.value.ediGlobalEvaluation.trafficLight)

        // Archive G6 by consulting G7
        vm.selectEdiGroupFromBank(7)
        vm.resolveEdiPendingGroupChange(
            EdiGroupChangeResolutionAction.ARCHIVE_PREVIOUS_AND_SWITCH_TO_CLEAN_TARGET
        )
        assertEquals(7, vm.uiState.value.ediEffectiveGroupNumber)

        // Restore ARCH_001_G6 within the same compatible context after explicit continuity confirmation
        vm.requestRestoreEdiArchivedSession("ARCH_001_G6")
        val pending = vm.uiState.value.ediEngineState.pendingRestoreRequest!!
        assertEquals(EdiArchivedCompatibilityStatus.COMPATIBLE, pending.compatibility.status)
        assertTrue(pending.compatibility.isFullyCompatibleForDefinitiveEvaluation)
        assertFalse(pending.samePatientAndEpisodeConfirmed)

        vm.setRestoreSamePatientContinuityConfirmed(true)
        vm.confirmRestoreEdiArchivedSession()

        assertEquals(6, vm.uiState.value.ediEffectiveGroupNumber)
        assertFalse(vm.uiState.value.ediHasGroupAgeMismatch)
        assertFalse(vm.uiState.value.isRestoredSessionFromDifferentPatientOrEpisode)
        assertFalse(vm.uiState.value.isRestoredSessionOriginUnverified)
        assertEquals(EdiGlobalTrafficLight.VERDE, vm.uiState.value.ediGlobalEvaluation.trafficLight)
        assertTrue(vm.uiState.value.ediGlobalEvaluation.isDefinitive)
    }

    @Test
    fun `18 prevent reusing old responses when changing essential patient identification or starting new patient episode even with same sex and birth date`() {
        val vm = CredEdiViewModel()
        vm.selectSexo(SexoBiologico.NINO)
        vm.updateFechaEvaluacion("20/05/2025")
        vm.updateFechaNacimiento("10/08/2024") // G6
        vm.confirmEdiTermWithoutWeeks()

        fillAllMandatoryForGroupAsGreen(vm, 6)
        assertEquals("EP_1", vm.uiState.value.ediEngineState.currentWorkEpisodeId)

        // 1) Start a new patient evaluation episode (e.g., twin or second child born on the exact same date 10/08/2024 and same sex NINO)
        vm.startNewEdiEvaluationEpisode("Evaluación de segundo paciente (gemelo / homónimo de fecha)")
        val stateAfterNewEpisode = vm.uiState.value
        assertEquals("EP_2", stateAfterNewEpisode.ediEngineState.currentWorkEpisodeId)
        assertFalse(stateAfterNewEpisode.ediEngineState.activeSession.hasRecordedClinicalResponses)
        assertEquals(1, stateAfterNewEpisode.ediEngineState.archivedSessionHistory.size)
        assertTrue(stateAfterNewEpisode.ediEngineState.currentEpisodeArchivedHistory.isEmpty())

        val archivedEp1 = stateAfterNewEpisode.ediEngineState.archivedSessionHistory.first()
        assertEquals("EP_1", archivedEp1.originContext.workEpisodeId)
        // Even though sexo (Niño) and fechaNacimiento (10/08/2024) are identical, compatibility detects different episode!
        val compTwin = stateAfterNewEpisode.evaluateArchivedEntryCompatibility(archivedEp1)
        assertEquals(
            EdiArchivedCompatibilityStatus.INCOMPATIBLE_DIFFERENT_PATIENT_OR_EPISODE,
            compTwin.status
        )
        assertFalse(compTwin.isFullyCompatibleForDefinitiveEvaluation)

        // R3: Attempting to restore EP_1 into EP_2 is strictly REJECTED and does not copy answers to activeSession
        vm.requestRestoreEdiArchivedSession(archivedEp1.archiveId)
        assertNull(vm.uiState.value.ediEngineState.pendingRestoreRequest)
        vm.confirmRestoreEdiArchivedSession(confirmSamePatientContinuity = true)
        assertNull(vm.uiState.value.ediEngineState.restoredFromArchiveId)
        assertFalse(vm.uiState.value.ediEngineState.activeSession.hasRecordedClinicalResponses)
        assertEquals(EdiGlobalTrafficLight.INCOMPLETE, vm.uiState.value.ediGlobalEvaluation.trafficLight)
        assertFalse(vm.uiState.value.ediGlobalEvaluation.isDefinitive)

        // 2) Start clean in EP_3, record answers for patient born 10/08/2024 (G6), then change birth date to 20/08/2024 (still G6!)
        vm.startNewEdiEvaluationEpisode()
        assertEquals("EP_3", vm.uiState.value.ediEngineState.currentWorkEpisodeId)
        vm.recordEdiCurrentItemResponse("G6_MG_6_1", EdiItemResponse.YES)
        assertTrue(vm.uiState.value.ediEngineState.activeSession.hasRecordedClinicalResponses)

        vm.updateFechaNacimiento("20/08/2024") // 9m 0d -> also G6!
        // Must NOT reuse old G6 answers for the new birth date, and must have archived the 10/08/2024 session
        assertEquals("EP_4", vm.uiState.value.ediEngineState.currentWorkEpisodeId)
        assertFalse(vm.uiState.value.ediEngineState.activeSession.hasRecordedClinicalResponses)
        assertTrue(
            vm.uiState.value.ediEngineState.archivedSessionHistory.any {
                it.originContext.workEpisodeId == "EP_3" &&
                    it.originContext.fechaNacimiento == "10/08/2024"
            }
        )
    }

    @Test
    fun `19 keep priority critical alerts ALA, EN and ROJO area visible when restored session is blocked as INCOMPLETE`() {
        val vm = CredEdiViewModel()
        vm.updateFechaEvaluacion("20/05/2025")
        vm.updateFechaNacimiento("10/08/2024") // G6
        vm.confirmEdiTermWithoutWeeks()

        // Record critical findings in G6: ALA positive, EN positive, and MG in ROJO (0 YES in G6 and G5 fallback)
        vm.recordEdiCurrentItemResponse("G6_ALA_6_1", EdiItemResponse.YES)
        vm.recordEdiCurrentItemResponse("EN_1_1", EdiItemResponse.YES)
        vm.recordEdiCurrentItemResponse("G6_MG_6_1", EdiItemResponse.NO)
        vm.recordEdiCurrentItemResponse("G6_MG_6_2", EdiItemResponse.NO)
        vm.recordEdiCurrentItemResponse("G6_MG_6_3", EdiItemResponse.NO)
        vm.recordEdiFallbackResponse("G6_RETRO_G5_MG_1", EdiItemResponse.NO)
        vm.recordEdiFallbackResponse("G6_RETRO_G5_MG_2", EdiItemResponse.NO)
        vm.recordEdiFallbackResponse("G6_RETRO_G5_MG_3", EdiItemResponse.NO)

        // Archive G6 and change evaluation date so official group is G7 (incompatible with G6)
        vm.selectEdiGroupFromBank(7)
        vm.resolveEdiPendingGroupChange(
            EdiGroupChangeResolutionAction.ARCHIVE_PREVIOUS_AND_SWITCH_TO_CLEAN_TARGET
        )
        vm.updateFechaEvaluacion("20/07/2025") // 11m 10d -> G7

        // Restore ARCH_001_G6 within same episode after confirming continuity
        vm.requestRestoreEdiArchivedSession("ARCH_001_G6")
        vm.confirmRestoreEdiArchivedSession(confirmSamePatientContinuity = true)

        val eval = vm.uiState.value.ediGlobalEvaluation
        assertEquals(EdiGlobalTrafficLight.INCOMPLETE, eval.trafficLight)
        assertFalse(eval.isDefinitive)
        assertTrue(eval.hasPriorityCriticalWarning)
        assertTrue(eval.priorityCriticalAlerts.size >= 3)
        assertEquals(1, eval.positiveAlaItems.size)
        assertEquals(1, eval.positiveEnItems.size)
        assertEquals(
            com.example.model.EdiAreaColorResult.ROJO,
            eval.axis3AreasSummary.areaResults[EdiDevArea.MG]?.status
        )
    }

    @Test
    fun `20 block definitive official PDF export when restored session is incompatible or incomplete while allowing draft PDF`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        File(context.cacheDir, "medio02_pdf_test").apply {
            deleteRecursively()
            mkdirs()
        }

        val vm = CredEdiViewModel()
        vm.updateFechaEvaluacion("20/05/2025")
        vm.updateFechaNacimiento("10/08/2024") // G6
        vm.confirmEdiTermWithoutWeeks()
        fillAllMandatoryForGroupAsGreen(vm, 6)

        vm.selectEdiGroupFromBank(7)
        vm.resolveEdiPendingGroupChange(
            EdiGroupChangeResolutionAction.ARCHIVE_PREVIOUS_AND_SWITCH_TO_CLEAN_TARGET
        )

        // Change eval date so official group is G7, then restore complete G6 session (incompatible with G7)
        vm.updateFechaEvaluacion("20/07/2025")
        vm.requestRestoreEdiArchivedSession("ARCH_001_G6")
        vm.confirmRestoreEdiArchivedSession(confirmSamePatientContinuity = true)

        assertTrue(vm.uiState.value.ediHasGroupAgeMismatch)
        assertEquals(EdiGlobalTrafficLight.INCOMPLETE, vm.uiState.value.ediGlobalEvaluation.trafficLight)
        assertFalse(vm.uiState.value.ediGlobalEvaluation.isDefinitive)

        val exportRes = EdiPdfExporter.exportarFichaOficialEdiPdf(
            context = context,
            state = vm.uiState.value
        )
        assertTrue(exportRes.message, exportRes.success)
        assertTrue(exportRes.isDraft)
        assertTrue(exportRes.fileName!!.startsWith("Borrador_EDI_Incompleta_"))
        assertFalse(exportRes.fileName!!.startsWith("Ficha_Oficial_EDI_2026_"))
    }

    @Test
    fun `21 verify in-memory session behavior and UI display of temporary storage notice, preview and restore dialogs with deliberate confirmation`() {
        val vm = CredEdiViewModel()
        vm.updateFechaEvaluacion("20/05/2025")
        vm.updateFechaNacimiento("10/08/2024") // G6
        vm.confirmEdiTermWithoutWeeks()
        vm.selectTab(AppTab.EDI)

        vm.recordEdiCurrentItemResponse("G6_MG_6_1", EdiItemResponse.YES)
        vm.updateEdiItemClinicalNote("G6_MG_6_1", "Nota visible en vista previa")
        vm.selectEdiGroupFromBank(7)
        vm.resolveEdiPendingGroupChange(
            EdiGroupChangeResolutionAction.ARCHIVE_PREVIOUS_AND_SWITCH_TO_CLEAN_TARGET
        )

        composeRule.setContent {
            CredEdiApp(viewModel = vm)
        }

        // Verify save panel and mandatory ephemeral notice in Window 4 Summary (MEDIO-07: header title removed)
        composeRule.onNodeWithTag("edi_archived_sessions_section")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("Guardado e Historial de Evaluaciones")
            .assertDoesNotExist()
        composeRule.onNodeWithText(
            "Las evaluaciones se guardan localmente en este dispositivo. Puedes recuperarlas desde el historial. Si se eliminan los datos de la aplicación o se restablece el emulador, los registros locales podrían perderse"
        ).assertIsDisplayed()

        // Verify archived entry card and open preview dialog
        composeRule.onNodeWithTag("edi_archived_entry_ARCH_001_G6")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag("edi_archived_preview_btn_ARCH_001_G6")
            .performScrollTo()
            .performClick()

        composeRule.onNodeWithTag("edi_archived_preview_dialog").assertIsDisplayed()
        composeRule.onNodeWithText("Nota visible en vista previa", substring = true).assertIsDisplayed()

        // Request restoration from preview dialog -> opens confirmation dialog
        composeRule.onNodeWithTag("edi_archived_preview_restore_btn").performClick()
        composeRule.onNodeWithTag("edi_restore_confirm_dialog").assertIsDisplayed()

        // R2: Confirm button must be disabled until deliberate confirmation is checked
        composeRule.onNodeWithTag("edi_restore_confirm_btn").assertIsNotEnabled()
        composeRule.onNodeWithText(
            "Confirmo que esta evaluación corresponde al mismo paciente y episodio de atención"
        ).assertIsDisplayed()
        composeRule.onNodeWithTag("edi_restore_other_patient_new_episode_btn")
            .assertIsDisplayed()

        // Check deliberate confirmation and confirm restoration
        composeRule.onNodeWithTag("edi_restore_confirm_same_patient_checkbox").performClick()
        composeRule.onNodeWithTag("edi_restore_confirm_btn").assertIsEnabled()
        composeRule.onNodeWithTag("edi_restore_confirm_btn").performClick()

        assertEquals(6, vm.uiState.value.ediEffectiveGroupNumber)
        assertEquals("ARCH_001_G6", vm.uiState.value.ediEngineState.restoredFromArchiveId)
        assertEquals(
            EdiItemResponse.YES,
            vm.uiState.value.ediEngineState.activeSession.currentGroupResponses["G6_MG_6_1"]
        )
    }

    // =========================================================================
    // PRUEBAS OBLIGATORIAS ADICIONALES DE SEGURIDAD R1, R2 Y R3 (SECCIÓN 6)
    // =========================================================================

    @Test
    fun `22 R1 test 1 - patient A archives G5 leaving active session empty, then changing birth date starts new episode EP_2`() {
        val vm = CredEdiViewModel()
        vm.updateFechaEvaluacion("20/05/2025")
        vm.updateFechaNacimiento("10/11/2024") // 6m 10d -> Grupo 5
        vm.confirmEdiTermWithoutWeeks()
        assertEquals(5, vm.uiState.value.ediEffectiveGroupNumber)
        assertEquals("EP_1", vm.uiState.value.ediEngineState.currentWorkEpisodeId)

        // Patient A records answers in G5 and archives G5 when switching to G6, leaving activeSession empty
        vm.recordEdiCurrentItemResponse("G5_MG_5_1", EdiItemResponse.YES)
        vm.selectEdiGroupFromBank(6)
        vm.resolveEdiPendingGroupChange(
            EdiGroupChangeResolutionAction.ARCHIVE_PREVIOUS_AND_SWITCH_TO_CLEAN_TARGET
        )

        val stateWithEmptyActive = vm.uiState.value
        assertFalse(stateWithEmptyActive.ediEngineState.activeSession.hasRecordedClinicalResponses)
        assertTrue(stateWithEmptyActive.ediEngineState.hasAnyDataInCurrentEpisode)
        assertEquals("EP_1", stateWithEmptyActive.ediEngineState.currentWorkEpisodeId)
        assertEquals(1, stateWithEmptyActive.ediEngineState.archivedSessionHistory.size)

        // Now change birth date to begin attending Patient B (even though activeSession is empty!)
        vm.updateFechaNacimiento("10/08/2024") // 9m 10d -> G6

        val statePatientB = vm.uiState.value
        assertEquals("EP_2", statePatientB.ediEngineState.currentWorkEpisodeId)
        assertTrue(
            statePatientB.ediEngineState.lastRestoreStatusMessage.orEmpty()
                .contains("Nuevo episodio temporal iniciado (EP_2)")
        )
        // Patient A's archived G5 remains intact in EP_1 and is not in EP_2's restorable list
        val archivedG5 = statePatientB.ediEngineState.archivedSessionHistory.first()
        assertEquals("EP_1", archivedG5.originContext.workEpisodeId)
        assertEquals("10/11/2024", archivedG5.originContext.fechaNacimiento)
        assertTrue(statePatientB.ediEngineState.currentEpisodeArchivedHistory.isEmpty())
    }

    @Test
    fun `23 R1 test 2 - patient A archives G5 leaving active session empty, then changing sex starts new episode EP_2`() {
        val vm = CredEdiViewModel()
        vm.selectSexo(SexoBiologico.NINO)
        vm.updateFechaEvaluacion("20/05/2025")
        vm.updateFechaNacimiento("10/11/2024") // Grupo 5
        vm.confirmEdiTermWithoutWeeks()

        vm.recordEdiCurrentItemResponse("G5_MG_5_1", EdiItemResponse.YES)
        vm.selectEdiGroupFromBank(6)
        vm.resolveEdiPendingGroupChange(
            EdiGroupChangeResolutionAction.ARCHIVE_PREVIOUS_AND_SWITCH_TO_CLEAN_TARGET
        )

        assertFalse(vm.uiState.value.ediEngineState.activeSession.hasRecordedClinicalResponses)
        assertTrue(vm.uiState.value.ediEngineState.hasAnyDataInCurrentEpisode)
        assertEquals("EP_1", vm.uiState.value.ediEngineState.currentWorkEpisodeId)

        // Change sex for Patient B while activeSession is empty
        vm.selectSexo(SexoBiologico.NINA)

        val statePatientB = vm.uiState.value
        assertEquals("EP_2", statePatientB.ediEngineState.currentWorkEpisodeId)
        assertTrue(
            statePatientB.ediEngineState.lastRestoreStatusMessage.orEmpty()
                .contains("Nuevo episodio temporal iniciado (EP_2)")
        )
        val archivedG5 = statePatientB.ediEngineState.archivedSessionHistory.first()
        assertEquals("EP_1", archivedG5.originContext.workEpisodeId)
        assertEquals("Niño", archivedG5.originContext.sexoLabel)
        assertTrue(statePatientB.ediEngineState.currentEpisodeArchivedHistory.isEmpty())
    }

    @Test
    fun `24 R2 test 3 and 4 - patients A and B with identical sex and birth date require deliberate continuity confirmation or new episode`() {
        val vm = CredEdiViewModel()
        vm.selectSexo(SexoBiologico.NINO)
        vm.updateFechaEvaluacion("20/05/2025")
        vm.updateFechaNacimiento("10/11/2024") // Grupo 5
        vm.confirmEdiTermWithoutWeeks()

        vm.recordEdiCurrentItemResponse("G5_MG_5_1", EdiItemResponse.YES)
        vm.selectEdiGroupFromBank(6)
        vm.resolveEdiPendingGroupChange(
            EdiGroupChangeResolutionAction.ARCHIVE_PREVIOUS_AND_SWITCH_TO_CLEAN_TARGET
        )

        // Request restore of ARCH_001_G5 in EP_1
        vm.requestRestoreEdiArchivedSession("ARCH_001_G5")
        assertNotNull(vm.uiState.value.ediEngineState.pendingRestoreRequest)
        assertFalse(vm.uiState.value.ediEngineState.pendingRestoreRequest!!.samePatientAndEpisodeConfirmed)

        // Attempting to confirm restore WITHOUT confirming same-patient continuity is REJECTED
        vm.confirmRestoreEdiArchivedSession()
        assertEquals(6, vm.uiState.value.ediEffectiveGroupNumber)
        assertNull(vm.uiState.value.ediEngineState.restoredFromArchiveId)
        assertFalse(vm.uiState.value.ediEngineState.activeSession.hasRecordedClinicalResponses)
        assertTrue(
            vm.uiState.value.ediEngineState.lastRestoreStatusMessage.orEmpty()
                .contains("Confirmo que esta evaluación corresponde al mismo paciente y episodio de atención")
        )

        // Choosing "Es otro paciente — iniciar nuevo episodio" starts EP_2 even with identical sex and birth date
        vm.cancelRestoreEdiArchivedSession()
        vm.startNewEdiEvaluationEpisode("Es otro paciente — iniciar nuevo episodio")
        assertEquals("EP_2", vm.uiState.value.ediEngineState.currentWorkEpisodeId)
        assertTrue(vm.uiState.value.ediEngineState.currentEpisodeArchivedHistory.isEmpty())

        // Attempting to restore ARCH_001_G5 in EP_2 is rejected even if sex and birth date are identical
        vm.requestRestoreEdiArchivedSession("ARCH_001_G5")
        assertNull(vm.uiState.value.ediEngineState.pendingRestoreRequest)
    }

    @Test
    fun `25 R3 test 6 7 8 9 - direct cross-episode restoration from ViewModel and SessionManager is rejected and keeps activeSession intact`() {
        val vm = CredEdiViewModel()
        vm.updateFechaEvaluacion("20/05/2025")
        vm.updateFechaNacimiento("10/08/2024") // G6
        vm.confirmEdiTermWithoutWeeks()

        // Record & archive ARCH_001_G6 in EP_1
        vm.recordEdiCurrentItemResponse("G6_MG_6_1", EdiItemResponse.YES)
        vm.updateEdiItemClinicalNote("G6_MG_6_1", "Nota paciente A en EP_1")
        vm.selectEdiGroupFromBank(7)
        vm.resolveEdiPendingGroupChange(
            EdiGroupChangeResolutionAction.ARCHIVE_PREVIOUS_AND_SWITCH_TO_CLEAN_TARGET
        )

        // Start EP_2 for Patient B and record active data in EP_2
        vm.startNewEdiEvaluationEpisode()
        assertEquals("EP_2", vm.uiState.value.ediEngineState.currentWorkEpisodeId)
        vm.recordEdiCurrentItemResponse("G6_MG_6_1", EdiItemResponse.NO)
        vm.recordEdiCurrentItemResponse("G6_LE_6_1", EdiItemResponse.YES)
        vm.updateEdiItemClinicalNote("G6_LE_6_1", "Nota activa paciente B en EP_2")

        val activeSnapshotEp2Before = vm.uiState.value.ediEngineState.activeSession
        val archivedEp1Before = vm.uiState.value.ediEngineState.archivedSessionHistory.first()

        // Test 7: Direct attempt via ViewModel to restore ARCH_001_G6 (EP_1) while in EP_2
        vm.requestRestoreEdiArchivedSession("ARCH_001_G6")
        assertNull(vm.uiState.value.ediEngineState.pendingRestoreRequest)
        vm.setRestoreSamePatientContinuityConfirmed(true)
        vm.confirmRestoreEdiArchivedSession(confirmSamePatientContinuity = true)

        // Test 9: Active session of EP_2 remains 100% intact and uncontaminated
        assertEquals(activeSnapshotEp2Before, vm.uiState.value.ediEngineState.activeSession)
        assertNull(vm.uiState.value.ediEngineState.restoredFromArchiveId)
        assertEquals(archivedEp1Before, vm.uiState.value.ediEngineState.archivedSessionHistory.first())

        // Test 8: Direct attempt via EdiPhase1SessionManager bypassing ViewModel (even with forged pendingRestoreRequest)
        val currentEngine = vm.uiState.value.ediEngineState
        val comp = vm.uiState.value.evaluateArchivedEntryCompatibility(archivedEp1Before)
        val afterRequest = EdiPhase1SessionManager.requestRestoreArchivedSession(
            state = currentEngine,
            archiveId = "ARCH_001_G6",
            compatibility = comp
        )
        assertNull(afterRequest.pendingRestoreRequest)
        assertEquals(activeSnapshotEp2Before, afterRequest.activeSession)

        val forgedEngineWithPending = currentEngine.copy(
            pendingRestoreRequest = EdiPendingRestoreRequest(
                targetArchiveId = archivedEp1Before.archiveId,
                targetEntry = archivedEp1Before,
                activeSessionWillBeArchivedFirst = true,
                compatibility = comp,
                warnings = emptyList(),
                samePatientAndEpisodeConfirmed = true
            )
        )
        val afterForgedConfirm = EdiPhase1SessionManager.confirmRestoreArchivedSession(
            state = forgedEngineWithPending,
            samePatientContinuityConfirmed = true
        )
        assertNull(afterForgedConfirm.pendingRestoreRequest)
        assertNull(afterForgedConfirm.restoredFromArchiveId)
        assertEquals(activeSnapshotEp2Before, afterForgedConfirm.activeSession)
        assertEquals(archivedEp1Before, afterForgedConfirm.archivedSessionHistory.first())
    }

    @Test
    fun `26 R3 test 10 - EP_1 archived entries are hidden and not shown as restorable in UI during EP_2`() {
        val vm = CredEdiViewModel()
        vm.updateFechaEvaluacion("20/05/2025")
        vm.updateFechaNacimiento("10/08/2024") // G6
        vm.confirmEdiTermWithoutWeeks()
        vm.selectTab(AppTab.EDI)

        vm.recordEdiCurrentItemResponse("G6_MG_6_1", EdiItemResponse.YES)
        vm.selectEdiGroupFromBank(7)
        vm.resolveEdiPendingGroupChange(
            EdiGroupChangeResolutionAction.ARCHIVE_PREVIOUS_AND_SWITCH_TO_CLEAN_TARGET
        )

        composeRule.setContent {
            CredEdiApp(viewModel = vm)
        }

        // In EP_1, ARCH_001_G6 is visible and restorable
        composeRule.onNodeWithTag("edi_archived_entry_ARCH_001_G6")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag("edi_archived_restore_btn_ARCH_001_G6")
            .assertIsEnabled()

        // Start EP_2 (via the continuity card button "Es otro paciente — iniciar nuevo episodio")
        composeRule.onNodeWithTag("edi_summary_other_patient_new_episode_btn")
            .performScrollTo()
            .performClick()
        composeRule.waitForIdle()

        assertEquals("EP_2", vm.uiState.value.ediEngineState.currentWorkEpisodeId)
        // ARCH_001_G6 is preserved in RAM history under EP_1, but NOT shown as restorable in EP_2 UI
        assertEquals(1, vm.uiState.value.ediEngineState.archivedSessionHistory.size)
        assertEquals(0, composeRule.onAllNodesWithTag("edi_archived_entry_ARCH_001_G6").fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithTag("edi_archived_restore_btn_ARCH_001_G6").fetchSemanticsNodes().size)
        composeRule.onNodeWithTag("edi_previous_episodes_protected_notice")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun `27 test 11 and 12 - archive and restore sessions containing ONLY clinical notes or ONLY dual evidence`() {
        val vm = CredEdiViewModel()
        vm.updateFechaEvaluacion("20/05/2025")
        vm.updateFechaNacimiento("10/03/2023") // 26m -> G12
        vm.selectTab(AppTab.EDI)

        // Test 11: Session in G12 containing ONLY a clinical note (0 item responses)
        vm.selectEdiGroupFromBank(12)
        vm.updateEdiItemClinicalNote("G12_MG_12_1", "Nota clínica única sin respuestas marcadas")
        assertEquals(0, vm.uiState.value.ediEngineState.activeSession.answeredCurrentCount)
        assertEquals(1, vm.uiState.value.ediEngineState.activeSession.clinicalNotesCount)
        assertTrue(vm.uiState.value.ediEngineState.activeSession.hasRecordedClinicalResponses)
        assertTrue(vm.uiState.value.ediEngineState.hasAnyDataInCurrentEpisode)

        // Switching to G13 prompts confirmation and archives the note-only session as ARCH_001_G12
        vm.selectEdiGroupFromBank(13)
        assertNotNull(vm.uiState.value.ediEngineState.pendingGroupChangeConfirmation)
        vm.resolveEdiPendingGroupChange(
            EdiGroupChangeResolutionAction.ARCHIVE_PREVIOUS_AND_SWITCH_TO_CLEAN_TARGET
        )
        assertEquals(1, vm.uiState.value.ediEngineState.archivedSessionHistory.size)
        assertEquals(
            "Nota clínica única sin respuestas marcadas",
            vm.uiState.value.ediEngineState.archivedSessionHistory[0].snapshot.itemClinicalNotes["G12_MG_12_1"]
        )

        // Test 12: Session in G13 containing ONLY unresolved dual evidence (obs=true, ent=false, res=null -> DISCREPANCY)
        vm.recordEdiDualEvidence(
            itemUniqueId = "G13_LE_13_3",
            observationEvidence = true,
            interviewEvidence = false,
            humanResolutionIfConflict = null,
            note = "Evidencia dual en conflicto pendiente"
        )
        assertEquals(1, vm.uiState.value.ediEngineState.activeSession.dualEvidenceCount)
        assertTrue(vm.uiState.value.ediEngineState.activeSession.hasRecordedClinicalResponses)

        vm.selectEdiGroupFromBank(14)
        assertNotNull(vm.uiState.value.ediEngineState.pendingGroupChangeConfirmation)
        vm.resolveEdiPendingGroupChange(
            EdiGroupChangeResolutionAction.ARCHIVE_PREVIOUS_AND_SWITCH_TO_CLEAN_TARGET
        )
        assertEquals(2, vm.uiState.value.ediEngineState.archivedSessionHistory.size)

        // Restore ARCH_001_G12 (note-only) and verify
        vm.requestRestoreEdiArchivedSession("ARCH_001_G12")
        vm.confirmRestoreEdiArchivedSession(confirmSamePatientContinuity = true)
        assertEquals(
            "Nota clínica única sin respuestas marcadas",
            vm.uiState.value.ediEngineState.activeSession.itemClinicalNotes["G12_MG_12_1"]
        )

        // Restore ARCH_002_G13 (dual-evidence) and verify
        vm.requestRestoreEdiArchivedSession("ARCH_002_G13")
        vm.confirmRestoreEdiArchivedSession(confirmSamePatientContinuity = true)
        val dualRec = vm.uiState.value.ediEngineState.activeSession.dualEvidenceRecords["G13_LE_13_3"]
        assertNotNull(dualRec)
        assertEquals(true, dualRec?.directObservationEvidence)
        assertEquals(false, dualRec?.directedInterviewEvidence)
        assertNull(dualRec?.professionalHumanResolution)
    }

    @Test
    fun `28 test 13 - correcting birth date of the same patient preserves episode, capture antecedents, and causes zero silent loss`() {
        val vm = CredEdiViewModel()
        vm.updateFechaEvaluacion("20/05/2025")
        vm.updateFechaNacimiento("10/08/2024") // 9m 10d -> G6
        vm.confirmEdiTermWithoutWeeks()

        // Record responses in G6 and archive G6 -> ARCH_001_G6
        vm.recordEdiCurrentItemResponse("G6_MG_6_1", EdiItemResponse.YES)
        vm.selectEdiGroupFromBank(6)
        vm.updateEdiItemClinicalNote("G6_MG_6_1", "Nota del mismo paciente antes de corregir F.N.")

        // 1) Correct birth date of the SAME patient within G6 (10/08/2024 -> 12/08/2024)
        vm.correctSamePatientFechaNacimiento("12/08/2024")
        assertEquals("EP_1", vm.uiState.value.ediEngineState.currentWorkEpisodeId)
        assertEquals(6, vm.uiState.value.ediEffectiveGroupNumber)
        // Active responses and notes are preserved without silent loss
        assertEquals(
            EdiItemResponse.YES,
            vm.uiState.value.ediEngineState.activeSession.currentGroupResponses["G6_MG_6_1"]
        )
        assertEquals(
            "Nota del mismo paciente antes de corregir F.N.",
            vm.uiState.value.ediEngineState.activeSession.itemClinicalNotes["G6_MG_6_1"]
        )
        // Capture antecedent retains original birth date 10/08/2024
        assertEquals(
            "10/08/2024",
            vm.uiState.value.ediEngineState.activeSessionOriginContext?.fechaNacimiento
        )

        // 2) Correct birth date of the SAME patient to a date that changes group (10/11/2024 -> G5)
        vm.correctSamePatientFechaNacimiento("10/11/2024")
        assertEquals("EP_1", vm.uiState.value.ediEngineState.currentWorkEpisodeId)
        // Requests group change confirmation instead of silently losing G6 responses
        val pendingGroupChange = vm.uiState.value.ediEngineState.pendingGroupChangeConfirmation
        assertNotNull(pendingGroupChange)
        assertEquals(6, pendingGroupChange?.previousGroupNumber)
        assertEquals(5, pendingGroupChange?.targetGroupNumber)
        assertEquals(
            EdiItemResponse.YES,
            pendingGroupChange?.preservedSnapshotOfPreviousGroup?.currentGroupResponses?.get("G6_MG_6_1")
        )

        vm.resolveEdiPendingGroupChange(
            EdiGroupChangeResolutionAction.ARCHIVE_PREVIOUS_AND_SWITCH_TO_CLEAN_TARGET
        )
        assertEquals(5, vm.uiState.value.ediEffectiveGroupNumber)
        assertEquals(1, vm.uiState.value.ediEngineState.currentEpisodeArchivedHistory.size)
        val archivedEntry = vm.uiState.value.ediEngineState.currentEpisodeArchivedHistory.first()
        assertEquals("10/08/2024", archivedEntry.originContext.fechaNacimiento)
    }

    @Test
    fun `29 test 18 and 20 - ViewModel restart clears RAM history and official catalog preserves 15 groups and 270 items`() {
        // Test 18: ViewModel restart has clean RAM-only state
        val vm1 = CredEdiViewModel()
        vm1.updateFechaEvaluacion("20/05/2025")
        vm1.updateFechaNacimiento("10/08/2024")
        vm1.confirmEdiTermWithoutWeeks()
        vm1.recordEdiCurrentItemResponse("G6_MG_6_1", EdiItemResponse.YES)
        vm1.selectEdiGroupFromBank(7)
        vm1.resolveEdiPendingGroupChange(
            EdiGroupChangeResolutionAction.ARCHIVE_PREVIOUS_AND_SWITCH_TO_CLEAN_TARGET
        )
        assertEquals(1, vm1.uiState.value.ediEngineState.archivedSessionHistory.size)

        val vm2 = CredEdiViewModel()
        assertTrue(vm2.uiState.value.ediEngineState.archivedSessionHistory.isEmpty())
        assertEquals("EP_1", vm2.uiState.value.ediEngineState.currentWorkEpisodeId)
        assertFalse(vm2.uiState.value.ediEngineState.hasAnyDataInCurrentEpisode)

        // Test 20: Integrity of the 15 official groups and 270 items
        val groups = EdiOfficialCatalog.groups
        assertEquals(15, groups.size)
        val totalAreaItems = groups.sumOf { spec -> spec.areaItems.values.flatten().size }
        val totalAleItems = groups.sumOf { spec -> spec.aleItems.size }
        val totalAlaItems = groups.sumOf { spec -> spec.alaItems.size }
        val totalFrbItems = EdiOfficialCatalog.frbItems.size
        val totalEnItems = EdiOfficialCatalog.allEnItems.size
        val totalSpecialCoRItems = EdiOfficialCatalog.specialGroup13CoRFallbackItems.size
        val totalOfficialCatalogItems =
            totalAreaItems + totalAleItems + totalAlaItems + totalFrbItems + totalEnItems + totalSpecialCoRItems
        assertEquals(161, totalAreaItems)
        assertEquals(62, totalAleItems)
        assertEquals(34, totalAlaItems)
        assertEquals(7, totalFrbItems)
        assertEquals(3, totalEnItems)
        assertEquals(3, totalSpecialCoRItems)
        assertEquals(270, totalOfficialCatalogItems)
    }

    // =========================================================================
    // PRUEBAS OBLIGATORIAS DE INTERFAZ — CORRECCIÓN DE FECHA DEL MISMO PACIENTE
    // =========================================================================

    @Test
    fun `30 UI correct birth date of the same patient preserves workEpisodeId, archived sessions and original context`() {
        val vm = CredEdiViewModel()
        vm.updateFechaEvaluacion("20/05/2025")
        vm.updateFechaNacimiento("10/08/2024") // 9m 10d -> Grupo 6
        vm.confirmEdiTermWithoutWeeks()
        vm.selectTab(AppTab.EDI)

        // Record & archive a session in G6, then return to G6 and record active responses + dual evidence + note
        vm.recordEdiCurrentItemResponse("G6_MG_6_1", EdiItemResponse.YES)
        vm.updateEdiItemClinicalNote("G6_MG_6_1", "Nota archivada en EP_1")
        vm.selectEdiGroupFromBank(7)
        vm.resolveEdiPendingGroupChange(
            EdiGroupChangeResolutionAction.ARCHIVE_PREVIOUS_AND_SWITCH_TO_CLEAN_TARGET
        )
        vm.selectEdiGroupFromBank(6)
        vm.recordEdiCurrentItemResponse("G6_MF_6_1", EdiItemResponse.YES)
        vm.recordEdiCurrentItemResponse("FRB_1", EdiItemResponse.NO)
        vm.recordEdiCurrentItemResponse("EN_1_1", EdiItemResponse.NO)
        vm.updateEdiItemClinicalNote("G6_MF_6_1", "Nota activa en G6")

        composeRule.setContent {
            CredEdiApp(viewModel = vm)
        }

        // 1. Lote 3D: El botón «Corregir fecha de nacimiento del mismo paciente» se retiró de la interfaz EDI,
        // pero las reglas clínicas de corrección de fecha del mismo paciente continúan íntegras.
        composeRule.onNodeWithTag("edi_summary_correct_birth_date_btn").assertDoesNotExist()

        // 2. Ejecutar corrección de fecha de nacimiento del mismo paciente en ViewModel
        vm.correctSamePatientFechaNacimiento("15/08/2024", "Rectificación de día según DNI físico del niño")
        composeRule.waitForIdle()

        // La fecha se actualiza dentro del MISMO episodio EP_1
        val state = vm.uiState.value
        assertEquals("15/08/2024", state.fechaNacimiento)
        assertEquals("EP_1", state.ediEngineState.currentWorkEpisodeId)
        assertEquals(9, state.edadCronologicaMeses)
        assertEquals(5, state.edadCronologicaDias)

        // Archived sessions of EP_1 remain visible and their historical originContext was NOT retroactively modified
        assertEquals(1, state.ediEngineState.currentEpisodeArchivedHistory.size)
        val archivedG6 = state.ediEngineState.currentEpisodeArchivedHistory.first()
        assertEquals("ARCH_001_G6", archivedG6.archiveId)
        assertEquals("EP_1", archivedG6.originContext.workEpisodeId)
        assertEquals("10/08/2024", archivedG6.originContext.fechaNacimiento)
        assertEquals("Nota archivada en EP_1", archivedG6.snapshot.itemClinicalNotes["G6_MG_6_1"])

        // Active session responses and notes remain intact
        assertEquals(EdiItemResponse.YES, state.ediEngineState.activeSession.currentGroupResponses["G6_MF_6_1"])
        assertEquals("Nota activa en G6", state.ediEngineState.activeSession.itemClinicalNotes["G6_MF_6_1"])
        assertEquals("10/08/2024", state.ediEngineState.activeSessionOriginContext?.fechaNacimiento)
    }

    @Test
    fun `31 UI cancel birth date correction dialog without modifying anything`() {
        val vm = CredEdiViewModel()
        vm.updateFechaEvaluacion("20/05/2025")
        vm.updateFechaNacimiento("10/08/2024")
        vm.confirmEdiTermWithoutWeeks()
        vm.selectTab(AppTab.EDI)
        vm.recordEdiCurrentItemResponse("G6_MG_6_1", EdiItemResponse.YES)

        composeRule.setContent {
            CredEdiApp(viewModel = vm)
        }

        // Lote 3D: el botón visible de corrección en EDI fue retirado; al no confirmar cambio alguno nada se modifica
        composeRule.onNodeWithTag("edi_summary_correct_birth_date_btn").assertDoesNotExist()
        assertEquals("10/08/2024", vm.uiState.value.fechaNacimiento)
        assertEquals("EP_1", vm.uiState.value.ediEngineState.currentWorkEpisodeId)
        assertEquals(EdiItemResponse.YES, vm.uiState.value.ediEngineState.activeSession.currentGroupResponses["G6_MG_6_1"])
    }

    @Test
    fun `32 UI and ViewModel reject invalid date, future date after evaluation, and blank correction reason`() {
        val vm = CredEdiViewModel()
        vm.updateFechaEvaluacion("20/05/2025")
        vm.updateFechaNacimiento("10/08/2024")
        vm.confirmEdiTermWithoutWeeks()
        vm.selectTab(AppTab.EDI)
        vm.recordEdiCurrentItemResponse("G6_MG_6_1", EdiItemResponse.YES)

        composeRule.setContent {
            CredEdiApp(viewModel = vm)
        }

        composeRule.onNodeWithTag("edi_summary_correct_birth_date_btn").assertDoesNotExist()

        // 1. Reject blank reason
        vm.correctSamePatientFechaNacimiento("12/08/2024", "   ")
        composeRule.waitForIdle()
        assertEquals("10/08/2024", vm.uiState.value.fechaNacimiento)
        assertEquals("EP_1", vm.uiState.value.ediEngineState.currentWorkEpisodeId)

        // 2. Reject invalid non-existent calendar date (31/02/2024)
        vm.correctSamePatientFechaNacimiento("31/02/2024", "Corrección con fecha inexistente")
        composeRule.waitForIdle()
        assertEquals("10/08/2024", vm.uiState.value.fechaNacimiento)

        // 3. Reject date with invalid month (05/13/2024)
        vm.correctSamePatientFechaNacimiento("05/13/2024", "Mes inválido")
        composeRule.waitForIdle()
        assertEquals("10/08/2024", vm.uiState.value.fechaNacimiento)

        // 4. Reject birth date future with respect to evaluation date (25/05/2025 > 20/05/2025)
        vm.correctSamePatientFechaNacimiento("25/05/2025", "Fecha futura respecto a evaluación")
        composeRule.waitForIdle()
        assertEquals("10/08/2024", vm.uiState.value.fechaNacimiento)
        assertEquals("EP_1", vm.uiState.value.ediEngineState.currentWorkEpisodeId)
    }

    @Test
    fun `33 UI birth date correction that changes group prompts confirmation, invalidates incompatible definitive results, and recalculates chronological and corrected age`() {
        val vm = CredEdiViewModel()
        vm.updateFechaEvaluacion("20/05/2025")
        vm.updateFechaNacimiento("10/08/2024") // 9m 10d cronológica -> A término = G6
        vm.togglePrematurez(true)
        vm.updateEgSemanas(32) // 8 semanas prematuridad (56 días) -> corregida ~7m 14d = G6
        vm.updateEgDias(0)
        vm.selectTab(AppTab.EDI)

        fillAllMandatoryForGroupAsGreen(vm, 6)
        assertEquals(6, vm.uiState.value.ediEffectiveGroupNumber)
        assertEquals(EdiGlobalTrafficLight.VERDE, vm.uiState.value.ediGlobalEvaluation.trafficLight)
        assertTrue(vm.uiState.value.ediGlobalEvaluation.isDefinitive)

        composeRule.setContent {
            CredEdiApp(viewModel = vm)
        }

        // Corregir fecha de nacimiento (conservando episodio y reglas clínicas): 10/10/2024 -> Cronológica 7m 10d; Corregida ~5m 14d -> Grupo 5!
        vm.correctSamePatientFechaNacimiento("10/10/2024", "Corrección de mes según carné CRED")
        composeRule.waitForIdle()

        val stateAfterCorrection = vm.uiState.value
        assertEquals("10/10/2024", stateAfterCorrection.fechaNacimiento)
        assertEquals("EP_1", stateAfterCorrection.ediEngineState.currentWorkEpisodeId)
        assertEquals(7, stateAfterCorrection.edadCronologicaMeses)
        assertEquals(10, stateAfterCorrection.edadCronologicaDias)
        assertTrue(stateAfterCorrection.usaEdadCorregida)
        assertEquals(5, stateAfterCorrection.edadCorregidaMeses)
        assertEquals(5, stateAfterCorrection.ediOfficialApplicableGroupNumber)

        // Because G6 had responses, group change confirmation dialog must appear (G6 -> G5)
        assertNotNull(stateAfterCorrection.ediEngineState.pendingGroupChangeConfirmation)
        composeRule.onNodeWithTag("edi_group_change_dialog").assertIsDisplayed()

        // While still in G6 (or if switch is canceled), G6 is incompatible with official G5 -> definitive VERDE is invalidated!
        assertTrue(stateAfterCorrection.ediHasGroupAgeMismatch)
        assertEquals(EdiGlobalTrafficLight.INCOMPLETE, stateAfterCorrection.ediGlobalEvaluation.trafficLight)
        assertFalse(stateAfterCorrection.ediGlobalEvaluation.isDefinitive)

        // Confirm archiving G6 and switching to clean G5 -> no responses mixed between G6 and G5
        composeRule.onNodeWithTag("edi_confirm_archive_switch").performClick()
        composeRule.waitForIdle()

        val stateInG5 = vm.uiState.value
        assertEquals(5, stateInG5.ediEffectiveGroupNumber)
        assertFalse(stateInG5.ediEngineState.activeSession.hasRecordedClinicalResponses)
        assertEquals(1, stateInG5.ediEngineState.currentEpisodeArchivedHistory.size)
        assertEquals("ARCH_001_G6", stateInG5.ediEngineState.currentEpisodeArchivedHistory.first().archiveId)
    }

    @Test
    fun `34 birth date correction re-evaluates gestational condition in under 24m and never bypasses R1, R2 or R3 cross-episode blocks`() {
        val vm = CredEdiViewModel()
        vm.updateFechaEvaluacion("20/05/2025")
        vm.updateFechaNacimiento("10/01/2022") // 40m -> Grupo 13 (>= 24m, UNSPECIFIED maturity allowed)
        vm.selectTab(AppTab.EDI)

        // Complete G13 as VERDE in EP_1
        fillAllMandatoryForGroupAsGreen(vm, 13)
        assertEquals(EdiGlobalTrafficLight.VERDE, vm.uiState.value.ediGlobalEvaluation.trafficLight)
        assertTrue(vm.uiState.value.ediGlobalEvaluation.isDefinitive)

        // Start EP_2 (which archives EP_1's G13 session under EP_1)
        vm.startNewEdiEvaluationEpisode("Cambio a nuevo paciente en EP_2")
        assertEquals("EP_2", vm.uiState.value.ediEngineState.currentWorkEpisodeId)

        // In EP_2, correct birth date to 10/08/2024 (< 24m, 9m 10d) while maturity is still UNSPECIFIED
        vm.correctSamePatientFechaNacimiento("10/08/2024", "Corrección a menor de 24 meses en EP_2")
        val stateUnder24m = vm.uiState.value
        assertEquals("EP_2", stateUnder24m.ediEngineState.currentWorkEpisodeId)
        assertTrue(stateUnder24m.isBirthMaturityConfirmationPendingInUnder24m)
        assertEquals(EdiGlobalTrafficLight.INCOMPLETE, stateUnder24m.ediGlobalEvaluation.trafficLight)
        assertFalse(stateUnder24m.ediGlobalEvaluation.isDefinitive)

        // R3: Correcting birth date in EP_2 must NEVER expose or allow restoring EP_1's archived session
        assertTrue(stateUnder24m.ediEngineState.currentEpisodeArchivedHistory.isEmpty())
        assertEquals(1, stateUnder24m.ediEngineState.previousEpisodesArchivedCount)
        vm.requestRestoreEdiArchivedSession("ARCH_001_G13")
        assertNull(vm.uiState.value.ediEngineState.pendingRestoreRequest)
        vm.confirmRestoreEdiArchivedSession(confirmSamePatientContinuity = true)
        assertNull(vm.uiState.value.ediEngineState.restoredFromArchiveId)
    }

    @Test
    fun `35 UI verify Nuevo episodio continues working and Windows 1, 2 and 3 have no regressions after birth date correction`() {
        val vm = CredEdiViewModel()
        vm.selectSexo(SexoBiologico.NINA)
        vm.updateFechaEvaluacion("20/05/2025")
        vm.updateFechaNacimiento("10/08/2024") // 9m 10d
        vm.confirmEdiTermWithoutWeeks()
        vm.setPesoKg(8.4)
        vm.setLongitudCm(70.5)
        vm.setPerimetroCefalicoCm(44.2)
        vm.selectTab(AppTab.EDI)

        // Correct birth date of same patient to 20/08/2024 (exact 9m 0d)
        vm.correctSamePatientFechaNacimiento("20/08/2024", "Ajuste de fecha del mismo paciente")
        assertEquals("EP_1", vm.uiState.value.ediEngineState.currentWorkEpisodeId)
        assertEquals(9, vm.uiState.value.edadCronologicaMeses)
        assertEquals(0, vm.uiState.value.edadCronologicaDias)

        composeRule.setContent {
            CredEdiApp(viewModel = vm)
        }

        // Lote 3D: El botón visible «Nuevo episodio» fue sustituido por «Nueva evaluación»,
        // mientras el gestor interno de episodios EDI continúa funcionando sin regresión.
        composeRule.onNodeWithTag("edi_summary_new_evaluation_btn")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag("edi_summary_new_episode_btn").assertDoesNotExist()
        vm.startNewEdiEvaluationEpisode()
        composeRule.waitForIdle()
        assertEquals("EP_2", vm.uiState.value.ediEngineState.currentWorkEpisodeId)

        // Verify Window 1 (Calculadora), Window 2 (Curvas OMS), and Window 3 (Diagnóstico) render without regressions
        vm.selectTab(AppTab.CALCULADORA)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Calculadora Antropométrica").assertIsDisplayed()
        assertEquals("20/08/2024", vm.uiState.value.fechaNacimiento)

        vm.selectTab(AppTab.CURVAS_OMS)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("P/E").assertIsDisplayed()

        vm.selectTab(AppTab.DIAGNOSTICO)
        composeRule.waitForIdle()
        assertTrue(vm.uiState.value.evalPE.zScore.isFinite())
        assertTrue(vm.uiState.value.evalTE.zScore.isFinite())
        assertTrue(vm.uiState.value.evalPT.zScore.isFinite())
    }
}
