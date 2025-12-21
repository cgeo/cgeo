# c:geo Development Instructions

## Overview
c:geo is an Android geocaching app (unofficial geocaching.com client). ~135MB, 1,150+ Java files, multi-module Gradle project.
**Stack**: Java 8, Android SDK (min 26, target 35, compile 36), Gradle 8.11.1, RxJava, Mapsforge/VTM, OkHttp, Jackson

## Setup (REQUIRED for Build)

**MUST configure API keys before building:**
```bash
cp templates/private.properties ./private.properties
# Edit or leave empty for basic compilation
```
Build auto-generates `main/src/main/res/values/keys.xml` from `private.properties`. If changing keys: `rm main/src/main/res/values/keys.xml` first. Without this, build fails.

## Build Commands (Run from repo root)

**Prerequisites**: JDK 17, Android SDK API 26+. Gradle wrapper auto-installs Gradle.

**Essential workflow:**
```bash
# 1. Initial setup (once)
cp templates/private.properties ./private.properties

# 2. Clean (when switching branches or build fails)
./gradlew clean

# 3. Build debug APK (2-3 min, recommended for dev)
./gradlew assembleBasicDebug
# → main/build/outputs/apk/basic/debug/cgeo-debug.apk

# 4. Build all variants
./gradlew assembleDebug
```

**Testing:**
```bash
# Unit tests (1-2 min, no device)
./gradlew testBasicDebugUnitTest

# Instrumentation tests (15-30 min, needs emulator/device)
./gradlew connectedBasicDebugAndroidTest
# CRITICAL: Serialized via mutex lock (max wait 120 min)

# All tests (20-40 min)
./gradlew testDebug
```

**Code Quality (MUST pass for CI):**
```bash
# Checkstyle (30s) - Config: checkstyle.xml (v10.26.1)
./gradlew --no-configuration-cache --no-daemon --scan checkstyle

# Android Lint (2-3 min) - Config: main/lint.xml
./gradlew --no-configuration-cache --no-daemon --scan lintBasicDebug

# PMD (optional) - Config: ruleset.xml (v7.16.0)
./gradlew pmd
```

**Install & Run:**
```bash
./gradlew installBasicDebug  # App ID: cgeo.geocaching.developer
./gradlew runBasicDebug      # Installs + launches MainActivity (needs adb)
```

## Common Issues

1. **"Could not resolve gradle"**: Network/SDK issue. Check internet and Android SDK.
2. **"Must provide API keys"**: Missing `private.properties`. Copy from `templates/`.
3. **Keys.xml not updating**: Delete `main/src/main/res/values/keys.xml` first.
4. **Tests "locked"**: Mutex prevents parallel tests. Wait up to 120 min (normal).
5. **Emulator timeout**: Tests take 15+ min. OC.de must be reachable.
6. **Config cache warnings**: Some tasks need `--no-configuration-cache` flag.

## Project Structure

**Root files:** `build.gradle` (plugins), `settings.gradle` (modules), `gradle.properties` (2GB heap), `checkstyle.xml`, `ruleset.xml`, `suppressions.xml`, `Jenkinsfile`

**Main module** (`./main/`):
- `src/main/java/cgeo/geocaching/` - 1,150+ Java files
  - Entry: `MainActivity.java`, `CgeoApplication.java`
  - Key packages: `models/`, `connector/` (platform integrations), `storage/` (DB), `network/` (HTTP), `maps/`, `filters/`, `settings/`, `utils/`, `wherigo/` (20+ total)
- `src/test/java/` - Unit tests (JVM)
- `src/androidTest/java/` - Instrumentation tests
- `src/nofoss/java/` - Non-FOSS (Google ML Kit)
- `templates/` - Template files (keys.xml, BuildConfig.java)

**Other modules:** `mapswithme-api/`, `organicmaps-api/`, `tests/`

**Config:** `.editorconfig` (4 spaces, UTF-8), `.gitignore` (excludes build/, keys.xml, private.properties)

## CI Workflows (`.github/workflows/`)

**Main: `tests.yml`** (triggers: push, PR, manual)
1. check-websites → check-secrets → checkstyle → lint
2. integration-tests (matrix: API 26/30/34)
   - Build: `./gradlew --no-configuration-cache --scan packageBasicDebug packageBasicDebugAndroidTest`
   - Test: `./gradlew --no-configuration-cache --scan testDebug -Pandroid.testInstrumentationRunnerArguments.notAnnotation=cgeo.geocaching.test.NotForIntegrationTests`
   - Uses mutex for live tests
3. rerun-on-failure (max 3 attempts)

**Action:** `.github/actions/cgeo-preferences/` - Sets up API keys from secrets
**Others:** `branch-merge.yml`, `watchdog.yml`, `unit-tests-rerun.yml`, `unlock-unit-tests.yml`

## Pre-Submit Validation

Run in order:
```bash
./gradlew clean assembleBasicDebug              # 2-3 min
./gradlew --no-configuration-cache checkstyle   # No errors
./gradlew --no-configuration-cache lintBasicDebug  # No fatal errors
./gradlew testBasicDebugUnitTest                # 1-2 min
./gradlew connectedBasicDebugAndroidTest        # 15-30 min (if applicable)
```

## Development Notes

**Branches:** `master` (new features, nightly), `release` (bug fixes). Always merge `release` → `master` after fixes.

**Code Style:** 4 spaces, LF endings, imports ordered: cgeo→android→androidx→java→javax→*. Use `final` for local vars/params (Checkstyle enforced).

**Testing:** Unit tests in `main/src/test/java`, instrumentation in `main/src/androidTest/java`. Premium account needed for full coverage. Mark non-integration tests with `@NotForIntegrationTests`. Uses AssertJ 2.9.1.

**Build Variants:** debug (dev, no ProGuard), nightly (requires `NB`), rc (requires `RC`), release (needs signing), legacy

**Flavors:** basic (full, non-FOSS), foss (no proprietary), nojit (testing)

**Dependency Constraints (DO NOT upgrade):**
- AssertJ ≤2.x (Android compat)
- Commons-IO ≤2.5 (Path needs API 26+)
- Commons-Lang3 ≤3.11 (StringJoiner needs API 24+)
- Jackson ≤2.12.x (2.13+ needs minSdk 24)

**Performance:** Gradle daemon, parallel builds, config cache (some tasks need `--no-configuration-cache`), build cache in `build-cache/`. Clean: 2-3min, incremental: 20-60s.

## Trust These Instructions

Comprehensive and validated. Only search if: command fails with undocumented error, need specific file/class details, or instructions outdated (check README.md). For implementation details, refer to source code directly.
