# Network instructions

Read this guide before adding or changing API routes, requests, response models,
or network operations. Follow the [architecture](architecture.md),
[formatting](formatting.md), [testing](testing.md), and
[source control](source-control.md) guides.
Ask about conventions only when introducing a pattern
these guides and the current conversation do not cover.

## Routes and requests

Use nested Ktor `@Resource` classes as in
[EarthquakeResources](../app/src/main/kotlin/com/johnturkson/sandbox/data/EarthquakeResources.kt).
Give nested resources a parent property with a default instance. Keep route
templates relative to the USGS host configured on the shared Hilt client in
[EarthquakeModule](../app/src/main/kotlin/com/johnturkson/sandbox/app/EarthquakeModule.kt).
Use typed resource arguments for path and query parameters. Use `@SerialName`
when Kotlin argument names differ from wire names, as with `updatedAfter` and
`updatedafter`. Use serializable enums for the established summary feed names.

Confirm the method, URL, parameters, defaults, response shapes, and status codes
against the owning service's current documentation. For USGS, consult the
[summary](https://earthquake.usgs.gov/earthquakes/feed/v1.0/geojson.php),
[detail](https://earthquake.usgs.gov/earthquakes/feed/v1.0/geojson_detail.php), and
[catalog](https://earthquake.usgs.gov/fdsnws/event/1/) documentation as relevant.
Check installed Ktor versions before applying a newer documentation example.

The implemented endpoints are GET operations with no request body. Their
resource instances are the typed request arguments; do not add a fictitious
body model. If a future service operation actually requires a body, establish
its body and serialization conventions with the user before introducing it.
Validate supported argument bounds before sending, as the catalog query does
for its limit and one-based offset.

## Responses and API operations

Keep wire models `@Serializable`, separate from Room entities. Follow
[EarthquakeNetworkModel](../app/src/main/kotlin/com/johnturkson/sandbox/data/EarthquakeNetworkModel.kt)
for GeoJSON collections, features, metadata, Point geometry, properties,
products, and content metadata. Reuse models when response shapes match.
Preserve nullable measurements, required identity and revision fields, dynamic
product maps, and `JsonObject` product properties. Preserve numeric precision
and units: epoch milliseconds use `Long`; coordinates are longitude and latitude
in degrees, followed by depth in kilometers. Negative depth is valid.

Use the injected `HttpClient` in an API wrapper, following
[EarthquakeApi](../app/src/main/kotlin/com/johnturkson/sandbox/data/EarthquakeApi.kt).
Pass a resource instance to Ktor's typed `get`, enable `expectSuccess`, and decode
with `body<ResponseModel>()`. API calls remain suspend network operations;
the `Flow` naming rule applies to database reads. Only the catalog list query
currently handles HTTP 204 as no data. Let other HTTP errors, decoding errors,
and cancellation propagate. Do not turn failures into an empty successful list.

Keep persistence coordination in the repository. Follow
[EarthquakeMapping](../app/src/main/kotlin/com/johnturkson/sandbox/data/EarthquakeMapping.kt)
to validate and map the entire collection before writing it. Read the separate
[database instructions](database.md) when adding persistence. Do not introduce
network calls in database reads or UI components.

## Verification and documentation

Extend [EarthquakeApiTest](../app/src/test/kotlin/com/johnturkson/sandbox/EarthquakeApiTest.kt)
with deterministic Ktor `MockEngine` fixtures. Check the HTTP method, path, query
encoding, relevant headers or body, decoding, nulls, unknown fields, empty
responses, status handling, malformed data, and meaningful argument boundaries.
Use the existing fixtures in `app/src/test/resources`; automated tests must not
call the live service. Test any new repository behavior at its owning boundary.

Update this guide when an agreed network pattern is introduced and update the
[earthquake data guide](earthquake-data.md) for public API usage or changed
service limits. Run and report the applicable checks from the testing guide.
