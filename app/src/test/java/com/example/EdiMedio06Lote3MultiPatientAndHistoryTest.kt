package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.example.model.AppTab
import com.example.model.AutosaveUiIndicatorState
import com.example.model.EdiAreaFallbackEngine
import com.example.model.EdiDevArea
import com.example.model.EdiGlobalTrafficLight
import com.example.model.EdiInternalView
import com.example.model.EdiItemResponse
import com.example.model.EdiOfficialCatalog
import com.example.model.GlobalDiagnosisCompleteness
import com.example.model.PerimetroCefalicoCaptureState
import com.example.model.SexoBiologico
import com.example.persistence.CredEdiCipherProvider
import com.example.persistence.CredEdiDatabase
import com.example.persistence.CredEdiPersistenceRepository
import com.example.persistence.JvmAesGcmCipherProvider
import com.example.persistence.SaveEvaluationResult
import com.example.viewmodel.CredEdiViewModel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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
 * MEDIO-06 — LOTE 3:
 * Suite automatizada obligatoria para validar el apartado «Guardado e Historial de Evaluaciones»,
 * la lista plegable respaldada por Room + AES-256-GCM + SHA-256, las acciones «Continuar», «Ver» y
 * «Eliminar» con confirmación deliberada, y el aislamiento multipaciente sin pérdida ni mezcla.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class EdiMedio06Lote3MultiPatientAndHistoryTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var database: CredEdiDatabase
    private lateinit var cipherProvider: JvmAesGcmCipherProvider
    private lateinit var repository: CredEdiPersistenceRepository
    private var simulatedClockMillis: Long = 1_781_520_000_000L

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

    private fun populateCompleteGreenGroup8InViewModel(viewModel: CredEdiViewModel) {
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

    @Test
    fun `caso A y B reemplazo visual del apartado por Guardado e Historial de Evaluaciones en Summary y GroupBank`() = runBlocking<Unit> {
        val vm = createPersistentViewModel()
        vm.selectTab(AppTab.EDI)

        composeRule.setContent {
            CredEdiApp(viewModel = vm)
        }

        // Caso A: EdiSummary5AxesView (MEDIO-07: panel de guardado sin encabezado ni identificación duplicada)
        composeRule.onNodeWithTag("edi_archived_sessions_section")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("Guardado e Historial de Evaluaciones")
            .assertDoesNotExist()
        composeRule.onNodeWithTag("edi_summary_active_patient_label")
            .assertDoesNotExist()
        composeRule.onNodeWithTag("edi_summary_active_patient_dni_label")
            .assertDoesNotExist()
        composeRule.onNodeWithTag("edi_summary_persistence_indicator_badge")
            .assertIsDisplayed()
        composeRule.onNodeWithText(
            "Las evaluaciones se guardan localmente en este dispositivo. Puedes recuperarlas desde el historial. Si se eliminan los datos de la aplicación o se restablece el emulador, los registros locales podrían perderse"
        ).assertIsDisplayed()
        composeRule.onNodeWithTag("edi_summary_save_evaluation_btn").assertIsDisplayed()
        composeRule.onNodeWithTag("edi_summary_toggle_history_btn").assertIsDisplayed()
        // Lote 3D: «Nueva evaluación» sustituye a «Nuevo episodio» en el apartado secundario y se retira «Corregir fecha»
        composeRule.onNodeWithTag("edi_summary_new_evaluation_btn")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag("edi_summary_new_episode_btn").assertDoesNotExist()
        composeRule.onNodeWithTag("edi_summary_toggle_episode_options_btn").assertDoesNotExist()
        composeRule.onNodeWithTag("edi_summary_correct_birth_date_btn").assertDoesNotExist()

        // Caso B: EdiGroupBank15View (MEDIO-07)
        vm.navigateEdiView(EdiInternalView.GROUP_BANK_15)
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("edi_archived_sessions_section")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("Guardado e Historial de Evaluaciones")
            .assertDoesNotExist()
        composeRule.onNodeWithTag("edi_bank_active_patient_label")
            .assertDoesNotExist()
        composeRule.onNodeWithTag("edi_bank_active_patient_dni_label")
            .assertDoesNotExist()
        composeRule.onNodeWithTag("edi_bank_save_evaluation_btn")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag("edi_bank_toggle_history_btn")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag("edi_bank_new_evaluation_btn")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag("edi_bank_new_episode_btn").assertDoesNotExist()
        composeRule.onNodeWithTag("edi_bank_toggle_episode_options_btn").assertDoesNotExist()
        composeRule.onNodeWithTag("edi_bank_correct_birth_date_btn").assertDoesNotExist()
    }

    @Test
    fun `caso C D y E historial inicialmente plegado Ver historial despliega evaluaciones Room y Ocultar historial pliega`() = runBlocking<Unit> {
        val vm = createPersistentViewModel()

        vm.updatePatientIdentification("Lucía Mendoza", "HC-301", "")
        vm.updateFechaNacimiento("15/04/2025")
        vm.updateFechaEvaluacion("15/06/2026")
        vm.confirmEdiTermWithoutWeeks()
        assertTrue(vm.saveCurrentEvaluationManuallySuspend() is SaveEvaluationResult.Success)

        // Caso C: Historial inicialmente plegado
        assertFalse(vm.uiState.value.isEvaluationHistoryExpanded)

        vm.selectTab(AppTab.EDI)
        composeRule.setContent {
            CredEdiApp(viewModel = vm)
        }

        composeRule.onNodeWithTag("edi_summary_toggle_history_btn")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("Ver historial").assertIsDisplayed()

        // Caso D: Al pulsar «Ver historial», se muestran las evaluaciones guardadas en Room
        val evalUuid = vm.uiState.value.evaluationUuid
        composeRule.onNodeWithTag("edi_summary_toggle_history_btn")
            .performScrollTo()
            .performClick()
        composeRule.waitForIdle()

        assertTrue(vm.uiState.value.isEvaluationHistoryExpanded)
        composeRule.onNodeWithTag("edi_summary_saved_history_list")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag("edi_summary_history_card_$evalUuid")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag("edi_summary_history_patient_label_$evalUuid")
            .performScrollTo()
            .assertIsDisplayed()

        // Caso E: Al pulsar «Ocultar historial», la lista se pliega
        composeRule.onNodeWithTag("edi_summary_toggle_history_btn")
            .performScrollTo()
            .performClick()
        composeRule.waitForIdle()

        assertFalse(vm.uiState.value.isEvaluationHistoryExpanded)
        composeRule.onNodeWithText("Ver historial").assertIsDisplayed()
    }

    @Test
    fun `caso F G H I y K multiples pacientes A B C separados por evaluationUuid Nueva evaluacion y Continuar guardando antes`() = runBlocking {
        val vm = createPersistentViewModel()

        // Paciente A: Grupo 8 completa (Verde)
        val uuidA = vm.uiState.value.evaluationUuid
        vm.updatePatientIdentification("Paciente A · Mateo", "EXP-A1", "Control 14m completo")
        vm.selectSexo(SexoBiologico.NINO)
        vm.updateFechaNacimiento("15/04/2025")
        vm.updateFechaEvaluacion("15/06/2026")
        vm.confirmEdiTermWithoutWeeks()
        vm.updatePesoInput("10.4")
        vm.updateLongitudInput("77.5")
        vm.updatePerimetroCefalicoInput("46.8")
        populateCompleteGreenGroup8InViewModel(vm)
        vm.updateEdiItemClinicalNote("G8_MG_8_1", "Nota exclusiva Paciente A")
        assertTrue(vm.uiState.value.ediGlobalEvaluation.isDefinitive)
        assertEquals(EdiGlobalTrafficLight.VERDE, vm.uiState.value.ediGlobalEvaluation.trafficLight)

        // Caso K: Pulsar «Nueva evaluación» guarda la activa (Paciente A) y crea sesión limpia con nuevo evaluationUuid (Paciente B)
        assertTrue(vm.startNewPatientEvaluationSuspend())

        val uuidB = vm.uiState.value.evaluationUuid
        assertNotEquals(uuidA, uuidB)
        assertEquals("", vm.uiState.value.patientIdentification.patientIdentifierOrAlias)
        assertEquals("", vm.uiState.value.pesoInput)
        assertEquals(0, vm.uiState.value.ediMandatoryItemsProgress.first)

        // Paciente B: Grupo 11 (27 meses) incompleta con retroceso, nota y respuesta parcial
        vm.updatePatientIdentification("Paciente B · Valeria", "EXP-B2", "Evaluación parcial G11")
        vm.selectSexo(SexoBiologico.NINA)
        vm.updateFechaNacimiento("15/03/2024")
        vm.updateFechaEvaluacion("15/06/2026")
        vm.confirmEdiTermWithoutWeeks()
        vm.updatePesoInput("12.6")
        vm.updateLongitudInput("88.2")
        assertEquals(11, vm.uiState.value.ediEffectiveGroupNumber)
        val g11MgItems = EdiOfficialCatalog.getGroup(11).areaItems[EdiDevArea.MG]!!
        g11MgItems.forEach { item ->
            vm.recordEdiCurrentItemResponse(item.uniqueId, EdiItemResponse.NO)
        }
        val fallbackG11Mg = EdiAreaFallbackEngine.getFallbackItemsForArea(11, EdiDevArea.MG).first()
        vm.recordEdiFallbackResponse(fallbackG11Mg.fallbackUniqueId, EdiItemResponse.YES)
        vm.recordEdiCurrentItemResponse("G11_LE_11_1", EdiItemResponse.YES)
        vm.updateEdiItemClinicalNote("G11_MG_11_1", "Nota exclusiva Paciente B en G11")

        // Crear Paciente C mediante «Nueva evaluación» (guarda Paciente B automáticamente)
        assertTrue(vm.startNewPatientEvaluationSuspend())

        val uuidC = vm.uiState.value.evaluationUuid
        assertNotEquals(uuidA, uuidC)
        assertNotEquals(uuidB, uuidC)

        // Paciente C: Grupo 6 (8 meses) incompleta con cambios pendientes sin guardar manualmente
        vm.updatePatientIdentification("Paciente C · Santiago", "EXP-C3", "Evaluación G6 en curso")
        vm.selectSexo(SexoBiologico.NINO)
        vm.updateFechaNacimiento("15/10/2025")
        vm.updateFechaEvaluacion("15/06/2026")
        vm.confirmEdiTermWithoutWeeks()
        assertEquals(6, vm.uiState.value.ediEffectiveGroupNumber)
        vm.recordEdiCurrentItemResponse("G6_MG_6_1", EdiItemResponse.YES)
        vm.updateEdiItemClinicalNote("G6_MG_6_1", "Nota exclusiva Paciente C")

        // Caso I y H: Al pulsar «Continuar» sobre Paciente B, primero guarda Paciente C y luego carga Paciente B íntegro
        assertTrue(vm.continueEvaluationFromHistorySuspend(uuidB))

        // Caso F: Los 3 pacientes existen en Room con UUIDs independientes
        val history = vm.uiState.value.savedEvaluationHistory
        assertEquals(3, history.size)
        assertEquals(setOf(uuidA, uuidB, uuidC), history.map { it.evaluationUuid }.toSet())

        // Caso G y H: Verificar estado cargado de Paciente B sin mezcla con A ni C
        val stateB = vm.uiState.value
        assertEquals(uuidB, stateB.evaluationUuid)
        assertEquals("Paciente B · Valeria", stateB.patientIdentification.patientIdentifierOrAlias)
        assertEquals(SexoBiologico.NINA, stateB.sexo)
        assertEquals(11, stateB.ediEffectiveGroupNumber)
        assertEquals("12.6", stateB.pesoInput)
        assertEquals("88.2", stateB.longitudInput)
        assertEquals(
            EdiItemResponse.NO,
            stateB.ediEngineState.activeSession.currentGroupResponses["G11_MG_11_1"]
        )
        assertEquals(
            EdiItemResponse.YES,
            stateB.ediEngineState.activeSession.fallbackGroupResponses[fallbackG11Mg.fallbackUniqueId]
        )
        assertEquals(
            EdiItemResponse.YES,
            stateB.ediEngineState.activeSession.currentGroupResponses["G11_LE_11_1"]
        )
        assertEquals(
            "Nota exclusiva Paciente B en G11",
            stateB.ediEngineState.activeSession.itemClinicalNotes["G11_MG_11_1"]
        )
        assertNull(stateB.ediEngineState.activeSession.itemClinicalNotes["G8_MG_8_1"])
        assertNull(stateB.ediEngineState.activeSession.itemClinicalNotes["G6_MG_6_1"])
        assertFalse(stateB.ediGlobalEvaluation.isDefinitive)
        assertEquals("Evaluación recuperada", stateB.persistenceStatusMessage)

        // Verificar que Paciente C se guardó antes del cambio y puede continuarse con su respuesta intacta
        assertTrue(vm.continueEvaluationFromHistorySuspend(uuidC))
        val stateC = vm.uiState.value
        assertEquals(uuidC, stateC.evaluationUuid)
        assertEquals("Paciente C · Santiago", stateC.patientIdentification.patientIdentifierOrAlias)
        assertEquals(6, stateC.ediEffectiveGroupNumber)
        assertEquals(
            EdiItemResponse.YES,
            stateC.ediEngineState.activeSession.currentGroupResponses["G6_MG_6_1"]
        )
        assertEquals(
            "Nota exclusiva Paciente C",
            stateC.ediEngineState.activeSession.itemClinicalNotes["G6_MG_6_1"]
        )
    }

    @Test
    fun `caso J pulsar Ver sobre una evaluacion completada abre en modo solo lectura sin sobrescribir el resultado`() = runBlocking {
        val vm = createPersistentViewModel()

        val completedUuid = vm.uiState.value.evaluationUuid
        vm.updatePatientIdentification("Paciente Completado", "EXP-COMP", "")
        vm.updateFechaNacimiento("15/04/2025")
        vm.updateFechaEvaluacion("15/06/2026")
        vm.confirmEdiTermWithoutWeeks()
        populateCompleteGreenGroup8InViewModel(vm)
        assertTrue(vm.saveCurrentEvaluationManuallySuspend() is SaveEvaluationResult.Success)

        // Iniciar nueva evaluación y luego abrir la evaluación completada con «Ver»
        assertTrue(vm.startNewPatientEvaluationSuspend())
        assertTrue(vm.viewCompletedEvaluationFromHistorySuspend(completedUuid))

        val viewedState = vm.uiState.value
        assertEquals(completedUuid, viewedState.evaluationUuid)
        assertTrue(viewedState.isReadOnlyConsultationMode)
        assertTrue(viewedState.ediGlobalEvaluation.isDefinitive)
        assertEquals(EdiGlobalTrafficLight.VERDE, viewedState.ediGlobalEvaluation.trafficLight)

        // Intentar alterar una respuesta en modo solo lectura -> debe bloquearse sin sobrescribir el resultado Verde
        vm.recordEdiCurrentItemResponse("G8_ALA_8_1", EdiItemResponse.YES)
        assertEquals(
            EdiItemResponse.NO,
            vm.uiState.value.ediEngineState.activeSession.currentGroupResponses["G8_ALA_8_1"]
        )
        assertEquals(EdiGlobalTrafficLight.VERDE, vm.uiState.value.ediGlobalEvaluation.trafficLight)

        // Habilitar edición clínica explícitamente -> permite editar con trazabilidad
        vm.enableEditingForViewedCompletedEvaluation("Corrección autorizada por especialista")
        assertFalse(vm.uiState.value.isReadOnlyConsultationMode)
        assertNotNull(vm.uiState.value.readOnlyEditableUnlockTrail)
        vm.recordEdiCurrentItemResponse("G8_ALA_8_1", EdiItemResponse.YES)
        assertEquals(
            EdiItemResponse.YES,
            vm.uiState.value.ediEngineState.activeSession.currentGroupResponses["G8_ALA_8_1"]
        )
    }

    @Test
    fun `caso L M y N Eliminar solicita confirmacion elimina solo el registro seleccionado y maneja eliminacion de activa`() = runBlocking {
        val vm = createPersistentViewModel()

        // Crear Evaluación 1
        val uuid1 = vm.uiState.value.evaluationUuid
        vm.updatePatientIdentification("Paciente 1", "EXP-001", "")
        vm.updateFechaNacimiento("15/04/2025")
        vm.updateFechaEvaluacion("15/06/2026")
        vm.confirmEdiTermWithoutWeeks()
        vm.recordEdiCurrentItemResponse("G8_MG_8_1", EdiItemResponse.YES)
        assertTrue(vm.saveCurrentEvaluationManuallySuspend() is SaveEvaluationResult.Success)

        // Crear Evaluación 2 (activa)
        assertTrue(vm.startNewPatientEvaluationSuspend())
        val uuid2 = vm.uiState.value.evaluationUuid
        vm.updatePatientIdentification("Paciente 2", "EXP-002", "")
        vm.updateFechaNacimiento("15/03/2024")
        vm.updateFechaEvaluacion("15/06/2026")
        vm.confirmEdiTermWithoutWeeks()
        vm.recordEdiCurrentItemResponse("G11_MG_11_1", EdiItemResponse.YES)
        assertTrue(vm.saveCurrentEvaluationManuallySuspend() is SaveEvaluationResult.Success)

        assertEquals(2, vm.uiState.value.savedEvaluationHistory.size)

        // Caso L: Pulsar «Eliminar» solicita confirmación y no elimina sin confirmar
        vm.requestDeleteEvaluationFromHistory(uuid1)
        assertNotNull(vm.uiState.value.pendingDeleteEvaluationSummary)
        assertEquals(uuid1, vm.uiState.value.pendingDeleteEvaluationSummary?.evaluationUuid)
        assertEquals(2, repository.listSavedEvaluationSummaries().size)

        vm.cancelDeleteEvaluationFromHistory()
        assertNull(vm.uiState.value.pendingDeleteEvaluationSummary)
        assertEquals(2, repository.listSavedEvaluationSummaries().size)

        // Caso M: Al confirmar eliminación de Evaluación 1, se elimina solo ese registro de Room
        vm.requestDeleteEvaluationFromHistory(uuid1)
        assertTrue(vm.confirmDeleteEvaluationFromHistorySuspend())

        assertNull(vm.uiState.value.pendingDeleteEvaluationSummary)
        val remainingAfterFirstDelete = repository.listSavedEvaluationSummaries()
        assertEquals(1, remainingAfterFirstDelete.size)
        assertEquals(uuid2, remainingAfterFirstDelete.first().evaluationUuid)
        assertEquals(uuid2, vm.uiState.value.evaluationUuid)

        // Caso N: Eliminar la evaluación activa inicia una evaluación limpia sin estado huérfano
        vm.requestDeleteEvaluationFromHistory(uuid2)
        assertTrue(vm.confirmDeleteEvaluationFromHistorySuspend())

        assertTrue(repository.listSavedEvaluationSummaries().isEmpty())
        assertTrue(vm.uiState.value.savedEvaluationHistory.isEmpty())
        assertNotEquals(uuid2, vm.uiState.value.evaluationUuid)
        assertEquals(0, vm.uiState.value.ediMandatoryItemsProgress.first)
    }

    @Test
    fun `caso O y P evaluacion anonima muestra identificador legible y evaluacion con alias muestra el dato tras descifrar`() = runBlocking {
        val vm = createPersistentViewModel()

        // Caso O: Evaluación anónima sin alias ni expediente
        val anonymousUuid = vm.uiState.value.evaluationUuid
        vm.updateFechaNacimiento("15/04/2025")
        vm.updateFechaEvaluacion("15/06/2026")
        vm.confirmEdiTermWithoutWeeks()
        assertTrue(vm.saveCurrentEvaluationManuallySuspend() is SaveEvaluationResult.Success)

        // Caso P: Evaluación con alias y expediente
        assertTrue(vm.startNewPatientEvaluationSuspend())
        val identifiedUuid = vm.uiState.value.evaluationUuid
        vm.updatePatientIdentification("Camila Rojas", "HC-9988", "")
        vm.updateFechaNacimiento("15/03/2024")
        vm.updateFechaEvaluacion("15/06/2026")
        vm.confirmEdiTermWithoutWeeks()
        assertTrue(vm.saveCurrentEvaluationManuallySuspend() is SaveEvaluationResult.Success)

        val history = vm.uiState.value.savedEvaluationHistory
        val anonSummary = history.first { it.evaluationUuid == anonymousUuid }
        val idSummary = history.first { it.evaluationUuid == identifiedUuid }

        val anonLabel = anonSummary.patientIdentification.displayLabelWithAnonymousFallback(anonymousUuid)
        assertTrue(anonLabel.startsWith("Evaluación anónima · ID "))

        val idLabel = idSummary.patientIdentification.displayLabelWithAnonymousFallback(identifiedUuid)
        assertEquals("Camila Rojas (Exp. HC-9988)", idLabel)

        // Verificar que en SQLite crudo el nombre "Camila Rojas" está cifrado
        val rawEntity = database.evaluationDao().getByUuid(identifiedUuid)!!
        assertFalse(rawEntity.encryptedPatientHeader.contains("Camila Rojas"))
    }

    @Test
    fun `caso Q si falla el guardado de la evaluacion activa antes de cambiar de paciente se bloquea el cambio sin perder datos`() = runBlocking {
        var shouldFailStorage = false
        val switchableCipher = object : CredEdiCipherProvider {
            override fun encrypt(plaintext: ByteArray, associatedData: ByteArray?): String {
                if (shouldFailStorage) {
                    throw GeneralSecurityException("Fallo simulado de Keystore al guardar antes del cambio")
                }
                return cipherProvider.encrypt(plaintext, associatedData)
            }

            override fun decrypt(ciphertextPayload: String, associatedData: ByteArray?): ByteArray {
                return cipherProvider.decrypt(ciphertextPayload, associatedData)
            }
        }
        val repoWithToggleFailure = CredEdiPersistenceRepository(
            evaluationDao = database.evaluationDao(),
            cipherProvider = switchableCipher,
            clockEpochMillis = {
                simulatedClockMillis += 1_000L
                simulatedClockMillis
            }
        )
        val vm = createPersistentViewModel(repo = repoWithToggleFailure, autoRestoreOnInit = false)

        // Guardar Paciente 1 normalmente
        val uuid1 = vm.uiState.value.evaluationUuid
        vm.updatePatientIdentification("Paciente 1", "EXP-1", "")
        vm.updateFechaNacimiento("15/04/2025")
        vm.updateFechaEvaluacion("15/06/2026")
        vm.confirmEdiTermWithoutWeeks()
        assertTrue(vm.saveCurrentEvaluationManuallySuspend() is SaveEvaluationResult.Success)

        // Iniciar Paciente 2 y registrar cambios en memoria
        assertTrue(vm.startNewPatientEvaluationSuspend())
        val uuid2 = vm.uiState.value.evaluationUuid
        vm.updatePatientIdentification("Paciente 2 con datos no guardados", "EXP-2", "")
        vm.updateFechaNacimiento("15/03/2024")
        vm.updateFechaEvaluacion("15/06/2026")
        vm.confirmEdiTermWithoutWeeks()
        vm.recordEdiCurrentItemResponse("G11_MG_11_1", EdiItemResponse.YES)

        // Simular fallo de guardado al intentar cambiar a Paciente 1 o crear Nueva Evaluación
        shouldFailStorage = true
        val switchResult = vm.continueEvaluationFromHistorySuspend(uuid1)
        assertFalse(switchResult)

        // El cambio debe haberse bloqueado y los datos de Paciente 2 deben seguir intactos en memoria
        assertEquals(uuid2, vm.uiState.value.evaluationUuid)
        assertEquals("Paciente 2 con datos no guardados", vm.uiState.value.patientIdentification.patientIdentifierOrAlias)
        assertEquals(
            EdiItemResponse.YES,
            vm.uiState.value.ediEngineState.activeSession.currentGroupResponses["G11_MG_11_1"]
        )
        assertEquals(AutosaveUiIndicatorState.ERROR, vm.uiState.value.persistenceIndicatorState)
        assertTrue(
            vm.uiState.value.persistenceStatusMessage.orEmpty()
                .contains("Error de guardado", ignoreCase = true)
        )

        val newEvalResult = vm.startNewPatientEvaluationSuspend()
        assertFalse(newEvalResult)
        assertEquals(uuid2, vm.uiState.value.evaluationUuid)
    }

    @Test
    fun `caso R si una evaluacion seleccionada del historial esta corrupta se informa el error sin destruir la evaluacion activa`() = runBlocking {
        val vm = createPersistentViewModel()

        // Guardar Paciente 1
        val uuid1 = vm.uiState.value.evaluationUuid
        vm.updatePatientIdentification("Paciente 1", "EXP-1", "")
        vm.updateFechaNacimiento("15/04/2025")
        vm.updateFechaEvaluacion("15/06/2026")
        vm.confirmEdiTermWithoutWeeks()
        assertTrue(vm.saveCurrentEvaluationManuallySuspend() is SaveEvaluationResult.Success)

        // Iniciar Paciente 2 activo con respuestas válidas
        assertTrue(vm.startNewPatientEvaluationSuspend())
        val uuid2 = vm.uiState.value.evaluationUuid
        vm.updatePatientIdentification("Paciente 2 Activo", "EXP-2", "")
        vm.updateFechaNacimiento("15/03/2024")
        vm.updateFechaEvaluacion("15/06/2026")
        vm.confirmEdiTermWithoutWeeks()
        vm.recordEdiCurrentItemResponse("G11_MG_11_1", EdiItemResponse.YES)
        assertTrue(vm.saveCurrentEvaluationManuallySuspend() is SaveEvaluationResult.Success)

        // Corromper el payload cifrado de Paciente 1 en SQLite
        val entity1 = database.evaluationDao().getByUuid(uuid1)!!
        database.evaluationDao().insertOrReplaceInternal(
            entity1.copy(
                plaintextSha256 = "0000000000000000000000000000000000000000000000000000000000000000"
            )
        )

        // Intentar continuar Paciente 1 desde el historial -> debe fallar de forma controlada
        val loaded = vm.continueEvaluationFromHistorySuspend(uuid1)
        assertFalse(loaded)

        // La evaluación activa (Paciente 2) no debe haberse destruido
        assertEquals(uuid2, vm.uiState.value.evaluationUuid)
        assertEquals("Paciente 2 Activo", vm.uiState.value.patientIdentification.patientIdentifierOrAlias)
        assertEquals(
            EdiItemResponse.YES,
            vm.uiState.value.ediEngineState.activeSession.currentGroupResponses["G11_MG_11_1"]
        )
        assertNotNull(vm.uiState.value.persistenceRecoveryErrorMessage)
    }

    @Test
    fun `caso S y T evaluacion incompleta recuperada del historial no emite semaforo definitivo ni diagnosticos falsos con peso talla y PC vacios`() = runBlocking {
        val vm = createPersistentViewModel()

        val uuidIncomplete = vm.uiState.value.evaluationUuid
        vm.updatePatientIdentification("Paciente Sin Antropometría", "EXP-ANTRO", "")
        vm.updateFechaNacimiento("15/04/2025")
        vm.updateFechaEvaluacion("15/06/2026")
        vm.confirmEdiTermWithoutWeeks()
        // Peso, talla y PC completamente vacíos
        vm.updatePesoInput("")
        vm.updateLongitudInput("")
        vm.updatePerimetroCefalicoInput("")
        // Solo 1 reactivo EDI respondido
        vm.recordEdiCurrentItemResponse("G8_MG_8_1", EdiItemResponse.YES)
        assertTrue(vm.saveCurrentEvaluationManuallySuspend() is SaveEvaluationResult.Success)

        // Cambiar a una nueva evaluación y luego recuperar uuidIncomplete desde el historial
        assertTrue(vm.startNewPatientEvaluationSuspend())
        assertTrue(vm.continueEvaluationFromHistorySuspend(uuidIncomplete))

        val restored = vm.uiState.value
        // Caso S: Evaluación incompleta nunca muestra semáforo definitivo
        assertFalse(restored.ediGlobalEvaluation.isDefinitive)
        val summaryInHistory = restored.savedEvaluationHistory.first { it.evaluationUuid == uuidIncomplete }
        assertFalse(summaryInHistory.isDefinitiveSemaphore)
        assertEquals("INCOMPLETE", summaryInHistory.semaphoreCode)
        assertEquals("Incompleto / En progreso", summaryInHistory.semaphoreLabel)

        // Caso T: Peso, talla y PC vacíos no producen diagnósticos falsos (ANTRO-02 y PC/E-01)
        assertFalse(restored.hasPesoIngresado)
        assertFalse(restored.hasLongitudIngresada)
        assertFalse(restored.hasPerimetroCefalicoIngresado)
        assertEquals("", restored.pesoInput)
        assertEquals("", restored.longitudInput)
        assertEquals("", restored.perimetroCefalicoInput)
        assertEquals(PerimetroCefalicoCaptureState.EMPTY_UNREGISTERED, restored.perimetroCefalicoCaptureState)
        assertEquals(GlobalDiagnosisCompleteness.UNAVAILABLE, restored.globalDiagnosis.completeness)
        assertEquals("Sin medición", restored.evalPCE.shortClassification)
        assertEquals("Sin medición", restored.evalPE.shortClassification)
        assertEquals("Sin medición", restored.evalTE.shortClassification)
        assertEquals("Sin medición", restored.evalPT.shortClassification)
        assertFalse(restored.globalDiagnosis.title.contains("Desnutrición", ignoreCase = true))
        assertFalse(restored.evalPCE.fullClassification.contains("Microcefalia", ignoreCase = true))
    }

    @Test
    fun `lote 3B simplificacion visual mantiene Guardar y Ver historial en panel principal y traslada Nueva evaluacion y Nuevo episodio a desplegables funcionales`() = runBlocking<Unit> {
        val vm = createPersistentViewModel()
        vm.updatePatientIdentification("Paciente Lote 3B", "EXP-3B", "")
        vm.updateFechaNacimiento("15/04/2025")
        vm.updateFechaEvaluacion("15/06/2026")
        vm.confirmEdiTermWithoutWeeks()
        vm.recordEdiCurrentItemResponse("G8_MG_8_1", EdiItemResponse.YES)
        val initialUuid = vm.uiState.value.evaluationUuid
        vm.selectTab(AppTab.EDI)

        composeRule.setContent {
            CredEdiApp(viewModel = vm)
        }

        // 1. Panel principal: Guardar evaluación, Ver historial y Nueva evaluación (sustituyendo Nuevo episodio)
        composeRule.onNodeWithTag("edi_summary_save_evaluation_btn")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag("edi_summary_toggle_history_btn")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag("edi_summary_new_evaluation_btn")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag("edi_summary_new_episode_btn").assertDoesNotExist()
        composeRule.onNodeWithTag("edi_summary_correct_birth_date_btn").assertDoesNotExist()

        // 2. Confirmar que «Guardar evaluación» funciona y muestra confirmación
        composeRule.onNodeWithTag("edi_summary_save_evaluation_btn")
            .performScrollTo()
            .performClick()
        vm.awaitPendingAutosave()
        composeRule.waitForIdle()
        assertEquals(AutosaveUiIndicatorState.SAVED, vm.uiState.value.persistenceIndicatorState)
        composeRule.onNodeWithTag("edi_summary_persistence_status_banner")
            .performScrollTo()
            .assertIsDisplayed()

        // 3. Desplegar «Ver historial»: muestra las evaluaciones guardadas sin la tarjeta «Gestión multipaciente»
        composeRule.onNodeWithTag("edi_summary_toggle_history_btn")
            .performScrollTo()
            .performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("edi_summary_saved_history_list")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("Gestión multipaciente (acción secundaria)").assertDoesNotExist()

        // Plegar «Ocultar historial» oculta la lista
        composeRule.onNodeWithTag("edi_summary_toggle_history_btn")
            .performScrollTo()
            .performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("edi_summary_saved_history_list").assertDoesNotExist()

        // 4. El motor interno de episodios EDI continúa funcionando sin regresión
        assertEquals("EP_1", vm.uiState.value.ediEngineState.currentWorkEpisodeId)
        vm.startNewEdiEvaluationEpisode()
        assertEquals("EP_2", vm.uiState.value.ediEngineState.currentWorkEpisodeId)

        // 5. La evaluación inicial continúa intacta en Room
        assertTrue(repository.listSavedEvaluationSummaries().any { it.evaluationUuid == initialUuid })
    }
}
