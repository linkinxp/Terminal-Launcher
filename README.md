# terminal-launcher

A terminal-styled Android home launcher (Kotlin, Jetpack Compose) with themes, home pages, folders,
a prompt (`user@phone:~$`) and an app drawer. Personal use, installed alongside the stock launcher.

## Build / install (on android-dev)
    ./gradlew --no-daemon assembleDebug testDebugUnitTest
    /opt/android-sdk/platform-tools/adb install -r app/build/outputs/apk/debug/app-debug.apk
Then Settings > Apps > Default apps > Home app > Terminal.

## Rollback
Switch the default Home app back in Settings, or `adb uninstall dev.linkinxp.termlauncher`.

## Prompt commands
`theme <name>` (classy-black, matrix, amber, solarized-dark, dracula) | `pin <app>` | `hide <app>` (toggles) |
`mkdir <folder>` | `mv <app> <folder>` | `rmdir <folder>` | `ls` (drawer) | `ls -a` (hidden apps) | `help`.
Any other text fuzzy-matches installed apps; Enter launches the top match.

## Gestures
Swipe up: app drawer. Swipe left/right: pages. Long-press an app: menu. Long-press empty page area: edit pages.

Font: JetBrains Mono (SIL OFL, see JetBrainsMono-OFL.txt).
