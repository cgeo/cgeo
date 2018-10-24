package cgeo.geocaching.connector.su;

import cgeo.geocaching.connector.oc.OCApiConnector.OAuthLevel;

import android.support.annotation.NonNull;


enum SuApiEndpoint {
    CACHE("/api/geocache.php", OAuthLevel.Level1),
    CACHE_LIST("/api/list.php", OAuthLevel.Level1),
    NOTE("/api/note.php", OAuthLevel.Level1);

    @NonNull
    final String methodName;
    @NonNull
    final OAuthLevel level;

    SuApiEndpoint(@NonNull final String methodName, @NonNull final OAuthLevel level) {
        this.methodName = methodName;
        this.level = level;
    }

}
