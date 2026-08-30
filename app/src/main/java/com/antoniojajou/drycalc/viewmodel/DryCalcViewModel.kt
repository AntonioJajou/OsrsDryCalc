package com.antoniojajou.drycalc.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.antoniojajou.drycalc.data.loadReport
import com.antoniojajou.drycalc.model.ItemDetail
import com.antoniojajou.drycalc.model.CoxPointAverages
import com.antoniojajou.drycalc.model.Report
import com.antoniojajou.drycalc.rates.accountSummary
import com.antoniojajou.drycalc.rates.raidsSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale

data class DryCalcUiState(
    val username: String = "",
    val regularCoxPoints: String = "49750",
    val challengeCoxPoints: String = "66400",
    val report: Report? = null,
    val status: String = "Tap Load boss rates to fetch your data.",
    val isLoading: Boolean = false,
    val selectedTab: String? = null,
    val selectedItem: ItemDetail? = null
)

class DryCalcViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(DryCalcUiState())
    val uiState: StateFlow<DryCalcUiState> = _uiState.asStateFlow()

    fun updateUsername(username: String) = _uiState.update { it.copy(username = username) }
    fun updateRegularCoxPoints(points: String) = _uiState.update { it.copy(regularCoxPoints = points) }
    fun updateChallengeCoxPoints(points: String) = _uiState.update { it.copy(challengeCoxPoints = points) }

    fun applyCoxPoints() {
        val report = _uiState.value.report ?: return
        val points = CoxPointAverages(
            regular = _uiState.value.regularCoxPoints.toDoubleOrNull() ?: 0.0,
            challenge = _uiState.value.challengeCoxPoints.toDoubleOrNull() ?: 0.0
        )
        if (points.regular <= 0 || points.challenge <= 0) {
            _uiState.update { it.copy(status = "Enter positive average Chambers points for regular and Challenge Mode.") }
            return
        }
        val summary = if (report.tabName == "Bosses") accountSummary(report.bosses, report.kills, points) else raidsSummary(report.bosses, report.kills, points)
        _uiState.update { it.copy(report = report.copy(coxPoints = points, accountRate = summary)) }
    }

    fun loadTab(tabName: String) {
        val username = _uiState.value.username.trim()
        if (username.isEmpty()) {
            _uiState.update { it.copy(status = "Enter a RuneScape username first.") }
            return
        }
        val coxPoints = CoxPointAverages(
            regular = _uiState.value.regularCoxPoints.toDoubleOrNull() ?: 0.0,
            challenge = _uiState.value.challengeCoxPoints.toDoubleOrNull() ?: 0.0
        )
        if (coxPoints.regular <= 0 || coxPoints.challenge <= 0) {
            _uiState.update { it.copy(status = "Enter positive average Chambers points for regular and Challenge Mode.") }
            return
        }

        _uiState.update {
            it.copy(
                selectedTab = tabName,
                report = null,
                isLoading = true,
                status = "Loading ${tabName.lowercase()} kill counts and collection-log drops…"
            )
        }
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { loadReport(username, tabName, coxPoints) }
                .onSuccess { report ->
                    _uiState.update {
                        it.copy(
                            report = report,
                            isLoading = false,
                            status = "${report.username} — ${report.tabName} log: ${report.obtained}/${report.total} unlocked\n" +
                                "Completion: ${"%.1f".format(Locale.US, report.obtained * 100.0 / report.total)}%\n${report.accountRate}"
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            status = "Could not load RuneProfile data. Check your connection and try again.\n\n${error.message}"
                        )
                    }
                }
        }
    }

    fun showItem(detail: ItemDetail) = _uiState.update { it.copy(selectedItem = detail) }

    fun hideItem() = _uiState.update { it.copy(selectedItem = null) }

    fun returnToLogSelection() = _uiState.update {
        it.copy(selectedTab = null, report = null, selectedItem = null, status = "Choose Boss log or Raids log.")
    }
}
