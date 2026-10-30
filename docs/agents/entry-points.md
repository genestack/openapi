# Agent entry points

Keep one authoritative rule set in `AGENTS.md` files. Before changing discovery
or adding adapters, check the official loading rules and the target agent setup.

- [Codex discovery](https://learn.chatgpt.com/docs/agent-configuration/agents-md)
  loads instructions from the repository root through its working directory at
  startup. It stops there; guides below that directory need explicit reading.
- [Claude loading](https://code.claude.com/docs/en/memory#agents-md) supports
  `AGENTS.md` natively from Claude Code 2.1.277, subject to the availability
  limitations on that page. With the default setting, project or ancestor `CLAUDE.md`,
  `.claude/CLAUDE.md`, or `CLAUDE.local.md` files select Claude instructions instead.
  A descendant `AGENTS.md` loads when Claude opens a file there with the Read tool
  and that directory has none of those Claude instruction files. Reading files
  with `cat` through Bash does not trigger this native `AGENTS.md` loading.

The [root reading routes](../../AGENTS.md#scope-and-reading-routes) require explicit
guide reads before planning, editing, or review, regardless of automatic loading.
A Claude setup without native support can import `@AGENTS.md` from a sibling
`CLAUDE.md`. Account for descendant loading too: an ancestor adapter affects native
`AGENTS.md` discovery. Keep adapters limited to imports and tool-specific details.
For supported installations with an ancestor Claude instruction file, the
user-level Project instructions setting can load both file types without adding
repository adapters. Confirm loaded files in the target session; a reading-route
walkthrough alone does not demonstrate automatic loading or task behavior.
