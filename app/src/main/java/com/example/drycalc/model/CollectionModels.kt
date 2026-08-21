package com.example.drycalc.model

data class LogItem(val id: Int, val name: String, val quantity: Int)
data class BossLog(val name: String, val items: List<LogItem>)
data class Report(
    val username: String,
    val tabName: String,
    val obtained: Int,
    val total: Int,
    val accountRate: String,
    val bosses: List<BossLog>,
    val kills: Map<String, Int>
)
data class ItemDetail(val boss: String, val item: LogItem)
