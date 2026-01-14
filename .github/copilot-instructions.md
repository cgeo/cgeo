# c:geo Copilot Instructions

This document provides repository-wide instructions for GitHub Copilot agents working on the c:geo project.

## About c:geo

c:geo is an open-source Android geocaching app written in Java. It's a full-featured client for geocaching.com (unofficial) and offers basic support for other geocaching platforms.

## Project Structure

- `main/` - Main application module
  - `src/main/java/` - Main application source code
  - `src/test/java` - Pure unit tests (JUnit)
  - `src/androidTest/java` - Android instrumented tests
  - `build.gradle` - Module build configuration
- `checkstyle.xml` - Checkstyle configuration (code style rules)
- `ruleset.xml` - PMD ruleset (code quality rules)
- `suppressions.xml` - Checkstyle suppressions

## Code Style and Quality

### Import Organization

Imports must be organized into the following groups, separated by empty lines:
1. `cgeo.*` - c:geo internal packages
2. `android.*` - Android framework
3. `androidx.*` - AndroidX libraries
4. `java.*` - Java standard library
5. `javax.*` - Java extensions
6. `*` - All other imports

Static and non-static imports from the same group shall not be separated by empty line.
Within each group, imports should be sorted alphabetically.

### Code Conventions

- **No unused imports** - Remove all unused imports from modified files
- **Use `final` for variables** - Mark variables as `final` whenever possible
- **No star imports** - Always use explicit imports (enforced by checkstyle)
- **No tabs** - Use spaces for indentation (enforced by checkstyle)
- **Final local variables** - Prefer final local variables (checkstyle warning level)
- **Final parameters** - Enforce final parameters (checkstyle warning level)

### Quality Rules

Apply all rules from:
- `checkstyle.xml` - All rules with severity "warning" or higher

Use `./gradlew --offline checkstyle` to check for those.

## Building and Testing

### Build Commands

```bash
# Build the system and check for compile errors
./gradlew --offline assembleBasicDebug

# Run unit tests
./gradlew --offline testBasicDebug

# Run checkstyle checks
./gradlew --offline checkstyle
```
Do not attempt to issue any other build commands. Do not attempt to run pmd checks or instrumented tests.
Always run gradle commands in offline mode.

### Testing Guidelines

- **Pure unit tests** go in `main/src/test/java` - prefer these when possible
- **Instrumented tests** go in `main/src/androidTest/java` - use when Android framework is required
- Test classes should be in the same package as the class under test
- Tests that interact with geocaching.com require a configured account on the emulator

## Branching Strategy

- `master` - Development of new features; source for nightly builds
- `release` - Bug fixes for released versions

## Dependencies

- Android SDK with Google APIs (API level 26+)
- AndroidX libraries
- The project uses Gradle for build management

## Special Considerations

- This is an Android application, so Android-specific patterns and APIs are commonly used
- Application is written in Java, so Java-specific patterns and APIs are commonly used
- The app interacts with multiple geocaching services (geocaching.com, opencaching sites, etc.)
- Location and mapping features are central to the application
- The codebase follows Android app architecture patterns

## Operational Coding Considerations

### Way of working
- Prefer smaller, logically grouped commits while working; we will squash at the end.
- Always enable PR chat and respond to follow-ups in the pull request conversation.
- Don't take shortcuts, think things through
- Always check whether code compiles for all code (main, unit test and instrumented tests)
- Always check whether checkstyle rules are followed for all code (main, unit test and instrumented tests)
- Always run unit tests and check whether all tests pass
- Never run instrumented tests. They will not work in your environment
- Never run PMD checks. Project is not set up properly for that

### Naming conventions

When working in the context of a github issue:
- $ISSUE_NUMBER shall refer to the issue number of that issue
- Every git branch created shall have the naming format: "copilot/issue-$ISSUE_NUMBER-$NAME", where $NAME is a meaningful name extracted from the issue title. You may postfix this name with a number if necessary to create a unique branch name.
- Every github pull request created shall have the naming format "fix #$ISSUE_NUMBER: $TITLE", where $TITLE is a meaningful sort title subscribing what is done in the PR.
- Every git commit created shall have the format "rel to #$ISSUE_NUMBER: $TITLE", where $DESCRIPTION is a meaningful short description of what is done in the commit
- If an issue has label "Feature Request" or is of type "Feature", and it is assigned on a base branch other than "master", then comment a warning to the user in the Pull Request.
- If an issue has label "Bug" or is of type "Bug", and it is assigned on a base branch other than "release", then comment a warning to the user in the Pull Request.


