package cgeo.geocaching.test;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ScrollView;

public class BottomAwareScrollView extends ScrollView {

    private boolean isAtBottom = true;

    public BottomAwareScrollView(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);
    }

    public BottomAwareScrollView(final Context context, final AttributeSet attrs) {
        super(context, attrs);
    }

    public BottomAwareScrollView(final Context context) {
        super(context);
    }

    @Override
    protected void onScrollChanged(final int l, final int t, final int oldl, final int oldt) {
        final View lastChildView = getChildAt(getChildCount() - 1);
        final int diff = lastChildView.getBottom() - (getHeight() + getScrollY());
        isAtBottom = diff <= 0;
        super.onScrollChanged(l, t, oldl, oldt);
    }

    public boolean isAtBottom() {
        return isAtBottom;
    }

}
