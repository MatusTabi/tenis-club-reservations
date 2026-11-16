---
name: create-bet
description: Create a new BET documenting a desired outcome. Use for requests such as "create a bet", "new bet", "vytvor BET", and equivalent expressions.
---

# Create a BET

Follow `docs/orchestration.md`. Create the BET after Grilling is complete and the human confirms shared understanding; if invoked before then, return control to the orchestrator for Grilling. The human owns the desired outcome; capture what they provide, not an implementation plan.

1. Read `docs/orchestration.md`, relevant project requirements, and `.opencode/skills/create-bet/template.md`. Inspect existing BETs in `docs/bets/`.
2. If an active BET is unfinished, or its completion cannot be established from evidence, obtain explicit human confirmation before replacing it. Do not infer completion from silence or a phase name alone.
3. Identify the next available numeric BET ID from existing BETs, starting at `BET-001`; do not overwrite an existing file. Save the new BET as `docs/bets/BET-NNN.md`, using the template. Capture the desired outcome, problem, value, scope, and measurable acceptance criteria grounded in the human's input and relevant requirements. Record unknowns as open questions and label assumptions; do not invent requirements. Ask for the desired outcome if it is not available.
4. Report the BET path and unresolved questions, then return control to the orchestrator for design. Do not create ADRs, implementation plans, or code.
