package wang.harlon.appsettings.ui

import androidx.compose.runtime.compositionLocalOf

/** 库自带的二级页。设置页框架通过 `onOpen(page)` 要求宿主打开它们，路由由宿主声明。 */
enum class SettingsPage {
    Appearance,
    About,
}

/** 由设置页框架提供，通用行据此找到导航出口。 */
internal val LocalSettingsPageOpener = compositionLocalOf<((SettingsPage) -> Unit)?> { null }

/** 由 Root 提供：宿主配了 `language { languages }` 才为 true，`languageItem()` 据此决定要不要出这一行。 */
internal val LocalLanguageItemEnabled = compositionLocalOf { false }
