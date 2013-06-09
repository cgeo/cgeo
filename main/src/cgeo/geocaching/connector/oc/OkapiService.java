package cgeo.geocaching.connector.oc;

import cgeo.geocaching.connector.oc.OCApiConnector.OAuthLevel;


enum OkapiService {
    SERVICE_CACHE("/okapi/services/caches/geocache", OAuthLevel.Level1),
    SERVICE_SEARCH_AND_RETRIEVE("/okapi/services/caches/shortcuts/search_and_retrieve", OAuthLevel.Level1),
    SERVICE_MARK_CACHE("/okapi/services/caches/mark", OAuthLevel.Level3),
    SERVICE_SUBMIT_LOG("/okapi/services/logs/submit", OAuthLevel.Level3),
    SERVICE_USER("/okapi/services/users/user", OAuthLevel.Level1);

    final String methodName;
    final OAuthLevel level;

    OkapiService(final String methodName, final OAuthLevel level) {
        this.methodName = methodName;
        this.level = level;
    }

}
