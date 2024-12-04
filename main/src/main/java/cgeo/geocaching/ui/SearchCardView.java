package cgeo.geocaching.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;

import cgeo.geocaching.R;

public class SearchCardView extends CardView {

    public SearchCardView(@NonNull Context context) {
        super(context);
    }

    public SearchCardView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public SearchCardView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setTitle(final int title) {
        final TextView text = findViewById(R.id.text);
        text.setText(title);
    }

    public void setIcon(final int icon) {
        final ImageView img = findViewById(R.id.icon);
        img.setImageDrawable(getResources().getDrawable(icon));
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
