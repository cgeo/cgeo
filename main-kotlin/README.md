# c:geo Kotlin Version

This directory contains a Kotlin conversion of the c:geo Android application.

## Overview

This module represents an automated conversion of the entire c:geo app from Java to Kotlin. The original Java code from `/main` has been converted using an automated Java-to-Kotlin converter script.

## Directory Structure

```
main-kotlin/
├── src/
│   ├── main/kotlin/          # Main application code (1,152 Kotlin files)
│   ├── test/kotlin/          # Unit tests (75 Kotlin files)
│   ├── androidTest/kotlin/   # Android instrumentation tests (113 Kotlin files)
│   └── nofoss/kotlin/        # Non-FOSS specific code (1 Kotlin file)
├── thirdparty/               # Third-party Java libraries (kept as Java)
├── build.gradle              # Kotlin-enabled build configuration
├── AndroidManifest.xml       # Android manifest
└── ...
```

## Conversion Statistics

- **Total Kotlin files**: 1,341
- **Main source files**: 1,152
- **Test files**: 75
- **AndroidTest files**: 113
- **NoFOSS files**: 1
- **Third-party Java files**: 1 (kept as-is)

## Build Configuration

### Key Changes from Original `/main`

1. **Kotlin Plugin**: Added `kotlin-android` plugin to build.gradle
2. **Kotlin Options**: Configured `kotlinOptions` with JVM target 1.8
3. **Source Directories**: Changed from `src/*/java` to `src/*/kotlin`
4. **Application ID**: Changed to `cgeo.geocaching.kotlin` to avoid conflicts
5. **Archive Name**: Changed to `cgeo-kotlin`
6. **Dependencies**: Added Kotlin standard library

### Gradle Configuration

The module is configured in `/settings.gradle` as:
```gradle
include ':main-kotlin'
```

## Build Commands

```bash
# Build debug APK
./gradlew :main-kotlin:assembleDebug

# Run tests
./gradlew :main-kotlin:testBasicDebugUnitTest

# Install on device
./gradlew :main-kotlin:installBasicDebug
```

## Conversion Notes

### What Was Converted

The automated conversion handled:
- Package and import declarations
- Class, interface, and enum declarations
- Basic method declarations
- Field declarations (with limitations)
- Primitive type conversions (int → Int, boolean → Boolean, etc.)
- Keyword conversions (instanceof → is, new keyword removed)
- System.out.println → println
- Basic annotation conversions (@Override → override)
- Semicolon removal
- Inheritance syntax (extends → :, implements → :)

### Known Limitations

This automated conversion has known issues that require manual fixes:

1. **Method Signatures**: Parameter types may not be correctly converted (Java: `Type name` vs Kotlin: `name: Type`)
2. **Return Types**: Method return types need manual review
3. **Generics**: Generic syntax requires corrections
4. **Constructors**: Constructor syntax needs conversion
5. **Static Members**: Static methods/fields need to be moved to companion objects
6. **Nullable Types**: Proper null-safety markers (?) need to be added
7. **Lambda Expressions**: Java lambdas need Kotlin lambda syntax
8. **Try-Catch Blocks**: May need syntax adjustments
9. **Arrays**: Array declaration syntax needs review
10. **Property Access**: Java getters/setters should use Kotlin property syntax

### Example Issues

```kotlin
// Issue: Incorrect method signature
override public Unit onCreate(final Bundle savedInstanceState) { ... }

// Should be:
override fun onCreate(savedInstanceState: Bundle?) { ... }

// Issue: Lambda syntax
button.setOnClickListener(v -> doSomething())

// Should be:
button.setOnClickListener { v -> doSomething() }
```

## Next Steps for Full Kotlin Conversion

To make this code production-ready, the following steps are needed:

1. **Fix Compilation Errors**: 
   - Correct method signatures
   - Fix generic types
   - Update constructors
   - Convert static members to companion objects

2. **Kotlin Idioms**:
   - Convert Java getters/setters to Kotlin properties
   - Use data classes where appropriate
   - Apply extension functions
   - Use scope functions (let, apply, with, run, also)

3. **Null Safety**:
   - Add proper nullable types (?)
   - Remove unnecessary null checks
   - Use safe calls (?.) and Elvis operator (?:)

4. **Modern Kotlin Features**:
   - Convert to sealed classes/interfaces where appropriate
   - Use coroutines instead of RxJava (optional)
   - Apply Kotlin Flow (optional)

5. **Testing**:
   - Verify all tests pass after fixes
   - Add Kotlin-specific tests
   - Test on multiple devices

## Recommended Approach

For a production-quality conversion, we recommend:

1. Use **IntelliJ IDEA** or **Android Studio**'s built-in Java-to-Kotlin converter
2. Convert files incrementally, testing after each conversion
3. Have the converted code reviewed by Kotlin-experienced developers
4. Run the full test suite after each major conversion batch

## Important Notes

- **This is a starting point, not a final solution**
- The code WILL NOT compile without manual fixes
- All files have a warning header noting they require review
- Third-party libraries in `/thirdparty` are intentionally kept as Java
- The conversion maintains the same application structure and logic

## Resources

- [Kotlin Documentation](https://kotlinlang.org/docs/home.html)
- [Calling Java from Kotlin](https://kotlinlang.org/docs/java-interop.html)
- [Calling Kotlin from Java](https://kotlinlang.org/docs/java-to-kotlin-interop.html)
- [Android Kotlin Style Guide](https://developer.android.com/kotlin/style-guide)

## License

Same as the original c:geo project.
