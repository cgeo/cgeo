package cgeo.geocaching.activity;

import butterknife.InjectView;

import cgeo.geocaching.Intents;
import cgeo.geocaching.R;
import cgeo.geocaching.network.Network;
import cgeo.geocaching.network.Parameters;
import cgeo.geocaching.utils.BundleUtils;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.MatcherWrapper;
import cgeo.geocaching.utils.RxUtils;

import ch.boye.httpclientandroidlib.HttpResponse;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.NonNull;

import rx.functions.Action0;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
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

public abstract class TokenAuthorizationActivity extends AbstractActivity {

    private static final Pattern PATTERN_IS_ERROR = Pattern.compile("error ([\\d]+)");
    private static final Pattern PATTERN_TOKEN = Pattern.compile("([\\w]+)");

    public static final int NOT_AUTHENTICATED = 0;
    public static final int AUTHENTICATED = 1;
    private static final int ERROR_EXT_MSG = 2;

    @NonNull private String urlToken = StringUtils.EMPTY;
    @NonNull private String urlRegister = StringUtils.EMPTY;
    @NonNull private String fieldUsername = StringUtils.EMPTY;
    @NonNull private String fieldPassword = StringUtils.EMPTY;

    @InjectView(R.id.start) protected Button startButton;
    @InjectView(R.id.register) protected Button registerButton;
    @InjectView(R.id.auth_1) protected TextView auth1;
    @InjectView(R.id.auth_2) protected TextView auth2;
    @InjectView(R.id.username) protected EditText usernameEditText;
    @InjectView(R.id.password) protected EditText passwordEditText;

    private ProgressDialog requestTokenDialog = null;

    protected final Handler requestTokenHandler = new Handler() {

        @Override
        public void handleMessage(final Message msg) {
            if (requestTokenDialog != null && requestTokenDialog.isShowing()) {
                requestTokenDialog.dismiss();
            }

            startButton.setOnClickListener(new StartListener());
            startButton.setEnabled(true);

            if (msg.what == AUTHENTICATED) {
                showToast(getAuthDialogCompleted());
                setResult(RESULT_OK);
                finish();
            } else if (msg.what == ERROR_EXT_MSG) {
                String errMsg = getErrAuthInitialize();
                errMsg += msg.obj != null ? '\n' + msg.obj.toString() : "";
                showToast(errMsg);
                startButton.setText(getAuthStart());
            } else {
                showToast(getErrAuthProcess());
                startButton.setText(getAuthStart());
            }
        }

    };

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        onCreate(savedInstanceState, R.layout.authorization_token_activity);

        final Bundle extras = getIntent().getExtras();
        if (extras != null) {
            urlToken = BundleUtils.getString(extras, Intents.EXTRA_TOKEN_AUTH_URL_TOKEN, urlToken);
            urlRegister = BundleUtils.getString(extras, Intents.EXTRA_TOKEN_AUTH_URL_REGISTER, urlRegister);
            fieldUsername = BundleUtils.getString(extras, Intents.EXTRA_TOKEN_AUTH_USERNAME, fieldUsername);
            fieldPassword = BundleUtils.getString(extras, Intents.EXTRA_TOKEN_AUTH_PASSWORD, fieldPassword);
        }

        setTitle(getAuthTitle());

        auth1.setText(getAuthExplainShort());
        auth2.setText(getAuthExplainLong());

        startButton.setText(getAuthAuthorize());
        startButton.setEnabled(true);
        startButton.setOnClickListener(new StartListener());

        registerButton.setText(getAuthRegister());
        registerButton.setEnabled(true);
        registerButton.setOnClickListener(new RegisterListener());

        startButton.setText(StringUtils.isBlank(getToken()) ? getAuthStart() : getAuthAgain());
    }

    @Override
    public void onNewIntent(final Intent intent) {
        setIntent(intent);
    }

    protected void requestToken(final String username, final String password) {
        final String nam = StringUtils.defaultString(username);
        final String pwd = StringUtils.defaultString(password);

        if (StringUtils.isBlank(username) || StringUtils.isBlank(password)) {
            ActivityMixin.showToast(this, R.string.err_missing_auth);
            requestTokenHandler.sendEmptyMessage(NOT_AUTHENTICATED);
            return;
        }

        final Parameters params = new Parameters(fieldUsername, nam, fieldPassword, pwd);
        final HttpResponse response = Network.postRequest(urlToken, params);

        int status = NOT_AUTHENTICATED;
        String message = StringUtils.EMPTY;

        if (Network.isSuccess(response)) {
            final String line = StringUtils.defaultString(Network.getResponseData(response));
            try {
                final MatcherWrapper errorMatcher = new MatcherWrapper(getPatternIsError(), line);
                final MatcherWrapper tokenMatcher = new MatcherWrapper(getPatternToken(), line);
                if (errorMatcher.find()) {
                    status = ERROR_EXT_MSG;
                    message = getExtendedErrorMsg(errorMatcher.group(1));
                } else if (tokenMatcher.find()) {
                    status = AUTHENTICATED;
                    setToken(tokenMatcher.group(1));
                }
            } catch (final Exception e) {
                Log.e("TokenAuthorizationActivity:", e);
            }

        } else {
            message = getExtendedErrorMsg(response);
        }

        if (StringUtils.isNotBlank(message)) {
            final Message msg = requestTokenHandler.obtainMessage(status, message);
            requestTokenHandler.sendMessage(msg);
        } else {
            requestTokenHandler.sendEmptyMessage(status);
        }
    }

    private class StartListener implements View.OnClickListener {

        @Override
        public void onClick(final View view) {
            if (requestTokenDialog == null) {
                requestTokenDialog = new ProgressDialog(TokenAuthorizationActivity.this);
                requestTokenDialog.setCancelable(false);
                requestTokenDialog.setMessage(getAuthDialogWait());
            }
            requestTokenDialog.show();
            startButton.setEnabled(false);
            startButton.setOnTouchListener(null);
            startButton.setOnClickListener(null);

            final String username = usernameEditText.getText().toString();
            final String password = passwordEditText.getText().toString();

            RxUtils.networkScheduler.createWorker().schedule(new Action0() {
                @Override
                public void call() {
                    requestToken(username, password);
                }
            });
        }
    }

    private class RegisterListener implements View.OnClickListener {

        @Override
        public void onClick(final View view) {
            final Activity activity = TokenAuthorizationActivity.this;
            try {
                activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(urlRegister)));
            } catch (final ActivityNotFoundException e) {
                Log.e("Cannot find suitable activity", e);
                ActivityMixin.showToast(activity, R.string.err_application_no);
            }
        }
    }

    // get resources from derived class

    protected abstract void setToken(String token);

    protected abstract String getToken();

    protected abstract String getAuthTitle();

    @SuppressWarnings("static-method")
    protected Pattern getPatternIsError() {
        return PATTERN_IS_ERROR;
    }

    @SuppressWarnings("static-method")
    protected Pattern getPatternToken() {
        return PATTERN_TOKEN;
    }

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
     * Allows deriving classes to check the response for error messages specific to their Token implementation
     *
     * @param response
     *            The error response of the token request
     * @return String with a more detailed error message (user-facing, localized), can be empty
     */
    @SuppressWarnings("static-method")
    protected String getExtendedErrorMsg(final HttpResponse response) {
        return StringUtils.EMPTY;
    }

    @SuppressWarnings("static-method")
    protected String getExtendedErrorMsg(@SuppressWarnings("unused") final String response) {
        return StringUtils.EMPTY;
    }

    protected String getAuthDialogWait() {
        return res.getString(R.string.auth_dialog_waiting, getAuthTitle());
    }

    protected String getAuthExplainShort() {
        return res.getString(R.string.auth_token_explain_short, getAuthTitle());
    }

    protected String getAuthExplainLong() {
        return res.getString(R.string.auth_token_explain_long, getAuthTitle());
    }

    protected String getAuthAuthorize() {
        return res.getString(R.string.auth_authorize, getAuthTitle());
    }

    protected String getAuthRegister() {
        return res.getString(R.string.auth_token_register, getAuthTitle());
    }

    public static class TokenAuthParameters {
        @NonNull public final String urlToken;
        @NonNull public final String urlRegister;
        @NonNull public final String fieldUsername;
        @NonNull public final String fieldPassword;

        public TokenAuthParameters(@NonNull final String urlToken,
                               @NonNull final String urlRegister,
                               @NonNull final String fieldUsername,
                               @NonNull final String fieldPassword) {
            this.urlToken = urlToken;
            this.urlRegister = urlRegister;
            this.fieldUsername = fieldUsername;
            this.fieldPassword = fieldPassword;
        }

        public void setTokenAuthExtras(final Intent intent) {
            if (intent != null) {
                intent.putExtra(Intents.EXTRA_TOKEN_AUTH_URL_TOKEN, urlToken);
                intent.putExtra(Intents.EXTRA_TOKEN_AUTH_URL_REGISTER, urlRegister);
                intent.putExtra(Intents.EXTRA_TOKEN_AUTH_USERNAME, fieldUsername);
                intent.putExtra(Intents.EXTRA_TOKEN_AUTH_PASSWORD, fieldPassword);
            }
        }

    }
}
