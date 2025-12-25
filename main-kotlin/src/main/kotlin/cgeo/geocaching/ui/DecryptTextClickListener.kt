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

package cgeo.geocaching.ui

import cgeo.geocaching.ui.dialog.ContextMenuDialog
import cgeo.geocaching.utils.CryptUtils
import cgeo.geocaching.utils.functions.Action1

import android.text.Spannable
import android.view.View
import android.widget.TextView

import androidx.annotation.NonNull

class DecryptTextClickListener : View.OnClickListener, Action1<ContextMenuDialog.Item> {

    private final TextView targetView

    public DecryptTextClickListener(final TextView targetView) {
        this.targetView = targetView
    }

    override     public final Unit onClick(final View view) {
        try {
            val text: CharSequence = targetView.getText()
            if (text is Spannable) {
                targetView.setText(CryptUtils.rot13((Spannable) text))
            } else {
                targetView.setText(CryptUtils.rot13((String) text))
            }
        } catch (final RuntimeException ignored) {
            // nothing
        }
    }

    override     public Unit call(final ContextMenuDialog.Item item) {
        onClick(null)
    }
}
