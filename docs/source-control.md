# Source control

Agents must commit each completed, verified change at a coherent checkpoint and
before ending a turn. This includes documentation, instruction files, deletions,
and symlinks. Human contributors are encouraged to follow the same workflow.

Completed changes must also reach the local default branch (`main` in this
repository) before the agent reports the task complete. A commit in an isolated
worktree is only a checkpoint. Committing there, passing checks there, or leaving
that worktree clean does not finish the task.

Integration is part of the assigned work. Perform and verify it in the same task;
do not leave it for the user or another session, or merely offer to integrate
later. A worktree assignment does not imply branch-only work. Only an explicit
user request for branch-only work waives the integration requirement. If
integration is blocked, the task remains incomplete; preserve the commits and
report the blocker and remaining step. Publishing or pushing changes still
requires authorization from the user.

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

Unrelated modified or staged files in the task worktree or on the integration
target must not stop an agent from committing its own verified changes. Commit
only the task's files or hunks and preserve the other work, including its staged
or unstaged state. Do not include unrelated changes just to make a checkout clean.

## Before committing

1. Review `git status` and `git diff` to identify your changes and any existing
   work. Preserve changes outside your task.
2. Follow the [testing guide](testing.md) to choose checks and assess their
   results. For device checks, wait for and reserve the shared Earthquake Project
   emulator, then verify idle-home cleanup before releasing it. Report pending
   device verification or failed cleanup separately; continue independent work
   while the waiter subagent monitors availability. Use the
   [build guide](building.md) for environment setup.
3. Stage the intended files or hunks explicitly and review the exact diff that
   will be committed. Use `git diff --cached` when the index contains only task
   changes. If unrelated changes are already staged, leave them staged and
   isolate the task's commit. For files whose changes all belong to the task,
   `git commit --only -- <task-files>` commits just those paths while preserving
   other staged files. Do not use a whole-file commit for files containing
   unrelated edits; isolate the task's hunks in a separate index or checkout
   instead.
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

Run the required checks from the [testing guide](testing.md) in the task worktree
before integration. Once the task's commits pass those checks, integrate them
before finishing:

1. Inspect the default branch's checkout and current instructions again. Confirm
   the target branch, record its current commit, and preserve any changes made
   since the task began. The target is local `main` in this repository; fetching
   or pushing is separate from local integration.
2. Commit only the task's verified changes, then rebase the task branch onto the
   latest target before updating the target checkout. Review the commits being
   replayed; exclude unrelated commits inherited from another feature branch.
   Rebase only task commits that no other session depends on. If the branch
   contains shared or unrelated commits, prepare an isolated integration branch
   from the latest target and cherry-pick only the task commits there instead.
   Use a clean checkout for the replay; preserve unrelated uncommitted work
   rather than stashing it or enabling autostash.
3. Resolve conflicts in the task or isolated integration checkout. Read both
   changes and preserve their intended behavior; do not select an entire side
   with `ours` or `theirs`, or skip a commit, merely to clear a conflict. Review
   overlapping instruction edits so one session's rewrite does not remove
   another session's rules. Stage only the resolved task paths or hunks and
   continue noninteractively. If the intended behaviors cannot be reconciled
   within the agreed requirements, ask the user for the specific decision.
4. Review the resulting commits and diff against the recorded target commit to
   confirm that they contain only the task's changes. Run the appropriate checks
   from the [testing guide](testing.md) on this combined result before updating
   the default branch. Resolve failures caused by the integration.
5. Recheck the target immediately before integration. If it advanced, replay the
   task commits onto its new tip, review the result, and rerun affected checks.
   Coordinate active operations on the target checkout and index, then use
   `git merge --ff-only --no-autostash <task-branch>` from that checkout. If the
   fast-forward fails because the target advanced, repeat the replay and checks;
   do not force the branch reference or create an unreviewed merge to bypass it.
6. Verify all intended task changes are present on the default branch, record the
   commit SHA there, and inspect its final status. If the original commits were
   retained, `git merge-base --is-ancestor <task-commit> <target-branch>` verifies
   reachability. If cherry-picking changed their SHAs, review the resulting
   commits and files on the target instead.

Preserve other contributors' staged, unstaged, and untracked work. Unrelated
tracked changes on the target are not an integration blocker and do not need to
be committed first. Inspect whether the task overlaps them, then integrate only
the verified task changes while preserving the other files and index entries.
Do not reset, clean, stash, or overwrite someone else's work to force integration.
Do not amend commits another session may already depend on.

If the final fast-forward would include or disturb unrelated staged changes,
prepare the integration against the latest target in an isolated checkout or
index. Apply only the resulting task changes to the target and commit only the
task paths or hunks. Review the resulting commit and confirm that unrelated work
retains its staged or unstaged state. A dirty checkout alone is not a reason to
leave passing task commits in a worktree.

Coordinate overlapping edits and active Git operations before touching the same
files or index. An unresolved conflict, an integration operation in progress, or
a check that fails because of the task needs resolution; unrelated files alone
do not justify stopping. Checks run against a dirty checkout may include other
work; use an isolated checkout of the combined committed result when needed to
verify the integration independently.

If integration cannot proceed safely, report the task as incomplete with the
specific blocker, preserved branch and commit SHAs, and required next step.
Ending a turn because of a blocker does not satisfy the completion requirement.

## Noninteractive Git commands

Use the available shell execution tool for Git operations; no dedicated rebase
API is required. Run without a PTY, close stdin, and use bounded command waits
so an unexpected prompt can be detected and stopped. Git must not wait for an
interactive editor or pager.

Supply commit messages with `git commit -m "Describe the completed change"`.
Use command-scoped editor overrides for rebase, including every continuation,
and disable autostash and updates to other branch references:

```sh
GIT_EDITOR=true GIT_SEQUENCE_EDITOR=true git rebase --no-autostash --no-update-refs main
```

After resolving and explicitly staging conflicts, continue with:

```sh
GIT_EDITOR=true GIT_SEQUENCE_EDITOR=true git rebase --continue
```

These commands keep existing commit messages. Use ordinary rebase rather than
`rebase -i`; use `git --no-pager diff` and `git --no-pager log` for inspection.
For a cherry-pick continuation, use `GIT_EDITOR=true git cherry-pick --continue`.
Keep these settings scoped to the command rather than changing the user's
global Git configuration. Do not bypass hooks or verification to avoid a prompt.
If an unexpected editor or prompt still starts, stop that invocation, inspect
the operation's state, and resume with explicit noninteractive options. Never
delete a Git lock file until its owning process is known to have exited.

## Before ending a turn

- Commit completed task changes and remove only your own unnecessary experiments.
  Inspect untracked files as well as staged and unstaged changes.
- Verify no completed work from your task remains uncommitted. Preserve unrelated
  work and identify it separately in the report.
- Finish integration and verification on the local default branch before
  reporting the task complete, unless the user explicitly requested branch-only
  work. A worktree commit alone must be reported as incomplete.
- Report the commit SHA on the integration target, checks performed, integration
  status, and any remaining task changes. If integration is blocked or the user
  requested branch-only work, give the branch and commit plus the exact handoff
  or next step. Do not leave integration implicit for another session.

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
