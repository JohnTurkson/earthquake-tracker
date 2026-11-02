# Source control

Agents must commit as they go. Human contributors are encouraged to follow the
same workflow: commit each coherent change once it is complete and the relevant
checks pass, before moving on to unrelated work.

## Commit scope

Each commit should have one clear purpose and be understandable on its own.
Keep unrelated fixes, cleanup, and formatting in separate commits. Include code,
tests, and documentation together when they are needed to complete the same
change. A small task may need only one commit; a larger task should have several
complete, reviewable steps.

## Before committing

1. Review `git status` and `git diff` to identify your changes and any existing
   work. Preserve changes outside your task.
2. Follow the [testing guide](testing.md) to choose checks and assess their
   results. Use the [build guide](building.md) for environment setup.
3. Stage the intended files or hunks explicitly, then review `git diff --cached`
   to confirm the commit contains only the intended change.
4. Write a concise commit message describing what changed, such as
   `Remove unused database schema export`.
5. Commit the completed change and check `git status` before continuing.

If a required check cannot run, record the limitation when reporting the change.
If a check fails, resolve the failure or establish that it is unrelated before
committing. Keep incomplete work uncommitted until it forms a coherent change.

## Hooks

The repository does not currently install Git hooks. The recommended setup is
fast, read-only checks before committing, with unit tests before pushing and the
full applicable checks in CI. Device tests need a suitable emulator or device
and belong in a dedicated test run.

Format intentionally before staging. A hook should report formatting problems
without rewriting files or automatically staging changes, which could mix
unrelated work into a commit. A hook that runs Gradle against the working tree
must account for unstaged changes: those results may not describe the staged
commit. See the [testing guide](testing.md) for the required checks.

Local hooks are a convenience, not the sole enforcement mechanism: Git allows
some hooks to be bypassed. See the [Git hooks documentation](https://git-scm.com/docs/githooks).
