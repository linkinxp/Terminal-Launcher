# terminal-launcher

A terminal-styled, cyberpunk-flavoured Android home launcher (Kotlin, Jetpack Compose) with themes, home pages,
folders, a prompt (`user@phone:~$`), a live system panel and an app drawer. Personal use, installed alongside the
stock launcher. Version 1.0.2, requires Android 14+ (minSdk 34).

## Build / install (on android-dev)
    ./gradlew --no-daemon assembleDebug testDebugUnitTest
    /opt/android-sdk/platform-tools/adb install -r app/build/outputs/apk/debug/app-debug.apk
Then Settings > Apps > Default apps > Home app > Terminal.
JVM screenshots of the screens (no emulator): `./gradlew --no-daemon recordRoborazziDebug` -> `app/build/shots/*.png`.

## Rollback
Switch the default Home app back in Settings, or `adb uninstall dev.linkinxp.termlauncher`.

## Themes
`classy-black`, `matrix`, `amber`, `solarized-dark`, `dracula`, and the cyber set `neon-city`, `synthwave`,
`blade-runner`, `glitch-red` (scanlines on by default), and the popular terminal palettes `gruvbox`, `nord`, `tokyo-night`,
`catppuccin`, `monokai`, `one-dark`. Switch with `theme <name>`.

### Your own themes
`theme add <name> <bg> <fg> <accent> <dim> [single|double|rounded|ascii|bracket]` with colors as `#RRGGBB` or `#RGB` (the `#` is optional),
e.g. `theme add mine #101010 #e0e0e0 #3d7bff #888888 rounded`. Names are 1-24 characters of `a-z 0-9 - _`, cannot be a built-in theme or
command word, and at most 8 custom themes fit; adding an existing name replaces it. A foreground/background contrast below 3:1 still adds
the theme but prints `theme: low contrast`. `theme rm <name>` removes one (the active theme then falls back to `classy-black`). Custom themes
show in the settings theme list with `+` and `[rm]`, and are part of the backup.

## Home layout
`layout apps-first` splits the home screen in two: page one is an apps page (a one-line clock, then your apps and folders in two columns
anchored to the bottom, next to your thumb) and page two is a glance page (big clock, search, sys, calendar and weather). `layout classic`
puts everything back on page one; `layout bottom` and `layout grid` change only how the items are arranged (one or two columns, anchored
to the bottom). Also in settings under `home`. The dock shows full app names (`messages`), cut at nine characters.

## Icons (`layout icons`)
`layout icons` (or settings > home > layout) switches to the icon style and moves the panels to their own glance page. The home page becomes a
4-column grid of themed icons with one-line labels, anchored near your thumb; folders are tiles previewing their first four apps. The dock
and the quick panel show icon cells too, and the drawer is a 4-column icon grid with the letter sections and the A-Z rail, with search and
`sort` / `[cfg]` in a bar at the bottom (swipe down on it or press Back to close; the keyboard still opens with the drawer). Icons are
drawn as rounded tiles in your theme colors: an app's monochrome (themed) icon is tinted with the theme's foreground; apps without one get
their icon grayscaled and tinted; an app with no icon gets its first letter. Tiles are drawn off the main thread and cached; the dock,
quick tiles and page one are drawn ahead of time, and so are the first 24 drawer cells a moment after start. Other layouts are unchanged.

## Density, wallpapers, gestures
- `density dense|compact|normal|roomy` (or settings > home > density): how much fits. In the icon style it sets the grid's columns (6 / 5 / 4 / 3)
  and the icon, label, dock and quick-icon sizes; it also scales widget heights (0.7x / 0.85x / 1x / 1.15x, never below what a widget needs).
- `wall mode svg`, then `wall svg circuit|hexnet|radar|tunnel|glitch|windows|helix|topo|next|random`: eight generated vector wallpapers drawn in
  the theme colors (the wall seed makes a picture repeatable; `wall dim` sets how strong it is).
- Swipe down on the left half of the screen: notifications; on the right half: quick settings. Double-tap an empty spot: lock the screen (switch
  on the "Terminal: lock screen" accessibility service once; settings > gestures has the shortcut). A one-line weather summary shows under the
  date when the weather panel is on. Long-press an app for its own shortcuts (when Terminal is the default home app).

## Search beyond apps, badges, media and toggles
- The drawer search also offers matching contacts (after you allow contacts), matching Android settings pages (`wifi`, `battery`, `dark`...) and
  "search the web: ..."; the Search key launches the best app match, or the web row when there is none.
- Icons show a small count badge when an app has notifications (settings > notifications: needs notification access; Terminal only counts
  them, it never reads their text). Two new panels, added with `[+panel]` in edit mode: `now playing` (title, artist, previous/play/next) and
  `toggles` (flashlight, and shortcuts to the wifi, bluetooth, sound and location screens).

## Effects (`fx`)
`fx scanlines on|off|auto`: CRT scanlines (dark lines plus a faint line in the theme color) and a vignette; `auto` is on for the cyber themes and
the glowing CRT themes (classy-black, matrix, amber). While scanlines are on, a soft band also sweeps down the screen every few seconds.
`fx glitch on|off` glitches the clock when the minute changes and, every 30 to 60 seconds, flashes a few colored glitch bars across the whole
screen; `fx flicker on|off` adds quick dark flashes. Everything stops under reduced motion and while the launcher is not on screen.

## Moving icons
In the icon style, press and hold an icon to pick it up and drop it on any slot: onto an empty one (it stays where you put it and leaves a hole
behind, so nothing else moves), onto another icon (it is inserted there and the rest shift over), or into the extra empty row that appears
while you drag. Hold an icon at the left or right edge and the page turns, so you can drop it on another page; drop it on the dock to pin
it there. A copy of the icon follows your finger. Hold without moving and you get the item menu as before. Swipes up and down on the home screen
need about 44 dp of travel (a little more than before) to open the drawer or the shade. The wallpaper settings only show the fields the chosen wallpaper uses.

## Matrix
- `wall mode rain` (also in settings > wallpaper): falling half-width katakana and digits behind home, in the theme color with a bright head
  per streak. `wall dim` sets how strong it is. It only moves while the launcher is on screen and is a single still frame with animations off.
- `matrix wake`: "Wake up, Neo..." typed out on black (tap to leave). `matrix` alone is still the 6-second full-screen rain.
- `pill red` switches to the `matrix` theme with the rain wallpaper; `pill blue` goes back to `classy-black` and a solid background.
  Also `follow the white rabbit` and `there is no spoon`.

## Clock
`clock auto|24|12` sets the hour format (`auto` follows the phone; 12-hour drops the leading zero and adds `am`/`pm`, also in the status
line, calendar and `date`). `clock style pixel|segment|figlet|binary|hex|epoch|plain` picks how the clock looks (`binary`, `hex` and
`epoch` always use 24-hour values; `epoch` ticks each second only while the launcher is on screen). Both are in settings under `clock`
and in the backup. A bare `clock` shows the current values.

## Prompt commands
`theme <name>` | `theme add|rm ...` | `clock ...` | `pill red|blue` | `matrix wake` | `pin <app>` | `hide <app>` (toggles) | `mkdir <folder>` | `mv <app> <folder>` |
`mv <folder> <newname>` (rename) | `rmdir <folder>` | `ls` (drawer) | `ls -a` (hidden apps) | `help` |
`fx [name on|off]` | `settings` | `wall ...` | `panel <cal|wx|search|quick> on|off` | `quick ...` | `wx ...` | `history` | `neofetch` | `whoami` | `uptime` | `date` | `fortune` | `clear` | `sudo ...` | `matrix`.
Any other text fuzzy-matches installed apps; Enter launches the top match. `⇥` completes the input;
with an empty prompt your last 5 commands are offered. Output appears in the `out` panel (12 lines, tap to dismiss).

## Effects (`fx <name> on|off`, saved)
| name | default | what |
|---|---|---|
| scanlines | on for cyber themes | scanlines + vignette |
| flicker | off | subtle random flicker |
| glitch | on | clock scrambles for ~0.4 s when the minute changes |
| cursor | on | blinking block cursor in the prompt |
| boot | on | ~1 s fake boot log on unlock (tap to skip) |
If system animations are off (animator scale 0), flicker, glitch, boot log and matrix rain are skipped.

## Wallpaper (`wall`)
`wall` shows the settings; `wall mode solid|system|ascii`, `wall random`, `wall next`, `wall style <glyph|circuit|contour|stars|hexdump|skyline>`,
`wall seed <number>`, `wall dim <0-100>`, `wall animate on|off`. Settings are saved.
- `solid` (default): the opaque theme background, as before.
- `system`: the launcher is transparent over the system wallpaper (any wallpaper you pick in Pixel's Wallpaper & style
  app shows through); a theme-colored dim layer (`wall dim`, default 55) keeps text readable.
- `ascii`: seeded ASCII art in the theme's dim color; the same style, seed and screen size always give the same picture.
  `wall animate on` scrolls it slowly (off by default, skipped when system animations are off).
A launcher cannot read the wallpaper files of Google's Wallpaper app; `system` mode shows whichever wallpaper is set.
The open drawer shows the wallpaper/art with an extra 35% readability layer.

## Settings, backup and import
- `settings` (or `[settings]` at the bottom of edit mode) opens the settings screen: tap a theme to apply it, change the wallpaper,
  effects (scanlines cycle `auto (theme)` / `on` / `off`), panels, drawer sort and icons, and use the backup section. A bare `theme`
  lists the themes; `fx scanlines auto` returns scanlines to following the theme.
- **Export / import layout** (settings > backup): saves `terminal-launcher-backup.json` (layout, folders, hidden apps, theme, effects,
  wallpaper, panels, weather city/units, drawer options; never widgets) through the system file picker, and restores it with a preview
  and `[replace]` / `[cancel]`.
- **Import from Nova / Lawnchair**: pick a backup file (a ZIP containing `launcher.db`, or a raw SQLite file with a `favorites` table).
  Pages, dock and folders come over; widgets are skipped (re-add them), apps not installed here are dropped, up to 5 pages. The reader
  is best-effort and untested against real backup files so far.
- **Pixel Launcher cannot be imported**: its layout is private (`ACCESS_LAUNCHER_DATA` is system-only), so there is no way for any app to read it.

## Search bar
A `g> search google` row under the clock: type a query and press Search or `[go]` to open Google in your browser or the Google app.
`panel search off` hides it (on by default).

## Quick tiles (`quick` panel, on by default)
Your important apps as big text tiles under the search bar, four per row, up to 8. First run: the phone's own dialer, messages, camera and
maps apps. Tap launches, long-press opens the app menu (pin to dock, hide, info...). Change them in settings (`quick tiles`: move up, remove,
add, reset) or with `quick add <app>`, `quick rm <app>`, `quick clear`, `quick reset`; `panel quick off` hides the panel. Included in backups.

## Panels (search, sys, calendar, weather; calendar and weather off by default)
Each panel is an item on a page, on page one under the clock by default. In edit mode (long-press an empty spot) every page lists its
panels with `[up]` `[down]` `[pg n]` (send to another page) and `[rm]`; `[+panel]` puts a removed one back on that page. `panel cal on` also
brings a removed panel back to page one. A panel still only shows while its `panel` switch is on. The clock and date stay at the top of page one.
- `panel cal on|off`: the next 6 calendar events within 7 days, e.g. `today 10:05  Standup`, `now  Standup`, `tmrw all day  Holiday`.
  The first `panel cal on` asks for calendar read permission; if you deny it the panel says so and works once granted in Settings.
- `panel wx on|off`: weather from Open-Meteo (free, no account). Set a city with `wx city <name>` (looked up once, switches the panel on),
  `wx units c|f`, `wx` refreshes now. Only the city's latitude/longitude (and the name for the one-time search) are sent; there is no
  location permission. Refreshes every 30 minutes while the launcher is open; the last result is cached and shown offline.
- `panel` alone lists the switches.

## Widgets
Long-press an empty page area -> `[+widget]` on a page lists the installed widgets. Android asks once whether the launcher may use the widget
(and opens the widget's own setup screen if it has one). Widgets show inside a terminal-style frame titled with their name and keep their own look.
Per widget in edit mode: size `S M L` and `[rm]`. Pages scroll when their content overflows; the swipe-up for the drawer works from the bottom area.

## Gestures
Swipe up: app drawer (anywhere on home; on a page that scrolls, once it is at the bottom). Swipe down: the notification shade (at the top of a scrolling page). Swipe left/right: pages. Long-press an app: menu. Long-press empty page area: edit pages.

## Permissions
`REQUEST_DELETE_PACKAGES` (uninstall from the app menu), `EXPAND_STATUS_BAR` (swipe down opens the notification shade), `ACCESS_NETWORK_STATE` (network type in the sys panel), `READ_CALENDAR` (calendar panel, asked at runtime), `INTERNET` (weather panel only).

Font: JetBrains Mono (SIL OFL, see JetBrainsMono-OFL.txt).
