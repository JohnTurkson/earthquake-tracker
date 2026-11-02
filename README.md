# Earthquake Tracker

Earthquake Tracker is an Android-only app for exploring recent earthquake activity
using the earthquake data API.

## Current explorer screen

Explore earthquakes in Map and List, open event details, and browse grouped map
markers. Compact windows show one view at a time; expanded windows show floating
panes over the map. Selection and view state survive layout changes.

Manual refresh, live monitoring, and opt-in alerts are available. Saved earthquakes
remain available offline, with refresh status and failures shown on screen. See
the [earthquake data guide](docs/earthquake-data.md) for storage and refresh behavior.

## Testing

Use automated tests to cover important behavior and relevant failure cases.
Follow the [testing guide](docs/testing.md) for required checks, passing criteria,
and reporting verification.

All device validation shares **Earthquake Project**, a Resizable API 37 Google
Play emulator. Use [$acquire-test-emulator](.agents/skills/acquire-test-emulator/SKILL.md) to set
it up, wait for exclusive access and idle home, and return it to idle home after
every session.

## Build and run

Google Maps Compose 9.0.1 is configured for this project. Follow the
[build guide](docs/building.md) for prerequisites, Google Cloud billing and API key
links, local key storage, and build and install commands.

The Gradle wrapper automatically downloads the required JDK when needed; no
separate JDK 21 installation is required.

The build reads `MAPS_API_KEY` through Gradle's property API, including your
machine's `~/.gradle/gradle.properties`, then falls back to the repository's
ignored `local.properties`. A nonempty key is required to build. No custom debug
signing is required.

For guided setup with Codex, use `$build-project` in this repository. The
[project build skill](.agents/skills/build-project/SKILL.md) follows the same guide.

Use the [Android CLI workflow](docs/android-cli.md) for Google's CLI installation
on macOS, builds, emulator deployment, live UI hierarchy, screenshots and component
images, taps and gestures, device configuration, and current Google documentation
and skills.

## Contributing

Follow the [formatting guidelines](docs/formatting.md),
[coding conventions](docs/coding-conventions.md), and
[architecture and state management guidelines](docs/architecture.md). These apply
to human contributors and agents, with examples from the current implementation.
Discuss additions as new project patterns are introduced and update the guides
with the agreed conventions and working examples.

Follow the [source control guide](docs/source-control.md). Agents must commit as
they go and before ending a turn, then integrate and verify completed changes on
the local default branch (`main`) before reporting the task complete. A worktree
commit is only a checkpoint. Only an explicit request for branch-only work waives
integration; blocked integration means the task remains incomplete. Human
contributors are encouraged to keep each commit focused on one coherent change.
Unrelated tracked changes must be preserved and must not block committing and
integrating the task's verified files or hunks.

Codex loads [AGENTS.md](AGENTS.md), which directs agents to the project guides
above. [CLAUDE.md](CLAUDE.md) imports the same file for Claude Code. Keep detailed
rules in the relevant guide rather than duplicating them in tool-specific files.
