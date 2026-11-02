# Coding conventions

Apply these conventions to Kotlin code throughout the repository, alongside the
[formatting guidelines](formatting.md) and
[architecture and state management guidelines](architecture.md).

## Conditional branches

Strongly prefer `when` over an `if` / `else if` chain with three or more branches,
counting a final `else` as a branch. This applies to both expressions that produce
a value and statements that perform actions or render UI. Simple one-condition
checks and two-branch `if` / `else` expressions can remain as `if`.

Use `when (value)` when the branches match the same value. Use a subjectless
`when` for different Boolean conditions. Preserve the original condition order
when converting a chain: the first matching branch wins. Keep any fallback in
the final `else` branch.

[EarthquakeMonitoringCard](../app/src/main/kotlin/com/johnturkson/sandbox/ui/monitoring/EarthquakeMonitoringCard.kt)
selects the refresh action's accessibility description in priority order:

```kotlin
when {
    state.refresh == EarthquakeMonitoringState.Refresh.Queued -> R.string.refresh_queued
    state.isRefreshing -> R.string.refreshing_earthquakes
    else -> R.string.refresh_earthquakes
}
```

[EarthquakeScreen](../app/src/main/kotlin/com/johnturkson/sandbox/ui/map/EarthquakeScreen.kt)
uses the same pattern for map padding.

## Date and time values

Follow the [date and time conventions](date-time.md). Keep moments as
`kotlin.time.Instant` and intervals as `kotlin.time.Duration`, using the current
Kotlin date/time APIs and `kotlinx.datetime` calendar/time-zone types where
applicable. Serialize instants to ISO-8601 strings at storage boundaries and
convert automatically; keep entities, queries, state, and navigation arguments
typed. The dedicated guide describes the required sortable Room format and
initial schema.

## String resources

Use **Earthquake Tracker** as the app's display name through `app_name` in
[`strings.xml`](../app/src/main/res/values/strings.xml). The
[`AndroidManifest.xml`](../app/src/main/AndroidManifest.xml) application label
uses that resource for launcher and system UI. Keep app text and internal names
independent of the data provider; retain technical service URLs as required.

Keep app-authored display text in Android string resources. Use numbered format
arguments for dynamic values so translations can reorder them. Resolve resources
with `stringResource` in composable functions and Context resource APIs such as
`context.getString` in non-composable Android code. Keep resource resolution in
the UI or the owning Android component rather than adding a Context to ViewModels
for display formatting.

[EarthquakeNotifications](../app/src/main/kotlin/com/johnturkson/sandbox/data/notifications/EarthquakeNotifications.kt)
uses a nonblank source title with the established magnitude-label formatting and
otherwise formats `earthquake_fallback_title` with named `magnitudeText` and
`placeText` values. Known magnitudes include the `M` prefix through
`earthquake_magnitude_value`; missing values use `unknown_magnitude` and
`unknown_location`:

```kotlin
context.getString(
    R.string.earthquake_fallback_title,
    magnitudeText,
    placeText,
)
```

[EarthquakeMapItem](../app/src/main/kotlin/com/johnturkson/sandbox/ui/event/EarthquakeMapItem.kt)
retains nullable source title and place values. Its composable `earthquakeTitle`
helper uses `stringResource` for the same fallback template, the one-decimal
`earthquake_magnitude` format (including its `M` prefix), `unknown_location`, and
`unknown_magnitude`. Missing magnitudes display **Unknown Magnitude** without an
`M` prefix. `EarthquakeDetailsCard` uses `unknown_magnitude` for its header and
event-information row. Keep the fallback title template independent of the
magnitude prefix so it can accept either a known value or the missing-value text.
Numeric resource formatting follows the current resource locale,
as the coordinate and cluster labels do. List rows, selection cards, and map
marker titles resolve that text in composition, following the existing
`earthquakeEventTime` helper pattern. SDK cluster wrappers receive the resolved
title from the map composable.

Keep fallback formatting readable with a block body and explicit returns, as in
`earthquakeTitle`: return an available source title first, then resolve named
`magnitudeText` and `placeText` values before passing them to the fallback template.
Keep the numeric `magnitude` separate from its formatted text instead of nesting
resource calls and scope functions inside the template arguments.

## Names and inferred types

Use `UPPER_SNAKE_CASE` for `const val` declarations, including companion-object
constants. Keep persisted string values independent of identifier renames, as in
[EarthquakeMonitor](../app/src/main/kotlin/com/johnturkson/sandbox/work/EarthquakeMonitor.kt)
and [EarthquakeNotifications](../app/src/main/kotlin/com/johnturkson/sandbox/data/notifications/EarthquakeNotifications.kt).
Functions returning `Unit` and annotated `@Composable` start with an uppercase
letter, including screenshot-test previews. Ordinary test methods remain lower
camel case. The [screenshot testing guide](testing.md#compose-screenshot-tests)
describes how to preserve references when renaming previews.

Specify the intended nullable or non-null type when a property initializer
returns an unchecked Java platform type. The Maps key in
[the app build script](../app/build.gradle.kts) is `String?`, preserving the
missing-key check; the formatter in
[EarthquakeInstantConverters](../app/src/main/kotlin/com/johnturkson/sandbox/data/database/EarthquakeInstantConverters.kt)
is a non-null `DateTimeFormatter`.

## Compose parameters and numeric state

Place `modifier: Modifier = Modifier` before other optional composable parameters,
as in [EarthquakeNotificationsScreen](../app/src/main/kotlin/com/johnturkson/sandbox/ui/notifications/EarthquakeNotificationsScreen.kt).
Use `mutableIntStateOf`, `mutableLongStateOf`, `mutableFloatStateOf`, or
`mutableDoubleStateOf` for non-null numeric Compose state. Keep nullable numeric
state, Boolean state, value classes such as `Dp`, and object state in
`mutableStateOf`; do not replace a meaningful null with a numeric sentinel.
[EarthquakeApp](../app/src/main/kotlin/com/johnturkson/sandbox/ui/navigation/EarthquakeApp.kt)
uses `mutableLongStateOf` for its revision and `mutableStateOf<Long?>` for a
pending revision that can be absent.

Use Kotlin type inference for obvious collection assignments, and destructure
pairs into meaningful names instead of reading `.first` and `.second` repeatedly.
Keep range checks explicit about the upper bound, as in the converter's
`value in Minimum..<Maximum` validation.

For a Boolean operation gated by several capabilities, use a short-circuiting
`&&` chain when it reads clearly. In
`EarthquakeNotifications.previewAlert`, the notification is constructed and
posted only after all capability checks succeed. Select related display-resource
IDs together when they share the same conditions, as the full and short title
pair in [EarthquakeMonitoringCard](../app/src/main/kotlin/com/johnturkson/sandbox/ui/monitoring/EarthquakeMonitoringCard.kt).

## Build-version compatibility

Keep AGP pinned to **9.1.1** for IntelliJ IDEA compatibility. Follow the
[build guide](building.md#prerequisites) when updating dependencies or build tools;
newer-version inspection suggestions must respect this pin. The concrete version
is declared in [the version catalog](../gradle/libs.versions.toml).
