package wang.harlon.appsettings.appearance

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.russhwolf.settings.Settings

/** 外观状态：读是 Compose State（同步初值），写走 [AppearanceStore]，事件从 [onEvent] 出去。 */
@Stable
class AppearanceState internal constructor(
    internal val store: AppearanceStore,
    val options: AppearanceOptions,
    /** 平台是否支持壁纸取色。 */
    val supportsSystemDynamic: Boolean,
) {
    internal var onEvent: (AppearanceEvent) -> Unit = {}

    var appearance: Appearance by mutableStateOf(store.appearance.value)
        internal set

    /** 色板里要不要显示壁纸取色档。 */
    val showsSystemDynamic: Boolean
        get() = options.colorSource is ColorSource.Seed && options.allowSystemDynamic && supportsSystemDynamic

    /**
     * 上次调好的自定义色：正在用的自定义档，或历史里最近一条。外观页色板末尾那颗圆显示它，
     * 点它时先把它应用上再进调色台；null 表示从没调过色。
     */
    val lastCustomColor: CustomColor?
        get() = (appearance.color as? ColorChoice.Custom)?.color ?: appearance.customHistory.firstOrNull()

    fun setMode(mode: ThemeMode) {
        if (mode !in options.modes || mode == appearance.mode) return
        write { it.copy(mode = mode) }
        onEvent(AppearanceEvent.ModeChanged(mode))
    }

    /** 选预设 / 壁纸取色 / 回到自定义色。不支持或不允许的选择直接忽略。 */
    fun select(choice: ColorChoice) {
        val accepted = when (choice) {
            is ColorChoice.Preset -> options.preset(choice.id) != null
            is ColorChoice.Custom -> options.allowCustom
            ColorChoice.SystemDynamic -> showsSystemDynamic
        }
        if (!accepted) return
        write { it.copy(color = choice) }
        onEvent(AppearanceEvent.ColorSelected(choice))
    }

    /** 调色台实时落盘用：只改生效配色，不入历史、不发事件。 */
    fun applyCustom(color: CustomColor) {
        if (!options.allowCustom) return
        write { it.copy(color = ColorChoice.Custom(color)) }
    }

    /** 记一条自定义配置：完全相同的组合提到最前而非重复入列，超出上限截断。 */
    fun pushHistory(color: CustomColor) {
        write { a ->
            a.copy(customHistory = (listOf(color) + a.customHistory.filterNot { it == color }).take(options.customHistoryLimit))
        }
    }

    /** 整体写回某个快照，调色台「撤销修改」用。 */
    fun restore(snapshot: Appearance) {
        write { snapshot }
    }

    /** 深浅判断的唯一来源：FollowSystem 读系统。 */
    @Composable
    fun isDark(): Boolean = when (appearance.mode) {
        ThemeMode.FollowSystem -> isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark, ThemeMode.Amoled -> true
    }

    internal fun emit(event: AppearanceEvent) = onEvent(event)

    private fun write(transform: (Appearance) -> Appearance) {
        store.update(transform)
        appearance = store.appearance.value
    }
}

/**
 * 用宿主已有的 [Settings] 建外观状态，存一个 JSON key（[DefaultAppearanceKey]）。
 * 挂在根上一次，经 [AppearanceTheme] 提供给 [LocalAppearanceState]。
 */
@Composable
fun rememberAppearanceState(
    settings: Settings,
    options: AppearanceOptions = AppearanceOptions(),
    onEvent: (AppearanceEvent) -> Unit = {},
): AppearanceState {
    val store = remember(settings, options) { AppearanceStore(settings, options) }
    return rememberAppearanceState(store, options, onEvent)
}

/** 自带 store 的重载，测试与特殊存储用。 */
@Composable
fun rememberAppearanceState(
    store: AppearanceStore,
    options: AppearanceOptions = AppearanceOptions(),
    onEvent: (AppearanceEvent) -> Unit = {},
): AppearanceState {
    val supportsDynamic = remember { platformSupportsDynamicColor() }
    val state = remember(store, options) { AppearanceState(store, options, supportsDynamic) }
    SideEffect { state.onEvent = onEvent }
    // store 若被别处改写（多实例、调试工具），这里跟上；库自己的写入已在 write() 里同步
    LaunchedEffect(state) { store.appearance.collect { state.appearance = it } }
    return state
}

/** 由 [AppearanceTheme] 提供；在它外面读会抛，提示接线顺序错了。 */
val LocalAppearanceState = compositionLocalOf<AppearanceState> {
    error("LocalAppearanceState 未提供：把 AppearanceTheme 挂在根上")
}
