package cgeo.geocaching.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.GridView;

/**
 * GridView that will adjust its height to really use wrap_content. The standard GridView only shows one line of items.
 *
 * @see <a href="https://gist.github.com/runemart/9781609">https://gist.github.com/runemart/9781609</a>
 */
public class WrappingGridView extends GridView {

    public WrappingGridView(final Context context) {
        super(context);
    }

    public WrappingGridView(final Context context, final AttributeSet attrs) {
        super(context, attrs);
    }

    public WrappingGridView(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec) {
        int heightSpec = heightMeasureSpec;
        if (getLayoutParams().height == android.view.ViewGroup.LayoutParams.WRAP_CONTENT) {
            // The two leftmost bits in the height measure spec have
            // a special meaning, hence we can't use them to describe height.
            heightSpec = MeasureSpec.makeMeasureSpec(Integer.MAX_VALUE >> 2, MeasureSpec.AT_MOST);
        }
        super.onMeasure(widthMeasureSpec, heightSpec);
    }

}
