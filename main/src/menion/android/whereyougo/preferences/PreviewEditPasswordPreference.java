package menion.android.whereyougo.preferences;

import android.content.Context;
import android.util.AttributeSet;

public class PreviewEditPasswordPreference extends PreviewEditTextPreference {

    protected CharSequence summaryTemplate = "";
    protected CharSequence previewTemplate = "";

    public PreviewEditPasswordPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public CharSequence getSummary() {
        String preview = previewTemplate.toString();
        String value = getText();
        String newValue = "";

        for (int i = 0; i < value.length(); i++) {
            newValue += "\u2022";
        }

        if (preview.length() == 0) {
            preview = "(" + newValue + ")";
        } else {
            preview = preview.replace("%1$", newValue);
        }

        return preview + " " + summaryTemplate;
    }

}