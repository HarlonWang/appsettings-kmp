package wang.harlon.appsettings.language

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.jetbrains.compose.resources.stringResource
import wang.harlon.appsettings.LocalAppSettings
import wang.harlon.appsettings.icons.AppSettingsIcons
import wang.harlon.appsettings.icons.Language
import wang.harlon.appsettings.resources.Res
import wang.harlon.appsettings.resources.appsettings_language_follow_system
import wang.harlon.appsettings.resources.appsettings_language_open_system_settings
import wang.harlon.appsettings.resources.appsettings_language_title
import wang.harlon.appsettings.ui.SettingsDropdownMenu
import wang.harlon.appsettings.ui.SettingsGroupScope

/**
 * 「界面语言」行。Android 整行点按展开下拉（跟随系统 + 宿主清单）；iOS 点按跳本 App 的系统设置页，在那里改。
 * 要先配 `language { languages = … }`，否则 trailing 为空、点按无效。
 */
fun SettingsGroupScope.languageItem() {
    // 下拉的展开状态住在 trailing 里（能 remember），整行 onClick 经这个转发器打开它
    val menu = MenuOpener()
    settingsItem(
        icon = AppSettingsIcons.Language,
        title = { Text(stringResource(Res.string.appsettings_language_title)) },
        description = if (platformSupportsInAppLanguage) null else {
            { Text(stringResource(Res.string.appsettings_language_open_system_settings)) }
        },
        trailing = { LanguageTrailing(menu) },
        onClick = if (platformSupportsInAppLanguage) menu::open else ::openPlatformAppSettings,
    )
}

private class MenuOpener {
    var show: () -> Unit = {}
    fun open() = show()
}

@Composable
private fun LanguageTrailing(menu: MenuOpener) {
    val appSettings = LocalAppSettings.current
    val state = appSettings.languageState ?: return
    val label = appSettings.config.language.label

    if (!platformSupportsInAppLanguage) {
        // iOS：只回显实际生效的语言，改在系统设置
        Text(text = labelOf(state.resolved(), label), color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    var expanded by remember { mutableStateOf(false) }
    SideEffect { menu.show = { expanded = true } }
    val current = state.current
    Box {
        Text(
            text = if (current.isSystem) stringResource(Res.string.appsettings_language_follow_system) else labelOf(current.tag!!, label),
            color = MaterialTheme.colorScheme.primary,
        )
        SettingsDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.appsettings_language_follow_system)) },
                onClick = { expanded = false; state.select(AppLanguage.System) },
            )
            state.languages.forEach { tag ->
                DropdownMenuItem(
                    text = { Text(labelOf(tag, label)) },
                    onClick = { expanded = false; state.select(AppLanguage(tag)) },
                )
            }
        }
    }
}

@Composable
private fun labelOf(tag: String, label: (@Composable (String) -> String)?): String =
    label?.invoke(tag) ?: platformLanguageDisplayName(tag)
