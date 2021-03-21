package menion.android.whereyougo.preferences;

import android.content.Context;
import android.preference.ListPreference;
import android.util.AttributeSet;

public class PreviewListPreference extends ListPreference {

    protected CharSequence summaryTemplate = "";
    protected CharSequence previewTemplate = "";

    public PreviewListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        summaryTemplate = super.getSummary();

        for (int i = 0; i < attrs.getAttributeCount(); i++) {

            String attr = attrs.getAttributeName(i);
            String val = attrs.getAttributeValue(i);
            if (attr.equalsIgnoreCase("previewTemplate")) {
                previewTemplate = val;
            }
        }
    }

    @Override
    public CharSequence getSummary() {
        String preview = previewTemplate.toString();
        if (preview.length() == 0) {
            preview = "(" + getEntry() + ")";
        } else {
            preview = preview.replace("%1$", getEntry());
        }

        return preview + " " + summaryTemplate;
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        setSummary(getSummary());
    }

}
