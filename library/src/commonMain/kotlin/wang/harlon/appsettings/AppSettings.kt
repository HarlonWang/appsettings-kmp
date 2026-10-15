package wang.harlon.appsettings

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.russhwolf.settings.Settings
import wang.harlon.appsettings.about.AboutLink
import wang.harlon.appsettings.about.UpdateStatus
import wang.harlon.appsettings.appearance.AppIconOption
import wang.harlon.appsettings.appearance.AppIconState
import wang.harlon.appsettings.appearance.AppearanceEvent
import wang.harlon.appsettings.appearance.AppearanceOptions
import wang.harlon.appsettings.appearance.AppearanceState
import wang.harlon.appsettings.appearance.AppearanceTheme
import wang.harlon.appsettings.appearance.ColorChoice
import wang.harlon.appsettings.appearance.ColorSource
import wang.harlon.appsettings.appearance.DefaultPalette
import wang.harlon.appsettings.appearance.ThemeMode
import wang.harlon.appsettings.appearance.ThemePreset
import wang.harlon.appsettings.appearance.rememberAppIconState
import wang.harlon.appsettings.appearance.rememberAppearanceState
import wang.harlon.appsettings.language.AppLanguage
import wang.harlon.appsettings.language.LanguageState
import wang.harlon.appsettings.language.PlatformApplyLanguageEffect
import wang.harlon.appsettings.language.rememberLanguageState
import wang.harlon.appsettings.whatsnew.WhatsNewHost
import wang.harlon.appsettings.whatsnew.WhatsNewState
import wang.harlon.appsettings.whatsnew.rememberWhatsNewState

/**
 * 唯一的配置入口。在根上调一次，拿到的 [AppSettings] 交给 [AppSettingsRoot]，之后所有页面与行都从它取东西。
 *
 * ```
 * val appSettings = rememberAppSettings {
 *     about { name = "My App"; privacy = PRIVACY_URL; terms = TERMS_URL }
 *     whatsNew { bundled = { Res.readBytes("files/whatsnew.json").decodeToString() } }
 * }
 * AppSettingsRoot(appSettings) { App() }
 * ```
 *
 * 不写的块走默认：外观默认开（全套预设 + 调色台 + 壁纸取色），关于页默认只有应用名与版本号，更新说明与界面语言默认不启用。
 * 配置块（含 about { } 等子块）都是 @Composable，里面可以直接 stringResource、读 Compose 状态；每次组合重新求值，但 [AppSettingsConfigScope.settings] 与
 * [AppearanceConfig] 里的结构性选项只在第一次组合时生效。
 */
@Composable
fun rememberAppSettings(block: @Composable AppSettingsConfigScope.() -> Unit = {}): AppSettings {
    val config = AppSettingsConfigScope().apply { block() }
    val settings = remember { config.settings ?: Settings() }
    val options = remember { config.appearance.toOptions() }
    val appearance = rememberAppearanceState(settings, options)
    val icons = remember { config.appearance.appIcons }.takeIf { it.isNotEmpty() }?.let { rememberAppIconState(it) }
    val platformInfo = rememberPlatformAppInfo()
    val whatsNew = remember { config.whatsNew.bundled }?.let { bundled ->
        rememberWhatsNewState(settings, currentVersion = config.appVersion ?: platformInfo.version, load = bundled)
    }
    val language = remember { config.language.languages }.takeIf { it.isNotEmpty() }?.let { rememberLanguageState(settings, it) }
    val appSettings = remember { AppSettings(appearance, icons, whatsNew, language) }
    // 配置可以每次组合都变（读了状态的 lambda），状态对象不重建，只把最新配置挂上去
    SideEffect {
        appSettings.config = config
        appSettings.platformInfo = platformInfo
        appearance.onEvent = config.onEvent
        icons?.onEvent = config.onEvent
        whatsNew?.onEvent = config.onEvent
        language?.onEvent = config.onEvent
    }
    return appSettings
}

/** 挂在根上：主题（深浅 / 配色 / 切换动画 / 系统栏）+ 界面语言应用 + 更新说明弹窗。等价于 MaterialTheme，typography / shapes 透传。 */
@Composable
fun AppSettingsRoot(
    appSettings: AppSettings,
    typography: Typography = MaterialTheme.typography,
    shapes: Shapes = MaterialTheme.shapes,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalAppSettings provides appSettings) {
        appSettings.languageState?.let { PlatformApplyLanguageEffect(it.current) }
        AppearanceTheme(
            state = appSettings.appearanceState,
            typography = typography,
            shapes = shapes,
            animate = appSettings.config.appearance.animate,
            syncSystemBars = appSettings.config.appearance.syncSystemBars,
        ) {
            content()
            appSettings.whatsNewState?.let { state ->
                val enabled = appSettings.config.whatsNew.enabled?.invoke() ?: true
                if (enabled) {
                    // 展示语言：显式配置 > 界面语言（启用了 language 块）> 系统语言
                    val language = appSettings.config.whatsNew.language?.invoke()
                        ?: appSettings.languageState?.resolved()
                        ?: platformSystemLanguage()
                    WhatsNewHost(state, language = language)
                }
            }
        }
    }
}

/** 库的全部运行时状态。宿主能读的只有少数几个值，其余是页面与行自己用的。 */
@Stable
class AppSettings internal constructor(
    internal val appearanceState: AppearanceState,
    internal val appIconState: AppIconState?,
    internal val whatsNewState: WhatsNewState?,
    internal val languageState: LanguageState?,
) {
    internal var config: AppSettingsConfigScope = AppSettingsConfigScope()
    internal var platformInfo: PlatformAppInfo = PlatformAppInfo("", "")

    val appearance: AppearanceInfo = AppearanceInfo()
    val language: LanguageInfo = LanguageInfo()

    /** 外观里宿主偶尔要读的值。 */
    inner class AppearanceInfo internal constructor() {
        /** 当前深浅模式。设置页入口的 trailing 由 `appearanceItem()` 自己显示，一般不用读它。 */
        val mode: ThemeMode get() = appearanceState.appearance.mode

        /** 当前是否深色（含跟随系统的解析结果）。 */
        @Composable
        fun isDark(): Boolean = appearanceState.isDark()
    }

    /** 界面语言里宿主要读的值。没配 `language { }` 时 [current] 恒为跟随系统，[resolved] 返回系统语言。 */
    inner class LanguageInfo internal constructor() {
        /** 用户的选择；null tag 即跟随系统。 */
        val current: AppLanguage get() = languageState?.current ?: AppLanguage.System

        /** 实际生效的语言标签：跟随系统时取系统语言并钳到清单。拼外链的 locale 参数、给服务端的 Accept-Language 都读它。 */
        fun resolved(): String = languageState?.resolved() ?: platformSystemLanguage()
    }

    internal val aboutName: String get() = config.about.name ?: platformInfo.name
    internal val aboutVersion: String get() = config.about.version ?: config.appVersion ?: platformInfo.version
}

internal val LocalAppSettings = compositionLocalOf<AppSettings> {
    error("appsettings-kmp：没有找到 AppSettings。把 AppSettingsRoot(rememberAppSettings { … }) 挂在 App 根上，页面与行都要在它里面。")
}

// ---- 配置 ----

class AppSettingsConfigScope internal constructor() {
    /** 宿主已有的 multiplatform-settings 实例；不给就由库自建一个（只写 `appsettings.*` 命名空间的 key）。 */
    var settings: Settings? = null

    /** 当前版本号；不给就读平台（Android PackageInfo / iOS CFBundleShortVersionString）。 */
    var appVersion: String? = null

    /** 所有事件的出口，宿主映射到自己的埋点；不接也可以。 */
    var onEvent: (AppSettingsEvent) -> Unit = {}

    internal val appearance = AppearanceConfig()
    internal val about = AboutConfig()
    internal val whatsNew = WhatsNewConfig()
    internal val language = LanguageConfig()
    internal val topBar = TopBarConfig()

    @Composable
    fun appearance(block: @Composable AppearanceConfig.() -> Unit) = appearance.block()
    @Composable
    fun about(block: @Composable AboutConfig.() -> Unit) = about.block()
    @Composable
    fun whatsNew(block: @Composable WhatsNewConfig.() -> Unit) = whatsNew.block()
    @Composable
    fun language(block: @Composable LanguageConfig.() -> Unit) = language.block()
    @Composable
    fun topBar(block: @Composable TopBarConfig.() -> Unit) = topBar.block()
}

/** 外观。结构性选项（色源、预设、模式集合）只在第一次组合时生效。 */
class AppearanceConfig internal constructor() {
    var modes: List<ThemeMode> = ThemeMode.entries
    /** 品牌色写死的 App 传 [ColorSource.Fixed]，色板与调色台随之隐藏。 */
    var colorSource: ColorSource = ColorSource.Seed
    var palette: List<ThemePreset> = DefaultPalette
    var defaultColor: ColorChoice = ColorChoice.Preset(DefaultPalette.first().id)
    var allowCustom: Boolean = true
    var allowSystemDynamic: Boolean = true
    var customHistoryLimit: Int = 10
    /** 动态图标档位；空表示不提供这一节。alias 声明在宿主 manifest，见 README。 */
    var appIcons: List<AppIconOption> = emptyList()
    var animate: Boolean = true
    /** Android 上按深浅同步系统栏前景（edge-to-edge）。 */
    var syncSystemBars: Boolean = true

    internal fun toOptions() = AppearanceOptions(
        modes = modes,
        colorSource = colorSource,
        palette = palette,
        // 宿主只换了 palette 没改 defaultColor 时，默认的 Preset("default") 可能已不在色板里：规范化为第一项
        defaultColor = defaultColor.takeUnless { it is ColorChoice.Preset && palette.none { p -> p.id == it.id } }
            ?: palette.firstOrNull()?.let { ColorChoice.Preset(it.id) } ?: defaultColor,
        allowCustom = allowCustom,
        allowSystemDynamic = allowSystemDynamic,
        customHistoryLimit = customHistoryLimit,
    )
}

/** 关于页。名字与版本不给就读平台；链接给了哪个就出哪一行。 */
class AboutConfig internal constructor() {
    var name: String? = null
    var version: String? = null
    var icon: (@Composable () -> Unit)? = null

    var changelog: String? = null
    var website: String? = null
    var privacy: String? = null
    var terms: String? = null
    var feedback: String? = null
    var licenses: String? = null

    /** 要自己定顺序或加 [AboutLink.Custom] 时给这个，给了就忽略上面六个 url。 */
    var links: List<AboutLink>? = null

    /** 所有链接的统一出口（在 App 内打开、埋点都在这里）；不给就用系统浏览器。 */
    var onLinkClick: ((AboutLink) -> Unit)? = null

    internal var updateCheck: UpdateCheck? = null

    /** 「检查更新」行，放在链接之前。检查逻辑与渠道判断在宿主，[status] 是 Compose 状态驱动的读取。 */
    fun checkUpdates(status: @Composable () -> UpdateStatus, onClick: () -> Unit) {
        updateCheck = UpdateCheck(status, onClick)
    }

    internal class UpdateCheck(val status: @Composable () -> UpdateStatus, val onClick: () -> Unit)

    internal fun resolvedLinks(): List<AboutLink> = links ?: listOfNotNull(
        changelog?.let { AboutLink.Changelog(it) },
        website?.let { AboutLink.Website(it) },
        privacy?.let { AboutLink.Privacy(it) },
        terms?.let { AboutLink.Terms(it) },
        feedback?.let { AboutLink.Feedback(it) },
        licenses?.let { AboutLink.Licenses(it) },
    )
}

/** 更新说明。给了 [bundled] 才启用。 */
class WhatsNewConfig internal constructor() {
    /** 读取随包的 JSON 文本，格式 `{"version":"1.2.0","zh":[…],"en":[…]}`，`version` 之外的 key 都是语言码。 */
    var bundled: (suspend () -> String?)? = null

    /** 展示语言；不给就用系统语言。 */
    var language: (() -> String)? = null

    /** 返回 false 时本次不弹（例如强更门亮着时），下次组合再读；不给就一直允许。待弹状态不会因此丢失。 */
    var enabled: (@Composable () -> Boolean)? = null
}

/**
 * 界面语言。给了 [languages] 才启用；只在第一次组合时生效。
 * Android 要求宿主 Activity 是 AppCompatActivity；iOS 行点按跳系统设置，宿主 Info.plist 要声明 CFBundleLocalizations。见 README。
 */
class LanguageConfig internal constructor() {
    /** 可选语言的 BCP 47 标签，顺序即菜单顺序，「跟随系统」固定第一；宿主翻译了哪些语言只有宿主知道。 */
    var languages: List<String> = emptyList()

    /** 选项文案；不给就用该语言自己的显示名（`zh` → 「中文」，`en` → 「English」）。 */
    var label: (@Composable (tag: String) -> String)? = null
}

/** 库自带页面的顶栏。默认跟 Material 3（`surface`），有自己风格的 App 在这里改一行。 */
class TopBarConfig internal constructor() {
    var containerColor: (@Composable () -> Color)? = null
}

/** 平台读到的应用名与版本号。 */
internal data class PlatformAppInfo(val name: String, val version: String)

@Composable
internal expect fun rememberPlatformAppInfo(): PlatformAppInfo

internal expect fun platformSystemLanguage(): String
