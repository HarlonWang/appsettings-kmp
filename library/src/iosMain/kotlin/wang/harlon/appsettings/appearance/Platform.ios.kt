package wang.harlon.appsettings.appearance

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.Foundation.NSError
import platform.UIKit.UIApplication
// ObjC category 成员在 Kotlin/Native 里是扩展，要单独 import
import platform.UIKit.alternateIconName
import platform.UIKit.setAlternateIconName
import platform.UIKit.supportsAlternateIcons

internal actual fun platformSupportsDynamicColor(): Boolean = false

@Composable
internal actual fun systemDynamicColorScheme(isDark: Boolean): ColorScheme? = null

@Composable
internal actual fun PlatformSystemBarsEffect(isDark: Boolean) = Unit

@Composable
internal actual fun rememberAppIconPlatform(options: List<AppIconOption>): AppIconPlatform =
    remember(options) { IosAppIconPlatform() }

/** iOS 的备用图标：名字在 Info.plist 的 CFBundleAlternateIcons 里声明，null 是主图标。 */
private class IosAppIconPlatform : AppIconPlatform {
    private val app get() = UIApplication.sharedApplication

    override val isSupported: Boolean get() = app.supportsAlternateIcons

    override fun current(options: List<AppIconOption>): String? {
        val name = app.alternateIconName
        return options.firstOrNull { it.iosIconName == name }?.id
    }

    override fun apply(target: AppIconOption, options: List<AppIconOption>, onResult: (Throwable?) -> Unit) {
        app.setAlternateIconName(target.iosIconName) { error: NSError? ->
            onResult(error?.let { IllegalStateException(it.localizedDescription) })
        }
    }
}
