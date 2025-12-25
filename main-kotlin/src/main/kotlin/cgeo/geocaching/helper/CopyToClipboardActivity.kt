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

package cgeo.geocaching.helper

import cgeo.geocaching.R
import cgeo.geocaching.activity.AbstractActivity
import cgeo.geocaching.ui.ViewUtils
import cgeo.geocaching.utils.ClipboardUtils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle

import androidx.annotation.Nullable

import org.apache.commons.lang3.StringUtils

/**
 * Helper activity to copy simple content to clipboard
 */
class CopyToClipboardActivity : AbstractActivity() {


    public static Intent createClipboardIntent(final Context context, final CharSequence text, final Uri uri) {
        if (context == null || (text == null && uri == null)) {
            return null
        }
        val clipboardIntent: Intent = Intent(context, CopyToClipboardActivity.class)
        clipboardIntent.putExtra(Intent.EXTRA_TEXT, text)
        clipboardIntent.putExtra(Intent.EXTRA_STREAM, uri)
        return clipboardIntent
    }

    override     public Unit onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState)

        val bundle: Bundle = getIntent().getExtras()
        if (bundle != null && !StringUtils.isBlank(bundle.getString(Intent.EXTRA_TEXT))) {
            ClipboardUtils.copyToClipboard(bundle.getString(Intent.EXTRA_TEXT))
            ViewUtils.showToast(this, R.string.clipboard_copy_ok)
        } else if (bundle != null && bundle.getParcelable(Intent.EXTRA_STREAM) != null) {
            ClipboardUtils.copyToClipboard(bundle.getString(Intent.EXTRA_STREAM))
            ViewUtils.showToast(this, R.string.clipboard_copy_ok)
        } else {
            ViewUtils.showToast(this, R.string.clipboard_copy_failed)
        }

        finish()
    }
}
