# Android CLI

Use Google's `android` CLI for builds, deployment, live UI inspection,
screenshots, and app interaction. Follow the [build guide](building.md) for
project prerequisites and the [testing guide](testing.md) for required checks.
Prefer native CLI commands and use the ADB fallbacks below for remaining
operations.

Keep this guide focused on setup and reusable usage instructions. Report
task-specific actions, verification results, and limitations in the chat.

## Install on macOS

With [Homebrew](https://brew.sh/) installed, follow Google's
[installation instructions](https://developer.android.com/tools/agents/android-cli/download):

```sh
brew tap android/tap
brew install android-cli
command -v android
android --version
android --help
```

Use `brew upgrade android-cli` to upgrade the Homebrew package and
`android update` to update the CLI bundle. Check the relevant subcommand's
`--help` before using it; installed versions can differ from online examples.
Keep CLI installations and caches outside the repository.

Use the SDK path from Android Studio. For the usual macOS location:

```sh
export ANDROID_HOME="$HOME/Library/Android/sdk"
export PATH="$ANDROID_HOME/platform-tools:$PATH"
android --sdk="$ANDROID_HOME" info
android sdk list
```

Install missing SDK packages with `android sdk install` using the versions the
project build requires. Configure Gradle's SDK location through `ANDROID_HOME`
or ignored `local.properties`, as described in the build guide.

## Select a device

List available AVDs and connected device serials:

```sh
android emulator list
adb devices -l
```

Use an **API 37** emulator with the **Resizable** hardware profile for device
tests, manual UI verification, and device screenshots. Reuse an existing AVD only
after confirming both its hardware profile and system image. Do not default to
Medium Phone or another fixed-size profile, even when one is already running.

If no matching AVD exists, create one in Android Studio Device Manager using the
**Resizable** hardware profile and an **API 37** system image. Use a Google APIs
or Google Play image for Maps checks. The CLI's predefined phone, tablet, and
desktop creation profiles do not replace the required Resizable profile.

Check the AVD's hardware profile and system image in Device Manager or its
`config.ini` (`hw.device.name=resizable` and an API 37 `image.sysdir.1`). An AVD's
name alone does not establish its configuration. After starting it, confirm the
running API level with `getprop` as shown below; it must report `37`.

For mockup testing, create a uniquely named dedicated emulator instance and
leave other active emulators unchanged. Record its AVD name and actual connected
serial, target that serial for every command, restore temporary settings, and
stop the instance when finished.

Start an AVD by its listed name, then select its actual connected serial.
Replace the example name and serial below with those values:

```sh
android emulator start Resizable_API_37 --headless
adb devices -l
export ANDROID_SERIAL=emulator-5554
adb -s "$ANDROID_SERIAL" shell getprop ro.build.version.sdk
export ARTIFACT_DIR="$PWD/build/android-cli"
mkdir -p "$ARTIFACT_DIR"
```

Pass `--device="$ANDROID_SERIAL"` on device commands. Keep captures in ignored
`build/` or a temporary directory. Stop an emulator you started when finished,
using `android emulator stop` with its AVD name.

## Build, install, and run

Run from the repository root:

```sh
android build ':app@debug'
android install --device="$ANDROID_SERIAL" app/build/outputs/apk/debug/app-debug.apk
android run --device="$ANDROID_SERIAL" app/build/outputs/apk/debug/app-debug.apk
```

Build references identify the Gradle module and variant. To build, deploy, and
launch in one step, use `android run --device="$ANDROID_SERIAL" ':app@debug'`.
Inspect the reported build log and confirm the expected APK exists. Run the
formatting, lint, and test commands from the testing guide as required.

## Inspect and interact

Inspect the live hierarchy before selecting a control:

```sh
android layout --device="$ANDROID_SERIAL" --pretty --output="$ARTIFACT_DIR/layout.json"
android layout --device="$ANDROID_SERIAL" --full --pretty --output="$ARTIFACT_DIR/layout-full.json"
```

Use text, descriptions, resource IDs, and bounds from that hierarchy. Refine
selectors until they identify the intended element uniquely. Replace the
uppercase selector placeholders below with values from the current UI:

```sh
android input --device="$ANDROID_SERIAL" tap 'desc=TARGET_DESCRIPTION'
android input --device="$ANDROID_SERIAL" swipe 'scrollable=true' --dir=up --distance=0.4
android input --device="$ANDROID_SERIAL" scroll 'scrollable=true' --dir=up --amount=0.5
android input --device="$ANDROID_SERIAL" text 'example query' 'id=INPUT_ID' --submit
android input --device="$ANDROID_SERIAL" key back
```

Scroll requires a visible scrollable container; text input requires an editable
field. Swipe direction describes finger movement. Inspect the resulting hierarchy
after each meaningful action and verify the expected state. Use PNGs for map
surfaces and other content the accessibility hierarchy cannot describe.

Use `--wait=5000` on a tap when the target may appear asynchronously; it waits
for the element, not work triggered by the action. For the current map, inspect
the `Earthquake map` description before panning, then capture the result to check
camera movement and tile rendering:

```sh
android input --device="$ANDROID_SERIAL" swipe 'desc=/^Earthquake map$/' --dir=left --distance=0.6
```

## Capture screens and components

Capture the current screen and open the PNG in the image viewer:

```sh
android screen capture --device="$ANDROID_SERIAL" --output="$ARTIFACT_DIR/screen.png"
```

Visually inspect the image before using it to choose an action or claiming the
UI is correct. For visual targets absent from the hierarchy, capture an annotated
image, view it, and resolve a label actually observed in that image:

```sh
android screen capture --device="$ANDROID_SERIAL" --annotate --output="$ARTIFACT_DIR/annotated.png"
android screen resolve --screenshot="$ARTIFACT_DIR/annotated.png" --string='#4'
```

Replace `#4` with the observed label. If a native selector is unavailable, use
the returned coordinates with `adb -s "$ANDROID_SERIAL" shell input tap X Y`,
then recapture and verify. Refresh captures after navigation, scrolling, or
configuration changes before using bounds or coordinates.

If a PNG cannot be opened, capture verification has failed even if the command
exited successfully. Report the CLI version and selected device in the chat.

Screen capture produces the whole display. For an isolated component image,
use a deterministic Compose preview, discover its fully qualified name, and
render it without a device:

```sh
android preview list ':app@debug' --format=json
android preview render ':app@debug' --fqn='actual.package.ActualPreview' \
  --component-tree --output="$ARTIFACT_DIR/previews" --format=json
```

Choose the name returned by discovery. Inspect the PNG and component-tree JSON.
Follow the [architecture guide](architecture.md) for state ownership and
deterministic fixtures. If discovery finds no previews, add a working fixture
before rendering; an empty selection does not verify a component. Follow the
[testing guide](testing.md#compose-screenshot-tests) for marker screenshot
fixtures and native-map coverage limits. Do not treat a blank map preview as
native-map pixel coverage. Preview renders and ad hoc device captures supplement
the screenshot tests required by the project.

## Change device configuration

Use native emulator commands for creation, start, cold boot, and stop. For OS
settings without a CLI command, use narrowly scoped ADB operations on the
selected development device. Read the current values first:

```sh
adb -s "$ANDROID_SERIAL" shell getprop ro.build.version.sdk
adb -s "$ANDROID_SERIAL" shell wm size
adb -s "$ANDROID_SERIAL" shell wm density
adb -s "$ANDROID_SERIAL" shell settings get system font_scale
adb -s "$ANDROID_SERIAL" shell cmd uimode night
```

For example, use `adb -s "$ANDROID_SERIAL" shell cmd uimode night yes` for dark
mode or `adb -s "$ANDROID_SERIAL" shell settings put system font_scale 1.3`
for font scaling. Change one setting at a time, inspect the resulting UI, and
restore the exact original values and any display overrides when finished.

## Use current documentation and focused skills

For unfamiliar or changing Android APIs, search Google's Knowledge Base and
fetch a relevant result using its returned `kb://` URL:

```sh
android docs search 'Navigation 3 saved state'
android docs fetch 'kb://URL_FROM_SEARCH'
android skills list
android skills find 'Navigation 3'
```

Compare guidance with the project's dependency versions and architecture before
applying it. Use `android init` for agent setup when needed; read a relevant
skill before applying it and review any files setup or installation changes.
Preserve the project's state, Hilt, Navigation 3, and Ktor conventions.

When using Android Studio integration, run `android studio check` first and
confirm that it is connected to this checkout. Consult Google's
[command reference](https://developer.android.com/tools/agents/android-cli/commands)
for additional capabilities. Agree on new app, testing, or external-service
conventions before introducing them, as required by `AGENTS.md`.
