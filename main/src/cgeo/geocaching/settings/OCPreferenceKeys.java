package cgeo.geocaching.settings;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.connector.oc.OCAuthParams;

import android.util.SparseArray;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public enum OCPreferenceKeys {

    OC_DE("oc.de", R.string.pref_connectorOCActive, R.string.preference_screen_ocde, R.string.pref_fakekey_ocde_authorization, R.string.pref_fakekey_ocde_website, R.string.pref_ocde_tokenpublic, R.string.pref_ocde_tokensecret, OCAuthParams.OC_DE_AUTH_PARAMS),
    OC_PL("oc.pl", R.string.pref_connectorOCPLActive, R.string.preference_screen_ocpl, R.string.pref_fakekey_ocpl_authorization, R.string.pref_fakekey_ocpl_website, R.string.pref_ocpl_tokenpublic, R.string.pref_ocpl_tokensecret, OCAuthParams.OC_PL_AUTH_PARAMS),
    OC_US("oc.us", R.string.pref_connectorOCUSActive, R.string.preference_screen_ocus, R.string.pref_fakekey_ocus_authorization, R.string.pref_fakekey_ocus_website, R.string.pref_ocus_tokenpublic, R.string.pref_ocus_tokensecret, OCAuthParams.OC_US_AUTH_PARAMS),
    OC_NL("oc.nl", R.string.pref_connectorOCNLActive, R.string.preference_screen_ocnl, R.string.pref_fakekey_ocnl_authorization, R.string.pref_fakekey_ocnl_website, R.string.pref_ocnl_tokenpublic, R.string.pref_ocnl_tokensecret, OCAuthParams.OC_NL_AUTH_PARAMS),
    OC_RO("oc.ro", R.string.pref_connectorOCROActive, R.string.preference_screen_ocro, R.string.pref_fakekey_ocro_authorization, R.string.pref_fakekey_ocro_website, R.string.pref_ocro_tokenpublic, R.string.pref_ocro_tokensecret, OCAuthParams.OC_RO_AUTH_PARAMS),
    OC_UK("oc.uk", R.string.pref_connectorOCUKActive, R.string.preference_screen_ocuk, R.string.pref_fakekey_ocuk_authorization, R.string.pref_fakekey_ocuk_website, R.string.pref_ocuk2_tokenpublic, R.string.pref_ocuk2_tokensecret, OCAuthParams.OC_UK_AUTH_PARAMS);

    OCPreferenceKeys(final String siteId, final int isActivePrefId, final int prefScreenId, final int authPrefId, final int websitePrefId, final int publicTokenPrefId, final int privateTokenPrefId, final OCAuthParams authParams) {
        this.siteId = siteId;
        this.isActivePrefId = isActivePrefId;
        this.prefScreenId = prefScreenId;
        this.authPrefId = authPrefId;
        this.websitePrefId = websitePrefId;
        this.publicTokenPrefId = publicTokenPrefId;
        this.privateTokenPrefId = privateTokenPrefId;
        this.authParams = authParams;
    }

    private static final SparseArray<OCPreferenceKeys> FIND_BY_ISACTIVE_ID;
    private static final Map<String, OCPreferenceKeys> FIND_BY_ISACTIVE_KEY;
    private static final SparseArray<OCPreferenceKeys> FIND_BY_AUTH_PREF_ID;

    static {
        FIND_BY_ISACTIVE_ID = new SparseArray<>(values().length);
        FIND_BY_AUTH_PREF_ID = new SparseArray<>(values().length);
        final Map<String, OCPreferenceKeys> byIsactiveKey = new HashMap<>();
        for (final OCPreferenceKeys key : values()) {
            FIND_BY_ISACTIVE_ID.put(key.isActivePrefId, key);
            FIND_BY_AUTH_PREF_ID.put(key.authPrefId, key);
            byIsactiveKey.put(CgeoApplication.getInstance().getString(key.isActivePrefId), key);
        }
        FIND_BY_ISACTIVE_KEY = Collections.unmodifiableMap(byIsactiveKey);
    }

    public static boolean isOCPreference(final String prefKey) {
        return FIND_BY_ISACTIVE_KEY.containsKey(prefKey);
    }

    public static OCPreferenceKeys getByAuthId(final int authPrefId) {
        return FIND_BY_AUTH_PREF_ID.get(authPrefId);
    }

    public static OCPreferenceKeys getByKey(final String prefKey) {
        return FIND_BY_ISACTIVE_KEY.get(prefKey);
    }

    public final String siteId;
    public final int isActivePrefId;
    public final int prefScreenId;
    public final int websitePrefId;
    public final int authPrefId;
    public final int publicTokenPrefId;
    public final int privateTokenPrefId;
    public final OCAuthParams authParams;

}
