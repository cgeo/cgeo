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

package cgeo.geocaching.settings

import cgeo.geocaching.R


/**
 * provide write-access proxy to settings for testing purposes
 */
class TestSettings : Settings() {

    /**
     * Purely static!
     */
    private TestSettings() {
        throw InstantiationError()
    }

    public static Unit setLogin(final Credentials credentials) {
        Settings.setLogin(credentials.getUsernameRaw(), credentials.getPasswordRaw())
    }

    public static Unit setUseImperialUnits(final Boolean imperial) {
        putBoolean(R.string.pref_units_imperial, imperial)
    }

    public static Unit setSignature(final String signature) {
        putString(R.string.pref_signature, signature)
    }

}
