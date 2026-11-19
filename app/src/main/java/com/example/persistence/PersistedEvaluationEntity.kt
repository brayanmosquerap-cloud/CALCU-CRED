package com.example.persistence

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.model.CredEdiUiState
import com.example.model.EdiBirthMaturityCondition
import com.example.model.EvaluationPersistenceStatus
import com.example.model.PatientIdentificationData
import com.example.model.SexoBiologico

/**
 * MEDIO-06 — LOTE 1:
 * Entidad Room que almacena una evaluación clínica individual identificada por [evaluationUuid].
 *
 * - Los datos personales/identificatorios del paciente ([encryptedPatientHeader]) y el snapshot
 *   clínico completo con todas las respuestas EDI, notas y antropometría ([encryptedClinicalPayload])
 *   se almacenan cifrados mediante `AES-256-GCM` autenticando `evaluationUuid` como AAD.
 * - [plaintextSha256] permite verificar tras el descifrado que el JSON clínico restaurado es
 *   idéntico bit a bit al que fue guardado.
 * - [monotonicRevision] garantiza que ninguna operación asíncrona atrasada sobrescriba una
 *   revisión más reciente.
 */
@Entity(
    tableName = "persisted_evaluations",
    indices = [
        Index(value = ["is_active_draft"]),
        Index(value = ["status"]),
        Index(value = ["updated_at_epoch_millis"])
    ]
)
data class PersistedEvaluationEntity(
    @PrimaryKey
    @ColumnInfo(name = "evaluation_uuid")
    val evaluationUuid: String,

    @ColumnInfo(name = "schema_version")
    val schemaVersion: Int,

    @ColumnInfo(name = "status")
    val status: String,

    @ColumnInfo(name = "is_active_draft")
    val isActiveDraft: Boolean,

    @ColumnInfo(name = "active_group_number")
    val activeGroupNumber: Int,

    @ColumnInfo(name = "semaphore_code")
    val semaphoreCode: String,

    @ColumnInfo(name = "is_definitive_semaphore")
    val isDefinitiveSemaphore: Boolean,

    @ColumnInfo(name = "answered_mandatory_count")
    val answeredMandatoryCount: Int,

    @ColumnInfo(name = "total_mandatory_count")
    val totalMandatoryCount: Int,

    @ColumnInfo(name = "has_priority_critical_warning")
    val hasPriorityCriticalWarning: Boolean,

    @ColumnInfo(name = "created_at_epoch_millis")
    val createdAtEpochMillis: Long,

    @ColumnInfo(name = "updated_at_epoch_millis")
    val updatedAtEpochMillis: Long,

    @ColumnInfo(name = "monotonic_revision")
    val monotonicRevision: Long,

    @ColumnInfo(name = "encrypted_patient_header")
    val encryptedPatientHeader: String,

    @ColumnInfo(name = "encrypted_clinical_payload")
    val encryptedClinicalPayload: String,

    @ColumnInfo(name = "plaintext_sha256")
    val plaintextSha256: String
)

/**
 * Resumen descifrado de una evaluación almacenada en Room para el apartado
 * «Guardado e Historial de Evaluaciones».
 */
data class SavedEvaluationSummary(
    val evaluationUuid: String,
    val status: EvaluationPersistenceStatus,
    val isActiveDraft: Boolean,
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
    val activeGroupNumber: Int,
    val activeGroupCode: String,
    val activeGroupTitle: String,
    val semaphoreCode: String,
    val semaphoreLabel: String,
    val isDefinitiveSemaphore: Boolean,
    val answeredMandatoryCount: Int,
    val totalMandatoryCount: Int,
    val hasPriorityCriticalWarning: Boolean,
    val anthropometrySummaryLabel: String,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    val monotonicRevision: Long
)

/**
 * Registro completo restaurado y verificado desde Room.
 */
data class RestoredEvaluationRecord(
    val summary: SavedEvaluationSummary,
    val restoredUiState: CredEdiUiState,
    val plaintextSha256: String
)

/**
 * Resultado verificable de una operación de guardado transaccional.
 * Nunca reporta [Success] si el cifrado, la escritura en Room o la verificación de lectura fallan.
 */
sealed class SaveEvaluationResult {
    data class Success(
        val evaluationUuid: String,
        val persistedStatus: EvaluationPersistenceStatus,
        val isActiveDraft: Boolean,
        val savedAtEpochMillis: Long,
        val monotonicRevision: Long,
        val plaintextSha256: String
    ) : SaveEvaluationResult()

    data class Failure(
        val evaluationUuid: String,
        val reason: String,
        val cause: Throwable? = null
    ) : SaveEvaluationResult()
}

/**
 * Resultado verificable de recuperación/restauración de una evaluación.
 */
sealed class LoadEvaluationResult {
    data class Success(val record: RestoredEvaluationRecord) : LoadEvaluationResult()
    data object NotFound : LoadEvaluationResult()
    data class CorruptedOrUnreadable(
        val evaluationUuid: String?,
        val reason: String,
        val cause: Throwable? = null
    ) : LoadEvaluationResult()
}
