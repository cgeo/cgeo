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

package cgeo.geocaching.connector.capability


import cgeo.geocaching.settings.Credentials

/**
 * Marker interface for connectors which have user name and password as credentials.
 */
interface ICredentials {
    Credentials getCredentials()

    /**
     * Get preference key of the user name.
     */
    Int getUsernamePreferenceKey()

    /**
     * Get preference key of the password.
     */
    Int getPasswordPreferenceKey()

}
