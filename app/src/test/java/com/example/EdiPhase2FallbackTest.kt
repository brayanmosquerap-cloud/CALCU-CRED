package com.example

import com.example.model.EdiAreaColorResult
import com.example.model.EdiAreaFallbackEngine
import com.example.model.EdiDevArea
import com.example.model.EdiItemResponse
import com.example.model.EdiOfficialCatalog
import com.example.model.EdiPhase1EngineState
import com.example.model.EdiPhase1SessionManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EdiPhase2FallbackTest {

    private fun recordAreaResponses(
        initialState: EdiPhase1EngineState,
        groupNumber: Int,
        area: EdiDevArea,
        responses: List<EdiItemResponse>
    ): EdiPhase1EngineState {
        val items = EdiOfficialCatalog.getGroup(groupNumber).areaItems[area].orEmpty()
        assertEquals("Cantidad esperada de reactivos para G$groupNumber ${area.code}", items.size, responses.size)
        var state = initialState
        items.forEachIndexed { index, item ->
            state = EdiPhase1SessionManager.recordCurrentGroupItemResponse(state, item.uniqueId, responses[index])
        }
        return state
    }

    private fun recordFallbackResponses(
        initialState: EdiPhase1EngineState,
        groupNumber: Int,
        area: EdiDevArea,
        responses: List<EdiItemResponse>
    ): EdiPhase1EngineState {
        val fbItems = EdiAreaFallbackEngine.getFallbackItemsForArea(groupNumber, area)
        assertEquals("Cantidad esperada de reactivos de retroceso para G$groupNumber ${area.code}", fbItems.size, responses.size)
        var state = initialState
        fbItems.forEachIndexed { index, fbItem ->
            state = EdiAreaFallbackEngine.recordValidatedFallbackResponse(state, fbItem.fallbackUniqueId, responses[index])
        }
        return state
    }

    @Test
    fun `grupo 1 califica 2 SI verde, 1 o 0 SI rojo sin retroceso, y CO no aplica`() {
        val applicableAreas = listOf(EdiDevArea.MG, EdiDevArea.MF, EdiDevArea.LE, EdiDevArea.SO)
        assertTrue(EdiAreaFallbackEngine.getAllFallbackItemsForGroup(1).isEmpty())

        applicableAreas.forEach { area ->
            // 2 SÍ = VERDE
            val s2Yes = recordAreaResponses(
                EdiPhase1SessionManager.createInitialState(1),
                1,
                area,
                listOf(EdiItemResponse.YES, EdiItemResponse.YES)
            )
            val r2Yes = EdiAreaFallbackEngine.evaluateArea(1, area, s2Yes.activeSession)
            assertEquals(EdiAreaColorResult.VERDE, r2Yes.status)
            assertFalse(r2Yes.fallbackRequired)
            assertEquals(2, r2Yes.currentYesCount)

            // 1 SÍ = ROJO (excepción oficial de Grupo 1, no amarillo)
            val s1Yes = recordAreaResponses(
                EdiPhase1SessionManager.createInitialState(1),
                1,
                area,
                listOf(EdiItemResponse.YES, EdiItemResponse.NO)
            )
            val r1Yes = EdiAreaFallbackEngine.evaluateArea(1, area, s1Yes.activeSession)
            assertEquals(EdiAreaColorResult.ROJO, r1Yes.status)
            assertFalse(r1Yes.fallbackRequired)

            // 0 SÍ = ROJO sin retroceso
            val s0Yes = recordAreaResponses(
                EdiPhase1SessionManager.createInitialState(1),
                1,
                area,
                listOf(EdiItemResponse.NO, EdiItemResponse.NO)
            )
            val r0Yes = EdiAreaFallbackEngine.evaluateArea(1, area, s0Yes.activeSession)
            assertEquals(EdiAreaColorResult.ROJO, r0Yes.status)
            assertFalse(r0Yes.fallbackRequired)
            assertTrue(r0Yes.fallbackItems.isEmpty())
            assertNull(r0Yes.fallbackSourceGroupNumber)
        }

        // CO en Grupo 1 siempre NOT_APPLICABLE
        val summaryG1 = EdiAreaFallbackEngine.evaluateAllAreasForGroup(
            1,
            EdiPhase1SessionManager.createInitialState(1).activeSession
        )
        assertEquals(EdiAreaColorResult.NOT_APPLICABLE, summaryG1.areaResults[EdiDevArea.CO]?.status)
        assertFalse(summaryG1.areaResults[EdiDevArea.CO]!!.appliesToGroup)
    }

    @Test
    fun `grupos 2 al 7 califican 2 SI verde, 1 SI amarillo, 0 SI retroceso a 2 items del grupo anterior`() {
        val applicableAreas = listOf(EdiDevArea.MG, EdiDevArea.MF, EdiDevArea.LE, EdiDevArea.SO)

        for (group in 2..7) {
            applicableAreas.forEach { area ->
                // 2 SÍ -> VERDE
                val sGreen = recordAreaResponses(
                    EdiPhase1SessionManager.createInitialState(group),
                    group,
                    area,
                    listOf(EdiItemResponse.YES, EdiItemResponse.YES)
                )
                val rGreen = EdiAreaFallbackEngine.evaluateArea(group, area, sGreen.activeSession)
                assertEquals("G$group ${area.code} 2 SÍ", EdiAreaColorResult.VERDE, rGreen.status)
                assertFalse(rGreen.fallbackRequired)

                // 1 SÍ -> AMARILLO sin retroceso
                val sYellowDirect = recordAreaResponses(
                    EdiPhase1SessionManager.createInitialState(group),
                    group,
                    area,
                    listOf(EdiItemResponse.NO, EdiItemResponse.YES)
                )
                val rYellowDirect = EdiAreaFallbackEngine.evaluateArea(group, area, sYellowDirect.activeSession)
                assertEquals("G$group ${area.code} 1 SÍ", EdiAreaColorResult.AMARILLO, rYellowDirect.status)
                assertFalse(rYellowDirect.fallbackRequired)

                // 0 SÍ (ambos NO) -> activa retroceso a 2 reactivos de G(n-1)
                val sZeroCurrent = recordAreaResponses(
                    EdiPhase1SessionManager.createInitialState(group),
                    group,
                    area,
                    listOf(EdiItemResponse.NO, EdiItemResponse.NO)
                )
                val rIncompleteFb = EdiAreaFallbackEngine.evaluateArea(group, area, sZeroCurrent.activeSession)
                assertTrue(rIncompleteFb.fallbackRequired)
                assertEquals(group - 1, rIncompleteFb.fallbackSourceGroupNumber)
                assertEquals(2, rIncompleteFb.fallbackItems.size)
                assertEquals(EdiAreaColorResult.INCOMPLETE, rIncompleteFb.status)

                // Retroceso 2/2 SÍ -> AMARILLO
                val sFb2Yes = recordFallbackResponses(
                    sZeroCurrent,
                    group,
                    area,
                    listOf(EdiItemResponse.YES, EdiItemResponse.YES)
                )
                val rFb2Yes = EdiAreaFallbackEngine.evaluateArea(group, area, sFb2Yes.activeSession)
                assertEquals("G$group ${area.code} retroceso 2/2", EdiAreaColorResult.AMARILLO, rFb2Yes.status)
                assertTrue(rFb2Yes.fallbackRequired)
                assertEquals(2, rFb2Yes.fallbackYesCount)

                // Retroceso 1/2 SÍ -> ROJO
                val sFb1Yes = recordFallbackResponses(
                    sZeroCurrent,
                    group,
                    area,
                    listOf(EdiItemResponse.YES, EdiItemResponse.NO)
                )
                val rFb1Yes = EdiAreaFallbackEngine.evaluateArea(group, area, sFb1Yes.activeSession)
                assertEquals("G$group ${area.code} retroceso 1/2", EdiAreaColorResult.ROJO, rFb1Yes.status)

                // Retroceso 0/2 SÍ -> ROJO
                val sFb0Yes = recordFallbackResponses(
                    sZeroCurrent,
                    group,
                    area,
                    listOf(EdiItemResponse.NO, EdiItemResponse.NO)
                )
                val rFb0Yes = EdiAreaFallbackEngine.evaluateArea(group, area, sFb0Yes.activeSession)
                assertEquals("G$group ${area.code} retroceso 0/2", EdiAreaColorResult.ROJO, rFb0Yes.status)
            }

            // CO siempre NOT_APPLICABLE en G2..G7
            val coResult = EdiAreaFallbackEngine.evaluateArea(
                group,
                EdiDevArea.CO,
                EdiPhase1SessionManager.createInitialState(group).activeSession
            )
            assertEquals("CO debe ser NOT_APPLICABLE en G$group", EdiAreaColorResult.NOT_APPLICABLE, coResult.status)
        }
    }

    @Test
    fun `grupo 8 aplica excepcion oficial retrocediendo a los 2 reactivos del grupo 7`() {
        val applicableAreas = listOf(EdiDevArea.MG, EdiDevArea.MF, EdiDevArea.LE, EdiDevArea.SO)

        applicableAreas.forEach { area ->
            // 3 SÍ -> VERDE
            val s3Yes = recordAreaResponses(
                EdiPhase1SessionManager.createInitialState(8),
                8,
                area,
                listOf(EdiItemResponse.YES, EdiItemResponse.YES, EdiItemResponse.YES)
            )
            assertEquals(EdiAreaColorResult.VERDE, EdiAreaFallbackEngine.evaluateArea(8, area, s3Yes.activeSession).status)

            // 2 SÍ -> VERDE
            val s2Yes = recordAreaResponses(
                EdiPhase1SessionManager.createInitialState(8),
                8,
                area,
                listOf(EdiItemResponse.YES, EdiItemResponse.NO, EdiItemResponse.YES)
            )
            val r2Yes = EdiAreaFallbackEngine.evaluateArea(8, area, s2Yes.activeSession)
            assertEquals(EdiAreaColorResult.VERDE, r2Yes.status)
            assertFalse(r2Yes.fallbackRequired)

            // 1 SÍ -> AMARILLO sin retroceso
            val s1Yes = recordAreaResponses(
                EdiPhase1SessionManager.createInitialState(8),
                8,
                area,
                listOf(EdiItemResponse.NO, EdiItemResponse.YES, EdiItemResponse.NO)
            )
            val r1Yes = EdiAreaFallbackEngine.evaluateArea(8, area, s1Yes.activeSession)
            assertEquals(EdiAreaColorResult.AMARILLO, r1Yes.status)
            assertFalse(r1Yes.fallbackRequired)

            // 0 SÍ -> retrocede a los 2 reactivos del Grupo 7
            val s0Yes = recordAreaResponses(
                EdiPhase1SessionManager.createInitialState(8),
                8,
                area,
                listOf(EdiItemResponse.NO, EdiItemResponse.NO, EdiItemResponse.NO)
            )
            val fbSpecs = EdiAreaFallbackEngine.getFallbackItemsForArea(8, area)
            assertEquals(2, fbSpecs.size)
            assertTrue(fbSpecs.all { it.sourceGroupNumber == 7 })
            assertEquals(
                listOf("G8_RETRO_G7_${area.code}_1", "G8_RETRO_G7_${area.code}_2"),
                fbSpecs.map { it.fallbackUniqueId }
            )

            // Retroceso G7: 2/2 = AMARILLO
            val sFb2 = recordFallbackResponses(s0Yes, 8, area, listOf(EdiItemResponse.YES, EdiItemResponse.YES))
            val rFb2 = EdiAreaFallbackEngine.evaluateArea(8, area, sFb2.activeSession)
            assertEquals(EdiAreaColorResult.AMARILLO, rFb2.status)
            assertEquals(7, rFb2.fallbackSourceGroupNumber)

            // Retroceso G7: 1/2 = ROJO
            val sFb1 = recordFallbackResponses(s0Yes, 8, area, listOf(EdiItemResponse.YES, EdiItemResponse.NO))
            assertEquals(EdiAreaColorResult.ROJO, EdiAreaFallbackEngine.evaluateArea(8, area, sFb1.activeSession).status)

            // Retroceso G7: 0/2 = ROJO
            val sFb0 = recordFallbackResponses(s0Yes, 8, area, listOf(EdiItemResponse.NO, EdiItemResponse.NO))
            assertEquals(EdiAreaColorResult.ROJO, EdiAreaFallbackEngine.evaluateArea(8, area, sFb0.activeSession).status)
        }

        // CO en Grupo 8 siempre NOT_APPLICABLE
        assertEquals(
            EdiAreaColorResult.NOT_APPLICABLE,
            EdiAreaFallbackEngine.evaluateArea(
                8,
                EdiDevArea.CO,
                EdiPhase1SessionManager.createInitialState(8).activeSession
            ).status
        )
    }

    @Test
    fun `grupos 9 al 12 califican 3 o 2 SI verde, 1 SI amarillo, 0 SI retroceso a 3 items del grupo anterior`() {
        val applicableAreas = listOf(EdiDevArea.MG, EdiDevArea.MF, EdiDevArea.LE, EdiDevArea.SO)

        for (group in 9..12) {
            applicableAreas.forEach { area ->
                // 3 SÍ y 2 SÍ -> VERDE
                val s3Yes = recordAreaResponses(
                    EdiPhase1SessionManager.createInitialState(group),
                    group,
                    area,
                    listOf(EdiItemResponse.YES, EdiItemResponse.YES, EdiItemResponse.YES)
                )
                assertEquals(EdiAreaColorResult.VERDE, EdiAreaFallbackEngine.evaluateArea(group, area, s3Yes.activeSession).status)

                val s2Yes = recordAreaResponses(
                    EdiPhase1SessionManager.createInitialState(group),
                    group,
                    area,
                    listOf(EdiItemResponse.YES, EdiItemResponse.YES, EdiItemResponse.NO)
                )
                assertEquals(EdiAreaColorResult.VERDE, EdiAreaFallbackEngine.evaluateArea(group, area, s2Yes.activeSession).status)

                // 1 SÍ -> AMARILLO sin retroceso
                val s1Yes = recordAreaResponses(
                    EdiPhase1SessionManager.createInitialState(group),
                    group,
                    area,
                    listOf(EdiItemResponse.NO, EdiItemResponse.NO, EdiItemResponse.YES)
                )
                val r1Yes = EdiAreaFallbackEngine.evaluateArea(group, area, s1Yes.activeSession)
                assertEquals(EdiAreaColorResult.AMARILLO, r1Yes.status)
                assertFalse(r1Yes.fallbackRequired)

                // 0 SÍ -> retroceso a 3 reactivos de G(n-1)
                val s0Yes = recordAreaResponses(
                    EdiPhase1SessionManager.createInitialState(group),
                    group,
                    area,
                    listOf(EdiItemResponse.NO, EdiItemResponse.NO, EdiItemResponse.NO)
                )
                val fbSpecs = EdiAreaFallbackEngine.getFallbackItemsForArea(group, area)
                assertEquals(3, fbSpecs.size)
                assertTrue(fbSpecs.all { it.sourceGroupNumber == group - 1 })

                // Retroceso 3/3 SÍ -> AMARILLO
                val sFb3 = recordFallbackResponses(
                    s0Yes,
                    group,
                    area,
                    listOf(EdiItemResponse.YES, EdiItemResponse.YES, EdiItemResponse.YES)
                )
                assertEquals(EdiAreaColorResult.AMARILLO, EdiAreaFallbackEngine.evaluateArea(group, area, sFb3.activeSession).status)

                // Retroceso 2/3 SÍ -> AMARILLO
                val sFb2 = recordFallbackResponses(
                    s0Yes,
                    group,
                    area,
                    listOf(EdiItemResponse.YES, EdiItemResponse.NO, EdiItemResponse.YES)
                )
                assertEquals(EdiAreaColorResult.AMARILLO, EdiAreaFallbackEngine.evaluateArea(group, area, sFb2.activeSession).status)

                // Retroceso 1/3 SÍ -> ROJO
                val sFb1 = recordFallbackResponses(
                    s0Yes,
                    group,
                    area,
                    listOf(EdiItemResponse.NO, EdiItemResponse.YES, EdiItemResponse.NO)
                )
                assertEquals(EdiAreaColorResult.ROJO, EdiAreaFallbackEngine.evaluateArea(group, area, sFb1.activeSession).status)

                // Retroceso 0/3 SÍ -> ROJO
                val sFb0 = recordFallbackResponses(
                    s0Yes,
                    group,
                    area,
                    listOf(EdiItemResponse.NO, EdiItemResponse.NO, EdiItemResponse.NO)
                )
                assertEquals(EdiAreaColorResult.ROJO, EdiAreaFallbackEngine.evaluateArea(group, area, sFb0.activeSession).status)
            }

            // CO siempre NOT_APPLICABLE en G9..G12
            val coRes = EdiAreaFallbackEngine.evaluateArea(
                group,
                EdiDevArea.CO,
                EdiPhase1SessionManager.createInitialState(group).activeSession
            )
            assertEquals("CO debe permanecer NOT_APPLICABLE en G$group", EdiAreaColorResult.NOT_APPLICABLE, coRes.status)
        }
    }

    @Test
    fun `grupo 13 usa retroceso a grupo 12 en MG-MF-LE-SO y exclusivamente CO-R 13_1 a 13_3 en CO`() {
        // MG, MF, LE, SO retroceden al Grupo 12 (3 items)
        listOf(EdiDevArea.MG, EdiDevArea.MF, EdiDevArea.LE, EdiDevArea.SO).forEach { area ->
            val fbSpecs = EdiAreaFallbackEngine.getFallbackItemsForArea(13, area)
            assertEquals(3, fbSpecs.size)
            assertTrue(fbSpecs.all { it.sourceGroupNumber == 12 && !it.isSpecialCoRItem })

            val s0Yes = recordAreaResponses(
                EdiPhase1SessionManager.createInitialState(13),
                13,
                area,
                listOf(EdiItemResponse.NO, EdiItemResponse.NO, EdiItemResponse.NO)
            )
            val sFb2 = recordFallbackResponses(
                s0Yes,
                13,
                area,
                listOf(EdiItemResponse.YES, EdiItemResponse.YES, EdiItemResponse.NO)
            )
            val rFb2 = EdiAreaFallbackEngine.evaluateArea(13, area, sFb2.activeSession)
            assertEquals(EdiAreaColorResult.AMARILLO, rFb2.status)
            assertFalse(rFb2.usesSpecialCoRFallback)
            assertEquals(12, rFb2.fallbackSourceGroupNumber)

            val sFb1 = recordFallbackResponses(
                s0Yes,
                13,
                area,
                listOf(EdiItemResponse.YES, EdiItemResponse.NO, EdiItemResponse.NO)
            )
            assertEquals(EdiAreaColorResult.ROJO, EdiAreaFallbackEngine.evaluateArea(13, area, sFb1.activeSession).status)
        }

        // Excepción oficial de Grupo 13 CO: utiliza exclusivamente CO-R 13.1, CO-R 13.2 y CO-R 13.3
        val coFbSpecs = EdiAreaFallbackEngine.getFallbackItemsForArea(13, EdiDevArea.CO)
        assertEquals(3, coFbSpecs.size)
        assertTrue(coFbSpecs.all { it.isSpecialCoRItem })
        assertEquals(listOf("CO-R 13.1", "CO-R 13.2", "CO-R 13.3"), coFbSpecs.map { it.sourceItem.itemCode })
        assertEquals(listOf("G13_RETRO_COR_13_1", "G13_RETRO_COR_13_2", "G13_RETRO_COR_13_3"), coFbSpecs.map { it.fallbackUniqueId })

        // CO actual: 2 SÍ = VERDE; 1 SÍ = AMARILLO
        val sCoGreen = recordAreaResponses(
            EdiPhase1SessionManager.createInitialState(13),
            13,
            EdiDevArea.CO,
            listOf(EdiItemResponse.YES, EdiItemResponse.YES, EdiItemResponse.NO)
        )
        assertEquals(EdiAreaColorResult.VERDE, EdiAreaFallbackEngine.evaluateArea(13, EdiDevArea.CO, sCoGreen.activeSession).status)

        val sCoYellow = recordAreaResponses(
            EdiPhase1SessionManager.createInitialState(13),
            13,
            EdiDevArea.CO,
            listOf(EdiItemResponse.NO, EdiItemResponse.YES, EdiItemResponse.NO)
        )
        assertEquals(EdiAreaColorResult.AMARILLO, EdiAreaFallbackEngine.evaluateArea(13, EdiDevArea.CO, sCoYellow.activeSession).status)

        // CO actual 0 SÍ -> activa CO-R
        val sCoZero = recordAreaResponses(
            EdiPhase1SessionManager.createInitialState(13),
            13,
            EdiDevArea.CO,
            listOf(EdiItemResponse.NO, EdiItemResponse.NO, EdiItemResponse.NO)
        )
        val rCoPending = EdiAreaFallbackEngine.evaluateArea(13, EdiDevArea.CO, sCoZero.activeSession)
        assertTrue(rCoPending.fallbackRequired)
        assertTrue(rCoPending.usesSpecialCoRFallback)
        assertEquals(EdiAreaColorResult.INCOMPLETE, rCoPending.status)

        // CO-R 3/3 = AMARILLO; 2/3 = AMARILLO; 1/3 = ROJO; 0/3 = ROJO
        val sCoR3 = recordFallbackResponses(sCoZero, 13, EdiDevArea.CO, listOf(EdiItemResponse.YES, EdiItemResponse.YES, EdiItemResponse.YES))
        assertEquals(EdiAreaColorResult.AMARILLO, EdiAreaFallbackEngine.evaluateArea(13, EdiDevArea.CO, sCoR3.activeSession).status)

        val sCoR2 = recordFallbackResponses(sCoZero, 13, EdiDevArea.CO, listOf(EdiItemResponse.YES, EdiItemResponse.NO, EdiItemResponse.YES))
        val rCoR2 = EdiAreaFallbackEngine.evaluateArea(13, EdiDevArea.CO, sCoR2.activeSession)
        assertEquals(EdiAreaColorResult.AMARILLO, rCoR2.status)
        assertTrue(rCoR2.usesSpecialCoRFallback)

        val sCoR1 = recordFallbackResponses(sCoZero, 13, EdiDevArea.CO, listOf(EdiItemResponse.NO, EdiItemResponse.YES, EdiItemResponse.NO))
        assertEquals(EdiAreaColorResult.ROJO, EdiAreaFallbackEngine.evaluateArea(13, EdiDevArea.CO, sCoR1.activeSession).status)

        val sCoR0 = recordFallbackResponses(sCoZero, 13, EdiDevArea.CO, listOf(EdiItemResponse.NO, EdiItemResponse.NO, EdiItemResponse.NO))
        assertEquals(EdiAreaColorResult.ROJO, EdiAreaFallbackEngine.evaluateArea(13, EdiDevArea.CO, sCoR0.activeSession).status)
    }

    @Test
    fun `grupos 14 y 15 aplican 3 items y retroceso a 3 items del grupo anterior en las 5 areas incluyendo CO`() {
        for (group in listOf(14, 15)) {
            EdiDevArea.entries.forEach { area ->
                val fbSpecs = EdiAreaFallbackEngine.getFallbackItemsForArea(group, area)
                assertEquals("G$group ${area.code} debe tener 3 reactivos de retroceso", 3, fbSpecs.size)
                assertTrue(fbSpecs.all { it.sourceGroupNumber == group - 1 && !it.isSpecialCoRItem })

                // 2 SÍ actuales -> VERDE
                val s2 = recordAreaResponses(
                    EdiPhase1SessionManager.createInitialState(group),
                    group,
                    area,
                    listOf(EdiItemResponse.YES, EdiItemResponse.NO, EdiItemResponse.YES)
                )
                assertEquals(EdiAreaColorResult.VERDE, EdiAreaFallbackEngine.evaluateArea(group, area, s2.activeSession).status)

                // 1 SÍ actual -> AMARILLO sin retroceso
                val s1 = recordAreaResponses(
                    EdiPhase1SessionManager.createInitialState(group),
                    group,
                    area,
                    listOf(EdiItemResponse.NO, EdiItemResponse.NO, EdiItemResponse.YES)
                )
                val r1 = EdiAreaFallbackEngine.evaluateArea(group, area, s1.activeSession)
                assertEquals(EdiAreaColorResult.AMARILLO, r1.status)
                assertFalse(r1.fallbackRequired)

                // 0 SÍ actuales -> retroceso a 3 ítems de G(n-1): 2/3 AMARILLO, 1/3 ROJO
                val s0 = recordAreaResponses(
                    EdiPhase1SessionManager.createInitialState(group),
                    group,
                    area,
                    listOf(EdiItemResponse.NO, EdiItemResponse.NO, EdiItemResponse.NO)
                )
                val sFb2 = recordFallbackResponses(s0, group, area, listOf(EdiItemResponse.YES, EdiItemResponse.YES, EdiItemResponse.NO))
                assertEquals(EdiAreaColorResult.AMARILLO, EdiAreaFallbackEngine.evaluateArea(group, area, sFb2.activeSession).status)

                val sFb1 = recordFallbackResponses(s0, group, area, listOf(EdiItemResponse.NO, EdiItemResponse.YES, EdiItemResponse.NO))
                assertEquals(EdiAreaColorResult.ROJO, EdiAreaFallbackEngine.evaluateArea(group, area, sFb1.activeSession).status)
            }
        }
    }

    @Test
    fun `controles de seguridad no interpretan sin responder, material faltante ni discrepancia como NO`() {
        // 1. Grupo 9 LE (LE 9.3 requiere Lámina A en protocolo):
        //    Si LE 9.1 = NO, LE 9.2 = NO, LE 9.3 = NOT_EVALUATED_MISSING_MATERIAL
        //    NO debe activar retroceso ni emitir color definitivo; debe devolver INCOMPLETE.
        var sG9 = EdiPhase1SessionManager.createInitialState(9)
        sG9 = recordAreaResponses(
            sG9,
            9,
            EdiDevArea.LE,
            listOf(EdiItemResponse.NO, EdiItemResponse.NO, EdiItemResponse.NOT_EVALUATED_MISSING_MATERIAL)
        )
        val rG9MissingMat = EdiAreaFallbackEngine.evaluateArea(9, EdiDevArea.LE, sG9.activeSession)
        assertEquals(EdiAreaColorResult.INCOMPLETE, rG9MissingMat.status)
        assertFalse(rG9MissingMat.fallbackRequired)
        assertEquals(1, rG9MissingMat.currentPendingCount)

        // 2. Incluso si ya tiene 2 SÍ pero el 3ro es UNANSWERED, nunca debe emitir VERDE prematuro
        var sG9Partial = EdiPhase1SessionManager.createInitialState(9)
        sG9Partial = recordAreaResponses(
            sG9Partial,
            9,
            EdiDevArea.MG,
            listOf(EdiItemResponse.YES, EdiItemResponse.YES, EdiItemResponse.UNANSWERED)
        )
        val rG9Partial = EdiAreaFallbackEngine.evaluateArea(9, EdiDevArea.MG, sG9Partial.activeSession)
        assertEquals(EdiAreaColorResult.INCOMPLETE, rG9Partial.status)
        assertFalse(rG9Partial.status.isDefinitiveColor)

        // 3. Discrepancia humana pendiente en LE 13.3 (Grupo 13):
        //    LE 13.1 = NO, LE 13.2 = NO, LE 13.3 con entrevista = SÍ y observación = NO -> DISCREPANCY
        //    NO debe activar retroceso ni emitirse como NO automático.
        var sG13 = EdiPhase1SessionManager.createInitialState(13)
        val le131 = EdiOfficialCatalog.findItemByCode("LE 13.1")!!
        val le132 = EdiOfficialCatalog.findItemByCode("LE 13.2")!!
        val le133 = EdiOfficialCatalog.findItemByCode("LE 13.3")!!
        sG13 = EdiPhase1SessionManager.recordCurrentGroupItemResponse(sG13, le131.uniqueId, EdiItemResponse.NO)
        sG13 = EdiPhase1SessionManager.recordCurrentGroupItemResponse(sG13, le132.uniqueId, EdiItemResponse.NO)
        sG13 = EdiPhase1SessionManager.recordDualModalityEvidence(
            state = sG13,
            itemUniqueId = le133.uniqueId,
            observationEvidence = false,
            interviewEvidence = true
        )
        val rG13Discrepancy = EdiAreaFallbackEngine.evaluateArea(13, EdiDevArea.LE, sG13.activeSession)
        assertEquals(EdiAreaColorResult.INCOMPLETE, rG13Discrepancy.status)
        assertFalse(rG13Discrepancy.fallbackRequired)
        assertEquals(
            EdiItemResponse.DISCREPANCY_REQUIRES_HUMAN_RESOLUTION,
            rG13Discrepancy.currentItemResponses[le133.uniqueId]
        )

        // Cuando el profesional resuelve la discrepancia como NO -> recién se activa el retroceso a G12
        sG13 = EdiPhase1SessionManager.recordDualModalityEvidence(
            state = sG13,
            itemUniqueId = le133.uniqueId,
            observationEvidence = false,
            interviewEvidence = true,
            humanResolutionIfConflict = false,
            note = "Confirmado NO tras evaluación directa."
        )
        val rG13ResolvedNo = EdiAreaFallbackEngine.evaluateArea(13, EdiDevArea.LE, sG13.activeSession)
        assertTrue(rG13ResolvedNo.fallbackRequired)
        assertEquals(EdiAreaColorResult.INCOMPLETE, rG13ResolvedNo.status)

        // 4. Discrepancia dentro del retroceso (G14 LE retrocede a G13, que incluye LE 13.3):
        var sG14 = recordAreaResponses(
            EdiPhase1SessionManager.createInitialState(14),
            14,
            EdiDevArea.LE,
            listOf(EdiItemResponse.NO, EdiItemResponse.NO, EdiItemResponse.NO)
        )
        val fbLe14 = EdiAreaFallbackEngine.getFallbackItemsForArea(14, EdiDevArea.LE)
        sG14 = EdiAreaFallbackEngine.recordValidatedFallbackResponse(sG14, fbLe14[0].fallbackUniqueId, EdiItemResponse.YES)
        sG14 = EdiAreaFallbackEngine.recordValidatedFallbackResponse(sG14, fbLe14[1].fallbackUniqueId, EdiItemResponse.YES)
        sG14 = EdiAreaFallbackEngine.recordDualModalityFallbackEvidence(
            state = sG14,
            fallbackUniqueId = fbLe14[2].fallbackUniqueId,
            observationEvidence = true,
            interviewEvidence = false
        )
        val rG14FbDiscrepancy = EdiAreaFallbackEngine.evaluateArea(14, EdiDevArea.LE, sG14.activeSession)
        assertEquals(EdiAreaColorResult.INCOMPLETE, rG14FbDiscrepancy.status)
        assertEquals(1, rG14FbDiscrepancy.fallbackPendingCount)
    }

    @Test
    fun `modificar respuesta actual tras registrar retroceso ignora el retroceso sin borrar el historial y mantiene separacion estricta`() {
        var state = recordAreaResponses(
            EdiPhase1SessionManager.createInitialState(6),
            6,
            EdiDevArea.MG,
            listOf(EdiItemResponse.NO, EdiItemResponse.NO)
        )
        state = recordFallbackResponses(
            state,
            6,
            EdiDevArea.MG,
            listOf(EdiItemResponse.NO, EdiItemResponse.NO)
        )
        val rRojo = EdiAreaFallbackEngine.evaluateArea(6, EdiDevArea.MG, state.activeSession)
        assertEquals(EdiAreaColorResult.ROJO, rRojo.status)
        assertTrue(rRojo.fallbackRequired)
        assertEquals(2, state.activeSession.fallbackGroupResponses.size)
        // Verificar estricta disyunción de claves entre respuestas actuales y de retroceso
        assertTrue(state.activeSession.currentGroupResponses.keys.intersect(state.activeSession.fallbackGroupResponses.keys).isEmpty())

        // El evaluador corrige MG 6.1 de NO a YES -> ya no corresponde retroceso (1/2 SÍ = AMARILLO)
        val mg61 = EdiOfficialCatalog.findItemByCode("MG 6.1")!!
        state = EdiPhase1SessionManager.recordCurrentGroupItemResponse(state, mg61.uniqueId, EdiItemResponse.YES)
        val rUpdated = EdiAreaFallbackEngine.evaluateArea(6, EdiDevArea.MG, state.activeSession)

        assertEquals(EdiAreaColorResult.AMARILLO, rUpdated.status)
        assertFalse(rUpdated.fallbackRequired)
        assertTrue(rUpdated.fallbackIgnoredDueToCurrentChange)
        assertTrue(rUpdated.activeFallbackResponses.isEmpty())
        // El historial de retroceso se conserva intacto en la sesión sin borrarse automáticamente
        assertEquals(2, state.activeSession.fallbackGroupResponses.size)
        assertEquals(2, rUpdated.preservedHistoricalFallbackResponses.size)

        // Si se consulta otro grupo con la sesión del Grupo 6, jamás reutiliza respuestas ajenas
        val rOtherGroup = EdiAreaFallbackEngine.evaluateArea(5, EdiDevArea.MG, state.activeSession)
        assertEquals(EdiAreaColorResult.INCOMPLETE, rOtherGroup.status)
        assertEquals(0, rOtherGroup.currentYesCount)
        assertEquals(0, rOtherGroup.currentNoCount)
        assertEquals(2, rOtherGroup.currentPendingCount)
    }
}
