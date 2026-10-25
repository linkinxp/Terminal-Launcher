# Terminal Launcher: design

Date: 2026-10-23. Status: draft for user review.

## Goal
A personal home-screen launcher for the user's Pixel 10 Pro (Android 17, API 37) with a terminal look: monospace type, box-drawing panels with titles cut into the border, a pixel-style clock, and switchable themes (OLED black, techy, geeky). Inspired by a "Classy black" terminal launcher screenshot. Personal daily driver only, not for the Play Store.

## Scope
**v1 (this spec):**
- HOME launcher (`CATEGORY_HOME`, `CATEGORY_DEFAULT`), installed alongside the stock launcher, selected in Settings > Default apps.
- Home screen: large pixel clock; date line with week number and day-of-year; small system panel (battery, time; wifi/storage optional); dock row of text tabs for 4-5 favorites; prompt line `user@phone:~$` at the bottom.
- Prompt: fuzzy-matches installed apps while typing; Enter launches the top match; tapping a suggestion launches it.
- App drawer: swipe up for a plain text table of all apps with a filter field, plus: alphabet fast-scroll index along the edge, sort options (A-Z, recently installed, most used), a "recent" section at the top, a hidden-apps list (`ls -a`), and an icons toggle (text only, mono/ASCII glyph, or real icons).
- Home screen pages: swipe left/right between pages. Page 1 is the clock + dock layout above; further pages (add/remove/reorder from an edit mode, up to 5) hold app shortcuts as text rows and panels. Page indicator shown as `[1] 2 3`.
- Folders: named groups of apps shown like directories (`~/games`, `~/work`). Create/rename/delete from the prompt (`mkdir`, `mv`, `rmdir`) or long-press; add an app with `mv <app> <folder>`. A folder can sit on a page, in the dock, or in the drawer, and opens as a bordered panel listing its apps.
- Long-press an app: menu with pin to dock, add to folder, hide, app info, uninstall.
- Themes: data-driven, 5 shipped, persisted.
- Prompt commands: `theme <name>`, `pin <app>`, `hide <app>`, `mkdir/mv/rmdir` for folders, `ls` (drawer), `ls -a` (hidden), `help`; any other text is an app query.

**Out of scope for v1 (later rounds):** weather, calendar/agenda, media controls, notification feed, quick toggles, Garmin panel, widget hosting, icon packs, gestures beyond swipe-up/left/right (double-tap lock, shade, custom swipes). No network access and no special permissions in v1 (only a `<queries>` launcher-intent entry to list apps, and `REQUEST_DELETE_PACKAGES` for uninstall).

## Architecture
Single-module Kotlin app, Jetpack Compose UI, new project `terminal-launcher` in `/root/projects` on CT138 (`ssh android-dev`), package `dev.linkinxp.termlauncher`. Same toolchain as the existing projects: JDK 17, AGP 9.4.1, Kotlin 2.4.21, compile/targetSdk 36, minSdk 31, built with `./gradlew --no-daemon assembleDebug`. State lives in DataStore (theme, pinned, hidden).

Units, each with one purpose:
- `apps/AppRepository`: lists launchable activities via `PackageManager`, exposes a `Flow<List<AppEntry>>`, refreshes on package add/remove broadcasts. Depends on Android only.
- `apps/FuzzyMatcher`: pure function, query + candidates -> ranked matches (prefix > word-start > subsequence). No Android deps; unit tested.
- `prompt/CommandParser`: pure; turns prompt text into `Command.Theme | Pin | Hide | Help | Query`. Unit tested.
- `theme/Theme` + `ThemeRegistry`: data class (background, foreground, accent, dim, border style single/double/rounded/ascii, font) and the 5 built-ins; `Theme` is provided through a `CompositionLocal`. Unit tested for name lookup.
- `ui/TermPanel`: bordered box composable with title(s) on the top edge, drawn from the theme's border style.
- `ui/PixelClock`: large blocky clock glyphs (bitmap font drawn on Canvas).
- `ui/HomeScreen`, `ui/AppDrawer`, `ui/PromptBar`, `ui/Dock`: compose the above.
- `layout/HomeLayout`: pure model of pages -> items (app shortcut | folder | panel); add/remove/move operations, folder membership rules (an app lives in at most one folder). Unit tested.
- `apps/AppStats`: launch counts and last-used timestamps recorded by the launcher itself (no usage-access permission) for the "most used" sort and the recent section.
- `ui/PagerHome`, `ui/FolderPanel`, `ui/AlphaIndex`, `ui/EditMode`: pager, folder view, fast-scroll index and layout editing.
- `data/Prefs`: DataStore wrapper for theme, `HomeLayout` (serialized), hidden set, drawer sort/icon mode, `AppStats`.

Data flow: `AppRepository` + `Prefs` -> `LauncherViewModel` (combines into UI state) -> composables; `PromptBar` text -> `CommandParser` -> either a prefs mutation or a `FuzzyMatcher` query over visible apps.

## Themes (v1)
classy-black (pure #000, white text, blue accent/glow), matrix (green on black), amber (CRT amber on black), solarized-dark, dracula/synthwave. Switch via `theme <name>` or a minimal settings screen. Adding a theme = one entry in `ThemeRegistry`.

## Error handling
- App launch failure (uninstalled, disabled): toast `exec: <name>: not found`, refresh list.
- Unknown prompt command with no app match: show `no match: <text>` in the suggestion area; never crash.
- Missing/corrupt prefs: fall back to classy-black, no pins, nothing hidden.
- Uninstall goes through the system confirmation dialog; uninstalled apps are pruned from pages, folders and the dock on the next refresh.
- Corrupt or unparsable layout data resets to a single default page rather than crashing.

## Testing
- JVM unit tests: `FuzzyMatcher`, `CommandParser`, `ThemeRegistry`, `HomeLayout` (folder rules, page ops, removal of uninstalled apps), drawer sort/index grouping.
- Compose previews for each theme (build-only check).
- Manual on the phone: debug APK installed over adb Wi-Fi; verify home, pages, folders, drawer (index, sorts, hidden), prompt launch, pin/hide, theme switch, rotation/back behavior, and that the stock launcher can be restored from Settings.
- No emulator (user's choice); visual checks are done by the user on the device.

## Rollback
It is a separate app. Switching the default home app back in Settings, or `adb uninstall dev.linkinxp.termlauncher`, restores the previous state.
