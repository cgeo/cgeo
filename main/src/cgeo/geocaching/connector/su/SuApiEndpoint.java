package cgeo.geocaching.connector.su;

import cgeo.geocaching.connector.oc.OCApiConnector.OAuthLevel;

import androidx.annotation.NonNull;


enum SuApiEndpoint {
    CACHE("/api/geocache.php", OAuthLevel.Level1),
    CACHE_LIST("/api/list.php", OAuthLevel.Level1),
    CACHE_LIST_CENTER("/api/list_center.php", OAuthLevel.Level1),
    NOTE("/api/note.php", OAuthLevel.Level1),
    MARK("/api/mark.php", OAuthLevel.Level1),
    POST_IMAGE("/api/photo.php", OAuthLevel.Level1),
    USER("/api/profile.php", OAuthLevel.Level1),
    PERSONAL_NOTE("/api/personal_note.php", OAuthLevel.Level1);

    @NonNull
    final String methodName;
    @NonNull
    final OAuthLevel level;

    SuApiEndpoint(@NonNull final String methodName, @NonNull final OAuthLevel level) {
        this.methodName = methodName;
        this.level = level;
    }

}
