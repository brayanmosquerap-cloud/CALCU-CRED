package com.example

import com.example.model.EdiAreaColorResult
import com.example.model.EdiAreaFallbackEngine
import com.example.model.EdiAxis
import com.example.model.EdiClassificationCauseType
import com.example.model.EdiDevArea
import com.example.model.EdiGlobalSemaphoreEngine
import com.example.model.EdiGlobalTrafficLight
import com.example.model.EdiGroupChangeResolutionAction
import com.example.model.EdiItemResponse
import com.example.model.EdiOfficialCatalog
import com.example.model.EdiPhase1EngineState
import com.example.model.EdiPhase1SessionManager
import com.example.model.EdiPriorityCriticalAlertType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EdiPhase3SemaphoreTest {

    private fun buildBaselineCleanState(
        groupNumber: Int,
        includeFrbAndAleAsNo: Boolean
    ): EdiPhase1EngineState {
        var state = EdiPhase1SessionManager.createInitialState(groupNumber)
        val spec = EdiOfficialCatalog.getGroup(groupNumber)

        if (includeFrbAndAleAsNo) {
            EdiOfficialCatalog.frbItems.forEach { frb ->
                state = EdiPhase1SessionManager.recordCurrentGroupItemResponse(state, frb.uniqueId, EdiItemResponse.NO)
            }
            spec.aleItems.forEach { ale ->
                state = EdiPhase1SessionManager.recordCurrentGroupItemResponse(state, ale.uniqueId, EdiItemResponse.NO)
            }
        }

        spec.areaItems.values.flatten().forEach { areaItem ->
            state = EdiPhase1SessionManager.recordCurrentGroupItemResponse(state, areaItem.uniqueId, EdiItemResponse.YES)
        }

        EdiOfficialCatalog.getApplicableEnItems(groupNumber).forEach { en ->
            state = EdiPhase1SessionManager.recordCurrentGroupItemResponse(state, en.uniqueId, EdiItemResponse.NO)
        }

        spec.alaItems.forEach { ala ->
            state = EdiPhase1SessionManager.recordCurrentGroupItemResponse(state, ala.uniqueId, EdiItemResponse.NO)
        }

        return state
    }

    private fun setAreaResponses(
        initialState: EdiPhase1EngineState,
        groupNumber: Int,
        area: EdiDevArea,
        responses: List<EdiItemResponse>
    ): EdiPhase1EngineState {
        val items = EdiOfficialCatalog.getGroup(groupNumber).areaItems[area].orEmpty()
        assertEquals(items.size, responses.size)
        var state = initialState
        items.forEachIndexed { idx, item ->
            state = EdiPhase1SessionManager.recordCurrentGroupItemResponse(state, item.uniqueId, responses[idx])
        }
        return state
    }

    private fun setAreaFallbackResponses(
        initialState: EdiPhase1EngineState,
        groupNumber: Int,
        area: EdiDevArea,
        responses: List<EdiItemResponse>
    ): EdiPhase1EngineState {
        val fbItems = EdiAreaFallbackEngine.getFallbackItemsForArea(groupNumber, area)
        assertEquals(fbItems.size, responses.size)
        var state = initialState
        fbItems.forEachIndexed { idx, fb ->
            state = EdiAreaFallbackEngine.recordValidatedFallbackResponse(state, fb.fallbackUniqueId, responses[idx])
        }
        return state
    }

    @Test
    fun `semaforo VERDE, AMARILLO y ROJO explícito para los 15 grupos oficiales`() {
        for (group in 1..15) {
            // 1. Caso VERDE explícito en G1..G15
            val greenState = buildBaselineCleanState(group, includeFrbAndAleAsNo = (group in 1..4))
            val greenEval = EdiGlobalSemaphoreEngine.evaluateGlobalSemaphore(greenState)
            assertEquals("Grupo $group debe emitir VERDE", EdiGlobalTrafficLight.VERDE, greenEval.trafficLight)
            assertTrue(greenEval.isDefinitive)
            assertTrue(greenEval.blockingReasons.isEmpty())

            // 2. Caso AMARILLO explícito en G1..G15
            val yellowState = if (group == 1) {
                // Grupo 1 no tiene áreas amarillas: 2 FRB positivos -> AMARILLO
                var s = greenState
                s = EdiPhase1SessionManager.recordCurrentGroupItemResponse(s, EdiOfficialCatalog.frbItems[0].uniqueId, EdiItemResponse.YES)
                EdiPhase1SessionManager.recordCurrentGroupItemResponse(s, EdiOfficialCatalog.frbItems[1].uniqueId, EdiItemResponse.YES)
            } else {
                // Grupos 2..15: exactamente 1 área AMARILLA (1 SÍ, resto NO) -> AMARILLO
                val itemsPerArea = EdiOfficialCatalog.getGroup(group).itemsPerActiveArea
                val yellowPattern = if (itemsPerArea == 2) {
                    listOf(EdiItemResponse.YES, EdiItemResponse.NO)
                } else {
                    listOf(EdiItemResponse.YES, EdiItemResponse.NO, EdiItemResponse.NO)
                }
                setAreaResponses(greenState, group, EdiDevArea.MG, yellowPattern)
            }
            val yellowEval = EdiGlobalSemaphoreEngine.evaluateGlobalSemaphore(yellowState)
            assertEquals("Grupo $group debe emitir AMARILLO", EdiGlobalTrafficLight.AMARILLO, yellowEval.trafficLight)
            assertTrue(yellowEval.isDefinitive)

            // 3. Caso ROJO explícito en G1..G15 (por una sola área ROJA)
            val redState = if (group == 1) {
                setAreaResponses(greenState, 1, EdiDevArea.MG, listOf(EdiItemResponse.YES, EdiItemResponse.NO))
            } else {
                val itemsPerArea = EdiOfficialCatalog.getGroup(group).itemsPerActiveArea
                var s = setAreaResponses(greenState, group, EdiDevArea.MG, List(itemsPerArea) { EdiItemResponse.NO })
                val fbCount = EdiAreaFallbackEngine.getFallbackItemsForArea(group, EdiDevArea.MG).size
                s = setAreaFallbackResponses(s, group, EdiDevArea.MG, List(fbCount) { EdiItemResponse.NO })
                s
            }
            val redEval = EdiGlobalSemaphoreEngine.evaluateGlobalSemaphore(redState)
            assertEquals("Grupo $group debe emitir ROJO", EdiGlobalTrafficLight.ROJO, redEval.trafficLight)
            assertTrue(redEval.isDefinitive)
            assertEquals(listOf(EdiDevArea.MG), redEval.redAreas)
        }
    }

    @Test
    fun `grupo 1 cumple todas las combinaciones de FRB, ALE, EN, areas y no exige ALA`() {
        val baseG1 = buildBaselineCleanState(1, includeFrbAndAleAsNo = true)
        val frb = EdiOfficialCatalog.frbItems
        val aleG1 = EdiOfficialCatalog.getGroup(1).aleItems

        val baseRes = EdiGlobalSemaphoreEngine.evaluateGlobalSemaphore(baseG1)
        assertFalse(baseRes.alaAppliesToGroup)
        assertTrue(baseRes.frbMandatoryForGroup)
        assertTrue(baseRes.aleMandatoryForGroup)

        // 1 FRB solo -> VERDE
        val s1Frb = EdiPhase1SessionManager.recordCurrentGroupItemResponse(baseG1, frb[0].uniqueId, EdiItemResponse.YES)
        assertEquals(EdiGlobalTrafficLight.VERDE, EdiGlobalSemaphoreEngine.evaluateGlobalSemaphore(s1Frb).trafficLight)

        // 1 ALE sola -> VERDE
        val s1Ale = EdiPhase1SessionManager.recordCurrentGroupItemResponse(baseG1, aleG1[0].uniqueId, EdiItemResponse.YES)
        assertEquals(EdiGlobalTrafficLight.VERDE, EdiGlobalSemaphoreEngine.evaluateGlobalSemaphore(s1Ale).trafficLight)

        // 2 FRB positivos -> AMARILLO
        var s2Frb = EdiPhase1SessionManager.recordCurrentGroupItemResponse(baseG1, frb[0].uniqueId, EdiItemResponse.YES)
        s2Frb = EdiPhase1SessionManager.recordCurrentGroupItemResponse(s2Frb, frb[3].uniqueId, EdiItemResponse.YES)
        val res2Frb = EdiGlobalSemaphoreEngine.evaluateGlobalSemaphore(s2Frb)
        assertEquals(EdiGlobalTrafficLight.AMARILLO, res2Frb.trafficLight)
        assertTrue(res2Frb.causes.any { it.type == EdiClassificationCauseType.G1_TO_G4_TWO_OR_MORE_POSITIVE_FRB })

        // 2 ALE positivas -> AMARILLO
        var s2Ale = EdiPhase1SessionManager.recordCurrentGroupItemResponse(baseG1, aleG1[0].uniqueId, EdiItemResponse.YES)
        s2Ale = EdiPhase1SessionManager.recordCurrentGroupItemResponse(s2Ale, aleG1[1].uniqueId, EdiItemResponse.YES)
        val res2Ale = EdiGlobalSemaphoreEngine.evaluateGlobalSemaphore(s2Ale)
        assertEquals(EdiGlobalTrafficLight.AMARILLO, res2Ale.trafficLight)
        assertTrue(res2Ale.causes.any { it.type == EdiClassificationCauseType.G1_TO_G4_TWO_OR_MORE_POSITIVE_ALE })

        // 1 FRB + 1 ALE positivas -> AMARILLO
        var s1Frb1Ale = EdiPhase1SessionManager.recordCurrentGroupItemResponse(baseG1, frb[2].uniqueId, EdiItemResponse.YES)
        s1Frb1Ale = EdiPhase1SessionManager.recordCurrentGroupItemResponse(s1Frb1Ale, aleG1[2].uniqueId, EdiItemResponse.YES)
        val res1Frb1Ale = EdiGlobalSemaphoreEngine.evaluateGlobalSemaphore(s1Frb1Ale)
        assertEquals(EdiGlobalTrafficLight.AMARILLO, res1Frb1Ale.trafficLight)
        assertTrue(res1Frb1Ale.causes.any { it.type == EdiClassificationCauseType.G1_TO_G4_ONE_POSITIVE_FRB_AND_ONE_POSITIVE_ALE })

        // 1 alteración EN (aún con áreas VERDES) -> ROJO
        val en11 = EdiOfficialCatalog.getApplicableEnItems(1).first()
        val sEnPos = EdiPhase1SessionManager.recordCurrentGroupItemResponse(s1Frb1Ale, en11.uniqueId, EdiItemResponse.YES)
        val resEnPos = EdiGlobalSemaphoreEngine.evaluateGlobalSemaphore(sEnPos)
        assertEquals(EdiGlobalTrafficLight.ROJO, resEnPos.trafficLight)
        assertEquals(listOf("EN 1.1"), resEnPos.causes.first().involvedCodes)

        // 1 área ROJA (1 SÍ, 1 NO en Grupo 1) -> ROJO; áreas en G1 nunca son AMARILLAS
        val sAreaRed = setAreaResponses(baseG1, 1, EdiDevArea.SO, listOf(EdiItemResponse.YES, EdiItemResponse.NO))
        val resAreaRed = EdiGlobalSemaphoreEngine.evaluateGlobalSemaphore(sAreaRed)
        assertEquals(EdiGlobalTrafficLight.ROJO, resAreaRed.trafficLight)
        assertTrue(resAreaRed.yellowAreas.isEmpty())
        assertEquals(listOf(EdiDevArea.SO), resAreaRed.redAreas)
    }

    @Test
    fun `grupos 2, 3 y 4 cumplen todas las combinaciones decisivas de FRB, ALE, areas, ALA y EN`() {
        for (group in 2..4) {
            val base = buildBaselineCleanState(group, includeFrbAndAleAsNo = true)
            val frb = EdiOfficialCatalog.frbItems
            val ale = EdiOfficialCatalog.getGroup(group).aleItems
            val ala = EdiOfficialCatalog.getGroup(group).alaItems
            val en = EdiOfficialCatalog.getApplicableEnItems(group)

            // 1 FRB solo -> VERDE; 1 ALE sola -> VERDE
            val s1Frb = EdiPhase1SessionManager.recordCurrentGroupItemResponse(base, frb[0].uniqueId, EdiItemResponse.YES)
            assertEquals(EdiGlobalTrafficLight.VERDE, EdiGlobalSemaphoreEngine.evaluateGlobalSemaphore(s1Frb).trafficLight)

            val s1Ale = EdiPhase1SessionManager.recordCurrentGroupItemResponse(base, ale[0].uniqueId, EdiItemResponse.YES)
            assertEquals(EdiGlobalTrafficLight.VERDE, EdiGlobalSemaphoreEngine.evaluateGlobalSemaphore(s1Ale).trafficLight)

            // 2 FRB -> AMARILLO; 2 ALE -> AMARILLO; 1 FRB + 1 ALE -> AMARILLO
            val s2Frb = EdiPhase1SessionManager.recordCurrentGroupItemResponse(s1Frb, frb[1].uniqueId, EdiItemResponse.YES)
            assertEquals(EdiGlobalTrafficLight.AMARILLO, EdiGlobalSemaphoreEngine.evaluateGlobalSemaphore(s2Frb).trafficLight)

            val s2Ale = EdiPhase1SessionManager.recordCurrentGroupItemResponse(s1Ale, ale[1].uniqueId, EdiItemResponse.YES)
            assertEquals(EdiGlobalTrafficLight.AMARILLO, EdiGlobalSemaphoreEngine.evaluateGlobalSemaphore(s2Ale).trafficLight)

            val s1Frb1Ale = EdiPhase1SessionManager.recordCurrentGroupItemResponse(s1Frb, ale[0].uniqueId, EdiItemResponse.YES)
            assertEquals(EdiGlobalTrafficLight.AMARILLO, EdiGlobalSemaphoreEngine.evaluateGlobalSemaphore(s1Frb1Ale).trafficLight)

            // Exactamente 1 área AMARILLA sin FRB ni ALE -> AMARILLO
            val s1Yellow = setAreaResponses(base, group, EdiDevArea.MF, listOf(EdiItemResponse.YES, EdiItemResponse.NO))
            val r1Yellow = EdiGlobalSemaphoreEngine.evaluateGlobalSemaphore(s1Yellow)
            assertEquals(EdiGlobalTrafficLight.AMARILLO, r1Yellow.trafficLight)
            assertEquals(listOf(EdiDevArea.MF), r1Yellow.yellowAreas)

            // 2 áreas AMARILLAS -> ROJO
            val s2Yellow = setAreaResponses(s1Yellow, group, EdiDevArea.LE, listOf(EdiItemResponse.NO, EdiItemResponse.YES))
            val r2Yellow = EdiGlobalSemaphoreEngine.evaluateGlobalSemaphore(s2Yellow)
            assertEquals(EdiGlobalTrafficLight.ROJO, r2Yellow.trafficLight)
            assertTrue(r2Yellow.causes.any { it.type == EdiClassificationCauseType.G2_TO_G4_TWO_OR_MORE_YELLOW_AREAS })

            // 1 área AMARILLA + 1 FRB positivo -> ROJO
            val s1YellowPlusFrb = EdiPhase1SessionManager.recordCurrentGroupItemResponse(s1Yellow, frb[0].uniqueId, EdiItemResponse.YES)
            val r1YellowPlusFrb = EdiGlobalSemaphoreEngine.evaluateGlobalSemaphore(s1YellowPlusFrb)
            assertEquals(EdiGlobalTrafficLight.ROJO, r1YellowPlusFrb.trafficLight)
            assertTrue(r1YellowPlusFrb.causes.any { it.type == EdiClassificationCauseType.G2_TO_G4_YELLOW_AREA_PLUS_POSITIVE_FRB })

            // 1 área AMARILLA + 1 ALE positiva -> ROJO
            val s1YellowPlusAle = EdiPhase1SessionManager.recordCurrentGroupItemResponse(s1Yellow, ale[0].uniqueId, EdiItemResponse.YES)
            val r1YellowPlusAle = EdiGlobalSemaphoreEngine.evaluateGlobalSemaphore(s1YellowPlusAle)
            assertEquals(EdiGlobalTrafficLight.ROJO, r1YellowPlusAle.trafficLight)
            assertTrue(r1YellowPlusAle.causes.any { it.type == EdiClassificationCauseType.G2_TO_G4_YELLOW_AREA_PLUS_POSITIVE_ALE })

            // 1 sola ALA positiva -> ROJO
            val s1Ala = EdiPhase1SessionManager.recordCurrentGroupItemResponse(base, ala[0].uniqueId, EdiItemResponse.YES)
            val r1Ala = EdiGlobalSemaphoreEngine.evaluateGlobalSemaphore(s1Ala)
            assertEquals(EdiGlobalTrafficLight.ROJO, r1Ala.trafficLight)
            assertTrue(r1Ala.causes.any { it.type == EdiClassificationCauseType.POSITIVE_ALA })

            // 1 sola alteración EN positiva -> ROJO
            val s1En = EdiPhase1SessionManager.recordCurrentGroupItemResponse(base, en[2].uniqueId, EdiItemResponse.YES)
            val r1En = EdiGlobalSemaphoreEngine.evaluateGlobalSemaphore(s1En)
            assertEquals(EdiGlobalTrafficLight.ROJO, r1En.trafficLight)
            assertTrue(r1En.causes.any { it.type == EdiClassificationCauseType.POSITIVE_EN })
        }
    }

    @Test
    fun `grupos 5 al 15 tratan FRB y ALE como opcionales sin modificar el semaforo y permiten multiples areas amarillas`() {
        for (group in 5..15) {
            // FRB y ALE completamente SIN RESPONDER -> debe emitir VERDE si Áreas=VERDE y EN/ALA=NO
            val baseSinFrbAle = buildBaselineCleanState(group, includeFrbAndAleAsNo = false)
            val resOptionalUnanswered = EdiGlobalSemaphoreEngine.evaluateGlobalSemaphore(baseSinFrbAle)
            assertFalse(resOptionalUnanswered.frbMandatoryForGroup)
            assertFalse(resOptionalUnanswered.aleMandatoryForGroup)
            assertFalse(resOptionalUnanswered.frbAndAleModifyTrafficLight)
            assertEquals(EdiGlobalTrafficLight.VERDE, resOptionalUnanswered.trafficLight)

            // Todos los FRB y todas las ALE positivos en G5..G15 -> NO cambian VERDE a AMARILLO ni ROJO
            var allFrbAndAleYes = baseSinFrbAle
            EdiOfficialCatalog.frbItems.forEach { frb ->
                allFrbAndAleYes = EdiPhase1SessionManager.recordCurrentGroupItemResponse(allFrbAndAleYes, frb.uniqueId, EdiItemResponse.YES)
            }
            EdiOfficialCatalog.getGroup(group).aleItems.forEach { ale ->
                allFrbAndAleYes = EdiPhase1SessionManager.recordCurrentGroupItemResponse(allFrbAndAleYes, ale.uniqueId, EdiItemResponse.YES)
            }
            val resAllFrbAlePos = EdiGlobalSemaphoreEngine.evaluateGlobalSemaphore(allFrbAndAleYes)
            assertEquals(7, resAllFrbAlePos.positiveFrbItems.size)
            assertEquals(EdiOfficialCatalog.getGroup(group).aleItems.size, resAllFrbAlePos.positiveAleItems.size)
            assertEquals("G$group debe seguir VERDE aunque FRB/ALE sean positivos", EdiGlobalTrafficLight.VERDE, resAllFrbAlePos.trafficLight)

            // Varias áreas AMARILLAS (e.g. MG y MF) + todos FRB/ALE positivos en G5..G15 -> AMARILLO (no ROJO)
            val itemsPerArea = EdiOfficialCatalog.getGroup(group).itemsPerActiveArea
            val yellowPattern = if (itemsPerArea == 2) {
                listOf(EdiItemResponse.YES, EdiItemResponse.NO)
            } else {
                listOf(EdiItemResponse.YES, EdiItemResponse.NO, EdiItemResponse.NO)
            }
            var twoYellows = setAreaResponses(allFrbAndAleYes, group, EdiDevArea.MG, yellowPattern)
            twoYellows = setAreaResponses(twoYellows, group, EdiDevArea.MF, yellowPattern)
            val resTwoYellows = EdiGlobalSemaphoreEngine.evaluateGlobalSemaphore(twoYellows)
            assertEquals("G$group con 2 áreas AMARILLAS debe ser AMARILLO", EdiGlobalTrafficLight.AMARILLO, resTwoYellows.trafficLight)
            assertEquals(listOf(EdiDevArea.MG, EdiDevArea.MF), resTwoYellows.yellowAreas)

            // 1 sola ALA positiva o 1 sola EN positiva en G5..G15 -> ROJO
            val alaFirst = EdiOfficialCatalog.getGroup(group).alaItems.first()
            val sAla = EdiPhase1SessionManager.recordCurrentGroupItemResponse(baseSinFrbAle, alaFirst.uniqueId, EdiItemResponse.YES)
            assertEquals(EdiGlobalTrafficLight.ROJO, EdiGlobalSemaphoreEngine.evaluateGlobalSemaphore(sAla).trafficLight)

            val enFirst = EdiOfficialCatalog.getApplicableEnItems(group).first()
            val sEn = EdiPhase1SessionManager.recordCurrentGroupItemResponse(baseSinFrbAle, enFirst.uniqueId, EdiItemResponse.YES)
            assertEquals(EdiGlobalTrafficLight.ROJO, EdiGlobalSemaphoreEngine.evaluateGlobalSemaphore(sEn).trafficLight)
        }
    }

    @Test
    fun `excepciones oficiales retroceso Grupo 8, CO-R Grupo 13 y exclusion EN 1_3 Grupo 15 en el semaforo global`() {
        // 1. Excepción Grupo 8: MG 0/3 -> retrocede a los 2 ítems de Grupo 7
        val baseG8 = buildBaselineCleanState(8, includeFrbAndAleAsNo = false)
        val g8MgZero = setAreaResponses(baseG8, 8, EdiDevArea.MG, listOf(EdiItemResponse.NO, EdiItemResponse.NO, EdiItemResponse.NO))
        // Sin responder retroceso -> INCOMPLETE
        assertEquals(EdiGlobalTrafficLight.INCOMPLETE, EdiGlobalSemaphoreEngine.evaluateGlobalSemaphore(g8MgZero).trafficLight)
        // Retroceso G7 2/2 -> área MG AMARILLA -> semáforo global AMARILLO
        val g8FbYellow = setAreaFallbackResponses(g8MgZero, 8, EdiDevArea.MG, listOf(EdiItemResponse.YES, EdiItemResponse.YES))
        val rG8Yellow = EdiGlobalSemaphoreEngine.evaluateGlobalSemaphore(g8FbYellow)
        assertEquals(EdiGlobalTrafficLight.AMARILLO, rG8Yellow.trafficLight)
        assertEquals(EdiAreaColorResult.AMARILLO, rG8Yellow.axis3AreasSummary.areaResults[EdiDevArea.MG]?.status)
        // Retroceso G7 1/2 -> área MG ROJA -> semáforo global ROJO
        val g8FbRed = setAreaFallbackResponses(g8MgZero, 8, EdiDevArea.MG, listOf(EdiItemResponse.YES, EdiItemResponse.NO))
        val rG8Red = EdiGlobalSemaphoreEngine.evaluateGlobalSemaphore(g8FbRed)
        assertEquals(EdiGlobalTrafficLight.ROJO, rG8Red.trafficLight)
        assertEquals(listOf(EdiDevArea.MG), rG8Red.redAreas)

        // 2. Excepción Grupo 13: CO 0/3 -> retrocede exclusivamente a CO-R 13.1, 13.2 y 13.3
        val baseG13 = buildBaselineCleanState(13, includeFrbAndAleAsNo = false)
        val g13CoZero = setAreaResponses(baseG13, 13, EdiDevArea.CO, listOf(EdiItemResponse.NO, EdiItemResponse.NO, EdiItemResponse.NO))
        assertEquals(EdiGlobalTrafficLight.INCOMPLETE, EdiGlobalSemaphoreEngine.evaluateGlobalSemaphore(g13CoZero).trafficLight)
        // CO-R 2/3 -> área CO AMARILLA -> semáforo global AMARILLO
        val g13CoRYellow = setAreaFallbackResponses(g13CoZero, 13, EdiDevArea.CO, listOf(EdiItemResponse.YES, EdiItemResponse.NO, EdiItemResponse.YES))
        val rG13Yellow = EdiGlobalSemaphoreEngine.evaluateGlobalSemaphore(g13CoRYellow)
        assertEquals(EdiGlobalTrafficLight.AMARILLO, rG13Yellow.trafficLight)
        assertTrue(rG13Yellow.axis3AreasSummary.areaResults[EdiDevArea.CO]!!.usesSpecialCoRFallback)
        // CO-R 1/3 -> área CO ROJA -> semáforo global ROJO
        val g13CoRRed = setAreaFallbackResponses(g13CoZero, 13, EdiDevArea.CO, listOf(EdiItemResponse.YES, EdiItemResponse.NO, EdiItemResponse.NO))
        val rG13Red = EdiGlobalSemaphoreEngine.evaluateGlobalSemaphore(g13CoRRed)
        assertEquals(EdiGlobalTrafficLight.ROJO, rG13Red.trafficLight)
        assertEquals(listOf(EdiDevArea.CO), rG13Red.redAreas)

        // 3. Exclusión EN 1.3 del Grupo 15:
        //    Aunque EN 1.3 nunca sea respondido (ni siquiera existe en la sesión del G15),
        //    con EN 1.1=NO y EN 1.2=NO debe emitir VERDE definitivo.
        val baseG15 = buildBaselineCleanState(15, includeFrbAndAleAsNo = false)
        val rG15 = EdiGlobalSemaphoreEngine.evaluateGlobalSemaphore(baseG15)
        assertFalse(rG15.en13AppliesToGroup)
        assertEquals(2, rG15.enResponsesResolved.size)
        assertFalse(rG15.enResponsesResolved.containsKey("EN_1_3"))
        assertEquals(EdiGlobalTrafficLight.VERDE, rG15.trafficLight)
        assertTrue(rG15.isDefinitive)
    }

    @Test
    fun `evaluaciones incompletas bloquean semaforo definitivo y exponen advertencias clinicas prioritarias`() {
        // Caso 1: Grupo 3 sin FRB/ALE respondidos -> INCOMPLETE (sin hallazgos críticos)
        var incompleteG3 = buildBaselineCleanState(3, includeFrbAndAleAsNo = false)
        val rIncompleteNoCritical = EdiGlobalSemaphoreEngine.evaluateGlobalSemaphore(incompleteG3)
        assertEquals(EdiGlobalTrafficLight.INCOMPLETE, rIncompleteNoCritical.trafficLight)
        assertFalse(rIncompleteNoCritical.isDefinitive)
        assertFalse(rIncompleteNoCritical.hasPriorityCriticalWarning)
        assertFalse(rIncompleteNoCritical.requiresImmediateClinicalAttentionDespiteIncomplete)
        assertTrue(rIncompleteNoCritical.pendingMandatoryItemCodes.contains("FRB 1"))
        assertTrue(rIncompleteNoCritical.pendingMandatoryItemCodes.contains("ALE 3.1"))

        // Caso 2: Durante la evaluación incompleta de G3 se detecta ALA positiva ->
        //         sigue INCOMPLETE pero activa ADVERTENCIA CLÍNICA PRIORITARIA (nunca queda oculta)
        val ala31 = EdiOfficialCatalog.getGroup(3).alaItems.first()
        val withAlaPositive = EdiPhase1SessionManager.recordCurrentGroupItemResponse(incompleteG3, ala31.uniqueId, EdiItemResponse.YES)
        val rIncompleteWithAla = EdiGlobalSemaphoreEngine.evaluateGlobalSemaphore(withAlaPositive)
        assertEquals(EdiGlobalTrafficLight.INCOMPLETE, rIncompleteWithAla.trafficLight)
        assertFalse(rIncompleteWithAla.isDefinitive)
        assertTrue(rIncompleteWithAla.hasPriorityCriticalWarning)
        assertTrue(rIncompleteWithAla.requiresImmediateClinicalAttentionDespiteIncomplete)
        assertEquals(1, rIncompleteWithAla.priorityCriticalAlerts.size)
        assertEquals(EdiPriorityCriticalAlertType.POSITIVE_ALARM_SIGNAL_ALA, rIncompleteWithAla.priorityCriticalAlerts.first().type)
        assertEquals("ALA 3.1", rIncompleteWithAla.priorityCriticalAlerts.first().code)

        // Caso 3: Evaluación incompleta con EN alterada y un área ROJA completamente evaluada en Grupo 10
        var incompleteG10 = EdiPhase1SessionManager.createInitialState(10)
        // Solo completamos MG con 0/3 y retroceso G9 0/3 (área MG cerrada en ROJO), más EN 1.1 = YES; el resto queda UNANSWERED
        incompleteG10 = setAreaResponses(incompleteG10, 10, EdiDevArea.MG, listOf(EdiItemResponse.NO, EdiItemResponse.NO, EdiItemResponse.NO))
        incompleteG10 = setAreaFallbackResponses(incompleteG10, 10, EdiDevArea.MG, listOf(EdiItemResponse.NO, EdiItemResponse.NO, EdiItemResponse.NO))
        incompleteG10 = EdiPhase1SessionManager.recordCurrentGroupItemResponse(incompleteG10, "EN_1_1", EdiItemResponse.YES)

        val rIncompleteG10 = EdiGlobalSemaphoreEngine.evaluateGlobalSemaphore(incompleteG10)
        assertEquals(EdiGlobalTrafficLight.INCOMPLETE, rIncompleteG10.trafficLight)
        assertFalse(rIncompleteG10.isDefinitive)
        assertTrue(rIncompleteG10.hasPriorityCriticalWarning)
        assertTrue(rIncompleteG10.requiresImmediateClinicalAttentionDespiteIncomplete)
        assertEquals(2, rIncompleteG10.priorityCriticalAlerts.size)
        assertTrue(rIncompleteG10.priorityCriticalAlerts.any { it.type == EdiPriorityCriticalAlertType.ALTERED_NEUROLOGICAL_EXAM_EN && it.code == "EN 1.1" })
        assertTrue(rIncompleteG10.priorityCriticalAlerts.any { it.type == EdiPriorityCriticalAlertType.RED_DEVELOPMENT_AREA && it.code == "MG" })
    }

    @Test
    fun `falta de materiales y discrepancias de evidencia bloquean el semaforo sin interpretarse como NO`() {
        // 1. Falta de Lámina A en LE 9.3 (Grupo 9) -> bloquea semáforo definitivo
        val baseG9 = buildBaselineCleanState(9, includeFrbAndAleAsNo = false)
        val le93 = EdiOfficialCatalog.findItemByCode("LE 9.3")!!
        val g9MissingMat = EdiPhase1SessionManager.recordCurrentGroupItemResponse(
            baseG9,
            le93.uniqueId,
            EdiItemResponse.NOT_EVALUATED_MISSING_MATERIAL
        )
        val resMissingMat = EdiGlobalSemaphoreEngine.evaluateGlobalSemaphore(g9MissingMat)
        assertEquals(EdiGlobalTrafficLight.INCOMPLETE, resMissingMat.trafficLight)
        assertFalse(resMissingMat.isDefinitive)
        assertEquals(listOf("LE 9.3"), resMissingMat.missingMaterialItemCodes)
        assertTrue(resMissingMat.causes.any { it.type == EdiClassificationCauseType.BLOCKED_MISSING_MATERIAL })

        // 2. Falta de cinta métrica en EN 1.3 (Grupo 11) -> bloquea semáforo definitivo
        val baseG11 = buildBaselineCleanState(11, includeFrbAndAleAsNo = false)
        val g11MissingEn13 = EdiPhase1SessionManager.recordCurrentGroupItemResponse(
            baseG11,
            "EN_1_3",
            EdiItemResponse.NOT_EVALUATED_MISSING_MATERIAL
        )
        val resMissingEn13 = EdiGlobalSemaphoreEngine.evaluateGlobalSemaphore(g11MissingEn13)
        assertEquals(EdiGlobalTrafficLight.INCOMPLETE, resMissingEn13.trafficLight)
        assertEquals(listOf("EN 1.3"), resMissingEn13.missingMaterialItemCodes)

        // 3. Grupo 12 LE 12.3 (área obligatoria) sin láminas B, C y D -> INCOMPLETO
        val baseG12 = buildBaselineCleanState(12, includeFrbAndAleAsNo = false)
        val le123 = EdiOfficialCatalog.findItemByCode("LE 12.3")!!
        val g12MissingLePlates = EdiPhase1SessionManager.recordCurrentGroupItemResponse(
            baseG12,
            le123.uniqueId,
            EdiItemResponse.NOT_EVALUATED_MISSING_MATERIAL
        )
        val resG12MissingLe = EdiGlobalSemaphoreEngine.evaluateGlobalSemaphore(g12MissingLePlates)
        assertEquals(EdiGlobalTrafficLight.INCOMPLETE, resG12MissingLe.trafficLight)
        assertFalse(resG12MissingLe.isDefinitive)
        assertEquals(listOf("LE 12.3"), resG12MissingLe.missingMaterialItemCodes)

        // 4. Grupo 12 ALE 12.3 (opcional en G5–G15) con discrepancia entre entrevista y observación,
        //    con componentes obligatorios normales -> VERDE y advertencia no bloqueante (nunca interpretado como NO)
        val ale123 = EdiOfficialCatalog.findItemByCode("ALE 12.3")!!
        val g12ConflictAle = EdiPhase1SessionManager.recordDualModalityEvidence(
            state = baseG12,
            itemUniqueId = ale123.uniqueId,
            observationEvidence = true,
            interviewEvidence = false
        )
        val resG12ConflictAle = EdiGlobalSemaphoreEngine.evaluateGlobalSemaphore(g12ConflictAle)
        assertEquals(EdiGlobalTrafficLight.VERDE, resG12ConflictAle.trafficLight)
        assertTrue(resG12ConflictAle.isDefinitive)
        assertTrue(resG12ConflictAle.blockingReasons.isEmpty())
        assertEquals(listOf("ALE 12.3"), resG12ConflictAle.optionalDiscrepancyItemCodes)
        assertTrue(resG12ConflictAle.nonBlockingClinicalWarnings.any { it.contains("ALE 12.3") })
        assertEquals(
            EdiItemResponse.DISCREPANCY_REQUIRES_HUMAN_RESOLUTION,
            resG12ConflictAle.aleResponsesResolved[ale123.uniqueId]
        )

        // 5. Grupo 14 ALE 14.1 (opcional en G5–G15) sin láminas B, C y D,
        //    con componentes obligatorios normales -> VERDE y advertencia no bloqueante (nunca interpretado como NO)
        val baseG14 = buildBaselineCleanState(14, includeFrbAndAleAsNo = false)
        val ale141 = EdiOfficialCatalog.findItemByCode("ALE 14.1")!!
        val g14MissingPlatesAle = EdiPhase1SessionManager.recordCurrentGroupItemResponse(
            baseG14,
            ale141.uniqueId,
            EdiItemResponse.NOT_EVALUATED_MISSING_MATERIAL
        )
        val resG14MissingAle = EdiGlobalSemaphoreEngine.evaluateGlobalSemaphore(g14MissingPlatesAle)
        assertEquals(EdiGlobalTrafficLight.VERDE, resG14MissingAle.trafficLight)
        assertTrue(resG14MissingAle.isDefinitive)
        assertTrue(resG14MissingAle.blockingReasons.isEmpty())
        assertEquals(listOf("ALE 14.1"), resG14MissingAle.optionalMissingMaterialItemCodes)
        assertTrue(resG14MissingAle.nonBlockingClinicalWarnings.any { it.contains("ALE 14.1") })
        assertEquals(
            EdiItemResponse.NOT_EVALUATED_MISSING_MATERIAL,
            resG14MissingAle.aleResponsesResolved[ale141.uniqueId]
        )

        // 6. Discrepancia clínica sin resolver en LE 13.3 (Grupo 13, área obligatoria) -> INCOMPLETO
        val baseG13 = buildBaselineCleanState(13, includeFrbAndAleAsNo = false)
        val le133 = EdiOfficialCatalog.findItemByCode("LE 13.3")!!
        val g13Conflict = EdiPhase1SessionManager.recordDualModalityEvidence(
            state = baseG13,
            itemUniqueId = le133.uniqueId,
            observationEvidence = false,
            interviewEvidence = true
        )
        val resG13Conflict = EdiGlobalSemaphoreEngine.evaluateGlobalSemaphore(g13Conflict)
        assertEquals(EdiGlobalTrafficLight.INCOMPLETE, resG13Conflict.trafficLight)
        assertEquals(listOf("LE 13.3"), resG13Conflict.discrepancyItemCodes)
        assertFalse(resG13Conflict.isDefinitive)
    }

    @Test
    fun `reglas obligatorias de FRB y ALE en G1-G4 vs opcionales no bloqueantes en G5-G15`() {
        // 1. Grupo 2: ALE obligatorio pendiente -> INCOMPLETO
        val completeG2 = buildBaselineCleanState(2, includeFrbAndAleAsNo = true)
        val ale21 = EdiOfficialCatalog.getGroup(2).aleItems.first()
        val g2PendingAle = EdiPhase1SessionManager.recordCurrentGroupItemResponse(
            completeG2,
            ale21.uniqueId,
            EdiItemResponse.UNANSWERED
        )
        val resG2PendingAle = EdiGlobalSemaphoreEngine.evaluateGlobalSemaphore(g2PendingAle)
        assertEquals(EdiGlobalTrafficLight.INCOMPLETE, resG2PendingAle.trafficLight)
        assertFalse(resG2PendingAle.isDefinitive)
        assertTrue(resG2PendingAle.pendingMandatoryItemCodes.contains("ALE 2.1"))

        // 2. Grupos 5–15: FRB/ALE opcionales pendientes, pero un área obligatoria sin completar -> INCOMPLETO
        val baseG10 = buildBaselineCleanState(10, includeFrbAndAleAsNo = false)
        val mf101 = EdiOfficialCatalog.getGroup(10).areaItems[EdiDevArea.MF]!!.first()
        val g10PendingArea = EdiPhase1SessionManager.recordCurrentGroupItemResponse(
            baseG10,
            mf101.uniqueId,
            EdiItemResponse.UNANSWERED
        )
        val resG10PendingArea = EdiGlobalSemaphoreEngine.evaluateGlobalSemaphore(g10PendingArea)
        assertEquals(EdiGlobalTrafficLight.INCOMPLETE, resG10PendingArea.trafficLight)
        assertFalse(resG10PendingArea.isDefinitive)
        assertTrue(resG10PendingArea.pendingMandatoryItemCodes.contains("MF 10.1"))
        assertTrue(resG10PendingArea.optionalPendingItemCodes.contains("FRB 1"))

        // 3. Grupos 5–15: ALA positiva con FRB/ALE opcionales pendientes/sin evaluar y componentes obligatorios completos -> ROJO
        val baseG14 = buildBaselineCleanState(14, includeFrbAndAleAsNo = false)
        val ale141 = EdiOfficialCatalog.findItemByCode("ALE 14.1")!!
        val ala141 = EdiOfficialCatalog.getGroup(14).alaItems.first()
        var g14AlaWithOptMissing = EdiPhase1SessionManager.recordCurrentGroupItemResponse(
            baseG14,
            ale141.uniqueId,
            EdiItemResponse.NOT_EVALUATED_MISSING_MATERIAL
        )
        g14AlaWithOptMissing = EdiPhase1SessionManager.recordCurrentGroupItemResponse(
            g14AlaWithOptMissing,
            ala141.uniqueId,
            EdiItemResponse.YES
        )
        val resG14AlaPos = EdiGlobalSemaphoreEngine.evaluateGlobalSemaphore(g14AlaWithOptMissing)
        assertEquals(EdiGlobalTrafficLight.ROJO, resG14AlaPos.trafficLight)
        assertTrue(resG14AlaPos.isDefinitive)
        assertEquals(listOf("ALA 14.1"), resG14AlaPos.causes.first().involvedCodes)
        assertEquals(listOf("ALE 14.1"), resG14AlaPos.optionalMissingMaterialItemCodes)
        assertTrue(resG14AlaPos.nonBlockingClinicalWarnings.any { it.contains("ALE 14.1") })
    }

    @Test
    fun `aislamiento entre sesiones y bloqueo por discrepancia o edad fuera de rango`() {
        // 1. Discrepancia de edad corregida (30/01/2025 al 12/06/2025 con 34 sem: Manual G3 vs Calendario G2)
        val ageDiscrepancy = EdiOfficialCatalog.compareCorrectedAgeMethods(
            fechaNacimiento = "30/01/2025",
            fechaEvaluacion = "12/06/2025",
            esPrematuro = true,
            egSemanas = 34,
            egDias = 0
        )
        val completeG3 = buildBaselineCleanState(3, includeFrbAndAleAsNo = true)
        val blockedByAge = EdiGlobalSemaphoreEngine.evaluateGlobalSemaphore(completeG3, ageComparison = ageDiscrepancy)
        assertEquals(EdiGlobalTrafficLight.INCOMPLETE, blockedByAge.trafficLight)
        assertFalse(blockedByAge.isDefinitive)
        assertTrue(blockedByAge.blockedByAgeDiscrepancy)
        assertTrue(blockedByAge.causes.any { it.type == EdiClassificationCauseType.BLOCKED_AGE_DISCREPANCY })

        // Incluso bloqueado por discrepancia de edad, una ALA positiva sigue generando advertencia prioritaria
        val ala31 = EdiOfficialCatalog.getGroup(3).alaItems.first()
        val completeG3WithAla = EdiPhase1SessionManager.recordCurrentGroupItemResponse(completeG3, ala31.uniqueId, EdiItemResponse.YES)
        val blockedWithCritical = EdiGlobalSemaphoreEngine.evaluateGlobalSemaphore(completeG3WithAla, ageComparison = ageDiscrepancy)
        assertEquals(EdiGlobalTrafficLight.INCOMPLETE, blockedWithCritical.trafficLight)
        assertTrue(blockedWithCritical.blockedByAgeDiscrepancy)
        assertTrue(blockedWithCritical.hasPriorityCriticalWarning)
        assertTrue(blockedWithCritical.requiresImmediateClinicalAttentionDespiteIncomplete)

        // 2. Edad fuera de rango (< 1 mes y >= 72 meses) -> bloquea semáforo definitivo
        val under1Month = EdiOfficialCatalog.compareCorrectedAgeMethods("10/03/2025", "25/03/2025", false, 39, 0)
        val completeG1 = buildBaselineCleanState(1, includeFrbAndAleAsNo = true)
        val resUnderMin = EdiGlobalSemaphoreEngine.evaluateGlobalSemaphore(completeG1, ageComparison = under1Month)
        assertEquals(EdiGlobalTrafficLight.INCOMPLETE, resUnderMin.trafficLight)
        assertTrue(resUnderMin.blockedByOutOfRangeAge)

        // 3. Aislamiento estricto: pasar sesión del Grupo 3 para evaluar Grupo 4 bloquea y no filtra respuestas
        val mismatchRes = EdiGlobalSemaphoreEngine.evaluateGlobalSemaphore(
            groupNumber = 4,
            session = completeG3WithAla.activeSession
        )
        assertEquals(EdiGlobalTrafficLight.INCOMPLETE, mismatchRes.trafficLight)
        assertTrue(mismatchRes.sessionGroupMismatchDetected)
        assertTrue(mismatchRes.positiveAlaItems.isEmpty())
        assertFalse(mismatchRes.hasPriorityCriticalWarning)

        // 4. Cambiar de grupo archivando el anterior (con hallazgos ROJOS en G3) deja limpio el G4
        var transitionState = EdiPhase1SessionManager.requestGroupChange(
            state = completeG3WithAla,
            targetGroupNumber = 4,
            reason = "Corrección de grupo"
        )
        transitionState = EdiPhase1SessionManager.confirmPendingGroupChange(
            state = transitionState,
            action = EdiGroupChangeResolutionAction.ARCHIVE_PREVIOUS_AND_SWITCH_TO_CLEAN_TARGET
        )
        val resNewCleanG4 = EdiGlobalSemaphoreEngine.evaluateGlobalSemaphore(transitionState)
        assertEquals(4, resNewCleanG4.groupNumber)
        assertEquals(EdiGlobalTrafficLight.INCOMPLETE, resNewCleanG4.trafficLight)
        assertFalse(resNewCleanG4.hasPriorityCriticalWarning)
        assertTrue(resNewCleanG4.positiveAlaItems.isEmpty())
    }
}
