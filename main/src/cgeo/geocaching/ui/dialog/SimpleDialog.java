package cgeo.geocaching.ui.dialog;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.Keyboard;
import cgeo.geocaching.ui.TextParam;
import cgeo.geocaching.ui.ViewUtils;
import cgeo.geocaching.utils.TextUtils;
import cgeo.geocaching.utils.functions.Action2;
import cgeo.geocaching.utils.functions.Func1;
import cgeo.geocaching.utils.functions.Func2;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Pair;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.core.util.Consumer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import static java.lang.Boolean.TRUE;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

/**
 * Builder-like class for simple dialogs based on {@link AlertDialog}.
 * <p>
 * If you want to show e.g. a simple text or confirmation dialog, use this class.
 */
public class SimpleDialog {

    /**
     * Convenience constant for button actions to do nothing (and close the dialog)
     */
    public static final DialogInterface.OnClickListener DO_NOTHING = (d, i) -> {
    };

    /**
     * Define common button text sets
     */
    public enum ButtonTextSet {
        OK_CANCEL(TextParam.id(android.R.string.ok), TextParam.id(android.R.string.cancel), null),
        YES_NO(TextParam.id(R.string.yes), TextParam.id(R.string.no), null);

        public final TextParam positive;
        public final TextParam negative;
        public final TextParam neutral;

        ButtonTextSet(final TextParam positive, final TextParam negative, final TextParam neutral) {
            this.positive = positive;
            this.negative = negative;
            this.neutral = neutral;
        }
    }

    private final Context context;

    private TextParam title;
    private TextParam message;

    private TextParam positiveButton = TextParam.id(android.R.string.ok);
    private TextParam negativeButton = TextParam.id(android.R.string.cancel);
    private TextParam neutralButton = TextParam.id(android.R.string.untitled);
    private boolean neutralButtonNeedsSelection = true;

    public static SimpleDialog of(final Activity activity) {
        return ofContext(activity);
    }

    public static SimpleDialog ofContext(final Context context) {
        return new SimpleDialog(context);
    }

    public Context getContext() {
        return this.context;
    }

    public SimpleDialog setTitle(final TextParam title) {
        this.title = title;
        return this;
    }

    public SimpleDialog setTitle(@StringRes final int stringId, final Object... params) {
        return setTitle(TextParam.id(stringId, params));
    }


    public SimpleDialog setMessage(final TextParam message) {
        this.message = message;
        return this;
    }

    public SimpleDialog setMessage(@StringRes final int stringId, final Object... params) {
        return setMessage(TextParam.id(stringId, params));
    }

    /**
     * Set the button set to use
     */
    public SimpleDialog setButtons(final ButtonTextSet set) {
        if (set != null) {
            setButtons(set.positive, set.negative, set.neutral);
        }

        return this;
    }

    public CharSequence getPositiveButton() {
        return positiveButton.getText(getContext());
    }

    public CharSequence getNegativeButton() {
        return negativeButton.getText(getContext());
    }

    public CharSequence getNeutralButton() {
        return neutralButton.getText(getContext());
    }

    /**
     * Up to three parameters will be processed. First is positive button, second is negative button, third is neutral button
     */
    public SimpleDialog setButtons(@StringRes final int ... buttonIds) {
        final TextParam[] buttonTps = new TextParam[buttonIds.length];
        for (int idx = 0; idx < buttonIds.length; idx++) {
            buttonTps[idx] = buttonIds[idx] == 0 ? null : TextParam.id(buttonIds[idx]);
        }
        return setButtons(buttonTps);
    }

    public SimpleDialog setSelectionForNeutral(final boolean neutralButtonNeedsSelection) {
        this.neutralButtonNeedsSelection = neutralButtonNeedsSelection;
        return this;
    }

    /**
     * Up to three parameters will be processed. First is positive button, second is negative button, third is neutral button
     */
    public SimpleDialog setButtons(final TextParam... buttons) {

        if (buttons != null) {
            if (buttons.length >= 1) {
                setPositiveButton(buttons[0]);
            }
            if (buttons.length >= 2) {
                setNegativeButton(buttons[1]);
            }
            if (buttons.length >= 3) {
                setNeutralButton(buttons[2]);
            }
        }
        return this;
    }

    public SimpleDialog setPositiveButton(final TextParam positiveButtonText) {
        if (positiveButtonText != null) {
            this.positiveButton = positiveButtonText;
        }
        return this;
    }

    public SimpleDialog setNegativeButton(final TextParam negativeButtonText) {
        if (negativeButtonText != null) {
            this.negativeButton = negativeButtonText;
        }
        return this;
    }

    public SimpleDialog setNeutralButton(final TextParam neutralButtonText) {
        if (neutralButtonText != null) {
            this.neutralButton = neutralButtonText;
        }
        return this;
    }

    private SimpleDialog(final Context context) {
        this.context = context;
    }


    private void applyCommons(final AlertDialog.Builder builder) {

        if (this.title != null) {
            builder.setTitle(this.title.getText(getContext()));
        }
        if (this.message != null) {
            builder.setMessage(this.message.getText(getContext()));
        }
    }

    /**
     * adjusts common dialog settings for the created dialog (e.g. title and message). Call this method after calling dialog.show()!
     */
    public void adjustCommons(final AlertDialog dialog) {
        if (this.title != null) {
            this.title.applyTo(dialog.findViewById(android.R.id.title));
        }
        if (this.message != null) {
            this.message.applyTo(dialog.findViewById(android.R.id.message));
        }
    }

    /**
     * Use this method to create and display a 'confirmation' dialog using the data defined before using this classes' setters
     * <p>
     * A confirm dialog always has at least a positive action and shows at least a positive and a negative button.
     * Provide up to three listeners to define actions for 'positive', 'negative' and 'neutral' button.
     */
    public void confirm(final DialogInterface.OnClickListener positiveListener, final DialogInterface.OnClickListener... otherListeners) {
        final DialogInterface.OnClickListener[] allListeners = ArrayUtils.addFirst(otherListeners, positiveListener);

        if (allListeners.length >= 2) {
            show(allListeners);
        } else {
            //"confirm" always needs at least "ok" and "cancel" button
            final DialogInterface.OnClickListener[] newListener = new DialogInterface.OnClickListener[2];
            newListener[0] = allListeners.length == 0 ? DO_NOTHING : allListeners[0];
            newListener[1] = DO_NOTHING;
            show(newListener);
        }
    }

    /**
     * Use this method to create and display a simple 'show message' dialog using the data defined before using this classes' setters
     * <p>
     * A show dialog just shows a message, only one positive button is mandatory (but does not need to have an action)
     * Provide up to three listeners to define actions for 'positive', 'negative' and 'neutral' button.
     */
    public void show(final DialogInterface.OnClickListener... listener) {
        final AlertDialog.Builder builder = Dialogs.newBuilder(getContext());
        applyCommons(builder);
        builder.setCancelable(true);
        if (listener == null || listener.length == 0) {
            builder.setPositiveButton(getPositiveButton(), DO_NOTHING);
        } else {
            builder.setPositiveButton(getPositiveButton(), listener[0]);
            if (listener.length > 1 && listener[1] != null) {
                builder.setNegativeButton(getNegativeButton(), listener[1]);
            }
            if (listener.length > 2 && listener[2] != null) {
                builder.setNeutralButton(getNeutralButton(), listener[2]);
            }
        }
        final AlertDialog dialog = builder.create();
        if (getContext() instanceof Activity) {
            dialog.setOwnerActivity((Activity) getContext());
        }
        dialog.show();
        adjustCommons(dialog);
    }

    /**
     * Use this method to create and display a 'select single' dialog using the data defined before using this classes' setters
     * <p>
     * A 'select single' dialog allows the user to select exactly one value out of a given list of choices
     *
     * @param items            the list of items to select from
     * @param displayMapper    mapper to get the display value for each of the items
     * @param preselect        index of the item to preselect. If this is not a valid index (e.g. -1), no value will be preselected
     * @param showChoice       if true, then items are shown with radio buttons to see the chosen value, and buttons must be used to end dialog.
     *                         If false, then no radio buttons and dialog buttons are displayed and clicking an item ends the dialog
     * @param onSelectListener is called when user made a selection (if showChoice=true then after clicking positive button; otherwise after clicking an item)
     * @param moreListeners    Provide up to two more listeners to define actions for 'negative' and 'neutral' button.
     */
    @SafeVarargs
    public final <T> void selectSingle(@NonNull final List<T> items, @NonNull final Func2<T, Integer, TextParam> displayMapper, final int preselect, final boolean showChoice, final Action2<T, Integer> onSelectListener, final Action2<T, Integer>... moreListeners) {
        selectSingleGrouped(items, displayMapper, preselect, showChoice, null, null, onSelectListener, moreListeners);
    }

    /**
     * Use this method to create and display a 'select single' dialog using the data defined before using this classes' setters
     * <p>
     * A 'select single' dialog allows the user to select exactly one value out of a given list of choices
     *
     * @param items            the list of items to select from
     * @param displayMapper    mapper to get the display value for each of the items
     * @param preselect        index of the item to preselect. If this is not a valid index (e.g. -1), no value will be preselected
     * @param showChoice       if true, then items are shown with radio buttons to see the chosen value, and buttons must be used to end dialog.
     *                         If false, then no radio buttons and dialog buttons are displayed and clicking an item ends the dialog
     * @param groupMapper      if not null, will display grouped display
     * @param onSelectListener is called when user made a selection (if showChoice=true then after clicking positive button; otherwise after clicking an item)
     * @param moreListeners    Provide up to two more listeners to define actions for 'negative' and 'neutral' button.
     */
    @SafeVarargs
    @SuppressWarnings({"PMD.NPathComplexity"}) // splitting up that method would not help improve readability
    public final <T, G> void selectSingleGrouped(@NonNull final List<T> items, @NonNull final Func2<T, Integer, TextParam> displayMapper, final int preselect, final boolean showChoice, @Nullable final Func2<T, Integer, G> groupMapper, @Nullable final Func1<G, TextParam> groupDisplayMapper, final Action2<T, Integer> onSelectListener, final Action2<T, Integer>... moreListeners) {

        final AlertDialog.Builder builder = Dialogs.newBuilder(getContext());
        applyCommons(builder);
        builder.setCancelable(true);

        final int preselectPos = preselect < 0 || preselect >= items.size() ? -1 : preselect;

        final int[] selectedPos = {preselectPos};

        final Pair<List<TextParam>, Func1<Integer, Integer>> groupedValues = createGroupedDisplayValues(items, displayMapper, groupMapper, groupDisplayMapper);

        // Maybe select_dialog_singlechoice_material / select_dialog_item_material instead ?
        // NOT android.R.layout.select_dialog_item -> makes font size too big
        final ListAdapter adapter = createListAdapter(groupedValues.first, showChoice);

        //use "setsinglechoiceItems", because otherwise the dialog will close always after selecting an item
        builder.setSingleChoiceItems(adapter, preselectPos, (dialog, clickpos) -> {
            final Integer pos = groupedValues.second.call(clickpos);
            if (pos == null) {
                return;
            }

            enableDisableButtons((AlertDialog) dialog, true);
            if (!showChoice) {
                dialog.dismiss();
                onSelectListener.call(items.get(pos), pos);
            } else {
                selectedPos[0] = pos;
            }
        });

        if (showChoice) {
            builder.setPositiveButton(getPositiveButton(), (dialog, which) -> onSelectListener.call(items.get(selectedPos[0]), selectedPos[0]));
            if (moreListeners != null) {
                if (moreListeners.length > 0) {
                    builder.setNegativeButton(getNegativeButton(), (dialog, which) -> moreListeners[0].call(items.get(selectedPos[0]), selectedPos[0]));
                }
                if (moreListeners.length > 1) {
                    builder.setNeutralButton(getNeutralButton(), (dialog, which) -> moreListeners[1].call(items.get(selectedPos[0]), selectedPos[0]));
                }
            }
        }

        if (!neutralButtonNeedsSelection && moreListeners != null && moreListeners.length > 1) {
            builder.setNeutralButton(getNeutralButton(), (dialog, which) -> moreListeners[1].call(null, -1));
        }

        final AlertDialog dialog = builder.create();
        dialog.show();
        adjustCommons(dialog);
        if (preselect < 0) {
            enableDisableButtons(dialog, false);
        }
    }

    /**
     * Use this method to create and display a 'select multi' dialog using the data defined before using this classes' setters
     * <p>
     * A 'select multi' dialog allows the user to select multiple (or no) value out of a given list of choices
     *
     * @param items            the list of items to select from
     * @param displayMapper    mapper to get the display value for each of the items
     * @param preselect        mapper defining for each of the given items whether it is preselected or not
     * @param onSelectListener provide the select listener called when user made a selection (called when user clicks on positive button)
     */
    public <T> void selectMultiple(final List<T> items, final Func2<T, Integer, TextParam> displayMapper, final Func2<T, Integer, Boolean> preselect, final Consumer<Set<T>> onSelectListener) {
        final CharSequence[] itemTexts = new CharSequence[items.size()];
        final boolean[] itemSelects = new boolean[items.size()];
        final Set<T> result = new HashSet<>();
        int idx = 0;
        for (T item : items) {
            final TextParam tp = displayMapper.call(item, idx);
            itemTexts[idx] = tp == null ? String.valueOf(item) : tp.getText(getContext());
            itemSelects[idx] = preselect != null && TRUE.equals(preselect.call(item, idx));
            if (itemSelects[idx]) {
                result.add(item);
            }
            idx++;
        }

        final AlertDialog.Builder builder = Dialogs.newBuilder(getContext(), R.style.cgeo_compactDialogs);
        applyCommons(builder);

        builder.setMultiChoiceItems(itemTexts, itemSelects, (d, i, c) -> {
            if (c) {
                result.add(items.get(i));
            } else {
                result.remove(items.get(i));
            }

        });
        builder.setPositiveButton(getPositiveButton(), (d, w) -> onSelectListener.accept(result));
        builder.setNegativeButton(getNegativeButton(), (d, w) -> d.dismiss());

        final AlertDialog dialog = builder.create();
        dialog.show();
        adjustCommons(dialog);
    }

    /**
     * Use this method to create and display an 'input' dialog using the data defined before using this classes' setters
     * <p>
     * An 'input' dialog allows the user to enter a value. It always has a positive action (to end input) and a negative action (to abort)
     *
     * @param inputType    input type flag mask, use constants defined in class {@link InputType}. If a value below 0 is given then standard input type settings (text) are assumed
     * @param defaultValue if non-null, this will be the prefilled value of the input field
     * @param label        if non-null & non-empty, this will be displayed as a hint within the input field (e.g. to display a hint)
     * @param suffix       if non-null & non-empty, this will be displayed as a suffix at the end of the input field (e.g. to display units like km/ft)
     * @param okayListener provide the select listener called when user entered something and finishes it (called when user clicks on positive button)
     */

    public void input(final int inputType, @Nullable final String defaultValue, @Nullable final String label, @Nullable final String suffix, final Consumer<String> okayListener) {
        input(inputType, defaultValue, label, suffix, StringUtils::isNotBlank, null, okayListener);
    }

    /**
     * Use this method to create and display an 'input' dialog using the data defined before using this classes' setters
     * <p>
     * An 'input' dialog allows the user to enter a value. It always has a positive action (to end input) and a negative action (to abort)
     *
     * @param inputType    input type flag mask, use constants defined in class {@link InputType}. If a value below 0 is given then standard input type settings (text) are assumed
     * @param defaultValue if non-null, this will be the prefilled value of the input field
     * @param label        if non-null & non-empty, this will be displayed as a hint within the input field (e.g. to display a hint)
     * @param suffix       if non-null & non-empty, this will be displayed as a suffix at the end of the input field (e.g. to display units like km/ft)
     * @param inputChecker if non-null, then ok button will only be clickable if given check is satisfied
     * @param allowedChars if non-null, then only chars passing this regex pattern will be allowed to enter
     * @param okayListener provide the select listener called when user entered something and finishes it (called when user clicks on positive button)
     */
    public void input(final int inputType, @Nullable final String defaultValue, @Nullable final String label, @Nullable final String suffix, final Func1<String, Boolean> inputChecker, final String allowedChars, final Consumer<String> okayListener) {

        final Pair<View, EditText> textField = ViewUtils.createTextField(getContext(), defaultValue, TextParam.text(label), TextParam.text(suffix), inputType, 1, 1);

        final AlertDialog.Builder builder = Dialogs.newBuilder(getContext());
        applyCommons(builder);
        builder.setView(textField.first);
        // remove whitespaces added by autocompletion of Android keyboard before calling okayListener
        builder.setPositiveButton(getPositiveButton(), (dialog, which) -> okayListener.accept(textField.second.getText().toString().trim()));
        builder.setNegativeButton(getNegativeButton(), (dialog, whichButton) -> dialog.dismiss());
        final AlertDialog dialog = builder.create();

        if (inputChecker != null) {
            textField.second.addTextChangedListener(new TextWatcher() {

                @Override
                public void onTextChanged(final CharSequence s, final int start, final int before, final int count) {
                    // empty
                }

                @Override
                public void beforeTextChanged(final CharSequence s, final int start, final int count, final int after) {
                    // empty
                }

                @Override
                public void afterTextChanged(final Editable editable) {
                    enableDialogButtonIf(dialog, editable.toString(), inputChecker);
                }
            });
        }
        if (allowedChars != null) {
            final Pattern charPattern = Pattern.compile(allowedChars);
            textField.second.setFilters(new InputFilter[]{(source, start, end, dest, dstart, dend) -> {
                for (int i = start; i < end; i++) {
                    if (!charPattern.matcher("" + source.charAt(i)).matches()) {
                        return "";
                    }
                }
                return null;
            } });
        }
        // force keyboard
        Keyboard.show(getContext(), textField.second);

        // disable button
        dialog.show();
        enableDialogButtonIf(dialog, String.valueOf(defaultValue), inputChecker);
        Dialogs.moveCursorToEnd(textField.second);
        adjustCommons(dialog);
    }

    private static void enableDialogButtonIf(final AlertDialog dialog, final String input, final Func1<String, Boolean> inputChecker) {
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(inputChecker == null || inputChecker.call(input));
    }


    private void enableDisableButtons(final AlertDialog dialog, final boolean enable) {
        if (dialog.getButton(DialogInterface.BUTTON_POSITIVE) != null) {
            dialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(enable);
        }
        if (dialog.getButton(DialogInterface.BUTTON_NEGATIVE) != null) {
            dialog.getButton(DialogInterface.BUTTON_NEGATIVE).setEnabled(enable);
        }

        if (dialog.getButton(DialogInterface.BUTTON_NEUTRAL) != null) {
            if (!neutralButtonNeedsSelection) {
                dialog.getButton(DialogInterface.BUTTON_NEUTRAL).setEnabled(true);
            } else {
                dialog.getButton(DialogInterface.BUTTON_NEUTRAL).setEnabled(enable);
            }
        }
    }


    @NotNull
    private ListAdapter createListAdapter(@NotNull final List<TextParam> items, final boolean showChoice) {

        //final int itemLayout = showChoice ? android.R.layout.simple_list_item_single_choice : android.R.layout.simple_list_item_1;
        final int itemLayout = showChoice ? R.layout.select_dialog_singlechoice_material : R.layout.select_dialog_item_material;

        return new ArrayAdapter<TextParam>(
            getContext(),
            itemLayout,
            android.R.id.text1,
            items) {
            public View getView(final int position, final View convertView, final ViewGroup parent) {
                //Use super class to create the View.
                final View v = super.getView(position, convertView, parent);
                final TextView tv = v.findViewById(android.R.id.text1);
                items.get(position).applyTo(tv, true);
                return v;
            }
        };
    }

    /** creates a group-including and group-styled list of elements along with a mapping from visual list to value indexes */
    @SuppressWarnings({"PMD.NPathComplexity"}) // splitting up that method would not help improve readability
    private static <T, G> Pair<List<TextParam>, Func1<Integer, Integer>> createGroupedDisplayValues(final List<T> items, @NotNull final Func2<T, Integer, TextParam> displayMapper, @Nullable final Func2<T, Integer, G> groupMapper, @Nullable final Func1<G, TextParam> groupDisplayMapper) {

        final Map<G, List<Pair<Integer, TextParam>>> groupedMapList = new HashMap<>();
        final List<TextParam> singleList = new ArrayList<>();
        int pos = 0;
        for (T value : items) {
            final G group = groupMapper == null ? null : groupMapper.call(value, pos);
            List<Pair<Integer, TextParam>> groupList = groupedMapList.get(group);
            if (groupList == null) {
                groupList = new ArrayList<>();
                groupedMapList.put(group, groupList);
            }
            final TextParam valueTextParam = displayMapper.call(value, pos);
            groupList.add(new Pair<>(pos, valueTextParam));
            singleList.add(valueTextParam);
            pos++;
        }

        if (groupedMapList.size() <= 1) {
            //no items at all or only only group (the later is far more likely) -> don't use groups at all
            return new Pair<>(groupedMapList.isEmpty() ? Collections.emptyList() : singleList, idx -> idx);
        }

        //sort groups by their display name
        final List<G> groupList = new ArrayList<>(groupedMapList.keySet());
        TextUtils.sortListLocaleAware(groupList, g -> g == null || groupDisplayMapper == null ? "--" : groupDisplayMapper.call(g).toString());

        //construct result
        final List<TextParam> result = new ArrayList<>();
        final Map<Integer, Integer> indexMap = new HashMap<>();
        for (G group : groupList) {

            //group name
            result.add(group == null || groupDisplayMapper == null ? TextParam.text("--") : groupDisplayMapper.call(group));

            //group items
            for (Pair<Integer, TextParam> valuePair : Objects.requireNonNull(groupedMapList.get(group))) {
                indexMap.put(result.size(), valuePair.first);
                result.add(valuePair.second);
            }
        }

        return new Pair<>(result, indexMap::get);
    }


}
