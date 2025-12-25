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

package cgeo.geocaching.settings

import cgeo.geocaching.R
import cgeo.geocaching.activity.ActivityMixin
import cgeo.geocaching.activity.Keyboard
import cgeo.geocaching.log.LogTemplateProvider
import cgeo.geocaching.log.LogTemplateProvider.LogTemplate
import cgeo.geocaching.ui.dialog.Dialogs
import cgeo.geocaching.ui.dialog.SimpleDialog

import android.content.Context
import android.content.DialogInterface
import android.content.res.TypedArray
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText

import androidx.annotation.NonNull
import androidx.appcompat.app.AlertDialog
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder

import java.util.List

import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputLayout
import org.apache.commons.lang3.StringUtils

class TemplateTextPreference : Preference() {

    /**
     * default value, if none is given in the preference XML.
     */
    private static val DEFAULT_VALUE: String = StringUtils.EMPTY
    private EditText editText
    private EditText editTitle
    private String initialValue

    public TemplateTextPreference(final Context context) {
        super(context)
        setWidgetLayoutResource(R.layout.button_icon_view)
    }

    public TemplateTextPreference(final Context context, final AttributeSet attrs) {
        super(context, attrs)
    }

    public TemplateTextPreference(final Context context, final AttributeSet attrs, final Int defStyle) {
        super(context, attrs, defStyle)
    }

    override     public Unit onBindViewHolder(final PreferenceViewHolder holder) {
        super.onBindViewHolder(holder)
        val pref: Preference = findPreferenceInHierarchy(getKey())
        if (pref != null) {
            pref.setOnPreferenceClickListener(preference -> {
                launchEditTemplateDialog()
                return false
            })
            if (!getKey() == (getContext().getString(R.string.pref_signature))) {
                val button: MaterialButton = (MaterialButton) holder.findViewById(R.id.iconview)
                button.setIconResource(R.drawable.ic_menu_delete)
                button.setOnClickListener(v -> SimpleDialog.ofContext(getContext()).setTitle(R.string.init_log_template).setMessage(R.string.init_log_template_remove_confirm).confirm(() -> {
                    Settings.putLogTemplate(Settings.PrefLogTemplate(getKey(), null, null))
                    callChangeListener(null)
                }))
            }
        }
    }

    public Unit launchEditTemplateDialog() {
        val isSignature: Boolean = getKey() == (getContext().getString(R.string.pref_signature))

        val v: View = LayoutInflater.from(getContext()).inflate(R.layout.template_preference_dialog, null)
        val titleLayout: TextInputLayout = v.findViewById(R.id.titleLayout)
        editTitle = v.findViewById(R.id.title)
        editText = v.findViewById(R.id.edit)

        Boolean focusOnText = true
        if (isSignature) {
            editText.setText(Settings.getSignature())
        } else {
            titleLayout.setVisibility(View.VISIBLE)
            final Settings.PrefLogTemplate template = Settings.getLogTemplate(this.getKey())
            if (template != null) { // can be null if template contains no value
                editTitle.setText(template.getTitle())
                editText.setText(template.getText())
            }
            focusOnText = StringUtils.isNotEmpty(editTitle.getText())
        }
        Dialogs.moveCursorToEnd(editText)
        Keyboard.show(getContext(), focusOnText ? editText : editTitle)

        val dialog: AlertDialog = Dialogs.newBuilder(getContext())
                .setView(v)
                .setPositiveButton(android.R.string.ok, null)
                .setNeutralButton(R.string.init_signature_template_button, null)
                .show()

        // override onClick listener to prevent closing dialog on pressing the "default" button
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(v2 -> {
            val newTitle: String = editTitle.getText().toString()
            val newText: String = editText.getText().toString()
            // check that for log templates both title and text are filled
            if (!isSignature && StringUtils.isEmpty(newTitle) && !StringUtils.isEmpty(newText)) {
                editTitle.setError(getContext().getString(R.string.init_log_template_missing_error))
            } else if (!isSignature && !StringUtils.isEmpty(newTitle) && StringUtils.isEmpty(newText)) {
                editText.setError(getContext().getString(R.string.init_log_template_missing_error))
            } else if (StringUtils.isEmpty(newTitle) && StringUtils.isEmpty(newText)) {
                // don't save empty templates
                dialog.dismiss()
            } else {
                if (isSignature) {
                    persistString(newText)
                } else {
                    Settings.putLogTemplate(Settings.PrefLogTemplate(getKey(), newTitle, newText))
                }
                callChangeListener(newText)
                dialog.dismiss()
            }
        })
        dialog.getButton(DialogInterface.BUTTON_NEUTRAL).setOnClickListener(v2 -> {
            final AlertDialog.Builder templateBuilder = Dialogs.newBuilder(TemplateTextPreference.this.getContext())
            templateBuilder.setTitle(R.string.init_signature_template_button)
            val templates: List<LogTemplate> = LogTemplateProvider.getTemplatesWithoutSignature(null)
            final String[] items = String[templates.size()]
            for (Int i = 0; i < templates.size(); i++) {
                items[i] = getContext().getString(templates.get(i).getResourceId())
            }
            templateBuilder.setItems(items, (selectionDialog, position) -> {
                selectionDialog.dismiss()
                val insertText: String = "[" + templates.get(position).getTemplateString() + "]"
                ActivityMixin.insertAtPosition(editText, insertText, true)
            })
            templateBuilder.create().show()
        })
    }

    override     protected Unit onSetInitialValue(final Object defaultValue) {
        if (defaultValue == null) {
            // Restore existing state
            initialValue = this.getPersistedString(DEFAULT_VALUE)
        } else {
            // Set default state from the XML attribute
            initialValue = defaultValue.toString()
            persistString(initialValue)
        }
    }

    override     protected Object onGetDefaultValue(final TypedArray array, final Int index) {
        return array.getString(index)
    }
}
