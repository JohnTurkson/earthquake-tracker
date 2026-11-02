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

[EarthquakeNotifications.foreground](../app/src/main/kotlin/com/johnturkson/sandbox/data/EarthquakeNotifications.kt)
selects a notification title in priority order:

```kotlin
when {
    offline -> R.string.monitoring_offline
    refreshing -> R.string.refreshing_earthquakes
    failed -> R.string.refresh_failed
    else -> R.string.monitor_notification_title
}
```

[EarthquakeScreen](../app/src/main/kotlin/com/johnturkson/sandbox/ui/EarthquakeScreen.kt)
uses the same pattern for map padding, and
[EarthquakeMonitoringCard](../app/src/main/kotlin/com/johnturkson/sandbox/ui/EarthquakeMonitoringCard.kt)
uses it for the refresh action's accessibility description.
