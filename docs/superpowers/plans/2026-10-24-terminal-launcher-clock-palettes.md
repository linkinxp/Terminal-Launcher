# Terminal Launcher Clock, Palettes and Custom Themes Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a 12/24-hour clock setting, seven clock styles, six popular terminal palettes and user-defined hex-color themes to the terminal launcher.

**Architecture:** Every format, text-art, color and validation rule is a pure Kotlin unit tested on the JVM. Thin Compose/DataStore layers (clock views, settings rows, prompt commands, backup fields) consume them and are verified by build, Robolectric/Roborazzi tests and a later on-device check.

**Tech Stack:** Kotlin 2.4.21, Jetpack Compose (BOM 2026.09.00), DataStore Preferences 1.3.0, kotlinx-serialization-json, Roborazzi/Robolectric (SDK 34), JUnit 4.

**Spec:** `docs/superpowers/specs/2026-10-24-terminal-launcher-clock-palettes-design.md`

## Global Constraints

- Work on android-dev (`ssh android-dev`), project `/root/projects/terminal-launcher`, branch `terminal-launcher`; commit after each task. Edit under `/tmp/tl` and push with `/tmp/tl-run.sh <gradle args>` (it only copies files over: delete removed files on the box too); commit with `/tmp/tl-commit6.sh <n> <msg> <result>` (ledger `/root/projects/.superpowers/progress-quick.md`). **Check the build/test result before committing**; never commit on a red run.
- Package `dev.linkinxp.termlauncher`; minSdk 34, compileSdk 37, targetSdk 36; JDK 17; Robolectric `@Config(sdk = [34])`.
- Per-task test command: `./gradlew --no-daemon testDebugUnitTest --tests '*<Class>'`; the whole suite `./gradlew --no-daemon assembleDebug testDebugUnitTest` must stay green (365 tests today).
- Robolectric tests share one DataStore: tests that read stores must reset them first (`BackupRepository.apply(LauncherBackup())` + clearing the layout, as `BackupRepositoryTest`/`SettingsScreenTest` do) and wait for async state with the polling helpers (`until`, `settle`); `vm.ui` is computed off the main thread.
- Defaults: clock format `AUTO`, clock style `PIXEL`; custom themes at most 8; blank clock digit `BLANK = -1`. Reserved words gain `clock`. New built-in themes appended after the nine existing ones, in this order: `gruvbox`, `nord`, `tokyo-night`, `catppuccin`, `monokai`, `one-dark`, all with `glow = false`, `cyber = false`.
- Help text stays within 12 lines of at most 40 columns and mentions every command word (`clock` shares a line with `theme`).
- No new permission. Performance: the drawer and home recomposition isolation added on 2026-10-24 must stay intact (do not read the whole `LauncherUi` in `MainActivity`'s root scope; add new fields to `homeUi` consumers only where needed).
- On-device look and feel is checked by the user afterwards; never take screenshots of the user's phone without them asking; never run the signed-release flow.
- Out of scope: Xresources/base16 import, per-element color overrides, animated clock transitions, a graphical color picker, seconds on any clock except EPOCH.

## Review Focus

- `clock` with odd input: `clock 13`, `clock style`, `clock style nope`, `CLOCK 12H`, extra words: usage message, nothing changed, no crash.
- Clock edge times: 00:00, 00:05, 12:00, 12:30, 23:59 in 12-hour mode (`12:xx am/pm`, blank leading digit), epoch values near Int limits, a blank digit passing through the glitch animation and every clock style.
- Custom themes: names with spaces/uppercase/emoji/reserved words/built-in names, hex values like `#fff`, `FFF`, `#GGGGGG`, `#12345`, an 9th theme, removing the active theme, corrupt stored data, a theme added while ASCII wallpaper is on.
- Theme lookups: every place that resolves a theme by name (`Prefs.theme`, `theme <name>`, settings, backup apply, import) must see custom themes; a stored name that no longer exists falls back to `classy-black` without a crash.
- Time-dependent UI: EPOCH style ticking only while resumed, 12/24 changes taking effect immediately, no extra recompositions of the home screen from the second ticker.

---

### Task 1: Clock format units, parser and reserved word (pure)

**Files:**
- Create: `clock/ClockFormat.kt`
- Modify: `ui/PixelClock.kt` (move `clockDigits` out; keep the old 2-arg call working via a default), `ui/StatusLine.kt` (`statusTail(..., h24: Boolean = true)`), `calendar/Agenda.kt` (`formatAgenda(..., h24: Boolean = true)`), `prompt/CommandParser.kt` (`Command.Clock(sub: String?, arg: String?)`), `layout/HomeLayout.kt` (reserved `clock`), `LauncherViewModel.kt` (temporary no-op branch, help line, completion list)
- Test: `clock/ClockFormatTest.kt` (drafted in `/tmp/tl`), `sys/FormatTest.kt`, `calendar/AgendaTest.kt`, `prompt/CommandParserTest.kt`, `layout/HomeLayoutTest.kt` (drafted additions in `/tmp/tl`)

**Interfaces:**
- Produces: `enum class ClockFormat { AUTO, H24, H12 }` with `fun is24(systemIs24: Boolean): Boolean` and `companion fun fromName(name: String?): ClockFormat` (unknown/null -> AUTO); `const val BLANK = -1`; `fun clockDigits(hour: Int, minute: Int, h24: Boolean = true): List<Int>`; `fun amPm(hour: Int): String`; `fun formatClockTime(hour: Int, minute: Int, h24: Boolean): String`; `fun parseClockFormat(arg: String?): Result<ClockFormat>`; `Command.Clock(val sub: String?, val arg: String?)`.

- [ ] **Step 1:** The tests are already drafted under `/tmp/tl/app/src/test/...` (see Global Constraints: they are pushed with the next `tl-run.sh`). Adjust the drafted parser test to the spec's shape: `"clock"` -> `Clock(null, null)`, `"clock 12"` -> `Clock("12", null)`, `"CLOCK AUTO"` -> `Clock("auto", null)`, `"clock style figlet"` -> `Clock("style", "figlet")`. Remove the placeholder `assertNull(null as ClockFormat?)` line from `ClockFormatTest`.
- [ ] **Step 2: Run** `--tests '*ClockFormatTest' --tests '*FormatTest' --tests '*AgendaTest' --tests '*CommandParserTest' --tests '*HomeLayoutTest'`. Expected: FAIL (unresolved references).
- [ ] **Step 3: Implement.** Move `clockDigits` into `clock/ClockFormat.kt` with the new signature (12-hour: hour 0 -> 12, 13..23 -> 1..11, first digit `BLANK` when the hour is below 10). `formatClockTime`: `HH:mm` in 24-hour, `h:mmam/pm` (no leading zero) in 12-hour. `parseClockFormat` accepts `auto`, `24`, `24h`, `12`, `12h` (trimmed, any case), else fails with `usage: clock auto|24|12`. `statusTail` and `formatAgenda` format times through `formatClockTime`; the agenda width/truncation logic stays exact. Parser: `clock` with the same rest-of-line handling as `wx`. Add `"clock"` to `COMMAND_WORDS` and `COMMANDS`; temporary `is Command.Clock -> null` in the ViewModel `when`; re-flow `HELP` so line 1 reads `theme <name> | clock auto|24|12` and everything still fits.
- [ ] **Step 4: Run** the same classes, then the whole suite. Expected: PASS.
- [ ] **Step 5: Commit** `terminal-launcher: clock format units`.

### Task 2: Clock format wiring (Android)

**Files:**
- Modify: `data/Prefs.kt` (`clock_format`), `LauncherViewModel.kt` (`LauncherUi.clockFormat`, `clock` command, `cycleClockFormat()`), `ui/PagerHome.kt` + `ui/PixelClock.kt` (use the format, blank digit, am/pm suffix on the date line), `ui/StatusLine.kt` call site, `calendar` call site, the `date` command, `ui/SettingsScreen.kt` (`clock` row, tag `clock-row`), `backup/LauncherBackup.kt` + `BackupRepository.kt` (`clockFormat: String = "AUTO"`)
- Test: `screens/SettingsScreenTest.kt`, `backup/LauncherBackupTest.kt`, `backup/BackupRepositoryTest.kt` (drafted additions)

**Interfaces:**
- Consumes: Task 1. Produces: `Prefs.clockFormat: Flow<ClockFormat>`, `Prefs.setClockFormat(f: ClockFormat)`, `LauncherUi.clockFormat: ClockFormat = ClockFormat.AUTO`.

- [ ] **Step 1:** The drafted tests cover: the settings row cycles AUTO -> H24 -> H12 -> AUTO, the backup round trip (`H12`; missing field -> `AUTO`) and the repository snapshot/apply. Add: `clock` at the prompt prints the format and style lines (`format  auto`, `style   pixel` once Task 4 exists; for now only the format line), `clock 12` saves H12, `clock 13` prints the usage line.
- [ ] **Step 2: Run** `--tests '*SettingsScreenTest' --tests '*LauncherBackupTest' --tests '*BackupRepositoryTest'`. Expected: FAIL.
- [ ] **Step 3: Implement.** Resolve `AUTO` with `DateFormat.is24HourFormat(context)` where times are drawn (`PagerHome`, the status line, the agenda refresh, the `date` command via `DateTimeFormatter` patterns `HH:mm:ss` / `hh:mm:ss a`). The pixel clock draws `BLANK` as an empty slot (skip drawing; `glitchFrame` keeps working because blank digits are just locked values). In 12-hour mode append `  am`/`pm` to the date line. Keep `homeUi` isolation: add `clockFormat` to `Extras` (combine) and `LauncherUi`.
- [ ] **Step 4: Run** the three classes, then `assembleDebug testDebugUnitTest`. Expected: PASS.
- [ ] **Step 5: Commit** `terminal-launcher: 12/24 hour clock setting`.

### Task 3: Clock style units (pure)

**Files:**
- Create: `clock/ClockStyle.kt`, `clock/ClockText.kt`
- Test: `clock/ClockStyleTest.kt`, `clock/ClockTextTest.kt`

**Interfaces:**
- Produces: `enum class ClockStyle { PIXEL, SEGMENT, FIGLET, BINARY, HEX, EPOCH, PLAIN }` with `companion fun fromName(name: String?): ClockStyle` (unknown -> PIXEL); `fun parseClockStyle(arg: String?): Result<ClockStyle>` (case-insensitive; failure `usage: clock style pixel|segment|figlet|binary|hex|epoch|plain`); `fun figletLines(digits: List<Int>): List<String>`; `fun binaryColumns(hour: Int, minute: Int): List<List<Boolean>>`; `fun hexTime(hour: Int, minute: Int): String`; `fun epochText(epochSeconds: Long): String`; `fun segmentMask(digit: Int): Int`.

- [ ] **Step 1: Write the failing tests.** `figletLines`: for every digit 0-9 and `BLANK`, the result has exactly 3 lines, all of equal width (digit width 4 plus one separating space per digit), only printable ASCII; `figletLines(listOf(2,1,0,5))` has width `4*5 - 1`; two different digits produce different art; a blank digit renders as spaces. `binaryColumns(21, 29)`: column sizes `[3, 4, 3, 4]`, values for tens-of-hour 2 (`[false, true, false]` with the least significant bit last), units-of-hour 1, tens-of-minute 2, units-of-minute 9 (`[true, false, false, true]`); `binaryColumns(0, 0)` all false; `binaryColumns(23, 59)` the matching bit patterns. `hexTime(0, 0) == "0x00:00"`, `hexTime(15, 30) == "0x0F:1E"`, `hexTime(23, 59) == "0x17:3B"`. `epochText(1_761_283_749L) == "1761283749"`; `epochText(0) == "0000000000"` (10 digits, zero padded); a negative value is clamped to 0. `segmentMask`: 8 -> 0b1111111, 1 -> exactly two segments, `BLANK` and out-of-range digits -> 0, every digit 0-9 distinct from the others except none equal. `ClockStyle.fromName` and `parseClockStyle` cover every name, mixed case, null, blank and `nope`.
- [ ] **Step 2: Run** the two classes. Expected: FAIL.
- [ ] **Step 3: Implement** the pure functions: a hand-drawn 3-line font in a `Map<Int, List<String>>` (every glyph 4 columns wide, made of ` _|/\`), BCD bit extraction for binary (hour tens 0-2 uses 3 rows, minute tens 0-5 uses 3 rows, units use 4), segment masks as `a=1 b=2 c=4 d=8 e=16 f=32 g=64`.
- [ ] **Step 4: Run** the tests and the whole suite. Expected: PASS.
- [ ] **Step 5: Commit** `terminal-launcher: clock style units`.

### Task 4: Clock styles (Android)

**Files:**
- Create: `ui/ClockView.kt` (dispatcher + SEGMENT/FIGLET/BINARY/HEX/EPOCH/PLAIN composables)
- Modify: `ui/PixelClock.kt` (stays the PIXEL style), `ui/PagerHome.kt` (use `ClockView`), `data/Prefs.kt` (`clock_style`), `LauncherViewModel.kt` (`clock style <name>`, `LauncherUi.clockStyle`, `cycleClockStyle()`), `ui/SettingsScreen.kt` (`clock style` row, tag `clock-style-row`), `backup/*` (`clockStyle: String = "PIXEL"`)
- Test: `screens/SettingsScreenTest.kt`, backup tests, `screens/ScreenshotsTest.kt`

**Interfaces:**
- Consumes: Tasks 2-3. Produces: `@Composable fun ClockView(style: ClockStyle, time: LocalDateTime, h24: Boolean, glitch: Boolean, modifier: Modifier = Modifier)`; `Prefs.clockStyle: Flow<ClockStyle>`, `Prefs.setClockStyle`.

- [ ] **Step 1: Write the failing tests.** Settings row cycles through all seven styles and wraps; `clock style figlet` saves FIGLET; `clock style nope` prints the usage line and keeps the style; bare `clock` prints both the `format` and `style` lines; backup round trip (`FIGLET`; missing -> `PIXEL`; unknown name -> `PIXEL`); `BackupRepositoryTest` snapshot/apply. Roborazzi cases (not assertions): each style at 21:29 in `neon-city` and `matrix` (14 PNGs) and the 12-hour variants for PIXEL and FIGLET.
- [ ] **Step 2: Run** the tests. Expected: FAIL.
- [ ] **Step 3: Implement.** SEGMENT: draw the lit segments (rounded bars) on a canvas with the pixel clock's size/glow/glitch handling; FIGLET: the three lines as monospace text (theme foreground, `Shadow` for glow when `theme.glow`); BINARY: a row of four columns of `●`/`○` plus a small decimal caption; HEX/PLAIN: large monospace text; EPOCH: ticks every second only while resumed (`repeatOnLifecycle(RESUMED)`; once per minute under reduce motion) and never recomposes anything but the clock. Each style keeps the clock's fixed height so the layout below does not jump. All styles go through `rememberReduceMotion`.
- [ ] **Step 4: Run** the tests, render with `recordRoborazziDebug`, look at the PNGs and fix visible problems (clipped glyphs, uneven spacing); then `assembleDebug testDebugUnitTest`. Expected: PASS.
- [ ] **Step 5: Commit** `terminal-launcher: clock styles`.

### Task 5: Six palettes

**Files:**
- Modify: `theme/ThemeRegistry.kt`
- Test: `theme/ThemeRegistryTest.kt`

- [ ] **Step 1: Write the failing tests.** `all` has 15 names in the order of Global Constraints; exact colors for every new theme from the spec's table (assert background, foreground, accent, dim, border); all six have `cyber == false` and `glow == false`; names stay unique and lowercase; `describeThemes` has 15 lines.
- [ ] **Step 2: Run** `--tests '*ThemeRegistryTest'`. Expected: FAIL.
- [ ] **Step 3: Implement** the six registry entries with the spec's exact values.
- [ ] **Step 4: Run** the test and the whole suite (existing tests that assert nine themes must be updated to fifteen). Expected: PASS.
- [ ] **Step 5: Commit** `terminal-launcher: popular palettes`.

### Task 6: Custom theme units (pure)

**Files:**
- Create: `theme/Colors.kt`, `theme/CustomThemes.kt`, `theme/ThemeBook.kt`
- Test: `theme/ColorsTest.kt`, `theme/CustomThemesTest.kt`, `theme/ThemeBookTest.kt`

**Interfaces:**
- Produces: `fun parseHexColor(text: String): Long?`; `fun contrastRatio(a: Long, b: Long): Double`; `fun validateCustomTheme(name: String, existingCustom: List<String>, builtIn: List<String>): String?`; `fun buildCustomTheme(name: String, bg: Long, fg: Long, accent: Long, dim: Long, border: BorderStyle): Theme`; `object CustomThemes { const val MAX = 8; fun encode(list: List<Theme>): String; fun decode(raw: String?): List<Theme> }`; `class ThemeBook(val custom: List<Theme>) { val all: List<Theme>; fun byName(name: String): Theme? }`; `fun parseThemeAdd(args: List<String>): Result<CustomThemeSpec>` with `data class CustomThemeSpec(val name: String, val bg: Long, val fg: Long, val accent: Long, val dim: Long, val border: BorderStyle)`.

- [ ] **Step 1: Write the failing tests.** `parseHexColor`: `#112233`, `112233`, `#abc` (-> `0xFFAABBCC`), `ABC`, mixed case, surrounding spaces all parse to `0xFF------`; `#12345`, `#GGGGGG`, `""`, `"#"`, `#1234567` -> null. `contrastRatio(0xFF000000, 0xFFFFFFFF)` is 21.0 (within 0.01), equal colors 1.0, symmetric. `validateCustomTheme`: valid name -> null; blank, 25 characters, uppercase letters, a space, `a/b`, emoji, a built-in name (`matrix`), a reserved word (`ls`, `settings`, `clock`) each give a message; a name equal to an existing custom one is allowed (replace); with 8 existing custom names and a new name -> `theme: too many custom themes (8 max)`. `parseThemeAdd`: `["mine","#000","#0f0","#0ff","#066"]` ok with border SINGLE; with `rounded` border; missing colors, bad color, bad border -> usage/messages; `["mine"]` -> usage `usage: theme add <name> <bg> <fg> <accent> <dim> [single|double|rounded|ascii|bracket]`. `CustomThemes`: round trip of three themes; `decode(null)`, `decode("")`, `decode("{bad")` -> empty; an entry with a bad color or blank name is skipped; more than 8 entries cut to 8. `ThemeBook`: `all` is built-ins followed by custom; `byName` case-insensitive and finds custom; a custom theme can never shadow a built-in (built-in wins).
- [ ] **Step 2: Run** the three classes. Expected: FAIL.
- [ ] **Step 3: Implement** (contrast per WCAG: relative luminance of sRGB channels; encoding as JSON via kotlinx-serialization with a small `@Serializable` DTO; border stored by enum name).
- [ ] **Step 4: Run** the tests and the whole suite. Expected: PASS.
- [ ] **Step 5: Commit** `terminal-launcher: custom theme units`.

### Task 7: Custom themes wiring (Android)

**Files:**
- Modify: `data/Prefs.kt` (`custom_themes`; `theme` flow resolves through `ThemeBook`), `LauncherViewModel.kt` (`theme add/rm`, `LauncherUi.customThemes`, bare `theme` lists custom ones with `+`), `prompt/CommandParser.kt` (`theme add ...` and `theme rm ...` parse into `Command.Theme(name)` today; add `Command.ThemeAdd(args: List<String>)` and `Command.ThemeRm(name: String)`), `ui/SettingsScreen.kt` (theme list from `ThemeBook`, `+` marker, `[rm]` on custom rows), `backup/*` (`customThemes: List<ThemeBackup>`), every `ThemeRegistry.byName`/`ThemeRegistry.all` use that must see custom themes
- Test: parser tests, `screens/SettingsScreenTest.kt`, `backup/*Test.kt`, `screens/CustomThemeFlowTest.kt` (new, Robolectric)

**Interfaces:**
- Consumes: Tasks 5-6. Produces: `Prefs.customThemes: Flow<List<Theme>>`, `Prefs.editCustomThemes(op: (List<Theme>) -> List<Theme>)`, `LauncherUi.customThemes: List<Theme>`.

- [ ] **Step 1: Write the failing tests.** Parser: `"theme add mine #000 #0f0 #0ff #066"` -> `ThemeAdd(["mine","#000","#0f0","#0ff","#066"])`; `"theme rm mine"` -> `ThemeRm("mine")`; `"theme add"` -> `ThemeAdd(emptyList())`; plain `theme matrix` unchanged; `"theme rm"` -> `Theme("rm")` falls to the existing no-such-theme message. `CustomThemeFlowTest`: through the ViewModel, `theme add mine #000000 #00ff00 #00ffff #006666` then wait until `ui.customThemes` holds `mine`; `theme mine` makes it the active theme; `theme rm mine` removes it and the active theme falls back to `classy-black`; adding with a bad hex prints the reason and adds nothing; a 9th custom theme is refused; adding the same name again replaces it; low-contrast colors still add but print `theme: low contrast`. Settings: the custom theme appears in the list and tapping it applies it; its `[rm]` removes it. Backup: custom themes round-trip through snapshot/apply; a stored active theme name that is custom survives restore.
- [ ] **Step 2: Run** the new/changed tests. Expected: FAIL.
- [ ] **Step 3: Implement.** `Prefs.theme` maps the stored name through a `ThemeBook` built from the stored custom list (combine of the two stores), unknown name -> default. Keep `ThemeRegistry` for built-ins only. Atomic edits for the custom list (`editCustomThemes`). The ViewModel's `setTheme` and the `theme <name>` command resolve through the current `ThemeBook`.
- [ ] **Step 4: Run** the tests, then `assembleDebug testDebugUnitTest`. Expected: PASS, BUILD SUCCESSFUL.
- [ ] **Step 5: Commit** `terminal-launcher: custom themes`.

### Task 8: Screenshots, README, version and final verification

**Files:**
- Modify: `screens/ScreenshotsTest.kt`, `README.md`, `app/build.gradle.kts` + `LauncherViewModel.LAUNCHER_VERSION` (`0.6.0`)

- [ ] **Step 1:** Render all screenshots (`recordRoborazziDebug`): the six new palettes (home), every clock style in two themes, 12-hour PIXEL/FIGLET, the settings screen showing the longer theme list with a custom entry. Look at them and fix visible problems.
- [ ] **Step 2:** Update `README.md` (clock format, styles, palettes, custom themes with the hex-color rules, `clock` and `theme add/rm` commands, backup fields) and bump the version to `0.6.0`.
- [ ] **Step 3:** Run `./gradlew --no-daemon clean assembleDebug testDebugUnitTest`. Expected: BUILD SUCCESSFUL, 0 failures; repeat the Robolectric-heavy classes (`SettingsScreenTest`, `CustomThemeFlowTest`, `BackupRepositoryTest`) three times to catch flakiness.
- [ ] **Step 4:** Commit `terminal-launcher: clock, palettes and custom themes round screenshots and docs, v0.6.0`; install the APK on the phone with adb (`install -r`).
