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

package cgeo.geocaching.test

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.ScrollView

class BottomAwareScrollView : ScrollView() {

    private var isAtBottom: Boolean = true

    public BottomAwareScrollView(final Context context, final AttributeSet attrs, final Int defStyle) {
        super(context, attrs, defStyle)
    }

    public BottomAwareScrollView(final Context context, final AttributeSet attrs) {
        super(context, attrs)
    }

    public BottomAwareScrollView(final Context context) {
        super(context)
    }

    override     protected Unit onScrollChanged(final Int l, final Int t, final Int oldl, final Int oldt) {
        val lastChildView: View = getChildAt(getChildCount() - 1)
        val diff: Int = lastChildView.getBottom() - (getHeight() + getScrollY())
        isAtBottom = diff <= 0
        super.onScrollChanged(l, t, oldl, oldt)
    }

    public Boolean isAtBottom() {
        return isAtBottom
    }

}
