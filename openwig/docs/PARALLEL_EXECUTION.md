# Parallel Cartridge Execution

OpenWIG now supports running multiple cartridge files in parallel. This feature allows you to execute different Wherigo games simultaneously on separate threads.

## Overview

The engine has been refactored to support multiple instances running concurrently. Each engine instance maintains its own:
- Lua state (`luaState`)
- UI implementation (`uiInstance`)
- LocationService (`gpsInstance`)
- Player object
- Cartridge state
- Event queue

## How It Works

The parallel execution support is built on a ThreadLocal-based architecture:

1. Each Engine instance stores its context in a ThreadLocal variable when it starts
2. Static method calls automatically resolve to the current thread's engine instance
3. Multiple engines can run on different threads without interfering with each other

## Usage

### Single Cartridge (Backward Compatible)

The existing API remains unchanged for single cartridge execution:

```java
// Traditional approach (still works)
UI ui = new MyUI();
LocationService gps = new MyLocationService();

Engine engine = Engine.newInstance(cartridge, log, ui, gps);
engine.start();
```

### Multiple Cartridges in Parallel

To run multiple cartridges in parallel, create separate Engine instances and run them on different threads:

```java
// Cartridge 1
CartridgeFile cartridge1 = CartridgeFile.read(file1, saveFile1);
UI ui1 = new MyUI();
LocationService gps1 = new MyLocationService();
Engine engine1 = new Engine(cartridge1, log1, ui1, gps1);

// Cartridge 2
CartridgeFile cartridge2 = CartridgeFile.read(file2, saveFile2);
UI ui2 = new MyUI();
LocationService gps2 = new MyLocationService();
Engine engine2 = new Engine(cartridge2, log2, ui2, gps2);

// Run on separate threads
Thread thread1 = new Thread(() -> engine1.start());
Thread thread2 = new Thread(() -> engine2.start());

thread1.start();
thread2.start();
```

### Using ExecutorService

For better thread management, use an ExecutorService:

```java
ExecutorService executor = Executors.newFixedThreadPool(2);

// Submit first cartridge
executor.submit(() -> {
    try {
        CartridgeFile cartridge1 = CartridgeFile.read(file1, saveFile1);
        UI ui1 = new MyUI();
        LocationService gps1 = new MyLocationService();
        Engine engine1 = new Engine(cartridge1, log1, ui1, gps1);
        engine1.start();
    } catch (IOException e) {
        e.printStackTrace();
    }
});

// Submit second cartridge
executor.submit(() -> {
    try {
        CartridgeFile cartridge2 = CartridgeFile.read(file2, saveFile2);
        UI ui2 = new MyUI();
        LocationService gps2 = new MyLocationService();
        Engine engine2 = new Engine(cartridge2, log2, ui2, gps2);
        engine2.start();
    } catch (IOException e) {
        e.printStackTrace();
    }
});

executor.shutdown();
```

## Important Considerations

### Thread Safety

- Each Engine instance must run on its own thread
- Each Engine requires its own UI and LocationService implementation
- Don't share UI or LocationService instances between engines
- Static fields are maintained for backward compatibility but are thread-local when multiple engines run

### Resource Management

- Each cartridge consumes memory for its Lua state and game objects
- Consider the device's capabilities when running multiple cartridges
- Properly shut down engines when done:

```java
Engine.kill(); // Stops the current thread's engine
```

### GPS and Location Updates

When running multiple cartridges:
- Each engine can have its own LocationService implementation
- You can share GPS data by implementing LocationService to return the same coordinates
- Or provide different locations for testing different scenarios

### Save Games

- Each cartridge should have its own save file
- Save/restore operations are per-engine instance
- Ensure saveFile references don't conflict

## Architecture Details

### ThreadLocal Context

The Engine uses ThreadLocal storage to maintain per-thread context:

```java
private static final ThreadLocal<Engine> threadLocalInstance = new ThreadLocal<>();

public static Engine getCurrentInstance() {
    Engine threadEngine = threadLocalInstance.get();
    return threadEngine != null ? threadEngine : instance;
}
```

This allows static methods throughout the codebase to automatically resolve to the correct engine instance.

### Backward Compatibility

The refactoring maintains full backward compatibility:

- Static `Engine.instance` field still exists (deprecated)
- Static `Engine.state` field still exists (deprecated)
- Static `Engine.ui` and `Engine.gps` still exist (deprecated)
- Old API `Engine.newInstance()` still works

New code should use the instance-based constructor:
```java
Engine engine = new Engine(cartridgeFile, logStream, ui, locationService);
```

## Migration Guide

### Updating Existing Code

If you have existing code using OpenWIG, no changes are required. The old singleton approach still works:

```java
// This still works
Engine.newInstance(cartridge, log, ui, gps);
Engine.instance.start();
```

### Adopting Parallel Execution

To add parallel execution support:

1. Replace `Engine.newInstance()` with `new Engine()`
2. Ensure each engine has its own UI and LocationService
3. Start engines on separate threads
4. Use the returned Engine instance instead of `Engine.instance`

## Example: Multi-Cartridge Game Manager

```java
public class MultiCartridgeManager {
    private final Map<String, Engine> engines = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newCachedThreadPool();
    
    public void startCartridge(String id, CartridgeFile cartridge, 
                              UI ui, LocationService gps, OutputStream log) throws IOException {
        executor.submit(() -> {
            try {
                Engine engine = new Engine(cartridge, log, ui, gps);
                engines.put(id, engine);
                engine.start();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                engines.remove(id);
            }
        });
    }
    
    public void stopCartridge(String id) {
        Engine engine = engines.get(id);
        if (engine != null) {
            // Stop the engine (implementation depends on engine state)
            engines.remove(id);
        }
    }
    
    public void shutdown() {
        executor.shutdown();
        try {
            executor.awaitTermination(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }
    }
}
```

## Limitations

- Engines on the same thread cannot run simultaneously (use separate threads)
- Static utility methods are provided for backward compatibility but may not work correctly when called from a thread without an engine context
- Some performance overhead from ThreadLocal lookups (minimal in practice)

## Testing

A complete test example is available in the test suite. See `ParallelCartridgeExecutionTest.java` for a working implementation.

## Support

For issues or questions about parallel execution, please open an issue on the GitHub repository.
