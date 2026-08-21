package cgeo.geocaching.ui.dialog;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.Keyboard;
import cgeo.geocaching.databinding.InputWithParentDialogBinding;
import cgeo.geocaching.ui.ImageParam;
import cgeo.geocaching.ui.TextParam;
import cgeo.geocaching.ui.ViewUtils;
import cgeo.geocaching.utils.EmojiUtils;
import cgeo.geocaching.utils.LocalizationUtils;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import org.apache.commons.lang3.StringUtils;

/** Dialog for creating or renaming a named item with optional parent group and icon. */
public class InputWithParentDialog {

    /** Options for prefilling the dialog fields. */
    public static class Options {
        String initialName = null;
        String initialParent = null;
        boolean renameMode = false;
        String initialMarker = null;
        @DrawableRes int defaultIconRes = R.drawable.ic_menu_list;
        String label = null;
        String hint = null;
        String suffix = null;

        /** Prefill the name field. */
        public Options setInitialName(final String name) {
            this.initialName = name;
            return this;
        }

        /**
         * Prefill the parent dropdown and activate rename-mode: OK is only enabled when name or parent changed.
         * Pass "" for a top-level item (no parent); pass null to skip rename-mode.
         */
        public Options setInitialParent(final String parent) {
            this.initialParent = parent;
            this.renameMode = true;
            return this;
        }

        /**
         * Prefill the parent dropdown without activating rename-mode.
         * OK is enabled as long as the name field is non-empty.
         */
        public Options setInitialParentPrefill(final String parent) {
            this.initialParent = parent;
            return this;
        }

        /** Prefill the icon/emoji marker; null or empty means no marker. */
        public Options setInitialMarker(@Nullable final String marker) {
            this.initialMarker = marker;
            return this;
        }

        /** Icon shown in the marker button when no emoji is selected. */
        public Options setDefaultIconRes(@DrawableRes final int defaultIconRes) {
            this.defaultIconRes = defaultIconRes;
            return this;
        }

        /** Hint label shown in the name TextInputLayout. */
        public Options setLabel(final String label) {
            this.label = label;
            return this;
        }

        /** Suffix text shown at the end of the name field. */
        public Options setSuffix(final String suffix) {
            this.suffix = suffix;
            return this;
        }
    }

    private final Context context;
    private TextParam title;
    private TextParam positiveButton;

    private InputWithParentDialog(final Context context) {
        this.context = context;
    }

    public static InputWithParentDialog of(final Activity activity) {
        return new InputWithParentDialog(activity);
    }

    public static InputWithParentDialog of(final Context context) {
        return new InputWithParentDialog(context);
    }

    public InputWithParentDialog setTitle(final TextParam title) {
        this.title = title;
        return this;
    }

    public InputWithParentDialog setTitle(@StringRes final int resId, final Object... params) {
        return setTitle(TextParam.id(resId, params));
    }

    public InputWithParentDialog setPositiveButton(final TextParam text) {
        this.positiveButton = text;
        return this;
    }

    public InputWithParentDialog setPositiveButton(@StringRes final int resId) {
        return setPositiveButton(TextParam.id(resId));
    }

    /**
     * Shows the dialog.
     *
     * @param options       field prefill options
     * @param parentChoices available parent group names; a "(none)" entry is prepended automatically
     * @param okayListener  called on confirmation with the composed full name and the selected marker
     */
    public void show(@NonNull final Options options, @NonNull final List<String> parentChoices,
            @Nullable final BiConsumer<String, String> okayListener) {
        final InputWithParentDialogBinding viewBinding = InputWithParentDialogBinding.inflate(LayoutInflater.from(context));

        // icon/emoji marker button
        final MaterialButton markerButton = viewBinding.inputMarkerButton;
        final String[] markerHolder = {options.initialMarker};
        final ColorStateList defaultIconTint = markerButton.getIconTint();
        final Runnable applyMarker = () -> {
            if (StringUtils.isNotBlank(markerHolder[0])) {
                markerButton.setIcon(ImageParam.emoji(markerHolder[0]).getAsDrawable(context));
                markerButton.setIconTint(null);
            } else {
                markerButton.setIconResource(options.defaultIconRes);
                markerButton.setIconTint(defaultIconTint);
            }
        };
        applyMarker.run();
        markerButton.setOnClickListener(v -> EmojiUtils.selectEmojiPopup(context, markerHolder[0], false, null, newMarker -> {
            markerHolder[0] = newMarker;
            applyMarker.run();
        }));

        // parent dropdown
        final String noneLabel = LocalizationUtils.getString(R.string.init_custombnitem_none);
        final List<String> allChoices = new ArrayList<>();
        allChoices.add(noneLabel);
        allChoices.addAll(parentChoices);

        final AutoCompleteTextView parentSelect = viewBinding.inputParentView;
        parentSelect.setAdapter(new ArrayAdapter<>(context, android.R.layout.simple_dropdown_item_1line, allChoices));
        final String initialParentDisplay = options.initialParent == null || options.initialParent.isEmpty()
                ? noneLabel : options.initialParent;
        parentSelect.setText(initialParentDisplay, false);

        final TextInputEditText newParentView = viewBinding.inputNewParentView;
        final View newParentRow = viewBinding.inputNewParentRow;

        // When there are no existing parent groups to choose from, go straight to the new-parent input
        // and hide the select-row and the back-to-select button (nothing to go back to).
        if (parentChoices.isEmpty()) {
            viewBinding.inputSelectParentRow.setVisibility(View.GONE);
            viewBinding.inputNewParentRow.setVisibility(View.VISIBLE);
            viewBinding.inputNewParentListButton.setVisibility(View.GONE);
        }

        viewBinding.inputParentAddButton.setOnClickListener(v -> {
            viewBinding.inputSelectParentRow.setVisibility(View.GONE);
            viewBinding.inputNewParentRow.setVisibility(View.VISIBLE);
            newParentView.requestFocus();
            Keyboard.show(context, newParentView);
        });
        viewBinding.inputNewParentListButton.setOnClickListener(v -> {
            viewBinding.inputNewParentRow.setVisibility(View.GONE);
            viewBinding.inputSelectParentRow.setVisibility(View.VISIBLE);
        });

        // name field
        final TextInputEditText nameView = viewBinding.inputNameView;
        final TextInputLayout nameLayout = viewBinding.inputNameLayout;
        if (options.initialName != null) {
            nameView.setText(options.initialName);
        }
        if (options.label != null) {
            nameLayout.setHint(options.label);
        }
        if (options.hint != null) {
            nameView.setHint(options.hint);
        }
        if (options.suffix != null) {
            nameLayout.setSuffixText(options.suffix);
        }

        // build dialog — pass null listener to prevent auto-dismiss
        final TextParam positiveLabel = positiveButton != null ? positiveButton : TextParam.id(android.R.string.ok);
        final AlertDialog.Builder builder = Dialogs.newBuilder(context)
                .setPositiveButton(positiveLabel.getText(context), null)
                .setNegativeButton(android.R.string.cancel, (d, which) -> d.dismiss())
                .setView(viewBinding.getRoot());
        if (title != null) {
            builder.setTitle(title.getText(context));
        }
        final AlertDialog dialog = builder.create();

        final boolean isRenameMode = options.renameMode;
        final String originalName = options.initialName != null ? options.initialName.trim() : null;

        final Runnable updateOk = () -> {
            final android.widget.Button btn = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
            if (btn == null) {
                return;
            }
            final String currentName = ViewUtils.getEditableText(nameView.getText()).trim();
            final String effectiveParent = getEffectiveParent(parentSelect, newParentView, newParentRow, noneLabel);
            final boolean enabled;
            if (isRenameMode) {
                final boolean currentParentIsNone = effectiveParent.isEmpty();
                final boolean originalParentIsNone = options.initialParent.isEmpty();
                final boolean parentUnchanged = currentParentIsNone == originalParentIsNone
                        && (currentParentIsNone || effectiveParent.equals(options.initialParent));
                final boolean nameUnchanged = originalName != null && currentName.equals(originalName);
                enabled = !currentName.isEmpty() && !(nameUnchanged && parentUnchanged);
            } else {
                enabled = !currentName.isEmpty();
            }
            btn.setEnabled(enabled);
        };

        nameView.addTextChangedListener(ViewUtils.createSimpleWatcher(s -> updateOk.run()));
        parentSelect.addTextChangedListener(ViewUtils.createSimpleWatcher(s -> updateOk.run()));
        newParentView.addTextChangedListener(ViewUtils.createSimpleWatcher(s -> updateOk.run()));

        dialog.show();

        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(v -> {
            final String name = ViewUtils.getEditableText(nameView.getText()).trim();
            if (name.isEmpty()) {
                return;
            }
            final String effectiveParent = getEffectiveParent(parentSelect, newParentView, newParentRow, noneLabel);
            final String fullName = effectiveParent.isEmpty() ? name : effectiveParent + ":" + name;
            if (okayListener != null) {
                okayListener.accept(fullName, markerHolder[0]);
            }
            dialog.dismiss();
        });

        Keyboard.show(context, nameView);
        updateOk.run();
        Dialogs.moveCursorToEnd(nameView);
    }

    private static String getEffectiveParent(final AutoCompleteTextView parentView, final TextInputEditText newParentView,
            final View newParentRow, final String noneLabel) {
        if (newParentRow.getVisibility() == View.VISIBLE) {
            return ViewUtils.getEditableText(newParentView.getText()).trim();
        }
        final String selected = parentView.getText().toString().trim();
        return selected.isEmpty() || selected.equals(noneLabel) ? "" : selected;
    }

    /**
     * Composes a hierarchical name from an optional parent prefix and a name, using ":" as separator.
     * Cleans extra separators and surrounding whitespace. Examples:
     * ("Parent", "Name") → "Parent:Name"; ("", "Name") → "Name"; ("Parent:", "Name") → "Parent:Name".
     */
    public static String composeWithParent(final String parent, final String name) {
        final String cleanParent = cleanNameSegments(parent);
        final String cleanName = cleanNameSegments(name);
        return cleanName.isEmpty() ? cleanParent : cleanParent.isEmpty() ? cleanName : cleanParent + ":" + cleanName;
    }

    private static String cleanNameSegments(final String input) {
        if (input == null) {
            return "";
        }
        return Arrays.stream(input.split(":", -1))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.joining(":"));
    }
}
