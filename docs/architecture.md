# Architecture and state management guidelines

Build on the concrete patterns already present in this project. These conventions
apply to agents and human contributors. Each section links to the implementation
that establishes the pattern and explains how to extend it.

Name app and feature types with the `Earthquake` prefix, as in `EarthquakeApp`,
`EarthquakeApplication`, `EarthquakeModule`, and `EarthquakeScreen`. Use the same
name for a type's Kotlin file. Keep technical names such as `MainDispatcher` tied
to the platform concept they describe.

The screen currently uses fixed earthquake fixtures. Product requirements in
[the README](../README.md) describe the intended app; runtime fetching and durable
storage have not been implemented.

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
[EarthquakePreviewState](../app/src/main/kotlin/com/johnturkson/sandbox/ui/EarthquakePreviewState.kt)
provides an implementation without a ViewModel. Follow that interface and
implementation pattern when expanding screen state, and honor the stability
contract when using `@Stable`.

`EarthquakeState` exposes `navKey`, read-only `mapItems`, `selectedMapItemId`, and
`selectedView`.
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
coordinates, title, optional snippet, and optional event time in epoch
milliseconds. The explorer uses the same values for its map and list. Format
event times in the UI using the device's locale and time preferences; do not
store formatted time strings as feature state. It has no network or database
annotations. Map markers are keyed by that ID; `rememberUpdatedMarkerState`
applies coordinate changes without turning the SDK marker into feature state.
Marker clicks emit the current item and map-background clicks emit a callback.
The ViewModel owns selection; the component derives marker color and stacking
from `selectedItemId` and suppresses the SDK's independent info-window selection.
`MarkerComposable` renders the real
[EarthquakeMapMarker](../app/src/main/kotlin/com/johnturkson/sandbox/ui/EarthquakeMapMarker.kt)
content into the SDK icon. Include selection in its render keys so the bitmap
updates. Marker artwork consumes only `isSelected` and a `Modifier`; the SDK
marker owns hit testing, while the ViewModel owns the selected ID.

Follow Maps Compose naming: use `cameraPositionState` for the component parameter
and local variables of type `CameraPositionState`, created by
`rememberCameraPositionState`. It describes the map's center, zoom, tilt, and
bearing. Pass it directly to `GoogleMap(cameraPositionState = cameraPositionState)`.

Camera state, projection, marker, and other SDK rendering objects belong in the UI.
The component accepts a hoisted `CameraPositionState`, `MapProperties`,
`MapUiSettings`, and content padding. Its remembered default camera restores
through Maps Compose's saver. Camera changes are UI state and must not be copied
into the ViewModel. The main screen fills its destination, including
behind system bars, and passes safe-drawing padding for Google attribution.
The default camera starts over San Francisco at zoom 10. Map defaults enable the
SDK's My Location button while hiding other optional controls and retaining
gestures. The button appears only when the caller enables the My Location layer
with granted location permission; enabling its UI setting does not request
permission. Keep Google attribution visible when adding overlays.

Use fixed development inputs such as
[EarthquakeMapFixtures](../app/src/main/kotlin/com/johnturkson/sandbox/ui/EarthquakeMapFixtures.kt)
to render deterministic map states without live API or storage access. Pass only
the presentation values needed by markers: ID, coordinates, title, and an
optional event time or snippet. Keep these values independent of network and
database models, and supply runtime feature data through the ViewModel's state
contract.

`EarthquakePreviewState` is a data class implementing the read-only `EarthquakeState`
contract. It accepts item and selection fixtures, so tests can render a populated
screen and update selection with `copy` without Hilt, storage, or API calls.
The ViewModel supplies the same fixed items and owns the restorable selection.

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

Use separate full-screen Map and List views in compact and expanded windows.
[EarthquakeViewSwitcher](../app/src/main/kotlin/com/johnturkson/sandbox/ui/EarthquakeViewSwitcher.kt)
places Material single-choice segmented buttons in an opaque floating surface
at the top, clear of system insets. Changing presentation is a screen action,
not a new Navigation 3 back-stack entry. Keep destination navigation in
`EarthquakeApp` when adding actual destinations.

[EarthquakeScreen](../app/src/main/kotlin/com/johnturkson/sandbox/ui/EarthquakeScreen.kt)
renders one presentation at a time. Both consume the same `mapItems` and selected
ID. Selecting a list row updates selection, centers the map on that item, and
switches to Map. Switching back to List highlights the same item. Clearing
selection on the map clears the list highlight too.

Keep `rememberCameraPositionState` and `rememberLazyListState` above the
presentation branch so changing views does not discard them. These are UI-owned
layout states; the ViewModel owns only the active presentation and selected ID.
Adapt to available constraints without checking whether the device is folded.
The list fills compact windows and is centered with a maximum width of 840 dp
in wider windows. Rows wrap rather than truncate event locations or times.

[EarthquakeSelectionCard](../app/src/main/kotlin/com/johnturkson/sandbox/ui/EarthquakeSelectionCard.kt)
shows the selected event's title, available time, and coordinates over the map.
Measure its rendered height in the UI and include that height in map content
padding so native attribution remains visible at larger font sizes.

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

Room and DataStore are declared dependencies with no current database, DAO,
repository, or storage provider. Hilt should supply storage dependencies; keep
Room operations in DAOs and coordinate data access through repositories when
storage is implemented. Discuss conventions for the first concrete storage
implementation before introducing it.

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
provided `Json`, `Logging`, and `Resources`. Extend the shared configuration
rather than creating a new client in each feature. No production API wrapper or
endpoints are implemented yet.

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
operations. The installed `Resources` plugin has no typed resource endpoints yet;
discuss endpoint conventions when implementing the first real requests.
