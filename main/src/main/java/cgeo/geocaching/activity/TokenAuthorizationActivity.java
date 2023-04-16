package cgeo.geocaching.activity;

import cgeo.geocaching.Intents;
import cgeo.geocaching.R;
import cgeo.geocaching.databinding.AuthorizationTokenActivityBinding;
import cgeo.geocaching.network.Network;
import cgeo.geocaching.network.Parameters;
import cgeo.geocaching.ui.WeakReferenceHandler;
import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.utils.AndroidRxUtils;
import cgeo.geocaching.utils.BundleUtils;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.MatcherWrapper;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import java.util.regex.Pattern;

import okhttp3.Response;
import org.apache.commons.lang3.StringUtils;

public abstract class TokenAuthorizationActivity extends AbstractActivity {

    private static final Pattern PATTERN_IS_ERROR = Pattern.compile("error ([\\d]+)");
    private static final Pattern PATTERN_TOKEN = Pattern.compile("([\\w]+)");

    public static final int NOT_AUTHENTICATED = 0;
    public static final int AUTHENTICATED = 1;
    private static final int ERROR_EXT_MSG = 2;

    @NonNull private String urlToken = StringUtils.EMPTY;
    @NonNull private String fieldUsername = StringUtils.EMPTY;
    @NonNull private String fieldPassword = StringUtils.EMPTY;

    private ProgressDialog requestTokenDialog = null;

    protected final Handler requestTokenHandler = new RequestTokenHandler(this);

    private AuthorizationTokenActivityBinding binding;

    private static final class RequestTokenHandler extends WeakReferenceHandler<TokenAuthorizationActivity> {
        RequestTokenHandler(final TokenAuthorizationActivity activity) {
            super(activity);
        }

        @Override
        public void handleMessage(final Message msg) {
            final TokenAuthorizationActivity activity = getReference();
            if (activity != null) {
                Dialogs.dismiss(activity.requestTokenDialog);

                final Button startButton = activity.binding.start;
                startButton.setOnClickListener(new StartListener(activity));
                startButton.setEnabled(true);

                if (msg.what == AUTHENTICATED) {
                    activity.showToast(activity.getAuthDialogCompleted());
                    activity.setResult(RESULT_OK);
                    activity.finish();
                } else if (msg.what == ERROR_EXT_MSG) {
                    String errMsg = activity.getErrAuthInitialize();
                    errMsg += msg.obj != null ? '\n' + msg.obj.toString() : "";
                    activity.showToast(errMsg);
                    startButton.setText(activity.getAuthStart());
                } else {
                    activity.showToast(activity.getErrAuthProcess());
                    startButton.setText(activity.getAuthStart());
                }
            }
        }
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme();
        setUpNavigationEnabled(true);
        binding = AuthorizationTokenActivityBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        final Bundle extras = getIntent().getExtras();
        if (extras != null) {
            urlToken = BundleUtils.getString(extras, Intents.EXTRA_TOKEN_AUTH_URL_TOKEN, urlToken);
            fieldUsername = BundleUtils.getString(extras, Intents.EXTRA_TOKEN_AUTH_USERNAME, fieldUsername);
            fieldPassword = BundleUtils.getString(extras, Intents.EXTRA_TOKEN_AUTH_PASSWORD, fieldPassword);
        }

        setTitle(getAuthTitle());

        binding.auth1.setText(getAuthExplainShort());
        binding.auth2.setText(getAuthExplainLong());
        binding.auth3.setText(getAuthRegisterExplain());

        binding.start.setText(getAuthAuthorize());
        binding.start.setOnClickListener(new StartListener(this));
        enableStartButtonIfReady();

        if (StringUtils.isEmpty(getCreateAccountUrl())) {
            binding.register.setVisibility(View.GONE);
        } else {
            binding.register.setText(getAuthRegister());
            binding.register.setEnabled(true);
            binding.register.setOnClickListener(new RegisterListener());
        }

        binding.start.setText(StringUtils.isBlank(getToken()) ? getAuthStart() : getAuthAgain());

        final EnableStartButtonWatcher enableStartButtonWatcher = new EnableStartButtonWatcher();
        binding.username.addTextChangedListener(enableStartButtonWatcher);
        binding.password.addTextChangedListener(enableStartButtonWatcher);
    }

    @Override
    public void onNewIntent(final Intent intent) {
        super.onNewIntent(intent);  // call super to make lint happy
        setIntent(intent);
    }

    @WorkerThread
    protected void requestToken(final String username, final String password) {
        if (StringUtils.isBlank(username) || StringUtils.isBlank(password)) {
            ActivityMixin.showToast(this, R.string.err_missing_auth);
            requestTokenHandler.sendEmptyMessage(NOT_AUTHENTICATED);
            return;
        }

        final String nam = StringUtils.defaultString(username);
        final String pwd = StringUtils.defaultString(password);

        final Parameters params = new Parameters(fieldUsername, nam, fieldPassword, pwd);

        int status = NOT_AUTHENTICATED;
        String message = StringUtils.EMPTY;

        try {
            final Response response = Network.postRequest(urlToken, params).blockingGet();
            if (response.isSuccessful()) {
                final String line = StringUtils.defaultString(Network.getResponseData(response));
                final MatcherWrapper errorMatcher = new MatcherWrapper(getPatternIsError(), line);
                final MatcherWrapper tokenMatcher = new MatcherWrapper(getPatternToken(), line);
                if (errorMatcher.find()) {
                    status = ERROR_EXT_MSG;
                    message = getExtendedErrorMsg(errorMatcher.group(1));
                } else if (tokenMatcher.find()) {
                    status = AUTHENTICATED;
                    setToken(tokenMatcher.group(1));
                }
            } else {
                message = getExtendedErrorMsg(response);
            }
        } catch (final Exception e) {
            Log.e("TokenAuthorizationActivity:", e);
        }

        if (StringUtils.isNotBlank(message)) {
            final Message msg = requestTokenHandler.obtainMessage(status, message);
            requestTokenHandler.sendMessage(msg);
        } else {
            requestTokenHandler.sendEmptyMessage(status);
        }
    }

    private static class StartListener extends WeakReferenceHandler<TokenAuthorizationActivity> implements View.OnClickListener {
        StartListener(final TokenAuthorizationActivity activity) {
            super(activity);
        }

        @Override
        public void onClick(final View view) {
            final TokenAuthorizationActivity activity = getReference();
            if (activity != null) {
                if (activity.requestTokenDialog == null) {
                    activity.requestTokenDialog = new ProgressDialog(activity);
                    activity.requestTokenDialog.setCancelable(false);
                    activity.requestTokenDialog.setMessage(activity.getAuthDialogWait());
                }
                activity.requestTokenDialog.show();

                final Button startButton = activity.binding.start;
                startButton.setEnabled(false);
                startButton.setOnTouchListener(null);
                startButton.setOnClickListener(null);

                final String username = activity.binding.username.getText().toString();
                final String password = activity.binding.password.getText().toString();

                AndroidRxUtils.networkScheduler.scheduleDirect(() -> activity.requestToken(username, password));
            }
        }
    }

    private class RegisterListener implements View.OnClickListener {

        @Override
        public void onClick(final View view) {
            final Activity activity = TokenAuthorizationActivity.this;
            try {
                activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getCreateAccountUrl())));
            } catch (final ActivityNotFoundException e) {
                Log.e("Cannot find suitable activity", e);
                ActivityMixin.showToast(activity, R.string.err_application_no);
            }
        }
    }

    // get resources from derived class

    protected abstract String getCreateAccountUrl();

    protected abstract void setToken(String token);

    protected abstract String getToken();

    protected abstract String getAuthTitle();

    protected Pattern getPatternIsError() {
        return PATTERN_IS_ERROR;
    }

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
     * @param response The error response of the token request
     * @return String with a more detailed error message (user-facing, localized), can be empty
     */
    protected String getExtendedErrorMsg(final Response response) {
        return StringUtils.EMPTY;
    }

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
        return res.getString(R.string.auth_authorize);
    }

    protected String getAuthRegisterExplain() {
        return res.getString(R.string.auth_register_explain);
    }

    protected String getAuthRegister() {
        return res.getString(R.string.auth_register);
    }

    /**
     * Enable or disable the start button depending on login/password field.
     * If both fields are not empty, button is enabled.
     */
    protected void enableStartButtonIfReady() {
        binding.start.setEnabled(StringUtils.isNotEmpty(binding.username.getText()) &&
                StringUtils.isNotEmpty(binding.password.getText()));
    }

    /**
     * A TextWatcher to monitor changes on usernameEditText and passwordEditText for enabling start button
     * dynamically.
     */
    private class EnableStartButtonWatcher implements TextWatcher {
        @Override
        public void beforeTextChanged(final CharSequence s, final int start, final int count, final int after) {
            // empty
        }

        @Override
        public void onTextChanged(final CharSequence s, final int start, final int before, final int count) {
            // empty
        }

        @Override
        public void afterTextChanged(final Editable s) {
            enableStartButtonIfReady();
        }
    }

    public static class TokenAuthParameters {
        @NonNull public final String urlToken;
        @NonNull public final String fieldUsername;
        @NonNull public final String fieldPassword;

        public TokenAuthParameters(@NonNull final String urlToken,
                                   @NonNull final String fieldUsername,
                                   @NonNull final String fieldPassword) {
            this.urlToken = urlToken;
            this.fieldUsername = fieldUsername;
            this.fieldPassword = fieldPassword;
        }

        public void setTokenAuthExtras(final Intent intent) {
            if (intent != null) {
                intent.putExtra(Intents.EXTRA_TOKEN_AUTH_URL_TOKEN, urlToken);
                intent.putExtra(Intents.EXTRA_TOKEN_AUTH_USERNAME, fieldUsername);
                intent.putExtra(Intents.EXTRA_TOKEN_AUTH_PASSWORD, fieldPassword);
            }
        }

    }

    @Override
    public void finish() {
        Dialogs.dismiss(requestTokenDialog);
        super.finish();
    }
}
