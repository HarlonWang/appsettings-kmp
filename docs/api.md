# API 签名（v1 草案）

设计见 [design.md](design.md)。这里只放公开签名与注释，用来在动手前检验 API；实现时以此为准，偏离要回来改这里。
`// v1.x` 标注的部分先不实现。

## 事件根

```kotlin
package wang.harlon.appsettings

/** 所有包的事件根。宿主写一个 `when` 映射到自己的埋点词汇表。 */
sealed interface AppSettingsEvent
```

## ui

```kotlin
package wang.harlon.appsettings.ui

/**
 * 设置项分组：一组连体卡片。首尾大圆角、中间小圆角、彼此留缝，分组之间靠圆角与 primary 色小标题分隔，不用分隔线。
 * `content` 不是 @Composable：先收集再渲染，收齐才知道总数、才能分配圆角。
 */
@Composable
fun SettingsGroup(
    modifier: Modifier = Modifier,
    title: String? = null,
    content: SettingsGroupScope.() -> Unit,
)

class SettingsGroupScope internal constructor() {
    /** [icon] 进 40dp 圆形容器；头像等自带形状的前导元素走 [leading]，优先于 [icon]。无 [onClick] 的行不带按钮语义。 */
    fun settingsItem(
        icon: ImageVector? = null,
        leading: (@Composable () -> Unit)? = null,
        title: @Composable () -> Unit,
        description: (@Composable () -> Unit)? = null,
        enabled: Boolean = true,
        onClick: (() -> Unit)? = null,
        trailing: (@Composable () -> Unit)? = null,
    )
}

/** 规格常量。先不做成可覆盖的 token，两个宿主要的是同一套；以后要开放时加 CompositionLocal 不破坏兼容。 */
object SettingsDefaults {
    val LargeCorner = 24.dp
    val SmallCorner = 6.dp
    val ItemGap = 4.dp
    val IconContainerSize = 40.dp
    val IconSize = 24.dp
    val ContentHorizontalPadding = 16.dp
    const val DisabledAlpha = 0.38f
}

/** 纯说明弹窗：标题 + 正文 + 一个「知道了」。要第二个动作的说明弹窗不适用，自己拼 AlertDialog。 */
@Composable
fun InfoDialog(
    title: String,
    content: String,
    onDismiss: () -> Unit,
    confirmText: String = stringResource(Res.string.appsettings_ok),
)

/** 底部浮层：固定标题字号 titleLarge、水平边距 24dp、底部 navigationBarsPadding + 16dp。内容区不自带滚动。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    sheetState: SheetState = rememberModalBottomSheetState(),
    titleAction: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
)

/** 下拉菜单：固定 24dp 圆角，与卡片组同一套大圆角语言。 */
@Composable
fun SettingsDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MenuDefaults.containerColor,
    tonalElevation: Dp = MenuDefaults.TonalElevation,
    content: @Composable ColumnScope.() -> Unit,
)
```

TrendingAI 侧 `TrendingBottomSheet` / `TrendingDropdownMenu` 可以保留为一行 typealias 或直接换 import。

## whatsnew

存储：`appsettings.whatsnew.lastSeen`（String）。

JSON 文件格式与两个宿主现有的 `whatsnew.json` 兼容：`version` 之外的每个 key 都是一种语言。

```kotlin
package wang.harlon.appsettings.whatsnew

data class WhatsNewInfo(
    val version: String,
    /** 语言码 → 条目；如 "zh" / "en"。 */
    val notes: Map<String, List<String>>,
) {
    val isEmpty: Boolean
    /** 精确命中 → 语言前缀命中 → "en" → 第一个非空；全空返回空表。 */
    fun pick(language: String): List<String>
}

fun parseWhatsNew(raw: String): WhatsNewInfo?

/** 弹出条件：bundled 版本等于当前版本、lastSeen 低于当前版本。占位 0.0.0 与 dev 构建不弹。 */
fun shouldShowWhatsNew(current: String, bundledVersion: String, lastSeen: String?): Boolean

sealed interface WhatsNewEvent : AppSettingsEvent {
    data class Shown(val version: String) : WhatsNewEvent
    data class Dismissed(val version: String) : WhatsNewEvent
}

@Stable
class WhatsNewState internal constructor() {
    /** 待弹的更新说明；null 表示本次启动没有要弹的。宿主可据此延后其他弹窗。 */
    val pending: WhatsNewInfo?
    fun dismiss()
}

/**
 * 首装静默记录当前版本不弹；升级后首启且 bundled 版本匹配才弹；不满足条件直接标记已看，避免一直 pending。
 * 不读宿主的旧 key：从旧实现迁过来的 App，老用户升级那一次按首装处理、少弹一回，已接受。
 * @param load 读取随包的 JSON 文本，宿主通常是 `Res.readBytes("files/whatsnew.json").decodeToString()`
 */
@Composable
fun rememberWhatsNewState(
    settings: Settings,
    currentVersion: String,
    load: suspend () -> String?,
    onEvent: (WhatsNewEvent) -> Unit = {},
): WhatsNewState

/** 挂在根上：有 pending 就弹。[language] 由宿主给（界面语言或系统语言）。 */
@Composable
fun WhatsNewHost(state: WhatsNewState, language: String)

/** 弹窗本体，想自己控制时机就直接用。 */
@Composable
fun WhatsNewDialog(version: String, items: List<String>, onDismiss: () -> Unit)
```

## about

无持久化，无 State。出口只有 `onLinkClick` 与 `updateCheck` 的 `onClick`。

```kotlin
package wang.harlon.appsettings.about

/** 内置链接类型自带中英文标题与图标；[Custom] 自己给。宿主在 onLinkClick 里决定 App 内打开还是系统浏览器，顺便埋点。 */
sealed interface AboutLink {
    val url: String
    data class Privacy(override val url: String) : AboutLink
    data class Terms(override val url: String) : AboutLink
    data class Website(override val url: String) : AboutLink
    data class Changelog(override val url: String) : AboutLink
    data class Feedback(override val url: String) : AboutLink
    data class Licenses(override val url: String) : AboutLink
    data class Custom(
        override val url: String,
        /** 宿主埋点用的稳定标识。 */
        val key: String,
        val label: @Composable () -> String,
        val icon: ImageVector? = null,
    ) : AboutLink
}

/** 只负责渲染更新行的 trailing 与可点性，检查逻辑在宿主（渠道相关）。 */
sealed interface UpdateStatus {
    data object Idle : UpdateStatus
    /** trailing 显示 LoadingIndicator，行不可点。 */
    data object Checking : UpdateStatus
    data object UpToDate : UpdateStatus
    data object Failed : UpdateStatus
    data class Available(val version: String) : UpdateStatus
}

/**
 * 关于页整页内容，自带滚动。头部为应用名 + 版本号 + 可选图标；[onVersionClick] 给隐藏开发者入口计数用，计数在宿主。
 * [onLinkClick] 默认用 LocalUriHandler 打开，即系统浏览器。
 */
@Composable
fun AboutContent(
    name: String,
    version: String,
    modifier: Modifier = Modifier,
    icon: (@Composable () -> Unit)? = null,
    onVersionClick: (() -> Unit)? = null,
    onLinkClick: (AboutLink) -> Unit = AboutDefaults.openInBrowser(),
    content: AboutScope.() -> Unit,
)

class AboutScope internal constructor() {
    fun group(title: String? = null, content: AboutGroupScope.() -> Unit)
}

class AboutGroupScope internal constructor() {
    /** 内置类型可覆盖标题与图标。 */
    fun link(link: AboutLink, title: (@Composable () -> String)? = null, icon: ImageVector? = null)
    /** 检查更新行。[Checking] 时自动不可点。 */
    fun updateCheck(status: UpdateStatus, onClick: () -> Unit)
    /** 任意自定义行，签名同 SettingsGroupScope.settingsItem。赞助、调试开关这类 App 私有行走这里。 */
    fun item(
        icon: ImageVector? = null,
        leading: (@Composable () -> Unit)? = null,
        title: @Composable () -> Unit,
        description: (@Composable () -> Unit)? = null,
        enabled: Boolean = true,
        onClick: (() -> Unit)? = null,
        trailing: (@Composable () -> Unit)? = null,
    )
}

/** 头部单独可用，宿主想自己排版时拿它拼。 */
@Composable
fun AboutHeader(
    name: String,
    version: String,
    modifier: Modifier = Modifier,
    icon: (@Composable () -> Unit)? = null,
    onVersionClick: (() -> Unit)? = null,
)
```

## appearance

存储：`appsettings.appearance.state`，一个 JSON，`schemaVersion = 1`。动态图标不持久化（真实状态在系统里）。

### 模型

```kotlin
package wang.harlon.appsettings.appearance

/** 按名称存储。Amoled 是 Dark 的变体：同一套配色，background / surface 压成纯黑。 */
enum class ThemeMode { FollowSystem, Light, Dark, Amoled }

/** 映射 materialkolor 的 PaletteStyle，按 storageValue 存。 */
enum class ThemeStyle(val storageValue: String) { Soft, Vibrant, Expressive, Neutral, Monochrome, /* ... */ }

enum class ThemeContrast(val storageValue: String, val level: Double) { Standard, Medium, High }

data class CustomColor(
    val argb: Long,
    val style: ThemeStyle = ThemeStyle.Soft,
    val contrast: ThemeContrast = ThemeContrast.Standard,
)

/** 非法状态写不出来：「自定义但没有自定义色」从类型上不存在。 */
sealed interface ColorChoice {
    /** 按 id 识别，不按色值；预设色值可以随时调，已选它的用户不会掉到兜底。 */
    data class Preset(val id: String) : ColorChoice
    data class Custom(val color: CustomColor) : ColorChoice
    /** Android 12+ 壁纸取色；平台不支持时 State 会拒绝选中并回落默认。 */
    data object SystemDynamic : ColorChoice
}

data class ThemePreset(
    val id: String,
    val argb: Long,
    val style: ThemeStyle = ThemeStyle.Soft,
    val label: @Composable () -> String,
)

/** 库内置的预设配色（TrendingAI 现有的那组，id 稳定）。 */
val DefaultPalette: List<ThemePreset>

/** 持久化的全部外观状态。 */
data class Appearance(
    val mode: ThemeMode = ThemeMode.FollowSystem,
    val color: ColorChoice,
    /** 调色台历史，最新在前，长度由 Options 限制。 */
    val customHistory: List<CustomColor> = emptyList(),
)

sealed interface ColorSource {
    /** 由 seed 经 materialkolor 生成，SPEC_2025。 */
    data object Seed : ColorSource
    /** 品牌色写死的 App：宿主给现成的两套 scheme，库只做深浅切换、纯黑变换与切换动画。配色相关 UI 不显示。 */
    data class Fixed(val light: ColorScheme, val dark: ColorScheme) : ColorSource
}

data class AppearanceOptions(
    val modes: List<ThemeMode> = ThemeMode.entries,
    val colorSource: ColorSource = ColorSource.Seed,
    val palette: List<ThemePreset> = DefaultPalette,
    val defaultColor: ColorChoice = ColorChoice.Preset(DefaultPalette.first().id),
    /** 是否显示调色台入口。 */
    val allowCustom: Boolean = true,
    /** 是否提供壁纸取色；平台不支持时自动隐藏。 */
    val allowSystemDynamic: Boolean = true,
    val customHistoryLimit: Int = 10,
)
```

### 存储与状态

```kotlin
interface AppearanceStore {
    val appearance: StateFlow<Appearance>
    fun update(transform: (Appearance) -> Appearance)
}

/** 默认实现：一个 key 存一份带 schemaVersion 的 JSON。读不出来（新装、损坏、未来版本）回落 Options 的默认值。 */
fun AppearanceStore(
    settings: Settings,
    options: AppearanceOptions,
    key: String = "appsettings.appearance.state",
): AppearanceStore

sealed interface AppearanceEvent : AppSettingsEvent {
    data class ModeChanged(val mode: ThemeMode) : AppearanceEvent
    data class ColorSelected(val choice: ColorChoice) : AppearanceEvent
    data object ColorLabOpened : AppearanceEvent
    /** 离开调色台时记一次最终值，拖动过程中不记。 */
    data class CustomApplied(val color: CustomColor) : AppearanceEvent
    data class AppIconChanged(val id: String) : AppearanceEvent
    data class AppIconChangeFailed(val id: String, val cause: Throwable) : AppearanceEvent
}

@Stable
class AppearanceState internal constructor() {
    val options: AppearanceOptions
    /** Compose State 支撑，同步可读。 */
    val appearance: Appearance

    fun setMode(mode: ThemeMode)
    /** 选预设 / 壁纸取色 / 回到上次的自定义色。SystemDynamic 在不支持的平台上无效。 */
    fun select(choice: ColorChoice)
    /** 调色台实时落盘用；不入历史。 */
    fun applyCustom(color: CustomColor)

    /** 深浅判断的唯一来源，FollowSystem 时读系统。 */
    @Composable fun isDark(): Boolean
    /** 平台是否支持壁纸取色。 */
    val supportsSystemDynamic: Boolean
}

@Composable
fun rememberAppearanceState(
    settings: Settings,
    options: AppearanceOptions = AppearanceOptions(),
    onEvent: (AppearanceEvent) -> Unit = {},
): AppearanceState

/** 自带 store 的重载，测试与特殊存储用。 */
@Composable
fun rememberAppearanceState(
    store: AppearanceStore,
    options: AppearanceOptions = AppearanceOptions(),
    onEvent: (AppearanceEvent) -> Unit = {},
): AppearanceState

val LocalAppearanceState: ProvidableCompositionLocal<AppearanceState>
```

### 主题

```kotlin
/**
 * 第一档：挂在根上，等价于 MaterialTheme。typography / shapes 透传。
 * Seed 源走 materialkolor 的 DynamicMaterialTheme（动画 600ms FastOutSlowIn）；Fixed 源由库做纯黑变换与 animateColorAsState。
 * 内部 Provide LocalAppearanceState。
 */
@Composable
fun AppearanceTheme(
    state: AppearanceState,
    typography: Typography = MaterialTheme.typography,
    shapes: Shapes = MaterialTheme.shapes,
    animate: Boolean = true,
    content: @Composable () -> Unit,
)

/** 第二档：只要算好的 scheme（不带动画），主题有自己写法的宿主用。 */
@Composable
fun rememberAppearanceColorScheme(state: AppearanceState): ColorScheme

// androidMain
/** edge-to-edge 下按 isDark 设置状态栏 / 导航栏前景，替代宿主在 Activity 里重抄的那段 when。 */
@Composable
fun SystemBarsEffect(state: AppearanceState)
```

### 设置 UI

```kotlin
/**
 * 外观页整页内容，自带滚动：模式卡片 → 配色网格（Seed 源才有）→ 动态图标（icons 非 null 才有）→ footer。
 * 调色台是独立页面，进入由宿主导航。
 */
@Composable
fun AppearanceContent(
    state: AppearanceState,
    modifier: Modifier = Modifier,
    onOpenColorLab: () -> Unit = {},
    icons: AppIconState? = null,
    footer: (@Composable ColumnScope.() -> Unit)? = null,
)

@Composable fun ThemeModePicker(state: AppearanceState, modifier: Modifier = Modifier)
@Composable fun PalettePicker(state: AppearanceState, onOpenColorLab: () -> Unit, modifier: Modifier = Modifier)

/**
 * 调色台会话：进入时拍快照，实时生效；[undo] 整体写回快照，[commit] 记一条历史并发 CustomApplied。
 * 宿主把 undo 接到顶栏按钮，把 commit 接到离开页面。
 */
@Stable
class ColorLabSession internal constructor() {
    val draft: CustomColor
    fun setHue(hue: Float); fun setSaturation(s: Float); fun setValue(v: Float)
    fun setArgb(argb: Long); fun setStyle(style: ThemeStyle); fun setContrast(contrast: ThemeContrast)
    val isDirty: Boolean
    fun undo()
    fun commit()
}

@Composable fun rememberColorLabSession(state: AppearanceState): ColorLabSession

/** 调色台整页内容，自带滚动：预览、HSV 滑杆、hex 输入、风格、对比度、历史。 */
@Composable fun ColorLabContent(session: ColorLabSession, modifier: Modifier = Modifier)
```

### 动态图标

```kotlin
data class AppIconOption(
    val id: String,
    /** 完整 activity-alias 类名；null 表示该档在 Android 不可用。 */
    val androidAlias: String?,
    /** iOS 备用图标名；null 表示主图标。 */
    val iosIconName: String?,
    val label: @Composable () -> String,
    /** 预览怎么画由宿主决定（合成的、位图都行）；库只负责选中环、缩放动画、标签与无障碍语义。 */
    val preview: @Composable () -> Unit,
)

@Stable
class AppIconState internal constructor() {
    val options: List<AppIconOption>
    /** 平台是否支持切换；不支持时 Picker 不显示。 */
    val isSupported: Boolean
    /** 当前生效的 id，读自系统真实状态，不另存。 */
    val current: String
    /** 保证同时只启用一个 alias，单个失败不连累其余；失败发 AppIconChangeFailed。 */
    fun select(id: String)
}

/**
 * debug 包首次调用时校验每个 androidAlias 存在、且有且只有一个默认启用，不满足抛 IllegalStateException 并给出 manifest 写法；
 * release 只打日志。
 */
@Composable
fun rememberAppIconState(
    options: List<AppIconOption>,
    onEvent: (AppearanceEvent) -> Unit = {},
): AppIconState

@Composable fun AppIconPicker(icons: AppIconState, modifier: Modifier = Modifier)
```

## update  // v1.x

```kotlin
package wang.harlon.appsettings.update

/** 语义化版本比较，忽略 -suffix 与 build metadata。 */
fun isVersionBlocked(current: String, minVersion: String?): Boolean

/** 当前版本低于 [minVersion] 时整屏替换 [content]，没有 dismiss 语义。minVersion 怎么拉、怎么缓存是宿主的事。 */
@Composable
fun ForceUpdateGate(
    currentVersion: String,
    minVersion: String?,
    onOpenStore: () -> Unit,
    onShown: () -> Unit = {},
    content: @Composable () -> Unit,
)
```

## language  // v1.x

```kotlin
package wang.harlon.appsettings.language

/** null 表示跟随系统。 */
data class AppLanguage(val tag: String?)

sealed interface LanguageEvent : AppSettingsEvent {
    data class Changed(val tag: String?) : LanguageEvent
}

@Stable
class AppLanguageState internal constructor() {
    val current: AppLanguage
    val options: List<AppLanguage>
    /** Android 走 per-app locales；iOS 不可在 App 内切，调用会打开系统设置。 */
    fun select(language: AppLanguage)
    val canChangeInApp: Boolean
}

@Composable
fun rememberAppLanguageState(
    settings: Settings,
    options: List<AppLanguage>,
    onEvent: (LanguageEvent) -> Unit = {},
): AppLanguageState

/** 设置页的一行：trailing 是当前值 + 下拉；iOS 显示「打开系统设置」。 */
fun SettingsGroupScope.languageItem(state: AppLanguageState)
```
