package cgeo.geocaching.settings;

import cgeo.geocaching.connector.ec.ECConnector;
import cgeo.geocaching.connector.ec.ECLogin;
import cgeo.geocaching.enumerations.StatusCode;

import org.apache.commons.lang3.tuple.ImmutablePair;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;

public class CheckECCredentialsPreference extends AbstractCheckCredentialsPreference {

    public CheckECCredentialsPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CheckECCredentialsPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected ImmutablePair<String, String> getCredentials() {
        return Settings.getCredentials(ECConnector.getInstance());
    }

    @Override
    protected ImmutablePair<StatusCode, Drawable> login() {
        return new ImmutablePair<>(ECLogin.getInstance().login(), null);
    }
}
