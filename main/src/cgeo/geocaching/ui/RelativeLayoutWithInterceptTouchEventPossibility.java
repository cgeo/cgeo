package cgeo.geocaching.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.RelativeLayout;

public class RelativeLayoutWithInterceptTouchEventPossibility extends RelativeLayout {
    private OnInterceptTouchEventListener onInterceptTouchEventListener = null;

    public RelativeLayoutWithInterceptTouchEventPossibility(final Context context) {
        super(context);
    }

    public RelativeLayoutWithInterceptTouchEventPossibility(final Context context, final AttributeSet attrs) {
        super(context, attrs);
    }

    public RelativeLayoutWithInterceptTouchEventPossibility(final Context context, final AttributeSet attrs, final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public RelativeLayoutWithInterceptTouchEventPossibility(final Context context, final AttributeSet attrs, final int defStyleAttr, final int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    public boolean onInterceptTouchEvent(final MotionEvent ev) {
        if (onInterceptTouchEventListener != null && onInterceptTouchEventListener.onTouchEvent(ev)) {
            return true;
        }
        return super.onInterceptTouchEvent(ev);
    }

    public void setOnInterceptTouchEventListener(final OnInterceptTouchEventListener listener) {
        onInterceptTouchEventListener = listener;
    }

    public interface OnInterceptTouchEventListener {
        /**
         * Called when the view group is touched.
         *
         * @param ev The motion event
         * @return true if the touch event should be intercepted, otherwise false.
         */
        boolean onTouchEvent(MotionEvent ev);
    }
}
