package com.noticol.app.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 单条被记录的通知。
 */
@Entity(
    tableName = "notifications",
    indices = [Index("packageName"), Index("postTime")]
)
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val appName: String,
    val title: String,
    val text: String,
    val bigText: String,
    val subText: String,
    val category: String,
    val channelId: String,
    /** 通知被系统发布的时间（毫秒） */
    val postTime: Long,
    /** 本应用捕获并入库的时间（毫秒） */
    val captureTime: Long,
    /** 系统分配的通知 key，用于去重判断 */
    val notifKey: String
)
