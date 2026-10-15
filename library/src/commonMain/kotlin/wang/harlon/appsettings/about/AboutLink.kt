package wang.harlon.appsettings.about

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * 关于页的一条链接。内置类型自带中英文标题与图标；[Custom] 自己给。
 * 怎么打开（App 内 WebView 还是系统浏览器）和要不要埋点，都在宿主的 `onLinkClick` 里决定。
 */
sealed interface AboutLink {
    val url: String

    data class Privacy(override val url: String) : AboutLink
    data class Terms(override val url: String) : AboutLink
    data class Website(override val url: String) : AboutLink
    data class Changelog(override val url: String) : AboutLink
    data class Feedback(override val url: String) : AboutLink
    data class Licenses(override val url: String) : AboutLink

    /**
     * 库里没有的链接类型。
     * @param key 宿主埋点用的稳定标识
     */
    data class Custom(
        override val url: String,
        val key: String,
        val label: @Composable () -> String,
        val icon: ImageVector? = null,
    ) : AboutLink
}

/** 「检查更新」行的显示状态。检查逻辑因渠道而异，留在宿主；这里只管 trailing 怎么画、行能不能点。 */
sealed interface UpdateStatus {
    data object Idle : UpdateStatus

    /** trailing 显示 LoadingIndicator，行不可点。 */
    data object Checking : UpdateStatus

    data object UpToDate : UpdateStatus

    data object Failed : UpdateStatus

    data class Available(val version: String) : UpdateStatus
}
