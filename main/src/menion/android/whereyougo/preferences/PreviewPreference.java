package menion.android.whereyougo.preferences;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.Preference;
import android.util.AttributeSet;

public class PreviewPreference extends Preference {

    protected CharSequence summaryTemplate = "";
    protected CharSequence previewTemplate = "";
    protected CharSequence defaultValue = "";
    protected String mValue = "";

    public PreviewPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        summaryTemplate = super.getSummary();

        for (int i = 0; i < attrs.getAttributeCount(); i++) {

            String attr = attrs.getAttributeName(i);
            String val = attrs.getAttributeValue(i);
            if (attr.equalsIgnoreCase("previewTemplate")) {
                previewTemplate = val;
            }
            if (attr.equalsIgnoreCase("previewTemplate")) {
                previewTemplate = val;
            }
            if (attr.equalsIgnoreCase("defaultValue")) {
                defaultValue = val;
            }

        }
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getString(index);
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        setValue(restoreValue ? getPersistedString((String) defaultValue) : (String) defaultValue);
    }

    public void setValue(String value) {
        mValue = value;
        persistString(mValue);
        notifyChanged();
    }

    @Override
    public CharSequence getSummary() {
        String preview = previewTemplate.toString();
        if (preview.length() == 0) {
            preview = "(" + mValue + ")";
        } else {
            preview = preview.replace("%1$", mValue);
        }

        return preview + " " + summaryTemplate;
    }

}
