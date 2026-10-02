# Dependency upgrades (deferred)

`./gradlew lint` reports 8 "newer version available" warnings. They are not errors, the app builds and
all tests pass, and the upgrades were deliberately left for later. Versions live in
`gradle/libs.versions.toml` (Gradle itself in `gradle/wrapper/gradle-wrapper.properties`).

Versions below are what lint reported on 2026-10-02; re-run `./gradlew lint` for current numbers.

| Dependency | Current | Lint reported | Risk |
|---|---|---|---|
| `androidx.core:core-ktx` | 1.10.1 | 1.19.1 | Low |
| `androidx.lifecycle:lifecycle-runtime-ktx` | 2.6.1 | 2.11.0 | Low |
| `androidx.activity:activity-compose` | 1.8.0 | 1.13.0 | Low |
| Compose BOM | 2026.02.01 | 2026.09.00 | Low to medium |
| Gradle wrapper | 9.6.0 | 9.8.0 | Medium (also check `gradle/gradle-daemon-jvm.properties`) |
| `kotlinx-coroutines-test` | 1.10.2 | 1.11.0 | Pinned, see below |
| Kotlin / `kotlin.compose` plugin | 2.2.10 | 2.4.20 | High, see below |
| mikepenz `multiplatform-markdown-renderer-m3` | 0.38.1 | 0.45.0 | Pinned, see below |

## Why some are pinned

- **Markdown renderer 0.38.1:** versions 0.42 and later pull in the Kotlin 2.4 stdlib, which Kotlin 2.2.10
  cannot read. It can only move together with Kotlin.
- **Kotlin 2.2.10:** moving to 2.4.x must stay compatible with AGP 9.4.1 (built-in Kotlin), KSP and Room. This
  is the same toolchain risk flagged for slice 0 of the plan.
- **`kotlinx-coroutines-test` 1.10.2:** pinned during slice 0. The reason was not recorded; check whether
  1.11.0 works with the Robolectric tests before changing it.

## Suggested order

1. **Low-risk group:** `core-ktx`, `lifecycle-runtime-ktx`, `activity-compose`, Compose BOM. Bump together.
2. **Gradle wrapper**, on its own.
3. **Kotlin 2.4.x with the markdown renderer**, together, after checking AGP, KSP and Room compatibility.
4. **`kotlinx-coroutines-test`**, with or after step 3.

## Procedure for each step

Project rule (CLAUDE.md): verify the app builds before changing any source code.

1. Change only that group in `gradle/libs.versions.toml`.
2. Run `./gradlew assembleDebug testDebugUnitTest`. Stop and report if anything fails.
3. Run `./gradlew lint`.
4. Commit one step at a time, e.g. `chore(deps): update androidx core, lifecycle, activity and Compose BOM`.
5. Do a quick manual check of the Compose screens (BOM bumps can change default component styling).
