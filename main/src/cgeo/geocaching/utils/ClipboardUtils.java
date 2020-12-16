package cgeo.geocaching.utils;

import cgeo.geocaching.CgeoApplication;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Clipboard Utilities. Functions to copy data to the Android clipboard.
 * This class uses the deprecated function ClipboardManager.setText(CharSequence).
 * API 11 introduced setPrimaryClip(ClipData)
 */
public final class ClipboardUtils {

    private ClipboardUtils() {
        // utility class
    }

    /**
     * Places the text passed in onto the clipboard as text
     *
     * @param text
     *            The text to place in the clipboard.
     */
    public static void copyToClipboard(@NonNull final CharSequence text) {
        // fully qualified name used here to avoid buggy deprecation warning (of javac) on the import statement
        final ClipboardManager clipboard = (ClipboardManager) CgeoApplication.getInstance().getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            final ClipData data = ClipData.newPlainText(null, text);
            clipboard.setPrimaryClip(data);
        }
    }

    /**
     * get clipboard content
     *
     */
    @Nullable
    public static String getText() {
        // fully qualified name used here to avoid buggy deprecation warning (of javac) on the import statement
        final ClipboardManager clipboard = (ClipboardManager) CgeoApplication.getInstance().getSystemService(Context.CLIPBOARD_SERVICE);
        final CharSequence text = clipboard.getText();
        return text != null ? text.toString() : null;
    }

    /**
     * clear clipboard content
     * (up to API level 28: replace with empty string)
     */
    public static void clearClipboard() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            ((ClipboardManager) CgeoApplication.getInstance().getSystemService(Context.CLIPBOARD_SERVICE)).clearPrimaryClip();
        } else {
            copyToClipboard("");
        }
    }
}
