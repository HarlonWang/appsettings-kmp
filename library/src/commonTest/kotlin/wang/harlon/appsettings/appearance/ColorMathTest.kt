package wang.harlon.appsettings.appearance

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ColorMathTest {

    @Test
    fun parses_hex_with_and_without_hash() {
        assertEquals(0xFF6750A4L, parseHexColor("#6750A4"))
        assertEquals(0xFF6750A4L, parseHexColor("6750A4"))
        assertEquals(0xFF6750A4L, parseHexColor("  6750a4  "))
    }

    @Test
    fun rejects_malformed_hex() {
        assertNull(parseHexColor(""))
        assertNull(parseHexColor("6750A"))
        assertNull(parseHexColor("6750A4F"))
        assertNull(parseHexColor("GGGGGG"))
        assertNull(parseHexColor("FF6750A4"), "8 位带 alpha 不接受：seed 必须不透明")
    }

    @Test
    fun formats_hex_uppercase_and_padded() {
        assertEquals("6750A4", formatHexColor(0xFF6750A4L))
        assertEquals("0000FF", formatHexColor(0xFF0000FFL))
        assertEquals("000000", formatHexColor(0xFF000000L))
    }

    @Test
    fun hex_round_trips() {
        listOf(0xFF6750A4L, 0xFFDC362EL, 0xFFFFB300L, 0xFF2E7D32L, 0xFF1976D2L, 0xFF3A3A3CL).forEach { argb ->
            assertEquals(argb, parseHexColor(formatHexColor(argb)))
        }
    }

    @Test
    fun hsv_round_trips_for_all_presets() {
        DefaultPalette.forEach { preset ->
            val back = hsvToArgb(argbToHsv(preset.argb))
            listOf(16, 8, 0).forEach { shift ->
                val e = ((preset.argb shr shift) and 0xFF).toInt()
                val a = ((back shr shift) and 0xFF).toInt()
                assertTrue(abs(e - a) <= 1, "${preset.id} 通道差异过大")
            }
        }
    }

    @Test
    fun hsv_handles_pure_and_achromatic_colors() {
        assertEquals(0f, argbToHsv(0xFFFF0000L).hue)
        assertEquals(120f, argbToHsv(0xFF00FF00L).hue)
        assertEquals(240f, argbToHsv(0xFF0000FFL).hue)
        val white = argbToHsv(0xFFFFFFFFL)
        assertEquals(0f, white.saturation)
        assertEquals(1f, white.value)
        val black = argbToHsv(0xFF000000L)
        assertEquals(0f, black.saturation)
        assertEquals(0f, black.value)
    }

    @Test
    fun hsv_to_argb_clamps_and_stays_opaque() {
        assertEquals(0xFF000000L, hsvToArgb(Hsv(hue = 400f, saturation = 1.5f, value = -0.2f)))
        assertTrue(hsvToArgb(Hsv(-60f, 1f, 1f)) != 0L)
        assertEquals(0xFF000000L, hsvToArgb(Hsv(210f, 0.5f, 0.5f)) and 0xFF000000L)
    }
}
