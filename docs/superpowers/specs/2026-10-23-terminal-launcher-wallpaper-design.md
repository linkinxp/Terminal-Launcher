# Terminal Launcher: wallpaper round design

Date: 2026-10-23. Status: draft for user review. Extends `2026-10-23-terminal-launcher-design.md` and `2026-10-23-terminal-launcher-cyberpunk-design.md`; where they disagree, this document wins. Design approved in chat 2026-10-23.

## Goal
Let the launcher show a wallpaper behind the home screen and the drawer: either the phone's own system wallpaper (so Pixel 10 wallpapers work) or generated, seeded ASCII art drawn in the theme's colors. Personal use on the user's Pixel 10 Pro. The default look does not change.

## Scope
- Background modes, switched with `wall mode solid|system|ascii`:
  - `solid` (default): the opaque theme background, exactly as today.
  - `system`: the launcher window is transparent so the system wallpaper shows through; a dim layer in the theme's background color keeps text readable.
  - `ascii`: generated ASCII art drawn behind everything in the theme's `dim` color at low contrast.
- ASCII generator with six styles: `glyph` (character field), `circuit` (board traces), `contour` (terrain lines), `stars` (starfield), `hexdump` (memory dump), `skyline` (city silhouette).
- Prompt commands (all persisted): `wall` (shows current settings), `wall random` (new style and seed), `wall next` (next style), `wall style <name>`, `wall seed <n>`, `wall mode <solid|system|ascii>`, `wall dim <0-100>`, `wall animate on|off`.
- Defaults: mode `solid`, style `glyph`, seed `1`, dim `55`, animate off.
- Out of scope: setting the art as the real home/lock screen wallpaper, picking a photo, animation on by default, reading wallpaper files from the Pixel app (not possible for a third-party app).

## Behavior details
- `dim` applies in `system` mode (alpha of a theme-background layer over the wallpaper, 0-100 -> 0.0-1.0) and in the drawer in all non-solid modes as an extra readability layer of fixed 35% alpha on top of that. Values outside 0-100 are rejected with `usage: wall dim <0-100>`.
- `wall seed` takes an integer (any `Int`); non-numbers are rejected with `usage: wall seed <number>`. `wall random` picks a random style and a random seed.
- The art depends only on style, seed and grid size: the same inputs always give the same picture. Grid size is derived from the screen: about 6.6 dp per column and 13 dp per row at 11 sp monospace text.
- Animation (`wall animate on`): the art scrolls up one row about every 250 ms (rows wrap around) while the launcher is resumed. It is disabled under reduce motion (animator scale 0) and costs nothing when off. Static art is composed once per size/seed/style.
- The drawer sits over the home screen; while the drawer is open the home content is kept composed but invisible, so the wallpaper/art shows in the drawer without losing the home page position.
- In `system` and `ascii` modes the scanlines/vignette from `fx` still draw on top.
- A `ScreenshotsTest` stand-in renders `system` mode over a checkerboard because the JVM has no system wallpaper.

## Architecture
New pure (JVM-tested) units, one purpose each:
- `wall/AsciiArt.kt`: `enum class AsciiStyle { GLYPH, CIRCUIT, CONTOUR, STARS, HEXDUMP, SKYLINE }`; `fun generateAscii(style: AsciiStyle, cols: Int, rows: Int, seed: Int): List<String>` returns exactly `rows` strings of exactly `cols` printable ASCII characters (0x20-0x7E); non-positive `cols` or `rows` give an empty list or empty strings, never an exception; deterministic for equal inputs; `fun driftLines(lines: List<String>, step: Int): List<String>` rotates rows upward by `step` (negative or larger than size wraps).
- `wall/WallSettings.kt`: `enum class WallMode { SOLID, SYSTEM, ASCII }`; `data class WallSettings(val mode: WallMode = SOLID, val style: AsciiStyle = GLYPH, val seed: Int = 1, val dim: Int = 55, val animate: Boolean = false)`; `fun applyWall(current: WallSettings, sub: String?, arg: String?, random: kotlin.random.Random): Result<WallSettings>` (sub/arg case-insensitive; unknown sub, bad value or missing argument fail with a usage message and leave settings unchanged); `fun WallSettings.describe(): String` (lines `mode`, `style`, `seed`, `dim`, `anim`).
- `prompt/CommandParser`: `Command.Wall(sub: String?, arg: String?)` (`wall` -> both null; extra tokens beyond the first argument are ignored); `wall` joins the reserved words (cannot be a folder name) and the help text and tab-completion list.
Android/UI layers (build + JVM screenshots + on-device check): `Prefs` stores the wall settings (corrupt/missing -> defaults); `LauncherUi.wall`; `ui/WallBackground.kt` (solid fill, dim layer over the transparent window, or the cached ASCII art; replaces the solid fill in `TermTheme`); manifest/theme `Theme.Launcher` with `windowShowWallpaper` and a transparent window background (solid mode paints its own opaque fill, so nothing changes visually); `PagerHome` keeps composing under an open drawer with alpha 0; `AppDrawer` root becomes transparent over the wall layer plus its readability layer.

## Error handling
- Unknown subcommand: `wall: <x>: unknown (random next style seed mode dim animate)`; invalid style name lists the six styles; invalid mode lists `solid system ascii`.
- Corrupt stored values fall back field by field to the defaults; the launcher always starts.
- If the grid computes to 0 columns or rows (tiny window), nothing is drawn.

## Testing
- JVM unit tests: `generateAscii` (exact dimensions for several sizes, determinism, different seeds differ, printable ASCII only, zero/negative sizes safe, per-style property: `stars` is at least 80% spaces, `glyph` at least 60% non-space, `skyline` bottom row at least 50% non-space, `hexdump` lines contain hex digits and a colon), `driftLines` (wrap, negative, empty), `applyWall` (every subcommand, dim bounds 0 and 100 ok / -1 and 101 rejected, case-insensitive, unknown sub/mode/style, missing arg, settings unchanged on failure, `random` is deterministic for a seeded `Random` and always changes style or seed), `describe`, parser (`wall`, `wall mode ascii`, `WALL Dim 40`, extra tokens), reserved word.
- JVM screenshots: all six ASCII styles in two themes, `system` mode over a checkerboard, the drawer over `ascii` mode.
- On device (after pairing): the real system wallpaper showing through in `system` mode (including a Pixel 10 wallpaper), dim level legibility, animation smoothness and battery, rotation, that `solid` mode still looks identical to before.
