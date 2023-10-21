package cgeo.geocaching.settings;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

/**
 * Behaves exactly the same as Preference, only needed to have a separate class to compare against
 */

public class PreferenceTextAlwaysShow extends Preference {

    public PreferenceTextAlwaysShow(final @NonNull Context context) {
        this(context, null);
    }

    public PreferenceTextAlwaysShow(final @NonNull Context context, final @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PreferenceTextAlwaysShow(final @NonNull Context context, final @Nullable AttributeSet attrs, final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void onBindViewHolder(@NonNull final PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        holder.setDividerAllowedAbove(false);
    }
}
