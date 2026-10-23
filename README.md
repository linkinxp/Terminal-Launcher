# terminal-launcher

A terminal-styled, cyberpunk-flavoured Android home launcher (Kotlin, Jetpack Compose) with themes, home pages,
folders, a prompt (`user@phone:~$`), a live system panel and an app drawer. Personal use, installed alongside the
stock launcher. Version 0.4.0, requires Android 14+ (minSdk 34).

## Build / install (on android-dev)
    ./gradlew --no-daemon assembleDebug testDebugUnitTest
    /opt/android-sdk/platform-tools/adb install -r app/build/outputs/apk/debug/app-debug.apk
Then Settings > Apps > Default apps > Home app > Terminal.
JVM screenshots of the screens (no emulator): `./gradlew --no-daemon recordRoborazziDebug` -> `app/build/shots/*.png`.

## Rollback
Switch the default Home app back in Settings, or `adb uninstall dev.linkinxp.termlauncher`.

## Themes
`classy-black`, `matrix`, `amber`, `solarized-dark`, `dracula`, and the cyber set `neon-city`, `synthwave`,
`blade-runner`, `glitch-red` (scanlines on by default). Switch with `theme <name>`.

## Prompt commands
`theme <name>` | `pin <app>` | `hide <app>` (toggles) | `mkdir <folder>` | `mv <app> <folder>` |
`mv <folder> <newname>` (rename) | `rmdir <folder>` | `ls` (drawer) | `ls -a` (hidden apps) | `help` |
`fx [name on|off]` | `wall ...` | `panel <cal|wx> on|off` | `wx ...` | `history` | `neofetch` | `whoami` | `uptime` | `date` | `fortune` | `clear` | `sudo ...` | `matrix`.
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

## Panels (page 1, under `sys`; both off by default)
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
Swipe up: app drawer. Swipe left/right: pages. Long-press an app: menu. Long-press empty page area: edit pages.

## Permissions
`REQUEST_DELETE_PACKAGES` (uninstall from the app menu), `ACCESS_NETWORK_STATE` (network type in the sys panel), `READ_CALENDAR` (calendar panel, asked at runtime), `INTERNET` (weather panel only).

Font: JetBrains Mono (SIL OFL, see JetBrainsMono-OFL.txt).
