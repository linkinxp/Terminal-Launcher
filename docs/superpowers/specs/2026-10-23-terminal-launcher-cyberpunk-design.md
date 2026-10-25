# Terminal Launcher: cyberpunk round design

Date: 2026-10-23. Status: draft for user review. Extends `2026-10-23-terminal-launcher-design.md` (v1); where the two disagree, this document wins.

## Goal
Make the launcher more geeky/techy/cyberpunk: neon themes, CRT and glitch effects, a live system panel with ASCII bars and a boot log, and a smarter prompt (history, tab-complete, fun commands). Personal use on the user's Pixel 10 Pro. Design approved in chat 2026-10-23.

## Changes to v1 rules
- Themes: 9 instead of 5 (the 5 existing + `neon-city`, `synthwave`, `blade-runner`, `glitch-red`). Border styles gain `BRACKET` (corner ticks only).
- Permissions: one new normal install-time permission, `ACCESS_NETWORK_STATE` (network type in the sys panel). Nothing else.
- Folder names may not collide with the new command words either: reserved words are `ls theme pin hide mkdir mv rmdir help fx history neofetch whoami uptime date fortune clear sudo matrix`.

## 1. Neon themes and status line
New themes (ARGB, exact values):
| name | background | foreground | accent | dim | border | glow | cyber |
|---|---|---|---|---|---|---|---|
| neon-city | 0xFF0B0014 | 0xFF00F0FF | 0xFFFF2BD6 | 0xFF7A3E8C | BRACKET | yes | yes |
| synthwave | 0xFF1A0B2E | 0xFFFFE6FF | 0xFFFF6AC1 | 0xFF8F6BD1 | ROUNDED | yes | yes |
| blade-runner | 0xFF04090B | 0xFF6FE7D2 | 0xFFFFA630 | 0xFF3F7F76 | DOUBLE | yes | yes |
| glitch-red | 0xFF000000 | 0xFFFF3B3B | 0xFFFFFFFF | 0xFF8C1F1F | BRACKET | yes | yes |

`Theme` gains `val cyber: Boolean = false` (existing five stay false). `TermPanel` draws `BRACKET` as short corner ticks (about 12 dp) instead of a full outline.

Status line replaces the plain `[1] 2` page dots, above the dock: ` [0] 1:home* 2:two │ bat 66% │ 21:08` (current page marked `*`, pages named `home` for page 1 and the number otherwise; page entries are tappable to switch pages).

## 2. CRT and glitch effects
Toggles, persisted in DataStore, changed with `fx <name> on|off` (bare `fx` lists current values; unknown name or missing/invalid on/off -> usage message):
- `scanlines`: horizontal lines every 3 dp at about 12% alpha plus a soft radial vignette over the whole home/drawer. Default: on for `cyber` themes, off otherwise, until the user sets it explicitly.
- `flicker`: very subtle alpha pulse (0.97-1.0) at random 2-6 s intervals. Default off.
- `glitch`: when the minute changes, the clock digits scramble for about 400 ms (digits lock in left to right) with a red/cyan channel split. Default on.
- `cursor`: blinking block cursor in the prompt (about 530 ms period). Default on.
- `boot`: about 1 s fake boot log overlay on unlock (`ACTION_USER_PRESENT`), tap to skip. Default on.
Reduce motion: if the system animator duration scale is 0, flicker, glitch scramble, boot log and matrix rain are disabled regardless of settings; scanlines and cursor stay.

## 3. Live system panel
`sys` panel lines: `bat  [######----] 62%` (with `+` when charging), `ram  [####------] 41%`, `disk [#######---] 68%`, `up   3d 4h`, `net  wifi|cell|offline`. Sources: `BatteryManager` sticky intent, `ActivityManager.MemoryInfo`, `StatFs(Environment.getDataDirectory())`, `SystemClock.elapsedRealtime()`, `ConnectivityManager` active-network capabilities. Refreshed with the existing minute tick and on screen-on. Any source that fails shows `?` for that line, never crashes.

## 4. Smarter prompt
- History: the last 50 non-blank submitted lines (consecutive duplicates dropped), persisted. With an empty prompt the last 5 appear as tappable suggestions that put the text back in the prompt; `history` prints them.
- Tab-complete: a `⇥` key at the end of the prompt line completes the input to the longest common prefix of all matching candidates (command words plus lowercase app labels); no match or ambiguous-with-no-extension leaves the text unchanged.
- New commands: `neofetch` (ASCII logo left, info right: device model, Android version, launcher version, uptime, theme, app count, ram %), `whoami` (`user`), `uptime` (as the sys panel), `date` (`fri 23 oct 2026 21:08:14`), `fortune` (random geek quote from a fixed list of at least 15), `clear` (empties the output), `sudo <anything>` (`user is not in the sudoers file. This incident will be reported.`), `matrix` (full-screen rain of green/theme-colored glyphs for 6 s, tap to exit).
- Output: command output shows in a bordered `out` panel above the prompt, newest content last, capped at 12 lines (older lines dropped), monospace so ASCII art aligns; tapping the panel dismisses it. Errors and messages that were one-liners before use the same panel.

## Architecture
New pure (JVM-tested) units, each one purpose:
- `theme/ThemeRegistry` (+4 themes), `Theme.cyber`, `BorderStyle.BRACKET`.
- `sys/Format.kt`: `asciiBar(fraction: Float, width: Int = 10): String` (`[######----]`; clamps to 0..1, NaN -> 0), `formatUptime(ms: Long): String` (`3d 4h`, `4h 12m`, `12m`; negative -> `0m`).
- `prompt/CommandHistory`: `add(line)`, `last(n): List<String>` (newest first), `all()`; max 50.
- `prompt/Completion.kt`: `fun complete(input: String, candidates: List<String>): String`.
- `prompt/CommandParser` extended with `Command.Fx(name: String?, on: Boolean?)`, `History`, `Neofetch`, `Whoami`, `Uptime`, `Date`, `Fortune`, `Clear`, `Sudo(rest: String)`, `Matrix`. A command word without required arguments still parses as `Query`.
- `fx/FxSettings`: `data class FxSettings(scanlines: Boolean?, flicker: Boolean, glitch: Boolean, cursor: Boolean, boot: Boolean)`, `fun with(name: String, on: Boolean): Result<FxSettings>`, `fun scanlinesOn(theme: Theme): Boolean`.
- `ui/GlitchFrame.kt`: `fun glitchFrame(digits: List<Int>, progress: Float, seed: Int): List<Int>` (progress 0..1; digit i shows the real value once `progress >= (i + 1) / 5f`, others seeded-random 0-9; `progress >= 1` returns the real digits).
- `prompt/Fortunes.kt`: `val fortunes: List<String>`, `fun fortune(seed: Int): String`; `sys/BootLog.kt`: `val bootLog: List<String>`; `prompt/Neofetch.kt`: `data class DeviceInfo(...)`, `fun neofetch(info: DeviceInfo): String`.
Android/UI layers (build + JVM screenshots + on-device check): `sys/SystemStats` (reads the sources above), `ui/StatusLine`, `ui/SysPanel`, `ui/EffectsOverlay` (scanlines, vignette, flicker), `ui/MatrixRain`, `ui/BootLogOverlay`, `ui/OutputPanel`, clock glitch in `PixelClock`, cursor + `⇥` + history suggestions in `PromptBar`; `Prefs` gains fx settings and history; `LauncherViewModel` gains output lines, history and the new commands.

## Error handling
- Unknown `fx` name: `fx: <name>: no such effect (scanlines flicker glitch cursor boot)`; invalid value: `usage: fx <name> on|off`.
- Corrupt stored fx/history: defaults / empty history; never blocks startup.
- `neofetch`/sys fields that cannot be read print `?`.
- Overlays (boot, matrix) always end: timeout plus tap, and are cancelled if the app is paused.

## Testing
- JVM unit tests: new themes (names, exact colors of defaults, unique), `asciiBar`, `formatUptime`, `CommandHistory`, `complete`, parser additions (including reserved words and no-arg fallbacks), `FxSettings.with` and `scanlinesOn`, `glitchFrame` (deterministic for a seed, locked digits, final frame equals real digits), `fortune` list/seed, `neofetch` contains every field and stays within 40 columns, folder `mkdir` rejects the new reserved words.
- JVM screenshots (Roborazzi, already set up): all 9 themes' home, status line, sys panel, `out` panel with neofetch and help, scanlines on.
- On device (after pairing): glitch, flicker, boot log, matrix rain, tab key and history, battery/ram/net values.

## Out of scope
Weather/calendar/media/notifications/toggles/Garmin panels, gestures, theme editor, sound.
