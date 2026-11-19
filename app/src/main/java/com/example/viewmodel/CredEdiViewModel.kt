package com.example.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.model.AppTab
import com.example.model.AutosaveUiIndicatorState
import com.example.model.ClinicalAgeCalculator
import com.example.model.CredEdiUiState
import com.example.model.CurvaIndicador
import com.example.model.EdiAgeDiscrepancyResolution
import com.example.model.EdiAreaFallbackEngine
import com.example.model.EdiAxis
import com.example.model.EdiBirthMaturityCondition
import com.example.model.EdiDevArea
import com.example.model.EdiGroupAutoSelectionStatus
import com.example.model.EdiGroupChangeResolutionAction
import com.example.model.EdiGroupFilterCategory
import com.example.model.EdiInternalView
import com.example.model.EdiItemResponse
import com.example.model.EdiOfficialCatalog
import com.example.model.EdiPhase1SessionManager
import com.example.model.NewEvaluationCreationResult
import com.example.model.PatientIdentificationData
import com.example.model.SemaforoColor
import com.example.model.SexoBiologico
import com.example.persistence.AndroidKeyStoreAesGcmCipherProvider
import com.example.persistence.CredEdiDatabase
import com.example.persistence.CredEdiPersistenceRepository
import com.example.persistence.LoadEvaluationResult
import com.example.persistence.SaveEvaluationResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.roundToInt

class CredEdiViewModel(
    private val persistenceRepository: CredEdiPersistenceRepository? = null,
    private val autoRestoreOnInit: Boolean = true,
    private val autosaveDebounceMs: Long = DEFAULT_AUTOSAVE_DEBOUNCE_MS,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val _uiState = MutableStateFlow(CredEdiUiState())
    val uiState: StateFlow<CredEdiUiState> = _uiState.asStateFlow()

    private val clinicalMutationCounter = AtomicLong(0L)
    private val saveExecutionMutex = Mutex()
    private var autosaveDebounceJob: Job? = null

    init {
        if (persistenceRepository != null) {
            if (autoRestoreOnInit) {
                restoreActiveDraftOnStartup(persistenceRepository)
            } else {
                val initialHistory = try {
                    runBlocking(ioDispatcher) {
                        persistenceRepository.listSavedEvaluationSummaries()
                    }
                } catch (_: Throwable) {
                    emptyList()
                }
                _uiState.update { state ->
                    state.copy(
                        evaluationUuid = if (state.evaluationUuid == "EVAL_DEFAULT_SESSION") {
                            persistenceRepository.generateNewEvaluationUuid()
                        } else {
                            state.evaluationUuid
                        },
                        savedEvaluationHistory = initialHistory
                    )
                }
            }
        }
    }

    /**
     * MEDIO-06 — LOTE 2 & LOTE 3:
     * Recupera automáticamente el borrador activo al iniciar el aplicativo/ViewModel y sincroniza
     * la lista del historial de evaluaciones almacenadas en Room.
     */
    private fun restoreActiveDraftOnStartup(repo: CredEdiPersistenceRepository) {
        val (loadResult, initialHistory) = try {
            runBlocking(ioDispatcher) {
                val draft = repo.loadActiveDraft()
                val history = repo.listSavedEvaluationSummaries()
                draft to history
            }
        } catch (t: Throwable) {
            LoadEvaluationResult.CorruptedOrUnreadable(
                evaluationUuid = null,
                reason = "Fallo al leer el almacenamiento local: ${t.message ?: t::class.simpleName}",
                cause = t
            ) to emptyList()
        }

        when (loadResult) {
            is LoadEvaluationResult.Success -> {
                val record = loadResult.record
                val restoredSynced = syncEdiTemplateQuestions(record.restoredUiState)
                clinicalMutationCounter.set(record.summary.monotonicRevision)
                _uiState.value = restoredSynced.copy(
                    persistenceIndicatorState = AutosaveUiIndicatorState.SAVED,
                    persistenceStatusMessage = "Evaluación recuperada",
                    lastSavedAtEpochMillis = record.summary.updatedAtEpochMillis,
                    lastPersistedRevision = record.summary.monotonicRevision,
                    wasRestoredFromPersistence = true,
                    persistenceRecoveryErrorMessage = null,
                    savedEvaluationHistory = initialHistory,
                    isReadOnlyConsultationMode = false,
                    readOnlyEditableUnlockTrail = null,
                    pendingDeleteEvaluationSummary = null
                )
            }

            is LoadEvaluationResult.CorruptedOrUnreadable -> {
                val freshUuid = repo.generateNewEvaluationUuid()
                clinicalMutationCounter.set(0L)
                _uiState.update { state ->
                    state.copy(
                        evaluationUuid = freshUuid,
                        persistenceIndicatorState = AutosaveUiIndicatorState.ERROR,
                        persistenceStatusMessage = "No se pudo recuperar la evaluación guardada: el registro está corrupto o ilegible.",
                        wasRestoredFromPersistence = false,
                        persistenceRecoveryErrorMessage = "No se pudo recuperar la evaluación previa: ${loadResult.reason}",
                        savedEvaluationHistory = initialHistory,
                        isReadOnlyConsultationMode = false,
                        readOnlyEditableUnlockTrail = null,
                        pendingDeleteEvaluationSummary = null
                    )
                }
            }

            is LoadEvaluationResult.NotFound -> {
                val freshUuid = repo.generateNewEvaluationUuid()
                clinicalMutationCounter.set(0L)
                _uiState.update { state ->
                    state.copy(
                        evaluationUuid = freshUuid,
                        persistenceIndicatorState = AutosaveUiIndicatorState.IDLE,
                        persistenceStatusMessage = null,
                        wasRestoredFromPersistence = false,
                        persistenceRecoveryErrorMessage = null,
                        savedEvaluationHistory = initialHistory,
                        isReadOnlyConsultationMode = false,
                        readOnlyEditableUnlockTrail = null,
                        pendingDeleteEvaluationSummary = null
                    )
                }
            }
        }
    }

    private fun hasPersistableClinicalChange(
        before: CredEdiUiState,
        after: CredEdiUiState
    ): Boolean {
        return before.evaluationUuid != after.evaluationUuid ||
            before.patientIdentification != after.patientIdentification ||
            before.sexo != after.sexo ||
            before.fechaNacimiento != after.fechaNacimiento ||
            before.fechaEvaluacion != after.fechaEvaluacion ||
            before.esPrematuro != after.esPrematuro ||
            before.egSemanas != after.egSemanas ||
            before.egDias != after.egDias ||
            before.ediEgRegistrada != after.ediEgRegistrada ||
            before.ediBirthMaturityCondition != after.ediBirthMaturityCondition ||
            before.pesoInput != after.pesoInput ||
            java.lang.Double.compare(before.pesoKg, after.pesoKg) != 0 ||
            before.longitudInput != after.longitudInput ||
            java.lang.Double.compare(before.longitudCm, after.longitudCm) != 0 ||
            before.perimetroCefalicoInput != after.perimetroCefalicoInput ||
            java.lang.Double.compare(before.perimetroCefalicoCm, after.perimetroCefalicoCm) != 0 ||
            before.ediEngineState != after.ediEngineState ||
            before.ediAgeDiscrepancyResolution != after.ediAgeDiscrepancyResolution ||
            before.ediManualGroupOverride != after.ediManualGroupOverride ||
            before.ediFechaEval != after.ediFechaEval ||
            before.ediEgSemanasInput != after.ediEgSemanasInput
    }

    private fun hasClinicalContentOrPendingChanges(state: CredEdiUiState): Boolean {
        val hasPendingOrError = clinicalMutationCounter.get() > state.lastPersistedRevision ||
            state.persistenceIndicatorState == AutosaveUiIndicatorState.PENDING_CHANGES ||
            state.persistenceIndicatorState == AutosaveUiIndicatorState.ERROR
        val hasClinicalInputs = state.patientIdentification.hasExplicitIdentification ||
            state.patientIdentification.generalClinicalNote.isNotBlank() ||
            state.fechaNacimiento.isNotBlank() ||
            state.esPrematuro ||
            state.ediEgRegistrada ||
            state.ediBirthMaturityCondition != EdiBirthMaturityCondition.UNSPECIFIED ||
            state.pesoInput.isNotBlank() ||
            state.longitudInput.isNotBlank() ||
            state.perimetroCefalicoInput.isNotBlank() ||
            state.ediEngineState.hasAnyDataInCurrentEpisode ||
            state.ediEngineState.archivedSessionHistory.isNotEmpty() ||
            state.lastSavedAtEpochMillis != null
        return hasPendingOrError || hasClinicalInputs
    }

    /**
     * Aplica una transformación clínica al estado.
     * - Si la evaluación está abierta en modo consulta de solo lectura (`isReadOnlyConsultationMode == true`),
     *   impide cualquier modificación accidental y conserva intacto el resultado definitivo previo.
     * - Si existe repositorio Room configurado y hubo cambio clínico real, incrementa la revisión
     *   monotónica, marca «Cambios pendientes» y programa el autoguardado fuera del hilo principal.
     */
    private fun updateClinicalState(transform: (CredEdiUiState) -> CredEdiUiState) {
        var shouldTriggerAutosave = false
        _uiState.update { current ->
            if (current.isReadOnlyConsultationMode) {
                return@update current.copy(
                    persistenceStatusMessage = "Modo consulta (solo lectura): evaluación completada protegida contra cambios accidentales. Pulse «Habilitar edición clínica» si requiere modificarla."
                )
            }
            val transformed = transform(current)
            if (persistenceRepository == null || !hasPersistableClinicalChange(current, transformed)) {
                transformed
            } else {
                val nextRev = maxOf(
                    clinicalMutationCounter.incrementAndGet(),
                    current.lastPersistedRevision + 1L
                )
                clinicalMutationCounter.set(nextRev)
                shouldTriggerAutosave = true
                transformed.copy(
                    persistenceIndicatorState = AutosaveUiIndicatorState.PENDING_CHANGES,
                    persistenceStatusMessage = null,
                    wasRestoredFromPersistence = false,
                    persistenceRecoveryErrorMessage = null
                )
            }
        }
        if (shouldTriggerAutosave) {
            scheduleAutosave()
        }
    }

    private fun scheduleAutosave() {
        val repo = persistenceRepository ?: return
        autosaveDebounceJob?.cancel()
        autosaveDebounceJob = viewModelScope.launch(ioDispatcher) {
            if (autosaveDebounceMs > 0L) {
                delay(autosaveDebounceMs)
            }
            drainPendingSavesInternal(repo = repo, isManualTrigger = false)
        }
    }

    /**
     * Fuerza inmediatamente el guardado de cualquier cambio pendiente de autoguardado sin esperar
     * al temporizador de debounce (invocado también en `onPause`/`onStop` de `MainActivity`).
     */
    fun flushPendingAutosaveAsync() {
        val repo = persistenceRepository ?: return
        val state = _uiState.value
        if (state.isReadOnlyConsultationMode) return
        val hasPending = clinicalMutationCounter.get() > state.lastPersistedRevision ||
            state.persistenceIndicatorState == AutosaveUiIndicatorState.PENDING_CHANGES
        if (!hasPending) return
        autosaveDebounceJob?.cancel()
        autosaveDebounceJob = viewModelScope.launch(ioDispatcher) {
            drainPendingSavesInternal(repo = repo, isManualTrigger = false)
        }
    }

    /**
     * Espera de forma determinista a que concluya cualquier autoguardado pendiente o en vuelo.
     */
    suspend fun awaitPendingAutosave(): SaveEvaluationResult? {
        val repo = persistenceRepository ?: return null
        autosaveDebounceJob?.cancel()
        return withContext(ioDispatcher) {
            drainPendingSavesInternal(repo = repo, isManualTrigger = false)
        }
    }

    /**
     * MEDIO-06 — LOTE 2 & LOTE 3:
     * Acción del botón «Guardar evaluación».
     */
    fun saveCurrentEvaluationManually() {
        val repo = persistenceRepository
        if (repo == null) {
            _uiState.update { state ->
                state.copy(
                    persistenceIndicatorState = AutosaveUiIndicatorState.ERROR,
                    persistenceStatusMessage = "No se pudo guardar: almacenamiento Room no inicializado."
                )
            }
            return
        }
        if (_uiState.value.isReadOnlyConsultationMode) {
            _uiState.update { state ->
                state.copy(
                    persistenceIndicatorState = AutosaveUiIndicatorState.SAVED,
                    persistenceStatusMessage = "Modo consulta (solo lectura): la evaluación completada ya se encuentra resguardada."
                )
            }
            return
        }
        autosaveDebounceJob?.cancel()
        _uiState.update { state ->
            state.copy(
                persistenceIndicatorState = AutosaveUiIndicatorState.SAVING,
                persistenceStatusMessage = "Guardando…"
            )
        }
        autosaveDebounceJob = viewModelScope.launch(ioDispatcher) {
            drainPendingSavesInternal(repo = repo, isManualTrigger = true)
        }
    }

    /**
     * Variante suspendida del guardado manual para verificación determinista en pruebas.
     */
    suspend fun saveCurrentEvaluationManuallySuspend(): SaveEvaluationResult {
        val repo = persistenceRepository
            ?: return SaveEvaluationResult.Failure(
                evaluationUuid = _uiState.value.evaluationUuid,
                reason = "Almacenamiento Room no inicializado."
            ).also {
                _uiState.update { state ->
                    state.copy(
                        persistenceIndicatorState = AutosaveUiIndicatorState.ERROR,
                        persistenceStatusMessage = "No se pudo guardar: almacenamiento Room no inicializado."
                    )
                }
            }
        autosaveDebounceJob?.cancel()
        _uiState.update { state ->
            state.copy(
                persistenceIndicatorState = AutosaveUiIndicatorState.SAVING,
                persistenceStatusMessage = "Guardando…"
            )
        }
        return withContext(ioDispatcher) {
            drainPendingSavesInternal(repo = repo, isManualTrigger = true)
                ?: SaveEvaluationResult.Failure(
                    evaluationUuid = _uiState.value.evaluationUuid,
                    reason = "No se ejecutó la transacción de guardado."
                )
        }
    }

    private suspend fun drainPendingSavesInternal(
        repo: CredEdiPersistenceRepository,
        isManualTrigger: Boolean
    ): SaveEvaluationResult? = saveExecutionMutex.withLock {
        var lastResult: SaveEvaluationResult? = null
        var firstIteration = true

        while (true) {
            val snapshot = _uiState.value
            if (snapshot.isReadOnlyConsultationMode && !isManualTrigger) {
                break
            }
            val currentTargetRev = clinicalMutationCounter.get()
            val alreadyUpToDate = snapshot.lastPersistedRevision >= currentTargetRev &&
                snapshot.persistenceIndicatorState == AutosaveUiIndicatorState.SAVED &&
                snapshot.lastSavedAtEpochMillis != null

            if (!isManualTrigger && alreadyUpToDate) {
                break
            }
            if (isManualTrigger && !firstIteration && alreadyUpToDate) {
                break
            }

            val revisionToPersist = if (isManualTrigger && firstIteration && currentTargetRev <= snapshot.lastPersistedRevision) {
                val forcedRev = snapshot.lastPersistedRevision + 1L
                clinicalMutationCounter.set(forcedRev)
                forcedRev
            } else {
                maxOf(currentTargetRev, snapshot.lastPersistedRevision + 1L).also {
                    if (it > currentTargetRev) {
                        clinicalMutationCounter.set(it)
                    }
                }
            }

            firstIteration = false

            _uiState.update { state ->
                state.copy(
                    persistenceIndicatorState = AutosaveUiIndicatorState.SAVING,
                    persistenceStatusMessage = if (isManualTrigger) "Guardando…" else state.persistenceStatusMessage
                )
            }

            val stateToPersist = _uiState.value
            val (saveResult, updatedHistory) = withContext(ioDispatcher + NonCancellable) {
                val res = repo.saveEvaluation(
                    state = stateToPersist,
                    requestedStatus = null,
                    markAsActiveDraft = true,
                    explicitRevision = revisionToPersist
                )
                val history = if (res is SaveEvaluationResult.Success) {
                    repo.listSavedEvaluationSummaries()
                } else {
                    stateToPersist.savedEvaluationHistory
                }
                res to history
            }
            lastResult = saveResult

            when (saveResult) {
                is SaveEvaluationResult.Success -> {
                    val latestRevAfterSave = clinicalMutationCounter.get()
                    if (latestRevAfterSave <= revisionToPersist) {
                        _uiState.update { state ->
                            state.copy(
                                persistenceIndicatorState = AutosaveUiIndicatorState.SAVED,
                                persistenceStatusMessage = if (isManualTrigger) {
                                    "Evaluación guardada correctamente"
                                } else {
                                    state.persistenceStatusMessage
                                },
                                lastSavedAtEpochMillis = saveResult.savedAtEpochMillis,
                                lastPersistedRevision = saveResult.monotonicRevision,
                                persistenceRecoveryErrorMessage = null,
                                savedEvaluationHistory = updatedHistory
                            )
                        }
                        break
                    } else {
                        _uiState.update { state ->
                            state.copy(
                                lastSavedAtEpochMillis = saveResult.savedAtEpochMillis,
                                lastPersistedRevision = saveResult.monotonicRevision,
                                savedEvaluationHistory = updatedHistory
                            )
                        }
                    }
                }

                is SaveEvaluationResult.Failure -> {
                    _uiState.update { state ->
                        state.copy(
                            persistenceIndicatorState = AutosaveUiIndicatorState.ERROR,
                            persistenceStatusMessage = "No se pudo guardar: ${saveResult.reason}"
                        )
                    }
                    break
                }
            }
        }
        lastResult
    }

    // =========================================================================
    // MEDIO-06 — LOTE 3: HISTORIAL DESPLEGABLE, CONTINUAR, VER, ELIMINAR Y NUEVA EVALUACIÓN
    // =========================================================================

    /**
     * Consulta y actualiza desde Room la lista de evaluaciones guardadas ordenadas por fecha
     * de modificación descendente.
     */
    fun refreshSavedEvaluationHistory() {
        val repo = persistenceRepository ?: return
        val latest = try {
            runBlocking(ioDispatcher) {
                repo.listSavedEvaluationSummaries()
            }
        } catch (_: Throwable) {
            _uiState.value.savedEvaluationHistory
        }
        _uiState.update { state ->
            state.copy(savedEvaluationHistory = latest)
        }
    }

    suspend fun refreshSavedEvaluationHistorySuspend() {
        val repo = persistenceRepository ?: return
        val latest = withContext(ioDispatcher) {
            repo.listSavedEvaluationSummaries()
        }
        _uiState.update { state ->
            state.copy(savedEvaluationHistory = latest)
        }
    }

    /**
     * Despliega u oculta el historial de evaluaciones dentro del apartado
     * «Guardado e Historial de Evaluaciones». Al desplegarlo, consulta siempre Room.
     */
    fun toggleEvaluationHistory() {
        val willExpand = !_uiState.value.isEvaluationHistoryExpanded
        val latestHistory = if (willExpand && persistenceRepository != null) {
            try {
                runBlocking(ioDispatcher) {
                    persistenceRepository.listSavedEvaluationSummaries()
                }
            } catch (_: Throwable) {
                _uiState.value.savedEvaluationHistory
            }
        } else {
            _uiState.value.savedEvaluationHistory
        }
        _uiState.update { state ->
            state.copy(
                isEvaluationHistoryExpanded = willExpand,
                savedEvaluationHistory = latestHistory
            )
        }
    }

    /**
     * Persiste la evaluación activa antes de cambiar de paciente/registro cuando tiene contenido
     * clínico o cambios pendientes y no está en modo consulta de solo lectura.
     * Retorna `false` si ocurre un error de almacenamiento para impedir perder datos del paciente activo.
     */
    private suspend fun persistCurrentBeforePatientSwitchIfNeeded(
        repo: CredEdiPersistenceRepository
    ): Boolean = saveExecutionMutex.withLock {
        val current = _uiState.value
        if (current.isReadOnlyConsultationMode) {
            return@withLock true
        }
        if (!hasClinicalContentOrPendingChanges(current)) {
            return@withLock true
        }
        val nextRev = maxOf(clinicalMutationCounter.get(), current.lastPersistedRevision) + 1L
        clinicalMutationCounter.set(nextRev)
        val saveResult = withContext(ioDispatcher + NonCancellable) {
            repo.saveEvaluation(
                state = current,
                requestedStatus = null,
                markAsActiveDraft = false,
                explicitRevision = nextRev
            )
        }
        return@withLock when (saveResult) {
            is SaveEvaluationResult.Success -> {
                _uiState.update { state ->
                    state.copy(
                        persistenceIndicatorState = AutosaveUiIndicatorState.SAVED,
                        lastSavedAtEpochMillis = saveResult.savedAtEpochMillis,
                        lastPersistedRevision = saveResult.monotonicRevision,
                        persistenceRecoveryErrorMessage = null
                    )
                }
                true
            }
            is SaveEvaluationResult.Failure -> {
                _uiState.update { state ->
                    state.copy(
                        persistenceIndicatorState = AutosaveUiIndicatorState.ERROR,
                        persistenceStatusMessage = "Error de guardado: no se puede cambiar de paciente sin antes resguardar la evaluación actual (${saveResult.reason})."
                    )
                }
                false
            }
        }
    }

    /**
     * MEDIO-06 — LOTE 3 (Sección 6):
     * Inicia una «Nueva evaluación» correspondiente a un nuevo registro clínico / paciente:
     * 1. Comprueba que los cambios actuales hayan sido guardados en Room.
     * 2. Si ocurre un error de almacenamiento, impide el cambio silencioso de paciente.
     * 3. Conserva la evaluación anterior en Room.
     * 4. Crea un `evaluationUuid` nuevo y único.
     * 5. Crea un `CredEdiUiState` limpio sin copiar datos del paciente anterior.
     * 6. Reinicia respuestas EDI y notas clínicas.
     * 7. Deja peso, talla y perímetro cefálico estrictamente vacíos (`Double.NaN`).
     * 8. Muestra confirmación discreta de nueva evaluación iniciada.
     */
    fun startNewPatientEvaluation() {
        runBlocking(ioDispatcher) {
            startNewPatientEvaluationSuspend()
        }
    }

    suspend fun startNewPatientEvaluationSuspend(): Boolean {
        autosaveDebounceJob?.cancel()
        val repo = persistenceRepository
        if (repo != null) {
            val savedOk = persistCurrentBeforePatientSwitchIfNeeded(repo)
            if (!savedOk) {
                return false
            }
            withContext(ioDispatcher) {
                repo.deactivateAllActiveDrafts()
            }
        }

        val current = _uiState.value
        var freshUuid = repo?.generateNewEvaluationUuid()
            ?: "EVAL_${java.util.UUID.randomUUID()}"
        if (freshUuid == current.evaluationUuid) {
            freshUuid = "EVAL_${java.util.UUID.randomUUID()}"
        }

        val updatedHistory = if (repo != null) {
            withContext(ioDispatcher) {
                repo.listSavedEvaluationSummaries()
            }
        } else {
            current.savedEvaluationHistory
        }

        clinicalMutationCounter.set(0L)
        _uiState.value = CredEdiUiState(
            evaluationUuid = freshUuid,
            patientIdentification = PatientIdentificationData(),
            persistenceIndicatorState = AutosaveUiIndicatorState.IDLE,
            persistenceStatusMessage = "Nueva evaluación iniciada",
            lastSavedAtEpochMillis = null,
            lastPersistedRevision = 0L,
            wasRestoredFromPersistence = false,
            persistenceRecoveryErrorMessage = null,
            savedEvaluationHistory = updatedHistory,
            isEvaluationHistoryExpanded = current.isEvaluationHistoryExpanded,
            isReadOnlyConsultationMode = false,
            readOnlyEditableUnlockTrail = null,
            pendingDeleteEvaluationSummary = null,
            currentTab = current.currentTab,
            sexo = SexoBiologico.NINO,
            fechaNacimiento = "",
            fechaEvaluacion = ClinicalAgeCalculator.todayFormatted(),
            esPrematuro = false,
            egSemanas = 32,
            egDias = 0,
            pesoKg = Double.NaN,
            longitudCm = Double.NaN,
            perimetroCefalicoCm = Double.NaN,
            pesoInput = "",
            longitudInput = "",
            perimetroCefalicoInput = "",
            ediEngineState = EdiPhase1SessionManager.createInitialState(1),
            ediAgeDiscrepancyResolution = null,
            ediManualGroupOverride = false,
            ediEgRegistrada = false,
            ediBirthMaturityCondition = EdiBirthMaturityCondition.UNSPECIFIED
        )
        return true
    }

    /**
     * MEDIO-06 — LOTE 3D (Secciones 5 y 6):
     * Flujo seguro de creación de una nueva evaluación independiente con validación previa de
     * identificación del paciente:
     * 1. Valida nombre completo (campo único) y DNI numérico de 8 dígitos preservando ceros iniciales
     *    (o flujo explícito de identificación pendiente sin inventar ningún DNI).
     * 2. Verifica cambios pendientes de la evaluación actual y la persiste en Room antes del cambio.
     * 3. Si falla el guardado, aborta la creación y conserva intacta la evaluación actual.
     * 4. Genera un `evaluationUuid` nuevo e inicializa una evaluación completamente limpia
     *    (`Double.NaN` en peso/talla/PC, respuestas y notas EDI vacías), asociando nombre y DNI.
     * 5. Conserva intactas las evaluaciones previas y abre la pestaña Calculadora para continuar.
     */
    fun createNewPatientEvaluationWithIdentification(
        fullName: String,
        dni: String,
        allowPendingIdentification: Boolean = false
    ): NewEvaluationCreationResult {
        return runBlocking(ioDispatcher) {
            createNewPatientEvaluationWithIdentificationSuspend(
                fullName = fullName,
                dni = dni,
                allowPendingIdentification = allowPendingIdentification
            )
        }
    }

    suspend fun createNewPatientEvaluationWithIdentificationSuspend(
        fullName: String,
        dni: String,
        allowPendingIdentification: Boolean = false
    ): NewEvaluationCreationResult {
        val cleanName = fullName.replace("\r", " ").replace("\n", " ").trim()
        val rawDni = dni.trim()

        val finalDni = if (allowPendingIdentification) {
            if (rawDni.isNotEmpty() && (rawDni.length != 8 || !rawDni.all { it.isDigit() })) {
                return NewEvaluationCreationResult.ValidationError(
                    "Si ingresa un DNI debe contener exactamente 8 dígitos numéricos, o déjelo vacío para identificación pendiente."
                )
            }
            ""
        } else {
            if (cleanName.isEmpty()) {
                return NewEvaluationCreationResult.ValidationError(
                    "Ingrese el nombre completo y apellidos del paciente antes de crear la evaluación."
                )
            }
            if (rawDni.length != 8 || !rawDni.all { it.isDigit() }) {
                return NewEvaluationCreationResult.ValidationError(
                    "Ingrese un DNI peruano válido de 8 dígitos numéricos (se conservan los ceros iniciales) o marque «Identificación pendiente»."
                )
            }
            rawDni
        }

        autosaveDebounceJob?.cancel()
        val repo = persistenceRepository
        val previousState = _uiState.value
        val previousMutationCount = clinicalMutationCounter.get()

        if (repo != null) {
            val savedOk = persistCurrentBeforePatientSwitchIfNeeded(repo)
            if (!savedOk) {
                val errMsg = _uiState.value.persistenceStatusMessage
                    ?: "Error de guardado: no se pudo guardar la evaluación actual antes de crear una nueva."
                return NewEvaluationCreationResult.PersistenceError(errMsg)
            }
            try {
                withContext(ioDispatcher) {
                    repo.deactivateAllActiveDrafts()
                }
            } catch (t: Throwable) {
                val errMsg = "Error de guardado: no se pudo preparar el nuevo registro (${t.message ?: t::class.simpleName})."
                _uiState.update { state ->
                    state.copy(
                        persistenceIndicatorState = AutosaveUiIndicatorState.ERROR,
                        persistenceStatusMessage = errMsg
                    )
                }
                return NewEvaluationCreationResult.PersistenceError(errMsg)
            }
        }

        var freshUuid = repo?.generateNewEvaluationUuid()
            ?: "EVAL_${java.util.UUID.randomUUID()}"
        if (freshUuid == previousState.evaluationUuid) {
            freshUuid = "EVAL_${java.util.UUID.randomUUID()}"
        }

        val cleanNewState = CredEdiUiState(
            evaluationUuid = freshUuid,
            patientIdentification = PatientIdentificationData(
                patientIdentifierOrAlias = cleanName,
                clinicalRecordCode = finalDni,
                generalClinicalNote = ""
            ),
            persistenceIndicatorState = AutosaveUiIndicatorState.IDLE,
            persistenceStatusMessage = if (allowPendingIdentification) {
                "Nueva evaluación creada con identificación pendiente"
            } else {
                "Nueva evaluación creada"
            },
            lastSavedAtEpochMillis = null,
            lastPersistedRevision = 0L,
            wasRestoredFromPersistence = false,
            persistenceRecoveryErrorMessage = null,
            savedEvaluationHistory = previousState.savedEvaluationHistory,
            isEvaluationHistoryExpanded = false,
            isReadOnlyConsultationMode = false,
            readOnlyEditableUnlockTrail = null,
            pendingDeleteEvaluationSummary = null,
            currentTab = AppTab.CALCULADORA,
            sexo = SexoBiologico.NINO,
            fechaNacimiento = "",
            fechaEvaluacion = ClinicalAgeCalculator.todayFormatted(),
            esPrematuro = false,
            egSemanas = 32,
            egDias = 0,
            pesoKg = Double.NaN,
            longitudCm = Double.NaN,
            perimetroCefalicoCm = Double.NaN,
            pesoInput = "",
            longitudInput = "",
            perimetroCefalicoInput = "",
            ediEngineState = EdiPhase1SessionManager.createInitialState(1),
            ediAgeDiscrepancyResolution = null,
            ediManualGroupOverride = false,
            ediEgRegistrada = false,
            ediBirthMaturityCondition = EdiBirthMaturityCondition.UNSPECIFIED
        )

        if (repo != null) {
            val initialSaveResult = saveExecutionMutex.withLock {
                withContext(ioDispatcher + NonCancellable) {
                    repo.saveEvaluation(
                        state = cleanNewState,
                        requestedStatus = null,
                        markAsActiveDraft = true,
                        explicitRevision = 1L
                    )
                }
            }
            when (initialSaveResult) {
                is SaveEvaluationResult.Success -> {
                    val refreshedHistory = withContext(ioDispatcher) {
                        repo.listSavedEvaluationSummaries()
                    }
                    clinicalMutationCounter.set(initialSaveResult.monotonicRevision)
                    _uiState.value = cleanNewState.copy(
                        persistenceIndicatorState = AutosaveUiIndicatorState.SAVED,
                        lastSavedAtEpochMillis = initialSaveResult.savedAtEpochMillis,
                        lastPersistedRevision = initialSaveResult.monotonicRevision,
                        savedEvaluationHistory = refreshedHistory
                    )
                    return NewEvaluationCreationResult.Success(freshUuid)
                }
                is SaveEvaluationResult.Failure -> {
                    clinicalMutationCounter.set(previousMutationCount)
                    val errMsg = "Error de guardado: no se pudo crear la nueva evaluación (${initialSaveResult.reason})."
                    _uiState.value = previousState.copy(
                        persistenceIndicatorState = AutosaveUiIndicatorState.ERROR,
                        persistenceStatusMessage = errMsg
                    )
                    return NewEvaluationCreationResult.PersistenceError(errMsg)
                }
            }
        } else {
            clinicalMutationCounter.set(0L)
            _uiState.value = cleanNewState
            return NewEvaluationCreationResult.Success(freshUuid)
        }
    }

    /**
     * MEDIO-06 — LOTE 3 (Sección 5.A):
     * Acción «Continuar» del historial para evaluaciones pendientes o incompletas.
     * - Guarda primero la evaluación activa si tiene cambios o contenido clínico; si falla, aborta el cambio.
     * - Carga el registro seleccionado desde Room, verifica integridad criptográfica AES-256-GCM + SHA-256
     *   y versión de esquema, restaura todas sus respuestas, notas, retrocesos y antropometría (`Double.NaN`
     *   para mediciones vacías), recalcula semáforo y diagnósticos OMS y muestra «Evaluación recuperada».
     */
    fun continueEvaluationFromHistory(targetEvaluationUuid: String) {
        runBlocking(ioDispatcher) {
            continueEvaluationFromHistorySuspend(targetEvaluationUuid)
        }
    }

    suspend fun continueEvaluationFromHistorySuspend(targetEvaluationUuid: String): Boolean {
        val repo = persistenceRepository ?: return false
        val cleanTargetUuid = targetEvaluationUuid.trim()
        if (cleanTargetUuid.isEmpty()) return false
        autosaveDebounceJob?.cancel()

        val current = _uiState.value
        if (cleanTargetUuid != current.evaluationUuid) {
            val savedOk = persistCurrentBeforePatientSwitchIfNeeded(repo)
            if (!savedOk) {
                return false
            }
        }

        val loadResult = withContext(ioDispatcher) {
            repo.loadEvaluationByUuid(cleanTargetUuid)
        }
        return when (loadResult) {
            is LoadEvaluationResult.Success -> {
                val record = loadResult.record
                if (record.restoredUiState.evaluationUuid != cleanTargetUuid) {
                    _uiState.update { state ->
                        state.copy(
                            persistenceIndicatorState = AutosaveUiIndicatorState.ERROR,
                            persistenceStatusMessage = "Error de seguridad: el identificador de la evaluación restaurada no coincide con el solicitado."
                        )
                    }
                    return false
                }
                val updatedHistory = withContext(ioDispatcher) {
                    repo.selectActiveDraft(cleanTargetUuid)
                    repo.listSavedEvaluationSummaries()
                }
                val restoredSynced = syncEdiTemplateQuestions(record.restoredUiState)
                clinicalMutationCounter.set(record.summary.monotonicRevision)
                _uiState.value = restoredSynced.copy(
                    currentTab = current.currentTab,
                    persistenceIndicatorState = AutosaveUiIndicatorState.SAVED,
                    persistenceStatusMessage = "Evaluación recuperada",
                    lastSavedAtEpochMillis = record.summary.updatedAtEpochMillis,
                    lastPersistedRevision = record.summary.monotonicRevision,
                    wasRestoredFromPersistence = true,
                    persistenceRecoveryErrorMessage = null,
                    savedEvaluationHistory = updatedHistory,
                    isEvaluationHistoryExpanded = current.isEvaluationHistoryExpanded,
                    isReadOnlyConsultationMode = false,
                    readOnlyEditableUnlockTrail = null,
                    pendingDeleteEvaluationSummary = null
                )
                true
            }

            is LoadEvaluationResult.CorruptedOrUnreadable -> {
                _uiState.update { state ->
                    state.copy(
                        persistenceIndicatorState = AutosaveUiIndicatorState.ERROR,
                        persistenceStatusMessage = "No se pudo recuperar la evaluación seleccionada: ${loadResult.reason}",
                        persistenceRecoveryErrorMessage = loadResult.reason
                    )
                }
                false
            }

            is LoadEvaluationResult.NotFound -> {
                _uiState.update { state ->
                    state.copy(
                        persistenceIndicatorState = AutosaveUiIndicatorState.ERROR,
                        persistenceStatusMessage = "La evaluación solicitada no existe en el almacenamiento local."
                    )
                }
                false
            }
        }
    }

    /**
     * MEDIO-06 — LOTE 3 (Sección 5.B):
     * Acción «Ver» del historial para evaluaciones completadas.
     * - Abre el registro para consulta en modo de solo lectura (`isReadOnlyConsultationMode = true`),
     *   impidiendo modificaciones accidentales a sus respuestas o a su resultado definitivo.
     * - Si se requiere editar posteriormente, el usuario debe pulsar explícitamente «Habilitar edición clínica».
     */
    fun viewCompletedEvaluationFromHistory(targetEvaluationUuid: String) {
        runBlocking(ioDispatcher) {
            viewCompletedEvaluationFromHistorySuspend(targetEvaluationUuid)
        }
    }

    suspend fun viewCompletedEvaluationFromHistorySuspend(targetEvaluationUuid: String): Boolean {
        val repo = persistenceRepository ?: return false
        val cleanTargetUuid = targetEvaluationUuid.trim()
        if (cleanTargetUuid.isEmpty()) return false
        autosaveDebounceJob?.cancel()

        val current = _uiState.value
        if (cleanTargetUuid != current.evaluationUuid) {
            val savedOk = persistCurrentBeforePatientSwitchIfNeeded(repo)
            if (!savedOk) {
                return false
            }
        }

        val loadResult = withContext(ioDispatcher) {
            repo.loadEvaluationByUuid(cleanTargetUuid)
        }
        return when (loadResult) {
            is LoadEvaluationResult.Success -> {
                val record = loadResult.record
                if (record.restoredUiState.evaluationUuid != cleanTargetUuid) {
                    _uiState.update { state ->
                        state.copy(
                            persistenceIndicatorState = AutosaveUiIndicatorState.ERROR,
                            persistenceStatusMessage = "Error de seguridad: el identificador de la evaluación consultada no coincide con el solicitado."
                        )
                    }
                    return false
                }
                val updatedHistory = withContext(ioDispatcher) {
                    repo.selectActiveDraft(cleanTargetUuid)
                    repo.listSavedEvaluationSummaries()
                }
                val restoredSynced = syncEdiTemplateQuestions(record.restoredUiState)
                clinicalMutationCounter.set(record.summary.monotonicRevision)
                _uiState.value = restoredSynced.copy(
                    currentTab = current.currentTab,
                    persistenceIndicatorState = AutosaveUiIndicatorState.SAVED,
                    persistenceStatusMessage = "Evaluación recuperada en modo consulta (solo lectura)",
                    lastSavedAtEpochMillis = record.summary.updatedAtEpochMillis,
                    lastPersistedRevision = record.summary.monotonicRevision,
                    wasRestoredFromPersistence = true,
                    persistenceRecoveryErrorMessage = null,
                    savedEvaluationHistory = updatedHistory,
                    isEvaluationHistoryExpanded = current.isEvaluationHistoryExpanded,
                    isReadOnlyConsultationMode = true,
                    readOnlyEditableUnlockTrail = null,
                    pendingDeleteEvaluationSummary = null
                )
                true
            }

            is LoadEvaluationResult.CorruptedOrUnreadable -> {
                _uiState.update { state ->
                    state.copy(
                        persistenceIndicatorState = AutosaveUiIndicatorState.ERROR,
                        persistenceStatusMessage = "No se pudo abrir la evaluación seleccionada: ${loadResult.reason}",
                        persistenceRecoveryErrorMessage = loadResult.reason
                    )
                }
                false
            }

            is LoadEvaluationResult.NotFound -> {
                _uiState.update { state ->
                    state.copy(
                        persistenceIndicatorState = AutosaveUiIndicatorState.ERROR,
                        persistenceStatusMessage = "La evaluación solicitada no existe en el almacenamiento local."
                    )
                }
                false
            }
        }
    }

    /**
     * Desbloquea explícitamente la edición de una evaluación completada abierta mediante «Ver»,
     * registrando la trazabilidad del cambio sin sobrescribir silenciosamente el resultado previo.
     */
    fun enableEditingForViewedCompletedEvaluation(
        reason: String = "Edición clínica autorizada explícitamente por el profesional"
    ) {
        val cleanReason = reason.trim().ifEmpty {
            "Edición clínica autorizada explícitamente por el profesional"
        }
        _uiState.update { state ->
            if (!state.isReadOnlyConsultationMode) {
                state
            } else {
                state.copy(
                    isReadOnlyConsultationMode = false,
                    readOnlyEditableUnlockTrail = "Edición habilitada explícitamente: $cleanReason",
                    persistenceStatusMessage = "Edición clínica habilitada ($cleanReason)."
                )
            }
        }
    }

    /**
     * MEDIO-06 — LOTE 3 (Sección 5.C):
     * Solicita eliminar una evaluación del historial abriendo el diálogo de confirmación explícita.
     */
    fun requestDeleteEvaluationFromHistory(targetEvaluationUuid: String) {
        val cleanUuid = targetEvaluationUuid.trim()
        if (cleanUuid.isEmpty()) return
        val repo = persistenceRepository
        val currentList = if (repo != null) {
            try {
                runBlocking(ioDispatcher) {
                    repo.listSavedEvaluationSummaries()
                }
            } catch (_: Throwable) {
                _uiState.value.savedEvaluationHistory
            }
        } else {
            _uiState.value.savedEvaluationHistory
        }
        val summary = currentList.firstOrNull { it.evaluationUuid == cleanUuid }
            ?: _uiState.value.savedEvaluationHistory.firstOrNull { it.evaluationUuid == cleanUuid }
            ?: return
        _uiState.update { state ->
            state.copy(
                savedEvaluationHistory = currentList,
                pendingDeleteEvaluationSummary = summary
            )
        }
    }

    fun cancelDeleteEvaluationFromHistory() {
        _uiState.update { state ->
            state.copy(pendingDeleteEvaluationSummary = null)
        }
    }

    /**
     * Confirma la eliminación exclusiva de la evaluación seleccionada en `pendingDeleteEvaluationSummary`,
     * actualizando inmediatamente el historial sin afectar a las demás evaluaciones.
     */
    fun confirmDeleteEvaluationFromHistory() {
        runBlocking(ioDispatcher) {
            confirmDeleteEvaluationFromHistorySuspend()
        }
    }

    suspend fun confirmDeleteEvaluationFromHistorySuspend(): Boolean {
        val repo = persistenceRepository ?: return false
        val pending = _uiState.value.pendingDeleteEvaluationSummary ?: return false
        val targetUuid = pending.evaluationUuid
        val deletingActiveEvaluation = targetUuid == _uiState.value.evaluationUuid
        if (deletingActiveEvaluation) {
            autosaveDebounceJob?.cancel()
        }

        val (deletedOk, updatedHistory) = withContext(ioDispatcher) {
            val ok = repo.deleteEvaluation(targetUuid)
            val list = repo.listSavedEvaluationSummaries()
            ok to list
        }
        if (!deletedOk) {
            _uiState.update { state ->
                state.copy(
                    pendingDeleteEvaluationSummary = null,
                    persistenceIndicatorState = AutosaveUiIndicatorState.ERROR,
                    persistenceStatusMessage = "No se pudo eliminar la evaluación seleccionada."
                )
            }
            return false
        }

        val current = _uiState.value
        if (deletingActiveEvaluation) {
            val freshUuid = repo.generateNewEvaluationUuid()
            clinicalMutationCounter.set(0L)
            _uiState.value = CredEdiUiState(
                evaluationUuid = freshUuid,
                patientIdentification = PatientIdentificationData(),
                persistenceIndicatorState = AutosaveUiIndicatorState.IDLE,
                persistenceStatusMessage = "Evaluación eliminada del historial local",
                lastSavedAtEpochMillis = null,
                lastPersistedRevision = 0L,
                wasRestoredFromPersistence = false,
                persistenceRecoveryErrorMessage = null,
                savedEvaluationHistory = updatedHistory,
                isEvaluationHistoryExpanded = current.isEvaluationHistoryExpanded,
                isReadOnlyConsultationMode = false,
                readOnlyEditableUnlockTrail = null,
                pendingDeleteEvaluationSummary = null,
                currentTab = current.currentTab,
                sexo = SexoBiologico.NINO,
                fechaNacimiento = "",
                fechaEvaluacion = ClinicalAgeCalculator.todayFormatted(),
                esPrematuro = false,
                egSemanas = 32,
                egDias = 0,
                pesoKg = Double.NaN,
                longitudCm = Double.NaN,
                perimetroCefalicoCm = Double.NaN,
                pesoInput = "",
                longitudInput = "",
                perimetroCefalicoInput = "",
                ediEngineState = EdiPhase1SessionManager.createInitialState(1),
                ediAgeDiscrepancyResolution = null,
                ediManualGroupOverride = false,
                ediEgRegistrada = false,
                ediBirthMaturityCondition = EdiBirthMaturityCondition.UNSPECIFIED
            )
        } else {
            _uiState.update { state ->
                state.copy(
                    savedEvaluationHistory = updatedHistory,
                    pendingDeleteEvaluationSummary = null,
                    persistenceStatusMessage = "Evaluación eliminada del historial local"
                )
            }
        }
        return true
    }

    /**
     * Actualiza los datos identificatorios opcionales del paciente / expediente vinculados al UUID actual.
     */
    fun updatePatientIdentification(
        patientIdentifierOrAlias: String = _uiState.value.patientIdentification.patientIdentifierOrAlias,
        clinicalRecordCode: String = _uiState.value.patientIdentification.clinicalRecordCode,
        generalClinicalNote: String = _uiState.value.patientIdentification.generalClinicalNote
    ) {
        updateClinicalState { state ->
            state.copy(
                patientIdentification = PatientIdentificationData(
                    patientIdentifierOrAlias = patientIdentifierOrAlias,
                    clinicalRecordCode = clinicalRecordCode,
                    generalClinicalNote = generalClinicalNote
                )
            )
        }
    }

    /**
     * MEDIO-06 — LOTE 3C:
     * Actualiza «Nombres y Apellidos» en un único campo de identificación del paciente en Calculadora,
     * conectado al estado global y al autoguardado cifrado de la evaluación activa.
     */
    fun updatePatientFullName(fullName: String) {
        val sanitizedName = fullName.replace("\r", " ").replace("\n", " ")
        updateClinicalState { state ->
            state.copy(
                patientIdentification = state.patientIdentification.copy(
                    patientIdentifierOrAlias = sanitizedName
                )
            )
        }
    }

    /**
     * MEDIO-06 — LOTE 3C:
     * Actualiza el DNI del paciente conservando estrictamente hasta 8 dígitos numéricos
     * y preservando ceros iniciales (p. ej. "00451234").
     */
    fun updatePatientDni(rawDni: String) {
        val sanitizedDni = rawDni.filter { it.isDigit() }.take(8)
        updateClinicalState { state ->
            state.copy(
                patientIdentification = state.patientIdentification.copy(
                    clinicalRecordCode = sanitizedDni
                )
            )
        }
    }

    private fun syncEdi2026StateForAgeChange(state: CredEdiUiState): CredEdiUiState {
        val validResolution = state.ediAgeDiscrepancyResolution?.takeIf {
            it.matchesInputs(
                fn = state.fechaNacimiento,
                fe = state.fechaEvaluacion,
                prematuro = state.esPrematuro,
                sem = state.egSemanas,
                dias = state.egDias
            )
        }
        if (
            !state.isChronologicalAgeVerifiable ||
            state.ediHasGestationalContradiction ||
            state.isCorrectedAgePending ||
            state.isBirthMaturityConfirmationPendingInUnder24m
        ) {
            return state.copy(
                ediAgeDiscrepancyResolution = null
            )
        }
        val comparison = state.ediCorrectedAgeComparison

        var updatedEngine = state.ediEngineState
        var updatedOverride = state.ediManualGroupOverride

        if (comparison.autoSelectionStatus == EdiGroupAutoSelectionStatus.AUTO_SELECTED_CONSISTENT) {
            val targetGroup = comparison.automaticallyAssignedGroup?.groupNumber
            if (targetGroup != null && !updatedOverride && targetGroup != updatedEngine.activeGroupNumber) {
                updatedEngine = EdiPhase1SessionManager.requestGroupChange(
                    state = updatedEngine,
                    targetGroupNumber = targetGroup,
                    reason = "Actualización automática por edad EDI: corresponde al Grupo $targetGroup"
                )
            }
        } else if (
            comparison.autoSelectionStatus == EdiGroupAutoSelectionStatus.BLOCKED_GROUP_DISCREPANCY_REQUIRES_PROFESSIONAL_REVIEW &&
            validResolution == null
        ) {
            updatedOverride = false
        }

        return state.copy(
            ediEngineState = updatedEngine,
            ediAgeDiscrepancyResolution = validResolution,
            ediManualGroupOverride = updatedOverride
        )
    }

    private fun syncEdiTemplateQuestions(state: CredEdiUiState): CredEdiUiState {
        val template = state.ediTemplate
        val updatedSa = state.saItems.mapIndexed { index, item ->
            val newQuestion = template.saQuestions.getOrElse(index) { item.title }
            item.copy(title = newQuestion)
        }
        val updatedAreas = state.areaItems.mapIndexed { index, item ->
            val pair = template.areaQuestions.getOrNull(index)
            if (pair != null) {
                item.copy(title = pair.first, question = pair.second)
            } else {
                item
            }
        }
        return syncEdi2026StateForAgeChange(
            state.copy(
                saItems = updatedSa,
                areaItems = updatedAreas
            )
        )
    }

    fun selectTab(tab: AppTab) {
        _uiState.update {
            it.copy(
                currentTab = tab,
                showDatosEdiModal = false
            )
        }
    }

    fun selectSexo(sexo: SexoBiologico) {
        updateClinicalState { state ->
            if (sexo != state.sexo && state.ediEngineState.hasAnyDataInCurrentEpisode) {
                val origin = state.ediEngineState.activeSessionOriginContext
                    ?: state.buildCurrentEdiClinicalContext()
                val newEpisodeEngine = EdiPhase1SessionManager.startNewWorkEpisode(
                    state = state.ediEngineState,
                    targetGroupNumber = state.ediEngineState.activeGroupNumber,
                    reason = "Cambio de identificación clínica (Sexo: ${state.sexo.label} -> ${sexo.label})",
                    previousOriginContext = origin
                )
                state.copy(
                    sexo = sexo,
                    ediEngineState = newEpisodeEngine
                )
            } else {
                state.copy(sexo = sexo)
            }
        }
    }

    private fun normalizeDateInput(raw: String): String {
        val trimmed = raw.take(10)
        return if (trimmed.length == 8 && trimmed.all { it.isDigit() }) {
            "${trimmed.substring(0, 2)}/${trimmed.substring(2, 4)}/${trimmed.substring(4, 8)}"
        } else {
            trimmed
        }
    }

    fun updateFechaNacimiento(fecha: String) {
        val normalized = normalizeDateInput(fecha)
        updateClinicalState { state ->
            val hadPreviousNonBlankBirthDate = state.fechaNacimiento.trim().isNotEmpty()
            val changedRegisteredBirthDateWithData =
                hadPreviousNonBlankBirthDate &&
                    normalized != state.fechaNacimiento &&
                    state.ediEngineState.hasAnyDataInCurrentEpisode
            val baseState = if (changedRegisteredBirthDateWithData) {
                val origin = state.ediEngineState.activeSessionOriginContext
                    ?: state.buildCurrentEdiClinicalContext()
                val newEpisodeEngine = EdiPhase1SessionManager.startNewWorkEpisode(
                    state = state.ediEngineState,
                    targetGroupNumber = state.ediEngineState.activeGroupNumber,
                    reason = "Cambio de fecha de nacimiento registrada (${state.fechaNacimiento} -> $normalized)",
                    previousOriginContext = origin
                )
                state.copy(
                    fechaNacimiento = normalized,
                    ediEngineState = newEpisodeEngine,
                    ediManualGroupOverride = false
                )
            } else {
                state.copy(
                    fechaNacimiento = normalized,
                    ediManualGroupOverride = false
                )
            }
            syncEdiTemplateQuestions(baseState)
        }
    }

    private fun parseStrictCalendarDate(raw: String): com.example.model.SimpleDate? {
        val cleaned = normalizeDateInput(raw.trim())
        if (cleaned.isEmpty()) return null
        val parts = cleaned.split("/", "-", ".")
        if (parts.size != 3) return null
        val p0 = parts[0].trim().toIntOrNull() ?: return null
        val p1 = parts[1].trim().toIntOrNull() ?: return null
        val p2 = parts[2].trim().toIntOrNull() ?: return null

        if (parts[0].trim().length == 4 && p0 in 1900..2100 && p1 in 1..12 && p2 in 1..ClinicalAgeCalculator.daysInMonth(p0, p1)) {
            return com.example.model.SimpleDate(day = p2, month = p1, year = p0)
        }
        if (parts[2].trim().length == 4 && p2 in 1900..2100 && p1 in 1..12 && p0 in 1..ClinicalAgeCalculator.daysInMonth(p2, p1)) {
            return com.example.model.SimpleDate(day = p0, month = p1, year = p2)
        }
        return null
    }

    /**
     * Corrige la fecha de nacimiento del mismo paciente dentro del episodio actual sin abrir un nuevo
     * episodio ni sobrescribir retroactivamente los antecedentes de captura de las sesiones archivadas.
     * Valida motivo obligatorio, existencia real en calendario y que no sea posterior a la fecha de evaluación.
     * Si la corrección cambia el grupo etario oficial existiendo respuestas en la sesión activa,
     * solicita confirmación de cambio de grupo preservando íntegramente las respuestas sin pérdida silenciosa.
     */
    fun correctSamePatientFechaNacimiento(
        fecha: String,
        reason: String = "Corrección de fecha de nacimiento del mismo paciente"
    ) {
        val trimmedReason = reason.trim()
        val parsedBirth = parseStrictCalendarDate(fecha)
        updateClinicalState { state ->
            if (trimmedReason.isEmpty()) {
                return@updateClinicalState state.copy(
                    ediEngineState = state.ediEngineState.copy(
                        lastRestoreStatusMessage = "Corrección rechazada: es obligatorio registrar el motivo de la corrección de fecha de nacimiento."
                    )
                )
            }
            if (parsedBirth == null) {
                return@updateClinicalState state.copy(
                    ediEngineState = state.ediEngineState.copy(
                        lastRestoreStatusMessage = "Corrección rechazada: la fecha de nacimiento ingresada ('${fecha.trim()}') es inválida o inexistente."
                    )
                )
            }
            val parsedEval = ClinicalAgeCalculator.parseDate(state.fechaEvaluacion)
            if (parsedEval == null) {
                return@updateClinicalState state.copy(
                    ediEngineState = state.ediEngineState.copy(
                        lastRestoreStatusMessage = "Corrección rechazada: la fecha de evaluación actual ('${state.fechaEvaluacion}') no es válida para verificar compatibilidad."
                    )
                )
            }
            val birthLocal = java.time.LocalDate.of(parsedBirth.year, parsedBirth.month, parsedBirth.day)
            val evalLocal = java.time.LocalDate.of(parsedEval.year, parsedEval.month, parsedEval.day)
            val formattedBirth = String.format(Locale.US, "%02d/%02d/%04d", parsedBirth.day, parsedBirth.month, parsedBirth.year)
            if (birthLocal.isAfter(evalLocal)) {
                return@updateClinicalState state.copy(
                    ediEngineState = state.ediEngineState.copy(
                        lastRestoreStatusMessage = "Corrección rechazada: la nueva fecha de nacimiento ($formattedBirth) no puede ser posterior a la fecha de evaluación (${state.fechaEvaluacion})."
                    )
                )
            }
            if (formattedBirth == state.fechaNacimiento) return@updateClinicalState state
            val previousBirthDate = state.fechaNacimiento
            val preservedActiveOrigin = if (
                state.ediEngineState.activeSession.hasRecordedClinicalResponses &&
                state.ediEngineState.activeSessionOriginContext == null
            ) {
                state.buildCurrentEdiClinicalContext()
            } else {
                state.ediEngineState.activeSessionOriginContext
            }
            val updatedEngine = state.ediEngineState.copy(
                activeSessionOriginContext = preservedActiveOrigin,
                lastRestoreStatusMessage = "$trimmedReason (${state.ediEngineState.currentWorkEpisodeId}: ${previousBirthDate.ifBlank { "Sin F.N." }} -> $formattedBirth). Antecedentes de captura preservados."
            )
            val baseState = state.copy(
                fechaNacimiento = formattedBirth,
                ediEngineState = updatedEngine,
                ediManualGroupOverride = false
            )
            syncEdiTemplateQuestions(baseState)
        }
    }

    fun updateFechaEvaluacion(fecha: String) {
        val normalized = normalizeDateInput(fecha)
        updateClinicalState { state ->
            val parsed = ClinicalAgeCalculator.parseDate(normalized)
            val syncedEdiDate = if (parsed != null) {
                String.format(Locale.US, "%02d/%02d/%04d", parsed.day, parsed.month, parsed.year)
            } else {
                normalized
            }
            syncEdiTemplateQuestions(
                state.copy(
                    fechaEvaluacion = normalized,
                    ediFechaEval = syncedEdiDate,
                    ediManualGroupOverride = false
                )
            )
        }
    }

    fun togglePrematurez(enabled: Boolean) {
        updateClinicalState { state ->
            val maturityCondition = if (enabled) {
                EdiBirthMaturityCondition.CONFIRMED_PRETERM
            } else {
                EdiBirthMaturityCondition.CONFIRMED_TERM
            }
            val updatedFrb = state.frbItems.map { item ->
                if (item.id == 1) {
                    if (enabled) {
                        when {
                            state.ediHasRegisteredEg && state.egSemanas < 37 ->
                                item.copy(
                                    title = "Prematurez < 37 semanas (${state.egSemanas} sem EG)",
                                    checked = true
                                )
                            state.ediHasRegisteredEg ->
                                item.copy(
                                    title = "Contradicción EG (${state.egSemanas} sem EG vs Prematuro)",
                                    checked = false
                                )
                            else ->
                                item.copy(
                                    title = "Prematurez < 37 semanas (EG exacta no registrada)",
                                    checked = false
                                )
                        }
                    } else {
                        item.copy(checked = false)
                    }
                } else {
                    item
                }
            }
            syncEdiTemplateQuestions(
                state.copy(
                    esPrematuro = enabled,
                    ediBirthMaturityCondition = maturityCondition,
                    frbItems = updatedFrb
                )
            )
        }
    }

    fun confirmCurrentEgSemanas() {
        updateEgSemanas(_uiState.value.egSemanas)
    }

    fun updateEgSemanas(semanas: Int) {
        val clamped = semanas.coerceIn(20, 44)
        val isPrematuro = clamped < 37
        val maturityCondition = if (isPrematuro) {
            EdiBirthMaturityCondition.CONFIRMED_PRETERM
        } else {
            EdiBirthMaturityCondition.CONFIRMED_TERM
        }
        updateClinicalState { state ->
            val updatedFrb = state.frbItems.map { item ->
                if (item.id == 1) {
                    if (isPrematuro) {
                        item.copy(title = "Prematurez < 37 semanas ($clamped sem EG)", checked = true)
                    } else {
                        item.copy(title = "A término ($clamped sem EG)", checked = false)
                    }
                } else {
                    item
                }
            }
            syncEdiTemplateQuestions(
                state.copy(
                    egSemanas = clamped,
                    ediEgRegistrada = true,
                    ediEgSemanasInput = clamped.toString(),
                    esPrematuro = isPrematuro,
                    ediBirthMaturityCondition = maturityCondition,
                    frbItems = updatedFrb
                )
            )
        }
    }

    private fun sanitizeDecimal(raw: String): String {
        val normalized = raw.replace(',', '.')
        val sb = StringBuilder()
        var hasDot = false
        for (ch in normalized) {
            if (ch.isDigit()) {
                sb.append(ch)
            } else if (ch == '.' && !hasDot) {
                hasDot = true
                sb.append(ch)
            }
        }
        return sb.toString().take(6)
    }

    fun updatePesoInput(raw: String) {
        val cleaned = sanitizeDecimal(raw)
        val isCompleteNumeric = cleaned.isNotEmpty() && !cleaned.endsWith(".")
        val parsed = if (isCompleteNumeric) cleaned.toDoubleOrNull() else null
        updateClinicalState { state ->
            if (parsed != null && parsed.isFinite() && parsed.toFloat().isFinite() && parsed >= CredEdiUiState.PESO_MIN_VALID_KG) {
                state.copy(
                    pesoInput = cleaned,
                    pesoKg = parsed
                )
            } else {
                state.copy(
                    pesoInput = cleaned,
                    pesoKg = Double.NaN
                )
            }
        }
    }

    fun adjustPeso(delta: Double) {
        updateClinicalState { state ->
            val basePeso = if (state.effectivePesoKg.isFinite() && state.effectivePesoKg >= CredEdiUiState.PESO_MIN_VALID_KG) {
                state.effectivePesoKg
            } else {
                val ageM = state.edadEfectivaMeses.coerceIn(0, 60)
                when {
                    ageM <= 2 -> 5.40
                    ageM <= 6 -> 7.50
                    ageM <= 12 -> 9.50
                    ageM <= 24 -> 12.00
                    else -> 16.00
                }
            }
            val next = (((basePeso + delta) * 100.0).roundToInt() / 100.0).coerceAtLeast(CredEdiUiState.PESO_MIN_VALID_KG)
            state.copy(
                pesoKg = next,
                pesoInput = next.formatDecimal(2)
            )
        }
    }

    fun setPesoKg(peso: Double) {
        updateClinicalState { state ->
            if (peso.isFinite() && peso.toFloat().isFinite() && peso >= CredEdiUiState.PESO_MIN_VALID_KG) {
                state.copy(
                    pesoKg = peso,
                    pesoInput = peso.formatDecimal(2)
                )
            } else {
                state.copy(
                    pesoKg = Double.NaN,
                    pesoInput = ""
                )
            }
        }
    }

    fun updateLongitudInput(raw: String) {
        val cleaned = sanitizeDecimal(raw)
        val isCompleteNumeric = cleaned.isNotEmpty() && !cleaned.endsWith(".")
        val parsed = if (isCompleteNumeric) cleaned.toDoubleOrNull() else null
        updateClinicalState { state ->
            if (parsed != null && parsed.isFinite() && parsed.toFloat().isFinite() && parsed >= CredEdiUiState.LONGITUD_MIN_VALID_CM) {
                state.copy(
                    longitudInput = cleaned,
                    longitudCm = parsed
                )
            } else {
                state.copy(
                    longitudInput = cleaned,
                    longitudCm = Double.NaN
                )
            }
        }
    }

    fun adjustLongitud(delta: Double) {
        updateClinicalState { state ->
            val baseLongitud = if (state.effectiveLongitudCm.isFinite() && state.effectiveLongitudCm >= CredEdiUiState.LONGITUD_MIN_VALID_CM) {
                state.effectiveLongitudCm
            } else {
                val ageM = state.edadEfectivaMeses.coerceIn(0, 60)
                when {
                    ageM <= 2 -> 58.0
                    ageM <= 6 -> 66.0
                    ageM <= 12 -> 75.0
                    ageM <= 24 -> 86.0
                    else -> 100.0
                }
            }
            val next = (((baseLongitud + delta) * 10.0).roundToInt() / 10.0).coerceAtLeast(CredEdiUiState.LONGITUD_MIN_VALID_CM)
            state.copy(
                longitudCm = next,
                longitudInput = next.formatDecimal(1)
            )
        }
    }

    fun setLongitudCm(longitud: Double) {
        updateClinicalState { state ->
            if (longitud.isFinite() && longitud.toFloat().isFinite() && longitud >= CredEdiUiState.LONGITUD_MIN_VALID_CM) {
                state.copy(
                    longitudCm = longitud,
                    longitudInput = longitud.formatDecimal(1)
                )
            } else {
                state.copy(
                    longitudCm = Double.NaN,
                    longitudInput = ""
                )
            }
        }
    }

    fun updatePerimetroCefalicoInput(raw: String) {
        val cleaned = sanitizeDecimal(raw)
        val isCompleteNumeric = cleaned.isNotEmpty() && !cleaned.endsWith(".")
        val parsed = if (isCompleteNumeric) cleaned.toDoubleOrNull() else null
        updateClinicalState { state ->
            if (parsed != null && parsed.isFinite() && parsed.toFloat().isFinite() && parsed >= 10.0) {
                state.copy(
                    perimetroCefalicoInput = cleaned,
                    perimetroCefalicoCm = parsed
                )
            } else {
                state.copy(
                    perimetroCefalicoInput = cleaned,
                    perimetroCefalicoCm = Double.NaN
                )
            }
        }
    }

    fun adjustPerimetroCefalico(delta: Double) {
        updateClinicalState { state ->
            val base = if (state.perimetroCefalicoCm.isFinite() && state.perimetroCefalicoCm >= 10.0) {
                state.perimetroCefalicoCm
            } else {
                39.5
            }
            val next = (((base + delta) * 10.0).roundToInt() / 10.0).coerceIn(25.0, 60.0)
            state.copy(
                perimetroCefalicoCm = next,
                perimetroCefalicoInput = next.formatDecimal(1)
            )
        }
    }

    fun setPerimetroCefalicoCm(pc: Double) {
        updateClinicalState { state ->
            if (pc.isFinite() && pc.toFloat().isFinite() && pc >= 10.0) {
                state.copy(
                    perimetroCefalicoCm = pc,
                    perimetroCefalicoInput = pc.formatDecimal(1)
                )
            } else {
                state.copy(
                    perimetroCefalicoCm = Double.NaN,
                    perimetroCefalicoInput = ""
                )
            }
        }
    }

    fun selectCurvaIndicador(indicador: CurvaIndicador) {
        _uiState.update { it.copy(selectedIndicador = indicador) }
    }

    fun openDatosEdiModal() {
        _uiState.update { it.copy(showDatosEdiModal = true) }
    }

    fun closeDatosEdiModal() {
        _uiState.update { it.copy(showDatosEdiModal = false) }
    }

    fun adjustEdiFechaEvalYear(deltaYears: Int) {
        updateClinicalState { state ->
            val base = if (state.ediFechaEval.isNotBlank()) state.ediFechaEval else state.fechaEvaluacion
            val newDateStr = ClinicalAgeCalculator.adjustYear(base, deltaYears)
            syncEdiTemplateQuestions(
                state.copy(
                    ediFechaEval = newDateStr,
                    fechaEvaluacion = newDateStr
                )
            )
        }
    }

    fun updateEdiFechaEval(value: String) {
        updateClinicalState { state ->
            val parsed = ClinicalAgeCalculator.parseDate(value)
            val updatedState = if (parsed != null) {
                val formatted = String.format(Locale.US, "%02d/%02d/%04d", parsed.day, parsed.month, parsed.year)
                state.copy(
                    ediFechaEval = value,
                    fechaEvaluacion = formatted
                )
            } else {
                state.copy(
                    ediFechaEval = value
                )
            }
            syncEdiTemplateQuestions(updatedState)
        }
    }

    fun updateEdiEgSemanasInput(value: String) {
        val digits = value.filter { it.isDigit() }.take(2)
        val parsed = digits.toIntOrNull()
        updateClinicalState { state ->
            if (parsed != null && parsed in 20..44) {
                val isPrematuro = parsed < 37
                val maturityCondition = if (isPrematuro) {
                    EdiBirthMaturityCondition.CONFIRMED_PRETERM
                } else {
                    EdiBirthMaturityCondition.CONFIRMED_TERM
                }
                val updatedFrb = state.frbItems.map { item ->
                    if (item.id == 1) {
                        if (isPrematuro) {
                            item.copy(title = "Prematurez < 37 semanas ($parsed sem EG)", checked = true)
                        } else {
                            item.copy(title = "A término ($parsed sem EG)", checked = false)
                        }
                    } else {
                        item
                    }
                }
                syncEdiTemplateQuestions(
                    state.copy(
                        ediEgSemanasInput = digits,
                        egSemanas = parsed,
                        ediEgRegistrada = true,
                        esPrematuro = isPrematuro,
                        ediBirthMaturityCondition = maturityCondition,
                        frbItems = updatedFrb
                    )
                )
            } else {
                state.copy(
                    ediEgSemanasInput = digits
                )
            }
        }
    }

    fun adjustEdiEgSemanas(delta: Int) {
        updateClinicalState { state ->
            val next = (state.egSemanas + delta).coerceIn(20, 44)
            val isPrematuro = next < 37
            val maturityCondition = if (isPrematuro) {
                EdiBirthMaturityCondition.CONFIRMED_PRETERM
            } else {
                EdiBirthMaturityCondition.CONFIRMED_TERM
            }
            val updatedFrb = state.frbItems.map { item ->
                if (item.id == 1) {
                    if (isPrematuro) {
                        item.copy(title = "Prematurez < 37 semanas ($next sem EG)", checked = true)
                    } else {
                        item.copy(title = "A término ($next sem EG)", checked = false)
                    }
                } else {
                    item
                }
            }
            syncEdiTemplateQuestions(
                state.copy(
                    egSemanas = next,
                    ediEgRegistrada = true,
                    ediEgSemanasInput = next.toString(),
                    esPrematuro = isPrematuro,
                    ediBirthMaturityCondition = maturityCondition,
                    frbItems = updatedFrb
                )
            )
        }
    }

    fun toggleFrbItem(id: Int) {
        _uiState.update { state ->
            val updated = state.frbItems.map { item ->
                if (item.id == id) item.copy(checked = !item.checked) else item
            }
            state.copy(
                frbItems = updated
            )
        }
    }

    fun setSaItemPresent(id: Int, isPresent: Boolean) {
        _uiState.update { state ->
            val updated = state.saItems.map { item ->
                if (item.id == id) item.copy(isPresent = isPresent) else item
            }
            state.copy(
                saItems = updated
            )
        }
    }

    fun setAreaItemAchieved(id: Int, isAchieved: Boolean) {
        _uiState.update { state ->
            val updated = state.areaItems.map { item ->
                if (item.id == id) item.copy(isAchieved = isAchieved) else item
            }
            state.copy(
                areaItems = updated
            )
        }
    }

    fun toggleEnTono() {
        _uiState.update { state ->
            state.copy(
                enTonoSimetrico = !state.enTonoSimetrico
            )
        }
    }

    fun toggleEnMoro() {
        _uiState.update { state ->
            state.copy(
                enMoroPresente = !state.enMoroPresente
            )
        }
    }

    fun toggleEnSuccion() {
        _uiState.update { state ->
            state.copy(
                enSuccionRitmica = !state.enSuccionRitmica
            )
        }
    }

    fun toggleEnPrension() {
        _uiState.update { state ->
            state.copy(
                enPrensionPresente = !state.enPrensionPresente
            )
        }
    }

    fun toggleEnMovilidadOcular() {
        _uiState.update { state ->
            state.copy(
                enMovilidadOcularNormal = !state.enMovilidadOcularNormal
            )
        }
    }

    fun guardarYCalcularSemaforoEdi() {
        _uiState.update { state ->
            state.copy(
                showDatosEdiModal = false
            )
        }
    }

    // --- Controladores Fase 4A: Motor Clínico EDI Peruano 2026 ---

    fun navigateEdiView(view: EdiInternalView) {
        _uiState.update { it.copy(ediInternalView = view) }
    }

    fun navigateEdiBack(): Boolean {
        val current = _uiState.value.ediInternalView
        if (current == EdiInternalView.SUMMARY_5_AXES) return false
        _uiState.update { it.copy(ediInternalView = EdiInternalView.SUMMARY_5_AXES) }
        return true
    }

    fun selectEdiAxis(axis: EdiAxis) {
        _uiState.update { it.copy(ediSelectedAxis = axis) }
    }

    fun openEdiAxisQuestionnaire(axis: EdiAxis) {
        _uiState.update {
            it.copy(
                ediSelectedAxis = axis,
                ediInternalView = EdiInternalView.QUESTIONNAIRE_AND_FALLBACK
            )
        }
    }

    fun selectEdiDevAreaFilter(area: EdiDevArea?) {
        _uiState.update { it.copy(ediSelectedDevAreaFilter = area) }
    }

    fun updateEdiGroupSearchQuery(query: String) {
        _uiState.update { it.copy(ediGroupSearchQuery = query) }
    }

    fun selectEdiGroupFilter(filter: EdiGroupFilterCategory) {
        _uiState.update { it.copy(ediGroupFilterCategory = filter) }
    }

    fun updateEgDias(dias: Int) {
        val clamped = dias.coerceIn(0, 6)
        updateClinicalState { state ->
            val syncCondition = if (!state.ediHasRegisteredEg &&
                state.ediEffectiveMaturityCondition != EdiBirthMaturityCondition.CONFIRMED_TERM
            ) {
                val isPreterm = state.egSemanas < 37
                state.copy(
                    egDias = clamped,
                    ediEgRegistrada = true,
                    esPrematuro = isPreterm,
                    ediBirthMaturityCondition = if (isPreterm) {
                        EdiBirthMaturityCondition.CONFIRMED_PRETERM
                    } else {
                        EdiBirthMaturityCondition.CONFIRMED_TERM
                    }
                )
            } else {
                state.copy(
                    egDias = clamped,
                    ediEgRegistrada = true
                )
            }
            syncEdi2026StateForAgeChange(syncCondition)
        }
    }

    fun adjustEgDias(delta: Int) {
        updateClinicalState { state ->
            val next = (state.egDias + delta).coerceIn(0, 6)
            val syncCondition = if (!state.ediHasRegisteredEg &&
                state.ediEffectiveMaturityCondition != EdiBirthMaturityCondition.CONFIRMED_TERM
            ) {
                val isPreterm = state.egSemanas < 37
                state.copy(
                    egDias = next,
                    ediEgRegistrada = true,
                    esPrematuro = isPreterm,
                    ediBirthMaturityCondition = if (isPreterm) {
                        EdiBirthMaturityCondition.CONFIRMED_PRETERM
                    } else {
                        EdiBirthMaturityCondition.CONFIRMED_TERM
                    }
                )
            } else {
                state.copy(
                    egDias = next,
                    ediEgRegistrada = true
                )
            }
            syncEdi2026StateForAgeChange(syncCondition)
        }
    }

    fun confirmEdiTermWithoutWeeks() {
        updateClinicalState { state ->
            val updatedFrb = state.frbItems.map { item ->
                if (item.id == 1) {
                    item.copy(title = "A término — semanas exactas no registradas", checked = false)
                } else {
                    item
                }
            }
            syncEdiTemplateQuestions(
                state.copy(
                    esPrematuro = false,
                    egSemanas = 32,
                    egDias = 0,
                    ediEgRegistrada = false,
                    ediEgSemanasInput = "32",
                    ediBirthMaturityCondition = EdiBirthMaturityCondition.CONFIRMED_TERM,
                    frbItems = updatedFrb
                )
            )
        }
    }

    fun clearEdiRegisteredEg() {
        updateClinicalState { state ->
            val updatedFrb = state.frbItems.map { item ->
                if (item.id == 1) {
                    item.copy(title = "Prematurez < 37 semanas (EG pendiente de registro)", checked = false)
                } else {
                    item
                }
            }
            syncEdiTemplateQuestions(
                state.copy(
                    esPrematuro = false,
                    egSemanas = 32,
                    egDias = 0,
                    ediEgRegistrada = false,
                    ediEgSemanasInput = "32",
                    ediBirthMaturityCondition = EdiBirthMaturityCondition.UNSPECIFIED,
                    frbItems = updatedFrb
                )
            )
        }
    }

    fun confirmEdiContradictionKeepRegisteredEg() {
        updateClinicalState { state ->
            if (!state.ediHasRegisteredEg) return@updateClinicalState state
            val isPreterm = state.egSemanas < 37
            val maturityCondition = if (isPreterm) {
                EdiBirthMaturityCondition.CONFIRMED_PRETERM
            } else {
                EdiBirthMaturityCondition.CONFIRMED_TERM
            }
            val updatedFrb = state.frbItems.map { item ->
                if (item.id == 1) {
                    if (isPreterm) {
                        item.copy(title = "Prematurez < 37 semanas (${state.egSemanas} sem EG)", checked = true)
                    } else {
                        item.copy(title = "A término (${state.egSemanas} sem EG)", checked = false)
                    }
                } else {
                    item
                }
            }
            syncEdiTemplateQuestions(
                state.copy(
                    esPrematuro = isPreterm,
                    ediBirthMaturityCondition = maturityCondition,
                    frbItems = updatedFrb
                )
            )
        }
    }

    fun confirmEdiContradictionAsPretermWithoutWeeks() {
        updateClinicalState { state ->
            val updatedFrb = state.frbItems.map { item ->
                if (item.id == 1) {
                    item.copy(title = "Prematurez < 37 semanas (EG exacta no registrada)", checked = false)
                } else {
                    item
                }
            }
            syncEdiTemplateQuestions(
                state.copy(
                    esPrematuro = true,
                    egSemanas = 32,
                    egDias = 0,
                    ediEgRegistrada = false,
                    ediEgSemanasInput = "32",
                    ediBirthMaturityCondition = EdiBirthMaturityCondition.CONFIRMED_PRETERM,
                    frbItems = updatedFrb
                )
            )
        }
    }

    fun selectEdiGroupFromBank(groupNumber: Int) {
        val safeGroup = groupNumber.coerceIn(1, 15)
        updateClinicalState { state ->
            val autoGroup = state.ediCorrectedAgeComparison.automaticallyAssignedGroup?.groupNumber
            val updatedEngine = EdiPhase1SessionManager.requestGroupChange(
                state = state.ediEngineState,
                targetGroupNumber = safeGroup,
                reason = "Selección manual desde el Banco de los 15 Grupos EDI"
            )
            val switchedImmediately = updatedEngine.pendingGroupChangeConfirmation == null
            val isManualOverride = if (switchedImmediately && autoGroup != null) {
                safeGroup != autoGroup
            } else {
                state.ediManualGroupOverride
            }
            state.copy(
                ediEngineState = updatedEngine,
                ediManualGroupOverride = isManualOverride
            )
        }
    }

    fun useAgeSuggestedEdiGroup() {
        updateClinicalState { state ->
            val autoGroup = state.ediCorrectedAgeComparison.automaticallyAssignedGroup?.groupNumber
                ?: return@updateClinicalState state
            val updatedEngine = EdiPhase1SessionManager.requestGroupChange(
                state = state.ediEngineState,
                targetGroupNumber = autoGroup,
                reason = "Restaurar grupo asignado por edad ($autoGroup)"
            )
            val switchedImmediately = updatedEngine.pendingGroupChangeConfirmation == null
            state.copy(
                ediEngineState = updatedEngine,
                ediManualGroupOverride = if (switchedImmediately) false else state.ediManualGroupOverride
            )
        }
    }

    fun resolveEdiPendingGroupChange(action: EdiGroupChangeResolutionAction) {
        updateClinicalState { state ->
            val originContext = state.ediEngineState.activeSessionOriginContext
                ?: state.buildCurrentEdiClinicalContext()
            val resolvedEngine = EdiPhase1SessionManager.confirmPendingGroupChange(
                state = state.ediEngineState,
                action = action,
                originContext = originContext
            )
            val autoGroup = state.ediCorrectedAgeComparison.automaticallyAssignedGroup?.groupNumber
            val isOverride = if (autoGroup != null) {
                resolvedEngine.activeGroupNumber != autoGroup
            } else {
                false
            }
            state.copy(
                ediEngineState = resolvedEngine,
                ediManualGroupOverride = isOverride
            )
        }
    }

    fun openEdiArchivedSessionPreview(archiveId: String) {
        _uiState.update { state ->
            state.copy(
                ediEngineState = EdiPhase1SessionManager.openArchivedSessionPreview(
                    state = state.ediEngineState,
                    archiveId = archiveId
                )
            )
        }
    }

    fun closeEdiArchivedSessionPreview() {
        _uiState.update { state ->
            state.copy(
                ediEngineState = EdiPhase1SessionManager.closeArchivedSessionPreview(
                    state = state.ediEngineState
                )
            )
        }
    }

    fun requestRestoreEdiArchivedSession(archiveId: String) {
        _uiState.update { state ->
            val entry = state.ediEngineState.archivedSessionHistory.find { it.archiveId == archiveId }
                ?: return@update state.copy(
                    ediEngineState = state.ediEngineState.copy(
                        pendingRestoreRequest = null,
                        lastRestoreStatusMessage = "No se encontró la sesión archivada solicitada ($archiveId)."
                    )
                )
            val compatibility = state.evaluateArchivedEntryCompatibility(entry)
            state.copy(
                ediEngineState = EdiPhase1SessionManager.requestRestoreArchivedSession(
                    state = state.ediEngineState,
                    archiveId = archiveId,
                    compatibility = compatibility
                )
            )
        }
    }

    fun setRestoreSamePatientContinuityConfirmed(confirmed: Boolean) {
        _uiState.update { state ->
            state.copy(
                ediEngineState = EdiPhase1SessionManager.setPendingRestoreSamePatientConfirmed(
                    state = state.ediEngineState,
                    confirmed = confirmed
                )
            )
        }
    }

    fun cancelRestoreEdiArchivedSession() {
        _uiState.update { state ->
            state.copy(
                ediEngineState = EdiPhase1SessionManager.cancelRestoreArchivedSession(
                    state = state.ediEngineState
                )
            )
        }
    }

    fun confirmRestoreEdiArchivedSession(
        confirmSamePatientContinuity: Boolean = false
    ) {
        updateClinicalState { state ->
            val activeOriginForAutoArchive = state.ediEngineState.activeSessionOriginContext
                ?: state.buildCurrentEdiClinicalContext()
            val effectiveConfirmation = confirmSamePatientContinuity ||
                state.ediEngineState.pendingRestoreRequest?.samePatientAndEpisodeConfirmed == true
            val restoredEngine = EdiPhase1SessionManager.confirmRestoreArchivedSession(
                state = state.ediEngineState,
                activeOriginContextForAutoArchive = activeOriginForAutoArchive,
                samePatientContinuityConfirmed = effectiveConfirmation
            )
            val autoGroup = state.ediCorrectedAgeComparison.automaticallyAssignedGroup?.groupNumber
            val isOverride = if (autoGroup != null) {
                restoredEngine.activeGroupNumber != autoGroup
            } else {
                false
            }
            state.copy(
                ediEngineState = restoredEngine,
                ediManualGroupOverride = isOverride
            )
        }
    }

    fun startNewEdiEvaluationEpisode(
        reason: String = "Es otro paciente — iniciar nuevo episodio"
    ) {
        updateClinicalState { state ->
            val origin = state.ediEngineState.activeSessionOriginContext
                ?: state.buildCurrentEdiClinicalContext()
            val autoGroup = state.ediCorrectedAgeComparison.automaticallyAssignedGroup?.groupNumber
                ?: state.ediEngineState.activeGroupNumber
            val updatedEngine = EdiPhase1SessionManager.startNewWorkEpisode(
                state = state.ediEngineState,
                targetGroupNumber = autoGroup,
                reason = reason,
                previousOriginContext = origin
            )
            state.copy(
                ediEngineState = updatedEngine,
                ediManualGroupOverride = false
            )
        }
    }

    private fun bindActiveOriginContextIfNeeded(
        state: CredEdiUiState,
        updatedEngine: com.example.model.EdiPhase1EngineState
    ): com.example.model.EdiPhase1EngineState {
        return if (
            updatedEngine.activeSession.hasRecordedClinicalResponses &&
            updatedEngine.activeSessionOriginContext == null
        ) {
            updatedEngine.copy(activeSessionOriginContext = state.buildCurrentEdiClinicalContext())
        } else {
            updatedEngine
        }
    }

    fun confirmEdiAgeDiscrepancyResolution(
        selectedGroupNumber: Int,
        justificationNote: String
    ) {
        val trimmedNote = justificationNote.trim()
        if (trimmedNote.isEmpty()) return
        updateClinicalState { state ->
            val comparison = state.ediCorrectedAgeComparison
            val validCandidate = comparison.candidateGroupsForProfessionalReview
                .any { it.groupNumber == selectedGroupNumber }
            if (!validCandidate) return@updateClinicalState state

            val resolution = EdiAgeDiscrepancyResolution(
                fechaNacimiento = state.fechaNacimiento,
                fechaEvaluacion = state.fechaEvaluacion,
                esPrematuro = state.esPrematuro,
                egSemanas = state.egSemanas,
                egDias = state.egDias,
                selectedGroupNumber = selectedGroupNumber,
                clinicalJustificationNote = trimmedNote
            )
            val updatedEngine = EdiPhase1SessionManager.requestGroupChange(
                state = state.ediEngineState,
                targetGroupNumber = selectedGroupNumber,
                reason = "Resolución profesional de discrepancia etaria: $trimmedNote"
            )
            state.copy(
                ediAgeDiscrepancyResolution = resolution,
                ediEngineState = updatedEngine,
                ediManualGroupOverride = false
            )
        }
    }

    fun recordEdiCurrentItemResponse(
        itemUniqueId: String,
        response: EdiItemResponse
    ) {
        updateClinicalState { state ->
            val cleanedDual = if (response != EdiItemResponse.DISCREPANCY_REQUIRES_HUMAN_RESOLUTION) {
                state.ediEngineState.copy(
                    activeSession = state.ediEngineState.activeSession.copy(
                        dualEvidenceRecords = state.ediEngineState.activeSession.dualEvidenceRecords - itemUniqueId
                    )
                )
            } else {
                state.ediEngineState
            }
            val updated = EdiPhase1SessionManager.recordCurrentGroupItemResponse(
                state = cleanedDual,
                itemUniqueId = itemUniqueId,
                response = response
            )
            state.copy(ediEngineState = bindActiveOriginContextIfNeeded(state, updated))
        }
    }

    fun recordEdiDualEvidence(
        itemUniqueId: String,
        observationEvidence: Boolean?,
        interviewEvidence: Boolean?,
        humanResolutionIfConflict: Boolean? = null,
        note: String = ""
    ) {
        updateClinicalState { state ->
            val updated = EdiPhase1SessionManager.recordDualModalityEvidence(
                state = state.ediEngineState,
                itemUniqueId = itemUniqueId,
                observationEvidence = observationEvidence,
                interviewEvidence = interviewEvidence,
                humanResolutionIfConflict = humanResolutionIfConflict,
                note = note
            )
            state.copy(ediEngineState = bindActiveOriginContextIfNeeded(state, updated))
        }
    }

    fun recordEdiFallbackResponse(
        fallbackUniqueId: String,
        response: EdiItemResponse
    ) {
        updateClinicalState { state ->
            val cleanedDual = if (response != EdiItemResponse.DISCREPANCY_REQUIRES_HUMAN_RESOLUTION) {
                state.ediEngineState.copy(
                    activeSession = state.ediEngineState.activeSession.copy(
                        dualEvidenceRecords = state.ediEngineState.activeSession.dualEvidenceRecords - fallbackUniqueId
                    )
                )
            } else {
                state.ediEngineState
            }
            val updated = EdiAreaFallbackEngine.recordValidatedFallbackResponse(
                state = cleanedDual,
                fallbackUniqueId = fallbackUniqueId,
                response = response
            )
            state.copy(ediEngineState = bindActiveOriginContextIfNeeded(state, updated))
        }
    }

    fun recordEdiFallbackDualEvidence(
        fallbackUniqueId: String,
        observationEvidence: Boolean?,
        interviewEvidence: Boolean?,
        humanResolutionIfConflict: Boolean? = null,
        note: String = ""
    ) {
        updateClinicalState { state ->
            val updated = EdiAreaFallbackEngine.recordDualModalityFallbackEvidence(
                state = state.ediEngineState,
                fallbackUniqueId = fallbackUniqueId,
                observationEvidence = observationEvidence,
                interviewEvidence = interviewEvidence,
                humanResolutionIfConflict = humanResolutionIfConflict,
                note = note
            )
            state.copy(ediEngineState = bindActiveOriginContextIfNeeded(state, updated))
        }
    }

    fun updateEdiItemClinicalNote(
        itemUniqueId: String,
        note: String
    ) {
        updateClinicalState { state ->
            val session = state.ediEngineState.activeSession
            val updatedNotes = if (note.isBlank()) {
                session.itemClinicalNotes - itemUniqueId
            } else {
                session.itemClinicalNotes + (itemUniqueId to note)
            }
            val updatedEngine = state.ediEngineState.copy(
                activeSession = session.copy(itemClinicalNotes = updatedNotes)
            )
            state.copy(
                ediEngineState = bindActiveOriginContextIfNeeded(state, updatedEngine)
            )
        }
    }

    companion object {
        const val DEFAULT_AUTOSAVE_DEBOUNCE_MS = 120L

        fun provideFactory(
            context: Context,
            autoRestoreOnInit: Boolean = true,
            autosaveDebounceMs: Long = DEFAULT_AUTOSAVE_DEBOUNCE_MS
        ): ViewModelProvider.Factory {
            val appContext = context.applicationContext
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val database = CredEdiDatabase.getInstance(appContext)
                    val repository = CredEdiPersistenceRepository(
                        evaluationDao = database.evaluationDao(),
                        cipherProvider = AndroidKeyStoreAesGcmCipherProvider()
                    )
                    return CredEdiViewModel(
                        persistenceRepository = repository,
                        autoRestoreOnInit = autoRestoreOnInit,
                        autosaveDebounceMs = autosaveDebounceMs
                    ) as T
                }
            }
        }
    }
}

fun Double.formatDecimal(decimals: Int): String =
    if (!this.isFinite()) "N/D" else String.format(Locale.US, "%.${decimals}f", this)

fun Double.formatSignedDecimal(decimals: Int): String {
    if (!this.isFinite()) return "N/D"
    val formatted = String.format(Locale.US, "%.${decimals}f", this)
    return if (this > 0 && !formatted.startsWith("+")) "+$formatted" else formatted
}
