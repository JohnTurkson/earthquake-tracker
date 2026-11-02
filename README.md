# USGS Tracker

USGS Tracker is an Android-only app for exploring recent earthquake activity
using the USGS Earthquake API.

## Product goals

- View the same recent earthquakes on a map and in a list.
- Select an earthquake in either view to see more detail.
- Provide useful location context when available and remain usable without
  location access.
- Refresh earthquake data and retain previously viewed information for offline
  use.
- Make data availability and refresh failures clear.
- Keep information easy to understand and navigation straightforward.

## Testing

Use automated tests to cover important behavior and relevant failure cases.
Follow the [testing guide](docs/testing.md) for required checks, passing criteria,
and reporting verification.

## Build and run

Google Maps Compose 9.0.1 is configured for this project. Follow the
[build guide](docs/building.md) for prerequisites, Google Cloud billing and API key
links, local key storage, and build and install commands.

The Gradle wrapper automatically downloads the required JDK when needed; no
separate JDK 21 installation is required.

Google's Secrets Gradle Plugin reads `MAPS_API_KEY` from your machine's
`~/.gradle/gradle.properties`, falling back to the repository's ignored
`local.properties`. That local file must exist, and a key is required to build.
No custom debug signing is required.

For guided setup with Codex, use `$build-project` in this repository. The
[project build skill](.agents/skills/build-project/SKILL.md) follows the same guide.

Use the [Android CLI workflow](docs/android-cli.md) for Google's CLI installation
on macOS, builds, emulator deployment, live UI hierarchy, screenshots and component
images, taps and gestures, and device configuration. The
[Google content review](docs/android-cli-review.md) records applicable agent
capabilities and optional additions from Google's recent announcements.

## Contributing

Follow the [formatting guidelines](docs/formatting.md) and
[architecture and state management guidelines](docs/architecture.md). These apply
to human contributors and agents, with examples from the current implementation.
Discuss additions as new project patterns are introduced and update the guides
with the agreed conventions and working examples.

Follow the [source control guide](docs/source-control.md). Agents must commit as
they go and before ending a turn, then integrate completed changes onto the local
default branch unless branch-only work was requested or integration is blocked.
Human contributors are encouraged to keep each commit focused on one coherent
change.

Shared agent rules live in `AGENTS.md`. See the
[agent instruction guide](docs/agent-instructions.md) for Codex discovery and
sharing the rules with Claude Code.
