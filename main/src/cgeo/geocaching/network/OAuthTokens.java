package cgeo.geocaching.network;

import cgeo.geocaching.connector.oc.OCApiConnector;
import cgeo.geocaching.settings.Settings;

import android.support.annotation.NonNull;
import android.util.Pair;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;

public class OAuthTokens extends Pair<String, String> {

    public OAuthTokens(@NonNull final OCApiConnector connector) {
        this(Settings.getTokenPair(connector.getTokenPublicPrefKeyId(), connector.getTokenSecretPrefKeyId()));
    }

    public OAuthTokens(final ImmutablePair<String, String> tokenPair) {
        this(tokenPair.left, tokenPair.right);
    }

    public OAuthTokens(final String pub, final String secret) {
        super(pub, secret);
    }

    public boolean isValid() {
        return StringUtils.isNotBlank(getTokenPublic()) && StringUtils.isNotBlank(getTokenSecret());
    }

    public String getTokenPublic() {
        return first;
    }

    public String getTokenSecret() {
        return second;
    }

}
