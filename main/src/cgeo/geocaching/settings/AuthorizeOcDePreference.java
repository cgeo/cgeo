package cgeo.geocaching.settings;

import cgeo.geocaching.connector.oc.OCAuthorizationActivity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

public class AuthorizeOcDePreference extends Preference {

    public AuthorizeOcDePreference(Context context) {
        super(context);
    }

    public AuthorizeOcDePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AuthorizeOcDePreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected View onCreateView(ViewGroup parent) {
        setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent authIntent = new Intent(preference.getContext(),
                        OCAuthorizationActivity.class);
                preference.getContext().startActivity(authIntent);

                return false; // no shared preference has to be changed
            }
        });
        return super.onCreateView(parent);
    }
}
