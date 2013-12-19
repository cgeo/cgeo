package cgeo.geocaching.settings;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.network.Network;
import cgeo.geocaching.network.Parameters;
import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.utils.Log;

import ch.boye.httpclientandroidlib.HttpResponse;

import org.apache.commons.lang3.StringUtils;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

public class RegisterSend2CgeoPreference extends Preference {

    ProgressDialog progressDialog;
    SettingsActivity activity;

    public RegisterSend2CgeoPreference(Context context) {
        super(context);
    }

    public RegisterSend2CgeoPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public RegisterSend2CgeoPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    private Handler webAuthHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            try {
                if (progressDialog != null && progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }

                if (msg.what > 0) {
                    Dialogs.message(activity, R.string.init_sendToCgeo,
                            activity.getString(R.string.init_sendToCgeo_register_ok)
                                    .replace("####", String.valueOf(msg.what)));
                } else {
                    Dialogs.message(activity, R.string.init_sendToCgeo, R.string.init_sendToCgeo_register_fail);
                }
            } catch (Exception e) {
                ActivityMixin.showToast(activity, R.string.init_sendToCgeo_register_fail);
                Log.e("SettingsActivity.webHandler", e);
            }

            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
        }
    };

    @Override
    protected View onCreateView(ViewGroup parent) {
        activity = (SettingsActivity) getContext();

        setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                final String deviceName = Settings.getWebDeviceName();
                final String deviceCode = Settings.getWebDeviceCode();

                if (StringUtils.isBlank(deviceName)) {
                    ActivityMixin.showToast(activity, R.string.err_missing_device_name);
                    return false;
                }

                progressDialog = ProgressDialog.show(activity,
                        activity.getString(R.string.init_sendToCgeo),
                        activity.getString(R.string.init_sendToCgeo_registering), true);
                progressDialog.setCancelable(false);

                (new Thread() {

                    @Override
                    public void run() {
                        int pin = 0;

                        final String nam = StringUtils.defaultString(deviceName);
                        final String cod = StringUtils.defaultString(deviceCode);

                        final Parameters params = new Parameters("name", nam, "code", cod);
                        HttpResponse response = Network.getRequest("http://send2.cgeo.org/auth.html", params);

                        if (response != null && response.getStatusLine().getStatusCode() == 200) {
                            //response was OK
                            String[] strings = StringUtils.split(Network.getResponseData(response), ',');
                            try {
                                pin = Integer.parseInt(strings[1].trim());
                            } catch (Exception e) {
                                Log.e("webDialog", e);
                            }
                            String code = strings[0];
                            Settings.setWebNameCode(nam, code);
                        }

                        webAuthHandler.sendEmptyMessage(pin);
                    }
                }).start();

                return true;
            }
        });
        return super.onCreateView(parent);
    }

}
