package wang.harlon.appsettings.language

import wang.harlon.appsettings.AppSettingsEvent

/**
 * 界面语言。[tag] 是 BCP 47 语言标签（`zh`、`en`、`zh-Hant`），null 表示跟随系统。
 * 可选清单由宿主在 `language { languages = … }` 里给，库不枚举。
 */
data class AppLanguage(val tag: String?) {
    val isSystem: Boolean get() = tag == null

    /**
     * 实际生效的语言标签：明确选了就是它；跟随系统时取系统语言钳到 [languages]（先整标签、再主语言子标签），
     * 都不中回落清单第一项。给非 Compose 场景用（拼请求的 locale、Accept-Language）；Compose 里直接读 `appSettings.language.resolved()`。
     */
    fun resolve(languages: List<String>): String = resolveLanguage(this, languages, platformSystemLanguageTag())

    companion object {
        val System = AppLanguage(null)

        /**
         * 从 [APP_LANGUAGE_STORAGE_KEY] 的值还原：缺省、`system`、清单外的值都当作跟随系统，大小写按清单写法规范化。
         * 宿主在非 Compose 场景（ViewModel、网络层）用自己的 Settings 读同一个 key 时走这里，别自己解析。
         */
        fun fromStorage(stored: String?, languages: List<String>): AppLanguage = languageFromStorage(stored, languages)
    }
}

sealed interface LanguageEvent : AppSettingsEvent {
    /** 用户选了新的界面语言；[tag] 为 null 即跟随系统。记的是写入后的结果。 */
    data class Changed(val tag: String?) : LanguageEvent
}

/** 界面语言在 multiplatform-settings 里的 key。值是 BCP 47 标签或 `system`；稳定契约，宿主可直接读（配合 [AppLanguage.fromStorage]）。 */
const val APP_LANGUAGE_STORAGE_KEY = "appsettings.language.tag"
internal const val SYSTEM_TAG = "system"

/** 存储值 → 语言：缺省、`system`、清单外的旧值都当作跟随系统；大小写按清单里的写法规范化。 */
internal fun languageFromStorage(stored: String?, languages: List<String>): AppLanguage =
    stored?.takeIf { it != SYSTEM_TAG }?.let { canonicalTag(it, languages) }?.let(::AppLanguage) ?: AppLanguage.System

/** 标签在清单里的规范写法（BCP 47 大小写不敏感）；不在清单里返回 null。 */
internal fun canonicalTag(tag: String, languages: List<String>): String? =
    languages.firstOrNull { it.equals(tag.replace('_', '-'), ignoreCase = true) }

/**
 * 跟随系统时把系统语言钳到清单：先按完整标签匹配，再按主语言子标签（`zh-Hans` → `zh`）；都不中回落清单第一项。
 * 清单为空时原样返回系统语言。
 */
internal fun resolveLanguage(language: AppLanguage, languages: List<String>, systemTag: String): String {
    language.tag?.let { return it }
    if (languages.isEmpty()) return systemTag
    return matchLanguage(systemTag, languages) ?: languages.first()
}

internal fun matchLanguage(candidate: String, languages: List<String>): String? {
    val normalized = candidate.replace('_', '-').lowercase()
    languages.firstOrNull { it.lowercase() == normalized }?.let { return it }
    val primary = normalized.substringBefore('-')
    return languages.firstOrNull { it.lowercase().substringBefore('-') == primary }
}
