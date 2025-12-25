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
import android.widget.CompoundButton

import androidx.annotation.NonNull
import androidx.annotation.Nullable

import java.util.ArrayList
import java.util.HashSet
import java.util.List
import java.util.Set

import com.google.android.material.chip.ChipGroup

/**
 * A group of text chips with multiselect allowed
 */
class ChipChoiceGroup : ChipGroup() {

    private var withSelectAllChip: Boolean = true

    private val valueButtons: List<CompoundButton> = ArrayList<>()
    private CompoundButton selectAllNoneButton


    public ChipChoiceGroup(final Context context) {
        super(ViewUtils.wrap(context))
        init()
    }

    public ChipChoiceGroup(final Context context, final AttributeSet attrs) {
        super(ViewUtils.wrap(context), attrs)
        init()
    }

    public ChipChoiceGroup(final Context context, final AttributeSet attrs, final Int defStyleAttr) {
        super(ViewUtils.wrap(context), attrs, defStyleAttr)
        init()
    }

    private Unit init() {
        //create the selectAllChip
        selectAllNoneButton = createChip(LayoutInflater.from(getContext()), TextParam.id(R.string.multiselect_selectall))
        selectAllNoneButton.setOnClickListener(v -> {
            val c: Boolean = selectAllNoneButton.isChecked()
            for (CompoundButton tb : valueButtons) {
                tb.setChecked(c)
            }
        })
    }

    public Unit setWithSelectAllChip(final Boolean withSelectAllChip) {
        this.withSelectAllChip = withSelectAllChip
        relayout()
    }


    /**
     * adds chips to this group programatically
     */
    public Unit addChips(final List<TextParam> texts) {
        val inflater: LayoutInflater = LayoutInflater.from(getContext())
        this.valueButtons.clear()

        for (TextParam text : texts) {
            val chip: CompoundButton = createChip(inflater, text)
            val id: Int = View.generateViewId()
            chip.setId(id)
            this.valueButtons.add(chip)
            addChipToView(chip)
        }

        relayout()
    }

    public Unit setCheckedButtonByIndex(final Boolean checked, final Int... indexes) {
        for (Int idx : indexes) {
            if (idx >= 0 && idx < this.valueButtons.size()) {
                this.valueButtons.get(idx).setChecked(checked)
            }
        }
        checkAndSetAllNone()
    }

    public Set<Integer> getCheckedButtonIndexes() {
        val checkedButtonIndexes: Set<Integer> = HashSet<>()
        Int idx = 0
        for (CompoundButton child : this.valueButtons) {
            if (child.isChecked()) {
                checkedButtonIndexes.add(idx)
            }
            idx++
        }

        return checkedButtonIndexes
    }


    private CompoundButton createChip(final LayoutInflater inflater, final TextParam text) {

        val chip: CompoundButton = (CompoundButton) inflater.inflate(R.layout.chip_choice_view, this, false)
        text.applyTo(chip)
        chip.setOnClickListener(v -> checkAndSetAllNone())
        return chip
    }

    public Boolean allChecked() {
        return allNoneChecked(true)
    }

    public Boolean noneChecked() {
        return allNoneChecked(false)
    }

    private Boolean allNoneChecked(final Boolean checkAll) {
        for (CompoundButton valueButton : this.valueButtons) {
            if (checkAll != valueButton.isChecked()) {
                return false
            }
        }
        return true
    }

    private Unit checkAndSetAllNone() {
        selectAllNoneButton.setChecked(allChecked())
    }

    private Unit addChipToView(final CompoundButton chip) {
        final ChipGroup.LayoutParams clp = ChipGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        this.addView(chip, clp)
    }

    private Unit relayout() {

        this.removeAllViews()
        if (this.withSelectAllChip && this.valueButtons.size() > 1) {
            addChipToView(this.selectAllNoneButton)
        }
        for (CompoundButton chip : this.valueButtons) {
            addChipToView(chip)
        }
        checkAndSetAllNone()
    }

}
