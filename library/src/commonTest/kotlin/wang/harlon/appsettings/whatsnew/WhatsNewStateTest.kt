package wang.harlon.appsettings.whatsnew

import com.russhwolf.settings.MapSettings
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WhatsNewStateTest {

    private val bundled = """{"version":"1.1.0","zh":["新功能"],"en":["New feature"]}"""

    private fun state(settings: MapSettings, current: String, events: MutableList<WhatsNewEvent>) =
        WhatsNewState(settings, current).also { it.onEvent = events::add }

    @Test
    fun first_install_records_silently() = runTest {
        val settings = MapSettings()
        val events = mutableListOf<WhatsNewEvent>()
        val s = state(settings, "1.1.0", events)
        s.resolve { bundled }
        assertNull(s.pending)
        assertEquals("1.1.0", settings.getStringOrNull(LAST_SEEN_KEY))
        assertTrue(events.isEmpty())
    }

    @Test
    fun same_version_does_nothing() = runTest {
        val settings = MapSettings().apply { putString(LAST_SEEN_KEY, "1.1.0") }
        val events = mutableListOf<WhatsNewEvent>()
        val s = state(settings, "1.1.0", events)
        s.resolve { error("must not load") }
        assertNull(s.pending)
        assertTrue(events.isEmpty())
    }

    @Test
    fun upgrade_shows_then_dismiss_records() = runTest {
        val settings = MapSettings().apply { putString(LAST_SEEN_KEY, "1.0.0") }
        val events = mutableListOf<WhatsNewEvent>()
        val s = state(settings, "1.1.0", events)
        s.resolve { bundled }
        assertNotNull(s.pending)
        assertEquals<String?>("1.0.0", settings.getStringOrNull(LAST_SEEN_KEY), "弹出前不改 lastSeen，关掉才算看过")
        assertEquals<List<WhatsNewEvent>>(listOf(WhatsNewEvent.Shown("1.1.0")), events)

        s.dismiss()
        assertNull(s.pending)
        assertEquals("1.1.0", settings.getStringOrNull(LAST_SEEN_KEY))
        assertEquals<WhatsNewEvent>(WhatsNewEvent.Dismissed("1.1.0"), events.last())
    }

    @Test
    fun placeholder_bundle_marks_seen_without_showing() = runTest {
        val settings = MapSettings().apply { putString(LAST_SEEN_KEY, "1.0.0") }
        val events = mutableListOf<WhatsNewEvent>()
        val s = state(settings, "1.1.0", events)
        s.resolve { """{"version":"0.0.0","zh":[],"en":[]}""" }
        assertNull(s.pending)
        assertEquals("1.1.0", settings.getStringOrNull(LAST_SEEN_KEY))
        assertTrue(events.isEmpty())
    }

    @Test
    fun failing_load_marks_seen_without_showing() = runTest {
        val settings = MapSettings().apply { putString(LAST_SEEN_KEY, "1.0.0") }
        val s = state(settings, "1.1.0", mutableListOf())
        s.resolve { error("no file") }
        assertNull(s.pending)
        assertEquals("1.1.0", settings.getStringOrNull(LAST_SEEN_KEY))
    }

    @Test
    fun pending_helper_mirrors_last_seen() {
        val settings = MapSettings()
        assertFalse(isWhatsNewPending(settings, "1.1.0"), "首装不算 pending")
        settings.putString(LAST_SEEN_KEY, "1.0.0")
        assertTrue(isWhatsNewPending(settings, "1.1.0"))
        settings.putString(LAST_SEEN_KEY, "1.1.0")
        assertFalse(isWhatsNewPending(settings, "1.1.0"))
    }

    @Test
    fun dismiss_without_pending_is_noop() {
        val settings = MapSettings()
        val events = mutableListOf<WhatsNewEvent>()
        state(settings, "1.1.0", events).dismiss()
        assertNull(settings.getStringOrNull(LAST_SEEN_KEY))
        assertTrue(events.isEmpty())
    }
}
