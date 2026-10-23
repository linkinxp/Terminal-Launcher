# terminal-launcher

A terminal-styled, cyberpunk-flavoured Android home launcher (Kotlin, Jetpack Compose) with themes, home pages,
folders, a prompt (`user@phone:~$`), a live system panel and an app drawer. Personal use, installed alongside the
stock launcher. Version 0.3.0, requires Android 14+ (minSdk 34).

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
`fx [name on|off]` | `wall ...` | `history` | `neofetch` | `whoami` | `uptime` | `date` | `fortune` | `clear` | `sudo ...` | `matrix`.
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

## Gestures
Swipe up: app drawer. Swipe left/right: pages. Long-press an app: menu. Long-press empty page area: edit pages.

## Permissions
`REQUEST_DELETE_PACKAGES` (uninstall from the app menu), `ACCESS_NETWORK_STATE` (network type in the sys panel).

Font: JetBrains Mono (SIL OFL, see JetBrainsMono-OFL.txt).
