package wang.harlon.appsettings

import wang.harlon.appsettings.appearance.ColorChoice
import wang.harlon.appsettings.appearance.ThemePreset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AppSettingsConfigTest {

    private val brand = listOf(ThemePreset("brand", 0xFF112233L, label = { "Brand" }), ThemePreset("alt", 0xFF445566L, label = { "Alt" }))

    @Test
    fun custom_palette_normalizes_stale_default_preset() {
        val config = AppearanceConfig().apply { palette = brand }
        assertEquals(ColorChoice.Preset("brand"), config.toOptions().defaultColor, "只换 palette 时默认的 Preset(default) 规范化为第一项")
    }

    @Test
    fun explicit_default_in_palette_is_kept() {
        val config = AppearanceConfig().apply { palette = brand; defaultColor = ColorChoice.Preset("alt") }
        assertEquals(ColorChoice.Preset("alt"), config.toOptions().defaultColor)
    }

    @Test
    fun options_reject_default_preset_outside_palette() {
        assertFailsWith<IllegalArgumentException> {
            wang.harlon.appsettings.appearance.AppearanceOptions(palette = brand, defaultColor = ColorChoice.Preset("nope"))
        }
    }
}
