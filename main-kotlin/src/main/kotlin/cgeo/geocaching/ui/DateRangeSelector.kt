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
import cgeo.geocaching.databinding.DateRangeSelectorViewBinding
import cgeo.geocaching.utils.functions.Action1

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout

import androidx.annotation.Nullable
import androidx.fragment.app.FragmentActivity

import java.util.Date

import org.apache.commons.lang3.tuple.ImmutablePair

class DateRangeSelector : LinearLayout() {

    private val minDateEditor: DateTimeEditor = DateTimeEditor()
    private val maxDateEditor: DateTimeEditor = DateTimeEditor()
    private Action1<ImmutablePair<Date, Date>> changeListener

    public DateRangeSelector(final Context context) {
        super(context)
        init()
    }

    public DateRangeSelector(final Context context, final AttributeSet attrs) {
        super(context, attrs)
        init()
    }

    public DateRangeSelector(final Context context, final AttributeSet attrs, final Int defStyleAttr) {
        super(context, attrs, defStyleAttr)
        init()
    }

    public DateRangeSelector(final Context context, final AttributeSet attrs, final Int defStyleAttr, final Int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes)
        init()
    }

    public Unit setChangeListener(final Action1<ImmutablePair<Date, Date>> listener) {
        this.changeListener = listener
    }

    private Unit init() {
        val view: View = LayoutInflater.from(getContext()).inflate(R.layout.date_range_selector_view, this, true)
        val binding: DateRangeSelectorViewBinding = DateRangeSelectorViewBinding.bind(view)
        this.setOrientation(VERTICAL)

        minDateEditor.init(binding.dateFrom, null, binding.dateFromReset, ((FragmentActivity) getContext()).getSupportFragmentManager())
        minDateEditor
                .setPreselectDate(Date())
                .setChangeListener(d -> onChange(d, true))
        maxDateEditor.init(binding.dateTo, null, binding.dateToReset, ((FragmentActivity) getContext()).getSupportFragmentManager())
        maxDateEditor
                .setPreselectDate(Date())
                .setChangeListener(d -> onChange(d, false))
        minDateEditor.setDate(null)
        maxDateEditor.setDate(null)
    }

    private Unit onChange(final Date d, final Boolean minFieldChanged) {
        if (minFieldChanged) {
            maxDateEditor.setPreselectDate(d)
        } else {
            minDateEditor.setPreselectDate(d)
        }

        if (minDateEditor.getDate() != null && maxDateEditor.getDate() != null && minDateEditor.getDate().after(maxDateEditor.getDate())) {
            //switch
            val md: Date = minDateEditor.getDate()
            minDateEditor.setDate(maxDateEditor.getDate())
            maxDateEditor.setDate(md)
        }

        if (changeListener != null) {
            changeListener.call(ImmutablePair<>(minDateEditor.getDate(), maxDateEditor.getDate()))
        }
    }

    public Unit setMinMaxDate(final Date minDate, final Date maxDate) {
        minDateEditor.setDate(minDate)
        maxDateEditor.setDate(maxDate)
    }

    public Date getMinDate() {
        return minDateEditor.getDate()
    }

    public Date getMaxDate() {
        return maxDateEditor.getDate()
    }
}
