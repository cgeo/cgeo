package cgeo.geocaching.network;

import cgeo.geocaching.utils.CryptUtils;

import org.apache.commons.lang3.StringUtils;

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
                "oauth_version", "1.0");
        if (StringUtils.isNotEmpty(tokens.getTokenPublic())) {
            params.put("oauth_token", StringUtils.defaultString(tokens.getTokenPublic()));
        }
        params.put("oauth_signature",
                signCompletedOAuth(host, path, method, https, params, tokens.getTokenSecret(), consumerSecret));
    }

    static String signCompletedOAuth(final String host, final String path, final String method, final boolean https,
                                     final Parameters params, final String tokenSecret, final String consumerSecret) {
        params.sort();

        // Twitter requires that the signature is generated from the raw data that is received in the query string.
        // Opencaching sites require that the signature is generated from the percent-encoded versions of the parameters.
        // As a consequence, we will always use percent-encoding for parameters during the OAuth signing process, which
        // satisfies both constraints.
        params.usePercentEncoding();

        final String requestPacked = method + '&' + Parameters.percentEncode((https ? "https" : "http") + "://" + host + path) + '&' +
                Parameters.percentEncode(params.toString());
        final String keysPacked = Parameters.percentEncode(consumerSecret) + '&' + Parameters.percentEncode(StringUtils.defaultString(tokenSecret)); // both even if empty some of them!
        return CryptUtils.base64Encode(CryptUtils.hashHmac(requestPacked, keysPacked));
    }

}
