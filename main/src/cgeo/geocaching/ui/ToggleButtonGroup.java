package cgeo.geocaching.ui;

import cgeo.geocaching.R;
import cgeo.geocaching.utils.functions.Action2;
import static cgeo.geocaching.ui.ViewUtils.dpToPixel;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class ToggleButtonGroup extends LinearLayout {

    private final List<String> values = new ArrayList<>();
    private final List<ToggleButton> buttons = new ArrayList<>();
    private int selected = 0;

    private Action2<View, Integer> changeListener = null;

    public ToggleButtonGroup(final Context context) {
        super(context);
        init();
    }

    public ToggleButtonGroup(final Context context, @Nullable final AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ToggleButtonGroup(final Context context, @Nullable final AttributeSet attrs, final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public ToggleButtonGroup(final Context context, final AttributeSet attrs, final int defStyleAttr, final int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    public void setChangeListener(final Action2<View, Integer> listener) {
        this.changeListener = listener;
    }

    public void setValues(final List<String> values) {
        this.values.clear();
        this.values.addAll(values);
        relayout();
    }

    private void init() {
        this.setOrientation(HORIZONTAL);
        relayout();
    }

    private void relayout() {
        buttons.clear();
        this.removeAllViews();
        int idx = 0;
        for (String v : values) {
            final ToggleButton tb = createAddButton(v);
            final int bIdx = idx++;
            tb.setOnClickListener(vv -> select(bIdx));
            buttons.add(tb);
        }
        select(Math.max(selected, 0), true);
    }

    public void select(final int idx) {
        select(idx, false);
    }

    private void select(final int idx, final boolean force) {

        if (force || idx != selected) {
            int i = 0;
            for (ToggleButton tb : buttons) {
                tb.setChecked(i == idx);
                i++;
            }
            selected = idx < 0 || idx >= buttons.size() ? -1 : idx;

            if (this.changeListener != null) {
                this.changeListener.call(this, selected);
            }
        }
    }

    public int getSelected() {
        return selected;
    }

    private ToggleButton createAddButton(final String text) {
        final ToggleButton button = new ToggleButton(getContext(), null, R.style.button_small);
        button.setText(text);
        button.setPadding(dpToPixel(10), dpToPixel(10), dpToPixel(10), dpToPixel(10));
        final LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        this.addView(button, clp);
        return button;
    }

}
