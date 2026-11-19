# Informe de Auditoría Integral — Fase 5 (CRED & EDI Antropometría 1.0)

**Fecha de auditoría:** 2026-11-12  
**Alcance autorizado:** Fase 5 — Solo lectura, ejecución de pruebas y auditoría visual/documental (0 modificaciones al código de producción).  
**Política de datos:** Todas las evaluaciones y fichas PDF generadas emplean exclusivamente datos sintéticos/ficticios.

---

## 1. Verificación de Integridad y Solo Lectura (SHA-256)

Se tomó huella criptográfica `sha256sum` de los **41 archivos** de código fuente de producción y configuración Gradle (`app/src/main/**`, `app/build.gradle.kts`, `gradle/libs.versions.toml`, `settings.gradle.kts`) antes y después de la auditoría:
- Línea base: `/tmp/phase5_baseline_sha256.txt` (41 archivos)
- Verificación final: `/tmp/phase5_final_sha256.txt` (41 archivos)
- Resultado de `diff -u`: **0 diferencias (`SHA256_IDENTICAL=OK`)**.

Únicamente se agregaron suites de auditoría en `app/src/test/java/com/example/`:
- `EdiPhase5ComprehensiveAuditTest.kt`
- `EdiPhase5UiAndPdfAuditTest.kt`

Ningún defecto detectado fue corregido silenciosamente.

---

## 2. Resumen Ejecutivo de los 8 Pilares Auditados

| Pilar | Alcance Verificado | Estado | Observaciones Clave |
|---|---|---|---|
| **1. Catálogo Clínico** | 15 grupos (`1m 0d – 71m 29d`), 270 reactivos únicos (FRB, EN, Áreas, ALE, ALA, CO-R), modalidades y materiales | **Aprobado con observaciones textuales** | 8 observaciones de asteriscos/tildes (`LE 2.1`, `ALE 3.1`, `ALE 4.4`, `ALA 4.2`, `ALE 12.3`, `MF 13.1`, `LE 13.3`, `LE 14.3`). Cotejo literal contra PDF físico MINSA 2026 pendiente de revisión clínica externa. |
| **2. Motor de Retrocesos** | Exhaustivo G1–G15 × áreas aplicables × todas las combinaciones SÍ/NO + incompletos | **Aprobado** | Excepciones oficiales exactas: G8→G7 (2 ítems), G13 CO→CO-R (3 ítems), G1–G12 CO=N/A, G1 sin retroceso, historial ignorado sin contaminación. |
| **3. Semáforo Global** | Oráculo independiente G1, G2–G4, G5–G15 + sensibilidad a 1 respuesta + corrección Fase 3 | **Aprobado** | En G5–G15, FRB/ALE `UNANSWERED`, falta de material o discrepancia generan advertencias no bloqueantes. Alertas críticas preservadas en `INCOMPLETE`. |
| **4. Edad y Prematuridad** | Límites G1–G15, `<1m`, `>=72m`, `34s 0d` (`14/07/2024`→`20/03/2025`), semanas+días sin redondeo, CRED intacto | **Aprobado con observación UX en Tab 1** | Discrepancia `6m 24d` vs `6m 23d` verificada; bloqueo ante grupos distintos validado; `ClinicalAgeCalculator.calculateAges` intacto. Tab 1 no expone selector de días EG (`0–6`). |
| **5. Interfaz (Ventanas 1–4)** | 4 vistas internas EDI + flujo sin regresiones en Ventanas 1–3 | **Aprobado con hallazgos** | `A término (32s 0d)` visible en UI por defecto; falta acción UI/VM para restaurar sesiones en `archivedSessionsByGroup`; asteriscos Markdown crudos en tarjetas. |
| **6. Exportación PDF** | 8 escenarios oficiales + Escenario 9 contraste `ROJO` renderizados a 150 DPI | **Con hallazgo ALTO** | **ALTO-01:** pie de Página 1 exporta `Peso: 5.40 kg (Desnut. Severa)` y `Talla: 61.5 cm (Talla Baja Sev.)` por defecto cuando solo se ingresa PC. Colisión leve en texto vertical `PC` y contraste rojo/rojo mejorable. |
| **7. Pruebas Automatizadas** | `gradle :app:testDebugUnitTest --rerun-tasks` | **71/71 aprobadas** | 35 unitarias puras + 36 integración/UI Robolectric + 0 instrumentadas (sin emulador/ADB) + 22 PNGs inspeccionados. |
| **8. Empaquetado y Evidencia** | Matrices CSV/MD, PDFs, PNGs, XMLs JUnit, hashes SHA-256 y ZIP | **Completo** | Artefactos consolidados en `app/build/audit_phase5/` y paquete ZIP final. |

---

## 3. Hallazgos Clasificados por Severidad (Pendientes de Autorización)

### Severidad ALTA
1. **`[ALTO-01]` Valores antropométricos fantasma exportados en el pie de Página 1 del PDF cuando faltan Peso o Talla**
   - **Ubicación:** `CredEdiState.kt` (`CredEdiUiState` líneas 922–927, `datosAntropometricosCount` líneas 1137–1138) y `EdiPdfExporter.kt` (`drawClinicalFooterLine` líneas 635–646).
   - **Descripción:** `CredEdiUiState` inicializa `pesoKg = 5.40` y `longitudCm = 61.5` con `pesoInput = ""` y `longitudInput = ""`. En `EdiPdfExporter.kt:635`, la condición `if (state.datosAntropometricosCount > 0)` imprime **los tres valores y sus diagnósticos OMS (`evalPE`, `evalTE`)**, incluso si el usuario únicamente ingresó `perimetroCefalicoInput`.
   - **Evidencia visual:** Confirmado en `scenario_03`, `scenario_04`, `scenario_05`, `scenario_06` y `scenario_08` (niños de 14 meses a 5 años 4 meses diagnosticados erróneamente en el pie oficial con `Peso: 5.40 kg (Desnut. Severa) | Talla: 61.5 cm (Talla Baja Sev.)`).

### Severidad MEDIA
2. **`[MEDIO-01]` Edad gestacional por defecto inconsistente entre UI (`32s 0d`) y PDF (`≥37 sem`)**
   - **Ubicación:** `CredEdiState.kt` (919–921), `CredEdiViewModel.kt` (`togglePrematurez`, 146–168), `EdiScreen.kt` (340, 352, 887, 2446), `EdiPdfExporter.kt` (257–268).
   - **Descripción:** `CredEdiUiState` inicia con `esPrematuro = false`, `egSemanas = 32`, `egDias = 0`. La interfaz muestra etiquetas contradictorias como `"A término (32s 0d)"`, `"EG registrada: 32s 0d"` y `"Sexo: Niño · EG: 32s 0d"`; además `togglePrematurez(false)` no restablece `egSemanas >= 37`. El exportador PDF sustituye ese valor por `"≥37 sem"`, creando incoherencia entre pantalla y documento oficial.

3. **`[MEDIO-02]` Sesiones archivadas no tienen flujo de restauración hacia `activeSession`**
   - **Ubicación:** `EdiOfficialCatalog.kt` (`archivedSessionsByGroup`, 367, 537–544), `CredEdiViewModel.kt`, `EdiScreen.kt`.
   - **Descripción:** Aunque `EdiPhase1SessionManager.confirmPendingGroupChange` archiva correctamente la sesión previa al cambiar de grupo con respuestas, no existe método en `CredEdiViewModel` ni control en `EdiScreen` para reabrir y continuar una sesión archivada.

4. **`[MEDIO-03]` Inconsistencias textuales, marcadores Markdown crudos (`*`/`**`) y tildes en el catálogo y su renderizado**
   - **Ubicación:** `EdiOfficialCatalog.kt`, `EdiScreen.kt:1891`, `EdiPdfExporter.kt:1001`.
   - **Descripción:**
     - `EdiScreen.kt:1891` muestra `**` y `*` literales en pantalla; `EdiPdfExporter.kt:1001` solo elimina `**`, dejando un asterisco literal en `*MF 13.1.` en Página 2 (confirmado visualmente en `scenario_04_grupo_13_cor_p2.png`).
     - Observaciones de catálogo detectadas automáticamente en `01_matriz_cotejo_catalogo_edi_2026.md`:
       - `LE 2.1`: `hasObservationAsterisks = true` sin `**` ni `(Observe)` en `formatQuestionText`.
       - `ALE 4.4`: `hasObservationAsterisks = false` pero `formatQuestionText` contiene `**4. ... (Observe)`.
       - `MF 13.1`: `hasObservationAsterisks = true` pero `consultationQuestionText` inicia con `*` simple en lugar de `**`.
       - `LE 14.3`: `hasObservationAsterisks = false` (protocolo entrevista principal) pero `consultationQuestionText` inicia con `**LE 14.3.` y presenta tilde errónea `¿Cuándo`.
       - `ALE 3.1` y `ALA 4.2`: tilde interrogativa impropia `'¿Cuándo'` en oración condicional (`¿Cuándo lo sostiene...`).
       - `ALE 12.3` y `LE 13.3`: divergencias de asteriscos explicables por protocolo interno (dual / entrevista principal), que conviene unificar en presentación visual.

5. **`[MEDIO-04]` Calculadora CRED (Ventana 1) solo permite ajustar semanas EG y no días EG (0–6)**
   - **Ubicación:** `CalculadoraScreen.kt` (651–698).
   - **Descripción:** El slider de Ventana 1 únicamente ajusta `egSemanas` (`24s..36s`), mientras que `egDias` solo es editable desde Ventana 4 (`EdiScreen.kt`).

### Severidad BAJA
6. **`[BAJO-01]` Colisión leve de texto `PC` sobre plantilla y contraste bajo en marcas rojas sobre fondo rojo**
   - **Ubicación:** `EdiPdfExporter.kt` (`EXPLORACIÓN NEUROLÓGICA` y `CLINICAL_RED_INK` sobre casillas rojas).
   - **Evidencia visual:** En `scenario_01..09_p1.png`, el texto vertical de `PC` se superpone ligeramente a la línea preimpresa `"PC: ____ cm"` de `ficha_edi.jpg`. En `scenario_09_grupo_1_rojo_contraste_p1.png` y `zoom_s07_borrador_inicial.png`, las marcas rojas (`#B91C1C`) sobre fondo rojo vivo (`ROJO`, `ALA 4.1`, `EN 1.1`) tienen contraste reducido frente a alternativas con trazo oscuro o contorno blanco.

7. **`[BAJO-02]` Desbordamiento de 1 línea a Página 3 en Borrador G4 y doble paréntesis en pie de Página 1**
   - **Ubicación:** `EdiPdfExporter.kt:655` y paginación de Página 2 (`scenario_07_borrador_alerta_prioritaria_p3.png`).
   - **Descripción:** En el Escenario 7, la Página 3 contiene únicamente 1 línea (`ALA 4.2`). Adicionalmente, el pie de Página 1 concatena código y título produciendo `"G1 (Grupo 1 (de 1 mes a 1 mes 29 días))"`.

8. **`[BAJO-03]` Progreso `ediMandatoryItemsProgress` puede marcar 100 % mientras el semáforo está bloqueado por edad/discrepancia**
   - **Ubicación:** `CredEdiState.kt:1421–1454`.
   - **Descripción:** El contador de ítems obligatorios solo contabiliza reactivos contestados y no refleja un bloqueo activo por discrepancia de edad corregida o rango etario fuera de G1–G15.

---

## 4. Resultados Detallados de Pruebas Automatizadas (JUnit XML Reales)

Ejecución: `gradle :app:testDebugUnitTest --rerun-tasks` → **`BUILD SUCCESSFUL` (71 pruebas, 0 fallos, 0 errores, 0 omitidas)**.

| Archivo XML JUnit | Tipo de Prueba | Tests | Fallos | Errores | Omitidas | Tiempo |
|---|---|---|---|---|---|---|
| `TEST-com.example.ExampleUnitTest.xml` | Unitaria JVM | 1 | 0 | 0 | 0 | 0.000s |
| `TEST-com.example.ClinicalAgeCalculatorTest.xml` | Unitaria JVM | 5 | 0 | 0 | 0 | 0.068s |
| `TEST-com.example.EdiPhase1CatalogTest.xml` | Unitaria JVM | 7 | 0 | 0 | 0 | 0.010s |
| `TEST-com.example.EdiPhase2FallbackTest.xml` | Unitaria JVM | 8 | 0 | 0 | 0 | 0.045s |
| `TEST-com.example.EdiPhase3SemaphoreTest.xml` | Unitaria JVM | 9 | 0 | 0 | 0 | 0.062s |
| `TEST-com.example.EdiPhase5ComprehensiveAuditTest.xml` | Unitaria JVM (Auditoría Fase 5) | 5 | 0 | 0 | 0 | 0.095s |
| `TEST-com.example.ExampleRobolectricTest.xml` | Integración / UI Robolectric | 12 | 0 | 0 | 0 | 0.984s |
| `TEST-com.example.EdiPhase4AIntegrationTest.xml` | Integración / Compose UI Robolectric | 12 | 0 | 0 | 0 | 13.091s |
| `TEST-com.example.EdiPhase4BPdfExporterTest.xml` | Exportación PDF Robolectric | 9 | 0 | 0 | 0 | 2.158s |
| `TEST-com.example.EdiPhase5UiAndPdfAuditTest.xml` | UI Ventanas 1–4 + PDFs Fase 5 | 3 | 0 | 0 | 0 | 17.831s |
| **TOTAL** | **35 Unitarias + 36 Robolectric** | **71** | **0** | **0** | **0** | **34.344s** |

- **Pruebas instrumentadas en emulador Android / ADB (`androidTest`):** **0 ejecutadas** (no existe emulador Android ni ADB en este entorno).
- **Inspección visual de PDFs renderizados:** **9 PDFs** (8 escenarios solicitados + Escenario 9 contraste `ROJO`) renderizados con Ghostscript a 150 DPI en **22 imágenes PNG** (incluyendo recortes zoom de sección `Inicial`) e inspeccionados visualmente.

---

## 5. Límite Honesto de Validación Externa

Dentro del espacio de trabajo sí se verificaron íntegramente el código fuente, `implementation_plan.md` y la plantilla visual `ficha_edi.jpg`. El archivo PDF original completo del *Manual para la Aplicación de la Prueba EDI — Versión Peruana 2026 (MINSA/UNICEF)* no se encuentra presente en el contenedor; por ello, el cotejo tipográfico palabra por palabra de los 270 enunciados frente al documento editorial impreso queda explícitamente documentado como **pendiente de revisión clínica externa**.
