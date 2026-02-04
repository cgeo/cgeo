# OpenWIG

OpenWIG is an Android library for playing Wherigo location-based games. It provides a complete engine for running Wherigo cartridges (.gwc files), managing GPS-based gameplay, and interfacing with the Lua scripting environment.

## Overview

Wherigo is a platform for creating and playing location-based games. OpenWIG is an open-source implementation of the Wherigo player engine that can be embedded into Android applications.

**Key Features:**
- Complete Wherigo cartridge (.gwc) file support
- Lua scripting engine integration (Kahlua VM)
- GPS-based zone proximity detection
- Save/restore game state
- Event-driven architecture
- Media playback (images, audio)
- Task and inventory management
- **NEW:** Parallel cartridge execution - run multiple cartridges simultaneously

## Documentation

- **[README.md](README.md)** (this file) - Quick start guide and usage examples
- **[ARCHITECTURE.md](ARCHITECTURE.md)** - Comprehensive architecture overview and system design
- **[LUATABLE_HIERARCHY.md](LUATABLE_HIERARCHY.md)** - Detailed LuaTable class hierarchy
- **[PARALLEL_EXECUTION.md](PARALLEL_EXECUTION.md)** - Multi-threaded cartridge execution guide

## Requirements

- **Minimum SDK:** 26 (Android 8.0 Oreo)
- **Target SDK:** 36
- **Java Version:** 17
- **Compile SDK:** 36

## Dependencies

- `androidx.annotation:annotation:1.9.1`
- `androidx.appcompat:appcompat:1.7.1`
- `com.google.android.material:material:1.13.0`
- `org.apache.commons:commons-collections4:4.5.0`

## Architecture

For a comprehensive architecture overview, see [ARCHITECTURE.md](ARCHITECTURE.md).

OpenWIG consists of several key components:

### Core Engine

- **Engine**: The heart of OpenWIG, managing the Lua VM, GPS integration, and game loop
- **Cartridge**: Root container for all game objects (zones, items, tasks, etc.)
- **Player**: Represents the human player with GPS-synchronized position

### Game Objects

- **Zone**: Geographic areas with proximity detection (enters, exits, nearby)
- **Thing**: Items and characters that can be carried and interacted with
- **Task**: Objectives and quests for players to complete
- **Action**: Commands that players can execute on objects
- **Timer**: Time-based events and challenges
- **Media**: Images and audio resources

### Platform Interfaces

You must implement these interfaces to integrate OpenWIG into your application:

- **UI**: Display dialogs, screens, play sounds, show status
- **LocationService**: Provide GPS coordinates (lat/lon/altitude)
- **FileHandle**: File I/O for save games
- **SeekableFile**: Random-access file reading for cartridge files

### Data Management

- **EventTable**: Base class for all game objects with Lua table integration
- **Container**: Objects that can hold items (player inventory, zones, etc.)
- **ZonePoint**: Geographic coordinate with distance/bearing calculations
- **Savegame**: Serialization and deserialization of game state

## Usage

### Basic Integration

```java
// 1. Implement the platform interfaces
public class MyUI implements UI {
    @Override
    public void showDialog(String[] texts, Media[] media, 
                          String button1, String button2, 
                          LuaClosure callback) {
        // Show dialog to user
    }
    // ... implement other UI methods
}

public class MyLocationService implements LocationService {
    @Override
    public double getLatitude() {
        // Return current GPS latitude
    }
    // ... implement other location methods
}

// 2. Set up device ID (required)
WherigoLib.env.put(WherigoLib.DEVICE_ID, "your-device-id");

// 3. Create cartridge file
SeekableFile cartFile = /* your seekable file implementation */;
FileHandle saveFile = /* your save file handle */;
CartridgeFile cartridge = CartridgeFile.read(cartFile, saveFile);

// 4. Create and start engine
UI ui = new MyUI();
LocationService gps = new MyLocationService();
OutputStream log = System.out; // optional logging

Engine engine = Engine.newInstance(cartridge, log, ui, gps);

// For new game:
engine.start();

// For continuing saved game:
engine.restore();
```

### Handling Game Events

Game objects use events to notify your UI of game state changes:

```java
// The engine will call UI methods when events occur:
- ui.start() - Game is ready to start
- ui.end() - Game has ended
- ui.refresh() - Update display to reflect changes
- ui.showScreen(screenId, details) - Show specific screen
- ui.pushDialog(...) - Display dialog to user
- ui.pushInput(...) - Request input from user
```

### Position Updates

The engine continuously monitors player position:

```java
// Engine automatically:
// 1. Calls gps.getLatitude(), getLongitude(), getAltitude()
// 2. Updates player.position
// 3. Checks zones for proximity
// 4. Triggers zone events (OnEnter, OnExit, OnProximity)
```

### Saving and Loading

```java
// Manual save (also triggered by RequestSync from Lua):
engine.store();

// Save happens automatically when:
// - Lua script calls RequestSync()
// - Player enters/exits zones (with save-on-zone-change enabled)

// Load saved game:
engine.restore(); // instead of engine.start()
```

## Parallel Cartridge Execution

OpenWIG now supports running multiple cartridges simultaneously on different threads. This is useful for:
- Testing multiple scenarios
- Running background cartridges
- Multi-user applications

### Basic Parallel Execution

```java
// Create engines for multiple cartridges
Engine engine1 = new Engine(cartridge1, log1, ui1, gps1);
Engine engine2 = new Engine(cartridge2, log2, ui2, gps2);

// Run on separate threads
ExecutorService executor = Executors.newFixedThreadPool(2);
executor.submit(() -> engine1.start());
executor.submit(() -> engine2.start());
```

**Important:**
- Each engine must have its own UI and LocationService instance
- Engines must run on separate threads
- Static methods automatically resolve to the current thread's engine

For detailed information, see [PARALLEL_EXECUTION.md](PARALLEL_EXECUTION.md).

## API Documentation

Comprehensive Javadoc is provided for all public API classes. Key classes to understand:

- **Engine**: Main entry point, creates and manages game loop
- **UI**: Interface for displaying game content to users
- **LocationService**: Interface for GPS position data
- **CartridgeFile**: Reads .gwc cartridge files
- **EventTable**: Base class for all game objects
- **Zone**: Geographic zones with proximity detection
- **Thing**: Items and characters in the game
- **Player**: Special Thing representing the human player
- **Task**: Game objectives and quests
- **Action**: Commands that can be executed
- **Timer**: Time-based events
- **Media**: Images and audio resources
- **Savegame**: Game state persistence

## Building

### Full Android Library Build

This is an Android library project. To build the complete Android library with all features:

```bash
# Requires Android SDK installed
./gradlew build
```

**Note**: The full Android build requires Android SDK to be installed and configured.

### Building OpenWIGLibrary (Java Only)

To compile just the core OpenWIG library without Android dependencies:

```bash
cd OpenWIGLibrary
gradle compileJava   # Compile sources
gradle jar           # Create JAR file
gradle build         # Full build with checkstyle
```

This creates a standalone JAR at `OpenWIGLibrary/build/libs/OpenWIGLibrary.jar` that can be used in any Java project.

### Prerequisites

- **Java 17** or higher
- **Gradle** (or use included gradlew wrapper)
- **Android SDK** (only for full Android library build)

## Testing

```bash
# Run unit tests
./gradlew test

# Run Android instrumentation tests  
./gradlew connectedAndroidTest
```

## Version

Current version: **428** (compatible with Wherigo 2.11)

## Origin

This code was initially copied from [cgeo/openWIG](https://github.com/cgeo/openWIG) in April 2025, Release 1.1.0 (commit 4386a025b88aac759e1e67cb27bcc50692d61d9a), originally in package `cz.matejcik.openwig`.

Additional Gradle and build system improvements from [bekuno's PR #20](https://github.com/cgeo/openWIG/pull/20) have been applied, including:
- Build configuration enhancements with AGP 8.2.2
- Maven publishing support for library distribution
- Improved Git attributes for cross-platform development
- Enhanced .gitignore patterns for build artifacts and IDE files

## License

[Check the repository for license information]

## Contributing

[Add contribution guidelines as needed]

## Support

For issues and questions, please use the GitHub issue tracker.
