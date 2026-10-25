package dev.linkinxp.termlauncher.store

import android.app.Application
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureRoboImage
import dev.linkinxp.termlauncher.LauncherUi
import dev.linkinxp.termlauncher.LauncherViewModel
import dev.linkinxp.termlauncher.apps.AppEntry
import dev.linkinxp.termlauncher.apps.DrawerSort
import dev.linkinxp.termlauncher.apps.buildDrawer
import dev.linkinxp.termlauncher.calendar.AgendaEvent
import dev.linkinxp.termlauncher.calendar.formatAgenda
import dev.linkinxp.termlauncher.clock.ClockStyle
import dev.linkinxp.termlauncher.icons.IconRepository
import dev.linkinxp.termlauncher.layout.Density
import dev.linkinxp.termlauncher.layout.HomeLayout
import dev.linkinxp.termlauncher.layout.HomeStyle
import dev.linkinxp.termlauncher.layout.Item
import dev.linkinxp.termlauncher.layout.Page
import dev.linkinxp.termlauncher.layout.PanelKind
import dev.linkinxp.termlauncher.panel.PanelData
import dev.linkinxp.termlauncher.theme.ThemeRegistry
import dev.linkinxp.termlauncher.ui.AppDrawer
import dev.linkinxp.termlauncher.ui.PagerHome
import dev.linkinxp.termlauncher.ui.SettingsScreen
import dev.linkinxp.termlauncher.ui.TermText
import dev.linkinxp.termlauncher.ui.TermTheme
import dev.linkinxp.termlauncher.ui.color
import dev.linkinxp.termlauncher.wall.SvgStyle
import dev.linkinxp.termlauncher.wall.WallMode
import dev.linkinxp.termlauncher.wall.WallSettings
import dev.linkinxp.termlauncher.weather.Forecast
import dev.linkinxp.termlauncher.weather.HourPoint
import dev.linkinxp.termlauncher.weather.formatWeather
import dev.linkinxp.termlauncher.weather.weatherSummary
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.Instant

/**
 * The Google Play listing assets, drawn with invented app names and shape icons (no real brands):
 * `recordRoborazziDebug --tests '*StoreAssetsTest'` writes them to app/build/store.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w360dp-h720dp-xxhdpi")
class StoreAssetsTest {
    @get:Rule val rule = createComposeRule()

    /** A simple outline glyph (the monochrome layer of a made-up app icon). */
    private class Glyph(private val kind: Int) : Drawable() {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND; color = 0xFFFFFFFF.toInt() }
        override fun draw(c: Canvas) {
            val b = bounds; val w = b.width().toFloat(); val s = w / 108f
            paint.strokeWidth = 6f * s
            c.save(); c.translate(b.left.toFloat(), b.top.toFloat())
            fun p(x: Float) = x * s
            val path = Path()
            when (kind % 12) {
                0 -> c.drawCircle(p(54f), p(54f), p(17f), paint)
                1 -> { path.moveTo(p(54f), p(36f)); path.lineTo(p(72f), p(70f)); path.lineTo(p(36f), p(70f)); path.close(); c.drawPath(path, paint) }
                2 -> c.drawRoundRect(p(37f), p(37f), p(71f), p(71f), p(6f), p(6f), paint)
                3 -> { path.moveTo(p(54f), p(34f)); path.lineTo(p(74f), p(54f)); path.lineTo(p(54f), p(74f)); path.lineTo(p(34f), p(54f)); path.close(); c.drawPath(path, paint) }
                4 -> { c.drawLine(p(54f), p(36f), p(54f), p(72f), paint); c.drawLine(p(36f), p(54f), p(72f), p(54f), paint) }
                5 -> { for (i in 0..2) c.drawLine(p(38f + i * 16f), p(70f), p(38f + i * 16f), p(70f - (12f + i * 10f)), paint) }
                6 -> { path.moveTo(p(32f), p(56f)); path.cubicTo(p(42f), p(36f), p(50f), p(76f), p(60f), p(54f)); path.cubicTo(p(66f), p(42f), p(72f), p(56f), p(78f), p(50f)); c.drawPath(path, paint) }
                7 -> { for (k in 0..5) { val a = Math.toRadians(60.0 * k - 30); val x = p(54f) + p(20f) * Math.cos(a).toFloat(); val y = p(54f) + p(20f) * Math.sin(a).toFloat(); if (k == 0) path.moveTo(x, y) else path.lineTo(x, y) }; path.close(); c.drawPath(path, paint) }
                8 -> { c.drawRoundRect(p(32f), p(38f), p(76f), p(66f), p(9f), p(9f), paint); path.moveTo(p(44f), p(66f)); path.lineTo(p(42f), p(76f)); path.lineTo(p(54f), p(66f)); c.drawPath(path, paint) }
                9 -> { c.drawRoundRect(p(32f), p(40f), p(76f), p(70f), p(5f), p(5f), paint); path.moveTo(p(34f), p(43f)); path.lineTo(p(54f), p(58f)); path.lineTo(p(74f), p(43f)); c.drawPath(path, paint) }
                10 -> { c.drawCircle(p(54f), p(48f), p(11f), paint); path.moveTo(p(44f), p(56f)); path.lineTo(p(54f), p(76f)); path.lineTo(p(64f), p(56f)); c.drawPath(path, paint) }
                else -> { c.drawRoundRect(p(32f), p(42f), p(76f), p(72f), p(5f), p(5f), paint); c.drawLine(p(32f), p(42f), p(46f), p(34f), paint); c.drawLine(p(46f), p(34f), p(56f), p(42f), paint) }
            }
            c.restore()
        }
        override fun setAlpha(alpha: Int) { paint.alpha = alpha }
        override fun setColorFilter(cf: ColorFilter?) { paint.colorFilter = cf }
        @Deprecated("required override") override fun getOpacity() = PixelFormat.TRANSLUCENT
    }

    private val names = listOf("Phone", "Messages", "Browser", "Camera", "Maps", "Music", "Notes", "Files", "Mail", "Calendar", "Clock", "Photos", "Weather", "Wallet", "Games", "Podcasts", "Books", "Health", "Shop", "Radio", "Tasks", "Travel", "News", "Recipes")
    private val apps = names.mapIndexed { i, n -> AppEntry("store.${n.lowercase()}", n, i.toLong()) }
    private fun pkg(n: String) = "store.${n.lowercase()}"

    @Before fun installTheFakeApps() {
        IconRepository.reset()
        val app = ApplicationProvider.getApplicationContext<Application>()
        val pm = shadowOf(app.packageManager)
        apps.forEachIndexed { i, a ->
            pm.installPackage(PackageInfo().apply { packageName = a.pkg; applicationInfo = ApplicationInfo().apply { packageName = a.pkg } })
            pm.setApplicationIcon(a.pkg, AdaptiveIconDrawable(ColorDrawable(0xFF222222.toInt()), ColorDrawable(0), Glyph(i)))
        }
    }

    private fun out(name: String) = File("build/store").apply { mkdirs() }.resolve("$name.png").path

    private fun layout(vararg pages: List<Item>) = HomeLayout(
        listOf("Phone", "Messages", "Browser", "Camera").map { Item.App(pkg(it)) },
        pages.map { Page(it) },
    )

    private fun apps(vararg n: String) = n.map { Item.App(pkg(it)) }

    private fun base(theme: String, page: List<Item>, wall: WallSettings = WallSettings(), density: Density = Density.NORMAL) = LauncherUi(
        theme = ThemeRegistry.byName(theme)!!, apps = apps.associateBy { it.pkg },
        layout = layout(page, emptyList()), homeStyle = HomeStyle.ICONS, density = density, wall = wall,
        clockStyle = ClockStyle.PIXEL, clockFormat = dev.linkinxp.termlauncher.clock.ClockFormat.H24,
    )

    private fun shot(name: String, ui: LauncherUi, content: @Composable (LauncherUi, LauncherViewModel) -> Unit) {
        val vm = LauncherViewModel(ApplicationProvider.getApplicationContext<Application>())
        rule.setContent { TermTheme(ui.theme, ui.wall) { content(ui, vm) } }
        rule.waitForIdle(); Thread.sleep(1_200); rule.waitForIdle() // the icon tiles and the wallpaper are drawn off the main thread
        rule.onRoot().captureRoboImage(out(name))
    }

    private val homeApps = apps("Maps", "Music", "Notes", "Files", "Mail", "Calendar", "Photos", "Weather", "Wallet", "Games", "Podcasts", "Books") + Item.Folder("media", listOf(pkg("Music"), pkg("Podcasts"), pkg("Radio"), pkg("Photos")))

    @Test fun phone1IconHome() = shot("phone-1-icon-home", base("tokyo-night", homeApps.take(11) + homeApps.last())) { ui, vm -> PagerHome(ui, vm, {}, {}, {}, {}) }

    @Test fun phone2IconDrawer() {
        val ui = base("matrix", emptyList()).copy(
            drawer = buildDrawer(apps, emptySet(), mapOf(pkg("Phone") to dev.linkinxp.termlauncher.apps.AppStat(9, 5), pkg("Maps") to dev.linkinxp.termlauncher.apps.AppStat(6, 4), pkg("Music") to dev.linkinxp.termlauncher.apps.AppStat(4, 3)), DrawerSort.AZ, "", false),
            drawerOpen = true, drawerKeyboard = false,
        )
        shot("phone-2-icon-drawer", ui) { u, vm -> AppDrawer(u, vm, {}, {}) }
    }

    private fun forecast() = Forecast(
        temp = 14.0, humidity = 62, code = 1, windKmh = 11.0, windDeg = 250, isDay = true, high = 17.0, low = 8.0,
        hours = listOf(HourPoint(9, 12.0, 0), HourPoint(12, 15.0, 1), HourPoint(15, 17.0, 1), HourPoint(18, 14.0, 3), HourPoint(21, 11.0, 3)), fetchedAtMs = 0L,
    )

    @Test fun phone3GlancePage() {
        val start = Instant.now().toEpochMilli()
        val agenda = formatAgenda(
            listOf(
                AgendaEvent("Team standup", start + 3_600_000, start + 4_500_000, false),
                AgendaEvent("Lunch with Sam", start + 14_400_000, start + 18_000_000, false),
                AgendaEvent("Dentist", start + 86_400_000 + 36_000_000, start + 86_400_000 + 39_600_000, false),
                AgendaEvent("Weekend trip", start + 4 * 86_400_000L, start + 5 * 86_400_000L, true),
            ),
            start, java.time.ZoneId.systemDefault(),
        )
        val ui = LauncherUi(
            theme = ThemeRegistry.byName("nord")!!, apps = apps.associateBy { it.pkg }, homeStyle = HomeStyle.ICONS,
            layout = layout(listOf(Item.Panel(PanelKind.SEARCH), Item.Panel(PanelKind.SYS), Item.Panel(PanelKind.CAL), Item.Panel(PanelKind.WX)), emptyList()),
            calendar = PanelData.Lines(agenda), weather = PanelData.Lines(formatWeather("Zagreb", forecast(), false, 0L)),
            weatherLine = weatherSummary("Zagreb", forecast(), false), panels = dev.linkinxp.termlauncher.panel.PanelSettings(cal = true, wx = true, search = true),
            clockFormat = dev.linkinxp.termlauncher.clock.ClockFormat.H24,
        )
        shot("phone-3-glance-page", ui) { u, vm -> PagerHome(u, vm, {}, {}, {}, {}) }
    }

    @Test fun phone4SvgWallpaper() = shot(
        "phone-4-svg-wallpaper",
        base("neon-city", homeApps.take(8), WallSettings(mode = WallMode.SVG, svgStyle = SvgStyle.TUNNEL, dim = 25, seed = 4)),
    ) { ui, vm -> PagerHome(ui, vm, {}, {}, {}, {}) }

    @Test fun phone5Settings() {
        val ui = base("gruvbox", emptyList())
        shot("phone-5-settings", ui) { u, vm -> SettingsScreen(u, vm, {}, {}, {}, {}) }
    }

    @Test fun phone6DenseGrid() = shot(
        "phone-6-dense-grid",
        base("catppuccin", apps("Maps", "Music", "Notes", "Files", "Mail", "Calendar", "Photos", "Weather", "Wallet", "Games", "Podcasts", "Books", "Health", "Shop", "Radio", "Tasks", "Travel", "News", "Recipes", "Clock"), density = Density.DENSE),
    ) { ui, vm -> PagerHome(ui, vm, {}, {}, {}, {}) }

    @Test fun storeIcon512() {
        val size = 512
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(0xFF0A0F0D.toInt())
        val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND }
        val k = size / 72f // the 72 dp visible part of the 108 dp adaptive canvas fills the square
        fun pt(x: Float, y: Float) = (x - 18f) * k to (y - 18f) * k
        fun poly(vararg v: Pair<Float, Float>, color: Int, width: Float) {
            stroke.color = color; stroke.strokeWidth = width * k
            val path = Path(); v.forEachIndexed { i, (x, y) -> val (px, py) = pt(x, y); if (i == 0) path.moveTo(px, py) else path.lineTo(px, py) }
            c.drawPath(path, stroke)
        }
        poly(33f to 38f, 55f to 54f, 33f to 70f, color = 0xFF00FF41.toInt(), width = 7f)
        poly(60f to 71f, 77f to 71f, color = 0xFF00FF41.toInt(), width = 7f)
        poly(33f to 38f, 55f to 54f, 33f to 70f, color = 0xFF7DFFA0.toInt(), width = 2f)
        File("build/store").mkdirs()
        File(out("icon-512")).outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test @Config(sdk = [34], qualifiers = "w1024dp-h500dp-mdpi")
    fun featureGraphic1024x500() {
        val theme = ThemeRegistry.byName("matrix")!!
        rule.setContent {
            TermTheme(theme, WallSettings(mode = WallMode.SVG, svgStyle = SvgStyle.CIRCUIT, dim = 35, seed = 9)) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.background(theme.background.color().copy(alpha = 0.82f)).padding(horizontal = 40.dp, vertical = 28.dp)) {
                        TermText(">_ Terminal", size = 76.sp, bold = true, color = theme.foreground.color())
                        TermText("a launcher for people who live in the terminal", size = 24.sp, color = theme.accent.color(), modifier = Modifier.padding(top = 8.dp))
                        TermText("themes  |  icon grid  |  svg wallpapers  |  prompt", size = 18.sp, dim = true, modifier = Modifier.padding(top = 14.dp))
                    }
                }
            }
        }
        rule.waitForIdle(); Thread.sleep(1_200); rule.waitForIdle()
        rule.onRoot().captureRoboImage(out("feature-graphic-1024x500"))
    }
}
