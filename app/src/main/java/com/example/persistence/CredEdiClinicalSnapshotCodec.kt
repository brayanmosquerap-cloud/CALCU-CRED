package com.example.persistence

import com.example.model.AppTab
import com.example.model.CredEdiUiState
import com.example.model.CurvaIndicador
import com.example.model.EdiAgeDiscrepancyResolution
import com.example.model.EdiArchivedClinicalContext
import com.example.model.EdiArchivedOriginValidationState
import com.example.model.EdiArchivedSessionEntry
import com.example.model.EdiAxis
import com.example.model.EdiBirthMaturityCondition
import com.example.model.EdiClinicalModality
import com.example.model.EdiDevArea
import com.example.model.EdiDualEvidenceRecord
import com.example.model.EdiGroupFilterCategory
import com.example.model.EdiGroupSessionSnapshot
import com.example.model.EdiInternalView
import com.example.model.EdiItemResponse
import com.example.model.EdiPendingGroupChangeRequest
import com.example.model.EdiPhase1EngineState
import com.example.model.EdiPhase1SessionManager
import com.example.model.PatientIdentificationData
import com.example.model.SexoBiologico
import org.json.JSONArray
import org.json.JSONObject

/**
 * Encabezado clínico descifrado para poblar vistas de resumen en el historial de evaluaciones
 * sin necesidad de deserializar todo el árbol de sesiones archivadas.
 */
data class DecodedPatientHeader(
    val evaluationUuid: String,
    val patientIdentification: PatientIdentificationData,
    val sexo: SexoBiologico,
    val fechaNacimiento: String,
    val fechaEvaluacion: String,
    val esPrematuro: Boolean,
    val egSemanas: Int,
    val egDias: Int,
    val ediEgRegistrada: Boolean,
    val ediBirthMaturityCondition: EdiBirthMaturityCondition,
    val effectiveAgeLabel: String,
    val activeGroupCode: String,
    val activeGroupTitle: String,
    val semaphoreLabel: String,
    val anthropometrySummaryLabel: String
)

/**
 * Excepción verificable emitida cuando un snapshot clínico serializado es inválido,
 * incompatible o presenta discrepancia de integridad.
 */
class ClinicalSnapshotSerializationException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)

/**
 * MEDIO-06 — LOTE 1:
 * Serializador clínico determinista y versionado (`SCHEMA_VERSION = 1`) para `CredEdiUiState`.
 *
 * Garantías clínicas críticas:
 * 1. Preserva `Double.NaN` como `JSONObject.NULL` (`null` en JSON) y jamás lo convierte en `0.0`,
 *    `39.5`, `5.40` ni `61.5`, manteniendo intactos los lotes `PC/E-01` y `ANTRO-02`.
 * 2. Preserva íntegramente los 5 valores de `EdiItemResponse`:
 *    - `UNANSWERED`
 *    - `YES`
 *    - `NO`
 *    - `NOT_EVALUATED_MISSING_MATERIAL`
 *    - `DISCREPANCY_REQUIRES_HUMAN_RESOLUTION`
 * 3. Preserva `currentGroupResponses`, `fallbackGroupResponses`, `frbResponses`, `enResponses`,
 *    `dualEvidenceRecords`, `itemClinicalNotes`, `archivedSessionsByGroup`, `archivedSessionHistory`,
 *    `activeSessionOriginContext`, `restoredFromArchiveId`, `restoredSessionOriginContext`,
 *    `ediAgeDiscrepancyResolution` y `patientIdentification`.
 * 4. Al restaurar un snapshot, todos los motores derivados (`ediGlobalEvaluation`, `evalPE`, `evalTE`,
 *    `evalPT`, `evalIMCE`, `evalPCE`, `globalDiagnosis`) se recalculan dinámicamente con las reglas
 *    clínicas vigentes sin almacenar semáforos falsos ni promover estados incompletos.
 */
object CredEdiClinicalSnapshotCodec {
    const val CURRENT_SCHEMA_VERSION = 1

    fun encodePatientHeaderToJson(state: CredEdiUiState): String {
        val eval = state.ediGlobalEvaluation
        val semaforoText = when {
            eval.isDefinitive -> "${eval.trafficLight.displayLabel} (Definitivo)"
            eval.hasPriorityCriticalWarning -> "Incompleto con Alerta Crítica Prioritaria"
            else -> "Incompleto / En progreso"
        }
        val antroParts = mutableListOf<String>()
        if (state.hasPesoIngresado && state.effectivePesoKg.isFinite()) {
            antroParts.add("Peso: ${state.pesoInput.trim()} kg")
        }
        if (state.hasLongitudIngresada && state.effectiveLongitudCm.isFinite()) {
            antroParts.add("Talla: ${state.longitudInput.trim()} cm")
        }
        if (state.hasPerimetroCefalicoIngresado && state.effectivePerimetroCefalicoCm.isFinite()) {
            antroParts.add("PC: ${state.perimetroCefalicoInput.trim()} cm")
        }
        val antroSummary = if (antroParts.isEmpty()) {
            "Sin mediciones antropométricas registradas"
        } else {
            antroParts.joinToString(" · ")
        }
        val ageLabel = if (state.isChronologicalAgeVerifiable) {
            "${state.edadEfectivaAMD} (${state.edadEfectivaTipoLabel})"
        } else {
            "Edad no verificada"
        }

        return JSONObject().apply {
            put("schemaVersion", CURRENT_SCHEMA_VERSION)
            put("evaluationUuid", state.evaluationUuid)
            put("patientIdentifierOrAlias", state.patientIdentification.patientIdentifierOrAlias)
            put("clinicalRecordCode", state.patientIdentification.clinicalRecordCode)
            put("generalClinicalNote", state.patientIdentification.generalClinicalNote)
            put("sexo", state.sexo.name)
            put("fechaNacimiento", state.fechaNacimiento)
            put("fechaEvaluacion", state.fechaEvaluacion)
            put("esPrematuro", state.esPrematuro)
            put("egSemanas", state.egSemanas)
            put("egDias", state.egDias)
            put("ediEgRegistrada", state.ediEgRegistrada)
            put("ediBirthMaturityCondition", state.ediBirthMaturityCondition.name)
            put("effectiveAgeLabel", ageLabel)
            put("activeGroupCode", state.ediActiveGroupSpec.code)
            put("activeGroupTitle", state.ediActiveGroupSpec.officialTitle)
            put("semaphoreLabel", semaforoText)
            put("anthropometrySummaryLabel", antroSummary)
        }.toString()
    }

    @Throws(ClinicalSnapshotSerializationException::class)
    fun decodePatientHeaderFromJson(jsonString: String): DecodedPatientHeader {
        try {
            val root = JSONObject(jsonString)
            val uuid = root.optString("evaluationUuid", "").trim()
            if (uuid.isEmpty()) {
                throw ClinicalSnapshotSerializationException("El encabezado de paciente no contiene un evaluationUuid válido.")
            }
            return DecodedPatientHeader(
                evaluationUuid = uuid,
                patientIdentification = PatientIdentificationData(
                    patientIdentifierOrAlias = root.optString("patientIdentifierOrAlias", ""),
                    clinicalRecordCode = root.optString("clinicalRecordCode", ""),
                    generalClinicalNote = root.optString("generalClinicalNote", "")
                ),
                sexo = parseEnumOrThrow(root.optString("sexo", SexoBiologico.NINO.name), "sexo"),
                fechaNacimiento = root.optString("fechaNacimiento", ""),
                fechaEvaluacion = root.optString("fechaEvaluacion", ""),
                esPrematuro = root.optBoolean("esPrematuro", false),
                egSemanas = root.optInt("egSemanas", 32),
                egDias = root.optInt("egDias", 0),
                ediEgRegistrada = root.optBoolean("ediEgRegistrada", false),
                ediBirthMaturityCondition = parseEnumOrThrow(
                    root.optString("ediBirthMaturityCondition", EdiBirthMaturityCondition.UNSPECIFIED.name),
                    "ediBirthMaturityCondition"
                ),
                effectiveAgeLabel = root.optString("effectiveAgeLabel", "Edad no verificada"),
                activeGroupCode = root.optString("activeGroupCode", "G1"),
                activeGroupTitle = root.optString("activeGroupTitle", "Grupo 1"),
                semaphoreLabel = root.optString("semaphoreLabel", "Incompleto"),
                anthropometrySummaryLabel = root.optString("anthropometrySummaryLabel", "Sin mediciones")
            )
        } catch (e: ClinicalSnapshotSerializationException) {
            throw e
        } catch (e: Exception) {
            throw ClinicalSnapshotSerializationException("Error al decodificar el encabezado clínico del paciente.", e)
        }
    }

    fun encodeStateToJson(
        state: CredEdiUiState,
        savedAtEpochMillis: Long = System.currentTimeMillis()
    ): String {
        val root = JSONObject()
        root.put("schemaVersion", CURRENT_SCHEMA_VERSION)
        root.put("savedAtEpochMillis", savedAtEpochMillis)
        root.put("evaluationUuid", state.evaluationUuid)

        root.put(
            "patientIdentification",
            JSONObject().apply {
                put("patientIdentifierOrAlias", state.patientIdentification.patientIdentifierOrAlias)
                put("clinicalRecordCode", state.patientIdentification.clinicalRecordCode)
                put("generalClinicalNote", state.patientIdentification.generalClinicalNote)
            }
        )

        root.put(
            "anthropometryAndDemographics",
            JSONObject().apply {
                put("sexo", state.sexo.name)
                put("fechaNacimiento", state.fechaNacimiento)
                put("fechaEvaluacion", state.fechaEvaluacion)
                put("esPrematuro", state.esPrematuro)
                put("egSemanas", state.egSemanas)
                put("egDias", state.egDias)
                put("ediEgRegistrada", state.ediEgRegistrada)
                put("ediBirthMaturityCondition", state.ediBirthMaturityCondition.name)
                put("ediFechaEval", state.ediFechaEval)
                put("ediEgSemanasInput", state.ediEgSemanasInput)
                put("pesoInput", state.pesoInput)
                put("longitudInput", state.longitudInput)
                put("perimetroCefalicoInput", state.perimetroCefalicoInput)
                putNullableFiniteDouble("pesoKg", state.pesoKg)
                putNullableFiniteDouble("longitudCm", state.longitudCm)
                putNullableFiniteDouble("perimetroCefalicoCm", state.perimetroCefalicoCm)
            }
        )

        root.put(
            "navigationAndContext",
            JSONObject().apply {
                put("currentTab", state.currentTab.name)
                put("selectedIndicador", state.selectedIndicador.name)
                put("ediInternalView", state.ediInternalView.name)
                put("ediSelectedAxis", state.ediSelectedAxis.name)
                put("ediSelectedDevAreaFilter", state.ediSelectedDevAreaFilter?.name ?: JSONObject.NULL)
                put("ediGroupSearchQuery", state.ediGroupSearchQuery)
                put("ediGroupFilterCategory", state.ediGroupFilterCategory.name)
                put("ediManualGroupOverride", state.ediManualGroupOverride)
                put(
                    "ediAgeDiscrepancyResolution",
                    state.ediAgeDiscrepancyResolution?.let { encodeDiscrepancyResolution(it) } ?: JSONObject.NULL
                )
            }
        )

        root.put("ediEngineState", encodeEdiPhase1EngineState(state.ediEngineState))

        return root.toString()
    }

    @Throws(ClinicalSnapshotSerializationException::class)
    fun decodeStateFromJson(
        jsonString: String,
        expectedEvaluationUuid: String? = null
    ): CredEdiUiState {
        try {
            val root = JSONObject(jsonString)
            val schemaVersion = root.optInt("schemaVersion", -1)
            if (schemaVersion !in 1..CURRENT_SCHEMA_VERSION) {
                throw ClinicalSnapshotSerializationException(
                    "Versión de esquema clínico no soportada o ausente: $schemaVersion."
                )
            }
            val evaluationUuid = root.optString("evaluationUuid", "").trim()
            if (evaluationUuid.isEmpty()) {
                throw ClinicalSnapshotSerializationException("Snapshot clínico inválido: evaluationUuid vacío.")
            }
            if (expectedEvaluationUuid != null && expectedEvaluationUuid != evaluationUuid) {
                throw ClinicalSnapshotSerializationException(
                    "Discrepancia de identidad de evaluación: se esperaba '$expectedEvaluationUuid' pero el snapshot contiene '$evaluationUuid'."
                )
            }

            val patientObj = root.optJSONObject("patientIdentification")
            val patientIdentification = if (patientObj != null) {
                PatientIdentificationData(
                    patientIdentifierOrAlias = patientObj.optString("patientIdentifierOrAlias", ""),
                    clinicalRecordCode = patientObj.optString("clinicalRecordCode", ""),
                    generalClinicalNote = patientObj.optString("generalClinicalNote", "")
                )
            } else {
                PatientIdentificationData()
            }

            val antroObj = root.optJSONObject("anthropometryAndDemographics")
                ?: throw ClinicalSnapshotSerializationException("Snapshot clínico inválido: falta 'anthropometryAndDemographics'.")

            val sexo = parseEnumOrThrow<SexoBiologico>(
                antroObj.optString("sexo", ""),
                "anthropometryAndDemographics.sexo"
            )
            val fechaNacimiento = antroObj.optString("fechaNacimiento", "")
            val fechaEvaluacion = antroObj.optString("fechaEvaluacion", "")
            val esPrematuro = antroObj.optBoolean("esPrematuro", false)
            val egSemanas = antroObj.optInt("egSemanas", 32)
            val egDias = antroObj.optInt("egDias", 0)
            val ediEgRegistrada = antroObj.optBoolean("ediEgRegistrada", false)
            val ediBirthMaturityCondition = parseEnumOrThrow<EdiBirthMaturityCondition>(
                antroObj.optString("ediBirthMaturityCondition", EdiBirthMaturityCondition.UNSPECIFIED.name),
                "anthropometryAndDemographics.ediBirthMaturityCondition"
            )
            val ediFechaEval = antroObj.optString("ediFechaEval", fechaEvaluacion)
            val ediEgSemanasInput = antroObj.optString("ediEgSemanasInput", egSemanas.toString())

            val pesoInput = antroObj.optString("pesoInput", "")
            val longitudInput = antroObj.optString("longitudInput", "")
            val perimetroCefalicoInput = antroObj.optString("perimetroCefalicoInput", "")

            // MEDIO-05 PC/E-01 & ANTRO-02 Protection:
            // Si el campo input está vacío o el valor numérico fue serializado como null, se restaura estrictamente Double.NaN
            val rawPesoKg = optNullableFiniteDouble(antroObj, "pesoKg")
            val rawLongitudCm = optNullableFiniteDouble(antroObj, "longitudCm")
            val rawPerimetroCm = optNullableFiniteDouble(antroObj, "perimetroCefalicoCm")

            val pesoKg = if (pesoInput.trim().isEmpty()) Double.NaN else rawPesoKg
            val longitudCm = if (longitudInput.trim().isEmpty()) Double.NaN else rawLongitudCm
            val perimetroCefalicoCm = if (perimetroCefalicoInput.trim().isEmpty()) Double.NaN else rawPerimetroCm

            val navObj = root.optJSONObject("navigationAndContext") ?: JSONObject()
            val currentTab = parseEnumOrDefault(navObj.optString("currentTab", ""), AppTab.CALCULADORA)
            val selectedIndicador = parseEnumOrDefault(navObj.optString("selectedIndicador", ""), CurvaIndicador.PE)
            val ediInternalView = parseEnumOrDefault(navObj.optString("ediInternalView", ""), EdiInternalView.SUMMARY_5_AXES)
            val ediSelectedAxis = parseEnumOrDefault(navObj.optString("ediSelectedAxis", ""), EdiAxis.FRB)
            val devAreaFilterRaw = if (navObj.isNull("ediSelectedDevAreaFilter")) "" else navObj.optString("ediSelectedDevAreaFilter", "")
            val ediSelectedDevAreaFilter = if (devAreaFilterRaw.isBlank()) {
                null
            } else {
                parseEnumOrThrow<EdiDevArea>(devAreaFilterRaw, "navigationAndContext.ediSelectedDevAreaFilter")
            }
            val ediGroupSearchQuery = navObj.optString("ediGroupSearchQuery", "")
            val ediGroupFilterCategory = parseEnumOrDefault(
                navObj.optString("ediGroupFilterCategory", ""),
                EdiGroupFilterCategory.ALL
            )
            val ediManualGroupOverride = navObj.optBoolean("ediManualGroupOverride", false)
            val discrepancyObj = if (navObj.isNull("ediAgeDiscrepancyResolution")) null else navObj.optJSONObject("ediAgeDiscrepancyResolution")
            val ediAgeDiscrepancyResolution = discrepancyObj?.let { decodeDiscrepancyResolution(it) }

            val engineObj = root.optJSONObject("ediEngineState")
                ?: throw ClinicalSnapshotSerializationException("Snapshot clínico inválido: falta 'ediEngineState'.")
            val ediEngineState = decodeEdiPhase1EngineState(engineObj)

            return CredEdiUiState(
                evaluationUuid = evaluationUuid,
                patientIdentification = patientIdentification,
                currentTab = currentTab,
                sexo = sexo,
                fechaNacimiento = fechaNacimiento,
                fechaEvaluacion = fechaEvaluacion,
                esPrematuro = esPrematuro,
                egSemanas = egSemanas,
                egDias = egDias,
                pesoKg = pesoKg,
                longitudCm = longitudCm,
                perimetroCefalicoCm = perimetroCefalicoCm,
                pesoInput = pesoInput,
                longitudInput = longitudInput,
                perimetroCefalicoInput = perimetroCefalicoInput,
                selectedIndicador = selectedIndicador,
                ediInternalView = ediInternalView,
                ediSelectedAxis = ediSelectedAxis,
                ediSelectedDevAreaFilter = ediSelectedDevAreaFilter,
                ediGroupSearchQuery = ediGroupSearchQuery,
                ediGroupFilterCategory = ediGroupFilterCategory,
                ediEngineState = ediEngineState,
                ediAgeDiscrepancyResolution = ediAgeDiscrepancyResolution,
                ediManualGroupOverride = ediManualGroupOverride,
                ediEgRegistrada = ediEgRegistrada,
                ediBirthMaturityCondition = ediBirthMaturityCondition,
                ediFechaEval = ediFechaEval,
                ediEgSemanasInput = ediEgSemanasInput
            )
        } catch (e: ClinicalSnapshotSerializationException) {
            throw e
        } catch (e: Exception) {
            throw ClinicalSnapshotSerializationException("Error al deserializar snapshot clínico.", e)
        }
    }

    private fun JSONObject.putNullableFiniteDouble(key: String, value: Double) {
        if (value.isFinite()) {
            put(key, value)
        } else {
            put(key, JSONObject.NULL)
        }
    }

    private fun optNullableFiniteDouble(obj: JSONObject, key: String): Double {
        if (!obj.has(key) || obj.isNull(key)) return Double.NaN
        val raw = obj.optDouble(key, Double.NaN)
        return if (raw.isFinite()) raw else Double.NaN
    }

    private fun encodeDiscrepancyResolution(res: EdiAgeDiscrepancyResolution): JSONObject =
        JSONObject().apply {
            put("fechaNacimiento", res.fechaNacimiento)
            put("fechaEvaluacion", res.fechaEvaluacion)
            put("esPrematuro", res.esPrematuro)
            put("egSemanas", res.egSemanas)
            put("egDias", res.egDias)
            put("selectedGroupNumber", res.selectedGroupNumber)
            put("clinicalJustificationNote", res.clinicalJustificationNote)
        }

    private fun decodeDiscrepancyResolution(obj: JSONObject): EdiAgeDiscrepancyResolution {
        val groupNumber = obj.optInt("selectedGroupNumber", 1)
        if (groupNumber !in 1..15) {
            throw ClinicalSnapshotSerializationException("selectedGroupNumber fuera de rango (1..15): $groupNumber")
        }
        return EdiAgeDiscrepancyResolution(
            fechaNacimiento = obj.optString("fechaNacimiento", ""),
            fechaEvaluacion = obj.optString("fechaEvaluacion", ""),
            esPrematuro = obj.optBoolean("esPrematuro", false),
            egSemanas = obj.optInt("egSemanas", 32),
            egDias = obj.optInt("egDias", 0),
            selectedGroupNumber = groupNumber,
            clinicalJustificationNote = obj.optString("clinicalJustificationNote", "")
        )
    }

    private fun encodeEdiPhase1EngineState(engine: EdiPhase1EngineState): JSONObject =
        JSONObject().apply {
            put("activeGroupNumber", engine.activeGroupNumber)
            put("activeSession", encodeGroupSessionSnapshot(engine.activeSession))

            val archivedByGroupObj = JSONObject()
            engine.archivedSessionsByGroup.toSortedMap().forEach { (groupNum, snapshot) ->
                archivedByGroupObj.put(groupNum.toString(), encodeGroupSessionSnapshot(snapshot))
            }
            put("archivedSessionsByGroup", archivedByGroupObj)

            put(
                "pendingGroupChangeConfirmation",
                engine.pendingGroupChangeConfirmation?.let { pending ->
                    JSONObject().apply {
                        put("previousGroupNumber", pending.previousGroupNumber)
                        put("targetGroupNumber", pending.targetGroupNumber)
                        put("reason", pending.reason)
                        put(
                            "preservedSnapshotOfPreviousGroup",
                            encodeGroupSessionSnapshot(pending.preservedSnapshotOfPreviousGroup)
                        )
                    }
                } ?: JSONObject.NULL
            )

            val historyArray = JSONArray()
            engine.archivedSessionHistory.forEach { entry ->
                historyArray.put(encodeArchivedSessionEntry(entry))
            }
            put("archivedSessionHistory", historyArray)

            put("nextArchiveSequence", engine.nextArchiveSequence)
            put("currentWorkEpisodeId", engine.currentWorkEpisodeId)
            put("nextWorkEpisodeSequence", engine.nextWorkEpisodeSequence)
            put(
                "activeSessionOriginContext",
                engine.activeSessionOriginContext?.let { encodeArchivedClinicalContext(it) } ?: JSONObject.NULL
            )
            put("restoredFromArchiveId", engine.restoredFromArchiveId ?: JSONObject.NULL)
            put(
                "restoredSessionOriginContext",
                engine.restoredSessionOriginContext?.let { encodeArchivedClinicalContext(it) } ?: JSONObject.NULL
            )
            put("previewArchivedSessionId", engine.previewArchivedSessionId ?: JSONObject.NULL)
            put("lastRestoreStatusMessage", engine.lastRestoreStatusMessage ?: JSONObject.NULL)
        }

    private fun decodeEdiPhase1EngineState(obj: JSONObject): EdiPhase1EngineState {
        val activeGroupNumber = obj.optInt("activeGroupNumber", -1)
        if (activeGroupNumber !in 1..15) {
            throw ClinicalSnapshotSerializationException("activeGroupNumber inválido en ediEngineState: $activeGroupNumber")
        }
        val activeSessionObj = obj.optJSONObject("activeSession")
            ?: throw ClinicalSnapshotSerializationException("Falta activeSession en ediEngineState.")
        val activeSession = decodeGroupSessionSnapshot(activeSessionObj)

        val archivedSessionsByGroup = mutableMapOf<Int, EdiGroupSessionSnapshot>()
        val archivedByGroupObj = obj.optJSONObject("archivedSessionsByGroup")
        if (archivedByGroupObj != null) {
            val keys = archivedByGroupObj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val groupNum = key.toIntOrNull()
                    ?: throw ClinicalSnapshotSerializationException("Clave de grupo archivado inválida: '$key'")
                if (groupNum !in 1..15) {
                    throw ClinicalSnapshotSerializationException("Grupo archivado fuera de rango (1..15): $groupNum")
                }
                val snapObj = archivedByGroupObj.getJSONObject(key)
                archivedSessionsByGroup[groupNum] = decodeGroupSessionSnapshot(snapObj)
            }
        }

        val pendingGroupChangeObj = if (obj.isNull("pendingGroupChangeConfirmation")) {
            null
        } else {
            obj.optJSONObject("pendingGroupChangeConfirmation")
        }
        val pendingGroupChangeConfirmation = pendingGroupChangeObj?.let { pObj ->
            val prevGroup = pObj.optInt("previousGroupNumber", 1)
            val targetGroup = pObj.optInt("targetGroupNumber", 1)
            if (prevGroup !in 1..15 || targetGroup !in 1..15) {
                throw ClinicalSnapshotSerializationException("Grupos fuera de rango en pendingGroupChangeConfirmation.")
            }
            val snapObj = pObj.optJSONObject("preservedSnapshotOfPreviousGroup")
                ?: throw ClinicalSnapshotSerializationException("Falta preservedSnapshotOfPreviousGroup.")
            EdiPendingGroupChangeRequest(
                previousGroupNumber = prevGroup,
                targetGroupNumber = targetGroup,
                reason = pObj.optString("reason", ""),
                preservedSnapshotOfPreviousGroup = decodeGroupSessionSnapshot(snapObj)
            )
        }

        val archivedSessionHistory = mutableListOf<EdiArchivedSessionEntry>()
        val historyArray = obj.optJSONArray("archivedSessionHistory")
        if (historyArray != null) {
            for (i in 0 until historyArray.length()) {
                val entryObj = historyArray.getJSONObject(i)
                archivedSessionHistory.add(decodeArchivedSessionEntry(entryObj))
            }
        }

        val nextArchiveSequence = obj.optInt("nextArchiveSequence", (archivedSessionHistory.size + 1).coerceAtLeast(1))
        val currentWorkEpisodeId = obj.optString("currentWorkEpisodeId", "EP_1").ifBlank { "EP_1" }
        val nextWorkEpisodeSequence = obj.optInt("nextWorkEpisodeSequence", 2).coerceAtLeast(1)

        val activeOriginObj = if (obj.isNull("activeSessionOriginContext")) null else obj.optJSONObject("activeSessionOriginContext")
        val activeSessionOriginContext = activeOriginObj?.let { decodeArchivedClinicalContext(it) }

        val restoredFromArchiveId = if (obj.isNull("restoredFromArchiveId")) {
            null
        } else {
            obj.optString("restoredFromArchiveId", "").takeIf { it.isNotBlank() }
        }

        val restoredOriginObj = if (obj.isNull("restoredSessionOriginContext")) null else obj.optJSONObject("restoredSessionOriginContext")
        val restoredSessionOriginContext = restoredOriginObj?.let { decodeArchivedClinicalContext(it) }

        val previewArchivedSessionId = if (obj.isNull("previewArchivedSessionId")) {
            null
        } else {
            obj.optString("previewArchivedSessionId", "").takeIf { it.isNotBlank() }
        }

        val lastRestoreStatusMessage = if (obj.isNull("lastRestoreStatusMessage")) {
            null
        } else {
            obj.optString("lastRestoreStatusMessage", "").takeIf { it.isNotBlank() }
        }

        val baseInitial = EdiPhase1SessionManager.createInitialState(activeGroupNumber)
        return baseInitial.copy(
            activeGroupNumber = activeGroupNumber,
            activeSession = activeSession,
            archivedSessionsByGroup = archivedSessionsByGroup,
            pendingGroupChangeConfirmation = pendingGroupChangeConfirmation,
            archivedSessionHistory = archivedSessionHistory,
            nextArchiveSequence = nextArchiveSequence,
            currentWorkEpisodeId = currentWorkEpisodeId,
            nextWorkEpisodeSequence = nextWorkEpisodeSequence,
            activeSessionOriginContext = activeSessionOriginContext,
            restoredFromArchiveId = restoredFromArchiveId,
            restoredSessionOriginContext = restoredSessionOriginContext,
            previewArchivedSessionId = previewArchivedSessionId,
            pendingRestoreRequest = null,
            lastRestoreStatusMessage = lastRestoreStatusMessage
        )
    }

    private fun encodeGroupSessionSnapshot(snapshot: EdiGroupSessionSnapshot): JSONObject =
        JSONObject().apply {
            put("groupNumber", snapshot.groupNumber)
            put("currentGroupResponses", encodeResponseMap(snapshot.currentGroupResponses))
            put("fallbackGroupResponses", encodeResponseMap(snapshot.fallbackGroupResponses))
            put("frbResponses", encodeResponseMap(snapshot.frbResponses))
            put("enResponses", encodeResponseMap(snapshot.enResponses))

            val dualObj = JSONObject()
            snapshot.dualEvidenceRecords.toSortedMap().forEach { (itemId, record) ->
                dualObj.put(itemId, encodeDualEvidenceRecord(record))
            }
            put("dualEvidenceRecords", dualObj)

            val notesObj = JSONObject()
            snapshot.itemClinicalNotes.toSortedMap().forEach { (itemId, note) ->
                notesObj.put(itemId, note)
            }
            put("itemClinicalNotes", notesObj)
        }

    private fun decodeGroupSessionSnapshot(obj: JSONObject): EdiGroupSessionSnapshot {
        val groupNumber = obj.optInt("groupNumber", -1)
        if (groupNumber !in 1..15) {
            throw ClinicalSnapshotSerializationException("groupNumber fuera de rango (1..15) en EdiGroupSessionSnapshot: $groupNumber")
        }
        val currentGroupResponses = decodeResponseMap(obj.optJSONObject("currentGroupResponses"), "currentGroupResponses")
        val fallbackGroupResponses = decodeResponseMap(obj.optJSONObject("fallbackGroupResponses"), "fallbackGroupResponses")
        val frbResponses = decodeResponseMap(obj.optJSONObject("frbResponses"), "frbResponses")
        val enResponses = decodeResponseMap(obj.optJSONObject("enResponses"), "enResponses")

        val dualEvidenceRecords = mutableMapOf<String, EdiDualEvidenceRecord>()
        val dualObj = obj.optJSONObject("dualEvidenceRecords")
        if (dualObj != null) {
            val keys = dualObj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val recObj = dualObj.getJSONObject(key)
                dualEvidenceRecords[key] = decodeDualEvidenceRecord(recObj)
            }
        }

        val itemClinicalNotes = mutableMapOf<String, String>()
        val notesObj = obj.optJSONObject("itemClinicalNotes")
        if (notesObj != null) {
            val keys = notesObj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                itemClinicalNotes[key] = notesObj.optString(key, "")
            }
        }

        return EdiGroupSessionSnapshot(
            groupNumber = groupNumber,
            currentGroupResponses = currentGroupResponses,
            fallbackGroupResponses = fallbackGroupResponses,
            frbResponses = frbResponses,
            enResponses = enResponses,
            dualEvidenceRecords = dualEvidenceRecords,
            itemClinicalNotes = itemClinicalNotes
        )
    }

    private fun encodeResponseMap(map: Map<String, EdiItemResponse>): JSONObject {
        val obj = JSONObject()
        map.toSortedMap().forEach { (itemId, response) ->
            obj.put(itemId, response.name)
        }
        return obj
    }

    private fun decodeResponseMap(obj: JSONObject?, fieldName: String): Map<String, EdiItemResponse> {
        if (obj == null) return emptyMap()
        val result = mutableMapOf<String, EdiItemResponse>()
        val keys = obj.keys()
        while (keys.hasNext()) {
            val itemId = keys.next()
            val rawValue = obj.optString(itemId, "")
            val response = parseEnumOrThrow<EdiItemResponse>(rawValue, "$fieldName[$itemId]")
            result[itemId] = response
        }
        return result
    }

    private fun encodeDualEvidenceRecord(record: EdiDualEvidenceRecord): JSONObject =
        JSONObject().apply {
            put("itemUniqueId", record.itemUniqueId)
            put("modality", record.modality.name)
            put("directObservationEvidence", record.directObservationEvidence ?: JSONObject.NULL)
            put("directedInterviewEvidence", record.directedInterviewEvidence ?: JSONObject.NULL)
            put("professionalHumanResolution", record.professionalHumanResolution ?: JSONObject.NULL)
            put("clinicalJustificationNote", record.clinicalJustificationNote)
        }

    private fun decodeDualEvidenceRecord(obj: JSONObject): EdiDualEvidenceRecord {
        val itemUniqueId = obj.optString("itemUniqueId", "").trim()
        if (itemUniqueId.isEmpty()) {
            throw ClinicalSnapshotSerializationException("itemUniqueId vacío en EdiDualEvidenceRecord.")
        }
        val modality = parseEnumOrThrow<EdiClinicalModality>(
            obj.optString("modality", ""),
            "EdiDualEvidenceRecord.modality"
        )
        return EdiDualEvidenceRecord(
            itemUniqueId = itemUniqueId,
            modality = modality,
            directObservationEvidence = optNullableBoolean(obj, "directObservationEvidence"),
            directedInterviewEvidence = optNullableBoolean(obj, "directedInterviewEvidence"),
            professionalHumanResolution = optNullableBoolean(obj, "professionalHumanResolution"),
            clinicalJustificationNote = obj.optString("clinicalJustificationNote", "")
        )
    }

    private fun encodeArchivedSessionEntry(entry: EdiArchivedSessionEntry): JSONObject =
        JSONObject().apply {
            put("archiveId", entry.archiveId)
            put("sequenceNumber", entry.sequenceNumber)
            put("groupNumber", entry.groupNumber)
            put("snapshot", encodeGroupSessionSnapshot(entry.snapshot))
            put("archiveReason", entry.archiveReason)
            put("originContext", encodeArchivedClinicalContext(entry.originContext))
            put("compatibilityStatusAtArchive", entry.compatibilityStatusAtArchive)
        }

    private fun decodeArchivedSessionEntry(obj: JSONObject): EdiArchivedSessionEntry {
        val archiveId = obj.optString("archiveId", "").trim()
        if (archiveId.isEmpty()) {
            throw ClinicalSnapshotSerializationException("archiveId vacío en EdiArchivedSessionEntry.")
        }
        val groupNumber = obj.optInt("groupNumber", -1)
        if (groupNumber !in 1..15) {
            throw ClinicalSnapshotSerializationException("groupNumber inválido en EdiArchivedSessionEntry: $groupNumber")
        }
        val snapshotObj = obj.optJSONObject("snapshot")
            ?: throw ClinicalSnapshotSerializationException("Falta snapshot en EdiArchivedSessionEntry ($archiveId).")
        val originObj = obj.optJSONObject("originContext")
            ?: throw ClinicalSnapshotSerializationException("Falta originContext en EdiArchivedSessionEntry ($archiveId).")

        return EdiArchivedSessionEntry(
            archiveId = archiveId,
            sequenceNumber = obj.optInt("sequenceNumber", 1),
            groupNumber = groupNumber,
            snapshot = decodeGroupSessionSnapshot(snapshotObj),
            archiveReason = obj.optString("archiveReason", ""),
            originContext = decodeArchivedClinicalContext(originObj),
            compatibilityStatusAtArchive = obj.optString("compatibilityStatusAtArchive", "")
        )
    }

    private fun encodeArchivedClinicalContext(ctx: EdiArchivedClinicalContext): JSONObject =
        JSONObject().apply {
            put("workEpisodeId", ctx.workEpisodeId)
            put("originValidationState", ctx.originValidationState.name)
            put("sexoLabel", ctx.sexoLabel)
            put("fechaNacimiento", ctx.fechaNacimiento)
            put("fechaEvaluacion", ctx.fechaEvaluacion)
            put("esPrematuro", ctx.esPrematuro)
            put("egSemanas", ctx.egSemanas)
            put("egDias", ctx.egDias)
            put("ediEgRegistrada", ctx.ediEgRegistrada)
            put("ediBirthMaturityCondition", ctx.ediBirthMaturityCondition.name)
            put("effectiveAgeLabel", ctx.effectiveAgeLabel)
            put("gestationalAgeDisplayLabel", ctx.gestationalAgeDisplayLabel)
            put("applicableGroupNumberAtCapture", ctx.applicableGroupNumberAtCapture ?: JSONObject.NULL)
            put("validationSummaryAtCapture", ctx.validationSummaryAtCapture)
        }

    private fun decodeArchivedClinicalContext(obj: JSONObject): EdiArchivedClinicalContext {
        val originValidationState = parseEnumOrThrow<EdiArchivedOriginValidationState>(
            obj.optString("originValidationState", EdiArchivedOriginValidationState.UNVERIFIED.name),
            "EdiArchivedClinicalContext.originValidationState"
        )
        val birthMaturityCondition = parseEnumOrThrow<EdiBirthMaturityCondition>(
            obj.optString("ediBirthMaturityCondition", EdiBirthMaturityCondition.UNSPECIFIED.name),
            "EdiArchivedClinicalContext.ediBirthMaturityCondition"
        )
        val applicableGroup = if (!obj.has("applicableGroupNumberAtCapture") || obj.isNull("applicableGroupNumberAtCapture")) {
            null
        } else {
            obj.optInt("applicableGroupNumberAtCapture", -1).takeIf { it in 1..15 }
        }
        return EdiArchivedClinicalContext(
            workEpisodeId = obj.optString("workEpisodeId", "EP_1"),
            originValidationState = originValidationState,
            sexoLabel = obj.optString("sexoLabel", "Niño"),
            fechaNacimiento = obj.optString("fechaNacimiento", ""),
            fechaEvaluacion = obj.optString("fechaEvaluacion", ""),
            esPrematuro = obj.optBoolean("esPrematuro", false),
            egSemanas = obj.optInt("egSemanas", 32),
            egDias = obj.optInt("egDias", 0),
            ediEgRegistrada = obj.optBoolean("ediEgRegistrada", false),
            ediBirthMaturityCondition = birthMaturityCondition,
            effectiveAgeLabel = obj.optString("effectiveAgeLabel", "EDAD NO VERIFICADA"),
            gestationalAgeDisplayLabel = obj.optString("gestationalAgeDisplayLabel", "EG no registrada"),
            applicableGroupNumberAtCapture = applicableGroup,
            validationSummaryAtCapture = obj.optString("validationSummaryAtCapture", "NO VERIFICADO")
        )
    }

    private fun optNullableBoolean(obj: JSONObject, key: String): Boolean? {
        if (!obj.has(key) || obj.isNull(key)) return null
        return obj.optBoolean(key)
    }

    private inline fun <reified T : Enum<T>> parseEnumOrThrow(raw: String, fieldName: String): T {
        val trimmed = raw.trim()
        return enumValues<T>().firstOrNull { it.name == trimmed }
            ?: throw ClinicalSnapshotSerializationException(
                "Valor inválido '$raw' para el campo '$fieldName'. Valores permitidos: ${enumValues<T>().joinToString { it.name }}"
            )
    }

    private inline fun <reified T : Enum<T>> parseEnumOrDefault(raw: String, default: T): T {
        val trimmed = raw.trim()
        return enumValues<T>().firstOrNull { it.name == trimmed } ?: default
    }
}
