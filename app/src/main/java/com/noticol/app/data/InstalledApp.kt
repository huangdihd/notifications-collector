package com.noticol.app.data

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable

data class InstalledApp(
    val packageName: String,
    val label: String,
    val isSystem: Boolean,
    val icon: Drawable?
)

object AppLister {
    /** 列出已安装应用（默认排除本应用），按名称排序 */
    fun list(context: Context, selfPackage: String): List<InstalledApp> {
        val pm = context.packageManager
        return pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .asSequence()
            .filter { it.packageName != selfPackage }
            .map {
                InstalledApp(
                    packageName = it.packageName,
                    label = pm.getApplicationLabel(it).toString(),
                    isSystem = (it.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                    icon = runCatching { pm.getApplicationIcon(it) }.getOrNull()
                )
            }
            .sortedBy { it.label.lowercase() }
            .toList()
    }
}
