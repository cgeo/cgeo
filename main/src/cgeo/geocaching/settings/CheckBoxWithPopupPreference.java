package cgeo.geocaching.settings;

import cgeo.geocaching.R;
import cgeo.geocaching.ui.UrlPopup;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;

import androidx.preference.CheckBoxPreference;
import androidx.preference.PreferenceViewHolder;

public class CheckBoxWithPopupPreference extends CheckBoxPreference {

    // strings for the popup dialog
    private String title;
    private String text;
    private String url;
    private String urlButton;

    public CheckBoxWithPopupPreference(final Context context) {
        super(context);
    }

    public CheckBoxWithPopupPreference(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        processAttributes(context, attrs, 0);
    }

    public CheckBoxWithPopupPreference(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);
        processAttributes(context, attrs, defStyle);
    }

    private void processAttributes(final Context context, final AttributeSet attrs, final int defStyle) {
        if (attrs == null) {
            return; // coward's retreat
        }

        // Array need to be ordered. See: http://stackoverflow.com/a/19092511/944936
        final TypedArray types = context.obtainStyledAttributes(
                attrs,
                new int[]{R.attr.text, R.attr.popupTitle, R.attr.url, R.attr.urlButton},
                defStyle, 0);

        text = types.getString(0);
        title = types.getString(1);
        url = types.getString(2);
        urlButton = types.getString(3);

        types.recycle();
    }

    @Override
    public void onBindViewHolder(final PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        // show dialog when checkbox enabled
        setOnPreferenceChangeListener((preference, newValue) -> {
            if (!(Boolean) newValue) {
                return true;
            }
            new UrlPopup(preference.getContext()).show(title, text, url, urlButton);
            return true;
        });
    }

}
