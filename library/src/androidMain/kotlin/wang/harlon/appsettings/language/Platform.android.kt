package wang.harlon.appsettings.language

import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ApplicationInfo
import android.os.Build
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.os.LocaleListCompat
import java.util.Locale

private const val TAG = "appsettings-kmp"

internal actual fun platformSystemLanguageTag(): String = Locale.getDefault().toLanguageTag()

internal actual val platformSupportsInAppLanguage: Boolean = true

/**
 * 用户在行里选 → [LanguageState.platformApply] 调 `setApplicationLocales`，Activity 重建。
 * 启动时对齐一次。API 33+ 的 per-app locale 由系统持久化，系统里的值就是真相：
 * - 与存储一致 → 不动；
 * - 不同（用户在系统设置里改过本 App 的语言，含改回「跟随系统」后的空）→ 采纳系统的，不反向覆盖。
 * API 33 以下没有系统持久化，空只表示本进程还没应用过（没配 autoStoreLocales 的冷启动）→ 回放存储的选择。
 */
@Composable
internal actual fun PlatformLanguageEffect(state: LanguageState) {
    val context = LocalContext.current
    SideEffect { state.platformApply = ::applyLocales }
    LaunchedEffect(context) { validateActivity(context) }
    LaunchedEffect(state) {
        val effective = AppCompatDelegate.getApplicationLocales().toLanguageTags().substringBefore(',').takeIf { it.isNotEmpty() }
        val stored = state.current.tag
        val systemOwned = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        when {
            effective == null && !systemOwned -> if (stored != null) applyLocales(state.current)
            effective == null -> state.adopt(null)
            matchLanguage(effective, state.languages) == stored -> Unit
            else -> state.adopt(effective)
        }
    }
}

private fun applyLocales(language: AppLanguage) {
    val locales = language.tag?.let(LocaleListCompat::forLanguageTags) ?: LocaleListCompat.getEmptyLocaleList()
    AppCompatDelegate.setApplicationLocales(locales)
}

/** 不是 AppCompatActivity 时 API 33 以下静默无效：debug 包直接抛，release 只打日志。找不到 Activity（预览）不校验。 */
private fun validateActivity(context: Context) {
    val activity = context.findActivity() ?: return
    if (activity is AppCompatActivity) return
    val message = "appsettings-kmp 界面语言要求宿主 Activity 继承 AppCompatActivity（AppCompatDelegate.setApplicationLocales 在 API 33 以下靠它生效），" +
        "现在是 ${activity::class.java.name}。把 ComponentActivity 换成 AppCompatActivity，主题用 Theme.AppCompat.* 或 Theme.Material3.* 系列。"
    val debuggable = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    if (debuggable) throw IllegalStateException(message) else Log.w(TAG, message)
}

private tailrec fun Context.findActivity(): android.app.Activity? = when (this) {
    is android.app.Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/** Android 行内可切换，行的 onClick 不走这里；只为满足 expect。 */
internal actual fun openPlatformAppSettings() = Unit

internal actual fun platformLanguageDisplayName(tag: String): String {
    val locale = Locale.forLanguageTag(tag)
    return locale.getDisplayName(locale).replaceFirstChar { it.titlecase(locale) }
}
