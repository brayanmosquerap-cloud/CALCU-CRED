# Matriz de Hallazgos — Auditoría Integral Fase 5 (CRED & EDI Antropometría 1.0)

**Fecha:** 2026-11-12  
**Fuente:** `INFORME_AUDITORIA_FASE5.md` (artefacto persistido de la Auditoría Fase 5; 0 cambios al código de producción).  
**Política de datos:** Todas las evaluaciones y escenarios usan únicamente datos sintéticos.

---

## 1. Matriz Resumen de Hallazgos (8 Defectos Detectados)

| ID | Severidad | Área / Componente | Archivos y Líneas | Descripción del Hallazgo | Estado |
|---|---|---|---|---|---|
| `ALTO-01` | **ALTA** | Exportación PDF / Antropometría por defecto | `CredEdiState.kt` (922–927, 1137–1138); `EdiPdfExporter.kt` (635–646) | `CredEdiUiState` inicializa `pesoKg = 5.40` y `longitudCm = 61.5` aunque `pesoInput = ""` y `longitudInput = ""`. Si solo se ingresa `perimetroCefalicoInput`, `datosAntropometricosCount > 0` hace que el pie de Página 1 exporte diagnósticos fantasma (`Peso: 5.40 kg (Desnut. Severa) \| Talla: 61.5 cm (Talla Baja Sev.)`). | Pendiente de autorización para corrección |
| `MEDIO-01` | **MEDIA** | Edad Gestacional (UI vs PDF) | `CredEdiState.kt` (919–921); `CredEdiViewModel.kt` (146–168); `EdiScreen.kt` (340, 352, 887, 2446); `EdiPdfExporter.kt` (257–268) | Estado inicial con `esPrematuro = false`, `egSemanas = 32`, `egDias = 0`. La UI muestra `"A término (32s 0d)"`, `"EG registrada: 32s 0d"` y `"Sexo: Niño · EG: 32s 0d"`; `togglePrematurez(false)` no restablece `egSemanas >= 37`, mientras el PDF sustituye el valor por `"≥37 sem"`. | Pendiente de autorización para corrección |
| `MEDIO-02` | **MEDIA** | Gestión de sesiones EDI / Restauración | `EdiOfficialCatalog.kt` (367, 537–544); `CredEdiViewModel.kt`; `EdiScreen.kt` | `archivedSessionsByGroup` guarda el snapshot al cambiar de grupo con respuestas, pero no existe método en `CredEdiViewModel` ni control en `EdiScreen` para reabrir y continuar una sesión archivada. | Pendiente de autorización para corrección |
| `MEDIO-03` | **MEDIA** | Catálogo oficial y renderizado Markdown/tildes | `EdiOfficialCatalog.kt`; `EdiScreen.kt:1891`; `EdiPdfExporter.kt:1001` | UI muestra `**` y `*` crudos; el PDF solo elimina `**` y deja `*MF 13.1.` en Página 2. Observaciones de metadatos/texto en 8 reactivos: `LE 2.1`, `ALE 4.4`, `MF 13.1`, `LE 14.3` (además de `¿Cuándo`), `ALE 3.1` (`¿Cuándo`), `ALA 4.2` (`¿Cuándo`), y alineación visual de `ALE 12.3` / `LE 13.3`. | Pendiente de autorización para corrección |
| `MEDIO-04` | **MEDIA** | Ventana 1 (Calculadora CRED) | `CalculadoraScreen.kt` (651–698) | El control deslizante de prematuridad en Ventana 1 solo ajusta semanas enteras (`24s..36s`) y no expone días EG (`0–6`), los cuales solo pueden editarse en Ventana 4 (`EdiScreen.kt`). | Pendiente de autorización para corrección |
| `BAJO-01` | **BAJA** | Maquetación PDF Página 1 (`ficha_edi.jpg`) | `EdiPdfExporter.kt` (`EXPLORACIÓN NEUROLÓGICA`, `CLINICAL_RED_INK` `#B91C1C`) | El texto vertical de `PC` colisiona levemente con `"PC: ____ cm"` de la plantilla; las marcas rojas `#B91C1C` sobre casillas rojas (`ROJO`, `ALA 4.1`, `EN 1.1`) presentan bajo contraste. | Pendiente de autorización para corrección |
| `BAJO-02` | **BAJA** | Paginación Anexo PDF y pie de Página 1 | `EdiPdfExporter.kt:655` y paginación Página 2 | En Borrador G4 (Escenario 7), 1 sola línea (`ALA 4.2`) desborda a Página 3. Además, el pie de Página 1 concatena código y título produciendo doble paréntesis: `"G1 (Grupo 1 (de 1 mes a 1 mes 29 días))"`. | Pendiente de autorización para corrección |
| `BAJO-03` | **BAJA** | Indicador de progreso obligatorio EDI | `CredEdiState.kt` (1421–1454) | `ediMandatoryItemsProgress` puede mostrar `100 %` (`total/total`) aun cuando el semáforo global permanece en `INCOMPLETE` por bloqueo de discrepancia de edad corregida o rango etario fuera de G1–G15. | Pendiente de autorización para corrección |

---

## 2. Estado de los 8 Pilares de la Auditoría Fase 5

| Pilar | Resultado | Notas |
|---|---|---|
| 1. Catálogo Clínico (15 grupos, 270 reactivos) | Aprobado con observaciones (`MEDIO-03`) | Cotejo literal contra el PDF impreso MINSA/UNICEF 2026 pendiente de revisión clínica externa (manual no presente en el contenedor). |
| 2. Motor de Retrocesos | Aprobado | G1 sin retroceso, G2–G7, G8→G7 (2 ítems), G9–G12, G13 CO→CO-R, G14–G15, aislamiento de historial verificados. |
| 3. Semáforo Global | Aprobado | G1, G2–G4 y G5–G15 (FRB/ALE opcionales no bloqueantes en G5–G15; alertas críticas preservadas). |
| 4. Edad Corregida y Prematuridad | Aprobado con observación UX (`MEDIO-04`) | `ClinicalAgeCalculator.calculateAges` intacto; discrepancia `6m 24d` vs `6m 23d` (14/07/2024→20/03/2025, 34s) y bloqueo automático verificados. |
| 5. Interfaz (Ventanas 1–4) | Aprobado con hallazgos (`MEDIO-01`, `MEDIO-02`, `MEDIO-03`) | Flujo completo sin regresiones en Ventanas 1–3 y 4 vistas internas en Ventana 4. |
| 6. Exportación PDF (`ficha_edi.jpg` + anexo) | Con hallazgo `ALTO-01` y `BAJO-01`/`BAJO-02` | Verificado en 9 escenarios sintéticos renderizados a 150 DPI. |
| 7. Pruebas Automatizadas (`testDebugUnitTest`) | 71/71 aprobadas (35 JVM + 36 Robolectric) | 0 pruebas instrumentadas (`androidTest`: sin emulador/ADB en el entorno). |
| 8. Empaquetado y Evidencia | Requiere regeneración previa para nuevo ZIP ligero | Tras el reinicio/sincronización del contenedor entre sesiones, `app/build/audit_phase5/` quedó vacío; regenerar los 9 PDFs, 10 XMLs y la matriz de 270 reactivos requiere autorización explícita para ejecutar nuevamente las pruebas. |
