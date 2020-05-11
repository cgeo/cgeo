package cgeo.geocaching.utils;

import cgeo.geocaching.R;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.content.FileProvider;

import java.io.File;

import org.apache.commons.lang3.StringUtils;

public class ShareUtils {
    private ShareUtils() {
        // utility class
    }

    public static final String TYPE_EMAIL = "vnd.android.cursor.dir/email";

    public static void share(final Context context, @NonNull final File file, @NonNull final String mimeType, @StringRes final int titleResourceId) {
        shareInternal(context, mimeType, null, null, file, titleResourceId);
    }

    public static void share(final Context context, @NonNull final File file, @StringRes final int titleResourceId) {
        shareInternal(context, "*/*", null, null, file, titleResourceId);
    }

    public static void shareAsEMail(final Context context, final String subject, final String body, @Nullable final File file, @StringRes final int titleResourceId) {
        shareInternal(context, TYPE_EMAIL, subject, body, file, titleResourceId);
    }

    private static void shareInternal(final Context context, @NonNull final String mimeType, @Nullable final String subject, @Nullable final String body, @Nullable final File file, @StringRes final int titleResourceId) {
        Uri uri = null;
        if (null != file) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                uri = Uri.fromFile(file);
            } else {
                try {
                    uri = FileProvider.getUriForFile(context, context.getString(R.string.file_provider_authority), file);
                } catch (Exception e) {
                    Log.e("error on LogCat sharing");
                }
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
            if (null != file) {
                // Grant temporary read permission to the content URI.
                intent.putExtra(Intent.EXTRA_STREAM, uri);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }
            context.startActivity(Intent.createChooser(intent, context.getString(titleResourceId)));
        }

    }

}
