# Database instructions

Read this guide before adding or changing Room entities, relationships,
migrations, DAO queries, or repository database access. Follow the
[architecture](architecture.md), [formatting](formatting.md),
[testing](testing.md), and [source control](source-control.md) guides.
Ask about conventions only when introducing a pattern
these guides and the current conversation do not cover.

Call persistent earthquake storage the Room database or database. It is the
durable source for offline reads, with no automatic expiration or separate
in-memory earthquake cache. Deduplicate by the USGS ID primary key; merge
newer revisions transactionally and retain known aliases when the preferred
ID changes. For example, refreshing `[A, B]` with `[B, C]` retains `[A, B, C]`
and updates B when its source revision is newer.

## Observable reads

All database read methods must return observable `Flow` types and have names
ending in `Flow`. This applies to DAO SELECTs, internal SELECT helpers, and
repository methods that expose stored data. Use ordinary `fun`, not `suspend
fun`, for functions returning a Flow. Follow these implemented APIs:

```kotlin
fun getEarthquakesFlow(
    startTime: Instant? = null,
    endTime: Instant? = null,
    minimumMagnitude: Double? = null,
): Flow<List<EarthquakeDatabaseModel>>

fun getEarthquakeFlow(id: String): Flow<EarthquakeDatabaseModel?>
```

[EarthquakeDao](../app/src/main/kotlin/com/johnturkson/sandbox/data/EarthquakeDao.kt)
is the query example; [EarthquakeRepository](../app/src/main/kotlin/com/johnturkson/sandbox/data/EarthquakeRepository.kt)
preserves observable results through mapping. A missing single row emits null;
a missing collection emits an empty list. Values must update when relevant
tables change. Do not wrap a one-shot query in `flowOf`, maintain a manual copy
of Room data, or reduce a public read method to `.first()` and return a snapshot.
Database reads never fetch network data implicitly.

Room may rerun queries after table-level invalidations; do not promise exactly
one emission for every write or suppress meaningful state changes in tests.

## Entities, queries, and Hilt

Keep `@Entity` and `@PrimaryKey` on database representations, separate from
network serialization annotations. Follow
[EarthquakeDatabaseModel](../app/src/main/kotlin/com/johnturkson/sandbox/data/EarthquakeDatabaseModel.kt)
for primary keys, indexes, and alias foreign keys. Keep earthquake fields in
columns. Product metadata and separate detail snapshots are not stored.

`EarthquakeRepository` exposes only observable Room reads, including
`getRefreshFlow()` for the last successful refresh. It has no network dependency
or refresh methods. UI callers enqueue `EarthquakeWorkScheduler.refresh()`;
`EarthquakeRefreshWorker` alone invokes the refresh repository to fetch and save.

Register entities and DAO accessors in
[MainDatabase](../app/src/main/kotlin/com/johnturkson/sandbox/data/MainDatabase.kt)
and provide DAOs from
[EarthquakeModule](../app/src/main/kotlin/com/johnturkson/sandbox/app/EarthquakeModule.kt).
Use constructor injection for repositories; do not construct databases in
ViewModels or composables. Room schedules its query Flows; move expensive
repository transformations to the injected dispatcher when needed.

Specify filters, boundaries, null handling, and stable ordering in the DAO.
Earthquake lists use a half-open occurrence-time range `[startTime, endTime)`,
an optional minimum magnitude, and descending time with ID as a tie breaker.
Magnitude filtering excludes null magnitudes; unfiltered lists include them.
Single-event reads resolve preferred IDs and retained USGS aliases.

## Writes and transaction reads

Inserts, updates, deletes, and merge operations remain explicit suspend methods.
They perform effects when called and do not return a Flow to trigger a write on
collection. Keep atomic reads and writes inside the same Room transaction.

The merge in `EarthquakeDao` takes one emission of its internal query Flows
inside the write transaction, for example:

```kotlin
val existing = getAssociatedEarthquakesFlow(incomingAliases).first()
```

These helpers still expose Flow types. Use the snapshot only for that atomic
decision; do not start ongoing collectors inside a write transaction. Preserve
the transaction context and verify concurrent merges and rollback on Android.

Keep the existing earthquake merge invariants: newer USGS `updated` values win,
equal common-field revisions keep existing values, and older common values
cannot overwrite newer ones. The source timestamp is an internal merge input,
not a separate freshness state exposed to callers. Preserve known detail URLs
when omitted from an incoming record. Use explicit USGS ID associations when
merging preferred-ID changes; retain aliases without duplicate stored events.
Insert previously unseen IDs and retain events absent from a later response.
Refreshes must never clear the earthquake table or delete events outside the
requested time window. The unfiltered `getEarthquakesFlow()` reads all retained
history, including after the database is reopened without network access.
Empty feeds and refresh failures preserve existing data. See the
[data guide](earthquake-data.md) for the database's deletion and split limitations.

## Schema history and verification

Increment the database version for schema changes and preserve existing data.
Use auto-migrations where supported by the actual schema change. Remove an
obsolete table with an explicit nested `AutoMigrationSpec` annotated with
`@DeleteTable`, as in `MainDatabase.RemoveProductsMigration` and
`MainDatabase.EarthquakeOnlyMigration`. Register the spec on that version's
`AutoMigration`. Delete only the obsolete table; retain earthquake records,
aliases, refresh metadata, and alert history. Establish other new migration
patterns with the user when needed. Keep generated schema history under
`app/schemas` in Git; never format or hand-edit it. The current version is 6:
additive migrations from version 1 to 2 and 2 to 3, removal of
`earthquake_details` from version 3 to 4, conversion of numeric timestamp columns
to ISO-8601 text from 4 to 5, and removal of the unused item table from 5 to 6.
Register the explicit `EarthquakeTimestampMigration` with `addMigrations` on
disk-backed builders. Keep older schema exports even when they describe removed
tables. A read-method rename does not change the schema or require another
database version.

Extend [EarthquakeStorageTest](../app/src/androidTest/kotlin/com/johnturkson/sandbox/EarthquakeStorageTest.kt)
for behavior using real Room and Turbine on Android. Keep collectors active
across inserts and revisions, including filter membership, null-to-value reads,
refresh metadata and preferred-ID changes. Assertions on `.first()`
alone do not establish observability. Retain offline-reopen, transaction,
concurrency, and refresh-failure coverage when touching those paths. Use
[EarthquakeMigrationTest](../app/src/androidTest/kotlin/com/johnturkson/sandbox/EarthquakeMigrationTest.kt)
for schema upgrades and data retention.

Update this guide with agreed conventions and implementation examples. Run and
report the applicable checks from the testing guide, with an explicitly selected
device for storage tests. Do not claim UI or screenshot verification from DAO
tests; storage tests alone do not verify the connected explorer UI.

`EarthquakeRefreshDatabaseModel` and `EarthquakeAlertDatabaseModel` add refresh
metadata and delivered-notification history in version 3. `EarthquakeMonitoringDao`
provides observable reads and explicit writes. Advance refresh metadata in the
same database transaction as the event merge. `getDeliveredAlertsFlow` joins the
retained USGS alias table to deduplicate alerts across preferred-ID changes. The
monitoring history has no cascading link to preferred earthquake rows.

Follow the [date and time conventions](date-time.md) for `Instant` entity fields
and query parameters, automatic Room converters, and the fixed UTC format required
for chronological text comparisons.
