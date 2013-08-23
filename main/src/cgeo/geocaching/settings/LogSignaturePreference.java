package cgeo.geocaching.settings;

import cgeo.geocaching.R;

import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class LogSignaturePreference extends DialogPreference {

    private SettingsActivity settingsActivity;
    private EditText editText;

    public LogSignaturePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public LogSignaturePreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        setDialogLayoutResource(R.layout.log_signature_preference_dialog);
    }

    @Override
    protected void onBindDialogView(View view) {
        settingsActivity = (SettingsActivity) this.getContext();

        editText = (EditText) view.findViewById(R.id.signature_dialog_text);
        editText.setText(getPersistedString(""));
        settingsActivity.setSignatureTextView(editText);

        Button templates = (Button) view.findViewById(R.id.signature_templates);
        settingsActivity.registerForContextMenu(templates);
        templates.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View templates) {
                settingsActivity.openContextMenu(templates);
            }
        });

        super.onBindDialogView(view);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            String text = editText.getText().toString();
            persistString(text);
            callChangeListener(text);
        }
        super.onDialogClosed(positiveResult);
    }
}
