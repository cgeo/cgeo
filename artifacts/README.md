# C:GEO APK Artifacts

This folder contains built APK artifacts for the c:geo application.

## Current Build

- **File**: `cgeo-foss-debug.apk`
- **Build Type**: FossDebug (FOSS variant, Debug build)
- **Build Date**: 2026-02-04
- **Size**: ~25 MB

## Build Command

The APK was generated using the following Gradle command:

```bash
./gradlew assembleFossDebug
```

## Installation

To install this APK on an Android device:

1. Enable "Install from Unknown Sources" in your device settings
2. Transfer the APK to your device
3. Open the APK file and follow the installation prompts

## About GitHub Releases

While this APK has been added to the repository in the artifacts folder, creating a GitHub release was not possible due to permission constraints. If you need to create a GitHub release, you can:

1. Navigate to the GitHub repository's "Releases" page
2. Click "Create a new release"
3. Tag the release appropriately
4. Upload the APK from this artifacts folder
5. Publish the release

## Build Details

This build was created from the c:geo Android application, which is a full-featured geocaching app for Android devices. The FOSS variant is the free and open-source version without proprietary Google services.

## Important Note About Binary Artifacts in Git

Storing binary artifacts (like APKs) in a git repository is generally not recommended as it:
- Significantly increases repository size
- Slows down cloning and fetching operations
- Cannot be easily removed from git history once committed

**Recommended Alternatives:**
1. **GitHub Releases**: Use GitHub's release feature to attach binary artifacts without bloating the repository
2. **CI/CD Artifacts**: Configure GitHub Actions or other CI systems to build and store APKs as artifacts
3. **Build Documentation**: Provide clear build instructions so users can build the APK themselves
4. **External Storage**: Use services like GitHub Packages, Maven repositories, or cloud storage for distributing builds

This APK was added to the repository as a temporary solution per the specific requirements. For long-term distribution, consider migrating to one of the alternatives above.
