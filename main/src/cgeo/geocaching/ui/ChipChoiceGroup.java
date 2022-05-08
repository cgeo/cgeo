package cgeo.geocaching.ui;

import cgeo.geocaching.R;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.android.material.chip.ChipGroup;

/**
 * A group of text chips with multiselect allowed
 */
public class ChipChoiceGroup extends ChipGroup {

    private boolean withSelectAllChip = true;

    private final List<CompoundButton> valueButtons = new ArrayList<>();
    private CompoundButton selectAllNoneButton;


    public ChipChoiceGroup(final Context context) {
        super(ViewUtils.wrap(context));
        init();
    }

    public ChipChoiceGroup(final Context context, @Nullable final AttributeSet attrs) {
        super(ViewUtils.wrap(context), attrs);
        init();
    }

    public ChipChoiceGroup(final Context context, @Nullable final AttributeSet attrs, final int defStyleAttr) {
        super(ViewUtils.wrap(context), attrs, defStyleAttr);
        init();
    }

    private void init() {
        //create the selectAllChip
        selectAllNoneButton = createChip(LayoutInflater.from(getContext()), TextParam.id(R.string.chipchoicegroup_selectall));
        selectAllNoneButton.setOnClickListener(v -> {
            final boolean c = selectAllNoneButton.isChecked();
            for (CompoundButton tb : valueButtons) {
                tb.setChecked(c);
            }
        });
    }

    public void setWithSelectAllChip(final boolean withSelectAllChip) {
        this.withSelectAllChip = withSelectAllChip;
        relayout();
    }


    /**
     * adds chips to this group programatically
     */
    public void addChips(final List<TextParam> texts) {
        final LayoutInflater inflater = LayoutInflater.from(getContext());
        this.valueButtons.clear();

        for (TextParam text : texts) {
            final CompoundButton chip = createChip(inflater, text);
            final int id = View.generateViewId();
            chip.setId(id);
            this.valueButtons.add(chip);
            addChipToView(chip);
        }

        relayout();
    }

    public void setCheckedButtonByIndex(final boolean checked, final int... indexes) {
        for (int idx : indexes) {
            if (idx >= 0 && idx < this.valueButtons.size()) {
                this.valueButtons.get(idx).setChecked(checked);
            }
        }
        checkAndSetAllNone();
    }

    @NonNull
    public Set<Integer> getCheckedButtonIndexes() {
        final Set<Integer> checkedButtonIndexes = new HashSet<>();
        int idx = 0;
        for (CompoundButton child : this.valueButtons) {
            if (child.isChecked()) {
                checkedButtonIndexes.add(idx);
            }
            idx++;
        }

        return checkedButtonIndexes;
    }


    private CompoundButton createChip(final LayoutInflater inflater, final TextParam text) {

        final CompoundButton chip = (CompoundButton) inflater.inflate(R.layout.chip_choice_view, this, false);
        text.applyTo(chip);
        chip.setOnClickListener(v -> checkAndSetAllNone());
        return chip;
    }

    public boolean allChecked() {
        return allNoneChecked(true);
    }

    public boolean noneChecked() {
        return allNoneChecked(false);
    }

    private boolean allNoneChecked(final boolean checkAll) {
        for (CompoundButton valueButton : this.valueButtons) {
            if (checkAll != valueButton.isChecked()) {
                return false;
            }
        }
        return true;
    }

    private void checkAndSetAllNone() {
        selectAllNoneButton.setChecked(allChecked());
    }

    private void addChipToView(final CompoundButton chip) {
        final ChipGroup.LayoutParams clp = new ChipGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        this.addView(chip, clp);
    }

    private void relayout() {

        this.removeAllViews();
        if (this.withSelectAllChip && this.valueButtons.size() > 1) {
            addChipToView(this.selectAllNoneButton);
        }
        for (CompoundButton chip : this.valueButtons) {
            addChipToView(chip);
        }
        checkAndSetAllNone();
    }

}
