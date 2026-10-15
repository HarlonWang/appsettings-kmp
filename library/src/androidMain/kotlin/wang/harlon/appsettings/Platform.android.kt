package wang.harlon.appsettings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

@Composable
internal actual fun rememberPlatformAppInfo(): PlatformAppInfo {
    val context = LocalContext.current
    return remember(context) {
        val name = context.applicationInfo.loadLabel(context.packageManager).toString()
        val version = runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull().orEmpty()
        PlatformAppInfo(name = name, version = version)
    }
}

internal actual fun platformSystemLanguage(): String = Locale.getDefault().language
