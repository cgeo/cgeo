package cgeo.geocaching.settings;

import cgeo.geocaching.connector.gc.Login;
import cgeo.geocaching.enumerations.StatusCode;

import org.apache.commons.lang3.tuple.ImmutablePair;

import android.content.Context;
import android.util.AttributeSet;

public class CheckGcCredentialsPreference extends AbstractCheckCredentialsPreference {

    public CheckGcCredentialsPreference(Context context) {
        super(context);
    }

    public CheckGcCredentialsPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CheckGcCredentialsPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected ImmutablePair<String, String> getCredentials() {
        return Settings.getGcLogin();
    }

    @Override
    protected Object login() {
        final StatusCode loginResult = Login.login();
        Object payload = loginResult;
        if (loginResult == StatusCode.NO_ERROR) {
            Login.detectGcCustomDate();
            payload = Login.downloadAvatarAndGetMemberStatus();
        }
        return payload;
    }
}
