package com.antoniojajou.drycalc.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.antoniojajou.drycalc.data.loadReport
import com.antoniojajou.drycalc.model.ItemDetail
import com.antoniojajou.drycalc.model.CoxPointAverages
import com.antoniojajou.drycalc.model.Report
import com.antoniojajou.drycalc.model.ToaAverages
import com.antoniojajou.drycalc.rates.raidsSummary
import com.antoniojajou.drycalc.rates.accountSummary
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
    val normalToaPoints: String = "",
    val normalToaLevel: String = "",
    val expertToaPoints: String = "",
    val expertToaLevel: String = "",
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
    fun updateNormalToaPoints(value: String) = _uiState.update { it.copy(normalToaPoints = value) }
    fun updateNormalToaLevel(value: String) = _uiState.update { it.copy(normalToaLevel = value) }
    fun updateExpertToaPoints(value: String) = _uiState.update { it.copy(expertToaPoints = value) }
    fun updateExpertToaLevel(value: String) = _uiState.update { it.copy(expertToaLevel = value) }

    fun loadTab(tabName: String) {
        val username = _uiState.value.username.trim()
        if (username.isEmpty()) {
            _uiState.update { it.copy(status = "Enter a RuneScape username first.") }
            return
        }
        if (tabName == "Raids" && expertLevelIsBelowMinimum()) {
            _uiState.update { it.copy(status = "Expert Tombs raid level must be at least 300.") }
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
            runCatching { loadReport(username, tabName, coxPointsFromState(), toaAveragesFromState()) }
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

    fun applyCoxPoints() {
        val report = _uiState.value.report ?: return
        val points = coxPointsFromState()
        if (points.regular <= 0 || points.challenge <= 0) {
            _uiState.update { it.copy(status = "Enter positive average Chambers points for regular and Challenge Mode.") }
            return
        }
        val summary = if (report.tabName == "Bosses") accountSummary(report.bosses, report.kills, points) else raidsSummary(report.bosses, report.kills, points, report.toaAverages)
        _uiState.update { it.copy(report = report.copy(coxPoints = points, accountRate = summary)) }
    }

    fun applyToaAverages() {
        val report = _uiState.value.report ?: return
        val averages = toaAveragesFromState()
        val needsNormal = (report.kills["Tombs of Amascut"] ?: 0) > 0
        val needsExpert = (report.kills["Tombs of Amascut: Expert Mode"] ?: 0) > 0
        val normalInvalid = needsNormal && (averages.normalPoints <= 0 || averages.normalLevel <= 0)
        val expertInvalid = needsExpert && (averages.expertPoints <= 0 || averages.expertLevel <= 0)
        if (normalInvalid || expertInvalid) {
            _uiState.update { it.copy(status = "Enter positive Tombs points and raid levels for every mode with completed raids.") }
            return
        }
        if (expertLevelIsBelowMinimum()) {
            _uiState.update { it.copy(status = "Expert Tombs raid level must be at least 300.") }
            return
        }
        _uiState.update { it.copy(report = report.copy(toaAverages = averages, accountRate = raidsSummary(report.bosses, report.kills, report.coxPoints, averages))) }
    }

    private fun toaAveragesFromState() = ToaAverages(
        normalPoints = _uiState.value.normalToaPoints.toDoubleOrNull() ?: 0.0,
        normalLevel = _uiState.value.normalToaLevel.toDoubleOrNull() ?: 0.0,
        expertPoints = _uiState.value.expertToaPoints.toDoubleOrNull() ?: 0.0,
        expertLevel = _uiState.value.expertToaLevel.toDoubleOrNull() ?: 0.0
    )

    private fun coxPointsFromState() = CoxPointAverages(
        regular = _uiState.value.regularCoxPoints.toDoubleOrNull() ?: 0.0,
        challenge = _uiState.value.challengeCoxPoints.toDoubleOrNull() ?: 0.0
    )

    private fun expertLevelIsBelowMinimum(): Boolean {
        val value = _uiState.value.expertToaLevel
        return value.isNotBlank() && (value.toDoubleOrNull() ?: 0.0) < 300.0
    }

    fun returnToLogSelection() = _uiState.update {
        it.copy(selectedTab = null, report = null, selectedItem = null, status = "Choose Boss log or Raids log.")
    }
}
