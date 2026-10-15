package wang.harlon.appsettings.appearance

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable

/** 一组自定义配色：seed + 风格 + 对比度。 */
@Immutable
data class CustomColor(
    val argb: Long,
    val style: ThemeStyle = ThemeStyle.Soft,
    val contrast: ThemeContrast = ThemeContrast.Standard,
)

/** 配色来源。非法组合（如「自定义但没有自定义色」）从类型上就不存在。 */
@Immutable
sealed interface ColorChoice {
    /** 预设，按 [id] 识别而不是按色值——预设色值可以随时微调，已选它的用户不会掉到兜底。 */
    data class Preset(val id: String) : ColorChoice

    data class Custom(val color: CustomColor) : ColorChoice

    /** 壁纸取色（Android 12+）。平台不支持时 [AppearanceState.select] 忽略它。 */
    data object SystemDynamic : ColorChoice
}

/**
 * 一个预设配色。[id] 是稳定契约。
 * @param label 名称，lambda 而不是 StringResource，不逼宿主用 Compose Resources
 */
@Immutable
data class ThemePreset(
    val id: String,
    val argb: Long,
    val style: ThemeStyle = ThemeStyle.Soft,
    val label: @Composable () -> String,
)

/** 持久化的全部外观状态。 */
@Immutable
data class Appearance(
    val mode: ThemeMode = ThemeMode.FollowSystem,
    val color: ColorChoice,
    /** 调色台历史，最新在前，长度由 [AppearanceOptions.customHistoryLimit] 限制。 */
    val customHistory: List<CustomColor> = emptyList(),
)

/** 配色从哪来。 */
sealed interface ColorSource {
    /** 由 seed 经 materialkolor 生成（SPEC_2025）。预设、自定义、壁纸取色都在这条路上。 */
    data object Seed : ColorSource

    /**
     * 品牌色写死的 App：宿主给现成的两套 scheme，库只做深浅切换、纯黑变换与切换动画，
     * 配色相关 UI（色卡、调色台入口）不显示。
     */
    data class Fixed(val light: ColorScheme, val dark: ColorScheme) : ColorSource
}

/** 每个 App 的静态差异全部收在这里。 */
@Immutable
internal data class AppearanceOptions(
    val modes: List<ThemeMode> = ThemeMode.entries,
    val colorSource: ColorSource = ColorSource.Seed,
    val palette: List<ThemePreset> = DefaultPalette,
    val defaultColor: ColorChoice = ColorChoice.Preset(DefaultPalette.first().id),
    /** 是否提供调色台（色板末尾的自定义档）。 */
    val allowCustom: Boolean = true,
    /** 是否提供壁纸取色；平台不支持时自动隐藏。 */
    val allowSystemDynamic: Boolean = true,
    val customHistoryLimit: Int = 10,
) {
    init {
        require(modes.isNotEmpty()) { "modes 不能为空" }
        require(palette.isNotEmpty() || colorSource is ColorSource.Fixed) { "Seed 源至少要一个预设" }
    }

    /** 默认模式：有「跟随系统」就用它，否则取第一个。 */
    val defaultMode: ThemeMode get() = if (ThemeMode.FollowSystem in modes) ThemeMode.FollowSystem else modes.first()

    fun preset(id: String): ThemePreset? = palette.firstOrNull { it.id == id }
}
