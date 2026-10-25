# Terminal Launcher Cyberpunk Round Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add neon themes, CRT/glitch effects, a live system panel with a status line, and a smarter prompt to the existing terminal launcher.

**Architecture:** New logic lives in small pure Kotlin units that are unit tested on the JVM (formatting, history, completion, parser, fx settings, glitch frames, fortunes, neofetch). Thin Android/Compose layers (system stats, overlays, prompt UI) consume them and are verified by build, Roborazzi JVM screenshots, and a later on-device check.

**Tech Stack:** Kotlin 2.4.21, Jetpack Compose (BOM 2026.09.00), DataStore Preferences, Roborazzi + Robolectric (already set up, SDK 34), JUnit 4.

**Spec:** `docs/superpowers/specs/2026-10-23-terminal-launcher-cyberpunk-design.md` (extends `2026-10-23-terminal-launcher-design.md`)

## Global Constraints

- Work on android-dev (`ssh android-dev`), project `/root/projects/terminal-launcher`, branch `terminal-launcher`; commit after each task. Edit files locally under `/tmp/tl` and push with `/tmp/tl-run.sh <gradle args>` (it tars `/tmp/tl` to the project and runs `./gradlew --no-daemon ...`), or edit on the box directly; keep `/tmp/tl` and the project identical.
- Package `dev.linkinxp.termlauncher`; minSdk 31, compileSdk 37, targetSdk 36; JDK 17; Robolectric tests use `@Config(sdk = [34])`.
- Test command for a task: `./gradlew --no-daemon testDebugUnitTest --tests '*<Class>'`; the whole suite is `./gradlew --no-daemon assembleDebug testDebugUnitTest` (must stay green).
- Exactly 9 themes in this order: `classy-black`, `matrix`, `amber`, `solarized-dark`, `dracula`, `neon-city`, `synthwave`, `blade-runner`, `glitch-red`. Exact colors for the four new ones are in the spec's table; copy them verbatim.
- Reserved words (cannot be folder names): `ls theme pin hide mkdir mv rmdir help fx history neofetch whoami uptime date fortune clear sudo matrix`.
- One new permission only: `ACCESS_NETWORK_STATE`. No network use.
- Effect names exactly: `scanlines flicker glitch cursor boot`. Defaults: scanlines `null` (theme decides: on for `cyber` themes), flicker `false`, glitch `true`, cursor `true`, boot `true`.
- Output panel cap: 12 lines. History cap: 50 lines, last 5 shown as suggestions. Matrix rain 6 s. Boot log about 1 s. Glitch about 400 ms. Cursor period about 530 ms.
- Reduce motion = system animator duration scale 0: flicker, glitch scramble, boot log and matrix rain are skipped; scanlines and cursor remain.
- Visual checks of animations are done by the user on the phone after pairing; do not claim they work without that. Never run the signed-release flow.
- Out of scope: weather, calendar, media, notifications, toggles, Garmin panel, gestures, theme editor, sound.

## Review Focus

- `fx` with unknown name, missing value, wrong case (`FX Scanlines OFF`), or `fx scanlines maybe`: usage/no-such-effect message, settings unchanged, no crash.
- `complete` with empty or whitespace input, one candidate, different letter case, several candidates with no longer common prefix, multi-word app names: never throws, never shortens the user's text.
- History with blank lines, repeated lines, more than 50 entries, or corrupt/missing stored data: capped at 50, no blanks, starts empty on bad data.
- System sources failing (total RAM 0, no active network, battery extras missing, NaN or negative fractions): `?` or an empty bar, never a crash or exception on the UI thread.
- `glitchFrame` with progress below 0, above 1 or NaN, and overlays (boot, matrix) that must always end (timeout + tap, cancelled on pause) and respect reduce motion.

---

### Task 1: Neon themes, bracket panels, reserved words

**Files:**
- Modify: `theme/Theme.kt` (add `BorderStyle.BRACKET`, `Theme.cyber`), `theme/ThemeRegistry.kt`, `layout/HomeLayout.kt` (COMMAND_WORDS), `ui/TermPanel.kt` (draw BRACKET)
- Test: `theme/ThemeRegistryTest.kt`, `layout/HomeLayoutTest.kt`

**Interfaces:**
- Produces: `enum BorderStyle { SINGLE, DOUBLE, ROUNDED, ASCII, BRACKET }`; `Theme(..., val glow: Boolean, val cyber: Boolean = false)`; `ThemeRegistry.all` of 9; the four new themes with `cyber = true`.

- [ ] **Step 1: Write the failing tests.** In `ThemeRegistryTest`: change `allHasTheFiveSpecThemesInOrder` to the 9 names; add `newThemesUseSpecColors` (neon-city background `0xFF0B0014`, foreground `0xFF00F0FF`, accent `0xFFFF2BD6`, border `BRACKET`; glitch-red foreground `0xFFFF3B3B`; blade-runner border `DOUBLE`; synthwave border `ROUNDED`) and `onlyTheNewThemesAreCyber` (exactly the last four have `cyber`). In `HomeLayoutTest`: `mkdirRejectsNewReservedWords` looping `fx history neofetch whoami uptime date fortune clear sudo matrix` and asserting `mkdir(word).isFailure`.
- [ ] **Step 2: Run** `--tests '*ThemeRegistryTest' --tests '*HomeLayoutTest'`. Expected: FAIL (compile or assertion).
- [ ] **Step 3: Implement** the enum value, `cyber` field, four registry entries (values from the spec table, `glow = true`), the ten extra COMMAND_WORDS, and `TermPanel` drawing `BRACKET` as 12 dp corner ticks (four L-shaped strokes) instead of a full outline.
- [ ] **Step 4: Run** the same tests, then the whole suite. Expected: PASS.
- [ ] **Step 5: Commit** `terminal-launcher: neon themes, bracket panels, reserved words`.

### Task 2: Format helpers, system stats, sys panel, status line

**Files:**
- Create: `sys/Format.kt`, `sys/SystemStats.kt`, `ui/SysPanel.kt`, `ui/StatusLine.kt`
- Modify: `ui/PagerHome.kt` (sys panel + status line replace the old panel text and page dots), `AndroidManifest.xml` (`ACCESS_NETWORK_STATE`)
- Test: `sys/FormatTest.kt`

**Interfaces:**
- Produces: `fun asciiBar(fraction: Float, width: Int = 10): String`; `fun formatUptime(ms: Long): String`; `fun pageLabel(index: Int): String` and `fun statusTail(batteryPct: Int?, charging: Boolean, hour: Int, minute: Int): String` (both in `ui/StatusLine.kt`, pure); `data class SysSnapshot(val batteryPct: Int?, val charging: Boolean, val ramPct: Int?, val diskPct: Int?, val uptimeMs: Long, val net: String)` and `fun readSystemStats(context: Context): SysSnapshot` (each source wrapped so a failure gives `null`/`"?"`); `@Composable fun SysPanel(snapshot: SysSnapshot)`; `@Composable fun StatusLine(pageCount: Int, current: Int, snapshot: SysSnapshot, now: LocalDateTime, onPage: (Int) -> Unit)`.

- [ ] **Step 1: Write the failing tests** in `FormatTest`: `asciiBar(0.62f) == "[######----]"`, `asciiBar(0f) == "[----------]"`, `asciiBar(1f) == "[##########]"`, `asciiBar(-1f)` and `asciiBar(Float.NaN)` equal the empty bar, `asciiBar(2f)` the full bar, `asciiBar(0.5f, 4) == "[##--]"`; `formatUptime`: `0 -> "0m"`, `59_999 -> "0m"`, `12 * 60_000L -> "12m"`, `(4 * 60 + 12) * 60_000L -> "4h 12m"`, `(3 * 24 + 4) * 3_600_000L -> "3d 4h"`, `-5 -> "0m"`; `pageLabel`: `0 -> "1:home"`, `1 -> "2:two"`, `4 -> "5:five"`, `7 -> "8"`; `statusTail(66, false, 21, 8) == "bat 66% │ 21:08"`, `statusTail(66, true, 9, 5) == "bat 66%+ │ 09:05"`, `statusTail(null, false, 0, 0) == "bat ? │ 00:00"`.
- [ ] **Step 2: Run** `--tests '*FormatTest'`. Expected: FAIL (unresolved reference).
- [ ] **Step 3: Implement** the pure functions (bar = `[` + `#` x round(fraction x width) + `-` x rest + `]`; uptime picks the largest two units as in the tests), then `readSystemStats` (battery sticky intent, `ActivityManager.MemoryInfo` with total 0 -> null, `StatFs(Environment.getDataDirectory())`, `SystemClock.elapsedRealtime()`, `ConnectivityManager` capabilities -> `wifi`/`cell`/`offline`, else `?`), `SysPanel` (lines `bat`, `ram`, `disk` with bars, `up`, `net`; `?` when null), `StatusLine` (`[0] ` + page labels with `*` on the current one, each tappable, then ` │ ` + `statusTail`), and wire both into `PagerHome` (snapshot refreshed with the existing minute tick/screen-on). Add the permission.
- [ ] **Step 4: Run** `--tests '*FormatTest'`, then `assembleDebug testDebugUnitTest`. Expected: PASS and BUILD SUCCESSFUL.
- [ ] **Step 5: Commit** `terminal-launcher: system panel and status line`.

### Task 3: Pure prompt units

**Files:**
- Modify: `prompt/CommandParser.kt`
- Create: `prompt/CommandHistory.kt`, `prompt/Completion.kt`, `prompt/Fortunes.kt`, `prompt/Neofetch.kt`, `sys/BootLog.kt`
- Test: `prompt/CommandParserTest.kt` (extend), `prompt/CommandHistoryTest.kt`, `prompt/CompletionTest.kt`, `prompt/FortunesTest.kt`, `prompt/NeofetchTest.kt`, `sys/BootLogTest.kt`

**Interfaces:**
- Produces: `Command.Fx(name: String?, on: Boolean?)`, `Command.History`, `Neofetch`, `Whoami`, `Uptime`, `Date`, `Fortune`, `Clear`, `Matrix` (objects), `Command.Sudo(rest: String)`; `class CommandHistory(max: Int = 50)` with `add(line: String)`, `last(n: Int): List<String>` (newest first), `all(): List<String>` (oldest first), `encode(): String`, `companion fun decode(raw: String?): CommandHistory`; `fun complete(input: String, candidates: List<String>): String`; `val fortunes: List<String>`, `fun fortune(seed: Int): String`; `data class DeviceInfo(val model: String, val android: String, val launcher: String, val uptime: String, val theme: String, val apps: Int, val ramPct: Int?)`, `fun neofetch(info: DeviceInfo): String`; `val bootLog: List<String>`.

- [ ] **Step 1: Write the failing tests.**
  - Parser: `"fx"` -> `Fx(null, null)`; `"fx scanlines on"` -> `Fx("scanlines", true)`; `"FX Scanlines OFF"` -> `Fx("scanlines", false)`; `"fx scanlines"` and `"fx scanlines maybe"` -> `Fx("scanlines", null)`; each of `history neofetch whoami uptime date fortune clear matrix` parses to its object; `"date now"` and `"clear all"` -> `Query`; `"sudo rm -rf /"` -> `Sudo("rm -rf /")`; `"sudo"` -> `Query("sudo")`.
  - History: blank lines ignored; consecutive duplicates dropped, non-consecutive kept; `last(2)` newest first; 60 adds keep only the newest 50 (`all().size == 50`, oldest dropped); `decode(encode())` round-trips; `decode(null)` and `decode("  ")` are empty.
  - Completion: `complete("ch", ["chrome","chrm","camera"]) == "chr"`; `complete("cam", same) == "camera"`; `complete("CH", ["chrome"]) == "chrome"`; `complete("chr", ["chrome","chrm"]) == "chr"` (no extension); `complete("", x) == ""`; `complete("  ", x) == "  "`; `complete("zzz", x) == "zzz"`; `complete("garmin c", ["garmin connect"]) == "garmin connect"`; `complete("chrome", ["chrome"]) == "chrome"`.
  - Fortunes: at least 15 entries, none blank, each at most 80 chars; `fortune(7) == fortune(7)`; `fortune(-5)` does not throw and is in the list; seeds 0..30 yield more than one distinct entry.
  - Neofetch: every field value appears in the output; every line is at most 40 chars (also with a 60-char model string, which is truncated); `ramPct = null` prints `?`.
  - BootLog: non-empty, each line at most 40 chars, last line contains `user@phone`.
- [ ] **Step 2: Run** these test classes. Expected: FAIL.
- [ ] **Step 3: Implement** each unit. Parser: exact-word match for the argument-less commands (extra args -> `Query`), `fx` name lowercased, on/off parsed case-insensitively. `complete`: case-insensitive filter by `startsWith(input.trim-left-only)`; return the longest common prefix of matches (using the first match's casing) only if longer than the input, else the input unchanged; blank input returned as given. Fortunes: 15+ short geek quotes, index with `Math.floorMod(seed, size)`. Neofetch: ASCII logo (at most 8 lines, at most 14 columns wide) on the left, `key: value` rows on the right, truncate each line to 40 columns.
- [ ] **Step 4: Run** the new tests and the whole suite. Expected: PASS.
- [ ] **Step 5: Commit** `terminal-launcher: prompt units (parser, history, completion, fortunes, neofetch, boot log)`.

### Task 4: Prompt wiring: output panel, history, tab key, new commands, matrix rain

**Files:**
- Create: `prompt/OutputLines.kt`, `ui/OutputPanel.kt`, `ui/MatrixRain.kt`
- Modify: `LauncherViewModel.kt`, `data/Prefs.kt` (history), `ui/PromptBar.kt`, `MainActivity.kt`
- Test: `prompt/OutputLinesTest.kt`

**Interfaces:**
- Consumes: Task 3 units, `readSystemStats`/`formatUptime` (Task 2).
- Produces: `fun capLines(text: String, max: Int = 12): String` (keeps the last `max` lines; blank text -> empty); `PromptState` gains `val history: List<String>` (last 5); `LauncherUi` gains `val matrix: Boolean`; `LauncherViewModel`: `fun complete(input: String): String`, `fun startMatrix()`, `fun stopMatrix()`; `@Composable fun OutputPanel(text: String, onDismiss: () -> Unit)`; `@Composable fun MatrixRain(onDone: () -> Unit)`.

- [ ] **Step 1: Write the failing test** `OutputLinesTest`: `capLines` keeps all lines when under the cap, keeps exactly the last 12 of 20 lines, returns `""` for `""`, handles a trailing newline without producing a blank last line, and honors a custom `max`.
- [ ] **Step 2: Run** `--tests '*OutputLinesTest'`. Expected: FAIL.
- [ ] **Step 3: Implement** `capLines`; then the wiring: the ViewModel records each submitted line in a persisted `CommandHistory` (Prefs key, corrupt/missing -> empty) and exposes the last 5 in `PromptState.history`; new commands produce output text (`neofetch` via `DeviceInfo` built from `Build.MODEL`, `Build.VERSION.RELEASE`, `BuildConfig`-free version string `0.2.0`, uptime, theme name, app count, ram %; `whoami` -> `user`; `uptime` -> `up <formatUptime>`; `date` -> `EEE d MMM yyyy HH:mm:ss` lowercase via `Locale.ROOT`-safe formatting; `fortune` -> `fortune(System.nanoTime().toInt())`; `clear` -> no message and clears the output; `sudo` -> the fixed sudoers line; `matrix` -> `startMatrix()`; `history` -> numbered lines; `help` lists every command); all message output is passed through `capLines` and shown by `OutputPanel` (tap dismisses). `PromptBar`: `⇥` key at the end of the prompt calling `vm.complete(...)` with candidates = command words + lowercase app labels; with an empty prompt, `history` entries show as tappable rows that fill the prompt. `MainActivity` shows `MatrixRain` while `ui.matrix` (ends after 6 s or on tap; skipped under reduce motion).
- [ ] **Step 4: Run** `--tests '*OutputLinesTest'`, then `assembleDebug testDebugUnitTest`. Expected: PASS, BUILD SUCCESSFUL.
- [ ] **Step 5: Commit** `terminal-launcher: output panel, history, tab-complete, new commands, matrix rain`.

### Task 5: Effects (fx settings, scanlines, flicker, glitch, cursor, boot log)

**Files:**
- Create: `fx/FxSettings.kt`, `ui/GlitchFrame.kt`, `ui/EffectsOverlay.kt`, `ui/BootLogOverlay.kt`, `ui/ReduceMotion.kt`
- Modify: `data/Prefs.kt` (fx), `LauncherViewModel.kt` (`fx` command, `fx` in `LauncherUi`), `ui/PixelClock.kt` (glitch), `ui/PromptBar.kt` (blinking cursor), `ui/PagerHome.kt`/`MainActivity.kt` (overlays, boot trigger)
- Test: `fx/FxSettingsTest.kt`, `ui/GlitchFrameTest.kt`

**Interfaces:**
- Consumes: `Theme.cyber` (Task 1), `Command.Fx` (Task 3).
- Produces: `data class FxSettings(val scanlines: Boolean? = null, val flicker: Boolean = false, val glitch: Boolean = true, val cursor: Boolean = true, val boot: Boolean = true)` with `fun with(name: String, on: Boolean): Result<FxSettings>` (name case-insensitive; failure message `fx: <name>: no such effect (scanlines flicker glitch cursor boot)`), `fun scanlinesOn(theme: Theme): Boolean`, `fun describe(theme: Theme): String` (one `name on|off` line per effect); `fun glitchFrame(digits: List<Int>, progress: Float, seed: Int): List<Int>`; `@Composable fun rememberReduceMotion(): Boolean`; `@Composable fun EffectsOverlay(settings: FxSettings, theme: Theme, reduceMotion: Boolean)`; `@Composable fun BootLogOverlay(onDone: () -> Unit)`; `PixelClock(time, modifier, glitch: Boolean = false)`.

- [ ] **Step 1: Write the failing tests.** `FxSettingsTest`: defaults exactly as in Global Constraints; `with("glitch", false)` turns glitch off and leaves others; `with("SCANLINES", true)` sets scanlines; `with("nope", true)` fails with the message above; `scanlinesOn` returns the explicit value when set and `theme.cyber` when `null`; `describe` mentions every effect name. `GlitchFrameTest`: `progress = 1f` and `2f` return the real digits; `progress = 0f` returns 4 values all in 0..9 and the same list for the same seed; at `progress = 0.45f` digits 0 and 1 equal the real ones (locked) while across seeds 0..50 at least one seed makes digit 3 differ from the real one; negative and `Float.NaN` progress behave like 0 (no crash, 4 values in 0..9).
- [ ] **Step 2: Run** `--tests '*FxSettingsTest' --tests '*GlitchFrameTest'`. Expected: FAIL.
- [ ] **Step 3: Implement** both pure units (glitch: digit `i` is real when `progress >= (i + 1) / 5f`, otherwise `Random(seed + i).nextInt(10)`), then the Android layers: Prefs stores each fx value (missing/corrupt -> defaults); the ViewModel applies `Command.Fx` (bare `fx` -> `describe`, missing/invalid value -> `usage: fx <name> on|off`, otherwise `with(...)`) and exposes `fx` in `LauncherUi`; `EffectsOverlay` draws scanlines (a line every 3 dp at about 12% alpha) plus a radial vignette when `scanlinesOn`, and the 0.97-1.0 alpha flicker at random 2-6 s intervals when `flicker` and not reduce-motion, without intercepting touches; the clock runs a 400 ms scramble with a red/cyan offset when the minute changes (via `glitchFrame`, seed from the time) only if `glitch` and not reduce-motion; the prompt shows a blinking block cursor (about 530 ms) if `cursor`; `BootLogOverlay` types `bootLog` for about 1 s on `ACTION_USER_PRESENT` when `boot` and not reduce-motion, ends on timeout or tap and is cancelled on pause. `rememberReduceMotion` reads `Settings.Global.ANIMATOR_DURATION_SCALE == 0f`.
- [ ] **Step 4: Run** the two test classes, then `assembleDebug testDebugUnitTest`. Expected: PASS, BUILD SUCCESSFUL.
- [ ] **Step 5: Commit** `terminal-launcher: crt and glitch effects`.

### Task 6: Screenshots, README and final verification

**Files:**
- Modify: `app/src/test/kotlin/dev/linkinxp/termlauncher/screens/ScreenshotsTest.kt`, `README.md`

- [ ] **Step 1:** Extend `ScreenshotsTest` to render the home screen for all 9 themes, plus: the `out` panel showing `neofetch` output, scanlines on in `neon-city`, and the sys panel with sample stats. Run `./gradlew --no-daemon recordRoborazziDebug` (PNGs in `app/build/shots`). Expected: BUILD SUCCESSFUL and one PNG per case.
- [ ] **Step 2:** Look at the PNGs; fix any visible layout bug (text clipped, overlapping panels, unreadable contrast) and re-render until clean.
- [ ] **Step 3:** Update `README.md` (new commands, `fx` names and defaults, themes list, reduce-motion note, new permission). Bump `versionName` to `0.2.0`.
- [ ] **Step 4:** Run the whole suite from clean: `./gradlew --no-daemon clean assembleDebug testDebugUnitTest`. Expected: BUILD SUCCESSFUL, 0 failures.
- [ ] **Step 5:** Commit `terminal-launcher: cyberpunk round screenshots and docs`.
