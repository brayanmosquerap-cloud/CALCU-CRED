package com.example.persistence

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/**
 * MEDIO-06 — LOTE 1:
 * DAO Room con garantías transaccionales para persistencia de evaluaciones clínicas.
 */
@Dao
interface EvaluationDao {

    @Query("SELECT * FROM persisted_evaluations WHERE evaluation_uuid = :evaluationUuid LIMIT 1")
    suspend fun getByUuid(evaluationUuid: String): PersistedEvaluationEntity?

    @Query("SELECT * FROM persisted_evaluations WHERE is_active_draft = 1 ORDER BY updated_at_epoch_millis DESC LIMIT 1")
    suspend fun getActiveDraft(): PersistedEvaluationEntity?

    @Query("SELECT * FROM persisted_evaluations ORDER BY updated_at_epoch_millis DESC, monotonic_revision DESC")
    suspend fun listAllOrderedByUpdatedDesc(): List<PersistedEvaluationEntity>

    @Query("SELECT * FROM persisted_evaluations ORDER BY updated_at_epoch_millis DESC, monotonic_revision DESC")
    fun observeAllOrderedByUpdatedDesc(): Flow<List<PersistedEvaluationEntity>>

    @Query("SELECT COUNT(*) FROM persisted_evaluations")
    suspend fun countAll(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplaceInternal(entity: PersistedEvaluationEntity): Long

    @Query("UPDATE persisted_evaluations SET is_active_draft = 0 WHERE is_active_draft = 1 AND evaluation_uuid != :exceptUuid")
    suspend fun clearActiveDraftExcept(exceptUuid: String)

    @Query("UPDATE persisted_evaluations SET is_active_draft = 0 WHERE is_active_draft = 1")
    suspend fun clearAllActiveDraftFlags()

    @Query("DELETE FROM persisted_evaluations WHERE evaluation_uuid = :evaluationUuid")
    suspend fun deleteByUuid(evaluationUuid: String): Int

    @Query("DELETE FROM persisted_evaluations")
    suspend fun deleteAll(): Int

    /**
     * Guarda una evaluación de forma atómica verificando la revisión monotónica:
     * - Si ya existe una fila para [entity.evaluationUuid] con `monotonicRevision > entity.monotonicRevision`,
     *   rechaza la escritura atrasada y devuelve `false` para impedir que una tarea fuera de orden
     *   sobrescriba datos más recientes.
     * - Si [entity.isActiveDraft] es `true`, desactiva atómicamente la marca `is_active_draft` de
     *   cualquier otra evaluación en la misma transacción SQLite.
     */
    @Transaction
    suspend fun upsertAtomicallyWithRevisionGuard(entity: PersistedEvaluationEntity): Boolean {
        val existing = getByUuid(entity.evaluationUuid)
        if (existing != null && existing.monotonicRevision > entity.monotonicRevision) {
            return false
        }
        if (entity.isActiveDraft) {
            clearActiveDraftExcept(entity.evaluationUuid)
        }
        val rowId = insertOrReplaceInternal(entity)
        return rowId != -1L
    }

    /**
     * Marca una evaluación existente como el único borrador activo de forma atómica.
     */
    @Transaction
    suspend fun setExclusiveActiveDraft(evaluationUuid: String, updatedAtEpochMillis: Long): Boolean {
        val existing = getByUuid(evaluationUuid) ?: return false
        clearActiveDraftExcept(evaluationUuid)
        val updated = existing.copy(
            isActiveDraft = true,
            updatedAtEpochMillis = maxOf(updatedAtEpochMillis, existing.updatedAtEpochMillis),
            monotonicRevision = existing.monotonicRevision + 1L
        )
        return insertOrReplaceInternal(updated) != -1L
    }

    @Query("UPDATE persisted_evaluations SET is_active_draft = 1 WHERE evaluation_uuid = :evaluationUuid")
    suspend fun markSingleActiveDraftInternal(evaluationUuid: String): Int

    /**
     * Selecciona una evaluación existente como el borrador activo sin alterar su revisión monotónica
     * ni su marca de tiempo de último guardado clínico.
     */
    @Transaction
    suspend fun selectExclusiveActiveDraftPreservingMetadata(evaluationUuid: String): Boolean {
        getByUuid(evaluationUuid) ?: return false
        clearActiveDraftExcept(evaluationUuid)
        return markSingleActiveDraftInternal(evaluationUuid) > 0
    }
}
