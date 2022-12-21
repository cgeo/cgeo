package cgeo.geocaching.ui;

import cgeo.geocaching.utils.functions.Action1;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;

public class TouchableWrapper extends FrameLayout {

    Action1<MotionEvent> onTouch = null;

    public TouchableWrapper(final Context context) {
        super(context, null);
    }

    public TouchableWrapper(final Context context, final AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TouchableWrapper(final Context context, @Nullable final AttributeSet attrs, final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setOnTouch(final Action1<MotionEvent> onTouch) {
        this.onTouch = onTouch;
    }

    public boolean dispatchTouchEvent(final MotionEvent event) {
        if (onTouch != null) {
            onTouch.call(event);
        }
        return super.dispatchTouchEvent(event);
    }
}
