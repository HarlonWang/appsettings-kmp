package wang.harlon.appsettings.appearance

import androidx.compose.runtime.Composable
import com.materialkolor.hct.Hct
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import wang.harlon.appsettings.resources.Res
import wang.harlon.appsettings.resources.appsettings_theme_color_berry
import wang.harlon.appsettings.resources.appsettings_theme_color_brown
import wang.harlon.appsettings.resources.appsettings_theme_color_default
import wang.harlon.appsettings.resources.appsettings_theme_color_forest
import wang.harlon.appsettings.resources.appsettings_theme_color_graphite
import wang.harlon.appsettings.resources.appsettings_theme_color_indigo
import wang.harlon.appsettings.resources.appsettings_theme_color_mustard
import wang.harlon.appsettings.resources.appsettings_theme_color_olive
import wang.harlon.appsettings.resources.appsettings_theme_color_peacock
import wang.harlon.appsettings.resources.appsettings_theme_color_pine
import wang.harlon.appsettings.resources.appsettings_theme_color_plum
import wang.harlon.appsettings.resources.appsettings_theme_color_rust
import wang.harlon.appsettings.resources.appsettings_theme_color_slate
import wang.harlon.appsettings.resources.appsettings_theme_color_steel

/** 默认预设的 seed（M3 基线紫）。 */
const val DefaultSeedArgb: Long = 0xFF6750A4L

/** 色卡的统一彩度与明度，取自基线紫 `6750A4` 的 HCT 坐标——默认档不必改色值也能融进色板。 */
private const val SWATCH_CHROMA = 48.0
private const val SWATCH_TONE = 44.0

/** 11 个彩色档的色相：以基线紫 298° 为锚，沿色相环均匀铺开（间隔 32.7°）。 */
private const val HUE_STEP = 32.73

private fun hue(index: Int): Double = (298.0 + HUE_STEP * index) % 360.0

/** 按 HCT 坐标生成不透明 seed。 */
private fun seed(hue: Double, chroma: Double = SWATCH_CHROMA, tone: Double = SWATCH_TONE): Long =
    Hct.from(hue, chroma, tone).toInt().toLong() and 0xFFFFFFFFL

private fun preset(id: String, label: StringResource, argb: Long, style: ThemeStyle = ThemeStyle.Soft) =
    ThemePreset(id = id, argb = argb, style = style, label = { stringResource(label) })

/**
 * 内置的 14 色预设：11 个彩色沿色相环连续递增 + 3 档中性收尾，加自定义档正好 5×3 满排。
 * 彩度 / 明度全部归一、只有色相在变，色板才读成一条连续色带。**id 是稳定契约，色值可以调。**
 */
val DefaultPalette: List<ThemePreset> = listOf(
    preset("default", Res.string.appsettings_theme_color_default, DefaultSeedArgb),
    preset("plum", Res.string.appsettings_theme_color_plum, seed(hue(1))),
    preset("berry", Res.string.appsettings_theme_color_berry, seed(hue(2))),
    preset("rust", Res.string.appsettings_theme_color_rust, seed(hue(3))),
    preset("mustard", Res.string.appsettings_theme_color_mustard, seed(hue(4))),
    preset("olive", Res.string.appsettings_theme_color_olive, seed(hue(5))),
    preset("forest", Res.string.appsettings_theme_color_forest, seed(hue(6))),
    preset("pine", Res.string.appsettings_theme_color_pine, seed(hue(7))),
    preset("peacock", Res.string.appsettings_theme_color_peacock, seed(hue(8))),
    preset("steel", Res.string.appsettings_theme_color_steel, seed(hue(9))),
    preset("indigo", Res.string.appsettings_theme_color_indigo, seed(hue(10))),
    // 中性三档：同一 tone、靠低彩度与彩色档拉开；石墨再压暗一档做纯黑锚点
    preset("brown", Res.string.appsettings_theme_color_brown, seed(40.0, chroma = 16.0), ThemeStyle.Muted),
    preset("slate", Res.string.appsettings_theme_color_slate, seed(230.0, chroma = 14.0), ThemeStyle.Muted),
    preset("graphite", Res.string.appsettings_theme_color_graphite, seed(280.0, chroma = 2.0, tone = 30.0), ThemeStyle.Mono),
)
