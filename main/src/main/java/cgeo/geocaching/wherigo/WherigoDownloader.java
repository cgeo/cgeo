package cgeo.geocaching.wherigo;

import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.connector.StatusResult;
import cgeo.geocaching.connector.gc.GCConnector;
import cgeo.geocaching.enumerations.StatusCode;
import cgeo.geocaching.network.HttpRequest;
import cgeo.geocaching.network.HttpResponse;
import cgeo.geocaching.network.Parameters;
import cgeo.geocaching.settings.Credentials;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.storage.ContentStorage;
import cgeo.geocaching.utils.AndroidRxUtils;
import cgeo.geocaching.utils.Formatter;
import cgeo.geocaching.utils.Log;
import cgeo.geocaching.utils.workertask.ProgressDialogFeature;
import cgeo.geocaching.utils.workertask.WorkerTask;

import android.annotation.SuppressLint;
import android.net.Uri;
import android.util.Pair;

import androidx.core.app.ComponentActivity;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.Response;
import org.oscim.utils.IOUtils;

public class WherigoDownloader {

    private static final String LOG_PRAEFIX = "WherigoDownloader:";

    private static final String LOGIN = "https://www.wherigo.com/login/default.aspx";
    private static final String DOWNLOAD = "https://www.wherigo.com/cartridge/download.aspx";

    private static final Pattern REQUEST_VERIFICATIOn_TOKEN_PATTERN = Pattern.compile("<input name=\"__RequestVerificationToken\" type=\"hidden\" value=\"([^\"]+)\"");

    private final WorkerTask<Pair<String, Function<String, Uri>>, String, StatusResult> wherigoDownloadTask;


    public WherigoDownloader(@SuppressLint("RestrictedApi") final ComponentActivity activity, final Consumer<StatusResult> wherigoDownloadConsumer) {

        wherigoDownloadTask = WorkerTask.<Pair<String, Function<String, Uri>>, String, StatusResult>of(
                "wherigo-download",
                (input, progress, cancelFlag) -> downloadWherigoTask(input.first, input.second, progress, cancelFlag),
                AndroidRxUtils.networkScheduler)
            .addFeature(ProgressDialogFeature.of(activity).setTitle("Downloading Wherigo").setAllowCancel(true))
            .observeResult(activity, result -> {
                if (wherigoDownloadConsumer != null) {
                    wherigoDownloadConsumer.accept(result);
                }
            }, null);
    }

    public void downloadWherigo(final String cguid, final Function<String, Uri> targetUriSupplier) {
        wherigoDownloadTask.start(new Pair<>(cguid, targetUriSupplier));
    }

    private static StatusResult downloadWherigoTask(final String cguid, final Function<String, Uri> targetUriSupplier, final Consumer<String> progress, final Supplier<Boolean> cancelFlag) {
        final Credentials cred = Settings.getCredentials(GCConnector.getInstance());
        final String username = cred.getUsernameRaw();
        final String password = cred.getPasswordRaw();

        final Uri[] uriStorage = new Uri[1];
        final Function<String, OutputStream> outputSupplier = name -> {
            uriStorage[0] = targetUriSupplier.apply(name);
            return ContentStorage.get().openForWrite(uriStorage[0]);
        };
        final StatusResult result = performDownload(username, password, cguid, outputSupplier, progress, cancelFlag);
        if (!result.isOk() && uriStorage[0] != null) {
            ActivityMixin.showApplicationToast("Wherigo: Deleting leftover file");
            ContentStorage.get().delete(uriStorage[0]);
        }
        return result;
    }

    public static StatusResult performDownload(final String username, final String password, final String cguid, final Function<String, OutputStream> outputSupplier, final Consumer<String> progress, final Supplier<Boolean> cancelFlag) {

        progress.accept("Logging in");

        final StatusResult loginResult = login(username, password);
        if (!loginResult.isOk()) {
            return loginResult;
        }

        progress.accept("Start Download");
        return download(cguid, outputSupplier, (c, t) -> {
            final int percent = Math.round(((float) c) / t * 100);

            progress.accept("Downloaded " + Formatter.formatBytes(c) + "/" + Formatter.formatBytes(t) + " (" + percent + "%)");
        }, cancelFlag);
    }

    private static StatusResult login(final String username, final String password) {

        final Parameters params = new Parameters()
            .put("__EVENTTARGET", "")
            .put("__EVENTARGUMENT", "")
            .add("ctl00$ContentPlaceHolder1$Login1$Login1$UserName", username)
            .add("ctl00$ContentPlaceHolder1$Login1$Login1$Password", password)
            .add("ctl00$ContentPlaceHolder1$Login1$Login1$LoginButton", "Sign In");

        final HttpRequest loginRequest = new HttpRequest()
            .uri(LOGIN)
            .method(HttpRequest.Method.POST)
            .bodyForm(params);
        try (HttpResponse loginResponse = loginRequest.request().blockingGet()) {
            return loginResponse.isSuccessful() ? StatusResult.OK : new StatusResult(StatusCode.NOT_LOGGED_IN, loginResponse.getBodyString());
        }
    }

    private static StatusResult download(final String cguid, final Function<String, OutputStream> outputSupplier, final BiConsumer<Long, Long> progress, final Supplier<Boolean> cancelFlag) {

        //load Download page with GET to retrieve RequestVerificationToken
        final String page = new HttpRequest().uri(DOWNLOAD + "?CGUID=" + cguid).request().blockingGet().getBodyString();
        final Matcher m = REQUEST_VERIFICATIOn_TOKEN_PATTERN.matcher(page);
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
            .add("ctl00$ContentPlaceHolder1$btnDownload", "Download Now");

        final HttpRequest downloadRequest = new HttpRequest()
            .uri(DOWNLOAD + "?CGUID=" + cguid)
            .method(HttpRequest.Method.POST)
            .bodyForm(params);
        try (HttpResponse downloadResponse = downloadRequest.request().blockingGet()) {
            final String type = downloadResponse.getResponse().body().contentType().toString();
            if (!downloadResponse.isSuccessful() || !"application/octet-stream".equals(type)) {
                return new StatusResult(StatusCode.COMMUNICATION_ERROR, "cartridge not found [" + type + "]");
            }
            final Response response = downloadResponse.getResponse();
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

    private static StatusResult store(final String filename, final long total, final Function<String, OutputStream> outputSupplier, final InputStream stream, final BiConsumer<Long, Long> progress, final Supplier<Boolean> cancelFlag) {

            long completed = 0;
            boolean success = false;
            String errorMsg = "";
            final byte[] buffer = new byte[1024];
            final OutputStream outputStream = new BufferedOutputStream(outputSupplier.apply(filename));
            if (outputStream == null) {
                return new StatusResult(StatusCode.COMMUNICATION_ERROR, "Creating outputstream for: " + filename);
            }

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
