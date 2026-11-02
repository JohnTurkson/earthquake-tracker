# Testing and verification

Choose checks based on the behavior and files changed. Agents must follow this
guide before committing and when reporting a change. Human contributors should
use the same checks. Use the [build guide](building.md) for JDK, Android SDK,
device, and Maps configuration.

## Choose the checks

| Change | Required verification |
| --- | --- |
| Documentation or instruction files only | Review the text and local links; run `git diff --check`. No Android build is needed. |
| Kotlin source, resources, manifest, dependencies, or build configuration | Run formatting checks, assemble the debug app, run debug unit tests, and run Android lint. |
| UI, navigation, lifecycle, Android storage, or platform integration | Also run relevant device tests and manually exercise the changed behavior on a compatible device or emulator. |
| Maps configuration or rendering | Also follow the Maps checks in [Verify the run](building.md#verify-the-run). |

Run the standard checks from the repository root:

```sh
./gradlew :ktfmtCheck :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

`ktfmtCheck` already participates in the Gradle `check` tasks. The explicit
command above makes the intended checks clear. Formatting and lint are separate
from tests; compilation alone does not exercise behavior.

Use the configured ktfmt integration to format files you changed before staging.
Never format generated files or build/cache directories. The repository-wide
`ktfmtFormat` task can also rewrite unrelated files; review its scope before
using it and keep unrelated cleanup in a separate commit.

For device tests, connect a compatible device or emulator and run:

```sh
./gradlew :app:connectedDebugAndroidTest
```

Device tests require a configured instrumentation runner. The current app does
not explicitly set `testInstrumentationRunner`; configure an appropriate runner,
such as `androidx.test.runner.AndroidJUnitRunner`, before relying on this suite.
See Android's [instrumented test setup](https://developer.android.com/training/testing/instrumented-tests)
and [command-line testing guide](https://developer.android.com/studio/test/command-line).

## What counts as passing

- The command exits successfully, and the intended tasks and tests were selected
  for the current change. A task with `NO-SOURCE`, zero discovered tests, or only
  skipped tests does not demonstrate that behavior was tested.
- Test results show the expected cases with no failures or errors. Explain
  unexpected skips. Gradle's valid cached or up-to-date results can be reused
  for unchanged inputs; an old report by itself is not evidence for new changes.
- State the scope of filtered runs. A passing test selected with `--tests`
  verifies that selection, not the full unit suite.
- A failed check remains failed until resolved. If it predates the change,
  establish that with evidence and report it separately; do not describe the
  full check set as passing. Report flaky failures even if a retry passes.
- Missing SDKs, dependencies, credentials, devices, or runners mean the affected
  check is blocked or not run. Record the reason and remaining verification;
  committing the code does not make that verification complete.
- A manual check passes only when its expected behavior was observed. App launch,
  activity recreation, and map rendering are separate claims.

For local unit tests, inspect `app/build/reports/tests/testDebugUnitTest/` or
`app/build/test-results/testDebugUnitTest/`. Device reports are under
`app/build/reports/androidTests/connected/`, with XML results under
`app/build/outputs/androidTest-results/connected/`. Report test counts when a
suite ran and the results are available.

## Test meaningful behavior

Add regression coverage for a bug or a new behavior where an automated test
provides useful protection. Assert observable outcomes and include relevant
failure and boundary cases. Use controlled clocks, dispatchers, and network
responses where needed for deterministic tests. Do not weaken assertions or
disable tests to obtain a green result.

There is no required coverage percentage. Documentation and other reversible,
low-impact changes do not need tests that merely repeat the implementation.

The current suite includes template checks: an unconditional assertion, an empty
Flow, a mock HTTP response, empty Compose content, and an activity startup and
recreation check. The serialization test checks a concrete decoding behavior.
Passing these tests does not establish USGS fetching, offline behavior, refresh,
earthquake presentation, or Maps access. Add feature coverage as those behaviors
are implemented.

## Report the results

Report formatting, build, lint, unit tests, device tests, and manual checks
separately, identifying what passed, failed, or was not run. Include the command
or scenario, any relevant device/API level, and material limitations. For a
documentation-only change, reporting link and whitespace checks is sufficient.

After running checks, inspect `git status` for generated output and unrelated
changes before staging. Follow the [source control guide](source-control.md).
