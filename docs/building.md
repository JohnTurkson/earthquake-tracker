# Building the project

## Prerequisites

- Clone the repository and open it in Android Studio.
- Use the included Gradle wrapper, which selects Gradle 9.8.2 and automatically
  downloads the required JetBrains JDK 21 if it is not already available. No
  separate JDK 21 installation is required.
- Install Android SDK Platform 37 and the SDK tools requested during Gradle sync.
  Android Studio normally writes the SDK location to the ignored `local.properties`.
- The app supports Android 8.0 / API 26 and newer; Maps requires Google Play
  services. Project device tests, UI design/mockups, screenshots and validation
  use the single shared **Earthquake Project** emulator: **Resizable**, **API 37**,
  **Google Play**. Follow the
  [shared-emulator workflow](testing.md#shared-test-emulator) and setup skill.

The project uses AGP 9.5.0 with compile and target SDK 37. Google's
[API-level compatibility table](https://developer.android.com/build/releases/about-agp#api-level-support)
requires AGP 9.1.1 or newer for API 37; this pairing needs no unsupported-compile-SDK
warning suppression. When using IntelliJ IDEA, its Android plugin must support
the AGP 9.5 release series. IDE compatibility and command-line build compatibility
are separate checks.

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

## Configuration

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
`sdk.dir` and any other settings. A local file is unnecessary for the key when a
Gradle property supplies it; supply the SDK location separately if needed.

The [build script](../app/build.gradle.kts) resolves the key in this order:

1. [`providers.gradleProperty("MAPS_API_KEY")`](https://docs.gradle.org/current/kotlin-dsl/gradle/org.gradle.api.provider/-provider-factory/gradle-property.html),
   which reads normal Gradle property sources, including user-level
   `gradle.properties`. Command-line and environment project properties also work.
2. `gradleLocalProperties(rootDir, providers).getProperty("MAPS_API_KEY")`, which
   reads the repository-root `local.properties` when no Gradle property is set.

The local-file helper belongs to AGP's internal APIs. This project uses it with
AGP 9.1.1; verify its availability and signature when upgrading AGP. Neither
lookup requires constructing a properties-file path in the build script.

There is no dummy key or defaults file. A missing or blank resolved key fails
Gradle configuration with a message explaining where to set `MAPS_API_KEY`.
To use the local fallback, omit the Gradle property rather than leaving it empty.
The resolved value supplies the manifest placeholder directly. The Maps SDK reads
`com.google.android.geo.API_KEY` from the packaged manifest. Other properties
are not exported, and the Secrets Gradle Plugin is not used.

Keep real keys out of project-level `gradle.properties`. Sync or rebuild after
changing a key. Compilation does not verify that Google will authorize it.

The SDK setup uses
[Maps Compose 9.0.1](https://github.com/googlemaps/android-maps-compose/releases/tag/v9.0.1).
The dependency includes the underlying Maps SDK, so no separate Maps dependency
is declared.

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

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`. Reserve the
[shared test emulator](testing.md#shared-test-emulator) before installation:

```sh
android install --device="$ANDROID_SERIAL" app/build/outputs/apk/debug/app-debug.apk
android run --device="$ANDROID_SERIAL" app/build/outputs/apk/debug/app-debug.apk
```

Run those commands only after acquiring the skill's ADB reservation and exporting
the verified serial. Keep the reservation through verification, then return to
idle home and release it. Android signs debug builds automatically.

## Verify the run

Report these checks separately:

| Check                           | What it verifies                                                                                    |
| ------------------------------- | --------------------------------------------------------------------------------------------------- |
| Debug build and unit tests pass | The project compiles and its local tests pass; Maps authorization is not exercised.                 |
| The app installs and launches   | The startup UI runs on that device; map tiles still need to be checked.                             |
| A map loads fresh tiles         | Maps access works for that build, key, device, and network; this does not verify every app feature. |

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
- **Missing or blank key:** set a nonempty `MAPS_API_KEY` in user-level Gradle
  properties or the repository's ignored `local.properties`. Remove an empty
  Gradle property to allow the local fallback. There is no dummy fallback;
  missing configuration fails the build. Building alone does not verify Maps
  authorization.
- **Blank map or authorization failure:** check the selected Cloud project's
  billing, enabled Android Maps SDK, key restrictions, and device Google Play
  services. Use Logcat to inspect the Maps authorization error without sharing
  your key.
- **Launch crash mentioning `org.apache.http.ProtocolVersion`:** Maps SDK 20.0.0
  removed its Apache HTTP legacy library declaration, which can crash the legacy
  map renderer with older Google Play services. The app's
  [manifest](../app/src/main/AndroidManifest.xml) declares
  `<uses-library android:name="org.apache.http.legacy" android:required="false" />`
  inside `<application>` as Google's documented compatibility fix. Keep this
  declaration while supporting affected devices, then rebuild and reinstall.
  Google reports that Play services 25.50.34 and later avoid this specific crash;
  see the [Maps SDK release notes](https://developers.google.com/maps/documentation/android-sdk/release-notes#january_31_2026).
