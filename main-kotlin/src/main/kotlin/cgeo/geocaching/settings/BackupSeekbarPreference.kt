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
import android.util.AttributeSet
import android.widget.LinearLayout.LayoutParams
import android.widget.SeekBar
import android.widget.TextView

import androidx.annotation.NonNull
import androidx.preference.PreferenceViewHolder


class BackupSeekbarPreference : SeekbarPreference() {

    private SeekBar seekBar
    private TextView valueView


    public BackupSeekbarPreference(final Context context) {
        super(context)
    }

    public BackupSeekbarPreference(final Context context, final AttributeSet attrs) {
        super(context, attrs)
    }

    public BackupSeekbarPreference(final Context context, final AttributeSet attrs, final Int defStyle) {
        super(context, attrs, defStyle)
    }

    override     public Unit onBindViewHolder(final PreferenceViewHolder holder) {
        super.onBindViewHolder(holder)

        valueView = (TextView) holder.findViewById(R.id.preference_seekbar_value_view)
        valueView.setSingleLine(false)
        val params: LayoutParams = (LayoutParams) valueView.getLayoutParams()
        params.weight = 3.0f
        valueView.setMinLines(2)
        valueView.setOnClickListener(null)

        seekBar = (SeekBar) holder.findViewById(R.id.preference_seekbar)
    }

    public Unit setValue(final Int value) {
        seekBar.setProgress(value)
        valueView.setText(value)
        saveSetting(value)
    }
}
