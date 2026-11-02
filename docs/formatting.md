# Formatting guidelines

Apply these rules to code changes throughout the repository.

- Do not add comments to code.
- Never format generated files or build/cache directories.
- Prefer explicit imports; do not introduce star imports. ktfmt does not expand
  existing star imports, so follow this rule when adding or optimizing imports.

## Kotlin formatting

[The root Gradle configuration](../build.gradle.kts) configures ktfmt with
`kotlinLangStyle()`, a maximum width of 100, and `COMPLETE` trailing comma
management. Use these settings for Kotlin sources and Gradle Kotlin scripts.
[EditorConfig](../.editorconfig) supplies editor settings, including four-space
Kotlin indentation and explicit imports.

Use braces for every `if` and `else` branch, with the branch body on separate
lines. Apply this to early returns, single actions, and expressions that produce
values, including arguments and test fixtures. Keep `else if` conditions together,
with braces around each body. Format existing instances by default; flag any
proposed readability exception for review with its location and specific reason.

[`EarthquakeRefreshWorker.doWork`](../app/src/main/kotlin/com/johnturkson/sandbox/work/EarthquakeRefreshWorker.kt)
uses a braced early return:

```kotlin
if (!connectivity.isOnline()) {
    return Result.failure()
}
```

[`EarthquakeViewModel.applySelection`](../app/src/main/kotlin/com/johnturkson/sandbox/ui/EarthquakeViewModel.kt)
uses the same layout for a value expression:

```kotlin
requestedMapFocusId =
    if (focus) {
        result.earthquakeId
    } else {
        null
    }
```

Check formatting from the repository root:

```sh
./gradlew ktfmtCheck
```

The configured `check` tasks depend on this check. To apply formatting to the
configured source files and scripts:

```sh
./gradlew ktfmtFormat
```

These tasks exclude build and cache directories through their configured source
inputs. Preserve those exclusions and keep generated files out of formatting
inputs. Review the diff after formatting and keep unrelated formatting changes
out of the task.
