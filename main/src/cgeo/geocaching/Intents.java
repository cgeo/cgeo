package cgeo.geocaching;

import cgeo.geocaching.enumerations.CacheListType;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.NonNull;

import android.content.Intent;
import android.os.Bundle;

public class Intents {

    private Intents() {
        // Do not instantiate
    }

    private static final String PREFIX = "cgeo.geocaching.intent.extra.";

    public static final String EXTRA_ADDRESS = PREFIX + "address";
    public static final String EXTRA_COORDS = PREFIX + "coords";
    public static final String EXTRA_LATITUDE = PREFIX + "latitude";
    public static final String EXTRA_LONGITUDE = PREFIX + "longitude";
    public static final String EXTRA_COUNT = PREFIX + "count";
    public static final String EXTRA_GEOCODE = PREFIX + "geocode";
    public static final String EXTRA_GUID = PREFIX + "guid";
    public static final String EXTRA_IMAGES = PREFIX + "images";
    public static final String EXTRA_ID = PREFIX + "id";
    public static final String EXTRA_KEYWORD = PREFIX + "keyword";
    public static final String EXTRA_KEYWORD_SEARCH = PREFIX + "keyword_search";
    public static final String EXTRA_LIST_ID = PREFIX + "list_id";
    public static final String EXTRA_CAPTION = PREFIX + "caption";
    public static final String EXTRA_DESCRIPTION = PREFIX + "description";
    public static final String EXTRA_URI_AS_STRING = PREFIX + "uri";
    public static final String EXTRA_SCALE = PREFIX + "scale";

    public static final String EXTRA_WPTTYPE = PREFIX + "wpttype";
    public static final String EXTRA_MAPSTATE = PREFIX + "mapstate";
    public static final String EXTRA_MAP_TITLE = PREFIX + "mapTitle";
    public static final String EXTRA_MAP_MODE = PREFIX + "mapMode";
    public static final String EXTRA_LIVE_ENABLED = PREFIX + "liveEnabled";

    public static final String EXTRA_DOWNLOAD = PREFIX + "download";

    /**
     * list type to be used with the cache list activity. Be aware to use the String representation of the corresponding
     * enum.
     */
    private static final String EXTRA_LIST_TYPE = PREFIX + "list_type";
    public static final String EXTRA_MAP_FILE = PREFIX + "map_file";
    public static final String EXTRA_NAME = PREFIX + "name";
    public static final String EXTRA_SEARCH = PREFIX + "search";
    public static final String EXTRA_START_DIR = PREFIX + "start_dir";
    public static final String EXTRA_TRACKING_CODE = PREFIX + "tracking_code";
    public static final String EXTRA_TYPE = PREFIX + "type";
    public static final String EXTRA_USERNAME = PREFIX + "username";
    public static final String EXTRA_WAYPOINT_ID = PREFIX + "waypoint_id";
    public static final String EXTRA_CACHELIST = PREFIX + "cache_list";
    public static final String EXTRA_POCKET_GUID = PREFIX + "pocket_guid";

    private static final String PREFIX_ACTION = "cgeo.geocaching.intent.action.";
    public static final String ACTION_GEOCACHE = PREFIX_ACTION + "GEOCACHE";
    public static final String ACTION_TRACKABLE = PREFIX_ACTION + "TRACKABLE";

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

    public static Intent putListType(final Intent intent, final @NonNull CacheListType listType) {
        intent.putExtra(Intents.EXTRA_LIST_TYPE, listType.name());
        return intent;
    }

    public static @NonNull CacheListType getListType(final Intent intent) {
        final Bundle extras = intent.getExtras();
        if (extras == null) {
            return CacheListType.OFFLINE;
        }
        final String typeName = extras.getString(Intents.EXTRA_LIST_TYPE);
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
