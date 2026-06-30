package com.noticol.app

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.noticol.app.data.AppDatabase
import com.noticol.app.data.AppLister
import com.noticol.app.data.InstalledApp
import com.noticol.app.data.NotificationEntity
import com.noticol.app.data.SettingsStore
import com.noticol.app.export.Exporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val db = AppDatabase.get(app)
    private val settings = SettingsStore(app)

    val notifications: StateFlow<List<NotificationEntity>> =
        db.notificationDao().observeAll()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val count: StateFlow<Int> =
        db.notificationDao().observeCount()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val monitored: StateFlow<Set<String>> =
        settings.monitoredPackages
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val themeMode: StateFlow<Int> =
        settings.themeMode
            .stateIn(viewModelScope, SharingStarted.Eagerly, SettingsStore.THEME_SYSTEM)

    fun setThemeMode(mode: Int) {
        viewModelScope.launch { settings.setThemeMode(mode) }
    }

    private val installedFlow = kotlinx.coroutines.flow.MutableStateFlow<List<InstalledApp>>(emptyList())
    val installed: StateFlow<List<InstalledApp>> = installedFlow

    fun loadInstalledApps() {
        viewModelScope.launch {
            val apps = withContext(Dispatchers.IO) {
                AppLister.list(getApplication(), getApplication<Application>().packageName)
            }
            installedFlow.value = apps
        }
    }

    fun toggleMonitored(pkg: String, enabled: Boolean) {
        viewModelScope.launch { settings.toggle(pkg, enabled) }
    }

    fun clearAll() {
        viewModelScope.launch { db.notificationDao().clearAll() }
    }

    /**
     * 把全部记录写入用户通过 SAF 选择的 Uri。
     * @param asJson true=JSON, false=CSV
     * @param onDone 回调写入条数；-1 表示失败
     */
    fun export(uri: Uri, asJson: Boolean, onDone: (Int) -> Unit) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val items = db.notificationDao().getAll()
                    val content = if (asJson) Exporter.toJson(items) else Exporter.toCsv(items)
                    getApplication<Application>().contentResolver
                        .openOutputStream(uri)?.use { out ->
                            out.write(content.toByteArray(Charsets.UTF_8))
                        } ?: error("无法打开输出流")
                    items.size
                }.getOrDefault(-1)
            }
            onDone(result)
        }
    }
}
