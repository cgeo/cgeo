package cgeo.geocaching.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import cgeo.geocaching.R;

public class SectionHeader extends LinearLayout {

    private TextView titleView;

    public SectionHeader(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }


    private void init(Context context, @Nullable AttributeSet attrs) {
        final TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.SectionHeader, 0, 0);
        String title;
        int titleId;
        boolean showLine;
        try {
            title = a.getString(R.styleable.SectionHeader_android_text);
            titleId = a.getResourceId(R.styleable.SectionHeader_android_id, 0);
            showLine = a.getBoolean(R.styleable.SectionHeader_dividerAbove, false);
        } finally {
            a.recycle();
        }

        final View view = LayoutInflater.from(context).inflate(R.layout.section_header, this, false);

        final View lineView = view.findViewById(R.id.line);
        lineView.setVisibility(showLine ? VISIBLE : GONE);

        titleView = view.findViewById(R.id.title);
        titleView.setText(title);
        if (titleId != 0) {
            titleView.setId(titleId);
        }

        addView(view);
    }

    public void setText(final String text) {
        titleView.setText(text);
    }

    public void setText(final int text) {
        titleView.setText(text);
    }
}
