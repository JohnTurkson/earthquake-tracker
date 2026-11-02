# Building the Android project

## Prerequisites

- Clone the repository and open it in Android Studio.
- Use the included Gradle wrapper, which selects Gradle 9.6.1 and automatically
  downloads the required JetBrains JDK 21 if it is not already available. No
  separate JDK 21 installation is required.
- Install Android SDK Platform 37 and the SDK tools requested during Gradle sync.
  Android Studio normally writes the SDK location to the ignored `local.properties`.
- To run the app, use an Android 8.0 / API 26 or newer device or emulator. Maps
  requires Google Play services; select a Google APIs or Google Play emulator image.

Android Studio includes a Java runtime that can start Gradle. For terminal builds,
make an existing Java runtime available through `JAVA_HOME` or `java` on `PATH`;
Android Studio's bundled runtime can be used. Gradle then selects or downloads the
project's JDK independently of that runtime. The first build needs network access
for downloads; downloaded JDKs are cached in `~/.gradle/jdks` (or
`$GRADLE_USER_HOME/jdks` if customized).

## Get a Google Maps API key

1. Open [Google Cloud Console](https://console.cloud.google.com/) and create or
   select a project. Keep that project selected for the following steps.
2. Open [Billing](https://console.cloud.google.com/projectselector/billing) and
   link a billing account, creating one if necessary. Billing must be enabled even
   when your map usage is free.
3. Open [Maps SDK for Android](https://console.cloud.google.com/apis/library/maps-android-backend.googleapis.com)
   and click **Enable**. Maps Compose uses this SDK; it has no separate API to enable.
4. Open [Keys & Credentials](https://console.cloud.google.com/projectselector/google/maps-apis/credentials),
   choose **Create credentials → API key**, and create your key. If prompted for
   API restrictions, select **Maps SDK for Android**. Leave **Application
   restrictions** set to **None** for this local development setup.

This allows any local debug signing certificate: no fingerprint registration or
custom keystore is required. Application restrictions are optional; Google
[recommends them](https://developers.google.com/maps/api-security-best-practices)
for protecting distributed apps. Anyone who obtains an app-unrestricted key can
use it with its permitted APIs, so keep your development key out of Git.

See Google's [Android key setup](https://developers.google.com/maps/documentation/android-sdk/get-api-key)
and [current pricing](https://developers.google.com/maps/billing-and-pricing/pricing)
for the service requirements and costs.

## Store the key on your machine

Create or edit `~/.gradle/gradle.properties` and add this line, replacing the
placeholder with your own key without quotes:

```properties
MAPS_API_KEY=PASTE_YOUR_KEY_HERE
```

If you customize `GRADLE_USER_HOME`, use its `gradle.properties` instead. Keep any
other existing properties in the file. Do not put the key in the repository's
tracked `gradle.properties`.

Alternatively, add the same `MAPS_API_KEY` line to the repository root's
`local.properties`. This file is already ignored by Git. Preserve its existing
`sdk.dir` and any other settings. The Secrets plugin requires this file to exist
even when the key is in user-level Gradle properties. Android Studio normally
creates it for the SDK location; create an empty file if you supply that location
through `ANDROID_HOME` instead.

Google's [Secrets Gradle Plugin](https://developers.google.com/maps/documentation/android-sdk/secrets-gradle-plugin)
2.0.1 supplies the manifest placeholder and generated BuildConfig field. The
[`secrets` configuration](../app/build.gradle.kts) selects these file sources:

1. User-level `gradle.properties` in the Gradle user home directory.
2. Repository-root `local.properties` for a key missing from the user-level file.

There is no dummy key or checked-in defaults file. Set up your key before
building: a missing key leaves the manifest placeholder unresolved and fails the
build. An empty value can also fail generated code compilation. To use the local
fallback, omit the property from the user-level file rather than leaving it empty.

Only `MAPS_API_KEY` is exported; other user-level Gradle settings and credentials
are ignored by the Secrets plugin. It reads the selected files directly; `-P`
and environment-variable project properties do not override its file contents.
Gradle's [`providers.gradleProperty`](https://docs.gradle.org/current/kotlin-dsl/gradle/org.gradle.api.provider/-provider-factory/gradle-property.html)
resolves normal Gradle property sources, but this plugin does not accept that
provider and Gradle does not include `local.properties` in that lookup. The
explicit paths preserve the requested file precedence without a custom parser.
The Android Gradle Plugin merges the resulting manifest placeholder, and the
Maps SDK reads `com.google.android.geo.API_KEY` from the packaged manifest.

Keep real keys out of project-level `gradle.properties`. Sync or rebuild after
changing a key. Compilation does not verify that Google will authorize it.

The SDK setup uses
[Maps Compose 9.0.1](https://github.com/googlemaps/android-maps-compose/releases/tag/v9.0.1).
The dependency includes the underlying Maps SDK, so no separate Maps dependency
is declared.

## Distributing the app

The built APK or AAB includes its Maps key. People installing the app use that
key automatically; contributors building from source configure their own key.
The Secrets plugin keeps the key out of Git, but does not encrypt it or prevent
extraction from the compiled app.

Before distributing a build, use a separate production key restricted to Maps
SDK for Android and your application ID plus its release signing certificate's
SHA-1 fingerprint. For Google Play App Signing, use the app signing certificate,
not the upload certificate. Keep an unrestricted key for local development only.
See Google's [key restrictions](https://developers.google.com/maps/documentation/android-sdk/get-api-key#restrict_key)
and [security guidance](https://developers.google.com/maps/api-security-best-practices).

Even a map that only displays coordinate markers uses Google's Maps service for
its basemap and requires an API key, an enabled Maps SDK for Android, and billing.
It does not require enabling Places, Routes, or Geocoding when you already have
coordinates. There is no key-free local Google basemap mode; see the
[SDK setup requirements](https://developers.google.com/maps/documentation/android-sdk/get-api-key).

## Build and run

For Google's Android CLI installation and native build, install, and launch
commands, follow the [Android CLI workflow](android-cli.md). It also covers
emulator selection, UI inspection, component captures, and app interaction.
The wrapper commands below remain the direct Gradle alternatives.

From the repository root:

```sh
./gradlew :app:assembleDebug :app:testDebugUnitTest
```

This is a build and local-test smoke check. Follow the
[testing guide](testing.md) for the checks required for a change and what counts
as passing.

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`. To install it
on a connected device or running emulator:

```sh
./gradlew :app:installDebug
```

Launch the installed app from the device or select the `app` run configuration
in Android Studio. Android signs debug builds automatically.

## Verify the run

Report these checks separately:

| Check | What it verifies |
| --- | --- |
| Debug build and unit tests pass | The project compiles and its local tests pass; Maps authorization is not exercised. |
| The app installs and launches | The startup UI runs on that device; map tiles still need to be checked. |
| A map loads fresh tiles | Maps access works for that build, key, device, and network; this does not verify every app feature. |

To verify the Maps setup, launch the app and open its map screen with network
access. Confirm that actual geographic tiles and labels appear. Pan or zoom to a
new area to reduce the chance of checking only cached tiles, and inspect Logcat
for Maps authorization failures. An empty map surface, a Google logo, or the
absence of a crash is not a passing Maps check. If you cannot perform these
checks, report Maps rendering as unverified.

Missing key configuration fails this project's build. An invalid or unauthorized
key can still compile, then produce a blank map and an authorization error at
runtime. Do not rely on a particular crash behavior to validate configuration.
See Google's [map authorization troubleshooting](https://developers.google.com/maps/documentation/android-sdk/start).
Do not share unredacted logs containing keys.

## Troubleshooting

- **Java startup or JDK download errors:** check that `JAVA_HOME` or `java` on
  `PATH` points to a working Java runtime for terminal builds, and that network
  access is available for Gradle's automatic JDK download.
- **Android SDK errors:** verify SDK Platform 37 and the SDK location in
  `local.properties`.
- **Missing key or properties file:** ensure `local.properties` exists and set a
  nonempty `MAPS_API_KEY` in either supported file. There is no dummy fallback;
  missing configuration fails the build. Building alone does not verify Maps
  authorization.
- **Blank map or authorization failure:** check the selected Cloud project's
  billing, enabled Android Maps SDK, key restrictions, and device Google Play
  services. Use Logcat to inspect the Maps authorization error without sharing
  your key.
