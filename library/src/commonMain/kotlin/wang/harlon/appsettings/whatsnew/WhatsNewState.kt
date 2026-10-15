package wang.harlon.appsettings.whatsnew

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.russhwolf.settings.Settings

/** 更新说明的状态：本次启动有没有要弹的，以及关闭它。 */
@Stable
internal class WhatsNewState internal constructor(
    private val settings: Settings,
    private val currentVersion: String,
) {
    internal var onEvent: (WhatsNewEvent) -> Unit = {}

    /** 待弹的更新说明；null 表示本次启动没有要弹的。宿主可据此延后其他弹窗。 */
    var pending: WhatsNewInfo? by mutableStateOf(null)
        private set

    /** 关闭：记下当前版本已看，发 [WhatsNewEvent.Dismissed]。没有待弹的时候什么都不做。 */
    fun dismiss() {
        val shown = pending ?: return
        settings.putString(LAST_SEEN_KEY, currentVersion)
        pending = null
        onEvent(WhatsNewEvent.Dismissed(shown.version))
    }

    /**
     * 首次安装静默记录当前版本不弹；同版本不弹；升级后首启且内置版本匹配才弹，
     * 否则直接标记已看，避免一直 pending。[load] 抛异常按没有内置文件处理。
     */
    internal suspend fun resolve(load: suspend () -> String?) {
        val lastSeen = settings.getStringOrNull(LAST_SEEN_KEY)
        if (lastSeen == null) {
            settings.putString(LAST_SEEN_KEY, currentVersion)
            return
        }
        if (lastSeen == currentVersion) return

        val bundled = runCatching { load() }.getOrNull()?.let(::parseWhatsNew)
        if (bundled != null && !bundled.isEmpty && shouldShowWhatsNew(currentVersion, bundled.version, lastSeen)) {
            pending = bundled
            onEvent(WhatsNewEvent.Shown(bundled.version))
        } else {
            settings.putString(LAST_SEEN_KEY, currentVersion)
        }
    }
}

/**
 * 挂在根上一次。不读宿主的旧 key：从旧实现迁过来的 App，老用户升级那一次按首装处理、少弹一回。
 *
 * @param settings 宿主已有的 multiplatform-settings 实例，库只写 `appsettings.whatsnew.*` 下的 key
 * @param currentVersion 当前 App 版本号，须与随包 JSON 的 `version` 同一口径
 * @param load 读取随包 JSON 文本，通常是 `Res.readBytes("files/whatsnew.json").decodeToString()`
 */
@Composable
internal fun rememberWhatsNewState(
    settings: Settings,
    currentVersion: String,
    load: suspend () -> String?,
    onEvent: (WhatsNewEvent) -> Unit = {},
): WhatsNewState {
    val state = remember(settings, currentVersion) { WhatsNewState(settings, currentVersion) }
    SideEffect { state.onEvent = onEvent }
    LaunchedEffect(state) { state.resolve(load) }
    return state
}
