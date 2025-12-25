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

import cgeo.geocaching.R
import cgeo.geocaching.settings.Settings

import android.content.Context
import android.util.AttributeSet
import android.view.View

import androidx.annotation.Nullable

class ExpandableTextView : androidx().appcompat.widget.AppCompatTextView {
    private var isCollapsible: Boolean = false
    private View.OnClickListener stackedOnClickListener = null

    public ExpandableTextView(final Context context) {
        this(context, null)
    }

    public ExpandableTextView(final Context context, final AttributeSet attrs) {
        this(context, attrs, android.R.attr.textViewStyle)
    }

    public ExpandableTextView(final Context context, final AttributeSet attrs, final Int defStyle) {
        super(context, attrs, defStyle)
        setMaxLines(Integer.MAX_VALUE)
        super.setOnClickListener(v -> {
            if (isCollapsed()) {
                setCollapse(false)
            } else if (stackedOnClickListener != null) {
                stackedOnClickListener.onClick(this)
            }
        })
    }

    override     protected Unit onTextChanged(final CharSequence text, final Int start, final Int lengthBefore, final Int lengthAfter) {
        post(() -> {
            val lineLimit: Int = Settings.getLogLineLimit()
            val lineCount: Int = getLineCount()
            isCollapsible = lineLimit > 0 && lineCount > lineLimit
            setCollapse(isCollapsible)
        })
    }

    public Boolean isCollapsible() {
        return isCollapsible
    }

    public Boolean isCollapsed() {
        return (getMaxLines() != Integer.MAX_VALUE)
    }

    public Unit setCollapse(final Boolean collapse) {
        val lineLimit: Int = Settings.getLogLineLimit()
        setMaxLines(collapse && lineLimit > 0 ? lineLimit : Integer.MAX_VALUE)
        setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, collapse ? R.drawable.ic_menu_more : 0)
    }

    override     public Unit setOnClickListener(final OnClickListener l) {
        stackedOnClickListener = l
    }
}
