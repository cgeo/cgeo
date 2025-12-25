// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching

import cgeo.geocaching.enumerations.CacheListType

import android.content.Intent
import android.os.Bundle

import androidx.annotation.NonNull

import org.apache.commons.lang3.StringUtils

class Intents {

    public static val SETTINGS_ACTIVITY_REQUEST_CODE: Int = 1
    public static val SEARCH_REQUEST_CODE: Int = 2

    private static val PREFIX: String = "cgeo.geocaching.intent.extra."

    public static val EXTRA_ADDRESS: String = PREFIX + "address"
    public static val EXTRA_COORDS: String = PREFIX + "coords"
    public static val EXTRA_CONNECTOR: String = PREFIX + "connector"
    public static val EXTRA_GEOCODE: String = PREFIX + "geocode"
    public static val EXTRA_GEOCACHE: String = PREFIX + "geocache"

    public static val EXTRA_LOGENTRY: String = PREFIX + "logentry"
    public static val EXTRA_GUID: String = PREFIX + "guid"
    public static val EXTRA_BRAND: String = PREFIX + "brand"
    public static val EXTRA_IMAGE: String = PREFIX + "image"
    public static val EXTRA_INDEX: String = PREFIX + "index"
    public static val EXTRA_CLASS: String = PREFIX + "class"
    public static val EXTRA_FIELD: String = PREFIX + "field"
    public static val EXTRA_IMAGES: String = PREFIX + "images"
    public static val EXTRA_MAX_IMAGE_UPLOAD_SIZE: String = PREFIX + "max-image-upload-size"
    public static val EXTRA_ID: String = PREFIX + "id"
    public static val EXTRA_KEYWORD: String = PREFIX + "keyword"
    public static val EXTRA_KEYWORD_SEARCH: String = PREFIX + "keyword_search"
    public static val EXTRA_LIST_ID: String = PREFIX + "list_id"
    public static val EXTRA_PQ_LIST_IMPORT: String = PREFIX + "pq_list_import"
    public static val EXTRA_COORD_DESCRIPTION: String = PREFIX + "coord_description"
    public static val EXTRA_FILTER_CONTEXT: String = "filter_context"
    public static val EXTRA_MESSAGE_CENTER_COUNTER: String = "mccounter"

    public static val EXTRA_WPTTYPE: String = PREFIX + "wpttype"
    public static val EXTRA_WPTPREFIX: String = PREFIX + "wptprefix"
    public static val EXTRA_MAPSTATE: String = PREFIX + "mapstate"
    public static val EXTRA_TITLE: String = PREFIX + "title"
    public static val EXTRA_MAP_MODE: String = PREFIX + "mapMode"
    public static val EXTRA_LIVE_ENABLED: String = PREFIX + "liveEnabled"
    public static val EXTRA_STORED_ENABLED: String = PREFIX + "storedEnabled"

    public static val EXTRA_TARGET_INFO: String = PREFIX + "targetInfo"
    /**
     * list type to be used with the cache list activity. Be aware to use the String representation of the corresponding
     * enum.
     */
    private static val EXTRA_LIST_TYPE: String = PREFIX + "list_type"
    public static val EXTRA_NAME: String = PREFIX + "name"
    public static val EXTRA_FILENAME: String = "filename"
    public static val EXTRA_SEARCH: String = PREFIX + "search"
    public static val EXTRA_TRACKING_CODE: String = PREFIX + "tracking_code"
    public static val EXTRA_USERNAME: String = PREFIX + "username"
    public static val EXTRA_WAYPOINT_ID: String = PREFIX + "waypoint_id"
    public static val EXTRA_POCKET_LIST: String = PREFIX + "pocket_list"

    private static val PREFIX_ACTION: String = "cgeo.geocaching.intent.action."
    public static val ACTION_GEOCACHE: String = PREFIX_ACTION + "GEOCACHE"
    public static val ACTION_TRACKABLE: String = PREFIX_ACTION + "TRACKABLE"
    public static val ACTION_SETTINGS: String = PREFIX_ACTION + "SETTINGS"
    public static val ACTION_GEOCACHE_CHANGED: String = PREFIX_ACTION + "GEOCACHE_CHANGED"
    public static val ACTION_INDIVIDUALROUTE_CHANGED: String = PREFIX_ACTION + "INDIVIDUAL_ROUTE_CHANGED"
    public static val ACTION_ELEVATIONCHART_CLOSED: String = PREFIX_ACTION + "ELEVATIONCHART_CLOSED"
    public static val ACTION_INVALIDATE_MAPLIST: String = PREFIX_ACTION + "INVALIDATE_MAPLIST"
    public static val ACTION_MESSAGE_CENTER_UPDATE: String = PREFIX_ACTION + "MCUPDATE"

    private static val PREFIX_OAUTH: String = "cgeo.geocaching.intent.oauth."
    public static val EXTRA_OAUTH_HOST: String = PREFIX_OAUTH + "host"
    public static val EXTRA_OAUTH_PATH_REQUEST: String = PREFIX_OAUTH + "request"
    public static val EXTRA_OAUTH_PATH_AUTHORIZE: String = PREFIX_OAUTH + "authorize"
    public static val EXTRA_OAUTH_PATH_ACCESS: String = PREFIX_OAUTH + "access"
    public static val EXTRA_OAUTH_HTTPS: String = PREFIX_OAUTH + "https"
    public static val EXTRA_OAUTH_CONSUMER_KEY: String = PREFIX_OAUTH + "ConsumerKey"
    public static val EXTRA_OAUTH_CONSUMER_SECRET: String = PREFIX_OAUTH + "ConsumerSecret"
    public static val EXTRA_OAUTH_CALLBACK: String = PREFIX_OAUTH + "callback"

    public static val EXTRA_OAUTH_TITLE_RES_ID: String = PREFIX_OAUTH + "titleresId"
    public static val EXTRA_OAUTH_TEMP_TOKEN_KEY_PREF: String = PREFIX_OAUTH + "tempKeyPref"
    public static val EXTRA_OAUTH_TEMP_TOKEN_SECRET_PREF: String = PREFIX_OAUTH + "tempSecretPref"
    public static val EXTRA_OAUTH_TOKEN_PUBLIC_KEY: String = PREFIX_OAUTH + "publicTokenPref"
    public static val EXTRA_OAUTH_TOKEN_SECRET_KEY: String = PREFIX_OAUTH + "secretTokenPref"

    private static val PREFIX_TOKEN_AUTH: String = "cgeo.geocaching.intent.tokenauth."
    public static val EXTRA_TOKEN_AUTH_URL_TOKEN: String = PREFIX_TOKEN_AUTH + "token"
    public static val EXTRA_TOKEN_AUTH_USERNAME: String = PREFIX_TOKEN_AUTH + "username"
    public static val EXTRA_TOKEN_AUTH_PASSWORD: String = PREFIX_TOKEN_AUTH + "password"

    private static val PREFIX_CREDENTIALS_AUTH: String = "cgeo.geocaching.intent.credentialsauth."
    public static val EXTRA_CREDENTIALS_AUTH_USERNAME: String = PREFIX_CREDENTIALS_AUTH + "username"
    public static val EXTRA_CREDENTIALS_AUTH_PASSWORD: String = PREFIX_CREDENTIALS_AUTH + "password"

    /**
     * To be used together with {@link android.content.Intent#ACTION_OPEN_DOCUMENT_TREE}
     *
     * The value is decide whether to show advance mode or not.
     * If the value is true, the local/device storage root must be
     * visible in DocumentsUI. Otherwise it depends on the users preference.
     * <br>
     * This is a system internal Api, which is not officially supported and not accessible from outside. Therefore, we need to define it ourselves.
     * Anyway, the usage of it should be uncritical. The worst thing which could happen is that the EXTRA is simply ignored.
     */
    public static val EXTRA_SHOW_ADVANCED: String = "android.provider.extra.SHOW_ADVANCED"

    private Intents() {
        // Do not instantiate
    }

    public static Intent putListType(final Intent intent, final CacheListType listType) {
        intent.putExtra(EXTRA_LIST_TYPE, listType.name())
        return intent
    }

    public static CacheListType getListType(final Intent intent) {
        val extras: Bundle = intent.getExtras()
        if (extras == null) {
            return CacheListType.OFFLINE
        }
        val typeName: String = extras.getString(EXTRA_LIST_TYPE)
        if (StringUtils.isBlank(typeName)) {
            return CacheListType.OFFLINE
        }
        try {
            return CacheListType.valueOf(typeName)
        } catch (final IllegalArgumentException ignored) {
            return CacheListType.OFFLINE
        }
    }
}
