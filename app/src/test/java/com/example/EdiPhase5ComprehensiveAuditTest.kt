package com.example

import com.example.model.ClinicalAgeCalculator
import com.example.model.CredEdiUiState
import com.example.model.EdiAreaColorResult
import com.example.model.EdiAreaFallbackEngine
import com.example.model.EdiAxis
import com.example.model.EdiCatalogItem
import com.example.model.EdiClinicalModality
import com.example.model.EdiDevArea
import com.example.model.EdiDualEvidenceRecord
import com.example.model.EdiGlobalEvaluationResult
import com.example.model.EdiGlobalSemaphoreEngine
import com.example.model.EdiGlobalTrafficLight
import com.example.model.EdiGroupAutoSelectionStatus
import com.example.model.EdiGroupChangeResolutionAction
import com.example.model.EdiGroupSessionSnapshot
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
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.random.Random

/**
 * FASE 5 — AUDITORÍA INTEGRAL (MOTOR CLÍNICO, CATÁLOGO, RETROCESOS, SEMÁFORO Y EDAD).
 * Solo agrega pruebas y genera artefactos de auditoría en build/audit_phase5/.
 * Prohibido modificar código fuente de producción.
 */
class EdiPhase5ComprehensiveAuditTest {

    private data class CatalogCotejoRow(
        val sectionGroup: String,
        val axis: String,
        val area: String,
        val itemCode: String,
        val uniqueId: String,
        val formatQuestionText: String,
        val consultationQuestionText: String,
        val manualReference: String,
        val modality: String,
        val materialsProtocol: String,
        val plates: String,
        val matchStatus: String,
        val auditNotes: String
    )

    private fun ensureAuditDir(): File {
        val dir = File("build/audit_phase5")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun csvEscape(value: String): String {
        val clean = value.replace("\r\n", " ").replace("\n", " ")
        return "\"" + clean.replace("\"", "\"\"") + "\""
    }

    private fun mdEscape(value: String): String {
        return value.replace("\r\n", " ").replace("\n", " ").replace("|", "\\|")
    }

    @Test
    fun `01 audit catalog completeness consistency and generate cotejo matrix`() {
        val groups = EdiOfficialCatalog.groups
        assertEquals("Deben existir exactamente 15 grupos oficiales", 15, groups.size)
        assertEquals((1..15).toList(), groups.map { it.groupNumber })

        // Verificar límites etarios contiguos de 1 a 71 meses sin huecos ni solapamientos
        var expectedStartMonth = 1
        groups.forEach { spec ->
            assertEquals("G${spec.groupNumber} debe iniciar en mes $expectedStartMonth", expectedStartMonth, spec.minMonthsInclusive)
            assertTrue("G${spec.groupNumber} max >= min", spec.maxMonthsInclusive >= spec.minMonthsInclusive)
            expectedStartMonth = spec.maxMonthsInclusive + 1
        }
        assertEquals("El rango de 15 grupos debe cubrir exactamente hasta 71m 29d (< 72m)", 72, expectedStartMonth)

        // Verificar totales estructurales por grupo
        assertEquals(7, EdiOfficialCatalog.frbItems.size)
        assertEquals(3, EdiOfficialCatalog.allEnItems.size)
        assertEquals(3, EdiOfficialCatalog.specialGroup13CoRFallbackItems.size)

        val rows = mutableListOf<CatalogCotejoRow>()
        val seenIds = mutableSetOf<String>()
        val seenCodes = mutableSetOf<String>()
        val detectedObservations = mutableListOf<String>()

        fun inspectItem(
            sectionGroupLabel: String,
            item: EdiCatalogItem,
            manualRef: String
        ) {
            assertTrue("uniqueId duplicado: ${item.uniqueId}", seenIds.add(item.uniqueId))
            assertTrue("itemCode duplicado: ${item.itemCode}", seenCodes.add(item.itemCode))
            assertTrue("formatQuestionText vacío en ${item.itemCode}", item.formatQuestionText.isNotBlank())
            assertTrue("consultationQuestionText vacío en ${item.itemCode}", item.consultationQuestionText.isNotBlank())
            assertTrue("clinicalInstruction vacío en ${item.itemCode}", item.clinicalInstruction.isNotBlank())

            val itemFindings = mutableListOf<String>()
            val fmtHasStars = item.formatQuestionText.trimStart().startsWith("**")
            val fmtHasObsTag = item.formatQuestionText.contains("(Observe)", ignoreCase = true)
            val consHasDoubleStars = item.consultationQuestionText.trimStart().startsWith("**")
            val consHasSingleStarOnly = !consHasDoubleStars && item.consultationQuestionText.trimStart().startsWith("*")

            if (item.hasObservationAsterisks && (!fmtHasStars && !fmtHasObsTag)) {
                itemFindings.add("obs=true sin ** ni (Observe) en formato")
            }
            if (!item.hasObservationAsterisks && (fmtHasStars || fmtHasObsTag)) {
                itemFindings.add("obs=false pero formato contiene ** o (Observe) (${item.itemCode})")
            }
            if (item.hasObservationAsterisks && !consHasDoubleStars) {
                itemFindings.add("obs=true pero texto de consulta no inicia con ** (${item.itemCode})")
            }
            if (!item.hasObservationAsterisks && (consHasDoubleStars || consHasSingleStarOnly)) {
                itemFindings.add("obs=false pero texto de consulta inicia con asterisco(s) (${item.itemCode})")
            }
            if (item.consultationQuestionText.contains("¿Cuándo ", ignoreCase = false)) {
                itemFindings.add("tilde interrogativa '¿Cuándo' en oración condicional (${item.itemCode})")
            }
            if (item.formatQuestionText.contains("¿Cuándo ", ignoreCase = false)) {
                itemFindings.add("tilde interrogativa '¿Cuándo' en formato (${item.itemCode})")
            }

            // Verificar discrepancia intencional documentada en protocolo interno vs formato
            val isDocumentedProtocolControl = item.itemCode in setOf("ALE 9.1", "ALE 12.3", "LE 13.3", "LE 14.3", "ALE 14.1")
            val status = when {
                isDocumentedProtocolControl && itemFindings.isEmpty() ->
                    "CONFORME CON CONTROL INTERNO DOCUMENTADO"
                isDocumentedProtocolControl ->
                    "CONFORME CON OBSERVACIÓN TEXTUAL (${itemFindings.joinToString("; ")})"
                itemFindings.isEmpty() ->
                    "COINCIDE CON ESPECIFICACIÓN INTERNA (PENDIENTE COTEJO PDF MINSA EXTERNO)"
                else ->
                    "OBSERVACIÓN TEXTUAL/METADATO (${itemFindings.joinToString("; ")})"
            }
            if (itemFindings.isNotEmpty()) {
                detectedObservations.add("${item.itemCode}: ${itemFindings.joinToString("; ")}")
            }

            rows.add(
                CatalogCotejoRow(
                    sectionGroup = sectionGroupLabel,
                    axis = item.axis.code,
                    area = item.area?.code ?: "-",
                    itemCode = item.itemCode,
                    uniqueId = item.uniqueId,
                    formatQuestionText = item.formatQuestionText,
                    consultationQuestionText = item.consultationQuestionText,
                    manualReference = manualRef,
                    modality = item.modality.name,
                    materialsProtocol = if (item.protocolRequiredMaterials.isEmpty()) "Ninguno" else item.protocolRequiredMaterials.joinToString("; "),
                    plates = if (item.protocolRequiredPlates.isEmpty()) "-" else item.protocolRequiredPlates.joinToString(", ") { it.name },
                    matchStatus = status,
                    auditNotes = if (itemFindings.isEmpty()) "OK interno" else itemFindings.joinToString("; ")
                )
            )
        }

        // Eje 1: FRB
        EdiOfficialCatalog.frbItems.forEachIndexed { idx, item ->
            assertEquals("FRB ${idx + 1}", item.itemCode)
            inspectItem("Transversal (G1-G15)", item, "Pág. 12 (FRB 1..7)")
        }

        // Eje 4: EN
        EdiOfficialCatalog.allEnItems.forEachIndexed { idx, item ->
            assertEquals("EN 1.${idx + 1}", item.itemCode)
            val note = if (item.itemCode == "EN 1.3") "Pág. 13 (No aplica en G15, págs. 47 y 111)" else "Pág. 13"
            inspectItem("Transversal (G1-G15)", item, note)
        }

        // Grupos 1..15
        groups.forEach { spec ->
            val g = spec.groupNumber
            val fmtRange = "Formato pp. ${spec.formatPages.first}-${spec.formatPages.last}"
            val consRange = "Consulta pp. ${spec.consultationPages.first}-${spec.consultationPages.last}"

            val expectedPerArea = if (g <= 7) 2 else 3
            assertEquals(expectedPerArea, spec.itemsPerActiveArea)
            assertEquals(g in 1..4, spec.frbAndAleModifyGlobalTrafficLight)
            assertEquals(g >= 13, spec.conocimientoApplies)
            assertEquals(g != 1, spec.hasAlarmSignals)
            assertEquals(g != 15, spec.neurologicalEn13Applies)

            // Áreas del Desarrollo
            val expectedAreas = if (g <= 12) listOf(EdiDevArea.MG, EdiDevArea.MF, EdiDevArea.LE, EdiDevArea.SO) else EdiDevArea.entries
            expectedAreas.forEach { area ->
                val list = spec.areaItems[area].orEmpty()
                assertEquals("G$g ${area.code} cantidad de reactivos", expectedPerArea, list.size)
                list.forEachIndexed { i, item ->
                    assertEquals("${area.code} $g.${i + 1}", item.itemCode)
                    inspectItem("Grupo $g (${spec.shortIntervalLabel})", item, "$fmtRange / $consRange")
                }
            }
            if (g <= 12) {
                assertTrue("CO debe estar vacío en G$g", spec.areaItems[EdiDevArea.CO].orEmpty().isEmpty())
            }

            // Verificar duplicados dentro del mismo grupo/eje
            val allGroupItems = spec.areaItems.values.flatten() + spec.aleItems + spec.alaItems
            val allGroupConsultTexts = allGroupItems.map { it.consultationQuestionText }
            assertEquals("No debe haber textos duplicados dentro de G$g", allGroupConsultTexts.size, allGroupConsultTexts.toSet().size)

            // ALE
            assertTrue("G$g debe tener ALE", spec.aleItems.isNotEmpty())
            if (g == 14) {
                assertEquals("G14 debe tener exactamente 8 ALE", 8, spec.aleItems.size)
            }
            spec.aleItems.forEachIndexed { i, item ->
                assertEquals("ALE $g.${i + 1}", item.itemCode)
                inspectItem("Grupo $g (${spec.shortIntervalLabel})", item, "$fmtRange / $consRange")
            }

            // ALA
            if (g == 1) {
                assertTrue("G1 no tiene ALA", spec.alaItems.isEmpty())
            } else {
                assertTrue("G$g debe tener ALA", spec.alaItems.isNotEmpty())
                spec.alaItems.forEachIndexed { i, item ->
                    assertEquals("ALA $g.${i + 1}", item.itemCode)
                    inspectItem("Grupo $g (${spec.shortIntervalLabel})", item, "$fmtRange / $consRange")
                }
            }
        }

        // Reactivos especiales CO-R 13.1..13.3
        EdiOfficialCatalog.specialGroup13CoRFallbackItems.forEachIndexed { idx, item ->
            assertEquals("CO-R 13.${idx + 1}", item.itemCode)
            inspectItem("Grupo 13 Retroceso Especial CO-R", item, "Págs. 43 y 102")
        }

        // Controles específicos de materiales/láminas y modalidades internas
        val ale91 = EdiOfficialCatalog.findItemByUniqueId("G9_ALE_9_1")!!
        assertTrue(ale91.generalHeaderMaterials.any { it.contains("Lámina A", ignoreCase = true) })
        assertTrue("ALE 9.1 NO debe exigir Lámina A ni material directo en protocolo", ale91.protocolRequiredPlates.isEmpty())
        assertFalse(ale91.requiresMaterialInProtocol)

        val le93 = EdiOfficialCatalog.findItemByUniqueId("G9_LE_9_3")!!
        assertEquals(setOf(EdiOfficialPlate.LAMINA_A), le93.protocolRequiredPlates)
        assertTrue(le93.requiresMaterialInProtocol)

        val le123 = EdiOfficialCatalog.findItemByUniqueId("G12_LE_12_3")!!
        assertEquals(setOf(EdiOfficialPlate.LAMINA_B, EdiOfficialPlate.LAMINA_C, EdiOfficialPlate.LAMINA_D), le123.protocolRequiredPlates)

        val ale141 = EdiOfficialCatalog.findItemByUniqueId("G14_ALE_14_1")!!
        assertEquals(setOf(EdiOfficialPlate.LAMINA_B, EdiOfficialPlate.LAMINA_C, EdiOfficialPlate.LAMINA_D), ale141.protocolRequiredPlates)

        assertEquals(EdiClinicalModality.DUAL_OBSERVATION_AND_INTERVIEW_ALE_12_3, EdiOfficialCatalog.findItemByUniqueId("G12_ALE_12_3")!!.modality)
        assertEquals(EdiClinicalModality.INTERVIEW_PRIMARY_WITH_OBSERVATION_COMPLEMENT, EdiOfficialCatalog.findItemByUniqueId("G13_LE_13_3")!!.modality)
        assertEquals(EdiClinicalModality.INTERVIEW_PRIMARY_WITH_OBSERVATION_COMPLEMENT, EdiOfficialCatalog.findItemByUniqueId("G14_LE_14_3")!!.modality)

        // Exportar Matriz de Cotejo en CSV y Markdown a build/audit_phase5/
        val auditDir = ensureAuditDir()
        val csvFile = File(auditDir, "01_matriz_cotejo_catalogo_edi_2026.csv")
        val csvSb = StringBuilder()
        csvSb.appendLine("Grupo_Seccion,Eje,Area,Codigo_Reactivo,UniqueId,Texto_Formato,Texto_Consulta,Referencia_Manual,Modalidad,Materiales_Protocolo,Laminas,Estado_Coincidencia,Notas_Auditoria")
        rows.forEach { r ->
            csvSb.appendLine(
                listOf(
                    r.sectionGroup, r.axis, r.area, r.itemCode, r.uniqueId,
                    r.formatQuestionText, r.consultationQuestionText, r.manualReference,
                    r.modality, r.materialsProtocol, r.plates, r.matchStatus, r.auditNotes
                ).joinToString(",") { csvEscape(it) }
            )
        }
        csvFile.writeText(csvSb.toString(), Charsets.UTF_8)

        val mdFile = File(auditDir, "01_matriz_cotejo_catalogo_edi_2026.md")
        val mdSb = StringBuilder()
        mdSb.appendLine("# Matriz de Cotejo del Catálogo Clínico EDI Peruano 2026 (Auditoría Fase 5)")
        mdSb.appendLine()
        mdSb.appendLine("- **Total de reactivos auditados**: ${rows.size} (7 FRB + 3 EN + 3 CO-R + todos los reactivos de G1–G15)")
        mdSb.appendLine("- **Observaciones textuales/metadato detectadas automáticamente**: ${detectedObservations.size}")
        mdSb.appendLine("- **Alcance**: Verificación de consistencia interna frente al plan maestro aprobado y protocolos internos documentados. El cotejo palabra por palabra frente al PDF físico original del Manual MINSA/UNICEF 2026 queda marcado como **pendiente de revisión clínica externa** al no encontrarse dicho documento en el entorno.")
        mdSb.appendLine()
        if (detectedObservations.isNotEmpty()) {
            mdSb.appendLine("## Discrepancias / Observaciones Textuales Detectadas en el Catálogo")
            detectedObservations.forEach { mdSb.appendLine("- $it") }
            mdSb.appendLine()
        }
        mdSb.appendLine("## Matriz Completa de Reactivos (${rows.size} ítems)")
        mdSb.appendLine("| Grupo/Sección | Eje | Área | Código | Texto del Catálogo (Consulta) | Referencia del Manual | Modalidad | Materiales / Láminas | Estado de Coincidencia |")
        mdSb.appendLine("|---|---|---|---|---|---|---|---|---|")
        rows.forEach { r ->
            val matCol = if (r.plates == "-") r.materialsProtocol else "${r.materialsProtocol} [${r.plates}]"
            mdSb.appendLine(
                "| ${mdEscape(r.sectionGroup)} | ${r.axis} | ${r.area} | ${r.itemCode} | ${mdEscape(r.consultationQuestionText)} | ${mdEscape(r.manualReference)} | ${r.modality} | ${mdEscape(matCol)} | ${mdEscape(r.matchStatus)} |"
            )
        }
        mdFile.writeText(mdSb.toString(), Charsets.UTF_8)

        assertTrue("CSV de matriz debe haberse generado", csvFile.exists() && csvFile.length() > 10_000L)
        assertTrue("Markdown de matriz debe haberse generado", mdFile.exists() && mdFile.length() > 10_000L)
    }

    @Test
    fun `02 audit exhaustive fallback engine across all 15 groups all areas all yes no counts and incomplete states`() {
        for (g in 1..15) {
            val spec = EdiOfficialCatalog.getGroup(g)

            // 1. Verificar CO en G1..G12 siempre NOT_APPLICABLE aunque haya respuestas contaminantes
            if (g <= 12) {
                val dirtySession = EdiGroupSessionSnapshot(
                    groupNumber = g,
                    currentGroupResponses = mapOf("G13_CO_13_1" to EdiItemResponse.NO),
                    fallbackGroupResponses = mapOf("G13_RETRO_COR_13_1" to EdiItemResponse.NO)
                )
                val coRes = EdiAreaFallbackEngine.evaluateArea(g, EdiDevArea.CO, dirtySession)
                assertEquals("G$g CO debe ser NOT_APPLICABLE", EdiAreaColorResult.NOT_APPLICABLE, coRes.status)
                assertFalse(coRes.appliesToGroup)
                assertFalse(coRes.fallbackRequired)
            }

            // 2. Probar cada área aplicable con todas las combinaciones de SÍ/NO + incompletos
            val applicableAreas = if (g <= 12) listOf(EdiDevArea.MG, EdiDevArea.MF, EdiDevArea.LE, EdiDevArea.SO) else EdiDevArea.entries
            applicableAreas.forEach { area ->
                val items = spec.areaItems[area]!!
                val n = items.size
                assertEquals(if (g <= 7) 2 else 3, n)

                // A) Estados incompletos sin 0/N explícito (UNANSWERED, MISSING_MATERIAL, DISCREPANCY)
                for (incompleteIdx in 0 until n) {
                    val map = items.mapIndexed { idx, item ->
                        val r = if (idx == incompleteIdx) EdiItemResponse.UNANSWERED else EdiItemResponse.NO
                        item.uniqueId to r
                    }.toMap()
                    val resUnanswered = EdiAreaFallbackEngine.evaluateArea(
                        g,
                        area,
                        EdiGroupSessionSnapshot(groupNumber = g, currentGroupResponses = map)
                    )
                    assertEquals("G$g ${area.code} con UNANSWERED debe ser INCOMPLETE", EdiAreaColorResult.INCOMPLETE, resUnanswered.status)
                    assertFalse("UNANSWERED jamás debe activar retroceso", resUnanswered.fallbackRequired)

                    // Si un ítem del área requiere material, probar NOT_EVALUATED_MISSING_MATERIAL
                    if (items[incompleteIdx].requiresMaterialInProtocol) {
                        val matMap = items.mapIndexed { idx, item ->
                            item.uniqueId to if (idx == incompleteIdx) EdiItemResponse.NOT_EVALUATED_MISSING_MATERIAL else EdiItemResponse.NO
                        }.toMap()
                        val resMat = EdiAreaFallbackEngine.evaluateArea(
                            g,
                            area,
                            EdiGroupSessionSnapshot(groupNumber = g, currentGroupResponses = matMap)
                        )
                        assertEquals("Falta material en G$g ${area.code} debe ser INCOMPLETE", EdiAreaColorResult.INCOMPLETE, resMat.status)
                        assertFalse("Falta material jamás debe activar retroceso ni interpretarse como NO", resMat.fallbackRequired)
                    }
                }

                // B) Todos los conteos posibles de SÍ (1..n) sin retroceso
                for (yesCount in 1..n) {
                    val map = items.mapIndexed { idx, item ->
                        item.uniqueId to if (idx < yesCount) EdiItemResponse.YES else EdiItemResponse.NO
                    }.toMap()
                    val res = EdiAreaFallbackEngine.evaluateArea(
                        g,
                        area,
                        EdiGroupSessionSnapshot(groupNumber = g, currentGroupResponses = map)
                    )
                    assertFalse("Con $yesCount SÍ en G$g ${area.code} no debe requerir retroceso", res.fallbackRequired)
                    val expectedColor = when {
                        g == 1 -> if (yesCount == 2) EdiAreaColorResult.VERDE else EdiAreaColorResult.ROJO
                        g in 2..7 -> if (yesCount == 2) EdiAreaColorResult.VERDE else EdiAreaColorResult.AMARILLO
                        else -> if (yesCount >= 2) EdiAreaColorResult.VERDE else EdiAreaColorResult.AMARILLO
                    }
                    assertEquals("G$g ${area.code} con $yesCount/$n SÍ", expectedColor, res.status)
                }

                // C) 0/N SÍ (todos explícitamente NO)
                val allNoCurrentMap = items.associate { it.uniqueId to EdiItemResponse.NO }
                if (g == 1) {
                    val resG1 = EdiAreaFallbackEngine.evaluateArea(
                        1,
                        area,
                        EdiGroupSessionSnapshot(groupNumber = 1, currentGroupResponses = allNoCurrentMap)
                    )
                    assertFalse("G1 nunca aplica retroceso", resG1.fallbackRequired)
                    assertEquals(EdiAreaColorResult.ROJO, resG1.status)
                } else {
                    val fbSpecs = EdiAreaFallbackEngine.getFallbackItemsForArea(g, area)
                    val expectedFbCount = when {
                        g in 2..7 -> 2
                        g == 8 -> 2 // Excepción G8 -> G7
                        else -> 3
                    }
                    assertEquals("Cantidad de retroceso en G$g ${area.code}", expectedFbCount, fbSpecs.size)
                    if (g == 8) {
                        assertEquals(7, fbSpecs.first().sourceGroupNumber)
                    }
                    if (g == 13 && area == EdiDevArea.CO) {
                        assertEquals(listOf("CO-R 13.1", "CO-R 13.2", "CO-R 13.3"), fbSpecs.map { it.sourceItem.itemCode })
                    }

                    // Retroceso sin responder -> INCOMPLETE
                    val resFbMissing = EdiAreaFallbackEngine.evaluateArea(
                        g,
                        area,
                        EdiGroupSessionSnapshot(groupNumber = g, currentGroupResponses = allNoCurrentMap)
                    )
                    assertTrue(resFbMissing.fallbackRequired)
                    assertEquals(EdiAreaColorResult.INCOMPLETE, resFbMissing.status)

                    // Retroceso parcialmente respondido -> INCOMPLETE
                    val partialFbMap = mapOf(fbSpecs.first().fallbackUniqueId to EdiItemResponse.NO)
                    val resFbPartial = EdiAreaFallbackEngine.evaluateArea(
                        g,
                        area,
                        EdiGroupSessionSnapshot(
                            groupNumber = g,
                            currentGroupResponses = allNoCurrentMap,
                            fallbackGroupResponses = partialFbMap
                        )
                    )
                    assertEquals(EdiAreaColorResult.INCOMPLETE, resFbPartial.status)

                    // Probar todos los conteos posibles de SÍ en retroceso (0..expectedFbCount)
                    for (fbYes in 0..expectedFbCount) {
                        val fbMap = fbSpecs.mapIndexed { idx, specFb ->
                            specFb.fallbackUniqueId to if (idx < fbYes) EdiItemResponse.YES else EdiItemResponse.NO
                        }.toMap()
                        val resFb = EdiAreaFallbackEngine.evaluateArea(
                            g,
                            area,
                            EdiGroupSessionSnapshot(
                                groupNumber = g,
                                currentGroupResponses = allNoCurrentMap,
                                fallbackGroupResponses = fbMap
                            )
                        )
                        assertTrue(resFb.fallbackRequired)
                        val expectedFbColor = if (expectedFbCount == 2) {
                            if (fbYes == 2) EdiAreaColorResult.AMARILLO else EdiAreaColorResult.ROJO
                        } else {
                            if (fbYes >= 2) EdiAreaColorResult.AMARILLO else EdiAreaColorResult.ROJO
                        }
                        assertEquals(
                            "Retroceso G$g ${area.code} con $fbYes/$expectedFbCount SÍ",
                            expectedFbColor,
                            resFb.status
                        )

                        // Si se corrige un reactivo actual a SÍ, debe ignorar retroceso pero conservar historial
                        val correctedMap = allNoCurrentMap + (items.first().uniqueId to EdiItemResponse.YES)
                        val resCorrected = EdiAreaFallbackEngine.evaluateArea(
                            g,
                            area,
                            EdiGroupSessionSnapshot(
                                groupNumber = g,
                                currentGroupResponses = correctedMap,
                                fallbackGroupResponses = fbMap
                            )
                        )
                        assertFalse(resCorrected.fallbackRequired)
                        assertTrue(resCorrected.fallbackIgnoredDueToCurrentChange)
                        assertEquals(expectedFbCount, resCorrected.preservedHistoricalFallbackResponses.size)
                    }
                }
            }

            // 3. No contaminación desde otro grupo
            val otherGroup = if (g == 1) 2 else 1
            val wrongSpecItems = spec.areaItems.values.flatten() + spec.aleItems + spec.alaItems
            val wrongSession = EdiGroupSessionSnapshot(
                groupNumber = otherGroup,
                currentGroupResponses = wrongSpecItems.associate { it.uniqueId to EdiItemResponse.YES }
            )
            val crossRes = EdiAreaFallbackEngine.evaluateArea(g, EdiDevArea.MG, wrongSession)
            assertEquals("Sesión de G$otherGroup jamás debe contaminar G$g", EdiAreaColorResult.INCOMPLETE, crossRes.status)
            assertEquals(0, crossRes.currentYesCount)
        }
    }

    /**
     * Oráculo independiente basado en las reglas del Manual EDI 2026 para validar el motor global
     * sin compartir implementación con EdiGlobalSemaphoreEngine.
     */
    private fun independentGlobalSemaphoreOracle(
        groupNumber: Int,
        areaColors: Map<EdiDevArea, EdiAreaColorResult>,
        frbYesCount: Int,
        aleYesCount: Int,
        enYesCount: Int,
        alaYesCount: Int,
        hasBlockingIncompleteOrDiscrepancyOrAgeBlock: Boolean
    ): EdiGlobalTrafficLight {
        if (hasBlockingIncompleteOrDiscrepancyOrAgeBlock) return EdiGlobalTrafficLight.INCOMPLETE

        val redAreasCount = areaColors.values.count { it == EdiAreaColorResult.ROJO }
        val yellowAreasCount = areaColors.values.count { it == EdiAreaColorResult.AMARILLO }

        if (groupNumber == 1) {
            if (redAreasCount >= 1 || enYesCount >= 1) return EdiGlobalTrafficLight.ROJO
            if (frbYesCount >= 2 || aleYesCount >= 2 || (frbYesCount >= 1 && aleYesCount >= 1)) {
                return EdiGlobalTrafficLight.AMARILLO
            }
            return EdiGlobalTrafficLight.VERDE
        }

        if (groupNumber in 2..4) {
            if (
                redAreasCount >= 1 ||
                alaYesCount >= 1 ||
                enYesCount >= 1 ||
                yellowAreasCount >= 2 ||
                (yellowAreasCount >= 1 && frbYesCount >= 1) ||
                (yellowAreasCount >= 1 && aleYesCount >= 1)
            ) {
                return EdiGlobalTrafficLight.ROJO
            }
            if (
                yellowAreasCount == 1 ||
                frbYesCount >= 2 ||
                aleYesCount >= 2 ||
                (frbYesCount >= 1 && aleYesCount >= 1)
            ) {
                return EdiGlobalTrafficLight.AMARILLO
            }
            return EdiGlobalTrafficLight.VERDE
        }

        // Grupos 5..15: FRB y ALE NO modifican el semáforo global
        if (redAreasCount >= 1 || alaYesCount >= 1 || enYesCount >= 1) {
            return EdiGlobalTrafficLight.ROJO
        }
        if (yellowAreasCount >= 1) {
            return EdiGlobalTrafficLight.AMARILLO
        }
        return EdiGlobalTrafficLight.VERDE
    }

    private fun buildCleanGreenSession(groupNumber: Int, includeFrbAndAle: Boolean = true): EdiGroupSessionSnapshot {
        val spec = EdiOfficialCatalog.getGroup(groupNumber)
        val curr = linkedMapOf<String, EdiItemResponse>()
        spec.areaItems.values.flatten().forEach { curr[it.uniqueId] = EdiItemResponse.YES }
        spec.alaItems.forEach { curr[it.uniqueId] = EdiItemResponse.NO }
        if (includeFrbAndAle) {
            spec.aleItems.forEach { curr[it.uniqueId] = EdiItemResponse.NO }
        }
        val frb = if (includeFrbAndAle) {
            EdiOfficialCatalog.frbItems.associate { it.uniqueId to EdiItemResponse.NO }
        } else emptyMap()
        val en = EdiOfficialCatalog.getApplicableEnItems(groupNumber).associate { it.uniqueId to EdiItemResponse.NO }
        return EdiGroupSessionSnapshot(
            groupNumber = groupNumber,
            currentGroupResponses = curr,
            frbResponses = frb,
            enResponses = en
        )
    }

    @Test
    fun `03 audit global semaphore against independent oracle across all 15 groups and sensitivity checks`() {
        val rng = Random(20260505)

        // 1. Pruebas aleatorias controladas comparadas contra oráculo independiente (150 casos en G1..G15)
        for (g in 1..15) {
            val spec = EdiOfficialCatalog.getGroup(g)
            val applicableAreas = if (g <= 12) listOf(EdiDevArea.MG, EdiDevArea.MF, EdiDevArea.LE, EdiDevArea.SO) else EdiDevArea.entries
            val enItems = EdiOfficialCatalog.getApplicableEnItems(g)

            repeat(10) {
                var session = buildCleanGreenSession(g, includeFrbAndAle = true)
                val curr = session.currentGroupResponses.toMutableMap()
                val fb = mutableMapOf<String, EdiItemResponse>()

                // Asignar colores aleatorios a las áreas
                applicableAreas.forEach { area ->
                    val items = spec.areaItems[area]!!
                    when (rng.nextInt(4)) {
                        0 -> items.forEach { curr[it.uniqueId] = EdiItemResponse.YES } // VERDE
                        1 -> {
                            curr[items[0].uniqueId] = EdiItemResponse.YES
                            for (i in 1 until items.size) curr[items[i].uniqueId] = EdiItemResponse.NO // AMARILLO en G2-15, ROJO en G1
                        }
                        2 -> {
                            items.forEach { curr[it.uniqueId] = EdiItemResponse.NO }
                            if (g >= 2) {
                                val fbSpecs = EdiAreaFallbackEngine.getFallbackItemsForArea(g, area)
                                fbSpecs.forEach { fb[it.fallbackUniqueId] = EdiItemResponse.YES } // AMARILLO vía retroceso
                            }
                        }
                        3 -> {
                            items.forEach { curr[it.uniqueId] = EdiItemResponse.NO }
                            if (g >= 2) {
                                val fbSpecs = EdiAreaFallbackEngine.getFallbackItemsForArea(g, area)
                                fbSpecs.forEach { fb[it.fallbackUniqueId] = EdiItemResponse.NO } // ROJO vía retroceso
                            }
                        }
                    }
                }

                // FRB aleatorios (o pendientes en G5-15)
                val frbMap = if (g >= 5 && rng.nextBoolean()) {
                    emptyMap() // Opcionales sin responder en G5-G15
                } else {
                    EdiOfficialCatalog.frbItems.associate {
                        it.uniqueId to if (rng.nextInt(5) == 0) EdiItemResponse.YES else EdiItemResponse.NO
                    }
                }

                // ALE aleatorios (o pendientes/missing_material en G5-15)
                if (g >= 5 && rng.nextBoolean()) {
                    spec.aleItems.forEachIndexed { idx, item ->
                        curr[item.uniqueId] = when {
                            idx == 0 && item.requiresMaterialInProtocol -> EdiItemResponse.NOT_EVALUATED_MISSING_MATERIAL
                            idx == 1 -> EdiItemResponse.UNANSWERED
                            rng.nextInt(4) == 0 -> EdiItemResponse.YES
                            else -> EdiItemResponse.NO
                        }
                    }
                } else {
                    spec.aleItems.forEach { item ->
                        curr[item.uniqueId] = if (rng.nextInt(5) == 0) EdiItemResponse.YES else EdiItemResponse.NO
                    }
                }

                val enMap = enItems.associate {
                    it.uniqueId to if (rng.nextInt(8) == 0) EdiItemResponse.YES else EdiItemResponse.NO
                }
                spec.alaItems.forEach { item ->
                    curr[item.uniqueId] = if (rng.nextInt(8) == 0) EdiItemResponse.YES else EdiItemResponse.NO
                }

                session = session.copy(
                    currentGroupResponses = curr,
                    fallbackGroupResponses = fb,
                    frbResponses = frbMap,
                    enResponses = enMap
                )

                val actual = EdiGlobalSemaphoreEngine.evaluateGlobalSemaphore(g, session)
                val oracleAreas = actual.axis3AreasSummary.areaResults.mapValues { it.value.status }
                val expected = independentGlobalSemaphoreOracle(
                    groupNumber = g,
                    areaColors = oracleAreas,
                    frbYesCount = frbMap.values.count { it == EdiItemResponse.YES },
                    aleYesCount = spec.aleItems.count { curr[it.uniqueId] == EdiItemResponse.YES },
                    enYesCount = enMap.values.count { it == EdiItemResponse.YES },
                    alaYesCount = spec.alaItems.count { curr[it.uniqueId] == EdiItemResponse.YES },
                    hasBlockingIncompleteOrDiscrepancyOrAgeBlock = false
                )

                assertEquals(
                    "Oráculo vs EdiGlobalSemaphoreEngine falló en G$g (causes=${actual.causes.map { it.type }})",
                    expected,
                    actual.trafficLight
                )
            }
        }

        // 2. Sensibilidad a un solo cambio de reactivo (flip de 1 respuesta cambia el dictamen)
        // Caso G1: VERDE -> AMARILLO (2do FRB) -> ROJO (EN 1.1)
        val g1Green = buildCleanGreenSession(1).copy(
            frbResponses = EdiOfficialCatalog.frbItems.associate {
                it.uniqueId to if (it.uniqueId == "FRB_1") EdiItemResponse.YES else EdiItemResponse.NO
            }
        )
        assertEquals(EdiGlobalTrafficLight.VERDE, EdiGlobalSemaphoreEngine.evaluateGlobalSemaphore(1, g1Green).trafficLight)
        val g1OneAleMore = g1Green.copy(currentGroupResponses = g1Green.currentGroupResponses + ("G1_ALE_1_1" to EdiItemResponse.YES))
        assertEquals(EdiGlobalTrafficLight.AMARILLO, EdiGlobalSemaphoreEngine.evaluateGlobalSemaphore(1, g1OneAleMore).trafficLight)
        val g1OneEnFlip = g1OneAleMore.copy(enResponses = g1OneAleMore.enResponses + ("EN_1_1" to EdiItemResponse.YES))
        assertEquals(EdiGlobalTrafficLight.ROJO, EdiGlobalSemaphoreEngine.evaluateGlobalSemaphore(1, g1OneEnFlip).trafficLight)
        val g1IncompleteFlip = g1Green.copy(currentGroupResponses = g1Green.currentGroupResponses + ("G1_MG_1_1" to EdiItemResponse.UNANSWERED))
        assertEquals(EdiGlobalTrafficLight.INCOMPLETE, EdiGlobalSemaphoreEngine.evaluateGlobalSemaphore(1, g1IncompleteFlip).trafficLight)

        // Caso G3: 1 área AMARILLA + 0 FRB = AMARILLO -> sumar 1 FRB = ROJO
        val g3YellowArea = buildCleanGreenSession(3).let { base ->
            base.copy(currentGroupResponses = base.currentGroupResponses + ("G3_MF_3_1" to EdiItemResponse.NO))
        }
        assertEquals(EdiGlobalTrafficLight.AMARILLO, EdiGlobalSemaphoreEngine.evaluateGlobalSemaphore(3, g3YellowArea).trafficLight)
        val g3YellowPlus1Frb = g3YellowArea.copy(frbResponses = g3YellowArea.frbResponses + ("FRB_3" to EdiItemResponse.YES))
        assertEquals(EdiGlobalTrafficLight.ROJO, EdiGlobalSemaphoreEngine.evaluateGlobalSemaphore(3, g3YellowPlus1Frb).trafficLight)

        // Caso G5-G15: FRB/ALE en cualquier estado opcional jamás bloquean ni degradan, incluso discrepancia en ALE 12.3
        val g12WithDiscrepancyAle = buildCleanGreenSession(12, includeFrbAndAle = false).copy(
            dualEvidenceRecords = mapOf(
                "G12_ALE_12_3" to EdiDualEvidenceRecord(
                    itemUniqueId = "G12_ALE_12_3",
                    modality = EdiClinicalModality.DUAL_OBSERVATION_AND_INTERVIEW_ALE_12_3,
                    directObservationEvidence = true,
                    directedInterviewEvidence = false,
                    professionalHumanResolution = null
                )
            )
        )
        val g12Eval = EdiGlobalSemaphoreEngine.evaluateGlobalSemaphore(12, g12WithDiscrepancyAle)
        assertEquals("Discrepancia en ALE 12.3 opcional no debe bloquear G12", EdiGlobalTrafficLight.VERDE, g12Eval.trafficLight)
        assertTrue(g12Eval.optionalDiscrepancyItemCodes.contains("ALE 12.3"))
        assertTrue(g12Eval.nonBlockingClinicalWarnings.isNotEmpty())

        // Caso G14: Discrepancia en LE 14.3 obligatoria SÍ bloquea el semáforo global
        val g14WithLeConflict = buildCleanGreenSession(14, includeFrbAndAle = false).copy(
            dualEvidenceRecords = mapOf(
                "G14_LE_14_3" to EdiDualEvidenceRecord(
                    itemUniqueId = "G14_LE_14_3",
                    modality = EdiClinicalModality.INTERVIEW_PRIMARY_WITH_OBSERVATION_COMPLEMENT,
                    directObservationEvidence = false,
                    directedInterviewEvidence = true,
                    professionalHumanResolution = null
                )
            )
        )
        val g14Blocked = EdiGlobalSemaphoreEngine.evaluateGlobalSemaphore(14, g14WithLeConflict)
        assertEquals(EdiGlobalTrafficLight.INCOMPLETE, g14Blocked.trafficLight)
        assertTrue(g14Blocked.discrepancyItemCodes.contains("LE 14.3"))

        // Caso Control 6: Evaluación incompleta con ALA positiva y área roja ya cerradas mantiene alertas críticas prioritarias
        val g4IncompleteWithAlerts = EdiGroupSessionSnapshot(
            groupNumber = 4,
            currentGroupResponses = mapOf(
                "G4_ALA_4_1" to EdiItemResponse.YES,
                "G4_MG_4_1" to EdiItemResponse.YES,
                "G4_MG_4_2" to EdiItemResponse.NO // falta restar -> INCOMPLETE
            ),
            enResponses = mapOf("EN_1_2" to EdiItemResponse.YES)
        )
        val g4Res = EdiGlobalSemaphoreEngine.evaluateGlobalSemaphore(4, g4IncompleteWithAlerts)
        assertEquals(EdiGlobalTrafficLight.INCOMPLETE, g4Res.trafficLight)
        assertFalse(g4Res.isDefinitive)
        assertTrue(g4Res.hasPriorityCriticalWarning)
        assertTrue(g4Res.requiresImmediateClinicalAttentionDespiteIncomplete)
        assertEquals(2, g4Res.priorityCriticalAlerts.size)
    }

    @Test
    fun `04 audit age calculation all 15 group boundaries prematurity weeks days and discrepancies`() {
        val evalDate = LocalDate.of(2025, 3, 15)
        val fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy")

        // 1. Verificar exclusiones y todos los límites exactos de los 15 grupos
        val cases = listOf(
            // (mesesAtras, diasAtrasExtras, grupoEsperadoOrNull)
            Triple(0, 0, null),   // 0m 0d -> <1m
            Triple(0, 29, null),  // 0m 29d -> <1m
            Triple(1, 0, 1),      // G1: 1m 0d
            Triple(1, 29, 1),     // G1: 1m 29d
            Triple(2, 0, 2),      // G2: 2m 0d
            Triple(2, 29, 2),
            Triple(3, 0, 3),
            Triple(3, 29, 3),
            Triple(4, 0, 4),
            Triple(4, 29, 4),
            Triple(5, 0, 5),
            Triple(6, 29, 5),
            Triple(7, 0, 6),
            Triple(9, 29, 6),
            Triple(10, 0, 7),
            Triple(12, 29, 7),
            Triple(13, 0, 8),
            Triple(15, 29, 8),
            Triple(16, 0, 9),
            Triple(18, 29, 9),
            Triple(19, 0, 10),
            Triple(24, 29, 10),
            Triple(25, 0, 11),
            Triple(30, 29, 11),
            Triple(31, 0, 12),
            Triple(36, 29, 12),
            Triple(37, 0, 13),
            Triple(48, 29, 13),
            Triple(49, 0, 14),
            Triple(59, 29, 14),
            Triple(60, 0, 15),
            Triple(71, 29, 15),
            Triple(72, 0, null)   // >= 72m -> OVER_MAX_AGE
        )

        // Elegimos una fecha de evaluación cuyo día = 31 (p.ej. 31/05/2025) o con cálculo limpio para evitar desfases 30 vs 31 días
        val cleanEvalDate = LocalDate.of(2025, 5, 31)
        cases.forEach { (months, days, expectedGroup) ->
            val birth = cleanEvalDate.minusMonths(months.toLong()).minusDays(days.toLong())
            // En casos donde cleanEvalDate es 31 y birth queda con día < 31, Manual EDI usa préstamos de 30d;
            // probamos selectGroupForAge directamente para los límites puros de meses/días y compareCorrectedAgeMethods para fechas consistentes
            val (_, directGroup) = EdiOfficialCatalog.selectGroupForAge(months, days)
            assertEquals("Límite puro ${months}m ${days}d", expectedGroup, directGroup?.groupNumber)

            // Si birth.dayOfMonth <= cleanEvalDate.dayOfMonth y ambos meses alinean sin préstamo, ambos métodos coinciden exacto
            if (days == 0 && birth.dayOfMonth == cleanEvalDate.dayOfMonth) {
                val cmp = EdiOfficialCatalog.compareCorrectedAgeMethods(
                    fechaNacimiento = birth.format(fmt),
                    fechaEvaluacion = cleanEvalDate.format(fmt),
                    esPrematuro = false,
                    egSemanas = 40,
                    egDias = 0
                )
                assertEquals("Comparación $months m $days d", expectedGroup, cmp.automaticallyAssignedGroup?.groupNumber)
            }
        }

        // 2. Caso documentado obligatorio: 14/07/2024 al 20/03/2025 con 34 semanas exactas -> 6m 24d vs 6m 23d
        val manualTest = EdiOfficialCatalog.compareCorrectedAgeMethods(
            fechaNacimiento = "14/07/2024",
            fechaEvaluacion = "20/03/2025",
            esPrematuro = true,
            egSemanas = 34,
            egDias = 0
        )
        assertEquals("0a 6m 24d", manualTest.manualMethodResult.correctedAge.formattedAMD)
        assertEquals("0a 6m 23d", manualTest.calendarCredMethodResult.correctedAge.formattedAMD)
        assertEquals(EdiGroupAutoSelectionStatus.AUTO_SELECTED_CONSISTENT, manualTest.autoSelectionStatus)
        assertEquals(5, manualTest.automaticallyAssignedGroup?.groupNumber)

        // 3. Semanas y días sin redondeo (ej. 35s 4d -> 7*35+4 = 249 días; prematuridad = 280 - 249 = 31 días)
        val ga = GestationalAgeInput(weeks = 35, days = 4)
        assertEquals(249, ga.totalGestationalDays)
        assertEquals(31, ga.prematurityDaysTo40Weeks)
        assertEquals("35s 4d", ga.originalLabel)

        // 4. Bloqueo por discrepancia de grupo entre Manual EDI y Calendario CRED
        val discrepancy = EdiOfficialCatalog.compareCorrectedAgeMethods(
            fechaNacimiento = "15/02/2025",
            fechaEvaluacion = "09/05/2025",
            esPrematuro = true,
            egSemanas = 36,
            egDias = 5
        )
        assertEquals(
            EdiGroupAutoSelectionStatus.BLOCKED_GROUP_DISCREPANCY_REQUIRES_PROFESSIONAL_REVIEW,
            discrepancy.autoSelectionStatus
        )
        assertNull(discrepancy.automaticallyAssignedGroup)
        assertEquals(2, discrepancy.manualMethodResult.assignedGroup?.groupNumber)
        assertEquals(1, discrepancy.calendarCredMethodResult.assignedGroup?.groupNumber)

        // 5. Corte estricto de < 2 años cronológicos (1a 11m 29d sí corrige; 2a 0m 0d no corrige)
        val under2 = EdiOfficialCatalog.compareCorrectedAgeMethods("10/05/2023", "09/05/2025", true, 34, 0)
        assertTrue(under2.manualMethodResult.appliesCorrectedAge)
        assertTrue(under2.calendarCredMethodResult.appliesCorrectedAge)

        val exact2 = EdiOfficialCatalog.compareCorrectedAgeMethods("09/05/2023", "09/05/2025", true, 34, 0)
        assertFalse(exact2.manualMethodResult.appliesCorrectedAge)
        assertFalse(exact2.calendarCredMethodResult.appliesCorrectedAge)

        // 6. Verificar inmutabilidad de ClinicalAgeCalculator.calculateAges para CRED NTS 238
        val credCheck = ClinicalAgeCalculator.calculateAges("12/03/2025", "10/11/2026", true, 34, 0)
        assertEquals("1a 7m 29d", credCheck.edadCronologicaAMD)
        assertEquals("1a 6m 18d", credCheck.edadCorregidaAMD)
        assertEquals("23/04/2025", credCheck.fechaAjustada)
    }

    @Test
    fun `05 audit document state and catalog findings for phase 5 report`() {
        // Verificar y registrar hechos objetivos encontrados en la auditoría (sin alterar producción)
        val defaultState = CredEdiUiState()

        // Hallazgo: Valores por defecto de pesoKg (5.40), longitudCm (61.5) y egSemanas (32) con esPrematuro=false e inputs vacíos
        assertFalse(defaultState.esPrematuro)
        assertEquals(32, defaultState.egSemanas)
        assertEquals(0, defaultState.egDias)
        assertEquals("", defaultState.pesoInput)
        assertEquals("", defaultState.longitudInput)
        assertEquals(5.40, defaultState.pesoKg, 0.001)
        assertEquals(61.5, defaultState.longitudCm, 0.001)

        // Cuando sólo se ingresa PC (peso y talla vacíos), datosAntropometricosCount pasa a 1 aunque peso/talla estén vacíos
        val onlyPcState = defaultState.copy(perimetroCefalicoInput = "50.6", perimetroCefalicoCm = 50.6)
        assertFalse(onlyPcState.hasPesoIngresado)
        assertFalse(onlyPcState.hasLongitudIngresada)
        assertTrue(onlyPcState.hasPerimetroCefalicoIngresado)
        assertEquals(1, onlyPcState.datosAntropometricosCount)

        // Hallazgo en catálogo: MF 13.1 inicia con un solo *, LE 14.3 inicia con ** con obs=false, ALE 4.4 formato con ** y (Observe) con obs=false
        val mf131 = EdiOfficialCatalog.findItemByUniqueId("G13_MF_13_1")!!
        assertTrue(mf131.consultationQuestionText.startsWith("*MF 13.1."))
        assertFalse(mf131.consultationQuestionText.startsWith("**"))

        val le143 = EdiOfficialCatalog.findItemByUniqueId("G14_LE_14_3")!!
        assertFalse(le143.hasObservationAsterisks)
        assertTrue(le143.consultationQuestionText.startsWith("**LE 14.3."))

        val ale44 = EdiOfficialCatalog.findItemByUniqueId("G4_ALE_4_4")!!
        assertFalse(ale44.hasObservationAsterisks)
        assertTrue(ale44.formatQuestionText.startsWith("**4."))
    }
}
