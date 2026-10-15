package wang.harlon.appsettings.appearance

import android.content.ComponentName
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Color as AndroidColor
import android.os.Build
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

private const val TAG = "appsettings"

internal actual fun platformSupportsDynamicColor(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

@Composable
internal actual fun systemDynamicColorScheme(isDark: Boolean): ColorScheme? {
    if (!platformSupportsDynamicColor()) return null
    val context = LocalContext.current
    // 壁纸换色会触发配置变更、Activity 重建，按 context 记住即可
    return remember(context, isDark) { if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context) }
}

/** 按深浅设置状态栏 / 导航栏前景，替代宿主在 Activity 里重抄的那段 when。找不到 Activity（预览）就不做。 */
@Composable
internal actual fun PlatformSystemBarsEffect(isDark: Boolean) {
    val activity = LocalContext.current.findComponentActivity() ?: return
    DisposableEffect(activity, isDark) {
        activity.enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT) { isDark },
            navigationBarStyle = SystemBarStyle.auto(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT) { isDark },
        )
        onDispose {}
    }
}

private tailrec fun Context.findComponentActivity(): ComponentActivity? = when (this) {
    is ComponentActivity -> this
    is ContextWrapper -> baseContext.findComponentActivity()
    else -> null
}

@Composable
internal actual fun rememberAppIconPlatform(options: List<AppIconOption>): AppIconPlatform {
    val context = LocalContext.current.applicationContext
    return remember(context, options) { AndroidAppIconPlatform(context, options).also { it.validate() } }
}

/**
 * 遍历全部 launcher alias，先启用目标、再禁用其余，任何时刻至少一个桌面入口。
 * 真实状态只有系统里那一份：读 getComponentEnabledSetting，DEFAULT 时回退 manifest 的 enabled。
 */
private class AndroidAppIconPlatform(
    private val context: Context,
    private val options: List<AppIconOption>,
) : AppIconPlatform {
    private val pm: PackageManager = context.packageManager

    override val isSupported: Boolean = options.any { it.androidAlias != null }

    private fun component(alias: String) = ComponentName(context.packageName, alias)

    /** manifest 里声明的 enabled；alias 不存在返回 null。 */
    private fun manifestEnabled(alias: String): Boolean? = try {
        @Suppress("DEPRECATION")
        pm.getActivityInfo(component(alias), PackageManager.MATCH_DISABLED_COMPONENTS).enabled
    } catch (e: PackageManager.NameNotFoundException) {
        null
    }

    private fun isEnabled(alias: String): Boolean = when (pm.getComponentEnabledSetting(component(alias))) {
        PackageManager.COMPONENT_ENABLED_STATE_ENABLED -> true
        PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
        PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER,
        PackageManager.COMPONENT_ENABLED_STATE_DISABLED_UNTIL_USED,
        -> false
        else -> manifestEnabled(alias) ?: false
    }

    override fun current(options: List<AppIconOption>): String? =
        options.firstOrNull { it.androidAlias != null && isEnabled(it.androidAlias) }?.id

    override fun apply(target: AppIconOption, options: List<AppIconOption>, onResult: (Throwable?) -> Unit) {
        // 先启用目标，成功了才去禁用其余：目标没有 alias 或启用失败就原地停下，桌面入口不会被清空
        val targetAlias = target.androidAlias
        if (targetAlias == null) {
            onResult(IllegalStateException("图标档位 ${target.id} 没有 androidAlias，Android 上不可选"))
            return
        }
        try {
            pm.setComponentEnabledSetting(component(targetAlias), PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)
        } catch (e: Exception) {
            Log.w(TAG, "enable ${target.id} failed", e)
            onResult(e)
            return
        }
        options.forEach { option ->
            val alias = option.androidAlias ?: return@forEach
            if (option.id == target.id) return@forEach
            try {
                pm.setComponentEnabledSetting(component(alias), PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
            } catch (e: Exception) {
                // 单个 alias 禁用失败不阻断其余：目标已启用，最坏是桌面短暂多一个图标
                Log.w(TAG, "disable ${option.id} failed", e)
            }
        }
        onResult(null)
    }

    /** 声明错误要尽早暴露：写错 alias 名或 enabled 默认值平时没有任何报错，只会在用户手机上出现双图标或图标消失。 */
    fun validate() {
        val problems = buildList {
            options.forEach { option ->
                val alias = option.androidAlias ?: return@forEach
                if (manifestEnabled(alias) == null) add("manifest 里找不到 <activity-alias android:name=\"$alias\">")
            }
            val defaults = options.count { it.androidAlias?.let(::manifestEnabled) == true }
            if (defaults != 1) add("manifest 里应有且只有一个 alias 的 android:enabled=\"true\"，现在是 $defaults 个")
        }
        if (problems.isEmpty()) return
        val message = buildString {
            appendLine("appsettings-kmp 动态图标声明有误：")
            problems.forEach { appendLine("  - $it") }
            append("每个 AppIconOption.androidAlias 都要在 manifest 里有同名 <activity-alias>（带 MAIN/LAUNCHER intent-filter、")
            append("android:targetActivity 指向本体 Activity），且恰好一个 android:enabled=\"true\"；本体 Activity 不再直接出现在桌面。")
        }
        val debuggable = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        if (debuggable) throw IllegalStateException(message) else Log.w(TAG, message)
    }
}
