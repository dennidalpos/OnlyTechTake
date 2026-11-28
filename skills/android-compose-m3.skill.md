---
name: android-compose-m3
description: Best practices for Jetpack Compose Material 3 in OnlyTechTake. Covers theming, Preview requirements, and components.
---

# Jetpack Compose Material 3 (M3)

Guidelines for UI development in OnlyTechTake.

## Core Principles
1. **Material 3 Only**: Use `androidx.compose.material3` components. Avoid M2 or generic `material` libraries.
2. **Theming**: Use `OnlyTechTakeTheme` (or project equivalent). Do NOT hardcode colors or typography. Use `MaterialTheme.colorScheme` and `MaterialTheme.typography`.
3. **Strings**: NEVER hardcode strings. Always use `stringResource(id = R.string.key)`.
4. **Previews**: Every screen and significant component MUST have:
    - Light Mode Preview.
    - Dark Mode Preview.
    - Preview with representative data.

## Implementation Details
- **Version**: Refer to `libs.versions.toml` for `composeBom` and `material` versions.
- **Components**: Use `Scaffold`, `TopAppBar`, and `NavigationSuiteScaffold` (if applicable) for standard layout.
- **Adaptive**: Support multiple window sizes where possible using `WindowSizeClass`.

## References
- [Official M3 Documentation](https://developer.android.com/develop/ui/compose/designsystems/material3)
- [Compose API Guidelines](https://developer.android.com/develop/ui/compose/api-guidelines)
