package cgeo.geocaching.ui;

import cgeo.geocaching.R;

import android.content.Context;
import android.util.AttributeSet;

public class ExpandableTextView extends androidx.appcompat.widget.AppCompatTextView {
    private static final int MAX_LINES = 5;

    public ExpandableTextView(final Context context) {
        this(context, null);
    }

    public ExpandableTextView(final Context context, final AttributeSet attrs) {
        this(context, attrs, android.R.attr.textViewStyle);
    }

    public ExpandableTextView(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);
        setMaxLines(MAX_LINES);
        this.setOnLongClickListener(v -> {
            final boolean isCollapsed = (getMaxLines() != Integer.MAX_VALUE);
            if (isCollapsed) {
                setMaxLines(Integer.MAX_VALUE);
                setImage(false);
            }
            return isCollapsed;
        });
    }

    @Override
    protected void onTextChanged(final CharSequence text, final int start, final int lengthBefore, final int lengthAfter) {
        post(() -> setImage(getLineCount() > MAX_LINES));
    }

    private void setImage(final boolean show) {
        setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, show ? R.drawable.ic_menu_more : 0);
    }
}
