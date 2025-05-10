package cgeo.geocaching.ui;

import cgeo.geocaching.R;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

public class ExpandableTextView extends androidx.appcompat.widget.AppCompatTextView {
    private static final int MAX_LINES = 15;
    private boolean isCollapsible = false;
    private View.OnClickListener stackedOnClickListener = null;

    public ExpandableTextView(final Context context) {
        this(context, null);
    }

    public ExpandableTextView(final Context context, final AttributeSet attrs) {
        this(context, attrs, android.R.attr.textViewStyle);
    }

    public ExpandableTextView(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);
        setMaxLines(Integer.MAX_VALUE);
        super.setOnClickListener(v -> {
            if (isCollapsed()) {
                setCollapse(false);
            } else if (stackedOnClickListener != null) {
                stackedOnClickListener.onClick(this);
            }
        });
    }

    @Override
    protected void onTextChanged(final CharSequence text, final int start, final int lengthBefore, final int lengthAfter) {
        post(() -> {
            isCollapsible = getLineCount() > MAX_LINES;
            setCollapse(isCollapsible);
        });
    }

    public boolean isCollapsible() {
        return isCollapsible;
    }

    public boolean isCollapsed() {
        return (getMaxLines() != Integer.MAX_VALUE);
    }

    public void setCollapse(final boolean collapse) {
        setMaxLines(collapse ? MAX_LINES : Integer.MAX_VALUE);
        setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, collapse ? R.drawable.ic_menu_more : 0);
    }

    @Override
    public void setOnClickListener(@Nullable final OnClickListener l) {
        stackedOnClickListener = l;
    }
}
