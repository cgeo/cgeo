package cgeo.geocaching.utils;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.ActivityMixin;
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

    public static final String TYPE_CGEO_SUPPORT_EMAIL = "message/rfc822";
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

    public static void shareAsGCSupportEmail(final Context context, final String subject, final String body, @Nullable final File file, @StringRes final int titleResourceId) {
        shareInternal(context, TYPE_CGEO_SUPPORT_EMAIL, subject, body, file, titleResourceId);
    }

    private static void shareInternal(final Context context, @NonNull final String mimeType, @Nullable final String subject, @Nullable final String body, @Nullable final File file, @StringRes final int titleResourceId) {
        final Intent intent = createShareIntentInternal(context, mimeType, subject, body, file);
        shareInternal(context, intent, titleResourceId);
    }

    private static void shareInternal(final Context context, final Intent intent, @StringRes final int titleResourceId) {
        if (intent != null) {
            context.startActivity(Intent.createChooser(intent, context.getString(titleResourceId)));
        }
    }

    /** context needs to be filled only when file is not null */
    private static Intent createShareIntentInternal(final Context context, @NonNull final String mimeType, @Nullable final String subject, @Nullable final String body, @Nullable final File file) {
        Uri uri = null;
        if (null != file) {
            try {
                uri = FileProvider.getUriForFile(context, context.getString(R.string.file_provider_authority), file);
            } catch (Exception e) {
                Log.e("error on LogCat sharing");
            }
        }
        if (null == file || null != uri) {
            final Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType(mimeType);
            if (StringUtils.isNotBlank(subject)) {
                intent.putExtra(Intent.EXTRA_SUBJECT, subject);
            }
            if (StringUtils.isNotBlank(body)) {
                intent.putExtra(Intent.EXTRA_TEXT, body);
            }
            if (mimeType.equals(TYPE_CGEO_SUPPORT_EMAIL)) {
                intent.putExtra(Intent.EXTRA_EMAIL, new String[]{context.getString(R.string.support_mail)});
            }
            if (null != file) {
                // Grant temporary read permission to the content URI.
                intent.putExtra(Intent.EXTRA_STREAM, uri);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }
            return intent;
        }

        return null;
    }

    public static void sharePlainText(final Context context, final String text) {
        shareInternal(context, TYPE_TEXT, null, text, null, R.string.context_share_as_text);
    }

    public static void shareLink(final Context context, final String subject, final String url) {
        shareInternal(context, getShareLinkIntent(subject, url), R.string.context_share_as_link);
    }

    public static Intent getShareLinkIntent(final String subject, final String url) {
        return createShareIntentInternal(null, TYPE_TEXT, subject, StringUtils.defaultString(url), null);
    }

    public static void shareMultipleFiles(final Context context, @NonNull final List<File> files, @StringRes final int titleResourceId) {
        final ArrayList<Uri> uris = new ArrayList<Uri>();

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

    public static void openUrl(final Context context, final String url, final boolean forceIntentChooser) {
        if (StringUtils.isBlank(url)) {
            return;
        }

        try {
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

    public static void openCustomTab(final Context context, final String url) {
        if (ProcessUtils.isLaunchable("com.android.chrome")) {

            final CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();

            builder.enableUrlBarHiding();
            builder.setShowTitle(true);
            builder.addDefaultShareMenuItem();

            final Intent actionIntent = new Intent(context, ShareBroadcastReceiver.class);
            final PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, actionIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            builder.addMenuItem(context.getString(R.string.cache_menu_open_with), pendingIntent);

            final CustomTabsIntent customTabsIntent = builder.build();
            // custom tabs API was restricted to chrome as other browsers like firefox may loop back to c:geo (as of September 2020)
            customTabsIntent.intent.setPackage("com.android.chrome");
            customTabsIntent.launchUrl(context, Uri.parse(url));

        } else {
            openUrl(context, url, true);
        }
    }
}
