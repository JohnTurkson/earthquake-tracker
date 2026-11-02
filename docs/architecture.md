# Architecture and state management guidelines

Build on the concrete patterns already present in this project. These conventions
apply to agents and human contributors. Each section links to the implementation
that establishes the pattern and explains how to extend it.

Name app and feature types with the `Earthquake` prefix, as in `EarthquakeApp`,
`EarthquakeApplication`, `EarthquakeModule`, and `EarthquakeScreen`. Use the same
name for a type's Kotlin file. Keep technical names such as `MainDispatcher` tied
to the platform concept they describe.

The explorer observes stored earthquakes through `EarthquakeRepository`.
WorkManager updates the same Room database through `EarthquakeRefreshRepository`.
Fixed fixtures remain for previews and tests. Location and notification preferences
use the shared Preferences DataStore.

## UI and state ownership

Keep screen state and state transitions in the ViewModel. Keep rendering,
layout state, locale formatting, and UI lifecycle integration in the UI. UI
actions call ViewModel methods or callbacks; rendered components do not access
repositories, storage, or network clients.

Use read-only interfaces to describe the state consumers can observe.
[EarthquakeState](../app/src/main/kotlin/com/johnturkson/sandbox/ui/EarthquakeState.kt)
is annotated `@Stable`, and
[EarthquakeViewModel](../app/src/main/kotlin/com/johnturkson/sandbox/ui/EarthquakeViewModel.kt)
implements it.
[EarthquakePreviewState](../app/src/testFixtures/kotlin/com/johnturkson/sandbox/ui/EarthquakePreviewState.kt)
provides a test implementation without a ViewModel. Follow that interface and
implementation pattern when expanding screen state, and honor the stability
contract when using `@Stable`.

`EarthquakeState` describes the Map destination: read-only items, selected event and
cluster IDs, location values, monitoring, and pending camera focus.
[EarthquakeListState](../app/src/main/kotlin/com/johnturkson/sandbox/ui/EarthquakeListState.kt)
is implemented by its own
[EarthquakeListViewModel](../app/src/main/kotlin/com/johnturkson/sandbox/ui/EarthquakeListViewModel.kt)
and deterministic
[EarthquakeListPreviewState](../app/src/screenshotTest/kotlin/com/johnturkson/sandbox/ui/EarthquakeListPreviewState.kt).
[EarthquakeNotificationsState](../app/src/main/kotlin/com/johnturkson/sandbox/ui/EarthquakeNotificationsState.kt)
is implemented by
[EarthquakeNotificationsViewModel](../app/src/main/kotlin/com/johnturkson/sandbox/ui/EarthquakeNotificationsViewModel.kt).
Each destination owns its state independently in its decorated navigation entry.
Map and List observe the same repository data and communicate selection changes
through navigation results. Do not introduce a shared selection ViewModel or a
second selected-ID copy in a composable.

`applySelection` accepts an `EarthquakeSelectionResult`, ignores older or repeated
revisions, and updates only the receiving destination. Receiving a result must
not publish another result. Local actions publish through the navigation host,
then apply that revision locally without requesting their own scroll or camera
movement. Rendered components continue to receive values and action callbacks.

[EarthquakeScreen](../app/src/main/kotlin/com/johnturkson/sandbox/ui/EarthquakeScreen.kt)
keeps lifecycle integration at the screen entry and passes values and actions into
[EarthquakeMap](../app/src/main/kotlin/com/johnturkson/sandbox/ui/EarthquakeMap.kt).
Keep lifecycle integration at that entry when a feature needs it. Pass values
or read-only models, callbacks, and a `Modifier` into rendered components:

```kotlin
EarthquakeMap(
    items = state.mapItems,
    onItemClick = onMapItemClick,
    selectedItemId = state.selectedMapItemId,
    modifier = modifier,
)
```

`EarthquakeMapItem` is an immutable presentation value with a stable unique ID,
coordinates, title, optional numeric magnitude, optional snippet, and optional
`Instant` event time. The explorer uses the same values for its map and list. Format
event times in the UI using the device's locale and time preferences; do not
store formatted time strings as feature state. It has no network or database
annotations. Display magnitude labels without a space after `M`, as in `M5.0`
and `Max M5.0`. `EarthquakeDatabaseModel.toMapItem` removes the source title's
leading magnitude space for map, list, selection-card, and cluster-sheet titles;
keep stored source titles unchanged. Maps Utils owns unselected marker lifetimes
through immutable
[EarthquakeClusterItem](../app/src/main/kotlin/com/johnturkson/sandbox/ui/EarthquakeClusterItem.kt)
wrappers. The separately rendered selected marker is keyed by its event ID;
`rememberUpdatedMarkerState` applies coordinate changes without turning the SDK
marker into feature state.
Marker clicks emit the current item and map-background clicks emit a callback.
The ViewModel owns selection; the component derives marker color and stacking
from `selectedItemId` and suppresses the SDK's independent info-window selection.
`MarkerComposable` renders the real
[EarthquakeMapMarker](../app/src/main/kotlin/com/johnturkson/sandbox/ui/EarthquakeMapMarker.kt)
content into the SDK icon. Include selection in its render keys so the bitmap
updates. Marker artwork consumes only `isSelected` and a `Modifier`; the SDK
marker owns hit testing, while the ViewModel owns the selected ID.

Use Maps Compose Utils `Clustering` inside `EarthquakeMap`, with Google's default
distance-based algorithm. It handles item changes and reclustering after camera
movement; do not replace the map's native listeners manually. Set the renderer's
minimum cluster size to two so coincident pairs remain accessible even at maximum
zoom. Render exact counts and available maximum magnitude with
[EarthquakeClusterMarker](../app/src/main/kotlin/com/johnturkson/sandbox/ui/EarthquakeClusterMarker.kt).
Compute magnitude from numeric values, ignoring missing or non-finite values;
do not parse localized titles. Individual unselected items use the existing red
marker. Exclude the selected item from clustering and draw it separately in blue
with a higher z-index, so every earthquake is represented once and selection stays visible.
Anchor cluster badges above their map point with transparent clearance for the
blue marker, so coincident selection cannot cover the count or magnitude.

Cluster clicks emit immutable earthquake values, never SDK clusters. Save only
the member IDs in `EarthquakeViewModel.selectedClusterItemIds` using the existing
`SavedStateHandle.saved` and `MutableStateSerializer<List<String>>` pattern.
Resolve those IDs against current items in the UI; keep the original membership
while the sheet is open even when the camera moves. `selectMapCluster` opens it,
`dismissMapCluster` closes it without clearing event selection, and
`clearMapSelection` clears both. Missing members produce the sheet's empty state.

Follow Maps Compose naming: use `cameraPositionState` for the component parameter
and local variables of type `CameraPositionState`, created by
`rememberCameraPositionState`. It describes the map's center, zoom, tilt, and
bearing. Pass it directly to `GoogleMap(cameraPositionState = cameraPositionState)`.

Camera state, projection, marker, and other SDK rendering objects belong in the UI.
The rendering overload of `EarthquakeScreen` also accepts a hoisted
`CameraPositionState`, defaulting to its own remembered state. Tests can observe
that same UI owner when verifying saved-state restoration.
The component accepts a hoisted `CameraPositionState`, `MapProperties`,
`MapUiSettings`, and content padding. Its remembered default camera restores
through Maps Compose's saver. Camera changes are UI state and must not be copied
into the ViewModel. The main screen fills its destination, including
behind system bars, and passes safe-drawing padding for Google attribution.
The fallback camera starts over San Francisco at zoom 10. The ViewModel reads the
last known user location once through its repository and exposes primitive
coordinates in `initialMapLocation`. A null value means the initial read is
pending. The UI applies the result once; its saveable initialization flag keeps
later location readings from replacing a restored camera, a gesture, or an
explicit list-row camera move. SDK camera objects stay in the UI.

Map defaults enable the
SDK's My Location button while hiding other optional controls and retaining
gestures. The button appears only when the caller enables the My Location layer
with granted location permission; enabling its UI setting does not request
permission. Keep Google attribution visible when adding overlays.

Location permission belongs to the screen entry. Use Compose's
`rememberLauncherForActivityResult` with `ActivityResultContracts.RequestPermission`
for `ACCESS_COARSE_LOCATION`; the earthquake map does not require precise or
background access. Android owns the permission grant. Observe it in UI state and
refresh it with `LifecycleResumeEffect`, rather than persisting a granted flag in
DataStore, the ViewModel, or saved state. Save only transient UI decisions such as
whether a request was attempted or a rationale dialog is open.

[EarthquakeLocationButton](../app/src/main/kotlin/com/johnturkson/sandbox/ui/EarthquakeLocationButton.kt)
requests access from a user action and remains available to center the map after
permission is granted. The explorer disables the SDK's My Location button to
keep a single app-owned location control.
[EarthquakeLocationRationale](../app/src/main/kotlin/com/johnturkson/sandbox/ui/EarthquakeLocationRationale.kt)
explains a repeat request. When Android will no longer show the request, the
button opens app settings. Keep rendering components independent of permission
launchers: pass granted/settings values and action callbacks from the entry.
`EarthquakeExplorerControls` anchors the app button at the bottom end, clear of
safe-drawing insets, in both map layouts. Place the selection card above it and
measure their combined height for native map padding and attribution. The
monitoring banner's top spacing is independent of location permission. Denial
leaves map and list exploration available.

Use `LifecycleStartEffect` at the screen entry to start updates only while the
screen is started and permission is granted, and cancel on stop or disposal.
[EarthquakeLocationProvider](../app/src/main/kotlin/com/johnturkson/sandbox/data/EarthquakeLocationProvider.kt)
wraps the injected Fused Location Provider in a cold `callbackFlow` and removes
its callback in `awaitClose`. It uses balanced power, a requested 60-second
interval, and a 1,000-meter minimum displacement. Actual delivery depends on
Android and available location sources. The SDK layer displays live location;
the app's location stream updates the durable cache without following every fix
with the camera. An explicit app Locate request centers once when a measured
location is available; subsequent fixes leave the camera alone.

Use fixed development inputs such as
[EarthquakeMapFixtures](../app/src/testFixtures/kotlin/com/johnturkson/sandbox/ui/EarthquakeMapFixtures.kt)
to render deterministic map states without live API or storage access. Pass only
the presentation values needed by markers: ID, coordinates, title, numeric magnitude, and an
optional event time or snippet. Keep these values independent of network and
database models, and supply runtime feature data through the ViewModel's state
contract.

`EarthquakePreviewState` is a data class implementing the read-only `EarthquakeState`
contract. It accepts item and selection fixtures, so tests can render a populated
screen and update selection with `copy` without Hilt, storage, or API calls.
Keep it and `EarthquakeMapFixtures` in the standard `testFixtures` source set
described in the [testing guide](testing.md#test-source-sets), outside production sources.
The ViewModel maps observable database rows to presentation values, preserving
numeric magnitude for cluster summaries, and owns the restorable selection.
Opening an event from a notification closes any cluster sheet before focusing
that event.

[EarthquakeEventSnackbar](../app/src/main/kotlin/com/johnturkson/sandbox/ui/EarthquakeEventSnackbar.kt)
renders the same Material `SnackbarData` supplied by the screen's `SnackbarHost`.
Keep snackbar queueing, duration, dismissal, and the host state in the UI. The
ViewModel owns the unavailable-event flag; rendering a snackbar does not create
another owner of that feature state. Screenshot fixtures supply fixed snackbar
data to the actual component without starting a timer.

Every app UI component must have screenshot tests for all of its supported visual
states. Add or update coverage with each UI change. Enumerate those states from
its state contract and inputs, and render deterministic fixtures without live
storage, network calls, or ticking clocks. For the map and main screen, include
empty and populated items and selected/unselected markers. Check supported
presentation configurations with representative fixtures. Previews and UI
assertions alone do not satisfy screenshot coverage.

Use Google's Compose Preview Screenshot Testing for ordinary Compose visuals.
The selected/unselected marker previews in
[EarthquakeMapMarkerScreenshotTest](../app/src/screenshotTest/kotlin/com/johnturkson/sandbox/EarthquakeMapMarkerScreenshotTest.kt)
render the same composable used by `MarkerComposable`. Follow the
[testing guide](testing.md#compose-screenshot-tests) for reference generation,
visual review, validation, and the checked-in baselines.

Maps Compose 9.0.1 returns an empty layout box in inspection mode, omitting
Google's native surface and marker placement. Host screenshot tests cover our
marker artwork, not native map tiles, camera projection, or main-screen map
integration. Do not approve blank or substituted map images as coverage of those
pixels. Use device behavior tests and inspected device captures to check native
empty, populated, and selected map integration. Native-map screenshot regression
coverage is still missing and must be reported.

## Saved state

`EarthquakeViewModel.selectedMapItemId` uses `SavedStateHandle.saved` with
`MutableStateSerializer<String?>` and exposes a private setter. Selection actions
change that single restorable value. The map does not keep a second selection.
Camera restoration remains UI-owned through `rememberCameraPositionState`.

Map and List each save their own selected ID and last-applied result revision
through `SavedStateHandle.saved` and `MutableStateSerializer`. Map saves pending
camera focus; List saves a pending scroll result until the matching row is
available and the UI handles it. Use the existing serialization pattern for
these values rather than relying on an in-memory result bus for restoration.

The navigation host saves the compact view preference, expanded panel preference,
notification visibility, result revision, and unacknowledged result payloads
using `rememberSaveable`. These values describe navigation and delivery, not a
shared destination selection. Keep camera and `LazyListState` in their respective
entry's saveable UI scope so resizing and compact view switching preserve them.

Inject `SavedStateHandle` into the ViewModel for state that needs restoration.
Use the existing serialization pattern instead of maintaining an unrelated
saved copy of the same state:

```kotlin
override var selectedMapItemId: String? by
    savedStateHandle
        .saved(serializer = MutableStateSerializer<String?>()) { mutableStateOf(null) }
        .getValue(this, this::selectedMapItemId)
    private set
```

Decide which values must restore when introducing state. Keep one authoritative
owner and derive dependent values from it. Saved state supports UI restoration;
durable application data belongs in storage accessed through the repository.

## Window size classes

Use AndroidX `WindowSizeClass` for compact, medium, and expanded layout decisions
throughout the app, including changes as a window resizes. A resizable window can
belong to any size class; choose the presentation from its current size class.
Read it through Material Adaptive's
`currentWindowAdaptiveInfoV2().windowSizeClass`.

Use `isAtLeastBreakpoint`, `isWidthAtLeastBreakpoint`, or
`isHeightAtLeastBreakpoint` with the named AndroidX breakpoint constants. Width
and height are classified independently. Do not recreate size-class boundaries
with numeric dimension comparisons or infer the class from device type,
orientation, or fold state. Use at-least checks so large and extra-large widths
also receive the expanded presentation.

[EarthquakeApp](../app/src/main/kotlin/com/johnturkson/sandbox/ui/EarthquakeApp.kt)
uses the current adaptive information for both its presentation decision and pane
directive:

```kotlin
val adaptiveInfo = currentWindowAdaptiveInfoV2()
val expanded =
    adaptiveInfo.windowSizeClass.isAtLeastBreakpoint(
        WIDTH_DP_EXPANDED_LOWER_BOUND,
        HEIGHT_DP_MEDIUM_LOWER_BOUND,
    )
```

Keep classification in the UI and derive it from the current window information
so resizing updates the layout. Do not persist the size class in a ViewModel or
saved state. Use actual dimensions for content measurements and proportional
sizing after choosing the presentation; `EarthquakeClusterSheet` uses its height
class to select a fraction and its measured window height to calculate the cap.

## Map and list presentation

Map and List are separate Navigation 3 destinations. Compact windows show one at
a time, selected with `EarthquakeViewSwitcher`; expanded windows keep the map
full-screen with a floating list at the start edge.
[EarthquakeApp](../app/src/main/kotlin/com/johnturkson/sandbox/ui/EarthquakeApp.kt)
and [EarthquakeScreen](../app/src/main/kotlin/com/johnturkson/sandbox/ui/EarthquakeScreen.kt)
enable this expanded presentation for expanded-or-larger width and
medium-or-larger height using the window size class check above.

[EarthquakeViewSwitcher](../app/src/main/kotlin/com/johnturkson/sandbox/ui/EarthquakeViewSwitcher.kt)
uses an opaque floating surface with flush Material single-choice segments and
minimum 48 dp height. Measure localized labels with current typography and font
scale; use accessible icon-only segments when both labels cannot fit, and restore
labels when space permits. Keep controls on one row, clear of system insets.

Use Material Adaptive's `rememberSupportingPaneSceneStrategy` with the Map entry
as `mainPane`, List as `supportingPane`, and Notifications as `extraPane`.
In expanded windows, configure the two secondary roles with
`AdaptStrategy.Levitate`, aligned to the start and end respectively, without a
scrim so the map remains interactive. Set one expanded horizontal partition so
the main map fills the window. Compact windows use `AdaptStrategy.Hide` for the
secondary roles and display the current destination full-screen. Keep
`shouldHandleSinglePaneLayout` enabled to preserve one scaffold scene through
resizing. Use the library's scene layout rather than a custom `Scene`.

Keep notification navigation immediate and independent of explorer motion.
`EarthquakeApp` uses `SupportingPaneSceneStrategy.paneAnimation` with
`EnterTransition.None`, `ExitTransition.None`, and `snap()` bounds for
Notifications. Apply the same metadata to Map and List when opening or closing
Notifications, including Back, so the replaced or restored pane does not slide.
Map/List and expanded-list controls restore the library's default pane motion
when Notifications is closed. Keep this transient animation choice in the host;
it is not destination state and does not need saved-state restoration.

[EarthquakeListScreen](../app/src/main/kotlin/com/johnturkson/sandbox/ui/EarthquakeListScreen.kt)
uses the same entry-owned `LazyListState` for both presentations.
[EarthquakeListPanel](../app/src/main/kotlin/com/johnturkson/sandbox/ui/EarthquakeListPanel.kt)
renders the fixed heading and Minimize control above the real list. The preferred
outer pane width is 392 dp, including 16 dp outer spacing on each side, leaving
the established 360 dp surface. Use `EarthquakeList(isSupportingPane = true,
showTitle = false)` inside it. Rows omit the forward arrow in expanded windows.
Compact rows select the earthquake and navigate to Map; expanded rows update
Map through results while keeping the panel open.

[EarthquakeExplorerControls](../app/src/main/kotlin/com/johnturkson/sandbox/ui/EarthquakeExplorerControls.kt)
places the map overlays and compact controls. Runtime Map and List destinations
set `renderListContent = false` when using its controls so the List entry renders
the list independently. Its aggregate rendering option supports deterministic
component fixtures using the same list and panel components.

A Map selection sends a result to List. List retains a pending request while
hidden or waiting for data, resolves the current event index by ID, accounts for
the monitoring and heading items, and animates an offscreen row into view. Consume
the request after handling it; data refreshes and layout changes alone must not
repeatedly override manual scrolling. A fresh revision can reveal the same event
again. Clearing selection synchronizes the two destinations without scrolling.

Keep camera state in the Map entry and list state in the List entry. Minimize and
Show list change the expanded navigation presentation without changing the saved
compact view preference. The selected row is highlighted without a duplicate
map selection card while the list is open. Reserve the panel width and outer
spacing in map content padding to keep camera focus and attribution visible.
Minimizing removes that reservation. List rows wrap locations and times.

[EarthquakeSelectionCard](../app/src/main/kotlin/com/johnturkson/sandbox/ui/EarthquakeSelectionCard.kt)
shows the selected event's title, available time, and coordinates. Use the same
bottom-centered component in compact Map and expanded Map with the panel
minimized, with a maximum width of 480 dp. Measure the bottom controls' combined
rendered height in the UI and include it in map content padding so native
attribution remains visible at larger font sizes. Never show the selection card
alongside the open expanded list.

[EarthquakeClusterSheet](../app/src/main/kotlin/com/johnturkson/sandbox/ui/EarthquakeClusterSheet.kt)
uses Material `ModalBottomSheet` with a fixed count heading and Close control, and a bounded `LazyColumn`
of the existing `EarthquakeListRow` components. Limit content to 60% of the window
height, or 45% in the compact height size class, and allow every member to scroll;
do not paginate a cluster. Select the fraction with
`windowSizeClass.isHeightAtLeastBreakpoint(HEIGHT_DP_MEDIUM_LOWER_BOUND)`.
Read `LocalWindowInfo` for the actual window height used to calculate the cap;
the percentages remain component design choices. Keep
the explanation and magnitude summary inside the scrolling content so large
text in a short window cannot consume the entire list viewport. Sheet
expansion and list scroll position remain UI-owned.

Hide `EarthquakeExplorerControls` while the modal sheet is open and remove its
reserved map padding. These inactive controls must not cover cluster counts or
the selected marker at large font sizes. Restore them when the sheet closes;
their screen state and hoisted list scroll state remain independent of the sheet.

After measuring the sheet, center a current member in the padded map viewport
on the next Compose frame. Repeat that focus when the sheet's height changes so
its members remain visible above the sheet after a window or font-size change.
Keep this camera integration in the UI, including the initialization flag that
prevents a pending location read from replacing an explicit cluster focus.
Selecting a member updates the shared selected ID and centers its map location
while keeping the sheet open. Both cluster and explorer rows identify selection
through their background highlight and selected semantics. Omit the redundant
"Selected earthquake" text from rows and `EarthquakeSelectionCard`.
Closing, back, or scrim dismissal removes the sheet from composition. Measure
its content in the UI and reserve bottom map padding for it and the drag handle
so native attribution and the selected marker remain above the sheet.
`EarthquakeClusterSheetContent` is the same content used for deterministic host
screenshots, without substituting for native map or modal-window pixels.

## Dependency injection and storage

Use Hilt constructor injection for ViewModels, repositories, and API wrappers.
[EarthquakeApplication](../app/src/main/kotlin/com/johnturkson/sandbox/app/EarthquakeApplication.kt)
uses `@HiltAndroidApp`, and
[EarthquakeActivity](../app/src/main/kotlin/com/johnturkson/sandbox/app/EarthquakeActivity.kt)
uses `@AndroidEntryPoint`.

`EarthquakeViewModel` uses `@HiltViewModel(assistedFactory = Factory::class)` and
`@AssistedInject`. Its `@Assisted` route is supplied by a nested
`@AssistedFactory`; `EarthquakeScreen` passes `navKey` through the
`hiltViewModel<EarthquakeViewModel, EarthquakeViewModel.Factory>` creation callback.
Follow this pattern when a destination ViewModel needs its route at creation time.

[EarthquakeModule](../app/src/main/kotlin/com/johnturkson/sandbox/app/EarthquakeModule.kt)
is installed in `SingletonComponent`. It provides the shared `Json`, Ktor
`HttpClient`, and qualified coroutine dispatchers. Extend this graph when adding
shared dependencies. Do not construct databases, clients, or repositories in
composables or ViewModels.

Use Preferences DataStore for small persisted settings. `EarthquakeModule`
provides one singleton `DataStore<Preferences>` per file with an application
context and a scope using the injected IO dispatcher. Its
`provideEarthquakesDataStore` provider uses `preferencesDataStoreFile("earthquakes")`
for shared app preferences, including the last known user location. Add future
small settings to this store through their owning repositories.
[EarthquakeLocationRepository](../app/src/main/kotlin/com/johnturkson/sandbox/data/EarthquakeLocationRepository.kt)
owns private preference keys and atomic reads/writes of latitude, longitude, and
the location fix's ISO-8601 timestamp, exposed as `Instant`.
Missing, incomplete, invalid, or unreadable coordinates produce a null last known location; the ViewModel maps
that to `EarthquakeLocation.Default` (San Francisco). Never record that fallback
as a measured user location. A corrupt preferences file resets to empty values.
An older cached fix cannot overwrite a newer timestamp. Optional cache-write IO
failures preserve existing data and allow the next location update to retry.

Keep `EarthquakeLocation` independent of SDK `Location` and `LatLng` types. Convert
device readings at the provider boundary and map camera coordinates in the UI.
Keep data access in repositories and inject them into ViewModels. Room stores
earthquake history through the data layer described below.

Inject qualified dispatchers where execution context is needed.
`EarthquakeModule` provides `@IoDispatcher`, `@MainDispatcher`, and
`@DefaultDispatcher`. Keep execution contexts replaceable instead of hardcoding
dispatchers into feature logic.

## Navigation

Use Navigation 3 and serializable keys in
[Destination](../app/src/main/kotlin/com/johnturkson/sandbox/ui/Destination.kt).
The explorer has `EarthquakeMapRoute`, `EarthquakeListRoute`, and `EarthquakeNotificationsRoute`.
[EarthquakeApp](../app/src/main/kotlin/com/johnturkson/sandbox/ui/EarthquakeApp.kt)
owns the back stack, entry provider, Material scene strategy, and navigation
callbacks. Preserve the saveable state holder, ViewModel store, and result event
bus decorators. Hilt creates a separate assisted ViewModel in each entry scope.
Do not share a ViewModel instance between these destinations.

Keep one Map key and one List key in the stack, reordering them to choose the
compact destination or place the expanded floating list above Map. Do not append
another Map/List entry on every toggle. This preserves entry scopes and their
saved UI state. Add Notifications above them when opened and remove that entry
when closed. Back closes Notifications, minimizes an open expanded list, or
returns compact List to Map; Back from the Map root exits the activity.
The notification and expanded-list choices restore independently of the compact
view preference.

Selection results use `map_selection` and `list_selection` keys with
[EarthquakeSelectionResult](../app/src/main/kotlin/com/johnturkson/sandbox/ui/EarthquakeSelectionResult.kt)
payloads containing an optional earthquake ID and increasing revision. Publish
once from the source action. The host keeps only the latest unacknowledged
payload for each recipient in saveable state and clears obsolete delivery to
the source. This preserves updates for hidden destinations and through process
recreation; Navigation 3's in-memory bus alone does not provide that persistence.

[EarthquakeSelectionEffect](../app/src/main/kotlin/com/johnturkson/sandbox/ui/EarthquakeSelectionEffect.kt)
sends a pending payload through the result bus when the receiver is composed,
handles it with `ResultEffect`, and acknowledges the matching revision. Keep one
receiver per addressed result key. ViewModel revision checks make redelivery
idempotent. UI focus requests remain pending in the destination until the UI
can perform them. Keep navigation infrastructure out of rendered components and
data code; `EarthquakeNavigationActions` passes values and callbacks to entry
wrappers and provides the fixture seam for host navigation tests.

## Networking and data models

Use the injected Ktor client for API access.
`EarthquakeModule.provideHttpClient` configures CIO, `ContentNegotiation` with the
provided `Json`, `Logging`, and `Resources`, with the USGS base URL. Extend the
shared configuration rather than creating a new client in each feature.

Keep requests and response decoding in API wrappers, data coordination in
repositories, and presentation state in ViewModels. Keep network serialization
and database annotations on their respective models. Use a shared read-only
contract and distinct representations where network and database models describe
the same earthquake. UI components should consume only the values they need.

[EarthquakeSerializationTest](../app/src/test/kotlin/com/johnturkson/sandbox/EarthquakeSerializationTest.kt)
is the working example for decoding a test-local serializable response with Ktor
`MockEngine` and `runTest`. It verifies the request path, JSON decoding, and ignored
unknown fields without a live service. Its response and URL are test fixtures,
not a production USGS schema or endpoint. Use that approach when implementing API
operations.

## Earthquake history and offline storage

Use the existing path for runtime earthquake data:

```text
EarthquakeWorkScheduler -> EarthquakeRefreshWorker -> EarthquakeRefreshRepository
EarthquakeRefreshRepository -> EarthquakeApi -> USGS
EarthquakeRefreshRepository -> MainDatabase -> EarthquakeDao
EarthquakeDao + EarthquakeMonitoringDao -> EarthquakeRepository -> EarthquakeViewModel
```

Follow the [network instructions](networking.md) for typed Ktor endpoints and
separate network models, and the [database instructions](database.md) for Room
entities, transactions, migrations, and observable queries. Database read names
end in `Flow`; writes and explicit network refreshes are suspend operations.
Rendered components remain independent of these dependencies.

`EarthquakeDatabaseModel.id` is the primary key. Refreshes merge new IDs and
update existing IDs only for newer USGS `updated` revisions, retaining stored
events absent from subsequent feeds. Do not clear history on refresh or delete
an event merely because it aged out of a rolling feed. Empty and failed
responses preserve stored information. Read the Room database independently of
network access; an unrestricted `getEarthquakesFlow()` includes historical rows.
UI time filters must not delete stored history.

Keep alias handling for preferred-ID changes. Store ordinary earthquake records
without separate product snapshots, product maps, or detail-freshness wrappers.
`EarthquakeRepository` is read-only and exposes Room flows for records and refresh
metadata. The one-time `EarthquakeRefreshWorker` is the only runtime caller of
`EarthquakeRefreshRepository.refresh()`; route every update through the scheduler.
`MainDatabase` retains its original name and schema history to preserve
compatibility with existing installations. Version 6 removes
the unused item table through `EarthquakeOnlyMigration`; current entities and DAOs
are earthquake-specific. Preserve earthquake data during later schema changes.

See [earthquake data](earthquake-data.md) for the repository API and refresh
limits. WorkManager scheduling and screen integration should reuse this layer.

## Refresh, monitoring, and notifications

[`EarthquakeRefreshRepository`](../app/src/main/kotlin/com/johnturkson/sandbox/data/EarthquakeRefreshRepository.kt)
fetches and validates the response, then merges records and refresh metadata in
one Room transaction. It returns normally on success and lets network, decoding,
validation, and storage exceptions propagate. It does not catch transport error
types, classify failures, check connectivity, or own work status.

`EarthquakeRefreshWorker` calls the repository and returns `Result.success()`.
An exception escaping `doWork()` is handled by WorkManager as failed work. The
ViewModel maps `WorkInfo.State.FAILED` to the generic refresh-failed presentation;
it does not inspect exception types or parse custom result codes. Cancellation
propagates through suspending work.

After a successful data commit, the worker calls `publishAlerts()`. Only this
best-effort step has an exception guard: notification failures must not turn
successfully saved data into a failed refresh, and cancellation still propagates.
Read typed instants directly with `Clock.System.now()` for refresh metadata and
alert freshness; do not add a dedicated class that only forwards that system call.

[`EarthquakeWorkScheduler`](../app/src/main/kotlin/com/johnturkson/sandbox/work/EarthquakeWorkScheduler.kt)
routes manual refresh and every monitoring tick through the same unique one-time
`EarthquakeRefreshWorker` with `ExistingWorkPolicy.KEEP`. Enqueue directly instead
of checking for running work first; WorkManager keeps the existing pending work.
Do not add another mutex, running flag, or completion coordinator around it.

The screen observes `getWorkInfosForUniqueWorkFlow` through `refreshWork` for
queued/running/failed state and successful completion. No separate result enum or
error-code payload is needed for generic refresh feedback.
Each unique name contains a single one-time request, so use its current `WorkInfo`
without timestamp tags or a separate ordering scheme. Room independently supplies
stored earthquakes and the last-successful-refresh timestamp.

`EarthquakeMonitoringWorker` calls `setForeground` with the `dataSync` service
type. `EarthquakePollingLoop` owns cancellable minute pacing, skips catch-up
bursts, and wakes on reconnection. Each tick calls `refresh().await()` to finish
enqueueing; that operation does not wait for the refresh to finish. Later ticks
may enqueue again while a refresh is active because `KEEP` coalesces those
requests. The monitoring worker does not observe, await, or copy refresh results.

The foreground notification describes the monitoring session: active or paused
offline. The screen observes the separate refresh work for fetch progress and
failures. Do not mirror refresh state into monitoring progress or notification
state. Keep the monitoring session as separate unique one-time work with `KEEP`.
Stopping it cancels future ticks while any shared refresh already queued or
running finishes independently.

Neither request has a network constraint. Refresh attempts use the normal network
operation and its 30-second timeout; an offline attempt can fail without waiting
for connectivity. Do not add a refresh connectivity precheck or exception-type
catalog. The monitoring session separately observes connectivity so it retains
foreground status while paused offline.
Workers receive dependencies through `@HiltWorker` and the application's injected
`HiltWorkerFactory`. `EarthquakeApplication` supplies WorkManager's on-demand
configuration; remove only WorkManager's initializer from AndroidX Startup.
WorkManager owns execution and rescheduling. Do not add UI timers, restart work
on recomposition, or re-enqueue after explicit cancellation. Observe `WorkInfo`
to distinguish queued, running, stopped, and failed work. Android quota and
service limits can interrupt the requested cadence; there is no app-imposed
session cutoff.

`EarthquakeNotifications` registers stable, idempotent channels: low-importance
`earthquake_monitoring` for quiet foreground status, and high-importance
`earthquake_alerts` for opt-in large-earthquake alerts. Updating a channel must
preserve Android's user-controlled settings. Refresh notification capabilities
on screen resume and recheck before every post. Foreground work still supplies
its required notification when POST_NOTIFICATIONS is denied; ordinary alerts
are suppressed. Request that permission only from a user action in the screen
entry, with rationale and settings paths. Persist only the request attempt and
user preferences, never an OS permission grant.

`EarthquakeAlertPolicy` requires the USGS `earthquake` event type, event time
within the past 60 seconds, and the chosen minimum magnitude. A magnitude
revision may qualify while the event is fresh;
`EarthquakeAlertDelivery` records successful delivery by USGS event ID in Room.
Stable notification tags update the same event rather than creating duplicates.
The database commit and Android notification post are separate operations;
there is no exactly-once transaction spanning both. Late USGS reports and older
events found after reconnection are deliberately excluded by the one-minute
freshness policy. Notification intents have distinct event URIs and open the
Map destination and publish the resolved selection to List; an event no longer
in the database shows an availability message.

`EarthquakeMonitoringState` and `EarthquakeNotificationState` are immutable
rendering values exposed by the relevant destination's read-only contract.
[EarthquakeMonitoringObserver](../app/src/main/kotlin/com/johnturkson/sandbox/ui/EarthquakeMonitoringObserver.kt)
combines worker, repository, and connectivity observations without owning event
selection. Each destination collects it independently. Notifications observes
preferences and system capabilities in its own ViewModel and screen entry.
`EarthquakeMonitoringState.status` uses its nested `Status` enum for the monitoring
session; `refresh` describes the separate refresh operation.
`EarthquakeMonitoringCard` is the first keyed item of `EarthquakeList`'s existing
LazyColumn, so it scrolls away with the heading and rows. Keep the existing
hoisted list state when switching layouts or minimizing. Expanded list headers
remain fixed. Minimized maps show the same card as an overlay. A refresh keeps
the old data and timestamp until storage succeeds.

The bell has no badge and aligns to the top end edge in both layouts. Center
the compact switcher independently, reserving equal space on both sides so the
bell does not shift its center or overlap it. `EarthquakeExplorerControls` owns
both placements. The bell opens `EarthquakeNotificationsRoute`, independently of the Map/List switcher.
Closing it restores the compact view and expanded panel preference. Opening it
in expanded windows uses a floating right pane and temporarily hides List.
[EarthquakeNotificationsScreen](../app/src/main/kotlin/com/johnturkson/sandbox/ui/EarthquakeNotificationsScreen.kt)
owns notification permission launchers, rationale, settings actions, and resume
capability checks. Map retains location lifecycle integration in its own entry.
Camera, list scroll, and selection remain in their decorated entry scopes.
List rows retain selected semantics and background highlight without a selection
label.

`EarthquakeRefreshRepository` reuses the shared `EarthquakeApi` and network-to-Room
mapping, then calls the established `EarthquakeDao.mergeEarthquakes` inside a
`MainDatabase.withTransaction` with refresh metadata. It never clears event rows.
`EarthquakeViewModel` reads `EarthquakeRepository.getEarthquakesFlow` without time
filters and observes refresh metadata separately. A failed merge rolls back both
event changes and the last-successful-refresh timestamp.

`MainDatabase` version 3 adds only `earthquake_refresh` and `earthquake_alerts`,
using additive auto-migrations. Version 4 removes obsolete product snapshots.
Version 5 converts timestamp columns to ISO-8601 text with the explicit
`EarthquakeTimestampMigration`, retaining existing data. `EarthquakeMonitoringDao`
keeps all reads observable with names ending in `Flow`. Delivered-alert queries
join retained aliases, so a preferred-ID change does not repeat a notification.
Notification taps and existing selection resolve through the same repository's
alias-aware single-event query. Notification history has no cascading foreign key
to preferred earthquake rows.

`EarthquakeConnection` describes current connectivity and its observable changes;
`EarthquakeConnectivity` is the Android implementation. Hilt supplies the validated-network source to monitoring and the ViewModel.
Refresh execution does not depend on this connectivity snapshot. Tests use
MockEngine to control network outcomes while the device is offline. The refresh repository bounds its own summary request with a
30-second coroutine timeout, preserving the shared client's other endpoint
behavior.

Follow the [date and time conventions](date-time.md) for typed instants and
durations, ISO storage, SDK conversions, and timestamp serialization.
