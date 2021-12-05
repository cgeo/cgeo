package cgeo.geocaching.settings;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.log.LogTemplateProvider;
import cgeo.geocaching.log.LogTemplateProvider.LogTemplate;
import cgeo.geocaching.ui.dialog.Dialogs;

import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

public class TemplateTextPreference extends Preference {

    /**
     * default value, if none is given in the preference XML.
     */
    private static final String DEFAULT_VALUE = StringUtils.EMPTY;
    private SettingsActivity settingsActivity;
    private EditText editText;
    private String initialValue;

    public TemplateTextPreference(final Context context, final AttributeSet attrs) {
        super(context, attrs);
    }

    public TemplateTextPreference(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public void onBindViewHolder(final PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        final Preference pref = findPreferenceInHierarchy(getKey());
        if (pref != null) {
            pref.setOnPreferenceClickListener(preference -> {
                editSignature();
                return false;
            });
        }
    }

    private void editSignature() {
        settingsActivity = (SettingsActivity) this.getContext();

        final View v = LayoutInflater.from(getContext()).inflate(R.layout.template_preference_dialog, null);
        editText = v.findViewById(R.id.edit);
        editText.setText(getPersistedString(initialValue != null ? initialValue : StringUtils.EMPTY));
        Dialogs.moveCursorToEnd(editText);

        final AlertDialog dialog = Dialogs.newBuilder(getContext())
            .setView(v)
            .setPositiveButton(android.R.string.ok, (dialog1, which) -> {
                final String text = editText.getText().toString();
                persistString(text);
                callChangeListener(text);
            })
            .setNeutralButton(R.string.init_signature_template_button, null)
            .show();

        // override onClick listener to prevent closing dialog on pressing the "default" button
        dialog.getButton(DialogInterface.BUTTON_NEUTRAL).setOnClickListener(v2 -> {
            final AlertDialog.Builder templateBuilder = Dialogs.newBuilder(TemplateTextPreference.this.getContext());
            templateBuilder.setTitle(R.string.init_signature_template_button);
            final List<LogTemplate> templates = LogTemplateProvider.getTemplatesWithSignatureAndLogText();
            final String[] items = new String[templates.size()];
            for (int i = 0; i < templates.size(); i++) {
                items[i] = settingsActivity.getString(templates.get(i).getResourceId());
            }
            templateBuilder.setItems(items, (dialog3, position) -> {
                dialog3.dismiss();
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
