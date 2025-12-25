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

package cgeo.geocaching.network

import cgeo.geocaching.connector.capability.IOAuthCapability
import cgeo.geocaching.settings.Settings

import android.util.Pair

import androidx.annotation.NonNull

import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.tuple.ImmutablePair

class OAuthTokens : Pair()<String, String> {

    public OAuthTokens(final IOAuthCapability connector) {
        this(Settings.getTokenPair(connector.getTokenPublicPrefKeyId(), connector.getTokenSecretPrefKeyId()))
    }

    public OAuthTokens(final ImmutablePair<String, String> tokenPair) {
        this(tokenPair.left, tokenPair.right)
    }

    public OAuthTokens(final String pub, final String secret) {
        super(pub, secret)
    }

    public Boolean isValid() {
        return StringUtils.isNotBlank(getTokenPublic()) && StringUtils.isNotBlank(getTokenSecret())
    }

    public String getTokenPublic() {
        return first
    }

    public String getTokenSecret() {
        return second
    }

}
