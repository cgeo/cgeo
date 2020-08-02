package cgeo.geocaching.ui;

import cgeo.geocaching.utils.Formatter;
import cgeo.geocaching.utils.functions.Action1;

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

    /**
     * Initializes the instance. Typically called in an activities 'onCreate' method.
     * @param dateView textview to use for date display and picker handling. Usually this is a text button
     * @param timeView textview to use for time display and picker handling. Might be null
     * @param fragmentManager manager to use for creating sub dialogs (date picker)
     */
    public void init(@NonNull final TextView dateView, @Nullable final TextView timeView, @NonNull final FragmentManager fragmentManager) {

        this.dateButton = dateView;
        this.timeButton = timeView;
        this.fragmentManager = fragmentManager;
        this.date = Calendar.getInstance();

        this.dateButton.setOnClickListener(new DateListener());
        if (this.timeButton != null) {
            this.timeButton.setOnClickListener(new TimeListener());
        }
        triggerGui();
    }

    public void setTimeVisible(final boolean isVisible) {
        if (this.timeButton == null) {
            return;
        }

        if (isVisible) {
            this.timeButton.setVisibility(View.VISIBLE);
        } else {
            this.timeButton.setVisibility(View.GONE);
        }
    }

    public void setCalendar(final Calendar date) {
        setDate(date.getTime());
    }

    public void setDate(final Date date) {
        this.date.setTime(date);

        triggerGui();
    }

    private void triggerGui() {
        //make it visible
        this.dateButton.setText(Formatter.formatShortDateVerbally(this.date.getTime().getTime()));
        if (this.timeButton != null) {
            this.timeButton.setText(Formatter.formatTime(this.date.getTime().getTime()));
        }
    }

    public Calendar getCalendar() {
        return date;
    }

    public Date getDate() {
        return date.getTime();
    }

    private class DateListener implements View.OnClickListener {

        @Override
        public void onClick(final View arg0) {
            final DateDialog dateDialog = new DateDialog(date, c -> triggerGui());
            dateDialog.setCancelable(true);
            dateDialog.show(fragmentManager, "date_dialog");
        }
    }

    private class TimeListener implements View.OnClickListener {
        @Override
        public void onClick(final View arg0) {
            final TimeDialog timeDialog = new TimeDialog(date, c -> triggerGui());
            timeDialog.setCancelable(true);
            timeDialog.show(fragmentManager, "time_dialog");
        }
    }

    public static class DateDialog extends DialogFragment implements DatePickerDialog.OnDateSetListener {

        private final Calendar date;
        private final Action1<Calendar> changeTrigger;

        public DateDialog(final Calendar date, final Action1<Calendar> changeTrigger) {
            this.date = date;
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
            dialog.onDateChanged(dialog.getDatePicker(), year, month, day);
            return dialog;
        }

        @Override
        public void onDateSet(final DatePicker view, final int year, final int monthOfYear, final int dayOfMonth) {
            date.set(year, monthOfYear, dayOfMonth);
            this.changeTrigger.call(date);
        }
    }


    public static class TimeDialog extends DialogFragment implements TimePickerDialog.OnTimeSetListener {

        private final Calendar date;
        private final Action1<Calendar> changeTrigger;

        public TimeDialog(final Calendar date, final Action1<Calendar> changeTrigger) {
            this.date = date;
            this.changeTrigger = changeTrigger;
        }

        @Override
        @NonNull
        public Dialog onCreateDialog(final Bundle savedInstanceState) {

            final int hour = date.get(Calendar.HOUR_OF_DAY);
            final int minute = date.get(Calendar.MINUTE);

            // Create a new instance of TimePickerDialog and return it
            return new TimePickerDialog(getActivity(), this, hour, minute,
                    DateFormat.is24HourFormat(getActivity()));
        }

        @Override
        public void onTimeSet(final TimePicker view, final int hourOfDay, final int minute) {
            date.set(Calendar.HOUR_OF_DAY, hourOfDay);
            date.set(Calendar.MINUTE, minute);
            changeTrigger.call(date);
         }
    }


}
