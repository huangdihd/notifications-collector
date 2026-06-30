package com.noticol.app.export

import com.noticol.app.data.NotificationEntity
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Exporter {

    private val dateFmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    fun toJson(items: List<NotificationEntity>): String {
        val arr = JSONArray()
        for (n in items) {
            val o = JSONObject()
            o.put("id", n.id)
            o.put("packageName", n.packageName)
            o.put("appName", n.appName)
            o.put("title", n.title)
            o.put("text", n.text)
            o.put("bigText", n.bigText)
            o.put("subText", n.subText)
            o.put("category", n.category)
            o.put("channelId", n.channelId)
            o.put("postTime", n.postTime)
            o.put("postTimeText", dateFmt.format(Date(n.postTime)))
            o.put("captureTime", n.captureTime)
            o.put("notifKey", n.notifKey)
            arr.put(o)
        }
        return arr.toString(2)
    }

    fun toCsv(items: List<NotificationEntity>): String {
        val header = listOf(
            "id", "packageName", "appName", "title", "text", "bigText",
            "subText", "category", "channelId", "postTime", "postTimeText",
            "captureTime", "notifKey"
        )
        val sb = StringBuilder()
        // UTF-8 BOM，方便 Excel 正确识别中文
        sb.append('﻿')
        sb.append(header.joinToString(",") { csvField(it) }).append("\r\n")
        for (n in items) {
            val row = listOf(
                n.id.toString(),
                n.packageName,
                n.appName,
                n.title,
                n.text,
                n.bigText,
                n.subText,
                n.category,
                n.channelId,
                n.postTime.toString(),
                dateFmt.format(Date(n.postTime)),
                n.captureTime.toString(),
                n.notifKey
            )
            sb.append(row.joinToString(",") { csvField(it) }).append("\r\n")
        }
        return sb.toString()
    }

    /** RFC 4180 转义：含逗号/引号/换行时用引号包裹，内部引号翻倍 */
    private fun csvField(value: String): String {
        val needsQuote = value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }
        val escaped = value.replace("\"", "\"\"")
        return if (needsQuote) "\"$escaped\"" else escaped
    }
}
