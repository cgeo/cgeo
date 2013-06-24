package cgeo.geocaching.settings;

import cgeo.geocaching.OldSettings;
import cgeo.geocaching.R;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.connector.gc.Login;
import cgeo.geocaching.enumerations.StatusCode;
import cgeo.geocaching.network.Cookies;
import cgeo.geocaching.utils.Log;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

public class CheckGcCredentialsPreference extends Preference {

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
    protected View onCreateView(ViewGroup parent) {
        setOnPreferenceClickListener(GC_LOGIN_CHECK);
        return super.onCreateView(parent);
    }

    private final GcLoginCheck GC_LOGIN_CHECK = new GcLoginCheck();

    private class GcLoginCheck implements OnPreferenceClickListener {
        private Resources res;
        private Activity activity;

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
                        ActivityMixin.helpDialog(activity,
                                res.getString(R.string.oldinit_login_popup),
                                res.getString(R.string.oldinit_login_popup_ok),
                                (Drawable) msg.obj);
                    } else {
                        ActivityMixin.helpDialog(activity,
                                res.getString(R.string.oldinit_login_popup),
                                res.getString(R.string.oldinit_login_popup_failed_reason)
                                        + " "
                                        + ((StatusCode) msg.obj).getErrorString(res)
                                        + ".");
                    }
                } catch (Exception e) {
                    ActivityMixin.showToast(activity, R.string.err_login_failed);
                    Log.e("SettingsActivity.logInHandler", e);
                }

                if (loginDialog != null && loginDialog.isShowing()) {
                    loginDialog.dismiss();
                }
            }
        };

        @Override
        public boolean onPreferenceClick(Preference preference) {
            this.activity = (Activity) CheckGcCredentialsPreference.this.getContext();
            this.res = activity.getResources();

            ImmutablePair<String, String> credentials = OldSettings.getGcLogin();

            // check credentials for validity
            if (credentials == null || StringUtils.isBlank(credentials.getLeft())
                    || StringUtils.isBlank(credentials.getRight())) {
                ActivityMixin.showToast(activity, R.string.err_missing_auth);
                return false;
            }

            loginDialog = ProgressDialog.show(activity,
                    res.getString(R.string.oldinit_login_popup),
                    res.getString(R.string.oldinit_login_popup_working), true);
            loginDialog.setCancelable(false);
            Cookies.clearCookies();

            (new Thread() {
                @Override
                public void run() {
                    final StatusCode loginResult = Login.login();
                    Object payload = loginResult;
                    if (loginResult == StatusCode.NO_ERROR) {
                        Login.detectGcCustomDate();
                        payload = Login.downloadAvatarAndGetMemberStatus();
                    }
                    logInHandler.obtainMessage(0, payload).sendToTarget();
                }
            }).start();

            return false; // no shared preference has to be changed
        }
    }
}
