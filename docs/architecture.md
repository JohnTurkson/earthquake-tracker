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

`EarthquakeState` exposes `navKey`, read-only `mapItems`, `selectedMapItemId`,
`selectedClusterItemIds`, `selectedView`, `isListPanelVisible`, `initialMapLocation`, and the latest measured
`userLocation`.
`EarthquakeScreen` has a Hilt entry overload and a rendering overload that accepts
`EarthquakeState` and callbacks. Extend this read-only contract when expanding screen
state and provide deterministic preview and test implementations.

`EarthquakeViewModel` exposes selection through `EarthquakeState` with a private
setter and owns changes through `selectMapItem` and `clearMapSelection`. UI
consumers receive read-only state and request changes through callbacks. Do not
create a second copy of the same feature state inside a composable.

[EarthquakeScreen](../app/src/main/kotlin/com/johnturkson/sandbox/ui/EarthquakeScreen.kt)
keeps Hilt lookup at the screen entry and passes values and actions into
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
annotations. Maps Utils owns unselected marker lifetimes through immutable
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
requests access from a user action before the SDK button is available.
[EarthquakeLocationRationale](../app/src/main/kotlin/com/johnturkson/sandbox/ui/EarthquakeLocationRationale.kt)
explains a repeat request. When Android will no longer show the request, the
button opens app settings. Keep rendering components independent of permission
launchers: pass granted/settings values and action callbacks from the entry.
`EarthquakeExplorerControls` owns placement of the app button in both window
layouts. Denial leaves map and list exploration available.

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

`EarthquakeViewModel.selectedView` uses the same `SavedStateHandle.saved` and
`MutableStateSerializer` pattern with the serializable `EarthquakeView` enum.
It restores the Map/List choice independently of earthquake selection.
`isListPanelVisible` uses the same saved Boolean pattern, defaulting to true.
`updateListPanelVisibility` changes only the expanded panel's visibility; compact
presentation and the shared selection remain independent. Restore this choice
through activity/process recreation rather than adding a navigation entry.

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

## Map and list presentation

Use separate full-screen Map and List views in compact windows.
[EarthquakeViewSwitcher](../app/src/main/kotlin/com/johnturkson/sandbox/ui/EarthquakeViewSwitcher.kt)
places Material single-choice segmented buttons in an opaque floating surface
at the top, clear of system insets. Keep the segments flush with that surface,
with no outer padding or overlap for the borderless buttons. Each segment has a
minimum visual height of 48 dp so its fill covers the accessible touch target.
Changing presentation is a screen action,
not a new Navigation 3 back-stack entry. Keep destination navigation in
`EarthquakeApp` when adding actual destinations.
Measure the localized labels with the current typography and font scale. When
both icon-and-label segments cannot fit, show only their Map/List icons and
retain their accessible names and selected semantics. Keep the controls on one
row; resizing restores labels when space permits.

[EarthquakeScreen](../app/src/main/kotlin/com/johnturkson/sandbox/ui/EarthquakeScreen.kt)
classifies its available constraints in the UI. At widths of at least 840 dp
and heights of at least 480 dp, keep the map visible and show a floating 360 dp
list panel at the start edge, with no Map/List toggle. Shorter or narrower
windows use the compact presentation. Avoid hardware or fold-state checks.

[EarthquakeExplorerControls](../app/src/main/kotlin/com/johnturkson/sandbox/ui/EarthquakeExplorerControls.kt)
renders the ordinary Compose content around the native map. Its `expanded`
input comes from the screen's layout decision. Compact Map shows the floating
switcher and an optional selection card; compact List shows the same switcher
above the full list. The expanded panel contains a fixed heading and Minimize
button above the list; the selected row is highlighted without a duplicate
selection card. `EarthquakeList(isSupportingPane = true, showTitle = false)`
omits its own title and uses a short local-time label. Its rows omit the forward
arrow because selection updates the adjacent map rather than opening another
presentation.

Both layouts consume the same `mapItems` and selected ID. Selecting a list row
updates selection and centers the map on that item. Only compact list selection
switches to Map. Expanded selection leaves the compact view preference unchanged.
Returning to compact restores that preference. Clearing selection on the map or
in the details clears the list highlight too.

Keep `rememberCameraPositionState` and `rememberLazyListState` above the layout
branch so changing views or resizing does not discard them. These are UI-owned
layout states; the ViewModel owns the compact presentation, panel visibility,
and selected ID.
The list fills compact windows. Rows wrap rather than truncate event locations
or times. Reserve the expanded panel's width plus its outer spacing in the map's
start content padding while the panel is visible so the camera center and
attribution stay in the visible map area. Minimizing removes that reserved width,
keeps the existing native map and camera, and shows a floating Show list button
at the start edge. Resizing to compact ignores the visibility preference;
returning to expanded restores it. These overlay transitions are screen state,
not Navigation 3 back-stack changes. Material surfaces and buttons render them;
the explorer explicitly controls their visibility and placement.

[EarthquakeSelectionCard](../app/src/main/kotlin/com/johnturkson/sandbox/ui/EarthquakeSelectionCard.kt)
shows the selected event's title, available time, and coordinates. Use the same
bottom-centered component in compact Map and expanded Map with the panel
minimized, with a maximum width of 480 dp. Measure its rendered height in the UI
and include that height in map content padding so native attribution remains
visible at larger font sizes. Never show it alongside the open expanded list.

[EarthquakeClusterSheet](../app/src/main/kotlin/com/johnturkson/sandbox/ui/EarthquakeClusterSheet.kt)
uses Material `ModalBottomSheet` with a fixed count heading and Close control, and a bounded `LazyColumn`
of the existing `EarthquakeListRow` components. Limit content to 60% of the window
height, or 45% in windows shorter than 600 dp, and allow every member to scroll;
do not paginate a cluster. Read `LocalWindowInfo` for the actual window bounds. Keep
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
while keeping the sheet open. Enable `EarthquakeListRow.showSelectionLabel` in
the sheet to identify the selected member with text as well as its highlight;
the main explorer list retains its own presentation.
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

Use Navigation 3. Add serializable destination keys to
[Destination](../app/src/main/kotlin/com/johnturkson/sandbox/ui/Destination.kt),
which is a sealed `NavKey` interface. Existing destinations are `@Serializable`
data objects; the current destination is `EarthquakeRoute`.

[EarthquakeApp](../app/src/main/kotlin/com/johnturkson/sandbox/ui/EarthquakeApp.kt)
owns `rememberNavBackStack(EarthquakeRoute)`, `NavDisplay`, and the typed
`entryProvider` entries. It installs the saveable state holder, ViewModel store,
and result event bus entry decorators. Preserve these entry scopes when adding
destinations; pass the entry's route to the screen and its assisted ViewModel
factory when needed.

Keep back stack changes in `EarthquakeApp`. Preserve the event/handler boundary
when adding earthquake destinations: screens emit actions and `EarthquakeApp`
changes the back stack. Keep navigation infrastructure out of rendered components
and data code.

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
`MainDatabase` retains its original name, schema
history, and legacy `ItemDatabaseModel` table to preserve compatibility with
existing installations; that table is not a feature or a pattern for new work.
Use data-preserving migrations for later schema changes.

See [earthquake data](earthquake-data.md) for the repository API and refresh
limits. WorkManager scheduling and screen integration should reuse this layer.

## Refresh, monitoring, and notifications

[`EarthquakeRefreshRepository`](../app/src/main/kotlin/com/johnturkson/sandbox/data/EarthquakeRefreshRepository.kt)
checks validated connectivity, fetches before merging stored earthquakes, and
publishes alerts after commit. It returns `EarthquakeRefreshResult` outcomes for
success, offline, network, decode, and storage failures. Cancellation propagates;
notification failures must not roll back saved earthquake data. The repository
does not own work status or deduplicate refresh requests.
Read typed instants directly with `Clock.System.now()` for refresh
metadata and alert freshness; do not add a dedicated class that only forwards
that system call.

[`EarthquakeWorkScheduler`](../app/src/main/kotlin/com/johnturkson/sandbox/work/EarthquakeWorkScheduler.kt)
routes manual refresh and every monitoring tick through the same unique one-time
`EarthquakeRefreshWorker` with `KEEP`. Enqueue directly instead of checking for
running work first; WorkManager owns deduplication. `refreshAndAwait` awaits the
enqueue operation, then observes the unique work until it finishes. `KEEP` can
discard a new request, so never await the newly constructed request's ID. The
enqueue operation's completion is not the refresh result.

Observe `refreshWork` for queued/running state and terminal `WorkInfo.outputData`
for the typed result. Do not combine it with a second in-memory repository status
or the monitoring worker's last refresh result. Room independently supplies the
stored earthquakes and last-successful-refresh timestamp.

The monitoring session is separate unique one-time work with `KEEP`. Stopping it
cancels its polling and result observation, leaving any shared refresh already
queued or running to finish. Neither request has a network constraint:
manual offline work must fail instead of waiting, and a monitoring session must
retain its foreground status while paused offline. Workers receive dependencies
through `@HiltWorker` and the application's injected `HiltWorkerFactory`.
`EarthquakeApplication` supplies WorkManager's on-demand configuration; remove
only WorkManager's initializer from AndroidX Startup.

`EarthquakeMonitoringWorker` calls `setForeground` with the `dataSync` service
type and uses `refreshAndAwait` instead of calling the repository directly.
Its refreshing notification is shown only when the shared refresh work is
observed in `WorkInfo.State.RUNNING`, rather than while waiting in the queue.
`EarthquakePollingLoop` owns cancellable minute pacing, skips catch-up
bursts after slow requests, and wakes on reconnection. WorkManager owns
execution and rescheduling. Do not add UI timers, restart work on recomposition,
or re-enqueue after explicit cancellation. Observe `WorkInfo` to distinguish
queued, running, stopped, and failed work. Android quota and service limits can
interrupt the requested cadence; there is no app-imposed session cutoff.

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
shared selection; an event no longer in the database shows an availability message.

`EarthquakeMonitoringState` and `EarthquakeNotificationState` are immutable
rendering values exposed by `EarthquakeState`. The ViewModel maps worker,
repository, connectivity, and preference observations into those values.
`EarthquakeMonitoringState.status` uses its nested `Status` enum for the monitoring
session; `refresh` describes the separate refresh operation.
`EarthquakeMonitoringCard` is the first keyed item of `EarthquakeList`'s existing
LazyColumn, so it scrolls away with the heading and rows. Keep the existing
hoisted list state when switching layouts or minimizing. Expanded list headers
remain fixed. Minimized maps show the same card as an overlay. A refresh keeps
the old data and timestamp until storage succeeds.

The bell is independent of the Map/List switcher and has no badge. Align it to
the top end edge in compact and expanded layouts. Center the compact switcher
independently, reserving equal space on both sides so the bell does not shift
its center or overlap it. `EarthquakeExplorerControls` owns both placements.
Notification page visibility is restorable explorer state, like the expanded
list panel.
Closing it restores the existing compact view and expanded panel preference;
opening it in expanded windows uses the right pane and temporarily hides the
list. Keep the camera, list state, location flow, and selection owners above
these layout branches. List rows retain selected semantics and background
highlight without repeating the selection label used by the map's detail card.

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
`EarthquakeConnectivity` is the Android implementation. Hilt supplies the same
validated-network source to workers and the ViewModel. Controlled implementations
allow worker/API/database integration tests to use MockEngine while the device
is offline. The refresh repository bounds its own summary request with a
30-second coroutine timeout, preserving the shared client's other endpoint
behavior.

Follow the [date and time conventions](date-time.md) for typed instants and
durations, ISO storage, SDK conversions, and timestamp serialization.
