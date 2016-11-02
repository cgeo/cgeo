package cgeo.geocaching.settings;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.network.Network;
import cgeo.geocaching.network.Parameters;
import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.utils.AndroidRxUtils;
import cgeo.geocaching.utils.Log;

import android.app.ProgressDialog;
import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;

import java.util.concurrent.Callable;

import io.reactivex.Observable;
import io.reactivex.functions.Consumer;
import okhttp3.Response;
import org.apache.commons.lang3.StringUtils;

public class RegisterSend2CgeoPreference extends AbstractClickablePreference {

    public RegisterSend2CgeoPreference(final Context context, final AttributeSet attrs) {
        super(context, attrs);
    }

    public RegisterSend2CgeoPreference(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected OnPreferenceClickListener getOnPreferenceClickListener(final SettingsActivity activity) {
        return new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(final Preference preference) {
                // satisfy static code analysis
                if (activity == null) {
                    return true;
                }

                final String deviceName = Settings.getWebDeviceName();
                if (StringUtils.isBlank(deviceName)) {
                    ActivityMixin.showToast(activity, R.string.err_missing_device_name);
                    return false;
                }

                final String deviceCode = Settings.getWebDeviceCode();

                final ProgressDialog progressDialog = ProgressDialog.show(activity,
                        activity.getString(R.string.init_sendToCgeo),
                        activity.getString(R.string.init_sendToCgeo_registering), true);
                progressDialog.setCancelable(false);

                AndroidRxUtils.bindActivity(activity, Observable.defer(new Callable<Observable<Integer>>() {
                    @Override
                    public Observable<Integer> call() {
                        final String nam = StringUtils.defaultString(deviceName);
                        final String cod = StringUtils.defaultString(deviceCode);

                        final Parameters params = new Parameters("name", nam, "code", cod);

                        try {
                            final Response response = Network.getRequest("http://send2.cgeo.org/auth.html", params)
                                    .flatMap(Network.withSuccess).blockingGet();

                            final String[] strings = StringUtils.split(Network.getResponseData(response), ',');
                            if (strings != null) {
                                Settings.setWebNameCode(nam, strings[0]);
                                return Observable.just(Integer.valueOf(strings[1].trim()));
                            }
                        } catch (final Exception e) {
                            Log.e("RegisterSend2CgeoPreference", e);
                        }

                        return Observable.empty();
                    }
                }).first(0)).subscribeOn(AndroidRxUtils.networkScheduler).subscribe(new Consumer<Integer>() {
                    @Override
                    public void accept(final Integer pin) {
                        progressDialog.dismiss();
                        if (pin > 0) {
                            Dialogs.message(activity, R.string.init_sendToCgeo,
                                    activity.getString(R.string.init_sendToCgeo_register_ok, pin));
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
