# c:geo Copilot Instructions

This document provides repository-wide instructions for GitHub Copilot agents working on the c:geo project.

## About c:geo

c:geo is an open-source Android geocaching app written in Java. It's a full-featured client for geocaching.com (unofficial) and offers basic support for other geocaching platforms.

## Project Structure

- `main/` - Main application module
  - `src/main/java/` - Main application source code
  - `src/test/` - Pure unit tests (JUnit)
  - `src/androidTest/` - Android instrumented tests
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

Within each group, imports should be sorted alphabetically.

### Code Conventions

- **No unused imports** - Remove all unused imports from modified files
- **Use `final` for variables** - Mark variables as `final` whenever possible
- **No star imports** - Always use explicit imports (enforced by checkstyle)
- **No tabs** - Use spaces for indentation (enforced by checkstyle)
- **Final local variables** - Prefer final local variables (checkstyle warning level)
- **Final parameters** - Prefer final parameters (checkstyle warning level)

### Quality Rules

Apply all rules from:
- `checkstyle.xml` - All rules with severity "warning" or higher
- `ruleset.xml` - PMD rules for code quality

## Building and Testing

### Build Commands

```bash
# Build the debug variant
./gradlew assembleBasicDebug

# Run unit tests
./gradlew test

# Run instrumented tests (requires Android emulator/device)
./gradlew connectedAndroidTest
```

### API Keys Configuration

The app requires API keys for full functionality:
1. Copy `./templates/private.properties` to `./`
2. Edit `private.properties` with your API keys (optional for development)
3. The build system generates `keys.xml` automatically

### Testing Guidelines

- **Pure unit tests** go in `main/src/test/` - prefer these when possible
- **Instrumented tests** go in `main/src/androidTest/` - use when Android framework is required
- Test classes should be in the same package as the class under test
- Tests that interact with geocaching.com require a configured account on the emulator

## Branching Strategy

- `master` - Development of new features; source for nightly builds
- `release` - Bug fixes for released versions

## Dependencies

- Android SDK with Google APIs V26+
- AndroidX libraries
- The project uses Gradle for build management

## Special Considerations

- This is an Android application, so Android-specific patterns and APIs are commonly used
- The app interacts with multiple geocaching services (geocaching.com, opencaching sites, etc.)
- Location and mapping features are central to the application
- The codebase follows Android app architecture patterns
