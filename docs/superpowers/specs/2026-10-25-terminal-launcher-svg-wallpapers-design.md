# Terminal Launcher: built-in SVG wallpapers

Date: 2026-10-25. Status: draft for user review. Version 0.8.0. Requested by the user in chat on 2026-10-25 ("other wallpapers, hacker type, geeky, with svg"; "built in is good" = no importing of the user's own SVGs for now). Extends the wallpaper spec; where they disagree, this document wins.

## Goal
Eight crisp, theme-colored vector wallpapers with a hacker/geek flavor, selectable like the existing wallpaper modes, drawn from SVG so they stay sharp on any screen.

## Behavior
- New wall mode `SVG` (after `RAIN`): `wall mode svg`; the settings wallpaper row cycles it. Styles (`SvgStyle`): `CIRCUIT` (PCB traces, vias and chips), `HEXNET` (a hex grid with lit cells and links), `RADAR` (concentric rings, sweep wedge, blips), `TUNNEL` (nested squares fading away with binary digits), `GLITCH` (a skull made of simple shapes cut into shifted horizontal slices), `WINDOWS` (stacked terminal windows with code lines), `HELIX` (a DNA double helix with base-pair rungs), `TOPO` (contour-map rings with elevation labels).
- Commands: `wall svg` shows the style; `wall svg <style>` sets it; `wall svg next` cycles; `wall svg random` picks a style and a new seed; unknown names print `usage: wall svg circuit|hexnet|radar|tunnel|glitch|windows|helix|topo|next|random`. The existing `wall seed`/`[random]` seed applies: the same style, seed, theme colors and screen size always give the same picture. Settings gets a `svg style` row (cycle) next to the existing `style` row; both are saved and in backups (`WallBackup.svgStyle`, default `CIRCUIT`, unknown names fall back to `CIRCUIT`).
- Look: the SVG is generated as text from the theme: background fill, art in `dim`, highlights in `accent`, a few bright details in `foreground`; `dim` (the wall dim setting) lowers the art's opacity like the rain wallpaper (strength (100 - dim)/100, at least 15%). Under the drawer and settings the existing veil rules apply (drawer 0.85 over SVG, settings stay solid).
- Static: no animation, no per-frame work.

## Architecture
- Pure, JVM-tested: `wall/SvgWallpaper.kt`: `enum class SvgStyle`, `fun svgText(style: SvgStyle, seed: Int, colors: SvgColors, width: Int, height: Int): String` (a well-formed SVG 1.1 document with `viewBox="0 0 width height"`, only shapes/paths/text/groups/transforms from the basic set AndroidSVG renders; at most ~1500 elements), `data class SvgColors(background, foreground, accent, dim: Long)`, `applyWall` extended with `svg`.
- Android: dependency `com.caverock:androidsvg-aar:1.4` (Apache 2.0, ~200 KB); `SvgRenderer` renders `svgText` to a screen-sized `ImageBitmap` on `Dispatchers.Default` and caches the last two (key: style, seed, colors, size); `SvgBackground` composable shows the bitmap (placeholder: the theme background) in its own graphics layer; invalid SVG or a renderer failure falls back to the solid background.

## Error handling
Any exception while generating or rendering shows the plain background; width/height of 0 or negative render nothing; the cache keeps at most two bitmaps so memory stays bounded.

## Testing
JVM: every style yields a parseable XML document with the right viewBox, uses each theme color, is deterministic per seed and differs between seeds and between styles, stays under the element cap; `applyWall` svg cases and usage; backup round trip and unknown-name fallback. Robolectric: the renderer draws a non-blank bitmap for every style and a different one for a different theme; the settings row cycles the eight styles and wraps; a screenshot of each style in two themes (look at them and fix problems). On device (user): look and feel.

## Out of scope
Importing the user's own SVGs, animation, per-style tuning settings, bundled static SVG assets.
