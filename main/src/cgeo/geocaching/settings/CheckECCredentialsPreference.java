package cgeo.geocaching.settings;

import cgeo.geocaching.connector.ec.ECConnector;
import cgeo.geocaching.connector.ec.ECLogin;
import cgeo.geocaching.enumerations.StatusCode;

import org.apache.commons.lang3.tuple.ImmutablePair;

import rx.Observable;

import android.graphics.drawable.Drawable;
import android.util.AttributeSet;

public class CheckECCredentialsPreference extends AbstractCheckCredentialsPreference {

    public CheckECCredentialsPreference(final SettingsActivity activity, final AttributeSet attrs) {
        super(activity, attrs);
    }

    public CheckECCredentialsPreference(final SettingsActivity activity, final AttributeSet attrs, final int defStyle) {
        super(activity, attrs, defStyle);
    }

    @Override
    protected ImmutablePair<String, String> getCredentials() {
        return Settings.getCredentials(ECConnector.getInstance());
    }

    @Override
    protected ImmutablePair<StatusCode, Observable<Drawable>> login() {
        return new ImmutablePair<>(ECLogin.getInstance().login(), null);
    }
}
