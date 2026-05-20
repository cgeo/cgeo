package cgeo.geocaching.utils.offlinetranslate;

import cgeo.geocaching.utils.Log;

public class TranslateAccessor {

    private static final ITranslateAccessor INSTANCE;
    // Set to true to use the DevTranslateAccessor stub (simulates translation without real backend)
    private static final boolean DO_TEST = false;

    static {
        ITranslateAccessor instance = null;
        if (DO_TEST) {
            instance = new DevTranslateAccessor();
        } else {
            // Bergamot is temporarily disabled due to native-crash issues (see #18112).
            // Its JNI calls (e.g. language detection) can throw java.lang.Error subtypes
            // that are not caught by the surrounding catch(Exception) blocks, which lets the
            // error propagate through RxJava to the uncaught-exception handler and crash the
            // app. Re-enable (and restore the catch→Throwable fixes in BergamotTranslateAccessor)
            // once the root cause in the native library is identified and resolved.
            Log.iForce("TranslateAccessor: Bergamot temporarily disabled (#18112)");
            // Fall back to MLKit if Bergamot failed (e.g. .so not bundled yet)
            if (instance == null) {
                try {
                    final Class<ITranslateAccessor> mlkitClass = (Class<ITranslateAccessor>)
                            Class.forName("cgeo.geocaching.utils.offlinetranslate.MLKitTranslateAccessor");
                    if (mlkitClass != null) {
                        instance = mlkitClass.newInstance();
                        Log.iForce("TranslateAccessor: MLKit instance created (fallback)");
                    }
                } catch (final Exception re) {
                    Log.iForce("TranslateAccessor: Could not find MLKit");
                }
            }
        }
        INSTANCE = instance == null ? new NoopTranslateAccessor() : instance;
    }

    private TranslateAccessor() {
        //no instances
    }

    public static ITranslateAccessor get() {
        return INSTANCE;
    }

}
