package cgeo.geocaching.ui.dialog;

import android.app.Dialog;
import android.app.TimePickerDialog;
import android.app.TimePickerDialog.OnTimeSetListener;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.widget.TimePicker;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import java.util.Calendar;

public class TimeDialog extends DialogFragment implements OnTimeSetListener {

    private Calendar date;

    public interface TimeDialogParent {
        void setTime(Calendar date);
    }

    public static TimeDialog getInstance(final Calendar date) {
        final TimeDialog timeDialog = new TimeDialog();
        final Bundle args = new Bundle();
        args.putSerializable("date", date);
        timeDialog.setArguments(args);
        return timeDialog;
    }

    @Override
    @NonNull
    public Dialog onCreateDialog(final Bundle savedInstanceState) {

        final Bundle args = getArguments();
        date = (Calendar) args.getSerializable("date");

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

        ((TimeDialogParent) getActivity()).setTime(date);
    }
}
