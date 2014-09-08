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
import rx.android.observables.AndroidObservable;
import rx.functions.Action1;
import rx.functions.Func0;

import android.app.ProgressDialog;
import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;

public class CheckGeokretyCredentialsPreference extends AbstractClickablePreference {

    public CheckGeokretyCredentialsPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CheckGeokretyCredentialsPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected OnPreferenceClickListener getOnPreferenceClickListener(final SettingsActivity activity) {
        return new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                // satisfy static code analysis
                if (activity == null) {
                    return true;
                }

                final String username = Settings.getGeokretyUsername();
                final String password = Settings.getGeokretyPassword();

                if (StringUtils.isBlank(username) || StringUtils.isBlank(password)) {
                    ActivityMixin.showToast(activity, R.string.err_missing_auth);
                    return false;
                }

                final ProgressDialog progressDialog = ProgressDialog.show(activity,
                        activity.getString(R.string.init_geokrety),
                        activity.getString(R.string.init_geokrety_login), true);
                progressDialog.setCancelable(false);

                AndroidObservable.bindActivity(activity, Observable.defer(new Func0<Observable<Integer>>() {
                    @Override
                    public Observable<Integer> call() {
                        final String nam = StringUtils.defaultString(username);
                        final String pwd = StringUtils.defaultString(password);

                        final Parameters params = new Parameters("login", nam, "password", pwd);
                        HttpResponse response = Network.postRequest("http://geokrety.org/api-login2secid.php", params);

                        if (response != null && response.getStatusLine().getStatusCode() == 200) {
                            //response was OK
                            final String secid = Network.getResponseData(response);
                            // TODO Here I expect the secid field to be updated or maybe do not show secid box
                            Settings.setGeokretySecId(secid);
                            try {
                                return Observable.just(1); // ??? what that ??? Integer.parseInt(secid.trim()));
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
                            Dialogs.message(activity, R.string.init_geokrety, R.string.init_geokrety_register_ok);
                        } else {
                            Dialogs.message(activity, R.string.init_geokrety, R.string.init_geokrety_register_fail);
                        }
                    }
                });

                return true;
            }
        };
    }

}
