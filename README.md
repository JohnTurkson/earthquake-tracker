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
in compact Map; Show list restores it. Map, List, and Notifications are separate Navigation 3 destinations. Map and List
have independent ViewModels that observe the same data and exchange selection
results. Selecting a map marker reveals its row when the list is visible or next
opened.
Selecting a list row centers its map location; the camera, list scroll position,
and compact view choice survive layout changes.

The reusable
`EarthquakeMap` component accepts immutable item values, selection callbacks,
map properties/UI settings, padding, and UI-owned camera state.
Nearby earthquakes group through Maps Utils as the camera zooms out. Cluster
badges show exact event counts and the maximum reported magnitude when available.
Tap a cluster to browse all its earthquakes in a scrollable bottom sheet, then
select an event to highlight it in blue on the map. Events at identical
coordinates remain accessible through the cluster list.

The screen observes a Room database populated from the USGS all-day GeoJSON feed.
Manual refresh and live-monitoring checks share one unique one-time WorkManager
refresh request and preserve stored data on failure. Refresh attempts run without a
network constraint and report a generic failure when they cannot complete,
retaining saved data. Live monitoring uses a
foreground WorkManager worker that checks every minute while active, pauses
offline, and resumes on reconnection. Android can stop or delay work; the screen
observes the actual worker state and refresh result. Stopping a session cancels
future checks while letting an already queued or running refresh finish.

The separate bell opens the Notifications destination full-screen in compact
windows and as a floating right pane in expanded windows. Material Adaptive
scenes arrange the destinations as the window resizes. Expanded windows use a
Scaffold top app bar for monitoring, Show list/Minimize, refresh, and notifications.
The monitoring message stays on one line at the standard 64 dp app bar height.
Compact windows retain the floating map banner and first scrolling list item at
the same 64 dp height. Large-earthquake
alerts use a separate channel from quiet monitoring status. Alerts default to
M6+ events within the past minute and are opt-in. Previously delivered event IDs
are retained to avoid repeated alerts.

Keep UI components independent of runtime API and storage access by passing
presentation values through the screen's state contract.

The [earthquake data layer](docs/earthquake-data.md) uses Room as its source of
truth. The read-only repository exposes stored records and refresh metadata;
all updates go through the shared one-time refresh worker. It retains events,
merges newer source updates by ID and known aliases, and supports offline reads
after reopening the app. Product metadata and separate detail-freshness models
are not stored. The explorer observes unrestricted history queries.

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
