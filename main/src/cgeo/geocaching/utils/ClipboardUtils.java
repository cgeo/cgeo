package cgeo.geocaching.utils;

import cgeo.geocaching.CgeoApplication;

import org.eclipse.jdt.annotation.Nullable;

import android.content.Context;

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
    @SuppressWarnings("deprecation")
    public static void copyToClipboard(final CharSequence text) {
        // fully qualified name used here to avoid buggy deprecation warning (of javac) on the import statement
        final android.text.ClipboardManager clipboard = (android.text.ClipboardManager) CgeoApplication.getInstance().getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setText(text);
    }

    /**
     * get clipboard content
     *
     */
    @SuppressWarnings("deprecation")
    @Nullable
    public static String getText() {
        // fully qualified name used here to avoid buggy deprecation warning (of javac) on the import statement
        final android.text.ClipboardManager clipboard = (android.text.ClipboardManager) CgeoApplication.getInstance().getSystemService(Context.CLIPBOARD_SERVICE);
        final CharSequence text = clipboard.getText();
        return text != null ? text.toString() : null;
    }

}
