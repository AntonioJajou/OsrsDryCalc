package com.example.drycalc

import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale

private val Parchment = Color(0xFFF2E2BB)
private val Wood = Color(0xFF5A351C)
private val DarkWood = Color(0xFF2D190E)
private val Gold = Color(0xFFC89B3C)
private val GoldDark = Color(0xFF8D651D)
private val Ink = Color(0xFF2A1A10)
private val CompletedParchment = Color(0xFFD8E8C8)

private val verifiedFallbackRates = """
    abyssal sire|abyssal orphan|2560
    alchemical hydra|ikkle hydra|3000
    alchemical hydra|jar of chemicals|2000
    amoxliatl|moxi|3000
    amoxliatl|glacial temotli|100
    araxxor|nid|3000
    bryophyta|bryophyta's essence|118
    cerberus|jar of souls|2000
    chaos elemental|pet chaos elemental|300
    chaos elemental|dragon 2h sword|64
    chaos elemental|dragon pickaxe|256
    chaos fanatic|pet chaos elemental|1000
    chaos fanatic|odium shard 1|256
    chaos fanatic|malediction shard 1|256
    commander zilyana|pet zilyana|5000
    commander zilyana|saradomin hilt|508
    corporeal beast|pet dark core|5000
    corporeal beast|elysian sigil|4095
    corporeal beast|spectral sigil|1365
    corporeal beast|arcane sigil|1365
    corporeal beast|spirit shield|64
    corporeal beast|jar of spirits|1000
    crazy archaeologist|fedora|128
    crazy archaeologist|malediction shard 2|256
    deranged archaeologist|steel ring|43.7
    doom of mokhaiotl|dom|1000
    doom of mokhaiotl|avernic treads|1350
    doom of mokhaiotl|eye of ayak (uncharged)|2000
    doom of mokhaiotl|mokhaiotl cloth|2500
    fortis colosseum|smol heredit|200
    fortis colosseum|echo crystal|12.4
    giant mole|baby mole|3000
    grotesque guardians|noon|3000
    grotesque guardians|black tourmaline core|500
    grotesque guardians|granite gloves|250
    grotesque guardians|granite ring|250
    grotesque guardians|granite hammer|375
    grotesque guardians|jar of stone|5000
    hespori|bottomless compost bucket|35
    kalphite queen|kalphite princess|3000
    kalphite queen|kq head|128
    king black dragon|prince black dragon|3000
    king black dragon|kbd heads|128
    king black dragon|draconic visage|5000
    kraken|pet kraken|3000
    kree'arra|pet kree'arra|5000
    kree'arra|armadyl chainskirt|381
    k'ril tsutsaroth|pet k'ril tsutsaroth|5000
    k'ril tsutsaroth|staff of the dead|508
    k'ril tsutsaroth|steam battlestaff|127
    nex|nexling|500
    nex|ancient hilt|516
    nex|zaryte vambraces|172
    obor|hill giant club|118
    phantom muspah|muphin|2500
    royal titans|bran|3000
    royal titans|fire element staff crown|75
    sarachnis|sraracha|3000
    scorpia|scorpia's offspring|2015.8
    scorpia|odium shard 3|256
    scorpia|malediction shard 3|256
    scurrius|scurry|3000
    shellbane gryphon|gull|3000
    shellbane gryphon|jar of feathers|2000
    shellbane gryphon|belle's folly (tarnished)|256
    skotizo|skotos|65
    skotizo|jar of darkness|200
    skotizo|dark claw|25
    tempoross|tiny tempor|8000
    tempoross|big harpoonfish|1600
    thermonuclear smoke devil|pet smoke devil|3000
    thermonuclear smoke devil|jar of smoke|2000
    vardorvis|executioner's axe head|1088
    vorkath|vorki|3000
    vorkath|draconic visage|5000
    vorkath|skeletal visage|5000
    vorkath|jar of decay|3000
    vorkath|dragonbone necklace|1000
    the whisperer|wisp|2000
    the whisperer|siren's staff|512
    wintertodt|phoenix|5000
    yama|oathplate chest|600
    zalcano|smolcano|2250
    zalcano|crystal tool seed|205.1
    zalcano|zalcano shard|750
    zulrah|jar of swamp|3000
    the nightmare|little nightmare|800
    the nightmare|inquisitor's mace|750
    the nightmare|inquisitor's great helm|420
    the nightmare|inquisitor's hauberk|420
    the nightmare|inquisitor's plateskirt|420
    the nightmare|nightmare staff|300
    the nightmare|volatile orb|960
    the nightmare|harmonised orb|960
    the nightmare|eldritch orb|960
    the nightmare|jar of dreams|1900
    the fight caves|tzrek-jad|200
    the inferno|jal-nib-rek|100
    the hueycoatl|huberte|400
    the hueycoatl|dragon hunter wand|105
    the hueycoatl|tome of earth (empty)|90
""".trimIndent().lineSequence().associate { row ->
    val (boss, item, rate) = row.split("|")
    "$boss|$item" to rate.toDouble()
}

private fun sharedRateSources(item: String): Map<String, Double> = when (item) {
    "Virtus mask", "Virtus robe top", "Virtus robe bottom" -> mapOf("Duke Sucellus" to 2160.0, "Vardorvis" to 3264.0, "The Leviathan" to 2304.0, "The Whisperer" to 1536.0)
    "Chromium ingot" -> mapOf("Duke Sucellus" to 240.0, "Vardorvis" to 362.7, "The Leviathan" to 256.0, "The Whisperer" to 170.7)
    "Awakener's orb" -> mapOf("Duke Sucellus" to 48.5, "Vardorvis" to 80.6, "The Leviathan" to 53.6, "The Whisperer" to 34.5)
    "Godsword shard 1", "Godsword shard 2", "Godsword shard 3" -> mapOf("Commander Zilyana" to 762.0, "General Graardor" to 762.0, "Kree'Arra" to 762.0, "K'ril Tsutsaroth" to 762.0)
    "Draconic visage" -> mapOf("King Black Dragon" to 5000.0, "Vorkath" to 5000.0)
    "Dragon chainbody" -> mapOf("Kalphite Queen" to 128.0, "Thermonuclear Smoke Devil" to 2000.0)
    "Pet Chaos Elemental" -> mapOf("Chaos Elemental" to 300.0, "Chaos Fanatic" to 1000.0)
    else -> emptyMap()
}

private fun isSharedDt2Unique(boss: String, item: String): Boolean =
    sharedRateSources(item).containsKey(if (boss == "Kree'arra") "Kree'Arra" else boss)

private fun sharedDt2Rate(item: String, actual: Int, kills: Map<String, Int>): String {
    val rates = sharedRateSources(item)
    val expected = rates.entries.sumOf { (boss, denominator) -> (kills[boss] ?: 0) / denominator }
    if (expected <= 0) return "special calculation needed"
    if (actual == 0 && expected < 1) return "${"%.2f".format(Locale.US, expected)} expected • Haven't hit rate yet"
    if (actual == 0) return "${"%.2f".format(Locale.US, expected)} expected • ${kotlin.math.round(expected * 100).toInt()}% dry"
    return expectedText(actual, expected) ?: "special calculation needed"
}

private fun fallbackRate(boss: String, item: String) =
    if (isSharedDt2Unique(boss, item)) null else verifiedFallbackRates["${boss.lowercase()}|${item.lowercase()}"]

private fun isExcludedFromRateCalculation(item: String) = item in setOf("Draconic visage", "Occult necklace")

data class LogItem(val id: Int, val name: String, val quantity: Int)
data class BossLog(val name: String, val items: List<LogItem>)
data class Report(val username: String, val tabName: String, val obtained: Int, val total: Int, val accountRate: String, val bosses: List<BossLog>, val kills: Map<String, Int>)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { OsrsDryCalcApp() }
    }
}

@Composable
private fun OsrsDryCalcApp() {
    var username by rememberSaveable { mutableStateOf("") }
    var report by remember { mutableStateOf<Report?>(null) }
    var status by remember { mutableStateOf("Tap Load boss rates to fetch your data.") }
    var loading by remember { mutableStateOf(false) }
    var selectedTab by rememberSaveable { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val loadTab: (String) -> Unit = { tabName ->
        if (username.trim().isEmpty()) { status = "Enter a RuneScape username first." }
        else {
            selectedTab = tabName; loading = true; report = null; status = "Loading ${tabName.lowercase()} kill counts and collection-log drops…"
            scope.launch {
                runCatching { withContext(Dispatchers.IO) { loadReport(username.trim(), tabName) } }
                    .onSuccess { report = it; status = "${it.username} — ${it.tabName} log: ${it.obtained}/${it.total} unlocked\nCompletion: ${"%.1f".format(Locale.US, it.obtained * 100.0 / it.total)}%\n${it.accountRate}" }
                    .onFailure { status = "Could not load RuneProfile data. Check your connection and try again.\n\n${it.message}" }
                loading = false
            }
        }
    }
    MaterialTheme(colorScheme = lightColorScheme(primary = Wood, secondary = Gold, background = Parchment, surface = Parchment)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().background(Parchment).padding(horizontal = 20.dp),
            contentPadding = PaddingValues(vertical = 20.dp)
        ) {
            item {
                Text("How Dry Am I?", color = GoldDark, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text("Enter OSRS Username") }, singleLine = true, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Ink, unfocusedTextColor = Ink, focusedBorderColor = GoldDark, focusedLabelColor = GoldDark))
                Spacer(Modifier.height(12.dp))
            }
            if (selectedTab == null) {
                item {
                    Button(onClick = { loadTab("Bosses") }, enabled = !loading, colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Ink), modifier = Modifier.fillMaxWidth()) { Text("Boss log") }
                    Spacer(Modifier.height(10.dp))
                    Button(onClick = { loadTab("Raids") }, enabled = !loading, colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Ink), modifier = Modifier.fillMaxWidth()) { Text("Raids log") }
                    Spacer(Modifier.height(18.dp)); Text(status, color = Ink, fontSize = 15.sp)
                }
            } else {
                item {
                    OutlinedButton(onClick = { selectedTab = null; report = null; status = "Choose Boss log or Raids log." }, modifier = Modifier.fillMaxWidth()) { Text("Back to log selection") }
                    if (loading) { Spacer(Modifier.height(12.dp)); LinearProgressIndicator(Modifier.fillMaxWidth(), color = GoldDark) }
                    Spacer(Modifier.height(18.dp)); Text(status, color = Ink, fontSize = 15.sp)
                }
                report?.bosses?.forEach { boss ->
                    item(key = boss.name) { BossCard(boss, report!!.kills) }
                }
            }
            item { Spacer(Modifier.height(20.dp)) }
        }
    }
}

@Composable private fun BossCard(boss: BossLog, kills: Map<String, Int>) {
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
            if (expanded) boss.items.forEach { ItemRow(boss.name, it, kills) }
        }
    }
}

@Composable private fun ItemRow(boss: String, item: LogItem, kills: Map<String, Int>) {
    val bitmap by produceState<android.graphics.Bitmap?>(null, item.id) { value = withContext(Dispatchers.IO) { runCatching { BitmapFactory.decodeStream(URL("https://cdn.runeprofile.com/item/${item.id}.png").openStream()) }.getOrNull() } }
    val obtained = item.quantity > 0
    val showDropRate = !(boss == "Chambers of Xeric" && coxItemWeight(item.name) != null)
    Row(Modifier.fillMaxWidth().heightIn(min = 72.dp).padding(vertical = 5.dp).alpha(if (obtained) 1f else 0.42f), verticalAlignment = Alignment.CenterVertically) {
        bitmap?.let { Image(it.asImageBitmap(), item.name, Modifier.size(55.dp)) }
        Spacer(Modifier.width(12.dp)); Column {
            Text(if (obtained) "${item.name} ×${item.quantity}" else item.name, color = Ink, fontSize = 16.sp)
            if (obtained) {
                if (showDropRate) Text("Expected drop rate: ${dropRateLabel(boss, item.name)}", color = Ink, fontSize = 14.sp)
                rateDescription(boss, item.name, item.quantity, kills)?.let { Text("Rate: $it", color = Ink, fontSize = 14.sp) }
            }
            else {
                if (showDropRate) Text("Expected drop rate: ${dropRateLabel(boss, item.name)}", color = Ink, fontSize = 14.sp)
                uncollectedRateDescription(boss, item.name, kills)?.let { Text("Rate: $it", color = Ink, fontSize = 14.sp) }
            }
        }
    }
}

private fun fetch(url: String): String { val c = URL(url).openConnection() as HttpURLConnection; c.connectTimeout = 15000; c.readTimeout = 20000; c.setRequestProperty("User-Agent", "OsrsDryCalc/1.0"); return c.inputStream.bufferedReader().use { it.readText() }.also { c.disconnect() } }
private fun loadReport(username: String, tabName: String): Report {
    val encoded = URLEncoder.encode(username, "UTF-8").replace("+", "%20")
    val account = JSONObject(fetch("https://api.runeprofile.com/v1/accounts/$encoded/full")); val hiscores = JSONObject(fetch("https://secure.runescape.com/m=hiscore_oldschool/index_lite.json?player=$encoded"))
    val kills = buildMap { hiscores.getJSONArray("activities").let { a -> for (i in 0 until a.length()) put(a.getJSONObject(i).getString("name"), a.getJSONObject(i).getInt("score")) } }
    val tab = account.getJSONObject("collectionLog").getJSONArray("tabs").let { tabs -> (0 until tabs.length()).map { tabs.getJSONObject(it) }.first { it.getString("name") == tabName } }
    val bosses = tab.getJSONArray("pages").let { pages -> (0 until pages.length()).map { p -> pages.getJSONObject(p).let { page -> BossLog(page.getString("name"), page.getJSONArray("items").let { items -> (0 until items.length()).map { x -> items.getJSONObject(x).let { LogItem(it.getInt("id"), it.getString("name"), it.getInt("quantity")) } } }) } } }
    val summary = if (tabName == "Bosses") accountSummary(bosses, kills) else raidsSummary(bosses, kills)
    return Report(account.getString("username"), tabName, tab.getInt("obtained"), tab.getInt("total"), summary, bosses, kills)
}

private fun collectionKills(boss: String, k: Map<String, Int>): String {
    fun n(name: String) = String.format(Locale.US, "%,d", k[name] ?: 0)
    return when (boss) {
        "Dagannoth Kings" -> "Prime ${n("Dagannoth Prime")} • Rex ${n("Dagannoth Rex")} • Supreme ${n("Dagannoth Supreme")} KC"
        "The Gauntlet" -> "Gauntlet ${n("The Gauntlet")} • Corrupted ${n("The Corrupted Gauntlet")} KC"
        "The Fight Caves" -> "${n("TzTok-Jad")} KC"
        "Fortis Colosseum" -> "${n("Sol Heredit")} KC"
        "The Inferno" -> "${n("TzKal-Zuk")} KC"
        "Kree'arra" -> "${n("Kree'Arra")} KC"
        "Chambers of Xeric" -> "Regular ${n("Chambers of Xeric")} • Challenge ${n("Chambers of Xeric: Challenge Mode")} KC"
        "Theatre of Blood" -> "${n("Theatre of Blood")} KC"
        "Tombs of Amascut" -> "${n("Tombs of Amascut")} KC"
        else -> k[boss]?.let { "${n(boss)} KC" } ?: "KC not available in official HiScores"
    }
}
private fun expectedText(actual: Int, expected: Double) = if (expected <= 0) null else "${"%.2f".format(Locale.US, expected)} expected • ${kotlin.math.round(actual / expected * 100).toInt()}% ${if (actual / expected < 1) "dry" else "spooned"}"
private fun uncollectedRateDescription(boss: String, item: String, kills: Map<String, Int>): String? {
    val expected = rateDescription(boss, item, 0, kills)?.substringBefore(" expected")?.toDoubleOrNull() ?: return null
    if (expected < 1.0) return "${"%.2f".format(Locale.US, expected)} expected • Haven't hit rate yet"
    return "${"%.2f".format(Locale.US, expected)} expected • ${kotlin.math.round(expected * 100).toInt()}% dry"
}

// Ancient chest: a 1% unique chance per 8,676 points, then the item's current
// normal-mode (out of 60) or Challenge Mode (out of 56) table weight.
// Completion point values are intentionally app assumptions.
private data class CoxWeight(val normal: Int, val challenge: Int)
private fun coxItemWeight(item: String): CoxWeight? = when (item) {
    "Dexterous prayer scroll", "Arcane prayer scroll" -> CoxWeight(normal = 14, challenge = 12)
    "Twisted buckler", "Dragon hunter crossbow" -> CoxWeight(normal = 4, challenge = 4)
    "Dinh's bulwark" -> CoxWeight(normal = 3, challenge = 3)
    "Ancestral hat", "Ancestral robe top", "Ancestral robe bottom" -> CoxWeight(normal = 4, challenge = 4)
    "Dragon claws" -> CoxWeight(normal = 3, challenge = 3)
    "Elder maul", "Kodai insignia", "Twisted bow" -> CoxWeight(normal = 2, challenge = 2)
    else -> null
}
private fun coxExpected(item: String, k: Map<String, Int>): Double? {
    if (item in setOf("Uncut onyx", "Onyx")) return ((k["Chambers of Xeric"] ?: 0) + (k["Chambers of Xeric: Challenge Mode"] ?: 0)) / 400.0
    if (item in setOf("Twisted ancestral colour kit", "Twisted ancestral color kit", "Twisted kit")) return (k["Chambers of Xeric: Challenge Mode"] ?: 0) / 75.0
    if (item in setOf("Torn prayer scroll", "Dark relic")) return ((k["Chambers of Xeric"] ?: 0) + (k["Chambers of Xeric: Challenge Mode"] ?: 0)) * 2.0 / 33.0
    val weight = coxItemWeight(item) ?: return null
    val regularPoints = (k["Chambers of Xeric"] ?: 0) * 49_750.0
    val challengePoints = (k["Chambers of Xeric: Challenge Mode"] ?: 0) * 66_400.0
    return regularPoints / 867_600.0 * weight.normal / 60.0 +
        challengePoints / 867_600.0 * weight.challenge / 56.0
}
private fun coxRateDescription(item: String, actual: Int, k: Map<String, Int>): String? {
    val expected = coxExpected(item, k) ?: return null
    return expectedText(actual, expected)
}
private fun shouldIncludeInWeightedRate(boss: String, item: LogItem, kills: Map<String, Int>): Boolean {
    if (item.quantity > 0) return true
    val expected = rateDescription(boss, item.name, 0, kills)
        ?.substringBefore(" expected")?.toDoubleOrNull() ?: return false
    return expected > 1.0
}
private fun rateDescription(boss: String, item: String, actual: Int, k: Map<String, Int>): String? {
    if (boss == "Chambers of Xeric") return coxRateDescription(item, actual, k)
    if (boss in setOf("Theatre of Blood", "Tombs of Amascut")) return null
    if (isExcludedFromRateCalculation(item)) return null
    if (isSharedDt2Unique(boss, item)) return sharedDt2Rate(item, actual, k)
    if (boss == "Dagannoth Kings") {
        val source = when (item) {
            "Seers ring", "Mud battlestaff", "Pet Dagannoth Prime" -> "Dagannoth Prime"
            "Berserker ring", "Warrior ring", "Pet Dagannoth Rex" -> "Dagannoth Rex"
            "Archers ring", "Seercull", "Pet Dagannoth Supreme" -> "Dagannoth Supreme"
            else -> return null
        }
        val denominator = if (item.startsWith("Pet Dagannoth")) 5000.0 else 128.0
        return expectedText(actual, (k[source] ?: 0) / denominator)
    }
    val kills = k[if (boss == "Kree'arra") "Kree'Arra" else boss] ?: 0
    if (boss == "Amoxliatl" && item == "Frozen tear") return expectedText(actual, kills * 5.55)
    if (boss == "Yama") {
        when (item) {
            "Oathplate shards" -> return expectedText(actual, kills * 12.0 / 17.07)
            "Chasm teleport scroll" -> return expectedText(actual, kills * 24.0 / 95.11)
        }
    }
    if (boss == "The Gauntlet") { val expected = when (item) { "Crystal armour seed", "Crystal weapon seed" -> (k["The Gauntlet"] ?: 0) / 120.0 + (k["The Corrupted Gauntlet"] ?: 0) / 50.0; "Enhanced crystal weapon seed" -> (k["The Gauntlet"] ?: 0) / 2000.0 + (k["The Corrupted Gauntlet"] ?: 0) / 400.0; else -> 0.0 }; return expectedText(actual, expected) }
    val d = when (boss) {
        "Abyssal Sire" -> if (item == "Unsired") 100.0 else 0.0
        "Barrows Chests" -> if (barrows(item)) 350.14 else 0.0
        "Amoxliatl" -> if (item == "Pendant of ates (inert)") 25.0 else 0.0
        "Brutus" -> mapOf("Mooleta" to 30.0, "Bottomless milk bucket (empty)" to 37.5, "Cow slippers" to 150.0, "Beef" to 1000.0)[item] ?: 0.0
        "Alchemical Hydra" -> when (item) { "Hydra's claw" -> 1001.0; "Hydra tail" -> 513.0; "Hydra leather" -> 514.0; "Hydra's eye", "Hydra's fang", "Hydra's heart" -> 181.1; else -> 0.0 }
        "Araxxor" -> when (item) { "Araxyte fang", "Noxious point", "Noxious blade", "Noxious pommel" -> 600.0; "Araxyte head" -> 250.0; "Jar of venom" -> 1500.0; else -> 0.0 }
        "Cerberus" -> if (item == "Hellpuppy") 3000.0 else if (item in setOf("Eternal crystal", "Pegasian crystal", "Primordial crystal", "Smouldering stone")) 520.0 else 0.0
        "Duke Sucellus" -> when (item) { "Magus vestige", "Eye of the duke" -> 720.0; "Baron" -> 2500.0; else -> 0.0 }
        "General Graardor" -> if (item == "Bandos hilt") 508.0 else if (item in setOf("Bandos chestplate", "Bandos tassets", "Bandos boots")) 381.0 else 0.0
        "Commander Zilyana" -> mapOf("Armadyl crossbow" to 508.0, "Saradomin sword" to 127.0, "Saradomin's light" to 254.0)[item] ?: 0.0
        "Kree'arra" -> if (item == "Armadyl hilt") 508.0 else if (item in setOf("Armadyl helmet", "Armadyl chestplate")) 381.0 else 0.0
        "K'ril Tsutsaroth" -> if (item == "Zamorakian spear") 127.0 else if (item == "Zamorak hilt") 508.0 else 0.0
        "Kalphite Queen" -> if (item == "Jar of sand") 2000.0 else 0.0
        "Nex" -> if (item in setOf("Nihil horn", "Torva platebody (damaged)")) 258.0 else 0.0
        "Phantom Muspah" -> if (item == "Venator shard") 100.0 else if (item == "Ancient icon") 50.0 else 0.0
        "Sarachnis" -> mapOf("Sarachnis cudgel" to 384.0, "Giant egg sac(full)" to 20.0, "Pristine spider silk" to 50.0)[item] ?: 0.0
        "Scurrius" -> if (item == "Scurrius' spine") 33.0 else 0.0; "Maggot King" -> if (item == "Elder venator fang") 340.0 else 0.0; "Vardorvis" -> when (item) { "Ultor vestige", "Executioner's axe head" -> 1088.0; "Butch" -> 3000.0; else -> 0.0 }; "The Leviathan" -> when (item) { "Venator vestige", "Leviathan's lure" -> 768.0; "Lil'viathan" -> 2500.0; else -> 0.0 }; "The Whisperer" -> when (item) { "Bellator vestige", "Siren's staff" -> 512.0; "Wisp" -> 2000.0; "Shadow quartz" -> 209.3; "Sirenic tablet" -> 26.2; else -> 0.0 }
        "Yama" -> when (item) { "Yami" -> 2500.0; "Soulflame horn" -> 300.0; "Oathplate helm", "Oathplate legs" -> 600.0; "Dossier" -> 12.1; "Forgotten lockbox" -> 33.0; "Barrel of demonic tallow (full)" -> 95.11 / 5.0; else -> 0.0 }
        "Zulrah" -> if (item in setOf("Tanzanite fang", "Magic fang", "Serpentine visage")) 512.0 else if (item in setOf("Tanzanite mutagen", "Magma mutagen")) 6553.5 else if (item == "Pet Snakeling") 4000.0 else 0.0
        else -> 0.0
    }
    val resolvedDenominator = if (d == 0.0) fallbackRate(boss, item) ?: 0.0 else d
    return if (kills == 0 || resolvedDenominator == 0.0) null else expectedText(actual, kills / resolvedDenominator)
}
private fun barrows(i: String) = i.startsWith("Ahrim's ") || i.startsWith("Dharok's ") || i.startsWith("Guthan's ") || i.startsWith("Karil's ") || i.startsWith("Torag's ") || i.startsWith("Verac's ")
private fun dropRateLabel(boss: String, item: String): String {
    if (boss == "Chambers of Xeric") return when {
        coxItemWeight(item) != null -> "weighted Ancient chest unique rate"
        item in setOf("Uncut onyx", "Onyx") -> "1 in 400 per completion"
        item in setOf("Twisted ancestral colour kit", "Twisted ancestral color kit", "Twisted kit") -> "1 in 75 per Challenge Mode completion"
        item in setOf("Torn prayer scroll", "Dark relic") -> "1 in 16.5 per completion"
        else -> "special calculation needed"
    }
    if (boss in setOf("Theatre of Blood", "Tombs of Amascut")) return "special calculation needed (raid points and settings required)"
    if (isExcludedFromRateCalculation(item)) return "special calculation needed"
    if (boss == "Dagannoth Kings") return when {
        item.startsWith("Pet Dagannoth") -> "1 in 5,000 from its matching king"
        item in setOf("Seers ring", "Mud battlestaff", "Berserker ring", "Warrior ring", "Archers ring", "Seercull") -> "1 in 128 from its matching king"
        item == "Dragon axe" -> "special calculation needed"
        else -> "not mapped yet"
    }
    if (isSharedDt2Unique(boss, item)) return "combined across all source bosses"
    if (boss == "The Gauntlet") return when (item) { "Crystal armour seed", "Crystal weapon seed" -> "1 in 120 (normal) / 1 in 50 (corrupted)"; "Enhanced crystal weapon seed" -> "1 in 2,000 (normal) / 1 in 400 (corrupted)"; else -> "not mapped yet" }
    if (boss == "Amoxliatl" && item == "Frozen tear") return "5.55 per kill"
    if (boss == "Yama" && item == "Oathplate shards") return "12 per 17.07 kills"
    if (boss == "Yama" && item == "Chasm teleport scroll") return "24 per 95.11 kills"
    val denominator = when (boss) {
        "Abyssal Sire" -> if (item == "Unsired") 100.0 else 0.0
        "Barrows Chests" -> if (barrows(item)) 350.14 else 0.0
        "Amoxliatl" -> if (item == "Pendant of ates (inert)") 25.0 else 0.0
        "Brutus" -> mapOf("Mooleta" to 30.0, "Bottomless milk bucket (empty)" to 37.5, "Cow slippers" to 150.0, "Beef" to 1000.0)[item] ?: 0.0
        "Alchemical Hydra" -> when (item) { "Hydra's claw" -> 1001.0; "Hydra tail" -> 513.0; "Hydra leather" -> 514.0; "Hydra's eye", "Hydra's fang", "Hydra's heart" -> 181.1; else -> 0.0 }
        "Araxxor" -> when (item) { "Araxyte fang", "Noxious point", "Noxious blade", "Noxious pommel" -> 600.0; "Araxyte head" -> 250.0; "Jar of venom" -> 1500.0; else -> 0.0 }
        "Cerberus" -> if (item == "Hellpuppy") 3000.0 else if (item in setOf("Eternal crystal", "Pegasian crystal", "Primordial crystal", "Smouldering stone")) 520.0 else 0.0
        "Duke Sucellus" -> if (item in setOf("Magus vestige", "Eye of the duke")) 720.0 else 0.0
        "General Graardor" -> if (item == "Bandos hilt") 508.0 else if (item in setOf("Bandos chestplate", "Bandos tassets", "Bandos boots")) 381.0 else 0.0
        "Commander Zilyana" -> mapOf("Armadyl crossbow" to 508.0, "Saradomin sword" to 127.0, "Saradomin's light" to 254.0)[item] ?: 0.0
        "Kree'arra" -> if (item == "Armadyl hilt") 508.0 else if (item in setOf("Armadyl helmet", "Armadyl chestplate")) 381.0 else 0.0
        "K'ril Tsutsaroth" -> if (item == "Zamorakian spear") 127.0 else if (item == "Zamorak hilt") 508.0 else 0.0
        "Kalphite Queen" -> if (item == "Jar of sand") 2000.0 else 0.0
        "Nex" -> if (item in setOf("Nihil horn", "Torva platebody (damaged)")) 258.0 else 0.0
        "Phantom Muspah" -> if (item == "Venator shard") 100.0 else if (item == "Ancient icon") 50.0 else 0.0
        "Sarachnis" -> mapOf("Sarachnis cudgel" to 384.0, "Giant egg sac(full)" to 20.0, "Pristine spider silk" to 50.0)[item] ?: 0.0
        "Scurrius" -> if (item == "Scurrius' spine") 33.0 else 0.0
        "Maggot King" -> if (item == "Elder venator fang") 340.0 else 0.0
        "Vardorvis" -> if (item == "Ultor vestige") 1088.0 else 0.0
        "The Whisperer" -> if (item == "Bellator vestige") 512.0 else 0.0
        "Yama" -> when (item) { "Yami" -> 2500.0; "Soulflame horn" -> 300.0; "Oathplate helm", "Oathplate legs" -> 600.0; "Dossier" -> 12.1; "Forgotten lockbox" -> 33.0; "Barrel of demonic tallow (full)" -> 95.11 / 5.0; else -> 0.0 }
        "Zulrah" -> if (item in setOf("Tanzanite fang", "Magic fang", "Serpentine visage")) 512.0 else if (item in setOf("Tanzanite mutagen", "Magma mutagen")) 6553.5 else if (item == "Pet Snakeling") 4000.0 else 0.0
        else -> 0.0
    }
    val resolvedDenominator = if (denominator == 0.0) fallbackRate(boss, item) ?: 0.0 else denominator
    return if (resolvedDenominator == 0.0) "not mapped yet" else "1 in ${String.format(Locale.US, "%,.2f", resolvedDenominator).replace(".00", "")}" 
}
private fun bossSummary(b: BossLog, k: Map<String, Int>): String {
    if (b.name == "Chambers of Xeric") return coxSummary(b.items, k)
    var weightedRate = 0.0
    var totalWeight = 0.0
    val kills = when (b.name) { "Kree'arra" -> k["Kree'Arra"] ?: 0; "The Gauntlet" -> (k["The Gauntlet"] ?: 0) + (k["The Corrupted Gauntlet"] ?: 0); else -> k[b.name] ?: 0 }
    b.items.filter { shouldIncludeInWeightedRate(b.name, it, k) }.forEach { item ->
        val expected = rateDescription(b.name, item.name, item.quantity, k)?.substringBefore(" expected")?.toDoubleOrNull() ?: return@forEach
        if (expected <= 0 || kills <= 0) return@forEach
        val weight = kills / expected
        weightedRate += (item.quantity / expected) * weight
        totalWeight += weight
    }
    if (totalWeight == 0.0) return "Weighted total: special calculation needed"
    val percent = kotlin.math.round(weightedRate / totalWeight * 100).toInt()
    return "Weighted total: $percent% ${if (percent < 100) "dry" else "spooned"}"
}
private fun coxSummary(items: List<LogItem>, k: Map<String, Int>): String {
    var weightedRate = 0.0
    var totalWeight = 0.0
    items.filter { shouldIncludeInWeightedRate("Chambers of Xeric", it, k) }.forEach { item ->
        val expected = coxExpected(item.name, k) ?: return@forEach
        if (expected <= 0) return@forEach
        val rarityWeight = 1.0 / expected
        weightedRate += (item.quantity / expected) * rarityWeight
        totalWeight += rarityWeight
    }
    if (totalWeight == 0.0) return "Weighted total: special calculation needed"
    val percent = kotlin.math.round(weightedRate / totalWeight * 100).toInt()
    return "Weighted total: $percent% ${if (percent < 100) "dry" else "spooned"}"
}
private fun raidsSummary(bosses: List<BossLog>, k: Map<String, Int>): String {
    val chambers = bosses.firstOrNull { it.name == "Chambers of Xeric" }
        ?: return "Mapped weighted rate: special calculation needed"
    return coxSummary(chambers.items, k).replace("Weighted total", "Chambers mapped rate")
}
private fun accountSummary(bosses: List<BossLog>, k: Map<String, Int>): String {
    var weightedRate = 0.0
    var totalWeight = 0.0
    bosses.forEach { boss ->
        boss.items.filter { shouldIncludeInWeightedRate(boss.name, it, k) }.forEach itemLoop@ { item ->
            val expected = rateDescription(boss.name, item.name, item.quantity, k)
                ?.substringBefore(" expected")?.toDoubleOrNull() ?: return@itemLoop
            val kills = when (boss.name) {
                "Kree'arra" -> k["Kree'Arra"] ?: 0
                "The Gauntlet" -> (k["The Gauntlet"] ?: 0) + (k["The Corrupted Gauntlet"] ?: 0)
                else -> k[boss.name] ?: 0
            }
            if (expected <= 0 || kills <= 0) return@itemLoop
            val rarityWeight = kills / expected
            weightedRate += (item.quantity / expected) * rarityWeight
            totalWeight += rarityWeight
        }
    }
    if (totalWeight == 0.0) return "Mapped weighted rate: special calculation needed"
    val percent = kotlin.math.round(weightedRate / totalWeight * 100).toInt()
    return "Mapped weighted rate: $percent% ${if (percent < 100) "dry" else "spooned"}"
}
