package cgeo.geocaching;

import cgeo.geocaching.activity.AbstractActivity;

import org.apache.commons.lang3.StringUtils;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;

public class cgeoauth extends AbstractActivity {
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

        OAtoken = prefs.getString("temp-token-public", null);
        OAtokenSecret = prefs.getString("temp-token-secret", null);

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
        final String host = "twitter.com";
        final String pathRequest = "/oauth/request_token";
        final String pathAuthorize = "/oauth/authorize";
        final String method = "GET";

        int status = 0;
        try {
            String lineOne = null;
            HttpsURLConnection connection = null;

            try {
                final StringBuilder sb = new StringBuilder();
                final String params = cgOAuth.signOAuth(host, pathRequest, method, true, new Parameters(), null, null);

                int code = -1;
                int retries = 0;

                do {
                    // base.trustAllHosts();
                    Log.d(Settings.tag, "https://" + host + pathRequest + "?" + params);
                    final URL u = new URL("https://" + host + pathRequest + "?" + params);
                    final URLConnection uc = u.openConnection();
                    connection = (HttpsURLConnection) uc;

                    // connection.setHostnameVerifier(base.doNotVerify);
                    connection.setReadTimeout(30000);
                    connection.setRequestMethod(method);
                    HttpURLConnection.setFollowRedirects(true);
                    connection.setDoInput(true);
                    connection.setDoOutput(false);

                    final InputStream in = connection.getInputStream();
                    final InputStreamReader ins = new InputStreamReader(in);
                    final BufferedReader br = new BufferedReader(ins, 16 * 1024);

                    while ((lineOne = br.readLine()) != null) {
                        sb.append(lineOne);
                        sb.append('\n');
                    }

                    code = connection.getResponseCode();
                    retries++;

                    Log.i(Settings.tag, host + ": " + connection.getResponseCode() + " " + connection.getResponseMessage());

                    br.close();
                    in.close();
                    ins.close();
                } while (code == -1 && retries < 5);

                final String line = sb.toString();

                if (StringUtils.isNotBlank(line)) {
                    final Matcher paramsMatcher1 = paramsPattern1.matcher(line);
                    if (paramsMatcher1.find() && paramsMatcher1.groupCount() > 0) {
                        OAtoken = paramsMatcher1.group(1);
                    }
                    final Matcher paramsMatcher2 = paramsPattern2.matcher(line);
                    if (paramsMatcher2.find() && paramsMatcher2.groupCount() > 0) {
                        OAtokenSecret = paramsMatcher2.group(1);
                    }

                    if (StringUtils.isNotBlank(OAtoken) && StringUtils.isNotBlank(OAtokenSecret)) {
                        final SharedPreferences.Editor prefsEdit = getSharedPreferences(Settings.preferences, 0).edit();
                        prefsEdit.putString("temp-token-public", OAtoken);
                        prefsEdit.putString("temp-token-secret", OAtokenSecret);
                        prefsEdit.commit();

                        try {
                            final Parameters paramsPre = new Parameters();
                            paramsPre.put("oauth_callback", "oob");

                            final String paramsBrowser = cgOAuth.signOAuth(host, pathAuthorize, "GET", true, paramsPre, OAtoken, OAtokenSecret);

                            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://" + host + pathAuthorize + "?" + paramsBrowser)));

                            status = 1;
                        } catch (Exception e) {
                            Log.e(Settings.tag, "cgeoauth.requestToken(2): " + e.toString());
                        }
                    }
                }
            } catch (IOException eio) {
                Log.e(Settings.tag, "cgeoauth.requestToken(IO): " + eio.toString() + " ~ " + connection.getResponseCode() + ": " + connection.getResponseMessage());
            } catch (Exception e) {
                Log.e(Settings.tag, "cgeoauth.requestToken(1): " + e.toString());
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        } catch (Exception e2) {
            Log.e(Settings.tag, "cgeoauth.requestToken(3): " + e2.toString());
        }

        requestTokenHandler.sendEmptyMessage(status);
    }

    private void changeToken() {
        final String host = "twitter.com";
        final String path = "/oauth/access_token";
        final String method = "POST";

        int status = 0;
        String lineOne = null;

        try {
            final Parameters paramsPre = new Parameters();
            paramsPre.put("oauth_verifier", pinEntry.getText().toString());

            int code = -1;
            int retries = 0;

            final String params = cgOAuth.signOAuth(host, path, method, true, paramsPre, OAtoken, OAtokenSecret);
            final StringBuilder sb = new StringBuilder();
            do {
                // base.trustAllHosts();
                final URL u = new URL("https://" + host + path);
                final URLConnection uc = u.openConnection();
                final HttpsURLConnection connection = (HttpsURLConnection) uc;

                // connection.setHostnameVerifier(base.doNotVerify);
                connection.setReadTimeout(30000);
                connection.setRequestMethod(method);
                HttpURLConnection.setFollowRedirects(true);
                connection.setDoOutput(true);
                connection.setDoInput(true);

                final OutputStream out = connection.getOutputStream();
                final OutputStreamWriter wr = new OutputStreamWriter(out);

                wr.write(params);
                wr.flush();
                wr.close();
                out.close();

                final InputStream in = connection.getInputStream();
                final InputStreamReader ins = new InputStreamReader(in);
                final BufferedReader br = new BufferedReader(ins, 16 * 1024);

                while ((lineOne = br.readLine()) != null) {
                    sb.append(lineOne);
                    sb.append('\n');
                }

                code = connection.getResponseCode();
                retries++;

                Log.i(Settings.tag, host + ": " + connection.getResponseCode() + " " + connection.getResponseMessage());

                br.close();
                ins.close();
                in.close();
                connection.disconnect();
            } while (code == -1 && retries < 5);

            final String line = sb.toString();

            OAtoken = "";
            OAtokenSecret = "";

            final Matcher paramsMatcher1 = paramsPattern1.matcher(line);
            if (paramsMatcher1.find() && paramsMatcher1.groupCount() > 0) {
                OAtoken = paramsMatcher1.group(1);
            }
            final Matcher paramsMatcher2 = paramsPattern2.matcher(line);
            if (paramsMatcher2.find() && paramsMatcher2.groupCount() > 0) {
                OAtokenSecret = paramsMatcher2.group(1);
            }

            if (StringUtils.isBlank(OAtoken) && StringUtils.isBlank(OAtokenSecret)) {
                OAtoken = "";
                OAtokenSecret = "";

                final SharedPreferences.Editor prefs = getSharedPreferences(Settings.preferences, 0).edit();
                prefs.putString("tokenpublic", null);
                prefs.putString("tokensecret", null);
                prefs.putInt("twitter", 0);
                prefs.commit();
            } else {
                final SharedPreferences.Editor prefs = getSharedPreferences(Settings.preferences, 0).edit();
                prefs.remove("temp-token-public");
                prefs.remove("temp-token-secret");
                prefs.putString("tokenpublic", OAtoken);
                prefs.putString("tokensecret", OAtokenSecret);
                prefs.putInt("twitter", 1);
                prefs.commit();

                status = 1;
            }
        } catch (Exception e) {
            Log.e(Settings.tag, "cgeoauth.changeToken: " + e.toString());
        }

        changeTokensHandler.sendEmptyMessage(status);
    }

    private class startListener implements View.OnClickListener {

        public void onClick(View arg0) {
            if (requestTokenDialog == null) {
                requestTokenDialog = new ProgressDialog(cgeoauth.this);
                requestTokenDialog.setCancelable(false);
                requestTokenDialog.setMessage(res.getString(R.string.auth_dialog_wait));
            }
            requestTokenDialog.show();
            startButton.setEnabled(false);
            startButton.setOnTouchListener(null);
            startButton.setOnClickListener(null);

            final SharedPreferences.Editor prefs = getSharedPreferences(Settings.preferences, 0).edit();
            prefs.putString("temp-token-public", null);
            prefs.putString("temp-token-secret", null);
            prefs.commit();

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
                changeTokensDialog = new ProgressDialog(cgeoauth.this);
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
