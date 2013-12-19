package cgeo.geocaching.settings;

import cgeo.geocaching.R;
import cgeo.geocaching.ui.UrlPopup;

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

/**
 * Preference which shows a dialog containing textual explanation. The dialog has two buttons, where one will open a
 * hyper link with more detailed information.
 * <p>
 * The URL for the hyper link and the text are given as custom attributes in the preference XML definition.
 * </p>
 *
 */
public class InfoPreference extends AbstractAttributeBasedPrefence {

    /**
     * Content of the dialog, filled from preferences XML.
     */
    private String text;
    /**
     * URL for the second button, filled from preferences XML.
     */
    private String url;
    /**
     * text for the second button to open an URL, filled from preferences XML.
     */
    private String urlButton;

    private LayoutInflater inflater;

    public InfoPreference(Context context) {
        super(context);
        init(context);
    }

    public InfoPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public InfoPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {
        inflater = ((Activity) context).getLayoutInflater();
        setPersistent(false);
    }

    @Override
    protected int[] getAttributeNames() {
        return new int[] { android.R.attr.text, R.attr.url, R.attr.urlButton };
    }

    @Override
    protected void processAttributeValues(TypedArray values) {
        text = values.getString(0);
        url = values.getString(1);
        urlButton = values.getString(2);
    }

    @Override
    protected View onCreateView(ViewGroup parent) {

        // show popup when clicked
        setOnPreferenceClickListener(new OnPreferenceClickListener() {

            @Override
            public boolean onPreferenceClick(final Preference preference) {
                new UrlPopup(preference.getContext()).show(preference.getTitle().toString(), text, url, urlButton);
                // don't update the preference value
                return false;
            }
        });

        return addInfoIcon(parent);
    }

    /**
     * Add an info icon at the left hand side of the preference.
     *
     * @param parent
     * @return
     */
    private View addInfoIcon(ViewGroup parent) {
        View preferenceView = super.onCreateView(parent);

        ImageView iconView = (ImageView) inflater.inflate(R.layout.preference_info_icon, parent, false);
        LinearLayout frame = (LinearLayout) preferenceView.findViewById(android.R.id.widget_frame);
        frame.setVisibility(View.VISIBLE);
        frame.addView(iconView);

        return preferenceView;
    }

}
