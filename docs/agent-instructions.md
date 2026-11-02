# Agent instructions

Keep shared repository rules in [AGENTS.md](../AGENTS.md). It explicitly directs
agents to the [testing guide](testing.md) and
[source control guide](source-control.md). Update those guides rather than
duplicating their contents in each tool's instruction file.

## Codex

Codex discovers `AGENTS.md` automatically. Use `~/.codex/AGENTS.md` for personal
defaults, the repository root for shared rules, and nested `AGENTS.md` files for
directory-specific rules. Discovery follows the path from the repository root
to the starting working directory; deeper guidance takes precedence. In one
directory, `AGENTS.override.md` replaces `AGENTS.md`.

An arbitrary file such as `docs/testing.md` is not automatically loaded as an
instruction file. Keep an explicit instruction in `AGENTS.md` to read and follow
it. Start a new session when verifying automatic discovery after changes.

Discovery builds an instruction chain at the start of a run. An existing session
may therefore have older injected guidance. Before editing on a later turn,
reread the current repository instructions and their applicable linked guides.
For sessions already open when rules change, explicitly direct them to reread
the updated files or start a new session.

Instructions must be committed and integrated onto the local default branch to
become the baseline for new worktrees. Older worktrees must also reconcile newer
instruction changes from that branch; a clean worktree alone does not establish
that its guidance is current. Follow the
[source control guide](source-control.md) for checkpoints, integration, and the
final status report.

See the [official Codex instructions documentation](https://learn.chatgpt.com/docs/agent-configuration/agents-md).

## Claude Code

The root [CLAUDE.md](../CLAUDE.md) contains only `@AGENTS.md`. Claude Code expands
that import, sharing the rules without maintaining a second copy. Keep any
future Claude-specific additions below the import.

Claude Code v2.1.277 and later can also load `AGENTS.md` directly when no project
`CLAUDE.md` or `CLAUDE.local.md` takes precedence and its instruction-loading
support is enabled. The import supports sessions that rely on `CLAUDE.md`.
Check `/context` in Claude Code to confirm which instruction files loaded.

If adding nested rules, account for both tools' discovery behavior. For Claude
sessions using `CLAUDE.md`, add a matching nested `CLAUDE.md` import where needed;
do not assume every nested `AGENTS.md` loads automatically.

See the [Claude Code memory and instructions documentation](https://code.claude.com/docs/en/memory#agentsmd).
