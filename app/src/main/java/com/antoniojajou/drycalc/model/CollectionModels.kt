package com.antoniojajou.drycalc.model

data class LogItem(val id: Int, val name: String, val quantity: Int)
data class BossLog(val name: String, val items: List<LogItem>)
data class CoxPointAverages(
    val regular: Double = 49_750.0,
    val challenge: Double = 66_400.0
)
data class Report(
    val username: String,
    val tabName: String,
    val obtained: Int,
    val total: Int,
    val accountRate: String,
    val bosses: List<BossLog>,
    val kills: Map<String, Int>,
    val coxPoints: CoxPointAverages = CoxPointAverages()
)
data class ItemDetail(val boss: String, val item: LogItem)
