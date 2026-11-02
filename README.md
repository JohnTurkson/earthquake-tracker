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

## Current explorer screen

Compact windows have separate full-screen Map and List views, selected with a
floating control at the top. Expanded windows show a floating earthquake list
over the map, with no view toggle. The panel highlights the selected earthquake.
Minimize the panel to see the full map and the same bottom selection card used
in compact Map; Show list restores it. Both layouts share data and selection.
Selecting a list row centers its map location; the camera, list scroll position,
and compact view choice survive layout changes.

The reusable
`EarthquakeMap` component accepts immutable item values, selection callbacks,
map properties/UI settings, padding, and UI-owned camera state.

The screen uses a fixed USGS snapshot for UI development, rather than a live
feed. Keep UI components
independent of runtime API and storage access by passing marker presentation
values through the screen's state contract.

## Testing

Use automated tests to cover important behavior and relevant failure cases.
Follow the [testing guide](docs/testing.md) for required checks, passing criteria,
and reporting verification.

All device validation shares **Earthquake Project**, a Resizable API 37 Google
Play emulator. Use [$test-emulator](.agents/skills/test-emulator/SKILL.md) to set
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

Follow the [formatting guidelines](docs/formatting.md) and
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
