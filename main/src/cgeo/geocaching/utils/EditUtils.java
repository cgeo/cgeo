package cgeo.geocaching.utils;

import android.text.InputType;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

public final class EditUtils {

    private EditUtils() {
        // utility class
    }

    public static void setActionListener(final EditText editText, final Runnable runnable) {
        editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {

            @Override
            public boolean onEditorAction(final TextView v, final int actionId, final KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_GO) {
                    runnable.run();
                    return true;
                }

                return false;
            }
        });

        editText.setOnKeyListener(new View.OnKeyListener() {

            @Override
            public boolean onKey(final View v, final int keyCode, final KeyEvent event) {
                // If the event is a key-down event on the "enter" button
                if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    runnable.run();
                    return true;
                }
                return false;
            }
        });

    }

    public static void disableSuggestions(final EditText edit) {
        edit.setInputType(edit.getInputType()
                | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
                | InputType.TYPE_TEXT_VARIATION_FILTER);
    }
}
