package wang.harlon.appsettings.appearance

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable

/** 平台是否支持壁纸取色（Android 12+）。 */
internal expect fun platformSupportsDynamicColor(): Boolean

/** 系统动态配色；不支持的平台返回 null。 */
@Composable
internal expect fun systemDynamicColorScheme(isDark: Boolean): ColorScheme?

/** 按深浅设置系统栏前景（Android edge-to-edge）；其他平台空操作。 */
@Composable
internal expect fun PlatformSystemBarsEffect(isDark: Boolean)

/** 动态 App 图标的平台实现。 */
internal interface AppIconPlatform {
    val isSupported: Boolean

    /** 当前生效的档位 id，读自系统真实状态；读不出来返回 null。 */
    fun current(options: List<AppIconOption>): String?

    /** 切到 [target]，其余禁用。结束时回调一次，失败带异常。 */
    fun apply(target: AppIconOption, options: List<AppIconOption>, onResult: (Throwable?) -> Unit)
}

@Composable
internal expect fun rememberAppIconPlatform(options: List<AppIconOption>): AppIconPlatform
