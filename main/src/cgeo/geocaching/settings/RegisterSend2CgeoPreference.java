package cgeo.geocaching.settings;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.network.Network;
import cgeo.geocaching.network.Parameters;
import cgeo.geocaching.ui.dialog.SimpleDialog;
import cgeo.geocaching.utils.AndroidRxUtils;
import cgeo.geocaching.utils.Log;

import android.app.ProgressDialog;
import android.content.Context;
import android.util.AttributeSet;

import io.reactivex.rxjava3.core.Observable;
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
        return preference -> {
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

            AndroidRxUtils.bindActivity(activity, Observable.defer(() -> {
                final String nam = StringUtils.defaultString(deviceName);
                final String cod = StringUtils.defaultString(deviceCode);

                final Parameters params = new Parameters("name", nam, "code", cod);

                try {
                    final Response response = Network.getRequest("https://send2.cgeo.org/auth.html", params)
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
            }).first(0)).subscribeOn(AndroidRxUtils.networkScheduler).subscribe(pin -> {
                progressDialog.dismiss();
                if (pin > 0) {
                    SimpleDialog.of(activity).setTitle(R.string.init_sendToCgeo)
                            .setMessage(R.string.init_sendToCgeo_register_ok, pin).show();
                } else {
                    SimpleDialog.of(activity).setTitle(R.string.init_sendToCgeo)
                            .setMessage(R.string.init_sendToCgeo_register_fail).show();
                }
            });

            return true;
        };
    }

}
