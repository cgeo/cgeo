package cgeo.geocaching.settings;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;

import android.util.SparseArray;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public enum OCPreferenceKeys {

    OC_DE("oc.de", R.string.pref_connectorOCActive, R.string.preference_screen_ocde, R.string.pref_ocde_tokenpublic, R.string.pref_ocde_tokensecret),
    OC_PL("oc.pl", R.string.pref_connectorOCPLActive, R.string.preference_screen_ocpl, R.string.pref_ocpl_tokenpublic, R.string.pref_ocpl_tokensecret),
    OC_US("oc.us", R.string.pref_connectorOCUSActive, R.string.preference_screen_ocus, R.string.pref_ocus_tokenpublic, R.string.pref_ocus_tokensecret),
    OC_NL("oc.nl", R.string.pref_connectorOCNLActive, R.string.preference_screen_ocnl, R.string.pref_ocnl_tokenpublic, R.string.pref_ocnl_tokensecret),
    OC_RO("oc.ro", R.string.pref_connectorOCROActive, R.string.preference_screen_ocro, R.string.pref_ocro_tokenpublic, R.string.pref_ocro_tokensecret);


    private OCPreferenceKeys(final String siteId, final int isActivePrefId, final int prefScreenId, final int publicTokenPrefId, final int privateTokenPrefId) {
        this.siteId = siteId;
        this.isActivePrefId = isActivePrefId;
        this.prefScreenId = prefScreenId;
        this.publicTokenPrefId = publicTokenPrefId;
        this.privateTokenPrefId = privateTokenPrefId;
    }

    private static final SparseArray<OCPreferenceKeys> FIND_BY_ISACTIVE_ID;
    private static final Map<String, OCPreferenceKeys> FIND_BY_ISACTIVE_KEY;

    static {
        FIND_BY_ISACTIVE_ID = new SparseArray<OCPreferenceKeys>(values().length);
        Map<String, OCPreferenceKeys> byIsactiveKey = new HashMap<String, OCPreferenceKeys>();
        for (OCPreferenceKeys key : values()) {
            FIND_BY_ISACTIVE_ID.put(key.isActivePrefId, key);
            byIsactiveKey.put(CgeoApplication.getInstance().getString(key.isActivePrefId), key);
        }
        FIND_BY_ISACTIVE_KEY = Collections.unmodifiableMap(byIsactiveKey);
    }

    public static boolean isOCPreference(final int prefId) {
        return FIND_BY_ISACTIVE_ID.get(prefId) != null;
    }

    public static boolean isOCPreference(final String prefKey) {
        return FIND_BY_ISACTIVE_KEY.containsKey(prefKey);
    }

    public static OCPreferenceKeys getById(final int prefId) {
        return FIND_BY_ISACTIVE_ID.get(prefId);
    }

    public static OCPreferenceKeys getByKey(final String prefKey) {
        return FIND_BY_ISACTIVE_KEY.get(prefKey);
    }

    public final String siteId;
    public final int isActivePrefId;
    public final int prefScreenId;
    public final int publicTokenPrefId;
    public final int privateTokenPrefId;

}
