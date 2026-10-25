# Terminal Launcher Settings, Backup and Import Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a Google search bar below the clock, a full settings screen, own backup export/import, and import of a layout from a Nova/Lawnchair backup file.

**Architecture:** Backup encoding/decoding, the Launcher3 favorites mapping, search URL building and the small settings helpers are pure Kotlin units tested on the JVM. Thin Android layers (settings screen, document pickers, SQLite/ZIP reader, import dialog, search bar) consume them and are verified by build, Robolectric/Roborazzi and a later on-device check.

**Tech Stack:** Kotlin 2.4.21, Jetpack Compose (BOM 2026.09.00), DataStore Preferences 1.3.0, kotlinx-serialization-json, `SQLiteDatabase`, `java.util.zip`, Activity Result API, Roborazzi/Robolectric (SDK 34), JUnit 4.

**Specs:** `docs/superpowers/specs/2026-10-23-terminal-launcher-settings-import-design.md` for Tasks 2-8; Task 1 (search bar) is specified here (added by the user on 2026-10-24).

## Global Constraints

- Work on android-dev (`ssh android-dev`), project `/root/projects/terminal-launcher`, branch `terminal-launcher`; commit after each task. Edit under `/tmp/tl` and push with `/tmp/tl-run.sh <gradle args>`; commit and ledger with a copy of `/tmp/tl-commit4.sh` using ledger `/root/projects/.superpowers/progress-settings.md`.
- Package `dev.linkinxp.termlauncher`; minSdk 34, compileSdk 37, targetSdk 36; JDK 17; Robolectric `@Config(sdk = [34])`.
- Per-task test command: `./gradlew --no-daemon testDebugUnitTest --tests '*<Class>'`; the whole suite `./gradlew --no-daemon assembleDebug testDebugUnitTest` must stay green.
- No new permission. File access only through the system document pickers (`CreateDocument`, `OpenDocument`).
- Backup file: `terminal-launcher-backup.json`, `format` `"terminal-launcher-backup"`, `version` 1. Widgets are never exported. Nova/Lawnchair import reads a ZIP containing `launcher.db` (any depth, name compared case-insensitively) or a raw SQLite file with a `favorites` table; files larger than 64 MB are refused; the cache copy is always deleted.
- Reserved words gain `settings`. Help text stays within 12 lines of at most 40 columns and still mentions every command word.
- Importing from Pixel Launcher is impossible (permission `ACCESS_LAUNCHER_DATA`); never try to read other apps' private files or use `run-as`/root.
- Real file pickers, a real Nova/Lawnchair backup, Google search handling and the settings screen feel are checked by the user on the phone; do not claim them working without that. Never take screenshots of the user's phone. Never run the signed-release flow.
- Out of scope: Pixel Launcher import, carrying widgets across, cloud backup, merging an import into the current layout, icon packs, a custom theme editor, voice/Lens in the search bar.

## Review Focus

- Search: queries containing `& = # % + ?`, emoji, newlines, leading/trailing spaces, whitespace-only, 10 000 characters; no handler installed for the intent; never a crash, never a malformed URL.
- Backup decode: null, empty, truncated JSON, wrong JSON types, wrong `format`, newer `version`, missing sections, unknown fields, a huge file; export contains no widgets; import with apps not installed here.
- Favorites mapping: sparse and more than 5 screens, a folder whose `container` points at itself or at a missing folder, malformed intents, duplicate packages across pages and folders, titles that are empty/reserved/with slashes/very long, 5 000 rows.
- Reader: a ZIP with no `launcher.db`, a ZIP bomb or oversized file, a non-SQLite file, a database without `favorites` or with missing columns, a locked/corrupt database; the cache copy deleted on every path.
- Settings: rapid taps, values at their bounds (dim 0/100, seed overflow), switching theme while ascii wallpaper is on, Back and Home closing it, rotation not losing state.

---

### Task 1: Google search bar below the clock

**Files:**
- Create: `search/SearchQuery.kt`, `ui/SearchBar.kt`
- Modify: `panel/PanelSettings.kt` (add `search`), `data/Prefs.kt` (store it), `LauncherViewModel.kt` (`panels` already exposed; `search` uses it), `ui/PagerHome.kt` (bar under the date line, above `sys`), `MainActivity.kt` (hand the query to the system)
- Test: `search/SearchQueryTest.kt`, `panel/PanelSettingsTest.kt` (update)

**Interfaces:**
- Produces: `fun cleanQuery(raw: String): String?` (trimmed, inner whitespace runs (including newlines) collapsed to one space, cut to 500 characters, null when blank); `fun searchUri(raw: String): String?` (`https://www.google.com/search?q=` plus the cleaned query URL-encoded as UTF-8 with spaces as `+`, null for a blank query); `PanelSettings(cal: Boolean = false, wx: Boolean = false, search: Boolean = true)`; `applyPanel` accepts the name `search`; `@Composable fun SearchBar(onSearch: (String) -> Unit)`.

- [ ] **Step 1: Write the failing tests.** `SearchQueryTest`: `cleanQuery("  hello   world \n")` -> `"hello world"`; `cleanQuery("")`, `("   ")`, `("\n\t")` -> null; a 10 000-character input -> exactly 500 characters; `searchUri("cats & dogs")` == `https://www.google.com/search?q=cats+%26+dogs`; `searchUri("a=b#c%d+e?f")` == `https://www.google.com/search?q=a%3Db%23c%25d%2Be%3Ff`; `searchUri("São Paulo")` == `https://www.google.com/search?q=S%C3%A3o+Paulo`; emoji is percent-encoded (`🎉` -> `%F0%9F%8E%89`); `searchUri("  ")` null. `PanelSettingsTest`: default is `PanelSettings(cal = false, wx = false, search = true)`; `applyPanel(d, "search", false)` turns it off; the unknown-panel message becomes `panel: x: no such panel (cal wx search)`; the usage message becomes `usage: panel <cal|wx|search> on|off`; `describe` has a `search` line.
- [ ] **Step 2: Run** the two classes. Expected: FAIL.
- [ ] **Step 3: Implement** the pure functions and the settings change (Prefs key `panel_search`, missing -> true). `SearchBar`: a row `g> ` in the accent color plus a single-line `BasicTextField` (hint `search google` in the dim color, IME action Search, `[go]` on the right); submitting passes the raw text to `onSearch` and clears the field; blank input does nothing. `PagerHome` shows it under the date line when `ui.panels.search` is true. `MainActivity.onSearch`: `Intent(Intent.ACTION_VIEW, Uri.parse(searchUri(text)))` with `FLAG_ACTIVITY_NEW_TASK` in a try/catch for `ActivityNotFoundException` (message `search: no browser found` through `vm.showMessage`). Update the help text so `panel` shows `<cal|wx|search>`.
- [ ] **Step 4: Run** the two classes, then `assembleDebug testDebugUnitTest`. Expected: PASS, BUILD SUCCESSFUL.
- [ ] **Step 5: Commit** `terminal-launcher: google search bar`.

### Task 2: Settings helpers (pure)

**Files:**
- Modify: `theme/ThemeRegistry.kt` (`describeThemes`), `fx/FxSettings.kt` (`withScanlines`), `prompt/CommandParser.kt` (`settings`, `fx scanlines auto`), `layout/HomeLayout.kt` (reserved word), `LauncherViewModel.kt` (temporary branches, help/completion)
- Test: `theme/ThemeRegistryTest.kt`, `fx/FxSettingsTest.kt`, `prompt/CommandParserTest.kt`, `layout/HomeLayoutTest.kt` (extend)

**Interfaces:**
- Produces: `fun describeThemes(current: String): String` (one line per theme, `* name` for the current one and `  name` for the others, in registry order); `FxSettings.withScanlines(value: Boolean?): FxSettings`; `Command.Fx(val name: String?, val on: Boolean?, val auto: Boolean = false)`; `Command.Settings` (object); bare `theme` parses to `Command.Theme(null)`.

- [ ] **Step 1: Write the failing tests.** `describeThemes("matrix")` has 9 lines, exactly one starting with `* matrix`, others with two spaces; `FxSettings().withScanlines(true).scanlines == true`, `.withScanlines(null).scanlines == null`; parser: `"fx scanlines auto"` -> `Fx("scanlines", null, auto = true)`, `"fx glitch auto"` -> `Fx("glitch", null, auto = true)` (the ViewModel rejects `auto` for anything but scanlines), `"settings"` -> `Command.Settings`, `"settings x"` -> `Query`, `"theme"` -> `Command.Theme(null)`; `mkdir("settings")` fails.
- [ ] **Step 2: Run** the four classes. Expected: FAIL.
- [ ] **Step 3: Implement.** ViewModel: `Command.Theme(null)` prints `describeThemes(cur.theme.name)`; `Command.Fx` with `auto` on scanlines saves `withScanlines(null)` and on any other name returns `fx: <name>: auto is only for scanlines`; `Command.Settings` -> temporary `null` branch (Task 3 opens the screen); add `settings` to `COMMANDS`; re-flow `HELP` to include `settings` within 12 lines.
- [ ] **Step 4: Run** the tests and the whole suite. Expected: PASS.
- [ ] **Step 5: Commit** `terminal-launcher: settings helpers`.

### Task 3: Settings screen

**Files:**
- Create: `ui/SettingsScreen.kt`
- Modify: `LauncherViewModel.kt` (setters), `MainActivity.kt` (overlay state, command and edit-mode entry), `ui/EditMode.kt` (`[settings]` link)
- Test: `screens/SettingsScreenTest.kt` (Robolectric)

**Interfaces:**
- Consumes: Task 2, `PanelSettings.search` (Task 1), existing pure settings functions.
- Produces: ViewModel `fun setTheme(name: String)`, `fun setFx(name: String, on: Boolean)`, `fun setScanlines(value: Boolean?)`, `fun setWall(w: WallSettings)`, `fun setPanel(name: String, on: Boolean)`, `fun setWxUnits(fahrenheit: Boolean)`, `fun setWxCity(name: String)` (same behavior as `wx city`), `fun setDrawerSort(s: DrawerSort)` (existing `setSort`), `fun setIcons(m: IconMode)` (existing `setIconMode`); `@Composable fun SettingsScreen(ui: LauncherUi, vm: LauncherViewModel, onExport: () -> Unit, onImport: () -> Unit, onImportLauncher: () -> Unit, onClose: () -> Unit)`.

- [ ] **Step 1: Write the failing test** `SettingsScreenTest` (Robolectric, compose rule, the real ViewModel): rendering `SettingsScreen` shows every section title (`theme`, `wallpaper`, `effects`, `panels`, `drawer`, `backup`, `about`); clicking the `matrix` row calls `setTheme("matrix")` (assert `vm.ui.value.theme.name == "matrix"` after idling); clicking the `dim` `[+]` twice from 55 gives 65 and `[-]` never goes below 0 or `[+]` above 100 (set via `vm.setWall` first); the `scanlines` row cycles `auto (theme)` -> `on` -> `off` -> `auto (theme)`; toggling `flicker` flips `fx.flicker`.
- [ ] **Step 2: Run** `--tests '*SettingsScreenTest'`. Expected: FAIL.
- [ ] **Step 3: Implement** the setters (each uses the corresponding Prefs setter/pure function, no new storage) and the screen per the spec: full-screen overlay with its own `BackHandler`, sections as `TermPanel`s, theme rows with four color chips, wallpaper/effects/panels/drawer controls, a `backup` section with the three buttons wired to the callbacks (no-ops until Tasks 5 and 7), an `about` section (version `0.5.0`, the way back to the old launcher). `MainActivity`: `settingsOpen` state opened by `Command.Settings` (via a ViewModel event flow like `permissionRequests`) and by `[settings]` in edit mode; closed by the Home press signal and Back. Bump `LAUNCHER_VERSION` and `versionName` to `0.5.0`.
- [ ] **Step 4: Run** `--tests '*SettingsScreenTest'`, then `assembleDebug testDebugUnitTest`. Expected: PASS, BUILD SUCCESSFUL.
- [ ] **Step 5: Commit** `terminal-launcher: settings screen`.

### Task 4: Own backup format (pure)

**Files:**
- Create: `backup/LauncherBackup.kt`
- Modify: `layout/HomeLayout.kt` (`withoutWidgets`)
- Test: `backup/LauncherBackupTest.kt`, `layout/HomeLayoutTest.kt` (extend)

**Interfaces:**
- Produces: `@Serializable data class LauncherBackup(val format: String = "terminal-launcher-backup", val version: Int = 1, val layout: HomeLayout = HomeLayout.default(), val hidden: Set<String> = emptySet(), val theme: String = "classy-black", val fx: FxBackup = FxBackup(), val wall: WallBackup = WallBackup(), val panels: PanelBackup = PanelBackup(), val wx: WxBackup = WxBackup(), val drawerSort: String = "AZ", val iconMode: String = "TEXT")` with small `@Serializable` data classes for the sections (all fields defaulted); `fun encodeBackup(b: LauncherBackup): String`; `fun decodeBackup(json: String?): Result<LauncherBackup>`; `fun HomeLayout.withoutWidgets(): HomeLayout`; `fun describeBackup(b: LauncherBackup, installed: Set<String>): List<String>`.

- [ ] **Step 1: Write the failing tests.** Round trip of a populated backup (pages, dock, folders, hidden, theme, fx incl. `scanlines = null` and `= true`, wall, panels, wx with city/lat/lon/fahrenheit, sort, icons); `encodeBackup` of a layout containing a widget has no `"widget"` in the output and `withoutWidgets()` removes widgets only; `decodeBackup(null)`, `("")`, `("{bad")`, `("[]")`, a wrong `format`, and `version = 2` each fail with messages `import: not a launcher backup` / `import: backup is from a newer version`; missing sections decode to defaults; unknown extra fields are ignored; a wrong-typed field (`"theme": 5`) fails cleanly; `describeBackup` lines include the number of pages, dock items, folders, apps not installed here (given `installed`), and the theme name.
- [ ] **Step 2: Run** `--tests '*LauncherBackupTest' --tests '*HomeLayoutTest'`. Expected: FAIL.
- [ ] **Step 3: Implement** with kotlinx-serialization (`ignoreUnknownKeys = true`, `encodeDefaults = true`, `explicitNulls = true` so `scanlines = null` round-trips); decode catches every exception and returns the failure messages above; the export applies `withoutWidgets()`.
- [ ] **Step 4: Run** the tests and the whole suite. Expected: PASS.
- [ ] **Step 5: Commit** `terminal-launcher: backup format`.

### Task 5: Backup export and import (Android)

**Files:**
- Create: `backup/BackupRepository.kt`, `ui/ImportDialog.kt`
- Modify: `MainActivity.kt` (document launchers), `LauncherViewModel.kt` (import state), `ui/SettingsScreen.kt` (wire the buttons)
- Test: `backup/BackupRepositoryTest.kt` (Robolectric)

**Interfaces:**
- Consumes: Task 4, Prefs setters.
- Produces: `class BackupRepository(context, prefs)` with `suspend fun snapshot(): LauncherBackup` (reads every store) and `suspend fun apply(b: LauncherBackup)`; ViewModel `val importPreview: StateFlow<ImportPreview?>` where `data class ImportPreview(val lines: List<String>, val apply: suspend () -> Unit)`, `fun startImport(json: String?)`, `fun confirmImport()`, `fun cancelImport()`, `suspend fun exportJson(): String`.

- [ ] **Step 1: Write the failing test** `BackupRepositoryTest` (Robolectric, real `Prefs`): set a non-default theme, hidden app, fx, wall, panels, wx and a layout with a widget, call `snapshot()`; assert it equals the expected `LauncherBackup` (layout without widgets); then `apply(backup)` after changing everything and assert each store matches; `apply` of a backup whose `theme` is unknown keeps the current theme; `apply` never throws on defaults.
- [ ] **Step 2: Run** `--tests '*BackupRepositoryTest'`. Expected: FAIL.
- [ ] **Step 3: Implement** `snapshot`/`apply` (one DataStore edit per area; unknown theme/enum names fall back field by field), the ViewModel preview flow (`startImport` decodes, builds the preview from `describeBackup`, on failure shows the message through `showMessage`), `ImportDialog` with `[replace]`/`[cancel]`, and in `MainActivity` a `CreateDocument("application/json")` launcher (writes `exportJson()` to the URI, message `backup: saved`) and an `OpenDocument` launcher (reads at most 5 MB as UTF-8 and calls `startImport`; read errors -> `import: could not read that file`). Wire the settings buttons `[export layout]` and `[import layout]`.
- [ ] **Step 4: Run** the test, then `assembleDebug testDebugUnitTest`. Expected: PASS, BUILD SUCCESSFUL.
- [ ] **Step 5: Commit** `terminal-launcher: backup export and import`.

### Task 6: Launcher3 favorites mapping (pure)

**Files:**
- Create: `importer/FavoritesMapper.kt`
- Test: `importer/FavoritesMapperTest.kt`

**Interfaces:**
- Produces: `data class FavRow(val id: Int, val itemType: Int, val container: Int, val screen: Int, val cellX: Int, val cellY: Int, val title: String?, val intent: String?)`; `data class ImportedLayout(val layout: HomeLayout, val apps: Int, val folders: Int, val skippedWidgets: Int, val missingApps: Int)`; `fun packageFromIntent(intent: String?): String?`; `fun mapFavorites(rows: List<FavRow>, installed: Set<String>): ImportedLayout`.

- [ ] **Step 1: Write the failing tests.** `packageFromIntent`: `"#Intent;action=android.intent.action.MAIN;category=android.intent.category.LAUNCHER;launchFlags=0x10200000;component=com.android.chrome/com.google.android.apps.chrome.Main;end"` -> `com.android.chrome`; the `;package=com.foo;` form -> `com.foo`; `null`, `""`, `"garbage"`, `"component=;end"` -> null. `mapFavorites` (installed = a given set): two desktop screens (0 and 1) with apps in mixed cell order -> two pages, items ordered by `cellY` then `cellX`; sparse screens (2, 7, 9) -> three consecutive pages; 7 screens -> 5 pages with the items of screens 6 and 7 appended to the fifth page; hotseat (container -101) with 6 items -> dock of 5 ordered by `cellX`, the sixth placed on page 1; a folder row (itemType 2, id 10) with three child rows (container 10) -> `Item.Folder` with the three packages in cell order, and those apps not repeated on pages; a child whose package is not installed is dropped and counted; a widget row (itemType 4) is skipped and counted; unknown itemTypes skipped; a folder whose child rows point at a missing folder id are ignored; a folder with `container = id` of itself is handled without looping; the same package twice -> placed once; folder titles: empty -> `folder`, `"My Games"` -> `My-Games`, `"a/b"` -> `ab`, `"theme"` -> `theme_`, 40 characters -> 24, two folders named `Games` -> `Games` and `Games2`; empty input -> default layout; 5 000 rows complete quickly (test completes).
- [ ] **Step 2: Run** `--tests '*FavoritesMapperTest'`. Expected: FAIL.
- [ ] **Step 3: Implement** as specified in the spec (screens sorted then compacted to consecutive pages, cap 5 with overflow appended to the last page, hotseat to dock cap 5 with overflow to page 1, folder-name cleaning with the layout's reserved words and uniqueness, installed filtering, first occurrence of a package wins, counts).
- [ ] **Step 4: Run** the tests and the whole suite. Expected: PASS.
- [ ] **Step 5: Commit** `terminal-launcher: launcher favorites mapping`.

### Task 7: Nova/Lawnchair backup import (Android)

**Files:**
- Create: `importer/FavoritesReader.kt`
- Modify: `MainActivity.kt` (document launcher), `LauncherViewModel.kt` (reuse the import preview for layouts), `ui/ImportDialog.kt`, `ui/SettingsScreen.kt` (wire `[import nova/lawnchair]`)
- Test: `importer/FavoritesReaderTest.kt` (Robolectric)

**Interfaces:**
- Consumes: Task 6, the preview flow (Task 5).
- Produces: `object FavoritesReader { fun read(input: java.io.InputStream, cacheDir: java.io.File): Result<List<FavRow>> }` (the caller owns the stream; the temp copy lives in `cacheDir` and is deleted on every path); failure messages `import: no launcher layout found in this file` and `import: could not read that file`.

- [ ] **Step 1: Write the failing test** `FavoritesReaderTest` (Robolectric): build a SQLite file in the test with a `favorites` table (the eight columns plus an extra column) and a few rows, then: reading it raw returns the rows; the same bytes wrapped in a ZIP as `backup/launcher.db` and as `LAUNCHER.DB` read the same; a ZIP without `launcher.db` fails with the no-layout message; a plain text file and an empty stream fail; a database without a `favorites` table fails; a database whose `favorites` lacks the `intent` column fails; a stream larger than 64 MB is refused (use a fake stream that reports that many bytes without allocating them); in every case no file is left in `cacheDir`.
- [ ] **Step 2: Run** `--tests '*FavoritesReaderTest'`. Expected: FAIL.
- [ ] **Step 3: Implement** the reader: sniff the first bytes (`PK` zip or `SQLite format 3`), copy to a temp file with a 64 MB cap (stop and fail past it), open with `SQLiteDatabase.openDatabase(path, null, OPEN_READONLY)`, query `favorites` with the eight columns, close and delete in `finally`. Then the flow: an `OpenDocument` launcher for `*/*` -> read -> `mapFavorites(rows, installedPackages)` -> a preview (`pages`, `dock items`, `folders`, `apps found`, `apps not installed here`, `widgets skipped`) -> `[replace]` swaps only the layout (pages, dock, folders; keeps hidden apps and all other settings) through `prefs.editLayout`.
- [ ] **Step 4: Run** the test, then `assembleDebug testDebugUnitTest`. Expected: PASS, BUILD SUCCESSFUL.
- [ ] **Step 5: Commit** `terminal-launcher: import from nova/lawnchair backup`.

### Task 8: Screenshots, README and final verification

**Files:**
- Modify: `screens/ScreenshotsTest.kt`, `README.md`

- [ ] **Step 1:** Extend `ScreenshotsTest`: home with the search bar in two themes; the settings screen in two themes (top, and scrolled to wallpaper/effects if it scrolls); the import preview dialog content as a panel with sample lines. Run `./gradlew --no-daemon recordRoborazziDebug`. Expected: BUILD SUCCESSFUL and a PNG per case.
- [ ] **Step 2:** Look at the PNGs; fix visible problems (clipped rows, overlapping controls, unreadable chips) and re-render until clean.
- [ ] **Step 3:** Update `README.md` (search bar and `panel search`, `settings`, bare `theme`, `fx scanlines auto`, backup export/import, Nova/Lawnchair import with its limits and the Pixel Launcher limitation); bump nothing else (version already 0.5.0).
- [ ] **Step 4:** Run `./gradlew --no-daemon clean assembleDebug testDebugUnitTest`. Expected: BUILD SUCCESSFUL, 0 failures.
- [ ] **Step 5:** Commit `terminal-launcher: settings and import round screenshots and docs, v0.5.0`.
