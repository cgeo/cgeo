package cgeo.geocaching.settings;

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

    public TextPreference(Context context) {
        super(context);
    }

    public TextPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public TextPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected int[] getAttributeNames() {
        return new int[] { android.R.attr.text };
    }

    @Override
    protected void processAttributeValues(TypedArray values) {
        this.text = values.getString(0);
    }

    @Override
    protected View onCreateView(ViewGroup parent) {
        this.setSelectable(false);

        View v = super.onCreateView(parent);

        TextView text = (TextView) v.findViewById(R.id.textPreferenceText);
        text.setText(this.text);

        summaryView = (TextView) v.findViewById(R.id.textPreferenceSummary);
        setSummary(null); // show saved summary text

        return v;
    }

    @Override
    public void setSummary(CharSequence summaryText) {
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
