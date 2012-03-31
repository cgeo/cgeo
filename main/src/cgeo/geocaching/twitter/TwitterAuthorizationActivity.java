package cgeo.geocaching.twitter;

import cgeo.geocaching.R;
import cgeo.geocaching.Settings;
import cgeo.geocaching.activity.AbstractActivity;
import cgeo.geocaching.network.Network;
import cgeo.geocaching.network.OAuth;
import cgeo.geocaching.network.Parameters;
import cgeo.geocaching.utils.Log;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.util.EntityUtils;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TwitterAuthorizationActivity extends AbstractActivity {
    private String OAtoken = null;
    private String OAtokenSecret = null;
    private final Pattern paramsPattern1 = Pattern.compile("oauth_token=([a-zA-Z0-9\\-\\_.]+)");
    private final Pattern paramsPattern2 = Pattern.compile("oauth_token_secret=([a-zA-Z0-9\\-\\_.]+)");
    private Button startButton = null;
    private EditText pinEntry = null;
    private Button pinEntryButton = null;
    private ProgressDialog requestTokenDialog = null;
    private ProgressDialog changeTokensDialog = null;
    private Handler requestTokenHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            if (requestTokenDialog != null && requestTokenDialog.isShowing()) {
                requestTokenDialog.dismiss();
            }

            startButton.setOnClickListener(new startListener());
            startButton.setEnabled(true);

            if (msg.what == 1) {
                startButton.setText(res.getString(R.string.auth_again));

                pinEntry.setVisibility(View.VISIBLE);
                pinEntryButton.setVisibility(View.VISIBLE);
                pinEntryButton.setOnClickListener(new confirmPINListener());
            } else {
                showToast(res.getString(R.string.err_auth_initialize));
                startButton.setText(res.getString(R.string.auth_start));
            }
        }
    };
    private Handler changeTokensHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            if (changeTokensDialog != null && changeTokensDialog.isShowing()) {
                changeTokensDialog.dismiss();
            }

            pinEntryButton.setOnClickListener(new confirmPINListener());
            pinEntryButton.setEnabled(true);

            if (msg.what == 1) {
                showToast(res.getString(R.string.auth_dialog_completed));

                pinEntryButton.setVisibility(View.GONE);

                finish();
            } else {
                showToast(res.getString(R.string.err_auth_process));

                pinEntry.setVisibility(View.GONE);
                pinEntryButton.setVisibility(View.GONE);
                startButton.setText(res.getString(R.string.auth_start));
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // init
        app.setAction("setting up");

        setTheme();
        setContentView(R.layout.auth);
        setTitle(res.getString(R.string.auth_twitter));

        init();
    }

    @Override
    public void onResume() {
        super.onResume();

    }

    private void init() {
        startButton = (Button) findViewById(R.id.start);
        pinEntry = (EditText) findViewById(R.id.pin);
        pinEntryButton = (Button) findViewById(R.id.pin_button);

        ImmutablePair<String, String> tempToken = Settings.getTempToken();
        OAtoken = tempToken.left;
        OAtokenSecret = tempToken.right;

        startButton.setEnabled(true);
        startButton.setOnClickListener(new startListener());

        if (StringUtils.isBlank(OAtoken) && StringUtils.isBlank(OAtokenSecret)) {
            // start authorization process
            startButton.setText(res.getString(R.string.auth_start));
        } else {
            // already have temporary tokens, continue from pin
            startButton.setText(res.getString(R.string.auth_again));

            pinEntry.setVisibility(View.VISIBLE);
            pinEntryButton.setVisibility(View.VISIBLE);
            pinEntryButton.setOnClickListener(new confirmPINListener());
        }
    }

    private void requestToken() {
        final String host = "api.twitter.com";
        final String pathRequest = "/oauth/request_token";
        final String pathAuthorize = "/oauth/authorize";
        final String method = "GET";

        int status = 0;
        try {
            final Parameters params = new Parameters();
            OAuth.signOAuth(host, pathRequest, method, true, params, null, null);
            final String line = Network.getResponseData(Network.request("https://" + host + pathRequest, params, false));


            if (StringUtils.isNotBlank(line)) {
                final Matcher paramsMatcher1 = paramsPattern1.matcher(line);
                if (paramsMatcher1.find()) {
                    OAtoken = paramsMatcher1.group(1);
                }
                final Matcher paramsMatcher2 = paramsPattern2.matcher(line);
                if (paramsMatcher2.find()) {
                    OAtokenSecret = paramsMatcher2.group(1);
                }

                if (StringUtils.isNotBlank(OAtoken) && StringUtils.isNotBlank(OAtokenSecret)) {
                    Settings.setTwitterTempTokens(OAtoken, OAtokenSecret);
                    try {
                        final Parameters paramsBrowser = new Parameters();
                        paramsBrowser.put("oauth_callback", "oob");
                        OAuth.signOAuth(host, pathAuthorize, "GET", true, paramsBrowser, OAtoken, OAtokenSecret);
                        final String encodedParams = EntityUtils.toString(new UrlEncodedFormEntity(paramsBrowser));
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://" + host + pathAuthorize + "?" + encodedParams)));
                        status = 1;
                    } catch (Exception e) {
                        Log.e(Settings.tag, "TwitterAuthorizationActivity.requestToken(2): " + e.toString());
                    }
                }
            }
        } catch (Exception e) {
            Log.e(Settings.tag, "TwitterAuthorizationActivity.requestToken(1): " + e.toString());
        }

        requestTokenHandler.sendEmptyMessage(status);
    }

    private void changeToken() {
        final String host = "api.twitter.com";
        final String path = "/oauth/access_token";
        final String method = "POST";

        int status = 0;

        try {
            final Parameters params = new Parameters("oauth_verifier", pinEntry.getText().toString());

            OAuth.signOAuth(host, path, method, true, params, OAtoken, OAtokenSecret);
            final String line = StringUtils.defaultString(Network.getResponseData(Network.postRequest("https://" + host + path, params)));

            OAtoken = "";
            OAtokenSecret = "";

            final Matcher paramsMatcher1 = paramsPattern1.matcher(line);
            if (paramsMatcher1.find()) {
                OAtoken = paramsMatcher1.group(1);
            }
            final Matcher paramsMatcher2 = paramsPattern2.matcher(line);
            if (paramsMatcher2.find() && paramsMatcher2.groupCount() > 0) {
                OAtokenSecret = paramsMatcher2.group(1);
            }

            if (StringUtils.isBlank(OAtoken) && StringUtils.isBlank(OAtokenSecret)) {
                OAtoken = "";
                OAtokenSecret = "";
                Settings.setTwitterTokens(null, null, false);
            } else {
                Settings.setTwitterTokens(OAtoken, OAtokenSecret, true);
                status = 1;
            }
        } catch (Exception e) {
            Log.e(Settings.tag, "TwitterAuthorizationActivity.changeToken: " + e.toString());
        }

        changeTokensHandler.sendEmptyMessage(status);
    }

    private class startListener implements View.OnClickListener {

        public void onClick(View arg0) {
            if (requestTokenDialog == null) {
                requestTokenDialog = new ProgressDialog(TwitterAuthorizationActivity.this);
                requestTokenDialog.setCancelable(false);
                requestTokenDialog.setMessage(res.getString(R.string.auth_dialog_wait));
            }
            requestTokenDialog.show();
            startButton.setEnabled(false);
            startButton.setOnTouchListener(null);
            startButton.setOnClickListener(null);

            Settings.setTwitterTempTokens(null, null);
            (new Thread() {

                @Override
                public void run() {
                    requestToken();
                }
            }).start();
        }
    }

    private class confirmPINListener implements View.OnClickListener {

        public void onClick(View arg0) {
            if (((EditText) findViewById(R.id.pin)).getText().toString().length() == 0) {
                helpDialog(res.getString(R.string.auth_dialog_pin_title), res.getString(R.string.auth_dialog_pin_message));
                return;
            }

            if (changeTokensDialog == null) {
                changeTokensDialog = new ProgressDialog(TwitterAuthorizationActivity.this);
                changeTokensDialog.setCancelable(false);
                changeTokensDialog.setMessage(res.getString(R.string.auth_dialog_wait));
            }
            changeTokensDialog.show();
            pinEntryButton.setEnabled(false);
            pinEntryButton.setOnTouchListener(null);
            pinEntryButton.setOnClickListener(null);

            (new Thread() {

                @Override
                public void run() {
                    changeToken();
                }
            }).start();
        }
    }
}
