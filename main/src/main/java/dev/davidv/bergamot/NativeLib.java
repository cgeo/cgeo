package dev.davidv.bergamot;

/**
 * JNI bindings for libbergamot-sys.so
 * Package name must match the JNI export symbols in bergamot.cpp:
 * Java_dev_davidv_bergamot_NativeLib_*
 * We load the same .so but map it into cgeo's package via a shim approach.
 */
@SuppressWarnings("PMD.AvoidUsingNativeCode")
public class NativeLib {

    static {
        System.loadLibrary("bergamot-sys");
    }

    /**
     * Bootstraps the Bergamot translation engine.
     * Must be called exactly once before any {@link #loadModelIntoCache} or {@link #translateMultiple} calls.
     */
    public native void initializeService();

    public native void loadModelIntoCache(String config, String key);

    public native String[] translateMultiple(String[] inputs, String key);

    public native String[] pivotMultiple(String firstKey, String secondKey, String[] inputs);

    public native void cleanup();
}
