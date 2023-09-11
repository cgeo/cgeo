package cgeo.geocaching.ui;

import cgeo.geocaching.R;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import java.util.ArrayList;
import java.util.List;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;

public class ButtonToggleGroup extends MaterialButtonToggleGroup {

    private boolean useRelativeWidth = false;
    private List<Integer> minWidths = null;

    public ButtonToggleGroup(final Context context) {
        super(ViewUtils.wrap(context));
        init();
    }

    public ButtonToggleGroup(final Context context, @Nullable final AttributeSet attrs) {
        super(ViewUtils.wrap(context), attrs);
        init();
    }

    public ButtonToggleGroup(final Context context, @Nullable final AttributeSet attrs, final int defStyleAttr) {
        super(ViewUtils.wrap(context), attrs, defStyleAttr);
        init();
    }

    private void init() {
        this.setOrientation(HORIZONTAL);
        this.setSingleSelection(true);
        this.setSelectionRequired(true);
    }

    /**
     * select whether buttons should be layouted relative (using LinearLayout.width parameter) or absolute (using "WRAP_CONTENT" for width)
     */
    public void setUseRelativeWidth(final boolean useRelativeWidth) {
        this.useRelativeWidth = useRelativeWidth;
        relayout();
    }

    /**
     * adds text buttons to this buttontogglegroup
     */
    public @IdRes
    int[] addButtons(@StringRes final int... textIds) {
        final LayoutInflater inflater = LayoutInflater.from(getContext());
        final int[] result = new int[textIds.length];

        for (int idx = 0; idx < textIds.length; idx++) {
            final Button b = (Button) inflater.inflate(R.layout.button_view, this, false);
            final int id = View.generateViewId();
            b.setId(id);
            b.setText(textIds[idx]);
            result[idx] = id;
            addToView(b, 0, 0);
        }

        if (this.isSelectionRequired() && this.getCheckedButtonIds().isEmpty()) {
            //if nothing is selected but selection is required, then select the first item which was just added
            ((MaterialButton) this.getChildAt(this.getChildCount() - textIds.length)).setChecked(true);
        }

        return result;
    }

    public void setCheckedButtonByIndex(final int idx, final boolean checked) {
        if (idx >= 0 && idx < this.getChildCount()) {
            ((MaterialButton) getChildAt(idx)).setChecked(checked);
        }
    }

    /**
     * Like {@link MaterialButtonToggleGroup#getCheckedButtonId()}, but returns the INDEX of the view instead of it's id
     */
    public int getCheckedButtonIndex() {
        final List<Integer> checkedButtonIndexes = getCheckedButtonIndexes();
        return checkedButtonIndexes.size() == 1 ? checkedButtonIndexes.get(0) : View.NO_ID;
    }

    /**
     * Like {@link MaterialButtonToggleGroup#getCheckedButtonIds()}, but returns the INDEXEX of the checked views instead of their ids
     */
    @NonNull
    public List<Integer> getCheckedButtonIndexes() {
        final List<Integer> checkedButtonIndexes = new ArrayList<>();
        for (int i = 0; i < getChildCount(); i++) {
            final MaterialButton child = (MaterialButton) getChildAt(i);
            if (child.isChecked()) {
                checkedButtonIndexes.add(i);
            }
        }

        return checkedButtonIndexes;
    }

    /**
     * Aligns the widths of the buttons of given ButtonToggleGroups so they match
     */
    public static void alignWidths(final ButtonToggleGroup... groups) {
        //get max widths
        final List<Integer> maxWidths = new ArrayList<>();
        int pos = 0;
        while (true) {
            int foundCnt = 0;
            int maxWidth = 0;
            for (ButtonToggleGroup group : groups) {
                if (pos < group.getChildCount()) {
                    final Button child = (Button) group.getChildAt(pos);
                    child.measure(0, 0);
                    maxWidth = Math.max(maxWidth, child.getMeasuredWidth());
                    foundCnt++;
                }
            }
            if (foundCnt < 1) {
                break;
            }
            maxWidths.add(maxWidth);
            pos++;
        }

        //now apply found maxwidths to each element
        for (ButtonToggleGroup group : groups) {
            group.setMinWidths(maxWidths);
        }

    }


    private void setMinWidths(final List<Integer> minWidths) {
        this.minWidths = minWidths;
        relayout();
    }

    private void addToView(final Button b, final int minWidth, final int totalMinWidth) {
        final LinearLayout.LayoutParams lp;
        if (this.useRelativeWidth) {
            lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, totalMinWidth == 0 ? 1 : ((float) minWidth) / totalMinWidth);
        } else {
            lp = new LinearLayout.LayoutParams(totalMinWidth == 0 ? ViewGroup.LayoutParams.WRAP_CONTENT : minWidth,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        addView(b, lp);
    }

    private void relayout() {
        final List<Button> children = new ArrayList<>();
        for (int idx = 0; idx < this.getChildCount(); idx++) {
            children.add((Button) this.getChildAt(idx));
        }
        int totalWidth = 0;
        if (minWidths != null) {
            for (int w : minWidths) {
                totalWidth += w;
            }
        }
        this.removeAllViews();
        int pos = 0;
        for (Button child : children) {
            addToView(child, minWidths != null && pos < minWidths.size() ? minWidths.get(pos) : 0, totalWidth);
            pos++;
        }
    }

}
