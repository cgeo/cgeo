package cgeo.geocaching.wherigo;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.connector.StatusResult;
import cgeo.geocaching.databinding.GcManualLoginBinding;
import cgeo.geocaching.enumerations.StatusCode;
import cgeo.geocaching.network.Cookies;
import cgeo.geocaching.network.HttpRequest;
import cgeo.geocaching.network.HttpResponse;
import cgeo.geocaching.network.Network;
import cgeo.geocaching.network.Parameters;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.ContentStorage;
import cgeo.geocaching.ui.TextParam;
import cgeo.geocaching.ui.dialog.SimpleDialog;
import cgeo.geocaching.utils.AndroidRxUtils;
import cgeo.geocaching.utils.Formatter;
import cgeo.geocaching.utils.LocalizationUtils;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.workertask.ProgressDialogFeature;
import cgeo.geocaching.utils.workertask.WorkerTask;
import static cgeo.geocaching.connector.gc.GCLogin.initializeWebview;

import android.app.AlertDialog;
import android.net.Uri;
import android.util.Pair;
import android.view.LayoutInflater;
import android.webkit.CookieManager;

import androidx.activity.ComponentActivity;
import androidx.annotation.UiThread;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.Cookie;
import okhttp3.HttpUrl;
import okhttp3.Response;
import org.apache.commons.lang3.StringUtils;
import org.oscim.utils.IOUtils;

public class WherigoDownloader {

    private static final String LOG_PRAEFIX = "WherigoDownloader:";

    private static final String WHERIGO_URL = "https://www.wherigo.com";
    private static final String LOGIN_PAGE = WHERIGO_URL + "/login/";
    private static final String DOWNLOAD = WHERIGO_URL + "/cartridge/download.aspx";
    private static final String HOME_PAGE = WHERIGO_URL + "/home.aspx";
    private static final String AUTH_COOKIE_NAME = ".ASPXAUTH";

    private static final Pattern REQUEST_VERIFICATION_TOKEN_PATTERN = Pattern.compile("<input name=\"__RequestVerificationToken\" type=\"hidden\" value=\"([^\"]+)\"");

    private final WorkerTask<Pair<String, Function<String, Uri>>, String, Pair<String, StatusResult>> wherigoDownloadTask;
    private final ComponentActivity activity;


    public WherigoDownloader(final ComponentActivity activity, final BiConsumer<String, StatusResult> wherigoDownloadConsumer) {
        this.activity = activity;

        wherigoDownloadTask = WorkerTask.<Pair<String, Function<String, Uri>>, String, Pair<String, StatusResult>>of(
                "wherigo-download",
                (input, progress, cancelFlag) -> downloadWherigoTask(input.first, input.second, progress, cancelFlag),
                AndroidRxUtils.networkScheduler)
            .addFeature(ProgressDialogFeature.of(activity).setTitle(LocalizationUtils.getString(R.string.wherigo_download_title)).setAllowCancel(true))
            .observeResult(activity, result -> {
                if (wherigoDownloadConsumer != null) {
                    wherigoDownloadConsumer.accept(result.first, result.second);
                }
            }, error -> {
                if (wherigoDownloadConsumer != null) {
                    wherigoDownloadConsumer.accept("-", new StatusResult(StatusCode.COMMUNICATION_ERROR, "Error: " + error));
                }
            });
    }

    public void downloadWherigo(final String cguid, final Function<String, Uri> targetUriSupplier) {
        wherigoDownloadTask.start(new Pair<>(cguid, targetUriSupplier));
    }

    private Pair<String, StatusResult> downloadWherigoTask(final String cguid, final Function<String, Uri> targetUriSupplier, final Consumer<String> progress, final Supplier<Boolean> cancelFlag) {
        try {
            if (!isLoggedIn(progress)) {
                return new Pair<>(cguid, new StatusResult(StatusCode.NOT_LOGGED_IN, null));
            }

            final Uri[] uriStorage = new Uri[1];
            final Function<String, OutputStream> outputSupplier = name -> {
                uriStorage[0] = targetUriSupplier.apply(name);
                return ContentStorage.get().openForWrite(uriStorage[0]);
            };
            progress.accept(LocalizationUtils.getString(R.string.wherigo_download_progress_started));
            final StatusResult result = download(cguid, outputSupplier, (c, t) -> {
                    final int percent = Math.round(((float) c) / t * 100);

                    progress.accept(LocalizationUtils.getString(R.string.wherigo_download_progress_download_status,
                            Formatter.formatBytes(c), Formatter.formatBytes(t), String.valueOf(percent)));
                }, cancelFlag);
            if (!result.isOk() && uriStorage[0] != null) {
                ActivityMixin.showApplicationToast(LocalizationUtils.getString(R.string.wherigo_download_delete_leftover_toast));
                ContentStorage.get().delete(uriStorage[0]);
            }
            return new Pair<>(cguid, result);
        } catch (RuntimeException re) {
            return new Pair<>(cguid, new StatusResult(StatusCode.COMMUNICATION_ERROR, "Unexpected problem: " + re));
        }
    }

    /**
     * Ensure the user is authenticated against wherigo.com, else start WebView based login.
     */
    private boolean isLoggedIn(final Consumer<String> progress) {
        if (isLoggedIn()) {
            return true;
        }
        progress.accept(LocalizationUtils.getString(R.string.wherigo_download_progress_login));

        final AtomicBoolean confirmed = new AtomicBoolean(false);
        final CountDownLatch dialogClosed = new CountDownLatch(1);
        AndroidRxUtils.runOnUi(() -> performManualLogin(ok -> {
            confirmed.set(ok);
            dialogClosed.countDown();
        }));
        try {
            dialogClosed.await();
        } catch (final InterruptedException ex) {
            Thread.currentThread().interrupt();
            Log.w(LOG_PRAEFIX + " interrupted while waiting for manual login");
            return false;
        }
        // Respect explicit cancel/back
        if (!confirmed.get()) {
            return false;
        }
        return isLoggedIn();
    }

    @UiThread
    private void performManualLogin(final Consumer<Boolean> onResult) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this.activity, R.style.cgeo_fullScreenDialog);
        final GcManualLoginBinding binding = GcManualLoginBinding.inflate(LayoutInflater.from(this.activity));
        final AlertDialog dialog = builder.create();
        dialog.setView(binding.getRoot());
        initializeWebview(binding.webview);

        WindowCompat.enableEdgeToEdge(this.activity.getWindow());
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, windowInsets) -> {
            final Insets innerPadding = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout() | WindowInsetsCompat.Type.ime());
            v.setPadding(innerPadding.left, innerPadding.top, innerPadding.right, innerPadding.bottom);
            return windowInsets;
        });

        // Confirmed = OK with a usable cookie. Anything else (cancel, back, outside-tap, activity destroy) is treated as cancelled.
        final AtomicBoolean confirmed = new AtomicBoolean(false);
        dialog.setOnDismissListener(d -> onResult.accept(confirmed.get()));

        CookieManager.getInstance().removeAllCookies(b -> {
            binding.webview.loadUrl(LOGIN_PAGE);
            binding.okButton.setOnClickListener(bo -> {
                // Extract the wherigo auth cookie from the WebView.
                final String webViewCookies = CookieManager.getInstance().getCookie(WHERIGO_URL);
                final List<Cookie> authCookies = Cookies.extractCookies(WHERIGO_URL, webViewCookies, c -> c.name().equals(AUTH_COOKIE_NAME));
                if (authCookies.isEmpty()) {
                    // No cookie yet: keep the dialog open so the user can finish logging in and retry.
                    SimpleDialog.ofContext(this.activity).setTitle(TextParam.id(R.string.init_login_manual)).setMessage(TextParam.id(R.string.init_login_manual_error_nocookie)).show();
                    return;
                }

                // Persist the cookie
                Cookies.cookieJar.saveFromResponse(HttpUrl.get(WHERIGO_URL), authCookies);
                Settings.putString(R.string.pref_wherigocomcookie, authCookies.get(0).value());
                confirmed.set(true);
                dialog.dismiss();
            });
            binding.cancelButton.setOnClickListener(bo -> dialog.dismiss());
            dialog.show();
        });
    }

    private static void injectStoredCookie() {
        final String value = Settings.getString(R.string.pref_wherigocomcookie, "");
        Cookies.cookieJar.saveFromResponse(HttpUrl.get(WHERIGO_URL),
            List.of(new Cookie.Builder().domain("www.wherigo.com").name(AUTH_COOKIE_NAME).value(value).build()));
    }

    private static boolean isLoggedIn() {
        injectStoredCookie();
        final Response response = Network.getRequest(HOME_PAGE).blockingGet();
        final String data = Network.getResponseData(response);
        return data != null && data.contains("Signed In:");
    }

    private static StatusResult download(final String cguid, final Function<String, OutputStream> outputSupplier, final BiConsumer<Long, Long> progress, final Supplier<Boolean> cancelFlag) {

        // load Download page with GET to retrieve RequestVerificationToken
        final String page = new HttpRequest().uri(DOWNLOAD + "?CGUID=" + cguid).request().blockingGet().getBodyString();
        final Matcher m = REQUEST_VERIFICATION_TOKEN_PATTERN.matcher(page);
        if (!m.find()) {
            return new StatusResult(StatusCode.COMMUNICATION_ERROR, "Couldn't find RequestVerificationToken on page");
        }
        final String token = m.group(1);

        //place download request
        final Parameters params = new Parameters()
            .add("__EVENTTARGET", "")
            .add("__EVENTARGUMENT", "")
            .add("__RequestVerificationToken", token)
            .add("ctl00$ContentPlaceHolder1$uxDeviceList", "4")
            .add("ctl00$ContentPlaceHolder1$EULAControl1$uxEulaAgree", "On")
            .add("ctl00$ContentPlaceHolder1$btnDownload", "Download Now");

        final HttpRequest downloadRequest = new HttpRequest()
            .uri(DOWNLOAD + "?CGUID=" + cguid)
            .method(HttpRequest.Method.POST)
            .bodyForm(params);
        try (HttpResponse downloadResponse = downloadRequest.request().blockingGet()) {
            final String type = downloadResponse.getResponse().body().contentType().toString();
            if (!downloadResponse.isSuccessful()) {
                return cartridgeNotFound(type);
            }
            final Response response = downloadResponse.getResponse();
            if (StringUtils.startsWith(type, "text/html")) {
                final String body = downloadResponse.getBodyString();
                if (StringUtils.contains(body, "<textarea name=\"ctl00$ContentPlaceHolder1$EULAControl1$uxEulaText\"")) {
                    return new StatusResult(StatusCode.UNAPPROVED_LICENSE, LocalizationUtils.getString(R.string.wherigo_download_accept_eula));
                } else {
                    return cartridgeNotFound(type);
                }
            } else if (!"application/octet-stream".equals(type)) {
                return cartridgeNotFound(type);
            }

            final String contentDisposition = response.header("Content-Disposition", "");
            final String pattern = "(?i)^ *attachment *; *filename *= *(.*) *$";
            final String filename;
            if (contentDisposition.matches(pattern)) {
                filename = cguid + "_" + contentDisposition.replaceFirst(pattern, "$1");
            } else {
                filename = cguid + ".gwc";
            }
            final long total = Long.parseLong(response.header("Content-Length", "0"));

            return store(filename, total, outputSupplier, response.body().byteStream(), progress, cancelFlag);
        }
    }

    private static StatusResult cartridgeNotFound(final String type) {
        return new StatusResult(StatusCode.COMMUNICATION_ERROR, "cartridge not found [" + type + "]");
    }

    private static StatusResult store(final String filename, final long total, final Function<String, OutputStream> outputSupplier, final InputStream stream, final BiConsumer<Long, Long> progress, final Supplier<Boolean> cancelFlag) {

            long completed = 0;
            boolean success = false;
            String errorMsg = "";
            final byte[] buffer = new byte[1024];
            final OutputStream rawStream = outputSupplier.apply(filename);
            if (rawStream == null) {
                return new StatusResult(StatusCode.COMMUNICATION_ERROR, "Creating outputstream for: " + filename);
            }
            final OutputStream outputStream = new BufferedOutputStream(rawStream);

            try (BufferedInputStream inputStream = new BufferedInputStream(stream)) {
                progress.accept(completed, total);
                int length;
                while ((length = inputStream.read(buffer)) > 0 && !cancelFlag.get()) {
                    outputStream.write(buffer, 0, length);
                    completed += length;
                    progress.accept(completed, total);
                }
                success = true;
            } catch (IOException e) {
                errorMsg += "download(" + filename + ") failed: " + e.getMessage();
                Log.e(LOG_PRAEFIX + "download(" + filename + ")", e);
            } finally {
                IOUtils.closeQuietly(outputStream);
            }
            errorMsg += " (c=" + completed + ", t=" + total + ")";
            success = success & (completed == total);
            return success ? StatusResult.OK : new StatusResult(StatusCode.COMMUNICATION_ERROR, errorMsg);
    }


}
