# LuaTable Class Hierarchy Documentation

## Overview

This document describes the class hierarchy of `LuaTable` and its implementations in the openWIG project. The LuaTable hierarchy provides the foundation for representing Lua tables in Java, which is essential for the Wherigo game engine.

## Class Hierarchy

```
LuaTable (interface)
├── LuaTableImpl (implementation class)
│   └── EventTable (extends LuaTableImpl)
│       ├── Timer (extends EventTable)
│       ├── Task (extends EventTable)
│       ├── Media (extends EventTable)
│       ├── Action (extends EventTable)
│       └── Container (extends EventTable)
│           └── Thing (extends Container)
│               ├── Zone (extends Thing)
│               └── Player (extends Thing)
└── ZonePoint (implements LuaTable directly)
```

## Detailed Class Responsibilities

```
┌─────────────────────────────────────────────────────────────────┐
│ LuaTable (Interface)                                            │
│ • Core table operations (rawget, rawset, next, len, keys)      │
│ • Metatable support                                             │
│ • Serializable for save game persistence                        │
└─────────────────────────────────────────────────────────────────┘
          ▲                                          ▲
          │                                          │
          │                                          │
┌─────────┴───────────────────────┐    ┌────────────┴─────────────────────┐
│ LuaTableImpl                    │    │ ZonePoint                         │
│ • Hash table with chaining      │    │ • Geographic coordinates          │
│ • Weak reference support         │    │ • Lightweight (3 fixed keys)     │
│ • Dynamic resizing/rehashing     │    │ • Distance/bearing calculations  │
│ • Key caching optimization       │    │ • No metatable support           │
└─────────┬───────────────────────┘    └──────────────────────────────────┘
          │
          │
┌─────────┴───────────────────────────────────────────────────────┐
│ EventTable                                                       │
│ • Event system (callEvent, hasEvent)                            │
│ • Property interception (setItem/getItem hooks)                 │
│ • Game object properties (name, description, position, visible) │
│ • Serialization/deserialization for save games                  │
│ • Media and icon management                                     │
└─────────┬───────────────────────────────────────────────────────┘
          │
          ├── Timer: Countdown/interval timers with OnTick events
          │
          ├── Task: Quest tasks with active/complete states
          │
          ├── Media: Media resources (images, sounds) with file refs
          │
          ├── Action: Player action commands
          │
          └── Container: Inventory management
                  │
                  └── Thing: Items and characters
                          │
                          ├── Zone: Geographic zones with boundaries
                          │
                          └── Player: Player character with inventory
```

## Core Components

### 1. LuaTable (Interface)

**Location**: `src/main/java/cgeo/geocaching/wherigo/kahlua/vm/LuaTable.java`

**Purpose**: Defines the contract for Lua table implementations.

**Key Methods**:
- `void rawset(Object key, Object value)` - Sets a value in the table
- `<T> T rawget(Object key)` - Gets a value from the table
- `Object next(Object key)` - Iterator support for table traversal
- `int len()` - Returns the length of the table
- `Iterator<Object> keys()` - Returns an iterator over keys
- `void setMetatable(LuaTable metatable)` - Sets the metatable
- `LuaTable getMetatable()` - Gets the metatable

**Design Notes**: This is a clean interface that follows the Lua table semantics closely. It extends `Serializable` to support game state persistence.

### 2. LuaTableImpl (Implementation)

**Location**: `src/main/java/cgeo/geocaching/wherigo/kahlua/vm/LuaTableImpl.java`

**Purpose**: Provides the core hash table implementation for Lua tables.

**Key Features**:
- **Hash-based storage**: Uses separate chaining for collision resolution
- **Weak references**: Supports weak keys and weak values through the metatable `__mode` field
- **Dynamic resizing**: Automatically rehashes when the table grows
- **Performance optimization**: Implements key index caching for faster lookups
- **Lua semantics**: Handles special cases like NaN keys and double equality

**Internal Structure**:
- `Object[] keys` - Key storage array
- `Object[] values` - Value storage array
- `int[] next` - Collision chain pointers
- `int freeIndex` - Index of next free slot
- `Object keyIndexCacheKey` / `int keyIndexCacheValue` - Single-entry cache

**Weak Reference Handling**:
The table supports weak keys and weak values based on the metatable's `__mode` field:
- `'k'` in mode string = weak keys
- `'v'` in mode string = weak values
- Uses Java's `WeakReference` for weak entries

### 3. EventTable (Extended Implementation)

**Location**: `src/main/java/cgeo/geocaching/wherigo/openwig/EventTable.java`

**Purpose**: Extends `LuaTableImpl` to add game object functionality including:
- Event handling (OnTick, OnStart, OnStop, etc.)
- Serialization/deserialization for save games
- Common game object properties (name, description, position, visibility)
- Media and icon management

**Key Features**:
- **Event system**: `callEvent(String name, Object param)` method for Lua event callbacks
- **Property interception**: Overrides `rawset()` to intercept specific property assignments
- **Custom tostring**: Implements custom Lua `__tostring` metamethod
- **Position tracking**: Maintains `ZonePoint position` for locatable objects

**Subclasses**:
- `Timer`: Implements countdown and interval timers with event callbacks
- `Task`: Represents Wherigo tasks with active/complete states
- `Media`: Represents media resources (images, sounds) with file management
- `Action`: Represents player actions
- `Container`: Adds inventory management for objects that can contain other objects
  - `Thing`: Represents items and characters in the game
    - `Zone`: Geographic zones with boundary checking
    - `Player`: The player character with inventory

### 4. ZonePoint (Direct Implementation)

**Location**: `src/main/java/cgeo/geocaching/wherigo/openwig/ZonePoint.java`

**Purpose**: Represents a geographic coordinate (latitude, longitude, altitude) as a Lua table.

**Special Characteristics**:
- **Lightweight**: Implements `LuaTable` directly without hash table overhead
- **Fixed keys**: Only supports three keys: "latitude", "longitude", "altitude"
- **No metatable**: Returns null for metatable operations
- **Geographic calculations**: Provides distance, bearing, and coordinate conversion methods

**Design Rationale**: Since ZonePoint only needs three fixed properties, it implements `LuaTable` directly rather than extending `LuaTableImpl`, avoiding the memory and performance overhead of a full hash table.

## Identified Issues and Improvements

### ~~Critical Issue: Infinite Recursion in EventTable.rawset()~~ - FIXED

**Location**: `EventTable.java`, line 158-165

**Problem**: 
```java
public void rawset(Object key, Object value) {
    if (key instanceof String) {
        setItem((String) key, value);
    }
    this.rawset(key, value);  // BUG: Calls itself recursively!
    Engine.log(...);
}
```

This method called itself unconditionally, causing a `StackOverflowError`.

**Status**: **FIXED** - Changed to call `super.rawset(key, value)` to delegate to parent class.

### ~~Issue: Missing 'table' Field Declaration~~ - FIXED

**Location**: `EventTable.java`, `Timer.java`, `Thing.java`, `Container.java`

**Problem**: The code referenced `table` field (e.g., `table.rawset()` in Timer), but EventTable and its subclasses didn't declare this field. This would cause compilation errors.

**Examples**: 
- Timer.java line 69-71 used `table.rawset()` 
- Container.java line 45-48 used `table.rawset()`
- Thing.java line 39 used `table.rawset()`

**Status**: **FIXED** - Replaced all `table.rawset()` calls with `rawset()` since EventTable itself is a LuaTable.

### ~~Issue: Null Metatable in EventTable Constructor~~ - FIXED

**Location**: `EventTable.java`, line 43

**Problem**: The constructor tried to call `super.getMetatable().rawset()` but the metatable is initially null, which would cause a `NullPointerException`.

**Status**: **FIXED** - Updated constructor to create and initialize a metatable before setting the `__tostring` metamethod.

### Issue: Weak Reference Memory Leaks

**Location**: `LuaTableImpl.java`, weak reference handling

**Problem**: The weak reference implementation doesn't handle the case where a WeakReference becomes null (garbage collected) during normal operations. When iterating or searching, null values from collected weak references may cause unexpected behavior.

**Recommendation**:
- Add periodic cleanup to remove entries with collected weak references
- Document that weak tables may have "ghost" entries
- Consider implementing a more robust weak reference cleanup strategy

### Issue: Thread Safety

**Problem**: Neither `LuaTableImpl` nor `EventTable` are thread-safe. If the game engine uses multiple threads (e.g., UI thread and game logic thread), race conditions could occur.

**Current Status**: No synchronization mechanisms are present.

**Recommendation**:
- Document that LuaTable implementations are not thread-safe
- If multi-threading is needed, consider:
  - Adding synchronized blocks to critical sections
  - Using `ConcurrentHashMap`-based implementation
  - Documenting thread ownership requirements

### Issue: Serialization Fragility

**Problem**: The serialization implementation in `EventTable` and subclasses uses `DataInputStream`/`DataOutputStream` with a custom format. This is fragile:
- No version control for save game format
- Changing class structure breaks old save games
- Manual serialization is error-prone

**Current Workaround**: The code has a `isDeserializing` flag to suppress errors during deserialization (see EventTable line 124-128).

**Recommendation**:
- Add version numbers to serialized format
- Consider using a more robust serialization framework
- Document the serialization format
- Add migration support for old save games

## Suggested Improvements

### 1. Interface Segregation

**Current Problem**: The `LuaTable` interface is a one-size-fits-all contract. ZonePoint implements it but doesn't need most of the functionality.

**Recommendation**: Split into smaller interfaces:

```java
public interface LuaTableBasic extends Serializable {
    void rawset(Object key, Object value);
    <T> T rawget(Object key);
}

public interface LuaTableIterable extends LuaTableBasic {
    Object next(Object key);
    int len();
    Iterator<Object> keys();
}

public interface LuaTableMetatable extends LuaTableIterable {
    void setMetatable(LuaTable metatable);
    LuaTable getMetatable();
}

// Full interface for backward compatibility
public interface LuaTable extends LuaTableMetatable {
}
```

### 2. Separate Concerns in EventTable

**Current Problem**: EventTable mixes multiple concerns:
- Lua table functionality (inherited)
- Event handling
- Serialization
- Game object properties
- Logging

**Recommendation**: Consider composition over inheritance:

```java
public class EventTable extends LuaTableImpl {
    private final EventHandler eventHandler;
    private final GameObjectProperties properties;
    // ...
}
```

### 3. Documentation Improvements

Add JavaDoc comments explaining:
- The hash table algorithm in LuaTableImpl
- The weak reference behavior and its implications
- The event system and when events are called
- Thread safety guarantees (or lack thereof)
- Serialization format and compatibility

### 4. Testing Coverage

**Current State**: Only one test exists (`LuaTableImplTest.java`) that tests basic get/set operations.

**Recommendation**: Add tests for:
- Hash collisions and rehashing
- Weak reference behavior
- Metatable operations
- EventTable event handling
- ZonePoint geographic calculations
- Serialization/deserialization
- Edge cases (null keys, NaN, large tables)

### 5. Performance Optimization Opportunities

1. **Cache key hashcodes**: Currently recalculated on every lookup
2. **Optimize iteration**: The `keys()` method creates filtered iterators which have overhead
3. **Pool WeakReference objects**: Reduce GC pressure in tables with many weak entries
4. **Consider array part**: Lua's reference implementation uses a hybrid array+hash structure for better performance with sequential integer keys

## Design Patterns Used

1. **Template Method**: EventTable provides `setItem()` and `getItem()` hooks that subclasses override
2. **Strategy Pattern**: Weak reference handling can be enabled/disabled via metatable
3. **Iterator Pattern**: Support for table traversal via `next()` and `keys()`
4. **Adapter Pattern**: ZonePoint adapts geographic coordinates to the LuaTable interface

## Migration Path

Implemented improvements and remaining work:

1. **Phase 1**: Fix critical bugs - **COMPLETED**
   - ✅ Fixed infinite recursion in EventTable.rawset()
   - ✅ Fixed missing 'table' field references
   - ✅ Fixed null metatable in EventTable constructor
2. **Phase 2**: Add comprehensive tests - TODO
3. **Phase 3**: Improve documentation with JavaDoc - IN PROGRESS
   - ✅ Added comprehensive class-level JavaDoc for LuaTableImpl
   - TODO: Add method-level JavaDoc for complex algorithms
4. **Phase 4**: Add thread safety if needed - TODO
5. **Phase 5**: Consider interface segregation (requires API changes) - TODO
6. **Phase 6**: Refactor EventTable for better separation of concerns - TODO

## Conclusion

The LuaTable hierarchy is a well-structured implementation of Lua tables in Java with specialized extensions for the Wherigo game engine. The main areas for improvement are:

1. **Fix the critical infinite recursion bug** in EventTable.rawset()
2. **Clarify the 'table' field** usage in EventTable and Timer
3. **Improve documentation** for complex algorithms and design decisions
4. **Add comprehensive tests** for robustness
5. **Consider thread safety** if multi-threading is used
6. **Enhance serialization** for better forward compatibility

The architecture follows object-oriented principles well, with appropriate use of inheritance and interfaces. The direct implementation of LuaTable by ZonePoint shows good judgment in avoiding unnecessary overhead.
