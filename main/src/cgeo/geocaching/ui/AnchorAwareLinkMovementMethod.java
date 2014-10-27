package cgeo.geocaching.ui;

import android.text.Spannable;
import android.text.method.LinkMovementMethod;
import android.view.MotionEvent;
import android.widget.TextView;

/**
 * <code>LinkMovementMethod</code> with built-in suppression of errors for links, where the URL cannot be handled
 * correctly by Android.
 *
 */
public class AnchorAwareLinkMovementMethod extends LinkMovementMethod {

    private AnchorAwareLinkMovementMethod() {
        // singleton
    }

    private static final class Holder {
        // initialization on demand holder
        private static final AnchorAwareLinkMovementMethod INSTANCE = new AnchorAwareLinkMovementMethod();
    }

    public static AnchorAwareLinkMovementMethod getInstance() {
        return Holder.INSTANCE;
    }

    @Override
    public boolean onTouchEvent(TextView widget, Spannable buffer, MotionEvent event) {
        try {
            return super.onTouchEvent(widget, buffer, event);
        } catch (Exception ignored) {
            // local links to anchors don't work
        }
        return false;
    }
}
