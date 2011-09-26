package cgeo.geocaching;

import cgeo.geocaching.utils.CryptUtils;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class cgOAuth {
    public static String signOAuth(String host, String path, String method, boolean https, Map<String, String> params, String token, String tokenSecret) {
        String paramsDone = "";
        if (method.equalsIgnoreCase("GET") == false && method.equalsIgnoreCase("POST") == false) {
            method = "POST";
        } else {
            method = method.toUpperCase();
        }

        if (token == null)
            token = "";
        if (tokenSecret == null)
            tokenSecret = "";

        long currentTime = new Date().getTime(); // miliseconds
        currentTime = currentTime / 1000; // seconds
        currentTime = (long) Math.floor(currentTime);

        params.put("oauth_consumer_key", cgSettings.keyConsumerPublic);
        params.put("oauth_nonce", CryptUtils.md5(Long.toString(System.currentTimeMillis())));
        params.put("oauth_signature_method", "HMAC-SHA1");
        params.put("oauth_timestamp", Long.toString(currentTime));
        params.put("oauth_token", token);
        params.put("oauth_version", "1.0");

        String[] keys = new String[params.keySet().size()];
        params.keySet().toArray(keys);
        Arrays.sort(keys);

        List<String> paramsEncoded = new ArrayList<String>();
        for (String key : keys) {
            String value = params.get(key);
            paramsEncoded.add(key + "=" + cgBase.urlencode_rfc3986(value));
        }

        String keysPacked;
        String requestPacked;

        keysPacked = cgSettings.keyConsumerSecret + "&" + tokenSecret; // both even if empty some of them!
        if (https)
            requestPacked = method + "&" + cgBase.urlencode_rfc3986("https://" + host + path) + "&" + cgBase.urlencode_rfc3986(StringUtils.join(paramsEncoded.toArray(), '&'));
        else
            requestPacked = method + "&" + cgBase.urlencode_rfc3986("http://" + host + path) + "&" + cgBase.urlencode_rfc3986(StringUtils.join(paramsEncoded.toArray(), '&'));
        paramsEncoded.add("oauth_signature=" + cgBase.urlencode_rfc3986(cgBase.base64Encode(CryptUtils.hashHmac(requestPacked, keysPacked))));

        paramsDone = StringUtils.join(paramsEncoded.toArray(), '&');

        return paramsDone;
    }
}
