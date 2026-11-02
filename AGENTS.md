# Project requirements

Read [README.md](README.md) before planning or changing this project. It defines
USGS Tracker's product goals. Keep the app Android-only and use these goals to
guide design, implementation, and testing.

# Project instructions

At the start of every turn that changes files, reread and follow:

- [Formatting guidelines](docs/formatting.md).
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

Before finishing, compare the change against these guides and report any unmet
requirements, including missing screenshot coverage for UI changes.

# Source control

- Commit each completed, verified change at a coherent checkpoint and before
  ending a turn, including documentation, instruction files, and symlinks.
- Integrate completed commits onto the local default branch before finishing
  unless the user requested branch-only work or integration is blocked. Report
  the commit SHA, verification, integration status, and any remaining task changes
  explicitly.
- Use a named `codex/` branch when the assigned worktree has a detached HEAD.
- Check for newer instruction changes on the local default branch; reconcile
  applicable guidance before editing in an older worktree.
- Stage only changes that belong to the current task. Preserve unrelated work.
- Follow the [source control guide](docs/source-control.md) for commit scope,
  review, and validation.

# Testing

- Read and follow the [testing guide](docs/testing.md) when choosing checks,
  writing tests, and reporting verification for a change.
- Report formatting, build, lint, unit tests, device tests, and manual checks
  separately. State which checks passed, failed, or were not run, and why.

# Android CLI

Before building, deploying, inspecting UI, capturing component images, interacting
with the app, or changing a development device's configuration, read and follow
the [Android CLI workflow](docs/android-cli.md). Prefer Google's `android` CLI
for supported operations, select the device explicitly, inspect live hierarchy
before targeting controls, and verify the resulting UI. Follow the documented
ADB fallbacks when the CLI lacks a needed operation. Keep artifacts out of Git,
restore temporary device settings, and report unverified workflows accurately.

Use the workflow's guidance for current Android documentation, focused skills,
and component previews. Agree on new app, testing, or external-service conventions
before introducing them.
