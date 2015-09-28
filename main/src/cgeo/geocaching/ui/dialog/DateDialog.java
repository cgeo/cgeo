package cgeo.geocaching.ui.dialog;

import android.annotation.TargetApi;
import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.app.Dialog;
import android.os.Build;
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
        final DatePickerDialog dialog = new DatePickerDialog(getActivity(), this, year, month, day);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            forceTitleUpdate(year, month, day, dialog);
        }
        return dialog;
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private static void forceTitleUpdate(final int year, final int month, final int day, final DatePickerDialog dialog) {
        dialog.onDateChanged(dialog.getDatePicker(), year, month, day);
    }

    @Override
    public void onDateSet(final DatePicker view, final int year, final int monthOfYear, final int dayOfMonth) {
        date.set(year, monthOfYear, dayOfMonth);

        ((DateDialogParent) getActivity()).setDate(date);
    }
}