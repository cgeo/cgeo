package cgeo.geocaching.utils;

import cgeo.geocaching.cgeoapplication;

import android.content.Context;
import android.text.ClipboardManager;

/**
 * Clipboard Utilities. Functions to copy data to the Android clipboard.
 * This class uses the deprecated function ClipboardManager.setText(CharSequence).
 * API 11 introduced setPrimaryClip(ClipData)
 */
public final class ClipboardUtils {

    /**
     * Places the text passed in onto the clipboard as text
     *
     * @param text
     *            The text to place in the clipboard.
     */
    public static void copyToClipboard(final CharSequence text) {
        final ClipboardManager clipboard = (ClipboardManager) cgeoapplication.getInstance().getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setText(text);
    }

}
