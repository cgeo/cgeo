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

import cgeo.geocaching.utils.ShareUtils

import android.text.Layout
import android.text.Selection
import android.text.Spannable
import android.text.method.LinkMovementMethod
import android.text.method.Touch
import android.text.style.ClickableSpan
import android.text.style.URLSpan
import android.view.MotionEvent
import android.view.View
import android.widget.TextView

import androidx.annotation.NonNull

/**
 * {@code LinkMovementMethod} with built-in suppression of errors for links, where the URL cannot be handled
 * correctly by Android.
 */
class AnchorAwareLinkMovementMethod : LinkMovementMethod() {

    private AnchorAwareLinkMovementMethod() {
        // singleton
    }

    private static class Holder {
        // initialization on demand holder
        private static val INSTANCE: AnchorAwareLinkMovementMethod = AnchorAwareLinkMovementMethod()
    }

    public static AnchorAwareLinkMovementMethod getInstance() {
        return Holder.INSTANCE
    }

    override     public Boolean onTouchEvent(final TextView widget, final Spannable buffer, final MotionEvent event) {
        try {
            val action: Int = event.getAction()

            if (action == MotionEvent.ACTION_UP ||
                    action == MotionEvent.ACTION_DOWN) {
                Int x = (Int) event.getX()
                Int y = (Int) event.getY()

                x -= widget.getTotalPaddingLeft()
                y -= widget.getTotalPaddingTop()

                x += widget.getScrollX()
                y += widget.getScrollY()

                val layout: Layout = widget.getLayout()
                val line: Int = layout.getLineForVertical(y)
                val off: Int = layout.getOffsetForHorizontal(line, x)

                final ClickableSpan[] link = buffer.getSpans(off, off, ClickableSpan.class)

                if (link.length != 0) {
                    if (action == MotionEvent.ACTION_UP) {
                        if (link[0] is URLSpan) {
                            ShareUtils.openUrl(widget.getContext(), ((URLSpan) link[0]).getURL())
                        } else {
                            link[0].onClick(widget)
                        }
                    } else {
                        Selection.setSelection(buffer,
                                buffer.getSpanStart(link[0]),
                                buffer.getSpanEnd(link[0]))
                    }
                    return true
                }
            }

            return Touch.onTouchEvent(widget, buffer, event)
        } catch (final Exception ignored) {
            // local links to anchors don't work
        }
        return false
    }

    override     public Unit onTakeFocus(final TextView view, final Spannable text, final Int dir) {
        if ((dir & (View.FOCUS_FORWARD | View.FOCUS_DOWN)) != 0) {
            if (view.getLayout() == null) {
                // This shouldn't be null, but do something sensible if it is.
                Selection.setSelection(text, text.length())
            }
        } else {
            Selection.setSelection(text, text.length())
        }
    }
}
