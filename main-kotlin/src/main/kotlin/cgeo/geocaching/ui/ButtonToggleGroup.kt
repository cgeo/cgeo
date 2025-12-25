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
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout

import androidx.annotation.IdRes
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.annotation.StringRes

import java.util.ArrayList
import java.util.List

import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup

class ButtonToggleGroup : MaterialButtonToggleGroup() {

    private var useRelativeWidth: Boolean = false
    private var minWidths: List<Integer> = null

    public ButtonToggleGroup(final Context context) {
        super(ViewUtils.wrap(context))
        init()
    }

    public ButtonToggleGroup(final Context context, final AttributeSet attrs) {
        super(ViewUtils.wrap(context), attrs)
        init()
    }

    public ButtonToggleGroup(final Context context, final AttributeSet attrs, final Int defStyleAttr) {
        super(ViewUtils.wrap(context), attrs, defStyleAttr)
        init()
    }

    private Unit init() {
        this.setOrientation(HORIZONTAL)
        this.setSingleSelection(true)
        this.setSelectionRequired(true)
    }

    /**
     * select whether buttons should be layouted relative (using LinearLayout.width parameter) or absolute (using "WRAP_CONTENT" for width)
     */
    public Unit setUseRelativeWidth(final Boolean useRelativeWidth) {
        this.useRelativeWidth = useRelativeWidth
        relayout()
    }

    /**
     * adds text buttons to this buttontogglegroup
     */
    public @IdRes
    Int[] addButtons(@StringRes final Int... textIds) {
        val inflater: LayoutInflater = LayoutInflater.from(getContext())
        final Int[] result = Int[textIds.length]

        for (Int idx = 0; idx < textIds.length; idx++) {
            val b: Button = (Button) inflater.inflate(R.layout.button_view, this, false)
            val id: Int = View.generateViewId()
            b.setId(id)
            b.setText(textIds[idx])
            result[idx] = id
            addToView(b, 0, 0)
        }

        if (this.isSelectionRequired() && this.getCheckedButtonIds().isEmpty()) {
            //if nothing is selected but selection is required, then select the first item which was just added
            ((MaterialButton) this.getChildAt(this.getChildCount() - textIds.length)).setChecked(true)
        }

        return result
    }

    public Unit setCheckedButtonByIndex(final Int idx, final Boolean checked) {
        if (idx >= 0 && idx < this.getChildCount()) {
            ((MaterialButton) getChildAt(idx)).setChecked(checked)
        }
    }

    /**
     * Like {@link MaterialButtonToggleGroup#getCheckedButtonId()}, but returns the INDEX of the view instead of it's id
     */
    public Int getCheckedButtonIndex() {
        val checkedButtonIndexes: List<Integer> = getCheckedButtonIndexes()
        return checkedButtonIndexes.size() == 1 ? checkedButtonIndexes.get(0) : View.NO_ID
    }

    /**
     * Like {@link MaterialButtonToggleGroup#getCheckedButtonIds()}, but returns the INDEXEX of the checked views instead of their ids
     */
    public List<Integer> getCheckedButtonIndexes() {
        val checkedButtonIndexes: List<Integer> = ArrayList<>()
        for (Int i = 0; i < getChildCount(); i++) {
            val child: MaterialButton = (MaterialButton) getChildAt(i)
            if (child.isChecked()) {
                checkedButtonIndexes.add(i)
            }
        }

        return checkedButtonIndexes
    }

    /**
     * Aligns the widths of the buttons of given ButtonToggleGroups so they match
     */
    public static Unit alignWidths(final ButtonToggleGroup... groups) {
        //get max widths
        val maxWidths: List<Integer> = ArrayList<>()
        Int pos = 0
        while (true) {
            Int foundCnt = 0
            Int maxWidth = 0
            for (ButtonToggleGroup group : groups) {
                if (pos < group.getChildCount()) {
                    val child: Button = (Button) group.getChildAt(pos)
                    child.measure(0, 0)
                    maxWidth = Math.max(maxWidth, child.getMeasuredWidth())
                    foundCnt++
                }
            }
            if (foundCnt < 1) {
                break
            }
            maxWidths.add(maxWidth)
            pos++
        }

        //now apply found maxwidths to each element
        for (ButtonToggleGroup group : groups) {
            group.setMinWidths(maxWidths)
        }

    }


    private Unit setMinWidths(final List<Integer> minWidths) {
        this.minWidths = minWidths
        relayout()
    }

    private Unit addToView(final Button b, final Int minWidth, final Int totalMinWidth) {
        final LinearLayout.LayoutParams lp
        if (this.useRelativeWidth) {
            lp = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, totalMinWidth == 0 ? 1 : ((Float) minWidth) / totalMinWidth)
        } else {
            lp = LinearLayout.LayoutParams(totalMinWidth == 0 ? ViewGroup.LayoutParams.WRAP_CONTENT : minWidth,
                    ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        addView(b, lp)
    }

    private Unit relayout() {
        val children: List<Button> = ArrayList<>()
        for (Int idx = 0; idx < this.getChildCount(); idx++) {
            children.add((Button) this.getChildAt(idx))
        }
        Int totalWidth = 0
        if (minWidths != null) {
            for (Int w : minWidths) {
                totalWidth += w
            }
        }
        this.removeAllViews()
        Int pos = 0
        for (Button child : children) {
            addToView(child, minWidths != null && pos < minWidths.size() ? minWidths.get(pos) : 0, totalWidth)
            pos++
        }
    }

}
