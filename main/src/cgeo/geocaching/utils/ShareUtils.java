package cgeo.geocaching.utils;

import cgeo.geocaching.R;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.core.content.FileProvider;

import java.io.File;

public class ShareUtils {
    private ShareUtils() {
        // utility class
    }

    public static void share(final Context context, @NonNull final File file, @NonNull final String mimeType, @StringRes final int titleResourceId) {
        final Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        final Uri uri = FileProvider.getUriForFile(context, context.getString(R.string.file_provider_authority), file);
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
        shareIntent.setType(mimeType);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        context.startActivity(Intent.createChooser(shareIntent, context.getString(titleResourceId)));
    }

    public static void share(final Context context, @NonNull final File file, @StringRes final int titleResourceId) {
        share(context, file, "*/*", titleResourceId);
    }
}
