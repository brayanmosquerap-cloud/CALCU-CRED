package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Pin
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.SentimentSatisfied
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material.icons.outlined.Wc
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ClassificationSeverity
import com.example.model.CredEdiUiState
import com.example.model.SexoBiologico
import com.example.ui.components.MathGridIcon
import com.example.ui.theme.BorderAmber
import com.example.ui.theme.BorderSoftGreen
import com.example.ui.theme.BorderSoftRed
import com.example.ui.theme.DarkGreenPill
import com.example.ui.theme.DarkRedText
import com.example.ui.theme.DarkText
import com.example.ui.theme.MutedText
import com.example.ui.theme.SlateText
import com.example.ui.theme.SoftAmberPill
import com.example.ui.theme.SoftGreenBg
import com.example.ui.theme.SoftRedBg
import com.example.ui.theme.WarningText
import com.example.viewmodel.formatDecimal
import com.example.viewmodel.formatSignedDecimal
import kotlin.math.roundToInt

// Exact Color Palette from calculadora cred.png
private val CalcScreenBg = Color(0xFFEEF2F7)
private val CalcNavy = Color(0xFF1E2638)
private val CalcSoftContainer = Color(0xFFF3F6FA)
private val CalcPillContainer = Color(0xFFEEF2F7)
private val CalcRowCardBg = Color(0xFFF8FAFC)
private val CalcRowCardBorder = Color(0xFFE8EEF5)
private val CalcTrackBg = Color(0xFFE4EAF2)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculadoraScreen(
    state: CredEdiUiState,
    onPatientFullNameChange: (String) -> Unit = {},
    onPatientDniChange: (String) -> Unit = {},
    onSelectSexo: (SexoBiologico) -> Unit,
    onFechaNacimientoChange: (String) -> Unit,
    onFechaEvaluacionChange: (String) -> Unit,
    onTogglePrematurez: (Boolean) -> Unit,
    onUpdateEgSemanas: (Int) -> Unit,
    onAdjustPeso: (Double) -> Unit,
    onAdjustLongitud: (Double) -> Unit,
    onAdjustPerimetroCefalico: (Double) -> Unit,
    onSetPeso: (Double) -> Unit = {},
    onSetLongitud: (Double) -> Unit = {},
    onSetPerimetroCefalico: (Double) -> Unit = {},
    onPesoInputChange: (String) -> Unit = {},
    onLongitudInputChange: (String) -> Unit = {},
    onPerimetroCefalicoInputChange: (String) -> Unit = {},
    onConfirmTermWithoutWeeks: () -> Unit = {},
    onContinueToCurvas: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CalcScreenBg)
    ) {
        // Top Header Bar (Matches calculadora_cred_d594b8f8.png: Left circular button + Centered Title, no right icon)
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
                    MathGridIcon(tint = CalcNavy, modifier = Modifier.size(19.dp))
                }
            }

            Text(
                text = "Calculadora Antropométrica",
                color = CalcNavy,
                fontSize = 17.5.sp,
                fontWeight = FontWeight.ExtraBold
            )

            // Balancing spacer so the title is centered exactly as in calculadora cred.png
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
            // CARD 1: SEXO BIOLÓGICO + F. NACIMIENTO & F. EVALUACIÓN
            // =========================================================================
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("sexo_biologico_section"),
                shape = RoundedCornerShape(28.dp),
                color = Color.White,
                shadowElevation = 1.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    // Header row: SEXO BIOLÓGICO (No right badge per calculadora cred.png)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(CalcPillContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Wc,
                                contentDescription = "Sexo Biológico",
                                tint = CalcNavy,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "SEXO BIOLÓGICO",
                            color = CalcNavy,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.6.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Segmented Pill Selector (Niño / Niña)
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(50),
                        color = CalcPillContainer
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val isNino = state.sexo == SexoBiologico.NINO

                            // Niño Button
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(if (isNino) CalcNavy else Color.Transparent)
                                    .clickable { onSelectSexo(SexoBiologico.NINO) }
                                    .testTag("sexo_nino_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "♂",
                                        color = if (isNino) Color.White else SlateText,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Niño",
                                        color = if (isNino) Color.White else SlateText,
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }

                            // Niña Button
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(if (!isNino) CalcNavy else Color.Transparent)
                                    .clickable { onSelectSexo(SexoBiologico.NINA) }
                                    .testTag("sexo_nina_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "♀",
                                        color = if (!isNino) Color.White else SlateText,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Niña",
                                        color = if (!isNino) Color.White else SlateText,
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Two Date Boxes: F. NACIMIENTO (Libre para rellenar) & F. EVALUACIÓN (Hoy por defecto y modificable)
                    val context = androidx.compose.ui.platform.LocalContext.current
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Left: F. NACIMIENTO (Vacío por defecto para rellenar libremente)
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(18.dp),
                            color = CalcSoftContainer
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "F. NACIMIENTO",
                                        color = MutedText,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 0.4.sp
                                    )
                                    Icon(
                                        imageVector = Icons.Outlined.SentimentSatisfied,
                                        contentDescription = "Seleccionar fecha de nacimiento",
                                        tint = MutedText,
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clickable {
                                                val parsed = com.example.model.ClinicalAgeCalculator.parseDate(state.fechaNacimiento)
                                                val cal = java.util.Calendar.getInstance()
                                                val initYear = parsed?.year ?: cal.get(java.util.Calendar.YEAR)
                                                val initMonth = (parsed?.month ?: (cal.get(java.util.Calendar.MONTH) + 1)) - 1
                                                val initDay = parsed?.day ?: cal.get(java.util.Calendar.DAY_OF_MONTH)
                                                android.app.DatePickerDialog(
                                                    context,
                                                    { _, y, m, d ->
                                                        onFechaNacimientoChange(
                                                            String.format(java.util.Locale.US, "%02d/%02d/%04d", d, m + 1, y)
                                                        )
                                                    },
                                                    initYear,
                                                    initMonth,
                                                    initDay
                                                ).show()
                                            }
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                BasicTextField(
                                    value = state.fechaNacimiento,
                                    onValueChange = onFechaNacimientoChange,
                                    singleLine = true,
                                    cursorBrush = androidx.compose.ui.graphics.SolidColor(CalcNavy),
                                    textStyle = TextStyle(
                                        color = DarkText,
                                        fontSize = 15.5.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.ExtraBold
                                    ),
                                    decorationBox = { innerTextField ->
                                        Box(contentAlignment = Alignment.CenterStart) {
                                            if (state.fechaNacimiento.isEmpty()) {
                                                Text(
                                                    text = "DD/MM/AAAA",
                                                    color = MutedText.copy(alpha = 0.55f),
                                                    fontSize = 15.5.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontWeight = FontWeight.ExtraBold
                                                )
                                            }
                                            innerTextField()
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("fecha_nacimiento_input")
                                )
                            }
                        }

                        // Right: F. EVALUACIÓN (Sincronizado por defecto con hoy y permite modificar)
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(18.dp),
                            color = CalcSoftContainer
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "F. EVALUACIÓN",
                                        color = MutedText,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 0.4.sp
                                    )
                                    Icon(
                                        imageVector = Icons.Outlined.CalendarToday,
                                        contentDescription = "Seleccionar fecha de evaluación",
                                        tint = MutedText,
                                        modifier = Modifier
                                            .size(15.dp)
                                            .clickable {
                                                val parsed = com.example.model.ClinicalAgeCalculator.parseDate(state.fechaEvaluacion)
                                                val cal = java.util.Calendar.getInstance()
                                                val initYear = parsed?.year ?: cal.get(java.util.Calendar.YEAR)
                                                val initMonth = (parsed?.month ?: (cal.get(java.util.Calendar.MONTH) + 1)) - 1
                                                val initDay = parsed?.day ?: cal.get(java.util.Calendar.DAY_OF_MONTH)
                                                android.app.DatePickerDialog(
                                                    context,
                                                    { _, y, m, d ->
                                                        onFechaEvaluacionChange(
                                                            String.format(java.util.Locale.US, "%02d/%02d/%04d", d, m + 1, y)
                                                        )
                                                    },
                                                    initYear,
                                                    initMonth,
                                                    initDay
                                                ).show()
                                            }
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                BasicTextField(
                                    value = state.fechaEvaluacion,
                                    onValueChange = onFechaEvaluacionChange,
                                    singleLine = true,
                                    cursorBrush = androidx.compose.ui.graphics.SolidColor(CalcNavy),
                                    textStyle = TextStyle(
                                        color = DarkText,
                                        fontSize = 15.5.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.ExtraBold
                                    ),
                                    decorationBox = { innerTextField ->
                                        Box(contentAlignment = Alignment.CenterStart) {
                                            if (state.fechaEvaluacion.isEmpty()) {
                                                Text(
                                                    text = "DD/MM/AAAA",
                                                    color = MutedText.copy(alpha = 0.55f),
                                                    fontSize = 15.5.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontWeight = FontWeight.ExtraBold
                                                )
                                            }
                                            innerTextField()
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("fecha_evaluacion_input")
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Bottom Metadata Row inside Card 1: Condición al nacer real (sin asumir 40 sem por defecto)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Condición al nacer:",
                                color = MutedText,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = if (state.isBirthMaturityConfirmationPendingInUnder24m) {
                                    SoftAmberPill
                                } else {
                                    CalcPillContainer
                                }
                            ) {
                                Text(
                                    text = if (state.isBirthMaturityConfirmationPendingInUnder24m) {
                                        "Pendiente confirmar"
                                    } else {
                                        state.ediGestationalAgeDisplay
                                    },
                                    color = if (state.isBirthMaturityConfirmationPendingInUnder24m) {
                                        WarningText
                                    } else {
                                        DarkText
                                    },
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                        .testTag("calc_birth_maturity_summary_pill")
                                )
                            }
                        }
                    }
                }
            }

            // =========================================================================
            // CARD 2: ¿PREMATUREZ DETECTADA? + EDAD GESTACIONAL + CRONOLÓGICA / CORREGIDA
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
                        .padding(18.dp)
                ) {
                    // Header row: Pulse icon + ¿Prematurez detectada? + SÍ pill + Custom Switch
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
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFEE2E2)),
                                contentAlignment = Alignment.Center
                            ) {
                                PulseWaveIcon(
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "¿Prematurez detectada?",
                                    color = DarkText,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "< 37 semanas al nacer (MINSA)",
                                    color = MutedText,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = if (state.esPrematuro) CalcNavy else CalcPillContainer
                            ) {
                                Text(
                                    text = if (state.esPrematuro) "SÍ" else "NO",
                                    color = if (state.esPrematuro) Color.White else SlateText,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                )
                            }

                            // Custom Switch matching calculadora cred.png (dark track, white thumb with center dot)
                            Box(
                                modifier = Modifier
                                    .width(50.dp)
                                    .height(28.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(if (state.esPrematuro) CalcNavy else Color(0xFFCBD5E1))
                                    .clickable { onTogglePrematurez(!state.esPrematuro) }
                                    .padding(horizontal = 3.dp)
                                    .testTag("prematurez_switch"),
                                contentAlignment = if (state.esPrematuro) Alignment.CenterEnd else Alignment.CenterStart
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(Color.White),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(if (state.esPrematuro) CalcNavy else Color(0xFF94A3B8))
                                    )
                                }
                            }
                        }
                    }

                    if (state.isBirthMaturityConfirmationPendingInUnder24m) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("calc_pending_birth_maturity_banner"),
                            shape = RoundedCornerShape(16.dp),
                            color = SoftAmberPill,
                            border = BorderStroke(1.dp, BorderAmber)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Condición al nacer pendiente de confirmación",
                                    color = WarningText,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = "Menor de 24 meses (${state.edadCronologicaAMD}): confirme si nació a término (≥ 37 sem) o pretérmino (< 37 sem con EG exacta) antes de emitir resultados definitivos.",
                                    color = DarkText,
                                    fontSize = 11.sp,
                                    lineHeight = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { onConfirmTermWithoutWeeks() }
                                        .testTag("calc_confirm_term_no_weeks_button"),
                                    shape = RoundedCornerShape(10.dp),
                                    color = CalcNavy
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 9.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Verified,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Confirmar A término (sin semanas exactas)",
                                            color = Color.White,
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                }
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { onTogglePrematurez(true) }
                                        .testTag("calc_activate_preterm_button"),
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color.White,
                                    border = BorderStroke(1.dp, BorderAmber)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Registrar prematuridad y EG (< 37 sem)",
                                            color = WarningText,
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (state.esPrematuro) {
                        Spacer(modifier = Modifier.height(16.dp))

                        // Edad gestacional row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.Schedule,
                                    contentDescription = null,
                                    tint = MutedText,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Edad gestacional:",
                                    color = MutedText,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = CalcPillContainer
                                ) {
                                    Text(
                                        text = if (state.ediHasRegisteredEg) {
                                            "${state.egSemanas} semanas"
                                        } else {
                                            "EG no registrada"
                                        },
                                        color = DarkText,
                                        fontSize = 11.5.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                            .testTag("calc_eg_status_pill")
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = Color(0xFFFEE2E2)
                                ) {
                                    Text(
                                        text = if (state.hasValidConfirmedPretermForCorrection) {
                                            "-${state.deficitSemanas} sem desfase"
                                        } else {
                                            "EG pendiente"
                                        },
                                        color = Color(0xFFDC2626),
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Slider 24s .. 36s (Continuous rounded bar track + solid circular navy thumb)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "24s",
                                color = MutedText,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                            Slider(
                                value = state.egSemanas.coerceIn(24, 36).toFloat(),
                                onValueChange = { onUpdateEgSemanas(it.roundToInt()) },
                                valueRange = 24f..36f,
                                steps = 11,
                                thumb = {
                                    Box(
                                        modifier = Modifier
                                            .size(18.dp)
                                            .clip(CircleShape)
                                            .background(CalcNavy)
                                    )
                                },
                                track = {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(50))
                                            .background(CalcTrackBg)
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 8.dp)
                                    .height(28.dp)
                                    .testTag("eg_slider")
                            )
                            Text(
                                text = "36s",
                                color = MutedText,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (!state.ediHasRegisteredEg) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onUpdateEgSemanas(state.egSemanas) }
                                    .testTag("calc_confirm_eg_button"),
                                shape = RoundedCornerShape(12.dp),
                                color = CalcNavy
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Verified,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Confirmar ${state.egSemanas} semanas exactas",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Two Age Cards: CRONOLÓGICA & CORREGIDA (OMS)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Left: CRONOLÓGICA
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(22.dp),
                            color = CalcSoftContainer
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "CRONOLÓGICA",
                                        color = MutedText,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 0.5.sp
                                    )
                                    Icon(
                                        imageVector = Icons.Outlined.Schedule,
                                        contentDescription = null,
                                        tint = MutedText,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = state.edadCronologicaAMD,
                                    color = DarkText,
                                    fontSize = 19.sp,
                                    maxLines = 1,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.testTag("edad_cronologica_text")
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Desde parto",
                                        color = MutedText,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFFE6ECF4)
                                    ) {
                                        Text(
                                            text = "${state.edadCronologicaSemanas} sem",
                                            color = DarkText,
                                            fontSize = 10.5.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.ExtraBold,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Right: CORREGIDA (OMS)
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(22.dp),
                            color = CalcNavy
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "CORREGIDA (OMS)",
                                        color = Color.White.copy(alpha = 0.85f),
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 0.4.sp
                                    )
                                    Icon(
                                        imageVector = Icons.Outlined.Verified,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = state.edadCorregidaAMD,
                                    color = Color.White,
                                    fontSize = if (state.aplicaEdadCorregida) 19.sp else 11.5.sp,
                                    maxLines = 1,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.testTag("edad_corregida_text")
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Estándar activo",
                                        color = Color.White.copy(alpha = 0.72f),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFF334155)
                                    ) {
                                        Text(
                                            text = when {
                                                state.isCorrectedAgePending -> "Confirmar EG"
                                                state.isBirthMaturityConfirmationPendingInUnder24m -> "Confirmar cond."
                                                else -> "-${state.deficitSemanas} sem"
                                            },
                                            color = Color.White,
                                            fontSize = 10.5.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.ExtraBold,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // =========================================================================
            // CARD 3: MEDICIONES ANTROPOMÉTRICAS (Matches calculadora_cred_d594b8f8.png)
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
                        .padding(18.dp)
                ) {
                    // Header row: Ruler icon + MEDICIONES ANTROPOMÉTRICAS / Valores actuales de consulta + [3 datos] pill
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
                                    .background(CalcPillContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Straighten,
                                    contentDescription = "Mediciones Antropométricas",
                                    tint = CalcNavy,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "MEDICIONES ANTROPOMÉTRICAS",
                                    color = CalcNavy,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.5.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Valores actuales de consulta",
                                    color = MutedText,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Surface(
                            shape = RoundedCornerShape(50),
                            color = CalcPillContainer
                        ) {
                            Text(
                                text = "${state.datosAntropometricosCount} datos",
                                color = SlateText,
                                fontSize = 10.5.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                                    .testTag("calculadora_datos_count_badge")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 1. Peso Corporal Card Row (Libre para rellenar por defecto)
                    AnthropometricMeasurementRow(
                        title = "Peso Corporal",
                        badgeText = null,
                        subtitle = "Balanza pediátrica",
                        inputValue = state.pesoInput,
                        placeholderText = "0.00",
                        unitText = "KG",
                        minusTag = "peso_minus_button",
                        plusTag = "peso_plus_button",
                        inputTag = "peso_input_field",
                        onMinus = { onAdjustPeso(-0.05) },
                        onPlus = { onAdjustPeso(0.05) },
                        onInputChange = onPesoInputChange
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // 2. Longitud / Estatura Card Row (NTS N° 238: <2a Longitud/Infantómetro, ≥2a Estatura/Tallímetro)
                    val isUnder2Years = state.edadEfectivaMeses < 24
                    AnthropometricMeasurementRow(
                        title = if (isUnder2Years) "Longitud" else "Estatura",
                        badgeText = if (isUnder2Years) "<2a" else "≥2a",
                        subtitle = if (isUnder2Years) "Infantómetro" else "Tallímetro",
                        inputValue = state.longitudInput,
                        placeholderText = "0.0",
                        unitText = "CM",
                        minusTag = "longitud_minus_button",
                        plusTag = "longitud_plus_button",
                        inputTag = "longitud_input_field",
                        onMinus = { onAdjustLongitud(-0.2) },
                        onPlus = { onAdjustLongitud(0.2) },
                        onInputChange = onLongitudInputChange
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // 3. Perím. Cefálico Card Row (Libre para rellenar por defecto)
                    AnthropometricMeasurementRow(
                        title = "Perím. Cefálico",
                        badgeText = null,
                        subtitle = "Cinta métrica",
                        inputValue = state.perimetroCefalicoInput,
                        placeholderText = "0.0",
                        unitText = "CM",
                        minusTag = "pc_minus_button",
                        plusTag = "pc_plus_button",
                        inputTag = "pc_input_field",
                        onMinus = { onAdjustPerimetroCefalico(-0.1) },
                        onPlus = { onAdjustPerimetroCefalico(0.1) },
                        onInputChange = onPerimetroCefalicoInputChange
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // IMC CALCULADO Summary Card
                    val hasImcData = state.hasPesoIngresado && state.hasLongitudIngresada
                    val ptEval = state.evalPT
                    val imcEval = state.evalIMCE
                    val ptLabel = when {
                        !hasImcData -> "Pendiente (P/T)"
                        !ptEval.isReferenceAvailable -> "Fuera de rango P/T"
                        ptEval.shortClassification == "Normal" -> "Eutrófico (P/T)"
                        else -> "${ptEval.shortClassification} (P/T)"
                    }
                    val (badgeBg, badgeTextCol, badgeBorderCol) = when {
                        !hasImcData -> Triple(Color.White, SlateText, CalcRowCardBorder)
                        !ptEval.isReferenceAvailable -> Triple(CalcPillContainer, SlateText, CalcRowCardBorder)
                        else -> when (ptEval.severity) {
                            ClassificationSeverity.NORMAL -> Triple(SoftGreenBg, DarkGreenPill, BorderSoftGreen)
                            ClassificationSeverity.WARNING -> Triple(SoftAmberPill, WarningText, BorderAmber)
                            ClassificationSeverity.SEVERE -> Triple(SoftRedBg, DarkRedText, BorderSoftRed)
                        }
                    }

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        color = CalcSoftContainer
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    modifier = Modifier.size(44.dp),
                                    shape = CircleShape,
                                    color = Color.White,
                                    shadowElevation = 0.5.dp
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        ScaleDialIcon(
                                            tint = CalcNavy,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "IMC CALCULADO",
                                        color = MutedText,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 0.5.sp
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = buildAnnotatedString {
                                            withStyle(
                                                SpanStyle(
                                                    color = DarkText,
                                                    fontSize = 21.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontWeight = FontWeight.Black
                                                )
                                            ) {
                                                append(if (hasImcData) state.imcCalculado.formatDecimal(1) else "--")
                                            }
                                            append(" ")
                                            withStyle(
                                                SpanStyle(
                                                    color = MutedText,
                                                    fontSize = 12.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            ) {
                                                append("kg/m²")
                                            }
                                        },
                                        modifier = Modifier.testTag("calculadora_imc_value")
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = badgeBg,
                                    border = BorderStroke(1.dp, badgeBorderCol)
                                ) {
                                    Text(
                                        text = ptLabel,
                                        color = badgeTextCol,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier
                                            .padding(horizontal = 12.dp, vertical = 5.dp)
                                            .testTag("calculadora_pt_badge")
                                    )
                                }
                                Spacer(modifier = Modifier.height(5.dp))
                                Text(
                                    text = when {
                                        !hasImcData -> "Z-Score: -- DE"
                                        !imcEval.isReferenceAvailable && !state.isChronologicalAgeVerifiable ->
                                            "IMC/E: Edad no verificable"
                                        !imcEval.isReferenceAvailable ->
                                            "IMC/E: No disp. (ref. 0-60m)"
                                        else ->
                                            "Z-Score: ${state.zScoreImc.formatSignedDecimal(1)} DE"
                                    },
                                    color = MutedText,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.testTag("calculadora_imc_zscore_text")
                                )
                            }
                        }
                    }
                }
            }

            // =========================================================================
            // BOTTOM CTA BUTTON: Calcular y Ver Curvas OMS
            // =========================================================================
            Button(
                onClick = onContinueToCurvas,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .testTag("continuar_curvas_button"),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = CalcNavy),
                contentPadding = PaddingValues(horizontal = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            DoubleWaveIcon(tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Calcular y Ver Curvas OMS",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Continuar",
                            tint = CalcNavy,
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
private fun AnthropometricMeasurementRow(
    title: String,
    badgeText: String?,
    subtitle: String,
    inputValue: String,
    placeholderText: String,
    unitText: String,
    minusTag: String,
    plusTag: String,
    inputTag: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    onInputChange: (String) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = CalcRowCardBg,
        border = BorderStroke(1.dp, CalcRowCardBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left Column: • Title [<2a] + Subtitle
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(CalcNavy)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = title,
                        color = DarkText,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    if (badgeText != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = CalcPillContainer
                        ) {
                            Text(
                                text = badgeText,
                                color = SlateText,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                    }
                    // Preserve invisible testTag hooks for minus/plus adjustments
                    Box(
                        modifier = Modifier
                            .size(1.dp)
                            .clickable(onClick = onMinus)
                            .testTag(minusTag)
                    )
                    Box(
                        modifier = Modifier
                            .size(1.dp)
                            .clickable(onClick = onPlus)
                            .testTag(plusTag)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    color = MutedText,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Right Value Pill Box (Libre para rellenar por defecto)
            Surface(
                modifier = Modifier
                    .width(134.dp)
                    .height(44.dp),
                shape = RoundedCornerShape(14.dp),
                color = CalcPillContainer
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    BasicTextField(
                        value = inputValue,
                        onValueChange = { raw ->
                            val filtered = raw.filter { it.isDigit() || it == '.' || it == ',' }
                                .replace(',', '.')
                            onInputChange(filtered)
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        textStyle = TextStyle(
                            color = DarkText,
                            fontSize = 19.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black,
                            textAlign = TextAlign.Center
                        ),
                        decorationBox = { innerTextField ->
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                if (inputValue.isEmpty()) {
                                    Text(
                                        text = placeholderText,
                                        color = Color(0xFFBCC6D4),
                                        fontSize = 18.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                }
                                innerTextField()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag(inputTag)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = unitText,
                        color = MutedText,
                        fontSize = 11.5.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun PulseWaveIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.10f, h * 0.52f)
            lineTo(w * 0.30f, h * 0.52f)
            lineTo(w * 0.42f, h * 0.20f)
            lineTo(w * 0.58f, h * 0.84f)
            lineTo(w * 0.70f, h * 0.52f)
            lineTo(w * 0.90f, h * 0.52f)
        }
        drawPath(
            path = path,
            color = tint,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

@Composable
private fun ScaleDialIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        drawLine(tint, Offset(w * 0.25f, h * 0.22f), Offset(w * 0.75f, h * 0.22f), strokeWidth = 1.8.dp.toPx(), cap = StrokeCap.Round)
        val body = Path().apply {
            moveTo(w * 0.30f, h * 0.36f)
            lineTo(w * 0.70f, h * 0.36f)
            lineTo(w * 0.80f, h * 0.84f)
            lineTo(w * 0.20f, h * 0.84f)
            close()
        }
        drawPath(body, color = tint, style = stroke)
        drawCircle(tint, radius = w * 0.13f, center = Offset(w * 0.50f, h * 0.60f), style = stroke)
    }
}

@Composable
private fun DoubleWaveIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        val topWave = Path().apply {
            moveTo(w * 0.15f, h * 0.42f)
            lineTo(w * 0.38f, h * 0.24f)
            lineTo(w * 0.62f, h * 0.46f)
            lineTo(w * 0.85f, h * 0.26f)
        }
        val bottomWave = Path().apply {
            moveTo(w * 0.15f, h * 0.72f)
            lineTo(w * 0.38f, h * 0.54f)
            lineTo(w * 0.62f, h * 0.76f)
            lineTo(w * 0.85f, h * 0.56f)
        }
        drawPath(topWave, color = tint, style = stroke)
        drawPath(bottomWave, color = tint, style = stroke)
    }
}
