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
2. Run checks appropriate to the change. For application code, use the relevant
   build and tests described in the [build guide](building.md). For documentation
   changes, check links and run `git diff --check`.
3. Stage the intended files or hunks explicitly, then review `git diff --cached`
   to confirm the commit contains only the intended change.
4. Write a concise commit message describing what changed, such as
   `Remove unused database schema export`.
5. Commit the completed change and check `git status` before continuing.

If a required check cannot run, record the limitation when reporting the change.
If a check fails, resolve the failure or establish that it is unrelated before
committing. Keep incomplete work uncommitted until it forms a coherent change.
