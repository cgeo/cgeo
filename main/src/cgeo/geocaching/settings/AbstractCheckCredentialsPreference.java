package cgeo.geocaching.settings;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.enumerations.StatusCode;
import cgeo.geocaching.network.Cookies;
import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.utils.Log;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.preference.Preference;
import android.util.AttributeSet;

public abstract class AbstractCheckCredentialsPreference extends AbstractClickablePreference {

    public AbstractCheckCredentialsPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AbstractCheckCredentialsPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected OnPreferenceClickListener getOnPreferenceClickListener(final SettingsActivity activity) {
        return new LoginCheckClickListener(activity);
    }

    protected abstract ImmutablePair<String, String> getCredentials();

    protected abstract Object login();

    private class LoginCheckClickListener implements OnPreferenceClickListener {
        final private Resources res;
        final private SettingsActivity activity;

        LoginCheckClickListener(final SettingsActivity activity) {
            this.activity = activity;
            this.res = activity.getResources();
        }

        private ProgressDialog loginDialog;
        @SuppressLint("HandlerLeak")
        private Handler logInHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                try {
                    if (loginDialog != null && loginDialog.isShowing()) {
                        loginDialog.dismiss();
                    }

                    if (msg.obj == null || (msg.obj instanceof Drawable)) {
                        Dialogs.message(activity, R.string.init_login_popup, R.string.init_login_popup_ok, (Drawable) msg.obj);
                    } else {
                        Dialogs.message(activity, R.string.init_login_popup,
                                res.getString(R.string.init_login_popup_failed_reason)
                                        + " "
                                        + ((StatusCode) msg.obj).getErrorString(res)
                                        + ".");
                    }
                } catch (Exception e) {
                    ActivityMixin.showToast(activity, R.string.err_login_failed);
                    Log.e("SettingsActivity.logInHandler", e);
                } finally {
                    if (loginDialog != null && loginDialog.isShowing()) {
                        loginDialog.dismiss();
                    }
                    // enable/disable basic member preferences
                    activity.initBasicMemberPreferences();
                }
            }
        };

        @Override
        public boolean onPreferenceClick(Preference preference) {
            final ImmutablePair<String, String> credentials = getCredentials();

            // check credentials for validity
            if (StringUtils.isBlank(credentials.getLeft())
                    || StringUtils.isBlank(credentials.getRight())) {
                ActivityMixin.showToast(activity, R.string.err_missing_auth);
                return false;
            }

            loginDialog = ProgressDialog.show(activity,
                    res.getString(R.string.init_login_popup),
                    res.getString(R.string.init_login_popup_working), true);
            loginDialog.setCancelable(false);
            Cookies.clearCookies();

            (new Thread() {
                @Override
                public void run() {
                    Object payload = login();
                    logInHandler.obtainMessage(0, payload).sendToTarget();
                }
            }).start();

            return false; // no shared preference has to be changed
        }
    }
}
