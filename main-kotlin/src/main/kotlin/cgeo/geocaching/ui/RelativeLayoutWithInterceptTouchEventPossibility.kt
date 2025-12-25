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
import android.view.MotionEvent
import android.widget.RelativeLayout

class RelativeLayoutWithInterceptTouchEventPossibility : RelativeLayout() {
    private var onInterceptTouchEventListener: OnInterceptTouchEventListener = null

    public RelativeLayoutWithInterceptTouchEventPossibility(final Context context) {
        super(context)
    }

    public RelativeLayoutWithInterceptTouchEventPossibility(final Context context, final AttributeSet attrs) {
        super(context, attrs)
    }

    public RelativeLayoutWithInterceptTouchEventPossibility(final Context context, final AttributeSet attrs, final Int defStyleAttr) {
        super(context, attrs, defStyleAttr)
    }

    public RelativeLayoutWithInterceptTouchEventPossibility(final Context context, final AttributeSet attrs, final Int defStyleAttr, final Int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes)
    }

    override     public Boolean onInterceptTouchEvent(final MotionEvent ev) {
        if (onInterceptTouchEventListener != null && onInterceptTouchEventListener.onTouchEvent(ev)) {
            return true
        }
        return super.onInterceptTouchEvent(ev)
    }

    public Unit setOnInterceptTouchEventListener(final OnInterceptTouchEventListener listener) {
        onInterceptTouchEventListener = listener
    }

    interface OnInterceptTouchEventListener {
        /**
         * Called when the view group is touched.
         *
         * @param ev The motion event
         * @return true if the touch event should be intercepted, otherwise false.
         */
        Boolean onTouchEvent(MotionEvent ev)
    }
}
