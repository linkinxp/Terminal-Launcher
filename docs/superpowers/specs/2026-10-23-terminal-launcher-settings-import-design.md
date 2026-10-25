# Terminal Launcher: settings, backup and import round design

Date: 2026-10-23. Status: draft for user review. Extends the earlier specs in this folder; where they disagree, this document wins. Design approved in chat 2026-10-23. Version 0.5.0. Built in this order, one commit per part: (1) settings screen, (2) own backup export/import, (3) Nova/Lawnchair backup import.

## Goal
One place to see and change every launcher setting (themes, wallpaper, effects, panels, drawer), a way to save and restore the layout and settings, and a way to bring a layout over from another launcher. Personal use on the user's Pixel 10 Pro.

## Findings that shape the scope
- Importing from Pixel Launcher is not possible for any app: its layout provider requires `ACCESS_LAUNCHER_DATA` (system-only). Verified 2026-10-23 with a read-only adb query, which got a permission denial.
- Launcher3-family launchers that offer a backup file (Nova, Lawnchair) keep the layout in a SQLite `favorites` table; that is importable from a file the user picks. The exact backup file structure is not verified here (no sample file): the reader accepts a ZIP that contains a file named `launcher.db` (at any depth) or a raw SQLite file with a `favorites` table, and says so clearly when it finds neither. The user tests it with their own backup.

## 1. Settings screen
- Opens with the `settings` command (new reserved word) and a `[settings]` link at the bottom of edit mode; full-screen like the drawer; Back and the Home press close it. `help` lists `settings`.
- Sections, each a `TermPanel` (all controls change settings immediately and use the same setters/pure functions as the prompt commands):
  - `theme`: one row per theme (9), name plus four color chips (background, foreground, accent, dim), a `*` on the current one; tapping applies it at once.
  - `wallpaper`: `mode` (tap cycles solid, system, ascii), `style` (tap cycles), `seed` with a `[random]` button, `dim` with `[-]` `[+]` in steps of 5 (0-100), `animate` toggle.
  - `effects`: `scanlines` (tap cycles `auto (theme)`, `on`, `off`), and toggles for `flicker`, `glitch`, `cursor`, `boot`.
  - `panels`: `cal` and `wx` toggles, `city` shown with a text field and `[set]` (same behavior as `wx city <name>`), `units` toggle C/F.
  - `drawer`: `sort` (tap cycles) and `icons` (tap cycles).
  - `backup`: `[export layout]`, `[import layout]`, `[import nova/lawnchair]` (parts 2 and 3).
  - `about`: version, and a line `back to your old launcher: Settings > Apps > Default apps > Home app`.
- Small prompt changes: a bare `theme` lists the themes (current marked with `*`) instead of acting as an app search; `fx scanlines auto` returns scanlines to "follow the theme" (resolves the earlier deferred gap): `Command.Fx(name: String?, on: Boolean?, auto: Boolean = false)`, `FxSettings.withScanlines(value: Boolean?): FxSettings`.

## 2. Own backup (export / import)
- Export writes `terminal-launcher-backup.json` through the system file creator (`ActivityResultContracts.CreateDocument("application/json")`); import reads through `OpenDocument`. No storage permission.
- Content (`LauncherBackup`): `format` (`"terminal-launcher-backup"`), `version` (1), layout (pages, dock, folders; widgets are NOT exported because their host ids mean nothing on another install), hidden app packages, theme name, fx, wall, panels, wx (city, lat, lon, units), drawer sort and icon mode.
- Pure units: `data class LauncherBackup(...)`; `fun encodeBackup(b: LauncherBackup): String`; `fun decodeBackup(json: String?): Result<LauncherBackup>` (null/garbage/wrong `format`/newer `version` fail with a clear message; missing sections fall back to defaults; unknown fields ignored); `fun HomeLayout.withoutWidgets(): HomeLayout`; `fun describeBackup(b: LauncherBackup, installed: Set<String>): List<String>` (preview lines: pages, dock items, folders, apps not installed here, theme).
- Import shows the preview in a dialog with `[replace]` / `[cancel]`; `[replace]` overwrites layout and settings atomically-ish (one DataStore edit per area) and keeps the current widgets only if the new layout has none of its own (widgets are never in a backup, so they are removed with their host ids by the existing cleanup).

## 3. Import from a Nova/Lawnchair backup
- The user picks a file; the reader copies it to the cache directory, opens the SQLite database read-only and queries `favorites` (`_id, itemType, container, screen, cellX, cellY, title, intent`); any missing column, missing table or unreadable file gives `import: no launcher layout found in this file`; the cache copy is deleted afterwards.
- Pure mapping: `data class FavRow(val id: Int, val itemType: Int, val container: Int, val screen: Int, val cellX: Int, val cellY: Int, val title: String?, val intent: String?)`; `data class ImportedLayout(val layout: HomeLayout, val apps: Int, val folders: Int, val skippedWidgets: Int, val missingApps: Int)`; `fun packageFromIntent(intent: String?): String?` (reads `component=<pkg>/<cls>` from an Android intent URI, falling back to `package=<pkg>`); `fun mapFavorites(rows: List<FavRow>, installed: Set<String>): ImportedLayout`.
  - `itemType` 0 = app, 2 = folder, 4 = widget (skipped and counted), anything else is skipped; `container` -100 = desktop, -101 = hotseat, a folder's `_id` = inside that folder.
  - Desktop screens, sorted, become pages in order (screens that are numerically sparse still become consecutive pages); at most 5 pages: items of any screen beyond the fifth are appended to the fifth page. Within a page items are ordered by `cellY` then `cellX`.
  - Hotseat items go to the dock ordered by `cellX`, at most 5 (extra ones are placed on page 1).
  - Folders become `Item.Folder`; the name is cleaned to our rules (whitespace -> `-`, `/` removed, reserved words get `_` appended, empty -> `folder`, at most 24 characters, made unique with a numeric suffix); apps inside are listed once, in cell order; apps already inside a folder are not also placed on a page.
  - Packages not installed here are dropped and counted in `missingApps`; the same package is never placed twice (first occurrence wins).
- The preview dialog shows pages, dock items, folders, apps found, apps missing here and widgets skipped, with `[replace]` / `[cancel]`; `[replace]` swaps the layout (pages + dock + folders) and leaves all other settings alone.

## Architecture
Pure, JVM-tested: `backup/LauncherBackup.kt` (`LauncherBackup`, `encodeBackup`, `decodeBackup`, `describeBackup`), `layout/HomeLayout.kt` (`withoutWidgets`), `importer/FavoritesMapper.kt` (`FavRow`, `ImportedLayout`, `packageFromIntent`, `mapFavorites`), `fx/FxSettings.kt` (`withScanlines`), parser (`settings`, `fx scanlines auto`, bare `theme`), `theme/ThemeRegistry` (listing helper `describeThemes(current: String): String`). Android layers (build, JVM screenshots/Robolectric, on-device): `ui/SettingsScreen.kt` (+ small row components), `backup/BackupRepository.kt` (reads/writes the document URIs, builds a `LauncherBackup` from the stores, applies one), `importer/FavoritesReader.kt` (ZIP/raw SQLite reading into `FavRow`s), `ui/ImportDialog.kt`, `MainActivity` (document launchers, settings overlay state), ViewModel methods for each setter the screen needs (`setTheme`, `setFxValue`, `setWallSettings`, `setPanelFlag`, `setDrawerSort`...), `Prefs` helpers for applying a backup.

## Error handling
- Backup/import file problems never crash: the dialog or output panel shows one clear line (`import: not a launcher backup`, `import: backup is from a newer version`, `import: no launcher layout found in this file`, `import: could not read that file`).
- A cancelled file picker does nothing. A partially valid backup applies only the valid sections.
- Replacing the layout while widgets exist removes their host ids through the existing cleanup.

## Testing
- JVM unit tests: `mapFavorites` (sample rows: two desktop screens, sparse screen numbers, 7 screens -> 5 pages with the rest on page 5, hotseat with 6 items, folders with 3 apps, a folder-contained app not duplicated, a widget skipped and counted, uninstalled apps dropped and counted, duplicate packages, odd folder titles incl. empty, reserved, with slash, 40 characters, two identically named folders, empty input), `packageFromIntent` (component form, package form, malformed, null), `encodeBackup`/`decodeBackup` (round trip, widgets excluded, garbage, wrong format, newer version, missing sections default, unknown fields ignored), `describeBackup`, `withoutWidgets`, `withScanlines` and the parser (`fx scanlines auto`, `settings`, bare `theme`), `describeThemes`.
- Robolectric tests: `FavoritesReader` on a generated SQLite file and on a ZIP containing `launcher.db`, a file without `favorites`, a non-SQLite file; the settings screen renders (screenshots for two themes) and each control changes the matching setting.
- On device (user): the real Nova/Lawnchair backup file, export then import round trip, theme tap-to-apply, the file pickers.

## Out of scope
Importing from Pixel Launcher (impossible), carrying widgets across, cloud backup, merging an import into the current layout, icon packs, a custom theme editor.
