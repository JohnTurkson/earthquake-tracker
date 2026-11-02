# Testing and verification

Choose checks based on the behavior and files changed. Agents must follow this
guide before committing and when reporting a change. Human contributors should
use the same checks. Use the [build guide](building.md) for JDK, Android SDK,
device, and Maps configuration.

## Choose the checks

| Change | Required verification |
| --- | --- |
| Documentation or instruction files only | Review the text and local links; run `git diff --check`. No Android build is needed. |
| Kotlin source, resources, manifest, dependencies, or build configuration | Run formatting checks, assemble the debug app, run debug unit tests, and run Android lint. |
| UI, navigation, lifecycle, Android storage, or platform integration | Also run relevant device tests and manually exercise the changed behavior on the resizable API 37 emulator described below. |
| Maps configuration or rendering | Also follow the Maps checks in [Verify the run](building.md#verify-the-run). |

Run the standard checks from the repository root:

```sh
./gradlew :ktfmtCheck :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

`ktfmtCheck` already participates in the Gradle `check` tasks. The explicit
command above makes the intended checks clear. Formatting and lint are separate
from tests; compilation alone does not exercise behavior.

Use the configured ktfmt integration to format files you changed before staging.
Never format generated files or build/cache directories. The repository-wide
`ktfmtFormat` task can also rewrite unrelated files; review its scope before
using it and keep unrelated cleanup in a separate commit.

Use an API 37 emulator with the **Resizable** hardware profile for device tests,
manual UI verification, and device screenshots. Medium Phone and other fixed-size
profiles do not meet this requirement. Follow the
[Android CLI device-selection workflow](android-cli.md#select-a-device) to confirm
the hardware profile and API level before testing. If the required AVD is missing,
create it rather than substituting an already-running fixed-size emulator. Other
devices and API levels may supplement this required test environment.

For device tests, start and select that emulator, then run:

```sh
./gradlew :app:connectedDebugAndroidTest
```

Device tests use `androidx.test.runner.AndroidJUnitRunner`, configured in
[the app module](../app/build.gradle.kts). Select the device explicitly, for
example by setting `ANDROID_SERIAL` to its actual serial when running Gradle.
See Android's [instrumented test setup](https://developer.android.com/training/testing/instrumented-tests)
and [command-line testing guide](https://developer.android.com/studio/test/command-line).

## What counts as passing

- The command exits successfully, and the intended tasks and tests were selected
  for the current change. A task with `NO-SOURCE`, zero discovered tests, or only
  skipped tests does not demonstrate that behavior was tested.
- Test results show the expected cases with no failures or errors. Explain
  unexpected skips. Gradle's valid cached or up-to-date results can be reused
  for unchanged inputs; an old report by itself is not evidence for new changes.
- State the scope of filtered runs. A passing test selected with `--tests`
  verifies that selection, not the full unit suite.
- A failed check remains failed until resolved. If it predates the change,
  establish that with evidence and report it separately; do not describe the
  full check set as passing. Report flaky failures even if a retry passes.
- Missing SDKs, dependencies, credentials, devices, or runners mean the affected
  check is blocked or not run. Record the reason and remaining verification;
  committing the code does not make that verification complete.
- A manual check passes only when its expected behavior was observed. App launch,
  activity recreation, and map rendering are separate claims.

For local unit tests, inspect `app/build/reports/tests/testDebugUnitTest/` or
`app/build/test-results/testDebugUnitTest/`. Device reports are under
`app/build/reports/androidTests/connected/`, with XML results under
`app/build/outputs/androidTest-results/connected/`. Report test counts when a
suite ran and the results are available.

## Test meaningful behavior

Add regression coverage for a bug or a new behavior where an automated test
provides useful protection. Assert observable outcomes and include relevant
failure and boundary cases. Use controlled clocks, dispatchers, and network
responses where needed for deterministic tests. Do not weaken assertions or
disable tests to obtain a green result.

Documentation and other reversible, low-impact changes do not need tests that
merely repeat the implementation.

The current suite covers response decoding with a test-local fixture, activity
startup and recreation, map behavior, and marker artwork. Keep tests focused on
observable behavior rather than unconditional assertions or empty content.
Passing these tests does not establish runtime earthquake fetching, offline
behavior, refresh, or Maps authorization. Add feature coverage as those behaviors
are implemented.

## Map behavior and screenshot coverage

[EarthquakeMapTest](../app/src/androidTest/kotlin/com/johnturkson/sandbox/EarthquakeMapTest.kt)
uses a plain `ComponentActivity`, immutable item fixtures, and a hoisted
`CameraPositionState`.
It checks empty-map configuration, retained gestures, current marker callbacks
after reordering and coordinate changes, item removal/background clicks, and the
screen rendered from an `EarthquakePreviewState` data-class fixture with selection
callbacks.
`MapType.NONE` avoids relying on live geographic tiles for these behavior checks;
it still initializes Google's native SDK and needs a compatible device.
`EarthquakeActivityTest` separately checks real destination startup and recreation.

## Compose screenshot tests

Use Google's Compose Preview Screenshot Testing. The current AGP
9.1.1 project uses the compatible `com.android.compose.screenshot` plugin and
`screenshot-validation-api` version 0.0.1-alpha16. Its experimental source-set
flags are configured in `gradle.properties` and the app module. Newer AGP test-suite
integration requires a separate build-version change.

Mark test previews with `@PreviewTest` and `@Preview` in
`app/src/screenshotTest/kotlin/`. The working example is
[EarthquakeMapMarkerScreenshotTest](../app/src/screenshotTest/kotlin/com/johnturkson/sandbox/EarthquakeMapMarkerScreenshotTest.kt).
It covers every marker artwork state: the small red unselected dot and larger
blue selected dot, both with a white border. The preview background and 48 dp
bounds are fixed; no network, database, dynamic color, or clock is involved.

[EarthquakeViewsScreenshotTest](../app/src/screenshotTest/kotlin/com/johnturkson/sandbox/EarthquakeViewsScreenshotTest.kt)
covers the ordinary Compose explorer visuals: compact and expanded List,
selected and empty List, dark mode, large text at narrow width, the Map-selected
switcher, rows with no time, and selected-event cards with and without time.
List previews render the real `EarthquakeScreen` List branch. Use deterministic
snippet strings for time labels in pixel fixtures so host time zones do not
change their pixels. These images do not cover the native map surface.

[EarthquakeViewsTest](../app/src/androidTest/kotlin/com/johnturkson/sandbox/EarthquakeViewsTest.kt)
checks selecting an event from List, opening it on Map, retaining its highlight
when switching back, and clearing the shared selection. Exercise window resizing
and activity recreation on a device as well as switching presentations.

Generate references only for an intentional visual change:

```sh
./gradlew :app:updateDebugScreenshotTest
```

Open and visually review every generated PNG under
`app/src/screenshotTestDebug/reference/` before staging it. Commit reviewed
references alongside the component and tests. Inspect the diff when replacing
existing references; do not regenerate merely to hide an unexpected failure.
Validate without changing references:

```sh
./gradlew :app:validateDebugScreenshotTest
```

Check that the intended preview tests were discovered and passed, rather than accepting
an empty suite. The HTML report is under
`app/build/reports/screenshotTest/preview/debug/`. Keep reports, actual renders,
and diff images in ignored `build/`. Reference PNGs are intentionally tracked
regression inputs, rather than build artifacts. Matching host/tool versions
matter for pixel reproducibility; report rendering differences instead of
silently relaxing comparisons.

The native-map integration state matrix is empty items, populated/unselected
items, and populated/selected items. Maps Compose 9.0.1 intentionally omits the
native map surface and marker placement in preview inspection mode. Do not
substitute a fake renderer or approve blank main-screen/map previews as native
pixel coverage. Our marker artwork is tested directly with the same composable
used by the SDK. Use device tests and manual captures to verify native
integration; native-map screenshot regression coverage remains missing.

## Report the results

Report task-specific activity and verification results in the chat. Do not create
or append repository files containing session journals, dated verification
reports, research activity, command histories, or results from a particular run.
Keep project and tool guides focused on reusable prerequisites, commands, usage
instructions, passing criteria, and troubleshooting, with concrete source
examples where useful.

Keep generated logs, test reports, exploratory screenshots, and other run
artifacts in ignored `build/` or a temporary directory. Automated test sources,
deterministic fixtures, and reviewed screenshot reference baselines remain
tracked project inputs under their established conventions.

Before finishing, compare the change against the applicable project guides and
report any unmet requirements, including missing screenshot coverage for UI
changes. Follow the [architecture guide's component requirements](architecture.md#ui-and-state-ownership)
when assessing screenshot coverage.

For exploratory device checks, use the [Android CLI workflow](android-cli.md) to
inspect live hierarchy, perform selector-based actions, and visually review PNGs.
Capture the resulting state after meaningful actions. Include the CLI version,
device/API, scenario, observed result, and artifact paths in the chat. Restore
temporary device configuration. These checks and preview renders supplement
automated tests; they do not establish screenshot regression baselines or satisfy
missing coverage by themselves.

Report formatting, build, lint, unit tests, device tests, and manual checks
separately in the chat, identifying what passed, failed, or was not run. Include
the command or scenario, any relevant device/API level, and material limitations.
For a documentation-only change, reporting link and whitespace checks is
sufficient.

After running checks, inspect `git status` for generated output and unrelated
changes before staging. Follow the [source control guide](source-control.md).
