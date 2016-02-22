package cgeo.geocaching.network;

import cgeo.geocaching.utils.CryptUtils;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.NonNull;

public class OAuth {
    private OAuth() {
        // utility class
    }

    public static void signOAuth(final String host,
            final String path,
            final String method,
            final boolean https,
            final Parameters params,
            final OAuthTokens tokens,
            final String consumerKey,
            final String consumerSecret) {
        params.put(
                "oauth_consumer_key", consumerKey,
                "oauth_nonce", CryptUtils.md5(Long.toString(System.currentTimeMillis())),
                "oauth_signature_method", "HMAC-SHA1",
                "oauth_timestamp", Long.toString(System.currentTimeMillis() / 1000),
                "oauth_token", StringUtils.defaultString(tokens.getTokenPublic()),
                "oauth_version", "1.0");
        params.sort();

        final StringBuilder paramsEncodedBuilder = new StringBuilder();
        for (final NameValuePair nameValue : params) {
            paramsEncodedBuilder.append('&').append(percentEncode(nameValue.getName()))
                    .append('=').append(percentEncode(nameValue.getValue()));
        }
        final String paramsEncoded = paramsEncodedBuilder.substring(1);

        final String requestPacked = method + '&' + percentEncode((https ? "https" : "http") + "://" + host + path) + '&' +
                percentEncode(paramsEncoded);
        final String keysPacked = percentEncode(consumerSecret) + '&' + percentEncode(StringUtils.defaultString(tokens.getTokenSecret())); // both even if empty some of them!
        params.put("oauth_signature", CryptUtils.base64Encode(CryptUtils.hashHmac(requestPacked, keysPacked)));
    }

    /**
     * Percent encode following http://tools.ietf.org/html/rfc5849#section-3.6
     */
    static String percentEncode(@NonNull final String url) {
        return StringUtils.replace(Network.rfc3986URLEncode(url), "*", "%2A");
    }
}
