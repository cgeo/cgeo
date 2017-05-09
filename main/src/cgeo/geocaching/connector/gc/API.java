package cgeo.geocaching.connector.gc;

import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.network.Network;
import cgeo.geocaching.network.Parameters;
import cgeo.geocaching.utils.AndroidRxUtils;

import android.support.annotation.NonNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.reactivex.Single;
import io.reactivex.SingleSource;
import io.reactivex.functions.Function;
import org.apache.commons.lang3.StringUtils;

class API {

    private static final Object cacheLock = new Object();
    private static final String API_URL = "https://www.geocaching.com/api/proxy/web/v1";
    private static Authorization cachedAuthorization;
    private static long cachedAuthorizationExpires;

    private API() {
        // Utility class, do not instantiate
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static final class Authorization {
        String token_type;
        String access_token;
        long expires_in;      // In seconds

        String getAuthorizationField() {
            return token_type + ' ' + access_token;
        }
    }

    private static Single<Authorization> getAuthorization() {
        return Network.getRequest("https://www.geocaching.com/account/oauth/token", Authorization.class, null, null);
    }

    private static Single<Authorization> getCachedAuthorization() {
        synchronized (cacheLock) {
            if (System.currentTimeMillis() < cachedAuthorizationExpires) {
                return Single.just(cachedAuthorization);
            }
            // We may request several authorizations at the same time. This is not a big deal, and the web
            // implementation does this much more than we will ever do.
            return getAuthorization().map(new Function<Authorization, Authorization>() {
                @Override
                public Authorization apply(@NonNull final Authorization authorization) throws Exception {
                    synchronized (cacheLock) {
                        cachedAuthorization = authorization;
                        // Expires after .8 of authorized caching time.
                        cachedAuthorizationExpires = System.currentTimeMillis() + authorization.expires_in * 800;
                        return cachedAuthorization;
                    }
                }
            });
        }
    }

    static Single<Parameters> getAuthorizationHeader() {
        return getCachedAuthorization().map(new Function<Authorization, Parameters>() {
            @Override
            public Parameters apply(@NonNull final Authorization authorization) throws Exception {
                return new Parameters("Authorization", authorization.getAuthorizationField());
            }
        });
    }

    /*
    {"id":6189730,"referenceCode":"GC74HPM","postedCoordinates":{"latitude":48.818817,"longitude":2.337833},
    "callerSpecific":{"favorited":false},
    "owner":{"id":15646120,"referenceCode":"PRHBZWF"},
    "geocacheType":{"id":3,"name":"Multi-cache"}}
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    static final class CacheDetails {
        long id;
        String referenceCode;
        PostedCoordinates postedCoordinates;
        CallerSpecific callerSpecific;
        Owner owner;
        GeocacheType geocacheType;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static final class Owner {
        long id;
        String referenceCode;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static final class GeocacheType {
        long id;
        String name;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static final class PostedCoordinates {
        double latitude;
        double longitude;

        Geopoint toCoords() {
            return new Geopoint(latitude, longitude);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static final class CallerSpecific {
        boolean favorited;
    }

    private static <T> Single<T> getAPI(final String path, final Class<T> clazz) {
        return getAuthorizationHeader().flatMap(new Function<Parameters, SingleSource<T>>() {
            @Override
            public SingleSource<T> apply(@NonNull final Parameters headers) throws Exception {
                return Network.getRequest(API_URL + path, clazz, null, headers).subscribeOn(AndroidRxUtils.networkScheduler);
            }
        });
    }

    static Single<CacheDetails> getCacheDetails(final String geocode) {
        return getAPI("/geocache/" + StringUtils.lowerCase(geocode), CacheDetails.class);
    }

}
