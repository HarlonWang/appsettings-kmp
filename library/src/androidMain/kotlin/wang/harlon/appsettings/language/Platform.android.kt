package wang.harlon.appsettings.language

import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ApplicationInfo
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.os.LocaleListCompat
import java.util.Locale

private const val TAG = "appsettings-kmp"

internal actual fun platformSystemLanguageTag(): String = Locale.getDefault().toLanguageTag()

internal actual val platformSupportsInAppLanguage: Boolean = true

/**
 * 应用 per-app locale：API 33+ 由系统持久化并在进程启动前生效；更低版本由 AppCompat 在 Activity 重建时应用，
 * 要跨进程持久化需宿主在 manifest 声明 `AppLocalesMetadataHolderService`（`autoStoreLocales=true`），见 README。
 * `setApplicationLocales` 同值不重建，每次启动都调一遍是安全的。
 */
@Composable
internal actual fun PlatformApplyLanguageEffect(language: AppLanguage) {
    val context = LocalContext.current
    LaunchedEffect(context) { validateActivity(context) }
    LaunchedEffect(language) {
        val locales = language.tag?.let(LocaleListCompat::forLanguageTags) ?: LocaleListCompat.getEmptyLocaleList()
        AppCompatDelegate.setApplicationLocales(locales)
    }
}

/** 不是 AppCompatActivity 时 API 33 以下静默无效：debug 包直接抛，release 只打日志。找不到 Activity（预览）不校验。 */
private fun validateActivity(context: Context) {
    val activity = context.findActivity() ?: return
    if (activity is AppCompatActivity) return
    val message = "appsettings-kmp 界面语言要求宿主 Activity 继承 AppCompatActivity（AppCompatDelegate.setApplicationLocales 在 API 33 以下靠它生效），" +
        "现在是 ${activity::class.java.name}。把 ComponentActivity 换成 AppCompatActivity，主题用 Theme.AppCompat.* 系列。"
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
