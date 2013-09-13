package cgeo.geocaching.settings;

import cgeo.geocaching.R;
import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.connector.oc.OCDEAuthorizationActivity;
import cgeo.geocaching.connector.oc.OCPLAuthorizationActivity;

import android.content.Context;
import android.content.Intent;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

public class AuthorizeOcPreference extends Preference {

    private static final int NO_KEY = -1;

    private enum OCAuthorizations {
        NONE(NO_KEY, null),
        OCDE(R.string.pref_fakekey_ocde_authorization, OCDEAuthorizationActivity.class),
        OCPL(R.string.pref_fakekey_ocpl_authorization, OCPLAuthorizationActivity.class);

        public int prefKeyId;
        public Class<?> authActivity;

        OCAuthorizations(int prefKeyId, Class<?> clazz) {
            this.prefKeyId = prefKeyId;
            this.authActivity = clazz;
        }
    }

    private final OCAuthorizations ocAuth;

    private OCAuthorizations getAuthorization() {
        final String prefKey = getKey();
        for (OCAuthorizations auth : OCAuthorizations.values()) {
            if (auth.prefKeyId != NO_KEY && prefKey.equals(CgeoApplication.getInstance().getString(auth.prefKeyId))) {
                return auth;
            }
        }
        return OCAuthorizations.NONE;
    }

    public AuthorizeOcPreference(Context context) {
        super(context);
        this.ocAuth = getAuthorization();
    }

    public AuthorizeOcPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.ocAuth = getAuthorization();
    }

    public AuthorizeOcPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.ocAuth = getAuthorization();
    }

    @Override
    protected View onCreateView(ViewGroup parent) {
        final SettingsActivity activity = (SettingsActivity) getContext();

        setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                if (ocAuth.authActivity != null) {
                Intent authIntent = new Intent(preference.getContext(),
                            ocAuth.authActivity);
                activity.startActivityForResult(authIntent,
                            ocAuth.prefKeyId);
                }
                return false; // no shared preference has to be changed
            }
        });

        activity.setOcAuthTitle(ocAuth.prefKeyId);
        return super.onCreateView(parent);
    }
}
