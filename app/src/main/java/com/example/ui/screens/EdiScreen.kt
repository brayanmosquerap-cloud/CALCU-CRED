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
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MedicalInformation
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Pin
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.ReportProblem
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AutosaveUiIndicatorState
import com.example.model.NewEvaluationCreationResult
import com.example.model.CredEdiUiState
import com.example.model.EdiArchivedCompatibilityStatus
import com.example.model.EdiArchivedSessionEntry
import com.example.model.EdiAreaColorResult
import com.example.model.EdiAreaEvaluationResult
import com.example.model.EdiAxis
import com.example.model.EdiBirthMaturityCondition
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
import com.example.model.EdiPendingRestoreRequest
import com.example.model.EvaluationPersistenceStatus
import com.example.persistence.SavedEvaluationSummary
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
    onConfirmTermWithoutWeeks: () -> Unit = {},
    onClearRegisteredEg: () -> Unit = {},
    onConfirmContradictionKeepRegisteredEg: () -> Unit = {},
    onConfirmContradictionAsPretermWithoutWeeks: () -> Unit = {},
    onUpdateEgSemanas: (Int) -> Unit = {},
    onOpenArchivedPreview: (String) -> Unit = {},
    onCloseArchivedPreview: () -> Unit = {},
    onRequestRestoreArchived: (String) -> Unit = {},
    onSetRestoreSamePatientContinuityConfirmed: (Boolean) -> Unit = {},
    onCancelRestoreArchived: () -> Unit = {},
    onConfirmRestoreArchived: () -> Unit = {},
    onStartNewEvaluationEpisode: () -> Unit = {},
    onCorrectSamePatientFechaNacimiento: (String, String) -> Unit = { _, _ -> },
    onSaveEvaluationManually: () -> Unit = {},
    onToggleEvaluationHistory: () -> Unit = {},
    onStartNewPatientEvaluation: () -> Unit = {},
    onCreateNewPatientEvaluation: (String, String, Boolean) -> NewEvaluationCreationResult = { _, _, _ ->
        NewEvaluationCreationResult.Success("")
    },
    onContinueEvaluationFromHistory: (String) -> Unit = {},
    onViewCompletedEvaluationFromHistory: (String) -> Unit = {},
    onEnableEditingForViewedCompletedEvaluation: () -> Unit = {},
    onRequestDeleteEvaluationFromHistory: (String) -> Unit = {},
    onCancelDeleteEvaluationFromHistory: () -> Unit = {},
    onConfirmDeleteEvaluationFromHistory: () -> Unit = {},
    onUpdatePatientIdentification: (String, String, String) -> Unit = { _, _, _ -> },
    onPatientFullNameChange: ((String) -> Unit)? = null,
    onPatientDniChange: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val effectiveFullNameChange: (String) -> Unit = onPatientFullNameChange ?: { newFullName ->
        onUpdatePatientIdentification(
            newFullName.replace("\r", " ").replace("\n", " "),
            state.patientIdentification.clinicalRecordCode,
            state.patientIdentification.generalClinicalNote
        )
    }
    val effectiveDniChange: (String) -> Unit = onPatientDniChange ?: { rawDni ->
        onUpdatePatientIdentification(
            state.patientIdentification.patientIdentifierOrAlias,
            rawDni.filter { it.isDigit() }.take(8),
            state.patientIdentification.generalClinicalNote
        )
    }
    var showCorrectBirthDateDialog by remember { mutableStateOf(false) }
    var showNewPatientEvaluationDialog by remember { mutableStateOf(false) }

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

    val previewEntry = state.ediEngineState.previewArchivedEntry
    if (previewEntry != null) {
        EdiArchivedSessionPreviewDialog(
            state = state,
            entry = previewEntry,
            onClose = onCloseArchivedPreview,
            onRequestRestore = {
                onCloseArchivedPreview()
                onRequestRestoreArchived(previewEntry.archiveId)
            }
        )
    }

    val pendingRestore = state.ediEngineState.pendingRestoreRequest
    if (pendingRestore != null) {
        EdiRestoreArchivedConfirmationDialog(
            request = pendingRestore,
            onToggleSamePatientContinuity = onSetRestoreSamePatientContinuityConfirmed,
            onConfirm = onConfirmRestoreArchived,
            onStartNewEpisodeForOtherPatient = {
                onCancelRestoreArchived()
                onStartNewEvaluationEpisode()
            },
            onCancel = onCancelRestoreArchived
        )
    }

    val pendingDeleteSummary = state.pendingDeleteEvaluationSummary
    if (pendingDeleteSummary != null) {
        EdiHistoryDeleteConfirmationDialog(
            summary = pendingDeleteSummary,
            onCancel = onCancelDeleteEvaluationFromHistory,
            onConfirm = onConfirmDeleteEvaluationFromHistory
        )
    }

    if (showCorrectBirthDateDialog) {
        EdiCorrectSamePatientBirthDateDialog(
            state = state,
            onDismiss = { showCorrectBirthDateDialog = false },
            onConfirmCorrection = { newDate, reason ->
                onCorrectSamePatientFechaNacimiento(newDate, reason)
                showCorrectBirthDateDialog = false
            },
            onRejectedCorrectionAttempt = { newDate, reason ->
                onCorrectSamePatientFechaNacimiento(newDate, reason)
            }
        )
    }

    if (showNewPatientEvaluationDialog) {
        EdiNewPatientIdentificationDialog(
            onDismiss = { showNewPatientEvaluationDialog = false },
            onCreateEvaluation = { fullName, dni, allowPendingId ->
                val result = onCreateNewPatientEvaluation(fullName, dni, allowPendingId)
                if (result is NewEvaluationCreationResult.Success) {
                    showNewPatientEvaluationDialog = false
                }
                result
            }
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
                onOpenAxisQuestionnaire = onOpenAxisQuestionnaire,
                onConfirmTermWithoutWeeks = onConfirmTermWithoutWeeks,
                onTogglePrematurez = onTogglePrematurez,
                onUseAgeSuggestedGroup = onUseAgeSuggestedGroup,
                onOpenArchivedPreview = onOpenArchivedPreview,
                onRequestRestoreArchived = onRequestRestoreArchived,
                onStartNewEvaluationEpisode = onStartNewEvaluationEpisode,
                onOpenCorrectBirthDateDialog = { showCorrectBirthDateDialog = true },
                onSaveEvaluationManually = onSaveEvaluationManually,
                onToggleEvaluationHistory = onToggleEvaluationHistory,
                onStartNewPatientEvaluation = { showNewPatientEvaluationDialog = true },
                onContinueEvaluationFromHistory = onContinueEvaluationFromHistory,
                onViewCompletedEvaluationFromHistory = onViewCompletedEvaluationFromHistory,
                onEnableEditingForViewedCompletedEvaluation = onEnableEditingForViewedCompletedEvaluation,
                onRequestDeleteEvaluationFromHistory = onRequestDeleteEvaluationFromHistory,
                onUpdatePatientIdentification = onUpdatePatientIdentification,
                onPatientFullNameChange = effectiveFullNameChange,
                onPatientDniChange = effectiveDniChange
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
                onAdjustEdiFechaEvalYear = onAdjustEdiFechaEvalYear,
                onConfirmTermWithoutWeeks = onConfirmTermWithoutWeeks,
                onClearRegisteredEg = onClearRegisteredEg,
                onConfirmContradictionKeepRegisteredEg = onConfirmContradictionKeepRegisteredEg,
                onConfirmContradictionAsPretermWithoutWeeks = onConfirmContradictionAsPretermWithoutWeeks,
                onUpdateEgSemanas = onUpdateEgSemanas,
                onOpenArchivedPreview = onOpenArchivedPreview,
                onRequestRestoreArchived = onRequestRestoreArchived,
                onStartNewEvaluationEpisode = onStartNewEvaluationEpisode,
                onOpenCorrectBirthDateDialog = { showCorrectBirthDateDialog = true },
                onSaveEvaluationManually = onSaveEvaluationManually,
                onToggleEvaluationHistory = onToggleEvaluationHistory,
                onStartNewPatientEvaluation = { showNewPatientEvaluationDialog = true },
                onContinueEvaluationFromHistory = onContinueEvaluationFromHistory,
                onViewCompletedEvaluationFromHistory = onViewCompletedEvaluationFromHistory,
                onEnableEditingForViewedCompletedEvaluation = onEnableEditingForViewedCompletedEvaluation,
                onRequestDeleteEvaluationFromHistory = onRequestDeleteEvaluationFromHistory,
                onUpdatePatientIdentification = onUpdatePatientIdentification
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
                onUpdateItemClinicalNote = onUpdateItemClinicalNote,
                onConfirmTermWithoutWeeks = onConfirmTermWithoutWeeks,
                onTogglePrematurez = onTogglePrematurez
            )

            EdiInternalView.RESULTS_TRACEABILITY -> EdiResultsTraceabilityView(
                state = state,
                onBack = onNavigateBack,
                onNavigateView = onNavigateView,
                onConfirmTermWithoutWeeks = onConfirmTermWithoutWeeks,
                onTogglePrematurez = onTogglePrematurez,
                onUseAgeSuggestedGroup = onUseAgeSuggestedGroup
            )
        }
    }
}

@Composable
private fun EdiSummary5AxesView(
    state: CredEdiUiState,
    onNavigateView: (EdiInternalView) -> Unit,
    onOpenAxisQuestionnaire: (EdiAxis) -> Unit,
    onConfirmTermWithoutWeeks: () -> Unit = {},
    onTogglePrematurez: (Boolean) -> Unit = {},
    onUseAgeSuggestedGroup: () -> Unit = {},
    onOpenArchivedPreview: (String) -> Unit = {},
    onRequestRestoreArchived: (String) -> Unit = {},
    onStartNewEvaluationEpisode: () -> Unit = {},
    onOpenCorrectBirthDateDialog: () -> Unit = {},
    onSaveEvaluationManually: () -> Unit = {},
    onToggleEvaluationHistory: () -> Unit = {},
    onStartNewPatientEvaluation: () -> Unit = {},
    onContinueEvaluationFromHistory: (String) -> Unit = {},
    onViewCompletedEvaluationFromHistory: (String) -> Unit = {},
    onEnableEditingForViewedCompletedEvaluation: () -> Unit = {},
    onRequestDeleteEvaluationFromHistory: (String) -> Unit = {},
    onUpdatePatientIdentification: (String, String, String) -> Unit = { _, _, _ -> },
    onPatientFullNameChange: (String) -> Unit = {},
    onPatientDniChange: (String) -> Unit = {}
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
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("edi_view_summary"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
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
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Manual EDI Peruano 2026 · 5 Ejes Oficiales",
                    color = SlateText,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
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

        // MEDIO-06 — LOTE 3E: Sección independiente «IDENTIFICACIÓN DEL PACIENTE» al inicio del contenido EDI,
        // ubicada antes de las tarjetas de edad, ejes, cuestionarios, botones de continuación y panel de historial.
        EdiPatientIdentificationTopSection(
            state = state,
            onPatientFullNameChange = onPatientFullNameChange,
            onPatientDniChange = onPatientDniChange
        )

        // Chronological & Corrected Age Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ClinicalSectionCard(
                modifier = Modifier.weight(1f),
                contentPadding = 8.dp
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CalendarToday,
                        contentDescription = "Edad cronológica",
                        tint = EdiBlueAccent,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = "Edad Cronológica",
                        color = SlateText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (state.ediHasFechaNacimientoValida) state.edadCronologicaAMD else "Sin fecha",
                    color = DarkText,
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (state.ediHasFechaNacimientoValida) {
                        "${state.edadCronologicaMeses}m ${state.edadCronologicaDias}d"
                    } else {
                        "Ingrese F.N. en Tab 1"
                    },
                    color = MutedText,
                    fontSize = 10.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            ClinicalSectionCard(
                modifier = Modifier.weight(1f),
                contentPadding = 8.dp
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Schedule,
                        contentDescription = "Edad corregida",
                        tint = if (state.usaEdadCorregida) ClinicalGreen else SlateText,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = "Edad Corregida",
                        color = SlateText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = when {
                        state.ediHasGestationalContradiction -> state.ediGestationalAgeHeadlineForSummary
                        state.isCorrectedAgePending -> "EDAD CORREGIDA PENDIENTE"
                        state.isBirthMaturityConfirmationPendingInUnder24m -> "CONFIRMAR CONDICIÓN AL NACER"
                        !state.ediHasFechaNacimientoValida -> state.ediGestationalAgeHeadlineForSummary
                        state.usaEdadCorregida -> state.edadCorregidaAMD
                        else -> "No aplica"
                    },
                    color = when {
                        state.ediHasGestationalContradiction || state.isCorrectedAgePending -> DarkRedText
                        state.isBirthMaturityConfirmationPendingInUnder24m -> WarningText
                        else -> DarkText
                    },
                    fontSize = if (state.isCorrectedAgePending || state.isBirthMaturityConfirmationPendingInUnder24m) 11.sp else 15.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("edi_summary_corrected_age_value")
                )
                Text(
                    text = state.ediGestationalAgeDisplay,
                    color = if (state.ediHasGestationalContradiction) DarkRedText else MutedText,
                    fontSize = 10.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("edi_summary_ga_status_text")
                )
            }
        }

        // Active Group Banner
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = EdiNavy
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
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

                if (state.isEdiManualConsultationMode) {
                    Spacer(modifier = Modifier.height(8.dp))
                    PillBadge(
                        text = "MODO CONSULTA MANUAL / SIMULACIÓN · EDAD NO VERIFICABLE",
                        backgroundColor = Color(0xFF334155),
                        textColor = Color(0xFFFDE68A),
                        borderColor = Color(0xFFF59E0B),
                        fontSizeSp = 10
                    )
                } else if (state.ediManualGroupOverride) {
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

        // Manual Consultation Mode / Unverifiable Age Banner
        if (state.isEdiManualConsultationMode) {
            EdiUnverifiableAgeConsultationBanner(
                state = state,
                testTag = "edi_summary_unverifiable_age_banner"
            )
        }

        // Gestational Age Contradiction Warning (if present)
        if (state.ediHasGestationalContradiction) {
            EdiGestationalContradictionBanner(
                state = state,
                onGoToBank = { onNavigateView(EdiInternalView.GROUP_BANK_15) },
                testTag = "edi_summary_ga_contradiction_banner"
            )
        }

        // Birth Maturity Confirmation Pending Warning in < 24m (Option 2)
        if (state.isBirthMaturityConfirmationPendingInUnder24m) {
            EdiBirthMaturityPendingBanner(
                state = state,
                onConfirmTermWithoutWeeks = onConfirmTermWithoutWeeks,
                onRegisterPreterm = {
                    onTogglePrematurez(true)
                    onNavigateView(EdiInternalView.GROUP_BANK_15)
                },
                testTag = "edi_summary_birth_maturity_pending_banner"
            )
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

        // Group / Restored Session Compatibility Warning (MEDIO-02)
        if (
            state.ediHasGroupAgeMismatch ||
            state.isRestoredSessionFromDifferentPatientOrEpisode ||
            state.isRestoredSessionOriginUnverified ||
            state.isRestoredSessionGestationalOrDateContextModified
        ) {
            EdiGroupAndRestoreCompatibilityBanner(
                state = state,
                onUseAgeSuggestedGroup = onUseAgeSuggestedGroup,
                onGoToBank = { onNavigateView(EdiInternalView.GROUP_BANK_15) },
                testTag = "edi_summary_compatibility_warning_banner"
            )
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

        // Sección MEDIO-02 / MEDIO-06 Lote 3: Guardado e Historial de Evaluaciones
        EdiArchivedSessionsTemporalSection(
            state = state,
            onOpenArchivedPreview = onOpenArchivedPreview,
            onRequestRestoreArchived = onRequestRestoreArchived,
            onStartNewEvaluationEpisode = onStartNewEvaluationEpisode,
            onOpenCorrectBirthDateDialog = onOpenCorrectBirthDateDialog,
            onSaveEvaluationManually = onSaveEvaluationManually,
            onToggleEvaluationHistory = onToggleEvaluationHistory,
            onStartNewPatientEvaluation = onStartNewPatientEvaluation,
            onContinueEvaluationFromHistory = onContinueEvaluationFromHistory,
            onViewCompletedEvaluationFromHistory = onViewCompletedEvaluationFromHistory,
            onEnableEditingForViewedCompletedEvaluation = onEnableEditingForViewedCompletedEvaluation,
            onRequestDeleteEvaluationFromHistory = onRequestDeleteEvaluationFromHistory,
            onUpdatePatientIdentification = onUpdatePatientIdentification,
            testTagPrefix = "edi_summary"
        )
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
    onAdjustEdiFechaEvalYear: (Int) -> Unit,
    onConfirmTermWithoutWeeks: () -> Unit,
    onClearRegisteredEg: () -> Unit,
    onConfirmContradictionKeepRegisteredEg: () -> Unit,
    onConfirmContradictionAsPretermWithoutWeeks: () -> Unit,
    onUpdateEgSemanas: (Int) -> Unit,
    onOpenArchivedPreview: (String) -> Unit = {},
    onRequestRestoreArchived: (String) -> Unit = {},
    onStartNewEvaluationEpisode: () -> Unit = {},
    onOpenCorrectBirthDateDialog: () -> Unit = {},
    onSaveEvaluationManually: () -> Unit = {},
    onToggleEvaluationHistory: () -> Unit = {},
    onStartNewPatientEvaluation: () -> Unit = {},
    onContinueEvaluationFromHistory: (String) -> Unit = {},
    onViewCompletedEvaluationFromHistory: (String) -> Unit = {},
    onEnableEditingForViewedCompletedEvaluation: () -> Unit = {},
    onRequestDeleteEvaluationFromHistory: (String) -> Unit = {},
    onUpdatePatientIdentification: (String, String, String) -> Unit = { _, _, _ -> }
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
            if (state.isEdiManualConsultationMode) {
                EdiUnverifiableAgeConsultationBanner(
                    state = state,
                    testTag = "edi_bank_unverifiable_age_banner"
                )
            }

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
                            text = "Estado EG: ${state.ediGestationalAgeDisplay}",
                            color = if (state.ediHasGestationalContradiction) DarkRedText else SlateText,
                            fontSize = 11.sp,
                            fontWeight = if (state.ediHasGestationalContradiction) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.testTag("edi_bank_ga_status_text")
                        )
                    }
                    Switch(
                        checked = state.esPrematuro,
                        onCheckedChange = onTogglePrematurez,
                        modifier = Modifier.testTag("edi_prematurity_switch")
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(
                        onClick = onConfirmTermWithoutWeeks,
                        modifier = Modifier
                            .defaultMinSize(minHeight = 40.dp)
                            .testTag("edi_btn_confirm_term_no_weeks"),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Confirmar A término (sin semanas)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    if (!state.ediHasRegisteredEg && state.esPrematuro) {
                        OutlinedButton(
                            onClick = { onUpdateEgSemanas(32) },
                            modifier = Modifier
                                .defaultMinSize(minHeight = 40.dp)
                                .testTag("edi_btn_register_32s"),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Registrar 32s 0d exactas",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    if (state.ediHasRegisteredEg) {
                        OutlinedButton(
                            onClick = onClearRegisteredEg,
                            modifier = Modifier
                                .defaultMinSize(minHeight = 40.dp)
                                .testTag("edi_btn_clear_registered_eg"),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Limpiar EG",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Weeks stepper
                    StepperBox(
                        label = "Semanas EG",
                        valueText = if (state.ediHasRegisteredEg) "${state.egSemanas} sem" else "Sin reg.",
                        onMinus = { onAdjustEgSemanas(-1) },
                        onPlus = { onAdjustEgSemanas(1) },
                        minusTag = "edi_ga_weeks_minus",
                        plusTag = "edi_ga_weeks_plus",
                        modifier = Modifier.weight(1f)
                    )
                    // Days stepper (0..6, no rounding)
                    StepperBox(
                        label = "Días EG (0–6)",
                        valueText = if (state.ediHasRegisteredEg) "${state.egDias} d" else "-- d",
                        onMinus = { onAdjustEgDias(-1) },
                        onPlus = { onAdjustEgDias(1) },
                        minusTag = "edi_ga_days_minus",
                        plusTag = "edi_ga_days_plus",
                        modifier = Modifier.weight(1f)
                    )
                }

                if (state.ediHasGestationalContradiction) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edi_ga_contradiction_card"),
                        shape = RoundedCornerShape(14.dp),
                        color = SoftRedBg,
                        border = BorderStroke(1.5.dp, AlertRed)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "⚠ Contradicción de Edad Gestacional — Confirmación requerida",
                                color = DarkRedText,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = state.ediGestationalContradictionMessage,
                                color = DarkText,
                                fontSize = 11.5.sp
                            )
                            if (state.ediHasRegisteredEg && state.egSemanas < 37) {
                                Button(
                                    onClick = onConfirmContradictionKeepRegisteredEg,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .defaultMinSize(minHeight = 44.dp)
                                        .testTag("edi_ga_confirm_keep_eg"),
                                    colors = ButtonDefaults.buttonColors(containerColor = EdiNavy),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(
                                        text = "Confirmar Pretérmino (${state.egSemanas}s ${state.egDias}d)",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                OutlinedButton(
                                    onClick = onConfirmTermWithoutWeeks,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .defaultMinSize(minHeight = 44.dp)
                                        .testTag("edi_ga_confirm_term_no_weeks"),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, AlertRed)
                                ) {
                                    Text(
                                        text = "Confirmar A término (sin semanas exactas)",
                                        color = DarkRedText,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            } else if (state.ediHasRegisteredEg && state.egSemanas >= 37) {
                                Button(
                                    onClick = onConfirmContradictionKeepRegisteredEg,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .defaultMinSize(minHeight = 44.dp)
                                        .testTag("edi_ga_confirm_keep_eg"),
                                    colors = ButtonDefaults.buttonColors(containerColor = EdiNavy),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(
                                        text = "Confirmar A término (${state.egSemanas}s ${state.egDias}d)",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                OutlinedButton(
                                    onClick = onConfirmContradictionAsPretermWithoutWeeks,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .defaultMinSize(minHeight = 44.dp)
                                        .testTag("edi_ga_confirm_preterm_clear_eg"),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, AlertRed)
                                ) {
                                    Text(
                                        text = "Confirmar Prematuro (limpiar ≥37s e ingresar EG)",
                                        color = DarkRedText,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                } else if (state.ediHasRegisteredEg && state.egSemanas < 37) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = state.ediGestationalAgeInput.formulaDocumentation,
                        color = EdiBlueAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                } else if (!state.ediHasRegisteredEg && state.esPrematuro) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Prematuridad confirmada sin semanas exactas registradas. Ingrese semanas y días reales para la corrección EDI.",
                        color = WarningText,
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

            if (state.isBirthMaturityConfirmationPendingInUnder24m) {
                EdiBirthMaturityPendingBanner(
                    state = state,
                    onConfirmTermWithoutWeeks = onConfirmTermWithoutWeeks,
                    onRegisterPreterm = { onTogglePrematurez(true) },
                    testTag = "edi_bank_birth_maturity_pending_banner"
                )
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
                val isSuggested = !state.isBirthMaturityConfirmationPendingInUnder24m &&
                    comparison.automaticallyAssignedGroup?.groupNumber == group.groupNumber
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

            // Sección MEDIO-02 / MEDIO-06 Lote 3 en Banco de Grupos: Guardado e Historial de Evaluaciones
            EdiArchivedSessionsTemporalSection(
                state = state,
                onOpenArchivedPreview = onOpenArchivedPreview,
                onRequestRestoreArchived = onRequestRestoreArchived,
                onStartNewEvaluationEpisode = onStartNewEvaluationEpisode,
                onOpenCorrectBirthDateDialog = onOpenCorrectBirthDateDialog,
                onSaveEvaluationManually = onSaveEvaluationManually,
                onToggleEvaluationHistory = onToggleEvaluationHistory,
                onStartNewPatientEvaluation = onStartNewPatientEvaluation,
                onContinueEvaluationFromHistory = onContinueEvaluationFromHistory,
                onViewCompletedEvaluationFromHistory = onViewCompletedEvaluationFromHistory,
                onEnableEditingForViewedCompletedEvaluation = onEnableEditingForViewedCompletedEvaluation,
                onRequestDeleteEvaluationFromHistory = onRequestDeleteEvaluationFromHistory,
                onUpdatePatientIdentification = onUpdatePatientIdentification,
                testTagPrefix = "edi_bank"
            )
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
    val isBlockedByGestation = state.ediHasGestationalContradiction ||
        state.isCorrectedAgePending ||
        state.isBirthMaturityConfirmationPendingInUnder24m
    val isDiscrepancy = !isBlockedByGestation && comparison.autoSelectionStatus ==
        EdiGroupAutoSelectionStatus.BLOCKED_GROUP_DISCREPANCY_REQUIRES_PROFESSIONAL_REVIEW
    val isOutOfRange = !isBlockedByGestation && state.ediIsOutOfOfficialAgeRange

    val bg = when {
        state.ediHasGestationalContradiction -> SoftRedBg
        state.isCorrectedAgePending || state.isBirthMaturityConfirmationPendingInUnder24m -> SoftAmberBg
        isDiscrepancy && !state.ediIsDiscrepancyResolvedForCurrentInputs -> SoftAmberBg
        isOutOfRange -> SoftRedBg
        comparison.autoSelectionStatus == EdiGroupAutoSelectionStatus.AUTO_SELECTED_CONSISTENT -> SoftGreenBg
        else -> Color.White
    }
    val border = when {
        state.ediHasGestationalContradiction -> BorderSoftRed
        state.isCorrectedAgePending || state.isBirthMaturityConfirmationPendingInUnder24m -> BorderAmber
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
                    text = when {
                        state.ediHasGestationalContradiction -> "Confirmar EG (Bloqueado)"
                        state.isCorrectedAgePending -> "EDAD CORREGIDA PENDIENTE"
                        state.isBirthMaturityConfirmationPendingInUnder24m -> "Confirmar condición al nacer (Bloqueado)"
                        else -> "${comparison.manualMethodResult.effectiveAgeUsed.formattedMonthsDays} -> ${comparison.manualMethodResult.assignedGroup?.code ?: "Fuera de rango"}"
                    },
                    color = if (isBlockedByGestation) DarkRedText else DarkText,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Método Calendario CRED", color = SlateText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = when {
                        state.ediHasGestationalContradiction -> "Confirmar EG (Bloqueado)"
                        state.isCorrectedAgePending -> "EDAD CORREGIDA PENDIENTE"
                        state.isBirthMaturityConfirmationPendingInUnder24m -> "Confirmar condición al nacer (Bloqueado)"
                        else -> "${comparison.calendarCredMethodResult.effectiveAgeUsed.formattedMonthsDays} -> ${comparison.calendarCredMethodResult.assignedGroup?.code ?: "Fuera de rango"}"
                    },
                    color = if (isBlockedByGestation) DarkRedText else DarkText,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = when {
                state.ediHasGestationalContradiction ->
                    "Asignación automática bloqueada: resuelva la contradicción entre prematuridad y edad gestacional registrada."
                state.isCorrectedAgePending ->
                    "Asignación automática bloqueada en menor de 24 meses: prematuridad confirmada sin EG exacta registrada (EDAD CORREGIDA PENDIENTE)."
                state.isBirthMaturityConfirmationPendingInUnder24m ->
                    "Asignación automática bloqueada en menor de 24 meses (${state.edadCronologicaAMD}): condición al nacer sin confirmar (UNSPECIFIED). Confirme si nació a término o prematuro."
                else ->
                    comparison.clinicalReviewMessage
            },
            color = DarkText,
            fontSize = 12.sp
        )
        if (!state.ediHasRegisteredEg) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Estado EG: ${state.ediGestationalAgeDisplay}.",
                color = SlateText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }

        if (!isBlockedByGestation && state.ediManualGroupOverride && comparison.automaticallyAssignedGroup != null) {
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
    onUpdateItemClinicalNote: (String, String) -> Unit,
    onConfirmTermWithoutWeeks: () -> Unit = {},
    onTogglePrematurez: (Boolean) -> Unit = {}
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
                        Text(
                            text = "EG: ${state.ediGestationalAgeDisplay}",
                            color = if (state.ediHasGestationalContradiction) DarkRedText else SlateText,
                            fontSize = 10.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.testTag("edi_q_ga_status_text")
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
            if (state.isEdiManualConsultationMode) {
                EdiUnverifiableAgeConsultationBanner(
                    state = state,
                    testTag = "edi_q_unverifiable_age_banner"
                )
            }
            if (state.ediHasGestationalContradiction) {
                EdiGestationalContradictionBanner(
                    state = state,
                    onGoToBank = { onNavigateView(EdiInternalView.GROUP_BANK_15) },
                    testTag = "edi_q_ga_contradiction_banner"
                )
            }
            if (state.isBirthMaturityConfirmationPendingInUnder24m) {
                EdiBirthMaturityPendingBanner(
                    state = state,
                    onConfirmTermWithoutWeeks = onConfirmTermWithoutWeeks,
                    onRegisterPreterm = {
                        onTogglePrematurez(true)
                        onNavigateView(EdiInternalView.GROUP_BANK_15)
                    },
                    testTag = "edi_q_birth_maturity_pending_banner"
                )
            }
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
                        text = "Material mencionado en el encabezado general; no indispensable para este reactivo",
                        backgroundColor = SoftGreenBg,
                        textColor = DarkGreenPill,
                        fontSizeSp = 10,
                        modifier = Modifier.testTag("edi_header_material_only_badge_$effectiveUniqueId")
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
    onNavigateView: (EdiInternalView) -> Unit,
    onConfirmTermWithoutWeeks: () -> Unit = {},
    onTogglePrematurez: (Boolean) -> Unit = {},
    onUseAgeSuggestedGroup: () -> Unit = {}
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
        state.egDias,
        state.ediEgRegistrada,
        state.ediBirthMaturityCondition
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
            if (state.ediHasGestationalContradiction) {
                EdiGestationalContradictionBanner(
                    state = state,
                    onGoToBank = { onNavigateView(EdiInternalView.GROUP_BANK_15) },
                    testTag = "edi_res_ga_contradiction_banner"
                )
            }

            if (state.isBirthMaturityConfirmationPendingInUnder24m) {
                EdiBirthMaturityPendingBanner(
                    state = state,
                    onConfirmTermWithoutWeeks = onConfirmTermWithoutWeeks,
                    onRegisterPreterm = {
                        onTogglePrematurez(true)
                        onNavigateView(EdiInternalView.GROUP_BANK_15)
                    },
                    testTag = "edi_res_birth_maturity_pending_banner"
                )
            }

            // Priority Critical Alerts Banner (ALWAYS visible when hasPriorityCriticalWarning, even if INCOMPLETE)
            if (globalEval.hasPriorityCriticalWarning) {
                EdiPriorityWarningBanner(globalEval = globalEval)
            }

            if (
                state.ediHasGroupAgeMismatch ||
                state.isRestoredSessionFromDifferentPatientOrEpisode ||
                state.isRestoredSessionOriginUnverified ||
                state.isRestoredSessionGestationalOrDateContextModified
            ) {
                EdiGroupAndRestoreCompatibilityBanner(
                    state = state,
                    onUseAgeSuggestedGroup = onUseAgeSuggestedGroup,
                    onGoToBank = { onNavigateView(EdiInternalView.GROUP_BANK_15) },
                    testTag = "edi_res_compatibility_warning_banner"
                )
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
                        text = if (state.isEdiManualConsultationMode) {
                            "INCOMPLETO / EDAD NO VERIFICABLE · MODO CONSULTA MANUAL"
                        } else {
                            "SEMÁFORO CALCULADO · MOTOR EDI 2026"
                        },
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

            if (state.isEdiManualConsultationMode) {
                EdiUnverifiableAgeConsultationBanner(
                    state = state,
                    testTag = "edi_res_unverifiable_age_banner"
                )
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
                    text = "Sexo: ${state.sexo.label} · EG: ${state.ediGestationalAgeDisplay}",
                    color = if (state.ediHasGestationalContradiction) DarkRedText else DarkText,
                    fontSize = 12.sp,
                    modifier = Modifier.testTag("edi_res_ga_status_text")
                )
                val resCorrectedAgeText = if (state.isBirthMaturityConfirmationPendingInUnder24m) {
                    "CONFIRMAR CONDICIÓN AL NACER"
                } else {
                    state.edadCorregidaAMD
                }
                Text(
                    text = "Edad Cronológica: ${if (state.ediHasFechaNacimientoValida) state.edadCronologicaAMD else "No ingresada"} · Edad Corregida: $resCorrectedAgeText",
                    color = SlateText,
                    fontSize = 12.sp
                )
                if (state.ediHasFechaNacimientoValida) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = when {
                            state.ediHasGestationalContradiction ->
                                "Asignación automática bloqueada: contradicción entre prematuridad y edad gestacional registrada."
                            state.isCorrectedAgePending ->
                                "Asignación automática bloqueada (< 24 meses): prematuridad confirmada sin EG exacta registrada (EDAD CORREGIDA PENDIENTE)."
                            state.isBirthMaturityConfirmationPendingInUnder24m ->
                                "Asignación automática bloqueada (< 24 meses): condición al nacer no confirmada (UNSPECIFIED). Confirme si nació a término o prematuro."
                            else ->
                                state.ediCorrectedAgeComparison.clinicalReviewMessage
                        },
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
private fun EdiUnverifiableAgeConsultationBanner(
    state: CredEdiUiState,
    testTag: String
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag),
        shape = RoundedCornerShape(18.dp),
        color = SoftAmberBg,
        border = BorderStroke(1.5.dp, WarningAmber)
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
                    imageVector = Icons.Outlined.WarningAmber,
                    contentDescription = "Modo consulta manual por edad no verificable",
                    tint = WarningAmber,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "INCOMPLETO / EDAD NO VERIFICABLE · MODO DE CONSULTA MANUAL",
                    color = WarningText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            Text(
                text = "${state.ediUnverifiableAgeReason} Puede consultar los 15 grupos, revisar los 270 reactivos y ensayar respuestas o retrocesos sin perder datos; el semáforo clínico definitivo y la Ficha Oficial PDF requieren fechas válidas.",
                color = DarkText,
                fontSize = 11.5.sp
            )
        }
    }
}

@Composable
private fun EdiBirthMaturityPendingBanner(
    state: CredEdiUiState,
    onConfirmTermWithoutWeeks: () -> Unit,
    onRegisterPreterm: () -> Unit,
    testTag: String
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag),
        shape = RoundedCornerShape(18.dp),
        color = SoftAmberBg,
        border = BorderStroke(1.5.dp, WarningAmber)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.WarningAmber,
                    contentDescription = "Condición al nacer pendiente de confirmación",
                    tint = WarningAmber,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "CONDICIÓN AL NACER PENDIENTE DE CONFIRMACIÓN (< 24 MESES)",
                    color = WarningText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            Text(
                text = "Menor de 24 meses (${state.edadCronologicaAMD}) sin antecedente gestacional confirmado. Confirme si nació a término o prematuro antes de asignar automáticamente el grupo EDI o emitir el semáforo definitivo.",
                color = DarkText,
                fontSize = 11.5.sp
            )
            Button(
                onClick = onConfirmTermWithoutWeeks,
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 44.dp)
                    .testTag("${testTag}_confirm_term"),
                colors = ButtonDefaults.buttonColors(containerColor = EdiNavy),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = "Confirmar A término (sin semanas exactas)",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            OutlinedButton(
                onClick = onRegisterPreterm,
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 44.dp)
                    .testTag("${testTag}_register_preterm"),
                border = BorderStroke(1.dp, WarningAmber),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = "Registrar prematuridad y EG",
                    color = WarningText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun EdiGestationalContradictionBanner(
    state: CredEdiUiState,
    onGoToBank: () -> Unit,
    testTag: String
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag),
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
                    imageVector = Icons.Outlined.WarningAmber,
                    contentDescription = "Advertencia de edad gestacional contradictoria",
                    tint = AlertRed,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "CONTRADICCIÓN EN EDAD GESTACIONAL",
                    color = DarkRedText,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            Text(
                text = state.ediGestationalContradictionMessage,
                color = DarkText,
                fontSize = 12.sp
            )
            OutlinedButton(
                onClick = onGoToBank,
                modifier = Modifier.defaultMinSize(minHeight = 44.dp),
                border = BorderStroke(1.dp, AlertRed),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = "Revisar y confirmar EG en Banco EDI",
                    color = DarkRedText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
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

@Composable
private fun EdiGroupAndRestoreCompatibilityBanner(
    state: CredEdiUiState,
    onUseAgeSuggestedGroup: () -> Unit,
    onGoToBank: () -> Unit,
    testTag: String
) {
    val officialGroup = state.ediOfficialApplicableGroupNumber
    val titleText = when {
        state.isRestoredSessionFromDifferentPatientOrEpisode ->
            "BLOQUEO DE SEGURIDAD · SESIÓN RESTAURADA DE OTRO EPISODIO / PACIENTE"
        state.isRestoredSessionOriginUnverified ->
            "BLOQUEO DE SEGURIDAD · CONTEXTO ORIGINAL NO VERIFICADO"
        state.ediHasGroupAgeMismatch ->
            "INCOMPATIBILIDAD ETARIA Y GESTACIONAL · G${state.ediEffectiveGroupNumber} vs G${officialGroup ?: "-"}"
        else ->
            "ADVERTENCIA · CONTEXTO MODIFICADO DESPUÉS DEL ARCHIVADO"
    }
    val bodyText = when {
        state.isRestoredSessionFromDifferentPatientOrEpisode -> {
            val orig = state.ediEngineState.restoredSessionOriginContext
            "La sesión restaurada (${state.ediEngineState.restoredFromArchiveId}) fue registrada en el episodio ${orig?.workEpisodeId ?: "-"} (${orig?.sexoLabel ?: "-"}, F.N.: ${orig?.fechaNacimiento?.ifBlank { "Sin F.N." } ?: "-"}). No se permite reutilizar respuestas antiguas para emitir un dictamen definitivo en el episodio actual (${state.ediEngineState.currentWorkEpisodeId})."
        }
        state.isRestoredSessionOriginUnverified -> {
            "La sesión restaurada (${state.ediEngineState.restoredFromArchiveId}) fue capturada con contexto clínico NO VERIFICADO (${state.ediEngineState.restoredSessionOriginContext?.effectiveAgeLabel ?: "NO VERIFICADO"}). Sus respuestas se conservan para revisión, pero no pueden emitir automáticamente un semáforo definitivo."
        }
        state.ediHasGroupAgeMismatch -> {
            state.ediGroupAgeMismatchMessage
        }
        else -> {
            val orig = state.ediEngineState.restoredSessionOriginContext
            "La sesión restaurada (${state.ediEngineState.restoredFromArchiveId}) conserva su contexto original (${orig?.effectiveAgeLabel ?: "-"}, ${orig?.gestationalAgeDisplayLabel ?: "-"}). Verifique la correspondencia clínica actual."
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag),
        shape = RoundedCornerShape(18.dp),
        color = SoftAmberBg,
        border = BorderStroke(1.5.dp, WarningAmber)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.WarningAmber,
                    contentDescription = "Advertencia de compatibilidad de sesión o grupo EDI",
                    tint = WarningAmber,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = titleText,
                    color = WarningText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            Text(
                text = bodyText,
                color = DarkText,
                fontSize = 11.5.sp
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (state.ediHasGroupAgeMismatch && officialGroup != null) {
                    Button(
                        onClick = onUseAgeSuggestedGroup,
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 44.dp)
                            .testTag("${testTag}_use_official_group"),
                        colors = ButtonDefaults.buttonColors(containerColor = EdiNavy),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "Cambiar a G$officialGroup oficial",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                OutlinedButton(
                    onClick = onGoToBank,
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 44.dp)
                        .testTag("${testTag}_review_bank"),
                    border = BorderStroke(1.dp, WarningAmber),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "Revisar grupo en Banco EDI",
                        color = WarningText,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun EdiArchivedSessionsTemporalSection(
    state: CredEdiUiState,
    onOpenArchivedPreview: (String) -> Unit,
    onRequestRestoreArchived: (String) -> Unit,
    onStartNewEvaluationEpisode: () -> Unit,
    onOpenCorrectBirthDateDialog: () -> Unit = {},
    onSaveEvaluationManually: () -> Unit = {},
    onToggleEvaluationHistory: () -> Unit = {},
    onStartNewPatientEvaluation: () -> Unit = {},
    onContinueEvaluationFromHistory: (String) -> Unit = {},
    onViewCompletedEvaluationFromHistory: (String) -> Unit = {},
    onEnableEditingForViewedCompletedEvaluation: () -> Unit = {},
    onRequestDeleteEvaluationFromHistory: (String) -> Unit = {},
    onUpdatePatientIdentification: (String, String, String) -> Unit = { _, _, _ -> },
    testTagPrefix: String
) {
    // R3: Por privacidad, mostrar únicamente las entradas correspondientes al episodio actual
    val currentEpisodeHistory = state.ediEngineState.currentEpisodeArchivedHistory
    val previousEpisodesCount = state.ediEngineState.previousEpisodesArchivedCount
    val isSavingNow = state.persistenceIndicatorState == AutosaveUiIndicatorState.SAVING

    val (indicatorBg, indicatorFg, indicatorBorder) = when (state.persistenceIndicatorState) {
        AutosaveUiIndicatorState.SAVED -> Triple(SoftGreenPill, DarkGreenPill, BorderSoftGreen)
        AutosaveUiIndicatorState.PENDING_CHANGES -> Triple(SoftAmberPill, WarningText, BorderAmber)
        AutosaveUiIndicatorState.SAVING -> Triple(EdiSoftBlueBg, EdiBlueAccent, EdiSoftBlueBorder)
        AutosaveUiIndicatorState.ERROR -> Triple(SoftRedBg, DarkRedText, BorderSoftRed)
        AutosaveUiIndicatorState.IDLE -> Triple(SoftGrayBg, SlateText, Color(0xFFCBD5E1))
    }

    ClinicalSectionCard(
        backgroundColor = Color.White,
        borderColor = EdiSoftBlueBorder,
        modifier = Modifier.testTag("edi_archived_sessions_section")
    ) {
        // MEDIO-07: Ajuste visual del panel de guardado (sin título ni identificación duplicada;
        // se conserva exclusivamente el indicador de estado de guardado sin espacios vacíos sobrantes).
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            PillBadge(
                text = state.persistenceIndicatorState.displayLabel,
                backgroundColor = indicatorBg,
                textColor = indicatorFg,
                borderColor = indicatorBorder,
                fontSizeSp = 10,
                modifier = Modifier.testTag("${testTagPrefix}_persistence_indicator_badge")
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // MEDIO-06 — LOTE 3B: Únicos dos botones principales visibles en el panel:
        // «Guardar evaluación» y «Ver historial / Ocultar historial»
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onSaveEvaluationManually,
                enabled = !isSavingNow && !state.isReadOnlyConsultationMode,
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = 48.dp)
                    .testTag("${testTagPrefix}_save_evaluation_btn"),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = EdiBlueAccent,
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Verified,
                    contentDescription = "Guardar evaluación",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = if (isSavingNow) "Guardando…" else "Guardar evaluación",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            OutlinedButton(
                onClick = onToggleEvaluationHistory,
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = 48.dp)
                    .testTag("${testTagPrefix}_toggle_history_btn"),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, EdiSoftBlueBorder),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (state.isEvaluationHistoryExpanded) EdiSoftBlueBg else Color.White,
                    contentColor = EdiNavy
                ),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = if (state.isEvaluationHistoryExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    contentDescription = if (state.isEvaluationHistoryExpanded) "Ocultar historial" else "Ver historial",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (state.isEvaluationHistoryExpanded) "Ocultar historial" else "Ver historial",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // MEDIO-06 — LOTE 3 (Sección 4.B): Banner de modo consulta de solo lectura para evaluaciones completadas
        if (state.isReadOnlyConsultationMode) {
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("${testTagPrefix}_readonly_consultation_banner"),
                shape = RoundedCornerShape(10.dp),
                color = SoftAmberBg,
                border = BorderStroke(1.dp, WarningAmber)
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Modo consulta de evaluación completada (solo lectura): las respuestas y el semáforo definitivo están protegidos contra cambios accidentales.",
                        color = DarkText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedButton(
                        onClick = onEnableEditingForViewedCompletedEvaluation,
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("${testTagPrefix}_enable_editing_btn"),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, EdiNavy)
                    ) {
                        Text(
                            text = "Habilitar edición clínica",
                            color = EdiNavy,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Avisos de confirmación real de guardado, recuperación automática o error con opción de reintentar
        if (state.wasRestoredFromPersistence || state.persistenceStatusMessage != null || state.persistenceRecoveryErrorMessage != null) {
            Spacer(modifier = Modifier.height(8.dp))
            val isErrorBanner = state.persistenceIndicatorState == AutosaveUiIndicatorState.ERROR ||
                state.persistenceRecoveryErrorMessage != null
            val bannerBg = when {
                isErrorBanner -> SoftRedBg
                state.wasRestoredFromPersistence -> EdiSoftBlueBg
                else -> SoftGreenBg
            }
            val bannerBorder = when {
                isErrorBanner -> BorderSoftRed
                state.wasRestoredFromPersistence -> EdiSoftBlueBorder
                else -> BorderSoftGreen
            }
            val bannerTextColor = when {
                isErrorBanner -> DarkRedText
                state.wasRestoredFromPersistence -> EdiNavy
                else -> DarkGreenPill
            }
            val bannerText = state.persistenceRecoveryErrorMessage
                ?: state.persistenceStatusMessage
                ?: if (state.wasRestoredFromPersistence) "Evaluación recuperada" else ""

            if (bannerText.isNotBlank()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("${testTagPrefix}_persistence_status_banner"),
                    shape = RoundedCornerShape(10.dp),
                    color = bannerBg,
                    border = BorderStroke(1.dp, bannerBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = bannerText,
                            color = bannerTextColor,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (isErrorBanner) {
                            OutlinedButton(
                                onClick = onSaveEvaluationManually,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .defaultMinSize(minHeight = 48.dp)
                                    .testTag("${testTagPrefix}_retry_save_btn"),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, AlertRed)
                            ) {
                                Text(
                                    text = "Reintentar guardado",
                                    color = DarkRedText,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // MEDIO-06 — LOTE 3D (Sección 3): Lista plegable simplificada de evaluaciones guardadas en Room
        // (sin tarjeta «Gestión multipaciente» ni botón duplicado de nueva evaluación)
        if (state.isEvaluationHistoryExpanded) {
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("${testTagPrefix}_saved_history_list"),
                shape = RoundedCornerShape(12.dp),
                color = SoftGrayBg,
                border = BorderStroke(1.dp, Color(0xFFCBD5E1))
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Historial local en dispositivo (${state.savedEvaluationHistory.size} evaluación(es) registrada(s))",
                        color = DarkText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold
                    )

                    if (state.savedEvaluationHistory.isEmpty()) {
                        Text(
                            text = "No hay evaluaciones guardadas en el historial local de este dispositivo.",
                            color = MutedText,
                            fontSize = 11.5.sp,
                            modifier = Modifier.testTag("${testTagPrefix}_saved_history_empty_text")
                        )
                    } else {
                        state.savedEvaluationHistory.forEach { item ->
                            val isCurrentActiveEval = item.evaluationUuid == state.evaluationUuid
                            val patientDisplay = item.patientIdentification
                                .historyTitleLine(item.evaluationUuid)
                            val patientDniDisplay = item.patientIdentification.historyDniLine
                            val stateLabel = when {
                                item.status == EvaluationPersistenceStatus.COMPLETED -> "Completada"
                                item.status == EvaluationPersistenceStatus.ARCHIVED -> "Archivada"
                                item.isActiveDraft -> "En curso"
                                else -> "Incompleta"
                            }
                            val (stateBg, stateFg) = when (stateLabel) {
                                "Completada" -> SoftGreenPill to DarkGreenPill
                                "En curso" -> EdiSoftBlueBg to EdiBlueAccent
                                "Archivada" -> SoftGrayBg to SlateText
                                else -> SoftAmberPill to WarningText
                            }
                            val semaphoreDisplayLabel = if (item.isDefinitiveSemaphore) {
                                when (item.semaphoreCode) {
                                    "VERDE" -> "Verde"
                                    "AMARILLO" -> "Amarillo"
                                    "ROJO" -> "Rojo"
                                    else -> item.semaphoreLabel
                                }
                            } else {
                                "Sin semáforo definitivo"
                            }
                            val (semBg, semFg) = if (item.isDefinitiveSemaphore) {
                                when (item.semaphoreCode) {
                                    "VERDE" -> SoftGreenPill to DarkGreenPill
                                    "AMARILLO" -> SoftAmberPill to WarningText
                                    "ROJO" -> SoftRedBg to DarkRedText
                                    else -> SoftGrayBg to SlateText
                                }
                            } else {
                                SoftGrayBg to SlateText
                            }

                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("${testTagPrefix}_history_card_${item.evaluationUuid}"),
                                shape = RoundedCornerShape(12.dp),
                                color = if (isCurrentActiveEval) EdiSoftBlueBg else Color.White,
                                border = BorderStroke(
                                    1.dp,
                                    if (isCurrentActiveEval) EdiBlueAccent else Color(0xFFCBD5E1)
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = patientDisplay,
                                                color = DarkText,
                                                fontSize = 12.5.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                modifier = Modifier
                                                    .testTag("${testTagPrefix}_history_patient_label_${item.evaluationUuid}")
                                            )
                                            Text(
                                                text = patientDniDisplay,
                                                color = SlateText,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                modifier = Modifier
                                                    .testTag("${testTagPrefix}_history_patient_dni_${item.evaluationUuid}")
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        PillBadge(
                                            text = stateLabel,
                                            backgroundColor = stateBg,
                                            textColor = stateFg,
                                            fontSizeSp = 10,
                                            modifier = Modifier.testTag("${testTagPrefix}_history_state_badge_${item.evaluationUuid}")
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Grupo EDI: ${item.activeGroupCode} (${item.activeGroupTitle}) · Reactivos: ${item.answeredMandatoryCount}/${item.totalMandatoryCount}",
                                            color = DarkText,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        PillBadge(
                                            text = semaphoreDisplayLabel,
                                            backgroundColor = semBg,
                                            textColor = semFg,
                                            fontSizeSp = 10,
                                            modifier = Modifier.testTag("${testTagPrefix}_history_semaphore_badge_${item.evaluationUuid}")
                                        )
                                    }

                                    Text(
                                        text = "Fecha evaluación: ${item.fechaEvaluacion.ifBlank { "No registrada" }} · Último guardado: ${formatHistoryEpochMillis(item.updatedAtEpochMillis)}",
                                        color = SlateText,
                                        fontSize = 10.5.sp
                                    )

                                    Text(
                                        text = "Antropometría: ${item.anthropometrySummaryLabel} · ID: ${item.evaluationUuid}",
                                        color = SlateText,
                                        fontSize = 10.5.sp
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        if (item.status == EvaluationPersistenceStatus.COMPLETED) {
                                            Button(
                                                onClick = { onViewCompletedEvaluationFromHistory(item.evaluationUuid) },
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .defaultMinSize(minHeight = 48.dp)
                                                    .testTag("${testTagPrefix}_history_view_btn_${item.evaluationUuid}"),
                                                colors = ButtonDefaults.buttonColors(containerColor = EdiNavy),
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Text(
                                                    text = "Ver",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        } else {
                                            Button(
                                                onClick = { onContinueEvaluationFromHistory(item.evaluationUuid) },
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .defaultMinSize(minHeight = 48.dp)
                                                    .testTag("${testTagPrefix}_history_continue_btn_${item.evaluationUuid}"),
                                                colors = ButtonDefaults.buttonColors(containerColor = EdiBlueAccent),
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Text(
                                                    text = "Continuar",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }

                                        OutlinedButton(
                                            onClick = { onRequestDeleteEvaluationFromHistory(item.evaluationUuid) },
                                            modifier = Modifier
                                                .weight(1f)
                                                .defaultMinSize(minHeight = 48.dp)
                                                .testTag("${testTagPrefix}_history_delete_btn_${item.evaluationUuid}"),
                                            shape = RoundedCornerShape(10.dp),
                                            border = BorderStroke(1.dp, AlertRed)
                                        ) {
                                            Text(
                                                text = "Eliminar",
                                                color = DarkRedText,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // MEDIO-06 — LOTE 3D (Secciones 4 y 7):
        // Sustitución del control visible «Nuevo episodio» / «Opciones de episodio EDI» por «Nueva evaluación»,
        // que solicita primero la identificación del paciente antes de crear un registro con UUID nuevo.
        // El botón «Corregir fecha de nacimiento del mismo paciente» se retira de la vista principal,
        // conservando íntegramente las reglas clínicas de fecha/edad y el gestor interno de episodios.
        OutlinedButton(
            onClick = onStartNewPatientEvaluation,
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 48.dp)
                .testTag("${testTagPrefix}_new_evaluation_btn"),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, EdiNavy),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Color.White,
                contentColor = EdiNavy
            ),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = "Nueva evaluación",
                tint = EdiNavy,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Nueva evaluación",
                color = EdiNavy,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // MEDIO-06 — LOTE 3 (Sección 8): Aviso obligatorio de guardado local en el dispositivo
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("edi_archived_ephemeral_notice"),
            shape = RoundedCornerShape(12.dp),
            color = SoftAmberBg,
            border = BorderStroke(1.dp, BorderAmber)
        ) {
            Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = "Aviso de guardado local en el dispositivo",
                    tint = WarningAmber,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Las evaluaciones se guardan localmente en este dispositivo. Puedes recuperarlas desde el historial. Si se eliminan los datos de la aplicación o se restablece el emulador, los registros locales podrían perderse",
                    color = DarkText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Notificación cuando se abre un nuevo episodio o se registra estado de restauración (R1/R2/R3)
        state.ediEngineState.lastRestoreStatusMessage?.let { statusMsg ->
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("edi_episode_status_message"),
                shape = RoundedCornerShape(10.dp),
                color = EdiSoftBlueBg,
                border = BorderStroke(1.dp, EdiSoftBlueBorder)
            ) {
                Text(
                    text = statusMsg,
                    color = EdiNavy,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                )
            }
        }

        // Privacidad de episodios anteriores (R3)
        if (previousEpisodesCount > 0) {
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("edi_previous_episodes_protected_notice"),
                shape = RoundedCornerShape(10.dp),
                color = SoftGrayBg,
                border = BorderStroke(1.dp, Color(0xFFCBD5E1))
            ) {
                Text(
                    text = "Privacidad por episodio: $previousEpisodesCount evaluación(es) de episodio(s) anterior(es) permanecen resguardadas y ocultas en el episodio actual (${state.ediEngineState.currentWorkEpisodeId}).",
                    color = SlateText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                )
            }
        }

        // R2: Cuando existan archivos previos en el episodio actual y la sesión activa esté vacía,
        // facilitar la elección explícita entre continuar el mismo episodio o comenzar uno nuevo.
        if (currentEpisodeHistory.isNotEmpty() && !state.ediEngineState.activeSession.hasRecordedClinicalResponses) {
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("${testTagPrefix}_empty_active_continuity_card"),
                shape = RoundedCornerShape(12.dp),
                color = SoftAmberBg,
                border = BorderStroke(1.dp, WarningAmber)
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Sesión activa vacía con evaluaciones archivadas (${state.ediEngineState.currentWorkEpisodeId})",
                        color = WarningText,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "La coincidencia de sexo y fecha de nacimiento NO verifica la identidad del paciente. Antes de reutilizar evaluaciones archivadas, confirme si continúa con el mismo paciente o inicie un nuevo episodio si atiende a otro paciente.",
                        color = DarkText,
                        fontSize = 11.sp
                    )
                    OutlinedButton(
                        onClick = onStartNewEvaluationEpisode,
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("${testTagPrefix}_other_patient_new_episode_btn"),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, WarningAmber)
                    ) {
                        Text(
                            text = "Es otro paciente — iniciar nuevo episodio",
                            color = WarningText,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        state.ediEngineState.restoredFromArchiveId?.let { restoredId ->
            Spacer(modifier = Modifier.height(8.dp))
            PillBadge(
                text = "Trazabilidad activa: restaurada desde $restoredId",
                backgroundColor = EdiSoftBlueBg,
                textColor = EdiBlueAccent,
                borderColor = EdiSoftBlueBorder,
                fontSizeSp = 11
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (currentEpisodeHistory.isEmpty()) {
            Text(
                text = "No hay evaluaciones archivadas en el episodio activo (${state.ediEngineState.currentWorkEpisodeId}).",
                color = MutedText,
                fontSize = 12.sp,
                modifier = Modifier.testTag("edi_archived_empty_text")
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                currentEpisodeHistory.forEach { entry ->
                    val compatibility = state.evaluateArchivedEntryCompatibility(entry)
                    val isRestorableInCurrentEpisode =
                        entry.originContext.workEpisodeId == state.ediEngineState.currentWorkEpisodeId &&
                            compatibility.status != EdiArchivedCompatibilityStatus.INCOMPATIBLE_DIFFERENT_PATIENT_OR_EPISODE
                    val (badgeBg, badgeFg) = when (compatibility.status) {
                        EdiArchivedCompatibilityStatus.COMPATIBLE -> SoftGreenPill to DarkGreenPill
                        EdiArchivedCompatibilityStatus.INCOMPATIBLE_GROUP_OR_GESTATIONAL_STATE -> SoftAmberPill to WarningText
                        EdiArchivedCompatibilityStatus.INCOMPATIBLE_ORIGIN_UNVERIFIED -> SoftAmberPill to WarningText
                        EdiArchivedCompatibilityStatus.INCOMPATIBLE_DIFFERENT_PATIENT_OR_EPISODE -> SoftRedBg to DarkRedText
                    }
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edi_archived_entry_${entry.archiveId}"),
                        shape = RoundedCornerShape(14.dp),
                        color = SoftGrayBg,
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    PillBadge(
                                        text = "G${entry.groupNumber}",
                                        backgroundColor = EdiNavy,
                                        textColor = Color.White,
                                        fontSizeSp = 10
                                    )
                                    Text(
                                        text = "ID: ${entry.archiveId} · Orden #${entry.sequenceNumber}",
                                        color = DarkText,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                                PillBadge(
                                    text = compatibility.status.badgeLabel,
                                    backgroundColor = badgeBg,
                                    textColor = badgeFg,
                                    fontSizeSp = 9
                                )
                            }

                            Text(
                                text = "Resumen: ${entry.snapshot.summaryText}",
                                color = DarkText,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Origen (${entry.originContext.workEpisodeId}): ${entry.originContext.originValidationState.displayLabel} · ${entry.originContext.sexoLabel} · F.N.: ${entry.originContext.fechaNacimiento.ifBlank { "Sin F.N." }} · F.Eval: ${entry.originContext.fechaEvaluacion.ifBlank { "Sin F.Eval" }} · ${entry.originContext.gestationalAgeDisplayLabel}",
                                color = SlateText,
                                fontSize = 11.sp
                            )
                            Text(
                                text = "Compatibilidad: ${compatibility.summaryLabel}",
                                color = badgeFg,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { onOpenArchivedPreview(entry.archiveId) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .defaultMinSize(minHeight = 48.dp)
                                        .testTag("edi_archived_preview_btn_${entry.archiveId}"),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Revisar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                Button(
                                    onClick = { onRequestRestoreArchived(entry.archiveId) },
                                    enabled = isRestorableInCurrentEpisode,
                                    modifier = Modifier
                                        .weight(1f)
                                        .defaultMinSize(minHeight = 48.dp)
                                        .testTag("edi_archived_restore_btn_${entry.archiveId}"),
                                    colors = ButtonDefaults.buttonColors(containerColor = EdiNavy),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Restaurar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EdiArchivedSessionPreviewDialog(
    state: CredEdiUiState,
    entry: EdiArchivedSessionEntry,
    onClose: () -> Unit,
    onRequestRestore: () -> Unit
) {
    val compatibility = state.evaluateArchivedEntryCompatibility(entry)
    val canRestoreInCurrentEpisode =
        entry.originContext.workEpisodeId == state.ediEngineState.currentWorkEpisodeId &&
            compatibility.status != EdiArchivedCompatibilityStatus.INCOMPATIBLE_DIFFERENT_PATIENT_OR_EPISODE
    val snap = entry.snapshot
    val answeredCurrent = snap.currentGroupResponses.filterValues { it != EdiItemResponse.UNANSWERED }
    val answeredFallback = snap.fallbackGroupResponses.filterValues { it != EdiItemResponse.UNANSWERED }
    val answeredFrb = snap.frbResponses.filterValues { it != EdiItemResponse.UNANSWERED }
    val answeredEn = snap.enResponses.filterValues { it != EdiItemResponse.UNANSWERED }

    AlertDialog(
        onDismissRequest = onClose,
        modifier = Modifier.testTag("edi_archived_preview_dialog"),
        title = {
            Text(
                text = "Vista previa · ${entry.archiveId} (Grupo G${entry.groupNumber} · Orden #${entry.sequenceNumber})",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 15.sp
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Modo de solo lectura: revisar esta sesión archivada NO modifica la evaluación activa (G${state.ediEffectiveGroupNumber}).",
                    color = EdiBlueAccent,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Motivo de archivado: ${entry.archiveReason}",
                    color = DarkText,
                    fontSize = 12.sp
                )
                Text(
                    text = "Contexto clínico de origen (${entry.originContext.workEpisodeId}): ${entry.originContext.originValidationState.displayLabel} · ${entry.originContext.sexoLabel} · F.N.: ${entry.originContext.fechaNacimiento.ifBlank { "No registrada" }} · F.Eval: ${entry.originContext.fechaEvaluacion.ifBlank { "No registrada" }} · ${entry.originContext.gestationalAgeDisplayLabel} · Edad: ${entry.originContext.effectiveAgeLabel}",
                    color = SlateText,
                    fontSize = 11.5.sp
                )
                Text(
                    text = "Estado de compatibilidad actual: ${compatibility.summaryLabel}",
                    color = if (compatibility.isFullyCompatibleForDefinitiveEvaluation) ClinicalGreen else WarningText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                compatibility.warnings.forEach { warn ->
                    Text(
                        text = "• $warn",
                        color = WarningText,
                        fontSize = 11.sp
                    )
                }
                HorizontalDivider()
                Text(
                    text = "Resumen del Snapshot (${snap.summaryText})",
                    color = DarkText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                if (answeredCurrent.isNotEmpty()) {
                    Text(
                        text = "Reactivos grupo G${entry.groupNumber}: ${answeredCurrent.entries.joinToString { "${it.key}=${it.value.name}" }}",
                        color = DarkText,
                        fontSize = 11.sp
                    )
                }
                if (answeredFallback.isNotEmpty()) {
                    Text(
                        text = "Retrocesos: ${answeredFallback.entries.joinToString { "${it.key}=${it.value.name}" }}",
                        color = DarkText,
                        fontSize = 11.sp
                    )
                }
                if (answeredFrb.isNotEmpty()) {
                    Text(
                        text = "FRB: ${answeredFrb.entries.joinToString { "${it.key}=${it.value.name}" }}",
                        color = DarkText,
                        fontSize = 11.sp
                    )
                }
                if (answeredEn.isNotEmpty()) {
                    Text(
                        text = "EN: ${answeredEn.entries.joinToString { "${it.key}=${it.value.name}" }}",
                        color = DarkText,
                        fontSize = 11.sp
                    )
                }
                if (snap.dualEvidenceRecords.isNotEmpty()) {
                    Text(
                        text = "Evidencias duales: ${snap.dualEvidenceRecords.entries.joinToString { "${it.key}(obs=${it.value.directObservationEvidence}, ent=${it.value.directedInterviewEvidence}, res=${it.value.professionalHumanResolution})" }}",
                        color = DarkText,
                        fontSize = 11.sp
                    )
                }
                if (snap.itemClinicalNotes.isNotEmpty()) {
                    Text(
                        text = "Notas clínicas: ${snap.itemClinicalNotes.entries.joinToString { "${it.key}: \"${it.value}\"" }}",
                        color = DarkText,
                        fontSize = 11.sp
                    )
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onClose,
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("edi_archived_preview_close_btn")
                ) {
                    Text("Cerrar vista previa", fontSize = 12.sp)
                }
                Button(
                    onClick = onRequestRestore,
                    enabled = canRestoreInCurrentEpisode,
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("edi_archived_preview_restore_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = EdiNavy)
                ) {
                    Text("Solicitar restauración", fontSize = 12.sp)
                }
            }
        }
    )
}

@Composable
private fun EdiRestoreArchivedConfirmationDialog(
    request: EdiPendingRestoreRequest,
    onToggleSamePatientContinuity: (Boolean) -> Unit,
    onConfirm: () -> Unit,
    onStartNewEpisodeForOtherPatient: () -> Unit,
    onCancel: () -> Unit
) {
    val entry = request.targetEntry
    val snap = entry.snapshot
    AlertDialog(
        onDismissRequest = onCancel,
        modifier = Modifier.testTag("edi_restore_confirm_dialog"),
        title = {
            Text(
                text = "Confirmar restauración · ${entry.archiveId} (Grupo G${entry.groupNumber})",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 15.sp
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Se restaurará íntegramente la sesión ${entry.archiveId} (Orden #${entry.sequenceNumber}, Grupo G${entry.groupNumber}) sin mezclar respuestas con la evaluación activa.",
                    color = DarkText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Contenido a restaurar: ${snap.summaryText}",
                    color = SlateText,
                    fontSize = 11.5.sp
                )
                Text(
                    text = "Contexto original (${entry.originContext.workEpisodeId}): ${entry.originContext.originValidationState.displayLabel} · ${entry.originContext.sexoLabel} · F.N.: ${entry.originContext.fechaNacimiento.ifBlank { "No registrada" }} · ${entry.originContext.gestationalAgeDisplayLabel}",
                    color = SlateText,
                    fontSize = 11.5.sp
                )
                Text(
                    text = "Nota de seguridad clínica: la coincidencia de sexo y fecha de nacimiento NO verifica la identidad del paciente.",
                    color = WarningText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
                if (request.warnings.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = SoftAmberBg,
                        border = BorderStroke(1.dp, WarningAmber),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edi_restore_warnings_box")
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Advertencias de restauración y compatibilidad:",
                                color = WarningText,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            request.warnings.forEach { warning ->
                                Text(
                                    text = "• $warning",
                                    color = DarkText,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }

                // R2: Confirmación deliberada obligatoria de continuidad del paciente y episodio
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (request.samePatientAndEpisodeConfirmed) SoftGreenPill else SoftGrayBg,
                    border = BorderStroke(
                        1.5.dp,
                        if (request.samePatientAndEpisodeConfirmed) ClinicalGreen else Color(0xFF94A3B8)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onToggleSamePatientContinuity(!request.samePatientAndEpisodeConfirmed)
                        }
                        .testTag("edi_restore_confirm_same_patient_checkbox")
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Checkbox(
                            checked = request.samePatientAndEpisodeConfirmed,
                            onCheckedChange = null
                        )
                        Text(
                            text = "Confirmo que esta evaluación corresponde al mismo paciente y episodio de atención",
                            color = DarkText,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onConfirm,
                    enabled = request.samePatientAndEpisodeConfirmed,
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("edi_restore_confirm_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = EdiNavy)
                ) {
                    Text(
                        text = if (request.activeSessionWillBeArchivedFirst) {
                            "Archivar sesión activa y restaurar ${entry.archiveId}"
                        } else {
                            "Confirmar restauración de ${entry.archiveId}"
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                OutlinedButton(
                    onClick = onStartNewEpisodeForOtherPatient,
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("edi_restore_other_patient_new_episode_btn"),
                    border = BorderStroke(1.dp, WarningAmber)
                ) {
                    Text(
                        text = "Es otro paciente — iniciar nuevo episodio",
                        color = WarningText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                TextButton(
                    onClick = onCancel,
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("edi_restore_cancel_btn")
                ) {
                    Text("Cancelar y mantener sesión activa", fontSize = 12.sp)
                }
            }
        }
    )
}

private fun parseStrictDialogDate(raw: String): com.example.model.SimpleDate? {
    val trimmed = raw.trim().take(10)
    val normalized = if (trimmed.length == 8 && trimmed.all { it.isDigit() }) {
        "${trimmed.substring(0, 2)}/${trimmed.substring(2, 4)}/${trimmed.substring(4, 8)}"
    } else {
        trimmed
    }
    if (normalized.isEmpty()) return null
    val parts = normalized.split("/", "-", ".")
    if (parts.size != 3) return null
    val p0 = parts[0].trim().toIntOrNull() ?: return null
    val p1 = parts[1].trim().toIntOrNull() ?: return null
    val p2 = parts[2].trim().toIntOrNull() ?: return null
    if (parts[0].trim().length == 4 && p0 in 1900..2100 && p1 in 1..12 && p2 in 1..com.example.model.ClinicalAgeCalculator.daysInMonth(p0, p1)) {
        return com.example.model.SimpleDate(day = p2, month = p1, year = p0)
    }
    if (parts[2].trim().length == 4 && p2 in 1900..2100 && p1 in 1..12 && p0 in 1..com.example.model.ClinicalAgeCalculator.daysInMonth(p2, p1)) {
        return com.example.model.SimpleDate(day = p0, month = p1, year = p2)
    }
    return null
}

@Composable
private fun EdiCorrectSamePatientBirthDateDialog(
    state: CredEdiUiState,
    onDismiss: () -> Unit,
    onConfirmCorrection: (String, String) -> Unit,
    onRejectedCorrectionAttempt: (String, String) -> Unit
) {
    var newBirthDateInput by remember { mutableStateOf("") }
    var correctionReasonInput by remember { mutableStateOf("") }
    var validationError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("edi_correct_birth_date_dialog"),
        title = {
            Text(
                text = "Corregir fecha de nacimiento del mismo paciente",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 15.sp,
                modifier = Modifier.testTag("edi_correct_fn_dialog_title")
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SoftAmberBg,
                    border = BorderStroke(1.5.dp, WarningAmber),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edi_correct_fn_same_patient_warning")
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Si se trata de otro paciente, utilice Nuevo episodio. Esta acción solo corrige los datos del mismo paciente",
                            color = WarningText,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "Conserva el episodio activo (${state.ediEngineState.currentWorkEpisodeId}), el historial archivado y los antecedentes de captura originales sin modificarlos retroactivamente.",
                            color = DarkText,
                            fontSize = 11.sp
                        )
                    }
                }

                Text(
                    text = "Fecha de nacimiento actual: ${state.fechaNacimiento.ifBlank { "No registrada" }}",
                    color = DarkText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.testTag("edi_correct_fn_current_date_text")
                )
                Text(
                    text = "Fecha de evaluación actual: ${state.fechaEvaluacion.ifBlank { "No registrada" }}",
                    color = SlateText,
                    fontSize = 11.5.sp,
                    modifier = Modifier.testTag("edi_correct_fn_eval_date_text")
                )

                validationError?.let { errorMsg ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = SoftRedBg,
                        border = BorderStroke(1.dp, AlertRed),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edi_correct_fn_error_banner")
                    ) {
                        Text(
                            text = errorMsg,
                            color = DarkRedText,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = newBirthDateInput,
                    onValueChange = {
                        newBirthDateInput = it
                        validationError = null
                    },
                    label = { Text("Nueva fecha de nacimiento (DD/MM/AAAA)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edi_correct_fn_new_date_input")
                )

                OutlinedTextField(
                    value = correctionReasonInput,
                    onValueChange = {
                        correctionReasonInput = it
                        validationError = null
                    },
                    label = { Text("Motivo obligatorio de la corrección") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edi_correct_fn_reason_input")
                )
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("edi_correct_fn_cancel_btn"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Cancelar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = {
                        val trimmedReason = correctionReasonInput.trim()
                        val parsedBirth = parseStrictDialogDate(newBirthDateInput)
                        val parsedEval = com.example.model.ClinicalAgeCalculator.parseDate(state.fechaEvaluacion)
                        when {
                            trimmedReason.isEmpty() -> {
                                validationError = "Es obligatorio indicar el motivo de la corrección."
                                onRejectedCorrectionAttempt(newBirthDateInput, correctionReasonInput)
                            }
                            parsedBirth == null -> {
                                validationError = "La fecha ingresada no tiene un formato válido o no existe en el calendario (use DD/MM/AAAA)."
                                onRejectedCorrectionAttempt(newBirthDateInput, correctionReasonInput)
                            }
                            parsedEval == null -> {
                                validationError = "La fecha de evaluación actual no es válida para comprobar compatibilidad."
                                onRejectedCorrectionAttempt(newBirthDateInput, correctionReasonInput)
                            }
                            java.time.LocalDate.of(parsedBirth.year, parsedBirth.month, parsedBirth.day)
                                .isAfter(java.time.LocalDate.of(parsedEval.year, parsedEval.month, parsedEval.day)) -> {
                                validationError = "La nueva fecha de nacimiento no puede ser posterior a la fecha de evaluación (${state.fechaEvaluacion})."
                                onRejectedCorrectionAttempt(newBirthDateInput, correctionReasonInput)
                            }
                            else -> {
                                validationError = null
                                onConfirmCorrection(newBirthDateInput.trim(), trimmedReason)
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("edi_correct_fn_confirm_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = EdiNavy),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Confirmar corrección", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    )
}

private fun formatHistoryEpochMillis(epochMillis: Long): String {
    if (epochMillis <= 0L) return "No registrado"
    return runCatching {
        val instant = java.time.Instant.ofEpochMilli(epochMillis)
        val zoned = instant.atZone(java.time.ZoneId.systemDefault())
        val formatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
        zoned.format(formatter)
    }.getOrElse {
        epochMillis.toString()
    }
}

@Composable
private fun EdiHistoryDeleteConfirmationDialog(
    summary: SavedEvaluationSummary,
    onCancel: () -> Unit,
    onConfirm: () -> Unit
) {
    val patientLabel = summary.patientIdentification.historyTitleLine(summary.evaluationUuid)
    val patientDniLabel = summary.patientIdentification.historyDniLine
    val stateLabel = when {
        summary.status == EvaluationPersistenceStatus.COMPLETED -> "Completada"
        summary.status == EvaluationPersistenceStatus.ARCHIVED -> "Archivada"
        summary.isActiveDraft -> "En curso"
        else -> "Incompleta"
    }
    AlertDialog(
        onDismissRequest = onCancel,
        modifier = Modifier.testTag("edi_history_delete_confirm_dialog"),
        title = {
            Text(
                text = "Confirmar eliminación de evaluación",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 15.sp,
                modifier = Modifier.testTag("edi_history_delete_dialog_title")
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SoftRedBg,
                    border = BorderStroke(1.dp, AlertRed),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "¿Desea eliminar permanentemente esta evaluación del almacenamiento local?",
                            color = DarkRedText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "Paciente: $patientLabel · $patientDniLabel",
                            color = DarkText,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "ID único: ${summary.evaluationUuid} · Estado: $stateLabel",
                            color = DarkText,
                            fontSize = 11.sp
                        )
                        Text(
                            text = "Grupo EDI: ${summary.activeGroupCode} (${summary.activeGroupTitle}) · Fecha evaluación: ${summary.fechaEvaluacion.ifBlank { "No registrada" }}",
                            color = SlateText,
                            fontSize = 11.sp
                        )
                    }
                }
                Text(
                    text = "Esta operación elimina únicamente el registro seleccionado en la base de datos local sin afectar las demás evaluaciones guardadas.",
                    color = SlateText,
                    fontSize = 11.sp
                )
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("edi_history_delete_cancel_btn"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Cancelar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = onConfirm,
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("edi_history_delete_confirm_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = AlertRed),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "Confirmar eliminación",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    )
}

/**
 * MEDIO-06 — LOTE 3D (Secciones 5 y 6):
 * Diálogo previo de identificación del nuevo paciente al pulsar «Nueva evaluación».
 * Solicita nombre completo y apellidos en un único campo + DNI peruano de 8 dígitos
 * (preservando ceros iniciales) o flujo explícito de identificación pendiente sin DNI ficticio.
 * No crea ningún registro al abrirse; solo al confirmar «Crear evaluación» tras validar y guardar
 * de forma segura la evaluación anterior.
 */
@Composable
private fun EdiNewPatientIdentificationDialog(
    onDismiss: () -> Unit,
    onCreateEvaluation: (String, String, Boolean) -> NewEvaluationCreationResult
) {
    var fullName by remember { mutableStateOf("") }
    var dni by remember { mutableStateOf("") }
    var allowPendingId by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("new_evaluation_patient_id_dialog"),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(EdiSoftBlueBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Badge,
                        contentDescription = "Identificación del nuevo paciente",
                        tint = EdiNavy,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    text = "Nueva evaluación",
                    color = EdiNavy,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    modifier = Modifier.testTag("new_eval_dialog_title")
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Registre los datos de identificación del nuevo paciente antes de crear la ficha. La evaluación actual se guardará primero en el historial.",
                    color = SlateText,
                    fontSize = 11.5.sp
                )

                OutlinedTextField(
                    value = fullName,
                    onValueChange = { raw ->
                        fullName = raw.replace("\r", " ").replace("\n", " ")
                        validationError = null
                    },
                    label = { Text("Nombre completo y apellidos") },
                    placeholder = { Text("Ej. Mateo Sebastián Paredes") },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Person,
                            contentDescription = "Nombre completo y apellidos",
                            tint = MutedText
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_eval_full_name_input")
                )

                OutlinedTextField(
                    value = dni,
                    onValueChange = { raw ->
                        dni = raw.filter { it.isDigit() }.take(8)
                        validationError = null
                    },
                    enabled = !allowPendingId,
                    label = { Text("DNI (8 dígitos)") },
                    placeholder = { Text("8 dígitos (ej. 00451234)") },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Pin,
                            contentDescription = "DNI de 8 dígitos",
                            tint = MutedText
                        )
                    },
                    supportingText = {
                        Text(
                            text = if (allowPendingId) {
                                "Sin DNI asignado (identificación pendiente; nunca se asigna un DNI ficticio)."
                            } else {
                                "Solo dígitos numéricos · ${dni.length}/8 dígitos (conserva ceros iniciales)."
                            },
                            fontSize = 10.5.sp
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_eval_dni_input")
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                        .clickable {
                            allowPendingId = !allowPendingId
                            if (allowPendingId) {
                                dni = ""
                            }
                            validationError = null
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = allowPendingId,
                        onCheckedChange = { checked ->
                            allowPendingId = checked
                            if (checked) {
                                dni = ""
                            }
                            validationError = null
                        },
                        modifier = Modifier.testTag("new_eval_pending_id_checkbox")
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Identificación pendiente (registrar sin DNI por ahora)",
                        color = DarkText,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                validationError?.let { errMsg ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("new_eval_validation_error"),
                        shape = RoundedCornerShape(10.dp),
                        color = SoftRedBg,
                        border = BorderStroke(1.dp, AlertRed)
                    ) {
                        Text(
                            text = errMsg,
                            color = DarkRedText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("new_eval_cancel_btn"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "Cancelar",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Button(
                    onClick = {
                        when (val result = onCreateEvaluation(fullName, dni, allowPendingId)) {
                            is NewEvaluationCreationResult.Success -> {
                                validationError = null
                            }
                            is NewEvaluationCreationResult.ValidationError -> {
                                validationError = result.message
                            }
                            is NewEvaluationCreationResult.PersistenceError -> {
                                validationError = result.message
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("new_eval_confirm_btn"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EdiBlueAccent,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "Crear evaluación",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    )
}

/**
 * MEDIO-06 — LOTE 3E:
 * Apartado independiente «IDENTIFICACIÓN DEL PACIENTE» al inicio de la pantalla principal EDI.
 * Reproduce fielmente el diseño de la imagen de referencia (dos campos en una misma fila horizontal:
 * ~65 % «NOMBRES Y APELLIDOS» y ~35 % «DNI» numérico de 8 dígitos preservando ceros iniciales),
 * sincronizado con `PatientIdentificationData` del `evaluationUuid` activo, autoguardado en Room
 * y bloqueado en modo consulta de solo lectura.
 */
@Composable
private fun EdiPatientIdentificationTopSection(
    state: CredEdiUiState,
    onPatientFullNameChange: (String) -> Unit,
    onPatientDniChange: (String) -> Unit
) {
    val fullNameFocusRequester = remember { FocusRequester() }
    val dniFocusRequester = remember { FocusRequester() }
    val idHeaderNavy = Color(0xFF1E2638)
    val idPillContainer = Color(0xFFEEF2F7)
    val idFieldContainer = Color(0xFFF3F6FA)
    val dniText = state.patientIdentification.clinicalRecordCode
    val isPartialDniInProgress = dniText.isNotEmpty() && dniText.length < 8

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("patient_identification_section"),
        shape = RoundedCornerShape(22.dp),
        color = Color.White,
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            // Encabezado con ícono circular de identificación + título en mayúsculas
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(idPillContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Badge,
                        contentDescription = "Identificación del paciente",
                        tint = idHeaderNavy,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "IDENTIFICACIÓN DEL PACIENTE",
                    color = idHeaderNavy,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.testTag("patient_identification_header")
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Dos campos en una sola fila horizontal: ~65 % Nombres y Apellidos | ~35 % DNI
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
                    .testTag("patient_identification_row"),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Campo izquierdo (~65 %): NOMBRES Y APELLIDOS
                Surface(
                    modifier = Modifier
                        .weight(0.65f)
                        .fillMaxHeight()
                        .clickable(enabled = !state.isReadOnlyConsultationMode) {
                            fullNameFocusRequester.requestFocus()
                        }
                        .testTag("patient_full_name_container"),
                    shape = RoundedCornerShape(16.dp),
                    color = idFieldContainer
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "NOMBRES Y APELLIDOS",
                                color = MutedText,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.3.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Outlined.Person,
                                contentDescription = "Nombres y apellidos del paciente",
                                tint = MutedText,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        BasicTextField(
                            value = state.patientIdentification.patientIdentifierOrAlias,
                            onValueChange = { raw ->
                                onPatientFullNameChange(raw.replace("\r", " ").replace("\n", " "))
                            },
                            enabled = !state.isReadOnlyConsultationMode,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Words,
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Next
                            ),
                            cursorBrush = SolidColor(idHeaderNavy),
                            textStyle = TextStyle(
                                color = DarkText,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            decorationBox = { innerTextField ->
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    if (state.patientIdentification.patientIdentifierOrAlias.isEmpty()) {
                                        Text(
                                            text = "Ej. Mateo Sebastián Paredes",
                                            color = SlateText.copy(alpha = 0.72f),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    innerTextField()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 18.dp)
                                .focusRequester(fullNameFocusRequester)
                                .testTag("patient_full_name_input")
                        )
                    }
                }

                // Campo derecho (~35 %): DNI (numérico, máx. 8 dígitos, preserva ceros iniciales y entradas parciales)
                Surface(
                    modifier = Modifier
                        .weight(0.35f)
                        .fillMaxHeight()
                        .clickable(enabled = !state.isReadOnlyConsultationMode) {
                            dniFocusRequester.requestFocus()
                        }
                        .testTag("patient_dni_container"),
                    shape = RoundedCornerShape(16.dp),
                    color = idFieldContainer
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "DNI",
                                color = MutedText,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.3.sp,
                                maxLines = 1
                            )
                            Icon(
                                imageVector = Icons.Outlined.Pin,
                                contentDescription = "DNI del paciente",
                                tint = MutedText,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        BasicTextField(
                            value = dniText,
                            onValueChange = { raw ->
                                onPatientDniChange(raw.filter { it.isDigit() }.take(8))
                            },
                            enabled = !state.isReadOnlyConsultationMode,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done
                            ),
                            cursorBrush = SolidColor(idHeaderNavy),
                            textStyle = TextStyle(
                                color = DarkText,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            decorationBox = { innerTextField ->
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    if (dniText.isEmpty()) {
                                        Text(
                                            text = "8 dígitos",
                                            color = SlateText.copy(alpha = 0.72f),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                    innerTextField()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 18.dp)
                                .focusRequester(dniFocusRequester)
                                .testTag("patient_dni_input")
                        )
                    }
                }
            }

            if (isPartialDniInProgress) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "DNI en ingreso: ${dniText.length}/8 dígitos registrados (complete los 8 dígitos numéricos).",
                    color = WarningText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.testTag("patient_dni_partial_validation_hint")
                )
            }
        }
    }
}

private fun areaStatusColors(status: EdiAreaColorResult): Pair<Color, Color> = when (status) {
    EdiAreaColorResult.VERDE -> SoftGreenPill to DarkGreenPill
    EdiAreaColorResult.AMARILLO -> SoftAmberPill to WarningText
    EdiAreaColorResult.ROJO -> SoftRedBg to DarkRedText
    EdiAreaColorResult.INCOMPLETE -> SoftGrayBg to SlateText
    EdiAreaColorResult.NOT_APPLICABLE -> SoftGrayBg to MutedText
}
