package cgeo.geocaching.settings;

import cgeo.geocaching.Intents;
import cgeo.geocaching.R;
import cgeo.geocaching.activity.AbstractActivity;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.connector.ConnectorFactory;
import cgeo.geocaching.databinding.AuthorizationCredentialsActivityBinding;
import cgeo.geocaching.enumerations.StatusCode;
import cgeo.geocaching.ui.TextParam;
import cgeo.geocaching.ui.dialog.SimpleDialog;
import cgeo.geocaching.utils.AndroidRxUtils;
import cgeo.geocaching.utils.BundleUtils;
import cgeo.geocaching.utils.Log;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.reactivex.rxjava3.core.Observable;
import org.apache.commons.lang3.StringUtils;

public abstract class AbstractCredentialsAuthorizationActivity extends AbstractActivity {

    @NonNull private String connectorUsername = StringUtils.EMPTY;
    @NonNull private String connectorPassword = StringUtils.EMPTY;

    private AuthorizationCredentialsActivityBinding binding;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme();
        binding = AuthorizationCredentialsActivityBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        final Bundle extras = getIntent().getExtras();
        if (extras != null) {
            connectorUsername = BundleUtils.getString(extras, Intents.EXTRA_CREDENTIALS_AUTH_USERNAME, connectorUsername);
            connectorPassword = BundleUtils.getString(extras, Intents.EXTRA_CREDENTIALS_AUTH_PASSWORD, connectorPassword);
        }

        setTitle(getAuthTitle());

        binding.auth1.setText(getAuthExplainShort());
        binding.auth2.setText(getAuthExplainLong());
        binding.auth3.setText(getAuthRegisterExplain());

        binding.check.setText(getAuthCheck());
        binding.check.setOnClickListener(new CheckListener());
        enableCheckButtonIfReady();

        binding.username.setText(connectorUsername);
        binding.password.setText(connectorPassword);
        enableCheckButtonIfReady();

        if (StringUtils.isEmpty(getCreateAccountUrl())) {
            binding.register.setVisibility(View.GONE);
        } else {
            binding.register.setText(getAuthRegister());
            binding.register.setEnabled(true);
            binding.register.setOnClickListener(new RegisterListener());
        }

        final EnableStartButtonWatcher enableStartButtonWatcher = new EnableStartButtonWatcher();
        binding.username.addTextChangedListener(enableStartButtonWatcher);
        binding.password.addTextChangedListener(enableStartButtonWatcher);
    }

    @Override
    public void onNewIntent(final Intent intent) {
        super.onNewIntent(intent);  // call super to make lint happy
        setIntent(intent);
    }

    private void checkCredentials(final String username, final String password) {
        if (StringUtils.isBlank(username) || StringUtils.isBlank(password)) {
            ActivityMixin.showToast(this, R.string.err_missing_auth);
            return;
        }

        final String nam = StringUtils.defaultString(username);
        final String pwd = StringUtils.defaultString(password);

        final Credentials credentials = new Credentials(nam, pwd);
        final AbstractCredentialsAuthorizationActivity authorizationActivity = this;

        final ProgressDialog loginDialog = ProgressDialog.show(authorizationActivity,
                res.getString(R.string.init_login_popup), getAuthDialogWait(), true);
        loginDialog.setCancelable(false);

        AndroidRxUtils.bindActivity(authorizationActivity, Observable.defer(() -> Observable.just(checkCredentials(credentials)))).subscribeOn(AndroidRxUtils.networkScheduler).subscribe(statusCode -> {
            loginDialog.dismiss();
            if (statusCode == StatusCode.NO_ERROR) {
                setCredentials(credentials);
                ConnectorFactory.forceRelog();
                showToast(getAuthDialogCompleted());
                setResult(RESULT_OK);
                finish();
            } else {
                SimpleDialog.of(authorizationActivity).setTitle(R.string.init_login_popup)
                        .setMessage(TextParam.concat(TextParam.id(R.string.init_login_popup_failed_reason, ""),
                                TextParam.id(statusCode.errorString)).setMovement(true))
                        .show();
                binding.check.setText(getAuthCheckAgain());
                binding.check.setOnClickListener(new CheckListener());
                binding.check.setEnabled(true);
            }
        });
    }

    private class CheckListener implements View.OnClickListener {

        @Override
        public void onClick(final View view) {
            hideKeyboard();

            binding.check.setEnabled(false);
            binding.check.setOnTouchListener(null);
            binding.check.setOnClickListener(null);

            final String username = binding.username.getText().toString();
            final String password = binding.password.getText().toString();

            checkCredentials(username, password);
        }
    }

    private class RegisterListener implements View.OnClickListener {

        @Override
        public void onClick(final View view) {
            final Activity activity = AbstractCredentialsAuthorizationActivity.this;
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

    protected abstract void setCredentials(Credentials credentials);

    protected abstract StatusCode checkCredentials(Credentials credentials);

    protected abstract String getAuthTitle();

    protected String getAuthDialogCompleted() {
        return res.getString(R.string.auth_dialog_completed_geokrety, getAuthTitle());
    }

    protected String getAuthDialogWait() {
        return res.getString(R.string.auth_dialog_waiting, getAuthTitle());
    }

    protected String getAuthExplainShort() {
        return res.getString(R.string.auth_credentials_explain_short, getAuthTitle());
    }

    protected String getAuthExplainLong() {
        return res.getString(R.string.auth_credentials_explain_long, getAuthTitle());
    }

    protected String getAuthCheck() {
        return res.getString(R.string.auth_check);
    }

    protected String getAuthCheckAgain() {
        return res.getString(R.string.auth_check_again);
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
    protected void enableCheckButtonIfReady() {
        binding.check.setEnabled(StringUtils.isNotEmpty(binding.username.getText()) &&
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
            enableCheckButtonIfReady();
        }
    }

    public static class CredentialsAuthParameters {
        @Nullable public final String username;
        @Nullable public final String password;

        public CredentialsAuthParameters(@Nullable final String username, @Nullable final String password) {
            this.username = username;
            this.password = password;
        }

        public void setCredentialsAuthExtras(final Intent intent) {
            if (intent != null) {
                intent.putExtra(Intents.EXTRA_CREDENTIALS_AUTH_USERNAME, StringUtils.defaultIfBlank(username, StringUtils.EMPTY));
                intent.putExtra(Intents.EXTRA_CREDENTIALS_AUTH_PASSWORD, StringUtils.defaultIfBlank(password, StringUtils.EMPTY));
            }
        }

    }
}
