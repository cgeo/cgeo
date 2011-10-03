package cgeo.geocaching;

import cgeo.geocaching.utils.CryptUtils;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.NameValuePair;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class cgOAuth {
    public static String signOAuth(String host, String path, String method, boolean https, Parameters params, String token, String tokenSecret) {
        String paramsDone = "";

        long currentTime = new Date().getTime(); // miliseconds
        currentTime = currentTime / 1000; // seconds
        currentTime = (long) Math.floor(currentTime);

        params.put("oauth_consumer_key", Settings.getKeyConsumerPublic());
        params.put("oauth_nonce", CryptUtils.md5(Long.toString(System.currentTimeMillis())));
        params.put("oauth_signature_method", "HMAC-SHA1");
        params.put("oauth_timestamp", Long.toString(currentTime));
        params.put("oauth_token", StringUtils.defaultString(token));
        params.put("oauth_version", "1.0");

        params.sort();

        List<String> paramsEncoded = new ArrayList<String>();
        for (final NameValuePair nameValue : params) {
            paramsEncoded.add(nameValue.getName() + "=" + cgBase.urlencode_rfc3986(nameValue.getValue()));
        }

        final String keysPacked = Settings.getKeyConsumerSecret() + "&" + StringUtils.defaultString(tokenSecret); // both even if empty some of them!
        final String requestPacked = method + "&" + cgBase.urlencode_rfc3986((https ? "https" : "http") + "://" + host + path) + "&" + cgBase.urlencode_rfc3986(StringUtils.join(paramsEncoded.toArray(), '&'));
        paramsEncoded.add("oauth_signature=" + cgBase.urlencode_rfc3986(cgBase.base64Encode(CryptUtils.hashHmac(requestPacked, keysPacked))));

        paramsDone = StringUtils.join(paramsEncoded.toArray(), '&');

        return paramsDone;
    }
}
