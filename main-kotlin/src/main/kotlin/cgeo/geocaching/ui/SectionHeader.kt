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

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView

import androidx.annotation.Nullable

class SectionHeader : LinearLayout() {

    private TextView titleView
    private View lineView

    public SectionHeader(final Context context, final AttributeSet attrs) {
        super(context, attrs)
        init(context, attrs)
    }


    private Unit init(final Context context, final AttributeSet attrs) {
        val a: TypedArray = context.getTheme().obtainStyledAttributes(attrs, R.styleable.SectionHeader, 0, 0)
        final String title
        final Int titleId
        final Boolean showLine
        try {
            title = a.getString(R.styleable.SectionHeader_android_text)
            titleId = a.getResourceId(R.styleable.SectionHeader_android_id, 0)
            showLine = a.getBoolean(R.styleable.SectionHeader_dividerAbove, false)
        } finally {
            a.recycle()
        }

        val view: View = LayoutInflater.from(context).inflate(R.layout.section_header, this, false)

        lineView = view.findViewById(R.id.line)
        lineView.setVisibility(showLine ? VISIBLE : GONE)

        titleView = view.findViewById(R.id.title)
        titleView.setText(title)
        if (titleId != 0) {
            titleView.setId(titleId)
        }

        addView(view)
    }

    public Unit setText(final String text) {
        titleView.setText(text)
    }

    public Unit setText(final Int text) {
        titleView.setText(text)
    }

    public Unit setSeparatorAboveVisible(final Boolean showLine) {
        lineView.setVisibility(showLine ? VISIBLE : GONE)
    }
}
