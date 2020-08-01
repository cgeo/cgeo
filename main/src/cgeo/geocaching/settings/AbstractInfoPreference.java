package cgeo.geocaching.settings;

import cgeo.geocaching.R;
import cgeo.geocaching.ui.UrlPopup;
import cgeo.geocaching.ui.dialog.Dialogs;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import org.apache.commons.lang3.StringUtils;

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
    private String urlButton;

    private boolean isIntent = false;
    protected int icon;
    protected LayoutInflater inflater;

    // for isIntent == true:
    private Runnable startIntent;

    // for isIntent == false:
    protected String url;
    protected boolean forwarder = false;

    public AbstractInfoPreference(final Context context) {
        this(context, null);
    }

    public AbstractInfoPreference(final Context context, final AttributeSet attrs) {
        this(context, attrs, android.R.attr.preferenceStyle);
    }

    public AbstractInfoPreference(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);
        init(context, R.layout.preference_info_icon, false);
    }

    protected void init(final Context context, final int icon, final boolean forwarder) {
        this.isIntent = false;
        this.forwarder = forwarder;

        this.icon = icon;
        inflater = LayoutInflater.from(context);
        setPersistent(false);
    }

    protected void init(final Activity activity, final int icon, final Runnable startIntent) {
        this.isIntent = true;
        this.startIntent = startIntent;

        this.icon = icon;
        inflater = LayoutInflater.from(activity);
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
            if (isIntent) {
                if (StringUtils.isNotBlank(text) && StringUtils.isNotBlank(urlButton)) {
                    Dialogs.confirm((Activity) preference.getContext(), preference.getTitle().toString(), text, urlButton, (dialog, which) -> startIntent.run());
                } else {
                    startIntent.run();
                }
            } else {
                if (forwarder) {
                    new UrlPopup(preference.getContext()).forward(preference.getTitle().toString(), text, url);
                } else {
                    new UrlPopup(preference.getContext()).show(preference.getTitle().toString(), text, url, urlButton);
                }
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
