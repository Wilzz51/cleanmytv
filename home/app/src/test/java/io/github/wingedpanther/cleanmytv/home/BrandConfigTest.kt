package io.github.wingedpanther.cleanmytv.home

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class BrandConfigTest {
    @Test fun `old settings keep the default wordmark visible`() {
        val config = HomeConfig.fromJson(JSONObject())
        assertTrue(config.showBrand)
        assertEquals("CleanMyTV", config.brandLabel)
    }

    @Test fun `hiding preserves custom text and other header labels through save and layout changes`() {
        val custom = HomeConfig(names = mapOf("brand" to "Mon salon", "wifi" to "Réseau"))
        val hidden = HomeConfig.fromJson(JSONObject().put("show_brand", false), custom)
        assertFalse(hidden.showBrand)
        assertEquals("Mon salon", hidden.brandLabel)
        assertEquals(hidden, HomeConfig.fromJson(hidden.toJson()))
        for (layout in listOf("cinema", "horizon", "epure")) {
            val changed = HomeConfig.fromJson(JSONObject().put("layout", layout), hidden)
            assertFalse(changed.showBrand)
            assertEquals(custom.names, changed.names)
            assertEquals("Mon salon", HomeConfig.fromJson(JSONObject().put("show_brand", true), changed).brandLabel)
        }
    }

    @Test fun `empty text restores the default while preserving other labels`() {
        val names = JSONObject().put("brand", "  ").put("wifi", "Réseau")
        val config = HomeConfig.fromJson(JSONObject().put("names", names))
        assertEquals("CleanMyTV", config.brandLabel)
        assertEquals(mapOf("wifi" to "Réseau"), config.names)
        assertEquals(24, HomeConfig.fromJson(JSONObject().put("names", JSONObject().put("brand", "x".repeat(40)))).brandLabel.length)
    }
}
