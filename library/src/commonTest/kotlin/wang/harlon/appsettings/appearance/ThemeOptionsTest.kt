package wang.harlon.appsettings.appearance

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ThemeOptionsTest {

    @Test
    fun style_options_cover_every_preset_style() {
        // 从某个预设进调色台时档位必须对得上号，否则会被静默改成别的风格
        DefaultPalette.forEach { preset ->
            assertEquals(preset.style.paletteStyle, ThemeStyle.fromPalette(preset.style.paletteStyle).paletteStyle, preset.id)
        }
    }

    @Test
    fun style_and_contrast_storage_round_trip_and_fall_back() {
        ThemeStyle.entries.forEach { assertEquals(it, ThemeStyle.fromStorage(it.storageValue)) }
        ThemeContrast.entries.forEach { assertEquals(it, ThemeContrast.fromStorage(it.storageValue)) }
        assertEquals(ThemeStyle.Soft, ThemeStyle.fromStorage("nope"))
        assertEquals(ThemeStyle.Soft, ThemeStyle.fromStorage(null))
        assertEquals(ThemeContrast.Standard, ThemeContrast.fromStorage("nope"))
    }

    @Test
    fun contrast_levels_stay_in_supported_range() {
        ThemeContrast.entries.forEach { assertTrue(it.level in 0.0..1.0, it.storageValue) }
    }

    @Test
    fun theme_mode_storage_round_trips_by_name() {
        ThemeMode.entries.forEach { assertEquals(it, ThemeMode.fromStorage(it.name)) }
        assertEquals(null, ThemeMode.fromStorage("FOLLOW_SYSTEM"), "旧的 ordinal / 大写名不认")
    }
}
