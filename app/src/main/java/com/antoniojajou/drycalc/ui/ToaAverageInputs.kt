package com.antoniojajou.drycalc.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antoniojajou.drycalc.viewmodel.DryCalcUiState
import com.antoniojajou.drycalc.viewmodel.DryCalcViewModel

@Composable
fun ToaAverageInputs(state: DryCalcUiState, viewModel: DryCalcViewModel) {
    Spacer(Modifier.height(10.dp))
    Text("Average personal Tombs points and raid level", color = Ink, fontSize = 14.sp)
    Spacer(Modifier.height(6.dp))
    Text("Normal / Entry", color = GoldDark, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        ToaAverageField(state.normalToaPoints, viewModel::updateNormalToaPoints, "Points", Modifier.weight(1f))
        ToaAverageField(state.normalToaLevel, viewModel::updateNormalToaLevel, "Raid level", Modifier.weight(1f))
    }
    Spacer(Modifier.height(6.dp))
    Text("Expert", color = GoldDark, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        ToaAverageField(state.expertToaPoints, viewModel::updateExpertToaPoints, "Points", Modifier.weight(1f))
        ToaAverageField(state.expertToaLevel, viewModel::updateExpertToaLevel, "Raid level", Modifier.weight(1f))
    }
    Spacer(Modifier.height(8.dp))
    OutlinedButton(viewModel::applyToaAverages, Modifier.fillMaxWidth()) { Text("Apply Tombs averages") }
}

@Composable
private fun ToaAverageField(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier) {
    OutlinedTextField(value, onValueChange, label = { Text(label) }, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = modifier,
        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Ink, unfocusedTextColor = Ink, focusedBorderColor = GoldDark, focusedLabelColor = GoldDark))
}
