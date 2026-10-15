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

    var current: AppLanguage by mutableStateOf(languageFromStorage(settings.getStringOrNull(LANGUAGE_KEY), languages))
        private set

    /** 实际生效的语言标签：跟随系统时取系统语言并钳到清单。 */
    fun resolved(): String = resolveLanguage(current, languages, systemLanguage())

    /** 写入并发 [LanguageEvent.Changed]；清单外的标签忽略，同值不重复发事件。 */
    fun select(language: AppLanguage) {
        val tag = language.tag
        if (tag != null && tag !in languages) return
        if (language == current) return
        settings.putString(LANGUAGE_KEY, tag ?: SYSTEM_TAG)
        current = language
        onEvent(LanguageEvent.Changed(tag))
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
