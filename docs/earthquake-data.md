# Earthquake data

The network and Room layers support recent-earthquake lists, detail metadata,
observable local ID lookups and queries, and explicit refreshes. Hilt provides the
shared Ktor client, `MainDatabase`, and `EarthquakeDao`; constructor injection
provides `EarthquakeApi` and `EarthquakeRepository`. These operations are not
connected to the UI yet.

## USGS formats and revisions

Use USGS's [GeoJSON summary feeds](https://earthquake.usgs.gov/earthquakes/feed/v1.0/geojson.php)
for recent lists and [GeoJSON detail](https://earthquake.usgs.gov/earthquakes/feed/v1.0/geojson_detail.php)
for event product metadata. All 20 documented summary combinations are modeled:
all events, magnitude 1.0+, 2.5+, 4.5+, and significant events for hour, day,
week, and month windows. All-events feeds can include other seismic event
types; retain `type` rather than silently treating every feature as a confirmed
earthquake. Summary entries include a detail URL; detail responses can omit
that property and include a dynamic `products` map instead.

An earthquake record is mutable. USGS explains that
[magnitude and location are revised](https://www.usgs.gov/faqs/whywhen-does-usgs-update-magnitude-earthquake)
as observations arrive and analysts review events, including later catalog
revisions. Compare each feature's `properties.updated`, not its occurrence time
or the collection's generation time, when deciding which stored values to keep.
Times are epoch milliseconds. Coordinates are longitude, latitude, and depth in
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

Inject `EarthquakeRepository`. Refreshes write Room; read operations return only
stored data as observable `Flow` types and work without network access. Database
read method names end in `Flow`; writes and network operations remain suspend
functions:

```kotlin
repository.refreshEarthquakes(EarthquakeFeed.ALL_HOUR)
val earthquake = repository.getEarthquakeFlow("us7000example")
val earthquakes = repository.getEarthquakesFlow()
val recent = repository.getEarthquakesFlow(
    startTime = startEpochMillis,
    endTime = endEpochMillis,
    minimumMagnitude = 2.5,
)
repository.refreshEarthquake("us7000example")
val detail = repository.getEarthquakeDetailFlow("us7000example")
```

`EarthquakeApi.getEarthquakes` returns a collection response;
`getEarthquake` returns one Feature with products. Both use nested Ktor
`@Resource` routes and GET requests with no body. Product types, properties,
contents, checksums, and inline content are represented; referenced files such
as ShakeMap images or QuakeML documents are not downloaded. Stored products are
decoded when observing detail. `isDetailCurrent` is false if a newer summary
arrived after the stored detail snapshot; stored products remain readable.

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

An empty feed does not erase previously seen information. HTTP, decoding,
validation, and storage failures propagate, and cancellation is not caught.
Choose refresh error presentation and retry behavior when wiring the UI.

## Refresh boundaries

The past-hour feed describes events that occurred in that window. It is not a
stream of all records modified during the last hour. Refresh a broader summary
window or use the [FDSN event query](https://earthquake.usgs.gov/fdsnws/event/1/)
to revisit older events with `updatedafter`:

```kotlin
repository.refreshQuery(
    EarthquakeCatalogResources.Query(
        startTime = "2026-01-01T00:00:00Z",
        updatedAfter = "2026-10-31T00:00:00Z",
        limit = 1000,
        offset = 1,
    )
)
```

This operation fetches and stores one page. Offsets start at 1 and the service
limits a page to 20,000 events. The caller must choose its occurrence-time range,
paginate, and revisit overlapping ranges as needed; the implementation does not
claim complete incremental synchronization or advance a durable watermark.
Without `startTime`, the service defaults to the last 30 days, even when
`updatedAfter` is specified. Catalog HTTP 204 leaves the database unchanged.

The service supports deleted-event options, but this database does not reconcile
deletions or event disassociations. Disappearance from a rolling feed is not
proof of deletion. Retention limits, background refresh, polling cadence, and
full-catalog synchronization are not implemented. USGS documents
[feed caching intervals](https://www.usgs.gov/faqs/how-quickly-earthquake-information-posted-usgs-website-and-sent-out-earthquake-notification);
repeated requests do not guarantee newly generated data.

See the [network instructions](networking.md), [database instructions](database.md),
[architecture conventions](architecture.md#earthquake-history-and-offline-storage),
and [automated verification examples](testing.md#earthquake-data-tests).
