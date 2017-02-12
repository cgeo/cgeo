package cgeo.geocaching.ui;

import android.content.Context;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Toast;

import cgeo.geocaching.R;

import static android.text.InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS;
import static android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS;

/**
 * This class allows for a user to change the text on a button with a long-click.
 */

public class EditButton extends RelativeLayout {

    EditText edit;  // EditText used to facilitate keyboard entry.
    Button butt;    // The actual button used for the most part.

    private class CoordDigitLongClickListener implements View.OnLongClickListener {

        // This implementation was obtained from 'Adithya' via the stack overflow question 'Long-press on Button to Change Text':
        // https://stackoverflow.com/questions/44858720/long-press-on-button-to-change-text/44859328#44859328

        @Override
        public boolean onLongClick(final View view) {

            final InputMethodManager imm = (InputMethodManager)
                    getContext().getSystemService(Context.INPUT_METHOD_SERVICE);

            butt.setVisibility(View.INVISIBLE);
            edit.setVisibility(View.VISIBLE);

            if (edit.requestFocus()) {
                imm.showSoftInput(edit, InputMethodManager.SHOW_IMPLICIT);
            }

            edit.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(final CharSequence s, final int start, final int count, final int after) {
                    // Intentionally left empty
                }

                @Override
                public void onTextChanged(final CharSequence s, final int start, final int before, final int count) {
                    // Intentionally left empty
                }

                @Override
                public void afterTextChanged(final Editable s) {
                    final Editable text = edit.getText();

                    if (text.length() > 0) {
                        final char customChar = text.charAt(0);

                        if ('A' <= customChar && customChar <= 'Z'
                         || 'a' <= customChar && customChar <= 'z'
                         || '0' <= customChar && customChar <= '9'
                         || customChar == ' ') {
                            setCustomChar(Character.toUpperCase(customChar));
                        } else {
                            final Context context = getContext();
                            Toast.makeText(context, context.getString(R.string.warn_invalid_character), Toast.LENGTH_SHORT).show();
                        }

                        edit.setText("");
                    } else {
                        edit.clearFocus();
                    }
                }
            });

            edit.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(final View v, final boolean hasFocus) {
                    if (!hasFocus) {
                        edit.setVisibility(View.INVISIBLE);
                        butt.setVisibility(View.VISIBLE);
                        imm.hideSoftInputFromWindow(edit.getWindowToken(), 0);
                    }
                }
            });

            return true;
        }
    }

    // These variables are accessed from the derived class 'CalculateButton'.
    public EditButton(final Context context) {
        super(context);
        addViews(context);
    }

    public EditButton(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        addViews(context);
    }

    private void addViews(final Context context) {

        setLongClickable(true);

        edit = new EditText(context);
        edit.setMaxLines(1);
        edit.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM);
        edit.setInputType(InputType.TYPE_CLASS_TEXT);
        edit.setClickable(false);
        edit.setLongClickable(false);
        edit.setTextSize(22f);
        edit.setInputType(TYPE_TEXT_FLAG_CAP_CHARACTERS | TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        edit.setPadding(0, 0, 0, 0);

        butt = new Button(context);
        butt.setClickable(false);
        butt.setLongClickable(false);
        butt.setTextSize(22f);
        butt.setInputType(TYPE_TEXT_FLAG_CAP_CHARACTERS | TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        butt.setPadding(0, 0, 0, 0);
        butt.setOnLongClickListener(new CoordDigitLongClickListener());

        final RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        edit.setLayoutParams(lp);
        butt.setLayoutParams(lp);

        addView(butt);
        addView(edit);

        edit.setVisibility(INVISIBLE);
    }

    public void setCustomChar(final char theChar) {
        butt.setText(String.valueOf(theChar));
    }

    public char getLabel() {
        return butt.getText().charAt(0);
    }

    public void setTextChangedListener(final TextWatcher watcher) {
        butt.addTextChangedListener(watcher);
    }
}
