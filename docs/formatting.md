# Formatting guidelines

Apply these rules to code changes throughout the repository.

- Do not add comments to code.
- Never format generated files or build/cache directories.
- Prefer explicit imports; do not introduce star imports. ktfmt does not expand
  existing star imports, so follow this rule when adding or optimizing imports.

## Kotlin formatting

Use the standalone [ktfmt CLI](https://github.com/Kotlin/ktfmt) version **0.64**
for Kotlin sources and Gradle Kotlin scripts. Do not add ktfmt Gradle or IDE
plugins. Agents must run the formatter and its check explicitly before committing
Kotlin or build changes; formatting is independent of Gradle's `check` tasks.

Use braces for multiline `if` and `else` branches, with each branch body on
separate lines. A simple `if` or two-branch `if` / `else` may omit braces when the
entire conditional fits on one line within the 100-character limit. An assignment
may wrap before that single-line conditional. If the condition or either branch
wraps, use braces for every branch. Apply these rules to early returns, single
actions, and expressions that produce values, including arguments and test
fixtures. Keep `else if` conditions together, with braces around each body.
Format existing instances by default; flag any proposed readability exception
for review with its location and specific reason.

[`EarthquakeRefreshWorker.doWork`](../app/src/main/kotlin/com/johnturkson/sandbox/work/EarthquakeRefreshWorker.kt)
uses a braced early return:

```kotlin
if (!connectivity.isOnline()) {
    return Result.failure()
}
```

[`EarthquakeViewModel.applySelection`](../app/src/main/kotlin/com/johnturkson/sandbox/ui/map/EarthquakeViewModel.kt)
uses the same layout for a value expression:

```kotlin
requestedMapFocusId =
    if (focus) {
        result.earthquakeId
    } else {
        null
    }
```

[`EarthquakeApp`](../app/src/main/kotlin/com/johnturkson/sandbox/ui/navigation/EarthquakeApp.kt)
keeps a short conditional on one line without braces:

```kotlin
val canExitFromCurrentView =
    if (expanded) !listVisible else compactView == EarthquakeView.Map
```

Pass `--kotlinlang-style --enable-editorconfig` on every invocation.
[EditorConfig](../.editorconfig) defines four-space block and continuation
indentation, a maximum width of 100, and `COMPLETE` trailing comma management.
Its native IDE settings also request explicit imports. The CLI reads the
supported formatting properties through `--enable-editorconfig`; it does not
expand star imports.

Download the standalone JAR once into a cache outside the repository, with a Java
runtime available on `PATH`:

```sh
export KTFMT_JAR="$HOME/.cache/ktfmt/ktfmt-0.64-with-dependencies.jar"
mkdir -p "$(dirname "$KTFMT_JAR")"
curl --fail --location \
  --output "$KTFMT_JAR" \
  https://repo.maven.apache.org/maven2/com/facebook/ktfmt/0.64/ktfmt-0.64-with-dependencies.jar
```

Set `KTFMT_JAR` to that path in each new shell. Check all project Kotlin files
from the repository root with ripgrep and null-delimited file arguments:

```sh
rg --files -0 -g '*.kt' -g '*.kts' |
  xargs -0 java -jar "$KTFMT_JAR" --kotlinlang-style --enable-editorconfig \
    --dry-run --set-exit-if-changed
```

`--dry-run` leaves files unchanged, and `--set-exit-if-changed` makes formatting
differences fail the check. This selection includes app sources, tests, fixtures,
and Gradle Kotlin scripts. Ripgrep respects the repository's ignore rules and
skips hidden directories, keeping build/cache directories out of the inputs.
Preserve those exclusions and never pass generated files to the formatter.

Apply formatting only to the files changed by the task, naming them explicitly.
For example, after editing the root build script:

```sh
java -jar "$KTFMT_JAR" --kotlinlang-style --enable-editorconfig build.gradle.kts
```

Review the diff after formatting and keep unrelated formatting changes out of the
task. The Gradle build does not provide `ktfmtCheck` or `ktfmtFormat` tasks.
