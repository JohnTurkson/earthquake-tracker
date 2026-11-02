---
name: build-project
description: Help a contributor configure, build, and run this Android project with their own Google Maps API key. Use for local build setup and Maps credential troubleshooting.
---

# Build Project

Read the [build guide](../../../docs/building.md) and use it as the setup reference.
Check the repository's Gradle configuration if the documented requirements no
longer match the checkout.

Guide the contributor through enabling Cloud billing and Maps SDK for Android,
creating their own key, and storing `MAPS_API_KEY` in their user-level Gradle
properties file or the repository root's ignored `local.properties`. Preserve any
existing local SDK settings. Google's Secrets Gradle Plugin reads the user-level
Gradle properties file first and uses the local file as fallback, exporting only
`MAPS_API_KEY`. Do not put real keys in the checked-in defaults. The local
development setup leaves application restrictions unset and uses automatic debug
signing; do not require fingerprint registration or a shared keystore.

Never ask the contributor to paste their key into chat, print its value, or put
it in tracked files. If checking configuration, report only whether the key is
present.

Use the Gradle wrapper to assemble the debug APK and run local unit tests. When
asked to run the app, install and launch it on the selected compatible device or
emulator, then follow [Verify the run](../../../docs/building.md#verify-the-run).

Assume the app has a map screen. Report build/test results, app launch, and Maps
rendering separately, stating what was actually observed. A successful build or
launch alone does not verify Maps access.

Launch the app and open its map screen with network access. Verify actual map
tiles and labels, pan or zoom to load a new area, and check Logcat for Maps
authorization failures before reporting the Maps setup as verified. If these
checks cannot be performed, report Maps rendering as unverified. A Google logo or
an app that stays open is not sufficient verification.
A missing or empty key can fail initialization or cause a crash when a map is
created; an invalid or unauthorized key can instead leave a blank map and log an
error. Do not require a crash as evidence of missing credentials or treat its
absence as evidence of a valid key. Redact credentials when reporting errors.
