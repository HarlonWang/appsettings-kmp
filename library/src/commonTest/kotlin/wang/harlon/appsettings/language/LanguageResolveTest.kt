package wang.harlon.appsettings.language

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LanguageResolveTest {

    private val languages = listOf("zh", "en")

    @Test
    fun missing_or_system_storage_is_follow_system() {
        assertEquals(AppLanguage.System, languageFromStorage(null, languages))
        assertEquals(AppLanguage.System, languageFromStorage("system", languages))
    }

    @Test
    fun storage_outside_list_is_follow_system() {
        assertEquals(AppLanguage.System, languageFromStorage("ja", languages), "清单外的旧值当作跟随系统")
        assertEquals(AppLanguage("en"), languageFromStorage("en", languages))
    }

    @Test
    fun explicit_choice_wins() {
        assertEquals("en", resolveLanguage(AppLanguage("en"), languages, "zh-Hans-CN"))
    }

    @Test
    fun system_is_clamped_by_primary_subtag() {
        assertEquals("zh", resolveLanguage(AppLanguage.System, languages, "zh-Hans-CN"))
        assertEquals("en", resolveLanguage(AppLanguage.System, languages, "en_US"))
    }

    @Test
    fun exact_tag_beats_primary_match() {
        val list = listOf("zh", "zh-Hant", "en")
        assertEquals("zh-Hant", matchLanguage("zh-Hant", list))
        assertEquals("zh", matchLanguage("zh-Hans", list))
    }

    @Test
    fun system_outside_list_falls_back_to_first() {
        assertEquals("zh", resolveLanguage(AppLanguage.System, languages, "ja-JP"))
        assertNull(matchLanguage("ja", languages))
    }

    @Test
    fun empty_list_returns_system_as_is() {
        assertEquals("ja-JP", resolveLanguage(AppLanguage.System, emptyList(), "ja-JP"))
    }
}
