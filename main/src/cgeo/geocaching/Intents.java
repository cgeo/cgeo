package cgeo.geocaching;

import cgeo.geocaching.enumerations.CacheListType;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;

import org.apache.commons.lang3.StringUtils;

public class Intents {

    public static final int SETTINGS_ACTIVITY_REQUEST_CODE = 1;
    public static final int SEARCH_REQUEST_CODE = 2;

    private static final String PREFIX = "cgeo.geocaching.intent.extra.";

    public static final String EXTRA_ADDRESS = PREFIX + "address";
    public static final String EXTRA_COORDS = PREFIX + "coords";
    public static final String EXTRA_GEOCODE = PREFIX + "geocode";
    public static final String EXTRA_GEOCACHE = PREFIX + "geocache";
    public static final String EXTRA_GUID = PREFIX + "guid";
    public static final String EXTRA_BRAND = PREFIX + "brand";
    public static final String EXTRA_IMAGE = PREFIX + "image";
    public static final String EXTRA_INDEX = PREFIX + "index";
    public static final String EXTRA_DELETE_FLAG = PREFIX + "deleteflag";
    public static final String EXTRA_IMAGES = PREFIX + "images";
    public static final String EXTRA_MAX_IMAGE_UPLOAD_SIZE = PREFIX + "max-image-upload-size";
    public static final String EXTRA_IMAGE_CAPTION_MANDATORY = PREFIX + "image-caption-mandatory";
    public static final String EXTRA_ID = PREFIX + "id";
    public static final String EXTRA_KEYWORD = PREFIX + "keyword";
    public static final String EXTRA_KEYWORD_SEARCH = PREFIX + "keyword_search";
    public static final String EXTRA_LIST_ID = PREFIX + "list_id";
    public static final String EXTRA_PQ_LIST_IMPORT = PREFIX + "pq_list_import";
    public static final String EXTRA_COORD_DESCRIPTION = PREFIX + "coord_description";
    public static final String EXTRA_WPT_PAGE_UPDATE = PREFIX + "wpt_page_update";
    public static final String EXTRA_FILTER_CONTEXT = "filter_context";

    public static final String EXTRA_WPTTYPE = PREFIX + "wpttype";
    public static final String EXTRA_MAPSTATE = PREFIX + "mapstate";
    public static final String EXTRA_TITLE = PREFIX + "title";
    public static final String EXTRA_MAP_MODE = PREFIX + "mapMode";
    public static final String EXTRA_LIVE_ENABLED = PREFIX + "liveEnabled";
    public static final String EXTRA_STORED_ENABLED = PREFIX + "storedEnabled";

    public static final String EXTRA_TARGET_INFO = PREFIX + "targetInfo";
    /**
     * list type to be used with the cache list activity. Be aware to use the String representation of the corresponding
     * enum.
     */
    private static final String EXTRA_LIST_TYPE = PREFIX + "list_type";
    public static final String EXTRA_NAME = PREFIX + "name";
    public static final String EXTRA_FILENAME = "filename";
    public static final String EXTRA_SEARCH = PREFIX + "search";
    public static final String EXTRA_TRACKING_CODE = PREFIX + "tracking_code";
    public static final String EXTRA_USERNAME = PREFIX + "username";
    public static final String EXTRA_WAYPOINT_ID = PREFIX + "waypoint_id";
    public static final String EXTRA_POCKET_GUID = PREFIX + "pocket_guid";

    private static final String PREFIX_ACTION = "cgeo.geocaching.intent.action.";
    public static final String ACTION_GEOCACHE = PREFIX_ACTION + "GEOCACHE";
    public static final String ACTION_TRACKABLE = PREFIX_ACTION + "TRACKABLE";
    public static final String ACTION_SETTINGS = PREFIX_ACTION + "SETTINGS";
    public static final String ACTION_GEOCACHE_CHANGED = PREFIX_ACTION + "GEOCACHE_CHANGED";

    private static final String PREFIX_OAUTH = "cgeo.geocaching.intent.oauth.";
    public static final String EXTRA_OAUTH_HOST = PREFIX_OAUTH + "host";
    public static final String EXTRA_OAUTH_PATH_REQUEST = PREFIX_OAUTH + "request";
    public static final String EXTRA_OAUTH_PATH_AUTHORIZE = PREFIX_OAUTH + "authorize";
    public static final String EXTRA_OAUTH_PATH_ACCESS = PREFIX_OAUTH + "access";
    public static final String EXTRA_OAUTH_HTTPS = PREFIX_OAUTH + "https";
    public static final String EXTRA_OAUTH_CONSUMER_KEY = PREFIX_OAUTH + "ConsumerKey";
    public static final String EXTRA_OAUTH_CONSUMER_SECRET = PREFIX_OAUTH + "ConsumerSecret";
    public static final String EXTRA_OAUTH_CALLBACK = PREFIX_OAUTH + "callback";

    public static final String EXTRA_OAUTH_TITLE_RES_ID = PREFIX_OAUTH + "titleresId";
    public static final String EXTRA_OAUTH_TEMP_TOKEN_KEY_PREF = PREFIX_OAUTH + "tempKeyPref";
    public static final String EXTRA_OAUTH_TEMP_TOKEN_SECRET_PREF = PREFIX_OAUTH + "tempSecretPref";
    public static final String EXTRA_OAUTH_TOKEN_PUBLIC_KEY = PREFIX_OAUTH + "publicTokenPref";
    public static final String EXTRA_OAUTH_TOKEN_SECRET_KEY = PREFIX_OAUTH + "secretTokenPref";

    private static final String PREFIX_TOKEN_AUTH = "cgeo.geocaching.intent.tokenauth.";
    public static final String EXTRA_TOKEN_AUTH_URL_TOKEN = PREFIX_TOKEN_AUTH + "token";
    public static final String EXTRA_TOKEN_AUTH_USERNAME = PREFIX_TOKEN_AUTH + "username";
    public static final String EXTRA_TOKEN_AUTH_PASSWORD = PREFIX_TOKEN_AUTH + "password";

    private static final String PREFIX_CREDENTIALS_AUTH = "cgeo.geocaching.intent.credentialsauth.";
    public static final String EXTRA_CREDENTIALS_AUTH_USERNAME = PREFIX_CREDENTIALS_AUTH + "username";
    public static final String EXTRA_CREDENTIALS_AUTH_PASSWORD = PREFIX_CREDENTIALS_AUTH + "password";

    /**
     * To be used together with {@link android.content.Intent#ACTION_OPEN_DOCUMENT_TREE}
     *
     * The value is decide whether to show advance mode or not.
     * If the value is true, the local/device storage root must be
     * visible in DocumentsUI. Otherwise it depends on the users preference.
     *
     * This is a system internal Api, which is not officially supported and not accessible from outside. Therefore, we need to define it ourselves.
     * Anyway, the usage of it should be uncritical. The worst thing which could happen is that the EXTRA is simply ignored.
     */
    public static final String EXTRA_SHOW_ADVANCED = "android.provider.extra.SHOW_ADVANCED";

    private Intents() {
        // Do not instantiate
    }

    public static Intent putListType(final Intent intent, @NonNull final CacheListType listType) {
        intent.putExtra(EXTRA_LIST_TYPE, listType.name());
        return intent;
    }

    @NonNull
    public static CacheListType getListType(final Intent intent) {
        final Bundle extras = intent.getExtras();
        if (extras == null) {
            return CacheListType.OFFLINE;
        }
        final String typeName = extras.getString(EXTRA_LIST_TYPE);
        if (StringUtils.isBlank(typeName)) {
            return CacheListType.OFFLINE;
        }
        try {
            return CacheListType.valueOf(typeName);
        } catch (final IllegalArgumentException ignored) {
            return CacheListType.OFFLINE;
        }
    }
}
