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
import android.widget.GridView

/**
 * GridView that will adjust its height to really use wrap_content. The standard GridView only shows one line of items.
 *
 * @see <a href="https://gist.github.com/runemart/9781609">https://gist.github.com/runemart/9781609</a>
 */
class WrappingGridView : GridView() {

    public WrappingGridView(final Context context) {
        super(context)
    }

    public WrappingGridView(final Context context, final AttributeSet attrs) {
        super(context, attrs)
    }

    public WrappingGridView(final Context context, final AttributeSet attrs, final Int defStyle) {
        super(context, attrs, defStyle)
    }

    override     protected Unit onMeasure(final Int widthMeasureSpec, final Int heightMeasureSpec) {
        Int heightSpec = heightMeasureSpec
        if (getLayoutParams().height == android.view.ViewGroup.LayoutParams.WRAP_CONTENT) {
            // The two leftmost bits in the height measure spec have
            // a special meaning, hence we can't use them to describe height.
            heightSpec = MeasureSpec.makeMeasureSpec(Integer.MAX_VALUE >> 2, MeasureSpec.AT_MOST)
        }
        super.onMeasure(widthMeasureSpec, heightSpec)
    }

}
