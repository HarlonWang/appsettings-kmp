package wang.harlon.appsettings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.Foundation.NSBundle
import platform.Foundation.NSLocale
import platform.Foundation.currentLocale
import platform.Foundation.languageCode

@Composable
internal actual fun rememberPlatformAppInfo(): PlatformAppInfo = remember {
    val info = NSBundle.mainBundle.infoDictionary
    PlatformAppInfo(
        name = (info?.get("CFBundleDisplayName") ?: info?.get("CFBundleName")) as? String ?: "",
        version = info?.get("CFBundleShortVersionString") as? String ?: "",
    )
}

internal actual fun platformSystemLanguage(): String = NSLocale.currentLocale.languageCode
