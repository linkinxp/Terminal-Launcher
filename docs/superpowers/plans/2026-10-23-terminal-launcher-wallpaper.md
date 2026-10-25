# Terminal Launcher Wallpaper Round Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add wallpaper backgrounds to the launcher: the transparent system wallpaper and generated seeded ASCII art, controlled by `wall` prompt commands.

**Architecture:** Pure Kotlin units (ASCII generator, drift, settings/command logic, parser) are unit tested on the JVM. A thin Compose layer (`WallBackground`, transparent window theme, drawer/home composition change) consumes them and is verified by build, Roborazzi JVM screenshots and a later on-device check.

**Tech Stack:** Kotlin 2.4.21, Jetpack Compose (BOM 2026.09.00), DataStore Preferences, Roborazzi/Robolectric (SDK 34), JUnit 4.

**Spec:** `docs/superpowers/specs/2026-10-23-terminal-launcher-wallpaper-design.md`

## Global Constraints

- Work on android-dev (`ssh android-dev`), project `/root/projects/terminal-launcher`, branch `terminal-launcher`; commit after each task. Edit files under `/tmp/tl` and push with `/tmp/tl-run.sh <gradle args>` (tars `/tmp/tl` to the project and runs `./gradlew --no-daemon ...`); `/tmp/tl-commit2.sh <n> <msg> <result>` commits and ledgers. Ledger file: `/root/projects/.superpowers/progress-wall.md`.
- Package `dev.linkinxp.termlauncher`; minSdk 31, compileSdk 37, targetSdk 36; JDK 17; Robolectric `@Config(sdk = [34])`.
- Per-task test command: `./gradlew --no-daemon testDebugUnitTest --tests '*<Class>'`; the whole suite `./gradlew --no-daemon assembleDebug testDebugUnitTest` must stay green.
- Modes `SOLID | SYSTEM | ASCII`, styles `GLYPH | CIRCUIT | CONTOUR | STARS | HEXDUMP | SKYLINE` (typed names lowercase), defaults mode `SOLID`, style `GLYPH`, seed `1`, dim `55`, animate `false`. Dim range 0-100.
- Reserved word added: `wall` (cannot be a folder name; also in help text and tab-completion list).
- Art grid: about 6.6 dp per column and 13 dp per row at 11 sp monospace (`TermFont`); art color is the theme's `dim` at low contrast; drawer readability layer is a fixed 35% theme-background alpha.
- Animation: scroll up one row about every 250 ms only while resumed; off under reduce motion; costs nothing when off.
- No new permission. Do not read or write system wallpaper files; no photo picker; no set-as-system-wallpaper.
- Default look (`SOLID`) must be pixel-identical to before; verify with the existing screenshots.
- On-device behavior (real wallpaper showing through, legibility over bright wallpapers, animation smoothness, battery) is checked by the user after pairing; do not claim it works without that. Never run the signed-release flow.

## Review Focus

- `wall dim` with non-numbers, -1, 101, 0, 100; `wall seed` with a number too large for Int, with text, or missing; `wall style`/`mode`/`dim` with no argument: usage message, settings unchanged, never a crash.
- `generateAscii` with 0, 1 and negative columns/rows and with a large grid (500 x 500): exact dimensions, printable ASCII only, no exception, sane time.
- Mode changes with the drawer open and closed: home stays composed (invisible) under the drawer, the pager page is kept, no touches reach the invisible home while the drawer is open, and alpha is back to 1 when it closes.
- Drift with a huge or negative step (Int overflow) and an empty art list: wraps with floorMod, never throws.
- Corrupt or missing stored wall values (bad enum name, dim outside 0-100): field-by-field fallback to defaults, launcher still starts.

---

### Task 1: ASCII art generator and drift

**Files:**
- Create: `wall/AsciiArt.kt`
- Test: `wall/AsciiArtTest.kt`

**Interfaces:**
- Produces: `enum class AsciiStyle { GLYPH, CIRCUIT, CONTOUR, STARS, HEXDUMP, SKYLINE }`; `fun generateAscii(style: AsciiStyle, cols: Int, rows: Int, seed: Int): List<String>`; `fun driftLines(lines: List<String>, step: Int): List<String>`.

- [ ] **Step 1: Write the failing tests** in `AsciiArtTest`: for every style and sizes `(80,60)`, `(1,1)`, `(7,3)` the result has exactly `rows` strings of exactly `cols` chars, all in 0x20-0x7E; `cols = 0` or `rows = 0` or negative sizes return an empty list or empty strings without throwing; same inputs give equal output; a different seed gives different output for every style at 80x60; `STARS` has at least 80% spaces; `GLYPH` at least 60% non-space; `SKYLINE` last row at least 50% non-space; `HEXDUMP` rows (cols >= 20) contain a `:` and at least one hex digit; `generateAscii(GLYPH, 500, 500, 1)` completes (assert it returns 500 rows). `driftLines(listOf("a","b","c"), 1) == listOf("b","c","a")`, step `-1 -> ["c","a","b"]`, step `4 -> ["b","c","a"]`, step `Int.MIN_VALUE` and `Int.MAX_VALUE` do not throw, empty list returns empty.
- [ ] **Step 2: Run** `--tests '*AsciiArtTest'`. Expected: FAIL (unresolved reference).
- [ ] **Step 3: Implement** both functions, deterministic via `kotlin.random.Random(seed * 31 + style.ordinal)`. Styles: `GLYPH` random cell from `01{}<>/\|=+*#$%&:;` with 15% spaces; `CIRCUIT` mostly spaces with random horizontal/vertical traces of `-` `|` joined by `+`, pads `o` and `#`; `CONTOUR` height field from a few seeded sine waves mapped to bands drawn with ` .:-=+*#%`; `STARS` about 4% of cells set to `.` `+` `*`; `HEXDUMP` rows like `0a3c: 4f 2e ..  ascii` (4-hex-digit address, `: `, byte pairs, two spaces, printable ASCII) cut or padded to `cols`; `SKYLINE` adjacent buildings 3-8 columns wide, heights up to 70% of rows, filled with `#` and a window pattern, no gaps so the bottom row is built up. `driftLines` rotates rows with `Math.floorMod(step, size)`.
- [ ] **Step 4: Run** `--tests '*AsciiArtTest'`, then the whole suite. Expected: PASS.
- [ ] **Step 5: Commit** `terminal-launcher: ascii art generator`.

### Task 2: Wall settings, commands and parser

**Files:**
- Create: `wall/WallSettings.kt`
- Modify: `prompt/CommandParser.kt` (`Command.Wall`), `layout/HomeLayout.kt` (reserved word), `LauncherViewModel.kt` (temporary no-op branch, help text, completion list)
- Test: `wall/WallSettingsTest.kt`, `prompt/CommandParserTest.kt` (extend), `layout/HomeLayoutTest.kt` (extend)

**Interfaces:**
- Consumes: `AsciiStyle` (Task 1).
- Produces: `enum class WallMode { SOLID, SYSTEM, ASCII }`; `data class WallSettings(val mode: WallMode = WallMode.SOLID, val style: AsciiStyle = AsciiStyle.GLYPH, val seed: Int = 1, val dim: Int = 55, val animate: Boolean = false)`; `fun applyWall(current: WallSettings, sub: String?, arg: String?, random: kotlin.random.Random): Result<WallSettings>`; `fun WallSettings.describe(): String`; `Command.Wall(val sub: String?, val arg: String?)`.

- [ ] **Step 1: Write the failing tests.**
  - `applyWall` (all with defaults as `current`): `("style","circuit")` sets style; `("STYLE","Stars")` case-insensitive; `("style","nope")` fails with a message listing the six styles; `("style", null)` fails; `("next", null)` goes GLYPH -> CIRCUIT and wraps SKYLINE -> GLYPH; `("seed","42")` sets 42; `("seed","-7")` ok; `("seed","99999999999")`, `("seed","x")`, `("seed", null)` fail with `usage: wall seed <number>`; `("mode","ascii")`/`system`/`solid` set the mode, `("mode","bad")` fails listing `solid system ascii`; `("dim","0")` and `("dim","100")` ok, `("dim","-1")`, `("dim","101")`, `("dim","x")`, `("dim", null)` fail with `usage: wall dim <0-100>`; `("animate","on")` and `("animate","off")` set it, `("animate","maybe")` fails; `("random", null)` with `Random(1)` gives the same result every time and differs from `current` in style or seed (also when the random draw would repeat the current values); unknown sub `("bogus", null)` fails with `wall: bogus: unknown (random next style seed mode dim animate)`; `(null, null)` returns `current` unchanged. Every failure leaves nothing to apply (the caller keeps `current`).
  - `describe`: contains lines starting `mode`, `style`, `seed`, `dim`, `anim` with the current values.
  - Parser: `"wall"` -> `Wall(null, null)`; `"wall mode ascii"` -> `Wall("mode", "ascii")`; `"WALL Dim 40"` -> `Wall("dim", "40")`; `"wall style stars extra"` -> `Wall("style", "stars")`.
  - `HomeLayoutTest`: `mkdir("wall")` fails.
- [ ] **Step 2: Run** the three test classes. Expected: FAIL.
- [ ] **Step 3: Implement** the types and functions, the parser branch, `wall` in `COMMAND_WORDS`, a temporary `is Command.Wall -> null` branch in the ViewModel `when` (replaced in Task 3), `wall [mode|style|...]` line in `HELP` (keep help at most 12 lines and each at most 40 columns) and `"wall"` in `COMMANDS`. `random`: new style from `AsciiStyle.entries` and new `random.nextInt()` seed, bumping the seed by 1 if both equal the current values.
- [ ] **Step 4: Run** the new tests, then the whole suite. Expected: PASS.
- [ ] **Step 5: Commit** `terminal-launcher: wall settings and commands`.

### Task 3: Wallpaper backgrounds in the app

**Files:**
- Create: `ui/WallBackground.kt`, `res/values/themes.xml`
- Modify: `AndroidManifest.xml` (application theme), `data/Prefs.kt`, `LauncherViewModel.kt` (`wall` in `LauncherUi`, command, `wall` persisted), `ui/TermTheme.kt` (wall parameter), `ui/PagerHome.kt`, `ui/AppDrawer.kt`, `MainActivity.kt`

**Interfaces:**
- Consumes: Tasks 1-2, `rememberReduceMotion()`.
- Produces: `LauncherUi.wall: WallSettings`; `@Composable fun WallBackground(wall: WallSettings, theme: Theme, reduceMotion: Boolean, modifier: Modifier = Modifier)`; `TermTheme(theme: Theme, wall: WallSettings = WallSettings(), content: @Composable () -> Unit)`.

- [ ] **Step 1:** Add `res/values/themes.xml` with `Theme.Launcher` (parent `android:Theme.Material.Wallpaper.NoTitleBar`, transparent window background, `windowShowWallpaper` true) and set it on `<application>`. Run `assembleDebug`. Expected: BUILD SUCCESSFUL (the existing `@android:style` theme reference is replaced).
- [ ] **Step 2:** `Prefs`: store `wall_mode`, `wall_style`, `wall_seed`, `wall_dim`, `wall_animate` and expose `val wall: Flow<WallSettings>` (each missing/invalid/out-of-range field falls back to its default) plus `suspend fun setWall(w: WallSettings)`. ViewModel: add `wall` to `Persisted`/`LauncherUi`; handle `Command.Wall` (bare -> `describe()`, otherwise `applyWall(cur.wall, sub, arg, Random.Default).fold(save and null, message)`).
- [ ] **Step 3:** `WallBackground`: `SOLID` fills the theme background; `SYSTEM` fills the theme background at alpha `dim / 100f` over the transparent window; `ASCII` fills the opaque theme background and draws `generateAscii(...)` (grid from the canvas size and 6.6 dp x 13 dp cells) as ONE multi-line text layout per frame in `theme.dim` at low alpha, measured with `rememberTextMeasurer`, remembered per (style, seed, grid size, drift step). When `animate && !reduceMotion` a `LaunchedEffect` advances a step counter every 250 ms and the shown lines are `driftLines(lines, step)`. `TermTheme(theme, wall)` uses `WallBackground` in place of its solid fill and `MainActivity` passes `ui.wall`.
- [ ] **Step 4:** Drawer over wall: `PagerHome` gets `Modifier.alpha(0f)` from `MainActivity` while `ui.drawerOpen` (still composed); the `AppDrawer` root drops its opaque background in `SYSTEM`/`ASCII` mode and instead draws a fixed 35% theme-background layer; in `SOLID` mode it keeps today's opaque background.
- [ ] **Step 5:** Run `assembleDebug testDebugUnitTest`. Expected: BUILD SUCCESSFUL, all tests PASS (existing screenshots unchanged for `SOLID`).
- [ ] **Step 6: Commit** `terminal-launcher: wallpaper backgrounds`.

### Task 4: Screenshots, README and final verification

**Files:**
- Modify: `screens/ScreenshotsTest.kt`, `README.md`

- [ ] **Step 1:** Extend `ScreenshotsTest`: render each of the six styles over `neon-city` and `matrix` (`mode = ASCII`, fixed seed), `SYSTEM` mode over a checkerboard stand-in behind the launcher, and the drawer over `ASCII` mode. Run `./gradlew --no-daemon recordRoborazziDebug`. Expected: BUILD SUCCESSFUL, one PNG per case in `app/build/shots`.
- [ ] **Step 2:** Look at the PNGs; fix any visible problem (art too bright to read text, wrong grid size, clipped rows, drawer unreadable) and re-render until clean.
- [ ] **Step 3:** Update `README.md` (`wall` commands, modes, styles, defaults, animation and reduce-motion note, the system-wallpaper limit) and bump `versionName` to `0.3.0`.
- [ ] **Step 4:** Run `./gradlew --no-daemon clean assembleDebug testDebugUnitTest`. Expected: BUILD SUCCESSFUL, 0 failures.
- [ ] **Step 5:** Commit `terminal-launcher: wallpaper round screenshots and docs, v0.3.0`.
