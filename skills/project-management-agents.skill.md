---
name: project-management-agents
description: Active task tracking for OnlyTechTake.
---

# Project management

- `PROJECT_STATUS.json` contains exactly one key, `todos`, whose value is an array of pending task strings.
- Remove a task as soon as its completion is verified. Add concrete unresolved findings as actionable strings; order them by priority.
- Keep evidence and completed-task history out of the tracker. Report verification results in the conversation or an explicitly requested commit.
- Update the relevant document in `docs/` with each completed task and remove obsolete guidance.
- Do not change package identifiers, `versionCode`, or SDK configuration without approval. Read current values from `app/build.gradle.kts`.
- Upload acceptance, device qualification and public privacy-policy availability require direct evidence; local preparation alone does not complete publication.

See [project instructions](../AGENTS.md) and the [release checklist](../docs/play-store/release-checklist.md).
