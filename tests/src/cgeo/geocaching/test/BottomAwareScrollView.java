package cgeo.geocaching.test;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ScrollView;

public class BottomAwareScrollView extends ScrollView {

    private boolean isAtBottom = true;

    public BottomAwareScrollView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public BottomAwareScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public BottomAwareScrollView(Context context) {
        super(context);
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        View lastChildView = getChildAt(getChildCount() - 1);
        int diff = (lastChildView.getBottom() - (getHeight() + getScrollY()));
        isAtBottom = diff <= 0;
        super.onScrollChanged(l, t, oldl, oldt);
    }

    public boolean isAtBottom() {
        return isAtBottom;
    }

}
