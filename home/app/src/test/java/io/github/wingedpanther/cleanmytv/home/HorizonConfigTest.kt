package io.github.wingedpanther.cleanmytv.home

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

class HorizonConfigTest {
    @Test fun `Horizon round trip preserves favourites groups privacy and look`() {
        val cinema = HomeConfig(layout = "cinema", dockSize = 4,
            order = listOf("com.netflix.ninja", "org.videolan.vlc"), hidden = setOf("com.vendor.ads"),
            rows = listOf(Group("Films", listOf("com.netflix.ninja"))),
            folders = listOf(Group("Outils", listOf("org.videolan.vlc"))),
            lockOn = true, lockApps = listOf("com.netflix.ninja"), lockShow = "hide",
            accent = 0xFF3399FF, dockScale = 80, iconStyle = "mono", tileShape = "square")
        for (layout in listOf("horizon", "epure")) {
            val alternative = HomeConfig.fromJson(JSONObject().put("layout", layout), cinema)
            assertEquals(cinema.copy(layout = layout), alternative)
            assertEquals(alternative, HomeConfig.fromJson(alternative.toJson()))
            assertEquals(cinema, HomeConfig.fromJson(JSONObject().put("layout", "cinema"), alternative))
        }
    }

    @Test fun `Horizon does not force the dock clock or media back on`() {
        val base = HomeConfig(clock = "off", dockSize = 0, sections = emptyList())
        for (layout in listOf("horizon", "epure")) {
            assertEquals(base.copy(layout = layout), HomeConfig.fromJson(JSONObject().put("layout", layout), base))
        }
    }
}
