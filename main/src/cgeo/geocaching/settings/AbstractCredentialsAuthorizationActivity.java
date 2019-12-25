package cgeo.geocaching.settings;

import cgeo.geocaching.Intents;
import cgeo.geocaching.R;
import cgeo.geocaching.activity.AbstractActivity;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.enumerations.StatusCode;
import cgeo.geocaching.ui.dialog.Dialogs;
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
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.concurrent.Callable;

import butterknife.BindView;
import io.reactivex.Observable;
import io.reactivex.functions.Consumer;
import org.apache.commons.lang3.StringUtils;

public abstract class AbstractCredentialsAuthorizationActivity extends AbstractActivity {

    @NonNull private String connectorUsername = StringUtils.EMPTY;
    @NonNull private String connectorPassword = StringUtils.EMPTY;

    @BindView(R.id.check) protected Button checkButton;
    @BindView(R.id.register) protected Button registerButton;
    @BindView(R.id.auth_1) protected TextView auth1;
    @BindView(R.id.auth_2) protected TextView auth2;
    @BindView(R.id.username) protected EditText usernameEditText;
    @BindView(R.id.password) protected EditText passwordEditText;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        onCreate(savedInstanceState, R.layout.authorization_credentials_activity);

        final Bundle extras = getIntent().getExtras();
        if (extras != null) {
            connectorUsername = BundleUtils.getString(extras, Intents.EXTRA_CREDENTIALS_AUTH_USERNAME, connectorUsername);
            connectorPassword = BundleUtils.getString(extras, Intents.EXTRA_CREDENTIALS_AUTH_PASSWORD, connectorPassword);
        }

        setTitle(getAuthTitle());

        auth1.setText(getAuthExplainShort());
        auth2.setText(getAuthExplainLong());

        checkButton.setText(getAuthCheck());
        checkButton.setOnClickListener(new CheckListener());
        enableCheckButtonIfReady();

        usernameEditText.setText(connectorUsername);
        passwordEditText.setText(connectorPassword);
        enableCheckButtonIfReady();

        if (StringUtils.isEmpty(getCreateAccountUrl())) {
            registerButton.setVisibility(View.GONE);
        } else {
            registerButton.setText(getAuthRegister());
            registerButton.setEnabled(true);
            registerButton.setOnClickListener(new RegisterListener());
        }

        final EnableStartButtonWatcher enableStartButtonWatcher = new EnableStartButtonWatcher();
        usernameEditText.addTextChangedListener(enableStartButtonWatcher);
        passwordEditText.addTextChangedListener(enableStartButtonWatcher);
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

        AndroidRxUtils.bindActivity(authorizationActivity, Observable.defer(new Callable<Observable<StatusCode>>() {
            @Override
            public Observable<StatusCode> call() {
                return Observable.just(checkCredentials(credentials));
            }
        })).subscribeOn(AndroidRxUtils.networkScheduler).subscribe(new Consumer<StatusCode>() {
            @Override
            public void accept(final StatusCode statusCode) {
                loginDialog.dismiss();
                if (statusCode == StatusCode.NO_ERROR) {
                    setCredentials(credentials);
                    showToast(getAuthDialogCompleted());
                    setResult(RESULT_OK);
                    finish();
                } else {
                    Dialogs.message(authorizationActivity, R.string.init_login_popup,
                        res.getString(R.string.init_login_popup_failed_reason, statusCode.getErrorString(res))
                    );
                    checkButton.setText(getAuthCheckAgain());
                    checkButton.setOnClickListener(new CheckListener());
                    checkButton.setEnabled(true);
                }
            }
        });
    }

    private class CheckListener implements View.OnClickListener {

        @Override
        public void onClick(final View view) {
            hideKeyboard();

            checkButton.setEnabled(false);
            checkButton.setOnTouchListener(null);
            checkButton.setOnClickListener(null);

            final String username = usernameEditText.getText().toString();
            final String password = passwordEditText.getText().toString();

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

    protected String getAuthRegister() {
        return res.getString(R.string.auth_register);
    }

    /**
     * Enable or disable the start button depending on login/password field.
     * If both fields are not empty, button is enabled.
     *
     */
    protected void enableCheckButtonIfReady() {
        checkButton.setEnabled(StringUtils.isNotEmpty(usernameEditText.getText()) &&
                StringUtils.isNotEmpty(passwordEditText.getText()));
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
