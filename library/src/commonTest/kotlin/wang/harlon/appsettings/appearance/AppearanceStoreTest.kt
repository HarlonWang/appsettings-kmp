package wang.harlon.appsettings.appearance

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AppearanceStoreTest {

    private val options = AppearanceOptions()

    @Test
    fun fresh_install_uses_defaults_and_writes_nothing() {
        val settings = MapSettings()
        val store = AppearanceStore(settings, options)
        assertEquals(Appearance(ThemeMode.FollowSystem, ColorChoice.Preset("default")), store.appearance.value)
        assertEquals(null, settings.getStringOrNull(DefaultAppearanceKey))
    }

    @Test
    fun round_trips_every_field_through_json() {
        val settings = MapSettings()
        val custom = CustomColor(0xFF123456L, ThemeStyle.Bold, ThemeContrast.High)
        AppearanceStore(settings, options).update {
            Appearance(ThemeMode.Amoled, ColorChoice.Custom(custom), customHistory = listOf(custom, CustomColor(0xFF654321L)))
        }
        val raw = assertNotNull(settings.getStringOrNull(DefaultAppearanceKey))
        assertTrue(raw.contains("\"schemaVersion\":1"), raw)
        assertTrue(raw.contains("\"Amoled\""), "模式按名存")

        val reread = AppearanceStore(settings, options).appearance.value
        assertEquals(ThemeMode.Amoled, reread.mode)
        assertEquals(ColorChoice.Custom(custom), reread.color)
        assertEquals(listOf(custom, CustomColor(0xFF654321L)), reread.customHistory)
    }

    @Test
    fun preset_and_system_choices_round_trip() {
        val settings = MapSettings()
        val store = AppearanceStore(settings, options)
        store.update { it.copy(color = ColorChoice.Preset("pine")) }
        assertEquals(ColorChoice.Preset("pine"), AppearanceStore(settings, options).appearance.value.color)
        store.update { it.copy(color = ColorChoice.SystemDynamic) }
        assertEquals(ColorChoice.SystemDynamic, AppearanceStore(settings, options).appearance.value.color)
    }

    @Test
    fun corrupted_or_foreign_json_falls_back_to_defaults() {
        listOf("{not json", "[]", """{"schemaVersion":99,"mode":"Dark","color":{"type":"martian"}}""").forEach { raw ->
            val settings = MapSettings().apply { putString(DefaultAppearanceKey, raw) }
            val a = AppearanceStore(settings, options).appearance.value
            assertEquals(ColorChoice.Preset("default"), a.color, raw)
        }
    }

    @Test
    fun fields_fall_back_independently() {
        val raw = """{"schemaVersion":1,"mode":"Dark","color":{"type":"preset","id":"no-such-preset"},"customHistory":[{"argb":1,"style":"vivid","contrast":"high"}]}"""
        val settings = MapSettings().apply { putString(DefaultAppearanceKey, raw) }
        val a = AppearanceStore(settings, options).appearance.value
        assertEquals(ThemeMode.Dark, a.mode, "未知预设不连累模式")
        assertEquals(ColorChoice.Preset("default"), a.color)
        assertEquals(listOf(CustomColor(1L, ThemeStyle.Vivid, ThemeContrast.High)), a.customHistory)
    }

    @Test
    fun choices_not_allowed_by_options_fall_back() {
        val strict = AppearanceOptions(modes = listOf(ThemeMode.Light, ThemeMode.Dark), allowCustom = false, allowSystemDynamic = false)
        val raw = """{"schemaVersion":1,"mode":"FollowSystem","color":{"type":"system"}}"""
        val settings = MapSettings().apply { putString(DefaultAppearanceKey, raw) }
        val a = AppearanceStore(settings, strict).appearance.value
        assertEquals(ThemeMode.Light, a.mode, "modes 里没有跟随系统时取第一个")
        assertEquals(ColorChoice.Preset("default"), a.color)
    }

    @Test
    fun history_is_capped_on_read() {
        val entries = (1..15).joinToString(",") { """{"argb":$it,"style":"soft","contrast":"standard"}""" }
        val raw = """{"schemaVersion":1,"mode":"Light","color":{"type":"preset","id":"default"},"customHistory":[$entries]}"""
        val settings = MapSettings().apply { putString(DefaultAppearanceKey, raw) }
        assertEquals(10, AppearanceStore(settings, options).appearance.value.customHistory.size)
    }
}
