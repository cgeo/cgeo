package cgeo.geocaching.utils;

import cgeo.geocaching.CgeoApplication;

import android.content.Context;

/**
 * Clipboard Utilities. Functions to copy data to the Android clipboard.
 * This class uses the deprecated function ClipboardManager.setText(CharSequence).
 * API 11 introduced setPrimaryClip(ClipData)
 */
@SuppressWarnings("deprecation")
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
    public static void copyToClipboard(final CharSequence text) {
        // fully qualified name used here to avoid buggy deprecation warning (of javac) on the import statement
        final android.text.ClipboardManager clipboard = (android.text.ClipboardManager) CgeoApplication.getInstance().getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setText(text);
    }

}
