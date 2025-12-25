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
import cgeo.geocaching.databinding.AuthorizationActivityBinding
import cgeo.geocaching.network.Network
import cgeo.geocaching.network.OAuth
import cgeo.geocaching.network.OAuthTokens
import cgeo.geocaching.network.Parameters
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
import android.view.View
import android.widget.Button

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.annotation.WorkerThread

import java.util.regex.Pattern

import okhttp3.HttpUrl
import okhttp3.Response
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.tuple.ImmutablePair

abstract class OAuthAuthorizationActivity : AbstractActivity() {

    public static val NOT_AUTHENTICATED: Int = 0
    public static val AUTHENTICATED: Int = 1

    private static val STATUS_ERROR: Int = 0
    private static val STATUS_SUCCESS: Int = 1
    private static val STATUS_ERROR_EXT_MSG: Int = 2
    private static val PARAMS_PATTERN_1: Pattern = Pattern.compile("oauth_token=([\\w_.-]+)")
    private static val PARAMS_PATTERN_2: Pattern = Pattern.compile("oauth_token_secret=([\\w_.-]+)")

    private var host: String = StringUtils.EMPTY
    private var pathRequest: String = StringUtils.EMPTY
    private var pathAuthorize: String = StringUtils.EMPTY
    private var pathAccess: String = StringUtils.EMPTY
    private var https: Boolean = false
    private var consumerKey: String = StringUtils.EMPTY
    private var consumerSecret: String = StringUtils.EMPTY
    private var callback: String = StringUtils.EMPTY
    private var oAtoken: String = null
    private var oAtokenSecret: String = null

    private var requestTokenDialog: ProgressDialog = null
    private var changeTokensDialog: ProgressDialog = null

    private var lastVerifier: String = null

    private val requestTokenHandler: Handler = RequestTokenHandler(this)
    private val changeTokensHandler: Handler = ChangeTokensHandler(this)

    private AuthorizationActivityBinding binding

    private static class RequestTokenHandler : WeakReferenceHandler()<OAuthAuthorizationActivity> {

        RequestTokenHandler(final OAuthAuthorizationActivity activity) {
            super(activity)
        }

        override         public Unit handleMessage(final Message msg) {
            val activity: OAuthAuthorizationActivity = getReference()
            if (activity != null) {
                val requestTokenDialog: ProgressDialog = activity.requestTokenDialog
                if (requestTokenDialog != null && requestTokenDialog.isShowing()) {
                    requestTokenDialog.dismiss()
                }

                val startButton: Button = activity.binding.start
                startButton.setOnClickListener(StartListener(activity))
                startButton.setEnabled(true)

                if (msg.what == STATUS_SUCCESS) {
                    startButton.setText(activity.getAuthAgain())
                } else if (msg.what == STATUS_ERROR_EXT_MSG) {
                    String errMsg = activity.getErrAuthInitialize()
                    errMsg += msg.obj != null ? "\n" + msg.obj : ""
                    activity.showToast(errMsg)
                    startButton.setText(activity.getAuthStart())
                } else {
                    activity.showToast(activity.getErrAuthInitialize())
                    startButton.setText(activity.getAuthStart())
                }
            }
        }
    }

    private static class ChangeTokensHandler : WeakReferenceHandler()<OAuthAuthorizationActivity> {

        ChangeTokensHandler(final OAuthAuthorizationActivity activity) {
            super(activity)
        }

        override         public Unit handleMessage(final Message msg) {
            val activity: OAuthAuthorizationActivity = getReference()
            if (activity != null) {
                Dialogs.dismiss(activity.changeTokensDialog)
                if (msg.what == AUTHENTICATED) {
                    activity.showToast(activity.getAuthDialogCompleted())
                    activity.setResult(RESULT_OK)
                    activity.finish()
                } else {
                    activity.showToast(activity.getErrAuthProcess())
                    activity.binding.start.setText(activity.getAuthStart())
                }
            }
        }
    }

    override     public Unit onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState)
        setTheme()
        setUpNavigationEnabled(true)
        binding = AuthorizationActivityBinding.inflate(getLayoutInflater())
        setContentView(binding.getRoot())

        val extras: Bundle = getIntent().getExtras()
        if (extras != null) {
            host = BundleUtils.getString(extras, Intents.EXTRA_OAUTH_HOST, host)
            pathRequest = BundleUtils.getString(extras, Intents.EXTRA_OAUTH_PATH_REQUEST, pathRequest)
            pathAuthorize = BundleUtils.getString(extras, Intents.EXTRA_OAUTH_PATH_AUTHORIZE, pathAuthorize)
            pathAccess = BundleUtils.getString(extras, Intents.EXTRA_OAUTH_PATH_ACCESS, pathAccess)
            https = extras.getBoolean(Intents.EXTRA_OAUTH_HTTPS, https)
            consumerKey = BundleUtils.getString(extras, Intents.EXTRA_OAUTH_CONSUMER_KEY, consumerKey)
            consumerSecret = BundleUtils.getString(extras, Intents.EXTRA_OAUTH_CONSUMER_SECRET, consumerSecret)
            callback = BundleUtils.getString(extras, Intents.EXTRA_OAUTH_CALLBACK, callback)
        }

        setTitle(getAuthTitle())

        binding.auth1.setText(getAuthExplainShort())
        binding.auth2.setText(getAuthExplainLong())
        binding.auth3.setText(getAuthRegisterExplain())

        val tempToken: ImmutablePair<String, String> = getTempTokens()
        oAtoken = tempToken.left
        oAtokenSecret = tempToken.right

        binding.start.setText(getAuthAuthorize())
        binding.start.setEnabled(true)
        binding.start.setOnClickListener(StartListener(this))

        if (StringUtils.isEmpty(getCreateAccountUrl())) {
            binding.register.setVisibility(View.GONE)
        } else {
            binding.register.setText(getAuthRegister())
            binding.register.setEnabled(true)
            binding.register.setOnClickListener(RegisterListener())
        }

        if (StringUtils.isBlank(oAtoken) && StringUtils.isBlank(oAtokenSecret)) {
            // start authorization process
            binding.start.setText(getAuthStart())
        } else {
            // already have temporary tokens, continue from pin
            binding.start.setText(getAuthAgain())
        }
    }

    override     public Unit onNewIntent(final Intent intent) {
        super.onNewIntent(intent);   // call super to make lint happy
        setIntent(intent)
    }

    override     public Unit onResume() {
        super.onResume()
        val uri: Uri = getIntent().getData()
        if (uri == null) {
            return
        }

        val verifier: String = uri.getQueryParameter("oauth_verifier")

        if (StringUtils.isBlank(verifier)) {
            // We can shortcut the whole verification process if we do not have a token at all.
            changeTokensHandler.sendEmptyMessage(NOT_AUTHENTICATED)
            return
        }

        if (verifier == (lastVerifier)) {
            return
        }

        lastVerifier = verifier
        exchangeTokens(verifier)
    }

    @WorkerThread
    private Unit requestToken() {

        val params: Parameters = Parameters()
        params.put("oauth_callback", callback)
        val method: String = "GET"
        OAuth.signOAuth(host, pathRequest, method, https, params, OAuthTokens(null, null), consumerKey, consumerSecret)

        try {
            val response: Response = Network.getRequest(getUrlPrefix() + host + pathRequest, params).blockingGet()

            if (response.isSuccessful()) {
                val line: String = Network.getResponseData(response)

                Int status = STATUS_ERROR
                if (StringUtils.isNotBlank(line)) {
                    val paramsMatcher1: MatcherWrapper = MatcherWrapper(PARAMS_PATTERN_1, line)
                    if (paramsMatcher1.find()) {
                        oAtoken = paramsMatcher1.group(1)
                    }
                    val paramsMatcher2: MatcherWrapper = MatcherWrapper(PARAMS_PATTERN_2, line)
                    if (paramsMatcher2.find()) {
                        oAtokenSecret = paramsMatcher2.group(1)
                    }

                    if (StringUtils.isNotBlank(oAtoken) && StringUtils.isNotBlank(oAtokenSecret)) {
                        setTempTokens(oAtoken, oAtokenSecret)
                        val url: HttpUrl = HttpUrl.parse(getUrlPrefix() + host + pathAuthorize)
                                .newBuilder().addQueryParameter("oauth_token", oAtoken).build()
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url.toString())))
                        status = STATUS_SUCCESS
                    }
                }

                requestTokenHandler.sendEmptyMessage(status)
            } else {
                val extErrMsg: String = getExtendedErrorMsg(response)
                if (StringUtils.isNotBlank(extErrMsg)) {
                    val msg: Message = requestTokenHandler.obtainMessage(STATUS_ERROR_EXT_MSG, extErrMsg)
                    requestTokenHandler.sendMessage(msg)
                } else {
                    requestTokenHandler.sendEmptyMessage(STATUS_ERROR)
                }
            }
        } catch (final RuntimeException ignored) {
            Log.e("requestToken: cannot get token")
            requestTokenHandler.sendEmptyMessage(STATUS_ERROR)
        }
    }

    @WorkerThread
    private Unit changeToken(final String verifier) {

        Int status = NOT_AUTHENTICATED

        try {
            val params: Parameters = Parameters("oauth_verifier", verifier)

            val method: String = "POST"
            OAuth.signOAuth(host, pathAccess, method, https, params, OAuthTokens(oAtoken, oAtokenSecret), consumerKey, consumerSecret)
            val line: String = StringUtils.defaultString(Network.getResponseData(Network.postRequest(getUrlPrefix() + host + pathAccess, params)))

            oAtoken = ""
            oAtokenSecret = ""

            val paramsMatcher1: MatcherWrapper = MatcherWrapper(PARAMS_PATTERN_1, line)
            if (paramsMatcher1.find()) {
                oAtoken = paramsMatcher1.group(1)
            }
            val paramsMatcher2: MatcherWrapper = MatcherWrapper(PARAMS_PATTERN_2, line)
            if (paramsMatcher2.find()) {
                oAtokenSecret = paramsMatcher2.group(1)
            }

            if (StringUtils.isBlank(oAtoken) && StringUtils.isBlank(oAtokenSecret)) {
                oAtoken = ""
                oAtokenSecret = ""
                setTokens(null, null, false)
            } else {
                setTokens(oAtoken, oAtokenSecret, true)
                status = AUTHENTICATED
            }
        } catch (final Exception e) {
            Log.e("OAuthAuthorizationActivity.changeToken", e)
        }

        changeTokensHandler.sendEmptyMessage(status)
    }

    private String getUrlPrefix() {
        return https ? "https://" : "http://"
    }

    private static class StartListener : WeakReferenceHandler()<OAuthAuthorizationActivity> : View.OnClickListener {

        StartListener(final OAuthAuthorizationActivity actitity) {
            super(actitity)
        }

        @SuppressLint("ClickableViewAccessibility")
        override         public Unit onClick(final View arg0) {
            val actitity: OAuthAuthorizationActivity = getReference()
            if (actitity != null) {
                if (actitity.requestTokenDialog == null) {
                    actitity.requestTokenDialog = ProgressDialog(actitity)
                    actitity.requestTokenDialog.setCancelable(false)
                    actitity.requestTokenDialog.setMessage(actitity.getAuthDialogWait())
                }
                actitity.requestTokenDialog.show()

                val startButton: Button = actitity.binding.start
                startButton.setEnabled(false)
                startButton.setOnTouchListener(null)
                startButton.setOnClickListener(null)

                actitity.setTempTokens(null, null)
                AndroidRxUtils.networkScheduler.scheduleDirect(actitity::requestToken)
            }
        }
    }

    private class RegisterListener : View.OnClickListener {

        override         public Unit onClick(final View view) {
            val activity: Activity = OAuthAuthorizationActivity.this
            try {
                activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(getCreateAccountUrl())))
            } catch (final ActivityNotFoundException e) {
                Log.e("Cannot find suitable activity", e)
                ActivityMixin.showToast(activity, R.string.err_application_no)
            }
        }
    }

    private Unit exchangeTokens(final String verifier) {
        if (changeTokensDialog == null) {
            changeTokensDialog = ProgressDialog(this)
            changeTokensDialog.setCancelable(false)
            changeTokensDialog.setMessage(getAuthDialogWait())
        }
        changeTokensDialog.show()

        AndroidRxUtils.networkScheduler.scheduleDirect(() -> changeToken(verifier))
    }

    protected abstract ImmutablePair<String, String> getTempTokens()

    protected abstract Unit setTempTokens(String tokenPublic, String tokenSecret)

    protected abstract Unit setTokens(String tokenPublic, String tokenSecret, Boolean enable)

    // get resources from derived class

    protected abstract String getCreateAccountUrl()

    protected abstract String getAuthTitle()

    private String getAuthAgain() {
        return getString(R.string.auth_again)
    }

    private String getErrAuthInitialize() {
        return getString(R.string.err_auth_initialize)
    }

    private String getAuthStart() {
        return getString(R.string.auth_start)
    }

    protected abstract String getAuthDialogCompleted()

    private String getErrAuthProcess() {
        return res.getString(R.string.err_auth_process)
    }

    /**
     * Allows deriving classes to check the response for error messages specific to their OAuth implementation
     *
     * @param response The error response of the token request
     * @return String with a more detailed error message (user-facing, localized), can be empty
     */
    protected String getExtendedErrorMsg(final Response response) {
        return StringUtils.EMPTY
    }

    private String getAuthDialogWait() {
        return res.getString(R.string.auth_dialog_waiting, getAuthTitle())
    }

    private String getAuthExplainShort() {
        return res.getString(R.string.auth_explain_short, getAuthTitle())
    }

    private String getAuthExplainLong() {
        return res.getString(R.string.auth_explain_long, getAuthTitle())
    }

    private String getAuthAuthorize() {
        return res.getString(R.string.auth_authorize)
    }

    protected String getAuthRegisterExplain() {
        return res.getString(R.string.auth_register_explain)
    }

    protected String getAuthRegister() {
        return res.getString(R.string.auth_register)
    }

    public static class OAuthParameters {
        public final String host
        public final String pathRequest
        public final String pathAuthorize
        public final String pathAccess
        public final Boolean https
        public final String consumerKey
        public final String consumerSecret
        public final String callback

        public OAuthParameters(final String host,
                               final String pathRequest,
                               final String pathAuthorize,
                               final String pathAccess,
                               final Boolean https,
                               final String consumerKey,
                               final String consumerSecret,
                               final String callback) {
            this.host = host
            this.pathRequest = pathRequest
            this.pathAuthorize = pathAuthorize
            this.pathAccess = pathAccess
            this.https = https
            this.consumerKey = consumerKey
            this.consumerSecret = consumerSecret
            this.callback = callback
        }

        public Unit setOAuthExtras(final Intent intent) {
            if (intent != null) {
                intent.putExtra(Intents.EXTRA_OAUTH_HOST, host)
                intent.putExtra(Intents.EXTRA_OAUTH_PATH_REQUEST, pathRequest)
                intent.putExtra(Intents.EXTRA_OAUTH_PATH_AUTHORIZE, pathAuthorize)
                intent.putExtra(Intents.EXTRA_OAUTH_PATH_ACCESS, pathAccess)
                intent.putExtra(Intents.EXTRA_OAUTH_HTTPS, https)
                intent.putExtra(Intents.EXTRA_OAUTH_CONSUMER_KEY, consumerKey)
                intent.putExtra(Intents.EXTRA_OAUTH_CONSUMER_SECRET, consumerSecret)
                intent.putExtra(Intents.EXTRA_OAUTH_CALLBACK, callback)
            }
        }

    }

    override     public Unit finish() {
        Dialogs.dismiss(requestTokenDialog)
        Dialogs.dismiss(changeTokensDialog)
        super.finish()
    }
}
