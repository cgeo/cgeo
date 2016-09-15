package cgeo.geocaching.connector.gc;

import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.utils.AndroidRxUtils;

import android.util.Pair;

import java.util.concurrent.Callable;

import io.reactivex.Observable;
import org.apache.commons.lang3.StringUtils;

/**
 * Wrapper type to make map tokens more type safe than with a String array.
 *
 */
public final class MapTokens extends Pair<String, String> {

    public static final MapTokens INVALID_TOKENS = new MapTokens(null, null);

    MapTokens(final String userSession, final String sessionToken) {
        super(userSession, sessionToken);
    }

    public String getUserSession() {
        return first;
    }

    public String getSessionToken() {
        return second;
    }

    /**
     * Check if the tokens are valid.
     *
     * @return <tt>true</tt> if the tokens are valid
     */
    public boolean valid() {
        return !StringUtils.isEmpty(getUserSession()) && !StringUtils.isEmpty(getSessionToken());
    }

    /**
     * Return an infinite stream of map tokens. If the tokens are valid, they are not queried again from the web site.
     * Otherwise, they are requested until they become valid in case connectivity with geocaching.com is restored.
     *
     * @return an infinite stream of map tokens, which stay identical as soon as they are valid
     */
    public static Observable<MapTokens> retrieveMapTokens() {
        if (Settings.isGCConnectorActive()) {
            return Observable.defer(new Callable<Observable<MapTokens>>() {
                @Override
                public Observable<MapTokens> call() {
                    final MapTokens tokens = GCLogin.getInstance().getMapTokens();
                    if (tokens.valid()) {
                        return Observable.just(tokens).repeat();
                    } else {
                        return Observable.just(tokens).concatWith(retrieveMapTokens());
                    }
                }
            }).subscribeOn(AndroidRxUtils.networkScheduler);
        } else {
            return Observable.just(INVALID_TOKENS).repeat();
        }
    }

}
