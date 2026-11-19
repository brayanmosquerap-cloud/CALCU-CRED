package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.CredEdiUiState
import com.example.model.EdiBirthMaturityCondition
import com.example.model.EdiClinicalModality
import com.example.model.EdiGlobalTrafficLight
import com.example.model.EdiItemResponse
import com.example.model.EdiOfficialCatalog
import com.example.model.EdiPhase1SessionManager
import com.example.model.EvaluationPersistenceStatus
import com.example.model.GlobalDiagnosisCompleteness
import com.example.model.PatientIdentificationData
import com.example.model.PerimetroCefalicoCaptureState
import com.example.model.SexoBiologico
import com.example.persistence.CredEdiCipherProvider
import com.example.persistence.CredEdiDatabase
import com.example.persistence.CredEdiPersistenceRepository
import com.example.persistence.JvmAesGcmCipherProvider
import com.example.persistence.LoadEvaluationResult
import com.example.persistence.SaveEvaluationResult
import com.example.util.EdiPdfExporter
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.security.GeneralSecurityException

/**
 * MEDIO-06 — LOTE 1:
 * Suite de pruebas automatizadas para el serializador clínico (`CredEdiClinicalSnapshotCodec`),
 * cifrado autenticado (`AES-256-GCM` + `SHA-256`) y persistencia transaccional en Room
 * (`CredEdiDatabase`, `EvaluationDao`, `CredEdiPersistenceRepository`).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class EdiMedio06Lote1PersistenceCodecAndRoomTest {

    private lateinit var context: Context
    private lateinit var database: CredEdiDatabase
    private lateinit var cipherProvider: JvmAesGcmCipherProvider
    private lateinit var repository: CredEdiPersistenceRepository
    private var simulatedClockMillis: Long = 1_750_000_000_000L

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = CredEdiDatabase.createInMemoryForTesting(context)
        cipherProvider = JvmAesGcmCipherProvider()
        repository = CredEdiPersistenceRepository(
            evaluationDao = database.evaluationDao(),
            cipherProvider = cipherProvider,
            clockEpochMillis = {
                simulatedClockMillis += 1_000L
                simulatedClockMillis
            }
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    /**
     * Construye un estado completo y definitivo en Verde para el Grupo 8 (14 meses, nacido a término).
     */
    private fun buildCompleteGreenGroup8State(
        evaluationUuid: String = "EVAL_G8_GREEN_001",
        patientAlias: String = "Paciente G8 Verde"
    ): CredEdiUiState {
        val groupNumber = 8
        val spec = EdiOfficialCatalog.getGroup(groupNumber)
        var engine = EdiPhase1SessionManager.createInitialState(groupNumber)

        // FRB todos NO
        EdiOfficialCatalog.frbItems.forEach { item ->
            engine = EdiPhase1SessionManager.recordCurrentGroupItemResponse(engine, item.uniqueId, EdiItemResponse.NO)
        }
        // ALE todos NO
        spec.aleItems.forEach { item ->
            engine = EdiPhase1SessionManager.recordCurrentGroupItemResponse(engine, item.uniqueId, EdiItemResponse.NO)
        }
        // Áreas todos SÍ
        spec.areaItems.values.flatten().forEach { item ->
            engine = EdiPhase1SessionManager.recordCurrentGroupItemResponse(engine, item.uniqueId, EdiItemResponse.YES)
        }
        // EN todos NO (normales)
        EdiOfficialCatalog.getApplicableEnItems(groupNumber).forEach { item ->
            engine = EdiPhase1SessionManager.recordCurrentGroupItemResponse(engine, item.uniqueId, EdiItemResponse.NO)
        }
        // ALA todos NO
        spec.alaItems.forEach { item ->
            engine = EdiPhase1SessionManager.recordCurrentGroupItemResponse(engine, item.uniqueId, EdiItemResponse.NO)
        }
        // Nota clínica de trazabilidad en el primer reactivo MG
        val firstMgId = spec.areaItems.values.flatten().first().uniqueId
        engine = engine.copy(
            activeSession = engine.activeSession.copy(
                itemClinicalNotes = engine.activeSession.itemClinicalNotes +
                    (firstMgId to "Marcha independiente observada en consultorio CRED.")
            )
        )

        return CredEdiUiState(
            evaluationUuid = evaluationUuid,
            patientIdentification = PatientIdentificationData(
                patientIdentifierOrAlias = patientAlias,
                clinicalRecordCode = "HC-2026-008",
                generalClinicalNote = "Control CRED de 14 meses sin factores de riesgo."
            ),
            sexo = SexoBiologico.NINO,
            fechaNacimiento = "15/04/2025",
            fechaEvaluacion = "15/06/2026",
            esPrematuro = false,
            egSemanas = 39,
            egDias = 2,
            ediEgRegistrada = true,
            ediBirthMaturityCondition = EdiBirthMaturityCondition.CONFIRMED_TERM,
            pesoInput = "10.2",
            pesoKg = 10.2,
            longitudInput = "78.0",
            longitudCm = 78.0,
            perimetroCefalicoInput = "46.5",
            perimetroCefalicoCm = 46.5,
            ediEngineState = engine
        )
    }

    // =========================================================================
    // 1. Guardar y recuperar evaluación incompleta (prohibido promover a completada)
    // =========================================================================
    @Test
    fun `test01 - guardar y recuperar evaluacion incompleta sin promoverla a completada`() = runBlocking {
        val groupSpec = EdiOfficialCatalog.getGroup(8)
        var engine = EdiPhase1SessionManager.createInitialState(8)
        val firstMg = groupSpec.areaItems.values.flatten().first()
        engine = EdiPhase1SessionManager.recordCurrentGroupItemResponse(engine, firstMg.uniqueId, EdiItemResponse.YES)

        val incompleteState = CredEdiUiState(
            evaluationUuid = "EVAL_INCOMPLETE_001",
            patientIdentification = PatientIdentificationData(
                patientIdentifierOrAlias = "Lactante Evaluación Parcial",
                clinicalRecordCode = "EXP-101"
            ),
            sexo = SexoBiologico.NINA,
            fechaNacimiento = "15/04/2025",
            fechaEvaluacion = "15/06/2026",
            esPrematuro = false,
            ediBirthMaturityCondition = EdiBirthMaturityCondition.CONFIRMED_TERM,
            ediEngineState = engine
        )

        assertFalse(incompleteState.ediGlobalEvaluation.isDefinitive)
        assertEquals(EdiGlobalTrafficLight.INCOMPLETE, incompleteState.ediGlobalEvaluation.trafficLight)

        // Incluso si se intenta solicitar COMPLETED por error, el repositorio debe degradarla a INCOMPLETE
        val saveResult = repository.saveEvaluation(
            state = incompleteState,
            requestedStatus = EvaluationPersistenceStatus.COMPLETED,
            markAsActiveDraft = true
        )
        assertTrue(saveResult is SaveEvaluationResult.Success)
        val success = saveResult as SaveEvaluationResult.Success
        assertEquals(EvaluationPersistenceStatus.INCOMPLETE, success.persistedStatus)

        val loadResult = repository.loadEvaluationByUuid("EVAL_INCOMPLETE_001")
        assertTrue(loadResult is LoadEvaluationResult.Success)
        val restored = (loadResult as LoadEvaluationResult.Success).record

        assertEquals(EvaluationPersistenceStatus.INCOMPLETE, restored.summary.status)
        assertFalse(restored.summary.isDefinitiveSemaphore)
        assertFalse(restored.restoredUiState.ediGlobalEvaluation.isDefinitive)
        assertEquals(EdiGlobalTrafficLight.INCOMPLETE, restored.restoredUiState.ediGlobalEvaluation.trafficLight)
        assertEquals(
            EdiItemResponse.YES,
            restored.restoredUiState.ediEngineState.activeSession.currentGroupResponses[firstMg.uniqueId]
        )
    }

    // =========================================================================
    // 2. Guardar y recuperar evaluación completa en Verde (Grupo 8)
    // =========================================================================
    @Test
    fun `test02 - guardar y recuperar evaluacion completa en Verde Grupo 8 intacta`() = runBlocking {
        val greenState = buildCompleteGreenGroup8State()
        assertTrue(greenState.ediGlobalEvaluation.isDefinitive)
        assertEquals(EdiGlobalTrafficLight.VERDE, greenState.ediGlobalEvaluation.trafficLight)

        val saveResult = repository.saveEvaluation(greenState)
        assertTrue(saveResult is SaveEvaluationResult.Success)
        val saved = saveResult as SaveEvaluationResult.Success
        assertEquals(EvaluationPersistenceStatus.COMPLETED, saved.persistedStatus)

        val loadResult = repository.loadEvaluationByUuid(greenState.evaluationUuid)
        assertTrue(loadResult is LoadEvaluationResult.Success)
        val restoredRecord = (loadResult as LoadEvaluationResult.Success).record
        val restoredState = restoredRecord.restoredUiState

        assertEquals(EvaluationPersistenceStatus.COMPLETED, restoredRecord.summary.status)
        assertTrue(restoredRecord.summary.isDefinitiveSemaphore)
        assertEquals(EdiGlobalTrafficLight.VERDE.name, restoredRecord.summary.semaphoreCode)
        assertEquals("Paciente G8 Verde", restoredState.patientIdentification.patientIdentifierOrAlias)
        assertEquals("HC-2026-008", restoredState.patientIdentification.clinicalRecordCode)
        assertTrue(restoredState.ediGlobalEvaluation.isDefinitive)
        assertEquals(EdiGlobalTrafficLight.VERDE, restoredState.ediGlobalEvaluation.trafficLight)
        assertEquals(
            greenState.ediEngineState.activeSession.currentGroupResponses,
            restoredState.ediEngineState.activeSession.currentGroupResponses
        )
        assertEquals(
            greenState.ediEngineState.activeSession.itemClinicalNotes,
            restoredState.ediEngineState.activeSession.itemClinicalNotes
        )
    }

    // =========================================================================
    // 3. Guardar y recuperar evaluación en Rojo con causas y alertas críticas
    // =========================================================================
    @Test
    fun `test03 - guardar y recuperar evaluacion en Rojo con causas y alertas criticas prioritarias`() = runBlocking {
        val baseGreen = buildCompleteGreenGroup8State(
            evaluationUuid = "EVAL_G8_RED_003",
            patientAlias = "Paciente G8 Rojo Alerta"
        )
        val spec = EdiOfficialCatalog.getGroup(8)
        val firstAla = spec.alaItems.first()
        val firstEn = EdiOfficialCatalog.getApplicableEnItems(8).first()

        var redEngine = EdiPhase1SessionManager.recordCurrentGroupItemResponse(
            baseGreen.ediEngineState,
            firstAla.uniqueId,
            EdiItemResponse.YES
        )
        redEngine = EdiPhase1SessionManager.recordCurrentGroupItemResponse(
            redEngine,
            firstEn.uniqueId,
            EdiItemResponse.YES
        )

        val redState = baseGreen.copy(ediEngineState = redEngine)
        val evalBefore = redState.ediGlobalEvaluation
        assertTrue(evalBefore.isDefinitive)
        assertEquals(EdiGlobalTrafficLight.ROJO, evalBefore.trafficLight)
        assertTrue(evalBefore.hasPriorityCriticalWarning)
        assertTrue(evalBefore.priorityCriticalAlerts.isNotEmpty())

        val saveResult = repository.saveEvaluation(redState)
        assertTrue(saveResult is SaveEvaluationResult.Success)

        val loaded = repository.loadEvaluationByUuid("EVAL_G8_RED_003") as LoadEvaluationResult.Success
        val evalAfter = loaded.record.restoredUiState.ediGlobalEvaluation

        assertTrue(evalAfter.isDefinitive)
        assertEquals(EdiGlobalTrafficLight.ROJO, evalAfter.trafficLight)
        assertTrue(evalAfter.hasPriorityCriticalWarning)
        assertEquals(evalBefore.priorityCriticalAlerts.size, evalAfter.priorityCriticalAlerts.size)
        assertEquals(
            evalBefore.causes.map { it.type to it.involvedCodes },
            evalAfter.causes.map { it.type to it.involvedCodes }
        )
    }

    // =========================================================================
    // 4. Cambiar una respuesta y verificar que el nuevo guardado actualiza revisión
    // =========================================================================
    @Test
    fun `test04 - cambiar respuesta y re-guardar actualiza estado y revision monotonica`() = runBlocking {
        val initial = buildCompleteGreenGroup8State(evaluationUuid = "EVAL_MUTATE_004")
        val save1 = repository.saveEvaluation(initial) as SaveEvaluationResult.Success
        assertEquals(1L, save1.monotonicRevision)

        val spec = EdiOfficialCatalog.getGroup(8)
        val firstAla = spec.alaItems.first()
        val updatedEngine = EdiPhase1SessionManager.recordCurrentGroupItemResponse(
            initial.ediEngineState,
            firstAla.uniqueId,
            EdiItemResponse.YES
        )
        val updatedState = initial.copy(ediEngineState = updatedEngine)
        val save2 = repository.saveEvaluation(updatedState) as SaveEvaluationResult.Success
        assertEquals(2L, save2.monotonicRevision)
        assertNotEquals(save1.plaintextSha256, save2.plaintextSha256)

        val loaded = repository.loadEvaluationByUuid("EVAL_MUTATE_004") as LoadEvaluationResult.Success
        assertEquals(2L, loaded.record.summary.monotonicRevision)
        assertEquals(EdiGlobalTrafficLight.ROJO, loaded.record.restoredUiState.ediGlobalEvaluation.trafficLight)
        assertEquals(
            EdiItemResponse.YES,
            loaded.record.restoredUiState.ediEngineState.activeSession.currentGroupResponses[firstAla.uniqueId]
        )
    }

    // =========================================================================
    // 5. Guardar y recuperar reactivos con "No evaluado por falta de material" y evidencia dual
    // =========================================================================
    @Test
    fun `test05 - guardar y recuperar reactivos con falta de material retroceso y evidencia dual`() = runBlocking {
        val groupNumber = 12
        val spec = EdiOfficialCatalog.getGroup(groupNumber)
        var engine = EdiPhase1SessionManager.createInitialState(groupNumber)

        val itemWithMaterial = spec.areaItems.values.flatten().first { it.requiresMaterialInProtocol }
        engine = EdiPhase1SessionManager.recordCurrentGroupItemResponse(
            engine,
            itemWithMaterial.uniqueId,
            EdiItemResponse.NOT_EVALUATED_MISSING_MATERIAL
        )

        // Retroceso al Grupo 11
        val prevSpec = EdiOfficialCatalog.getGroup(11)
        val prevMgItem = prevSpec.areaItems.values.flatten().first()
        engine = EdiPhase1SessionManager.recordFallbackItemResponse(
            engine,
            prevMgItem.uniqueId,
            EdiItemResponse.NO
        )

        // Evidencia dual en ALE 12.3 con discrepancia pendiente de resolución humana
        val ale123 = spec.aleItems.first { it.modality == EdiClinicalModality.DUAL_OBSERVATION_AND_INTERVIEW_ALE_12_3 }
        engine = EdiPhase1SessionManager.recordDualModalityEvidence(
            state = engine,
            itemUniqueId = ale123.uniqueId,
            observationEvidence = true,
            interviewEvidence = false,
            humanResolutionIfConflict = null,
            note = "Observación directa difiere de reporte materno."
        )

        val stateWithSpecialResponses = CredEdiUiState(
            evaluationUuid = "EVAL_SPECIAL_005",
            sexo = SexoBiologico.NINA,
            fechaNacimiento = "10/01/2024",
            fechaEvaluacion = "15/06/2026",
            esPrematuro = false,
            ediEngineState = engine
        )

        val saveRes = repository.saveEvaluation(stateWithSpecialResponses)
        assertTrue(saveRes is SaveEvaluationResult.Success)

        val loaded = (repository.loadEvaluationByUuid("EVAL_SPECIAL_005") as LoadEvaluationResult.Success).record.restoredUiState
        val restoredSession = loaded.ediEngineState.activeSession

        assertEquals(
            EdiItemResponse.NOT_EVALUATED_MISSING_MATERIAL,
            restoredSession.currentGroupResponses[itemWithMaterial.uniqueId]
        )
        assertEquals(
            EdiItemResponse.NO,
            restoredSession.fallbackGroupResponses[prevMgItem.uniqueId]
        )
        assertEquals(
            EdiItemResponse.DISCREPANCY_REQUIRES_HUMAN_RESOLUTION,
            restoredSession.currentGroupResponses[ale123.uniqueId]
        )
        val restoredDual = restoredSession.dualEvidenceRecords[ale123.uniqueId]
        assertNotNull(restoredDual)
        assertEquals(true, restoredDual?.directObservationEvidence)
        assertEquals(false, restoredDual?.directedInterviewEvidence)
        assertEquals(null, restoredDual?.professionalHumanResolution)
        assertEquals("Observación directa difiere de reporte materno.", restoredDual?.clinicalJustificationNote)
    }

    // =========================================================================
    // 6. Guardar dos pacientes distintos y recuperar cada uno sin mezcla de datos
    // =========================================================================
    @Test
    fun `test06 - guardar dos pacientes distintos sin mezcla de datos y con aislamiento criptografico AAD`() = runBlocking {
        val patientA = buildCompleteGreenGroup8State(
            evaluationUuid = "EVAL_PATIENT_A",
            patientAlias = "Mateo Quispe (14m)"
        ).copy(
            pesoInput = "10.4",
            pesoKg = 10.4,
            longitudInput = "78.5",
            longitudCm = 78.5
        )

        var engineB = EdiPhase1SessionManager.createInitialState(4)
        val specG4 = EdiOfficialCatalog.getGroup(4)
        val alaG4 = specG4.alaItems.first()
        engineB = EdiPhase1SessionManager.recordCurrentGroupItemResponse(engineB, alaG4.uniqueId, EdiItemResponse.YES)

        val patientB = CredEdiUiState(
            evaluationUuid = "EVAL_PATIENT_B",
            patientIdentification = PatientIdentificationData(
                patientIdentifierOrAlias = "Valeria Huamán (4m)",
                clinicalRecordCode = "HC-2026-402",
                generalClinicalNote = "Lactante de 4 meses en evaluación."
            ),
            sexo = SexoBiologico.NINA,
            fechaNacimiento = "15/02/2026",
            fechaEvaluacion = "15/06/2026",
            esPrematuro = true,
            egSemanas = 34,
            egDias = 3,
            ediEgRegistrada = true,
            ediBirthMaturityCondition = EdiBirthMaturityCondition.CONFIRMED_PRETERM,
            pesoInput = "5.8",
            pesoKg = 5.8,
            longitudInput = "60.2",
            longitudCm = 60.2,
            ediEngineState = engineB
        )

        assertTrue(repository.saveEvaluation(patientA, markAsActiveDraft = false) is SaveEvaluationResult.Success)
        assertTrue(repository.saveEvaluation(patientB, markAsActiveDraft = true) is SaveEvaluationResult.Success)

        // Verificar que las columnas cifradas en SQLite no exponen los nombres en texto claro
        val rawEntityA = database.evaluationDao().getByUuid("EVAL_PATIENT_A")!!
        val rawEntityB = database.evaluationDao().getByUuid("EVAL_PATIENT_B")!!
        assertFalse(rawEntityA.encryptedPatientHeader.contains("Mateo"))
        assertFalse(rawEntityA.encryptedClinicalPayload.contains("Mateo"))
        assertFalse(rawEntityB.encryptedPatientHeader.contains("Valeria"))
        assertFalse(rawEntityB.encryptedClinicalPayload.contains("Valeria"))

        // Recuperar ambos pacientes y confirmar ausencia absoluta de mezcla
        val loadedA = (repository.loadEvaluationByUuid("EVAL_PATIENT_A") as LoadEvaluationResult.Success).record.restoredUiState
        val loadedB = (repository.loadEvaluationByUuid("EVAL_PATIENT_B") as LoadEvaluationResult.Success).record.restoredUiState

        assertEquals("Mateo Quispe (14m)", loadedA.patientIdentification.patientIdentifierOrAlias)
        assertEquals(SexoBiologico.NINO, loadedA.sexo)
        assertEquals(8, loadedA.ediEngineState.activeGroupNumber)
        assertEquals(10.4, loadedA.effectivePesoKg, 0.001)
        assertEquals(EdiGlobalTrafficLight.VERDE, loadedA.ediGlobalEvaluation.trafficLight)

        assertEquals("Valeria Huamán (4m)", loadedB.patientIdentification.patientIdentifierOrAlias)
        assertEquals(SexoBiologico.NINA, loadedB.sexo)
        assertEquals(4, loadedB.ediEngineState.activeGroupNumber)
        assertEquals(5.8, loadedB.effectivePesoKg, 0.001)
        assertTrue(loadedB.esPrematuro)
        assertEquals(34, loadedB.egSemanas)
        assertEquals(3, loadedB.egDias)
        assertEquals(
            EdiItemResponse.YES,
            loadedB.ediEngineState.activeSession.currentGroupResponses[alaG4.uniqueId]
        )

        // Verificar que si un atacante o corrupción intenta asociar el payload cifrado de A al UUID de B, falla AAD GCM
        val tamperedB = rawEntityB.copy(
            encryptedClinicalPayload = rawEntityA.encryptedClinicalPayload,
            plaintextSha256 = rawEntityA.plaintextSha256
        )
        database.evaluationDao().insertOrReplaceInternal(tamperedB)
        val tamperedLoad = repository.loadEvaluationByUuid("EVAL_PATIENT_B")
        assertTrue(tamperedLoad is LoadEvaluationResult.CorruptedOrUnreadable)
    }

    // =========================================================================
    // 7. Eliminar una evaluación del historial
    // =========================================================================
    @Test
    fun `test07 - eliminar una evaluacion del historial sin afectar las demas`() = runBlocking {
        val state1 = buildCompleteGreenGroup8State(evaluationUuid = "EVAL_DEL_1", patientAlias = "Paciente 1")
        val state2 = buildCompleteGreenGroup8State(evaluationUuid = "EVAL_DEL_2", patientAlias = "Paciente 2")

        repository.saveEvaluation(state1, markAsActiveDraft = false)
        repository.saveEvaluation(state2, markAsActiveDraft = true)
        assertEquals(2, repository.listSavedEvaluationSummaries().size)

        val deleted = repository.deleteEvaluation("EVAL_DEL_1")
        assertTrue(deleted)

        assertTrue(repository.loadEvaluationByUuid("EVAL_DEL_1") is LoadEvaluationResult.NotFound)
        val remaining = repository.listSavedEvaluationSummaries()
        assertEquals(1, remaining.size)
        assertEquals("EVAL_DEL_2", remaining.first().evaluationUuid)
    }

    // =========================================================================
    // 8. Iniciar una nueva evaluación sin borrar el historial anterior
    // =========================================================================
    @Test
    fun `test08 - iniciar nueva evaluacion asigna nuevo UUID y conserva historial previo`() = runBlocking {
        val prevEvaluation = buildCompleteGreenGroup8State(
            evaluationUuid = "EVAL_PREV_008",
            patientAlias = "Paciente Previo Conservado"
        )
        repository.saveEvaluation(prevEvaluation, markAsActiveDraft = true)

        val newUuid = repository.generateNewEvaluationUuid()
        assertNotEquals("EVAL_PREV_008", newUuid)
        val freshEvaluation = CredEdiUiState(
            evaluationUuid = newUuid,
            patientIdentification = PatientIdentificationData(patientIdentifierOrAlias = "Nuevo Paciente En Blanco"),
            fechaNacimiento = "01/01/2025",
            fechaEvaluacion = "15/06/2026"
        )
        repository.saveEvaluation(freshEvaluation, markAsActiveDraft = true)

        val summaries = repository.listSavedEvaluationSummaries()
        assertEquals(2, summaries.size)
        val activeDrafts = summaries.filter { it.isActiveDraft }
        assertEquals(1, activeDrafts.size)
        assertEquals(newUuid, activeDrafts.first().evaluationUuid)

        // La evaluación previa continúa intacta en el historial
        val loadedPrev = repository.loadEvaluationByUuid("EVAL_PREV_008")
        assertTrue(loadedPrev is LoadEvaluationResult.Success)
        assertEquals(
            "Paciente Previo Conservado",
            (loadedPrev as LoadEvaluationResult.Success).record.restoredUiState.patientIdentification.patientIdentifierOrAlias
        )
    }

    // =========================================================================
    // 9. Guardar y recuperar antropometría vacía, parcial y válida (preserva PC/E-01 y ANTRO-02)
    // =========================================================================
    @Test
    fun `test09 - guardar y recuperar antropometria vacia parcial y valida preservando PCE01 y ANTRO02`() = runBlocking {
        // Caso 9A: 14 meses nacido a término con peso, talla y PC completamente vacíos
        val emptyAntroState = CredEdiUiState(
            evaluationUuid = "EVAL_ANTRO_EMPTY",
            sexo = SexoBiologico.NINO,
            fechaNacimiento = "15/04/2025",
            fechaEvaluacion = "15/06/2026",
            esPrematuro = false,
            ediBirthMaturityCondition = EdiBirthMaturityCondition.CONFIRMED_TERM,
            pesoInput = "",
            pesoKg = Double.NaN,
            longitudInput = "",
            longitudCm = Double.NaN,
            perimetroCefalicoInput = "",
            perimetroCefalicoCm = Double.NaN
        )

        repository.saveEvaluation(emptyAntroState)
        val restoredEmpty = (repository.loadEvaluationByUuid("EVAL_ANTRO_EMPTY") as LoadEvaluationResult.Success)
            .record.restoredUiState

        assertEquals("", restoredEmpty.pesoInput)
        assertEquals("", restoredEmpty.longitudInput)
        assertEquals("", restoredEmpty.perimetroCefalicoInput)
        assertTrue(restoredEmpty.pesoKg.isNaN())
        assertTrue(restoredEmpty.longitudCm.isNaN())
        assertTrue(restoredEmpty.perimetroCefalicoCm.isNaN())
        assertTrue(restoredEmpty.effectivePesoKg.isNaN())
        assertTrue(restoredEmpty.effectiveLongitudCm.isNaN())
        assertTrue(restoredEmpty.effectivePerimetroCefalicoCm.isNaN())
        assertFalse(restoredEmpty.hasPesoIngresado)
        assertFalse(restoredEmpty.hasLongitudIngresada)
        assertFalse(restoredEmpty.hasPerimetroCefalicoIngresado)
        assertEquals(0, restoredEmpty.datosAntropometricosCount)
        assertEquals(PerimetroCefalicoCaptureState.EMPTY_UNREGISTERED, restoredEmpty.perimetroCefalicoCaptureState)
        assertEquals(GlobalDiagnosisCompleteness.UNAVAILABLE, restoredEmpty.globalDiagnosis.completeness)
        assertFalse(restoredEmpty.evalPE.isReferenceAvailable)
        assertFalse(restoredEmpty.evalTE.isReferenceAvailable)
        assertFalse(restoredEmpty.evalPCE.isReferenceAvailable)
        assertEquals("Sin medición", restoredEmpty.evalPE.shortClassification)
        assertEquals("Sin medición", restoredEmpty.evalTE.shortClassification)
        assertEquals("Sin medición", restoredEmpty.evalPCE.shortClassification)
        assertFalse(restoredEmpty.globalDiagnosis.title.contains("Desnutrición", ignoreCase = true))
        assertFalse(restoredEmpty.evalPCE.fullClassification.contains("Microcefalia", ignoreCase = true))

        // Caso 9B: Solo Peso registrado (10.1 kg), Talla y PC vacíos
        val partialAntroState = emptyAntroState.copy(
            evaluationUuid = "EVAL_ANTRO_PARTIAL",
            pesoInput = "10.1",
            pesoKg = 10.1
        )
        repository.saveEvaluation(partialAntroState)
        val restoredPartial = (repository.loadEvaluationByUuid("EVAL_ANTRO_PARTIAL") as LoadEvaluationResult.Success)
            .record.restoredUiState

        assertTrue(restoredPartial.hasPesoIngresado)
        assertEquals(10.1, restoredPartial.effectivePesoKg, 0.001)
        assertFalse(restoredPartial.hasLongitudIngresada)
        assertTrue(restoredPartial.effectiveLongitudCm.isNaN())
        assertFalse(restoredPartial.hasPerimetroCefalicoIngresado)
        assertTrue(restoredPartial.effectivePerimetroCefalicoCm.isNaN())
        assertEquals(1, restoredPartial.datosAntropometricosCount)
        assertEquals(GlobalDiagnosisCompleteness.PARTIAL, restoredPartial.globalDiagnosis.completeness)
    }

    // =========================================================================
    // 10. Simular reinicio del proceso y verificar recuperación del borrador activo
    // =========================================================================
    @Test
    fun `test10 - simular reinicio del proceso recupera borrador activo desde Room`() = runBlocking {
        val stateBeforeProcessDeath = buildCompleteGreenGroup8State(
            evaluationUuid = "EVAL_PROCESS_RESTART_010",
            patientAlias = "Paciente Recuperado Tras Reinicio"
        )
        val saveRes = repository.saveEvaluation(stateBeforeProcessDeath, markAsActiveDraft = true)
        assertTrue(saveRes is SaveEvaluationResult.Success)

        // Simular reinicio del proceso creando una nueva instancia de CredEdiPersistenceRepository
        // que se conecta al mismo almacenamiento Room y a la misma clave maestra
        val newRepositoryAfterProcessDeath = CredEdiPersistenceRepository(
            evaluationDao = database.evaluationDao(),
            cipherProvider = cipherProvider
        )

        val activeDraftLoad = newRepositoryAfterProcessDeath.loadActiveDraft()
        assertTrue(activeDraftLoad is LoadEvaluationResult.Success)
        val recoveredRecord = (activeDraftLoad as LoadEvaluationResult.Success).record

        assertEquals("EVAL_PROCESS_RESTART_010", recoveredRecord.summary.evaluationUuid)
        assertEquals(
            "Paciente Recuperado Tras Reinicio",
            recoveredRecord.restoredUiState.patientIdentification.patientIdentifierOrAlias
        )
        assertEquals(
            EdiGlobalTrafficLight.VERDE,
            recoveredRecord.restoredUiState.ediGlobalEvaluation.trafficLight
        )
        assertTrue(recoveredRecord.restoredUiState.ediGlobalEvaluation.isDefinitive)
    }

    // =========================================================================
    // 11. Simular error de escritura/cifrado y rechazo de revisión obsoleta
    // =========================================================================
    @Test
    fun `test11 - error de cifrado o revision obsoleta devuelve Failure y nunca reporta Guardado falso`() = runBlocking {
        val failingCipher = object : CredEdiCipherProvider {
            override fun encrypt(plaintext: ByteArray, associatedData: ByteArray?): String {
                throw GeneralSecurityException("Fallo simulado de hardware KeyStore / almacenamiento lleno.")
            }

            override fun decrypt(ciphertextPayload: String, associatedData: ByteArray?): ByteArray {
                throw GeneralSecurityException("Fallo simulado de descifrado.")
            }
        }
        val failingRepo = CredEdiPersistenceRepository(
            evaluationDao = database.evaluationDao(),
            cipherProvider = failingCipher
        )

        val state = buildCompleteGreenGroup8State(evaluationUuid = "EVAL_FAIL_011")
        val failedSave = failingRepo.saveEvaluation(state)
        assertTrue(failedSave is SaveEvaluationResult.Failure)
        assertEquals(0, database.evaluationDao().countAll())

        // Verificar protección contra escritura fuera de orden (revisión 1 intenta sobrescribir revisión 2)
        val okSaveRev2 = repository.saveEvaluation(state, explicitRevision = 2L)
        assertTrue(okSaveRev2 is SaveEvaluationResult.Success)

        val staleState = state.copy(
            patientIdentification = PatientIdentificationData(patientIdentifierOrAlias = "Escritura Atrasada")
        )
        val staleSaveRev1 = repository.saveEvaluation(staleState, explicitRevision = 1L)
        assertTrue(staleSaveRev1 is SaveEvaluationResult.Failure)

        val loaded = (repository.loadEvaluationByUuid("EVAL_FAIL_011") as LoadEvaluationResult.Success).record
        assertEquals(2L, loaded.summary.monotonicRevision)
        assertEquals("Paciente G8 Verde", loaded.restoredUiState.patientIdentification.patientIdentifierOrAlias)
    }

    // =========================================================================
    // 12. Reexportar PDF desde evaluación recuperada
    // =========================================================================
    @Test
    fun `test12 - reexportar PDF desde evaluacion recuperada conserva semaforo grupo y estado oficial`() = runBlocking {
        val originalGreen = buildCompleteGreenGroup8State(
            evaluationUuid = "EVAL_PDF_012",
            patientAlias = "Paciente PDF Recuperado"
        )
        repository.saveEvaluation(originalGreen)

        val restoredState = (repository.loadEvaluationByUuid("EVAL_PDF_012") as LoadEvaluationResult.Success)
            .record.restoredUiState

        val pdfOriginal = EdiPdfExporter.exportarFichaOficialEdiPdf(context, originalGreen)
        val pdfRestored = EdiPdfExporter.exportarFichaOficialEdiPdf(context, restoredState)

        assertTrue(pdfOriginal.success)
        assertTrue(pdfRestored.success)
        assertFalse(pdfOriginal.isDraft)
        assertFalse(pdfRestored.isDraft)
        assertEquals(pdfOriginal.groupNumber, pdfRestored.groupNumber)
        assertEquals(pdfOriginal.trafficLightLabel, pdfRestored.trafficLightLabel)
    }
}
