# Project requirements

Read [README.md](README.md) before planning or changing this project. It defines
USGS Tracker's product goals. Keep the app Android-only and use these goals to
guide design, implementation, and testing.

# Project instructions

At the start of every turn that changes files, reread and follow:

- [Formatting guidelines](docs/formatting.md).
- [Coding conventions](docs/coding-conventions.md).
- [Architecture and state management guidelines](docs/architecture.md).
- [Source control guide](docs/source-control.md).
- [Testing guide](docs/testing.md).

These guidelines apply to agents and human contributors. Follow the linked
implementation examples and preserve their state ownership and responsibility
boundaries. Existing scaffolding is identified in the guidelines; do not treat it
as a completed feature.

When new work introduces an element or pattern the guidelines do not cover, ask
the user how they want to expand the relevant conventions before choosing that
pattern. Continue independent work that follows established conventions. Do not
ask again about decisions already made in the conversation.

Update the relevant guide with agreed decisions and concrete implementation
examples as part of the change. Add sections only for elements the project
actually introduces. Expand testing guidance with the user as testing patterns
are established; do not add placeholder sections for future work.

Keep repository documentation focused on reusable project instructions and
concrete source examples. Report task-specific activity, research, and test or
verification results in the chat; do not add them as repository files or append
them to project guides. Follow the [testing guide's reporting instructions](docs/testing.md#report-the-results)
for results and generated artifacts.

# Android CLI

Before building, deploying, inspecting UI, capturing component images, interacting
with the app, or changing a development device's configuration, read and follow
the [Android CLI workflow](docs/android-cli.md).
