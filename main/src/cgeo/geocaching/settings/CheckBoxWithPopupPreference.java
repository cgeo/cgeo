package cgeo.geocaching.settings;

import cgeo.geocaching.R;
import cgeo.geocaching.ui.UrlPopup;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

public class CheckBoxWithPopupPreference extends CheckBoxPreference {

    // strings for the popup dialog
    private String title;
    private String text;
    private String url;
    private String urlButton;
    private OnPreferenceChangeListener baseOnPrefChangeListener = null;

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

        final TypedArray types = context.obtainStyledAttributes(attrs, new int[] {
                R.attr.title, R.attr.text, R.attr.url, R.attr.urlButton },
                defStyle, 0);

        title = types.getString(0);
        text = types.getString(1);
        url = types.getString(2);
        urlButton = types.getString(3);

        types.recycle();
    }

    @Override
    protected View onCreateView(final ViewGroup parent) {

        if (baseOnPrefChangeListener == null) {
            baseOnPrefChangeListener = getOnPreferenceChangeListener();
        }

        // show dialog when checkbox enabled
        setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(final Preference preference, final Object newValue) {
                if (baseOnPrefChangeListener != null) {
                    baseOnPrefChangeListener.onPreferenceChange(preference, newValue);
                }
                if (!(Boolean) newValue) {
                    return true;
                }
                new UrlPopup(preference.getContext()).show(title, text, url, urlButton);
                return true;
            }
        });

        return super.onCreateView(parent);
    }

}
