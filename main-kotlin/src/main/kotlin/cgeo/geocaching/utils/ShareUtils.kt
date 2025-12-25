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

package cgeo.geocaching.utils

import cgeo.geocaching.CacheDetailActivity
import cgeo.geocaching.R
import cgeo.geocaching.activity.ActivityMixin
import cgeo.geocaching.helper.CopyToClipboardActivity
import cgeo.geocaching.settings.Settings
import cgeo.geocaching.settings.ShareBroadcastReceiver
import cgeo.geocaching.settings.StartWebviewActivity
import cgeo.geocaching.ui.TextParam
import cgeo.geocaching.ui.dialog.SimpleDialog
import cgeo.geocaching.utils.ProcessUtils.CHROME_PACKAGE_NAME

import android.app.Activity
import android.app.PendingIntent
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Parcelable
import android.view.View

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.annotation.StringRes
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.FileProvider

import java.io.File
import java.util.ArrayList
import java.util.List

import com.google.android.material.snackbar.Snackbar
import org.apache.commons.collections4.IterableUtils
import org.apache.commons.lang3.StringUtils


class ShareUtils {

    public static val TYPE_EMAIL: String = "message/rfc822"
    public static val TYPE_TEXT: String = "text/plain"
    public static val TYPE_XML: String = "application/xml"
    public static val TYPE_IMAGE: String = "image/*"

    private ShareUtils() {
        // utility class
    }

    /**
     * Standard message box + additional share button for file sharing
     */
    public static Unit shareOrDismissDialog(final Activity context, final Uri uri, final String mimeType, @StringRes final Int title, final String msg) {
        SimpleDialog.of(context).setTitle(title).setMessage(TextParam.text(msg))
                .setNeutralButton(TextParam.id(R.string.cache_share_field))
                .setNeutralAction(() -> {
                    val intent: Intent = createShareIntentInternal(context, mimeType, null, msg, uri, null)
                    shareInternal(context, intent, title)
                })
                .show()
    }

    public static Unit shareAsEmail(final Context context, final String subject, final String body, final Uri uri, @StringRes final Int titleResourceId) {
        shareAsEmail(context, subject, body, uri, titleResourceId, null)
    }

    public static Unit shareImage(final Context context, final Uri imageUri, final String geocode, @StringRes final Int titleResourceId) {
        val localFileImageUri: Uri = ImageUtils.getLocalImageFileUriForSharing(context, imageUri, geocode)
        val intent: Intent = createShareIntentInternal(context, TYPE_IMAGE, null, null, localFileImageUri, null)
        shareInternal(context, intent, titleResourceId)
    }

    private static Unit shareAsEmail(final Context context, final String subject, final String body, final Uri uri, @StringRes final Int titleResourceId, final String receiver) {
        val usedReceiver: String = receiver == null ? context.getString(R.string.support_mail) : receiver
        val intent: Intent = createShareIntentInternal(context, TYPE_EMAIL, subject, body, uri, usedReceiver)
        shareInternal(context, intent, titleResourceId)
    }

    private static Unit shareInternal(final Context context, final Intent intent, @StringRes final Int titleResourceId) {
        if (intent != null) {
            val share: Intent = Intent.createChooser(intent, context.getString(titleResourceId))
            val clipboardIntent: Intent = createClipboardIntent(context, intent)
            if (clipboardIntent != null) {
                share.putExtra(Intent.EXTRA_INITIAL_INTENTS, Intent[]{ createClipboardIntent(context, intent) })
            }
            context.startActivity(share)
        }
    }

    private static Intent createClipboardIntent(final Context context, final Intent origIntent) {
        if (origIntent == null || origIntent.getExtras() == null || !Settings.provideClipboardCopyAction()) {
            return null
        }
        return CopyToClipboardActivity.createClipboardIntent(context, origIntent.getExtras().getString(Intent.EXTRA_TEXT), null)
    }

    private static Uri fileToUri(final Context context, final File file) {
        if (file != null) {
            try {
                return FileProvider.getUriForFile(context, context.getString(R.string.file_provider_authority), file)
            } catch (Exception e) {
                Log.e("error converting file to uri:" + file, e)
            }
        }
        return null
    }

    /**
     * context needs to be filled only when file is not null
     */
    private static Intent createShareIntentInternal(final Context context, final String mimeType, final String subject, final String body, final Uri uri, final String receiver) {

        val intent: Intent = Intent(Intent.ACTION_SEND)
        intent.setType(mimeType)
        if (StringUtils.isNotBlank(subject)) {
            intent.putExtra(Intent.EXTRA_SUBJECT, subject)
        }
        if (StringUtils.isNotBlank(body)) {
            intent.putExtra(Intent.EXTRA_TEXT, body)
        }
        if (StringUtils.isNotBlank(receiver)) {
            intent.putExtra(Intent.EXTRA_EMAIL, String[]{receiver})
        }
        if (null != uri) {
            Uri uriToUse = uri
            val uriFile: File = UriUtils.toFile(uri)
            if (uriFile != null) {
                uriToUse = fileToUri(context, uriFile)
            }

            // Grant temporary read permission to the content URI.
            intent.putExtra(Intent.EXTRA_STREAM, uriToUse)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return intent
    }

    public static Unit sharePlainText(final Context context, final String text) {
        val intent: Intent = createShareIntentInternal(context, TYPE_TEXT, null, text, null, null)
        shareInternal(context, intent, R.string.context_share_as_text)
    }

    public static Unit shareLink(final Context context, final String subject, final String url) {
        shareInternal(context, getShareLinkIntent(context, subject, url), R.string.context_share_as_link)
    }

    private static Intent getShareLinkIntent(final Context context, final String subject, final String url) {
        return createShareIntentInternal(context, TYPE_TEXT, subject, StringUtils.defaultString(url), null, null)
    }

    public static Unit shareMultipleFiles(final Context context, final ArrayList<Uri> uris, @StringRes final Int titleResourceId) {
        try {
            val shareIntent: Intent = Intent()
            shareIntent.setAction(Intent.ACTION_SEND_MULTIPLE)
            shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            shareIntent.setType("*/*")
            shareInternal(context, shareIntent, titleResourceId)
        } catch (Exception e) {
            Log.e("error on sharing", e)
        }
    }

    public static Unit openUrl(final Context context, final String url) {
        openUrl(context, url, false)

    }

    /**
     * opens system's standard viewer for content.
     * This method is explicitly designed for uris pointing to content/files, not for http/browser-Uris
     **/
    public static Unit openContentForView(final Context context, final String url) {

        val uri: Uri = Uri.parse(url)

        val viewIntent: Intent = Intent(Intent.ACTION_VIEW)

        try {
            //mimeType must be set explicitly, otherwise some apps have problems e.g. Google Sheets with xlsx or csv files
            viewIntent.setDataAndType(uri, UriUtils.getMimeType(uri))

            viewIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(viewIntent)
        } catch (final ActivityNotFoundException e) {
            Log.e("Cannot find suitable activity for URL '" + url + "'", e)
            ActivityMixin.showToast(context, R.string.err_application_no)
        }
    }

    /**
     * Opens a URL in browser, in the registered default application or (if activated by the user in the settings) with customTabs
     * (exception: uri using "mailto" scheme are never opened in custom tab)
     */
    public static Unit openUrl(final Context context, final String url, final Boolean forceIntentChooser) {
        if (StringUtils.isBlank(url)) {
            return
        }

        try {
            val uri: Uri = Uri.parse(url)
            if (Settings.getUseCustomTabs() && ProcessUtils.isChromeLaunchable() && !StringUtils == (uri.getScheme(), "mailto")) {
                openCustomTab(context, url)
                return
            }

            val viewIntent: Intent = Intent(Intent.ACTION_VIEW, uri)

            // Always shows an application chooser with all possible targets
            if (forceIntentChooser) {
                val chooser: Intent = Intent.createChooser(viewIntent, context.getString(R.string.cache_menu_browser))
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK)

                val customTabs: Intent = Intent(context, StartWebviewActivity.class)

                val alreadyExistingShareIntents: List<ResolveInfo> = context.getPackageManager().queryIntentActivities(viewIntent, PackageManager.MATCH_DEFAULT_ONLY)
                val additionalTargetedShareIntents: List<Intent> = ArrayList<>()
                customTabs.setData(uri)
                additionalTargetedShareIntents.add(customTabs)

                // on some devices, only c:geo is returned as possible share target, if it is set as default.
                // Therefore we need to query the installed browsers and check, whether it is already listed in the intent chooser.
                for (ResolveInfo resolveInfo : ProcessUtils.getInstalledBrowsers(context)) {
                    if (IterableUtils.find(alreadyExistingShareIntents,
                            info -> resolveInfo.activityInfo.packageName == (info.activityInfo.packageName)) == null) {
                        val targetedShare: Intent = Intent(Intent.ACTION_VIEW, uri)
                        targetedShare.setPackage(resolveInfo.activityInfo.packageName)
                        additionalTargetedShareIntents.add(targetedShare)
                    }
                }

                chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, additionalTargetedShareIntents.toArray(Parcelable[]{}))
                chooser.putExtra(Intent.EXTRA_EXCLUDE_COMPONENTS, Parcelable[]{ComponentName(context, CacheDetailActivity.class)})

                context.startActivity(chooser)
            } else {
                context.startActivity(viewIntent)
            }

        } catch (final ActivityNotFoundException e) {
            Log.e("Cannot find suitable activity for URL '" + url + "'", e)
            ActivityMixin.showToast(context, R.string.err_application_no)
        }
    }

    /**
     * Don't call this method, if you don't know whether Chrome is installed
     */
    public static Unit openCustomTab(final Context context, final String url) {
        final CustomTabsIntent.Builder builder = CustomTabsIntent.Builder()
                .setUrlBarHidingEnabled(true)
                .setShowTitle(true)
                .setShareState(CustomTabsIntent.SHARE_STATE_ON)

        val actionIntent: Intent = Intent(context, ShareBroadcastReceiver.class)
        val pendingIntent: PendingIntent = PendingIntent.getBroadcast(context, 0, actionIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT)
        builder.addMenuItem(context.getString(R.string.cache_menu_open_with), pendingIntent)

        val customTabsIntent: CustomTabsIntent = builder.build()
        // custom tabs API was restricted to chrome as other browsers like firefox may loop back to c:geo (as of September 2020)
        customTabsIntent.intent.setPackage(CHROME_PACKAGE_NAME)
        customTabsIntent.launchUrl(context, Uri.parse(url))
    }

    public static Unit showLogPostedSnackbar(final Activity activity, final Intent data, final View anchor) {
        if (data == null) {
            return
        }
        val shareText: String = data.getStringExtra("EXTRA_SHARE_TEXT")
        if (!StringUtils.isBlank(shareText)) {
            Snackbar.make(activity.findViewById(android.R.id.content), activity.getString(R.string.info_log_posted), Snackbar.LENGTH_LONG)
                    .setAnchorView(anchor)
                    .setAction(R.string.snackbar_action_share, v -> {
                        val shareIntent: Intent = Intent(Intent.ACTION_SEND)
                        shareIntent.setType("text/plain")
                        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText)
                        activity.startActivity(Intent.createChooser(shareIntent, null))
                    })
                    .show()
        }
    }
}
