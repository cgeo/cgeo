package cgeo.geocaching.utils;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.settings.ShareBroadcastReceiver;

import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.core.content.FileProvider;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

public class ShareUtils {

    public static final String CHROME_PACKAGE_NAME = "com.android.chrome";
    public static final String TYPE_EMAIL = "message/rfc822";
    public static final String TYPE_TEXT = "text/plain";

    private ShareUtils() {
        // utility class
    }

    public static void share(final Context context, @NonNull final File file, @NonNull final String mimeType, @StringRes final int titleResourceId) {
        shareInternal(context, mimeType, null, null, file, titleResourceId);
    }

    public static void share(final Context context, @NonNull final File file, @StringRes final int titleResourceId) {
        shareInternal(context, "*/*", null, null, file, titleResourceId);
    }

    public static void shareAsEmail(final Context context, final String subject, final String body, @Nullable final Uri uri, @StringRes final int titleResourceId) {
        shareAsEmail(context, subject, body, uri, titleResourceId, null);
    }

    public static void shareAsEmail(final Context context, final String subject, final String body, @Nullable final Uri uri, @StringRes final int titleResourceId, final String receiver) {
        final String usedReceiver = receiver == null ? context.getString(R.string.support_mail) : receiver;
        final Intent intent = createShareIntentInternal(TYPE_EMAIL, subject, body, uri, usedReceiver);
        shareInternal(context, intent, titleResourceId);
    }

    private static void shareInternal(final Context context, @NonNull final String mimeType, @Nullable final String subject, @Nullable final String body, @Nullable final File file, @StringRes final int titleResourceId) {
        final Intent intent = createShareIntentInternal(mimeType, subject, body, fileToUri(context, file), null);
        shareInternal(context, intent, titleResourceId);
    }

    private static void shareInternal(final Context context, final Intent intent, @StringRes final int titleResourceId) {
        if (intent != null) {
            context.startActivity(Intent.createChooser(intent, context.getString(titleResourceId)));
        }
    }

    @Nullable
    private static Uri fileToUri(@NonNull final Context context, @Nullable final File file) {
        if (file != null) {
            try {
                return FileProvider.getUriForFile(context, context.getString(R.string.file_provider_authority), file);
            } catch (Exception e) {
                Log.e("error converting file to uri:" + file, e);
            }
        }
        return null;
    }

    /** context needs to be filled only when file is not null */
    private static Intent createShareIntentInternal(@NonNull final String mimeType, @Nullable final String subject, @Nullable final String body, @Nullable final Uri uri, @Nullable final String receiver) {

        final Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType(mimeType);
        if (StringUtils.isNotBlank(subject)) {
            intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        }
        if (StringUtils.isNotBlank(body)) {
            intent.putExtra(Intent.EXTRA_TEXT, body);
        }
        if (StringUtils.isNotBlank(receiver)) {
            intent.putExtra(Intent.EXTRA_EMAIL, new String[]{receiver});
        }
        if (null != uri) {
            // Grant temporary read permission to the content URI.
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }
        return intent;
    }

    public static void sharePlainText(final Context context, final String text) {
        shareInternal(context, TYPE_TEXT, null, text, null, R.string.context_share_as_text);
    }

    public static void shareLink(final Context context, final String subject, final String url) {
        shareInternal(context, getShareLinkIntent(subject, url), R.string.context_share_as_link);
    }

    public static Intent getShareLinkIntent(final String subject, final String url) {
        return createShareIntentInternal(TYPE_TEXT, subject, StringUtils.defaultString(url), null, null);
    }

    public static void shareMultipleFiles(final Context context, @NonNull final List<File> files, @StringRes final int titleResourceId) {
        final ArrayList<Uri> uris = new ArrayList<>();

        try {
            for (File file : files) {
                uris.add(FileProvider.getUriForFile(context, context.getString(R.string.file_provider_authority), file));
            }

            final Intent shareIntent = new Intent();
            shareIntent.setAction(Intent.ACTION_SEND_MULTIPLE);
            shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
            shareIntent.setType("*/*");
            shareInternal(context, shareIntent, titleResourceId);

        } catch (Exception e) {
            Log.e("error on sharing", e);
        }
    }

    public static void openUrl(final Context context, final String url) {
        openUrl(context, url, false);

    }

    /* Opens a URL in browser, in the registered default application or if activated by the user in the settings with customTabs */
    public static void openUrl(final Context context, final String url, final boolean forceIntentChooser) {
        if (StringUtils.isBlank(url)) {
            return;
        }

        try {
            if (Settings.getUseCustomTabs() && isChromeLaunchable()) {
                openCustomTab(context, url);
                return;
            }

            final Intent viewIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));

            // Always shows an application chooser with all possible targets
            if (forceIntentChooser) {
                final Intent chooser = Intent.createChooser(viewIntent, context.getString(R.string.cache_menu_browser));
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
                context.startActivity(chooser);
            } else {
                context.startActivity(viewIntent);
            }

        } catch (final ActivityNotFoundException e) {
            Log.e("Cannot find suitable activity for URL '" + url + "'", e);
            ActivityMixin.showToast(context, R.string.err_application_no);
        }
    }

    private static void openCustomTab(final Context context, final String url) {
        final CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder()
            .setUrlBarHidingEnabled(true)
            .setShowTitle(true)
            .setShareState(CustomTabsIntent.SHARE_STATE_ON);

        final Intent actionIntent = new Intent(context, ShareBroadcastReceiver.class);
        final PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, actionIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.addMenuItem(context.getString(R.string.cache_menu_open_with), pendingIntent);

        final CustomTabsIntent customTabsIntent = builder.build();
        // custom tabs API was restricted to chrome as other browsers like firefox may loop back to c:geo (as of September 2020)
        customTabsIntent.intent.setPackage(CHROME_PACKAGE_NAME);
        customTabsIntent.launchUrl(context, Uri.parse(url));
    }

    public static boolean isChromeLaunchable() {
        return ProcessUtils.isLaunchable(CHROME_PACKAGE_NAME);
    }
}
