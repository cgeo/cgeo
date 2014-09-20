package cgeo.geocaching.settings;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.network.Network;
import cgeo.geocaching.network.Parameters;
import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.RxUtils;

import ch.boye.httpclientandroidlib.HttpResponse;

import org.apache.commons.lang3.StringUtils;

import rx.Observable;
import rx.android.app.AppObservable;
import rx.functions.Action1;
import rx.functions.Func0;

import android.app.ProgressDialog;
import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;

public class CheckGeokretyCredentialsPreference extends AbstractClickablePreference {

    public CheckGeokretyCredentialsPreference(final Context context, final AttributeSet attrs) {
        super(context, attrs);
    }

    public CheckGeokretyCredentialsPreference(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected OnPreferenceClickListener getOnPreferenceClickListener(final SettingsActivity settingsActivity) {
        return new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(final Preference preference) {
                // satisfy static code analysis
                if (settingsActivity == null) {
                    return true;
                }

                final String username = Settings.getGeokretyUsername();
                final String password = Settings.getGeokretyPassword();

                if (StringUtils.isBlank(username) || StringUtils.isBlank(password)) {
                    ActivityMixin.showToast(settingsActivity, R.string.err_missing_auth);
                    return false;
                }

                final ProgressDialog progressDialog = ProgressDialog.show(settingsActivity,
                        settingsActivity.getString(R.string.init_geokrety),
                        settingsActivity.getString(R.string.init_geokrety_login), true);
                progressDialog.setCancelable(false);

                AppObservable.bindActivity(settingsActivity, Observable.defer(new Func0<Observable<Integer>>() {
                    @Override
                    public Observable<Integer> call() {
                        final String nam = StringUtils.defaultString(username);
                        final String pwd = StringUtils.defaultString(password);

                        final Parameters params = new Parameters("login", nam, "password", pwd);
                        final HttpResponse response = Network.postRequest("http://geokrety.org/api-login2secid.php", params);

                        if (response != null && response.getStatusLine().getStatusCode() == 200) {
                            //response was OK
                            final String secid = Network.getResponseData(response);
                            try {
                                if (secid != null && secid.startsWith("error ")) {
                                    return Observable.just(0);
                                }
                                Settings.setGeokretySecId(secid);
                                return Observable.just(1);
                            } catch (final Exception e) {
                                Log.e("CheckGeokretyCredentialsPreference", e);
                            }
                        }

                        return Observable.empty();
                    }
                }).firstOrDefault(0)).subscribeOn(RxUtils.networkScheduler).subscribe(new Action1<Integer>() {
                    @Override
                    public void call(final Integer pin) {
                        progressDialog.dismiss();
                        if (pin > 0) {
                            Dialogs.message(settingsActivity, R.string.init_geokrety, R.string.init_geokrety_register_ok);
                        } else {
                            Dialogs.message(settingsActivity, R.string.init_geokrety, R.string.init_geokrety_register_fail);
                        }
                    }
                });

                return true;
            }
        };
    }

}
