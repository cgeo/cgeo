package cgeo.geocaching.utils;

import android.support.annotation.NonNull;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.StringRes;

import java.io.File;

public class ShareUtils {
    private ShareUtils() {
        // utility class
    }

    public static void share(final Context context, @NonNull final File file, @NonNull final String mimeType, @StringRes final int titleResourceId) {
        final Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
        shareIntent.setType(mimeType);
        context.startActivity(Intent.createChooser(shareIntent, context.getString(titleResourceId)));
    }

    public static void share(final Context context, @NonNull final File file, @StringRes final int titleResourceId) {
        share(context, file, "*/*", titleResourceId);
    }
}
