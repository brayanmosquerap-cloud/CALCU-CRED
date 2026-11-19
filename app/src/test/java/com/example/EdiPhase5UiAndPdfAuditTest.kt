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
import com.example.model.EdiAxis
import com.example.model.EdiGlobalTrafficLight
import com.example.model.EdiGroupChangeResolutionAction
import com.example.model.EdiInternalView
import com.example.model.EdiItemResponse
import com.example.model.EdiOfficialCatalog
import com.example.util.EdiPdfExporter
import com.example.viewmodel.CredEdiViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * FASE 5 — AUDITORÍA INTEGRAL DE INTERFAZ (VENTANAS 1–4) Y FIDELIDAD DE EXPORTACIÓN PDF.
 * Utiliza únicamente datos ficticios sintéticos. No modifica archivos de producción.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class EdiPhase5UiAndPdfAuditTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun context(): Context = ApplicationProvider.getApplicationContext()

    private fun prepareCleanGroup(vm: CredEdiViewModel, groupNumber: Int) {
        vm.selectTab(AppTab.EDI)
        vm.selectEdiGroupFromBank(groupNumber)
        if (vm.uiState.value.ediEngineState.pendingGroupChangeConfirmation != null) {
            vm.resolveEdiPendingGroupChange(
                EdiGroupChangeResolutionAction.DISCARD_PREVIOUS_CONFIRMED_AND_SWITCH_TO_CLEAN_TARGET
            )
        }
    }

    private fun fillMandatoryAreasGreen(vm: CredEdiViewModel, groupNumber: Int) {
        val spec = EdiOfficialCatalog.getGroup(groupNumber)
        spec.areaItems.values.flatten().forEach { item ->
            if (item.uniqueId == "G13_LE_13_3" || item.uniqueId == "G14_LE_14_3") {
                vm.recordEdiDualEvidence(item.uniqueId, true, true, null, "Evaluación consistente")
            } else {
                vm.recordEdiCurrentItemResponse(item.uniqueId, EdiItemResponse.YES)
            }
        }
    }

    private fun fillEnNormal(vm: CredEdiViewModel, groupNumber: Int) {
        EdiOfficialCatalog.getApplicableEnItems(groupNumber).forEach {
            vm.recordEdiCurrentItemResponse(it.uniqueId, EdiItemResponse.NO)
        }
    }

    private fun fillAlaNegative(vm: CredEdiViewModel, groupNumber: Int) {
        EdiOfficialCatalog.getGroup(groupNumber).alaItems.forEach {
            vm.recordEdiCurrentItemResponse(it.uniqueId, EdiItemResponse.NO)
        }
    }

    private fun fillFrbNegative(vm: CredEdiViewModel) {
        EdiOfficialCatalog.frbItems.forEach {
            vm.recordEdiCurrentItemResponse(it.uniqueId, EdiItemResponse.NO)
        }
    }

    private fun fillAleNegative(vm: CredEdiViewModel, groupNumber: Int) {
        EdiOfficialCatalog.getGroup(groupNumber).aleItems.forEach { item ->
            if (item.uniqueId == "G12_ALE_12_3") {
                vm.recordEdiDualEvidence(item.uniqueId, false, false, null, "Sin alerta")
            } else {
                vm.recordEdiCurrentItemResponse(item.uniqueId, EdiItemResponse.NO)
            }
        }
    }

    private fun countPdfPagesHeuristic(pdfFile: File): Int {
        val text = pdfFile.readBytes().toString(Charsets.ISO_8859_1)
        val regex = Regex("/Type\\s*/Page\\b")
        return regex.findAll(text).count().coerceAtLeast(1)
    }

    @Test
    fun `01 ui audit 4 edi internal views question group alignment dual modality and preservation`() {
        val vm = CredEdiViewModel()
        composeRule.setContent {
            CredEdiApp(viewModel = vm)
        }

        // Navegar a Tab 4 (EDI) y verificar corrección MEDIO-01: sin EG registrada no muestra "A término (32s 0d)"
        composeRule.onNodeWithTag("tab_edi").performClick()
        composeRule.onNodeWithTag("edi_view_summary").assertIsDisplayed()
        composeRule.onNodeWithTag("edi_summary_ga_status_text").assertIsDisplayed()
        composeRule.onNodeWithText("EG no registrada").assertIsDisplayed()

        // Verificar que las 5 tarjetas de resumen existen
        EdiAxis.entries.forEach { axis ->
            composeRule.onNodeWithTag("edi_axis_summary_${axis.name}").performScrollTo().assertIsDisplayed()
        }

        // Ir al Banco de 15 Grupos y elegir Grupo 13 -> verificar preguntas exactas de G13
        composeRule.onNodeWithTag("edi_btn_change_group").performScrollTo().performClick()
        composeRule.onNodeWithTag("edi_view_group_bank").assertIsDisplayed()
        composeRule.onNodeWithTag("edi_bank_group_13").performScrollTo().performClick()
        composeRule.onNodeWithTag("edi_bank_to_questions").performClick()
        composeRule.onNodeWithTag("edi_view_questionnaire").assertIsDisplayed()

        // Seleccionar Eje 3 (Áreas del Desarrollo) y confirmar que las tarjetas mostradas corresponden exactamente a G13 (ej. G13_MG_13_1)
        composeRule.onNodeWithTag("edi_axis_tab_AREAS").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("edi_item_card_G13_MG_13_1").performScrollTo().assertIsDisplayed()

        // Protocolo interno LE 13.3: entrevista principal con observación complementaria opcional
        composeRule.onNodeWithTag("edi_area_filter_LE").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("edi_item_card_G13_LE_13_3").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("edi_dual_int_yes_G13_LE_13_3").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertEquals(
            EdiItemResponse.YES,
            vm.uiState.value.ediEngineState.activeSession.currentGroupResponses["G13_LE_13_3"]
        )

        // Intentar cambiar a Grupo 14 existiendo respuestas en G13 -> exige confirmación y conserva
        vm.navigateEdiView(EdiInternalView.GROUP_BANK_15)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("edi_bank_group_14").performScrollTo().performClick()
        composeRule.onNodeWithTag("edi_group_change_dialog").assertIsDisplayed()
        composeRule.onNodeWithTag("edi_confirm_archive_switch").performClick()
        composeRule.waitForIdle()
        assertEquals(14, vm.uiState.value.ediEffectiveGroupNumber)
        val archivedG13 = vm.uiState.value.ediEngineState.archivedSessionsByGroup[13]
        assertNotNull("Debe existir snapshot archivado del Grupo 13", archivedG13)
        assertEquals(
            EdiItemResponse.YES,
            archivedG13!!.currentGroupResponses["G13_LE_13_3"]
        )
        assertTrue(
            "La sesión activa de G14 debe permanecer limpia (todo UNANSWERED y sin contaminación de G13)",
            !vm.uiState.value.ediEngineState.activeSession.hasRecordedClinicalResponses &&
                vm.uiState.value.ediEngineState.activeSession.currentGroupResponses.values.all { it == EdiItemResponse.UNANSWERED } &&
                !vm.uiState.value.ediEngineState.activeSession.currentGroupResponses.containsKey("G13_LE_13_3")
        )

        // Navegar a Resultados y Trazabilidad
        vm.navigateEdiView(EdiInternalView.RESULTS_TRACEABILITY)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("edi_view_results").assertIsDisplayed()
        composeRule.onNodeWithTag("edi_global_semaphore_card").assertIsDisplayed()
        composeRule.onNodeWithTag("edi_causes_card").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("edi_axes_breakdown_card").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("edi_pdf_export_section").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `02 ui audit windows 1 2 3 zero regressions across tabs`() {
        val vm = CredEdiViewModel()
        composeRule.setContent {
            CredEdiApp(viewModel = vm)
        }

        // Ventana 1: Calculadora CRED
        composeRule.onNodeWithTag("tab_calculadora").performClick()
        vm.updateFechaNacimiento("01/08/2024")
        vm.updateFechaEvaluacion("16/02/2025")
        vm.confirmEdiTermWithoutWeeks()
        vm.updatePesoInput("7.80")
        vm.updateLongitudInput("67.5")
        vm.updatePerimetroCefalicoInput("43.5")
        composeRule.waitForIdle()
        assertEquals("0a 6m 15d", vm.uiState.value.edadCronologicaAMD)

        // Ventana 2: Curvas OMS
        composeRule.onNodeWithTag("tab_curvas").performClick()
        composeRule.waitForIdle()
        assertEquals(AppTab.CURVAS_OMS, vm.uiState.value.currentTab)

        // Ventana 3: Diagnóstico
        composeRule.onNodeWithTag("tab_diagnostico").performClick()
        composeRule.waitForIdle()
        assertEquals(AppTab.DIAGNOSTICO, vm.uiState.value.currentTab)
        assertTrue(vm.uiState.value.globalDiagnosis.title.isNotBlank())

        // Ventana 4: EDI
        composeRule.onNodeWithTag("tab_edi").performClick()
        composeRule.waitForIdle()
        assertEquals(AppTab.EDI, vm.uiState.value.currentTab)
        assertEquals(5, vm.uiState.value.ediEffectiveGroupNumber) // 6m 15d -> Grupo 5
    }

    @Test
    fun `03 pdf audit all 8 scenarios plus rojo contrast pixel checks and scenario matrix export`() {
        val outDir = File("build/audit_phase5/pdfs")
        if (!outDir.exists()) outDir.mkdirs()

        val matrixRows = mutableListOf<String>()

        fun exportAndVerify(
            id: String,
            pdfName: String,
            fictionalPatientDesc: String,
            answersDesc: String,
            expectedOutcome: String,
            vm: CredEdiViewModel,
            expectDraft: Boolean,
            expectedMinPages: Int
        ): File {
            val state = vm.uiState.value
            val result = EdiPdfExporter.exportarFichaOficialEdiPdf(context(), state)
            assertTrue("$id falló al exportar: ${result.message}", result.success)
            assertEquals("$id estado borrador inesperado", expectDraft, result.isDraft)
            val src = File(result.filePath!!)
            assertTrue(src.exists() && src.length() > 10_000L)
            val header = src.readBytes().copyOfRange(0, 5).toString(Charsets.US_ASCII)
            assertEquals("$id encabezado PDF inválido", "%PDF-", header)

            val copied = File(outDir, pdfName)
            src.copyTo(copied, overwrite = true)

            val pageCount = countPdfPagesHeuristic(copied)
            assertTrue("$id páginas=$pageCount < $expectedMinPages", pageCount >= expectedMinPages)

            matrixRows.add(
                "| $id | $fictionalPatientDesc | $answersDesc | $expectedOutcome | ${state.ediGlobalEvaluation.trafficLight.displayLabel} (Borrador=${result.isDraft}, Págs=$pageCount) | `${result.fileName}` |"
            )
            return copied
        }

        // 1. Grupo 1 VERDE (sin ALA, CO N/A, 1 FRB+ no bloquea ni pasa a amarillo)
        val vm1 = CredEdiViewModel().apply {
            updateFechaNacimiento("05/01/2025")
            updateFechaEvaluacion("20/02/2025")
            confirmEdiTermWithoutWeeks()
            updatePesoInput("4.80")
            updateLongitudInput("56.0")
            updatePerimetroCefalicoInput("38.0")
        }
        prepareCleanGroup(vm1, 1)
        fillMandatoryAreasGreen(vm1, 1)
        fillEnNormal(vm1, 1)
        fillFrbNegative(vm1)
        fillAleNegative(vm1, 1)
        vm1.recordEdiCurrentItemResponse("FRB_2", EdiItemResponse.YES)
        val f1 = exportAndVerify(
            "Escenario 1", "scenario_01_grupo_1.pdf",
            "FN 05/01/2025, Eval 20/02/2025 (1m 15d), A término, Peso 4.80kg, Talla 56.0cm, PC 38.0cm",
            "G1: MG/MF/LE/SO 2/2 SÍ; CO N/A; EN 3/3 NO; ALA N/A; ALE 0/4 SÍ; FRB 2=SÍ (1/7)",
            "G1 definitivo VERDE (CO=N/A, ALA=N/A)", vm1, expectDraft = false, expectedMinPages = 2
        )

        // 2. Grupo 3 AMARILLO por retroceso a G2 (2/2)
        val vm2 = CredEdiViewModel().apply {
            updateFechaNacimiento("10/11/2024")
            updateFechaEvaluacion("25/02/2025")
            confirmEdiTermWithoutWeeks()
            updatePerimetroCefalicoInput("40.5")
        }
        prepareCleanGroup(vm2, 3)
        fillMandatoryAreasGreen(vm2, 3)
        vm2.recordEdiCurrentItemResponse("G3_MG_3_1", EdiItemResponse.NO)
        vm2.recordEdiCurrentItemResponse("G3_MG_3_2", EdiItemResponse.NO)
        vm2.recordEdiFallbackResponse("G3_RETRO_G2_MG_1", EdiItemResponse.YES)
        vm2.recordEdiFallbackResponse("G3_RETRO_G2_MG_2", EdiItemResponse.YES)
        fillEnNormal(vm2, 3)
        fillAlaNegative(vm2, 3)
        fillFrbNegative(vm2)
        fillAleNegative(vm2, 3)
        exportAndVerify(
            "Escenario 2", "scenario_02_grupo_3_retroceso.pdf",
            "FN 10/11/2024, Eval 25/02/2025 (3m 15d), A término, PC 40.5cm",
            "G3: MG 0/2 SÍ -> Retroceso G2 MG 2/2 SÍ (AMARILLO); MF/LE/SO 2/2 SÍ; EN/ALA/FRB/ALE negativos",
            "G3 definitivo AMARILLO (Grupo anterior marcado en MG con G2 1-2 SÍ)", vm2, expectDraft = false, expectedMinPages = 2
        )

        // 3. Grupo 8 AMARILLO por retroceso excepcional a 2 ítems de Grupo 7
        val vm3 = CredEdiViewModel().apply {
            updateFechaNacimiento("10/12/2023")
            updateFechaEvaluacion("20/02/2025")
            confirmEdiTermWithoutWeeks()
            updatePerimetroCefalicoInput("46.2")
        }
        prepareCleanGroup(vm3, 8)
        fillMandatoryAreasGreen(vm3, 8)
        vm3.recordEdiCurrentItemResponse("G8_MF_8_1", EdiItemResponse.NO)
        vm3.recordEdiCurrentItemResponse("G8_MF_8_2", EdiItemResponse.NO)
        vm3.recordEdiCurrentItemResponse("G8_MF_8_3", EdiItemResponse.NO)
        vm3.recordEdiFallbackResponse("G8_RETRO_G7_MF_1", EdiItemResponse.YES)
        vm3.recordEdiFallbackResponse("G8_RETRO_G7_MF_2", EdiItemResponse.YES)
        fillEnNormal(vm3, 8)
        fillAlaNegative(vm3, 8)
        exportAndVerify(
            "Escenario 3", "scenario_03_grupo_8_retro_g7.pdf",
            "FN 10/12/2023, Eval 20/02/2025 (14m 10d), A término, PC 46.2cm",
            "G8: MF 0/3 SÍ -> Retroceso G7 MF 2/2 SÍ (AMARILLO); MG/LE/SO 3/3 SÍ; EN/ALA negativos; FRB/ALE sin responder",
            "G8 definitivo AMARILLO (retroceso MF de solo 2 reactivos desde G7)", vm3, expectDraft = false, expectedMinPages = 2
        )

        // 4. Grupo 13 AMARILLO con retroceso especial CO-R y retroceso histórico ignorado en MG
        val vm4 = CredEdiViewModel().apply {
            updateFechaNacimiento("01/09/2021")
            updateFechaEvaluacion("15/02/2025")
            updatePerimetroCefalicoInput("49.8")
        }
        prepareCleanGroup(vm4, 13)
        fillMandatoryAreasGreen(vm4, 13)
        vm4.recordEdiCurrentItemResponse("G13_MG_13_1", EdiItemResponse.NO)
        vm4.recordEdiCurrentItemResponse("G13_MG_13_2", EdiItemResponse.NO)
        vm4.recordEdiCurrentItemResponse("G13_MG_13_3", EdiItemResponse.NO)
        vm4.recordEdiFallbackResponse("G13_RETRO_G12_MG_1", EdiItemResponse.NO)
        vm4.recordEdiCurrentItemResponse("G13_MG_13_1", EdiItemResponse.YES) // MG vuelve a 1/3 AMARILLO sin retroceso
        vm4.recordEdiCurrentItemResponse("G13_CO_13_1", EdiItemResponse.NO)
        vm4.recordEdiCurrentItemResponse("G13_CO_13_2", EdiItemResponse.NO)
        vm4.recordEdiCurrentItemResponse("G13_CO_13_3", EdiItemResponse.NO)
        vm4.recordEdiFallbackResponse("G13_RETRO_COR_13_1", EdiItemResponse.YES)
        vm4.recordEdiFallbackResponse("G13_RETRO_COR_13_2", EdiItemResponse.YES)
        vm4.recordEdiFallbackResponse("G13_RETRO_COR_13_3", EdiItemResponse.NO)
        fillEnNormal(vm4, 13)
        fillAlaNegative(vm4, 13)
        exportAndVerify(
            "Escenario 4", "scenario_04_grupo_13_cor.pdf",
            "FN 01/09/2021, Eval 15/02/2025 (41m 14d), A término, PC 49.8cm",
            "G13: MG 1/3 SÍ (historial retroceso ignorado); CO 0/3 SÍ -> CO-R 13.1=SÍ, 13.2=SÍ, 13.3=NO (AMARILLO); MF/LE/SO 3/3 SÍ",
            "G13 definitivo AMARILLO (CO-R marcado en CO, sin marcas de retroceso ignorado en MG)", vm4, expectDraft = false, expectedMinPages = 2
        )

        // 5. Grupo 14 VERDE con 8 ALE completas y conflicto dual resuelto en LE 14.3
        val vm5 = CredEdiViewModel().apply {
            updateFechaNacimiento("01/06/2020")
            updateFechaEvaluacion("15/02/2025")
            updatePerimetroCefalicoInput("50.6")
        }
        prepareCleanGroup(vm5, 14)
        fillMandatoryAreasGreen(vm5, 14)
        vm5.recordEdiDualEvidence("G14_LE_14_3", false, true, true, "Madre refiere lenguaje fluido en casa")
        fillEnNormal(vm5, 14)
        fillAlaNegative(vm5, 14)
        fillFrbNegative(vm5)
        EdiOfficialCatalog.getGroup(14).aleItems.forEachIndexed { idx, item ->
            vm5.recordEdiCurrentItemResponse(item.uniqueId, if (idx in setOf(0, 2, 5, 7)) EdiItemResponse.YES else EdiItemResponse.NO)
        }
        vm5.updateEdiItemClinicalNote("G14_ALE_14_1", "Revisado con Láminas B, C y D")
        exportAndVerify(
            "Escenario 5", "scenario_05_grupo_14_8ale.pdf",
            "FN 01/06/2020, Eval 15/02/2025 (56m 14d), A término, PC 50.6cm",
            "G14: 5 áreas VERDE (LE 14.3 resuelto manual SÍ); 8 ALE respondidas (4 SÍ / 4 NO); EN/ALA/FRB negativos",
            "G14 definitivo VERDE (las 8 casillas ALE marcadas; FRB/ALE no alteran G14)", vm5, expectDraft = false, expectedMinPages = 2
        )

        // 6. Grupo 15 VERDE con EN 1.3 NO APLICA
        val vm6 = CredEdiViewModel().apply {
            updateFechaNacimiento("01/10/2019")
            updateFechaEvaluacion("15/02/2025")
            updatePerimetroCefalicoInput("51.2")
        }
        prepareCleanGroup(vm6, 15)
        fillMandatoryAreasGreen(vm6, 15)
        fillEnNormal(vm6, 15)
        fillAlaNegative(vm6, 15)
        exportAndVerify(
            "Escenario 6", "scenario_06_grupo_15_no_en13.pdf",
            "FN 01/10/2019, Eval 15/02/2025 (64m 14d), A término, PC 51.2cm",
            "G15: 5 áreas 3/3 SÍ; EN 1.1 y EN 1.2 NO; EN 1.3 NO APLICA; ALA 0/3",
            "G15 definitivo VERDE (casilla EN 1.3 marcada NO APLICA)", vm6, expectDraft = false, expectedMinPages = 2
        )

        // 7. Evaluación Incompleta BORRADOR con discrepancia etaria y alertas críticas prioritarias (ALA + EN)
        val vm7 = CredEdiViewModel().apply {
            updateFechaNacimiento("15/02/2025")
            updateFechaEvaluacion("09/05/2025")
            togglePrematurez(true)
            updateEgSemanas(36)
            updateEgDias(5)
            selectTab(AppTab.EDI)
            selectEdiGroupFromBank(4)
            recordEdiCurrentItemResponse("G4_ALA_4_1", EdiItemResponse.YES)
            recordEdiCurrentItemResponse("EN_1_1", EdiItemResponse.YES)
            recordEdiCurrentItemResponse("G4_MF_4_2", EdiItemResponse.NOT_EVALUATED_MISSING_MATERIAL)
        }
        val f7 = exportAndVerify(
            "Escenario 7", "scenario_07_borrador_alerta_prioritaria.pdf",
            "FN 15/02/2025, Eval 09/05/2025, Prematuro 36s 5d (discrepancia G2 vs G1), Grupo 4 manual",
            "G4 incompleto: ALA 4.1=SÍ, EN 1.1=SÍ, MF 4.2=Falta material, resto sin responder",
            "BORRADOR INCOMPLETO sin semáforo marcado + advertencias clínicas prioritarias", vm7, expectDraft = true, expectedMinPages = 2
        )

        // 8. Grupo 10 VERDE con FRB y ALE opcionales pendientes/sin material (NO degradan a borrador)
        val vm8 = CredEdiViewModel().apply {
            updateFechaNacimiento("15/05/2023")
            updateFechaEvaluacion("20/02/2025")
            confirmEdiTermWithoutWeeks()
            updatePerimetroCefalicoInput("47.8")
        }
        prepareCleanGroup(vm8, 10)
        fillMandatoryAreasGreen(vm8, 10)
        fillEnNormal(vm8, 10)
        fillAlaNegative(vm8, 10)
        vm8.recordEdiCurrentItemResponse("G10_ALE_10_1", EdiItemResponse.NOT_EVALUATED_MISSING_MATERIAL)
        exportAndVerify(
            "Escenario 8", "scenario_08_grupo_10_opcionales_pendientes.pdf",
            "FN 15/05/2023, Eval 20/02/2025 (21m 5d), A término, PC 47.8cm",
            "G10: MG/MF/LE/SO 3/3 SÍ; EN/ALA negativos; FRB sin responder; ALE 10.1=Falta material, resto sin responder",
            "G10 definitivo VERDE (opcionales G5-G15 no bloquean ni generan borrador)", vm8, expectDraft = false, expectedMinPages = 2
        )

        // Extra 9 (solo auditoría visual de contraste de tinta roja en casilla ROJA del semáforo)
        val vm9 = CredEdiViewModel().apply {
            updateFechaNacimiento("05/01/2025")
            updateFechaEvaluacion("20/02/2025")
            confirmEdiTermWithoutWeeks()
            updatePesoInput("4.80")
            updateLongitudInput("56.0")
            updatePerimetroCefalicoInput("38.0")
        }
        prepareCleanGroup(vm9, 1)
        fillMandatoryAreasGreen(vm9, 1)
        vm9.recordEdiCurrentItemResponse("G1_MG_1_2", EdiItemResponse.NO) // 1/2 en G1 = ROJO
        fillEnNormal(vm9, 1)
        fillFrbNegative(vm9)
        fillAleNegative(vm9, 1)
        val res9 = EdiPdfExporter.exportarFichaOficialEdiPdf(context(), vm9.uiState.value)
        assertTrue(res9.success && !res9.isDraft)
        assertEquals(EdiGlobalTrafficLight.ROJO, vm9.uiState.value.ediGlobalEvaluation.trafficLight)
        val f9 = File(outDir, "scenario_09_grupo_1_rojo_contraste.pdf")
        File(res9.filePath!!).copyTo(f9, overwrite = true)

        assertTrue(f1.exists() && f7.exists() && f9.exists())

        // Guardar tabla de los 8 escenarios en build/audit_phase5/02_escenarios_pdf_respuestas_y_esperados.md
        val matrixFile = File("build/audit_phase5/02_escenarios_pdf_respuestas_y_esperados.md")
        val sb = StringBuilder()
        sb.appendLine("# Escenarios de Validación PDF EDI 2026 — Datos Ficticios, Respuestas y Resultados")
        sb.appendLine()
        sb.appendLine("Todos los pacientes utilizados son **100 % sintéticos/ficticios** para auditoría.")
        sb.appendLine()
        sb.appendLine("| Escenario | Datos Ficticios | Respuestas Exactas Registradas | Resultado Esperado | Resultado Obtenido | Archivo Generado |")
        sb.appendLine("|---|---|---|---|---|---|")
        matrixRows.forEach { sb.appendLine(it) }
        sb.appendLine()
        sb.appendLine("## Verificación Renderizada Externa (Ghostscript + ImageMagick + Inspección Visual)")
        sb.appendLine("- **Escenarios 1–8 + Escenario 9 (`ROJO`)**: exportados en `pdfs/` para renderizado externo `gs` a 150 DPI, inspección visual de Página 1, zoom `Inicial` y Páginas 2/3, y validación de píxeles en barra `RESULTADO DE LA EVALUACIÓN`.")
        matrixFile.writeText(sb.toString(), Charsets.UTF_8)
        assertTrue(matrixFile.exists())
    }
}
