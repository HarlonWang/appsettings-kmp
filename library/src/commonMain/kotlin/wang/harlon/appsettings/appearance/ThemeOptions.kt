package wang.harlon.appsettings.appearance

import androidx.compose.runtime.Composable
import com.materialkolor.PaletteStyle
import org.jetbrains.compose.resources.stringResource
import wang.harlon.appsettings.resources.Res
import wang.harlon.appsettings.resources.appsettings_color_lab_contrast_high
import wang.harlon.appsettings.resources.appsettings_color_lab_contrast_medium
import wang.harlon.appsettings.resources.appsettings_color_lab_contrast_standard
import wang.harlon.appsettings.resources.appsettings_color_lab_style_bold
import wang.harlon.appsettings.resources.appsettings_color_lab_style_faithful
import wang.harlon.appsettings.resources.appsettings_color_lab_style_mono
import wang.harlon.appsettings.resources.appsettings_color_lab_style_muted
import wang.harlon.appsettings.resources.appsettings_color_lab_style_soft
import wang.harlon.appsettings.resources.appsettings_color_lab_style_vivid

/**
 * 调色台暴露给用户的「风格」档位——把 materialkolor 的算法名翻译成大白话。按 [storageValue] 持久化。
 * 必须覆盖 [DefaultPalette] 用到的全部 [PaletteStyle]（有单测守着），否则从预设进调色台会对不上档。
 */
enum class ThemeStyle(val storageValue: String, val paletteStyle: PaletteStyle) {
    Soft("soft", PaletteStyle.TonalSpot),
    Vivid("vivid", PaletteStyle.Vibrant),
    Bold("bold", PaletteStyle.Expressive),
    Faithful("faithful", PaletteStyle.Fidelity),
    Muted("muted", PaletteStyle.Neutral),
    Mono("mono", PaletteStyle.Monochrome),
    ;

    companion object {
        /** 未知值（降级安装、手改配置）回落 [Soft]。 */
        fun fromStorage(value: String?): ThemeStyle = entries.firstOrNull { it.storageValue == value } ?: Soft

        fun fromPalette(style: PaletteStyle): ThemeStyle = entries.firstOrNull { it.paletteStyle == style } ?: Soft
    }
}

/**
 * 对比度档位。materialkolor 的 contrastLevel 取值域是 -1.0..1.0，只暴露 0 及以上三档——
 * 负值会把配色压得比标准还低，无障碍上没有意义。
 */
enum class ThemeContrast(val storageValue: String, val level: Double) {
    Standard("standard", 0.0),
    Medium("medium", 0.5),
    High("high", 1.0),
    ;

    companion object {
        fun fromStorage(value: String?): ThemeContrast = entries.firstOrNull { it.storageValue == value } ?: Standard
    }
}

@Composable
fun ThemeStyle.label(): String = when (this) {
    ThemeStyle.Soft -> stringResource(Res.string.appsettings_color_lab_style_soft)
    ThemeStyle.Vivid -> stringResource(Res.string.appsettings_color_lab_style_vivid)
    ThemeStyle.Bold -> stringResource(Res.string.appsettings_color_lab_style_bold)
    ThemeStyle.Faithful -> stringResource(Res.string.appsettings_color_lab_style_faithful)
    ThemeStyle.Muted -> stringResource(Res.string.appsettings_color_lab_style_muted)
    ThemeStyle.Mono -> stringResource(Res.string.appsettings_color_lab_style_mono)
}

@Composable
fun ThemeContrast.label(): String = when (this) {
    ThemeContrast.Standard -> stringResource(Res.string.appsettings_color_lab_contrast_standard)
    ThemeContrast.Medium -> stringResource(Res.string.appsettings_color_lab_contrast_medium)
    ThemeContrast.High -> stringResource(Res.string.appsettings_color_lab_contrast_high)
}
