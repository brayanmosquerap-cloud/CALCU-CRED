package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.example.model.AppTab
import com.example.model.AutosaveUiIndicatorState
import com.example.model.EdiAreaFallbackEngine
import com.example.model.EdiBirthMaturityCondition
import com.example.model.EdiClinicalModality
import com.example.model.EdiDevArea
import com.example.model.EdiGlobalTrafficLight
import com.example.model.EdiItemResponse
import com.example.model.EdiOfficialCatalog
import com.example.model.EvaluationPersistenceStatus
import com.example.model.GlobalDiagnosisCompleteness
import com.example.model.PerimetroCefalicoCaptureState
import com.example.persistence.AndroidKeyStoreAesGcmCipherProvider
import com.example.persistence.CredEdiCipherProvider
import com.example.persistence.CredEdiDatabase
import com.example.persistence.CredEdiPersistenceRepository
import com.example.persistence.JvmAesGcmCipherProvider
import com.example.persistence.LoadEvaluationResult
import com.example.persistence.SaveEvaluationResult
import com.example.util.EdiPdfExporter
import com.example.viewmodel.CredEdiViewModel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.security.GeneralSecurityException

/**
 * MEDIO-06 — LOTE 2:
 * Suite de pruebas automatizadas de regresión e integración para:
 * - Autoguardado automático fuera del hilo principal con revisiones monotónicas.
 * - Botón «Guardar evaluación» dentro de «Evaluaciones archivadas — almacenamiento temporal».
 * - Recuperación automática al iniciar el ViewModel / proceso.
 * - Casos obligatorios A a M (y N junto con las 247 pruebas previas).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class EdiMedio06Lote2AutosaveAndRecoveryTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var database: CredEdiDatabase
    private lateinit var cipherProvider: JvmAesGcmCipherProvider
    private lateinit var repository: CredEdiPersistenceRepository
    private var simulatedClockMillis: Long = 1_760_000_000_000L

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

    private fun createPersistentViewModel(
        repo: CredEdiPersistenceRepository = repository,
        autoRestoreOnInit: Boolean = true,
        autosaveDebounceMs: Long = 10L
    ): CredEdiViewModel {
        return CredEdiViewModel(
            persistenceRepository = repo,
            autoRestoreOnInit = autoRestoreOnInit,
            autosaveDebounceMs = autosaveDebounceMs
        )
    }

    /**
     * Configura en el ViewModel un niño de 14 meses nacido a término (Grupo 8) y responde todos
     * los reactivos en Verde normal.
     */
    private fun populateCompleteGreenGroup8InViewModel(viewModel: CredEdiViewModel) {
        viewModel.updatePatientIdentification(
            patientIdentifierOrAlias = "Paciente Grupo 8 Verde",
            clinicalRecordCode = "HC-G8-2026",
            generalClinicalNote = "Control CRED de 14 meses completo."
        )
        viewModel.updateFechaNacimiento("15/04/2025")
        viewModel.updateFechaEvaluacion("15/06/2026")
        viewModel.togglePrematurez(false)
        viewModel.updatePesoInput("10.2")
        viewModel.updateLongitudInput("78.0")
        viewModel.updatePerimetroCefalicoInput("46.5")

        val spec = EdiOfficialCatalog.getGroup(8)
        EdiOfficialCatalog.frbItems.forEach { item ->
            viewModel.recordEdiCurrentItemResponse(item.uniqueId, EdiItemResponse.NO)
        }
        spec.aleItems.forEach { item ->
            viewModel.recordEdiCurrentItemResponse(item.uniqueId, EdiItemResponse.NO)
        }
        spec.areaItems.values.flatten().forEach { item ->
            viewModel.recordEdiCurrentItemResponse(item.uniqueId, EdiItemResponse.YES)
        }
        EdiOfficialCatalog.getApplicableEnItems(8).forEach { item ->
            viewModel.recordEdiCurrentItemResponse(item.uniqueId, EdiItemResponse.NO)
        }
        spec.alaItems.forEach { item ->
            viewModel.recordEdiCurrentItemResponse(item.uniqueId, EdiItemResponse.NO)
        }
    }

    // =========================================================================
    // CASO A: Autoguardado de una respuesta EDI
    // =========================================================================
    @Test
    fun `caso A - autoguardado automatico de una respuesta EDI persiste en Room`() = runBlocking {
        val vm = createPersistentViewModel()
        vm.updateFechaNacimiento("15/04/2025")
        vm.updateFechaEvaluacion("15/06/2026")
        vm.togglePrematurez(false)

        val specG8 = EdiOfficialCatalog.getGroup(8)
        val firstMg = specG8.areaItems[EdiDevArea.MG]!!.first()

        vm.recordEdiCurrentItemResponse(firstMg.uniqueId, EdiItemResponse.YES)
        val saveResult = vm.awaitPendingAutosave()

        assertTrue(saveResult is SaveEvaluationResult.Success)
        assertEquals(AutosaveUiIndicatorState.SAVED, vm.uiState.value.persistenceIndicatorState)

        val loaded = repository.loadActiveDraft()
        assertTrue(loaded is LoadEvaluationResult.Success)
        val restored = (loaded as LoadEvaluationResult.Success).record.restoredUiState
        assertEquals(
            EdiItemResponse.YES,
            restored.ediEngineState.activeSession.currentGroupResponses[firstMg.uniqueId]
        )
    }

    // =========================================================================
    // CASO B: Autoguardado de diez respuestas consecutivas
    // =========================================================================
    @Test
    fun `caso B - autoguardado de diez respuestas consecutivas conserva las 10 respuestas`() = runBlocking {
        val vm = createPersistentViewModel(autosaveDebounceMs = 15L)
        vm.updateFechaNacimiento("15/04/2025")
        vm.updateFechaEvaluacion("15/06/2026")
        vm.togglePrematurez(false)

        val specG8 = EdiOfficialCatalog.getGroup(8)
        val allAreaItems = specG8.areaItems.values.flatten()
        val tenItems = (allAreaItems + specG8.aleItems + specG8.alaItems).take(10)
        assertEquals(10, tenItems.size)

        tenItems.forEachIndexed { idx, item ->
            val resp = if (idx % 2 == 0) EdiItemResponse.YES else EdiItemResponse.NO
            vm.recordEdiCurrentItemResponse(item.uniqueId, resp)
        }

        val saveRes = vm.awaitPendingAutosave()
        assertTrue(saveRes is SaveEvaluationResult.Success)
        assertEquals(AutosaveUiIndicatorState.SAVED, vm.uiState.value.persistenceIndicatorState)

        val loaded = (repository.loadActiveDraft() as LoadEvaluationResult.Success).record.restoredUiState
        tenItems.forEachIndexed { idx, item ->
            val expected = if (idx % 2 == 0) EdiItemResponse.YES else EdiItemResponse.NO
            assertEquals(
                "El reactivo #${idx + 1} (${item.uniqueId}) debe conservar su respuesta autoguardada",
                expected,
                loaded.ediEngineState.activeSession.currentGroupResponses[item.uniqueId]
            )
        }
    }

    // =========================================================================
    // CASO C: Guardado manual de evaluación incompleta (botón «Guardar evaluación»)
    // =========================================================================
    @Test
    fun `caso C - boton Guardar evaluacion guarda evaluacion incompleta con confirmacion real sin marcarla completada`() = runBlocking {
        val vm = createPersistentViewModel(autosaveDebounceMs = 5_000L)
        vm.updateFechaNacimiento("15/04/2025")
        vm.updateFechaEvaluacion("15/06/2026")
        vm.togglePrematurez(false)

        val firstMg = EdiOfficialCatalog.getGroup(8).areaItems[EdiDevArea.MG]!!.first()
        vm.recordEdiCurrentItemResponse(firstMg.uniqueId, EdiItemResponse.YES)
        vm.selectTab(AppTab.EDI)

        // Antes de que expire el debounce, el indicador debe mostrar "Cambios pendientes"
        assertEquals(AutosaveUiIndicatorState.PENDING_CHANGES, vm.uiState.value.persistenceIndicatorState)

        composeRule.setContent {
            CredEdiApp(viewModel = vm)
        }
        composeRule.waitForIdle()

        // Verificar presencia del indicador discreto "Cambios pendientes" y del botón "Guardar evaluación"
        // dentro del apartado «Evaluaciones archivadas — almacenamiento temporal»
        composeRule.onNodeWithTag("edi_archived_sessions_section").assertExists()
        composeRule.onNodeWithTag("edi_summary_persistence_indicator_badge").assertExists()
        composeRule.onNodeWithTag("edi_summary_save_evaluation_btn").performScrollTo().assertIsDisplayed()

        // Ejecutar el guardado manual y esperar confirmación de la transacción Room
        val manualResult = vm.saveCurrentEvaluationManuallySuspend()
        assertTrue(manualResult is SaveEvaluationResult.Success)
        val success = manualResult as SaveEvaluationResult.Success
        assertEquals(EvaluationPersistenceStatus.INCOMPLETE, success.persistedStatus)

        composeRule.waitForIdle()
        assertEquals(AutosaveUiIndicatorState.SAVED, vm.uiState.value.persistenceIndicatorState)
        assertEquals("Evaluación guardada correctamente", vm.uiState.value.persistenceStatusMessage)
        assertFalse(vm.uiState.value.ediGlobalEvaluation.isDefinitive)
        assertEquals(EdiGlobalTrafficLight.INCOMPLETE, vm.uiState.value.ediGlobalEvaluation.trafficLight)

        composeRule.onNodeWithTag("edi_summary_persistence_status_banner").performScrollTo().assertIsDisplayed()
        assertTrue(
            composeRule.onAllNodesWithText("Evaluación guardada correctamente", substring = true, useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        )
    }

    // =========================================================================
    // CASO D: Recuperación automática de una evaluación verde del Grupo 8
    // =========================================================================
    @Test
    fun `caso D - recuperacion automatica de una evaluacion verde del Grupo 8 al iniciar ViewModel`() = runBlocking {
        val vm1 = createPersistentViewModel()
        populateCompleteGreenGroup8InViewModel(vm1)
        val saveRes = vm1.awaitPendingAutosave()
        assertTrue(saveRes is SaveEvaluationResult.Success)
        assertEquals(
            EvaluationPersistenceStatus.COMPLETED,
            (saveRes as SaveEvaluationResult.Success).persistedStatus
        )

        // Recrear ViewModel simulando reinicio de la aplicación
        val vmRecovered = createPersistentViewModel()
        val recoveredState = vmRecovered.uiState.value

        assertTrue(recoveredState.wasRestoredFromPersistence)
        assertEquals("Evaluación recuperada", recoveredState.persistenceStatusMessage)
        assertEquals(AutosaveUiIndicatorState.SAVED, recoveredState.persistenceIndicatorState)
        assertEquals(8, recoveredState.ediEngineState.activeGroupNumber)
        assertTrue(recoveredState.ediGlobalEvaluation.isDefinitive)
        assertEquals(EdiGlobalTrafficLight.VERDE, recoveredState.ediGlobalEvaluation.trafficLight)
        assertEquals("Paciente Grupo 8 Verde", recoveredState.patientIdentification.patientIdentifierOrAlias)
        assertEquals(10.2, recoveredState.effectivePesoKg, 0.001)
        assertEquals(78.0, recoveredState.effectiveLongitudCm, 0.001)
        assertEquals(46.5, recoveredState.effectivePerimetroCefalicoCm, 0.001)
    }

    // =========================================================================
    // CASO E: Recuperación de una evaluación roja con sus causas
    // =========================================================================
    @Test
    fun `caso E - recuperacion automatica de una evaluacion roja con sus causas y alertas criticas`() = runBlocking {
        val vm1 = createPersistentViewModel()
        populateCompleteGreenGroup8InViewModel(vm1)

        val specG8 = EdiOfficialCatalog.getGroup(8)
        val firstAla = specG8.alaItems.first()
        val firstEn = EdiOfficialCatalog.getApplicableEnItems(8).first()

        vm1.recordEdiCurrentItemResponse(firstAla.uniqueId, EdiItemResponse.YES)
        vm1.recordEdiCurrentItemResponse(firstEn.uniqueId, EdiItemResponse.YES)
        vm1.awaitPendingAutosave()

        val evalBefore = vm1.uiState.value.ediGlobalEvaluation
        assertTrue(evalBefore.isDefinitive)
        assertEquals(EdiGlobalTrafficLight.ROJO, evalBefore.trafficLight)
        assertTrue(evalBefore.hasPriorityCriticalWarning)

        val vmRecovered = createPersistentViewModel()
        val evalAfter = vmRecovered.uiState.value.ediGlobalEvaluation

        assertTrue(vmRecovered.uiState.value.wasRestoredFromPersistence)
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
    // CASO F: Recuperación de «No evaluado por falta de material»
    // =========================================================================
    @Test
    fun `caso F - recuperacion de reactivo No evaluado por falta de material`() = runBlocking {
        val vm1 = createPersistentViewModel()
        vm1.updateFechaNacimiento("15/04/2025")
        vm1.updateFechaEvaluacion("15/06/2026")
        vm1.togglePrematurez(false)

        val specG8 = EdiOfficialCatalog.getGroup(8)
        val itemWithMaterial = specG8.areaItems.values.flatten().first { it.requiresMaterialInProtocol }
        vm1.recordEdiCurrentItemResponse(
            itemWithMaterial.uniqueId,
            EdiItemResponse.NOT_EVALUATED_MISSING_MATERIAL
        )
        vm1.awaitPendingAutosave()

        val vmRecovered = createPersistentViewModel()
        val restoredSession = vmRecovered.uiState.value.ediEngineState.activeSession

        assertEquals(
            EdiItemResponse.NOT_EVALUATED_MISSING_MATERIAL,
            restoredSession.currentGroupResponses[itemWithMaterial.uniqueId]
        )
        assertFalse(vmRecovered.uiState.value.ediGlobalEvaluation.isDefinitive)
        assertEquals(
            EdiGlobalTrafficLight.INCOMPLETE,
            vmRecovered.uiState.value.ediGlobalEvaluation.trafficLight
        )
    }

    // =========================================================================
    // CASO G: Recuperación de notas clínicas, retrocesos y evidencia dual
    // =========================================================================
    @Test
    fun `caso G - recuperacion de notas clinicas retrocesos y evidencia dual`() = runBlocking {
        val vm1 = createPersistentViewModel()
        // Niño de 33 meses (Grupo 12: 31m a 36m 29d) para probar retroceso a Grupo 11 y evidencia dual ALE 12.3
        vm1.updateFechaNacimiento("15/09/2023")
        vm1.updateFechaEvaluacion("15/06/2026")
        vm1.togglePrematurez(false)
        assertEquals(12, vm1.uiState.value.ediEngineState.activeGroupNumber)

        val specG12 = EdiOfficialCatalog.getGroup(12)
        val mgItemsG12 = specG12.areaItems[EdiDevArea.MG]!!
        // Marcar NO en los reactivos MG de G12 para activar retroceso obligatorio a G11
        mgItemsG12.forEach { item ->
            vm1.recordEdiCurrentItemResponse(item.uniqueId, EdiItemResponse.NO)
        }
        vm1.updateEdiItemClinicalNote(
            mgItemsG12.first().uniqueId,
            "Dificultad observada en salto bipodal; se activa retroceso a G11."
        )

        val fallbackMgSpec = EdiAreaFallbackEngine.getFallbackItemsForArea(12, EdiDevArea.MG).first()
        vm1.recordEdiFallbackResponse(fallbackMgSpec.fallbackUniqueId, EdiItemResponse.YES)

        val ale123 = specG12.aleItems.first { it.modality == EdiClinicalModality.DUAL_OBSERVATION_AND_INTERVIEW_ALE_12_3 }
        vm1.recordEdiDualEvidence(
            itemUniqueId = ale123.uniqueId,
            observationEvidence = true,
            interviewEvidence = false,
            humanResolutionIfConflict = true,
            note = "Resolución profesional documentada tras observación en sala CRED."
        )

        vm1.awaitPendingAutosave()

        val vmRecovered = createPersistentViewModel()
        val restoredSession = vmRecovered.uiState.value.ediEngineState.activeSession

        assertEquals(
            "Dificultad observada en salto bipodal; se activa retroceso a G11.",
            restoredSession.itemClinicalNotes[mgItemsG12.first().uniqueId]
        )
        assertEquals(
            EdiItemResponse.YES,
            restoredSession.fallbackGroupResponses[fallbackMgSpec.fallbackUniqueId]
        )
        val dual = restoredSession.dualEvidenceRecords[ale123.uniqueId]
        assertNotNull(dual)
        assertEquals(true, dual?.directObservationEvidence)
        assertEquals(false, dual?.directedInterviewEvidence)
        assertEquals(true, dual?.professionalHumanResolution)
        assertEquals(
            "Resolución profesional documentada tras observación en sala CRED.",
            dual?.clinicalJustificationNote
        )
    }

    // =========================================================================
    // CASO H: Recuperación de datos antropométricos ausentes (preserva PC/E-01 y ANTRO-02)
    // =========================================================================
    @Test
    fun `caso H - recuperacion de datos antropometricos ausentes sin generar mediciones ficticias`() = runBlocking {
        val vm1 = createPersistentViewModel()
        vm1.updateFechaNacimiento("15/04/2025")
        vm1.updateFechaEvaluacion("15/06/2026")
        vm1.togglePrematurez(false)
        // No se ingresa peso, ni talla, ni perímetro cefálico
        vm1.awaitPendingAutosave()

        val vmRecovered = createPersistentViewModel()
        val state = vmRecovered.uiState.value

        assertTrue(state.wasRestoredFromPersistence)
        assertEquals("", state.pesoInput)
        assertEquals("", state.longitudInput)
        assertEquals("", state.perimetroCefalicoInput)
        assertTrue(state.pesoKg.isNaN())
        assertTrue(state.longitudCm.isNaN())
        assertTrue(state.perimetroCefalicoCm.isNaN())
        assertTrue(state.effectivePesoKg.isNaN())
        assertTrue(state.effectiveLongitudCm.isNaN())
        assertTrue(state.effectivePerimetroCefalicoCm.isNaN())
        assertFalse(state.hasPesoIngresado)
        assertFalse(state.hasLongitudIngresada)
        assertFalse(state.hasPerimetroCefalicoIngresado)
        assertEquals(0, state.datosAntropometricosCount)
        assertEquals(PerimetroCefalicoCaptureState.EMPTY_UNREGISTERED, state.perimetroCefalicoCaptureState)
        assertEquals(GlobalDiagnosisCompleteness.UNAVAILABLE, state.globalDiagnosis.completeness)
        assertEquals("Sin medición", state.evalPE.shortClassification)
        assertEquals("Sin medición", state.evalTE.shortClassification)
        assertEquals("Sin medición", state.evalPCE.shortClassification)
        assertFalse(state.globalDiagnosis.title.contains("Desnutrición", ignoreCase = true))
        assertFalse(state.evalPCE.fullClassification.contains("Microcefalia", ignoreCase = true))
    }

    // =========================================================================
    // CASO I: Borrado de una medición previamente registrada y recuperación sin valores antiguos
    // =========================================================================
    @Test
    fun `caso I - borrado de mediciones previamente registradas y recuperacion sin valores antiguos`() = runBlocking {
        val vm1 = createPersistentViewModel()
        vm1.updateFechaNacimiento("15/06/2022")
        vm1.updateFechaEvaluacion("15/06/2026")
        vm1.updatePesoInput("17.5")
        vm1.updateLongitudInput("110.5")
        vm1.updatePerimetroCefalicoInput("50.0")
        vm1.awaitPendingAutosave()

        assertTrue(vm1.uiState.value.hasPesoIngresado)
        assertTrue(vm1.uiState.value.hasLongitudIngresada)
        assertTrue(vm1.uiState.value.hasPerimetroCefalicoIngresado)

        // Ahora el usuario borra el Perímetro Cefálico y el Peso
        vm1.updatePerimetroCefalicoInput("")
        vm1.updatePesoInput("")
        vm1.awaitPendingAutosave()

        // Al recuperar en un nuevo ViewModel, los campos borrados deben estar vacíos y en NaN, jamás con 50.0, 39.5 o 17.5
        val vmRecovered = createPersistentViewModel()
        val restored = vmRecovered.uiState.value

        assertEquals("", restored.perimetroCefalicoInput)
        assertTrue(restored.perimetroCefalicoCm.isNaN())
        assertTrue(restored.effectivePerimetroCefalicoCm.isNaN())
        assertFalse(restored.hasPerimetroCefalicoIngresado)
        assertEquals("Sin medición", restored.evalPCE.shortClassification)
        assertFalse(restored.evalPCE.fullClassification.contains("Microcefalia", ignoreCase = true))

        assertEquals("", restored.pesoInput)
        assertTrue(restored.pesoKg.isNaN())
        assertTrue(restored.effectivePesoKg.isNaN())
        assertFalse(restored.hasPesoIngresado)
        assertEquals("Sin medición", restored.evalPE.shortClassification)

        // La talla (110.5 cm) que no fue borrada sí debe recuperarse intacta
        assertEquals("110.5", restored.longitudInput)
        assertEquals(110.5, restored.effectiveLongitudCm, 0.001)
        assertTrue(restored.hasLongitudIngresada)
    }

    // =========================================================================
    // CASO J: Simulación de reinicio del ViewModel y del proceso (y detección de corrupción)
    // =========================================================================
    @Test
    fun `caso J - simulacion de reinicio del ViewModel y del proceso y deteccion de corrupcion sin falso recuperado`() = runBlocking {
        val vmBeforeKill = createPersistentViewModel()
        populateCompleteGreenGroup8InViewModel(vmBeforeKill)
        vmBeforeKill.awaitPendingAutosave()
        val activeUuid = vmBeforeKill.uiState.value.evaluationUuid

        // 1) Simular reinicio completo del proceso creando nuevo repositorio y nuevo ViewModel sobre la misma DB y clave
        val repositoryAfterProcessRestart = CredEdiPersistenceRepository(
            evaluationDao = database.evaluationDao(),
            cipherProvider = cipherProvider
        )
        val vmAfterProcessRestart = CredEdiViewModel(
            persistenceRepository = repositoryAfterProcessRestart,
            autoRestoreOnInit = true,
            autosaveDebounceMs = 10L
        )

        assertTrue(vmAfterProcessRestart.uiState.value.wasRestoredFromPersistence)
        assertEquals("Evaluación recuperada", vmAfterProcessRestart.uiState.value.persistenceStatusMessage)
        assertEquals(activeUuid, vmAfterProcessRestart.uiState.value.evaluationUuid)
        assertEquals(
            EdiGlobalTrafficLight.VERDE,
            vmAfterProcessRestart.uiState.value.ediGlobalEvaluation.trafficLight
        )

        // 2) Simular corrupción del payload en SQLite y verificar que al iniciar NO presenta evaluación vacía como recuperada
        val rawEntity = database.evaluationDao().getByUuid(activeUuid)!!
        database.evaluationDao().insertOrReplaceInternal(
            rawEntity.copy(plaintextSha256 = "0000000000000000000000000000000000000000000000000000000000000000")
        )

        val vmOnCorruptedStorage = CredEdiViewModel(
            persistenceRepository = repositoryAfterProcessRestart,
            autoRestoreOnInit = true,
            autosaveDebounceMs = 10L
        )
        val corruptedState = vmOnCorruptedStorage.uiState.value
        assertFalse(corruptedState.wasRestoredFromPersistence)
        assertEquals(AutosaveUiIndicatorState.ERROR, corruptedState.persistenceIndicatorState)
        assertNotNull(corruptedState.persistenceRecoveryErrorMessage)
        assertTrue(
            corruptedState.persistenceStatusMessage.orEmpty().contains("No se pudo recuperar", ignoreCase = true)
        )
    }

    // =========================================================================
    // CASO K: Fallo de almacenamiento sin confirmación falsa y reintento exitoso
    // =========================================================================
    @Test
    fun `caso K - fallo de almacenamiento o cifrado nunca muestra Guardado falso y permite reintentar`() = runBlocking {
        var shouldFailStorage = true
        val switchableCipher = object : CredEdiCipherProvider {
            override fun encrypt(plaintext: ByteArray, associatedData: ByteArray?): String {
                if (shouldFailStorage) {
                    throw GeneralSecurityException("Fallo simulado de almacenamiento / KeyStore.")
                }
                return cipherProvider.encrypt(plaintext, associatedData)
            }

            override fun decrypt(ciphertextPayload: String, associatedData: ByteArray?): ByteArray {
                return cipherProvider.decrypt(ciphertextPayload, associatedData)
            }
        }

        val switchableRepo = CredEdiPersistenceRepository(
            evaluationDao = database.evaluationDao(),
            cipherProvider = switchableCipher
        )
        val vm = createPersistentViewModel(repo = switchableRepo, autoRestoreOnInit = false)
        vm.updateFechaNacimiento("15/04/2025")
        vm.updateFechaEvaluacion("15/06/2026")
        vm.togglePrematurez(false)

        val failedAutosave = vm.awaitPendingAutosave()
        assertTrue(failedAutosave is SaveEvaluationResult.Failure)
        assertEquals(AutosaveUiIndicatorState.ERROR, vm.uiState.value.persistenceIndicatorState)
        assertTrue(vm.uiState.value.persistenceStatusMessage.orEmpty().contains("No se pudo guardar"))

        // Intentar guardado manual mientras sigue fallando -> tampoco debe confirmar guardado falso
        val failedManual = vm.saveCurrentEvaluationManuallySuspend()
        assertTrue(failedManual is SaveEvaluationResult.Failure)
        assertEquals(AutosaveUiIndicatorState.ERROR, vm.uiState.value.persistenceIndicatorState)
        assertFalse(
            vm.uiState.value.persistenceStatusMessage.orEmpty()
                .contains("Evaluación guardada correctamente")
        )

        // Verificar también la regla de seguridad Sección 6: AndroidKeyStoreAesGcmCipherProvider en entorno sin hardware KeyStore
        // lanza GeneralSecurityException en lugar de sustituir la clave por una alternativa insegura
        val strictKeystoreProvider = AndroidKeyStoreAesGcmCipherProvider()
        var threwSecurityException = false
        try {
            strictKeystoreProvider.encrypt("test".toByteArray(), "EVAL_1".toByteArray())
        } catch (_: GeneralSecurityException) {
            threwSecurityException = true
        }
        assertTrue(
            "AndroidKeyStoreAesGcmCipherProvider debe fallar de forma segura si AndroidKeyStore no está disponible",
            threwSecurityException
        )

        // Ahora el almacenamiento se restablece y el usuario reintenta guardar manualmente
        shouldFailStorage = false
        val retryResult = vm.saveCurrentEvaluationManuallySuspend()
        assertTrue(retryResult is SaveEvaluationResult.Success)
        assertEquals(AutosaveUiIndicatorState.SAVED, vm.uiState.value.persistenceIndicatorState)
        assertEquals("Evaluación guardada correctamente", vm.uiState.value.persistenceStatusMessage)
    }

    // =========================================================================
    // CASO L: Escrituras sucesivas sin pérdida de la última modificación
    // =========================================================================
    @Test
    fun `caso L - escrituras sucesivas rapidas conservan siempre la ultima modificacion con revision monotonica`() = runBlocking {
        val vm = createPersistentViewModel(autosaveDebounceMs = 5L)
        vm.updateFechaNacimiento("15/04/2025")
        vm.updateFechaEvaluacion("15/06/2026")
        vm.togglePrematurez(false)

        val firstMg = EdiOfficialCatalog.getGroup(8).areaItems[EdiDevArea.MG]!!.first()

        // Ráfaga de cambios alternando respuesta, peso y nota clínica
        vm.recordEdiCurrentItemResponse(firstMg.uniqueId, EdiItemResponse.YES)
        vm.updatePesoInput("9.5")
        vm.recordEdiCurrentItemResponse(firstMg.uniqueId, EdiItemResponse.NO)
        vm.updatePesoInput("10.0")
        vm.recordEdiCurrentItemResponse(firstMg.uniqueId, EdiItemResponse.NOT_EVALUATED_MISSING_MATERIAL)
        vm.updatePesoInput("10.45")
        vm.recordEdiCurrentItemResponse(firstMg.uniqueId, EdiItemResponse.YES)
        vm.updateEdiItemClinicalNote(firstMg.uniqueId, "Nota versión 1")
        vm.updateEdiItemClinicalNote(firstMg.uniqueId, "Nota versión 2")
        vm.updateEdiItemClinicalNote(firstMg.uniqueId, "Nota final definitiva versión 3")

        val finalSave = vm.awaitPendingAutosave()
        assertTrue(finalSave is SaveEvaluationResult.Success)

        val vmRecovered = createPersistentViewModel()
        val restored = vmRecovered.uiState.value

        assertEquals(10.45, restored.effectivePesoKg, 0.001)
        assertEquals("10.45", restored.pesoInput)
        assertEquals(
            EdiItemResponse.YES,
            restored.ediEngineState.activeSession.currentGroupResponses[firstMg.uniqueId]
        )
        assertEquals(
            "Nota final definitiva versión 3",
            restored.ediEngineState.activeSession.itemClinicalNotes[firstMg.uniqueId]
        )
        assertTrue(restored.lastPersistedRevision >= 10L)
    }

    // =========================================================================
    // CASO M: Exportación PDF coherente con la evaluación restaurada
    // =========================================================================
    @Test
    fun `caso M - exportacion PDF desde ViewModel restaurado es coherente con la evaluacion original`() = runBlocking {
        val vmOriginal = createPersistentViewModel()
        populateCompleteGreenGroup8InViewModel(vmOriginal)
        vmOriginal.awaitPendingAutosave()

        val pdfBeforeRestart = EdiPdfExporter.exportarFichaOficialEdiPdf(context, vmOriginal.uiState.value)
        assertTrue(pdfBeforeRestart.success)
        assertFalse(pdfBeforeRestart.isDraft)

        val vmRestored = createPersistentViewModel()
        val pdfAfterRestart = EdiPdfExporter.exportarFichaOficialEdiPdf(context, vmRestored.uiState.value)

        assertTrue(pdfAfterRestart.success)
        assertFalse(pdfAfterRestart.isDraft)
        assertEquals(pdfBeforeRestart.groupNumber, pdfAfterRestart.groupNumber)
        assertEquals(pdfBeforeRestart.trafficLightLabel, pdfAfterRestart.trafficLightLabel)
        assertEquals(8, pdfAfterRestart.groupNumber)
        assertEquals(EdiGlobalTrafficLight.VERDE.displayLabel, pdfAfterRestart.trafficLightLabel)
    }
}
