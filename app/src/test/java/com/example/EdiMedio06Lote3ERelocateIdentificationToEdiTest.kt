package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
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
import com.example.model.EdiGlobalTrafficLight
import com.example.model.EdiItemResponse
import com.example.model.EdiOfficialCatalog
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
 * MEDIO-06 — LOTE 3E:
 * Suite automatizada obligatoria que verifica los 10 criterios de la Sección 9:
 * 1. Identificación visible al inicio de la ventana EDI (antes de ejes, cuestionarios, botones y panel de historial).
 * 2. Identificación ausente de Calculadora (empezando por «SEXO BIOLÓGICO»).
 * 3. Campos alineados horizontalmente (~65 % «NOMBRES Y APELLIDOS» y ~35 % «DNI»).
 * 4. Autoguardado de nombre y DNI (incluyendo preservación de ceros iniciales y entradas parciales con validación).
 * 5. Recuperación exacta tras reinicio.
 * 6. Historial con identificación correcta.
 * 7. Cambio entre dos pacientes sin mezcla de datos.
 * 8. Modo solo lectura para evaluaciones completadas (campos de identificación bloqueados hasta habilitar edición).
 * 9. Nueva evaluación con datos propios reflejados automáticamente en los campos superiores de EDI.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class EdiMedio06Lote3ERelocateIdentificationToEdiTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var database: CredEdiDatabase
    private lateinit var cipherProvider: JvmAesGcmCipherProvider
    private lateinit var repository: CredEdiPersistenceRepository
    private var simulatedClockMillis: Long = 1_784_000_000_000L

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
        autoRestoreOnInit: Boolean = false,
        autosaveDebounceMs: Long = 10L
    ): CredEdiViewModel {
        return CredEdiViewModel(
            persistenceRepository = repository,
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
    fun `lote 3E 1 2 y 3 - identificacion ausente en Calculadora y visible al inicio de EDI con fila horizontal 65-35`() {
        val vm = createPersistentViewModel()

        composeRule.setContent {
            CredEdiApp(viewModel = vm)
        }

        // 2. Identificación ausente en Calculadora; empieza por SEXO BIOLÓGICO
        assertEquals(AppTab.CALCULADORA, vm.uiState.value.currentTab)
        composeRule.onNodeWithTag("patient_identification_section").assertDoesNotExist()
        composeRule.onNodeWithText("IDENTIFICACIÓN DEL PACIENTE").assertDoesNotExist()
        composeRule.onNodeWithTag("sexo_biologico_section").assertIsDisplayed()
        composeRule.onNodeWithText("SEXO BIOLÓGICO").assertIsDisplayed()

        // 1. Al cambiar a EDI, la tarjeta aparece al inicio del contenido antes de edad, ejes y panel de historial
        vm.selectTab(AppTab.EDI)
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("patient_identification_section").assertIsDisplayed()
        composeRule.onNodeWithText("IDENTIFICACIÓN DEL PACIENTE").assertIsDisplayed()
        composeRule.onNodeWithText("NOMBRES Y APELLIDOS").assertIsDisplayed()
        composeRule.onNodeWithText("Ej. Mateo Sebastián Paredes").assertIsDisplayed()
        composeRule.onNodeWithText("DNI").assertIsDisplayed()
        composeRule.onNodeWithText("8 dígitos").assertIsDisplayed()

        val idBounds = composeRule.onNodeWithTag("patient_identification_section").getUnclippedBoundsInRoot()
        val cronoAgeBounds = composeRule.onNodeWithText("Edad Cronológica").getUnclippedBoundsInRoot()
        assertTrue(
            "IDENTIFICACIÓN DEL PACIENTE debe aparecer arriba de las tarjetas de edad, ejes y panel de historial",
            idBounds.bottom <= cronoAgeBounds.top
        )

        // 3. Campos alineados horizontalmente según la imagen (~65 % y ~35 %)
        val nameBounds = composeRule.onNodeWithTag("patient_full_name_container").getUnclippedBoundsInRoot()
        val dniBounds = composeRule.onNodeWithTag("patient_dni_container").getUnclippedBoundsInRoot()
        assertTrue(nameBounds.right <= dniBounds.left)
        assertEquals(nameBounds.top.value, dniBounds.top.value, 1.5f)
        assertEquals(nameBounds.bottom.value, dniBounds.bottom.value, 1.5f)

        val nameW = nameBounds.right.value - nameBounds.left.value
        val dniW = dniBounds.right.value - dniBounds.left.value
        val ratioName = nameW / (nameW + dniW)
        val ratioDni = dniW / (nameW + dniW)
        assertTrue(ratioName in 0.60f..0.70f)
        assertTrue(ratioDni in 0.30f..0.40f)
    }

    @Test
    fun `lote 3E 4 y 5 - entrada parcial con validacion ceros iniciales autoguardado y recuperacion tras reinicio`() = runBlocking {
        val vm1 = createPersistentViewModel(autoRestoreOnInit = false)
        val uuid = vm1.uiState.value.evaluationUuid
        vm1.selectTab(AppTab.EDI)

        composeRule.setContent {
            CredEdiApp(viewModel = vm1)
        }

        // Conservar entradas parciales mientras se escribe y mostrar validación sin inventar números
        composeRule.onNodeWithTag("patient_full_name_input")
            .performTextInput("Mateo Sebastián Paredes")
        composeRule.onNodeWithTag("patient_dni_input")
            .performTextInput("0045")
        composeRule.waitForIdle()

        assertEquals("0045", vm1.uiState.value.patientIdentification.clinicalRecordCode)
        composeRule.onNodeWithTag("patient_dni_partial_validation_hint").assertIsDisplayed()

        // Completar 8 dígitos con ceros iniciales -> aviso parcial desaparece y se autoguarda en Room
        composeRule.onNodeWithTag("patient_dni_input")
            .performTextReplacement("00451234")
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("patient_dni_partial_validation_hint").assertDoesNotExist()
        assertEquals("00451234", vm1.uiState.value.patientIdentification.clinicalRecordCode)

        vm1.awaitPendingAutosave()
        assertEquals(AutosaveUiIndicatorState.SAVED, vm1.uiState.value.persistenceIndicatorState)

        // Simular reinicio del aplicativo y comprobar recuperación en los campos superiores de EDI
        val vmRestarted = createPersistentViewModel(autoRestoreOnInit = true)
        assertTrue(vmRestarted.uiState.value.wasRestoredFromPersistence)
        assertEquals(uuid, vmRestarted.uiState.value.evaluationUuid)
        assertEquals("Mateo Sebastián Paredes", vmRestarted.uiState.value.patientIdentification.patientIdentifierOrAlias)
        assertEquals("00451234", vmRestarted.uiState.value.patientIdentification.clinicalRecordCode)
    }

    @Test
    fun `lote 3E 6 7 8 y 9 - historial cambio multipaciente modo solo lectura en completadas y nueva evaluacion reflejada arriba`() = runBlocking<Unit> {
        val vm = createPersistentViewModel(autoRestoreOnInit = false)

        // Paciente 1: evaluación completada (Verde en Grupo 8)
        val uuid1 = vm.uiState.value.evaluationUuid
        vm.updatePatientFullName("Camila Valentina Rojas")
        vm.updatePatientDni("00112233")
        vm.selectSexo(SexoBiologico.NINA)
        vm.updateFechaEvaluacion("15/06/2026")
        vm.updateFechaNacimiento("15/04/2025")
        vm.confirmEdiTermWithoutWeeks()
        populateCompleteGreenGroup8(vm)
        assertEquals(EdiGlobalTrafficLight.VERDE, vm.uiState.value.ediGlobalEvaluation.trafficLight)
        assertTrue(vm.uiState.value.ediGlobalEvaluation.isDefinitive)
        assertTrue(vm.saveCurrentEvaluationManuallySuspend() is SaveEvaluationResult.Success)

        vm.selectTab(AppTab.EDI)
        composeRule.setContent {
            CredEdiApp(viewModel = vm)
        }

        // 9. Crear Nueva evaluación desde el diálogo: datos propios aparecen automáticamente en los campos superiores de EDI
        composeRule.onNodeWithTag("edi_summary_new_evaluation_btn")
            .performScrollTo()
            .performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("new_eval_full_name_input")
            .performTextInput("Santiago Nicolás Huamán")
        composeRule.onNodeWithTag("new_eval_dni_input")
            .performTextInput("76543210")
        composeRule.onNodeWithTag("new_eval_confirm_btn")
            .performClick()
        composeRule.waitForIdle()

        val uuid2 = vm.uiState.value.evaluationUuid
        assertNotEquals(uuid1, uuid2)

        // Pasar a EDI y verificar que los campos superiores ya tienen los datos del Paciente 2 sin volver a pedirlos
        vm.selectTab(AppTab.EDI)
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("patient_full_name_input")
            .performScrollTo()
            .assertIsEnabled()
            .assertTextEquals("Santiago Nicolás Huamán")
        composeRule.onNodeWithTag("patient_dni_input")
            .performScrollTo()
            .assertIsEnabled()
            .assertTextEquals("76543210")
        composeRule.onNodeWithTag("edi_summary_active_patient_label").assertDoesNotExist()
        composeRule.onNodeWithTag("edi_summary_active_patient_dni_label").assertDoesNotExist()

        // 6. Desplegar «Ver historial» y verificar que ambos pacientes muestran su identificación correcta
        composeRule.onNodeWithTag("edi_summary_toggle_history_btn")
            .performScrollTo()
            .performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("edi_summary_history_patient_label_$uuid1")
            .performScrollTo()
            .assertTextEquals("Camila Valentina Rojas")
        composeRule.onNodeWithTag("edi_summary_history_patient_dni_$uuid1")
            .performScrollTo()
            .assertTextEquals("DNI: ••••2233")

        composeRule.onNodeWithTag("edi_summary_history_patient_label_$uuid2")
            .performScrollTo()
            .assertTextEquals("Santiago Nicolás Huamán")
        composeRule.onNodeWithTag("edi_summary_history_patient_dni_$uuid2")
            .performScrollTo()
            .assertTextEquals("DNI: ••••3210")

        // 8. Abrir Paciente 1 completada con «Ver» -> modo solo lectura bloquea los campos superiores de identificación
        composeRule.onNodeWithTag("edi_summary_history_view_btn_$uuid1")
            .performScrollTo()
            .performClick()
        composeRule.waitForIdle()

        assertEquals(uuid1, vm.uiState.value.evaluationUuid)
        assertTrue(vm.uiState.value.isReadOnlyConsultationMode)

        composeRule.onNodeWithTag("patient_full_name_input")
            .performScrollTo()
            .assertIsNotEnabled()
            .assertTextEquals("Camila Valentina Rojas")
        composeRule.onNodeWithTag("patient_dni_input")
            .performScrollTo()
            .assertIsNotEnabled()
            .assertTextEquals("00112233")

        // Intentos de edición en ViewModel mientras está en modo solo lectura son rechazados
        vm.updatePatientFullName("Intento Modificar En Solo Lectura")
        vm.updatePatientDni("99999999")
        assertEquals("Camila Valentina Rojas", vm.uiState.value.patientIdentification.patientIdentifierOrAlias)
        assertEquals("00112233", vm.uiState.value.patientIdentification.clinicalRecordCode)

        // Habilitar explícitamente la edición desbloquea ambos campos superiores
        composeRule.onNodeWithTag("edi_summary_enable_editing_btn")
            .performScrollTo()
            .performClick()
        composeRule.waitForIdle()

        assertFalse(vm.uiState.value.isReadOnlyConsultationMode)
        composeRule.onNodeWithTag("patient_full_name_input")
            .performScrollTo()
            .assertIsEnabled()
        composeRule.onNodeWithTag("patient_dni_input")
            .performScrollTo()
            .assertIsEnabled()

        // 7. Volver a Paciente 2 con «Continuar» desde el historial refleja sus datos arriba sin mezcla
        composeRule.onNodeWithTag("edi_summary_history_continue_btn_$uuid2")
            .performScrollTo()
            .performClick()
        composeRule.waitForIdle()

        assertEquals(uuid2, vm.uiState.value.evaluationUuid)
        composeRule.onNodeWithTag("patient_full_name_input")
            .performScrollTo()
            .assertTextEquals("Santiago Nicolás Huamán")
        composeRule.onNodeWithTag("patient_dni_input")
            .performScrollTo()
            .assertTextEquals("76543210")
    }
}
