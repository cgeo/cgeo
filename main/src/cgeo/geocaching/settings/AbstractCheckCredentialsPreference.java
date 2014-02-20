package cgeo.geocaching.settings;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.enumerations.StatusCode;
import cgeo.geocaching.network.Cookies;
import cgeo.geocaching.ui.dialog.Dialogs;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import rx.Observable;
import rx.android.observables.AndroidObservable;
import rx.functions.Action1;
import rx.functions.Func0;
import rx.schedulers.Schedulers;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
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
        final private SettingsActivity activity;

        LoginCheckClickListener(final SettingsActivity activity) {
            this.activity = activity;
        }

        @Override
        public boolean onPreferenceClick(Preference preference) {
            final Resources res = activity.getResources();
            final ImmutablePair<String, String> credentials = getCredentials();

            // check credentials for validity
            if (StringUtils.isBlank(credentials.getLeft())
                    || StringUtils.isBlank(credentials.getRight())) {
                ActivityMixin.showToast(activity, R.string.err_missing_auth);
                return false;
            }

            final ProgressDialog loginDialog = ProgressDialog.show(activity,
                    res.getString(R.string.init_login_popup),
                    res.getString(R.string.init_login_popup_working), true);
            loginDialog.setCancelable(false);
            Cookies.clearCookies();

            AndroidObservable.fromActivity(activity, Observable.defer(new Func0<Observable<?>>() {
                @Override
                public Observable<?> call() {
                    return Observable.from(login());
                }
            }).subscribeOn(Schedulers.io())).subscribe(new Action1<Object>() {
                @Override
                public void call(final Object obj) {
                    loginDialog.dismiss();
                    if (obj == null || obj instanceof Drawable) {
                        Dialogs.message(activity, R.string.init_login_popup, R.string.init_login_popup_ok, (Drawable) obj);
                    } else {
                        Dialogs.message(activity, R.string.init_login_popup,
                                res.getString(R.string.init_login_popup_failed_reason)
                                        + " "
                                        + ((StatusCode) obj).getErrorString(res)
                                        + ".");
                    }
                    activity.initBasicMemberPreferences();
                }
            });

            return false; // no shared preference has to be changed
        }
    }
}
