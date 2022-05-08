package cgeo.geocaching.ui;

import cgeo.geocaching.R;
import cgeo.geocaching.utils.Formatter;
import cgeo.geocaching.utils.LocalizationUtils;
import cgeo.geocaching.utils.functions.Action1;

import android.text.format.DateFormat;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;

/**
 * cgeo-specifc implementation for a date and time selector.
 *
 * It is based on two text views (usually buttons) where selected date and time is displayed in a user-friendly way
 * and where on click a cgeo-specific pickup dialog appears. Time and reset is optional.
 */
public class DateTimeEditor {

    private TextView dateButton;
    private TextView timeButton;
    private View resetButton;
    private FragmentManager fragmentManager;

    private Calendar date;
    private final boolean[] dateUnset = new boolean[]{false};
    private Action1<Date> changeListener = null;

    /**
     * Initializes the instance. Typically called in an activities 'onCreate' method.
     *
     * @param dateView        textview to use for date display and picker handling. Usually this is a text button
     * @param timeView        textview to use for time display and picker handling. Might be null
     * @param resetButton     view to use for resetting the date. Might be null
     * @param fragmentManager manager to use for creating sub dialogs (date picker)
     */
    public DateTimeEditor init(@NonNull final TextView dateView, @Nullable final TextView timeView, @Nullable final View resetButton, @NonNull final FragmentManager fragmentManager) {

        this.dateButton = dateView;
        this.timeButton = timeView;
        this.resetButton = resetButton;
        this.fragmentManager = fragmentManager;
        this.date = Calendar.getInstance();

        this.dateButton.setOnClickListener(new DateListener());
        if (this.timeButton != null) {
            this.timeButton.setOnClickListener(new TimeListener());
        }
        if (this.resetButton != null) {
            this.resetButton.setOnClickListener(new ResetListener());
        }
        triggerChange();
        return this;
    }

    public DateTimeEditor setTimeVisible(final boolean isVisible) {
        if (this.timeButton == null) {
            return this;
        }

        if (isVisible) {
            this.timeButton.setVisibility(View.VISIBLE);
        } else {
            this.timeButton.setVisibility(View.GONE);
        }
        return this;
    }

    public DateTimeEditor setCalendar(final Calendar date) {
        return setDate(date == null ? null : date.getTime());
    }

    public DateTimeEditor setDate(final Date date) {

        this.date.setTime(date == null ? new Date() : date);
        this.dateUnset[0] = date == null;

        triggerChange();
        return this;
    }

    public DateTimeEditor setChangeListener(final Action1<Date> changeListener) {
        this.changeListener = changeListener;
        return this;
    }

    /**
     * If currently there is no date/time set, then this method sets the preselected date for later calls.
     * If a date is selected, this method does nothing
     */
    public DateTimeEditor setPreselectDate(final Date preselectDate) {
        if (dateUnset[0] && preselectDate != null) {
            date.setTime(preselectDate);
        }
        return this;
    }

    private void triggerChange() {
        //GUI: make it visible
        this.dateButton.setText(dateUnset[0] ? LocalizationUtils.getString(R.string.datetime_nodateset_display) : Formatter.formatShortDateVerbally(this.date.getTime().getTime()));
        if (this.timeButton != null) {
            this.timeButton.setText(dateUnset[0] ? LocalizationUtils.getString(R.string.datetime_nodateset_display) : Formatter.formatTime(this.date.getTime().getTime()));
        }
        if (this.resetButton != null) {
            this.resetButton.setVisibility(dateUnset[0] ? View.INVISIBLE : View.VISIBLE);
        }

        //call listener if any
        if (this.changeListener != null) {
            this.changeListener.call(dateUnset[0] ? null : this.date.getTime());
        }
    }

    public Calendar getCalendar() {
        return dateUnset[0] ? null : date;
    }

    public Date getDate() {
        return dateUnset[0] ? null : date.getTime();
    }

    private class DateListener implements View.OnClickListener {

        @Override
        public void onClick(final View arg0) {
            final MaterialDatePicker<Long> dateDialog = MaterialDatePicker.Builder
                    .datePicker()
                    .setSelection(date.getTimeInMillis() + TimeZone.getDefault().getRawOffset())
                    .build();
            dateDialog.addOnPositiveButtonClickListener(timestamp -> {
                if (resetButton != null) {
                    dateUnset[0] = false;
                }

                final Calendar newDate = Calendar.getInstance();
                newDate.setTimeInMillis(timestamp - TimeZone.getDefault().getRawOffset());

                // only update the date, but keep the previous time
                date.set(newDate.get(Calendar.YEAR), newDate.get(Calendar.MONTH), newDate.get(Calendar.DAY_OF_MONTH));
                triggerChange();
            });
            dateDialog.show(fragmentManager, "date_dialog");
        }
    }

    private class TimeListener implements View.OnClickListener {

        @Override
        public void onClick(final View arg0) {
            final MaterialTimePicker timeDialog = new MaterialTimePicker.Builder()
                    .setTimeFormat(DateFormat.is24HourFormat(timeButton.getContext()) ? TimeFormat.CLOCK_24H : TimeFormat.CLOCK_12H)
                    .setHour(date.get(Calendar.HOUR_OF_DAY))
                    .setMinute(date.get(Calendar.MINUTE))
                    .build();
            timeDialog.addOnPositiveButtonClickListener(v -> {
                if (resetButton != null) {
                    dateUnset[0] = false;
                }
                date.set(Calendar.HOUR_OF_DAY, timeDialog.getHour());
                date.set(Calendar.MINUTE, timeDialog.getMinute());
                triggerChange();
            });
            timeDialog.show(fragmentManager, "time_dialog");
        }
    }

    private class ResetListener implements View.OnClickListener {

        @Override
        public void onClick(final View arg0) {
            dateUnset[0] = true;
            date.setTime(new Date());
            triggerChange();
        }
    }
}
