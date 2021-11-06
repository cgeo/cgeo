package cgeo.geocaching.settings;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.log.LogTemplateProvider;
import cgeo.geocaching.log.LogTemplateProvider.LogTemplate;
import cgeo.geocaching.ui.dialog.Dialogs;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.EditTextPreference;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

public class TemplateTextPreference extends EditTextPreference {

    // TODO

    /**
     * default value, if none is given in the preference XML.
     */
    private static final String DEFAULT_VALUE = StringUtils.EMPTY;
    private SettingsActivity settingsActivity;
    private EditText editText;
    private String initialValue;

    public TemplateTextPreference(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public TemplateTextPreference(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        setDialogLayoutResource(R.layout.template_preference_dialog);
    }

    /*@Override
    protected void onBindDialogView(final View view) {
        settingsActivity = (SettingsActivity) this.getContext();

        // TODO: replace with android id
        editText = view.findViewById(R.id.signature_dialog_text);
        editText.setText(getPersistedString(initialValue != null ? initialValue : StringUtils.EMPTY));
        Dialogs.moveCursorToEnd(editText);

        final Button button = view.findViewById(R.id.signature_templates);
        button.setOnClickListener(button1 -> {
            final AlertDialog.Builder alert = Dialogs.newBuilder(TemplateTextPreference.this.getContext());
            alert.setTitle(R.string.init_signature_template_button);
            final List<LogTemplate> templates = LogTemplateProvider.getTemplatesWithSignatureAndLogText();
            final String[] items = new String[templates.size()];
            for (int i = 0; i < templates.size(); i++) {
                items[i] = settingsActivity.getString(templates.get(i).getResourceId());
            }
            alert.setItems(items, (dialog, position) -> {
                dialog.dismiss();
                final LogTemplate template = templates.get(position);
                insertSignatureTemplate(template);
            });
            alert.create().show();
        });

        super.onBindDialogView(view);
    }

    */

    private void insertSignatureTemplate(final LogTemplate template) {
        final String insertText = "[" + template.getTemplateString() + "]";
        ActivityMixin.insertAtPosition(editText, insertText, true);
    }

    /*

    @Override
    protected void onDialogClosed(final boolean positiveResult) {
        if (positiveResult) {
            final String text = editText.getText().toString();
            persistString(text);
            callChangeListener(text);
        }
        super.onDialogClosed(positiveResult);
    }*/


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
