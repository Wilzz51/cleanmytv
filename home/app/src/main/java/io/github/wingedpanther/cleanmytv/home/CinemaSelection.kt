package io.github.wingedpanther.cleanmytv.home

/** Select only from the current visible apps, including after an uninstall, hide or app-lock change. */
object CinemaSelection {
    fun pick(focused: String?, visible: List<String>, firstKey: String?): String? {
        if (focused != null && focused in visible) return focused
        val first = firstKey?.substringAfter(':', "")
        return first?.takeIf { it in visible } ?: visible.firstOrNull()
    }
}
