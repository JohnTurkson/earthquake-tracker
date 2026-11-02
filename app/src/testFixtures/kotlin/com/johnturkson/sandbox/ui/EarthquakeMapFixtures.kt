package com.johnturkson.sandbox.ui

import kotlin.time.Instant

internal object EarthquakeMapFixtures {
    val items =
        listOf(
            EarthquakeMapItem(
                id = "us7000tuku",
                latitude = 33.822,
                longitude = 136.9854,
                title = "M 6.0 - 58 km SSE of Shima, Japan",
                magnitude = 6.0,
                eventTime = Instant.parse("2026-10-31T22:15:02.901000+00:00"),
            ),
            EarthquakeMapItem(
                id = "us7000tuky",
                latitude = -13.8729,
                longitude = 165.6456,
                title = "M 5.2 - 200 km NW of Port-Olry, Vanuatu",
                magnitude = 5.2,
                eventTime = Instant.parse("2026-10-31T22:42:39.540000+00:00"),
            ),
            EarthquakeMapItem(
                id = "us7000tujf",
                latitude = 7.5692,
                longitude = -80.2547,
                title = "M 5.2 - 4 km S of Bajo Corral, Panama",
                magnitude = 5.2,
                eventTime = Instant.parse("2026-10-31T15:50:18.909000+00:00"),
            ),
            EarthquakeMapItem(
                id = "us7000tukm",
                latitude = 28.114,
                longitude = 51.5076,
                title = "M 5.0 - 115 km SE of Bushehr, Iran",
                magnitude = 5.0,
                eventTime = Instant.parse("2026-10-31T21:20:39.173000+00:00"),
            ),
            EarthquakeMapItem(
                id = "us7000tuly",
                latitude = -21.698,
                longitude = -68.435,
                title = "M 4.5 - 55 km SSW of Ollagüe, Chile",
                magnitude = 4.5,
                eventTime = Instant.parse("2026-11-01T01:24:52.448000+00:00"),
            ),
            EarthquakeMapItem(
                id = "us7000tuk4",
                latitude = 51.6994,
                longitude = -172.7931,
                title = "M 4.5 - 111 km ESE of Atka, Alaska",
                magnitude = 4.5,
                eventTime = Instant.parse("2026-10-31T18:51:02.721000+00:00"),
            ),
            EarthquakeMapItem(
                id = "us7000tuj1",
                latitude = 19.1534,
                longitude = 120.9249,
                title = "M 4.4 - 62 km NNW of Claveria, Philippines",
                magnitude = 4.4,
                eventTime = Instant.parse("2026-10-31T13:31:14.759000+00:00"),
            ),
            EarthquakeMapItem(
                id = "us7000tui9",
                latitude = -19.2212,
                longitude = -177.4956,
                title = "M 4.4 - Fiji region",
                magnitude = 4.4,
                eventTime = Instant.parse("2026-10-31T10:42:10.031000+00:00"),
            ),
            EarthquakeMapItem(
                id = "us7000tuh8",
                latitude = 32.5197,
                longitude = 94.0675,
                title = "M 4.4 - 222 km ENE of Nagqu, China",
                magnitude = 4.4,
                eventTime = Instant.parse("2026-10-31T05:52:03.468000+00:00"),
            ),
            EarthquakeMapItem(
                id = "us7000tuh7",
                latitude = 52.6834,
                longitude = 158.7423,
                title = "M 4.4 - 35 km SE of Vilyuchinsk, Russia",
                magnitude = 4.4,
                eventTime = Instant.parse("2026-10-31T05:29:19.961000+00:00"),
            ),
            EarthquakeMapItem(
                id = "us7000tuiw",
                latitude = -6.4305,
                longitude = 128.9289,
                title = "M 4.3 - Banda Sea",
                magnitude = 4.3,
                eventTime = Instant.parse("2026-10-31T13:10:08.987000+00:00"),
            ),
            EarthquakeMapItem(
                id = "uu80162166",
                latitude = 38.2876666666667,
                longitude = -112.2915,
                title = "M 3.3 - 8 km NW of Junction, Utah",
                magnitude = 3.3,
                eventTime = Instant.parse("2026-10-31T04:44:15.520000+00:00"),
            ),
            EarthquakeMapItem(
                id = "aka2026vplgct",
                latitude = 60.898,
                longitude = -149.835,
                title = "M 3.1 - 10 km WSW of Hope, Alaska",
                magnitude = 3.1,
                eventTime = Instant.parse("2026-10-31T23:17:43.520000+00:00"),
            ),
            EarthquakeMapItem(
                id = "nc75460382",
                latitude = 37.4236679077148,
                longitude = -121.457664489746,
                title = "M 2.9 - 27 km WSW of Westley, CA",
                magnitude = 2.9,
                eventTime = Instant.parse("2026-10-31T16:44:50.250000+00:00"),
            ),
            EarthquakeMapItem(
                id = "aka2026vovsfh",
                latitude = 60.232,
                longitude = -152.198,
                title = "M 2.6 - 35 km NW of Ninilchik, Alaska",
                magnitude = 2.6,
                eventTime = Instant.parse("2026-10-31T15:32:36.070000+00:00"),
            ),
            EarthquakeMapItem(
                id = "hv75066971",
                latitude = 19.3933334350586,
                longitude = -155.274673461914,
                title = "M 2.4 - 6 km SW of Volcano, Hawaii",
                magnitude = 2.4,
                eventTime = Instant.parse("2026-10-31T23:30:27.980000+00:00"),
            ),
        )

    val denseItems =
        List(120) { index ->
            val magnitude = (10 + (index * 7) % 39) / 10.0
            val group = index / 30
            val latitude = 37.7749 + (group / 2) * 0.025
            val longitude = -122.4194 + (group % 2) * 0.035
            EarthquakeMapItem(
                id = "synthetic-$index",
                latitude = latitude + if (index < 8) 0.0 else ((index * 17) % 23 - 11) * 0.0003,
                longitude = longitude + if (index < 8) 0.0 else ((index * 13) % 19 - 9) * 0.0003,
                title = "M $magnitude · San Francisco sample ${index + 1}",
                snippet = "Synthetic event · ${index + 1} min ago",
                magnitude = magnitude,
            )
        }
}
