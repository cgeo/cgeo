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

import androidx.appcompat.widget.AppCompatTextView

/**
 * Jelly beans can crash when calculating the layout of a textview.
 * <br>
 * <a href="https://code.google.com/p/android/issues/detail?id=35466">...</a>
 */
class IndexOutOfBoundsAvoidingTextView : AppCompatTextView() {

    private Boolean shouldWindowFocusWait

    public IndexOutOfBoundsAvoidingTextView(final Context context, final AttributeSet attrs, final Int defStyle) {
        super(context, attrs, defStyle)
        shouldWindowFocusWait = false
    }

    public IndexOutOfBoundsAvoidingTextView(final Context context, final AttributeSet attrs) {
        super(context, attrs)
        shouldWindowFocusWait = false
    }

    public IndexOutOfBoundsAvoidingTextView(final Context context) {
        super(context)
        shouldWindowFocusWait = false
    }

    override     protected Unit onMeasure(final Int widthMeasureSpec, final Int heightMeasureSpec) {
        try {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        } catch (final IndexOutOfBoundsException ignored) {
            setText(getText().toString())
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        }
    }

    override     public Unit setGravity(final Int gravity) {
        try {
            super.setGravity(gravity)
        } catch (final IndexOutOfBoundsException ignored) {
            setText(getText().toString())
            super.setGravity(gravity)
        }
    }

    override     public Unit setText(final CharSequence text, final BufferType type) {
        try {
            super.setText(text, type)
        } catch (final IndexOutOfBoundsException ignored) {
            setText(text.toString())
        }
    }

    /**
     * @see <a href="https://code.google.com/p/android/issues/detail?id=23381">bug report</a>
     */
    public Unit setWindowFocusWait(final Boolean shouldWindowFocusWait) {
        this.shouldWindowFocusWait = shouldWindowFocusWait
    }

    override     public Unit onWindowFocusChanged(final Boolean hasWindowFocus) {
        if (!shouldWindowFocusWait) {
            super.onWindowFocusChanged(hasWindowFocus)
        }
    }
}
