package cgeo.geocaching.network;

import cgeo.geocaching.utils.CryptUtils;

import ch.boye.httpclientandroidlib.NameValuePair;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.NonNull;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class OAuth {
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
                "oauth_timestamp", Long.toString(new Date().getTime() / 1000),
                "oauth_token", StringUtils.defaultString(tokens.getTokenPublic()),
                "oauth_version", "1.0");
        params.sort();

        final List<String> paramsEncoded = new ArrayList<>();
        for (final NameValuePair nameValue : params) {
            paramsEncoded.add(nameValue.getName() + "=" + OAuth.percentEncode(nameValue.getValue()));
        }

        final String keysPacked = consumerSecret + "&" + StringUtils.defaultString(tokens.getTokenSecret()); // both even if empty some of them!
        final @NonNull String joinedParams = StringUtils.join(paramsEncoded.toArray(), '&');
        final String requestPacked = method + "&" + OAuth.percentEncode((https ? "https" : "http") + "://" + host + path) + "&" + OAuth.percentEncode(joinedParams);
        params.put("oauth_signature", CryptUtils.base64Encode(CryptUtils.hashHmac(requestPacked, keysPacked)));
    }

    /**
     * percent encode following http://tools.ietf.org/html/rfc5849#section-3.6
     *
     * @param url
     * @return
     */
    static String percentEncode(@NonNull final String url) {
        return StringUtils.replace(Network.rfc3986URLEncode(url), "*", "%2A");
    }
}
