package cgeo.geocaching.utils.offlinetranslate;

import cgeo.geocaching.utils.Log;

public class TranslateAccessor {

    private static final ITranslateAccessor INSTANCE;
    private static final boolean DO_TEST = false;

    static {
        ITranslateAccessor instance = null;
        if (DO_TEST) {
            instance = new DevTranslateAccessor();
        } else {
            try {
                final Class<ITranslateAccessor> mlkitClass = (Class<ITranslateAccessor>)
                        Class.forName("cgeo.geocaching.utils.offlinetranslate.MLKitTranslateAccessor");
                if (mlkitClass != null) {
                    instance = mlkitClass.newInstance();
                }
            } catch (Exception re) {
                //mlkit not found
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
