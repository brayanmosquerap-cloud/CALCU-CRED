package com.example.persistence

import com.example.model.CredEdiUiState
import com.example.model.EvaluationPersistenceStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.nio.charset.StandardCharsets
import java.util.UUID

/**
 * MEDIO-06 — LOTE 1:
 * Repositorio transaccional de persistencia clínica con cifrado autenticado `AES-256-GCM`
 * y verificación criptográfica `SHA-256`.
 *
 * Reglas de seguridad clínica implementadas:
 * 1. Serialización de escrituras concurrentes mediante [writeMutex] y revisión monotónica
 *    por evaluación (`monotonicRevision`) para impedir condiciones de carrera.
 * 2. Prohibición absoluta de promover una evaluación incompleta a `COMPLETED` o semáforo definitivo:
 *    si `state.ediGlobalEvaluation.isDefinitive == false`, el estado persistido nunca será `COMPLETED`.
 * 3. Confirmación real post-escritura: `saveEvaluation` verifica que la entidad escrita pueda leerse
 *    desde el DAO con el mismo `plaintextSha256` y `monotonicRevision` antes de emitir `SaveEvaluationResult.Success`.
 * 4. Separación estricta de múltiples pacientes por `evaluationUuid` usando el UUID como AAD
 *    (Additional Authenticated Data) en `AES-256-GCM`, de modo que un payload cifrado para el
 *    Paciente A falla criptográficamente si se intenta asociar al UUID del Paciente B.
 */
class CredEdiPersistenceRepository(
    private val evaluationDao: EvaluationDao,
    private val cipherProvider: CredEdiCipherProvider = AndroidKeyStoreAesGcmCipherProvider(),
    private val clockEpochMillis: () -> Long = { System.currentTimeMillis() }
) {
    private val writeMutex = Mutex()

    fun generateNewEvaluationUuid(): String = "EVAL_${UUID.randomUUID()}"

    /**
     * Determina el estado canónico de persistencia respetando la completitud clínica real:
     * - Jamás permite `COMPLETED` si `ediGlobalEvaluation.isDefinitive` es `false`.
     */
    fun resolveCanonicalPersistenceStatus(
        state: CredEdiUiState,
        requestedStatus: EvaluationPersistenceStatus? = null
    ): EvaluationPersistenceStatus {
        val globalEval = state.ediGlobalEvaluation
        if (requestedStatus == EvaluationPersistenceStatus.ARCHIVED) {
            return EvaluationPersistenceStatus.ARCHIVED
        }
        return if (globalEval.isDefinitive) {
            when (requestedStatus) {
                EvaluationPersistenceStatus.PENDING -> EvaluationPersistenceStatus.PENDING
                else -> EvaluationPersistenceStatus.COMPLETED
            }
        } else {
            when (requestedStatus) {
                EvaluationPersistenceStatus.PENDING -> EvaluationPersistenceStatus.PENDING
                // Aunque se solicite COMPLETED por error, si no es definitiva se degrada a INCOMPLETE
                EvaluationPersistenceStatus.COMPLETED -> EvaluationPersistenceStatus.INCOMPLETE
                else -> EvaluationPersistenceStatus.INCOMPLETE
            }
        }
    }

    /**
     * Guarda de manera atómica y cifrada el estado completo de una evaluación.
     *
     * @param state Estado completo `CredEdiUiState` a persistir.
     * @param requestedStatus Estado solicitado (`null` para deducir automáticamente según completitud).
     * @param markAsActiveDraft Si es `true`, esta evaluación queda marcada como el único borrador activo
     *   para recuperación automática tras reinicio del proceso.
     * @param explicitRevision Revisión monotónica opcional para pruebas de concurrencia/ordenamiento.
     */
    suspend fun saveEvaluation(
        state: CredEdiUiState,
        requestedStatus: EvaluationPersistenceStatus? = null,
        markAsActiveDraft: Boolean = true,
        explicitRevision: Long? = null
    ): SaveEvaluationResult = writeMutex.withLock {
        val uuid = state.evaluationUuid.trim()
        if (uuid.isEmpty()) {
            return@withLock SaveEvaluationResult.Failure(
                evaluationUuid = "",
                reason = "No se puede guardar una evaluación con evaluationUuid vacío."
            )
        }

        return@withLock try {
            val now = clockEpochMillis()
            val existing = evaluationDao.getByUuid(uuid)
            val targetRevision = explicitRevision ?: ((existing?.monotonicRevision ?: 0L) + 1L)
            if (existing != null && targetRevision < existing.monotonicRevision) {
                return@withLock SaveEvaluationResult.Failure(
                    evaluationUuid = uuid,
                    reason = "Escritura descartada por revisión obsoleta ($targetRevision < ${existing.monotonicRevision})."
                )
            }

            val createdAt = existing?.createdAtEpochMillis ?: now
            val updatedAt = maxOf(now, existing?.updatedAtEpochMillis ?: now)
            val resolvedStatus = resolveCanonicalPersistenceStatus(state, requestedStatus)
            val globalEval = state.ediGlobalEvaluation
            val (answeredMandatory, totalMandatory) = state.ediMandatoryItemsProgress

            val headerJson = CredEdiClinicalSnapshotCodec.encodePatientHeaderToJson(state)
            val payloadJson = CredEdiClinicalSnapshotCodec.encodeStateToJson(
                state = state,
                savedAtEpochMillis = updatedAt
            )
            val plaintextSha256 = CredEdiCryptoManager.sha256Hex(payloadJson)
            val aadBytes = uuid.toByteArray(StandardCharsets.UTF_8)

            val encryptedHeader = cipherProvider.encrypt(
                plaintext = headerJson.toByteArray(StandardCharsets.UTF_8),
                associatedData = aadBytes
            )
            val encryptedPayload = cipherProvider.encrypt(
                plaintext = payloadJson.toByteArray(StandardCharsets.UTF_8),
                associatedData = aadBytes
            )

            val entity = PersistedEvaluationEntity(
                evaluationUuid = uuid,
                schemaVersion = CredEdiClinicalSnapshotCodec.CURRENT_SCHEMA_VERSION,
                status = resolvedStatus.name,
                isActiveDraft = markAsActiveDraft,
                activeGroupNumber = state.ediEngineState.activeGroupNumber,
                semaphoreCode = globalEval.trafficLight.name,
                isDefinitiveSemaphore = globalEval.isDefinitive,
                answeredMandatoryCount = answeredMandatory,
                totalMandatoryCount = totalMandatory,
                hasPriorityCriticalWarning = globalEval.hasPriorityCriticalWarning,
                createdAtEpochMillis = createdAt,
                updatedAtEpochMillis = updatedAt,
                monotonicRevision = targetRevision,
                encryptedPatientHeader = encryptedHeader,
                encryptedClinicalPayload = encryptedPayload,
                plaintextSha256 = plaintextSha256
            )

            val persistedOk = evaluationDao.upsertAtomicallyWithRevisionGuard(entity)
            if (!persistedOk) {
                return@withLock SaveEvaluationResult.Failure(
                    evaluationUuid = uuid,
                    reason = "La transacción SQLite rechazó la escritura (revisión monotónica superada o error de inserción)."
                )
            }

            val verifiedEntity = evaluationDao.getByUuid(uuid)
            if (verifiedEntity == null ||
                verifiedEntity.plaintextSha256 != plaintextSha256 ||
                verifiedEntity.monotonicRevision != targetRevision
            ) {
                return@withLock SaveEvaluationResult.Failure(
                    evaluationUuid = uuid,
                    reason = "Fallo de verificación post-escritura en Room para la evaluación $uuid."
                )
            }

            SaveEvaluationResult.Success(
                evaluationUuid = uuid,
                persistedStatus = resolvedStatus,
                isActiveDraft = markAsActiveDraft,
                savedAtEpochMillis = updatedAt,
                monotonicRevision = targetRevision,
                plaintextSha256 = plaintextSha256
            )
        } catch (t: Throwable) {
            SaveEvaluationResult.Failure(
                evaluationUuid = uuid,
                reason = "Error al cifrar o persistir la evaluación en Room: ${t.message ?: t::class.simpleName}",
                cause = t
            )
        }
    }

    /**
     * Recupera y descifra una evaluación específica por su [evaluationUuid],
     * verificando su etiqueta de autenticación GCM, su AAD y su hash `SHA-256`.
     */
    suspend fun loadEvaluationByUuid(evaluationUuid: String): LoadEvaluationResult {
        val cleanUuid = evaluationUuid.trim()
        if (cleanUuid.isEmpty()) {
            return LoadEvaluationResult.NotFound
        }
        val entity = try {
            evaluationDao.getByUuid(cleanUuid)
        } catch (t: Throwable) {
            return LoadEvaluationResult.CorruptedOrUnreadable(
                evaluationUuid = cleanUuid,
                reason = "Error de acceso a Room al consultar $cleanUuid: ${t.message}",
                cause = t
            )
        } ?: return LoadEvaluationResult.NotFound

        return decryptAndValidateEntity(entity)
    }

    /**
     * Recupera el borrador activo (`is_active_draft = 1`) para restauración tras reinicio del proceso.
     */
    suspend fun loadActiveDraft(): LoadEvaluationResult {
        val entity = try {
            evaluationDao.getActiveDraft()
        } catch (t: Throwable) {
            return LoadEvaluationResult.CorruptedOrUnreadable(
                evaluationUuid = null,
                reason = "Error de acceso a Room al consultar el borrador activo: ${t.message}",
                cause = t
            )
        } ?: return LoadEvaluationResult.NotFound

        return decryptAndValidateEntity(entity)
    }

    /**
     * Lista todos los resúmenes de evaluaciones guardadas ordenados por fecha de actualización descendente.
     * Las filas corruptas o ilegibles se omiten del listado activo sin provocar cierre inesperado (`crash`).
     */
    suspend fun listSavedEvaluationSummaries(): List<SavedEvaluationSummary> {
        val entities = try {
            evaluationDao.listAllOrderedByUpdatedDesc()
        } catch (_: Throwable) {
            emptyList()
        }
        return entities.mapNotNull { entity ->
            try {
                decryptSummaryOnly(entity)
            } catch (_: Throwable) {
                null
            }
        }
    }

    /**
     * Flujo reactivo de resúmenes de evaluaciones guardadas para observar cambios desde la UI.
     */
    fun observeSavedEvaluationSummaries(): Flow<List<SavedEvaluationSummary>> {
        return evaluationDao.observeAllOrderedByUpdatedDesc().map { entities ->
            entities.mapNotNull { entity ->
                try {
                    decryptSummaryOnly(entity)
                } catch (_: Throwable) {
                    null
                }
            }
        }
    }

    /**
     * Elimina una evaluación individual por su [evaluationUuid].
     */
    suspend fun deleteEvaluation(evaluationUuid: String): Boolean = writeMutex.withLock {
        val cleanUuid = evaluationUuid.trim()
        if (cleanUuid.isEmpty()) return@withLock false
        return@withLock try {
            evaluationDao.deleteByUuid(cleanUuid) > 0
        } catch (_: Throwable) {
            false
        }
    }

    /**
     * Selecciona de forma atómica qué evaluación almacenada queda marcada como borrador activo
     * sin alterar su contenido clínico ni su marca de tiempo de guardado.
     */
    suspend fun selectActiveDraft(evaluationUuid: String): Boolean = writeMutex.withLock {
        val cleanUuid = evaluationUuid.trim()
        if (cleanUuid.isEmpty()) return@withLock false
        return@withLock try {
            evaluationDao.selectExclusiveActiveDraftPreservingMetadata(cleanUuid)
        } catch (_: Throwable) {
            false
        }
    }

    /**
     * Desactiva la marca `is_active_draft` de todas las evaluaciones almacenadas (p. ej. al iniciar
     * una nueva evaluación limpia aún sin cambios o al abrir una evaluación completada en modo solo lectura).
     */
    suspend fun deactivateAllActiveDrafts(): Boolean = writeMutex.withLock {
        return@withLock try {
            evaluationDao.clearAllActiveDraftFlags()
            true
        } catch (_: Throwable) {
            false
        }
    }

    /**
     * Limpia de forma segura todo el historial de evaluaciones locales.
     */
    suspend fun clearAllEvaluationsSecurely(): Boolean = writeMutex.withLock {
        return@withLock try {
            evaluationDao.deleteAll()
            evaluationDao.countAll() == 0
        } catch (_: Throwable) {
            false
        }
    }

    private fun decryptAndValidateEntity(entity: PersistedEvaluationEntity): LoadEvaluationResult {
        val uuid = entity.evaluationUuid
        val aadBytes = uuid.toByteArray(StandardCharsets.UTF_8)
        return try {
            val summary = decryptSummaryOnly(entity)
            val decryptedPayloadBytes = cipherProvider.decrypt(
                ciphertextPayload = entity.encryptedClinicalPayload,
                associatedData = aadBytes
            )
            val payloadJson = String(decryptedPayloadBytes, StandardCharsets.UTF_8)
            val actualSha256 = CredEdiCryptoManager.sha256Hex(payloadJson)
            if (!actualSha256.equals(entity.plaintextSha256, ignoreCase = true)) {
                return LoadEvaluationResult.CorruptedOrUnreadable(
                    evaluationUuid = uuid,
                    reason = "Discrepancia de integridad SHA-256 en el snapshot clínico de $uuid."
                )
            }
            val restoredState = CredEdiClinicalSnapshotCodec.decodeStateFromJson(
                jsonString = payloadJson,
                expectedEvaluationUuid = uuid
            )
            LoadEvaluationResult.Success(
                RestoredEvaluationRecord(
                    summary = summary,
                    restoredUiState = restoredState,
                    plaintextSha256 = actualSha256
                )
            )
        } catch (t: Throwable) {
            LoadEvaluationResult.CorruptedOrUnreadable(
                evaluationUuid = uuid,
                reason = "No fue posible descifrar o validar la evaluación $uuid: ${t.message ?: t::class.simpleName}",
                cause = t
            )
        }
    }

    private fun decryptSummaryOnly(entity: PersistedEvaluationEntity): SavedEvaluationSummary {
        val uuid = entity.evaluationUuid
        val aadBytes = uuid.toByteArray(StandardCharsets.UTF_8)
        val decryptedHeaderBytes = cipherProvider.decrypt(
            ciphertextPayload = entity.encryptedPatientHeader,
            associatedData = aadBytes
        )
        val headerJson = String(decryptedHeaderBytes, StandardCharsets.UTF_8)
        val decodedHeader = CredEdiClinicalSnapshotCodec.decodePatientHeaderFromJson(headerJson)
        if (decodedHeader.evaluationUuid != uuid) {
            throw ClinicalSnapshotSerializationException(
                "Discrepancia de evaluationUuid en encabezado descifrado: '${decodedHeader.evaluationUuid}' != '$uuid'"
            )
        }
        val parsedStatus = EvaluationPersistenceStatus.entries.firstOrNull {
            it.name == entity.status
        } ?: EvaluationPersistenceStatus.INCOMPLETE

        return SavedEvaluationSummary(
            evaluationUuid = uuid,
            status = parsedStatus,
            isActiveDraft = entity.isActiveDraft,
            patientIdentification = decodedHeader.patientIdentification,
            sexo = decodedHeader.sexo,
            fechaNacimiento = decodedHeader.fechaNacimiento,
            fechaEvaluacion = decodedHeader.fechaEvaluacion,
            esPrematuro = decodedHeader.esPrematuro,
            egSemanas = decodedHeader.egSemanas,
            egDias = decodedHeader.egDias,
            ediEgRegistrada = decodedHeader.ediEgRegistrada,
            ediBirthMaturityCondition = decodedHeader.ediBirthMaturityCondition,
            effectiveAgeLabel = decodedHeader.effectiveAgeLabel,
            activeGroupNumber = entity.activeGroupNumber,
            activeGroupCode = decodedHeader.activeGroupCode,
            activeGroupTitle = decodedHeader.activeGroupTitle,
            semaphoreCode = entity.semaphoreCode,
            semaphoreLabel = decodedHeader.semaphoreLabel,
            isDefinitiveSemaphore = entity.isDefinitiveSemaphore,
            answeredMandatoryCount = entity.answeredMandatoryCount,
            totalMandatoryCount = entity.totalMandatoryCount,
            hasPriorityCriticalWarning = entity.hasPriorityCriticalWarning,
            anthropometrySummaryLabel = decodedHeader.anthropometrySummaryLabel,
            createdAtEpochMillis = entity.createdAtEpochMillis,
            updatedAtEpochMillis = entity.updatedAtEpochMillis,
            monotonicRevision = entity.monotonicRevision
        )
    }
}
