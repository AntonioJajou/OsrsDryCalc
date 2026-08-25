package com.antoniojajou.drycalc.rates

import com.antoniojajou.drycalc.model.BossLog
import com.antoniojajou.drycalc.model.LogItem
import java.util.Locale

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
    if (expected <= 0) return "Dry rate unavailable"
    if (actual == 0 && expected < 1) return "${"%.2f".format(Locale.US, expected)} expected • Haven't hit rate yet"
    if (actual == 0) return "${"%.2f".format(Locale.US, expected)} expected • ${kotlin.math.round(expected * 100).toInt()}% dry"
    return expectedText(actual, expected) ?: "Dry rate unavailable"
}

private fun fallbackRate(boss: String, item: String) =
    if (isSharedDt2Unique(boss, item)) null else verifiedFallbackRates["${boss.lowercase()}|${item.lowercase()}"]

// These collection-log entries are supplies, reward-pool items, or guaranteed
// completion rewards. They remain visible but are deliberately excluded from dry calculations.
private val excludedBossLogItems = setOf(
    "Araxyte venom sac", "Coagulated venom", "Bolt rack", "Key master teleport",
    "Immaculate mole skin", "Mole claw", "Mole skin", "Granite dust",
    "Ancient essence", "Charged ice", "Frozen cache", "Sunfire splinters",
    "Atlatl dart", "Nihil shard", "Desiccated page", "Soaked page", "Spirit flakes",
    "Bruma torch", "Burnt page", "Zulrah's scales", "Huasca seed", "Hueycoatl hide",
    "Soiled page", "Dark totem", "Fire cape", "Infernal cape", "Gauntlet cape",
    "Pyromancer boots", "Pyromancer garb", "Pyromancer hood", "Pyromancer robe",
    "Warm gloves", "Tome of Fire (empty)", "Fish barrel", "Tackle box",
    "Spirit angler boots", "Spirit angler headband", "Spirit angler top", "Spirit angler waders",
    "Tome of Water (empty)"
)
// These can also come from Slayer monsters, but the official HiScores do not
// provide enough source-specific KC to calculate a fair boss-only dry rate.
private val sharedWithSlayerLogItems = setOf(
    "Araxyte head",
    "Abyssal dagger", "Abyssal head", "Abyssal whip", "Draconic visage",
    "Dragon chainbody", "Dragon harpoon", "Dragon knife", "Dragon thrownaxe",
    "Frozen tear", "Glacial temotli", "Gryphon feather", "Hydra tail",
    "Hydra's eye", "Hydra's fang", "Hydra's heart", "Kraken tentacle",
    "Occult necklace", "Pendant of Ates (inert)"
)
fun isSharedWithSlayerMonster(item: String) = item in sharedWithSlayerLogItems
fun isExcludedFromRateCalculation(item: String) =
    item in excludedBossLogItems || item in sharedWithSlayerLogItems
fun collectionKills(boss: String, k: Map<String, Int>): String {
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
val bludgeonPieces = setOf("Bludgeon spine", "Bludgeon claw", "Bludgeon axon")

fun uncollectedRateDescription(boss: String, item: String, kills: Map<String, Int>): String? {
    val expected = rateDescription(boss, item, 0, kills)?.substringBefore(" expected")?.toDoubleOrNull() ?: return null
    if (expected < 1.0) return "${"%.2f".format(Locale.US, expected)} expected • Haven't hit rate yet"
    return "${"%.2f".format(Locale.US, expected)} expected • ${kotlin.math.round(expected * 100).toInt()}% dry"
}

// Ancient chest: a 1% unique chance per 8,676 points, then the item's current
// normal-mode (out of 60) or Challenge Mode (out of 56) table weight.
// Completion point values are intentionally app assumptions.
data class CoxWeight(val normal: Int, val challenge: Int)
fun coxItemWeight(item: String): CoxWeight? = when (item) {
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
fun rateDescription(boss: String, item: String, actual: Int, k: Map<String, Int>): String? {
    if (boss == "Chambers of Xeric") return coxRateDescription(item, actual, k)
    if (boss == "Abyssal Sire" && item in bludgeonPieces) {
        return expectedText(actual, (k[boss] ?: 0) / (100.0 * 128.0 / 62.0))
    }
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
        "Abyssal Sire" -> when (item) {
            "Unsired" -> 100.0
            "Jar of Miasma" -> 100.0 * 128.0 / 13.0
            else -> 0.0
        }
        "Barrows Chests" -> if (barrows(item)) 350.14 else 0.0
        "Amoxliatl" -> if (item == "Pendant of ates (inert)") 25.0 else 0.0
        "Brutus" -> mapOf("Mooleta" to 30.0, "Bottomless milk bucket (empty)" to 37.5, "Cow slippers" to 150.0, "Beef" to 1000.0)[item] ?: 0.0
        "Alchemical Hydra" -> when (item) { "Hydra's claw" -> 1001.0; "Hydra tail" -> 513.0; "Hydra leather" -> 514.0; "Hydra's eye", "Hydra's fang", "Hydra's heart" -> 181.1; "Alchemical hydra heads" -> 256.0; else -> 0.0 }
        "Araxxor" -> when (item) { "Araxyte fang", "Noxious point", "Noxious blade", "Noxious pommel" -> 600.0; "Araxyte head" -> 250.0; "Jar of Venom" -> 1500.0; else -> 0.0 }
        "Cerberus" -> if (item == "Hellpuppy") 3000.0 else if (item in setOf("Eternal crystal", "Pegasian crystal", "Primordial crystal", "Smouldering stone")) 520.0 else 0.0
        "Duke Sucellus" -> when (item) { "Magus vestige", "Eye of the Duke" -> 720.0; "Baron" -> 2500.0; else -> 0.0 }
        "General Graardor" -> if (item == "Bandos hilt") 508.0 else if (item in setOf("Bandos chestplate", "Bandos tassets", "Bandos boots")) 381.0 else 0.0
        "Commander Zilyana" -> mapOf("Armadyl crossbow" to 508.0, "Saradomin sword" to 127.0, "Saradomin's light" to 254.0)[item] ?: 0.0
        "Kree'arra" -> if (item == "Armadyl hilt") 508.0 else if (item in setOf("Armadyl helmet", "Armadyl chestplate")) 381.0 else 0.0
        "K'ril Tsutsaroth" -> if (item == "Zamorakian spear") 127.0 else if (item == "Zamorak hilt") 508.0 else 0.0
        "Kalphite Queen" -> if (item == "Jar of Sand") 2000.0 else 0.0
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
fun dropRateLabel(boss: String, item: String): String {
    if (boss == "Chambers of Xeric") return when {
        coxItemWeight(item) != null -> "weighted Ancient chest unique rate"
        item in setOf("Uncut onyx", "Onyx") -> "1 in 400 per completion"
        item in setOf("Twisted ancestral colour kit", "Twisted ancestral color kit", "Twisted kit") -> "1 in 75 per Challenge Mode completion"
        item in setOf("Torn prayer scroll", "Dark relic") -> "1 in 16.5 per completion"
        else -> "Dry rate unavailable"
    }
    if (boss in setOf("Theatre of Blood", "Tombs of Amascut")) return "Dry rate unavailable — raid points and settings required"
    if (boss == "Abyssal Sire" && item in bludgeonPieces) return "1 in 206.45"
    if (item in sharedWithSlayerLogItems) return "Not included — dropped by Slayer monster"
    if (item in excludedBossLogItems) return "Not included in dry calculations"
    if (isExcludedFromRateCalculation(item)) return "Not included in dry calculations"
    if (boss == "Dagannoth Kings") return when {
        item.startsWith("Pet Dagannoth") -> "1 in 5,000 from its matching king"
        item in setOf("Seers ring", "Mud battlestaff", "Berserker ring", "Warrior ring", "Archers ring", "Seercull") -> "1 in 128 from its matching king"
        item == "Dragon axe" -> "Dry rate unavailable — source-specific KC required"
        else -> "not mapped yet"
    }
    if (isSharedDt2Unique(boss, item)) return "combined across all source bosses"
    if (boss == "The Gauntlet") return when (item) { "Crystal armour seed", "Crystal weapon seed" -> "1 in 120 (normal) / 1 in 50 (corrupted)"; "Enhanced crystal weapon seed" -> "1 in 2,000 (normal) / 1 in 400 (corrupted)"; else -> "not mapped yet" }
    if (boss == "Amoxliatl" && item == "Frozen tear") return "5.55 per kill"
    if (boss == "Yama" && item == "Oathplate shards") return "12 per 17.07 kills"
    if (boss == "Yama" && item == "Chasm teleport scroll") return "24 per 95.11 kills"
    val denominator = when (boss) {
        "Abyssal Sire" -> when (item) {
            "Unsired" -> 100.0
            "Jar of Miasma" -> 100.0 * 128.0 / 13.0
            else -> 0.0
        }
        "Barrows Chests" -> if (barrows(item)) 350.14 else 0.0
        "Amoxliatl" -> if (item == "Pendant of ates (inert)") 25.0 else 0.0
        "Brutus" -> mapOf("Mooleta" to 30.0, "Bottomless milk bucket (empty)" to 37.5, "Cow slippers" to 150.0, "Beef" to 1000.0)[item] ?: 0.0
        "Alchemical Hydra" -> when (item) { "Hydra's claw" -> 1001.0; "Hydra tail" -> 513.0; "Hydra leather" -> 514.0; "Hydra's eye", "Hydra's fang", "Hydra's heart" -> 181.1; "Alchemical hydra heads" -> 256.0; else -> 0.0 }
        "Araxxor" -> when (item) { "Araxyte fang", "Noxious point", "Noxious blade", "Noxious pommel" -> 600.0; "Araxyte head" -> 250.0; "Jar of Venom" -> 1500.0; else -> 0.0 }
        "Cerberus" -> if (item == "Hellpuppy") 3000.0 else if (item in setOf("Eternal crystal", "Pegasian crystal", "Primordial crystal", "Smouldering stone")) 520.0 else 0.0
        "Duke Sucellus" -> if (item in setOf("Magus vestige", "Eye of the Duke")) 720.0 else 0.0
        "General Graardor" -> if (item == "Bandos hilt") 508.0 else if (item in setOf("Bandos chestplate", "Bandos tassets", "Bandos boots")) 381.0 else 0.0
        "Commander Zilyana" -> mapOf("Armadyl crossbow" to 508.0, "Saradomin sword" to 127.0, "Saradomin's light" to 254.0)[item] ?: 0.0
        "Kree'arra" -> if (item == "Armadyl hilt") 508.0 else if (item in setOf("Armadyl helmet", "Armadyl chestplate")) 381.0 else 0.0
        "K'ril Tsutsaroth" -> if (item == "Zamorakian spear") 127.0 else if (item == "Zamorak hilt") 508.0 else 0.0
        "Kalphite Queen" -> if (item == "Jar of Sand") 2000.0 else 0.0
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
fun bossSummary(b: BossLog, k: Map<String, Int>): String? {
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
    if (totalWeight == 0.0) return "Weighted total: Haven't hit an expected drop rate"
    val percent = kotlin.math.round(weightedRate / totalWeight * 100).toInt()
    return "Weighted total: $percent% ${if (percent < 100) "dry" else "spooned"}"
}
private fun coxSummary(items: List<LogItem>, k: Map<String, Int>): String? {
    var weightedRate = 0.0
    var totalWeight = 0.0
    items.filter { shouldIncludeInWeightedRate("Chambers of Xeric", it, k) }.forEach { item ->
        val expected = coxExpected(item.name, k) ?: return@forEach
        if (expected <= 0) return@forEach
        val rarityWeight = 1.0 / expected
        weightedRate += (item.quantity / expected) * rarityWeight
        totalWeight += rarityWeight
    }
    if (totalWeight == 0.0) return "Weighted total: Haven't hit an expected drop rate"
    val percent = kotlin.math.round(weightedRate / totalWeight * 100).toInt()
    return "Weighted total: $percent% ${if (percent < 100) "dry" else "spooned"}"
}
fun raidsSummary(bosses: List<BossLog>, k: Map<String, Int>): String {
    val chambers = bosses.firstOrNull { it.name == "Chambers of Xeric" }
        ?: return "Mapped weighted rate: no Chambers data available"
    return coxSummary(chambers.items, k)?.replace("Weighted total", "Chambers mapped rate")
        ?: "Chambers mapped rate: Haven't hit an expected drop rate"
}
fun accountSummary(bosses: List<BossLog>, k: Map<String, Int>): String {
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
    if (totalWeight == 0.0) return "Mapped weighted rate: Haven't hit an expected drop rate yet"
    val percent = kotlin.math.round(weightedRate / totalWeight * 100).toInt()
    return "Mapped weighted rate: $percent% ${if (percent < 100) "dry" else "spooned"}"
}
