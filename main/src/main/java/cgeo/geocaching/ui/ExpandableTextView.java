package cgeo.geocaching.ui;

import cgeo.geocaching.R;

import android.content.Context;
import android.util.AttributeSet;

public class ExpandableTextView extends androidx.appcompat.widget.AppCompatTextView {
    private static final int MAX_LINES = 5;
    private boolean isCollapsible = false;

    public ExpandableTextView(final Context context) {
        this(context, null);
    }

    public ExpandableTextView(final Context context, final AttributeSet attrs) {
        this(context, attrs, android.R.attr.textViewStyle);
    }

    public ExpandableTextView(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);
        setCollapse(true);
        this.setOnLongClickListener(v -> {
            final boolean isCollapsed = isCollapsed();
            if (isCollapsed) {
                setCollapse(false);
            }
            return isCollapsed;
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
}
