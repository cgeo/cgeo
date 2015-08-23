package cgeo.geocaching.ui.dialog;

import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.widget.DatePicker;

import java.util.Calendar;

public class DateDialog extends DialogFragment implements OnDateSetListener {

    public interface DateDialogParent {
        abstract public void setDate(final Calendar date);
    }

    private Calendar date;

    public static DateDialog getInstance(final Calendar date) {
        final DateDialog dateDialog = new DateDialog();
        final Bundle args = new Bundle();
        args.putSerializable("date", date);
        dateDialog.setArguments(args);
        return dateDialog;
    }

    @Override
    public Dialog onCreateDialog(final Bundle savedInstanceState) {

        final Bundle args = getArguments();
        date = (Calendar) args.getSerializable("date");

        final int year = date.get(Calendar.YEAR);
        final int month = date.get(Calendar.MONTH);
        final int day = date.get(Calendar.DAY_OF_MONTH);

        // Create a new instance of DatePickerDialog and return it
        return new DatePickerDialog(getActivity(), this, year, month, day);
    }

    @Override
    public void onDateSet(final DatePicker view, final int year, final int monthOfYear, final int dayOfMonth) {
        date.set(year, monthOfYear, dayOfMonth);

        ((DateDialogParent) getActivity()).setDate(date);
    }
}