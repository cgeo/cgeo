package cgeo.geocaching.utils;

import android.text.InputType;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;

public final class EditUtils {

    private EditUtils() {
        // utility class
    }

    public static void setActionListener(final EditText editText, final Runnable runnable) {
        editText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO) {
                runnable.run();
                return true;
            }

            return false;
        });

        editText.setOnKeyListener((v, keyCode, event) -> {
            // If the event is a key-down event on the "enter" button
            if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                runnable.run();
                return true;
            }
            return false;
        });

    }

    public static void disableSuggestions(final EditText edit) {
        edit.setInputType(edit.getInputType()
                | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
                | InputType.TYPE_TEXT_VARIATION_FILTER);
    }
}
