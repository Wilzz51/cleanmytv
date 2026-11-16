package io.github.wingedpanther.cleanmytv.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CinemaSelectionTest {
    private val netflix = "com.netflix.ninja"
    private val youtube = "com.google.android.youtube.tv"

    @Test fun `spotlight follows the app that has focus`() {
        assertEquals(youtube, CinemaSelection.pick(youtube, listOf(netflix, youtube), "dock:$netflix"))
    }

    @Test fun `initial spotlight follows the first enabled section rather than alphabetical order`() {
        assertEquals(youtube, CinemaSelection.pick(null, listOf(netflix, youtube), "row0:$youtube"))
    }

    @Test fun `a hidden locked or uninstalled app cannot stay in the spotlight`() {
        assertEquals(netflix, CinemaSelection.pick(youtube, listOf(netflix), "dock:$netflix"))
        assertEquals(netflix, CinemaSelection.pick(youtube, listOf(netflix), "grid:$youtube"))
    }

    @Test fun `empty app list and absent sections have a safe fallback`() {
        assertNull(CinemaSelection.pick(netflix, emptyList(), "dock:$netflix"))
        assertEquals(netflix, CinemaSelection.pick(null, listOf(netflix), null))
    }
}
