package com.antoniojajou.drycalc.rates

// Backup denominators for collection-log drops without a bespoke calculation.
val verifiedFallbackRates = """
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
    corporeal beast|holy elixir|170.67
    corporeal beast|jar of spirits|1000
    crazy archaeologist|fedora|128
    crazy archaeologist|malediction shard 2|256
    deranged archaeologist|steel ring|43.7
    doom of mokhaiotl|dom|1000
    doom of mokhaiotl|avernic treads|1350
    doom of mokhaiotl|eye of ayak (uncharged)|2000
    doom of mokhaiotl|mokhaiotl cloth|2500
    duke sucellus|frozen tablet|25.8
    duke sucellus|ice quartz|206.6
    fortis colosseum|smol heredit|200
    fortis colosseum|echo crystal|12.4
    giant mole|baby mole|3000
    general graardor|pet general graardor|5000
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
    kraken|jar of dirt|2000
    kree'arra|pet kree'arra|5000
    kree'arra|armadyl chainskirt|381
    k'ril tsutsaroth|pet k'ril tsutsaroth|5000
    k'ril tsutsaroth|staff of the dead|508
    k'ril tsutsaroth|steam battlestaff|127
    nex|nexling|500
    nex|ancient hilt|516
    nex|zaryte vambraces|172
    nex|torva full helm (damaged)|258
    nex|torva platelegs (damaged)|258
    obor|hill giant club|118
    phantom muspah|muphin|2500
    royal titans|bran|3000
    royal titans|fire element staff crown|75
    sarachnis|sraracha|3000
    sarachnis|jar of eyes|2000
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
    thermonuclear smoke devil|smoke battlestaff|512
    thermonuclear smoke devil|jar of smoke|2000
    vardorvis|executioner's axe head|1088
    vardorvis|blood quartz|204
    vardorvis|strangled tablet|25.5
    vorkath|vorki|3000
    vorkath|vorkath's head|50
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
