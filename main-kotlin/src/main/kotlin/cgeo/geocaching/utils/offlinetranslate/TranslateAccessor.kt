// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.utils.offlinetranslate

import cgeo.geocaching.utils.Log

class TranslateAccessor {

    private static final ITranslateAccessor INSTANCE
    private static val DO_TEST: Boolean = false

    static {
        ITranslateAccessor instance = null
        if (DO_TEST) {
            instance = DevTranslateAccessor()
        } else {
            try {
                val mlkitClass: Class<ITranslateAccessor> = (Class<ITranslateAccessor>)
                        Class.forName("cgeo.geocaching.utils.offlinetranslate.MLKitTranslateAccessor")
                if (mlkitClass != null) {
                    instance = mlkitClass.newInstance()
                    Log.iForce("TranslateAccessor: MLKit instance created")
                } else {
                    Log.iForce("TranslateAccessor: MLKit class not found")
                }
            } catch (Exception re) {
                //mlkit not found
                Log.iForce("TranslateAccessor: Could not find MLKit")
            }
        }
        INSTANCE = instance == null ? NoopTranslateAccessor() : instance
    }

    private TranslateAccessor() {
        //no instances
    }

    public static ITranslateAccessor get() {
        return INSTANCE
    }

}
