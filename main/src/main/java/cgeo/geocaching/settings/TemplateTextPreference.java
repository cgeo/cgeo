package cgeo.geocaching.settings;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.activity.Keyboard;
import cgeo.geocaching.log.LogTemplateProvider;
import cgeo.geocaching.log.LogTemplateProvider.LogTemplate;
import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.utils.LocalizationUtils;

import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import java.util.List;
import java.util.function.BiConsumer;

import com.google.android.material.textfield.TextInputLayout;
import org.apache.commons.lang3.StringUtils;

public class TemplateTextPreference extends Preference {

    /**
     * default value, if none is given in the preference XML.
     */
    private static final String DEFAULT_VALUE = StringUtils.EMPTY;
    private String initialValue;

    public TemplateTextPreference(final Context context) {
        super(context);
    }

    public TemplateTextPreference(final Context context, final AttributeSet attrs) {
        super(context, attrs);
    }

    public TemplateTextPreference(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public void onBindViewHolder(@NonNull final PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        final Preference pref = findPreferenceInHierarchy(getKey());
        if (pref != null) {
            pref.setOnPreferenceClickListener(preference -> {
                showEditDialog(getContext(), true, null, Settings.getSignature(), (title, text) -> {
                    persistString(text);
                    callChangeListener(text);
                });
                return false;
            });
        }
    }

    /**
     * Shows a dialog to edit a signature (title is hidden then) or a log template (title and text).
     * onSave is called with the new title and text once the user confirms a valid, non-empty input.
     */
    public static void showEditDialog(@NonNull final Context context, final boolean isSignature, @Nullable final String initialTitle, @Nullable final String initialText, @NonNull final BiConsumer<String, String> onSave) {
        final View v = LayoutInflater.from(context).inflate(R.layout.template_preference_dialog, null);
        final TextInputLayout titleLayout = v.findViewById(R.id.titleLayout);
        final EditText editTitle = v.findViewById(R.id.title);
        final EditText editText = v.findViewById(R.id.edit);

        editText.setText(initialText);
        boolean focusOnText = true;
        if (!isSignature) {
            titleLayout.setVisibility(View.VISIBLE);
            editTitle.setText(initialTitle);
            focusOnText = StringUtils.isNotEmpty(editTitle.getText());
        }
        Dialogs.moveCursorToEnd(editText);
        Keyboard.show(context, focusOnText ? editText : editTitle);

        final AlertDialog dialog = Dialogs.newBuilder(context)
                .setView(v)
                .setPositiveButton(android.R.string.ok, null)
                .setNeutralButton(R.string.init_signature_template_button, null)
                .show();

        // override onClick listener to prevent closing dialog on pressing the "default" button
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(v2 -> {
            final String newTitle = editTitle.getText().toString();
            final String newText = editText.getText().toString();
            // check that for log templates both title and text are filled
            if (!isSignature && StringUtils.isEmpty(newTitle) && !StringUtils.isEmpty(newText)) {
                editTitle.setError(LocalizationUtils.getString(R.string.init_log_template_missing_error));
            } else if (!isSignature && !StringUtils.isEmpty(newTitle) && StringUtils.isEmpty(newText)) {
                editText.setError(LocalizationUtils.getString(R.string.init_log_template_missing_error));
            } else if (StringUtils.isEmpty(newTitle) && StringUtils.isEmpty(newText)) {
                // don't save empty templates
                dialog.dismiss();
            } else {
                onSave.accept(newTitle, newText);
                dialog.dismiss();
            }
        });
        dialog.getButton(DialogInterface.BUTTON_NEUTRAL).setOnClickListener(v2 -> {
            final AlertDialog.Builder templateBuilder = Dialogs.newBuilder(context);
            templateBuilder.setTitle(R.string.init_signature_template_button);
            final List<LogTemplate> templates = LogTemplateProvider.getTemplatesWithoutSignature(null);
            final String[] items = new String[templates.size()];
            for (int i = 0; i < templates.size(); i++) {
                items[i] = LocalizationUtils.getString(templates.get(i).getResourceId());
            }
            templateBuilder.setItems(items, (selectionDialog, position) -> {
                selectionDialog.dismiss();
                final String insertText = "[" + templates.get(position).getTemplateString() + "]";
                ActivityMixin.insertAtPosition(editText, insertText, true);
            });
            templateBuilder.create().show();
        });
    }

    @Override
    protected void onSetInitialValue(final Object defaultValue) {
        if (defaultValue == null) {
            // Restore existing state
            initialValue = this.getPersistedString(DEFAULT_VALUE);
        } else {
            // Set default state from the XML attribute
            initialValue = defaultValue.toString();
            persistString(initialValue);
        }
    }

    @Override
    protected Object onGetDefaultValue(final TypedArray array, final int index) {
        return array.getString(index);
    }
}
