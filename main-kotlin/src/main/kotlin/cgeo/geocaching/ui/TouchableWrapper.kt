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

import cgeo.geocaching.utils.functions.Action1

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.FrameLayout

import androidx.annotation.Nullable

class TouchableWrapper : FrameLayout() {

    Action1<MotionEvent> onTouch = null

    public TouchableWrapper(final Context context) {
        super(context, null)
    }

    public TouchableWrapper(final Context context, final AttributeSet attrs) {
        this(context, attrs, 0)
    }

    public TouchableWrapper(final Context context, final AttributeSet attrs, final Int defStyleAttr) {
        super(context, attrs, defStyleAttr)
    }

    public Unit setOnTouch(final Action1<MotionEvent> onTouch) {
        this.onTouch = onTouch
    }

    public Boolean dispatchTouchEvent(final MotionEvent event) {
        if (onTouch != null) {
            onTouch.call(event)
        }
        return super.dispatchTouchEvent(event)
    }
}
