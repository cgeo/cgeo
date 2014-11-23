package cgeo.geocaching.activity;

import butterknife.InjectView;

import cgeo.geocaching.Intents;
import cgeo.geocaching.R;
import cgeo.geocaching.network.Network;
import cgeo.geocaching.network.OAuth;
import cgeo.geocaching.network.OAuthTokens;
import cgeo.geocaching.network.Parameters;
import cgeo.geocaching.utils.BundleUtils;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.MatcherWrapper;
import cgeo.geocaching.utils.RxUtils;

import ch.boye.httpclientandroidlib.HttpResponse;
import ch.boye.httpclientandroidlib.ParseException;
import ch.boye.httpclientandroidlib.client.entity.UrlEncodedFormEntity;
import ch.boye.httpclientandroidlib.util.EntityUtils;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import rx.functions.Action0;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.IOException;
import java.util.regex.Pattern;

public abstract class OAuthAuthorizationActivity extends AbstractActivity {

    public static final int NOT_AUTHENTICATED = 0;
    public static final int AUTHENTICATED = 1;

    private static final int STATUS_ERROR = 0;
    private static final int STATUS_SUCCESS = 1;
    private static final int STATUS_ERROR_EXT_MSG = 2;
    private static final Pattern PARAMS_PATTERN_1 = Pattern.compile("oauth_token=([\\w_.-]+)");
    private static final Pattern PARAMS_PATTERN_2 = Pattern.compile("oauth_token_secret=([\\w_.-]+)");

    @NonNull private String host = StringUtils.EMPTY;
    @NonNull private String pathRequest = StringUtils.EMPTY;
    @NonNull private String pathAuthorize = StringUtils.EMPTY;
    @NonNull private String pathAccess = StringUtils.EMPTY;
    private boolean https = false;
    @NonNull private String consumerKey = StringUtils.EMPTY;
    @NonNull private String consumerSecret = StringUtils.EMPTY;
    @NonNull private String callback = StringUtils.EMPTY;
    private String OAtoken = null;
    private String OAtokenSecret = null;
    @InjectView(R.id.start) protected Button startButton;
    @InjectView(R.id.auth_1) protected TextView auth_1;
    @InjectView(R.id.auth_2) protected TextView auth_2;
    private ProgressDialog requestTokenDialog = null;
    private ProgressDialog changeTokensDialog = null;

    private final Handler requestTokenHandler = new Handler() {

        @Override
        public void handleMessage(final Message msg) {
            if (requestTokenDialog != null && requestTokenDialog.isShowing()) {
                requestTokenDialog.dismiss();
            }

            startButton.setOnClickListener(new StartListener());
            startButton.setEnabled(true);

            if (msg.what == STATUS_SUCCESS) {
                startButton.setText(getAuthAgain());
            } else if (msg.what == STATUS_ERROR_EXT_MSG) {
                String errMsg = getErrAuthInitialize();
                errMsg += msg.obj != null ? "\n" + msg.obj.toString() : "";
                showToast(errMsg);
                startButton.setText(getAuthStart());
            } else {
                showToast(getErrAuthInitialize());
                startButton.setText(getAuthStart());
            }
        }

    };

    private final Handler changeTokensHandler = new Handler() {

        @Override
        public void handleMessage(final Message msg) {
            if (changeTokensDialog != null && changeTokensDialog.isShowing()) {
                changeTokensDialog.dismiss();
            }

            if (msg.what == AUTHENTICATED) {
                showToast(getAuthDialogCompleted());
                setResult(RESULT_OK);
                finish();
            } else {
                showToast(getErrAuthProcess());
                startButton.setText(getAuthStart());
            }
        }
    };

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState, R.layout.authorization_activity);

        final Bundle extras = getIntent().getExtras();
        if (extras != null) {
            host = BundleUtils.getString(extras, Intents.EXTRA_OAUTH_HOST, host);
            pathRequest = BundleUtils.getString(extras, Intents.EXTRA_OAUTH_PATH_REQUEST, pathRequest);
            pathAuthorize = BundleUtils.getString(extras, Intents.EXTRA_OAUTH_PATH_AUTHORIZE, pathAuthorize);
            pathAccess = BundleUtils.getString(extras, Intents.EXTRA_OAUTH_PATH_ACCESS, pathAccess);
            https = extras.getBoolean(Intents.EXTRA_OAUTH_HTTPS, https);
            consumerKey = BundleUtils.getString(extras, Intents.EXTRA_OAUTH_CONSUMER_KEY, consumerKey);
            consumerSecret = BundleUtils.getString(extras, Intents.EXTRA_OAUTH_CONSUMER_SECRET, consumerSecret);
            callback = BundleUtils.getString(extras, Intents.EXTRA_OAUTH_CALLBACK, callback);
        }

        setTitle(getAuthTitle());

        auth_1.setText(getAuthExplainShort());
        auth_2.setText(getAuthExplainLong());

        final ImmutablePair<String, String> tempToken = getTempTokens();
        OAtoken = tempToken.left;
        OAtokenSecret = tempToken.right;

        startButton.setText(getAuthAuthorize());
        startButton.setEnabled(true);
        startButton.setOnClickListener(new StartListener());

        if (StringUtils.isBlank(OAtoken) && StringUtils.isBlank(OAtokenSecret)) {
            // start authorization process
            startButton.setText(getAuthStart());
        } else {
            // already have temporary tokens, continue from pin
            startButton.setText(getAuthAgain());
        }
    }

    @Override
    public void onNewIntent(final Intent intent) {
        setIntent(intent);
    }

    @Override
    public void onResume() {
        super.onResume();
        final Uri uri = getIntent().getData();
        if (uri != null) {
            final String verifier = uri.getQueryParameter("oauth_verifier");
            if (StringUtils.isNotBlank(verifier)) {
                exchangeTokens(verifier);
            } else {
                // We can shortcut the whole verification process if we do not have a token at all.
                changeTokensHandler.sendEmptyMessage(NOT_AUTHENTICATED);
            }
        }
    }

    private void requestToken() {

        final Parameters params = new Parameters();
        params.put("oauth_callback", callback);
        final String method = "GET";
        OAuth.signOAuth(host, pathRequest, method, https, params, new OAuthTokens(null, null), consumerKey, consumerSecret);
        final HttpResponse response = Network.getRequest(getUrlPrefix() + host + pathRequest, params);

        if (Network.isSuccess(response)) {
            final String line = Network.getResponseData(response);

            int status = STATUS_ERROR;
            if (StringUtils.isNotBlank(line)) {
                assert line != null;
                final MatcherWrapper paramsMatcher1 = new MatcherWrapper(PARAMS_PATTERN_1, line);
                if (paramsMatcher1.find()) {
                    OAtoken = paramsMatcher1.group(1);
                }
                final MatcherWrapper paramsMatcher2 = new MatcherWrapper(PARAMS_PATTERN_2, line);
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
                        status = STATUS_SUCCESS;
                    } catch (ParseException | IOException e) {
                        Log.e("OAuthAuthorizationActivity.requestToken", e);
                    }
                }
            }

            requestTokenHandler.sendEmptyMessage(status);
        } else {
            final String extErrMsg = getExtendedErrorMsg(response);
            if (StringUtils.isNotBlank(extErrMsg)) {
                final Message msg = requestTokenHandler.obtainMessage(STATUS_ERROR_EXT_MSG, extErrMsg);
                requestTokenHandler.sendMessage(msg);
            } else {
                requestTokenHandler.sendEmptyMessage(STATUS_ERROR);
            }
        }
    }

    private void changeToken(final String verifier) {

        int status = NOT_AUTHENTICATED;

        try {
            final Parameters params = new Parameters("oauth_verifier", verifier);

            final String method = "POST";
            OAuth.signOAuth(host, pathAccess, method, https, params, new OAuthTokens(OAtoken, OAtokenSecret), consumerKey, consumerSecret);
            final String line = StringUtils.defaultString(Network.getResponseData(Network.postRequest(getUrlPrefix() + host + pathAccess, params)));

            OAtoken = "";
            OAtokenSecret = "";

            final MatcherWrapper paramsMatcher1 = new MatcherWrapper(PARAMS_PATTERN_1, line);
            if (paramsMatcher1.find()) {
                OAtoken = paramsMatcher1.group(1);
            }
            final MatcherWrapper paramsMatcher2 = new MatcherWrapper(PARAMS_PATTERN_2, line);
            if (paramsMatcher2.find() && paramsMatcher2.groupCount() > 0) {
                OAtokenSecret = paramsMatcher2.group(1);
            }

            if (StringUtils.isBlank(OAtoken) && StringUtils.isBlank(OAtokenSecret)) {
                OAtoken = "";
                OAtokenSecret = "";
                setTokens(null, null, false);
            } else {
                setTokens(OAtoken, OAtokenSecret, true);
                status = AUTHENTICATED;
            }
        } catch (final Exception e) {
            Log.e("OAuthAuthorizationActivity.changeToken", e);
        }

        changeTokensHandler.sendEmptyMessage(status);
    }

    private String getUrlPrefix() {
        return https ? "https://" : "http://";
    }

    private class StartListener implements View.OnClickListener {

        @Override
        public void onClick(final View arg0) {
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
            RxUtils.networkScheduler.createWorker().schedule(new Action0() {
                @Override
                public void call() {
                    requestToken();
                }
            });
        }
    }

    private void exchangeTokens(final String verifier) {
        if (changeTokensDialog == null) {
            changeTokensDialog = new ProgressDialog(this);
            changeTokensDialog.setCancelable(false);
            changeTokensDialog.setMessage(getAuthDialogWait());
        }
        changeTokensDialog.show();

        RxUtils.networkScheduler.createWorker().schedule(new Action0() {
            @Override
            public void call() {
                changeToken(verifier);
            }
        });
    }

    protected abstract ImmutablePair<String, String> getTempTokens();

    protected abstract void setTempTokens(@Nullable String tokenPublic, @Nullable String tokenSecret);

    protected abstract void setTokens(@Nullable String tokenPublic, @Nullable String tokenSecret, boolean enable);

    // get resources from derived class

    protected abstract String getAuthTitle();

    protected String getAuthAgain() {
        return getString(R.string.auth_again);
    }

    protected String getErrAuthInitialize() {
        return getString(R.string.err_auth_initialize);
    }

    protected String getAuthStart() {
        return getString(R.string.auth_start);
    }

    protected abstract String getAuthDialogCompleted();

    protected String getErrAuthProcess() {
        return res.getString(R.string.err_auth_process);
    }

    /**
     * Allows deriving classes to check the response for error messages specific to their OAuth implementation
     *
     * @param response
     *            The error response of the token request
     * @return String with a more detailed error message (user-facing, localized), can be empty
     */
    @SuppressWarnings("static-method")
    protected String getExtendedErrorMsg(final HttpResponse response) {
        return StringUtils.EMPTY;
    }

    protected String getAuthDialogWait() {
        return res.getString(R.string.auth_dialog_waiting, getAuthTitle());
    }

    protected String getAuthExplainShort() {
        return res.getString(R.string.auth_explain_short, getAuthTitle());
    }

    protected String getAuthExplainLong() {
        return res.getString(R.string.auth_explain_long, getAuthTitle());
    }

    protected String getAuthAuthorize() {
        return res.getString(R.string.auth_authorize, getAuthTitle());
    }

    public static class OAuthParameters {
        @NonNull public final String host;
        @NonNull public final String pathRequest;
        @NonNull public final String pathAuthorize;
        @NonNull public final String pathAccess;
        public final boolean https;
        @NonNull public final String consumerKey;
        @NonNull public final String consumerSecret;
        @NonNull public final String callback;

        public OAuthParameters(@NonNull final String host,
                @NonNull final String pathRequest,
                @NonNull final String pathAuthorize,
                @NonNull final String pathAccess,
                final boolean https,
                @NonNull final String consumerKey,
                @NonNull final String consumerSecret,
                @NonNull final String callback) {
            this.host = host;
            this.pathRequest = pathRequest;
            this.pathAuthorize = pathAuthorize;
            this.pathAccess = pathAccess;
            this.https = https;
            this.consumerKey = consumerKey;
            this.consumerSecret = consumerSecret;
            this.callback = callback;
        }

        public void setOAuthExtras(final Intent intent) {
            if (intent != null) {
                intent.putExtra(Intents.EXTRA_OAUTH_HOST, host);
                intent.putExtra(Intents.EXTRA_OAUTH_PATH_REQUEST, pathRequest);
                intent.putExtra(Intents.EXTRA_OAUTH_PATH_AUTHORIZE, pathAuthorize);
                intent.putExtra(Intents.EXTRA_OAUTH_PATH_ACCESS, pathAccess);
                intent.putExtra(Intents.EXTRA_OAUTH_HTTPS, https);
                intent.putExtra(Intents.EXTRA_OAUTH_CONSUMER_KEY, consumerKey);
                intent.putExtra(Intents.EXTRA_OAUTH_CONSUMER_SECRET, consumerSecret);
                intent.putExtra(Intents.EXTRA_OAUTH_CALLBACK, callback);
            }
        }

    }
}
