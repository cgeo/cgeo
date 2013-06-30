package cgeo.geocaching.settings;

import cgeo.geocaching.R;

import org.apache.commons.lang3.StringUtils;

import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class NumberPickerPreference extends DialogPreference {

    private EditText editText;

    public NumberPickerPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public NumberPickerPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onBindDialogView(View view) {
        String msg = (String) this.getDialogMessage();
        if (StringUtils.isNotBlank(msg)) {
            TextView tv = (TextView) view.findViewById(R.id.number_picker_message);
            tv.setText(msg);
        }

        editText = (EditText) view.findViewById(R.id.number_picker_input);
        setValue(getPersistedInt(0));

        Button minus = (Button) view.findViewById(R.id.number_picker_minus);
        Button plus = (Button) view.findViewById(R.id.number_picker_plus);

        minus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View button) {
                Integer value = getValue();
                if (value != null) {
                    setValue(--value);
                }
            }
        });

        plus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View button) {
                Integer value = getValue();
                if (value != null) {
                    setValue(++value);
                }
            }
        });

        super.onBindDialogView(view);
    }

    private Integer getValue() {
        try {
            return Integer.parseInt(editText.getText().toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void setValue(final int value) {
        int v = value;
        if (v <= 0) {
            v = 0;
        } else if (v > Integer.MAX_VALUE) {
            v = Integer.MAX_VALUE;
        }
        editText.setText(String.valueOf(v));
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            Integer value = getValue();
            if (value != null) {
                persistInt(value);
                callChangeListener(value);
            }
        }
        super.onDialogClosed(positiveResult);
    }
}
