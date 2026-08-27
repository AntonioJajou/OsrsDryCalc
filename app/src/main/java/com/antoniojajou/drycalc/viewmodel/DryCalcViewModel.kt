package com.antoniojajou.drycalc.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.antoniojajou.drycalc.data.loadReport
import com.antoniojajou.drycalc.model.ItemDetail
import com.antoniojajou.drycalc.model.Report
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale

data class DryCalcUiState(
    val username: String = "",
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

    fun loadTab(tabName: String) {
        val username = _uiState.value.username.trim()
        if (username.isEmpty()) {
            _uiState.update { it.copy(status = "Enter a RuneScape username first.") }
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
            runCatching { loadReport(username, tabName) }
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
