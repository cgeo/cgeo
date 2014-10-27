package cgeo.geocaching.settings;

import butterknife.ButterKnife;

import cgeo.geocaching.R;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * Preference to simply show a text message. Links are not shown.
 * <p>
 * Usage: The displayed text is taken from the "android:text" attribute of the preference definition. Example:
 *
 * <pre>
 * <cgeo.geocaching.settings.TextPreference
 *      android:text="@string/legal_note"
 *      android:layout="@string/text_preference_default_layout"
 * />
 * </pre>
 *
 * </p>
 */
public class TextPreference extends AbstractAttributeBasedPrefence {

    private String text;
    private TextView summaryView;
    private CharSequence summaryText;

    public TextPreference(final Context context) {
        super(context);
    }

    public TextPreference(final Context context, final AttributeSet attrs) {
        super(context, attrs);
    }

    public TextPreference(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected int[] getAttributeNames() {
        return new int[] { android.R.attr.text };
    }

    @Override
    protected void processAttributeValues(final TypedArray values) {
        this.text = values.getString(0);
    }

    @Override
    protected View onCreateView(final ViewGroup parent) {
        this.setSelectable(false);

        final View v = super.onCreateView(parent);

        final TextView text = ButterKnife.findById(v, R.id.textPreferenceText);
        text.setText(this.text);

        summaryView = ButterKnife.findById(v, R.id.textPreferenceSummary);
        setSummary(null); // show saved summary text

        return v;
    }

    @Override
    public void setSummary(final CharSequence summaryText) {
        // the layout hasn't been inflated yet, save the summaryText for later use
        if (summaryView == null) {
            this.summaryText = summaryText;
            return;
        }

        // if summaryText is null, take it from the previously saved summary
        if (summaryText == null) {
            if (this.summaryText == null) {
                return;
            }
            summaryView.setText(this.summaryText);
        } else {
            summaryView.setText(summaryText);
        }
        this.summaryView.setVisibility(View.VISIBLE);
    }

}
