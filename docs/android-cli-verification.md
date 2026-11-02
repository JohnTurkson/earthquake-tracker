# Android CLI verification, 2026-10-31

The [workflow guide](android-cli.md) was exercised against the app sources at
`d0f0264`. This change adds instructions; it does not change the app's UI or
architecture. Google's Android CLI and launcher reported **1.1.16717390** after
`android update`; Homebrew already had that launcher installed. A fresh Homebrew
installation was not repeated.

The device was `emulator-5554`, AVD `Resizable_API_37_2_Google_APIs`, Android API
37, Google APIs ARM64, 1080 by 2400 pixels at density 420. It was started with
`android emulator start Resizable_API_37_2_Google_APIs --headless`.

## Build and static checks

| Check | Result |
| --- | --- |
| `android build ':app@debug'` | Passed; the build log selected `:app:assembleDebug`, reported `BUILD SUCCESSFUL`, and produced `app/build/outputs/apk/debug/app-debug.apk`. Valid Gradle cache hits were used. |
| `./gradlew :ktfmtCheck :app:testDebugUnitTest :app:lintDebug --continue` | Combined command failed because formatting failed; the other selected checks completed. |
| Formatting | `:ktfmtCheckSources` failed on 15 existing Kotlin files before any task edits. App sources remain unchanged. |
| Unit tests | `:app:testDebugUnitTest` passed from the Gradle cache: 4 tests, 0 failures/errors/skips in the XML results (1 serialization test and 3 template tests). |
| Lint | `:app:lintDebug` passed; XML reported 3 warnings and no errors. |
| Instrumented device tests | Not run. This documentation task adds no Android behavior, and the project lacks an explicit `testInstrumentationRunner`, as recorded in the testing guide. |

Documentation verification includes local-link review, shell-block syntax,
embedded Python syntax, and `git diff --check`. The formatting failure is a
pre-existing repository issue and is not described as passing.

## Manual device checks

| Scenario | Result |
| --- | --- |
| `android install --device=emulator-5554 app/build/outputs/apk/debug/app-debug.apk` | Passed; standard installation transferred the APK. |
| `android run --device=emulator-5554 ':app@debug'` | Passed; built, delta-installed, and launched the app. The following hierarchy contained the scaffold's controls. |
| `android layout --pretty`, plus `--full` | Passed; captured application/system windows, text, descriptions, interactions, and component bounds. |
| Native tap by `desc=/^Start new timer$/` | Passed; navigation exposed Save on the detail screen. |
| Native tap by `text=/^Save$/` | Passed; returned to the main screen. |
| Native tap by `text=/^[+]10$/` | Passed; populated the timer scaffold. |
| Native `swipe 'scrollable=true' --dir=up --distance=0.4` | Passed after adding enough rows; hierarchy bounds changed after the gesture. |
| Native `scroll 'scrollable=true' --dir=up --amount=0.5` | Passed; subsequent hierarchy showed a changed visible list region. |
| Unannotated `android screen capture` | Initially produced an unreadable file despite exit 0: a multi-display warning preceded the PNG signature. The documented signature-strip workaround recovered the PNG; it was visually inspected. |
| Annotated `android screen capture --annotate` | Passed; produced a valid PNG with numbered visual regions, including the Save button, and was visually inspected. |
| Save component PNG | Passed; used enclosing bounds `[11,2176][1069,2302]`, cropped with macOS `sips` to 1058 by 126 pixels, and visually inspected the complete button. |
| Device configuration | Read API, size, density, font scale, and night mode. Changed night mode from `no` to `yes`, verified the setting, and restored `no` in a `finally` block. This is command/configuration verification, not coverage of all theme visuals. |
| `android preview list ':app@debug' --format=json` | Discovery succeeded with `previews: []`. No active preview exists, so component preview rendering was not verified. |

An early scroll attempt returned `No matching elements` while the list exposed
no scrollable container. This was resolved by populating the list, waiting for
the resulting state, and inspecting the full hierarchy before retrying. The
guide includes that precondition.

Live-device artifacts from the first pass were saved in
`/tmp/usgs-android-cli-check/`. They are local evidence, not checked-in test
baselines. The independent agent pass uses ignored
`build/android-cli/independent-smoke/`.

## Independent agent pass

A second agent followed the written guide without changing tracked files. It
independently passed the native build, APK install, APK run, hierarchy capture,
selector navigation to detail, Save navigation, and hardware Back navigation.
Fresh hierarchies confirmed the destinations after each action.

That pass reproduced the 347-byte warning prefix and recovered a viewable
1080 by 2400 PNG with the documented Python command. It visually inspected the
full screen and the 1058 by 126 Save-button crop. It also visually inspected a
fresh annotated capture, observed Save at label `#4`, and verified that
`android screen resolve --screenshot=... --string='#4'` returned `540 2238`.
The agent reported no blocking documentation defects.

The research pass also successfully exercised Knowledge Base search and fetch
for Navigation 3 results, as recorded in the
[Google content review](android-cli-review.md#instructions-applicable-to-this-project-now).

## Limits

The UI is still the timer/navigation scaffold. These results do not verify USGS
requests, earthquake presentation, offline behavior, or Maps tiles. No active map
screen was available to verify Maps authorization. The scaffold has no editable
field, so native text input was help-checked rather than exercised in this app.

Existing screenshot regression coverage remains missing; ad hoc captures and
preview images do not satisfy the architecture guide's state coverage rule. No
new screenshot framework or baseline convention was introduced.
