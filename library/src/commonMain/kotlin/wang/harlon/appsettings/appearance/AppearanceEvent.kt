package wang.harlon.appsettings.appearance

import wang.harlon.appsettings.AppSettingsEvent

sealed interface AppearanceEvent : AppSettingsEvent {
    data class ModeChanged(val mode: ThemeMode) : AppearanceEvent

    /** 选了预设 / 壁纸取色 / 回到上次的自定义色。 */
    data class ColorSelected(val choice: ColorChoice) : AppearanceEvent

    data object ColorLabOpened : AppearanceEvent

    /** 离开调色台时记一次最终值，拖动过程中不记；只看不改的不发。 */
    data class CustomApplied(val color: CustomColor) : AppearanceEvent

    data class AppIconChanged(val id: String) : AppearanceEvent

    data class AppIconChangeFailed(val id: String, val cause: Throwable) : AppearanceEvent
}
