package cgeo.geocaching.ui;

import cgeo.geocaching.R;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

/**
 * A group of text chips with multiselect allowed (or single choice, see {@link #setSingleChoice(boolean)})
 */
public class ChipChoiceGroup extends ChipGroup {

    private boolean withSelectAllChip = true;
    private boolean singleChoice = false;

    private final List<CompoundButton> valueButtons = new ArrayList<>();
    private CompoundButton selectAllNoneButton;
    @Nullable
    private Runnable onSelectionChangedListener;
    /** checked value chips before the latest user interaction, to notify only real changes */
    @NonNull
    private Set<Integer> previousSelection = new HashSet<>();


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
        selectAllNoneButton = createChip(LayoutInflater.from(getContext()), TextParam.id(R.string.multiselect_selectall));
        selectAllNoneButton.setId(View.generateViewId());
        selectAllNoneButton.setOnClickListener(v -> {
            if (!singleChoice) {
                final boolean c = selectAllNoneButton.isChecked();
                for (CompoundButton tb : valueButtons) {
                    tb.setChecked(c);
                }
            }
            notifySelectionChangedIfChanged();
        });
    }

    /**
     * switches to single choice: exactly one chip is checked, the selectAll chip stands for "no restriction"
     * and is checked if no value chip is checked. Checked chips show no check icon (default: multiselect)
     */
    public void setSingleChoice(final boolean singleChoice) {
        this.singleChoice = singleChoice;
        setSingleSelection(singleChoice);
        setSelectionRequired(singleChoice);
        setCheckedIconVisible(selectAllNoneButton);
        for (CompoundButton tb : valueButtons) {
            setCheckedIconVisible(tb);
        }
        relayout();
    }

    public void setSelectAllText(final TextParam text) {
        text.applyTo(selectAllNoneButton);
    }

    public void setWithSelectAllChip(final boolean withSelectAllChip) {
        this.withSelectAllChip = withSelectAllChip;
        relayout();
    }

    /**
     * checks/unchecks the selectAll chip together with all value chips
     * (single choice: checking the selectAll chip unchecks all value chips)
     */
    public void setSelectAllChecked(final boolean checked) {
        selectAllNoneButton.setChecked(checked);
        if (!singleChoice) {
            for (CompoundButton tb : valueButtons) {
                tb.setChecked(checked);
            }
        }
        rememberSelection();
    }

    /**
     * sets a listener which is called whenever the user changes the selection
     * (not called for programmatic changes or for clicks which don't change the selection)
     */
    public void setOnSelectionChangedListener(@Nullable final Runnable onSelectionChangedListener) {
        this.onSelectionChangedListener = onSelectionChangedListener;
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
        rememberSelection();
    }

    /**
     * shows or hides value chips. Hidden chips are ignored when checking for all/none selected
     * (single choice: if the checked chip gets hidden, the selectAll chip gets checked)
     */
    public void setButtonVisibleByIndex(final boolean visible, final int... indexes) {
        for (int idx : indexes) {
            if (idx >= 0 && idx < this.valueButtons.size()) {
                this.valueButtons.get(idx).setVisibility(visible ? View.VISIBLE : View.GONE);
            }
        }
        checkAndSetAllNone();
        rememberSelection();
    }

    /**
     * sets an icon in front of the text of a value chip, tinted like the text
     */
    public void setButtonIconByIndex(@DrawableRes final int iconId, final int index) {
        if (index >= 0 && index < this.valueButtons.size() && this.valueButtons.get(index) instanceof Chip) {
            final Chip chip = (Chip) this.valueButtons.get(index);
            chip.setChipIconResource(iconId);
            chip.setChipIconSize(ViewUtils.dpToPixelFloat(18));
            chip.setChipIconTintResource(R.color.chip_textcolor_selector);
            chip.setChipIconVisible(true);
        }
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
        chip.setOnClickListener(v -> {
            checkAndSetAllNone();
            notifySelectionChangedIfChanged();
        });
        setCheckedIconVisible(chip);
        return chip;
    }

    private void setCheckedIconVisible(final CompoundButton chip) {
        if (chip instanceof Chip) {
            ((Chip) chip).setCheckedIconVisible(!singleChoice);
        }
    }

    public boolean allChecked() {
        return allNoneChecked(true);
    }

    public boolean noneChecked() {
        return allNoneChecked(false);
    }

    private boolean allNoneChecked(final boolean checkAll) {
        for (CompoundButton valueButton : this.valueButtons) {
            if (valueButton.getVisibility() == View.VISIBLE && checkAll != valueButton.isChecked()) {
                return false;
            }
        }
        return true;
    }

    private void checkAndSetAllNone() {
        if (!singleChoice) {
            selectAllNoneButton.setChecked(allChecked());
        } else if (noneChecked()) {
            // the group unchecks a hidden value chip
            selectAllNoneButton.setChecked(true);
        }
    }

    private void rememberSelection() {
        previousSelection = getCheckedButtonIndexes();
    }

    /**
     * notifies the listener only if the user really changed the selection
     * (single choice: a click on the checked chip keeps it checked)
     */
    private void notifySelectionChangedIfChanged() {
        final Set<Integer> selection = getCheckedButtonIndexes();
        if (selection.equals(previousSelection)) {
            return;
        }
        previousSelection = selection;
        if (onSelectionChangedListener != null) {
            onSelectionChangedListener.run();
        }
    }

    private void addChipToView(final CompoundButton chip) {
        final ChipGroup.LayoutParams clp = new ChipGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        this.addView(chip, clp);
    }

    private void relayout() {

        this.removeAllViews();
        if (this.withSelectAllChip && (this.singleChoice || this.valueButtons.size() > 1)) {
            addChipToView(this.selectAllNoneButton);
        }
        for (CompoundButton chip : this.valueButtons) {
            addChipToView(chip);
        }
        checkAndSetAllNone();
        rememberSelection();
    }

}
