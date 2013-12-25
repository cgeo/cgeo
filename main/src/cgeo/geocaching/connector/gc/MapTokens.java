package cgeo.geocaching.connector.gc;

import android.util.Pair;

/**
 * Wrapper type to make map tokens more type safe than with a String array.
 * 
 */
public final class MapTokens extends Pair<String, String> {

    public MapTokens(String userSession, String sessionToken) {
        super(userSession, sessionToken);
    }

    public String getUserSession() {
        return first;
    }

    public String getSessionToken() {
        return second;
    }

}
