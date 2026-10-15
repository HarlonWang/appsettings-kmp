package wang.harlon.appsettings.language

import androidx.compose.runtime.Composable
import platform.Foundation.NSLocale
import platform.Foundation.NSURL
import platform.Foundation.currentLocale
import platform.Foundation.localeIdentifier
import platform.Foundation.localizedStringForLocaleIdentifier
import platform.Foundation.preferredLanguages
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString

/** iOS 的 currentLocale 已经是 per-app 语言（用户在系统设置里给本 App 选的），直接取。 */
internal actual fun platformSystemLanguageTag(): String =
    NSLocale.preferredLanguages.firstOrNull() as? String ?: NSLocale.currentLocale.localeIdentifier

/** 没有不重启的 per-app locale 切换 API；用户去系统设置的「App 语言」里改，系统会重启 App。 */
internal actual val platformSupportsInAppLanguage: Boolean = false

@Composable
internal actual fun PlatformApplyLanguageEffect(language: AppLanguage) = Unit

internal actual fun openPlatformAppSettings() {
    val url = NSURL(string = UIApplicationOpenSettingsURLString)
    val app = UIApplication.sharedApplication
    if (app.canOpenURL(url)) app.openURL(url, options = emptyMap<Any?, Any?>(), completionHandler = null)
}

internal actual fun platformLanguageDisplayName(tag: String): String {
    val name = NSLocale(localeIdentifier = tag).localizedStringForLocaleIdentifier(tag)
    return name.replaceFirstChar { it.uppercase() }
}
