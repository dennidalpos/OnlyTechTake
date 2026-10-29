---
name: testing-robot-pattern
description: UI Testing standards using the Robot pattern for OnlyTechTake.
---

# Testing (Robot Pattern)

OnlyTechTake uses the Robot pattern to decouple test logic from UI implementation.

## Guidelines
1. **Robot Pattern**: Every screen should have a corresponding `Robot` class (e.g., `IntakeRobot`).
2. **Instrumentation**: Use `androidx.compose.ui.test` for Compose testing.
3. **State Management**: Call `TestUtils.refreshAppContainer()` after seeding data or resetting the database to ensure the UI reflects the new state.
4. **Seed Data**: Use `DemoDataSeeder.kt` for consistent test datasets.

## Structure
- `IntakeScreenTest.kt` -> Uses `IntakeRobot`.
- `IntakeRobot` -> Contains `checkTitle()`, `fillForm()`, `submit()`.

## References
- [Testing layouts with Jetpack Compose](https://developer.android.com/develop/ui/compose/testing)
- [TestUtils.kt](file:///D:/GITHUB/OnlyTechTake/app/src/androidTest/java/com/example/app/TestUtils.kt) (Example path, adjust as needed)
