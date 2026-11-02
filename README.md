# USGS Tracker

USGS Tracker is an Android-only, in-pocket companion for staying informed about
recent earthquake activity. Convenience and usability are first-class concerns:
users should quickly understand what is happening, where it is happening, and how
it relates to them.

This repository starts from an existing Android template and preserves the full
commit history of its `usgs` branch. The requirements below define the intended
product.

## Data source

Use the USGS Earthquake API as the source of recent earthquake data.

## Map

- Display earthquakes geographically on a map.
- Provide useful context relative to the user's location when available.
- Allow users to select an earthquake to view additional details.
- Keep the experience useful when location access is unavailable or denied.

## List

- Show the same earthquake activity presented on the map.
- Communicate the information most useful to users in each list item.
- Allow users to select an item to view additional earthquake details.

## Refresh and connectivity

- Allow users to refresh earthquake information.
- Preserve access to previously seen information when connectivity is limited or
  unavailable.
- Handle failed requests and unavailable fresh data appropriately, with clear
  feedback about the information users can access.

## Testing

Include automated tests for important behavior and structure the app so that this
behavior can be tested effectively. Choose test types using engineering judgment;
there is no prescribed coverage percentage or mix of test types.

## Design decisions

Presentation, ordering, navigation, information hierarchy, and the details of the
offline and failure experience are open design decisions. Make these choices in
service of convenience, usability, and the product requirements above.

## Build and run

Google Maps Compose 9.0.1 is configured for this project. Follow the
[build guide](docs/building.md) for prerequisites, Google Cloud billing and API key
links, local key storage, and build and install commands.

Store your key as `MAPS_API_KEY` in your machine's `~/.gradle/gradle.properties`.
No custom debug signing is required.

For guided setup with Codex, use `$build-project` in this repository. The
[project build skill](.agents/skills/build-project/SKILL.md) follows the same guide.
