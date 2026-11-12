package com.example.model

/**
 * Motor Clínico de Calificación por Área y Reglas de Retroceso EDI (Fase 2)
 * Manual para la Aplicación de la Prueba de Evaluación del Desarrollo Infantil (EDI)
 * Versión Peruana - Primera Edición 2026 (MINSA / UNICEF)
 *
 * Alcance estricto de Fase 2:
 * - Evalúa únicamente el Eje 3 (Áreas del Desarrollo: MG, MF, LE, SO, CO) y sus retrocesos.
 * - Mantiene respuestas actuales y de retroceso completamente separadas.
 * - No implementa todavía el semáforo global de la Fase 3 ni modifica la interfaz actual.
 */

enum class EdiAreaColorResult(
    val displayLabel: String,
    val isDefinitiveColor: Boolean
) {
    VERDE("Verde", true),
    AMARILLO("Amarillo", true),
    ROJO("Rojo", true),
    INCOMPLETE("Área incompleta", false),
    NOT_APPLICABLE("No aplica", false)
}

/**
 * Representa un reactivo específico dentro de la secuencia de retroceso de un grupo.
 * Su identificador [fallbackUniqueId] está estrictamente separado del mapa de reactivos actuales.
 */
data class EdiFallbackItemSpec(
    val fallbackUniqueId: String,
    val targetGroupNumber: Int,
    val sourceGroupNumber: Int,
    val area: EdiDevArea,
    val sourceItem: EdiCatalogItem,
    val isSpecialCoRItem: Boolean = false
)

/**
 * Resultado auditable de la evaluación de una sola área del desarrollo para un grupo dado.
 */
data class EdiAreaEvaluationResult(
    val groupNumber: Int,
    val area: EdiDevArea,
    val status: EdiAreaColorResult,
    val appliesToGroup: Boolean,
    val currentItems: List<EdiCatalogItem>,
    val currentItemResponses: Map<String, EdiItemResponse>,
    val currentYesCount: Int,
    val currentNoCount: Int,
    val currentPendingCount: Int,
    val isCurrentSectionComplete: Boolean,
    val fallbackRequired: Boolean,
    val fallbackSourceGroupNumber: Int?,
    val usesSpecialCoRFallback: Boolean,
    val fallbackItems: List<EdiFallbackItemSpec>,
    val activeFallbackResponses: Map<String, EdiItemResponse>,
    val preservedHistoricalFallbackResponses: Map<String, EdiItemResponse>,
    val fallbackIgnoredDueToCurrentChange: Boolean,
    val fallbackYesCount: Int,
    val fallbackNoCount: Int,
    val fallbackPendingCount: Int,
    val isFallbackSectionComplete: Boolean,
    val clinicalRuleSummary: String
)

/**
 * Resumen de calificación de las 5 áreas del Eje 3 para un grupo EDI.
 * No emite semáforo global (reservado para Fase 3).
 */
data class EdiAxis3AreasSummary(
    val groupNumber: Int,
    val areaResults: Map<EdiDevArea, EdiAreaEvaluationResult>,
    val areasRequiringFallback: List<EdiDevArea>,
    val completeApplicableAreasCount: Int,
    val incompleteApplicableAreasCount: Int,
    val allApplicableAreasComplete: Boolean
)

object EdiAreaFallbackEngine {

    /**
     * Obtiene la lista oficial de reactivos de retroceso correspondientes a un [groupNumber] y [area].
     *
     * Reglas de origen:
     * - Grupo 1: sin retroceso.
     * - Grupos 1–12 área CO: no aplica (sin retroceso).
     * - Grupos 2–7: 2 reactivos del grupo anterior (G1..G6).
     * - Grupo 8 (excepción oficial): retrocede a los 2 reactivos del Grupo 7.
     * - Grupos 9–12: 3 reactivos del grupo anterior (G8..G11).
     * - Grupo 13: MG, MF, LE, SO retroceden a 3 reactivos de G12; CO retrocede exclusivamente
     *   a los 3 reactivos especiales CO-R 13.1, CO-R 13.2 y CO-R 13.3.
     * - Grupos 14–15: 3 reactivos del grupo anterior en las 5 áreas (incluyendo CO).
     */
    fun getFallbackItemsForArea(
        groupNumber: Int,
        area: EdiDevArea
    ): List<EdiFallbackItemSpec> {
        val groupSpec = EdiOfficialCatalog.getGroup(groupNumber)
        if (groupNumber == 1) return emptyList()
        if (!groupSpec.conocimientoApplies && area == EdiDevArea.CO) return emptyList()

        if (groupNumber == 13 && area == EdiDevArea.CO && groupSpec.usesSpecialCoRFallback) {
            return EdiOfficialCatalog.specialGroup13CoRFallbackItems.map { corItem ->
                EdiFallbackItemSpec(
                    fallbackUniqueId = corItem.uniqueId,
                    targetGroupNumber = 13,
                    sourceGroupNumber = 13,
                    area = EdiDevArea.CO,
                    sourceItem = corItem,
                    isSpecialCoRItem = true
                )
            }
        }

        val prevGroupNumber = groupSpec.fallbackPreviousGroupNumber ?: return emptyList()
        val prevGroupSpec = EdiOfficialCatalog.getGroup(prevGroupNumber)
        val sourceItems = prevGroupSpec.areaItems[area].orEmpty()

        return sourceItems.map { prevItem ->
            EdiFallbackItemSpec(
                fallbackUniqueId = buildFallbackUniqueId(
                    targetGroupNumber = groupNumber,
                    sourceGroupNumber = prevGroupNumber,
                    area = area,
                    itemIndexInSection = prevItem.itemIndexInSection
                ),
                targetGroupNumber = groupNumber,
                sourceGroupNumber = prevGroupNumber,
                area = area,
                sourceItem = prevItem,
                isSpecialCoRItem = false
            )
        }
    }

    fun getAllFallbackItemsForGroup(groupNumber: Int): List<EdiFallbackItemSpec> =
        EdiDevArea.entries.flatMap { area -> getFallbackItemsForArea(groupNumber, area) }

    fun findFallbackItemSpec(groupNumber: Int, fallbackUniqueId: String): EdiFallbackItemSpec? =
        getAllFallbackItemsForGroup(groupNumber).firstOrNull { it.fallbackUniqueId == fallbackUniqueId }

    fun buildFallbackUniqueId(
        targetGroupNumber: Int,
        sourceGroupNumber: Int,
        area: EdiDevArea,
        itemIndexInSection: Int
    ): String = "G${targetGroupNumber}_RETRO_G${sourceGroupNumber}_${area.code}_$itemIndexInSection"

    /**
     * Registra de forma segura una respuesta de retroceso en [EdiGroupSessionSnapshot.fallbackGroupResponses],
     * validando que pertenezca al grupo activo de la sesión y sin alterar [currentGroupResponses].
     */
    fun recordValidatedFallbackResponse(
        state: EdiPhase1EngineState,
        fallbackUniqueId: String,
        response: EdiItemResponse
    ): EdiPhase1EngineState {
        val spec = findFallbackItemSpec(state.activeGroupNumber, fallbackUniqueId)
            ?: return state
        val effectiveResponse = if (
            response == EdiItemResponse.NOT_EVALUATED_MISSING_MATERIAL &&
            !spec.sourceItem.requiresMaterialInProtocol
        ) {
            EdiItemResponse.UNANSWERED
        } else {
            response
        }
        return EdiPhase1SessionManager.recordFallbackItemResponse(
            state = state,
            fallbackItemUniqueId = fallbackUniqueId,
            response = effectiveResponse
        )
    }

    /**
     * Registra evidencia dual (observación/entrevista) para un reactivo de retroceso que emplee
     * protocolo combinado o entrevista principal (por ejemplo, LE 13.3 en retroceso de G14 o
     * LE 14.3 en retroceso de G15), almacenando la resolución exclusivamente en retroceso.
     */
    fun recordDualModalityFallbackEvidence(
        state: EdiPhase1EngineState,
        fallbackUniqueId: String,
        observationEvidence: Boolean?,
        interviewEvidence: Boolean?,
        humanResolutionIfConflict: Boolean? = null,
        note: String = ""
    ): EdiPhase1EngineState {
        val spec = findFallbackItemSpec(state.activeGroupNumber, fallbackUniqueId)
            ?: return state
        val record = EdiDualEvidenceRecord(
            itemUniqueId = fallbackUniqueId,
            modality = spec.sourceItem.modality,
            directObservationEvidence = observationEvidence,
            directedInterviewEvidence = interviewEvidence,
            professionalHumanResolution = humanResolutionIfConflict,
            clinicalJustificationNote = note
        )
        val resolvedStatus = record.resolveItemResponse()
        val s = state.activeSession
        return state.copy(
            activeSession = s.copy(
                fallbackGroupResponses = s.fallbackGroupResponses + (fallbackUniqueId to resolvedStatus),
                dualEvidenceRecords = s.dualEvidenceRecords + (fallbackUniqueId to record)
            )
        )
    }

    private fun resolveEffectiveResponse(
        itemUniqueId: String,
        responseMap: Map<String, EdiItemResponse>,
        dualEvidenceRecords: Map<String, EdiDualEvidenceRecord>
    ): EdiItemResponse {
        val direct = responseMap[itemUniqueId] ?: EdiItemResponse.UNANSWERED
        if (direct == EdiItemResponse.NOT_EVALUATED_MISSING_MATERIAL) {
            return EdiItemResponse.NOT_EVALUATED_MISSING_MATERIAL
        }
        val dualRecord = dualEvidenceRecords[itemUniqueId]
        return dualRecord?.resolveItemResponse() ?: direct
    }

    /**
     * Evalúa una sola área del desarrollo para el [groupNumber] especificado utilizando la [session].
     *
     * Controles de seguridad implementados:
     * 1. En Grupos 1–12, CO devuelve siempre NOT_APPLICABLE (nunca ROJO ni INCOMPLETE).
     * 2. Si la sesión no pertenece al [groupNumber] consultado, no reutiliza respuestas ajenas.
     * 3. UNANSWERED, NOT_EVALUATED_MISSING_MATERIAL y DISCREPANCY_REQUIRES_HUMAN_RESOLUTION nunca
     *    se interpretan como NO.
     * 4. Cualquier respuesta obligatoria faltante en la sección actual o en un retroceso activo
     *    devuelve EdiAreaColorResult.INCOMPLETE.
     * 5. El retroceso sólo se activa en G2–G15 cuando TODOS los reactivos actuales están
     *    respondidos explícitamente como NO.
     * 6. Si una respuesta actual cambia y ya no corresponde retroceso, las respuestas previas
     *    en [fallbackGroupResponses] se ignoran en la calificación sin eliminarse del historial.
     */
    fun evaluateArea(
        groupNumber: Int,
        area: EdiDevArea,
        session: EdiGroupSessionSnapshot
    ): EdiAreaEvaluationResult {
        val groupSpec = EdiOfficialCatalog.getGroup(groupNumber)
        val currentItems = groupSpec.areaItems[area].orEmpty()

        // Control: En Grupos 1–12, CO permanece NO APLICA (nunca ROJO ni INCOMPLETO)
        if (currentItems.isEmpty()) {
            return EdiAreaEvaluationResult(
                groupNumber = groupNumber,
                area = area,
                status = EdiAreaColorResult.NOT_APPLICABLE,
                appliesToGroup = false,
                currentItems = emptyList(),
                currentItemResponses = emptyMap(),
                currentYesCount = 0,
                currentNoCount = 0,
                currentPendingCount = 0,
                isCurrentSectionComplete = true,
                fallbackRequired = false,
                fallbackSourceGroupNumber = null,
                usesSpecialCoRFallback = false,
                fallbackItems = emptyList(),
                activeFallbackResponses = emptyMap(),
                preservedHistoricalFallbackResponses = emptyMap(),
                fallbackIgnoredDueToCurrentChange = false,
                fallbackYesCount = 0,
                fallbackNoCount = 0,
                fallbackPendingCount = 0,
                isFallbackSectionComplete = true,
                clinicalRuleSummary = "${area.officialTitle}: No aplica para el Grupo $groupNumber."
            )
        }

        // Aislamiento estricto: jamás reutilizar respuestas si la sesión corresponde a otro grupo
        val safeCurrentMap = if (session.groupNumber == groupNumber) session.currentGroupResponses else emptyMap()
        val safeFallbackMap = if (session.groupNumber == groupNumber) session.fallbackGroupResponses else emptyMap()
        val safeDualMap = if (session.groupNumber == groupNumber) session.dualEvidenceRecords else emptyMap()

        val resolvedCurrentResponses = linkedMapOf<String, EdiItemResponse>()
        currentItems.forEach { item ->
            resolvedCurrentResponses[item.uniqueId] = resolveEffectiveResponse(
                itemUniqueId = item.uniqueId,
                responseMap = safeCurrentMap,
                dualEvidenceRecords = safeDualMap
            )
        }

        val currentYes = resolvedCurrentResponses.values.count { it == EdiItemResponse.YES }
        val currentNo = resolvedCurrentResponses.values.count { it == EdiItemResponse.NO }
        val currentPending = resolvedCurrentResponses.values.count { !it.isAnsweredDefinitively }
        val isCurrentComplete = currentPending == 0

        val allFallbackSpecs = getFallbackItemsForArea(groupNumber, area)
        val preservedHistoricalFallback = linkedMapOf<String, EdiItemResponse>()
        allFallbackSpecs.forEach { fbSpec ->
            val recorded = resolveEffectiveResponse(
                itemUniqueId = fbSpec.fallbackUniqueId,
                responseMap = safeFallbackMap,
                dualEvidenceRecords = safeDualMap
            )
            if (recorded != EdiItemResponse.UNANSWERED) {
                preservedHistoricalFallback[fbSpec.fallbackUniqueId] = recorded
            }
        }

        // El retroceso solo se activa en Grupos 2..15 cuando TODOS los reactivos actuales están explícitamente en NO
        val fallbackTriggered = groupNumber > 1 &&
            isCurrentComplete &&
            currentNo == currentItems.size

        if (!fallbackTriggered) {
            val ignoredDueToChange = preservedHistoricalFallback.isNotEmpty()
            val status = if (!isCurrentComplete) {
                EdiAreaColorResult.INCOMPLETE
            } else {
                classifyWithoutFallback(
                    groupNumber = groupNumber,
                    totalItems = currentItems.size,
                    yesCount = currentYes
                )
            }
            val summary = buildNonFallbackSummary(
                groupNumber = groupNumber,
                area = area,
                status = status,
                yesCount = currentYes,
                totalCount = currentItems.size,
                pendingCount = currentPending,
                ignoredHistoricalFallback = ignoredDueToChange
            )
            return EdiAreaEvaluationResult(
                groupNumber = groupNumber,
                area = area,
                status = status,
                appliesToGroup = true,
                currentItems = currentItems,
                currentItemResponses = resolvedCurrentResponses,
                currentYesCount = currentYes,
                currentNoCount = currentNo,
                currentPendingCount = currentPending,
                isCurrentSectionComplete = isCurrentComplete,
                fallbackRequired = false,
                fallbackSourceGroupNumber = null,
                usesSpecialCoRFallback = false,
                fallbackItems = allFallbackSpecs,
                activeFallbackResponses = emptyMap(),
                preservedHistoricalFallbackResponses = preservedHistoricalFallback,
                fallbackIgnoredDueToCurrentChange = ignoredDueToChange,
                fallbackYesCount = 0,
                fallbackNoCount = 0,
                fallbackPendingCount = 0,
                isFallbackSectionComplete = true,
                clinicalRuleSummary = summary
            )
        }

        // Retroceso activado (0 SÍ / todos NO explícitos en grupo 2..15)
        val activeFallbackResponses = linkedMapOf<String, EdiItemResponse>()
        allFallbackSpecs.forEach { fbSpec ->
            activeFallbackResponses[fbSpec.fallbackUniqueId] = resolveEffectiveResponse(
                itemUniqueId = fbSpec.fallbackUniqueId,
                responseMap = safeFallbackMap,
                dualEvidenceRecords = safeDualMap
            )
        }

        val fbYes = activeFallbackResponses.values.count { it == EdiItemResponse.YES }
        val fbNo = activeFallbackResponses.values.count { it == EdiItemResponse.NO }
        val fbPending = activeFallbackResponses.values.count { !it.isAnsweredDefinitively }
        val isFbComplete = fbPending == 0 && allFallbackSpecs.isNotEmpty()

        val usesCoR = groupNumber == 13 && area == EdiDevArea.CO
        val sourceGroupNumber = allFallbackSpecs.firstOrNull()?.sourceGroupNumber

        val finalStatus = if (!isFbComplete) {
            EdiAreaColorResult.INCOMPLETE
        } else {
            classifyFallbackResult(
                fallbackTotalItems = allFallbackSpecs.size,
                fallbackYesCount = fbYes
            )
        }

        val fbSourceLabel = if (usesCoR) {
            "reactivos especiales CO-R 13.1–13.3"
        } else {
            "Grupo ${sourceGroupNumber ?: (groupNumber - 1)} (${allFallbackSpecs.size} reactivos)"
        }
        val summary = when (finalStatus) {
            EdiAreaColorResult.INCOMPLETE ->
                "${area.officialTitle} (G$groupNumber): 0/${currentItems.size} SÍ en grupo actual; requiere completar retroceso en $fbSourceLabel ($fbPending pendiente(s))."
            EdiAreaColorResult.AMARILLO ->
                "${area.officialTitle} (G$groupNumber): 0/${currentItems.size} SÍ actuales -> retroceso a $fbSourceLabel con $fbYes/${allFallbackSpecs.size} SÍ = AMARILLO."
            EdiAreaColorResult.ROJO ->
                "${area.officialTitle} (G$groupNumber): 0/${currentItems.size} SÍ actuales -> retroceso a $fbSourceLabel con $fbYes/${allFallbackSpecs.size} SÍ = ROJO."
            else ->
                "${area.officialTitle} (G$groupNumber): retroceso evaluado."
        }

        return EdiAreaEvaluationResult(
            groupNumber = groupNumber,
            area = area,
            status = finalStatus,
            appliesToGroup = true,
            currentItems = currentItems,
            currentItemResponses = resolvedCurrentResponses,
            currentYesCount = currentYes,
            currentNoCount = currentNo,
            currentPendingCount = currentPending,
            isCurrentSectionComplete = isCurrentComplete,
            fallbackRequired = true,
            fallbackSourceGroupNumber = sourceGroupNumber,
            usesSpecialCoRFallback = usesCoR,
            fallbackItems = allFallbackSpecs,
            activeFallbackResponses = activeFallbackResponses,
            preservedHistoricalFallbackResponses = preservedHistoricalFallback,
            fallbackIgnoredDueToCurrentChange = false,
            fallbackYesCount = fbYes,
            fallbackNoCount = fbNo,
            fallbackPendingCount = fbPending,
            isFallbackSectionComplete = isFbComplete,
            clinicalRuleSummary = summary
        )
    }

    /**
     * Evalúa simultáneamente las 5 áreas del desarrollo para el [groupNumber] dado.
     */
    fun evaluateAllAreasForGroup(
        groupNumber: Int,
        session: EdiGroupSessionSnapshot
    ): EdiAxis3AreasSummary {
        val results = linkedMapOf<EdiDevArea, EdiAreaEvaluationResult>()
        EdiDevArea.entries.forEach { area ->
            results[area] = evaluateArea(groupNumber, area, session)
        }
        val applicable = results.values.filter { it.appliesToGroup }
        val completeCount = applicable.count { it.status.isDefinitiveColor }
        val incompleteCount = applicable.count { it.status == EdiAreaColorResult.INCOMPLETE }
        val fallbackAreas = applicable.filter { it.fallbackRequired }.map { it.area }

        return EdiAxis3AreasSummary(
            groupNumber = groupNumber,
            areaResults = results,
            areasRequiringFallback = fallbackAreas,
            completeApplicableAreasCount = completeCount,
            incompleteApplicableAreasCount = incompleteCount,
            allApplicableAreasComplete = incompleteCount == 0
        )
    }

    private fun classifyWithoutFallback(
        groupNumber: Int,
        totalItems: Int,
        yesCount: Int
    ): EdiAreaColorResult {
        return if (groupNumber == 1) {
            // Grupo 1: 2 SÍ = VERDE; 1 o 0 SÍ = ROJO (sin retroceso)
            if (yesCount == totalItems) EdiAreaColorResult.VERDE else EdiAreaColorResult.ROJO
        } else if (totalItems == 2) {
            // Grupos 2 a 7: 2 SÍ = VERDE; 1 SÍ = AMARILLO
            when (yesCount) {
                2 -> EdiAreaColorResult.VERDE
                1 -> EdiAreaColorResult.AMARILLO
                else -> EdiAreaColorResult.ROJO
            }
        } else {
            // Grupos 8 a 15 (3 reactivos actuales): 3 o 2 SÍ = VERDE; 1 SÍ = AMARILLO
            when {
                yesCount >= 2 -> EdiAreaColorResult.VERDE
                yesCount == 1 -> EdiAreaColorResult.AMARILLO
                else -> EdiAreaColorResult.ROJO
            }
        }
    }

    private fun classifyFallbackResult(
        fallbackTotalItems: Int,
        fallbackYesCount: Int
    ): EdiAreaColorResult {
        return when (fallbackTotalItems) {
            // Retroceso de 2 reactivos (Grupos 2–7 hacia G1–G6, y excepción Grupo 8 hacia Grupo 7):
            // 2/2 SÍ = AMARILLO; 1/2 o 0/2 SÍ = ROJO
            2 -> if (fallbackYesCount == 2) EdiAreaColorResult.AMARILLO else EdiAreaColorResult.ROJO

            // Retroceso de 3 reactivos (Grupos 9–12, Grupo 13 MG..SO y CO-R, Grupos 14–15):
            // 3 o 2 SÍ = AMARILLO; 1 o 0 SÍ = ROJO
            3 -> if (fallbackYesCount >= 2) EdiAreaColorResult.AMARILLO else EdiAreaColorResult.ROJO

            else -> EdiAreaColorResult.INCOMPLETE
        }
    }

    private fun buildNonFallbackSummary(
        groupNumber: Int,
        area: EdiDevArea,
        status: EdiAreaColorResult,
        yesCount: Int,
        totalCount: Int,
        pendingCount: Int,
        ignoredHistoricalFallback: Boolean
    ): String {
        val base = when (status) {
            EdiAreaColorResult.INCOMPLETE ->
                "${area.officialTitle} (G$groupNumber): Área incompleta ($pendingCount reactivo(s) pendiente(s))."
            EdiAreaColorResult.VERDE ->
                "${area.officialTitle} (G$groupNumber): $yesCount/$totalCount SÍ = VERDE."
            EdiAreaColorResult.AMARILLO ->
                "${area.officialTitle} (G$groupNumber): $yesCount/$totalCount SÍ = AMARILLO."
            EdiAreaColorResult.ROJO ->
                "${area.officialTitle} (G$groupNumber): $yesCount/$totalCount SÍ = ROJO (sin retroceso en Grupo 1)."
            EdiAreaColorResult.NOT_APPLICABLE ->
                "${area.officialTitle} (G$groupNumber): No aplica."
        }
        return if (ignoredHistoricalFallback) {
            "$base Historial previo de retroceso conservado pero ignorado al cambiar respuesta actual."
        } else {
            base
        }
    }
}
