package dev.linkinxp.termlauncher.screens

import android.app.Application
import android.os.Looper
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import dev.linkinxp.termlauncher.LauncherUi
import dev.linkinxp.termlauncher.LauncherViewModel
import dev.linkinxp.termlauncher.apps.AppEntry
import dev.linkinxp.termlauncher.apps.DrawerSort
import dev.linkinxp.termlauncher.apps.buildDrawer
import dev.linkinxp.termlauncher.layout.HomeStyle
import dev.linkinxp.termlauncher.theme.ThemeRegistry
import dev.linkinxp.termlauncher.ui.AppDrawer
import dev.linkinxp.termlauncher.ui.TermTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class IconDrawerTest {
    @get:Rule val rule = createComposeRule()

    private val names = listOf("Bandcamp", "Calendar", "Camera", "Chrome", "Maps", "Notes")
    private val apps = names.mapIndexed { i, n -> AppEntry("pkg.${n.lowercase()}", n, i.toLong()) }

    private fun show(filter: String = "", style: HomeStyle = HomeStyle.ICONS): LauncherViewModel {
        val vm = LauncherViewModel(ApplicationProvider.getApplicationContext<Application>())
        val drawer = buildDrawer(apps, emptySet(), emptyMap(), DrawerSort.AZ, filter, false)
        val ui = LauncherUi(
            theme = ThemeRegistry.default, apps = apps.associateBy { it.pkg }, drawer = drawer, filter = filter,
            drawerOpen = true, homeStyle = style, drawerKeyboard = false,
        )
        rule.setContent { TermTheme(ui.theme) { AppDrawer(ui, vm, {}, {}) } }
        return vm
    }

    @Test fun theGridShowsLetterHeadersCellsAndTheRail() {
        show()
        rule.onNodeWithText("-- B --").assertIsDisplayed()
        rule.onNodeWithText("bandcamp").assertIsDisplayed()
        rule.onNodeWithText("camera").assertIsDisplayed()
        assertTrue(rule.onAllNodesWithText("B").fetchSemanticsNodes().isNotEmpty()) // the A-Z rail
    }

    @Test fun searchingShowsAFlatGridWithoutHeaders() {
        show(filter = "cam")
        rule.onNodeWithText("camera").assertIsDisplayed()
        assertEquals(0, rule.onAllNodesWithText("-- C --").fetchSemanticsNodes().size)
    }

    @Test fun theControlsLiveInTheBottomBar() {
        show()
        rule.onNodeWithTag("drawer-cfg").assertIsDisplayed()
        rule.onNodeWithText("sort:a-z", substring = true).assertIsDisplayed()
        rule.onNodeWithTag("drawer-search").assertIsDisplayed()
    }

    @Test fun tappingACellLaunchesIt() {
        val vm = show()
        rule.onNodeWithText("maps").performClick()
        val end = System.currentTimeMillis() + 8_000
        while (vm.ui.value.prompt.message == null && System.currentTimeMillis() < end) { shadowOf(Looper.getMainLooper()).idle(); rule.waitForIdle(); Thread.sleep(20) }
        assertEquals("exec: pkg.maps: not found", vm.ui.value.prompt.message) // no such app installed in the test: launch reports its package
    }

    @Test fun otherStylesKeepTheListDrawer() {
        show(style = HomeStyle.CLASSIC)
        rule.onNodeWithText("-- B --").assertIsDisplayed()
        rule.onNodeWithText("| grep ", substring = true).assertIsDisplayed()
    }

    @Test fun theSearchFieldIsNotFocusedWhenTheKeyboardSettingIsOff() {
        val vm = LauncherViewModel(ApplicationProvider.getApplicationContext<Application>())
        val open = androidx.compose.runtime.mutableStateOf(false)
        val ui = LauncherUi(theme = ThemeRegistry.default, apps = apps.associateBy { it.pkg }, drawer = buildDrawer(apps, emptySet(), emptyMap(), DrawerSort.AZ, "", false), homeStyle = HomeStyle.ICONS, drawerKeyboard = false)
        rule.setContent { TermTheme(ui.theme) { AppDrawer(ui, vm, {}, {}, open = { open.value }) } }
        open.value = true
        rule.waitForIdle()
        rule.onNodeWithTag("drawer-search").assertIsNotFocused()
    }

    @Test fun theLastSearchIsGoneWhenTheDrawerIsOpenedAgain() {
        val vm = LauncherViewModel(ApplicationProvider.getApplicationContext<Application>())
        val open = androidx.compose.runtime.mutableStateOf(true)
        val ui = LauncherUi(theme = ThemeRegistry.default, apps = apps.associateBy { it.pkg }, drawer = buildDrawer(apps, emptySet(), emptyMap(), DrawerSort.AZ, "", false), homeStyle = HomeStyle.ICONS, drawerKeyboard = false)
        rule.setContent { TermTheme(ui.theme) { AppDrawer(ui, vm, {}, {}, open = { open.value }) } }
        rule.onNodeWithTag("drawer-search").performTextInput("Claude")
        rule.onNodeWithText("Claude", substring = true).assertIsDisplayed()
        open.value = false
        pump()
        open.value = true
        pump()
        assertEquals(0, rule.onAllNodesWithText("Claude", substring = true).fetchSemanticsNodes().size)
    }

    @Test fun aClosedDrawerCannotTakeFocusSoItNeverPullsTheKeyboardUpOnHome() {
        val vm = LauncherViewModel(ApplicationProvider.getApplicationContext<Application>())
        val ui = LauncherUi(theme = ThemeRegistry.default, apps = apps.associateBy { it.pkg }, drawer = buildDrawer(apps, emptySet(), emptyMap(), DrawerSort.AZ, "", false), homeStyle = HomeStyle.ICONS, drawerKeyboard = true)
        rule.setContent { TermTheme(ui.theme) { AppDrawer(ui, vm, {}, {}, open = { false }) } }
        rule.onNodeWithTag("drawer-search").performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.RequestFocus)
        pump()
        rule.onNodeWithTag("drawer-search").assertIsNotFocused()
    }

    private fun pump() { repeat(15) { shadowOf(Looper.getMainLooper()).idle(); rule.waitForIdle(); Thread.sleep(20) } }

    @Test fun theDrawerGridFollowsTheDensityColumns() {
        val many = (1..6).map { AppEntry("pkg.c$it", "Cc$it", it.toLong()) }
        val vm = LauncherViewModel(ApplicationProvider.getApplicationContext<Application>())
        val ui = LauncherUi(
            theme = ThemeRegistry.default, apps = many.associateBy { it.pkg }, drawer = buildDrawer(many, emptySet(), emptyMap(), DrawerSort.AZ, "", false),
            drawerOpen = true, homeStyle = HomeStyle.ICONS, density = dev.linkinxp.termlauncher.layout.Density.DENSE, drawerKeyboard = false,
        )
        rule.setContent { TermTheme(ui.theme) { AppDrawer(ui, vm, {}, {}) } }
        val a = rule.onNodeWithText("cc1").getUnclippedBoundsInRoot().top
        val f = rule.onNodeWithText("cc6").getUnclippedBoundsInRoot().top
        assertEquals(a.value, f.value, 0.5f)
    }

    @Test fun searchExtrasAppearUnderTheAppsAndTapsAreReported() {
        val vm = LauncherViewModel(ApplicationProvider.getApplicationContext<Application>())
        val web = mutableListOf<String>()
        val ui = LauncherUi(
            theme = ThemeRegistry.default, apps = apps.associateBy { it.pkg }, drawer = buildDrawer(apps, emptySet(), emptyMap(), DrawerSort.AZ, "cam", false), filter = "cam",
            drawerOpen = true, homeStyle = HomeStyle.ICONS, drawerKeyboard = false,
            extras = dev.linkinxp.termlauncher.search.buildExtras("cam", emptyList(), contactsAllowed = true),
        )
        rule.setContent { TermTheme(ui.theme) { AppDrawer(ui, vm, {}, {}, onWebSearch = { web += it }) } }
        rule.onNodeWithText("search the web: cam", substring = true).assertIsDisplayed()
        rule.onNodeWithText("search the web: cam", substring = true).performClick()
        assertEquals(listOf("cam"), web)
    }
}
