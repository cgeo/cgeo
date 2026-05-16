# c:geo Copilot Instructions

This document provides repository-wide instructions for GitHub Copilot agents working on the c:geo project.

## Sources of Truth

When instructions conflict, apply this priority order (highest first):

1. This file (`/.github/copilot-instructions.md`) — agent behavior rules
2. `/checkstyle.xml` and `/.editorconfig` — authoritative code style and formatting rules
3. `/README.md` — general project and contributor context
4. `/.github/workflows/` — reference for what the real CI pipeline does

Only reference files that are tracked in git (i.e. not excluded by `/.gitignore`).

## About c:geo

c:geo is an open-source Android geocaching app written in Java. It's a full-featured client for geocaching.com (unofficial) and offers basic support for other geocaching platforms.

## Project Structure

- `main/` - Main application module
  - `src/main/java/` - Main application source code
  - `src/test/java` - Pure unit tests (JUnit)
  - `src/androidTest/java` - Android instrumented tests
  - `build.gradle` - Module build configuration
- `checkstyle.xml` - Checkstyle configuration (authoritative style rules)
- `ruleset.xml` - PMD ruleset (code quality rules)
- `suppressions.xml` - Checkstyle suppressions
- `.editorconfig` - Authoritative formatting rules (indentation, import layout)

## Code Style and Quality

The **authoritative rules** for code style are defined in `checkstyle.xml` and `.editorconfig`.
The summary below reflects those files. If in doubt, those files take precedence over this prose.

### Import Organization

Imports must be organized into the following groups, separated by blank lines
(matches `ImportOrder` in `checkstyle.xml` and `ij_java_imports_layout` in `.editorconfig`):

1. `cgeo.*` — c:geo internal packages
2. `android.*` — Android framework
3. `androidx.*` — AndroidX libraries
4. `java.*` — Java standard library
5. `javax.*` — Java extensions
6. `*` — All other imports

Static and non-static imports from the same group must not be separated by a blank line.
Within each group, imports must be sorted alphabetically.

### Code Conventions (Must)

- **No unused imports** — remove all unused imports from modified files (`UnusedImports` in `checkstyle.xml`, severity warning)
- **No star imports** — always use explicit imports (`AvoidStarImport` in `checkstyle.xml`)
- **No tabs** — use spaces for indentation (`FileTabCharacter` in `checkstyle.xml`; `indent_style = space` in `.editorconfig`)
- **4-space indent** — indent size is 4 (`indent_size = 4` in `.editorconfig`)
- **File ends with newline** — all files must end with a newline (`NewlineAtEndOfFile` in `checkstyle.xml`)
- **`@Override` annotation** — always use `@Override` where applicable (`MissingOverride` in `checkstyle.xml`, severity warning)
- **No `equals()` without `hashCode()`** — keep them in sync (`EqualsHashCode` in `checkstyle.xml`, severity warning)
- **`final` for local variables** — prefer `final` for local variables whenever possible (`FinalLocalVariable` in `checkstyle.xml`, severity warning)
- **`final` for parameters** — prefer `final` method parameters whenever possible (`FinalParameters` in `checkstyle.xml`, severity warning)

### Quality Rules

Before submitting, ensure all checkstyle rules with severity `warning` or higher in `checkstyle.xml` pass.

## Building and Testing

### Allowed Build Commands

Only issue these Gradle commands when working as an agent.
When running as a Copilot agent (e.g. via github.com or in CI), always use `--offline` — the Gradle cache is pre-populated by the setup workflow (`.github/workflows/copilot-setup-steps.yml`). For local developer builds outside of the agent context, `--offline` is not required.

| Purpose | Command | Required |
|---|---|---|
| Compile & build | `./gradlew --offline assembleBasicDebug` | yes |
| Unit tests | `./gradlew --offline testBasicDebug` | yes |
| Checkstyle | `./gradlew --offline checkstyle` | yes |
| Instrumented tests | — only if explicitly asked — | n/a |
| PMD checks | — only if explicitly asked — | n/a |
| Any other command | — avoid — | n/a |

> **Note:** Instrumented tests and PMD checks require special preconditions (emulator, configured account, etc.) and take a long time. Do **not** run them automatically as part of verifying a code change — only run them when the user explicitly asks for it.

> **Note:** "Compile for all code" means that main, unit test, and instrumented test sources must compile. This is verified by `assembleBasicDebug` combined with `testBasicDebug`. Instrumented tests are not executed as part of this, but their compilation is covered by the build step.

### Testing Guidelines

- **Pure unit tests** go in `main/src/test/java` — prefer these whenever possible
- **Instrumented tests** go in `main/src/androidTest/java` — write them when Android framework is required, but only run them when explicitly asked by the user
- Test classes should be in the same package as the class under test
- Tests that interact with geocaching.com require a configured account on the emulator — only run these when explicitly asked by the user

### Mandatory Quality Gates

After **every** code change, without being asked, always run all three of these and fix any failures before considering the task done:

1. `./gradlew --offline assembleBasicDebug` — no compile errors
2. `./gradlew --offline testBasicDebug` — all unit tests pass
3. `./gradlew --offline checkstyle` — no checkstyle warnings or errors

(Use `--offline` when running as a Copilot agent; omit it for local developer builds.)

## Branching Strategy

- `master` — development of new features; source for nightly builds
- `release` — bug fixes for already-released versions

> A bug fix should always target `release`. A new feature should always target `master`. See naming conventions below for guidance on how to flag a mismatch.

## Dependencies

- Android SDK with Google APIs (API level 26+)
- AndroidX libraries
- The project uses Gradle for build management
- Java 8 language features and APIs are used (core library desugaring enabled for older API levels)

## Special Considerations

- This is an Android application — Android-specific patterns and APIs are commonly used
- The application is written in Java — Java-specific patterns and APIs are used throughout
- The app interacts with multiple geocaching services (geocaching.com, opencaching sites, etc.)
- Location and mapping features are central to the application
- The codebase follows standard Android app architecture patterns

## Operational Coding Considerations

### Way of Working

**Must:**
- Prefer smaller, logically grouped commits; they will be squashed at the end
- After every code change, automatically run all three quality gates (compile, unit tests, checkstyle) and fix any failures — do this without being asked
- Do not run instrumented tests or PMD checks automatically; only run them when the user explicitly asks
- Think things through; do not take shortcuts

**Should:**
- Respond to follow-up questions in the pull request conversation when a PR exists
- Add or adjust pure unit tests when changing logic

### Naming Conventions (GitHub Issue Context)

These conventions apply **only when working on a GitHub issue and when git/GitHub operations are requested**.
If no issue number is available, skip these rules.

- `$ISSUE_NUMBER` refers to the issue number of the GitHub issue being worked on
- **Branch name format:** `copilot/issue-$ISSUE_NUMBER-$NAME`
  - `$NAME` is a concise, meaningful name derived from the issue title
  - Append a number suffix (e.g. `-2`) only if necessary to ensure uniqueness
- **Pull request title format:** `fix #$ISSUE_NUMBER: $TITLE`
  - `$TITLE` is a short, meaningful description of what the PR does
- **Commit message format:** `rel to #$ISSUE_NUMBER: $TITLE`
  - `$TITLE` is a short, meaningful description of what the commit does

### Branch Mismatch Warnings (GitHub PR Context)

These rules apply **only when a pull request is being created or reviewed**
and the issue type or labels are known.

- If the issue has label **"Feature Request"** or type **"Feature"**, and the base branch is **not** `master`:
  → Add a warning comment in the pull request
- If the issue has label **"Bug"** or type **"Bug"**, and the base branch is **not** `release`:
  → Add a warning comment in the pull request

### Typical Workflows

**Bug fix:**
- Base branch: `release`
- Validate with: compile + unit tests + checkstyle (run automatically)
- Do not run instrumented tests unless explicitly asked

**Feature:**
- Base branch: `master`
- Add or adjust pure unit tests where applicable
- Validate with: compile + unit tests + checkstyle (run automatically)
- Do not run instrumented tests unless explicitly asked

**Refactoring:**
- Preserve existing behavior
- Prefer small, focused diffs
- Do not mix unrelated cleanup in the same commit
