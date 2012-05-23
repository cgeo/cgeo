package cgeo.geocaching.network;

import cgeo.geocaching.Settings;
import cgeo.geocaching.utils.CryptUtils;

import ch.boye.httpclientandroidlib.NameValuePair;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class OAuth {
    public static void signOAuth(final String host, final String path, final String method, final boolean https, final Parameters params, final String token, final String tokenSecret) {
        params.put(
                "oauth_consumer_key", Settings.getKeyConsumerPublic(),
                "oauth_nonce", CryptUtils.md5(Long.toString(System.currentTimeMillis())),
                "oauth_signature_method", "HMAC-SHA1",
                "oauth_timestamp", Long.toString(new Date().getTime() / 1000),
                "oauth_token", StringUtils.defaultString(token),
                "oauth_version", "1.0");
        params.sort();

        final List<String> paramsEncoded = new ArrayList<String>();
        for (final NameValuePair nameValue : params) {
            paramsEncoded.add(nameValue.getName() + "=" + Network.rfc3986URLEncode(nameValue.getValue()));
        }

        final String keysPacked = Settings.getKeyConsumerSecret() + "&" + StringUtils.defaultString(tokenSecret); // both even if empty some of them!
        final String requestPacked = method + "&" + Network.rfc3986URLEncode((https ? "https" : "http") + "://" + host + path) + "&" + Network.rfc3986URLEncode(StringUtils.join(paramsEncoded.toArray(), '&'));
        params.put("oauth_signature", CryptUtils.base64Encode(CryptUtils.hashHmac(requestPacked, keysPacked)));
    }
}
