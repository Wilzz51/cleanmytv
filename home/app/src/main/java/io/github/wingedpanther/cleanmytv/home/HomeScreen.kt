package io.github.wingedpanther.cleanmytv.home

import android.graphics.BitmapFactory
import android.util.Log
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AcUnit
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Dehaze
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.OpenWith
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.Thunderstorm
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.WbCloudy
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.zIndex
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Glow
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.util.Calendar
import java.util.Date

// CleanMyTV's dark world: warm off-white and sand text on the green-black ground.
private val TEXT = Color(0xFFF0EBE5)
private val MUTED = Color(0xFFD7CFC4)
private val TILE = Color(0xE62D3A2E)
private val TILE_FOCUSED = Color(0xFF3A4A3B)

/** A focused row in Home's menus: the accent, mixed into the tile so the light text still reads (#112). */
private fun menuFocus(ui: Ui): Color = lerp(if (ui.cfg.layout == "cinema") Color(0xFF1C212C) else TILE, ui.accent, 0.45f)
private val GROUND = Color(0xFF1C2A1F)
private val PANEL = Color(0xFF2D3A2E)

// The desktop app's face, for the clock and date: tabular figures that don't jitter as the minutes turn.
private val CLOCK = FontFamily(Font(R.font.jetbrains_mono_light, FontWeight.Light), Font(R.font.jetbrains_mono_regular, FontWeight.Normal))

/** Text on an accent fill: the ground colour or off-white, whichever reads better on it. */
private fun onAccent(c: Color): Color = if (c.luminance() > 0.36f) GROUND else TEXT

/**
 * A shade in the background colour, so overlays keep the theme's own dark instead of plain black. Before
 * 1.7.5 it was always the default green-black, which tinted a black background green (#102). A light
 * background is darkened first, so text over the shade still reads.
 */
private fun scrim(cfg: HomeConfig, alpha: Float): Color {
    val g = Color(cfg.background)
    return (if (g.luminance() > 0.2f) lerp(g, Color.Black, 0.75f) else g).copy(alpha = alpha)
}

// A 1080p TV is 960x540 dp; 5% of each edge stays clear because some TVs crop it.
private val SAFE_X = 48.dp
private val SAFE_Y = 27.dp
private val GAP = 16.dp
// Room on each side of a row so a focused tile's glow, shadow and zoom aren't clipped.
private val BLEED = 22.dp

/** The sunrise after a new wallpaper, in its own scope so its frames don't recompose the screen. */
@Composable
private fun IntroLayer(intro: Animatable<Float, *>) {
    if (intro.value < 1f) Sunrise(intro.value)
}

/** Everything the layouts draw, worked out once from the apps on the TV and the settings. */
private class Home(all: List<TvApp>, val cfg: HomeConfig, recentPkgs: List<String> = emptyList()) {
    // "Hide locked apps": they stay out of every section, and only open from elsewhere with the PIN.
    private val apps = if (cfg.lockOn && cfg.lockShow == "hide") all.filter { it.pkg !in cfg.lockApps } else all
    private val byPkg = apps.associateBy { it.pkg }
    val shown = arrange(apps, cfg)
    private val inFolders = cfg.folders.flatMap { it.apps }.toSet()
    /** Phone apps with no TV banner, in their own folder when the owner asks (#3). Apps they placed stay put. */
    private val other = if (cfg.otherApps) shown.filter { !it.banner && it.pkg !in inFolders } else emptyList()
    val dock = shown.filter { it.pkg !in inFolders && it !in other }.take(cfg.dockSize)
    val grid = shown.filter { it.pkg !in inFolders && it !in dock && it !in other }
    val rows = cfg.rows.map { g -> g.name to g.apps.mapNotNull { byPkg[it] }.filter { it.pkg !in cfg.hidden } }.filter { it.second.isNotEmpty() }
    val folders = (cfg.folders.map { g -> g.name to g.apps.mapNotNull { byPkg[it] } } + listOf("Other apps" to other)).filter { it.second.isNotEmpty() }
    val recent = recentPkgs.mapNotNull { byPkg[it] }.filter { it.pkg !in cfg.hidden }
    /** The tile that takes focus when Home opens, as "<section>:<package>" (an app can sit in two sections). */
    val first: String? = if (cfg.layout == "horizon") {
        if ("dock" in cfg.sections) dock.firstOrNull()?.let { "dock:${it.pkg}" } else null
    } else cfg.sections.firstNotNullOfOrNull { s ->
        when (s) {
            "recent" -> recent.firstOrNull()?.let { "recent:${it.pkg}" }
            "dock" -> dock.firstOrNull()?.let { "dock:${it.pkg}" }
            "rows" -> rows.firstOrNull()?.second?.firstOrNull()?.let { "row0:${it.pkg}" }
            "grid" -> grid.firstOrNull()?.let { "grid:${it.pkg}" }
            else -> null
        }
    }
}

@Composable
fun HomeScreen(visualiser: Visualiser, appsVersion: Int, open: androidx.compose.runtime.MutableState<String?>) {
    val ctx = LocalContext.current
    val cfg by ConfigStore.config.collectAsState()
    val wallVersion by ConfigStore.wallpaperVersion.collectAsState()
    var apps by remember { mutableStateOf<List<TvApp>>(emptyList()) }
    var inputs by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    val glyphs = cfg.iconStyle != "banner" || cfg.layout in setOf("cinema", "horizon", "epure")
    LaunchedEffect(appsVersion, glyphs, cfg.liveTv) {
        apps = withContext(Dispatchers.Default) { loadApps(ctx, glyphs) }
        inputs = withContext(Dispatchers.Default) { tvInputs(ctx, cfg.liveTv) }
    }
    // Animations off in the TV's settings (or from CleanMyTV): Home stops moving too.
    var motion by remember { mutableStateOf(true) }
    LaunchedEffect(appsVersion) {
        motion = runCatching { android.provider.Settings.Global.getFloat(ctx.contentResolver, android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f) }.getOrDefault(1f) > 0f
    }
    var wallpaper by remember { mutableStateOf<ImageBitmap?>(null) }
    val intro = remember { Animatable(1f) }
    var wallSeen by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(wallVersion) {
        wallpaper = withContext(Dispatchers.IO) {
            val f = ConfigStore.wallpaper(ctx)
            if (f.exists()) runCatching { BitmapFactory.decodeFile(f.path)?.asImageBitmap() }.getOrNull() else null
        }
        // A new wallpaper, not the one Home opened with, comes in like a sunrise.
        val fresh = wallSeen != null && wallSeen != wallVersion
        wallSeen = wallVersion
        if (fresh && wallpaper != null && cfg.intro && motion) { intro.snapTo(0f); intro.animateTo(1f, tween(1800, easing = FastOutSlowInEasing)) }
    }
    // Slideshow: a new wallpaper every few minutes while Home is open, from the owner's pictures or the theme's.
    LaunchedEffect(cfg.slideMinutes, cfg.slideFrom) {
        if (cfg.slideMinutes <= 0) return@LaunchedEffect
        while (true) {
            delay(cfg.slideMinutes * 60_000L)
            withContext(Dispatchers.IO) { runCatching { if (cfg.slideFrom == "theme") ThemePack.next(ctx) else Slides.next(ctx) } }
        }
    }
    val fireVersion by Fire.version.collectAsState()
    val fireWall = cfg.fireWall && remember(fireVersion) { Fire.ready(ctx) }
    // Idle screen: no button pressed for cfg.idleMinutes, and nothing open over Home. Any button wakes it,
    // and that press only wakes it, so it can't open the app that happens to have focus.
    var idle by remember { mutableStateOf(false) }
    var lastPress by remember { mutableLongStateOf(0L) }
    var swallowUp by remember { mutableStateOf(false) }
    LaunchedEffect(appsVersion) { idle = false; lastPress = System.currentTimeMillis() } // Home came back into view

    LaunchedEffect(cfg.vizBars, cfg.vizSensitivity, cfg.visualiser, idle) {
        visualiser.bars = cfg.vizBars
        visualiser.sensitivity = cfg.vizSensitivity / 100f
        if (cfg.visualiser && !idle) visualiser.start(ctx) else visualiser.stop()
    }

    val track by NowPlaying.track.collectAsState()
    var weather by remember { mutableStateOf<WeatherNow?>(null) }
    LaunchedEffect(cfg.weatherPlace, cfg.weatherLat, cfg.weatherLon, cfg.weatherUnit) {
        weather = null
        if (cfg.weatherPlace.isBlank()) return@LaunchedEffect
        // A failed refresh keeps the last reading; the next try is in 30 minutes, or 2 if there's none yet.
        while (true) {
            Weather.fetch(cfg)?.let { weather = it }
            delay(if (weather == null) 2 * 60_000L else 30 * 60_000L)
        }
    }

    // Free memory under the clock, read every 10 seconds; Clean memory reads it again straight away.
    var memory by remember { mutableStateOf<MemoryNow?>(null) }
    var memoryTick by remember { mutableIntStateOf(0) }
    LaunchedEffect(cfg.memory, memoryTick, appsVersion) {
        memory = null
        if (!cfg.memory) return@LaunchedEffect
        while (true) {
            memory = withContext(Dispatchers.Default) { runCatching { Memory.read(ctx) }.getOrNull() }
            delay(10_000L)
        }
    }
    // Bing every day: looked at when Home comes back into view, and each hour while it stays.
    LaunchedEffect(cfg.bingDaily, appsVersion) {
        while (cfg.bingDaily) { Wallpapers.daily(ctx); delay(60 * 60_000L) }
    }
    // Day and night looks: checked each minute while Home is open, and when it comes back into view.
    LaunchedEffect(cfg.dayNight, cfg.dayAt, cfg.nightAt, appsVersion) {
        if (!cfg.dayNight) return@LaunchedEffect
        while (true) {
            val c = Calendar.getInstance()
            withContext(Dispatchers.IO) { runCatching { DayNight.check(ctx, c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE)) } }
            delay(60_000L - System.currentTimeMillis() % 60_000L)
        }
    }
    val scope = rememberCoroutineScope()

    // Read again each time Home comes back, which is when the list can have changed.
    var recentPkgs by remember { mutableStateOf<List<String>>(emptyList()) }
    val wantRecent = "recent" in cfg.sections
    LaunchedEffect(appsVersion, wantRecent, apps) {
        if (wantRecent) recentPkgs = withContext(Dispatchers.Default) { Recent.apps(ctx, apps.map { it.pkg }.toSet()) }
    }
    // Worked out again only when the apps or settings change, not on every redraw.
    val home = remember(apps, cfg, recentPkgs, wantRecent) { Home(apps, cfg, if (wantRecent) recentPkgs else emptyList()) }
    val accent = Color(cfg.accent)
    // Quick-settings icons this TV can actually open; Inputs only on TVs that have inputs.
    val all = remember { quickActions() }
    // App shortcuts use the app's icon; a TV app's banner is too wide for the round button.
    val quickPkgs = cfg.quick.mapNotNull { HomeConfig.quickApp(it) }
    var quickIcons by remember { mutableStateOf<Map<String, ImageBitmap>>(emptyMap()) }
    LaunchedEffect(appsVersion, quickPkgs) {
        quickIcons = withContext(Dispatchers.IO) {
            quickPkgs.mapNotNull { p ->
                runCatching { p to ctx.packageManager.getApplicationIcon(p).toBitmap(96, 96).asImageBitmap() }.getOrNull()
            }.toMap()
        }
    }
    val byPkg = apps.associateBy { it.pkg }
    // A newer Home on the release page: its icon goes first in the bar until it's installed, skipped or put off.
    val update by Updates.available.collectAsState()
    LaunchedEffect(appsVersion, cfg.updateCheck) {
        val r = Updates.check(ctx) ?: return@LaunchedEffect
        if (Updates.announce(ctx, r)) android.widget.Toast.makeText(ctx, "CleanMyTV Home ${r.version} is out. The update icon is first in the top bar.", android.widget.Toast.LENGTH_LONG).show()
    }
    val quick = listOfNotNull(all["update"]?.takeIf { update != null }) + cfg.quick.mapNotNull { id ->
        HomeConfig.quickApp(id)?.let { p -> byPkg[p]?.let { appAction(it, quickIcons[p]) } } ?: all[id]?.let { a -> cfg.names[id]?.let { a.copy(label = it) } ?: a }
    }.filter { canOpen(ctx, it, inputs.isNotEmpty()) }
    var openFolder by remember { mutableStateOf<Pair<String, List<TvApp>>?>(null) }
    // Long press on a tile: its menu, and Move, which hands the arrow keys to the tile until OK or Back.
    var menuFor by remember { mutableStateOf<Pair<String, TvApp>?>(null) }
    var moving by remember { mutableStateOf<Moving?>(null) }
    // After a move, Home leaves focus on the app that moved instead of going back to the first tile.
    var keepFocus by remember { mutableStateOf(false) }
    LaunchedEffect(appsVersion) { moving = null; keepFocus = false }
    var showInputs by remember { mutableStateOf(false) }
    var showPanel by remember { mutableStateOf(false) }
    var showThemes by remember { mutableStateOf(false) }
    var showTimer by remember { mutableStateOf(false) }
    var showUpdate by remember { mutableStateOf(false) }
    var panelPin by remember { mutableStateOf(false) }
    val openPanel = { if (Lock.needsPin(ctx, Lock.PANEL)) panelPin = true else showPanel = true }
    // App lock asks for the PIN here when its "Customize Home" switch is on.
    var editPin by remember { mutableStateOf(false) }
    val openEditor = { if (Lock.needsPin(ctx, Lock.EDITOR)) editPin = true else openEditorNow(ctx) }
    LaunchedEffect(lastPress, cfg.idleMinutes, openFolder, showInputs, showPanel, showThemes) {
        if (cfg.idleMinutes == 0 || openFolder != null || showInputs || showPanel || showThemes) return@LaunchedEffect
        delay(cfg.idleMinutes * 60_000L)
        idle = true
    }
    val shown by animateFloatAsState(if (idle) 0f else 1f, tween(if (idle) 1200 else 250), label = "idle")
    LaunchedEffect(open.value) {
        when (open.value) {
            "inputs" -> showInputs = true
            "panel" -> openPanel()
            "themes" -> showThemes = true
            "customize" -> openEditor()
            "timer" -> showTimer = true
            "update" -> showUpdate = true
            "fireplace" -> runCatching { ctx.startActivity(android.content.Intent(ctx, FireplaceActivity::class.java)) }
        }
        open.value = null
    }
    val onQuick: (QuickAction) -> Unit = { a ->
        if (a.id == "inputs") showInputs = true
        else if (a.id == "panel") openPanel()
        else if (a.id == "themes") showThemes = true
        else if (a.id == "customize") openEditor()
        else if (a.id == "timer") showTimer = true
        else if (a.id == "update") showUpdate = true
        else if (a.id == "fireplace") runCatching { ctx.startActivity(android.content.Intent(ctx, FireplaceActivity::class.java)) }
        else if (a.id == "memory") scope.launch { Memory.cleanAndSay(ctx); memoryTick++ }
        else if (a.id == "music") ConfigStore.update(ctx, org.json.JSONObject().put("visualiser", !cfg.visualiser))
        else resolve(ctx, a)?.let { i -> runCatching { ctx.startActivity(i) }.onFailure { Log.w(TAG, "${a.id}: $it") } }
    }
    val first = remember { FocusRequester() }
    LaunchedEffect(home.first, cfg.layout) { if (home.first != null && !keepFocus) runCatching { first.requestFocus() } }
    // While an app moves, the tile that holds focus is the moving one, wherever its new place is.
    val movingKey = moving?.let { m -> if (m.kind == "main") "${if (home.dock.any { it.pkg == m.pkg }) "dock" else "grid"}:${m.pkg}" else "${m.kind}:${m.pkg}" }
    LaunchedEffect(movingKey, cfg.order, cfg.rows) {
        if (movingKey == null) return@LaunchedEffect
        repeat(5) { delay(40); runCatching { first.requestFocus() } }
    }
    fun step(by: Int) {
        val m = moving ?: return
        val change = if (m.kind == "main") {
            val movable = if (cfg.layout == "horizon") home.dock else home.dock + home.grid
            JSONObject().put("order", JSONArray(Reorder.order(cfg.order, movable.map { it.pkg }, m.pkg, by)))
        } else {
            val (name, list) = home.rows.getOrNull(m.kind.removePrefix("row").toIntOrNull() ?: -1) ?: return
            val rows = cfg.rows.map { g -> if (g.name == name) Group(g.name, Reorder.row(g.apps, list.map { it.pkg }, m.pkg, by)) else g }
            JSONObject().put("rows", JSONArray(rows.map { g -> JSONObject().put("name", g.name).put("apps", JSONArray(g.apps)) }))
        }
        ConfigStore.update(ctx, change)
    }
    var focusColour by remember { mutableStateOf<Color?>(null) }
    var cinemaFocus by remember { mutableStateOf<String?>(null) }
    // Resolve against the visible model on every app-list change: hidden/removed apps never stay actionable.
    val cinemaApp = remember(home, cinemaFocus) {
        val pkg = CinemaSelection.pick(cinemaFocus, home.shown.map { it.pkg }, home.first)
        home.shown.firstOrNull { it.pkg == pkg }
    }
    val tint by animateColorAsState(focusColour ?: Color.Transparent, tween(600), label = "tint")
    val ui = Ui(cfg, accent, first, movingKey ?: home.first, onFolder = { openFolder = it }, track = track, weather = weather, memory = memory,
        onFocusApp = { a -> focusColour = a.colour?.let { Color(it) }; if (cfg.layout == "cinema") cinemaFocus = a.pkg },
        motion = motion, onAppMenu = { s, a -> menuFor = s to a }, movingKey = movingKey, cinemaApp = cinemaApp)

    Box(
        Modifier.fillMaxSize().background(Color(cfg.background)).onPreviewKeyEvent { e ->
            lastPress = System.currentTimeMillis()
            if (moving != null) {
                val inGrid = movingKey?.startsWith("grid:") == true
                if (e.type == KeyEventType.KeyDown) when (e.key) {
                    Key.DirectionLeft -> step(-1)
                    Key.DirectionRight -> step(1)
                    Key.DirectionUp -> if (inGrid) step(-cfg.columns)
                    Key.DirectionDown -> if (inGrid) step(cfg.columns)
                    else -> {}
                }
                if (e.type == KeyEventType.KeyUp && e.key in DONE_KEYS) { moving = null; keepFocus = true }
                return@onPreviewKeyEvent true
            }
            if (e.type == KeyEventType.KeyDown) Motion.seen(e.key)
            when {
                idle && e.type == KeyEventType.KeyDown -> { idle = false; swallowUp = true; true }
                swallowUp && e.type == KeyEventType.KeyUp -> { swallowUp = false; true }
                else -> false
            }
        }
    ) {
        if (fireWall) {
            FireWallpaper(cfg, motion)
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = cfg.wallpaperDim / 100f)))
        } else wallpaper?.let {
            Image(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = cfg.wallpaperDim / 100f)))
        }
        IntroLayer(intro)
        // The focused app's colour washes in from the top; the wallpaper still shows through.
        // Drawn in the draw phase, so the fading colour doesn't redraw the whole screen each frame.
        if (cfg.followColour && cfg.wash > 0) Box(Modifier.fillMaxSize().drawBehind {
            drawRect(Brush.verticalGradient(0f to tint.copy(alpha = tint.alpha * cfg.wash / 100f), cfg.washReach / 100f to Color.Transparent))
        })
        Box(Modifier.fillMaxSize().graphicsLayer { alpha = shown }) {
            // Rows sit over the lower half of the photo; a ground-tinted shade keeps their names readable.
            if (cfg.shadeBottom > 0 && cfg.shadeBottomReach > 0) {
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(1f - cfg.shadeBottomReach / 100f to Color.Transparent, 1f to scrim(cfg, cfg.shadeBottom / 100f))))
            }
            when (cfg.layout) {
                "epure" -> EpureLayout(home, ui, quick, onQuick)
                "horizon" -> HorizonLayout(home, ui, quick, onQuick)
                "cinema" -> CinemaLayout(home, ui, quick, onQuick)
                "split" -> SplitLayout(home, ui, quick, onQuick)
                "rail" -> RailLayout(home, ui, quick, onQuick)
                else -> ShelfLayout(home, ui, quick, onQuick)
            }
            if (cfg.visualiser) Bars(visualiser, cfg, accent, Modifier.align(Alignment.BottomCenter).fillMaxWidth())
        }
        if (shown < 1f) IdleScreen(ui, Modifier.fillMaxSize().graphicsLayer { alpha = 1f - shown })
        moving?.let { m ->
            Text(
                "Moving ${m.label}. Use the arrows, then OK.",
                color = TEXT, fontSize = 16.sp,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = SAFE_Y).background(PANEL, RoundedCornerShape(12.dp)).padding(horizontal = 18.dp, vertical = 10.dp)
            )
        }
    }

    openFolder?.let { (name, list) -> FolderOverlay(name, list, ui) { openFolder = null } }
    if (showInputs) InputsOverlay(inputs, ui) { showInputs = false }
    menuFor?.let { (section, app) ->
        // Folders keep their order in the desktop app; the dock, All apps and rows move here.
        val kind = when {
            section == "dock" || section == "grid" -> "main"
            section.startsWith("row") -> section
            else -> null
        }
        AppMenu(app, ui, onMove = kind?.let { k -> { menuFor = null; openFolder = null; moving = Moving(k, app.pkg, app.label) } }) { menuFor = null }
    }
    if (showPanel) SettingsPanel(accent) { showPanel = false }
    if (showThemes) ThemeCarousel(accent) { showThemes = false }
    if (showTimer) TimerMenu(ui) { showTimer = false }
    if (showUpdate) update?.let { UpdateDialog(it, accent) { showUpdate = false } }
    if (panelPin) Dialog(onDismissRequest = { panelPin = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        PinPad("Enter the PIN for Quick settings", Lock.PANEL, accent) { panelPin = false; showPanel = true }
    }
    if (editPin) Dialog(onDismissRequest = { editPin = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        PinPad("Enter the PIN to customize Home", Lock.EDITOR, accent) { editPin = false; openEditorNow(ctx) }
    }
}

private fun openEditorNow(ctx: android.content.Context) {
    runCatching { ctx.startActivity(android.content.Intent(ctx, io.github.wingedpanther.cleanmytv.home.customization.CustomizationActivity::class.java)) }
        .onFailure { Log.w(TAG, "editor: $it") }
}

/**
 * The fireplace behind Home, in place of the picture. It plays while Home is on screen and holds still while
 * an app is open, or when the TV's animations are off. Silent unless the owner turned its sound on.
 */
@Composable
private fun FireWallpaper(cfg: HomeConfig, motion: Boolean) {
    val ctx = LocalContext.current
    val owner = ctx as? androidx.activity.ComponentActivity
    var resumed by remember { mutableStateOf(owner?.lifecycle?.currentState?.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED) ?: true) }
    DisposableEffect(owner) {
        val obs = androidx.lifecycle.LifecycleEventObserver { _, e ->
            if (e == androidx.lifecycle.Lifecycle.Event.ON_RESUME) resumed = true
            if (e == androidx.lifecycle.Lifecycle.Event.ON_PAUSE) resumed = false
        }
        owner?.lifecycle?.addObserver(obs)
        onDispose { owner?.lifecycle?.removeObserver(obs) }
    }
    val volume = if (cfg.fireWallSound) cfg.fireVolume / 100f else 0f
    androidx.compose.ui.viewinterop.AndroidView(
        factory = { FireView(it) { if (ConfigStore.config.value.fireWallSound) ConfigStore.config.value.fireVolume / 100f else 0f } },
        modifier = Modifier.fillMaxSize(),
        update = { v -> v.setVolume(volume); v.play(resumed && motion) },
        onRelease = { it.release() },
    )
}

/**
 * A new wallpaper's entrance: the picture starts in a cool, dim twilight, the light arrives from the top
 * down, and a soft band of light crosses it once. [p] runs from 0 to 1.
 */
@Composable
private fun Sunrise(p: Float) {
    Box(Modifier.fillMaxSize().drawWithContent {
        val dusk = Color(0xFF0A1424)
        val edge = -0.25f + p * 1.5f
        val a = (edge - 0.3f).coerceIn(0f, 0.999f)
        val b = edge.coerceIn(a + 0.001f, 1f)
        drawRect(Brush.verticalGradient(a to dusk.copy(alpha = 0f), b to dusk.copy(alpha = 0.92f), 1f to dusk.copy(alpha = 0.92f)))
        drawRect(dusk.copy(alpha = 0.35f * (1f - p)))
        val w = size.width
        val x = -w * 0.6f + p * w * 1.8f
        val glow = kotlin.math.sin(p * Math.PI).toFloat() * 0.16f
        drawRect(Brush.linearGradient(0f to Color.Transparent, 0.5f to Color.White.copy(alpha = glow), 1f to Color.Transparent,
            start = Offset(x, 0f), end = Offset(x + w * 0.35f, size.height)))
    })
}

/** What every tile and section needs to know. */
private class Ui(
    val cfg: HomeConfig,
    val accent: Color,
    val focus: FocusRequester,
    val focusKey: String?,
    val onFolder: (Pair<String, List<TvApp>>) -> Unit,
    val track: Track? = null,
    val weather: WeatherNow? = null,
    val memory: MemoryNow? = null,
    val onFocusApp: (TvApp) -> Unit = {},
    /** False when the TV's animations are off: tiles still lift, without springs, tilt or sheen. */
    val motion: Boolean = true,
    /** A long press on a tile, with the section it is in. */
    val onAppMenu: (String, TvApp) -> Unit = { _, _ -> },
    /** "<section>:<package>" of the app being moved, drawn with an outline. */
    val movingKey: String? = null,
    val cinemaApp: TvApp? = null,
) {
    /** Banners are always wide; the icon styles take the chosen shape. */
    val shape: String = if (cfg.layout in setOf("cinema", "horizon", "epure")) "round" else if (cfg.iconStyle == "banner") "wide" else cfg.tileShape
    fun focusFor(section: String, app: TvApp): Modifier = if ("$section:${app.pkg}" == focusKey) Modifier.focusRequester(focus) else Modifier
}

// ---- Layouts --------------------------------------------------------------------------------------

/** A restrained library: no hero or colour wash, just compact app sections and a quiet header. */
@Composable
private fun EpureLayout(home: Home, ui: Ui, quick: List<QuickAction>, onQuick: (QuickAction) -> Unit) {
    val allApps = stringResource(R.string.cinema_apps)
    val headerFocus = remember { FocusRequester() }
    LaunchedEffect(home.first) { if (home.first == null) runCatching { headerFocus.requestFocus() } }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(start = SAFE_X, end = SAFE_X, top = SAFE_Y),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            if (ui.cfg.showBrand) Text(ui.cfg.names["brand"] ?: "CLEANMYTV", color = TEXT.copy(alpha = 0.45f), fontSize = 12.sp,
                letterSpacing = 2.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 180.dp))
            Spacer(Modifier.weight(1f))
            CinemaAction(allApps, ui, quiet = true,
                modifier = if (home.first == null) Modifier.focusRequester(headerFocus) else Modifier) { ui.onFolder(allApps to home.shown) }
            LazyRow(Modifier.widthIn(max = 180.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), contentPadding = PaddingValues(4.dp)) {
                items(quick, key = { it.id }) { QuickButton(it, ui, onQuick, compact = true) }
            }
            Clock(ui.cfg, 19.sp, 11.sp, Alignment.End)
        }
        Box(Modifier.padding(horizontal = SAFE_X, vertical = 14.dp).fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.06f)))
        Row(Modifier.fillMaxWidth().padding(start = SAFE_X, end = SAFE_X, top = 5.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.epure_title), color = TEXT, fontSize = 26.sp, fontWeight = FontWeight.Light, letterSpacing = (-0.5).sp)
                Text(stringResource(R.string.epure_subtitle), color = TEXT.copy(alpha = 0.4f), fontSize = 12.sp, modifier = Modifier.padding(top = 5.dp))
            }
            Column(horizontalAlignment = Alignment.End) {
                WeatherLine(ui, 12.sp)
                MemoryLine(ui, 11.sp, Modifier.padding(top = 4.dp))
            }
        }
        Sections(home, ui, Modifier.fillMaxSize(), PaddingValues(start = SAFE_X - BLEED, end = SAFE_X - BLEED, bottom = 96.dp), dockWidth = 88.dp)
    }
}

/** A quiet clock-led home with a centred dock. App drawers reuse the normal lock-aware tiles. */
@Composable
private fun HorizonLayout(home: Home, ui: Ui, quick: List<QuickAction>, onQuick: (QuickAction) -> Unit) {
    val cfg = ui.cfg
    val allApps = stringResource(R.string.cinema_apps)
    val dock = if ("dock" in cfg.sections) home.dock else emptyList()
    val headerFocus = remember { FocusRequester() }
    LaunchedEffect(home.first) { if (home.first == null) runCatching { headerFocus.requestFocus() } }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().drawBehind {
            drawRect(Brush.radialGradient(listOf(Color(0xFF73A5FF).copy(alpha = 0.13f), Color.Transparent),
                center = Offset(size.width * 0.5f, size.height * 0.35f), radius = size.width * 0.65f))
            drawLine(Brush.horizontalGradient(listOf(Color.Transparent, Color.White.copy(alpha = 0.08f), Color.Transparent)),
                Offset(0f, size.height * 0.62f), Offset(size.width, size.height * 0.62f), strokeWidth = 1.dp.toPx())
        })
        Row(Modifier.align(Alignment.TopCenter).fillMaxWidth().padding(horizontal = SAFE_X, vertical = SAFE_Y),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            if (cfg.showBrand) Text(cfg.brandLabel, color = TEXT.copy(alpha = 0.7f), fontSize = 15.sp, fontWeight = FontWeight.Medium,
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 140.dp))
            LazyRow(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp), contentPadding = PaddingValues(5.dp)) {
                item("all") { CinemaAction(allApps, ui, quiet = true,
                    modifier = if (home.first == null) Modifier.focusRequester(headerFocus) else Modifier) { ui.onFolder(allApps to home.shown) } }
                if ("rows" in cfg.sections) itemsIndexed(home.rows, key = { index, _ -> "row:$index" }) { _, group ->
                    CinemaAction(group.first, ui, quiet = true) { ui.onFolder(group) }
                }
                if ("folders" in cfg.sections) itemsIndexed(home.folders, key = { index, _ -> "folder:$index" }) { _, group ->
                    CinemaAction(group.first, ui, quiet = true) { ui.onFolder(group) }
                }
                if ("recent" in cfg.sections && home.recent.isNotEmpty()) item("recent") {
                    val title = stringResource(R.string.horizon_recent)
                    CinemaAction(title, ui, quiet = true) { ui.onFolder(title to home.recent) }
                }
            }
            LazyRow(Modifier.widthIn(max = 200.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), contentPadding = PaddingValues(5.dp)) {
                items(quick, key = { it.id }) { QuickButton(it, ui, onQuick, compact = true) }
            }
        }
        Row(Modifier.align(Alignment.CenterStart).fillMaxWidth().padding(start = SAFE_X + 12.dp, end = SAFE_X, bottom = 135.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                if (cfg.clock != "off") Clock(cfg, 72.sp, 14.sp, Alignment.Start)
                else Text(stringResource(R.string.horizon_welcome), color = TEXT, fontSize = 44.sp, fontWeight = FontWeight.Light)
                Spacer(Modifier.height(12.dp))
                Greeting(cfg, 17.sp)
                WeatherLine(ui, 13.sp, Modifier.padding(top = 5.dp))
                MemoryLine(ui, 12.sp, Modifier.padding(top = 5.dp))
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                if ("nowplaying" in cfg.sections) ui.track?.let { NowPlayingCard(it, ui) }
            }
        }
        if (dock.isNotEmpty()) {
            val iconWidth = 64.dp * (cfg.dockScale / 100f)
            val panelWidth = minOf(maxWidth - SAFE_X * 2, iconWidth * dock.size + 20.dp * (dock.size - 1) + 40.dp)
            val list = rememberLazyListState()
            LaunchedEffect(ui.movingKey) {
                val pkg = ui.movingKey?.removePrefix("dock:") ?: return@LaunchedEffect
                val index = dock.indexOfFirst { it.pkg == pkg }
                if (index >= 0 && list.layoutInfo.visibleItemsInfo.none { it.index == index }) list.scrollToItem(index)
            }
            LazyRow(Modifier.align(Alignment.BottomCenter).padding(bottom = SAFE_Y + if (cfg.visualiser) 64.dp else 20.dp)
                .width(panelWidth).background(Color(0xD51A1E27), RoundedCornerShape(28.dp))
                .border(1.dp, Color.White.copy(alpha = 0.09f), RoundedCornerShape(28.dp)),
                state = list, horizontalArrangement = Arrangement.spacedBy(20.dp),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp)) {
                items(dock, key = { it.pkg }) { app -> Tile("dock", app, ui, Modifier.width(iconWidth), big = true) }
            }
        }
        if (dock.isNotEmpty()) Text(stringResource(R.string.horizon_hint), color = TEXT.copy(alpha = 0.4f), fontSize = 11.sp,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = SAFE_Y))
    }
}

/** Floating navigation and a contextual spotlight above the round apps, using actual media metadata. */
@Composable
private fun CinemaLayout(home: Home, ui: Ui, quick: List<QuickAction>, onQuick: (QuickAction) -> Unit) {
    val ctx = LocalContext.current
    val cfg = ui.cfg
    val allApps = stringResource(R.string.cinema_apps)
    val favourites = stringResource(R.string.cinema_favourites)
    val selected = ui.cinemaApp
    val playing = ui.track?.takeIf { "nowplaying" in cfg.sections && it.app == selected?.label }
    CinemaAmbient(ui)
    Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(0f to Color(0x45080B11), 1f to Color.Transparent)))
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.35f to Color.Transparent, 1f to Color(0x66080B11))))
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(start = SAFE_X, end = SAFE_X, top = SAFE_Y, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            if (cfg.showBrand) Text(cfg.brandLabel, color = TEXT.copy(alpha = 0.8f), fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 160.dp))
            Row(Modifier.background(Color.White.copy(alpha = 0.07f), RoundedCornerShape(30.dp))
                .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(30.dp)).padding(5.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.cinema_home), color = TEXT, fontSize = 13.sp, fontWeight = FontWeight.Medium,
                    modifier = Modifier.background(Color.White.copy(alpha = 0.13f), RoundedCornerShape(24.dp)).padding(horizontal = 15.dp, vertical = 9.dp))
                CinemaAction(allApps, ui, quiet = true) { ui.onFolder(allApps to home.shown) }
                if (home.dock.isNotEmpty()) CinemaAction(favourites, ui, quiet = true) { ui.onFolder(favourites to home.dock) }
            }
            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                if (quick.isNotEmpty()) LazyRow(
                    Modifier.width(minOf(240.dp, 34.dp * quick.size + 12.dp))
                        .background(Color.White.copy(alpha = 0.09f), CircleShape)
                        .border(1.dp, Color.White.copy(alpha = 0.07f), CircleShape),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    contentPadding = PaddingValues(horizontal = 7.dp, vertical = 5.dp)
                ) {
                    items(quick, key = { it.id }) { QuickButton(it, ui, onQuick, compact = true) }
                }
            }
            Clock(cfg.copy(clock = if (cfg.clock == "off") "off" else "time"), 19.sp, 12.sp, Alignment.End)
        }
        Sections(home, ui, Modifier.weight(1f).fillMaxWidth(),
            PaddingValues(start = SAFE_X - BLEED, end = SAFE_X - BLEED, bottom = 96.dp), dockWidth = 104.dp,
            hero = {
                Column(Modifier.padding(horizontal = BLEED).fillMaxWidth().height(208.dp), verticalArrangement = Arrangement.Center) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        selected?.glyph?.let { Image(it.art, null, Modifier.size(18.dp), contentScale = ContentScale.Fit) }
                        Text(if (playing != null) playing.app else stringResource(R.string.cinema_selected),
                            color = TEXT.copy(alpha = 0.65f), fontSize = 11.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.Medium)
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(playing?.title ?: selected?.label ?: stringResource(R.string.cinema_empty_title), color = TEXT,
                        fontSize = if ((playing?.title ?: selected?.label.orEmpty()).length > 22) 36.sp else 48.sp,
                        lineHeight = 51.sp, fontWeight = FontWeight.Medium, letterSpacing = (-1).sp, maxLines = 2,
                        overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth(0.7f))
                    Spacer(Modifier.height(10.dp))
                    Text(playing?.artist?.takeIf { it.isNotBlank() } ?: stringResource(if (selected == null) R.string.cinema_empty_body else R.string.cinema_focus_hint),
                        color = TEXT.copy(alpha = 0.65f), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth(0.7f))
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (selected != null) CinemaAction(stringResource(R.string.cinema_open), ui, filled = true, icon = Icons.Outlined.PlayArrow) {
                            launch(ctx, selected)
                        }
                        val action = quickActions().getValue(if (cfg.editOnTv) "customize" else "panel")
                        CinemaAction(stringResource(if (cfg.editOnTv) R.string.cinema_customize else R.string.cinema_settings), ui, icon = Icons.Outlined.Dehaze) { onQuick(action) }
                    }
                }
            })
    }
}

/** A low-contrast glow instead of an automatic photo; animation only invalidates this draw layer. */
@Composable
private fun CinemaAmbient(ui: Ui) {
    val app = ui.cinemaApp
    val pkg = app?.pkg.orEmpty()
    val colour = when {
        "youtube" in pkg -> Color(0xFFFF0033)
        "netflix" in pkg -> Color(0xFFE50914)
        "amazon" in pkg || "amzn" in pkg -> Color(0xFF159EFF)
        "disney" in pkg -> Color(0xFF2563EB)
        "spotify" in pkg -> Color(0xFF1ED760)
        "twitch" in pkg -> Color(0xFF9146FF)
        else -> app?.glyph?.plate?.let { Color(it) } ?: app?.colour?.let { Color(it) } ?: Color(0xFF6F7785)
    }
    val ambient = animateColorAsState(colour, tween(if (ui.motion) 900 else 0), label = "cinema ambient")
    Box(Modifier.fillMaxSize().drawBehind {
        drawRect(Brush.radialGradient(listOf(ambient.value.copy(alpha = 0.14f), Color.Transparent),
            center = Offset(size.width * 0.18f, size.height * 0.42f), radius = size.width * 0.7f))
    })
}

@Composable
private fun CinemaAction(label: String, ui: Ui, filled: Boolean = false, quiet: Boolean = false, icon: ImageVector? = null, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    val (scale, glow, border) = focusStyle(ui, shape, zoom = 1.04f)
    var focused by remember { mutableStateOf(false) }
    Surface(onClick = onClick, modifier = modifier.pointer(onClick = onClick).onFocusChanged { focused = it.hasFocus },
        shape = ClickableSurfaceDefaults.shape(shape), scale = scale, glow = glow, border = border,
        colors = ClickableSurfaceDefaults.colors(containerColor = if (filled) TEXT else Color.White.copy(alpha = if (quiet) 0f else 0.08f), focusedContainerColor = ui.accent)) {
        val ink = if (focused) onAccent(ui.accent) else if (filled) GROUND else TEXT
        Row(Modifier.padding(horizontal = 16.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            if (icon != null) Image(icon, null, Modifier.size(17.dp), colorFilter = ColorFilter.tint(ink))
            Text(label, color = ink, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
    }
}

/** Shelf: a slim top bar, open wallpaper above the apps, the sections below. */
@Composable
private fun ShelfLayout(home: Home, ui: Ui, quick: List<QuickAction>, onQuick: (QuickAction) -> Unit) {
    val cfg = ui.cfg
    if (cfg.shadeTop > 0 && cfg.shadeTopReach > 0) {
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to scrim(cfg, cfg.shadeTop / 100f), cfg.shadeTopReach / 100f to Color.Transparent)))
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val open = maxHeight * (cfg.topSpace / 100f)
        Column(Modifier.fillMaxSize()) {
            // Greeting on the left; on the right the clock, the date, and the quick-settings icons under them.
            Row(Modifier.fillMaxWidth().padding(start = SAFE_X, end = SAFE_X, top = SAFE_Y), verticalAlignment = Alignment.Top) {
                Box(Modifier.padding(top = 4.dp)) { Greeting(cfg, 20.sp) }
                Spacer(Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.End) {
                    Clock(cfg, 26.sp, 12.sp, Alignment.End)
                    WeatherLine(ui, 13.sp, Modifier.padding(top = 4.dp))
                    MemoryLine(ui, 13.sp, Modifier.padding(top = 4.dp))
                    if (quick.isNotEmpty()) {
                        if (cfg.clock != "off") Spacer(Modifier.height(10.dp))
                        Row(Modifier.focusGroup(), horizontalArrangement = Arrangement.spacedBy(10.dp)) { quick.forEach { QuickButton(it, ui, onQuick) } }
                    }
                }
            }
            Sections(home, ui, Modifier.fillMaxSize(), PaddingValues(start = SAFE_X - BLEED, end = SAFE_X - BLEED, top = open, bottom = 96.dp), dockWidth = 200.dp)
        }
    }
}

/** Split: the time, greeting and quick settings hold the left third; the apps scroll on the right. */
@Composable
private fun SplitLayout(home: Home, ui: Ui, quick: List<QuickAction>, onQuick: (QuickAction) -> Unit) {
    val cfg = ui.cfg
    Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(scrim(cfg, 0.7f), Color.Transparent), endX = 900f)))
    Row(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxWidth(0.32f).fillMaxHeight().padding(start = SAFE_X, top = SAFE_Y + 24.dp, bottom = SAFE_Y + 48.dp)) {
            // 46 sp keeps "12:51 PM" on one line in the left third; 12-hour time is the widest.
            Clock(cfg, 46.sp, 15.sp, Alignment.Start)
            WeatherLine(ui, 15.sp, Modifier.padding(top = 6.dp))
            MemoryLine(ui, 15.sp, Modifier.padding(top = 6.dp))
            Spacer(Modifier.height(14.dp))
            Greeting(cfg, 18.sp)
            Spacer(Modifier.weight(1f))
            Column(Modifier.focusGroup(), verticalArrangement = Arrangement.spacedBy(6.dp)) { quick.forEach { QuickRow(it, ui, onQuick, expanded = true) } }
        }
        Sections(home, ui, Modifier.fillMaxSize(), PaddingValues(start = 0.dp, end = SAFE_X - BLEED, top = SAFE_Y + 12.dp, bottom = 96.dp), dockWidth = 168.dp)
    }
}

/** Rail: icons down the left edge that widen with labels when focus moves onto them. */
@Composable
private fun RailLayout(home: Home, ui: Ui, quick: List<QuickAction>, onQuick: (QuickAction) -> Unit) {
    val cfg = ui.cfg
    var railFocused by remember { mutableStateOf(false) }
    // Icons start at the 48 dp safe edge; only the rail's shade runs to the screen edge.
    val railW by animateDpAsState(if (railFocused) 260.dp else SAFE_X + 56.dp, tween(220), label = "rail")
    Row(Modifier.fillMaxSize()) {
        Column(
            Modifier.width(railW).fillMaxHeight()
                .background(Brush.horizontalGradient(listOf(scrim(cfg, if (railFocused) 0.92f else 0.6f), scrim(cfg, if (railFocused) 0.75f else 0.1f))))
                .padding(start = SAFE_X - 4.dp, top = SAFE_Y + 8.dp, end = 8.dp)
                .onFocusChanged { railFocused = it.hasFocus }
                .focusGroup(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            quick.forEach { QuickRow(it, ui, onQuick, expanded = railFocused) }
            if (home.folders.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                home.folders.forEach { f ->
                    RailItem(Icons.Outlined.Folder, f.first, ui, railFocused) { ui.onFolder(f) }
                }
            }
        }
        Column(Modifier.fillMaxSize()) {
            // As Shelf: the greeting level with the time, and the date, weather and memory stacked under the time.
            // Side by side they were centred on the clock's two lines and sat between them.
            Row(Modifier.fillMaxWidth().padding(start = 24.dp, end = SAFE_X, top = SAFE_Y), verticalAlignment = Alignment.Top) {
                Box(Modifier.padding(top = 4.dp)) { Greeting(cfg, 20.sp) }
                Spacer(Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.End) {
                    Clock(cfg, 26.sp, 12.sp, Alignment.End)
                    WeatherLine(ui, 13.sp, Modifier.padding(top = 4.dp))
                    MemoryLine(ui, 13.sp, Modifier.padding(top = 4.dp))
                }
            }
            Sections(home, ui, Modifier.fillMaxSize(), PaddingValues(start = 24.dp - BLEED, end = SAFE_X - BLEED, top = 20.dp, bottom = 96.dp), dockWidth = 196.dp, showFolders = false)
        }
    }
}

// ---- Sections -------------------------------------------------------------------------------------

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun Sections(home: Home, ui: Ui, modifier: Modifier, padding: PaddingValues, dockWidth: Dp, showFolders: Boolean = true, hero: (@Composable () -> Unit)? = null) {
    val cfg = ui.cfg
    // Rows keep Android TV's own sideways scrolling; only the column up and down follows STAY.
    val rowSpec = LocalBringIntoViewSpec.current
    BoxWithConstraints(modifier) {
        val inner = maxWidth - padding.calcHorizontal() - BLEED * 2
        val gridW: Dp = (inner - GAP * (cfg.columns - 1)) / cfg.columns
        // At 100% the dock fits the screen; the size slider scales that, and a bigger dock scrolls sideways (#89).
        val n = home.dock.size.coerceIn(1, 5)
        val dockW: Dp = minOf(dockWidth, (inner - GAP * (n - 1)) / n) * (cfg.dockScale / 100f)
        // An app moved with the arrows can land in a row that isn't drawn yet: scroll there so it can keep focus.
        val list = rememberLazyListState()
        LaunchedEffect(ui.movingKey, home.grid.size) {
            val key = ui.movingKey ?: return@LaunchedEffect
            val (section, pkg) = key.split(':', limit = 2).let { it[0] to it.getOrElse(1) { "" } }
            val target = if (section == "grid") "grid${home.grid.indexOfFirst { it.pkg == pkg } / cfg.columns}" else section
            if (list.layoutInfo.visibleItemsInfo.none { it.key == target }) {
                val at = sectionKeys(home, ui, showFolders).indexOf(target)
                if (at >= 0) list.scrollToItem(at)
            }
        }
        CompositionLocalProvider(LocalBringIntoViewSpec provides STAY) { LazyColumn(Modifier.fillMaxSize().fadeTop(), state = list, contentPadding = padding) {
            if (hero != null) item("cinema-hero") { hero() }
            for (s in cfg.sections) when (s) {
                "nowplaying" -> ui.track?.let { t -> item("nowplaying") { NowPlayingCard(t, ui) } }
                "recent" -> if (home.recent.isNotEmpty()) {
                    item("recent-title") { SectionTitle(cfg.names["recent"] ?: if (cfg.layout == "epure") stringResource(R.string.horizon_recent) else "Recently used", cfg.layout == "epure") }
                    item("recent") { TileRow("recent", home.recent, ui, (if (cfg.layout == "epure") 88.dp else if (cfg.layout == "cinema") 104.dp else 148.dp) * (cfg.tileScale / 100f), big = false, rowSpec) }
                }
                "dock" -> if (home.dock.isNotEmpty()) {
                    if (cfg.layout in setOf("cinema", "epure")) item("dock-title") { SectionTitle(stringResource(R.string.cinema_my_apps), cfg.layout == "epure") }
                    item("dock") { TileRow("dock", home.dock, ui, dockW, big = true, rowSpec) }
                }
                "rows" -> home.rows.forEachIndexed { i, (name, list) ->
                    item("row$i-title") { SectionTitle(name, cfg.layout == "epure") }
                    item("row$i") { TileRow("row$i", list, ui, (if (cfg.layout == "epure") 88.dp else if (cfg.layout == "cinema") 104.dp else 148.dp) * (cfg.tileScale / 100f), big = false, rowSpec) }
                }
                "folders" -> if (showFolders && home.folders.isNotEmpty()) {
                    item("folders-title") { SectionTitle(if (cfg.layout == "epure") stringResource(R.string.epure_folders) else "Folders", cfg.layout == "epure") }
                    item("folders") {
                        CompositionLocalProvider(LocalBringIntoViewSpec provides rowSpec) {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(GAP), contentPadding = PaddingValues(horizontal = BLEED, vertical = 16.dp)) {
                                items(home.folders, key = { it.first }) { f -> FolderTile(f, ui, Modifier.width(148.dp * (cfg.tileScale / 100f))) }
                            }
                        }
                    }
                }
                "grid" -> if (home.grid.isNotEmpty()) grid(home, ui, gridW)
            }
        } }
    }
}

/** The keys of Sections' items, in the order it lays them out. */
private fun sectionKeys(home: Home, ui: Ui, showFolders: Boolean): List<String> = buildList {
    if (ui.cfg.layout == "cinema") add("cinema-hero")
    for (s in ui.cfg.sections) when (s) {
        "nowplaying" -> if (ui.track != null) add("nowplaying")
        "recent" -> if (home.recent.isNotEmpty()) { add("recent-title"); add("recent") }
        "dock" -> if (home.dock.isNotEmpty()) { if (ui.cfg.layout in setOf("cinema", "epure")) add("dock-title"); add("dock") }
        "rows" -> home.rows.indices.forEach { add("row$it-title"); add("row$it") }
        "folders" -> if (showFolders && home.folders.isNotEmpty()) { add("folders-title"); add("folders") }
        "grid" -> if (home.grid.isNotEmpty()) { add("grid-title"); home.grid.chunked(ui.cfg.columns).indices.forEach { add("grid$it") } }
    }
}

/**
 * Up and down, the sections stay where the layout puts them while the focused tile is on screen below the
 * top 30%, so Top space holds (#90). Android TV's own rule moved every focused row to 30% from the top,
 * which pulled the dock up. Off screen, or above that line, the row still goes to 30%; the list stops at its top.
 */
@OptIn(ExperimentalFoundationApi::class)
private val STAY = object : BringIntoViewSpec {
    override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float {
        val pivot = containerSize * 0.3f
        return if (offset + size > containerSize || offset < pivot) offset - pivot else 0f
    }
}

/** Rows scrolling up fade out over the top 28 dp instead of being sliced by a hard edge. */
private fun Modifier.fadeTop(): Modifier = graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen).drawWithContent {
    drawContent()
    drawRect(Brush.verticalGradient(0f to Color.Transparent, 1f to Color.Black, endY = 28.dp.toPx()), blendMode = BlendMode.DstIn)
}

private fun PaddingValues.calcHorizontal(): Dp =
    calculateLeftPadding(androidx.compose.ui.unit.LayoutDirection.Ltr) + calculateRightPadding(androidx.compose.ui.unit.LayoutDirection.Ltr)

private fun LazyListScope.grid(home: Home, ui: Ui, w: Dp) {
    val label = ui.cfg.names["all_apps"] ?: if (home.dock.isEmpty() && home.rows.isEmpty()) "Apps" else "All apps"
    item("grid-title") { SectionTitle(if (ui.cfg.layout == "epure" && ui.cfg.names["all_apps"] == null) stringResource(R.string.epure_all_apps) else label, ui.cfg.layout == "epure") }
    // Shift the row to align the visible icons with the heading. Use an offset, not negative padding:
    // on wide slots (especially Cinema / Épure), the inset can exceed BLEED and padding would crash Home.
    val inset = if (ui.shape == "wide") 0.dp else w * (if (ui.cfg.layout == "epure") 0.25f else if (ui.cfg.layout == "cinema") 0.21f else 0.14f)
    home.grid.chunked(ui.cfg.columns).forEachIndexed { i, row ->
        item("grid$i") {
            Row(Modifier.offset(x = -inset).padding(horizontal = BLEED, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(GAP)) {
                for (app in row) Tile("grid", app, ui, Modifier.width(w), big = false)
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String, quiet: Boolean = false) {
    Text(text, color = TEXT.copy(alpha = if (quiet) 0.65f else 1f), fontSize = if (quiet) 16.sp else 18.sp,
        fontWeight = if (quiet) FontWeight.Normal else FontWeight.Medium, modifier = Modifier.padding(start = BLEED, top = 12.dp))
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun TileRow(section: String, list: List<TvApp>, ui: Ui, w: Dp, big: Boolean, rowSpec: BringIntoViewSpec) {
    val row = @Composable {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(if (ui.cfg.layout == "epure") 26.dp else if (ui.cfg.layout == "cinema") 22.dp else GAP), contentPadding = PaddingValues(horizontal = BLEED, vertical = if (ui.cfg.layout in setOf("cinema", "epure")) 12.dp else 16.dp)) {
            val tw = if (ui.shape == "wide") w else w * 0.72f
            items(list, key = { it.pkg }) { app -> Tile(section, app, ui, Modifier.width(tw), big) }
        }
    }
    CompositionLocalProvider(LocalBringIntoViewSpec provides if (ui.cfg.pinFocus) PINNED else rowSpec) { row() }
}

// ---- Tiles ----------------------------------------------------------------------------------------

/** [lifted]: the caller zooms the tile itself with [lift], so the surface doesn't. */
@Composable
private fun focusStyle(ui: Ui, shape: Shape, zoom: Float = 1.08f, lifted: Boolean = false) = Triple(
    ClickableSurfaceDefaults.scale(focusedScale = if (ui.cfg.focus == "none" || (lifted && ui.cfg.focus == "lift")) 1f else if (ui.cfg.layout == "epure") 1.04f else zoom),
    ClickableSurfaceDefaults.glow(
        focusedGlow = when (ui.cfg.focus) {
            "glow" -> Glow(ui.accent.copy(alpha = if (ui.cfg.layout == "epure") 0.08f else if (ui.cfg.layout == "cinema") 0.3f else 1f), if (ui.cfg.layout == "epure") 6.dp else if (ui.cfg.layout == "cinema") 10.dp else 16.dp)
            "lift" -> Glow(ui.accent.copy(alpha = if (ui.cfg.layout == "epure") 0.08f else 0.35f), if (ui.cfg.layout == "epure") 6.dp else 16.dp)
            else -> Glow.None
        }
    ),
    ClickableSurfaceDefaults.border(
        focusedBorder = when (ui.cfg.focus) {
            "outline" -> Border(BorderStroke(if (ui.cfg.layout == "epure") 1.5.dp else 3.dp, ui.accent), shape = shape)
            "glow" -> Border(BorderStroke(if (ui.cfg.layout == "epure") 1.5.dp else 2.dp, TEXT), shape = shape)
            "lift" -> Border(BorderStroke(if (ui.cfg.layout == "cinema") 2.dp else 1.dp, Color.White.copy(alpha = if (ui.cfg.layout == "cinema") 0.95f else 0.28f)), shape = shape)
            else -> Border.None
        }
    ),
)

/** The last D-pad move, for the tilt: -1 or 1 on one axis. */
private object Motion {
    var dx = 0
    var dy = 0
    fun seen(k: Key) {
        when (k) {
            Key.DirectionLeft -> { dx = -1; dy = 0 }
            Key.DirectionRight -> { dx = 1; dy = 0 }
            Key.DirectionUp -> { dx = 0; dy = -1 }
            Key.DirectionDown -> { dx = 0; dy = 1 }
            else -> {}
        }
    }
}

/** Lift: the tile springs up on a soft shadow and, with tilt on, leans toward the move and settles. */
@Composable
private fun Modifier.lift(ui: Ui, shape: Shape, focused: Boolean): Modifier {
    if (ui.cfg.focus != "lift") return this
    val scale by animateFloatAsState(if (focused) 1.09f else 1f, if (ui.motion) spring(dampingRatio = 0.62f, stiffness = 420f) else androidx.compose.animation.core.snap(), label = "lift")
    val lean = remember { Animatable(0f) }
    val leanX = remember { mutableStateOf(true) }
    LaunchedEffect(focused) {
        if (focused && ui.cfg.tilt && ui.motion && (Motion.dx != 0 || Motion.dy != 0)) {
            leanX.value = Motion.dx != 0
            lean.snapTo(if (Motion.dx != 0) Motion.dx * 4f else -Motion.dy * 4f)
            lean.animateTo(0f, tween(220))
        } else lean.snapTo(0f)
    }
    return zIndex(if (focused) 1f else 0f)
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
            translationY = -(scale - 1f) * 40f
            if (leanX.value) rotationY = lean.value else rotationX = lean.value
            cameraDistance = 12f * density
        }
        .shadow(if (focused) 12.dp else 0.dp, shape, clip = false, ambientColor = Color.Black, spotColor = Color.Black)
}

/** Over a lifted tile: one sweep of light when focus lands, and a slight dim on the tiles around it. */
@Composable
private fun LiftLight(ui: Ui, focused: Boolean) {
    if (ui.cfg.focus != "lift") return
    val p = remember { Animatable(0f) }
    LaunchedEffect(focused) {
        p.snapTo(0f)
        if (focused && ui.motion) { delay(80); p.animateTo(1f, tween(650, easing = FastOutSlowInEasing)) }
    }
    if (!focused) Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.12f)))
    else if (p.value in 0.001f..0.999f) Box(
        Modifier.fillMaxSize().drawWithContent {
            val w = size.width
            val x = -w * 0.8f + p.value * w * 1.6f
            drawRect(
                Brush.linearGradient(
                    0f to Color.Transparent, 0.45f to Color.White.copy(alpha = 0.22f), 0.55f to Color.White.copy(alpha = 0.05f), 1f to Color.Transparent,
                    start = Offset(x, 0f), end = Offset(x + w * 0.6f, size.height * 0.4f)
                )
            )
        }
    )
}

/** Keeps the focused tile one step in from the left; the row slides under it. */
@OptIn(ExperimentalFoundationApi::class)
private val PINNED = object : BringIntoViewSpec {
    override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float = offset - containerSize * 0.12f
}

@Composable
private fun Tile(section: String, app: TvApp, ui: Ui, size: Modifier, big: Boolean) {
    val ctx = LocalContext.current
    val shape = tileShape(ui.shape, big)
    val (scale, glow, border) = focusStyle(ui, shape, lifted = true)
    var focused by remember { mutableStateOf(false) }
    // With names on focus, the name keeps its room so the rows don't jump.
    val nameAlpha by animateFloatAsState(if (!ui.cfg.nameFocus || focused) 1f else 0f, tween(if (ui.motion) 200 else 0), label = "name")
    val wide = ui.shape == "wide"
    // In the grid and folders each slot keeps its width; a square tile sits in the middle of it.
    val inSlot = section == "grid" || section == "folder"
    val banner = ui.cfg.iconStyle == "banner" && ui.cfg.layout !in setOf("cinema", "horizon", "epure")
    // Mono glass is see-through, so the Glow focus showed inside the tile too: a focused one gets a solid back.
    val solid = ui.cfg.iconStyle == "mono" && ui.cfg.focus == "glow"
    Column(size, horizontalAlignment = if (wide) Alignment.Start else Alignment.CenterHorizontally) {
        Surface(
            onClick = { launch(ctx, app) },
            onLongClick = { ui.onAppMenu(section, app) },
            modifier = Modifier.pointer(onLongClick = { ui.onAppMenu(section, app) }) { launch(ctx, app) }.then(ui.focusFor(section, app))
                .fillMaxWidth(if (!wide && inSlot) { if (ui.cfg.layout == "epure") 0.5f else if (ui.cfg.layout == "cinema") 0.58f else 0.72f } else 1f).aspectRatio(if (wide) 16f / 9f else 1f)
                .onFocusChanged { focused = it.isFocused; if (it.isFocused) ui.onFocusApp(app) }
                .lift(ui, shape, focused),
            shape = ClickableSurfaceDefaults.shape(shape), scale = scale, glow = glow, border = border,
            colors = ClickableSurfaceDefaults.colors(containerColor = if (banner) TILE else Color.Transparent, focusedContainerColor = if (banner) TILE_FOCUSED else if (solid) Color(ui.cfg.background) else Color.Transparent)
        ) {
            when {
                !banner -> IconTile(app, ui)
                app.banner -> Image(app.art, app.label, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                else -> GeneratedTile(app, big, named = !ui.cfg.showNames)
            }
            LiftLight(ui, focused)
            if (ui.movingKey == "$section:${app.pkg}") Box(Modifier.fillMaxSize().border(3.dp, ui.accent, shape))
            if (Lock.isLocked(ui.cfg, app.pkg)) LockBadge(Modifier.align(if (ui.shape == "round") Alignment.BottomCenter else Alignment.TopEnd))
        }
        if (ui.cfg.showNames) {
            Text(app.label, color = TEXT.copy(alpha = if (ui.cfg.layout !in setOf("cinema", "horizon", "epure") || focused) 1f else 0.65f), fontSize = if (ui.cfg.layout in setOf("cinema", "horizon", "epure")) 12.sp else 14.sp,
                fontWeight = if (ui.cfg.layout in setOf("cinema", "horizon", "epure") && focused) FontWeight.Medium else FontWeight.Normal,
                maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = if (wide) TextAlign.Start else TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 7.dp, start = 2.dp).graphicsLayer { alpha = nameAlpha })
        }
    }
}

/** Opens an app, through the PIN pad when it's locked. */
private fun launch(ctx: android.content.Context, app: TvApp) {
    val i = when {
        Lock.needsPin(ctx, app.pkg) -> LockActivity.intent(ctx, app.pkg, app.launch)
        Limits.over(ctx, app.pkg) -> LockActivity.intent(ctx, app.pkg, app.launch, limit = true)
        else -> app.launch
    }
    runCatching { ctx.startActivity(i); Recent.note(ctx, app.pkg) }.onFailure { Log.w(TAG, "launch ${app.pkg}: $it") }
}

/** Rounded rectangle, rounded square, squircle (a superellipse, like Samsung's and Belle UI's) or circle. */
private fun tileShape(kind: String, big: Boolean): Shape = when (kind) {
    "square" -> RoundedCornerShape(22)
    "squircle" -> SQUIRCLE
    "round" -> CircleShape
    else -> RoundedCornerShape(if (big) 12.dp else 10.dp)
}

private val SQUIRCLE = GenericShape { size, _ ->
    val cx = size.width / 2; val cy = size.height / 2
    for (i in 0..96) {
        val t = i / 96.0 * Math.PI * 2
        val c = kotlin.math.cos(t); val s = kotlin.math.sin(t)
        val x = cx + cx * Math.signum(c) * kotlin.math.sqrt(kotlin.math.abs(c))
        val y = cy + cy * Math.signum(s) * kotlin.math.sqrt(kotlin.math.abs(s))
        if (i == 0) moveTo(x.toFloat(), y.toFloat()) else lineTo(x.toFloat(), y.toFloat())
    }
    close()
}

/** The small lock in a locked app's corner. */
@Composable
private fun LockBadge(modifier: Modifier) {
    Box(modifier.padding(6.dp).size(20.dp).background(Color.Black.copy(alpha = 0.6f), CircleShape), contentAlignment = Alignment.Center) {
        Image(Icons.Outlined.Lock, "Locked", Modifier.size(12.dp), colorFilter = ColorFilter.tint(TEXT))
    }
}

/**
 * An app in one of the icon styles: its logo on a tile, every app drawn the same way.
 * Mono: the logo in one colour from the accent, on glass. Colour: white on the brand's own colour (or
 * the logo's own colours on a dark tile). Duotone: two shades of the accent on the theme's colours.
 * An app with no usable logo gets its first letter, in the same style.
 */
@Composable
private fun IconTile(app: TvApp, ui: Ui) {
    if (ui.cfg.layout in setOf("cinema", "horizon", "epure")) { CinemaIcon(app); return }
    val accent = ui.accent
    val ground = Color(ui.cfg.background)
    // A picture icon has no logo to recolour: Mono and Duotone use the letter instead.
    val g = app.glyph?.takeIf { !it.picture || ui.cfg.iconStyle == "colour" }
    val ink = lerp(accent, Color.White, 0.35f)
    val back: Brush; val filter: ColorFilter?; val letterInk: Color
    when (ui.cfg.iconStyle) {
        "mono" -> {
            val glass = ui.cfg.tileGlass / 100f
            back = Brush.linearGradient(listOf(Color.White.copy(alpha = glass * 1.3f), Color.White.copy(alpha = glass * 0.7f))); filter = ColorFilter.tint(ink); letterInk = ink
        }
        "duo" -> {
            back = Brush.linearGradient(listOf(lerp(ground, accent, 0.3f), lerp(ground, Color.Black, 0.2f)))
            filter = ColorFilter.colorMatrix(IconArt.duotone(lerp(accent, Color.Black, 0.25f), lerp(accent, Color.White, 0.55f))); letterInk = accent
        }
        else -> {
            val plate = g?.plate?.let { Color(it) } ?: if (g == null) lerp(accent, Color.Black, 0.35f) else null
            back = if (plate != null) Brush.linearGradient(listOf(lerp(plate, Color.White, 0.08f), lerp(plate, Color.Black, 0.25f)))
                   else Brush.linearGradient(listOf(lerp(ground, Color.White, 0.12f), lerp(ground, Color.White, 0.06f)))
            filter = if (plate != null) ColorFilter.tint(Color.White) else null; letterInk = Color.White
        }
    }
    BoxWithConstraints(Modifier.fillMaxSize().background(back), contentAlignment = Alignment.Center) {
        val wide = ui.shape == "wide"
        if (g != null && g.picture && ui.cfg.iconStyle == "colour") {
            Image(g.art, app.label, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else if (g != null && !g.picture) {
            val art = if (ui.cfg.iconStyle == "mono") g.mono ?: g.art else g.art
            Image(art, app.label, Modifier.size(width = maxWidth * 0.62f, height = maxHeight * (if (wide) 0.48f else 0.46f)), contentScale = ContentScale.Fit, colorFilter = filter)
        } else {
            Text(app.label.take(1).uppercase(), color = letterInk, fontSize = with(androidx.compose.ui.platform.LocalDensity.current) { (minOf(maxWidth, maxHeight) * 0.42f).toSp() }, fontWeight = FontWeight.SemiBold)
        }
    }
}

/** Keep brand colours in the round cinema icons, including Netflix's red N on black. */
@Composable
private fun CinemaIcon(app: TvApp) {
    val g = app.glyph
    val plate = when {
        app.pkg.contains("youtube") -> Color(0xFFFF0000)
        app.pkg.contains("spotify") -> Color(0xFF1ED760)
        app.pkg.contains("amazon") || app.pkg.contains("amzn") -> Color(0xFF0878D1)
        app.pkg.contains("disney") -> Color(0xFF071B4D)
        app.pkg.contains("arte") -> Color(0xFFF5EFE6)
        else -> Color(0xFF171D27)
    }
    val filter = when {
        app.pkg.contains("spotify") -> ColorFilter.tint(Color.Black)
        app.pkg.contains("youtube") || app.pkg.contains("amazon") || app.pkg.contains("amzn") || app.pkg.contains("disney") -> ColorFilter.tint(Color.White)
        else -> null
    }
    Box(Modifier.fillMaxSize().background(plate), contentAlignment = Alignment.Center) {
        if (g != null) Image(g.art, app.label, if (g.picture) Modifier.fillMaxSize() else Modifier.fillMaxSize(0.6f),
            contentScale = if (g.picture) ContentScale.Crop else ContentScale.Fit, colorFilter = if (g.picture) null else filter)
        else Text(app.label.take(1).uppercase(), color = TEXT, fontSize = 32.sp, fontWeight = FontWeight.Bold)
    }
}

/**
 * A tile for an app with no TV banner, like most sideloaded apps (#4): the icon on a gradient of its own
 * colour. With names off it reads like a banner, icon on the left and the name beside it.
 */
@Composable
private fun GeneratedTile(app: TvApp, big: Boolean, named: Boolean) {
    val base = app.tint?.let { Color(it) }
    val fill = if (base == null) Modifier else Modifier.background(Brush.linearGradient(listOf(lerp(base, Color.White, 0.12f), lerp(base, Color.Black, 0.35f))))
    BoxWithConstraints(Modifier.fillMaxSize().then(fill), contentAlignment = Alignment.Center) {
        // Grid tiles are ~120 dp wide: a name beside the icon would break mid-word, so it goes underneath.
        val wide = maxWidth >= 170.dp
        val icon = if (wide) (if (big) 52.dp else 44.dp) else 32.dp
        when {
            !named -> Image(app.art, null, Modifier.size(icon))
            wide -> Row(Modifier.fillMaxSize().padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Image(app.art, null, Modifier.size(icon))
                Spacer(Modifier.width(12.dp))
                Text(app.label, color = TEXT, fontSize = if (big) 17.sp else 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 19.sp)
            }
            else -> Column(Modifier.padding(horizontal = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Image(app.art, null, Modifier.size(icon))
                Spacer(Modifier.height(5.dp))
                Text(app.label, color = TEXT, fontSize = 11.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/** A folder: a 2x2 peek at its apps, the name underneath. OK opens it over the home screen. */
@Composable
private fun FolderTile(folder: Pair<String, List<TvApp>>, ui: Ui, size: Modifier) {
    val shape = RoundedCornerShape(10.dp)
    val (scale, glow, border) = focusStyle(ui, shape, lifted = true)
    var focused by remember { mutableStateOf(false) }
    // The accent mixed into the background, a quarter and then two fifths, so folders read as the theme's own.
    val ground = Color(ui.cfg.background)
    Column(size) {
        Surface(
            onClick = { ui.onFolder(folder) },
            modifier = Modifier.pointer { ui.onFolder(folder) }.fillMaxWidth().aspectRatio(16f / 9f).onFocusChanged { focused = it.isFocused }.lift(ui, shape, focused),
            shape = ClickableSurfaceDefaults.shape(shape), scale = scale, glow = glow, border = border,
            colors = ClickableSurfaceDefaults.colors(containerColor = lerp(ground, ui.accent, 0.25f).copy(alpha = 0.9f), focusedContainerColor = lerp(ground, ui.accent, 0.4f))
        ) {
            Column(Modifier.fillMaxSize().padding(8.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                folder.second.take(4).chunked(2).forEach { pair ->
                    Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        pair.forEach { app ->
                            Image(app.art, null, Modifier.weight(1f).fillMaxHeight().background(app.tint?.let { Color(it) } ?: Color.Black.copy(alpha = 0.25f), RoundedCornerShape(5.dp)), contentScale = if (app.banner) ContentScale.Crop else ContentScale.Fit)
                        }
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
            LiftLight(ui, focused)
        }
        Text("${folder.first} · ${folder.second.size}", color = TEXT, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 7.dp, start = 2.dp))
    }
}

/** What another app is playing, with play/pause and next. Only there while a session has something loaded. */
@Composable
private fun NowPlayingCard(t: Track, ui: Ui) {
    val cinema = ui.cfg.layout in setOf("cinema", "horizon", "epure")
    Row(
        Modifier.padding(horizontal = BLEED, vertical = if (cinema) 8.dp else 12.dp).width(if (cinema) 400.dp else 520.dp)
            .background(if (cinema) Color(0xB3181D26) else PANEL.copy(alpha = 0.88f), RoundedCornerShape(if (cinema) 16.dp else 14.dp))
            .then(if (cinema) Modifier.border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp)) else Modifier)
            .padding(if (cinema) 10.dp else 12.dp).focusGroup(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(if (cinema) 36.dp else 64.dp).clip(RoundedCornerShape(8.dp)).background(if (cinema) Color(0xFF262C37) else TILE), contentAlignment = Alignment.Center) {
            if (t.art != null) Image(t.art, null, Modifier.fillMaxSize().background(Color.Black, RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
            else Image(Icons.Outlined.MusicNote, null, Modifier.size(if (cinema) 20.dp else 28.dp), colorFilter = ColorFilter.tint(MUTED))
        }
        Column(Modifier.weight(1f).padding(horizontal = if (cinema) 10.dp else 14.dp)) {
            Text(t.title, color = TEXT, fontSize = if (cinema) 14.sp else 17.sp,
                lineHeight = if (cinema) 16.sp else androidx.compose.ui.unit.TextUnit.Unspecified,
                fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(listOf(t.artist, t.app).filter { it.isNotBlank() }.joinToString(" · "), color = MUTED, fontSize = if (cinema) 11.sp else 13.sp,
                lineHeight = if (cinema) 14.sp else androidx.compose.ui.unit.TextUnit.Unspecified,
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 3.dp))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(if (cinema) 6.dp else 10.dp)) {
            RoundButton(if (t.playing) Icons.Outlined.Pause else Icons.Outlined.PlayArrow, if (t.playing) "Pause" else "Play", ui) { NowPlaying.playPause() }
            RoundButton(Icons.Outlined.SkipNext, "Next", ui) { NowPlaying.next() }
        }
    }
}

// ---- Top bar pieces -------------------------------------------------------------------------------

/** Icon, temperature, today's high and low, and the place, in one muted line. MET Norway's data asks for its name. */
@Composable
private fun WeatherLine(ui: Ui, size: androidx.compose.ui.unit.TextUnit, modifier: Modifier = Modifier) {
    val w = ui.weather ?: return
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Image(weatherIcon(w), null, Modifier.size(with(androidx.compose.ui.platform.LocalDensity.current) { size.toDp() * 1.3f }), colorFilter = ColorFilter.tint(MUTED))
        Spacer(Modifier.width(6.dp))
        Text("${w.temp}°  ${w.high}° / ${w.low}°  ${ui.cfg.weatherPlace}${if (w.metNo) " · MET Norway" else ""}", color = MUTED, fontSize = size, fontFamily = CLOCK, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Free memory, in the weather's quiet style, and when the sleep timer turns the TV off. */
@Composable
private fun MemoryLine(ui: Ui, size: androidx.compose.ui.unit.TextUnit, modifier: Modifier = Modifier) {
    val sleepAt by SleepTimer.at.collectAsState()
    val m = ui.memory
    if (m == null && sleepAt == null) return
    val icon = with(androidx.compose.ui.platform.LocalDensity.current) { size.toDp() * 1.3f }
    Column(modifier) {
        m?.let {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(Icons.Outlined.Memory, null, Modifier.size(icon), colorFilter = ColorFilter.tint(MUTED))
                Spacer(Modifier.width(6.dp))
                Text(Memory.line(it), color = MUTED, fontSize = size, fontFamily = CLOCK, maxLines = 1)
            }
        }
        sleepAt?.let {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(Icons.Outlined.Timer, null, Modifier.size(icon), colorFilter = ColorFilter.tint(MUTED))
                Spacer(Modifier.width(6.dp))
                Text("Sleeps at ${DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(it))}", color = MUTED, fontSize = size, fontFamily = CLOCK, maxLines = 1)
            }
        }
    }
}

/** WMO weather codes, as Open-Meteo reports them, to a drawn icon. */
private fun weatherIcon(w: WeatherNow): ImageVector = when (w.code) {
    0 -> if (w.day) Icons.Outlined.WbSunny else Icons.Outlined.NightsStay
    1, 2 -> if (w.day) Icons.Outlined.WbCloudy else Icons.Outlined.NightsStay
    3 -> Icons.Outlined.Cloud
    45, 48 -> Icons.Outlined.Dehaze
    in 71..77, 85, 86 -> Icons.Outlined.AcUnit
    in 95..99 -> Icons.Outlined.Thunderstorm
    in 51..67, in 80..82 -> Icons.Outlined.WaterDrop
    else -> Icons.Outlined.Cloud
}

@Composable
private fun RoundButton(icon: ImageVector, label: String, ui: Ui, onClick: () -> Unit) {
    val compact = ui.cfg.layout in setOf("cinema", "horizon", "epure")
    val (scale, glow, border) = focusStyle(ui, RoundedCornerShape(50), zoom = 1.12f)
    var focused by remember { mutableStateOf(false) }
    Surface(
        onClick = onClick,
        modifier = Modifier.pointer(onClick = onClick).size(if (compact) 32.dp else 44.dp).onFocusChanged { focused = it.isFocused },
        shape = ClickableSurfaceDefaults.shape(CircleShape), scale = scale, glow = glow, border = border,
        colors = ClickableSurfaceDefaults.colors(containerColor = Color.White.copy(alpha = 0.08f), focusedContainerColor = ui.accent.copy(alpha = 0.9f))
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Image(icon, label, Modifier.size(if (compact) 18.dp else 24.dp), colorFilter = ColorFilter.tint(if (focused) onAccent(ui.accent) else TEXT))
        }
    }
}

@Composable
private fun QuickButton(a: QuickAction, ui: Ui, onQuick: (QuickAction) -> Unit, compact: Boolean = false) {
    val glass = compact && ui.cfg.layout == "cinema"
    val (scale, glow, border) = focusStyle(ui, RoundedCornerShape(50), zoom = 1.12f)
    var focused by remember { mutableStateOf(false) }
    Surface(
        onClick = { onQuick(a) },
        modifier = Modifier.pointer { onQuick(a) }.size(if (compact) 32.dp else 40.dp).onFocusChanged { focused = it.isFocused },
        shape = ClickableSurfaceDefaults.shape(CircleShape),
        scale = if (glass) ClickableSurfaceDefaults.scale(focusedScale = 1.06f) else scale,
        glow = if (glass) ClickableSurfaceDefaults.glow() else glow,
        border = if (glass) ClickableSurfaceDefaults.border(focusedBorder = Border(BorderStroke(1.dp, Color.White.copy(alpha = 0.65f)), shape = CircleShape)) else border,
        colors = ClickableSurfaceDefaults.colors(containerColor = Color.White.copy(alpha = if (compact) 0f else 0.08f),
            focusedContainerColor = if (glass) Color.White.copy(alpha = 0.2f) else ui.accent.copy(alpha = 0.9f))
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            QuickGlyph(a, ui, focused && !glass, if (glass) 16.dp else if (compact) 18.dp else 22.dp)
        }
    }
}

/**
 * The icon inside a quick button: the drawn glyph, or an app's own icon at the glyph's size plus the room
 * a glyph leaves inside its box, so app icons sit on the same centre line and look the same size.
 */
@Composable
private fun QuickGlyph(a: QuickAction, ui: Ui, focused: Boolean, size: Dp) {
    val art = a.art
    if (art != null) Image(art, a.label, Modifier.size(size + 4.dp).clip(CircleShape))
    else Image(iconFor(a, ui.cfg), a.label, Modifier.size(size), colorFilter = ColorFilter.tint(if (focused) onAccent(ui.accent) else TEXT))
}

@Composable
private fun QuickRow(a: QuickAction, ui: Ui, onQuick: (QuickAction) -> Unit, expanded: Boolean) =
    RailItem(iconFor(a, ui.cfg), a.label, ui, expanded, glyph = { focused -> QuickGlyph(a, ui, focused, 22.dp) }) { onQuick(a) }

@Composable
private fun RailItem(icon: ImageVector, label: String, ui: Ui, expanded: Boolean, glyph: (@Composable (Boolean) -> Unit)? = null, onClick: () -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    val (_, glow, border) = focusStyle(ui, shape)
    var focused by remember { mutableStateOf(false) }
    val ink = if (focused) onAccent(ui.accent) else TEXT
    Surface(
        onClick = onClick,
        modifier = Modifier.pointer(onClick = onClick).height(44.dp).then(if (expanded) Modifier.fillMaxWidth() else Modifier.width(44.dp)).onFocusChanged { focused = it.isFocused },
        shape = ClickableSurfaceDefaults.shape(shape), scale = ClickableSurfaceDefaults.scale(focusedScale = 1.04f), glow = glow, border = border,
        colors = ClickableSurfaceDefaults.colors(containerColor = Color.Transparent, focusedContainerColor = ui.accent.copy(alpha = 0.9f))
    ) {
        Row(Modifier.fillMaxSize().padding(horizontal = 11.dp), verticalAlignment = Alignment.CenterVertically) {
            // A fixed 22 dp slot, so labels line up whether the icon is a glyph or an app's.
            Box(Modifier.size(22.dp), contentAlignment = Alignment.Center) {
                if (glyph != null) glyph(focused) else Image(icon, label, Modifier.size(22.dp), colorFilter = ColorFilter.tint(ink))
            }
            if (expanded) {
                Spacer(Modifier.width(14.dp))
                Text(label, color = ink, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun Greeting(cfg: HomeConfig, size: androidx.compose.ui.unit.TextUnit) {
    if (cfg.greeting.isBlank()) return
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val part = when (hour) { in 5..11 -> "Good morning"; in 12..17 -> "Good afternoon"; else -> "Good evening" }
    // The name in a light shade of the accent, light enough to read on any wallpaper.
    val name = lerp(Color(cfg.accent), Color.White, 0.45f)
    Text(buildAnnotatedString { append("$part, "); withStyle(SpanStyle(color = name)) { append(cfg.greeting) } }, color = TEXT, fontSize = size, fontWeight = FontWeight.Medium, maxLines = 2)
}

@Composable
private fun Clock(cfg: HomeConfig, timeSize: androidx.compose.ui.unit.TextUnit, dateSize: androidx.compose.ui.unit.TextUnit, align: Alignment.Horizontal) {
    if (cfg.clock == "off") return
    var now by remember { mutableStateOf(Date()) }
    LaunchedEffect(Unit) { while (true) { now = Date(); delay(1000L - System.currentTimeMillis() % 1000) } }
    Column(horizontalAlignment = align) {
        Text(DateFormat.getTimeInstance(DateFormat.SHORT).format(now), color = TEXT, fontSize = timeSize, fontFamily = CLOCK, fontWeight = FontWeight.Light, maxLines = 1, softWrap = false)
        if (cfg.clock == "time_date") Text(DateFormat.getDateInstance(DateFormat.FULL).format(now), color = MUTED, fontSize = dateSize, fontFamily = CLOCK)
    }
}

/**
 * The idle screen, in the owner's style: a large clock and the weather in the lower left (corner), a bigger
 * one in the middle (center), or the slideshow's photos, or the themes' when there are none, with the clock
 * over them (photos). Everything stays inside the safe area.
 */
@Composable
private fun IdleScreen(ui: Ui, modifier: Modifier) {
    Box(modifier) {
        when (ui.cfg.idleStyle) {
            "center" -> Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                Clock(ui.cfg, 120.sp, 22.sp, Alignment.CenterHorizontally)
                WeatherLine(ui, 18.sp, Modifier.padding(top = 12.dp))
            }
            else -> {
                if (ui.cfg.idleStyle == "photos") IdlePhotos()
                Column(Modifier.align(Alignment.BottomStart).padding(start = SAFE_X, bottom = SAFE_Y + 12.dp)) {
                    Clock(ui.cfg, 64.sp, 18.sp, Alignment.Start)
                    WeatherLine(ui, 16.sp, Modifier.padding(top = 8.dp))
                }
            }
        }
    }
}

/** A new photo every 20 seconds, fading over the last, with a shade so the clock reads on any of them. */
@Composable
private fun IdlePhotos() {
    val ctx = LocalContext.current
    val files = remember {
        (0 until Slides.count(ctx)).map { Slides.file(ctx, it) }.ifEmpty {
            ThemePack.list(ctx).flatMap { t -> (0 until t.pictures).map { ThemePack.picture(ctx, t.id, it) } }
        }
    }
    if (files.isEmpty()) return
    var at by remember { mutableIntStateOf(0) }
    var photo by remember { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(Unit) {
        while (true) {
            photo = withContext(Dispatchers.IO) { runCatching { BitmapFactory.decodeFile(files[at % files.size].path)?.asImageBitmap() }.getOrNull() }
            delay(20_000L)
            at++
        }
    }
    androidx.compose.animation.Crossfade(photo, animationSpec = tween(1500), label = "idle photo") { p ->
        if (p != null) Image(p, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
    }
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.55f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.7f))))
}

/** The sleep timer's choices, from the top bar icon. */
@Composable
private fun TimerMenu(ui: Ui, onClose: () -> Unit) {
    val ctx = LocalContext.current
    val first = remember { FocusRequester() }
    val sleepAt by SleepTimer.at.collectAsState()
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(scrim(ui.cfg, 0.85f)), contentAlignment = Alignment.Center) {
            Column(Modifier.width(360.dp).background(PANEL, RoundedCornerShape(16.dp)).padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Sleep timer", color = TEXT, fontSize = 22.sp, fontWeight = FontWeight.Medium)
                Text(sleepAt?.let { "The TV sleeps at ${DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(it))}" } ?: "The TV goes to sleep after", color = MUTED, fontSize = 15.sp, modifier = Modifier.padding(bottom = 6.dp))
                val items = SleepTimer.CHOICES.map { m -> Limits.words(m) to { SleepTimer.set(ctx, m) } } + (if (sleepAt != null) listOf("Off" to { SleepTimer.set(ctx, null) }) else emptyList())
                items.forEachIndexed { i, (label, act) ->
                    val shape = RoundedCornerShape(12.dp)
                    val (scale, glow, border) = focusStyle(ui, shape, zoom = 1.03f)
                    Surface(
                        onClick = { act(); onClose() },
                        modifier = Modifier.pointer { act(); onClose() }.then(if (i == 0) Modifier.focusRequester(first) else Modifier).fillMaxWidth().height(50.dp),
                        shape = ClickableSurfaceDefaults.shape(shape), scale = scale, glow = glow, border = border,
                        colors = ClickableSurfaceDefaults.colors(containerColor = TILE, focusedContainerColor = menuFocus(ui))
                    ) {
                        Box(Modifier.fillMaxSize().padding(horizontal = 18.dp), contentAlignment = Alignment.CenterStart) { Text(label, color = TEXT, fontSize = 17.sp) }
                    }
                }
            }
        }
        LaunchedEffect(Unit) { runCatching { first.requestFocus() } }
    }
}

// ---- Overlays -------------------------------------------------------------------------------------

/** A folder's apps over a dimmed home screen. Back closes it. */
@Composable
private fun FolderOverlay(name: String, list: List<TvApp>, ui: Ui, onClose: () -> Unit) {
    if (ui.cfg.layout == "cinema") {
        CinemaAppLibrary(name, list, ui, onClose)
        return
    }
    val focus = remember { FocusRequester() }
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(scrim(ui.cfg, 0.9f)), contentAlignment = Alignment.Center) {
            Column(Modifier.background(PANEL, RoundedCornerShape(16.dp)).padding(horizontal = 20.dp, vertical = 18.dp)) {
                Text(name, color = TEXT, fontSize = 24.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(start = BLEED, bottom = 6.dp))
                val inner = Ui(ui.cfg, ui.accent, focus, list.firstOrNull()?.let { "folder:${it.pkg}" }, ui.onFolder, motion = ui.motion, onAppMenu = ui.onAppMenu)
                list.chunked(5).forEach { row ->
                    Row(Modifier.padding(horizontal = BLEED, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(GAP)) {
                        for (app in row) Tile("folder", app, inner, Modifier.width(148.dp), big = false)
                    }
                }
            }
        }
        LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    }
}

/** A bounded, scrollable application library: long lists stay navigable with the remote. */
@Composable
private fun CinemaAppLibrary(name: String, apps: List<TvApp>, ui: Ui, onClose: () -> Unit) {
    val focus = remember { FocusRequester() }
    val inner = Ui(ui.cfg.copy(focus = "outline", showNames = true, nameFocus = false), ui.accent, focus,
        apps.firstOrNull()?.let { "folder:${it.pkg}" }, ui.onFolder, motion = ui.motion, onAppMenu = ui.onAppMenu)
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.68f)), contentAlignment = Alignment.Center) {
            val shape = RoundedCornerShape(28.dp)
            Column(Modifier.widthIn(max = 880.dp).fillMaxWidth(0.9f).fillMaxHeight(0.86f)
                .shadow(24.dp, shape).background(Color(0xFF15171C), shape)
                .border(1.dp, Color.White.copy(alpha = 0.09f), shape).padding(28.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(name, color = TEXT, fontSize = 27.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(stringResource(R.string.cinema_library_count, apps.size), color = TEXT.copy(alpha = 0.45f), fontSize = 12.sp)
                    }
                    CinemaAction(stringResource(R.string.cinema_library_close), ui, quiet = true,
                        modifier = if (apps.isEmpty()) Modifier.focusRequester(focus) else Modifier, onClick = onClose)
                }
                Spacer(Modifier.height(20.dp))
                Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.07f)))
                if (apps.isEmpty()) {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.cinema_empty_body), color = MUTED, fontSize = 15.sp)
                    }
                } else {
                    LazyVerticalGrid(columns = GridCells.Adaptive(108.dp), modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentPadding = PaddingValues(vertical = 24.dp, horizontal = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(22.dp)) {
                        gridItems(apps, key = { it.pkg }) { app ->
                            Tile("folder", app, inner, Modifier.fillMaxWidth(), big = false)
                        }
                    }
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.07f)))
                Text(stringResource(R.string.cinema_library_hint), color = TEXT.copy(alpha = 0.4f), fontSize = 11.sp,
                    modifier = Modifier.padding(top = 16.dp))
            }
        }
        LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    }
}

/** An app being moved with the arrow keys: "main" for the dock and All apps, or "row<i>". */
private data class Moving(val kind: String, val pkg: String, val label: String)

private val DONE_KEYS = setOf(Key.DirectionCenter, Key.Enter, Key.NumPadEnter, Key.Back, Key.Escape)
private val OK_KEYS = setOf(Key.DirectionCenter, Key.Enter, Key.NumPadEnter)

/**
 * What a long press on a tile offers (Discord request): Move, Hide, App info (where Android keeps Force stop
 * and Clear data) and, for apps the owner installed, Uninstall. Android lets only system apps force-stop
 * another app, so that one goes through App info.
 */
@Composable
private fun AppMenu(app: TvApp, ui: Ui, onMove: (() -> Unit)?, onClose: () -> Unit) {
    val ctx = LocalContext.current
    val focus = remember { FocusRequester() }
    val installed = remember(app.pkg) {
        runCatching { ctx.packageManager.getApplicationInfo(app.pkg, 0).flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM == 0 }.getOrDefault(false)
    }
    fun open(i: android.content.Intent) {
        onClose()
        runCatching { ctx.startActivity(i.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)) }.onFailure { Log.w(TAG, "${app.pkg}: $it") }
    }
    val pkgUri = android.net.Uri.fromParts("package", app.pkg, null)
    val moveLabel = stringResource(R.string.app_menu_move)
    val hideLabel = stringResource(R.string.app_menu_hide)
    val infoLabel = stringResource(R.string.app_menu_info)
    val uninstallLabel = stringResource(R.string.app_menu_uninstall)
    val items = buildList<Triple<String, ImageVector, () -> Unit>> {
        onMove?.let { add(Triple(moveLabel, Icons.Outlined.OpenWith, it)) }
        add(Triple(hideLabel, Icons.Outlined.VisibilityOff, { onClose(); ConfigStore.update(ctx, JSONObject().put("hidden", JSONArray((ui.cfg.hidden + app.pkg).distinct()))) }))
        add(Triple(infoLabel, Icons.Outlined.Info, { open(android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, pkgUri)) }))
        if (installed) add(Triple(uninstallLabel, Icons.Outlined.DeleteOutline, { open(android.content.Intent(android.content.Intent.ACTION_DELETE, pkgUri)) }))
    }
    val panel = when (ui.cfg.layout) {
        "cinema" -> Color(0xFF191B20)
        "horizon" -> Color(0xFF151D2C)
        "epure" -> Color(0xFF171719)
        else -> lerp(Color(0xFF181D19), ui.accent, 0.04f)
    }
    val highlight = when (ui.cfg.layout) {
        "epure" -> Color(0xFFE9E9EC)
        "horizon" -> Color(0xFFABC9FF)
        else -> ui.accent
    }
    // The menu opens while OK is still held: that press's repeats and its release would pick Move. Only a
    // press that starts inside the menu counts.
    var fresh by remember { mutableStateOf(false) }
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.62f)).onPreviewKeyEvent { e ->
                when {
                    e.key !in OK_KEYS -> false
                    e.type == KeyEventType.KeyDown && e.nativeKeyEvent.repeatCount == 0 -> { fresh = true; false }
                    else -> !fresh
                }
            },
            contentAlignment = Alignment.Center
        ) {
            val panelShape = RoundedCornerShape(if (ui.cfg.layout == "epure") 18.dp else 26.dp)
            Column(Modifier.width(390.dp).shadow(28.dp, panelShape).background(panel, panelShape)
                .border(1.dp, Color.White.copy(alpha = 0.1f), panelShape).padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(Modifier.fillMaxWidth().padding(start = 8.dp, top = 6.dp, bottom = 14.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Box(Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(Color.White.copy(alpha = 0.05f)), contentAlignment = Alignment.Center) {
                        Image(app.art, null, Modifier.size(36.dp), contentScale = ContentScale.Fit)
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(app.label, color = TEXT, fontSize = 21.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(stringResource(R.string.app_menu_subtitle), color = TEXT.copy(alpha = 0.5f), fontSize = 12.sp)
                    }
                }
                Box(Modifier.fillMaxWidth().padding(horizontal = 8.dp).height(1.dp).background(Color.White.copy(alpha = 0.07f)))
                Spacer(Modifier.height(4.dp))
                items.forEachIndexed { i, (label, icon, act) ->
                    val shape = RoundedCornerShape(14.dp)
                    val destructive = icon == Icons.Outlined.DeleteOutline
                    val actionColour = if (destructive) Color(0xFFFFAAA7) else highlight
                    Surface(
                        onClick = act,
                        modifier = Modifier.pointer { act() }.then(if (i == 0) Modifier.focusRequester(focus) else Modifier).fillMaxWidth().height(52.dp),
                        shape = ClickableSurfaceDefaults.shape(shape),
                        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.015f),
                        glow = ClickableSurfaceDefaults.glow(),
                        border = ClickableSurfaceDefaults.border(focusedBorder = Border(BorderStroke(1.dp, actionColour.copy(alpha = 0.65f)), shape = shape)),
                        colors = ClickableSurfaceDefaults.colors(containerColor = Color.Transparent, focusedContainerColor = lerp(panel, actionColour, 0.13f))
                    ) {
                        Row(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            Image(icon, null, Modifier.size(20.dp), colorFilter = ColorFilter.tint(actionColour))
                            Text(label, color = if (destructive) actionColour else TEXT, fontSize = 15.sp)
                        }
                    }
                }
                Text(stringResource(R.string.app_menu_back), color = TEXT.copy(alpha = 0.38f), fontSize = 11.sp,
                    modifier = Modifier.padding(start = 10.dp, top = 10.dp, bottom = 3.dp))
            }
        }
        LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    }
}

@Composable
private fun InputsOverlay(inputs: List<Pair<String, String>>, ui: Ui, onClose: () -> Unit) {
    val ctx = LocalContext.current
    val focus = remember { FocusRequester() }
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(scrim(ui.cfg, 0.9f)), contentAlignment = Alignment.Center) {
            Column(Modifier.width(400.dp).background(PANEL, RoundedCornerShape(16.dp)).padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(ui.cfg.names["inputs"] ?: "Inputs", color = TEXT, fontSize = 24.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(bottom = 6.dp))
                inputs.forEachIndexed { i, (id, label) ->
                    val shape = RoundedCornerShape(12.dp)
                    val (scale, glow, border) = focusStyle(ui, shape, zoom = 1.03f)
                    Surface(
                        onClick = { onClose(); openInput(ctx, id) },
                        modifier = Modifier.pointer { onClose(); openInput(ctx, id) }.then(if (i == 0) Modifier.focusRequester(focus) else Modifier).fillMaxWidth().height(52.dp),
                        shape = ClickableSurfaceDefaults.shape(shape), scale = scale, glow = glow, border = border,
                        colors = ClickableSurfaceDefaults.colors(containerColor = TILE, focusedContainerColor = menuFocus(ui))
                    ) {
                        Box(Modifier.fillMaxSize().padding(horizontal = 18.dp), contentAlignment = Alignment.CenterStart) {
                            Text(label, color = TEXT, fontSize = 17.sp)
                        }
                    }
                }
            }
        }
        LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    }
}
