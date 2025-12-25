// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.activity

import cgeo.geocaching.Intents
import cgeo.geocaching.R
import cgeo.geocaching.databinding.AuthorizationTokenActivityBinding
import cgeo.geocaching.network.Network
import cgeo.geocaching.network.Parameters
import cgeo.geocaching.ui.ViewUtils
import cgeo.geocaching.ui.WeakReferenceHandler
import cgeo.geocaching.ui.dialog.Dialogs
import cgeo.geocaching.utils.AndroidRxUtils
import cgeo.geocaching.utils.BundleUtils
import cgeo.geocaching.utils.Log
import cgeo.geocaching.utils.MatcherWrapper

import android.annotation.SuppressLint
import android.app.Activity
import android.app.ProgressDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.text.TextWatcher
import android.view.View
import android.widget.Button

import androidx.annotation.NonNull
import androidx.annotation.WorkerThread

import java.util.regex.Pattern

import okhttp3.Response
import org.apache.commons.lang3.StringUtils

abstract class TokenAuthorizationActivity : AbstractActivity() {

    private static val PATTERN_IS_ERROR: Pattern = Pattern.compile("error ([\\d]+)")
    private static val PATTERN_TOKEN: Pattern = Pattern.compile("([\\w]+)")

    public static val NOT_AUTHENTICATED: Int = 0
    public static val AUTHENTICATED: Int = 1
    private static val ERROR_EXT_MSG: Int = 2

    private var urlToken: String = StringUtils.EMPTY
    private var fieldUsername: String = StringUtils.EMPTY
    private var fieldPassword: String = StringUtils.EMPTY

    private var requestTokenDialog: ProgressDialog = null

    protected val requestTokenHandler: Handler = RequestTokenHandler(this)

    private AuthorizationTokenActivityBinding binding

    private static class RequestTokenHandler : WeakReferenceHandler()<TokenAuthorizationActivity> {
        RequestTokenHandler(final TokenAuthorizationActivity activity) {
            super(activity)
        }

        override         public Unit handleMessage(final Message msg) {
            val activity: TokenAuthorizationActivity = getReference()
            if (activity != null) {
                Dialogs.dismiss(activity.requestTokenDialog)

                val startButton: Button = activity.binding.start
                startButton.setOnClickListener(StartListener(activity))
                startButton.setEnabled(true)

                if (msg.what == AUTHENTICATED) {
                    activity.showToast(activity.getAuthDialogCompleted())
                    activity.setResult(RESULT_OK)
                    activity.finish()
                } else if (msg.what == ERROR_EXT_MSG) {
                    String errMsg = activity.getErrAuthInitialize()
                    errMsg += msg.obj != null ? '\n' + msg.obj.toString() : ""
                    activity.showToast(errMsg)
                    startButton.setText(activity.getAuthStart())
                } else {
                    activity.showToast(activity.getErrAuthProcess())
                    startButton.setText(activity.getAuthStart())
                }
            }
        }
    }

    override     public Unit onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState)
        setTheme()
        setUpNavigationEnabled(true)
        binding = AuthorizationTokenActivityBinding.inflate(getLayoutInflater())
        setContentView(binding.getRoot())

        val extras: Bundle = getIntent().getExtras()
        if (extras != null) {
            urlToken = BundleUtils.getString(extras, Intents.EXTRA_TOKEN_AUTH_URL_TOKEN, urlToken)
            fieldUsername = BundleUtils.getString(extras, Intents.EXTRA_TOKEN_AUTH_USERNAME, fieldUsername)
            fieldPassword = BundleUtils.getString(extras, Intents.EXTRA_TOKEN_AUTH_PASSWORD, fieldPassword)
        }

        setTitle(getAuthTitle())

        binding.auth1.setText(getAuthExplainShort())
        binding.auth2.setText(getAuthExplainLong())
        binding.auth3.setText(getAuthRegisterExplain())

        binding.start.setText(getAuthAuthorize())
        binding.start.setOnClickListener(StartListener(this))
        enableStartButtonIfReady()

        if (StringUtils.isEmpty(getCreateAccountUrl())) {
            binding.register.setVisibility(View.GONE)
        } else {
            binding.register.setText(getAuthRegister())
            binding.register.setEnabled(true)
            binding.register.setOnClickListener(RegisterListener())
        }

        binding.start.setText(StringUtils.isBlank(getToken()) ? getAuthStart() : getAuthAgain())

        val enableStartButtonWatcher: TextWatcher = ViewUtils.createSimpleWatcher(s -> enableStartButtonIfReady())
        binding.username.addTextChangedListener(enableStartButtonWatcher)
        binding.password.addTextChangedListener(enableStartButtonWatcher)
    }

    override     public Unit onNewIntent(final Intent intent) {
        super.onNewIntent(intent);  // call super to make lint happy
        setIntent(intent)
    }

    @WorkerThread
    protected Unit requestToken(final String username, final String password) {
        if (StringUtils.isBlank(username) || StringUtils.isBlank(password)) {
            ActivityMixin.showToast(this, R.string.err_missing_auth)
            requestTokenHandler.sendEmptyMessage(NOT_AUTHENTICATED)
            return
        }

        val nam: String = StringUtils.defaultString(username)
        val pwd: String = StringUtils.defaultString(password)

        val params: Parameters = Parameters(fieldUsername, nam, fieldPassword, pwd)

        Int status = NOT_AUTHENTICATED
        String message = StringUtils.EMPTY

        try {
            val response: Response = Network.postRequest(urlToken, params).blockingGet()
            if (response.isSuccessful()) {
                val line: String = StringUtils.defaultString(Network.getResponseData(response))
                val errorMatcher: MatcherWrapper = MatcherWrapper(getPatternIsError(), line)
                val tokenMatcher: MatcherWrapper = MatcherWrapper(getPatternToken(), line)
                if (errorMatcher.find()) {
                    status = ERROR_EXT_MSG
                    message = getExtendedErrorMsg(errorMatcher.group(1))
                } else if (tokenMatcher.find()) {
                    status = AUTHENTICATED
                    setToken(tokenMatcher.group(1))
                }
            } else {
                message = getExtendedErrorMsg(response)
            }
        } catch (final Exception e) {
            Log.e("TokenAuthorizationActivity:", e)
        }

        if (StringUtils.isNotBlank(message)) {
            val msg: Message = requestTokenHandler.obtainMessage(status, message)
            requestTokenHandler.sendMessage(msg)
        } else {
            requestTokenHandler.sendEmptyMessage(status)
        }
    }

    private static class StartListener : WeakReferenceHandler()<TokenAuthorizationActivity> : View.OnClickListener {
        StartListener(final TokenAuthorizationActivity activity) {
            super(activity)
        }

        @SuppressLint("ClickableViewAccessibility")
        override         public Unit onClick(final View view) {
            val activity: TokenAuthorizationActivity = getReference()
            if (activity != null) {
                if (activity.requestTokenDialog == null) {
                    activity.requestTokenDialog = ProgressDialog(activity)
                    activity.requestTokenDialog.setCancelable(false)
                    activity.requestTokenDialog.setMessage(activity.getAuthDialogWait())
                }
                activity.requestTokenDialog.show()

                val startButton: Button = activity.binding.start
                startButton.setEnabled(false)
                startButton.setOnTouchListener(null)
                startButton.setOnClickListener(null)

                val username: String = ViewUtils.getEditableText(activity.binding.username.getText())
                val password: String = ViewUtils.getEditableText(activity.binding.password.getText())

                AndroidRxUtils.networkScheduler.scheduleDirect(() -> activity.requestToken(username, password))
            }
        }
    }

    private class RegisterListener : View.OnClickListener {

        override         public Unit onClick(final View view) {
            val activity: Activity = TokenAuthorizationActivity.this
            try {
                activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(getCreateAccountUrl())))
            } catch (final ActivityNotFoundException e) {
                Log.e("Cannot find suitable activity", e)
                ActivityMixin.showToast(activity, R.string.err_application_no)
            }
        }
    }

    // get resources from derived class

    protected abstract String getCreateAccountUrl()

    protected abstract Unit setToken(String token)

    protected abstract String getToken()

    protected abstract String getAuthTitle()

    protected Pattern getPatternIsError() {
        return PATTERN_IS_ERROR
    }

    protected Pattern getPatternToken() {
        return PATTERN_TOKEN
    }

    protected String getAuthAgain() {
        return getString(R.string.auth_again)
    }

    protected String getErrAuthInitialize() {
        return getString(R.string.err_auth_initialize)
    }

    protected String getAuthStart() {
        return getString(R.string.auth_start)
    }

    protected abstract String getAuthDialogCompleted()

    protected String getErrAuthProcess() {
        return res.getString(R.string.err_auth_process)
    }

    /**
     * Allows deriving classes to check the response for error messages specific to their Token implementation
     *
     * @param response The error response of the token request
     * @return String with a more detailed error message (user-facing, localized), can be empty
     */
    protected String getExtendedErrorMsg(final Response response) {
        return StringUtils.EMPTY
    }

    protected String getExtendedErrorMsg(@SuppressWarnings("unused") final String response) {
        return StringUtils.EMPTY
    }

    protected String getAuthDialogWait() {
        return res.getString(R.string.auth_dialog_waiting, getAuthTitle())
    }

    protected String getAuthExplainShort() {
        return res.getString(R.string.auth_token_explain_short, getAuthTitle())
    }

    protected String getAuthExplainLong() {
        return res.getString(R.string.auth_token_explain_long, getAuthTitle())
    }

    protected String getAuthAuthorize() {
        return res.getString(R.string.auth_authorize)
    }

    protected String getAuthRegisterExplain() {
        return res.getString(R.string.auth_register_explain)
    }

    protected String getAuthRegister() {
        return res.getString(R.string.auth_register)
    }

    /**
     * Enable or disable the start button depending on login/password field.
     * If both fields are not empty, button is enabled.
     */
    protected Unit enableStartButtonIfReady() {
        binding.start.setEnabled(StringUtils.isNotEmpty(binding.username.getText()) &&
                StringUtils.isNotEmpty(binding.password.getText()))
    }

    public static class TokenAuthParameters {
        public final String urlToken
        public final String fieldUsername
        public final String fieldPassword

        public TokenAuthParameters(final String urlToken,
                                   final String fieldUsername,
                                   final String fieldPassword) {
            this.urlToken = urlToken
            this.fieldUsername = fieldUsername
            this.fieldPassword = fieldPassword
        }

        public Unit setTokenAuthExtras(final Intent intent) {
            if (intent != null) {
                intent.putExtra(Intents.EXTRA_TOKEN_AUTH_URL_TOKEN, urlToken)
                intent.putExtra(Intents.EXTRA_TOKEN_AUTH_USERNAME, fieldUsername)
                intent.putExtra(Intents.EXTRA_TOKEN_AUTH_PASSWORD, fieldPassword)
            }
        }

    }

    override     public Unit finish() {
        Dialogs.dismiss(requestTokenDialog)
        super.finish()
    }
}
