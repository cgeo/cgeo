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
            // Try Bergamot first (open-source, works in all builds including FOSS)
            try {
                instance = new BergamotTranslateAccessor();
                Log.iForce("TranslateAccessor: Bergamot instance created");
            } catch (final Throwable e) {
                // Catch Error (e.g. UnsatisfiedLinkError when .so is missing for this ABI)
                // as well as Exception, so the app can fall back gracefully.
                Log.iForce("TranslateAccessor: Could not initialize Bergamot: " + e.getMessage());
            }
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
