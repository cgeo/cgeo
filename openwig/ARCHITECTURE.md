# OpenWIG Architecture

## System Architecture Overview

OpenWIG is a Wherigo game engine library for Android that integrates a Lua VM with GPS-based gaming. The architecture follows a clean separation between the core engine, game objects, and platform interfaces.

```
┌──────────────────────────────────────────────────────────────────────┐
│                          Android Application                         │
│              (Implements UI, LocationService, FileHandle)            │
└────────────────────────────────┬─────────────────────────────────────┘
                                 │
                                 ↓
┌──────────────────────────────────────────────────────────────────────┐
│                         OpenWIG Library                              │
│                                                                      │
│  ┌────────────────┐    ┌─────────────────┐    ┌─────────────────┐  │
│  │  Engine Core   │───→│  Lua VM (Kahlua)│───→│  Game Objects   │  │
│  │                │    │                 │    │                 │  │
│  │ • Game Loop    │    │ • Script Exec   │    │ • EventTable    │  │
│  │ • GPS Sync     │    │ • State Mgmt    │    │ • Container     │  │
│  │ • Event Dispatch    │ • LuaTable      │    │ • Thing         │  │
│  │ • Save/Restore │    │ • Coroutines    │    │ • Zone          │  │
│  └────────────────┘    └─────────────────┘    │ • Task          │  │
│                                                │ • Timer         │  │
│                                                │ • Action        │  │
│                                                │ • Media         │  │
│                                                └─────────────────┘  │
│                                                                      │
│  ┌────────────────────────────────────────────────────────────────┐ │
│  │              Platform Interfaces (User Implements)             │ │
│  │  • UI: dialogs, screens, sounds, input                        │ │
│  │  • LocationService: GPS coordinates                           │ │
│  │  • FileHandle: save game I/O                                  │ │
│  │  • SeekableFile: cartridge file reading                       │ │
│  └────────────────────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────────────────┘
```

## Component Architecture

### 1. Engine Core (`Engine.java`)

The central orchestrator that manages the entire game lifecycle.

**Responsibilities:**

- Initialize and manage the Lua VM (Kahlua)
- Synchronize player GPS position with game state
- Execute the game loop and process events
- Coordinate between platform interfaces and game objects
- Handle save/restore operations
- Support parallel cartridge execution (thread-safe)

**Key Methods:**
```java
Engine.newInstance(cartridge, log, ui, locationService)
engine.start()      // Start new game
engine.restore()    // Load saved game
engine.store()      // Save game state
```

### 2. Lua Virtual Machine (Kahlua)

Embedded Lua 5.1 VM that executes Wherigo cartridge scripts.

**Core Components:**

- `LuaState`: VM state and execution context
- `LuaTable`: Lua table implementation with Java generics
- `LuaCallFrame`: Function call stack management
- Standard libraries: Base, String, Math, Table, Coroutine

**Key Features:**

- Full Lua 5.1 compatibility
- Optimized for embedded use
- Thread-safe for parallel execution
- Integrated with Java type system

### 3. Game Object Hierarchy

All game objects inherit from `EventTable`, which provides:

- Lua table semantics
- Event handling (OnClick, OnEnter, OnExit, etc.)
- Property management (name, description, visible, etc.)
- Serialization for save games

```
EventTable (base class)
├── Container: Objects that hold items
│   └── Thing: Movable game objects
│       ├── Player: Special Thing representing the user
│       └── Zone: Geographic areas
├── Task: Quest objectives
├── Timer: Time-based events
├── Media: Images and audio
└── Action: Player commands on objects
```

**Key Properties:**

- `name`, `description`: Display text
- `visible`: Whether shown to player
- `active`: Whether object is enabled
- `position`: Geographic location (ZonePoint)

### 4. Zone System

Zones are geographic areas with proximity-based triggers.

**Zone Types:**

- **Point Zone**: Single location with radius
- **Polygonal Zone**: Defined by boundary points

**Zone States:**

- `INSIDE`: Player is within zone
- `PROXIMITY`: Player is near zone (distance < proximity range)
- `DISTANT`: Player is far from zone
- `NEARBY`: Player is approaching zone

**Events:**

- `OnEnter`: Triggered when player enters zone
- `OnExit`: Triggered when player leaves zone
- `OnProximity`: Triggered when player approaches

### 5. Platform Interfaces

Applications must implement these interfaces to integrate OpenWIG.

#### UI Interface


```java
public interface UI {
    void start();
    void end();
    void refresh();
    void showScreen(int screenId, EventTable details);
    void pushDialog(String[] texts, Media[] media, 
                   String button1, String button2, 
                   LuaClosure callback);
    void pushInput(EventTable input);
    void playSound(byte[] data, String mime);
    void showStatusText(String text);
    void setStatusIcon(byte[] data);
    void debugMsg(String msg);
}
```

#### LocationService Interface


```java
public interface LocationService {
    double getLatitude();
    double getLongitude();
    double getAltitude();
    double getHeading();
    double getPrecision();
}
```

#### FileHandle Interface


```java
public interface FileHandle {
    OutputStream openWrite() throws IOException;
    InputStream openRead() throws IOException;
    void delete() throws IOException;
    boolean exists();
}
```

### 6. Save Game System (`Savegame.java`)

Manages serialization and deserialization of game state.

**Saved Components:**

- Lua VM state (global variables, tables)
- Game object states (position, visibility, etc.)
- Player inventory
- Task completion status
- Timer states
- Zone states (inside/outside)

**Format:**

- Custom binary format optimized for Wherigo
- Handles circular references
- Preserves Lua table structure
- Supports forward/backward compatibility

### 7. Cartridge File Format (`CartridgeFile.java`)

Reads compiled Wherigo cartridge files (.gwc).

**Structure:**

- Header with metadata
- Compiled Lua bytecode
- Media resources (images, audio)
- Localization strings

### 8. Parallel Execution Support

OpenWIG supports running multiple cartridges simultaneously on different threads.

**Architecture:**

- Each `Engine` instance is bound to a specific thread
- Thread-local storage for engine context
- Automatic engine resolution for static method calls
- Separate Lua VM per engine instance

**Use Cases:**

- Multi-user scenarios
- Testing multiple cartridges
- Background cartridge processing

See [PARALLEL_EXECUTION.md](PARALLEL_EXECUTION.md) for detailed documentation.

## Data Flow

### Game Startup Flow

```
1. Application creates platform implementations (UI, LocationService)
2. Application loads cartridge file (.gwc)
3. Application creates Engine with cartridge and platform interfaces
4. Engine initializes Lua VM
5. Engine loads cartridge bytecode into VM
6. Engine calls WherigoLib.messageBox() to show cartridge info
7. Engine starts game loop
8. Lua script execution begins
9. Game displays main screen
```

### Position Update Flow

```
1. Engine queries LocationService.getLatitude/getLongitude()
2. Engine updates Player.position (ZonePoint)
3. Engine checks all Zone objects for proximity changes
4. For each zone state change:
   a. Update zone.inside/zone.proximity
   b. Trigger OnEnter/OnExit/OnProximity events
   c. Execute Lua event handlers
5. Engine calls UI.refresh() if state changed
```

### Event Handling Flow

```
1. Player action occurs (click, command, input)
2. Application calls Engine.callEvent() or object.callEvent()
3. Engine looks up event handler in object's Lua table
4. If handler exists, Engine executes Lua function
5. Lua script may:
   - Update game state
   - Show dialogs via WherigoLib.messageBox()
   - Change object properties
   - Complete tasks
   - Start timers
6. Engine calls UI.refresh() to update display
```

### Save/Restore Flow

```
Save:
1. Engine.store() called (manual or automatic)
2. Savegame serializer walks Lua VM state
3. All LuaTables are serialized recursively
4. Game object states written to FileHandle
5. Save file closed

Restore:
1. Engine.restore() called instead of start()
2. Savegame deserializer reads file
3. Lua VM state reconstructed
4. Game objects restored with saved state
5. Position checks performed
6. Game resumes from saved point
```

## Threading Model

### Single-Threaded Model (Traditional)

- One Engine instance
- All operations on main/UI thread
- Simple and predictable

### Multi-Threaded Model (Parallel Execution)

- Multiple Engine instances
- Each Engine on separate thread
- Thread-local engine context
- Separate Lua VM per thread
- No shared mutable state between engines

## Performance Considerations

### Optimizations

1. **LuaTable Key Caching**: Frequently accessed keys are cached
2. **Weak References**: Optional for memory-constrained devices
3. **Distance Calculations**: Cached between position updates
4. **Event Batching**: Multiple changes trigger single UI refresh
5. **Lazy Zone Checking**: Only active zones checked for proximity

### Memory Management

- Lua VM uses garbage collection
- Java objects managed by JVM GC
- Media resources loaded on-demand
- Save games use streaming I/O

## Type System

### LuaTable Generics

The `LuaTable<K,V>` interface provides type-safe Lua table operations:

```java
public interface LuaTable<K,V> extends Iterable<Map.Entry<K,V>> {
    void rawset(K key, V value);
    V rawget(K key);
    K next(K key);
    int len();
    default void rawset(Map.Entry<K,V> entry) { ... }
    default K next(K key) { return null; }
    default Iterator<Map.Entry<K,V>> iterator() { ... }
}
```

**Benefits:**

- Type safety at compile time
- Better IDE support
- Reduced casting errors
- Simplified API usage

See [LUATABLE_HIERARCHY.md](LUATABLE_HIERARCHY.md) for detailed class hierarchy.

## Extension Points

### Custom Game Objects

Extend `EventTable` or `Container` to create custom game objects:

```java
public class CustomObject extends EventTable {
    @Override
    public void setItem(String key, Object value) {
        // Custom property handling
        super.setItem(key, value);
    }
}
```

### Custom Events

Register custom event handlers in Lua:

```lua
myObject.OnCustomEvent = function(self)
    -- Handle custom event
end
```

Then trigger from Java:

```java
customObject.callEvent("OnCustomEvent", null);
```

## Build and Integration

### Building the Library

```bash
# Compile Java sources
cd OpenWIGLibrary
gradle compileJava

# Create JAR
gradle jar

# Run checkstyle
gradle checkstyle
```

### Integrating into Android App

1. Add OpenWIG library to dependencies
2. Implement required interfaces (UI, LocationService, FileHandle, SeekableFile)
3. Request location permissions in AndroidManifest.xml
4. Create Engine instance with implementations
5. Start game loop
6. Handle UI callbacks

See [README.md](README.md) for detailed usage examples.

## Dependencies

- **androidx.annotation:annotation**: Android annotations for nullability
- **org.apache.commons:commons-collections4**: Utility collections
- No external Lua library needed (Kahlua is embedded)

## Security Considerations

- Lua scripts run in sandboxed VM
- File access controlled through FileHandle interface
- Network access not provided by default
- GPS data controlled through LocationService interface
- Applications control what APIs exposed to Lua

## Version Compatibility

- **Wherigo 2.11** compatible
- **Java 17** required
- **Android 8.0 (API 26)** minimum
- **Android 14 (API 36)** target

## References

- [README.md](README.md) - Getting started and usage
- [LUATABLE_HIERARCHY.md](LUATABLE_HIERARCHY.md) - Detailed class hierarchy
- [PARALLEL_EXECUTION.md](PARALLEL_EXECUTION.md) - Multi-threaded execution
- [Wherigo Foundation](http://www.wherigo.com)
