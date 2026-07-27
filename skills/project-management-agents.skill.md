---
name: project-management-agents
description: Rules for project tracking and versioning in OnlyTechTake.
---

# Project Management (Agents)

AI Agents must adhere to strict tracking and versioning rules.

## Core Rules
1. **PROJECT_STATUS.json**: This is the source of truth. Update `last_updated` and `backlog` with every significant change.
2. **Versioning**: `versionCode = 1` and `targetSdk = 37` are locked. Do NOT increment without explicit approval.
3. **Task Completion**: Clear `completed_recently` frequently to keep the file concise.
4. **SDK Consistency**: Use `gradle/libs.versions.toml` for all dependency management.

## References
- [PROJECT_STATUS.json](file:///D:/GITHUB/OnlyTechTake/PROJECT_STATUS.json)
- [AGENTS.md](file:///D:/GITHUB/OnlyTechTake/AGENTS.md)
