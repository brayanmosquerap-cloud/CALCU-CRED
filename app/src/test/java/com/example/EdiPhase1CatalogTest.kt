package com.example

import com.example.model.ClinicalAgeCalculator
import com.example.model.EdiAgeRangeStatus
import com.example.model.EdiAxis
import com.example.model.EdiClinicalModality
import com.example.model.EdiDevArea
import com.example.model.EdiGroupAutoSelectionStatus
import com.example.model.EdiGroupChangeResolutionAction
import com.example.model.EdiItemResponse
import com.example.model.EdiOfficialCatalog
import com.example.model.EdiOfficialPlate
import com.example.model.EdiPhase1SessionManager
import com.example.model.GestationalAgeInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EdiPhase1CatalogTest {

    @Test
    fun `verifica los 15 intervalos de edad oficiales y exclusion fuera de rango`() {
        assertEquals(15, EdiOfficialCatalog.groups.size)

        // Fuera de rango inferior (< 1 mes: 0m 0d a 0m 29d)
        val (status0m0d, group0m0d) = EdiOfficialCatalog.selectGroupForAge(0, 0)
        assertEquals(EdiAgeRangeStatus.UNDER_MIN_AGE_LESS_THAN_1_MONTH, status0m0d)
        assertNull(group0m0d)

        val (status0m29d, group0m29d) = EdiOfficialCatalog.selectGroupForAge(0, 29)
        assertEquals(EdiAgeRangeStatus.UNDER_MIN_AGE_LESS_THAN_1_MONTH, status0m29d)
        assertNull(group0m29d)

        // Límites exactos de los 15 grupos oficiales (págs. 6 y 17-111)
        val expectedBounds = listOf(
            Triple(1, 1, 1),
            Triple(2, 2, 2),
            Triple(3, 3, 3),
            Triple(4, 4, 4),
            Triple(5, 5, 6),
            Triple(6, 7, 9),
            Triple(7, 10, 12),
            Triple(8, 13, 15),
            Triple(9, 16, 18),
            Triple(10, 19, 24),
            Triple(11, 25, 30),
            Triple(12, 31, 36),
            Triple(13, 37, 48),
            Triple(14, 49, 59),
            Triple(15, 60, 71)
        )

        expectedBounds.forEach { (expectedGroupNum, minM, maxM) ->
            val spec = EdiOfficialCatalog.getGroup(expectedGroupNum)
            assertEquals(minM, spec.minMonthsInclusive)
            assertEquals(maxM, spec.maxMonthsInclusive)

            val (statusStart, groupStart) = EdiOfficialCatalog.selectGroupForAge(minM, 0)
            assertEquals(EdiAgeRangeStatus.IN_OFFICIAL_RANGE_G1_TO_G15, statusStart)
            assertEquals(expectedGroupNum, groupStart?.groupNumber)

            val (statusEnd, groupEnd) = EdiOfficialCatalog.selectGroupForAge(maxM, 29)
            assertEquals(EdiAgeRangeStatus.IN_OFFICIAL_RANGE_G1_TO_G15, statusEnd)
            assertEquals(expectedGroupNum, groupEnd?.groupNumber)
        }

        // Fuera de rango superior (>= 72 meses / 6 años)
        val (status72m, group72m) = EdiOfficialCatalog.selectGroupForAge(72, 0)
        assertEquals(EdiAgeRangeStatus.OVER_MAX_AGE_72_MONTHS_OR_MORE, status72m)
        assertNull(group72m)
    }

    @Test
    fun `verifica estructura completa de los 5 ejes y conteo de reactivos por grupo`() {
        // Eje 1: 7 FRB comunes
        assertEquals(7, EdiOfficialCatalog.frbItems.size)
        EdiOfficialCatalog.frbItems.forEachIndexed { idx, item ->
            assertEquals("FRB ${idx + 1}", item.itemCode)
            assertEquals(EdiAxis.FRB, item.axis)
            assertFalse(item.yesIsNormalGreen)
        }

        // Eje 4: 3 EN en G1..G14, y sólo 2 EN en G15 (EN 1.3 no aplica en G15)
        for (g in 1..14) {
            val enForGroup = EdiOfficialCatalog.getApplicableEnItems(g)
            assertEquals("Grupo $g debe tener 3 ítems EN", 3, enForGroup.size)
            assertTrue(EdiOfficialCatalog.getGroup(g).neurologicalEn13Applies)
        }
        val enG15 = EdiOfficialCatalog.getApplicableEnItems(15)
        assertEquals(2, enG15.size)
        assertEquals(listOf("EN 1.1", "EN 1.2"), enG15.map { it.itemCode })
        assertFalse(EdiOfficialCatalog.getGroup(15).neurologicalEn13Applies)

        // Grupos 1 a 7: 2 reactivos en MG, MF, LE, SO; CO no aplica (0)
        for (g in 1..7) {
            val spec = EdiOfficialCatalog.getGroup(g)
            assertEquals(2, spec.itemsPerActiveArea)
            assertEquals(2, spec.areaItems[EdiDevArea.MG]?.size)
            assertEquals(2, spec.areaItems[EdiDevArea.MF]?.size)
            assertEquals(2, spec.areaItems[EdiDevArea.LE]?.size)
            assertEquals(2, spec.areaItems[EdiDevArea.SO]?.size)
            assertTrue(spec.areaItems[EdiDevArea.CO].isNullOrEmpty())
            assertFalse(spec.conocimientoApplies)
        }

        // Grupos 8 a 12: 3 reactivos en MG, MF, LE, SO; CO no aplica (0)
        for (g in 8..12) {
            val spec = EdiOfficialCatalog.getGroup(g)
            assertEquals(3, spec.itemsPerActiveArea)
            assertEquals(3, spec.areaItems[EdiDevArea.MG]?.size)
            assertEquals(3, spec.areaItems[EdiDevArea.MF]?.size)
            assertEquals(3, spec.areaItems[EdiDevArea.LE]?.size)
            assertEquals(3, spec.areaItems[EdiDevArea.SO]?.size)
            assertTrue(spec.areaItems[EdiDevArea.CO].isNullOrEmpty())
            assertFalse(spec.conocimientoApplies)
        }

        // Excepción de retroceso del Grupo 8: retrocede a los 2 reactivos del Grupo 7
        val g8 = EdiOfficialCatalog.getGroup(8)
        assertEquals(7, g8.fallbackPreviousGroupNumber)
        assertEquals(2, g8.fallbackItemsCountPerArea)

        // Grupos 13 a 15: 3 reactivos en las 5 áreas (incluyendo CO)
        for (g in 13..15) {
            val spec = EdiOfficialCatalog.getGroup(g)
            assertEquals(3, spec.itemsPerActiveArea)
            EdiDevArea.entries.forEach { area ->
                assertEquals(3, spec.areaItems[area]?.size)
            }
            assertTrue(spec.conocimientoApplies)
        }

        // Excepción de retroceso del Grupo 13: 3 reactivos especiales CO-R 13.1, 13.2 y 13.3
        val g13 = EdiOfficialCatalog.getGroup(13)
        assertTrue(g13.usesSpecialCoRFallback)
        assertEquals(3, EdiOfficialCatalog.specialGroup13CoRFallbackItems.size)
        assertEquals(
            listOf("CO-R 13.1", "CO-R 13.2", "CO-R 13.3"),
            EdiOfficialCatalog.specialGroup13CoRFallbackItems.map { it.itemCode }
        )

        // Conteo exacto de ALE y ALA por grupo según el Manual 2026
        val expectedAleCounts = mapOf(
            1 to 4, 2 to 4, 3 to 4, 4 to 4, 5 to 6,
            6 to 4, 7 to 4, 8 to 4, 9 to 3, 10 to 3,
            11 to 2, 12 to 5, 13 to 3, 14 to 8, 15 to 4
        )
        val expectedAlaCounts = mapOf(
            1 to 0, 2 to 3, 3 to 2, 4 to 2, 5 to 2,
            6 to 2, 7 to 2, 8 to 1, 9 to 4, 10 to 2,
            11 to 2, 12 to 4, 13 to 3, 14 to 2, 15 to 3
        )
        for (g in 1..15) {
            val spec = EdiOfficialCatalog.getGroup(g)
            assertEquals("Conteo ALE en G$g", expectedAleCounts[g], spec.aleItems.size)
            assertEquals("Conteo ALA en G$g", expectedAlaCounts[g], spec.alaItems.size)
        }
    }

    @Test
    fun `verifica comparacion de edad corregida 6m 24d vs 6m 23d y bloqueo cuando difiere el grupo`() {
        // Caso documentado pág. 11 del Manual: 14/07/2024 al 20/03/2025 con 34 semanas
        val comparison = EdiOfficialCatalog.compareCorrectedAgeMethods(
            fechaNacimiento = "14/07/2024",
            fechaEvaluacion = "20/03/2025",
            esPrematuro = true,
            egSemanas = 34,
            egDias = 0
        )

        // Edad cronológica: 8m 6d en ambos métodos
        assertEquals(8, comparison.manualMethodResult.chronologicalAge.totalMonths)
        assertEquals(6, comparison.manualMethodResult.chronologicalAge.days)

        // Método Manual EDI (préstamos de 30 días): 6m 24d -> Grupo 5
        assertEquals(6, comparison.manualMethodResult.correctedAge.totalMonths)
        assertEquals(24, comparison.manualMethodResult.correctedAge.days)
        assertEquals(5, comparison.manualMethodResult.assignedGroup?.groupNumber)

        // Método Calendario CRED (ClinicalAgeCalculator.calculateAges intacto): 6m 23d -> Grupo 5
        val credDirect = ClinicalAgeCalculator.calculateAges("14/07/2024", "20/03/2025", true, 34, 0)
        assertEquals(6, credDirect.corregidaMeses)
        assertEquals(23, credDirect.corregidaDias)
        assertEquals(6, comparison.calendarCredMethodResult.correctedAge.totalMonths)
        assertEquals(23, comparison.calendarCredMethodResult.correctedAge.days)
        assertEquals(5, comparison.calendarCredMethodResult.assignedGroup?.groupNumber)

        // Ambos seleccionan Grupo 5 -> Asignación automática permitida
        assertTrue(comparison.selectsSameGroup)
        assertEquals(EdiGroupAutoSelectionStatus.AUTO_SELECTED_CONSISTENT, comparison.autoSelectionStatus)
        assertEquals(5, comparison.automaticallyAssignedGroup?.groupNumber)

        // Caso de frontera donde el préstamo de 30 días y el calendario real caen en grupos distintos:
        // Nacimiento: 30/01/2025, Evaluación: 13/06/2025, Prematuro 34 semanas (42 días):
        // - Manual EDI: crono = (13+30)-30 = 13d, m = (6-1)-1 = 4m -> 4m 13d;
        //   menos 42 días (pide 1m=30d -> 3m 43d - 42d) = 3m 01d -> Grupo 3.
        // - Calendario CRED: 30/01/2025 + 42d = 13/03/2025; al 13/06/2025 = 3m 0d -> Grupo 3.
        // Ahora con Evaluación 12/06/2025:
        // - Manual EDI: crono = (12+30)-30 = 12d, m = 4m -> 4m 12d;
        //   menos 42 días (pide 1m=30d -> 3m 42d - 42d) = 3m 0d -> Grupo 3 (3m 0d - 3m 29d).
        // - Calendario CRED: 13/03/2025 al 12/06/2025 = 2m 30d -> Grupo 2 (2m 0d - 2m 29d).
        val boundaryDiscrepancy = EdiOfficialCatalog.compareCorrectedAgeMethods(
            fechaNacimiento = "30/01/2025",
            fechaEvaluacion = "12/06/2025",
            esPrematuro = true,
            egSemanas = 34,
            egDias = 0
        )
        assertEquals(3, boundaryDiscrepancy.manualMethodResult.assignedGroup?.groupNumber)
        assertEquals(2, boundaryDiscrepancy.calendarCredMethodResult.assignedGroup?.groupNumber)
        assertFalse(boundaryDiscrepancy.selectsSameGroup)
        assertEquals(
            EdiGroupAutoSelectionStatus.BLOCKED_GROUP_DISCREPANCY_REQUIRES_PROFESSIONAL_REVIEW,
            boundaryDiscrepancy.autoSelectionStatus
        )
        assertNull(boundaryDiscrepancy.automaticallyAssignedGroup)
        assertEquals(2, boundaryDiscrepancy.candidateGroupsForProfessionalReview.size)
    }

    @Test
    fun `verifica edad gestacional en semanas y dias sin redondeo`() {
        val ga = GestationalAgeInput(weeks = 33, days = 5)
        assertEquals("33s 5d", ga.originalLabel)
        assertEquals(236, ga.totalGestationalDays)
        assertTrue(ga.isPreterm)
        assertEquals(44, ga.prematurityDaysTo40Weeks)
        assertTrue(ga.formulaDocumentation.contains("280 - (33 × 7 + 5) = 280 - 236 = 44 días"))
    }

    @Test
    fun `verifica dependencia auditada de Lamina A en ALE 9_1 frente a LE 9_3, LE 12_3 y ALE 14_1`() {
        val ale91 = EdiOfficialCatalog.findItemByCode("ALE 9.1")
        assertNotNull(ale91)
        // ALE 9.1 tiene la Lámina A listada en el encabezado general del Grupo 9 (pág. 83),
        // pero NO la utiliza en su protocolo clínico directo (señalar a papá/mamá/cuidador).
        assertTrue(ale91!!.generalHeaderMaterials.isNotEmpty())
        assertTrue(ale91.isListedWithHeaderMaterialOnly())
        assertFalse(ale91.requiresMaterialInProtocol)
        assertFalse(ale91.requiresPlateInProtocol(EdiOfficialPlate.LAMINA_A))

        // LE 9.3 SÍ requiere directamente la Lámina A en su protocolo (pág. 85)
        val le93 = EdiOfficialCatalog.findItemByCode("LE 9.3")
        assertNotNull(le93)
        assertTrue(le93!!.requiresMaterialInProtocol)
        assertTrue(le93.requiresPlateInProtocol(EdiOfficialPlate.LAMINA_A))

        // LE 12.3 y ALE 14.1 requieren directamente las Láminas B, C y D en su protocolo
        val le123 = EdiOfficialCatalog.findItemByCode("LE 12.3")
        val ale141 = EdiOfficialCatalog.findItemByCode("ALE 14.1")
        assertNotNull(le123)
        assertNotNull(ale141)
        assertEquals(
            setOf(EdiOfficialPlate.LAMINA_B, EdiOfficialPlate.LAMINA_C, EdiOfficialPlate.LAMINA_D),
            le123!!.protocolRequiredPlates
        )
        assertEquals(
            setOf(EdiOfficialPlate.LAMINA_B, EdiOfficialPlate.LAMINA_C, EdiOfficialPlate.LAMINA_D),
            ale141!!.protocolRequiredPlates
        )

        // Si en Grupo 9 se intenta marcar NO_EVALUADO_MISSING_MATERIAL en ALE 9.1,
        // el motor no permite bloquearlo por falta de material porque su protocolo no usa lámina.
        var stateG9 = EdiPhase1SessionManager.createInitialState(9)
        stateG9 = EdiPhase1SessionManager.recordCurrentGroupItemResponse(
            stateG9,
            ale91.uniqueId,
            EdiItemResponse.NOT_EVALUATED_MISSING_MATERIAL
        )
        assertEquals(
            EdiItemResponse.UNANSWERED,
            stateG9.activeSession.currentGroupResponses[ale91.uniqueId]
        )

        // En cambio, LE 9.3 sí acepta NOT_EVALUATED_MISSING_MATERIAL cuando falta la Lámina A
        stateG9 = EdiPhase1SessionManager.recordCurrentGroupItemResponse(
            stateG9,
            le93.uniqueId,
            EdiItemResponse.NOT_EVALUATED_MISSING_MATERIAL
        )
        assertEquals(
            EdiItemResponse.NOT_EVALUATED_MISSING_MATERIAL,
            stateG9.activeSession.currentGroupResponses[le93.uniqueId]
        )
    }

    @Test
    fun `verifica modalidades clinicas de ALE 12_3, LE 13_3 y LE 14_3 y bloqueo por discrepancia`() {
        val ale123 = EdiOfficialCatalog.findItemByCode("ALE 12.3")!!
        val le133 = EdiOfficialCatalog.findItemByCode("LE 13.3")!!
        val le143 = EdiOfficialCatalog.findItemByCode("LE 14.3")!!

        assertEquals(EdiClinicalModality.DUAL_OBSERVATION_AND_INTERVIEW_ALE_12_3, ale123.modality)
        assertEquals(EdiClinicalModality.INTERVIEW_PRIMARY_WITH_OBSERVATION_COMPLEMENT, le133.modality)
        assertEquals(EdiClinicalModality.INTERVIEW_PRIMARY_WITH_OBSERVATION_COMPLEMENT, le143.modality)

        // En ALE 12.3: si sólo hay observación pero falta entrevista complementaria -> UNANSWERED
        var stateG12 = EdiPhase1SessionManager.createInitialState(12)
        stateG12 = EdiPhase1SessionManager.recordDualModalityEvidence(
            state = stateG12,
            itemUniqueId = ale123.uniqueId,
            observationEvidence = false,
            interviewEvidence = null
        )
        assertEquals(
            EdiItemResponse.UNANSWERED,
            stateG12.activeSession.currentGroupResponses[ale123.uniqueId]
        )

        // Si observación = NO (false) y entrevista = SÍ (true) -> DISCREPANCY_REQUIRES_HUMAN_RESOLUTION
        stateG12 = EdiPhase1SessionManager.recordDualModalityEvidence(
            state = stateG12,
            itemUniqueId = ale123.uniqueId,
            observationEvidence = false,
            interviewEvidence = true
        )
        assertEquals(
            EdiItemResponse.DISCREPANCY_REQUIRES_HUMAN_RESOLUTION,
            stateG12.activeSession.currentGroupResponses[ale123.uniqueId]
        )

        // Cuando el profesional resuelve manualmente la discrepancia -> respuesta confirmada
        stateG12 = EdiPhase1SessionManager.recordDualModalityEvidence(
            state = stateG12,
            itemUniqueId = ale123.uniqueId,
            observationEvidence = false,
            interviewEvidence = true,
            humanResolutionIfConflict = false,
            note = "Se corrobora contacto visual adecuado durante toda la consulta."
        )
        assertEquals(
            EdiItemResponse.NO,
            stateG12.activeSession.currentGroupResponses[ale123.uniqueId]
        )
    }

    @Test
    fun `verifica conservacion de respuestas y confirmacion antes de descartar al cambiar de grupo`() {
        var state = EdiPhase1SessionManager.createInitialState(3)
        val mg31 = EdiOfficialCatalog.findItemByCode("MG 3.1")!!
        state = EdiPhase1SessionManager.recordCurrentGroupItemResponse(
            state,
            mg31.uniqueId,
            EdiItemResponse.YES
        )
        assertTrue(state.activeSession.hasRecordedClinicalResponses)

        // Solicitar cambio al Grupo 4: NO elimina las respuestas automáticamente ni cambia sin confirmación
        state = EdiPhase1SessionManager.requestGroupChange(
            state = state,
            targetGroupNumber = 4,
            reason = "Cambio de fecha de evaluación"
        )
        assertEquals(3, state.activeGroupNumber)
        assertNotNull(state.pendingGroupChangeConfirmation)
        assertEquals(
            EdiItemResponse.YES,
            state.pendingGroupChangeConfirmation?.preservedSnapshotOfPreviousGroup?.currentGroupResponses?.get(mg31.uniqueId)
        )

        // Confirmar archivando el registro previo: cambia a Grupo 4 limpio y conserva el snapshot del Grupo 3
        state = EdiPhase1SessionManager.confirmPendingGroupChange(
            state = state,
            action = EdiGroupChangeResolutionAction.ARCHIVE_PREVIOUS_AND_SWITCH_TO_CLEAN_TARGET
        )
        assertEquals(4, state.activeGroupNumber)
        assertNull(state.pendingGroupChangeConfirmation)
        assertFalse(state.activeSession.hasRecordedClinicalResponses)
        assertEquals(
            EdiItemResponse.YES,
            state.archivedSessionsByGroup[3]?.currentGroupResponses?.get(mg31.uniqueId)
        )
    }
}
