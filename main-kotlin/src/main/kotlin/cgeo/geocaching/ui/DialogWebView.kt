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

import android.content.Context
import android.util.AttributeSet
import android.webkit.WebView

import androidx.annotation.NonNull
import androidx.annotation.Nullable

/** A WebView usable in (Alert)Dialogs */
class DialogWebView : WebView() {
    public DialogWebView(final Context context) {
        super(context)
    }

    public DialogWebView(final Context context, final AttributeSet attrs) {
        super(context, attrs)
    }

    public DialogWebView(final Context context, final AttributeSet attrs, final Int defStyleAttr) {
        super(context, attrs, defStyleAttr)
    }

    public DialogWebView(final Context context, final AttributeSet attrs, final Int defStyleAttr, final Int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes)
    }

    /** This override is important so soft keyboard is shown on text input fields in WebView in Dialogs */
    override     public Boolean onCheckIsTextEditor() {
        return true
    }
}
