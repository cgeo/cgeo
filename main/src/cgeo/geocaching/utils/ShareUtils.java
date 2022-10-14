package cgeo.geocaching.utils;

import cgeo.geocaching.CacheDetailActivity;
import cgeo.geocaching.R;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.settings.ShareBroadcastReceiver;
import cgeo.geocaching.settings.StartWebviewActivity;
import cgeo.geocaching.ui.TextParam;
import cgeo.geocaching.ui.dialog.SimpleDialog;
import static cgeo.geocaching.utils.ProcessUtils.CHROME_PACKAGE_NAME;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.core.content.FileProvider;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.lang3.StringUtils;


public class ShareUtils {

    public static final String TYPE_EMAIL = "message/rfc822";
    public static final String TYPE_TEXT = "text/plain";
    public static final String TYPE_XML = "application/xml";
    public static final String TYPE_IMAGE = "image/*";

    private ShareUtils() {
        // utility class
    }

    /**
     * Standard message box + additional share button for file sharing
     */
    public static void shareOrDismissDialog(final Activity context, @NonNull final Uri uri, @NonNull final String mimeType, @StringRes final int title, final String msg) {
        SimpleDialog.of(context).setTitle(title).setMessage(TextParam.text(msg))
                .setButtons(0, 0, R.string.cache_share_field)
                .show(SimpleDialog.DO_NOTHING, null,
                        (dialog, which) -> {
                            final Intent intent = createShareIntentInternal(context, mimeType, null, msg, uri, null);
                            shareInternal(context, intent, title);
                        });
    }

    public static void shareAsEmail(final Context context, final String subject, final String body, @Nullable final Uri uri, @StringRes final int titleResourceId) {
        shareAsEmail(context, subject, body, uri, titleResourceId, null);
    }

    public static void shareImage(final Context context, final Uri imageUri, final String geocode, @StringRes final int titleResourceId) {
        final Uri localFileImageUri = ImageUtils.getLocalImageFileUriForSharing(context, imageUri, geocode);
        final Intent intent = createShareIntentInternal(context, TYPE_IMAGE, null, null, localFileImageUri, null);
        shareInternal(context, intent, titleResourceId);
    }

    private static void shareAsEmail(final Context context, final String subject, final String body, @Nullable final Uri uri, @StringRes final int titleResourceId, final String receiver) {
        final String usedReceiver = receiver == null ? context.getString(R.string.support_mail) : receiver;
        final Intent intent = createShareIntentInternal(context, TYPE_EMAIL, subject, body, uri, usedReceiver);
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

    /**
     * context needs to be filled only when file is not null
     */
    private static Intent createShareIntentInternal(@NonNull final Context context, @NonNull final String mimeType, @Nullable final String subject, @Nullable final String body, @Nullable final Uri uri, @Nullable final String receiver) {

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
            Uri uriToUse = uri;
            final File uriFile = UriUtils.toFile(uri);
            if (uriFile != null) {
                uriToUse = fileToUri(context, uriFile);
            }

            // Grant temporary read permission to the content URI.
            intent.putExtra(Intent.EXTRA_STREAM, uriToUse);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }
        return intent;
    }

    public static void sharePlainText(final Context context, final String text) {
        final Intent intent = createShareIntentInternal(context, TYPE_TEXT, null, text, null, null);
        shareInternal(context, intent, R.string.context_share_as_text);
    }

    public static void shareLink(final Context context, final String subject, final String url) {
        shareInternal(context, getShareLinkIntent(context, subject, url), R.string.context_share_as_link);
    }

    private static Intent getShareLinkIntent(final Context context, final String subject, final String url) {
        return createShareIntentInternal(context, TYPE_TEXT, subject, StringUtils.defaultString(url), null, null);
    }

    public static void shareMultipleFiles(final Context context, @NonNull final ArrayList<Uri> uris, @StringRes final int titleResourceId) {
        try {
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

    /**
     * opens system's standard viewer for content.
     * This method is explicitly designed for uris pointing to content/files, not for http/browser-Uris
     **/
    public static void openContentForView(final Context context, final String url) {

        final Uri uri = Uri.parse(url);

        final Intent viewIntent = new Intent(Intent.ACTION_VIEW);

        try {
            //mimeType must be set explicitly, otherwise some apps have problems e.g. Google Sheets with xlsx or csv files
            viewIntent.setDataAndType(uri, UriUtils.getMimeType(uri));

            viewIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(viewIntent);
        } catch (final ActivityNotFoundException e) {
            Log.e("Cannot find suitable activity for URL '" + url + "'", e);
            ActivityMixin.showToast(context, R.string.err_application_no);
        }
    }

    /**
     * Opens a URL in browser, in the registered default application or if activated by the user in the settings with customTabs
     */
    public static void openUrl(final Context context, final String url, final boolean forceIntentChooser) {
        if (StringUtils.isBlank(url)) {
            return;
        }

        try {
            if (Settings.getUseCustomTabs() && ProcessUtils.isChromeLaunchable()) {
                openCustomTab(context, url);
                return;
            }

            final Intent viewIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));

            // Always shows an application chooser with all possible targets
            if (forceIntentChooser) {
                final Intent chooser = Intent.createChooser(viewIntent, context.getString(R.string.cache_menu_browser));
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);

                final Intent customTabs = new Intent(context, StartWebviewActivity.class);

                final List<ResolveInfo> alreadyExistingShareIntents = context.getPackageManager().queryIntentActivities(viewIntent, PackageManager.MATCH_DEFAULT_ONLY);
                final List<Intent> additionalTargetedShareIntents = new ArrayList<>();
                customTabs.setData(Uri.parse(url));
                additionalTargetedShareIntents.add(customTabs);

                // on some devices, only c:geo is returned as possible share target, if it is set as default.
                // Therefore we need to query the installed browsers and check, whether it is already listed in the intent chooser.
                for (ResolveInfo resolveInfo : ProcessUtils.getInstalledBrowsers(context)) {
                    if (IterableUtils.find(alreadyExistingShareIntents,
                            info -> resolveInfo.activityInfo.packageName.equals(info.activityInfo.packageName)) == null) {
                        final Intent targetedShare = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        targetedShare.setPackage(resolveInfo.activityInfo.packageName);
                        additionalTargetedShareIntents.add(targetedShare);
                    }
                }

                chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, additionalTargetedShareIntents.toArray(new Parcelable[]{}));
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    chooser.putExtra(Intent.EXTRA_EXCLUDE_COMPONENTS, new Parcelable[]{new ComponentName(context, CacheDetailActivity.class)});
                }

                context.startActivity(chooser);
            } else {
                context.startActivity(viewIntent);
            }

        } catch (final ActivityNotFoundException e) {
            Log.e("Cannot find suitable activity for URL '" + url + "'", e);
            ActivityMixin.showToast(context, R.string.err_application_no);
        }
    }

    /**
     * Don't call this method, if you don't know whether Chrome is installed
     */
    public static void openCustomTab(final Context context, final String url) {
        final CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder()
                .setUrlBarHidingEnabled(true)
                .setShowTitle(true)
                .setShareState(CustomTabsIntent.SHARE_STATE_ON);

        final Intent actionIntent = new Intent(context, ShareBroadcastReceiver.class);
        final PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, actionIntent, (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0) | PendingIntent.FLAG_UPDATE_CURRENT);
        builder.addMenuItem(context.getString(R.string.cache_menu_open_with), pendingIntent);

        final CustomTabsIntent customTabsIntent = builder.build();
        // custom tabs API was restricted to chrome as other browsers like firefox may loop back to c:geo (as of September 2020)
        customTabsIntent.intent.setPackage(CHROME_PACKAGE_NAME);
        customTabsIntent.launchUrl(context, Uri.parse(url));
    }
}
