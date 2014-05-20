package cgeo.geocaching.ui.dialog;

import cgeo.geocaching.R;

import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;

import java.util.Calendar;

public class DateDialog extends DialogFragment {

    public interface DateDialogParent {
        abstract public void setDate(final Calendar date);
    }

    private Calendar date;

    public static DateDialog getInstance(Calendar date) {
        DateDialog dd = new DateDialog();
        Bundle args = new Bundle();
        args.putSerializable("date", date);
        dd.setArguments(args);
        return dd;
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_TITLE, 0);
        Bundle args = getArguments();
        date = (Calendar) args.getSerializable("date");
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.date, container, false);

        final DatePicker picker = (DatePicker) v.findViewById(R.id.picker);
        picker.init(date.get(Calendar.YEAR), date.get(Calendar.MONTH), date.get(Calendar.DATE), new DatePickerListener());
        return v;
    }

    private class DatePickerListener implements DatePicker.OnDateChangedListener {

        @Override
        public void onDateChanged(DatePicker picker, int year, int month, int day) {
            date.set(year, month, day);

            ((DateDialogParent) getActivity()).setDate(date);

        }
    }
}