package wang.harlon.appsettings.appearance

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.materialkolor.DynamicMaterialTheme
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.ktx.animateColorScheme
import com.materialkolor.rememberDynamicMaterialThemeState

/**
 * 第一档：挂在根上，等价于 MaterialTheme。typography / shapes 透传。
 *
 * - [ColorSource.Seed]：走 materialkolor，显式 SPEC_2025（M3 Expressive 的 2025 色规，更饱和、层次更强，不作为可调项暴露）。
 * - [ColorSource.Fixed] 与壁纸取色：现成 scheme，库做纯黑变换与切换动画。
 * - [syncSystemBars] 在 Android 上按深浅设置系统栏前景（edge-to-edge）。
 */
@Composable
internal fun AppearanceTheme(
    state: AppearanceState,
    typography: Typography = MaterialTheme.typography,
    shapes: Shapes = MaterialTheme.shapes,
    animate: Boolean = true,
    syncSystemBars: Boolean = true,
    content: @Composable () -> Unit,
) {
    val isDark = state.isDark()
    run {
        if (syncSystemBars) PlatformSystemBarsEffect(isDark)
        when (val plan = schemePlan(state, isDark)) {
            is SchemePlan.Seed -> {
                val themeState = rememberDynamicMaterialThemeState(
                    seedColor = Color(plan.color.argb),
                    isDark = isDark,
                    // 纯黑档把 background / surface 压到全黑，OLED 屏上这些像素直接熄灭
                    isAmoled = plan.amoled,
                    style = plan.color.style.paletteStyle,
                    contrastLevel = plan.color.contrast.level,
                    specVersion = ColorSpec.SpecVersion.SPEC_2025,
                )
                DynamicMaterialTheme(
                    state = themeState,
                    shapes = shapes,
                    typography = typography,
                    animate = animate,
                    animationSpec = ThemeAnimationSpec,
                    content = content,
                )
            }
            is SchemePlan.Static -> {
                val target = if (plan.amoled) plan.scheme.amoled() else plan.scheme
                val scheme = if (animate) animateColorScheme(target, animationSpec = { ThemeAnimationSpec }) else target
                MaterialTheme(colorScheme = scheme, shapes = shapes, typography = typography, content = content)
            }
        }
    }
}

private val ThemeAnimationSpec: FiniteAnimationSpec<Color> = tween(durationMillis = 600, easing = FastOutSlowInEasing)

internal sealed interface SchemePlan {
    val amoled: Boolean

    data class Seed(val color: CustomColor, override val amoled: Boolean) : SchemePlan

    data class Static(val scheme: ColorScheme, override val amoled: Boolean) : SchemePlan
}

/** 把「来源 + 选择 + 模式」解析成一条配色方案；壁纸取色在不支持的平台回落默认预设。 */
@Composable
internal fun schemePlan(state: AppearanceState, isDark: Boolean): SchemePlan {
    val appearance = state.appearance
    val options = state.options
    val amoled = isDark && appearance.mode == ThemeMode.Amoled
    return when (val source = options.colorSource) {
        is ColorSource.Fixed -> SchemePlan.Static(if (isDark) source.dark else source.light, amoled)
        ColorSource.Seed -> when (val choice = appearance.color) {
            is ColorChoice.Preset -> SchemePlan.Seed(options.preset(choice.id)?.toCustom() ?: options.defaultCustom(), amoled)
            is ColorChoice.Custom -> SchemePlan.Seed(choice.color, amoled)
            ColorChoice.SystemDynamic -> systemDynamicColorScheme(isDark)
                ?.let { SchemePlan.Static(it, amoled) }
                ?: SchemePlan.Seed(options.defaultCustom(), amoled)
        }
    }
}

/** 预设的等价自定义配置：预设钦定的风格 + 标准对比度。 */
internal fun ThemePreset.toCustom(): CustomColor = CustomColor(argb, style, ThemeContrast.Standard)

/** 默认选择对应的 seed 配置；壁纸取色不可用时也用它。 */
internal fun AppearanceOptions.defaultCustom(): CustomColor = when (val d = defaultColor) {
    is ColorChoice.Preset -> preset(d.id)?.toCustom() ?: palette.first().toCustom()
    is ColorChoice.Custom -> d.color
    ColorChoice.SystemDynamic -> palette.first().toCustom()
}

/** 纯黑变换：底与面压成黑，容器层保留，卡片才看得出边界。 */
internal fun ColorScheme.amoled(): ColorScheme = copy(
    background = Color.Black,
    surface = Color.Black,
    surfaceContainerLowest = Color.Black,
)
