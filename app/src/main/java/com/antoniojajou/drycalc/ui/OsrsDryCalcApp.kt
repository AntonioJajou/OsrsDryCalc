package com.antoniojajou.drycalc.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antoniojajou.drycalc.model.*
import com.antoniojajou.drycalc.rates.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.antoniojajou.drycalc.viewmodel.DryCalcViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL
import java.util.Locale

@Composable
fun OsrsDryCalcApp(viewModel: DryCalcViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    var returnScrollIndex by rememberSaveable { mutableIntStateOf(0) }
    var returnScrollOffset by rememberSaveable { mutableIntStateOf(0) }
    var restoreScrollPosition by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(uiState.selectedItem, restoreScrollPosition) {
        if (uiState.selectedItem == null && restoreScrollPosition) {
            listState.scrollToItem(returnScrollIndex, returnScrollOffset)
            restoreScrollPosition = false
        }
    }
    MaterialTheme(colorScheme = lightColorScheme(primary = Wood, secondary = Gold, background = Parchment, surface = Parchment)) {
        LazyColumn(Modifier.fillMaxSize().background(Parchment).padding(horizontal = 20.dp), state = listState, contentPadding = PaddingValues(vertical = 20.dp)) {
            item {
                Text("How Dry Am I?", color = GoldDark, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(uiState.username, viewModel::updateUsername, label = { Text("Enter OSRS Username") }, singleLine = true, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Ink, unfocusedTextColor = Ink, focusedBorderColor = GoldDark, focusedLabelColor = GoldDark))
                Spacer(Modifier.height(12.dp))
            }
            when {
                uiState.selectedItem != null -> item { ItemDetailsScreen(uiState.selectedItem!!, uiState.report?.kills.orEmpty(), uiState.report?.coxPoints ?: CoxPointAverages(), viewModel::hideItem) }
                uiState.selectedTab == null -> item {
                    Button(onClick = { viewModel.loadTab("Bosses") }, enabled = !uiState.isLoading, colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Ink), modifier = Modifier.fillMaxWidth()) { Text("Boss log") }
                    Spacer(Modifier.height(10.dp))
                    Button(onClick = { viewModel.loadTab("Raids") }, enabled = !uiState.isLoading, colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Ink), modifier = Modifier.fillMaxWidth()) { Text("Raids log") }
                    Spacer(Modifier.height(18.dp)); Text(uiState.status, color = Ink, fontSize = 15.sp)
                }
                else -> {
                    item {
                        OutlinedButton(viewModel::returnToLogSelection, Modifier.fillMaxWidth()) { Text("Back to log selection") }
                        if (uiState.isLoading) { Spacer(Modifier.height(12.dp)); LinearProgressIndicator(Modifier.fillMaxWidth(), color = GoldDark) }
                        Spacer(Modifier.height(18.dp)); Text(uiState.status, color = Ink, fontSize = 15.sp)
                    }
                    uiState.report?.bosses?.forEach { boss -> item(key = boss.name) {
                        BossCard(
                            boss = boss,
                            kills = uiState.report!!.kills,
                            coxPoints = uiState.report!!.coxPoints,
                            regularCoxPoints = uiState.regularCoxPoints,
                            challengeCoxPoints = uiState.challengeCoxPoints,
                            onRegularPointsChanged = viewModel::updateRegularCoxPoints,
                            onChallengePointsChanged = viewModel::updateChallengeCoxPoints,
                            onApplyCoxPoints = viewModel::applyCoxPoints
                        ) { detail ->
                            returnScrollIndex = listState.firstVisibleItemIndex
                            returnScrollOffset = listState.firstVisibleItemScrollOffset
                            restoreScrollPosition = true
                            viewModel.showItem(detail)
                        }
                    } }
                }
            }
            item { Spacer(Modifier.height(20.dp)) }
        }
    }
}

@Composable private fun BossCard(
    boss: BossLog,
    kills: Map<String, Int>,
    coxPoints: CoxPointAverages,
    regularCoxPoints: String,
    challengeCoxPoints: String,
    onRegularPointsChanged: (String) -> Unit,
    onChallengePointsChanged: (String) -> Unit,
    onApplyCoxPoints: () -> Unit,
    onItemClick: (ItemDetail) -> Unit
) {
    var expanded by rememberSaveable(boss.name) { mutableStateOf(false) }
    if (boss.items.isEmpty()) return
    val cardColor = if (boss.items.all { it.quantity > 0 }) CompletedParchment else Parchment
    Spacer(Modifier.height(10.dp))
    Card(colors = CardDefaults.cardColors(containerColor = cardColor), border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(GoldDark)), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth().clickable { expanded = !expanded }, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("${boss.name} — ${collectionKills(boss.name, kills)}", color = GoldDark, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                    if (boss.name == "Chambers of Xeric") Text("Average points: Regular ${formatPoints(coxPoints.regular)} • Challenge ${formatPoints(coxPoints.challenge)}", color = Ink, fontSize = 13.sp)
                    bossSummary(boss, kills, coxPoints)?.let { Text(it, color = Ink, fontSize = 14.sp) }
                }
                Text(if (expanded) "⌃" else "⌄", color = GoldDark, fontSize = 30.sp)
            }
            if (expanded) {
                if (boss.name == "Chambers of Xeric") {
                    Spacer(Modifier.height(10.dp))
                    Text("Set your average points per completion", color = Ink, fontSize = 14.sp)
                    Spacer(Modifier.height(6.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(regularCoxPoints, onRegularPointsChanged, label = { Text("Regular") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Ink, unfocusedTextColor = Ink, focusedBorderColor = GoldDark, focusedLabelColor = GoldDark))
                        OutlinedTextField(challengeCoxPoints, onChallengePointsChanged, label = { Text("Challenge") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Ink, unfocusedTextColor = Ink, focusedBorderColor = GoldDark, focusedLabelColor = GoldDark))
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onApplyCoxPoints, Modifier.fillMaxWidth()) { Text("Apply Chambers averages") }
                }
                boss.items.forEach { item -> ItemRow(boss.name, item, kills, coxPoints) { onItemClick(ItemDetail(boss.name, it)) } }
            }
        }
    }
}

@Composable private fun ItemRow(boss: String, item: LogItem, kills: Map<String, Int>, coxPoints: CoxPointAverages, onItemClick: (LogItem) -> Unit) {
    val bitmap by produceState<android.graphics.Bitmap?>(null, item.id) { value = withContext(Dispatchers.IO) { runCatching { BitmapFactory.decodeStream(URL("https://cdn.runeprofile.com/item/${item.id}.png").openStream()) }.getOrNull() } }
    val obtained = item.quantity > 0
    val showDropRate = !(boss == "Chambers of Xeric" && coxItemWeight(item.name) != null)
    Row(Modifier.fillMaxWidth().heightIn(min = 72.dp).clickable { onItemClick(item) }.padding(vertical = 5.dp).alpha(if (obtained) 1f else 0.42f), verticalAlignment = Alignment.CenterVertically) {
        bitmap?.let { Image(it.asImageBitmap(), item.name, Modifier.size(55.dp)) }
        Spacer(Modifier.width(12.dp)); Column {
            Text(if (obtained) "${item.name} ×${item.quantity}" else item.name, color = Ink, fontSize = 16.sp)
            if (showDropRate) {
                val rateLabel = dropRateLabel(boss, item.name)
                Text(if (isExcludedFromRateCalculation(item.name)) rateLabel else "Expected drop rate: $rateLabel", color = Ink, fontSize = 14.sp)
            }
            val rate = if (obtained) rateDescription(boss, item.name, item.quantity, kills, coxPoints) else uncollectedRateDescription(boss, item.name, kills, coxPoints)
            rate?.let { Text("Rate: $it", color = Ink, fontSize = 14.sp) }
        }
        Spacer(Modifier.weight(1f)); Text("›", color = GoldDark, fontSize = 28.sp)
    }
}

@Composable private fun ItemDetailsScreen(detail: ItemDetail, kills: Map<String, Int>, coxPoints: CoxPointAverages, onBack: () -> Unit) {
    val item = detail.item
    val bitmap by produceState<android.graphics.Bitmap?>(null, item.id) { value = withContext(Dispatchers.IO) { runCatching { BitmapFactory.decodeStream(URL("https://cdn.runeprofile.com/item/${item.id}.png").openStream()) }.getOrNull() } }
    val obtained = item.quantity > 0
    val calculatedRate = if (obtained) rateDescription(detail.boss, item.name, item.quantity, kills, coxPoints) else uncollectedRateDescription(detail.boss, item.name, kills, coxPoints)
    Column(Modifier.fillMaxWidth()) {
        OutlinedButton(onBack, Modifier.fillMaxWidth()) { Text("Back to ${detail.boss}") }
        Spacer(Modifier.height(20.dp)); Text("Item details", color = GoldDark, fontSize = 28.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(12.dp))
        Card(colors = CardDefaults.cardColors(containerColor = Parchment), border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(GoldDark)), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    bitmap?.let { Image(it.asImageBitmap(), item.name, Modifier.size(80.dp)) }
                    Spacer(Modifier.width(16.dp)); Column {
                        Text(item.name, color = Ink, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Text("${if (obtained) "Collected" else "Not collected"}${if (obtained) " ×${item.quantity}" else ""}", color = Ink, fontSize = 16.sp)
                    }
                }
                Spacer(Modifier.height(18.dp)); Text("Collection log: ${detail.boss}", color = Ink, fontSize = 16.sp)
                Text("Kill count: ${collectionKills(detail.boss, kills)}", color = Ink, fontSize = 16.sp)
                if (detail.boss == "Chambers of Xeric") Text("Average points: ${formatPoints(coxPoints.regular)} regular • ${formatPoints(coxPoints.challenge)} Challenge", color = Ink, fontSize = 16.sp)
                Spacer(Modifier.height(12.dp))
                val dropRate = itemDetailsDropRate(detail.boss, item.name)
                Text(if (isExcludedFromRateCalculation(item.name)) dropRate else "Expected drop rate: $dropRate", color = Ink, fontSize = 16.sp)
                Text("Your rate: ${calculatedRate ?: "Not mapped yet"}", color = Ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

private fun itemDetailsDropRate(boss: String, item: String): String {
    if (boss == "Chambers of Xeric") coxItemWeight(item)?.let { return "Normal ${it.normal}/60 unique table • Challenge ${it.challenge}/56 unique table" }
    return dropRateLabel(boss, item)
}

private fun formatPoints(points: Double) = String.format(Locale.US, "%,.0f", points)

@Preview(showBackground = true, backgroundColor = 0xFFF2E2BB)
@Composable private fun OsrsDryCalcPreview() = OsrsDryCalcApp(DryCalcViewModel())
