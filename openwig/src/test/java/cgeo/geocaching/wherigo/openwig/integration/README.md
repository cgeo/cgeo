# GWC Integration Test

## Overview

The `GwcIntegrationTest` class provides integration testing for the OpenWIG framework's ability to download, parse, and initialize Wherigo cartridge (GWC) files.

## Purpose

This test validates the complete workflow of working with GWC files:

1. **File Creation/Download**: Creates a minimal valid GWC file in memory (simulating a download)
2. **Cartridge Loading**: Uses the CartridgeFile class to parse the GWC file format
3. **Metadata Extraction**: Verifies that cartridge metadata (name, author, coordinates, etc.) is correctly parsed
4. **Bytecode Extraction**: Ensures Lua bytecode can be extracted from the cartridge
5. **Engine Initialization**: Validates that the Engine can be initialized with the loaded cartridge

## Test Coverage

### Tests Included

1. **testLoadGwcCartridge()**: 
   - Loads a test GWC file
   - Verifies all metadata fields are correctly parsed
   - Validates bytecode extraction
   - Confirms engine initialization succeeds

2. **testLoadMultipleGwcCartridges()**:
   - Tests loading multiple cartridges independently
   - Verifies each cartridge maintains its own state
   - Confirms multiple engines can be created

## Test Implementation Details

### Test Helper Classes

The test includes complete implementations of the platform interfaces required by OpenWIG:

- **TestSeekableFile**: In-memory implementation of SeekableFile for reading GWC data
- **TestFileHandle**: In-memory implementation of FileHandle for save game files
- **TestUI**: Mock UI implementation that tracks method calls
- **TestLocationService**: Mock GPS service with fixed coordinates

### GWC File Format

The test creates minimal valid GWC files with the following structure:

```
- 7-byte header: 02 0a CART 00
- 2-byte file count
- File table (id + offset for each file)
- Metadata header:
  - Coordinates (latitude, longitude)
  - Cartridge properties (type, name, author, version, etc.)
- Lua bytecode (minimal valid Lua 5.1 bytecode)
```

## Limitations

### Why Not Full Execution Testing?

The test focuses on loading and initialization rather than full cartridge execution because:

1. **Missing Resources**: Full execution requires `stdlib.lbc` (Lua standard library bytecode) which is not in the source tree. This file is typically provided at runtime in an Android environment.

2. **Android Runtime**: OpenWIG is an Android library. Full execution would require:
   - Android SDK
   - Android runtime environment
   - Proper resource loading infrastructure

3. **Minimal Changes**: Following the project's minimal changes principle, creating or bundling `stdlib.lbc` would be a significant addition.

### What Is Tested

Despite not running full execution, the test still validates:

- ✅ Complete GWC file format parsing
- ✅ Binary data structure reading (little-endian integers, doubles, strings)
- ✅ Cartridge metadata extraction
- ✅ Lua bytecode validation
- ✅ Engine object creation and initialization
- ✅ Platform interface integration
- ✅ Multiple cartridge support

### What Requires Manual/Android Testing

The following aspects require testing in a full Android environment:

- [ ] Lua script execution
- [ ] Event system operation
- [ ] GPS-based zone detection
- [ ] Save/restore functionality
- [ ] Media playback
- [ ] Full game loop execution

## Running the Tests

### Standard Test Execution

```bash
# Run all tests
./gradlew test

# Run only integration tests
./gradlew test --tests "*.integration.*"

# Run specific test
./gradlew test --tests "GwcIntegrationTest"
```

### Expected Results

Both test methods should pass, demonstrating:
- Successful GWC file parsing
- Correct metadata extraction
- Valid engine initialization
- Support for multiple cartridges

## Extending the Tests

### Adding More Test Scenarios

To add additional integration test scenarios:

1. **Different Cartridge Types**: Use `createCustomTestGwcFile()` to create cartridges with different metadata
2. **Invalid Files**: Add negative tests for malformed GWC files
3. **Large Cartridges**: Test with cartridges containing multiple embedded resources
4. **Coordinate Variations**: Test cartridges from different geographic locations

### Example: Testing Invalid Files

```java
@Test(expected = IOException.class)
public void testInvalidGwcFile() throws Exception {
    // Create a file with invalid header
    byte[] invalidData = new byte[] { 0x00, 0x00, 0x00, 0x00 };
    TestSeekableFile invalidFile = new TestSeekableFile(invalidData);
    TestFileHandle saveFile = new TestFileHandle();
    
    // This should throw IOException
    CartridgeFile.read(invalidFile, saveFile);
}
```

## Integration with CI/CD

These tests can be integrated into CI/CD pipelines:

```yaml
# Example GitHub Actions workflow
name: Run Tests

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      
      - name: Set up JDK
        uses: actions/setup-java@v2
        with:
          java-version: '17'
          distribution: 'temurin'
      
      - name: Run Unit Tests
        run: ./gradlew test
      
      - name: Run Integration Tests
        run: ./gradlew test --tests "*.integration.*"
```

## References

- **GWC Format**: See `CartridgeFile.java` for complete format documentation
- **Engine Architecture**: See `Engine.java` for initialization process
- **Platform Interfaces**: See `platform/` package for interface definitions
- **Lua Integration**: See `kahlua/` package for Lua VM implementation

## Contributing

When adding new integration tests:

1. Follow the existing test structure
2. Use test helper classes for platform interfaces
3. Document test purpose and limitations
4. Add appropriate assertions
5. Consider both positive and negative test cases
6. Keep tests focused and independent
