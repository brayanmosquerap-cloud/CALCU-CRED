package com.example.model

/**
 * Motor Clínico del Semáforo Global EDI Peruano 2026 (Fase 3)
 * Manual para la Aplicación de la Prueba de Evaluación del Desarrollo Infantil (EDI)
 * Versión Peruana - Primera Edición 2026 (MINSA / UNICEF)
 *
 * Alcance estricto de Fase 3:
 * - Consume directamente los resultados de [EdiAreaFallbackEngine] sin duplicar ni reinterpretar retrocesos.
 * - Integra los 5 ejes de [EdiOfficialCatalog] (FRB, ALE, Áreas de Desarrollo, EN y ALA).
 * - Aplica las reglas oficiales diferenciadas para Grupo 1, Grupos 2–4 y Grupos 5–15.
 * - Mantiene control estricto de evaluaciones incompletas, advertencias clínicas prioritarias,
 *   bloqueo por discrepancia de edad y trazabilidad completa de causas.
 * - Permanece desconectado de la interfaz actual (EdiScreen.kt / EdiPdfExporter.kt).
 */

enum class EdiGlobalTrafficLight(
    val displayLabel: String,
    val isDefinitive: Boolean
) {
    VERDE("Verde — Desarrollo normal", true),
    AMARILLO("Amarillo — Rezago en el desarrollo", true),
    ROJO("Rojo — Riesgo de retraso en el desarrollo", true),
    INCOMPLETE("Evaluación incompleta (sin semáforo definitivo)", false)
}

enum class EdiClassificationCauseType {
    RED_AREA,
    POSITIVE_ALA,
    POSITIVE_EN,
    G2_TO_G4_TWO_OR_MORE_YELLOW_AREAS,
    G2_TO_G4_YELLOW_AREA_PLUS_POSITIVE_FRB,
    G2_TO_G4_YELLOW_AREA_PLUS_POSITIVE_ALE,
    G5_TO_G15_ONE_OR_MORE_YELLOW_AREAS,
    G2_TO_G4_EXACTLY_ONE_YELLOW_AREA,
    G1_TO_G4_TWO_OR_MORE_POSITIVE_FRB,
    G1_TO_G4_TWO_OR_MORE_POSITIVE_ALE,
    G1_TO_G4_ONE_POSITIVE_FRB_AND_ONE_POSITIVE_ALE,
    ALL_CRITERIA_NORMAL_GREEN,
    BLOCKED_INCOMPLETE_MANDATORY_ITEMS,
    BLOCKED_MISSING_MATERIAL,
    BLOCKED_MODALITY_DISCREPANCY,
    BLOCKED_AGE_DISCREPANCY,
    BLOCKED_OUT_OF_AGE_RANGE,
    BLOCKED_SESSION_GROUP_MISMATCH
}

/**
 * Causa auditable y trazable que explica por qué el motor emitió el resultado final
 * (incluyendo exactamente qué área, ALA, EN o combinación FRB/ALE intervino).
 */
data class EdiClassificationCause(
    val type: EdiClassificationCauseType,
    val axis: EdiAxis?,
    val resultingColorContribution: EdiGlobalTrafficLight,
    val involvedCodes: List<String>,
    val description: String
)

enum class EdiPriorityCriticalAlertType {
    POSITIVE_ALARM_SIGNAL_ALA,
    ALTERED_NEUROLOGICAL_EXAM_EN,
    RED_DEVELOPMENT_AREA
}

/**
 * Advertencia clínica prioritaria independiente del semáforo definitivo.
 * Garantiza que un hallazgo crítico (ALA positiva, EN alterada o Área ROJA completamente evaluada)
 * jamás quede oculto cuando la evaluación aún se encuentra INCOMPLETA.
 */
data class EdiPriorityCriticalAlert(
    val type: EdiPriorityCriticalAlertType,
    val axis: EdiAxis,
    val code: String,
    val message: String
)

/**
 * Resultado completo y auditable del semáforo global EDI para una sesión y grupo.
 */
data class EdiGlobalEvaluationResult(
    val groupNumber: Int,
    val trafficLight: EdiGlobalTrafficLight,
    val isDefinitive: Boolean,
    val axis3AreasSummary: EdiAxis3AreasSummary,
    val redAreas: List<EdiDevArea>,
    val yellowAreas: List<EdiDevArea>,
    val greenAreas: List<EdiDevArea>,
    val incompleteAreas: List<EdiDevArea>,
    val notApplicableAreas: List<EdiDevArea>,
    val frbResponsesResolved: Map<String, EdiItemResponse>,
    val aleResponsesResolved: Map<String, EdiItemResponse>,
    val enResponsesResolved: Map<String, EdiItemResponse>,
    val alaResponsesResolved: Map<String, EdiItemResponse>,
    val positiveFrbItems: List<EdiCatalogItem>,
    val positiveAleItems: List<EdiCatalogItem>,
    val positiveEnItems: List<EdiCatalogItem>,
    val positiveAlaItems: List<EdiCatalogItem>,
    val frbAndAleModifyTrafficLight: Boolean,
    val frbMandatoryForGroup: Boolean,
    val aleMandatoryForGroup: Boolean,
    val en13AppliesToGroup: Boolean,
    val alaAppliesToGroup: Boolean,
    val pendingMandatoryItemCodes: List<String>,
    val missingMaterialItemCodes: List<String>,
    val discrepancyItemCodes: List<String>,
    val blockingReasons: List<String>,
    val optionalPendingItemCodes: List<String> = emptyList(),
    val optionalMissingMaterialItemCodes: List<String> = emptyList(),
    val optionalDiscrepancyItemCodes: List<String> = emptyList(),
    val nonBlockingClinicalWarnings: List<String> = emptyList(),
    val causes: List<EdiClassificationCause>,
    val priorityCriticalAlerts: List<EdiPriorityCriticalAlert>,
    val hasPriorityCriticalWarning: Boolean,
    val requiresImmediateClinicalAttentionDespiteIncomplete: Boolean,
    val blockedByAgeDiscrepancy: Boolean,
    val blockedByOutOfRangeAge: Boolean,
    val sessionGroupMismatchDetected: Boolean,
    val clinicalSummaryText: String
)

object EdiGlobalSemaphoreEngine {

    /**
     * Evalúa el semáforo global a partir del estado completo de sesión.
     */
    fun evaluateGlobalSemaphore(
        state: EdiPhase1EngineState,
        ageComparison: EdiCorrectedAgeComparison? = null
    ): EdiGlobalEvaluationResult = evaluateGlobalSemaphore(
        groupNumber = state.activeGroupNumber,
        session = state.activeSession,
        ageComparison = ageComparison
    )

    /**
     * Calcula el semáforo global EDI Peruano 2026 de forma determinística a partir de:
     * 1. Las respuestas individuales registradas en [session] para los 5 ejes.
     * 2. Los resultados de áreas y retrocesos emitidos por [EdiAreaFallbackEngine].
     * 3. La validación opcional de asignación etaria [ageComparison].
     *
     * Nunca acepta un color manual y nunca reutiliza respuestas si [session.groupNumber] != [groupNumber].
     */
    fun evaluateGlobalSemaphore(
        groupNumber: Int,
        session: EdiGroupSessionSnapshot,
        ageComparison: EdiCorrectedAgeComparison? = null
    ): EdiGlobalEvaluationResult {
        val groupSpec = EdiOfficialCatalog.getGroup(groupNumber)
        val sessionGroupMismatch = session.groupNumber != groupNumber

        // Aislamiento estricto: jamás leer respuestas de otra sesión/grupo
        val safeFrbMap = if (!sessionGroupMismatch) session.frbResponses else emptyMap()
        val safeEnMap = if (!sessionGroupMismatch) session.enResponses else emptyMap()
        val safeCurrentMap = if (!sessionGroupMismatch) session.currentGroupResponses else emptyMap()
        val safeDualMap = if (!sessionGroupMismatch) session.dualEvidenceRecords else emptyMap()

        // Delegar Eje 3 y todas las reglas de retroceso exclusivamente a EdiAreaFallbackEngine
        val axis3Summary = EdiAreaFallbackEngine.evaluateAllAreasForGroup(groupNumber, session)

        val redAreas = mutableListOf<EdiDevArea>()
        val yellowAreas = mutableListOf<EdiDevArea>()
        val greenAreas = mutableListOf<EdiDevArea>()
        val incompleteAreas = mutableListOf<EdiDevArea>()
        val notApplicableAreas = mutableListOf<EdiDevArea>()

        EdiDevArea.entries.forEach { area ->
            val areaRes = axis3Summary.areaResults[area]
            when (areaRes?.status) {
                EdiAreaColorResult.ROJO -> redAreas.add(area)
                EdiAreaColorResult.AMARILLO -> yellowAreas.add(area)
                EdiAreaColorResult.VERDE -> greenAreas.add(area)
                EdiAreaColorResult.INCOMPLETE -> incompleteAreas.add(area)
                EdiAreaColorResult.NOT_APPLICABLE, null -> notApplicableAreas.add(area)
            }
        }

        // Resolución individual de los reactivos de FRB, ALE, EN (excluyendo EN 1.3 en G15) y ALA
        val frbItems = EdiOfficialCatalog.frbItems
        val aleItems = groupSpec.aleItems
        val enItems = EdiOfficialCatalog.getApplicableEnItems(groupNumber)
        val alaItems = groupSpec.alaItems

        val frbResolved = resolveAxisResponses(frbItems, safeFrbMap, safeDualMap)
        val aleResolved = resolveAxisResponses(aleItems, safeCurrentMap, safeDualMap)
        val enResolved = resolveAxisResponses(enItems, safeEnMap, safeDualMap)
        val alaResolved = resolveAxisResponses(alaItems, safeCurrentMap, safeDualMap)

        // En todos estos ejes (FRB, ALE, EN, ALA), yesIsNormalGreen == false, es decir: YES = hallazgo positivo
        val positiveFrbItems = frbItems.filter { frbResolved[it.uniqueId] == EdiItemResponse.YES }
        val positiveAleItems = aleItems.filter { aleResolved[it.uniqueId] == EdiItemResponse.YES }
        val positiveEnItems = enItems.filter { enResolved[it.uniqueId] == EdiItemResponse.YES }
        val positiveAlaItems = alaItems.filter { alaResolved[it.uniqueId] == EdiItemResponse.YES }

        val frbAndAleMandatory = groupNumber in 1..4
        val frbAndAleModifyTrafficLight = groupSpec.frbAndAleModifyGlobalTrafficLight

        // Recopilar causas de bloqueo obligatorio separadas de advertencias opcionales (G5–G15)
        val pendingMandatoryCodes = mutableListOf<String>()
        val missingMaterialCodes = mutableListOf<String>()
        val discrepancyCodes = mutableListOf<String>()
        val optionalPendingCodes = mutableListOf<String>()
        val optionalMissingMaterialCodes = mutableListOf<String>()
        val optionalDiscrepancyCodes = mutableListOf<String>()
        val blockingReasons = mutableListOf<String>()
        val nonBlockingWarnings = mutableListOf<String>()

        if (sessionGroupMismatch) {
            blockingReasons.add(
                "Aislamiento de sesión: la sesión pertenece al Grupo ${session.groupNumber} y no puede calificar al Grupo $groupNumber."
            )
        }

        var blockedByAgeDiscrepancy = false
        var blockedByOutOfRangeAge = false

        if (ageComparison != null) {
            when (ageComparison.autoSelectionStatus) {
                EdiGroupAutoSelectionStatus.BLOCKED_GROUP_DISCREPANCY_REQUIRES_PROFESSIONAL_REVIEW -> {
                    blockedByAgeDiscrepancy = true
                    blockingReasons.add(ageComparison.clinicalReviewMessage)
                }

                EdiGroupAutoSelectionStatus.OUT_OF_RANGE_UNDER_1_MONTH,
                EdiGroupAutoSelectionStatus.OUT_OF_RANGE_72_MONTHS_OR_MORE,
                EdiGroupAutoSelectionStatus.INVALID_DATES -> {
                    blockedByOutOfRangeAge = true
                    blockingReasons.add(ageComparison.clinicalReviewMessage)
                }

                EdiGroupAutoSelectionStatus.AUTO_SELECTED_CONSISTENT -> {
                    val assigned = ageComparison.automaticallyAssignedGroup?.groupNumber
                    if (assigned != null && assigned != groupNumber) {
                        blockingReasons.add(
                            "Inconsistencia de grupo etario: la edad corresponde al Grupo $assigned y no al Grupo $groupNumber."
                        )
                    }
                }
            }
        }

        // Inspeccionar FRB (obligatorio en G1–G4; en G5–G15 es opcional y no bloquea por UNANSWERED, falta de material ni discrepancia)
        inspectAxisCompleteness(
            items = frbItems,
            resolvedMap = frbResolved,
            isAxisMandatory = frbAndAleMandatory,
            pendingMandatoryCodes = pendingMandatoryCodes,
            missingMaterialCodes = missingMaterialCodes,
            discrepancyCodes = discrepancyCodes,
            optionalPendingCodes = optionalPendingCodes,
            optionalMissingMaterialCodes = optionalMissingMaterialCodes,
            optionalDiscrepancyCodes = optionalDiscrepancyCodes
        )

        // Inspeccionar ALE (obligatorio en G1–G4; en G5–G15 es opcional y no bloquea por UNANSWERED, falta de material ni discrepancia)
        inspectAxisCompleteness(
            items = aleItems,
            resolvedMap = aleResolved,
            isAxisMandatory = frbAndAleMandatory,
            pendingMandatoryCodes = pendingMandatoryCodes,
            missingMaterialCodes = missingMaterialCodes,
            discrepancyCodes = discrepancyCodes,
            optionalPendingCodes = optionalPendingCodes,
            optionalMissingMaterialCodes = optionalMissingMaterialCodes,
            optionalDiscrepancyCodes = optionalDiscrepancyCodes
        )

        // Inspeccionar Eje 3 (Áreas aplicables y retrocesos cuando correspondan)
        axis3Summary.areaResults.values.filter { it.appliesToGroup }.forEach { areaRes ->
            if (!areaRes.isCurrentSectionComplete) {
                areaRes.currentItems.forEach { item ->
                    val resp = areaRes.currentItemResponses[item.uniqueId] ?: EdiItemResponse.UNANSWERED
                    classifyBlockingResponse(
                        code = item.itemCode,
                        response = resp,
                        pendingMandatoryCodes = pendingMandatoryCodes,
                        missingMaterialCodes = missingMaterialCodes,
                        discrepancyCodes = discrepancyCodes
                    )
                }
            }
            if (areaRes.fallbackRequired && !areaRes.isFallbackSectionComplete) {
                areaRes.fallbackItems.forEach { fbSpec ->
                    val resp = areaRes.activeFallbackResponses[fbSpec.fallbackUniqueId] ?: EdiItemResponse.UNANSWERED
                    val fbLabel = "${fbSpec.sourceItem.itemCode} (retroceso ${areaRes.area.code} G$groupNumber)"
                    classifyBlockingResponse(
                        code = fbLabel,
                        response = resp,
                        pendingMandatoryCodes = pendingMandatoryCodes,
                        missingMaterialCodes = missingMaterialCodes,
                        discrepancyCodes = discrepancyCodes
                    )
                }
            }
        }

        // Inspeccionar Eje 4 (EN aplicable; en G15 excluye EN 1.3)
        inspectAxisCompleteness(
            items = enItems,
            resolvedMap = enResolved,
            isAxisMandatory = true,
            pendingMandatoryCodes = pendingMandatoryCodes,
            missingMaterialCodes = missingMaterialCodes,
            discrepancyCodes = discrepancyCodes,
            optionalPendingCodes = optionalPendingCodes,
            optionalMissingMaterialCodes = optionalMissingMaterialCodes,
            optionalDiscrepancyCodes = optionalDiscrepancyCodes
        )

        // Inspeccionar Eje 5 (ALA aplicable; en G1 está vacío y no se exige)
        inspectAxisCompleteness(
            items = alaItems,
            resolvedMap = alaResolved,
            isAxisMandatory = true,
            pendingMandatoryCodes = pendingMandatoryCodes,
            missingMaterialCodes = missingMaterialCodes,
            discrepancyCodes = discrepancyCodes,
            optionalPendingCodes = optionalPendingCodes,
            optionalMissingMaterialCodes = optionalMissingMaterialCodes,
            optionalDiscrepancyCodes = optionalDiscrepancyCodes
        )

        if (pendingMandatoryCodes.isNotEmpty()) {
            blockingReasons.add(
                "Reactivos obligatorios sin responder: ${pendingMandatoryCodes.joinToString(", ")}."
            )
        }
        if (missingMaterialCodes.isNotEmpty()) {
            blockingReasons.add(
                "Reactivos no evaluados por falta de material/lámina oficial: ${missingMaterialCodes.joinToString(", ")}."
            )
        }
        if (discrepancyCodes.isNotEmpty()) {
            blockingReasons.add(
                "Reactivos con discrepancia entre observación y entrevista pendientes de resolución humana: ${discrepancyCodes.joinToString(", ")}."
            )
        }

        if (optionalMissingMaterialCodes.isNotEmpty()) {
            nonBlockingWarnings.add(
                "Reactivos opcionales (FRB/ALE) sin evaluar por falta de material/lámina (no bloquean semáforo en Grupos 5–15): ${optionalMissingMaterialCodes.joinToString(", ")}."
            )
        }
        if (optionalDiscrepancyCodes.isNotEmpty()) {
            nonBlockingWarnings.add(
                "Reactivos opcionales (FRB/ALE) con discrepancia pendientes de aclaración (no bloquean semáforo en Grupos 5–15): ${optionalDiscrepancyCodes.joinToString(", ")}."
            )
        }
        if (optionalPendingCodes.isNotEmpty()) {
            nonBlockingWarnings.add(
                "Reactivos opcionales (FRB/ALE) sin responder (no bloquean semáforo en Grupos 5–15): ${optionalPendingCodes.joinToString(", ")}."
            )
        }

        // Advertencias clínicas prioritarias independientes del semáforo definitivo:
        // ALA positiva, EN alterada o Área ROJA completamente evaluada jamás quedan ocultas.
        val priorityCriticalAlerts = buildPriorityCriticalAlerts(
            positiveAlaItems = positiveAlaItems,
            positiveEnItems = positiveEnItems,
            redAreas = redAreas
        )

        // Si falta cualquier dato obligatorio, hay material faltante, discrepancia o bloqueo etario/sesión -> INCOMPLETE
        if (blockingReasons.isNotEmpty()) {
            val blockedCauses = buildBlockedCauses(
                sessionGroupMismatch = sessionGroupMismatch,
                blockedByAgeDiscrepancy = blockedByAgeDiscrepancy,
                blockedByOutOfRangeAge = blockedByOutOfRangeAge,
                missingMaterialCodes = missingMaterialCodes,
                discrepancyCodes = discrepancyCodes,
                pendingMandatoryCodes = pendingMandatoryCodes,
                blockingReasons = blockingReasons
            )
            val hasCritical = priorityCriticalAlerts.isNotEmpty()
            val summaryText = buildIncompleteSummaryText(
                groupNumber = groupNumber,
                blockingReasons = blockingReasons,
                priorityCriticalAlerts = priorityCriticalAlerts
            )
            return EdiGlobalEvaluationResult(
                groupNumber = groupNumber,
                trafficLight = EdiGlobalTrafficLight.INCOMPLETE,
                isDefinitive = false,
                axis3AreasSummary = axis3Summary,
                redAreas = redAreas,
                yellowAreas = yellowAreas,
                greenAreas = greenAreas,
                incompleteAreas = incompleteAreas,
                notApplicableAreas = notApplicableAreas,
                frbResponsesResolved = frbResolved,
                aleResponsesResolved = aleResolved,
                enResponsesResolved = enResolved,
                alaResponsesResolved = alaResolved,
                positiveFrbItems = positiveFrbItems,
                positiveAleItems = positiveAleItems,
                positiveEnItems = positiveEnItems,
                positiveAlaItems = positiveAlaItems,
                frbAndAleModifyTrafficLight = frbAndAleModifyTrafficLight,
                frbMandatoryForGroup = frbAndAleMandatory,
                aleMandatoryForGroup = frbAndAleMandatory,
                en13AppliesToGroup = groupSpec.neurologicalEn13Applies,
                alaAppliesToGroup = groupSpec.hasAlarmSignals,
                pendingMandatoryItemCodes = pendingMandatoryCodes,
                missingMaterialItemCodes = missingMaterialCodes,
                discrepancyItemCodes = discrepancyCodes,
                blockingReasons = blockingReasons,
                optionalPendingItemCodes = optionalPendingCodes,
                optionalMissingMaterialItemCodes = optionalMissingMaterialCodes,
                optionalDiscrepancyItemCodes = optionalDiscrepancyCodes,
                nonBlockingClinicalWarnings = nonBlockingWarnings,
                causes = blockedCauses,
                priorityCriticalAlerts = priorityCriticalAlerts,
                hasPriorityCriticalWarning = hasCritical,
                requiresImmediateClinicalAttentionDespiteIncomplete = hasCritical,
                blockedByAgeDiscrepancy = blockedByAgeDiscrepancy,
                blockedByOutOfRangeAge = blockedByOutOfRangeAge,
                sessionGroupMismatchDetected = sessionGroupMismatch,
                clinicalSummaryText = summaryText
            )
        }

        // Todas las respuestas obligatorias completas: aplicar reglas oficiales exactas por grupo
        val (finalTrafficLight, causes) = when (groupNumber) {
            1 -> classifyGroup1(
                redAreas = redAreas,
                positiveEnItems = positiveEnItems,
                positiveFrbItems = positiveFrbItems,
                positiveAleItems = positiveAleItems
            )

            in 2..4 -> classifyGroups2To4(
                redAreas = redAreas,
                yellowAreas = yellowAreas,
                positiveAlaItems = positiveAlaItems,
                positiveEnItems = positiveEnItems,
                positiveFrbItems = positiveFrbItems,
                positiveAleItems = positiveAleItems
            )

            else -> classifyGroups5To15(
                groupNumber = groupNumber,
                redAreas = redAreas,
                yellowAreas = yellowAreas,
                positiveAlaItems = positiveAlaItems,
                positiveEnItems = positiveEnItems
            )
        }

        val summaryText = buildDefinitiveSummaryText(
            groupNumber = groupNumber,
            trafficLight = finalTrafficLight,
            causes = causes,
            nonBlockingWarnings = nonBlockingWarnings
        )

        return EdiGlobalEvaluationResult(
            groupNumber = groupNumber,
            trafficLight = finalTrafficLight,
            isDefinitive = true,
            axis3AreasSummary = axis3Summary,
            redAreas = redAreas,
            yellowAreas = yellowAreas,
            greenAreas = greenAreas,
            incompleteAreas = incompleteAreas,
            notApplicableAreas = notApplicableAreas,
            frbResponsesResolved = frbResolved,
            aleResponsesResolved = aleResolved,
            enResponsesResolved = enResolved,
            alaResponsesResolved = alaResolved,
            positiveFrbItems = positiveFrbItems,
            positiveAleItems = positiveAleItems,
            positiveEnItems = positiveEnItems,
            positiveAlaItems = positiveAlaItems,
            frbAndAleModifyTrafficLight = frbAndAleModifyTrafficLight,
            frbMandatoryForGroup = frbAndAleMandatory,
            aleMandatoryForGroup = frbAndAleMandatory,
            en13AppliesToGroup = groupSpec.neurologicalEn13Applies,
            alaAppliesToGroup = groupSpec.hasAlarmSignals,
            pendingMandatoryItemCodes = emptyList(),
            missingMaterialItemCodes = emptyList(),
            discrepancyItemCodes = emptyList(),
            blockingReasons = emptyList(),
            optionalPendingItemCodes = optionalPendingCodes,
            optionalMissingMaterialItemCodes = optionalMissingMaterialCodes,
            optionalDiscrepancyItemCodes = optionalDiscrepancyCodes,
            nonBlockingClinicalWarnings = nonBlockingWarnings,
            causes = causes,
            priorityCriticalAlerts = priorityCriticalAlerts,
            hasPriorityCriticalWarning = priorityCriticalAlerts.isNotEmpty(),
            requiresImmediateClinicalAttentionDespiteIncomplete = false,
            blockedByAgeDiscrepancy = false,
            blockedByOutOfRangeAge = false,
            sessionGroupMismatchDetected = false,
            clinicalSummaryText = summaryText
        )
    }

    private fun resolveAxisResponses(
        items: List<EdiCatalogItem>,
        responseMap: Map<String, EdiItemResponse>,
        dualEvidenceRecords: Map<String, EdiDualEvidenceRecord>
    ): Map<String, EdiItemResponse> {
        val result = linkedMapOf<String, EdiItemResponse>()
        items.forEach { item ->
            val direct = responseMap[item.uniqueId] ?: EdiItemResponse.UNANSWERED
            val effective = if (direct == EdiItemResponse.NOT_EVALUATED_MISSING_MATERIAL) {
                EdiItemResponse.NOT_EVALUATED_MISSING_MATERIAL
            } else {
                dualEvidenceRecords[item.uniqueId]?.resolveItemResponse() ?: direct
            }
            result[item.uniqueId] = effective
        }
        return result
    }

    private fun inspectAxisCompleteness(
        items: List<EdiCatalogItem>,
        resolvedMap: Map<String, EdiItemResponse>,
        isAxisMandatory: Boolean,
        pendingMandatoryCodes: MutableList<String>,
        missingMaterialCodes: MutableList<String>,
        discrepancyCodes: MutableList<String>,
        optionalPendingCodes: MutableList<String>,
        optionalMissingMaterialCodes: MutableList<String>,
        optionalDiscrepancyCodes: MutableList<String>
    ) {
        items.forEach { item ->
            val resp = resolvedMap[item.uniqueId] ?: EdiItemResponse.UNANSWERED
            when (resp) {
                EdiItemResponse.YES, EdiItemResponse.NO -> Unit
                EdiItemResponse.NOT_EVALUATED_MISSING_MATERIAL -> {
                    if (isAxisMandatory) {
                        missingMaterialCodes.add(item.itemCode)
                    } else {
                        optionalMissingMaterialCodes.add(item.itemCode)
                    }
                }
                EdiItemResponse.DISCREPANCY_REQUIRES_HUMAN_RESOLUTION -> {
                    if (isAxisMandatory) {
                        discrepancyCodes.add(item.itemCode)
                    } else {
                        optionalDiscrepancyCodes.add(item.itemCode)
                    }
                }
                EdiItemResponse.UNANSWERED -> {
                    if (isAxisMandatory) {
                        pendingMandatoryCodes.add(item.itemCode)
                    } else {
                        optionalPendingCodes.add(item.itemCode)
                    }
                }
            }
        }
    }

    private fun classifyBlockingResponse(
        code: String,
        response: EdiItemResponse,
        pendingMandatoryCodes: MutableList<String>,
        missingMaterialCodes: MutableList<String>,
        discrepancyCodes: MutableList<String>
    ) {
        when (response) {
            EdiItemResponse.UNANSWERED -> pendingMandatoryCodes.add(code)
            EdiItemResponse.NOT_EVALUATED_MISSING_MATERIAL -> missingMaterialCodes.add(code)
            EdiItemResponse.DISCREPANCY_REQUIRES_HUMAN_RESOLUTION -> discrepancyCodes.add(code)
            EdiItemResponse.YES, EdiItemResponse.NO -> Unit
        }
    }

    private fun classifyGroup1(
        redAreas: List<EdiDevArea>,
        positiveEnItems: List<EdiCatalogItem>,
        positiveFrbItems: List<EdiCatalogItem>,
        positiveAleItems: List<EdiCatalogItem>
    ): Pair<EdiGlobalTrafficLight, List<EdiClassificationCause>> {
        val redCauses = mutableListOf<EdiClassificationCause>()
        if (redAreas.isNotEmpty()) {
            redCauses.add(
                EdiClassificationCause(
                    type = EdiClassificationCauseType.RED_AREA,
                    axis = EdiAxis.AREAS,
                    resultingColorContribution = EdiGlobalTrafficLight.ROJO,
                    involvedCodes = redAreas.map { it.code },
                    description = "Grupo 1: ${redAreas.size} área(s) del desarrollo en ROJO (${redAreas.joinToString(", ") { it.code }})."
                )
            )
        }
        if (positiveEnItems.isNotEmpty()) {
            redCauses.add(
                EdiClassificationCause(
                    type = EdiClassificationCauseType.POSITIVE_EN,
                    axis = EdiAxis.EN,
                    resultingColorContribution = EdiGlobalTrafficLight.ROJO,
                    involvedCodes = positiveEnItems.map { it.itemCode },
                    description = "Grupo 1: ${positiveEnItems.size} alteración(es) en Exploración Neurológica (${positiveEnItems.joinToString(", ") { it.itemCode }})."
                )
            )
        }
        if (redCauses.isNotEmpty()) {
            return EdiGlobalTrafficLight.ROJO to redCauses
        }

        val yellowCauses = mutableListOf<EdiClassificationCause>()
        if (positiveFrbItems.size >= 2) {
            yellowCauses.add(
                EdiClassificationCause(
                    type = EdiClassificationCauseType.G1_TO_G4_TWO_OR_MORE_POSITIVE_FRB,
                    axis = EdiAxis.FRB,
                    resultingColorContribution = EdiGlobalTrafficLight.AMARILLO,
                    involvedCodes = positiveFrbItems.map { it.itemCode },
                    description = "Grupo 1: ${positiveFrbItems.size} Factores de Riesgo Biológico positivos (${positiveFrbItems.joinToString(", ") { it.itemCode }})."
                )
            )
        }
        if (positiveAleItems.size >= 2) {
            yellowCauses.add(
                EdiClassificationCause(
                    type = EdiClassificationCauseType.G1_TO_G4_TWO_OR_MORE_POSITIVE_ALE,
                    axis = EdiAxis.ALE,
                    resultingColorContribution = EdiGlobalTrafficLight.AMARILLO,
                    involvedCodes = positiveAleItems.map { it.itemCode },
                    description = "Grupo 1: ${positiveAleItems.size} Señales de Alerta (ALE) positivas (${positiveAleItems.joinToString(", ") { it.itemCode }})."
                )
            )
        }
        if (positiveFrbItems.isNotEmpty() && positiveAleItems.isNotEmpty()) {
            val combined = positiveFrbItems.map { it.itemCode } + positiveAleItems.map { it.itemCode }
            yellowCauses.add(
                EdiClassificationCause(
                    type = EdiClassificationCauseType.G1_TO_G4_ONE_POSITIVE_FRB_AND_ONE_POSITIVE_ALE,
                    axis = null,
                    resultingColorContribution = EdiGlobalTrafficLight.AMARILLO,
                    involvedCodes = combined,
                    description = "Grupo 1: combinación de FRB positivo (${positiveFrbItems.joinToString(", ") { it.itemCode }}) más ALE positiva (${positiveAleItems.joinToString(", ") { it.itemCode }})."
                )
            )
        }
        if (yellowCauses.isNotEmpty()) {
            return EdiGlobalTrafficLight.AMARILLO to yellowCauses
        }

        return EdiGlobalTrafficLight.VERDE to listOf(
            EdiClassificationCause(
                type = EdiClassificationCauseType.ALL_CRITERIA_NORMAL_GREEN,
                axis = null,
                resultingColorContribution = EdiGlobalTrafficLight.VERDE,
                involvedCodes = (positiveFrbItems + positiveAleItems).map { it.itemCode },
                description = "Grupo 1: todas las áreas aplicables en VERDE, Exploración Neurológica normal y sin combinación de riesgo FRB/ALE que amerite AMARILLO."
            )
        )
    }

    private fun classifyGroups2To4(
        redAreas: List<EdiDevArea>,
        yellowAreas: List<EdiDevArea>,
        positiveAlaItems: List<EdiCatalogItem>,
        positiveEnItems: List<EdiCatalogItem>,
        positiveFrbItems: List<EdiCatalogItem>,
        positiveAleItems: List<EdiCatalogItem>
    ): Pair<EdiGlobalTrafficLight, List<EdiClassificationCause>> {
        val redCauses = mutableListOf<EdiClassificationCause>()
        if (redAreas.isNotEmpty()) {
            redCauses.add(
                EdiClassificationCause(
                    type = EdiClassificationCauseType.RED_AREA,
                    axis = EdiAxis.AREAS,
                    resultingColorContribution = EdiGlobalTrafficLight.ROJO,
                    involvedCodes = redAreas.map { it.code },
                    description = "${redAreas.size} área(s) del desarrollo en ROJO (${redAreas.joinToString(", ") { it.code }})."
                )
            )
        }
        if (positiveAlaItems.isNotEmpty()) {
            redCauses.add(
                EdiClassificationCause(
                    type = EdiClassificationCauseType.POSITIVE_ALA,
                    axis = EdiAxis.ALA,
                    resultingColorContribution = EdiGlobalTrafficLight.ROJO,
                    involvedCodes = positiveAlaItems.map { it.itemCode },
                    description = "${positiveAlaItems.size} Señal(es) de Alarma (ALA) positiva(s) (${positiveAlaItems.joinToString(", ") { it.itemCode }})."
                )
            )
        }
        if (positiveEnItems.isNotEmpty()) {
            redCauses.add(
                EdiClassificationCause(
                    type = EdiClassificationCauseType.POSITIVE_EN,
                    axis = EdiAxis.EN,
                    resultingColorContribution = EdiGlobalTrafficLight.ROJO,
                    involvedCodes = positiveEnItems.map { it.itemCode },
                    description = "${positiveEnItems.size} alteración(es) en Exploración Neurológica (${positiveEnItems.joinToString(", ") { it.itemCode }})."
                )
            )
        }
        if (yellowAreas.size >= 2) {
            redCauses.add(
                EdiClassificationCause(
                    type = EdiClassificationCauseType.G2_TO_G4_TWO_OR_MORE_YELLOW_AREAS,
                    axis = EdiAxis.AREAS,
                    resultingColorContribution = EdiGlobalTrafficLight.ROJO,
                    involvedCodes = yellowAreas.map { it.code },
                    description = "${yellowAreas.size} áreas del desarrollo en AMARILLO (${yellowAreas.joinToString(", ") { it.code }}) equivalen a ROJO en Grupos 2–4."
                )
            )
        }
        if (yellowAreas.isNotEmpty() && positiveFrbItems.isNotEmpty()) {
            redCauses.add(
                EdiClassificationCause(
                    type = EdiClassificationCauseType.G2_TO_G4_YELLOW_AREA_PLUS_POSITIVE_FRB,
                    axis = null,
                    resultingColorContribution = EdiGlobalTrafficLight.ROJO,
                    involvedCodes = yellowAreas.map { it.code } + positiveFrbItems.map { it.itemCode },
                    description = "Área AMARILLA (${yellowAreas.joinToString(", ") { it.code }}) combinada con FRB positivo (${positiveFrbItems.joinToString(", ") { it.itemCode }})."
                )
            )
        }
        if (yellowAreas.isNotEmpty() && positiveAleItems.isNotEmpty()) {
            redCauses.add(
                EdiClassificationCause(
                    type = EdiClassificationCauseType.G2_TO_G4_YELLOW_AREA_PLUS_POSITIVE_ALE,
                    axis = null,
                    resultingColorContribution = EdiGlobalTrafficLight.ROJO,
                    involvedCodes = yellowAreas.map { it.code } + positiveAleItems.map { it.itemCode },
                    description = "Área AMARILLA (${yellowAreas.joinToString(", ") { it.code }}) combinada con ALE positiva (${positiveAleItems.joinToString(", ") { it.itemCode }})."
                )
            )
        }
        if (redCauses.isNotEmpty()) {
            return EdiGlobalTrafficLight.ROJO to redCauses
        }

        val yellowCauses = mutableListOf<EdiClassificationCause>()
        if (yellowAreas.size == 1) {
            yellowCauses.add(
                EdiClassificationCause(
                    type = EdiClassificationCauseType.G2_TO_G4_EXACTLY_ONE_YELLOW_AREA,
                    axis = EdiAxis.AREAS,
                    resultingColorContribution = EdiGlobalTrafficLight.AMARILLO,
                    involvedCodes = yellowAreas.map { it.code },
                    description = "Exactamente 1 área del desarrollo en AMARILLO (${yellowAreas.first().code}) sin FRB ni ALE positivos."
                )
            )
        }
        if (positiveFrbItems.size >= 2) {
            yellowCauses.add(
                EdiClassificationCause(
                    type = EdiClassificationCauseType.G1_TO_G4_TWO_OR_MORE_POSITIVE_FRB,
                    axis = EdiAxis.FRB,
                    resultingColorContribution = EdiGlobalTrafficLight.AMARILLO,
                    involvedCodes = positiveFrbItems.map { it.itemCode },
                    description = "${positiveFrbItems.size} Factores de Riesgo Biológico positivos (${positiveFrbItems.joinToString(", ") { it.itemCode }})."
                )
            )
        }
        if (positiveAleItems.size >= 2) {
            yellowCauses.add(
                EdiClassificationCause(
                    type = EdiClassificationCauseType.G1_TO_G4_TWO_OR_MORE_POSITIVE_ALE,
                    axis = EdiAxis.ALE,
                    resultingColorContribution = EdiGlobalTrafficLight.AMARILLO,
                    involvedCodes = positiveAleItems.map { it.itemCode },
                    description = "${positiveAleItems.size} Señales de Alerta (ALE) positivas (${positiveAleItems.joinToString(", ") { it.itemCode }})."
                )
            )
        }
        if (positiveFrbItems.isNotEmpty() && positiveAleItems.isNotEmpty()) {
            yellowCauses.add(
                EdiClassificationCause(
                    type = EdiClassificationCauseType.G1_TO_G4_ONE_POSITIVE_FRB_AND_ONE_POSITIVE_ALE,
                    axis = null,
                    resultingColorContribution = EdiGlobalTrafficLight.AMARILLO,
                    involvedCodes = positiveFrbItems.map { it.itemCode } + positiveAleItems.map { it.itemCode },
                    description = "Combinación de al menos 1 FRB positivo (${positiveFrbItems.joinToString(", ") { it.itemCode }}) más al menos 1 ALE positiva (${positiveAleItems.joinToString(", ") { it.itemCode }})."
                )
            )
        }
        if (yellowCauses.isNotEmpty()) {
            return EdiGlobalTrafficLight.AMARILLO to yellowCauses
        }

        return EdiGlobalTrafficLight.VERDE to listOf(
            EdiClassificationCause(
                type = EdiClassificationCauseType.ALL_CRITERIA_NORMAL_GREEN,
                axis = null,
                resultingColorContribution = EdiGlobalTrafficLight.VERDE,
                involvedCodes = (positiveFrbItems + positiveAleItems).map { it.itemCode },
                description = "Todas las áreas en VERDE, EN y ALA negativos, y sin criterios de riesgo AMARILLO/ROJO en FRB/ALE."
            )
        )
    }

    private fun classifyGroups5To15(
        groupNumber: Int,
        redAreas: List<EdiDevArea>,
        yellowAreas: List<EdiDevArea>,
        positiveAlaItems: List<EdiCatalogItem>,
        positiveEnItems: List<EdiCatalogItem>
    ): Pair<EdiGlobalTrafficLight, List<EdiClassificationCause>> {
        val redCauses = mutableListOf<EdiClassificationCause>()
        if (redAreas.isNotEmpty()) {
            redCauses.add(
                EdiClassificationCause(
                    type = EdiClassificationCauseType.RED_AREA,
                    axis = EdiAxis.AREAS,
                    resultingColorContribution = EdiGlobalTrafficLight.ROJO,
                    involvedCodes = redAreas.map { it.code },
                    description = "Grupo $groupNumber: ${redAreas.size} área(s) del desarrollo en ROJO (${redAreas.joinToString(", ") { it.code }})."
                )
            )
        }
        if (positiveAlaItems.isNotEmpty()) {
            redCauses.add(
                EdiClassificationCause(
                    type = EdiClassificationCauseType.POSITIVE_ALA,
                    axis = EdiAxis.ALA,
                    resultingColorContribution = EdiGlobalTrafficLight.ROJO,
                    involvedCodes = positiveAlaItems.map { it.itemCode },
                    description = "Grupo $groupNumber: ${positiveAlaItems.size} Señal(es) de Alarma (ALA) positiva(s) (${positiveAlaItems.joinToString(", ") { it.itemCode }})."
                )
            )
        }
        if (positiveEnItems.isNotEmpty()) {
            redCauses.add(
                EdiClassificationCause(
                    type = EdiClassificationCauseType.POSITIVE_EN,
                    axis = EdiAxis.EN,
                    resultingColorContribution = EdiGlobalTrafficLight.ROJO,
                    involvedCodes = positiveEnItems.map { it.itemCode },
                    description = "Grupo $groupNumber: ${positiveEnItems.size} alteración(es) en Exploración Neurológica (${positiveEnItems.joinToString(", ") { it.itemCode }})."
                )
            )
        }
        if (redCauses.isNotEmpty()) {
            return EdiGlobalTrafficLight.ROJO to redCauses
        }

        if (yellowAreas.isNotEmpty()) {
            return EdiGlobalTrafficLight.AMARILLO to listOf(
                EdiClassificationCause(
                    type = EdiClassificationCauseType.G5_TO_G15_ONE_OR_MORE_YELLOW_AREAS,
                    axis = EdiAxis.AREAS,
                    resultingColorContribution = EdiGlobalTrafficLight.AMARILLO,
                    involvedCodes = yellowAreas.map { it.code },
                    description = "Grupo $groupNumber: ${yellowAreas.size} área(s) del desarrollo en AMARILLO (${yellowAreas.joinToString(", ") { it.code }}); FRB y ALE no modifican el semáforo en Grupos 5–15."
                )
            )
        }

        return EdiGlobalTrafficLight.VERDE to listOf(
            EdiClassificationCause(
                type = EdiClassificationCauseType.ALL_CRITERIA_NORMAL_GREEN,
                axis = null,
                resultingColorContribution = EdiGlobalTrafficLight.VERDE,
                involvedCodes = emptyList(),
                description = "Grupo $groupNumber: todas las áreas aplicables en VERDE con EN y ALA negativos (FRB/ALE opcionales no alteran el resultado)."
            )
        )
    }

    private fun buildPriorityCriticalAlerts(
        positiveAlaItems: List<EdiCatalogItem>,
        positiveEnItems: List<EdiCatalogItem>,
        redAreas: List<EdiDevArea>
    ): List<EdiPriorityCriticalAlert> {
        val alerts = mutableListOf<EdiPriorityCriticalAlert>()
        positiveAlaItems.forEach { item ->
            alerts.add(
                EdiPriorityCriticalAlert(
                    type = EdiPriorityCriticalAlertType.POSITIVE_ALARM_SIGNAL_ALA,
                    axis = EdiAxis.ALA,
                    code = item.itemCode,
                    message = "ADVERTENCIA CLÍNICA PRIORITARIA: Señal de Alarma positiva (${item.itemCode})."
                )
            )
        }
        positiveEnItems.forEach { item ->
            alerts.add(
                EdiPriorityCriticalAlert(
                    type = EdiPriorityCriticalAlertType.ALTERED_NEUROLOGICAL_EXAM_EN,
                    axis = EdiAxis.EN,
                    code = item.itemCode,
                    message = "ADVERTENCIA CLÍNICA PRIORITARIA: Alteración en Exploración Neurológica (${item.itemCode})."
                )
            )
        }
        redAreas.forEach { area ->
            alerts.add(
                EdiPriorityCriticalAlert(
                    type = EdiPriorityCriticalAlertType.RED_DEVELOPMENT_AREA,
                    axis = EdiAxis.AREAS,
                    code = area.code,
                    message = "ADVERTENCIA CLÍNICA PRIORITARIA: Área ${area.officialTitle} (${area.code}) cerrada en ROJO."
                )
            )
        }
        return alerts
    }

    private fun buildBlockedCauses(
        sessionGroupMismatch: Boolean,
        blockedByAgeDiscrepancy: Boolean,
        blockedByOutOfRangeAge: Boolean,
        missingMaterialCodes: List<String>,
        discrepancyCodes: List<String>,
        pendingMandatoryCodes: List<String>,
        blockingReasons: List<String>
    ): List<EdiClassificationCause> {
        val causes = mutableListOf<EdiClassificationCause>()
        if (sessionGroupMismatch) {
            causes.add(
                EdiClassificationCause(
                    type = EdiClassificationCauseType.BLOCKED_SESSION_GROUP_MISMATCH,
                    axis = null,
                    resultingColorContribution = EdiGlobalTrafficLight.INCOMPLETE,
                    involvedCodes = emptyList(),
                    description = blockingReasons.first()
                )
            )
        }
        if (blockedByAgeDiscrepancy) {
            causes.add(
                EdiClassificationCause(
                    type = EdiClassificationCauseType.BLOCKED_AGE_DISCREPANCY,
                    axis = null,
                    resultingColorContribution = EdiGlobalTrafficLight.INCOMPLETE,
                    involvedCodes = emptyList(),
                    description = "Asignación automática de grupo bloqueada por discrepancia entre método manual EDI y calendario CRED."
                )
            )
        }
        if (blockedByOutOfRangeAge) {
            causes.add(
                EdiClassificationCause(
                    type = EdiClassificationCauseType.BLOCKED_OUT_OF_AGE_RANGE,
                    axis = null,
                    resultingColorContribution = EdiGlobalTrafficLight.INCOMPLETE,
                    involvedCodes = emptyList(),
                    description = "Edad fuera del rango oficial EDI (1m 0d a 71m 29d) o fechas inválidas."
                )
            )
        }
        if (missingMaterialCodes.isNotEmpty()) {
            causes.add(
                EdiClassificationCause(
                    type = EdiClassificationCauseType.BLOCKED_MISSING_MATERIAL,
                    axis = null,
                    resultingColorContribution = EdiGlobalTrafficLight.INCOMPLETE,
                    involvedCodes = missingMaterialCodes,
                    description = "Falta de material oficial o lámina en: ${missingMaterialCodes.joinToString(", ")}."
                )
            )
        }
        if (discrepancyCodes.isNotEmpty()) {
            causes.add(
                EdiClassificationCause(
                    type = EdiClassificationCauseType.BLOCKED_MODALITY_DISCREPANCY,
                    axis = null,
                    resultingColorContribution = EdiGlobalTrafficLight.INCOMPLETE,
                    involvedCodes = discrepancyCodes,
                    description = "Discrepancia entre observación y entrevista pendiente de resolución humana en: ${discrepancyCodes.joinToString(", ")}."
                )
            )
        }
        if (pendingMandatoryCodes.isNotEmpty()) {
            causes.add(
                EdiClassificationCause(
                    type = EdiClassificationCauseType.BLOCKED_INCOMPLETE_MANDATORY_ITEMS,
                    axis = null,
                    resultingColorContribution = EdiGlobalTrafficLight.INCOMPLETE,
                    involvedCodes = pendingMandatoryCodes,
                    description = "Reactivos obligatorios sin responder: ${pendingMandatoryCodes.joinToString(", ")}."
                )
            )
        }
        return causes
    }

    private fun buildIncompleteSummaryText(
        groupNumber: Int,
        blockingReasons: List<String>,
        priorityCriticalAlerts: List<EdiPriorityCriticalAlert>
    ): String {
        val base = "Grupo $groupNumber: EVALUACIÓN INCOMPLETA (sin semáforo definitivo). ${blockingReasons.joinToString(" ")}"
        return if (priorityCriticalAlerts.isNotEmpty()) {
            "$base | ALERTAS CRÍTICAS DETECTADAS: ${priorityCriticalAlerts.joinToString(" | ") { it.message }}"
        } else {
            base
        }
    }

    private fun buildDefinitiveSummaryText(
        groupNumber: Int,
        trafficLight: EdiGlobalTrafficLight,
        causes: List<EdiClassificationCause>,
        nonBlockingWarnings: List<String> = emptyList()
    ): String {
        val base = "Grupo $groupNumber — ${trafficLight.displayLabel}: ${causes.joinToString(" ") { it.description }}"
        return if (nonBlockingWarnings.isNotEmpty()) {
            "$base | Advertencias opcionales: ${nonBlockingWarnings.joinToString(" ")}"
        } else {
            base
        }
    }
}
