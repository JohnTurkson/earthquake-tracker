package com.johnturkson.sandbox.data

class EarthquakeAlertDelivery(
    private val wasDelivered: suspend (String) -> Boolean,
    private val publish: (EarthquakeDatabaseModel) -> Boolean,
    private val recordDelivery: suspend (EarthquakeAlertDatabaseModel) -> Unit,
) {
    private val policy = EarthquakeAlertPolicy()

    suspend fun deliver(
        items: List<EarthquakeDatabaseModel>,
        settings: EarthquakeSettings,
        nowMillis: Long,
    ) {
        if (!settings.alertsEnabled) return
        items
            .filter { policy.qualifies(it, settings.minimumMagnitude, nowMillis) }
            .forEach { item ->
                if (!wasDelivered(item.id) && publish(item))
                    recordDelivery(EarthquakeAlertDatabaseModel(item.id, nowMillis))
            }
    }
}
