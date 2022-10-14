package cgeo.geocaching.ui;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.Browser;
import android.text.Layout;
import android.text.Selection;
import android.text.Spannable;
import android.text.method.LinkMovementMethod;
import android.text.method.Touch;
import android.text.style.ClickableSpan;
import android.text.style.URLSpan;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;

/**
 * {@code LinkMovementMethod} with built-in suppression of errors for links, where the URL cannot be handled
 * correctly by Android.
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
    public boolean onTouchEvent(@NonNull final TextView widget, @NonNull final Spannable buffer, @NonNull final MotionEvent event) {
        try {
            final int action = event.getAction();

            if (action == MotionEvent.ACTION_UP ||
                    action == MotionEvent.ACTION_DOWN) {
                int x = (int) event.getX();
                int y = (int) event.getY();

                x -= widget.getTotalPaddingLeft();
                y -= widget.getTotalPaddingTop();

                x += widget.getScrollX();
                y += widget.getScrollY();

                final Layout layout = widget.getLayout();
                final int line = layout.getLineForVertical(y);
                final int off = layout.getOffsetForHorizontal(line, x);

                final ClickableSpan[] link = buffer.getSpans(off, off, ClickableSpan.class);

                if (link.length != 0) {
                    if (action == MotionEvent.ACTION_UP) {
                        if (link[0] instanceof URLSpan) {
                            // copied from URLSpan.java
                            final Uri uri = Uri.parse(((URLSpan) link[0]).getURL());
                            final Context context = widget.getContext();
                            final Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                            intent.putExtra(Browser.EXTRA_APPLICATION_ID, context.getPackageName());
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // this is different from the original!
                            try {
                                context.startActivity(intent);
                            } catch (ActivityNotFoundException e) {
                                Log.w("URLSpan", "Actvity was not found for intent, " + intent);
                            }
                            // end copy from URLSpan.java
                        } else {
                            link[0].onClick(widget);
                        }
                    } else {
                        Selection.setSelection(buffer,
                                buffer.getSpanStart(link[0]),
                                buffer.getSpanEnd(link[0]));
                    }
                    return true;
                }
            }

            return Touch.onTouchEvent(widget, buffer, event);
        } catch (final Exception ignored) {
            // local links to anchors don't work
        }
        return false;
    }

    @Override
    public void onTakeFocus(final TextView view, @NonNull final Spannable text, final int dir) {
        if ((dir & (View.FOCUS_FORWARD | View.FOCUS_DOWN)) != 0) {
            if (view.getLayout() == null) {
                // This shouldn't be null, but do something sensible if it is.
                Selection.setSelection(text, text.length());
            }
        } else {
            Selection.setSelection(text, text.length());
        }
    }

    @Override
    public boolean canSelectArbitrarily() {
        return true;
    }
}
