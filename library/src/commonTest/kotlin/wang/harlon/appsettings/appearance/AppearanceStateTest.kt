package wang.harlon.appsettings.appearance

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AppearanceStateTest {

    private fun state(
        options: AppearanceOptions = AppearanceOptions(),
        supportsDynamic: Boolean = true,
        events: MutableList<AppearanceEvent> = mutableListOf(),
        settings: MapSettings = MapSettings(),
    ) = AppearanceState(AppearanceStore(settings, options), options, supportsDynamic).also { it.onEvent = events::add }

    @Test
    fun set_mode_persists_and_emits_once() {
        val events = mutableListOf<AppearanceEvent>()
        val s = state(events = events)
        s.setMode(ThemeMode.Dark)
        s.setMode(ThemeMode.Dark)
        assertEquals(ThemeMode.Dark, s.appearance.mode)
        assertEquals(listOf<AppearanceEvent>(AppearanceEvent.ModeChanged(ThemeMode.Dark)), events)
    }

    @Test
    fun mode_outside_options_is_ignored() {
        val s = state(AppearanceOptions(modes = listOf(ThemeMode.Light, ThemeMode.Dark)))
        s.setMode(ThemeMode.Amoled)
        assertEquals(ThemeMode.Light, s.appearance.mode)
    }

    @Test
    fun select_validates_against_options_and_platform() {
        val events = mutableListOf<AppearanceEvent>()
        val s = state(supportsDynamic = false, events = events)
        s.select(ColorChoice.Preset("no-such"))
        s.select(ColorChoice.SystemDynamic)
        assertEquals(ColorChoice.Preset("default"), s.appearance.color)
        assertTrue(events.isEmpty())

        s.select(ColorChoice.Preset("pine"))
        assertEquals(ColorChoice.Preset("pine"), s.appearance.color)
        assertEquals(AppearanceEvent.ColorSelected(ColorChoice.Preset("pine")), events.single())
    }

    @Test
    fun system_dynamic_is_accepted_only_when_supported_and_allowed() {
        state(supportsDynamic = true).let { s ->
            s.select(ColorChoice.SystemDynamic)
            assertEquals(ColorChoice.SystemDynamic, s.appearance.color)
        }
        state(AppearanceOptions(allowSystemDynamic = false), supportsDynamic = true).let { s ->
            s.select(ColorChoice.SystemDynamic)
            assertEquals(ColorChoice.Preset("default"), s.appearance.color)
        }
    }

    @Test
    fun apply_custom_is_silent_and_history_is_promoted_and_capped() {
        val events = mutableListOf<AppearanceEvent>()
        val s = state(events = events)
        val a = CustomColor(0xFF112233L)
        val b = CustomColor(0xFF445566L)
        s.applyCustom(a)
        assertEquals(ColorChoice.Custom(a), s.appearance.color)
        assertTrue(events.isEmpty(), "实时落盘不发事件")

        s.pushHistory(a); s.pushHistory(b); s.pushHistory(a)
        assertEquals(listOf(a, b), s.appearance.customHistory, "相同组合提前而非重复")
        repeat(12) { s.pushHistory(CustomColor(0xFF000000L + it)) }
        assertEquals(10, s.appearance.customHistory.size)
        assertEquals(0xFF00000BL, s.appearance.customHistory.first().argb)
    }

    @Test
    fun last_custom_color_prefers_active_then_history() {
        val s = state()
        assertNull(s.lastCustomColor)
        val a = CustomColor(0xFF112233L)
        s.pushHistory(a)
        assertEquals(a, s.lastCustomColor)
        val b = CustomColor(0xFF445566L)
        s.applyCustom(b)
        assertEquals(b, s.lastCustomColor)
        s.select(ColorChoice.Preset("pine"))
        assertEquals(a, s.lastCustomColor, "回到预设后显示历史里最近一条")
    }

    @Test
    fun restore_writes_back_the_whole_snapshot_without_touching_history_semantics() {
        val settings = MapSettings()
        val s = state(settings = settings)
        s.select(ColorChoice.Preset("pine"))
        s.pushHistory(CustomColor(0xFF112233L))
        val snapshot = s.appearance

        s.applyCustom(CustomColor(0xFFD219ABL, ThemeStyle.Vivid, ThemeContrast.High))
        s.setMode(ThemeMode.Dark)
        s.restore(snapshot)

        assertEquals(snapshot, s.appearance)
        assertEquals(snapshot, AppearanceStore(settings, AppearanceOptions()).appearance.value, "落了盘")
    }

    @Test
    fun state_reads_persisted_value_synchronously() {
        val settings = MapSettings()
        state(settings = settings).setMode(ThemeMode.Amoled)
        assertEquals(ThemeMode.Amoled, state(settings = settings).appearance.mode)
    }
}
