package wang.harlon.appsettings.appearance

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import wang.harlon.appsettings.resources.Res
import wang.harlon.appsettings.resources.appsettings_theme_mode_amoled
import wang.harlon.appsettings.resources.appsettings_theme_mode_dark
import wang.harlon.appsettings.resources.appsettings_theme_mode_follow_system
import wang.harlon.appsettings.resources.appsettings_theme_mode_light

/**
 * 深浅模式。按名称持久化（稳定契约），不按 ordinal。
 * [Amoled] 是 [Dark] 的变体：同一套配色，background / surface 压成纯黑，OLED 屏上这些像素直接熄灭。
 */
enum class ThemeMode {
    FollowSystem,
    Light,
    Dark,
    Amoled,
    ;

    companion object {
        fun fromStorage(name: String?): ThemeMode? = entries.firstOrNull { it.name == name }
    }
}

/** 展示文案（库内中英文兜底），设置页入口的 trailing 与外观页卡片共用。 */
@Composable
fun ThemeMode.label(): String = when (this) {
    ThemeMode.FollowSystem -> stringResource(Res.string.appsettings_theme_mode_follow_system)
    ThemeMode.Light -> stringResource(Res.string.appsettings_theme_mode_light)
    ThemeMode.Dark -> stringResource(Res.string.appsettings_theme_mode_dark)
    ThemeMode.Amoled -> stringResource(Res.string.appsettings_theme_mode_amoled)
}
