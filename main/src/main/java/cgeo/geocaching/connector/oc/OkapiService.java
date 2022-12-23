package cgeo.geocaching.connector.oc;

import cgeo.geocaching.connector.oc.OCApiConnector.OAuthLevel;

import androidx.annotation.NonNull;


enum OkapiService {
    SERVICE_CACHE("/okapi/services/caches/geocache", OAuthLevel.Level1),
    SERVICE_SEARCH_AND_RETRIEVE("/okapi/services/caches/shortcuts/search_and_retrieve", OAuthLevel.Level1),
    SERVICE_MARK_CACHE("/okapi/services/caches/mark", OAuthLevel.Level3),
    SERVICE_SUBMIT_LOG("/okapi/services/logs/submit", OAuthLevel.Level3),
    SERVICE_ADD_LOG_IMAGE("/okapi/services/logs/images/add", OAuthLevel.Level3),
    SERVICE_USER("/okapi/services/users/user", OAuthLevel.Level1),
    SERVICE_USER_BY_USERNAME("/okapi/services/users/by_username", OAuthLevel.Level1),
    SERVICE_USER_BY_USERID("/okapi/services/users/by_internal_id", OAuthLevel.Level1),
    SERVICE_UPLOAD_PERSONAL_NOTE("/okapi/services/caches/save_personal_notes", OAuthLevel.Level3),
    SERVICE_RESOLVE_URL("/okapi/services/caches/search/by_urls", OAuthLevel.Level1),
    SERVICE_API_INSTALLATION("/okapi/services/apisrv/installation", OAuthLevel.Level0),
    SERVICE_LOG_ENTRY("/okapi/services/logs/entry", OAuthLevel.Level1);

    @NonNull final String methodName;
    @NonNull final OAuthLevel level;

    OkapiService(@NonNull final String methodName, @NonNull final OAuthLevel level) {
        this.methodName = methodName;
        this.level = level;
    }

}
