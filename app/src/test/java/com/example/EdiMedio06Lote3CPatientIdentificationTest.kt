package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.getUnclippedBoundsInRoot
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
import com.example.model.EdiItemResponse
import com.example.model.SexoBiologico
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * MEDIO-06 — LOTE 3C:
 * Suite automatizada para validar el apartado «IDENTIFICACIÓN DEL PACIENTE» en Calculadora
 * (diseño fiel a la referencia visual), reglas de captura de Nombres y Apellidos y DNI
 * (numérico, máx. 8 dígitos, ceros iniciales preservados), autoguardado cifrado AES-256-GCM
 * en Room, recuperación tras reinicio, visualización automática en EDI sin duplicar campos
 * y aislamiento multipaciente.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class EdiMedio06Lote3CPatientIdentificationTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var database: CredEdiDatabase
    private lateinit var cipherProvider: JvmAesGcmCipherProvider
    private lateinit var repository: CredEdiPersistenceRepository
    private var simulatedClockMillis: Long = 1_782_000_000_000L

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
        autoRestoreOnInit: Boolean = true,
        autosaveDebounceMs: Long = 10L
    ): CredEdiViewModel {
        return CredEdiViewModel(
            persistenceRepository = repository,
            autoRestoreOnInit = autoRestoreOnInit,
            autosaveDebounceMs = autosaveDebounceMs
        )
    }

    @Test
    fun `lote 3C 1 - tarjeta IDENTIFICACION DEL PACIENTE al inicio de EDI con dos campos en una sola fila 65-35 y ausente en Calculadora`() {
        val vm = createPersistentViewModel(autoRestoreOnInit = false)

        composeRule.setContent {
            CredEdiApp(viewModel = vm)
        }

        // Lote 3E: En Calculadora ya no existe IDENTIFICACIÓN DEL PACIENTE; inicia con SEXO BIOLÓGICO
        composeRule.onNodeWithTag("patient_identification_section").assertDoesNotExist()
        composeRule.onNodeWithTag("sexo_biologico_section").assertIsDisplayed()

        // En EDI aparece al inicio del contenido
        vm.selectTab(AppTab.EDI)
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("patient_identification_section").assertIsDisplayed()
        composeRule.onNodeWithText("IDENTIFICACIÓN DEL PACIENTE").assertIsDisplayed()
        composeRule.onNodeWithText("NOMBRES Y APELLIDOS").assertIsDisplayed()
        composeRule.onNodeWithText("Ej. Mateo Sebastián Paredes").assertIsDisplayed()
        composeRule.onNodeWithText("DNI").assertIsDisplayed()
        composeRule.onNodeWithText("8 dígitos").assertIsDisplayed()

        // Verificar que ambos campos están en una sola fila horizontal con proporción ~65 % / ~35 %
        val nameBoxBounds = composeRule.onNodeWithTag("patient_full_name_container").getUnclippedBoundsInRoot()
        val dniBoxBounds = composeRule.onNodeWithTag("patient_dni_container").getUnclippedBoundsInRoot()
        assertTrue(
            "El campo NOMBRES Y APELLIDOS debe quedar a la izquierda de DNI en la misma fila",
            nameBoxBounds.right <= dniBoxBounds.left
        )
        assertEquals(
            "Ambos campos deben iniciar en la misma línea horizontal",
            nameBoxBounds.top.value,
            dniBoxBounds.top.value,
            1.5f
        )
        assertEquals(
            "Ambos campos deben tener la misma altura alineada",
            nameBoxBounds.bottom.value,
            dniBoxBounds.bottom.value,
            1.5f
        )

        val nameWidth = nameBoxBounds.right.value - nameBoxBounds.left.value
        val dniWidth = dniBoxBounds.right.value - dniBoxBounds.left.value
        val totalFieldsWidth = nameWidth + dniWidth
        val nameRatio = nameWidth / totalFieldsWidth
        val dniRatio = dniWidth / totalFieldsWidth
        assertTrue("El campo izquierdo debe ocupar ~65 % (actual: $nameRatio)", nameRatio in 0.60f..0.70f)
        assertTrue("El campo derecho debe ocupar ~35 % (actual: $dniRatio)", dniRatio in 0.30f..0.40f)
    }

    @Test
    fun `lote 3C 2 - captura de nombres largos y DNI numerico maximo 8 digitos preservando ceros iniciales`() {
        val vm = createPersistentViewModel(autoRestoreOnInit = false)
        vm.selectTab(AppTab.EDI)

        composeRule.setContent {
            CredEdiApp(viewModel = vm)
        }

        val longFullName = "Mateo Sebastián Paredes Huamancayo Velásquez"
        composeRule.onNodeWithTag("patient_full_name_input")
            .performTextInput(longFullName)
        composeRule.onNodeWithTag("patient_dni_input")
            .performTextInput("00AB45-1234999")
        composeRule.waitForIdle()

        assertEquals(longFullName, vm.uiState.value.patientIdentification.patientIdentifierOrAlias)
        assertEquals("00451234", vm.uiState.value.patientIdentification.clinicalRecordCode)

        // También validar ceros iniciales consecutivos en el ViewModel
        vm.updatePatientDni("000012345678")
        assertEquals("00001234", vm.uiState.value.patientIdentification.clinicalRecordCode)
        assertEquals(8, vm.uiState.value.patientIdentification.clinicalRecordCode.length)
    }

    @Test
    fun `lote 3C 3 - autoguardado cifrado AES-256-GCM en Room y recuperacion exacta tras reinicio del proceso`() = runBlocking {
        val vm1 = createPersistentViewModel(autoRestoreOnInit = false)
        val uuid = vm1.uiState.value.evaluationUuid

        vm1.updatePatientFullName("Mateo Sebastián Paredes")
        vm1.updatePatientDni("00451234")
        vm1.updateFechaNacimiento("15/04/2025")
        vm1.updateFechaEvaluacion("15/06/2026")
        vm1.confirmEdiTermWithoutWeeks()
        vm1.awaitPendingAutosave()

        assertEquals(AutosaveUiIndicatorState.SAVED, vm1.uiState.value.persistenceIndicatorState)

        // Confirmar cifrado real en SQLite: ni el nombre ni el DNI aparecen en texto claro
        val rawEntity = database.evaluationDao().getByUuid(uuid)
        assertTrue(rawEntity != null)
        assertFalse(rawEntity!!.encryptedPatientHeader.contains("Mateo Sebastián Paredes"))
        assertFalse(rawEntity.encryptedPatientHeader.contains("00451234"))
        assertFalse(rawEntity.encryptedClinicalPayload.contains("Mateo Sebastián Paredes"))
        assertFalse(rawEntity.encryptedClinicalPayload.contains("00451234"))

        // Simular reinicio completo de la aplicación con un ViewModel nuevo sobre la misma DB Room
        val vmRestarted = createPersistentViewModel(autoRestoreOnInit = true)

        assertTrue(vmRestarted.uiState.value.wasRestoredFromPersistence)
        assertEquals(uuid, vmRestarted.uiState.value.evaluationUuid)
        assertEquals("Mateo Sebastián Paredes", vmRestarted.uiState.value.patientIdentification.patientIdentifierOrAlias)
        assertEquals("00451234", vmRestarted.uiState.value.patientIdentification.clinicalRecordCode)
    }

    @Test
    fun `lote 3C 4 y 5 - visualizacion automatica en EDI e historial sin duplicar campos de edicion y fallback claro sin identificacion`() = runBlocking<Unit> {
        val vm = createPersistentViewModel(autoRestoreOnInit = false)
        val anonymousUuid = vm.uiState.value.evaluationUuid
        val shortAnonCode = anonymousUuid.removePrefix("EVAL_").take(8).uppercase()

        vm.selectTab(AppTab.EDI)
        composeRule.setContent {
            CredEdiApp(viewModel = vm)
        }

        // MEDIO-07: el panel de guardado ya no duplica el encabezado ni los textos de paciente activo / DNI
        composeRule.onNodeWithTag("edi_summary_active_patient_label").assertDoesNotExist()
        composeRule.onNodeWithTag("edi_summary_active_patient_dni_label").assertDoesNotExist()
        assertEquals(
            "Paciente activo: Sin identificación registrada · ID $shortAnonCode",
            vm.uiState.value.patientIdentification.activePatientHeaderLine(anonymousUuid)
        )

        // Registrar nombre y DNI desde la sección superior de EDI (Lote 3E)
        composeRule.onNodeWithTag("patient_full_name_input")
            .performScrollTo()
            .performTextReplacement("Mateo Sebastián Paredes")
        composeRule.onNodeWithTag("patient_dni_input")
            .performScrollTo()
            .performTextReplacement("12345678")
        vm.awaitPendingAutosave()
        assertTrue(vm.saveCurrentEvaluationManuallySuspend() is SaveEvaluationResult.Success)
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("patient_full_name_input")
            .performScrollTo()
            .assertTextEquals("Mateo Sebastián Paredes")
        composeRule.onNodeWithTag("patient_dni_input")
            .performScrollTo()
            .assertTextEquals("12345678")

        // Desplegar «Ver historial»: muestra nombre y DNI en tarjeta del historial y NO duplica campos de edición
        composeRule.onNodeWithTag("edi_summary_toggle_history_btn")
            .performScrollTo()
            .performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("edi_summary_history_patient_label_$anonymousUuid")
            .performScrollTo()
            .assertIsDisplayed()
            .assertTextEquals("Mateo Sebastián Paredes")
        composeRule.onNodeWithTag("edi_summary_history_patient_dni_$anonymousUuid")
            .performScrollTo()
            .assertIsDisplayed()
            .assertTextEquals("DNI: ••••5678")

        // Confirmar que los controles duplicados de edición de identificación ya no existen en EDI
        composeRule.onNodeWithTag("edi_summary_toggle_patient_id_btn").assertDoesNotExist()
        composeRule.onNodeWithTag("edi_summary_patient_alias_input").assertDoesNotExist()
        composeRule.onNodeWithTag("edi_summary_patient_record_input").assertDoesNotExist()
    }

    @Test
    fun `lote 3C 6 - multipaciente dos pacientes con nombres y DNI distintos permanecen aislados sin mezcla`() = runBlocking<Unit> {
        val vm = createPersistentViewModel(autoRestoreOnInit = false)

        // Paciente 1
        val uuid1 = vm.uiState.value.evaluationUuid
        vm.updatePatientFullName("Mateo Sebastián Paredes")
        vm.updatePatientDni("00451234")
        vm.selectSexo(SexoBiologico.NINO)
        vm.updateFechaNacimiento("15/04/2025")
        vm.updateFechaEvaluacion("15/06/2026")
        vm.confirmEdiTermWithoutWeeks()
        vm.recordEdiCurrentItemResponse("G8_MG_8_1", EdiItemResponse.YES)
        assertTrue(vm.saveCurrentEvaluationManuallySuspend() is SaveEvaluationResult.Success)

        // Crear nueva evaluación para Paciente 2
        assertTrue(vm.startNewPatientEvaluationSuspend())
        val uuid2 = vm.uiState.value.evaluationUuid
        assertNotEquals(uuid1, uuid2)
        // Nueva evaluación inicia limpia sin datos identificatorios del Paciente 1
        assertEquals("", vm.uiState.value.patientIdentification.patientIdentifierOrAlias)
        assertEquals("", vm.uiState.value.patientIdentification.clinicalRecordCode)

        vm.updatePatientFullName("Valeria Sofía Quispe Torres")
        vm.updatePatientDni("74829105")
        vm.selectSexo(SexoBiologico.NINA)
        vm.updateFechaNacimiento("15/03/2024")
        vm.updateFechaEvaluacion("15/06/2026")
        vm.confirmEdiTermWithoutWeeks()
        vm.recordEdiCurrentItemResponse("G11_MG_11_1", EdiItemResponse.NO)
        assertTrue(vm.saveCurrentEvaluationManuallySuspend() is SaveEvaluationResult.Success)

        // Verificar ambos pacientes en la UI del historial EDI
        vm.selectTab(AppTab.EDI)
        composeRule.setContent {
            CredEdiApp(viewModel = vm)
        }

        composeRule.onNodeWithTag("patient_full_name_input")
            .performScrollTo()
            .assertTextEquals("Valeria Sofía Quispe Torres")
        composeRule.onNodeWithTag("patient_dni_input")
            .performScrollTo()
            .assertTextEquals("74829105")
        composeRule.onNodeWithTag("edi_summary_active_patient_label").assertDoesNotExist()
        composeRule.onNodeWithTag("edi_summary_active_patient_dni_label").assertDoesNotExist()

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
            .assertTextEquals("Valeria Sofía Quispe Torres")
        composeRule.onNodeWithTag("edi_summary_history_patient_dni_$uuid2")
            .performScrollTo()
            .assertTextEquals("DNI: ••••9105")

        // Continuar Paciente 1 desde el historial y comprobar que Calculadora y EDI reflejan Paciente 1 sin mezcla
        composeRule.onNodeWithTag("edi_summary_history_continue_btn_$uuid1")
            .performScrollTo()
            .performClick()
        composeRule.waitForIdle()

        assertEquals(uuid1, vm.uiState.value.evaluationUuid)
        assertEquals("Mateo Sebastián Paredes", vm.uiState.value.patientIdentification.patientIdentifierOrAlias)
        assertEquals("00451234", vm.uiState.value.patientIdentification.clinicalRecordCode)
        assertEquals(SexoBiologico.NINO, vm.uiState.value.sexo)
        assertEquals(
            EdiItemResponse.YES,
            vm.uiState.value.ediEngineState.activeSession.currentGroupResponses["G8_MG_8_1"]
        )

        // Confirmar que los campos superiores de EDI reflejan exactamente los datos del Paciente 1
        composeRule.onNodeWithTag("patient_full_name_input")
            .performScrollTo()
            .assertTextEquals("Mateo Sebastián Paredes")
        composeRule.onNodeWithTag("patient_dni_input")
            .performScrollTo()
            .assertTextEquals("00451234")
    }
}
