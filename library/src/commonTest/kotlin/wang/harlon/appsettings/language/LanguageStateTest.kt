package wang.harlon.appsettings.language

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LanguageStateTest {

    private val languages = listOf("zh", "en")

    private fun state(settings: MapSettings, system: String = "en-US", events: MutableList<LanguageEvent> = mutableListOf()) =
        LanguageState(settings, languages, { system }).also { it.onEvent = events::add }

    @Test
    fun initial_value_is_read_synchronously() {
        val settings = MapSettings().apply { putString(LANGUAGE_KEY, "zh") }
        assertEquals(AppLanguage("zh"), state(settings).current)
    }

    @Test
    fun select_persists_and_emits() {
        val settings = MapSettings()
        val events = mutableListOf<LanguageEvent>()
        val s = state(settings, events = events)
        s.select(AppLanguage("zh"))
        assertEquals("zh", settings.getStringOrNull(LANGUAGE_KEY))
        assertEquals("zh", s.resolved())
        assertEquals<List<LanguageEvent>>(listOf(LanguageEvent.Changed("zh")), events)
    }

    @Test
    fun select_system_stores_sentinel_and_resolves_from_system() {
        val settings = MapSettings().apply { putString(LANGUAGE_KEY, "zh") }
        val s = state(settings, system = "en-GB")
        s.select(AppLanguage.System)
        assertEquals(SYSTEM_TAG, settings.getStringOrNull(LANGUAGE_KEY))
        assertEquals("en", s.resolved())
    }

    @Test
    fun select_outside_list_or_same_value_is_ignored() {
        val settings = MapSettings()
        val events = mutableListOf<LanguageEvent>()
        val s = state(settings, events = events)
        s.select(AppLanguage("ja"))
        s.select(AppLanguage.System)
        assertEquals(AppLanguage.System, s.current)
        assertTrue(events.isEmpty())
        assertTrue(!settings.hasKey(LANGUAGE_KEY))
    }
}
