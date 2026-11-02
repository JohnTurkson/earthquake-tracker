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

[EarthquakeMonitoringCard](../app/src/main/kotlin/com/johnturkson/sandbox/ui/EarthquakeMonitoringCard.kt)
selects the refresh action's accessibility description in priority order:

```kotlin
when {
    state.refresh == EarthquakeMonitoringState.Refresh.Queued -> R.string.refresh_queued
    state.isRefreshing -> R.string.refreshing_earthquakes
    else -> R.string.refresh_earthquakes
}
```

[EarthquakeScreen](../app/src/main/kotlin/com/johnturkson/sandbox/ui/EarthquakeScreen.kt)
uses the same pattern for map padding.

## Date and time values

Follow the [date and time conventions](date-time.md). Keep moments as
`kotlin.time.Instant` and intervals as `kotlin.time.Duration`, using the current
Kotlin date/time APIs and `kotlinx.datetime` calendar/time-zone types where
applicable. Serialize instants to ISO-8601 strings at storage boundaries and
convert automatically; keep entities, queries, state, and navigation arguments
typed. The dedicated guide describes the required sortable Room format and
legacy migration.
