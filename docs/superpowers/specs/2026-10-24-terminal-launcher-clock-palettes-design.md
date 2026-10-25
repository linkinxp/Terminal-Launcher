# Terminal Launcher: clock styles, palettes and custom themes

Date: 2026-10-24. Status: draft for user review. Extends the earlier specs in this folder; where they disagree, this document wins. Direction chosen by the user in chat on 2026-10-24 ("Clock and palettes"). Version 0.6.0. Built in this order, one commit per part: (1) 12/24-hour clock format (requested earlier), (2) clock styles, (3) popular palettes, (4) custom themes.

## Goal
Make the home screen feel more like a hacker's terminal: the clock can look like a FIGlet banner, a 7-segment display, a binary or hex clock; the theme list gains the palettes people actually use in their terminals; and the user can define their own theme from four hex colors.

## 1. Clock format (12 / 24 hour)
- `ClockFormat { AUTO, H24, H12 }`, default `AUTO` (follows the phone's own 12/24-hour setting, `DateFormat.is24HourFormat`).
- Command `clock auto|24|12` (accepts `24h`/`12h`, any case); bare `clock` shows the current value; anything else prints `usage: clock auto|24|12`. Settings row `clock` cycles auto -> 24h -> 12h. Saved, included in backups (`clockFormat`, default `AUTO`).
- Applies to the clock in every style that shows hours, the status line time (`9:08pm`), calendar event times (`10:05am`), and the `date` command (`hh:mm:ss a`). In 12-hour mode the hour has no leading zero (blank first digit on the pixel clock) and the date line gets a trailing `am`/`pm`; midnight is `12:xx am`, noon `12:xx pm`.
- Pure units: `clockDigits(hour, minute, h24: Boolean = true): List<Int>` (the blank leading digit is `BLANK = -1`), `amPm(hour): String`, `formatClockTime(hour, minute, h24): String`, `parseClockFormat(arg: String?): Result<ClockFormat>`, `ClockFormat.is24(systemIs24: Boolean)`, `ClockFormat.fromName(name: String?)` (unknown -> AUTO); `statusTail(..., h24: Boolean = true)`, `formatAgenda(..., h24: Boolean = true)`.

## 2. Clock styles
- `ClockStyle { PIXEL, SEGMENT, FIGLET, BINARY, HEX, EPOCH, PLAIN }`, default `PIXEL` (today's look). Command `clock style <name>`; settings row `clock style` cycles; saved and in backups (`clockStyle`).
- `PIXEL`: unchanged blocky clock with glow. `SEGMENT`: 7-segment digits drawn as bars on the same canvas (same glitch/glow behavior). `FIGLET`: three-line big ASCII digits (a built-in 3x5-ish font made of `_ | /` and spaces) as monospace text, theme foreground, soft glow. `BINARY`: four columns (H tens, H units, M tens, M units) of `●`/`○` bits (3, 4, 3, 4 rows, least significant at the bottom) with a tiny decimal caption under it. `HEX`: `0x15:1D` (hour and minute in hex, uppercase), large monospace text. `EPOCH`: unix time in seconds as large monospace text (`1761283749`), refreshed every second only while this style is shown and the launcher is resumed. `PLAIN`: `21:29` in big monospace text.
- All styles respect reduce motion (no glitch scramble; EPOCH updates once per minute instead of per second) and the 12/24 setting (BINARY, HEX, EPOCH use 24-hour values and ignore it; PIXEL, SEGMENT, FIGLET, PLAIN follow it).
- Pure units: `figletLines(digits: List<Int>): List<String>` (exactly 3 lines, each digit 4 columns wide + 1 space, blank digit = 4 spaces, colon not used), `binaryColumns(hour, minute): List<List<Boolean>>` (4 columns of 3, 4, 3, 4 bits), `hexTime(hour, minute): String`, `epochText(epochSeconds: Long): String`, `segmentMask(digit: Int): Int` (7-bit mask of lit segments, `BLANK` -> 0), `ClockStyle.fromName(name: String?)`, `parseClockStyle(arg: String?): Result<ClockStyle>` (usage `usage: clock style pixel|segment|figlet|binary|hex|epoch|plain`).
- The prompt command is `clock` with a sub-word: `clock` (shows format and style), `clock 12` / `clock auto|24` (format), `clock style <name>`. Parsed as `Command.Clock(sub: String?, arg: String?)` (rest of the line).

## 3. Popular palettes (6 new built-in themes)
All new themes use `cyber = false` and `glow = false`. Exact values (ARGB):
| name | background | foreground | accent | dim | border |
|---|---|---|---|---|---|
| gruvbox | 0xFF282828 | 0xFFEBDBB2 | 0xFFFE8019 | 0xFF928374 | SINGLE |
| nord | 0xFF2E3440 | 0xFFD8DEE9 | 0xFF88C0D0 | 0xFF4C566A | ROUNDED |
| tokyo-night | 0xFF1A1B26 | 0xFFC0CAF5 | 0xFF7AA2F7 | 0xFF565F89 | ROUNDED |
| catppuccin | 0xFF1E1E2E | 0xFFCDD6F4 | 0xFFCBA6F7 | 0xFF6C7086 | ROUNDED |
| monokai | 0xFF272822 | 0xFFF8F8F2 | 0xFFA6E22E | 0xFF75715E | DOUBLE |
| one-dark | 0xFF282C34 | 0xFFABB2BF | 0xFF61AFEF | 0xFF5C6370 | SINGLE |
The registry order becomes the nine existing themes followed by these six (15 built-in). The reserved folder words are unchanged. `describeThemes` and the settings theme list include them.

## 4. Custom themes (hex colors)
- Command `theme add <name> <bg> <fg> <accent> <dim> [single|double|rounded|ascii|bracket]`, colors as `#RRGGBB` (or `RRGGBB`); border defaults to `single`. `theme rm <name>` removes a custom theme (built-ins cannot be removed). Custom themes are listed by `theme` and in the settings theme list (marked `+`), selectable like any theme, saved, and included in backups (`customThemes`).
- Rules: the name is 1-24 characters, lowercase letters, digits, `-` and `_` only; it may not equal a built-in theme name or a reserved command word or an existing custom theme (case-insensitive; adding an existing custom name replaces it); at most 8 custom themes. Invalid input prints a one-line reason and changes nothing. A warning line `theme: low contrast` is shown (but the theme is still added) when the foreground/background contrast ratio is below 3.0.
- If the active theme is removed, the theme falls back to `classy-black`.
- Pure units: `parseHexColor(text: String): Long?` (`#RGB` and `#RRGGBB`, case-insensitive, optional `#`; result is `0xFF------`), `contrastRatio(a: Long, b: Long): Double` (WCAG relative luminance), `validateCustomTheme(name, existing: List<String>, builtIn: List<String>): String?` (error message or null), `buildCustomTheme(name, colors, border): Theme`, `CustomThemes.encode(list): String` / `decode(raw: String?): List<Theme>` (bad data -> empty list, entries with invalid fields skipped, capped at 8), and `ThemeBook(custom: List<Theme>)` with `all`, `byName(name)`.

## Architecture
Pure, JVM-tested: `clock/ClockFormat.kt`, `clock/ClockStyle.kt`, `clock/ClockText.kt` (figlet/binary/hex/epoch/segment units), `theme/Colors.kt` (parse + contrast), `theme/CustomThemes.kt`, `theme/ThemeBook.kt`, plus parser changes (`Command.Clock`, `theme add/rm`). Android/UI: `ui/ClockView.kt` (dispatches on style, one composable per style, reusing the pixel canvas), `ui/PixelClock.kt` (blank digit support), a new `Prefs` section for clock format, style and custom themes (distinct flows), settings rows, backup fields, ViewModel handling, `LauncherUi` gains `clockFormat`, `clockStyle`, `customThemes`. The existing `ThemeRegistry.all/byName` stay for built-ins; everything that looks a theme up by name goes through `ThemeBook`.

## Error handling
Unknown stored enum names fall back (format AUTO, style PIXEL); corrupt custom-theme data yields an empty list; an active theme that no longer exists falls back to `classy-black`; no input can crash the clock (digits out of range render blank).

## Testing
JVM unit tests for every pure unit above (all digits 0-9 in the figlet font fit 3 lines of equal width, binary bit patterns for known times such as 21:29, hex for 0:00 / 15:30 / 23:59, epoch formatting, segment masks, hex parsing incl. bad input, contrast ratios for known pairs (black/white = 21), custom-theme validation rules and encode/decode round-trips, parser cases, backup round-trips) and Robolectric tests (the settings rows cycle, a custom theme can be added through the command and then selected, removing the active custom theme falls back). JVM screenshots of every clock style in two themes and the six new palettes. On device (user): the look and feel of each style, 12/24 behavior against the phone setting, hex color entry.

## Out of scope
Importing terminal color files (Xresources, base16), per-element color overrides, animated clock transitions, a graphical color picker, seconds on any clock except EPOCH.
