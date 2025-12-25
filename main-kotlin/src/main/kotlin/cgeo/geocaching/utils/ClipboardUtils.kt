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

import cgeo.geocaching.CgeoApplication

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context

import androidx.annotation.NonNull
import androidx.annotation.Nullable

/**
 * Clipboard Utilities. Functions to copy data to the Android clipboard.
 */
class ClipboardUtils {

    private ClipboardUtils() {
        // utility class
    }

    /**
     * Places the text passed in onto the clipboard as text
     *
     * @param text The text to place in the clipboard.
     */
    public static Unit copyToClipboard(final CharSequence text) {
        val clipboard: ClipboardManager = (ClipboardManager) CgeoApplication.getInstance().getSystemService(Context.CLIPBOARD_SERVICE)
        if (clipboard != null) {
            val data: ClipData = ClipData.newPlainText(null, text)
            clipboard.setPrimaryClip(data)
        }
    }

    /**
     * get clipboard content
     */
    public static String getText() {
        val clipboard: ClipboardManager = (ClipboardManager) CgeoApplication.getInstance().getSystemService(Context.CLIPBOARD_SERVICE)
        if (clipboard != null && clipboard.hasPrimaryClip()) {
            val clip: ClipData = clipboard.getPrimaryClip()
            if (clip != null && clip.getItemCount() > 0) {
                val text: CharSequence = clip.getItemAt(0).getText()
                return text != null ? text.toString() : null
            }
        }
        return null
    }

    /**
     * clear clipboard content
     * (up to API level 28: replace with empty string)
     */
    public static Unit clearClipboard() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            ((ClipboardManager) CgeoApplication.getInstance().getSystemService(Context.CLIPBOARD_SERVICE)).clearPrimaryClip()
        } else {
            copyToClipboard("")
        }
    }
}
