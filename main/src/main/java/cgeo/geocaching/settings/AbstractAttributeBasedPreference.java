package cgeo.geocaching.settings;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;

import androidx.annotation.Nullable;
import androidx.preference.Preference;

/**
 * Base class for preferences which evaluate their XML attributes for further processing.
 */
public abstract class AbstractAttributeBasedPreference extends Preference {

    public AbstractAttributeBasedPreference(final Context context) {
        super(context);
    }

    public AbstractAttributeBasedPreference(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        processAttributes(context, attrs, 0);
    }

    public AbstractAttributeBasedPreference(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);
        processAttributes(context, attrs, defStyle);
    }

    private void processAttributes(final Context context, @Nullable final AttributeSet attrs, final int defStyle) {
        if (attrs == null) {
            return;
        }
        final TypedArray types = context.obtainStyledAttributes(attrs, getAttributeNames(),
                defStyle, 0);

        processAttributeValues(types);

        types.recycle();
    }

    /**
     * Evaluate the attributes which where requested in {@link AbstractAttributeBasedPreference#getAttributeNames()}.
     */
    protected abstract void processAttributeValues(TypedArray values);

    /**
     * @return the names of the attributes you want to read in your preference implementation
     */
    protected abstract int[] getAttributeNames();

}
