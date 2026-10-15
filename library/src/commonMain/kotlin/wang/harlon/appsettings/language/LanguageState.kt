package wang.harlon.appsettings.language

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.russhwolf.settings.Settings

/** 界面语言的状态：当前选择、解析结果、写入。初值同步读存储，首帧即正确。 */
@Stable
internal class LanguageState internal constructor(
    private val settings: Settings,
    internal val languages: List<String>,
    private val systemLanguage: () -> String,
) {
    internal var onEvent: (LanguageEvent) -> Unit = {}

    /** 平台把选择落到系统（Android 的 per-app locale）；由平台层在 Root 里挂上，iOS 为空操作。 */
    internal var platformApply: (AppLanguage) -> Unit = {}

    var current: AppLanguage by mutableStateOf(languageFromStorage(settings.getStringOrNull(APP_LANGUAGE_STORAGE_KEY), languages))
        private set

    /** 实际生效的语言标签：跟随系统时取系统语言并钳到清单。 */
    fun resolved(): String = resolveLanguage(current, languages, systemLanguage())

    /** 用户在行里选：写入、发 [LanguageEvent.Changed]、交给平台生效。清单外的标签忽略，同值不重复。 */
    fun select(language: AppLanguage) {
        val chosen = language.tag?.let { canonicalTag(it, languages)?.let(::AppLanguage) ?: return } ?: AppLanguage.System
        if (!commit(chosen)) return
        platformApply(chosen)
    }

    /**
     * 平台发现系统里已经是另一种语言（Android 13+ 用户在系统设置里改过本 App 的语言）：以系统为准更新状态，不再反向应用。
     * [tag] 为 null 即系统里改回了「跟随系统」；清单外的标签什么都不做，留着系统的选择。
     */
    internal fun adopt(tag: String?) {
        val language = tag?.let { matchLanguage(it, languages) ?: return }?.let(::AppLanguage) ?: AppLanguage.System
        commit(language)
    }

    private fun commit(language: AppLanguage): Boolean {
        if (language == current) return false
        settings.putString(APP_LANGUAGE_STORAGE_KEY, language.tag ?: SYSTEM_TAG)
        current = language
        onEvent(LanguageEvent.Changed(language.tag))
        return true
    }
}

@Composable
internal fun rememberLanguageState(
    settings: Settings,
    languages: List<String>,
    onEvent: (LanguageEvent) -> Unit = {},
): LanguageState {
    val state = remember(settings, languages) { LanguageState(settings, languages, ::platformSystemLanguageTag) }
    SideEffect { state.onEvent = onEvent }
    return state
}
