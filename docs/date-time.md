# Date and time conventions

Keep timestamps typed throughout network models, repositories, Room entities and
queries, state contracts, and UI models. Use `kotlin.time.Instant` for moments and
`kotlin.time.Duration` for intervals. These are the current Kotlin date/time APIs:
`Instant` and `Clock` [moved from `kotlinx.datetime` to the standard library](https://github.com/Kotlin/kotlinx-datetime#deprecation-of-instant). Use
`kotlinx.datetime` types when calendar or time-zone values are needed; keep a
moment as `Instant` rather than a local date/time or a formatted string. Do not
introduce the deprecated `kotlinx.datetime.Instant` alias.

## Typed values and arithmetic

[EarthquakeAlertPolicy](../app/src/main/kotlin/com/johnturkson/sandbox/data/EarthquakeAlertPolicy.kt)
accepts `now: Instant` and checks `now - item.time in Duration.ZERO..1.minutes`.
Name fields for their meaning, as with `eventTime`, `updatedAt`, `deliveredAt`,
and `timestamp`; avoid millisecond suffixes on typed values. Optional instants
remain nullable. The database query uses nullable `Instant` bounds for an
unrestricted range, rather than numeric sentinel timestamps.

Read wall-clock moments directly with `Clock.System.now()`, as in
[EarthquakeRefreshRepository](../app/src/main/kotlin/com/johnturkson/sandbox/data/EarthquakeRefreshRepository.kt). Use a monotonic `TimeSource`
and `TimeMark` to measure elapsed time. The polling loop accepts a `TimeSource`
and a `Duration`; its tests use the coroutine scheduler's `timeSource`. A wall
clock can change during an interval and must not determine polling delays.

## Storage and serialization boundaries

Room persists instants as ISO-8601 `TEXT`, using the automatically applied
[EarthquakeInstantConverters](../app/src/main/kotlin/com/johnturkson/sandbox/data/EarthquakeInstantConverters.kt).
Entities and DAO parameters still use `Instant`. Every indexed/comparable stored
value uses UTC, a four-digit year, and exactly nine fractional-second digits:
`2026-11-01T00:00:00.123456789Z`. This preserves nanoseconds and makes SQLite's
ordinary text ordering chronological. Mixed offsets or variable fractional
precision must not be written to these columns. Supported stored years are
0000 through 9999; the converter rejects values outside that range rather than
silently misordering expanded or negative years. No numeric timestamp arithmetic
belongs in these text queries.

Use data-preserving migrations when changing a stored representation.
[EarthquakeTimestampMigration](../app/src/main/kotlin/com/johnturkson/sandbox/data/EarthquakeTimestampMigration.kt)
converts legacy numeric columns, preserves aliases and relationships, and retains
unrelated legacy rows. It upgrades version 4 to version 5 after the existing
product-removal migration. Register it on disk-backed Room builders. Room's generated schema exports remain the schema history.

The feed's numeric wire timestamps use
[EarthquakeEpochMillisecondsSerializer](../app/src/main/kotlin/com/johnturkson/sandbox/data/EarthquakeEpochMillisecondsSerializer.kt).
Convert at decoding/encoding boundaries; the API's wire format does not determine
the database's storage format.

Preferences DataStore stores the location timestamp as an ISO string, parses it
on read, and compares typed instants before accepting a newer location. The
location repository reads the legacy numeric key and replaces it on the next
accepted location write. Missing or malformed timestamps remain null while valid
coordinates stay available. New work should write ISO strings rather than add
numeric timestamp preferences.

## Android and navigation boundaries

Convert to epoch milliseconds only where an Android API requires them, such as
`Location.time`, notification `setWhen`, and the device's `DateFormat` formatting
through `Date`. Convert SDK readings to `Instant` immediately. Convert typed
durations to primitive units only at SDK boundaries that require them. Keep
localized display labels in the UI.

Navigation currently carries event IDs, with no timestamp arguments. Kotlin
serialization supports `kotlin.time.Instant` as an ISO string automatically, so
a serializable route or saved-state value can keep its property typed as
`Instant`; callers do not need to change the property to `String`. For an
external URI or platform API that requires text, encode an ISO value and parse
it at that boundary. The catalog query's typed instant arguments demonstrate
ISO serialization through Ktor Resources.

Use deterministic typed fixtures. The instant, alert, DataStore, migration, and
Room storage tests cover wire decoding, string round trips, offset normalization,
nanosecond boundaries, chronological text queries, and legacy data retention.
Follow the [testing guide](testing.md) for required host and device checks.
