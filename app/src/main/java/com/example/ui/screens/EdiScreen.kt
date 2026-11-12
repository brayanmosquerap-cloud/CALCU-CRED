package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MedicalInformation
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.ReportProblem
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CredEdiUiState
import com.example.model.EdiAreaColorResult
import com.example.model.EdiAreaEvaluationResult
import com.example.model.EdiAxis
import com.example.model.EdiCatalogItem
import com.example.model.EdiClinicalModality
import com.example.model.EdiCorrectedAgeComparison
import com.example.model.EdiDevArea
import com.example.model.EdiDualEvidenceRecord
import com.example.model.EdiGlobalEvaluationResult
import com.example.model.EdiGlobalTrafficLight
import com.example.model.EdiGroupAutoSelectionStatus
import com.example.model.EdiGroupChangeResolutionAction
import com.example.model.EdiGroupFilterCategory
import com.example.model.EdiGroupSpec
import com.example.model.EdiInternalView
import com.example.model.EdiItemResponse
import com.example.model.EdiOfficialCatalog
import com.example.model.EdiPendingGroupChangeRequest
import com.example.ui.components.ClinicalSectionCard
import com.example.ui.components.PillBadge
import com.example.util.EdiPdfExporter
import com.example.util.ExportPdfResult
import com.example.ui.theme.AlertRed
import com.example.ui.theme.BorderAmber
import com.example.ui.theme.BorderGreen
import com.example.ui.theme.BorderSoftGreen
import com.example.ui.theme.BorderSoftRed
import com.example.ui.theme.ClinicalGreen
import com.example.ui.theme.DarkGreenPill
import com.example.ui.theme.DarkRedText
import com.example.ui.theme.DarkText
import com.example.ui.theme.LocalGenderPalette
import com.example.ui.theme.MutedText
import com.example.ui.theme.SlateText
import com.example.ui.theme.SoftAmberBg
import com.example.ui.theme.SoftAmberPill
import com.example.ui.theme.SoftGrayBg
import com.example.ui.theme.SoftGreenBg
import com.example.ui.theme.SoftGreenPill
import com.example.ui.theme.SoftRedBg
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WarningText

private val EdiScreenBg = Color(0xFFEEF2F7)
private val EdiNavy = Color(0xFF1E2638)
private val EdiSoftBlueBg = Color(0xFFEAF2FF)
private val EdiSoftBlueBorder = Color(0xFFBFD7FE)
private val EdiBlueAccent = Color(0xFF0B57D0)

@Composable
fun EdiScreen(
    state: CredEdiUiState,
    onNavigateView: (EdiInternalView) -> Unit,
    onNavigateBack: () -> Unit,
    onOpenAxisQuestionnaire: (EdiAxis) -> Unit,
    onSelectAxis: (EdiAxis) -> Unit,
    onSelectDevAreaFilter: (EdiDevArea?) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSelectGroupFilter: (EdiGroupFilterCategory) -> Unit,
    onSelectGroupFromBank: (Int) -> Unit,
    onUseAgeSuggestedGroup: () -> Unit,
    onResolvePendingGroupChange: (EdiGroupChangeResolutionAction) -> Unit,
    onConfirmAgeDiscrepancyResolution: (Int, String) -> Unit,
    onTogglePrematurez: (Boolean) -> Unit,
    onAdjustEgSemanas: (Int) -> Unit,
    onAdjustEgDias: (Int) -> Unit,
    onUpdateEdiFechaEval: (String) -> Unit,
    onAdjustEdiFechaEvalYear: (Int) -> Unit,
    onRecordCurrentItemResponse: (String, EdiItemResponse) -> Unit,
    onRecordDualEvidence: (String, Boolean?, Boolean?, Boolean?, String) -> Unit,
    onRecordFallbackResponse: (String, EdiItemResponse) -> Unit,
    onRecordFallbackDualEvidence: (String, Boolean?, Boolean?, Boolean?, String) -> Unit,
    onUpdateItemClinicalNote: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (state.ediInternalView != EdiInternalView.SUMMARY_5_AXES) {
        BackHandler { onNavigateBack() }
    }

    val pendingConfirmation = state.ediEngineState.pendingGroupChangeConfirmation
    if (pendingConfirmation != null) {
        EdiGroupChangeConfirmationDialog(
            request = pendingConfirmation,
            onResolve = onResolvePendingGroupChange
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(EdiScreenBg)
    ) {
        when (state.ediInternalView) {
            EdiInternalView.SUMMARY_5_AXES -> EdiSummary5AxesView(
                state = state,
                onNavigateView = onNavigateView,
                onOpenAxisQuestionnaire = onOpenAxisQuestionnaire
            )

            EdiInternalView.GROUP_BANK_15 -> EdiGroupBank15View(
                state = state,
                onBack = onNavigateBack,
                onNavigateView = onNavigateView,
                onSearchQueryChange = onSearchQueryChange,
                onSelectGroupFilter = onSelectGroupFilter,
                onSelectGroupFromBank = onSelectGroupFromBank,
                onUseAgeSuggestedGroup = onUseAgeSuggestedGroup,
                onConfirmAgeDiscrepancyResolution = onConfirmAgeDiscrepancyResolution,
                onTogglePrematurez = onTogglePrematurez,
                onAdjustEgSemanas = onAdjustEgSemanas,
                onAdjustEgDias = onAdjustEgDias,
                onUpdateEdiFechaEval = onUpdateEdiFechaEval,
                onAdjustEdiFechaEvalYear = onAdjustEdiFechaEvalYear
            )

            EdiInternalView.QUESTIONNAIRE_AND_FALLBACK -> EdiQuestionnaireAndFallbackView(
                state = state,
                onBack = onNavigateBack,
                onNavigateView = onNavigateView,
                onSelectAxis = onSelectAxis,
                onSelectDevAreaFilter = onSelectDevAreaFilter,
                onRecordCurrentItemResponse = onRecordCurrentItemResponse,
                onRecordDualEvidence = onRecordDualEvidence,
                onRecordFallbackResponse = onRecordFallbackResponse,
                onRecordFallbackDualEvidence = onRecordFallbackDualEvidence,
                onUpdateItemClinicalNote = onUpdateItemClinicalNote
            )

            EdiInternalView.RESULTS_TRACEABILITY -> EdiResultsTraceabilityView(
                state = state,
                onBack = onNavigateBack,
                onNavigateView = onNavigateView
            )
        }
    }
}

@Composable
private fun EdiSummary5AxesView(
    state: CredEdiUiState,
    onNavigateView: (EdiInternalView) -> Unit,
    onOpenAxisQuestionnaire: (EdiAxis) -> Unit
) {
    val palette = LocalGenderPalette.current
    val groupSpec = state.ediActiveGroupSpec
    val globalEval = state.ediGlobalEvaluation
    val (doneMandatory, totalMandatory) = state.ediMandatoryItemsProgress
    val progressFraction = (doneMandatory.toFloat() / totalMandatory.toFloat()).coerceIn(0f, 1f)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .testTag("edi_view_summary"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Evaluación del Desarrollo (EDI)",
                    color = DarkText,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Manual EDI Peruano 2026 · 5 Ejes Oficiales",
                    color = SlateText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            PillBadge(
                text = groupSpec.code,
                backgroundColor = EdiNavy,
                textColor = Color.White,
                fontSizeSp = 12,
                horizontalPadding = 12.dp,
                verticalPadding = 6.dp
            )
        }

        // Chronological & Corrected Age Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ClinicalSectionCard(
                modifier = Modifier.weight(1f),
                contentPadding = 12.dp
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CalendarToday,
                        contentDescription = "Edad cronológica",
                        tint = EdiBlueAccent,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Edad Cronológica",
                        color = SlateText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (state.ediHasFechaNacimientoValida) state.edadCronologicaAMD else "Sin fecha",
                    color = DarkText,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = if (state.ediHasFechaNacimientoValida) {
                        "${state.edadCronologicaMeses}m ${state.edadCronologicaDias}d"
                    } else {
                        "Ingrese F.N. en Tab 1"
                    },
                    color = MutedText,
                    fontSize = 11.sp
                )
            }

            ClinicalSectionCard(
                modifier = Modifier.weight(1f),
                contentPadding = 12.dp
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Schedule,
                        contentDescription = "Edad corregida",
                        tint = if (state.usaEdadCorregida) ClinicalGreen else SlateText,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Edad Corregida",
                        color = SlateText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = when {
                        !state.ediHasFechaNacimientoValida -> "EG ${state.egSemanas}s ${state.egDias}d"
                        state.usaEdadCorregida -> state.edadCorregidaAMD
                        else -> "No aplica"
                    },
                    color = DarkText,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = if (state.esPrematuro) {
                        "Prematuro ${state.egSemanas}s ${state.egDias}d"
                    } else {
                        "A término (${state.egSemanas}s ${state.egDias}d)"
                    },
                    color = MutedText,
                    fontSize = 11.sp
                )
            }
        }

        // Active Group Banner
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = EdiNavy
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "GRUPO ETARIO ACTIVO",
                            color = Color(0xFF94A3B8),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = groupSpec.officialTitle,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${groupSpec.shortIntervalLabel} · ${groupSpec.itemsPerActiveArea} reactivos/área",
                            color = Color(0xFFCBD5E1),
                            fontSize = 12.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedButton(
                        onClick = { onNavigateView(EdiInternalView.GROUP_BANK_15) },
                        modifier = Modifier
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("edi_btn_change_group"),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color(0xFF475569)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color(0xFF334155),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.SwapHoriz,
                            contentDescription = "Cambiar grupo EDI",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Cambiar", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (state.ediManualGroupOverride) {
                    Spacer(modifier = Modifier.height(8.dp))
                    PillBadge(
                        text = "Selección manual desde Banco (Edad sugiere ${state.ediCorrectedAgeComparison.automaticallyAssignedGroup?.code ?: "-"})",
                        backgroundColor = Color(0xFF334155),
                        textColor = Color(0xFFFDE68A),
                        borderColor = Color(0xFFF59E0B),
                        fontSizeSp = 11
                    )
                }
            }
        }

        // Age Discrepancy or Out-of-range Warning
        if (state.ediHasUnresolvedAgeDiscrepancy || state.ediIsOutOfOfficialAgeRange) {
            ClinicalSectionCard(
                backgroundColor = SoftAmberBg,
                borderColor = BorderAmber,
                modifier = Modifier.testTag("edi_summary_age_warning")
            ) {
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.WarningAmber,
                        contentDescription = "Advertencia de edad EDI",
                        tint = WarningAmber,
                        modifier = Modifier.size(22.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (state.ediHasUnresolvedAgeDiscrepancy) {
                                "Discrepancia de grupo etario (Manual EDI vs Calendario CRED)"
                            } else {
                                "Edad fuera del rango oficial EDI (1m a 71m 29d)"
                            },
                            color = WarningText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = state.ediCorrectedAgeComparison.clinicalReviewMessage,
                            color = DarkText,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { onNavigateView(EdiInternalView.GROUP_BANK_15) },
                            modifier = Modifier
                                .defaultMinSize(minHeight = 48.dp)
                                .testTag("edi_btn_resolve_age_discrepancy"),
                            colors = ButtonDefaults.buttonColors(containerColor = WarningAmber),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Revisar en Banco de Grupos", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Priority Critical Warnings (visible even if incomplete)
        if (globalEval.hasPriorityCriticalWarning) {
            EdiPriorityWarningBanner(globalEval = globalEval)
        }

        // Progress + Calculated Status Card
        ClinicalSectionCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Avance de reactivos obligatorios",
                        color = DarkText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "$doneMandatory de $totalMandatory completados",
                        color = SlateText,
                        fontSize = 12.sp
                    )
                }
                EdiTrafficLightPill(globalEval.trafficLight)
            }
            Spacer(modifier = Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { progressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(50)),
                color = palette.primary,
                trackColor = SoftGrayBg
            )
        }

        // 5 Axes Summary Cards
        Text(
            text = "Los 5 Ejes de Evaluación EDI",
            color = DarkText,
            fontSize = 15.sp,
            fontWeight = FontWeight.ExtraBold
        )

        val frbYesCount = globalEval.positiveFrbItems.size
        val frbAnswered = globalEval.frbResponsesResolved.values.count { it.isAnsweredDefinitively }
        EdiAxisSummaryCard(
            axis = EdiAxis.FRB,
            title = "Eje 1 · Factores de Riesgo Biológico (FRB)",
            subtitle = "$frbAnswered/7 evaluados · $frbYesCount positivo(s)",
            badgeText = if (globalEval.frbMandatoryForGroup) "Obligatorio (G1-G4)" else "Opcional en G5-G15",
            badgeBg = if (frbYesCount > 0) SoftAmberPill else SoftGrayBg,
            badgeTextColor = if (frbYesCount > 0) WarningText else SlateText,
            onClick = { onOpenAxisQuestionnaire(EdiAxis.FRB) }
        )

        val aleTotal = groupSpec.aleItems.size
        val aleAnswered = globalEval.aleResponsesResolved.values.count { it.isAnsweredDefinitively }
        val alePos = globalEval.positiveAleItems.size
        EdiAxisSummaryCard(
            axis = EdiAxis.ALE,
            title = "Eje 2 · Señales de Alerta (ALE)",
            subtitle = "$aleAnswered/$aleTotal evaluadas · $alePos positiva(s)",
            badgeText = if (globalEval.aleMandatoryForGroup) "Obligatorio (G1-G4)" else "Opcional en G5-G15",
            badgeBg = if (alePos > 0) SoftAmberPill else SoftGrayBg,
            badgeTextColor = if (alePos > 0) WarningText else SlateText,
            onClick = { onOpenAxisQuestionnaire(EdiAxis.ALE) }
        )

        // Axis 3: Areas del Desarrollo
        val areaSummary = globalEval.axis3AreasSummary
        val fallbackCount = areaSummary.areasRequiringFallback.size
        ClinicalSectionCard(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .clickable { onOpenAxisQuestionnaire(EdiAxis.AREAS) }
                .testTag("edi_axis_summary_AREAS")
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Eje 3 · Áreas del Desarrollo",
                        color = DarkText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = if (fallbackCount > 0) {
                            "Retroceso activo en: ${areaSummary.areasRequiringFallback.joinToString(", ") { it.code }}"
                        } else {
                            "${areaSummary.completeApplicableAreasCount} área(s) completas · ${areaSummary.incompleteApplicableAreasCount} pendiente(s)"
                        },
                        color = if (fallbackCount > 0) WarningText else SlateText,
                        fontSize = 12.sp
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Abrir Áreas del Desarrollo",
                    tint = SlateText
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                EdiDevArea.entries.forEach { area ->
                    val res = areaSummary.areaResults[area]
                    val status = res?.status ?: EdiAreaColorResult.NOT_APPLICABLE
                    val (bg, fg) = areaStatusColors(status)
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        color = bg
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = area.code,
                                color = fg,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = when (status) {
                                    EdiAreaColorResult.VERDE -> "Verde"
                                    EdiAreaColorResult.AMARILLO -> "Amar."
                                    EdiAreaColorResult.ROJO -> "Rojo"
                                    EdiAreaColorResult.INCOMPLETE -> "Pend."
                                    EdiAreaColorResult.NOT_APPLICABLE -> "N/A"
                                },
                                color = fg,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        val enItems = EdiOfficialCatalog.getApplicableEnItems(groupSpec.groupNumber)
        val enAnswered = globalEval.enResponsesResolved.values.count { it.isAnsweredDefinitively }
        val enPos = globalEval.positiveEnItems.size
        EdiAxisSummaryCard(
            axis = EdiAxis.EN,
            title = "Eje 4 · Exploración Neurológica (EN)",
            subtitle = if (groupSpec.groupNumber == 15) {
                "$enAnswered/${enItems.size} evaluados · EN 1.3 no aplica en G15"
            } else {
                "$enAnswered/${enItems.size} evaluados · $enPos alteración(es)"
            },
            badgeText = if (enPos > 0) "$enPos alterada(s)" else "Obligatorio",
            badgeBg = if (enPos > 0) SoftRedBg else SoftGrayBg,
            badgeTextColor = if (enPos > 0) DarkRedText else SlateText,
            onClick = { onOpenAxisQuestionnaire(EdiAxis.EN) }
        )

        val alaItems = groupSpec.alaItems
        val alaAnswered = globalEval.alaResponsesResolved.values.count { it.isAnsweredDefinitively }
        val alaPos = globalEval.positiveAlaItems.size
        EdiAxisSummaryCard(
            axis = EdiAxis.ALA,
            title = "Eje 5 · Señales de Alarma (ALA)",
            subtitle = if (alaItems.isEmpty()) {
                "No aplica en el Grupo 1 (solo 4 ejes en G1)"
            } else {
                "$alaAnswered/${alaItems.size} evaluadas · $alaPos positiva(s)"
            },
            badgeText = if (alaItems.isEmpty()) "No aplica (G1)" else if (alaPos > 0) "$alaPos positiva(s)" else "Obligatorio",
            badgeBg = when {
                alaItems.isEmpty() -> SoftGrayBg
                alaPos > 0 -> SoftRedBg
                else -> SoftGrayBg
            },
            badgeTextColor = if (alaPos > 0) DarkRedText else SlateText,
            onClick = { onOpenAxisQuestionnaire(EdiAxis.ALA) }
        )

        // Navigation CTAs
        Button(
            onClick = { onNavigateView(EdiInternalView.QUESTIONNAIRE_AND_FALLBACK) },
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 52.dp)
                .testTag("edi_btn_continue_evaluation"),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = EdiNavy)
        ) {
            Icon(Icons.Outlined.Checklist, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Continuar Evaluación de Ítems",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = { onNavigateView(EdiInternalView.GROUP_BANK_15) },
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = 48.dp)
                    .testTag("edi_btn_open_bank"),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, palette.borderMedium)
            ) {
                Text("15 Grupos EDI", fontWeight = FontWeight.Bold, color = DarkText, fontSize = 13.sp)
            }
            OutlinedButton(
                onClick = { onNavigateView(EdiInternalView.RESULTS_TRACEABILITY) },
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = 48.dp)
                    .testTag("edi_btn_view_results"),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, palette.borderMedium)
            ) {
                Text("Ver Resultados EDI", fontWeight = FontWeight.Bold, color = DarkText, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun EdiAxisSummaryCard(
    axis: EdiAxis,
    title: String,
    subtitle: String,
    badgeText: String,
    badgeBg: Color,
    badgeTextColor: Color,
    onClick: () -> Unit
) {
    ClinicalSectionCard(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .testTag("edi_axis_summary_${axis.name}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = DarkText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = SlateText,
                    fontSize = 12.sp
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            PillBadge(
                text = badgeText,
                backgroundColor = badgeBg,
                textColor = badgeTextColor,
                fontSizeSp = 10
            )
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Ir a $title",
                tint = SlateText,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EdiGroupBank15View(
    state: CredEdiUiState,
    onBack: () -> Unit,
    onNavigateView: (EdiInternalView) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSelectGroupFilter: (EdiGroupFilterCategory) -> Unit,
    onSelectGroupFromBank: (Int) -> Unit,
    onUseAgeSuggestedGroup: () -> Unit,
    onConfirmAgeDiscrepancyResolution: (Int, String) -> Unit,
    onTogglePrematurez: (Boolean) -> Unit,
    onAdjustEgSemanas: (Int) -> Unit,
    onAdjustEgDias: (Int) -> Unit,
    onUpdateEdiFechaEval: (String) -> Unit,
    onAdjustEdiFechaEvalYear: (Int) -> Unit
) {
    val comparison = state.ediCorrectedAgeComparison
    val filteredGroups = remember(state.ediGroupSearchQuery, state.ediGroupFilterCategory) {
        val query = state.ediGroupSearchQuery.trim().lowercase()
        EdiOfficialCatalog.groups.filter { group ->
            val matchesCat = when (state.ediGroupFilterCategory) {
                EdiGroupFilterCategory.ALL -> true
                EdiGroupFilterCategory.LACTANTES_MENORES -> group.groupNumber in 1..5
                EdiGroupFilterCategory.LACTANTES_MAYORES -> group.groupNumber in 6..9
                EdiGroupFilterCategory.NINOS_19_36 -> group.groupNumber in 10..12
                EdiGroupFilterCategory.PREESCOLARES -> group.groupNumber in 13..15
            }
            val matchesQuery = query.isEmpty() ||
                group.code.lowercase().contains(query) ||
                group.officialTitle.lowercase().contains(query) ||
                group.shortIntervalLabel.lowercase().contains(query) ||
                "grupo ${group.groupNumber}".contains(query)
            matchesCat && matchesQuery
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("edi_view_group_bank")
    ) {
        // Top Bar
        Surface(color = Color.White, shadowElevation = 1.dp) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("edi_bank_back")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver al resumen EDI",
                        tint = DarkText
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Banco de los 15 Grupos EDI",
                        color = DarkText,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Control etario, prematuridad y plantillas oficiales 2026",
                        color = SlateText,
                        fontSize = 11.sp
                    )
                }
                TextButton(
                    onClick = { onNavigateView(EdiInternalView.QUESTIONNAIRE_AND_FALLBACK) },
                    modifier = Modifier
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("edi_bank_to_questions")
                ) {
                    Text("Evaluar", fontWeight = FontWeight.Bold)
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Gestational Age (Weeks + Days) & Evaluation Date Controls
            ClinicalSectionCard {
                Text(
                    text = "Edad Gestacional y Fecha de Evaluación",
                    color = DarkText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Prematuridad (< 37 sem)",
                            color = DarkText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "EG registrada: ${state.ediGestationalAgeInput.originalLabel}",
                            color = SlateText,
                            fontSize = 11.sp
                        )
                    }
                    Switch(
                        checked = state.esPrematuro,
                        onCheckedChange = onTogglePrematurez,
                        modifier = Modifier.testTag("edi_prematurity_switch")
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Weeks stepper
                    StepperBox(
                        label = "Semanas EG",
                        valueText = "${state.egSemanas} sem",
                        onMinus = { onAdjustEgSemanas(-1) },
                        onPlus = { onAdjustEgSemanas(1) },
                        minusTag = "edi_ga_weeks_minus",
                        plusTag = "edi_ga_weeks_plus",
                        modifier = Modifier.weight(1f)
                    )
                    // Days stepper (0..6, no rounding)
                    StepperBox(
                        label = "Días EG (0–6)",
                        valueText = "${state.egDias} d",
                        onMinus = { onAdjustEgDias(-1) },
                        onPlus = { onAdjustEgDias(1) },
                        minusTag = "edi_ga_days_minus",
                        plusTag = "edi_ga_days_plus",
                        modifier = Modifier.weight(1f)
                    )
                }

                if (state.esPrematuro) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = state.ediGestationalAgeInput.formulaDocumentation,
                        color = EdiBlueAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = state.ediFechaEval,
                        onValueChange = onUpdateEdiFechaEval,
                        label = { Text("Fecha evaluación (dd/MM/yyyy)") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("edi_eval_date_input")
                    )
                    OutlinedButton(
                        onClick = { onAdjustEdiFechaEvalYear(-1) },
                        modifier = Modifier
                            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                            .testTag("edi_eval_year_minus"),
                        contentPadding = PaddingValues(horizontal = 10.dp)
                    ) {
                        Text("-1a", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = { onAdjustEdiFechaEvalYear(1) },
                        modifier = Modifier
                            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                            .testTag("edi_eval_year_plus"),
                        contentPadding = PaddingValues(horizontal = 10.dp)
                    ) {
                        Text("+1a", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Comparison: Manual EDI vs Calendario CRED
            EdiAgeComparisonCard(
                state = state,
                comparison = comparison,
                onUseAgeSuggestedGroup = onUseAgeSuggestedGroup,
                onConfirmAgeDiscrepancyResolution = onConfirmAgeDiscrepancyResolution
            )

            // Search & Filter
            OutlinedTextField(
                value = state.ediGroupSearchQuery,
                onValueChange = onSearchQueryChange,
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                placeholder = { Text("Buscar grupo (ej. Grupo 8, 13, 37m...)") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("edi_bank_search_input"),
                shape = RoundedCornerShape(14.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                EdiGroupFilterCategory.entries.forEach { category ->
                    FilterChip(
                        selected = state.ediGroupFilterCategory == category,
                        onClick = { onSelectGroupFilter(category) },
                        label = { Text(category.label, fontSize = 12.sp) },
                        modifier = Modifier
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("edi_bank_filter_${category.name}"),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EdiNavy,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            // 15 Official Groups List
            filteredGroups.forEach { group ->
                val isActive = group.groupNumber == state.ediEffectiveGroupNumber
                val isSuggested = comparison.automaticallyAssignedGroup?.groupNumber == group.groupNumber
                ClinicalSectionCard(
                    backgroundColor = if (isActive) EdiSoftBlueBg else Color.White,
                    borderColor = if (isActive) EdiBlueAccent else Color(0xFFE2E8F0),
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { onSelectGroupFromBank(group.groupNumber) }
                        .testTag("edi_bank_group_${group.groupNumber}")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(if (isActive) EdiNavy else SoftGrayBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = group.code,
                                    color = if (isActive) Color.White else DarkText,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                            Column {
                                Text(
                                    text = group.officialTitle,
                                    color = DarkText,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = "Intervalo: ${group.shortIntervalLabel}",
                                    color = SlateText,
                                    fontSize = 11.sp
                                )
                            }
                        }
                        if (isActive) {
                            PillBadge(
                                text = "ACTIVO",
                                backgroundColor = EdiNavy,
                                textColor = Color.White,
                                fontSizeSp = 10
                            )
                        } else {
                            OutlinedButton(
                                onClick = { onSelectGroupFromBank(group.groupNumber) },
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 48.dp)
                                    .testTag("edi_bank_select_${group.groupNumber}"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Usar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        PillBadge(
                            text = "${group.itemsPerActiveArea} react./área",
                            backgroundColor = SoftGrayBg,
                            textColor = SlateText,
                            fontSizeSp = 10
                        )
                        PillBadge(
                            text = "${group.aleItems.size} ALE",
                            backgroundColor = SoftGrayBg,
                            textColor = SlateText,
                            fontSizeSp = 10
                        )
                        PillBadge(
                            text = if (group.hasAlarmSignals) "${group.alaItems.size} ALA" else "Sin ALA",
                            backgroundColor = SoftGrayBg,
                            textColor = SlateText,
                            fontSizeSp = 10
                        )
                        PillBadge(
                            text = if (group.conocimientoApplies) "Incluye CO" else "Sin CO",
                            backgroundColor = if (group.conocimientoApplies) SoftGreenPill else SoftGrayBg,
                            textColor = if (group.conocimientoApplies) DarkGreenPill else SlateText,
                            fontSizeSp = 10
                        )
                        val fallbackLabel = when {
                            group.groupNumber == 1 -> "Sin retroceso"
                            group.groupNumber == 8 -> "Retroceso G7 (2)"
                            group.groupNumber == 13 -> "Retroceso G12 + CO-R"
                            else -> "Retroceso G${group.fallbackPreviousGroupNumber} (${group.fallbackItemsCountPerArea})"
                        }
                        PillBadge(
                            text = fallbackLabel,
                            backgroundColor = SoftAmberBg,
                            textColor = WarningText,
                            fontSizeSp = 10
                        )
                        if (isSuggested) {
                            PillBadge(
                                text = "Sugerido por edad",
                                backgroundColor = SoftGreenPill,
                                textColor = DarkGreenPill,
                                fontSizeSp = 10
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StepperBox(
    label: String,
    valueText: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    minusTag: String,
    plusTag: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = SoftGrayBg,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(text = label, color = SlateText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onMinus,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag(minusTag)
                ) {
                    Icon(Icons.Filled.Remove, contentDescription = "Disminuir $label", tint = DarkText)
                }
                Text(
                    text = valueText,
                    color = DarkText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                IconButton(
                    onClick = onPlus,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag(plusTag)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Aumentar $label", tint = DarkText)
                }
            }
        }
    }
}

@Composable
private fun EdiAgeComparisonCard(
    state: CredEdiUiState,
    comparison: EdiCorrectedAgeComparison,
    onUseAgeSuggestedGroup: () -> Unit,
    onConfirmAgeDiscrepancyResolution: (Int, String) -> Unit
) {
    val isDiscrepancy = comparison.autoSelectionStatus ==
        EdiGroupAutoSelectionStatus.BLOCKED_GROUP_DISCREPANCY_REQUIRES_PROFESSIONAL_REVIEW
    val isOutOfRange = state.ediIsOutOfOfficialAgeRange

    val bg = when {
        isDiscrepancy && !state.ediIsDiscrepancyResolvedForCurrentInputs -> SoftAmberBg
        isOutOfRange -> SoftRedBg
        comparison.autoSelectionStatus == EdiGroupAutoSelectionStatus.AUTO_SELECTED_CONSISTENT -> SoftGreenBg
        else -> Color.White
    }
    val border = when {
        isDiscrepancy && !state.ediIsDiscrepancyResolvedForCurrentInputs -> BorderAmber
        isOutOfRange -> BorderSoftRed
        comparison.autoSelectionStatus == EdiGroupAutoSelectionStatus.AUTO_SELECTED_CONSISTENT -> BorderSoftGreen
        else -> Color(0xFFE2E8F0)
    }

    ClinicalSectionCard(
        backgroundColor = bg,
        borderColor = border,
        modifier = Modifier.testTag("edi_age_comparison_card")
    ) {
        Text(
            text = "Comparación Aislada de Edad Corregida (Manual EDI vs CRED)",
            color = DarkText,
            fontSize = 13.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Método Manual EDI (30d/12m)", color = SlateText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = "${comparison.manualMethodResult.effectiveAgeUsed.formattedMonthsDays} -> ${comparison.manualMethodResult.assignedGroup?.code ?: "Fuera de rango"}",
                    color = DarkText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Método Calendario CRED", color = SlateText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = "${comparison.calendarCredMethodResult.effectiveAgeUsed.formattedMonthsDays} -> ${comparison.calendarCredMethodResult.assignedGroup?.code ?: "Fuera de rango"}",
                    color = DarkText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = comparison.clinicalReviewMessage,
            color = DarkText,
            fontSize = 12.sp
        )

        if (state.ediManualGroupOverride && comparison.automaticallyAssignedGroup != null) {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = onUseAgeSuggestedGroup,
                modifier = Modifier
                    .defaultMinSize(minHeight = 48.dp)
                    .testTag("edi_btn_reset_to_age_group")
            ) {
                Text("Usar grupo sugerido por edad (${comparison.automaticallyAssignedGroup.code})")
            }
        }

        if (isDiscrepancy) {
            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = BorderAmber)
            Spacer(modifier = Modifier.height(10.dp))

            if (state.ediIsDiscrepancyResolvedForCurrentInputs) {
                val res = state.ediAgeDiscrepancyResolution!!
                PillBadge(
                    text = "Discrepancia resuelta: G${res.selectedGroupNumber} · ${res.clinicalJustificationNote}",
                    backgroundColor = SoftGreenPill,
                    textColor = DarkGreenPill,
                    fontSizeSp = 11
                )
            } else {
                var selectedCandidate by remember(comparison.clinicalReviewMessage) {
                    mutableStateOf(
                        comparison.candidateGroupsForProfessionalReview.firstOrNull()?.groupNumber
                    )
                }
                var justificationNote by remember(comparison.clinicalReviewMessage) {
                    mutableStateOf("")
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edi_discrepancy_panel"),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Revisión profesional obligatoria: seleccione el grupo y documente el criterio clínico",
                        color = WarningText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        comparison.candidateGroupsForProfessionalReview.forEach { candidate ->
                            val selected = selectedCandidate == candidate.groupNumber
                            FilterChip(
                                selected = selected,
                                onClick = { selectedCandidate = candidate.groupNumber },
                                label = { Text("${candidate.code} (${candidate.shortIntervalLabel})", fontSize = 12.sp) },
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 48.dp)
                                    .testTag("edi_discrepancy_candidate_G${candidate.groupNumber}")
                            )
                        }
                    }
                    OutlinedTextField(
                        value = justificationNote,
                        onValueChange = { justificationNote = it },
                        label = { Text("Justificación clínica de la selección") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edi_discrepancy_note_input")
                    )
                    Button(
                        onClick = {
                            selectedCandidate?.let { g ->
                                onConfirmAgeDiscrepancyResolution(g, justificationNote)
                            }
                        },
                        enabled = selectedCandidate != null && justificationNote.trim().isNotEmpty(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("edi_discrepancy_confirm_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = EdiNavy)
                    ) {
                        Text("Confirmar grupo y documentar criterio", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun EdiQuestionnaireAndFallbackView(
    state: CredEdiUiState,
    onBack: () -> Unit,
    onNavigateView: (EdiInternalView) -> Unit,
    onSelectAxis: (EdiAxis) -> Unit,
    onSelectDevAreaFilter: (EdiDevArea?) -> Unit,
    onRecordCurrentItemResponse: (String, EdiItemResponse) -> Unit,
    onRecordDualEvidence: (String, Boolean?, Boolean?, Boolean?, String) -> Unit,
    onRecordFallbackResponse: (String, EdiItemResponse) -> Unit,
    onRecordFallbackDualEvidence: (String, Boolean?, Boolean?, Boolean?, String) -> Unit,
    onUpdateItemClinicalNote: (String, String) -> Unit
) {
    val groupSpec = state.ediActiveGroupSpec
    val globalEval = state.ediGlobalEvaluation
    val session = state.ediEngineState.activeSession

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("edi_view_questionnaire")
    ) {
        // Header bar
        Surface(color = Color.White, shadowElevation = 1.dp) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("edi_q_back")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver al resumen",
                            tint = DarkText
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${groupSpec.code} · Registro Individual",
                            color = DarkText,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = groupSpec.officialTitle,
                            color = SlateText,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    TextButton(
                        onClick = { onNavigateView(EdiInternalView.RESULTS_TRACEABILITY) },
                        modifier = Modifier
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("edi_q_to_results")
                    ) {
                        Text("Resultados", fontWeight = FontWeight.Bold)
                    }
                }

                // 5-Axis Selector Strip
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    EdiAxis.entries.forEach { axis ->
                        val selected = state.ediSelectedAxis == axis
                        val label = when (axis) {
                            EdiAxis.FRB -> "1. FRB"
                            EdiAxis.ALE -> "2. ALE"
                            EdiAxis.AREAS -> "3. Desarrollo"
                            EdiAxis.EN -> "4. Neurológica"
                            EdiAxis.ALA -> "5. Alarma"
                        }
                        FilterChip(
                            selected = selected,
                            onClick = { onSelectAxis(axis) },
                            label = { Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                            modifier = Modifier
                                .defaultMinSize(minHeight = 48.dp)
                                .testTag("edi_axis_tab_${axis.name}"),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EdiNavy,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (globalEval.hasPriorityCriticalWarning) {
                EdiPriorityWarningBanner(globalEval = globalEval)
            }

            when (state.ediSelectedAxis) {
                EdiAxis.FRB -> {
                    ClinicalSectionCard(
                        backgroundColor = if (globalEval.frbMandatoryForGroup) EdiSoftBlueBg else SoftGrayBg,
                        borderColor = if (globalEval.frbMandatoryForGroup) EdiSoftBlueBorder else Color(0xFFE2E8F0)
                    ) {
                        Text(
                            text = if (globalEval.frbMandatoryForGroup) {
                                "Eje 1 (FRB) — Obligatorio en ${groupSpec.code} (Grupos 1–4): modifica la calificación global."
                            } else {
                                "Eje 1 (FRB) — Opcional en ${groupSpec.code} (Grupos 5–15): se registra para trazabilidad clínica y no bloquea el semáforo global."
                            },
                            color = DarkText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    EdiOfficialCatalog.frbItems.forEach { item ->
                        key(item.uniqueId) {
                            OfficialItemCard(
                                item = item,
                                effectiveUniqueId = item.uniqueId,
                                response = globalEval.frbResponsesResolved[item.uniqueId] ?: EdiItemResponse.UNANSWERED,
                                dualRecord = session.dualEvidenceRecords[item.uniqueId],
                                clinicalNote = session.itemClinicalNotes[item.uniqueId].orEmpty(),
                                isFallback = false,
                                onRecordResponse = { resp -> onRecordCurrentItemResponse(item.uniqueId, resp) },
                                onRecordDualEvidence = { obs, ent, res, note ->
                                    onRecordDualEvidence(item.uniqueId, obs, ent, res, note)
                                },
                                onUpdateNote = { note -> onUpdateItemClinicalNote(item.uniqueId, note) }
                            )
                        }
                    }
                }

                EdiAxis.ALE -> {
                    ClinicalSectionCard(
                        backgroundColor = if (globalEval.aleMandatoryForGroup) EdiSoftBlueBg else SoftGrayBg,
                        borderColor = if (globalEval.aleMandatoryForGroup) EdiSoftBlueBorder else Color(0xFFE2E8F0)
                    ) {
                        Text(
                            text = if (globalEval.aleMandatoryForGroup) {
                                "Eje 2 (ALE) — Obligatorio en ${groupSpec.code} (Grupos 1–4): modifica la calificación global."
                            } else {
                                "Eje 2 (ALE) — Opcional en ${groupSpec.code} (Grupos 5–15): no bloquea el semáforo global si queda pendiente."
                            },
                            color = DarkText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    groupSpec.aleItems.forEach { item ->
                        key(item.uniqueId) {
                            OfficialItemCard(
                                item = item,
                                effectiveUniqueId = item.uniqueId,
                                response = globalEval.aleResponsesResolved[item.uniqueId] ?: EdiItemResponse.UNANSWERED,
                                dualRecord = session.dualEvidenceRecords[item.uniqueId],
                                clinicalNote = session.itemClinicalNotes[item.uniqueId].orEmpty(),
                                isFallback = false,
                                onRecordResponse = { resp -> onRecordCurrentItemResponse(item.uniqueId, resp) },
                                onRecordDualEvidence = { obs, ent, res, note ->
                                    onRecordDualEvidence(item.uniqueId, obs, ent, res, note)
                                },
                                onUpdateNote = { note -> onUpdateItemClinicalNote(item.uniqueId, note) }
                            )
                        }
                    }
                }

                EdiAxis.AREAS -> {
                    // Optional Dev Area Filter Pills
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = state.ediSelectedDevAreaFilter == null,
                            onClick = { onSelectDevAreaFilter(null) },
                            label = { Text("Todas (5)", fontSize = 12.sp) },
                            modifier = Modifier
                                .defaultMinSize(minHeight = 48.dp)
                                .testTag("edi_area_filter_ALL")
                        )
                        EdiDevArea.entries.forEach { area ->
                            FilterChip(
                                selected = state.ediSelectedDevAreaFilter == area,
                                onClick = { onSelectDevAreaFilter(area) },
                                label = { Text(area.code, fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 48.dp)
                                    .testTag("edi_area_filter_${area.code}")
                            )
                        }
                    }

                    val areasToShow = state.ediSelectedDevAreaFilter?.let { listOf(it) } ?: EdiDevArea.entries
                    areasToShow.forEach { area ->
                        val areaEval = globalEval.axis3AreasSummary.areaResults[area]
                        if (areaEval != null) {
                            EdiAreaSectionBlock(
                                groupSpec = groupSpec,
                                areaEval = areaEval,
                                dualEvidenceRecords = session.dualEvidenceRecords,
                                clinicalNotes = session.itemClinicalNotes,
                                onRecordCurrentItemResponse = onRecordCurrentItemResponse,
                                onRecordDualEvidence = onRecordDualEvidence,
                                onRecordFallbackResponse = onRecordFallbackResponse,
                                onRecordFallbackDualEvidence = onRecordFallbackDualEvidence,
                                onUpdateItemClinicalNote = onUpdateItemClinicalNote
                            )
                        }
                    }
                }

                EdiAxis.EN -> {
                    val enItems = EdiOfficialCatalog.getApplicableEnItems(groupSpec.groupNumber)
                    if (groupSpec.groupNumber == 15) {
                        ClinicalSectionCard(
                            backgroundColor = SoftGrayBg,
                            modifier = Modifier.testTag("edi_en13_not_applicable_g15")
                        ) {
                            Text(
                                text = "EN 1.3 (Perímetro cefálico) — No aplica en el Grupo 15",
                                color = DarkText,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "Según las páginas 47 y 111 del Manual EDI 2026, en el Grupo 15 (60 a 71 meses 29 días) únicamente aplican EN 1.1 y EN 1.2.",
                                color = SlateText,
                                fontSize = 12.sp
                            )
                        }
                    }
                    enItems.forEach { item ->
                        key(item.uniqueId) {
                            OfficialItemCard(
                                item = item,
                                effectiveUniqueId = item.uniqueId,
                                response = globalEval.enResponsesResolved[item.uniqueId] ?: EdiItemResponse.UNANSWERED,
                                dualRecord = session.dualEvidenceRecords[item.uniqueId],
                                clinicalNote = session.itemClinicalNotes[item.uniqueId].orEmpty(),
                                isFallback = false,
                                onRecordResponse = { resp -> onRecordCurrentItemResponse(item.uniqueId, resp) },
                                onRecordDualEvidence = { obs, ent, res, note ->
                                    onRecordDualEvidence(item.uniqueId, obs, ent, res, note)
                                },
                                onUpdateNote = { note -> onUpdateItemClinicalNote(item.uniqueId, note) }
                            )
                        }
                    }
                }

                EdiAxis.ALA -> {
                    if (!groupSpec.hasAlarmSignals) {
                        ClinicalSectionCard(
                            backgroundColor = SoftGrayBg,
                            modifier = Modifier.testTag("edi_ala_not_applicable_g1")
                        ) {
                            Text(
                                text = "Señales de Alarma (ALA) — No aplica en el Grupo 1",
                                color = DarkText,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "En el Grupo 1 (1m a 1m 29d) la evaluación oficial contempla 4 ejes (FRB, ALE, Áreas del Desarrollo y EN) y no exige Señales de Alarma ALA.",
                                color = SlateText,
                                fontSize = 12.sp
                            )
                        }
                    } else {
                        groupSpec.alaItems.forEach { item ->
                            key(item.uniqueId) {
                                OfficialItemCard(
                                    item = item,
                                    effectiveUniqueId = item.uniqueId,
                                    response = globalEval.alaResponsesResolved[item.uniqueId] ?: EdiItemResponse.UNANSWERED,
                                    dualRecord = session.dualEvidenceRecords[item.uniqueId],
                                    clinicalNote = session.itemClinicalNotes[item.uniqueId].orEmpty(),
                                    isFallback = false,
                                    onRecordResponse = { resp -> onRecordCurrentItemResponse(item.uniqueId, resp) },
                                    onRecordDualEvidence = { obs, ent, res, note ->
                                        onRecordDualEvidence(item.uniqueId, obs, ent, res, note)
                                    },
                                    onUpdateNote = { note -> onUpdateItemClinicalNote(item.uniqueId, note) }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Button(
                onClick = { onNavigateView(EdiInternalView.RESULTS_TRACEABILITY) },
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 52.dp)
                    .testTag("edi_q_btn_finish_results"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EdiNavy)
            ) {
                Icon(Icons.Outlined.Assessment, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Ver Resultados y Semáforo EDI", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}

@Composable
private fun EdiAreaSectionBlock(
    groupSpec: EdiGroupSpec,
    areaEval: EdiAreaEvaluationResult,
    dualEvidenceRecords: Map<String, EdiDualEvidenceRecord>,
    clinicalNotes: Map<String, String>,
    onRecordCurrentItemResponse: (String, EdiItemResponse) -> Unit,
    onRecordDualEvidence: (String, Boolean?, Boolean?, Boolean?, String) -> Unit,
    onRecordFallbackResponse: (String, EdiItemResponse) -> Unit,
    onRecordFallbackDualEvidence: (String, Boolean?, Boolean?, Boolean?, String) -> Unit,
    onUpdateItemClinicalNote: (String, String) -> Unit
) {
    val (statusBg, statusFg) = areaStatusColors(areaEval.status)

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ClinicalSectionCard(
            backgroundColor = Color.White,
            modifier = Modifier.testTag("edi_area_header_${areaEval.area.code}")
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${areaEval.area.officialTitle} (${areaEval.area.code})",
                    color = DarkText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                PillBadge(
                    text = areaEval.status.displayLabel,
                    backgroundColor = statusBg,
                    textColor = statusFg,
                    fontSizeSp = 11
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = areaEval.clinicalRuleSummary,
                color = SlateText,
                fontSize = 12.sp
            )
        }

        if (!areaEval.appliesToGroup) {
            ClinicalSectionCard(
                backgroundColor = SoftGrayBg,
                modifier = Modifier.testTag("edi_area_not_applicable_${areaEval.area.code}")
            ) {
                Text(
                    text = "Área ${areaEval.area.officialTitle} (${areaEval.area.code}) — No aplica en ${groupSpec.code} (Grupos 1–12)",
                    color = SlateText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            return
        }

        areaEval.currentItems.forEach { item ->
            key(item.uniqueId) {
                OfficialItemCard(
                    item = item,
                    effectiveUniqueId = item.uniqueId,
                    response = areaEval.currentItemResponses[item.uniqueId] ?: EdiItemResponse.UNANSWERED,
                    dualRecord = dualEvidenceRecords[item.uniqueId],
                    clinicalNote = clinicalNotes[item.uniqueId].orEmpty(),
                    isFallback = false,
                    onRecordResponse = { resp -> onRecordCurrentItemResponse(item.uniqueId, resp) },
                    onRecordDualEvidence = { obs, ent, res, note ->
                        onRecordDualEvidence(item.uniqueId, obs, ent, res, note)
                    },
                    onUpdateNote = { note -> onUpdateItemClinicalNote(item.uniqueId, note) }
                )
            }
        }

        if (areaEval.fallbackIgnoredDueToCurrentChange && !areaEval.fallbackRequired) {
            ClinicalSectionCard(
                backgroundColor = SoftGrayBg,
                modifier = Modifier.testTag("edi_fallback_ignored_note_${areaEval.area.code}")
            ) {
                Text(
                    text = "Historial de retroceso conservado en auditoría e ignorado al cambiar respuesta actual.",
                    color = SlateText,
                    fontSize = 11.sp
                )
            }
        }

        // Conditional Fallback Section (ONLY when triggered by fallbackRequired == true)
        if (areaEval.fallbackRequired) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("edi_fallback_section_${areaEval.area.code}"),
                shape = RoundedCornerShape(20.dp),
                color = SoftAmberBg,
                border = BorderStroke(1.5.dp, BorderAmber)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val fbTitle = when {
                        areaEval.usesSpecialCoRFallback ->
                            "RETROCESO ESPECIAL ACTIVADO · Reactivos CO-R 13.1–13.3"
                        groupSpec.groupNumber == 8 ->
                            "RETROCESO EXCEPCIONAL (Grupo 8 -> 2 reactivos del Grupo 7)"
                        else ->
                            "RETROCESO ACTIVADO -> Grupo ${areaEval.fallbackSourceGroupNumber} (${areaEval.fallbackItems.size} reactivos)"
                    }
                    Text(
                        text = fbTitle,
                        color = WarningText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "0 SÍ en ${areaEval.area.code} del grupo actual. Evalúe los reactivos de retroceso (almacenados de forma independiente).",
                        color = DarkText,
                        fontSize = 11.sp
                    )

                    areaEval.fallbackItems.forEach { fbSpec ->
                        key(fbSpec.fallbackUniqueId) {
                            OfficialItemCard(
                                item = fbSpec.sourceItem,
                                effectiveUniqueId = fbSpec.fallbackUniqueId,
                                response = areaEval.activeFallbackResponses[fbSpec.fallbackUniqueId]
                                    ?: EdiItemResponse.UNANSWERED,
                                dualRecord = dualEvidenceRecords[fbSpec.fallbackUniqueId],
                                clinicalNote = clinicalNotes[fbSpec.fallbackUniqueId].orEmpty(),
                                isFallback = true,
                                onRecordResponse = { resp ->
                                    onRecordFallbackResponse(fbSpec.fallbackUniqueId, resp)
                                },
                                onRecordDualEvidence = { obs, ent, res, note ->
                                    onRecordFallbackDualEvidence(fbSpec.fallbackUniqueId, obs, ent, res, note)
                                },
                                onUpdateNote = { note -> onUpdateItemClinicalNote(fbSpec.fallbackUniqueId, note) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OfficialItemCard(
    item: EdiCatalogItem,
    effectiveUniqueId: String,
    response: EdiItemResponse,
    dualRecord: EdiDualEvidenceRecord?,
    clinicalNote: String,
    isFallback: Boolean,
    onRecordResponse: (EdiItemResponse) -> Unit,
    onRecordDualEvidence: (Boolean?, Boolean?, Boolean?, String) -> Unit,
    onUpdateNote: (String) -> Unit
) {
    var showProtocol by remember(effectiveUniqueId) { mutableStateOf(false) }
    var showNoteField by remember(effectiveUniqueId) { mutableStateOf(clinicalNote.isNotBlank()) }

    val cardBorder = when (response) {
        EdiItemResponse.YES -> if (item.yesIsNormalGreen) BorderSoftGreen else BorderAmber
        EdiItemResponse.NO -> if (item.yesIsNormalGreen) BorderAmber else BorderSoftGreen
        EdiItemResponse.DISCREPANCY_REQUIRES_HUMAN_RESOLUTION -> AlertRed
        EdiItemResponse.NOT_EVALUATED_MISSING_MATERIAL -> BorderAmber
        EdiItemResponse.UNANSWERED -> Color(0xFFE2E8F0)
    }

    ClinicalSectionCard(
        borderColor = cardBorder,
        backgroundColor = Color.White,
        modifier = Modifier.testTag("edi_item_card_$effectiveUniqueId")
    ) {
        // Item header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f)
            ) {
                PillBadge(
                    text = if (isFallback) "${item.itemCode} (Retroceso)" else item.itemCode,
                    backgroundColor = EdiNavy,
                    textColor = Color.White,
                    fontSizeSp = 11
                )
                val modalityLabel = when (item.modality) {
                    EdiClinicalModality.DIRECTED_INTERVIEW -> "Entrevista"
                    EdiClinicalModality.DIRECT_OBSERVATION -> "Observación (**)"
                    EdiClinicalModality.DUAL_OBSERVATION_AND_INTERVIEW_ALE_12_3 -> "Obs + Entrevista"
                    EdiClinicalModality.INTERVIEW_PRIMARY_WITH_OBSERVATION_COMPLEMENT -> "Entrevista + Obs"
                }
                PillBadge(
                    text = modalityLabel,
                    backgroundColor = EdiSoftBlueBg,
                    textColor = EdiBlueAccent,
                    fontSizeSp = 10
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            EdiItemResponseBadge(response = response, yesIsNormalGreen = item.yesIsNormalGreen)
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = item.consultationQuestionText,
            color = DarkText,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )

        // Official Materials & Plates Badges
        if (item.protocolRequiredPlates.isNotEmpty() ||
            item.protocolRequiredMaterials.isNotEmpty() ||
            item.isListedWithHeaderMaterialOnly()
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                item.protocolRequiredPlates.forEach { plate ->
                    PillBadge(
                        text = "${plate.title} (pág. ${plate.manualPage})",
                        backgroundColor = SoftAmberBg,
                        textColor = WarningText,
                        borderColor = BorderAmber,
                        fontSizeSp = 10
                    )
                }
                item.protocolRequiredMaterials.forEach { mat ->
                    PillBadge(
                        text = "Material: $mat",
                        backgroundColor = SoftGrayBg,
                        textColor = SlateText,
                        fontSizeSp = 10
                    )
                }
                if (item.isListedWithHeaderMaterialOnly()) {
                    PillBadge(
                        text = "Protocolo clínico sin lámina física (observación del cuidador)",
                        backgroundColor = SoftGreenBg,
                        textColor = DarkGreenPill,
                        fontSizeSp = 10
                    )
                }
            }
        }

        // Expandable MINSA Clinical Instruction + Note toggle
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(
                onClick = { showProtocol = !showProtocol },
                modifier = Modifier
                    .defaultMinSize(minHeight = 48.dp)
                    .testTag("edi_protocol_toggle_$effectiveUniqueId"),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                Icon(
                    imageVector = if (showProtocol) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (showProtocol) "Ocultar instrucción" else "Instrucción clínica",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            TextButton(
                onClick = { showNoteField = !showNoteField },
                modifier = Modifier
                    .defaultMinSize(minHeight = 48.dp)
                    .testTag("edi_note_toggle_$effectiveUniqueId"),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Description,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (clinicalNote.isNotBlank()) "Nota guardada" else "Nota clínica",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        AnimatedVisibility(visible = showProtocol) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                shape = RoundedCornerShape(12.dp),
                color = SoftGrayBg
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = item.modality.description,
                        color = EdiBlueAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = item.clinicalInstruction,
                        color = DarkText,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Dual Modality Controls for ALE 12.3, LE 13.3, LE 14.3
        val isDualModality = item.modality == EdiClinicalModality.DUAL_OBSERVATION_AND_INTERVIEW_ALE_12_3 ||
            item.modality == EdiClinicalModality.INTERVIEW_PRIMARY_WITH_OBSERVATION_COMPLEMENT

        if (isDualModality) {
            val obsVal = dualRecord?.directObservationEvidence
            val intVal = dualRecord?.directedInterviewEvidence
            val resVal = dualRecord?.professionalHumanResolution
            val existingNote = dualRecord?.clinicalJustificationNote ?: ""

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("edi_dual_panel_$effectiveUniqueId"),
                shape = RoundedCornerShape(14.dp),
                color = EdiSoftBlueBg,
                border = BorderStroke(1.dp, EdiSoftBlueBorder)
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (item.modality == EdiClinicalModality.DUAL_OBSERVATION_AND_INTERVIEW_ALE_12_3) {
                            "Protocolo dual obligatorio: Observación directa + Entrevista"
                        } else {
                            "Protocolo entrevista principal + Observación complementaria"
                        },
                        color = EdiBlueAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold
                    )

                    // Observation Row
                    DualEvidenceRow(
                        label = "Observación directa",
                        currentValue = obsVal,
                        yesTag = "edi_dual_obs_yes_$effectiveUniqueId",
                        noTag = "edi_dual_obs_no_$effectiveUniqueId",
                        onSelect = { newObs ->
                            onRecordDualEvidence(newObs, intVal, null, existingNote)
                        }
                    )

                    // Interview Row
                    DualEvidenceRow(
                        label = "Entrevista dirigida",
                        currentValue = intVal,
                        yesTag = "edi_dual_int_yes_$effectiveUniqueId",
                        noTag = "edi_dual_int_no_$effectiveUniqueId",
                        onSelect = { newInt ->
                            onRecordDualEvidence(obsVal, newInt, null, existingNote)
                        }
                    )

                    if (dualRecord?.hasConflictBetweenEvidences == true) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("edi_dual_conflict_$effectiveUniqueId"),
                            shape = RoundedCornerShape(12.dp),
                            color = SoftRedBg,
                            border = BorderStroke(1.dp, BorderSoftRed)
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Discrepancia entre observación y entrevista: requiere resolución humana",
                                    color = DarkRedText,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = {
                                            onRecordDualEvidence(obsVal, intVal, true, existingNote)
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .defaultMinSize(minHeight = 48.dp)
                                            .testTag("edi_dual_resolve_yes_$effectiveUniqueId"),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (resVal == true) ClinicalGreen else EdiNavy
                                        )
                                    ) {
                                        Text("Resolver SÍ", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Button(
                                        onClick = {
                                            onRecordDualEvidence(obsVal, intVal, false, existingNote)
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .defaultMinSize(minHeight = 48.dp)
                                            .testTag("edi_dual_resolve_no_$effectiveUniqueId"),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (resVal == false) AlertRed else EdiNavy
                                        )
                                    ) {
                                        Text("Resolver NO", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onRecordResponse(EdiItemResponse.UNANSWERED) },
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 48.dp)
                                .testTag("edi_resp_unanswered_$effectiveUniqueId")
                        ) {
                            Text("Sin responder", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        if (item.requiresMaterialInProtocol) {
                            OutlinedButton(
                                onClick = {
                                    onRecordResponse(EdiItemResponse.NOT_EVALUATED_MISSING_MATERIAL)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .defaultMinSize(minHeight = 48.dp)
                                    .testTag("edi_resp_missing_mat_$effectiveUniqueId")
                            ) {
                                Text("Falta material", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        } else {
            // Standard Single-Modality Controls: SÍ / NO / SIN RESPONDER
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ResponseButton(
                    label = "SÍ",
                    selected = response == EdiItemResponse.YES,
                    selectedColor = if (item.yesIsNormalGreen) ClinicalGreen else WarningAmber,
                    testTag = "edi_resp_yes_$effectiveUniqueId",
                    onClick = { onRecordResponse(EdiItemResponse.YES) },
                    modifier = Modifier.weight(1f)
                )
                ResponseButton(
                    label = "NO",
                    selected = response == EdiItemResponse.NO,
                    selectedColor = if (item.yesIsNormalGreen) AlertRed else ClinicalGreen,
                    testTag = "edi_resp_no_$effectiveUniqueId",
                    onClick = { onRecordResponse(EdiItemResponse.NO) },
                    modifier = Modifier.weight(1f)
                )
                ResponseButton(
                    label = "Sin resp.",
                    selected = response == EdiItemResponse.UNANSWERED,
                    selectedColor = EdiNavy,
                    testTag = "edi_resp_unanswered_$effectiveUniqueId",
                    onClick = { onRecordResponse(EdiItemResponse.UNANSWERED) },
                    modifier = Modifier.weight(1f)
                )
            }

            if (item.requiresMaterialInProtocol) {
                Spacer(modifier = Modifier.height(6.dp))
                val missingSelected = response == EdiItemResponse.NOT_EVALUATED_MISSING_MATERIAL
                OutlinedButton(
                    onClick = {
                        onRecordResponse(EdiItemResponse.NOT_EVALUATED_MISSING_MATERIAL)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("edi_resp_missing_mat_$effectiveUniqueId"),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, if (missingSelected) WarningAmber else Color(0xFFCBD5E1)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (missingSelected) SoftAmberBg else Color.White,
                        contentColor = if (missingSelected) WarningText else SlateText
                    )
                ) {
                    Text(
                        text = "No evaluado (Falta material/lámina)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Per-item clinical note input (stored separately in itemClinicalNotes)
        AnimatedVisibility(visible = showNoteField) {
            OutlinedTextField(
                value = clinicalNote,
                onValueChange = onUpdateNote,
                label = { Text("Observación clínica del ítem ${item.itemCode}") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .testTag("edi_note_input_$effectiveUniqueId")
            )
        }
    }
}

@Composable
private fun DualEvidenceRow(
    label: String,
    currentValue: Boolean?,
    yesTag: String,
    noTag: String,
    onSelect: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = label,
            color = DarkText,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        ResponseButton(
            label = "SÍ",
            selected = currentValue == true,
            selectedColor = EdiNavy,
            testTag = yesTag,
            onClick = { onSelect(true) },
            modifier = Modifier.width(76.dp)
        )
        ResponseButton(
            label = "NO",
            selected = currentValue == false,
            selectedColor = EdiNavy,
            testTag = noTag,
            onClick = { onSelect(false) },
            modifier = Modifier.width(76.dp)
        )
    }
}

@Composable
private fun ResponseButton(
    label: String,
    selected: Boolean,
    selectedColor: Color,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag(testTag),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) selectedColor else SoftGrayBg,
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) selectedColor else Color(0xFFCBD5E1)
        )
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                color = if (selected) Color.White else DarkText,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun EdiItemResponseBadge(
    response: EdiItemResponse,
    yesIsNormalGreen: Boolean
) {
    val (bg, fg) = when (response) {
        EdiItemResponse.UNANSWERED -> SoftGrayBg to SlateText
        EdiItemResponse.YES -> if (yesIsNormalGreen) SoftGreenPill to DarkGreenPill else SoftAmberPill to WarningText
        EdiItemResponse.NO -> if (yesIsNormalGreen) SoftAmberPill to WarningText else SoftGreenPill to DarkGreenPill
        EdiItemResponse.NOT_EVALUATED_MISSING_MATERIAL -> SoftAmberPill to WarningText
        EdiItemResponse.DISCREPANCY_REQUIRES_HUMAN_RESOLUTION -> SoftRedBg to DarkRedText
    }
    PillBadge(
        text = response.displayLabel,
        backgroundColor = bg,
        textColor = fg,
        fontSizeSp = 10
    )
}

@Composable
private fun EdiResultsTraceabilityView(
    state: CredEdiUiState,
    onBack: () -> Unit,
    onNavigateView: (EdiInternalView) -> Unit
) {
    val groupSpec = state.ediActiveGroupSpec
    val globalEval = state.ediGlobalEvaluation
    val context = LocalContext.current
    var exportResult by remember(
        groupSpec.groupNumber,
        globalEval.trafficLight,
        state.ediEngineState.activeSession,
        state.fechaNacimiento,
        state.fechaEvaluacion,
        state.esPrematuro,
        state.egSemanas,
        state.egDias
    ) { mutableStateOf<ExportPdfResult?>(null) }
    var openPdfError by remember(exportResult) { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("edi_view_results")
    ) {
        Surface(color = Color.White, shadowElevation = 1.dp) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("edi_res_back")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver al resumen",
                        tint = DarkText
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Resultados EDI y Trazabilidad",
                        color = DarkText,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "${groupSpec.code} · ${groupSpec.officialTitle}",
                        color = SlateText,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                TextButton(
                    onClick = { onNavigateView(EdiInternalView.QUESTIONNAIRE_AND_FALLBACK) },
                    modifier = Modifier
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("edi_res_to_questions")
                ) {
                    Text("Editar", fontWeight = FontWeight.Bold)
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Priority Critical Alerts Banner (ALWAYS visible when hasPriorityCriticalWarning, even if INCOMPLETE)
            if (globalEval.hasPriorityCriticalWarning) {
                EdiPriorityWarningBanner(globalEval = globalEval)
            }

            // Global Semaphore Card (STRICTLY from EdiGlobalSemaphoreEngine, NEVER manual)
            val (bannerBg, bannerBorder, bannerFg) = when (globalEval.trafficLight) {
                EdiGlobalTrafficLight.VERDE -> Triple(SoftGreenBg, BorderGreen, DarkGreenPill)
                EdiGlobalTrafficLight.AMARILLO -> Triple(SoftAmberBg, WarningAmber, WarningText)
                EdiGlobalTrafficLight.ROJO -> Triple(SoftRedBg, AlertRed, DarkRedText)
                EdiGlobalTrafficLight.INCOMPLETE -> Triple(SoftGrayBg, Color(0xFF94A3B8), DarkText)
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("edi_global_semaphore_card"),
                shape = RoundedCornerShape(20.dp),
                color = bannerBg,
                border = BorderStroke(2.dp, bannerBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    PillBadge(
                        text = "SEMÁFORO CALCULADO · MOTOR EDI 2026",
                        backgroundColor = bannerFg.copy(alpha = 0.12f),
                        textColor = bannerFg,
                        fontSizeSp = 10
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = globalEval.trafficLight.displayLabel,
                        color = bannerFg,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.testTag("edi_global_semaphore_label")
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = globalEval.clinicalSummaryText,
                        color = DarkText,
                        fontSize = 13.sp
                    )
                }
            }

            // Child & Age Context
            ClinicalSectionCard {
                Text(
                    text = "Datos Clínicos y Edad Aplicada",
                    color = DarkText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Sexo: ${state.sexo.label} · EG: ${state.ediGestationalAgeInput.originalLabel}",
                    color = DarkText,
                    fontSize = 12.sp
                )
                Text(
                    text = "Edad Cronológica: ${if (state.ediHasFechaNacimientoValida) state.edadCronologicaAMD else "No ingresada"} · Edad Corregida: ${state.edadCorregidaAMD}",
                    color = SlateText,
                    fontSize = 12.sp
                )
                if (state.ediHasFechaNacimientoValida) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = state.ediCorrectedAgeComparison.clinicalReviewMessage,
                        color = SlateText,
                        fontSize = 11.sp
                    )
                }
                if (state.ediIsDiscrepancyResolvedForCurrentInputs) {
                    val res = state.ediAgeDiscrepancyResolution!!
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Criterio profesional documentado: G${res.selectedGroupNumber} — ${res.clinicalJustificationNote}",
                        color = ClinicalGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Blocking Reasons
            if (globalEval.blockingReasons.isNotEmpty()) {
                ClinicalSectionCard(
                    backgroundColor = SoftAmberBg,
                    borderColor = BorderAmber,
                    modifier = Modifier.testTag("edi_blocking_reasons_card")
                ) {
                    Text(
                        text = "Motivos de evaluación incompleta / bloqueo",
                        color = WarningText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    globalEval.blockingReasons.forEach { reason ->
                        Text(
                            text = "• $reason",
                            color = DarkText,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                    }
                }
            }

            // Non-blocking clinical warnings (Optional FRB/ALE in Groups 5–15)
            if (globalEval.nonBlockingClinicalWarnings.isNotEmpty()) {
                ClinicalSectionCard(
                    backgroundColor = EdiSoftBlueBg,
                    borderColor = EdiSoftBlueBorder,
                    modifier = Modifier.testTag("edi_non_blocking_warnings_card")
                ) {
                    Text(
                        text = "Advertencias clínicas no bloqueantes (FRB/ALE opcionales G5–G15)",
                        color = EdiBlueAccent,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    globalEval.nonBlockingClinicalWarnings.forEach { warning ->
                        Text(
                            text = "• $warning",
                            color = DarkText,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                    }
                }
            }

            // Causes Traceability
            ClinicalSectionCard(modifier = Modifier.testTag("edi_causes_card")) {
                Text(
                    text = "Trazabilidad de Causas Clínicas",
                    color = DarkText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                globalEval.causes.forEach { cause ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Verified,
                            contentDescription = null,
                            tint = EdiBlueAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = cause.description,
                                color = DarkText,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (cause.involvedCodes.isNotEmpty()) {
                                Text(
                                    text = "Códigos: ${cause.involvedCodes.joinToString(", ")}",
                                    color = SlateText,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }

            // 5-Axis Breakdown
            ClinicalSectionCard(modifier = Modifier.testTag("edi_axes_breakdown_card")) {
                Text(
                    text = "Desglose de los 5 Ejes (${groupSpec.code})",
                    color = DarkText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Eje 1 (FRB): ${globalEval.positiveFrbItems.size}/7 positivos" +
                        if (globalEval.positiveFrbItems.isNotEmpty()) {
                            " (${globalEval.positiveFrbItems.joinToString { it.itemCode }})"
                        } else "",
                    color = DarkText,
                    fontSize = 12.sp
                )
                Text(
                    text = "Eje 2 (ALE): ${globalEval.positiveAleItems.size}/${groupSpec.aleItems.size} positivas" +
                        if (globalEval.positiveAleItems.isNotEmpty()) {
                            " (${globalEval.positiveAleItems.joinToString { it.itemCode }})"
                        } else "",
                    color = DarkText,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text("Eje 3 (Áreas del Desarrollo):", color = DarkText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                globalEval.axis3AreasSummary.areaResults.values.forEach { areaRes ->
                    Text(
                        text = "• ${areaRes.area.code}: ${areaRes.status.displayLabel}" +
                            if (areaRes.fallbackRequired) " [Retroceso]" else "",
                        color = SlateText,
                        fontSize = 12.sp
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Eje 4 (EN): ${globalEval.positiveEnItems.size} alteración(es)" +
                        if (!globalEval.en13AppliesToGroup) " (EN 1.3 no aplica en G15)" else "",
                    color = DarkText,
                    fontSize = 12.sp
                )
                Text(
                    text = if (!globalEval.alaAppliesToGroup) {
                        "Eje 5 (ALA): No aplica en Grupo 1"
                    } else {
                        "Eje 5 (ALA): ${globalEval.positiveAlaItems.size}/${groupSpec.alaItems.size} positiva(s)"
                    },
                    color = DarkText,
                    fontSize = 12.sp
                )
            }

            // Exportación PDF EDI Peruano 2026 (Habilitada tras validación visual Fase 4B)
            val isDraftExport = !globalEval.isDefinitive || globalEval.trafficLight == EdiGlobalTrafficLight.INCOMPLETE
            ClinicalSectionCard(
                backgroundColor = if (isDraftExport) SoftAmberBg else Color.White,
                borderColor = if (isDraftExport) BorderAmber else EdiSoftBlueBorder,
                modifier = Modifier.testTag("edi_pdf_export_section")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = if (isDraftExport) Icons.Outlined.WarningAmber else Icons.Outlined.PictureAsPdf,
                        contentDescription = if (isDraftExport) {
                            "Exportación de Borrador EDI Incompleto"
                        } else {
                            "Exportación de Ficha Oficial EDI PDF"
                        },
                        tint = if (isDraftExport) WarningAmber else EdiBlueAccent,
                        modifier = Modifier.size(22.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isDraftExport) {
                                "Borrador EDI — Evaluación Incompleta (PDF)"
                            } else {
                                "Ficha Oficial EDI 2026 (PDF)"
                            },
                            color = if (isDraftExport) WarningText else DarkText,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isDraftExport) {
                                "Faltan ítems obligatorios o controles clínicos. Solo se emitirá un Borrador rotulado (sin dictamen definitivo ni semáforo) con el detalle de pendientes y alertas prioritarias."
                            } else {
                                "Genera la Ficha Oficial EDI Peruano 2026 (${groupSpec.code}) y su Anexo de Trazabilidad completo en Descargas sin sobrescribir exportaciones anteriores."
                            },
                            color = SlateText,
                            fontSize = 11.5.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        exportResult = EdiPdfExporter.exportarFichaOficialEdiPdf(context, state)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("edi_pdf_export_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDraftExport) WarningAmber else EdiBlueAccent,
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PictureAsPdf,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isDraftExport) {
                            "Exportar Borrador (Evaluación Incompleta) PDF"
                        } else {
                            "Exportar Ficha Oficial EDI (PDF)"
                        },
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                exportResult?.let { result ->
                    Spacer(modifier = Modifier.height(12.dp))
                    val resBg = when {
                        !result.success -> SoftRedBg
                        result.isDraft -> Color.White
                        else -> SoftGreenBg
                    }
                    val resBorder = when {
                        !result.success -> AlertRed
                        result.isDraft -> BorderAmber
                        else -> BorderGreen
                    }
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edi_pdf_export_result"),
                        shape = RoundedCornerShape(14.dp),
                        color = resBg,
                        border = BorderStroke(1.dp, resBorder)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            PillBadge(
                                text = when {
                                    !result.success -> "ERROR DE EXPORTACIÓN"
                                    result.isDraft -> "BORRADOR INCOMPLETO EXPORTADO"
                                    else -> "FICHA OFICIAL DEFINITIVA EXPORTADA"
                                },
                                backgroundColor = when {
                                    !result.success -> SoftRedBg
                                    result.isDraft -> SoftAmberPill
                                    else -> SoftGreenPill
                                },
                                textColor = when {
                                    !result.success -> DarkRedText
                                    result.isDraft -> WarningText
                                    else -> DarkGreenPill
                                },
                                fontSizeSp = 10
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = result.message,
                                color = DarkText,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            if (result.success && result.viewIntent != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = {
                                        try {
                                            val chooser = Intent.createChooser(
                                                result.viewIntent,
                                                "Abrir documento EDI PDF"
                                            ).apply {
                                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            }
                                            context.startActivity(chooser)
                                            openPdfError = null
                                        } catch (_: Exception) {
                                            openPdfError = "Archivo guardado en Descargas (${result.fileName ?: "PDF"}), pero no se encontró visor de PDF en el dispositivo."
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .defaultMinSize(minHeight = 48.dp)
                                        .testTag("edi_pdf_open_button"),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Description,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Abrir PDF generado",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            openPdfError?.let { err ->
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = err,
                                    color = WarningText,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EdiPriorityWarningBanner(
    globalEval: EdiGlobalEvaluationResult
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("edi_priority_warning_banner"),
        shape = RoundedCornerShape(18.dp),
        color = SoftRedBg,
        border = BorderStroke(1.5.dp, AlertRed)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.ReportProblem,
                    contentDescription = "Advertencia clínica prioritaria",
                    tint = AlertRed,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = "ADVERTENCIA CLÍNICA PRIORITARIA",
                    color = DarkRedText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            globalEval.priorityCriticalAlerts.forEach { alert ->
                Text(
                    text = "• ${alert.message}",
                    color = DarkRedText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun EdiTrafficLightPill(trafficLight: EdiGlobalTrafficLight) {
    val (bg, fg) = when (trafficLight) {
        EdiGlobalTrafficLight.VERDE -> SoftGreenPill to DarkGreenPill
        EdiGlobalTrafficLight.AMARILLO -> SoftAmberPill to WarningText
        EdiGlobalTrafficLight.ROJO -> SoftRedBg to DarkRedText
        EdiGlobalTrafficLight.INCOMPLETE -> SoftGrayBg to SlateText
    }
    PillBadge(
        text = when (trafficLight) {
            EdiGlobalTrafficLight.VERDE -> "VERDE"
            EdiGlobalTrafficLight.AMARILLO -> "AMARILLO"
            EdiGlobalTrafficLight.ROJO -> "ROJO"
            EdiGlobalTrafficLight.INCOMPLETE -> "INCOMPLETO"
        },
        backgroundColor = bg,
        textColor = fg,
        fontSizeSp = 11
    )
}

@Composable
private fun EdiGroupChangeConfirmationDialog(
    request: EdiPendingGroupChangeRequest,
    onResolve: (EdiGroupChangeResolutionAction) -> Unit
) {
    val recordedCount = request.preservedSnapshotOfPreviousGroup.currentGroupResponses.values
        .count { it != EdiItemResponse.UNANSWERED } +
        request.preservedSnapshotOfPreviousGroup.fallbackGroupResponses.values
            .count { it != EdiItemResponse.UNANSWERED } +
        request.preservedSnapshotOfPreviousGroup.frbResponses.values
            .count { it != EdiItemResponse.UNANSWERED } +
        request.preservedSnapshotOfPreviousGroup.enResponses.values
            .count { it != EdiItemResponse.UNANSWERED }

    AlertDialog(
        onDismissRequest = {
            onResolve(EdiGroupChangeResolutionAction.CANCEL_SWITCH_KEEP_CURRENT_GROUP)
        },
        modifier = Modifier.testTag("edi_group_change_dialog"),
        title = {
            Text(
                text = "Confirmar cambio de Grupo G${request.previousGroupNumber} -> G${request.targetGroupNumber}",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = request.reason,
                    fontSize = 13.sp,
                    color = DarkText
                )
                Text(
                    text = "Existen $recordedCount respuesta(s) clínica(s) registrada(s) en el Grupo ${request.previousGroupNumber}. Nunca se reutilizarán respuestas entre grupos distintos.",
                    fontSize = 12.sp,
                    color = SlateText
                )
            }
        },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        onResolve(EdiGroupChangeResolutionAction.ARCHIVE_PREVIOUS_AND_SWITCH_TO_CLEAN_TARGET)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("edi_confirm_archive_switch"),
                    colors = ButtonDefaults.buttonColors(containerColor = EdiNavy)
                ) {
                    Text("Archivar G${request.previousGroupNumber} y abrir G${request.targetGroupNumber} limpio", fontSize = 12.sp)
                }
                OutlinedButton(
                    onClick = {
                        onResolve(EdiGroupChangeResolutionAction.DISCARD_PREVIOUS_CONFIRMED_AND_SWITCH_TO_CLEAN_TARGET)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("edi_confirm_discard_switch"),
                    border = BorderStroke(1.dp, AlertRed)
                ) {
                    Text("Descartar registro previo y cambiar", color = AlertRed, fontSize = 12.sp)
                }
                TextButton(
                    onClick = {
                        onResolve(EdiGroupChangeResolutionAction.CANCEL_SWITCH_KEEP_CURRENT_GROUP)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("edi_confirm_cancel_switch")
                ) {
                    Text("Cancelar y mantener Grupo ${request.previousGroupNumber}", fontSize = 12.sp)
                }
            }
        }
    )
}

private fun areaStatusColors(status: EdiAreaColorResult): Pair<Color, Color> = when (status) {
    EdiAreaColorResult.VERDE -> SoftGreenPill to DarkGreenPill
    EdiAreaColorResult.AMARILLO -> SoftAmberPill to WarningText
    EdiAreaColorResult.ROJO -> SoftRedBg to DarkRedText
    EdiAreaColorResult.INCOMPLETE -> SoftGrayBg to SlateText
    EdiAreaColorResult.NOT_APPLICABLE -> SoftGrayBg to MutedText
}
