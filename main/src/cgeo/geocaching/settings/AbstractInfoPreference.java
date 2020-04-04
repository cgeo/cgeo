package cgeo.geocaching.settings;

import cgeo.geocaching.R;
import cgeo.geocaching.ui.UrlPopup;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
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
public abstract class AbstractInfoPreference extends AbstractAttributeBasedPreference {

    /**
     * Content of the dialog, filled from preferences XML.
     */
    protected String text;
    /**
     * URL for the second button, filled from preferences XML.
     */
    protected String url;
    /**
     * text for the second button to open an URL, filled from preferences XML.
     */
    private String urlButton;

    protected int icon;
    protected boolean forwarder = false;
    protected LayoutInflater inflater;

    public AbstractInfoPreference(final Context context) {
        super(context);
        init(context, R.layout.preference_info_icon, false);
    }

    public AbstractInfoPreference(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        init(context, R.layout.preference_info_icon, false);
    }

    public AbstractInfoPreference(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);
        init(context, R.layout.preference_info_icon, false);
    }

    protected void init(final Context context, final int icon, final boolean forwarder) {
        this.icon = icon;
        this.forwarder = forwarder;
        inflater = LayoutInflater.from(context);
        setPersistent(false);
    }

    @Override
    protected int[] getAttributeNames() {
        return new int[] { android.R.attr.text, R.attr.url, R.attr.urlButton };
    }

    @SuppressLint("ResourceType")
    @Override
    protected void processAttributeValues(final TypedArray values) {
        text = values.getString(0);
        url = values.getString(1);
        urlButton = values.getString(2);
    }

    @Override
    protected View onCreateView(final ViewGroup parent) {
        super.onCreateView(parent);   // call super to make lint happy

        // show popup when clicked
        setOnPreferenceClickListener(preference -> {
            if (forwarder) {
                new UrlPopup(preference.getContext()).forward(preference.getTitle().toString(), text, url);
            } else {
                new UrlPopup(preference.getContext()).show(preference.getTitle().toString(), text, url, urlButton);
            }
            // don't update the preference value
            return false;
        });

        return addInfoIcon(parent);
    }

    /**
     * Add an info icon at the left hand side of the preference.
     *
     */
    protected View addInfoIcon(final ViewGroup parent) {
        final View preferenceView = super.onCreateView(parent);

        final ImageView iconView = (ImageView) inflater.inflate(icon, parent, false);
        final LinearLayout frame = preferenceView.findViewById(android.R.id.widget_frame);
        frame.setVisibility(View.VISIBLE);
        frame.addView(iconView);

        return preferenceView;
    }

}
