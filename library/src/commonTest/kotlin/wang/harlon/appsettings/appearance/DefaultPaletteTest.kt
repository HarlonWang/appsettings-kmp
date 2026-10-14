package wang.harlon.appsettings.appearance

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DefaultPaletteTest {

    @Test
    fun palette_has_fourteen_unique_opaque_seeds() {
        assertEquals(14, DefaultPalette.size)
        assertEquals(DefaultPalette.size, DefaultPalette.map { it.id }.toSet().size, "duplicate id")
        assertEquals(DefaultPalette.size, DefaultPalette.map { it.argb }.toSet().size, "duplicate argb")
        DefaultPalette.forEach { assertEquals(0xFFL, (it.argb shr 24) and 0xFFL, "${it.id} not opaque") }
    }

    @Test
    fun first_entry_is_the_default_seed() {
        assertEquals("default", DefaultPalette.first().id)
        assertEquals(DefaultSeedArgb, DefaultPalette.first().argb)
    }

    @Test
    fun stable_ids_are_unchanged() {
        // id 是稳定契约：改了等于让已选它的用户掉到默认
        assertEquals(
            listOf("default", "plum", "berry", "rust", "mustard", "olive", "forest", "pine", "peacock", "steel", "indigo", "brown", "slate", "graphite"),
            DefaultPalette.map { it.id },
        )
        assertTrue(AppearanceOptions().defaultColor == ColorChoice.Preset("default"))
    }
}
