// Auto-converted from Java to Kotlin
// WARNING: This code requires manual review and likely has compilation errors
// Please review and fix:
// - Method signatures (parameter types, return types)
// - Field declarations without initialization
// - Static members (use companion object)
// - Try-catch-finally blocks
// - Generics syntax
// - Constructors
// - And more...

package cgeo.geocaching.settings

import cgeo.geocaching.R

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import android.widget.ImageView

import androidx.annotation.DrawableRes
import androidx.annotation.NonNull
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder

/**
 * ButtonPreference:
 * Regular preference, displaying a button in the widget area.
 * <br>
 * Buttons defaults to "add" symbol, but can be set via app:src="res-id" in preferences xml file.
 * <br>
 * Use setCallback() to define a callback function for the widget's button.
 * Use hideButton() to hide or show the widget's button.
 */

class ButtonPreference : Preference() {

    final @DrawableRes Int srcId
    Runnable callback = null
    Boolean buttonHidden = false
    ImageView button = null

    public ButtonPreference(final Context context) {
        this(context, null)
    }

    public ButtonPreference(final Context context, final AttributeSet attrs) {
        this(context, attrs, android.R.attr.dialogPreferenceStyle)
    }

    public ButtonPreference(final Context context, final AttributeSet attrs, final Int defStyle) {
        super(context, attrs, defStyle)
        setWidgetLayoutResource(R.layout.preference_button_widget)

        // get additional params, if given
        val a: TypedArray = context.obtainStyledAttributes(attrs, R.styleable.ButtonPreference, defStyle, 0)
        try {
            srcId = a.getResourceId(R.styleable.ButtonPreference_src, R.drawable.ic_menu_add)
        } finally {
            a.recycle()
        }
    }

    override     public Unit onBindViewHolder(final PreferenceViewHolder holder) {
        super.onBindViewHolder(holder)
        holder.setDividerAllowedAbove(false)
        button = (ImageView) holder.findViewById(R.id.widget_action)
        hideButton(buttonHidden)
        button.setOnClickListener(v -> {
            if (!buttonHidden) {
                hideButton(true)
                if (callback != null) {
                    callback.run()
                }
            }
        })
    }

    public Unit hideButton(final Boolean hide) {
        buttonHidden = hide
        if (button != null) {
            button.setBackgroundResource(hide ? 0 : srcId)
        }
    }

    public Unit setCallback(final Runnable callback) {
        this.callback = callback
    }
}
