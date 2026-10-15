package wang.harlon.appsettings.whatsnew

import wang.harlon.appsettings.AppSettingsEvent

sealed interface WhatsNewEvent : AppSettingsEvent {
    /** 更新说明弹出。 */
    data class Shown(val version: String) : WhatsNewEvent

    /** 用户关闭了更新说明（点「知道了」或点外部）。 */
    data class Dismissed(val version: String) : WhatsNewEvent
}
