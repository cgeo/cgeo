package cgeo.geocaching.settings;

import cgeo.geocaching.R;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.ImageView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

/**
 * ButtonPreference:
 * Regular preference, displaying a button in the widget area.
 * <br>
 * Buttons defaults to "add" symbol, but can be set via app:src="res-id" in preferences xml file.
 * <br>
 * Use setCallback() to define a callback function for the widget's button.
 * Use hideButton() to hide or show the widget's button.
 */

public class ButtonPreference extends Preference {

    final @DrawableRes int srcId;
    Runnable callback = null;
    boolean buttonHidden = false;
    ImageView button = null;

    public ButtonPreference(final Context context) {
        this(context, null);
    }

    public ButtonPreference(final Context context, final AttributeSet attrs) {
        this(context, attrs, android.R.attr.dialogPreferenceStyle);
    }

    public ButtonPreference(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);
        setWidgetLayoutResource(R.layout.preference_button_widget);

        // get additional params, if given
        final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ButtonPreference, defStyle, 0);
        try {
            srcId = a.getResourceId(R.styleable.ButtonPreference_src, R.drawable.ic_menu_add);
        } finally {
            a.recycle();
        }
    }

    @Override
    public void onBindViewHolder(@NonNull final PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        holder.setDividerAllowedAbove(false);
        button = (ImageView) holder.findViewById(R.id.widget_action);
        hideButton(buttonHidden);
        button.setOnClickListener(v -> {
            if (!buttonHidden) {
                hideButton(true);
                if (callback != null) {
                    callback.run();
                }
            }
        });
    }

    public void hideButton(final boolean hide) {
        buttonHidden = hide;
        if (button != null) {
            button.setBackgroundResource(hide ? 0 : srcId);
        }
    }

    public void setCallback(final Runnable callback) {
        this.callback = callback;
    }
}
