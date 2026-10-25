# Terminal Launcher Panels and Widgets Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a terminal-style calendar panel and weather panel, and hosting of real Android widgets on home pages.

**Architecture:** Formatting, parsing, settings and layout logic are pure Kotlin units tested on the JVM. Thin Android layers (calendar/weather repositories, widget host controller, Compose panels, permission and bind launchers) consume them and are verified by build, Roborazzi JVM screenshots and a later on-device check.

**Tech Stack:** Kotlin 2.4.21, Jetpack Compose (BOM 2026.09.00), DataStore Preferences 1.3.0, kotlinx-serialization-json, `HttpURLConnection`, `AppWidgetHost`, Roborazzi/Robolectric (SDK 34), JUnit 4.

**Spec:** `docs/superpowers/specs/2026-10-23-terminal-launcher-panels-widgets-design.md`

## Global Constraints

- Work on android-dev (`ssh android-dev`), project `/root/projects/terminal-launcher`, branch `terminal-launcher`; commit after each task. Edit under `/tmp/tl` and push with `/tmp/tl-run.sh <gradle args>`; commit and ledger with `/tmp/tl-commit3.sh` adapted to ledger `/root/projects/.superpowers/progress-panels.md` (copy the script, change the ledger name).
- Package `dev.linkinxp.termlauncher`; minSdk 34, compileSdk 37, targetSdk 36; JDK 17; Robolectric `@Config(sdk = [34])`.
- Per-task test command: `./gradlew --no-daemon testDebugUnitTest --tests '*<Class>'`; the whole suite `./gradlew --no-daemon assembleDebug testDebugUnitTest` must stay green.
- New permissions: `READ_CALENDAR` (runtime) and `INTERNET`. Nothing else. Weather sends only latitude/longitude (and the city name for the one-time search) to Open-Meteo; no location permission.
- Reserved words gain `panel` and `wx`. Help text must stay within 12 lines of at most 40 columns (merge related commands).
- Panels default off; order on page 1 under `sys`: `cal`, then `wx`. Calendar: 6 events, 7 days, width 34. Weather: refresh every 30 minutes while resumed and on demand; cache last good response; every formatted weather line at most 40 columns.
- Widget heights: SMALL 96, MEDIUM 192, LARGE 288 dp, minimum wins, cap 400. Host id constant `0x7E57`.
- Real calendar/weather/widget behavior is checked by the user on the phone after this build; never claim it works without that. Never take screenshots of the user's phone (adb screencap). Never run the signed-release flow.
- Out of scope: notifications feed, media controls, quick toggles, widgets on the dock or in folders, widget drag-reordering, location-based weather, alerts, multiple cities, calendar write access.

## Review Focus

- Agenda: DST day boundaries, all-day events (UTC midnights) against the local date, events spanning midnight or ongoing, blank/very long/emoji/RTL titles, empty list.
- Weather: null or missing JSON fields, fewer than 18 future hourly points, HTML or error bodies instead of JSON, city names with spaces, unicode and URL metacharacters (`São Paulo`, `a&b=c`), units c/f.
- Commands with bad input: `panel`, `panel cal`, `panel x on`, `panel cal maybe`, `wx city` without a name, `wx units x`, odd casing: usage message, settings unchanged, no crash.
- Widgets: a provider uninstalled or crashing, bind refused, configure cancelled, a page removed or a widget removed: host ids deleted exactly once and the layout never keeps an id that was deleted.
- Lifecycle: calendar permission denied then granted or revoked later, offline start with and without cache, refresh work stopped while the launcher is backgrounded, stale-cache check on resume.

---

### Task 1: Panel settings, parser and reserved words

**Files:**
- Create: `panel/PanelSettings.kt`, `panel/PanelData.kt`
- Modify: `prompt/CommandParser.kt` (`Command.Panel`, `Command.Wx`), `layout/HomeLayout.kt` (reserved words), `LauncherViewModel.kt` (temporary no-op branches, help text, completion list)
- Test: `panel/PanelSettingsTest.kt`, `prompt/CommandParserTest.kt` (extend), `layout/HomeLayoutTest.kt` (extend)

**Interfaces:**
- Produces: `data class PanelSettings(val cal: Boolean = false, val wx: Boolean = false)`; `fun applyPanel(current: PanelSettings, name: String?, on: Boolean?): Result<PanelSettings>`; `fun PanelSettings.describe(): String`; `sealed interface PanelData { data object Off; data class Lines(val lines: List<String>); data class Message(val text: String) }`; `Command.Panel(val name: String?, val on: Boolean?)`; `Command.Wx(val sub: String?, val arg: String?)`.

- [ ] **Step 1: Write the failing tests.** `applyPanel`: `(null, null)` returns current; `("cal", true)` and `("WX", false)` set (case-insensitive); `("x", true)` fails with `panel: x: no such panel (cal wx)`; `("cal", null)` fails with `usage: panel <cal|wx> on|off`; `describe` has a line per panel with `on`/`off`. Parser: `"panel"` -> `Panel(null, null)`; `"panel cal on"` -> `Panel("cal", true)`; `"PANEL WX Off"` -> `Panel("wx", false)`; `"panel cal maybe"` -> `Panel("cal", null)`; `"panel x on"` -> `Panel("x", true)`; `"wx"` -> `Wx(null, null)`; `"wx units f"` -> `Wx("units", "f")`; `"wx city São Paulo"` -> `Wx("city", "São Paulo")` (the argument is the rest of the line); `"WX City  new york "` -> `Wx("city", "new york")`. `HomeLayoutTest`: `mkdir("panel")` and `mkdir("wx")` fail.
- [ ] **Step 2: Run** the three test classes. Expected: FAIL.
- [ ] **Step 3: Implement** the units, parser branches (`wx`: sub is the lowercased first token, arg is the trimmed remainder or null), `panel`/`wx` in `COMMAND_WORDS`, `"panel"`/`"wx"` in the ViewModel `COMMANDS`, temporary `is Command.Panel, is Command.Wx -> null` branch in the ViewModel `when`, and re-flowed `HELP` (at most 12 lines of at most 40 columns, containing every command word).
- [ ] **Step 4: Run** the tests, then the whole suite. Expected: PASS.
- [ ] **Step 5: Commit** `terminal-launcher: panel settings and commands`.

### Task 2: Calendar panel

**Files:**
- Create: `calendar/Agenda.kt`, `calendar/CalendarRepository.kt`, `ui/CalPanel.kt`
- Modify: `AndroidManifest.xml` (`READ_CALENDAR`), `data/Prefs.kt` (panel settings), `LauncherViewModel.kt` (`panels`, `calendar: PanelData` in `LauncherUi`, `panel` command, permission request flow, refresh), `MainActivity.kt` (permission launcher), `ui/PagerHome.kt` (panels under `sys`)
- Test: `calendar/AgendaTest.kt`

**Interfaces:**
- Consumes: `PanelSettings`, `PanelData`, `applyPanel` (Task 1).
- Produces: `data class AgendaEvent(val title: String, val startMs: Long, val endMs: Long, val allDay: Boolean)`; `fun formatAgenda(events: List<AgendaEvent>, nowMs: Long, zone: java.time.ZoneId, maxLines: Int = 6, width: Int = 34): List<String>`; `class CalendarRepository(context)` with `fun hasPermission(): Boolean` and `suspend fun events(fromMs: Long, toMs: Long): List<AgendaEvent>`; ViewModel `val permissionRequests: SharedFlow<String>`, `fun onCalendarPermission(granted: Boolean)`, `fun refreshPanels()`.

- [ ] **Step 1: Write the failing tests** in `AgendaTest` (zone `UTC` unless stated; `now` = 2026-10-23T09:00Z, a Friday): events are sorted by start; an event that ended before now is dropped; an ongoing event line starts with `now` and the others with `today`/`tmrw`/weekday (`sat`, `tue`) followed by `HH:mm`, two spaces and the title (exact expected strings asserted); an all-day event shows `all day` and uses UTC-midnight begin/end compared with the local date (all-day on 2026-10-24 -> `tmrw all day  <title>`); a title longer than the room is cut with `…` so the line is exactly `width` characters; blank title -> `(no title)`; empty list -> `["no events"]`; `maxLines = 2` keeps the first two; a DST zone (`Europe/Zagreb`, event on 2026-10-25 after the clock change) shows the local time correctly; an event spanning midnight that has started shows `now`; titles with emoji or RTL characters never make a line longer than `width` code units.
- [ ] **Step 2: Run** `--tests '*AgendaTest'`. Expected: FAIL.
- [ ] **Step 3: Implement** `formatAgenda`. Then the Android layer: manifest permission; `CalendarRepository` (permission check with `ContextCompat.checkSelfPermission`, `CalendarContract.Instances` query for `[from, to]` with TITLE/BEGIN/END/ALL_DAY, on `Dispatchers.IO`, any exception -> empty list); Prefs stores `PanelSettings` (corrupt/missing -> both off); ViewModel handles `Command.Panel` (bare -> `describe()`, otherwise `applyPanel` then save; turning `cal` on without permission emits `Manifest.permission.READ_CALENDAR` on `permissionRequests`), computes `calendar: PanelData` (`Off` when off; `Message("cal: no permission (grant in Settings)")` without permission; otherwise `Lines(formatAgenda(...))`), and `refreshPanels()` is called on the minute tick, on resume and when a panel/permission changes; `MainActivity` collects `permissionRequests` with an `ActivityResultContracts.RequestPermission` launcher and reports the result; `CalPanel` shows a `TermPanel("cal")` and `PagerHome` places it under the `sys` panel when the data is not `Off`.
- [ ] **Step 4: Run** `--tests '*AgendaTest'`, then `assembleDebug testDebugUnitTest`. Expected: PASS, BUILD SUCCESSFUL.
- [ ] **Step 5: Commit** `terminal-launcher: calendar panel`.

### Task 3: Weather logic (pure)

**Files:**
- Create: `weather/Forecast.kt`, `weather/WeatherArt.kt`, `weather/WeatherFormat.kt`, `weather/WxSettings.kt`
- Test: `weather/ForecastTest.kt`, `weather/WeatherArtTest.kt`, `weather/WeatherFormatTest.kt`, `weather/WxSettingsTest.kt`

**Interfaces:**
- Produces: `HourPoint(hour: Int, temp: Double, code: Int)`; `Forecast(temp, humidity: Int, code: Int, windKmh: Double, windDeg: Int, isDay: Boolean, high: Double, low: Double, hours: List<HourPoint>, fetchedAtMs: Long)`; `fun parseForecast(json: String, fetchedAtMs: Long): Result<Forecast>`; `data class GeoResult(val name: String, val lat: Double, val lon: Double)`; `fun parseGeocode(json: String): Result<GeoResult>`; `enum class Condition { CLEAR, PARTLY, CLOUDY, FOG, DRIZZLE, RAIN, SNOW, SHOWERS, THUNDER }`; `fun conditionOf(code: Int): Condition`; `fun conditionText(c: Condition): String`; `fun asciiIcon(c: Condition, isDay: Boolean): List<String>`; `fun compass(deg: Int): String`; `fun formatWeather(city: String, f: Forecast, fahrenheit: Boolean, nowMs: Long): List<String>`; `data class WxSettings(val city: String? = null, val lat: Double? = null, val lon: Double? = null, val fahrenheit: Boolean = false)`; `sealed interface WxAction { data object Refresh; data class Units(val fahrenheit: Boolean); data class City(val name: String) }`; `fun parseWxAction(sub: String?, arg: String?): Result<WxAction>`.

- [ ] **Step 1: Write the failing tests.**
  - `ForecastTest`: build the Open-Meteo JSON in the test with a helper (`current` with `time`, `temperature_2m`, `relative_humidity_2m`, `weather_code`, `wind_speed_10m`, `wind_direction_10m`, `is_day`; `hourly` arrays for 48 hours; `daily` max/min for 2 days). Assert every field parsed, `hours` has 6 points exactly 3 hours apart after `current.time` with correct `hour` values, `high`/`low` come from day 0, `fetchedAtMs` is passed through; with fewer than 18 hours left `hours` is shorter and no error; a missing `current` block, a `null` temperature, a non-JSON string, an HTML page and an empty string each give `Result.failure` and never throw; `parseGeocode`: one result -> name/lat/lon, `{"results":[]}` and `{}` -> failure with message `not found`, garbage -> failure.
  - `WeatherArtTest`: `conditionOf` for 0, 1, 2, 3, 45, 48, 51, 55, 57, 61, 65, 67, 71, 75, 77, 80, 82, 85, 86, 95, 96, 99 and unknown (-1, 1000 -> CLOUDY); `asciiIcon` for every condition and day/night: exactly 5 lines, each at most 12 columns, printable ASCII, `CLEAR` and `PARTLY` differ between day and night; `conditionText` is non-blank for every condition.
  - `WeatherFormatTest`: `compass(0)=N`, `45=NE`, `90=E`, `135=SE`, `180=S`, `225=SW`, `270=W`, `315=NW`, `360=N`, `-45=NW`, `22=N`, `23=NE`; `formatWeather` output: first line is the lowercase city; contains `10°C` for celsius and `…°F` for fahrenheit, the wind compass and speed, `humid 93%`, `hi`/`lo`, a 3-hour strip that never exceeds 40 columns, and a last line `updated just now` / `updated 12m ago` / `updated 3h ago` / `updated 2d ago` for ages 0 / 12 minutes / 3 hours / 2 days; every line at most 40 columns even with a 60-character city name.
  - `WxSettingsTest`: `parseWxAction(null, null)` -> `Refresh`; `("units","f")` -> `Units(true)`; `("UNITS","C")` -> `Units(false)`; `("units","x")` and `("units", null)` fail with `usage: wx units c|f`; `("city","São Paulo")` -> `City("São Paulo")`; `("city", null)` and `("city","  ")` fail with `usage: wx city <name>`; `("bogus", null)` fails with `wx: bogus: unknown (city units)`.
- [ ] **Step 2: Run** the four test classes. Expected: FAIL.
- [ ] **Step 3: Implement** with `kotlinx.serialization.json` (`Json.parseToJsonElement`, all lookups null-safe, any exception -> `Result.failure`). `hours`: index `i` of `current.time` truncated to the hour in `hourly.time`, then `i+3, i+6, ... i+18` while they exist. WMO mapping as in the spec. Icons: five hand-drawn ASCII lines per condition. `formatWeather` layout per the spec (city line, five icon+info rows, strip, updated line), `°` is the only non-ASCII character allowed.
- [ ] **Step 4: Run** the new tests and the whole suite. Expected: PASS.
- [ ] **Step 5: Commit** `terminal-launcher: weather logic`.

### Task 4: Weather panel (Android)

**Files:**
- Create: `weather/WeatherRepository.kt`, `ui/WxPanel.kt`
- Modify: `AndroidManifest.xml` (`INTERNET`), `data/Prefs.kt` (wx settings, cached JSON + time), `LauncherViewModel.kt` (`weather: PanelData`, `wx` command, refresh scheduling), `ui/PagerHome.kt`

**Interfaces:**
- Consumes: Task 3 units, `PanelData` (Task 1), `refreshPanels()` pattern (Task 2).
- Produces: `class WeatherRepository` with `suspend fun fetch(lat: Double, lon: Double, fahrenheit: Boolean): Result<String>` (raw JSON body) and `suspend fun geocode(name: String): Result<GeoResult>`; Prefs `val wx: Flow<WxSettings>`, `val wxCache: Flow<Pair<String, Long>?>`.

- [ ] **Step 1:** `WeatherRepository`: `HttpURLConnection` GET with 10 s connect/read timeouts on `Dispatchers.IO`, URLs exactly as in the spec with the city name URL-encoded (`URLEncoder.encode(name, "UTF-8")`), non-200 status or any exception -> `Result.failure` with a short message; the body is returned only for HTTP 200. Add the `INTERNET` permission. Run `assembleDebug`. Expected: BUILD SUCCESSFUL.
- [ ] **Step 2:** Prefs: store `WxSettings` (corrupt/missing -> defaults) and the cache (JSON string + fetched-at ms). ViewModel: `Command.Wx` goes through `parseWxAction`; `Refresh` fetches now and prints a status line (`wx: ok` or the error) in the output panel; `Units` saves and refetches; `City` geocodes once, saves name/lat/lon, turns `panel wx` on and fetches (failure -> `wx: city <name>: not found` or `wx: offline`); `weather: PanelData` is `Off` when the panel is off, `Message("wx: set a city: wx city <name>")` without a city, otherwise `Lines(formatWeather(...))` from the cache (or `Message("wx: offline")` with no cache and a failed fetch). Fetching runs on resume and on the minute tick only when the cache is older than 30 minutes and the panel is on, never while the launcher is not resumed; a failed fetch keeps the cache.
- [ ] **Step 3:** `WxPanel` shows `TermPanel("wx", trailing = city)` with the formatted lines; `PagerHome` places it after the `cal` panel.
- [ ] **Step 4: Run** `assembleDebug testDebugUnitTest`. Expected: BUILD SUCCESSFUL, all tests PASS.
- [ ] **Step 5: Commit** `terminal-launcher: weather panel`.

### Task 5: Widget layout model (pure)

**Files:**
- Create: `layout/WidgetHeight.kt`
- Modify: `layout/HomeLayout.kt`
- Test: `layout/WidgetHeightTest.kt`, `layout/HomeLayoutTest.kt` (extend)

**Interfaces:**
- Produces: `enum class WidgetSize { SMALL, MEDIUM, LARGE }`; `Item.Widget(val id: Int, val provider: String, val size: WidgetSize)` (`@SerialName("widget")`); `HomeLayout.addWidget(page: Int, widget: Item.Widget): HomeLayout`; `HomeLayout.removeWidget(id: Int): HomeLayout`; `HomeLayout.resizeWidget(id: Int, size: WidgetSize): HomeLayout`; `HomeLayout.pruneWidgets(providers: Set<String>): Pair<HomeLayout, List<Int>>`; `HomeLayout.widgetIds(): Set<Int>`; `fun widgetHeightDp(providerMinHeightDp: Int, size: WidgetSize): Int`.

- [ ] **Step 1: Write the failing tests.** `widgetHeightDp`: `(0, SMALL)=96`, `(0, MEDIUM)=192`, `(0, LARGE)=288`, `(250, SMALL)=250`, `(500, LARGE)=400`, `(-5, SMALL)=96`. Layout: `addWidget(0, w)` puts the widget on page 0; a missing page leaves the layout unchanged; a duplicate id is ignored; `removeWidget` removes it and leaves other items; `resizeWidget` changes only that widget; `pruneWidgets(setOf("keep"))` removes widgets whose provider is not in the set and returns the removed ids, keeping others and non-widget items; `widgetIds()` lists ids on all pages; `removePage` of a page holding widgets makes those ids disappear from `widgetIds()`; `encode`/`decode` round-trips a layout with widgets; an old layout JSON without widgets (`{"v":1,"layout":{"dock":[],"pages":[{"items":[]}]}}`) decodes unchanged; `prune(installed)` (apps) leaves widgets alone; `mkdir`/`mv`/`rmdir`/`unfile`/`pinFolder` still work on a layout containing a widget.
- [ ] **Step 2: Run** `--tests '*WidgetHeightTest' --tests '*HomeLayoutTest'`. Expected: FAIL.
- [ ] **Step 3: Implement.** `when` expressions over `Item` elsewhere (`folders()`, `prune`, UI code such as `itemLabel`/`dockLabel`) must handle the new subtype: widgets have no label and are skipped by label-based UI.
- [ ] **Step 4: Run** the tests and the whole suite. Expected: PASS.
- [ ] **Step 5: Commit** `terminal-launcher: widget layout model`.

### Task 6: Widget hosting (Android)

**Files:**
- Create: `widgets/WidgetHostController.kt`, `ui/WidgetPanel.kt`
- Modify: `ui/EditMode.kt`, `ui/PagerHome.kt` (render widgets, scrolling rule), `LauncherViewModel.kt`, `MainActivity.kt`

**Interfaces:**
- Consumes: Task 5 model.
- Produces: `class WidgetHostController(context)` with `fun providers(): List<WidgetProviderInfo>` (`data class WidgetProviderInfo(val provider: String, val label: String, val minHeightDp: Int)`), `fun allocate(): Int`, `fun delete(id: Int)`, `fun bindIfAllowed(id: Int, provider: String): Boolean`, `fun bindIntent(id: Int, provider: String): Intent`, `fun needsConfigure(provider: String): Boolean`, `fun configureIntentSender(id: Int)` (via `AppWidgetHost.startAppWidgetConfigureActivityForResult`), `fun createView(id: Int): AppWidgetHostView?`, `fun start()` / `fun stop()` (`startListening`/`stopListening`), host id `0x7E57`.

- [ ] **Step 1:** `WidgetHostController` as above (provider lookup by flattened `ComponentName` among `AppWidgetManager.getInstalledProviders()`; every call tolerates a missing provider and returns false/null instead of throwing). Run `assembleDebug`. Expected: BUILD SUCCESSFUL.
- [ ] **Step 2:** ViewModel/MainActivity flow: add-widget request from edit mode -> `allocate` -> `bindIfAllowed`; if false launch `bindIntent` through an `ActivityResultContracts.StartActivityForResult` launcher; if `needsConfigure`, start the configure activity and wait for its result; on success `addWidget(page, Item.Widget(id, provider, MEDIUM))`, on cancel/failure `delete(id)` and the message `widget: <label>: not added`. After every layout change the ViewModel deletes host ids that were in the previous `widgetIds()` and are gone from the new one (covers remove widget, remove page, prune), and `pruneWidgets(installed providers)` runs on app refresh. `MainActivity` calls `start()`/`stop()` in `onStart`/`onStop`.
- [ ] **Step 3:** `WidgetPanel`: `TermPanel(label.lowercase())` containing `AndroidView` of `createView(id)` with fixed height `widgetHeightDp(minHeight, size)` and width from the panel, calling `updateAppWidgetOptions` with the dp size; a missing provider renders nothing. `PagerHome` renders `Item.Widget` items on their page and makes the page column `verticalScroll(state, enabled = state.maxValue > 0)`; the bottom area (status line, dock, prompt) always accepts the drawer swipe-up. `EditMode`: `[+widget]` per page (provider list), per widget `[S] [M] [L]` and `[rm]`.
- [ ] **Step 4: Run** `assembleDebug testDebugUnitTest`. Expected: BUILD SUCCESSFUL, all tests PASS.
- [ ] **Step 5: Commit** `terminal-launcher: widget hosting`.

### Task 7: Screenshots, README and final verification

**Files:**
- Modify: `screens/ScreenshotsTest.kt`, `README.md`, `app/build.gradle.kts` (`versionName`), `LauncherViewModel.kt` (`LAUNCHER_VERSION`)

- [ ] **Step 1:** Extend `ScreenshotsTest`: home with the calendar panel (sample agenda) in two themes; the cal panel with a permission message; home with the weather panel for four condition groups (clear day, night partly cloudy, rain, thunder) using a built `Forecast`; both panels together over an ASCII wall; the edit-mode widget list with sample providers; a stand-in widget panel (a colored box). Run `./gradlew --no-daemon recordRoborazziDebug`. Expected: BUILD SUCCESSFUL and a PNG per case in `app/build/shots`.
- [ ] **Step 2:** Look at the PNGs; fix visible problems (clipped lines, panels overlapping, unreadable contrast) and re-render until clean.
- [ ] **Step 3:** Update `README.md` (panels, `panel`/`wx` commands, calendar permission, weather source and privacy, widget hosting, new permissions, scrolling note); bump `versionName` and `LAUNCHER_VERSION` to `0.4.0`.
- [ ] **Step 4:** Run `./gradlew --no-daemon clean assembleDebug testDebugUnitTest`. Expected: BUILD SUCCESSFUL, 0 failures.
- [ ] **Step 5:** Commit `terminal-launcher: panels and widgets round screenshots and docs, v0.4.0`.
