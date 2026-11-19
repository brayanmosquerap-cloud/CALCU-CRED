package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ApplicationProvider
import com.example.model.AppTab
import com.example.model.AutosaveUiIndicatorState
import com.example.model.EdiDevArea
import com.example.model.EdiGlobalTrafficLight
import com.example.model.EdiGroupChangeResolutionAction
import com.example.model.EdiItemResponse
import com.example.model.EdiOfficialCatalog
import com.example.model.GlobalDiagnosisCompleteness
import com.example.model.NewEvaluationCreationResult
import com.example.model.PerimetroCefalicoCaptureState
import com.example.model.SexoBiologico
import com.example.persistence.CredEdiCipherProvider
import com.example.persistence.CredEdiDatabase
import com.example.persistence.CredEdiPersistenceRepository
import com.example.persistence.JvmAesGcmCipherProvider
import com.example.persistence.SaveEvaluationResult
import com.example.util.EdiPdfExporter
import com.example.viewmodel.CredEdiViewModel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.security.GeneralSecurityException

/**
 * MEDIO-06 — LOTE 3D:
 * Suite automatizada obligatoria que verifica los 17 criterios de la Sección 10:
 * - Simplificación de «Ver historial» (sin tarjeta «Gestión multipaciente»).
 * - Sustitución de «Nuevo episodio» por «Nueva evaluación» con diálogo previo de identificación.
 * - Retiro visual de «Corregir fecha de nacimiento del mismo paciente» manteniendo protecciones clínicas.
 * - Validación de nombre y DNI peruano de 8 dígitos (ceros iniciales), flujo explícito de identificación pendiente,
 *   bloqueo ante fallo de guardado, aislamiento multipaciente, DNI enmascarado en listados, motor de episodios
 *   y retrocesos EDI, autoguardado y congruencia del PDF con evaluación restaurada.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class EdiMedio06Lote3DHistorySimplificationAndNewEvaluationFlowTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var database: CredEdiDatabase
    private lateinit var cipherProvider: JvmAesGcmCipherProvider
    private lateinit var repository: CredEdiPersistenceRepository
    private var simulatedClockMillis: Long = 1_783_000_000_000L

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
        autoRestoreOnInit: Boolean = false,
        autosaveDebounceMs: Long = 10L
    ): CredEdiViewModel {
        return CredEdiViewModel(
            persistenceRepository = repo,
            autoRestoreOnInit = autoRestoreOnInit,
            autosaveDebounceMs = autosaveDebounceMs
        )
    }

    private fun populateCompleteGreenGroup8(viewModel: CredEdiViewModel) {
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
    fun `lote 3D 1 y 2 - historial simplificado sin Gestion multipaciente Nueva evaluacion sustituye Nuevo episodio y se retira Corregir fecha`() = runBlocking<Unit> {
        val vm = createPersistentViewModel()
        vm.updatePatientFullName("Mateo Sebastián Paredes")
        vm.updatePatientDni("00451234")
        vm.updateFechaNacimiento("15/04/2025")
        vm.updateFechaEvaluacion("15/06/2026")
        vm.confirmEdiTermWithoutWeeks()
        assertTrue(vm.saveCurrentEvaluationManuallySuspend() is SaveEvaluationResult.Success)
        val savedUuid = vm.uiState.value.evaluationUuid

        vm.selectTab(AppTab.EDI)
        composeRule.setContent {
            CredEdiApp(viewModel = vm)
        }

        // Historial inicialmente plegado
        assertFalse(vm.uiState.value.isEvaluationHistoryExpanded)
        composeRule.onNodeWithTag("edi_summary_saved_history_list").assertDoesNotExist()

        // Aparece «Nueva evaluación» en el apartado secundario (sustituyendo «Nuevo episodio» / «Opciones de episodio EDI»)
        composeRule.onNodeWithTag("edi_summary_new_evaluation_btn")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag("edi_summary_new_episode_btn").assertDoesNotExist()
        composeRule.onNodeWithTag("edi_summary_toggle_episode_options_btn").assertDoesNotExist()
        composeRule.onNodeWithText("Nuevo episodio").assertDoesNotExist()
        composeRule.onNodeWithText("Opciones de episodio EDI").assertDoesNotExist()

        // «Corregir fecha de nacimiento del mismo paciente» retirado de la interfaz
        composeRule.onNodeWithTag("edi_summary_correct_birth_date_btn").assertDoesNotExist()
        composeRule.onNodeWithText("Corregir fecha de nacimiento del mismo paciente").assertDoesNotExist()

        // Al desplegar «Ver historial», no existe la tarjeta «Gestión multipaciente» ni un segundo botón «Nueva evaluación»
        composeRule.onNodeWithTag("edi_summary_toggle_history_btn")
            .performScrollTo()
            .performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("edi_summary_saved_history_list")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("Gestión multipaciente (acción secundaria)").assertDoesNotExist()
        composeRule.onNodeWithText("Guarda la evaluación activa y abre una nueva ficha limpia para otro paciente.").assertDoesNotExist()

        // El listado conserva la evaluación guardada con DNI parcialmente oculto (Sección 8)
        composeRule.onNodeWithTag("edi_summary_history_patient_label_$savedUuid")
            .performScrollTo()
            .assertTextEquals("Mateo Sebastián Paredes")
        composeRule.onNodeWithTag("edi_summary_history_patient_dni_$savedUuid")
            .performScrollTo()
            .assertTextEquals("DNI: ••••1234")
    }

    @Test
    fun `lote 3D 3 4 y 5 - Nueva evaluacion abre dialogo de identificacion Cancelar no crea registro y DNI invalido bloquea creacion`() = runBlocking<Unit> {
        val vm = createPersistentViewModel()
        val originalUuid = vm.uiState.value.evaluationUuid
        vm.updatePatientFullName("Paciente Inicial")
        vm.updatePatientDni("12345678")
        vm.updateFechaNacimiento("15/04/2025")
        vm.updateFechaEvaluacion("15/06/2026")
        vm.confirmEdiTermWithoutWeeks()
        assertTrue(vm.saveCurrentEvaluationManuallySuspend() is SaveEvaluationResult.Success)
        val initialDbCount = repository.listSavedEvaluationSummaries().size
        assertEquals(1, initialDbCount)

        vm.selectTab(AppTab.EDI)
        composeRule.setContent {
            CredEdiApp(viewModel = vm)
        }

        // 3. Pulsar «Nueva evaluación» abre primero el diálogo de identificación sin crear ningún registro
        composeRule.onNodeWithTag("edi_summary_new_evaluation_btn")
            .performScrollTo()
            .performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("new_evaluation_patient_id_dialog").assertIsDisplayed()
        assertEquals(originalUuid, vm.uiState.value.evaluationUuid)
        assertEquals(1, repository.listSavedEvaluationSummaries().size)

        // 4. Pulsar «Cancelar» cierra el diálogo sin crear ningún registro ni modificar al paciente actual
        composeRule.onNodeWithTag("new_eval_full_name_input").performTextInput("Paciente Cancelado")
        composeRule.onNodeWithTag("new_eval_dni_input").performTextInput("87654321")
        composeRule.onNodeWithTag("new_eval_cancel_btn").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("new_evaluation_patient_id_dialog").assertDoesNotExist()
        assertEquals(originalUuid, vm.uiState.value.evaluationUuid)
        assertEquals("Paciente Inicial", vm.uiState.value.patientIdentification.patientIdentifierOrAlias)
        assertEquals("12345678", vm.uiState.value.patientIdentification.clinicalRecordCode)
        assertEquals(1, repository.listSavedEvaluationSummaries().size)

        // 5. DNI inválido (< 8 dígitos o con letras) NO genera una evaluación nueva y muestra error
        composeRule.onNodeWithTag("edi_summary_new_evaluation_btn")
            .performScrollTo()
            .performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("new_eval_full_name_input").performTextReplacement("Sofía Belén Quispe")
        composeRule.onNodeWithTag("new_eval_dni_input").performTextReplacement("00AB45") // Solo 4 dígitos válidos
        composeRule.onNodeWithTag("new_eval_confirm_btn").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("new_evaluation_patient_id_dialog").assertIsDisplayed()
        composeRule.onNodeWithTag("new_eval_validation_error")
            .performScrollTo()
            .assertIsDisplayed()
        assertEquals(originalUuid, vm.uiState.value.evaluationUuid)
        assertEquals(1, repository.listSavedEvaluationSummaries().size)

        // También validar en ViewModel que DNI con caracteres no numéricos o longitud != 8 es rechazado
        val directInvalidResult = vm.createNewPatientEvaluationWithIdentificationSuspend(
            fullName = "Sofía Belén Quispe",
            dni = "1234ABCD"
        )
        assertTrue(directInvalidResult is NewEvaluationCreationResult.ValidationError)
        assertEquals(originalUuid, vm.uiState.value.evaluationUuid)
        assertEquals(1, repository.listSavedEvaluationSummaries().size)
    }

    @Test
    fun `lote 3D 6 - fallo de guardado de la evaluacion actual bloquea la creacion de un nuevo registro sin perdida`() = runBlocking {
        var shouldFailEncryption = false
        val switchableCipher = object : CredEdiCipherProvider {
            override fun encrypt(plaintext: ByteArray, associatedData: ByteArray?): String {
                if (shouldFailEncryption) {
                    throw GeneralSecurityException("Error simulado de Keystore al guardar antes de crear")
                }
                return cipherProvider.encrypt(plaintext, associatedData)
            }

            override fun decrypt(ciphertextPayload: String, associatedData: ByteArray?): ByteArray {
                return cipherProvider.decrypt(ciphertextPayload, associatedData)
            }
        }
        val failingRepo = CredEdiPersistenceRepository(
            evaluationDao = database.evaluationDao(),
            cipherProvider = switchableCipher,
            clockEpochMillis = {
                simulatedClockMillis += 1_000L
                simulatedClockMillis
            }
        )
        val vm = createPersistentViewModel(repo = failingRepo)
        val originalUuid = vm.uiState.value.evaluationUuid

        vm.updatePatientFullName("Paciente Protegido")
        vm.updatePatientDni("44556677")
        vm.updateFechaNacimiento("15/04/2025")
        vm.updateFechaEvaluacion("15/06/2026")
        vm.confirmEdiTermWithoutWeeks()
        vm.recordEdiCurrentItemResponse("G8_MG_8_1", EdiItemResponse.YES)

        // Provocar fallo de almacenamiento antes de crear nueva evaluación
        shouldFailEncryption = true
        val creationResult = vm.createNewPatientEvaluationWithIdentificationSuspend(
            fullName = "Nuevo Paciente Bloqueado",
            dni = "00998877"
        )

        assertTrue(creationResult is NewEvaluationCreationResult.PersistenceError)
        assertEquals(originalUuid, vm.uiState.value.evaluationUuid)
        assertEquals("Paciente Protegido", vm.uiState.value.patientIdentification.patientIdentifierOrAlias)
        assertEquals("44556677", vm.uiState.value.patientIdentification.clinicalRecordCode)
        assertEquals(
            EdiItemResponse.YES,
            vm.uiState.value.ediEngineState.activeSession.currentGroupResponses["G8_MG_8_1"]
        )
        assertEquals(AutosaveUiIndicatorState.ERROR, vm.uiState.value.persistenceIndicatorState)
    }

    @Test
    fun `lote 3D 7 8 9 10 11 y 12 - creacion exitosa genera UUID limpio abre Calculadora conserva historial y aisla dos pacientes`() = runBlocking<Unit> {
        val vm = createPersistentViewModel()

        // Paciente 1 con datos completos antropométricos + EDI + nota clínica
        val uuid1 = vm.uiState.value.evaluationUuid
        vm.updatePatientFullName("Mateo Sebastián Paredes")
        vm.updatePatientDni("00451234")
        vm.selectSexo(SexoBiologico.NINO)
        vm.updateFechaNacimiento("15/04/2025")
        vm.updateFechaEvaluacion("15/06/2026")
        vm.confirmEdiTermWithoutWeeks()
        vm.updatePesoInput("10.2")
        vm.updateLongitudInput("77.0")
        vm.updatePerimetroCefalicoInput("46.5")
        vm.recordEdiCurrentItemResponse("G8_MG_8_1", EdiItemResponse.YES)
        vm.updateEdiItemClinicalNote("G8_MG_8_1", "Nota exclusiva de Mateo")
        vm.awaitPendingAutosave()

        vm.selectTab(AppTab.EDI)
        composeRule.setContent {
            CredEdiApp(viewModel = vm)
        }

        // Abrir diálogo desde «Nueva evaluación» y registrar Paciente 2 con nombre compuesto, tildes y DNI con ceros iniciales
        composeRule.onNodeWithTag("edi_summary_new_evaluation_btn")
            .performScrollTo()
            .performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("new_eval_full_name_input")
            .performTextInput("María José Núñez Córdova")
        composeRule.onNodeWithTag("new_eval_dni_input")
            .performTextInput("00891234")
        composeRule.onNodeWithTag("new_eval_confirm_btn")
            .performClick()
        composeRule.waitForIdle()

        // 7. Nuevo UUID independiente y diálogo cerrado
        composeRule.onNodeWithTag("new_evaluation_patient_id_dialog").assertDoesNotExist()
        val uuid2 = vm.uiState.value.evaluationUuid
        assertNotEquals(uuid1, uuid2)

        // 8 y 10 (Sección 6): Abre automáticamente pestaña Calculadora (sin tarjeta duplicada de identificación tras Lote 3E)
        assertEquals(AppTab.CALCULADORA, vm.uiState.value.currentTab)
        composeRule.onNodeWithTag("patient_identification_section").assertDoesNotExist()
        composeRule.onNodeWithTag("sexo_biologico_section").assertIsDisplayed()
        assertEquals("María José Núñez Córdova", vm.uiState.value.patientIdentification.patientIdentifierOrAlias)
        assertEquals("00891234", vm.uiState.value.patientIdentification.clinicalRecordCode)

        // 9. El nuevo paciente NO hereda ningún dato anterior (peso/talla/PC = Double.NaN, respuestas y notas vacías)
        val newPatientState = vm.uiState.value
        assertEquals("", newPatientState.fechaNacimiento)
        assertTrue(newPatientState.pesoKg.isNaN())
        assertTrue(newPatientState.longitudCm.isNaN())
        assertTrue(newPatientState.perimetroCefalicoCm.isNaN())
        assertEquals("", newPatientState.pesoInput)
        assertEquals("", newPatientState.longitudInput)
        assertEquals("", newPatientState.perimetroCefalicoInput)
        assertEquals(PerimetroCefalicoCaptureState.EMPTY_UNREGISTERED, newPatientState.perimetroCefalicoCaptureState)
        assertEquals(GlobalDiagnosisCompleteness.UNAVAILABLE, newPatientState.globalDiagnosis.completeness)
        assertFalse(newPatientState.ediEngineState.activeSession.hasRecordedClinicalResponses)
        assertTrue(newPatientState.ediEngineState.activeSession.currentGroupResponses.values.all { it == EdiItemResponse.UNANSWERED })
        assertTrue(newPatientState.ediEngineState.activeSession.itemClinicalNotes.isEmpty())
        assertTrue(newPatientState.ediEngineState.archivedSessionHistory.isEmpty())

        // 8 (en EDI): Al pasar a EDI, nombre y DNI aparecen en la sección superior de identificación (sin duplicar en panel inferior)
        vm.selectTab(AppTab.EDI)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("patient_full_name_input")
            .performScrollTo()
            .assertTextEquals("María José Núñez Córdova")
        composeRule.onNodeWithTag("patient_dni_input")
            .performScrollTo()
            .assertTextEquals("00891234")
        composeRule.onNodeWithTag("edi_summary_active_patient_label").assertDoesNotExist()
        composeRule.onNodeWithTag("edi_summary_active_patient_dni_label").assertDoesNotExist()

        // 10. La evaluación previa (Paciente 1) permanece intacta en el historial
        composeRule.onNodeWithTag("edi_summary_toggle_history_btn")
            .performScrollTo()
            .performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("edi_summary_history_patient_label_$uuid1")
            .performScrollTo()
            .assertTextEquals("Mateo Sebastián Paredes")
        composeRule.onNodeWithTag("edi_summary_history_patient_dni_$uuid1")
            .performScrollTo()
            .assertTextEquals("DNI: ••••1234")
        composeRule.onNodeWithTag("edi_summary_history_patient_label_$uuid2")
            .performScrollTo()
            .assertTextEquals("María José Núñez Córdova")
        composeRule.onNodeWithTag("edi_summary_history_patient_dni_$uuid2")
            .performScrollTo()
            .assertTextEquals("DNI: ••••1234") // ambos terminan en 1234, pero diferenciados por nombre y UUID

        // 11 y 12. Continuar Paciente 1 desde el historial restaura exactamente sus datos sin mezclar con Paciente 2
        composeRule.onNodeWithTag("edi_summary_history_continue_btn_$uuid1")
            .performScrollTo()
            .performClick()
        composeRule.waitForIdle()

        val restoredMateo = vm.uiState.value
        assertEquals(uuid1, restoredMateo.evaluationUuid)
        assertEquals("Mateo Sebastián Paredes", restoredMateo.patientIdentification.patientIdentifierOrAlias)
        assertEquals("00451234", restoredMateo.patientIdentification.clinicalRecordCode)
        assertEquals("10.2", restoredMateo.pesoInput)
        assertEquals("77.0", restoredMateo.longitudInput)
        assertEquals("46.5", restoredMateo.perimetroCefalicoInput)
        assertEquals(EdiItemResponse.YES, restoredMateo.ediEngineState.activeSession.currentGroupResponses["G8_MG_8_1"])
        assertEquals("Nota exclusiva de Mateo", restoredMateo.ediEngineState.activeSession.itemClinicalNotes["G8_MG_8_1"])

        // Confirmar flujo explícito de identificación pendiente sin DNI ficticio
        val pendingResult = vm.createNewPatientEvaluationWithIdentificationSuspend(
            fullName = "RN de Ana Torres",
            dni = "",
            allowPendingIdentification = true
        )
        assertTrue(pendingResult is NewEvaluationCreationResult.Success)
        assertEquals("RN de Ana Torres", vm.uiState.value.patientIdentification.patientIdentifierOrAlias)
        assertEquals("", vm.uiState.value.patientIdentification.clinicalRecordCode)
        assertEquals("DNI: No registrado", vm.uiState.value.patientIdentification.activePatientDniLine)
    }

    @Test
    fun `lote 3D 13 14 15 y 16 - fecha en Calculadora episodios y retrocesos EDI autoguardado y PDF congruente tras restaurar`() = runBlocking {
        val vm = createPersistentViewModel()
        val uuid = vm.uiState.value.evaluationUuid

        vm.updatePatientFullName("Lucas Gabriel Morales")
        vm.updatePatientDni("09123456")
        vm.selectSexo(SexoBiologico.NINO)
        vm.updateFechaEvaluacion("15/06/2026")
        vm.updateFechaNacimiento("15/04/2025") // 14m -> Grupo 8
        vm.confirmEdiTermWithoutWeeks()
        vm.updatePesoInput("10.3")
        vm.updateLongitudInput("77.8")
        vm.updatePerimetroCefalicoInput("46.9")

        // 14. Verificar retroceso EDI (fallar 2+ ítems en MG de G8 activa evaluación de grupo anterior G7) y episodio
        vm.recordEdiCurrentItemResponse("G8_MG_8_1", EdiItemResponse.NO)
        vm.recordEdiCurrentItemResponse("G8_MG_8_2", EdiItemResponse.NO)
        vm.recordEdiCurrentItemResponse("G8_MG_8_3", EdiItemResponse.NO)
        val fallbackState = vm.uiState.value.ediGlobalEvaluation.axis3AreasSummary.areaResults[EdiDevArea.MG]
        assertNotNull(fallbackState)
        assertTrue(fallbackState!!.fallbackRequired)
        assertEquals(7, fallbackState.fallbackSourceGroupNumber)

        // 13. Cambiar fecha de nacimiento desde Calculadora con respuestas EDI registradas
        // activa automáticamente la protección clínica de episodios (archiva sesión previa en EP_1,
        // abre EP_2 limpio en Grupo 11 sin mezclar respuestas y oculta episodios anteriores por R3)
        assertEquals("EP_1", vm.uiState.value.ediEngineState.currentWorkEpisodeId)
        vm.updateFechaNacimiento("15/04/2024")
        assertEquals("EP_2", vm.uiState.value.ediEngineState.currentWorkEpisodeId)
        assertEquals(11, vm.uiState.value.ediEffectiveGroupNumber)
        assertFalse(vm.uiState.value.ediEngineState.activeSession.hasRecordedClinicalResponses)
        assertEquals(1, vm.uiState.value.ediEngineState.previousEpisodesArchivedCount)

        // Volver a Grupo 8, completar evaluación Verde y verificar autoguardado (15)
        vm.updateFechaNacimiento("15/04/2025")
        vm.confirmEdiTermWithoutWeeks()
        assertEquals(8, vm.uiState.value.ediEffectiveGroupNumber)
        populateCompleteGreenGroup8(vm)
        vm.awaitPendingAutosave()
        assertEquals(AutosaveUiIndicatorState.SAVED, vm.uiState.value.persistenceIndicatorState)
        assertEquals(EdiGlobalTrafficLight.VERDE, vm.uiState.value.ediGlobalEvaluation.trafficLight)
        assertTrue(vm.uiState.value.ediGlobalEvaluation.isDefinitive)

        // Confirmar cifrado real en SQLite (ningún nombre ni DNI en texto claro)
        val rawEntity = database.evaluationDao().getByUuid(uuid)!!
        assertFalse(rawEntity.encryptedPatientHeader.contains("Lucas Gabriel Morales"))
        assertFalse(rawEntity.encryptedPatientHeader.contains("09123456"))

        // Simular reinicio del proceso y restaurar evaluación desde Room
        val vmRestored = createPersistentViewModel(autoRestoreOnInit = true)
        assertTrue(vmRestored.uiState.value.wasRestoredFromPersistence)
        assertEquals(uuid, vmRestored.uiState.value.evaluationUuid)
        assertEquals("Lucas Gabriel Morales", vmRestored.uiState.value.patientIdentification.patientIdentifierOrAlias)
        assertEquals("09123456", vmRestored.uiState.value.patientIdentification.clinicalRecordCode)
        assertEquals(EdiGlobalTrafficLight.VERDE, vmRestored.uiState.value.ediGlobalEvaluation.trafficLight)
        assertTrue(vmRestored.uiState.value.ediGlobalEvaluation.isDefinitive)

        // 16. Exportar PDF desde la evaluación restaurada y verificar congruencia (ficha oficial completa, no borrador)
        val pdfResult = EdiPdfExporter.exportarFichaOficialEdiPdf(context, vmRestored.uiState.value)
        assertTrue("Falló exportación PDF restaurada: ${pdfResult.message}", pdfResult.success)
        assertFalse("Evaluación completa restaurada debe emitir ficha oficial, no borrador", pdfResult.isDraft)
        assertNotNull(pdfResult.filePath)
        val pdfFile = File(pdfResult.filePath!!)
        assertTrue(pdfFile.exists() && pdfFile.length() > 10_000L)
        assertEquals("%PDF-", pdfFile.readBytes().copyOfRange(0, 5).toString(Charsets.US_ASCII))
    }
}
