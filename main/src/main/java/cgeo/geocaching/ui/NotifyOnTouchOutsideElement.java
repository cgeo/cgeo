package cgeo.geocaching.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;

/**
 * Notify me when touch is outside a specified view element
 */
public class NotifyOnTouchOutsideElement extends LinearLayout {

    private View exceptionElement;
    private Runnable notify;

    public NotifyOnTouchOutsideElement(final Context context) {
        super(context);
    }

    public NotifyOnTouchOutsideElement(final Context context, final AttributeSet attrs) {
        super(context, attrs);
    }

    public NotifyOnTouchOutsideElement(final Context context, final AttributeSet attrs, final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setExceptionElement(final @NonNull View exceptionElement, final @NonNull Runnable notify) {
        this.exceptionElement = exceptionElement;
        this.notify = notify;
    }

    @Override
    public boolean dispatchTouchEvent(final MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN && exceptionElement != null) {
            // calculate relative coordinates of element to be checked
            final int[] middleLocation = new int[2];
            exceptionElement.getLocationInWindow(middleLocation);

            final int left = middleLocation[0];
            final int top = middleLocation[1];
            final int right = left + exceptionElement.getWidth();
            final int bottom = top + exceptionElement.getHeight();

            // relative coordinates of touch
            final float x = ev.getX();
            final float y = ev.getY();

            // touched outside exceptionElement?
            if (x < left || x > right || y < top || y > bottom) {
                post(() -> notify.run());
            }

        }
        return super.dispatchTouchEvent(ev);
    }
}
