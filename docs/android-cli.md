# Android CLI development workflow

Use Google's current `android` executable for builds, deployment, live UI
inspection, and app interaction. It is a separate tool from ADB and the retired
SDK Tools command with the same name. Follow the [build guide](building.md) for
the project's JDK, SDK, and private Maps key setup, and the
[testing guide](testing.md) for required verification.

## Install on macOS

With [Homebrew](https://brew.sh/) installed, Google's
[download instructions](https://developer.android.com/tools/agents/android-cli/download)
support both Apple silicon and Intel Macs:

```sh
brew tap android/tap
brew install android-cli
command -v android
android update
android --version
android --help
```

Use `brew upgrade android-cli` to upgrade the Homebrew package; `android update`
updates the CLI's downloaded bundle. Check the reported CLI version rather than
assuming the Homebrew package version proves which bundle is running. These
workflows were checked with CLI and launcher **1.1.16717390**. Older releases may
lack `build`, `preview`, or native `input` subcommands. Recheck a command's
`--help` after upgrading.

Homebrew manages the executable; `command -v android` gives its actual location
(commonly `/opt/homebrew/bin/android` or `/usr/local/bin/android`). The CLI also
writes bundles, locks, and caches under `~/.android/cli`. Keep tool installations
outside the repository. A sandboxed agent may need the normal execution approval
mechanism for those paths, Gradle caches, SDK tools, and the emulator connection.
An access error is an environment failure, not an app failure.

Initialize Google's CLI skill for the local agent environment:

```sh
android init
android skills list
android info
```

Review any files setup changes before committing. Google's skills supplement the
repository's architecture, formatting, testing, and source-control conventions.
For a particular Android task, find an applicable installed skill and read it;
installing every skill is unnecessary for building and inspecting this app.

For the usual Android Studio SDK location on macOS:

```sh
export ANDROID_HOME="$HOME/Library/Android/sdk"
export PATH="$ANDROID_HOME/platform-tools:$PATH"
android --sdk="$ANDROID_HOME" info
android sdk list
```

If tools or the compile SDK are missing, install the packages requested by the
build. For the current API 37.0 configuration, for example:

```sh
android sdk install platform-tools platforms/android-37.0 build-tools/37.0.0
```

Use the SDK path reported by Android Studio when it differs. `android info`
checks the CLI's SDK selection; it does not configure Gradle's SDK location.
Preserve `sdk.dir` in ignored `local.properties` or supply `ANDROID_HOME` to the
build. Java must also be available as described in the build guide.

## Select and start a device

```sh
android emulator list
android emulator create --list-profiles
adb devices -l
```

`emulator list` lists AVD names; `adb devices -l` lists connected serials. Prefer
an existing compatible AVD. To create one when needed, choose a listed profile,
for example `android emulator create medium_phone`, then list the resulting AVD.
The creation command selects a profile; it has no arbitrary API/image/density
flags in the tested version. Use Android Studio Device Manager for an exact
system image or hardware configuration. Maps verification requires a Google
APIs or Google Play image with Google Play services.

Start a named AVD from the list; `--headless` is useful for agent work:

```sh
android emulator start Resizable_API_37_2_Google_APIs --headless
adb devices -l
export ANDROID_SERIAL=emulator-5554
export ARTIFACT_DIR="$PWD/build/android-cli"
mkdir -p "$ARTIFACT_DIR"
```

Replace the example AVD and serial with the actual values. Start waits until the
device is ready. Add `--cold` when deliberately testing a cold boot. Pass
`--device="$ANDROID_SERIAL"` on every device command, including when multiple
devices are connected. Keep captures in ignored `build/` or a temporary directory.
Stop an emulator you started when finished:

```sh
android emulator stop Resizable_API_37_2_Google_APIs
```

## Build, install, and run this app

Run from the repository root so the CLI uses this checkout and its Gradle
wrapper:

```sh
android build ':app@debug'
android install --device="$ANDROID_SERIAL" app/build/outputs/apk/debug/app-debug.apk
android run --device="$ANDROID_SERIAL" ':app@debug'
```

`build` assembles the chosen variant; `install` deploys an existing APK without
launching it; `run` with a build reference builds, deploys, and launches it. To
deploy and launch an existing APK instead:

```sh
android run --device="$ANDROID_SERIAL" app/build/outputs/apk/debug/app-debug.apk
```

The target is the Gradle module and variant, not an `assembleDebug` task name.
Quote references containing `?` in zsh, for example
`android build 'gradle://?:app@debug'`. Arguments after `--` go to Gradle.
The CLI prints a build-log path; inspect it for `:app:assembleDebug` and the
build result before claiming success. Verify the APK exists. Gradle caching and
delta installs are expected optimizations.

Use the existing wrapper commands for formatting, lint, unit tests, and device
tests from the testing guide. Building or launching through the CLI does not
replace those checks. The app still uses package `com.johnturkson.sandbox` and
timer/navigation scaffolding; successful launch does not establish earthquake
features or Maps authorization.

## Inspect the UI while developing

Inspect the live hierarchy before choosing a target and again after each action
whose result matters:

```sh
android layout --device="$ANDROID_SERIAL" --pretty --output="$ARTIFACT_DIR/layout.json"
android layout --device="$ANDROID_SERIAL" --full --pretty --output="$ARTIFACT_DIR/layout-full.json"
```

The JSON includes application and system windows, text, accessibility content
descriptions, resource IDs when available, interactions, state, bounds, and
centers. Default output filters uninformative nodes; `--full` includes hidden
and non-interactive nodes and can expose a useful component ancestor. A Compose
text node may sit inside its clickable parent; use the parent's bounds when
capturing the whole button. This is an accessibility hierarchy, not the complete
Compose source tree. Lazy items outside the viewport may not exist yet.

If an animated or ticking screen cannot become idle, retry with `--no-idle` and
record that the capture may show a transition. `--diff` is deprecated and a no-op
in the tested version. Inspect images, map surfaces, and visual layout with PNGs;
the hierarchy alone cannot prove their appearance.

## Interact with the app

Prefer native `android input` selectors taken from the current layout. Quote
selectors. String values match case-insensitive substrings by default; an
anchored regular expression gives an exact match. Ambiguous matches should fail:
refine the selector, using `--match=first` only when choosing the first is the
intended action. `--wait=5000` waits for a target, not for all resulting work.

These commands use the existing scaffold's accessible controls:

```sh
android input --device="$ANDROID_SERIAL" tap 'desc=/^Start new timer$/' --wait=5000
android input --device="$ANDROID_SERIAL" tap 'text=/^Save$/' --wait=5000
android input --device="$ANDROID_SERIAL" tap 'text=/^[+]10$/' --wait=5000
android input --device="$ANDROID_SERIAL" key back
```

For scrolling, first add enough rows that the layout exposes a `SCROLLABLE`
container. A short list may have no `scrollable=true` match. Inspect again after
the population action has completed. On a scrollable list:

```sh
android input --device="$ANDROID_SERIAL" swipe 'scrollable=true' --dir=up --distance=0.4
android input --device="$ANDROID_SERIAL" scroll 'scrollable=true' --dir=up --amount=0.5
```

Swipe direction describes the finger movement. Scroll supports
`--until='text=TARGET'` when a known destination should become visible; choose an
unambiguous container and bound the command with the execution tool's timeout.
Verify the resulting content or bounds changed; command success alone is not a
passing behavior check.

For a screen that actually contains an editable field, replace the example
selector with its live ID or description:

```sh
android input --device="$ANDROID_SERIAL" text 'example query' 'id=actual_input_id' --submit
```

This replaces existing text and submits it. The current timer scaffold has no
editable field, so this is syntax guidance, not a tested app interaction.
`key home`, `key recents`, and `key enter` are also supported. Reopen the app and
inspect it before continuing after leaving its screen.

## Capture screenshots and specific components

Capture the current device screen for visual verification:

```sh
android screen capture --device="$ANDROID_SERIAL" --output="$ARTIFACT_DIR/screen.png"
```

Open and visually examine each captured PNG before using it to choose an action
or claiming the UI is correct. Use the agent's image viewer; on macOS a human can
use `open "$ARTIFACT_DIR/screen.png"`. Match the image's state and dimensions to
the layout capture. Do not reuse coordinates after navigation, scrolling,
rotation, keyboard changes, or another configuration change.

For visual regions absent from the accessibility hierarchy:

```sh
android screen capture --device="$ANDROID_SERIAL" --annotate --output="$ARTIFACT_DIR/annotated.png"
```

View that image before resolving a label selected from it:

```sh
android screen resolve --screenshot="$ARTIFACT_DIR/annotated.png" --string='#4'
```

Replace `#4` with the label actually observed. This returns coordinates; it does
not click. Prefer a native selector when available. If only a visual target is
available, use those fresh coordinates with the narrow fallback
`adb -s "$ANDROID_SERIAL" shell input tap X Y`, then recapture and verify.

### Crop a live component on macOS

`screen capture` captures a whole display; it has no component-selector or crop
option. Capture a clean, unannotated screen and the matching `--full` hierarchy.
Find the component's enclosing bounds `[left,top][right,bottom]`. Crop with macOS
`sips`, using height `bottom-top`, width `right-left`, and offset `top left`.

For example, the tested Save button's parent occupied `[11,2176][1069,2302]`
on a 1080 by 2400 screen:

```sh
sips --cropToHeightWidth 126 1058 --cropOffset 2176 11 \
  "$ARTIFACT_DIR/screen.png" --out "$ARTIFACT_DIR/save-button.png"
```

Replace all four numbers from the current hierarchy; these values are not stable
device-independent coordinates. View the cropped PNG and retain its full-screen
capture for context. Cropping captures only the component's current visible state.

### Render a Compose component without a device

For components with a deterministic `@Preview`, discover the actual preview name
and render just that fixture:

```sh
android preview list ':app@debug' --format=json
android preview render ':app@debug' --fqn='actual.package.ActualPreview' \
  --component-tree --output="$ARTIFACT_DIR/previews" --format=json
```

Replace the FQN with a result from `list`. View the PNG and inspect the companion
component-tree JSON for bounds, semantics, and source locations. This workflow
runs without Android Studio or an emulator. Use isolated previews to inspect
supported component states before checking integration on a device, following
the existing read-only state and deterministic fixture conventions.

The current checkout has no active previews: discovery returns an empty list,
and `MainScreenPreview` is commented out and no longer matches the screen's
signature. No preview PNG was verified for this app. Do not claim a render passed
when zero previews were selected. Preview renders and ad hoc device captures do
not satisfy the required screenshot regression coverage; agree on that framework
and baseline process as directed by the architecture guide.

### Resizable emulator PNG workaround

With CLI 1.1.16717390, the tested resizable API 37 emulator prepended an Android
multi-display warning to an unannotated capture. The command exited successfully
but image viewers rejected the file. Annotated capture produced a valid PNG.
`screen capture --help` exposes no display-ID flag in this version.

Prefer a single-display emulator. To recover this specific unannotated capture
without changing its pixels, the following Python 3 command removes bytes before
the PNG signature and prints the warning. Run it only for this known symptom;
absence of a PNG signature means capture failed:

```sh
python3 - "$ARTIFACT_DIR/screen.png" <<'PY'
from pathlib import Path
import sys

path = Path(sys.argv[1])
data = path.read_bytes()
offset = data.find(b'\x89PNG\r\n\x1a\n')
if offset < 0:
    raise SystemExit('Capture contains no PNG signature')
if offset:
    print(data[:offset].decode('utf-8', errors='replace'))
    path.write_bytes(data[offset:])
PY
```

Reopen and visually verify the recovered image before cropping or interacting.

## Device configuration

Use native emulator commands for profile creation, start, cold boot, and stop.
For display and OS settings, CLI 1.1.16717390 has no general configuration
subcommand. Use narrowly scoped ADB commands on the selected development device.
Record existing values before changing them and restore those exact values when
finished; `reset` removes an override, which may differ from the original value.

```sh
adb -s "$ANDROID_SERIAL" shell getprop ro.build.version.sdk
adb -s "$ANDROID_SERIAL" shell wm size
adb -s "$ANDROID_SERIAL" shell wm density
adb -s "$ANDROID_SERIAL" shell settings get system font_scale
adb -s "$ANDROID_SERIAL" shell cmd uimode night
```

Examples for deliberately inspecting alternate configurations:

```sh
adb -s "$ANDROID_SERIAL" shell cmd uimode night yes
adb -s "$ANDROID_SERIAL" shell settings put system font_scale 1.3
adb -s "$ANDROID_SERIAL" shell wm size 1080x1920
adb -s "$ANDROID_SERIAL" shell wm density 420
```

Change one setting at a time, wait for the app to settle, then recapture layout
and screenshots. Restore the recorded mode (`yes`, `no`, or `auto`), font scale,
and any pre-existing display overrides. Use `wm size reset` and `wm density reset`
only when there were no overrides before the check; delete a settings key with
`settings delete` if its original value was `null`. The theme query, change, and
restoration were exercised; font and display mutations above are command
examples rather than verified visual-state coverage.

## Ground Android work in current Google guidance

Before implementing unfamiliar or changing Android APIs, search the official
Knowledge Base and fetch a relevant result using its returned `kb://` URL:

```sh
android docs search 'Navigation 3 saved state'
```

Then run `android docs fetch 'kb://URL_FROM_SEARCH'`. Compare the guidance with
this project's actual dependency versions and architecture before applying it.
Do not copy a tutorial's networking, DI, navigation, or state ownership over the
established Ktor, Hilt, Navigation 3, and state-interface patterns.

Google's [command reference](https://developer.android.com/tools/agents/android-cli/commands)
and installed `--help` are the syntax references. Additional capabilities from
Google's recent announcements are assessed in the
[Google content review](android-cli-review.md); capabilities requiring new project
conventions or external services remain proposals for the user to choose.

See the [dated verification record](android-cli-verification.md) for the commands
exercised on this app, observed results, and remaining limitations.
