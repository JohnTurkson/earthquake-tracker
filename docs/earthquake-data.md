# Earthquake data

Room is the source of truth for earthquake records, retained history, local ID
lookups, and the last successful refresh timestamp. WorkManager owns all runtime
updates. Hilt provides the shared Ktor client, `MainDatabase`, and `EarthquakeDao`; constructor injection
provides `EarthquakeApi` and `EarthquakeRepository`. The explorer observes the
unrestricted stored history. `EarthquakeRefreshRepository` reuses this API and
merge path for the shared one-time refresh worker. Manual refresh, startup, and
every foreground-monitoring tick enqueue that same unique work.

## USGS formats and revisions

Use USGS's [GeoJSON summary feeds](https://earthquake.usgs.gov/earthquakes/feed/v1.0/geojson.php)
for runtime earthquake updates. All 20 documented summary combinations are modeled:
all events, magnitude 1.0+, 2.5+, 4.5+, and significant events for hour, day,
week, and month windows. All-events feeds can include other seismic event
types; retain `type` rather than silently treating every feature as a confirmed
earthquake. Summary entries include a detail URL; detail responses can omit
that property and include product metadata. The app ignores unused products;
there are no product/content models or stored product snapshots.

An earthquake record is mutable. USGS explains that
[magnitude and location are revised](https://www.usgs.gov/faqs/whywhen-does-usgs-update-magnitude-earthquake)
as observations arrive and analysts review events, including later catalog
revisions. Compare each feature's `properties.updated`, not its occurrence time
or the collection's generation time, when deciding which stored values to keep.
USGS wire times are epoch milliseconds; app models use `Instant` and storage
uses ISO-8601 text. See the [date and time conventions](date-time.md). Coordinates are longitude, latitude, and depth in
kilometers. Missing measurements remain null, not zero.

Do not treat the preferred feature ID as an immutable identifier for a physical
event. USGS's [indexer documentation](https://usgs.github.io/pdl/indexer.html)
states that the event ID comes from the preferred origin product. The
[current PDL documentation](https://ghsc.code-pages.usgs.gov/hazdev/pdl/userguide-2.9.1/receiving.html)
distinguishes the preferred ID from known associated event IDs. The database uses
the GeoJSON feature ID and comma-delimited `properties.ids` associations for
lookup after a preferred-ID change and to avoid duplicate associated rows.
Use those explicit associations for deduplication; similar coordinates, event
times, or magnitudes alone do not establish that two earthquakes are the same.

## Calling the data layer

Inject `EarthquakeRepository` for observable local reads. It has no network
client or write methods. Inject `EarthquakeWorkScheduler` to request refreshes:

```kotlin
val earthquakes = repository.getEarthquakesFlow()
val earthquake = repository.getEarthquakeFlow("us7000example")
val refresh = repository.getRefreshFlow()
val recent = repository.getEarthquakesFlow(
    startTime = Instant.parse("2026-10-01T00:00:00Z"),
    endTime = Instant.parse("2026-11-01T00:00:00Z"),
    minimumMagnitude = 2.5,
)
workScheduler.refresh()
```

`EarthquakeRefreshWorker` alone calls `EarthquakeRefreshRepository.refresh()`.
The repository fetches the all-day summary, validates the complete response, and
merges records and the successful-refresh timestamp in one Room transaction.
The foreground monitoring worker enqueues that same unique one-time work every
minute while active. It awaits only the enqueue operation; it does not wait for
the refresh or copy its result. `KEEP` coalesces manual and monitoring requests.
Stopping monitoring cancels future polling, leaving an already queued or running
refresh to finish. Android can delay or stop work; the minute interval is a
requested cadence.

Observe WorkManager for queued/running state and success or generic failure.
Observe Room independently for earthquake records and successful-refresh metadata. Reading a Flow does not initiate network access. No in-memory
freshness flag or separate detail revision is exposed.

`EarthquakeApi` retains typed summary, single-event, and catalog GET endpoints.
Single-event decoding reads ordinary earthquake fields and ignores product
metadata. Low-level API calls do not persist data. Runtime updates use the
one-time worker, without alternate repository refresh methods.

Every successful collection refresh merges into the existing database: unseen IDs
are inserted, newer revisions update associated events, and events absent from
the response remain available. Reading without time filters returns all retained
history, including after process termination or a device restart. No automatic
history expiration is configured.

While the app is running, keep the screen observing the database's unfiltered
`getEarthquakesFlow()`. Refresh operations only write to that database; do not
replace the displayed history with the latest network response. Network loss
or a failed refresh leaves the existing database observation and stored rows
available. Apply display filters separately without deleting history.

An empty feed does not erase previously seen information. The refresh worker
reports HTTP, decoding, validation, and storage exceptions as failed work
while retaining existing rows and the last successful timestamp. Cancellation
propagates.

## Refresh boundaries

The past-hour feed describes events that occurred in that window. It is not a
stream of all records modified during the last hour. Refresh a broader summary
window or the [FDSN event query](https://earthquake.usgs.gov/fdsnws/event/1/)
can revisit older events with `updatedafter`. The current refresh worker uses
only the all-day summary. Adding catalog synchronization requires routing it
through the same unique refresh worker rather than adding a repository writer.

The low-level catalog API returns one page without storing it. Offsets start at 1 and the service
limits a page to 20,000 events. The caller must choose its occurrence-time range,
paginate, and revisit overlapping ranges as needed; the implementation does not
claim complete incremental synchronization or advance a durable watermark.
Without `startTime`, the service defaults to the last 30 days, even when
`updatedAfter` is specified. Catalog HTTP 204 returns null.

The service supports deleted-event options, but this database does not reconcile
deletions or event disassociations. Disappearance from a rolling feed is not
proof of deletion. Retention limits and full-catalog synchronization are not
implemented. Foreground monitoring requests a refresh every minute while its
WorkManager session runs; Android can stop or delay execution. USGS documents
[feed caching intervals](https://www.usgs.gov/faqs/how-quickly-earthquake-information-posted-usgs-website-and-sent-out-earthquake-notification);
repeated requests do not guarantee newly generated data.

See the [network instructions](networking.md), [database instructions](database.md),
[architecture conventions](architecture.md#earthquake-history-and-offline-storage),
and [automated verification examples](testing.md#earthquake-data-tests).
