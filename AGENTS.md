# OnlyTechTake — agent instructions

User requests take priority. Preserve unrelated work; check Git status first. Never commit/push without a request, discard user data, expose signing secrets, rewrite pushed history or run destructive Git commands.

## Environment

Windows/PowerShell; UTF-8 without BOM; preserve line endings. Use repository-relative script paths and cross-platform APIs. Scratch outputs belong in ignored `logs/` or `tmp/`; remove them before completion. Do not stop others' processes.

Android module: `app/`; Gradle wrapper; Full, Demo and Showcase flavors. Read versions/packages/SDKs from `app/build.gradle.kts`, dependencies from `gradle/libs.versions.toml`. Signing files are local and ignored. Changes to public contracts, package identifiers, versionCode or SDK requirements need approval.

Run builds, tests and checks strictly in sequence. Use targeted tests when a concrete problem emerges; report actual results. PowerShell test wrappers require -Fast (agent) and -Full (manual). Fail with nonzero exit codes; never bypass checks. The build wrapper accepts process `JAVA_HOME` pointing to JDK 21 but does not discover Gradle's cached JDKs.

## Scope and code

Plan multi-step work; ask about material uncertainty. Keep entrypoints thin and preserve Presentation → Application → Domain → Infrastructure boundaries. Use explicit types, concise practical comments and existing dependencies. Remove obsolete code in the edited scope; avoid unrelated refactors and edits to generated files. Update regression tests for business logic changes.

## Tracker and docs

`PROJECT_STATUS.json` contains exactly `{"todos": ["Pending task"]}`. Immediately remove verified completed/obsolete entries. Add concrete unresolved findings as prioritized strings. No completed history, evidence logs or metadata objects in the tracker.

Canonical documentation lives in `docs/`; update it per completed task and prune obsolete references. Local preparation does not establish Google Play acceptance or device qualification. Use current authoritative publication sources.

Guidelines are `skills/*.skill.md`: load project-management for tracker work, devops-powershell for scripts/builds, and architecture, persistence, media, Compose, WorkManager or testing guidelines for the respective scope.

Final response: changes, files, actual checks, cleanliness, remaining risks and necessary next steps.
