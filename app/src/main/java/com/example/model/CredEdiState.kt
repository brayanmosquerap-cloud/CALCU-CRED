package com.example.model

import java.time.LocalDate
import java.time.Period
import java.time.temporal.ChronoUnit
import java.util.Locale

enum class AppTab {
    CALCULADORA,
    CURVAS_OMS,
    DIAGNOSTICO,
    EDI
}

enum class SexoBiologico(val label: String) {
    NINO("Niño"),
    NINA("Niña")
}

enum class CurvaIndicador(
    val shortCode: String,
    val subLabel: String,
    val fullTitle: String,
    val unitLabel: String,
    val yAxisLabel: String
) {
    PE("P/E", "(Peso)", "Peso para la Edad (P/E) · Puntuaciones Z", "kg / meses", "Peso (kg)"),
    TE("T/E", "(Talla)", "Talla para la Edad (T/E) · Puntuaciones Z", "cm / meses", "Longitud (cm)"),
    PT("P/T", "(Proporción)", "Peso para la Talla (P/T) · Puntuaciones Z", "kg / cm", "Peso (kg)"),
    IMCE("IMC", "(IMC/E)", "IMC para la Edad (IMC/E) · Puntuaciones Z", "kg/m² / m", "IMC (kg/m²)"),
    PCE("PC/E", "(Cefálico)", "Perímetro Cefálico para la Edad (PC/E) · Puntuaciones Z", "cm / meses", "Perímetro Cefálico (cm)")
}

enum class SemaforoColor(
    val label: String,
    val statusText: String,
    val subText: String,
    val dictamenHeader: String,
    val dictamenDescription: String
) {
    VERDE(
        label = "Verde",
        statusText = "Normal",
        subText = "Aprobado",
        dictamenHeader = "VERDE · Normal",
        dictamenDescription = "Desarrollo psicomotor adecuado para EC"
    ),
    AMARILLO(
        label = "Amarillo",
        statusText = "Rezago",
        subText = "1-2 Fallas",
        dictamenHeader = "AMARILLO · Rezago",
        dictamenDescription = "Rezago en el desarrollo · Requiere estimulación"
    ),
    ROJO(
        label = "Rojo",
        statusText = "Riesgo",
        subText = ">2 o Alarma",
        dictamenHeader = "ROJO · Riesgo de Retraso",
        dictamenDescription = "Referencia prioritaria por señales de alerta o EN"
    )
}

data class FrbItem(
    val id: Int,
    val title: String,
    val checked: Boolean
)

data class SaItem(
    val id: Int,
    val title: String,
    val isPresent: Boolean // false = No, true = Sí
)

data class EdiAreaItem(
    val id: Int,
    val code: String,      // "MG", "MF", "LE", "SO", "CO"
    val title: String,     // Ej: "Motor Grueso (MG):"
    val question: String,  // Hito correspondiente al grupo etario según Manual EDI
    val isAchieved: Boolean = true // true = Sí, false = No
)

data class EdiAgeGroupTemplate(
    val groupCode: String,        // Ej: "Grupo 3"
    val fullLabel: String,        // Ej: "Grupo 3 (1m a 2m 29d)"
    val saQuestions: List<String>,
    val areaQuestions: List<Pair<String, String>>, // (title, question) para MG, MF, LE, SO, CO
    val pauta1: String,
    val pauta2: String
)

data class SimpleDate(
    val day: Int,
    val month: Int,
    val year: Int
)

data class CalendarPeriod(
    val years: Int,
    val months: Int,
    val days: Int,
    val totalMonths: Int,
    val totalDays: Int
) {
    val formattedAMD: String
        get() = "${years}a ${months}m ${days}d"

    val formattedVerbose: String
        get() = ClinicalAgeCalculator.formatPeriodVerbose(years, months, days)
}

data class AgeResult(
    val cronologicaMeses: Int,
    val cronologicaDias: Int,
    val cronologicaSemanas: Int,
    val corregidaMeses: Int,
    val corregidaDias: Int,
    val cronologicaTotalDias: Int = 0,
    val diasRestarPrematuridad: Int = 0,
    val corregidaTotalDias: Int = 0,
    val cronologicaAnios: Int = cronologicaMeses / 12,
    val cronologicaMesesDelAnio: Int = cronologicaMeses % 12,
    val corregidaAnios: Int = corregidaMeses / 12,
    val corregidaMesesDelAnio: Int = corregidaMeses % 12,
    val aplicaEdadCorregida: Boolean = false,
    val fechaAjustada: String = ClinicalAgeCalculator.NO_APLICA_EDAD_CORREGIDA,
    val semanasPrematuridad: Int = diasRestarPrematuridad / 7,
    val diasPrematuridadRestantes: Int = diasRestarPrematuridad % 7
) {
    val edadCronologicaAMD: String
        get() = "${cronologicaAnios}a ${cronologicaMesesDelAnio}m ${cronologicaDias}d"

    val edadCronologicaCompleta: String
        get() = ClinicalAgeCalculator.formatPeriodVerbose(
            cronologicaAnios,
            cronologicaMesesDelAnio,
            cronologicaDias
        )

    val edadCorregidaNumericaAMD: String
        get() = "${corregidaAnios}a ${corregidaMesesDelAnio}m ${corregidaDias}d"

    val edadCorregidaAMD: String
        get() = if (aplicaEdadCorregida) {
            edadCorregidaNumericaAMD
        } else {
            ClinicalAgeCalculator.NO_APLICA_EDAD_CORREGIDA
        }

    val edadCorregidaCompleta: String
        get() = if (aplicaEdadCorregida) {
            ClinicalAgeCalculator.formatPeriodVerbose(
                corregidaAnios,
                corregidaMesesDelAnio,
                corregidaDias
            )
        } else {
            ClinicalAgeCalculator.NO_APLICA_EDAD_CORREGIDA
        }
}

data class NextCredControlInfo(
    val fechaFormateada: String,       // Ej: "15 de Marzo, 2025"
    val fechaCorta: String,            // Ej: "15/03/2025"
    val intervaloBadge: String,        // Ej: "En 30 días", "En 60 días", "En 90 días", "En 180 días", "En 360 días", "En 7 días"
    val frecuenciaLabel: String,       // Ej: "Mensual", "Bimestral", "Trimestral", "Semestral", "Anual", "Semanal (RN)"
    val controlActualLabel: String,    // Ej: "2° Control (2m)"
    val proximoControlLabel: String,   // Ej: "3° Control (3m)"
    val diasIntervalo: Int             // 7, 30, 60, 90, 180, 360
)

object ClinicalAgeCalculator {
    const val NO_APLICA_EDAD_CORREGIDA = "No aplica edad corregida"
    private const val FULL_TERM_WEEKS = 40
    private const val PRETERM_CUTOFF_WEEKS = 37
    private const val MAX_CORRECTED_AGE_YEARS = 2

    private val monthNamesEs = listOf(
        "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
        "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
    )

    fun formatPeriodVerbose(years: Int, months: Int, days: Int): String {
        val yLabel = if (years == 1) "1 año" else "$years años"
        val mLabel = if (months == 1) "1 mes" else "$months meses"
        val dLabel = if (days == 1) "1 día" else "$days días"
        return "$yLabel, $mLabel y $dLabel"
    }

    fun todayFormatted(): String {
        val today = LocalDate.now()
        return String.format(
            Locale.US,
            "%02d/%02d/%04d",
            today.dayOfMonth,
            today.monthValue,
            today.year
        )
    }

    fun parseDate(raw: String): SimpleDate? {
        val cleaned = raw.trim()
        if (cleaned.isEmpty()) return null
        val parts = cleaned.split("/", "-", ".")
        if (parts.size != 3) {
            val digits = cleaned.filter { it.isDigit() }
            if (digits.length == 8) {
                val d = digits.substring(0, 2).toIntOrNull() ?: return null
                val m = digits.substring(2, 4).toIntOrNull() ?: return null
                val y = digits.substring(4, 8).toIntOrNull() ?: return null
                if (y in 1900..2100 && m in 1..12 && d in 1..daysInMonth(y, m)) {
                    return SimpleDate(day = d, month = m, year = y)
                }
            }
            return null
        }
        val p0 = parts[0].trim().toIntOrNull() ?: return null
        val p1 = parts[1].trim().toIntOrNull() ?: return null
        val p2 = parts[2].trim().toIntOrNull() ?: return null

        // Support ISO yyyy-MM-dd as well as standard dd/MM/yyyy
        if (p0 in 1900..2100 && p1 in 1..12 && p2 in 1..daysInMonth(p0, p1)) {
            return SimpleDate(day = p2, month = p1, year = p0)
        }

        val year = when {
            p2 in 1900..2100 -> p2
            p2 in 0..99 -> 2000 + p2
            else -> return null
        }

        val (day, month) = if (p1 in 1..12 && p0 in 1..31) {
            p0 to p1
        } else if (p0 in 1..12 && p1 in 1..31) {
            p1 to p0
        } else {
            return null
        }

        val maxDay = daysInMonth(year, month)
        if (day !in 1..maxDay) return null
        return SimpleDate(day = day, month = month, year = year)
    }

    fun adjustYear(dateStr: String, deltaYears: Int): String {
        val parsed = parseDate(dateStr) ?: return dateStr
        val newYear = (parsed.year + deltaYears).coerceIn(1990, 2050)
        val maxDays = daysInMonth(newYear, parsed.month)
        val adjustedDay = parsed.day.coerceAtMost(maxDays)
        return String.format(Locale.US, "%02d/%02d/%04d", adjustedDay, parsed.month, newYear)
    }

    fun isLeapYear(year: Int): Boolean =
        (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)

    fun daysInMonth(year: Int, month: Int): Int = when (month) {
        1, 3, 5, 7, 8, 10, 12 -> 31
        4, 6, 9, 11 -> 30
        2 -> if (isLeapYear(year)) 29 else 28
        else -> 30
    }

    private fun SimpleDate.toLocalDate(): LocalDate =
        LocalDate.of(year, month, day)

    private fun LocalDate.toSimpleDate(): SimpleDate =
        SimpleDate(day = dayOfMonth, month = monthValue, year = year)

    private fun addDays(date: SimpleDate, daysToAdd: Int): SimpleDate =
        date.toLocalDate().plusDays(daysToAdd.coerceAtLeast(0).toLong()).toSimpleDate()

    private fun addMonths(date: SimpleDate, monthsToAdd: Int): SimpleDate =
        date.toLocalDate().plusMonths(monthsToAdd.toLong()).toSimpleDate()

    private fun formatFullSpanishDate(date: SimpleDate): String {
        val monthName = monthNamesEs.getOrElse(date.month - 1) { "Marzo" }
        return "${date.day} de $monthName, ${date.year}"
    }

    private fun formatShortDate(date: SimpleDate): String =
        String.format(Locale.US, "%02d/%02d/%04d", date.day, date.month, date.year)

    /**
     * Único algoritmo consistente para calcular diferencias exactas entre fechas de calendario
     * (sin componentes de hora ni conversiones UTC/zona horaria).
     *
     * Criterio de calendario aplicado (ISO-8601 / java.time.Period):
     * - El día de inicio (nacimiento o fecha ajustada) se considera día 0.
     * - Se suman los años y meses calendario completos desde `startDate` hasta `anchorDate = startDate.plusMonths(totalMonths)`
     *   sin exceder `endDate`, respetando los días reales de cada mes (28, 29, 30, 31) y años bisiestos.
     * - Los días sobrantes corresponden exactamente a los días de calendario entre `anchorDate` y `endDate`.
     */
    fun calculateExactCalendarPeriod(startDate: LocalDate, endDate: LocalDate): CalendarPeriod {
        if (!endDate.isAfter(startDate)) {
            return CalendarPeriod(
                years = 0,
                months = 0,
                days = 0,
                totalMonths = 0,
                totalDays = 0
            )
        }
        val period = Period.between(startDate, endDate)
        val years = period.years.coerceAtLeast(0)
        val months = period.months.coerceAtLeast(0)
        val days = period.days.coerceAtLeast(0)
        val totalMonths = years * 12 + months
        val totalDays = ChronoUnit.DAYS.between(startDate, endDate).toInt().coerceAtLeast(0)
        return CalendarPeriod(
            years = years,
            months = months,
            days = days,
            totalMonths = totalMonths,
            totalDays = totalDays
        )
    }

    /**
     * Calcula la Edad Cronológica y la Edad Corregida según la NTS N.° 238-MINSA/DGIESP-2025:
     * 1. Edad Cronológica: diferencia exacta de calendario entre `fechaNacimiento` y `fechaEvaluacion`.
     * 2. Condición de Edad Corregida: se aplica únicamente cuando el niño nació antes de las 37 semanas
     *    de gestación (`esPrematuro && egSemanas < 37`) y tiene menos de 2 años de edad cronológica (`crono.years < 2`).
     * 3. Procedimiento de Edad Corregida:
     *    - Calcula las semanas y días faltantes para completar 40 semanas (280 días) y los convierte a días.
     *    - Suma los días de prematuridad a la fecha de nacimiento para obtener la `fechaAjustada`.
     *    - Calcula la diferencia exacta de calendario entre `fechaAjustada` y `fechaEvaluacion` usando
     *      el mismo algoritmo `calculateExactCalendarPeriod`.
     */
    fun calculateAges(
        fechaNacimiento: String,
        fechaEvaluacion: String,
        esPrematuro: Boolean,
        egSemanas: Int,
        egDias: Int = 0
    ): AgeResult {
        val birth = parseDate(fechaNacimiento)
        val eval = parseDate(fechaEvaluacion)
        val safeEgSemanas = egSemanas.coerceIn(20, 44)
        val safeEgDias = egDias.coerceIn(0, 6)
        val isBornPreterm = esPrematuro && safeEgSemanas < PRETERM_CUTOFF_WEEKS

        val fullTermTotalDays = FULL_TERM_WEEKS * 7
        val gestationalTotalDays = safeEgSemanas * 7 + safeEgDias
        val diasPrematuridad = if (isBornPreterm) {
            (fullTermTotalDays - gestationalTotalDays).coerceAtLeast(0)
        } else {
            0
        }
        val semanasPrematuridad = diasPrematuridad / 7
        val diasPrematuridadRestantes = diasPrematuridad % 7

        if (birth == null || eval == null) {
            return AgeResult(
                cronologicaMeses = 0,
                cronologicaDias = 0,
                cronologicaSemanas = 0,
                corregidaMeses = 0,
                corregidaDias = 0,
                cronologicaTotalDias = 0,
                diasRestarPrematuridad = diasPrematuridad,
                corregidaTotalDias = 0,
                cronologicaAnios = 0,
                cronologicaMesesDelAnio = 0,
                corregidaAnios = 0,
                corregidaMesesDelAnio = 0,
                aplicaEdadCorregida = false,
                fechaAjustada = NO_APLICA_EDAD_CORREGIDA,
                semanasPrematuridad = semanasPrematuridad,
                diasPrematuridadRestantes = diasPrematuridadRestantes
            )
        }

        val birthLocalDate = birth.toLocalDate()
        val evalLocalDate = eval.toLocalDate()

        if (evalLocalDate.isBefore(birthLocalDate)) {
            return AgeResult(
                cronologicaMeses = 0,
                cronologicaDias = 0,
                cronologicaSemanas = 0,
                corregidaMeses = 0,
                corregidaDias = 0,
                cronologicaTotalDias = 0,
                diasRestarPrematuridad = diasPrematuridad,
                corregidaTotalDias = 0,
                cronologicaAnios = 0,
                cronologicaMesesDelAnio = 0,
                corregidaAnios = 0,
                corregidaMesesDelAnio = 0,
                aplicaEdadCorregida = false,
                fechaAjustada = NO_APLICA_EDAD_CORREGIDA,
                semanasPrematuridad = semanasPrematuridad,
                diasPrematuridadRestantes = diasPrematuridadRestantes
            )
        }

        // 1. Edad Cronológica exacta
        val cronoPeriod = calculateExactCalendarPeriod(birthLocalDate, evalLocalDate)
        val cronologicaSemanas = cronoPeriod.totalDays / 7

        // 2. Validar ambas condiciones para Edad Corregida:
        //    a) Nacido antes de las 37 semanas de gestación (isBornPreterm)
        //    b) Menos de 2 años de edad cronológica (cronoPeriod.years < 2)
        val aplicaEdadCorregida = isBornPreterm && cronoPeriod.years < MAX_CORRECTED_AGE_YEARS

        // 3. Fecha ajustada = fechaNacimiento + días de prematuridad
        val fechaAjustadaLocalDate = birthLocalDate.plusDays(diasPrematuridad.toLong())
        val fechaAjustadaFormatted = if (aplicaEdadCorregida) {
            formatShortDate(fechaAjustadaLocalDate.toSimpleDate())
        } else {
            NO_APLICA_EDAD_CORREGIDA
        }

        // 4. Diferencia exacta entre la fecha ajustada y la fecha de evaluación
        val corrPeriod = if (aplicaEdadCorregida) {
            calculateExactCalendarPeriod(fechaAjustadaLocalDate, evalLocalDate)
        } else {
            CalendarPeriod(0, 0, 0, 0, 0)
        }

        return AgeResult(
            cronologicaMeses = cronoPeriod.totalMonths,
            cronologicaDias = cronoPeriod.days,
            cronologicaSemanas = cronologicaSemanas,
            cronologicaTotalDias = cronoPeriod.totalDays,
            corregidaMeses = if (aplicaEdadCorregida) corrPeriod.totalMonths else cronoPeriod.totalMonths,
            corregidaDias = if (aplicaEdadCorregida) corrPeriod.days else cronoPeriod.days,
            diasRestarPrematuridad = diasPrematuridad,
            corregidaTotalDias = if (aplicaEdadCorregida) corrPeriod.totalDays else cronoPeriod.totalDays,
            cronologicaAnios = cronoPeriod.years,
            cronologicaMesesDelAnio = cronoPeriod.months,
            corregidaAnios = if (aplicaEdadCorregida) corrPeriod.years else cronoPeriod.years,
            corregidaMesesDelAnio = if (aplicaEdadCorregida) corrPeriod.months else cronoPeriod.months,
            aplicaEdadCorregida = aplicaEdadCorregida,
            fechaAjustada = fechaAjustadaFormatted,
            semanasPrematuridad = semanasPrematuridad,
            diasPrematuridadRestantes = diasPrematuridadRestantes
        )
    }

    /**
     * Calcula la fecha exacta (día, mes y año) y el intervalo del Próximo Control CRED
     * según el nuevo esquema de periodicidad de la NTS N° 238-MINSA/DGIESP-2025 (numeral 6.3 y 6.4).
     */
    fun calculateNextCredControl(
        fechaEvaluacion: String,
        edadMeses: Int,
        edadDias: Int
    ): NextCredControlInfo {
        val evalDate = parseDate(fechaEvaluacion)
            ?: parseDate(todayFormatted())
            ?: SimpleDate(6, 11, 2026)
        val totalDias = edadMeses * 30 + edadDias

        return when {
            edadMeses == 0 && totalDias <= 28 -> {
                val (actualCtrl, proxCtrl) = when {
                    totalDias < 7 -> "1° Control RN (<7d)" to "2° Control RN (7-14d)"
                    totalDias < 14 -> "2° Control RN (7-14d)" to "3° Control RN (14-21d)"
                    else -> "3° Control RN (14-21d)" to "1° Control <1a (1 mes)"
                }
                val nextDate = addDays(evalDate, 7)
                NextCredControlInfo(
                    fechaFormateada = formatFullSpanishDate(nextDate),
                    fechaCorta = formatShortDate(nextDate),
                    intervaloBadge = "En 7 días",
                    frecuenciaLabel = "Semanal (RN)",
                    controlActualLabel = actualCtrl,
                    proximoControlLabel = proxCtrl,
                    diasIntervalo = 7
                )
            }

            edadMeses <= 1 -> {
                val nextDate = addMonths(evalDate, 1)
                NextCredControlInfo(
                    fechaFormateada = formatFullSpanishDate(nextDate),
                    fechaCorta = formatShortDate(nextDate),
                    intervaloBadge = "En 30 días",
                    frecuenciaLabel = "Mensual",
                    controlActualLabel = "1° Control (1m)",
                    proximoControlLabel = "2° Control (2m)",
                    diasIntervalo = 30
                )
            }

            edadMeses == 2 -> {
                val nextDate = addMonths(evalDate, 1)
                NextCredControlInfo(
                    fechaFormateada = formatFullSpanishDate(nextDate),
                    fechaCorta = formatShortDate(nextDate),
                    intervaloBadge = "En 30 días",
                    frecuenciaLabel = "Mensual",
                    controlActualLabel = "2° Control (2m)",
                    proximoControlLabel = "3° Control (3m)",
                    diasIntervalo = 30
                )
            }

            edadMeses == 3 -> {
                val nextDate = addMonths(evalDate, 1)
                NextCredControlInfo(
                    fechaFormateada = formatFullSpanishDate(nextDate),
                    fechaCorta = formatShortDate(nextDate),
                    intervaloBadge = "En 30 días",
                    frecuenciaLabel = "Mensual",
                    controlActualLabel = "3° Control (3m)",
                    proximoControlLabel = "4° Control (4m)",
                    diasIntervalo = 30
                )
            }

            edadMeses in 4..5 -> {
                val nextDate = addMonths(evalDate, 2)
                NextCredControlInfo(
                    fechaFormateada = formatFullSpanishDate(nextDate),
                    fechaCorta = formatShortDate(nextDate),
                    intervaloBadge = "En 60 días",
                    frecuenciaLabel = "Bimestral",
                    controlActualLabel = "4° Control (${edadMeses}m)",
                    proximoControlLabel = "5° Control (6m)",
                    diasIntervalo = 60
                )
            }

            edadMeses == 6 -> {
                val nextDate = addMonths(evalDate, 1)
                NextCredControlInfo(
                    fechaFormateada = formatFullSpanishDate(nextDate),
                    fechaCorta = formatShortDate(nextDate),
                    intervaloBadge = "En 30 días",
                    frecuenciaLabel = "Mensual",
                    controlActualLabel = "5° Control (6m)",
                    proximoControlLabel = "6° Control (7m)",
                    diasIntervalo = 30
                )
            }

            edadMeses in 7..8 -> {
                val nextDate = addMonths(evalDate, 2)
                NextCredControlInfo(
                    fechaFormateada = formatFullSpanishDate(nextDate),
                    fechaCorta = formatShortDate(nextDate),
                    intervaloBadge = "En 60 días",
                    frecuenciaLabel = "Bimestral",
                    controlActualLabel = "6° Control (${edadMeses}m)",
                    proximoControlLabel = "7° Control (9m)",
                    diasIntervalo = 60
                )
            }

            edadMeses in 9..11 -> {
                val nextDate = addMonths(evalDate, 3)
                NextCredControlInfo(
                    fechaFormateada = formatFullSpanishDate(nextDate),
                    fechaCorta = formatShortDate(nextDate),
                    intervaloBadge = "En 90 días",
                    frecuenciaLabel = "Trimestral",
                    controlActualLabel = "7° Control (${edadMeses}m)",
                    proximoControlLabel = "1° Control 1a (12m)",
                    diasIntervalo = 90
                )
            }

            edadMeses in 12..23 -> {
                val (actualCtrl, proxCtrl) = when (edadMeses) {
                    in 12..14 -> "1° Control 1a (12m)" to "2° Control 1a (15m)"
                    in 15..17 -> "2° Control 1a (15m)" to "3° Control 1a (18m)"
                    in 18..20 -> "3° Control 1a (18m)" to "4° Control 1a (21m)"
                    else -> "4° Control 1a (21m)" to "1° Control 2a (24m)"
                }
                val nextDate = addMonths(evalDate, 3)
                NextCredControlInfo(
                    fechaFormateada = formatFullSpanishDate(nextDate),
                    fechaCorta = formatShortDate(nextDate),
                    intervaloBadge = "En 90 días",
                    frecuenciaLabel = "Trimestral",
                    controlActualLabel = actualCtrl,
                    proximoControlLabel = proxCtrl,
                    diasIntervalo = 90
                )
            }

            edadMeses in 24..59 -> {
                val (actualCtrl, proxCtrl) = when (edadMeses) {
                    in 24..29 -> "1° Control 2a (24m)" to "2° Control 2a (30m)"
                    in 30..35 -> "2° Control 2a (30m)" to "1° Control 3a (36m)"
                    in 36..41 -> "1° Control 3a (36m)" to "2° Control 3a (42m)"
                    in 42..47 -> "2° Control 3a (42m)" to "1° Control 4a (48m)"
                    in 48..53 -> "1° Control 4a (48m)" to "2° Control 4a (54m)"
                    else -> "2° Control 4a (54m)" to "1° Control 5a (60m)"
                }
                val nextDate = addMonths(evalDate, 6)
                NextCredControlInfo(
                    fechaFormateada = formatFullSpanishDate(nextDate),
                    fechaCorta = formatShortDate(nextDate),
                    intervaloBadge = "En 180 días",
                    frecuenciaLabel = "Semestral",
                    controlActualLabel = actualCtrl,
                    proximoControlLabel = proxCtrl,
                    diasIntervalo = 180
                )
            }

            else -> {
                val anios = (edadMeses / 12).coerceIn(5, 11)
                val proxAnios = (anios + 1).coerceAtMost(11)
                val nextDate = addMonths(evalDate, 12)
                NextCredControlInfo(
                    fechaFormateada = formatFullSpanishDate(nextDate),
                    fechaCorta = formatShortDate(nextDate),
                    intervaloBadge = "En 360 días",
                    frecuenciaLabel = "Anual",
                    controlActualLabel = "1° Control ${anios}a",
                    proximoControlLabel = "1° Control ${proxAnios}a",
                    diasIntervalo = 360
                )
            }
        }
    }

    /**
     * Plantillas oficiales de evaluación EDI según el Manual para la Aplicación de la Prueba EDI
     * (Versión Peruana - Primera Edición 2026 / NTS N° 238-MINSA).
     */
    fun getEdiTemplateForAge(meses: Int, dias: Int): EdiAgeGroupTemplate {
        return when {
            meses < 1 -> EdiAgeGroupTemplate(
                groupCode = "Grupo 1",
                fullLabel = "Grupo 1 (0m a 29d)",
                saQuestions = listOf(
                    "Al evaluar el tono muscular, ¿lo siente totalmente flácido o rígido?",
                    "En reflejos de Moro, prensión palmar y plantar, ¿falta alguno de los 3?",
                    "¿Tiene problemas para lactar: se atora, se pone morado o succiona débil?",
                    "¿Considera que el desarrollo de su bebé es inadecuado para su edad?"
                ),
                areaQuestions = listOf(
                    "Motor Grueso (MG):" to "¿Acostado boca abajo puede voltear su cabeza para los dos lados y patea/mueve los brazos?",
                    "Motor Fino (MF):" to "¿Cuando le da en la mano una sonaja la retiene durante al menos 3 segundos?",
                    "Lenguaje / Comunicación (LE):" to "¿Se tranquiliza cuando escucha su voz y hace ruidos o llora cuando quiere comer?",
                    "Social / Afectivo (SO):" to "¿Cuando llora se tranquiliza al hablarle y sigue con la mirada sus movimientos?",
                    "Conocimiento / Cognitivo (CO):" to "¿Fija la mirada brevemente ante el rostro de la madre o estímulos luminosos?"
                ),
                pauta1 = "Fortalecer contacto piel a piel, lactancia materna exclusiva a libre demanda y estímulo auditivo suave.",
                pauta2 = "Vigilar reflejos arcaicos (Moro, succión, prensión) y ganancia ponderal neonatal semanal."
            )

            meses in 1..2 -> EdiAgeGroupTemplate(
                groupCode = "Grupo 3",
                fullLabel = "Grupo 3 (1m a 2m 29d)",
                saQuestions = listOf(
                    "No fija la mirada en el examinador / cuidador",
                    "Ausencia de sonrisa social en interacción",
                    "Manos cerradas persistentes con pulgar incluido",
                    "Irritabilidad extrema e inconsolable persistente"
                ),
                areaQuestions = listOf(
                    "Motor Grueso (MG)" to "¿Boca abajo se apoya en antebrazos y levanta la cabeza a 45°?",
                    "Motor Fino (MF)" to "¿Mantiene manos abiertas transitoriamente y sigue objetos al centro?",
                    "Lenguaje / Comunicación (LE)" to "¿Emite sonidos guturales ('agu', gorjeos) y reacciona a ruidos fuertes?",
                    "Social / Afectivo (SO)" to "¿Sonríe en respuesta a la sonrisa o voz del examinador (sonrisa social)?",
                    "Conocimiento / Cognitivo (CO)" to "¿Fija la mirada en el rostro del examinador y sigue estímulos?"
                ),
                pauta1 = "Reforzar estimulación en casa: posición boca abajo vigilada (tummy time) y seguimiento de objetos.",
                pauta2 = "Suplementación preventiva con hierro y monitoreo antropométrico con edad corregida."
            )

            meses == 3 -> EdiAgeGroupTemplate(
                groupCode = "Grupo 3",
                fullLabel = "Grupo 3 (3m a 3m 29d)",
                saQuestions = listOf(
                    "Cuando lo sostiene por el tronco, su cabeza cae bruscamente hacia adelante o atrás",
                    "Se le dificulta seguir con la vista a las personas y a los objetos brillantes",
                    "Se mantiene serio o inexpresivo cuando la madre o cuidador le sonríe",
                    "Reflejo de Moro ausente o asimétrico, o hipotonía/hipertonía marcada"
                ),
                areaQuestions = listOf(
                    "Motor Grueso (MG):" to "¿Boca abajo y apoyado en sus antebrazos logra sostener la cabeza al menos 3 segundos?",
                    "Motor Fino (MF):" to "¿Observa sus manos al menos 3 segundos mientras las mueve y las mantiene abiertas?",
                    "Lenguaje / Comunicación (LE):" to "¿Cuando balbucea pronuncia alguna consonante como 'agu' o 'ga'?",
                    "Social / Afectivo (SO):" to "¿Sonríe con facilidad y voltea la cabeza tratando de ubicar el origen de la voz?",
                    "Conocimiento / Cognitivo (CO):" to "¿Explora visualmente el entorno y sigue objetos de colores vivos en 180°?"
                ),
                pauta1 = "Estimular control cefálico en posición prona y ofrecer sonajas para apertura manual e integración en línea media.",
                pauta2 = "Hablarle y cantarle frente a frente promoviendo el diálogo vocal ('agu', 'ga') y sonrisa recíproca."
            )

            meses == 4 -> EdiAgeGroupTemplate(
                groupCode = "Grupo 4",
                fullLabel = "Grupo 4 (4m a 4m 29d)",
                saQuestions = listOf(
                    "Hay asimetría en los movimientos de las manos o miembros superiores",
                    "Muestra indiferencia por los objetos que se ponen a su alcance",
                    "Persiste el reflejo de Galant (incurvación) o reflejo de Moro asimétrico",
                    "Cuando lo sostiene del tronco, su cabeza cae bruscamente sin sostén"
                ),
                areaQuestions = listOf(
                    "Motor Grueso (MG):" to "¿Sentado con apoyo mantiene su cabeza firme al menos 1 minuto y boca arriba se voltea hacia los lados?",
                    "Motor Fino (MF):" to "¿Extiende alguno de sus brazos para agarrar un objeto a su alcance y lo retiene 10 segundos?",
                    "Lenguaje / Comunicación (LE):" to "¿Balbucea o grita para llamar la atención y emite sonidos como 'le', 'be', 'pa', 'gu'?",
                    "Social / Afectivo (SO):" to "¿Reconoce cuando le van a dar de lactar y le gusta jugar con otras personas?",
                    "Conocimiento / Cognitivo (CO):" to "¿Dirige la mirada y ambas manos hacia objetos sonoros en la línea media?"
                ),
                pauta1 = "Fomentar giros laterales en superficie firme y ofrecer objetos seguros para prensión voluntaria bimanual.",
                pauta2 = "Iniciar suplementación preventiva con hierro a los 4 meses según esquema NTS N° 238-MINSA."
            )

            meses in 5..6 -> EdiAgeGroupTemplate(
                groupCode = "Grupo 5",
                fullLabel = "Grupo 5 (5m a 6m 29d)",
                saQuestions = listOf(
                    "Presenta desinterés por sus manos o asimetría en sus movimientos",
                    "Rara vez o nunca le ha oído balbucear o emitir sonidos guturales",
                    "Cuando lo toma de las manos acostado, no hace esfuerzo para sentarse",
                    "Boca arriba se le dificulta girar su cuerpo hacia los lados"
                ),
                areaQuestions = listOf(
                    "Motor Grueso (MG):" to "¿Al tomarlo de los brazos para sentarlo ayuda jalándose y parado aguanta su peso en las piernas?",
                    "Motor Fino (MF):" to "¿Cuando agarra algún objeto se lo pasa de una mano a otra e intenta alcanzar objetos cercanos?",
                    "Lenguaje / Comunicación (LE):" to "¿Si lo llaman cuando no puede ver mueve la cabeza hacia la voz e imita sonidos?",
                    "Social / Afectivo (SO):" to "¿Se ríe cuando juegan a taparse la cara con una tela ('aquí estoy')?",
                    "Conocimiento / Cognitivo (CO):" to "¿Busca con la mirada objetos que llaman su atención e intercambia juguetes de mano?"
                ),
                pauta1 = "Ejercitar sedestación con apoyo progresivo, transferencia de objetos entre manos y juegos de escondite ('peek-a-boo').",
                pauta2 = "A los 6 meses iniciar alimentación complementaria rica en hierro e integrar tamizaje de hemoglobina CRED."
            )

            meses in 7..9 -> EdiAgeGroupTemplate(
                groupCode = "Grupo 6",
                fullLabel = "Grupo 6 (7m a 9m 29d)",
                saQuestions = listOf(
                    "Se le dificulta mantener un objeto en cada mano simultáneamente",
                    "Se le dificulta seguir con la mirada un objeto que se cayó al suelo",
                    "Persiste el reflejo de Moro o se encuentra ausente el reflejo de paracaídas",
                    "Es indiferente a los objetos y estímulos de su entorno"
                ),
                areaQuestions = listOf(
                    "Motor Grueso (MG):" to "¿Permanece sentado apoyándose en sus manos y se voltea boca abajo para arrastrarse/gatear?",
                    "Motor Fino (MF):" to "¿Agarra objetos pequeños usando sus dedos como rastrillo y golpea objetos entre sí?",
                    "Lenguaje / Comunicación (LE):" to "¿Emite dos sílabas iguales como 'ba-ba', 'da-da', 'ta-ta' y hace gestos para pedir?",
                    "Social / Afectivo (SO):" to "¿Reacciona contento ante personas conocidas y ayuda a sostener su vaso o taza?",
                    "Conocimiento / Cognitivo (CO):" to "¿Sigue con la mirada objetos que caen y explora activamente juguetes con ambas manos?"
                ),
                pauta1 = "Promover el gateo libre en piso seguro, juegos de causa-efecto y pinza inferior con trozos blandos de alimento.",
                pauta2 = "Estimular bisílabos ('ma-ma', 'pa-pa', 'da-da') y gestos comunicativos durante las rutinas diarias."
            )

            meses in 10..12 -> EdiAgeGroupTemplate(
                groupCode = "Grupo 7",
                fullLabel = "Grupo 7 (10m a 12m 29d)",
                saQuestions = listOf(
                    "Necesita apoyarse en sus manos para poder permanecer sentado",
                    "Se le dificulta desplazarse o gatear por sí mismo",
                    "Se le dificulta comunicarse con balbuceo o gestos (señalar, saludar)",
                    "Rara vez lo ha visto jugar o es indiferente a los objetos"
                ),
                areaQuestions = listOf(
                    "Motor Grueso (MG):" to "¿Se desplaza gateando (manos y rodillas) y puede caminar agarrado de los muebles?",
                    "Motor Fino (MF):" to "¿Sujeta un objeto pequeño con los dedos (pinza) y sabe aplaudir?",
                    "Lenguaje / Comunicación (LE):" to "¿Dice 'papá' o 'mamá' y detiene su actividad cuando le dicen 'no, no'?",
                    "Social / Afectivo (SO):" to "¿Empieza a comer por sí solo con los dedos y ayuda a meter el brazo por la manga al vestirlo?",
                    "Conocimiento / Cognitivo (CO):" to "¿Busca objetos que fueron escondidos frente a él (permanencia del objeto)?"
                ),
                pauta1 = "Estimular marcha lateral con apoyo en muebles seguros, pinza fina índice-pulgar y juegos de entregar/recibir objetos.",
                pauta2 = "Nombrar objetos cotidianos claramente, reforzar órdenes sencillas y asegurar paquete completo CRED de 1 año."
            )

            meses in 13..15 -> EdiAgeGroupTemplate(
                groupCode = "Grupo 8",
                fullLabel = "Grupo 8 (13m a 15m 29d)",
                saQuestions = listOf(
                    "No logra mantenerse parado aunque lo sujeten del tronco",
                    "Se le dificulta desplazarse solo o gatear",
                    "Aún balbucea y no se expresa con ninguna palabra con sentido",
                    "Se le dificulta comunicarse de forma verbal o gestual con su cuidador"
                ),
                areaQuestions = listOf(
                    "Motor Grueso (MG):" to "¿Camina solo, se para sin ayuda desde sentado y se agacha a recoger un objeto sin caerse?",
                    "Motor Fino (MF):" to "¿Mete y saca cosas de recipientes y toma una bolita de papel usando las yemas de los dedos?",
                    "Lenguaje / Comunicación (LE):" to "¿Señala su zapato o alguna parte del cuerpo y dice otra palabra además de papá y mamá?",
                    "Social / Afectivo (SO):" to "¿Come solo sin ayuda, abraza/besa a sus padres y obedece órdenes sencillas ('ven', 'dámelo')?",
                    "Conocimiento / Cognitivo (CO):" to "¿Explora recipientes metiendo y sacando al menos 4 objetos con intención funcional?"
                ),
                pauta1 = "Favorecer caminata independiente en espacios seguros, juegos de encajar/guardar bloques y señalar partes del cuerpo.",
                pauta2 = "Fomentar autonomía en la alimentación familiar y ampliar vocabulario nombrando alimentos y juguetes."
            )

            meses in 16..18 -> EdiAgeGroupTemplate(
                groupCode = "Grupo 9",
                fullLabel = "Grupo 9 (16m a 18m 29d)",
                saQuestions = listOf(
                    "Se comunica únicamente con balbuceo, gritos o ruidos aislados",
                    "Para desplazarse solamente gatea o se arrastra (no camina)",
                    "Muestra conductas repetitivas o estereotipadas (balancearse, golpearse)",
                    "Ha experimentado pérdida importante y constante de habilidades previas"
                ),
                areaQuestions = listOf(
                    "Motor Grueso (MG):" to "¿Se sienta solo en una silla pequeña, patea una pelota y corre aunque sea de forma torpe?",
                    "Motor Fino (MF):" to "¿Coloca cubos uno encima del otro, mete bolitas en un frasco e intenta garabatear con crayola?",
                    "Lenguaje / Comunicación (LE):" to "¿Dice cuatro palabras además de mamá y papá, señala 2-3 partes del cuerpo y nombra objetos?",
                    "Social / Afectivo (SO):" to "¿Utiliza la cuchara para comer, ayuda a desvestirse e imita tareas de la casa (barrer, sacudir)?",
                    "Conocimiento / Cognitivo (CO):" to "¿Reconoce y nombra objetos cotidianos en láminas y construye torres de 2 a 3 cubos?"
                ),
                pauta1 = "Estimular armado de torres con cubos, garabateo libre con crayolas gruesas y juegos de patear pelota.",
                pauta2 = "Conversar nombrando objetos y acciones cotidianas; vigilar hitos comunicativos y contacto visual conjunto."
            )

            meses in 19..24 -> EdiAgeGroupTemplate(
                groupCode = "Grupo 10",
                fullLabel = "Grupo 10 (19m a 24m 29d)",
                saQuestions = listOf(
                    "Tiene dificultad para seguir instrucciones sencillas como 'dame tu zapato'",
                    "Al caminar apoya primero los dedos o camina solo de puntitas",
                    "Muestra indiferencia excesiva al entorno y dificultad para relacionarse",
                    "Ha experimentado pérdida importante y constante de habilidades previas"
                ),
                areaQuestions = listOf(
                    "Motor Grueso (MG):" to "¿Corre sin caerse, patea la pelota sin perder el equilibrio y se sube solo a los muebles?",
                    "Motor Fino (MF):" to "¿Intenta abrir un frasco con tapa rosca, desenvuelve un objeto y come con cuchara sin tirar?",
                    "Lenguaje / Comunicación (LE):" to "¿Dice frases de dos palabras, dice 8 o más palabras y obedece órdenes sencillas sin gestos?",
                    "Social / Afectivo (SO):" to "¿Intenta hacer cosas solo, avisa cuando ha ensuciado el pañal y pide ayuda cuando no alcanza algo?",
                    "Conocimiento / Cognitivo (CO):" to "¿Resuelve tareas simples de giro de rosca y comprende órdenes verbales de un paso?"
                ),
                pauta1 = "Impulsar frases de 2 palabras ('mamá agua', 'dame pelota'), juegos de desenroscar envases y marcha ágil.",
                pauta2 = "Acompañar la autonomía en alimentación e inicio progresivo de reconocimiento de control de esfínteres."
            )

            meses in 25..36 -> EdiAgeGroupTemplate(
                groupCode = "Grupo 12",
                fullLabel = "Grupo 12 (25m a 36m 29d)",
                saQuestions = listOf(
                    "Babea constantemente o habla de manera poco entendible",
                    "Se comunica solo con palabras aisladas sin formar oraciones simples",
                    "Evita el contacto visual cuando le hablan o usa juguetes de forma repetitiva",
                    "Se cae frecuentemente y le cuesta trabajo subir o bajar escaleras"
                ),
                areaQuestions = listOf(
                    "Motor Grueso (MG):" to "¿Salta con ambos pies, se para en un solo pie, camina en puntas de pies y sube/baja escaleras?",
                    "Motor Fino (MF):" to "¿Hace hileras con 4 cubos, imita trazos verticales/horizontales y desabotona una prenda?",
                    "Lenguaje / Comunicación (LE):" to "¿Dice su nombre y edad, usa pronombres ('yo', 'mío') y forma oraciones de 3 palabras?",
                    "Social / Afectivo (SO):" to "¿Juega con otros niños, simula cosas que vio (juego simbólico) y se lava solo manos y cara?",
                    "Conocimiento / Cognitivo (CO):" to "¿Comprende conceptos de pertenencia ('mío'/'tuyo') y emplea plurales al hablar?"
                ),
                pauta1 = "Fomentar juego simbólico grupal, trazos verticales/horizontales, ensartado de cuentas y saltos bipodales.",
                pauta2 = "Estimular conversación con oraciones de 3 o más palabras y hábitos de higiene personal autónoma."
            )

            else -> EdiAgeGroupTemplate(
                groupCode = "Grupo 14",
                fullLabel = "Grupo 14 (37m a 59m 29d)",
                saQuestions = listOf(
                    "Le cuesta trabajo tomar un lápiz con dedos índice, pulgar y medio o copiar un círculo",
                    "Solamente dice frases de 2 palabras o usa incorrectamente plurales/pasado",
                    "Cuando está con otros niños los ignora y prefiere jugar completamente solo",
                    "Se distrae fácilmente y no logra concentrarse más de 5 minutos en una actividad"
                ),
                areaQuestions = listOf(
                    "Motor Grueso (MG):" to "¿Salta en un solo pie al menos 3 veces, atrapa una pelota grande y sube/baja escaleras sin apoyo?",
                    "Motor Fino (MF):" to "¿Dibuja un círculo, una cruz o un cuadrado y dibuja una figura humana con 2 a 4 partes del cuerpo?",
                    "Lenguaje / Comunicación (LE):" to "¿Cuenta lo que hizo ayer, pregunta '¿por qué?', usa tiempo futuro y puede relatar cuentos?",
                    "Social / Afectivo (SO):" to "¿Puede vestirse/desvestirse e ir al baño solo y juega asumiendo roles con otros niños?",
                    "Conocimiento / Cognitivo (CO):" to "¿Conoce el nombre de 4 colores, cuenta correctamente hasta 10 y usa 'ayer', 'hoy' y 'mañana'?"
                ),
                pauta1 = "Reforzar dibujo de figuras geométricas y figura humana, conteo numérico, reconocimiento de colores y lectura de cuentos.",
                pauta2 = "Promover socialización con pares, juegos de reglas y autonomía completa en vestido e higiene."
            )
        }
    }

    fun ediGrupoForAge(meses: Int, dias: Int): String =
        getEdiTemplateForAge(meses, dias).fullLabel
}

enum class EdiInternalView {
    SUMMARY_5_AXES,
    GROUP_BANK_15,
    QUESTIONNAIRE_AND_FALLBACK,
    RESULTS_TRACEABILITY
}

enum class EdiGroupFilterCategory(val label: String) {
    ALL("Todos (15)"),
    LACTANTES_MENORES("1m - 6m (G1-G5)"),
    LACTANTES_MAYORES("7m - 18m (G6-G9)"),
    NINOS_19_36("19m - 36m (G10-G12)"),
    PREESCOLARES("37m - 71m (G13-G15)")
}

data class EdiAgeDiscrepancyResolution(
    val fechaNacimiento: String,
    val fechaEvaluacion: String,
    val esPrematuro: Boolean,
    val egSemanas: Int,
    val egDias: Int,
    val selectedGroupNumber: Int,
    val clinicalJustificationNote: String
) {
    fun matchesInputs(
        fn: String,
        fe: String,
        prematuro: Boolean,
        sem: Int,
        dias: Int
    ): Boolean =
        fechaNacimiento == fn &&
            fechaEvaluacion == fe &&
            esPrematuro == prematuro &&
            egSemanas == sem &&
            egDias == dias
}

data class CredEdiUiState(
    val currentTab: AppTab = AppTab.CALCULADORA,
    // Tab 1: Calculadora & Antropometría CRED
    val sexo: SexoBiologico = SexoBiologico.NINO,
    val fechaNacimiento: String = "",
    val fechaEvaluacion: String = ClinicalAgeCalculator.todayFormatted(),
    val esPrematuro: Boolean = false,
    val egSemanas: Int = 32,
    val egDias: Int = 0,
    val pesoKg: Double = 5.40,
    val longitudCm: Double = 61.5,
    val perimetroCefalicoCm: Double = 39.5,
    val pesoInput: String = "",
    val longitudInput: String = "",
    val perimetroCefalicoInput: String = "",

    // Tab 2: Curvas de Crecimiento OMS
    val selectedIndicador: CurvaIndicador = CurvaIndicador.PE,

    // Tab 4 (Fase 4A - Motor Clínico EDI Peruano 2026)
    val ediInternalView: EdiInternalView = EdiInternalView.SUMMARY_5_AXES,
    val ediSelectedAxis: EdiAxis = EdiAxis.FRB,
    val ediSelectedDevAreaFilter: EdiDevArea? = null,
    val ediGroupSearchQuery: String = "",
    val ediGroupFilterCategory: EdiGroupFilterCategory = EdiGroupFilterCategory.ALL,
    val ediEngineState: EdiPhase1EngineState = EdiPhase1SessionManager.createInitialState(1),
    val ediAgeDiscrepancyResolution: EdiAgeDiscrepancyResolution? = null,
    val ediManualGroupOverride: Boolean = false,

    // Tab 4 Legacy properties kept intact exclusively for compatibility with legacy tests
    val showDatosEdiModal: Boolean = false,
    val ediFechaEval: String = fechaEvaluacion,
    val ediEgSemanasInput: String = "32",
    val frbItems: List<FrbItem> = listOf(
        FrbItem(0, "Asfixia perinatal / Apgar < 7 a los 5 min", false),
        FrbItem(1, "Prematurez < 37 semanas (32 sem EG)", false),
        FrbItem(2, "Peso al nacer < 1,500g o UCIN > 48 horas (18 d UCIN)", false),
        FrbItem(3, "Madre adolescente < 15 años o edad materna avanzada", false)
    ),
    val saItems: List<SaItem> = listOf(
        SaItem(0, "No fija la mirada en el examinador / cuidador", false),
        SaItem(1, "Ausencia de sonrisa social en interacción", false),
        SaItem(2, "Manos cerradas persistentes con pulgar incluido", false),
        SaItem(3, "Irritabilidad extrema e inconsolable persistente", false)
    ),
    val areaItems: List<EdiAreaItem> = listOf(
        EdiAreaItem(
            id = 0,
            code = "MG",
            title = "Motor Grueso (MG)",
            question = "¿Boca abajo se apoya en antebrazos y levanta la cabeza a 45°?",
            isAchieved = false
        ),
        EdiAreaItem(
            id = 1,
            code = "MF",
            title = "Motor Fino (MF)",
            question = "¿Mantiene manos abiertas transitoriamente y sigue objetos al centro?",
            isAchieved = false
        ),
        EdiAreaItem(
            id = 2,
            code = "LE",
            title = "Lenguaje / Comunicación (LE)",
            question = "¿Emite sonidos guturales ('agu', gorjeos) y reacciona a ruidos fuertes?",
            isAchieved = false
        ),
        EdiAreaItem(
            id = 3,
            code = "SO",
            title = "Social / Afectivo (SO)",
            question = "¿Sonríe en respuesta a la sonrisa o voz del examinador (sonrisa social)?",
            isAchieved = false
        ),
        EdiAreaItem(
            id = 4,
            code = "CO",
            title = "Conocimiento / Cognitivo (CO)",
            question = "¿Fija la mirada en el rostro del examinador y sigue estímulos?",
            isAchieved = false
        )
    ),
    // Eje 4: Exploración Neurológica (EN) - Desactivados por defecto y libres para marcar manualmente
    val enTonoSimetrico: Boolean = false,
    val enMoroPresente: Boolean = false,
    val enSuccionRitmica: Boolean = false,
    val enPrensionPresente: Boolean = false,
    val enMovilidadOcularNormal: Boolean = false
) {
    val deficitSemanas: Int
        get() = if (esPrematuro && egSemanas < 37) (40 - egSemanas).coerceAtLeast(0) else 0

    private val calculatedAges: AgeResult
        get() = ClinicalAgeCalculator.calculateAges(
            fechaNacimiento = fechaNacimiento,
            fechaEvaluacion = fechaEvaluacion,
            esPrematuro = esPrematuro,
            egSemanas = egSemanas,
            egDias = egDias
        )

    val edadCronologicaAnios: Int
        get() = calculatedAges.cronologicaAnios

    val edadCronologicaMesesRestantes: Int
        get() = calculatedAges.cronologicaMesesDelAnio

    val edadCronologicaMeses: Int
        get() = calculatedAges.cronologicaMeses

    val edadCronologicaDias: Int
        get() = calculatedAges.cronologicaDias

    val edadCronologicaSemanas: Int
        get() = calculatedAges.cronologicaSemanas

    val edadCronologicaTotalDias: Int
        get() = calculatedAges.cronologicaTotalDias

    val edadCronologicaAMD: String
        get() = calculatedAges.edadCronologicaAMD

    val edadCronologicaCompleta: String
        get() = calculatedAges.edadCronologicaCompleta

    val edadCorregidaAnios: Int
        get() = calculatedAges.corregidaAnios

    val edadCorregidaMesesRestantes: Int
        get() = calculatedAges.corregidaMesesDelAnio

    val edadCorregidaMeses: Int
        get() = calculatedAges.corregidaMeses

    val edadCorregidaDias: Int
        get() = calculatedAges.corregidaDias

    val diasPrematuridad: Int
        get() = calculatedAges.diasRestarPrematuridad

    val diasRestarPrematuridad: Int
        get() = calculatedAges.diasRestarPrematuridad

    val edadCorregidaTotalDias: Int
        get() = calculatedAges.corregidaTotalDias

    val aplicaEdadCorregida: Boolean
        get() = calculatedAges.aplicaEdadCorregida

    val fechaAjustada: String
        get() = calculatedAges.fechaAjustada

    val edadCorregidaNumericaAMD: String
        get() = calculatedAges.edadCorregidaNumericaAMD

    val edadCorregidaAMD: String
        get() = calculatedAges.edadCorregidaAMD

    val edadCorregidaCompleta: String
        get() = calculatedAges.edadCorregidaCompleta

    val usaEdadCorregida: Boolean
        get() = calculatedAges.aplicaEdadCorregida

    val edadEfectivaMeses: Int
        get() = if (usaEdadCorregida) edadCorregidaMeses else edadCronologicaMeses

    val edadEfectivaAnios: Int
        get() = edadEfectivaMeses / 12

    val edadEfectivaMesesRestantes: Int
        get() = edadEfectivaMeses % 12

    val edadEfectivaDias: Int
        get() = if (usaEdadCorregida) edadCorregidaDias else edadCronologicaDias

    val edadEfectivaAMD: String
        get() = if (usaEdadCorregida) edadCorregidaNumericaAMD else edadCronologicaAMD

    val edadEfectivaTipoLabel: String
        get() = if (usaEdadCorregida) "Edad Corregida" else "Edad Cronológica"

    val grupoEtarioOmsClasificado: String
        get() = if (edadEfectivaMeses >= 24) "2 a 5 años (24-60m)" else "0 a 2 años (0-24m)"

    val ediTemplate: EdiAgeGroupTemplate
        get() = ClinicalAgeCalculator.getEdiTemplateForAge(edadEfectivaMeses, edadEfectivaDias)

    val ediGrupoEtario: String
        get() = ediTemplate.fullLabel

    val ediGrupoCorto: String
        get() = ediTemplate.groupCode

    // Próximo Control CRED según el esquema de periodicidad NTS N° 238-MINSA/DGIESP-2025
    val proximoControlCronologico: NextCredControlInfo
        get() = ClinicalAgeCalculator.calculateNextCredControl(
            fechaEvaluacion = fechaEvaluacion,
            edadMeses = edadCronologicaMeses,
            edadDias = edadCronologicaDias
        )

    val proximoControlCorregido: NextCredControlInfo
        get() = ClinicalAgeCalculator.calculateNextCredControl(
            fechaEvaluacion = fechaEvaluacion,
            edadMeses = edadEfectivaMeses,
            edadDias = edadEfectivaDias
        )

    val proximoControlInfo: NextCredControlInfo
        get() = if (usaEdadCorregida) proximoControlCorregido else proximoControlCronologico

    val proximoControlFecha: String
        get() = proximoControlInfo.fechaFormateada

    val hasPesoIngresado: Boolean
        get() = pesoInput.toDoubleOrNull()?.let { it > 0.0 } == true

    val hasLongitudIngresada: Boolean
        get() = longitudInput.toDoubleOrNull()?.let { it > 0.0 } == true

    val hasPerimetroCefalicoIngresado: Boolean
        get() = perimetroCefalicoInput.toDoubleOrNull()?.let { it > 0.0 } == true

    val datosAntropometricosCount: Int
        get() = listOf(hasPesoIngresado, hasLongitudIngresada, hasPerimetroCefalicoIngresado).count { it }

    val imcCalculado: Double
        get() {
            val tallaMetros = longitudCm / 100.0
            return if (tallaMetros > 0) pesoKg / (tallaMetros * tallaMetros) else 0.0
        }

    // WHO Indicator Evaluations (P/E, T/E, P/T, IMC/E, PC/E) evaluated using exact Años, Meses y Días
    val evalPE: IndicatorEvaluation
        get() = WhoGrowthStandards.evaluateIndicator(
            indicador = CurvaIndicador.PE,
            sexo = sexo,
            edadMeses = edadEfectivaMeses,
            edadDias = edadEfectivaDias,
            pesoKg = pesoKg,
            longitudCm = longitudCm,
            imcCalculado = imcCalculado
        )

    val evalTE: IndicatorEvaluation
        get() = WhoGrowthStandards.evaluateIndicator(
            indicador = CurvaIndicador.TE,
            sexo = sexo,
            edadMeses = edadEfectivaMeses,
            edadDias = edadEfectivaDias,
            pesoKg = pesoKg,
            longitudCm = longitudCm,
            imcCalculado = imcCalculado
        )

    val evalPT: IndicatorEvaluation
        get() = WhoGrowthStandards.evaluateIndicator(
            indicador = CurvaIndicador.PT,
            sexo = sexo,
            edadMeses = edadEfectivaMeses,
            edadDias = edadEfectivaDias,
            pesoKg = pesoKg,
            longitudCm = longitudCm,
            imcCalculado = imcCalculado
        )

    val evalIMCE: IndicatorEvaluation
        get() = WhoGrowthStandards.evaluateIndicator(
            indicador = CurvaIndicador.IMCE,
            sexo = sexo,
            edadMeses = edadEfectivaMeses,
            edadDias = edadEfectivaDias,
            pesoKg = pesoKg,
            longitudCm = longitudCm,
            imcCalculado = imcCalculado,
            perimetroCefalicoCm = perimetroCefalicoCm
        )

    val evalPCE: IndicatorEvaluation
        get() = WhoGrowthStandards.evaluateIndicator(
            indicador = CurvaIndicador.PCE,
            sexo = sexo,
            edadMeses = edadEfectivaMeses,
            edadDias = edadEfectivaDias,
            pesoKg = pesoKg,
            longitudCm = longitudCm,
            imcCalculado = imcCalculado,
            perimetroCefalicoCm = perimetroCefalicoCm
        )

    val currentCurvaEvaluation: IndicatorEvaluation
        get() = when (selectedIndicador) {
            CurvaIndicador.PE -> evalPE
            CurvaIndicador.TE -> evalTE
            CurvaIndicador.PT -> evalPT
            CurvaIndicador.IMCE -> evalIMCE
            CurvaIndicador.PCE -> evalPCE
        }

    val globalDiagnosis: GlobalDiagnosticSummary
        get() = WhoGrowthStandards.evaluateGlobalDiagnosis(
            sexo = sexo,
            edadMeses = edadEfectivaMeses,
            edadDias = edadEfectivaDias,
            pesoKg = pesoKg,
            longitudCm = longitudCm,
            perimetroCefalicoCm = perimetroCefalicoCm,
            peEval = evalPE,
            teEval = evalTE,
            ptEval = evalPT,
            imcEval = evalIMCE
        )

    val zScorePE: Double
        get() = evalPE.zScore

    val zScoreTE: Double
        get() = evalTE.zScore

    val zScorePT: Double
        get() = evalPT.zScore

    val zScoreImc: Double
        get() = evalIMCE.zScore

    val zScorePC: Double
        get() = globalDiagnosis.pcZScore

    val frbDetectadosCount: Int
        get() = frbItems.count { it.checked }

    val saPresentesCount: Int
        get() = saItems.count { it.isPresent }

    val hitosLogradosCount: Int
        get() = areaItems.count { it.isAchieved }

    val hitosTotalesCount: Int
        get() = areaItems.size

    val fallasAreasCount: Int
        get() = (hitosTotalesCount - hitosLogradosCount).coerceAtLeast(0)

    // EN 1.3 Manual EDI (p. 13): Perímetro cefálico normal entre -2 DE y +2 DE para su edad (corregida si <24m prematuro)
    val pcEnRangoNormalOms: Boolean
        get() = (!hasPerimetroCefalicoIngresado && ClinicalAgeCalculator.parseDate(fechaNacimiento) == null) ||
            (zScorePC in -2.0..2.0)

    val enItemsMarcadosCount: Int
        get() = listOf(
            enTonoSimetrico,
            enMoroPresente,
            enSuccionRitmica,
            enPrensionPresente,
            enMovilidadOcularNormal
        ).count { it }

    val enItemsTotalesCount: Int
        get() = 5

    val exploracionNeurologicaNormal: Boolean
        get() = enTonoSimetrico &&
            enMoroPresente &&
            enSuccionRitmica &&
            enPrensionPresente &&
            enMovilidadOcularNormal &&
            pcEnRangoNormalOms

    val areasLogradasSubtitle: String
        get() {
            val achievedCodes = areaItems.filter { it.isAchieved }.map { it.code }
            return when {
                achievedCodes.size == areaItems.size -> "MG · MF · LE · SO · CO logrados"
                achievedCodes.isEmpty() -> "Ninguna área lograda (Requiere evaluación)"
                else -> "${achievedCodes.joinToString(" · ")} logrados"
            }
        }

    val frbResumenSubtitle: String
        get() {
            return when {
                frbDetectadosCount == 0 -> "Sin factores de riesgo biológico registrados"
                frbItems.getOrNull(1)?.checked == true && frbItems.getOrNull(2)?.checked == true && frbDetectadosCount == 2 ->
                    "Prematurez ${egSemanas}s · UCIN 18 días"
                else -> {
                    val labels = mutableListOf<String>()
                    if (frbItems.getOrNull(0)?.checked == true) labels.add("Asfixia/Apgar<7")
                    if (frbItems.getOrNull(1)?.checked == true) labels.add("Prematurez ${egSemanas}s")
                    if (frbItems.getOrNull(2)?.checked == true) labels.add("UCIN 18 días")
                    if (frbItems.getOrNull(3)?.checked == true) labels.add("Riesgo materno")
                    labels.joinToString(" · ")
                }
            }
        }

    /**
     * Calificación global oficial del Semáforo EDI según el Manual para la Aplicación
     * de la Prueba EDI (Versión Peruana 2026, págs. 8, 10, 13 y 17-48):
     * - ROJO (Riesgo de retraso):
     *   * 1 o más ítems de Exploración Neurológica alterados (incluyendo PC < -2 DE o > +2 DE), o
     *   * > 2 áreas del desarrollo no logradas, o
     *   * > 2 señales de alerta presentes, o
     *   * >= 1 área en amarillo/no lograda combinada con señales de alerta y factores de riesgo biológico.
     * - AMARILLO (Rezago en el desarrollo):
     *   * 1 a 2 áreas del desarrollo no logradas, o
     *   * 1 a 2 señales de alerta presentes, o
     *   * >= 3 factores de riesgo biológico acumulados.
     * - VERDE (Desarrollo Normal):
     *   * 5/5 áreas del desarrollo logradas, 0 señales de alerta y exploración neurológica normal.
     */
    val semaforoCalculado: SemaforoColor
        get() = when {
            !exploracionNeurologicaNormal -> SemaforoColor.ROJO
            fallasAreasCount > 2 -> SemaforoColor.ROJO
            saPresentesCount > 2 -> SemaforoColor.ROJO
            fallasAreasCount >= 1 && saPresentesCount >= 1 && frbDetectadosCount >= 1 -> SemaforoColor.ROJO
            fallasAreasCount in 1..2 -> SemaforoColor.AMARILLO
            saPresentesCount in 1..2 -> SemaforoColor.AMARILLO
            frbDetectadosCount >= 3 -> SemaforoColor.AMARILLO
            else -> SemaforoColor.VERDE
        }

    // --- Motor Clínico EDI Peruano 2026 (Fases 1, 2 y 3 integrados) ---

    val ediGestationalAgeInput: GestationalAgeInput
        get() = GestationalAgeInput(weeks = egSemanas, days = egDias)

    val ediHasFechaNacimientoValida: Boolean
        get() = ClinicalAgeCalculator.parseDate(fechaNacimiento) != null &&
            ClinicalAgeCalculator.parseDate(fechaEvaluacion) != null

    val ediCorrectedAgeComparison: EdiCorrectedAgeComparison
        get() = EdiOfficialCatalog.compareCorrectedAgeMethods(
            fechaNacimiento = fechaNacimiento,
            fechaEvaluacion = fechaEvaluacion,
            esPrematuro = esPrematuro,
            egSemanas = egSemanas,
            egDias = egDias
        )

    val ediEffectiveGroupNumber: Int
        get() = ediEngineState.activeGroupNumber

    val ediActiveGroupSpec: EdiGroupSpec
        get() = EdiOfficialCatalog.getGroup(ediEffectiveGroupNumber)

    val ediIsDiscrepancyResolvedForCurrentInputs: Boolean
        get() {
            val res = ediAgeDiscrepancyResolution ?: return false
            val comparison = ediCorrectedAgeComparison
            if (comparison.autoSelectionStatus != EdiGroupAutoSelectionStatus.BLOCKED_GROUP_DISCREPANCY_REQUIRES_PROFESSIONAL_REVIEW) {
                return false
            }
            val validCandidate = comparison.candidateGroupsForProfessionalReview
                .any { it.groupNumber == res.selectedGroupNumber }
            return validCandidate &&
                res.clinicalJustificationNote.isNotBlank() &&
                res.matchesInputs(fechaNacimiento, fechaEvaluacion, esPrematuro, egSemanas, egDias) &&
                ediEffectiveGroupNumber == res.selectedGroupNumber
        }

    val ediHasUnresolvedAgeDiscrepancy: Boolean
        get() = ediHasFechaNacimientoValida &&
            ediCorrectedAgeComparison.autoSelectionStatus ==
            EdiGroupAutoSelectionStatus.BLOCKED_GROUP_DISCREPANCY_REQUIRES_PROFESSIONAL_REVIEW &&
            !ediIsDiscrepancyResolvedForCurrentInputs

    val ediIsOutOfOfficialAgeRange: Boolean
        get() = ediHasFechaNacimientoValida && (
            ediCorrectedAgeComparison.autoSelectionStatus == EdiGroupAutoSelectionStatus.OUT_OF_RANGE_UNDER_1_MONTH ||
                ediCorrectedAgeComparison.autoSelectionStatus == EdiGroupAutoSelectionStatus.OUT_OF_RANGE_72_MONTHS_OR_MORE
            )

    /**
     * Resultado clínico visible y oficial para la Ventana 4 (EDI).
     * Procede exclusivamente de [EdiGlobalSemaphoreEngine] consumiendo [EdiAreaFallbackEngine]
     * y [EdiOfficialCatalog]. Jamás utiliza `semaforoCalculado` ni plantillas antiguas.
     */
    val ediGlobalEvaluation: EdiGlobalEvaluationResult
        get() {
            val comparison = ediCorrectedAgeComparison
            val ageComparisonForEngine: EdiCorrectedAgeComparison? = when {
                // Sin fecha de nacimiento ingresada: permite evaluar cualquier grupo seleccionado desde el banco
                fechaNacimiento.isBlank() -> null
                // Con fecha ingresada pero formato inválido o fuera de rango (<1m o >=72m): siempre bloquea
                !ediHasFechaNacimientoValida -> comparison
                ediIsOutOfRangeForComparison(comparison) -> comparison
                // Discrepancia entre Manual EDI y Calendario CRED: bloquea hasta resolución profesional documentada
                comparison.autoSelectionStatus == EdiGroupAutoSelectionStatus.BLOCKED_GROUP_DISCREPANCY_REQUIRES_PROFESSIONAL_REVIEW ->
                    if (ediIsDiscrepancyResolvedForCurrentInputs) null else comparison
                // Asignación consistente: si el profesional eligió manualmente otro grupo en el banco, no bloquea; si no, valida consistencia
                ediManualGroupOverride -> null
                else -> comparison
            }
            return EdiGlobalSemaphoreEngine.evaluateGlobalSemaphore(
                state = ediEngineState,
                ageComparison = ageComparisonForEngine
            )
        }

    private fun ediIsOutOfRangeForComparison(comparison: EdiCorrectedAgeComparison): Boolean =
        comparison.autoSelectionStatus == EdiGroupAutoSelectionStatus.OUT_OF_RANGE_UNDER_1_MONTH ||
            comparison.autoSelectionStatus == EdiGroupAutoSelectionStatus.OUT_OF_RANGE_72_MONTHS_OR_MORE

    val ediAxis3AreasSummary: EdiAxis3AreasSummary
        get() = ediGlobalEvaluation.axis3AreasSummary

    val ediMandatoryItemsProgress: Pair<Int, Int>
        get() {
            val spec = ediActiveGroupSpec
            val eval = ediGlobalEvaluation
            val session = ediEngineState.activeSession
            val totalFrb = if (eval.frbMandatoryForGroup) EdiOfficialCatalog.frbItems.size else 0
            val doneFrb = if (eval.frbMandatoryForGroup) {
                eval.frbResponsesResolved.values.count { it.isAnsweredDefinitively }
            } else 0
            val totalAle = if (eval.aleMandatoryForGroup) spec.aleItems.size else 0
            val doneAle = if (eval.aleMandatoryForGroup) {
                eval.aleResponsesResolved.values.count { it.isAnsweredDefinitively }
            } else 0
            val applicableAreas = eval.axis3AreasSummary.areaResults.values.filter { it.appliesToGroup }
            val totalAreasItems = applicableAreas.sumOf { areaRes ->
                areaRes.currentItems.size + if (areaRes.fallbackRequired) areaRes.fallbackItems.size else 0
            }
            val doneAreasItems = applicableAreas.sumOf { areaRes ->
                val currDone = areaRes.currentItemResponses.values.count { it.isAnsweredDefinitively }
                val fbDone = if (areaRes.fallbackRequired) {
                    areaRes.activeFallbackResponses.values.count { it.isAnsweredDefinitively }
                } else 0
                currDone + fbDone
            }
            val totalEn = EdiOfficialCatalog.getApplicableEnItems(spec.groupNumber).size
            val doneEn = eval.enResponsesResolved.values.count { it.isAnsweredDefinitively }
            val totalAla = spec.alaItems.size
            val doneAla = eval.alaResponsesResolved.values.count { it.isAnsweredDefinitively }
            val total = totalFrb + totalAle + totalAreasItems + totalEn + totalAla
            val done = (doneFrb + doneAle + doneAreasItems + doneEn + doneAla).coerceAtMost(total)
            // Also keep reference to session so snapshot updates re-evaluate cleanly
            val _keep = session.groupNumber
            return done to total.coerceAtLeast(1)
        }
}
