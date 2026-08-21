package com.example.drycalc.data

import com.example.drycalc.model.BossLog
import com.example.drycalc.model.LogItem
import com.example.drycalc.model.Report
import com.example.drycalc.rates.accountSummary
import com.example.drycalc.rates.raidsSummary
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

fun loadReport(username: String, tabName: String): Report {
    val encoded = URLEncoder.encode(username, "UTF-8").replace("+", "%20")
    val account = JSONObject(fetch("https://api.runeprofile.com/v1/accounts/$encoded/full"))
    val hiscores = JSONObject(fetch("https://secure.runescape.com/m=hiscore_oldschool/index_lite.json?player=$encoded"))
    val kills = buildMap { hiscores.getJSONArray("activities").let { activities -> for (i in 0 until activities.length()) put(activities.getJSONObject(i).getString("name"), activities.getJSONObject(i).getInt("score")) } }
    val tab = account.getJSONObject("collectionLog").getJSONArray("tabs").let { tabs ->
        (0 until tabs.length()).map { tabs.getJSONObject(it) }.first { it.getString("name") == tabName }
    }
    val bosses = tab.getJSONArray("pages").let { pages ->
        (0 until pages.length()).map { pageIndex ->
            pages.getJSONObject(pageIndex).let { page ->
                BossLog(page.getString("name"), page.getJSONArray("items").let { items ->
                    (0 until items.length()).map { itemIndex ->
                        items.getJSONObject(itemIndex).let { LogItem(it.getInt("id"), it.getString("name"), it.getInt("quantity")) }
                    }
                })
            }
        }
    }
    val summary = if (tabName == "Bosses") accountSummary(bosses, kills) else raidsSummary(bosses, kills)
    return Report(account.getString("username"), tabName, tab.getInt("obtained"), tab.getInt("total"), summary, bosses, kills)
}

private fun fetch(url: String): String {
    val connection = URL(url).openConnection() as HttpURLConnection
    connection.connectTimeout = 15_000
    connection.readTimeout = 20_000
    connection.setRequestProperty("User-Agent", "OsrsDryCalc/1.0")
    return connection.inputStream.bufferedReader().use { it.readText() }.also { connection.disconnect() }
}
