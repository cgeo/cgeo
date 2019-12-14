package cgeo.geocaching.ui.dialog;

import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.app.Dialog;
import android.os.Bundle;
import android.widget.DatePicker;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import java.util.Calendar;

public class DateDialog extends DialogFragment implements OnDateSetListener {

    private Calendar date;

    public interface DateDialogParent {
        void setDate(Calendar date);
    }

    public static DateDialog getInstance(final Calendar date) {
        final DateDialog dateDialog = new DateDialog();
        final Bundle args = new Bundle();
        args.putSerializable("date", date);
        dateDialog.setArguments(args);
        return dateDialog;
    }

    @Override
    @NonNull
    public Dialog onCreateDialog(final Bundle savedInstanceState) {
        final Bundle args = getArguments();
        date = (Calendar) args.getSerializable("date");

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

        ((DateDialogParent) getActivity()).setDate(date);
    }
}
