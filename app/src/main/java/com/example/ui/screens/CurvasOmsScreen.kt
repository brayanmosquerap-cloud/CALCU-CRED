package com.example.ui.screens

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ClassificationSeverity
import com.example.model.CredEdiUiState
import com.example.model.CurvaIndicador
import com.example.model.CutoffBand
import com.example.model.CutoffRowSpec
import com.example.model.IndicatorEvaluation
import com.example.model.SexoBiologico
import com.example.ui.components.CheckBoxOutlineIcon
import com.example.ui.components.TrendLineIcon
import com.example.ui.theme.AlertRed
import com.example.ui.theme.BorderSoftGreen
import com.example.ui.theme.BorderSoftRed
import com.example.ui.theme.BorderSubtleGray
import com.example.ui.theme.ClinicalGreen
import com.example.ui.theme.DarkGreenPill
import com.example.ui.theme.DarkRedText
import com.example.ui.theme.DarkText
import com.example.ui.theme.MutedText
import com.example.ui.theme.SlateText
import com.example.ui.theme.SoftAmberBg
import com.example.ui.theme.SoftGrayBg
import com.example.ui.theme.SoftGreenBg
import com.example.ui.theme.SoftRedBg
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WarningText
import com.example.viewmodel.formatSignedDecimal
import kotlin.math.abs

// Exact Palette from curvas_oms_251c1c24.png
private val CurvasScreenBg = Color(0xFFEEF2F7)
private val CurvasNavy = Color(0xFF1E2638)
private val CurvasActiveTabDark = Color(0xFF2B2F31)
private val CurvasSelectorBg = Color(0xFFF3F6FA)

@Composable
fun CurvasOmsScreen(
    state: CredEdiUiState,
    onSelectIndicador: (CurvaIndicador) -> Unit,
    onContinueToDiagnostico: () -> Unit,
    modifier: Modifier = Modifier
) {
    val eval = state.currentCurvaEvaluation
    val isGirl = state.sexo == SexoBiologico.NINA

    // Gender-specific chart card & canvas colors (Blue for Niño matching curvas oms.png, Pink for Niña)
    val chartOuterCardBg = if (isGirl) Color(0xFFFFF3F7) else Color(0xFFF0F8FF)
    val chartOuterCardBorder = if (isGirl) Color(0xFFFBCFE8) else Color(0xFFD4ECFF)
    val chartIconCircleBg = if (isGirl) Color(0xFFFCE0EC) else Color(0xFFDDF0FF)
    val chartAccentColor = if (isGirl) Color(0xFFD81B60) else Color(0xFF0284C7)
    val chartSummaryBorder = if (isGirl) Color(0xFFFBCFE8) else Color(0xFFD8EEFE)
    val chartCanvasSurfaceBg = if (isGirl) Color(0xFFFDF0F6) else Color(0xFFEBF6FF)
    val chartCanvasBorder = if (isGirl) Color(0xFFF9A8D4) else Color(0xFFBAE6FD)
    val chartPlotFillColor = if (isGirl) Color(0xFFFCE7F1) else Color(0xFFE6F4FF)
    val chartGridColor = if (isGirl) Color(0xFFF5D0E2) else Color(0xFFD5E9FA)
    val chartAxisTextColor = if (isGirl) Color(0xFF834865) else Color(0xFF64829E)
    val chartReferenceLineColor = if (isGirl) Color(0xFFB56584) else Color(0xFF7B93AC)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CurvasScreenBg)
    ) {
        // Top Header Bar (Matches curvas_oms_251c1c24.png: Left circular button + Centered Title, no right icon)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = Color.White,
                shadowElevation = 1.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    TrendLineIcon(tint = CurvasNavy, modifier = Modifier.size(19.dp))
                }
            }

            Text(
                text = "Curvas de Crecimiento OMS",
                color = CurvasNavy,
                fontSize = 17.5.sp,
                fontWeight = FontWeight.ExtraBold
            )

            // Balancing spacer so title stays centered as in curvas oms.png
            Spacer(modifier = Modifier.size(40.dp))
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // =========================================================================
            // CARD 1: EDAD CALCULADA: 2 meses & 5-TAB INDICATOR SELECTOR
            // =========================================================================
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                color = Color.White,
                shadowElevation = 1.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFE0F2FE)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.CalendarToday,
                                    contentDescription = null,
                                    tint = Color(0xFF0284C7),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = buildAnnotatedString {
                                        withStyle(
                                            SpanStyle(
                                                color = MutedText,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                letterSpacing = 0.4.sp
                                            )
                                        ) {
                                            append("EDAD CALCULADA:  ")
                                        }
                                        withStyle(
                                            SpanStyle(
                                                color = DarkText,
                                                fontSize = 15.5.sp,
                                                fontWeight = FontWeight.Black
                                            )
                                        ) {
                                            append(state.edadEfectivaAMD)
                                        }
                                    },
                                    modifier = Modifier.testTag("curva_edad_calculada_text")
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${state.edadEfectivaTipoLabel} · Grupo OMS: ${state.grupoEtarioOmsClasificado}",
                                    color = MutedText,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.testTag("curva_grupo_clasificado_subtitle")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (state.usaEdadCorregida || state.isAgeDependentEvaluationBlocked) SoftAmberBg else Color(0xFFE0F2FE),
                            border = BorderStroke(
                                1.dp,
                                if (state.usaEdadCorregida || state.isAgeDependentEvaluationBlocked) WarningAmber.copy(alpha = 0.5f) else Color(0xFFBAE6FD)
                            )
                        ) {
                            Text(
                                text = when {
                                    state.isBirthMaturityConfirmationPendingInUnder24m -> "Confirmar cond."
                                    state.isAgeDependentEvaluationBlocked -> "EC Pendiente"
                                    state.usaEdadCorregida -> "EC Corregida"
                                    else -> "Cronológica"
                                },
                                color = if (state.usaEdadCorregida || state.isAgeDependentEvaluationBlocked) WarningText else Color(0xFF0369A1),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                            )
                        }
                    }

                    if (state.isAgeDependentEvaluationBlocked) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("curvas_pending_corrected_age_banner"),
                            shape = RoundedCornerShape(14.dp),
                            color = SoftAmberBg,
                            border = BorderStroke(1.dp, WarningAmber.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = when {
                                    state.ediHasGestationalContradiction ->
                                        "⚠ Contradicción gestacional: confirme la condición al nacer y la EG exacta para obtener la clasificación OMS definitiva."
                                    state.isBirthMaturityConfirmationPendingInUnder24m ->
                                        "⚠ CONDICIÓN AL NACER PENDIENTE DE CONFIRMACIÓN: menor de 24 meses (${state.edadCronologicaAMD}) sin antecedente gestacional confirmado. Confirme si nació a término o prematuro para obtener la clasificación OMS definitiva por edad."
                                    else ->
                                        "⚠ EDAD CORREGIDA PENDIENTE: pretérmino confirmado (< 24 meses) sin EG exacta registrada. La clasificación por edad requiere confirmar las semanas de gestación."
                                },
                                color = WarningText,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    } else if (!eval.isReferenceAvailable) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("curvas_out_of_range_banner"),
                            shape = RoundedCornerShape(14.dp),
                            color = SoftGrayBg,
                            border = BorderStroke(1.dp, BorderSubtleGray)
                        ) {
                            Text(
                                text = "ℹ ${eval.unavailabilityTitle}: ${eval.unavailabilityReason}",
                                color = SlateText,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 5-Column Segmented Indicator Selector (P/E, T/E, P/T, IMC/E, PC/E)
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        color = CurvasSelectorBg
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(5.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CurvaIndicador.entries.forEach { indicador ->
                                val isSelected = state.selectedIndicador == indicador
                                val topCode = when (indicador) {
                                    CurvaIndicador.IMCE -> "IMC/E"
                                    else -> indicador.shortCode
                                }
                                val bottomSub = when (indicador) {
                                    CurvaIndicador.PE -> "Peso"
                                    CurvaIndicador.TE -> "Talla"
                                    CurvaIndicador.PT -> "Proporción"
                                    CurvaIndicador.IMCE -> "Masa"
                                    CurvaIndicador.PCE -> "Cefálico"
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(15.dp))
                                        .background(if (isSelected) CurvasActiveTabDark else Color.Transparent)
                                        .clickable { onSelectIndicador(indicador) }
                                        .padding(vertical = 8.dp, horizontal = 2.dp)
                                        .testTag("indicador_${indicador.name.lowercase()}"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = topCode,
                                            color = if (isSelected) Color.White else SlateText,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(1.dp))
                                        Text(
                                            text = bottomSub,
                                            color = if (isSelected) Color.White.copy(alpha = 0.85f) else MutedText,
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // =========================================================================
            // CARD 2: PATRÓN OMS + SUMMARY BANNER + WHO Z-SCORE CHART + LEGEND
            // =========================================================================
            val isSelectedIndicatorMissingMeasurement = state.isOmsAgeRangeAvailable && when (state.selectedIndicador) {
                CurvaIndicador.PE -> !state.hasPesoIngresado
                CurvaIndicador.TE -> !state.hasLongitudIngresada
                CurvaIndicador.PT -> !state.hasPesoIngresado || !state.hasLongitudIngresada
                CurvaIndicador.IMCE -> !state.hasPesoIngresado || !state.hasLongitudIngresada
                CurvaIndicador.PCE -> !state.hasPerimetroCefalicoIngresado
            }
            val isSelectedIndicatorOutOfRange = !eval.isReferenceAvailable || isSelectedIndicatorMissingMeasurement
            val isSelectedIndicatorBlockedByAge = !isSelectedIndicatorMissingMeasurement && when (state.selectedIndicador) {
                CurvaIndicador.PT -> state.isCorrectedAgePending || state.ediHasGestationalContradiction
                else -> state.isAgeDependentEvaluationBlocked
            }
            val isSelectedIndicatorPendingBirthMaturity = !isSelectedIndicatorMissingMeasurement &&
                state.selectedIndicador != CurvaIndicador.PT && state.isBirthMaturityConfirmationPendingInUnder24m

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                color = chartOuterCardBg,
                border = BorderStroke(1.dp, chartOuterCardBorder),
                shadowElevation = 1.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    // Header row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(chartIconCircleBg),
                                contentAlignment = Alignment.Center
                            ) {
                                TrendLineIcon(
                                    tint = if (isSelectedIndicatorOutOfRange) SlateText else chartAccentColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = eval.grupoEtarioTitle,
                                    color = DarkText,
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.testTag("curva_patron_header_title")
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = state.selectedIndicador.fullTitle,
                                    color = MutedText,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White.copy(alpha = 0.85f),
                            border = BorderStroke(1.dp, Color.White)
                        ) {
                            Text(
                                text = state.selectedIndicador.unitLabel.replace(" / ", " /\n"),
                                color = DarkText,
                                fontSize = 10.5.sp,
                                lineHeight = 13.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Current Patient Value Summary Banner (White rounded card inside tinted Card 2)
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, chartSummaryBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                val statusDotColor = when {
                                    isSelectedIndicatorOutOfRange -> SlateText
                                    isSelectedIndicatorBlockedByAge -> WarningAmber
                                    else -> when (eval.severity) {
                                        ClassificationSeverity.NORMAL -> chartAccentColor
                                        ClassificationSeverity.WARNING -> WarningAmber
                                        ClassificationSeverity.SEVERE -> AlertRed
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .size(11.dp)
                                        .clip(CircleShape)
                                        .background(statusDotColor)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = when {
                                            isSelectedIndicatorMissingMeasurement ->
                                                "Sin medición (${eval.patientValueFormatted} ${eval.unit}) · ${eval.unavailabilityTitle}"
                                            isSelectedIndicatorOutOfRange ->
                                                "${eval.patientValueFormatted} ${eval.unit} · ${eval.unavailabilityTitle}"
                                            isSelectedIndicatorPendingBirthMaturity ->
                                                "${eval.patientValueFormatted} ${eval.unit} · Clasificación pendiente de confirmar condición al nacer (Provisional cronológica: ${eval.shortClassification})"
                                            isSelectedIndicatorBlockedByAge ->
                                                "${eval.patientValueFormatted} ${eval.unit} · Clasificación pendiente de Edad Corregida"
                                            else ->
                                                "${eval.patientValueFormatted} ${eval.unit} (${eval.zScore.formatSignedDecimal(1)} DE) · ${eval.shortClassification}"
                                        },
                                        color = DarkText,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.testTag("curva_summary_headline")
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    val subLine = when {
                                        isSelectedIndicatorOutOfRange ->
                                            eval.unavailabilityReason
                                        isSelectedIndicatorPendingBirthMaturity ->
                                            "Medición registrada conservada · Confirme si nació a término o prematuro (< 24m)"
                                        isSelectedIndicatorBlockedByAge ->
                                            "Medición registrada conservada · Confirme la Edad Gestacional exacta"
                                        else -> when (state.selectedIndicador) {
                                            CurvaIndicador.PE -> "Control a ${state.edadEfectivaAMD} (${state.edadEfectivaTipoLabel}) · Velocidad: +${state.globalDiagnosis.gananciaGramosMes} g/mes"
                                            CurvaIndicador.PCE -> "Control a ${state.edadEfectivaAMD} (${state.edadEfectivaTipoLabel}) · Fontanela normotensa"
                                            else -> "Control a ${state.edadEfectivaAMD} (${state.edadEfectivaTipoLabel}) · ${eval.clinicalDetail}"
                                        }
                                    }
                                    Text(
                                        text = subLine,
                                        color = MutedText,
                                        fontSize = 11.sp,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = if (isSelectedIndicatorOutOfRange) SoftGrayBg else SoftGreenBg,
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelectedIndicatorOutOfRange) BorderSubtleGray else BorderSoftGreen
                                )
                            ) {
                                Text(
                                    text = when {
                                        isSelectedIndicatorMissingMeasurement -> "Sin medición"
                                        isSelectedIndicatorOutOfRange -> "Fuera de rango"
                                        isSelectedIndicatorPendingBirthMaturity -> "Pendiente cond."
                                        isSelectedIndicatorBlockedByAge -> "Pendiente EC"
                                        else -> "Mediana: ${eval.medianFormatted}"
                                    },
                                    color = if (isSelectedIndicatorOutOfRange) SlateText else DarkGreenPill,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // WHO Z-Score Chart Canvas Container (or neutral out-of-range notice without plotting clamped point)
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(295.dp)
                            .testTag("who_growth_chart_surface"),
                        shape = RoundedCornerShape(18.dp),
                        color = if (isSelectedIndicatorOutOfRange) SoftGrayBg else chartCanvasSurfaceBg,
                        border = BorderStroke(
                            1.dp,
                            if (isSelectedIndicatorOutOfRange) BorderSubtleGray else chartCanvasBorder
                        )
                    ) {
                        if (isSelectedIndicatorOutOfRange) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(20.dp)
                                    .testTag("curvas_chart_unavailable_card"),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = eval.unavailabilityTitle,
                                    color = DarkText,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = eval.unavailabilityReason,
                                    color = SlateText,
                                    fontSize = 12.sp,
                                    lineHeight = 17.sp,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color.White,
                                    border = BorderStroke(1.dp, BorderSubtleGray)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "Rango OMS implementado: ${eval.implementedRangeLabel}",
                                            color = DarkText,
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = if (isSelectedIndicatorMissingMeasurement) {
                                                "Medición actual: -- (Sin medición) · Edad: ${eval.edadAMD}"
                                            } else {
                                                "Dato conservado: ${eval.patientValueFormatted} ${eval.unit} · Edad: ${eval.edadAMD}"
                                            },
                                            color = MutedText,
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.SemiBold,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        } else {
                            WhoGrowthChartCanvas(
                                evaluation = eval,
                                plotBgColor = chartPlotFillColor,
                                gridColor = chartGridColor,
                                axisTextColor = chartAxisTextColor,
                                primaryAccentColor = chartAccentColor,
                                navyAccentColor = CurvasNavy,
                                referenceLineColor = chartReferenceLineColor,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(10.dp)
                            )
                        }
                    }

                    if (!isSelectedIndicatorOutOfRange) {
                        Spacer(modifier = Modifier.height(14.dp))

                        // Clean Bottom Legend Row (Matches curvas_oms_251c1c24.png)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LegendItem(
                                color = ClinicalGreen,
                                text = "0 DE: Mediana",
                                textColor = DarkText,
                                isBold = true,
                                pillHeight = 5.dp
                            )
                            LegendItem(
                                color = WarningAmber,
                                text = "±2 DE: Alerta",
                                textColor = SlateText,
                                isBold = false,
                                pillHeight = 5.dp
                            )
                            LegendItem(
                                color = AlertRed,
                                text = "±3 DE: Severo",
                                textColor = SlateText,
                                isBold = false,
                                pillHeight = 2.dp
                            )
                        }
                    }
                }
            }

            // =========================================================================
            // CARD 3: PUNTOS DE CORTE OMS (Matches curvas_oms_251c1c24.png)
            // =========================================================================
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                color = Color.White,
                shadowElevation = 1.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    // Header row: Checkbox icon + Puntos de Corte OMS
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CheckBoxOutlineIcon(
                            tint = CurvasNavy,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = when {
                                isSelectedIndicatorOutOfRange ->
                                    eval.cutoffHeaderTitle
                                isSelectedIndicatorPendingBirthMaturity ->
                                    "Puntos de Corte OMS (Condición al nacer pendiente)"
                                isSelectedIndicatorBlockedByAge ->
                                    "Puntos de Corte OMS (Edad Corregida Pendiente)"
                                else ->
                                    eval.cutoffHeaderTitle
                            },
                            color = DarkText,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (isSelectedIndicatorOutOfRange || eval.cutoffRows.isEmpty()) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("curvas_cutoff_unavailable_card"),
                            shape = RoundedCornerShape(16.dp),
                            color = SoftGrayBg,
                            border = BorderStroke(1.dp, BorderSubtleGray)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp)
                            ) {
                                Text(
                                    text = if (isSelectedIndicatorMissingMeasurement) {
                                        when (state.selectedIndicador) {
                                            CurvaIndicador.PCE -> "Sin clasificación cefálica (Sin medición)"
                                            CurvaIndicador.PE -> "Sin clasificación de Peso/Edad (Sin medición)"
                                            CurvaIndicador.TE -> "Sin clasificación de Talla/Edad (Sin medición)"
                                            CurvaIndicador.PT -> "Sin clasificación de Peso/Talla (Sin medición)"
                                            CurvaIndicador.IMCE -> "Sin clasificación de IMC/Edad (Sin medición)"
                                        }
                                    } else {
                                        "Sin puntos de corte OMS aplicables"
                                    },
                                    color = DarkText,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (isSelectedIndicatorMissingMeasurement) {
                                        "${eval.patientValueFormatted} ${eval.unit} · ${eval.unavailabilityTitle}"
                                    } else {
                                        "${eval.patientValueFormatted} ${eval.unit} · Sin referencia OMS (${eval.implementedRangeLabel})"
                                    },
                                    color = SlateText,
                                    fontSize = 11.5.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = eval.unavailabilityReason,
                                    color = MutedText,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    } else {
                        // Active classification card FIRST, followed by compact single-line reference rows
                        val selectedRow = eval.cutoffRows.firstOrNull { it.isSelected } ?: eval.cutoffRows[2]
                        val otherRows = eval.cutoffRows.filter { it != selectedRow }
                        val displayRow = when {
                            isSelectedIndicatorPendingBirthMaturity ->
                                selectedRow.copy(
                                    title = "Clasificación pendiente de confirmar condición al nacer",
                                    subtitle = "Provisional cronológica (${state.edadCronologicaAMD}): ${selectedRow.title} · Confirme si nació a término o prematuro",
                                    severity = ClassificationSeverity.WARNING
                                )
                            isSelectedIndicatorBlockedByAge ->
                                selectedRow.copy(
                                    title = "Clasificación pendiente de Edad Corregida",
                                    subtitle = "Confirme la Edad Gestacional exacta para calcular el puntaje Z definitivo",
                                    severity = ClassificationSeverity.WARNING
                                )
                            else ->
                                selectedRow
                        }

                        ActiveCutoffCard(
                            row = displayRow,
                            patientBadgeText = when {
                                isSelectedIndicatorPendingBirthMaturity ->
                                    "${eval.patientValueFormatted} ${eval.unit} · Condición pendiente"
                                isSelectedIndicatorBlockedByAge ->
                                    "${eval.patientValueFormatted} ${eval.unit} · EC Pendiente"
                                else ->
                                    "${eval.patientValueFormatted} ${eval.unit} · Z = ${eval.zScore.formatSignedDecimal(1)} DE"
                            }
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            otherRows.forEach { row ->
                                CompactCutoffReferenceRow(row = row)
                            }
                        }
                    }
                }
            }

            // =========================================================================
            // BOTTOM CTA BUTTON: Continuar a Diagnóstico Nutricional
            // =========================================================================
            Button(
                onClick = onContinueToDiagnostico,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .testTag("continuar_diagnostico_button"),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = CurvasNavy),
                contentPadding = PaddingValues(horizontal = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            CheckBoxOutlineIcon(tint = Color.White, modifier = Modifier.size(19.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Continuar a Diagnóstico\nNutricional",
                            color = Color.White,
                            fontSize = 14.5.sp,
                            lineHeight = 18.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Continuar",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
private fun ActiveCutoffCard(
    row: CutoffRowSpec,
    patientBadgeText: String
) {
    val (bgCol, borderCol, accentCol, darkTextCol) = when (row.severity) {
        ClassificationSeverity.NORMAL -> listOf(
            Color(0xFFF0FDF4),
            Color(0xFF16A34A),
            ClinicalGreen,
            DarkGreenPill
        )
        ClassificationSeverity.WARNING -> listOf(
            SoftAmberBg,
            WarningAmber,
            WarningAmber,
            WarningText
        )
        ClassificationSeverity.SEVERE -> listOf(
            SoftRedBg,
            AlertRed,
            AlertRed,
            DarkRedText
        )
    }

    val subtitleText = if (row.band == CutoffBand.NORMAL_RANGE) {
        "Rango normativo esperado (-2 a +2 DE)"
    } else {
        row.subtitle
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("active_cutoff_card"),
        shape = RoundedCornerShape(16.dp),
        color = bgCol,
        border = BorderStroke(1.2.dp, borderCol)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(accentCol),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = row.title,
                        color = darkTextCol,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.height(5.dp))
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = accentCol
                    ) {
                        Text(
                            text = patientBadgeText,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(5.dp))
                    Text(
                        text = subtitleText,
                        color = accentCol,
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = row.rangeText,
                color = accentCol,
                fontSize = 12.5.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Composable
private fun CompactCutoffReferenceRow(
    row: CutoffRowSpec
) {
    val isBelowMinus3 = row.band == CutoffBand.BELOW_MINUS_3
    val bgCol = if (isBelowMinus3) SoftRedBg else Color(0xFFF8FAFC)
    val borderCol = if (isBelowMinus3) BorderSoftRed else Color(0xFFE8EEF5)

    val dotCol = when (row.band) {
        CutoffBand.ABOVE_PLUS_3 -> Color(0xFFF87171)
        CutoffBand.PLUS_2_TO_3, CutoffBand.MINUS_2_TO_3 -> Color(0xFFFB923C)
        CutoffBand.NORMAL_RANGE -> ClinicalGreen
        CutoffBand.BELOW_MINUS_3 -> Color(0xFFEF4444)
    }

    val singleLineTitle = when (row.band) {
        CutoffBand.ABOVE_PLUS_3 -> "${row.title} (> +3 DE)"
        CutoffBand.PLUS_2_TO_3 -> "${row.title} (+2 a +3 DE)"
        CutoffBand.NORMAL_RANGE -> "${row.title} (-2 a +2 DE)"
        CutoffBand.MINUS_2_TO_3 -> {
            val shortTitle = row.title.replace("Riesgo Desnutrición", "Riesgo")
            "$shortTitle (< -2 a -3 DE)"
        }
        CutoffBand.BELOW_MINUS_3 -> "${row.title} (< -3 DE)"
    }

    val titleColor = if (isBelowMinus3) DarkRedText else DarkText
    val rangeColor = when (row.band) {
        CutoffBand.BELOW_MINUS_3 -> DarkRedText
        CutoffBand.NORMAL_RANGE -> DarkGreenPill
        else -> WarningText
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = bgCol,
        border = BorderStroke(1.dp, borderCol)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(dotCol)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = singleLineTitle,
                    color = titleColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = row.rangeText,
                color = rangeColor,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Composable
private fun LegendItem(
    color: Color,
    text: String,
    textColor: Color,
    isBold: Boolean,
    pillHeight: androidx.compose.ui.unit.Dp
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .width(12.dp)
                .height(pillHeight)
                .clip(RoundedCornerShape(50))
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = if (isBold) FontWeight.ExtraBold else FontWeight.SemiBold
        )
    }
}

@Composable
private fun WhoGrowthChartCanvas(
    evaluation: IndicatorEvaluation,
    plotBgColor: Color,
    gridColor: Color,
    axisTextColor: Color,
    primaryAccentColor: Color,
    navyAccentColor: Color,
    referenceLineColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val leftPad = 36.dp.toPx()
        val rightPad = 26.dp.toPx()
        val topPad = 14.dp.toPx()
        val bottomPad = 36.dp.toPx()

        val chartW = size.width - leftPad - rightPad
        val chartH = size.height - topPad - bottomPad

        val xSpan = (evaluation.xMax - evaluation.xMin).coerceAtLeast(1f)
        val ySpan = (evaluation.yMax - evaluation.yMin).coerceAtLeast(1f)

        fun mapX(xVal: Float): Float =
            leftPad + ((xVal.coerceIn(evaluation.xMin, evaluation.xMax) - evaluation.xMin) / xSpan) * chartW

        fun mapY(yVal: Float): Float =
            topPad + chartH - ((yVal.coerceIn(evaluation.yMin, evaluation.yMax) - evaluation.yMin) / ySpan) * chartH

        // Inner plot area background (WHO Blue for Niño / WHO Pink for Niña)
        drawRect(
            color = plotBgColor,
            topLeft = Offset(leftPad, topPad),
            size = Size(chartW, chartH)
        )

        val axisTextPaint = Paint().apply {
            isAntiAlias = true
            color = axisTextColor.toArgb()
            textSize = 9.5.dp.toPx()
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }

        // Horizontal grid lines & Y-axis labels
        evaluation.yStepValues.forEach { yStep ->
            val y = mapY(yStep.toFloat())
            drawLine(
                color = gridColor,
                start = Offset(leftPad, y),
                end = Offset(leftPad + chartW, y),
                strokeWidth = 1.dp.toPx()
            )
            axisTextPaint.textAlign = Paint.Align.RIGHT
            drawContext.canvas.nativeCanvas.drawText(
                yStep.toString(),
                leftPad - 6.dp.toPx(),
                y + 3.dp.toPx(),
                axisTextPaint
            )
        }

        // Vertical grid lines & X-axis labels
        val closestTickX = evaluation.xTickLabels.minByOrNull { abs(it.first - evaluation.patientX) }?.first
        evaluation.xTickLabels.forEach { (xVal, label) ->
            val x = mapX(xVal)
            drawLine(
                color = gridColor,
                start = Offset(x, topPad),
                end = Offset(x, topPad + chartH),
                strokeWidth = 1.dp.toPx()
            )
            val isHighlighted = closestTickX != null && abs(xVal - closestTickX) < 0.01f
            val labelPaint = Paint(axisTextPaint).apply {
                textAlign = Paint.Align.CENTER
                if (isHighlighted) {
                    color = primaryAccentColor.toArgb()
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
            }
            drawContext.canvas.nativeCanvas.drawText(
                label,
                x,
                topPad + chartH + 14.dp.toPx(),
                labelPaint
            )
        }

        // X-axis title
        val titlePaint = Paint(axisTextPaint).apply {
            textAlign = Paint.Align.CENTER
            color = Color(0xFF475569).toArgb()
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 9.5.dp.toPx()
        }
        drawContext.canvas.nativeCanvas.drawText(
            evaluation.xAxisLabel,
            leftPad + chartW * 0.5f,
            size.height - 3.dp.toPx(),
            titlePaint
        )

        // Y-axis rotated title
        drawContext.canvas.nativeCanvas.save()
        drawContext.canvas.nativeCanvas.rotate(
            -90f,
            10.dp.toPx(),
            topPad + chartH * 0.5f
        )
        drawContext.canvas.nativeCanvas.drawText(
            evaluation.yAxisLabel,
            10.dp.toPx(),
            topPad + chartH * 0.5f,
            titlePaint
        )
        drawContext.canvas.nativeCanvas.restore()

        // Dynamic Classified Group Band Shading (highlights the Z-score zone where the patient is classified)
        val curvesByZ = evaluation.curves.associateBy { it.zLevel }
        val activeZoneFillColor = when (evaluation.severity) {
            ClassificationSeverity.NORMAL -> Color(0xFF16A34A).copy(alpha = 0.11f)
            ClassificationSeverity.WARNING -> Color(0xFFF97316).copy(alpha = 0.14f)
            ClassificationSeverity.SEVERE -> Color(0xFFEF4444).copy(alpha = 0.15f)
        }

        val (upperCurvePts, lowerCurvePts) = when (evaluation.activeBand) {
            CutoffBand.ABOVE_PLUS_3 -> {
                val c3 = curvesByZ[3]?.points.orEmpty()
                val topEdge = c3.map { (px, _) -> px to evaluation.yMax }
                topEdge to c3
            }
            CutoffBand.PLUS_2_TO_3 -> {
                curvesByZ[3]?.points.orEmpty() to curvesByZ[2]?.points.orEmpty()
            }
            CutoffBand.NORMAL_RANGE -> {
                curvesByZ[2]?.points.orEmpty() to curvesByZ[-2]?.points.orEmpty()
            }
            CutoffBand.MINUS_2_TO_3 -> {
                curvesByZ[-2]?.points.orEmpty() to curvesByZ[-3]?.points.orEmpty()
            }
            CutoffBand.BELOW_MINUS_3 -> {
                val cm3 = curvesByZ[-3]?.points.orEmpty()
                val bottomEdge = cm3.map { (px, _) -> px to evaluation.yMin }
                cm3 to bottomEdge
            }
        }

        if (upperCurvePts.size >= 2 && lowerCurvePts.size >= 2) {
            val bandPath = Path().apply {
                val firstUp = upperCurvePts.first()
                moveTo(mapX(firstUp.first), mapY(firstUp.second))
                upperCurvePts.drop(1).forEach { (px, py) ->
                    lineTo(mapX(px), mapY(py))
                }
                lowerCurvePts.asReversed().forEach { (px, py) ->
                    lineTo(mapX(px), mapY(py))
                }
                close()
            }
            drawPath(
                path = bandPath,
                color = activeZoneFillColor
            )
        }

        val hasValidPatientPoint = evaluation.patientX.isFinite() &&
            evaluation.patientY.isFinite() &&
            evaluation.zScore.isFinite()
        val classifiedPointColor = when (evaluation.severity) {
            ClassificationSeverity.NORMAL -> ClinicalGreen
            ClassificationSeverity.WARNING -> WarningAmber
            ClassificationSeverity.SEVERE -> AlertRed
        }
        if (hasValidPatientPoint) {
            // Vertical dashed line at current patient X position
            val xRef = mapX(evaluation.patientX)
            drawLine(
                color = primaryAccentColor.copy(alpha = 0.75f),
                start = Offset(xRef, topPad),
                end = Offset(xRef, topPad + chartH),
                strokeWidth = 1.3.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
            )
        }

        // WHO Z-score curves: (+3, +2, +1, 0, -1, -2, -3)
        evaluation.curves.forEach { curve ->
            val (curveColor, isDashed, strokeDp) = when (curve.zLevel) {
                3, -3 -> Triple(Color(0xFFEF4444), true, 1.3f)
                2, -2 -> Triple(Color(0xFFF97316), false, 1.6f)
                1, -1 -> Triple(referenceLineColor, true, 1.1f)
                else -> Triple(Color(0xFF16A34A), false, 2.3f)
            }

            val path = Path()
            val mapped = curve.points.map { (px, py) -> Offset(mapX(px), mapY(py)) }
            if (mapped.isNotEmpty()) {
                path.moveTo(mapped.first().x, mapped.first().y)
                for (i in 0 until mapped.size - 1) {
                    val p0 = mapped[i]
                    val p1 = mapped[i + 1]
                    val cx1 = p0.x + (p1.x - p0.x) * 0.45f
                    val cy1 = p0.y + (p1.y - p0.y) * 0.55f
                    val cx2 = p0.x + (p1.x - p0.x) * 0.65f
                    val cy2 = p1.y
                    path.cubicTo(cx1, cy1, cx2, cy2, p1.x, p1.y)
                }
            }
            drawPath(
                path = path,
                color = curveColor,
                style = Stroke(
                    width = strokeDp.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                    pathEffect = if (isDashed) {
                        PathEffect.dashPathEffect(floatArrayOf(5f, 5f), 0f)
                    } else {
                        null
                    }
                )
            )

            // End label (+3, +2, +1, 0, -1, -2, -3)
            if (mapped.isNotEmpty()) {
                val endPt = mapped.last()
                val endPaint = Paint(axisTextPaint).apply {
                    textAlign = Paint.Align.LEFT
                    color = curveColor.toArgb()
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textSize = 9.dp.toPx()
                }
                drawContext.canvas.nativeCanvas.drawText(
                    curve.label,
                    endPt.x + 4.dp.toPx(),
                    endPt.y + 3.dp.toPx(),
                    endPaint
                )
            }
        }

        if (hasValidPatientPoint) {
            val currPt = Offset(mapX(evaluation.patientX), mapY(evaluation.patientY))
            if (evaluation.previousX.isFinite() && evaluation.previousY.isFinite()) {
                // Patient Trajectory: Previous point -> Current point
                val prevPt = Offset(mapX(evaluation.previousX), mapY(evaluation.previousY))
                drawLine(
                    color = navyAccentColor,
                    start = prevPt,
                    end = currPt,
                    strokeWidth = 2.2.dp.toPx(),
                    cap = StrokeCap.Round
                )

                // Previous point (hollow white circle with navyAccentColor stroke)
                drawCircle(
                    color = Color.White,
                    radius = 4.5.dp.toPx(),
                    center = prevPt
                )
                drawCircle(
                    color = navyAccentColor,
                    radius = 4.5.dp.toPx(),
                    center = prevPt,
                    style = Stroke(width = 2.dp.toPx())
                )
            }

            // Current point halo + white ring + solid navy/severity center
            drawCircle(
                color = classifiedPointColor.copy(alpha = 0.25f),
                radius = 9.dp.toPx(),
                center = currPt
            )
            drawCircle(
                color = Color.White,
                radius = 6.dp.toPx(),
                center = currPt
            )
            drawCircle(
                color = navyAccentColor,
                radius = 4.5.dp.toPx(),
                center = currPt
            )

            // On-chart floating callout pill showing classified group & exact age/coordinate
            val calloutText = "${evaluation.shortClassification} (${evaluation.edadAMD})"
            val calloutPaint = Paint(axisTextPaint).apply {
                textAlign = Paint.Align.CENTER
                color = Color.White.toArgb()
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textSize = 8.5.dp.toPx()
            }
            val textWidth = calloutPaint.measureText(calloutText)
            val pillW = textWidth + 12.dp.toPx()
            val pillH = 16.dp.toPx()
            val rawPillX = (currPt.x - pillW / 2f).coerceIn(leftPad + 2.dp.toPx(), leftPad + chartW - pillW - 2.dp.toPx())
            val rawPillY = if (currPt.y - pillH - 8.dp.toPx() >= topPad + 2.dp.toPx()) {
                currPt.y - pillH - 8.dp.toPx()
            } else {
                (currPt.y + 8.dp.toPx()).coerceAtMost(topPad + chartH - pillH - 2.dp.toPx())
            }

            drawRoundRect(
                color = classifiedPointColor,
                topLeft = Offset(rawPillX, rawPillY),
                size = Size(pillW, pillH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(pillH / 2f, pillH / 2f)
            )
            drawContext.canvas.nativeCanvas.drawText(
                calloutText,
                rawPillX + pillW / 2f,
                rawPillY + pillH * 0.68f,
                calloutPaint
            )
        }
    }
}
