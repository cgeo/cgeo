package cgeo.geocaching.ui;

import cgeo.geocaching.R;
import cgeo.geocaching.databinding.DateRangeSelectorViewBinding;
import cgeo.geocaching.utils.functions.Action1;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import java.util.Date;

import org.apache.commons.lang3.tuple.ImmutablePair;

public class DateRangeSelector extends LinearLayout {

    private final DateTimeEditor minDateEditor = new DateTimeEditor();
    private final DateTimeEditor maxDateEditor = new DateTimeEditor();
    private Action1<ImmutablePair<Date, Date>> changeListener;

    public DateRangeSelector(final Context context) {
        super(context);
        init();
    }

    public DateRangeSelector(final Context context, @Nullable final AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public DateRangeSelector(final Context context, @Nullable final AttributeSet attrs, final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public DateRangeSelector(final Context context, final AttributeSet attrs, final int defStyleAttr, final int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    public void setChangeListener(final Action1<ImmutablePair<Date, Date>> listener) {
        this.changeListener = listener;
    }

    private void init() {
        final View view = LayoutInflater.from(getContext()).inflate(R.layout.date_range_selector_view, this, true);
        final DateRangeSelectorViewBinding binding = DateRangeSelectorViewBinding.bind(view);
        this.setOrientation(VERTICAL);

        minDateEditor.init(binding.dateFrom, null, binding.dateFromReset, ((FragmentActivity) getContext()).getSupportFragmentManager());
        minDateEditor
                .setPreselectDate(new Date())
                .setChangeListener(d -> onChange(d, true));
        maxDateEditor.init(binding.dateTo, null, binding.dateToReset, ((FragmentActivity) getContext()).getSupportFragmentManager());
        maxDateEditor
                .setPreselectDate(new Date())
                .setChangeListener(d -> onChange(d, false));
        minDateEditor.setDate(null);
        maxDateEditor.setDate(null);
    }

    private void onChange(final Date d, final boolean minFieldChanged) {
        if (minFieldChanged) {
            maxDateEditor.setPreselectDate(d);
        } else {
            minDateEditor.setPreselectDate(d);
        }

        if (minDateEditor.getDate() != null && maxDateEditor.getDate() != null && minDateEditor.getDate().after(maxDateEditor.getDate())) {
            //switch
            final Date md = minDateEditor.getDate();
            minDateEditor.setDate(maxDateEditor.getDate());
            maxDateEditor.setDate(md);
        }

        if (changeListener != null) {
            changeListener.call(new ImmutablePair<>(minDateEditor.getDate(), maxDateEditor.getDate()));
        }
    }

    public void setMinMaxDate(final Date minDate, final Date maxDate) {
        minDateEditor.setDate(minDate);
        maxDateEditor.setDate(maxDate);
    }

    public Date getMinDate() {
        return minDateEditor.getDate();
    }

    public Date getMaxDate() {
        return maxDateEditor.getDate();
    }
}
