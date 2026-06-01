# TapLog — MD Update Prime Directives
> Read this before touching any document at end of session.

---

## The documents

| File | What it is | How to update |
|---|---|---|
| `taplog-curriculum.md` | Full build log — every module, every lesson | Full replacement. Never append-only. |
| `taplog-spec.md` | Living technical spec — stack, data model, file map, roadmap | Full replacement. |
| `taplog-investor-summary.md` | Pitch document — traction section evolves each session | Full replacement. |
| `taplog-lightbulbs.md` | Product insight moments captured during the build | Full replacement. Add new entries at the bottom. |
| `taplog-pilot-tester-pool.md` | Real people — handle with care | Full replacement only if something changed. |
| `taplog-session-handoff.md` | Generated at end of each session for the next session | Always regenerate fresh. Not a living document. |
| `taplog-md-prime-directives.md` | This file — workflow rules for AI sessions | Full replacement when workflow evolves. |
| `.claude/AGENTS.md` | Auto-read by Claude Code at session start — copy of prime directives | Keep in sync with taplog-md-prime-directives.md. Run: `copy taplog-md-prime-directives.md .claude\AGENTS.md` |

---

## Prime directives

1. **Always deliver full file replacements.** Never deliver append-only fragments, diffs, or partial updates. The person replaces the file via select-all-paste in their editor. Fragments create friction and risk.

2. **The curriculum is the most important document.** It is the public-facing record of the build. Treat it with the same care as production code. Every module gets a complete entry with What we did + Key lessons.

3. **Never split a document update across multiple files.** If the curriculum needs updating, deliver one complete `taplog-curriculum.md`. Not a `module-N-append.md` alongside it.

4. **Read the current document before writing the replacement.** All docs are uploaded at session start. Never reconstruct from memory.

5. **The session handoff is a separate deliverable.** It is not a substitute for updating the living documents. Both must be produced at end of session.

6. **Lightbulb moments are first-class.** If an insight worth capturing comes up mid-session, note it. Write it into `taplog-lightbulbs.md` at end of session with full context: trigger → insight → action → broader principle.

7. **Update the curriculum glossary and key files table** whenever a new term or file is introduced. These are reference sections, not afterthoughts.

8. **The session footer line in the curriculum** (`*Session N complete — ...`) must be updated to reflect the current session and set the next module correctly.

---

## End-of-session workflow

Run this at the end of every session, in order:

1. **Update all md docs** — full replacements for every doc that changed this session
2. **Generate session handoff** — fresh `taplog-session-handoff.md` summarizing what was done, current state, and what's next
3. **Sync AGENTS.md** — run `copy taplog-md-prime-directives.md .claude\AGENTS.md` in the terminal at `C:\dev\taplog` to keep Claude Code in sync
4. **Confirm delivery** — all files presented as downloads, no fragments, no appends

---

## File replacement workflow (Android Studio)

When replacing file contents in Android Studio:
- **Always use select-all (Ctrl+A) → paste** inside the editor
- **Never copy files via Windows Explorer** — triggers refactor dialogs that append instead of replace, causing redeclaration errors
- After pasting, save with Ctrl+S

---

## OpenSpec workflow (per module)

OpenSpec is installed at `C:\dev\taplog`. Use it for every module going forward.

**Starting a new module:**
```
/opsx:propose "module-N-description"
```
AI reads existing specs, creates proposal.md + design.md + tasks.md. Review and approve before any code is written.

**Implementing:**
```
/opsx:apply
```
AI implements against the approved plan task by task.

**Archiving after module complete:**
```
/opsx:archive
```
Changes archived to `openspec/changes/archive/`. Specs updated.

**Key principle:** No code gets written until the proposal is reviewed and approved. This is the enforcement mechanism that replaces "how do I know you read the directives."

**Updating OpenSpec:**
```
npm install -g @fission-ai/openspec@latest
openspec update
```
