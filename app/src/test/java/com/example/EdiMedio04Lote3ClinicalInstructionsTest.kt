package com.example

import com.example.model.ClassificationSeverity
import com.example.model.CurvaIndicador
import com.example.model.EdiAreaFallbackEngine
import com.example.model.EdiClinicalModality
import com.example.model.EdiDevArea
import com.example.model.EdiGlobalTrafficLight
import com.example.model.EdiItemResponse
import com.example.model.EdiOfficialCatalog
import com.example.model.GlobalDiagnosisCompleteness
import com.example.model.SexoBiologico
import com.example.model.WhoGrowthStandards
import com.example.viewmodel.CredEdiViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * MEDIO-04 — LOTE 3:
 * Restitución literal de tres fragmentos de instrucciones clínicas omitidos en EdiOfficialCatalog.kt
 * según el Manual EDI Peruano (Primera Edición, julio 2026):
 * - Hallazgo #1: EN 1.2 (PDF p. 13 / manual p. 14)
 * - Hallazgo #2: SO 4.2 (PDF p. 65 / manual p. 66)
 * - Hallazgo #3: LE 11.2 (PDF p. 92 / manual p. 93)
 */
class EdiMedio04Lote3ClinicalInstructionsTest {

    @Test
    fun `caso A EN 1_2 contiene instruccion clinica completa y literal del Manual EDI 2026`() {
        val en12 = EdiOfficialCatalog.allEnItems.firstOrNull { it.uniqueId == "EN_1_2" }
        assertNotNull(en12)
        val expected =
            "Coloque al niño acostado o sentado sobre la mesa de exploración. " +
                "Sujete con la mano derecha el juguete llamativo no sonoro a 20 cm frente al niño y después de obtener la fijación visual hacia el juguete, " +
                "desplácelo lentamente en el eje horizontal, vertical y en ambas diagonales. " +
                "Analice la presencia o ausencia de mirada binocular en posición primaria (ojos viendo al frente), " +
                "incapacidad de la movilidad de uno o ambos ojos o alguna alteración de la mirada primaria conjugada. " +
                "Después con el niño en la misma posición, observe la asimetría en la expresión facial. " +
                "En caso de estar presente, puede ser más visible cuando el niño llora, ya que se observa que el lado afectado permanece fijo, " +
                "la boca se ve desviada al lado opuesto (sano) y el párpado queda entreabierto mientras se cierra en lado sano."

        assertEquals(expected, en12!!.clinicalInstruction)
        assertTrue(
            en12.clinicalInstruction.endsWith(
                "En caso de estar presente, puede ser más visible cuando el niño llora, ya que se observa que el lado afectado permanece fijo, " +
                    "la boca se ve desviada al lado opuesto (sano) y el párpado queda entreabierto mientras se cierra en lado sano."
            )
        )
    }

    @Test
    fun `caso B SO 4_2 contiene pregunta completa sobre estrategias para impedir que el adulto se aleje en posicion oficial`() {
        val g4 = EdiOfficialCatalog.getGroup(4)
        val so42 = g4.areaItems[EdiDevArea.SO]?.firstOrNull { it.uniqueId == "G4_SO_4_2" }
        assertNotNull(so42)

        val expected =
            "Pregunte a la madre, padre o cuidador principal: ¿cómo manifiesta que quiere jugar con los adultos?, " +
                "¿llora, grita, jala la ropa o toca al adulto cuando quiere que le presten atención y jueguen con él?, " +
                "¿qué hace el niño cuando el adulto deja de jugar con él e intenta alejarse? " +
                "¿Recurre el niño a estrategias para impedir que el adulto se aleje? " +
                "Defina la frecuencia e intensidad de la conducta en comparación con otros niños de su misma edad."

        assertEquals(expected, so42!!.clinicalInstruction)

        val prevSentence = "¿qué hace el niño cuando el adulto deja de jugar con él e intenta alejarse?"
        val restoredSentence = "¿Recurre el niño a estrategias para impedir que el adulto se aleje?"
        val nextSentence = "Defina la frecuencia e intensidad de la conducta en comparación con otros niños de su misma edad."
        val idxPrev = so42.clinicalInstruction.indexOf(prevSentence)
        val idxRestored = so42.clinicalInstruction.indexOf(restoredSentence)
        val idxNext = so42.clinicalInstruction.indexOf(nextSentence)
        assertTrue(idxPrev >= 0 && idxRestored > idxPrev && idxNext > idxRestored)
    }

    @Test
    fun `caso C LE 11_2 contiene las dos oraciones oficiales sobre frecuencia intensidad y tercera persona en posicion exacta`() {
        val g11 = EdiOfficialCatalog.getGroup(11)
        val le112 = g11.areaItems[EdiDevArea.LE]?.firstOrNull { it.uniqueId == "G11_LE_11_2" }
        assertNotNull(le112)

        val expected =
            "Pregunte a la madre, padre o cuidador principal si utiliza de manera espontánea y apropiada pronombres personales como: " +
                "\"yo\" (cuando se refiere a sí mismo), \"tú\" (cuando se dirige a otro), \"él/ella\" (cuando habla de una tercera persona) " +
                "y el pronombre posesivo \"mío\" (cuando se refiere a algo que le pertenece). " +
                "Defina la frecuencia e intensidad de la conducta en comparación con otros niños de su misma edad. " +
                "Asegúrese que no se refiera a sí mismo en tercera persona (hablar de sí mismo como si fuera otra persona). " +
                "Es suficiente con que utilice una de estas palabras correctamente."

        assertEquals(expected, le112!!.clinicalInstruction)

        val pronounsSentence = "y el pronombre posesivo \"mío\" (cuando se refiere a algo que le pertenece)."
        val freqSentence = "Defina la frecuencia e intensidad de la conducta en comparación con otros niños de su misma edad."
        val thirdPersonSentence = "Asegúrese que no se refiera a sí mismo en tercera persona (hablar de sí mismo como si fuera otra persona)."
        val closingSentence = "Es suficiente con que utilice una de estas palabras correctamente."

        val i1 = le112.clinicalInstruction.indexOf(pronounsSentence)
        val i2 = le112.clinicalInstruction.indexOf(freqSentence)
        val i3 = le112.clinicalInstruction.indexOf(thirdPersonSentence)
        val i4 = le112.clinicalInstruction.indexOf(closingSentence)
        assertTrue(i1 >= 0 && i2 > i1 && i3 > i2 && i4 > i3)
    }

    @Test
    fun `caso D integridad completa del catalogo oficial con 270 identificadores unicos`() {
        assertEquals(15, EdiOfficialCatalog.groups.size)
        val allItems = EdiOfficialCatalog.groups.flatMap { spec ->
            spec.areaItems.values.flatten() + spec.alaItems + spec.aleItems
        } + EdiOfficialCatalog.frbItems +
            EdiOfficialCatalog.allEnItems +
            EdiOfficialCatalog.specialGroup13CoRFallbackItems

        val uniqueIds = allItems.map { it.uniqueId }
        assertEquals(270, uniqueIds.size)
        assertEquals(270, uniqueIds.toSet().size)
        assertTrue(allItems.all { it.clinicalInstruction.isNotBlank() })
    }

    @Test
    fun `caso E preservacion de modalidad polaridad materiales y preguntas en EN 1_2 SO 4_2 y LE 11_2`() {
        val en12 = EdiOfficialCatalog.allEnItems.first { it.uniqueId == "EN_1_2" }
        assertEquals("EN 1.2", en12.itemCode)
        assertEquals(EdiClinicalModality.DIRECT_OBSERVATION, en12.modality)
        assertTrue(en12.hasObservationAsterisks)
        assertFalse(en12.yesIsNormalGreen)
        assertEquals(listOf("Juguete llamativo no sonoro"), en12.generalHeaderMaterials)
        assertEquals(listOf("Juguete llamativo no sonoro"), en12.protocolRequiredMaterials)
        assertTrue(en12.protocolRequiredPlates.isEmpty())
        assertEquals(
            "**2. ¿Presenta alteración o asimetría en la movilidad de ojos o expresión facial? (Observe)",
            en12.formatQuestionText
        )
        assertEquals(
            "**EN 1.2. ¿Presenta alteración o asimetría en la movilidad de ojos o expresión facial?",
            en12.consultationQuestionText
        )

        val so42 = EdiOfficialCatalog.getGroup(4).areaItems[EdiDevArea.SO]!!.first { it.uniqueId == "G4_SO_4_2" }
        assertEquals("SO 4.2", so42.itemCode)
        assertEquals(EdiClinicalModality.DIRECTED_INTERVIEW, so42.modality)
        assertFalse(so42.hasObservationAsterisks)
        assertTrue(so42.yesIsNormalGreen)
        assertTrue(so42.generalHeaderMaterials.isEmpty())
        assertTrue(so42.protocolRequiredMaterials.isEmpty())
        assertTrue(so42.protocolRequiredPlates.isEmpty())
        assertEquals("2. ¿Le gusta jugar con otras personas y llora cuando dejan de jugar con él?", so42.formatQuestionText)
        assertEquals("SO 4.2. ¿Le gusta jugar con otras personas y llora cuando dejan de jugar con él?", so42.consultationQuestionText)

        val le112 = EdiOfficialCatalog.getGroup(11).areaItems[EdiDevArea.LE]!!.first { it.uniqueId == "G11_LE_11_2" }
        assertEquals("LE 11.2", le112.itemCode)
        assertEquals(EdiClinicalModality.DIRECTED_INTERVIEW, le112.modality)
        assertFalse(le112.hasObservationAsterisks)
        assertTrue(le112.yesIsNormalGreen)
        assertTrue(le112.generalHeaderMaterials.isEmpty())
        assertTrue(le112.protocolRequiredMaterials.isEmpty())
        assertTrue(le112.protocolRequiredPlates.isEmpty())
        assertEquals("2. ¿Utiliza palabras como \"yo\", \"mío\", \"él\", \"ella\", \"tú\"?", le112.formatQuestionText)
        assertEquals("LE 11.2. ¿Utiliza palabras como \"yo\", \"mío\", \"él\", \"ella\", \"tú\"?", le112.consultationQuestionText)
    }

    @Test
    fun `caso F retrocesos G5 hacia G4 SO 4_2 y G12 hacia G11 LE 11_2 heredan textos completos restituidos`() {
        val g5SoFallback = EdiAreaFallbackEngine.getFallbackItemsForArea(5, EdiDevArea.SO)
        val g5RetroSo42 = g5SoFallback.firstOrNull { it.sourceItem.uniqueId == "G4_SO_4_2" }
        assertNotNull(g5RetroSo42)
        assertEquals("G5_RETRO_G4_SO_2", g5RetroSo42!!.fallbackUniqueId)
        assertTrue(
            g5RetroSo42.sourceItem.clinicalInstruction.contains(
                "¿Recurre el niño a estrategias para impedir que el adulto se aleje?"
            )
        )

        val g12LeFallback = EdiAreaFallbackEngine.getFallbackItemsForArea(12, EdiDevArea.LE)
        val g12RetroLe112 = g12LeFallback.firstOrNull { it.sourceItem.uniqueId == "G11_LE_11_2" }
        assertNotNull(g12RetroLe112)
        assertEquals("G12_RETRO_G11_LE_2", g12RetroLe112!!.fallbackUniqueId)
        assertTrue(
            g12RetroLe112.sourceItem.clinicalInstruction.contains(
                "Defina la frecuencia e intensidad de la conducta en comparación con otros niños de su misma edad. " +
                    "Asegúrese que no se refiera a sí mismo en tercera persona (hablar de sí mismo como si fuera otra persona)."
            )
        )
    }

    @Test
    fun `caso G EN 1_2 se aplica en grupos 1 a 15 mientras EN 1_3 no aplica en grupo 15`() {
        for (groupNumber in 1..14) {
            val applicable = EdiOfficialCatalog.getApplicableEnItems(groupNumber)
            assertEquals(listOf("EN_1_1", "EN_1_2", "EN_1_3"), applicable.map { it.uniqueId })
            val en12 = applicable.first { it.uniqueId == "EN_1_2" }
            assertTrue(en12.clinicalInstruction.contains("el párpado queda entreabierto mientras se cierra en lado sano."))
        }

        val g15En = EdiOfficialCatalog.getApplicableEnItems(15)
        assertEquals(listOf("EN_1_1", "EN_1_2"), g15En.map { it.uniqueId })
        assertFalse(g15En.any { it.uniqueId == "EN_1_3" })
        assertTrue(
            g15En.first { it.uniqueId == "EN_1_2" }.clinicalInstruction.contains(
                "el párpado queda entreabierto mientras se cierra en lado sano."
            )
        )
    }

    @Test
    fun `caso H regresion de lotes 1 2 y 2B intactos sin alteraciones`() {
        // Lote 1: alertas críticas en evaluación INCOMPLETE
        val vmAlert = CredEdiViewModel()
        vmAlert.updateFechaNacimiento("15/06/2022")
        vmAlert.updateFechaEvaluacion("15/06/2026") // 48m 0d -> G13
        val en12 = EdiOfficialCatalog.getApplicableEnItems(vmAlert.uiState.value.ediEffectiveGroupNumber)
            .first { it.uniqueId == "EN_1_2" }
        assertTrue(en12.clinicalInstruction.contains("cuando el niño llora"))
        vmAlert.recordEdiCurrentItemResponse(en12.uniqueId, EdiItemResponse.YES)
        val evalIncomplete = vmAlert.uiState.value.ediGlobalEvaluation
        assertEquals(EdiGlobalTrafficLight.INCOMPLETE, evalIncomplete.trafficLight)
        assertTrue(evalIncomplete.requiresImmediateClinicalAttentionDespiteIncomplete)
        assertTrue(evalIncomplete.priorityCriticalAlerts.any { it.code == "EN 1.2" })

        // Lote 2: > 60m0d sin recorte silencioso a 60m
        val overAgePe = WhoGrowthStandards.evaluateIndicator(CurvaIndicador.PE, SexoBiologico.NINA, 60, 20.0, 112.0, 15.9, edadDias = 1)
        assertFalse(overAgePe.isReferenceAvailable)
        assertTrue(overAgePe.zScore.isNaN())

        // Lote 2B: P/T fuera de rango en edad válida emite PARTIAL y nunca Eutrófico / Crecimiento Adecuado ni Ganancia Óptima
        val vmPartial = CredEdiViewModel()
        vmPartial.selectSexo(SexoBiologico.NINO)
        vmPartial.updateFechaNacimiento("15/06/2022")
        vmPartial.updateFechaEvaluacion("15/06/2026")
        vmPartial.updatePesoInput("17.50")
        vmPartial.updateLongitudInput("110.5")
        vmPartial.updatePerimetroCefalicoInput("50.2")
        val partialGlobal = vmPartial.uiState.value.globalDiagnosis
        assertEquals(GlobalDiagnosisCompleteness.PARTIAL, partialGlobal.completeness)
        assertEquals(ClassificationSeverity.WARNING, partialGlobal.severity)
        assertFalse(partialGlobal.title.contains("Eutrófico / Crecimiento Adecuado"))
        assertFalse(partialGlobal.gananciaStatusTitle == "Ganancia Ponderal Óptima")

        // Lote 2B: entradas no finitas rechazadas explícitamente
        val ref48 = WhoGrowthStandards.RefPoint(48f, 11.2f, 12.7f, 14.4f, 16.3f, 18.6f, 21.2f, 24.2f)
        assertTrue(WhoGrowthStandards.computeZScore(Double.NaN, ref48).isNaN())
        assertTrue(WhoGrowthStandards.computeZScore(Double.POSITIVE_INFINITY, ref48).isNaN())
    }
}
