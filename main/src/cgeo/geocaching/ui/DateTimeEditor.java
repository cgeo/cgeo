package cgeo.geocaching.ui;

import cgeo.geocaching.R;
import cgeo.geocaching.utils.Formatter;
import cgeo.geocaching.utils.LocalizationUtils;
import cgeo.geocaching.utils.functions.Action1;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.DatePicker;
import android.widget.TextView;
import android.widget.TimePicker;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import java.util.Calendar;
import java.util.Date;

/**
 * cgeo-specifc implementation for a date and time selector.
 *
 * It is based on two text views (usually buttons) where selected date and time is displayed in a user-friendly way
 * and where on click a cgeo-specific pickup dialog appears. Time is optional.
 */
public class DateTimeEditor {

    private TextView dateButton;
    private TextView timeButton;
    private FragmentManager fragmentManager;

    private Calendar date;
    private final boolean[] dateUnset = new boolean[]{ false };
    private boolean allowUserToUnset = false;
    private Action1<Date> changeListener = null;

    /**
     * Initializes the instance. Typically called in an activities 'onCreate' method.
     * @param dateView textview to use for date display and picker handling. Usually this is a text button
     * @param timeView textview to use for time display and picker handling. Might be null
     * @param fragmentManager manager to use for creating sub dialogs (date picker)
     */
    public DateTimeEditor init(@NonNull final TextView dateView, @Nullable final TextView timeView, @NonNull final FragmentManager fragmentManager) {

        this.dateButton = dateView;
        this.timeButton = timeView;
        this.fragmentManager = fragmentManager;
        this.date = Calendar.getInstance();

        this.dateButton.setOnClickListener(new DateListener());
        if (this.timeButton != null) {
            this.timeButton.setOnClickListener(new TimeListener());
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

    public DateTimeEditor setAllowUserToUnset(final boolean allowUserToUnset) {
        this.allowUserToUnset = allowUserToUnset;
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
        if (dateUnset[0]) {
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
            final DateDialog dateDialog = new DateDialog(date, allowUserToUnset ? dateUnset : null, c -> triggerChange());
            dateDialog.setCancelable(true);
            dateDialog.show(fragmentManager, "date_dialog");
        }
    }

    private class TimeListener implements View.OnClickListener {
        @Override
        public void onClick(final View arg0) {
            final TimeDialog timeDialog = new TimeDialog(date, allowUserToUnset ? dateUnset : null, c -> triggerChange());
            timeDialog.setCancelable(true);
            timeDialog.show(fragmentManager, "time_dialog");
        }
    }

    public static class DateDialog extends DialogFragment implements DatePickerDialog.OnDateSetListener {

        private final Calendar date;
        private final boolean[] dateUnset;
        private final Action1<Calendar> changeTrigger;

        public DateDialog(final Calendar date, final boolean[] dateUnset, final Action1<Calendar> changeTrigger) {
            this.date = date;
            this.dateUnset = dateUnset;
            this.changeTrigger = changeTrigger;
        }

        @Override
        @NonNull
        public Dialog onCreateDialog(final Bundle savedInstanceState) {

            final int year = date.get(Calendar.YEAR);
            final int month = date.get(Calendar.MONTH);
            final int day = date.get(Calendar.DAY_OF_MONTH);

            // Create a new instance of DatePickerDialog and return it
            final DatePickerDialog dialog = new DatePickerDialog(getActivity(), this, year, month, day);
            if (dateUnset != null) {
                dialog.setButton(AlertDialog.BUTTON_NEUTRAL, LocalizationUtils.getString(R.string.datetime_clear_button), (v, b) -> {
                    dateUnset[0] = true;
                    date.setTime(new Date());
                    this.changeTrigger.call(date);
                });
            }
            return dialog;
        }

        @Override
        public void onDateSet(final DatePicker view, final int year, final int monthOfYear, final int dayOfMonth) {
            if (dateUnset != null) {
                dateUnset[0] = false;
            }
            date.set(year, monthOfYear, dayOfMonth);
            this.changeTrigger.call(date);
        }
    }


    public static class TimeDialog extends DialogFragment implements TimePickerDialog.OnTimeSetListener {

        private final Calendar date;
        private final boolean[] dateUnset;
        private final Action1<Calendar> changeTrigger;

        public TimeDialog(final Calendar date, final boolean[] dateUnset, final Action1<Calendar> changeTrigger) {
            this.date = date;
            this.dateUnset = dateUnset;
            this.changeTrigger = changeTrigger;
        }

        @Override
        @NonNull
        public Dialog onCreateDialog(final Bundle savedInstanceState) {

            final int hour = date.get(Calendar.HOUR_OF_DAY);
            final int minute = date.get(Calendar.MINUTE);

            // Create a new instance of TimePickerDialog and return it
            final TimePickerDialog dialog = new TimePickerDialog(getActivity(), this, hour, minute,
                    DateFormat.is24HourFormat(getActivity()));
            if (dateUnset != null) {
                dialog.setButton(AlertDialog.BUTTON_NEUTRAL, LocalizationUtils.getString(R.string.datetime_clear_button), (v, b) -> {
                    dateUnset[0] = true;
                    date.setTime(new Date());
                    this.changeTrigger.call(date);
                });
            }
            return dialog;
        }

        @Override
        public void onTimeSet(final TimePicker view, final int hourOfDay, final int minute) {
            if (dateUnset != null) {
                dateUnset[0] = false;
            }
            date.set(Calendar.HOUR_OF_DAY, hourOfDay);
            date.set(Calendar.MINUTE, minute);
            changeTrigger.call(date);
         }
    }


}
