package cgeo.geocaching.ui;

import cgeo.geocaching.R;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;

public class SearchCardView extends com.google.android.material.card.MaterialCardView {

    public SearchCardView(final @NonNull Context context) {
        super(context);
    }

    public SearchCardView(final @NonNull Context context, final @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public SearchCardView(final @NonNull Context context, final @Nullable AttributeSet attrs, final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setTitle(final int title) {
        final TextView text = findViewById(R.id.text);
        text.setText(title);
    }

    public void setIcon(final int icon) {
        final ImageView img = findViewById(R.id.icon);
        img.setImageDrawable(AppCompatResources.getDrawable(getContext(), icon));
    }

    public SearchCardView addOnClickListener(final Runnable runnable) {
        setOnClickListener(v -> {
            runnable.run();
        });
        return this;
    }

    public SearchCardView addOnLongClickListener(final Runnable runnable) {
        setOnLongClickListener(v -> {
            runnable.run();
            return true;
        });
        return this;
    }
}
