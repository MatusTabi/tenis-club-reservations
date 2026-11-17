---
name: create-adr
description: Create an ADR for a feature or fix, with or without a BET, or for a significant decision. Use for requests to create an ADR, record an architectural decision, "vytvor ADR", and equivalent intents.
---

# Create an ADR

Record one decision per ADR, keeping it specific and focused on why the approach was chosen. Document the change even when the approach follows an existing convention.

1. Read `docs/orchestration.md`, `.opencode/skills/create-adr/template.md`, the active BET if one exists, the feature or fix design, and applicable project guidance. Respect the human gates. If the context or authorization for this work is unclear, ask the human or orchestrator for clarification.
2. Establish the decision, context, genuine alternatives, reasons and consequences from the supplied input and project evidence. For every change, with or without a BET, create at least one ADR documenting the change and its chosen approach, even if it follows an existing convention or has no significant architectural impact. Describe the actual scope and rationale; do not invent alternatives or architectural significance. If essential facts are unclear, ask rather than inventing requirements or a decision. Record exactly one decision per ADR; separate independent decisions.
3. Inspect existing ADRs in `docs/adr/`, including their status and content, for duplicates, conflicts and applicable decisions. Reference an applicable existing ADR when documenting a new change that follows it. If the proposed decision duplicates or conflicts with one, stop and ask for human clarification; do not silently duplicate, contradict, change, or supersede an existing ADR. A later decision can supersede an existing ADR by referencing it in a new ADR, without modifying the committed record.
4. Find the highest numeric ID among existing `ADR-NNN.md` files in `docs/adr/`, add one, and format it with at least three digits; start at `ADR-001` if none exist. Never fill gaps or reuse IDs. Save as `docs/adr/ADR-NNN.md` using exclusive creation so an existing file cannot be overwritten; if the path is taken, re-inspect the IDs and choose again.
5. Write the ADR using the template. Use the draft creation date in ISO format, and identify any superseded ADR or write `None`. Record genuine alternatives, if any, in Context and consequences in Consequences. New ADRs start as `Proposed`; only explicit human approval may change a status to `Accepted`. Follow the review and immutability rules in `docs/orchestration.md`: never commit a proposed ADR or modify a committed ADR.

   Every generated ADR must have fewer than 60 physical lines, including headings and blank lines. Count the complete file before finishing; if it has 60 or more lines, shorten it without omitting required sections or inventing facts, then count again.
6. Report the ADR path, its proposed status, and any unresolved questions to the orchestrator for the design review and human gate. Do not implement code, invoke Critic, advance a phase, or assume approval; these responsibilities belong to the orchestrator and human.
