package cgeo.geocaching;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.widget.DatePicker;

import java.util.Calendar;

public class cgeodate extends Dialog {

    private cgLogForm parent = null;
    private Calendar date = Calendar.getInstance();

    public cgeodate(Activity contextIn, cgLogForm parentIn, Calendar dateIn) {
        super(contextIn);

        // init
        date = dateIn;
        parent = parentIn;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            requestWindowFeature(Window.FEATURE_NO_TITLE);
            getWindow().setLayout(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
        } catch (Exception e) {
            // nothing
        }

        setContentView(R.layout.date);

        DatePicker picker = (DatePicker) findViewById(R.id.picker);
        picker.init(date.get(Calendar.YEAR), date.get(Calendar.MONTH), date.get(Calendar.DATE), new pickerListener());
    }

    public class pickerListener implements DatePicker.OnDateChangedListener {

        @Override
        public void onDateChanged(DatePicker picker, int year, int month, int day) {
            if (parent != null) {
                date.set(year, month, day);

                parent.setDate(date);
            }
        }
    }
}
