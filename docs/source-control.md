# Source control

Agents must commit each completed, verified change at a coherent checkpoint and
before ending a turn. This includes documentation, instruction files, deletions,
and symlinks. Human contributors are encouraged to follow the same workflow.

Completed changes must also reach the local default branch before the agent
finishes, unless the user requested branch-only work or integration is blocked.
A commit in an isolated worktree is a checkpoint; report integration separately.
Publishing or pushing changes still requires authorization from the user.

Use the repository's configured default branch or the user's specified
integration target. Confirm the target from repository configuration or current
project context rather than assuming a branch name.

## Before editing

At the start of every turn that changes files, reread the current `AGENTS.md` and
its applicable linked guides. Inspect `git status --short --branch`, the staged
and unstaged diffs, and `git worktree list`. Record which changes already exist
so you can distinguish your work from another contributor's.

Check whether the local default branch contains newer instruction changes than
your checkout. Read those changes before proceeding and reconcile applicable
guidance without overwriting existing work. Worktrees retain their own instruction
files; changes in another worktree do not update them automatically.

Use the assigned checkout. If it has a detached HEAD, create a named `codex/`
branch before editing so its commits remain reachable. Do not switch branches in
another active session's checkout to make your changes.

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
committing. If work is interrupted before a coherent checkpoint, report the
unfinished files, remaining work, and reason they could not be committed. Do not
describe the task as complete. Never discard needed work merely to make the
checkout clean.

## Integrate onto the default branch

After committing and verifying a completed change, inspect the default branch's
checkout again. Integrate the focused commits using a fast-forward when possible
or cherry-pick them onto the latest integration target, resolving conflicts and
running the appropriate checks on the combined result. Review overlapping
instruction edits so one session's rewrite does not remove another session's
rules.

Preserve other contributors' staged, unstaged, and untracked work. If the target
checkout is dirty or another session is changing it, coordinate the handoff or
report the blocker; do not reset, clean, stash, or overwrite its files to force
integration. Do not amend commits another session may already depend on.

Verify the intended changes are present on the default branch and inspect its
final status. When cherry-picking changes a commit's SHA, report the SHA now on
the integration target.

## Before ending a turn

- Commit completed task changes and remove only your own unnecessary experiments.
  Inspect untracked files as well as staged and unstaged changes.
- Verify no completed work from your task remains uncommitted. Preserve unrelated
  work and identify it separately in the report.
- Report the commit SHA, checks performed, whether the changes reached the local
  default branch, and any remaining task changes. If integration is blocked or
  the user requested branch-only work, give the branch and commit plus the exact
  handoff or next step. Do not leave integration implicit for another session.

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
Hooks that run on commit or push cannot detect a turn that ends without either
operation. The end-of-turn status and integration checks above are still required.
