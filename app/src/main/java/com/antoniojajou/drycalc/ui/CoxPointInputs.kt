package com.antoniojajou.drycalc.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antoniojajou.drycalc.viewmodel.DryCalcUiState
import com.antoniojajou.drycalc.viewmodel.DryCalcViewModel

@Composable
fun CoxPointInputs(state: DryCalcUiState, viewModel: DryCalcViewModel) {
    Spacer(Modifier.height(10.dp))
    Text("Set your average points per completion", color = Ink, fontSize = 14.sp)
    Spacer(Modifier.height(6.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        CoxPointField(state.regularCoxPoints, viewModel::updateRegularCoxPoints, "Regular", Modifier.weight(1f))
        CoxPointField(state.challengeCoxPoints, viewModel::updateChallengeCoxPoints, "Challenge", Modifier.weight(1f))
    }
    Spacer(Modifier.height(8.dp))
    OutlinedButton(viewModel::applyCoxPoints, Modifier.fillMaxWidth()) { Text("Apply Chambers averages") }
}

@Composable
private fun CoxPointField(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier) {
    OutlinedTextField(value, onValueChange, label = { Text(label) }, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = modifier,
        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Ink, unfocusedTextColor = Ink, focusedBorderColor = GoldDark, focusedLabelColor = GoldDark))
}
