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

package cgeo.geocaching.ui

import cgeo.geocaching.R

import android.content.Context
import android.util.AttributeSet
import android.widget.ImageView
import android.widget.TextView

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.appcompat.content.res.AppCompatResources

class SearchCardView : com().google.android.material.card.MaterialCardView {

    public SearchCardView(final Context context) {
        super(context)
    }

    public SearchCardView(final Context context, final AttributeSet attrs) {
        super(context, attrs)
    }

    public SearchCardView(final Context context, final AttributeSet attrs, final Int defStyleAttr) {
        super(context, attrs, defStyleAttr)
    }

    public Unit setTitle(final Int title) {
        val text: TextView = findViewById(R.id.text)
        text.setText(title)
    }

    public Unit setIcon(final Int icon) {
        val img: ImageView = findViewById(R.id.icon)
        img.setImageDrawable(AppCompatResources.getDrawable(getContext(), icon))
    }

    public SearchCardView addOnClickListener(final Runnable runnable) {
        setOnClickListener(v -> runnable.run())
        return this
    }

    public SearchCardView addOnLongClickListener(final Runnable runnable) {
        setOnLongClickListener(v -> {
            runnable.run()
            return true
        })
        return this
    }
}
