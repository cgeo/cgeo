package cgeo.geocaching.network;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.AbstractActivity;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.MatcherWrapper;

import ch.boye.httpclientandroidlib.client.entity.UrlEncodedFormEntity;
import ch.boye.httpclientandroidlib.util.EntityUtils;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.util.regex.Pattern;

public abstract class OAuthAuthorizationActivity extends AbstractActivity {

    private String host;
    private String pathRequest;
    private String pathAuthorize;
    private String pathAccess;
    private boolean https;
    private String consumerKey;
    private String consumerSecret;
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

            startButton.setOnClickListener(new StartListener());
            startButton.setEnabled(true);

            if (msg.what == 1) {
                startButton.setText(getAuthAgain());

                pinEntry.setVisibility(View.VISIBLE);
                pinEntryButton.setVisibility(View.VISIBLE);
                pinEntryButton.setOnClickListener(new ConfirmPINListener());
            } else {
                showToast(getErrAuthInitialize());
                startButton.setText(getAuthStart());
            }
        }

    };
    private Handler changeTokensHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            if (changeTokensDialog != null && changeTokensDialog.isShowing()) {
                changeTokensDialog.dismiss();
            }

            pinEntryButton.setOnClickListener(new ConfirmPINListener());
            pinEntryButton.setEnabled(true);

            if (msg.what == 1) {
                showToast(getAuthDialogCompleted());

                pinEntryButton.setVisibility(View.GONE);

                setResult(RESULT_OK);
                finish();
            } else {
                showToast(getErrAuthProcess());

                pinEntry.setVisibility(View.GONE);
                pinEntryButton.setVisibility(View.GONE);
                startButton.setText(getAuthStart());
            }
        }
    };

    public OAuthAuthorizationActivity(String host,
            String pathRequest,
            String pathAuthorize,
            String pathAccess,
            boolean https,
            String consumerKey,
            String consumerSecret) {
        this.host = host;
        this.pathRequest = pathRequest;
        this.pathAuthorize = pathAuthorize;
        this.pathAccess = pathAccess;
        this.https = https;
        this.consumerKey = consumerKey;
        this.consumerSecret = consumerSecret;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState, R.layout.authorization_activity);

        setTitle(getAuthTitle());

        init();
    }

    private void init() {
        startButton = (Button) findViewById(R.id.start);
        pinEntry = (EditText) findViewById(R.id.pin);
        pinEntryButton = (Button) findViewById(R.id.pin_button);

        TextView auth = (TextView) findViewById(R.id.auth_1);
        auth.setText(getAboutAuth1());
        auth = (TextView) findViewById(R.id.auth_2);
        auth.setText(getAboutAuth2());

        ImmutablePair<String, String> tempToken = getTempToken();
        OAtoken = tempToken.left;
        OAtokenSecret = tempToken.right;

        startButton.setText(getAuthAuthorize());
        pinEntryButton.setText(getAuthFinish());

        startButton.setEnabled(true);
        startButton.setOnClickListener(new StartListener());

        if (StringUtils.isBlank(OAtoken) && StringUtils.isBlank(OAtokenSecret)) {
            // start authorization process
            startButton.setText(getAuthStart());
        } else {
            // already have temporary tokens, continue from pin
            startButton.setText(getAuthAgain());

            pinEntry.setHint(getAuthPinHint());
            pinEntry.setVisibility(View.VISIBLE);
            pinEntryButton.setVisibility(View.VISIBLE);
            pinEntryButton.setOnClickListener(new ConfirmPINListener());
        }
    }

    private void requestToken() {

        int status = 0;
        try {
            final Parameters params = new Parameters();
            params.put("oauth_callback", "oob");
            final String method = "GET";
            OAuth.signOAuth(host, pathRequest, method, https, params, null, null, consumerKey, consumerSecret);
            final String line = Network.getResponseData(Network.getRequest(getUrlPrefix() + host + pathRequest, params));

            if (StringUtils.isNotBlank(line)) {
                final MatcherWrapper paramsMatcher1 = new MatcherWrapper(paramsPattern1, line);
                if (paramsMatcher1.find()) {
                    OAtoken = paramsMatcher1.group(1);
                }
                final MatcherWrapper paramsMatcher2 = new MatcherWrapper(paramsPattern2, line);
                if (paramsMatcher2.find()) {
                    OAtokenSecret = paramsMatcher2.group(1);
                }

                if (StringUtils.isNotBlank(OAtoken) && StringUtils.isNotBlank(OAtokenSecret)) {
                    setTempTokens(OAtoken, OAtokenSecret);
                    try {
                        final Parameters paramsBrowser = new Parameters();
                        paramsBrowser.put("oauth_token", OAtoken);
                        final String encodedParams = EntityUtils.toString(new UrlEncodedFormEntity(paramsBrowser));
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getUrlPrefix() + host + pathAuthorize + "?" + encodedParams)));
                        status = 1;
                    } catch (Exception e) {
                        Log.e("OAuthAuthorizationActivity.requestToken(2)", e);
                    }
                }
            }
        } catch (Exception e) {
            Log.e("OAuthAuthorizationActivity.requestToken(1)", e);
        }

        requestTokenHandler.sendEmptyMessage(status);
    }

    private void changeToken() {

        int status = 0;

        try {
            final Parameters params = new Parameters("oauth_verifier", pinEntry.getText().toString());

            final String method = "POST";
            OAuth.signOAuth(host, pathAccess, method, https, params, OAtoken, OAtokenSecret, consumerKey, consumerSecret);
            final String line = StringUtils.defaultString(Network.getResponseData(Network.postRequest(getUrlPrefix() + host + pathAccess, params)));

            OAtoken = "";
            OAtokenSecret = "";

            final MatcherWrapper paramsMatcher1 = new MatcherWrapper(paramsPattern1, line);
            if (paramsMatcher1.find()) {
                OAtoken = paramsMatcher1.group(1);
            }
            final MatcherWrapper paramsMatcher2 = new MatcherWrapper(paramsPattern2, line);
            if (paramsMatcher2.find() && paramsMatcher2.groupCount() > 0) {
                OAtokenSecret = paramsMatcher2.group(1);
            }

            if (StringUtils.isBlank(OAtoken) && StringUtils.isBlank(OAtokenSecret)) {
                OAtoken = "";
                OAtokenSecret = "";
                setTokens(null, null, false);
            } else {
                setTokens(OAtoken, OAtokenSecret, true);
                status = 1;
            }
        } catch (Exception e) {
            Log.e("OAuthAuthorizationActivity.changeToken", e);
        }

        changeTokensHandler.sendEmptyMessage(status);
    }

    private String getUrlPrefix() {
        return https ? "https://" : "http://";
    }

    private class StartListener implements View.OnClickListener {

        @Override
        public void onClick(View arg0) {
            if (requestTokenDialog == null) {
                requestTokenDialog = new ProgressDialog(OAuthAuthorizationActivity.this);
                requestTokenDialog.setCancelable(false);
                requestTokenDialog.setMessage(getAuthDialogWait());
            }
            requestTokenDialog.show();
            startButton.setEnabled(false);
            startButton.setOnTouchListener(null);
            startButton.setOnClickListener(null);

            setTempTokens(null, null);
            (new Thread() {

                @Override
                public void run() {
                    requestToken();
                }
            }).start();
        }
    }

    private class ConfirmPINListener implements View.OnClickListener {

        @Override
        public void onClick(View arg0) {
            if (StringUtils.isEmpty(((EditText) findViewById(R.id.pin)).getText().toString())) {
                helpDialog(getAuthDialogPinTitle(), getAuthDialogPinMessage());
                return;
            }

            if (changeTokensDialog == null) {
                changeTokensDialog = new ProgressDialog(OAuthAuthorizationActivity.this);
                changeTokensDialog.setCancelable(false);
                changeTokensDialog.setMessage(getAuthDialogWait());
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

    protected abstract ImmutablePair<String, String> getTempToken();

    protected abstract void setTempTokens(String tokenPublic, String tokenSecret);

    protected abstract void setTokens(String tokenPublic, String tokenSecret, boolean enable);

    // get resources from derived class

    protected abstract String getAuthTitle();

    protected abstract String getAuthAgain();

    protected abstract String getErrAuthInitialize();

    protected abstract String getAuthStart();

    protected abstract String getAuthDialogCompleted();

    protected abstract String getErrAuthProcess();

    protected abstract String getAuthDialogWait();

    protected abstract String getAuthDialogPinTitle();

    protected abstract String getAuthDialogPinMessage();

    protected abstract String getAboutAuth1();

    protected abstract String getAboutAuth2();

    protected abstract String getAuthAuthorize();

    protected abstract String getAuthPinHint();

    protected abstract String getAuthFinish();
}
