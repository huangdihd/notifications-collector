package com.noticol.app.service

import android.app.Notification
import android.content.pm.PackageManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.noticol.app.data.AppDatabase
import com.noticol.app.data.NotificationEntity
import com.noticol.app.data.SettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/**
 * 核心服务：系统在任意通知发布时回调 onNotificationPosted。
 * 我们只记录用户在设置里勾选的应用的通知，写入 Room 数据库。
 *
 * 需要用户在「系统设置 → 通知 → 通知使用权」中授予本应用权限，系统才会绑定此服务。
 */
class NotificationCollectorService : NotificationListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var settings: SettingsStore
    private lateinit var db: AppDatabase

    // 内存缓存监控集合，避免每条通知都读 DataStore
    private val monitored = MutableStateFlow<Set<String>>(emptySet())
    private val captureAll = MutableStateFlow(false)

    override fun onCreate() {
        super.onCreate()
        settings = SettingsStore(applicationContext)
        db = AppDatabase.get(applicationContext)
        scope.launch {
            settings.monitoredPackages.collect { monitored.value = it }
        }
        scope.launch {
            settings.captureAll.collect { captureAll.value = it }
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val notif = sbn ?: return
        val pkg = notif.packageName ?: return
        // 不记录本应用自身，避免自循环
        if (pkg == packageName) return
        // 调试开关开启时记录全部；否则只记录勾选的应用
        if (!captureAll.value && pkg !in monitored.value) return

        val entity = extract(notif)
        scope.launch {
            runCatching { db.notificationDao().insert(entity) }
        }
    }

    private fun extract(sbn: StatusBarNotification): NotificationEntity {
        val extras = sbn.notification?.extras
        fun cs(key: String): String =
            extras?.getCharSequence(key)?.toString().orEmpty()

        return NotificationEntity(
            packageName = sbn.packageName,
            appName = resolveAppName(sbn.packageName),
            title = cs(Notification.EXTRA_TITLE),
            text = cs(Notification.EXTRA_TEXT),
            bigText = cs(Notification.EXTRA_BIG_TEXT),
            subText = cs(Notification.EXTRA_SUB_TEXT),
            category = sbn.notification?.category.orEmpty(),
            channelId = sbn.notification?.channelId.orEmpty(),
            postTime = sbn.postTime,
            captureTime = System.currentTimeMillis(),
            notifKey = sbn.key.orEmpty()
        )
    }

    private fun resolveAppName(pkg: String): String =
        runCatching {
            val pm = packageManager
            pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
        }.getOrDefault(pkg)

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
