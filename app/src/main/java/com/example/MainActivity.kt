package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.AppTab
import com.example.model.EdiInternalView
import com.example.ui.components.CredEdiBottomBar
import com.example.ui.screens.CalculadoraScreen
import com.example.ui.screens.CurvasOmsScreen
import com.example.ui.screens.DiagnosticoScreen
import com.example.ui.screens.EdiScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.CredEdiViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                Color.White.toArgb(),
                Color.White.toArgb()
            ),
            navigationBarStyle = SystemBarStyle.light(
                Color.White.toArgb(),
                Color.White.toArgb()
            )
        )
        setContent {
            CredEdiApp()
        }
    }
}

@Composable
fun CredEdiApp(
    viewModel: CredEdiViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    MyApplicationTheme(sexo = state.sexo) {
        if (state.currentTab != AppTab.CALCULADORA && !state.showDatosEdiModal) {
            BackHandler {
                if (state.currentTab == AppTab.EDI && state.ediInternalView != EdiInternalView.SUMMARY_5_AXES) {
                    viewModel.navigateEdiBack()
                } else {
                    val previousTab = when (state.currentTab) {
                        AppTab.EDI -> AppTab.DIAGNOSTICO
                        AppTab.DIAGNOSTICO -> AppTab.CURVAS_OMS
                        AppTab.CURVAS_OMS -> AppTab.CALCULADORA
                        AppTab.CALCULADORA -> AppTab.CALCULADORA
                    }
                    viewModel.selectTab(previousTab)
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                when (state.currentTab) {
                    AppTab.CALCULADORA -> CalculadoraScreen(
                        state = state,
                        onSelectSexo = viewModel::selectSexo,
                        onFechaNacimientoChange = viewModel::updateFechaNacimiento,
                        onFechaEvaluacionChange = viewModel::updateFechaEvaluacion,
                        onTogglePrematurez = viewModel::togglePrematurez,
                        onUpdateEgSemanas = viewModel::updateEgSemanas,
                        onAdjustPeso = viewModel::adjustPeso,
                        onAdjustLongitud = viewModel::adjustLongitud,
                        onAdjustPerimetroCefalico = viewModel::adjustPerimetroCefalico,
                        onSetPeso = viewModel::setPesoKg,
                        onSetLongitud = viewModel::setLongitudCm,
                        onSetPerimetroCefalico = viewModel::setPerimetroCefalicoCm,
                        onPesoInputChange = viewModel::updatePesoInput,
                        onLongitudInputChange = viewModel::updateLongitudInput,
                        onPerimetroCefalicoInputChange = viewModel::updatePerimetroCefalicoInput,
                        onContinueToCurvas = { viewModel.selectTab(AppTab.CURVAS_OMS) }
                    )

                    AppTab.CURVAS_OMS -> CurvasOmsScreen(
                        state = state,
                        onSelectIndicador = viewModel::selectCurvaIndicador,
                        onContinueToDiagnostico = { viewModel.selectTab(AppTab.DIAGNOSTICO) }
                    )

                    AppTab.DIAGNOSTICO -> DiagnosticoScreen(
                        state = state,
                        onContinueToEdi = { viewModel.selectTab(AppTab.EDI) },
                        onBack = { viewModel.selectTab(AppTab.CURVAS_OMS) }
                    )

                    AppTab.EDI -> EdiScreen(
                        state = state,
                        onNavigateView = viewModel::navigateEdiView,
                        onNavigateBack = { viewModel.navigateEdiBack() },
                        onOpenAxisQuestionnaire = viewModel::openEdiAxisQuestionnaire,
                        onSelectAxis = viewModel::selectEdiAxis,
                        onSelectDevAreaFilter = viewModel::selectEdiDevAreaFilter,
                        onSearchQueryChange = viewModel::updateEdiGroupSearchQuery,
                        onSelectGroupFilter = viewModel::selectEdiGroupFilter,
                        onSelectGroupFromBank = viewModel::selectEdiGroupFromBank,
                        onUseAgeSuggestedGroup = viewModel::useAgeSuggestedEdiGroup,
                        onResolvePendingGroupChange = viewModel::resolveEdiPendingGroupChange,
                        onConfirmAgeDiscrepancyResolution = viewModel::confirmEdiAgeDiscrepancyResolution,
                        onTogglePrematurez = viewModel::togglePrematurez,
                        onAdjustEgSemanas = viewModel::adjustEdiEgSemanas,
                        onAdjustEgDias = viewModel::adjustEgDias,
                        onUpdateEdiFechaEval = viewModel::updateEdiFechaEval,
                        onAdjustEdiFechaEvalYear = viewModel::adjustEdiFechaEvalYear,
                        onRecordCurrentItemResponse = viewModel::recordEdiCurrentItemResponse,
                        onRecordDualEvidence = viewModel::recordEdiDualEvidence,
                        onRecordFallbackResponse = viewModel::recordEdiFallbackResponse,
                        onRecordFallbackDualEvidence = viewModel::recordEdiFallbackDualEvidence,
                        onUpdateItemClinicalNote = viewModel::updateEdiItemClinicalNote
                    )
                }
            }

            CredEdiBottomBar(
                currentTab = state.currentTab,
                onSelectTab = viewModel::selectTab
            )
        }
    }
}
