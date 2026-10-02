# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with
code in this repository.

## Project

Android app (Kotlin, Jetpack Compose, Material3) — currently the unmodified
Android Studio "Empty Activity" template: a single `MainActivity` showing a
`Greeting` composable, plus theme files in `ui/theme/`. Single Gradle module
`:app`, package `dev.fitiavana.learning_mgmt`.

## Commands

Use the Gradle wrapper (`local.properties` holds the local Android SDK path and
is untracked):

- Build debug APK: `./gradlew assembleDebug`
- Unit tests (JVM, `app/src/test`): `./gradlew testDebugUnitTest`
- Build and test in one command: `./gradlew assembleDebug testDebugUnitTest`
- Single unit test: `./gradlew testDebugUnitTest --tests
  "dev.fitiavana.learning_mgmt.ExampleUnitTest"`
- Lint: `./gradlew lint`

## Build configuration notes

- Dependencies and plugin versions are managed in the version catalog
  `gradle/libs.versions.toml`; add new ones there and reference via `libs.*` in
  `app/build.gradle.kts`.
- Very new toolchain: AGP 9.4.1, Kotlin 2.2.10, compileSdk/targetSdk 37, minSdk
  26, Compose BOM 2026.02.01. Kotlin Compose compiler comes from the
  `kotlin.compose` plugin (no separate compiler version). AGP 9 has built-in
  Kotlin, so there is no `kotlin.android` plugin applied.
- Release build has optimization (R8) disabled; Java 11 source/target.
- `gradle/gradle-daemon-jvm.properties` pins the Gradle daemon JVM.

### When adding new libraries

When adding a new library (in build.gradle.kts / libs.versions.toml),
always verify the app can be built successfully before making changes to
source code.

## Code

- DRY: Reuse code when possible, refactor if needed

## Testing

- **TDD is mandatory**: for every change (new feature, bug fix, refactor),
  write a failing test first, verify it actually fails by running the test
  command, then write the minimum code to make it pass, then refactor. Never
  write production code before there is a test that requires it.

## UI

- Minimalist UI design. Simple colors
- Focus on functionality, more than fancy visuals
- Respect basic UI/UX principles, such as colors on primary vs. non-primary
  actions, colors on destructive actions

## Commits

- Never include Co-Authored-By in commits

### Commit messages

Use [Conventional Commits](https://www.conventionalcommits.org/):

```
<type>(<scope>): <description>
```

Common types: `feat`, `fix`, `docs`, `style`, `refactor`, `test`, `chore`,
`perf`. Use `!` after type/scope (e.g. `feat!:`) for breaking changes.

## On new features

After every change, generate a testing checklist for the user to manually 
verify the changes work.
