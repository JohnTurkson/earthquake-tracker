# Architecture and state management guidelines

Build on the concrete patterns already present in this project. These conventions
apply to agents and human contributors. Each section links to the implementation
that establishes the pattern and explains how to extend it.

The current implementation still contains timer UI and `Item` data scaffolding.
Use their responsibility boundaries when developing USGS features. Product
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

`MainState` currently exposes only `navKey`; `MainScreen` reads `timers` directly
from the ViewModel, and its preview is commented out. When expanding this screen,
extend its read-only state contract to cover the values the UI needs and provide
deterministic preview and test implementations.

The `Timer` model in `MainViewModel.kt` shows the more detailed state pattern:

```kotlin
@Stable
interface Timer {
    val id: Uuid
    val duration: Duration
    val isRunning: Boolean
}
```

`MutableTimer` implements that interface with serializable Compose state internally.
The ViewModel exposes `List<Timer>` and owns changes through `addTimer`,
`toggleTimer`, and `toggleAllTimers`. Preserve that boundary: UI consumers receive
the read-only interface and request changes through actions. Do not create a
second copy of the same feature state inside a composable.

[MainScreen](../app/src/main/kotlin/com/johnturkson/sandbox/ui/MainScreen.kt)
contains both a screen entry and a reusable `Timer` composable. Keep Hilt lookup
and lifecycle integration at the screen entry. Pass values or read-only models,
callbacks, and a `Modifier` into rendered components, following this existing
component signature:

```kotlin
@Composable
fun Timer(
    timer: Timer,
    onClick: (Timer) -> Unit,
    modifier: Modifier = Modifier,
)
```

The timer row formats `duration` for the current locale and sends clicks through
`onClick`; the ViewModel decides how `isRunning` changes. The screen owns its
`LazyListState` and uses `LifecycleStartEffect` to request updates while started
and stop them on stop or disposal. Keep UI layout objects out of ViewModels.

Every app UI component must have screenshot tests for all of its supported visual
states. Add or update coverage with each UI change. Enumerate those states from
its state contract and inputs, and render deterministic fixtures without live
storage, network calls, or ticking clocks. For the existing timer row, include
running and stopped states with zero and nonzero duration, including hour
formatting. For the current main screen, include empty and populated lists and
mixed running states. Previews and UI assertions alone do not satisfy screenshot
coverage.

There is currently no screenshot test framework or baseline suite.
[MainComposeTest](../app/src/androidTest/kotlin/com/johnturkson/sandbox/MainComposeTest.kt)
contains empty-content Compose and restoration scaffolding;
[MainActivityTest](../app/src/androidTest/kotlin/com/johnturkson/sandbox/MainActivityTest.kt)
checks startup and recreation. When implementing the first screenshot tests, ask
the user how to establish the framework, baseline review, and state coverage
conventions, then document the agreed approach alongside the working examples.
Report missing coverage explicitly until it exists.

## Saved state

Inject `SavedStateHandle` into the ViewModel for state that needs restoration.
Use the existing serialization pattern instead of maintaining an unrelated
saved copy of the same state. `MainViewModel.timers` uses a
`SnapshotStateListSerializer<MutableTimer>` and exposes the result through
`List<Timer>`:

```kotlin
val timers: List<Timer> field = savedStateHandle.saved(
    serializer = SnapshotStateListSerializer<MutableTimer>(),
) {
    mutableStateListOf()
}.getValue(this, this::timers)
```

`MutableTimer` is `@Serializable`. Its private `MutableState<Duration>` and
`MutableState<Boolean>` properties use `MutableStateSerializer`, and its public
properties delegate to those state holders. Follow those serializer choices when
adding equivalent observable, restorable state.

Decide which values must restore when introducing state. Keep one authoritative
owner and derive dependent values from it, as `runningTimers` is derived from
`timers`. Saved state supports UI restoration; durable application data belongs
in storage accessed through the repository.

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
`HttpClient`, Room database and DAO, clock dependencies, and qualified coroutine
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
receives `@IoDispatcher`, and the ViewModel receives `@DefaultDispatcher`.
`MainModule` also provides `@MainDispatcher`, `MonotonicFrameClock`, and
`TimeSource`. Keep these dependencies replaceable instead of hardcoding
dispatchers or clocks into feature logic. The current timer update loop is work
in progress; its injection pattern does not establish a finished timekeeping
algorithm.

## Navigation

Use Navigation 3. Add serializable destination keys to
[Destination](../app/src/main/kotlin/com/johnturkson/sandbox/ui/Destination.kt),
which is a sealed `NavKey` interface. Existing destinations are `@Serializable`
data objects, `MainRoute` and `DetailRoute`.

[MainContent](../app/src/main/kotlin/com/johnturkson/sandbox/ui/MainContent.kt)
owns `rememberNavBackStack(MainRoute)`, `NavDisplay`, and the typed
`entryProvider` entries. It installs the saveable state holder, ViewModel store,
and result event bus entry decorators. Preserve these entry scopes when adding
destinations; pass the entry's route to the screen and its assisted ViewModel
factory when needed.

Keep back stack changes in `MainContent`. The current screens send result events
through `LocalResultEventBus`: `MainScreen` sends `"addTimerDetails"`, and
[DetailScreen](../app/src/main/kotlin/com/johnturkson/sandbox/ui/DetailScreen.kt)
sends `"saveTimer"`. `ResultEffect` handlers in `MainContent` push or pop
`DetailRoute`. Follow this event and handler boundary when extending the current
flow; keep navigation infrastructure out of rendered components and data code.

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
