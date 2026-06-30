package com.noticol.app

import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.viewmodel.compose.viewModel
import com.noticol.app.data.NotificationEntity
import com.noticol.app.data.SettingsStore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val vm: MainViewModel = viewModel()
            val mode by vm.themeMode.collectAsState()
            val context = LocalContext.current
            val dark = when (mode) {
                SettingsStore.THEME_LIGHT -> false
                SettingsStore.THEME_DARK -> true
                else -> isSystemInDarkTheme()
            }
            val scheme = when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
                    if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
                dark -> darkColorScheme()
                else -> lightColorScheme()
            }
            MaterialTheme(colorScheme = scheme) {
                Surface(color = MaterialTheme.colorScheme.background) {
                    AppScreen(vm)
                }
            }
        }
    }
}

/** 判断是否已授予通知使用权 */
private fun isNotificationAccessGranted(activity: ComponentActivity): Boolean {
    val enabled = Settings.Secure.getString(
        activity.contentResolver, "enabled_notification_listeners"
    ) ?: return false
    if (TextUtils.isEmpty(enabled)) return false
    val self = activity.packageName
    return enabled.split(":").any { it.substringBefore("/") == self }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScreen(vm: MainViewModel = viewModel()) {
    val context = LocalContext.current
    val activity = context as ComponentActivity

    var tab by remember { mutableStateOf(0) }
    var granted by remember { mutableStateOf(isNotificationAccessGranted(activity)) }

    // 从设置返回时重新检查权限
    val settingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { granted = isNotificationAccessGranted(activity) }

    val count by vm.count.collectAsState()

    // 导出：分别用 JSON / CSV 两个 SAF launcher
    val jsonExport = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let { vm.export(it, asJson = true) { n -> toastResult(activity, n) } }
    }
    val csvExport = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let { vm.export(it, asJson = false) { n -> toastResult(activity, n) } }
    }

    var menuOpen by remember { mutableStateOf(false) }
    var themeMenuOpen by remember { mutableStateOf(false) }
    val themeMode by vm.themeMode.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("通知记录器  ·  $count 条") },
                actions = {
                    IconButton(onClick = { themeMenuOpen = true }) {
                        Icon(Icons.Default.BrightnessMedium, contentDescription = "主题")
                    }
                    DropdownMenu(expanded = themeMenuOpen, onDismissRequest = { themeMenuOpen = false }) {
                        ThemeMenuItem("跟随系统", SettingsStore.THEME_SYSTEM, themeMode) {
                            vm.setThemeMode(it); themeMenuOpen = false
                        }
                        ThemeMenuItem("亮色", SettingsStore.THEME_LIGHT, themeMode) {
                            vm.setThemeMode(it); themeMenuOpen = false
                        }
                        ThemeMenuItem("暗色", SettingsStore.THEME_DARK, themeMode) {
                            vm.setThemeMode(it); themeMenuOpen = false
                        }
                    }
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Default.Download, contentDescription = "导出")
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(text = { Text("导出为 JSON") }, onClick = {
                            menuOpen = false
                            jsonExport.launch("notifications_${stamp()}.json")
                        })
                        DropdownMenuItem(text = { Text("导出为 CSV") }, onClick = {
                            menuOpen = false
                            csvExport.launch("notifications_${stamp()}.csv")
                        })
                    }
                    IconButton(onClick = { vm.clearAll() }) {
                        Icon(Icons.Default.Delete, contentDescription = "清空")
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            if (!granted) {
                PermissionBanner {
                    settingsLauncher.launch(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                }
            }
            TabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("记录") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("监控应用") })
            }
            when (tab) {
                0 -> NotificationsTab(vm)
                1 -> AppsTab(vm)
            }
        }
    }
}

@Composable
private fun ThemeMenuItem(label: String, mode: Int, current: Int, onPick: (Int) -> Unit) {
    DropdownMenuItem(
        text = { Text(label) },
        onClick = { onPick(mode) },
        trailingIcon = {
            if (mode == current) Icon(Icons.Default.Check, contentDescription = "已选")
        }
    )
}

@Composable
private fun PermissionBanner(onClick: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.errorContainer) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "尚未授予通知使用权，无法记录。",
                Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Button(onClick = onClick) { Text("去授权") }
        }
    }
}

@Composable
private fun NotificationsTab(vm: MainViewModel) {
    val list by vm.notifications.collectAsState()
    if (list.isEmpty()) {
        EmptyHint("暂无记录。\n请在「监控应用」中勾选应用，并确保已授予通知使用权。")
        return
    }
    LazyColumn(Modifier.fillMaxSize()) {
        items(list, key = { it.id }) { n -> NotificationRow(n) }
    }
}

@Composable
private fun NotificationRow(n: NotificationEntity) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
        Row(Modifier.fillMaxWidth()) {
            Text(
                n.appName.ifEmpty { n.packageName },
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(timeFmt(n.postTime), style = MaterialTheme.typography.labelSmall)
        }
        if (n.title.isNotEmpty()) {
            Text(n.title, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        val body = n.text.ifEmpty { n.bigText }
        if (body.isNotEmpty()) {
            Text(body, style = MaterialTheme.typography.bodyMedium, maxLines = 4, overflow = TextOverflow.Ellipsis)
        }
    }
    HorizontalDivider()
}

@Composable
private fun AppsTab(vm: MainViewModel) {
    val installed by vm.installed.collectAsState()
    val monitored by vm.monitored.collectAsState()
    var showSystem by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { vm.loadInstalledApps() }

    Column(Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = { Text("搜索应用名或包名") },
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { query = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "清除")
                    }
                }
            }
        )
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("显示系统应用", Modifier.weight(1f))
            Switch(checked = showSystem, onCheckedChange = { showSystem = it })
        }
        HorizontalDivider()
        if (installed.isEmpty()) {
            EmptyHint("正在加载已安装应用…")
            return@Column
        }
        val q = query.trim().lowercase()
        val shown = installed.filter { app ->
            (showSystem || !app.isSystem) &&
                (q.isEmpty() ||
                    app.label.lowercase().contains(q) ||
                    app.packageName.lowercase().contains(q))
        }
        if (shown.isEmpty()) {
            EmptyHint("没有匹配的应用")
            return@Column
        }
        LazyColumn(Modifier.fillMaxSize()) {
            items(shown, key = { it.packageName }) { appInfo ->
                val checked = appInfo.packageName in monitored
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AppIcon(appInfo.icon)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(appInfo.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            appInfo.packageName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Checkbox(
                        checked = checked,
                        onCheckedChange = { vm.toggleMonitored(appInfo.packageName, it) }
                    )
                }
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun AppIcon(drawable: Drawable?) {
    val shape = Modifier.size(40.dp).clip(CircleShape)
    val bmp = remember(drawable) {
        runCatching { drawable?.toBitmap(96, 96)?.asImageBitmap() }.getOrNull()
    }
    if (bmp != null) {
        Image(bitmap = bmp, contentDescription = null, modifier = shape)
    } else {
        Box(shape.background(MaterialTheme.colorScheme.surfaceVariant))
    }
}

@Composable
private fun EmptyHint(text: String) {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun toastResult(activity: ComponentActivity, n: Int) {
    val msg = if (n >= 0) "已导出 $n 条" else "导出失败"
    Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show()
}

private fun timeFmt(t: Long): String =
    SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault()).format(Date(t))

private fun stamp(): String =
    SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
