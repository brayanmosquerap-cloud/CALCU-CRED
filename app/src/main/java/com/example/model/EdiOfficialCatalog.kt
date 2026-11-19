package com.example.model

/**
 * Catálogo Oficial y Base del Motor Clínico EDI (Fase 1)
 * Manual para la Aplicación de la Prueba de Evaluación del Desarrollo Infantil (EDI)
 * Versión Peruana - Primera Edición 2026 (MINSA / UNICEF)
 *
 * Este módulo es independiente de la interfaz y mantiene intacto ClinicalAgeCalculator.calculateAges.
 */

enum class EdiAxis(
    val number: Int,
    val code: String,
    val officialTitle: String
) {
    FRB(1, "FRB", "Factores de Riesgo Biológico"),
    ALE(2, "ALE", "Señales de Alerta"),
    AREAS(3, "AREAS", "Áreas del Desarrollo"),
    EN(4, "EN", "Exploración Neurológica"),
    ALA(5, "ALA", "Señales de Alarma")
}

enum class EdiDevArea(
    val code: String,
    val officialTitle: String
) {
    MG("MG", "Motriz Gruesa"),
    MF("MF", "Motriz Fina"),
    LE("LE", "Lenguaje"),
    SO("SO", "Social"),
    CO("CO", "Conocimiento")
}

/**
 * Estados individuales de cada reactivo clínico EDI.
 * Nunca se asume que un ítem sin responder equivale a SÍ o NO.
 */
enum class EdiItemResponse(val displayLabel: String, val isAnsweredDefinitively: Boolean) {
    UNANSWERED("Sin responder", false),
    YES("Sí", true),
    NO("No", true),
    NOT_EVALUATED_MISSING_MATERIAL("No evaluado (falta material)", false),
    DISCREPANCY_REQUIRES_HUMAN_RESOLUTION("Discrepancia (requiere revisión profesional)", false)
}

/**
 * Modalidad clínica de aplicación de cada reactivo según el Manual EDI Peruano 2026
 * y el protocolo interno de auditoría para ALE 12.3, LE 13.3 y LE 14.3.
 */
enum class EdiClinicalModality(val description: String) {
    DIRECTED_INTERVIEW("Pregunta dirigida a madre, padre o cuidador primario"),
    DIRECT_OBSERVATION("Observación directa del niño (** en el manual)"),
    DUAL_OBSERVATION_AND_INTERVIEW_ALE_12_3(
        "Protocolo dual obligatorio (ALE 12.3): requiere observación directa y entrevista complementaria"
    ),
    INTERVIEW_PRIMARY_WITH_OBSERVATION_COMPLEMENT(
        "Protocolo entrevista principal (LE 13.3 / LE 14.3): emplea entrevista como modalidad principal con observación complementaria"
    )
}

/**
 * Láminas oficiales del Manual EDI (págs. 114-117).
 */
enum class EdiOfficialPlate(
    val code: String,
    val title: String,
    val manualPage: Int,
    val description: String
) {
    LAMINA_A(
        code = "A",
        title = "Lámina A",
        manualPage = 114,
        description = "6 dibujos de objetos cotidianos: flor, oso, pelota, taza, perro y carro."
    ),
    LAMINA_B(
        code = "B",
        title = "Lámina B",
        manualPage = 115,
        description = "Singular vs. plural: 1 perro / 2 perros."
    ),
    LAMINA_C(
        code = "C",
        title = "Lámina C",
        manualPage = 116,
        description = "Singular vs. plural: 1 pelota / 2 pelotas."
    ),
    LAMINA_D(
        code = "D",
        title = "Lámina D",
        manualPage = 117,
        description = "Singular vs. plural: 1 niño / 2 niños."
    )
}

/**
 * Representa un reactivo individual oficial del Manual EDI 2026.
 *
 * Se diferencia explícitamente:
 * - [generalHeaderMaterials]: materiales listados de manera general en el encabezado del grupo
 *   (por ejemplo, "Lámina A" listada para ALE 9.1 en el encabezado de la pág. 83).
 * - [protocolRequiredMaterials] y [protocolRequiredPlates]: materiales y láminas utilizados
 *   directamente durante la ejecución del protocolo clínico del reactivo.
 */
data class EdiCatalogItem(
    val uniqueId: String,
    val groupNumber: Int,
    val axis: EdiAxis,
    val area: EdiDevArea? = null,
    val itemCode: String,
    val itemIndexInSection: Int,
    val hasObservationAsterisks: Boolean,
    val formatQuestionText: String,
    val consultationQuestionText: String,
    val clinicalInstruction: String,
    val modality: EdiClinicalModality,
    val generalHeaderMaterials: List<String> = emptyList(),
    val protocolRequiredMaterials: List<String> = emptyList(),
    val protocolRequiredPlates: Set<EdiOfficialPlate> = emptySet(),
    val yesIsNormalGreen: Boolean,
    val isSpecialFallbackItem: Boolean = false
) {
    /**
     * Indica si el protocolo clínico de este reactivo depende directamente de algún material
     * físico o lámina oficial para poder ejecutarse.
     *
     * Ejemplo clave (ALE 9.1): Aunque el encabezado general del Grupo 9 (pág. 83) menciona
     * "ALE 9.1 y LE 9.3: Lámina A", el protocolo clínico de ALE 9.1 evalúa señalar a papá,
     * mamá o cuidador presente en el consultorio y NO utiliza la Lámina A. Por tanto, en ALE 9.1
     * [requiresMaterialInProtocol] es `false` y [requiresPlateInProtocol] es `false`.
     */
    val requiresMaterialInProtocol: Boolean
        get() = protocolRequiredMaterials.isNotEmpty() || protocolRequiredPlates.isNotEmpty()

    fun requiresPlateInProtocol(plate: EdiOfficialPlate): Boolean =
        plate in protocolRequiredPlates

    fun isListedWithHeaderMaterialOnly(): Boolean =
        generalHeaderMaterials.isNotEmpty() && !requiresMaterialInProtocol
}

/**
 * Especificación oficial de cada uno de los 15 grupos de edad EDI (págs. 17-111).
 */
data class EdiGroupSpec(
    val groupNumber: Int,
    val code: String,
    val officialTitle: String,
    val shortIntervalLabel: String,
    val minMonthsInclusive: Int,
    val minDaysInclusive: Int = 0,
    val maxMonthsInclusive: Int,
    val maxDaysInclusive: Int = 29,
    val formatPages: IntRange,
    val consultationPages: IntRange,
    val generalHeaderMaterialsText: String,
    val itemsPerActiveArea: Int,
    val fallbackPreviousGroupNumber: Int?,
    val fallbackItemsCountPerArea: Int,
    val usesSpecialCoRFallback: Boolean = false,
    val frbAndAleModifyGlobalTrafficLight: Boolean,
    val neurologicalEn13Applies: Boolean = true,
    val areaItems: Map<EdiDevArea, List<EdiCatalogItem>>,
    val aleItems: List<EdiCatalogItem>,
    val alaItems: List<EdiCatalogItem>
) {
    val activeDevelopmentalAreas: List<EdiDevArea>
        get() = EdiDevArea.entries.filter { areaItems[it].orEmpty().isNotEmpty() }

    val conocimientoApplies: Boolean
        get() = areaItems[EdiDevArea.CO].orEmpty().isNotEmpty()

    val hasAlarmSignals: Boolean
        get() = alaItems.isNotEmpty()
}

/**
 * Edad gestacional exacta en semanas y días, sin redondeos.
 * Cumple Control 2: conserva el dato original y documenta el cálculo de prematuridad.
 */
data class GestationalAgeInput(
    val weeks: Int,
    val days: Int = 0
) {
    val safeWeeks: Int = weeks.coerceIn(20, 44)
    val safeDays: Int = days.coerceIn(0, 6)
    val originalLabel: String = "${safeWeeks}s ${safeDays}d"
    val totalGestationalDays: Int = safeWeeks * 7 + safeDays
    val isPreterm: Boolean = safeWeeks < 37
    val prematurityDaysTo40Weeks: Int =
        if (isPreterm) (280 - totalGestationalDays).coerceAtLeast(0) else 0
    val formulaDocumentation: String =
        "Días de prematuridad = 280 - (${safeWeeks} × 7 + ${safeDays}) = 280 - $totalGestationalDays = $prematurityDaysTo40Weeks días"
}

/**
 * Desglose de edad (años, meses, días y meses totales acumulados).
 */
data class EdiAgeBreakdown(
    val years: Int,
    val months: Int,
    val days: Int,
    val borrowStepsDescription: List<String> = emptyList()
) {
    val totalMonths: Int
        get() = years * 12 + months

    val formattedAMD: String
        get() = "${years}a ${months}m ${days}d"

    val formattedMonthsDays: String
        get() = "${totalMonths}m ${days}d"
}

enum class EdiAgeRangeStatus {
    UNDER_MIN_AGE_LESS_THAN_1_MONTH,
    IN_OFFICIAL_RANGE_G1_TO_G15,
    OVER_MAX_AGE_72_MONTHS_OR_MORE,
    INVALID_DATE_INPUT
}

data class EdiSingleMethodGroupResolution(
    val methodLabel: String,
    val chronologicalAge: EdiAgeBreakdown,
    val correctedAge: EdiAgeBreakdown,
    val appliesCorrectedAge: Boolean,
    val effectiveAgeUsed: EdiAgeBreakdown,
    val rangeStatus: EdiAgeRangeStatus,
    val assignedGroup: EdiGroupSpec?
)

enum class EdiGroupAutoSelectionStatus {
    AUTO_SELECTED_CONSISTENT,
    BLOCKED_GROUP_DISCREPANCY_REQUIRES_PROFESSIONAL_REVIEW,
    OUT_OF_RANGE_UNDER_1_MONTH,
    OUT_OF_RANGE_72_MONTHS_OR_MORE,
    INVALID_DATES
}

/**
 * Resultado de la comparación aislada entre el método del Manual EDI (préstamos de 30d/12m)
 * y el método de calendario CRED (ClinicalAgeCalculator.calculateAges).
 * Cumple Control 1: si ambos métodos seleccionan grupos distintos, se bloquea la asignación automática.
 */
data class EdiCorrectedAgeComparison(
    val gestationalAge: GestationalAgeInput,
    val isPretermFlag: Boolean,
    val manualMethodResult: EdiSingleMethodGroupResolution,
    val calendarCredMethodResult: EdiSingleMethodGroupResolution,
    val daysDifferenceBetweenMethods: Int,
    val selectsSameGroup: Boolean,
    val autoSelectionStatus: EdiGroupAutoSelectionStatus,
    val automaticallyAssignedGroup: EdiGroupSpec?,
    val candidateGroupsForProfessionalReview: List<EdiGroupSpec>,
    val clinicalReviewMessage: String
)

/**
 * Registro dual de evidencias para reactivos con protocolo especial (ALE 12.3, LE 13.3, LE 14.3).
 * Cumple Control 3: si hay discrepancia entre observación y entrevista, no se emite respuesta automática.
 */
data class EdiDualEvidenceRecord(
    val itemUniqueId: String,
    val modality: EdiClinicalModality,
    val directObservationEvidence: Boolean? = null,
    val directedInterviewEvidence: Boolean? = null,
    val professionalHumanResolution: Boolean? = null,
    val clinicalJustificationNote: String = ""
) {
    val hasConflictBetweenEvidences: Boolean
        get() = directObservationEvidence != null &&
            directedInterviewEvidence != null &&
            directObservationEvidence != directedInterviewEvidence

    fun resolveItemResponse(): EdiItemResponse {
        return when (modality) {
            EdiClinicalModality.DUAL_OBSERVATION_AND_INTERVIEW_ALE_12_3 -> {
                if (directObservationEvidence == null || directedInterviewEvidence == null) {
                    EdiItemResponse.UNANSWERED
                } else if (directObservationEvidence == directedInterviewEvidence) {
                    if (directObservationEvidence) EdiItemResponse.YES else EdiItemResponse.NO
                } else {
                    when (professionalHumanResolution) {
                        true -> EdiItemResponse.YES
                        false -> EdiItemResponse.NO
                        null -> EdiItemResponse.DISCREPANCY_REQUIRES_HUMAN_RESOLUTION
                    }
                }
            }

            EdiClinicalModality.INTERVIEW_PRIMARY_WITH_OBSERVATION_COMPLEMENT -> {
                if (directedInterviewEvidence == null && directObservationEvidence == null) {
                    EdiItemResponse.UNANSWERED
                } else if (
                    directedInterviewEvidence != null &&
                    directObservationEvidence != null &&
                    directedInterviewEvidence != directObservationEvidence
                ) {
                    when (professionalHumanResolution) {
                        true -> EdiItemResponse.YES
                        false -> EdiItemResponse.NO
                        null -> EdiItemResponse.DISCREPANCY_REQUIRES_HUMAN_RESOLUTION
                    }
                } else {
                    val effective = directedInterviewEvidence ?: directObservationEvidence!!
                    if (effective) EdiItemResponse.YES else EdiItemResponse.NO
                }
            }

            else -> {
                val single = directObservationEvidence ?: directedInterviewEvidence
                when (single) {
                    true -> EdiItemResponse.YES
                    false -> EdiItemResponse.NO
                    null -> EdiItemResponse.UNANSWERED
                }
            }
        }
    }
}

/**
 * Snapshot inmutable de las respuestas clínicas registradas para un grupo específico.
 * Mantiene estrictamente separadas las respuestas del grupo actual y las de retroceso.
 */
data class EdiGroupSessionSnapshot(
    val groupNumber: Int,
    val currentGroupResponses: Map<String, EdiItemResponse> = emptyMap(),
    val fallbackGroupResponses: Map<String, EdiItemResponse> = emptyMap(),
    val frbResponses: Map<String, EdiItemResponse> = emptyMap(),
    val enResponses: Map<String, EdiItemResponse> = emptyMap(),
    val dualEvidenceRecords: Map<String, EdiDualEvidenceRecord> = emptyMap(),
    val itemClinicalNotes: Map<String, String> = emptyMap()
) {
    val answeredCurrentCount: Int
        get() = currentGroupResponses.values.count { it != EdiItemResponse.UNANSWERED }

    val answeredFallbackCount: Int
        get() = fallbackGroupResponses.values.count { it != EdiItemResponse.UNANSWERED }

    val answeredFrbCount: Int
        get() = frbResponses.values.count { it != EdiItemResponse.UNANSWERED }

    val answeredEnCount: Int
        get() = enResponses.values.count { it != EdiItemResponse.UNANSWERED }

    val dualEvidenceCount: Int
        get() = dualEvidenceRecords.size

    val clinicalNotesCount: Int
        get() = itemClinicalNotes.values.count { it.isNotBlank() }

    val totalRecordedElementsCount: Int
        get() = answeredCurrentCount +
            answeredFallbackCount +
            answeredFrbCount +
            answeredEnCount +
            dualEvidenceCount +
            clinicalNotesCount

    val hasRecordedClinicalResponses: Boolean
        get() = answeredCurrentCount > 0 ||
            answeredFallbackCount > 0 ||
            answeredFrbCount > 0 ||
            answeredEnCount > 0 ||
            dualEvidenceRecords.isNotEmpty() ||
            clinicalNotesCount > 0

    val summaryText: String
        get() = "G$groupNumber: $answeredCurrentCount act., $answeredFallbackCount retro., " +
            "$answeredFrbCount FRB, $answeredEnCount EN, $dualEvidenceCount dual, $clinicalNotesCount nota(s)"
}

/**
 * Estado de validación del contexto clínico disponible al momento de capturar/archivar una sesión.
 */
enum class EdiArchivedOriginValidationState(val displayLabel: String) {
    VERIFIED("VERIFICADO"),
    UNVERIFIED("NO VERIFICADO")
}

/**
 * Contexto clínico de origen preservado de manera inmutable al capturar o archivar una sesión EDI.
 * Nunca asume que dos niños sean el mismo paciente solo porque coincidan sexo y fecha de nacimiento;
 * incluye además el identificador de episodio temporal de trabajo ([workEpisodeId]).
 */
data class EdiArchivedClinicalContext(
    val workEpisodeId: String = "EP_1",
    val originValidationState: EdiArchivedOriginValidationState = EdiArchivedOriginValidationState.UNVERIFIED,
    val sexoLabel: String = "Niño",
    val fechaNacimiento: String = "",
    val fechaEvaluacion: String = "",
    val esPrematuro: Boolean = false,
    val egSemanas: Int = 32,
    val egDias: Int = 0,
    val ediEgRegistrada: Boolean = false,
    val ediBirthMaturityCondition: EdiBirthMaturityCondition = EdiBirthMaturityCondition.UNSPECIFIED,
    val effectiveAgeLabel: String = "EDAD NO VERIFICADA",
    val gestationalAgeDisplayLabel: String = "EG no registrada",
    val applicableGroupNumberAtCapture: Int? = null,
    val validationSummaryAtCapture: String = "NO VERIFICADO"
) {
    val isOriginContextVerified: Boolean
        get() = originValidationState == EdiArchivedOriginValidationState.VERIFIED
}

/**
 * Clasificación verificable de compatibilidad entre una sesión archivada y el estado clínico actual.
 */
enum class EdiArchivedCompatibilityStatus(val badgeLabel: String) {
    COMPATIBLE("COMPATIBLE"),
    INCOMPATIBLE_GROUP_OR_GESTATIONAL_STATE("ADVERTENCIA ETARIA / GESTACIONAL"),
    INCOMPATIBLE_ORIGIN_UNVERIFIED("NO VERIFICADO"),
    INCOMPATIBLE_DIFFERENT_PATIENT_OR_EPISODE("OTRO EPISODIO / PACIENTE")
}

/**
 * Resultado verificable de compatibilidad clínica de una sesión archivada frente al contexto actual.
 */
data class EdiArchivedSessionCompatibility(
    val status: EdiArchivedCompatibilityStatus,
    val isFullyCompatibleForDefinitiveEvaluation: Boolean,
    val summaryLabel: String,
    val warnings: List<String> = emptyList()
)

/**
 * Entrada inmutable dentro del historial canónico de sesiones EDI archivadas en memoria temporal.
 * Permite conservar múltiples evaluaciones del mismo o de distintos grupos sin sobrescribir ninguna.
 */
data class EdiArchivedSessionEntry(
    val archiveId: String,
    val sequenceNumber: Int,
    val groupNumber: Int,
    val snapshot: EdiGroupSessionSnapshot,
    val archiveReason: String,
    val originContext: EdiArchivedClinicalContext,
    val compatibilityStatusAtArchive: String
)

/**
 * Solicitud pendiente de restauración atómica de una sesión archivada.
 * Exige confirmación explícita antes de sustituir la sesión activa.
 */
data class EdiPendingRestoreRequest(
    val targetArchiveId: String,
    val targetEntry: EdiArchivedSessionEntry,
    val activeSessionWillBeArchivedFirst: Boolean,
    val compatibility: EdiArchivedSessionCompatibility,
    val warnings: List<String>,
    val samePatientAndEpisodeConfirmed: Boolean = false
)

/**
 * Solicitud pendiente de confirmación cuando un cambio de edad, fecha o plantilla cambiaría
 * el grupo activo existiendo respuestas clínicas ya registradas.
 * Cumple condición 4 de Fase 1 y Control 5:
 * - No elimina respuestas clínicas automáticamente.
 * - Tampoco reutiliza respuestas de un grupo anterior en el nuevo grupo.
 */
data class EdiPendingGroupChangeRequest(
    val previousGroupNumber: Int,
    val targetGroupNumber: Int,
    val reason: String,
    val preservedSnapshotOfPreviousGroup: EdiGroupSessionSnapshot
)

enum class EdiGroupChangeResolutionAction {
    ARCHIVE_PREVIOUS_AND_SWITCH_TO_CLEAN_TARGET,
    DISCARD_PREVIOUS_CONFIRMED_AND_SWITCH_TO_CLEAN_TARGET,
    CANCEL_SWITCH_KEEP_CURRENT_GROUP
}

data class EdiPhase1EngineState(
    val activeGroupNumber: Int,
    val activeSession: EdiGroupSessionSnapshot,
    val archivedSessionsByGroup: Map<Int, EdiGroupSessionSnapshot> = emptyMap(),
    val pendingGroupChangeConfirmation: EdiPendingGroupChangeRequest? = null,
    val archivedSessionHistory: List<EdiArchivedSessionEntry> = emptyList(),
    val nextArchiveSequence: Int = 1,
    val currentWorkEpisodeId: String = "EP_1",
    val nextWorkEpisodeSequence: Int = 2,
    val activeSessionOriginContext: EdiArchivedClinicalContext? = null,
    val restoredFromArchiveId: String? = null,
    val restoredSessionOriginContext: EdiArchivedClinicalContext? = null,
    val previewArchivedSessionId: String? = null,
    val pendingRestoreRequest: EdiPendingRestoreRequest? = null,
    val lastRestoreStatusMessage: String? = null
) {
    /**
     * Condición canónica (R1): verdadero cuando existen datos clínicos registrados en la sesión activa
     * O existen evaluaciones archivadas pertenecientes al episodio temporal actual ([currentWorkEpisodeId]).
     */
    val hasAnyDataInCurrentEpisode: Boolean
        get() = activeSession.hasRecordedClinicalResponses ||
            archivedSessionHistory.any {
                it.originContext.workEpisodeId == currentWorkEpisodeId
            }

    /**
     * Sesiones archivadas correspondientes exclusivamente al episodio de atención actual (R3).
     */
    val currentEpisodeArchivedHistory: List<EdiArchivedSessionEntry>
        get() = archivedSessionHistory.filter {
            it.originContext.workEpisodeId == currentWorkEpisodeId
        }

    /**
     * Número de evaluaciones archivadas pertenecientes a episodios anteriores resguardadas en RAM.
     */
    val previousEpisodesArchivedCount: Int
        get() = archivedSessionHistory.count {
            it.originContext.workEpisodeId != currentWorkEpisodeId
        }

    /**
     * Entrada archivada actualmente seleccionada para vista previa (solo lectura, restringida al episodio actual).
     */
    val previewArchivedEntry: EdiArchivedSessionEntry?
        get() = archivedSessionHistory.find {
            it.archiveId == previewArchivedSessionId &&
                it.originContext.workEpisodeId == currentWorkEpisodeId
        }

    /**
     * Obtiene todas las sesiones archivadas para un grupo específico en orden cronológico de archivado.
     */
    fun getArchivedEntriesForGroup(groupNumber: Int): List<EdiArchivedSessionEntry> =
        archivedSessionHistory.filter { it.groupNumber == groupNumber }

    /**
     * Obtiene las sesiones archivadas del episodio actual para un grupo específico.
     */
    fun getCurrentEpisodeArchivedEntriesForGroup(groupNumber: Int): List<EdiArchivedSessionEntry> =
        currentEpisodeArchivedHistory.filter { it.groupNumber == groupNumber }
}

/**
 * Base del motor clínico independiente (Fase 1 + MEDIO-02) para gestionar estados de respuesta,
 * modalidades duales, materiales faltantes, historial canónico de sesiones archivadas con ID único
 * y restauración atómica protegida sin pérdida ni mezcla de evaluaciones.
 */
object EdiPhase1SessionManager {

    private fun deriveArchivedSessionsByGroup(
        history: List<EdiArchivedSessionEntry>
    ): Map<Int, EdiGroupSessionSnapshot> {
        val map = linkedMapOf<Int, EdiGroupSessionSnapshot>()
        history.forEach { entry ->
            map[entry.groupNumber] = entry.snapshot
        }
        return map
    }

    fun createInitialState(groupNumber: Int): EdiPhase1EngineState {
        val validGroup = groupNumber.coerceIn(1, 15)
        return EdiPhase1EngineState(
            activeGroupNumber = validGroup,
            activeSession = createEmptySessionForGroup(validGroup)
        )
    }

    fun createEmptySessionForGroup(groupNumber: Int): EdiGroupSessionSnapshot {
        val spec = EdiOfficialCatalog.getGroup(groupNumber)
        val currentMap = linkedMapOf<String, EdiItemResponse>()
        spec.areaItems.values.flatten().forEach { currentMap[it.uniqueId] = EdiItemResponse.UNANSWERED }
        spec.aleItems.forEach { currentMap[it.uniqueId] = EdiItemResponse.UNANSWERED }
        spec.alaItems.forEach { currentMap[it.uniqueId] = EdiItemResponse.UNANSWERED }

        val frbMap = linkedMapOf<String, EdiItemResponse>()
        EdiOfficialCatalog.frbItems.forEach { frbMap[it.uniqueId] = EdiItemResponse.UNANSWERED }

        val enMap = linkedMapOf<String, EdiItemResponse>()
        EdiOfficialCatalog.getApplicableEnItems(groupNumber).forEach {
            enMap[it.uniqueId] = EdiItemResponse.UNANSWERED
        }

        return EdiGroupSessionSnapshot(
            groupNumber = groupNumber,
            currentGroupResponses = currentMap,
            fallbackGroupResponses = emptyMap(),
            frbResponses = frbMap,
            enResponses = enMap
        )
    }

    /**
     * Registra una respuesta individual del grupo activo, validando que el identificador
     * pertenezca al grupo actual y que si se marca NO_EVALUADO sea un ítem con material en protocolo.
     */
    fun recordCurrentGroupItemResponse(
        state: EdiPhase1EngineState,
        itemUniqueId: String,
        response: EdiItemResponse
    ): EdiPhase1EngineState {
        val item = EdiOfficialCatalog.findItemByUniqueId(itemUniqueId)
            ?: return state
        if (item.groupNumber != state.activeGroupNumber && item.axis != EdiAxis.FRB && item.axis != EdiAxis.EN) {
            // Bloquea mezclar respuestas de otro grupo en el grupo activo
            return state
        }
        val effectiveResponse = if (
            response == EdiItemResponse.NOT_EVALUATED_MISSING_MATERIAL &&
            !item.requiresMaterialInProtocol
        ) {
            // Si el ítem no utiliza materiales en su protocolo (ej. ALE 9.1), no puede bloquearse por falta de material
            EdiItemResponse.UNANSWERED
        } else {
            response
        }

        val s = state.activeSession
        val updatedSession = when (item.axis) {
            EdiAxis.FRB -> s.copy(frbResponses = s.frbResponses + (itemUniqueId to effectiveResponse))
            EdiAxis.EN -> s.copy(enResponses = s.enResponses + (itemUniqueId to effectiveResponse))
            else -> s.copy(currentGroupResponses = s.currentGroupResponses + (itemUniqueId to effectiveResponse))
        }
        return state.copy(activeSession = updatedSession)
    }

    /**
     * Registra una respuesta de retroceso en el mapa separado [fallbackGroupResponses].
     */
    fun recordFallbackItemResponse(
        state: EdiPhase1EngineState,
        fallbackItemUniqueId: String,
        response: EdiItemResponse
    ): EdiPhase1EngineState {
        val s = state.activeSession
        return state.copy(
            activeSession = s.copy(
                fallbackGroupResponses = s.fallbackGroupResponses + (fallbackItemUniqueId to response)
            )
        )
    }

    /**
     * Registra evidencia dual (observación y/o entrevista) para ALE 12.3, LE 13.3 o LE 14.3.
     */
    fun recordDualModalityEvidence(
        state: EdiPhase1EngineState,
        itemUniqueId: String,
        observationEvidence: Boolean?,
        interviewEvidence: Boolean?,
        humanResolutionIfConflict: Boolean? = null,
        note: String = ""
    ): EdiPhase1EngineState {
        val item = EdiOfficialCatalog.findItemByUniqueId(itemUniqueId) ?: return state
        if (item.groupNumber != state.activeGroupNumber) return state

        val record = EdiDualEvidenceRecord(
            itemUniqueId = itemUniqueId,
            modality = item.modality,
            directObservationEvidence = observationEvidence,
            directedInterviewEvidence = interviewEvidence,
            professionalHumanResolution = humanResolutionIfConflict,
            clinicalJustificationNote = note
        )
        val resolvedStatus = record.resolveItemResponse()
        val s = state.activeSession
        return state.copy(
            activeSession = s.copy(
                currentGroupResponses = s.currentGroupResponses + (itemUniqueId to resolvedStatus),
                dualEvidenceRecords = s.dualEvidenceRecords + (itemUniqueId to record)
            )
        )
    }

    /**
     * Solicita cambiar de grupo.
     * - Si el grupo actual NO tiene respuestas registradas, cambia directamente a una sesión limpia
     *   del grupo destino (sin reutilizar nada del grupo anterior).
     * - Si el grupo actual SÍ tiene respuestas registradas, NO las elimina automáticamente:
     *   genera [pendingGroupChangeConfirmation] conservando intacto el snapshot para que el
     *   profesional confirme si desea archivar/conservar el registro previo o descartarlo.
     */
    fun requestGroupChange(
        state: EdiPhase1EngineState,
        targetGroupNumber: Int,
        reason: String
    ): EdiPhase1EngineState {
        val safeTarget = targetGroupNumber.coerceIn(1, 15)
        if (safeTarget == state.activeGroupNumber) {
            return state.copy(pendingGroupChangeConfirmation = null)
        }
        return if (!state.activeSession.hasRecordedClinicalResponses) {
            state.copy(
                activeGroupNumber = safeTarget,
                activeSession = createEmptySessionForGroup(safeTarget),
                activeSessionOriginContext = null,
                restoredFromArchiveId = null,
                restoredSessionOriginContext = null,
                pendingGroupChangeConfirmation = null
            )
        } else {
            state.copy(
                pendingGroupChangeConfirmation = EdiPendingGroupChangeRequest(
                    previousGroupNumber = state.activeGroupNumber,
                    targetGroupNumber = safeTarget,
                    reason = reason,
                    preservedSnapshotOfPreviousGroup = state.activeSession
                )
            )
        }
    }

    /**
     * Construye una nueva entrada archivada única e inmutable para [snapshot].
     */
    private fun createArchivedEntry(
        sequenceNumber: Int,
        snapshot: EdiGroupSessionSnapshot,
        reason: String,
        originContext: EdiArchivedClinicalContext
    ): EdiArchivedSessionEntry {
        val archiveId = String.format(
            java.util.Locale.US,
            "ARCH_%03d_G%d",
            sequenceNumber,
            snapshot.groupNumber
        )
        return EdiArchivedSessionEntry(
            archiveId = archiveId,
            sequenceNumber = sequenceNumber,
            groupNumber = snapshot.groupNumber,
            snapshot = snapshot,
            archiveReason = reason,
            originContext = originContext,
            compatibilityStatusAtArchive = originContext.validationSummaryAtCapture
        )
    }

    /**
     * Resuelve de forma explícita y auditable una solicitud pendiente de cambio de grupo.
     * - En ningún caso se reutilizan las respuestas del grupo anterior dentro del nuevo grupo.
     * - Al archivar, agrega una entrada única a [archivedSessionHistory] sin sobrescribir sesiones
     *   anteriores del mismo grupo, y proyecta [archivedSessionsByGroup] desde el historial canónico.
     * - Al descartar ([EdiGroupChangeResolutionAction.DISCARD_PREVIOUS_CONFIRMED_AND_SWITCH_TO_CLEAN_TARGET]),
     *   descarta únicamente la sesión activa y NUNCA elimina entradas históricas ya archivadas.
     */
    fun confirmPendingGroupChange(
        state: EdiPhase1EngineState,
        action: EdiGroupChangeResolutionAction,
        originContext: EdiArchivedClinicalContext? = null
    ): EdiPhase1EngineState {
        val pending = state.pendingGroupChangeConfirmation ?: return state
        return when (action) {
            EdiGroupChangeResolutionAction.ARCHIVE_PREVIOUS_AND_SWITCH_TO_CLEAN_TARGET -> {
                val resolvedContext = originContext
                    ?: state.activeSessionOriginContext
                    ?: EdiArchivedClinicalContext(workEpisodeId = state.currentWorkEpisodeId)
                val newEntry = createArchivedEntry(
                    sequenceNumber = state.nextArchiveSequence,
                    snapshot = pending.preservedSnapshotOfPreviousGroup,
                    reason = pending.reason,
                    originContext = resolvedContext
                )
                val updatedHistory = state.archivedSessionHistory + newEntry
                state.copy(
                    activeGroupNumber = pending.targetGroupNumber,
                    activeSession = createEmptySessionForGroup(pending.targetGroupNumber),
                    archivedSessionHistory = updatedHistory,
                    archivedSessionsByGroup = deriveArchivedSessionsByGroup(updatedHistory),
                    nextArchiveSequence = state.nextArchiveSequence + 1,
                    activeSessionOriginContext = null,
                    restoredFromArchiveId = null,
                    restoredSessionOriginContext = null,
                    pendingGroupChangeConfirmation = null
                )
            }

            EdiGroupChangeResolutionAction.DISCARD_PREVIOUS_CONFIRMED_AND_SWITCH_TO_CLEAN_TARGET -> {
                val preservedMap = if (state.archivedSessionHistory.isNotEmpty()) {
                    deriveArchivedSessionsByGroup(state.archivedSessionHistory)
                } else {
                    state.archivedSessionsByGroup
                }
                state.copy(
                    activeGroupNumber = pending.targetGroupNumber,
                    activeSession = createEmptySessionForGroup(pending.targetGroupNumber),
                    archivedSessionsByGroup = preservedMap,
                    activeSessionOriginContext = null,
                    restoredFromArchiveId = null,
                    restoredSessionOriginContext = null,
                    pendingGroupChangeConfirmation = null
                )
            }

            EdiGroupChangeResolutionAction.CANCEL_SWITCH_KEEP_CURRENT_GROUP -> {
                state.copy(pendingGroupChangeConfirmation = null)
            }
        }
    }

    /**
     * Abre la vista previa en modo de solo lectura para una sesión archivada del episodio actual.
     * No modifica [EdiPhase1EngineState.activeSession] ni [EdiPhase1EngineState.activeGroupNumber].
     * Por privacidad (R3), rechaza abrir sesiones de otros episodios.
     */
    fun openArchivedSessionPreview(
        state: EdiPhase1EngineState,
        archiveId: String
    ): EdiPhase1EngineState {
        val targetEntry = state.archivedSessionHistory.find { it.archiveId == archiveId }
            ?: return state
        if (targetEntry.originContext.workEpisodeId != state.currentWorkEpisodeId) {
            return state.copy(
                previewArchivedSessionId = null,
                lastRestoreStatusMessage = "Privacidad por episodio: la evaluación $archiveId pertenece al episodio ${targetEntry.originContext.workEpisodeId} y no puede abrirse en el episodio activo (${state.currentWorkEpisodeId})."
            )
        }
        return state.copy(previewArchivedSessionId = archiveId)
    }

    /**
     * Cierra la vista previa de sesión archivada sin alterar la sesión activa.
     */
    fun closeArchivedSessionPreview(
        state: EdiPhase1EngineState
    ): EdiPhase1EngineState = state.copy(previewArchivedSessionId = null)

    /**
     * Inicia una solicitud de restauración protegida para [archiveId].
     * - R3: Si la entrada pertenece a otro episodio ([EdiArchivedClinicalContext.workEpisodeId] != [EdiPhase1EngineState.currentWorkEpisodeId]),
     *   rechaza inmediatamente la operación sin habilitar restauración ni mutar la sesión activa.
     * - R2: Inicializa [EdiPendingRestoreRequest.samePatientAndEpisodeConfirmed] en `false` para exigir
     *   confirmación deliberada de continuidad del mismo paciente y episodio antes de restaurar.
     */
    fun requestRestoreArchivedSession(
        state: EdiPhase1EngineState,
        archiveId: String,
        compatibility: EdiArchivedSessionCompatibility
    ): EdiPhase1EngineState {
        val targetEntry = state.archivedSessionHistory.find { it.archiveId == archiveId }
            ?: return state.copy(
                pendingRestoreRequest = null,
                lastRestoreStatusMessage = "No se encontró la sesión archivada solicitada ($archiveId)."
            )
        if (
            targetEntry.originContext.workEpisodeId != state.currentWorkEpisodeId ||
            compatibility.status == EdiArchivedCompatibilityStatus.INCOMPATIBLE_DIFFERENT_PATIENT_OR_EPISODE
        ) {
            return state.copy(
                pendingRestoreRequest = null,
                lastRestoreStatusMessage = "Restauración rechazada: la evaluación ${targetEntry.archiveId} pertenece a otro episodio (${targetEntry.originContext.workEpisodeId}) y está prohibido restaurarla en el episodio activo (${state.currentWorkEpisodeId})."
            )
        }
        val activeWillBeArchived = state.activeSession.hasRecordedClinicalResponses
        val allWarnings = buildList {
            if (activeWillBeArchived) {
                add(
                    "La evaluación activa actual (G${state.activeGroupNumber}) contiene respuestas clínicas y será archivada automáticamente como una nueva entrada única antes de restaurar ${targetEntry.archiveId}."
                )
            }
            addAll(compatibility.warnings)
        }
        return state.copy(
            pendingRestoreRequest = EdiPendingRestoreRequest(
                targetArchiveId = targetEntry.archiveId,
                targetEntry = targetEntry,
                activeSessionWillBeArchivedFirst = activeWillBeArchived,
                compatibility = compatibility,
                warnings = allWarnings,
                samePatientAndEpisodeConfirmed = false
            )
        )
    }

    /**
     * Registra la confirmación deliberada del profesional sobre la continuidad del mismo paciente
     * y episodio de atención («Confirmo que esta evaluación corresponde al mismo paciente y episodio de atención»).
     */
    fun setPendingRestoreSamePatientConfirmed(
        state: EdiPhase1EngineState,
        confirmed: Boolean
    ): EdiPhase1EngineState {
        val pending = state.pendingRestoreRequest ?: return state
        if (pending.targetEntry.originContext.workEpisodeId != state.currentWorkEpisodeId) {
            return state.copy(
                pendingRestoreRequest = null,
                lastRestoreStatusMessage = "Restauración rechazada: la evaluación ${pending.targetArchiveId} pertenece a otro episodio (${pending.targetEntry.originContext.workEpisodeId})."
            )
        }
        return state.copy(
            pendingRestoreRequest = pending.copy(samePatientAndEpisodeConfirmed = confirmed)
        )
    }

    /**
     * Cancela una solicitud pendiente de restauración conservando intacta la sesión activa.
     */
    fun cancelRestoreArchivedSession(
        state: EdiPhase1EngineState
    ): EdiPhase1EngineState = state.copy(pendingRestoreRequest = null)

    /**
     * Ejecuta la restauración atómica protegida tras confirmación explícita del profesional:
     * 1. Verifica que la entrada objetivo exista en el historial canónico.
     * 2. R3: Rechaza cualquier intento de restaurar una evaluación cuyo `workEpisodeId` no coincida
     *    con `currentWorkEpisodeId`, conservando intacta la sesión activa y el archivo original.
     * 3. R2: Exige confirmación explícita de continuidad del paciente (`samePatientContinuityConfirmed`
     *    o `pending.samePatientAndEpisodeConfirmed == true`); sin ella, rechaza la restauración.
     * 4. Si la evaluación activa contiene respuestas clínicas, la archiva primero como nueva entrada única.
     * 5. Restaura íntegramente el snapshot elegido (6 mapas completos) sin mezclar respuestas.
     * 6. Conserva la trazabilidad inmutable ([EdiPhase1EngineState.restoredFromArchiveId] y [EdiPhase1EngineState.restoredSessionOriginContext]).
     */
    fun confirmRestoreArchivedSession(
        state: EdiPhase1EngineState,
        activeOriginContextForAutoArchive: EdiArchivedClinicalContext? = null,
        samePatientContinuityConfirmed: Boolean = state.pendingRestoreRequest?.samePatientAndEpisodeConfirmed == true
    ): EdiPhase1EngineState {
        val pending = state.pendingRestoreRequest ?: return state.copy(
            lastRestoreStatusMessage = "Restauración rechazada: no existe una solicitud de restauración pendiente."
        )
        val targetEntry = state.archivedSessionHistory.find { it.archiveId == pending.targetArchiveId }
            ?: return state.copy(
                pendingRestoreRequest = null,
                lastRestoreStatusMessage = "Error: la sesión archivada ${pending.targetArchiveId} no existe; se conservó intacta la sesión activa."
            )

        if (
            targetEntry.originContext.workEpisodeId != state.currentWorkEpisodeId ||
            pending.targetEntry.originContext.workEpisodeId != state.currentWorkEpisodeId ||
            pending.compatibility.status == EdiArchivedCompatibilityStatus.INCOMPATIBLE_DIFFERENT_PATIENT_OR_EPISODE
        ) {
            return state.copy(
                pendingRestoreRequest = null,
                lastRestoreStatusMessage = "Restauración rechazada: la evaluación ${targetEntry.archiveId} pertenece al episodio ${targetEntry.originContext.workEpisodeId} y está prohibido restaurarla en el episodio activo (${state.currentWorkEpisodeId})."
            )
        }

        val isContinuityConfirmed = samePatientContinuityConfirmed || pending.samePatientAndEpisodeConfirmed
        if (!isContinuityConfirmed) {
            return state.copy(
                lastRestoreStatusMessage = "Restauración rechazada: debe confirmar explícitamente «Confirmo que esta evaluación corresponde al mismo paciente y episodio de atención» antes de restaurar ${targetEntry.archiveId}."
            )
        }

        var workingHistory = state.archivedSessionHistory
        var workingSeq = state.nextArchiveSequence

        if (state.activeSession.hasRecordedClinicalResponses) {
            val activeOrigin = activeOriginContextForAutoArchive
                ?: state.activeSessionOriginContext
                ?: EdiArchivedClinicalContext(workEpisodeId = state.currentWorkEpisodeId)
            val preRestoreArchiveEntry = createArchivedEntry(
                sequenceNumber = workingSeq,
                snapshot = state.activeSession,
                reason = "Respaldo automático previo a restaurar ${targetEntry.archiveId} (G${targetEntry.groupNumber})",
                originContext = activeOrigin
            )
            val candidateHistory = workingHistory + preRestoreArchiveEntry
            val verifiedSaved = candidateHistory.any {
                it.archiveId == preRestoreArchiveEntry.archiveId &&
                    it.snapshot == state.activeSession
            }
            if (!verifiedSaved) {
                return state.copy(
                    pendingRestoreRequest = null,
                    lastRestoreStatusMessage = "No se pudo verificar el archivado previo de la sesión activa; se abortó la restauración sin pérdida de datos."
                )
            }
            workingHistory = candidateHistory
            workingSeq += 1
        }

        val restoredSnapshot = targetEntry.snapshot.copy(
            currentGroupResponses = LinkedHashMap(targetEntry.snapshot.currentGroupResponses),
            fallbackGroupResponses = LinkedHashMap(targetEntry.snapshot.fallbackGroupResponses),
            frbResponses = LinkedHashMap(targetEntry.snapshot.frbResponses),
            enResponses = LinkedHashMap(targetEntry.snapshot.enResponses),
            dualEvidenceRecords = LinkedHashMap(targetEntry.snapshot.dualEvidenceRecords),
            itemClinicalNotes = LinkedHashMap(targetEntry.snapshot.itemClinicalNotes)
        )

        return state.copy(
            activeGroupNumber = targetEntry.groupNumber,
            activeSession = restoredSnapshot,
            archivedSessionHistory = workingHistory,
            archivedSessionsByGroup = deriveArchivedSessionsByGroup(workingHistory),
            nextArchiveSequence = workingSeq,
            activeSessionOriginContext = targetEntry.originContext,
            restoredFromArchiveId = targetEntry.archiveId,
            restoredSessionOriginContext = targetEntry.originContext,
            previewArchivedSessionId = null,
            pendingRestoreRequest = null,
            pendingGroupChangeConfirmation = null,
            lastRestoreStatusMessage = "Sesión ${targetEntry.archiveId} (G${targetEntry.groupNumber}) restaurada íntegramente tras confirmar continuidad del mismo paciente y episodio (${state.currentWorkEpisodeId})."
        )
    }

    /**
     * Inicia un nuevo episodio temporal de evaluación (p. ej. al iniciar la evaluación de otro paciente
     * o cuando cambian datos esenciales de identificación existiendo datos en el episodio actual).
     * Si la sesión activa tiene respuestas registradas, la archiva primero bajo su contexto de origen
     * antes de abrir una sesión limpia en el nuevo episodio temporal.
     */
    fun startNewWorkEpisode(
        state: EdiPhase1EngineState,
        targetGroupNumber: Int = state.activeGroupNumber,
        reason: String = "Inicio de nuevo episodio temporal de evaluación",
        previousOriginContext: EdiArchivedClinicalContext? = null
    ): EdiPhase1EngineState {
        val safeTarget = targetGroupNumber.coerceIn(1, 15)
        var workingHistory = state.archivedSessionHistory
        var workingSeq = state.nextArchiveSequence

        if (state.activeSession.hasRecordedClinicalResponses) {
            val origin = previousOriginContext
                ?: state.activeSessionOriginContext
                ?: EdiArchivedClinicalContext(workEpisodeId = state.currentWorkEpisodeId)
            val archivedEntry = createArchivedEntry(
                sequenceNumber = workingSeq,
                snapshot = state.activeSession,
                reason = reason,
                originContext = origin
            )
            workingHistory = workingHistory + archivedEntry
            workingSeq += 1
        }

        val newEpisodeId = "EP_${state.nextWorkEpisodeSequence}"
        return state.copy(
            activeGroupNumber = safeTarget,
            activeSession = createEmptySessionForGroup(safeTarget),
            archivedSessionHistory = workingHistory,
            archivedSessionsByGroup = deriveArchivedSessionsByGroup(workingHistory),
            nextArchiveSequence = workingSeq,
            currentWorkEpisodeId = newEpisodeId,
            nextWorkEpisodeSequence = state.nextWorkEpisodeSequence + 1,
            activeSessionOriginContext = null,
            restoredFromArchiveId = null,
            restoredSessionOriginContext = null,
            previewArchivedSessionId = null,
            pendingRestoreRequest = null,
            pendingGroupChangeConfirmation = null,
            lastRestoreStatusMessage = "Nuevo episodio temporal iniciado ($newEpisodeId): $reason"
        )
    }
}

object EdiOfficialCatalog {

    /**
     * Los 7 Factores de Riesgo Biológico oficiales comunes a todos los grupos (pág. 12).
     */
    val frbItems: List<EdiCatalogItem> = listOf(
        EdiCatalogItem(
            uniqueId = "FRB_1",
            groupNumber = 0,
            axis = EdiAxis.FRB,
            itemCode = "FRB 1",
            itemIndexInSection = 1,
            hasObservationAsterisks = false,
            formatQuestionText = "1. Asistencia a dos o menos controles prenatales.",
            consultationQuestionText = "FRB 1. Asistencia a dos o menos controles prenatales",
            clinicalInstruction = "Pregunte a cuántos controles prenatales asistió la madre durante el embarazo. La madre debió acudir a más de dos.",
            modality = EdiClinicalModality.DIRECTED_INTERVIEW,
            yesIsNormalGreen = false
        ),
        EdiCatalogItem(
            uniqueId = "FRB_2",
            groupNumber = 0,
            axis = EdiAxis.FRB,
            itemCode = "FRB 2",
            itemIndexInSection = 2,
            hasObservationAsterisks = false,
            formatQuestionText = "2. Presencia de sangrados, infecciones de vías urinarias o cervicovaginitis, presión alta o enfermedades sistémicas durante el embarazo.",
            consultationQuestionText = "FRB 2. Presencia de sangrados, infecciones de vías urinarias o cervicovaginitis, presión alta o enfermedades sistémicas durante el embarazo.",
            clinicalInstruction = "Pregunte si la madre presentó alguna complicación durante el embarazo como: sangrados transvaginales, amenaza de aborto o parto pretérmino, cervicovaginitis o infecciones urinarias de repetición (2 o más), hipertensión arterial u otra enfermedad sistémica (lupus eritematoso, artritis reumatoide, diabetes mellitus, epilepsia, hipertiroidismo, entre otras), intervenciones quirúrgicas mayores, ingesta de medicamentos teratogénicos, drogas, alcohol, tabaquismo u hospitalizaciones maternas frecuentes por otra causa.",
            modality = EdiClinicalModality.DIRECTED_INTERVIEW,
            yesIsNormalGreen = false
        ),
        EdiCatalogItem(
            uniqueId = "FRB_3",
            groupNumber = 0,
            axis = EdiAxis.FRB,
            itemCode = "FRB 3",
            itemIndexInSection = 3,
            hasObservationAsterisks = false,
            formatQuestionText = "3. Gestación menor a 34 semanas.",
            consultationQuestionText = "FRB 3. Gestación menor a 34 semanas.",
            clinicalInstruction = "Pregunte la duración del embarazo. Es posible que el cuidador desconozca el número de semanas, por lo que puede preguntar el número de meses y en caso de ser menor a 7 meses y medio o haber nacido 6 semanas antes de la fecha probable de parto o cesárea, debe tomarse como menor a 34 semanas de gestación.",
            modality = EdiClinicalModality.DIRECTED_INTERVIEW,
            yesIsNormalGreen = false
        ),
        EdiCatalogItem(
            uniqueId = "FRB_4",
            groupNumber = 0,
            axis = EdiAxis.FRB,
            itemCode = "FRB 4",
            itemIndexInSection = 4,
            hasObservationAsterisks = false,
            formatQuestionText = "4. Peso de su niño al nacer de 1500 g o menos.",
            consultationQuestionText = "FRB 4. Peso de su niño al nacer de 1500 g o menos.",
            clinicalInstruction = "Pregunte cuál fue el peso de su niño al nacimiento. Si trae Certificado de Nacido Vivo (CNV), carné de atención integral de salud de la niña o niño menor de 5 años, carné neonatal o libreta de cuidados, obtenga la información de dicho documento.",
            modality = EdiClinicalModality.DIRECTED_INTERVIEW,
            yesIsNormalGreen = false
        ),
        EdiCatalogItem(
            uniqueId = "FRB_5",
            groupNumber = 0,
            axis = EdiAxis.FRB,
            itemCode = "FRB 5",
            itemIndexInSection = 5,
            hasObservationAsterisks = false,
            formatQuestionText = "5. Retardo en la respiración o circular de cordón a cuello durante el parto o cesárea.",
            consultationQuestionText = "FRB 5. Retardo en la respiración y circular de cordón umbilical en el cuello durante el parto o cesárea.",
            clinicalInstruction = "Pregunte: a) Si el bebé lloró al nacer y si no fue así, cuánto tiempo tardó en llorar o respirar; b) si le comentaron que tenía una coloración \"azulosa o morada\", es decir, si estaba cianótico. De ser posible obtenga el Apgar al minuto y a los cinco minutos, y pregunte si requirió reanimación (Apgar menor o igual a 6). También indague si presentó circular de cordón a cuello. \"¿Fue necesario que le hicieran cesárea porque tenía el cordón enredado en su cuello?\"",
            modality = EdiClinicalModality.DIRECTED_INTERVIEW,
            yesIsNormalGreen = false
        ),
        EdiCatalogItem(
            uniqueId = "FRB_6",
            groupNumber = 0,
            axis = EdiAxis.FRB,
            itemCode = "FRB 6",
            itemIndexInSection = 6,
            hasObservationAsterisks = false,
            formatQuestionText = "6. Hospitalización de su niño en la Unidad de Cuidados Intensivos Neonatales (UCIN) al nacimiento o antes del primer mes de vida con una duración mayor a 4 días.",
            consultationQuestionText = "FRB 6. Hospitalización de su niño en la Unidad de Cuidados Intensivos Neonatales (UCIN) al nacimiento o antes del primer mes de vida con una duración mayor a 4 días.",
            clinicalInstruction = "Pregunte si el niño requirió ser hospitalizado o permanecer en la UCIN durante el período neonatal. De obtener una respuesta afirmativa, investigue la causa y la duración de la hospitalización.",
            modality = EdiClinicalModality.DIRECTED_INTERVIEW,
            yesIsNormalGreen = false
        ),
        EdiCatalogItem(
            uniqueId = "FRB_7",
            groupNumber = 0,
            axis = EdiAxis.FRB,
            itemCode = "FRB 7",
            itemIndexInSection = 7,
            hasObservationAsterisks = false,
            formatQuestionText = "7. Madre menor a 16 años al momento del parto.",
            consultationQuestionText = "FRB 7. Madre menor a 16 años al momento del parto o cesárea",
            clinicalInstruction = "Pregunte la edad de la madre al momento del nacimiento de su niño.",
            modality = EdiClinicalModality.DIRECTED_INTERVIEW,
            yesIsNormalGreen = false
        )
    )

    /**
     * Los 3 ítems oficiales de Exploración Neurológica (pág. 13).
     * Nota clínica importante: En el Grupo 15 (60 a 71 meses 29 días), el ítem EN 1.3
     * (perímetro cefálico) NO aplica según las páginas 47 y 111 del manual oficial.
     */
    val allEnItems: List<EdiCatalogItem> = listOf(
        EdiCatalogItem(
            uniqueId = "EN_1_1",
            groupNumber = 0,
            axis = EdiAxis.EN,
            itemCode = "EN 1.1",
            itemIndexInSection = 1,
            hasObservationAsterisks = true,
            formatQuestionText = "**1. ¿Presenta alteración en la movilidad de alguna parte del cuerpo? (Observe)",
            consultationQuestionText = "**EN 1.1. ¿Presenta alteración en la movilidad de alguna parte del cuerpo?",
            clinicalInstruction = "Coloque al niño relajado, acostado o sentado sobre la mesa de exploración y observe mientras lo estimula, si moviliza la cabeza, el tronco y las cuatro extremidades. Analice la presencia de movimientos normales (aquellos con una velocidad, intensidad y cantidad media), anormales (aquellos con una velocidad, intensidad y cantidad pobre o excesiva) o ausencia de movimiento en alguno(s) de los segmentos corporales o en todo su cuerpo.",
            modality = EdiClinicalModality.DIRECT_OBSERVATION,
            generalHeaderMaterials = emptyList(),
            protocolRequiredMaterials = emptyList(),
            yesIsNormalGreen = false
        ),
        EdiCatalogItem(
            uniqueId = "EN_1_2",
            groupNumber = 0,
            axis = EdiAxis.EN,
            itemCode = "EN 1.2",
            itemIndexInSection = 2,
            hasObservationAsterisks = true,
            formatQuestionText = "**2. ¿Presenta alteración o asimetría en la movilidad de ojos o expresión facial? (Observe)",
            consultationQuestionText = "**EN 1.2. ¿Presenta alteración o asimetría en la movilidad de ojos o expresión facial?",
            clinicalInstruction = "Coloque al niño acostado o sentado sobre la mesa de exploración. Sujete con la mano derecha el juguete llamativo no sonoro a 20 cm frente al niño y después de obtener la fijación visual hacia el juguete, desplácelo lentamente en el eje horizontal, vertical y en ambas diagonales. Analice la presencia o ausencia de mirada binocular en posición primaria (ojos viendo al frente), incapacidad de la movilidad de uno o ambos ojos o alguna alteración de la mirada primaria conjugada. Después con el niño en la misma posición, observe la asimetría en la expresión facial. En caso de estar presente, puede ser más visible cuando el niño llora, ya que se observa que el lado afectado permanece fijo, la boca se ve desviada al lado opuesto (sano) y el párpado queda entreabierto mientras se cierra en lado sano.",
            modality = EdiClinicalModality.DIRECT_OBSERVATION,
            generalHeaderMaterials = listOf("Juguete llamativo no sonoro"),
            protocolRequiredMaterials = listOf("Juguete llamativo no sonoro"),
            yesIsNormalGreen = false
        ),
        EdiCatalogItem(
            uniqueId = "EN_1_3",
            groupNumber = 0,
            axis = EdiAxis.EN,
            itemCode = "EN 1.3",
            itemIndexInSection = 3,
            hasObservationAsterisks = true,
            formatQuestionText = "**3. ¿Presenta perímetro cefálico por arriba o por debajo de 2 desviaciones estándar para su edad? (Observe)",
            consultationQuestionText = "**EN 1.3. ¿Presenta perímetro cefálico por arriba o debajo de 2 desviaciones estándar para su edad?",
            clinicalInstruction = "Coloque al niño acostado o sentado sobre la mesa de exploración. Pase la cinta métrica sobre las partes más prominentes del cráneo: desde la prominencia occipital externa llegando a los arcos supraciliares y luego hasta la glabela. Registre la medición realizada y compare esta con los valores de normalidad acorde a sexo y edad. En menores de 24 meses se compara en las tablas con edad corregida en caso de antecedente de prematurez. Lo normal es encontrar el perímetro cefálico entre +/- dos desviaciones estándar acorde a sexo y edad.",
            modality = EdiClinicalModality.DIRECT_OBSERVATION,
            generalHeaderMaterials = listOf("Cinta métrica"),
            protocolRequiredMaterials = listOf("Cinta métrica"),
            yesIsNormalGreen = false
        )
    )

    fun getApplicableEnItems(groupNumber: Int): List<EdiCatalogItem> =
        if (groupNumber == 15) allEnItems.take(2) else allEnItems

    /**
     * Los 3 reactivos especiales de retroceso para Conocimiento en el Grupo 13 (CO-R 13.1 a 13.3, págs. 43 y 102).
     * Se aplican cuando el área de Conocimiento del Grupo 13 queda en Amarillo con 0 respuestas en Verde,
     * dado que el Grupo 12 (31-36m) no cuenta con área de Conocimiento.
     */
    val specialGroup13CoRFallbackItems: List<EdiCatalogItem> = listOf(
        EdiCatalogItem(
            uniqueId = "G13_RETRO_COR_13_1",
            groupNumber = 13,
            axis = EdiAxis.AREAS,
            area = EdiDevArea.CO,
            itemCode = "CO-R 13.1",
            itemIndexInSection = 1,
            hasObservationAsterisks = true,
            formatQuestionText = "**31-36 1. ¿Sabe decir si es niño o niña? (Observe)",
            consultationQuestionText = "**CO-R 13.1. ¿Sabe decir si es niño o niña?",
            clinicalInstruction = "Pregunte al niño, en tono divertido: \"Dime, y tú ¿eres niña o niño?\"",
            modality = EdiClinicalModality.DIRECT_OBSERVATION,
            yesIsNormalGreen = true,
            isSpecialFallbackItem = true
        ),
        EdiCatalogItem(
            uniqueId = "G13_RETRO_COR_13_2",
            groupNumber = 13,
            axis = EdiAxis.AREAS,
            area = EdiDevArea.CO,
            itemCode = "CO-R 13.2",
            itemIndexInSection = 2,
            hasObservationAsterisks = false,
            formatQuestionText = "31-36 2. ¿Dice frases como \"tengo sueño\", \"me quiero ir a dormir\"?",
            consultationQuestionText = "CO-R 13.2. ¿Dice frases como: \"tengo sueño\", \"me quiero ir a dormir\"?",
            clinicalInstruction = "Pregunte a la madre, padre o cuidador principal: cuando su niño está cansado ¿lo expresa con palabras?, ¿cómo lo dice? Si el niño quiere ir a la cama, ¿se lo hace saber?",
            modality = EdiClinicalModality.DIRECTED_INTERVIEW,
            yesIsNormalGreen = true,
            isSpecialFallbackItem = true
        ),
        EdiCatalogItem(
            uniqueId = "G13_RETRO_COR_13_3",
            groupNumber = 13,
            axis = EdiAxis.AREAS,
            area = EdiDevArea.CO,
            itemCode = "CO-R 13.3",
            itemIndexInSection = 3,
            hasObservationAsterisks = false,
            formatQuestionText = "31-36 3. ¿Hace preguntas como: \"¿quién?\", \"¿cuándo?\", \"¿dónde?\", \"¿por qué?\"?",
            consultationQuestionText = "CO-R 13.3. Hace preguntas como: \"¿quién?\", \"¿cuándo?\", \"¿dónde?\", \"¿por qué?\"",
            clinicalInstruction = "Pregunte a la madre, padre o cuidador principal si su niño hace preguntas que comienzan con \"quién\", \"cuándo\", \"dónde\" y \"por qué\". Defina la frecuencia e intensidad de la conducta en comparación con otros niños de su misma edad.",
            modality = EdiClinicalModality.DIRECTED_INTERVIEW,
            yesIsNormalGreen = true,
            isSpecialFallbackItem = true
        )
    )

    private fun devItem(
        group: Int,
        area: EdiDevArea,
        index: Int,
        obs: Boolean,
        formatQ: String,
        consultQ: String,
        instruction: String,
        modality: EdiClinicalModality = if (obs) EdiClinicalModality.DIRECT_OBSERVATION else EdiClinicalModality.DIRECTED_INTERVIEW,
        headerMats: List<String> = emptyList(),
        protocolMats: List<String> = emptyList(),
        plates: Set<EdiOfficialPlate> = emptySet()
    ) = EdiCatalogItem(
        uniqueId = "G${group}_${area.code}_${group}_$index",
        groupNumber = group,
        axis = EdiAxis.AREAS,
        area = area,
        itemCode = "${area.code} $group.$index",
        itemIndexInSection = index,
        hasObservationAsterisks = obs,
        formatQuestionText = formatQ,
        consultationQuestionText = consultQ,
        clinicalInstruction = instruction,
        modality = modality,
        generalHeaderMaterials = headerMats,
        protocolRequiredMaterials = protocolMats,
        protocolRequiredPlates = plates,
        yesIsNormalGreen = true
    )

    private fun aleItem(
        group: Int,
        index: Int,
        obs: Boolean,
        formatQ: String,
        consultQ: String,
        instruction: String,
        modality: EdiClinicalModality = if (obs) EdiClinicalModality.DIRECT_OBSERVATION else EdiClinicalModality.DIRECTED_INTERVIEW,
        headerMats: List<String> = emptyList(),
        protocolMats: List<String> = emptyList(),
        plates: Set<EdiOfficialPlate> = emptySet()
    ) = EdiCatalogItem(
        uniqueId = "G${group}_ALE_${group}_$index",
        groupNumber = group,
        axis = EdiAxis.ALE,
        area = null,
        itemCode = "ALE $group.$index",
        itemIndexInSection = index,
        hasObservationAsterisks = obs,
        formatQuestionText = formatQ,
        consultationQuestionText = consultQ,
        clinicalInstruction = instruction,
        modality = modality,
        generalHeaderMaterials = headerMats,
        protocolRequiredMaterials = protocolMats,
        protocolRequiredPlates = plates,
        yesIsNormalGreen = false
    )

    private fun alaItem(
        group: Int,
        index: Int,
        obs: Boolean,
        formatQ: String,
        consultQ: String,
        instruction: String,
        headerMats: List<String> = emptyList(),
        protocolMats: List<String> = emptyList()
    ) = EdiCatalogItem(
        uniqueId = "G${group}_ALA_${group}_$index",
        groupNumber = group,
        axis = EdiAxis.ALA,
        area = null,
        itemCode = "ALA $group.$index",
        itemIndexInSection = index,
        hasObservationAsterisks = obs,
        formatQuestionText = formatQ,
        consultationQuestionText = consultQ,
        clinicalInstruction = instruction,
        modality = if (obs) EdiClinicalModality.DIRECT_OBSERVATION else EdiClinicalModality.DIRECTED_INTERVIEW,
        generalHeaderMaterials = headerMats,
        protocolRequiredMaterials = protocolMats,
        yesIsNormalGreen = false
    )

    private fun buildGroup1(): EdiGroupSpec = EdiGroupSpec(
        groupNumber = 1,
        code = "G1",
        officialTitle = "Grupo 1 (de 1 mes a 1 mes 29 días)",
        shortIntervalLabel = "1m 0d - 1m 29d",
        minMonthsInclusive = 1,
        maxMonthsInclusive = 1,
        formatPages = 17..18,
        consultationPages = 51..53,
        generalHeaderMaterialsText = "MF 1.2: Sonaja o aro que el niño pueda sujetar.",
        itemsPerActiveArea = 2,
        fallbackPreviousGroupNumber = null,
        fallbackItemsCountPerArea = 0,
        frbAndAleModifyGlobalTrafficLight = true,
        areaItems = mapOf(
            EdiDevArea.MG to listOf(
                devItem(
                    1, EdiDevArea.MG, 1, true,
                    "**1. Cuando su bebé está acostado boca abajo, ¿puede voltear su cabeza para los dos lados? (Observe)",
                    "**MG 1.1. Cuando su bebé está acostado boca abajo, ¿puede voltear su cabeza para los dos lados?",
                    "Estando boca abajo el bebé, llame su atención con un objeto atractivo (sonaja o juguete) para que voltee la cabeza hacia el lado en que le muestra el objeto. Observe si lo hace. Repetición del ítem: hasta 2 veces más de cada lado.",
                    protocolMats = listOf("Objeto atractivo (sonaja o juguete)")
                ),
                devItem(
                    1, EdiDevArea.MG, 2, false,
                    "2. Cuando desviste a su bebé, ¿patea y mueve los brazos?",
                    "MG 1.2. Cuando desviste a su bebé, ¿patea y mueve los brazos?",
                    "Pregunte a la madre, padre o cuidador principal ¿Ha observado si el bebé mueve los brazos y las piernas por igual cuando lo desviste?, ¿diría que mueve los brazos y piernas por igual o hay algún miembro que mueva menos que los otros? Defina la frecuencia e intensidad de la conducta en comparación con otros niños de su misma edad."
                )
            ),
            EdiDevArea.MF to listOf(
                devItem(
                    1, EdiDevArea.MF, 1, false,
                    "1. ¿Su bebé mantiene las manos cerradas la mayor parte del tiempo?",
                    "MF 1.1. ¿Su bebé mantiene las manos cerradas/empuñadas la mayor parte del tiempo?",
                    "Pregunte a la madre, padre o cuidador principal si el bebé mantiene las manos cerradas/empuñadas la mayor parte del día o, si por el contrario, las tiene abiertas y flojas."
                ),
                devItem(
                    1, EdiDevArea.MF, 2, true,
                    "**2. Cuando le da en la mano una sonaja a su bebé, ¿la retiene durante al menos 3 segundos? (Observe)",
                    "**MF 1.2. Cuando le da en la mano una sonaja a su bebé ¿la retiene durante al menos 3 segundos?",
                    "Coloque al bebé en decúbito supino y ponga en su mano el aro o la sonaja. Si la mano está cerrada deslícele suavemente los dedos hacia afuera para provocar su apertura, colocando después la sonaja en la mano debiendo retenerla durante al menos 3 segundos. Observe si lo hace con ambas manos.",
                    headerMats = listOf("Sonaja o aro que el niño pueda sujetar"),
                    protocolMats = listOf("Sonaja o aro que el niño pueda sujetar")
                )
            ),
            EdiDevArea.LE to listOf(
                devItem(
                    1, EdiDevArea.LE, 1, false,
                    "1. ¿Su bebé se tranquiliza cuando escucha su voz?",
                    "LE 1.1. ¿Su bebé se tranquiliza cuando escucha su voz?",
                    "Pregunte a la madre, padre o cuidador principal ¿deja de llorar el bebé si usted le habla con una voz tranquilizadora?, ¿deja de llorar cuando escucha el sonido de su voz, aunque usted no lo toque o lo cargue? Defina la frecuencia e intensidad de la conducta en comparación con otros niños de su misma edad."
                ),
                devItem(
                    1, EdiDevArea.LE, 2, false,
                    "2. ¿Hace algún ruido con la boca, llora cuando está incómodo o quiere comer?",
                    "LE 1.2. ¿Hace algún ruido con la boca, llora cuando está incómodo o quiere lactar?",
                    "Pregunte a la madre, padre o cuidador primario: ¿tiene el bebé diferentes tipos de llantos para comunicar lo que necesita?, ¿cómo conoce usted que el bebé tiene hambre o tiene sueño?, ¿llora de una manera diferente o hace algún sonido especial? Defina la frecuencia e intensidad de la conducta en comparación con otros niños de su misma edad."
                )
            ),
            EdiDevArea.SO to listOf(
                devItem(
                    1, EdiDevArea.SO, 1, false,
                    "1. Cuando llora, ¿se tranquiliza al hablarle o levantarlo?",
                    "SO 1.1. Cuando llora, ¿se tranquiliza al hablarle o levantarlo?",
                    "Pregunte a la madre, padre o cuidador principal: si el bebé está llorando, ¿se tranquiliza al hablarle o levantarlo?, ¿qué otra estrategia usa para calmarlo si está llorando?, ¿cuánto tiempo necesita para que se calme? En general, ¿usted diría que el bebé se calma cuando está en sus brazos? Defina la frecuencia e intensidad de la conducta en comparación con otros niños de su misma edad."
                ),
                devItem(
                    1, EdiDevArea.SO, 2, true,
                    "**2. Cuando usted está frente a su bebé, ¿sigue con la mirada sus movimientos? (Observe)",
                    "**SO 1.2. Cuando usted está frente a su bebé, ¿sigue con la mirada sus movimientos?",
                    "Mientras cargan al bebé, colóquese de frente a unos 20 cm de distancia, llame su atención para que se fije en usted y muévase hacia la derecha e izquierda. Observe si el bebé sigue sus movimientos con la mirada. Repetición del ítem: hasta 2 veces más cada lado."
                )
            ),
            EdiDevArea.CO to emptyList()
        ),
        aleItems = listOf(
            aleItem(
                1, 1, true,
                "**1. Al evaluar el tono muscular de su niño, ¿lo siente totalmente flácido o presenta rigidez en sus extremidades? (Observe)",
                "**ALE 1.1. Al evaluar el tono muscular del niño, ¿lo siente totalmente flácido o presenta rigidez en sus extremidades?",
                "Realice las maniobras correspondientes para valorar en el niño su tono pasivo, evaluando ángulos y simetría de las maniobras, tomando en cuenta que una limitación importante o incapacidad para lograr los ángulos según la edad, es un dato sugestivo de hipertonía (espasticidad); mientras que sobrepasar estos ángulos de manera importante sugiere hipotonía (flacidez)."
            ),
            aleItem(
                1, 2, true,
                "**2. En la evaluación de los reflejos de Moro, prensión palmar y plantar, ¿falta alguno de estos 3 reflejos? (Observe)",
                "**ALE 1.2. En la evaluación de los reflejos de Moro, prensión palmar y plantar, ¿falta alguno de estos 3 reflejos?",
                "Realice las maniobras correspondientes para valorar en el niño cada uno de estos 3 reflejos y durante su realización valore su presencia, así como la simetría."
            ),
            aleItem(
                1, 3, false,
                "3. ¿Su hijo tiene problemas para comer: se atraganta, se pone morado o presenta problemas para succionar?",
                "ALE 1.3. ¿Su hijo(a) tiene problemas para lactar: se atora/atraganta, se pone morado, presenta problemas para succionar?",
                "Pregunte a la madre, padre o cuidador principal: \"¿El bebé presenta tos o una expresión en la cara de molestia cuando lacta?\", \"¿Durante la alimentación frecuentemente presenta náuseas, vómitos, se pone morado, pálido, o se atora/atraganta?\". Defina la frecuencia e intensidad de la conducta en comparación con otros niños(as) de su misma edad."
            ),
            aleItem(
                1, 4, false,
                "4. ¿Considera que el desarrollo de su niño es inadecuado?",
                "ALE 1.4. ¿Considera que el desarrollo de su niño es inadecuado?",
                "Pregunte a la madre, padre o cuidador principal ¿Ha observado algo que le parece que no va del todo bien?, ¿tiene usted alguna preocupación por la manera en que su bebé se mueve, se comporta o se relaciona con los demás? Defina la frecuencia e intensidad de la conducta en comparación con otros niños de su misma edad."
            )
        ),
        alaItems = emptyList()
    )

    private fun buildGroup2(): EdiGroupSpec = EdiGroupSpec(
        groupNumber = 2,
        code = "G2",
        officialTitle = "Grupo 2 (de 2 meses a 2 meses 29 días)",
        shortIntervalLabel = "2m 0d - 2m 29d",
        minMonthsInclusive = 2,
        maxMonthsInclusive = 2,
        formatPages = 19..20,
        consultationPages = 55..58,
        generalHeaderMaterialsText = "MG 2.1: Sonaja o juguete llamativo. MF 2.1: Pelota roja sostenida por una cuerda. LE 2.1: Sonaja o hacer sonidos con las llaves.",
        itemsPerActiveArea = 2,
        fallbackPreviousGroupNumber = 1,
        fallbackItemsCountPerArea = 2,
        frbAndAleModifyGlobalTrafficLight = true,
        areaItems = mapOf(
            EdiDevArea.MG to listOf(
                devItem(
                    2, EdiDevArea.MG, 1, true,
                    "**1. Cuando acuesta a su bebé boca abajo, ¿levanta su cabeza durante al menos 3 segundos? (Observe)",
                    "**MG 2.1. Cuando acuesta a su bebé boca abajo, ¿levanta su cabeza durante al menos 3 segundos?",
                    "Coloque al niño en posición prona (boca abajo) y llame su atención con un objeto atractivo (sonaja o juguete) para que levante la cabeza y la mantenga así por lo menos tres segundos. Observe si lo hace. Repetición del ítem: hasta 2 veces más de cada lado.",
                    headerMats = listOf("Sonaja o juguete llamativo"),
                    protocolMats = listOf("Sonaja o juguete llamativo")
                ),
                devItem(
                    2, EdiDevArea.MG, 2, true,
                    "**2. Cuando carga a su bebé, ¿logra sostener la cabeza sin que se le vaya bruscamente hacia atrás o hacia alguno de los lados? (Observe)",
                    "**MG 2.2. Cuando carga a su bebé con la cabeza hacia arriba, ¿logra sostener la cabeza sin que se le vaya bruscamente hacia atrás o hacia alguno de los lados?",
                    "Cargue al bebé dando apoyo a su cadera y cabeza. Colóquelo en posición vertical, lentamente deslice hacia abajo la mano que sostiene la cabeza y observe si la mantiene estable o cae bruscamente (hacia adelante, atrás o a los lados)."
                )
            ),
            EdiDevArea.MF to listOf(
                devItem(
                    2, EdiDevArea.MF, 1, true,
                    "**1. ¿Puede seguir con la vista el movimiento de objetos de colores vivos? (Observe)",
                    "**MF 2.1. ¿Puede seguir con la vista el movimiento de objetos de colores vivos?",
                    "Coloque al bebé en posición supina y posicionándose usted detrás, fuera de su campo visual, suspenda la cuerda con la pelota por encima de sus ojos a 20 cm de distancia. Atraiga la mirada moviendo el objeto lentamente en el eje horizontal primero a la derecha, luego al centro y finalmente a la izquierda. Observe si lo hace. Repetición del ítem: hasta 2 veces más de cada lado.",
                    headerMats = listOf("Pelota roja sostenida por una cuerda"),
                    protocolMats = listOf("Pelota roja sostenida por una cuerda")
                ),
                devItem(
                    2, EdiDevArea.MF, 2, true,
                    "**2. ¿Se lleva la mano a la boca? (Observe)",
                    "**MF 2.2. ¿Se lleva la mano a la boca?",
                    "Durante toda la visita observe si el niño intencionadamente se lleva la mano a la boca."
                )
            ),
            EdiDevArea.LE to listOf(
                devItem(
                    2, EdiDevArea.LE, 1, true,
                    "1. Cuando su bebé oye un ruido, ¿voltea hacia la fuente de sonido, estando ésta fuera de su campo visual?",
                    "**LE 2.1. Cuando su bebé oye un ruido fuera de su campo visual, ¿voltea en dirección al sonido?",
                    "Colocándose fuera del campo visual del bebé, utilice algún objeto ruidoso (campana) y hágalo sonar primero en su costado izquierdo y luego en el derecho esperando que voltee hacia la fuente de sonido. Repetición del ítem: hasta 2 veces más de cada lado.",
                    headerMats = listOf("Sonaja o hacer sonidos con las llaves"),
                    protocolMats = listOf("Sonaja, campana o llaves para producir sonido")
                ),
                devItem(
                    2, EdiDevArea.LE, 2, false,
                    "2. ¿Hace sonidos como \"a\", \"e\", \"u\"?",
                    "LE 2.2. ¿Hace sonidos como \"a\", \"e\", \"u\"?",
                    "Pregunte a la madre, padre o cuidador principal: ¿qué sonidos hace el bebé?, ¿puede describirlos? Entre los sonidos que hace, ¿ha notado si dice: \"aaa\" o \"eee\" o \"uuu\"? Defina la frecuencia e intensidad de la conducta en comparación con otros niños de su misma edad."
                )
            ),
            EdiDevArea.SO to listOf(
                devItem(
                    2, EdiDevArea.SO, 1, false,
                    "1. ¿Hace sonidos con la boca cuando le hablan?",
                    "SO 2.1. ¿Hace sonidos con la boca cuando le hablan?",
                    "Pregunte a la madre, padre o cuidador principal: ¿cómo se comporta si alguien le habla cariñosamente?, ¿hace algún sonido para contestar a eso? Defina la frecuencia e intensidad de la conducta en comparación con otros niños de su misma edad."
                ),
                devItem(
                    2, EdiDevArea.SO, 2, false,
                    "2. ¿Reconoce los rostros que le son familiares y sonríe?",
                    "SO 2.2. ¿Reconoce los rostros que le son familiares y sonríe?",
                    "Pregunte a la madre, padre o cuidador principal: ¿el niño sonríe cuando ve a una persona familiar?, ¿qué otras señales da al reconocer a la persona? Defina la frecuencia e intensidad de la conducta en comparación con otros niños de su misma edad."
                )
            ),
            EdiDevArea.CO to emptyList()
        ),
        aleItems = listOf(
            aleItem(
                2, 1, true,
                "**1. ¿Están las manos de su niño permanentemente empuñadas con el pulgar adentro? (Observe)",
                "**ALE 2.1. ¿Están las manos de su niño permanentemente cerradas/empuñadas con el pulgar adentro?",
                "Observe durante la consulta las manos del bebé, y en caso de que estén cerradas, si el pulgar está atrapado (anormal) o por fuera de los dedos (normal)."
            ),
            aleItem(
                2, 2, false,
                "2. ¿Está el niño frecuentemente irritable y cuesta trabajo tranquilizarlo a pesar de haber satisfecho sus necesidades y de forma más notoria que otros niños de su edad?",
                "ALE 2.2. ¿Está el niño frecuentemente irritable y cuesta trabajo tranquilizarlo a pesar de haber satisfecho sus necesidades y de forma más notoria que otros niños de su edad?",
                "Pregunte a la madre, padre o cuidador principal: ¿usted diría que el bebé es tranquilo o por el contrario es muy llorón e irritable?, ¿le cuesta mucho calmarlo?, ¿cuándo se calma, vuelve a llorar nuevamente al poco rato? Defina la frecuencia e intensidad de la conducta en comparación con otros niños de su misma edad."
            ),
            aleItem(
                2, 3, false,
                "3. ¿Frecuentemente se encuentra aletargado y no muestra interés por lo que sucede a su alrededor?, ¿cuesta trabajo mantenerlo despierto?",
                "ALE 2.3. ¿Frecuentemente se encuentra aletargado(a)/adormecido(a) y no muestra interés por lo que sucede a su alrededor?, ¿cuesta trabajo mantenerlo despierto?",
                "Pregunte a la madre, padre o cuidador principal: ¿usted diría que el bebé tiende a estar somnoliento demasiado tiempo, comparado con otros de su edad? Defina la frecuencia e intensidad de la conducta en comparación con otros niños de su misma edad."
            ),
            aleItem(
                2, 4, false,
                "4. ¿Considera que algo no va del todo bien en el comportamiento, desarrollo o características de su bebé?",
                "ALE 2.4. ¿Considera que algo no va del todo bien en el comportamiento, desarrollo o características de su bebé?",
                "Pregunte a la madre, padre o cuidador principal: ¿Ha observado algo que le parece que no va del todo bien?, ¿tiene usted alguna preocupación por la manera en que el bebé se mueve, se comporta o se relaciona con los demás? Defina la frecuencia e intensidad de la conducta en comparación con otros niños de su misma edad."
            )
        ),
        alaItems = listOf(
            alaItem(
                2, 1, true,
                "**1. Al evaluar el tono muscular de su niño, ¿lo siente totalmente flácido o presenta rigidez en sus extremidades? (Observe)",
                "**ALA 2.1. Al evaluar el tono muscular del niño, ¿lo siente totalmente flácido o presenta rigidez en sus extremidades?",
                "Realice en el niño las maniobras correspondientes para valorar el tono pasivo del niño, evaluando ángulos y simetría de las maniobras, tomando en cuenta que una limitación importante o incapacidad para lograr los ángulos según la edad, es un dato sugestivo de hipertonía (espasticidad, rigidez); mientras que el sobrepasar estos ángulos de manera importante sugiere hipotonía (flacidez)."
            ),
            alaItem(
                2, 2, true,
                "**2. ¿El reflejo de Moro está ausente o es asimétrico? (Observe)",
                "**ALA 2.2. ¿El reflejo de Moro está ausente o es asimétrico?",
                "Realice en el niño la maniobra correspondiente para valorar este reflejo y durante su realización valore la simetría."
            ),
            alaItem(
                2, 3, false,
                "3. ¿Su hijo tiene problemas para comer: se atraganta, se pone morado o presenta problemas para succionar?",
                "ALA 2.3. ¿Su hijo tiene problemas para lactar: se atora/atraganta, se pone morado o presenta problemas para succionar?",
                "Pregunte a la madre, padre o cuidador principal: ¿el bebé presenta tos o una expresión en la cara de molestia durante la comida?, ¿durante la alimentación frecuentemente presenta náuseas, vómito, se pone morado o pálido, o se atraganta?. Defina la frecuencia e intensidad de la conducta en comparación con otros niños de su misma edad."
            )
        )
    )

    private fun buildGroup3(): EdiGroupSpec = EdiGroupSpec(
        groupNumber = 3,
        code = "G3",
        officialTitle = "Grupo 3 (de 3 meses a 3 meses 29 días)",
        shortIntervalLabel = "3m 0d - 3m 29d",
        minMonthsInclusive = 3,
        maxMonthsInclusive = 3,
        formatPages = 21..22,
        consultationPages = 59..61,
        generalHeaderMaterialsText = "ALE 3.2: Pelota roja sostenida por una cuerda. MG 3.1: Sonaja o juguete llamativo.",
        itemsPerActiveArea = 2,
        fallbackPreviousGroupNumber = 2,
        fallbackItemsCountPerArea = 2,
        frbAndAleModifyGlobalTrafficLight = true,
        areaItems = mapOf(
            EdiDevArea.MG to listOf(
                devItem(
                    3, EdiDevArea.MG, 1, true,
                    "**1. Cuando su bebé está boca abajo y apoyado en sus antebrazos, ¿logra sostener la cabeza al menos 3 segundos? (Observe)",
                    "**MG 3.1. Cuando su bebé está boca abajo y apoyado en sus antebrazos, ¿logra sostener la cabeza al menos 3 segundos?",
                    "Coloque al bebé en posición prona (boca abajo) y llame su atención hablándole o con algún objeto atractivo (sonaja o juguete) para que eleve la cabeza, continúe llamando su atención mientras cuantifica el tiempo que logra mantenerla así.",
                    headerMats = listOf("Sonaja o juguete llamativo"),
                    protocolMats = listOf("Sonaja o juguete llamativo")
                ),
                devItem(
                    3, EdiDevArea.MG, 2, true,
                    "**2. Cuando su bebé se sienta, ¿Ha observado si logra sostener la cabeza sin que se vaya bruscamente hacia adelante, atrás o hacia alguno de los lados? (Observe)",
                    "**MG 3.2. Cuando sienta al bebé, ¿ha observado si logra sostener la cabeza sin que se le vaya bruscamente hacia adelante, atrás o hacia alguno de los lados?",
                    "Estando sentado el bebé, deje de sostener la cabeza y vea si la mantiene o se cae."
                )
            ),
            EdiDevArea.MF to listOf(
                devItem(
                    3, EdiDevArea.MF, 1, false,
                    "1. ¿Se ha fijado si su bebé observa sus manos durante al menos 3 segundos mientras las está moviendo?",
                    "MF 3.1. ¿Se ha fijado si su bebé observa sus manos durante al menos 3 segundos mientras las está moviendo?",
                    "Pregunte a la madre, padre o cuidador principal si el bebé se da cuenta de sus manos, si las mira mientras las mueve durante al menos 3 segundos o si juega con ellas y con qué frecuencia lo hace. Defina la frecuencia e intensidad de la conducta en comparación con otros niños de su misma edad."
                ),
                devItem(
                    3, EdiDevArea.MF, 2, true,
                    "**2. ¿Tiene su bebé las manos abiertas la mayor parte del tiempo? (Observe)",
                    "**MF 3.2. ¿Tiene el bebé las manos abiertas la mayor parte del tiempo?",
                    "Coloque al bebé en la mesa de exploración permitiéndole que se quede tranquilo, mientras ambas manos toman la posición en reposo, que debe ser abierta la mayor parte del tiempo. Es importante que el bebé no esté cogiendo o intentando coger algún objeto."
                )
            ),
            EdiDevArea.LE to listOf(
                devItem(
                    3, EdiDevArea.LE, 1, false,
                    "1. Cuando le hablan a su bebé, ¿hace sonidos con la boca o sonríe?",
                    "LE 3.1. Cuando le hablan a su bebé, ¿hace sonidos con la boca o sonríe?",
                    "Pregunte a la madre, padre o cuidador principal: ¿ha observado si el bebé sonríe cuando algún adulto le habla?, ¿hace el bebé algún sonido con la boca cuando un adulto le habla, como queriendo responderle? Defina la frecuencia e intensidad de la conducta en comparación con otros niños de su misma edad."
                ),
                devItem(
                    3, EdiDevArea.LE, 2, false,
                    "2. Cuando balbucea, ¿pronuncia alguna consonante como \"agu\" o \"ga\"?",
                    "LE 3.2. Cuando balbucea, ¿pronuncia algunas consonantes como \"agú\" o \"ga\"?",
                    "Pregunte a la madre, padre o cuidador principal: ¿qué sonidos hace el bebé?, ¿puede describirlos?, entre los sonidos que hace ¿ha notado si dice \"gaaaa\" o \"agú\" o \"guuuu\"? Defina la frecuencia e intensidad de la conducta en comparación con otros niños de su misma edad."
                )
            ),
            EdiDevArea.SO to listOf(
                devItem(
                    3, EdiDevArea.SO, 1, false,
                    "1. ¿Sonríe su bebé con facilidad?",
                    "SO 3.1. ¿Sonríe su bebé con facilidad?",
                    "Pregunte a la madre, padre o cuidador principal: ¿cuándo sonríe el bebé?, ¿sonríe cuando los demás le sonríen?, ¿sonríe cuando los demás lo miran a la cara y le hablan tiernamente? Defina la frecuencia e intensidad de la conducta en comparación con otros niños de su misma edad."
                ),
                devItem(
                    3, EdiDevArea.SO, 2, true,
                    "**2. Cuando usted le habla al bebé fuera de su campo visual, ¿voltea la cabeza y trata de ubicar el origen de su voz? (Observe)",
                    "**SO 3.2. Cuando usted le habla a su bebé fuera del campo visual, ¿voltea la cabeza y trata de ubicar el origen de su voz?",
                    "Colóquese detrás del bebé, fuera de su campo visual, y háblele cariñosamente para que voltee tratando de ubicar donde está usted. Repetición del ítem: hasta 2 veces más de cada lado."
                )
            ),
            EdiDevArea.CO to emptyList()
        ),
        aleItems = listOf(
            aleItem(
                3, 1, true,
                "**1. Cuando lo sostiene por el tronco su cabeza cae bruscamente hacia adelante, atrás o a los lados? (Observe)",
                "**ALE 3.1. ¿Cuándo lo sostiene por el tronco, su cabeza cae bruscamente hacia adelante, atrás o a los lados?",
                "Sosteniendo al bebé por el tronco observe si logra mantener su cabeza, sin dejarla caer hacia adelante, hacia atrás o hacia los lados."
            ),
            aleItem(
                3, 2, true,
                "**2. ¿Se le dificulta seguir con la vista a las personas y a los objetos brillantes? (Observe)",
                "**ALE 3.2. ¿Se le dificulta seguir con la vista a las personas y a los objetos brillantes?",
                "Durante la consulta colóquese a la vista del bebé, apóyese con un objeto brillante desplazándolo y observe si el bebé lo sigue con la mirada.",
                headerMats = listOf("Pelota roja sostenida por una cuerda"),
                protocolMats = listOf("Pelota roja sostenida por una cuerda u objeto brillante")
            ),
            aleItem(
                3, 3, false,
                "3. ¿Se mantiene serio o inexpresivo cuando la madre, padre o cuidador primario le sonríe?",
                "ALE 3.3. ¿Se mantiene serio o inexpresivo cuando la madre, padre o cuidador primario le sonríe?",
                "Pregunte a la madre, padre o cuidador principal: ¿sonríe el bebé cuando usted le hace gestos?, ¿le devuelve la sonrisa si usted le hace caras simpáticas y le habla de manera cariñosa? Defina la frecuencia e intensidad de la conducta en comparación con otros niños de su misma edad."
            ),
            aleItem(
                3, 4, false,
                "4. ¿Considera que algo no va del todo bien en el comportamiento, desarrollo o características de su bebé?",
                "ALE 3.4. ¿Considera que algo no va del todo bien en el comportamiento, desarrollo o características de su bebé?",
                "Pregunte a la madre, padre o cuidador principal: ¿ha observado algo que le parece que no va del todo bien?, ¿tiene usted alguna preocupación por la manera en que el bebé se mueve, se comporta o se relaciona con los demás? Defina la frecuencia e intensidad de la conducta en comparación con otros niños de su misma edad."
            )
        ),
        alaItems = listOf(
            alaItem(
                3, 1, true,
                "**1. Al evaluar el tono muscular del niño, ¿lo siente totalmente flácido o presenta rigidez en sus extremidades? (Observe)",
                "**ALA 3.1. Al evaluar el tono muscular del niño, ¿lo siente totalmente flácido o presenta rigidez en sus extremidades?",
                "Realice las maniobras correspondientes para valorar el tono pasivo en el niño, evaluando ángulos y simetría de las maniobras, tomando en cuenta que una limitación importante o incapacidad para lograr los ángulos según la edad, es un dato sugestivo de hipertonía (espasticidad); mientras que el sobrepasar estos ángulos de manera importante sugiere hipotonía (flacidez)."
            ),
            alaItem(
                3, 2, true,
                "**2. ¿El reflejo de Moro está ausente o es asimétrico? (Observe)",
                "**ALA 3.2. ¿El reflejo de Moro está ausente o es asimétrico?",
                "Realice la maniobra correspondiente en el niño para valorar este reflejo y durante su realización valore la simetría."
            )
        )
    )

    private fun buildGroup4(): EdiGroupSpec = EdiGroupSpec(
        groupNumber = 4,
        code = "G4",
        officialTitle = "Grupo 4 (de 4 meses a 4 meses 29 días)",
        shortIntervalLabel = "4m 0d - 4m 29d",
        minMonthsInclusive = 4,
        maxMonthsInclusive = 4,
        formatPages = 23..24,
        consultationPages = 63..65,
        generalHeaderMaterialsText = "ALE 4.2 y MF 4.1: Pelota roja sostenida por una cuerda. MF 4.2: Sonaja o juguete llamativo.",
        itemsPerActiveArea = 2,
        fallbackPreviousGroupNumber = 3,
        fallbackItemsCountPerArea = 2,
        frbAndAleModifyGlobalTrafficLight = true,
        areaItems = mapOf(
            EdiDevArea.MG to listOf(
                devItem(
                    4, EdiDevArea.MG, 1, true,
                    "**1. Cuando su bebé está sentado y lo sostiene suavemente de la espalda, ¿mantiene su cabeza, sin dejarla caer hacia ningún lado por lo menos 1 minuto? (Observe)",
                    "**MG 4.1. Cuando su bebé está sentado y lo sostiene suavemente de la espalda, ¿mantiene su cabeza, sin dejarla caer hacia ningún lado por lo menos un minuto?",
                    "Pida a la madre, padre o cuidador principal que siente al bebé en sus piernas, dándole soporte con una mano al pecho y otra a la región occipital del cráneo. Después, solicítele que baje lentamente la mano que sostiene la cabeza para que el bebé logre sostenerla por lo menos un minuto mientras le sostiene el tronco."
                ),
                devItem(
                    4, EdiDevArea.MG, 2, true,
                    "**2. Cuando su bebé está acostado boca arriba, ¿se voltea hacia los lados? (Observe)",
                    "**MG 4.2. Cuando el bebé está acostado boca arriba, ¿se voltea hacia los lados?",
                    "Coloque al bebé en decúbito dorsal (boca arriba) y llame su atención hablándole, cantándole o mostrándole algún juguete atractivo para que logre ponerse de costado, dándose vuelta hacia el lado del estímulo. Repetición del ítem: hasta 2 veces más de cada lado."
                )
            ),
            EdiDevArea.MF to listOf(
                devItem(
                    4, EdiDevArea.MF, 1, true,
                    "**1. ¿Extiende su bebé alguno de sus brazos para tratar de agarrar un objeto que está a su alcance? (Observe)",
                    "**MF 4.1. ¿Extiende su bebé alguno de sus brazos para tratar de coger un objeto que está a su alcance?",
                    "Coloque al bebé en decúbito dorsal (boca arriba) sosteniendo la cuerda con la pelota por encima de su cabeza y muévala para atraer su atención. Aliéntelo para que trate de agarrar el objeto. Realice el procedimiento en ambos lados.",
                    headerMats = listOf("Pelota roja sostenida por una cuerda"),
                    protocolMats = listOf("Pelota roja sostenida por una cuerda")
                ),
                devItem(
                    4, EdiDevArea.MF, 2, true,
                    "**2. ¿Puede mantener en la mano un objeto al menos 10 segundos? (Observe)",
                    "**MF 4.2. ¿Puede mantener en la mano un objeto al menos 10 segundos?",
                    "Coloque al bebé en decúbito dorsal (boca arriba) y ofrézcale un objeto atractivo (sonaja, cubo, etc.) para que lo mantenga en la mano al menos 10 segundos. Valore ambas manos. Repetición del ítem: hasta 2 veces más de cada lado.",
                    headerMats = listOf("Sonaja o juguete llamativo"),
                    protocolMats = listOf("Sonaja, cubo o juguete llamativo")
                )
            ),
            EdiDevArea.LE to listOf(
                devItem(
                    4, EdiDevArea.LE, 1, false,
                    "1. ¿Balbucea o grita para llamar su atención?",
                    "LE 4.1. ¿Balbucea o grita para llamar su atención?",
                    "Pregunte a la madre, padre o cuidador principal: ¿cómo hace el bebé para llamar su atención?, ¿si usted se aleja y el bebé le quiere cerca, solo comienza a llorar o tiene alguna otra forma de comunicarse, como balbuceo o gritos? Defina la frecuencia e intensidad de la conducta en comparación con otros niños de su misma edad."
                ),
                devItem(
                    4, EdiDevArea.LE, 2, false,
                    "2. ¿Emite sonidos como \"le\", \"be\", \"pa\", \"gu\"?",
                    "LE 4.2. ¿Emite sonidos como \"le\", \"be\", \"pa\", \"gu\"?",
                    "Pregunte a la madre, padre o cuidador principal: ¿qué sonidos hace el bebé?, ¿puede describirlos? Entre los sonidos que hace ¿ha notado si dice \"le\", \"be\", \"pa\", \"gu\" o algún otro parecido? ¿En qué momento usa estos sonidos? ¿Lo hace con frecuencia? Verifique que se trata de una combinación consonante-vocal. Defina la frecuencia e intensidad de la conducta en comparación con otros niños de su misma edad."
                )
            ),
            EdiDevArea.SO to listOf(
                devItem(
                    4, EdiDevArea.SO, 1, false,
                    "1. Piensa usted que cuando su bebé ve su pecho o la leche, ¿sabe que en poco tiempo le va a dar de lactar?",
                    "SO 4.1. Piensa usted que cuando su bebé ve su pecho o la leche, ¿sabe que en poco tiempo le va a dar de lactar?",
                    "Pregunte a la madre, padre o cuidador principal si el bebé tiene conductas anticipatorias cuando ve el pecho materno, como cambios en la expresión facial, agitación o aumento de la frecuencia cardiaca, demostrando que sabe que se le va a alimentar. Defina la frecuencia e intensidad de la conducta en comparación con otros niños de su misma edad. Nota: Si por razones médicas el bebé no puede recibir lactancia materna, adecue la pregunta a la situación específica."
                ),
                devItem(
                    4, EdiDevArea.SO, 2, false,
                    "2. ¿Le gusta jugar con otras personas y llora cuando dejan de jugar con él?",
                    "SO 4.2. ¿Le gusta jugar con otras personas y llora cuando dejan de jugar con él?",
                    "Pregunte a la madre, padre o cuidador principal: ¿cómo manifiesta que quiere jugar con los adultos?, ¿llora, grita, jala la ropa o toca al adulto cuando quiere que le presten atención y jueguen con él?, ¿qué hace el niño cuando el adulto deja de jugar con él e intenta alejarse? ¿Recurre el niño a estrategias para impedir que el adulto se aleje? Defina la frecuencia e intensidad de la conducta en comparación con otros niños de su misma edad."
                )
            ),
            EdiDevArea.CO to emptyList()
        ),
        aleItems = listOf(
            aleItem(
                4, 1, true,
                "**1. ¿Hay asimetría en los movimientos de las manos? (Observe)",
                "**ALE 4.1. ¿Hay asimetría en los movimientos de las manos?",
                "Analice si durante la consulta el bebé usa ambas manos o sólo una de ellas. Coloque objetos frente al bebé de ambos lados y confirme que mueva ambas manos por igual."
            ),
            aleItem(
                4, 2, true,
                "**2. ¿Muestra indiferencia por los objetos? (Observe)",
                "**ALE 4.2. ¿Muestra indiferencia por los objetos?",
                "Coloque al bebé en supino (boca arriba) sosteniendo la cuerda con la pelota por encima de su cabeza y muévala para atraer su atención. Aliente al bebé para que trate de coger el objeto.",
                headerMats = listOf("Pelota roja sostenida por una cuerda"),
                protocolMats = listOf("Pelota roja sostenida por una cuerda")
            ),
            aleItem(
                4, 3, true,
                "**3. ¿Persiste el reflejo de Galant o de incurvación? (Observe)",
                "**ALE 4.3. ¿Persiste el reflejo de Galant o de incurvación?",
                "Coloque al bebé en decúbito ventral (boca abajo) y con su dedo índice aplique un estímulo en la línea paravertebral izquierda o derecha (2-3 cm al lado de la línea media), desde la región interescapular hasta el glúteo. El reflejo está presente si flexiona la cadera hacia el lado estimulado. Deben explorarse ambos lados y el reflejo debe ser simétrico."
            ),
            aleItem(
                4, 4, false,
                "**4. ¿Considera que algo no va del todo bien en el comportamiento, desarrollo o características de su bebé? (Observe)",
                "ALE 4.4. ¿Considera que algo no va del todo bien en el comportamiento, desarrollo o características de su bebé?",
                "Pregunte a la madre, padre o cuidador principal: ¿ha observado algo que le parece que no va del todo bien?, ¿tiene usted alguna preocupación por la manera en que el bebé se mueve, se comporta o se relaciona con los demás? Defina la frecuencia e intensidad de la conducta en comparación con otros niños de su misma edad."
            )
        ),
        alaItems = listOf(
            alaItem(
                4, 1, true,
                "**1. ¿El reflejo de Moro está ausente o es asimétrico? (Observe)",
                "**ALA 4.1. ¿El reflejo de Moro está ausente o es asimétrico?",
                "Realice en el niño la maniobra correspondiente para valorar este reflejo y durante su realización valore la simetría."
            ),
            alaItem(
                4, 2, true,
                "**2. ¿Cuándo lo sostiene del tronco su cabeza cae bruscamente hacia adelante, atrás o a los lados? (Observe)",
                "**ALA 4.2. ¿Cuándo lo sostiene del tronco su cabeza cae bruscamente hacia adelante, atrás o a los lados?",
                "Sostenga al bebé por el tronco y siéntelo, permitiendo que logre mantener su cabeza, sin dejarla caer hacia adelante, atrás o a los lados, aunque sea muy brevemente."
            )
        )
    )

    private fun buildGroup5(): EdiGroupSpec = EdiGroupSpec(
        groupNumber = 5,
        code = "G5",
        officialTitle = "Grupo 5 (de 5 meses a 6 meses 29 días)",
        shortIntervalLabel = "5m 0d - 6m 29d",
        minMonthsInclusive = 5,
        maxMonthsInclusive = 6,
        formatPages = 25..26,
        consultationPages = 67..70,
        generalHeaderMaterialsText = "ALE 5.3, MF 5.1 y MF 5.2: Sonaja, cubo u otro objeto de tamaño similar. SO 5.2: Tela blanca opaca de 30 x 30 cm.",
        itemsPerActiveArea = 2,
        fallbackPreviousGroupNumber = 4,
        fallbackItemsCountPerArea = 2,
        frbAndAleModifyGlobalTrafficLight = false,
        areaItems = mapOf(
            EdiDevArea.MG to listOf(
                devItem(
                    5, EdiDevArea.MG, 1, true,
                    "**1. Cuando usted toma a su bebé de los brazos para sentarlo, ¿él le ayuda jalándose y elevando la cabeza? (Observe)",
                    "**MG 5.1. Cuando usted toma a su bebé de los brazos para sentarlo, ¿él le ayuda jalándose y elevando la cabeza?",
                    "Coloque al bebé en decúbito dorsal (boca arriba) y levántelo tomándolo de los brazos, permita que eleve la cabeza y se impulse con el cuerpo hacia la posición sentado."
                ),
                devItem(
                    5, EdiDevArea.MG, 2, true,
                    "**2. Cuando usted para a su bebé sobre una superficie plana, ¿aguanta su peso sobre las piernas? (Observe)",
                    "**MG 5.2. Cuando usted para a su bebé sobre una superficie plana, ¿aguanta su peso sobre las piernas?",
                    "Coloque al bebé parado sobre una superficie firme mientras sostiene el tronco por debajo de las axilas permitiendo que sea capaz de aguantar todo el peso de su cuerpo sobre las piernas."
                )
            ),
            EdiDevArea.MF to listOf(
                devItem(
                    5, EdiDevArea.MF, 1, true,
                    "**1. Cuando su bebé agarra algún objeto, ¿se lo pasa de una mano a otra? (Observe)",
                    "**MF 5.1. Cuando su bebé agarra algún objeto, ¿se lo pasa de una mano a otra?",
                    "Coloque al bebé en decúbito supino (boca arriba) y ofrézcale un objeto atractivo (sonaja, cubo, etc.) para que lo tome con una de sus manos, una vez que lo sostenga ofrézcale un segundo objeto a la misma mano permitiendo que cambie el primer objeto a la mano contraria con la intención de tomar el segundo. Repetición del ítem: hasta 2 veces más de cada lado.",
                    headerMats = listOf("Sonaja, cubo u otro objeto de tamaño similar"),
                    protocolMats = listOf("Dos objetos atractivos (sonaja, cubo u otro de tamaño similar)")
                ),
                devItem(
                    5, EdiDevArea.MF, 2, true,
                    "**2. Cuando sienta a su bebé encima de usted, ¿intenta insistentemente agarrar un objeto cercano a él, aunque no pueda alcanzarlo? (Observe)",
                    "**MF 5.2. Cuando la madre, padre o cuidador principal sostiene al bebé, ¿intenta insistentemente agarrar un objeto cercano a él, aunque no pueda alcanzarlo?",
                    "Coloque al bebé sentado sobre las piernas del cuidador y sobre la mesa sitúe un objeto atractivo (sonaja, cubo o muñeco) moviéndolo para llamar su atención. Motívelo para que trate de coger el objeto.",
                    headerMats = listOf("Sonaja, cubo u otro objeto de tamaño similar"),
                    protocolMats = listOf("Sonaja, cubo o muñeco")
                )
            ),
            EdiDevArea.LE to listOf(
                devItem(
                    5, EdiDevArea.LE, 1, true,
                    "**1. Si llama a su bebé cuando él no lo puede ver, ¿mueve la cabeza hacia su voz? (Observe)",
                    "**LE 5.1. Si llama a su bebé cuando él no lo puede ver, ¿mueve la cabeza en dirección a su voz?",
                    "Pregunte a la madre, padre o cuidador principal qué nombre o sobrenombre utiliza para dirigirse al bebé en casa. Colóquese detrás suyo, sin que él lo note y llámelo por su nombre para que mueva la cabeza en dirección a su voz. Repetición del ítem: hasta 2 veces más de cada lado."
                ),
                devItem(
                    5, EdiDevArea.LE, 2, false,
                    "2. Si usted imita los sonidos que su bebé emite, ¿vuelve él a repetirlos?",
                    "LE 5.2. Si usted imita los sonidos que su bebé emite, ¿vuelve él a repetirlos?",
                    "Pregunte si juega con el bebé a conversar, por ejemplo: ¿cuándo usted escucha que el bebé hace \"aaaa\", usted le responde \"aaaa\" y él repite el sonido nuevamente? Defina la frecuencia e intensidad de la conducta en comparación con otros niños de su misma edad."
                )
            ),
            EdiDevArea.SO to listOf(
                devItem(
                    5, EdiDevArea.SO, 1, false,
                    "1. ¿Se pone intranquilo cuando una persona desconocida trata de cargarlo?",
                    "SO 5.1. ¿Se pone intranquilo cuando una persona desconocida trata de cargarlo?",
                    "¿Cómo se comporta el bebé cuando personas extrañas hacen el intento de cargarlo?, ¿el bebé levanta los brazos para que lo carguen?, ¿se muestra intranquilo o se comporta como con los otros miembros de la familia? Defina la frecuencia e intensidad de la conducta en comparación con otros niños de su misma edad."
                ),
                devItem(
                    5, EdiDevArea.SO, 2, true,
                    "**2. Cuando usted se tapa la cabeza con una tela, ¿se ríe cuando usted se destapa? (Observe)",
                    "**SO 5.2. Cuando usted se tapa la cabeza con una tela, ¿se ríe cuando usted se destapa?",
                    "Coloque al bebé sobre las piernas del cuidador y tome la tela blanca tapándose la cara, luego reaparezca a un lado de la tela con cara divertida. Puede acompañar el juego diciendo: \"aquí estoy\". Permitiendo que él mire con atención al lugar donde usted reaparezca e incluso reírse con el juego. Si el niño no responde, apóyese del cuidador. Repetición del ítem: hasta 2 veces más.",
                    headerMats = listOf("Tela blanca opaca de 30 x 30 cm"),
                    protocolMats = listOf("Tela blanca opaca de 30 x 30 cm")
                )
            ),
            EdiDevArea.CO to emptyList()
        ),
        aleItems = listOf(
            aleItem(
                5, 1, false,
                "1. ¿Presenta desinterés por sus manos?",
                "ALE 5.1. ¿Presenta desinterés por sus manos?",
                "Pregunte si el bebé se da cuenta de sus manos, si las mira mientras las mueve o por el contrario parece no darse cuenta de ellas. Defina la frecuencia e intensidad de la conducta en comparación con otros de su misma edad."
            ),
            aleItem(
                5, 2, false,
                "2. Rara vez o nunca le ha oído balbucear o emitir sonidos guturales.",
                "ALE 5.2. ¿Rara vez o nunca lo ha oído balbucear o emitir sonidos guturales?",
                "Pregunte: ¿qué sonidos hace el bebé? ¿Puede describirlos? Entre los sonidos que hace, ¿ha notado si dice \"gaaaa\" o \"agú\" o \"guuuu\"? Defina la frecuencia e intensidad de la conducta en comparación con otros niños de su misma edad."
            ),
            aleItem(
                5, 3, true,
                "**3. ¿Se le dificulta sostener objetos con las manos durante al menos 10 segundos? (Observe)",
                "**ALE 5.3. ¿Se le dificulta sostener objetos con las manos durante al menos 10 segundos?",
                "Coloque al bebé en decúbito dorsal (boca arriba) y ofrézcale un objeto atractivo (sonaja, cubo, etc.) para que lo mantenga en la mano al menos 10 segundos. Valore ambas manos. Repetición del ítem: hasta dos veces más de cada lado.",
                headerMats = listOf("Sonaja, cubo u otro objeto de tamaño similar"),
                protocolMats = listOf("Sonaja, cubo u otro objeto atractivo")
            ),
            aleItem(
                5, 4, true,
                "**4. Cuando está acostado y usted lo toma de las manos, ¿no hace esfuerzo para sentarse? (Observe)",
                "**ALE 5.4. Cuando está acostado y usted lo toma de las manos, ¿no hace esfuerzo para sentarse?",
                "Coloque al bebé en decúbito dorsal (boca arriba) y tomándolo de las manos levántelo, permita que eleve la cabeza y se impulse con el cuerpo hacia la posición de sentado."
            ),
            aleItem(
                5, 5, true,
                "**5. Cuando está boca arriba, ¿se le dificulta girar su cuerpo hacia los lados? (Observe)",
                "**ALE 5.5. Cuando está boca arriba, ¿se le dificulta girar su cuerpo hacia los lados?",
                "Coloque al niño en decúbito dorsal (boca arriba) llamando su atención hablándole, cantándole o mostrándole algún juguete atractivo para que logre ponerse de costado, dándose vuelta hacia el lado del estímulo. Repetición del ítem: hasta 2 veces más de cada lado."
            ),
            aleItem(
                5, 6, false,
                "6. ¿Considera que algo no va del todo bien en el comportamiento, desarrollo o características de su bebé?",
                "ALE 5.6. ¿Considera que algo no va del todo bien en el comportamiento, desarrollo o características de su bebé?",
                "Pregunte a la madre, padre o cuidador principal: ¿ha observado algo que le parece que no va del todo bien?, ¿tiene usted alguna preocupación por la manera en que su bebé se mueve, se comporta o se relaciona con los demás? Defina la frecuencia e intensidad de la conducta en comparación con otros niños de su misma edad."
            )
        ),
        alaItems = listOf(
            alaItem(
                5, 1, true,
                "**1. ¿Hay asimetría en los movimientos de las manos? (Observe)",
                "**ALA 5.1. ¿Hay asimetría en los movimientos de las manos?",
                "Analice si durante la consulta el bebé usa ambas manos o sólo una de ellas. Coloque objetos frente al bebé de ambos lados y vea que mueva las dos manos por igual."
            ),
            alaItem(
                5, 2, true,
                "**2. ¿Persiste el reflejo de Galant o de incurvación? (Observe)",
                "**ALA 5.2. ¿Persiste el reflejo de Galant o de incurvación?",
                "Coloque al bebé en decúbito ventral (boca abajo) y con su dedo índice aplique un estímulo en la línea paravertebral izquierda o derecha (2-3 cm al lado de la línea media), desde la región interescapular hasta el glúteo. El reflejo está presente si flexiona la cadera hacia el lado estimulado. Deben explorarse ambos lados y el reflejo debe ser simétrico."
            )
        )
    )

    private fun buildGroup6(): EdiGroupSpec = EdiGroupSpec(
        groupNumber = 6,
        code = "G6",
        officialTitle = "Grupo 6 (de 7 meses a 9 meses 29 días)",
        shortIntervalLabel = "7m 0d - 9m 29d",
        minMonthsInclusive = 7,
        maxMonthsInclusive = 9,
        formatPages = 28..29,
        consultationPages = 71..74,
        generalHeaderMaterialsText = "ALE 6.1: Dos cubos. ALE 6.2, ALE 6.4, MG 6.2 y MF 6.1: Sonaja, cubo u otro objeto de tamaño similar.",
        itemsPerActiveArea = 2,
        fallbackPreviousGroupNumber = 5,
        fallbackItemsCountPerArea = 2,
        frbAndAleModifyGlobalTrafficLight = false,
        areaItems = mapOf(
            EdiDevArea.MG to listOf(
                devItem(
                    6, EdiDevArea.MG, 1, true,
                    "**1. Al sentar a su bebé en el suelo, ¿permanece sentado aunque sea apoyándose en sus manos? (Observe)",
                    "**MG 6.1. Al sentar a su bebé en el suelo, ¿permanece sentado aunque sea apoyándose en sus manos?",
                    "Coloque al bebé sentado sobre una superficie firme sin apoyo y obsérvelo."
                ),
                devItem(
                    6, EdiDevArea.MG, 2, true,
                    "**2. Cuando su bebé quiere ir de un lado a otro y se encuentra boca arriba, ¿se voltea para quedar boca abajo y arrastrarse sobre su abdomen? (Observe)",
                    "**MG 6.2. Cuando su bebé quiere ir de un lado a otro y se encuentra boca arriba, ¿se voltea para quedar boca abajo y arrastrarse sobre su abdomen?",
                    "Coloque al bebé recostado boca arriba sobre una superficie plana y amplia donde pueda desplazarse. Enséñele un juguete y aléjelo de su alcance para que el bebé se ruede boca abajo y se arrastre hacia el objeto. Repetición del ítem: hasta 2 veces más de cada lado.",
                    headerMats = listOf("Sonaja, cubo u otro objeto de tamaño similar"),
                    protocolMats = listOf("Sonaja, cubo o juguete atractivo")
                )
            ),
            EdiDevArea.MF to listOf(
                devItem(
                    6, EdiDevArea.MF, 1, true,
                    "**1. Cuando quiere agarrar las cosas, ¿utiliza sus dedos como si fueran un rastrillo? (Observe)",
                    "**MF 6.1. Cuando quiere agarrar las cosas, ¿utiliza sus dedos como si estuviera arrastrando un objeto con los dedos separados (rastrillo)?",
                    "Sobre la mesa sitúe el cubo o un juguete llamativo y pequeño, moviéndolo para llamar su atención. Motívelo para que trate de arrastrar el objeto hacia él con la mano. Repetición del ítem: hasta 2 veces más.",
                    headerMats = listOf("Sonaja, cubo u otro objeto de tamaño similar"),
                    protocolMats = listOf("Cubo o juguete llamativo pequeño")
                ),
                devItem(
                    6, EdiDevArea.MF, 2, false,
                    "2. ¿Le gusta golpear objetos contra la mesa o el suelo?",
                    "MF 6.2. ¿Le gusta golpear objetos contra la mesa o el suelo?",
                    "Pregunte a la madre, padre o cuidador principal si le gusta golpear objetos contra la mesa o suelo, por ejemplo una cuchara o sus juguetes."
                )
            ),
            EdiDevArea.LE to listOf(
                devItem(
                    6, EdiDevArea.LE, 1, false,
                    "1. ¿Puede hacer dos sonidos iguales como \"ba-ba\", \"da-da\", \"ta-ta\"?",
                    "LE 6.1. ¿Puede hacer dos sonidos iguales como \"ba-ba\", \"da-da\", \"ta-ta\"?",
                    "Pregunte a la madre, padre o cuidador principal si de manera espontánea, emite sonidos de dos o más sílabas en combinación consonante-vocal de manera sucesiva, como por ejemplo: \"ba-ba\", \"da-da\", \"ta-ta\". ¿Qué sonidos hace su bebé?, ¿puede describirlos?, ¿en qué momentos hace esos sonidos?"
                ),
                devItem(
                    6, EdiDevArea.LE, 2, false,
                    "2. ¿Hace gestos para decir lo que quiere?",
                    "LE 6.2. ¿Hace gestos o señas para decir lo que quiere?",
                    "Pregunte a la madre, padre o cuidador principal si el bebé utiliza gestos para comunicar sus necesidades: ¿qué gestos hace el bebé?, ¿puede describirlos? Entre los gestos que hace ¿ha notado si señala con la mano, si niega con la cabeza, si levanta los brazos cuando quiere que lo carguen?"
                )
            ),
            EdiDevArea.SO to listOf(
                devItem(
                    6, EdiDevArea.SO, 1, false,
                    "1. Cuando está con otras personas conocidas, ¿reacciona ante sus expresiones y parece contento?",
                    "SO 6.1. Cuando está con otras personas conocidas, ¿reacciona ante sus expresiones y parece contento?",
                    "Pregunte a la madre, padre o cuidador principal sobre el interés social del bebé y su respuesta a la interacción de los demás. Describa qué hace cuando está con familiares cercanos como tíos, primos, abuelos. ¿Parece contento si está con personas de la familia y reacciona ante sus expresiones?"
                ),
                devItem(
                    6, EdiDevArea.SO, 2, false,
                    "2. Cuando le da de beber líquidos, ¿Lo ayuda a sostener el vaso o la taza?",
                    "SO 6.2. Cuando le da de beber líquidos, ¿lo ayuda a sostener/coger el vaso o la taza?",
                    "Pregunte a la madre, padre o cuidador principal: ¿él sostiene su vaso, aunque sea unos minutos?, ¿cuánto tiempo es capaz de sostener la taza si está sentado?, ¿bebe de su vaso o taza sin ayuda?"
                )
            ),
            EdiDevArea.CO to emptyList()
        ),
        aleItems = listOf(
            aleItem(
                6, 1, true,
                "**1. ¿Se le dificulta mantener un objeto en cada mano? (Observe)",
                "**ALE 6.1. ¿Se le dificulta mantener un objeto en cada mano?",
                "Coloque al bebé sentado sobre las piernas del cuidador. Muéstrele un cubo y atraiga su atención golpeándolo contra la mesa. Cuando tome el cubo saque otro y haga lo mismo, dejándolo cerca de la mano libre del bebé.",
                headerMats = listOf("Dos cubos"),
                protocolMats = listOf("Dos cubos")
            ),
            aleItem(
                6, 2, true,
                "**2. ¿Se le dificulta seguir con la mirada un objeto que se cayó? (Observe)",
                "**ALE 6.2. ¿Se le dificulta seguir con la mirada un objeto que se cayó?",
                "Colóquese frente al bebé y llame su atención con algún juguete. Deje que tome el objeto y juegue con él. Posteriormente, de manera rápida y tratando de que lo vea, arroje el juguete al suelo y observe su reacción.",
                headerMats = listOf("Sonaja, cubo u otro objeto de tamaño similar"),
                protocolMats = listOf("Sonaja, cubo o juguete")
            ),
            aleItem(
                6, 3, true,
                "**3. ¿Es indiferente a los objetos de su entorno? (Observe)",
                "**ALE 6.3. ¿Es indiferente a los objetos de su entorno?",
                "Deje diversos juguetes como muñecos, carros, sonajas o cubos al alcance del bebé. Llame su atención con los juguetes y después déjelo tranquilo para que juegue con ellos.",
                protocolMats = listOf("Muñecos, carros, sonajas o cubos")
            ),
            aleItem(
                6, 4, false,
                "4. ¿Considera que algo no va del todo bien en el comportamiento, desarrollo o características de su bebé?",
                "ALE 6.4. ¿Considera que algo no va del todo bien en el comportamiento, desarrollo o características de su bebé?",
                "Pregunte a la madre, padre o cuidador principal: ¿ha observado algo que le parece que no va del todo bien?, ¿tiene usted alguna preocupación por la manera en que su bebé se mueve, se comporta o se relaciona con los demás? Defina la frecuencia e intensidad de la conducta en comparación con otros niños de su misma edad.",
                headerMats = listOf("Sonaja, cubo u otro objeto de tamaño similar (listado en encabezado general pág. 71)"),
                protocolMats = emptyList()
            )
        ),
        alaItems = listOf(
            alaItem(
                6, 1, true,
                "**1. ¿Persiste el reflejo de Moro? (Observe)",
                "**ALA 6.1. ¿Persiste el reflejo de Moro?",
                "Realice en el bebé la maniobra correspondiente para valorar este reflejo y durante su realización valore la simetría."
            ),
            alaItem(
                6, 2, true,
                "**2. ¿Se encuentra ausente el reflejo de paracaídas? (Observe)",
                "**ALA 6.2. ¿Se encuentra ausente el reflejo de paracaídas?",
                "Colóquese detrás del bebé, sujételo fuerte pero gentilmente de la espalda y cadera con ambas manos, acérquelo a usted en suspensión ventral (suspendido boca abajo). Después, sujételo firmemente e inclínelo con la cabeza hacia adelante sobre la mesa de exploración, cuidando que no se golpee o se caiga durante la maniobra. Usted observará que el bebé realiza un movimiento brusco de extensión de los miembros superiores, con apertura de las manos, como si quisiera protegerse de una caída."
            )
        )
    )

    private fun buildGroup7(): EdiGroupSpec = EdiGroupSpec(
        groupNumber = 7,
        code = "G7",
        officialTitle = "Grupo 7 (de 10 meses a 12 meses 29 días)",
        shortIntervalLabel = "10m 0d - 12m 29d",
        minMonthsInclusive = 10,
        maxMonthsInclusive = 12,
        formatPages = 30..31,
        consultationPages = 75..77,
        generalHeaderMaterialsText = "ALE 7.1 y MG 7.1: Tapete y juguete llamativo; ALE 7.2 y LE 7.1: Sonaja o juguete llamativo; MF 7.1: Objeto pequeño o bola pequeña de papel.",
        itemsPerActiveArea = 2,
        fallbackPreviousGroupNumber = 6,
        fallbackItemsCountPerArea = 2,
        frbAndAleModifyGlobalTrafficLight = false,
        areaItems = mapOf(
            EdiDevArea.MG to listOf(
                devItem(
                    7, EdiDevArea.MG, 1, true,
                    "**1. Cuando su bebé se desplaza de un lado a otro, ¿utiliza sus manos y rodillas? (Observe)",
                    "**MG 7.1. Cuando su bebé se desplaza de un lado a otro, ¿utiliza sus manos y rodillas?",
                    "Coloque al bebé sobre una superficie firme y segura, como un tapete, de manera que se apoye en rodillas y manos. Estando a un metro de distancia frente a él, llame su atención con un juguete atractivo y déjelo sobre el suelo alentándolo para que lo busque.",
                    headerMats = listOf("Tapete y juguete llamativo"),
                    protocolMats = listOf("Tapete y juguete llamativo")
                ),
                devItem(
                    7, EdiDevArea.MG, 2, true,
                    "**2. ¿Puede su bebé caminar agarrado de los muebles? (Observe)",
                    "**MG 7.2. ¿Puede su niño caminar agarrado de los muebles?",
                    "Coloque un par de sillas firmes, una al lado de la otra y cercanas a la mesa. Coloque al niño en posición de pie, tomado de alguno de los muebles. Pídale al cuidador que se sitúe a un metro de distancia y que lo llame para que quiera ir hacia él.",
                    protocolMats = listOf("Dos sillas firmes o muebles de apoyo")
                )
            ),
            EdiDevArea.MF to listOf(
                devItem(
                    7, EdiDevArea.MF, 1, true,
                    "**1. ¿Puede sujetar un objeto pequeño con los dedos y mantenerlo en el centro de la mano? (Observe)",
                    "**MF 7.1. ¿Puede sujetar un objeto pequeño con los dedos y mantenerlo en el centro de la mano?",
                    "Coloque al bebé sentado con sus brazos encima de la mesa. Coloque un objeto pequeño sobre la mesa moviéndolo para llamar su atención. Motívelo para que trate de tomarlo con la mano.",
                    headerMats = listOf("Objeto pequeño o bola pequeña de papel"),
                    protocolMats = listOf("Objeto pequeño o bola pequeña de papel")
                ),
                devItem(
                    7, EdiDevArea.MF, 2, false,
                    "2. ¿Sabe aplaudir?",
                    "MF 7.2. ¿Sabe aplaudir?",
                    "Pregunte a la madre, padre o cuidador principal si el bebé ya sabe aplaudir. Pídale que le dé ejemplos de cómo lo hace."
                )
            ),
            EdiDevArea.LE to listOf(
                devItem(
                    7, EdiDevArea.LE, 1, true,
                    "**1. Cuando su bebé está jugando en una actividad que lo tiene entretenido y usted le dice \"no, no\", ¿deja de hacerla? (Observe)",
                    "**LE 7.1. Cuando su niño está jugando en una actividad que lo tiene entretenido y usted le dice \"no, no\", ¿deja de hacerla, aunque sea brevemente?",
                    "Deje sobre la mesa un objeto atractivo a una distancia cercana, para que el niño intente cogerlo con la mano. Cuando esté haciendo el movimiento para alcanzar el juguete dígale con voz clara y firme \"no, no\".",
                    headerMats = listOf("Sonaja o juguete llamativo"),
                    protocolMats = listOf("Sonaja o juguete llamativo")
                ),
                devItem(
                    7, EdiDevArea.LE, 2, false,
                    "2. ¿Dice papá o mamá?",
                    "LE 7.2. ¿Dice papá o mamá?",
                    "Pregunte a la madre, padre o cuidador principal: ¿dice el bebé la palabra mamá?, ¿puede usted repetir exactamente como lo dice?, ¿dice \"mamá\" o dice \"ma-ma-ma-ma\"?, ¿se lo dice a la mamá únicamente o a todas las mujeres?, ¿dice \"papá\" o \"pa-pa-pa-pa\"?, ¿lo dice solo cuando ve a su papá o con cualquier persona u objeto?"
                )
            ),
            EdiDevArea.SO to listOf(
                devItem(
                    7, EdiDevArea.SO, 1, false,
                    "1. ¿Empieza a comer por sí solo con los dedos?",
                    "SO 7.1. ¿Empieza a comer por sí solo con los dedos?",
                    "Pregunte a la madre, padre o cuidador principal: ¿cómo come?, ¿está usando los dedos para llevarse a la boca algunos alimentos?. Por ejemplo: ¿come pequeños trocitos de pan, carne cortada en pequeños bocados, verduras o frutas?"
                ),
                devItem(
                    7, EdiDevArea.SO, 2, false,
                    "2. Cuando lo viste, ¿le ayuda terminando de meter el brazo por la manga?",
                    "SO 7.2. Cuando lo viste, ¿puede ayudarle terminando de meter el brazo por la manga?",
                    "Pregunte a la madre, padre o cuidador principal: ¿cómo se comporta cuando usted lo viste?, ¿ayuda de alguna manera estirando el brazo o la pierna para pasarlos por la ropa?"
                )
            ),
            EdiDevArea.CO to emptyList()
        ),
        aleItems = listOf(
            aleItem(
                7, 1, true,
                "**1. ¿Se le dificulta desplazarse o gatear? (Observe)",
                "**ALE 7.1. ¿Se le dificulta desplazarse o gatear?",
                "Coloque al niño sentado en el suelo y llame su atención con un juguete atractivo que se encuentre fuera de su alcance para que el niño tenga que desplazarse sobre su abdomen o gateando para llegar a él.",
                headerMats = listOf("Tapete y juguete llamativo"),
                protocolMats = listOf("Tapete y juguete llamativo")
            ),
            aleItem(
                7, 2, true,
                "**2. ¿Es indiferente a los objetos de su entorno? (Observe)",
                "**ALE 7.2. ¿Es indiferente a los objetos de su entorno?",
                "Deje diversos juguetes como muñecos, carros, sonajas, cubos al alcance del bebé. Llame su atención con los juguetes y después déjelo tranquilo para que juegue con ellos.",
                headerMats = listOf("Sonaja o juguete llamativo"),
                protocolMats = listOf("Muñecos, carros, sonajas o cubos")
            ),
            aleItem(
                7, 3, false,
                "3. ¿Rara vez lo ha visto jugar?",
                "ALE 7.3. ¿Rara vez lo ha visto jugar?",
                "Pregunte a la madre, padre o cuidador principal: ¿tiene algunos juguetes en casa?, ¿cuando está cerca de ellos, juega con ellos, o los ignora y se queda sin hacer nada? El objetivo de la pregunta es ver si interactúa con los objetos del entorno o es indiferente al medio que lo rodea."
            ),
            aleItem(
                7, 4, false,
                "4. ¿Considera que algo no va del todo bien en el comportamiento, desarrollo o características de su bebé?",
                "ALE 7.4. ¿Considera que algo no va del todo bien en el comportamiento, desarrollo o características de su bebé?",
                "Pregunte a la madre, padre o cuidador principal: ¿ha observado algo que le parece que no va del todo bien?, ¿tiene usted alguna preocupación por la manera en que su bebé se mueve, se comporta o se relaciona con los demás? Defina la frecuencia e intensidad de la conducta en comparación con otros niños de su misma edad."
            )
        ),
        alaItems = listOf(
            alaItem(
                7, 1, true,
                "**1. ¿Necesita apoyarse en sus manos para permanecer sentado? (Observe)",
                "**ALA 7.1. ¿Necesita apoyarse en sus manos para permanecer sentado?",
                "Coloque al bebé sentado sobre una superficie firme sin apoyo y obsérvelo."
            ),
            alaItem(
                7, 2, false,
                "2. ¿Se le dificulta comunicarse, ya sea balbuceando o con gestos? Por ejemplo: señalar lo que desea, mover la mano para saludar, hacer señas, jalar para llamar la atención.",
                "ALA 7.2. ¿Se le dificulta comunicarse, ya sea balbuceando o con gestos? Por ejemplo: señalar lo que desea, mover la mano para saludar, hacer señas, jalar para llamar la atención.",
                "Pregunte a la madre, padre o cuidador principal si el bebé tiene interés y desea comunicarse con los demás. Lo importante es el esfuerzo y la intención de comunicarse con otros, no importa el nivel de lenguaje que haya alcanzado."
            )
        )
    )

    private fun buildGroup8(): EdiGroupSpec = EdiGroupSpec(
        groupNumber = 8,
        code = "G8",
        officialTitle = "Grupo 8 (de 13 meses a 15 meses 29 días)",
        shortIntervalLabel = "13m 0d - 15m 29d",
        minMonthsInclusive = 13,
        maxMonthsInclusive = 15,
        formatPages = 32..33,
        consultationPages = 79..82,
        generalHeaderMaterialsText = "MG 8.2 y MG 8.3: Tapete y sonaja o juguete llamativo. MF 8.2: Caja de cartón o bolsa con varios objetos de tamaño mediano. MF 8.3: Objetos pequeños o bolas pequeñas de papel. SO 8.3: Pelota o juguete llamativo. ALA 8.1: Juguete llamativo.",
        itemsPerActiveArea = 3,
        fallbackPreviousGroupNumber = 7,
        fallbackItemsCountPerArea = 2,
        frbAndAleModifyGlobalTrafficLight = false,
        areaItems = mapOf(
            EdiDevArea.MG to listOf(
                devItem(
                    8, EdiDevArea.MG, 1, true,
                    "**1. ¿Camina solo? (Observe)",
                    "**MG 8.1. ¿Camina solo?",
                    "Si el niño puede mantenerse de pie por sí solo, sitúelo en el centro del consultorio, a un metro de distancia del cuidador, lejos de muebles o paredes en los cuales pueda apoyarse. Luego pida al cuidador que lo llame para que vaya hacia él."
                ),
                devItem(
                    8, EdiDevArea.MG, 2, true,
                    "**2. Cuando su niño está sentado, ¿puede pararse solo? (Observe)",
                    "**MG 8.2. Cuando su niño está sentado, ¿puede pararse solo?",
                    "Siente al niño en una silla pequeña o en un tapete y motívelo para que se levante. Observe si logra ponerse de pie desde la posición de sentado apoyando ligeramente las manos en el suelo.",
                    headerMats = listOf("Tapete y sonaja o juguete llamativo"),
                    protocolMats = listOf("Tapete o silla pequeña")
                ),
                devItem(
                    8, EdiDevArea.MG, 3, true,
                    "**3. ¿Puede su niño agacharse para agarrar un objeto del suelo y volver a ponerse de pie sin apoyarse en algo? (Observe)",
                    "**MG 8.3. ¿Puede su niño agacharse para agarrar un objeto del suelo y volver a ponerse de pie sin apoyarse en algo?",
                    "Sitúe al niño de pie en el centro del consultorio, lejos de muebles en donde pueda apoyarse. Coloque frente a él un juguete atractivo en el piso, pídale que agarre el juguete y se lo entregue.",
                    headerMats = listOf("Tapete y sonaja o juguete llamativo"),
                    protocolMats = listOf("Sonaja o juguete llamativo")
                )
            ),
            EdiDevArea.MF to listOf(
                devItem(
                    8, EdiDevArea.MF, 1, false,
                    "1. ¿Le gusta tirar o aventar juguetes u otros objetos al suelo?",
                    "MF 8.1. ¿Le gusta tirar o lanzar juguetes u otros objetos al suelo?",
                    "Pregunte a la madre, padre o cuidador principal si le gusta tirar o aventar juguetes al suelo o si encuentra divertido tirarlo una y otra vez, esperando que el cuidador lo levante del suelo y se lo devuelva."
                ),
                devItem(
                    8, EdiDevArea.MF, 2, true,
                    "**2. ¿Le gusta sacar y meter cosas de las bolsas, cajas o recipientes? (Observe)",
                    "**MF 8.2. ¿Le gusta sacar y meter cosas de las bolsas, cajas o recipientes?",
                    "Coloque la caja, bolsa o recipiente sobre la mesa. Ábrala y dígale al niño: \"Mira, aquí hay unos juguetes divertidos. Voy a sacar algunos\". Empuje la caja hacia él y estimúlelo para que los saque. \"Ahora sácalos tú y míralos. ¿Te gustaría jugar con ellos?\". Luego de que el niño haya sacado por lo menos 4 juguetes deje que juegue un rato con ellos y diga: \"Ahora vamos a guardar todos los juguetes\".",
                    headerMats = listOf("Caja de cartón o bolsa con varios objetos de tamaño mediano"),
                    protocolMats = listOf("Caja de cartón o bolsa con al menos 4 objetos de tamaño mediano")
                ),
                devItem(
                    8, EdiDevArea.MF, 3, true,
                    "**3. Sin apoyar la mano o el brazo en la mesa, ¿puede tomar una bolita de papel utilizando las yemas de los dedos? (Observe)",
                    "**MF 8.3. Sin apoyar la mano o el brazo en la mesa, ¿puede tomar una bolita de papel utilizando las yemas de los dedos?",
                    "Coloque al niño sentado sobre las piernas del cuidador. Sitúe sobre la mesa una bolita de papel, motívelo para que la tome con las yemas de los dedos.",
                    headerMats = listOf("Objetos pequeños o bolas pequeñas de papel"),
                    protocolMats = listOf("Bolita pequeña de papel")
                )
            ),
            EdiDevArea.LE to listOf(
                devItem(
                    8, EdiDevArea.LE, 1, true,
                    "**1. ¿Puede señalar su zapato o alguna parte del cuerpo? (Observe)",
                    "**LE 8.1. ¿Puede señalar su zapato o alguna parte del cuerpo?",
                    "Colóquese frente al niño y pregúntele de manera divertida: ¿dónde está tu zapato?, ¿dónde están tus ojos?, ¿dónde está tu mano?"
                ),
                devItem(
                    8, EdiDevArea.LE, 2, false,
                    "2. ¿Puede decir otra palabra además de papá y mamá?",
                    "LE 8.2. ¿Puede decir otra palabra además de papá y mamá?",
                    "Pregunte a la madre, padre o cuidador principal: ¿dice algunas palabras además de mamá y papá?, ¿cuáles son? Las palabras deben ser apropiadas para mencionar objetos o situaciones específicas. No es necesario que las palabras sean pronunciadas claramente."
                ),
                devItem(
                    8, EdiDevArea.LE, 3, false,
                    "3. Cuando quiere algo, ¿se lo comunica con gestos y sonidos?",
                    "LE 8.3. Cuando quiere algo, ¿se lo comunica con gestos y sonidos?",
                    "Pregunte a la madre, padre o cuidador principal: ¿qué gestos hace?, ¿puede describirlos? Cuando hace gestos ¿ha notado si los acompaña con sonidos? Por ejemplo señala para pedir algo y vocaliza, jala de la ropa y vocaliza, niega con la cabeza y dice \"no\", levanta la taza y dice \"eche\" o levanta las manos para que lo carguen y grita o vocaliza."
                )
            ),
            EdiDevArea.SO to listOf(
                devItem(
                    8, EdiDevArea.SO, 1, false,
                    "1. ¿Come solo, sin que alguien le ayude?",
                    "SO 8.1. ¿Come solo, sin que alguien le ayude?",
                    "Pregunte a la madre, padre o cuidador principal si es capaz de comer solo algunas comidas sencillas. Puede ser con los dedos o con alguna cuchara, si ya la maneja. Lo importante es valorar si se alimenta a sí mismo. Debe ser capaz de hacerlo varias veces al día, sobre todo cuando se trata de comidas simples como verduras, fideos, galletas, trocitos de carne, etc."
                ),
                devItem(
                    8, EdiDevArea.SO, 2, false,
                    "2. ¿Le gusta abrazar o besar a sus papás?",
                    "SO 8.2. ¿Le gusta abrazar o besar a sus padres/cuidadores?",
                    "Pregunte a la madre, padre o cuidador principal ¿demuestra afecto e interés por sus padres?, ¿suele abrazar y besar a papá o mamá u otros adultos o cuidadores cercanos?, ¿demuestra cariño hacia alguna persona en particular?"
                ),
                devItem(
                    8, EdiDevArea.SO, 3, true,
                    "**3. ¿Obedece órdenes sencillas como: \"ven\", \"dámelo\", \"trae tu pelota\", etc.? (Observe)",
                    "**SO 8.3. ¿Obedece órdenes sencillas como: \"ven\", \"dámelo\", \"trae tu pelota\", etc.?",
                    "Haga contacto visual con el niño y dele una orden sencilla como \"ven\" o \"trae la pelota\". El objetivo de este ítem es evaluar la respuesta social al adulto, si responde a sus pedidos y llamados, si mira al adulto cuando se le llama por su nombre, si intenta cooperar y satisfacer los pedidos que le hace el adulto.",
                    headerMats = listOf("Pelota o juguete llamativo")
                )
            ),
            EdiDevArea.CO to emptyList()
        ),
        aleItems = listOf(
            aleItem(
                8, 1, true,
                "**1. Cuando coloca de pie al niño en una superficie plana, ¿no logra mantenerse parado, aunque lo sujeten del tronco? (Observe)",
                "**ALE 8.1. Cuando colocas de pie a tu niño en una superficie plana, ¿no logra mantenerse parado aunque lo sujetes del tronco?",
                "Coloque al bebé de pie sobre la mesa mientras lo sostiene firmemente del tronco, observe si logra la postura completa de manera independiente."
            ),
            aleItem(
                8, 2, false,
                "2. ¿Se le dificulta comunicarse de forma verbal?",
                "ALE 8.2. ¿Se le dificulta comunicarse de forma verbal?",
                "Pregunte a la madre, padre o cuidador principal o si al bebé se le dificulta comunicarse con los demás utilizando palabras o sonidos."
            ),
            aleItem(
                8, 3, false,
                "3. ¿Aún balbucea y no se expresa con palabras?",
                "ALE 8.3. ¿Aún balbucea y no se expresa con palabras?",
                "Pregunte a la madre, padre o cuidador principal: ¿solo se comunica con balbuceos sin articular ninguna palabra?"
            ),
            aleItem(
                8, 4, false,
                "4. ¿Considera que algo no va del todo bien en el comportamiento, desarrollo o características de su bebé?",
                "ALE 8.4. ¿Considera que algo no va del todo bien en el comportamiento, desarrollo o características de su bebé?",
                "Pregunte a la madre, padre o cuidador principal: ¿ha observado algo que le parece que no va del todo bien?, ¿tiene usted alguna preocupación por la manera en que el bebé se mueve, se comporta o se relaciona con los demás? Defina la frecuencia e intensidad de la conducta en comparación con otros niños de su misma edad."
            )
        ),
        alaItems = listOf(
            alaItem(
                8, 1, true,
                "**1. ¿Se le dificulta desplazarse solo o gatear? (Observe)",
                "**ALA 8.1. ¿Se le dificulta desplazarse solo o gatear?",
                "Coloque al niño sentado en el suelo y llame su atención con un juguete atractivo que se encuentre fuera de su alcance. Deje muy cerca una silla pequeña o un mueble para que se apoye al ponerse de pie si lo necesita o quédese usted cerca para que utilice sus piernas de apoyo. Para que esté presente esta señal de alarma, el niño debe ser incapaz de ponerse de pie y caminar, de gatear hasta el juguete o de arrastrarse sobre su abdomen.",
                headerMats = listOf("Juguete llamativo"),
                protocolMats = listOf("Juguete llamativo")
            )
        )
    )

    private fun buildGroup9(): EdiGroupSpec = EdiGroupSpec(
        groupNumber = 9,
        code = "G9",
        officialTitle = "Grupo 9 (de 16 meses a 18 meses 29 días)",
        shortIntervalLabel = "16m 0d - 18m 29d",
        minMonthsInclusive = 16,
        maxMonthsInclusive = 18,
        formatPages = 34..35,
        consultationPages = 83..86,
        generalHeaderMaterialsText = "ALE 9.1 y LE 9.3: Lámina A. ALE 9.2 y MG 9.2: Pelota. MF 9.1: Cinco cubos. MF 9.2: Frasco de boca chica y objetos pequeños. MF 9.3: Papel y crayones.",
        itemsPerActiveArea = 3,
        fallbackPreviousGroupNumber = 8,
        fallbackItemsCountPerArea = 3,
        frbAndAleModifyGlobalTrafficLight = false,
        areaItems = mapOf(
            EdiDevArea.MG to listOf(
                devItem(
                    9, EdiDevArea.MG, 1, false,
                    "1. ¿Puede su niño sentarse solo en una silla pequeña?",
                    "MG 9.1. ¿Puede su niño sentarse solo en una silla pequeña?",
                    "Pregunte a la madre, padre o cuidador principal: ¿es capaz de sentarse solo en una silla pequeña o necesita ayuda?"
                ),
                devItem(
                    9, EdiDevArea.MG, 2, true,
                    "**2. ¿Puede su niño patear una pelota? (Observe)",
                    "**MG 9.2. ¿Puede su niño patear una pelota?",
                    "Ubíquese junto al niño en un lugar con espacio suficiente, lejos de objetos que puedan romperse o caerse. Llame su atención de manera divertida y diga: \"Vamos a jugar a la pelota. Muéstrame como pateas la pelota\". Si el niño no lo hace muéstrele cómo hacerlo. Observe si lo hace.",
                    headerMats = listOf("Pelota"),
                    protocolMats = listOf("Pelota")
                ),
                devItem(
                    9, EdiDevArea.MG, 3, true,
                    "**3. ¿Corre aunque sea de forma torpe? (Observe)",
                    "**MG 9.3. ¿Corre aunque sea de forma torpe?",
                    "En un lugar con espacio suficiente y de manera divertida invite al niño a jugar a las carreritas. Dígale por su nombre: \"¡vamos a correr! Mira como lo hago\". Observe si lo hace."
                )
            ),
            EdiDevArea.MF to listOf(
                devItem(
                    9, EdiDevArea.MF, 1, true,
                    "**1. ¿Sabe colocar cubos uno encima del otro? (Observe)",
                    "**MF 9.1. ¿Sabe colocar cubos uno encima del otro?",
                    "Siéntelo con los brazos sobre la mesa. Colóquese de frente y muéstrale los cubos. Dígale: \"mira, vamos a hacer una torre\". Construya una torre con dos cubos y diga: \"ahora hazla tú\". Empuje el resto de los cubos pero deje el modelo que usted construyó sobre la mesa. Observe si lo hace. Repetición del ítem: hasta 2 veces más.",
                    headerMats = listOf("Cinco cubos"),
                    protocolMats = listOf("Cinco cubos")
                ),
                devItem(
                    9, EdiDevArea.MF, 2, true,
                    "**2. ¿Puede meter bolitas de papel en un frasco de plástico que tenga la boca chica? (Observe)",
                    "**MF 9.2. ¿Puede meter bolitas de papel en un frasco de plástico que tenga la boca chica?",
                    "Siente al niño con los brazos encima de la mesa. Colóquese de frente y muéstrele la bola pequeña de papel y el frasco de plástico. Diga: \"mira, vamos a meter esta bolita de papel dentro del frasco\". Coloque la bola pequeña de papel asegurándose de que lo observe. Luego vierta la bola dando vuelta al frasco y acérquelos en su dirección. Diga: \"ahora hazlo tú\". Observe si lo hace. Repetición del ítem: hasta 2 veces más.",
                    headerMats = listOf("Frasco de boca chica y objetos pequeños"),
                    protocolMats = listOf("Frasco de plástico de boca chica y bolita pequeña de papel")
                ),
                devItem(
                    9, EdiDevArea.MF, 3, true,
                    "**3. Si usted le da un pedazo de papel y un lápiz o crayola, ¿utiliza la punta para tratar de dibujar? (Observe)",
                    "**MF 9.3. Si usted le da un pedazo de papel y un lápiz o crayola, ¿utiliza la punta para tratar de dibujar?",
                    "Ponga frente al niño papel y un lápiz o crayola. Observe si trata de dibujar.",
                    headerMats = listOf("Papel y crayones"),
                    protocolMats = listOf("Papel y lápiz o crayola")
                )
            ),
            EdiDevArea.LE to listOf(
                devItem(
                    9, EdiDevArea.LE, 1, false,
                    "1. ¿Sabe decir cuatro palabras además de \"mamá\" y \"papá\"?",
                    "LE 9.1. ¿Sabe decir cuatro palabras además de \"mamá\" y \"papá\"?",
                    "Pregunte a la madre, padre o cuidador principal: qué palabras con sentido y de manera espontánea dice el niño. Haga una lista. Por ejemplo puede decir \"guau-guau\" para perro, \"eche\" para leche, \"aba\" para agua, etc. Si el cuidador tiene problemas para recordar, enumere usted las más frecuentes preguntando que palabras utiliza para nombrar lo siguiente: Pan, Silla, Mesa, Carro, Leche, Pelota, Perro, Teta."
                ),
                devItem(
                    9, EdiDevArea.LE, 2, true,
                    "**2. ¿Puede señalar 2 o 3 partes de su cuerpo? (Observe)",
                    "**LE 9.2. ¿Puede señalar 2 o 3 partes de su cuerpo?",
                    "Pregunte a la madre, padre o cuidador principal qué partes del cuerpo conoce y proceda a preguntarle al niño de forma divertida: ¿dónde están tus ojos?, \"muéstrame tu nariz\". Observe si lo hace. Repetición del ítem: hasta 2 veces."
                ),
                devItem(
                    9, EdiDevArea.LE, 3, true,
                    "**3. ¿Sabe el nombre de algunos objetos? (Observe)",
                    "**LE 9.3. ¿Sabe el nombre de algunos objetos?",
                    "Colóquese frente al niño y muéstrele la lámina A (Manual para la Formación de Facilitadores en la Prueba Evaluación del Desarrollo Infantil, página 109). Comience a señalar cada dibujo, pregúntele: \"¿qué es esto?, ¿cómo se llama?\". Observe si el niño nombra cada uno de los objetos.",
                    headerMats = listOf("Lámina A"),
                    protocolMats = listOf("Lámina A (6 dibujos: flor, oso, pelota, taza, perro, carro)"),
                    plates = setOf(EdiOfficialPlate.LAMINA_A)
                )
            ),
            EdiDevArea.SO to listOf(
                devItem(
                    9, EdiDevArea.SO, 1, false,
                    "1. ¿Utiliza la cuchara para comer?",
                    "SO 9.1. ¿Utiliza la cuchara para comer?",
                    "Pregunte a la madre, padre o cuidador principal: ¿ya está usando la cuchara para comer?, ¿derrama mucho el alimento o maneja la cuchara con habilidad? Puede sustituir cuchara por pan en la misma oración, en caso de ser necesario."
                ),
                devItem(
                    9, EdiDevArea.SO, 2, false,
                    "2. Cuando va a desvestir a su niño, ¿le ayuda quitándose parte de su ropa, como los zapatos o sandalias?",
                    "SO 9.2. Cuando va a desvestir a su niño, ¿le ayuda quitándose parte de su ropa, como los zapatos o sandalias?",
                    "Pregunte a la madre, padre o cuidador principal: ¿cuando usted desviste a su niño, él intenta ayudarla quitándose alguna prenda?, ¿qué ropa se quita?"
                ),
                devItem(
                    9, EdiDevArea.SO, 3, false,
                    "3. A su hijo, ¿le gusta imitarlo en tareas de la casa como barrer o sacudir?",
                    "SO 9.3. A su hijo, ¿le gusta imitarlo en tareas de la casa como barrer o sacudir?",
                    "Pregunte a la madre, padre o cuidador principal: ¿suele interesarse el niño por lo que usted hace en la casa?, ¿lo ha visto imitando alguna tarea que usted hace habitualmente, como barrer o limpiar?"
                )
            ),
            EdiDevArea.CO to emptyList()
        ),
        aleItems = listOf(
            // NOTA CLÍNICA AUDITADA (Condición 5):
            // Aunque el encabezado general del Grupo 9 (pág. 83) lista "ALE 9.1 y LE 9.3: Lámina A",
            // el protocolo clínico directo de ALE 9.1 pregunta al niño por su papá, mamá o cuidador
            // que lo acompañe en el consultorio y NO utiliza la Lámina A.
            aleItem(
                9, 1, true,
                "**1. Cuando le preguntan dónde está una persona conocida, ¿es incapaz de señalarla? (Observe)",
                "**ALE 9.1. Cuando le preguntan dónde está una persona conocida, ¿es incapaz de señalarla?",
                "Pregunte al niño: ¿dónde está tu papá? (mamá o cuidador que lo acompañe).",
                headerMats = listOf("Lámina A (listada de manera general en encabezado de Grupo 9, pág. 83; no utilizada en el protocolo directo)"),
                protocolMats = emptyList(),
                plates = emptySet()
            ),
            aleItem(
                9, 2, true,
                "**2. ¿Se le dificulta seguir órdenes sencillas, como: \"trae la pelota\"? (Observe)",
                "**ALE 9.2. ¿Se le dificulta seguir órdenes sencillas, como: \"trae la pelota\"?",
                "Ubique la pelota en el otro extremo de la habitación y luego acérquese al niño y dígale por su nombre \"por favor, ¡trae la pelota!\". Recuerde no hacer gestos con la mano o cabeza. Observe la conducta del niño.",
                headerMats = listOf("Pelota"),
                protocolMats = listOf("Pelota")
            ),
            aleItem(
                9, 3, false,
                "3. ¿Se enoja mucho y tiene dificultad para calmarse, comparado con otros niños de su edad?",
                "ALE 9.3. ¿Se enoja mucho y tiene dificultad para calmarse, comparado con otros niños de su edad?",
                "Pregunte a la madre, padre o cuidador principal: diría que el niño ¿es tranquilo, enojón o de carácter difícil? Comparado con otros niños de su edad: ¿se enoja más?, ¿se enoja más fácilmente por cosas pequeñas?, ¿se calma rápido o demora en tranquilizarse? Defina la frecuencia de la conducta."
            )
        ),
        alaItems = listOf(
            alaItem(
                9, 1, false,
                "1. ¿Se comunica únicamente con balbuceo, gritos o ruidos aislados?",
                "ALA 9.1. ¿Se comunica únicamente con balbuceo, gritos o ruidos aislados?",
                "Pregunte a la madre, padre o cuidador principal: ¿cómo se comunica el niño, se limita a gritar, balbucear o ya logra decir alguna palabra?"
            ),
            alaItem(
                9, 2, true,
                "**2. Para desplazarse, ¿solamente gatea o se arrastra? (Observe)",
                "**ALA 9.2. Para desplazarse, ¿solamente gatea o se arrastra?",
                "Durante la evaluación observe la manera en que el niño se desplaza."
            ),
            alaItem(
                9, 3, false,
                "3. ¿Muestra conductas repetitivas o estereotipadas, por ejemplo: balancearse sin parar en su silla o golpearse la cabeza con las manos?",
                "ALA 9.3. ¿Muestra conductas repetitivas o estereotipadas, por ejemplo: balancearse sin parar en su silla o golpearse la cabeza con las manos?",
                "Pregunte a la madre, padre o cuidador principal: si tiene algún movimiento repetitivo o estereotipado como mover los dedos delante de la cara, balancearse sin parar, girar como un trompo, golpearse la cabeza con las manos o contra un objeto, aletear con los brazos, caminar de puntas. Pregúntele si esto aumenta con emociones fuertes. Defina la intensidad y frecuencia de la conducta."
            ),
            alaItem(
                9, 4, false,
                "4. ¿La madre, padre o cuidador primario reporta que el niño ha experimentado una pérdida importante y constante de las habilidades que en algún momento tuvo?",
                "ALA 9.4. ¿La madre, padre o cuidador principal reporta que el niño ha experimentado una pérdida importante y constante de las habilidades que en algún momento tuvo?",
                "Pregunte a la madre, padre o cuidador principal si el niño ha perdido alguna habilidad o aprendizaje que ya había logrado. Distinga pérdida de un hito que ha desaparecido para evolucionar y convertirse en otro de un nivel mayor de desarrollo. La pérdida debe ser: mayor a tres meses, importante, constante y no transitoria."
            )
        )
    )

    private fun buildGroup10(): EdiGroupSpec = EdiGroupSpec(
        groupNumber = 10,
        code = "G10",
        officialTitle = "Grupo 10 (de 19 meses a 24 meses 29 días)",
        shortIntervalLabel = "19m 0d - 24m 29d",
        minMonthsInclusive = 19,
        maxMonthsInclusive = 24,
        formatPages = 36..37,
        consultationPages = 87..90,
        generalHeaderMaterialsText = "MG 10.3: Pelota. MF 10.1: Frasco de plástico con tapa rosca. MF 10.2: Juguete envuelto en papel celofán.",
        itemsPerActiveArea = 3,
        fallbackPreviousGroupNumber = 9,
        fallbackItemsCountPerArea = 3,
        frbAndAleModifyGlobalTrafficLight = false,
        areaItems = mapOf(
            EdiDevArea.MG to listOf(
                devItem(
                    10, EdiDevArea.MG, 1, false,
                    "1. ¿Puede su niño subirse solo a los muebles?",
                    "MG 10.1. ¿Puede su niño subirse solo a los muebles?",
                    "Pregunte a la madre, padre o cuidador principal si el niño es capaz de trepar solo a los muebles bajos como sillones, sillas, camas y cuánta ayuda necesita cuando lo hace."
                ),
                devItem(
                    10, EdiDevArea.MG, 2, true,
                    "**2. ¿Corre su niño sin caerse? (Observe)",
                    "**MG 10.2. ¿Corre su niño sin caerse?",
                    "En un lugar con espacio suficiente y de manera divertida, invite al niño a jugar a las carreras. Diga: -Ven (decir el nombre del niño) ¡Vamos a correr! ¡Mira cómo lo hago! Puede invitar al cuidador a jugar con ustedes para animarlo. Observe si lo hace."
                ),
                devItem(
                    10, EdiDevArea.MG, 3, true,
                    "**3. ¿Patea la pelota sin perder el equilibrio? (Observe)",
                    "**MG 10.3. ¿Patea la pelota sin perder el equilibrio?",
                    "Ubíquese junto al niño en un lugar con espacio suficiente, lejos de objetos que puedan romperse. Colóquese frente al niño, guardando una distancia mayor de un metro. Luego diga: \"¡Vamos a jugar a la pelota. Muéstrame como pateas la pelota!\" Si el niño no lo hace muéstrele cómo hacerlo. Observe si pierde el equilibrio al hacerlo.",
                    headerMats = listOf("Pelota"),
                    protocolMats = listOf("Pelota")
                )
            ),
            EdiDevArea.MF to listOf(
                devItem(
                    10, EdiDevArea.MF, 1, true,
                    "**1. ¿Intenta abrir un frasco de plástico girando la tapa rosca con la mano? (Observe)",
                    "**MF 10.1. ¿Intenta abrir un frasco de plástico girando la tapa rosca con la mano?",
                    "Siente al niño con los brazos encima de la mesa. Colóquese frente a él y muéstrele un frasco de tamaño mediano con tapa rosca. Diga: \"Mira, voy a abrir este frasco\". Asegúrese de que lo está observando mientras lo hace. Vuelva a cerrar el frasco poniendo cuidado de que no quede muy apretada la tapa. Luego extiéndale el frasco y diga: \"¡Ahora inténtalo tú!\" Observe si lo hace.",
                    headerMats = listOf("Frasco de plástico con tapa rosca"),
                    protocolMats = listOf("Frasco de plástico con tapa rosca")
                ),
                devItem(
                    10, EdiDevArea.MF, 2, true,
                    "**2. ¿Puede desenvolver un juguete que esté envuelto en papel celofán? (Observe)",
                    "**MF 10.2. ¿Puede desenvolver un juguete u objeto pequeño que esté envuelto en papel celofán?",
                    "Muéstrale un juguete u objeto pequeño envuelto en un papel celofán. Diga: \"Mira, aquí tengo un juguete, lo voy a abrir\". Desenvuelva el juguete quitándole el papel. Asegúrese de que lo está mirando mientras lo hace. Ahora ofrézcale un nuevo juguete al niño y diga: \"¿quieres abrirlo tú? Aquí lo tienes\". Observe si lo desenvuelve.",
                    headerMats = listOf("Juguete envuelto en papel celofán"),
                    protocolMats = listOf("Juguete u objeto pequeño envuelto en papel celofán")
                ),
                devItem(
                    10, EdiDevArea.MF, 3, false,
                    "3. ¿Puede comer ayudándose de una cuchara sin tirar la comida?",
                    "MF 10.3. ¿Puede comer ayudándose de una cuchara sin tirar la comida?",
                    "Pregunte a la madre, padre o cuidador principal: si el niño puede comer ayudándose de una cuchara o trozo de pan (o la palabra que corresponda en el contexto local). Pídale al cuidador que le dé ejemplos. Defina la frecuencia de la conducta."
                )
            ),
            EdiDevArea.LE to listOf(
                devItem(
                    10, EdiDevArea.LE, 1, false,
                    "1. ¿Dice frases de dos palabras?",
                    "LE 10.1. ¿Dice frases de dos palabras?",
                    "Pregunte a la madre, padre o cuidador principal si junta dos palabras con sentido y de manera espontánea, formando una frase. Haga una lista. Si el cuidador tiene problemas para recordar, enumere usted algunas frases frecuentes como: \"mamá teta\", \"papá leche\", \"perro malo\", \"más agua\", \"leche rica\", \"dame papa\"."
                ),
                devItem(
                    10, EdiDevArea.LE, 2, false,
                    "2. ¿Sabe decir ocho o más palabras además de \"mamá\" y \"papá\"?",
                    "LE 10.2. ¿Sabe decir ocho o más palabras además de \"mamá\" y \"papá\"?",
                    "Pregunte a la madre, padre o cuidador principal qué palabras con sentido y de manera espontánea dice. Haga una lista. Si la mamá o el cuidador tiene problemas para recordar, enumere usted las más frecuentes preguntando qué palabra utiliza para nombrar estas u otras palabras comunes en el contexto: Pan, Silla, Mesa, Carro, Leche, Pelota, Perro, Teta, Taza, Agua, Upa, Gato, Galleta, Muñeco, Jugo, Niño, Zapato, Papa."
                ),
                devItem(
                    10, EdiDevArea.LE, 3, true,
                    "**3. ¿Obedece órdenes sencillas? (Observe)",
                    "**LE 10.3. ¿Obedece órdenes sencillas?",
                    "En este ítem se busca conocer si el niño comprende el lenguaje a través de órdenes sencillas de un sólo paso y sin el uso de gestos. Puede apoyarse con objetos que haya nombrado previamente o que le interesen. Puede darle las siguientes órdenes: Dame la pelota, Pon el lápiz sobre la mesa, Levanta los brazos, Trae el muñeco, Dale el vaso a mamá, Abre la puerta."
                )
            ),
            EdiDevArea.SO to listOf(
                devItem(
                    10, EdiDevArea.SO, 1, false,
                    "1. ¿Trata de hacer las cosas solo y se molesta cuando no lo dejan?",
                    "SO 10.1. ¿Trata de hacer las cosas solo y se molesta cuando no lo dejan?",
                    "Pregunte a la madre, padre o cuidador primario si hace pequeños intentos de ser independiente y de hacer las cosas solo, o si por el contrario siempre está pidiendo ayuda para todo. Puede apoyarse en esta lista de actividades: vestido y desvestido / lavado de manos / sonarse la nariz / comer su porción de comida / servirse líquido de una botella / sacar los juguetes."
                ),
                devItem(
                    10, EdiDevArea.SO, 2, false,
                    "2. ¿Avisa cuando ha ensuciado el pañal?",
                    "SO 10.2. ¿Avisa cuando ha ensuciado el pañal?",
                    "Pregunte si el niño avisa cuando tiene sucio el pañal o si está en una etapa más evolucionada del control de esfínteres. ¿Avisa, ya sea con palabras o con gestos si ha ensuciado el pañal?"
                ),
                devItem(
                    10, EdiDevArea.SO, 3, false,
                    "3. ¿Le pide ayuda cuando quiere algo que no alcanza?",
                    "SO 10.3. ¿Le pide ayuda cuando quiere algo que no alcanza?",
                    "Pregunte a la madre, padre o cuidador principal: ¿cómo se comporta cuando quiere algo que no alcanza?, ¿señala con la punta del dedo?, ¿después de señalar, mira al cuidador para confirmar que sí lo está atendiendo y nuevamente mira y señala el objeto deseado? (atención conjunta), ¿hace contacto visual para pedir el objeto y luego utiliza palabras o gestos para expresar lo que quiere?"
                )
            ),
            EdiDevArea.CO to emptyList()
        ),
        aleItems = listOf(
            aleItem(
                10, 1, true,
                "**1. ¿Tiene dificultad para seguir instrucciones sencillas, como \"dame tu zapato\"? (Observe)",
                "**ALE 10.1. ¿Tiene dificultad para seguir instrucciones sencillas, como \"dame tu zapato\"?",
                "Se busca identificar si el niño tiene dificultades perceptivas o de comprensión del lenguaje. Colóquese frente al niño y dígale por su nombre: ¿me puedes dar tu zapato, por favor? Recuerde, no haga el gesto ni con la mano ni con la cabeza."
            ),
            aleItem(
                10, 2, false,
                "2. ¿Se le dificulta mucho relacionarse con personas que no le son familiares?",
                "ALE 10.2. ¿Se le dificulta mucho relacionarse con personas que no le son familiares?",
                "Pregunte a la madre, padre o cuidador principal: comparado con otros niños de su edad, ¿es más tímido?, ¿tiene problemas para relacionarse?, ¿por qué cree que tiene dificultades para relacionarse con las personas?, ¿por su temperamento tímido?, ¿por qué no entiende el lenguaje?, ¿por qué no habla bien y sabe que no le entienden? Defina la intensidad y frecuencia."
            ),
            aleItem(
                10, 3, false,
                "3. ¿Muestra indiferencia excesiva al entorno, parece no interesarle nada de lo que sucede a su alrededor?",
                "ALE 10.3. ¿Muestra indiferencia excesiva al entorno, parece no interesarle nada de lo que sucede a su alrededor?",
                "Pregunte a la madre, padre o cuidador principal: ¿participa en conversaciones familiares? o ¿parece como si estuviera en su propio mundo, sin enterarse de lo que pasa en casa?, ¿cuándo le hablas no responde? Defina la frecuencia de la conducta."
            )
        ),
        alaItems = listOf(
            alaItem(
                10, 1, true,
                "**1. Cuando camina, ¿presenta una forma inmadura de hacerlo, primero apoya los dedos y luego el talón o después de varios meses sólo apoya las puntas de los pies? (Observe)",
                "**ALA 10.1. Cuando camina, ¿primero apoya los dedos y luego el talón o después de varios meses solo apoya la punta de los pies?",
                "Coloque al niño en sus dos pies sobre el piso y pídale que camine de ida y vuelta en dirección del cuidador, preferentemente en un pasillo. Valore la marcha."
            ),
            alaItem(
                10, 2, false,
                "2. ¿La madre, padre o cuidador primario reporta que el niño ha experimentado una pérdida importante y constante de las habilidades que en algún momento tuvo?",
                "ALA 10.2. ¿La madre, padre o cuidador primario reporta que el niño ha experimentado una pérdida importante y constante de las habilidades que en algún momento tuvo?",
                "Pregunte a la madre, padre o cuidador principal si el niño ha perdido alguna habilidad o aprendizaje que ya había logrado. Distinga pérdida de un hito que ha desaparecido para evolucionar y convertirse en otro de un nivel mayor de desarrollo. La pérdida debe ser: mayor a tres meses, importante, constante y no transitoria."
            )
        )
    )

    private fun buildGroup11(): EdiGroupSpec = EdiGroupSpec(
        groupNumber = 11,
        code = "G11",
        officialTitle = "Grupo 11 (de 25 meses a 30 meses 29 días)",
        shortIntervalLabel = "25m 0d - 30m 29d",
        minMonthsInclusive = 25,
        maxMonthsInclusive = 30,
        formatPages = 38..39,
        consultationPages = 91..93,
        generalHeaderMaterialsText = "MF 11.1: Frasco con tapa rosca. MF 11.2: Pelota. MF 11.3: Seis cubos.",
        itemsPerActiveArea = 3,
        fallbackPreviousGroupNumber = 10,
        fallbackItemsCountPerArea = 3,
        frbAndAleModifyGlobalTrafficLight = false,
        areaItems = mapOf(
            EdiDevArea.MG to listOf(
                devItem(
                    11, EdiDevArea.MG, 1, true,
                    "**1. Cuando su niño salta, ¿levanta los dos pies del suelo a la vez? (Observe)",
                    "**MG 11.1. Cuando su niño salta, ¿levanta los dos pies del suelo a la vez?",
                    "Busque un lugar con espacio suficiente y diga: ¡ahora vamos a jugar a dar saltos!, ¡mira como lo hago! Ahora es tu turno. ¡Muéstrame cómo lo haces!, ¡da saltos con los dos pies, como yo lo hice! Observe si lo hace. Repetición del ítem: hasta 2 veces más."
                ),
                devItem(
                    11, EdiDevArea.MG, 2, false,
                    "2. ¿Puede subir o bajar al menos dos escalones con ayuda?",
                    "MG 11.2. ¿Puede subir o bajar al menos dos escalones con ayuda?",
                    "Pregunte a la madre, padre o cuidador principal: ¿puede subir o bajar al menos dos escalones?, ¿cuánta ayuda necesita para subir?, ¿cuánta ayuda necesita para bajar?"
                ),
                devItem(
                    11, EdiDevArea.MG, 3, true,
                    "**3. ¿Puede su niño pararse sobre un solo pie? (Observe)",
                    "**MG 11.3. ¿Puede su niño pararse sobre un solo pie?",
                    "Colóquese frente al niño y diga: \"¡ahora vamos a jugar a pararnos en un sólo pie!, ¡mira cómo lo hago! Ahora es tu turno. ¡Muéstrame cómo lo haces!, ¡párate en un sólo pie, como yo lo hice!\" Repetición del ítem: hasta 2 veces más de cada lado."
                )
            ),
            EdiDevArea.MF to listOf(
                devItem(
                    11, EdiDevArea.MF, 1, true,
                    "**1. ¿Abre un frasco de plástico solo? (Observe)",
                    "**MF 11.1. ¿Puede abrir un frasco con tapa rosca de plástico solo?",
                    "Dele un frasco de plástico con tapa rosca y dígale: \"aquí tienes un frasco. ¡Enséñame como lo abres!\" No le muestre cómo hacerlo; espere a que lo haga solo. Nota: Colocar dentro del frasco un objeto pequeño que produzca interés por sacarlo.",
                    headerMats = listOf("Frasco con tapa rosca"),
                    protocolMats = listOf("Frasco de plástico con tapa rosca y objeto pequeño dentro")
                ),
                devItem(
                    11, EdiDevArea.MF, 2, true,
                    "**2. ¿Lanza una pelota con la mano? (Observe)",
                    "**MF 11.2. ¿Lanza una pelota con la mano?",
                    "Entregue la pelota al niño y colóquese a dos metros de distancia. Diga: \"vamos a jugar con esta pelota. ¿Me la lanzas?\" Si no lo hace, muéstrele como. Repetición del ítem: hasta 2 veces más de cada lado.",
                    headerMats = listOf("Pelota"),
                    protocolMats = listOf("Pelota")
                ),
                devItem(
                    11, EdiDevArea.MF, 3, true,
                    "**3. ¿Hace hileras con 4 cubos? (Observe)",
                    "**MF 11.3. ¿Hace hileras con 4 cubos?",
                    "Colóquese frente al niño y muéstrele los cubos. Diga: \"Mira..... gusanito\" \"Mira, voy a construir un tren/gusanito.\" \"¡Ahora es tu turno, muéstrame como construyes un tren/gusanito!\" Repetición del ítem: hasta 2 veces más.",
                    headerMats = listOf("Seis cubos"),
                    protocolMats = listOf("Seis cubos")
                )
            ),
            EdiDevArea.LE to listOf(
                devItem(
                    11, EdiDevArea.LE, 1, true,
                    "**1. ¿Sabe decir su nombre? (Observe)",
                    "**LE 11.1. ¿Sabe decir su nombre?",
                    "Pregunta: \"¿Cómo te llamas?, ¡Dime tu nombre!\" Espere a que responda con su nombre. (Es suficiente si dice su nombre de pila)."
                ),
                devItem(
                    11, EdiDevArea.LE, 2, false,
                    "2. ¿Utiliza palabras como \"yo\", \"mío\", \"él\", \"ella\", \"tú\"?",
                    "LE 11.2. ¿Utiliza palabras como \"yo\", \"mío\", \"él\", \"ella\", \"tú\"?",
                    "Pregunte a la madre, padre o cuidador principal si utiliza de manera espontánea y apropiada pronombres personales como: \"yo\" (cuando se refiere a sí mismo), \"tú\" (cuando se dirige a otro), \"él/ella\" (cuando habla de una tercera persona) y el pronombre posesivo \"mío\" (cuando se refiere a algo que le pertenece). Defina la frecuencia e intensidad de la conducta en comparación con otros niños de su misma edad. Asegúrese que no se refiera a sí mismo en tercera persona (hablar de sí mismo como si fuera otra persona). Es suficiente con que utilice una de estas palabras correctamente."
                ),
                devItem(
                    11, EdiDevArea.LE, 3, false,
                    "3. ¿Usa oraciones de 3 palabras, por ejemplo: \"quiero mi pelota\", \"dame mi leche\"?",
                    "LE 11.3. ¿Usa oraciones de 3 palabras, por ejemplo: \"quiero mi pelota\", \"dame mi leche\"?",
                    "Pregunte a la madre, padre o cuidador principal si utiliza oraciones de 3 palabras (la oración debe incluir verbo). Haga una lista, por ejemplo: \"mamá quiero leche\", \"papá mira carro\", \"dame mi pelota\"."
                )
            ),
            EdiDevArea.SO to listOf(
                devItem(
                    11, EdiDevArea.SO, 1, false,
                    "1. ¿Le ayuda a guardar las cosas de la casa en su lugar?",
                    "SO 11.1. ¿Le ayuda a guardar las cosas de la casa en su lugar?",
                    "Pregunte a la madre, padre o cuidador principal si su niño se entusiasma y colabora guardando las cosas de la casa en su lugar. Este ítem busca evaluar su interés social y deseos de ser responsable y colaborar con la familia. Defina la frecuencia e intensidad de la conducta en comparación con otros niños de su misma edad."
                ),
                devItem(
                    11, EdiDevArea.SO, 2, false,
                    "2. ¿Juega a simular cosas que vió?",
                    "SO 11.2. ¿Juega a simular cosas que vió?",
                    "Pregunte a la madre, padre o cuidador principal: ¿a qué le gusta jugar?, ¿juega espontáneamente a imitar acciones de la vida en el hogar, el mercado o la comunidad? Este ítem busca investigar si tiene juego simbólico."
                ),
                devItem(
                    11, EdiDevArea.SO, 3, false,
                    "3. ¿Se lava solo las manos y la cara?",
                    "SO 11.3. ¿Se lava solo las manos y la cara?",
                    "Pregunte a la madre, padre o cuidador principal si su niño se lava la cara o las manos sin ayuda cuando se le pide. Si es capaz de abrir por sí solo la llave, coger el jabón, frotarse/jabonarse las manos, enjuagarlas con agua. Cerciórese que también puede lavarse la cara. Defina la frecuencia e intensidad de la conducta en comparación con otros niños de su misma edad."
                )
            ),
            EdiDevArea.CO to emptyList()
        ),
        aleItems = listOf(
            aleItem(
                11, 1, false,
                "1. ¿Se le dificulta mucho relacionarse con personas que no le son familiares?",
                "ALE 11.1. ¿Se le dificulta relacionarse con personas que no le son familiares?",
                "Pregunte a la madre, padre o cuidador principal: ¿tiene dificultad para relacionarse con otros niños de su edad?, ¿este problema se manifiesta con mayor frecuencia que en otros niños de su edad? Defina la frecuencia e intensidad de la conducta en comparación con otros niños de su misma edad."
            ),
            aleItem(
                11, 2, false,
                "2. ¿Se frustra mucho cuando se le dificulta hacer algo y deja de hacerlo o se pone a llorar?",
                "ALE 11.2. ¿Se frustra cuando se le dificulta hacer algo y deja de hacerlo o se pone a llorar?",
                "Pregunte a la madre, padre o cuidador principal: ¿se frustra el niño ante tareas difíciles y deja de hacerlas?, ¿ni siquiera intenta realizar tareas o es poco perseverante y las abandona rápidamente? Defina la frecuencia e intensidad de la conducta en comparación con otros niños de su misma edad."
            )
        ),
        alaItems = listOf(
            alaItem(
                11, 1, false,
                "1. ¿Muestra indiferencia excesiva al entorno?",
                "ALA 11.1. ¿Muestra indiferencia excesiva al entorno?",
                "Pregunte a la madre, padre o cuidador principal: ¿parece como si estuviera en su propio mundo, sin interesarse en nada de lo que pasa?, ¿parece no escuchar cuando se le habla?"
            ),
            alaItem(
                11, 2, false,
                "2. ¿La madre, padre o cuidador primario reporta que el niño ha experimentado una pérdida importante y constante de las habilidades que en algún momento tuvo?",
                "ALA 11.2. ¿La madre, padre o cuidador primario reporta que el niño ha experimentado una pérdida importante y constante de las habilidades que en algún momento tuvo?",
                "Pregunte a la madre, padre o cuidador principal si el niño ha perdido alguna habilidad o aprendizaje que ya había logrado, que ha desaparecido para evolucionar y convertirse en otro de un nivel mayor de desarrollo. La pérdida debe ser: mayor a tres meses, importante, constante y no transitoria."
            )
        )
    )

    private fun buildGroup12(): EdiGroupSpec = EdiGroupSpec(
        groupNumber = 12,
        code = "G12",
        officialTitle = "Grupo 12 (de 31 meses a 36 meses 29 días)",
        shortIntervalLabel = "31m 0d - 36m 29d",
        minMonthsInclusive = 31,
        maxMonthsInclusive = 36,
        formatPages = 40..41,
        consultationPages = 95..98,
        generalHeaderMaterialsText = "ALE 12.1: Cinco objetos pequeños y un frasco de plástico con tapa rosca. MF 12.2: Bola de papel pequeña. MF 12.3: Papel y crayones o lápices de colores. LE 12.3: Láminas B, C y D.",
        itemsPerActiveArea = 3,
        fallbackPreviousGroupNumber = 11,
        fallbackItemsCountPerArea = 3,
        frbAndAleModifyGlobalTrafficLight = false,
        areaItems = mapOf(
            EdiDevArea.MG to listOf(
                devItem(
                    12, EdiDevArea.MG, 1, true,
                    "**1. ¿Puede su niño agacharse fácilmente hacia adelante sin caerse? (Observe)",
                    "**MG 12.1. ¿Puede su niño agacharse fácilmente hacia adelante sin caerse?",
                    "En un lugar con espacio suficiente, diga: \"¡ahora vamos a hacer ejercicio!, ¡mira cómo puedo agacharme y tocar el piso!, ¡ahora tú!, ¡toca el suelo con las dos manos!\". Observe si lo hace. Repetición del ítem: hasta 2 veces más."
                ),
                devItem(
                    12, EdiDevArea.MG, 2, true,
                    "**2. ¿Puede su niño caminar sobre la punta de sus pies? (Observe)",
                    "**MG 12.2. ¿Puede su niño caminar sobre la punta de sus pies?",
                    "Retírele los zapatos y en un lugar con espacio suficiente diga: \"¡ahora vamos a jugar a caminar de puntas!\". Camine sobre la punta de sus pies y diga: ¡ahora te toca a ti!, ¡muéstrame cómo lo haces! Observe si lo hace. Repetición del ítem: hasta 2 veces más."
                ),
                devItem(
                    12, EdiDevArea.MG, 3, true,
                    "**3. ¿Puede caminar hacia atrás? (Observe)",
                    "**MG 12.3. ¿Puede caminar hacia atrás?",
                    "Busque un lugar con espacio suficiente y diga: ¡ahora vamos a jugar a caminar hacia atrás!, ¡mira como lo hago!, ¡ahora te toca a ti!, ¡camina para atrás! Repetición del ítem: hasta 2 veces más."
                )
            ),
            EdiDevArea.MF to listOf(
                devItem(
                    12, EdiDevArea.MF, 1, false,
                    "1. ¿Puede abotonar un botón de una prenda de vestir?",
                    "MF 12.1. ¿Puede su niño abotonar un botón de una prenda de vestir?",
                    "Pregunte a la madre, padre o cuidador principal: ¿se logra abotonar botones de su chompa, pantalón o alguna otra prenda de vestir sin ayuda?"
                ),
                devItem(
                    12, EdiDevArea.MF, 2, true,
                    "**2. ¿Puede levantar una bola pequeña de papel del suelo tomándola con los dedos índice y pulgar? (Observe)",
                    "**MF 12.2. ¿Puede levantar una bola pequeña de papel del suelo tomándola con los dedos índice y pulgar?",
                    "Ponga una bola pequeña de papel en el suelo y diga: \"¡mira, voy a levantar esta bolita del suelo usando estos 2 dedos! Ahora te toca a ti. Levántala\". Observe si lo hace. Repetición del ítem: hasta 2 veces más.",
                    headerMats = listOf("Bola de papel pequeña"),
                    protocolMats = listOf("Bola de papel pequeña")
                ),
                devItem(
                    12, EdiDevArea.MF, 3, true,
                    "**3. ¿Imita trazos verticales y horizontales? (Observe)",
                    "**MF 12.3. ¿Imita trazos verticales y horizontales?",
                    "Estando sentado dígale: \"Mira voy a dibujar estas líneas de arriba hacia abajo. Ahora hazlo tú. Ahora voy a dibujar estas líneas de un lado a otro. Ahora tú\". Observe si lo hace. Repetición del ítem: hasta 2 veces más.",
                    headerMats = listOf("Papel y crayones o lápices de colores"),
                    protocolMats = listOf("Papel y crayones o lápices de colores")
                )
            ),
            EdiDevArea.LE to listOf(
                devItem(
                    12, EdiDevArea.LE, 1, true,
                    "**1. ¿Sabe decir su edad? (Observe)",
                    "**LE 12.1. ¿Sabe decir su edad?",
                    "Pregúntele al niño: \"¿cuántos años tienes?\" Observe si lo hace ya sea con los dedos o con palabras."
                ),
                devItem(
                    12, EdiDevArea.LE, 2, false,
                    "2. Cuando está con personas que no conoce, ¿éstas entienden la mayoría de las palabras que dice?",
                    "LE 12.2. Cuando está con personas que no conoce, ¿éstas entienden la mayoría de las palabras que dice?",
                    "Pregunte a la madre, padre o cuidador principal: las personas que no son de la familia, ¿comprenden lo qué dice su niño?, ¿cuánto cree usted que alcanzan a entender? Las personas que no son familiares deben comprender 75 por ciento o más o la mayor parte de lo que el niño(a) hable."
                ),
                devItem(
                    12, EdiDevArea.LE, 3, true,
                    "**3. Cuando habla, ¿utiliza el plural de las palabras: \"perros\", \"juguetes\", \"niños\", etc.? (Observe)",
                    "**LE 12.3. Cuando habla, ¿utiliza el plural de las palabras: \"perros\", \"juguetes\", \"niños\", etcétera?",
                    "Muéstrele al niño la lámina B, señale el dibujo de un solo perro y diga: ¿qué es esto? Luego señale el de los dos perros y diga: ¿Y estos? Repita mismo procedimiento con lámina C y D. (Manual para la Formación de Facilitadores en la Prueba Evaluación del Desarrollo Infantil, páginas 110-112). Debe observar que utilice el plural(s).",
                    headerMats = listOf("Láminas B, C y D"),
                    protocolMats = listOf("Láminas B, C y D (singular/plural: perro/perros, pelota/pelotas, niño/niños)"),
                    plates = setOf(EdiOfficialPlate.LAMINA_B, EdiOfficialPlate.LAMINA_C, EdiOfficialPlate.LAMINA_D)
                )
            ),
            EdiDevArea.SO to listOf(
                devItem(
                    12, EdiDevArea.SO, 1, false,
                    "1. ¿Juega con otros niños?",
                    "SO 12.1. ¿Juega con otros niños?",
                    "Pregunte a la madre, padre o cuidador principal: ¿le interesa jugar con otros?, ¿prefiere jugar solo?, ¿es capaz de jugar de manera cooperativa y amistosa con otra persona? Este ítem busca conocer el interés social del niño. Defina la frecuencia e intensidad de la conducta en comparación con otros niños de su misma edad."
                ),
                devItem(
                    12, EdiDevArea.SO, 2, false,
                    "2. ¿Entiende la diferencia entre \"mío\" y \"tuyo\"?",
                    "SO 12.2. ¿Entiende la diferencia entre \"mío\" y \"tuyo\"?",
                    "Pregunte a la madre, padre o cuidador principal: ¿sabe cuál es la diferencia entre \"mío\" y \"tuyo\"? ¿Entiende que algunas cosas pertenecen a otras personas y debe respetarlas? Este ítem busca evaluar si el niño reconoce la diferencia entre algo propio y algo que pertenece a otro y si se comporta en consecuencia."
                ),
                devItem(
                    12, EdiDevArea.SO, 3, false,
                    "3. ¿Se separa fácilmente de sus padres?",
                    "SO 12.3. ¿Se separa fácilmente de sus padres?",
                    "Pregunte a la madre, padre o cuidador principal si se separa fácilmente de sus padres cuando tienen que dejarlo en algún sitio con otros familiares o en la guardería. No debe presentar ansiedad marcada ni indiferencia completa ante la separación. Defina la frecuencia e intensidad de la conducta en comparación con otros niños de su misma edad."
                )
            ),
            EdiDevArea.CO to emptyList()
        ),
        aleItems = listOf(
            aleItem(
                12, 1, true,
                "**1. ¿Le cuesta trabajo manipular objetos pequeños como: bolas pequeñas de papel? (Observe)",
                "**ALE 12.1. ¿Le cuesta trabajo manipular objetos pequeños como: bolas pequeñas de papel?",
                "Colóquele enfrente un frasco de plástico y cinco bolas pequeñas de papel, solicítele que introduzca los objetos en el frasco. Si no lo realiza ponga el ejemplo. Esta señal está presente si le cuesta trabajo manipular los objetos. Nota: Se recomienda prestar atención en el caso de usar semillas pues podrían tener insectos.",
                headerMats = listOf("Cinco objetos pequeños y un frasco de plástico con tapa rosca"),
                protocolMats = listOf("Cinco bolas pequeñas de papel y un frasco de plástico")
            ),
            aleItem(
                12, 2, true,
                "**2. ¿Le cuesta trabajo entender órdenes sencillas como: \"ve con tu mamá\"? (Observe)",
                "**ALE 12.2. ¿Le cuesta trabajo entender órdenes sencillas como: \"ve con tu mamá\"?",
                "Colóquese frente al niño y dígale: \"ve con tu mamá (papá, abuelita, tía)\". La señal está presente si muestra dificultades para entender instrucciones."
            ),
            // CONTROL 3: ALE 12.3 requiere observación directa y entrevista complementaria.
            aleItem(
                12, 3, true,
                "**3. Cuando alguien le habla, ¿evita el contacto visual? (Observe)",
                "ALE 12.3. Cuando alguien le habla, ¿evita el contacto visual?",
                "Protocolo combinado (Observación directa + Entrevista complementaria): Observe el contacto visual durante la evaluación y pregunte a la madre, padre o cuidador principal: ¿cuándo usted le habla a su niño evita mirarla a los ojos?, ¿cuándo usted le habla al niño pareciera como si no la escuchara? Defina la frecuencia e intensidad de la conducta en comparación con otros de su misma edad. Para este ítem debe tomarse en cuenta el aspecto cultural para ser considerado normal o anormal.",
                modality = EdiClinicalModality.DUAL_OBSERVATION_AND_INTERVIEW_ALE_12_3
            ),
            aleItem(
                12, 4, false,
                "4. ¿Le cuesta mucho trabajo separarse de su mamá o de la persona que lo cuida y cuando se aleja de él se pone a llorar?",
                "ALE 12.4. ¿Le cuesta mucho trabajo separarse de su mamá o de la persona que lo cuida y cuando se alejan de él se pone a llorar?",
                "Pregunte a la madre, padre o cuidador principal: ¿tiene mayor dificultad para separarse de usted que otros niños de su edad?, ¿cuesta mucho dejarlo en casa y salir debido a que inmediatamente se pone a llorar y reclama su presencia? Defina la frecuencia e intensidad de la conducta en comparación con otros niños de su misma edad."
            ),
            aleItem(
                12, 5, false,
                "5. ¿Usa sus juguetes de forma inusual, es decir, en vez de rodar sus carritos o jugar con sus peluches, sólo los ordena por tamaño, color o forma?",
                "ALE 12.5. ¿Usa sus juguetes de forma inusual, es decir, en vez de rodar los carros o jugar con sus juguetes, solo los ordena por tamaño, color o forma?",
                "Esta señal de alerta busca investigar si utiliza los juguetes con un juego funcional o simbólico y no para un uso repetitivo o estereotipado, como ordenar o poner en fila. La señal está presente si utiliza los juguetes exclusivamente para ordenarlos o ponerlos en fila."
            )
        ),
        alaItems = listOf(
            alaItem(
                12, 1, false,
                "1. ¿Babea constantemente o habla de manera poco entendible?",
                "ALA 12.1. ¿Babea constantemente o habla de manera poco entendible?",
                "Pregunte a la madre, padre o cuidador principal: ¿su niño babea continuamente?, ¿tiene problemas para hablar correctamente? Defina la frecuencia e intensidad de la conducta en comparación con otros niños de su misma edad."
            ),
            alaItem(
                12, 2, false,
                "2. ¿Se comunica con palabras aisladas es decir, no forma oraciones como \"mamá quiero leche\", y sólo dice \"leche\"?",
                "ALA 12.2. ¿Se comunica con palabras aisladas es decir, no forma oraciones como: \"mamá quiero leche\", y sólo dice \"leche\"?",
                "Pregunte a la madre, padre o cuidador principal si el niño forma oraciones complejas que incluyen la acción (verbos) o si utiliza únicamente palabras aisladas. En esta señal se busca evaluar si existe un retraso severo en el desarrollo del lenguaje. Por ejemplo: \"leche\", \"carro\", \"pelota\"."
            ),
            alaItem(
                12, 3, false,
                "3. ¿Se cae frecuentemente y le cuesta trabajo subir o bajar escaleras?",
                "ALA 12.3. ¿Se cae frecuentemente o le cuesta trabajo subir y bajar escaleras?",
                "Pregunte a la madre, padre o cuidador principal si el niño se cae frecuentemente al caminar, correr o si le cuesta trabajo subir y bajar escaleras. Defina la frecuencia e intensidad de la conducta en comparación con otros niños de su misma edad."
            ),
            alaItem(
                12, 4, false,
                "4. ¿La madre, padre o cuidador primario reporta que el niño ha experimentado una pérdida importante y constante de las habilidades que en algún momento tuvo?",
                "ALA 12.4. ¿La madre, padre o cuidador reporta que el niño ha experimentado una pérdida importante y constante de las habilidades que en algún momento tuvo?",
                "Pregunte a la madre, padre o cuidador principal si el niño ha perdido alguna habilidad o aprendizaje que ya había logrado. Distinga pérdida de un hito que ha desaparecido para evolucionar y convertirse en otro de un nivel mayor de desarrollo. La pérdida debe ser: mayor a tres meses, importante, constante y no transitoria."
            )
        )
    )

    private fun buildGroup13(): EdiGroupSpec = EdiGroupSpec(
        groupNumber = 13,
        code = "G13",
        officialTitle = "Grupo 13 (de 37 meses a 48 meses 29 días)",
        shortIntervalLabel = "37m 0d - 48m 29d",
        minMonthsInclusive = 37,
        maxMonthsInclusive = 48,
        formatPages = 42..43,
        consultationPages = 99..102,
        generalHeaderMaterialsText = "ALE 13.1, MF 13.2, MF 13.3, ALA 13.1: Papel y crayolas o lápices de colores. MG 13.2: Pelota. MF 13.1: Pasador y cuatro cuentas para ensartar. CO 13.1: Discos de 6 cm de diámetro de diez colores diferentes.",
        itemsPerActiveArea = 3,
        fallbackPreviousGroupNumber = 12,
        fallbackItemsCountPerArea = 3,
        usesSpecialCoRFallback = true,
        frbAndAleModifyGlobalTrafficLight = false,
        areaItems = mapOf(
            EdiDevArea.MG to listOf(
                devItem(
                    13, EdiDevArea.MG, 1, true,
                    "**1. ¿Salta su niño en un solo pie, en el mismo lugar, por lo menos 3 veces sin sujetarse? (Observe)",
                    "**MG 13.1. ¿Salta su niño en un solo pie, en el mismo lugar, por lo menos 3 veces sin sujetarse?",
                    "En un lugar con espacio suficiente, diga: \"¡Ahora vamos a jugar a saltar en un pie! ¡Mira cómo lo hago! ¡Ahora tú! ¡Salta en un pie como yo lo hice!!\" Debe saltar en un pie por lo menos 3 veces. Observe si lo hace. Repetición del ítem: hasta 2 veces más, de cada lado."
                ),
                devItem(
                    13, EdiDevArea.MG, 2, true,
                    "**2. Cuando le avientan a su niño una pelota grande, ¿puede atraparla? (Observe)",
                    "**MG 13.2. Cuando le lanzan a su niño una pelota grande, ¿puede cogerla/atraparla?",
                    "En un lugar con espacio suficiente, colóquese frente al niño, a una distancia mayor a un metro y diga: \"¡Vamos a jugar a la pelota!\" Láncele la pelota diciendo: \"¡Atrapa la pelota!\" Observe si lo hace. Repetición del ítem: Hasta 2 veces más.",
                    headerMats = listOf("Pelota"),
                    protocolMats = listOf("Pelota grande")
                ),
                devItem(
                    13, EdiDevArea.MG, 3, true,
                    "**3. ¿Sube y baja las escaleras sin apoyarse de la pared o de la baranda? (Observe)",
                    "**MG 13.3. ¿Sube y baja las escaleras sin apoyarse de la pared o de la baranda?",
                    "Pregunte a la madre, padre o cuidador principal: ¿puede su niño subir o bajar las escaleras por sí solo, sin que lo lleven de la mano y sin apoyarse de la pared o de la baranda? Corrobore invitándolo a subir y después a bajar hasta 4 escalones de manera lenta. Observe cuánta ayuda necesita."
                )
            ),
            EdiDevArea.MF to listOf(
                devItem(
                    13, EdiDevArea.MF, 1, true,
                    "**1. ¿Puede meter un pasador o cordón por los agujeros de una cuenta o de un zapato? (Observe)",
                    "*MF 13.1. ¿Puede meter un pasador o cordón por los agujeros de una bolita o de un zapato?",
                    "Colóquese frente al niño y diga: \"Mira, aquí tengo unas bolitas y un cordón. Voy a meter el cordón en estas bolitas\". Retire las bolitas y déselas junto con el pasador y diga: \"Ahora te toca a ti. Mete las bolitas una por una\". Repetición del ítem: hasta 2 veces más.",
                    headerMats = listOf("Pasador y cuatro cuentas para ensartar"),
                    protocolMats = listOf("Pasador o cordón y cuatro cuentas para ensartar")
                ),
                devItem(
                    13, EdiDevArea.MF, 2, true,
                    "**2. ¿Puede dibujar una persona con 2 o más partes del cuerpo: ojos, nariz, boca, pelo, tronco, brazos, piernas, etc.? (Observe)",
                    "**MF 13.2. ¿Puede dibujar una persona con 2 o más partes del cuerpo: \"ojos\", \"nariz\", \"boca\", \"pelo\", \"tronco\", \"brazos\", \"piernas\", etcétera?",
                    "Colóquese frente al niño y diga: \"¡Ahora vamos a dibujar! ¿Puedes hacer un dibujo para mí? Dibuja un niño o una niña\". Observe si lo hace. Repetición del ítem: hasta 2 veces más.",
                    headerMats = listOf("Papel y crayolas o lápices de colores"),
                    protocolMats = listOf("Papel y crayolas o lápices de colores")
                ),
                devItem(
                    13, EdiDevArea.MF, 3, true,
                    "**3. ¿Puede dibujar un círculo o una cruz? (Observe)",
                    "**MF 13.3. ¿Puede dibujar un círculo o una cruz?",
                    "Colóquese frente al niño y diga: \"¡Ahora vamos a dibujar!, ¿puedes hacer un dibujo para mí? Dibuja un círculo\". Luego diga: \"¿Puedes hacer una cruz para mí?\" Si no lo hace, muéstrele cómo. Repetición del ítem: hasta 2 veces más cada una de las figuras.",
                    headerMats = listOf("Papel y crayolas o lápices de colores"),
                    protocolMats = listOf("Papel y crayolas o lápices de colores")
                )
            ),
            EdiDevArea.LE to listOf(
                devItem(
                    13, EdiDevArea.LE, 1, false,
                    "1. ¿Le dice lo que quiere con palabras?",
                    "LE 13.1. ¿Le dice lo que quiere con palabras?",
                    "Pregunte a la madre, padre o cuidador principal: ¿Conversa su niño con usted? ¿Cuando su niño quiere algo, lo dice con palabras o sólo con gestos o haciendo algún sonido repetitivo como \"uhh-uhh\"?"
                ),
                devItem(
                    13, EdiDevArea.LE, 2, true,
                    "**2. ¿Puede contarle algo de lo que hizo ayer? (Observe)",
                    "**LE 13.2. ¿Puede contarle algo de lo que hizo ayer?",
                    "Pregunte al niño: ¿qué hiciste ayer?, ¿qué comiste ayer?, ¿a dónde fuiste ayer? Corrobore la respuesta con la madre, padre o el cuidador primario."
                ),
                // CONTROL 3: LE 13.3 emplea entrevista como modalidad principal (con observación complementaria).
                devItem(
                    13, EdiDevArea.LE, 3, false,
                    "3. Frecuentemente pregunta \"¿por qué?\" (Observe)",
                    "LE 13.3. Frecuentemente pregunta \"¿por qué?\"",
                    "Modalidad principal Entrevista (con observación complementaria): Pregunte a la madre, padre o cuidador principal si cuando el niño tiene curiosidad acerca de algo, hace preguntas que comienzan con un \"¿por qué...?\"",
                    modality = EdiClinicalModality.INTERVIEW_PRIMARY_WITH_OBSERVATION_COMPLEMENT
                )
            ),
            EdiDevArea.SO to listOf(
                devItem(
                    13, EdiDevArea.SO, 1, false,
                    "1. ¿Puede vestirse y desvestirse solo?",
                    "SO 13.1. ¿Puede vestirse y desvestirse solo?",
                    "Pregunte a la madre, padre o cuidador principal si el niño se viste o desviste solo. Para aprobar este ítem debe ser independiente en casi la totalidad de los pasos de esta conducta, que implica quitarse pantalones, camisa, playera, calcetines y zapatos. Defina la frecuencia e intensidad de la conducta en comparación con otros niños de su misma edad."
                ),
                devItem(
                    13, EdiDevArea.SO, 2, false,
                    "2. ¿Puede ir al baño solo?",
                    "SO 13.2. ¿Puede ir al baño solo?",
                    "Pregunte a la madre, padre o cuidador principal si el niño es capaz de ir al baño solo, sin requerir ayuda del adulto. Para aprobar este ítem el niño debe ser capaz de bajarse la ropa, hacer sus necesidades, limpiarse y vestirse nuevamente. Defina la frecuencia e intensidad de la conducta en comparación con otros niños de su misma edad."
                ),
                devItem(
                    13, EdiDevArea.SO, 3, false,
                    "3. ¿Juega con otros niños al papá, a la mamá, al doctor, policías y ladrones o a algún otro personaje?",
                    "SO 13.3. ¿Juega con otros niños al papá, a la mamá, al doctor, al vendedor de tienda o el mercado u otros del contexto?",
                    "Pregunte a la madre, padre o cuidador principal si el niño juega asumiendo roles de adultos tales como \"el doctor\", \"la maestra\", \"el policía\", etc. Defina la frecuencia e intensidad de la conducta en comparación con otros niños de su misma edad."
                )
            ),
            EdiDevArea.CO to listOf(
                devItem(
                    13, EdiDevArea.CO, 1, true,
                    "**1. ¿Conoce el nombre de cuatro colores? (Observe)",
                    "**CO 13.1. ¿Conoce los nombres de cuatro colores?",
                    "Colóquese frente al niño y diga: \"Ahora vamos a jugar con estos discos, escoge tus favoritos\". Ya elegidos 4 pregúntele: \"¿Qué color es éste?\" Independientemente de la respuesta continúe hasta que haya respondido al menos 4 nombres de colores. Nota: Es correcto aunque no coincida el nombre con el color del disco señalado. Observe si lo hace. Repetición del ítem: hasta 2 veces más.",
                    headerMats = listOf("Discos de 6 cm de diámetro de diez colores diferentes"),
                    protocolMats = listOf("Discos de 6 cm de diámetro de diez colores diferentes")
                ),
                devItem(
                    13, EdiDevArea.CO, 2, false,
                    "2. ¿Puede decir los nombres de dos o más números?",
                    "CO 13.2. ¿Puede decir los nombres de dos o más números?",
                    "Pregunte a la madre, padre o cuidador principal: ¿su niño dice algún número?, ¿cuál dice? Defina la frecuencia e intensidad de la conducta en comparación con otros niños de su edad."
                ),
                devItem(
                    13, EdiDevArea.CO, 3, false,
                    "3. ¿Puede contarle una parte del cuento que le hayan contado o leído previamente?",
                    "CO 13.3. ¿Puede contarle una parte del cuento que le haya contado o leído previamente?",
                    "Pregunte a la madre, padre o cuidador principal si su niño es capaz de relatar un cuento. En este ítem interesa conocer si es capaz de recordar una historia en su mayor parte y si puede narrarla con coherencia, organizando su estructura en principio, desarrollo y final."
                )
            )
        ),
        aleItems = listOf(
            aleItem(
                13, 1, true,
                "**1. ¿Le cuesta trabajo coger un lápiz o una crayola con los dedos índice, pulgar y medio? (Observe)",
                "**ALE 13.1. ¿Le cuesta trabajo tomar un lápiz o crayola con los dedos índice, pulgar y medio?",
                "Ofrézcale al niño un lápiz o un crayola y una hoja de papel y dígale: \"¡Haz un dibujo para mí!, ¡dibuja lo que quieras!\" La señal está presente si tiene dificultades para tomar el lápiz o crayola con dedos índice, pulgar y medio.",
                headerMats = listOf("Papel y crayolas o lápices de colores"),
                protocolMats = listOf("Papel y lápiz o crayola")
            ),
            aleItem(
                13, 2, false,
                "2. ¿Es muy apegado a los padres y llora cuando se alejan, y es más notorio que en los otros niños de su edad?",
                "ALE 13.2. ¿Es muy apegado a los padres y llora cuando se alejan, y es más notorio que en los otros niños de su edad?",
                "Pregunte a la madre, padre o cuidador principal: ¿diría que su niño es demasiado apegado a sus padres?, ¿ha llegado al extremo de tener que quedarse en casa o llevarlo cuando no lo planeaba, para que así deje de llorar? Defina la frecuencia e intensidad de la conducta en comparación con otros niños de su misma edad."
            ),
            aleItem(
                13, 3, false,
                "3. Cuando está con otros niños, ¿los ignora y prefiere jugar solo?",
                "ALE 13.3. Cuando está con otros niños, ¿los ignora y prefiere jugar solo?",
                "Pregunte a la madre, padre o cuidador principal si tiene la oportunidad de jugar con otros niños conocidos, como hermanos y primos, ¿prefiere jugar solo?, ¿su juego es repetitivo y estereotipado? Solicite ejemplos para determinar si la señal de alerta está presente."
            )
        ),
        alaItems = listOf(
            alaItem(
                13, 1, true,
                "**1. ¿Se le dificulta copiar un círculo en una hoja de papel? (Observe)",
                "**ALA 13.1. ¿Se le dificulta copiar un círculo en una hoja de papel?",
                "Colóquese frente al niño y diga: \"¡Ahora vamos a dibujar! ¡Mira voy a dibujar un círculo!, ¿lo ves? ¡Ahora es tu turno! ¿Puedes dibujar un círculo como éste?\" Observe si lo hace.",
                headerMats = listOf("Papel y crayolas o lápices de colores"),
                protocolMats = listOf("Papel y crayola o lápiz")
            ),
            alaItem(
                13, 2, false,
                "2. ¿Solamente dice frases de 2 palabras?",
                "ALA 13.2. ¿Solamente dice frases de 2 palabras?",
                "Pregunte a la madre, padre o cuidador principal: ¿cómo es el lenguaje de su niño?, ¿habla con palabras sueltas o solamente puede decir frases de dos palabras, por ejemplo: \"mamá leche\", \"dame más\", etc.?"
            ),
            alaItem(
                13, 3, false,
                "3. ¿La madre, padre o cuidador primario reporta que el niño ha experimentado una pérdida importante y constante de las habilidades que en algún momento tuvo?",
                "ALA 13.3. ¿La madre, padre o cuidador principal reporta que el niño ha experimentado una pérdida importante y constante de las habilidades que en algún momento tuvo?",
                "Pregunte a la madre, padre o cuidador principal si el niño ha perdido alguna habilidad o aprendizaje que ya había logrado. Distinga pérdida de un hito que ha desaparecido para evolucionar y convertirse en otro de un nivel mayor de desarrollo. La pérdida debe ser: mayor a tres meses, importante, constante y no transitoria."
            )
        )
    )

    private fun buildGroup14(): EdiGroupSpec = EdiGroupSpec(
        groupNumber = 14,
        code = "G14",
        officialTitle = "Grupo 14 (de 49 meses a 59 meses 29 días)",
        shortIntervalLabel = "49m 0d - 59m 29d",
        minMonthsInclusive = 49,
        maxMonthsInclusive = 59,
        formatPages = 44..46,
        consultationPages = 103..106,
        generalHeaderMaterialsText = "MF 14.1, MF 14.2 y MF 14.3: Papel y crayones o lápices de colores. CO 14.1: Discos de 6 cm de diámetro de diez colores diferentes. CO 14.2: Diez cubos de 3 x 3 cm del mismo color.",
        itemsPerActiveArea = 3,
        fallbackPreviousGroupNumber = 13,
        fallbackItemsCountPerArea = 3,
        frbAndAleModifyGlobalTrafficLight = false,
        areaItems = mapOf(
            EdiDevArea.MG to listOf(
                devItem(
                    14, EdiDevArea.MG, 1, false,
                    "1. ¿Sabe su niño darse volantines?",
                    "MG 14.1. ¿Sabe su niño darse volantines?",
                    "Pregunte a la madre, padre o cuidador principal: \"¿su niño logra darse volantines?\" Defina la frecuencia e intensidad de la conducta."
                ),
                devItem(
                    14, EdiDevArea.MG, 2, true,
                    "**2. ¿Puede su niño saltar hacia adelante cayendo con los pies juntos? (Observe)",
                    "**MG 14.2. ¿Puede su niño saltar hacia adelante cayendo con los pies juntos?",
                    "En un lugar con espacio suficiente, diga: \"¡vamos a jugar a saltar con los dos pies juntos! ¡Mira cómo lo hago! ¡Ahora te toca a ti!\""
                ),
                devItem(
                    14, EdiDevArea.MG, 3, true,
                    "**3. ¿Puede su niño saltar con un solo pie hacia adelante tres veces cayendo con el mismo pie? (Observe)",
                    "**MG 14.3. ¿Puede su niño brincar con un solo pie hacia adelante tres veces cayendo con el mismo pie?",
                    "En un lugar con espacio suficiente, diga: \"¡ahora vamos a jugar a saltar en un pie! ¡Mira cómo lo hago!, ¡Ahora tú salta en un pie como yo lo hice!\" Debe saltar con el mismo pie hacia adelante por lo menos 3 veces. Repetición del ítem: hasta 2 veces más de cada lado."
                )
            ),
            EdiDevArea.MF to listOf(
                devItem(
                    14, EdiDevArea.MF, 1, true,
                    "**1. ¿Puede dibujar un cuadrado? (Observe)",
                    "**MF 14.1. ¿Puede dibujar un cuadrado?",
                    "Colóquese frente al niño y diga: \"¡Ahora vamos a dibujar!, ¿puedes hacer un cuadrado para mí? Dibuja un cuadrado\". Criterios de aprobación: cuadrado con cuatro líneas y cuatro ángulos de los cuales tres, por lo menos, deben estar cerrados. Las líneas deben ser rectas y enteras. Nota: si no lo hace muéstrele cómo.",
                    headerMats = listOf("Papel y crayones o lápices de colores"),
                    protocolMats = listOf("Papel y crayones o lápices de colores")
                ),
                devItem(
                    14, EdiDevArea.MF, 2, true,
                    "**2. ¿Sabe escribir 2 o más letras? (Observe)",
                    "**MF 14.2. ¿Sabe escribir 2 o más letras?",
                    "Colóquese frente al niño y diga: \"¡Ahora vamos a escribir! ¿Puedes escribir para mí? Escribe algunas letras que te sepas, por ejemplo tu nombre\". Si no lo hace, dibuje algunas letras sencillas como: \"A\", \"V\", \"M\" o \"T\" y pídale que las copie.",
                    headerMats = listOf("Papel y crayones o lápices de colores"),
                    protocolMats = listOf("Papel y crayones o lápices de colores")
                ),
                devItem(
                    14, EdiDevArea.MF, 3, true,
                    "**3. ¿Puede dibujar una persona con 4 o más partes del cuerpo: ojos, nariz, boca, pelo, tronco, brazos, piernas, etc.? (Observe)",
                    "**MF 14.3. ¿Puede dibujar una persona con 4 o más partes del cuerpo: ojos, nariz, boca, pelo, tronco, brazos, piernas, etc.?",
                    "Colóquese frente al niño y diga: \"¡Ahora vamos a dibujar!, ¿puedes hacer un dibujo para mí? Dibuja un niño o una niña\". Observe si lo hace. Repetición del ítem: hasta 2 veces más.",
                    headerMats = listOf("Papel y crayones o lápices de colores"),
                    protocolMats = listOf("Papel y crayones o lápices de colores")
                )
            ),
            EdiDevArea.LE to listOf(
                devItem(
                    14, EdiDevArea.LE, 1, true,
                    "**1. ¿Sabe usar el tiempo futuro? (Observe)",
                    "**LE 14.1. ¿Sabe usar el tiempo futuro?",
                    "Pregunte al niño: \"¿Qué vas a hacer mañana? ¿A dónde vas a ir más tarde? ¿Cuándo irás a la escuela?\""
                ),
                devItem(
                    14, EdiDevArea.LE, 2, true,
                    "**2. ¿Puede contar cuentos? (Observe)",
                    "**LE 14.2. ¿Puede contar cuentos?",
                    "Pídale al niño que le cuente un cuento. En este ítem interesa valorar la capacidad de estructurar una narración coherente, con una historia que tenga: inicio, desarrollo, nudo y desenlace. Registre el uso de artículos, adjetivos y adverbios en el relato."
                ),
                // CONTROL 3: LE 14.3 emplea entrevista como modalidad principal (con observación complementaria).
                devItem(
                    14, EdiDevArea.LE, 3, false,
                    "3. Cuando algo le gusta mucho, ¿dice: \"dame más\"?",
                    "**LE 14.3. ¿Cuándo algo le gusta mucho, dice: \"dame más\"?",
                    "Modalidad principal Entrevista (con observación complementaria): Pregunte a la madre, padre o cuidador principal si el niño utiliza la expresión \"dame más\" cuando algo le gusta mucho. Por ejemplo: \"dame más papa\". Defina la frecuencia e intensidad de la conducta en comparación con otros niños de su misma edad.",
                    modality = EdiClinicalModality.INTERVIEW_PRIMARY_WITH_OBSERVATION_COMPLEMENT
                )
            ),
            EdiDevArea.SO to listOf(
                devItem(
                    14, EdiDevArea.SO, 1, false,
                    "1. ¿Puede decir el nombre de dos de sus amigos?",
                    "SO 14.1. ¿Puede decir el nombre de dos de sus amigos?",
                    "Pregunte a la madre, padre o cuidador principal si el niño dice el nombre de dos de sus amigos."
                ),
                devItem(
                    14, EdiDevArea.SO, 2, false,
                    "2. ¿Le gusta jugar al papá o a la mamá o actuar como otra persona de la familia?",
                    "SO 14.2. ¿Le gusta jugar al papá o a la mamá o actuar como otra persona de la familia?",
                    "Pregunte a la madre, padre o cuidador principal si el niño imita a personas de su familia y de su entorno cercano y asume roles típicos de algunos de sus miembros, como: \"hacer como que cocina\", \"hacer como que regaña a otro niño\", \"hacer como que se va a trabajar\". Defina la frecuencia e intensidad de la conducta en comparación con otros niños de su misma edad."
                ),
                devItem(
                    14, EdiDevArea.SO, 3, false,
                    "3. ¿Se puede bañar solo?",
                    "SO 14.3. ¿Se puede bañar solo?",
                    "Pregunte a la madre, padre o cuidador principal si el niño puede bañarse solo cumpliendo la mayoría de los pasos como desvestirse, enjabonarse, enjuagarse y secarse, aunque necesite ayuda para alcanzar algunos productos de higiene como el champú o la toalla. Defina la frecuencia e intensidad de la conducta en comparación con otros niños de su misma edad."
                )
            ),
            EdiDevArea.CO to listOf(
                devItem(
                    14, EdiDevArea.CO, 1, true,
                    "**1. ¿Puede decir el nombre correcto de cuatro colores y los puede reconocer? (Observe)",
                    "**CO 14.1. ¿Puede decir el nombre correcto de 4 colores y los puede reconocer?",
                    "Colóquese frente al niño y diga: \"Ahora vamos a jugar con estos discos. ¡Muéstrame el color rojo, verde, azul...\" La segunda parte del ítem es igual que en el grupo anterior. El niño debe nombrar 4 colores: \"¿Qué color es este?\" Independientemente de la respuesta continúe hasta que haya respondido al menos 4 correctamente. Repetición del ítem: hasta 2 veces más.",
                    headerMats = listOf("Discos de 6 cm de diámetro de diez colores diferentes"),
                    protocolMats = listOf("Discos de 6 cm de diámetro de diez colores diferentes")
                ),
                devItem(
                    14, EdiDevArea.CO, 2, true,
                    "**2. ¿Puede contar correctamente hasta diez? (Observe)",
                    "**CO 14.2. ¿Puede contar correctamente hasta diez?",
                    "Coloque diez cubos frente al niño y diga: \"Ahora vamos a jugar con estos cubos, quiero que los cuentes en voz alta. Cuenta cada uno tocándolos con tu dedo\". Repetición del ítem: hasta 2 veces más.",
                    headerMats = listOf("Diez cubos de 3 x 3 cm del mismo color"),
                    protocolMats = listOf("Diez cubos de 3 x 3 cm del mismo color")
                ),
                devItem(
                    14, EdiDevArea.CO, 3, false,
                    "3. Cuando habla, ¿utiliza las palabras \"ayer\", \"hoy\" y \"mañana\"?",
                    "CO 14.3. Cuando habla, ¿utiliza las palabras \"ayer\", \"hoy\" y \"mañana\"?",
                    "Pregunte a la madre, padre o cuidador principal si el niño utiliza palabras que expresan tiempo, como por ejemplo: \"ayer\", \"hoy\" y \"mañana\". Si responde afirmativamente, pregunte si lo hace de manera correcta. Defina la frecuencia e intensidad de la conducta en comparación con otros niños de su misma edad."
                )
            )
        ),
        // Las 8 Señales de Alerta oficiales del Grupo 14 (págs. 46, 103 y 104).
        aleItems = listOf(
            // NOTA CLÍNICA AUDITADA: Aunque el encabezado de la pág. 103 no menciona las Láminas B, C y D,
            // el protocolo directo de ALE 14.1 exige mostrar las Láminas B, C y D.
            aleItem(
                14, 1, true,
                "**1. Cuando habla, ¿usa incorrectamente los plurales y el tiempo pasado?, por ejemplo, no dice palabras como: \"tazas\", \"cucharas\", \"ayer vino mi abuelita\" (Observe)",
                "**ALE 14.1. Cuando habla, ¿usa incorrectamente los plurales y el tiempo pasado?, por ejemplo se le dificulta decir palabras como: \"tazas\", \"cucharas\", \"ayer vino mi abuelita\".",
                "Muéstrele la lámina B, señale el dibujo de un perro y diga: \"¿Qué es esto?\" Luego señale el de los dos perros y diga: \"¿Y estos?\" Repita el procedimiento con láminas C y D (Manual para la Formación de Facilitadores en la Prueba Evaluación del Desarrollo Infantil, páginas 111-112). Pregunte al niño: \"¿Qué hiciste ayer?, ¿qué comiste ayer?, ¿a dónde fuiste ayer?\"",
                headerMats = emptyList(),
                protocolMats = listOf("Láminas B, C y D (singular/plural: perro/perros, pelota/pelotas, niño/niños)"),
                plates = setOf(EdiOfficialPlate.LAMINA_B, EdiOfficialPlate.LAMINA_C, EdiOfficialPlate.LAMINA_D)
            ),
            aleItem(
                14, 2, false,
                "2. ¿Aún se le dificulta desvestirse solo, por ejemplo: abotonar, amarrar pasadores, ponerse o quitarse un polo/camiseta, etc.?",
                "ALE 14.2. ¿Aún se le dificulta desvestirse solo, por ejemplo: abotonar, amarrar pasadores, ponerse o quitarse un polo, etc.?",
                "Pregunte a la madre, padre o cuidador principal si su niño se viste o desviste solo. Se debe calificar como Sí, en caso de que al niño aún se le dificulte quitarse pantalones, camisa, polo, medias y zapatos."
            ),
            aleItem(
                14, 3, false,
                "3. ¿Se distrae fácilmente y le cuesta mucho trabajo concentrarse en alguna actividad por más de 5 minutos?",
                "ALE 14.3. ¿Se distrae fácilmente y le cuesta mucho trabajo concentrarse en alguna actividad por más de 5 minutos?",
                "Pregunte a la madre, padre o cuidador principal: \"¿diría que su niño es muy distraído comparado con otros niños de su edad?\", \"¿cuánto tiempo puede poner atención a algo que no le interesa mucho?\", \"¿cree que puede superar los 5 minutos?\" Defina la frecuencia e intensidad de la conducta."
            ),
            aleItem(
                14, 4, false,
                "4. ¿Actúa tímidamente o se muestra demasiado asustado?",
                "ALE 14.4. ¿Actúa tímidamente o se muestra demasiado asustado?",
                "Pregunte a la madre, padre o cuidador principal: \"¿su niño es demasiado tímido?, ¿cómo se comporta con niños o adultos que no conoce?, ¿se esconde o asusta?\" Defina la frecuencia e intensidad de la conducta en comparación con otros niños de su misma edad."
            ),
            aleItem(
                14, 5, false,
                "5. Cuando sus padres se separan de él ¿llora y protesta mucho?",
                "ALE 14.5. Cuando sus padres se separan de él, ¿llora y protesta mucho?",
                "Pregunte a la madre, padre o cuidador principal: \"¿diría que su niño tiene mucha dificultad para separarse de usted?\" Defina la frecuencia e intensidad de la conducta en comparación con otros niños de su misma edad."
            ),
            aleItem(
                14, 6, true,
                "**6. ¿Se le dificulta decir correctamente su nombre y apellido? (Observe)",
                "**ALE 14.6. ¿Se le dificulta decir correctamente su nombre y apellido?",
                "Pregunte al niño: \"¿cómo te llamas? Dime tu nombre y apellido, por favor\"."
            ),
            aleItem(
                14, 7, false,
                "7. ¿Expresa de manera pobre sus emociones?",
                "ALE 14.7. ¿Expresa de manera limitada sus emociones?",
                "¿Sabe usted cuando está contento, triste, enojado?, ¿cómo lo sabe? La emoción que expresa el niño, ¿coincide con lo que está pasando o no? Defina la frecuencia e intensidad de la conducta en comparación con otros niños de su misma edad."
            ),
            aleItem(
                14, 8, false,
                "8. ¿Constantemente se lo ve inactivo y poco interesado en lo que pasa a su alrededor?",
                "ALE 14.8. ¿Constantemente se lo ve inactivo y poco interesado en lo que pasa a su alrededor?",
                "Pregunte a la madre, padre o cuidador principal: \"¿parece como si estuviera en su propio mundo, sin interesarse por lo que pasa en la casa?, ¿parece no escuchar cuando le hablan?, ¿es demasiado independiente y no parece necesitar la ayuda de nadie?\" Defina la frecuencia e intensidad de la conducta en comparación con otros niños de su misma edad."
            )
        ),
        alaItems = listOf(
            alaItem(
                14, 1, false,
                "1. Cuando está con otros niños, ¿los ignora y prefiere jugar solo?",
                "ALA 14.1. Cuando está con otros niños, ¿los ignora y prefiere jugar solo?",
                "Pregúntele a la madre, padre o cuidador principal si está con otros niños que lo invitan a jugar, ¿lo acepta y juega? o ¿los ignora y se va a jugar solo? ¿Su juego es repetitivo y estereotipado? Solicite ejemplos para determinar si la señal está presente."
            ),
            alaItem(
                14, 2, false,
                "2. ¿La madre, padre o cuidador primario reporta que el niño ha experimentado una pérdida importante y constante de las habilidades que en algún momento tuvo?",
                "ALA 14.2. ¿La madre, padre o cuidador principal reporta que el niño ha experimentado una pérdida importante y constante de las habilidades que en algún momento tuvo?",
                "Pregunte a la madre, padre o cuidador principal si el niño ha perdido alguna habilidad o aprendizaje que ya había logrado. Distinga pérdida de un hito que ha desaparecido para evolucionar y convertirse en otro de un nivel mayor de desarrollo. La pérdida debe ser: mayor a tres meses, importante, constante y no transitoria."
            )
        )
    )

    private fun buildGroup15(): EdiGroupSpec = EdiGroupSpec(
        groupNumber = 15,
        code = "G15",
        officialTitle = "Grupo 15 (de 60 meses a 71 meses 29 días)",
        shortIntervalLabel = "60m 0d - 71m 29d",
        minMonthsInclusive = 60,
        maxMonthsInclusive = 71,
        formatPages = 47..48,
        consultationPages = 107..111,
        generalHeaderMaterialsText = "MG 15.3: Rollo de cinta adhesiva. MF 15.1: papel y lápiz. MF 15.3: Papel y tijeras de punta redondeada.",
        itemsPerActiveArea = 3,
        fallbackPreviousGroupNumber = 14,
        fallbackItemsCountPerArea = 3,
        frbAndAleModifyGlobalTrafficLight = false,
        neurologicalEn13Applies = false, // En Grupo 15 sólo aplican EN 1.1 y EN 1.2 (págs. 47 y 111)
        areaItems = mapOf(
            EdiDevArea.MG to listOf(
                devItem(
                    15, EdiDevArea.MG, 1, true,
                    "**1. ¿Puede su niño saltar con un solo pie hacia adelante siete veces cayendo con el mismo pie? (Observe)",
                    "**MG 15.1. ¿Puede su niño saltar con un solo pie hacia adelante siete veces cayendo con el mismo pie?",
                    "En un lugar con suficiente espacio diga: \"¡vamos a jugar a saltar con un pie!, ¡mira cómo lo hago, ahora te toca a ti!\" Repetición del ítem: hasta dos veces con cada pie."
                ),
                devItem(
                    15, EdiDevArea.MG, 2, true,
                    "**2. ¿Puede saltar hacia atrás con los pies juntos? (Observe)",
                    "**MG 15.2. ¿Puede brincar hacia atrás con los pies juntos?",
                    "En un lugar con espacio suficiente, muéstrale cómo se hace, después colóquese detrás del niño a una distancia razonable para prevenir accidentes y dígale: \"vamos a jugar a saltar\"."
                ),
                devItem(
                    15, EdiDevArea.MG, 3, true,
                    "**3. ¿Camina siguiendo una línea recta, juntando el talón de un pie con la punta del otro pie, por lo menos 5 pasos? (Observe)",
                    "**MG 15.3. ¿Camina siguiendo una línea recta, juntando el talón de un pie con la punta del otro pie, por lo menos cinco pasos?",
                    "Realice previamente una línea recta en el suelo. Muéstrele cómo se hace. Repetición del ítem hasta dos veces más.",
                    headerMats = listOf("Rollo de cinta adhesiva"),
                    protocolMats = listOf("Rollo de cinta adhesiva (o línea recta marcada en el suelo)")
                )
            ),
            EdiDevArea.MF to listOf(
                devItem(
                    15, EdiDevArea.MF, 1, true,
                    "**1. ¿Puede dibujar un triángulo copiándolo? (Observe)",
                    "**MF 15.1. ¿Puede dibujar un triángulo copiándolo?",
                    "Tenga papel y lápiz para el niño y para usted, colóquese frente a él y diga: \"ahora vamos a dibujar, ¿puedes copiar un triángulo como este?\" Usted dibuje un triángulo de lados iguales. Para calificar el ítem la figura debe tener tres líneas y tres ángulos cerrados, las líneas deben estar completas y rectas. Repetición del ítem: hasta dos veces más.",
                    headerMats = listOf("Papel y lápiz"),
                    protocolMats = listOf("Papel y lápiz")
                ),
                devItem(
                    15, EdiDevArea.MF, 2, true,
                    "**2. ¿Puede tocar la punta de su dedo pulgar con la punta de cada dedo de la misma mano consecutivamente? (Observe)",
                    "**MF 15.2. ¿Puede tocar la punta de su dedo pulgar con la punta de cada dedo de la misma mano consecutivamente?",
                    "Colóquese frente al niño y diga: \"mira lo que hago\". Toque sucesivamente las puntas de los dedos de cada mano con el dedo pulgar de la misma mano. Diga: \"ahora haz lo que yo hice\". Repetición del ítem: hasta una vez más de cada lado."
                ),
                devItem(
                    15, EdiDevArea.MF, 3, true,
                    "**3. ¿Puede cortar el papel con tijeras de punta redonda siguiendo una línea recta, teniendo 1 cm de margen de error? (Observe)",
                    "**MF 15.3. ¿Puede cortar el papel con tijeras de punta redonda siguiendo una línea recta, teniendo 1 cm de margen de error?",
                    "Dibuje una línea recta sobre el papel y dele tijeras de punta redonda (para niños), colóquese de frente y diga: \"corta el papel tratando de seguir la línea, córtalo en dos pedazos\". Si el niño no lo hace, repita la indicación. Para calificar el ítem el niño debe de cortar la hoja en dos pedazos teniendo un margen de 1 cm desde la línea.",
                    headerMats = listOf("Papel y tijeras de punta redondeada"),
                    protocolMats = listOf("Papel con línea recta y tijeras de punta redondeada")
                )
            ),
            EdiDevArea.LE to listOf(
                devItem(
                    15, EdiDevArea.LE, 1, true,
                    "**1. ¿Habla con suficiente claridad para que otros lo entiendan? (Observe)",
                    "**LE 15.1. ¿Habla con suficiente claridad para que otros lo entiendan?",
                    "Colóquese frente al niño y pregunte: \"¿qué hiciste hoy en la escuela?, ¿a qué te gusta jugar?\". La respuesta debe ser clara y coherente para que sea entendida por personas ajenas a la familia."
                ),
                devItem(
                    15, EdiDevArea.LE, 2, true,
                    "**2. ¿Comunica sus emociones diciendo palabras como: \"feliz\", \"triste\", \"molesto\"? (si recibes regalos, ¿cómo te sientes?). (Observe)",
                    "**LE 15.2. ¿Comunica sus emociones diciendo palabras como: \"feliz\", \"triste\" y \"molesto\"? \"si recibes regalos o tu comida favorita, ¿cómo te sientes?\"",
                    "Pregunta al niño: \"¿cómo te sientes cuando recibes regalos?\" Para calificarlo sólo se debe expresar una emoción (feliz, triste, enojado). Si responde \"bien\" o \"mal\" la respuesta es incorrecta."
                ),
                devItem(
                    15, EdiDevArea.LE, 3, true,
                    "**3. ¿Puede seguir órdenes verbales de tres pasos, por ejemplo: \"aplaude, dame el lápiz y ponte de pie\"? (Observe)",
                    "**LE 15.3. ¿Puede seguir órdenes verbales de tres pasos, por ejemplo: \"aplaude, dame el lápiz y levántate\"?",
                    "Colóquese frente al niño y diga: \"toca tu nariz, aplaude y levántate\". Para validar el ítem el niño debe hacer todo en el orden dado."
                )
            ),
            EdiDevArea.SO to listOf(
                devItem(
                    15, EdiDevArea.SO, 1, false,
                    "1. ¿La mayoría de las veces comparte fácilmente sus cosas con otros niños?",
                    "SO 15.1. ¿La mayoría de las veces comparte fácilmente sus cosas con otros niños?",
                    "Pregunte a la madre, padre o cuidador principal si el niño comparte voluntariamente y sin pelear sus pertenencias con otros niños de su edad que le agraden."
                ),
                devItem(
                    15, EdiDevArea.SO, 2, false,
                    "2. ¿Le gusta ir a la escuela?",
                    "SO 15.2. ¿Le gusta ir a la escuela?",
                    "Pregunte a la madre, padre o cuidador principal si al niño le gusta ir a la escuela."
                ),
                devItem(
                    15, EdiDevArea.SO, 3, false,
                    "3. ¿Espera su turno con facilidad cuando interactúa con sus compañeros, maestros o cuidadores primarios?",
                    "SO 15.3. ¿Espera su turno con facilidad cuando interactúa con sus compañeros, maestros o cuidadores primarios?",
                    "Pregunte a la madre, padre o cuidador principal si el niño puede esperar su turno, por ejemplo: en el salón de clases cuando el maestro está ocupado o si, por el contrario, interrumpe frecuentemente las conversaciones de los demás."
                )
            ),
            EdiDevArea.CO to listOf(
                devItem(
                    15, EdiDevArea.CO, 1, true,
                    "**1. Cuando le piden que escriba 2 números o 2 letras, ¿lo hace? (Observe)",
                    "**CO 15.1. Cuando le pides que escriba dos números o 2 letras, ¿lo hace?",
                    "Colóquese frente al niño y dele papel y lápiz, pida que escriba dos letras que conozca o algunos números. Es necesario que escriba dos letras o dos números para validar el ítem.",
                    protocolMats = listOf("Papel y lápiz")
                ),
                devItem(
                    15, EdiDevArea.CO, 2, true,
                    "**2. ¿Puede completar oraciones con la palabra que significa lo opuesto? Por ejemplo: \"el conejo es rápido, la tortuga es...\" (Observe)",
                    "**CO 15.2. ¿Puede completar oraciones con la palabra que significa lo opuesto? Por ejemplo: \"El sol sale de día y la luna sale de...\".",
                    "Colóquese frente al niño y diga: \"Pon mucha atención, vamos a jugar a las adivinanzas, si yo digo: 'el sol sale de día, ¿la luna sale de...?' o 'Los peces van por el agua, ¿los pájaros van por el...?'\". Puede repetir la oración una vez más."
                ),
                devItem(
                    15, EdiDevArea.CO, 3, true,
                    "**3. ¿Identifica el valor de dos o más monedas o billetes? (Observe)",
                    "**CO 15.3. ¿Identifica el valor de dos o más números en diferentes textos, incluyendo monedas o billetes?",
                    "Colóquese frente al niño y muestre monedas o billetes de diferente denominación. Debe identificar por lo menos dos denominaciones.",
                    protocolMats = listOf("Monedas o billetes de diferente denominación")
                )
            )
        ),
        aleItems = listOf(
            aleItem(
                15, 1, false,
                "1. ¿El niño necesita acercarse mucho a los objetos que le llaman la atención o inclina la cabeza para verlos?",
                "ALE 15.1. ¿El niño necesita acercarse mucho a los objetos que le llaman la atención o inclina la cabeza para verlos?",
                "Pregunte a la madre, padre o cuidador principal: cuando su niño usa dispositivos electrónicos, juguetes o está escribiendo o dibujando, ¿se acerca demasiado a éstos? Defina la frecuencia e intensidad de la conducta en comparación con otros niños de su misma edad."
            ),
            aleItem(
                15, 2, false,
                "2. ¿El niño se cae frecuentemente al correr?",
                "ALE 15.2. ¿El niño se cae frecuentemente al correr?",
                "Pregunte a la madre, padre o cuidador principal: \"Cuando su niño corre, ¿se cae con mayor frecuencia que otros niños de su edad?\""
            ),
            aleItem(
                15, 3, false,
                "3. ¿El niño se orina en la cama por las noches?",
                "ALE 15.3. ¿El niño se orina en la cama por las noches?",
                "Pregunte a la madre, padre o cuidador principal: \"¿Su niño se orina en la cama al menos dos veces por semana durante la noche?\" Corroborar que la causa no sea ingesta de líquidos antes de dormir o de algún medicamento."
            ),
            aleItem(
                15, 4, false,
                "4. ¿Se distrae fácilmente y le cuesta mucho trabajo concentrarse en alguna actividad por más de cinco minutos?",
                "ALE 15.4. ¿Se distrae fácilmente y le cuesta mucho trabajo concentrarse en alguna actividad por más de cinco minutos?",
                "Pregunte a la madre, padre o cuidador principal: ¿El niño tiene dificultad para mantener la atención en tareas o actividades de lectoescritura?"
            )
        ),
        alaItems = listOf(
            alaItem(
                15, 1, false,
                "1. ¿Presenta dolores de cabeza persistentes, visión borrosa o mareo?",
                "ALA 15.1. ¿Presenta dolores de cabeza persistentes, visión borrosa o mareo?",
                "Pregúntele a la madre, padre o cuidador principal si el niño presenta dolor de cabeza, visión borrosa o mareo la mayoría de los días de la semana en comparación con otros niños de su misma edad."
            ),
            alaItem(
                15, 2, false,
                "2. ¿Presenta dificultad para cepillarse los dientes, lavarse y secarse las manos o desvestirse sin ayuda?",
                "ALA 15.2. ¿Presenta dificultad para cepillarse los dientes, lavarse y secarse las manos o desvestirse sin ayuda?",
                "Pregunte a la madre, padre o cuidador principal si a pesar de enseñarle, el niño no logra aún cepillarse los dientes, lavarse y secarse las manos o desvestirse sin ayuda."
            ),
            alaItem(
                15, 3, false,
                "3. ¿Durante más de tres días a la semana el niño presenta miedo, agresión, timidez o tristeza en mayor intensidad que los niños de su edad?",
                "ALA 15.3. ¿Durante más de tres días a la semana el niño presenta miedo, agresión, timidez o tristeza en mayor intensidad que los niños de su edad?",
                "Pregunte a la madre, padre o cuidador principal si el niño muestra miedo, agresión, timidez o tristeza durante cuatro días a la semana, en mayor intensidad que otros niños de su misma edad."
            )
        )
    )

    val groups: List<EdiGroupSpec> = listOf(
        buildGroup1(),
        buildGroup2(),
        buildGroup3(),
        buildGroup4(),
        buildGroup5(),
        buildGroup6(),
        buildGroup7(),
        buildGroup8(),
        buildGroup9(),
        buildGroup10(),
        buildGroup11(),
        buildGroup12(),
        buildGroup13(),
        buildGroup14(),
        buildGroup15()
    )

    private val itemsByUniqueId: Map<String, EdiCatalogItem> by lazy {
        val all = mutableListOf<EdiCatalogItem>()
        all.addAll(frbItems)
        all.addAll(allEnItems)
        all.addAll(specialGroup13CoRFallbackItems)
        groups.forEach { g ->
            g.areaItems.values.forEach { all.addAll(it) }
            all.addAll(g.aleItems)
            all.addAll(g.alaItems)
        }
        all.associateBy { it.uniqueId }
    }

    private val itemsByOfficialCode: Map<String, EdiCatalogItem> by lazy {
        itemsByUniqueId.values.associateBy { it.itemCode }
    }

    fun getGroup(groupNumber: Int): EdiGroupSpec =
        groups.firstOrNull { it.groupNumber == groupNumber }
            ?: error("Grupo EDI fuera de rango oficial (1..15): $groupNumber")

    fun findItemByUniqueId(uniqueId: String): EdiCatalogItem? =
        itemsByUniqueId[uniqueId]

    fun findItemByCode(itemCode: String): EdiCatalogItem? =
        itemsByOfficialCode[itemCode.trim()]

    fun getItemsRequiringPlatesInProtocol(): List<EdiCatalogItem> =
        itemsByUniqueId.values.filter { it.protocolRequiredPlates.isNotEmpty() }

    /**
     * Selecciona el grupo oficial EDI (1..15) a partir de la edad en meses totales y días,
     * excluyendo automáticamente las edades fuera del rango oficial (< 1 mes y >= 72 meses).
     */
    fun selectGroupForAge(totalMonths: Int, days: Int): Pair<EdiAgeRangeStatus, EdiGroupSpec?> {
        if (totalMonths < 0 || days < 0) {
            return EdiAgeRangeStatus.INVALID_DATE_INPUT to null
        }
        if (totalMonths < 1) {
            return EdiAgeRangeStatus.UNDER_MIN_AGE_LESS_THAN_1_MONTH to null
        }
        if (totalMonths >= 72) {
            return EdiAgeRangeStatus.OVER_MAX_AGE_72_MONTHS_OR_MORE to null
        }
        val group = groups.firstOrNull { totalMonths in it.minMonthsInclusive..it.maxMonthsInclusive }
        return EdiAgeRangeStatus.IN_OFFICIAL_RANGE_G1_TO_G15 to group
    }

    /**
     * Calcula la Edad Cronológica y la Edad Corregida siguiendo exactamente el algoritmo de resta
     * en papel del Manual EDI Peruano 2026 (págs. 10-11):
     * - Pide prestados 30 días al mes cuando el día de evaluación es menor al día de nacimiento
     *   o cuando los días cronológicos son menores a los días de prematuridad.
     * - Pide prestados 12 meses al año cuando el mes de evaluación es menor al mes de nacimiento.
     */
    fun calculateManualEdiAges(
        fechaNacimiento: String,
        fechaEvaluacion: String,
        esPrematuro: Boolean,
        gestationalAge: GestationalAgeInput
    ): EdiSingleMethodGroupResolution {
        val birth = ClinicalAgeCalculator.parseDate(fechaNacimiento)
        val eval = ClinicalAgeCalculator.parseDate(fechaEvaluacion)
        if (birth == null || eval == null) {
            val zero = EdiAgeBreakdown(0, 0, 0)
            return EdiSingleMethodGroupResolution(
                methodLabel = "Método Manual EDI (30d/12m)",
                chronologicalAge = zero,
                correctedAge = zero,
                appliesCorrectedAge = false,
                effectiveAgeUsed = zero,
                rangeStatus = EdiAgeRangeStatus.INVALID_DATE_INPUT,
                assignedGroup = null
            )
        }

        val cronoSteps = mutableListOf<String>()
        var y = eval.year
        var m = eval.month
        var d = eval.day

        if (d < birth.day) {
            m -= 1
            d += 30
            cronoSteps.add("Préstamo cronológico de 1 mes (30 días): día = $d, mes = $m")
        }
        if (m < birth.month) {
            y -= 1
            m += 12
            cronoSteps.add("Préstamo cronológico de 1 año (12 meses): mes = $m, año = $y")
        }

        var cronoYears = y - birth.year
        var cronoMonths = m - birth.month
        var cronoDays = d - birth.day

        if (cronoYears < 0 || (cronoYears == 0 && cronoMonths == 0 && cronoDays < 0)) {
            val zero = EdiAgeBreakdown(0, 0, 0)
            return EdiSingleMethodGroupResolution(
                methodLabel = "Método Manual EDI (30d/12m)",
                chronologicalAge = zero,
                correctedAge = zero,
                appliesCorrectedAge = false,
                effectiveAgeUsed = zero,
                rangeStatus = EdiAgeRangeStatus.INVALID_DATE_INPUT,
                assignedGroup = null
            )
        }

        if (cronoDays >= 30) {
            cronoMonths += cronoDays / 30
            cronoDays %= 30
        }
        if (cronoMonths >= 12) {
            cronoYears += cronoMonths / 12
            cronoMonths %= 12
        }

        val chronological = EdiAgeBreakdown(
            years = cronoYears,
            months = cronoMonths,
            days = cronoDays,
            borrowStepsDescription = cronoSteps
        )

        val appliesCorrected = esPrematuro && gestationalAge.isPreterm && cronoYears < 2
        val prematurityDays = if (appliesCorrected) gestationalAge.prematurityDaysTo40Weeks else 0

        val corrected = if (appliesCorrected && prematurityDays > 0) {
            val corrSteps = mutableListOf<String>()
            var cy = cronoYears
            var cm = cronoMonths
            var cd = cronoDays
            var borrowedMonthsCount = 0
            while (cd < prematurityDays) {
                if (cm <= 0 && cy > 0) {
                    cy -= 1
                    cm += 12
                    corrSteps.add("Préstamo en corrección de 1 año (12 meses)")
                }
                cm -= 1
                cd += 30
                borrowedMonthsCount += 1
            }
            if (borrowedMonthsCount > 0) {
                corrSteps.add("Préstamo en corrección de $borrowedMonthsCount mes(es) (${borrowedMonthsCount * 30} días): días acumulados = $cd")
            }
            val finalDays = cd - prematurityDays
            corrSteps.add("Resta de prematuridad: $cd - $prematurityDays = $finalDays días")
            if (cm < 0) {
                EdiAgeBreakdown(0, 0, 0, corrSteps)
            } else {
                EdiAgeBreakdown(
                    years = cy.coerceAtLeast(0),
                    months = cm.coerceAtLeast(0),
                    days = finalDays.coerceAtLeast(0),
                    borrowStepsDescription = corrSteps
                )
            }
        } else {
            chronological
        }

        val effectiveAge = if (appliesCorrected) corrected else chronological
        val (status, group) = selectGroupForAge(effectiveAge.totalMonths, effectiveAge.days)

        return EdiSingleMethodGroupResolution(
            methodLabel = "Método Manual EDI (30d/12m)",
            chronologicalAge = chronological,
            correctedAge = corrected,
            appliesCorrectedAge = appliesCorrected,
            effectiveAgeUsed = effectiveAge,
            rangeStatus = status,
            assignedGroup = group
        )
    }

    /**
     * Compara de manera aislada el Método Manual EDI (págs. 10-11) frente al Método Calendario CRED
     * (`ClinicalAgeCalculator.calculateAges`, que permanece intacto).
     *
     * Cumple Control 1:
     * - Si ambos métodos seleccionan el mismo grupo (ej. 14/07/2024 al 20/03/2025 con 34 semanas:
     *   6m 24d vs 6m 23d, ambos en Grupo 5), asigna automáticamente ese grupo.
     * - Si ambos métodos seleccionan grupos distintos, BLOQUEA la asignación automática y exige
     *   revisión profesional.
     */
    fun compareCorrectedAgeMethods(
        fechaNacimiento: String,
        fechaEvaluacion: String,
        esPrematuro: Boolean,
        egSemanas: Int,
        egDias: Int = 0
    ): EdiCorrectedAgeComparison {
        val ga = GestationalAgeInput(weeks = egSemanas, days = egDias)
        val manualRes = calculateManualEdiAges(
            fechaNacimiento = fechaNacimiento,
            fechaEvaluacion = fechaEvaluacion,
            esPrematuro = esPrematuro,
            gestationalAge = ga
        )

        val credAgeResult = ClinicalAgeCalculator.calculateAges(
            fechaNacimiento = fechaNacimiento,
            fechaEvaluacion = fechaEvaluacion,
            esPrematuro = esPrematuro,
            egSemanas = ga.safeWeeks,
            egDias = ga.safeDays
        )

        val credChrono = EdiAgeBreakdown(
            years = credAgeResult.cronologicaAnios,
            months = credAgeResult.cronologicaMesesDelAnio,
            days = credAgeResult.cronologicaDias
        )
        val credCorr = EdiAgeBreakdown(
            years = credAgeResult.corregidaAnios,
            months = credAgeResult.corregidaMesesDelAnio,
            days = credAgeResult.corregidaDias
        )
        val credEffective = if (credAgeResult.aplicaEdadCorregida) credCorr else credChrono
        val (credStatus, credGroup) = selectGroupForAge(credEffective.totalMonths, credEffective.days)

        val credRes = EdiSingleMethodGroupResolution(
            methodLabel = "Método Calendario CRED",
            chronologicalAge = credChrono,
            correctedAge = credCorr,
            appliesCorrectedAge = credAgeResult.aplicaEdadCorregida,
            effectiveAgeUsed = credEffective,
            rangeStatus = credStatus,
            assignedGroup = credGroup
        )

        val manualApproxTotalDays = manualRes.effectiveAgeUsed.totalMonths * 30 + manualRes.effectiveAgeUsed.days
        val credApproxTotalDays = credRes.effectiveAgeUsed.totalMonths * 30 + credRes.effectiveAgeUsed.days
        val daysDiff = kotlin.math.abs(manualApproxTotalDays - credApproxTotalDays)

        val sameGroup = manualRes.rangeStatus == credRes.rangeStatus &&
            manualRes.assignedGroup?.groupNumber == credRes.assignedGroup?.groupNumber

        val autoStatus = when {
            manualRes.rangeStatus == EdiAgeRangeStatus.INVALID_DATE_INPUT ||
                credRes.rangeStatus == EdiAgeRangeStatus.INVALID_DATE_INPUT ->
                EdiGroupAutoSelectionStatus.INVALID_DATES

            !sameGroup ->
                EdiGroupAutoSelectionStatus.BLOCKED_GROUP_DISCREPANCY_REQUIRES_PROFESSIONAL_REVIEW

            manualRes.rangeStatus == EdiAgeRangeStatus.UNDER_MIN_AGE_LESS_THAN_1_MONTH ->
                EdiGroupAutoSelectionStatus.OUT_OF_RANGE_UNDER_1_MONTH

            manualRes.rangeStatus == EdiAgeRangeStatus.OVER_MAX_AGE_72_MONTHS_OR_MORE ->
                EdiGroupAutoSelectionStatus.OUT_OF_RANGE_72_MONTHS_OR_MORE

            else ->
                EdiGroupAutoSelectionStatus.AUTO_SELECTED_CONSISTENT
        }

        val candidates = listOfNotNull(manualRes.assignedGroup, credRes.assignedGroup)
            .distinctBy { it.groupNumber }

        val message = when (autoStatus) {
            EdiGroupAutoSelectionStatus.AUTO_SELECTED_CONSISTENT -> {
                val g = manualRes.assignedGroup!!
                "Asignación consistente en ${g.code} (${g.shortIntervalLabel}). Manual EDI: ${manualRes.effectiveAgeUsed.formattedMonthsDays} | Calendario CRED: ${credRes.effectiveAgeUsed.formattedMonthsDays}."
            }

            EdiGroupAutoSelectionStatus.BLOCKED_GROUP_DISCREPANCY_REQUIRES_PROFESSIONAL_REVIEW -> {
                val gMan = manualRes.assignedGroup?.code ?: manualRes.rangeStatus.name
                val gCred = credRes.assignedGroup?.code ?: credRes.rangeStatus.name
                "BLOQUEO DE SEGURIDAD CLÍNICA: Discrepancia de grupo entre Método Manual EDI (${manualRes.effectiveAgeUsed.formattedMonthsDays} -> $gMan) y Método Calendario CRED (${credRes.effectiveAgeUsed.formattedMonthsDays} -> $gCred). Se requiere revisión y selección manual del profesional."
            }

            EdiGroupAutoSelectionStatus.OUT_OF_RANGE_UNDER_1_MONTH ->
                "Edad menor a 1 mes (${manualRes.effectiveAgeUsed.formattedMonthsDays}): fuera del rango oficial de aplicación automática EDI (inicia a partir de 1 mes 0 días)."

            EdiGroupAutoSelectionStatus.OUT_OF_RANGE_72_MONTHS_OR_MORE ->
                "Edad de 72 meses o más (${manualRes.effectiveAgeUsed.formattedMonthsDays}): fuera del rango oficial de aplicación automática EDI (finaliza a los 71 meses 29 días)."

            EdiGroupAutoSelectionStatus.INVALID_DATES ->
                "Fechas de nacimiento o evaluación inválidas."
        }

        return EdiCorrectedAgeComparison(
            gestationalAge = ga,
            isPretermFlag = esPrematuro,
            manualMethodResult = manualRes,
            calendarCredMethodResult = credRes,
            daysDifferenceBetweenMethods = daysDiff,
            selectsSameGroup = sameGroup,
            autoSelectionStatus = autoStatus,
            automaticallyAssignedGroup = if (autoStatus == EdiGroupAutoSelectionStatus.AUTO_SELECTED_CONSISTENT) {
                manualRes.assignedGroup
            } else {
                null
            },
            candidateGroupsForProfessionalReview = candidates,
            clinicalReviewMessage = message
        )
    }
}
