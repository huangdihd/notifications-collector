package com.noticol.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

/**
 * 保存用户选择要监控的应用包名集合。
 * 服务每次收到通知时读取这个集合判断是否记录。
 */
class SettingsStore(private val context: Context) {

    private val monitoredKey = stringSetPreferencesKey("monitored_packages")
    private val themeKey = intPreferencesKey("theme_mode")

    val monitoredPackages: Flow<Set<String>> =
        context.dataStore.data.map { it[monitoredKey] ?: emptySet() }

    /** 主题模式：0=跟随系统，1=亮色，2=暗色 */
    val themeMode: Flow<Int> =
        context.dataStore.data.map { it[themeKey] ?: THEME_SYSTEM }

    suspend fun setThemeMode(mode: Int) {
        context.dataStore.edit { it[themeKey] = mode }
    }

    companion object {
        const val THEME_SYSTEM = 0
        const val THEME_LIGHT = 1
        const val THEME_DARK = 2
    }

    suspend fun getMonitoredPackages(): Set<String> =
        context.dataStore.data.first()[monitoredKey] ?: emptySet()

    suspend fun setMonitored(packages: Set<String>) {
        context.dataStore.edit { it[monitoredKey] = packages }
    }

    suspend fun toggle(packageName: String, enabled: Boolean) {
        context.dataStore.edit { prefs ->
            val current = (prefs[monitoredKey] ?: emptySet()).toMutableSet()
            if (enabled) current.add(packageName) else current.remove(packageName)
            prefs[monitoredKey] = current
        }
    }
}
