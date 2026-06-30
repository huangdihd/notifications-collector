# 通知记录器 (NotificationsCollector)

一个 Android 16 应用，记录**指定 app** 的通知到本地数据库，并可导出为 **CSV / JSON**。

## 实现方式

采用 Android 官方的 `NotificationListenerService`，**无需 root 或 Xposed**。
用户只需在系统设置里授予一次「通知使用权」即可稳定捕获通知。

> 如果将来确实需要隐蔽运行或捕获被系统特殊限制的通知，再考虑改用 Xposed/LSPosed 模块方案。
> 对「记录指定 app 通知 + 导出」这个需求，当前方案是最稳定、最易维护的。

## 技术栈

- Kotlin + Jetpack Compose (Material 3)
- Room：本地通知存储
- DataStore：保存用户选择要监控的应用包名
- Storage Access Framework：导出文件（不申请存储权限）
- compileSdk / targetSdk = 36 (Android 16)，minSdk = 26 (Android 8)

## 构建

1. 用 **Android Studio**（建议 Ladybug 或更新版本）直接打开本目录。
   首次打开时 IDE 会自动下载 Gradle 8.9 并生成 wrapper。
   - 若用命令行，请先安装 Gradle 后执行 `gradle wrapper`，再 `./gradlew assembleDebug`。
2. 连接已开启 USB 调试的设备（Android 8+），点击 Run，或：
   ```
   ./gradlew installDebug
   ```

## 使用步骤

1. 打开应用，顶部若提示未授权，点「去授权」→ 在系统列表里找到「通知记录器」并打开开关。
2. 切到「监控应用」标签，勾选你想记录通知的 app（默认隐藏系统应用，可用开关显示）。
3. 之后这些 app 的通知会自动入库。在「记录」标签实时查看。
4. 点右上角下载图标 → 选择「导出为 JSON / CSV」→ 选择保存位置。
5. 垃圾桶图标可清空全部记录。
6. 亮度图标可切换主题：跟随系统 / 亮色 / 暗色(Android 12+ 跟随系统壁纸动态取色)。

## 导出字段

| 字段 | 说明 |
|------|------|
| id | 自增主键 |
| packageName | 应用包名 |
| appName | 应用名称 |
| title | 通知标题 |
| text | 通知正文 |
| bigText | 展开后的长文本 |
| subText | 附属文本 |
| category | 通知分类（如 msg / call） |
| channelId | 通知渠道 ID |
| postTime / postTimeText | 系统发布时间（毫秒 / 可读） |
| captureTime | 本应用入库时间（毫秒） |
| notifKey | 系统通知 key |

CSV 带 UTF-8 BOM，Excel 打开中文不乱码；字段按 RFC 4180 转义。

## 注意事项

- 系统重启或「通知使用权」被关闭后需要重新授权；服务由系统按需拉起。
- 部分厂商 ROM（MIUI/ColorOS 等）需额外允许自启动/后台运行以保证服务不被杀。
- 仅记录文本类字段，不抓取通知中的图片/媒体。

## 开源协议

[MIT](LICENSE)
