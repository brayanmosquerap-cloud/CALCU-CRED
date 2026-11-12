package com.example.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.example.model.ClinicalAgeCalculator
import com.example.model.CredEdiUiState
import com.example.model.EdiAreaColorResult
import com.example.model.EdiDevArea
import com.example.model.EdiGlobalEvaluationResult
import com.example.model.EdiGlobalTrafficLight
import com.example.model.EdiItemResponse
import com.example.model.EdiOfficialCatalog
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ExportPdfResult(
    val success: Boolean,
    val message: String,
    val fileUri: Uri? = null,
    val filePath: String? = null,
    val viewIntent: Intent? = null,
    val isDraft: Boolean = false,
    val fileName: String? = null,
    val groupNumber: Int = 1,
    val trafficLightLabel: String = ""
)

object EdiPdfExporter {

    private const val PAGE_WIDTH = 1824
    private const val PAGE_HEIGHT = 2344

    private const val CLINICAL_BLUE_INK = 0xFF0D47A1.toInt()
    private const val CLINICAL_GREEN_INK = 0xFF1B5E20.toInt()
    private const val CLINICAL_AMBER_INK = 0xFFE65100.toInt()
    private const val CLINICAL_RED_INK = 0xFFB71C1C.toInt()
    private const val CLINICAL_SLATE_INK = 0xFF334155.toInt()
    private const val CLINICAL_DARK_INK = 0xFF0F172A.toInt()
    private const val SOFT_RED_FILL = 0xFFFFEBEE.toInt()
    private const val SOFT_AMBER_FILL = 0xFFFFF8E1.toInt()
    private const val SOFT_GREEN_FILL = 0xFFE8F5E9.toInt()
    private const val SOFT_BLUE_FILL = 0xFFEAF2FF.toInt()
    private const val SOFT_GRAY_FILL = 0xFFF1F5F9.toInt()
    private const val BORDER_GRAY = 0xFFCBD5E1.toInt()

    private data class AreaRowGeometry(
        val area: EdiDevArea,
        val itemYs: List<Float>,
        val resultY: Float
    )

    // Coordenadas exactas verificadas píxel a píxel sobre app/src/main/assets/ficha_edi.jpg (1824 x 2344)
    private val AREA_ROWS = listOf(
        AreaRowGeometry(EdiDevArea.MG, listOf(920.5f, 948.5f, 976.5f), 1003.5f),
        AreaRowGeometry(EdiDevArea.MF, listOf(1031.0f, 1059.0f, 1086.5f), 1114.0f),
        AreaRowGeometry(EdiDevArea.LE, listOf(1141.5f, 1168.5f, 1196.0f), 1223.5f),
        AreaRowGeometry(EdiDevArea.SO, listOf(1251.0f, 1279.0f, 1307.0f), 1334.5f),
        AreaRowGeometry(EdiDevArea.CO, listOf(1362.0f, 1390.0f, 1418.0f), 1445.5f)
    )

    // Áreas del Desarrollo (Inicial): Amarillo=NO [348..378], Verde=SÍ [378..437]
    private const val AREA_CURRENT_NO_YELLOW_X = 363.0f
    private const val AREA_CURRENT_YES_GREEN_X = 407.5f

    // Áreas del Desarrollo (Grupo anterior): Etiqueta [437..475], Rojo=NO [475..505], Amarillo=SÍ [505..535]
    private const val AREA_FB_CODE_X = 456.0f
    private const val AREA_FB_NO_RED_X = 490.0f
    private const val AREA_FB_YES_YELLOW_X = 520.0f
    private const val AREA_RESULT_X = 486.0f

    // Señales de Alarma (ALA): Rojo=SÍ [348..378], Verde=NO [378..437], Resultado [475..535]
    private val ALA_ROW_YS = listOf(1536.5f, 1563.5f, 1591.0f, 1619.0f)
    private const val ALA_YES_RED_X = 363.0f
    private const val ALA_NO_GREEN_X = 407.5f
    private const val ALA_RESULT_X = 505.0f
    private const val ALA_RESULT_Y = 1578.0f

    // Exploración Neurológica (EN): Rojo=SÍ(alterada) [347..378], Verde=NO(normal) [378..437], PC [437..475], Resultado [505..535]
    private val EN_ROW_YS = listOf(1714.5f, 1751.0f, 1791.5f)
    private const val EN_YES_RED_X = 362.5f
    private const val EN_NO_GREEN_X = 407.5f
    private const val EN_PC_CENTER_X = 456.0f
    private const val EN_PC_CENTER_Y = 1756.0f
    private const val EN_RESULT_X = 520.0f
    private const val EN_RESULT_Y = 1755.0f

    // Resultado de la Evaluación (Semáforo barra horizontal y=1852..1880): Verde [301..378], Amarillo [378..475], Rojo [475..535]
    private const val SEM_GREEN_X = 339.5f
    private const val SEM_YELLOW_X = 426.5f
    private const val SEM_RED_X = 505.0f
    private const val SEM_ROW_Y = 1866.0f

    // FRB (Inicial, 7 filas): Amarillo=SÍ [151..228], Verde=NO [228..300], Resultado [229..348]
    private val FRB_ROW_YS = listOf(2050.0f, 2077.5f, 2105.5f, 2133.0f, 2160.0f, 2186.5f, 2219.5f)
    private const val FRB_YES_YELLOW_X = 189.5f
    private const val FRB_NO_GREEN_X = 264.0f
    private const val FRB_RESULT_X = 288.5f
    private const val FRB_RESULT_Y = 2268.0f

    // Señales de Alerta (ALE Inicial, 8 filas): Amarillo=SÍ [378..438], Verde=NO [438..475], Resultado [475..535]
    private val ALE_ROW_YS = listOf(2050.0f, 2077.5f, 2105.5f, 2132.5f, 2159.5f, 2186.5f, 2220.0f, 2263.0f)
    private const val ALE_YES_YELLOW_X = 408.0f
    private const val ALE_NO_GREEN_X = 456.5f
    private const val ALE_RESULT_X = 505.0f

    fun exportarFichaOficialEdiPdf(
        context: Context,
        state: CredEdiUiState
    ): ExportPdfResult {
        val pages = mutableListOf<Bitmap>()
        return try {
            val baseBitmap = loadTemplateBitmap(context)
                ?: return ExportPdfResult(
                    success = false,
                    message = "No se pudo cargar la plantilla oficial de la Ficha EDI."
                )

            val groupSpec = state.ediActiveGroupSpec
            val globalEval = state.ediGlobalEvaluation
            val isDraft = !globalEval.isDefinitive || globalEval.trafficLight == EdiGlobalTrafficLight.INCOMPLETE

            // Página 1: Plantilla oficial MINSA/EDI 2026 con marcación exacta desde la sesión activa
            val page1Bitmap = if (baseBitmap.width == PAGE_WIDTH && baseBitmap.height == PAGE_HEIGHT) {
                baseBitmap.copy(Bitmap.Config.ARGB_8888, true)
            } else {
                Bitmap.createScaledBitmap(baseBitmap, PAGE_WIDTH, PAGE_HEIGHT, true)
                    .copy(Bitmap.Config.ARGB_8888, true)
            }
            if (baseBitmap !== page1Bitmap) {
                baseBitmap.recycle()
            }
            drawOfficialTemplatePage(
                canvas = Canvas(page1Bitmap),
                state = state,
                globalEval = globalEval,
                isDraft = isDraft
            )
            pages.add(page1Bitmap)

            // Páginas 2+: Anexo clínico / Borrador con trazabilidad completa, alertas prioritarias y retrocesos
            val addendumPages = renderTraceabilityAddendumPages(
                state = state,
                globalEval = globalEval,
                isDraft = isDraft
            )
            pages.addAll(addendumPages)

            val parsedEval = ClinicalAgeCalculator.parseDate(state.fechaEvaluacion)
            val dateSlug = parsedEval?.let {
                String.format(Locale.US, "%02d_%02d_%04d", it.day, it.month, it.year)
            } ?: "sin_fecha"

            val basePrefix = if (isDraft) {
                "Borrador_EDI_Incompleta_${groupSpec.code}_$dateSlug"
            } else {
                "Ficha_Oficial_EDI_${groupSpec.code}_$dateSlug"
            }

            val downloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                ?: context.filesDir
            val uniqueFile = resolveUniquePdfFile(downloadsDir, basePrefix)

            saveMultiPagePdfDocument(
                context = context,
                pages = pages,
                localFile = uniqueFile,
                isDraft = isDraft,
                groupNumber = groupSpec.groupNumber,
                trafficLightLabel = globalEval.trafficLight.displayLabel
            )
        } catch (e: Exception) {
            ExportPdfResult(
                success = false,
                message = "Error al exportar PDF EDI: ${e.localizedMessage ?: "error desconocido"}"
            )
        } finally {
            pages.forEach { bm ->
                if (!bm.isRecycled) {
                    bm.recycle()
                }
            }
        }
    }

    private fun drawOfficialTemplatePage(
        canvas: Canvas,
        state: CredEdiUiState,
        globalEval: EdiGlobalEvaluationResult,
        isDraft: Boolean
    ) {
        val groupSpec = state.ediActiveGroupSpec

        val inkTextPaint = Paint().apply {
            color = CLINICAL_BLUE_INK
            textSize = 23f
            isAntiAlias = true
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val compactInkPaint = Paint(inkTextPaint).apply {
            textSize = 18f
        }
        val tinyInkPaint = Paint(inkTextPaint).apply {
            textSize = 13.5f
        }
        val markPaint = Paint().apply {
            color = CLINICAL_BLUE_INK
            strokeWidth = 4.2f
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            isAntiAlias = true
        }
        val redMarkPaint = Paint(markPaint).apply {
            color = CLINICAL_RED_INK
        }

        // 0) Banda superior limpia (y=8..58, totalmente por encima del título y=67..196)
        drawTopStatusRibbon(
            canvas = canvas,
            state = state,
            globalEval = globalEval,
            isDraft = isDraft
        )

        // A) Fecha de Nacimiento (dd, mm, aa) — Nunca inventar si falta
        val parsedNac = ClinicalAgeCalculator.parseDate(state.fechaNacimiento)
        if (parsedNac != null) {
            canvas.drawText(String.format(Locale.US, "%02d", parsedNac.day), 125f, 446f, inkTextPaint)
            canvas.drawText(String.format(Locale.US, "%02d", parsedNac.month), 188.5f, 446f, inkTextPaint)
            canvas.drawText((parsedNac.year % 100).toString().padStart(2, '0'), 264f, 446f, inkTextPaint)
        } else {
            canvas.drawText("--", 125f, 446f, compactInkPaint)
            canvas.drawText("--", 188.5f, 446f, compactInkPaint)
            canvas.drawText("--", 264f, 446f, compactInkPaint)
        }

        // B) Edad Gestacional en Semanas y Días (sin redondeos ni valores engañosos en niños a término)
        val gaSheetLabel = when {
            state.esPrematuro -> state.ediGestationalAgeInput.originalLabel
            state.egSemanas >= 37 -> state.ediGestationalAgeInput.originalLabel
            else -> "≥37 sem"
        }
        canvas.drawText(gaSheetLabel, 200f, 558f, inkTextPaint)
        canvas.drawText(
            if (state.esPrematuro) "(Prematuro)" else "(A término)",
            200f,
            590f,
            tinyInkPaint
        )

        // C) Fecha de Evaluación Inicial (alineada entre las barras preimpresas "/")
        val parsedEval = ClinicalAgeCalculator.parseDate(state.fechaEvaluacion)
        if (parsedEval != null) {
            canvas.drawText(String.format(Locale.US, "%02d", parsedEval.day), 385f, 446f, compactInkPaint)
            canvas.drawText(String.format(Locale.US, "%02d", parsedEval.month), 448f, 446f, compactInkPaint)
            canvas.drawText(String.format(Locale.US, "%04d", parsedEval.year), 504f, 446f, compactInkPaint)
        }

        // D) Edad Cronológica (Inicial)
        if (state.ediHasFechaNacimientoValida) {
            canvas.drawText(state.edadCronologicaAnios.toString(), 418f, 497f, inkTextPaint)
            canvas.drawText(
                "${state.edadCronologicaMesesRestantes}m ${state.edadCronologicaDias}d",
                408f,
                543f,
                compactInkPaint
            )
        } else {
            canvas.drawText("-", 418f, 497f, compactInkPaint)
            canvas.drawText("Sin F.N.", 408f, 543f, compactInkPaint)
        }

        // E) Edad Corregida (Inicial)
        if (state.ediHasFechaNacimientoValida && state.usaEdadCorregida) {
            canvas.drawText(state.edadCorregidaAnios.toString(), 418f, 596f, inkTextPaint)
            canvas.drawText(
                "${state.edadCorregidaMesesRestantes}m ${state.edadCorregidaDias}d",
                408f,
                646f,
                compactInkPaint
            )
        } else {
            canvas.drawText("-", 418f, 596f, compactInkPaint)
            canvas.drawText("No aplica", 408f, 646f, compactInkPaint)
        }

        // F) Grupo de Edad Activo (caja blanca derecha x=437..535)
        canvas.drawText(groupSpec.code, 486f, 695f, inkTextPaint)

        // G) SECCIÓN 1: Áreas del Desarrollo (MG, MF, LE, SO, CO) + Retroceso en Grupo anterior
        val areaResults = globalEval.axis3AreasSummary.areaResults
        AREA_ROWS.forEach { row ->
            val areaEval = areaResults[row.area]
            if (areaEval == null || !areaEval.appliesToGroup) {
                // En Grupos 1–12 CO no aplica: no marcar casillas SÍ/NO; registrar N/A en Resultado
                val naPaint = Paint(compactInkPaint).apply { color = CLINICAL_SLATE_INK }
                canvas.drawText("N/A", AREA_RESULT_X, row.resultY + 6f, naPaint)
            } else {
                // Marcar únicamente los reactivos reales del grupo (2 en G1–G7, 3 en G8–G15)
                areaEval.currentItems.take(3).forEachIndexed { idx, item ->
                    val resp = areaEval.currentItemResponses[item.uniqueId] ?: EdiItemResponse.UNANSWERED
                    val y = row.itemYs[idx]
                    when (resp) {
                        EdiItemResponse.YES -> drawX(canvas, AREA_CURRENT_YES_GREEN_X, y, 7.5f, markPaint)
                        EdiItemResponse.NO -> drawX(canvas, AREA_CURRENT_NO_YELLOW_X, y, 7.5f, markPaint)
                        EdiItemResponse.UNANSWERED,
                        EdiItemResponse.NOT_EVALUATED_MISSING_MATERIAL,
                        EdiItemResponse.DISCREPANCY_REQUIRES_HUMAN_RESOLUTION -> Unit
                    }
                }

                // Retroceso en columna "Grupo anterior" SOLO cuando está activo (no marcar historial ignorado)
                if (areaEval.fallbackRequired) {
                    areaEval.fallbackItems.take(3).forEachIndexed { fbIdx, fbSpec ->
                        val fbResp = areaEval.activeFallbackResponses[fbSpec.fallbackUniqueId]
                            ?: EdiItemResponse.UNANSWERED
                        val y = row.itemYs[fbIdx]
                        if (fbSpec.isSpecialCoRItem) {
                            canvas.drawText("CO-R", AREA_FB_CODE_X, y + 4.5f, Paint(tinyInkPaint).apply {
                                textSize = 11f
                            })
                        } else if (fbIdx == 0 && areaEval.fallbackSourceGroupNumber != null) {
                            canvas.drawText("G${areaEval.fallbackSourceGroupNumber}", AREA_FB_CODE_X, y + 4.5f, Paint(tinyInkPaint).apply {
                                textSize = 12f
                            })
                        }
                        when (fbResp) {
                            EdiItemResponse.YES -> drawX(canvas, AREA_FB_YES_YELLOW_X, y, 7.5f, markPaint)
                            EdiItemResponse.NO -> drawX(canvas, AREA_FB_NO_RED_X, y, 7.5f, redMarkPaint)
                            EdiItemResponse.UNANSWERED,
                            EdiItemResponse.NOT_EVALUATED_MISSING_MATERIAL,
                            EdiItemResponse.DISCREPANCY_REQUIRES_HUMAN_RESOLUTION -> Unit
                        }
                    }
                }

                // Resultado del Área (solo color definitivo si status.isDefinitiveColor; nunca inventar)
                val areaResPaint = Paint().apply {
                    textSize = 15.5f
                    isAntiAlias = true
                    typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                    textAlign = Paint.Align.CENTER
                    color = when (areaEval.status) {
                        EdiAreaColorResult.VERDE -> CLINICAL_GREEN_INK
                        EdiAreaColorResult.AMARILLO -> CLINICAL_AMBER_INK
                        EdiAreaColorResult.ROJO -> CLINICAL_RED_INK
                        EdiAreaColorResult.INCOMPLETE -> CLINICAL_SLATE_INK
                        EdiAreaColorResult.NOT_APPLICABLE -> CLINICAL_SLATE_INK
                    }
                }
                val areaResLabel = when (areaEval.status) {
                    EdiAreaColorResult.VERDE -> "VERDE"
                    EdiAreaColorResult.AMARILLO -> "AMARILLO"
                    EdiAreaColorResult.ROJO -> "ROJO"
                    EdiAreaColorResult.INCOMPLETE -> "PEND."
                    EdiAreaColorResult.NOT_APPLICABLE -> "N/A"
                }
                canvas.drawText(areaResLabel, AREA_RESULT_X, row.resultY + 5.5f, areaResPaint)
            }
        }

        // H) SECCIÓN 2: Señales de Alarma (ALA) — Exclusivamente groupSpec.alaItems; NO APLICA en Grupo 1
        if (!globalEval.alaAppliesToGroup || groupSpec.alaItems.isEmpty()) {
            val naPaint = Paint(compactInkPaint).apply { color = CLINICAL_SLATE_INK }
            canvas.drawText("ALA: NO APLICA (G1)", 201f, ALA_RESULT_Y + 6f, naPaint)
            canvas.drawText("N/A", ALA_RESULT_X, ALA_RESULT_Y + 6f, naPaint)
        } else {
            groupSpec.alaItems.take(4).forEachIndexed { idx, item ->
                val resp = globalEval.alaResponsesResolved[item.uniqueId] ?: EdiItemResponse.UNANSWERED
                val y = ALA_ROW_YS[idx]
                when (resp) {
                    EdiItemResponse.YES -> drawX(canvas, ALA_YES_RED_X, y, 7.5f, redMarkPaint)
                    EdiItemResponse.NO -> drawX(canvas, ALA_NO_GREEN_X, y, 7.5f, markPaint)
                    EdiItemResponse.UNANSWERED,
                    EdiItemResponse.NOT_EVALUATED_MISSING_MATERIAL,
                    EdiItemResponse.DISCREPANCY_REQUIRES_HUMAN_RESOLUTION -> Unit
                }
            }
            val alaComplete = groupSpec.alaItems.all {
                (globalEval.alaResponsesResolved[it.uniqueId] ?: EdiItemResponse.UNANSWERED).isAnsweredDefinitively
            }
            val (alaLabel, alaColor) = when {
                globalEval.positiveAlaItems.isNotEmpty() ->
                    "SÍ (${globalEval.positiveAlaItems.size})" to CLINICAL_RED_INK
                alaComplete ->
                    "NORM" to CLINICAL_GREEN_INK
                else ->
                    "PEND." to CLINICAL_SLATE_INK
            }
            val alaPaint = Paint(compactInkPaint).apply {
                textSize = 15.5f
                color = alaColor
            }
            canvas.drawText(alaLabel, ALA_RESULT_X, ALA_RESULT_Y + 6f, alaPaint)
        }

        // I) SECCIÓN 3: Exploración Neurológica (EN 1.1..1.3) — En G15 EN 1.3 NO APLICA
        val enItems = EdiOfficialCatalog.getApplicableEnItems(groupSpec.groupNumber)
        enItems.forEachIndexed { idx, item ->
            if (idx < EN_ROW_YS.size) {
                val resp = globalEval.enResponsesResolved[item.uniqueId] ?: EdiItemResponse.UNANSWERED
                val y = EN_ROW_YS[idx]
                when (resp) {
                    EdiItemResponse.YES -> drawX(canvas, EN_YES_RED_X, y, 7.5f, redMarkPaint)
                    EdiItemResponse.NO -> drawX(canvas, EN_NO_GREEN_X, y, 7.5f, markPaint)
                    EdiItemResponse.UNANSWERED,
                    EdiItemResponse.NOT_EVALUATED_MISSING_MATERIAL,
                    EdiItemResponse.DISCREPANCY_REQUIRES_HUMAN_RESOLUTION -> Unit
                }
            }
        }
        if (!globalEval.en13AppliesToGroup) {
            val clearBg = Paint().apply {
                color = 0xFFF8FAFC.toInt()
                style = Paint.Style.FILL
            }
            canvas.drawRect(350f, 1775f, 434f, 1808f, clearBg)
            val naEnPaint = Paint(tinyInkPaint).apply {
                color = CLINICAL_SLATE_INK
                textSize = 13f
            }
            canvas.drawText("NO APLICA", 392f, 1796f, naEnPaint)
        }

        // Perímetro cefálico (solo si fue ingresado por el usuario)
        if (state.hasPerimetroCefalicoIngresado) {
            val pcText = String.format(Locale.US, "%.1f", state.perimetroCefalicoCm)
            drawVerticalText(canvas, pcText, EN_PC_CENTER_X, EN_PC_CENTER_Y, Paint(compactInkPaint).apply {
                textSize = 16.5f
            })
        }

        val enComplete = enItems.all {
            (globalEval.enResponsesResolved[it.uniqueId] ?: EdiItemResponse.UNANSWERED).isAnsweredDefinitively
        }
        val (enResText, enResColor) = when {
            globalEval.positiveEnItems.isNotEmpty() ->
                "ALT (${globalEval.positiveEnItems.size})" to CLINICAL_RED_INK
            enComplete ->
                "NORMAL" to CLINICAL_GREEN_INK
            else ->
                "PEND." to CLINICAL_SLATE_INK
        }
        drawVerticalText(
            canvas,
            enResText,
            EN_RESULT_X,
            EN_RESULT_Y,
            Paint(compactInkPaint).apply {
                textSize = 15.5f
                color = enResColor
            }
        )

        // J) RESULTADO DE LA EVALUACIÓN (Semáforo Global)
        // REGLA CRÍTICA: Jamás marcar un color definitivo si la evaluación es INCOMPLETA
        if (!isDraft && globalEval.isDefinitive) {
            val targetX = when (globalEval.trafficLight) {
                EdiGlobalTrafficLight.VERDE -> SEM_GREEN_X
                EdiGlobalTrafficLight.AMARILLO -> SEM_YELLOW_X
                EdiGlobalTrafficLight.ROJO -> SEM_RED_X
                EdiGlobalTrafficLight.INCOMPLETE -> null
            }
            if (targetX != null) {
                val semPaint = if (globalEval.trafficLight == EdiGlobalTrafficLight.ROJO) redMarkPaint else markPaint
                drawX(canvas, targetX, SEM_ROW_Y, 10.5f, semPaint)
                drawRing(canvas, targetX, SEM_ROW_Y, 16.5f, semPaint)
            }
        } else {
            val draftLabelPaint = Paint(tinyInkPaint).apply {
                color = CLINICAL_RED_INK
                textSize = 14.5f
            }
            canvas.drawText("BORRADOR (INCOMPLETO)", 200f, 1872f, draftLabelPaint)
        }

        // K) SECCIÓN 4: Factores de Riesgo Biológico (FRB 1..7)
        EdiOfficialCatalog.frbItems.take(7).forEachIndexed { idx, item ->
            val resp = globalEval.frbResponsesResolved[item.uniqueId] ?: EdiItemResponse.UNANSWERED
            val y = FRB_ROW_YS[idx]
            when (resp) {
                EdiItemResponse.YES -> drawX(canvas, FRB_YES_YELLOW_X, y, 7.5f, markPaint)
                EdiItemResponse.NO -> drawX(canvas, FRB_NO_GREEN_X, y, 7.5f, markPaint)
                EdiItemResponse.UNANSWERED,
                EdiItemResponse.NOT_EVALUATED_MISSING_MATERIAL,
                EdiItemResponse.DISCREPANCY_REQUIRES_HUMAN_RESOLUTION -> Unit
            }
        }
        val frbAnsweredCount = globalEval.frbResponsesResolved.values.count { it.isAnsweredDefinitively }
        val frbPositiveCount = globalEval.positiveFrbItems.size
        val frbResLabel = when {
            frbAnsweredCount == 7 -> "$frbPositiveCount/7 pos"
            !globalEval.frbMandatoryForGroup -> "$frbPositiveCount pos ($frbAnsweredCount/7)"
            else -> "Pend ($frbAnsweredCount/7)"
        }
        canvas.drawText(
            frbResLabel,
            FRB_RESULT_X,
            FRB_RESULT_Y,
            Paint(compactInkPaint).apply { textSize = 16.5f }
        )

        // L) SECCIÓN 5: Señales de Alerta (ALE 1..N, hasta 8 filas — ej. 8 ALE en Grupo 14)
        groupSpec.aleItems.take(8).forEachIndexed { idx, item ->
            val resp = globalEval.aleResponsesResolved[item.uniqueId] ?: EdiItemResponse.UNANSWERED
            val y = ALE_ROW_YS[idx]
            when (resp) {
                EdiItemResponse.YES -> drawX(canvas, ALE_YES_YELLOW_X, y, 7.5f, markPaint)
                EdiItemResponse.NO -> drawX(canvas, ALE_NO_GREEN_X, y, 7.5f, markPaint)
                EdiItemResponse.UNANSWERED,
                EdiItemResponse.NOT_EVALUATED_MISSING_MATERIAL,
                EdiItemResponse.DISCREPANCY_REQUIRES_HUMAN_RESOLUTION -> Unit
            }
        }
        val aleTotal = groupSpec.aleItems.size
        val aleAnsweredCount = globalEval.aleResponsesResolved.values.count { it.isAnsweredDefinitively }
        val alePositiveCount = globalEval.positiveAleItems.size
        canvas.drawText(
            "$alePositiveCount/$aleTotal pos",
            ALE_RESULT_X,
            2102f,
            Paint(compactInkPaint).apply { textSize = 13.5f }
        )
        val aleSubLabel = when {
            aleAnsweredCount == aleTotal -> "Eval."
            !globalEval.aleMandatoryForGroup -> "Opc($aleAnsweredCount/$aleTotal)"
            else -> "Pend($aleAnsweredCount/$aleTotal)"
        }
        canvas.drawText(
            aleSubLabel,
            ALE_RESULT_X,
            2124f,
            Paint(tinyInkPaint).apply {
                textSize = 11.5f
                color = CLINICAL_SLATE_INK
            }
        )

        // M) Pie limpio en el margen inferior (y=2296..2342, debajo del borde y=2286)
        drawBottomSummaryMargin(canvas, state, globalEval, isDraft)
    }

    private fun drawTopStatusRibbon(
        canvas: Canvas,
        state: CredEdiUiState,
        globalEval: EdiGlobalEvaluationResult,
        isDraft: Boolean
    ) {
        val rect = RectF(100f, 8f, 1724f, 56f)
        val (bgColor, borderColor, textColor) = when {
            isDraft && globalEval.hasPriorityCriticalWarning ->
                Triple(SOFT_RED_FILL, CLINICAL_RED_INK, CLINICAL_RED_INK)
            isDraft ->
                Triple(SOFT_AMBER_FILL, CLINICAL_AMBER_INK, CLINICAL_AMBER_INK)
            globalEval.trafficLight == EdiGlobalTrafficLight.ROJO ->
                Triple(SOFT_RED_FILL, CLINICAL_RED_INK, CLINICAL_RED_INK)
            globalEval.trafficLight == EdiGlobalTrafficLight.AMARILLO ->
                Triple(SOFT_AMBER_FILL, CLINICAL_AMBER_INK, CLINICAL_AMBER_INK)
            else ->
                Triple(SOFT_GREEN_FILL, CLINICAL_GREEN_INK, CLINICAL_GREEN_INK)
        }

        val fillPaint = Paint().apply {
            color = bgColor
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        val strokePaint = Paint().apply {
            color = borderColor
            style = Paint.Style.STROKE
            strokeWidth = 3f
            isAntiAlias = true
        }
        canvas.drawRoundRect(rect, 10f, 10f, fillPaint)
        canvas.drawRoundRect(rect, 10f, 10f, strokePaint)

        val label = if (isDraft) {
            val prioritySuffix = if (globalEval.hasPriorityCriticalWarning) {
                " · ⚠ ADVERTENCIA CLÍNICA PRIORITARIA (${globalEval.priorityCriticalAlerts.size}) — VER ANEXO"
            } else {
                " · VER PENDIENTES EN ANEXO PÁG. 2"
            }
            "BORRADOR — EVALUACIÓN INCOMPLETA (NO DEFINITIVA) · ${state.ediActiveGroupSpec.code}$prioritySuffix"
        } else {
            "FICHA OFICIAL EDI PERUANO 2026 · ${state.ediActiveGroupSpec.code} (${state.ediActiveGroupSpec.shortIntervalLabel}) · DICTAMEN: ${globalEval.trafficLight.displayLabel.uppercase()}"
        }

        val ribbonPaint = Paint().apply {
            color = textColor
            textSize = 20f
            isAntiAlias = true
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(label, 912f, 39f, ribbonPaint)
    }

    private fun drawBottomSummaryMargin(
        canvas: Canvas,
        state: CredEdiUiState,
        globalEval: EdiGlobalEvaluationResult,
        isDraft: Boolean
    ) {
        val footerPaint = Paint().apply {
            color = if (isDraft) CLINICAL_RED_INK else CLINICAL_BLUE_INK
            textSize = 18.5f
            isAntiAlias = true
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.LEFT
        }
        val statusText = if (isDraft) {
            "BORRADOR INCOMPLETO · Faltan ${globalEval.pendingMandatoryItemCodes.size + globalEval.missingMaterialItemCodes.size + globalEval.discrepancyItemCodes.size} ítem(s)/control(es)"
        } else {
            "DICTAMEN EDI 2026: ${globalEval.trafficLight.displayLabel}"
        }
        val credSummary = if (state.datosAntropometricosCount > 0) {
            String.format(
                Locale.US,
                "CRED NTS 238 · Sexo: %s | Peso: %.2f kg (%s) | Talla: %.1f cm (%s) | PC: %.1f cm",
                state.sexo.label,
                state.pesoKg,
                state.evalPE.shortClassification,
                state.longitudCm,
                state.evalTE.shortClassification,
                state.perimetroCefalicoCm
            )
        } else {
            val gaFooter = if (state.esPrematuro || state.egSemanas >= 37) {
                state.ediGestationalAgeInput.originalLabel
            } else {
                "A término (≥37 sem)"
            }
            "CRED NTS 238 · Sexo: ${state.sexo.label} | EG: $gaFooter | Ver Anexo de Trazabilidad en Página 2"
        }
        canvas.drawText(
            "$statusText · ${state.ediActiveGroupSpec.code} (${state.ediActiveGroupSpec.officialTitle})",
            100f,
            2313f,
            footerPaint
        )
        canvas.drawText(
            credSummary,
            100f,
            2336f,
            Paint(footerPaint).apply {
                color = CLINICAL_SLATE_INK
                textSize = 17f
            }
        )
    }

    private fun renderTraceabilityAddendumPages(
        state: CredEdiUiState,
        globalEval: EdiGlobalEvaluationResult,
        isDraft: Boolean
    ): List<Bitmap> {
        val pages = mutableListOf<Bitmap>()
        var currentBitmap = Bitmap.createBitmap(PAGE_WIDTH, PAGE_HEIGHT, Bitmap.Config.ARGB_8888)
        var canvas = Canvas(currentBitmap)
        canvas.drawColor(0xFFFFFFFF.toInt())

        val margin = 72f
        val contentWidth = PAGE_WIDTH - margin * 2
        val maxY = PAGE_HEIGHT - 84f
        var cursorY = 76f

        fun startNewPage(subtitle: String) {
            drawPageFooter(canvas, pages.size + 2, isDraft)
            pages.add(currentBitmap)
            currentBitmap = Bitmap.createBitmap(PAGE_WIDTH, PAGE_HEIGHT, Bitmap.Config.ARGB_8888)
            canvas = Canvas(currentBitmap)
            canvas.drawColor(0xFFFFFFFF.toInt())
            cursorY = 76f
            val contPaint = Paint().apply {
                color = CLINICAL_SLATE_INK
                textSize = 20f
                isAntiAlias = true
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            }
            canvas.drawText(subtitle, margin, cursorY, contPaint)
            cursorY += 38f
        }

        val titlePaint = Paint().apply {
            color = CLINICAL_DARK_INK
            textSize = 30f
            isAntiAlias = true
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val sectionTitlePaint = Paint().apply {
            color = CLINICAL_BLUE_INK
            textSize = 24f
            isAntiAlias = true
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val bodyPaint = Paint().apply {
            color = CLINICAL_DARK_INK
            textSize = 20f
            isAntiAlias = true
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        val boldBodyPaint = Paint(bodyPaint).apply {
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }

        fun ensureSpace(neededHeight: Float, headerOnBreak: String) {
            if (cursorY + neededHeight > maxY) {
                startNewPage(headerOnBreak)
            }
        }

        fun drawCard(
            title: String,
            lines: List<Pair<String, Int>>,
            bgColor: Int = SOFT_GRAY_FILL,
            borderColor: Int = BORDER_GRAY,
            titleColor: Int = CLINICAL_BLUE_INK
        ) {
            val wrapped = mutableListOf<Pair<String, Int>>()
            lines.forEach { (txt, col) ->
                wrapText(txt, bodyPaint, contentWidth - 48f).forEach { wLine ->
                    wrapped.add(wLine to col)
                }
            }
            val lineHeight = 29f
            val cardHeight = 64f + (wrapped.size * lineHeight) + 18f
            if (cursorY + cardHeight > maxY && cursorY > 140f) {
                startNewPage("ANEXO DE TRAZABILIDAD CLÍNICA EDI 2026 (CONTINUACIÓN)")
            }
            val rect = RectF(margin, cursorY, PAGE_WIDTH - margin, cursorY + cardHeight)
            canvas.drawRoundRect(rect, 18f, 18f, Paint().apply {
                color = bgColor
                style = Paint.Style.FILL
                isAntiAlias = true
            })
            canvas.drawRoundRect(rect, 18f, 18f, Paint().apply {
                color = borderColor
                style = Paint.Style.STROKE
                strokeWidth = 2.5f
                isAntiAlias = true
            })
            canvas.drawText(
                title,
                margin + 24f,
                cursorY + 38f,
                Paint(sectionTitlePaint).apply { color = titleColor }
            )
            var lineY = cursorY + 74f
            wrapped.forEach { (line, color) ->
                if (lineY > maxY - 24f) {
                    startNewPage("$title (cont.)")
                    lineY = cursorY + 12f
                }
                canvas.drawText(
                    line,
                    margin + 24f,
                    lineY,
                    Paint(bodyPaint).apply { this.color = color }
                )
                lineY += lineHeight
            }
            cursorY += cardHeight + 22f
        }

        // 1) Encabezado principal del Anexo
        val groupSpec = state.ediActiveGroupSpec
        val (hdrBg, hdrBorder, hdrTitleColor) = when {
            isDraft -> Triple(SOFT_AMBER_FILL, CLINICAL_AMBER_INK, CLINICAL_RED_INK)
            globalEval.trafficLight == EdiGlobalTrafficLight.ROJO ->
                Triple(SOFT_RED_FILL, CLINICAL_RED_INK, CLINICAL_RED_INK)
            globalEval.trafficLight == EdiGlobalTrafficLight.AMARILLO ->
                Triple(SOFT_AMBER_FILL, CLINICAL_AMBER_INK, CLINICAL_AMBER_INK)
            else ->
                Triple(SOFT_GREEN_FILL, CLINICAL_GREEN_INK, CLINICAL_GREEN_INK)
        }
        val headerTitle = if (isDraft) {
            "BORRADOR — EVALUACIÓN INCOMPLETA (NO VÁLIDO COMO DICTAMEN DEFINITIVO)"
        } else {
            "ANEXO DE TRAZABILIDAD CLÍNICA · FICHA OFICIAL EDI PERUANO 2026"
        }
        drawCard(
            title = headerTitle,
            lines = listOf(
                "Grupo Activo: ${groupSpec.code} — ${groupSpec.officialTitle} (${groupSpec.shortIntervalLabel})" to CLINICAL_DARK_INK,
                "Resultado del Motor Global (EdiGlobalSemaphoreEngine): ${globalEval.trafficLight.displayLabel}" to hdrTitleColor,
                "Resumen clínico: ${globalEval.clinicalSummaryText}" to CLINICAL_SLATE_INK
            ),
            bgColor = hdrBg,
            borderColor = hdrBorder,
            titleColor = hdrTitleColor
        )

        // 2) Advertencias Clínicas Prioritarias (siempre visibles cuando existan, incluso en BORRADOR)
        if (globalEval.hasPriorityCriticalWarning) {
            val alertLines = globalEval.priorityCriticalAlerts.map { alert ->
                "• [${alert.axis.code} · ${alert.code}] ${alert.message}" to CLINICAL_RED_INK
            }
            drawCard(
                title = "⚠ ADVERTENCIAS CLÍNICAS PRIORITARIAS",
                lines = alertLines,
                bgColor = SOFT_RED_FILL,
                borderColor = CLINICAL_RED_INK,
                titleColor = CLINICAL_RED_INK
            )
        }

        // 3) Motivos de Evaluación Incompleta / Bloqueo (cuando corresponda)
        if (isDraft) {
            val blockingLines = mutableListOf<Pair<String, Int>>()
            globalEval.blockingReasons.forEach { reason ->
                val color = if (reason.contains("Discrepancia", ignoreCase = true) || reason.contains("BLOQUEO", ignoreCase = true)) {
                    CLINICAL_RED_INK
                } else {
                    CLINICAL_DARK_INK
                }
                blockingLines.add("• $reason" to color)
            }
            val combinedReasonsText = globalEval.blockingReasons.joinToString(" ")
            if (globalEval.pendingMandatoryItemCodes.isNotEmpty() && !combinedReasonsText.contains("sin responder", ignoreCase = true)) {
                blockingLines.add(
                    "• Reactivos obligatorios sin responder: ${globalEval.pendingMandatoryItemCodes.joinToString(", ")}" to CLINICAL_AMBER_INK
                )
            }
            if (globalEval.missingMaterialItemCodes.isNotEmpty() && !combinedReasonsText.contains("material", ignoreCase = true)) {
                blockingLines.add(
                    "• No evaluados por falta de material/lámina: ${globalEval.missingMaterialItemCodes.joinToString(", ")}" to CLINICAL_AMBER_INK
                )
            }
            if (globalEval.discrepancyItemCodes.isNotEmpty() && !combinedReasonsText.contains("discrepancia", ignoreCase = true)) {
                blockingLines.add(
                    "• Discrepancias observación/entrevista pendientes de resolución humana: ${globalEval.discrepancyItemCodes.joinToString(", ")}" to CLINICAL_RED_INK
                )
            }
            drawCard(
                title = "CONTROLES PENDIENTES Y MOTIVOS DE BORRADOR INCOMPLETO",
                lines = blockingLines.ifEmpty {
                    listOf("Evaluación incompleta: complete los ítems obligatorios." to CLINICAL_DARK_INK)
                },
                bgColor = SOFT_AMBER_FILL,
                borderColor = CLINICAL_AMBER_INK,
                titleColor = CLINICAL_AMBER_INK
            )
        }

        // 4) Edad Gestacional, Edad Cronológica/Corregida y Comparación EDI vs CRED
        val ageLines = mutableListOf<Pair<String, Int>>()
        ageLines.add(
            "Fecha Nacimiento: ${state.fechaNacimiento.ifBlank { "No ingresada" }}  |  Fecha Evaluación: ${state.fechaEvaluacion}  |  Sexo: ${state.sexo.label}" to CLINICAL_DARK_INK
        )
        val gaDetailText = when {
            state.esPrematuro -> "${state.ediGestationalAgeInput.originalLabel} (${state.ediGestationalAgeInput.formulaDocumentation})"
            state.egSemanas >= 37 -> "${state.ediGestationalAgeInput.originalLabel} (A término ≥37 semanas; no aplica corrección por prematuridad)"
            else -> "A término (≥37 semanas; no aplica corrección por prematuridad)"
        }
        ageLines.add(
            "Edad Gestacional: $gaDetailText" to CLINICAL_DARK_INK
        )
        val cronoStr = if (state.ediHasFechaNacimientoValida) {
            "${state.edadCronologicaAMD} (${state.edadCronologicaMeses}m ${state.edadCronologicaDias}d)"
        } else "No calculada"
        val corrStr = if (state.ediHasFechaNacimientoValida && state.usaEdadCorregida) {
            "${state.edadCorregidaAMD} (${state.edadCorregidaMeses}m ${state.edadCorregidaDias}d)"
        } else "No aplica"
        ageLines.add("Edad Cronológica (CRED): $cronoStr  |  Edad Corregida: $corrStr" to CLINICAL_DARK_INK)
        if (state.ediHasFechaNacimientoValida) {
            ageLines.add(
                "Comparación Manual EDI vs Calendario CRED: ${state.ediCorrectedAgeComparison.clinicalReviewMessage}" to CLINICAL_SLATE_INK
            )
        }
        state.ediAgeDiscrepancyResolution?.takeIf { state.ediIsDiscrepancyResolvedForCurrentInputs }?.let { res ->
            ageLines.add(
                "Resolución profesional documentada: G${res.selectedGroupNumber} — ${res.clinicalJustificationNote}" to CLINICAL_GREEN_INK
            )
        }
        if (state.ediManualGroupOverride) {
            ageLines.add(
                "Selección manual de grupo activa desde el Banco de los 15 Grupos (grupo evaluado: ${groupSpec.code})." to CLINICAL_AMBER_INK
            )
        }
        drawCard(
            title = "DATOS ETARIOS, PREMATURIDAD Y COMPARACIÓN DE MÉTODOS",
            lines = ageLines,
            bgColor = SOFT_BLUE_FILL,
            borderColor = CLINICAL_BLUE_INK,
            titleColor = CLINICAL_BLUE_INK
        )

        // 5) Resumen por Ejes, Retrocesos y Advertencias Opcionales (G5–G15)
        val axesLines = mutableListOf<Pair<String, Int>>()
        val frbScope = if (globalEval.frbMandatoryForGroup) "Obligatorio (G1–G4)" else "Opcional en G5–G15"
        axesLines.add(
            "Eje 1 (FRB - $frbScope): ${globalEval.positiveFrbItems.size}/7 positivos" +
                (if (globalEval.positiveFrbItems.isNotEmpty()) " (${globalEval.positiveFrbItems.joinToString(", ") { it.itemCode }})" else "") to CLINICAL_DARK_INK
        )
        val aleScope = if (globalEval.aleMandatoryForGroup) "Obligatorio (G1–G4)" else "Opcional en G5–G15"
        axesLines.add(
            "Eje 2 (ALE - $aleScope): ${globalEval.positiveAleItems.size}/${groupSpec.aleItems.size} positivas" +
                (if (globalEval.positiveAleItems.isNotEmpty()) " (${globalEval.positiveAleItems.joinToString(", ") { it.itemCode }})" else "") to CLINICAL_DARK_INK
        )
        axesLines.add("Eje 3 (Áreas del Desarrollo):" to CLINICAL_BLUE_INK)
        globalEval.axis3AreasSummary.areaResults.values.forEach { areaRes ->
            val areaColor = when (areaRes.status) {
                EdiAreaColorResult.VERDE -> CLINICAL_GREEN_INK
                EdiAreaColorResult.AMARILLO -> CLINICAL_AMBER_INK
                EdiAreaColorResult.ROJO -> CLINICAL_RED_INK
                else -> CLINICAL_SLATE_INK
            }
            axesLines.add("  • ${areaRes.clinicalRuleSummary}" to areaColor)
            if (areaRes.fallbackRequired) {
                val fbSummary = areaRes.fallbackItems.joinToString(" | ") { fbSpec ->
                    val r = areaRes.activeFallbackResponses[fbSpec.fallbackUniqueId] ?: EdiItemResponse.UNANSWERED
                    "${fbSpec.sourceItem.itemCode}=${responseShortLabel(r)}"
                }
                axesLines.add("    Retroceso activo: $fbSummary" to CLINICAL_DARK_INK)
            }
        }
        axesLines.add(
            "Eje 4 (EN): ${globalEval.positiveEnItems.size} alteración(es)" +
                (if (!globalEval.en13AppliesToGroup) " · EN 1.3 NO APLICA en Grupo 15" else "") to CLINICAL_DARK_INK
        )
        axesLines.add(
            if (!globalEval.alaAppliesToGroup) {
                "Eje 5 (ALA): NO APLICA en Grupo 1"
            } else {
                "Eje 5 (ALA): ${globalEval.positiveAlaItems.size}/${groupSpec.alaItems.size} positiva(s)" +
                    if (globalEval.positiveAlaItems.isNotEmpty()) {
                        " (${globalEval.positiveAlaItems.joinToString(", ") { it.itemCode }})"
                    } else ""
            } to CLINICAL_DARK_INK
        )
        globalEval.causes.forEach { cause ->
            axesLines.add("Causa de dictamen: ${cause.description}" to CLINICAL_SLATE_INK)
        }
        if (globalEval.nonBlockingClinicalWarnings.isNotEmpty()) {
            axesLines.add("Advertencias no bloqueantes (FRB/ALE opcionales G5–G15):" to CLINICAL_BLUE_INK)
            globalEval.nonBlockingClinicalWarnings.forEach { warn ->
                axesLines.add("  • $warn" to CLINICAL_SLATE_INK)
            }
        }
        drawCard(
            title = "DESGLOSE DE LOS 5 EJES, RETROCESOS Y TRAZABILIDAD",
            lines = axesLines
        )

        // 6) Evidencias Duales y Notas Clínicas por Ítem (si existen)
        val dualRecords = state.ediEngineState.activeSession.dualEvidenceRecords
        val itemNotes = state.ediEngineState.activeSession.itemClinicalNotes
        if (dualRecords.isNotEmpty() || itemNotes.isNotEmpty()) {
            val extraLines = mutableListOf<Pair<String, Int>>()
            dualRecords.forEach { (id, rec) ->
                val obs = when (rec.directObservationEvidence) {
                    true -> "SÍ"
                    false -> "NO"
                    null -> "Pend."
                }
                val ent = when (rec.directedInterviewEvidence) {
                    true -> "SÍ"
                    false -> "NO"
                    null -> "Pend."
                }
                val res = when (rec.professionalHumanResolution) {
                    true -> "SÍ"
                    false -> "NO"
                    null -> if (rec.hasConflictBetweenEvidences) "Sin resolver" else "Consistente"
                }
                val notePart = if (rec.clinicalJustificationNote.isNotBlank()) " · Nota: ${rec.clinicalJustificationNote}" else ""
                extraLines.add("• Protocolo dual [$id]: Obs=$obs, Entrevista=$ent, Resolución=$res$notePart" to CLINICAL_DARK_INK)
            }
            itemNotes.forEach { (id, note) ->
                extraLines.add("• Nota clínica [$id]: $note" to CLINICAL_SLATE_INK)
            }
            drawCard(
                title = "MODALIDADES DUALES Y OBSERVACIONES CLÍNICAS",
                lines = extraLines,
                bgColor = SOFT_BLUE_FILL,
                borderColor = CLINICAL_BLUE_INK
            )
        }

        // 7) Registro Individualizado Completo de Reactivos (correspondencia 1:1 con la Ficha)
        val itemizedLines = mutableListOf<Pair<String, Int>>()
        fun cleanQ(raw: String): String = raw.replace("**", "").trim()
        EdiOfficialCatalog.frbItems.forEach { item ->
            val r = globalEval.frbResponsesResolved[item.uniqueId] ?: EdiItemResponse.UNANSWERED
            itemizedLines.add("${item.itemCode}: ${responseShortLabel(r)} — ${cleanQ(item.consultationQuestionText)}" to responseColor(r))
        }
        groupSpec.aleItems.forEach { item ->
            val r = globalEval.aleResponsesResolved[item.uniqueId] ?: EdiItemResponse.UNANSWERED
            itemizedLines.add("${item.itemCode}: ${responseShortLabel(r)} — ${cleanQ(item.consultationQuestionText)}" to responseColor(r))
        }
        EdiDevArea.entries.forEach { area ->
            val areaRes = globalEval.axis3AreasSummary.areaResults[area]
            if (areaRes == null || !areaRes.appliesToGroup) {
                itemizedLines.add("${area.code}: NO APLICA en ${groupSpec.code}" to CLINICAL_SLATE_INK)
            } else {
                areaRes.currentItems.forEach { item ->
                    val r = areaRes.currentItemResponses[item.uniqueId] ?: EdiItemResponse.UNANSWERED
                    itemizedLines.add("${item.itemCode}: ${responseShortLabel(r)} — ${cleanQ(item.consultationQuestionText)}" to responseColor(r))
                }
                if (areaRes.fallbackRequired) {
                    areaRes.fallbackItems.forEach { fbSpec ->
                        val fbR = areaRes.activeFallbackResponses[fbSpec.fallbackUniqueId] ?: EdiItemResponse.UNANSWERED
                        itemizedLines.add(
                            "  ↳ Retroceso ${fbSpec.sourceItem.itemCode}: ${responseShortLabel(fbR)} — ${cleanQ(fbSpec.sourceItem.consultationQuestionText)}" to responseColor(fbR)
                        )
                    }
                }
            }
        }
        EdiOfficialCatalog.getApplicableEnItems(groupSpec.groupNumber).forEach { item ->
            val r = globalEval.enResponsesResolved[item.uniqueId] ?: EdiItemResponse.UNANSWERED
            itemizedLines.add("${item.itemCode}: ${responseShortLabel(r)} — ${cleanQ(item.consultationQuestionText)}" to responseColor(r))
        }
        if (!globalEval.en13AppliesToGroup) {
            itemizedLines.add("EN 1.3: NO APLICA en Grupo 15" to CLINICAL_SLATE_INK)
        }
        if (!globalEval.alaAppliesToGroup) {
            itemizedLines.add("ALA: NO APLICA en Grupo 1" to CLINICAL_SLATE_INK)
        } else {
            groupSpec.alaItems.forEach { item ->
                val r = globalEval.alaResponsesResolved[item.uniqueId] ?: EdiItemResponse.UNANSWERED
                itemizedLines.add("${item.itemCode}: ${responseShortLabel(r)} — ${cleanQ(item.consultationQuestionText)}" to responseColor(r))
            }
        }

        ensureSpace(140f, "REGISTRO INDIVIDUAL DE REACTIVOS EDI 2026")
        canvas.drawText(
            "REGISTRO INDIVIDUAL DE REACTIVOS (${groupSpec.code})",
            margin,
            cursorY + 24f,
            titlePaint.apply { textSize = 24f }
        )
        cursorY += 48f

        itemizedLines.forEach { (line, color) ->
            val wrapped = wrapText(line, boldBodyPaint, contentWidth - 24f)
            wrapped.forEach { wLine ->
                if (cursorY + 28f > maxY) {
                    startNewPage("REGISTRO INDIVIDUAL DE REACTIVOS (${groupSpec.code} — cont.)")
                }
                canvas.drawText(
                    wLine,
                    margin + 12f,
                    cursorY,
                    Paint(bodyPaint).apply {
                        textSize = 18.5f
                        this.color = color
                    }
                )
                cursorY += 25f
            }
        }

        drawPageFooter(canvas, pages.size + 2, isDraft)
        pages.add(currentBitmap)
        return pages
    }

    private fun drawPageFooter(canvas: Canvas, pageNumber: Int, isDraft: Boolean) {
        val footerPaint = Paint().apply {
            color = CLINICAL_SLATE_INK
            textSize = 16f
            isAntiAlias = true
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        val tag = if (isDraft) "BORRADOR INCOMPLETO" else "FICHA OFICIAL EDI 2026"
        canvas.drawText(
            "$tag · Manual EDI Peruano (Primera Edición 2026 — MINSA/UNICEF) · Página $pageNumber",
            72f,
            PAGE_HEIGHT - 32f,
            footerPaint
        )
    }

    private fun responseShortLabel(response: EdiItemResponse): String = when (response) {
        EdiItemResponse.YES -> "SÍ"
        EdiItemResponse.NO -> "NO"
        EdiItemResponse.UNANSWERED -> "SIN RESPONDER"
        EdiItemResponse.NOT_EVALUATED_MISSING_MATERIAL -> "NO EVALUADO (FALTA MATERIAL)"
        EdiItemResponse.DISCREPANCY_REQUIRES_HUMAN_RESOLUTION -> "DISCREPANCIA (REQ. RESOLUCIÓN)"
    }

    private fun responseColor(response: EdiItemResponse): Int = when (response) {
        EdiItemResponse.YES -> CLINICAL_BLUE_INK
        EdiItemResponse.NO -> CLINICAL_DARK_INK
        EdiItemResponse.UNANSWERED -> CLINICAL_SLATE_INK
        EdiItemResponse.NOT_EVALUATED_MISSING_MATERIAL -> CLINICAL_AMBER_INK
        EdiItemResponse.DISCREPANCY_REQUIRES_HUMAN_RESOLUTION -> CLINICAL_RED_INK
    }

    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        if (text.isBlank()) return listOf("")
        val words = text.split(Regex("\\s+"))
        val lines = mutableListOf<String>()
        val current = StringBuilder()
        words.forEach { word ->
            val candidate = if (current.isEmpty()) word else "$current $word"
            if (paint.measureText(candidate) <= maxWidth) {
                current.clear()
                current.append(candidate)
            } else {
                if (current.isNotEmpty()) {
                    lines.add(current.toString())
                    current.clear()
                }
                current.append(word)
            }
        }
        if (current.isNotEmpty()) {
            lines.add(current.toString())
        }
        return lines
    }

    private fun loadTemplateBitmap(context: Context): Bitmap? {
        try {
            context.assets.open("ficha_edi.jpg").use { inputStream ->
                val bm = BitmapFactory.decodeStream(inputStream)
                if (bm != null) return bm
            }
        } catch (_: Exception) {}

        val candidatePaths = listOf(
            File("app/src/main/assets/ficha_edi.jpg"),
            File("src/main/assets/ficha_edi.jpg"),
            File("../app/src/main/assets/ficha_edi.jpg")
        )
        for (f in candidatePaths) {
            if (f.exists() && f.canRead()) {
                try {
                    f.inputStream().use { stream ->
                        val bm = BitmapFactory.decodeStream(stream)
                        if (bm != null) return bm
                    }
                } catch (_: Exception) {}
            }
        }
        return null
    }

    private fun resolveUniquePdfFile(directory: File, basePrefix: String): File {
        if (!directory.exists()) {
            directory.mkdirs()
        }
        val stamp = SimpleDateFormat("HHmmss", Locale.US).format(Date())
        var candidate = File(directory, "${basePrefix}_$stamp.pdf")
        var suffix = 1
        while (candidate.exists()) {
            candidate = File(directory, "${basePrefix}_${stamp}_$suffix.pdf")
            suffix++
        }
        return candidate
    }

    private fun drawX(canvas: Canvas, cx: Float, cy: Float, radius: Float, paint: Paint) {
        canvas.drawLine(cx - radius, cy - radius, cx + radius, cy + radius, paint)
        canvas.drawLine(cx + radius, cy - radius, cx - radius, cy + radius, paint)
    }

    private fun drawRing(canvas: Canvas, cx: Float, cy: Float, radius: Float, paint: Paint) {
        canvas.drawCircle(cx, cy, radius, paint)
    }

    private fun drawVerticalText(
        canvas: Canvas,
        text: String,
        cx: Float,
        cy: Float,
        paint: Paint
    ) {
        canvas.save()
        canvas.rotate(-90f, cx, cy)
        val metrics = paint.fontMetrics
        val baselineOffset = -(metrics.ascent + metrics.descent) / 2f
        canvas.drawText(text, cx, cy + baselineOffset, paint)
        canvas.restore()
    }

    private fun saveMultiPagePdfDocument(
        context: Context,
        pages: List<Bitmap>,
        localFile: File,
        isDraft: Boolean,
        groupNumber: Int,
        trafficLightLabel: String
    ): ExportPdfResult {
        var pdfWritten = false
        val pdfDocument = PdfDocument()
        try {
            pages.forEachIndexed { idx, bm ->
                val pageInfo = PdfDocument.PageInfo.Builder(bm.width, bm.height, idx + 1).create()
                val page = pdfDocument.startPage(pageInfo)
                page.canvas.drawBitmap(bm, 0f, 0f, null)
                pdfDocument.finishPage(page)
            }
            FileOutputStream(localFile).use { out ->
                pdfDocument.writeTo(out)
            }
            pdfWritten = localFile.exists() && localFile.length() > 1000L
        } catch (_: Throwable) {
            pdfWritten = false
        } finally {
            try {
                pdfDocument.close()
            } catch (_: Throwable) {}
        }

        // Respaldo estándar PDF 1.4 multipágina (por ejemplo en pruebas JVM locales sin Skia nativo)
        if (!pdfWritten) {
            FileOutputStream(localFile).use { out ->
                writeMultiPageImagesToPdf(out, pages)
            }
        }

        var publicUri: Uri? = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, localFile.name)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = context.contentResolver.insert(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    contentValues
                )
                if (uri != null) {
                    context.contentResolver.openOutputStream(uri)?.use { outStream ->
                        localFile.inputStream().use { inStream ->
                            inStream.copyTo(outStream)
                        }
                    }
                    publicUri = uri
                }
            } catch (_: Exception) {}
        }

        val contentUri = try {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                localFile
            )
        } catch (_: Exception) {
            publicUri ?: Uri.fromFile(localFile)
        }

        val viewIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(contentUri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val userMessage = if (isDraft) {
            "Borrador EDI (Evaluación Incompleta) guardado en Descargas: ${localFile.name}. No constituye un dictamen definitivo."
        } else {
            "Ficha Oficial EDI 2026 guardada en Descargas: ${localFile.name}"
        }

        return ExportPdfResult(
            success = true,
            message = userMessage,
            fileUri = contentUri,
            filePath = localFile.absolutePath,
            viewIntent = viewIntent,
            isDraft = isDraft,
            fileName = localFile.name,
            groupNumber = groupNumber,
            trafficLightLabel = trafficLightLabel
        )
    }

    private fun writeMultiPageImagesToPdf(out: OutputStream, bitmaps: List<Bitmap>) {
        val header = "%PDF-1.4\n%âãÏÓ\n".toByteArray(Charsets.US_ASCII)
        out.write(header)
        var currentOffset = header.size
        val offsets = mutableListOf<Int>()

        val pageCount = bitmaps.size
        // Objetos: 1=Catalog, 2=Pages, luego por cada página i (0..pageCount-1):
        // pageObj = 3 + i*3, contentObj = 4 + i*3, imageObj = 5 + i*3
        val pageObjIds = (0 until pageCount).map { 3 + it * 3 }
        val kidsStr = pageObjIds.joinToString(" ") { "$it 0 R" }

        fun writeAsciiObj(content: String) {
            offsets.add(currentOffset)
            val bytes = content.toByteArray(Charsets.US_ASCII)
            out.write(bytes)
            currentOffset += bytes.size
        }

        // Obj 1: Catalog
        writeAsciiObj("1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n")
        // Obj 2: Pages
        writeAsciiObj("2 0 obj\n<< /Type /Pages /Kids [$kidsStr] /Count $pageCount >>\nendobj\n")

        bitmaps.forEachIndexed { idx, bitmap ->
            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 92, baos)
            val jpegBytes = baos.toByteArray()
            val w = bitmap.width
            val h = bitmap.height

            val pageId = 3 + idx * 3
            val contentId = 4 + idx * 3
            val imageId = 5 + idx * 3
            val imName = "/Im$idx"

            val streamContent = "q $w 0 0 $h 0 0 cm $imName Do Q\n".toByteArray(Charsets.US_ASCII)

            writeAsciiObj(
                "$pageId 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 $w $h] /Contents $contentId 0 R /Resources << /XObject << $imName $imageId 0 R >> >> >>\nendobj\n"
            )
            writeAsciiObj(
                "$contentId 0 obj\n<< /Length ${streamContent.size} >>\nstream\n${String(streamContent, Charsets.US_ASCII)}endstream\nendobj\n"
            )

            offsets.add(currentOffset)
            val imgHeader = (
                "$imageId 0 obj\n<< /Type /XObject /Subtype /Image /Width $w /Height $h " +
                    "/ColorSpace /DeviceRGB /BitsPerComponent 8 /Filter /DCTDecode /Length ${jpegBytes.size} >>\nstream\n"
                ).toByteArray(Charsets.US_ASCII)
            val imgFooter = "\nendstream\nendobj\n".toByteArray(Charsets.US_ASCII)
            out.write(imgHeader)
            out.write(jpegBytes)
            out.write(imgFooter)
            currentOffset += imgHeader.size + jpegBytes.size + imgFooter.size
        }

        val totalObjects = offsets.size + 1
        val xrefOffset = currentOffset
        out.write("xref\n0 $totalObjects\n0000000000 65535 f \n".toByteArray(Charsets.US_ASCII))
        offsets.forEach { off ->
            out.write(String.format(Locale.US, "%010d 00000 n \n", off).toByteArray(Charsets.US_ASCII))
        }
        val trailer = "trailer\n<< /Size $totalObjects /Root 1 0 R >>\nstartxref\n$xrefOffset\n%%EOF\n"
            .toByteArray(Charsets.US_ASCII)
        out.write(trailer)
        out.flush()
    }
}
