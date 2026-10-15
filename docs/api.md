# 公开 API（接入面 v2）

只列宿主会碰到的东西；状态对象、存储、`*Content`、Session 全部 internal。设计与取舍见 [design.md](design.md)。

## 根

```kotlin
package wang.harlon.appsettings

/** 唯一的配置入口。块是 @Composable，可以读状态；每次组合重新求值，结构性选项只在首次组合生效。 */
@Composable fun rememberAppSettings(block: @Composable AppSettingsConfigScope.() -> Unit = {}): AppSettings

/** 主题 + 系统栏 + 更新说明弹窗；等价于 MaterialTheme。 */
@Composable fun AppSettingsRoot(appSettings: AppSettings, typography: Typography = …, shapes: Shapes = …, content: @Composable () -> Unit)

class AppSettings {
    val appearance: AppearanceInfo       // mode: ThemeMode；@Composable isDark()
}

class AppSettingsConfigScope {
    var settings: Settings?                     // 不给则库自建
    var appVersion: String?                     // 不给则读平台
    var onEvent: (AppSettingsEvent) -> Unit
    fun appearance(block: AppearanceConfig.() -> Unit)
    fun about(block: AboutConfig.() -> Unit)
    fun whatsNew(block: WhatsNewConfig.() -> Unit)
    fun topBar(block: TopBarConfig.() -> Unit)
}

class AppearanceConfig {
    var modes: List<ThemeMode>; var colorSource: ColorSource; var palette: List<ThemePreset>; var defaultColor: ColorChoice
    var allowCustom: Boolean; var allowSystemDynamic: Boolean; var customHistoryLimit: Int
    var appIcons: List<AppIconOption>; var animate: Boolean; var syncSystemBars: Boolean
}
class AboutConfig {
    var name: String?; var version: String?; var icon: (@Composable () -> Unit)?
    var changelog / website / privacy / terms / feedback / licenses: String?
    var links: List<AboutLink>?                 // 自定顺序或含 Custom 时用，给了就忽略上面六个
    var onLinkClick: ((AboutLink) -> Unit)?     // 不给用系统浏览器
    fun checkUpdates(status: @Composable () -> UpdateStatus, onClick: () -> Unit)
}
class WhatsNewConfig { var bundled: (suspend () -> String?)?; var language: (() -> String)?; var enabled: (@Composable () -> Boolean)?  /* false 时本次不弹，如强更门亮着 */ }
class TopBarConfig { var containerColor: (@Composable () -> Color)? }
```

## 页面

```kotlin
/** 设置页框架：壳、滚动、间距由库给，组与行在 content 里摆；通用行的点击经 onOpen 交给宿主导航。 */
@Composable fun SettingsScreen(onBack: () -> Unit, onOpen: (SettingsPage) -> Unit, modifier: Modifier = Modifier, title: String? = null, snackbarHost: @Composable () -> Unit = {}, content: @Composable ColumnScope.() -> Unit)
@Composable fun AppearanceScreen(onBack: () -> Unit, onOpenColorLab: () -> Unit = {}, modifier: Modifier = Modifier)
@Composable fun ColorLabScreen(onBack: () -> Unit, modifier: Modifier = Modifier)
@Composable fun AboutScreen(onBack: () -> Unit, modifier: Modifier = Modifier, onVersionClick: (() -> Unit)? = null, extraItems: (AboutGroupScope.() -> Unit)? = null)

enum class SettingsPage { Appearance, About }     // wang.harlon.appsettings.ui

/** 通用行，可插进任何 SettingsGroup；onClick 不给就走 SettingsScreen 的 onOpen。 */
fun SettingsGroupScope.appearanceItem(onClick: (() -> Unit)? = null)
fun SettingsGroupScope.aboutItem(onClick: (() -> Unit)? = null)
@Composable fun GeneralSettingsGroup(modifier: Modifier = Modifier, title: String? = "通用")
```

## 设置 UI 基元（`wang.harlon.appsettings.ui`）

```kotlin
@Composable fun SettingsGroup(modifier: Modifier = Modifier, title: String? = null, content: SettingsGroupScope.() -> Unit)
class SettingsGroupScope { fun settingsItem(icon: ImageVector? = null, leading: (@Composable () -> Unit)? = null, title: @Composable () -> Unit, description: (@Composable () -> Unit)? = null, enabled: Boolean = true, onClick: (() -> Unit)? = null, trailing: (@Composable () -> Unit)? = null) }
object SettingsDefaults { LargeCorner, SmallCorner, ItemGap, IconContainerSize, IconSize, ContentHorizontalPadding, DisabledAlpha; fun groupItemShape(index: Int, count: Int): Shape }
@Composable fun InfoDialog(title: String, content: String, onDismiss: () -> Unit, confirmText: String? = null)
@Composable fun SettingsBottomSheet(onDismissRequest: () -> Unit, modifier: Modifier = Modifier, title: String? = null, sheetState: SheetState = …, titleAction: (@Composable () -> Unit)? = null, content: @Composable ColumnScope.() -> Unit)
@Composable fun SettingsDropdownMenu(expanded: Boolean, onDismissRequest: () -> Unit, modifier: Modifier = Modifier, containerColor: Color = …, tonalElevation: Dp = …, content: @Composable ColumnScope.() -> Unit)
```

## 模型与事件

```kotlin
// appearance
enum class ThemeMode { FollowSystem, Light, Dark, Amoled }   @Composable fun ThemeMode.label(): String
enum class ThemeStyle(storageValue, paletteStyle)            enum class ThemeContrast(storageValue, level)
data class CustomColor(argb, style, contrast)
sealed interface ColorChoice { Preset(id); Custom(color); SystemDynamic }
data class ThemePreset(id, argb, style, label: @Composable () -> String)   val DefaultPalette; const val DefaultSeedArgb
sealed interface ColorSource { Seed; Fixed(light, dark) }
data class AppIconOption(id, androidAlias: String?, iosIconName: String?, label: @Composable () -> String, preview: @Composable () -> Unit)
sealed interface AppearanceEvent : AppSettingsEvent { ModeChanged(mode); ColorSelected(choice); ColorLabOpened; CustomApplied(color); AppIconChanged(id); AppIconChangeFailed(id, cause) }

// about
sealed interface AboutLink { Privacy(url); Terms(url); Website(url); Changelog(url); Feedback(url); Licenses(url); Custom(url, key, label, icon) }
sealed interface UpdateStatus { Idle; Checking; UpToDate; Failed; Available(version) }
class AboutGroupScope { fun link(link: AboutLink, title: (@Composable () -> String)? = null, icon: ImageVector? = null); fun updateCheck(status: UpdateStatus, onClick: () -> Unit); fun item(…同 settingsItem…) }

// whatsnew
data class WhatsNewInfo(version, notes: Map<String, List<String>>) { isEmpty; fun pick(language): List<String> }
fun parseWhatsNew(raw: String): WhatsNewInfo?
fun shouldShowWhatsNew(currentVersion, bundledVersion, lastSeenVersion): Boolean
fun isWhatsNewPending(settings: Settings, currentVersion: String): Boolean   // 同一次启动避开双弹窗
sealed interface WhatsNewEvent : AppSettingsEvent { Shown(version); Dismissed(version) }

interface AppSettingsEvent   // 根是普通接口（sealed 子类须同包），各包子树各自 sealed
```

## 稳定契约

- 存储：`appsettings.<包名>.` 命名空间、各 JSON 的 `schemaVersion` 与字段名
- `ThemeMode` 名称、内置预设 `id`
- `AboutLink`、`AppSettingsEvent` 子类集合（新增不算破坏）
- `SettingsPage` 取值

## v1.x（未实现，签名草案）

设计与取舍见 [design.md](design.md) 第 11 节。接法沿用：配置块给了才启用，页面与行由库给。

```kotlin
// 根上新增两个配置块
class AppSettingsConfigScope {
    fun update(block: UpdateConfig.() -> Unit)
    fun language(block: LanguageConfig.() -> Unit)
}
class AppSettings {
    val language: LanguageInfo            // current: AppLanguage；resolved(): String
}

// update：强更门。给了 minVersion 才启用；门在 AppSettingsRoot 里、content 外层，亮着时更新说明不弹
class UpdateConfig {
    var minVersion: (suspend () -> String?)?        // 冷启动调一次；null / 异常不改缓存
    var storeUrl: String?                           // 按钮默认 openUrl(storeUrl)
    var onUpdateClick: (() -> Unit)?                // 给了就替代 storeUrl
    var title / message / button: (@Composable () -> String)?
}
fun isVersionBlocked(current: String, minVersion: String?): Boolean   // 只比 -/+ 前的数值段，解析失败不拦
sealed interface UpdateEvent : AppSettingsEvent { ForceUpdateShown(current, min); ForceUpdateClicked }

// language：界面语言。给了 languages 才启用；Android 走 AppCompatDelegate.setApplicationLocales，iOS 行点按跳系统设置
class LanguageConfig {
    var languages: List<String>                     // BCP 47 tag，顺序即菜单顺序；「跟随系统」固定第一
    var label: (@Composable (tag: String) -> String)?   // 默认该语言自己的显示名
}
data class AppLanguage(val tag: String?)            // null 为跟随系统
fun SettingsGroupScope.languageItem()                // 同 appearanceItem：Android 行内下拉选择，iOS 点按跳系统设置
sealed interface LanguageEvent : AppSettingsEvent { Changed(tag: String?) }
```

存储：`appsettings.update.minVersion`、`appsettings.language.tag`（`"system"` 为跟随系统），都是单值 string。
