# Terminal Launcher: icon redesign ("icons everywhere, keep the terminal soul")

Date: 2026-10-24. Status: draft for user review. Version 0.7.0. Direction chosen by the user in chat on 2026-10-24: icons everywhere while keeping the terminal look; themed monochrome icons; the home page becomes an icon grid. Extends the earlier specs in this folder; where they disagree, this document wins.

## Goal
Replace the all-text app lists with icons (home, dock, quick tiles, drawer, folders, menus) so the launcher is easier to scan and tap, without losing what makes it this launcher: theme colors, the mono font, the prompt bar, panels, themes and wallpapers.

## 1. Themed icon engine (`icons/`)
- Source: `PackageManager.getApplicationIcon(pkg)`. If it is an `AdaptiveIconDrawable` with a monochrome layer (Android 13+ themed icons), that layer is the glyph. Otherwise the full icon is the glyph after a grayscale + tint color matrix (keeps detail; never a flat blob).
- Look: a rounded-square tile (corner radius 28% of the size) filled with the theme's `dim` color at 18% alpha, a 1 px border of `dim` at 40%, and the glyph tinted with the theme's `foreground` (dock and the selected item use `accent`) inside the 66% safe zone.
- Pure units, JVM-tested: `tintMatrix(argb: Long): FloatArray` (grayscale then multiply by the color), `IconKey(pkg, themeKey, sizePx)`, `themeKey(theme)` (changes when any of foreground/dim/accent/background changes), `IconCache` (LRU of 256 bitmaps, thread-safe).
- Android: `IconRepository` renders on `Dispatchers.Default`, returns a cached `ImageBitmap`; `@Composable rememberThemedIcon(pkg, size)` shows an empty tile of the same size until the bitmap is ready (no layout jump). The cache is warmed for the dock, quick tiles and the first home page when the theme or app list changes, and for the first 24 drawer cells once the app is idle after start.
- A package without a launchable icon draws the tile with the first letter of its label.

## 2. Home style `ICONS`
- `HomeStyle` gains `ICONS` (after `GRID`): page content is a 4-column grid of icon cells (icon 56 dp, label below in 11 sp, one line, lowercase, ellipsis), anchored to the bottom like `BOTTOM`/`GRID`; the clock is the one-line compact clock; pages with panels stay top-down with the big clock (the glance page), exactly as `GRID` does.
- Folders are cells whose tile shows the first four apps as a 2x2 of mini icons; tap opens the existing folder panel. Widgets and panels keep the full width.
- `layout icons` = style `ICONS` plus the apps-first restructure (panels to their own glance page). `layout classic|bottom|grid|apps-first` keep their meaning. The settings `layout` row cycles classic, bottom, grid, icons. Existing layouts and styles are unchanged until the user picks one; the backup `homeStyle` accepts `ICONS`.
- Long-press on a cell opens the same item menu as today; the edit-mode long-press on empty space is unchanged.

## 3. Dock and quick tiles
- With style `ICONS` the dock shows up to five icon cells (tile 48 dp, label 10 sp underneath) instead of text tabs; other styles keep the text dock (full names).
- With style `ICONS` the `quick` panel shows its tiles as icon cells (4 per row, label `tileLabel`, 7 chars); other styles keep the bracket text tiles.

## 4. Drawer (style `ICONS`)
- A `LazyVerticalGrid` of 4 columns: a `recent` row, then letter sections with full-span headers (`-- B --`), the A-Z rail on the right as today; searching shows a flat grid, best match first.
- The search field and the `sort` / `[cfg]` controls move to a bottom bar above the keyboard (thumb zone); the keyboard still opens with the drawer; `[x]` is dropped (swipe down on the bar or Back closes). The old `icons:` toggle is not shown in this style.
- Other styles keep today's list drawer; its `icons:ico` mode now uses the themed icons.

## 5. Elsewhere
- Edit mode's add-app list, the item menu header and folder panel rows show themed icons (24 dp) next to the names.
- Prompt bar, status line, panels, settings, themes, wallpapers: unchanged.

## Performance (hard requirements)
- Icon work never runs on the main thread; no icon is decoded or drawn per frame; bitmaps are cached and reused across drawer/home.
- The drawer and home recomposition isolation added earlier stays intact (no whole-`LauncherUi` reads in `MainActivity`'s root scope).
- Measured on the phone before and after with the existing harness: janky frames while opening the drawer must not exceed 5% and p99 must stay under 25 ms; time from swipe to the drawer on screen under 60 ms (keyboard timeline as measured: first visible ~135 ms).

## Error handling
Icon load failures fall back to the letter tile; an unknown stored `homeStyle` falls back to `CLASSIC`; the cache is bounded so a 500-app phone cannot exhaust memory.

## Testing
JVM: tint matrix, icon keys and theme keys, cache eviction, grid chunking, `layout icons` plan, style names. Robolectric: the tile renders a tinted glyph from a fake adaptive icon with a monochrome layer and a letter tile without one; the home page shows one cell per app and tapping launches; the dock and quick tiles show icons in `ICONS` and text in `CLASSIC`; the drawer grid shows sections and the rail and the search results flat; screenshots of home, drawer, dock in three themes. On device (user): look and feel of the tiles and fallback icons, drawer smoothness (harness numbers above).

## Out of scope
Icon packs, per-app icon overrides, icon animations, notification badges, changing the prompt bar or panels, a list/grid toggle for the drawer.
