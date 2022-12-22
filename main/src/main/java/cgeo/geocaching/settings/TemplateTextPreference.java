package cgeo.geocaching.settings;

import cgeo.geocaching.R;
import cgeo.geocaching.activity.ActivityMixin;
import cgeo.geocaching.log.LogTemplateProvider;
import cgeo.geocaching.log.LogTemplateProvider.LogTemplate;
import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.ui.dialog.SimpleDialog;

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

import com.google.android.material.textfield.TextInputLayout;
import org.apache.commons.lang3.StringUtils;

public class TemplateTextPreference extends Preference {

    /**
     * default value, if none is given in the preference XML.
     */
    private static final String DEFAULT_VALUE = StringUtils.EMPTY;
    private EditText editText;
    private EditText editTitle;
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
    public void onBindViewHolder(final PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        final Preference pref = findPreferenceInHierarchy(getKey());
        if (pref != null) {
            pref.setOnPreferenceClickListener(preference -> {
                launchEditTemplateDialog();
                return false;
            });
        }
        final boolean isSignature = getKey().equals(getContext().getString(R.string.pref_signature));
        if (!isSignature) {
            holder.itemView.setOnLongClickListener(v -> {
                SimpleDialog.ofContext(getContext()).setTitle(R.string.init_log_template).setMessage(R.string.init_log_template_remove_confirm).confirm((dialog, which) -> {
                    Settings.putLogTemplate(new Settings.PrefLogTemplate(getKey(), null, null));
                    callChangeListener(null);
                });
                return true;
            });

        }
    }

    public void launchEditTemplateDialog() {
        final boolean isSignature = getKey().equals(getContext().getString(R.string.pref_signature));

        final View v = LayoutInflater.from(getContext()).inflate(R.layout.template_preference_dialog, null);
        final TextInputLayout titleLayout = v.findViewById(R.id.titleLayout);
        editTitle = v.findViewById(R.id.title);
        editText = v.findViewById(R.id.edit);

        if (isSignature) {
            editText.setText(Settings.getSignature());
        } else {
            titleLayout.setVisibility(View.VISIBLE);
            final Settings.PrefLogTemplate template = Settings.getLogTemplate(this.getKey());
            if (template != null) { // can be null if template contains no value
                editTitle.setText(template.getTitle());
                editText.setText(template.getText());
            }
        }
        Dialogs.moveCursorToEnd(editText);

        final AlertDialog dialog = Dialogs.newBuilder(getContext())
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
                editTitle.setError(getContext().getString(R.string.init_log_template_missing_error));
            } else if (!isSignature && !StringUtils.isEmpty(newTitle) && StringUtils.isEmpty(newText)) {
                editText.setError(getContext().getString(R.string.init_log_template_missing_error));
            } else if (StringUtils.isEmpty(newTitle) && StringUtils.isEmpty(newText)) {
                // don't save empty templates
                dialog.dismiss();
            } else {
                if (isSignature) {
                    persistString(newText);
                } else {
                    Settings.putLogTemplate(new Settings.PrefLogTemplate(getKey(), newTitle, newText));
                }
                callChangeListener(newText);
                dialog.dismiss();
            }
        });
        dialog.getButton(DialogInterface.BUTTON_NEUTRAL).setOnClickListener(v2 -> {
            final AlertDialog.Builder templateBuilder = Dialogs.newBuilder(TemplateTextPreference.this.getContext());
            templateBuilder.setTitle(R.string.init_signature_template_button);
            final List<LogTemplate> templates = LogTemplateProvider.getTemplatesWithoutSignature();
            final String[] items = new String[templates.size()];
            for (int i = 0; i < templates.size(); i++) {
                items[i] = getContext().getString(templates.get(i).getResourceId());
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
