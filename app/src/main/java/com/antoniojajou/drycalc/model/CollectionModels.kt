package com.antoniojajou.drycalc.model

data class LogItem(val id: Int, val name: String, val quantity: Int)
data class BossLog(val name: String, val items: List<LogItem>)
data class CoxPointAverages(
    val regular: Double = 49_750.0,
    val challenge: Double = 66_400.0
)
data class ToaAverages(
    val normalPoints: Double = 0.0,
    val normalLevel: Double = 0.0,
    val expertPoints: Double = 0.0,
    val expertLevel: Double = 0.0
)
data class Report(
    val username: String,
    val tabName: String,
    val obtained: Int,
    val total: Int,
    val accountRate: String,
    val bosses: List<BossLog>,
    val kills: Map<String, Int>,
    val coxPoints: CoxPointAverages = CoxPointAverages(),
    val toaAverages: ToaAverages = ToaAverages()
)
data class ItemDetail(val boss: String, val item: LogItem)
