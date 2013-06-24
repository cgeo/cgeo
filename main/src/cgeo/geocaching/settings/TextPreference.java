package cgeo.geocaching.settings;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * Preference to simply show a text message.
 * <p>
 * Links are not shown - I tried everything (koem)
 * <p>
 * example: <cgeo.geocaching.settings.TextPreference android:text="@string/legal_note"
 * android:layout="@string/text_preference_default_layout" />
 */
public class TextPreference extends Preference {

    private String text;

    public TextPreference(Context context) {
        super(context);
    }

    public TextPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        processAttributes(context, attrs, 0);
    }

    public TextPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        processAttributes(context, attrs, defStyle);
    }

    private void processAttributes(Context context, AttributeSet attrs, int defStyle) {
        if (attrs == null) {
            return;
        }

        TypedArray types = context.obtainStyledAttributes(attrs, new int[] {
                android.R.attr.text }, defStyle, 0);
        this.text = types.getString(0);
        types.recycle();
    }

    @Override
    protected View onCreateView(ViewGroup parent) {
        this.setSelectable(false);

        View v = super.onCreateView(parent);
        if (v instanceof TextView) {
            ((TextView) v).setText(this.text);
        }
        return v;
    }
}
