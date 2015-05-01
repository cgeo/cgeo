package cgeo.geocaching.settings;

import cgeo.geocaching.connector.gc.GCLogin;
import cgeo.geocaching.enumerations.StatusCode;

import org.apache.commons.lang3.tuple.ImmutablePair;

import rx.Observable;

import android.graphics.drawable.Drawable;
import android.util.AttributeSet;

public class CheckGcCredentialsPreference extends AbstractCheckCredentialsPreference {

    public CheckGcCredentialsPreference(final SettingsActivity activity, final AttributeSet attrs) {
        super(activity, attrs);
    }

    public CheckGcCredentialsPreference(final SettingsActivity activity, final AttributeSet attrs, final int defStyle) {
        super(activity, attrs, defStyle);
    }

    @Override
    protected ImmutablePair<String, String> getCredentials() {
        return Settings.getGcCredentials();
    }

    @Override
    protected ImmutablePair<StatusCode, Observable<Drawable>> login() {
        final StatusCode loginResult = GCLogin.getInstance().login();
        switch (loginResult) {
            case NO_ERROR:
                return ImmutablePair.of(StatusCode.NO_ERROR, GCLogin.getInstance().downloadAvatar());
            default:
                return ImmutablePair.of(loginResult, null);
        }
    }
}
