package io.github.wingedpanther.cleanmytv.home

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

class CinemaConfigTest {
    @Test fun `cinema survives a save without changing app groups or icon preferences`() {
        val base = HomeConfig(
            order = listOf("com.netflix.ninja", "org.videolan.vlc"),
            hidden = setOf("com.vendor.ads"),
            rows = listOf(Group("Films", listOf("org.videolan.vlc"))),
            folders = listOf(Group("Outils", listOf("com.android.settings"))),
            iconStyle = "mono", tileShape = "square"
        )
        val cinema = HomeConfig.fromJson(JSONObject().put("layout", "cinema"), base)
        assertEquals("cinema", cinema.layout)
        assertEquals(base.order, cinema.order)
        assertEquals(base.hidden, cinema.hidden)
        assertEquals(base.rows, cinema.rows)
        assertEquals(base.folders, cinema.folders)
        assertEquals("mono", cinema.iconStyle)
        assertEquals("square", cinema.tileShape)
        assertEquals(cinema, HomeConfig.fromJson(cinema.toJson()))
    }

    @Test fun `old configurations keep their layout and invalid layouts do not replace it`() {
        for (layout in listOf("shelf", "split", "rail", "cinema", "horizon", "epure")) {
            val base = HomeConfig(layout = layout)
            assertEquals(layout, HomeConfig.fromJson(JSONObject(), base).layout)
            assertEquals(layout, HomeConfig.fromJson(JSONObject().put("layout", "invalid"), base).layout)
        }
        assertEquals("shelf", HomeConfig.fromJson(JSONObject()).layout)
    }
}
