# Terminal Launcher Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a terminal-styled, themeable Android home launcher (pages, folders, prompt, app drawer) for the user's Pixel 10 Pro.

**Architecture:** Single-module Kotlin/Compose app. Pure-Kotlin core (fuzzy match, command parser, themes, layout model, drawer grouping) is unit tested on the JVM; thin Android layers (PackageManager repo, DataStore prefs, Compose UI) sit on top and are verified by build plus a manual on-device checklist.

**Tech Stack:** Kotlin 2.4.21, AGP 9.4.1, Gradle 9.8.2 (wrapper), Compose BOM 2026.09.00, material3, activity-compose 1.13.0, DataStore Preferences, kotlinx-serialization-json, JUnit 4.

**Spec:** `docs/superpowers/specs/2026-10-23-terminal-launcher-design.md`

## Global Constraints

- Work on android-dev (`ssh android-dev`), project dir `/root/projects/terminal-launcher`, inside the existing `/root/projects` git repo; commit after each task.
- Package/namespace/applicationId `dev.linkinxp.termlauncher`; minSdk 31, compileSdk/targetSdk 36; JDK 17.
- Build with `./gradlew --no-daemon assembleDebug`; tests with `./gradlew --no-daemon testDebugUnitTest` (cold build ~90 s, keep RAM free for neighbours).
- Copy `gradlew`, `gradlew.bat`, `gradle/wrapper/*`, `gradle.properties` and `.gitignore` from `/root/projects/hello-widget`; settings/root build files follow hello-widget's (plugin versions pinned in root `build.gradle.kts`).
- HOME launcher: intent-filter `MAIN` + `HOME` + `DEFAULT`; installed alongside the stock launcher, never replaces it.
- No network, no special permissions in v1; only a `<queries>` launcher-intent entry and `REQUEST_DELETE_PACKAGES`.
- 5 themes exactly: `classy-black`, `matrix`, `amber`, `solarized-dark`, `dracula`. Border styles: single, double, rounded, ascii. Max 5 pages. Prompt string `user@phone:~$`.
- Prompt commands exactly: `theme <name>`, `pin <app>`, `hide <app>`, `mkdir <folder>`, `mv <app> <folder>`, `rmdir <folder>`, `ls`, `ls -a`, `help`; any other text is an app query.
- Visual checks are done by the user on the phone (no emulator); install over adb Wi-Fi with `/opt/android-sdk/platform-tools/adb`. Never run the signed-release flow.
- Out of v1: weather, calendar, media, notifications, toggles, Garmin panel, gestures, widget hosting, icon packs.

## Review Focus

- Query is empty, whitespace-only, mixed case or has trailing spaces: no crash, empty or all-app list, case-insensitive match.
- Two apps with identical labels, or an empty/non-Latin label: both stay listed and launchable, stable ordering.
- Folder or page name that collides with a command word (`ls`, `theme`) or contains spaces/slashes: rejected with a message, layout unchanged.
- App uninstalled, disabled or hidden while pinned, in a folder, or on a page: pruned on next refresh, no stale tile, no crash on tap.
- Stored layout/prefs missing, corrupt or from a newer version: reset to defaults, launcher still opens.

---

### Task 1: Project scaffold that installs as a launcher

**Files:**
- Create: `terminal-launcher/{settings.gradle.kts,build.gradle.kts,gradle.properties,.gitignore,gradlew,gradlew.bat,gradle/wrapper/*}` (copied/adapted from hello-widget)
- Create: `terminal-launcher/app/build.gradle.kts`, `app/src/main/AndroidManifest.xml`, `app/src/main/kotlin/dev/linkinxp/termlauncher/MainActivity.kt`

**Interfaces:**
- Produces: buildable app with `MainActivity` (HOME launcher, `singleTask`, `exported=true`) showing a Compose `Text("user@phone:~$")`; test source set `app/src/test/kotlin/...` with JUnit 4 available.

- [ ] **Step 1:** Create the directory and copy the wrapper/props files from hello-widget; `rootProject.name = "terminal-launcher"`; root `build.gradle.kts` also applies `org.jetbrains.kotlin.plugin.serialization` version `2.4.21` (`apply false`).
- [ ] **Step 2:** `app/build.gradle.kts`: plugins application + compose + serialization; android block per Global Constraints with `buildFeatures { compose = true }`; dependencies: compose BOM `2026.09.00`, `material3`, `ui-tooling-preview`, `activity-compose:1.13.0`, `lifecycle-viewmodel-compose`, `androidx.datastore:datastore-preferences`, `kotlinx-serialization-json`, `testImplementation("junit:junit:4.13.2")`. Pick current stable versions for the unpinned ones.
- [ ] **Step 3:** Manifest: `<queries>` with `MAIN`/`LAUNCHER` intent, `REQUEST_DELETE_PACKAGES`, `MainActivity` with intent-filter `MAIN`+`HOME`+`DEFAULT`, `launchMode="singleTask"`, `stateNotNeeded`, label "Terminal".
- [ ] **Step 4:** Run `ssh android-dev 'cd /root/projects/terminal-launcher && ./gradlew --no-daemon assembleDebug testDebugUnitTest'`. Expected: BUILD SUCCESSFUL, APK at `app/build/outputs/apk/debug/app-debug.apk`.
- [ ] **Step 5:** `git add terminal-launcher && git commit -m "terminal-launcher: project scaffold"`.

### Task 2: Themes

**Files:**
- Create: `app/src/main/kotlin/dev/linkinxp/termlauncher/theme/Theme.kt`, `theme/ThemeRegistry.kt`
- Test: `app/src/test/kotlin/dev/linkinxp/termlauncher/theme/ThemeRegistryTest.kt`

**Interfaces:**
- Produces: `enum class BorderStyle { SINGLE, DOUBLE, ROUNDED, ASCII }`; `data class Theme(val name: String, val background: Long, val foreground: Long, val accent: Long, val dim: Long, val border: BorderStyle, val glow: Boolean)` (colors are ARGB `Long` so core stays Android-free); `object ThemeRegistry { val all: List<Theme>; fun byName(name: String): Theme?; val default: Theme }`.

- [ ] **Step 1:** Write failing tests: `all` has exactly the 5 spec names in order; `byName("MATRIX")` returns matrix (case-insensitive); `byName("nope")` is null; `default.name == "classy-black"` and its `background == 0xFF000000`; names are unique.
- [ ] **Step 2:** Run `./gradlew --no-daemon testDebugUnitTest --tests '*ThemeRegistryTest'`. Expected: FAIL (unresolved reference).
- [ ] **Step 3:** Implement the types and registry. classy-black: black bg, white fg, blue accent, `glow = true`; matrix green; amber CRT; solarized-dark; dracula/synthwave. Mix border styles so every style is used at least once.
- [ ] **Step 4:** Re-run the tests. Expected: PASS.
- [ ] **Step 5:** Commit `terminal-launcher: themes`.

### Task 3: Fuzzy matcher and command parser

**Files:**
- Create: `app/src/main/kotlin/dev/linkinxp/termlauncher/apps/FuzzyMatcher.kt`, `prompt/CommandParser.kt`
- Test: `apps/FuzzyMatcherTest.kt`, `prompt/CommandParserTest.kt` (same package paths under `src/test`)

**Interfaces:**
- Produces: `fun fuzzyRank(query: String, labels: List<String>): List<Int>` (indices of matching labels, best first; empty/blank query returns all indices in original order; ties keep original order); `sealed interface Command { data class Theme(val name: String); data class Pin(val app: String); data class Hide(val app: String); data class Mkdir(val folder: String); data class Mv(val app: String, val folder: String); data class Rmdir(val folder: String); data class Ls(val hidden: Boolean); data object Help; data class Query(val text: String) }`; `fun parseCommand(input: String): Command` (trims input, command word is case-insensitive, args keep case; `mv` takes the last token as folder and the rest as app; a command word with no args, e.g. `theme`, parses as `Query`).

- [ ] **Step 1:** Tests for the matcher: `fuzzyRank("chr", ["Camera","Chrome","Files"]) == [1]`; ranking prefix > word-start > subsequence (`"mt"` matches `"Material Tool"` word-start before `"Smart Tube"` subsequence); case-insensitive; `"  "` returns all indices; duplicates (`["A","A"]`) both returned in order; non-Latin labels match on exact substring/prefix without crashing; no match returns empty.
- [ ] **Step 2:** Tests for the parser: `"theme matrix"`, `"  THEME  Matrix "` -> `Theme("Matrix")`; `"mv my app games"` -> `Mv("my app","games")`; `"ls -a"` -> `Ls(true)`, `"ls"` -> `Ls(false)`; `"help"`; `"theme"` -> `Query("theme")`; `"chrome"` -> `Query("chrome")`; `""` -> `Query("")`.
- [ ] **Step 3:** Run both test classes. Expected: FAIL.
- [ ] **Step 4:** Implement both as pure Kotlin. Score tiers: prefix 0, word-start 1, subsequence 2; stable sort.
- [ ] **Step 5:** Re-run. Expected: PASS. Commit `terminal-launcher: fuzzy matcher and command parser`.

### Task 4: Home layout model

**Files:**
- Create: `app/src/main/kotlin/dev/linkinxp/termlauncher/layout/HomeLayout.kt`
- Test: `layout/HomeLayoutTest.kt`

**Interfaces:**
- Produces (all `@Serializable`, immutable, operations return a new `HomeLayout`):
  `sealed interface Item { data class App(val pkg: String) ; data class Folder(val name: String, val apps: List<String>) }`; `data class Page(val items: List<Item>)`; `data class HomeLayout(val dock: List<Item>, val pages: List<Page>)` with `companion fun default(): HomeLayout` (empty dock, one empty page);
  `fun pin(pkg: String): HomeLayout` (to dock, max 5, no duplicates); `fun unpin(pkg)`; `fun addPage(): HomeLayout` (max 5 pages, else unchanged); `fun removePage(index: Int)` (never removes the last page); `fun movePage(from: Int, to: Int)`; `fun addToPage(page: Int, item: Item)`; `fun mkdir(name: String): Result<HomeLayout>`; `fun mv(pkg: String, folder: String): Result<HomeLayout>` (removes the app from any other folder first); `fun rmdir(name: String): HomeLayout` (apps return to page 0 as `App` items); `fun prune(installed: Set<String>): HomeLayout`; `fun encode(): String` and `companion fun decode(json: String?): HomeLayout` (null/corrupt/unknown version -> `default()`).
- Folder name rules (`mkdir` returns failure): non-blank, no spaces or `/`, max 24 chars, not a command word (`ls theme pin hide mkdir mv rmdir help`), case-insensitive unique.

- [ ] **Step 1:** Failing tests: pin 6th app unchanged; pin twice no duplicate; `mkdir("ls")`, `mkdir("my games")`, `mkdir("a/b")`, `mkdir("")`, duplicate `"Games"`/`"games"` all fail; `mv` moves an app between folders leaving it in exactly one; `rmdir` returns apps to page 0; `prune` removes uninstalled packages from dock, pages and folders but keeps empty folders; `addPage` stops at 5; `removePage` keeps at least one page; `decode(null)`, `decode("{bad")` and `decode("{\"v\":99}")` equal `default()`; `decode(encode())` round-trips a populated layout.
- [ ] **Step 2:** Run `--tests '*HomeLayoutTest'`. Expected: FAIL.
- [ ] **Step 3:** Implement with kotlinx-serialization-json (`encodeDefaults = true`, `ignoreUnknownKeys = true`, wrap in a versioned envelope `{"v":1,"layout":...}`; decode catches any exception).
- [ ] **Step 4:** Re-run. Expected: PASS. Commit `terminal-launcher: home layout model`.

### Task 5: Drawer grouping, sorting, stats

**Files:**
- Create: `app/src/main/kotlin/dev/linkinxp/termlauncher/apps/AppEntry.kt`, `apps/DrawerModel.kt`
- Test: `apps/DrawerModelTest.kt`

**Interfaces:**
- Consumes: `fuzzyRank` (Task 3).
- Produces: `data class AppEntry(val pkg: String, val label: String, val installTime: Long)`; `data class AppStat(val launches: Int, val lastUsed: Long)`; `enum class DrawerSort { AZ, RECENTLY_INSTALLED, MOST_USED }`; `data class DrawerSection(val letter: Char, val apps: List<AppEntry>)`; `fun buildDrawer(apps: List<AppEntry>, hidden: Set<String>, stats: Map<String, AppStat>, sort: DrawerSort, filter: String, showHidden: Boolean): DrawerState` where `data class DrawerState(val recent: List<AppEntry>, val sections: List<DrawerSection>)`. `recent` = up to 4 most recently used (by `lastUsed`, only when `filter` is blank); sections group by uppercase first letter of label, non-letters under `#`, sections sorted with `#` last, apps within sorted per `sort` (AZ case-insensitive; ties by pkg).

- [ ] **Step 1:** Failing tests: hidden apps excluded unless `showHidden`; `showHidden` returns only hidden apps (that is `ls -a`); AZ ordering is case-insensitive and groups `#` last; MOST_USED orders by launches desc; `recent` empty when filter non-blank; non-blank filter applies `fuzzyRank`; duplicate labels both kept with stable order; empty label goes under `#`.
- [ ] **Step 2:** Run `--tests '*DrawerModelTest'`. Expected: FAIL.
- [ ] **Step 3:** Implement as pure Kotlin (no Android types).
- [ ] **Step 4:** Re-run. Expected: PASS. Commit `terminal-launcher: drawer model`.

### Task 6: Android data layer

**Files:**
- Create: `app/src/main/kotlin/dev/linkinxp/termlauncher/data/Prefs.kt`, `apps/AppRepository.kt`, `LauncherViewModel.kt`

**Interfaces:**
- Consumes: `HomeLayout` (Task 4), `ThemeRegistry` (Task 2), `AppEntry`/`AppStat`/`DrawerSort` (Task 5), `Command`/`parseCommand` (Task 3).
- Produces: `class Prefs(context)` exposing `Flow`s and setters for `themeName: String`, `layoutJson: String?`, `hidden: Set<String>`, `drawerSort: DrawerSort`, `iconMode: IconMode` (`enum IconMode { TEXT, GLYPH, ICON }`), `stats: Map<String, AppStat>` (stored as one JSON string) plus `suspend fun recordLaunch(pkg: String)`; `class AppRepository(context)` with `val apps: Flow<List<AppEntry>>` (launchable activities via `LauncherApps`/`PackageManager`, excludes this app, refreshed by `PACKAGE_ADDED/REMOVED/CHANGED` receiver, user label, install time) and `fun launch(pkg: String): Boolean` (false when no launch intent) and `fun uninstall(pkg: String)` and `fun appInfo(pkg: String)`; `class LauncherViewModel(app)` exposing `val ui: StateFlow<LauncherUi>` (`theme`, `layout` pruned against installed apps, `dock`, `drawer: DrawerState`, `prompt: PromptState` with `suggestions: List<AppEntry>` and `message: String?`) and `fun onPromptSubmit(text: String)`, `onPromptChange(text)`, `setFilter`, `launch(pkg)`, plus a method per layout operation (`pin`, `hide`, `mkdir`, `mv`, `rmdir`, `addPage`, `removePage`, `movePage`).

- [ ] **Step 1:** Implement `Prefs` (DataStore Preferences; any parse failure falls back to defaults: classy-black, `HomeLayout.default()`, empty hidden, AZ, TEXT).
- [ ] **Step 2:** Implement `AppRepository` and the receiver registration (register in the ViewModel lifecycle, unregister on clear).
- [ ] **Step 3:** Implement `LauncherViewModel`: prompt submit runs `parseCommand`; `Theme` unknown name -> message `theme: <name>: no such theme`; layout failures (`Result.failure`) -> message with the failure text and layout unchanged; `Query` launches the top `fuzzyRank` match or sets `no match: <text>`; failed launch -> message `exec: <label>: not found`; successful launch calls `recordLaunch`; `help` -> message listing the commands.
- [ ] **Step 4:** Run `./gradlew --no-daemon assembleDebug testDebugUnitTest`. Expected: BUILD SUCCESSFUL, all earlier tests PASS.
- [ ] **Step 5:** Commit `terminal-launcher: data layer and view model`.

### Task 7: Terminal UI primitives and themed home

**Files:**
- Create: `app/src/main/kotlin/dev/linkinxp/termlauncher/ui/{TermTheme.kt,TermPanel.kt,PixelClock.kt,Dock.kt,PromptBar.kt,PagerHome.kt}`; modify `MainActivity.kt`
- Add: a bundled monospace font in `app/src/main/res/font/` (an open-licence mono such as JetBrains Mono; record the licence file next to it)

**Interfaces:**
- Consumes: `LauncherViewModel`/`LauncherUi` (Task 6), `Theme`/`BorderStyle` (Task 2).
- Produces: `@Composable fun TermTheme(theme: Theme, content: @Composable () -> Unit)` (provides `LocalTheme`, monospace typography, edge-to-edge system bars); `@Composable fun TermPanel(title: String, modifier: Modifier, trailing: String? = null, content: @Composable ColumnScope.() -> Unit)` drawing the theme's border with the title cut into the top edge; `@Composable fun PixelClock(time: java.time.LocalTime)` (blocky digit bitmaps on Canvas, glow when `theme.glow`); `@Composable fun PagerHome(ui: LauncherUi, vm: LauncherViewModel)` = horizontal pager of pages, page 0 has clock + date line (`thu 22 oct 2026  wk 43  day 295`) + system panel (battery, time), dock row of text tabs, prompt line with suggestion list above it, page indicator `[1] 2 3`.

- [ ] **Step 1:** Implement `TermTheme`, `TermPanel` and `PixelClock` with a `@Preview` per theme (5 previews).
- [ ] **Step 2:** Implement `Dock`, `PromptBar` (IME action Go -> `onPromptSubmit`, text changes -> `onPromptChange`, suggestion tap -> `launch`), `PagerHome`; `MainActivity` hosts `TermTheme { PagerHome(...) }`; Back on home does nothing.
- [ ] **Step 3:** `./gradlew --no-daemon assembleDebug`. Expected: BUILD SUCCESSFUL.
- [ ] **Step 4:** Install on the phone (`adb install -r`), set as default home in Settings, ask the user to check the clock, panels, themes (`theme matrix` etc.), dock and prompt launch. Fix what they report.
- [ ] **Step 5:** Commit `terminal-launcher: terminal UI and themed home`.

### Task 8: App drawer and app menu

**Files:**
- Create: `app/src/main/kotlin/dev/linkinxp/termlauncher/ui/{AppDrawer.kt,AlphaIndex.kt,AppMenu.kt,AppRow.kt}`; modify `PagerHome.kt`

**Interfaces:**
- Consumes: `DrawerState`, `DrawerSort`, `IconMode` (Tasks 5-6), view model operations.
- Produces: `@Composable fun AppDrawer(ui: LauncherUi, vm: LauncherViewModel, onClose: () -> Unit)` (swipe up from home opens it, swipe down or Back closes): filter field, sort toggle, icon-mode toggle (TEXT/GLYPH/ICON), "recent" section, lettered sections, `AlphaIndex` fast-scroll rail on the right edge; `ls` and `ls -a` open it (hidden list for `-a`); `AppMenu` (long-press on any app row/dock tab): pin, add to folder (list of existing folders), hide/unhide, app info, uninstall.

- [ ] **Step 1:** Implement `AppRow` (text row, glyph row, or icon row by `IconMode`), `AlphaIndex` (drag selects a letter and scrolls to the section), `AppDrawer`, `AppMenu`.
- [ ] **Step 2:** Wire swipe-up/down in `PagerHome` and the `ls`/`ls -a` commands to open the drawer.
- [ ] **Step 3:** `./gradlew --no-daemon assembleDebug testDebugUnitTest`. Expected: BUILD SUCCESSFUL, tests PASS.
- [ ] **Step 4:** Install and ask the user to check: drawer open/close, index drag, three sorts, recent section after launching a few apps, hide then `ls -a`, icon modes, menu actions including uninstall confirmation. Fix what they report.
- [ ] **Step 5:** Commit `terminal-launcher: app drawer and app menu`.

### Task 9: Folders and page edit mode

**Files:**
- Create: `app/src/main/kotlin/dev/linkinxp/termlauncher/ui/{FolderPanel.kt,EditMode.kt}`; modify `PagerHome.kt`, `Dock.kt`, `AppDrawer.kt`

**Interfaces:**
- Consumes: `HomeLayout` operations through the view model (Task 6).
- Produces: `@Composable fun FolderPanel(folder: Item.Folder, ...)` (bordered panel titled `~/name` listing its apps; tap opens, long-press an app offers remove from folder); folders render as `~/name` rows on pages, in the dock, and as a section at the top of the drawer; `EditMode` (long-press empty page area): add page, remove page, reorder pages, add an app shortcut or folder to the page, exit on Back/Done.

- [ ] **Step 1:** Implement `FolderPanel` and folder rows in dock, pages and drawer; `mkdir`/`mv`/`rmdir` results show their messages in the prompt area.
- [ ] **Step 2:** Implement `EditMode` and the `[1] 2 3` indicator updates (indicator never exceeds 5 pages).
- [ ] **Step 3:** `./gradlew --no-daemon assembleDebug testDebugUnitTest`. Expected: BUILD SUCCESSFUL, tests PASS.
- [ ] **Step 4:** Install and ask the user to check: `mkdir games`, `mv <app> games`, folder open, `rmdir games`, rejected names (`mkdir ls`, `mkdir "a b"`), add/remove/reorder pages, uninstall an app that is in a folder.
- [ ] **Step 5:** Commit `terminal-launcher: folders and page edit mode`.

### Task 10: Final verification and hand-off

**Files:**
- Create: `terminal-launcher/README.md` (build, install, rollback, command list)

- [ ] **Step 1:** Run the full suite: `./gradlew --no-daemon clean assembleDebug testDebugUnitTest`. Expected: BUILD SUCCESSFUL, all tests PASS.
- [ ] **Step 2:** On the phone verify rotation and process restart (`adb shell am force-stop dev.linkinxp.termlauncher`, press Home): layout, theme, hidden apps and folders persist; corrupt data check by clearing app storage -> default layout, launcher still opens.
- [ ] **Step 3:** Confirm rollback works: switch the default home app back to the stock launcher in Settings, and `adb uninstall dev.linkinxp.termlauncher` succeeds.
- [ ] **Step 4:** Write the README, add a memory note for the project (location, build/install commands, rollback), commit `terminal-launcher: README`.
