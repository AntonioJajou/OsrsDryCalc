package com.example.drycalc.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.drycalc.data.loadReport
import com.example.drycalc.model.*
import com.example.drycalc.rates.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL
import java.util.Locale

@Composable
fun OsrsDryCalcApp() {
    var username by rememberSaveable { mutableStateOf("") }
    var report by remember { mutableStateOf<Report?>(null) }
    var status by remember { mutableStateOf("Tap Load boss rates to fetch your data.") }
    var loading by remember { mutableStateOf(false) }
    var selectedTab by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedItem by remember { mutableStateOf<ItemDetail?>(null) }
    val scope = rememberCoroutineScope()
    val loadTab: (String) -> Unit = { tabName ->
        if (username.trim().isEmpty()) status = "Enter a RuneScape username first."
        else {
            selectedTab = tabName; loading = true; report = null
            status = "Loading ${tabName.lowercase()} kill counts and collection-log drops…"
            scope.launch {
                runCatching { withContext(Dispatchers.IO) { loadReport(username.trim(), tabName) } }
                    .onSuccess { loaded ->
                        report = loaded
                        status = "${loaded.username} — ${loaded.tabName} log: ${loaded.obtained}/${loaded.total} unlocked\nCompletion: ${"%.1f".format(Locale.US, loaded.obtained * 100.0 / loaded.total)}%\n${loaded.accountRate}"
                    }
                    .onFailure { status = "Could not load RuneProfile data. Check your connection and try again.\n\n${it.message}" }
                loading = false
            }
        }
    }
    MaterialTheme(colorScheme = lightColorScheme(primary = Wood, secondary = Gold, background = Parchment, surface = Parchment)) {
        LazyColumn(Modifier.fillMaxSize().background(Parchment).padding(horizontal = 20.dp), contentPadding = PaddingValues(vertical = 20.dp)) {
            item {
                Text("How Dry Am I?", color = GoldDark, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(username, { username = it }, label = { Text("Enter OSRS Username") }, singleLine = true, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Ink, unfocusedTextColor = Ink, focusedBorderColor = GoldDark, focusedLabelColor = GoldDark))
                Spacer(Modifier.height(12.dp))
            }
            when {
                selectedItem != null -> item { ItemDetailsScreen(selectedItem!!, report?.kills.orEmpty()) { selectedItem = null } }
                selectedTab == null -> item {
                    Button(onClick = { loadTab("Bosses") }, enabled = !loading, colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Ink), modifier = Modifier.fillMaxWidth()) { Text("Boss log") }
                    Spacer(Modifier.height(10.dp))
                    Button(onClick = { loadTab("Raids") }, enabled = !loading, colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Ink), modifier = Modifier.fillMaxWidth()) { Text("Raids log") }
                    Spacer(Modifier.height(18.dp)); Text(status, color = Ink, fontSize = 15.sp)
                }
                else -> {
                    item {
                        OutlinedButton({ selectedTab = null; report = null; status = "Choose Boss log or Raids log." }, Modifier.fillMaxWidth()) { Text("Back to log selection") }
                        if (loading) { Spacer(Modifier.height(12.dp)); LinearProgressIndicator(Modifier.fillMaxWidth(), color = GoldDark) }
                        Spacer(Modifier.height(18.dp)); Text(status, color = Ink, fontSize = 15.sp)
                    }
                    report?.bosses?.forEach { boss -> item(key = boss.name) { BossCard(boss, report!!.kills) { selectedItem = ItemDetail(boss.name, it) } } }
                }
            }
            item { Spacer(Modifier.height(20.dp)) }
        }
    }
}

@Composable private fun BossCard(boss: BossLog, kills: Map<String, Int>, onItemClick: (LogItem) -> Unit) {
    var expanded by rememberSaveable(boss.name) { mutableStateOf(false) }
    if (boss.items.isEmpty()) return
    val cardColor = if (boss.items.all { it.quantity > 0 }) CompletedParchment else Parchment
    Spacer(Modifier.height(10.dp))
    Card(colors = CardDefaults.cardColors(containerColor = cardColor), border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(GoldDark)), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth().clickable { expanded = !expanded }, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("${boss.name} — ${collectionKills(boss.name, kills)}", color = GoldDark, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                    if (boss.name == "Chambers of Xeric") Text("Assumed points per completion: Regular 49,750 • Challenge 66,400", color = Ink, fontSize = 13.sp)
                    Text(bossSummary(boss, kills), color = Ink, fontSize = 14.sp)
                }
                Text(if (expanded) "⌃" else "⌄", color = GoldDark, fontSize = 30.sp)
            }
            if (expanded) boss.items.forEach { ItemRow(boss.name, it, kills, onItemClick) }
        }
    }
}

@Composable private fun ItemRow(boss: String, item: LogItem, kills: Map<String, Int>, onItemClick: (LogItem) -> Unit) {
    val bitmap by produceState<android.graphics.Bitmap?>(null, item.id) { value = withContext(Dispatchers.IO) { runCatching { BitmapFactory.decodeStream(URL("https://cdn.runeprofile.com/item/${item.id}.png").openStream()) }.getOrNull() } }
    val obtained = item.quantity > 0
    val showDropRate = !(boss == "Chambers of Xeric" && coxItemWeight(item.name) != null)
    Row(Modifier.fillMaxWidth().heightIn(min = 72.dp).clickable { onItemClick(item) }.padding(vertical = 5.dp).alpha(if (obtained) 1f else 0.42f), verticalAlignment = Alignment.CenterVertically) {
        bitmap?.let { Image(it.asImageBitmap(), item.name, Modifier.size(55.dp)) }
        Spacer(Modifier.width(12.dp)); Column {
            Text(if (obtained) "${item.name} ×${item.quantity}" else item.name, color = Ink, fontSize = 16.sp)
            if (showDropRate) Text("Expected drop rate: ${dropRateLabel(boss, item.name)}", color = Ink, fontSize = 14.sp)
            val rate = if (obtained) rateDescription(boss, item.name, item.quantity, kills) else uncollectedRateDescription(boss, item.name, kills)
            rate?.let { Text("Rate: $it", color = Ink, fontSize = 14.sp) }
        }
        Spacer(Modifier.weight(1f)); Text("›", color = GoldDark, fontSize = 28.sp)
    }
}

@Composable private fun ItemDetailsScreen(detail: ItemDetail, kills: Map<String, Int>, onBack: () -> Unit) {
    val item = detail.item
    val bitmap by produceState<android.graphics.Bitmap?>(null, item.id) { value = withContext(Dispatchers.IO) { runCatching { BitmapFactory.decodeStream(URL("https://cdn.runeprofile.com/item/${item.id}.png").openStream()) }.getOrNull() } }
    val obtained = item.quantity > 0
    val calculatedRate = if (obtained) rateDescription(detail.boss, item.name, item.quantity, kills) else uncollectedRateDescription(detail.boss, item.name, kills)
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
                if (detail.boss == "Chambers of Xeric") Text("Assumed points: 49,750 regular • 66,400 Challenge", color = Ink, fontSize = 16.sp)
                Spacer(Modifier.height(12.dp)); Text("Expected drop rate: ${itemDetailsDropRate(detail.boss, item.name)}", color = Ink, fontSize = 16.sp)
                Text("Your rate: ${calculatedRate ?: "Not mapped yet"}", color = Ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

private fun itemDetailsDropRate(boss: String, item: String): String {
    if (boss == "Chambers of Xeric") coxItemWeight(item)?.let { return "Normal ${it.normal}/60 unique table • Challenge ${it.challenge}/56 unique table" }
    return dropRateLabel(boss, item)
}

@Preview(showBackground = true, backgroundColor = 0xFFF2E2BB)
@Composable private fun OsrsDryCalcPreview() = OsrsDryCalcApp()
