package cgeo.geocaching.settings;

import cgeo.geocaching.twitter.TwitterAuthorizationActivity;

import android.content.Context;
import android.content.Intent;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

public class AuthorizeTwitterPreference extends Preference {

    public AuthorizeTwitterPreference(Context context) {
        super(context);
    }

    public AuthorizeTwitterPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AuthorizeTwitterPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected View onCreateView(ViewGroup parent) {
        final SettingsActivity activity = (SettingsActivity) getContext();

        setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent authIntent = new Intent(preference.getContext(),
                        TwitterAuthorizationActivity.class);
                activity.startActivityForResult(authIntent,
                        SettingsActivity.OAUTH_TWITTER_REQUEST);

                return false; // no shared preference has to be changed
            }
        });

        activity.setTwitterAuthTitle();
        return super.onCreateView(parent);
    }
}
