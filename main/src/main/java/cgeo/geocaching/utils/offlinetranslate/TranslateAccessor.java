package cgeo.geocaching.utils.offlinetranslate;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.utils.Log;

public class TranslateAccessor {

    private static final ITranslateAccessor INSTANCE;

    static {
        ITranslateAccessor instance = null;
        // Try Bergamot first (open-source, works in all builds including FOSS)
        try {
            instance = new BergamotTranslateAccessor(CgeoApplication.getInstance());
            Log.iForce("TranslateAccessor: Bergamot instance created");
        } catch (Exception e) {
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
            } catch (Exception re) {
                Log.iForce("TranslateAccessor: Could not find MLKit");
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
