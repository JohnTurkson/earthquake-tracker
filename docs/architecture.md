# Architecture and state management guidelines

Build on the concrete patterns already present in this project. These conventions
apply to agents and human contributors. Each section links to the implementation
that establishes the pattern and explains how to extend it.

The timer sample has been removed. The current implementation still contains
`Item` data scaffolding pending its replacement with earthquake API operations.
Use the established responsibility boundaries when developing USGS features. Product
requirements in [the README](../README.md) describe the intended app; they do not
mean those features have been implemented.

## UI and state ownership

Keep screen state and state transitions in the ViewModel. Keep rendering,
layout state, locale formatting, and UI lifecycle integration in the UI. UI
actions call ViewModel methods or callbacks; rendered components do not access
repositories, storage, or network clients.

Use read-only interfaces to describe the state consumers can observe.
[MainState](../app/src/main/kotlin/com/johnturkson/sandbox/ui/MainState.kt)
is annotated `@Stable`, and
[MainViewModel](../app/src/main/kotlin/com/johnturkson/sandbox/ui/MainViewModel.kt)
implements it.
[MainPreviewState](../app/src/main/kotlin/com/johnturkson/sandbox/ui/MainPreviewState.kt)
provides an implementation without a ViewModel. Follow that interface and
implementation pattern when expanding screen state, and honor the stability
contract when using `@Stable`.

`MainState` exposes `navKey`, read-only `mapItems`, and `selectedMapItemId`.
`MainScreen` has a Hilt entry overload and a rendering overload that accepts
`MainState` and callbacks. Extend this read-only contract when expanding screen
state and provide deterministic preview and test implementations.

`MainViewModel` exposes selection through `MainState` with a private setter and
owns changes through `selectMapItem` and `clearMapSelection`. UI consumers receive
read-only state and request changes through callbacks. Do not create a second
copy of the same feature state inside a composable.

[MainScreen](../app/src/main/kotlin/com/johnturkson/sandbox/ui/MainScreen.kt)
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
coordinates, title, and optional snippet. It has no network or database
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
Map defaults hide optional controls while retaining gestures and do not request
location access. Keep Google attribution visible when adding overlays.

For this UI integration, the ViewModel uses a fixed, manually downloaded USGS
snapshot in
[EarthquakeMapFixtures](../app/src/main/kotlin/com/johnturkson/sandbox/ui/EarthquakeMapFixtures.kt).
It contains 16 geographically varied events selected from 212 events generated
at 2026-11-01 03:02 UTC (2026-10-31 20:02 Pacific). The source is the USGS
[past-day GeoJSON feed](https://earthquake.usgs.gov/earthquakes/feed/v1.0/summary/all_day.geojson).
These are static development fixtures, not a live feed. Only the marker inputs
needed by the UI are copied: ID, coordinates, title, and event-time snippet.
This matches the fields exposed by the parallel earthquake-data implementation
without coupling UI code to its network/database models. Runtime API/storage
integration remains with that work. Detail navigation, clustering, and heatmaps
are not implemented here.

`MainPreviewState` is a data class implementing the read-only `MainState`
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
pixels. Native empty, populated, and selected map integration remains covered by
device behavior tests and inspected device captures; native-map screenshot
regression coverage is still missing and must be reported.

## Saved state

`MainViewModel.selectedMapItemId` uses `SavedStateHandle.saved` with
`MutableStateSerializer<String?>` and exposes a private setter. Selection actions
change that single restorable value. The map does not keep a second selection.
Camera restoration remains UI-owned through `rememberCameraPositionState`.

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

## Dependency injection and storage

Use Hilt constructor injection for ViewModels, repositories, and API wrappers.
[MainApplication](../app/src/main/kotlin/com/johnturkson/sandbox/app/MainApplication.kt)
uses `@HiltAndroidApp`, and
[MainActivity](../app/src/main/kotlin/com/johnturkson/sandbox/app/MainActivity.kt)
uses `@AndroidEntryPoint`.

`MainViewModel` uses `@HiltViewModel(assistedFactory = Factory::class)` and
`@AssistedInject`. Its `@Assisted` route is supplied by a nested
`@AssistedFactory`; `MainScreen` passes `navKey` through the
`hiltViewModel<MainViewModel, MainViewModel.Factory>` creation callback. Follow
this pattern when a destination ViewModel needs its route at creation time.

[MainModule](../app/src/main/kotlin/com/johnturkson/sandbox/app/MainModule.kt)
is installed in `SingletonComponent`. It provides the shared `Json`, Ktor
`HttpClient`, Room database and DAO, and qualified coroutine
dispatchers. Extend this graph when adding shared dependencies. Do not construct
databases, clients, or repositories in composables or ViewModels.

Hilt supplies storage dependencies; Room stores the data. The existing path is:

```text
MainModule -> MainDatabase -> ItemDao -> MainRepository -> MainViewModel
```

[MainDatabase](../app/src/main/kotlin/com/johnturkson/sandbox/data/MainDatabase.kt)
declares `ItemDatabaseModel` as its entity and exposes `itemDao()`.
`MainModule` builds the database with the application context and provides its
DAO to
[MainRepository](../app/src/main/kotlin/com/johnturkson/sandbox/data/MainRepository.kt).
Keep database operations in DAOs and coordinate data access through repositories.
The current DAO and repository have no operations yet. DataStore is declared as
a dependency but has no implementation or provider; discuss its conventions when
it is first introduced.

Inject qualified dispatchers where execution context is needed. The repository
receives `@IoDispatcher`. `MainModule` also provides `@MainDispatcher` and
`@DefaultDispatcher`. Keep execution contexts replaceable instead of hardcoding
dispatchers into feature logic. Timer jobs, timer models, and timer-specific
clock providers have been removed.

## Navigation

Use Navigation 3. Add serializable destination keys to
[Destination](../app/src/main/kotlin/com/johnturkson/sandbox/ui/Destination.kt),
which is a sealed `NavKey` interface. Existing destinations are `@Serializable`
data objects; the current destination is `MainRoute`.

[MainContent](../app/src/main/kotlin/com/johnturkson/sandbox/ui/MainContent.kt)
owns `rememberNavBackStack(MainRoute)`, `NavDisplay`, and the typed
`entryProvider` entries. It installs the saveable state holder, ViewModel store,
and result event bus entry decorators. Preserve these entry scopes when adding
destinations; pass the entry's route to the screen and its assisted ViewModel
factory when needed.

Keep back stack changes in `MainContent`. The timer detail screen and its result
events have been removed. Preserve the event/handler boundary when adding real
earthquake destinations: screens emit actions and `MainContent` changes the back
stack. Keep navigation infrastructure out of rendered components and data code.

## Networking and data models

Use the injected Ktor client for API access.
[ItemApi](../app/src/main/kotlin/com/johnturkson/sandbox/data/ItemApi.kt)
receives `HttpClient` through `@Inject`; it currently has no endpoints.
`MainModule.provideHttpClient` configures CIO, `ContentNegotiation` with the
provided `Json`, `Logging`, and `Resources`. Extend the shared configuration
rather than creating a new client in each feature.

Keep requests and response decoding in API wrappers, data coordination in
repositories, and presentation state in ViewModels. The current constructor
dependencies establish this path:

```text
HttpClient -> ItemApi -> MainRepository -> MainViewModel -> UI
```

Keep network serialization and database annotations on their respective models.
[Item](../app/src/main/kotlin/com/johnturkson/sandbox/data/Item.kt) is a sealed
interface exposing `id`;
[ItemNetworkModel](../app/src/main/kotlin/com/johnturkson/sandbox/data/ItemNetworkModel.kt)
implements it with `@Serializable`, while
[ItemDatabaseModel](../app/src/main/kotlin/com/johnturkson/sandbox/data/ItemDatabaseModel.kt)
implements it with `@Entity` and `@PrimaryKey`. Follow this shared contract and
distinct representation pattern where the models describe the same item. UI
components should consume only the values they need.

[KtorSerializationTest](../app/src/test/kotlin/com/johnturkson/sandbox/KtorSerializationTest.kt)
is the working example for decoding a network model with Ktor `MockEngine` and
`runTest`. It verifies the request path, JSON decoding, and ignored unknown fields
without a live service. Use that approach when implementing API operations. The
installed `Resources` plugin has no typed resource endpoints yet; discuss the
endpoint conventions when implementing the first real requests.
