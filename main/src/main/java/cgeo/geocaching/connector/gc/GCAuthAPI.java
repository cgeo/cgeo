package cgeo.geocaching.connector.gc;

import cgeo.geocaching.network.HttpRequest;
import cgeo.geocaching.network.Network;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.reactivex.rxjava3.core.Single;

/** Base methods to issue OAuth authenticated HTTP requests against gc.com */
public final class GCAuthAPI {

    private static final Object CACHE_LOCK = new Object();
    public static final String WEBSITE_URL = "https://www.geocaching.com";
    public static final String API_PROXY_URL = WEBSITE_URL + "/api/proxy";

    private static Authorization cachedAuthorization;
    private static long cachedAuthorizationExpires;


    private GCAuthAPI() {
        //utility class, no instances
    }

    public static HttpRequest httpReq() {
        return new HttpRequest().requestPreparer(reqBuilder -> getCachedAuthorization().map(a -> {
            reqBuilder.addHeader("Authorization", a.getAuthorizationField());
            return reqBuilder;
        }));
    }

    public static HttpRequest websiteReq() {
        return httpReq().uriBase(WEBSITE_URL);
    }


    public static HttpRequest apiProxyReq() {
        return httpReq().uriBase(API_PROXY_URL);
    }

    public static void triggerAuthenticationTokenRetrieval() {
        getCachedAuthorization().subscribe();
    }

    private static Single<Authorization> getAuthorization() {
        return Network.getRequest("https://www.geocaching.com/account/oauth/token", Authorization.class, null, null);
    }

    private static Single<Authorization> getCachedAuthorization() {
        synchronized (CACHE_LOCK) {
            if (System.currentTimeMillis() < cachedAuthorizationExpires) {
                return Single.just(cachedAuthorization);
            }
            // We may request several authorizations at the same time. This is not a big deal, and the web
            // implementation does this much more than we will ever do.
            return getAuthorization().map(authorization -> {
                synchronized (CACHE_LOCK) {
                    cachedAuthorization = authorization;
                    // Expires after .8 of authorized caching time.
                    cachedAuthorizationExpires = System.currentTimeMillis() + authorization.expiresIn * 800;
                    return cachedAuthorization;
                }
            });
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static final class Authorization {
        @JsonProperty("token_type")
        String tokenType;
        @JsonProperty("access_token")
        String accessToken;
        @JsonProperty("expires_in")
        long expiresIn;      // In seconds

        String getAuthorizationField() {
            return tokenType + ' ' + accessToken;
        }
    }
}
