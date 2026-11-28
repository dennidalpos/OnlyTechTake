# Project verification scripts

Run from the repository root. Use explicit variants and noninteractive mode; retain the command output and distinguish static checks, local builds and device tests.

```powershell
./scripts/powershell/project/quality.ps1 -Flavor Full -NonInteractive
./scripts/powershell/project/check_project.ps1 -Flavor Full -SkipReleaseBuild -NonInteractive
```

## Checks

- `quality.ps1` runs Spotless formatting checks and Android Lint. `-Flavor` defaults to Full; `-BuildType` defaults to Debug. `-AllVariants` broadens lint coverage and `-SkipLint` omits lint. `-Apply` rewrites formatting and must be selected deliberately.
- `check_project.ps1` runs lint, Kotlin compilation, unit tests and a debug APK build for Full, Demo or Showcase. Without `-SkipReleaseBuild`, it also checks local signing and assembles a signed release. `-AndroidTests` adds instrumented tests on a connected test device.
- `check_dependency_vulnerabilities.ps1` inspects Gradle lock files using the OSV API (`-OsvApiUrl` overrides its endpoint). It requires networking. An advisory scan is not a complete security qualification.

The wrappers create Gradle/module outputs. Run managed verification in the repository's isolated runtime work area. Local success does not establish release signing enrollment, Google Play acceptance, device compatibility or production readiness.

## Cleanup is a separate operation

`clean_repo.ps1` removes temporary/build outputs; `-Deep` includes caches, `-ResetIdeMetadata` targets IDE metadata and `-StopGradle` stops daemons. `-DryRun` previews the operation. Do not execute cleanup while verifying documentation or remove existing artifacts without an explicit, concrete deletion scope.

See [Android scripts](../android/README.md) for prerequisites and [release preparation](../../../docs/play-store/release-checklist.md) for signed artifacts and upload boundaries.
