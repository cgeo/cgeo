package cgeo.geocaching.connector.gc;

import cgeo.geocaching.R;
import cgeo.geocaching.network.HttpRequest;
import cgeo.geocaching.network.HttpResponse;
import cgeo.geocaching.network.Parameters;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.utils.AndroidRxUtils;
import cgeo.geocaching.utils.LocalizationUtils;
import cgeo.geocaching.utils.Log;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Base64;
import android.widget.Toast;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.StringUtils;

/**
 * OAuth2 + PKCE authorization helper for the GC Live API.
 *
 * Usage:
 * 1. Call {@link #startAuthorization(Context)} — stores PKCE verifier, opens browser
 * 2. Browser redirects to gcdroid://oauth2.callback/callback?code=...
 * 3. MainActivity receives the callback, calls {@link #handleCallback(Context, Uri)}
 * 4. Tokens are exchanged and stored in Settings
 */
public final class GCLiveOAuth {

    private static final String AUTH_URL = "https://www.geocaching.com/oauth/authorize.aspx";
    private static final String TOKEN_URL = "https://oauth.geocaching.com/token";

    private GCLiveOAuth() {
        // utility class
    }

    /**
     * Start the OAuth2 authorization flow: generate PKCE verifier, persist it,
     * and open the browser to the GC authorize page.
     */
    public static void startAuthorization(final Context context) {
        final String clientId = LocalizationUtils.getString(R.string.gc_live_client_id);
        final String redirectUri = LocalizationUtils.getString(R.string.gc_live_redirect_uri);

        final String codeVerifier = generateCodeVerifier();
        Settings.setGCLiveCodeVerifier(codeVerifier);

        final String codeChallenge = generateCodeChallenge(codeVerifier);

        final Uri uri = Uri.parse(AUTH_URL).buildUpon()
                .appendQueryParameter("client_id", clientId)
                .appendQueryParameter("response_type", "code")
                .appendQueryParameter("scope", "*")
                .appendQueryParameter("redirect_uri", redirectUri)
                .appendQueryParameter("code_challenge", codeChallenge)
                .appendQueryParameter("code_challenge_method", "S256")
                .build();

        Log.i("GCLiveOAuth: Opening browser for authorization");
        final Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        context.startActivity(intent);
    }

    /**
     * Check whether the given URI is a GC Live OAuth callback and handle it.
     *
     * @return true if the URI was an OAuth callback (handled or error), false otherwise
     */
    public static boolean handleCallback(final Context context, final Uri uri) {
        if (uri == null) {
            return false;
        }

        final String redirectUri = LocalizationUtils.getString(R.string.gc_live_redirect_uri);
        if (StringUtils.isBlank(redirectUri)) {
            return false;
        }

        final Uri redirect = Uri.parse(redirectUri);
        if (!StringUtils.equals(uri.getScheme(), redirect.getScheme())
                || !StringUtils.equals(uri.getAuthority(), redirect.getAuthority())
                || !StringUtils.equals(uri.getPath(), redirect.getPath())) {
            return false;
        }

        Log.i("GCLiveOAuth: Received OAuth callback");

        final String code = uri.getQueryParameter("code");
        if (StringUtils.isBlank(code)) {
            final String error = uri.getQueryParameter("error");
            Log.e("GCLiveOAuth: No code in callback: " + (error != null ? error : "unknown"));
            final String msg = error != null
                    ? context.getString(R.string.gc_live_auth_failed, error)
                    : context.getString(R.string.gc_live_auth_failed_no_code);
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
            return true;
        }

        final String codeVerifier = Settings.getGCLiveCodeVerifier();
        // Clear it immediately so it can't be reused
        Settings.setGCLiveCodeVerifier(null);

        if (StringUtils.isBlank(codeVerifier)) {
            Log.e("GCLiveOAuth: No code verifier found in Settings");
            Toast.makeText(context, R.string.gc_live_auth_failed_no_verifier, Toast.LENGTH_LONG).show();
            return true;
        }

        Log.i("GCLiveOAuth: Got authorization code, exchanging for tokens...");
        AndroidRxUtils.networkScheduler.scheduleDirect(() -> exchangeCodeForTokens(context, code, codeVerifier));
        return true;
    }

    private static void exchangeCodeForTokens(final Context context, final String code, final String codeVerifier) {
        try {
            final String clientId = LocalizationUtils.getString(R.string.gc_live_client_id);
            final String clientSecret = LocalizationUtils.getString(R.string.gc_live_client_secret);
            final String redirectUri = LocalizationUtils.getString(R.string.gc_live_redirect_uri);

            final Parameters body = new Parameters();
            body.put("client_id", clientId);
            body.put("client_secret", clientSecret);
            body.put("grant_type", "authorization_code");
            body.put("redirect_uri", redirectUri);
            body.put("code", code);
            body.put("code_verifier", codeVerifier);

            final HttpResponse response = new HttpRequest()
                    .uriBase(TOKEN_URL)
                    .uri("")
                    .bodyForm(body)
                    .request()
                    .blockingGet();

            final int status = response.getStatusCode();
            if (status != 200) {
                Log.e("GCLiveOAuth: Token exchange HTTP " + status);
                showToast(context, context.getString(R.string.gc_live_auth_failed_http, status));
                return;
            }

            final TokenResponse token = response.parseJson(TokenResponse.class, null);
            if (token == null || StringUtils.isBlank(token.accessToken)) {
                Log.e("GCLiveOAuth: Empty token response");
                showToast(context, context.getString(R.string.gc_live_auth_failed_empty_token));
                return;
            }

            final long now = System.currentTimeMillis() / 1000;
            Settings.setGCLiveAccessToken(token.accessToken);
            Settings.setGCLiveTokenIssuedAt(now);
            if (StringUtils.isNotBlank(token.refreshToken)) {
                Settings.setGCLiveRefreshToken(token.refreshToken);
            }

            Log.i("GCLiveOAuth: Tokens stored successfully");
            showToast(context, context.getString(R.string.gc_live_auth_success));

        } catch (final Exception e) {
            Log.e("GCLiveOAuth: Token exchange failed", e);
            showToast(context, context.getString(R.string.gc_live_auth_failed, e.getMessage()));
        }
    }

    private static void showToast(final Context context, final String msg) {
        AndroidRxUtils.runOnUi(() -> Toast.makeText(context.getApplicationContext(), msg, Toast.LENGTH_LONG).show());
    }

    private static String generateCodeVerifier() {
        final byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.encodeToString(bytes, Base64.URL_SAFE | Base64.NO_PADDING | Base64.NO_WRAP);
    }

    private static String generateCodeChallenge(final String verifier) {
        try {
            final MessageDigest digest = MessageDigest.getInstance("SHA-256");
            final byte[] hash = digest.digest(verifier.getBytes(StandardCharsets.US_ASCII));
            return Base64.encodeToString(hash, Base64.URL_SAFE | Base64.NO_PADDING | Base64.NO_WRAP);
        } catch (final Exception e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static final class TokenResponse {
        @JsonProperty("access_token")
        String accessToken;
        @JsonProperty("refresh_token")
        String refreshToken;
        @JsonProperty("token_type")
        String tokenType;
        @JsonProperty("expires_in")
        long expiresIn;
    }
}
