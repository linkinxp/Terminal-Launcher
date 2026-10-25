# Terminal Launcher Icon Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Themed monochrome app icons everywhere (home grid, dock, quick tiles, drawer grid, folders, menus) behind a new `ICONS` home style, keeping the terminal look.

**Architecture:** A small pure icon core (keys, tint matrix, bounded cache) plus an Android renderer that draws one themed tile bitmap per (app, theme, size) off the main thread and caches it. UI pieces (`IconCell`, icon grid page, icon dock/quick, grid drawer) consume `rememberThemedIcon`; everything is gated by `HomeStyle.ICONS`, so existing styles are untouched.

**Tech Stack:** Kotlin 2.4.21, Jetpack Compose (BOM 2026.09.00, `LazyVerticalGrid`), DataStore, Robolectric/Roborazzi, JUnit 4.

**Spec:** `docs/superpowers/specs/2026-10-24-terminal-launcher-icons-design.md`

## Global Constraints

- Work on android-dev (`ssh android-dev`), `/root/projects/terminal-launcher`, branch `terminal-launcher`; edit under `/tmp/tl`, push with `/tmp/tl-run.sh <gradle args>`, commit on the box after each task **only after a green run**.
- Package `dev.linkinxp.termlauncher`; minSdk 34; Robolectric `@Config(sdk = [34])`; whole suite `assembleDebug testDebugUnitTest` stays green.
- Robolectric tests share one DataStore: reset stores like `SettingsScreenTest.show()`; poll with the `until`/`settle` helpers; do not drive `vm.onPromptSubmit` from a test class that then runs store resets (it hung the suite once): test commands as pure functions.
- Defaults unchanged: existing styles and layouts behave exactly as before; `ICONS` only when chosen (`layout icons` or the settings row).
- Icons: tile corner radius 28%, fill `dim` at 18% alpha, 1 px border `dim` at 40%, glyph in the 66% safe zone tinted `foreground` (dock and selected: `accent`); cache LRU of 256; rendering only on `Dispatchers.Default`; placeholder tile keeps the size.
- Performance: do not read the whole `LauncherUi` in `MainActivity`'s root scope; no per-frame icon work; drawer harness numbers from the spec must hold (janky <= 5%, p99 < 25 ms, swipe to drawer on screen < 60 ms).
- Never screenshot the phone; the phone harness (`/tmp/drawer-perf.sh`-style, timestamps only) is allowed while Terminal is default only with the user's say-so, restore the Pixel Launcher afterwards.
- Out of scope: icon packs, per-app overrides, animations, badges, prompt bar/panels changes, a list/grid drawer toggle.

## Review Focus

- A phone with an app whose icon is not adaptive, has no monochrome layer, or whose `getApplicationIcon` throws: letter tile, no crash.
- A theme change while icons are cached: tiles recolor (cache key includes theme colors), no stale colors.
- 0 apps, 1 app, an odd number of apps (last grid row), 500 apps: grids render, bounded memory.
- Drawer grid: letter headers span the width, the A-Z rail still jumps to the right row, searching shows a flat grid with the best match first, Search key launches the best match.
- The keyboard opening with the drawer must not resize into a broken layout (bottom bar above the IME, grid still scrolls).
- Dock with 0..5 items and a folder pinned; quick panel with 1..8 tiles; page that is both short (anchored bottom) and long (scrolls).

---

### Task 1: Icon core (pure)

**Files:** Create `icons/IconCore.kt`; Test `icons/IconCoreTest.kt`.

**Interfaces:** Produces `fun themeKey(theme: Theme): String` (changes when background, foreground, accent or dim changes); `data class IconKey(val pkg: String, val themeKey: String, val sizePx: Int)`; `fun tintMatrix(argb: Long): FloatArray` (20-float Android ColorMatrix: output = luminance(rgb) x color, alpha kept); `class IconCache<V>(capacity: Int = 256)` with `get(key)`, `put(key, value)`, `size` (thread-safe LRU).

- [ ] **Step 1: Write the failing tests.** `themeKey` equal for equal colors and different when any one of the four colors differs (not when `name`/`glow` differ); `tintMatrix(0xFFFF0000)` maps white to pure red and black to black, keeps alpha row `[0,0,0,1,0]`, and gray 50% maps to half red; `IconCache` evicts the least recently used beyond capacity (put 3 into capacity 2 after touching the first), `get` refreshes recency, concurrent put/get from 8 threads leaves `size <= capacity`.
- [ ] **Step 2: Run** `--tests '*IconCoreTest'`. Expected: FAIL (unresolved references).
- [ ] **Step 3: Implement** the three units (LinkedHashMap access-order under a lock for the cache).
- [ ] **Step 4: Run** the test and the whole suite. Expected: PASS.
- [ ] **Step 5: Commit** `terminal-launcher: icon core`.

### Task 2: Themed icon renderer and tile

**Files:** Create `icons/IconRepository.kt`, `ui/IconTile.kt`; Modify `ui/AppRow.kt` (the `ICON` mode uses the themed tile); Test `screens/IconTileTest.kt`.

**Interfaces:** Consumes Task 1. Produces `class IconRepository(context)` with `suspend fun tile(pkg: String, label: String, theme: Theme, sizePx: Int): ImageBitmap` (never throws; cached by `IconKey`; letter tile on any failure) and `fun invalidate()`; `@Composable fun rememberThemedIcon(pkg: String, label: String, size: Dp, selected: Boolean = false): ImageBitmap?`; `@Composable fun IconTile(pkg: String, label: String, size: Dp, modifier: Modifier = Modifier, selected: Boolean = false)` (an empty tile of the final size until the bitmap arrives); one `IconRepository` per process via a `staticCompositionLocalOf`/singleton.

- [ ] **Step 1: Write the failing Robolectric tests** with fake apps: an `AdaptiveIconDrawable(bg, fg, monochrome)` registered through `ShadowPackageManager` whose monochrome layer is solid white renders a tile whose center pixel is the theme foreground and whose corner pixel is background-ish (not the original red); an app with a plain `ColorDrawable(red)` icon renders a tinted (not red) center; an unknown package renders the letter tile (nonblank, no exception); two themes give two different bitmaps for the same app; the same key twice returns the same cached instance.
- [ ] **Step 2: Run** `--tests '*IconTileTest'`. Expected: FAIL.
- [ ] **Step 3: Implement.** Draw into an `android.graphics.Bitmap` canvas: rounded-rect fill + border, then the glyph: monochrome layer drawn into the 66% safe zone with a `PorterDuffColorFilter(foreground, SRC_IN)`; otherwise the icon drawn with `ColorMatrixColorFilter(tintMatrix(foreground))`. `rememberThemedIcon` is a `produceState` keyed on pkg, theme key and size that calls the repository on `Dispatchers.Default`.
- [ ] **Step 4: Run** the test and the whole suite. Expected: PASS.
- [ ] **Step 5: Commit** `terminal-launcher: themed icon tiles`.

### Task 3: `ICONS` home style, `layout icons`, settings, backup

**Files:** Modify `layout/HomeStyle.kt` (`ICONS`, `layoutPlan("icons")`), `ui/SettingsScreen.kt` (row already cycles all entries), `backup` (string already generic), `LauncherViewModel.kt` (HELP line); Test `layout/HomeStyleTest.kt`, `screens/SettingsScreenTest.kt`, `prompt/HelpTextTest.kt`.

**Interfaces:** Produces `HomeStyle.ICONS`; `layoutPlan("icons") == LayoutPlan(HomeStyle.ICONS, Restructure.APPS_FIRST, "layout: icons (icon grid, glance page)")`; usage text `usage: layout classic|bottom|grid|icons|apps-first`.

- [ ] **Step 1: Tests:** the plan above, the new usage string, `HomeStyle.fromName("ICONS")`, the settings row now cycles classic, bottom, grid, icons and wraps, backup round trip with `ICONS`, help still fits (12 lines, 40 columns, mentions `layout`).
- [ ] **Step 2: Run** the three classes. Expected: FAIL.
- [ ] **Step 3: Implement** (help line: `layout classic|bottom|grid|icons|apps-first` fits 40 columns only as `layout classic|bottom|grid|icons|apps`: keep the usage message full, shorten the help line).
- [ ] **Step 4: Run** tests and suite. Expected: PASS. **Step 5: Commit** `terminal-launcher: icons home style`.

### Task 4: Icon grid home page, dock and quick tiles

**Files:** Create `ui/IconCell.kt`; Modify `ui/PagerHome.kt` (ICONS branch: 4-column grid, folder tile with 2x2 minis, bottom anchored; glance page unchanged), `ui/Dock.kt` (icon dock when ICONS), `ui/QuickPanel.kt` (icon tiles when ICONS); Test `screens/IconHomeTest.kt`, update `QuickPlacementTest` only if needed.

**Interfaces:** Consumes Task 2. Produces `@Composable fun IconCell(label: String, onClick, onLongClick, modifier, icon: @Composable () -> Unit)`; `fun iconRows(items: List<Item>, columns: Int = 4): List<List<Item>>` (apps and folders chunked; panels/widgets break rows).

- [ ] **Step 1: Tests:** pure `iconRows` (empty, 1, 4, 5, 9 items; a widget between apps splits runs); Robolectric: `ICONS` page shows one cell per app with its label and tapping a cell calls `onItemTap`, long-press calls `onItemLongPress`; folder cell shows its name and opens via tap; dock shows icon cells with labels for 0..5 items; quick panel shows icon cells (tag `quick-<pkg>` kept); `CLASSIC`/`GRID` pages and the text dock are unchanged (existing tests still pass); an odd count leaves empty trailing cells without crashing; 300 apps scroll.
- [ ] **Step 2: Run** the new test. Expected: FAIL.
- [ ] **Step 3: Implement.** Reuse the page structure from `PagerHome` (`HomeStyle` branches); cells are 56 dp tiles + 11 sp one-line label; mini folder tile draws four 22 dp tiles in a 2x2.
- [ ] **Step 4: Run** tests and suite. Expected: PASS. **Step 5: Commit** `terminal-launcher: icon home, dock and quick tiles`.

### Task 5: Icon drawer (grid + bottom bar)

**Files:** Create `ui/IconDrawer.kt`; Modify `ui/AppDrawer.kt` (choose list vs icon drawer by `ui.homeStyle`, keep the effect/keyboard/back handling shared), `ui/AlphaIndex.kt` only if the rail needs a row mapping; Test `screens/IconDrawerTest.kt`.

**Interfaces:** Produces `fun gridItems(d: DrawerState, folders: Int, filtering: Boolean): List<GridEntry>` (pure: headers span the full width, apps are cells; returns the row index of each letter for the rail) with `sealed interface GridEntry { Header(text), Folder(name), Cell(app) }`.

- [ ] **Step 1: Tests:** pure `gridItems` (recent block then lettered sections; filtering gives a flat best-first list with no headers; header indexes for the rail), Robolectric: grid shows headers and cells for sample apps, the rail letter scrolls to its header, typing in the bottom search shows a flat result grid, the Search key launches the top match, `[cfg]` and `sort` are in the bottom bar and work, Back closes, the keyboard still requests on open (`drawerKeyboard` true) and not when off.
- [ ] **Step 2: Run** the test. Expected: FAIL.
- [ ] **Step 3: Implement** with `LazyVerticalGrid(GridCells.Fixed(4))`, headers via `span = { GridItemSpan(maxLineSpan) }`, keys on every item, the bottom bar (`sort`, `[cfg]`, search field) pinned with `imePadding`, swipe-down on the bar closes; the list drawer stays for other styles.
- [ ] **Step 4: Run** tests and suite. Expected: PASS. **Step 5: Commit** `terminal-launcher: icon drawer`.

### Task 6: Icons in menus and edit mode, warm-up

**Files:** Modify `ui/EditMode.kt` (add-app rows), `ui/AppMenu.kt` (header), `ui/FolderPanel.kt` (rows use `AppRow` already), `MainActivity.kt` or the ViewModel (warm the cache for dock, quick and page one after the app list or theme changes, and the first 24 drawer cells when idle); Test `screens/IconWarmupTest.kt`.

- [ ] **Step 1: Tests:** changing the theme changes every cached key (a second theme renders different bitmaps for the dock apps); warming fills the cache for the dock and quick apps without touching the main thread (assert via the repository call count on a test dispatcher); edit-mode add list and the app menu header show a themed icon (node with a content description `icon-<pkg>`).
- [ ] **Step 2: Run.** Expected: FAIL. **Step 3: Implement.** **Step 4: Run** tests and suite. Expected: PASS. **Step 5: Commit** `terminal-launcher: icons in menus, cache warm-up`.

### Task 7: Screenshots, README, version, measurement, final verification

**Files:** `screens/ScreenshotsTest.kt`, `README.md`, `app/build.gradle.kts` + `LauncherViewModel.LAUNCHER_VERSION` (`0.7.0`).

- [ ] **Step 1:** Record screenshots (`recordRoborazziDebug`): icon home, icon drawer, icon dock/quick in `tokyo-night`, `matrix` and `neon-city`; look at them and fix visible problems.
- [ ] **Step 2:** README (icon style, `layout icons`, themed fallback rules) and version `0.7.0`.
- [ ] **Step 3:** `clean assembleDebug testDebugUnitTest`; repeat the Robolectric-heavy classes twice.
- [ ] **Step 4:** Commit; final whole-branch review by a fresh opus reviewer with the Review Focus above; one fix pass with failing tests first.
- [ ] **Step 5:** Install with adb; with the user's say-so run the phone harness (Terminal default, timestamps only) and compare with the targets; restore the Pixel Launcher.
