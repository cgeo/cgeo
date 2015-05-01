package cgeo.geocaching.settings;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.enumerations.StatusCode;
import cgeo.geocaching.network.Cookies;
import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.utils.RxUtils;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;

import rx.Observable;
import rx.android.app.AppObservable;
import rx.functions.Action1;
import rx.functions.Func0;

import android.app.ProgressDialog;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.preference.Preference;
import android.util.AttributeSet;

public abstract class AbstractCheckCredentialsPreference extends AbstractClickablePreference {

    public AbstractCheckCredentialsPreference(final SettingsActivity activity, final AttributeSet attrs) {
        super(activity, attrs);
    }

    public AbstractCheckCredentialsPreference(final SettingsActivity activity, final AttributeSet attrs, final int defStyle) {
        super(activity, attrs, defStyle);
    }

    @Override
    protected OnPreferenceClickListener getOnPreferenceClickListener(final SettingsActivity activity) {
        return new LoginCheckClickListener(activity);
    }

    protected abstract ImmutablePair<String, String> getCredentials();

    /**
     * Try to login.
     *
     * @return A pair containing the status code, and, if the status code is
     * <tt>NO_ERROR</tt>, an observable (or <tt>null</tt>) wihch may emit
     * the avatar for the user (every drawable will be shown in place of the previous one).
     */
    protected abstract ImmutablePair<StatusCode, Observable<Drawable>> login();

    private class LoginCheckClickListener implements OnPreferenceClickListener {
        final private SettingsActivity settingsActivity;

        LoginCheckClickListener(final SettingsActivity activity) {
            settingsActivity = activity;
        }

        @Override
        public boolean onPreferenceClick(final Preference preference) {
            final Resources res = settingsActivity.getResources();
            final ImmutablePair<String, String> credentials = getCredentials();

            // check credentials for validity
            if (StringUtils.isBlank(credentials.getLeft())
                    || StringUtils.isBlank(credentials.getRight())) {
                ActivityMixin.showToast(settingsActivity, R.string.err_missing_auth);
                return false;
            }

            final ProgressDialog loginDialog = ProgressDialog.show(settingsActivity,
                    res.getString(R.string.init_login_popup),
                    res.getString(R.string.init_login_popup_working), true);
            loginDialog.setCancelable(false);
            Cookies.clearCookies();

            AppObservable.bindActivity(settingsActivity, Observable.defer(new Func0<Observable<ImmutablePair<StatusCode, Observable<Drawable>>>>() {
                @Override
                public Observable<ImmutablePair<StatusCode, Observable<Drawable>>> call() {
                    return Observable.just(login());
                }
            })).subscribeOn(RxUtils.networkScheduler).subscribe(new Action1<ImmutablePair<StatusCode, Observable<Drawable>>>() {
                @Override
                public void call(final ImmutablePair<StatusCode, Observable<Drawable>> loginInfo) {
                    loginDialog.dismiss();
                    if (loginInfo.getLeft() == StatusCode.NO_ERROR) {
                        Dialogs.message(settingsActivity, R.string.init_login_popup, R.string.init_login_popup_ok, loginInfo.getRight());
                    } else {
                        Dialogs.message(settingsActivity, R.string.init_login_popup,
                                res.getString(R.string.init_login_popup_failed_reason)
                                        + " "
                                        + loginInfo.getLeft().getErrorString(res)
                                        + "."
                        );
                    }
                    settingsActivity.initBasicMemberPreferences();
                }
            });

            return false; // no shared preference has to be changed
        }
    }
}
