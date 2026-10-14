package wang.harlon.appsettings.whatsnew

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WhatsNewTest {

    // shouldShowWhatsNew

    @Test
    fun first_install_does_not_show() {
        assertFalse(shouldShowWhatsNew("0.9.0", "0.9.0", lastSeenVersion = null))
    }

    @Test
    fun upgrade_with_matching_bundle_shows() {
        assertTrue(shouldShowWhatsNew("0.9.0", "0.9.0", lastSeenVersion = "0.8.0"))
    }

    @Test
    fun same_version_does_not_show() {
        assertFalse(shouldShowWhatsNew("0.9.0", "0.9.0", lastSeenVersion = "0.9.0"))
    }

    @Test
    fun bundled_placeholder_does_not_show() {
        assertFalse(shouldShowWhatsNew("0.9.0", "0.0.0", lastSeenVersion = "0.8.0"))
    }

    @Test
    fun prerelease_suffix_requires_exact_match() {
        assertTrue(shouldShowWhatsNew("0.9.0-beta", "0.9.0-beta", lastSeenVersion = "0.8.0"))
        assertFalse(shouldShowWhatsNew("0.9.0-beta", "0.9.0", lastSeenVersion = "0.8.0"))
    }

    // parseWhatsNew

    @Test
    fun parse_full_payload() {
        val info = parseWhatsNew("""{"version":"0.9.0","zh":["修复崩溃"],"en":["Fix crash"]}""")
        assertEquals("0.9.0", info?.version)
        assertEquals(listOf("修复崩溃"), info?.notes?.get("zh"))
        assertEquals(listOf("Fix crash"), info?.notes?.get("en"))
        assertFalse(info!!.isEmpty)
    }

    @Test
    fun parse_placeholder_is_empty() {
        assertTrue(parseWhatsNew("""{"version":"0.0.0","zh":[],"en":[]}""")!!.isEmpty)
    }

    @Test
    fun parse_missing_fields_falls_back_to_defaults() {
        val info = parseWhatsNew("""{"version":"0.9.0"}""")
        assertEquals("0.9.0", info?.version)
        assertTrue(info!!.isEmpty)
    }

    @Test
    fun parse_ignores_non_array_keys() {
        val info = parseWhatsNew("""{"version":"0.9.0","zh":["a"],"en":["b"],"extra":1,"obj":{"x":1}}""")
        assertEquals(setOf("zh", "en"), info?.notes?.keys)
    }

    @Test
    fun parse_keeps_any_language_key() {
        val info = parseWhatsNew("""{"version":"1.0.0","ja":["修正"],"en":["Fix"]}""")
        assertEquals(listOf("修正"), info?.notes?.get("ja"))
    }

    @Test
    fun parse_invalid_json_returns_null() {
        assertNull(parseWhatsNew("not json"))
        assertNull(parseWhatsNew(""))
        assertNull(parseWhatsNew("[1,2]"))
    }

    // pick

    private val zhEn = WhatsNewInfo("1.0.0", mapOf("zh" to listOf("中文"), "en" to listOf("en")))

    @Test
    fun exact_and_prefix_match() {
        assertEquals(listOf("中文"), zhEn.pick("zh"))
        assertEquals(listOf("中文"), zhEn.pick("zh-Hans"))
        assertEquals(listOf("中文"), zhEn.pick("zh_CN"))
        assertEquals(listOf("en"), zhEn.pick("en-US"))
    }

    @Test
    fun unknown_language_falls_back_to_en() {
        assertEquals(listOf("en"), zhEn.pick("ja"))
    }

    @Test
    fun empty_preferred_list_falls_back_to_other() {
        assertEquals(listOf("en"), WhatsNewInfo("1", mapOf("zh" to emptyList(), "en" to listOf("en"))).pick("zh"))
        assertEquals(listOf("中文"), WhatsNewInfo("1", mapOf("zh" to listOf("中文"), "en" to emptyList())).pick("en"))
    }

    @Test
    fun no_en_falls_back_to_first_non_empty() {
        assertEquals(listOf("修正"), WhatsNewInfo("1", mapOf("ja" to listOf("修正"))).pick("de"))
    }

    @Test
    fun all_empty_returns_empty() {
        assertEquals(emptyList(), WhatsNewInfo("1", mapOf("zh" to emptyList())).pick("zh"))
        assertEquals(emptyList(), WhatsNewInfo("1", emptyMap()).pick("zh"))
    }
}
