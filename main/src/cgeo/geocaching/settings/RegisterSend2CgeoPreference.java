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

public class RegisterSend2CgeoPreference extends AbstractClickablePreference {

    public RegisterSend2CgeoPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public RegisterSend2CgeoPreference(Context context, AttributeSet attrs, int defStyle) {
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

                final String deviceName = Settings.getWebDeviceName();
                final String deviceCode = Settings.getWebDeviceCode();

                if (StringUtils.isBlank(deviceName)) {
                    ActivityMixin.showToast(activity, R.string.err_missing_device_name);
                    return false;
                }

                final ProgressDialog progressDialog = ProgressDialog.show(activity,
                        activity.getString(R.string.init_sendToCgeo),
                        activity.getString(R.string.init_sendToCgeo_registering), true);
                progressDialog.setCancelable(false);

                AndroidObservable.bindActivity(activity, Observable.defer(new Func0<Observable<Integer>>() {
                    @Override
                    public Observable<Integer> call() {
                        final String nam = StringUtils.defaultString(deviceName);
                        final String cod = StringUtils.defaultString(deviceCode);

                        final Parameters params = new Parameters("name", nam, "code", cod);
                        HttpResponse response = Network.getRequest("http://send2.cgeo.org/auth.html", params);

                        if (response != null && response.getStatusLine().getStatusCode() == 200) {
                            //response was OK
                            final String[] strings = StringUtils.split(Network.getResponseData(response), ',');
                            if (strings != null) {
                                Settings.setWebNameCode(nam, strings[0]);
                                try {
                                    return Observable.just(Integer.parseInt(strings[1].trim()));
                                } catch (final Exception e) {
                                    Log.e("RegisterSend2CgeoPreference", e);
                                }
                            }
                        }

                        return Observable.empty();
                    }
                }).firstOrDefault(0)).subscribeOn(RxUtils.networkScheduler).subscribe(new Action1<Integer>() {
                    @Override
                    public void call(final Integer pin) {
                        progressDialog.dismiss();
                        if (pin > 0) {
                            Dialogs.message(activity, R.string.init_sendToCgeo,
                                    activity.getString(R.string.init_sendToCgeo_register_ok)
                                            .replace("####", String.valueOf(pin)));
                        } else {
                            Dialogs.message(activity, R.string.init_sendToCgeo, R.string.init_sendToCgeo_register_fail);
                        }
                    }
                });

                return true;
            }
        };
    }

}
