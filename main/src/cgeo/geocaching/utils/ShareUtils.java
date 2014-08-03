package cgeo.geocaching.utils;

import org.eclipse.jdt.annotation.NonNull;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import java.io.File;

public class ShareUtils {
    private ShareUtils() {
        // utility class
    }

    public static void share(final Context context, final @NonNull File file, final @NonNull String mimeType, final int titleResourceId) {
        final Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
        shareIntent.setType(mimeType);
        context.startActivity(Intent.createChooser(shareIntent, context.getString(titleResourceId)));
    }

    public static void share(final Context context, final @NonNull File file, final int titleResourceId) {
        share(context, file, "*/*", titleResourceId);
    }
}
