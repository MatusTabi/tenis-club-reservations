# Agentic development pipeline

**Feature or fix → Scope assessment → Grilling → BET (when warranted) → ADR ↔ Critic → Human Gate 1 → Implementation ↔ Critic → Human Gate 2**

The human provides a high-level feature or fix description, answers questions, resolves escalations, and explicitly approves both gates. The orchestrator coordinates the stages, supplies context to reviewers, preserves artifacts and decisions, and stops when human input or approval is required.

Grilling and Critic review are distinct: the `grilling` skill questions the human to clarify the feature, while the independent Critic reviews the resulting artifacts. Neither replaces the other.

## Stages

1. **Scope assessment:** Decide whether the work warrants a BET. Use one for a substantial outcome with broad or uncertain scope, multiple capabilities, or work likely to span increments. For a small, bounded feature or fix, proceed without one. Explain the choice; if scope cannot be determined, clarify with the human. Reassess after Grilling if the scope changes.
2. **Grilling:** Invoke and follow the `grilling` skill to clarify the feature or fix with the human, but ask only one question at a time. Complete its process and obtain the human's confirmation of shared understanding. If required input is unavailable, stop and ask the human.
3. **BET, when warranted:** Invoke the `create-bet` skill to record the clarified outcome. For small, bounded work without a BET, preserve the confirmed scope and acceptance criteria in the design and create an ADR in the design stage. Automatically begin design after Grilling and any BET creation; do not wait for a separate increment request.
4. **Design and ADRs:** Define the feature's scope, approach, constraints, and acceptance criteria. For every change, whether or not a BET exists, use the `create-adr` skill to draft at least one ADR documenting the change and its chosen approach, even when it follows an existing convention or has no significant architectural impact. Draft additional ADRs for independent decisions as needed. Consider applicable accepted ADRs and reference them where relevant. If the necessary facts are unclear or an existing decision conflicts, ask the human rather than inventing or silently changing a decision. Submit the design and all proposed ADRs to the design review loop.
5. **Human Gate 1:** Once the Critic approves the design and every proposed ADR, present the reviewed versions and findings to the human. Obtain explicit approval for the design, scope, and each ADR needed for implementation. If the human requests substantive changes, revise and repeat the design review before requesting approval again. After approval, change the approved ADRs' statuses to `Accepted`; status-only changes do not require another Critic review. Silence is not approval.
6. **Implementation:** Implement the approved scope and decisions, and collect evidence against the acceptance criteria. Run relevant existing tests at each applicable level, and add or update tests according to the behavior changed and its risks:
   - **Unit:** Test meaningful behavior and edge cases of small units, especially business logic, in isolation. Replace dependencies with test doubles when useful.
   - **Integration:** Test collaborating components and their boundaries, including database queries, mappings, and constraints and the web layer where applicable.
   - **End-to-end:** Test critical application workflows from start to finish through the running application. Make these tests start and stop the application as needed and run as part of `./mvnw clean verify` in CI.

   Organize tests under `src/test/java` by feature, mirroring production packages where practical. Name new test classes with a Maven-discovered suffix: `Test` for unit tests, `IntegrationTest` for integration tests, and `E2ETest` for end-to-end tests. Verify that `./mvnw clean verify` discovers and runs them. Document any test level not run and why in the verification evidence for implementation review and Human Gate 2. If the approved design or scope must change, return to design review and Gate 1. Submit the implementation and evidence to the implementation review loop.
7. **Human Gate 2:** Once the Critic approves, present the implementation, verification evidence, and review findings to the human. Obtain explicit completion approval. If changes are requested, revise and repeat the implementation review; return to design review and Gate 1 first if the approved design or scope must change. After approval, export the current session, including the approval, to a uniquely named file in `docs/sessions/`, then stop and await further human direction. If export fails, report the failure to the human. Silence is not approval.

## Critic review loops

For every invocation, give the independent Critic the artifact, review scope, evaluation criteria, and necessary background, including the BET if one exists, confirmed scope, applicable accepted ADRs, and constraints. Print the Critic's complete output to the human after every session, before acting on its findings. The Critic reports `APPROVED` or `CHANGES_REQUESTED`; it never approves a human gate.

- **Design:** Review the design and each proposed ADR against the BET if one exists, scope, applicable decisions, and acceptance criteria. Apply findings and resubmit every substantively changed ADR and design until the Critic approves and no blocking findings remain. A status-only ADR change is exempt from re-review.
- **Implementation:** Review the change set and verification evidence against the approved design, scope, acceptance criteria, and applicable accepted ADRs. Apply findings and resubmit until the Critic approves and no blocking findings remain.

If a finding cannot be resolved, stop and present the issue, alternatives, and impact to the human. Record the decision and review resulting substantive changes again. An escalation or Critic assessment never substitutes for explicit human approval.

## ADR integrity

- Never commit an ADR with `Proposed` status. An ADR becomes `Accepted` only after explicit human approval at Gate 1.
- Never modify a committed ADR, including its status. Record a later decision or supersession in a new ADR that references the existing record; do not edit the old record.
- Track the reviewed ADR versions and human approvals in the conversation so approval applies to the reviewed content. ADRs remain separate decision records; do not create separate BET increment-tracking files.
