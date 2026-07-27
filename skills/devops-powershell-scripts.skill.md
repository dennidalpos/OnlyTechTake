---
name: devops-powershell-scripts
description: Operational guide for OnlyTechTake PowerShell automation scripts.
---

# DevOps (PowerShell Scripts)

Automation is handled via PowerShell wrappers located in `scripts/powershell/`.

## Key Scripts
- `project/check_project.ps1`: Full project health check. Use with `-Flavor Full -SkipReleaseBuild -NonInteractive`.
- `project/quality.ps1`: Runs Spotless and Lint. Use with `-Apply`.
- `assets/regenerate_public.ps1`: Master script for asset generation (screenshots, videos).
- `assets/create_demo_assets.ps1`: Generates screenshots from JSON profiles.

## Execution Rules
- Always run from the project root.
- Use `scripts/powershell/` as the base directory.
- Verify environment (Windows 11 + PowerShell) before execution.

## References
- [AGENTS.md](file:///D:/GITHUB/OnlyTechTake/AGENTS.md)
- [scripts/](file:///D:/GITHUB/OnlyTechTake/scripts/)
