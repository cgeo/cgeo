package cgeo.geocaching.ui.dialog;

import butterknife.ButterKnife;

import cgeo.geocaching.R;

import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TimePicker;

import java.util.Calendar;

public class TimeDialog extends DialogFragment {

    public interface TimeDialogParent {
        abstract public void setTime(final Calendar date);
    }

    private Calendar date;

    public static TimeDialog getInstance(final Calendar date) {
        final TimeDialog timeDialog = new TimeDialog();
        final Bundle args = new Bundle();
        args.putSerializable("date", date);
        timeDialog.setArguments(args);
        return timeDialog;
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_TITLE, 0);
        final Bundle args = getArguments();
        date = (Calendar) args.getSerializable("date");
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.time, container, false);

        final TimePicker picker = ButterKnife.findById(view, R.id.picker);
        picker.setCurrentHour(date.get(Calendar.HOUR_OF_DAY));
        picker.setCurrentMinute(date.get(Calendar.MINUTE));
        picker.setOnTimeChangedListener(new TimePickerListener());
        picker.setIs24HourView(DateFormat.is24HourFormat(getActivity()));
        return view;
    }

    private class TimePickerListener implements TimePicker.OnTimeChangedListener {

        @Override
        public void onTimeChanged(final TimePicker picker, final int hour, final int minutes) {
            date.set(Calendar.HOUR_OF_DAY, hour);
            date.set(Calendar.MINUTE, minutes);

            ((TimeDialogParent) getActivity()).setTime(date);

        }
    }
}