package cgeo.geocaching.settings;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceViewHolder;

public class PreferenceCategory extends androidx.preference.PreferenceCategory {

    public PreferenceCategory(final Context ctx, final AttributeSet attrs, final int defStyle) {
        super(ctx, attrs, defStyle);
    }

    public PreferenceCategory(final Context ctx, final AttributeSet attrs) {
        super(ctx, attrs);
    }

    @Override
    public void onBindViewHolder(@NonNull final PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        final TextView summary = (TextView) holder.findViewById(android.R.id.summary);
        if (summary != null) {
            // Enable multiple line support
            summary.setSingleLine(false);
            summary.setMaxLines(10);
        }
    }

}
