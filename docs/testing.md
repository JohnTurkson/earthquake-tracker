# Testing and verification

Choose checks based on the behavior and files changed. Agents must follow this
guide before committing and when reporting a change. Human contributors should
use the same checks. Use the [build guide](building.md) for JDK, Android SDK,
device, and Maps configuration.

## Choose the checks

| Change | Required verification |
| --- | --- |
| Documentation or instruction files only | Review the text and local links; run `git diff --check`. No Android build is needed. Prototype new device coordination instructions on the shared emulator. |
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

## Shared test emulator

All agents and worktrees on this host MUST use the same **Earthquake Project**
emulator (AVD ID `Earthquake_Project`), with the **Resizable** hardware profile,
**API 37**, and a **Google Play** system image. Reserve it exclusively for device
tests, UI design/mockups, screenshots, and other project validation. Do not create
per-agent instances or substitute another device while it is busy. Host-only
unit tests, builds, lint and Compose preview screenshot tests need no reservation.

Use the repository's [$acquire-test-emulator skill](../.agents/skills/acquire-test-emulator/SKILL.md)
to create it once and verify its configuration. That skill is shared with Claude
through the existing `.claude/skills` symlink. The workflow is **check, claim,
test, clean up, release**, using Android CLI/ADB commands directly; no shell or
Python helper script is required.

Check on demand as device work approaches. The skill resolves one ignored
`.earthquake-emulator.lock` in the primary checkout for every worktree and
subagent. If busy, continue implementation and host checks, then retry later;
an optional read-only waiter subagent can monitor longer waits. When unclaimed,
check boot completion, SDK 37, awake/unlocked state and launcher focus with the
skill's ADB queries, observing readiness twice at least three seconds apart.
An app's main screen is busy. Never navigate or change settings to make it free.

When ready to test, the parent creates that lock with the skill's shell noclobber
command and its unique chat/attempt token. Only one concurrent create can
succeed. A check followed by an ordinary file write is not sufficient. If another
agent wins, keep working and retry later; do not overwrite/delete its lock or
create another emulator. An empty lock is also busy. A readiness observation is
only a snapshot, never ownership.

After claiming, recheck HOME focus and use `android layout`'s default UI-idle
wait before testing. Keep the reservation throughout device work and gaps between
tool calls, even when Home is temporarily visible. Testing subagents use the
parent's token and run their device work sequentially; only the parent releases
the lock after all children finish. Do not start detached device work that
outlives it. Record settings before changing them.

Agents **MUST return the emulator to an unlocked, idle home screen before
releasing it**, including when tests fail or work is interrupted. Restore
scenario-specific settings and stop/join all their device work. Follow the skill's
cleanup commands: restore settings and display configuration, stop the project
app, wake/unlock, press Home, and verify stable idle home before removing only
their own reservation. Leave this emulator running for the next agent. If cleanup
fails, retain the reservation and report incomplete cleanup. Reservations never
expire automatically; coordinate with an interrupted owner before recovery.

The shared emulator's installed app and stored history must survive device runs.
`gradle.properties` sets
`android.injected.androidTest.leaveApksInstalledAfterRun=true` because AGP's
default cleanup uninstalls the target and test APKs. Keep this setting enabled;
do not clear app data or uninstall the shared app to prepare tests. Use
test-local databases and preference files for isolated fixtures. Install only a
build compatible with the existing Room schema and verify retained data after
device runs.

Device tests use `androidx.test.runner.AndroidJUnitRunner`, configured in
[the app module](../app/build.gradle.kts). Export the actual `ANDROID_SERIAL`
confirmed by `adb -s SERIAL emu avd name`, and pass `--device="$ANDROID_SERIAL"`
or `adb -s "$ANDROID_SERIAL"` on every device action. While reserved, run:

```sh
ANDROID_SERIAL="$ANDROID_SERIAL" ./gradlew :app:connectedDebugAndroidTest
```

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
Passing fixture-based UI tests alone does not establish runtime fetching,
offline screen behavior, refresh, or Maps authorization. The data-layer tests
below check persistence independently; the notification integration tests also
exercise the real refresh worker and repository with controlled network input.

## Test source sets

Keep unit tests and their helpers in `app/src/test/kotlin`, instrumented tests
in `app/src/androidTest/kotlin`, and Compose screenshot tests in
`app/src/screenshotTest/kotlin`.

Use the default source directories without custom `sourceSets` configuration.
Keep Kotlin helpers used by multiple suites once in `app/src/testFixtures/kotlin`.
The [app build script](../app/build.gradle.kts) enables AGP's built-in fixture
support with `testFixtures { enable = true }`. AGP automatically supplies these
fixtures to the app module's unit, instrumented, and screenshot tests; they are
excluded from the app APK. Helpers used by only one suite stay in that suite's
own source directory.

[EarthquakeMapFixtures](../app/src/testFixtures/kotlin/com/johnturkson/sandbox/ui/EarthquakeMapFixtures.kt)
and [EarthquakePreviewState](../app/src/testFixtures/kotlin/com/johnturkson/sandbox/ui/EarthquakePreviewState.kt)
serve instrumented and screenshot tests.
[EarthquakeMonitoringFixtures](../app/src/testFixtures/kotlin/com/johnturkson/sandbox/EarthquakeMonitoringFixtures.kt)
supplies the same event values to unit and instrumented tests.
Fixtures compile separately from production code, so
declare their required dependencies with `testFixturesImplementation`.
The preview state exposes `Destination.EarthquakeMapRoute`, whose `NavKey` supertype
requires `testFixturesImplementation(libs.androidx.navigation3.runtime)`.

Declare test-only dependencies in the matching test configuration.
Screenshot tests use `screenshotTestImplementation(libs.androidx.compose.ui.tooling)`,
which exports the preview annotations and parameter providers. They inherit the
app's Compose BOM. Instrumented tests still declare their own Compose and Ktor
BOMs because their separate runtime classpath needs those version constraints.
Check both compile and runtime dependency resolution before removing a platform
that appears duplicated.

## Earthquake data tests

[EarthquakeApiTest](../app/src/test/kotlin/com/johnturkson/sandbox/EarthquakeApiTest.kt)
uses Ktor `MockEngine` and deterministic JSON fixtures to verify typed earthquake
routes, query arguments, decoding, and failures without live requests.
Keep JSON inputs shared by unit and instrumented tests once in
[`app/src/testFixtures/resources`](../app/src/testFixtures/resources).
Both suites load them through
[EarthquakeJsonFixtures.read](../app/src/testFixtures/kotlin/com/johnturkson/sandbox/EarthquakeJsonFixtures.kt),
which reads classpath resources supplied by the built-in fixture dependency.
Suite-specific inputs, such as the API detail response, stay in that suite's
default resources directory. No asset copies or `sourceSets` overrides are needed.

[EarthquakeStorageTest](../app/src/androidTest/kotlin/com/johnturkson/sandbox/EarthquakeStorageTest.kt)
uses real Room with active Turbine collectors. Preserve coverage for new IDs,
duplicate IDs, newer/older revisions, retained history across rolling refreshes,
alias changes, filters, atomic rollback, and concurrent merges.
Keep a list collector active through a preferred-ID change, an offline refresh
failure, and the next successful refresh. Verify that unrelated prior events
remain in every populated result and known aliases resolve to one updated row.
Close and reopen the disk-backed database to verify retained records remain
readable through the read-only repository. Refresh-related storage tests execute
`EarthquakeRefreshWorker` with controlled network input, then observe Room across
successes and failures. Database reads
must not initiate network requests. A database reopen tests persistent storage;
it does not alone establish offline screen behavior or an actual device reboot.

[EarthquakeMigrationTest](../app/src/androidTest/kotlin/com/johnturkson/sandbox/EarthquakeMigrationTest.kt)
uses Room's `MigrationTestHelper` and tracked schema exports to validate the
upgrade paths and removal of obsolete item and product tables while retaining
earthquakes, aliases, refresh metadata, and alert history. The version 4-to-5
timestamp migration also checks ISO text storage and pre-epoch data retention.
Exercise the complete upgrade through version 6, including the version-5-to-6
item table removal, and reopen Room to verify retained records remain readable.
Storage tests cover nanosecond precision and chronological half-open queries.
Generate schema exports through Room; never hand-edit them.

Run these tests on the shared Earthquake Project emulator under the exclusive
reservation described above. Continue to run the normal build, formatting,
unit, and lint checks. Storage-only changes do not add UI states or screenshot
baselines.

## Map behavior and screenshot coverage

[EarthquakeLocationRepositoryTest](../app/src/test/kotlin/com/johnturkson/sandbox/EarthquakeLocationRepositoryTest.kt)
uses temporary Preferences DataStore files and coroutine test scopes to check
missing/incomplete data, persistence after reopening, stale fixes, and invalid
coordinates. Cancel and join the first store's scope before reopening its file;
never run two active DataStore instances for the same file. Use a controlled
failing `DataStore` to exercise read errors without changing device storage.

[EarthquakeMapTest](../app/src/androidTest/kotlin/com/johnturkson/sandbox/EarthquakeMapTest.kt)
uses a plain `ComponentActivity`, immutable item fixtures, and a hoisted
`CameraPositionState`.
It checks empty-map configuration, retained gestures, current marker callbacks
after reordering and coordinate changes, item removal/background clicks, and the
screen rendered from an `EarthquakePreviewState` data-class fixture with selection
callbacks.
It also verifies that stored initial coordinates apply once and that camera
state survives Compose saved-state restoration without being recentered by a
later location value. Use `StateRestorationTester` for that UI-owned state.
Wait for the hoisted `CameraPositionState` to receive native camera changes and
become idle before saving; Compose idle alone does not await SDK camera events.
`MapType.NONE` avoids relying on live geographic tiles for these behavior checks;
the native SDK checks still require online access on the shared emulator.
`EarthquakeActivityTest` separately checks real destination startup and recreation.
Maps Utils creates offscreen Compose roots to render marker bitmaps. Target the
native `MapView` with Espresso for startup assertions and projected map taps;
`onRoot()` is ambiguous once these rendering roots exist. Convert projected
coordinates to screen coordinates using the view's origin before injecting a tap.
Cover cluster focus after sheet measurement: the focused member must project
above the sheet, and inactive explorer controls must be absent while it is open.

## Compose screenshot tests

Build each component's fixture matrix from its rendering inputs. Cover distinct
rendered branches and their interactions, including simultaneous monitoring and
refresh states. Use representative light, dark, narrow, and large-text
configurations. Inputs with no visual effect can share a fixture. Arbitrary
coordinates, event counts, strings, and scroll offsets need representative
boundaries rather than every possible value.

[EarthquakeComponentScreenshotTest](../app/src/screenshotTest/kotlin/com/johnturkson/sandbox/EarthquakeComponentScreenshotTest.kt)
isolates the list, rows, and unavailable-event snackbar. Its list provider covers
empty, single-event, and multiple-event data with both title and supporting-pane
flags. Its row provider covers selection, available/missing time, and the map
arrow. Render each fixture in light, dark, and narrow large-text configurations.
The fallback-title provider renders real rows and selection cards with every
available/missing magnitude and place combination when the source title is absent,
in light, dark, and narrow large-text configurations. The snackbar uses the same
`EarthquakeEventSnackbar` as the runtime host, with fixed `SnackbarData` and no
running duration or queue.
Host queueing, timing, and dismissal are separate from these pixel fixtures.

The component state inventory is:

| Component | Required rendering cases |
| --- | --- |
| Map marker artwork | Selected and unselected. |
| Cluster badge | Available/missing magnitude, small/large count, dark, and large text. |
| Cluster sheet content | Empty, populated, selected, missing magnitude/time, and the last scrolled member; include short windows. |
| List | Empty/single/multiple events with each title/supporting-pane presentation; monitoring present and absent in parent fixtures. |
| List row | Selected/unselected, available/missing time, map arrow shown/hidden, and fallback titles with available/missing magnitude and place. |
| Selection card | Available/missing time, wrapped text, More details shown/hidden, and fallback titles with available/missing magnitude and place. |
| Earthquake details | Populated/missing measurements, loading/unavailable, source rows after scrolling, dark, RTL, narrow large text, expanded left-pane replacement, report expansion, and Close. |
| View switcher | Map/List selected with labels and with icons only, including dark and large text. |
| Explorer controls | Compact Map/List, expanded open/minimized panel, notification pane, empty/populated selection, narrow/wide/RTL placement, and Locate placement. |
| Location button and rationale | Request/settings labels and the rationale, with light/dark and large text. |
| Monitoring card | Each status, each refresh outcome, available/missing update time and stored data, and concurrent checking/monitoring/failure/offline with refresh work. |
| Notification page and rationale | Checking, denied/settings permission, alerts on/off, channel availability, each magnitude threshold and monitoring status, enabled/disabled preview, settings error, and rationale. |
| Notification bell | Its ordinary appearance and placement through the explorer fixtures. |
| Event snackbar | Visible unavailable-event message with normal and wrapped text. |

[EarthquakeClusterScreenshotTest](../app/src/screenshotTest/kotlin/com/johnturkson/sandbox/EarthquakeClusterScreenshotTest.kt)
covers cluster badges with and without magnitude, small and large counts, dark
mode, and large text. It renders real sheet content for populated, selected,
empty, unknown-magnitude, dark, narrow large-text, and short large-text states.
[EarthquakeClusterSheetTest](../app/src/androidTest/kotlin/com/johnturkson/sandbox/EarthquakeClusterSheetTest.kt)
checks the real modal container, scrolling to the last member, retained selection
after dismissal, back dismissal through the real screen, and disappearing members.
Back-dismissal fixtures explicitly cover compact and expanded windows: restore
the compact selection card or expanded app bar and selected row as appropriate.
It measures the content cap and checks last-member access across compact and
medium height boundaries, including the former 600 dp threshold. The compact
and medium height content previews cover the corresponding bounded viewports;
the device test verifies the window-dependent sizing policy.
[EarthquakeClusterStateTest](../app/src/androidTest/kotlin/com/johnturkson/sandbox/EarthquakeClusterStateTest.kt)
checks serialization and restoration of the ViewModel's cluster member IDs,
independent event selection, numeric magnitude mapping from Room, and cluster
dismissal when a notification opens another event. Use test-local in-memory
Room data and a MockEngine client; run it with the offline UI selection below.
Extend native map behavior tests with
coincident pairs at maximum zoom, current members after updates, zoom splitting,
and selected marker interaction. Use deterministic dense fixtures, including
identical coordinates, from `EarthquakeMapFixtures.denseItems`. These synthetic
events are test inputs, not reported earthquake activity.

On the shared Resizable API 37 emulator, inspect native cluster counts and
individual markers at multiple zoom levels, open and scroll the actual modal
sheet, select an event, and verify attribution above the sheet. Include dark and
large-text configurations and an expanded window. Host sheet-content references
do not cover the SDK surface, the modal scrim, or its drag handle; inspect those
in actual device captures and report the native pixel-regression gap.

[EarthquakeLocationScreenshotTest](../app/src/screenshotTest/kotlin/com/johnturkson/sandbox/EarthquakeLocationScreenshotTest.kt)
covers the request button, app-settings button, and permission rationale using
the real stateless components, including dark and large-text fixtures. It also
renders the actual controls layer with Locate in compact, open expanded, and
minimized expanded windows, plus compact selected, dark, and narrow large-text
layouts with live monitoring. `EarthquakeViewsTest` verifies that the bottom-right
location action does not move the monitoring banner or cover the selection card.
These fixtures do not substitute for native-map pixels. Keep Android permission
dialogs out of host previews. On the Resizable API 37 device,
exercise first request, denial, rationale, grant, settings return/revocation,
foreground-only updates, the native SDK location layer, and the bottom-right
Locate action before and after permission is granted. Device captures
supplement the native-map regression coverage limitation described below.

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
covers the ordinary Compose explorer visuals: compact List,
selected and empty List, dark mode, large text at narrow width, the Map-selected
switcher, rows with no time, and selected-event cards with and without time.
Isolated Map-selected and List-selected switcher previews cover light, dark,
and large-text configurations, including the selection fill at the outer edges.
List previews render the real `EarthquakeListScreen` destination content. Use deterministic
snippet strings for time labels in pixel fixtures so host time zones do not
change their pixels. These images do not cover the native map surface.

[EarthquakeExplorerControlsScreenshotTest](../app/src/screenshotTest/kotlin/com/johnturkson/sandbox/EarthquakeExplorerControlsScreenshotTest.kt)
renders the actual controls layer independently of the native map. Cover the
compact map switcher with no selection, selected events with and without time,
dark mode, and large text. Cover the expanded floating panel with empty and
populated items, selected and unselected events, missing time, dark mode, and
large text. Cover the minimized expanded state with no selection, a selected
event with and without time, dark mode, and large text. Verify the fixed Minimize
control, Show list button, and reuse of the bottom card. The fixture's neutral
background only makes overlay bounds visible; it is not a substitute map or
coverage of map pixels. Do not render blank native map previews to claim coverage
of these controls.
Include narrow icon-only controls, a wider compact window, and right-to-left
placement so the bell stays at the logical end while the switcher stays
centered. Cover both selected icons, dark mode, and large text in the switcher's
own previews; resizing must preserve accessible names and selection.
[EarthquakeViewSwitcherTest](../app/src/androidTest/kotlin/com/johnturkson/sandbox/EarthquakeViewSwitcherTest.kt)
checks label removal and restoration, icon accessibility names, and selection
callbacks across width changes. It also checks the switcher's center and the
bell's logical end alignment and click action in compact, expanded, minimized,
and right-to-left layouts.

[EarthquakeViewsTest](../app/src/androidTest/kotlin/com/johnturkson/sandbox/EarthquakeViewsTest.kt)
checks deterministic explorer component selection, compact switching, and
expanded panel minimize/restore behavior.
[EarthquakeNavigationTest](../app/src/androidTest/kotlin/com/johnturkson/sandbox/EarthquakeNavigationTest.kt)
renders the real Nav3 host and Material adaptive scene with deterministic
entry content. Use separate entry-scoped test ViewModels to verify result
delivery, hidden-recipient restoration, row reveal, Notifications return, and
compact/expanded resizing without substituting for native map coverage.
Run this class with the reserved emulator window at least 900 dp wide and 600 dp
tall. Its host uses Compose's `DeviceConfigurationOverride.WindowSize` so both
content constraints and `LocalWindowInfo` reflect each fixture size. Include the
839/840 dp width and 479/480 dp height boundaries and verify entry owners survive
resizing. Verify expanded pane bounds stay below the app bar so its controls
remain available through Android accessibility selectors. Compare their screen
coordinates because levitated panes use separate windows. Keep fixture sizes
within the available window to retain the device's native density while
exercising the Material scene's lookahead layout.
Record and restore the display override through the
[Android CLI workflow](android-cli.md#change-device-configuration).
Freeze the Compose clock to check notification navigation before an ordinary
slide could finish: the replaced pane must disappear and the restored pane must
already have its settled bounds. Cover compact Map and List, expanded open and
minimized List, Close and Back, and verify Map/List switching still animates
after returning from Notifications. Existing screenshot fixtures cover the
settled destination appearances; timing assertions cover their transitions.


[EarthquakePaneScreenshotTest](../app/src/screenshotTest/kotlin/com/johnturkson/sandbox/EarthquakePaneScreenshotTest.kt)
renders the real adaptive scaffold through the same `EarthquakePaneOverrides`
wrapper used by the navigation host, with the notification and list surfaces in
light, dark, and large-text configurations, plus the compact
notification fallback. Its solid background exposes unwanted shading in the
transparent pane margins; it is not a map fixture.
[EarthquakePaneTest](../app/src/androidTest/kotlin/com/johnturkson/sandbox/EarthquakePaneTest.kt)
checks actual device pixels above the floating notification card, interaction
with the underlying destination, Close and Back, and saved notification state
across compact/expanded changes. Use `DeviceConfigurationOverride.WindowSize`
for those fixtures and run it with the same minimum 900 dp by 600 dp device
window required by the navigation tests.

[EarthquakeDestinationStateTest](../app/src/androidTest/kotlin/com/johnturkson/sandbox/EarthquakeDestinationStateTest.kt)
uses the real Map and List ViewModels with test-local Room data to check
independent selection, saved result revisions, pending scroll restoration,
stale-result rejection, clearing, and repeated selection of the same event.
Receiving a result must not publish another one or erase a newer request.

[EarthquakeDetailsTest](../app/src/androidTest/kotlin/com/johnturkson/sandbox/EarthquakeDetailsTest.kt)
uses the real Details ViewModel and test-local Room to verify alias lookup,
observable source revisions, unavailable records, late arrivals, and zero felt
reports. No runtime API request is needed to read details. Extend
`EarthquakeNavigationTest` with explicit map-card/list-arrow entry, return to the
retained List, replacement of List by Details in the expanded left pane, list
minimize/restore, selection changes while open, Notifications return, saved-state
restoration, and resizing. Ordinary row/marker selection must not open details.

[EarthquakeDetailsScreenshotTest](../app/src/screenshotTest/kotlin/com/johnturkson/sandbox/EarthquakeDetailsScreenshotTest.kt)
renders the real card, header, row action, and expanded panel components. Supply a
deterministic update label to `EarthquakeDetailsCard`, and include its lower
source rows with a hoisted initial list position. Capture loading/unavailable
screen wrappers separately. The neutral expanded fixture background exposes pane
bounds without claiming coverage of native Maps pixels. On the shared emulator,
verify the real same-left-pane arrangement, map attribution and location
control clearance, both detail entry points, and retained camera/list state.

Reserve the shared Earthquake Project emulator and use Android CLI to verify
the runtime destinations across the 840 dp width breakpoint. Include selection
and camera retention, automatic row reveal, list scroll restoration, the compact
view preference, notification Back/Close behavior, and unobscured native-map
attribution in open and minimized panel states. Exercise activity recreation as
well as switching destinations. The ordinary Compose screenshot fixtures render
the real `EarthquakeListScreen`, `EarthquakeListPanel`, controls, and notification
page; keep their full supported-state coverage when changing entry wrappers.

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
temporary device configuration and verify idle-home cleanup before release.
These checks and preview renders supplement
automated tests; they do not establish screenshot regression baselines or satisfy
missing coverage by themselves.

Report formatting, build, lint, unit tests, device tests, and manual checks
separately in the chat, identifying what passed, failed, or was not run. Include
the command or scenario, any relevant device/API level, and material limitations.
For a documentation-only change, reporting link and whitespace checks is
sufficient.

After running checks, inspect `git status` for generated output and unrelated
changes before staging. Follow the [source control guide](source-control.md).

## Notification and refresh coverage

`EarthquakeRefreshRepositoryTest` uses real Room and Ktor MockEngine for exception
propagation, retained data after failed requests, atomic rollback, cancellation,
and separate post-commit alert delivery. Its transaction-failure fixture verifies
that both event rows and refresh metadata roll back. `EarthquakeWorkSchedulerTest` uses real
WorkManager with delayed refresh work to verify duplicate requests keep the
existing request and stopping monitoring leaves it queued. It checks fresh
failed outcomes through the same `WorkInfo` Flow used by the screen and verifies
that a monitoring tick enqueues the shared refresh worker. Run these tests with
device internet disabled under the shared reservation. MockEngine fixtures
provide controlled responses without live earthquake feed requests.
`EarthquakeApiTest` uses Ktor MockEngine with real feed-shaped JSON.
`EarthquakePollingLoopTest` advances a coroutine test scheduler through minute
boundaries, slow requests, offline pauses, reconnection, and cancellation;
no real-minute sleeps are needed. Supply its monotonic clock from that scheduler.
`EarthquakeAlertPolicyTest` covers magnitude and age boundaries and revisions;
`EarthquakeAlertDeliveryTest` covers delivery deduplication and suppressed posts.

On the Resizable API 37 emulator, `EarthquakeMonitoringStorageTest` checks durable data and
alert history after reopening, transaction rollback, alias-aware alert deduplication, retained history after empty feeds, and the additive version-2-to-3 migration.
`EarthquakeRefreshWorkerTest` uses TestListenableWorkerBuilder with a controlled
refresher to check save-before-alert order, propagation of refresh exceptions and
cancellation, and best-effort notification failure isolation. Calling `doWork()`
directly does not run WorkManager's exception-to-failed-state handling; assert
those exceptions in builder tests and assert `WorkInfo.State.FAILED` through the
real WorkManager integration tests. The UI has one generic refresh failure,
without separate network, decoding, or storage outcomes.
`EarthquakeNotificationsTest` queries the actual NotificationManager channels,
rechecks registration, compares capabilities to Android's current runtime grant,
and inspects posted notifications, distinct event PendingIntents, foreground
channel, and Stop action. Run this class with notification permission granted and
revoked externally to cover both platform states without killing instrumentation
while it is running. Restore the original grant afterward and hold the shared
emulator reservation throughout platform notification checks.

`EarthquakeNotificationUiTest` exercises page capability changes, actions,
threshold selection, disabled preview, refresh progress/failure, and the fixed
expanded status across list scrolling and minimize/restore. It checks the compact
and expanded bars remain 64 dp for every monitoring/refresh combination. Compact
List retains its scrolling card and restored list position. The
`EarthquakeNotificationsScreenshotTest` providers enumerate monitoring and page
visual states, with narrow/large-font, dark, scrolled, expanded right-pane, and
minimized fixtures. Review every provider-generated image as well as named
previews. Host screenshots cover our controls, not native map pixels or Android's
permission dialog and notification drawer.
The monitoring provider includes concurrent refresh and session statuses, with
no data, stored data without a timestamp, and a known update time. Preserve those
combinations when extending the state contract. Render the monitoring provider
in the compact floating bar and expanded app bar, including dark mode and large
text. Include the expanded Minimize and Show list presentations, and visually
verify one line at the same 64 dp height. The scrolled page-action provider
keeps enabled preview, disabled preview, and settings-error pixels visible rather
than relying on controls below a preview's viewport. Include checking with an
already-selected alerts toggle and the compact notification screen's placement.

Also manually exercise a real foreground session, Stop, offline/reconnection,
notification permission denial/settings return, channel settings, event taps,
and compact/expanded resizing with selected events. Confirm that worker state,
foreground notification, and the displayed snapshot agree. Do not treat virtual
clock tests as evidence that Android will grant indefinite runtime or exact
minute delivery.

Run the data, notification, UI and offline-worker checks with internet access
disabled under the shared reservation. Production activity startup schedules a
refresh; disabling internet prevents those checks from calling live earthquake feed
endpoints. API/repository tests use MockEngine. Run native-map checks separately
online; the complementary selections exercise the complete device suite:

```sh
ANDROID_SERIAL="$ANDROID_SERIAL" ./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.notClass=com.johnturkson.sandbox.EarthquakeMapTest
```

Restore online access, then run:

```sh
ANDROID_SERIAL="$ANDROID_SERIAL" ./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.johnturkson.sandbox.EarthquakeMapTest
```

The offline WorkManager integration test asserts its network precondition and
leaves connectivity changes to the parent reservation owner. It verifies the
offline foreground title when notifications are permitted, and that Stop removes
foreground status while an ordinary alert remains. Restore recorded
network settings after manual Maps-tile and real monitoring checks, then complete
the shared idle-home cleanup.

`EarthquakeRefreshIntegrationTest` builds the actual refresh CoroutineWorker with
WorkManager's test builder, the real refresh repository and Room database,
Preferences DataStore, and a MockEngine earthquake feed. It checks history
retention, preferred-ID changes, alert deduplication/permission gating, and a
network failure that preserves rows and refresh metadata. Run it with ordinary
notification permission granted and denied. Platform notification assertions
wait for the expected active tags because Android enqueues posts asynchronously.

`EarthquakeReportImageTest` uses Ktor MockEngine and a generated PNG to verify
that reopening an image downloads it again, both caches remain absent, a failed
load can retry, and the full-size source action receives the correct URL.
`EarthquakeReportScreenshotTest` uses Coil's inspection-mode preview handler for
deterministic loading, error, and success fixtures. Its synthetic image verifies
component layout rather than a particular USGS report's pixels. Include dark and
narrow large-text configurations and expanded/collapsed report sections. Manual
verification of `EarthquakeReportPrototypeActivity` checks actual published
ShakeMap and DYFI images separately; automated tests must not call live USGS.
