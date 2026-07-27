# Implementation Plan: Skill Generation and Project Guidance Update

The goal is to analyze the repository, generate specialized "Skill" files for the project's tech stack (using official Android documentation as a baseline), organize them in a new `skills/` directory, and update the `AGENTS.md` file to integrate these skills into the development workflow.

## Proposed Changes

### [New] Skills Directory
- Create `D:/GITHUB/OnlyTechTake/skills/` to store localized skill files.

### [NEW] Skill Files
We will generate 8 skill files covering the entire tech stack:

1.  **[android-compose-m3](file:///D:/GITHUB/OnlyTechTake/skills/android-compose-m3.skill.md)**: Jetpack Compose Material 3 standards, theming, and Project-specific Preview requirements.
2.  **[persistence-room-sqlcipher](file:///D:/GITHUB/OnlyTechTake/skills/persistence-room-sqlcipher.skill.md)**: Secure database management using Room and SQLCipher.
3.  **[architecture-mvvm-hilt](file:///D:/GITHUB/OnlyTechTake/skills/architecture-mvvm-hilt.skill.md)**: Dependency Injection and layered architecture principles.
4.  **[testing-robot-pattern](file:///D:/GITHUB/OnlyTechTake/skills/testing-robot-pattern.skill.md)**: UI testing standards using the Robot pattern and data seeding.
5.  **[devops-powershell-scripts](file:///D:/GITHUB/OnlyTechTake/skills/devops-powershell-scripts.skill.md)**: Operational guide for using the project's PowerShell automation suite.
6.  **[project-management-agents](file:///D:/GITHUB/OnlyTechTake/skills/project-management-agents.skill.md)**: Guidelines for maintaining `PROJECT_STATUS.json` and versioning rules.
7.  **[media-camerax](file:///D:/GITHUB/OnlyTechTake/skills/media-camerax.skill.md)**: Camera integration using CameraX for photos and videos.
8.  **[background-workmanager](file:///D:/GITHUB/OnlyTechTake/skills/background-workmanager.skill.md)**: Reliable background task execution with WorkManager and Hilt.

### [MODIFY] [AGENTS.md](file:///D:/GITHUB/OnlyTechTake/AGENTS.md)
- Add a new section **"Skill Management"** instructing AI agents to:
    - Load relevant skills from the `skills/` directory using `read_file` before starting a task.
    - Update skills if new official standards are released or project patterns evolve.

### [MODIFY] [PROJECT_STATUS.json](file:///D:/GITHUB/OnlyTechTake/PROJECT_STATUS.json)
- Update `last_updated` and reflect the completion of the "Skill System" integration.

## Verification Plan

### Automated Tests
- No automated tests for documentation, but I will verify the YAML format of each file.

### Manual Verification
- Verify that `AGENTS.md` correctly links to the new directory.
- Verify that all skill files contain the required `YAML Frontmatter` and `Body Content`.
- Check that all paths used in `AGENTS.md` are absolute as per project rules (where applicable) or consistent with the project root.
